package dev.lumina.typescript;

import dev.lumina.util.Settings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Languages & Frameworks > TypeScript > Vue in Lumina IDE.
 * Dynamically discovers Vue language servers and manages service configuration.
 */
public class VueServiceSettingsManager {

    public static final String KEY_SERVER_PACKAGE = "typescript.vue.server_package";
    public static final String KEY_MODE = "typescript.vue.mode";
    public static final String KEY_ENABLE_TYPE_ENGINE = "typescript.vue.enable_service_powered_type_engine";
    public static final String KEY_LS3_PREVIEW = "typescript.vue.ls3_preview";

    private static volatile VueServiceSettingsManager instance;
    private VueServiceSettings currentSettings;
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private VueServiceSettingsManager() {
        loadSettings();
    }

    public static VueServiceSettingsManager getInstance() {
        if (instance == null) {
            synchronized (VueServiceSettingsManager.class) {
                if (instance == null) {
                    instance = new VueServiceSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized VueServiceSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(VueServiceSettings settings) {
        if (settings == null) return;
        this.currentSettings = settings.clone();
        saveSettings();
        notifyListeners();
    }

    public synchronized void loadSettings() {
        VueServiceSettings s = new VueServiceSettings();

        String server = Settings.get(KEY_SERVER_PACKAGE);
        if (server != null && !server.isBlank()) {
            s.setServerPackage(server);
        }

        String mode = Settings.get(KEY_MODE);
        if (mode != null && !mode.isBlank()) {
            s.setMode(mode);
        }

        String engine = Settings.get(KEY_ENABLE_TYPE_ENGINE);
        if (engine != null) {
            s.setEnableServicePoweredTypeEngine(Boolean.parseBoolean(engine));
        }

        String preview = Settings.get(KEY_LS3_PREVIEW);
        if (preview != null) {
            s.setVueLs3Preview(Boolean.parseBoolean(preview));
        }

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.put(KEY_SERVER_PACKAGE, currentSettings.getServerPackage());
        Settings.put(KEY_MODE, currentSettings.getMode());
        Settings.put(KEY_ENABLE_TYPE_ENGINE, String.valueOf(currentSettings.isEnableServicePoweredTypeEngine()));
        Settings.put(KEY_LS3_PREVIEW, String.valueOf(currentSettings.isVueLs3Preview()));
    }

    public List<String> discoverVueServers() {
        List<String> list = new ArrayList<>();
        list.add(VueServiceSettings.DEFAULT_SERVER);
        list.add("Detect from node_modules");
        list.add("Custom...");
        return list;
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : changeListeners) {
            try {
                listener.run();
            } catch (Throwable ignored) {}
        }
    }
}
