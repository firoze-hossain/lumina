package dev.lumina.sql;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Languages & Frameworks > SQL Dialects settings in Lumina IDE.
 */
public class SqlDialectsSettingsManager {

    public static final String KEY_SQL_DIALECT_GLOBAL = "sql.dialects.global";
    public static final String KEY_SQL_DIALECT_PROJECT = "sql.dialects.project";
    public static final String KEY_SQL_DIALECT_MAPPINGS = "sql.dialects.mappings";

    private static volatile SqlDialectsSettingsManager instance;
    private SqlDialectsSettings currentSettings;
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private SqlDialectsSettingsManager() {
        loadSettings();
    }

    public static SqlDialectsSettingsManager getInstance() {
        if (instance == null) {
            synchronized (SqlDialectsSettingsManager.class) {
                if (instance == null) {
                    instance = new SqlDialectsSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized SqlDialectsSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(SqlDialectsSettings settings) {
        if (settings == null) return;
        this.currentSettings = settings.clone();
        saveSettings();
        notifyListeners();
    }

    public synchronized void loadSettings() {
        SqlDialectsSettings s = new SqlDialectsSettings();

        String global = Settings.get(KEY_SQL_DIALECT_GLOBAL);
        if (global != null && !global.isBlank()) {
            s.setGlobalDialect(global);
        }

        String project = Settings.get(KEY_SQL_DIALECT_PROJECT);
        if (project != null && !project.isBlank()) {
            s.setProjectDialect(project);
        }

        String mappingsStr = Settings.get(KEY_SQL_DIALECT_MAPPINGS);
        if (mappingsStr != null && !mappingsStr.isBlank()) {
            List<SqlDialectMapping> list = new ArrayList<>();
            String[] entries = mappingsStr.split("###");
            for (String entry : entries) {
                if (!entry.isBlank()) {
                    String[] parts = entry.split("@@@", 2);
                    String path = parts[0];
                    String dialect = parts.length > 1 ? parts[1] : "<None>";
                    list.add(new SqlDialectMapping(path, dialect));
                }
            }
            s.setMappings(list);
        }

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.put(KEY_SQL_DIALECT_GLOBAL, currentSettings.getGlobalDialect());
        Settings.put(KEY_SQL_DIALECT_PROJECT, currentSettings.getProjectDialect());

        StringBuilder sb = new StringBuilder();
        for (SqlDialectMapping m : currentSettings.getMappings()) {
            if (sb.length() > 0) sb.append("###");
            sb.append(m.getPath() != null ? m.getPath() : "")
              .append("@@@")
              .append(m.getDialect() != null ? m.getDialect() : "<None>");
        }
        Settings.put(KEY_SQL_DIALECT_MAPPINGS, sb.toString());
    }

    /**
     * Dynamically resolves the effective SQL dialect for a given file or directory path.
     * Searches path mappings first (longest prefix match), falls back to project dialect, then global dialect.
     */
    public synchronized String resolveDialect(String path) {
        if (currentSettings == null) {
            loadSettings();
        }
        if (path != null && !path.isBlank()) {
            String bestMatchDialect = null;
            int bestMatchLen = -1;
            for (SqlDialectMapping mapping : currentSettings.getMappings()) {
                String mapPath = mapping.getPath();
                if (mapPath != null && !mapPath.isBlank()) {
                    if (path.equals(mapPath) || path.startsWith(mapPath)) {
                        if (mapPath.length() > bestMatchLen) {
                            bestMatchLen = mapPath.length();
                            bestMatchDialect = mapping.getDialect();
                        }
                    }
                }
            }
            if (bestMatchDialect != null && !"<None>".equals(bestMatchDialect)) {
                return bestMatchDialect;
            }
        }

        String proj = currentSettings.getProjectDialect();
        if (proj != null && !"<None>".equals(proj)) {
            return proj;
        }

        String glob = currentSettings.getGlobalDialect();
        if (glob != null && !"<None>".equals(glob)) {
            return glob;
        }

        return "<None>";
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable r : changeListeners) {
            try {
                r.run();
            } catch (Exception ignored) {}
        }
    }
}
