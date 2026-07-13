package dev.lumina.semantics;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * M4 — pure documentation utilities: Javadoc extraction from source text,
 * the enclosing-call scanner behind parameter info, and shared records.
 * No JavaFX/JavaParser imports; fully unit-testable.
 */
public final class Docs {

    /** Where the documentation for a symbol lives. */
    public record DocTarget(String signature, Path file, int line,
                            String libraryFqcn, String member, int paramCount) {
        public boolean isProject() {
            return file != null;
        }
        public boolean isLibrary() {
            return libraryFqcn != null;
        }
    }

    /** One overload for the parameter-info popup. */
    public record Signature(String returnType, String name, List<String> params) {
    }

    /** The call surrounding the caret: name, receiver, current arg index. */
    public record Call(String receiver, String method, int argIndex,
                       int openParenOffset) {
    }

    private Docs() {
    }

    // ------------------------------------------------------ javadoc extract

    /**
     * The cleaned Javadoc block directly above a 1-based declaration line,
     * skipping annotations and blank lines; null when there is none.
     */
    public static String javadocAbove(List<String> lines, int declLine) {
        int i = declLine - 2;   // 0-based line above the declaration
        while (i >= 0) {
            String s = lines.get(i).strip();
            if (s.isEmpty() || s.startsWith("@")) {
                i--;
                continue;
            }
            break;
        }
        if (i < 0 || !lines.get(i).strip().endsWith("*/")) return null;
        int end = i;
        int start = end;
        while (start >= 0 && !lines.get(start).strip().startsWith("/**")) {
            start--;
        }
        if (start < 0) return null;

        StringBuilder out = new StringBuilder();
        for (int k = start; k <= end; k++) {
            String s = lines.get(k).strip();
            s = s.replaceFirst("^/\\*\\*", "")
                    .replaceFirst("\\*/$", "")
                    .replaceFirst("^\\*", "")
                    .strip();
            if (s.isEmpty()) {
                if (!out.isEmpty() && out.charAt(out.length() - 1) != '\n') {
                    out.append('\n');
                }
                continue;
            }
            if (s.startsWith("@param") || s.startsWith("@return")
                    || s.startsWith("@throws")) {
                out.append('\n').append(s).append('\n');
            } else {
                if (!out.isEmpty() && out.charAt(out.length() - 1) != '\n') {
                    out.append(' ');
                }
                out.append(s);
            }
        }
        String text = out.toString()
                .replaceAll("<p>", "\n")
                .replaceAll("(?i)</?(b|i|em|strong|code|pre|ul|ol|li|br|tt|a href[^>]*|a)>", "")
                .replaceAll("\\{@code\\s+([^}]*)}", "$1")
                .replaceAll("\\{@link\\s+([^}]*)}", "$1")
                .replaceAll("\n{3,}", "\n\n")
                .strip();
        return text.isEmpty() ? null : text;
    }

    /**
     * 1-based line of a member (or type) declaration inside source text —
     * used against library sources-jar content. paramCount < 0 matches any
     * arity; type lookups pass member == the simple type name with
     * paramCount == -2.
     */
    public static int findMemberLine(String source, String member,
                                     int paramCount) {
        String[] lines = source.split("\n", -1);
        java.util.regex.Pattern typeDecl = java.util.regex.Pattern.compile(
                "\\b(class|interface|enum|record)\\s+"
                        + java.util.regex.Pattern.quote(member) + "\\b");
        java.util.regex.Pattern memberDecl = java.util.regex.Pattern.compile(
                "\\b" + java.util.regex.Pattern.quote(member) + "\\s*\\(");
        int nameOnly = -1;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (paramCount == -2) {
                if (typeDecl.matcher(line).find()) return i + 1;
                continue;
            }
            if (!memberDecl.matcher(line).find()) continue;
            String stripped = line.strip();
            if (stripped.startsWith("*") || stripped.startsWith("//")
                    || stripped.startsWith("return ")
                    || stripped.contains("." + member + "(")) {
                continue;
            }
            if (nameOnly < 0) nameOnly = i + 1;
            if (paramCount >= 0 && arity(line, member) == paramCount) {
                return i + 1;
            }
        }
        return nameOnly;
    }

    /** Rough argument count on a single declaration line. */
    private static int arity(String line, String member) {
        int open = line.indexOf(member);
        open = line.indexOf('(', open);
        if (open < 0) return -1;
        int depth = 0;
        int count = 0;
        boolean any = false;
        for (int i = open; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '(' || c == '<' || c == '[') depth++;
            else if (c == ')' || c == '>' || c == ']') {
                depth--;
                if (c == ')' && depth == 0) {
                    return any ? count + 1 : 0;
                }
            } else if (c == ',' && depth == 1) count++;
            else if (depth == 1 && !Character.isWhitespace(c)) any = true;
        }
        return -1;   // signature continues on the next line
    }

    // ------------------------------------------------------- enclosing call

    /**
     * Scan back from the caret for the unmatched '(' of the surrounding
     * call. Returns the method name, its receiver (may be empty), and which
     * argument the caret sits in; null when the caret is not inside a call.
     */
    public static Call enclosingCall(String text, int caret) {
        int depth = 0;
        int commas = 0;
        for (int i = Math.min(caret, text.length()) - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == '"' ) {                       // skip string literals
                i = openingQuote(text, i);
                if (i < 0) return null;
                continue;
            }
            if (c == ')' || c == ']' || c == '}') depth++;
            else if (c == ']' || c == '}') depth++;
            else if (c == '(' ) {
                if (depth == 0) {
                    // identifier before '(' is the method name
                    int nameEnd = i;
                    int nameStart = nameEnd;
                    while (nameStart > 0 && Character.isJavaIdentifierPart(
                            text.charAt(nameStart - 1))) {
                        nameStart--;
                    }
                    if (nameStart == nameEnd) return null;   // grouping paren
                    String method = text.substring(nameStart, nameEnd);
                    String receiver = "";
                    if (nameStart > 0 && text.charAt(nameStart - 1) == '.') {
                        int rEnd = nameStart - 1;
                        int rStart = rEnd;
                        while (rStart > 0 && Character.isJavaIdentifierPart(
                                text.charAt(rStart - 1))) {
                            rStart--;
                        }
                        receiver = text.substring(rStart, rEnd);
                    }
                    return new Call(receiver, method, commas, i);
                }
                depth--;
            } else if (c == ',' && depth == 0) commas++;
            else if (c == ';' || c == '{') return null;   // left the statement
        }
        return null;
    }

    private static int openingQuote(String text, int closing) {
        for (int i = closing - 1; i >= 0; i--) {
            if (text.charAt(i) == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                return i;
            }
        }
        return -1;
    }
}