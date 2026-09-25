package dev.lumina.settings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class StickyLinesSettingsTest {

    private StickyLinesSettings settings;

    @BeforeEach
    void setUp() {
        settings = new StickyLinesSettings();
        settings.initDefaults();
    }

    @Test
    void testDefaultsMatchScreenshot() {
        assertTrue(settings.isShowStickyLines());
        assertEquals(5, settings.getMaxLines());

        // Check language counts
        assertEquals(29, StickyLinesSettings.ALL_LANGUAGES.size());
        assertEquals(10, StickyLinesSettings.COLUMN_1_LANGUAGES.size());
        assertEquals(10, StickyLinesSettings.COLUMN_2_LANGUAGES.size());
        assertEquals(9, StickyLinesSettings.COLUMN_3_LANGUAGES.size());

        // All 29 languages enabled by default
        for (String lang : StickyLinesSettings.ALL_LANGUAGES) {
            assertTrue(settings.isLanguageEnabled(lang), "Expected language enabled: " + lang);
        }
    }

    @Test
    void testSpecificLanguagesInColumns() {
        List<String> col1 = StickyLinesSettings.COLUMN_1_LANGUAGES;
        assertEquals(List.of("CSS", "ERB", "FreeMarker", "Go", "Groovy", "HTML", "Java", "JavaScript", "JSON", "JSP"), col1);

        List<String> col2 = StickyLinesSettings.COLUMN_2_LANGUAGES;
        assertEquals(List.of("JSPX", "Jupyter", "Kotlin", "Less", "Markdown", "PHP", "protobuf", "Python", "Ruby", "Rust"), col2);

        List<String> col3 = StickyLinesSettings.COLUMN_3_LANGUAGES;
        assertEquals(List.of("Sass", "Scala", "SCSS", "SQL", "TypeScript", "VTL", "XHTML", "XML", "YAML"), col3);
    }

    @Test
    void testIsModified() {
        StickyLinesSettings copy = settings.copy();
        assertFalse(settings.isModified(copy));

        copy.setShowStickyLines(false);
        assertTrue(settings.isModified(copy));
        copy.setShowStickyLines(true);
        assertFalse(settings.isModified(copy));

        copy.setMaxLines(10);
        assertTrue(settings.isModified(copy));
        copy.setMaxLines(5);
        assertFalse(settings.isModified(copy));

        copy.setLanguageEnabled("Java", false);
        assertTrue(settings.isModified(copy));
        copy.setLanguageEnabled("Java", true);
        assertFalse(settings.isModified(copy));
    }

    @Test
    void testSaveAndLoad() {
        settings.setShowStickyLines(false);
        settings.setMaxLines(8);
        settings.setEnabledLanguages(Set.of("Java", "Kotlin", "Rust"));
        settings.save();

        StickyLinesSettings loaded = new StickyLinesSettings();
        loaded.load();

        assertFalse(loaded.isShowStickyLines());
        assertEquals(8, loaded.getMaxLines());
        assertTrue(loaded.isLanguageEnabled("Java"));
        assertTrue(loaded.isLanguageEnabled("Kotlin"));
        assertTrue(loaded.isLanguageEnabled("Rust"));
        assertFalse(loaded.isLanguageEnabled("CSS"));

        // Restore defaults
        settings.initDefaults();
        settings.save();
    }

    @Test
    void testListenerNotification() {
        AtomicBoolean notified = new AtomicBoolean(false);
        StickyLinesSettings.Listener l = s -> notified.set(true);

        settings.addListener(l);
        settings.save();
        assertTrue(notified.get());

        notified.set(false);
        settings.removeListener(l);
        settings.save();
        assertFalse(notified.get());
    }
}
