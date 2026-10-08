package dev.lumina.debugger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.lumina.util.Settings;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central manager for Debugger settings across Lumina IDE.
 * Dynamically persists and restores configurations, evaluates type renderers,
 * and manages async stack trace rules matching IntelliJ IDEA.
 */
public class DebuggerSettingsManager {

    public static final String KEY_GENERAL_SETTINGS = "debugger.general.settings";
    public static final String KEY_ASYNC_STACKS = "debugger.async.stacks";
    public static final String KEY_DATA_VIEWS = "debugger.data.views";
    public static final String KEY_DATA_VIEWS_JAVA = "debugger.data.views.java";
    public static final String KEY_DATA_VIEWS_JS = "debugger.data.views.js";
    public static final String KEY_TYPE_RENDERERS = "debugger.type.renderers";
    public static final String KEY_HOTSWAP = "debugger.hotswap";
    public static final String KEY_STEPPING = "debugger.stepping";

    private static DebuggerSettingsManager instance;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private DebuggerGeneralSettings generalSettings = new DebuggerGeneralSettings();
    private AsyncStackTraceSettings asyncStackTraceSettings = new AsyncStackTraceSettings();
    private DataViewsSettings dataViewsSettings = new DataViewsSettings();
    private JavaDataViewsSettings javaDataViewsSettings = new JavaDataViewsSettings();
    private JavaScriptDataViewsSettings javaScriptDataViewsSettings = new JavaScriptDataViewsSettings();
    private JavaTypeRendererSettings javaTypeRendererSettings = new JavaTypeRendererSettings();
    private DebuggerHotSwapSettings debuggerHotSwapSettings = new DebuggerHotSwapSettings();
    private DebuggerSteppingSettings debuggerSteppingSettings = new DebuggerSteppingSettings();

    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private DebuggerSettingsManager() {
        loadSettings();
    }

    public static synchronized DebuggerSettingsManager getInstance() {
        if (instance == null) {
            instance = new DebuggerSettingsManager();
        }
        return instance;
    }

    // ============================================================
    // Getters & Setters (returning cloned instances to prevent external mutations)
    // ============================================================

    public DebuggerGeneralSettings getGeneralSettings() {
        return generalSettings.clone();
    }

    public void setGeneralSettings(DebuggerGeneralSettings settings) {
        if (settings != null) {
            this.generalSettings = settings.clone();
            saveSettings();
            notifyChanged();
        }
    }

    public AsyncStackTraceSettings getAsyncStackTraceSettings() {
        return asyncStackTraceSettings.clone();
    }

    public void setAsyncStackTraceSettings(AsyncStackTraceSettings settings) {
        if (settings != null) {
            this.asyncStackTraceSettings = settings.clone();
            saveSettings();
            notifyChanged();
        }
    }

    public DataViewsSettings getDataViewsSettings() {
        return dataViewsSettings.clone();
    }

    public void setDataViewsSettings(DataViewsSettings settings) {
        if (settings != null) {
            this.dataViewsSettings = settings.clone();
            saveSettings();
            notifyChanged();
        }
    }

    public JavaDataViewsSettings getJavaDataViewsSettings() {
        return javaDataViewsSettings.clone();
    }

    public void setJavaDataViewsSettings(JavaDataViewsSettings settings) {
        if (settings != null) {
            this.javaDataViewsSettings = settings.clone();
            saveSettings();
            notifyChanged();
        }
    }

    public JavaScriptDataViewsSettings getJavaScriptDataViewsSettings() {
        return javaScriptDataViewsSettings.clone();
    }

    public void setJavaScriptDataViewsSettings(JavaScriptDataViewsSettings settings) {
        if (settings != null) {
            this.javaScriptDataViewsSettings = settings.clone();
            saveSettings();
            notifyChanged();
        }
    }

    public JavaTypeRendererSettings getJavaTypeRendererSettings() {
        return javaTypeRendererSettings.clone();
    }

    public void setJavaTypeRendererSettings(JavaTypeRendererSettings settings) {
        if (settings != null) {
            this.javaTypeRendererSettings = settings.clone();
            saveSettings();
            notifyChanged();
        }
    }

    public DebuggerHotSwapSettings getDebuggerHotSwapSettings() {
        return debuggerHotSwapSettings.clone();
    }

