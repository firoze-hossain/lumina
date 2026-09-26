package dev.lumina.settings;

import dev.lumina.util.Settings;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic persistent model and manager for Editor > Color Scheme settings.
 * Backed by ~/.lumina/lumina.properties and ~/.lumina/colors/, supporting
 * dynamic scheme registration, duplication, restore defaults, export/import,
 * live preview synchronization, and runtime editor updates.
 */
public final class EditorColorSchemeSettings {

    public static final String DEFAULT_SCHEME = "Islands Dark Theme default";

    public static final List<String> BUNDLED_SCHEMES = List.of(
            "Islands Dark Theme default",
            "Light",
            "High Contrast",
            "Classic Light",
            "Darcula",
            "Darcula Contrast",
            "Dark"
    );

    private static class InstanceHolder {
        private static final EditorColorSchemeSettings INSTANCE = new EditorColorSchemeSettings();
    }

    public static EditorColorSchemeSettings getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public enum EffectType {
        NONE("None"),
        UNDERSCORED("Underscored"),
        BOLD_UNDERSCORED("Bold Underscored"),
        UNDERWAVED("Underwaved"),
        BORDERED("Bordered"),
        STRIKEOUT("Strikeout"),
        DOTTED_LINE("Dotted Line");

        private final String displayName;

        EffectType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }

