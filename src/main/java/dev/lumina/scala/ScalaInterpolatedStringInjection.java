package dev.lumina.scala;

import java.util.Objects;

/**
 * Model representing an interpolated string language injection rule.
 * Matches Image 5:
 *  - Interpolated String prefix
 *  - Language ID
 */
public class ScalaInterpolatedStringInjection {

    private String prefix;
    private String languageId;

    public ScalaInterpolatedStringInjection() {
        this("", "");
    }

    public ScalaInterpolatedStringInjection(String prefix, String languageId) {
        this.prefix = prefix != null ? prefix : "";
        this.languageId = languageId != null ? languageId : "";
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix != null ? prefix : "";
    }

    public String getLanguageId() {
        return languageId;
    }

    public void setLanguageId(String languageId) {
        this.languageId = languageId != null ? languageId : "";
    }

    public ScalaInterpolatedStringInjection copy() {
        return new ScalaInterpolatedStringInjection(prefix, languageId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScalaInterpolatedStringInjection that = (ScalaInterpolatedStringInjection) o;
        return Objects.equals(prefix, that.prefix) &&
                Objects.equals(languageId, that.languageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix, languageId);
    }

    @Override
    public String toString() {
        return "ScalaInterpolatedStringInjection{" +
                "prefix='" + prefix + '\'' +
                ", languageId='" + languageId + '\'' +
                '}';
    }
}
