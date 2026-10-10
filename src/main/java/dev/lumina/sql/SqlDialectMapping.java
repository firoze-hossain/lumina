package dev.lumina.sql;

import java.util.Objects;

/**
 * Represents a path-to-SQL-dialect mapping.
 */
public class SqlDialectMapping implements Cloneable {

    private String path = "";
    private String dialect = "<None>";

    public SqlDialectMapping() {
    }

    public SqlDialectMapping(String path, String dialect) {
        this.path = path != null ? path : "";
        this.dialect = dialect != null ? dialect : "<None>";
    }

    public SqlDialectMapping(SqlDialectMapping other) {
        if (other != null) {
            this.path = other.path;
            this.dialect = other.dialect;
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path != null ? path : "";
    }

    public String getDialect() {
        return dialect;
    }

    public void setDialect(String dialect) {
        this.dialect = dialect != null ? dialect : "<None>";
    }

    @Override
    public SqlDialectMapping clone() {
        return new SqlDialectMapping(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlDialectMapping that = (SqlDialectMapping) o;
        return Objects.equals(path, that.path) && Objects.equals(dialect, that.dialect);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, dialect);
    }

    @Override
    public String toString() {
        return path + " -> " + dialect;
    }
}
