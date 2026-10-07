package dev.lumina.settings;

import dev.lumina.settings.EmmetSettings.CssPrefixEntry;
import dev.lumina.util.Settings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EmmetSettingsTest {

    private EmmetSettings settings;

    @BeforeEach
    void setUp() {
        Settings.clear();
        settings = EmmetSettings.getInstance();
        settings.resetToDefaults();
    }

    @AfterEach
    void tearDown() {
        Settings.clear();
    }

    @Test
    void testDefaultsMatchIntelliJIdea() {
        assertTrue(settings.isEnableEmmet());
        assertEquals("Tab", settings.getExpandAbbreviationWith());

        assertTrue(settings.isEnableCssEmmet());
        assertFalse(settings.isEnableFuzzySearch());
        assertFalse(settings.isEnableUnknownProperties());
        assertTrue(settings.isAutoInsertVendorPrefixes());

        List<CssPrefixEntry> entries = settings.getCssProperties();
        assertNotNull(entries);
        assertFalse(entries.isEmpty());

        // Check CSS properties from screenshots 1, 2, 3
        CssPrefixEntry anim = settings.getCssEntry("animation");
        assertNotNull(anim);
        assertTrue(anim.isWebkit());
        assertTrue(anim.isMoz());
        assertFalse(anim.isMs());
        assertTrue(anim.isO());
        assertFalse(anim.isKhtml());

        CssPrefixEntry borderRad = settings.getCssEntry("border-radius");
        assertNotNull(borderRad);
        assertTrue(borderRad.isWebkit());
        assertTrue(borderRad.isMoz());

        // From Image 1
        CssPrefixEntry maskClip = settings.getCssEntry("mask-clip");
        assertNotNull(maskClip);
        assertTrue(maskClip.isWebkit());
        assertFalse(maskClip.isMoz());

        // From Image 2
        CssPrefixEntry tabSize = settings.getCssEntry("tab-size");
        assertNotNull(tabSize);
        assertTrue(tabSize.isMoz());
        assertTrue(tabSize.isO());

        // From Image 3
        CssPrefixEntry transform = settings.getCssEntry("transform");
        assertNotNull(transform);
        assertTrue(transform.isWebkit());
        assertTrue(transform.isMoz());
        assertTrue(transform.isMs());
        assertTrue(transform.isO());
        assertFalse(transform.isKhtml());

        CssPrefixEntry writingMode = settings.getCssEntry("writing-mode");
        assertNotNull(writingMode);
        assertTrue(writingMode.isWebkit());
        assertTrue(writingMode.isMs());
    }

    @Test
    void testCopyAndIsModified() {
        EmmetSettings copy = settings.copy();
        assertFalse(copy.isModified(settings));

        copy.setExpandAbbreviationWith("Space");
        assertTrue(copy.isModified(settings));

        settings.applyFrom(copy);
        assertEquals("Space", settings.getExpandAbbreviationWith());
        assertFalse(copy.isModified(settings));
    }

    @Test
    void testPrefixEntryModification() {
        EmmetSettings copy = settings.copy();
        CssPrefixEntry box = copy.getCssEntry("box-shadow");
        assertNotNull(box);
        box.setMs(true);
        assertTrue(copy.isModified(settings));

        settings.applyFrom(copy);
        assertTrue(settings.getCssEntry("box-shadow").isMs());
    }

    @Test
    void testPersistence() {
        settings.setExpandAbbreviationWith("Enter");
        settings.setAutoInsertVendorPrefixes(false);
        CssPrefixEntry anim = settings.getCssEntry("animation");
        anim.setKhtml(true);
        settings.save();

        EmmetSettings reloaded = new EmmetSettings();
        reloaded.load();

        assertEquals("Enter", reloaded.getExpandAbbreviationWith());
        assertFalse(reloaded.isAutoInsertVendorPrefixes());
        CssPrefixEntry reloadedAnim = reloaded.getCssEntry("animation");
        assertNotNull(reloadedAnim);
        assertTrue(reloadedAnim.isKhtml());
    }

    @Test
    void testHtmlAndJsxSettings() {
        // Image 4 defaults
        assertTrue(settings.isEnableXmlHtmlEmmet());
        assertFalse(settings.isEnableAbbreviationPreview());
        assertTrue(settings.isEnableAutoUrlRecognition());
        assertFalse(settings.isAddEditPointAtEndOfTemplate());

        assertEquals("__", settings.getBemElementSeparator());
        assertEquals("_", settings.getBemModifierSeparator());
        assertEquals("-", settings.getBemShortElementPrefix());

        assertFalse(settings.isFilterXslTuning());
        assertFalse(settings.isFilterCommentTags());
        assertFalse(settings.isFilterEscape());
        assertFalse(settings.isFilterSingleLine());
        assertFalse(settings.isFilterBem());
        assertFalse(settings.isFilterTrimLineMarkers());

        // Image 5 defaults
        assertTrue(settings.isEnableJsxEmmet());

        // Modify and test persistence
        settings.setBemElementSeparator("---");
        settings.setFilterBem(true);
        settings.setEnableJsxEmmet(false);
        settings.save();

        EmmetSettings reloaded = new EmmetSettings();
        reloaded.load();

        assertEquals("---", reloaded.getBemElementSeparator());
        assertTrue(reloaded.isFilterBem());
        assertFalse(reloaded.isEnableJsxEmmet());
    }
}
