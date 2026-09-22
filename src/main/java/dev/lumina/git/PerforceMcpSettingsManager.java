package dev.lumina.git;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Manages Perforce MCP settings matching IntelliJ IDEA's "Version Control > Perforce > Perforce MCP".
 * Backed by Java Preferences for dynamic persistence.
 */
public class PerforceMcpSettingsManager {

    public enum McpEnvironmentMode {
        DO_NOT_USE,
        USE_PROJECT_SETTINGS,
        OVERRIDE_CUSTOM
    }

    private static final PerforceMcpSettingsManager INSTANCE = new PerforceMcpSettingsManager();

    private final Preferences prefs = Preferences.userNodeForPackage(PerforceMcpSettingsManager.class);
    private final List<Runnable> listeners = new ArrayList<>();

    // Executable
    private String mcpExecutablePath = "";

    // Settings
    private boolean readOnlyMode = true;
    private boolean allowAnonymousUsageStats = false;

    // Environment
    private McpEnvironmentMode environmentMode = McpEnvironmentMode.USE_PROJECT_SETTINGS;
    private String customServerPort = "";
    private String customUser = "";
    private String customClientWorkspace = "";

    // Toolset
    private boolean toolsetFiles = true;
    private boolean toolsetChangelists = true;
    private boolean toolsetShelves = true;
    private boolean toolsetWorkspaces = true;
    private boolean toolsetJobs = true;

    private PerforceMcpSettingsManager() {
        loadPreferences();
    }

    public static PerforceMcpSettingsManager getInstance() {
        return INSTANCE;
    }

    public synchronized String getMcpExecutablePath() { return mcpExecutablePath; }
    public synchronized void setMcpExecutablePath(String path) {
        this.mcpExecutablePath = path != null ? path.trim() : "";
        prefs.put("p4_mcp_executable", this.mcpExecutablePath);
        notifyListeners();
    }

    public synchronized boolean isReadOnlyMode() { return readOnlyMode; }
    public synchronized void setReadOnlyMode(boolean val) {
        if (this.readOnlyMode != val) {
            this.readOnlyMode = val;
            prefs.putBoolean("p4_mcp_read_only", val);
            notifyListeners();
        }
    }

    public synchronized boolean isAllowAnonymousUsageStats() { return allowAnonymousUsageStats; }
    public synchronized void setAllowAnonymousUsageStats(boolean val) {
        if (this.allowAnonymousUsageStats != val) {
            this.allowAnonymousUsageStats = val;
            prefs.putBoolean("p4_mcp_anon_stats", val);
            notifyListeners();
        }
    }

    public synchronized McpEnvironmentMode getEnvironmentMode() { return environmentMode; }
    public synchronized void setEnvironmentMode(McpEnvironmentMode mode) {
        if (mode != null && this.environmentMode != mode) {
            this.environmentMode = mode;
            prefs.put("p4_mcp_env_mode", mode.name());
            notifyListeners();
        }
    }

    public synchronized String getCustomServerPort() { return customServerPort; }
    public synchronized void setCustomServerPort(String val) {
        this.customServerPort = val != null ? val : "";
        prefs.put("p4_mcp_custom_server", this.customServerPort);
        notifyListeners();
    }

    public synchronized String getCustomUser() { return customUser; }
    public synchronized void setCustomUser(String val) {
        this.customUser = val != null ? val : "";
        prefs.put("p4_mcp_custom_user", this.customUser);
        notifyListeners();
    }

    public synchronized String getCustomClientWorkspace() { return customClientWorkspace; }
    public synchronized void setCustomClientWorkspace(String val) {
        this.customClientWorkspace = val != null ? val : "";
        prefs.put("p4_mcp_custom_workspace", this.customClientWorkspace);
        notifyListeners();
    }

    public synchronized boolean isAllToolsetsEnabled() {
        return toolsetFiles && toolsetChangelists && toolsetShelves && toolsetWorkspaces && toolsetJobs;
    }

    public synchronized void setAllToolsets(boolean enabled) {
        this.toolsetFiles = enabled;
        this.toolsetChangelists = enabled;
        this.toolsetShelves = enabled;
        this.toolsetWorkspaces = enabled;
        this.toolsetJobs = enabled;
        saveToolsets();
        notifyListeners();
    }

