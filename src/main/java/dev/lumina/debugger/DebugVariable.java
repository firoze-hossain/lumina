package dev.lumina.debugger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a variable (this, parameter, local, or field) in the variables tree.
 * Example: request = {LoginRequest@19222} "LoginRequest(email=superadmin@..., password=...)"
 */
public final class DebugVariable {
    private final String name;
    private final String typeName;
    private final String valueString;
    private final String kind; // "this", "param", "local", "field", "watch"
    private final List<DebugVariable> children;

    public DebugVariable(String name, String typeName, String valueString, String kind) {
        this(name, typeName, valueString, kind, Collections.emptyList());
    }

    public DebugVariable(String name, String typeName, String valueString, String kind, List<DebugVariable> children) {
        this.name = name;
        this.typeName = typeName;
        this.valueString = valueString;
        this.kind = kind;
        this.children = children != null ? new ArrayList<>(children) : new ArrayList<>();
    }

    public String name() { return name; }
    public String typeName() { return typeName; }
    public String valueString() { return valueString; }
    public String kind() { return kind; }
    public List<DebugVariable> children() { return Collections.unmodifiableList(children); }

    public void addChild(DebugVariable child) {
        this.children.add(child);
    }

    public String displayText() {
        if ("this".equals(kind)) {
            return name + " = {" + typeName + "}";
        }
        if (valueString != null && !valueString.isBlank()) {
            return name + " = " + valueString;
        }
        return name + " = {" + typeName + "}";
    }

    @Override
    public String toString() {
        return displayText();
    }
}
