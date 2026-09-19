package dev.lumina.semantics;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dynamic Lombok support matching IntelliJ IDEA:
 * - Automatically synthesizes getters, setters, builders, constructors, and loggers
 *   for classes annotated with Lombok annotations (@Data, @Getter, @Setter, @Builder,
 *   @SuperBuilder, @NoArgsConstructor, @AllArgsConstructor, @RequiredArgsConstructor,
 *   @Value, @Slf4j, @Log, @CommonsLog, @With, etc.).
 * - Suppresses false 'cannot find symbol' javac diagnostics emitted when javac compiles
 *   single files in memory without annotation-processing dependencies on -sourcepath.
 * - Powers code completion (Ctrl+Space) for Lombok-generated members.
 * - Powers Go-to-Declaration (Ctrl+Click) to navigate from synthetic methods back to fields/annotations.
 */
public final class LombokSupport {

    private static final ParserConfiguration PARSER_CONFIG = new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
    private static final JavaParser PARSER = new JavaParser(PARSER_CONFIG);

    private static final Map<Path, CachedClassInfo> CACHE = new ConcurrentHashMap<>();
    private static final Map<Path, Map<String, Path>> SIMPLE_NAME_INDEX_BY_ROOT = new ConcurrentHashMap<>();

    private record CachedClassInfo(FileTime mtime, Map<String, ClassLombokInfo> classes) {}

    public static class FieldInfo {
        public final String name;
        public final String type;
        public final boolean isStatic;
        public final boolean isFinal;
        public final Set<String> annotations;
        public final int line;

        public FieldInfo(String name, String type, boolean isStatic, boolean isFinal,
                         Set<String> annotations, int line) {
            this.name = name;
            this.type = type;
            this.isStatic = isStatic;
            this.isFinal = isFinal;
            this.annotations = annotations;
            this.line = line;
        }
    }

    public static class ClassLombokInfo {
        public final String className;
        public final String qualifiedName;
        public final String superClassName;
        public final Set<String> classAnnotations;
        public final List<FieldInfo> fields;
        public final Set<String> explicitMethodSignatures; // "name:paramCount"
        public final Set<Integer> explicitConstructorParamCounts;
        public final Map<String, Integer> fieldLineMap;
        public final Map<String, Integer> annotationLineMap;
        public final Path sourceFile;
        public final int classLine;

        public ClassLombokInfo(String className, String qualifiedName, String superClassName,
                               Set<String> classAnnotations, List<FieldInfo> fields,
                               Set<String> explicitMethodSignatures,
                               Set<Integer> explicitConstructorParamCounts,
                               Map<String, Integer> fieldLineMap,
                               Map<String, Integer> annotationLineMap,
                               Path sourceFile, int classLine) {
            this.className = className;
            this.qualifiedName = qualifiedName;
            this.superClassName = superClassName;
            this.classAnnotations = classAnnotations;
            this.fields = fields;
            this.explicitMethodSignatures = explicitMethodSignatures;
            this.explicitConstructorParamCounts = explicitConstructorParamCounts;
            this.fieldLineMap = fieldLineMap;
            this.annotationLineMap = annotationLineMap;
            this.sourceFile = sourceFile;
            this.classLine = classLine;
        }

        public boolean hasClassAnnotation(String name) {
            return classAnnotations.contains(name) || classAnnotations.contains("lombok." + name)
                    || classAnnotations.contains("lombok.experimental." + name)
                    || classAnnotations.contains("lombok.extern.slf4j." + name);
        }

        public boolean hasData() {
            return hasClassAnnotation("Data");
        }

        public boolean hasValue() {
            return hasClassAnnotation("Value");
        }

        public boolean hasGetter() {
            return hasClassAnnotation("Getter") || hasData() || hasValue();
        }

        public boolean hasSetter() {
            return (hasClassAnnotation("Setter") || hasData()) && !hasValue();
        }