    public void setDebuggerHotSwapSettings(DebuggerHotSwapSettings settings) {
        if (settings != null) {
            this.debuggerHotSwapSettings = settings.clone();
            saveSettings();
            notifyChanged();
        }
    }

    public DebuggerHotSwapSettings getHotSwapSettings() {
        return getDebuggerHotSwapSettings();
    }

    public void setHotSwapSettings(DebuggerHotSwapSettings settings) {
        setDebuggerHotSwapSettings(settings);
    }

    public DebuggerSteppingSettings getDebuggerSteppingSettings() {
        return debuggerSteppingSettings.clone();
    }

    public void setDebuggerSteppingSettings(DebuggerSteppingSettings settings) {
        if (settings != null) {
            this.debuggerSteppingSettings = settings.clone();
            saveSettings();
            notifyChanged();
        }
    }

    public DebuggerSteppingSettings getSteppingSettings() {
        return getDebuggerSteppingSettings();
    }

    public void setSteppingSettings(DebuggerSteppingSettings settings) {
        setDebuggerSteppingSettings(settings);
    }

    // ============================================================
    // Persistence
    // ============================================================

    public synchronized void saveSettings() {
        try {
            Settings.put(KEY_GENERAL_SETTINGS, GSON.toJson(generalSettings));
            Settings.put(KEY_ASYNC_STACKS, GSON.toJson(asyncStackTraceSettings));
            Settings.put(KEY_DATA_VIEWS, GSON.toJson(dataViewsSettings));
            Settings.put(KEY_DATA_VIEWS_JAVA, GSON.toJson(javaDataViewsSettings));
            Settings.put(KEY_DATA_VIEWS_JS, GSON.toJson(javaScriptDataViewsSettings));
            Settings.put(KEY_TYPE_RENDERERS, GSON.toJson(javaTypeRendererSettings));
            Settings.put(KEY_HOTSWAP, GSON.toJson(debuggerHotSwapSettings));
            Settings.put(KEY_STEPPING, GSON.toJson(debuggerSteppingSettings));
        } catch (Exception ignored) {
        }
    }

    public synchronized void loadSettings() {
        try {
            String gen = Settings.get(KEY_GENERAL_SETTINGS);
            if (gen != null && !gen.isBlank()) {
                DebuggerGeneralSettings loaded = GSON.fromJson(gen, DebuggerGeneralSettings.class);
                if (loaded != null) this.generalSettings = loaded;
            }

            String async = Settings.get(KEY_ASYNC_STACKS);
            if (async != null && !async.isBlank()) {
                AsyncStackTraceSettings loaded = GSON.fromJson(async, AsyncStackTraceSettings.class);
                if (loaded != null) this.asyncStackTraceSettings = loaded;
            }

            String dv = Settings.get(KEY_DATA_VIEWS);
            if (dv != null && !dv.isBlank()) {
                DataViewsSettings loaded = GSON.fromJson(dv, DataViewsSettings.class);
                if (loaded != null) this.dataViewsSettings = loaded;
            }

            String dvj = Settings.get(KEY_DATA_VIEWS_JAVA);
            if (dvj != null && !dvj.isBlank()) {
                JavaDataViewsSettings loaded = GSON.fromJson(dvj, JavaDataViewsSettings.class);
                if (loaded != null) this.javaDataViewsSettings = loaded;
            }

            String dvjs = Settings.get(KEY_DATA_VIEWS_JS);
            if (dvjs != null && !dvjs.isBlank()) {
                JavaScriptDataViewsSettings loaded = GSON.fromJson(dvjs, JavaScriptDataViewsSettings.class);
                if (loaded != null) this.javaScriptDataViewsSettings = loaded;
            }

            String tr = Settings.get(KEY_TYPE_RENDERERS);
            if (tr != null && !tr.isBlank()) {
                JavaTypeRendererSettings loaded = GSON.fromJson(tr, JavaTypeRendererSettings.class);
                if (loaded != null) this.javaTypeRendererSettings = loaded;
            }

            String hs = Settings.get(KEY_HOTSWAP);
            if (hs != null && !hs.isBlank()) {
                DebuggerHotSwapSettings loaded = GSON.fromJson(hs, DebuggerHotSwapSettings.class);
                if (loaded != null) this.debuggerHotSwapSettings = loaded;
            }

            String step = Settings.get(KEY_STEPPING);
            if (step != null && !step.isBlank()) {
                DebuggerSteppingSettings loaded = GSON.fromJson(step, DebuggerSteppingSettings.class);
                if (loaded != null) this.debuggerSteppingSettings = loaded;
            }
        } catch (Exception ignored) {
        }
    }

