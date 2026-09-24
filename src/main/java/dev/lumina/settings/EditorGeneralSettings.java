package dev.lumina.settings;

import dev.lumina.util.Settings;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * Dynamic persistent configuration and model for IntelliJ IDEA-style Editor > General settings.
 * Backed by ~/.lumina/lumina.properties, supporting dynamic listeners,
 * real-time UI synchronization, and on-save formatting.
 */
public final class EditorGeneralSettings {

    private static final EditorGeneralSettings INSTANCE = new EditorGeneralSettings();

    public static EditorGeneralSettings getInstance() {
        return INSTANCE;
    }

    public enum MouseWheelFontSizeScope {
        ACTIVE_EDITOR("Active editor"),
        ALL_EDITORS("All editors");

        private final String label;
        MouseWheelFontSizeScope(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum WordBoundaryPolicy {
        CURRENT_WORD_BOUNDARIES("Jump to the current word boundaries", "IDE default"),
        ALWAYS_WORD_START("Always jump to the word start", "Windows default"),
        ALWAYS_WORD_END("Always jump to the word end", null),
        NEXT_PREV_WORD_BOUNDARIES("Jump to the next/previous word boundaries", null),
        STOP_BOTH_WORD_BOUNDARIES("Stop at both word boundaries", null);

        private final String label;
        private final String badge;
        WordBoundaryPolicy(String label, String badge) {
            this.label = label;
            this.badge = badge;
        }
        public String getLabel() { return label; }
        public String getBadge() { return badge; }
    }

    public enum LineBreakPolicy {
        NEXT_PREV_LINE_BOUNDARIES("Jump to the next/previous line boundaries", "IDE default"),
        IGNORE_LINE_BREAKS("Ignore line breaks", "Linux default"),
        STOP_BOTH_LINE_BOUNDARIES("Stop at both line boundaries", "Windows default"),
        CURRENT_LINE_BOUNDARIES("Jump to the current line boundaries", null),
        ALWAYS_LINE_START("Always jump to the line start", null),
        ALWAYS_LINE_END("Always jump to the line end", null);

        private final String label;
        private final String badge;
        LineBreakPolicy(String label, String badge) {
            this.label = label;
            this.badge = badge;
        }
        public String getLabel() { return label; }
        public String getBadge() { return badge; }
    }

    public enum CaretBehavior {
        KEEP_CARET_SCROLL_CANVAS("Keep the caret in place, scroll editor canvas"),
        MOVE_CARET_MINIMIZE_SCROLL("Move caret, minimize editor scrolling");

        private final String label;
        CaretBehavior(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum TrailingSpacesMode {
        MODIFIED_LINES("Modified lines"),
        ALL_LINES("All lines");

        private final String label;
        TrailingSpacesMode(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public static final List<String> RICH_TEXT_COLOR_SCHEMES = List.of(
            "Active scheme",
            "Light",
            "Dark",
            "High Contrast",
            "Classic Light",
            "Darcula",
            "Darcula Contrast",
            "Islands Dark"
    );

    @FunctionalInterface
    public interface Listener {
        void onSettingsChanged(EditorGeneralSettings settings);
    }

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    // 1. Mouse Control
    private boolean mouseControlChangeFontSize = false;
    private MouseWheelFontSizeScope mouseControlFontSizeScope = MouseWheelFontSizeScope.ACTIVE_EDITOR;
    private boolean moveCodeFragmentsDragAndDrop = true;

    // 2. Soft Wraps
    private boolean softWrapFilesEnabled = false;
    private String softWrapFilePatterns = "*.md; *.txt; *.rst; *.adoc";
    private boolean useOriginalLineIndentForWraps = true;
    private int additionalIndentSymbols = 0;
    private boolean onlyShowSoftWrapIndicatorsCurrentLine = true;

    // 3. Virtual Space
    private boolean caretPlacementAfterEndOfLine = false;
    private boolean caretPlacementInsideTabs = false;
    private boolean virtualSpaceAtBottom = false;

    // 4. Scroll Offset
    private int verticalScrollOffset = 1;
    private int verticalScrollJump = 0;
    private int horizontalScrollOffset = 3;
    private int horizontalScrollJump = 0;

    // 5. Caret Movement
    private WordBoundaryPolicy wordBoundaryPolicy = WordBoundaryPolicy.CURRENT_WORD_BOUNDARIES;
    private LineBreakPolicy lineBreakPolicy = LineBreakPolicy.NEXT_PREV_LINE_BOUNDARIES;

    // 6. Scrolling
    private boolean smoothScrolling = true;
    private CaretBehavior caretBehavior = CaretBehavior.KEEP_CARET_SCROLL_CANVAS;

    // 7. Rich-Text Copy
    private boolean copyAsRichText = true;
    private String richTextColorScheme = "Active scheme";

    // 8. On Save
    private boolean removeTrailingSpacesOnSave = true;
    private TrailingSpacesMode trailingSpacesMode = TrailingSpacesMode.MODIFIED_LINES;
    private boolean keepTrailingSpacesOnCaretLine = true;
    private boolean removeTrailingBlankLinesAtEof = false;
    private boolean ensureEndsWithLineBreak = false;

    public EditorGeneralSettings() {
        load();
    }

    public void resetToDefaults() {
        mouseControlChangeFontSize = false;
        mouseControlFontSizeScope = MouseWheelFontSizeScope.ACTIVE_EDITOR;
        moveCodeFragmentsDragAndDrop = true;

        softWrapFilesEnabled = false;
        softWrapFilePatterns = "*.md; *.txt; *.rst; *.adoc";
        useOriginalLineIndentForWraps = true;
        additionalIndentSymbols = 0;
        onlyShowSoftWrapIndicatorsCurrentLine = true;

        caretPlacementAfterEndOfLine = false;
        caretPlacementInsideTabs = false;
        virtualSpaceAtBottom = false;

        verticalScrollOffset = 1;
        verticalScrollJump = 0;
        horizontalScrollOffset = 3;
        horizontalScrollJump = 0;

        wordBoundaryPolicy = WordBoundaryPolicy.CURRENT_WORD_BOUNDARIES;
        lineBreakPolicy = LineBreakPolicy.NEXT_PREV_LINE_BOUNDARIES;

        smoothScrolling = true;
        caretBehavior = CaretBehavior.KEEP_CARET_SCROLL_CANVAS;

        copyAsRichText = true;
        richTextColorScheme = "Active scheme";

        removeTrailingSpacesOnSave = true;
        trailingSpacesMode = TrailingSpacesMode.MODIFIED_LINES;
        keepTrailingSpacesOnCaretLine = true;
        removeTrailingBlankLinesAtEof = false;
        ensureEndsWithLineBreak = false;
    }

    public void addListener(Listener l) {
        if (l != null && !listeners.contains(l)) {
            listeners.add(l);
        }
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    public void fireChanged() {
        for (Listener l : listeners) {
            l.onSettingsChanged(this);
        }
    }

    /**
     * Loads settings from persistent store (lumina.properties).
     */
    public synchronized void load() {
        // Mouse Control
        String val = Settings.get("editor.general.mouse.changeFontSize");
        if (val != null) mouseControlChangeFontSize = Boolean.parseBoolean(val);

        val = Settings.get("editor.general.mouse.fontSizeScope");
        if (val != null) {
            try { mouseControlFontSizeScope = MouseWheelFontSizeScope.valueOf(val); } catch (Exception ignored) {}
        }

        val = Settings.get("editor.general.mouse.dragDrop");
        if (val != null) moveCodeFragmentsDragAndDrop = Boolean.parseBoolean(val);

        // Soft Wraps
        val = Settings.get("editor.general.softwraps.enabled");
        if (val != null) softWrapFilesEnabled = Boolean.parseBoolean(val);

        val = Settings.get("editor.general.softwraps.patterns");
        if (val != null) softWrapFilePatterns = val;

        val = Settings.get("editor.general.softwraps.useOriginalIndent");
        if (val != null) useOriginalLineIndentForWraps = Boolean.parseBoolean(val);

        val = Settings.get("editor.general.softwraps.additionalIndent");
        if (val != null) {
            try { additionalIndentSymbols = Integer.parseInt(val); } catch (Exception ignored) {}
        }

        val = Settings.get("editor.general.softwraps.currentLineOnly");
        if (val != null) onlyShowSoftWrapIndicatorsCurrentLine = Boolean.parseBoolean(val);

        // Virtual Space
        val = Settings.get("editor.general.virtual.afterEol");
        if (val != null) caretPlacementAfterEndOfLine = Boolean.parseBoolean(val);

        val = Settings.get("editor.general.virtual.insideTabs");
        if (val != null) caretPlacementInsideTabs = Boolean.parseBoolean(val);

        val = Settings.get("editor.general.virtual.atBottom");
        if (val != null) virtualSpaceAtBottom = Boolean.parseBoolean(val);

        // Scroll Offset
        val = Settings.get("editor.general.scroll.vertOffset");
        if (val != null) {
            try { verticalScrollOffset = Integer.parseInt(val); } catch (Exception ignored) {}
        }

        val = Settings.get("editor.general.scroll.vertJump");
        if (val != null) {
            try { verticalScrollJump = Integer.parseInt(val); } catch (Exception ignored) {}
        }

        val = Settings.get("editor.general.scroll.horizOffset");
        if (val != null) {
            try { horizontalScrollOffset = Integer.parseInt(val); } catch (Exception ignored) {}
        }

        val = Settings.get("editor.general.scroll.horizJump");
        if (val != null) {
            try { horizontalScrollJump = Integer.parseInt(val); } catch (Exception ignored) {}
        }

        // Caret Movement
        val = Settings.get("editor.general.caret.wordPolicy");
        if (val != null) {
            try { wordBoundaryPolicy = WordBoundaryPolicy.valueOf(val); } catch (Exception ignored) {}
        }

        val = Settings.get("editor.general.caret.lineBreakPolicy");
        if (val != null) {
            try { lineBreakPolicy = LineBreakPolicy.valueOf(val); } catch (Exception ignored) {}
        }

        // Scrolling
        val = Settings.get("editor.general.scroll.smooth");
        if (val != null) smoothScrolling = Boolean.parseBoolean(val);

        val = Settings.get("editor.general.scroll.caretBehavior");
        if (val != null) {
            try { caretBehavior = CaretBehavior.valueOf(val); } catch (Exception ignored) {}
        }

        // Rich-Text Copy
        val = Settings.get("editor.general.copy.richText");
        if (val != null) copyAsRichText = Boolean.parseBoolean(val);

        val = Settings.get("editor.general.copy.scheme");
        if (val != null && !val.isBlank()) richTextColorScheme = val;

        // On Save
        val = Settings.get("editor.general.save.removeTrailingSpaces");
        if (val != null) removeTrailingSpacesOnSave = Boolean.parseBoolean(val);

        val = Settings.get("editor.general.save.trailingSpacesMode");
        if (val != null) {
            try { trailingSpacesMode = TrailingSpacesMode.valueOf(val); } catch (Exception ignored) {}
        }

        val = Settings.get("editor.general.save.keepTrailingCaret");
        if (val != null) keepTrailingSpacesOnCaretLine = Boolean.parseBoolean(val);

        val = Settings.get("editor.general.save.removeTrailingBlankLines");
        if (val != null) removeTrailingBlankLinesAtEof = Boolean.parseBoolean(val);

        val = Settings.get("editor.general.save.ensureLineBreak");
        if (val != null) ensureEndsWithLineBreak = Boolean.parseBoolean(val);
    }

    /**
     * Persists settings to lumina.properties.
     */
    public synchronized void save() {
        Settings.put("editor.general.mouse.changeFontSize", String.valueOf(mouseControlChangeFontSize));
        Settings.put("editor.general.mouse.fontSizeScope", mouseControlFontSizeScope.name());
        Settings.put("editor.general.mouse.dragDrop", String.valueOf(moveCodeFragmentsDragAndDrop));

        Settings.put("editor.general.softwraps.enabled", String.valueOf(softWrapFilesEnabled));
        Settings.put("editor.general.softwraps.patterns", softWrapFilePatterns);
        Settings.put("editor.general.softwraps.useOriginalIndent", String.valueOf(useOriginalLineIndentForWraps));
        Settings.put("editor.general.softwraps.additionalIndent", String.valueOf(additionalIndentSymbols));
        Settings.put("editor.general.softwraps.currentLineOnly", String.valueOf(onlyShowSoftWrapIndicatorsCurrentLine));

        Settings.put("editor.general.virtual.afterEol", String.valueOf(caretPlacementAfterEndOfLine));
        Settings.put("editor.general.virtual.insideTabs", String.valueOf(caretPlacementInsideTabs));
        Settings.put("editor.general.virtual.atBottom", String.valueOf(virtualSpaceAtBottom));

        Settings.put("editor.general.scroll.vertOffset", String.valueOf(verticalScrollOffset));
        Settings.put("editor.general.scroll.vertJump", String.valueOf(verticalScrollJump));
        Settings.put("editor.general.scroll.horizOffset", String.valueOf(horizontalScrollOffset));
        Settings.put("editor.general.scroll.horizJump", String.valueOf(horizontalScrollJump));

        Settings.put("editor.general.caret.wordPolicy", wordBoundaryPolicy.name());
        Settings.put("editor.general.caret.lineBreakPolicy", lineBreakPolicy.name());

        Settings.put("editor.general.scroll.smooth", String.valueOf(smoothScrolling));
        Settings.put("editor.general.scroll.caretBehavior", caretBehavior.name());

        Settings.put("editor.general.copy.richText", String.valueOf(copyAsRichText));
        Settings.put("editor.general.copy.scheme", richTextColorScheme);

        Settings.put("editor.general.save.removeTrailingSpaces", String.valueOf(removeTrailingSpacesOnSave));
        Settings.put("editor.general.save.trailingSpacesMode", trailingSpacesMode.name());
        Settings.put("editor.general.save.keepTrailingCaret", String.valueOf(keepTrailingSpacesOnCaretLine));
        Settings.put("editor.general.save.removeTrailingBlankLines", String.valueOf(removeTrailingBlankLinesAtEof));
        Settings.put("editor.general.save.ensureLineBreak", String.valueOf(ensureEndsWithLineBreak));
    }

    public synchronized void copyFrom(EditorGeneralSettings o) {
        this.mouseControlChangeFontSize = o.mouseControlChangeFontSize;
        this.mouseControlFontSizeScope = o.mouseControlFontSizeScope;
        this.moveCodeFragmentsDragAndDrop = o.moveCodeFragmentsDragAndDrop;

        this.softWrapFilesEnabled = o.softWrapFilesEnabled;
        this.softWrapFilePatterns = o.softWrapFilePatterns;
        this.useOriginalLineIndentForWraps = o.useOriginalLineIndentForWraps;
        this.additionalIndentSymbols = o.additionalIndentSymbols;
        this.onlyShowSoftWrapIndicatorsCurrentLine = o.onlyShowSoftWrapIndicatorsCurrentLine;

        this.caretPlacementAfterEndOfLine = o.caretPlacementAfterEndOfLine;
        this.caretPlacementInsideTabs = o.caretPlacementInsideTabs;
        this.virtualSpaceAtBottom = o.virtualSpaceAtBottom;

        this.verticalScrollOffset = o.verticalScrollOffset;
        this.verticalScrollJump = o.verticalScrollJump;
        this.horizontalScrollOffset = o.horizontalScrollOffset;
        this.horizontalScrollJump = o.horizontalScrollJump;

        this.wordBoundaryPolicy = o.wordBoundaryPolicy;
        this.lineBreakPolicy = o.lineBreakPolicy;

        this.smoothScrolling = o.smoothScrolling;
        this.caretBehavior = o.caretBehavior;

        this.copyAsRichText = o.copyAsRichText;
        this.richTextColorScheme = o.richTextColorScheme;

        this.removeTrailingSpacesOnSave = o.removeTrailingSpacesOnSave;
        this.trailingSpacesMode = o.trailingSpacesMode;
        this.keepTrailingSpacesOnCaretLine = o.keepTrailingSpacesOnCaretLine;
        this.removeTrailingBlankLinesAtEof = o.removeTrailingBlankLinesAtEof;
        this.ensureEndsWithLineBreak = o.ensureEndsWithLineBreak;
    }

    public boolean isModified(EditorGeneralSettings o) {
        return this.mouseControlChangeFontSize != o.mouseControlChangeFontSize
                || this.mouseControlFontSizeScope != o.mouseControlFontSizeScope
                || this.moveCodeFragmentsDragAndDrop != o.moveCodeFragmentsDragAndDrop
                || this.softWrapFilesEnabled != o.softWrapFilesEnabled
                || !Objects.equals(this.softWrapFilePatterns, o.softWrapFilePatterns)
                || this.useOriginalLineIndentForWraps != o.useOriginalLineIndentForWraps
                || this.additionalIndentSymbols != o.additionalIndentSymbols
                || this.onlyShowSoftWrapIndicatorsCurrentLine != o.onlyShowSoftWrapIndicatorsCurrentLine
                || this.caretPlacementAfterEndOfLine != o.caretPlacementAfterEndOfLine
                || this.caretPlacementInsideTabs != o.caretPlacementInsideTabs
                || this.virtualSpaceAtBottom != o.virtualSpaceAtBottom
                || this.verticalScrollOffset != o.verticalScrollOffset
                || this.verticalScrollJump != o.verticalScrollJump
                || this.horizontalScrollOffset != o.horizontalScrollOffset
                || this.horizontalScrollJump != o.horizontalScrollJump
                || this.wordBoundaryPolicy != o.wordBoundaryPolicy
                || this.lineBreakPolicy != o.lineBreakPolicy
                || this.smoothScrolling != o.smoothScrolling
                || this.caretBehavior != o.caretBehavior
                || this.copyAsRichText != o.copyAsRichText
                || !Objects.equals(this.richTextColorScheme, o.richTextColorScheme)
                || this.removeTrailingSpacesOnSave != o.removeTrailingSpacesOnSave
                || this.trailingSpacesMode != o.trailingSpacesMode
                || this.keepTrailingSpacesOnCaretLine != o.keepTrailingSpacesOnCaretLine
                || this.removeTrailingBlankLinesAtEof != o.removeTrailingBlankLinesAtEof
                || this.ensureEndsWithLineBreak != o.ensureEndsWithLineBreak;
    }

    /**
     * Checks if a filename matches the configured soft-wrap file patterns.
     */
    public boolean matchesSoftWrapPattern(String filename) {
        if (!softWrapFilesEnabled || filename == null || softWrapFilePatterns == null || softWrapFilePatterns.isBlank()) {
            return false;
        }
        String[] patterns = softWrapFilePatterns.split(";");
        String lowerName = filename.toLowerCase();
        for (String p : patterns) {
            String trimmed = p.trim();
            if (trimmed.isEmpty()) continue;
            String regex = "^" + Pattern.quote(trimmed)
                    .replace("*", "\\E.*\\Q")
                    .replace("?", "\\E.\\Q") + "$";
            regex = regex.replace("\\Q\\E", "");
            try {
                if (Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(lowerName).matches()) {
                    return true;
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    /**
     * Process editor text before saving according to configured On Save options.
     */
    public String processTextForSave(String text, int caretLine1Based) {
        if (text == null) return null;
        String[] lines = text.split("\n", -1);
        boolean modified = false;

        if (removeTrailingSpacesOnSave) {
            for (int i = 0; i < lines.length; i++) {
                int line1Based = i + 1;
                if (keepTrailingSpacesOnCaretLine && line1Based == caretLine1Based) {
                    continue;
                }
                String line = lines[i];
                int end = line.length();
                while (end > 0 && Character.isWhitespace(line.charAt(end - 1))) {
                    end--;
                }
                if (end < line.length()) {
                    lines[i] = line.substring(0, end);
                    modified = true;
                }
            }
        }

        List<String> list = new ArrayList<>(Arrays.asList(lines));
        if (removeTrailingBlankLinesAtEof) {
            while (list.size() > 1 && list.get(list.size() - 1).trim().isEmpty()) {
                list.remove(list.size() - 1);
                modified = true;
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) {
                sb.append("\n");
            }
        }

        if (ensureEndsWithLineBreak && !sb.isEmpty() && sb.charAt(sb.length() - 1) != '\n') {
            sb.append("\n");
            modified = true;
        }

        return modified ? sb.toString() : text;
    }

    // Getters and Setters
    public boolean isMouseControlChangeFontSize() { return mouseControlChangeFontSize; }
    public void setMouseControlChangeFontSize(boolean v) { this.mouseControlChangeFontSize = v; }

    public MouseWheelFontSizeScope getMouseControlFontSizeScope() { return mouseControlFontSizeScope; }
    public void setMouseControlFontSizeScope(MouseWheelFontSizeScope v) { this.mouseControlFontSizeScope = v; }

    public boolean isMoveCodeFragmentsDragAndDrop() { return moveCodeFragmentsDragAndDrop; }
    public void setMoveCodeFragmentsDragAndDrop(boolean v) { this.moveCodeFragmentsDragAndDrop = v; }

    public boolean isSoftWrapFilesEnabled() { return softWrapFilesEnabled; }
    public void setSoftWrapFilesEnabled(boolean v) { this.softWrapFilesEnabled = v; }

    public String getSoftWrapFilePatterns() { return softWrapFilePatterns; }
    public void setSoftWrapFilePatterns(String v) { this.softWrapFilePatterns = v; }

    public boolean isUseOriginalLineIndentForWraps() { return useOriginalLineIndentForWraps; }
    public void setUseOriginalLineIndentForWraps(boolean v) { this.useOriginalLineIndentForWraps = v; }

    public int getAdditionalIndentSymbols() { return additionalIndentSymbols; }
    public void setAdditionalIndentSymbols(int v) { this.additionalIndentSymbols = v; }

    public boolean isOnlyShowSoftWrapIndicatorsCurrentLine() { return onlyShowSoftWrapIndicatorsCurrentLine; }
    public void setOnlyShowSoftWrapIndicatorsCurrentLine(boolean v) { this.onlyShowSoftWrapIndicatorsCurrentLine = v; }

    public boolean isCaretPlacementAfterEndOfLine() { return caretPlacementAfterEndOfLine; }
    public void setCaretPlacementAfterEndOfLine(boolean v) { this.caretPlacementAfterEndOfLine = v; }

    public boolean isCaretPlacementInsideTabs() { return caretPlacementInsideTabs; }
    public void setCaretPlacementInsideTabs(boolean v) { this.caretPlacementInsideTabs = v; }

    public boolean isVirtualSpaceAtBottom() { return virtualSpaceAtBottom; }
    public void setVirtualSpaceAtBottom(boolean v) { this.virtualSpaceAtBottom = v; }

    public int getVerticalScrollOffset() { return verticalScrollOffset; }
    public void setVerticalScrollOffset(int v) { this.verticalScrollOffset = v; }

    public int getVerticalScrollJump() { return verticalScrollJump; }
    public void setVerticalScrollJump(int v) { this.verticalScrollJump = v; }

    public int getHorizontalScrollOffset() { return horizontalScrollOffset; }
    public void setHorizontalScrollOffset(int v) { this.horizontalScrollOffset = v; }

    public int getHorizontalScrollJump() { return horizontalScrollJump; }
    public void setHorizontalScrollJump(int v) { this.horizontalScrollJump = v; }

    public WordBoundaryPolicy getWordBoundaryPolicy() { return wordBoundaryPolicy; }
    public void setWordBoundaryPolicy(WordBoundaryPolicy v) { this.wordBoundaryPolicy = v; }

    public LineBreakPolicy getLineBreakPolicy() { return lineBreakPolicy; }
    public void setLineBreakPolicy(LineBreakPolicy v) { this.lineBreakPolicy = v; }

    public boolean isSmoothScrolling() { return smoothScrolling; }
    public void setSmoothScrolling(boolean v) { this.smoothScrolling = v; }

    public CaretBehavior getCaretBehavior() { return caretBehavior; }
    public void setCaretBehavior(CaretBehavior v) { this.caretBehavior = v; }

    public boolean isCopyAsRichText() { return copyAsRichText; }
    public void setCopyAsRichText(boolean v) { this.copyAsRichText = v; }

    public String getRichTextColorScheme() { return richTextColorScheme; }
    public void setRichTextColorScheme(String v) { this.richTextColorScheme = v; }

    public boolean isRemoveTrailingSpacesOnSave() { return removeTrailingSpacesOnSave; }
    public void setRemoveTrailingSpacesOnSave(boolean v) { this.removeTrailingSpacesOnSave = v; }

    public TrailingSpacesMode getTrailingSpacesMode() { return trailingSpacesMode; }
    public void setTrailingSpacesMode(TrailingSpacesMode v) { this.trailingSpacesMode = v; }

    public boolean isKeepTrailingSpacesOnCaretLine() { return keepTrailingSpacesOnCaretLine; }
    public void setKeepTrailingSpacesOnCaretLine(boolean v) { this.keepTrailingSpacesOnCaretLine = v; }

    public boolean isRemoveTrailingBlankLinesAtEof() { return removeTrailingBlankLinesAtEof; }
    public void setRemoveTrailingBlankLinesAtEof(boolean v) { this.removeTrailingBlankLinesAtEof = v; }

    public boolean isEnsureEndsWithLineBreak() { return ensureEndsWithLineBreak; }
    public void setEnsureEndsWithLineBreak(boolean v) { this.ensureEndsWithLineBreak = v; }
}
