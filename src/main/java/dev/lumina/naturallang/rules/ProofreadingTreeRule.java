package dev.lumina.naturallang.rules;

import java.util.Objects;

/**
 * Represents an individual linguistic rule within the "Other rules" hierarchical tree.
 */
public class ProofreadingTreeRule {

    private final String id;
    private final String categoryName;
    private final String name;
    private final String description;
    private final boolean defaultEnabled;

    public ProofreadingTreeRule(String id, String categoryName, String name, String description, boolean defaultEnabled) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.categoryName = Objects.requireNonNull(categoryName, "categoryName cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.description = description != null ? description : "";
        this.defaultEnabled = defaultEnabled;
    }

    public ProofreadingTreeRule(String id, String categoryName, String name, boolean defaultEnabled) {
        this(id, categoryName, name, "", defaultEnabled);
    }

    public String getId() {
        return id;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDefaultEnabled() {
        return defaultEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProofreadingTreeRule that)) return false;
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
