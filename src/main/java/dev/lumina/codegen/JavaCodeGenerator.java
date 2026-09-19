package dev.lumina.codegen;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AST-driven Java code generator for IntelliJ-style actions:
 * - Getters and Setters
 * - Constructors and Constructor Parameters
 * - toString()
 * - equals() and hashCode()
 * - Logger
 * - Safe field removal
 * Also generates live previews with target insertion line numbers.
 */
public final class JavaCodeGenerator {

    public record FieldInfo(String name, String type) {}
    public record Preview(int startLine, String code) {}

    private JavaCodeGenerator() {}

    // ---------------------------------------------------------------- Getters & Setters

    public static Preview previewGettersAndSetters(String source, String fieldName) {
        ClassModel model = parseModel(source);
        int insertLine = model.insertLineBeforeClassClose();
        String code = buildGetterAndSetterCode(model, fieldName);
        return new Preview(insertLine, code);
    }

    public static Preview previewGetter(String source, String fieldName) {
        ClassModel model = parseModel(source);
        int insertLine = model.insertLineBeforeClassClose();
        String code = buildGetterCode(model, fieldName);
        return new Preview(insertLine, code);
    }

    public static Preview previewSetter(String source, String fieldName) {
        ClassModel model = parseModel(source);
        int insertLine = model.insertLineBeforeClassClose();
        String code = buildSetterCode(model, fieldName);
        return new Preview(insertLine, code);
    }

    public static Preview previewAddConstructorParam(String source, String fieldName) {
        ClassModel model = parseModel(source);
        FieldInfo field = model.findField(fieldName);
        String type = field != null ? field.type() : "Object";
        String cap = capitalize(fieldName);

        if (model.constructors.isEmpty()) {
            int line = model.findFirstMethodOrConstructorLine();
            if (line <= 0) line = model.insertLineBeforeClassClose();
            String code = "public " + model.className + "(" + type + " " + fieldName + ") {\n"
                    + "    this." + fieldName + " = " + fieldName + ";\n"
                    + "}";
            return new Preview(line, code);
        } else {
            ConstructorInfo firstCtor = model.constructors.get(0);
            String params = firstCtor.paramList().isEmpty()
                    ? type + " " + fieldName
                    : firstCtor.paramList() + ", " + type + " " + fieldName;
            String code = "public " + model.className + "(" + params + ") {\n"
                    + (firstCtor.body().isBlank() ? "" : firstCtor.body() + "\n")
                    + "    this." + fieldName + " = " + fieldName + ";\n"
                    + "}";
            return new Preview(firstCtor.startLine(), code);
        }
    }

    public static Preview previewRemoveField(String source, String fieldName) {
        ClassModel model = parseModel(source);
        int line = model.fieldLine(fieldName);
        return new Preview(Math.max(1, line), "// field '" + fieldName + "' removed");
    }

    public static Preview previewThreadLocal(String source, String fieldName) {
        ClassModel model = parseModel(source);
        FieldInfo field = model.findField(fieldName);
        String type = field != null ? field.type() : "String";
        int line = model.fieldLine(fieldName);
        String code = "private ThreadLocal<" + type + "> " + fieldName + " = new ThreadLocal<>();";
        return new Preview(Math.max(1, line), code);
    }

    public static Preview previewAtomic(String source, String fieldName) {
        ClassModel model = parseModel(source);
        FieldInfo field = model.findField(fieldName);
        String type = field != null ? field.type() : "String";
        int line = model.fieldLine(fieldName);
        String code = "private java.util.concurrent.atomic.AtomicReference<" + type + "> " + fieldName + " = new java.util.concurrent.atomic.AtomicReference<>();";
        return new Preview(Math.max(1, line), code);
    }

    public static String convertToThreadLocal(String source, String fieldName) {
        ClassModel model = parseModel(source);
        FieldInfo field = model.findField(fieldName);
        String type = field != null ? field.type() : "String";
        Pattern pat = Pattern.compile("^[ \\t]*private\\s+[^;]*?\\b" + Pattern.quote(fieldName) + "\\s*(?:=[^;]+)?;[ \\t]*", Pattern.MULTILINE);
        Matcher m = pat.matcher(source);
        if (m.find()) {
            String repl = "    private ThreadLocal<" + type + "> " + fieldName + " = new ThreadLocal<>();";
            return source.substring(0, m.start()) + repl + source.substring(m.end());
        }
        return source;
    }

