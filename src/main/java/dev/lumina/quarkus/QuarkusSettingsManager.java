package dev.lumina.quarkus;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Languages & Frameworks > Quarkus settings in Lumina IDE.
 */
public class QuarkusSettingsManager {

    public static final String KEY_QUARKUS_CREATE_RUN_CONFIG = "quarkus.create.run.configuration.automatically";

    private static QuarkusSettingsManager instance;

    private QuarkusSettings currentSettings = new QuarkusSettings();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private QuarkusSettingsManager() {
        loadSettings();
    }

    public static synchronized QuarkusSettingsManager getInstance() {
        if (instance == null) {
            instance = new QuarkusSettingsManager();
        }
        return instance;
    }

    public synchronized QuarkusSettings getSettings() {
        return currentSettings.copy();
    }

    public synchronized void setSettings(QuarkusSettings settings) {
        this.currentSettings = settings != null ? settings.copy() : new QuarkusSettings();
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
        Settings.put(KEY_QUARKUS_CREATE_RUN_CONFIG, String.valueOf(currentSettings.isCreateRunConfigurationAutomatically()));
    }

    public synchronized void loadSettings() {
        String val = Settings.get(KEY_QUARKUS_CREATE_RUN_CONFIG);
        if (val != null) {
            this.currentSettings = new QuarkusSettings(Boolean.parseBoolean(val));
        } else {
            this.currentSettings = new QuarkusSettings(true);
        }
    }

    public synchronized void resetDefaults() {
        this.currentSettings = new QuarkusSettings(true);
        saveSettings();
        notifyListeners();
    }
}
