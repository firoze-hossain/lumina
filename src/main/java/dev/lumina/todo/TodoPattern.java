package dev.lumina.todo;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a TODO comment pattern regex and its visual styling attributes in Lumina IDE.
 * Faithfully models all options from the reference IDE Add/Edit Pattern dialog:
 * case sensitivity, icon type, color scheme default inheritance, custom colors,
 * font attributes (bold, italic), error stripe mark, and text effects (bordered, etc.).
 */
public class TodoPattern {

    private final String id;
    private String pattern;
    private boolean caseSensitive;
    private TodoIconType iconType;
    private boolean useDefaultColors = true;

    private boolean bold;
    private boolean italic;

    private boolean hasForeground = false;
    private String foregroundColor;

    private boolean hasBackground = false;
    private String backgroundColor;

    private boolean hasErrorStripe = false;
    private String errorStripeColor;

    private boolean hasEffects = false;
    private String effectsColor;
    private String effectType = "Bordered";

    private boolean builtIn;
    private boolean enabled;

    public TodoPattern(String id, String pattern, boolean caseSensitive, TodoIconType iconType,
                       String foregroundColor, String backgroundColor, boolean bold, boolean italic,
                       boolean builtIn, boolean enabled) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.pattern = Objects.requireNonNull(pattern, "pattern cannot be null").trim();
        this.caseSensitive = caseSensitive;
        this.iconType = iconType != null ? iconType : TodoIconType.TODO;
        this.foregroundColor = foregroundColor;
        this.hasForeground = foregroundColor != null && !foregroundColor.isBlank();
        this.backgroundColor = backgroundColor;
        this.hasBackground = backgroundColor != null && !backgroundColor.isBlank();
        this.bold = bold;
        this.italic = italic;
        this.useDefaultColors = (foregroundColor == null && backgroundColor == null);
        this.builtIn = builtIn;
        this.enabled = enabled;
    }

    public TodoPattern(TodoPattern other) {
        this.id = other.id;
        this.pattern = other.pattern;
        this.caseSensitive = other.caseSensitive;
        this.iconType = other.iconType;
        this.useDefaultColors = other.useDefaultColors;
        this.bold = other.bold;
        this.italic = other.italic;
        this.hasForeground = other.hasForeground;
        this.foregroundColor = other.foregroundColor;
        this.hasBackground = other.hasBackground;
        this.backgroundColor = other.backgroundColor;
        this.hasErrorStripe = other.hasErrorStripe;
        this.errorStripeColor = other.errorStripeColor;
        this.hasEffects = other.hasEffects;
        this.effectsColor = other.effectsColor;
        this.effectType = other.effectType;
        this.builtIn = other.builtIn;
        this.enabled = other.enabled;
    }

    public static TodoPattern of(String pattern, boolean caseSensitive, TodoIconType iconType, boolean builtIn) {
        return new TodoPattern(UUID.randomUUID().toString(), pattern, caseSensitive, iconType,
                null, null, false, false, builtIn, true);
    }

    public String getId() {
        return id;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = Objects.requireNonNull(pattern, "pattern cannot be null").trim();
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public TodoIconType getIconType() {
        return iconType;
    }

    public void setIconType(TodoIconType iconType) {
        this.iconType = iconType != null ? iconType : TodoIconType.TODO;
    }

    public boolean isUseDefaultColors() {
        return useDefaultColors;
    }

    public void setUseDefaultColors(boolean useDefaultColors) {
        this.useDefaultColors = useDefaultColors;
    }

    public boolean isBold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    public boolean isHasForeground() {
        return hasForeground;
    }

    public void setHasForeground(boolean hasForeground) {
        this.hasForeground = hasForeground;
    }

    public String getForegroundColor() {
        return foregroundColor;
    }

    public void setForegroundColor(String foregroundColor) {
        this.foregroundColor = foregroundColor;
    }

    public boolean isHasBackground() {
        return hasBackground;
    }

    public void setHasBackground(boolean hasBackground) {
        this.hasBackground = hasBackground;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public boolean isHasErrorStripe() {
        return hasErrorStripe;
    }

    public void setHasErrorStripe(boolean hasErrorStripe) {
        this.hasErrorStripe = hasErrorStripe;
    }

    public String getErrorStripeColor() {
        return errorStripeColor;
    }

    public void setErrorStripeColor(String errorStripeColor) {
        this.errorStripeColor = errorStripeColor;
    }

    public boolean isHasEffects() {
        return hasEffects;
    }

    public void setHasEffects(boolean hasEffects) {
        this.hasEffects = hasEffects;
    }

    public String getEffectsColor() {
        return effectsColor;
    }

    public void setEffectsColor(String effectsColor) {
        this.effectsColor = effectsColor;
    }

    public String getEffectType() {
        return effectType;
    }

    public void setEffectType(String effectType) {
        this.effectType = effectType != null ? effectType : "Bordered";
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(boolean builtIn) {
        this.builtIn = builtIn;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TodoPattern that)) return false;
        return caseSensitive == that.caseSensitive &&
                useDefaultColors == that.useDefaultColors &&
                bold == that.bold &&
                italic == that.italic &&
                hasForeground == that.hasForeground &&
                hasBackground == that.hasBackground &&
                hasErrorStripe == that.hasErrorStripe &&
                hasEffects == that.hasEffects &&
                builtIn == that.builtIn &&
                enabled == that.enabled &&
                Objects.equals(id, that.id) &&
                Objects.equals(pattern, that.pattern) &&
                iconType == that.iconType &&
                Objects.equals(foregroundColor, that.foregroundColor) &&
                Objects.equals(backgroundColor, that.backgroundColor) &&
                Objects.equals(errorStripeColor, that.errorStripeColor) &&
                Objects.equals(effectsColor, that.effectsColor) &&
                Objects.equals(effectType, that.effectType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, pattern, caseSensitive, iconType, useDefaultColors,
                bold, italic, hasForeground, foregroundColor, hasBackground, backgroundColor,
                hasErrorStripe, errorStripeColor, hasEffects, effectsColor, effectType, builtIn, enabled);
    }

    @Override
    public String toString() {
        return pattern + " (" + iconType + ")";
    }
}
