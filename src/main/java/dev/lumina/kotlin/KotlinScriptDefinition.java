package dev.lumina.kotlin;

import java.util.Objects;

/**
 * Represents a Kotlin Script definition entry in Lumina IDE.
 */
public class KotlinScriptDefinition {

    private String name;
    private String pattern;
    private boolean enabled;
    private boolean locked;

    public KotlinScriptDefinition() {
        this("", "", true, false);
    }

    public KotlinScriptDefinition(String name, String pattern, boolean enabled) {
        this(name, pattern, enabled, false);
    }

    public KotlinScriptDefinition(String name, String pattern, boolean enabled, boolean locked) {
        this.name = name != null ? name : "";
        this.pattern = pattern != null ? pattern : "";
        this.enabled = enabled;
        this.locked = locked;
    }

    public KotlinScriptDefinition(KotlinScriptDefinition other) {
        if (other != null) {
            this.name = other.name;
            this.pattern = other.pattern;
            this.enabled = other.enabled;
            this.locked = other.locked;
        }
    }

    public KotlinScriptDefinition copy() {
        return new KotlinScriptDefinition(this);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern != null ? pattern : "";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (!locked) {
            this.enabled = enabled;
        }
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KotlinScriptDefinition that = (KotlinScriptDefinition) o;
        return enabled == that.enabled &&
                locked == that.locked &&
                Objects.equals(name, that.name) &&
                Objects.equals(pattern, that.pattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, pattern, enabled, locked);
    }

    @Override
    public String toString() {
        return name + " (" + pattern + ") enabled=" + enabled;
    }
}
