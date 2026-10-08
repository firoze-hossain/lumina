package dev.lumina.build;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration model for Compiler > Excludes.
 */
public class CompilerExcludesSettings implements Cloneable {

    private List<CompilerExcludeEntry> entries = new ArrayList<>();

    public CompilerExcludesSettings() {
    }

    public CompilerExcludesSettings(CompilerExcludesSettings other) {
        if (other != null && other.entries != null) {
            this.entries = new ArrayList<>();
            for (CompilerExcludeEntry e : other.entries) {
                this.entries.add(e.clone());
            }
        }
    }

    public List<CompilerExcludeEntry> getEntries() {
        return entries != null ? entries : new ArrayList<>();
    }

    public void setEntries(List<CompilerExcludeEntry> entries) {
        if (entries != null) {
            this.entries = new ArrayList<>(entries);
        } else {
            this.entries = new ArrayList<>();
        }
    }

    @Override
    public CompilerExcludesSettings clone() {
        return new CompilerExcludesSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompilerExcludesSettings that = (CompilerExcludesSettings) o;
        return Objects.equals(entries, that.entries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entries);
    }
}
