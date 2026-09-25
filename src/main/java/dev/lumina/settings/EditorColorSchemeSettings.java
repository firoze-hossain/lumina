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

    private static final EditorColorSchemeSettings INSTANCE = new EditorColorSchemeSettings();

    public static EditorColorSchemeSettings getInstance() {
        return INSTANCE;
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
                    Objects.equals(inheritFrom, that.inheritFrom);
        }

        @Override
        public int hashCode() {
            return Objects.hash(foreground, background, errorStripeColor, effectType, effectColor, bold, italic, inherit, inheritFrom);
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

    private Map<String, ColorAttribute> createIslandsDarkDefaults() {
        Map<String, ColorAttribute> map = new LinkedHashMap<>();

        // Code (media_1790343290072.png & media_1790343304243.png)
        map.put("Code // Identifier under caret", new ColorAttribute(null, "#373B39", "#5B786A", EffectType.UNDERSCORED, null, false, false));
        map.put("Code // Identifier under caret (write)", new ColorAttribute(null, "#402E3B", "#BA5F9E", EffectType.UNDERSCORED, null, false, false));
        map.put("Code // Injected language fragment", new ColorAttribute(null, "#2B3838", false, false));
        map.put("Code // Line number", new ColorAttribute("#4E5157", null, false, false));
        map.put("Code // Line number on caret row", new ColorAttribute("#A1A3AB", null, null, EffectType.UNDERSCORED, null, false, false));
        map.put("Code // Matched brace", new ColorAttribute(null, "#3B514D", false, false));
        map.put("Code // Method separator color", new ColorAttribute("#393B40", null, false, false));
        map.put("Code // TODO defaults", new ColorAttribute("#549159", null, true, true));
        map.put("Code // Unmatched brace", new ColorAttribute(null, "#562423", false, false));

        // Editor (media_1790343317824.png, media_1790343353746.png, media_1790343452222.png - media_1790343516571.png)
        map.put("Editor // Bookmarks", new ColorAttribute(null, null, "#F7E9C6", EffectType.BORDERED, null, false, false));
        map.put("Editor // Breadcrumbs // Border", new ColorAttribute("#393B40", null, false, false));
        map.put("Editor // Breadcrumbs // Current", new ColorAttribute("#DFE1E5", "#2B2D30", null, EffectType.BORDERED, null, false, false));
        map.put("Editor // Breadcrumbs // Default", new ColorAttribute("#848BA3", null, false, false));
        map.put("Editor // Breadcrumbs // Hovered", new ColorAttribute("#DFE1E5", "#35373B", false, false));
        map.put("Editor // Breadcrumbs // Inactive", new ColorAttribute("#5F6368", null, false, false));
        map.put("Editor // Caret", new ColorAttribute("#DFE1E5", null, false, false));
        map.put("Editor // Caret row", new ColorAttribute(null, "#1F2024", false, false));

        // Guides (media_1790343452222.png)
        map.put("Editor // Guides // Hard wrap guide", new ColorAttribute("#323438", null, false, false));
        map.put("Editor // Guides // Indent guide", new ColorAttribute("#313438", null, false, false));
        map.put("Editor // Guides // Indent guide selected", new ColorAttribute("#4E5157", null, false, false));
        map.put("Editor // Guides // Matched brace guide", new ColorAttribute("#3B514D", null, false, false));
        map.put("Editor // Guides // Visual guides", new ColorAttribute("#393B40", null, false, false));

        map.put("Editor // Gutter background", new ColorAttribute(null, null, false, false));
        map.put("Editor // Notification background", new ColorAttribute(null, "#25324D", false, false));
        map.put("Editor // Selection background", new ColorAttribute(null, "#214283", false, false));
        map.put("Editor // Selection foreground", new ColorAttribute(null, null, false, false));

        // Sticky Lines (media_1790343472376.png, media_1790343485724.png, media_1790343496020.png)
        map.put("Editor // Sticky Lines // Background", new ColorAttribute(null, null, false, false));

        ColorAttribute stickyBorder = new ColorAttribute();
        stickyBorder.setInherit(true);
        stickyBorder.setInheritFrom("Editor // Guides // Hard wrap guide");
        map.put("Editor // Sticky Lines // Border", stickyBorder);

        ColorAttribute stickyHovered = new ColorAttribute();
        stickyHovered.setInherit(true);
        stickyHovered.setInheritFrom("Editor // Caret row");
        map.put("Editor // Sticky Lines // Hovered", stickyHovered);

        // Tabs (media_1790343516571.png)
        map.put("Editor // Tabs // Modified icon color", new ColorAttribute("#4083C9", null, false, false));
        map.put("Editor // Tabs // Selected Tab", new ColorAttribute(null, "#1E1F22", false, false));
        map.put("Editor // Tabs // Selected Tab inactive", new ColorAttribute(null, "#2B2D30", false, false));
        map.put("Editor // Tabs // Underline", new ColorAttribute("#3574F0", null, false, false));
        map.put("Editor // Tabs // Underline inactive", new ColorAttribute("#4E5157", null, false, false));

        map.put("Editor // Tear line", new ColorAttribute("#393B40", null, false, false));
        map.put("Editor // Tear line selection", new ColorAttribute("#4E5157", null, false, false));
        map.put("Editor // Vertical Scrollbar // Thumb", new ColorAttribute(null, "#4E5157", false, false));
        map.put("Editor // Vertical Scrollbar // Thumb while scrolling", new ColorAttribute(null, "#A6A6A6", false, false));
        map.put("Editor // Vertical Scrollbar // Track", new ColorAttribute(null, "#1E1F22", false, false));

        // Errors and Warnings (media_1790345048357.png - media_1790345076709.png)
        map.put("Errors and Warnings // Deprecated symbol", new ColorAttribute(null, null, null, EffectType.STRIKEOUT, "#8C8C8C", false, false));
        map.put("Errors and Warnings // Deprecated symbol marked for removal", new ColorAttribute(null, null, null, EffectType.STRIKEOUT, "#F75464", false, false));
        map.put("Errors and Warnings // Duplicate from server", new ColorAttribute(null, "#5E5339", null, EffectType.BORDERED, null, false, false));
        map.put("Errors and Warnings // Error", new ColorAttribute(null, null, "#E5534B", EffectType.UNDERWAVED, "#F75464", false, false));
        map.put("Errors and Warnings // Grammar error", new ColorAttribute(null, null, null, EffectType.UNDERWAVED, "#713D40", false, false));
        map.put("Errors and Warnings // Problem from server", new ColorAttribute(null, null, null, EffectType.BORDERED, "#C29E4A", false, false));
        map.put("Errors and Warnings // Runtime problem", new ColorAttribute(null, null, null, EffectType.UNDERWAVED, "#F75464", false, false));
        map.put("Errors and Warnings // Text style suggestion", new ColorAttribute(null, null, null, EffectType.UNDERSCORED, "#589DF6", false, false));
        map.put("Errors and Warnings // Typo", new ColorAttribute(null, null, null, EffectType.UNDERWAVED, "#4B7258", false, false));
        map.put("Errors and Warnings // Unknown symbol", new ColorAttribute("#F75464", null, false, false));
        map.put("Errors and Warnings // Unused code", new ColorAttribute("#70727B", null, false, false));
        map.put("Errors and Warnings // Warning", new ColorAttribute(null, null, "#C29E4A", EffectType.UNDERWAVED, "#F2C55C", false, false));
        map.put("Errors and Warnings // Weak Warning", new ColorAttribute(null, null, "#B9BECF", EffectType.UNDERWAVED, "#B9BECF", false, false));

        // Hyperlinks
        map.put("Hyperlinks // Inactive hyperlink", new ColorAttribute("#70727B", null, null, EffectType.UNDERSCORED, "#70727B", false, false));
        map.put("Hyperlinks // Followed hyperlink", new ColorAttribute("#C77DBB", null, null, EffectType.UNDERSCORED, "#C77DBB", false, false));
        map.put("Hyperlinks // Reference hyperlink", new ColorAttribute("#548AF7", null, null, EffectType.UNDERSCORED, "#548AF7", false, false));

        // Identifiers
        map.put("Identifiers // Identifier under caret", new ColorAttribute(null, "#373B39", "#5B786A", EffectType.UNDERSCORED, null, false, false));
        map.put("Identifiers // Identifier under caret (write)", new ColorAttribute(null, "#402E3B", "#BA5F9E", EffectType.UNDERSCORED, null, false, false));

        // Line Coverage
        map.put("Line Coverage // Full coverage", new ColorAttribute("#499C54", null, false, false));
        map.put("Line Coverage // Partial coverage", new ColorAttribute("#D8A657", null, false, false));
        map.put("Line Coverage // Uncovered", new ColorAttribute("#E5534B", null, false, false));

        // Live Templates
        map.put("Live Templates // Active template", new ColorAttribute(null, null, null, EffectType.BORDERED, "#385E9D", false, false));
        map.put("Live Templates // Inactive template", new ColorAttribute(null, null, null, EffectType.BORDERED, "#5A5D63", false, false));

        // Popups and Hints
        map.put("Popups and Hints // Parameter hint", new ColorAttribute("#848BA3", "#2B2D30", false, false));
        map.put("Popups and Hints // Inlay hint", new ColorAttribute("#848BA3", "#2B2D30", false, false));

        // Preview
        map.put("Preview // Preview scope", new ColorAttribute("#56A8F5", null, false, false));

        // Search Results
        map.put("Search Results // Search result", new ColorAttribute(null, "#265261", "#2E5F7E", EffectType.NONE, null, false, false));
        map.put("Search Results // Search result (write access)", new ColorAttribute(null, "#582E37", "#732936", EffectType.NONE, null, false, false));

        // Text
        map.put("Text // Default text", new ColorAttribute("#DFE1E5", "#1E1F22", false, false));
        map.put("Text // Folded text", new ColorAttribute("#8C8C8C", "#393B40", false, false));
        map.put("Text // Deleted text", new ColorAttribute("#E5534B", null, null, EffectType.STRIKEOUT, "#E5534B", false, false));
        map.put("Text // Injected language fragment", new ColorAttribute(null, "#2B3838", false, false));

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
        Map<String, ColorAttribute> map = getSchemeAttributes(schemeName);
        return map.get(key);
    }

    public ColorAttribute resolveAttribute(String schemeName, String key) {
        ColorAttribute attr = getAttribute(schemeName, key);
        if (attr == null) return new ColorAttribute();
        if (attr.isInherit() && attr.getInheritFrom() != null && !attr.getInheritFrom().isBlank()) {
            ColorAttribute parent = resolveAttribute(schemeName, attr.getInheritFrom());
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
        Map<String, ColorAttribute> map = getSchemeAttributes(schemeName);
        if (attribute != null) {
            map.put(key, attribute.clone());
        } else {
            map.remove(key);
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
