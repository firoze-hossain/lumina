package dev.lumina.jvm;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for JVM Logging settings in Lumina IDE.
 */
public class JvmLoggingSettingsManager {

    public static final String KEY_VARIABLE_NAME = "jvm.logging.variable.name";
    public static final String KEY_LOGGER = "jvm.logging.logger";

    private static JvmLoggingSettingsManager instance;

    private JvmLoggingSettings currentSettings = new JvmLoggingSettings();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private JvmLoggingSettingsManager() {
        loadSettings();
    }

    public static synchronized JvmLoggingSettingsManager getInstance() {
        if (instance == null) {
            instance = new JvmLoggingSettingsManager();
        }
        return instance;
    }

    public synchronized JvmLoggingSettings getSettings() {
        return currentSettings.copy();
    }

    public synchronized void setSettings(JvmLoggingSettings settings) {
        this.currentSettings = settings != null ? settings.copy() : new JvmLoggingSettings();
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
        Settings.put(KEY_VARIABLE_NAME, currentSettings.getVariableName());
        Settings.put(KEY_LOGGER, currentSettings.getLogger());
    }

    public synchronized void loadSettings() {
        String varName = Settings.get(KEY_VARIABLE_NAME);
        if (varName == null || varName.isBlank()) {
            varName = JvmLoggingSettings.DEFAULT_VARIABLE_NAME;
        }

        String logger = Settings.get(KEY_LOGGER);
        if (logger == null || logger.isBlank()) {
            logger = JvmLoggingSettings.DEFAULT_LOGGER;
        }

        this.currentSettings = new JvmLoggingSettings(varName, logger);
    }

    public synchronized void resetDefaults() {
        this.currentSettings = new JvmLoggingSettings();
        saveSettings();
        notifyListeners();
    }
}
