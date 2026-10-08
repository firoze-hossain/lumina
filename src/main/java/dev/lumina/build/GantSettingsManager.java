package dev.lumina.build;

import dev.lumina.util.Settings;

/**
 * Singleton manager responsible for loading and saving Gant configuration.
 */
public class GantSettingsManager {

    public static final String KEY_GANT_HOME = "gant.home";

    private static volatile GantSettingsManager instance;
    private GantSettings currentSettings;

    private GantSettingsManager() {
        loadSettings();
    }

    public static GantSettingsManager getInstance() {
        if (instance == null) {
            synchronized (GantSettingsManager.class) {
                if (instance == null) {
                    instance = new GantSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized GantSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(GantSettings newSettings) {
        if (newSettings == null) return;
        this.currentSettings = newSettings.clone();
        saveSettings();
    }

    public synchronized void loadSettings() {
        GantSettings s = new GantSettings();
        String home = Settings.get(KEY_GANT_HOME);
        if (home != null) s.setGantHome(home);
        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.set(KEY_GANT_HOME, currentSettings.getGantHome());
    }
}
