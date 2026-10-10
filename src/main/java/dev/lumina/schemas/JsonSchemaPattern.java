package dev.lumina.schemas;

import java.util.Objects;

/**
 * Model representing a file, file pattern, or directory mapping target for a JSON Schema.
 */
public class JsonSchemaPattern {

    public enum PatternType {
        FILE("file"),
        PATTERN("file path pattern"),
        DIRECTORY("directory");

        private final String displayName;

        PatternType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private String pattern;
    private PatternType type = PatternType.PATTERN;

    public JsonSchemaPattern() {
        this("", PatternType.PATTERN);
    }

    public JsonSchemaPattern(String pattern, PatternType type) {
        this.pattern = pattern != null ? pattern : "";
        this.type = type != null ? type : PatternType.PATTERN;
    }

    public JsonSchemaPattern(PatternType type, String pattern) {
        this(pattern, type);
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern != null ? pattern : "";
    }

    public PatternType getType() {
        return type;
    }

    public void setType(PatternType type) {
        this.type = type != null ? type : PatternType.PATTERN;
    }

    public JsonSchemaPattern copy() {
        return new JsonSchemaPattern(pattern, type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JsonSchemaPattern that = (JsonSchemaPattern) o;
        return Objects.equals(pattern, that.pattern) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pattern, type);
    }

    @Override
    public String toString() {
        return "JsonSchemaPattern{" +
                "pattern='" + pattern + '\'' +
                ", type=" + type +
                '}';
    }
}
