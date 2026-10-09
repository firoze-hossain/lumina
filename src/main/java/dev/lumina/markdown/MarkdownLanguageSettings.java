package dev.lumina.markdown;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Settings model for Languages & Frameworks > Markdown in Lumina IDE.
 * Dynamically discovers pandoc executable from system PATH.
 */
public class MarkdownLanguageSettings {

    public static final String DEFAULT_PREVIEW_ENGINE = "Chromium browser";
    public static final String DEFAULT_LAYOUT = "Editor and Preview";
    public static final String DEFAULT_PREVIEW_LAYOUT = "Split vertically";
    public static final int DEFAULT_FONT_SIZE = 13;

    public static final List<String> PREVIEW_ENGINES = List.of(
            "Chromium browser",
            "JavaFX WebView"
    );

    public static final List<String> DEFAULT_LAYOUTS = List.of(
            "Editor and Preview",
            "Editor only",
            "Preview only"
    );

    public static final List<String> PREVIEW_LAYOUTS = List.of(
            "Split vertically",
            "Split horizontally"
    );

    public static final List<Integer> FONT_SIZES = List.of(
            8, 9, 10, 11, 12, 13, 14, 15, 16, 18, 20, 22, 24
    );

    private String previewEngine = DEFAULT_PREVIEW_ENGINE;
    private String defaultLayout = DEFAULT_LAYOUT;
    private String previewLayout = DEFAULT_PREVIEW_LAYOUT;
    private int previewFontSize = DEFAULT_FONT_SIZE;

    private boolean syncScroll = true;
    private boolean injectLanguagesInCodeFences = true;
    private boolean showProblemsInCodeFences = true;
    private boolean groupDocumentsWithSameName = false;
    private boolean detectCommands = true;

    private boolean plantUmlEnabled = false;

    // Custom CSS
    private boolean loadCustomCssFrom = false;
    private String customCssPath = "";
    private boolean customCssRulesEnabled = false;
    private String customCssRules = "";

    // Pandoc Settings
    private String pandocExecutablePath = "";
    private String saveImagesFromWordPath = "";

    public MarkdownLanguageSettings() {
        this.pandocExecutablePath = detectPandocExecutable();
    }

    public MarkdownLanguageSettings(MarkdownLanguageSettings other) {
        if (other != null) {
            this.previewEngine = other.previewEngine;
            this.defaultLayout = other.defaultLayout;
            this.previewLayout = other.previewLayout;
            this.previewFontSize = other.previewFontSize;
            this.syncScroll = other.syncScroll;
            this.injectLanguagesInCodeFences = other.injectLanguagesInCodeFences;
            this.showProblemsInCodeFences = other.showProblemsInCodeFences;
            this.groupDocumentsWithSameName = other.groupDocumentsWithSameName;
            this.detectCommands = other.detectCommands;
            this.plantUmlEnabled = other.plantUmlEnabled;
            this.loadCustomCssFrom = other.loadCustomCssFrom;
            this.customCssPath = other.customCssPath;
            this.customCssRulesEnabled = other.customCssRulesEnabled;
            this.customCssRules = other.customCssRules;
            this.pandocExecutablePath = other.pandocExecutablePath;
            this.saveImagesFromWordPath = other.saveImagesFromWordPath;
        }
    }

    public MarkdownLanguageSettings copy() {
        return new MarkdownLanguageSettings(this);
    }

