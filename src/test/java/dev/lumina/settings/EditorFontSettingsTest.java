package dev.lumina.settings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class EditorFontSettingsTest {

    private EditorFontSettings settings;

    @BeforeEach
    void setUp() {
        settings = EditorFontSettings.getInstance();
        settings.initDefaults();
    }

    @Test
    void testDefaultsMatchReferenceScreenshots() {
        assertEquals("JetBrains Mono", settings.getFontFamily());
        assertEquals(13.0, settings.getFontSize(), 0.001);
        assertEquals(1.2, settings.getLineHeight(), 0.001);
        assertFalse(settings.isEnableLigatures());
        assertEquals("Regular", settings.getMainWeight());
        assertEquals("Bold Recommended", settings.getBoldWeight());
        assertEquals("<None>", settings.getFallbackFont());
    }

    @Test
    void testIsModified() {
        EditorFontSettings copy = settings.copy();
        assertFalse(settings.isModified(copy));

        copy.setFontSize(15.0);
        assertTrue(settings.isModified(copy));
        copy.setFontSize(13.0);
        assertFalse(settings.isModified(copy));

        copy.setEnableLigatures(true);
        assertTrue(settings.isModified(copy));
        copy.setEnableLigatures(false);
        assertFalse(settings.isModified(copy));

        copy.setMainWeight("Medium");
        assertTrue(settings.isModified(copy));
        copy.setMainWeight("Regular");
        assertFalse(settings.isModified(copy));
    }

    @Test
    void testSaveAndLoad() {
        settings.setFontFamily("Consolas");
        settings.setFontSize(14.5);
        settings.setLineHeight(1.4);
        settings.setEnableLigatures(true);
        settings.setMainWeight("Medium");
        settings.setBoldWeight("Bold");
        settings.setFallbackFont("Monospace");
        settings.save();

        EditorFontSettings loaded = new EditorFontSettings();
        loaded.load();

        assertEquals("Consolas", loaded.getFontFamily());
        assertEquals(14.5, loaded.getFontSize(), 0.001);
        assertEquals(1.4, loaded.getLineHeight(), 0.001);
        assertTrue(loaded.isEnableLigatures());
        assertEquals("Medium", loaded.getMainWeight());
        assertEquals("Bold", loaded.getBoldWeight());
        assertEquals("Monospace", loaded.getFallbackFont());
    }

    @Test
    void testListeners() {
        AtomicBoolean notified = new AtomicBoolean(false);
        EditorFontSettings.Listener listener = s -> notified.set(true);
        settings.addListener(listener);

        settings.save();
        assertTrue(notified.get());

        notified.set(false);
        settings.removeListener(listener);
        settings.save();
        assertFalse(notified.get());
    }
}
