package dev.lumina.database.versioning;

import java.util.Objects;

/**
 * Model representing Tools > Database Versioning > Flyway settings in Lumina IDE.
 */
public class DatabaseFlywaySettings implements Cloneable {

    private String migrationPrefix = "V";
    private String versionPattern = "#increment(1, 1, \"0\")";
    private String migrationSeparator = "__";
    private String migrationDescription = "";
    private boolean useFlywayWithoutDependency = false;

    public DatabaseFlywaySettings() {
    }

    public String getMigrationPrefix() {
        return migrationPrefix;
    }

    public void setMigrationPrefix(String migrationPrefix) {
        this.migrationPrefix = migrationPrefix != null ? migrationPrefix : "V";
    }

    public String getVersionPattern() {
        return versionPattern;
    }

    public void setVersionPattern(String versionPattern) {
        this.versionPattern = versionPattern != null ? versionPattern : "#increment(1, 1, \"0\")";
    }

    public String getMigrationSeparator() {
        return migrationSeparator;
    }

    public void setMigrationSeparator(String migrationSeparator) {
        this.migrationSeparator = migrationSeparator != null ? migrationSeparator : "__";
    }

    public String getMigrationDescription() {
        return migrationDescription;
    }

    public void setMigrationDescription(String migrationDescription) {
        this.migrationDescription = migrationDescription != null ? migrationDescription : "";
    }

    public boolean isUseFlywayWithoutDependency() {
        return useFlywayWithoutDependency;
    }

    public void setUseFlywayWithoutDependency(boolean useFlywayWithoutDependency) {
        this.useFlywayWithoutDependency = useFlywayWithoutDependency;
    }

    @Override
    public DatabaseFlywaySettings clone() {
        try {
            return (DatabaseFlywaySettings) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatabaseFlywaySettings that = (DatabaseFlywaySettings) o;
        return useFlywayWithoutDependency == that.useFlywayWithoutDependency &&
                Objects.equals(migrationPrefix, that.migrationPrefix) &&
                Objects.equals(versionPattern, that.versionPattern) &&
                Objects.equals(migrationSeparator, that.migrationSeparator) &&
                Objects.equals(migrationDescription, that.migrationDescription);
    }

    @Override
    public int hashCode() {
        return Objects.hash(migrationPrefix, versionPattern, migrationSeparator, migrationDescription, useFlywayWithoutDependency);
    }
}