        public boolean hasBuilder() {
            return hasClassAnnotation("Builder") || hasClassAnnotation("SuperBuilder");
        }

        public boolean hasNoArgsConstructor() {
            return hasClassAnnotation("NoArgsConstructor");
        }

        public boolean hasAllArgsConstructor() {
            return hasClassAnnotation("AllArgsConstructor");
        }

        public boolean hasRequiredArgsConstructor() {
            return hasClassAnnotation("RequiredArgsConstructor");
        }

        public boolean hasSlf4j() {
            return hasClassAnnotation("Slf4j");
        }

        public boolean hasLog() {
            return hasSlf4j() || hasClassAnnotation("Log") || hasClassAnnotation("CommonsLog")
                    || hasClassAnnotation("Log4j") || hasClassAnnotation("Log4j2")
                    || hasClassAnnotation("CustomLog") || hasClassAnnotation("XSlf4j")
                    || hasClassAnnotation("JBossLog") || hasClassAnnotation("Flogger");
        }

        public boolean hasWith() {
            return hasClassAnnotation("With");
        }

        public boolean hasToString() {
            return hasClassAnnotation("ToString") || hasData() || hasValue();
        }

        public boolean hasEqualsAndHashCode() {
            return hasClassAnnotation("EqualsAndHashCode") || hasData() || hasValue();
        }
    }

    private LombokSupport() {}

    // ----------------------------------------------------------------------------------
    // Diagnostic suppression logic
    // ----------------------------------------------------------------------------------

