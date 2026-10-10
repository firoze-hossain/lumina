package dev.lumina.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model for Languages & Frameworks > SQL Resolution Scopes settings in Lumina IDE.
 * Matches reference screenshot media_1791600399648_fe125a89.png:
 *  - Project mapping: [ <Default> v ]
 *  - Path to Resolution Scope mappings list
 */
public class SqlResolutionScopesSettings implements Cloneable {

    public static final List<String> STANDARD_SCOPES = List.of(
            "<Default>",
            "<All Data Sources>",
            "<None>"
    );

    private String projectMapping = "<Default>";
    private List<SqlResolutionScopeMapping> mappings = new ArrayList<>();

    public SqlResolutionScopesSettings() {
    }

    public SqlResolutionScopesSettings(SqlResolutionScopesSettings other) {
        if (other != null) {
            this.projectMapping = other.projectMapping != null ? other.projectMapping : "<Default>";
            this.mappings = new ArrayList<>();
            if (other.mappings != null) {
                for (SqlResolutionScopeMapping m : other.mappings) {
                    this.mappings.add(m.clone());
                }
            }
        }
    }

    public String getProjectMapping() {
        return projectMapping;
    }

    public void setProjectMapping(String projectMapping) {
        this.projectMapping = projectMapping != null ? projectMapping : "<Default>";
    }

    public List<SqlResolutionScopeMapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<SqlResolutionScopeMapping> mappings) {
        this.mappings = mappings != null ? new ArrayList<>(mappings) : new ArrayList<>();
    }

    public void addMapping(SqlResolutionScopeMapping mapping) {
        if (mapping != null) {
            this.mappings.add(mapping);
        }
    }

    public void removeMapping(SqlResolutionScopeMapping mapping) {
        this.mappings.remove(mapping);
    }

    @Override
    public SqlResolutionScopesSettings clone() {
        return new SqlResolutionScopesSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlResolutionScopesSettings that = (SqlResolutionScopesSettings) o;
        return Objects.equals(projectMapping, that.projectMapping) &&
                Objects.equals(mappings, that.mappings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectMapping, mappings);
    }

    @Override
    public String toString() {
        return "SqlResolutionScopesSettings{" +
                "projectMapping='" + projectMapping + '\'' +
                ", mappings=" + mappings +
                '}';
    }
}
