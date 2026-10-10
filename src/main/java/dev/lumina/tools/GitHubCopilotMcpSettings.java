package dev.lumina.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing Tools > GitHub Copilot > Model Context Protocol (MCP) settings matching Image 3.
 */
public class GitHubCopilotMcpSettings implements Cloneable {

    public static final String DEFAULT_REGISTRY_URL = "https://api.mcp.github.com";

    private String mcpRegistryBaseUrl = DEFAULT_REGISTRY_URL;
    private List<String> allowedSamplingModels = new ArrayList<>(List.of("Claude 3.5 Sonnet", "GPT-4o", "Claude 3.7 Sonnet"));
    private boolean autoApproveSampling = false;
    private List<String> autoApprovedServers = new ArrayList<>();

    public GitHubCopilotMcpSettings() {
    }

    public GitHubCopilotMcpSettings(String mcpRegistryBaseUrl, List<String> allowedSamplingModels, boolean autoApproveSampling, List<String> autoApprovedServers) {
        this.mcpRegistryBaseUrl = mcpRegistryBaseUrl != null ? mcpRegistryBaseUrl : DEFAULT_REGISTRY_URL;
        if (allowedSamplingModels != null) {
            this.allowedSamplingModels = new ArrayList<>(allowedSamplingModels);
        }
        this.autoApproveSampling = autoApproveSampling;
        if (autoApprovedServers != null) {
            this.autoApprovedServers = new ArrayList<>(autoApprovedServers);
        }
    }

    public String getMcpRegistryBaseUrl() {
        return mcpRegistryBaseUrl;
    }

    public void setMcpRegistryBaseUrl(String mcpRegistryBaseUrl) {
        this.mcpRegistryBaseUrl = mcpRegistryBaseUrl != null ? mcpRegistryBaseUrl : DEFAULT_REGISTRY_URL;
    }

    public List<String> getAllowedSamplingModels() {
        return allowedSamplingModels;
    }

    public List<String> getAllowedModels() {
        return allowedSamplingModels;
    }

    public void setAllowedSamplingModels(List<String> allowedSamplingModels) {
        this.allowedSamplingModels = allowedSamplingModels != null ? new ArrayList<>(allowedSamplingModels) : new ArrayList<>();
    }

    public void setAllowedModels(List<String> allowedModels) {
        setAllowedSamplingModels(allowedModels);
    }

    public boolean isAutoApproveSampling() {
        return autoApproveSampling;
    }

    public void setAutoApproveSampling(boolean autoApproveSampling) {
        this.autoApproveSampling = autoApproveSampling;
    }

    public List<String> getAutoApprovedServers() {
        return autoApprovedServers;
    }

    public void setAutoApprovedServers(List<String> autoApprovedServers) {
        this.autoApprovedServers = autoApprovedServers != null ? new ArrayList<>(autoApprovedServers) : new ArrayList<>();
    }

    @Override
    public GitHubCopilotMcpSettings clone() {
        return new GitHubCopilotMcpSettings(mcpRegistryBaseUrl, allowedSamplingModels, autoApproveSampling, autoApprovedServers);
    }

    public GitHubCopilotMcpSettings copy() {
        return clone();
    }

    @Override
    public String toString() {
        return "GitHubCopilotMcpSettings{registry='" + mcpRegistryBaseUrl + "', autoApprove=" + autoApproveSampling + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GitHubCopilotMcpSettings that = (GitHubCopilotMcpSettings) o;
        return autoApproveSampling == that.autoApproveSampling &&
                Objects.equals(mcpRegistryBaseUrl, that.mcpRegistryBaseUrl) &&
                Objects.equals(allowedSamplingModels, that.allowedSamplingModels) &&
                Objects.equals(autoApprovedServers, that.autoApprovedServers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mcpRegistryBaseUrl, allowedSamplingModels, autoApproveSampling, autoApprovedServers);
    }
}
