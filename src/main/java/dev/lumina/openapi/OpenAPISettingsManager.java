package dev.lumina.openapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Languages & Frameworks > OpenAPI Specifications settings in Lumina IDE.
 */
public class OpenAPISettingsManager {

    public static final String KEY_OPENAPI_SETTINGS = "openapi.specifications.settings";

    private static OpenAPISettingsManager instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private OpenAPISettings currentSettings = new OpenAPISettings();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private OpenAPISettingsManager() {
        loadSettings();
    }

    public static synchronized OpenAPISettingsManager getInstance() {
        if (instance == null) {
            instance = new OpenAPISettingsManager();
        }
        return instance;
    }

    public synchronized OpenAPISettings getSettings() {
        return currentSettings.copy();
    }

    public synchronized void setSettings(OpenAPISettings settings) {
        this.currentSettings = settings != null ? settings.copy() : new OpenAPISettings();
        saveSettings();
        notifyListeners();
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

    public synchronized void saveSettings() {
        Settings.put(KEY_OPENAPI_SETTINGS, GSON.toJson(currentSettings));
    }

    public synchronized void loadSettings() {
        String json = Settings.get(KEY_OPENAPI_SETTINGS);
        if (json != null && !json.isBlank()) {
            try {
                OpenAPISettings parsed = GSON.fromJson(json, OpenAPISettings.class);
                if (parsed != null) {
                    this.currentSettings = parsed;
                    return;
                }
            } catch (Exception ignored) {}
        }
        this.currentSettings = new OpenAPISettings();
    }

    public synchronized void resetDefaults() {
        this.currentSettings = new OpenAPISettings();
        saveSettings();
        notifyListeners();
    }
}