    public static String convertToAtomic(String source, String fieldName) {
        ClassModel model = parseModel(source);
        FieldInfo field = model.findField(fieldName);
        String type = field != null ? field.type() : "String";
        Pattern pat = Pattern.compile("^[ \\t]*private\\s+[^;]*?\\b" + Pattern.quote(fieldName) + "\\s*(?:=[^;]+)?;[ \\t]*", Pattern.MULTILINE);
        Matcher m = pat.matcher(source);
        if (m.find()) {
            String repl = "    private java.util.concurrent.atomic.AtomicReference<" + type + "> " + fieldName + " = new java.util.concurrent.atomic.AtomicReference<>();";
            return source.substring(0, m.start()) + repl + source.substring(m.end());
        }
        return source;
    }

    public static String generateGettersAndSetters(String source, String... targetFields) {
        ClassModel model = parseModel(source);
        List<String> toGen = targetFields != null && targetFields.length > 0
                ? Arrays.asList(targetFields)
                : model.fieldsWithoutGetterOrSetter();

        StringBuilder sb = new StringBuilder();
        for (String f : toGen) {
            String gs = buildGetterAndSetterCode(model, f);
            if (!gs.isEmpty()) {
                if (!sb.isEmpty()) sb.append("\n\n");
                sb.append(gs);
            }
        }
        if (sb.isEmpty()) return source;
        return insertBeforeClassClosingBrace(source, sb.toString());
    }

    public static String generateGetters(String source, String... targetFields) {
        ClassModel model = parseModel(source);
        List<String> toGen = targetFields != null && targetFields.length > 0
                ? Arrays.asList(targetFields)
                : model.fieldsWithoutGetter();

        StringBuilder sb = new StringBuilder();
        for (String f : toGen) {
            String g = buildGetterCode(model, f);
            if (!g.isEmpty()) {
                if (!sb.isEmpty()) sb.append("\n\n");
                sb.append(g);
            }
        }
        if (sb.isEmpty()) return source;
        return insertBeforeClassClosingBrace(source, sb.toString());
    }

    public static String generateSetters(String source, String... targetFields) {
        ClassModel model = parseModel(source);
        List<String> toGen = targetFields != null && targetFields.length > 0
                ? Arrays.asList(targetFields)
                : model.fieldsWithoutSetter();

        StringBuilder sb = new StringBuilder();
        for (String f : toGen) {
            String s = buildSetterCode(model, f);
            if (!s.isEmpty()) {
                if (!sb.isEmpty()) sb.append("\n\n");
                sb.append(s);
            }
        }
        if (sb.isEmpty()) return source;
        return insertBeforeClassClosingBrace(source, sb.toString());
    }

    public static String generateConstructor(String source, String... targetFields) {
        ClassModel model = parseModel(source);
        List<FieldInfo> fields = new ArrayList<>();
        if (targetFields != null && targetFields.length > 0) {
            for (String tf : targetFields) {
                FieldInfo fi = model.findField(tf);
                if (fi != null) fields.add(fi);
            }
        } else {
            fields.addAll(model.fields.values());
        }

        StringBuilder params = new StringBuilder();
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            FieldInfo f = fields.get(i);
            if (i > 0) params.append(", ");
            params.append(f.type()).append(" ").append(f.name());
            body.append("    this.").append(f.name()).append(" = ").append(f.name()).append(";\n");
        }

        String ctor = "public " + model.className + "(" + params + ") {\n"
                + body
                + "}";

