package dev.lumina.php;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Supported PHP language levels and key language features for Lumina IDE.
 * Dynamically managed and formatted with version and feature descriptions.
 */
public enum PhpLanguageLevel {
    PHP_8_4("8.4", "8.4 (property hooks, asymmetric visibility, new without parentheses)"),
    PHP_8_3("8.3", "8.3 (typed class constants, json_validate, dynamic class constant fetch)"),
    PHP_8_2("8.2", "8.2 (readonly classes, null/false/true standalone types, DNF types)"),
    PHP_8_1("8.1", "8.1 (enums, readonly properties, first-class callables, fibers)"),
    PHP_8_0("8.0", "8.0 (attributes, union types, match expression, constructor property promotion)"),
    PHP_7_4("7.4", "7.4 (typed properties, arrow functions, null coalescing assignment)"),
    PHP_7_3("7.3", "7.3 (references in list assignments, flexible heredocs)"),
    PHP_7_2("7.2", "7.2 (object type hint, abstract function override)"),
    PHP_7_1("7.1", "7.1 (const visibility, nullables, multiple exceptions)"),
    PHP_7_0("7.0", "7.0 (return types, scalar type hints)"),
    PHP_5_6("5.6", "5.6 (variadic functions, argument unpacking)"),
    PHP_5_5("5.5", "5.5 (finally, generators)"),
    PHP_5_4("5.4", "5.4 (traits, short array syntax)"),
    PHP_5_3("5.3", "5.3 (namespaces, closures)");

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

    public String getFeaturesDescription() {
        int idx = displayName.indexOf('(');
        if (idx >= 0) {
            return displayName.substring(idx);
        }
        return "";
    }

    public static List<String> getAllDisplayNames() {
        List<String> list = new ArrayList<>();
        for (PhpLanguageLevel level : values()) {
            list.add(level.getDisplayName());
        }
        return list;
    }

    public static PhpLanguageLevel fromDisplayName(String name) {
        if (name == null || name.isBlank()) return PHP_5_6;
        String trimmed = name.trim();
        for (PhpLanguageLevel level : values()) {
            if (level.displayName.equalsIgnoreCase(trimmed) || level.version.equalsIgnoreCase(trimmed)) {
                return level;
            }
            if (trimmed.startsWith(level.version + " ") || trimmed.startsWith(level.version + "(")) {
                return level;
            }
        }
        return PHP_5_6;
    }

    public static PhpLanguageLevel fromVersion(String ver) {
        if (ver == null || ver.isBlank()) return PHP_5_6;
        String trimmed = ver.trim();
        for (PhpLanguageLevel level : values()) {
            if (level.version.equalsIgnoreCase(trimmed)) {
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