    public synchronized void resetToDefaults() {
        this.generalSettings = new DebuggerGeneralSettings();
        this.asyncStackTraceSettings = new AsyncStackTraceSettings();
        this.dataViewsSettings = new DataViewsSettings();
        this.javaDataViewsSettings = new JavaDataViewsSettings();
        this.javaScriptDataViewsSettings = new JavaScriptDataViewsSettings();
        this.javaTypeRendererSettings = new JavaTypeRendererSettings();
        this.debuggerHotSwapSettings = new DebuggerHotSwapSettings();
        this.debuggerSteppingSettings = new DebuggerSteppingSettings();
        saveSettings();
        notifyChanged();
    }

    // ============================================================
    // Listener Support
    // ============================================================

    public void addChangeListener(Runnable listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void notifyChanged() {
        for (Runnable l : changeListeners) {
            try {
                l.run();
            } catch (Exception ignored) {
            }
        }
    }

    // ============================================================
    // Dynamic Runtime Queries for DebuggerService
    // ============================================================

    /**
     * Find the first active JavaTypeRenderer matching the given target class name.
     */
    public JavaTypeRenderer findRendererForClass(String fqcn) {
        if (fqcn == null || fqcn.isBlank()) return null;
        for (JavaTypeRenderer renderer : javaTypeRendererSettings.getRenderers()) {
            if (renderer.isEnabled()) {
                String target = renderer.getTargetClassName();
                if (target != null) {
                    if (target.equals(fqcn)) {
                        return renderer;
                    }
                    if (target.endsWith(".*")) {
                        String pkg = target.substring(0, target.length() - 2);
                        if (fqcn.startsWith(pkg + ".")) {
                            return renderer;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Determines whether toString() should be rendered for an object instance.
     */
    public boolean shouldFormatToString(String fqcn, boolean overridesToString) {
        if (!javaDataViewsSettings.isEnableToStringObjectView()) return false;
        if (javaDataViewsSettings.getToStringMode() == JavaDataViewsSettings.ToStringMode.ALL_OVERRIDING) {
            return overridesToString;
        }
        if (fqcn == null) return false;
        for (String pattern : javaDataViewsSettings.getToStringClassPatterns()) {
            if (pattern != null && !pattern.isBlank()) {
                if (pattern.endsWith(".*")) {
                    String prefix = pattern.substring(0, pattern.length() - 2);
                    if (fqcn.startsWith(prefix + ".")) return true;
                } else if (pattern.equals(fqcn)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ============================================================
    // Import / Export JSON helpers
    // ============================================================

    public String exportAsyncRulesToJson() {
        return GSON.toJson(asyncStackTraceSettings.getRules());
    }

    public boolean importAsyncRulesFromJson(String json) {
        try {
            Type listType = new TypeToken<List<AsyncStackTraceRule>>() {}.getType();
            List<AsyncStackTraceRule> imported = GSON.fromJson(json, listType);
            if (imported != null && !imported.isEmpty()) {
                List<AsyncStackTraceRule> current = new ArrayList<>(asyncStackTraceSettings.getRules());
                current.addAll(imported);
                asyncStackTraceSettings.setRules(current);
                saveSettings();
                notifyChanged();
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public String exportTypeRenderersToJson() {
        return GSON.toJson(javaTypeRendererSettings.getRenderers());
    }

    public boolean importTypeRenderersFromJson(String json) {
        try {
            Type listType = new TypeToken<List<JavaTypeRenderer>>() {}.getType();
            List<JavaTypeRenderer> imported = GSON.fromJson(json, listType);
            if (imported != null && !imported.isEmpty()) {
                List<JavaTypeRenderer> current = new ArrayList<>(javaTypeRendererSettings.getRenderers());
                current.addAll(imported);
                javaTypeRendererSettings.setRenderers(current);
                saveSettings();
                notifyChanged();
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
