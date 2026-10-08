package dev.lumina.todo;

/**
 * Supported icon types for TODO patterns in Lumina IDE.
 */
public enum TodoIconType {
    TODO("TODO"),
    FIXME("FIXME"),
    DEFAULT("Default"),
    CUSTOM("Custom");

    private final String displayName;

    TodoIconType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
