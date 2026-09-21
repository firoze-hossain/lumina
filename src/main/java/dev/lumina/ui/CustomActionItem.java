package dev.lumina.ui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a configurable action, group, or separator in IntelliJ-style Menus and Toolbars.
 */
public class CustomActionItem implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String text;
    private String iconGlyph;
    private boolean group;
    private boolean separator;
    private boolean custom;
    private final List<CustomActionItem> children = new ArrayList<>();

    public CustomActionItem() {
    }

    public CustomActionItem(String id, String text, String iconGlyph, boolean group, boolean separator) {
        this.id = id;
        this.text = text;
        this.iconGlyph = iconGlyph != null ? iconGlyph : "";
        this.group = group;
        this.separator = separator;
        this.custom = false;
    }

    public static CustomActionItem action(String id, String text, String iconGlyph) {
        return new CustomActionItem(id, text, iconGlyph, false, false);
    }

    public static CustomActionItem group(String id, String text) {
        return new CustomActionItem(id, text, "", true, false);
    }

    public static CustomActionItem group(String id, String text, String iconGlyph) {
        return new CustomActionItem(id, text, iconGlyph, true, false);
    }

    public static CustomActionItem separator() {
        return new CustomActionItem("Separator_" + System.nanoTime(), "--------------------", "", false, true);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getIconGlyph() {
        return iconGlyph;
    }

    public void setIconGlyph(String iconGlyph) {
        this.iconGlyph = iconGlyph != null ? iconGlyph : "";
    }

    public boolean isGroup() {
        return group;
    }

    public void setGroup(boolean group) {
        this.group = group;
    }

    public boolean isSeparator() {
        return separator;
    }

    public void setSeparator(boolean separator) {
        this.separator = separator;
    }

    public boolean isCustom() {
        return custom;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }

    public List<CustomActionItem> getChildren() {
        return children;
    }

    public void addChild(CustomActionItem child) {
        if (child != null) {
            children.add(child);
        }
    }

    public void addChild(int index, CustomActionItem child) {
        if (child != null) {
            if (index >= 0 && index <= children.size()) {
                children.add(index, child);
            } else {
                children.add(child);
            }
        }
    }

    public boolean removeChild(CustomActionItem child) {
        return children.remove(child);
    }

    public CustomActionItem deepCopy() {
        CustomActionItem copy = new CustomActionItem(this.id, this.text, this.iconGlyph, this.group, this.separator);
        copy.setCustom(this.custom);
        for (CustomActionItem child : this.children) {
            copy.addChild(child.deepCopy());
        }
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomActionItem that = (CustomActionItem) o;
        return group == that.group &&
                separator == that.separator &&
                Objects.equals(id, that.id) &&
                Objects.equals(text, that.text) &&
                Objects.equals(iconGlyph, that.iconGlyph) &&
                Objects.equals(children, that.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, text, iconGlyph, group, separator, children);
    }

    @Override
    public String toString() {
        if (separator) {
            return "--------------------";
        }
        if (iconGlyph != null && !iconGlyph.isEmpty()) {
            return iconGlyph + " " + text;
        }
        return text;
    }
}
