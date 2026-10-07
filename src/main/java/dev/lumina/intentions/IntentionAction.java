package dev.lumina.intentions;

import java.util.Objects;

/**
 * Model representing a code intention action.
 * Holds category hierarchy, description, plugin attribution, and Before/After code samples.
 */
public class IntentionAction {
    private final String id;
    private final String name;
    private final String category;
    private final String subcategory;
    private String description;
    private String pluginName; // e.g. "Ktor plugin"
    private String beforeTemplate;
    private String afterTemplate;
    private boolean enabled = true;
    private final boolean defaultEnabled;

    public IntentionAction(String id, String name, String category, String subcategory) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.subcategory = subcategory;
        this.defaultEnabled = true;
    }

    public IntentionAction(String id, String name, String category, String subcategory,
                           String description, String beforeTemplate, String afterTemplate) {
        this(id, name, category, subcategory);
        this.description = description;
        this.beforeTemplate = beforeTemplate;
        this.afterTemplate = afterTemplate;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getSubcategory() { return subcategory; }
    public boolean hasSubcategory() { return subcategory != null && !subcategory.isBlank(); }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPluginName() { return pluginName; }
    public void setPluginName(String pluginName) { this.pluginName = pluginName; }

    public String getBeforeTemplate() { return beforeTemplate; }
    public void setBeforeTemplate(String beforeTemplate) { this.beforeTemplate = beforeTemplate; }

    public String getAfterTemplate() { return afterTemplate; }
    public void setAfterTemplate(String afterTemplate) { this.afterTemplate = afterTemplate; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isDefaultEnabled() { return defaultEnabled; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IntentionAction that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
