package dev.lumina.settings;

import dev.lumina.settings.InlineCompletionSettings.DownloadModelsMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InlineCompletionSettingsTest {

    private InlineCompletionSettings settings;

    @BeforeEach
    void setUp() {
        settings = new InlineCompletionSettings();
        settings.initDefaults();
    }

    @Test
    void testDefaultsMatchScreenshots() {
        assertTrue(settings.isEnableLocalFullLine());
        assertEquals(DownloadModelsMode.ASK_BEFORE_DOWNLOADING, settings.getDownloadModelsMode());

        assertTrue(settings.isLanguageEnabled("Kotlin"));
        assertTrue(settings.isLanguageEnabled("Java"));
        assertFalse(settings.isLanguageEnabled("CSS-like"));
        assertFalse(settings.isLanguageEnabled("HTML"));
        assertFalse(settings.isLanguageEnabled("JavaScript / TypeScript"));
        assertFalse(settings.isLanguageEnabled("Python"));
        assertFalse(settings.isLanguageEnabled("PHP"));
        assertFalse(settings.isLanguageEnabled("Rust"));
        assertFalse(settings.isLanguageEnabled("Scala"));
        assertFalse(settings.isLanguageEnabled("Go"));
        assertFalse(settings.isLanguageEnabled("Ruby"));

        assertTrue(settings.isLanguageDownloaded("Kotlin"));
        assertTrue(settings.isLanguageDownloaded("Java"));
        assertFalse(settings.isLanguageDownloaded("CSS-like"));
        assertFalse(settings.isLanguageDownloaded("Rust"));

        assertFalse(settings.isEnableCloudCompletion());
        assertFalse(settings.isCloudActivated());

        assertTrue(settings.isEnableAutomaticCompletionOnTyping());
        assertTrue(settings.isEnableMultilineSuggestions());
        assertFalse(settings.isSynchronizeInlineAndPopup());
    }

    @Test
    void testIsModified() {
        InlineCompletionSettings copy = settings.copy();
        assertFalse(settings.isModified(copy));

        copy.setEnableLocalFullLine(false);
        assertTrue(settings.isModified(copy));
        copy.setEnableLocalFullLine(true);
        assertFalse(settings.isModified(copy));

        copy.setDownloadModelsMode(DownloadModelsMode.AUTOMATICALLY);
        assertTrue(settings.isModified(copy));
        copy.setDownloadModelsMode(DownloadModelsMode.ASK_BEFORE_DOWNLOADING);
        assertFalse(settings.isModified(copy));

        copy.setLanguageEnabled("Rust", true);
        assertTrue(settings.isModified(copy));
        copy.setLanguageEnabled("Rust", false);
        assertFalse(settings.isModified(copy));

        copy.setLanguageDownloaded("Rust", true);
        assertTrue(settings.isModified(copy));
        copy.setLanguageDownloaded("Rust", false);
        assertFalse(settings.isModified(copy));

        copy.setEnableAutomaticCompletionOnTyping(false);
        assertTrue(settings.isModified(copy));
        copy.setEnableAutomaticCompletionOnTyping(true);
        assertFalse(settings.isModified(copy));

        copy.setEnableMultilineSuggestions(false);
        assertTrue(settings.isModified(copy));
        copy.setEnableMultilineSuggestions(true);
        assertFalse(settings.isModified(copy));

        copy.setSynchronizeInlineAndPopup(true);
        assertTrue(settings.isModified(copy));
        copy.setSynchronizeInlineAndPopup(false);
        assertFalse(settings.isModified(copy));
    }

    @Test
    void testSaveAndLoad() {
        settings.setEnableLocalFullLine(false);
        settings.setDownloadModelsMode(DownloadModelsMode.MANUALLY);
        settings.setLanguageDownloaded("Rust", true);
        settings.setLanguageEnabled("Rust", true);
        settings.setEnableCloudCompletion(true);
        settings.setCloudActivated(true);
        settings.setSynchronizeInlineAndPopup(true);
        settings.save();

        InlineCompletionSettings loaded = new InlineCompletionSettings();
        loaded.load();

        assertFalse(loaded.isEnableLocalFullLine());
        assertEquals(DownloadModelsMode.MANUALLY, loaded.getDownloadModelsMode());
        assertTrue(loaded.isLanguageDownloaded("Rust"));
        assertTrue(loaded.isLanguageEnabled("Rust"));
        assertTrue(loaded.isEnableCloudCompletion());
        assertTrue(loaded.isCloudActivated());
        assertTrue(loaded.isSynchronizeInlineAndPopup());

        // Restore defaults
        settings.initDefaults();
        settings.save();
    }

    @Test
    void testListener() {
        boolean[] called = {false};
        InlineCompletionSettings.Listener listener = s -> called[0] = true;
        settings.addListener(listener);

        settings.setEnableMultilineSuggestions(false);
        settings.save();

        assertTrue(called[0]);
        settings.removeListener(listener);
    }
}
