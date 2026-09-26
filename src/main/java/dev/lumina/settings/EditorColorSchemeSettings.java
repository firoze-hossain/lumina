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
        DOTTED_LINE("Dotted line");

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

    private static final Map<String, AttributesDescriptor> DESCRIPTORS_BY_KEY;
    static {
        Map<String, AttributesDescriptor> map = new LinkedHashMap<>();
        for (AttributesDescriptor desc : GENERAL_DESCRIPTORS) {
            map.put(desc.getKey(), desc);
        }
        for (AttributesDescriptor desc : LANGUAGE_DEFAULTS_DESCRIPTORS) {
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

    public void save() {
        Settings.put("editor.colorscheme.active", activeSchemeName);
        Settings.put("editor.colorscheme.custom.names", String.join(",", customSchemes));
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
        save();
        notifyListeners();
    }

    public boolean isModified(EditorColorSchemeSettings original) {
        if (!Objects.equals(this.activeSchemeName, original.activeSchemeName)) return true;
        if (!this.customSchemes.equals(original.customSchemes)) return true;
        return !this.schemeAttributes.equals(original.schemeAttributes);
    }
}
