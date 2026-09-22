package dev.lumina.keymap;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.Objects;

/**
 * Immutable model representing a keyboard shortcut with modifiers and an optional second stroke.
 * Formats shortcuts using authentic IntelliJ glyphs (⇧⌘A, ^Space, ⌥⌘L, → or ^F) or text (Ctrl+Shift+A).
 */
public class KeyboardShortcut {

    private final KeyCode keyCode;
    private final boolean control;
    private final boolean alt;
    private final boolean shift;
    private final boolean meta;
    private final KeyboardShortcut secondStroke;

    public KeyboardShortcut(KeyCode keyCode, boolean control, boolean alt, boolean shift, boolean meta) {
        this(keyCode, control, alt, shift, meta, null);
    }

    public KeyboardShortcut(KeyCode keyCode, boolean control, boolean alt, boolean shift, boolean meta, KeyboardShortcut secondStroke) {
        this.keyCode = Objects.requireNonNull(keyCode, "keyCode cannot be null");
        this.control = control;
        this.alt = alt;
        this.shift = shift;
        this.meta = meta;
        this.secondStroke = secondStroke;
    }

    public KeyCode getKeyCode() {
        return keyCode;
    }

    public boolean isControl() {
        return control;
    }

    public boolean isAlt() {
        return alt;
    }

    public boolean isShift() {
        return shift;
    }

    public boolean isMeta() {
        return meta;
    }

    public KeyboardShortcut getSecondStroke() {
        return secondStroke;
    }

    public boolean hasSecondStroke() {
        return secondStroke != null;
    }

    /**
     * Formats the shortcut as IntelliJ-style glyphs (e.g. ⇧⌘A, ^Space, ⌘M, →).
     */
    public String formatGlyphs() {
        StringBuilder sb = new StringBuilder();
        appendStrokeGlyphs(sb, this);
        if (secondStroke != null) {
            sb.append(", ");
            appendStrokeGlyphs(sb, secondStroke);
        }
        return sb.toString();
    }

    private static void appendStrokeGlyphs(StringBuilder sb, KeyboardShortcut s) {
        // macOS standard order: Control, Option (Alt), Shift, Command
        if (s.control) sb.append("^");
        if (s.alt) sb.append("⌥");
        if (s.shift) sb.append("⇧");
        if (s.meta) sb.append("⌘");
        sb.append(formatKeyCodeGlyph(s.keyCode));
    }

    /**
     * Formats the shortcut as standard text (e.g. Ctrl+Shift+A).
     */
    public String formatText() {
        StringBuilder sb = new StringBuilder();
        appendStrokeText(sb, this);
        if (secondStroke != null) {
            sb.append(", ");
            appendStrokeText(sb, secondStroke);
        }
        return sb.toString();
    }

    private static void appendStrokeText(StringBuilder sb, KeyboardShortcut s) {
        boolean first = true;
        if (s.control) {
            sb.append("Ctrl");
            first = false;
        }
        if (s.alt) {
            if (!first) sb.append("+");
            sb.append("Alt");
            first = false;
        }
        if (s.shift) {
            if (!first) sb.append("+");
            sb.append("Shift");
            first = false;
        }
        if (s.meta) {
            if (!first) sb.append("+");
            sb.append("Meta");
            first = false;
        }
        if (!first) sb.append("+");
        sb.append(formatKeyCodeText(s.keyCode));
    }

    private static String formatKeyCodeGlyph(KeyCode code) {
        if (code == null) return "";
        return switch (code) {
            case ENTER -> "↵";
            case BACK_SPACE -> "⌫";
            case TAB -> "⇥";
            case ESCAPE -> "Esc";
            case SPACE -> "Space";
            case LEFT -> "←";
            case RIGHT -> "→";
            case UP -> "↑";
            case DOWN -> "↓";
            case DELETE -> "Del";
            case HOME -> "Home";
            case END -> "End";
            case PAGE_UP -> "PageUp";
            case PAGE_DOWN -> "PageDown";
            default -> code.getName();
        };
    }

    private static String formatKeyCodeText(KeyCode code) {
        if (code == null) return "";
        return switch (code) {
            case BACK_SPACE -> "Backspace";
            case ESCAPE -> "Esc";
            case DELETE -> "Delete";
            default -> code.getName();
        };
    }

    /**
     * Checks if this single-stroke shortcut matches the given KeyEvent.
     */
    public boolean matchesEvent(KeyEvent event) {
        if (event == null || event.getCode() == null) return false;
        if (this.secondStroke != null) {
            // Multi-stroke events cannot match a single KeyEvent
            return false;
        }
        return event.getCode() == this.keyCode &&
                event.isControlDown() == this.control &&
                event.isAltDown() == this.alt &&
                event.isShiftDown() == this.shift &&
                event.isMetaDown() == this.meta;
    }

    /**
     * Serializes this shortcut into a string for Java Preferences persistence.
     */
    public String toStorageString() {
        StringBuilder sb = new StringBuilder();
        sb.append(keyCode.name())
                .append(":")
                .append(control ? "1" : "0")
                .append(":")
                .append(alt ? "1" : "0")
                .append(":")
                .append(shift ? "1" : "0")
                .append(":")
                .append(meta ? "1" : "0");
        if (secondStroke != null) {
            sb.append(";").append(secondStroke.toStorageString());
        }
        return sb.toString();
    }

    /**
     * Deserializes a shortcut from its storage string representation.
     */
    public static KeyboardShortcut fromStorageString(String str) {
        if (str == null || str.isBlank()) return null;
        String[] strokes = str.split(";", 2);
        KeyboardShortcut first = parseSingleStroke(strokes[0]);
        if (first == null) return null;
        if (strokes.length > 1 && !strokes[1].isBlank()) {
            KeyboardShortcut second = parseSingleStroke(strokes[1]);
            return new KeyboardShortcut(first.keyCode, first.control, first.alt, first.shift, first.meta, second);
        }
        return first;
    }

    private static KeyboardShortcut parseSingleStroke(String strokeStr) {
        String[] parts = strokeStr.split(":");
        if (parts.length < 5) return null;
        try {
            KeyCode code = KeyCode.valueOf(parts[0]);
            boolean ctrl = "1".equals(parts[1]);
            boolean alt = "1".equals(parts[2]);
            boolean shift = "1".equals(parts[3]);
            boolean meta = "1".equals(parts[4]);
            return new KeyboardShortcut(code, ctrl, alt, shift, meta);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeyboardShortcut that = (KeyboardShortcut) o;
        return control == that.control &&
                alt == that.alt &&
                shift == that.shift &&
                meta == that.meta &&
                keyCode == that.keyCode &&
                Objects.equals(secondStroke, that.secondStroke);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyCode, control, alt, shift, meta, secondStroke);
    }

    @Override
    public String toString() {
        return formatGlyphs();
    }
}
