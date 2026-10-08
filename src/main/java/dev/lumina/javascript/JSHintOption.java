package dev.lumina.javascript;

/**
 * Descriptor for a JSHint configurable option matching IntelliJ IDEA.
 */
public class JSHintOption {

    public enum Category {
        ENFORCING,
        RELAXING,
        ENVIRONMENT
    }

    private final String key;
    private final String label;
    private final Category category;
    private final boolean defaultBooleanValue;

    public JSHintOption(String key, String label, Category category, boolean defaultBooleanValue) {
        this.key = key;
        this.label = label;
        this.category = category;
        this.defaultBooleanValue = defaultBooleanValue;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isDefaultBooleanValue() {
        return defaultBooleanValue;
    }
}
