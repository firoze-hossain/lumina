package dev.lumina.settings;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic persistent configuration and model for IntelliJ IDEA-style Editor > General > Appearance settings.
 * Backed by ~/.lumina/lumina.properties, supporting dynamic listeners,
 * real-time UI synchronization, and live editor updates.
 */
public final class EditorAppearanceSettings {

    private static final EditorAppearanceSettings INSTANCE = new EditorAppearanceSettings();

    public static EditorAppearanceSettings getInstance() {
        return INSTANCE;
    }

    public enum LineNumbersMode {
        ABSOLUTE("Absolute"),
        RELATIVE("Relative"),
        HYBRID("Hybrid");

        private final String label;

        LineNumbersMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static LineNumbersMode fromLabel(String label) {
            for (LineNumbersMode m : values()) {
                if (m.label.equalsIgnoreCase(label) || m.name().equalsIgnoreCase(label)) {
                    return m;
                }
            }
            return ABSOLUTE;
        }
    }

    @FunctionalInterface
    public interface Listener {
        void onSettingsChanged(EditorAppearanceSettings settings);
    }

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    // 1. Caret & Occurrences
    private boolean caretBlinking = true;
    private int caretBlinkingMs = 500;
    private boolean useBlockCaret = false;
    private boolean useFullLineHeightCaret = false;
    private boolean highlightOccurrences = true;
    private boolean showHardWrapAndVisualGuides = true;

    // 2. Line numbers
    private boolean showLineNumbers = true;
    private LineNumbersMode lineNumbersMode = LineNumbersMode.ABSOLUTE;

    // 3. Method separators & Whitespaces
    private boolean showMethodSeparators = false;
    private boolean showWhitespaces = false;
    private boolean whitespaceLeading = true;
    private boolean whitespaceInner = true;
    private boolean whitespaceTrailing = true;
    private boolean whitespaceSelection = true;

    // 4. Guides, bulbs, docs & hints
    private boolean showIndentGuides = true;
    private boolean showIntentionBulb = true;
    private boolean showIntentionPreview = true;
    private boolean renderDocComments = false;
    private boolean showCodeLensOnScrollbarHover = true;
    private boolean useEditorFontForInlayHints = false;

    // 5. HTML/XML tag tree & Colors & Languages
    private boolean enableTagTreeHighlighting = true;
    private int tagTreeHighlightLevels = 6;
    private double tagTreeHighlightOpacity = 0.1;
    private boolean showCssColorPreviewAsBackground = false;
    private boolean highlightRDocSyntaxInComments = true;
    private boolean showPhpClassAndNamespaceSeparators = false;
    private boolean alwaysEnablePhpCodeBackgroundHighlighting = false;
    private boolean alwaysEnableBladeTemplateHighlighting = false;

    public EditorAppearanceSettings() {
        initDefaults();
        load();
    }

    private void initDefaults() {
        caretBlinking = true;
        caretBlinkingMs = 500;
        useBlockCaret = false;
        useFullLineHeightCaret = false;
        highlightOccurrences = true;
        showHardWrapAndVisualGuides = true;

        showLineNumbers = true;
        lineNumbersMode = LineNumbersMode.ABSOLUTE;

        showMethodSeparators = false;
        showWhitespaces = false;
        whitespaceLeading = true;
        whitespaceInner = true;
        whitespaceTrailing = true;
        whitespaceSelection = true;

        showIndentGuides = true;
        showIntentionBulb = true;
        showIntentionPreview = true;
        renderDocComments = false;
        showCodeLensOnScrollbarHover = true;
        useEditorFontForInlayHints = false;

        enableTagTreeHighlighting = true;
        tagTreeHighlightLevels = 6;
        tagTreeHighlightOpacity = 0.1;
        showCssColorPreviewAsBackground = false;
        highlightRDocSyntaxInComments = true;
        showPhpClassAndNamespaceSeparators = false;
        alwaysEnablePhpCodeBackgroundHighlighting = false;
        alwaysEnableBladeTemplateHighlighting = false;
    }

