package dev.lumina.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing Tools > Diff & Merge > External Diff Tools settings in Lumina IDE.
 */
public class ExternalDiffToolsSettings implements Cloneable {

    private boolean enableExternalTools = false;
    private List<ExternalDiffToolDefinition> configuredTools = new ArrayList<>();
    private List<ExternalDiffToolAssociation> associations = new ArrayList<>();

    public ExternalDiffToolsSettings() {
        initDefaults();
    }

    private void initDefaults() {
        configuredTools.clear();
        associations.clear();
        associations.add(new ExternalDiffToolAssociation("Default", "Built-in", "Built-in"));
    }

    public boolean isEnableExternalTools() {
        return enableExternalTools;
    }

    public void setEnableExternalTools(boolean enableExternalTools) {
        this.enableExternalTools = enableExternalTools;
    }

    public List<ExternalDiffToolDefinition> getConfiguredTools() {
        return configuredTools;
    }

    public void setConfiguredTools(List<ExternalDiffToolDefinition> configuredTools) {
        this.configuredTools = configuredTools != null ? new ArrayList<>(configuredTools) : new ArrayList<>();
    }

    public List<ExternalDiffToolAssociation> getAssociations() {
        return associations;
    }

    public void setAssociations(List<ExternalDiffToolAssociation> associations) {
        this.associations = associations != null ? new ArrayList<>(associations) : new ArrayList<>();
    }

    @Override
    public ExternalDiffToolsSettings clone() {
        try {
            ExternalDiffToolsSettings copy = (ExternalDiffToolsSettings) super.clone();
            copy.configuredTools = new ArrayList<>();
            for (ExternalDiffToolDefinition t : this.configuredTools) {
                copy.configuredTools.add(t.clone());
            }
            copy.associations = new ArrayList<>();
            for (ExternalDiffToolAssociation a : this.associations) {
                copy.associations.add(a.clone());
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
        ExternalDiffToolsSettings that = (ExternalDiffToolsSettings) o;
        return enableExternalTools == that.enableExternalTools &&
                Objects.equals(configuredTools, that.configuredTools) &&
                Objects.equals(associations, that.associations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enableExternalTools, configuredTools, associations);
    }
}
