package dev.lumina.syntax;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Regex-based Java syntax highlighting producing RichTextFX style spans. */
public final class JavaSyntaxHighlighter {

    private static final String[] KEYWORDS = {
            "abstract", "assert", "boolean", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "default", "do", "double",
            "else", "enum", "extends", "final", "finally", "float", "for",
            "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private",
            "protected", "public", "record", "return", "sealed", "short",
            "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "var", "void", "volatile",
            "while", "yield", "permits", "non-sealed"
    };

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String TYPE_PATTERN = "\\b[A-Z][A-Za-z0-9_]*\\b";
    private static final String ANNOTATION_PATTERN = "@[A-Za-z][A-Za-z0-9_]*";
    private static final String NUMBER_PATTERN = "\\b\\d[\\d_]*(\\.\\d+)?([eE][+-]?\\d+)?[fFdDlL]?\\b";
    private static final String STRING_PATTERN = "\"\"\"(.|\\R)*?\"\"\"|\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)'";
    private static final String COMMENT_PATTERN = "//[^\n]*|/\\*(.|\\R)*?\\*/";
    private static final String PAREN_PATTERN = "[()\\[\\]{}]";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<COMMENT>" + COMMENT_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<ANNOTATION>" + ANNOTATION_PATTERN + ")"
                    + "|(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<NUMBER>" + NUMBER_PATTERN + ")"
                    + "|(?<TYPE>" + TYPE_PATTERN + ")"
                    + "|(?<PAREN>" + PAREN_PATTERN + ")");

    private JavaSyntaxHighlighter() {
    }

    public static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastEnd = 0;
        StyleSpansBuilder<Collection<String>> spans = new StyleSpansBuilder<>();

        while (matcher.find()) {
            String styleClass =
                    matcher.group("COMMENT") != null ? "sx-comment"
                            : matcher.group("STRING") != null ? "sx-string"
                            : matcher.group("ANNOTATION") != null ? "sx-annotation"
                            : matcher.group("KEYWORD") != null ? "sx-keyword"
                            : matcher.group("NUMBER") != null ? "sx-number"
                            : matcher.group("TYPE") != null ? "sx-type"
                            : "sx-paren";
            spans.add(Collections.emptyList(), matcher.start() - lastEnd);
            spans.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastEnd = matcher.end();
        }
        spans.add(Collections.emptyList(), text.length() - lastEnd);
        return spans.create();
    }
}
