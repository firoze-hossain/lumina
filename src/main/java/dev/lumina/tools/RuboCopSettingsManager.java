package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton configuration manager for Tools > RuboCop settings in Lumina IDE.
 */
public class RuboCopSettingsManager {

    private static final String KEY_CONFIG_FILE = "tools.rubocop.config_file";
    private static final String KEY_USE_STANDARD_GEM = "tools.rubocop.use_standard_gem";
    private static final String KEY_RUN_ON_SAVE = "tools.rubocop.run_on_save";

    private static final RuboCopSettingsManager INSTANCE = new RuboCopSettingsManager();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private RuboCopSettingsManager() {
    }

    public static RuboCopSettingsManager getInstance() {
        return INSTANCE;
    }

    public RuboCopSettings load() {
        return getSettings();
    }

    public RuboCopSettings getSettings() {
        RuboCopSettings s = new RuboCopSettings();

        String config = Settings.get(KEY_CONFIG_FILE);
        if (config != null) s.setConfigFile(config);

        String standardGem = Settings.get(KEY_USE_STANDARD_GEM);
        if (standardGem != null) s.setUseStandardGem(Boolean.parseBoolean(standardGem));

        String runOnSave = Settings.get(KEY_RUN_ON_SAVE);
        if (runOnSave != null) s.setRunRuboCopOnSave(Boolean.parseBoolean(runOnSave));

        return s;
    }

    public void setSettings(RuboCopSettings s) {
        if (s == null) return;

        Settings.put(KEY_CONFIG_FILE, s.getConfigFile());
        Settings.put(KEY_USE_STANDARD_GEM, String.valueOf(s.isUseStandardGem()));
        Settings.put(KEY_RUN_ON_SAVE, String.valueOf(s.isRunRuboCopOnSave()));

        notifyListeners();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable r : listeners) {
            try {
                r.run();
            } catch (Throwable ignored) {
            }
        }
    }
}
