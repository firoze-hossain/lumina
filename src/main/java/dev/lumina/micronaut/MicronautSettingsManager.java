package dev.lumina.micronaut;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Micronaut settings in Lumina IDE.
 */
public class MicronautSettingsManager {

    public static final String KEY_MICRONAUT_CREATE_RUN_CONFIG = "micronaut.create.run.configuration.automatically";

    private static MicronautSettingsManager instance;

    private MicronautSettings currentSettings = new MicronautSettings();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private MicronautSettingsManager() {
        loadSettings();
    }

    public static synchronized MicronautSettingsManager getInstance() {
        if (instance == null) {
            instance = new MicronautSettingsManager();
        }
        return instance;
    }

    public synchronized MicronautSettings getSettings() {
        return currentSettings.copy();
    }

    public synchronized void setSettings(MicronautSettings settings) {
        this.currentSettings = settings != null ? settings.copy() : new MicronautSettings();
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
        Settings.put(KEY_MICRONAUT_CREATE_RUN_CONFIG, String.valueOf(currentSettings.isCreateRunConfigurationAutomatically()));
    }

    public synchronized void loadSettings() {
        String val = Settings.get(KEY_MICRONAUT_CREATE_RUN_CONFIG);
        if (val != null) {
            this.currentSettings = new MicronautSettings(Boolean.parseBoolean(val));
        } else {
            this.currentSettings = new MicronautSettings(true);
        }
    }

    public synchronized void resetDefaults() {
        this.currentSettings = new MicronautSettings(true);
        saveSettings();
        notifyListeners();
    }
}
