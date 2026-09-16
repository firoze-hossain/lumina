package dev.lumina.syntax;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Colorful application.yml highlighting, IntelliJ-style: keys, string/
 * number/boolean values, {@code ${...}} placeholders, {@code -} list
 * markers, anchors/tags, {@code #} comments and {@code ---} document
 * markers each get their own color.
 */
public final class YamlSyntaxHighlighter {

    private static final int MAX_HIGHLIGHT_LENGTH = 400_000;

    private static final Pattern VALUE_PATTERN = Pattern.compile(
            "(?<PLACEHOLDER>\\$\\{[^}\n]*\\})"
                    + "|(?<STRING>\"([^\"\\\\\n]|\\\\.)*\"|'([^'\n]|'')*')"
                    + "|(?<ANCHOR>[&*][A-Za-z0-9_.-]+)"
                    + "|(?<TAG>!!?[A-Za-z0-9_/.-]+)"
                    + "|(?<BOOL>(?i:true|false|yes|no|on|off|null)\\b|~)"
                    + "|(?<NUMBER>\\b\\d[\\d_]*(\\.\\d+)?([eE][+-]?\\d+)?\\b)");

    private YamlSyntaxHighlighter() {
    }

    public static StyleSpans<Collection<String>> computeHighlighting(String text) {
        StyleSpansBuilder<Collection<String>> spans = new StyleSpansBuilder<>();
        if (text.length() > MAX_HIGHLIGHT_LENGTH) {
            spans.add(Collections.emptyList(), text.length());
            return spans.create();
        }
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            highlightLine(spans, lines[i]);
            if (i < lines.length - 1) {
                spans.add(Collections.emptyList(), 1);   // the '\n' itself
            }
        }
        return spans.create();
    }

    private static void highlightLine(StyleSpansBuilder<Collection<String>> spans, String line) {
        String trimmed = line.stripLeading();
        int indent = line.length() - trimmed.length();
        if (trimmed.isEmpty()) {
            spans.add(Collections.emptyList(), line.length());
            return;
        }
        if (trimmed.startsWith("---") || trimmed.startsWith("...")) {
            spans.add(Collections.emptyList(), indent);
            spans.add(Collections.singleton("yaml-doc-marker"), line.length() - indent);
            return;
        }
        if (trimmed.charAt(0) == '#') {
            spans.add(Collections.emptyList(), indent);
            spans.add(Collections.singleton("yaml-comment"), line.length() - indent);
            return;
        }

        int col;
        if (trimmed.startsWith("- ") || trimmed.equals("-")) {
            spans.add(Collections.emptyList(), indent);
            spans.add(Collections.singleton("yaml-dash"), 1);
            int sp = indent + 1;
            while (sp < line.length() && line.charAt(sp) == ' ') sp++;
            if (sp > indent + 1) spans.add(Collections.emptyList(), sp - (indent + 1));
            col = sp;
        } else {
            spans.add(Collections.emptyList(), indent);
            col = indent;
        }

        String remainder = line.substring(col);
        if (remainder.isEmpty()) return;
        if (remainder.charAt(0) == '#') {
            spans.add(Collections.singleton("yaml-comment"), remainder.length());
            return;
        }

        int colon = findYamlColon(remainder);
        if (colon < 0) {
            highlightScalarRun(spans, remainder);
            return;
        }
        String key = remainder.substring(0, colon);
        spans.add(Collections.singleton("yaml-key"), key.length());
        spans.add(Collections.emptyList(), 1);   // the ':'
        String afterColon = colon + 1 <= remainder.length() ? remainder.substring(colon + 1) : "";
        highlightScalarRun(spans, afterColon);
    }

    private static void highlightScalarRun(StyleSpansBuilder<Collection<String>> spans, String s) {
        int lead = 0;
        while (lead < s.length() && Character.isWhitespace(s.charAt(lead))) lead++;
        if (lead > 0) spans.add(Collections.emptyList(), lead);
        String rest = s.substring(lead);

        int commentAt = findCommentStart(rest);
        String value = commentAt < 0 ? rest : rest.substring(0, commentAt);
        String comment = commentAt < 0 ? "" : rest.substring(commentAt);

        tokenizeValue(spans, value);
        if (!comment.isEmpty()) {
            spans.add(Collections.singleton("yaml-comment"), comment.length());
        }
    }

    private static void tokenizeValue(StyleSpansBuilder<Collection<String>> spans, String value) {
        if (value.isEmpty()) return;
        Matcher m = VALUE_PATTERN.matcher(value);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) {
                spans.add(Collections.singleton("yaml-value"), m.start() - last);
            }
            String cls = m.group("PLACEHOLDER") != null ? "yaml-placeholder"
                    : m.group("STRING") != null ? "yaml-string"
                    : m.group("ANCHOR") != null ? "yaml-anchor"
                    : m.group("TAG") != null ? "yaml-anchor"
                    : m.group("BOOL") != null ? "yaml-boolean"
                    : "yaml-number";
            spans.add(Collections.singleton(cls), m.end() - m.start());
            last = m.end();
        }
        if (value.length() > last) {
            spans.add(Collections.singleton("yaml-value"), value.length() - last);
        }
    }

    /** First ':' acting as a key/value separator (followed by whitespace or
     *  end of line) — skips colons inside quoted scalars and in bare
     *  values like "http://host:1234" that have no space after the ':'. */
    private static int findYamlColon(String s) {
        boolean inSingle = false, inDouble = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' && !inDouble) inSingle = !inSingle;
            else if (c == '"' && !inSingle) inDouble = !inDouble;
            else if (c == ':' && !inSingle && !inDouble) {
                if (i + 1 >= s.length() || Character.isWhitespace(s.charAt(i + 1))) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int findCommentStart(String s) {
        boolean inSingle = false, inDouble = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' && !inDouble) inSingle = !inSingle;
            else if (c == '"' && !inSingle) inDouble = !inDouble;
            else if (c == '#' && !inSingle && !inDouble
                    && (i == 0 || Character.isWhitespace(s.charAt(i - 1)))) {
                return i;
            }
        }
        return -1;
    }
}
