package dev.lumina.semantics;

import java.util.ArrayList;
import java.util.List;

/**
 * M2 — pure completion utilities: caret-context detection, prefix and
 * camel-hump matching, keyword/template items, and auto-import text logic.
 * No JavaFX or JavaParser imports, so this is fully unit-testable.
 */
public final class Completion {

    public enum Kind { VARIABLE, FIELD, METHOD, CLASS, INTERFACE, KEYWORD, TEMPLATE, ANNOTATION }

    /**
     * One completion row. name is what matching runs against; label is what
     * the popup shows; insert is what lands in the editor; caretBack moves
     * the caret left after insertion (into parentheses); importFqcn, when
     * set and missing from the file, is added as an import.
     */
    public record Item(String name, String label, String insert, String detail,
                       Kind kind, String importFqcn, int caretBack) {
    }

    /**
     * Where completion was invoked. member=true means "receiver.prefix|";
     * annotation=true means "@prefix|"; extendsInterface is set when after
     * "interface <Name> extends ";
     * prefixStart is the document offset where the typed prefix begins.
     */
    public record Context(boolean member, boolean annotation, String receiver, String prefix,
                          int prefixStart, String extendsInterface) {
        public Context(boolean member, boolean annotation, String receiver, String prefix,
                       int prefixStart) {
            this(member, annotation, receiver, prefix, prefixStart, null);
        }
        public Context(boolean member, String receiver, String prefix, int prefixStart) {
            this(member, false, receiver, prefix, prefixStart, null);
        }
    }

    private Completion() {
    }

    // -------------------------------------------------------------- context

    /**
     * Inspect the text before the caret. Returns an annotation context after
     * "@", a member context after "receiver.", an extends context after "interface X extends ",
     * a scope context otherwise, or null when completion makes no sense here.
     */
    public static Context contextAt(String text, int caret) {
        if (caret < 0 || caret > text.length()) return null;
        int prefixStart = caret;
        while (prefixStart > 0 && isIdentChar(text.charAt(prefixStart - 1))) {
            prefixStart--;
        }
        String prefix = text.substring(prefixStart, caret);
        if (prefixStart > 0 && text.charAt(prefixStart - 1) == '@') {
            return new Context(false, true, "", prefix, prefixStart, null);
        }
        if (prefixStart > 0 && text.charAt(prefixStart - 1) == '.') {
            int dot = prefixStart - 1;
            int receiverStart = dot;
            while (receiverStart > 0 && isIdentChar(text.charAt(receiverStart - 1))) {
                receiverStart--;
            }
            String receiver = text.substring(receiverStart, dot);
            if (receiver.isEmpty() && dot > 2 && text.charAt(dot - 1) == ')' && text.charAt(dot - 2) == '(') {
                int callEnd = dot - 2;
                int callStart = callEnd;
                while (callStart > 0 && isIdentChar(text.charAt(callStart - 1))) {
                    callStart--;
                }
                String methodName = text.substring(callStart, callEnd);
                if ("builder".equals(methodName)) {
                    int pDot = callStart - 1;
                    while (pDot >= 0 && Character.isWhitespace(text.charAt(pDot))) pDot--;
                    if (pDot >= 0 && text.charAt(pDot) == '>') {
                        int depth = 1;
                        pDot--;
                        while (pDot >= 0 && depth > 0) {
                            if (text.charAt(pDot) == '>') depth++;
                            else if (text.charAt(pDot) == '<') depth--;
                            pDot--;
                        }
                        while (pDot >= 0 && Character.isWhitespace(text.charAt(pDot))) pDot--;
                    }
                    if (pDot >= 0 && text.charAt(pDot) == '.') {
                        int typeStart = pDot;
                        while (typeStart > 0 && isIdentChar(text.charAt(typeStart - 1))) {
                            typeStart--;
                        }
                        String typeName = text.substring(typeStart, pDot);
                        receiver = typeName + "Builder";
                    }
                }
            }
            if (receiver.isEmpty()) return null;   // "foo()." or "]." — not yet
            // number literal like "3." is not a member access
            if (Character.isDigit(receiver.charAt(0))) return null;
            return new Context(true, false, receiver, prefix, prefixStart, null);
        }
        String extendsInterface = detectExtendsInterface(text, prefixStart);
        return new Context(false, false, "", prefix, prefixStart, extendsInterface);
    }

