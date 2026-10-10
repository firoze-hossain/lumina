package dev.lumina.stylesheets;

import java.util.Objects;

/**
 * Represents a path-to-CSS-dialect mapping.
 */
public class CssDialectMapping implements Cloneable {

    private String path = "";
    private String dialect = "<None>";

    public CssDialectMapping() {
    }

    public CssDialectMapping(String path, String dialect) {
        this.path = path != null ? path : "";
        this.dialect = dialect != null ? dialect : "<None>";
    }

    public CssDialectMapping(CssDialectMapping other) {
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
    public CssDialectMapping clone() {
        return new CssDialectMapping(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CssDialectMapping that = (CssDialectMapping) o;
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
