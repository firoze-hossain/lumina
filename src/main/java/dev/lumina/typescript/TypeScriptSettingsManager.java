package dev.lumina.typescript;

import dev.lumina.util.Settings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Languages & Frameworks > TypeScript in Lumina IDE.
 * Dynamically discovers Node interpreters and manages TypeScript language service settings.
 */
public class TypeScriptSettingsManager {

    public static final String KEY_NODE_INTERPRETER = "typescript.node_interpreter";
    public static final String KEY_PACKAGE = "typescript.package";
    public static final String KEY_USE_LANGUAGE_SERVICE = "typescript.use_language_service";
    public static final String KEY_SHOW_PROJECT_ERRORS = "typescript.show_project_errors";
    public static final String KEY_SHOW_SUGGESTIONS = "typescript.show_suggestions";
    public static final String KEY_ENABLE_TYPE_ENGINE = "typescript.enable_service_powered_type_engine";
    public static final String KEY_RECOMPILE_ON_CHANGES = "typescript.recompile_on_changes";
    public static final String KEY_OPTIONS = "typescript.options";

    private static volatile TypeScriptSettingsManager instance;
    private TypeScriptSettings currentSettings;
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private TypeScriptSettingsManager() {
        loadSettings();
    }

    public static TypeScriptSettingsManager getInstance() {
        if (instance == null) {
            synchronized (TypeScriptSettingsManager.class) {
                if (instance == null) {
                    instance = new TypeScriptSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized TypeScriptSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(TypeScriptSettings settings) {
        if (settings == null) return;
        this.currentSettings = settings.clone();
        saveSettings();
        notifyListeners();
    }

    public synchronized void loadSettings() {
        TypeScriptSettings s = new TypeScriptSettings();

        String node = Settings.get(KEY_NODE_INTERPRETER);
        if (node != null && !node.isBlank()) {
            s.setNodeInterpreter(node);
        }

        String pkg = Settings.get(KEY_PACKAGE);
        if (pkg != null && !pkg.isBlank()) {
            s.setTypeScriptPackage(pkg);
        }

        String service = Settings.get(KEY_USE_LANGUAGE_SERVICE);
        if (service != null) {
            s.setUseLanguageService(Boolean.parseBoolean(service));
        }

        String errors = Settings.get(KEY_SHOW_PROJECT_ERRORS);
        if (errors != null) {
            s.setShowProjectErrors(Boolean.parseBoolean(errors));
        }

        String sugg = Settings.get(KEY_SHOW_SUGGESTIONS);
        if (sugg != null) {
            s.setShowSuggestions(Boolean.parseBoolean(sugg));
        }

        String engine = Settings.get(KEY_ENABLE_TYPE_ENGINE);
        if (engine != null) {
            s.setEnableServicePoweredTypeEngine(Boolean.parseBoolean(engine));
        }

        String recompile = Settings.get(KEY_RECOMPILE_ON_CHANGES);
        if (recompile != null) {
            s.setRecompileOnChanges(Boolean.parseBoolean(recompile));
        }

        String options = Settings.get(KEY_OPTIONS);
        if (options != null) {
            s.setOptions(options);
        }

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.put(KEY_NODE_INTERPRETER, currentSettings.getNodeInterpreter());
        Settings.put(KEY_PACKAGE, currentSettings.getTypeScriptPackage());
        Settings.put(KEY_USE_LANGUAGE_SERVICE, String.valueOf(currentSettings.isUseLanguageService()));
        Settings.put(KEY_SHOW_PROJECT_ERRORS, String.valueOf(currentSettings.isShowProjectErrors()));
        Settings.put(KEY_SHOW_SUGGESTIONS, String.valueOf(currentSettings.isShowSuggestions()));
        Settings.put(KEY_ENABLE_TYPE_ENGINE, String.valueOf(currentSettings.isEnableServicePoweredTypeEngine()));
        Settings.put(KEY_RECOMPILE_ON_CHANGES, String.valueOf(currentSettings.isRecompileOnChanges()));
        Settings.put(KEY_OPTIONS, currentSettings.getOptions());
    }

    /**
     * Dynamically discovers Node.js interpreters available on the system.
     */
    public List<String> discoverNodeInterpreters() {
        List<String> list = new ArrayList<>();
        list.add(TypeScriptSettings.DEFAULT_NODE);

        String[] standardPaths = {
                "/usr/local/bin/node",
                "/opt/homebrew/bin/node",
                "/usr/bin/node",
                System.getProperty("user.home") + "/.nvm/versions/node",
                System.getProperty("user.home") + "/.asdf/shims/node"
        };

        for (String p : standardPaths) {
            File f = new File(p);
            if (f.exists()) {
                String entry = f.getAbsolutePath();
                if (!list.contains(entry) && !entry.equals("/usr/local/bin/node")) {
                    list.add(entry);
                }
            }
        }
        return list;
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) {
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
            } catch (Throwable ignored) {}
        }
    }
}
