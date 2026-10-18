package dev.lumina.syntax;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regex-based SQL syntax highlighting for RichTextFX CodeArea in Lumina.
 */
public final class SqlSyntaxHighlighter {

    private static final String[] KEYWORDS = {
            "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET",
            "DELETE", "CREATE", "ALTER", "DROP", "TABLE", "VIEW", "INDEX", "SEQUENCE", "DATABASE",
            "SCHEMA", "JOIN", "INNER", "LEFT", "RIGHT", "FULL", "OUTER", "CROSS", "ON",
            "GROUP", "BY", "ORDER", "HAVING", "LIMIT", "OFFSET", "UNION", "ALL",
            "DISTINCT", "AS", "AND", "OR", "NOT", "NULL", "IS", "IN", "BETWEEN",
            "LIKE", "EXISTS", "CASE", "WHEN", "THEN", "ELSE", "END", "PRIMARY", "KEY",
            "FOREIGN", "REFERENCES", "DEFAULT", "UNIQUE", "CONSTRAINT", "ADD", "COLUMN",
            "TRUNCATE", "BEGIN", "COMMIT", "ROLLBACK", "TRANSACTION", "GRANT", "REVOKE",
            "WITH", "RETURNING", "IF", "REPLACE", "SHOW", "DESCRIBE", "EXPLAIN", "USE",
            "ASC", "DESC", "CASCADE", "RESTRICT", "CONSTRAINTS", "PURGE", "CHECK", "TRIGGER",
            "FUNCTION", "PROCEDURE", "TEMPORARY", "TEMP", "SECURITY", "LABEL", "OWNER"
    };

    private static final String[] TYPES = {
            "INT", "INTEGER", "BIGINT", "SMALLINT", "TINYINT", "VARCHAR", "CHAR", "TEXT",
            "DATE", "TIME", "TIMESTAMP", "BOOLEAN", "BOOL", "DECIMAL", "NUMERIC",
            "FLOAT", "DOUBLE", "REAL", "SERIAL", "BIGSERIAL", "UUID", "JSON", "JSONB", "BYTEA", "BLOB", "CLOB"
    };

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>\\b(?i:" + String.join("|", KEYWORDS) + ")\\b)"
            + "|(?<TYPE>\\b(?i:" + String.join("|", TYPES) + ")\\b)"
            + "|(?<PARAMETER>(?<!:):(?!:)[A-Za-z_][A-Za-z0-9_]*)"
            + "|(?<FUNCTION>\\b[A-Za-z_][A-Za-z0-9_]*\\b(?=\\s*\\())"
            + "|(?<QUALIFIER>\\b[A-Za-z_][A-Za-z0-9_]*\\b(?=\\.))"
            + "|(?<STRING>'[^']*')"
            + "|(?<NUMBER>\\b\\d+(\\.\\d+)?\\b)"
            + "|(?<COMMENT>--[^\\n]*|/\\*(.|\\R)*?\\*/)"
    );

    private SqlSyntaxHighlighter() {}

    public static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword" :
                    matcher.group("TYPE") != null ? "type" :
                    matcher.group("PARAMETER") != null ? "qualifier" :
                    matcher.group("FUNCTION") != null ? "method" :
                    matcher.group("QUALIFIER") != null ? "qualifier" :
                    matcher.group("STRING") != null ? "string" :
                    matcher.group("NUMBER") != null ? "number" :
                    matcher.group("COMMENT") != null ? "comment" :
                    null;
            assert styleClass != null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
}
