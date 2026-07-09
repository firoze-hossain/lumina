package dev.lumina.syntax;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** XML/HTML highlighting for pom.xml and friends (IntelliJ-style colors). */
public final class XmlSyntaxHighlighter {

    private static final Pattern PATTERN = Pattern.compile(
            "(?<COMMENT><!--[\\s\\S]*?-->)"
                    + "|(?<CDATA><!\\[CDATA\\[[\\s\\S]*?]]>)"
                    + "|(?<VALUE>\"[^\"\n]*\"|'[^'\n]*')"
                    + "|(?<TAG></?[A-Za-z][\\w.:-]*|<\\?[\\w-]*|\\?>|/>|>)"
                    + "|(?<ATTR>\\b[A-Za-z][\\w.:-]*(?=\\s*=))");

    private static final int MAX_HIGHLIGHT_LENGTH = 400_000;

    private XmlSyntaxHighlighter() {
    }

    public static StyleSpans<Collection<String>> computeHighlighting(String text) {
        StyleSpansBuilder<Collection<String>> spans = new StyleSpansBuilder<>();
        if (text.length() > MAX_HIGHLIGHT_LENGTH) {
            spans.add(Collections.emptyList(), text.length());
            return spans.create();
        }
        Matcher matcher = PATTERN.matcher(text);
        int lastEnd = 0;
        while (matcher.find()) {
            String styleClass =
                    matcher.group("COMMENT") != null ? "xml-comment"
                            : matcher.group("CDATA") != null ? "xml-value"
                            : matcher.group("VALUE") != null ? "xml-value"
                            : matcher.group("TAG") != null ? "xml-tag"
                            : "xml-attr";
            spans.add(Collections.emptyList(), matcher.start() - lastEnd);
            spans.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastEnd = matcher.end();
        }
        spans.add(Collections.emptyList(), text.length() - lastEnd);
        return spans.create();
    }
}
