package dev.lumina.javascript;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Settings manager for JavaScript & Code Quality Tools matching IntelliJ IDEA.
 */
public class JavaScriptSettingsManager {

    public static final String KEY_JS_LANGUAGE_VERSION = "javascript.language.version";
    public static final String KEY_JS_ESLINT_SETTINGS = "javascript.codequality.eslint";
    public static final String KEY_JS_JSHINT_SETTINGS = "javascript.codequality.jshint";

    private static JavaScriptSettingsManager instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private JavaScriptLanguageVersion languageVersion = JavaScriptLanguageVersion.ECMASCRIPT_6_PLUS;
    private ESLintSettings eslintSettings = new ESLintSettings();
    private JSHintSettings jshintSettings = new JSHintSettings();

    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private JavaScriptSettingsManager() {
        loadSettings();
    }

    public static synchronized JavaScriptSettingsManager getInstance() {
        if (instance == null) {
            instance = new JavaScriptSettingsManager();
        }
        return instance;
    }

    // ============================================================
    // Language Version
    // ============================================================

    public synchronized JavaScriptLanguageVersion getLanguageVersion() {
        return languageVersion;
    }

    public synchronized void setLanguageVersion(JavaScriptLanguageVersion version) {
        this.languageVersion = version != null ? version : JavaScriptLanguageVersion.ECMASCRIPT_6_PLUS;
        saveSettings();
        notifyListeners();
    }

    // ============================================================
    // ESLint Settings
    // ============================================================

    public synchronized ESLintSettings getEslintSettings() {
        return eslintSettings.copy();
    }

    public synchronized void setEslintSettings(ESLintSettings settings) {
        this.eslintSettings = settings != null ? settings.copy() : new ESLintSettings();
        saveSettings();
        notifyListeners();
    }

    // ============================================================
    // JSHint Settings
    // ============================================================

    public synchronized JSHintSettings getJshintSettings() {
        return jshintSettings.copy();
    }

    public synchronized void setJshintSettings(JSHintSettings settings) {
        this.jshintSettings = settings != null ? settings.copy() : new JSHintSettings();
        saveSettings();
        notifyListeners();
    }

    // ============================================================
    // Persistence & Listeners
    // ============================================================

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
        this.languageVersion = JavaScriptLanguageVersion.ECMASCRIPT_6_PLUS;
        this.eslintSettings = new ESLintSettings();
        this.jshintSettings = new JSHintSettings();
        saveSettings();
        notifyListeners();
    }

    public synchronized void saveSettings() {
        Settings.put(KEY_JS_LANGUAGE_VERSION, languageVersion.getDisplayName());
        Settings.put(KEY_JS_ESLINT_SETTINGS, GSON.toJson(eslintSettings));
        Settings.put(KEY_JS_JSHINT_SETTINGS, GSON.toJson(jshintSettings));
    }

    public synchronized void loadSettings() {
        // 1. Language version
        String langVal = Settings.get(KEY_JS_LANGUAGE_VERSION);
        if (langVal != null && !langVal.isBlank()) {
            this.languageVersion = JavaScriptLanguageVersion.fromDisplayName(langVal);
        } else {
            this.languageVersion = JavaScriptLanguageVersion.ECMASCRIPT_6_PLUS;
        }

        // 2. ESLint
        String eslintJson = Settings.get(KEY_JS_ESLINT_SETTINGS);
        if (eslintJson != null && !eslintJson.isBlank()) {
            try {
                ESLintSettings parsed = GSON.fromJson(eslintJson, ESLintSettings.class);
                if (parsed != null) {
                    this.eslintSettings = parsed;
                }
            } catch (Exception ignored) {}
        } else {
            this.eslintSettings = new ESLintSettings();
        }

        // 3. JSHint
        String jshintJson = Settings.get(KEY_JS_JSHINT_SETTINGS);
        if (jshintJson != null && !jshintJson.isBlank()) {
            try {
                JSHintSettings parsed = GSON.fromJson(jshintJson, JSHintSettings.class);
                if (parsed != null) {
                    this.jshintSettings = parsed;
                }
            } catch (Exception ignored) {}
        } else {
            this.jshintSettings = new JSHintSettings();
        }
    }
}
