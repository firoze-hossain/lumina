package dev.lumina.readermode;

import java.util.Objects;

/**
 * Represents a configurable option in Reader Mode.
 * Enables dynamic extension without hardcoding settings items.
 */
public record ReaderModeOption(
        String id,
        String title,
        String subtitle,
        boolean defaultValue,
        boolean isFormattingScheme
) {
    public ReaderModeOption {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(title, "title cannot be null");
    }

    public static ReaderModeOption of(String id, String title, boolean defaultValue) {
        return new ReaderModeOption(id, title, null, defaultValue, false);
    }

    public static ReaderModeOption of(String id, String title, String subtitle, boolean defaultValue) {
        return new ReaderModeOption(id, title, subtitle, defaultValue, false);
    }
}
