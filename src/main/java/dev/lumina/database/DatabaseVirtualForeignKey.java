package dev.lumina.database;

import java.util.Objects;

/**
 * Model representing a Virtual Foreign Key pattern rule in Lumina IDE.
 */
public class DatabaseVirtualForeignKey implements Cloneable {

    private String columnPattern;
    private String targetColumnPattern;

    public DatabaseVirtualForeignKey() {
        this("(.*)_(?i)id", "$1\\.(?i)id");
    }

    public DatabaseVirtualForeignKey(String columnPattern, String targetColumnPattern) {
        this.columnPattern = columnPattern != null ? columnPattern : "";
        this.targetColumnPattern = targetColumnPattern != null ? targetColumnPattern : "";
    }

    public String getColumnPattern() {
        return columnPattern;
    }

    public void setColumnPattern(String columnPattern) {
        this.columnPattern = columnPattern != null ? columnPattern : "";
    }

    public String getTargetColumnPattern() {
        return targetColumnPattern;
    }

    public void setTargetColumnPattern(String targetColumnPattern) {
        this.targetColumnPattern = targetColumnPattern != null ? targetColumnPattern : "";
    }

    @Override
    public DatabaseVirtualForeignKey clone() {
        try {
            return (DatabaseVirtualForeignKey) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatabaseVirtualForeignKey that = (DatabaseVirtualForeignKey) o;
        return Objects.equals(columnPattern, that.columnPattern) &&
                Objects.equals(targetColumnPattern, that.targetColumnPattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(columnPattern, targetColumnPattern);
    }
}
