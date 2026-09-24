package dev.lumina.settings;

import dev.lumina.settings.EditorAppearanceSettings.LineNumbersMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class EditorAppearanceSettingsTest {

    private EditorAppearanceSettings settings;

    @BeforeEach
    void setUp() {
        dev.lumina.util.Settings.clear();
        settings = EditorAppearanceSettings.getInstance();
        settings.resetToDefaults();
    }

    @AfterEach
    void tearDown() {
        dev.lumina.util.Settings.clear();
    }

    @Test
    void testDefaultsMatchIntelliJIdea() {
        assertTrue(settings.isCaretBlinking());
        assertEquals(500, settings.getCaretBlinkingMs());
        assertFalse(settings.isUseBlockCaret());
        assertFalse(settings.isUseFullLineHeightCaret());
        assertTrue(settings.isHighlightOccurrences());
        assertTrue(settings.isShowHardWrapAndVisualGuides());

        assertTrue(settings.isShowLineNumbers());
        assertEquals(LineNumbersMode.ABSOLUTE, settings.getLineNumbersMode());

        assertFalse(settings.isShowMethodSeparators());
        assertFalse(settings.isShowWhitespaces());
        assertTrue(settings.isWhitespaceLeading());
        assertTrue(settings.isWhitespaceInner());
        assertTrue(settings.isWhitespaceTrailing());
        assertTrue(settings.isWhitespaceSelection());

        assertTrue(settings.isShowIndentGuides());
        assertTrue(settings.isShowIntentionBulb());
        assertTrue(settings.isShowIntentionPreview());
        assertFalse(settings.isRenderDocComments());
        assertTrue(settings.isShowCodeLensOnScrollbarHover());
        assertFalse(settings.isUseEditorFontForInlayHints());

        assertTrue(settings.isEnableTagTreeHighlighting());
        assertEquals(6, settings.getTagTreeHighlightLevels());
        assertEquals(0.1, settings.getTagTreeHighlightOpacity(), 0.001);
        assertFalse(settings.isShowCssColorPreviewAsBackground());
        assertTrue(settings.isHighlightRDocSyntaxInComments());
        assertFalse(settings.isShowPhpClassAndNamespaceSeparators());
        assertFalse(settings.isAlwaysEnablePhpCodeBackgroundHighlighting());
        assertFalse(settings.isAlwaysEnableBladeTemplateHighlighting());
    }

    @Test
    void testPersistenceAndReload() {
        settings.setCaretBlinking(false);
        settings.setCaretBlinkingMs(650);
        settings.setUseBlockCaret(true);
        settings.setLineNumbersMode(LineNumbersMode.HYBRID);
        settings.setShowWhitespaces(true);
        settings.setWhitespaceLeading(false);
        settings.setTagTreeHighlightLevels(8);
        settings.setTagTreeHighlightOpacity(0.35);
        settings.setAlwaysEnableBladeTemplateHighlighting(true);

        settings.save();

        EditorAppearanceSettings loaded = new EditorAppearanceSettings();
        assertFalse(loaded.isCaretBlinking());
        assertEquals(650, loaded.getCaretBlinkingMs());
        assertTrue(loaded.isUseBlockCaret());
        assertEquals(LineNumbersMode.HYBRID, loaded.getLineNumbersMode());
        assertTrue(loaded.isShowWhitespaces());
        assertFalse(loaded.isWhitespaceLeading());
        assertEquals(8, loaded.getTagTreeHighlightLevels());
        assertEquals(0.35, loaded.getTagTreeHighlightOpacity(), 0.001);
        assertTrue(loaded.isAlwaysEnableBladeTemplateHighlighting());
    }

    @Test
    void testIsModifiedDetection() {
        EditorAppearanceSettings copy = settings.copy();
        assertFalse(settings.isModified(copy));

        copy.setCaretBlinkingMs(800);
        assertTrue(settings.isModified(copy));

        copy.copyFrom(settings);
        assertFalse(settings.isModified(copy));

        copy.setLineNumbersMode(LineNumbersMode.RELATIVE);
        assertTrue(settings.isModified(copy));
    }

    @Test
    void testListenerNotification() {
        AtomicBoolean notified = new AtomicBoolean(false);
        EditorAppearanceSettings.Listener listener = s -> notified.set(true);
        settings.addListener(listener);

        settings.setUseBlockCaret(true);
        settings.save();

        assertTrue(notified.get());
        settings.removeListener(listener);
    }
}
