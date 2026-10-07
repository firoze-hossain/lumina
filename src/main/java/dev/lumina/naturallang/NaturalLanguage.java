package dev.lumina.naturallang;

import java.util.Objects;

/**
 * Domain model representing a Natural Language supported by the proofreading engine.
 * Encapsulates the language code (e.g. "en-US", "de-DE"), display name, dialect count,
 * and whether it is a regional dialect variant.
 */
public class NaturalLanguage implements Comparable<NaturalLanguage> {

    private final String code;
    private final String displayName;
    private final int ruleCount;
    private final boolean dialect;

    public NaturalLanguage(String code, String displayName, int ruleCount, boolean dialect) {
        this.code = Objects.requireNonNull(code, "code cannot be null");
        this.displayName = Objects.requireNonNull(displayName, "displayName cannot be null");
        this.ruleCount = Math.max(1, ruleCount);
        this.dialect = dialect;
    }

    public NaturalLanguage(String code, String displayName, int ruleCount) {
        this(code, displayName, ruleCount, false);
    }

    public NaturalLanguage(String code, String displayName) {
        this(code, displayName, 1, false);
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getRuleCount() {
        return ruleCount;
    }

    public boolean isDialect() {
        return dialect;
    }

    @Override
    public int compareTo(NaturalLanguage other) {
        if (other == null) return 1;
        return this.displayName.compareToIgnoreCase(other.displayName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NaturalLanguage that)) return false;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
