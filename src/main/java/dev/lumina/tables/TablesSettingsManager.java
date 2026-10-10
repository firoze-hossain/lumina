package dev.lumina.tables;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Languages & Frameworks > Tables settings in Lumina IDE.
 */
public class TablesSettingsManager {

    public static final String KEY_COLUMN_STATISTICS_MODE = "tables.column_statistics_mode";
    public static final String KEY_AUTO_COMPACT_FOR_SMALL_TABLES = "tables.auto_compact_for_small_tables";
    public static final String KEY_DISPLAY_TENSORS_AS_TABLE = "tables.display_tensors_as_table";
    public static final String KEY_LIMIT_RENDERED_COLUMNS = "tables.limit_rendered_columns";
    public static final String KEY_MAX_RENDERED_COLUMNS = "tables.max_rendered_columns";
    public static final String KEY_ENABLE_LOCAL_FILTERS_BY_DEFAULT = "tables.enable_local_filters_by_default";

    private static volatile TablesSettingsManager instance;
    private TablesSettings currentSettings;
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private TablesSettingsManager() {
        loadSettings();
    }

    public static TablesSettingsManager getInstance() {
        if (instance == null) {
            synchronized (TablesSettingsManager.class) {
                if (instance == null) {
                    instance = new TablesSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized TablesSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return new TablesSettings(currentSettings);
    }

    public synchronized void setSettings(TablesSettings settings) {
        if (settings == null) return;
        this.currentSettings = new TablesSettings(settings);
        saveSettings();
        notifyListeners();
    }

    public synchronized void loadSettings() {
        TablesSettings s = new TablesSettings();

        String mode = Settings.get(KEY_COLUMN_STATISTICS_MODE);
        if (mode != null && !mode.isBlank()) {
            s.setColumnStatisticsMode(mode);
        }

        String autoCompact = Settings.get(KEY_AUTO_COMPACT_FOR_SMALL_TABLES);
        if (autoCompact != null) {
            s.setAutoCompactForSmallTables(Boolean.parseBoolean(autoCompact));
        }

        String tensors = Settings.get(KEY_DISPLAY_TENSORS_AS_TABLE);
        if (tensors != null) {
            s.setDisplayTensorsAsTable(Boolean.parseBoolean(tensors));
        }

        String limitCols = Settings.get(KEY_LIMIT_RENDERED_COLUMNS);
        if (limitCols != null) {
            s.setLimitRenderedColumns(Boolean.parseBoolean(limitCols));
        }

        String maxCols = Settings.get(KEY_MAX_RENDERED_COLUMNS);
        if (maxCols != null) {
            try {
                s.setMaxRenderedColumns(Integer.parseInt(maxCols.trim()));
            } catch (NumberFormatException ignored) {}
        }

        String localFilters = Settings.get(KEY_ENABLE_LOCAL_FILTERS_BY_DEFAULT);
        if (localFilters != null) {
            s.setEnableLocalFiltersByDefault(Boolean.parseBoolean(localFilters));
        }

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.put(KEY_COLUMN_STATISTICS_MODE, currentSettings.getColumnStatisticsMode());
        Settings.put(KEY_AUTO_COMPACT_FOR_SMALL_TABLES, String.valueOf(currentSettings.isAutoCompactForSmallTables()));
        Settings.put(KEY_DISPLAY_TENSORS_AS_TABLE, String.valueOf(currentSettings.isDisplayTensorsAsTable()));
        Settings.put(KEY_LIMIT_RENDERED_COLUMNS, String.valueOf(currentSettings.isLimitRenderedColumns()));
        Settings.put(KEY_MAX_RENDERED_COLUMNS, String.valueOf(currentSettings.getMaxRenderedColumns()));
        Settings.put(KEY_ENABLE_LOCAL_FILTERS_BY_DEFAULT, String.valueOf(currentSettings.isEnableLocalFiltersByDefault()));
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : changeListeners) {
            try {
                listener.run();
            } catch (Throwable ignored) {}
        }
    }
}
