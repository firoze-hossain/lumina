package dev.lumina.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing Tools > MCP Server configuration settings in Lumina IDE.
 * Allows external AI clients (e.g. Claude App, Claude Code) to integrate IDE features via Model Context Protocol.
 */
public class McpServerSettings implements Cloneable {

    private boolean enableMcpServer = false;
    private List<String> detectedClients = new ArrayList<>();

    public McpServerSettings() {
        initDefaults();
    }

    private void initDefaults() {
        detectedClients.clear();
        detectedClients.add("Claude App");
        detectedClients.add("Claude Code");
    }

    public boolean isEnableMcpServer() {
        return enableMcpServer;
    }

    public void setEnableMcpServer(boolean enableMcpServer) {
        this.enableMcpServer = enableMcpServer;
    }

    public List<String> getDetectedClients() {
        return detectedClients;
    }

    public void setDetectedClients(List<String> detectedClients) {
        this.detectedClients = detectedClients != null ? new ArrayList<>(detectedClients) : new ArrayList<>();
    }

    public McpServerSettings copy() {
        return clone();
    }

    @Override
    public McpServerSettings clone() {
        try {
            McpServerSettings copy = (McpServerSettings) super.clone();
            copy.detectedClients = new ArrayList<>(this.detectedClients);
            return copy;
        } catch (CloneNotSupportedException e) {
            McpServerSettings copy = new McpServerSettings();
            copy.enableMcpServer = this.enableMcpServer;
            copy.detectedClients = new ArrayList<>(this.detectedClients);
            return copy;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        McpServerSettings that = (McpServerSettings) o;
        return enableMcpServer == that.enableMcpServer &&
                Objects.equals(detectedClients, that.detectedClients);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enableMcpServer, detectedClients);
    }
}
