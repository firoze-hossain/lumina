package dev.lumina.javascript;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.lumina.util.Settings;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Settings manager for JavaScript & Code Quality Tools in Lumina IDE.
 */
public class JavaScriptSettingsManager {

    public static final String KEY_JS_LANGUAGE_VERSION = "javascript.language.version";
    public static final String KEY_JS_ESLINT_SETTINGS = "javascript.codequality.eslint";
    public static final String KEY_JS_JSHINT_SETTINGS = "javascript.codequality.jshint";
    public static final String KEY_JS_LIBRARIES = "javascript.libraries";
    public static final String KEY_JS_PRETTIER_SETTINGS = "javascript.prettier";
    public static final String KEY_JS_STYLED_COMPONENTS_SETTINGS = "javascript.styledcomponents";
    public static final String KEY_JS_VITE_SETTINGS = "javascript.vite";
    public static final String KEY_JS_WEBPACK_SETTINGS = "javascript.webpack";
    public static final String KEY_JS_RUNTIME_SETTINGS = "javascript.runtime";

    private static JavaScriptSettingsManager instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private JavaScriptLanguageVersion languageVersion = JavaScriptLanguageVersion.ECMASCRIPT_6_PLUS;
    private ESLintSettings eslintSettings = new ESLintSettings();
    private JSHintSettings jshintSettings = new JSHintSettings();
    private List<JavaScriptLibrary> libraries = new ArrayList<>();
    private PrettierSettings prettierSettings = new PrettierSettings();
    private StyledComponentsSettings styledComponentsSettings = new StyledComponentsSettings();
    private ViteSettings viteSettings = new ViteSettings();
    private WebpackSettings webpackSettings = new WebpackSettings();
    private JavaScriptRuntimeSettings runtimeSettings = new JavaScriptRuntimeSettings();

    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private JavaScriptSettingsManager() {
        initDefaultLibraries();
        loadSettings();
    }

    public static synchronized JavaScriptSettingsManager getInstance() {
        if (instance == null) {
            instance = new JavaScriptSettingsManager();
        }
        return instance;
    }