    private static final Pattern SYMBOL_PATTERN = Pattern.compile(
            "symbol:\\s+(?:(?:(method|variable|class|constructor)\\s+(?:<[^>]+>\\s*)?([A-Za-z0-9_$]+)(?:\\s*\\(([^)]*)\\))?)|(?:<[^>]+>\\s*)?([A-Za-z0-9_$]+))",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern LOCATION_PATTERN = Pattern.compile(
            "location:\\s+(?:variable\\s+[A-Za-z0-9_$]+\\s+of\\s+type\\s+|class\\s+|interface\\s+|record\\s+)?([A-Za-z0-9_$.]+)",
            Pattern.CASE_INSENSITIVE);

    /**
     * Inspects a javac diagnostic. Returns true if the unresolved symbol is generated by
     * Lombok on a referenced class or on the current compiling class.
     */
    public static boolean isLombokDiagnostic(Diagnostic<? extends JavaFileObject> d,
                                             Path compilingFile, String sourceText,
                                             List<Path> sourceRoots, String classpath) {
        String msg = d.getMessage(Locale.ROOT);
        if (msg == null) return false;

        String code = d.getCode();
        boolean isCantResolve = (code != null && code.contains("cant.resolve"))
                || msg.contains("cannot find symbol")
                || msg.contains("cant.resolve");

        if (!isCantResolve) return false;

        // 1. Try parsing symbol and location from javac message
        Matcher symMatcher = SYMBOL_PATTERN.matcher(msg);
        Matcher locMatcher = LOCATION_PATTERN.matcher(msg);

        String symbolKind = null;
        String symbolName = null;
        int paramCount = -1;

        if (symMatcher.find()) {
            symbolKind = symMatcher.group(1);
            symbolName = symMatcher.group(2);
            String args = symMatcher.group(3);
            if (symbolName == null) {
                symbolName = symMatcher.group(4);
            }
            if (args != null) {
                paramCount = args.trim().isEmpty() ? 0 : args.split(",").length;
            }
        }

        String locationType = null;
        if (locMatcher.find()) {
            locationType = locMatcher.group(1);
        }

        if (symbolName != null) {
            int angle = symbolName.lastIndexOf('>');
            if (angle >= 0) {
                symbolName = symbolName.substring(angle + 1).trim();
            }
        }

        if (locationType != null) {
            int angle = locationType.indexOf('<');
            if (angle >= 0) {
                locationType = locationType.substring(0, angle).trim();
            }
        }

        if (symbolName != null && locationType != null) {
            if (checkLombokMember(locationType, symbolKind, symbolName, paramCount,
                    sourceRoots, sourceText)) {
                return true;
            }
        }

        // 2. Complementary fallback: inspect the expression in sourceText at diagnostic position
        long startPos = d.getStartPosition();
        if (startPos >= 0 && startPos < sourceText.length()) {
            if (checkSourceExpressionAt(sourceText, (int) startPos, sourceRoots)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks whether member symbolName on targetTypeName is generated by Lombok.
     */
    public static boolean checkLombokMember(String targetTypeName, String symbolKind,
                                           String symbolName, int paramCount,
                                           List<Path> sourceRoots, String currentFileText) {
        if (targetTypeName == null || symbolName == null) return false;

        if (targetTypeName.contains("<")) {
            targetTypeName = targetTypeName.substring(0, targetTypeName.indexOf('<')).trim();
        }
        if (symbolName.contains(">")) {
            symbolName = symbolName.substring(symbolName.lastIndexOf('>') + 1).trim();
        }

        // Check if targetTypeName is an inner builder (e.g. User.UserBuilder or UserBuilder)
        if (targetTypeName.endsWith("Builder")) {
            String outerTypeName = targetTypeName.contains(".")
                    ? targetTypeName.substring(0, targetTypeName.lastIndexOf('.'))
                    : targetTypeName.substring(0, targetTypeName.length() - "Builder".length());

            ClassLombokInfo outerInfo = findClassInfo(outerTypeName, sourceRoots, currentFileText);
            if (outerInfo != null && outerInfo.hasBuilder()) {
                if ("build".equals(symbolName) || "toString".equals(symbolName)) {
                    return true;
                }
                // Builder setter methods matching any field in the outer class
                for (FieldInfo f : getAllFields(outerInfo, sourceRoots)) {
                    if (f.name.equals(symbolName)) {
                        return true;
                    }
                }
            }
        }

        ClassLombokInfo info = findClassInfo(targetTypeName, sourceRoots, currentFileText);
        if (info == null) return false;

        // Check logger (e.g. log.info(...))
        if ("variable".equalsIgnoreCase(symbolKind) || symbolKind == null) {
            if ("log".equals(symbolName) && info.hasLog()) {
                return true;
            }
        }

        // Check builder() method or builder class
        if ("method".equalsIgnoreCase(symbolKind) || symbolKind == null) {
            if ("builder".equals(symbolName) && (paramCount <= 0) && info.hasBuilder()) {
                return true;
            }
        }

        if ("class".equalsIgnoreCase(symbolKind)) {
            if (symbolName.endsWith("Builder") && info.hasBuilder()) {
                return true;
            }
        }

        // Check constructors: @NoArgsConstructor, @AllArgsConstructor, @RequiredArgsConstructor, @Builder
        if ("constructor".equalsIgnoreCase(symbolKind)) {
            if (paramCount == 0 && (info.hasNoArgsConstructor() || info.hasData())) {
                return true;
            }
            if (paramCount > 0 && (info.hasAllArgsConstructor() || info.hasBuilder() || info.hasRequiredArgsConstructor())) {
                return true;
            }
        }

        // Check standard getters & setters
        List<FieldInfo> allFields = getAllFields(info, sourceRoots);
        for (FieldInfo f : allFields) {
            if (f.isStatic) continue;

            // Getter checks
            if (info.hasGetter() || f.annotations.contains("Getter")) {
                if (matchesGetterName(symbolName, f)) {
                    if (paramCount <= 0) return true;
                }
            }

            // Setter checks
            if ((info.hasSetter() || f.annotations.contains("Setter")) && !f.isFinal) {
                if (matchesSetterName(symbolName, f)) {
                    if (paramCount <= 1) return true;
                }
            }

            // With checks
            if (info.hasWith() || f.annotations.contains("With")) {
                if (matchesWithName(symbolName, f)) {
                    if (paramCount <= 1) return true;
                }
            }
        }

        // Check toString, equals, hashCode
        if ("toString".equals(symbolName) && paramCount <= 0 && info.hasToString()) return true;
        if ("equals".equals(symbolName) && paramCount == 1 && info.hasEqualsAndHashCode()) return true;
        if ("hashCode".equals(symbolName) && paramCount <= 0 && info.hasEqualsAndHashCode()) return true;

        return false;
    }

    private static boolean matchesGetterName(String methodName, FieldInfo field) {
        String base = field.name;
        String cap = capitalize(base);
        if (methodName.equals("get" + cap)) return true;

        String type = field.type;
        boolean isBool = "boolean".equals(type) || "Boolean".equals(type)
                || type.endsWith(".Boolean");
        if (isBool) {
            if (methodName.equals("is" + cap)) return true;
            if (base.startsWith("is") && base.length() > 2 && Character.isUpperCase(base.charAt(2))) {
                if (methodName.equals(base)) return true;
            }
        }
        // Accessors(fluent=true)
        if (methodName.equals(base)) return true;
        return false;
    }

    private static boolean matchesSetterName(String methodName, FieldInfo field) {
        String base = field.name;
        String cap = capitalize(base);
        if (methodName.equals("set" + cap)) return true;

        String type = field.type;
        boolean isBool = "boolean".equals(type) || "Boolean".equals(type);
        if (isBool && base.startsWith("is") && base.length() > 2 && Character.isUpperCase(base.charAt(2))) {
            if (methodName.equals("set" + base.substring(2))) return true;
            if (methodName.equals("setIs" + base.substring(2))) return true;
        }
        // Accessors(fluent=true)
        if (methodName.equals(base)) return true;
        return false;
    }

    private static boolean matchesWithName(String methodName, FieldInfo field) {
        String cap = capitalize(field.name);
        return methodName.equals("with" + cap);
    }

    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        if (s.length() > 1 && Character.isLowerCase(s.charAt(0)) && Character.isUpperCase(s.charAt(1))) {
            return s; // JavaBeans naming convention for e.g. uName -> getuName()
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ----------------------------------------------------------------------------------
    // Source expression inspection fallback
    // ----------------------------------------------------------------------------------

    private static boolean checkSourceExpressionAt(String sourceText, int offset, List<Path> sourceRoots) {
        // Find identifier or member access at offset
        int start = offset;
        while (start > 0 && Character.isJavaIdentifierPart(sourceText.charAt(start - 1))) {
            start--;
        }
        int end = offset;
        while (end < sourceText.length() && Character.isJavaIdentifierPart(sourceText.charAt(end))) {
            end++;
        }
        if (start >= end) return false;

        String word = sourceText.substring(start, end);

        // Check if preceding is '.' e.g. "receiver.word" or "receiver.<TypeArgs>word"
        int p = start - 1;
        while (p >= 0 && Character.isWhitespace(sourceText.charAt(p))) p--;
        if (p >= 0 && sourceText.charAt(p) == '>') {
            int depth = 1;
            p--;
            while (p >= 0 && depth > 0) {
                if (sourceText.charAt(p) == '>') depth++;
                else if (sourceText.charAt(p) == '<') depth--;
                p--;
            }
            while (p >= 0 && Character.isWhitespace(sourceText.charAt(p))) p--;
        }
        if (p >= 0 && sourceText.charAt(p) == '.') {
            int dot = p;
            int rEnd = dot - 1;
            while (rEnd >= 0 && Character.isWhitespace(sourceText.charAt(rEnd))) rEnd--;
            int rStart = rEnd;
            while (rStart > 0 && Character.isJavaIdentifierPart(sourceText.charAt(rStart - 1))) {
                rStart--;
            }
            if (rStart >= 0 && rEnd >= rStart) {
                String receiver = sourceText.substring(rStart, rEnd + 1);
                // Resolve receiver type in current file
                String receiverType = resolveReceiverType(sourceText, start, receiver);
                if (receiverType != null) {
                    return checkLombokMember(receiverType, "method", word, -1,
                            sourceRoots, sourceText);
                }
            }
        } else {
            // Bare identifier e.g. log
            if ("log".equals(word)) {
                String thisClass = findCurrentClassName(sourceText);
                if (thisClass != null) {
                    ClassLombokInfo info = findClassInfo(thisClass, sourceRoots, sourceText);
                    return info != null && info.hasLog();
                }
            }
        }
        return false;
    }

    private static String resolveReceiverType(String text, int offset, String receiver) {
        if (receiver.isEmpty()) return null;
        if (Character.isUpperCase(receiver.charAt(0))) {
            return receiver; // Static type receiver e.g. User.builder()
        }
        // Search backwards in text for declaration "Type receiver"
        Pattern declPattern = Pattern.compile("\\b([A-Z][A-Za-z0-9_<>,\\[\\]\\s]*?)\\s+" + Pattern.quote(receiver) + "\\b");
        Matcher m = declPattern.matcher(text.substring(0, Math.min(offset + 100, text.length())));
        String lastType = null;
        while (m.find()) {
            String rawType = m.group(1).trim();
            // strip generics
            int angle = rawType.indexOf('<');
            if (angle > 0) rawType = rawType.substring(0, angle).trim();
            if (rawType.contains(" ")) {
                rawType = rawType.substring(rawType.lastIndexOf(' ') + 1);
            }
            lastType = rawType;
        }
        return lastType;
    }

    private static String findCurrentClassName(String text) {
        Matcher m = Pattern.compile("\\bclass\\s+([A-Za-z0-9_$]+)\\b").matcher(text);
        if (m.find()) return m.group(1);
        return null;
    }

    // ----------------------------------------------------------------------------------
    // Completions (for SemanticEngine.membersOf)
    // ----------------------------------------------------------------------------------

    public static List<Completion.Item> getLombokCompletions(ClassOrInterfaceDeclaration decl,
                                                             String prefix,
                                                             boolean staticOnly,
                                                             List<Path> sourceRoots) {
        List<Completion.Item> items = new ArrayList<>();
        ClassLombokInfo info = parseClassInfo(decl, null);
        if (info == null) return items;

        // Static members: builder()
        if (info.hasBuilder()) {
            if (Completion.matches(prefix, "builder")) {
                items.add(new Completion.Item("builder", "builder()", "builder()",
                        ": " + info.className + "Builder",
                        Completion.Kind.METHOD, null, 0));
            }
        }

        if (staticOnly) return items;

        // Instance members: getters & setters
        List<FieldInfo> allFields = getAllFields(info, sourceRoots);
        for (FieldInfo f : allFields) {
            if (f.isStatic) continue;

            // Getters
            if (info.hasGetter() || f.annotations.contains("Getter")) {
                String cap = capitalize(f.name);
                String getterName = "get" + cap;
                if (Completion.matches(prefix, getterName)) {
                    items.add(new Completion.Item(getterName, getterName + "()", getterName + "()",
                            ": " + simpleType(f.type),
                            Completion.Kind.METHOD, null, 0));
                }
                boolean isBool = "boolean".equals(f.type) || "Boolean".equals(f.type);
                if (isBool) {
                    String isName = "is" + cap;
                    if (Completion.matches(prefix, isName)) {
                        items.add(new Completion.Item(isName, isName + "()", isName + "()",
                                ": " + simpleType(f.type),
                                Completion.Kind.METHOD, null, 0));
                    }
                }
            }

            // Setters
            if ((info.hasSetter() || f.annotations.contains("Setter")) && !f.isFinal) {
                String cap = capitalize(f.name);
                String setterName = "set" + cap;
                if (Completion.matches(prefix, setterName)) {
                    String param = simpleType(f.type) + " " + f.name;
                    items.add(new Completion.Item(setterName, setterName + "(" + param + ")",
                            setterName + "(",
                            ": void",
                            Completion.Kind.METHOD, null, 0));
                }
            }
        }

        // Logger
        if (info.hasLog()) {
            if (Completion.matches(prefix, "log")) {
                items.add(new Completion.Item("log", "log", "log",
                        ": Logger",
                        Completion.Kind.FIELD, null, 0));
            }
        }

        return items;
    }

    public static List<Completion.Item> getBuilderCompletions(ClassOrInterfaceDeclaration decl,
                                                              String prefix,
                                                              List<Path> sourceRoots) {
        List<Completion.Item> items = new ArrayList<>();
        ClassLombokInfo info = parseClassInfo(decl, null);
        if (info == null || !info.hasBuilder()) return items;

        String builderType = info.className + "Builder";

        // Field setters
        for (FieldInfo f : getAllFields(info, sourceRoots)) {
            if (f.isStatic) continue;
            if (Completion.matches(prefix, f.name)) {
                String param = simpleType(f.type) + " " + f.name;
                items.add(new Completion.Item(f.name, f.name + "(" + param + ")",
                        f.name + "(",
                        ": " + builderType,
                        Completion.Kind.METHOD, null, 0));
            }
        }

        // build()
        if (Completion.matches(prefix, "build")) {
            items.add(new Completion.Item("build", "build()", "build()",
                    ": " + info.className,
                    Completion.Kind.METHOD, null, 0));
        }

        return items;
    }

    private static String simpleType(String type) {
        if (type == null) return "void";
        int idx = type.lastIndexOf('.');
        return idx >= 0 ? type.substring(idx + 1) : type;
    }

    // ----------------------------------------------------------------------------------
    // Go-to-Declaration (Ctrl+Click navigation)
    // ----------------------------------------------------------------------------------

    public record Location(Path file, int line) {}

    public static Location resolveLombokDeclaration(String targetClassName,
                                                    String memberName,
                                                    List<Path> sourceRoots,
                                                    String currentFileText) {
        if (targetClassName == null || memberName == null) return null;

        boolean isBuilder = false;
        String actualClass = targetClassName;
        if (actualClass.endsWith("Builder")) {
            actualClass = actualClass.contains(".")
                    ? actualClass.substring(0, actualClass.lastIndexOf('.'))
                    : actualClass.substring(0, actualClass.length() - "Builder".length());
            isBuilder = true;
        }

        ClassLombokInfo info = findClassInfo(actualClass, sourceRoots, currentFileText);
        if (info == null) return null;

        if (isBuilder) {
            if ("build".equals(memberName)) {
                int line = info.annotationLineMap.getOrDefault("Builder", info.classLine);
                return new Location(info.sourceFile, line);
            }
            // Builder field setter: locate the field in the class
            for (FieldInfo f : getAllFields(info, sourceRoots)) {
                if (f.name.equals(memberName)) {
                    return new Location(info.sourceFile, f.line);
                }
            }
        }

        if ("builder".equals(memberName) && info.hasBuilder()) {
            int line = info.annotationLineMap.getOrDefault("Builder",
                    info.annotationLineMap.getOrDefault("SuperBuilder", info.classLine));
            return new Location(info.sourceFile, line);
        }

        if ("log".equals(memberName) && info.hasLog()) {
            int line = info.classLine;
            for (String ann : List.of("Slf4j", "Log", "CommonsLog", "Log4j", "Log4j2")) {
                if (info.annotationLineMap.containsKey(ann)) {
                    line = info.annotationLineMap.get(ann);
                    break;
                }
            }
            return new Location(info.sourceFile, line);
        }

        // Getters and setters
        for (FieldInfo f : getAllFields(info, sourceRoots)) {
            if (matchesGetterName(memberName, f) || matchesSetterName(memberName, f)
                    || matchesWithName(memberName, f)) {
                return new Location(info.sourceFile, f.line);
            }
        }

        return null;
    }

    // ----------------------------------------------------------------------------------
    // Class parsing & indexing
    // ----------------------------------------------------------------------------------

    public static ClassLombokInfo findClassInfo(String typeName, List<Path> sourceRoots,
                                                String currentFileText) {
        if (typeName == null || typeName.isBlank() || sourceRoots == null) return null;

        Path source = findSourceFile(typeName, sourceRoots, currentFileText);
        if (source == null) return null;

        return getClassInfo(source, simpleTypeName(typeName));
    }

    public static List<FieldInfo> getAllFields(ClassLombokInfo info, List<Path> sourceRoots) {
        List<FieldInfo> result = new ArrayList<>(info.fields);
        if (info.superClassName != null && !info.superClassName.equals("Object")
                && !info.superClassName.equals("java.lang.Object")) {
            ClassLombokInfo superInfo = findClassInfo(info.superClassName, sourceRoots, null);
            if (superInfo != null) {
                result.addAll(getAllFields(superInfo, sourceRoots));
            }
        }
        return result;
    }

    private static String simpleTypeName(String typeName) {
        int idx = typeName.lastIndexOf('.');
        return idx >= 0 ? typeName.substring(idx + 1) : typeName;
    }

    public static Path findSourceFile(String typeName, List<Path> sourceRoots,
                                      String currentFileText) {
        if (sourceRoots == null || sourceRoots.isEmpty()) return null;

        // 1. Direct FQCN path lookup: a.b.MyClass -> a/b/MyClass.java
        if (typeName.contains(".")) {
            String[] parts = typeName.split("\\.");
            for (int end = parts.length; end >= 1; end--) {
                StringBuilder rel = new StringBuilder();
                for (int i = 0; i < end; i++) {
                    if (i > 0) rel.append('/');
                    rel.append(parts[i]);
                }
                rel.append(".java");
                for (Path root : sourceRoots) {
                    Path candidate = root.resolve(rel.toString());
                    if (Files.isRegularFile(candidate)) return candidate;
                }
            }
        }

        // 2. Resolve simple name via currentFileText imports
        String simple = simpleTypeName(typeName);
        if (currentFileText != null) {
            String fqcn = resolveSimpleFromImports(simple, currentFileText);
            if (fqcn != null) {
                Path found = findSourceFile(fqcn, sourceRoots, null);
                if (found != null) return found;
            }
        }

        // 3. Scan simple name index across source roots
        for (Path root : sourceRoots) {
            Map<String, Path> index = SIMPLE_NAME_INDEX_BY_ROOT.computeIfAbsent(root,
                    LombokSupport::buildSimpleNameIndex);
            Path candidate = index.get(simple);
            if (candidate != null && Files.isRegularFile(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private static String resolveSimpleFromImports(String simple, String sourceText) {
        for (String raw : sourceText.split("\n")) {
            String line = raw.strip();
            if (line.startsWith("import ") && line.endsWith("." + simple + ";")) {
                return line.substring(7, line.length() - 1).trim();
            }
        }
        String pkg = "";
        for (String raw : sourceText.split("\n")) {
            String line = raw.strip();
            if (line.startsWith("package ") && line.endsWith(";")) {
                pkg = line.substring(8, line.length() - 1).trim();
                break;
            }
        }
        if (!pkg.isEmpty()) {
            return pkg + "." + simple;
        }
        return null;
    }

    private static Map<String, Path> buildSimpleNameIndex(Path root) {
        Map<String, Path> map = new HashMap<>();
        if (!Files.isDirectory(root)) return map;
        try (var stream = Files.walk(root)) {
            stream.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".java"))
                    .forEach(p -> {
                        String fname = p.getFileName().toString();
                        String name = fname.substring(0, fname.length() - ".java".length());
                        map.putIfAbsent(name, p);
                    });
        } catch (IOException ignored) {
        }
        return map;
    }

    public static ClassLombokInfo getClassInfo(Path file, String simpleName) {
        try {
            FileTime mtime = Files.getLastModifiedTime(file);
            CachedClassInfo cached = CACHE.get(file);
            if (cached != null && cached.mtime().equals(mtime)) {
                ClassLombokInfo info = cached.classes().get(simpleName);
                if (info != null) return info;
                if (cached.classes().size() == 1) {
                    return cached.classes().values().iterator().next();
                }
            }

            ParseResult<CompilationUnit> pr = PARSER.parse(file);
            if (pr.getResult().isEmpty()) return null;

            CompilationUnit cu = pr.getResult().get();
            Map<String, ClassLombokInfo> classMap = new HashMap<>();
            for (ClassOrInterfaceDeclaration decl : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                ClassLombokInfo info = parseClassInfo(decl, file);
                if (info != null) {
                    classMap.put(info.className, info);
                }
            }
            CACHE.put(file, new CachedClassInfo(mtime, classMap));

            ClassLombokInfo result = classMap.get(simpleName);
            if (result != null) return result;
            return classMap.isEmpty() ? null : classMap.values().iterator().next();
        } catch (Throwable t) {
            return null;
        }
    }

    public static ClassLombokInfo parseClassInfo(ClassOrInterfaceDeclaration decl, Path file) {
        String name = decl.getNameAsString();
        String qualified = decl.getFullyQualifiedName().orElse(name);
        String superName = decl.getExtendedTypes().isNonEmpty()
                ? decl.getExtendedTypes().get(0).getNameAsString() : null;

        Set<String> classAnns = new HashSet<>();
        Map<String, Integer> annLineMap = new HashMap<>();
        for (AnnotationExpr a : decl.getAnnotations()) {
            String aName = a.getNameAsString();
            String simple = aName.contains(".") ? aName.substring(aName.lastIndexOf('.') + 1) : aName;
            classAnns.add(simple);
            int line = a.getRange().map(r -> r.begin.line).orElse(decl.getRange().map(r -> r.begin.line).orElse(1));
            annLineMap.put(simple, line);
        }

        List<FieldInfo> fields = new ArrayList<>();
        Map<String, Integer> fieldLines = new HashMap<>();
        for (FieldDeclaration fd : decl.getFields()) {
            boolean isStatic = fd.isStatic();
            boolean isFinal = fd.isFinal();
            String type = fd.getElementType().asString();
            Set<String> fdAnns = new HashSet<>();
            for (AnnotationExpr a : fd.getAnnotations()) {
                String aName = a.getNameAsString();
                fdAnns.add(aName.contains(".") ? aName.substring(aName.lastIndexOf('.') + 1) : aName);
            }
            int line = fd.getRange().map(r -> r.begin.line).orElse(1);
            for (VariableDeclarator vd : fd.getVariables()) {
                String vName = vd.getNameAsString();
                fields.add(new FieldInfo(vName, type, isStatic, isFinal, fdAnns, line));
                fieldLines.put(vName, line);
            }
        }

        Set<String> methodSigs = new HashSet<>();
        for (MethodDeclaration md : decl.getMethods()) {
            methodSigs.add(md.getNameAsString() + ":" + md.getParameters().size());
        }

        Set<Integer> ctorParams = new HashSet<>();
        for (ConstructorDeclaration cd : decl.getConstructors()) {
            ctorParams.add(cd.getParameters().size());
        }

        int classLine = decl.getRange().map(r -> r.begin.line).orElse(1);

        return new ClassLombokInfo(name, qualified, superName, classAnns, fields,
                methodSigs, ctorParams, fieldLines, annLineMap, file, classLine);
    }
}
