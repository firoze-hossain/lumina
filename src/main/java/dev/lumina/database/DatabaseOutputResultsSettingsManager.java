package dev.lumina.database;

import dev.lumina.util.Settings;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic configuration manager for Database Output and Results settings in Lumina IDE.
 */
public class DatabaseOutputResultsSettingsManager {

    private static final String KEY_PREFIX = "database.output_results.";
    private static volatile DatabaseOutputResultsSettingsManager instance;

    private DatabaseOutputResultsSettings currentSettings;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private DatabaseOutputResultsSettingsManager() {
        this.currentSettings = loadSettings();
    }

    public static DatabaseOutputResultsSettingsManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseOutputResultsSettingsManager.class) {
                if (instance == null) {
                    instance = new DatabaseOutputResultsSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized DatabaseOutputResultsSettings getSettings() {
        if (currentSettings == null) {
            currentSettings = loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(DatabaseOutputResultsSettings newSettings) {
        if (newSettings == null) {
            this.currentSettings = new DatabaseOutputResultsSettings();
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

    private DatabaseOutputResultsSettings loadSettings() {
        DatabaseOutputResultsSettings s = new DatabaseOutputResultsSettings();
        String ts = Settings.get(KEY_PREFIX + "show_timestamp");
        if (ts != null) {
            s.setShowTimestampForQueryOutput(Boolean.parseBoolean(ts));
        }
        String dbms = Settings.get(KEY_PREFIX + "enable_dbms_output");
        if (dbms != null) {
            s.setEnableDbmsOutput(Boolean.parseBoolean(dbms));
        }
        String editor = Settings.get(KEY_PREFIX + "show_results_in_editor");
        if (editor != null) {
            s.setShowResultsInEditor(Boolean.parseBoolean(editor));
        }
        String title = Settings.get(KEY_PREFIX + "create_title_comment");
        if (title != null) {
            s.setCreateTitleFromComment(Boolean.parseBoolean(title));
        }
        String treat = Settings.get(KEY_PREFIX + "treat_title_after");
        if (treat != null) {
            s.setTreatTextAsTitleAfter(treat);
        }
        String toolwin = Settings.get(KEY_PREFIX + "show_services_tool_window");
        if (toolwin != null && !toolwin.isBlank()) {
            s.setShowServicesToolWindow(toolwin);
        }
        String focus = Settings.get(KEY_PREFIX + "focus_services");
        if (focus != null) {
            s.setFocusOnServicesWindow(Boolean.parseBoolean(focus));
        }
        String newTab = Settings.get(KEY_PREFIX + "open_new_services_tab");
        if (newTab != null) {
            s.setOpenNewServicesTab(Boolean.parseBoolean(newTab));
        }
        String act = Settings.get(KEY_PREFIX + "activate_services_pane");
        if (act != null) {
            s.setActivateServicesOutputPane(Boolean.parseBoolean(act));
        }
        return s;
    }

    private void saveSettings() {
        if (currentSettings == null) return;
        Settings.put(KEY_PREFIX + "show_timestamp", String.valueOf(currentSettings.isShowTimestampForQueryOutput()));
        Settings.put(KEY_PREFIX + "enable_dbms_output", String.valueOf(currentSettings.isEnableDbmsOutput()));
        Settings.put(KEY_PREFIX + "show_results_in_editor", String.valueOf(currentSettings.isShowResultsInEditor()));
        Settings.put(KEY_PREFIX + "create_title_comment", String.valueOf(currentSettings.isCreateTitleFromComment()));
        Settings.put(KEY_PREFIX + "treat_title_after", currentSettings.getTreatTextAsTitleAfter());
        Settings.put(KEY_PREFIX + "show_services_tool_window", currentSettings.getShowServicesToolWindow());
        Settings.put(KEY_PREFIX + "focus_services", String.valueOf(currentSettings.isFocusOnServicesWindow()));
        Settings.put(KEY_PREFIX + "open_new_services_tab", String.valueOf(currentSettings.isOpenNewServicesTab()));
        Settings.put(KEY_PREFIX + "activate_services_pane", String.valueOf(currentSettings.isActivateServicesOutputPane()));
    }
}
