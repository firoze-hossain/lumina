package dev.lumina.php;

import java.util.Objects;

/**
 * Model representing a PHP runtime extension/module item in the Runtime extensions tree.
 */
public class PhpRuntimeExtension {

    private String name;
    private String category; // "Core", "Bundled", "External", "PECL", "Others"
    private boolean enabled;

    public PhpRuntimeExtension() {
    }

    public PhpRuntimeExtension(String name, String category, boolean enabled) {
        this.name = name;
        this.category = category;
        this.enabled = enabled;
    }

    public PhpRuntimeExtension copy() {
        return new PhpRuntimeExtension(name, category, enabled);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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
        if (!(o instanceof PhpRuntimeExtension that)) return false;
        return enabled == that.enabled &&
                Objects.equals(name, that.name) &&
                Objects.equals(category, that.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, category, enabled);
    }

    @Override
    public String toString() {
        return name;
    }
}
