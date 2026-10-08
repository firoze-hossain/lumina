package dev.lumina.build;

import dev.lumina.util.Settings;

/**
 * Singleton manager responsible for loading and saving Scala Compile Server settings in Lumina IDE.
 */
public class ScalaCompileServerSettingsManager {

    public static final String KEY_USE_SERVER = "compiler.scala.server.use";
    public static final String KEY_PARALLEL = "compiler.scala.server.parallel";
    public static final String KEY_PARALLEL_THREADS = "compiler.scala.server.parallel.threads";
    public static final String KEY_STOP_IDLE = "compiler.scala.server.stop.idle";
    public static final String KEY_IDLE_MINUTES = "compiler.scala.server.idle.minutes";
    public static final String KEY_START_IN_PROJECT_DIR = "compiler.scala.server.start.in.project.dir";
    public static final String KEY_JDK = "compiler.scala.server.jdk";
    public static final String KEY_HEAP_MB = "compiler.scala.server.heap.mb";
    public static final String KEY_VM_OPTIONS = "compiler.scala.server.vm.options";

    private static volatile ScalaCompileServerSettingsManager instance;
    private ScalaCompileServerSettings currentSettings;

    private ScalaCompileServerSettingsManager() {
        loadSettings();
    }

    public static ScalaCompileServerSettingsManager getInstance() {
        if (instance == null) {
            synchronized (ScalaCompileServerSettingsManager.class) {
                if (instance == null) {
                    instance = new ScalaCompileServerSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized ScalaCompileServerSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(ScalaCompileServerSettings newSettings) {
        if (newSettings == null) return;
        this.currentSettings = newSettings.clone();
        saveSettings();
    }

    public synchronized void loadSettings() {
        ScalaCompileServerSettings s = new ScalaCompileServerSettings();

        String use = Settings.get(KEY_USE_SERVER);
        if (use != null) s.setUseCompileServer(Boolean.parseBoolean(use));

        String parallel = Settings.get(KEY_PARALLEL);
        if (parallel != null) s.setCompileIndependentModulesInParallel(Boolean.parseBoolean(parallel));

        String threads = Settings.get(KEY_PARALLEL_THREADS);
        if (threads != null) {
            try { s.setParallelThreads(Integer.parseInt(threads)); } catch (NumberFormatException ignored) {}
        }

        String stop = Settings.get(KEY_STOP_IDLE);
        if (stop != null) s.setStopIfIdle(Boolean.parseBoolean(stop));

        String idle = Settings.get(KEY_IDLE_MINUTES);
        if (idle != null) {
            try { s.setIdleTimeoutMinutes(Integer.parseInt(idle)); } catch (NumberFormatException ignored) {}
        }

        String startDir = Settings.get(KEY_START_IN_PROJECT_DIR);
        if (startDir != null) s.setStartProcessInProjectDirectory(Boolean.parseBoolean(startDir));

        String jdk = Settings.get(KEY_JDK);
        if (jdk != null && !jdk.isBlank()) s.setJdk(jdk);

        String heap = Settings.get(KEY_HEAP_MB);
        if (heap != null) {
            try { s.setMaximumHeapSizeMb(Integer.parseInt(heap)); } catch (NumberFormatException ignored) {}
        }

        String vm = Settings.get(KEY_VM_OPTIONS);
        if (vm != null) s.setVmOptions(vm);

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.set(KEY_USE_SERVER, String.valueOf(currentSettings.isUseCompileServer()));
        Settings.set(KEY_PARALLEL, String.valueOf(currentSettings.isCompileIndependentModulesInParallel()));
        Settings.set(KEY_PARALLEL_THREADS, String.valueOf(currentSettings.getParallelThreads()));
        Settings.set(KEY_STOP_IDLE, String.valueOf(currentSettings.isStopIfIdle()));
        Settings.set(KEY_IDLE_MINUTES, String.valueOf(currentSettings.getIdleTimeoutMinutes()));
        Settings.set(KEY_START_IN_PROJECT_DIR, String.valueOf(currentSettings.isStartProcessInProjectDirectory()));
        Settings.set(KEY_JDK, currentSettings.getJdk());
        Settings.set(KEY_HEAP_MB, String.valueOf(currentSettings.getMaximumHeapSizeMb()));
        Settings.set(KEY_VM_OPTIONS, currentSettings.getVmOptions());
    }
}