    public static String detectPandocExecutable() {
        String[] candidates = {
                "/usr/bin/pandoc",
                "/usr/local/bin/pandoc",
                System.getProperty("user.home") + "/.local/bin/pandoc"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists() && f.canExecute()) {
                return c;
            }
        }
        return "";
    }

    public static String testPandocVersion(String path) {
        if (path == null || path.isBlank()) return null;
        try {
            Process p = new ProcessBuilder(path, "--version")
                    .redirectErrorStream(true)
                    .start();
            boolean ok = p.waitFor(1, TimeUnit.SECONDS);
            if (ok && p.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null) return line.trim();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public String getPreviewEngine() { return previewEngine; }
    public void setPreviewEngine(String previewEngine) { this.previewEngine = previewEngine != null ? previewEngine : DEFAULT_PREVIEW_ENGINE; }

    public String getDefaultLayout() { return defaultLayout; }
    public void setDefaultLayout(String defaultLayout) { this.defaultLayout = defaultLayout != null ? defaultLayout : DEFAULT_LAYOUT; }

    public String getPreviewLayout() { return previewLayout; }
    public void setPreviewLayout(String previewLayout) { this.previewLayout = previewLayout != null ? previewLayout : DEFAULT_PREVIEW_LAYOUT; }

    public int getPreviewFontSize() { return previewFontSize; }
    public void setPreviewFontSize(int previewFontSize) { this.previewFontSize = previewFontSize > 0 ? previewFontSize : DEFAULT_FONT_SIZE; }

    public boolean isSyncScroll() { return syncScroll; }
    public void setSyncScroll(boolean syncScroll) { this.syncScroll = syncScroll; }

    public boolean isInjectLanguagesInCodeFences() { return injectLanguagesInCodeFences; }
    public void setInjectLanguagesInCodeFences(boolean injectLanguagesInCodeFences) { this.injectLanguagesInCodeFences = injectLanguagesInCodeFences; }

    public boolean isShowProblemsInCodeFences() { return showProblemsInCodeFences; }
    public void setShowProblemsInCodeFences(boolean showProblemsInCodeFences) { this.showProblemsInCodeFences = showProblemsInCodeFences; }

    public boolean isGroupDocumentsWithSameName() { return groupDocumentsWithSameName; }
    public void setGroupDocumentsWithSameName(boolean groupDocumentsWithSameName) { this.groupDocumentsWithSameName = groupDocumentsWithSameName; }

    public boolean isDetectCommands() { return detectCommands; }
    public void setDetectCommands(boolean detectCommands) { this.detectCommands = detectCommands; }

    public boolean isPlantUmlEnabled() { return plantUmlEnabled; }
    public void setPlantUmlEnabled(boolean plantUmlEnabled) { this.plantUmlEnabled = plantUmlEnabled; }

    public boolean isLoadCustomCssFrom() { return loadCustomCssFrom; }
    public void setLoadCustomCssFrom(boolean loadCustomCssFrom) { this.loadCustomCssFrom = loadCustomCssFrom; }

    public String getCustomCssPath() { return customCssPath; }
    public void setCustomCssPath(String customCssPath) { this.customCssPath = customCssPath != null ? customCssPath : ""; }

    public boolean isCustomCssRulesEnabled() { return customCssRulesEnabled; }
    public void setCustomCssRulesEnabled(boolean customCssRulesEnabled) { this.customCssRulesEnabled = customCssRulesEnabled; }

    public String getCustomCssRules() { return customCssRules; }
    public void setCustomCssRules(String customCssRules) { this.customCssRules = customCssRules != null ? customCssRules : ""; }

    public String getPandocExecutablePath() { return pandocExecutablePath; }
    public void setPandocExecutablePath(String pandocExecutablePath) { this.pandocExecutablePath = pandocExecutablePath != null ? pandocExecutablePath : ""; }

    public String getSaveImagesFromWordPath() { return saveImagesFromWordPath; }
    public void setSaveImagesFromWordPath(String saveImagesFromWordPath) { this.saveImagesFromWordPath = saveImagesFromWordPath != null ? saveImagesFromWordPath : ""; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MarkdownLanguageSettings that = (MarkdownLanguageSettings) o;
        return previewFontSize == that.previewFontSize &&
                syncScroll == that.syncScroll &&
                injectLanguagesInCodeFences == that.injectLanguagesInCodeFences &&
                showProblemsInCodeFences == that.showProblemsInCodeFences &&
                groupDocumentsWithSameName == that.groupDocumentsWithSameName &&
                detectCommands == that.detectCommands &&
                plantUmlEnabled == that.plantUmlEnabled &&
                loadCustomCssFrom == that.loadCustomCssFrom &&
                customCssRulesEnabled == that.customCssRulesEnabled &&
                Objects.equals(previewEngine, that.previewEngine) &&
                Objects.equals(defaultLayout, that.defaultLayout) &&
                Objects.equals(previewLayout, that.previewLayout) &&
                Objects.equals(customCssPath, that.customCssPath) &&
                Objects.equals(customCssRules, that.customCssRules) &&
                Objects.equals(pandocExecutablePath, that.pandocExecutablePath) &&
                Objects.equals(saveImagesFromWordPath, that.saveImagesFromWordPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(previewEngine, defaultLayout, previewLayout, previewFontSize,
                syncScroll, injectLanguagesInCodeFences, showProblemsInCodeFences, groupDocumentsWithSameName,
                detectCommands, plantUmlEnabled, loadCustomCssFrom, customCssPath, customCssRulesEnabled,
                customCssRules, pandocExecutablePath, saveImagesFromWordPath);
    }
}
