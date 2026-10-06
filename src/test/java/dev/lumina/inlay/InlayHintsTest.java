package dev.lumina.inlay;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InlayHintsTest {

    private InlayHintsManager manager;

    @BeforeEach
    void setUp() {
        manager = InlayHintsManager.getInstance();
        manager.resetToDefaults();
    }

    @Test
    void testDefaultSettings() {
        InlayHintsSettings settings = new InlayHintsSettings();
        assertEquals("Right", settings.getCodeVisionDefaultPosition());
        assertEquals(5, settings.getCodeVisionMaxAbove());
        assertEquals(5, settings.getCodeVisionMaxNext());

        // Check defaults matching IntelliJ screenshots
        assertTrue(settings.isHintEnabled("codevision", true));
        assertTrue(settings.isHintEnabled("codevision.inheritors", true));
        assertFalse(settings.isHintEnabled("param.java.reflected", true));
        assertTrue(settings.isHintEnabled("param.java.enum_constant", false));
        assertFalse(settings.isHintEnabled("types.kotlin", true));
        assertTrue(settings.isHintEnabled("types.javascript.variables_and_fields", false));
    }

    @Test
    void testCopyAndEquivalence() {
        InlayHintsSettings s1 = new InlayHintsSettings();
        InlayHintsSettings s2 = s1.copy();
        assertTrue(s1.isEquivalentTo(s2));

        s2.setCodeVisionDefaultPosition("Top");
        assertFalse(s1.isEquivalentTo(s2));

        s2.setCodeVisionDefaultPosition("Right");
        assertTrue(s1.isEquivalentTo(s2));

        s2.setItemPosition("codevision.inheritors", "Top");
        assertFalse(s1.isEquivalentTo(s2));

        s2.setItemPosition("codevision.inheritors", "Default");
        s1.setItemPosition("codevision.inheritors", "Default");
        assertTrue(s1.isEquivalentTo(s2));

        s2.setHintEnabled("types.kotlin", true);
        assertFalse(s1.isEquivalentTo(s2));
    }

    @Test
    void testManagerSaveAndLoad() {
        InlayHintsSettings settings = manager.getSettings();
        settings.setCodeVisionDefaultPosition("Top");
        settings.setCodeVisionMaxAbove(10);
        settings.setCodeVisionMaxNext(12);
        settings.setItemPosition("codevision.usages", "Right");
        settings.setHintEnabled("param.java.reflected", true);

        manager.save();

        manager.resetToDefaults();
        assertEquals("Right", manager.getSettings().getCodeVisionDefaultPosition());
        assertEquals(5, manager.getSettings().getCodeVisionMaxAbove());

        manager.load();
        assertEquals("Top", manager.getSettings().getCodeVisionDefaultPosition());
        assertEquals(10, manager.getSettings().getCodeVisionMaxAbove());
        assertEquals(12, manager.getSettings().getCodeVisionMaxNext());
        assertEquals("Right", manager.getSettings().getItemPosition("codevision.usages", "Default"));
        assertTrue(manager.getSettings().isHintEnabled("param.java.reflected", false));
    }
}
