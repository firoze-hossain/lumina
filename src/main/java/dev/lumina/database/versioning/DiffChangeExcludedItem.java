package dev.lumina.database.versioning;

import java.util.Objects;

/**
 * Model representing an excluded change item from Liquibase/Flyway diff generation.
 */
public class DiffChangeExcludedItem implements Cloneable {

    private String tagName;
    private String targetName;

    public DiffChangeExcludedItem() {
        this("", "");
    }

    public DiffChangeExcludedItem(String tagName, String targetName) {
        this.tagName = tagName != null ? tagName : "";
        this.targetName = targetName != null ? targetName : "";
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName != null ? tagName : "";
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName != null ? targetName : "";
    }

    @Override
    public DiffChangeExcludedItem clone() {
        try {
            return (DiffChangeExcludedItem) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiffChangeExcludedItem that = (DiffChangeExcludedItem) o;
        return Objects.equals(tagName, that.tagName) &&
                Objects.equals(targetName, that.targetName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tagName, targetName);
    }
}
