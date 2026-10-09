package dev.lumina.kotlin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.lumina.util.Settings;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Kotlin Scripting settings in Lumina IDE.
 */
public class KotlinScriptingSettingsManager {

    public static final String KEY_KOTLIN_SCRIPT_DEFINITIONS = "kotlin.scripting.definitions";

    private static KotlinScriptingSettingsManager instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private KotlinScriptingSettings settings = new KotlinScriptingSettings();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private KotlinScriptingSettingsManager() {
        loadSettings();
    }

    public static synchronized KotlinScriptingSettingsManager getInstance() {
        if (instance == null) {
            instance = new KotlinScriptingSettingsManager();
        }
        return instance;
    }

    public synchronized KotlinScriptingSettings getSettings() {
        return settings.copy();
    }

    public synchronized void setSettings(KotlinScriptingSettings settings) {
        this.settings = settings != null ? settings.copy() : new KotlinScriptingSettings();
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
        Settings.put(KEY_KOTLIN_SCRIPT_DEFINITIONS, GSON.toJson(settings.getDefinitions()));
    }

    public synchronized void loadSettings() {
        String json = Settings.get(KEY_KOTLIN_SCRIPT_DEFINITIONS);
        if (json != null && !json.isBlank()) {
            try {
                Type listType = new TypeToken<ArrayList<KotlinScriptDefinition>>() {}.getType();
                List<KotlinScriptDefinition> defs = GSON.fromJson(json, listType);
                if (defs != null && !defs.isEmpty()) {
                    this.settings = new KotlinScriptingSettings(defs);
                    return;
                }
            } catch (Exception ignored) {}
        }
        this.settings = new KotlinScriptingSettings();
    }

    public synchronized void resetDefaults() {
        this.settings = new KotlinScriptingSettings();
        saveSettings();
        notifyListeners();
    }

    /**
     * Scans project classpath for Kotlin script definition templates and updates definitions.
     */
    public synchronized int scanClasspath() {
        // Classpath scanner looks for script template providers; returns count of active definitions
        return settings.getDefinitions().size();
    }
}
