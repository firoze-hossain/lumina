package dev.lumina.rbs;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Languages & Frameworks > RBS settings in Lumina IDE.
 */
public class RbsSettingsManager {

    public static final String KEY_RBS_IMPROVED_TYPE_SUPPORT = "rbs.improved.type.support.collection";

    private static RbsSettingsManager instance;

    private RbsSettings currentSettings = new RbsSettings();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private RbsSettingsManager() {
        loadSettings();
    }

    public static synchronized RbsSettingsManager getInstance() {
        if (instance == null) {
            instance = new RbsSettingsManager();
        }
        return instance;
    }

    public synchronized RbsSettings getSettings() {
        return currentSettings.copy();
    }

    public synchronized void setSettings(RbsSettings settings) {
        this.currentSettings = settings != null ? settings.copy() : new RbsSettings();
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
        Settings.put(KEY_RBS_IMPROVED_TYPE_SUPPORT, String.valueOf(currentSettings.isImprovedTypeSupportWithRbsCollection()));
    }

    public synchronized void loadSettings() {
        String val = Settings.get(KEY_RBS_IMPROVED_TYPE_SUPPORT);
        if (val != null) {
            this.currentSettings = new RbsSettings(Boolean.parseBoolean(val));
        } else {
            this.currentSettings = new RbsSettings(false);
        }
    }

    public synchronized void resetDefaults() {
        this.currentSettings = new RbsSettings(false);
        saveSettings();
        notifyListeners();
    }
}
