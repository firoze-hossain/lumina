package dev.lumina.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings model for Tools > External Tools in Lumina IDE.
 */
public class ExternalToolsSettings implements Cloneable {

    private List<ExternalToolItem> tools = new ArrayList<>();

    public ExternalToolsSettings() {
    }

    public List<ExternalToolItem> getTools() {
        return tools;
    }

    public void setTools(List<ExternalToolItem> tools) {
        this.tools = tools != null ? new ArrayList<>(tools) : new ArrayList<>();
    }

    @Override
    public ExternalToolsSettings clone() {
        try {
            ExternalToolsSettings copy = (ExternalToolsSettings) super.clone();
            copy.tools = new ArrayList<>();
            for (ExternalToolItem item : this.tools) {
                copy.tools.add(item.clone());
            }
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExternalToolsSettings that = (ExternalToolsSettings) o;
        return Objects.equals(tools, that.tools);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tools);
    }
}
