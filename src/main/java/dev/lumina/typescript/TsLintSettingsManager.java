package dev.lumina.typescript;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Languages & Frameworks > TypeScript > TSLint settings in Lumina IDE.
 */
public class TsLintSettingsManager {

    public static final String KEY_TSLINT_MODE = "typescript.tslint.mode";
    public static final String KEY_TSLINT_PACKAGE = "typescript.tslint.package";
    public static final String KEY_TSLINT_CONFIG_FILE = "typescript.tslint.config_file";
    public static final String KEY_TSLINT_RULES_DIRECTORY = "typescript.tslint.rules_directory";

    private static volatile TsLintSettingsManager instance;
    private TsLintSettings currentSettings;
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private TsLintSettingsManager() {
        loadSettings();
    }

    public static TsLintSettingsManager getInstance() {
        if (instance == null) {
            synchronized (TsLintSettingsManager.class) {
                if (instance == null) {
                    instance = new TsLintSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized TsLintSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(TsLintSettings settings) {
        if (settings == null) return;
        this.currentSettings = settings.clone();
        saveSettings();
        notifyListeners();
    }

    public synchronized void loadSettings() {
        TsLintSettings s = new TsLintSettings();

        String mode = Settings.get(KEY_TSLINT_MODE);
        if (mode != null && !mode.isBlank()) {
            s.setMode(mode);
        }

        String pkg = Settings.get(KEY_TSLINT_PACKAGE);
        if (pkg != null) {
            s.setTslintPackage(pkg);
        }

        String cfg = Settings.get(KEY_TSLINT_CONFIG_FILE);
        if (cfg != null) {
            s.setConfigFile(cfg);
        }

        String rules = Settings.get(KEY_TSLINT_RULES_DIRECTORY);
        if (rules != null) {
            s.setRulesDirectory(rules);
        }

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.put(KEY_TSLINT_MODE, currentSettings.getMode());
        Settings.put(KEY_TSLINT_PACKAGE, currentSettings.getTslintPackage());
        Settings.put(KEY_TSLINT_CONFIG_FILE, currentSettings.getConfigFile());
        Settings.put(KEY_TSLINT_RULES_DIRECTORY, currentSettings.getRulesDirectory());
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
