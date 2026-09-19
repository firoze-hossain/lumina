package dev.lumina.diagnostics;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Inspection matching IntelliJ IDEA:
 * Detects private fields that are declared but never used in the class.
 * Emits a WARNING diagnostic with quick-fixes to generate getters/setters,
 * add constructor parameter, or remove the field.
 */
public final class FieldDiagnostics {

    private static final Pattern FIELD_DECL_PAT = Pattern.compile(
            "\\bprivate\\s+(?:final\\s+)?(?:static\\s+)?([A-Za-z0-9_<>\\[\\],\\s]+?)\\s+([a-z][A-Za-z0-9_]*)\\s*(?:=\\s*[^;]+)?;");

    private FieldDiagnostics() {}

    /**
     * Inspects Java source code for unused private fields.
     */
    public static List<JavaDiagnostics.Diag> checkUnusedFields(Path file, String text, String moduleName) {
        if (text == null || text.isBlank() || !text.contains("private ")) {
            return List.of();
        }

        List<JavaDiagnostics.Diag> diags = new ArrayList<>();
        try {
            ParserConfiguration config = new ParserConfiguration();
            config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
            JavaParser parser = new JavaParser(config);
            ParseResult<CompilationUnit> pr = parser.parse(text);

            if (pr.getResult().isPresent()) {
                CompilationUnit cu = pr.getResult().get();
                String pkg = cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");

                for (ClassOrInterfaceDeclaration cls : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                    if (cls.isInterface()) continue;

                    // If Lombok handles fields, skip
                    if (hasLombok(cls)) continue;

                    String className = cls.getNameAsString();
                    String fqcn = pkg.isEmpty() ? className : pkg + "." + className;

                    for (FieldDeclaration fd : cls.getFields()) {
                        if (!fd.isPrivate() || fd.isStatic()) continue;
                        if (hasLombok(fd)) continue;

                        for (VariableDeclarator var : fd.getVariables()) {
                            String name = var.getNameAsString();
                            int usages = countUsagesInClass(cls, var, name);
                            if (usages == 0) {
                                int line = 1;
                                int start = -1;
                                int end = -1;

                                if (var.getName().getRange().isPresent()) {
                                    var range = var.getName().getRange().get();
                                    line = range.begin.line;
                                    start = offsetOf(text, range.begin.line, range.begin.column);
                                    end = offsetOf(text, range.end.line, range.end.column);
                                }

                                if (start < 0 || end <= start) {
                                    // Fallback to text offset
                                    int varIdx = findFieldIdentifierOffset(text, line, name);
                                    if (varIdx >= 0) {
                                        start = varIdx;
                                        end = varIdx + name.length();
                                    }
                                }

                                if (start >= 0 && end > start) {
                                    String title = "Private field '" + name + "' is never used";
                                    String msg = "Private field '" + name + "' is never used";
                                    String desc = "private " + var.getTypeAsString() + " " + name;
                                    String quickFix = "unused-field:" + name;

                                    diags.add(new JavaDiagnostics.Diag(
                                            JavaDiagnostics.Severity.WARNING,
                                            line,
                                            start,
                                            end,
                                            msg,
                                            quickFix,
                                            title,
                                            null,
                                            desc,
                                            fqcn,
                                            moduleName
                                    ));
                                }
                            }
                        }
                    }
                }
                return diags;
            }
        } catch (Throwable ignored) {
        }

        // Fallback with regex if JavaParser failed due to unparseable buffer
        return fallbackRegexInspect(text, moduleName);
    }

    private static boolean hasLombok(ClassOrInterfaceDeclaration cls) {
        return cls.getAnnotations().stream().anyMatch(a -> {
            String name = a.getNameAsString();
            return name.equals("Data") || name.equals("Getter") || name.equals("Value")
                    || name.equals("AllArgsConstructor") || name.equals("RequiredArgsConstructor");
        });
    }

    private static boolean hasLombok(FieldDeclaration fd) {
        return fd.getAnnotations().stream().anyMatch(a -> {
            String name = a.getNameAsString();
            return name.equals("Getter") || name.equals("Setter");
        });
    }

