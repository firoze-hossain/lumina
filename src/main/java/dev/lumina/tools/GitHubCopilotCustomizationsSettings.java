package dev.lumina.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model holding settings for Tools > GitHub Copilot > Customizations.
 */
public class GitHubCopilotCustomizationsSettings {

    private List<CustomizationLocationEntry> instructionLocations = new ArrayList<>();
    private boolean useAgentsMd = true;
    private boolean useNestedAgentsMd = false;
    private boolean useClaudeMd = true;
    private boolean useNestedClaudeMd = false;

    private List<CustomizationLocationEntry> promptLocations = new ArrayList<>();
    private List<CustomizationLocationEntry> agentLocations = new ArrayList<>();
    private List<String> pluginMarketplaces = new ArrayList<>();

    public GitHubCopilotCustomizationsSettings() {
        initDefaults();
    }

    public GitHubCopilotCustomizationsSettings(GitHubCopilotCustomizationsSettings other) {
        if (other != null) {
            this.instructionLocations = new ArrayList<>();
            for (CustomizationLocationEntry e : other.instructionLocations) {
                this.instructionLocations.add(e.clone());
            }
            this.useAgentsMd = other.useAgentsMd;
            this.useNestedAgentsMd = other.useNestedAgentsMd;
            this.useClaudeMd = other.useClaudeMd;
            this.useNestedClaudeMd = other.useNestedClaudeMd;

            this.promptLocations = new ArrayList<>();
            for (CustomizationLocationEntry e : other.promptLocations) {
                this.promptLocations.add(e.clone());
            }

            this.agentLocations = new ArrayList<>();
            for (CustomizationLocationEntry e : other.agentLocations) {
                this.agentLocations.add(e.clone());
            }

            this.pluginMarketplaces = new ArrayList<>(other.pluginMarketplaces);
        } else {
            initDefaults();
        }
    }

    private void initDefaults() {
        instructionLocations.add(new CustomizationLocationEntry(".github/instructions", true));
        instructionLocations.add(new CustomizationLocationEntry("~/.copilot/instructions", false));

        promptLocations.add(new CustomizationLocationEntry(".github/prompts", true));
        promptLocations.add(new CustomizationLocationEntry("~/.copilot/prompts", false));

        agentLocations.add(new CustomizationLocationEntry(".claude/agents", true));
        agentLocations.add(new CustomizationLocationEntry(".github/agents", true));
        agentLocations.add(new CustomizationLocationEntry("~/.copilot/agents", true));
    }

    public List<CustomizationLocationEntry> getInstructionLocations() {
        return instructionLocations;
    }

    public void setInstructionLocations(List<CustomizationLocationEntry> instructionLocations) {
        this.instructionLocations = instructionLocations != null ? instructionLocations : new ArrayList<>();
    }

    public boolean isUseAgentsMd() {
        return useAgentsMd;
    }

    public void setUseAgentsMd(boolean useAgentsMd) {
        this.useAgentsMd = useAgentsMd;
    }

    public boolean isUseNestedAgentsMd() {
        return useNestedAgentsMd;
    }

    public void setUseNestedAgentsMd(boolean useNestedAgentsMd) {
        this.useNestedAgentsMd = useNestedAgentsMd;
    }

    public boolean isUseClaudeMd() {
        return useClaudeMd;
    }

    public void setUseClaudeMd(boolean useClaudeMd) {
        this.useClaudeMd = useClaudeMd;
    }

    public boolean isUseNestedClaudeMd() {
        return useNestedClaudeMd;
    }

    public void setUseNestedClaudeMd(boolean useNestedClaudeMd) {
        this.useNestedClaudeMd = useNestedClaudeMd;
    }

    public List<CustomizationLocationEntry> getPromptLocations() {
        return promptLocations;
    }

    public void setPromptLocations(List<CustomizationLocationEntry> promptLocations) {
        this.promptLocations = promptLocations != null ? promptLocations : new ArrayList<>();
    }

    public List<CustomizationLocationEntry> getAgentLocations() {
        return agentLocations;
    }

    public void setAgentLocations(List<CustomizationLocationEntry> agentLocations) {
        this.agentLocations = agentLocations != null ? agentLocations : new ArrayList<>();
    }

    public List<String> getPluginMarketplaces() {
        return pluginMarketplaces;
    }

    public void setPluginMarketplaces(List<String> pluginMarketplaces) {
        this.pluginMarketplaces = pluginMarketplaces != null ? pluginMarketplaces : new ArrayList<>();
    }

    public GitHubCopilotCustomizationsSettings clone() {
        return new GitHubCopilotCustomizationsSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GitHubCopilotCustomizationsSettings that = (GitHubCopilotCustomizationsSettings) o;
        return useAgentsMd == that.useAgentsMd &&
                useNestedAgentsMd == that.useNestedAgentsMd &&
                useClaudeMd == that.useClaudeMd &&
                useNestedClaudeMd == that.useNestedClaudeMd &&
                Objects.equals(instructionLocations, that.instructionLocations) &&
                Objects.equals(promptLocations, that.promptLocations) &&
                Objects.equals(agentLocations, that.agentLocations) &&
                Objects.equals(pluginMarketplaces, that.pluginMarketplaces);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instructionLocations, useAgentsMd, useNestedAgentsMd, useClaudeMd, useNestedClaudeMd,
                promptLocations, agentLocations, pluginMarketplaces);
    }

    @Override
    public String toString() {
        return "GitHubCopilotCustomizationsSettings{" +
                "instructionLocations=" + instructionLocations +
                ", useAgentsMd=" + useAgentsMd +
                ", useNestedAgentsMd=" + useNestedAgentsMd +
                ", useClaudeMd=" + useClaudeMd +
                ", useNestedClaudeMd=" + useNestedClaudeMd +
                ", promptLocations=" + promptLocations +
                ", agentLocations=" + agentLocations +
                ", pluginMarketplaces=" + pluginMarketplaces +
                '}';
    }
}
