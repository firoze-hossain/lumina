package dev.lumina.settings;

import dev.lumina.settings.EditorColorSchemeSettings.ColorAttribute;
import dev.lumina.settings.EditorColorSchemeSettings.EffectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class EditorColorSchemeSettingsTest {

    private EditorColorSchemeSettings settings;

    @BeforeEach
    void setUp() {
        settings = EditorColorSchemeSettings.getInstance();
        settings.initDefaults();
    }

    @Test
    void testDefaultSchemeAndBundledSchemes() {
        assertEquals("Islands Dark Theme default", settings.getActiveSchemeName());
        List<String> bundled = EditorColorSchemeSettings.BUNDLED_SCHEMES;
        assertEquals(7, bundled.size());
        assertTrue(bundled.contains("Islands Dark Theme default"));
        assertTrue(bundled.contains("Darcula"));
        assertTrue(bundled.contains("Light"));
        assertTrue(bundled.contains("High Contrast"));
        assertTrue(bundled.contains("Classic Light"));
        assertTrue(bundled.contains("Darcula Contrast"));
        assertTrue(bundled.contains("Dark"));
    }

    @Test
    void testDuplicateScheme() {
        boolean duplicated = settings.duplicateScheme("Islands Dark Theme default", "My Custom Dark");
        assertTrue(duplicated);
        assertEquals("My Custom Dark", settings.getActiveSchemeName());
        assertTrue(settings.isCustomScheme("My Custom Dark"));
        assertTrue(settings.getAvailableSchemes().contains("My Custom Dark"));

        // Duplicate with same name should fail
        assertFalse(settings.duplicateScheme("Islands Dark Theme default", "My Custom Dark"));
    }

    @Test
    void testRestoreDefaults() {
        String active = settings.getActiveSchemeName();
        ColorAttribute attr = new ColorAttribute("#FF0000", "#00FF00", true, true);
        settings.setAttribute(active, "Text // Default text", attr);

        assertTrue(settings.isSchemeModified(active));
        assertEquals("#FF0000", settings.getAttribute(active, "Text // Default text").getForeground());

        settings.restoreDefaults(active);
        assertFalse(settings.isSchemeModified(active));
        assertEquals("#DFE1E5", settings.getAttribute(active, "Text // Default text").getForeground());
    }

    @Test
    void testDirtyTrackingAndCopyApply() {
        EditorColorSchemeSettings copy = settings.copy();
        assertFalse(settings.isModified(copy));

        settings.setActiveSchemeName("Darcula");
        assertTrue(settings.isModified(copy));

        settings.applyFrom(copy);
        assertFalse(settings.isModified(copy));
        assertEquals("Islands Dark Theme default", settings.getActiveSchemeName());
    }

    @Test
    void testListenerNotification() {
        AtomicBoolean notified = new AtomicBoolean(false);
        EditorColorSchemeSettings.Listener listener = s -> notified.set(true);
        settings.addListener(listener);

        settings.setActiveSchemeName("High Contrast");
        assertTrue(notified.get());

        notified.set(false);
        settings.removeListener(listener);
        settings.setActiveSchemeName("Dark");
        assertFalse(notified.get());
    }
}
