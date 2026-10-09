package dev.lumina.javafx;

import dev.lumina.util.Settings;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Settings manager for JavaFX (e.g. SceneBuilder executable path) in Lumina IDE.
 */
public class JavaFXSettingsManager {

    public static final String KEY_JAVAFX_SCENEBUILDER_PATH = "javafx.scenebuilder.path";

    private static JavaFXSettingsManager instance;
    private String pathToSceneBuilder = "";
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private JavaFXSettingsManager() {
        loadSettings();
    }

    public static synchronized JavaFXSettingsManager getInstance() {
        if (instance == null) {
            instance = new JavaFXSettingsManager();
        }
        return instance;
    }

    public synchronized String getPathToSceneBuilder() {
        return pathToSceneBuilder;
    }

    public synchronized void setPathToSceneBuilder(String path) {
        this.pathToSceneBuilder = path != null ? path : "";
        saveSettings();
        notifyListeners();
    }

    /**
     * Dynamically detects the SceneBuilder executable on the user's system.
     */
    public synchronized String detectSceneBuilderPath() {
        List<String> candidates = List.of(
                "/opt/SceneBuilder/bin/SceneBuilder",
                "/opt/SceneBuilder/SceneBuilder",
                "/usr/bin/scenebuilder",
                "/usr/local/bin/scenebuilder",
                "/Applications/SceneBuilder.app/Contents/MacOS/SceneBuilder",
                System.getProperty("user.home") + "/SceneBuilder/SceneBuilder",
                "C:\\Program Files\\SceneBuilder\\SceneBuilder.exe",
                "C:\\Program Files (x86)\\SceneBuilder\\SceneBuilder.exe",
                System.getenv("LOCALAPPDATA") != null ? System.getenv("LOCALAPPDATA") + "\\SceneBuilder\\SceneBuilder.exe" : ""
        );

        for (String c : candidates) {
            if (c.isBlank()) continue;
            File f = new File(c);
            if (f.exists() && f.canExecute()) {
                return f.getAbsolutePath();
            }
        }
        return "";
    }

    public synchronized void addChangeListener(Runnable listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public synchronized void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable r : changeListeners) {
            try {
                r.run();
            } catch (Exception ignored) {}
        }
    }

    public synchronized void resetDefaults() {
        this.pathToSceneBuilder = "";
        saveSettings();
        notifyListeners();
    }

    public synchronized void saveSettings() {
        Settings.put(KEY_JAVAFX_SCENEBUILDER_PATH, pathToSceneBuilder);
    }

    public synchronized void loadSettings() {
        String val = Settings.get(KEY_JAVAFX_SCENEBUILDER_PATH);
        this.pathToSceneBuilder = val != null ? val : "";
    }
}
