package dev.lumina.php;

import java.util.Arrays;
import java.util.List;

/**
 * Supported PHP language levels matching IntelliJ IDEA / PhpStorm.
 */
public enum PhpLanguageLevel {
    PHP_5_3("5.3", "5.3"),
    PHP_5_4("5.4", "5.4"),
    PHP_5_5("5.5", "5.5"),
    PHP_5_6("5.6", "5.6 (variadic functions, argument unpacking)"),
    PHP_7_0("7.0", "7.0 (return types, scalar type hints)"),
    PHP_7_1("7.1", "7.1 (nullable types, void return type)"),
    PHP_7_2("7.2", "7.2"),
    PHP_7_3("7.3", "7.3"),
    PHP_7_4("7.4", "7.4 (typed properties)"),
    PHP_8_0("8.0", "8.0 (attributes, union types)"),
    PHP_8_1("8.1", "8.1 (enums, readonly properties, fibers)"),
    PHP_8_2("8.2", "8.2 (readonly classes, null/false/true types)"),
    PHP_8_3("8.3", "8.3 (typed class constants, dynamic class constant fetch)"),
    PHP_8_4("8.4", "8.4 (property hooks, asymmetric visibility)");

    private final String version;
    private final String displayName;

    PhpLanguageLevel(String version, String displayName) {
        this.version = version;
        this.displayName = displayName;
    }

    public String getVersion() {
        return version;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static List<String> getAllDisplayNames() {
        return Arrays.stream(values())
                .map(PhpLanguageLevel::getDisplayName)
                .toList();
    }

    public static PhpLanguageLevel fromDisplayName(String name) {
        if (name == null) return PHP_5_6;
        for (PhpLanguageLevel level : values()) {
            if (level.displayName.equalsIgnoreCase(name) || level.version.equalsIgnoreCase(name)) {
                return level;
            }
        }
        return PHP_5_6;
    }

    public static PhpLanguageLevel fromVersion(String ver) {
        if (ver == null) return PHP_5_6;
        for (PhpLanguageLevel level : values()) {
            if (level.version.equalsIgnoreCase(ver)) {
                return level;
            }
        }
        return PHP_5_6;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
