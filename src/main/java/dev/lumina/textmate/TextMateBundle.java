package dev.lumina.textmate;

import java.util.Objects;

/**
 * Represents a TextMate syntax and grammar bundle in Lumina IDE.
 * Can be a built-in bundled syntax or a custom user/plugin-added bundle.
 */
public class TextMateBundle {

    private final String name;
    private final String path;
    private final boolean builtIn;
    private boolean enabled;

    public TextMateBundle(String name, String path, boolean builtIn, boolean enabled) {
        this.name = Objects.requireNonNull(name, "name cannot be null").trim();
        this.path = path != null ? path.trim() : null;
        this.builtIn = builtIn;
        this.enabled = enabled;
    }

    public TextMateBundle(TextMateBundle other) {
        this.name = other.name;
        this.path = other.path;
        this.builtIn = other.builtIn;
        this.enabled = other.enabled;
    }

    public static TextMateBundle builtIn(String name) {
        return new TextMateBundle(name, null, true, true);
    }

    public static TextMateBundle custom(String name, String path) {
        return new TextMateBundle(name, path, false, true);
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TextMateBundle that)) return false;
        return builtIn == that.builtIn &&
                enabled == that.enabled &&
                Objects.equals(name, that.name) &&
                Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, path, builtIn, enabled);
    }

    @Override
    public String toString() {
        return name + (builtIn ? " [Built-in]" : " (" + path + ")") + (enabled ? "" : " [disabled]");
    }
}
