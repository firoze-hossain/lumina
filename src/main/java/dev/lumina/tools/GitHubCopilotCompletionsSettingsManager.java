package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manager handling persistence and change notifications for Tools > GitHub Copilot > Completions settings.
 */
public class GitHubCopilotCompletionsSettingsManager {

    private static final GitHubCopilotCompletionsSettingsManager INSTANCE = new GitHubCopilotCompletionsSettingsManager();

    private static final String KEY_ENABLED = "tools.copilot.completions.enabled";
    private static final String KEY_NES_ENABLED = "tools.copilot.completions.nes_enabled";
    private static final String KEY_SIDE_BY_SIDE = "tools.copilot.completions.side_by_side";
    private static final String KEY_MULTIPLE_SUGGESTIONS = "tools.copilot.completions.multiple_suggestions";
    private static final String KEY_COLOR_ENABLED = "tools.copilot.completions.color_enabled";
    private static final String KEY_COLOR_RGB = "tools.copilot.completions.color_rgb";
    private static final String KEY_MODEL = "tools.copilot.completions.model";

    private static final String KEY_LANGS_COUNT = "tools.copilot.completions.languages.count";
    private static final String KEY_LANG_NAME_PREFIX = "tools.copilot.completions.lang.name.";
    private static final String KEY_LANG_ENABLED_PREFIX = "tools.copilot.completions.lang.enabled.";

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private GitHubCopilotCompletionsSettingsManager() {
    }

    public static GitHubCopilotCompletionsSettingsManager getInstance() {
        return INSTANCE;
    }

    public GitHubCopilotCompletionsSettings getSettings() {
        GitHubCopilotCompletionsSettings s = new GitHubCopilotCompletionsSettings();

        String en = Settings.get(KEY_ENABLED);
        if (en != null) s.setEnableCopilotCompletions(Boolean.parseBoolean(en));

        String nes = Settings.get(KEY_NES_ENABLED);
        if (nes != null) s.setEnableNextEditSuggestions(Boolean.parseBoolean(nes));

        String sbs = Settings.get(KEY_SIDE_BY_SIDE);
        if (sbs != null) s.setShowIdeCompletionsSideBySide(Boolean.parseBoolean(sbs));

        String ms = Settings.get(KEY_MULTIPLE_SUGGESTIONS);
        if (ms != null) s.setShowMultipleSuggestionsInToolWindow(Boolean.parseBoolean(ms));

        String ce = Settings.get(KEY_COLOR_ENABLED);
        if (ce != null) s.setColorForCompletions(Boolean.parseBoolean(ce));

        String crgb = Settings.get(KEY_COLOR_RGB);
        if (crgb != null && !crgb.isBlank()) s.setCompletionColorRgb(crgb);

        String model = Settings.get(KEY_MODEL);
        if (model != null && !model.isBlank()) s.setModelForCompletions(model);

        String lcntStr = Settings.get(KEY_LANGS_COUNT);
        if (lcntStr != null) {
            try {
                int count = Integer.parseInt(lcntStr);
                Map<String, Boolean> map = new LinkedHashMap<>();
                for (int i = 0; i < count; i++) {
                    String name = Settings.get(KEY_LANG_NAME_PREFIX + i);
                    String val = Settings.get(KEY_LANG_ENABLED_PREFIX + i);
                    if (name != null) {
                        map.put(name, val != null ? Boolean.parseBoolean(val) : true);
                    }
                }
                if (!map.isEmpty()) {
                    s.setEnabledLanguages(map);
                }
            } catch (NumberFormatException ignored) {}
        }

        return s;
    }

    public void setSettings(GitHubCopilotCompletionsSettings settings) {
        save(settings);
    }

    public void save(GitHubCopilotCompletionsSettings settings) {
        if (settings == null) return;

        Settings.put(KEY_ENABLED, String.valueOf(settings.isEnableCopilotCompletions()));
        Settings.put(KEY_NES_ENABLED, String.valueOf(settings.isEnableNextEditSuggestions()));
        Settings.put(KEY_SIDE_BY_SIDE, String.valueOf(settings.isShowIdeCompletionsSideBySide()));
        Settings.put(KEY_MULTIPLE_SUGGESTIONS, String.valueOf(settings.isShowMultipleSuggestionsInToolWindow()));
        Settings.put(KEY_COLOR_ENABLED, String.valueOf(settings.isColorForCompletions()));
        Settings.put(KEY_COLOR_RGB, settings.getCompletionColorRgb());
        Settings.put(KEY_MODEL, settings.getModelForCompletions());

        // Save languages
        String prevCnt = Settings.get(KEY_LANGS_COUNT);
        if (prevCnt != null) {
            try {
                int oldCnt = Integer.parseInt(prevCnt);
                for (int i = 0; i < oldCnt; i++) {
                    Settings.put(KEY_LANG_NAME_PREFIX + i, null);
                    Settings.put(KEY_LANG_ENABLED_PREFIX + i, null);
                }
            } catch (NumberFormatException ignored) {}
        }

        Map<String, Boolean> langs = settings.getEnabledLanguages();
        Settings.put(KEY_LANGS_COUNT, String.valueOf(langs.size()));
        int idx = 0;
        for (Map.Entry<String, Boolean> e : langs.entrySet()) {
            Settings.put(KEY_LANG_NAME_PREFIX + idx, e.getKey());
            Settings.put(KEY_LANG_ENABLED_PREFIX + idx, String.valueOf(e.getValue()));
            idx++;
        }

        notifyListeners();
    }

    public void clear() {
        Settings.put(KEY_ENABLED, null);
        Settings.put(KEY_NES_ENABLED, null);
        Settings.put(KEY_SIDE_BY_SIDE, null);
        Settings.put(KEY_MULTIPLE_SUGGESTIONS, null);
        Settings.put(KEY_COLOR_ENABLED, null);
        Settings.put(KEY_COLOR_RGB, null);
        Settings.put(KEY_MODEL, null);

        String prevCnt = Settings.get(KEY_LANGS_COUNT);
        if (prevCnt != null) {
            try {
                int oldCnt = Integer.parseInt(prevCnt);
                for (int i = 0; i < oldCnt; i++) {
                    Settings.put(KEY_LANG_NAME_PREFIX + i, null);
                    Settings.put(KEY_LANG_ENABLED_PREFIX + i, null);
                }
            } catch (NumberFormatException ignored) {}
        }
        Settings.put(KEY_LANGS_COUNT, null);

        notifyListeners();
    }

    public void addListener(Runnable listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        if (listener != null) listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable r : listeners) {
            try {
                r.run();
            } catch (Throwable ignored) {}
        }
    }
}
