package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager handling persistence and change listeners for Tools > Kotlin Notebook settings.
 */
public class KotlinNotebookSettingsManager {

    private static final KotlinNotebookSettingsManager INSTANCE = new KotlinNotebookSettingsManager();

    private static final String KEY_PREFIX = "tools.kotlin_notebook.";
    private static final String KEY_KERNEL_VERSION = KEY_PREFIX + "kernel_version";
    private static final String KEY_JDK_PATH = KEY_PREFIX + "jdk_path";
    private static final String KEY_JVM_TARGET = KEY_PREFIX + "jvm_target_for_snippets";
    private static final String KEY_MAX_HEAP_SIZE = KEY_PREFIX + "max_heap_size";
    private static final String KEY_JVM_EXTRA_ARGS = KEY_PREFIX + "jvm_extra_arguments";
    private static final String KEY_ENV_VARS = KEY_PREFIX + "environment_variables";

    private static final String KEY_SHOW_SESSION_VARS = KEY_PREFIX + "show_notebook_session_variables";
    private static final String KEY_OPEN_VARS_TAB = KEY_PREFIX + "open_variables_tab_after_cell_execution";

    private static final String KEY_STOP_ON_FAILURE = KEY_PREFIX + "stop_execution_on_failure";
    private static final String KEY_RESOLVE_SOURCES = KEY_PREFIX + "resolve_sources";
    private static final String KEY_RESOLVE_MULTIPLATFORM = KEY_PREFIX + "resolve_multiplatform_dependencies";

    private static final String KEY_RENDER_KANDY = KEY_PREFIX + "render_kandy_plots_natively";
    private static final String KEY_RENDER_DATAFRAME = KEY_PREFIX + "render_dataframe_tables_natively";

    private static final String KEY_SHOW_TYPE_HINTS_ACTIVE = KEY_PREFIX + "show_type_hints_only_in_active_cell";

    private static final String KEY_DISPLAY_EXEC_COUNT = KEY_PREFIX + "display_execution_count";
    private static final String KEY_SHOW_FOLDABLE_REGIONS = KEY_PREFIX + "show_foldable_regions";

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private KotlinNotebookSettingsManager() {
    }

    public static KotlinNotebookSettingsManager getInstance() {
        return INSTANCE;
    }

    public KotlinNotebookSettings load() {
        return getSettings();
    }

    public KotlinNotebookSettings getSettings() {
        KotlinNotebookSettings s = new KotlinNotebookSettings();

        String val = Settings.get(KEY_KERNEL_VERSION);
        if (val != null) s.setKernelVersion(val);

        val = Settings.get(KEY_JDK_PATH);
        if (val != null) s.setJdkPath(val);

        val = Settings.get(KEY_JVM_TARGET);
        if (val != null) s.setJvmTargetForSnippets(val);

        val = Settings.get(KEY_MAX_HEAP_SIZE);
        if (val != null) {
            try {
                s.setMaxHeapSize(Integer.parseInt(val));
            } catch (NumberFormatException ignored) {
            }
        }

        val = Settings.get(KEY_JVM_EXTRA_ARGS);
        if (val != null) s.setJvmExtraArguments(val);

        val = Settings.get(KEY_ENV_VARS);
        if (val != null) s.setEnvironmentVariables(val);

        val = Settings.get(KEY_SHOW_SESSION_VARS);
        if (val != null) s.setShowNotebookSessionVariables(Boolean.parseBoolean(val));

        val = Settings.get(KEY_OPEN_VARS_TAB);
        if (val != null) s.setOpenVariablesTabAfterCellExecution(Boolean.parseBoolean(val));

        val = Settings.get(KEY_STOP_ON_FAILURE);
        if (val != null) s.setStopExecutionOnFailure(Boolean.parseBoolean(val));

        val = Settings.get(KEY_RESOLVE_SOURCES);
        if (val != null) s.setResolveSources(Boolean.parseBoolean(val));

        val = Settings.get(KEY_RESOLVE_MULTIPLATFORM);
        if (val != null) s.setResolveMultiplatformDependencies(Boolean.parseBoolean(val));

        val = Settings.get(KEY_RENDER_KANDY);
        if (val != null) s.setRenderKandyPlotsNatively(Boolean.parseBoolean(val));

        val = Settings.get(KEY_RENDER_DATAFRAME);
        if (val != null) s.setRenderDataFrameTablesNatively(Boolean.parseBoolean(val));

        val = Settings.get(KEY_SHOW_TYPE_HINTS_ACTIVE);
        if (val != null) s.setShowTypeHintsOnlyInActiveCell(Boolean.parseBoolean(val));

        val = Settings.get(KEY_DISPLAY_EXEC_COUNT);
        if (val != null) s.setDisplayExecutionCount(Boolean.parseBoolean(val));

        val = Settings.get(KEY_SHOW_FOLDABLE_REGIONS);
        if (val != null) s.setShowFoldableRegions(Boolean.parseBoolean(val));

        return s;
    }

    public void setSettings(KotlinNotebookSettings s) {
        if (s == null) return;

        Settings.put(KEY_KERNEL_VERSION, s.getKernelVersion());
        Settings.put(KEY_JDK_PATH, s.getJdkPath());
        Settings.put(KEY_JVM_TARGET, s.getJvmTargetForSnippets());
        Settings.put(KEY_MAX_HEAP_SIZE, String.valueOf(s.getMaxHeapSize()));
        Settings.put(KEY_JVM_EXTRA_ARGS, s.getJvmExtraArguments());
        Settings.put(KEY_ENV_VARS, s.getEnvironmentVariables());

        Settings.put(KEY_SHOW_SESSION_VARS, String.valueOf(s.isShowNotebookSessionVariables()));
        Settings.put(KEY_OPEN_VARS_TAB, String.valueOf(s.isOpenVariablesTabAfterCellExecution()));

        Settings.put(KEY_STOP_ON_FAILURE, String.valueOf(s.isStopExecutionOnFailure()));
        Settings.put(KEY_RESOLVE_SOURCES, String.valueOf(s.isResolveSources()));
        Settings.put(KEY_RESOLVE_MULTIPLATFORM, String.valueOf(s.isResolveMultiplatformDependencies()));

        Settings.put(KEY_RENDER_KANDY, String.valueOf(s.isRenderKandyPlotsNatively()));
        Settings.put(KEY_RENDER_DATAFRAME, String.valueOf(s.isRenderDataFrameTablesNatively()));

        Settings.put(KEY_SHOW_TYPE_HINTS_ACTIVE, String.valueOf(s.isShowTypeHintsOnlyInActiveCell()));

        Settings.put(KEY_DISPLAY_EXEC_COUNT, String.valueOf(s.isDisplayExecutionCount()));
        Settings.put(KEY_SHOW_FOLDABLE_REGIONS, String.valueOf(s.isShowFoldableRegions()));

        notifyListeners();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeChangeListener(Runnable listener) {
        if (listener != null) listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            try {
                listener.run();
            } catch (Throwable ignored) {
            }
        }
    }
}
