package dev.lumina.build;

import dev.lumina.util.Settings;

/**
 * Singleton manager responsible for loading and saving Compiler configuration.
 */
public class CompilerSettingsManager {

    public static final String KEY_RESOURCE_PATTERNS = "compiler.resource.patterns";
    public static final String KEY_CLEAR_OUTPUT_DIR = "compiler.clear.output.dir.on.rebuild";
    public static final String KEY_NOT_NULL_ASSERTIONS = "compiler.add.notnull.assertions";
    public static final String KEY_AUTO_SHOW_FIRST_ERROR = "compiler.auto.show.first.error";
    public static final String KEY_DISPLAY_NOTIFICATION = "compiler.display.notification.on.build";
    public static final String KEY_BUILD_AUTOMATICALLY = "compiler.build.automatically";
    public static final String KEY_REBUILD_ON_DEP_CHANGE = "compiler.rebuild.on.dependency.change";
    public static final String KEY_COMPILE_PARALLEL_MODE = "compiler.parallel.compilation.mode";
    public static final String KEY_SHARED_HEAP_SIZE = "compiler.shared.heap.size.mb";
    public static final String KEY_SHARED_VM_OPTIONS = "compiler.shared.vm.options";
    public static final String KEY_USER_LOCAL_HEAP_SIZE = "compiler.user.local.heap.size.mb";
    public static final String KEY_USER_LOCAL_VM_OPTIONS = "compiler.user.local.vm.options";

    private static volatile CompilerSettingsManager instance;
    private CompilerSettings currentSettings;

    private CompilerSettingsManager() {
        loadSettings();
    }

    public static CompilerSettingsManager getInstance() {
        if (instance == null) {
            synchronized (CompilerSettingsManager.class) {
                if (instance == null) {
                    instance = new CompilerSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized CompilerSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(CompilerSettings newSettings) {
        if (newSettings == null) return;
        this.currentSettings = newSettings.clone();
        saveSettings();
    }

    public synchronized void loadSettings() {
        CompilerSettings s = new CompilerSettings();

        String patterns = Settings.get(KEY_RESOURCE_PATTERNS);
        if (patterns != null) s.setResourcePatterns(patterns);

        String clearOut = Settings.get(KEY_CLEAR_OUTPUT_DIR);
        if (clearOut != null) s.setClearOutputDirectoryOnRebuild(Boolean.parseBoolean(clearOut));

        String notNull = Settings.get(KEY_NOT_NULL_ASSERTIONS);
        if (notNull != null) s.setAddRuntimeAssertionsNotNull(Boolean.parseBoolean(notNull));

        String err = Settings.get(KEY_AUTO_SHOW_FIRST_ERROR);
        if (err != null) s.setAutoShowFirstErrorInEditor(Boolean.parseBoolean(err));

        String notif = Settings.get(KEY_DISPLAY_NOTIFICATION);
        if (notif != null) s.setDisplayNotificationOnBuildCompletion(Boolean.parseBoolean(notif));

        String autoBuild = Settings.get(KEY_BUILD_AUTOMATICALLY);
        if (autoBuild != null) s.setBuildProjectAutomatically(Boolean.parseBoolean(autoBuild));

        String rebuildDep = Settings.get(KEY_REBUILD_ON_DEP_CHANGE);
        if (rebuildDep != null) s.setRebuildModuleOnDependencyChange(Boolean.parseBoolean(rebuildDep));

        String parallel = Settings.get(KEY_COMPILE_PARALLEL_MODE);
        if (parallel != null) s.setCompileModulesInParallel(parallel);

        String sharedHeap = Settings.get(KEY_SHARED_HEAP_SIZE);
        if (sharedHeap != null) s.setSharedHeapSizeMb(sharedHeap);

        String sharedVm = Settings.get(KEY_SHARED_VM_OPTIONS);
        if (sharedVm != null) s.setSharedVmOptions(sharedVm);

        String userHeap = Settings.get(KEY_USER_LOCAL_HEAP_SIZE);
        if (userHeap != null) s.setUserLocalHeapSizeMb(userHeap);

        String userVm = Settings.get(KEY_USER_LOCAL_VM_OPTIONS);
        if (userVm != null) s.setUserLocalVmOptions(userVm);

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.set(KEY_RESOURCE_PATTERNS, currentSettings.getResourcePatterns());
        Settings.set(KEY_CLEAR_OUTPUT_DIR, String.valueOf(currentSettings.isClearOutputDirectoryOnRebuild()));
        Settings.set(KEY_NOT_NULL_ASSERTIONS, String.valueOf(currentSettings.isAddRuntimeAssertionsNotNull()));
        Settings.set(KEY_AUTO_SHOW_FIRST_ERROR, String.valueOf(currentSettings.isAutoShowFirstErrorInEditor()));
        Settings.set(KEY_DISPLAY_NOTIFICATION, String.valueOf(currentSettings.isDisplayNotificationOnBuildCompletion()));
        Settings.set(KEY_BUILD_AUTOMATICALLY, String.valueOf(currentSettings.isBuildProjectAutomatically()));
        Settings.set(KEY_REBUILD_ON_DEP_CHANGE, String.valueOf(currentSettings.isRebuildModuleOnDependencyChange()));
        Settings.set(KEY_COMPILE_PARALLEL_MODE, currentSettings.getCompileModulesInParallel());
        Settings.set(KEY_SHARED_HEAP_SIZE, currentSettings.getSharedHeapSizeMb());
        Settings.set(KEY_SHARED_VM_OPTIONS, currentSettings.getSharedVmOptions());
        Settings.set(KEY_USER_LOCAL_HEAP_SIZE, currentSettings.getUserLocalHeapSizeMb());
        Settings.set(KEY_USER_LOCAL_VM_OPTIONS, currentSettings.getUserLocalVmOptions());
    }
}
