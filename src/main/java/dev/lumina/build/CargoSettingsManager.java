package dev.lumina.build;

import dev.lumina.util.Settings;

/**
 * Singleton manager responsible for loading and saving Cargo configuration.
 */
public class CargoSettingsManager {

    public static final String KEY_CARGO_AUTO_SHOW_FIRST_ERROR = "cargo.auto.show.first.error";
    public static final String KEY_CARGO_OFFLINE_MODE = "cargo.offline.mode";
    public static final String KEY_CARGO_EXECUTABLE = "cargo.executable.path";

    private static volatile CargoSettingsManager instance;
    private CargoSettings currentSettings;

    private CargoSettingsManager() {
        loadSettings();
    }

    public static CargoSettingsManager getInstance() {
        if (instance == null) {
            synchronized (CargoSettingsManager.class) {
                if (instance == null) {
                    instance = new CargoSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized CargoSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(CargoSettings newSettings) {
        if (newSettings == null) return;
        this.currentSettings = newSettings.clone();
        saveSettings();
    }

    public synchronized void loadSettings() {
        CargoSettings s = new CargoSettings();
        String auto = Settings.get(KEY_CARGO_AUTO_SHOW_FIRST_ERROR);
        if (auto != null) s.setAutoShowFirstError(Boolean.parseBoolean(auto));

        String offline = Settings.get(KEY_CARGO_OFFLINE_MODE);
        if (offline != null) s.setOfflineMode(Boolean.parseBoolean(offline));

        String exec = Settings.get(KEY_CARGO_EXECUTABLE);
        if (exec != null) s.setCargoExecutable(exec);

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.set(KEY_CARGO_AUTO_SHOW_FIRST_ERROR, String.valueOf(currentSettings.isAutoShowFirstError()));
        Settings.set(KEY_CARGO_OFFLINE_MODE, String.valueOf(currentSettings.isOfflineMode()));
        Settings.set(KEY_CARGO_EXECUTABLE, currentSettings.getCargoExecutable());
    }
}
