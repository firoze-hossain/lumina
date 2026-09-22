package dev.lumina.keymap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Model representing an action in the Keymap tree hierarchy.
 */
public class KeymapAction {

    private final String id;
    private final String name;
    private final List<String> categoryPath;
    private final String iconGlyph;
    private final String description;

    public KeymapAction(String id, String name, List<String> categoryPath) {
        this(id, name, categoryPath, null, null);
    }

    public KeymapAction(String id, String name, List<String> categoryPath, String iconGlyph) {
        this(id, name, categoryPath, iconGlyph, null);
    }

    public KeymapAction(String id, String name, List<String> categoryPath, String iconGlyph, String description) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.categoryPath = categoryPath != null ? List.copyOf(categoryPath) : List.of();
        this.iconGlyph = iconGlyph;
        this.description = description != null ? description : "";
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getCategoryPath() {
        return categoryPath;
    }

    public String getIconGlyph() {
        return iconGlyph;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Returns hierarchical subtitle path for dialogs (e.g. "Main Menu | Help").
     */
    public String getCategoryBreadcrumbs() {
        if (categoryPath.isEmpty()) return "";
        return String.join(" | ", categoryPath);
    }

    /**
     * Returns full path string (e.g. "Main Menu | Help | Find Action...").
     */
    public String getFullPath() {
        if (categoryPath.isEmpty()) return name;
        return getCategoryBreadcrumbs() + " | " + name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeymapAction that = (KeymapAction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name;
    }
}
