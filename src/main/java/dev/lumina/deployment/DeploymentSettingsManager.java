package dev.lumina.deployment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.lumina.util.Settings;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe singleton manager for Deployment configuration (Servers & Options) in Lumina IDE.
 * Dynamically persists configuration without hardcoding.
 */
public class DeploymentSettingsManager {

    public static final String KEY_DEPLOYMENT_SERVERS = "deployment.servers";
    public static final String KEY_DEPLOYMENT_DEFAULT_ID = "deployment.default.server.id";
    public static final String KEY_DEPLOYMENT_OPTIONS = "deployment.options";

    private static DeploymentSettingsManager instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final List<DeploymentServer> servers = new ArrayList<>();
    private String defaultServerId = null;
    private DeploymentOptions options = new DeploymentOptions();

    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private DeploymentSettingsManager() {
        loadSettings();
    }

    public static synchronized DeploymentSettingsManager getInstance() {
        if (instance == null) {
            instance = new DeploymentSettingsManager();
        }
        return instance;
    }

    // ============================================================
    // Server Operations
    // ============================================================

    public synchronized List<DeploymentServer> getServers() {
        List<DeploymentServer> copy = new ArrayList<>();
        for (DeploymentServer s : servers) {
            copy.add(s.clone());
        }
        return copy;
    }

    public synchronized void setServers(List<DeploymentServer> newServers) {
        this.servers.clear();
        if (newServers != null) {
            for (DeploymentServer s : newServers) {
                this.servers.add(s.clone());
            }
        }
        saveSettings();
        notifyChanged();
    }

    public synchronized void addServer(DeploymentServer server) {
        if (server != null) {
            servers.add(server.clone());
            if (servers.size() == 1) {
                setDefaultServerId(server.getId());
            }
            saveSettings();
            notifyChanged();
        }
    }

    public synchronized void removeServer(String serverId) {
        if (serverId != null) {
            servers.removeIf(s -> serverId.equals(s.getId()));
            if (serverId.equals(defaultServerId)) {
                defaultServerId = servers.isEmpty() ? null : servers.get(0).getId();
                for (DeploymentServer s : servers) {
                    s.setDefaultServer(Objects.equals(s.getId(), defaultServerId));
                }
            }
            saveSettings();
            notifyChanged();
        }
    }

    public synchronized DeploymentServer getServerById(String serverId) {
        if (serverId == null) return null;
        for (DeploymentServer s : servers) {
            if (serverId.equals(s.getId())) {
                return s.clone();
            }
        }
        return null;
    }

    public synchronized String getDefaultServerId() {
        return defaultServerId;
    }

    public synchronized void setDefaultServerId(String defaultServerId) {
        this.defaultServerId = defaultServerId;
        for (DeploymentServer s : servers) {
            s.setDefaultServer(Objects.equals(s.getId(), defaultServerId));
        }
        saveSettings();
        notifyChanged();
    }

    public synchronized DeploymentServer getDefaultServer() {
        if (defaultServerId != null) {
            DeploymentServer server = getServerById(defaultServerId);
            if (server != null) return server;
        }
        return servers.isEmpty() ? null : servers.get(0).clone();
    }

    // ============================================================
    // Options
    // ============================================================

    public synchronized DeploymentOptions getOptions() {
        return options.clone();
    }

    public synchronized void setOptions(DeploymentOptions options) {
        if (options != null) {
            this.options = options.clone();
            saveSettings();
            notifyChanged();
        }
    }

    // ============================================================
    // Persistence
    // ============================================================

    public synchronized void saveSettings() {
        try {
            Settings.put(KEY_DEPLOYMENT_SERVERS, GSON.toJson(servers));
            Settings.put(KEY_DEPLOYMENT_DEFAULT_ID, defaultServerId != null ? defaultServerId : "");
            Settings.put(KEY_DEPLOYMENT_OPTIONS, GSON.toJson(options));
        } catch (Exception e) {
            System.err.println("[DeploymentSettingsManager] Error saving settings: " + e.getMessage());
        }
    }

    public synchronized void loadSettings() {
        try {
            String serversJson = Settings.get(KEY_DEPLOYMENT_SERVERS);
            if (serversJson != null && !serversJson.isBlank()) {
                Type listType = new TypeToken<List<DeploymentServer>>() {}.getType();
                List<DeploymentServer> loaded = GSON.fromJson(serversJson, listType);
                if (loaded != null) {
                    this.servers.clear();
                    this.servers.addAll(loaded);
                }
            }

            String defId = Settings.get(KEY_DEPLOYMENT_DEFAULT_ID);
            if (defId != null && !defId.isBlank()) {
                this.defaultServerId = defId;
            } else {
                this.defaultServerId = null;
            }

            String optionsJson = Settings.get(KEY_DEPLOYMENT_OPTIONS);
            if (optionsJson != null && !optionsJson.isBlank()) {
                DeploymentOptions loaded = GSON.fromJson(optionsJson, DeploymentOptions.class);
                if (loaded != null) {
                    this.options = loaded;
                }
            }
        } catch (Exception e) {
            System.err.println("[DeploymentSettingsManager] Error loading settings: " + e.getMessage());
        }
    }

    public synchronized void resetToDefaults() {
        this.servers.clear();
        this.defaultServerId = null;
        this.options = new DeploymentOptions();
        saveSettings();
        notifyChanged();
    }

    // ============================================================
    // Change Listeners
    // ============================================================

    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        if (listener != null) {
            changeListeners.remove(listener);
        }
    }

    private void notifyChanged() {
        for (Runnable r : changeListeners) {
            try {
                r.run();
            } catch (Exception ignored) {
            }
        }
    }
}