        // Place constructor before first method if available, else before closing brace
        return insertBeforeFirstMethodOrClosingBrace(source, ctor);
    }

    public static String generateAddConstructorParam(String source, String fieldName) {
        ClassModel model = parseModel(source);
        FieldInfo fi = model.findField(fieldName);
        String type = fi != null ? fi.type() : "String";

        if (model.constructors.isEmpty()) {
            return generateConstructor(source, fieldName);
        }

        ConstructorInfo ctor = model.constructors.get(0);
        // Replace existing constructor
        String newParams = ctor.paramList().isEmpty()
                ? type + " " + fieldName
                : ctor.paramList() + ", " + type + " " + fieldName;
        String newBody = (ctor.body().isBlank() ? "" : ctor.body().stripTrailing() + "\n")
                + "    this." + fieldName + " = " + fieldName + ";";

        String newCtor = "public " + model.className + "(" + newParams + ") {\n"
                + newBody + "\n"
                + "}";

        return source.substring(0, ctor.startOffset()) + newCtor + source.substring(ctor.endOffset());
    }

    public static String generateToString(String source) {
        ClassModel model = parseModel(source);
        StringBuilder sb = new StringBuilder();
        sb.append("@Override\n");
        sb.append("public String toString() {\n");
        sb.append("    return \"").append(model.className).append("{\" +\n");

        int i = 0;
        for (FieldInfo f : model.fields.values()) {
            String prefix = i == 0 ? "            \"" : "            \", ";
            if ("String".equals(f.type())) {
                sb.append(prefix).append(f.name()).append("='\" + ").append(f.name()).append(" + '\\'' +\n");
            } else {
                sb.append(prefix).append(f.name()).append("=\" + ").append(f.name()).append(" +\n");
            }
            i++;
        }
        sb.append("            '}';\n");
        sb.append("}");

        return insertBeforeClassClosingBrace(source, sb.toString());
    }

    public static String generateEqualsAndHashCode(String source) {
        ClassModel model = parseModel(source);
        String pkgImport = "import java.util.Objects;\n";
        String updatedSource = source;
        if (!source.contains("java.util.Objects")) {
            updatedSource = insertImport(source, "java.util.Objects");
        }

        String varName = Character.toLowerCase(model.className.charAt(0)) + model.className.substring(1);
        StringBuilder eq = new StringBuilder();
        eq.append("@Override\n");
        eq.append("public boolean equals(Object o) {\n");
        eq.append("    if (this == o) return true;\n");
        eq.append("    if (o == null || getClass() != o.getClass()) return false;\n");
        eq.append("    ").append(model.className).append(" ").append(varName).append(" = (").append(model.className).append(") o;\n");

        if (model.fields.isEmpty()) {
            eq.append("    return true;\n");
        } else {
            StringBuilder checks = new StringBuilder();
            for (FieldInfo f : model.fields.values()) {
                if (!checks.isEmpty()) checks.append(" &&\n            ");
                checks.append("Objects.equals(").append(f.name()).append(", ").append(varName).append(".").append(f.name()).append(")");
            }
            eq.append("    return ").append(checks).append(";\n");
        }
        eq.append("}\n\n");

        eq.append("@Override\n");
        eq.append("public int hashCode() {\n");
        if (model.fields.isEmpty()) {
            eq.append("    return 0;\n");
        } else {
            StringBuilder args = new StringBuilder();
            for (FieldInfo f : model.fields.values()) {
                if (!args.isEmpty()) args.append(", ");
                args.append(f.name());
            }
            eq.append("    return Objects.hash(").append(args).append(");\n");
        }
        eq.append("}");

        return insertBeforeClassClosingBrace(updatedSource, eq.toString());
    }

    public static String generateLogger(String source) {
        ClassModel model = parseModel(source);
        String s = source;
        if (!s.contains("org.slf4j.Logger")) {
            s = insertImport(s, "org.slf4j.Logger");
        }
        if (!s.contains("org.slf4j.LoggerFactory")) {
            s = insertImport(s, "org.slf4j.LoggerFactory");
        }

        String loggerField = "    private static final Logger log = LoggerFactory.getLogger(" + model.className + ".class);\n\n";
        // Insert right after class opening brace
        Pattern p = Pattern.compile("(?:public\\s+)?class\\s+" + Pattern.quote(model.className) + "[^{]*\\{");
        Matcher m = p.matcher(s);
        if (m.find()) {
            int pos = m.end();
            return s.substring(0, pos) + "\n" + loggerField + s.substring(pos).stripLeading();
        }
        return s;
    }

    public static String removeField(String source, String fieldName) {
        Pattern pat = Pattern.compile("^[ \\t]*private\\s+[^;]*?\\b" + Pattern.quote(fieldName) + "\\s*(?:=[^;]+)?;[ \\t]*\\r?\\n?", Pattern.MULTILINE);
        Matcher m = pat.matcher(source);
        if (m.find()) {
            return source.substring(0, m.start()) + source.substring(m.end());
        }
        return source;
    }

    // ---------------------------------------------------------------- Internal Helpers

    private static String buildGetterAndSetterCode(ClassModel model, String fieldName) {
        FieldInfo f = model.findField(fieldName);
        if (f == null) return "";
        String g = buildGetterCode(model, fieldName);
        String s = buildSetterCode(model, fieldName);
        if (g.isEmpty() && s.isEmpty()) return "";
        if (g.isEmpty()) return s;
        if (s.isEmpty()) return g;
        return g + "\n\n" + s;
    }

    private static String buildGetterCode(ClassModel model, String fieldName) {
        FieldInfo f = model.findField(fieldName);
        if (f == null) return "";
        String cap = capitalize(fieldName);
        String prefix = "boolean".equalsIgnoreCase(f.type()) ? "is" : "get";
        String methodName = prefix + cap;
        if (model.hasMethod(methodName)) return "";

        return "public " + f.type() + " " + methodName + "() {\n"
                + "    return " + fieldName + ";\n"
                + "}";
    }

    private static String buildSetterCode(ClassModel model, String fieldName) {
        FieldInfo f = model.findField(fieldName);
        if (f == null) return "";
        String cap = capitalize(fieldName);
        String methodName = "set" + cap;
        if (model.hasMethod(methodName)) return "";

        return "public void " + methodName + "(" + f.type() + " " + fieldName + ") {\n"
                + "    this." + fieldName + " = " + fieldName + ";\n"
                + "}";
    }

    private static String insertBeforeClassClosingBrace(String source, String codeToInsert) {
        int lastBrace = source.lastIndexOf('}');
        if (lastBrace < 0) return source + "\n" + codeToInsert;

        // Indent lines
        String indented = indent(codeToInsert, "    ");
        String prefix = source.substring(0, lastBrace).stripTrailing();
        String suffix = source.substring(lastBrace);

        return prefix + "\n\n" + indented + "\n" + suffix;
    }

    private static String insertBeforeFirstMethodOrClosingBrace(String source, String codeToInsert) {
        ClassModel model = parseModel(source);
        int offset = model.firstMethodOrCtorOffset;
        if (offset > 0) {
            String indented = indent(codeToInsert, "    ");
            return source.substring(0, offset).stripTrailing() + "\n\n"
                    + indented + "\n\n    " + source.substring(offset).stripLeading();
        }
        return insertBeforeClassClosingBrace(source, codeToInsert);
    }

    private static String insertImport(String source, String fqcn) {
        if (source.contains("import " + fqcn + ";")) return source;
        Matcher m = Pattern.compile("^[ \\t]*package\\s+[^;]+;[ \\t]*\\r?\\n", Pattern.MULTILINE).matcher(source);
        if (m.find()) {
            int end = m.end();
            return source.substring(0, end) + "\nimport " + fqcn + ";\n" + source.substring(end).stripLeading();
        }
        return "import " + fqcn + ";\n\n" + source;
    }

    private static String indent(String code, String prefix) {
        String[] lines = code.split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) sb.append("\n");
            if (!lines[i].isBlank()) {
                sb.append(prefix).append(lines[i]);
            }
        }
        return sb.toString();
    }

    private static String capitalize(String name) {
        if (name == null || name.isEmpty()) return "";
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    // ---------------------------------------------------------------- AST Model

    public static record ConstructorInfo(int startLine, int startOffset, int endOffset, String paramList, String body) {}

    public static class ClassModel {
        public String className = "MyClass";
        public final Map<String, FieldInfo> fields = new LinkedHashMap<>();
        public final List<String> methods = new ArrayList<>();
        public final List<ConstructorInfo> constructors = new ArrayList<>();
        public int firstMethodOrCtorOffset = -1;
        public int firstMethodOrCtorLine = -1;
        public int lastClosingBraceLine = -1;
        public final Map<String, Integer> fieldLines = new LinkedHashMap<>();

        public FieldInfo findField(String name) {
            return fields.get(name);
        }

        public boolean hasMethod(String name) {
            return methods.contains(name);
        }

        public int fieldLine(String name) {
            return fieldLines.getOrDefault(name, 1);
        }

        public int insertLineBeforeClassClose() {
            return Math.max(1, lastClosingBraceLine);
        }

        public int findFirstMethodOrConstructorLine() {
            return firstMethodOrCtorLine;
        }

        public List<String> fieldsWithoutGetterOrSetter() {
            List<String> list = new ArrayList<>();
            for (Map.Entry<String, FieldInfo> e : fields.entrySet()) {
                String cap = capitalize(e.getKey());
                String g = ("boolean".equalsIgnoreCase(e.getValue().type()) ? "is" : "get") + cap;
                String s = "set" + cap;
                if (!methods.contains(g) || !methods.contains(s)) {
                    list.add(e.getKey());
                }
            }
            return list;
        }

        public List<String> fieldsWithoutGetter() {
            List<String> list = new ArrayList<>();
            for (Map.Entry<String, FieldInfo> e : fields.entrySet()) {
                String cap = capitalize(e.getKey());
                String g = ("boolean".equalsIgnoreCase(e.getValue().type()) ? "is" : "get") + cap;
                if (!methods.contains(g)) {
                    list.add(e.getKey());
                }
            }
            return list;
        }

        public List<String> fieldsWithoutSetter() {
            List<String> list = new ArrayList<>();
            for (Map.Entry<String, FieldInfo> e : fields.entrySet()) {
                String cap = capitalize(e.getKey());
                String s = "set" + cap;
                if (!methods.contains(s)) {
                    list.add(e.getKey());
                }
            }
            return list;
        }
    }

    public static ClassModel parseModel(String source) {
        ClassModel model = new ClassModel();
        if (source == null || source.isBlank()) return model;

        // Calculate last closing brace line
        int lastBrace = source.lastIndexOf('}');
        if (lastBrace >= 0) {
            int line = 1;
            for (int i = 0; i < lastBrace; i++) {
                if (source.charAt(i) == '\n') line++;
            }
            model.lastClosingBraceLine = line;
        }

        try {
            ParserConfiguration config = new ParserConfiguration();
            config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
            JavaParser parser = new JavaParser(config);
            ParseResult<CompilationUnit> pr = parser.parse(source);
            if (pr.getResult().isPresent()) {
                CompilationUnit cu = pr.getResult().get();
                Optional<ClassOrInterfaceDeclaration> optCls = cu.findFirst(ClassOrInterfaceDeclaration.class);
                if (optCls.isPresent()) {
                    ClassOrInterfaceDeclaration cls = optCls.get();
                    model.className = cls.getNameAsString();

                    for (FieldDeclaration fd : cls.getFields()) {
                        for (VariableDeclarator vd : fd.getVariables()) {
                            String fn = vd.getNameAsString();
                            model.fields.put(fn, new FieldInfo(fn, vd.getTypeAsString()));
                            vd.getName().getRange().ifPresent(r -> model.fieldLines.put(fn, r.begin.line));
                        }
                    }

                    for (MethodDeclaration md : cls.getMethods()) {
                        model.methods.add(md.getNameAsString());
                        if (model.firstMethodOrCtorLine < 0 && md.getRange().isPresent()) {
                            model.firstMethodOrCtorLine = md.getRange().get().begin.line;
                            model.firstMethodOrCtorOffset = offsetOf(source, md.getRange().get().begin.line, md.getRange().get().begin.column);
                        }
                    }

                    for (ConstructorDeclaration cd : cls.getConstructors()) {
                        if (cd.getRange().isPresent()) {
                            var r = cd.getRange().get();
                            int start = offsetOf(source, r.begin.line, r.begin.column);
                            int end = offsetOf(source, r.end.line, r.end.column);
                            StringBuilder params = new StringBuilder();
                            for (int i = 0; i < cd.getParameters().size(); i++) {
                                Parameter p = cd.getParameters().get(i);
                                if (i > 0) params.append(", ");
                                params.append(p.getTypeAsString()).append(" ").append(p.getNameAsString());
                            }
                            String body = cd.getBody().toString();
                            if (body.startsWith("{") && body.endsWith("}")) {
                                body = body.substring(1, body.length() - 1).strip();
                            }
                            model.constructors.add(new ConstructorInfo(r.begin.line, start, end, params.toString(), body));
                            if (model.firstMethodOrCtorLine < 0) {
                                model.firstMethodOrCtorLine = r.begin.line;
                                model.firstMethodOrCtorOffset = start;
                            }
                        }
                    }
                    return model;
                }
            }
        } catch (Throwable ignored) {
        }

        // Fallback regex parser
        Matcher clsM = Pattern.compile("(?:public\\s+)?class\\s+([A-Za-z0-9_]+)").matcher(source);
        if (clsM.find()) model.className = clsM.group(1);

        Matcher fm = Pattern.compile("(?:private|protected|public)\\s+(?:final\\s+)?([A-Za-z0-9_<>\\[\\],\\s]+?)\\s+([a-z][A-Za-z0-9_]*)\\s*(?:=[^;]+)?;").matcher(source);
        while (fm.find()) {
            String t = fm.group(1).trim();
            String n = fm.group(2);
            model.fields.put(n, new FieldInfo(n, t));
            int line = 1;
            for (int i = 0; i < fm.start(2); i++) {
                if (source.charAt(i) == '\n') line++;
            }
            model.fieldLines.put(n, line);
        }

        Matcher mm = Pattern.compile("(?:public|protected|private)?\\s+[A-Za-z0-9_<>\\[\\]]+\\s+([a-z][A-Za-z0-9_]*)\\s*\\(").matcher(source);
        while (mm.find()) {
            model.methods.add(mm.group(1));
            if (model.firstMethodOrCtorOffset < 0) {
                model.firstMethodOrCtorOffset = mm.start();
                int line = 1;
                for (int i = 0; i < mm.start(); i++) {
                    if (source.charAt(i) == '\n') line++;
                }
                model.firstMethodOrCtorLine = line;
            }
        }

        return model;
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
