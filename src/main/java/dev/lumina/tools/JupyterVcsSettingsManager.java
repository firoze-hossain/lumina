package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manager handling persistence and change notifications for Tools > Jupyter > Jupyter VCS settings.
 */
public class JupyterVcsSettingsManager {

    private static final JupyterVcsSettingsManager INSTANCE = new JupyterVcsSettingsManager();

    private static final String KEY_PREFIX = "tools.jupyter.vcs.";
    private static final String KEY_CLEAR_NOTEBOOK_OUTPUTS_BEFORE_COMMIT = KEY_PREFIX + "clear_notebook_outputs_before_commit";
    private static final String KEY_CLEAR_OUTPUTS_MODE = KEY_PREFIX + "clear_outputs_mode";
    private static final String KEY_FILE_SIZE_EXCEEDS_MB = KEY_PREFIX + "file_size_exceeds_mb";
    private static final String KEY_RICH_DIFF = KEY_PREFIX + "rich_diff";
    private static final String KEY_IGNORE_OUTPUTS = KEY_PREFIX + "ignore_outputs";

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private JupyterVcsSettingsManager() {
    }

    public static JupyterVcsSettingsManager getInstance() {
        return INSTANCE;
    }

    public JupyterVcsSettings load() {
        return getSettings();
    }

    public JupyterVcsSettings getSettings() {
        JupyterVcsSettings s = new JupyterVcsSettings();

        String val = Settings.get(KEY_CLEAR_NOTEBOOK_OUTPUTS_BEFORE_COMMIT);
        if (val != null) {
            s.setClearNotebookOutputsBeforeCommit(Boolean.parseBoolean(val));
        }

        val = Settings.get(KEY_CLEAR_OUTPUTS_MODE);
        if (val != null) {
            s.setClearOutputsMode(val);
        }

        val = Settings.get(KEY_FILE_SIZE_EXCEEDS_MB);
        if (val != null) {
            try {
                s.setFileSizeExceedsMb(Integer.parseInt(val));
            } catch (NumberFormatException ignored) {
            }
        }

        val = Settings.get(KEY_RICH_DIFF);
        if (val != null) {
            s.setShowRichDiffForNotebooks(Boolean.parseBoolean(val));
        }

        val = Settings.get(KEY_IGNORE_OUTPUTS);
        if (val != null) {
            s.setIgnoreOutputsInDiff(Boolean.parseBoolean(val));
        }

        return s;
    }

    public void setSettings(JupyterVcsSettings s) {
        if (s == null) return;

        Settings.put(KEY_CLEAR_NOTEBOOK_OUTPUTS_BEFORE_COMMIT, String.valueOf(s.isClearNotebookOutputsBeforeCommit()));
        Settings.put(KEY_CLEAR_OUTPUTS_MODE, s.getClearOutputsMode());
        Settings.put(KEY_FILE_SIZE_EXCEEDS_MB, String.valueOf(s.getFileSizeExceedsMb()));
        Settings.put(KEY_RICH_DIFF, String.valueOf(s.isShowRichDiffForNotebooks()));
        Settings.put(KEY_IGNORE_OUTPUTS, String.valueOf(s.isIgnoreOutputsInDiff()));

        notifyListeners();
    }

    public void resetDefaults() {
        setSettings(new JupyterVcsSettings());
    }

    public void clear() {
        resetDefaults();
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
