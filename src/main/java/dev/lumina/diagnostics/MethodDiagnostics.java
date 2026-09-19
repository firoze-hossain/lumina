package dev.lumina.diagnostics;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Inspection matching IntelliJ IDEA:
 * Detects methods that are declared but never used across the project or class.
 * Emits a WARNING diagnostic with muted gray styling and quick-fix to safe delete.
 */
public final class MethodDiagnostics {

    private static final Set<String> ENTRY_POINT_ANNOTATIONS = Set.of(
            "Override",
            "Test", "ParameterizedTest", "RepeatedTest", "TestFactory",
            "Bean", "PostConstruct", "PreDestroy", "EventListener", "Scheduled",
            "GetMapping", "PostMapping", "PutMapping", "DeleteMapping", "PatchMapping", "RequestMapping"
    );

    private MethodDiagnostics() {}

    public static List<JavaDiagnostics.Diag> checkUnusedMethods(
            Path file, String text, List<Path> sourceRoots, String moduleName) {
        if (text == null || text.isBlank() || !text.contains("(") || !text.contains(")")) {
            return List.of();
        }

        try {
            ParserConfiguration config = new ParserConfiguration();
            config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
            JavaParser parser = new JavaParser(config);
            ParseResult<CompilationUnit> pr = parser.parse(text);

            if (pr.getResult().isEmpty()) return List.of();
            CompilationUnit cu = pr.getResult().get();

            String pkg = cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");
            Map<String, String> importMap = buildImportMap(cu);

            List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);
            if (classes.isEmpty()) return List.of();

            List<CandidateMethod> candidates = new ArrayList<>();

            for (ClassOrInterfaceDeclaration cls : classes) {
                String className = cls.getNameAsString();
                String fqcn = pkg.isEmpty() ? className : pkg + "." + className;

                for (MethodDeclaration md : cls.getMethods()) {
                    if (isEntryPoint(md)) continue;

                    String name = md.getNameAsString();
                    int paramCount = md.getParameters().size();
                    boolean isPrivate = md.isPrivate();

                    int line = 1;
                    int start = -1;
                    int end = -1;
                    if (md.getName().getRange().isPresent()) {
                        var range = md.getName().getRange().get();
                        line = range.begin.line;
                        start = offsetOf(text, range.begin.line, range.begin.column);
                        end = offsetOf(text, range.end.line, range.end.column);
                    }

                    if (start < 0 || end <= start) {
                        int idx = findMethodIdentifierOffset(text, line, name);
                        if (idx >= 0) {
                            start = idx;
                            end = idx + name.length();
                        }
                    }

                    if (start >= 0 && end > start) {
                        candidates.add(new CandidateMethod(
                                md, cls, name, paramCount, isPrivate, fqcn, line, start, end));
                    }
                }
            }

            if (candidates.isEmpty()) return List.of();

            // Check usages
            // 1. Private methods: check inside current CU
            for (CandidateMethod c : candidates) {
                if (c.isPrivate) {
                    c.usages = countLocalUsages(c.cls, c.name, c.paramCount);
                }
            }

            // 2. Non-private methods: check project source roots
            List<CandidateMethod> nonPrivate = candidates.stream().filter(c -> !c.isPrivate).toList();
            if (!nonPrivate.isEmpty()) {
                checkProjectUsages(file, text, sourceRoots, nonPrivate);
            }

            // Build diagnostics for methods with 0 usages
            List<JavaDiagnostics.Diag> diags = new ArrayList<>();
            for (CandidateMethod c : candidates) {
                if (c.usages == 0) {
                    String fullParamTypes = formatParameterTypes(c.md, importMap, pkg, true);
                    String simpleParamTypes = formatParameterTypes(c.md, importMap, pkg, false);

                    String title = "Method '" + c.name + "(" + fullParamTypes + ")' is never used";
                    String msg = title;
                    String quickFix = "unused-method:" + c.name + "(" + simpleParamTypes + ")";
                    String desc = c.md.getDeclarationAsString(false, false, false);

                    diags.add(new JavaDiagnostics.Diag(
                            JavaDiagnostics.Severity.WARNING,
                            c.line,
                            c.start,
                            c.end,
                            msg,
                            quickFix,
                            title,
                            null,
                            desc,
                            c.fqcn,
                            moduleName
                    ));
                }
            }

            return diags;
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    private static boolean isEntryPoint(MethodDeclaration md) {
        // Exclude @Override, @Test, @Bean, etc.
        for (var a : md.getAnnotations()) {
            String aName = a.getNameAsString();
            if (ENTRY_POINT_ANNOTATIONS.contains(aName)) {
                return true;
            }
        }

        // Exclude main method
        if (md.getNameAsString().equals("main") && md.isStatic() && md.isPublic()) {
            if (md.getParameters().size() == 1) {
                String paramType = md.getParameter(0).getTypeAsString();
                if (paramType.equals("String[]") || paramType.equals("String...")) {
                    return true;
                }
            }
        }

        return false;
    }

    private static Map<String, String> buildImportMap(CompilationUnit cu) {
        Map<String, String> map = new HashMap<>();
        for (ImportDeclaration imp : cu.getImports()) {
            String qn = imp.getNameAsString();
            int lastDot = qn.lastIndexOf('.');
            if (lastDot > 0) {
                String simple = qn.substring(lastDot + 1);
                map.put(simple, qn);
            }
        }
        return map;
    }

    private static String formatParameterTypes(MethodDeclaration md, Map<String, String> importMap,
                                               String currentPkg, boolean fullFqcn) {
        List<String> list = new ArrayList<>();
        for (Parameter p : md.getParameters()) {
            String typeStr = p.getTypeAsString();
            if (fullFqcn) {
                list.add(toFqcn(typeStr, importMap, currentPkg));
            } else {
                list.add(cleanSimpleType(typeStr));
            }
        }
        return String.join(", ", list);
    }

    private static String cleanSimpleType(String type) {
        int genericIdx = type.indexOf('<');
        if (genericIdx > 0) {
            String base = type.substring(0, genericIdx);
            int lastDot = base.lastIndexOf('.');
            return (lastDot >= 0 ? base.substring(lastDot + 1) : base) + type.substring(genericIdx);
        }
        int lastDot = type.lastIndexOf('.');
        return lastDot >= 0 ? type.substring(lastDot + 1) : type;
    }

    private static String toFqcn(String type, Map<String, String> importMap, String currentPkg) {
        // Strip generics for resolution, then reconstruct
        int genericIdx = type.indexOf('<');
        String base = genericIdx > 0 ? type.substring(0, genericIdx).trim() : type.trim();
        String genericSuffix = "";
        if (genericIdx > 0 && type.endsWith(">")) {
            String inner = type.substring(genericIdx + 1, type.length() - 1);
            String[] parts = inner.split(",");
            List<String> resolvedParts = new ArrayList<>();
            for (String part : parts) {
                resolvedParts.add(toFqcn(part.trim(), importMap, currentPkg));
            }
            genericSuffix = "<" + String.join(", ", resolvedParts) + ">";
        }

        String resolvedBase;
        if (base.equals("String")) resolvedBase = "java.lang.String";
        else if (base.equals("Integer")) resolvedBase = "java.lang.Integer";
        else if (base.equals("Long")) resolvedBase = "java.lang.Long";
        else if (base.equals("Boolean")) resolvedBase = "java.lang.Boolean";
        else if (base.equals("Double")) resolvedBase = "java.lang.Double";
        else if (base.equals("Float")) resolvedBase = "java.lang.Float";
        else if (base.equals("Character")) resolvedBase = "java.lang.Character";
        else if (base.equals("Byte")) resolvedBase = "java.lang.Byte";
        else if (base.equals("Short")) resolvedBase = "java.lang.Short";
        else if (base.equals("Object")) resolvedBase = "java.lang.Object";
        else if (base.equals("int") || base.equals("long") || base.equals("boolean")
                || base.equals("double") || base.equals("float") || base.equals("char")
                || base.equals("byte") || base.equals("short") || base.equals("void")) {
            resolvedBase = base;
        } else if (importMap.containsKey(base)) {
            resolvedBase = importMap.get(base);
        } else if (base.contains(".")) {
            resolvedBase = base;
        } else if (!currentPkg.isEmpty()) {
            resolvedBase = currentPkg + "." + base;
        } else {
            resolvedBase = base;
        }

        return resolvedBase + genericSuffix;
    }

    private static int countLocalUsages(ClassOrInterfaceDeclaration cls, String name, int paramCount) {
        int count = 0;
        for (MethodCallExpr call : cls.findAll(MethodCallExpr.class)) {
            if (call.getNameAsString().equals(name)) {
                if (paramCount < 0 || call.getArguments().size() == paramCount) {
                    count++;
                }
            }
        }
        for (MethodReferenceExpr ref : cls.findAll(MethodReferenceExpr.class)) {
            if (ref.getIdentifier().equals(name)) {
                count++;
            }
        }
        return count;
    }

    private static void checkProjectUsages(Path currentFile, String currentText,
                                           List<Path> sourceRoots,
                                           List<CandidateMethod> candidates) {
        // Collect candidate names to search
        Set<String> namesToFind = new HashSet<>();
        for (CandidateMethod c : candidates) {
            namesToFind.add(c.name);
        }

        // Also check current file for calls to non-private methods (e.g. self-calls or sibling calls)
        checkContentForUsages(currentFile, currentText, candidates, namesToFind, true);

        if (namesToFind.isEmpty() || sourceRoots == null || sourceRoots.isEmpty()) {
            return;
        }

        // Scan all .java files across sourceRoots
        for (Path root : sourceRoots) {
            if (namesToFind.isEmpty()) break;
            if (!Files.isDirectory(root)) continue;

            try (Stream<Path> stream = Files.walk(root)) {
                List<Path> files = stream
                        .filter(p -> p.toString().endsWith(".java"))
                        .filter(p -> currentFile == null || !p.equals(currentFile))
                        .toList();

                for (Path p : files) {
                    if (namesToFind.isEmpty()) break;
                    String content;
                    try {
                        content = Files.readString(p);
                    } catch (Exception e) {
                        continue;
                    }

                    // Quick filter: does the content contain any candidate name as a word?
                    boolean anyMatch = false;
                    for (String name : namesToFind) {
                        if (content.contains(name)) {
                            anyMatch = true;
                            break;
                        }
                    }
                    if (!anyMatch) continue;

                    checkContentForUsages(p, content, candidates, namesToFind, false);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static void checkContentForUsages(Path file, String content,
                                              List<CandidateMethod> candidates,
                                              Set<String> namesToFind,
                                              boolean isDeclaringFile) {
        for (CandidateMethod c : candidates) {
            if (c.usages > 0) continue;
            if (!content.contains(c.name)) continue;

            // Check if content has method call or method reference
            Pattern callPat = Pattern.compile("\\b" + Pattern.quote(c.name) + "\\s*\\(");
            Pattern refPat = Pattern.compile("::\\s*" + Pattern.quote(c.name) + "\\b");

            if (!callPat.matcher(content).find() && !refPat.matcher(content).find()) {
                continue;
            }

            // In declaring file, avoid counting the declaration itself as a call
            if (isDeclaringFile) {
                int count = countLocalUsages(c.cls, c.name, c.paramCount);
                if (count > 0) {
                    c.usages += count;
                    namesToFind.remove(c.name);
                }
            } else {
                // In other files, any call or method reference to this candidate method counts as a usage
                c.usages++;
                namesToFind.remove(c.name);
            }
        }
    }

    private static int offsetOf(String text, int line, int column) {
        int curLine = 1;
        int curCol = 1;
        for (int i = 0; i < text.length(); i++) {
            if (curLine == line && curCol == column) return i;
            if (text.charAt(i) == '\n') {
                curLine++;
                curCol = 1;
            } else {
                curCol++;
            }
        }
        return -1;
    }

    private static int findMethodIdentifierOffset(String text, int line, String name) {
        String[] lines = text.split("\n", -1);
        if (line - 1 < 0 || line - 1 >= lines.length) return -1;
        int lineStart = 0;
        for (int i = 0; i < line - 1; i++) {
            lineStart += lines[i].length() + 1;
        }
        int idx = lines[line - 1].indexOf(name);
        return idx >= 0 ? lineStart + idx : -1;
    }

    private static final class CandidateMethod {
        final MethodDeclaration md;
        final ClassOrInterfaceDeclaration cls;
        final String name;
        final int paramCount;
        final boolean isPrivate;
        final String fqcn;
        final int line;
        final int start;
        final int end;
        int usages = 0;

        CandidateMethod(MethodDeclaration md, ClassOrInterfaceDeclaration cls, String name,
                        int paramCount, boolean isPrivate, String fqcn, int line, int start, int end) {
            this.md = md;
            this.cls = cls;
            this.name = name;
            this.paramCount = paramCount;
            this.isPrivate = isPrivate;
            this.fqcn = fqcn;
            this.line = line;
            this.start = start;
            this.end = end;
        }
    }
}
