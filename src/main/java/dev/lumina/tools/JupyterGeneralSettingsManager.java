package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manager handling persistence and change notifications for Tools > Jupyter > Jupyter General settings.
 */
public class JupyterGeneralSettingsManager {

    private static final JupyterGeneralSettingsManager INSTANCE = new JupyterGeneralSettingsManager();

    private static final String KEY_PREFIX = "tools.jupyter.general.";
    private static final String KEY_SHOW_ADD_CELL_POPUP = KEY_PREFIX + "show_add_cell_popup";
    private static final String KEY_SHOW_RUN_DEBUG_ACTIONS = KEY_PREFIX + "show_run_debug_actions";
    private static final String KEY_INVERT_IMAGE_OUTPUTS = KEY_PREFIX + "invert_image_outputs";
    private static final String KEY_MAX_OUTPUT_HEIGHT = KEY_PREFIX + "max_output_height";
    private static final String KEY_ASCII_COLORING_ERRORS = KEY_PREFIX + "ascii_coloring_errors";

    private static final String KEY_MARKDOWN_FONT_SCALE = KEY_PREFIX + "markdown_font_scale";
    private static final String KEY_RENDER_MARKDOWN_AUTO = KEY_PREFIX + "render_markdown_auto";

    private static final String KEY_OPEN_VARIABLES_FIRST = KEY_PREFIX + "open_variables_first";
    private static final String KEY_SHOW_INLINE_VALUES = KEY_PREFIX + "show_inline_values";
    private static final String KEY_INLINE_VALUES_SCOPE = KEY_PREFIX + "inline_values_scope";

    private static final String KEY_NOTIFY_EXECUTION_EXCEEDS = KEY_PREFIX + "notify_execution_exceeds";
    private static final String KEY_EXECUTION_TIMEOUT_SECS = KEY_PREFIX + "execution_timeout_secs";
    private static final String KEY_SHOW_TIMESTAMP_LABEL = KEY_PREFIX + "show_timestamp_label";
    private static final String KEY_EXECUTION_TIME_MODE = KEY_PREFIX + "execution_time_mode";
    private static final String KEY_INCLUDE_SOURCE_ROOTS_PATH = KEY_PREFIX + "include_source_roots_path";

    private static final String KEY_UPLOAD_SUPPORT_LIBS = KEY_PREFIX + "upload_support_libs";

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private JupyterGeneralSettingsManager() {
    }

    public static JupyterGeneralSettingsManager getInstance() {
        return INSTANCE;
    }

    public JupyterGeneralSettings load() {
        return getSettings();
    }

    public JupyterGeneralSettings getSettings() {
        JupyterGeneralSettings s = new JupyterGeneralSettings();

        String val = Settings.get(KEY_SHOW_ADD_CELL_POPUP);
        if (val != null) s.setShowAddCellPopup(Boolean.parseBoolean(val));

        val = Settings.get(KEY_SHOW_RUN_DEBUG_ACTIONS);
        if (val != null) s.setShowRunAndDebugActions(Boolean.parseBoolean(val));

        val = Settings.get(KEY_INVERT_IMAGE_OUTPUTS);
        if (val != null) s.setInvertImageOutputsForDarkThemes(Boolean.parseBoolean(val));

        val = Settings.get(KEY_MAX_OUTPUT_HEIGHT);
        if (val != null) {
            try {
                s.setMaxOutputHeightInTextLines(Integer.parseInt(val));
            } catch (Exception ignored) {
            }
        }

        val = Settings.get(KEY_ASCII_COLORING_ERRORS);
        if (val != null) s.setAsciiColoringInErrorOutputs(Boolean.parseBoolean(val));

        val = Settings.get(KEY_MARKDOWN_FONT_SCALE);
        if (val != null) {
            try {
                s.setMarkdownFontScale(Integer.parseInt(val));
            } catch (Exception ignored) {
            }
        }

        val = Settings.get(KEY_RENDER_MARKDOWN_AUTO);
        if (val != null) s.setRenderMarkdownCellsAutomatically(Boolean.parseBoolean(val));

        val = Settings.get(KEY_OPEN_VARIABLES_FIRST);
        if (val != null) s.setOpenVariablesOnFirstCellExecution(Boolean.parseBoolean(val));

        val = Settings.get(KEY_SHOW_INLINE_VALUES);
        if (val != null) s.setShowInlineValues(Boolean.parseBoolean(val));

        val = Settings.get(KEY_INLINE_VALUES_SCOPE);
        if (val != null) s.setInlineValuesScope(val);

        val = Settings.get(KEY_NOTIFY_EXECUTION_EXCEEDS);
        if (val != null) s.setNotifyWhenCellExecutionExceeds(Boolean.parseBoolean(val));

        val = Settings.get(KEY_EXECUTION_TIMEOUT_SECS);
        if (val != null) {
            try {
                s.setCellExecutionTimeoutSeconds(Integer.parseInt(val));
            } catch (Exception ignored) {
            }
        }

        val = Settings.get(KEY_SHOW_TIMESTAMP_LABEL);
        if (val != null) s.setShowTimestampOnExecutionLabel(Boolean.parseBoolean(val));

        val = Settings.get(KEY_EXECUTION_TIME_MODE);
        if (val != null) s.setExecutionTimeDisplayMode(val);

        val = Settings.get(KEY_INCLUDE_SOURCE_ROOTS_PATH);
        if (val != null) s.setIncludeProjectSourceRootsToPythonPath(Boolean.parseBoolean(val));

        val = Settings.get(KEY_UPLOAD_SUPPORT_LIBS);
        if (val != null) s.setUploadSupportLibsToJupyterServer(Boolean.parseBoolean(val));

        return s;
    }

