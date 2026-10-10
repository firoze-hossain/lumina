package dev.lumina.sql;

import java.util.Objects;

/**
 * Represents a path-to-SQL-resolution-scope mapping.
 */
public class SqlResolutionScopeMapping implements Cloneable {

    private String path = "";
    private String resolutionScope = "<Default>";

    public SqlResolutionScopeMapping() {
    }

    public SqlResolutionScopeMapping(String path, String resolutionScope) {
        this.path = path != null ? path : "";
        this.resolutionScope = resolutionScope != null ? resolutionScope : "<Default>";
    }

    public SqlResolutionScopeMapping(SqlResolutionScopeMapping other) {
        if (other != null) {
            this.path = other.path;
            this.resolutionScope = other.resolutionScope;
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path != null ? path : "";
    }

    public String getResolutionScope() {
        return resolutionScope;
    }

    public void setResolutionScope(String resolutionScope) {
        this.resolutionScope = resolutionScope != null ? resolutionScope : "<Default>";
    }

    @Override
    public SqlResolutionScopeMapping clone() {
        return new SqlResolutionScopeMapping(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlResolutionScopeMapping that = (SqlResolutionScopeMapping) o;
        return Objects.equals(path, that.path) && Objects.equals(resolutionScope, that.resolutionScope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, resolutionScope);
    }

    @Override
    public String toString() {
        return path + " -> " + resolutionScope;
    }
}
