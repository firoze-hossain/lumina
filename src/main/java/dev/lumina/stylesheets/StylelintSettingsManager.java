package dev.lumina.stylesheets;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Languages & Frameworks > Style Sheets > Stylelint settings in Lumina IDE.
 */
public class StylelintSettingsManager {

    public static final String KEY_STYLELINT_ENABLED = "stylesheets.stylelint.enabled";
    public static final String KEY_STYLELINT_PACKAGE = "stylesheets.stylelint.package";
    public static final String KEY_STYLELINT_CONFIG_FILE = "stylesheets.stylelint.config.file";
    public static final String KEY_STYLELINT_RUN_FOR_FILES = "stylesheets.stylelint.run.for.files";
    public static final String KEY_STYLELINT_FIX_ON_SAVE = "stylesheets.stylelint.fix.on.save";

    private static volatile StylelintSettingsManager instance;
    private StylelintSettings currentSettings;
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private StylelintSettingsManager() {
        loadSettings();
    }

    public static StylelintSettingsManager getInstance() {
        if (instance == null) {
            synchronized (StylelintSettingsManager.class) {
                if (instance == null) {
                    instance = new StylelintSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized StylelintSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(StylelintSettings settings) {
        if (settings == null) return;
        this.currentSettings = settings.clone();
        saveSettings();
        notifyListeners();
    }

    public synchronized void loadSettings() {
        StylelintSettings s = new StylelintSettings();

        String enabled = Settings.get(KEY_STYLELINT_ENABLED);
        if (enabled != null) {
            s.setEnabled(Boolean.parseBoolean(enabled));
        }

        String pkg = Settings.get(KEY_STYLELINT_PACKAGE);
        if (pkg != null) {
            s.setPackagePath(pkg);
        }

        String cfg = Settings.get(KEY_STYLELINT_CONFIG_FILE);
        if (cfg != null && !cfg.isBlank()) {
            s.setConfigurationFile(cfg);
        }

        String runFor = Settings.get(KEY_STYLELINT_RUN_FOR_FILES);
        if (runFor != null && !runFor.isBlank()) {
            s.setRunForFiles(runFor);
        }

        String fix = Settings.get(KEY_STYLELINT_FIX_ON_SAVE);
        if (fix != null) {
            s.setFixOnSave(Boolean.parseBoolean(fix));
        }

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.put(KEY_STYLELINT_ENABLED, String.valueOf(currentSettings.isEnabled()));
        Settings.put(KEY_STYLELINT_PACKAGE, currentSettings.getPackagePath());
        Settings.put(KEY_STYLELINT_CONFIG_FILE, currentSettings.getConfigurationFile());
        Settings.put(KEY_STYLELINT_RUN_FOR_FILES, currentSettings.getRunForFiles());
        Settings.put(KEY_STYLELINT_FIX_ON_SAVE, String.valueOf(currentSettings.isFixOnSave()));
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable r : changeListeners) {
            try {
                r.run();
            } catch (Exception ignored) {}
        }
    }
}
