package dev.lumina.sql;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Languages & Frameworks > SQL Resolution Scopes settings in Lumina IDE.
 */
public class SqlResolutionScopesSettingsManager {

    public static final String KEY_SQL_RESOLUTION_PROJECT = "sql.resolution.project.mapping";
    public static final String KEY_SQL_RESOLUTION_MAPPINGS = "sql.resolution.mappings";

    private static volatile SqlResolutionScopesSettingsManager instance;
    private SqlResolutionScopesSettings currentSettings;
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private SqlResolutionScopesSettingsManager() {
        loadSettings();
    }

    public static SqlResolutionScopesSettingsManager getInstance() {
        if (instance == null) {
            synchronized (SqlResolutionScopesSettingsManager.class) {
                if (instance == null) {
                    instance = new SqlResolutionScopesSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized SqlResolutionScopesSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(SqlResolutionScopesSettings settings) {
        if (settings == null) return;
        this.currentSettings = settings.clone();
        saveSettings();
        notifyListeners();
    }

    public synchronized void loadSettings() {
        SqlResolutionScopesSettings s = new SqlResolutionScopesSettings();

        String project = Settings.get(KEY_SQL_RESOLUTION_PROJECT);
        if (project != null && !project.isBlank()) {
            s.setProjectMapping(project);
        }

        String mappingsStr = Settings.get(KEY_SQL_RESOLUTION_MAPPINGS);
        if (mappingsStr != null && !mappingsStr.isBlank()) {
            List<SqlResolutionScopeMapping> list = new ArrayList<>();
            String[] entries = mappingsStr.split("###");
            for (String entry : entries) {
                if (!entry.isBlank()) {
                    String[] parts = entry.split("@@@", 2);
                    String path = parts[0];
                    String scope = parts.length > 1 ? parts[1] : "<Default>";
                    list.add(new SqlResolutionScopeMapping(path, scope));
                }
            }
            s.setMappings(list);
        }

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.put(KEY_SQL_RESOLUTION_PROJECT, currentSettings.getProjectMapping());

        StringBuilder sb = new StringBuilder();
        for (SqlResolutionScopeMapping m : currentSettings.getMappings()) {
            if (sb.length() > 0) sb.append("###");
            sb.append(m.getPath() != null ? m.getPath() : "")
              .append("@@@")
              .append(m.getResolutionScope() != null ? m.getResolutionScope() : "<Default>");
        }
        Settings.put(KEY_SQL_RESOLUTION_MAPPINGS, sb.toString());
    }

    /**
     * Dynamically resolves the SQL resolution scope for a given file or directory path.
     * Searches path mappings first (longest prefix match), falls back to project mapping.
     */
    public synchronized String resolveScope(String path) {
        if (currentSettings == null) {
            loadSettings();
        }
        if (path != null && !path.isBlank()) {
            String bestMatchScope = null;
            int bestMatchLen = -1;
            for (SqlResolutionScopeMapping mapping : currentSettings.getMappings()) {
                String mapPath = mapping.getPath();
                if (mapPath != null && !mapPath.isBlank()) {
                    if (path.equals(mapPath) || path.startsWith(mapPath)) {
                        if (mapPath.length() > bestMatchLen) {
                            bestMatchLen = mapPath.length();
                            bestMatchScope = mapping.getResolutionScope();
                        }
                    }
                }
            }
            if (bestMatchScope != null) {
                return bestMatchScope;
            }
        }

        String proj = currentSettings.getProjectMapping();
        return proj != null ? proj : "<Default>";
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
