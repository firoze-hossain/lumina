package dev.lumina.play;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Languages & Frameworks > Play settings in Lumina IDE.
 * Handles persistence, default restoration, and state synchronization.
 */
public class PlaySettingsManager {

    public static final String KEY_PLAY_USE_COMPILER = "play.use.compiler";
    public static final String KEY_PLAY_DONT_COMPILE_BEFORE_RUN = "play.dont.compile.before.run";
    public static final String KEY_PLAY_MODULE = "play.module";
    public static final String KEY_PLAY_PROJECT_URI = "play.project.uri";
    public static final String KEY_PLAY_ADDITIONAL_SBT_OPTIONS = "play.additional.sbt.options";

    public static final String KEY_PLAY_MIN_SPACES_ROUTES = "play.min.spaces.routes";
    public static final String KEY_PLAY_IGNORE_URL_DEPTH = "play.ignore.url.depth";
    public static final String KEY_PLAY_REFORMAT_ROUTES_ON_ENTER = "play.reformat.routes.on.enter";

    public static final String KEY_PLAY_EXCLUDE_TARGET_DIR = "play.exclude.target.dir";
    public static final String KEY_PLAY_COLORED_OUTPUT = "play.colored.output";
    public static final String KEY_PLAY_SHOW_CODE_REFS = "play.show.code.refs";
    public static final String KEY_PLAY_SET_TEMPLATE_IMPORTS_MANUALLY = "play.set.template.imports.manually";
    public static final String KEY_PLAY_TEMPLATE_IMPORTS = "play.template.imports";

    public static final String KEY_PLAY_GUTTER_ACTION_METHODS = "play.gutter.action.methods";
    public static final String KEY_PLAY_GUTTER_VIEW_CALLS = "play.gutter.view.calls";
    public static final String KEY_PLAY_GUTTER_RESULT_CALLS = "play.gutter.result.calls";

    private static PlaySettingsManager instance;

    private PlaySettings currentSettings = new PlaySettings();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private PlaySettingsManager() {
        loadSettings();
    }

    public static synchronized PlaySettingsManager getInstance() {
        if (instance == null) {
            instance = new PlaySettingsManager();
        }
        return instance;
    }

    public synchronized PlaySettings getSettings() {
        return currentSettings.copy();
    }

    public synchronized void setSettings(PlaySettings settings) {
        this.currentSettings = settings != null ? settings.copy() : new PlaySettings();
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
        Settings.put(KEY_PLAY_USE_COMPILER, String.valueOf(currentSettings.isUsePlayCompiler()));
        Settings.put(KEY_PLAY_DONT_COMPILE_BEFORE_RUN, String.valueOf(currentSettings.isDontCompileWithinIdeBeforeRun()));
        Settings.put(KEY_PLAY_MODULE, currentSettings.getPlayModule());
        Settings.put(KEY_PLAY_PROJECT_URI, currentSettings.getProjectUri());
        Settings.put(KEY_PLAY_ADDITIONAL_SBT_OPTIONS, currentSettings.getAdditionalSbtOptions());

        Settings.put(KEY_PLAY_MIN_SPACES_ROUTES, String.valueOf(currentSettings.getMinSpacesForRoutesFormatting()));
        Settings.put(KEY_PLAY_IGNORE_URL_DEPTH, String.valueOf(currentSettings.isIgnoreUrlDepthInRouteFiles()));
        Settings.put(KEY_PLAY_REFORMAT_ROUTES_ON_ENTER, String.valueOf(currentSettings.isReformatRoutesFileOnEnter()));

        Settings.put(KEY_PLAY_EXCLUDE_TARGET_DIR, String.valueOf(currentSettings.isExcludeTargetDirOnRefresh()));
        Settings.put(KEY_PLAY_COLORED_OUTPUT, String.valueOf(currentSettings.isColoredOutputConsole()));
        Settings.put(KEY_PLAY_SHOW_CODE_REFS, String.valueOf(currentSettings.isShowCodeRefsInOutputConsole()));
        Settings.put(KEY_PLAY_SET_TEMPLATE_IMPORTS_MANUALLY, String.valueOf(currentSettings.isSetTemplateImportsManually()));
        Settings.put(KEY_PLAY_TEMPLATE_IMPORTS, String.join("||", currentSettings.getTemplateImports()));

        Settings.put(KEY_PLAY_GUTTER_ACTION_METHODS, String.valueOf(currentSettings.isGutterActionMethods()));
        Settings.put(KEY_PLAY_GUTTER_VIEW_CALLS, String.valueOf(currentSettings.isGutterViewCalls()));
        Settings.put(KEY_PLAY_GUTTER_RESULT_CALLS, String.valueOf(currentSettings.isGutterResultCalls()));
    }

