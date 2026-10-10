package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manager handling persistence and change notifications for Tools > GitHub Copilot > Model Context Protocol (MCP) settings.
 */
public class GitHubCopilotMcpSettingsManager {

    private static final GitHubCopilotMcpSettingsManager INSTANCE = new GitHubCopilotMcpSettingsManager();

    private static final String KEY_REGISTRY_URL = "tools.copilot.mcp.registry_url";
    private static final String KEY_AUTO_APPROVE = "tools.copilot.mcp.auto_approve";
    private static final String KEY_MODELS_COUNT = "tools.copilot.mcp.models.count";
    private static final String KEY_MODELS_PREFIX = "tools.copilot.mcp.models.";
    private static final String KEY_SERVERS_COUNT = "tools.copilot.mcp.servers.count";
    private static final String KEY_SERVERS_PREFIX = "tools.copilot.mcp.servers.";

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private GitHubCopilotMcpSettingsManager() {
    }

    public static GitHubCopilotMcpSettingsManager getInstance() {
        return INSTANCE;
    }

    public GitHubCopilotMcpSettings load() {
        return getSettings();
    }

    public GitHubCopilotMcpSettings getSettings() {
        GitHubCopilotMcpSettings s = new GitHubCopilotMcpSettings();

        String reg = Settings.get(KEY_REGISTRY_URL);
        if (reg != null) s.setMcpRegistryBaseUrl(reg);

        String aa = Settings.get(KEY_AUTO_APPROVE);
        if (aa != null) s.setAutoApproveSampling(Boolean.parseBoolean(aa));

        String mcStr = Settings.get(KEY_MODELS_COUNT);
        if (mcStr != null) {
            try {
                int mc = Integer.parseInt(mcStr);
                List<String> models = new ArrayList<>();
                for (int i = 0; i < mc; i++) {
                    String m = Settings.get(KEY_MODELS_PREFIX + i);
                    if (m != null) models.add(m);
                }
                s.setAllowedSamplingModels(models);
            } catch (Exception ignored) {
            }
        }

        String scStr = Settings.get(KEY_SERVERS_COUNT);
        if (scStr != null) {
            try {
                int sc = Integer.parseInt(scStr);
                List<String> servers = new ArrayList<>();
                for (int i = 0; i < sc; i++) {
                    String sv = Settings.get(KEY_SERVERS_PREFIX + i);
                    if (sv != null) servers.add(sv);
                }
                s.setAutoApprovedServers(servers);
            } catch (Exception ignored) {
            }
        }

        return s;
    }

    public void setSettings(GitHubCopilotMcpSettings s) {
        if (s == null) return;
        Settings.put(KEY_REGISTRY_URL, s.getMcpRegistryBaseUrl());
        Settings.put(KEY_AUTO_APPROVE, String.valueOf(s.isAutoApproveSampling()));

        List<String> models = s.getAllowedSamplingModels();
        Settings.put(KEY_MODELS_COUNT, String.valueOf(models.size()));
        for (int i = 0; i < models.size(); i++) {
            Settings.put(KEY_MODELS_PREFIX + i, models.get(i));
        }

        List<String> servers = s.getAutoApprovedServers();
        Settings.put(KEY_SERVERS_COUNT, String.valueOf(servers.size()));
        for (int i = 0; i < servers.size(); i++) {
            Settings.put(KEY_SERVERS_PREFIX + i, servers.get(i));
        }

        notifyListeners();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    public void addListener(Runnable listener) {
        addChangeListener(listener);
    }

    public void removeListener(Runnable listener) {
        removeChangeListener(listener);
    }

    public void clear() {
        Settings.put(KEY_REGISTRY_URL, null);
        Settings.put(KEY_AUTO_APPROVE, null);

        String mcStr = Settings.get(KEY_MODELS_COUNT);
        if (mcStr != null) {
            try {
                int mc = Integer.parseInt(mcStr);
                for (int i = 0; i < mc; i++) Settings.put(KEY_MODELS_PREFIX + i, null);
            } catch (Exception ignored) {
            }
            Settings.put(KEY_MODELS_COUNT, null);
        }

        String scStr = Settings.get(KEY_SERVERS_COUNT);
        if (scStr != null) {
            try {
                int sc = Integer.parseInt(scStr);
                for (int i = 0; i < sc; i++) Settings.put(KEY_SERVERS_PREFIX + i, null);
            } catch (Exception ignored) {
            }
            Settings.put(KEY_SERVERS_COUNT, null);
        }

        notifyListeners();
    }

    public void resetDefaults() {
        setSettings(new GitHubCopilotMcpSettings());
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
