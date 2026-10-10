package dev.lumina.stylesheets;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Languages & Frameworks > Style Sheets > Dialects settings in Lumina IDE.
 */
public class CssDialectsSettingsManager {

    public static final String KEY_CSS_DIALECT_PROJECT = "stylesheets.dialects.project";
    public static final String KEY_CSS_DIALECT_MAPPINGS = "stylesheets.dialects.mappings";

    private static volatile CssDialectsSettingsManager instance;
    private CssDialectsSettings currentSettings;
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private CssDialectsSettingsManager() {
        loadSettings();
    }

    public static CssDialectsSettingsManager getInstance() {
        if (instance == null) {
            synchronized (CssDialectsSettingsManager.class) {
                if (instance == null) {
                    instance = new CssDialectsSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized CssDialectsSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(CssDialectsSettings settings) {
        if (settings == null) return;
        this.currentSettings = settings.clone();
        saveSettings();
        notifyListeners();
    }

    public synchronized void loadSettings() {
        CssDialectsSettings s = new CssDialectsSettings();

        String project = Settings.get(KEY_CSS_DIALECT_PROJECT);
        if (project != null && !project.isBlank()) {
            s.setProjectDialect(project);
        }

        String mappingsStr = Settings.get(KEY_CSS_DIALECT_MAPPINGS);
        if (mappingsStr != null && !mappingsStr.isBlank()) {
            List<CssDialectMapping> list = new ArrayList<>();
            String[] entries = mappingsStr.split("###");
            for (String entry : entries) {
                if (!entry.isBlank()) {
                    String[] parts = entry.split("@@@", 2);
                    String path = parts[0];
                    String dialect = parts.length > 1 ? parts[1] : "<None>";
                    list.add(new CssDialectMapping(path, dialect));
                }
            }
            s.setMappings(list);
        }

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.put(KEY_CSS_DIALECT_PROJECT, currentSettings.getProjectDialect());

        StringBuilder sb = new StringBuilder();
        for (CssDialectMapping m : currentSettings.getMappings()) {
            if (sb.length() > 0) sb.append("###");
            sb.append(m.getPath() != null ? m.getPath() : "")
              .append("@@@")
              .append(m.getDialect() != null ? m.getDialect() : "<None>");
        }
        Settings.put(KEY_CSS_DIALECT_MAPPINGS, sb.toString());
    }

    /**
     * Dynamically resolves the effective CSS dialect for a given file or directory path.
     * Searches path mappings first (longest prefix match), falls back to project dialect, then default file extension inference.
     */
    public synchronized String resolveDialect(String path) {
        if (currentSettings == null) {
            loadSettings();
        }
        if (path != null && !path.isBlank()) {
            String bestMatchDialect = null;
            int bestMatchLen = -1;
            for (CssDialectMapping mapping : currentSettings.getMappings()) {
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

        // Infer from file extension if path provided
        if (path != null) {
            String lower = path.toLowerCase();
            if (lower.endsWith(".scss")) return "SCSS";
            if (lower.endsWith(".sass")) return "Sass";
            if (lower.endsWith(".less")) return "Less";
            if (lower.endsWith(".styl") || lower.endsWith(".stylus")) return "Stylus";
            if (lower.endsWith(".pcss") || lower.endsWith(".postcss")) return "PostCSS";
            if (lower.endsWith(".css")) return "CSS";
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
