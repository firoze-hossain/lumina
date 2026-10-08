package dev.lumina.build;

import dev.lumina.util.Settings;

/**
 * Singleton manager responsible for loading and saving Python Console settings in Lumina IDE.
 */
public class PythonConsoleSettingsManager {

    public static final String KEY_ENV_VARS = "console.python.env.vars";
    public static final String KEY_USE_MODULE_SDK = "console.python.use.module.sdk";
    public static final String KEY_SELECTED_MODULE = "console.python.selected.module";
    public static final String KEY_USE_SPECIFIED = "console.python.use.specified.interpreter";
    public static final String KEY_SPECIFIED_INTERPRETER = "console.python.specified.interpreter";
    public static final String KEY_INTERPRETER_OPTIONS = "console.python.interpreter.options";
    public static final String KEY_WORKING_DIR = "console.python.working.dir";
    public static final String KEY_ADD_CONTENT_ROOTS = "console.python.add.content.roots";
    public static final String KEY_ADD_SOURCE_ROOTS = "console.python.add.source.roots";
    public static final String KEY_STARTING_SCRIPT = "console.python.starting.script";

    private static volatile PythonConsoleSettingsManager instance;
    private PythonConsoleSettings currentSettings;

    private PythonConsoleSettingsManager() {
        loadSettings();
    }

    public static PythonConsoleSettingsManager getInstance() {
        if (instance == null) {
            synchronized (PythonConsoleSettingsManager.class) {
                if (instance == null) {
                    instance = new PythonConsoleSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized PythonConsoleSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(PythonConsoleSettings newSettings) {
        if (newSettings == null) return;
        this.currentSettings = newSettings.clone();
        saveSettings();
    }

    public synchronized void loadSettings() {
        PythonConsoleSettings s = new PythonConsoleSettings();

        String env = Settings.get(KEY_ENV_VARS);
        if (env != null) s.setEnvironmentVariables(env);

        String useSdk = Settings.get(KEY_USE_MODULE_SDK);
        if (useSdk != null) s.setUseModuleSdk(Boolean.parseBoolean(useSdk));

        String mod = Settings.get(KEY_SELECTED_MODULE);
        if (mod != null && !mod.isBlank()) s.setSelectedModule(mod);

        String useSpec = Settings.get(KEY_USE_SPECIFIED);
        if (useSpec != null) s.setUseSpecifiedInterpreter(Boolean.parseBoolean(useSpec));

        String spec = Settings.get(KEY_SPECIFIED_INTERPRETER);
        if (spec != null && !spec.isBlank()) s.setSpecifiedInterpreter(spec);

        String opts = Settings.get(KEY_INTERPRETER_OPTIONS);
        if (opts != null) s.setInterpreterOptions(opts);

        String dir = Settings.get(KEY_WORKING_DIR);
        if (dir != null) s.setWorkingDirectory(dir);

        String contentRoots = Settings.get(KEY_ADD_CONTENT_ROOTS);
        if (contentRoots != null) s.setAddContentRootsToPythonPath(Boolean.parseBoolean(contentRoots));

        String sourceRoots = Settings.get(KEY_ADD_SOURCE_ROOTS);
        if (sourceRoots != null) s.setAddSourceRootsToPythonPath(Boolean.parseBoolean(sourceRoots));

        String script = Settings.get(KEY_STARTING_SCRIPT);
        if (script != null && !script.isBlank()) s.setStartingScript(script);

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.set(KEY_ENV_VARS, currentSettings.getEnvironmentVariables());
        Settings.set(KEY_USE_MODULE_SDK, String.valueOf(currentSettings.isUseModuleSdk()));
        Settings.set(KEY_SELECTED_MODULE, currentSettings.getSelectedModule());
        Settings.set(KEY_USE_SPECIFIED, String.valueOf(currentSettings.isUseSpecifiedInterpreter()));
        Settings.set(KEY_SPECIFIED_INTERPRETER, currentSettings.getSpecifiedInterpreter());
        Settings.set(KEY_INTERPRETER_OPTIONS, currentSettings.getInterpreterOptions());
        Settings.set(KEY_WORKING_DIR, currentSettings.getWorkingDirectory());
        Settings.set(KEY_ADD_CONTENT_ROOTS, String.valueOf(currentSettings.isAddContentRootsToPythonPath()));
        Settings.set(KEY_ADD_SOURCE_ROOTS, String.valueOf(currentSettings.isAddSourceRootsToPythonPath()));
        Settings.set(KEY_STARTING_SCRIPT, currentSettings.getStartingScript());
    }
}
