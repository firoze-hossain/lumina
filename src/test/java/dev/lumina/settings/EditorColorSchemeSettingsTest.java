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

    @Test
    void testInheritanceResolution() {
        String active = settings.getActiveSchemeName();

        // Sticky Lines // Border inherits from Guides // Hard wrap guide
        ColorAttribute borderAttr = settings.resolveAttribute(active, "Editor // Sticky Lines // Border");
        assertNotNull(borderAttr);
        assertEquals("#323438", borderAttr.getBackground());

        // Sticky Lines // Hovered inherits from Caret row (#1F2024)
        ColorAttribute hoveredAttr = settings.resolveAttribute(active, "Editor // Sticky Lines // Hovered");
        assertNotNull(hoveredAttr);
        assertEquals("#1F2024", hoveredAttr.getBackground());

        // Modify Caret row and verify Hovered dynamically reflects change
        ColorAttribute newCaretRow = new ColorAttribute(null, "#252830", false, false);
        settings.setAttribute(active, "Editor // Caret row", newCaretRow);

        ColorAttribute updatedHovered = settings.resolveAttribute(active, "Editor // Sticky Lines // Hovered");
        assertEquals("#252830", updatedHovered.getBackground());

        // Override Sticky Lines // Hovered explicitly (uncheck inherit)
        ColorAttribute explicitHovered = new ColorAttribute(null, "#333333", false, false);
        explicitHovered.setInherit(false);
        settings.setAttribute(active, "Editor // Sticky Lines // Hovered", explicitHovered);

        ColorAttribute resolvedExplicit = settings.resolveAttribute(active, "Editor // Sticky Lines // Hovered");
        assertEquals("#333333", resolvedExplicit.getBackground());
    }

    @Test
    void testIntelliJDescriptorsAndResolutions() {
        String active = settings.getActiveSchemeName();

        // Identifiers // Reassigned local variable (media_1790381886079.png)
        ColorAttribute reassigned = settings.resolveAttribute(active, "Identifiers // Reassigned local variable");
        assertNotNull(reassigned);
        assertEquals("#BCBEC4", reassigned.getForeground());
        assertEquals("#84868C", reassigned.getEffectColor());
        assertEquals(EffectType.UNDERSCORED, reassigned.getEffectType());

        // Line Coverage // Full (media_1790381912220.png)
        ColorAttribute lineCoverage = settings.resolveAttribute(active, "Line Coverage // Full");
        assertNotNull(lineCoverage);
        assertEquals("#375239", lineCoverage.getForeground());
        assertTrue(lineCoverage.isBold());

        // Live Templates // Template Variable (media_1790381939807.png)
        ColorAttribute templateVar = settings.resolveAttribute(active, "Live Templates // Template Variable");
        assertNotNull(templateVar);
        assertEquals("#B189F5", templateVar.getForeground());

        // Popups and Hints // Promotion pane (media_1790381958006.png)
        ColorAttribute promotion = settings.resolveAttribute(active, "Popups and Hints // Promotion pane");
        assertNotNull(promotion);
        assertEquals("#25324D", promotion.getBackground());

        // Preview // Border inherits from Editor // Guides // Indent guide (media_1790381971087.png)
        ColorAttribute previewBorder = settings.resolveAttribute(active, "Preview // Border");
        assertNotNull(previewBorder);
        assertEquals("#323438", previewBorder.getBackground());
    }
}
