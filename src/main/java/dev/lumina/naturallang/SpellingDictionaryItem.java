package dev.lumina.naturallang;

import java.util.Objects;

/**
 * Represents a dictionary entry in the Spelling settings (e.g. Application-level,
 * Project-level, or custom plain text / hunspell dictionary paths).
 */
public record SpellingDictionaryItem(String name, String path, boolean isBuiltIn) {

    public SpellingDictionaryItem {
        Objects.requireNonNull(name, "name cannot be null");
    }

    public static SpellingDictionaryItem builtIn(String name) {
        return new SpellingDictionaryItem(name, null, true);
    }

    public static SpellingDictionaryItem custom(String path) {
        return new SpellingDictionaryItem(path, path, false);
    }

    @Override
    public String toString() {
        return isBuiltIn ? name + " [built-in]" : path;
    }
}
