package dev.lumina.build;

import dev.lumina.util.Settings;

/**
 * Service managing Build Tools configuration in Lumina IDE.
 */
public class BuildToolsManager {

    public static final String KEY_SYNC_ENABLED = "build.tools.sync.enabled";
    public static final String KEY_SYNC_TRIGGER = "build.tools.sync.trigger";

    private static BuildToolsManager instance;

    private BuildToolsSettings settings = new BuildToolsSettings();

    private BuildToolsManager() {
        loadSettings();
    }

    public static synchronized BuildToolsManager getInstance() {
        if (instance == null) {
            instance = new BuildToolsManager();
        }
        return instance;
    }

    public BuildToolsSettings getSettings() {
        return settings.clone();
    }

    public void setSettings(BuildToolsSettings newSettings) {
        if (newSettings == null) return;
        this.settings = newSettings.clone();
        saveSettings();
    }

    public void loadSettings() {
        String enabled = Settings.get(KEY_SYNC_ENABLED);
        if (enabled != null) {
            settings.setSyncOnBuildScriptChanges(Boolean.parseBoolean(enabled));
        }

        String trigger = Settings.get(KEY_SYNC_TRIGGER);
        if (trigger != null) {
            try {
                settings.setSyncTrigger(BuildToolsSettings.SyncTrigger.valueOf(trigger));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void saveSettings() {
        Settings.set(KEY_SYNC_ENABLED, String.valueOf(settings.isSyncOnBuildScriptChanges()));
        Settings.set(KEY_SYNC_TRIGGER, settings.getSyncTrigger().name());
    }
}