        public static EffectType fromDisplayName(String name) {
            for (EffectType type : values()) {
                if (type.displayName.equalsIgnoreCase(name) || type.name().equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return NONE;
        }
    }

    public static final class ColorAttribute implements Cloneable {
        private String foreground;
        private String background;
        private String errorStripeColor;
        private EffectType effectType = EffectType.NONE;
        private String effectColor;
        private boolean bold;
        private boolean italic;
        private boolean inherit = true;
        private String inheritFrom;
        private String inheritScope;

        public ColorAttribute() {}

        public ColorAttribute(String foreground, String background, boolean bold, boolean italic) {
            this.foreground = foreground;
            this.background = background;
            this.bold = bold;
            this.italic = italic;
            this.inherit = false;
        }

        public ColorAttribute(String foreground, String background, String errorStripeColor, EffectType effectType, String effectColor, boolean bold, boolean italic) {
            this.foreground = foreground;
            this.background = background;
            this.errorStripeColor = errorStripeColor;
            this.effectType = effectType != null ? effectType : EffectType.NONE;
            this.effectColor = effectColor;
            this.bold = bold;
            this.italic = italic;
            this.inherit = false;
        }

        public String getForeground() { return foreground; }
        public void setForeground(String foreground) { this.foreground = foreground; }

        public String getBackground() { return background; }
        public void setBackground(String background) { this.background = background; }

        public String getErrorStripeColor() { return errorStripeColor; }
        public void setErrorStripeColor(String errorStripeColor) { this.errorStripeColor = errorStripeColor; }

        public EffectType getEffectType() { return effectType; }
        public void setEffectType(EffectType effectType) { this.effectType = effectType != null ? effectType : EffectType.NONE; }

        public String getEffectColor() { return effectColor; }
        public void setEffectColor(String effectColor) { this.effectColor = effectColor; }

        public boolean isBold() { return bold; }
        public void setBold(boolean bold) { this.bold = bold; }

        public boolean isItalic() { return italic; }
        public void setItalic(boolean italic) { this.italic = italic; }

        public boolean isInherit() { return inherit; }
        public void setInherit(boolean inherit) { this.inherit = inherit; }

        public String getInheritFrom() { return inheritFrom; }
        public void setInheritFrom(String inheritFrom) { this.inheritFrom = inheritFrom; }

        public String getInheritScope() { return inheritScope; }
        public void setInheritScope(String inheritScope) { this.inheritScope = inheritScope; }

        @Override
        public ColorAttribute clone() {
            try {
                return (ColorAttribute) super.clone();
            } catch (CloneNotSupportedException e) {
                ColorAttribute copy = new ColorAttribute();
                copy.foreground = this.foreground;
                copy.background = this.background;
                copy.errorStripeColor = this.errorStripeColor;
                copy.effectType = this.effectType;
                copy.effectColor = this.effectColor;
                copy.bold = this.bold;
                copy.italic = this.italic;
                copy.inherit = this.inherit;
                copy.inheritFrom = this.inheritFrom;
                copy.inheritScope = this.inheritScope;
                return copy;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ColorAttribute that = (ColorAttribute) o;
            return bold == that.bold &&
                    italic == that.italic &&
                    inherit == that.inherit &&
                    Objects.equals(foreground, that.foreground) &&
                    Objects.equals(background, that.background) &&
                    Objects.equals(errorStripeColor, that.errorStripeColor) &&
                    effectType == that.effectType &&
                    Objects.equals(effectColor, that.effectColor) &&
                    Objects.equals(inheritFrom, that.inheritFrom) &&
                    Objects.equals(inheritScope, that.inheritScope);
        }

        @Override
        public int hashCode() {
            return Objects.hash(foreground, background, errorStripeColor, effectType, effectColor, bold, italic, inherit, inheritFrom, inheritScope);
        }
    }

    @FunctionalInterface
    public interface Listener {
        void onColorSchemeChanged(EditorColorSchemeSettings settings);
    }

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private String activeSchemeName = DEFAULT_SCHEME;
    private final List<String> customSchemes = new CopyOnWriteArrayList<>();
    private final Map<String, Map<String, ColorAttribute>> schemeAttributes = new ConcurrentHashMap<>();
    private final Map<String, Map<String, ColorAttribute>> defaultSchemeAttributes = new ConcurrentHashMap<>();

    public EditorColorSchemeSettings() {
        initDefaults();
        load();
    }

    public void initDefaults() {
        activeSchemeName = DEFAULT_SCHEME;
        customSchemes.clear();
        schemeAttributes.clear();
        defaultSchemeAttributes.clear();

        // Populate base defaults for Islands Dark Theme default
        Map<String, ColorAttribute> islandsDark = createIslandsDarkDefaults();
        defaultSchemeAttributes.put(DEFAULT_SCHEME, cloneAttributesMap(islandsDark));
        schemeAttributes.put(DEFAULT_SCHEME, cloneAttributesMap(islandsDark));

        // Create defaults for other bundled schemes
        for (String bundled : BUNDLED_SCHEMES) {
            if (!bundled.equals(DEFAULT_SCHEME)) {
                Map<String, ColorAttribute> map = createIslandsDarkDefaults();
                if (bundled.contains("Light")) {
                    map.put("Text // Default text", new ColorAttribute("#000000", "#FFFFFF", false, false));
                }
                defaultSchemeAttributes.put(bundled, cloneAttributesMap(map));
                schemeAttributes.put(bundled, cloneAttributesMap(map));
            }
        }
    }

    public static final class AttributesDescriptor {
        private final String key;
        private final String displayName;
        private final List<String> categoryPath;
        private final ColorAttribute defaultAttribute;
        private final String inheritFrom;
        private final String inheritScope;
        private final boolean supportsForeground;
        private final boolean supportsBackground;
        private final boolean supportsErrorStripe;
        private final boolean supportsEffects;
        private final EffectType defaultEffectType;
        private final boolean supportsFont;

        public AttributesDescriptor(String key, String displayName, List<String> categoryPath,
                                    ColorAttribute defaultAttribute,
                                    String inheritFrom, String inheritScope,
                                    boolean supportsForeground, boolean supportsBackground,
                                    boolean supportsErrorStripe, boolean supportsEffects,
                                    EffectType defaultEffectType, boolean supportsFont) {
            this.key = key;
            this.displayName = displayName;
            this.categoryPath = categoryPath != null ? List.copyOf(categoryPath) : List.of();
            this.defaultAttribute = defaultAttribute != null ? defaultAttribute.clone() : new ColorAttribute();
            this.inheritFrom = inheritFrom;
            this.inheritScope = inheritScope;
            this.supportsForeground = supportsForeground;
            this.supportsBackground = supportsBackground;
            this.supportsErrorStripe = supportsErrorStripe;
            this.supportsEffects = supportsEffects;
            this.defaultEffectType = defaultEffectType != null ? defaultEffectType : EffectType.UNDERSCORED;
            this.supportsFont = supportsFont;
        }

        public String getKey() { return key; }
        public String getDisplayName() { return displayName; }
        public List<String> getCategoryPath() { return categoryPath; }
        public ColorAttribute getDefaultAttribute() { return defaultAttribute.clone(); }
        public String getInheritFrom() { return inheritFrom; }
        public String getInheritScope() { return inheritScope; }
        public boolean hasInheritance() { return inheritFrom != null && !inheritFrom.isBlank(); }
        public boolean isSupportsForeground() { return supportsForeground; }
        public boolean isSupportsBackground() { return supportsBackground; }
        public boolean isSupportsErrorStripe() { return supportsErrorStripe; }
        public boolean isSupportsEffects() { return supportsEffects; }
        public EffectType getDefaultEffectType() { return defaultEffectType; }
        public boolean isSupportsFont() { return supportsFont; }
    }

    private static ColorAttribute inheritAttr(String fg, String bg, String inheritFrom, String inheritScope) {
        ColorAttribute attr = new ColorAttribute(fg, bg, false, false);
        attr.setInherit(true);
        attr.setInheritFrom(inheritFrom);
        attr.setInheritScope(inheritScope);
        return attr;
    }

    private static ColorAttribute inheritAttr(String fg, String bg, String stripe, EffectType effectType, String effectColor, String inheritFrom, String inheritScope) {
        ColorAttribute attr = new ColorAttribute(fg, bg, stripe, effectType, effectColor, false, false);
        attr.setInherit(true);
        attr.setInheritFrom(inheritFrom);
        attr.setInheritScope(inheritScope);
        return attr;
    }

    private static ColorAttribute inheritAttr(String fg, String bg, String inheritFrom, String inheritScope, boolean inherit) {
        ColorAttribute attr = new ColorAttribute(fg, bg, false, false);
        attr.setInherit(inherit);
        attr.setInheritFrom(inheritFrom);
        attr.setInheritScope(inheritScope);
        return attr;
    }

    private static final List<AttributesDescriptor> GENERAL_DESCRIPTORS = List.of(
            // Code (media_1790343290072.png & media_1790343304243.png)
            new AttributesDescriptor("Code // Identifier under caret", "Identifier under caret", List.of("Code"),
                    new ColorAttribute(null, "#373B39", "#5B786A", EffectType.UNDERSCORED, null, false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Code // Identifier under caret (write)", "Identifier under caret (write)", List.of("Code"),
                    new ColorAttribute(null, "#402E3B", "#BA5F9E", EffectType.UNDERSCORED, null, false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Code // Injected language fragment", "Injected language fragment", List.of("Code"),
                    new ColorAttribute(null, "#2B3838", false, false),
                    null, null, true, true, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Code // Line number", "Line number", List.of("Code"),
                    new ColorAttribute("#4E5157", null, false, false),
                    null, null, true, false, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Code // Line number on caret row", "Line number on caret row", List.of("Code"),
                    new ColorAttribute("#A1A3AB", null, null, EffectType.UNDERSCORED, null, false, false),
                    null, null, true, true, false, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Code // Matched brace", "Matched brace", List.of("Code"),
                    new ColorAttribute(null, "#3B514D", false, false),
                    null, null, true, true, false, true, EffectType.BORDERED, false),
            new AttributesDescriptor("Code // Method separator color", "Method separator color", List.of("Code"),
                    new ColorAttribute("#393B40", null, false, false),
                    null, null, true, false, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Code // TODO defaults", "TODO defaults", List.of("Code"),
                    new ColorAttribute("#549159", null, true, true),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Code // Unmatched brace", "Unmatched brace", List.of("Code"),
                    new ColorAttribute(null, "#562423", false, false),
                    null, null, true, true, false, true, EffectType.BORDERED, false),

            // Editor (media_1790343317824.png, media_1790343353746.png, media_1790343452222.png - media_1790343516571.png)
            new AttributesDescriptor("Editor // Bookmarks", "Bookmarks", List.of("Editor"),
                    new ColorAttribute(null, null, "#F7E9C6", EffectType.BORDERED, null, false, false),
                    null, null, false, false, true, true, EffectType.BORDERED, false),
            new AttributesDescriptor("Editor // Breadcrumbs // Border", "Border", List.of("Editor", "Breadcrumbs"),
                    new ColorAttribute("#393B40", null, false, false),
                    null, null, true, true, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Editor // Breadcrumbs // Current", "Current", List.of("Editor", "Breadcrumbs"),
                    new ColorAttribute("#DFE1E5", "#2B2D30", null, EffectType.BORDERED, null, false, false),
                    null, null, true, true, false, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Editor // Breadcrumbs // Default", "Default", List.of("Editor", "Breadcrumbs"),
                    new ColorAttribute("#848BA3", null, false, false),
                    null, null, true, true, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Editor // Breadcrumbs // Hovered", "Hovered", List.of("Editor", "Breadcrumbs"),
                    new ColorAttribute("#DFE1E5", "#35373B", false, false),
                    null, null, true, true, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Editor // Breadcrumbs // Inactive", "Inactive", List.of("Editor", "Breadcrumbs"),
                    new ColorAttribute("#5F6368", null, false, false),
                    null, null, true, true, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Editor // Caret", "Caret", List.of("Editor"),
                    new ColorAttribute("#DFE1E5", null, false, false),
                    null, null, true, false, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Editor // Caret row", "Caret row", List.of("Editor"),
                    new ColorAttribute(null, "#1F2024", false, false),
                    null, null, false, true, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Editor // Guides // Hard wrap guide", "Hard wrap guide", List.of("Editor", "Guides"),
                    new ColorAttribute("#323438", null, false, false),
                    null, null, true, false, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Editor // Guides // Indent guide", "Indent guide", List.of("Editor", "Guides"),
                    new ColorAttribute("#323438", null, false, false),
                    null, null, true, false, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Editor // Guides // Indent guide selected", "Indent guide selected", List.of("Editor", "Guides"),
                    new ColorAttribute("#4E5157", null, false, false),
                    null, null, true, false, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Editor // Guides // Matched brace guide", "Matched brace guide", List.of("Editor", "Guides"),
                    new ColorAttribute("#3B514D", null, false, false),
                    null, null, true, false, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Editor // Guides // Visual guides", "Visual guides", List.of("Editor", "Guides"),
                    new ColorAttribute("#393B40", null, false, false),
                    null, null, true, false, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Editor // Gutter background", "Gutter background", List.of("Editor"),
                    new ColorAttribute(null, null, false, false),
                    null, null, false, true, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Editor // Notification background", "Notification background", List.of("Editor"),
                    new ColorAttribute(null, "#25324D", false, false),
                    null, null, false, true, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Editor // Selection background", "Selection background", List.of("Editor"),
                    new ColorAttribute(null, "#214283", false, false),
                    null, null, false, true, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Editor // Selection foreground", "Selection foreground", List.of("Editor"),
                    new ColorAttribute(null, null, false, false),
                    null, null, true, false, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Editor // Sticky Lines // Background", "Background", List.of("Editor", "Sticky Lines"),
                    new ColorAttribute(null, null, false, false),
                    null, null, false, true, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Editor // Sticky Lines // Border", "Border", List.of("Editor", "Sticky Lines"),
                    inheritAttr(null, null, "Editor // Guides // Hard wrap guide", "(General)"),
                    "Editor // Guides // Hard wrap guide", "(General)",
                    false, true, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Editor // Sticky Lines // Hovered", "Hovered", List.of("Editor", "Sticky Lines"),
                    inheritAttr(null, null, "Editor // Caret row", "(General)"),
                    "Editor // Caret row", "(General)",
                    false, true, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Editor // Tabs // Modified icon color", "Modified icon color", List.of("Editor", "Tabs"),
                    new ColorAttribute("#4083C9", null, false, false),
                    null, null, true, false, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Editor // Tabs // Selected Tab", "Selected Tab", List.of("Editor", "Tabs"),
                    new ColorAttribute(null, "#1E1F22", false, false),
                    null, null, false, true, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Editor // Tabs // Selected Tab inactive", "Selected Tab inactive", List.of("Editor", "Tabs"),
                    new ColorAttribute(null, "#2B2D30", false, false),
                    null, null, false, true, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Editor // Tabs // Underline", "Underline", List.of("Editor", "Tabs"),
                    new ColorAttribute("#3574F0", null, false, false),
                    null, null, true, false, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Editor // Tabs // Underline inactive", "Underline inactive", List.of("Editor", "Tabs"),
                    new ColorAttribute("#4E5157", null, false, false),
                    null, null, true, false, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Editor // Tear line", "Tear line", List.of("Editor"),
                    new ColorAttribute("#393B40", null, false, false),
                    null, null, true, false, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Editor // Tear line selection", "Tear line selection", List.of("Editor"),
                    new ColorAttribute("#4E5157", null, false, false),
                    null, null, true, false, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Editor // Vertical Scrollbar // Thumb", "Thumb", List.of("Editor", "Vertical Scrollbar"),
                    new ColorAttribute(null, "#4E5157", false, false),
                    null, null, false, true, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Editor // Vertical Scrollbar // Thumb while scrolling", "Thumb while scrolling", List.of("Editor", "Vertical Scrollbar"),
                    new ColorAttribute(null, "#A6A6A6", false, false),
                    null, null, false, true, false, false, EffectType.NONE, false),
            new AttributesDescriptor("Editor // Vertical Scrollbar // Track", "Track", List.of("Editor", "Vertical Scrollbar"),
                    new ColorAttribute(null, "#1E1F22", false, false),
                    null, null, false, true, false, false, EffectType.NONE, false),

            // Errors and Warnings (media_1790345048357.png - media_1790345076709.png)
            new AttributesDescriptor("Errors and Warnings // Deprecated symbol", "Deprecated symbol", List.of("Errors and Warnings"),
                    new ColorAttribute(null, null, null, EffectType.STRIKEOUT, "#8C8C8C", false, false),
                    null, null, true, true, true, true, EffectType.STRIKEOUT, true),
            new AttributesDescriptor("Errors and Warnings // Deprecated symbol marked for removal", "Deprecated symbol marked for removal", List.of("Errors and Warnings"),
                    new ColorAttribute(null, null, null, EffectType.STRIKEOUT, "#F75464", false, false),
                    null, null, true, true, true, true, EffectType.STRIKEOUT, true),
            new AttributesDescriptor("Errors and Warnings // Duplicate from server", "Duplicate from server", List.of("Errors and Warnings"),
                    new ColorAttribute(null, "#5E5339", null, EffectType.BORDERED, null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Errors and Warnings // Error", "Error", List.of("Errors and Warnings"),
                    new ColorAttribute(null, null, "#E5534B", EffectType.UNDERWAVED, "#F75464", false, false),
                    null, null, true, true, true, true, EffectType.UNDERWAVED, true),
            new AttributesDescriptor("Errors and Warnings // Grammar error", "Grammar error", List.of("Errors and Warnings"),
                    new ColorAttribute(null, null, null, EffectType.UNDERWAVED, "#713D40", false, false),
                    null, null, true, true, true, true, EffectType.UNDERWAVED, true),
            new AttributesDescriptor("Errors and Warnings // Problem from server", "Problem from server", List.of("Errors and Warnings"),
                    new ColorAttribute(null, null, null, EffectType.BORDERED, "#C29E4A", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Errors and Warnings // Runtime problem", "Runtime problem", List.of("Errors and Warnings"),
                    new ColorAttribute(null, null, null, EffectType.UNDERWAVED, "#F75464", false, false),
                    null, null, true, true, true, true, EffectType.UNDERWAVED, true),
            new AttributesDescriptor("Errors and Warnings // Text style suggestion", "Text style suggestion", List.of("Errors and Warnings"),
                    new ColorAttribute(null, null, null, EffectType.UNDERSCORED, "#589DF6", false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Errors and Warnings // Typo", "Typo", List.of("Errors and Warnings"),
                    new ColorAttribute(null, null, null, EffectType.UNDERWAVED, "#4B7258", false, false),
                    null, null, true, true, true, true, EffectType.UNDERWAVED, true),
            new AttributesDescriptor("Errors and Warnings // Unknown symbol", "Unknown symbol", List.of("Errors and Warnings"),
                    new ColorAttribute("#F75464", null, false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Errors and Warnings // Unused code", "Unused code", List.of("Errors and Warnings"),
                    new ColorAttribute("#70727B", null, false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Errors and Warnings // Warning", "Warning", List.of("Errors and Warnings"),
                    new ColorAttribute(null, null, "#C29E4A", EffectType.UNDERWAVED, "#F2C55C", false, false),
                    null, null, true, true, true, true, EffectType.UNDERWAVED, true),
            new AttributesDescriptor("Errors and Warnings // Weak Warning", "Weak Warning", List.of("Errors and Warnings"),
                    new ColorAttribute(null, null, "#B9BECF", EffectType.UNDERWAVED, "#B9BECF", false, false),
                    null, null, true, true, true, true, EffectType.UNDERWAVED, true),

            // Hyperlinks (media_1790358354725.png)
            new AttributesDescriptor("Hyperlinks // Followed", "Followed", List.of("Hyperlinks"),
                    new ColorAttribute("#B189F5", null, null, EffectType.UNDERSCORED, "#B189F5", false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Hyperlinks // Inactive", "Inactive", List.of("Hyperlinks"),
                    new ColorAttribute("#70727B", null, false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Hyperlinks // Reference", "Reference", List.of("Hyperlinks"),
                    new ColorAttribute("#548AF7", null, null, EffectType.UNDERSCORED, "#548AF7", false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Hyperlinks // Unfollowed", "Unfollowed", List.of("Hyperlinks"),
                    new ColorAttribute("#548AF7", null, null, EffectType.UNDERSCORED, "#548AF7", false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),

            // Identifiers (media_1790381886079.png)
            new AttributesDescriptor("Identifiers // Reassigned local variable", "Reassigned local variable", List.of("Identifiers"),
                    inheritAttr("#BCBEC4", null, null, EffectType.UNDERSCORED, "#84868C", "Identifiers // Reassigned local variable", "(Language Defaults)"),
                    "Identifiers // Reassigned local variable", "(Language Defaults)",
                    true, true, true, true, EffectType.UNDERSCORED, true),

            // Line Coverage (media_1790381912220.png)
            new AttributesDescriptor("Line Coverage // Full", "Full", List.of("Line Coverage"),
                    new ColorAttribute("#375239", null, true, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Line Coverage // Partial", "Partial", List.of("Line Coverage"),
                    new ColorAttribute("#6B5620", null, true, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Line Coverage // Uncovered", "Uncovered", List.of("Line Coverage"),
                    new ColorAttribute("#713D40", null, true, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),

            // Live Templates (media_1790381939807.png)
            new AttributesDescriptor("Live Templates // Active Segment", "Active Segment", List.of("Live Templates"),
                    new ColorAttribute(null, null, null, EffectType.BORDERED, "#385E9D", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Live Templates // Inactive Segment", "Inactive Segment", List.of("Live Templates"),
                    new ColorAttribute(null, null, null, EffectType.BORDERED, "#5A5D63", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Live Templates // Template Variable", "Template Variable", List.of("Live Templates"),
                    new ColorAttribute("#B189F5", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),

            // Popups and Hints (media_1790381958006.png)
            new AttributesDescriptor("Popups and Hints // Code lens", "Code lens", List.of("Popups and Hints"),
                    new ColorAttribute("#70727B", null, false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Popups and Hints // Completion", "Completion", List.of("Popups and Hints"),
                    new ColorAttribute("#DFE1E5", "#2B2D30", false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Popups and Hints // Documentation", "Documentation", List.of("Popups and Hints"),
                    new ColorAttribute("#DFE1E5", "#2B2D30", false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Popups and Hints // Error hint", "Error hint", List.of("Popups and Hints"),
                    new ColorAttribute("#DFE1E5", "#562423", false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Popups and Hints // Hint border", "Hint border", List.of("Popups and Hints"),
                    new ColorAttribute(null, null, null, EffectType.BORDERED, "#393B40", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Popups and Hints // Information hint", "Information hint", List.of("Popups and Hints"),
                    new ColorAttribute("#DFE1E5", "#2B2D30", false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Popups and Hints // Promotion pane", "Promotion pane", List.of("Popups and Hints"),
                    new ColorAttribute(null, "#25324D", false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Popups and Hints // Question hint", "Question hint", List.of("Popups and Hints"),
                    new ColorAttribute("#DFE1E5", "#25324D", false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Popups and Hints // Recent locations selection", "Recent locations selection", List.of("Popups and Hints"),
                    new ColorAttribute(null, "#2E436E", false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Popups and Hints // Tooltip", "Tooltip", List.of("Popups and Hints"),
                    new ColorAttribute("#DFE1E5", "#2B2D30", false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Popups and Hints // Warning hint", "Warning hint", List.of("Popups and Hints"),
                    new ColorAttribute(null, "#665014", false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),

            // Preview (media_1790381971087.png)
            new AttributesDescriptor("Preview // Background", "Background", List.of("Preview"),
                    new ColorAttribute(null, "#1E1F22", false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Preview // Border", "Border", List.of("Preview"),
                    inheritAttr(null, null, "Editor // Guides // Indent guide", "(General)"),
                    "Editor // Guides // Indent guide", "(General)",
                    true, true, true, true, EffectType.UNDERSCORED, true),

            // Search Results (media_1790358799065.png)
            new AttributesDescriptor("Search Results // Search result", "Search result", List.of("Search Results"),
                    new ColorAttribute(null, "#265261", "#2E5F7E", EffectType.NONE, null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Search Results // Search result (write access)", "Search result (write access)", List.of("Search Results"),
                    new ColorAttribute(null, "#66313F", "#FA7DB1", EffectType.NONE, null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Search Results // Text search result", "Text search result", List.of("Search Results"),
                    new ColorAttribute(null, "#32593D", "#3C804A", EffectType.NONE, null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),

            // Text (media_1790358818750.png)
            new AttributesDescriptor("Text // Background in read-only files", "Background in read-only files", List.of("Text"),
                    new ColorAttribute(null, null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Text // Default text", "Default text", List.of("Text"),
                    new ColorAttribute("#DFE1E5", "#1E1F22", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Text // Deleted text", "Deleted text", List.of("Text"),
                    new ColorAttribute("#E5534B", null, null, EffectType.STRIKEOUT, "#E5534B", false, false),
                    null, null, true, true, true, true, EffectType.STRIKEOUT, true),
            new AttributesDescriptor("Text // Folded text", "Folded text", List.of("Text"),
                    new ColorAttribute("#868991", "#393B40", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Text // Folded text with highlighting", "Folded text with highlighting", List.of("Text"),
                    new ColorAttribute("#868991", "#393B40", null, EffectType.UNDERWAVED, "#C29E4A", false, false),
                    null, null, true, true, true, true, EffectType.UNDERWAVED, true),
            new AttributesDescriptor("Text // Read-only fragment background", "Read-only fragment background", List.of("Text"),
                    new ColorAttribute(null, "#2B2D30", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Text // Soft wrap sign", "Soft wrap sign", List.of("Text"),
                    new ColorAttribute("#4E5157", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Text // Tabs", "Tabs", List.of("Text"),
                    new ColorAttribute("#4E5157", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Text // Whitespaces", "Whitespaces", List.of("Text"),
                    new ColorAttribute("#4E5157", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true)
    );

    private static final List<AttributesDescriptor> LANGUAGE_DEFAULTS_DESCRIPTORS = List.of(
            // Bad character (media_1790383980530.png)
            new AttributesDescriptor("Bad character", "Bad character", List.of(),
                    new ColorAttribute("#F75464", null, false, false),
                    null, null, true, true, true, true, EffectType.UNDERWAVED, true),

            // Braces and Operators (media_1790383998278.png)
            new AttributesDescriptor("Braces and Operators // Braces", "Braces", List.of("Braces and Operators"),
                    new ColorAttribute("#BCBEC4", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Braces and Operators // Brackets", "Brackets", List.of("Braces and Operators"),
                    new ColorAttribute("#BCBEC4", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Braces and Operators // Comma", "Comma", List.of("Braces and Operators"),
                    new ColorAttribute("#BCBEC4", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Braces and Operators // Dot", "Dot", List.of("Braces and Operators"),
                    new ColorAttribute("#BCBEC4", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Braces and Operators // Operation sign", "Operation sign", List.of("Braces and Operators"),
                    new ColorAttribute("#BCBEC4", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Braces and Operators // Parentheses", "Parentheses", List.of("Braces and Operators"),
                    new ColorAttribute("#BCBEC4", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Braces and Operators // Semicolon", "Semicolon", List.of("Braces and Operators"),
                    new ColorAttribute("#BCBEC4", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),

            // Classes (media_1790384011289.png)
            new AttributesDescriptor("Classes // Class name", "Class name", List.of("Classes"),
                    new ColorAttribute("#DFE1E5", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Classes // Class reference", "Class reference", List.of("Classes"),
                    inheritAttr("#BCBEC4", null, "Identifiers // Default", "(Language Defaults)", false),
                    "Identifiers // Default", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Classes // Instance field", "Instance field", List.of("Classes"),
                    new ColorAttribute("#C77DBB", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Classes // Instance method", "Instance method", List.of("Classes"),
                    new ColorAttribute("#56A8F5", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Classes // Interface name", "Interface name", List.of("Classes"),
                    new ColorAttribute("#DFE1E5", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Classes // Static field", "Static field", List.of("Classes"),
                    new ColorAttribute("#C77DBB", null, false, true),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Classes // Static method", "Static method", List.of("Classes"),
                    new ColorAttribute("#56A8F5", null, false, true),
                    null, null, true, true, true, true, EffectType.BORDERED, true),

            // Comments (media_1790384048158.png)
            new AttributesDescriptor("Comments // Block comment", "Block comment", List.of("Comments"),
                    new ColorAttribute("#7A7E85", null, false, true),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Comments // Doc comment // Code block", "Code block", List.of("Comments", "Doc comment"),
                    new ColorAttribute("#DFE1E5", null, false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Comments // Doc comment // Inline code fragment", "Inline code fragment", List.of("Comments", "Doc comment"),
                    new ColorAttribute("#DFE1E5", null, false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Comments // Doc comment // Link in rendered view", "Link in rendered view", List.of("Comments", "Doc comment"),
                    new ColorAttribute("#3887A1", null, false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Comments // Doc comment // Markup", "Markup", List.of("Comments", "Doc comment"),
                    new ColorAttribute("#549159", null, false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Comments // Doc comment // Shortcut", "Shortcut", List.of("Comments", "Doc comment"),
                    new ColorAttribute("#DFE1E5", null, false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Comments // Doc comment // Tag", "Tag", List.of("Comments", "Doc comment"),
                    new ColorAttribute("#CF8E6D", null, false, true),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Comments // Doc comment // Tag value", "Tag value", List.of("Comments", "Doc comment"),
                    new ColorAttribute("#DFE1E5", null, false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Comments // Doc comment // Text", "Text", List.of("Comments", "Doc comment"),
                    new ColorAttribute("#549159", null, false, true),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Comments // Doc comment // Vertical guide for rendered view", "Vertical guide for rendered view", List.of("Comments", "Doc comment"),
                    new ColorAttribute("#393B40", null, false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Comments // Line comment", "Line comment", List.of("Comments"),
                    new ColorAttribute("#7A7E85", null, false, true),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),

            // Identifiers
            new AttributesDescriptor("Identifiers // Constant", "Constant", List.of("Identifiers"),
                    new ColorAttribute("#C77DBB", null, false, true),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Identifiers // Default", "Default", List.of("Identifiers"),
                    new ColorAttribute("#BCBEC4", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Identifiers // Function call", "Function call", List.of("Identifiers"),
                    new ColorAttribute("#56A8F5", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Identifiers // Function declaration", "Function declaration", List.of("Identifiers"),
                    new ColorAttribute("#56A8F5", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Identifiers // Global variable", "Global variable", List.of("Identifiers"),
                    new ColorAttribute("#BCBEC4", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Identifiers // Label", "Label", List.of("Identifiers"),
                    new ColorAttribute("#BCBEC4", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Identifiers // Local variable", "Local variable", List.of("Identifiers"),
                    new ColorAttribute("#BCBEC4", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Identifiers // Parameter", "Parameter", List.of("Identifiers"),
                    new ColorAttribute("#BCBEC4", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Identifiers // Predefined symbol", "Predefined symbol", List.of("Identifiers"),
                    new ColorAttribute("#B3AE60", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),

            // Inline hints
            new AttributesDescriptor("Inline hints // Code lens", "Code lens", List.of("Inline hints"),
                    new ColorAttribute("#70727B", null, false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Inline hints // Current parameter", "Current parameter", List.of("Inline hints"),
                    new ColorAttribute("#DFE1E5", "#2B2D30", false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Inline hints // Parameter hint", "Parameter hint", List.of("Inline hints"),
                    new ColorAttribute("#70727B", "#2B2D30", false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),

            // Keyword
            new AttributesDescriptor("Keyword", "Keyword", List.of(),
                    new ColorAttribute("#CF8E6D", null, false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),

            // Markup
            new AttributesDescriptor("Markup // Entity", "Entity", List.of("Markup"),
                    new ColorAttribute("#CF8E6D", null, false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Markup // Tag", "Tag", List.of("Markup"),
                    new ColorAttribute("#DFE1E5", null, false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Markup // Tag attribute", "Tag attribute", List.of("Markup"),
                    new ColorAttribute("#BCBEC4", null, false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),

            // Metadata
            new AttributesDescriptor("Metadata", "Metadata", List.of(),
                    new ColorAttribute("#B3AE60", null, false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),

            // Number
            new AttributesDescriptor("Number", "Number", List.of(),
                    new ColorAttribute("#2AACB8", null, false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),

            // Semantic highlighting
            new AttributesDescriptor("Semantic highlighting", "Semantic highlighting", List.of(),
                    new ColorAttribute("#4CD98B", null, false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),

            // String
            new AttributesDescriptor("String // Escape sequence", "Escape sequence", List.of("String"),
                    new ColorAttribute("#CF8E6D", null, true, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("String // Invalid escape sequence", "Invalid escape sequence", List.of("String"),
                    new ColorAttribute("#F75464", null, null, EffectType.UNDERWAVED, "#F75464", false, false),
                    null, null, true, true, true, true, EffectType.UNDERWAVED, true),
            new AttributesDescriptor("String // String text", "String text", List.of("String"),
                    new ColorAttribute("#6AAB73", null, false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),

            // Template language
            new AttributesDescriptor("Template language", "Template language", List.of(),
                    new ColorAttribute(null, "#2B3838", false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true)
    );

    private static final List<AttributesDescriptor> CONSOLE_COLORS_DESCRIPTORS = List.of(
            // ANSI colors
            new AttributesDescriptor("ANSI colors // Black", "Black", List.of("ANSI colors"),
                    new ColorAttribute("#000000", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("ANSI colors // Blue", "Blue", List.of("ANSI colors"),
                    new ColorAttribute("#3574F0", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("ANSI colors // Bright Black", "Bright Black", List.of("ANSI colors"),
                    new ColorAttribute("#595959", "#424242", false, false),
                    "ANSI colors // White (Gray)", "(Console Colors)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("ANSI colors // Bright Blue", "Bright Blue", List.of("ANSI colors"),
                    new ColorAttribute("#3574F0", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("ANSI colors // Bright Cyan", "Bright Cyan", List.of("ANSI colors"),
                    new ColorAttribute("#22B4D6", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("ANSI colors // Bright Green", "Bright Green", List.of("ANSI colors"),
                    new ColorAttribute("#59A869", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("ANSI colors // Bright Magenta", "Bright Magenta", List.of("ANSI colors"),
                    new ColorAttribute("#C77DBB", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("ANSI colors // Bright Red", "Bright Red", List.of("ANSI colors"),
                    new ColorAttribute("#F75464", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("ANSI colors // Bright White", "Bright White", List.of("ANSI colors"),
                    new ColorAttribute("#FFFFFF", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("ANSI colors // Bright Yellow", "Bright Yellow", List.of("ANSI colors"),
                    new ColorAttribute("#F5D259", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("ANSI colors // Cyan", "Cyan", List.of("ANSI colors"),
                    new ColorAttribute("#22B4D6", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("ANSI colors // Green", "Green", List.of("ANSI colors"),
                    new ColorAttribute("#59A869", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("ANSI colors // Magenta", "Magenta", List.of("ANSI colors"),
                    new ColorAttribute("#C77DBB", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("ANSI colors // Red", "Red", List.of("ANSI colors"),
                    new ColorAttribute("#F75464", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("ANSI colors // White (Gray)", "White (Gray)", List.of("ANSI colors"),
                    new ColorAttribute("#DFE1E5", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("ANSI colors // Yellow", "Yellow", List.of("ANSI colors"),
                    new ColorAttribute("#F5D259", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),

            // Console
            new AttributesDescriptor("Console // Background", "Background", List.of("Console"),
                    new ColorAttribute(null, "#1E1F22", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Console // Error output", "Error output", List.of("Console"),
                    new ColorAttribute("#F75464", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Console // Standard output", "Standard output", List.of("Console"),
                    new ColorAttribute("#DFE1E5", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Console // System output", "System output", List.of("Console"),
                    new ColorAttribute("#BCBEC4", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Console // User input", "User input", List.of("Console"),
                    new ColorAttribute("#59A869", null, true, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),

            // Log console
            new AttributesDescriptor("Log console // Debug", "Debug", List.of("Log console"),
                    new ColorAttribute("#22B4D6", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Log console // Error", "Error", List.of("Log console"),
                    new ColorAttribute("#F75464", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Log console // Expired entry", "Expired entry", List.of("Log console"),
                    new ColorAttribute("#6F737A", null, "#6F737A", EffectType.STRIKEOUT, "#6F737A", false, false),
                    null, null, true, true, true, true, EffectType.STRIKEOUT, true),
            new AttributesDescriptor("Log console // Info", "Info", List.of("Log console"),
                    new ColorAttribute("#F5D259", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Log console // Verbose", "Verbose", List.of("Log console"),
                    new ColorAttribute("#3574F0", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Log console // Warning", "Warning", List.of("Log console"),
                    new ColorAttribute("#E59E37", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),

            // Reworked terminal
            new AttributesDescriptor("Reworked terminal // Black", "Black", List.of("Reworked terminal"),
                    new ColorAttribute("#000000", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Blue", "Blue", List.of("Reworked terminal"),
                    new ColorAttribute("#3574F0", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Bright Black", "Bright Black", List.of("Reworked terminal"),
                    new ColorAttribute("#4E5157", "#4E5157", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Bright Blue", "Bright Blue", List.of("Reworked terminal"),
                    new ColorAttribute("#3574F0", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Bright Cyan", "Bright Cyan", List.of("Reworked terminal"),
                    new ColorAttribute("#22B4D6", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Bright Green", "Bright Green", List.of("Reworked terminal"),
                    new ColorAttribute("#59A869", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Bright Magenta", "Bright Magenta", List.of("Reworked terminal"),
                    new ColorAttribute("#C77DBB", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Bright Red", "Bright Red", List.of("Reworked terminal"),
                    new ColorAttribute("#F75464", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Bright White", "Bright White", List.of("Reworked terminal"),
                    new ColorAttribute("#FFFFFF", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Bright Yellow", "Bright Yellow", List.of("Reworked terminal"),
                    new ColorAttribute("#F5D259", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Command", "Command", List.of("Reworked terminal"),
                    new ColorAttribute(null, null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Current search entry", "Current search entry", List.of("Reworked terminal"),
                    new ColorAttribute(null, "#32593D", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Cyan", "Cyan", List.of("Reworked terminal"),
                    new ColorAttribute("#22B4D6", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Default background", "Default background", List.of("Reworked terminal"),
                    new ColorAttribute(null, "#1E1F22", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Default foreground", "Default foreground", List.of("Reworked terminal"),
                    new ColorAttribute("#DFE1E5", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Error block border", "Error block border", List.of("Reworked terminal"),
                    new ColorAttribute(null, null, null, EffectType.BORDERED, "#F75464", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Generate command caret color", "Generate command caret color", List.of("Reworked terminal"),
                    new ColorAttribute("#FFFFFF", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Generate command placeholder foreground", "Generate command placeholder foreground", List.of("Reworked terminal"),
                    new ColorAttribute("#868991", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Generate command prompt text", "Generate command prompt text", List.of("Reworked terminal"),
                    new ColorAttribute("#DFE1E5", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Green", "Green", List.of("Reworked terminal"),
                    new ColorAttribute("#59A869", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Inactive selected block background", "Inactive selected block background", List.of("Reworked terminal"),
                    new ColorAttribute(null, "#2B2D30", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Inactive selected block border", "Inactive selected block border", List.of("Reworked terminal"),
                    new ColorAttribute(null, null, null, EffectType.BORDERED, "#393B40", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Magenta", "Magenta", List.of("Reworked terminal"),
                    new ColorAttribute("#C77DBB", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Prompt separator color", "Prompt separator color", List.of("Reworked terminal"),
                    new ColorAttribute("#393B40", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Red", "Red", List.of("Reworked terminal"),
                    new ColorAttribute("#F75464", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Reworked background gradient end", "Reworked background gradient end", List.of("Reworked terminal"),
                    new ColorAttribute(null, "#1E1F22", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Reworked background gradient start", "Reworked background gradient start", List.of("Reworked terminal"),
                    new ColorAttribute(null, "#2B2D30", null, EffectType.UNDERSCORED, "#DFE1E5", false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Reworked terminal // Reworked hovered background gradient end", "Reworked hovered background gradient end", List.of("Reworked terminal"),
                    new ColorAttribute(null, "#24262B", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Reworked hovered background gradient start", "Reworked hovered background gradient start", List.of("Reworked terminal"),
                    new ColorAttribute(null, "#313438", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Search entry", "Search entry", List.of("Reworked terminal"),
                    new ColorAttribute(null, "#2E436E", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Selected block background", "Selected block background", List.of("Reworked terminal"),
                    new ColorAttribute(null, "#2E436E", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Selected block border", "Selected block border", List.of("Reworked terminal"),
                    new ColorAttribute(null, null, null, EffectType.BORDERED, "#3574F0", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // White", "White", List.of("Reworked terminal"),
                    new ColorAttribute("#DFE1E5", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Reworked terminal // Yellow", "Yellow", List.of("Reworked terminal"),
                    new ColorAttribute("#F5D259", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),

            // Terminal
            new AttributesDescriptor("Terminal // Command to run using IDE", "Command to run using IDE", List.of("Terminal"),
                    new ColorAttribute(null, "#40503C", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Terminal // Black", "Black", List.of("Terminal"),
                    new ColorAttribute("#000000", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Terminal // Blue", "Blue", List.of("Terminal"),
                    new ColorAttribute("#3574F0", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Terminal // Cyan", "Cyan", List.of("Terminal"),
                    new ColorAttribute("#22B4D6", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Terminal // Green", "Green", List.of("Terminal"),
                    new ColorAttribute("#59A869", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Terminal // Magenta", "Magenta", List.of("Terminal"),
                    new ColorAttribute("#C77DBB", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Terminal // Red", "Red", List.of("Terminal"),
                    new ColorAttribute("#F75464", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Terminal // White", "White", List.of("Terminal"),
                    new ColorAttribute("#DFE1E5", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Terminal // Yellow", "Yellow", List.of("Terminal"),
                    new ColorAttribute("#F5D259", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true)
    );

    private static final List<AttributesDescriptor> CODE_WITH_ME_DESCRIPTORS = List.of(
            new AttributesDescriptor("User 1 cursor", "User 1 cursor", List.of(),
                    new ColorAttribute("#59A869", "#59A869", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("User 1 selection", "User 1 selection", List.of(),
                    new ColorAttribute(null, "#2D4733", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("User 2 cursor", "User 2 cursor", List.of(),
                    new ColorAttribute("#F75464", "#F75464", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("User 2 selection", "User 2 selection", List.of(),
                    new ColorAttribute(null, "#5E3838", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("User 3 cursor", "User 3 cursor", List.of(),
                    new ColorAttribute("#22B4D6", "#22B4D6", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("User 3 selection", "User 3 selection", List.of(),
                    new ColorAttribute(null, "#204A57", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("User 4 cursor", "User 4 cursor", List.of(),
                    new ColorAttribute("#C77DBB", "#C77DBB", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("User 4 selection", "User 4 selection", List.of(),
                    new ColorAttribute(null, "#4C2D50", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("User 5 cursor", "User 5 cursor", List.of(),
                    new ColorAttribute("#F5D259", "#F5D259", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("User 5 selection", "User 5 selection", List.of(),
                    new ColorAttribute(null, "#544820", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("User 6 cursor", "User 6 cursor", List.of(),
                    new ColorAttribute("#3574F0", "#3574F0", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("User 6 selection", "User 6 selection", List.of(),
                    new ColorAttribute(null, "#24395E", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true)
    );

    private static final List<AttributesDescriptor> DEBUGGER_DESCRIPTORS = List.of(
            new AttributesDescriptor("Breakpoint line", "Breakpoint line", List.of(),
                    new ColorAttribute(null, "#3A2323", "#DB5860", EffectType.NONE, null, false, false),
                    null, null, false, true, true, true, EffectType.BORDERED, false),
            new AttributesDescriptor("Evaluated expression text", "Evaluated expression text", List.of(),
                    new ColorAttribute("#868991", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Evaluated expression text for execution line", "Evaluated expression text for execution line", List.of(),
                    new ColorAttribute("#868991", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Execution point", "Execution point", List.of(),
                    new ColorAttribute(null, "#253B2F", "#3B7E58", EffectType.NONE, null, false, false),
                    null, null, false, true, true, true, EffectType.BORDERED, false),
            new AttributesDescriptor("Inline stack frames", "Inline stack frames", List.of(),
                    new ColorAttribute("#70727B", null, false, true),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Inlined modified values", "Inlined modified values", List.of(),
                    new ColorAttribute("#B2AE60", null, false, true),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Inlined values", "Inlined values", List.of(),
                    new ColorAttribute("#868991", null, false, true),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Inlined values for execution line", "Inlined values for execution line", List.of(),
                    new ColorAttribute("#868991", null, false, true),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Not top frame", "Not top frame", List.of(),
                    new ColorAttribute(null, "#2D3238", false, false),
                    null, null, false, true, true, true, EffectType.BORDERED, false),
            new AttributesDescriptor("Smart step into selection", "Smart step into selection", List.of(),
                    new ColorAttribute(null, "#223C4D", false, false),
                    null, null, false, true, true, true, EffectType.BORDERED, false),
            new AttributesDescriptor("Smart step into target", "Smart step into target", List.of(),
                    new ColorAttribute(null, "#2E436E", false, false),
                    null, null, false, true, true, true, EffectType.BORDERED, false)
    );

    private static final List<AttributesDescriptor> DIFF_MERGE_DESCRIPTORS = List.of(
            new AttributesDescriptor("Changed lines // Changed", "Changed", List.of("Changed lines"),
                    new ColorAttribute(null, "#385570", "#436980", EffectType.NONE, null, false, false),
                    null, null, true, true, true, false, EffectType.NONE, false),
            new AttributesDescriptor("Changed lines // Conflict", "Conflict", List.of("Changed lines"),
                    new ColorAttribute(null, "#4D3838", "#8F4545", EffectType.NONE, null, false, false),
                    null, null, true, true, true, false, EffectType.NONE, false),
            new AttributesDescriptor("Changed lines // Deleted", "Deleted", List.of("Changed lines"),
                    new ColorAttribute(null, "#454A4D", "#656E73", EffectType.NONE, null, false, false),
                    null, null, true, true, true, false, EffectType.NONE, false),
            new AttributesDescriptor("Changed lines // Inserted", "Inserted", List.of("Changed lines"),
                    new ColorAttribute(null, "#2E5938", "#3B7E58", EffectType.NONE, null, false, false),
                    null, null, true, true, true, false, EffectType.NONE, false),
            new AttributesDescriptor("Folded unchanged fragments // Wave", "Wave", List.of("Folded unchanged fragments"),
                    new ColorAttribute("#5C616B", null, false, false),
                    null, null, true, false, false, false, EffectType.NONE, false)
    );

    private static final List<AttributesDescriptor> JVM_LOGGING_DESCRIPTORS = List.of(
            new AttributesDescriptor("Classes // Class name", "Class name", List.of("Classes"),
                    new ColorAttribute(null, null, null, EffectType.DOTTED_LINE, "#888880", false, false),
                    null, null, true, true, true, true, EffectType.DOTTED_LINE, true),
            new AttributesDescriptor("Log string // Placeholder", "Placeholder", List.of("Log string"),
                    inheritAttr("#CF8E6D", null, "String // Escape sequence", "(Language Defaults)"),
                    "String // Escape sequence", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true)
    );

    private static final List<AttributesDescriptor> USER_DEFINED_FILE_TYPES_DESCRIPTORS = List.of(
            new AttributesDescriptor("Block comment", "Block comment", List.of(),
                    new ColorAttribute("#7A7E85", null, false, true),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Invalid string escape", "Invalid string escape", List.of(),
                    new ColorAttribute("#F75464", null, null, EffectType.UNDERWAVED, "#F75464", false, false),
                    null, null, true, true, true, true, EffectType.UNDERWAVED, true),
            new AttributesDescriptor("Keyword1", "Keyword1", List.of(),
                    new ColorAttribute("#CF8E6D", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Keyword2", "Keyword2", List.of(),
                    new ColorAttribute("#C77DBB", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Keyword3", "Keyword3", List.of(),
                    new ColorAttribute("#56A8F5", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Keyword4", "Keyword4", List.of(),
                    new ColorAttribute("#59A869", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Line comment", "Line comment", List.of(),
                    new ColorAttribute("#7A7E85", null, false, true),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Number", "Number", List.of(),
                    new ColorAttribute("#2AACB8", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("String", "String", List.of(),
                    new ColorAttribute("#6AAB73", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Valid string escape", "Valid string escape", List.of(),
                    new ColorAttribute("#CF8E6D", null, true, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true)
    );

    private static final List<AttributesDescriptor> VCS_DESCRIPTORS = List.of(
            // Editor Gutter (media_1790425840060.png)
            new AttributesDescriptor("Editor Gutter // Added ignored lines border", "Added ignored lines border", List.of("Editor Gutter"),
                    new ColorAttribute(null, null, "#3C4E42", EffectType.BORDERED, "#3C4E42", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, false),
            new AttributesDescriptor("Editor Gutter // Added lines", "Added lines", List.of("Editor Gutter"),
                    new ColorAttribute(null, "#385E39", "#43698D", EffectType.NONE, null, false, false),
                    null, null, true, true, true, true, EffectType.NONE, false),
            new AttributesDescriptor("Editor Gutter // Border", "Border", List.of("Editor Gutter"),
                    new ColorAttribute(null, "#393B40", null, EffectType.NONE, null, false, false),
                    null, null, true, true, true, true, EffectType.NONE, false),
            new AttributesDescriptor("Editor Gutter // Changed lines popup", "Changed lines popup", List.of("Editor Gutter"),
                    new ColorAttribute(null, "#2B2D30", null, EffectType.UNDERSCORED, null, false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, false),
            new AttributesDescriptor("Editor Gutter // Deleted ignored lines border", "Deleted ignored lines border", List.of("Editor Gutter"),
                    new ColorAttribute(null, null, "#544040", EffectType.BORDERED, "#544040", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, false),
            new AttributesDescriptor("Editor Gutter // Deleted lines", "Deleted lines", List.of("Editor Gutter"),
                    new ColorAttribute(null, "#6E3B3B", "#734545", EffectType.NONE, null, false, false),
                    null, null, true, true, true, true, EffectType.NONE, false),
            new AttributesDescriptor("Editor Gutter // Modified ignored lines border", "Modified ignored lines border", List.of("Editor Gutter"),
                    new ColorAttribute(null, null, "#3D4C59", EffectType.BORDERED, "#3D4C59", false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, false),
            new AttributesDescriptor("Editor Gutter // Modified lines", "Modified lines", List.of("Editor Gutter"),
                    new ColorAttribute(null, "#385570", "#436980", EffectType.NONE, null, false, false),
                    null, null, true, true, true, true, EffectType.NONE, false),
            new AttributesDescriptor("Editor Gutter // Whitespace-modified lines", "Whitespace-modified lines", List.of("Editor Gutter"),
                    new ColorAttribute(null, "#4E535E", "#5C616B", EffectType.NONE, null, false, false),
                    null, null, true, true, true, true, EffectType.NONE, false),

            // VCS Annotations (media_1790425840060.png)
            new AttributesDescriptor("VCS Annotations // Background color #1", "Background color #1", List.of("VCS Annotations"),
                    new ColorAttribute(null, "#23382B", false, false),
                    null, null, true, true, true, true, EffectType.NONE, false),
            new AttributesDescriptor("VCS Annotations // Background color #2", "Background color #2", List.of("VCS Annotations"),
                    new ColorAttribute(null, "#273548", false, false),
                    null, null, true, true, true, true, EffectType.NONE, false),
            new AttributesDescriptor("VCS Annotations // Background color #3", "Background color #3", List.of("VCS Annotations"),
                    new ColorAttribute(null, "#3D382B", false, false),
                    null, null, true, true, true, true, EffectType.NONE, false),
            new AttributesDescriptor("VCS Annotations // Background color #4", "Background color #4", List.of("VCS Annotations"),
                    new ColorAttribute(null, "#3C2B38", false, false),
                    null, null, true, true, true, true, EffectType.NONE, false),
            new AttributesDescriptor("VCS Annotations // Background color #5", "Background color #5", List.of("VCS Annotations"),
                    new ColorAttribute(null, "#2B3B3C", false, false),
                    null, null, true, true, true, true, EffectType.NONE, false),
            new AttributesDescriptor("VCS Annotations // Foreground", "Foreground", List.of("VCS Annotations"),
                    new ColorAttribute("#868A91", null, false, false),
                    null, null, true, true, true, true, EffectType.NONE, false),
            new AttributesDescriptor("VCS Annotations // Foreground for last commit", "Foreground for last commit", List.of("VCS Annotations"),
                    new ColorAttribute("#DFE1E5", null, true, false),
                    null, null, true, true, true, true, EffectType.NONE, false)
    );

    private static final List<AttributesDescriptor> JAVA_DESCRIPTORS = List.of(
            // Annotations (media_1790425875746.png)
            new AttributesDescriptor("Annotations // Annotation attribute name", "Annotation attribute name", List.of("Annotations"),
                    inheritAttr("#BCBEC4", null, "Metadata", "(Language Defaults)"),
                    "Metadata", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Annotations // Annotation name", "Annotation name", List.of("Annotations"),
                    inheritAttr("#B3AF60", null, "Metadata", "(Language Defaults)"),
                    "Metadata", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),

            // Braces and Operators (media_1790425905497.png)
            new AttributesDescriptor("Braces and Operators // Braces", "Braces", List.of("Braces and Operators"),
                    inheritAttr("#BCBEC4", null, "Braces and Operators->Braces", "(Language Defaults)"),
                    "Braces and Operators->Braces", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Braces and Operators // Brackets", "Brackets", List.of("Braces and Operators"),
                    inheritAttr("#BCBEC4", null, "Braces and Operators->Brackets", "(Language Defaults)"),
                    "Braces and Operators->Brackets", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Braces and Operators // Comma", "Comma", List.of("Braces and Operators"),
                    inheritAttr("#BCBEC4", null, "Braces and Operators->Comma", "(Language Defaults)"),
                    "Braces and Operators->Comma", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Braces and Operators // Dot", "Dot", List.of("Braces and Operators"),
                    inheritAttr("#BCBEC4", null, "Braces and Operators->Dot", "(Language Defaults)"),
                    "Braces and Operators->Dot", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Braces and Operators // Operator sign", "Operator sign", List.of("Braces and Operators"),
                    inheritAttr("#BCBEC4", null, "Braces and Operators->Operation sign", "(Language Defaults)"),
                    "Braces and Operators->Operation sign", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Braces and Operators // Parentheses", "Parentheses", List.of("Braces and Operators"),
                    inheritAttr("#BCBEC4", null, "Braces and Operators->Parentheses", "(Language Defaults)"),
                    "Braces and Operators->Parentheses", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Braces and Operators // Semicolon", "Semicolon", List.of("Braces and Operators"),
                    inheritAttr("#BCBEC4", null, "Braces and Operators->Semicolon", "(Language Defaults)"),
                    "Braces and Operators->Semicolon", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),

            // Class Fields (media_1790425922623.png)
            new AttributesDescriptor("Class Fields // Constant (static final field)", "Constant (static final field)", List.of("Class Fields"),
                    inheritAttr("#C77DBB", null, "Identifiers->Constant", "(Language Defaults)", false),
                    "Identifiers->Constant", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Class Fields // Constant (static final imported field)", "Constant (static final imported field)", List.of("Class Fields"),
                    inheritAttr("#C77DBB", null, "Identifiers->Constant", "(Language Defaults)", false),
                    "Identifiers->Constant", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Class Fields // Instance field", "Instance field", List.of("Class Fields"),
                    inheritAttr("#C77DBB", null, "Classes->Instance field", "(Language Defaults)"),
                    "Classes->Instance field", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Class Fields // Instance final field", "Instance final field", List.of("Class Fields"),
                    inheritAttr("#C77DBB", null, "Classes->Instance field", "(Language Defaults)"),
                    "Classes->Instance field", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Class Fields // Record component", "Record component", List.of("Class Fields"),
                    inheritAttr("#C77DBB", null, "Classes->Instance field", "(Language Defaults)"),
                    "Classes->Instance field", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Class Fields // Static field", "Static field", List.of("Class Fields"),
                    inheritAttr("#C77DBB", null, "Classes->Static field", "(Language Defaults)"),
                    "Classes->Static field", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Class Fields // Static imported field", "Static imported field", List.of("Class Fields"),
                    inheritAttr("#C77DBB", null, "Classes->Static field", "(Language Defaults)"),
                    "Classes->Static field", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),

            // Classes and Interfaces (media_1790425951849.png)
            new AttributesDescriptor("Classes and Interfaces // Abstract class", "Abstract class", List.of("Classes and Interfaces"),
                    inheritAttr("#BCBEC4", null, "Classes->Class name", "(Language Defaults)"),
                    "Classes->Class name", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Classes and Interfaces // Anonymous class", "Anonymous class", List.of("Classes and Interfaces"),
                    inheritAttr("#BCBEC4", null, "Classes->Class name", "(Language Defaults)"),
                    "Classes->Class name", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Classes and Interfaces // Class", "Class", List.of("Classes and Interfaces"),
                    inheritAttr("#BCBEC4", null, "Classes->Class name", "(Language Defaults)"),
                    "Classes->Class name", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Classes and Interfaces // Enum", "Enum", List.of("Classes and Interfaces"),
                    inheritAttr("#BCBEC4", null, "Classes->Class name", "(Language Defaults)"),
                    "Classes->Class name", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Classes and Interfaces // Interface", "Interface", List.of("Classes and Interfaces"),
                    inheritAttr("#BCBEC4", null, "Classes->Interface name", "(Language Defaults)"),
                    "Classes->Interface name", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Classes and Interfaces // Record", "Record", List.of("Classes and Interfaces"),
                    inheritAttr("#BCBEC4", null, "Classes->Class name", "(Language Defaults)"),
                    "Classes->Class name", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Classes and Interfaces // Type parameter", "Type parameter", List.of("Classes and Interfaces"),
                    inheritAttr("#16B3AC", null, "Classes->Type parameter", "(Language Defaults)"),
                    "Classes->Type parameter", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),

            // Comments (media_1790425951849.png)
            new AttributesDescriptor("Comments // Block comment", "Block comment", List.of("Comments"),
                    inheritAttr("#7A7E85", null, "Comments->Block comment", "(Language Defaults)"),
                    "Comments->Block comment", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Comments // JavaDoc // Markup", "Markup", List.of("Comments", "JavaDoc"),
                    inheritAttr("#56A8F5", null, "Comments->Doc comment->Markup", "(Language Defaults)"),
                    "Comments->Doc comment->Markup", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Comments // JavaDoc // Tag", "Tag", List.of("Comments", "JavaDoc"),
                    inheritAttr("#56A8F5", null, "Comments->Doc comment->Tag", "(Language Defaults)"),
                    "Comments->Doc comment->Tag", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Comments // JavaDoc // Tag value", "Tag value", List.of("Comments", "JavaDoc"),
                    inheritAttr("#BCBEC4", null, "Comments->Doc comment->Tag value", "(Language Defaults)"),
                    "Comments->Doc comment->Tag value", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Comments // JavaDoc // Text", "Text", List.of("Comments", "JavaDoc"),
                    inheritAttr("#7A7E85", null, "Comments->Doc comment->Text", "(Language Defaults)"),
                    "Comments->Doc comment->Text", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Comments // Line comment", "Line comment", List.of("Comments"),
                    inheritAttr("#7A7E85", null, "Comments->Line comment", "(Language Defaults)"),
                    "Comments->Line comment", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),

            // Keyword (media_1790425875746.png)
            new AttributesDescriptor("Keyword", "Keyword", List.of(),
                    inheritAttr("#CF8E6D", null, "Keyword", "(Language Defaults)"),
                    "Keyword", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),

            // Methods
            new AttributesDescriptor("Methods // Constructor call", "Constructor call", List.of("Methods"),
                    inheritAttr("#56A8F5", null, "Functions and Methods->Method call", "(Language Defaults)"),
                    "Functions and Methods->Method call", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Methods // Constructor declaration", "Constructor declaration", List.of("Methods"),
                    inheritAttr("#56A8F5", null, "Functions and Methods->Method declaration", "(Language Defaults)"),
                    "Functions and Methods->Method declaration", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Methods // Instance method call", "Instance method call", List.of("Methods"),
                    inheritAttr("#56A8F5", null, "Classes->Instance method", "(Language Defaults)"),
                    "Classes->Instance method", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Methods // Instance method declaration", "Instance method declaration", List.of("Methods"),
                    inheritAttr("#56A8F5", null, "Functions and Methods->Method declaration", "(Language Defaults)"),
                    "Functions and Methods->Method declaration", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Methods // Static method call", "Static method call", List.of("Methods"),
                    inheritAttr("#56A8F5", null, "Classes->Static method", "(Language Defaults)"),
                    "Classes->Static method", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Methods // Static method declaration", "Static method declaration", List.of("Methods"),
                    inheritAttr("#56A8F5", null, "Functions and Methods->Static method", "(Language Defaults)"),
                    "Functions and Methods->Static method", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),

            // Number
            new AttributesDescriptor("Number", "Number", List.of(),
                    inheritAttr("#2AACB8", null, "Number", "(Language Defaults)"),
                    "Number", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),

            // Parameters
            new AttributesDescriptor("Parameters // Implicit anonymous class parameter", "Implicit anonymous class parameter", List.of("Parameters"),
                    inheritAttr("#BCBEC4", null, "Identifiers->Parameter", "(Language Defaults)"),
                    "Identifiers->Parameter", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Parameters // Method call arguments", "Method call arguments", List.of("Parameters"),
                    inheritAttr("#BCBEC4", null, "Identifiers->Parameter", "(Language Defaults)"),
                    "Identifiers->Parameter", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Parameters // Parameter", "Parameter", List.of("Parameters"),
                    inheritAttr("#BCBEC4", null, "Identifiers->Parameter", "(Language Defaults)"),
                    "Identifiers->Parameter", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),

            // Semantic highlighting
            new AttributesDescriptor("Semantic highlighting", "Semantic highlighting", List.of(),
                    inheritAttr("#4CD98B", null, "Semantic highlighting", "(Language Defaults)"),
                    "Semantic highlighting", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),

            // String
            new AttributesDescriptor("String // Escape sequence // Invalid", "Invalid", List.of("String", "Escape sequence"),
                    inheritAttr("#F75464", null, "String->Invalid escape sequence", "(Language Defaults)"),
                    "String->Invalid escape sequence", "(Language Defaults)",
                    true, true, true, true, EffectType.UNDERWAVED, true),
            new AttributesDescriptor("String // Escape sequence // Valid", "Valid", List.of("String", "Escape sequence"),
                    inheritAttr("#CF8E6D", null, "String->Escape sequence", "(Language Defaults)"),
                    "String->Escape sequence", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("String // String text", "String text", List.of("String"),
                    inheritAttr("#6AAB73", null, "String->String text", "(Language Defaults)"),
                    "String->String text", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),

            // Variables
            new AttributesDescriptor("Variables // Implicit parameter", "Implicit parameter", List.of("Variables"),
                    inheritAttr("#BCBEC4", null, "Identifiers->Local variable", "(Language Defaults)"),
                    "Identifiers->Local variable", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Variables // Local variable", "Local variable", List.of("Variables"),
                    inheritAttr("#BCBEC4", null, "Identifiers->Local variable", "(Language Defaults)"),
                    "Identifiers->Local variable", "(Language Defaults)",
                    true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Variables // Reassigned local variable", "Reassigned local variable", List.of("Variables"),
                    new ColorAttribute("#BCBEC4", null, null, EffectType.UNDERSCORED, "#707D8B", false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),
            new AttributesDescriptor("Variables // Reassigned parameter", "Reassigned parameter", List.of("Variables"),
                    new ColorAttribute("#BCBEC4", null, null, EffectType.UNDERSCORED, "#707D8B", false, false),
                    null, null, true, true, true, true, EffectType.UNDERSCORED, true),

            // Visibility
            new AttributesDescriptor("Visibility // Package private", "Package private", List.of("Visibility"),
                    new ColorAttribute("#BCBEC4", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Visibility // Private", "Private", List.of("Visibility"),
                    new ColorAttribute("#BCBEC4", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Visibility // Protected", "Protected", List.of("Visibility"),
                    new ColorAttribute("#BCBEC4", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true),
            new AttributesDescriptor("Visibility // Public", "Public", List.of("Visibility"),
                    new ColorAttribute("#BCBEC4", null, false, false),
                    null, null, true, true, true, true, EffectType.BORDERED, true)
    );

    private static final Map<String, AttributesDescriptor> DESCRIPTORS_BY_KEY;
    static {
        Map<String, AttributesDescriptor> map = new LinkedHashMap<>();
        for (AttributesDescriptor desc : GENERAL_DESCRIPTORS) {
            map.put(desc.getKey(), desc);
        }
        for (AttributesDescriptor desc : LANGUAGE_DEFAULTS_DESCRIPTORS) {
            map.put(desc.getKey(), desc);
        }
        for (AttributesDescriptor desc : CONSOLE_COLORS_DESCRIPTORS) {
            map.put(desc.getKey(), desc);
        }
        for (AttributesDescriptor desc : CODE_WITH_ME_DESCRIPTORS) {
            map.put(desc.getKey(), desc);
        }
        for (AttributesDescriptor desc : DEBUGGER_DESCRIPTORS) {
            map.put(desc.getKey(), desc);
        }
        for (AttributesDescriptor desc : DIFF_MERGE_DESCRIPTORS) {
            map.put(desc.getKey(), desc);
        }
        for (AttributesDescriptor desc : JVM_LOGGING_DESCRIPTORS) {
            map.put(desc.getKey(), desc);
        }
        for (AttributesDescriptor desc : USER_DEFINED_FILE_TYPES_DESCRIPTORS) {
            map.put(desc.getKey(), desc);
        }
        for (AttributesDescriptor desc : VCS_DESCRIPTORS) {
            map.put(desc.getKey(), desc);
        }
        for (AttributesDescriptor desc : JAVA_DESCRIPTORS) {
            map.put(desc.getKey(), desc);
        }
        DESCRIPTORS_BY_KEY = Collections.unmodifiableMap(map);
    }

    public static List<AttributesDescriptor> getGeneralDescriptors() {
        return GENERAL_DESCRIPTORS;
    }

    public static List<AttributesDescriptor> getLanguageDefaultsDescriptors() {
        return LANGUAGE_DEFAULTS_DESCRIPTORS;
    }

    public static List<AttributesDescriptor> getConsoleColorsDescriptors() {
        return CONSOLE_COLORS_DESCRIPTORS;
    }

    public static List<AttributesDescriptor> getCodeWithMeDescriptors() {
        return CODE_WITH_ME_DESCRIPTORS;
    }

    public static List<AttributesDescriptor> getDebuggerDescriptors() {
        return DEBUGGER_DESCRIPTORS;
    }

    public static List<AttributesDescriptor> getDiffMergeDescriptors() {
        return DIFF_MERGE_DESCRIPTORS;
    }

    public static List<AttributesDescriptor> getJvmLoggingDescriptors() {
        return JVM_LOGGING_DESCRIPTORS;
    }

    public static List<AttributesDescriptor> getUserDefinedFileTypesDescriptors() {
        return USER_DEFINED_FILE_TYPES_DESCRIPTORS;
    }

    public static List<AttributesDescriptor> getVcsDescriptors() {
        return VCS_DESCRIPTORS;
    }

    public static List<AttributesDescriptor> getJavaDescriptors() {
        return JAVA_DESCRIPTORS;
    }

    public static AttributesDescriptor getDescriptor(String key) {
        if (key == null) return null;
        String norm = normalizeKey(key);
        return DESCRIPTORS_BY_KEY.get(norm);
    }

    public static String normalizeKey(String key) {
        if (key == null) return null;
        switch (key) {
            case "Hyperlinks // Inactive hyperlink": return "Hyperlinks // Inactive";
            case "Hyperlinks // Followed hyperlink": return "Hyperlinks // Followed";
            case "Hyperlinks // Reference hyperlink": return "Hyperlinks // Reference";
            case "Line Coverage // Full coverage": return "Line Coverage // Full";
            case "Line Coverage // Partial coverage": return "Line Coverage // Partial";
            case "Live Templates // Active template": return "Live Templates // Active Segment";
            case "Live Templates // Inactive template": return "Live Templates // Inactive Segment";
            case "Popups and Hints // Parameter hint": return "Popups and Hints // Code lens";
            case "Popups and Hints // Inlay hint": return "Popups and Hints // Information hint";
            case "Preview // Preview scope": return "Preview // Background";
            case "Log console // Expired": return "Log console // Expired entry";
            case "String->Escape sequence->Valid": return "String // Escape sequence";
            case "String // Escape sequence // Valid": return "String // Escape sequence";
            case "Metadata": return "Metadata";
            case "Braces and Operators->Braces": return "Braces and Operators // Braces";
            case "Braces and Operators->Brackets": return "Braces and Operators // Brackets";
            case "Braces and Operators->Comma": return "Braces and Operators // Comma";
            case "Braces and Operators->Dot": return "Braces and Operators // Dot";
            case "Braces and Operators->Operation sign": return "Braces and Operators // Operation sign";
            case "Braces and Operators->Parentheses": return "Braces and Operators // Parentheses";
            case "Braces and Operators->Semicolon": return "Braces and Operators // Semicolon";
            case "Classes->Class name": return "Classes // Class name";
            case "Classes->Instance field": return "Classes // Instance field";
            case "Classes->Interface name": return "Classes // Interface name";
            case "Classes->Static field": return "Classes // Static field";
            case "Classes->Type parameter": return "Classes // Type parameter";
            default: return key;
        }
    }

    private Map<String, ColorAttribute> createIslandsDarkDefaults() {
        Map<String, ColorAttribute> map = new LinkedHashMap<>();
        for (AttributesDescriptor desc : GENERAL_DESCRIPTORS) {
            map.put(desc.getKey(), desc.getDefaultAttribute());
        }
        for (AttributesDescriptor desc : LANGUAGE_DEFAULTS_DESCRIPTORS) {
            map.put(desc.getKey(), desc.getDefaultAttribute());
        }
        for (AttributesDescriptor desc : CONSOLE_COLORS_DESCRIPTORS) {
            map.put(desc.getKey(), desc.getDefaultAttribute());
        }
        for (AttributesDescriptor desc : CODE_WITH_ME_DESCRIPTORS) {
            map.put(desc.getKey(), desc.getDefaultAttribute());
        }
        for (AttributesDescriptor desc : DEBUGGER_DESCRIPTORS) {
            map.put(desc.getKey(), desc.getDefaultAttribute());
        }
        for (AttributesDescriptor desc : DIFF_MERGE_DESCRIPTORS) {
            map.put(desc.getKey(), desc.getDefaultAttribute());
        }
        for (AttributesDescriptor desc : JVM_LOGGING_DESCRIPTORS) {
            map.put(desc.getKey(), desc.getDefaultAttribute());
        }
        for (AttributesDescriptor desc : USER_DEFINED_FILE_TYPES_DESCRIPTORS) {
            map.put(desc.getKey(), desc.getDefaultAttribute());
        }
        for (AttributesDescriptor desc : VCS_DESCRIPTORS) {
            map.put(desc.getKey(), desc.getDefaultAttribute());
        }
        for (AttributesDescriptor desc : JAVA_DESCRIPTORS) {
            map.put(desc.getKey(), desc.getDefaultAttribute());
        }
        return map;
    }

    private Map<String, ColorAttribute> cloneAttributesMap(Map<String, ColorAttribute> source) {
        Map<String, ColorAttribute> copy = new LinkedHashMap<>();
        if (source != null) {
            for (Map.Entry<String, ColorAttribute> entry : source.entrySet()) {
                copy.put(entry.getKey(), entry.getValue() != null ? entry.getValue().clone() : new ColorAttribute());
            }
        }
        return copy;
    }

    public void addListener(Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Listener listener : listeners) {
            try {
                listener.onColorSchemeChanged(this);
            } catch (Throwable ignored) {
            }
        }
    }

    public String getActiveSchemeName() {
        return activeSchemeName;
    }

    public void setActiveSchemeName(String name) {
        if (name != null && !name.isBlank()) {
            this.activeSchemeName = name;
            notifyListeners();
        }
    }

    public List<String> getAvailableSchemes() {
        List<String> list = new ArrayList<>(BUNDLED_SCHEMES);
        for (String custom : customSchemes) {
            if (!list.contains(custom)) {
                list.add(custom);
            }
        }
        return Collections.unmodifiableList(list);
    }

    public List<String> getCustomSchemes() {
        return Collections.unmodifiableList(new ArrayList<>(customSchemes));
    }

    public boolean isCustomScheme(String schemeName) {
        return customSchemes.contains(schemeName);
    }

    public Map<String, ColorAttribute> getSchemeAttributes(String schemeName) {
        Map<String, ColorAttribute> map = schemeAttributes.get(schemeName);
        if (map == null) {
            map = createIslandsDarkDefaults();
            schemeAttributes.put(schemeName, map);
        }
        return map;
    }

    public ColorAttribute getAttribute(String schemeName, String key) {
        String normKey = normalizeKey(key);
        Map<String, ColorAttribute> map = getSchemeAttributes(schemeName);
        ColorAttribute attr = map.get(normKey);
        if (attr == null && !normKey.equals(key)) {
            attr = map.get(key);
        }
        return attr;
    }

    public ColorAttribute resolveAttribute(String schemeName, String key) {
        return resolveAttribute(schemeName, normalizeKey(key), new HashSet<>());
    }

    private ColorAttribute resolveAttribute(String schemeName, String key, Set<String> visited) {
        if (key == null || !visited.add(key)) {
            ColorAttribute raw = getAttribute(schemeName, key);
            return raw != null ? raw : new ColorAttribute();
        }
        ColorAttribute attr = getAttribute(schemeName, key);
        if (attr == null) return new ColorAttribute();
        if (attr.isInherit() && attr.getInheritFrom() != null && !attr.getInheritFrom().isBlank()) {
            String parentKey = normalizeKey(attr.getInheritFrom());
            if (parentKey.equals(key) || visited.contains(parentKey)) {
                return attr;
            }
            ColorAttribute parent = resolveAttribute(schemeName, parentKey, visited);
            ColorAttribute resolved = attr.clone();
            if (resolved.getForeground() == null) {
                resolved.setForeground(parent.getForeground());
            }
            if (resolved.getBackground() == null) {
                resolved.setBackground(parent.getBackground() != null ? parent.getBackground() : parent.getForeground());
            }
            if (resolved.getErrorStripeColor() == null) {
                resolved.setErrorStripeColor(parent.getErrorStripeColor());
            }
            if (resolved.getEffectType() == EffectType.NONE) {
                resolved.setEffectType(parent.getEffectType());
            }
            if (resolved.getEffectColor() == null) {
                resolved.setEffectColor(parent.getEffectColor());
            }
            if (!resolved.isBold()) resolved.setBold(parent.isBold());
            if (!resolved.isItalic()) resolved.setItalic(parent.isItalic());
            return resolved;
        }
        return attr;
    }

    public void setAttribute(String schemeName, String key, ColorAttribute attribute) {
        String normKey = normalizeKey(key);
        Map<String, ColorAttribute> map = getSchemeAttributes(schemeName);
        if (attribute != null) {
            map.put(normKey, attribute.clone());
        } else {
            map.remove(normKey);
        }
        notifyListeners();
    }

    public boolean duplicateScheme(String sourceName, String newName) {
        if (newName == null || newName.isBlank()) return false;
        String trimmed = newName.trim();
        if (BUNDLED_SCHEMES.contains(trimmed) || customSchemes.contains(trimmed)) {
            return false;
        }

        Map<String, ColorAttribute> sourceMap = getSchemeAttributes(sourceName);
        Map<String, ColorAttribute> newMap = cloneAttributesMap(sourceMap);

        customSchemes.add(trimmed);
        schemeAttributes.put(trimmed, newMap);
        defaultSchemeAttributes.put(trimmed, cloneAttributesMap(newMap));
        activeSchemeName = trimmed;

        save();
        notifyListeners();
        return true;
    }

    public boolean restoreDefaults(String schemeName) {
        Map<String, ColorAttribute> defaults = defaultSchemeAttributes.get(schemeName);
        if (defaults == null) {
            defaults = createIslandsDarkDefaults();
        }
        schemeAttributes.put(schemeName, cloneAttributesMap(defaults));
        save();
        notifyListeners();
        return true;
    }

    public boolean isSchemeModified(String schemeName) {
        Map<String, ColorAttribute> current = schemeAttributes.get(schemeName);
        Map<String, ColorAttribute> def = defaultSchemeAttributes.get(schemeName);
        if (current == null || def == null) return false;
        return !current.equals(def);
    }

    public void load() {
        String active = Settings.get("editor.colorscheme.active");
        if (active != null && !active.isBlank()) {
            this.activeSchemeName = active;
        }

        String customStr = Settings.get("editor.colorscheme.custom.names");
        if (customStr != null && !customStr.isBlank()) {
            for (String s : customStr.split(",")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty() && !customSchemes.contains(trimmed)) {
                    customSchemes.add(trimmed);
                    if (!schemeAttributes.containsKey(trimmed)) {
                        Map<String, ColorAttribute> defaults = createIslandsDarkDefaults();
                        schemeAttributes.put(trimmed, defaults);
                        defaultSchemeAttributes.put(trimmed, cloneAttributesMap(defaults));
                    }
                }
            }
        }
    }

    private final Map<String, SchemeFontPreferences> schemeFonts = new ConcurrentHashMap<>();
    private final Map<String, ConsoleFontPreferences> consoleFonts = new ConcurrentHashMap<>();

    public SchemeFontPreferences getSchemeFontPreferences() {
        return getSchemeFontPreferences(activeSchemeName);
    }

    public SchemeFontPreferences getSchemeFontPreferences(String schemeName) {
        String scheme = schemeName != null ? schemeName : activeSchemeName;
        return schemeFonts.computeIfAbsent(scheme, k -> new SchemeFontPreferences()).clone();
    }

    public void setSchemeFontPreferences(SchemeFontPreferences prefs) {
        setSchemeFontPreferences(activeSchemeName, prefs);
    }

    public void setSchemeFontPreferences(String schemeName, SchemeFontPreferences prefs) {
        String scheme = schemeName != null ? schemeName : activeSchemeName;
        schemeFonts.put(scheme, prefs != null ? prefs.clone() : new SchemeFontPreferences());
        notifyListeners();
    }

    public ConsoleFontPreferences getConsoleFontPreferences() {
        return getConsoleFontPreferences(activeSchemeName);
    }

    public ConsoleFontPreferences getConsoleFontPreferences(String schemeName) {
        String scheme = schemeName != null ? schemeName : activeSchemeName;
        return consoleFonts.computeIfAbsent(scheme, k -> new ConsoleFontPreferences()).clone();
    }

    public void setConsoleFontPreferences(ConsoleFontPreferences prefs) {
        setConsoleFontPreferences(activeSchemeName, prefs);
    }

    public void setConsoleFontPreferences(String schemeName, ConsoleFontPreferences prefs) {
        String scheme = schemeName != null ? schemeName : activeSchemeName;
        consoleFonts.put(scheme, prefs != null ? prefs.clone() : new ConsoleFontPreferences());
        notifyListeners();
    }

    public void save() {
        Settings.put("editor.colorscheme.active", activeSchemeName);
        Settings.put("editor.colorscheme.custom.names", String.join(",", customSchemes));

        SchemeFontPreferences sfp = getSchemeFontPreferences(activeSchemeName);
        Settings.put("editor.colorscheme.font.use", String.valueOf(sfp.isUseSchemeFont()));
        Settings.put("editor.colorscheme.font.name", sfp.getFontFamily());
        Settings.put("editor.colorscheme.font.size", String.valueOf(sfp.getFontSize()));
        Settings.put("editor.colorscheme.font.lineheight", String.valueOf(sfp.getLineHeight()));
        Settings.put("editor.colorscheme.font.fallback", sfp.getFallbackFont());
        Settings.put("editor.colorscheme.font.ligatures", String.valueOf(sfp.isEnableLigatures()));

        ConsoleFontPreferences cfp = getConsoleFontPreferences(activeSchemeName);
        Settings.put("editor.console.font.use", String.valueOf(cfp.isUseConsoleFont()));
        Settings.put("editor.console.font.name", cfp.getFontFamily());
        Settings.put("editor.console.font.size", String.valueOf(cfp.getFontSize()));
        Settings.put("editor.console.font.lineheight", String.valueOf(cfp.getLineHeight()));
        Settings.put("editor.console.font.fallback", cfp.getFallbackFont());
        Settings.put("editor.console.font.ligatures", String.valueOf(cfp.isEnableLigatures()));

        notifyListeners();
    }

    public EditorColorSchemeSettings copy() {
        EditorColorSchemeSettings c = new EditorColorSchemeSettings();
        c.activeSchemeName = this.activeSchemeName;
        c.customSchemes.clear();
        c.customSchemes.addAll(this.customSchemes);
        c.schemeAttributes.clear();
        for (Map.Entry<String, Map<String, ColorAttribute>> e : this.schemeAttributes.entrySet()) {
            c.schemeAttributes.put(e.getKey(), cloneAttributesMap(e.getValue()));
        }
        c.schemeFonts.clear();
        for (Map.Entry<String, SchemeFontPreferences> e : this.schemeFonts.entrySet()) {
            c.schemeFonts.put(e.getKey(), e.getValue().clone());
        }
        c.consoleFonts.clear();
        for (Map.Entry<String, ConsoleFontPreferences> e : this.consoleFonts.entrySet()) {
            c.consoleFonts.put(e.getKey(), e.getValue().clone());
        }
        return c;
    }

    public void applyFrom(EditorColorSchemeSettings other) {
        this.activeSchemeName = other.activeSchemeName;
        this.customSchemes.clear();
        this.customSchemes.addAll(other.customSchemes);
        this.schemeAttributes.clear();
        for (Map.Entry<String, Map<String, ColorAttribute>> e : other.schemeAttributes.entrySet()) {
            this.schemeAttributes.put(e.getKey(), cloneAttributesMap(e.getValue()));
        }
        this.schemeFonts.clear();
        for (Map.Entry<String, SchemeFontPreferences> e : other.schemeFonts.entrySet()) {
            this.schemeFonts.put(e.getKey(), e.getValue().clone());
        }
        this.consoleFonts.clear();
        for (Map.Entry<String, ConsoleFontPreferences> e : other.consoleFonts.entrySet()) {
            this.consoleFonts.put(e.getKey(), e.getValue().clone());
        }
        save();
        notifyListeners();
    }

    public boolean isModified(EditorColorSchemeSettings original) {
        if (!Objects.equals(this.activeSchemeName, original.activeSchemeName)) return true;
        if (!this.customSchemes.equals(original.customSchemes)) return true;
        if (!this.schemeFonts.equals(original.schemeFonts)) return true;
        if (!this.consoleFonts.equals(original.consoleFonts)) return true;
        return !this.schemeAttributes.equals(original.schemeAttributes);
    }

    public static class SchemeFontPreferences {
        private boolean useSchemeFont = false;
        private String fontFamily = "JetBrains Mono";
        private double fontSize = 13.0;
        private double lineHeight = 1.2;
        private String fallbackFont = "<None>";
        private boolean showOnlyMonospaced = true;
        private boolean enableLigatures = false;

        public SchemeFontPreferences() {}

        public SchemeFontPreferences(boolean useSchemeFont, String fontFamily, double fontSize, double lineHeight,
                                     String fallbackFont, boolean showOnlyMonospaced, boolean enableLigatures) {
            this.useSchemeFont = useSchemeFont;
            this.fontFamily = fontFamily != null ? fontFamily : "JetBrains Mono";
            this.fontSize = fontSize;
            this.lineHeight = lineHeight;
            this.fallbackFont = fallbackFont != null ? fallbackFont : "<None>";
            this.showOnlyMonospaced = showOnlyMonospaced;
            this.enableLigatures = enableLigatures;
        }

        public boolean isUseSchemeFont() { return useSchemeFont; }
        public void setUseSchemeFont(boolean useSchemeFont) { this.useSchemeFont = useSchemeFont; }
        public String getFontFamily() { return fontFamily; }
        public void setFontFamily(String fontFamily) { this.fontFamily = fontFamily; }
        public double getFontSize() { return fontSize; }
        public void setFontSize(double fontSize) { this.fontSize = fontSize; }
        public double getLineHeight() { return lineHeight; }
        public void setLineHeight(double lineHeight) { this.lineHeight = lineHeight; }
        public String getFallbackFont() { return fallbackFont; }
        public void setFallbackFont(String fallbackFont) { this.fallbackFont = fallbackFont; }
        public boolean isShowOnlyMonospaced() { return showOnlyMonospaced; }
        public void setShowOnlyMonospaced(boolean showOnlyMonospaced) { this.showOnlyMonospaced = showOnlyMonospaced; }
        public boolean isEnableLigatures() { return enableLigatures; }
        public void setEnableLigatures(boolean enableLigatures) { this.enableLigatures = enableLigatures; }

        public SchemeFontPreferences clone() {
            return new SchemeFontPreferences(useSchemeFont, fontFamily, fontSize, lineHeight, fallbackFont, showOnlyMonospaced, enableLigatures);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SchemeFontPreferences that)) return false;
            return useSchemeFont == that.useSchemeFont &&
                    Double.compare(that.fontSize, fontSize) == 0 &&
                    Double.compare(that.lineHeight, lineHeight) == 0 &&
                    showOnlyMonospaced == that.showOnlyMonospaced &&
                    enableLigatures == that.enableLigatures &&
                    Objects.equals(fontFamily, that.fontFamily) &&
                    Objects.equals(fallbackFont, that.fallbackFont);
        }

        @Override
        public int hashCode() {
            return Objects.hash(useSchemeFont, fontFamily, fontSize, lineHeight, fallbackFont, showOnlyMonospaced, enableLigatures);
        }
    }

    public static class ConsoleFontPreferences {
        private boolean useConsoleFont = false;
        private String fontFamily = "JetBrains Mono";
        private double fontSize = 13.0;
        private double lineHeight = 1.2;
        private String fallbackFont = "<None>";
        private boolean showOnlyMonospaced = true;
        private boolean enableLigatures = false;

        public ConsoleFontPreferences() {}

        public ConsoleFontPreferences(boolean useConsoleFont, String fontFamily, double fontSize, double lineHeight,
                                      String fallbackFont, boolean showOnlyMonospaced, boolean enableLigatures) {
            this.useConsoleFont = useConsoleFont;
            this.fontFamily = fontFamily != null ? fontFamily : "JetBrains Mono";
            this.fontSize = fontSize;
            this.lineHeight = lineHeight;
            this.fallbackFont = fallbackFont != null ? fallbackFont : "<None>";
            this.showOnlyMonospaced = showOnlyMonospaced;
            this.enableLigatures = enableLigatures;
        }

        public boolean isUseConsoleFont() { return useConsoleFont; }
        public void setUseConsoleFont(boolean useConsoleFont) { this.useConsoleFont = useConsoleFont; }
        public String getFontFamily() { return fontFamily; }
        public void setFontFamily(String fontFamily) { this.fontFamily = fontFamily; }
        public double getFontSize() { return fontSize; }
        public void setFontSize(double fontSize) { this.fontSize = fontSize; }
        public double getLineHeight() { return lineHeight; }
        public void setLineHeight(double lineHeight) { this.lineHeight = lineHeight; }
        public String getFallbackFont() { return fallbackFont; }
        public void setFallbackFont(String fallbackFont) { this.fallbackFont = fallbackFont; }
        public boolean isShowOnlyMonospaced() { return showOnlyMonospaced; }
        public void setShowOnlyMonospaced(boolean showOnlyMonospaced) { this.showOnlyMonospaced = showOnlyMonospaced; }
        public boolean isEnableLigatures() { return enableLigatures; }
        public void setEnableLigatures(boolean enableLigatures) { this.enableLigatures = enableLigatures; }

        public ConsoleFontPreferences clone() {
            return new ConsoleFontPreferences(useConsoleFont, fontFamily, fontSize, lineHeight, fallbackFont, showOnlyMonospaced, enableLigatures);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ConsoleFontPreferences that)) return false;
            return useConsoleFont == that.useConsoleFont &&
                    Double.compare(that.fontSize, fontSize) == 0 &&
                    Double.compare(that.lineHeight, lineHeight) == 0 &&
                    showOnlyMonospaced == that.showOnlyMonospaced &&
                    enableLigatures == that.enableLigatures &&
                    Objects.equals(fontFamily, that.fontFamily) &&
                    Objects.equals(fallbackFont, that.fallbackFont);
        }

        @Override
        public int hashCode() {
            return Objects.hash(useConsoleFont, fontFamily, fontSize, lineHeight, fallbackFont, showOnlyMonospaced, enableLigatures);
        }
    }
}
