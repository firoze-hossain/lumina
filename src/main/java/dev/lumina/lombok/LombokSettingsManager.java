package dev.lumina.lombok;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Lombok settings in Lumina IDE.
 */
public class LombokSettingsManager {

    public static final String KEY_LOMBOK_TRACK_AP_DEPS = "lombok.track.ap.dependencies";

    private static LombokSettingsManager instance;

    private LombokSettings currentSettings = new LombokSettings();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private LombokSettingsManager() {
        loadSettings();
    }

    public static synchronized LombokSettingsManager getInstance() {
        if (instance == null) {
            instance = new LombokSettingsManager();
        }
        return instance;
    }

    public synchronized LombokSettings getSettings() {
        return currentSettings.copy();
    }

    public synchronized void setSettings(LombokSettings settings) {
        this.currentSettings = settings != null ? settings.copy() : new LombokSettings();
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
        Settings.put(KEY_LOMBOK_TRACK_AP_DEPS, String.valueOf(currentSettings.isAutoAddTrackApDependencies()));
    }

    public synchronized void loadSettings() {
        String val = Settings.get(KEY_LOMBOK_TRACK_AP_DEPS);
        if (val != null) {
            this.currentSettings = new LombokSettings(Boolean.parseBoolean(val));
        } else {
            this.currentSettings = new LombokSettings(true);
        }
    }

    public synchronized void resetDefaults() {
        this.currentSettings = new LombokSettings(true);
        saveSettings();
        notifyListeners();
    }
}
