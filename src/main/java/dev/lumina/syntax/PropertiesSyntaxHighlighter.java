package dev.lumina.syntax;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Colorful application.properties highlighting, IntelliJ-style: a distinct
 * color for the key, the value, {@code #}/{@code !} comments, {@code ${...}}
 * placeholders and backslash escapes inside the value.
 */
public final class PropertiesSyntaxHighlighter {

    private static final int MAX_HIGHLIGHT_LENGTH = 400_000;
    private static final Pattern VALUE_TOKEN = Pattern.compile(
            "(?<PLACEHOLDER>\\$\\{[^}\n]*\\})|(?<ESCAPE>\\\\u[0-9A-Fa-f]{4}|\\\\.)");

    private PropertiesSyntaxHighlighter() {
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
        char first = trimmed.charAt(0);
        if (first == '#' || first == '!') {
            spans.add(Collections.emptyList(), indent);
            spans.add(Collections.singleton("prop-comment"), line.length() - indent);
            return;
        }
        int sep = findSeparator(line, indent);
        if (sep < 0) {
            spans.add(Collections.emptyList(), indent);
            spans.add(Collections.singleton("prop-key"), line.length() - indent);
            return;
        }
        spans.add(Collections.emptyList(), indent);
        spans.add(Collections.singleton("prop-key"), sep - indent);
        spans.add(Collections.emptyList(), 1);   // the '=' / ':' / space separator
        highlightValue(spans, line.substring(sep + 1));
    }

    /** First unescaped '=' or ':' or run of whitespace after the key. */
    private static int findSeparator(String line, int from) {
        for (int i = from; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\') { i++; continue; }
            if (c == '=' || c == ':' || Character.isWhitespace(c)) return i;
        }
        return -1;
    }

    private static void highlightValue(StyleSpansBuilder<Collection<String>> spans, String value) {
        int lead = 0;
        while (lead < value.length() && Character.isWhitespace(value.charAt(lead))) lead++;
        if (lead > 0) spans.add(Collections.emptyList(), lead);
        String rest = value.substring(lead);

        Matcher m = VALUE_TOKEN.matcher(rest);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) {
                spans.add(Collections.singleton("prop-value"), m.start() - last);
            }
            spans.add(Collections.singleton(m.group("PLACEHOLDER") != null
                    ? "prop-placeholder" : "prop-escape"), m.end() - m.start());
            last = m.end();
        }
        if (rest.length() > last) {
            spans.add(Collections.singleton("prop-value"), rest.length() - last);
        }
    }
}
