package dev.lumina.build;

import dev.lumina.util.Settings;

/**
 * Singleton manager responsible for loading and saving BSP configuration.
 */
public class BspSettingsManager {

    public static final String KEY_BSP_TRACE_LOG_ENABLED = "bsp.trace.log.enabled";

    private static volatile BspSettingsManager instance;
    private BspSettings currentSettings;

    private BspSettingsManager() {
        loadSettings();
    }

    public static BspSettingsManager getInstance() {
        if (instance == null) {
            synchronized (BspSettingsManager.class) {
                if (instance == null) {
                    instance = new BspSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized BspSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(BspSettings newSettings) {
        if (newSettings == null) return;
        this.currentSettings = newSettings.clone();
        saveSettings();
    }

    public synchronized void loadSettings() {
        BspSettings s = new BspSettings();
        String val = Settings.get(KEY_BSP_TRACE_LOG_ENABLED);
        if (val != null) s.setBspTraceLogEnabled(Boolean.parseBoolean(val));
        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.set(KEY_BSP_TRACE_LOG_ENABLED, String.valueOf(currentSettings.isBspTraceLogEnabled()));
    }
}