    public synchronized void resetToDefaults() {
        initDefaults();
        fireChanged();
    }

    public synchronized void load() {
        String val;

        val = Settings.get("editor.appearance.caretBlinking");
        if (val != null) caretBlinking = Boolean.parseBoolean(val);

        val = Settings.get("editor.appearance.caretBlinkingMs");
        if (val != null) {
            try { caretBlinkingMs = Integer.parseInt(val.trim()); } catch (Exception ignored) {}
        }

        val = Settings.get("editor.appearance.useBlockCaret");
        if (val != null) useBlockCaret = Boolean.parseBoolean(val);

        val = Settings.get("editor.appearance.useFullLineHeightCaret");
        if (val != null) useFullLineHeightCaret = Boolean.parseBoolean(val);

        val = Settings.get("editor.appearance.highlightOccurrences");
        if (val != null) highlightOccurrences = Boolean.parseBoolean(val);

        val = Settings.get("editor.appearance.showHardWrap");
        if (val != null) showHardWrapAndVisualGuides = Boolean.parseBoolean(val);

        val = Settings.get("editor.appearance.showLineNumbers");
        if (val != null) showLineNumbers = Boolean.parseBoolean(val);

        val = Settings.get("editor.appearance.lineNumbersMode");
        if (val != null) lineNumbersMode = LineNumbersMode.fromLabel(val);

        val = Settings.get("editor.appearance.showMethodSeparators");
        if (val != null) showMethodSeparators = Boolean.parseBoolean(val);

        val = Settings.get("editor.appearance.showWhitespaces");
        if (val != null) showWhitespaces = Boolean.parseBoolean(val);

        val = Settings.get("editor.appearance.whitespaceLeading");
        if (val != null) whitespaceLeading = Boolean.parseBoolean(val);

        val = Settings.get("editor.appearance.whitespaceInner");
        if (val != null) whitespaceInner = Boolean.parseBoolean(val);

        val = Settings.get("editor.appearance.whitespaceTrailing");
        if (val != null) whitespaceTrailing = Boolean.parseBoolean(val);

        val = Settings.get("editor.appearance.whitespaceSelection");
        if (val != null) whitespaceSelection = Boolean.parseBoolean(val);

        val = Settings.get("editor.appearance.showIndentGuides");
        if (val != null) showIndentGuides = Boolean.parseBoolean(val);

        val = Settings.get("editor.appearance.showIntentionBulb");
        if (val != null) showIntentionBulb = Boolean.parseBoolean(val);

        val = Settings.get("editor.appearance.showIntentionPreview");
        if (val != null) showIntentionPreview = Boolean.parseBoolean(val);

        val = Settings.get("editor.appearance.renderDocComments");
        if (val != null) renderDocComments = Boolean.parseBoolean(val);

        val = Settings.get("editor.appearance.showCodeLensOnScrollbarHover");
        if (val != null) showCodeLensOnScrollbarHover = Boolean.parseBoolean(val);

        val = Settings.get("editor.appearance.useEditorFontForInlayHints");
        if (val != null) useEditorFontForInlayHints = Boolean.parseBoolean(val);

        val = Settings.get("editor.appearance.enableTagTreeHighlighting");
        if (val != null) enableTagTreeHighlighting = Boolean.parseBoolean(val);

        val = Settings.get("editor.appearance.tagTreeHighlightLevels");
        if (val != null) {
            try { tagTreeHighlightLevels = Integer.parseInt(val.trim()); } catch (Exception ignored) {}
        }

        val = Settings.get("editor.appearance.tagTreeHighlightOpacity");
        if (val != null) {
            try { tagTreeHighlightOpacity = Double.parseDouble(val.trim()); } catch (Exception ignored) {}
        }

        val = Settings.get("editor.appearance.showCssColorPreviewAsBackground");
        if (val != null) showCssColorPreviewAsBackground = Boolean.parseBoolean(val);

        val = Settings.get("editor.appearance.highlightRDocSyntaxInComments");
        if (val != null) highlightRDocSyntaxInComments = Boolean.parseBoolean(val);

        val = Settings.get("editor.appearance.showPhpClassAndNamespaceSeparators");
        if (val != null) showPhpClassAndNamespaceSeparators = Boolean.parseBoolean(val);

        val = Settings.get("editor.appearance.alwaysEnablePhpCodeBackgroundHighlighting");
        if (val != null) alwaysEnablePhpCodeBackgroundHighlighting = Boolean.parseBoolean(val);

        val = Settings.get("editor.appearance.alwaysEnableBladeTemplateHighlighting");
        if (val != null) alwaysEnableBladeTemplateHighlighting = Boolean.parseBoolean(val);
    }

