package dev.lumina.build;

import dev.lumina.util.Settings;

/**
 * Singleton manager responsible for loading and saving sbt configuration.
 */
public class SbtSettingsManager {

    public static final String KEY_SBT_JRE = "sbt.jre";
    public static final String KEY_SBT_MAX_HEAP = "sbt.max.heap.mb";
    public static final String KEY_SBT_VM_PARAMS = "sbt.vm.parameters";
    public static final String KEY_SBT_OPTIONS = "sbt.options";
    public static final String KEY_SBT_ENV_VARS = "sbt.environment.variables";
    public static final String KEY_SBT_LAUNCHER = "sbt.launcher";
    public static final String KEY_SBT_CUSTOM_LAUNCHER = "sbt.custom.launcher";
    public static final String KEY_SBT_USE_SHELL_FOR_BUILDS = "sbt.use.shell.for.builds";
    public static final String KEY_SBT_USE_SHELL_FOR_IMPORTS = "sbt.use.shell.for.imports";

    private static volatile SbtSettingsManager instance;
    private SbtSettings currentSettings;

    private SbtSettingsManager() {
        loadSettings();
    }

    public static SbtSettingsManager getInstance() {
        if (instance == null) {
            synchronized (SbtSettingsManager.class) {
                if (instance == null) {
                    instance = new SbtSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized SbtSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(SbtSettings newSettings) {
        if (newSettings == null) return;
        this.currentSettings = newSettings.clone();
        saveSettings();
    }

    public synchronized void loadSettings() {
        SbtSettings s = new SbtSettings();
        String jre = Settings.get(KEY_SBT_JRE);
        if (jre != null && !jre.isBlank()) s.setJre(jre);

        String heap = Settings.get(KEY_SBT_MAX_HEAP);
        if (heap != null) s.setMaximumHeapSizeMb(heap);

        String vm = Settings.get(KEY_SBT_VM_PARAMS);
        if (vm != null) s.setVmParameters(vm);

        String opts = Settings.get(KEY_SBT_OPTIONS);
        if (opts != null) s.setSbtOptions(opts);

        String env = Settings.get(KEY_SBT_ENV_VARS);
        if (env != null) s.setEnvironmentVariables(env);

        String launch = Settings.get(KEY_SBT_LAUNCHER);
        if (launch != null && !launch.isBlank()) s.setLauncher(launch);

        String launcherPath = Settings.get(KEY_SBT_CUSTOM_LAUNCHER);
        if (launcherPath != null) s.setCustomLauncherPath(launcherPath);

        String b = Settings.get(KEY_SBT_USE_SHELL_FOR_BUILDS);
        if (b != null) s.setUseSbtShellForBuilds(Boolean.parseBoolean(b));

        String i = Settings.get(KEY_SBT_USE_SHELL_FOR_IMPORTS);
        if (i != null) s.setUseSbtShellForImports(Boolean.parseBoolean(i));

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.set(KEY_SBT_JRE, currentSettings.getJre());
        Settings.set(KEY_SBT_MAX_HEAP, currentSettings.getMaximumHeapSizeMb());
        Settings.set(KEY_SBT_VM_PARAMS, currentSettings.getVmParameters());
        Settings.set(KEY_SBT_OPTIONS, currentSettings.getSbtOptions());
        Settings.set(KEY_SBT_ENV_VARS, currentSettings.getEnvironmentVariables());
        Settings.set(KEY_SBT_LAUNCHER, currentSettings.getLauncher());
        Settings.set(KEY_SBT_CUSTOM_LAUNCHER, currentSettings.getCustomLauncherPath());
        Settings.set(KEY_SBT_USE_SHELL_FOR_BUILDS, String.valueOf(currentSettings.isUseSbtShellForBuilds()));
        Settings.set(KEY_SBT_USE_SHELL_FOR_IMPORTS, String.valueOf(currentSettings.isUseSbtShellForImports()));
    }
}
