package dev.lumina.presentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class PresentationAssistantManagerTest {

    private PresentationAssistantManager manager;

    @BeforeEach
    void setUp() {
        manager = PresentationAssistantManager.getInstance();
        manager.revertToDefaults();
    }

    @Test
    void testDefaults() {
        assertFalse(manager.isShowActionNamesAndShortcuts());
        assertEquals("Medium", manager.getPopupSize());
        assertEquals(4, manager.getDisplayDurationSeconds());
        assertEquals("Bottom Center", manager.getPosition());
        assertEquals("macOS", manager.getMainKeymap());
        assertEquals("macOS", manager.getMainLabel());
        assertFalse(manager.isAdditionalKeymapEnabled());
        assertEquals("Windows", manager.getAdditionalKeymap());
        assertEquals("Win/Linux", manager.getAdditionalLabel());
    }

    @Test
    void testBrandIsolationInKeymaps() {
        // Strict requirement: zero occurrences of "JetBrains" or "IntelliJ" across available keymaps
        for (String keymap : PresentationAssistantManager.AVAILABLE_KEYMAPS) {
            assertFalse(keymap.toLowerCase().contains("intellij"), "Keymap catalog contains 'intellij': " + keymap);
            assertFalse(keymap.toLowerCase().contains("jetbrains"), "Keymap catalog contains 'jetbrains': " + keymap);
        }
        assertTrue(PresentationAssistantManager.AVAILABLE_KEYMAPS.contains("Default Classic"));
        assertTrue(PresentationAssistantManager.AVAILABLE_KEYMAPS.contains("macOS"));
        assertTrue(PresentationAssistantManager.AVAILABLE_KEYMAPS.contains("Windows"));
    }

    @Test
    void testDisplayDurationConstraints() {
        manager.setDisplayDurationSeconds(8);
        assertEquals(8, manager.getDisplayDurationSeconds());

        // Negative or zero duration clamped to minimum of 1
        manager.setDisplayDurationSeconds(0);
        assertEquals(1, manager.getDisplayDurationSeconds());

        manager.setDisplayDurationSeconds(-5);
        assertEquals(1, manager.getDisplayDurationSeconds());
    }

    @Test
    void testPopupSizeOptions() {
        manager.setPopupSize("Small");
        assertEquals("Small", manager.getPopupSize());

        manager.setPopupSize("Large");
        assertEquals("Large", manager.getPopupSize());

        // Invalid size ignored
        manager.setPopupSize("Gigantic");
        assertEquals("Large", manager.getPopupSize());
    }

    @Test
    void testPositionOptions() {
        manager.setPosition("Top Right");
        assertEquals("Top Right", manager.getPosition());

        manager.setPosition("Bottom Left");
        assertEquals("Bottom Left", manager.getPosition());

        // Invalid position ignored
        manager.setPosition("Floating Center");
        assertEquals("Bottom Left", manager.getPosition());
    }

    @Test
    void testKeymapConfiguration() {
        manager.setMainKeymap("Default Classic");
        manager.setMainLabel("Lumina Default");
        assertEquals("Default Classic", manager.getMainKeymap());
        assertEquals("Lumina Default", manager.getMainLabel());

        manager.setAdditionalKeymapEnabled(true);
        manager.setAdditionalKeymap("Emacs");
        manager.setAdditionalLabel("Emacs Mode");
        assertTrue(manager.isAdditionalKeymapEnabled());
        assertEquals("Emacs", manager.getAdditionalKeymap());
        assertEquals("Emacs Mode", manager.getAdditionalLabel());
    }

    @Test
    void testListenerNotification() {
        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addListener(listener);

        notified.set(false);
        manager.setShowActionNamesAndShortcuts(true);
        assertTrue(notified.get());

        notified.set(false);
        manager.setPopupSize("Large");
        assertTrue(notified.get());

        notified.set(false);
        manager.setDisplayDurationSeconds(10);
        assertTrue(notified.get());

        manager.removeListener(listener);
    }
}