    public synchronized void save() {
        Settings.put("editor.appearance.caretBlinking", String.valueOf(caretBlinking));
        Settings.put("editor.appearance.caretBlinkingMs", String.valueOf(caretBlinkingMs));
        Settings.put("editor.appearance.useBlockCaret", String.valueOf(useBlockCaret));
        Settings.put("editor.appearance.useFullLineHeightCaret", String.valueOf(useFullLineHeightCaret));
        Settings.put("editor.appearance.highlightOccurrences", String.valueOf(highlightOccurrences));
        Settings.put("editor.appearance.showHardWrap", String.valueOf(showHardWrapAndVisualGuides));
        Settings.put("editor.appearance.showLineNumbers", String.valueOf(showLineNumbers));
        Settings.put("editor.appearance.lineNumbersMode", lineNumbersMode.name());
        Settings.put("editor.appearance.showMethodSeparators", String.valueOf(showMethodSeparators));
        Settings.put("editor.appearance.showWhitespaces", String.valueOf(showWhitespaces));
        Settings.put("editor.appearance.whitespaceLeading", String.valueOf(whitespaceLeading));
        Settings.put("editor.appearance.whitespaceInner", String.valueOf(whitespaceInner));
        Settings.put("editor.appearance.whitespaceTrailing", String.valueOf(whitespaceTrailing));
        Settings.put("editor.appearance.whitespaceSelection", String.valueOf(whitespaceSelection));
        Settings.put("editor.appearance.showIndentGuides", String.valueOf(showIndentGuides));
        Settings.put("editor.appearance.showIntentionBulb", String.valueOf(showIntentionBulb));
        Settings.put("editor.appearance.showIntentionPreview", String.valueOf(showIntentionPreview));
        Settings.put("editor.appearance.renderDocComments", String.valueOf(renderDocComments));
        Settings.put("editor.appearance.showCodeLensOnScrollbarHover", String.valueOf(showCodeLensOnScrollbarHover));
        Settings.put("editor.appearance.useEditorFontForInlayHints", String.valueOf(useEditorFontForInlayHints));
        Settings.put("editor.appearance.enableTagTreeHighlighting", String.valueOf(enableTagTreeHighlighting));
        Settings.put("editor.appearance.tagTreeHighlightLevels", String.valueOf(tagTreeHighlightLevels));
        Settings.put("editor.appearance.tagTreeHighlightOpacity", String.valueOf(tagTreeHighlightOpacity));
        Settings.put("editor.appearance.showCssColorPreviewAsBackground", String.valueOf(showCssColorPreviewAsBackground));
        Settings.put("editor.appearance.highlightRDocSyntaxInComments", String.valueOf(highlightRDocSyntaxInComments));
        Settings.put("editor.appearance.showPhpClassAndNamespaceSeparators", String.valueOf(showPhpClassAndNamespaceSeparators));
        Settings.put("editor.appearance.alwaysEnablePhpCodeBackgroundHighlighting", String.valueOf(alwaysEnablePhpCodeBackgroundHighlighting));
        Settings.put("editor.appearance.alwaysEnableBladeTemplateHighlighting", String.valueOf(alwaysEnableBladeTemplateHighlighting));

        fireChanged();
    }

