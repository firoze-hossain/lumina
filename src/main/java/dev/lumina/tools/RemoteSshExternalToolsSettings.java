package dev.lumina.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing Tools > Remote SSH External Tools configuration settings in Lumina IDE.
 */
public class RemoteSshExternalToolsSettings implements Cloneable {

    private List<RemoteSshExternalToolEntry> tools = new ArrayList<>();

    public RemoteSshExternalToolsSettings() {
    }

    public List<RemoteSshExternalToolEntry> getTools() {
        return tools;
    }

    public void setTools(List<RemoteSshExternalToolEntry> tools) {
        this.tools = tools != null ? new ArrayList<>(tools) : new ArrayList<>();
    }

    public RemoteSshExternalToolsSettings copy() {
        return clone();
    }

    @Override
    public RemoteSshExternalToolsSettings clone() {
        try {
            RemoteSshExternalToolsSettings copy = (RemoteSshExternalToolsSettings) super.clone();
            copy.tools = new ArrayList<>();
            for (RemoteSshExternalToolEntry t : this.tools) {
                copy.tools.add(t.clone());
            }
            return copy;
        } catch (CloneNotSupportedException e) {
            RemoteSshExternalToolsSettings copy = new RemoteSshExternalToolsSettings();
            copy.tools = new ArrayList<>();
            for (RemoteSshExternalToolEntry t : this.tools) {
                copy.tools.add(t.clone());
            }
            return copy;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RemoteSshExternalToolsSettings that = (RemoteSshExternalToolsSettings) o;
        return Objects.equals(tools, that.tools);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tools);
    }
}
