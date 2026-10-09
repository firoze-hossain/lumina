package dev.lumina.ktor;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Ktor settings in Lumina IDE.
 */
public class KtorSettingsManager {

    public static final String KEY_CREATE_RUN_CONFIG = "ktor.create.run.configuration.automatically";

    private static KtorSettingsManager instance;

    private KtorSettings currentSettings = new KtorSettings();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private KtorSettingsManager() {
        loadSettings();
    }

    public static synchronized KtorSettingsManager getInstance() {
        if (instance == null) {
            instance = new KtorSettingsManager();
        }
        return instance;
    }

    public synchronized KtorSettings getSettings() {
        return currentSettings.copy();
    }

    public synchronized void setSettings(KtorSettings settings) {
        this.currentSettings = settings != null ? settings.copy() : new KtorSettings();
        saveSettings();
        notifyListeners();
    }

    public synchronized void addChangeListener(Runnable listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public synchronized void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable r : changeListeners) {
            try {
                r.run();
            } catch (Exception ignored) {}
        }
    }

    public synchronized void saveSettings() {
        Settings.put(KEY_CREATE_RUN_CONFIG, String.valueOf(currentSettings.isCreateRunConfigurationAutomatically()));
    }

    public synchronized void loadSettings() {
        String val = Settings.get(KEY_CREATE_RUN_CONFIG);
        if (val != null) {
            this.currentSettings = new KtorSettings(Boolean.parseBoolean(val));
        } else {
            this.currentSettings = new KtorSettings();
        }
    }

    public synchronized void resetDefaults() {
        this.currentSettings = new KtorSettings();
        saveSettings();
        notifyListeners();
    }
}
