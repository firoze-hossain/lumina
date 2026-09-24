package dev.lumina.settings;

import dev.lumina.settings.EditorGeneralSettings.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EditorGeneralSettingsTest {

    private EditorGeneralSettings settings;

    @BeforeEach
    void setUp() {
        dev.lumina.util.Settings.clear();
        settings = new EditorGeneralSettings();
        settings.resetToDefaults();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        dev.lumina.util.Settings.clear();
    }

    @Test
    void testDefaultsMatchIntelliJIdea() {
        assertFalse(settings.isMouseControlChangeFontSize());
        assertEquals(MouseWheelFontSizeScope.ACTIVE_EDITOR, settings.getMouseControlFontSizeScope());
        assertTrue(settings.isMoveCodeFragmentsDragAndDrop());

        assertFalse(settings.isSoftWrapFilesEnabled());
        assertEquals("*.md; *.txt; *.rst; *.adoc", settings.getSoftWrapFilePatterns());
        assertTrue(settings.isUseOriginalLineIndentForWraps());
        assertEquals(0, settings.getAdditionalIndentSymbols());
        assertTrue(settings.isOnlyShowSoftWrapIndicatorsCurrentLine());

        assertFalse(settings.isCaretPlacementAfterEndOfLine());
        assertFalse(settings.isCaretPlacementInsideTabs());
        assertFalse(settings.isVirtualSpaceAtBottom());

        assertEquals(1, settings.getVerticalScrollOffset());
        assertEquals(0, settings.getVerticalScrollJump());
        assertEquals(3, settings.getHorizontalScrollOffset());
        assertEquals(0, settings.getHorizontalScrollJump());

        assertEquals(WordBoundaryPolicy.CURRENT_WORD_BOUNDARIES, settings.getWordBoundaryPolicy());
        assertEquals("Jump to the current word boundaries", settings.getWordBoundaryPolicy().getLabel());
        assertEquals("IDE default", settings.getWordBoundaryPolicy().getBadge());

        assertEquals(LineBreakPolicy.NEXT_PREV_LINE_BOUNDARIES, settings.getLineBreakPolicy());
        assertEquals("Jump to the next/previous line boundaries", settings.getLineBreakPolicy().getLabel());
        assertEquals("IDE default", settings.getLineBreakPolicy().getBadge());

        assertTrue(settings.isSmoothScrolling());
        assertEquals(CaretBehavior.KEEP_CARET_SCROLL_CANVAS, settings.getCaretBehavior());

        assertTrue(settings.isCopyAsRichText());
        assertEquals("Active scheme", settings.getRichTextColorScheme());
        assertEquals(8, EditorGeneralSettings.RICH_TEXT_COLOR_SCHEMES.size());

        assertTrue(settings.isRemoveTrailingSpacesOnSave());
        assertEquals(TrailingSpacesMode.MODIFIED_LINES, settings.getTrailingSpacesMode());
        assertTrue(settings.isKeepTrailingSpacesOnCaretLine());
        assertFalse(settings.isRemoveTrailingBlankLinesAtEof());
        assertFalse(settings.isEnsureEndsWithLineBreak());
    }

    @Test
    void testSoftWrapPatternMatching() {
        settings.setSoftWrapFilesEnabled(true);
        settings.setSoftWrapFilePatterns("*.md; *.txt; *.rst; *.adoc");

        assertTrue(settings.matchesSoftWrapPattern("README.md"));
        assertTrue(settings.matchesSoftWrapPattern("notes.txt"));
        assertTrue(settings.matchesSoftWrapPattern("guide.rst"));
        assertTrue(settings.matchesSoftWrapPattern("manual.adoc"));
        assertFalse(settings.matchesSoftWrapPattern("LuminaApp.java"));
        assertFalse(settings.matchesSoftWrapPattern("pom.xml"));

        settings.setSoftWrapFilesEnabled(false);
        assertFalse(settings.matchesSoftWrapPattern("README.md"));
    }

    @Test
    void testProcessTextForSaveTrailingSpaces() {
        settings.setRemoveTrailingSpacesOnSave(true);
        settings.setKeepTrailingSpacesOnCaretLine(true);
        settings.setRemoveTrailingBlankLinesAtEof(false);
        settings.setEnsureEndsWithLineBreak(false);

        String input = "line1   \nline2   \nline3   ";
        // Caret is on line 2 (1-based)
        String processed = settings.processTextForSave(input, 2);
        assertEquals("line1\nline2   \nline3", processed);

        // Caret line not preserved if setting is false
        settings.setKeepTrailingSpacesOnCaretLine(false);
        String processedNoKeep = settings.processTextForSave(input, 2);
        assertEquals("line1\nline2\nline3", processedNoKeep);
    }

    @Test
    void testProcessTextForSaveBlankLinesAndNewline() {
        settings.setRemoveTrailingSpacesOnSave(false);
        settings.setRemoveTrailingBlankLinesAtEof(true);
        settings.setEnsureEndsWithLineBreak(true);

        String input = "class Foo {\n}\n\n\n";
        String processed = settings.processTextForSave(input, 1);
        assertEquals("class Foo {\n}\n", processed);
    }

    @Test
    void testSaveAndLoadPersistence() {
        settings.setMouseControlChangeFontSize(true);
        settings.setMouseControlFontSizeScope(MouseWheelFontSizeScope.ALL_EDITORS);
        settings.setSoftWrapFilesEnabled(true);
        settings.setSoftWrapFilePatterns("*.log; *.out");
        settings.setVerticalScrollOffset(5);
        settings.setWordBoundaryPolicy(WordBoundaryPolicy.ALWAYS_WORD_START);
        settings.setLineBreakPolicy(LineBreakPolicy.IGNORE_LINE_BREAKS);
        settings.setRichTextColorScheme("Darcula");
        settings.setEnsureEndsWithLineBreak(true);

        settings.save();

        EditorGeneralSettings loaded = new EditorGeneralSettings();
        loaded.load();

        assertTrue(loaded.isMouseControlChangeFontSize());
        assertEquals(MouseWheelFontSizeScope.ALL_EDITORS, loaded.getMouseControlFontSizeScope());
        assertTrue(loaded.isSoftWrapFilesEnabled());
        assertEquals("*.log; *.out", loaded.getSoftWrapFilePatterns());
        assertEquals(5, loaded.getVerticalScrollOffset());
        assertEquals(WordBoundaryPolicy.ALWAYS_WORD_START, loaded.getWordBoundaryPolicy());
        assertEquals(LineBreakPolicy.IGNORE_LINE_BREAKS, loaded.getLineBreakPolicy());
        assertEquals("Darcula", loaded.getRichTextColorScheme());
        assertTrue(loaded.isEnsureEndsWithLineBreak());

        // Reset to default for clean environment
        EditorGeneralSettings fresh = new EditorGeneralSettings();
        fresh.save();
    }
}
