package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager handling persistence and change listeners for Tools > MCP Server settings.
 */
public class McpServerSettingsManager {

    private static final McpServerSettingsManager INSTANCE = new McpServerSettingsManager();

    private static final String KEY_PREFIX = "tools.mcp_server.";
    private static final String KEY_ENABLE_MCP_SERVER = KEY_PREFIX + "enable_mcp_server";

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private McpServerSettingsManager() {
    }

    public static McpServerSettingsManager getInstance() {
        return INSTANCE;
    }

    public McpServerSettings load() {
        return getSettings();
    }

    public McpServerSettings getSettings() {
        McpServerSettings s = new McpServerSettings();

        String val = Settings.get(KEY_ENABLE_MCP_SERVER);
        if (val != null) {
            s.setEnableMcpServer(Boolean.parseBoolean(val));
        }

        return s;
    }

    public void setSettings(McpServerSettings s) {
        if (s == null) return;

        Settings.put(KEY_ENABLE_MCP_SERVER, String.valueOf(s.isEnableMcpServer()));
        notifyListeners();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeChangeListener(Runnable listener) {
        if (listener != null) listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            try {
                listener.run();
            } catch (Throwable ignored) {
            }
        }
    }
}
