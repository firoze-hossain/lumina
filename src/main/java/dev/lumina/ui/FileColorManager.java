package dev.lumina.ui;

import dev.lumina.scope.ScopeManager;
import java.nio.file.Path;
import java.util.*;
import java.util.prefs.Preferences;

/**
 * Centralized manager for File Colors.
 */
public class FileColorManager {

    private static final FileColorManager INSTANCE = new FileColorManager();

    private final Preferences prefs = Preferences.userNodeForPackage(FileColorManager.class);
    private final List<FileColorConfiguration> configurations = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();

    private boolean enableFileColors = true;
    private boolean useInEditorTabs = true;
    private boolean useInProjectView = true;

    // Standard File Color Palette
    public static final Map<String, String> COLOR_HEX_MAP = new LinkedHashMap<>();
    public static final Map<String, String> COLOR_TINT_MAP = new LinkedHashMap<>();

    static {
        COLOR_HEX_MAP.put("Blue", "#264065");
        COLOR_HEX_MAP.put("Gray", "#383A42");
        COLOR_HEX_MAP.put("Green", "#27442D");
        COLOR_HEX_MAP.put("Orange", "#523223");
        COLOR_HEX_MAP.put("Rose", "#4E282C");
        COLOR_HEX_MAP.put("Violet", "#3F2D54");
        COLOR_HEX_MAP.put("Yellow", "#4A3E20");

        COLOR_TINT_MAP.put("Blue", "rgba(38, 64, 101, 0.45)");
        COLOR_TINT_MAP.put("Gray", "rgba(56, 58, 66, 0.45)");
        COLOR_TINT_MAP.put("Green", "rgba(39, 68, 45, 0.45)");
        COLOR_TINT_MAP.put("Orange", "rgba(82, 50, 35, 0.45)");
        COLOR_TINT_MAP.put("Rose", "rgba(78, 40, 44, 0.45)");
        COLOR_TINT_MAP.put("Violet", "rgba(63, 45, 84, 0.45)");
        COLOR_TINT_MAP.put("Yellow", "rgba(74, 62, 32, 0.45)");
    }

    private FileColorManager() {
        loadPreferences();
    }

    public static FileColorManager getInstance() {
        return INSTANCE;
    }

    public synchronized boolean isEnableFileColors() {
        return enableFileColors;
    }

    public synchronized void setEnableFileColors(boolean value) {
        this.enableFileColors = value;
        savePreferences();
        notifyListeners();
    }

    public synchronized boolean isUseInEditorTabs() {
        return useInEditorTabs;
    }

    public synchronized void setUseInEditorTabs(boolean value) {
        this.useInEditorTabs = value;
        savePreferences();
        notifyListeners();
    }

    public synchronized boolean isUseInProjectView() {
        return useInProjectView;
    }

    public synchronized void setUseInProjectView(boolean value) {
        this.useInProjectView = value;
        savePreferences();
        notifyListeners();
    }

    public synchronized List<FileColorConfiguration> getConfigurations() {
        return configurations;
    }

    public synchronized void addConfiguration(FileColorConfiguration config) {
        if (config != null) {
            configurations.add(config);
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized void removeConfiguration(FileColorConfiguration config) {
        if (config != null && configurations.remove(config)) {
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized void moveUp(int index) {
        if (index > 0 && index < configurations.size()) {
            FileColorConfiguration item = configurations.remove(index);
            configurations.add(index - 1, item);
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized void moveDown(int index) {
        if (index >= 0 && index < configurations.size() - 1) {
            FileColorConfiguration item = configurations.remove(index);
            configurations.add(index + 1, item);
            savePreferences();
            notifyListeners();
        }
    }

    /**
     * Resolves the color hex string for a given file path based on scope order.
     * The color of the first matching scope in the list is returned.
     */
    public synchronized String getFileColorHex(Path file, Path projectRoot) {
        if (!enableFileColors || file == null) return null;

        ScopeManager scopeManager = ScopeManager.getInstance();
        for (FileColorConfiguration cfg : configurations) {
            if (scopeManager.matches(cfg.getScopeName(), file, projectRoot)) {
                String colorName = cfg.getColorName();
                if ("Custom".equalsIgnoreCase(colorName)) {
                    return cfg.getCustomHex();
                }
                return COLOR_HEX_MAP.getOrDefault(colorName, "#264065");
            }
        }
        return null;
    }

    public synchronized String getFileColorTint(Path file, Path projectRoot) {
        if (!enableFileColors || file == null) return null;

        ScopeManager scopeManager = ScopeManager.getInstance();
        for (FileColorConfiguration cfg : configurations) {
            if (scopeManager.matches(cfg.getScopeName(), file, projectRoot)) {
                String colorName = cfg.getColorName();
                if ("Custom".equalsIgnoreCase(colorName)) {
                    return cfg.getCustomHex();
                }
                return COLOR_TINT_MAP.getOrDefault(colorName, "rgba(38, 64, 101, 0.45)");
            }
        }
        return null;
    }

    public synchronized void addListener(Runnable listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable l : new ArrayList<>(listeners)) {
            try {
                l.run();
            } catch (Exception ignored) {
            }
        }
    }

    public synchronized void resetToDefaults() {
        configurations.clear();
        configurations.add(new FileColorConfiguration("Non-Project Files", "Yellow", false));
        configurations.add(new FileColorConfiguration("Tests", "Green", false));
        configurations.add(new FileColorConfiguration("Generated Files", "Gray", false));
        enableFileColors = true;
        useInEditorTabs = true;
        useInProjectView = true;
        savePreferences();
        notifyListeners();
    }

    private void loadPreferences() {
        enableFileColors = prefs.getBoolean("file_colors_enabled", true);
        useInEditorTabs = prefs.getBoolean("file_colors_editor_tabs", true);
        useInProjectView = prefs.getBoolean("file_colors_project_view", true);

        configurations.clear();
        String saved = prefs.get("file_colors_configurations", "");
        if (saved.isEmpty()) {
            resetToDefaults();
            return;
        }

        String[] lines = saved.split("\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            String[] parts = line.split("\\|", 4);
            if (parts.length >= 3) {
                String scope = parts[0].trim();
                String color = parts[1].trim();
                boolean vcs = Boolean.parseBoolean(parts[2].trim());
                String custom = parts.length > 3 ? parts[3].trim() : "#2E436E";
                configurations.add(new FileColorConfiguration(scope, color, custom, vcs));
            }
        }

        if (configurations.isEmpty()) {
            resetToDefaults();
        }
    }

    public synchronized void savePreferences() {
        prefs.putBoolean("file_colors_enabled", enableFileColors);
        prefs.putBoolean("file_colors_editor_tabs", useInEditorTabs);
        prefs.putBoolean("file_colors_project_view", useInProjectView);

        StringBuilder sb = new StringBuilder();
        for (FileColorConfiguration cfg : configurations) {
            sb.append(cfg.getScopeName()).append("|")
                    .append(cfg.getColorName()).append("|")
                    .append(cfg.isSharedThroughVcs()).append("|")
                    .append(cfg.getCustomHex()).append("\n");
        }
        prefs.put("file_colors_configurations", sb.toString());
    }
}
