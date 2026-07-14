package dev.lumina.refactor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * M5 — pure refactoring machinery: text edits, offset math, identifier
 * validation, variable-name guessing, and extract-method code generation.
 * No JavaFX or JavaParser imports; fully unit-testable.
 */
public final class Refactor {

    /** One replacement: [start, end) replaced by text. */
    public record Edit(int start, int end, String text) {
    }

    /** The analysis result behind Extract Method. */
    public record MethodPlan(boolean valid, String reason,
                             List<String> paramDecls, List<String> paramNames,
                             String returnType, String returnVar,
                             int insertAfterLine, boolean isStatic) {
        public static MethodPlan invalid(String reason) {
            return new MethodPlan(false, reason, List.of(), List.of(),
                    "void", null, -1, false);
        }
    }

    private Refactor() {
    }

    // ---------------------------------------------------------------- edits

    /** Apply edits to source; order does not matter (sorted internally). */
    public static String apply(String source, List<Edit> edits) {
        List<Edit> sorted = new ArrayList<>(edits);
        sorted.sort(Comparator.comparingInt(Edit::start).reversed());
        StringBuilder sb = new StringBuilder(source);
        for (Edit edit : sorted) {
            if (edit.start() < 0 || edit.end() > sb.length()
                    || edit.start() > edit.end()) {
                continue;
            }
            sb.replace(edit.start(), edit.end(), edit.text());
        }
        return sb.toString();
    }

    /**
     * Edit for a semantic usage: 1-based line, 1-based inclusive columns
     * (JavaParser Range convention) mapped to absolute offsets.
     */
    public static Edit editForUsage(String source, int line, int startCol,
                                    int endCol, String replacement) {
        int lineStart = lineStartOffset(source, line);
        return new Edit(lineStart + startCol - 1, lineStart + endCol,
                replacement);
    }

    /** Offset of the first character of a 1-based line. */
    public static int lineStartOffset(String source, int line) {
        int offset = 0;
        int current = 1;
        while (current < line) {
            int nl = source.indexOf('\n', offset);
            if (nl < 0) return offset;
            offset = nl + 1;
            current++;
        }
        return offset;
    }

    /** Offset just past the end of a 1-based line, including its newline. */
    public static int lineEndOffset(String source, int line) {
        int start = lineStartOffset(source, line);
        int nl = source.indexOf('\n', start);
        return nl < 0 ? source.length() : nl + 1;
    }

    /** 1-based line containing the given offset. */
    public static int lineOf(String source, int offset) {
        int line = 1;
        for (int i = 0; i < Math.min(offset, source.length()); i++) {
            if (source.charAt(i) == '\n') line++;
        }
        return line;
    }

    /** Offset of the start of the line containing offset. */
    public static int startOfLineAt(String source, int offset) {
        int nl = source.lastIndexOf('\n', Math.max(0, offset - 1));
        return nl < 0 ? 0 : nl + 1;
    }

    /** Leading whitespace of the line beginning at lineStart. */
    public static String indentAt(String source, int lineStart) {
        int i = lineStart;
        while (i < source.length()
                && (source.charAt(i) == ' ' || source.charAt(i) == '\t')) {
            i++;
        }
        return source.substring(lineStart, i);
    }

    // ---------------------------------------------------------- identifiers

    private static final Set<String> KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "default", "do", "double",
            "else", "enum", "extends", "final", "finally", "float", "for",
            "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private",
            "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while", "var",
            "true", "false", "null");

    public static boolean isValidIdentifier(String name) {
        if (name == null || name.isEmpty() || KEYWORDS.contains(name)) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(name.charAt(0))) return false;
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) return false;
        }
        return true;
    }

    /** A sensible variable name for an extracted expression. */
    public static String guessVarName(String expression) {
        String expr = expression.strip();
        // new FooBar(...) -> fooBar
        java.util.regex.Matcher ctor = java.util.regex.Pattern
                .compile("^new\\s+([A-Z][A-Za-z0-9_]*)").matcher(expr);
        if (ctor.find()) return decap(ctor.group(1));
        // trailing method call: a.b.getUserName( -> userName; compute( -> compute
        java.util.regex.Matcher call = java.util.regex.Pattern
                .compile("([A-Za-z_][A-Za-z0-9_]*)\\s*\\(")
                .matcher(expr);
        String lastCall = null;
        while (call.find()) lastCall = call.group(1);
        if (lastCall != null) {
            String stripped = lastCall.replaceFirst("^(get|find|fetch|load|build|create|to)", "");
            if (!stripped.isEmpty() && !stripped.equals(lastCall)) {
                return decap(stripped);
            }
            return decap(lastCall);
        }
        if (expr.startsWith("\"")) return "text";
        if (!expr.isEmpty() && (Character.isDigit(expr.charAt(0))
                || expr.charAt(0) == '-')) {
            return "value";
        }
        return "extracted";
    }

    private static String decap(String s) {
        if (s.isEmpty()) return s;
        String name = Character.toLowerCase(s.charAt(0)) + s.substring(1);
        return KEYWORDS.contains(name) ? name + "1" : name;
    }

    // ------------------------------------------------------- extract method

    /** The new method's full text, inserted after the enclosing method. */
    public static String buildMethodText(String name, MethodPlan plan,
                                         List<String> bodyLines,
                                         String indent) {
        String inner = indent + "    ";
        int minIndent = Integer.MAX_VALUE;
        for (String line : bodyLines) {
            if (line.isBlank()) continue;
            minIndent = Math.min(minIndent, leadingWhitespace(line));
        }
        if (minIndent == Integer.MAX_VALUE) minIndent = 0;

        StringBuilder sb = new StringBuilder("\n");
        sb.append(indent).append("private ")
                .append(plan.isStatic() ? "static " : "")
                .append(plan.returnType()).append(' ').append(name)
                .append('(').append(String.join(", ", plan.paramDecls()))
                .append(") {\n");
        for (String line : bodyLines) {
            if (line.isBlank()) {
                sb.append('\n');
                continue;
            }
            sb.append(inner)
                    .append(line.substring(Math.min(minIndent, line.length())))
                    .append('\n');
        }
        if (plan.returnVar() != null) {
            sb.append(inner).append("return ").append(plan.returnVar())
                    .append(";\n");
        }
        sb.append(indent).append("}\n");
        return sb.toString();
    }

    /** The call statement that replaces the selection. */
    public static String buildCallLine(String name, MethodPlan plan,
                                       String indent) {
        String call = name + "(" + String.join(", ", plan.paramNames()) + ");";
        if (plan.returnVar() != null) {
            call = plan.returnType() + " " + plan.returnVar() + " = " + call;
        }
        return indent + call + "\n";
    }

    private static int leadingWhitespace(String line) {
        int i = 0;
        while (i < line.length()
                && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
            i++;
        }
        return i;
    }
}