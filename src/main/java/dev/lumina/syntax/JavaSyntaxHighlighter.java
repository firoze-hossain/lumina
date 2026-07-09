package dev.lumina.syntax;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regex-based Java highlighting with an IntelliJ-new-UI-style token set:
 * keywords, strings, numbers, comments, annotations, method calls,
 * constants, types, punctuation.
 */
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
            "while", "yield", "permits", "non-sealed", "true", "false", "null"
    };

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String CONST_PATTERN = "\\b[A-Z][A-Z0-9_]{2,}\\b";
    private static final String TYPE_PATTERN = "\\b[A-Z][A-Za-z0-9_]*\\b";
    private static final String METHOD_PATTERN = "\\b[a-z][A-Za-z0-9_]*(?=\\s*\\()";
    private static final String FIELD_PATTERN = "\\b[a-z][A-Za-z0-9_]*(?=\\s*\\.)";
    private static final String ANNOTATION_PATTERN = "@[A-Za-z][A-Za-z0-9_]*";
    private static final String NUMBER_PATTERN = "\\b\\d[\\d_]*(\\.\\d+)?([eE][+-]?\\d+)?[fFdDlL]?\\b";
    // NOTE: [\s\S]*? and unrolled string loops — never (.|\R)*?, which
    // recurses per character and overflows the JavaFX thread's stack.
    private static final String STRING_PATTERN =
            "\"\"\"[\\s\\S]*?\"\"\"|\"[^\"\\\\\n]*(\\\\.[^\"\\\\\n]*)*\"|'([^'\\\\]|\\\\.)'";
    private static final String COMMENT_PATTERN = "//[^\n]*|/\\*[\\s\\S]*?\\*/";
    private static final String PAREN_PATTERN = "[()\\[\\]{}]";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<COMMENT>" + COMMENT_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<ANNOTATION>" + ANNOTATION_PATTERN + ")"
                    + "|(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<NUMBER>" + NUMBER_PATTERN + ")"
                    + "|(?<METHOD>" + METHOD_PATTERN + ")"
                    + "|(?<FIELD>" + FIELD_PATTERN + ")"
                    + "|(?<CONST>" + CONST_PATTERN + ")"
                    + "|(?<TYPE>" + TYPE_PATTERN + ")"
                    + "|(?<PAREN>" + PAREN_PATTERN + ")");

    /** Files above this size are shown without highlighting (safety valve). */
    private static final int MAX_HIGHLIGHT_LENGTH = 400_000;

    private JavaSyntaxHighlighter() {
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
                    matcher.group("COMMENT") != null ? "sx-comment"
                            : matcher.group("STRING") != null ? "sx-string"
                            : matcher.group("ANNOTATION") != null ? "sx-annotation"
                            : matcher.group("KEYWORD") != null ? "sx-keyword"
                            : matcher.group("NUMBER") != null ? "sx-number"
                            : matcher.group("METHOD") != null ? "sx-method"
                            : matcher.group("FIELD") != null ? "sx-field"
                            : matcher.group("CONST") != null ? "sx-const"
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
