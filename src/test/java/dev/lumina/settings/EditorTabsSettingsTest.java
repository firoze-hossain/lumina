package dev.lumina.settings;

import dev.lumina.settings.EditorTabsSettings.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EditorTabsSettingsTest {

    private EditorTabsSettings settings;

    @BeforeEach
    void setUp() {
        settings = new EditorTabsSettings();
        settings.initDefaults();
    }

    @Test
    void testDefaultsMatchIntelliJScreenshots() {
        assertEquals(TabPlacement.TOP, settings.getTabPlacement());
        assertEquals(TabRowsMode.ONE_ROW, settings.getTabRowsMode());
        assertEquals(OneRowFitPolicy.SCROLL, settings.getOneRowFitPolicy());
        assertFalse(settings.isShowPinnedTabsInSeparateRow());
        assertTrue(settings.isShowFileIcon());
        assertTrue(settings.isShowFileExtension());
        assertTrue(settings.isShowDirectoryForNonUniqueNames());
        assertFalse(settings.isMarkModified());
        assertTrue(settings.isShowFullPathOnMouseHover());
        assertEquals(CloseButtonPosition.RIGHT, settings.getCloseButtonPosition());

        assertFalse(settings.isSortTabsAlphabetically());
        assertFalse(settings.isOpenNewTabsAtEnd());

        assertFalse(settings.isEnablePreviewTab());

        assertEquals(30, settings.getTabLimit());
        assertEquals(TabsExceedLimitPolicy.CLOSE_UNUSED, settings.getTabsExceedLimitPolicy());
        assertEquals(TabCloseActivatePolicy.ACTIVATE_LEFT, settings.getTabCloseActivatePolicy());

        assertFalse(settings.isDbAlwaysShowQualifiedNames());
        assertTrue(settings.isDbShortenDatasourceNames());
    }

    @Test
    void testIsModified() {
        EditorTabsSettings copy = settings.copy();
        assertFalse(settings.isModified(copy));

        copy.setTabPlacement(TabPlacement.LEFT);
        assertTrue(settings.isModified(copy));

        copy.setTabPlacement(TabPlacement.TOP);
        assertFalse(settings.isModified(copy));

        copy.setMarkModified(true);
        assertTrue(settings.isModified(copy));

        copy.setMarkModified(false);
        assertFalse(settings.isModified(copy));

        copy.setTabLimit(50);
        assertTrue(settings.isModified(copy));

        copy.setTabLimit(30);
        assertFalse(settings.isModified(copy));

        copy.setTabsExceedLimitPolicy(TabsExceedLimitPolicy.CLOSE_UNCHANGED);
        assertTrue(settings.isModified(copy));

        copy.setTabsExceedLimitPolicy(TabsExceedLimitPolicy.CLOSE_UNUSED);
        assertFalse(settings.isModified(copy));
    }

    @Test
    void testMutationAndPersistence() {
        int orig = settings.getTabLimit();
        try {
            settings.setTabLimit(42);
            settings.save();

            EditorTabsSettings loaded = new EditorTabsSettings();
            assertEquals(42, loaded.getTabLimit());
        } finally {
            settings.setTabLimit(orig);
            settings.save();
        }
    }

    @Test
    void testListeners() {
        boolean[] called = {false};
        EditorTabsSettings.Listener l = s -> called[0] = true;
        settings.addListener(l);

        settings.setMarkModified(!settings.isMarkModified());
        settings.save();

        assertTrue(called[0]);
        settings.removeListener(l);
    }
}
