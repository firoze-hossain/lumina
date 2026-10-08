package dev.lumina.build;

import dev.lumina.util.Settings;

/**
 * Singleton manager responsible for loading and saving RMI Compiler settings in Lumina IDE.
 */
public class RmiCompilerSettingsManager {

    public static final String KEY_ENABLE_STUBS = "compiler.rmi.enable.stubs";
    public static final String KEY_GENERATE_IIOP = "compiler.rmi.generate.iiop";
    public static final String KEY_GENERATE_DEBUG = "compiler.rmi.generate.debugging.info";
    public static final String KEY_GENERATE_NO_WARNINGS = "compiler.rmi.generate.no.warnings";
    public static final String KEY_ADDITIONAL_PARAMS = "compiler.rmi.additional.params";

    private static volatile RmiCompilerSettingsManager instance;
    private RmiCompilerSettings currentSettings;

    private RmiCompilerSettingsManager() {
        loadSettings();
    }

    public static RmiCompilerSettingsManager getInstance() {
        if (instance == null) {
            synchronized (RmiCompilerSettingsManager.class) {
                if (instance == null) {
                    instance = new RmiCompilerSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized RmiCompilerSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(RmiCompilerSettings newSettings) {
        if (newSettings == null) return;
        this.currentSettings = newSettings.clone();
        saveSettings();
    }

    public synchronized void loadSettings() {
        RmiCompilerSettings s = new RmiCompilerSettings();

        String enable = Settings.get(KEY_ENABLE_STUBS);
        if (enable != null) s.setEnableRmiStubsGeneration(Boolean.parseBoolean(enable));

        String iiop = Settings.get(KEY_GENERATE_IIOP);
        if (iiop != null) s.setGenerateIiopStubs(Boolean.parseBoolean(iiop));

        String debug = Settings.get(KEY_GENERATE_DEBUG);
        if (debug != null) s.setGenerateDebuggingInfo(Boolean.parseBoolean(debug));

        String noWarn = Settings.get(KEY_GENERATE_NO_WARNINGS);
        if (noWarn != null) s.setGenerateNoWarnings(Boolean.parseBoolean(noWarn));

        String additional = Settings.get(KEY_ADDITIONAL_PARAMS);
        if (additional != null) s.setAdditionalCommandLineParameters(additional);

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.set(KEY_ENABLE_STUBS, String.valueOf(currentSettings.isEnableRmiStubsGeneration()));
        Settings.set(KEY_GENERATE_IIOP, String.valueOf(currentSettings.isGenerateIiopStubs()));
        Settings.set(KEY_GENERATE_DEBUG, String.valueOf(currentSettings.isGenerateDebuggingInfo()));
        Settings.set(KEY_GENERATE_NO_WARNINGS, String.valueOf(currentSettings.isGenerateNoWarnings()));
        Settings.set(KEY_ADDITIONAL_PARAMS, currentSettings.getAdditionalCommandLineParameters());
    }
}
