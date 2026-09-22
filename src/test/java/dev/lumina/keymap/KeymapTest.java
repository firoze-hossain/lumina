package dev.lumina.keymap;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KeymapTest {

    @Test
    void testBasicProperties() {
        Keymap k = new Keymap("macOS", "macOS", null, false);
        assertEquals("macOS", k.getId());
        assertEquals("macOS", k.getName());
        assertNull(k.getParentKeymapId());
        assertFalse(k.isMutable());

        k.setName("macOS Updated");
        assertEquals("macOS Updated", k.getName());
    }

    @Test
    void testAddAndRemoveShortcuts() {
        Keymap k = new Keymap("custom", "Custom Keymap", "macOS", true);
        assertTrue(k.isMutable());

        KeyboardShortcut sc1 = new KeyboardShortcut(KeyCode.A, true, false, false, false);
        KeyboardShortcut sc2 = new KeyboardShortcut(KeyCode.B, true, false, false, false);

        k.addShortcut("TestAct", sc1);
        k.addShortcut("TestAct", sc2);

        assertTrue(k.hasOverride("TestAct"));
        List<KeyboardShortcut> list = k.getDirectShortcuts("TestAct");
        assertEquals(2, list.size());
        assertTrue(list.contains(sc1));
        assertTrue(list.contains(sc2));

        // Duplicate addition ignored
        k.addShortcut("TestAct", sc1);
        assertEquals(2, k.getDirectShortcuts("TestAct").size());

        // Remove shortcut
        k.removeShortcut("TestAct", sc1);
        assertEquals(1, k.getDirectShortcuts("TestAct").size());
        assertFalse(k.getDirectShortcuts("TestAct").contains(sc1));

        // Clear shortcuts (explicitly empty override)
        k.clearShortcuts("TestAct");
        assertTrue(k.hasOverride("TestAct"));
        assertTrue(k.getDirectShortcuts("TestAct").isEmpty());

        // Reset action (removes override completely)
        k.resetAction("TestAct");
        assertFalse(k.hasOverride("TestAct"));
    }

    @Test
    void testCloneAs() {
        Keymap parent = new Keymap("parent", "Parent Keymap", null, false);
        KeyboardShortcut sc = new KeyboardShortcut(KeyCode.S, false, false, false, true);
        parent.addShortcut("Save", sc);

        Keymap clone = parent.cloneAs("clone_1", "Clone Keymap");
        assertEquals("clone_1", clone.getId());
        assertEquals("Clone Keymap", clone.getName());
        assertEquals("parent", clone.getParentKeymapId());
        assertTrue(clone.isMutable());
        assertEquals(List.of(sc), clone.getDirectShortcuts("Save"));
    }

    @Test
    void testSerializationAndDeserialization() {
        Keymap k = new Keymap("test", "Test", null, true);
        KeyboardShortcut sc1 = new KeyboardShortcut(KeyCode.K, true, false, false, false);
        KeyboardShortcut sc2 = new KeyboardShortcut(KeyCode.C, true, false, false, false);
        k.addShortcut("Action1", sc1);
        k.addShortcut("Action1", sc2);

        String serialized = k.serializeShortcuts();
        assertNotNull(serialized);
        assertTrue(serialized.contains("Action1="));

        Keymap target = new Keymap("target", "Target", null, true);
        target.deserializeShortcuts(serialized);
        List<KeyboardShortcut> restored = target.getDirectShortcuts("Action1");
        assertEquals(2, restored.size());
        assertEquals(sc1, restored.get(0));
        assertEquals(sc2, restored.get(1));
    }
}
