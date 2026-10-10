package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton configuration manager for Tools > Tasks general settings in Lumina IDE.
 */
public class TasksGeneralSettingsManager {

    private static final String KEY_CHANGELIST_FORMAT = "tools.tasks.changelist_format";
    private static final String KEY_BRANCH_FORMAT = "tools.tasks.branch_format";
    private static final String KEY_LOWERCASED = "tools.tasks.lowercased";
    private static final String KEY_REPLACE_SPACES = "tools.tasks.replace_spaces";
    private static final String KEY_HISTORY_LENGTH = "tools.tasks.history_length";
    private static final String KEY_CONN_TIMEOUT = "tools.tasks.conn_timeout";
    private static final String KEY_SHOW_WIDGET_NO_TASKS = "tools.tasks.show_widget_no_tasks";
    private static final String KEY_SAVE_CONTEXT_COMMIT = "tools.tasks.save_context_commit";
    private static final String KEY_ENABLE_CACHE = "tools.tasks.enable_cache";
    private static final String KEY_UPDATE_ISSUES_COUNT = "tools.tasks.update_issues_count";
    private static final String KEY_CACHE_INTERVAL = "tools.tasks.cache_interval";

    private static final TasksGeneralSettingsManager INSTANCE = new TasksGeneralSettingsManager();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private TasksGeneralSettingsManager() {
    }

    public static TasksGeneralSettingsManager getInstance() {
        return INSTANCE;
    }

    public TasksGeneralSettings load() {
        return getSettings();
    }

    public TasksGeneralSettings getSettings() {
        TasksGeneralSettings s = new TasksGeneralSettings();

        String cl = Settings.get(KEY_CHANGELIST_FORMAT);
        if (cl != null) s.setChangelistNameFormat(cl);

        String br = Settings.get(KEY_BRANCH_FORMAT);
        if (br != null) s.setFeatureBranchNameFormat(br);

        String lc = Settings.get(KEY_LOWERCASED);
        if (lc != null) s.setLowercased(Boolean.parseBoolean(lc));

        String rs = Settings.get(KEY_REPLACE_SPACES);
        if (rs != null) s.setReplaceSpacesWith(rs);

        String hl = Settings.get(KEY_HISTORY_LENGTH);
        if (hl != null) {
            try { s.setTaskHistoryLength(Integer.parseInt(hl)); } catch (NumberFormatException ignored) {}
        }

        String to = Settings.get(KEY_CONN_TIMEOUT);
        if (to != null) {
            try { s.setConnectionTimeoutMs(Integer.parseInt(to)); } catch (NumberFormatException ignored) {}
        }

        String sw = Settings.get(KEY_SHOW_WIDGET_NO_TASKS);
        if (sw != null) s.setShowTaskWidgetIfNoActiveTasks(Boolean.parseBoolean(sw));

        String sc = Settings.get(KEY_SAVE_CONTEXT_COMMIT);
        if (sc != null) s.setSaveContextOnCommit(Boolean.parseBoolean(sc));

        String ec = Settings.get(KEY_ENABLE_CACHE);
        if (ec != null) s.setEnableCache(Boolean.parseBoolean(ec));

        String uc = Settings.get(KEY_UPDATE_ISSUES_COUNT);
        if (uc != null) {
            try { s.setUpdateIssuesCount(Integer.parseInt(uc)); } catch (NumberFormatException ignored) {}
        }

        String ci = Settings.get(KEY_CACHE_INTERVAL);
        if (ci != null) {
            try { s.setCacheIntervalMinutes(Integer.parseInt(ci)); } catch (NumberFormatException ignored) {}
        }

        return s;
    }

    public void setSettings(TasksGeneralSettings s) {
        if (s == null) return;

        Settings.put(KEY_CHANGELIST_FORMAT, s.getChangelistNameFormat());
        Settings.put(KEY_BRANCH_FORMAT, s.getFeatureBranchNameFormat());
        Settings.put(KEY_LOWERCASED, String.valueOf(s.isLowercased()));
        Settings.put(KEY_REPLACE_SPACES, s.getReplaceSpacesWith());
        Settings.put(KEY_HISTORY_LENGTH, String.valueOf(s.getTaskHistoryLength()));
        Settings.put(KEY_CONN_TIMEOUT, String.valueOf(s.getConnectionTimeoutMs()));
        Settings.put(KEY_SHOW_WIDGET_NO_TASKS, String.valueOf(s.isShowTaskWidgetIfNoActiveTasks()));
        Settings.put(KEY_SAVE_CONTEXT_COMMIT, String.valueOf(s.isSaveContextOnCommit()));
        Settings.put(KEY_ENABLE_CACHE, String.valueOf(s.isEnableCache()));
        Settings.put(KEY_UPDATE_ISSUES_COUNT, String.valueOf(s.getUpdateIssuesCount()));
        Settings.put(KEY_CACHE_INTERVAL, String.valueOf(s.getCacheIntervalMinutes()));

        notifyListeners();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable r : listeners) {
            try {
                r.run();
            } catch (Throwable ignored) {
            }
        }
    }
}
