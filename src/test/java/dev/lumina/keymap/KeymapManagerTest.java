package dev.lumina.keymap;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class KeymapManagerTest {

    private KeymapManager manager;

    @BeforeEach
    void setUp() {
        manager = KeymapManager.getInstance();
        manager.setActiveKeymapByName("macOS");
    }

    @Test
    void testBrandIsolation() {
        // Strict requirement: zero occurrences of "JetBrains" or "IntelliJ" across all keymaps and actions
        for (String name : KeymapManager.BUILT_IN_KEYMAP_NAMES) {
            assertFalse(name.toLowerCase().contains("intellij"), "Keymap name contains 'intellij': " + name);
            assertFalse(name.toLowerCase().contains("jetbrains"), "Keymap name contains 'jetbrains': " + name);
        }

        for (Keymap k : manager.getKeymaps()) {
            assertFalse(k.getName().toLowerCase().contains("intellij"), "Keymap contains 'intellij': " + k.getName());
            assertFalse(k.getName().toLowerCase().contains("jetbrains"), "Keymap contains 'jetbrains': " + k.getName());
        }

        for (KeymapAction a : manager.getAllActions()) {
            assertFalse(a.getId().toLowerCase().contains("intellij"), "Action ID contains 'intellij': " + a.getId());
            assertFalse(a.getName().toLowerCase().contains("intellij"), "Action Name contains 'intellij': " + a.getName());
            assertFalse(a.getId().toLowerCase().contains("jetbrains"), "Action ID contains 'jetbrains': " + a.getId());
            assertFalse(a.getName().toLowerCase().contains("jetbrains"), "Action Name contains 'jetbrains': " + a.getName());
        }

        // Verify Default Classic exists in place of IntelliJ IDEA Classic
        assertTrue(KeymapManager.BUILT_IN_KEYMAP_NAMES.contains("Default Classic"));
        assertNotNull(manager.getKeymapByName("Default Classic"));
    }

    @Test
    void testBuiltInKeymapsCatalog() {
        List<Keymap> list = manager.getKeymaps();
        assertNotNull(list);
        assertFalse(list.isEmpty());

        assertNotNull(manager.getKeymapByName("macOS"));
        assertNotNull(manager.getKeymapByName("Eclipse"));
        assertNotNull(manager.getKeymapByName("Emacs"));
        assertNotNull(manager.getKeymapByName("NetBeans"));
        assertNotNull(manager.getKeymapByName("Sublime Text"));
        assertNotNull(manager.getKeymapByName("Visual Studio"));
    }

    @Test
    void testActiveKeymapAndShortcuts() {
        Keymap mac = manager.getKeymapByName("macOS");
        assertNotNull(mac);
        manager.setActiveKeymap(mac);
        assertEquals("macOS", manager.getActiveKeymap().getName());

        // Find Action... in macOS is ⇧⌘A
        List<KeyboardShortcut> findActionShortcuts = manager.getActiveShortcuts("FindActions");
        assertFalse(findActionShortcuts.isEmpty());
        KeyboardShortcut sc = findActionShortcuts.get(0);
        assertEquals(KeyCode.A, sc.getKeyCode());
        assertTrue(sc.isShift());
        assertTrue(sc.isMeta());
        assertEquals("⇧⌘A", sc.formatGlyphs());

        // Basic Completion is ^Space
        List<KeyboardShortcut> basicShortcuts = manager.getActiveShortcuts("BasicCompletion");
        assertFalse(basicShortcuts.isEmpty());
        assertEquals("^Space", basicShortcuts.get(0).formatGlyphs());
    }

    @Test
    void testDuplicateRenameDeleteKeymap() {
        Keymap source = manager.getKeymapByName("macOS");
        Keymap custom = manager.duplicateKeymap(source, "My Custom Keymap");
        assertNotNull(custom);
        assertTrue(custom.isMutable());
        assertEquals("My Custom Keymap", custom.getName());
        assertEquals(custom, manager.getActiveKeymap());

        // Rename
        manager.renameKeymap(custom, "My Renamed Keymap");
        assertEquals("My Renamed Keymap", custom.getName());

        // Delete
        boolean deleted = manager.deleteKeymap(custom);
        assertTrue(deleted);
        assertNull(manager.getKeymap(custom.getId()));
    }

    @Test
    void testConflictDetection() {
        KeyboardShortcut cmdS = new KeyboardShortcut(KeyCode.S, false, false, false, true); // ⌘S
        List<KeymapAction> conflicts = manager.findConflicts(cmdS, "OtherAction");
        assertFalse(conflicts.isEmpty());
        assertTrue(conflicts.stream().anyMatch(a -> a.getId().equals("SaveAll")));
    }

    @Test
    void testMacSystemConflicts() {
        List<KeymapAction> sysConflicts = manager.getMacSystemConflicts();
        assertNotNull(sysConflicts);
        assertEquals(20, sysConflicts.size());

        // First 3 mentioned specifically in media_1790048100533.png
        assertTrue(sysConflicts.stream().anyMatch(a -> a.getId().equals("FindActions")));
        assertTrue(sysConflicts.stream().anyMatch(a -> a.getId().equals("BasicCompletion")));
        assertTrue(sysConflicts.stream().anyMatch(a -> a.getId().equals("MinimizeWindow")));
    }

    @Test
    void testFindActionsByShortcut() {
        KeyboardShortcut findActionSc = new KeyboardShortcut(KeyCode.A, false, false, true, true); // ⇧⌘A
        List<KeymapAction> actions = manager.findActionsByShortcut(findActionSc);
        assertFalse(actions.isEmpty());
        assertEquals("FindActions", actions.get(0).getId());
    }

    @Test
    void testListenerNotification() {
        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addListener(listener);

        notified.set(false);
        manager.setActiveKeymapByName("Eclipse");
        assertTrue(notified.get());

        manager.removeListener(listener);
    }
}
