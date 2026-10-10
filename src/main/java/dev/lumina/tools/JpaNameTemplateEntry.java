package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing a name template rule in JPA Entity Declaration settings matching Image 1.
 */
public class JpaNameTemplateEntry implements Cloneable {

    private boolean enabled;
    private String target;
    private String caseFormat;
    private String prefix;
    private String postfix;
    private boolean underscore;
    private boolean pluralize;

    public JpaNameTemplateEntry() {
        this(true, "Table", "Lower", "", "", false, false);
    }

    public JpaNameTemplateEntry(boolean enabled, String target, String caseFormat, String prefix, String postfix, boolean underscore, boolean pluralize) {
        this.enabled = enabled;
        this.target = target != null ? target : "";
        this.caseFormat = caseFormat != null ? caseFormat : "Lower";
        this.prefix = prefix != null ? prefix : "";
        this.postfix = postfix != null ? postfix : "";
        this.underscore = underscore;
        this.pluralize = pluralize;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target != null ? target : "";
    }

    public String getCaseFormat() {
        return caseFormat;
    }

    public void setCaseFormat(String caseFormat) {
        this.caseFormat = caseFormat != null ? caseFormat : "Lower";
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix != null ? prefix : "";
    }

    public String getPostfix() {
        return postfix;
    }

    public void setPostfix(String postfix) {
        this.postfix = postfix != null ? postfix : "";
    }

    public boolean isUnderscore() {
        return underscore;
    }

    public void setUnderscore(boolean underscore) {
        this.underscore = underscore;
    }

    public boolean isPluralize() {
        return pluralize;
    }

    public void setPluralize(boolean pluralize) {
        this.pluralize = pluralize;
    }

    public JpaNameTemplateEntry copy() {
        return clone();
    }

    @Override
    public JpaNameTemplateEntry clone() {
        return new JpaNameTemplateEntry(enabled, target, caseFormat, prefix, postfix, underscore, pluralize);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JpaNameTemplateEntry that = (JpaNameTemplateEntry) o;
        return enabled == that.enabled &&
                underscore == that.underscore &&
                pluralize == that.pluralize &&
                Objects.equals(target, that.target) &&
                Objects.equals(caseFormat, that.caseFormat) &&
                Objects.equals(prefix, that.prefix) &&
                Objects.equals(postfix, that.postfix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, target, caseFormat, prefix, postfix, underscore, pluralize);
    }

    @Override
    public String toString() {
        return "JpaNameTemplateEntry{" +
                "target='" + target + '\'' +
                ", enabled=" + enabled +
                ", caseFormat='" + caseFormat + '\'' +
                ", underscore=" + underscore +
                '}';
    }
}
