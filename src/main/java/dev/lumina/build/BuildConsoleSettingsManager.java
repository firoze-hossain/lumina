package dev.lumina.build;

import dev.lumina.util.Settings;

/**
 * Singleton manager responsible for loading and saving Build/Execution Console settings in Lumina IDE.
 */
public class BuildConsoleSettingsManager {

    public static final String KEY_ALWAYS_SHOW_DEBUG = "build.console.always.show.debug";
    public static final String KEY_USE_IPYTHON = "build.console.use.ipython";
    public static final String KEY_SHOW_VARIABLES = "build.console.show.variables";
    public static final String KEY_USE_EXISTING_PYTHON = "build.console.use.existing.python";
    public static final String KEY_COMMAND_QUEUE = "build.console.command.queue";
    public static final String KEY_CODE_COMPLETION = "build.console.code.completion";

    private static volatile BuildConsoleSettingsManager instance;
    private BuildConsoleSettings currentSettings;

    private BuildConsoleSettingsManager() {
        loadSettings();
    }

    public static BuildConsoleSettingsManager getInstance() {
        if (instance == null) {
            synchronized (BuildConsoleSettingsManager.class) {
                if (instance == null) {
                    instance = new BuildConsoleSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized BuildConsoleSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(BuildConsoleSettings newSettings) {
        if (newSettings == null) return;
        this.currentSettings = newSettings.clone();
        saveSettings();
    }

    public synchronized void loadSettings() {
        BuildConsoleSettings s = new BuildConsoleSettings();

        String debug = Settings.get(KEY_ALWAYS_SHOW_DEBUG);
        if (debug != null) s.setAlwaysShowDebugConsole(Boolean.parseBoolean(debug));

        String ipython = Settings.get(KEY_USE_IPYTHON);
        if (ipython != null) s.setUseIPythonIfAvailable(Boolean.parseBoolean(ipython));

        String vars = Settings.get(KEY_SHOW_VARIABLES);
        if (vars != null) s.setShowConsoleVariablesByDefault(Boolean.parseBoolean(vars));

        String existing = Settings.get(KEY_USE_EXISTING_PYTHON);
        if (existing != null) s.setUseExistingConsoleForRunWithPythonConsole(Boolean.parseBoolean(existing));

        String queue = Settings.get(KEY_COMMAND_QUEUE);
        if (queue != null) s.setCommandQueueForPythonConsole(Boolean.parseBoolean(queue));

        String completion = Settings.get(KEY_CODE_COMPLETION);
        if (completion != null && !completion.isBlank()) s.setCodeCompletion(completion);

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.set(KEY_ALWAYS_SHOW_DEBUG, String.valueOf(currentSettings.isAlwaysShowDebugConsole()));
        Settings.set(KEY_USE_IPYTHON, String.valueOf(currentSettings.isUseIPythonIfAvailable()));
        Settings.set(KEY_SHOW_VARIABLES, String.valueOf(currentSettings.isShowConsoleVariablesByDefault()));
        Settings.set(KEY_USE_EXISTING_PYTHON, String.valueOf(currentSettings.isUseExistingConsoleForRunWithPythonConsole()));
        Settings.set(KEY_COMMAND_QUEUE, String.valueOf(currentSettings.isCommandQueueForPythonConsole()));
        Settings.set(KEY_CODE_COMPLETION, currentSettings.getCodeCompletion());
    }
}
