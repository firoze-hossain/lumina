package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing an external diff or merge tool definition in Lumina IDE.
 */
public class ExternalDiffToolDefinition implements Cloneable {

    private String name;
    private String programPath;
    private String arguments;

    public ExternalDiffToolDefinition() {
        this("", "", "");
    }

    public ExternalDiffToolDefinition(String name, String programPath, String arguments) {
        this.name = name != null ? name : "";
        this.programPath = programPath != null ? programPath : "";
        this.arguments = arguments != null ? arguments : "";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public String getProgramPath() {
        return programPath;
    }

    public void setProgramPath(String programPath) {
        this.programPath = programPath != null ? programPath : "";
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments != null ? arguments : "";
    }

    @Override
    public ExternalDiffToolDefinition clone() {
        try {
            return (ExternalDiffToolDefinition) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExternalDiffToolDefinition that = (ExternalDiffToolDefinition) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(programPath, that.programPath) &&
                Objects.equals(arguments, that.arguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, programPath, arguments);
    }
}