    private void initDefaultLibraries() {
        libraries.clear();
        libraries.add(new JavaScriptLibrary("HTML", true, "Predefined"));
        libraries.add(new JavaScriptLibrary("HTTP Pre-Request and Response Handler", false, "Predefined"));
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
    // JavaScript Libraries
    // ============================================================

    public synchronized List<JavaScriptLibrary> getLibraries() {
        List<JavaScriptLibrary> copy = new ArrayList<>();
        for (JavaScriptLibrary lib : libraries) {
            copy.add(lib.copy());
        }
        return copy;
    }

    public synchronized void setLibraries(List<JavaScriptLibrary> libs) {
        this.libraries.clear();
        if (libs != null) {
            for (JavaScriptLibrary lib : libs) {
                this.libraries.add(lib.copy());
            }
        }
        saveSettings();
        notifyListeners();
    }

    // ============================================================
    // Prettier Settings
    // ============================================================

    public synchronized PrettierSettings getPrettierSettings() {
        return prettierSettings.copy();
    }

    public synchronized void setPrettierSettings(PrettierSettings settings) {
        this.prettierSettings = settings != null ? settings.copy() : new PrettierSettings();
        saveSettings();
        notifyListeners();
    }

    // ============================================================
    // Styled Components Settings
    // ============================================================

    public synchronized StyledComponentsSettings getStyledComponentsSettings() {
        return styledComponentsSettings.copy();
    }

    public synchronized void setStyledComponentsSettings(StyledComponentsSettings settings) {
        this.styledComponentsSettings = settings != null ? settings.copy() : new StyledComponentsSettings();
        saveSettings();
        notifyListeners();
    }

    // ============================================================
    // Vite Settings
    // ============================================================

    public synchronized ViteSettings getViteSettings() {
        return viteSettings.copy();
    }

    public synchronized void setViteSettings(ViteSettings settings) {
        this.viteSettings = settings != null ? settings.copy() : new ViteSettings();
        saveSettings();
        notifyListeners();
    }

    // ============================================================
    // Webpack Settings
    // ============================================================

    public synchronized WebpackSettings getWebpackSettings() {
        return webpackSettings.copy();
    }

    public synchronized void setWebpackSettings(WebpackSettings settings) {
        this.webpackSettings = settings != null ? settings.copy() : new WebpackSettings();
        saveSettings();
        notifyListeners();
    }

    // ============================================================
    // Runtime Settings
    // ============================================================

    public synchronized JavaScriptRuntimeSettings getRuntimeSettings() {
        return runtimeSettings.copy();
    }

    public synchronized void setRuntimeSettings(JavaScriptRuntimeSettings settings) {
        this.runtimeSettings = settings != null ? settings.copy() : new JavaScriptRuntimeSettings();
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
        initDefaultLibraries();
        this.prettierSettings = new PrettierSettings();
        this.styledComponentsSettings = new StyledComponentsSettings();
        this.viteSettings = new ViteSettings();
        this.webpackSettings = new WebpackSettings();
        this.runtimeSettings = new JavaScriptRuntimeSettings();
        saveSettings();
        notifyListeners();
    }

    public synchronized void saveSettings() {
        Settings.put(KEY_JS_LANGUAGE_VERSION, languageVersion.getDisplayName());
        Settings.put(KEY_JS_ESLINT_SETTINGS, GSON.toJson(eslintSettings));
        Settings.put(KEY_JS_JSHINT_SETTINGS, GSON.toJson(jshintSettings));
        Settings.put(KEY_JS_LIBRARIES, GSON.toJson(libraries));
        Settings.put(KEY_JS_PRETTIER_SETTINGS, GSON.toJson(prettierSettings));
        Settings.put(KEY_JS_STYLED_COMPONENTS_SETTINGS, GSON.toJson(styledComponentsSettings));
        Settings.put(KEY_JS_VITE_SETTINGS, GSON.toJson(viteSettings));
        Settings.put(KEY_JS_WEBPACK_SETTINGS, GSON.toJson(webpackSettings));
        Settings.put(KEY_JS_RUNTIME_SETTINGS, GSON.toJson(runtimeSettings));
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

        // 4. Libraries
        String libsJson = Settings.get(KEY_JS_LIBRARIES);
        if (libsJson != null && !libsJson.isBlank()) {
            try {
                Type listType = new TypeToken<ArrayList<JavaScriptLibrary>>() {}.getType();
                List<JavaScriptLibrary> parsed = GSON.fromJson(libsJson, listType);
                if (parsed != null && !parsed.isEmpty()) {
                    this.libraries = parsed;
                }
            } catch (Exception ignored) {}
        }

        // 5. Prettier
        String prettierJson = Settings.get(KEY_JS_PRETTIER_SETTINGS);
        if (prettierJson != null && !prettierJson.isBlank()) {
            try {
                PrettierSettings parsed = GSON.fromJson(prettierJson, PrettierSettings.class);
                if (parsed != null) {
                    this.prettierSettings = parsed;
                }
            } catch (Exception ignored) {}
        } else {
            this.prettierSettings = new PrettierSettings();
        }

        // 6. Styled Components
        String styledJson = Settings.get(KEY_JS_STYLED_COMPONENTS_SETTINGS);
        if (styledJson != null && !styledJson.isBlank()) {
            try {
                StyledComponentsSettings parsed = GSON.fromJson(styledJson, StyledComponentsSettings.class);
                if (parsed != null) {
                    this.styledComponentsSettings = parsed;
                }
            } catch (Exception ignored) {}
        } else {
            this.styledComponentsSettings = new StyledComponentsSettings();
        }

        // 7. Vite
        String viteJson = Settings.get(KEY_JS_VITE_SETTINGS);
        if (viteJson != null && !viteJson.isBlank()) {
            try {
                ViteSettings parsed = GSON.fromJson(viteJson, ViteSettings.class);
                if (parsed != null) {
                    this.viteSettings = parsed;
                }
            } catch (Exception ignored) {}
        } else {
            this.viteSettings = new ViteSettings();
        }

        // 8. Webpack
        String webpackJson = Settings.get(KEY_JS_WEBPACK_SETTINGS);
        if (webpackJson != null && !webpackJson.isBlank()) {
            try {
                WebpackSettings parsed = GSON.fromJson(webpackJson, WebpackSettings.class);
                if (parsed != null) {
                    this.webpackSettings = parsed;
                }
            } catch (Exception ignored) {}
        } else {
            this.webpackSettings = new WebpackSettings();
        }

        // 9. Runtime Settings
        String runtimeJson = Settings.get(KEY_JS_RUNTIME_SETTINGS);
        if (runtimeJson != null && !runtimeJson.isBlank()) {
            try {
                JavaScriptRuntimeSettings parsed = GSON.fromJson(runtimeJson, JavaScriptRuntimeSettings.class);
                if (parsed != null) {
                    this.runtimeSettings = parsed;
                }
            } catch (Exception ignored) {}
        } else {
            this.runtimeSettings = new JavaScriptRuntimeSettings();
        }
    }
}
