package dev.lumina.templates;

/**
 * Categories for File and Code Templates matching IntelliJ IDEA.
 */
public enum FileTemplateCategory {
    FILES("Files"),
    INCLUDES("Includes"),
    CODE("Code"),
    OTHER("Other");

    private final String displayName;

    FileTemplateCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
