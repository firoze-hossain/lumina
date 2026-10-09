package dev.lumina.go;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.lumina.project.GoMetadata;
import dev.lumina.util.Settings;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Settings manager for Go in Lumina IDE.
 * Dynamically probes GOROOT, GOPATH, and Go Modules without hardcoding.
 */
public class GoSettingsManager {

    public static final String KEY_GO_SETTINGS = "go.configuration.settings";

    private static GoSettingsManager instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private GoSettings settings = new GoSettings();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private GoSettingsManager() {
        loadSettings();
    }

    public static synchronized GoSettingsManager getInstance() {
        if (instance == null) {
            instance = new GoSettingsManager();
        }
        return instance;
    }

    public synchronized GoSettings getSettings() {
        return settings.copy();
    }

    public synchronized void setSettings(GoSettings newSettings) {
        this.settings = newSettings != null ? newSettings.copy() : new GoSettings();
        saveSettings();
        notifyListeners();
    }

    public synchronized void saveSettings() {
        Settings.put(KEY_GO_SETTINGS, GSON.toJson(settings));
    }

    public synchronized void loadSettings() {
        String json = Settings.get(KEY_GO_SETTINGS);
        if (json != null && !json.isBlank()) {
            try {
                GoSettings parsed = GSON.fromJson(json, GoSettings.class);
                if (parsed != null) {
                    this.settings = parsed;
                    return;
                }
            } catch (Exception ignored) {}
        }
        initDefaultSettings();
    }

    public synchronized void resetDefaults() {
        initDefaultSettings();
        saveSettings();
        notifyListeners();
    }

    private void initDefaultSettings() {
        settings = new GoSettings();
        // Dynamically detect GOROOT if available
        List<GoMetadata.GoSdk> discovered = GoMetadata.discoverGoRoots();
        if (!discovered.isEmpty()) {
            GoMetadata.GoSdk first = discovered.get(0);
            settings.setGoRootPath(first.path());
            settings.setGoRootVersion(first.version());
        }
        // Dynamically detect GOPATH if available in environment
        String envGoPath = System.getenv("GOPATH");
        if (envGoPath != null && !envGoPath.isBlank()) {
            settings.getGlobalGoPaths().add(envGoPath.trim());
        }
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
        for (Runnable listener : changeListeners) {
            try {
                listener.run();
            } catch (Exception ignored) {}
        }
    }

    public static boolean isGoModuleWorkspace() {
        String userDir = System.getProperty("user.dir", ".");
        File goMod = new File(userDir, "go.mod");
        return goMod.isFile();
    }
}
