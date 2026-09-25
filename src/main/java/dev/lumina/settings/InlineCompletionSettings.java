package dev.lumina.settings;

import dev.lumina.util.Settings;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic persistent configuration model for Editor > General > Inline Completion settings.
 * Backed by ~/.lumina/lumina.properties, supporting listeners, dirty tracking, and live editor updates.
 */
public final class InlineCompletionSettings {

    public enum DownloadModelsMode {
        ASK_BEFORE_DOWNLOADING("Ask before downloading"),
        AUTOMATICALLY("Automatically"),
        MANUALLY("Manually");

        private final String label;

        DownloadModelsMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static DownloadModelsMode fromLabel(String label) {
            for (DownloadModelsMode m : values()) {
                if (m.label.equalsIgnoreCase(label) || m.name().equalsIgnoreCase(label)) {
                    return m;
                }
            }
            return ASK_BEFORE_DOWNLOADING;
        }
    }

    public static final List<String> ALL_LANGUAGES = List.of(
            "Kotlin",
            "Java",
            "CSS-like",
            "HTML",
            "JavaScript / TypeScript",
            "Python",
            "PHP",
            "Rust",
            "Scala",
            "Go",
            "Ruby"
    );

    public static final Set<String> BUNDLED_LANGUAGES = Set.of("Kotlin", "Java");

    private static final InlineCompletionSettings INSTANCE = new InlineCompletionSettings();

    public static InlineCompletionSettings getInstance() {
        return INSTANCE;
    }

    @FunctionalInterface
    public interface Listener {
        void onSettingsChanged(InlineCompletionSettings settings);
    }

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    // 1. Local Full Line
    private boolean enableLocalFullLine = true;
    private final Set<String> enabledLanguages = new LinkedHashSet<>();
    private final Set<String> downloadedLanguages = new LinkedHashSet<>();
    private DownloadModelsMode downloadModelsMode = DownloadModelsMode.ASK_BEFORE_DOWNLOADING;

    // 2. Cloud Completion
    private boolean enableCloudCompletion = false;
    private boolean cloudActivated = false;

    // 3. Typing & Behavior
    private boolean enableAutomaticCompletionOnTyping = true;
    private boolean enableMultilineSuggestions = true;
    private boolean synchronizeInlineAndPopup = false;

    public InlineCompletionSettings() {
        initDefaults();
        load();
    }