    private static String detectExtendsInterface(String text, int offset) {
        int p = skipWhitespaceBackward(text, offset);
        if (p < 7 || !text.regionMatches(p - 7, "extends", 0, 7)) return null;
        if (p > 7 && isIdentChar(text.charAt(p - 8))) return null;

        int nameEnd = skipWhitespaceBackward(text, p - 7);
        int nameStart = nameEnd;
        while (nameStart > 0 && isIdentChar(text.charAt(nameStart - 1))) {
            nameStart--;
        }
        if (nameStart >= nameEnd) return null;
        String interfaceName = text.substring(nameStart, nameEnd);

        int intfEnd = skipWhitespaceBackward(text, nameStart);
        if (intfEnd < 9 || !text.regionMatches(intfEnd - 9, "interface", 0, 9)) return null;
        if (intfEnd > 9 && isIdentChar(text.charAt(intfEnd - 10))) return null;

        return interfaceName;
    }

    private static int skipWhitespaceBackward(String text, int from) {
        int p = from;
        while (p > 0 && Character.isWhitespace(text.charAt(p - 1))) {
            p--;
        }
        return p;
    }

    private static boolean isIdentChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    // ------------------------------------------------------------- matching

    /** Prefix match or camel-hump match ("gCB" matches getCollectionById). */
    public static boolean matches(String prefix, String candidate) {
        dev.lumina.settings.CodeCompletionSettings settings = dev.lumina.settings.CodeCompletionSettings.getInstance();
        boolean matchCase = settings.isMatchCase();
        boolean firstLetterOnly = settings.getMatchCaseMode() == dev.lumina.settings.CodeCompletionSettings.MatchCaseMode.FIRST_LETTER_ONLY;
        return matches(prefix, candidate, matchCase, firstLetterOnly);
    }

    public static boolean matches(String prefix, String candidate, boolean matchCase, boolean firstLetterOnly) {
        if (prefix.isEmpty()) return true;
        if (!matchCase) {
            if (candidate.regionMatches(true, 0, prefix, 0, prefix.length())) {
                return true;
            }
            String humps = humpsOf(candidate);
            return humps.regionMatches(true, 0, prefix, 0, prefix.length());
        }
        if (firstLetterOnly) {
            if (candidate.length() >= prefix.length() && candidate.charAt(0) == prefix.charAt(0)) {
                if (candidate.regionMatches(true, 0, prefix, 0, prefix.length())) {
                    return true;
                }
            }
            String humps = humpsOf(candidate);
            if (!humps.isEmpty() && humps.charAt(0) == prefix.charAt(0)) {
                return humps.regionMatches(true, 0, prefix, 0, prefix.length());
            }
            return false;
        } else {
            if (candidate.regionMatches(false, 0, prefix, 0, prefix.length())) {
                return true;
            }
            String humps = humpsOf(candidate);
            return humps.regionMatches(false, 0, prefix, 0, prefix.length());
        }
    }

    /** First character plus every subsequent uppercase character. */
    private static String humpsOf(String candidate) {
        if (candidate.isEmpty()) return "";
        StringBuilder humps = new StringBuilder();
        humps.append(candidate.charAt(0));
        for (int i = 1; i < candidate.length(); i++) {
            char c = candidate.charAt(i);
            if (Character.isUpperCase(c)) humps.append(c);
        }
        return humps.toString();
    }

    /**
     * Context for application.properties / application.yml key completion:
     * the prefix is everything typed so far on the current line, stopping
     * once a value has started (a properties "=" or a yaml "key: value").
     * Returns null once a value is present, since keys aren't being typed
     * anymore at that point.
     */
    public static Context contextForProperties(String text, int caret) {
        if (caret < 0 || caret > text.length()) return null;
        int lineStart = text.lastIndexOf('\n', caret - 1) + 1;
        String linePrefix = text.substring(lineStart, caret);
        if (linePrefix.contains("=")) return null;
        int colon = linePrefix.indexOf(':');
        if (colon >= 0 && !linePrefix.substring(colon + 1).isBlank()) return null;
        String trimmed = linePrefix.stripLeading();
        int keyStart = caret - trimmed.length();
        return new Context(false, "", trimmed, keyStart);
    }

