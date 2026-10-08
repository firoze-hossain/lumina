package dev.lumina.debugger;

import java.util.Objects;

/**
 * Filter rule used in Debugger Stepping settings (Images 3, 4, 5).
 */
public class SteppingFilter implements Cloneable {

    private boolean enabled = true;
    private String pattern = "";

    public SteppingFilter() {
    }

    public SteppingFilter(boolean enabled, String pattern) {
        this.enabled = enabled;
        this.pattern = pattern != null ? pattern : "";
    }

    public SteppingFilter(SteppingFilter other) {
        if (other != null) {
            this.enabled = other.enabled;
            this.pattern = other.pattern;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern != null ? pattern : "";
    }

    /**
     * Checks if this filter matches the given class name.
     */
    public boolean matches(String className) {
        if (!enabled || pattern == null || pattern.isBlank() || className == null) {
            return false;
        }
        String regex = pattern.replace(".", "\\.").replace("*", ".*").replace("?", ".");
        return className.matches(regex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SteppingFilter that)) return false;
        return enabled == that.enabled && Objects.equals(pattern, that.pattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, pattern);
    }

    @Override
    public SteppingFilter clone() {
        return new SteppingFilter(this);
    }
}
