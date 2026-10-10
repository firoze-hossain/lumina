package dev.lumina.database;

import java.util.Objects;

/**
 * Model representing a user parameter pattern rule for SQL consoles and files in Lumina IDE.
 */
public class DatabaseUserParameterPattern implements Cloneable {

    private String pattern;
    private String inScope;
    private String language;

    public DatabaseUserParameterPattern() {
        this("", "everywhere", "");
    }

    public DatabaseUserParameterPattern(String pattern, String inScope, String language) {
        this.pattern = pattern != null ? pattern : "";
        this.inScope = inScope != null ? inScope : "everywhere";
        this.language = language != null ? language : "";
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern != null ? pattern : "";
    }

    public String getInScope() {
        return inScope;
    }

    public void setInScope(String inScope) {
        this.inScope = inScope != null ? inScope : "everywhere";
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language != null ? language : "";
    }

    @Override
    public DatabaseUserParameterPattern clone() {
        try {
            return (DatabaseUserParameterPattern) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatabaseUserParameterPattern that = (DatabaseUserParameterPattern) o;
        return Objects.equals(pattern, that.pattern) &&
                Objects.equals(inScope, that.inScope) &&
                Objects.equals(language, that.language);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pattern, inScope, language);
    }
}