    /**
     * Context for application.yml key completion: unlike flat .properties
     * files, a yaml key is only ever the leaf segment typed on the current
     * line \u2014 the parent path is reconstructed by walking upward through
     * shallower-indented ancestor keys, so prefix carries the *full* dotted
     * path (e.g. "spring.datasource.ur") for the provider to match against
     * real property names, while prefixStart still points at just the
     * local fragment on the current line for correct in-place insertion.
     */
    public static Context contextForYaml(String text, int caret) {
        if (caret < 0 || caret > text.length()) return null;
        int lineStart = text.lastIndexOf('\n', caret - 1) + 1;
        String linePrefix = text.substring(lineStart, caret);
        int colon = linePrefix.indexOf(':');
        if (colon >= 0 && !linePrefix.substring(colon + 1).isBlank()) return null;
        String trimmed = linePrefix.stripLeading();
        if (trimmed.startsWith("-") || trimmed.startsWith("#")) return null;
        int keyStart = caret - trimmed.length();
        int myIndent = linePrefix.length() - trimmed.length();

        String[] priorLines = text.substring(0, lineStart).split("\n", -1);
        List<String> ancestors = new ArrayList<>();
        int neededIndent = myIndent;
        for (int i = priorLines.length - 1; i >= 0 && neededIndent > 0; i--) {
            String line = priorLines[i];
            String lineTrimmed = line.stripLeading();
            if (lineTrimmed.isBlank()) continue;
            int indent = line.length() - lineTrimmed.length();
            if (indent < neededIndent) {
                int c = lineTrimmed.indexOf(':');
                String key = c < 0 ? lineTrimmed.trim() : lineTrimmed.substring(0, c).trim();
                if (key.isEmpty() || key.startsWith("#") || key.startsWith("-")) break;
                ancestors.add(0, key);
                neededIndent = indent;
            }
        }
        String fullPrefix = ancestors.isEmpty() ? trimmed
                : String.join(".", ancestors) + "." + trimmed;
        return new Context(false, "", fullPrefix, keyStart);
    }

    // -------------------------------------------------------------- imports

    /** True when inserting fqcn requires adding an import to this source. */
    public static boolean needsImport(String source, String fqcn) {
        if (fqcn == null || fqcn.isEmpty()) return false;
        if (fqcn.startsWith("java.lang.") && fqcn.lastIndexOf('.') == 9) {
            return false;   // java.lang.X is implicit (but not java.lang.reflect.X)
        }
        int lastDot = fqcn.lastIndexOf('.');
        if (lastDot < 0) return false;   // default package
        String pkg = fqcn.substring(0, lastDot);
        for (String rawLine : source.split("\n", -1)) {
            String line = rawLine.strip();
            if (line.equals("package " + pkg + ";")) return false;   // same package
            if (line.equals("import " + fqcn + ";")) return false;   // already there
            if (line.equals("import " + pkg + ".*;")) return false;  // wildcard
        }
        return true;
    }

    /** Document offset where a new import line should be inserted. */
    public static int importInsertOffset(String source) {
        int offset = 0;
        int lineStart = 0;
        int afterPackage = -1;
        int afterLastImport = -1;
        String[] lines = source.split("\n", -1);
        for (String raw : lines) {
            String line = raw.strip();
            int lineEnd = lineStart + raw.length() + 1;   // +1 for the \n
            if (line.startsWith("package ")) afterPackage = Math.min(lineEnd, source.length());
            if (line.startsWith("import ")) afterLastImport = Math.min(lineEnd, source.length());
            lineStart = lineEnd;
        }
        if (afterLastImport >= 0) return afterLastImport;
        if (afterPackage >= 0) return afterPackage;
        return offset;
    }

    // ---------------------------------------------------- keywords/templates

    private static final String[] KEYWORDS = {
            "abstract", "assert", "boolean", "break", "byte", "case", "catch",
            "char", "class", "continue", "default", "do", "double", "else",
            "enum", "extends", "final", "finally", "float", "for", "if",
            "implements", "import", "instanceof", "int", "interface", "long",
            "native", "new", "package", "private", "protected", "public",
            "record", "return", "sealed", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "var", "void", "volatile", "while", "yield",
            "true", "false", "null",
    };

    /** Keyword completions; empty prefix returns nothing (avoid flooding). */
    public static List<Item> keywordItems(String prefix) {
        if (prefix.isEmpty()) return List.of();
        List<Item> items = new ArrayList<>();
        for (String kw : KEYWORDS) {
            if (matches(prefix, kw)) {
                items.add(new Item(kw, kw, kw + " ", "keyword",
                        Kind.KEYWORD, null, 0));
            }
        }
        return items;
    }

    /** IntelliJ-style live templates: sout, psvm, fori. */
    public static List<Item> templateItems(String prefix) {
        if (prefix.isEmpty()) return List.of();
        List<Item> items = new ArrayList<>();
        if (matches(prefix, "sout")) {
            items.add(new Item("sout", "sout \u2192 System.out.println()",
                    "System.out.println();", "print to stdout",
                    Kind.TEMPLATE, null, 2));
        }
        if (matches(prefix, "psvm")) {
            items.add(new Item("psvm", "psvm \u2192 public static void main",
                    "public static void main(String[] args) {\n    \n}",
                    "main method", Kind.TEMPLATE, null, 2));
        }
        if (matches(prefix, "fori")) {
            items.add(new Item("fori", "fori \u2192 for (int i = 0; \u2026)",
                    "for (int i = 0; i < ; i++) {\n    \n}",
                    "indexed loop", Kind.TEMPLATE, null, 15));
        }
        return items;
    }
}