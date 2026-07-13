package dev.lumina.semantics;

import java.util.ArrayList;
import java.util.List;

/**
 * M2 — pure completion utilities: caret-context detection, prefix and
 * camel-hump matching, keyword/template items, and auto-import text logic.
 * No JavaFX or JavaParser imports, so this is fully unit-testable.
 */
public final class Completion {

    public enum Kind { VARIABLE, FIELD, METHOD, CLASS, KEYWORD, TEMPLATE }

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
     * prefixStart is the document offset where the typed prefix begins.
     */
    public record Context(boolean member, String receiver, String prefix,
                          int prefixStart) {
    }

    private Completion() {
    }

    // -------------------------------------------------------------- context

    /**
     * Inspect the text before the caret. Returns a member context after
     * "receiver.", a scope context otherwise, or null when completion makes
     * no sense here (e.g. after "). " chains M2 doesn't support).
     */
    public static Context contextAt(String text, int caret) {
        if (caret < 0 || caret > text.length()) return null;
        int prefixStart = caret;
        while (prefixStart > 0 && isIdentChar(text.charAt(prefixStart - 1))) {
            prefixStart--;
        }
        String prefix = text.substring(prefixStart, caret);
        if (prefixStart > 0 && text.charAt(prefixStart - 1) == '.') {
            int dot = prefixStart - 1;
            int receiverStart = dot;
            while (receiverStart > 0 && isIdentChar(text.charAt(receiverStart - 1))) {
                receiverStart--;
            }
            String receiver = text.substring(receiverStart, dot);
            if (receiver.isEmpty()) return null;   // "foo()." or "]." — not yet
            // number literal like "3." is not a member access
            if (Character.isDigit(receiver.charAt(0))) return null;
            return new Context(true, receiver, prefix, prefixStart);
        }
        return new Context(false, "", prefix, prefixStart);
    }

    private static boolean isIdentChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    // ------------------------------------------------------------- matching

    /** Prefix match or camel-hump match ("gCB" matches getCollectionById). */
    public static boolean matches(String prefix, String candidate) {
        if (prefix.isEmpty()) return true;
        if (candidate.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return true;
        }
        String humps = humpsOf(candidate);
        return humps.regionMatches(true, 0, prefix, 0, prefix.length());
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