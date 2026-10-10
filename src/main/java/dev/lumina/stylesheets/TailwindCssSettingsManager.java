package dev.lumina.stylesheets;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Languages & Frameworks > Style Sheets > Tailwind CSS settings in Lumina IDE.
 */
public class TailwindCssSettingsManager {

    public static final String KEY_TAILWIND_SERVER = "stylesheets.tailwind.server";
    public static final String KEY_TAILWIND_VERSION = "stylesheets.tailwind.version";
    public static final String KEY_TAILWIND_CUSTOM_PATH = "stylesheets.tailwind.custom.path";
    public static final String KEY_TAILWIND_CONFIG_JSON = "stylesheets.tailwind.config.json";

    private static volatile TailwindCssSettingsManager instance;
    private TailwindCssSettings currentSettings;
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private TailwindCssSettingsManager() {
        loadSettings();
    }

    public static TailwindCssSettingsManager getInstance() {
        if (instance == null) {
            synchronized (TailwindCssSettingsManager.class) {
                if (instance == null) {
                    instance = new TailwindCssSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized TailwindCssSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(TailwindCssSettings settings) {
        if (settings == null) return;
        this.currentSettings = settings.clone();
        saveSettings();
        notifyListeners();
    }

    public synchronized void loadSettings() {
        TailwindCssSettings s = new TailwindCssSettings();

        String server = Settings.get(KEY_TAILWIND_SERVER);
        if (server != null && !server.isBlank()) {
            s.setLanguageServer(server);
        }

        String ver = Settings.get(KEY_TAILWIND_VERSION);
        if (ver != null && !ver.isBlank()) {
            s.setLanguageServerVersion(ver);
        }

        String path = Settings.get(KEY_TAILWIND_CUSTOM_PATH);
        if (path != null) {
            s.setCustomServerPath(path);
        }

        String json = Settings.get(KEY_TAILWIND_CONFIG_JSON);
        if (json != null && !json.isBlank()) {
            s.setConfigurationJson(json);
        }

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.put(KEY_TAILWIND_SERVER, currentSettings.getLanguageServer());
        Settings.put(KEY_TAILWIND_VERSION, currentSettings.getLanguageServerVersion());
        Settings.put(KEY_TAILWIND_CUSTOM_PATH, currentSettings.getCustomServerPath());
        Settings.put(KEY_TAILWIND_CONFIG_JSON, currentSettings.getConfigurationJson());
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable r : changeListeners) {
            try {
                r.run();
            } catch (Exception ignored) {}
        }
    }
}