    public void setSettings(JupyterGeneralSettings s) {
        if (s == null) return;

        Settings.put(KEY_SHOW_ADD_CELL_POPUP, String.valueOf(s.isShowAddCellPopup()));
        Settings.put(KEY_SHOW_RUN_DEBUG_ACTIONS, String.valueOf(s.isShowRunAndDebugActions()));
        Settings.put(KEY_INVERT_IMAGE_OUTPUTS, String.valueOf(s.isInvertImageOutputsForDarkThemes()));
        Settings.put(KEY_MAX_OUTPUT_HEIGHT, String.valueOf(s.getMaxOutputHeightInTextLines()));
        Settings.put(KEY_ASCII_COLORING_ERRORS, String.valueOf(s.isAsciiColoringInErrorOutputs()));

        Settings.put(KEY_MARKDOWN_FONT_SCALE, String.valueOf(s.getMarkdownFontScale()));
        Settings.put(KEY_RENDER_MARKDOWN_AUTO, String.valueOf(s.isRenderMarkdownCellsAutomatically()));

        Settings.put(KEY_OPEN_VARIABLES_FIRST, String.valueOf(s.isOpenVariablesOnFirstCellExecution()));
        Settings.put(KEY_SHOW_INLINE_VALUES, String.valueOf(s.isShowInlineValues()));
        Settings.put(KEY_INLINE_VALUES_SCOPE, s.getInlineValuesScope());

        Settings.put(KEY_NOTIFY_EXECUTION_EXCEEDS, String.valueOf(s.isNotifyWhenCellExecutionExceeds()));
        Settings.put(KEY_EXECUTION_TIMEOUT_SECS, String.valueOf(s.getCellExecutionTimeoutSeconds()));
        Settings.put(KEY_SHOW_TIMESTAMP_LABEL, String.valueOf(s.isShowTimestampOnExecutionLabel()));
        Settings.put(KEY_EXECUTION_TIME_MODE, s.getExecutionTimeDisplayMode());
        Settings.put(KEY_INCLUDE_SOURCE_ROOTS_PATH, String.valueOf(s.isIncludeProjectSourceRootsToPythonPath()));

        Settings.put(KEY_UPLOAD_SUPPORT_LIBS, String.valueOf(s.isUploadSupportLibsToJupyterServer()));

        notifyListeners();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    public void clear() {
        Settings.put(KEY_SHOW_ADD_CELL_POPUP, null);
        Settings.put(KEY_SHOW_RUN_DEBUG_ACTIONS, null);
        Settings.put(KEY_INVERT_IMAGE_OUTPUTS, null);
        Settings.put(KEY_MAX_OUTPUT_HEIGHT, null);
        Settings.put(KEY_ASCII_COLORING_ERRORS, null);

        Settings.put(KEY_MARKDOWN_FONT_SCALE, null);
        Settings.put(KEY_RENDER_MARKDOWN_AUTO, null);

        Settings.put(KEY_OPEN_VARIABLES_FIRST, null);
        Settings.put(KEY_SHOW_INLINE_VALUES, null);
        Settings.put(KEY_INLINE_VALUES_SCOPE, null);

        Settings.put(KEY_NOTIFY_EXECUTION_EXCEEDS, null);
        Settings.put(KEY_EXECUTION_TIMEOUT_SECS, null);
        Settings.put(KEY_SHOW_TIMESTAMP_LABEL, null);
        Settings.put(KEY_EXECUTION_TIME_MODE, null);
        Settings.put(KEY_INCLUDE_SOURCE_ROOTS_PATH, null);

        Settings.put(KEY_UPLOAD_SUPPORT_LIBS, null);

        notifyListeners();
    }

    public void resetDefaults() {
        setSettings(new JupyterGeneralSettings());
    }

    private void notifyListeners() {
        for (Runnable l : listeners) {
            try {
                l.run();
            } catch (Throwable ignored) {
            }
        }
    }
}