    public synchronized boolean isToolsetFiles() { return toolsetFiles; }
    public synchronized void setToolsetFiles(boolean val) {
        this.toolsetFiles = val;
        prefs.putBoolean("p4_mcp_tool_files", val);
        notifyListeners();
    }

    public synchronized boolean isToolsetChangelists() { return toolsetChangelists; }
    public synchronized void setToolsetChangelists(boolean val) {
        this.toolsetChangelists = val;
        prefs.putBoolean("p4_mcp_tool_changelists", val);
        notifyListeners();
    }

    public synchronized boolean isToolsetShelves() { return toolsetShelves; }
    public synchronized void setToolsetShelves(boolean val) {
        this.toolsetShelves = val;
        prefs.putBoolean("p4_mcp_tool_shelves", val);
        notifyListeners();
    }

    public synchronized boolean isToolsetWorkspaces() { return toolsetWorkspaces; }
    public synchronized void setToolsetWorkspaces(boolean val) {
        this.toolsetWorkspaces = val;
        prefs.putBoolean("p4_mcp_tool_workspaces", val);
        notifyListeners();
    }

    public synchronized boolean isToolsetJobs() { return toolsetJobs; }
    public synchronized void setToolsetJobs(boolean val) {
        this.toolsetJobs = val;
        prefs.putBoolean("p4_mcp_tool_jobs", val);
        notifyListeners();
    }

    private void saveToolsets() {
        prefs.putBoolean("p4_mcp_tool_files", toolsetFiles);
        prefs.putBoolean("p4_mcp_tool_changelists", toolsetChangelists);
        prefs.putBoolean("p4_mcp_tool_shelves", toolsetShelves);
        prefs.putBoolean("p4_mcp_tool_workspaces", toolsetWorkspaces);
        prefs.putBoolean("p4_mcp_tool_jobs", toolsetJobs);
    }

    public synchronized void revertToDefaults() {
        mcpExecutablePath = "";
        readOnlyMode = true;
        allowAnonymousUsageStats = false;
        environmentMode = McpEnvironmentMode.USE_PROJECT_SETTINGS;
        customServerPort = "";
        customUser = "";
        customClientWorkspace = "";
        toolsetFiles = true;
        toolsetChangelists = true;
        toolsetShelves = true;
        toolsetWorkspaces = true;
        toolsetJobs = true;
        savePreferences();
        notifyListeners();
    }

    public synchronized void addListener(Runnable listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable l : new ArrayList<>(listeners)) {
            try {
                l.run();
            } catch (Exception ignored) {}
        }
    }

    private void loadPreferences() {
        mcpExecutablePath = prefs.get("p4_mcp_executable", "");
        readOnlyMode = prefs.getBoolean("p4_mcp_read_only", true);
        allowAnonymousUsageStats = prefs.getBoolean("p4_mcp_anon_stats", false);
        try {
            environmentMode = McpEnvironmentMode.valueOf(prefs.get("p4_mcp_env_mode", McpEnvironmentMode.USE_PROJECT_SETTINGS.name()));
        } catch (Exception e) {
            environmentMode = McpEnvironmentMode.USE_PROJECT_SETTINGS;
        }
        customServerPort = prefs.get("p4_mcp_custom_server", "");
        customUser = prefs.get("p4_mcp_custom_user", "");
        customClientWorkspace = prefs.get("p4_mcp_custom_workspace", "");

        toolsetFiles = prefs.getBoolean("p4_mcp_tool_files", true);
        toolsetChangelists = prefs.getBoolean("p4_mcp_tool_changelists", true);
        toolsetShelves = prefs.getBoolean("p4_mcp_tool_shelves", true);
        toolsetWorkspaces = prefs.getBoolean("p4_mcp_tool_workspaces", true);
        toolsetJobs = prefs.getBoolean("p4_mcp_tool_jobs", true);
    }

    private void savePreferences() {
        prefs.put("p4_mcp_executable", mcpExecutablePath);
        prefs.putBoolean("p4_mcp_read_only", readOnlyMode);
        prefs.putBoolean("p4_mcp_anon_stats", allowAnonymousUsageStats);
        prefs.put("p4_mcp_env_mode", environmentMode.name());
        prefs.put("p4_mcp_custom_server", customServerPort);
        prefs.put("p4_mcp_custom_user", customUser);
        prefs.put("p4_mcp_custom_workspace", customClientWorkspace);
        saveToolsets();
    }
}