    public synchronized void loadSettings() {
        PlaySettings s = new PlaySettings();

        String useComp = Settings.get(KEY_PLAY_USE_COMPILER);
        if (useComp != null) s.setUsePlayCompiler(Boolean.parseBoolean(useComp));

        String dontComp = Settings.get(KEY_PLAY_DONT_COMPILE_BEFORE_RUN);
        if (dontComp != null) s.setDontCompileWithinIdeBeforeRun(Boolean.parseBoolean(dontComp));

        String mod = Settings.get(KEY_PLAY_MODULE);
        if (mod != null && !mod.isBlank()) s.setPlayModule(mod);

        String uri = Settings.get(KEY_PLAY_PROJECT_URI);
        if (uri != null) s.setProjectUri(uri);

        String sbt = Settings.get(KEY_PLAY_ADDITIONAL_SBT_OPTIONS);
        if (sbt != null) s.setAdditionalSbtOptions(sbt);

        String minSp = Settings.get(KEY_PLAY_MIN_SPACES_ROUTES);
        if (minSp != null) {
            try {
                s.setMinSpacesForRoutesFormatting(Integer.parseInt(minSp));
            } catch (NumberFormatException ignored) {}
        }

        String ignoreDepth = Settings.get(KEY_PLAY_IGNORE_URL_DEPTH);
        if (ignoreDepth != null) s.setIgnoreUrlDepthInRouteFiles(Boolean.parseBoolean(ignoreDepth));

        String reformatEnter = Settings.get(KEY_PLAY_REFORMAT_ROUTES_ON_ENTER);
        if (reformatEnter != null) s.setReformatRoutesFileOnEnter(Boolean.parseBoolean(reformatEnter));

        String exclTarget = Settings.get(KEY_PLAY_EXCLUDE_TARGET_DIR);
        if (exclTarget != null) s.setExcludeTargetDirOnRefresh(Boolean.parseBoolean(exclTarget));

        String colOut = Settings.get(KEY_PLAY_COLORED_OUTPUT);
        if (colOut != null) s.setColoredOutputConsole(Boolean.parseBoolean(colOut));

        String codeRefs = Settings.get(KEY_PLAY_SHOW_CODE_REFS);
        if (codeRefs != null) s.setShowCodeRefsInOutputConsole(Boolean.parseBoolean(codeRefs));

        String setImportsMan = Settings.get(KEY_PLAY_SET_TEMPLATE_IMPORTS_MANUALLY);
        if (setImportsMan != null) s.setSetTemplateImportsManually(Boolean.parseBoolean(setImportsMan));

        String importsStr = Settings.get(KEY_PLAY_TEMPLATE_IMPORTS);
        if (importsStr != null && !importsStr.isBlank()) {
            s.setTemplateImports(Arrays.asList(importsStr.split("\\|\\|")));
        }

        String gAct = Settings.get(KEY_PLAY_GUTTER_ACTION_METHODS);
        if (gAct != null) s.setGutterActionMethods(Boolean.parseBoolean(gAct));

        String gView = Settings.get(KEY_PLAY_GUTTER_VIEW_CALLS);
        if (gView != null) s.setGutterViewCalls(Boolean.parseBoolean(gView));

        String gRes = Settings.get(KEY_PLAY_GUTTER_RESULT_CALLS);
        if (gRes != null) s.setGutterResultCalls(Boolean.parseBoolean(gRes));

        this.currentSettings = s;
    }

    public synchronized void resetDefaults() {
        this.currentSettings = new PlaySettings();
        saveSettings();
        notifyListeners();
    }
}