    public void initDefaults() {
        enableLocalFullLine = true;
        enabledLanguages.clear();
        enabledLanguages.add("Kotlin");
        enabledLanguages.add("Java");

        downloadedLanguages.clear();
        downloadedLanguages.addAll(BUNDLED_LANGUAGES);

        downloadModelsMode = DownloadModelsMode.ASK_BEFORE_DOWNLOADING;

        enableCloudCompletion = false;
        cloudActivated = false;

        enableAutomaticCompletionOnTyping = true;
        enableMultilineSuggestions = true;
        synchronizeInlineAndPopup = false;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Listener l : listeners) {
            try {
                l.onSettingsChanged(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void load() {
        enableLocalFullLine = getBool("enable_local_full_line", enableLocalFullLine);
        downloadModelsMode = DownloadModelsMode.fromLabel(getStr("download_models_mode", downloadModelsMode.getLabel()));

        enableCloudCompletion = getBool("enable_cloud_completion", enableCloudCompletion);
        cloudActivated = getBool("cloud_activated", cloudActivated);

        enableAutomaticCompletionOnTyping = getBool("auto_on_typing", enableAutomaticCompletionOnTyping);
        enableMultilineSuggestions = getBool("multiline_suggestions", enableMultilineSuggestions);
        synchronizeInlineAndPopup = getBool("sync_inline_and_popup", synchronizeInlineAndPopup);

        String langs = Settings.get("editor.inline.completion.enabled_languages");
        if (langs != null) {
            enabledLanguages.clear();
            for (String part : langs.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    enabledLanguages.add(trimmed);
                }
            }
        }

        String downloaded = Settings.get("editor.inline.completion.downloaded_languages");
        if (downloaded != null) {
            downloadedLanguages.clear();
            downloadedLanguages.addAll(BUNDLED_LANGUAGES);
            for (String part : downloaded.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    downloadedLanguages.add(trimmed);
                }
            }
        }
    }

    public void save() {
        putBool("enable_local_full_line", enableLocalFullLine);
        putStr("download_models_mode", downloadModelsMode.getLabel());

        putBool("enable_cloud_completion", enableCloudCompletion);
        putBool("cloud_activated", cloudActivated);

        putBool("auto_on_typing", enableAutomaticCompletionOnTyping);
        putBool("multiline_suggestions", enableMultilineSuggestions);
        putBool("sync_inline_and_popup", synchronizeInlineAndPopup);

        Settings.put("editor.inline.completion.enabled_languages", String.join(",", enabledLanguages));
        Settings.put("editor.inline.completion.downloaded_languages", String.join(",", downloadedLanguages));

        notifyListeners();
    }

    public void copyFrom(InlineCompletionSettings o) {
        this.enableLocalFullLine = o.enableLocalFullLine;
        this.downloadModelsMode = o.downloadModelsMode;
        this.enableCloudCompletion = o.enableCloudCompletion;
        this.cloudActivated = o.cloudActivated;
        this.enableAutomaticCompletionOnTyping = o.enableAutomaticCompletionOnTyping;
        this.enableMultilineSuggestions = o.enableMultilineSuggestions;
        this.synchronizeInlineAndPopup = o.synchronizeInlineAndPopup;

        this.enabledLanguages.clear();
        this.enabledLanguages.addAll(o.enabledLanguages);

        this.downloadedLanguages.clear();
        this.downloadedLanguages.addAll(o.downloadedLanguages);
    }

    public InlineCompletionSettings copy() {
        InlineCompletionSettings c = new InlineCompletionSettings();
        c.copyFrom(this);
        return c;
    }

    public boolean isModified(InlineCompletionSettings o) {
        if (o == null) return true;
        return this.enableLocalFullLine != o.enableLocalFullLine
                || this.downloadModelsMode != o.downloadModelsMode
                || this.enableCloudCompletion != o.enableCloudCompletion
                || this.cloudActivated != o.cloudActivated
                || this.enableAutomaticCompletionOnTyping != o.enableAutomaticCompletionOnTyping
                || this.enableMultilineSuggestions != o.enableMultilineSuggestions
                || this.synchronizeInlineAndPopup != o.synchronizeInlineAndPopup
                || !this.enabledLanguages.equals(o.enabledLanguages)
                || !this.downloadedLanguages.equals(o.downloadedLanguages);
    }

    private boolean getBool(String key, boolean def) {
        String val = Settings.get("editor.inline.completion." + key);
        return val != null ? Boolean.parseBoolean(val) : def;
    }

    private void putBool(String key, boolean val) {
        Settings.put("editor.inline.completion." + key, String.valueOf(val));
    }

    private String getStr(String key, String def) {
        String val = Settings.get("editor.inline.completion." + key);
        return val != null ? val : def;
    }

    private void putStr(String key, String val) {
        Settings.put("editor.inline.completion." + key, val != null ? val : "");
    }

    // Getters and Setters
    public boolean isEnableLocalFullLine() {
        return enableLocalFullLine;
    }

    public void setEnableLocalFullLine(boolean enableLocalFullLine) {
        this.enableLocalFullLine = enableLocalFullLine;
    }

    public DownloadModelsMode getDownloadModelsMode() {
        return downloadModelsMode;
    }

    public void setDownloadModelsMode(DownloadModelsMode downloadModelsMode) {
        this.downloadModelsMode = downloadModelsMode != null ? downloadModelsMode : DownloadModelsMode.ASK_BEFORE_DOWNLOADING;
    }

    public boolean isEnableCloudCompletion() {
        return enableCloudCompletion;
    }

    public void setEnableCloudCompletion(boolean enableCloudCompletion) {
        this.enableCloudCompletion = enableCloudCompletion;
    }

    public boolean isCloudActivated() {
        return cloudActivated;
    }

    public void setCloudActivated(boolean cloudActivated) {
        this.cloudActivated = cloudActivated;
    }

    public boolean isEnableAutomaticCompletionOnTyping() {
        return enableAutomaticCompletionOnTyping;
    }

    public void setEnableAutomaticCompletionOnTyping(boolean enableAutomaticCompletionOnTyping) {
        this.enableAutomaticCompletionOnTyping = enableAutomaticCompletionOnTyping;
    }

    public boolean isEnableMultilineSuggestions() {
        return enableMultilineSuggestions;
    }

    public void setEnableMultilineSuggestions(boolean enableMultilineSuggestions) {
        this.enableMultilineSuggestions = enableMultilineSuggestions;
    }

    public boolean isSynchronizeInlineAndPopup() {
        return synchronizeInlineAndPopup;
    }

    public void setSynchronizeInlineAndPopup(boolean synchronizeInlineAndPopup) {
        this.synchronizeInlineAndPopup = synchronizeInlineAndPopup;
    }

    public Set<String> getEnabledLanguages() {
        return Collections.unmodifiableSet(enabledLanguages);
    }

    public boolean isLanguageEnabled(String lang) {
        return enabledLanguages.contains(lang);
    }

    public void setLanguageEnabled(String lang, boolean enabled) {
        if (enabled) {
            enabledLanguages.add(lang);
        } else {
            enabledLanguages.remove(lang);
        }
    }

    public Set<String> getDownloadedLanguages() {
        return Collections.unmodifiableSet(downloadedLanguages);
    }

    public boolean isLanguageDownloaded(String lang) {
        return BUNDLED_LANGUAGES.contains(lang) || downloadedLanguages.contains(lang);
    }

    public void setLanguageDownloaded(String lang, boolean downloaded) {
        if (downloaded) {
            downloadedLanguages.add(lang);
        } else if (!BUNDLED_LANGUAGES.contains(lang)) {
            downloadedLanguages.remove(lang);
        }
    }
}