    public synchronized EditorAppearanceSettings copy() {
        EditorAppearanceSettings c = new EditorAppearanceSettings();
        c.copyFrom(this);
        return c;
    }

    public synchronized void copyFrom(EditorAppearanceSettings o) {
        this.caretBlinking = o.caretBlinking;
        this.caretBlinkingMs = o.caretBlinkingMs;
        this.useBlockCaret = o.useBlockCaret;
        this.useFullLineHeightCaret = o.useFullLineHeightCaret;
        this.highlightOccurrences = o.highlightOccurrences;
        this.showHardWrapAndVisualGuides = o.showHardWrapAndVisualGuides;

        this.showLineNumbers = o.showLineNumbers;
        this.lineNumbersMode = o.lineNumbersMode;

        this.showMethodSeparators = o.showMethodSeparators;
        this.showWhitespaces = o.showWhitespaces;
        this.whitespaceLeading = o.whitespaceLeading;
        this.whitespaceInner = o.whitespaceInner;
        this.whitespaceTrailing = o.whitespaceTrailing;
        this.whitespaceSelection = o.whitespaceSelection;

        this.showIndentGuides = o.showIndentGuides;
        this.showIntentionBulb = o.showIntentionBulb;
        this.showIntentionPreview = o.showIntentionPreview;
        this.renderDocComments = o.renderDocComments;
        this.showCodeLensOnScrollbarHover = o.showCodeLensOnScrollbarHover;
        this.useEditorFontForInlayHints = o.useEditorFontForInlayHints;

        this.enableTagTreeHighlighting = o.enableTagTreeHighlighting;
        this.tagTreeHighlightLevels = o.tagTreeHighlightLevels;
        this.tagTreeHighlightOpacity = o.tagTreeHighlightOpacity;
        this.showCssColorPreviewAsBackground = o.showCssColorPreviewAsBackground;
        this.highlightRDocSyntaxInComments = o.highlightRDocSyntaxInComments;
        this.showPhpClassAndNamespaceSeparators = o.showPhpClassAndNamespaceSeparators;
        this.alwaysEnablePhpCodeBackgroundHighlighting = o.alwaysEnablePhpCodeBackgroundHighlighting;
        this.alwaysEnableBladeTemplateHighlighting = o.alwaysEnableBladeTemplateHighlighting;
    }

    public synchronized boolean isModified(EditorAppearanceSettings o) {
        return this.caretBlinking != o.caretBlinking ||
                this.caretBlinkingMs != o.caretBlinkingMs ||
                this.useBlockCaret != o.useBlockCaret ||
                this.useFullLineHeightCaret != o.useFullLineHeightCaret ||
                this.highlightOccurrences != o.highlightOccurrences ||
                this.showHardWrapAndVisualGuides != o.showHardWrapAndVisualGuides ||
                this.showLineNumbers != o.showLineNumbers ||
                this.lineNumbersMode != o.lineNumbersMode ||
                this.showMethodSeparators != o.showMethodSeparators ||
                this.showWhitespaces != o.showWhitespaces ||
                this.whitespaceLeading != o.whitespaceLeading ||
                this.whitespaceInner != o.whitespaceInner ||
                this.whitespaceTrailing != o.whitespaceTrailing ||
                this.whitespaceSelection != o.whitespaceSelection ||
                this.showIndentGuides != o.showIndentGuides ||
                this.showIntentionBulb != o.showIntentionBulb ||
                this.showIntentionPreview != o.showIntentionPreview ||
                this.renderDocComments != o.renderDocComments ||
                this.showCodeLensOnScrollbarHover != o.showCodeLensOnScrollbarHover ||
                this.useEditorFontForInlayHints != o.useEditorFontForInlayHints ||
                this.enableTagTreeHighlighting != o.enableTagTreeHighlighting ||
                this.tagTreeHighlightLevels != o.tagTreeHighlightLevels ||
                Math.abs(this.tagTreeHighlightOpacity - o.tagTreeHighlightOpacity) > 0.001 ||
                this.showCssColorPreviewAsBackground != o.showCssColorPreviewAsBackground ||
                this.highlightRDocSyntaxInComments != o.highlightRDocSyntaxInComments ||
                this.showPhpClassAndNamespaceSeparators != o.showPhpClassAndNamespaceSeparators ||
                this.alwaysEnablePhpCodeBackgroundHighlighting != o.alwaysEnablePhpCodeBackgroundHighlighting ||
                this.alwaysEnableBladeTemplateHighlighting != o.alwaysEnableBladeTemplateHighlighting;
    }

