package dev.lumina.keymap;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeyboardShortcutTest {

    @Test
    void testGlyphFormatting() {
        // ⇧⌘A (Shift + Command + A)
        KeyboardShortcut sc1 = new KeyboardShortcut(KeyCode.A, false, false, true, true);
        assertEquals("⇧⌘A", sc1.formatGlyphs());

        // ^Space (Control + Space)
        KeyboardShortcut sc2 = new KeyboardShortcut(KeyCode.SPACE, true, false, false, false);
        assertEquals("^Space", sc2.formatGlyphs());

        // ⌘M (Command + M)
        KeyboardShortcut sc3 = new KeyboardShortcut(KeyCode.M, false, false, false, true);
        assertEquals("⌘M", sc3.formatGlyphs());

        // ⌥⌘L (Option + Command + L)
        KeyboardShortcut sc4 = new KeyboardShortcut(KeyCode.L, false, true, false, true);
        assertEquals("⌥⌘L", sc4.formatGlyphs());

        // Arrows: →
        KeyboardShortcut scRight = new KeyboardShortcut(KeyCode.RIGHT, false, false, false, false);
        assertEquals("→", scRight.formatGlyphs());

        // Multi-stroke: Ctrl+K, Ctrl+C
        KeyboardShortcut stroke1 = new KeyboardShortcut(KeyCode.K, true, false, false, false);
        KeyboardShortcut stroke2 = new KeyboardShortcut(KeyCode.C, true, false, false, false);
        KeyboardShortcut multi = new KeyboardShortcut(stroke1.getKeyCode(), stroke1.isControl(), stroke1.isAlt(),
                stroke1.isShift(), stroke1.isMeta(), stroke2);
        assertEquals("^K, ^C", multi.formatGlyphs());
    }

    @Test
    void testTextFormatting() {
        // Ctrl+Shift+A
        KeyboardShortcut sc1 = new KeyboardShortcut(KeyCode.A, true, false, true, false);
        assertEquals("Ctrl+Shift+A", sc1.formatText());

        // Ctrl+Alt+L
        KeyboardShortcut sc2 = new KeyboardShortcut(KeyCode.L, true, true, false, false);
        assertEquals("Ctrl+Alt+L", sc2.formatText());
    }

    @Test
    void testSerializationAndDeserialization() {
        KeyboardShortcut original = new KeyboardShortcut(KeyCode.F, true, false, true, false);
        String stored = original.toStorageString();
        assertNotNull(stored);

        KeyboardShortcut parsed = KeyboardShortcut.fromStorageString(stored);
        assertEquals(original, parsed);

        // Multi-stroke serialization
        KeyboardShortcut stroke2 = new KeyboardShortcut(KeyCode.D, false, true, false, true);
        KeyboardShortcut multiOriginal = new KeyboardShortcut(original.getKeyCode(), original.isControl(),
                original.isAlt(), original.isShift(), original.isMeta(), stroke2);
        String multiStored = multiOriginal.toStorageString();

        KeyboardShortcut multiParsed = KeyboardShortcut.fromStorageString(multiStored);
        assertEquals(multiOriginal, multiParsed);
        assertTrue(multiParsed.hasSecondStroke());
        assertEquals(stroke2, multiParsed.getSecondStroke());
    }

    @Test
    void testEqualsAndHashCode() {
        KeyboardShortcut scA1 = new KeyboardShortcut(KeyCode.Z, false, false, false, true);
        KeyboardShortcut scA2 = new KeyboardShortcut(KeyCode.Z, false, false, false, true);
        KeyboardShortcut scB = new KeyboardShortcut(KeyCode.Z, false, false, true, true);

        assertEquals(scA1, scA2);
        assertEquals(scA1.hashCode(), scA2.hashCode());
        assertNotEquals(scA1, scB);
    }
}
