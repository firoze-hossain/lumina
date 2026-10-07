package dev.lumina.injections;

/**
 * Scope for language injections: Built-in (shipped with IDE/plugins),
 * IDE (user-wide configuration), or Project (committed in project configuration).
 */
public enum LanguageInjectionScope {
    BUILT_IN("Built-in"),
    IDE("IDE"),
    PROJECT("Project");

    private final String displayName;

    LanguageInjectionScope(String displayName) {
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