    public void addListener(Listener l) {
        if (l != null && !listeners.contains(l)) listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    private void fireChanged() {
        for (Listener l : listeners) {
            try {
                l.onSettingsChanged(this);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    // --- Getters and Setters ---

    public boolean isCaretBlinking() { return caretBlinking; }
    public void setCaretBlinking(boolean caretBlinking) { this.caretBlinking = caretBlinking; }

    public int getCaretBlinkingMs() { return caretBlinkingMs; }
    public void setCaretBlinkingMs(int caretBlinkingMs) { this.caretBlinkingMs = caretBlinkingMs; }

    public boolean isUseBlockCaret() { return useBlockCaret; }
    public void setUseBlockCaret(boolean useBlockCaret) { this.useBlockCaret = useBlockCaret; }

    public boolean isUseFullLineHeightCaret() { return useFullLineHeightCaret; }
    public void setUseFullLineHeightCaret(boolean useFullLineHeightCaret) { this.useFullLineHeightCaret = useFullLineHeightCaret; }

    public boolean isHighlightOccurrences() { return highlightOccurrences; }
    public void setHighlightOccurrences(boolean highlightOccurrences) { this.highlightOccurrences = highlightOccurrences; }

    public boolean isShowHardWrapAndVisualGuides() { return showHardWrapAndVisualGuides; }
    public void setShowHardWrapAndVisualGuides(boolean showHardWrapAndVisualGuides) { this.showHardWrapAndVisualGuides = showHardWrapAndVisualGuides; }

    public boolean isShowLineNumbers() { return showLineNumbers; }
    public void setShowLineNumbers(boolean showLineNumbers) { this.showLineNumbers = showLineNumbers; }

    public LineNumbersMode getLineNumbersMode() { return lineNumbersMode; }
    public void setLineNumbersMode(LineNumbersMode lineNumbersMode) { this.lineNumbersMode = lineNumbersMode != null ? lineNumbersMode : LineNumbersMode.ABSOLUTE; }

    public boolean isShowMethodSeparators() { return showMethodSeparators; }
    public void setShowMethodSeparators(boolean showMethodSeparators) { this.showMethodSeparators = showMethodSeparators; }

    public boolean isShowWhitespaces() { return showWhitespaces; }
    public void setShowWhitespaces(boolean showWhitespaces) { this.showWhitespaces = showWhitespaces; }

    public boolean isWhitespaceLeading() { return whitespaceLeading; }
    public void setWhitespaceLeading(boolean whitespaceLeading) { this.whitespaceLeading = whitespaceLeading; }

    public boolean isWhitespaceInner() { return whitespaceInner; }
    public void setWhitespaceInner(boolean whitespaceInner) { this.whitespaceInner = whitespaceInner; }

    public boolean isWhitespaceTrailing() { return whitespaceTrailing; }
    public void setWhitespaceTrailing(boolean whitespaceTrailing) { this.whitespaceTrailing = whitespaceTrailing; }

    public boolean isWhitespaceSelection() { return whitespaceSelection; }
    public void setWhitespaceSelection(boolean whitespaceSelection) { this.whitespaceSelection = whitespaceSelection; }

    public boolean isShowIndentGuides() { return showIndentGuides; }
    public void setShowIndentGuides(boolean showIndentGuides) { this.showIndentGuides = showIndentGuides; }

    public boolean isShowIntentionBulb() { return showIntentionBulb; }
    public void setShowIntentionBulb(boolean showIntentionBulb) { this.showIntentionBulb = showIntentionBulb; }

    public boolean isShowIntentionPreview() { return showIntentionPreview; }
    public void setShowIntentionPreview(boolean showIntentionPreview) { this.showIntentionPreview = showIntentionPreview; }

    public boolean isRenderDocComments() { return renderDocComments; }
    public void setRenderDocComments(boolean renderDocComments) { this.renderDocComments = renderDocComments; }

    public boolean isShowCodeLensOnScrollbarHover() { return showCodeLensOnScrollbarHover; }
    public void setShowCodeLensOnScrollbarHover(boolean showCodeLensOnScrollbarHover) { this.showCodeLensOnScrollbarHover = showCodeLensOnScrollbarHover; }

    public boolean isUseEditorFontForInlayHints() { return useEditorFontForInlayHints; }
    public void setUseEditorFontForInlayHints(boolean useEditorFontForInlayHints) { this.useEditorFontForInlayHints = useEditorFontForInlayHints; }

    public boolean isEnableTagTreeHighlighting() { return enableTagTreeHighlighting; }
    public void setEnableTagTreeHighlighting(boolean enableTagTreeHighlighting) { this.enableTagTreeHighlighting = enableTagTreeHighlighting; }

    public int getTagTreeHighlightLevels() { return tagTreeHighlightLevels; }
    public void setTagTreeHighlightLevels(int tagTreeHighlightLevels) { this.tagTreeHighlightLevels = tagTreeHighlightLevels; }

    public double getTagTreeHighlightOpacity() { return tagTreeHighlightOpacity; }
    public void setTagTreeHighlightOpacity(double tagTreeHighlightOpacity) { this.tagTreeHighlightOpacity = tagTreeHighlightOpacity; }

    public boolean isShowCssColorPreviewAsBackground() { return showCssColorPreviewAsBackground; }
    public void setShowCssColorPreviewAsBackground(boolean showCssColorPreviewAsBackground) { this.showCssColorPreviewAsBackground = showCssColorPreviewAsBackground; }

    public boolean isHighlightRDocSyntaxInComments() { return highlightRDocSyntaxInComments; }
    public void setHighlightRDocSyntaxInComments(boolean highlightRDocSyntaxInComments) { this.highlightRDocSyntaxInComments = highlightRDocSyntaxInComments; }

    public boolean isShowPhpClassAndNamespaceSeparators() { return showPhpClassAndNamespaceSeparators; }
    public void setShowPhpClassAndNamespaceSeparators(boolean showPhpClassAndNamespaceSeparators) { this.showPhpClassAndNamespaceSeparators = showPhpClassAndNamespaceSeparators; }

    public boolean isAlwaysEnablePhpCodeBackgroundHighlighting() { return alwaysEnablePhpCodeBackgroundHighlighting; }
    public void setAlwaysEnablePhpCodeBackgroundHighlighting(boolean alwaysEnablePhpCodeBackgroundHighlighting) { this.alwaysEnablePhpCodeBackgroundHighlighting = alwaysEnablePhpCodeBackgroundHighlighting; }

    public boolean isAlwaysEnableBladeTemplateHighlighting() { return alwaysEnableBladeTemplateHighlighting; }
    public void setAlwaysEnableBladeTemplateHighlighting(boolean alwaysEnableBladeTemplateHighlighting) { this.alwaysEnableBladeTemplateHighlighting = alwaysEnableBladeTemplateHighlighting; }
}
