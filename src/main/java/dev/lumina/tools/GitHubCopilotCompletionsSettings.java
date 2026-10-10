package dev.lumina.tools;

import java.util.*;

/**
 * Model holding settings for Tools > GitHub Copilot > Completions.
 */
public class GitHubCopilotCompletionsSettings {

    public static final List<String> DEFAULT_LANGUAGES = List.of(
            "$XSLT",
            ".dockerignore (DockerIgnore)",
            ".gitignore (GitIgnore)",
            ".hgignore (HgIgnore)",
            ".ignore (IgnoreLang)",
            "<FreeMarker>",
            "[FreeMarker]",
            "ActionScript",
            "Amazon Redshift",
            "Apache Config (.htaccess)",
            "Apex",
            "AppleScript",
            "C",
            "C#",
            "C++",
            "Clojure",
            "CMake",
            "CSS",
            "Dart",
            "Dockerfile",
            "Go",
            "Groovy",
            "HTML",
            "Java",
            "JavaScript",
            "JSON",
            "Kotlin",
            "Lua",
            "Markdown",
            "PHP",
            "Python",
            "R",
            "Ruby",
            "Rust",
            "SCSS",
            "SQL",
            "Swift",
            "TypeScript",
            "XML",
            "YAML"
    );

    public static final List<String> AVAILABLE_MODELS = List.of(
            "GPT-4.1 Copilot",
            "Claude 3.5 Sonnet",
            "o1-mini",
            "o3-mini",
            "GPT-4o"
    );

    private boolean enableCopilotCompletions = true;
    private boolean enableNextEditSuggestions = true;
    private boolean showIdeCompletionsSideBySide = false;
    private boolean showMultipleSuggestionsInToolWindow = true;

    private boolean colorForCompletions = false;
    private String completionColorRgb = "#6C707E";

    private String modelForCompletions = "GPT-4.1 Copilot";

    private Map<String, Boolean> enabledLanguages = new LinkedHashMap<>();

    public GitHubCopilotCompletionsSettings() {
        for (String lang : DEFAULT_LANGUAGES) {
            enabledLanguages.put(lang, true);
        }
    }

    public GitHubCopilotCompletionsSettings(GitHubCopilotCompletionsSettings other) {
        if (other != null) {
            this.enableCopilotCompletions = other.enableCopilotCompletions;
            this.enableNextEditSuggestions = other.enableNextEditSuggestions;
            this.showIdeCompletionsSideBySide = other.showIdeCompletionsSideBySide;
            this.showMultipleSuggestionsInToolWindow = other.showMultipleSuggestionsInToolWindow;
            this.colorForCompletions = other.colorForCompletions;
            this.completionColorRgb = other.completionColorRgb;
            this.modelForCompletions = other.modelForCompletions;
            this.enabledLanguages = new LinkedHashMap<>(other.enabledLanguages);
        }
    }

    public boolean isEnableCopilotCompletions() {
        return enableCopilotCompletions;
    }

    public void setEnableCopilotCompletions(boolean enableCopilotCompletions) {
        this.enableCopilotCompletions = enableCopilotCompletions;
    }

    public boolean isEnableNextEditSuggestions() {
        return enableNextEditSuggestions;
    }

    public void setEnableNextEditSuggestions(boolean enableNextEditSuggestions) {
        this.enableNextEditSuggestions = enableNextEditSuggestions;
    }

    public boolean isShowIdeCompletionsSideBySide() {
        return showIdeCompletionsSideBySide;
    }

    public void setShowIdeCompletionsSideBySide(boolean showIdeCompletionsSideBySide) {
        this.showIdeCompletionsSideBySide = showIdeCompletionsSideBySide;
    }

    public boolean isShowMultipleSuggestionsInToolWindow() {
        return showMultipleSuggestionsInToolWindow;
    }

    public void setShowMultipleSuggestionsInToolWindow(boolean showMultipleSuggestionsInToolWindow) {
        this.showMultipleSuggestionsInToolWindow = showMultipleSuggestionsInToolWindow;
    }

    public boolean isColorForCompletions() {
        return colorForCompletions;
    }

    public void setColorForCompletions(boolean colorForCompletions) {
        this.colorForCompletions = colorForCompletions;
    }

    public String getCompletionColorRgb() {
        return completionColorRgb;
    }

    public void setCompletionColorRgb(String completionColorRgb) {
        this.completionColorRgb = completionColorRgb != null ? completionColorRgb : "#6C707E";
    }

    public String getModelForCompletions() {
        return modelForCompletions;
    }

    public void setModelForCompletions(String modelForCompletions) {
        this.modelForCompletions = modelForCompletions != null ? modelForCompletions : "GPT-4.1 Copilot";
    }

    public Map<String, Boolean> getEnabledLanguages() {
        return enabledLanguages;
    }

    public void setEnabledLanguages(Map<String, Boolean> enabledLanguages) {
        this.enabledLanguages = enabledLanguages != null ? new LinkedHashMap<>(enabledLanguages) : new LinkedHashMap<>();
    }

    public boolean isLanguageEnabled(String language) {
        return enabledLanguages.getOrDefault(language, true);
    }

    public void setLanguageEnabled(String language, boolean enabled) {
        enabledLanguages.put(language, enabled);
    }

    public GitHubCopilotCompletionsSettings clone() {
        return new GitHubCopilotCompletionsSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GitHubCopilotCompletionsSettings that = (GitHubCopilotCompletionsSettings) o;
        return enableCopilotCompletions == that.enableCopilotCompletions &&
                enableNextEditSuggestions == that.enableNextEditSuggestions &&
                showIdeCompletionsSideBySide == that.showIdeCompletionsSideBySide &&
                showMultipleSuggestionsInToolWindow == that.showMultipleSuggestionsInToolWindow &&
                colorForCompletions == that.colorForCompletions &&
                Objects.equals(completionColorRgb, that.completionColorRgb) &&
                Objects.equals(modelForCompletions, that.modelForCompletions) &&
                Objects.equals(enabledLanguages, that.enabledLanguages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enableCopilotCompletions, enableNextEditSuggestions, showIdeCompletionsSideBySide,
                showMultipleSuggestionsInToolWindow, colorForCompletions, completionColorRgb,
                modelForCompletions, enabledLanguages);
    }

    @Override
    public String toString() {
        return "GitHubCopilotCompletionsSettings{" +
                "enableCopilotCompletions=" + enableCopilotCompletions +
                ", enableNextEditSuggestions=" + enableNextEditSuggestions +
                ", showIdeCompletionsSideBySide=" + showIdeCompletionsSideBySide +
                ", showMultipleSuggestionsInToolWindow=" + showMultipleSuggestionsInToolWindow +
                ", colorForCompletions=" + colorForCompletions +
                ", completionColorRgb='" + completionColorRgb + '\'' +
                ", modelForCompletions='" + modelForCompletions + '\'' +
                ", enabledLanguagesCount=" + enabledLanguages.size() +
                '}';
    }
}
