package dev.lumina.database;

import dev.lumina.util.Settings;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic configuration manager for Database Query Execution settings in Lumina IDE.
 */
public class DatabaseQueryExecutionSettingsManager {

    private static final String KEY_PREFIX = "database.query_execution.";
    private static volatile DatabaseQueryExecutionSettingsManager instance;

    private DatabaseQueryExecutionSettings currentSettings;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private DatabaseQueryExecutionSettingsManager() {
        this.currentSettings = loadSettings();
    }

    public static DatabaseQueryExecutionSettingsManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseQueryExecutionSettingsManager.class) {
                if (instance == null) {
                    instance = new DatabaseQueryExecutionSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized DatabaseQueryExecutionSettings getSettings() {
        if (currentSettings == null) {
            currentSettings = loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(DatabaseQueryExecutionSettings newSettings) {
        if (newSettings == null) {
            this.currentSettings = new DatabaseQueryExecutionSettings();
        } else {
            this.currentSettings = newSettings.clone();
        }
        saveSettings();
        notifyListeners();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            try {
                listener.run();
            } catch (Exception ignored) {
            }
        }
    }

    private DatabaseQueryExecutionSettings loadSettings() {
        DatabaseQueryExecutionSettings s = new DatabaseQueryExecutionSettings();
        String inside = Settings.get(KEY_PREFIX + "when_caret_inside");
        if (inside != null && !inside.isBlank()) {
            s.setWhenCaretInside(inside);
        }
        String outside = Settings.get(KEY_PREFIX + "when_caret_outside");
        if (outside != null && !outside.isBlank()) {
            s.setWhenCaretOutside(outside);
        }
        String sel = Settings.get(KEY_PREFIX + "for_selection");
        if (sel != null && !sel.isBlank()) {
            s.setForSelection(sel);
        }
        String openTab = Settings.get(KEY_PREFIX + "open_results_new_tab");
        if (openTab != null) {
            s.setOpenResultsInNewTab(Boolean.parseBoolean(openTab));
        }
        String split = Settings.get(KEY_PREFIX + "split_script");
        if (split != null && !split.isBlank()) {
            s.setSplitScript(split);
        }
        String review = Settings.get(KEY_PREFIX + "review_params");
        if (review != null) {
            s.setReviewParametersBeforeExecution(Boolean.parseBoolean(review));
        }
        String warn = Settings.get(KEY_PREFIX + "show_warning_unsafe");
        if (warn != null) {
            s.setShowWarningUnsafeQueries(Boolean.parseBoolean(warn));
        }
        return s;
    }

    private void saveSettings() {
        if (currentSettings == null) return;
        Settings.put(KEY_PREFIX + "when_caret_inside", currentSettings.getWhenCaretInside());
        Settings.put(KEY_PREFIX + "when_caret_outside", currentSettings.getWhenCaretOutside());
        Settings.put(KEY_PREFIX + "for_selection", currentSettings.getForSelection());
        Settings.put(KEY_PREFIX + "open_results_new_tab", String.valueOf(currentSettings.isOpenResultsInNewTab()));
        Settings.put(KEY_PREFIX + "split_script", currentSettings.getSplitScript());
        Settings.put(KEY_PREFIX + "review_params", String.valueOf(currentSettings.isReviewParametersBeforeExecution()));
        Settings.put(KEY_PREFIX + "show_warning_unsafe", String.valueOf(currentSettings.isShowWarningUnsafeQueries()));
    }
}
