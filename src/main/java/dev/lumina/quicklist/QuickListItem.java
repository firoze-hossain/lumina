package dev.lumina.quicklist;

import java.util.Objects;

/**
 * An item in a Quick List (either an IDE action or a visual separator).
 * Strictly matches media_1790045975661.png.
 */
public class QuickListItem {

    private final String actionId;
    private final String text;
    private final String iconGlyph;
    private final boolean separator;

    public QuickListItem(String actionId, String text, String iconGlyph, boolean separator) {
        this.actionId = actionId != null ? actionId : "";
        this.text = text != null ? text : "";
        this.iconGlyph = iconGlyph != null ? iconGlyph : "";
        this.separator = separator;
    }

    public static QuickListItem action(String actionId, String text, String iconGlyph) {
        return new QuickListItem(actionId, text, iconGlyph, false);
    }

    public static QuickListItem separator() {
        return new QuickListItem("---", "-------------", "", true);
    }

    public String getActionId() {
        return actionId;
    }

    public String getText() {
        return text;
    }

    public String getIconGlyph() {
        return iconGlyph;
    }

    public boolean isSeparator() {
        return separator;
    }

    public QuickListItem deepCopy() {
        return new QuickListItem(actionId, text, iconGlyph, separator);
    }

    public String serialize() {
        return (separator ? "SEP" : "ACT") + "\t" +
                actionId.replace("\t", " ") + "\t" +
                text.replace("\t", " ") + "\t" +
                iconGlyph.replace("\t", " ");
    }

    public static QuickListItem deserialize(String line) {
        if (line == null || line.isBlank()) return null;
        String[] parts = line.split("\t", -1);
        if (parts.length >= 4) {
            boolean sep = "SEP".equalsIgnoreCase(parts[0]);
            return new QuickListItem(parts[1], parts[2], parts[3], sep);
        } else if (parts.length >= 2) {
            return new QuickListItem(parts[0], parts[1], "", false);
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuickListItem that = (QuickListItem) o;
        return separator == that.separator &&
                Objects.equals(actionId, that.actionId) &&
                Objects.equals(text, that.text) &&
                Objects.equals(iconGlyph, that.iconGlyph);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actionId, text, iconGlyph, separator);
    }

    @Override
    public String toString() {
        return separator ? "-------------" : (iconGlyph.isEmpty() ? text : iconGlyph + " " + text);
    }
}
