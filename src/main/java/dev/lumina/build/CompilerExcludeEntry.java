package dev.lumina.build;

import java.util.Objects;

/**
 * Model representing a path excluded from compilation.
 * Matches standard IDE compiler excludes table (media_1791430013046.png).
 */
public class CompilerExcludeEntry implements Cloneable {

    private String path;
    private boolean recursive;

    public CompilerExcludeEntry() {
        this("", true);
    }

    public CompilerExcludeEntry(String path, boolean recursive) {
        this.path = path != null ? path : "";
        this.recursive = recursive;
    }

    public CompilerExcludeEntry(CompilerExcludeEntry other) {
        if (other != null) {
            this.path = other.path;
            this.recursive = other.recursive;
        }
    }

    public String getPath() {
        return path != null ? path : "";
    }

    public void setPath(String path) {
        this.path = path != null ? path.trim() : "";
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    @Override
    public CompilerExcludeEntry clone() {
        return new CompilerExcludeEntry(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompilerExcludeEntry that = (CompilerExcludeEntry) o;
        return recursive == that.recursive && Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, recursive);
    }
}
