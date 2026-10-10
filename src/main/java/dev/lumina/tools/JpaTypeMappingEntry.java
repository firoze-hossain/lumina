package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing an SQL to Java/Hibernate type mapping rule in JPA Reverse Engineering settings matching Image 2.
 */
public class JpaTypeMappingEntry implements Cloneable {

    private String databaseEngine = "mysql";
    private String sqlType = "";
    private String targetType = "";

    public JpaTypeMappingEntry() {
        this("mysql", "", "");
    }

    public JpaTypeMappingEntry(String databaseEngine, String sqlType, String targetType) {
        this.databaseEngine = databaseEngine != null ? databaseEngine : "mysql";
        this.sqlType = sqlType != null ? sqlType : "";
        this.targetType = targetType != null ? targetType : "";
    }

    public String getDatabaseEngine() {
        return databaseEngine;
    }

    public void setDatabaseEngine(String databaseEngine) {
        this.databaseEngine = databaseEngine != null ? databaseEngine : "mysql";
    }

    public String getSqlType() {
        return sqlType;
    }

    public void setSqlType(String sqlType) {
        this.sqlType = sqlType != null ? sqlType : "";
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType != null ? targetType : "";
    }

    public JpaTypeMappingEntry copy() {
        return clone();
    }

    @Override
    public JpaTypeMappingEntry clone() {
        return new JpaTypeMappingEntry(databaseEngine, sqlType, targetType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JpaTypeMappingEntry that = (JpaTypeMappingEntry) o;
        return Objects.equals(databaseEngine, that.databaseEngine) &&
                Objects.equals(sqlType, that.sqlType) &&
                Objects.equals(targetType, that.targetType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(databaseEngine, sqlType, targetType);
    }

    @Override
    public String toString() {
        return databaseEngine + ": " + sqlType + " -> " + targetType;
    }
}
