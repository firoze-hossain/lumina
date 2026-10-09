package dev.lumina.javascript;

import java.util.Arrays;
import java.util.List;

/**
 * JavaScript language versions in Lumina IDE.
 */
public enum JavaScriptLanguageVersion {
    ECMASCRIPT_5_1("ECMAScript 5.1", "Standard ECMAScript 5.1"),
    ECMASCRIPT_6_PLUS("ECMAScript 6+", "ECMAScript 2015+, some proposals and JSX"),
    FLOW("Flow", "Flow static type checker syntax"),
    JSX_HARMONY("JSX Harmony", "JSX with ECMAScript 6"),
    REACT_JSX("React JSX", "React JSX syntax");

    private final String displayName;
    private final String description;

    JavaScriptLanguageVersion(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static List<String> getAllDisplayNames() {
        return Arrays.stream(values())
                .map(JavaScriptLanguageVersion::getDisplayName)
                .toList();
    }

    public static JavaScriptLanguageVersion fromDisplayName(String name) {
        if (name == null) return ECMASCRIPT_6_PLUS;
        for (JavaScriptLanguageVersion v : values()) {
            if (v.displayName.equalsIgnoreCase(name)) {
                return v;
            }
        }
        return ECMASCRIPT_6_PLUS;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
