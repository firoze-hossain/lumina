package dev.lumina.typescript;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Languages & Frameworks > TypeScript > Angular settings in Lumina IDE.
 */
public class AngularPluginSettingsManager {

    public static final String KEY_ANGULAR_MODE = "typescript.angular.mode";
    public static final String KEY_ANGULAR_ENABLE_TYPE_ENGINE = "typescript.angular.enable_service_powered_type_engine";

    private static volatile AngularPluginSettingsManager instance;
    private AngularPluginSettings currentSettings;
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private AngularPluginSettingsManager() {
        loadSettings();
    }

    public static AngularPluginSettingsManager getInstance() {
        if (instance == null) {
            synchronized (AngularPluginSettingsManager.class) {
                if (instance == null) {
                    instance = new AngularPluginSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized AngularPluginSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(AngularPluginSettings settings) {
        if (settings == null) return;
        this.currentSettings = settings.clone();
        saveSettings();
        notifyListeners();
    }

    public synchronized void loadSettings() {
        AngularPluginSettings s = new AngularPluginSettings();

        String mode = Settings.get(KEY_ANGULAR_MODE);
        if (mode != null && !mode.isBlank()) {
            s.setMode(mode);
        }

        String engine = Settings.get(KEY_ANGULAR_ENABLE_TYPE_ENGINE);
        if (engine != null) {
            s.setEnableServicePoweredTypeEngine(Boolean.parseBoolean(engine));
        }

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.put(KEY_ANGULAR_MODE, currentSettings.getMode());
        Settings.put(KEY_ANGULAR_ENABLE_TYPE_ENGINE, String.valueOf(currentSettings.isEnableServicePoweredTypeEngine()));
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