    private static int countUsagesInClass(ClassOrInterfaceDeclaration cls, VariableDeclarator var, String name) {
        int count = 0;

        // Check inside methods
        for (MethodDeclaration md : cls.getMethods()) {
            count += countUsagesInNode(md, name);
        }

        // Check inside constructors
        for (ConstructorDeclaration cd : cls.getConstructors()) {
            count += countUsagesInNode(cd, name);
        }

        // Check inside initializers
        for (InitializerDeclaration id : cls.findAll(InitializerDeclaration.class)) {
            count += countUsagesInNode(id, name);
        }

        return count;
    }

    private static int countUsagesInNode(com.github.javaparser.ast.Node node, String name) {
        int count = 0;
        for (NameExpr ne : node.findAll(NameExpr.class)) {
            if (ne.getNameAsString().equals(name)) {
                count++;
            }
        }
        for (FieldAccessExpr fae : node.findAll(FieldAccessExpr.class)) {
            if (fae.getNameAsString().equals(name)) {
                count++;
            }
        }
        return count;
    }

    private static int findFieldIdentifierOffset(String text, int line, String name) {
        int lineStart = 0;
        int curLine = 1;
        while (curLine < line && lineStart < text.length()) {
            int nl = text.indexOf('\n', lineStart);
            if (nl < 0) break;
            lineStart = nl + 1;
            curLine++;
        }
        int lineEnd = text.indexOf('\n', lineStart);
        if (lineEnd < 0) lineEnd = text.length();
        String lineStr = text.substring(lineStart, lineEnd);
        Pattern p = Pattern.compile("\\b" + Pattern.quote(name) + "\\b");
        Matcher m = p.matcher(lineStr);
        if (m.find()) {
            return lineStart + m.start();
        }
        return -1;
    }

    private static List<JavaDiagnostics.Diag> fallbackRegexInspect(String text, String moduleName) {
        List<JavaDiagnostics.Diag> diags = new ArrayList<>();
        String fqcn = null;
        Matcher pkgM = Pattern.compile("package\\s+([A-Za-z0-9_.]+);").matcher(text);
        String pkg = pkgM.find() ? pkgM.group(1) : "";
        Matcher clsM = Pattern.compile("(?:public\\s+)?class\\s+([A-Za-z0-9_]+)").matcher(text);
        if (clsM.find()) {
            fqcn = pkg.isEmpty() ? clsM.group(1) : pkg + "." + clsM.group(1);
        }

        Matcher m = FIELD_DECL_PAT.matcher(text);
        while (m.find()) {
            String type = m.group(1).trim();
            String name = m.group(2);
            int nameStart = m.start(2);
            int nameEnd = m.end(2);

            // Count occurrences outside this declaration
            Pattern wordPat = Pattern.compile("\\b" + Pattern.quote(name) + "\\b");
            Matcher wm = wordPat.matcher(text);
            int occurrences = 0;
            while (wm.find()) {
                if (wm.start() != nameStart) {
                    occurrences++;
                }
            }

            if (occurrences == 0) {
                int line = 1;
                for (int i = 0; i < nameStart; i++) {
                    if (text.charAt(i) == '\n') line++;
                }
                String title = "Private field '" + name + "' is never used";
                String desc = "private " + type + " " + name;
                diags.add(new JavaDiagnostics.Diag(
                        JavaDiagnostics.Severity.WARNING,
                        line,
                        nameStart,
                        nameEnd,
                        title,
                        "unused-field:" + name,
                        title,
                        null,
                        desc,
                        fqcn,
                        moduleName
                ));
            }
        }
        return diags;
    }

    private static int offsetOf(String text, int line, int column) {
        int offset = 0;
        int current = 1;
        while (current < line) {
            int nl = text.indexOf('\n', offset);
            if (nl < 0) break;
            offset = nl + 1;
            current++;
        }
        return Math.min(offset + Math.max(0, column - 1), text.length());
    }
}
