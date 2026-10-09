package dev.lumina.markdown;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Languages & Frameworks > Markdown settings in Lumina IDE.
 */
public class MarkdownLanguageSettingsManager {

    public static final String KEY_MARKDOWN_LANGUAGE_SETTINGS = "markdown.language.settings";

    private static MarkdownLanguageSettingsManager instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private MarkdownLanguageSettings currentSettings = new MarkdownLanguageSettings();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private MarkdownLanguageSettingsManager() {
        loadSettings();
    }

    public static synchronized MarkdownLanguageSettingsManager getInstance() {
        if (instance == null) {
            instance = new MarkdownLanguageSettingsManager();
        }
        return instance;
    }

    public synchronized MarkdownLanguageSettings getSettings() {
        return currentSettings.copy();
    }

    public synchronized void setSettings(MarkdownLanguageSettings settings) {
        this.currentSettings = settings != null ? settings.copy() : new MarkdownLanguageSettings();
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
        Settings.put(KEY_MARKDOWN_LANGUAGE_SETTINGS, GSON.toJson(currentSettings));
    }

    public synchronized void loadSettings() {
        String json = Settings.get(KEY_MARKDOWN_LANGUAGE_SETTINGS);
        if (json != null && !json.isBlank()) {
            try {
                MarkdownLanguageSettings parsed = GSON.fromJson(json, MarkdownLanguageSettings.class);
                if (parsed != null) {
                    this.currentSettings = parsed;
                    return;
                }
            } catch (Exception ignored) {}
        }
        this.currentSettings = new MarkdownLanguageSettings();
    }

    public synchronized void resetDefaults() {
        this.currentSettings = new MarkdownLanguageSettings();
        saveSettings();
        notifyListeners();
    }
}
