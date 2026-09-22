package dev.lumina.git;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class VcsColorManagerTest {

    private VcsColorManager manager;

    @BeforeEach
    void setUp() {
        manager = VcsColorManager.getInstance();
        manager.revertAllToDefaults();
    }

    @Test
    void testAll25StatusDefinitionsPresent() {
        assertEquals(25, manager.getAllStatusDefinitions().size());
        assertEquals("73BD79", manager.getColorHex("Added"));
        assertEquals("56A8F5", manager.getColorHex("Modified"));
        assertEquals("787878", manager.getColorHex("Deleted"));
        assertEquals("8C7C54", manager.getColorHex("Ignored"));
        assertEquals("D5756C", manager.getColorHex("Changelist conflict"));
        assertEquals("D5756C", manager.getColorHex("Unknown"));
        assertEquals("DFE1E5", manager.getColorHex("Up to date"));
    }

    @Test
    void testColorCustomizationAndRestore() {
        assertFalse(manager.isCustomized("Added"));

        // Customize Added color
        manager.setColor("Added", "00FF00");
        assertTrue(manager.isCustomized("Added"));
        assertEquals("00FF00", manager.getColorHex("Added"));

        // GitFileStatus reflects the custom color
        assertEquals("#00FF00", GitFileStatus.ADDED.getColorHex());

        // Restore default
        manager.restoreDefault("Added");
        assertFalse(manager.isCustomized("Added"));
        assertEquals("73BD79", manager.getColorHex("Added"));
        assertEquals("#59A869", GitFileStatus.ADDED.getColorHex());
    }

    @Test
    void testListenerNotification() {
        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addListener(listener);

        manager.setColor("Modified", "112233");
        assertTrue(notified.get());

        notified.set(false);
        manager.restoreDefault("Modified");
        assertTrue(notified.get());

        manager.removeListener(listener);
    }
}
