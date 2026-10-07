package dev.lumina.settings;

import dev.lumina.util.Settings;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic persistent configuration model for Editor > Emmet and its sub-pages (CSS, HTML, JSX).
 * Backed by persistent storage, supporting dirty tracking, add/remove properties, and real-time updates.
 */
public final class EmmetSettings {

    public static class CssPrefixEntry implements Comparable<CssPrefixEntry> {
        private String property;
        private boolean webkit;
        private boolean moz;
        private boolean ms;
        private boolean o;
        private boolean khtml;

        public CssPrefixEntry(String property, boolean webkit, boolean moz, boolean ms, boolean o, boolean khtml) {
            this.property = property;
            this.webkit = webkit;
            this.moz = moz;
            this.ms = ms;
            this.o = o;
            this.khtml = khtml;
        }

        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }

        public boolean isWebkit() {
            return webkit;
        }

        public void setWebkit(boolean webkit) {
            this.webkit = webkit;
        }

        public boolean isMoz() {
            return moz;
        }

        public void setMoz(boolean moz) {
            this.moz = moz;
        }

        public boolean isMs() {
            return ms;
        }

        public void setMs(boolean ms) {
            this.ms = ms;
        }

        public boolean isO() {
            return o;
        }

        public void setO(boolean o) {
            this.o = o;
        }

        public boolean isKhtml() {
            return khtml;
        }

        public void setKhtml(boolean khtml) {
            this.khtml = khtml;
        }

        public CssPrefixEntry copy() {
            return new CssPrefixEntry(property, webkit, moz, ms, o, khtml);
        }

        @Override
        public int compareTo(CssPrefixEntry other) {
            return this.property.compareToIgnoreCase(other.property);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            CssPrefixEntry that = (CssPrefixEntry) obj;
            return webkit == that.webkit && moz == that.moz && ms == that.ms && o == that.o && khtml == that.khtml &&
                    Objects.equals(property, that.property);
        }

        @Override
        public int hashCode() {
            return Objects.hash(property, webkit, moz, ms, o, khtml);
        }
    }

    private static final EmmetSettings INSTANCE = new EmmetSettings();

    public static EmmetSettings getInstance() {
        return INSTANCE;
    }

    @FunctionalInterface
    public interface Listener {
        void onSettingsChanged(EmmetSettings settings);
    }

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    // General Emmet
    private boolean enableEmmet = true;
    private String expandAbbreviationWith = "Tab";

    // CSS Emmet
    private boolean enableCssEmmet = true;
    private boolean enableFuzzySearch = false;
    private boolean enableUnknownProperties = false;
    private boolean autoInsertVendorPrefixes = true;
    private final Map<String, CssPrefixEntry> cssProperties = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    // HTML Emmet
    private boolean enableXmlHtmlEmmet = true;
    private boolean enableAbbreviationPreview = false;
    private boolean enableAutoUrlRecognition = true;
    private boolean addEditPointAtEndOfTemplate = false;

    // BEM
    private String bemElementSeparator = "__";
    private String bemModifierSeparator = "_";
    private String bemShortElementPrefix = "-";

    // Filters enabled by default
    private boolean filterXslTuning = false;
    private boolean filterCommentTags = false;
    private boolean filterEscape = false;
    private boolean filterSingleLine = false;
    private boolean filterBem = false;
    private boolean filterTrimLineMarkers = false;

    // JSX Emmet
    private boolean enableJsxEmmet = true;

    public EmmetSettings() {
        initDefaults();
        load();
    }

    public void resetToDefaults() {
        initDefaults();
    }

    public void initDefaults() {
        enableEmmet = true;
        expandAbbreviationWith = "Tab";
        enableCssEmmet = true;
        enableFuzzySearch = false;
        enableUnknownProperties = false;
        autoInsertVendorPrefixes = true;

        enableXmlHtmlEmmet = true;
        enableAbbreviationPreview = false;
        enableAutoUrlRecognition = true;
        addEditPointAtEndOfTemplate = false;

        bemElementSeparator = "__";
        bemModifierSeparator = "_";
        bemShortElementPrefix = "-";

        filterXslTuning = false;
        filterCommentTags = false;
        filterEscape = false;
        filterSingleLine = false;
        filterBem = false;
        filterTrimLineMarkers = false;

        enableJsxEmmet = true;

        cssProperties.clear();
        // Exact CSS properties and default vendor prefixes from IntelliJ IDEA screenshots:
        addCssProp("accelerator", false, false, true, false, false);
        addCssProp("accesskey", false, false, false, true, false);
        addCssProp("animation", true, true, false, true, false);
        addCssProp("animation-delay", true, true, false, true, false);
        addCssProp("animation-direction", true, true, false, true, false);
        addCssProp("animation-duration", true, true, false, true, false);
        addCssProp("animation-fill-mode", true, true, false, true, false);
        addCssProp("animation-iteration-count", true, true, false, true, false);
        addCssProp("animation-name", true, true, false, true, false);
        addCssProp("animation-play-state", true, true, false, true, false);
        addCssProp("animation-timing-function", true, true, false, true, false);
        addCssProp("appearance", true, true, false, false, false);
        addCssProp("backface-visibility", true, true, true, false, false);
        addCssProp("background-clip", true, true, false, false, false);
        addCssProp("background-composite", true, false, false, false, false);
        addCssProp("background-inline-policy", false, true, false, false, false);
        addCssProp("background-origin", true, false, false, false, false);
        addCssProp("background-position-x", false, false, true, false, false);
        addCssProp("background-position-y", false, false, true, false, false);
        addCssProp("background-size", true, false, false, false, false);
        addCssProp("behavior", false, false, true, false, false);
        addCssProp("binding", false, true, false, false, false);
        addCssProp("block-progression", false, false, true, false, false);
        addCssProp("border-bottom-colors", false, true, false, false, false);
        addCssProp("border-fit", true, false, false, false, false);
        addCssProp("border-horizontal-spacing", true, false, false, false, false);
        addCssProp("border-image", true, true, false, true, false);
        addCssProp("border-left-colors", false, true, false, false, false);
        addCssProp("border-radius", true, true, false, false, false);
        addCssProp("border-right-colors", false, true, false, false, false);
        addCssProp("border-top-colors", false, true, false, false, false);
        addCssProp("border-vertical-spacing", true, false, false, false, false);
        addCssProp("box-align", true, true, true, false, false);
        addCssProp("box-direction", true, true, true, false, false);
        addCssProp("box-flex", true, true, true, false, false);
        addCssProp("box-flex-group", true, false, false, false, false);
        addCssProp("box-lines", true, false, true, false, false);
        addCssProp("box-ordinal-group", true, true, true, false, false);
        addCssProp("box-orient", true, true, true, false, false);
        addCssProp("box-pack", true, true, true, false, false);
        addCssProp("box-reflect", true, false, false, false, false);
        addCssProp("box-shadow", true, true, false, false, false);
        addCssProp("box-sizing", true, true, false, false, false);
        addCssProp("color-correction", true, false, false, false, false);
        addCssProp("column-break-after", true, false, false, false, false);
        addCssProp("column-break-before", true, false, false, false, false);
        addCssProp("column-break-inside", true, false, false, false, false);
        addCssProp("column-count", true, true, false, false, false);
        addCssProp("column-gap", true, true, false, false, false);
        addCssProp("column-rule-color", true, true, false, false, false);
        addCssProp("column-rule-style", true, true, false, false, false);
        addCssProp("column-rule-width", true, true, false, false, false);
        addCssProp("column-span", true, false, false, false, false);
        addCssProp("column-width", true, true, false, false, false);
        addCssProp("content-zoom-boundary", false, false, true, false, false);
        addCssProp("content-zoom-boundary-min", false, false, true, false, false);
        addCssProp("content-zoom-chaining", false, false, true, false, false);
        addCssProp("content-zoom-snap", false, false, true, false, false);
        addCssProp("content-zoom-snap-points", false, false, true, false, false);
        addCssProp("content-zoom-snap-type", false, false, true, false, false);
        addCssProp("dashboard-region", true, false, false, false, false);
        addCssProp("filter", false, false, true, false, false);
        addCssProp("float-edge", false, true, false, false, false);
        addCssProp("flow-from", false, false, true, false, false);
        addCssProp("flow-into", false, false, true, false, false);
        addCssProp("font-feature-settings", false, true, true, false, false);
        addCssProp("font-language-override", false, true, false, false, false);
        addCssProp("font-smoothing", true, false, false, false, false);
        addCssProp("force-broken-image-icon", false, true, false, false, false);
        addCssProp("grid-column", false, false, true, false, false);
        addCssProp("grid-column-align", false, false, true, false, false);
        addCssProp("grid-columns", false, false, true, false, false);
        addCssProp("grid-column-span", false, false, true, false, false);
        addCssProp("grid-layer", false, false, true, false, false);
        addCssProp("grid-row", false, false, true, false, false);
        addCssProp("grid-row-align", false, false, true, false, false);
        addCssProp("grid-rows", false, false, true, false, false);
        addCssProp("grid-row-span", false, false, true, false, false);
        addCssProp("high-contrast-adjust", false, false, true, false, false);
        addCssProp("highlight", true, false, false, false, false);
        addCssProp("hyphenate-character", true, false, false, false, false);
        addCssProp("hyphenate-limit-after", true, false, false, false, false);
        addCssProp("hyphenate-limit-before", true, false, false, false, false);
        addCssProp("hyphenate-limit-chars", false, false, true, false, false);
        addCssProp("hyphenate-limit-lines", false, false, true, false, false);
        addCssProp("hyphenate-limit-zone", false, false, true, false, false);
        addCssProp("hyphens", true, true, true, false, false);
        addCssProp("image-region", false, true, false, false, false);
        addCssProp("ime-mode", false, false, true, false, false);
        addCssProp("input-format", false, false, false, true, false);
        addCssProp("input-required", false, false, false, true, false);
        addCssProp("interpolation-mode", false, false, true, false, false);
        addCssProp("layout-flow", false, false, true, false, false);
        addCssProp("layout-grid", false, false, true, false, false);
        addCssProp("layout-grid-char", false, false, true, false, false);
        addCssProp("layout-grid-line", false, false, true, false, false);
        addCssProp("layout-grid-mode", false, false, true, false, false);
        addCssProp("layout-grid-type", false, false, true, false, false);
        addCssProp("line-box-contain", true, false, false, false, false);
        addCssProp("line-break", true, false, true, false, false);
        addCssProp("line-clamp", true, false, false, false, false);
        addCssProp("link", false, false, false, true, false);
        addCssProp("link-source", false, false, false, true, false);
        addCssProp("locale", true, false, false, false, false);
        addCssProp("margin-after-collapse", true, false, false, false, false);
        addCssProp("margin-before-collapse", true, false, false, false, false);
        addCssProp("marquee-dir", false, false, false, true, false);
        addCssProp("marquee-direction", true, false, false, false, false);
        addCssProp("marquee-increment", true, false, false, false, false);
        addCssProp("marquee-loop", false, false, false, true, false);
        addCssProp("marquee-repetition", true, false, false, false, false);
        addCssProp("marquee-speed", false, false, false, true, false);
        addCssProp("marquee-style", true, false, false, true, false);
        addCssProp("mask-attachment", true, false, false, false, false);
        addCssProp("mask-box-image", true, false, false, false, false);
        addCssProp("mask-box-image-outset", true, false, false, false, false);
        addCssProp("mask-box-image-repeat", true, false, false, false, false);
        addCssProp("mask-box-image-slice", true, false, false, false, false);
        addCssProp("mask-box-image-source", true, false, false, false, false);
        addCssProp("mask-box-image-width", true, false, false, false, false);
        addCssProp("mask-clip", true, false, false, false, false);
        addCssProp("mask-composite", true, false, false, false, false);
        addCssProp("mask-image", true, false, false, false, false);
        addCssProp("mask-origin", true, false, false, false, false);
        addCssProp("mask-position", true, false, false, false, false);
        addCssProp("mask-repeat", true, false, false, false, false);
        addCssProp("mask-size", true, false, false, false, false);
        addCssProp("nbsp-mode", true, false, false, false, false);
        addCssProp("object-fit", false, false, false, true, false);
        addCssProp("object-position", false, false, false, true, false);
        addCssProp("orient", false, true, false, false, false);
        addCssProp("outline-radius-bottomleft", false, true, false, false, false);
        addCssProp("outline-radius-bottomright", false, true, false, false, false);
        addCssProp("outline-radius-topleft", false, true, false, false, false);
        addCssProp("outline-radius-topright", false, true, false, false, false);
        addCssProp("overflow-style", false, false, true, false, false);
        addCssProp("perspective", true, true, true, false, false);
        addCssProp("perspective-origin", true, true, true, false, false);
        addCssProp("perspective-origin-x", false, false, true, false, false);
        addCssProp("perspective-origin-y", false, false, true, false, false);
        addCssProp("rtl-ordering", true, false, false, false, false);
        addCssProp("scrollbar-arrow-color", false, false, true, false, false);
        addCssProp("scrollbar-base-color", false, false, true, false, false);
        addCssProp("scrollbar-darkshadow-color", false, false, true, false, false);
        addCssProp("scrollbar-face-color", false, false, true, false, false);
        addCssProp("scrollbar-highlight-color", false, false, true, false, false);
        addCssProp("scrollbar-shadow-color", false, false, true, false, false);
        addCssProp("scrollbar-track-color", false, false, true, false, false);
        addCssProp("scroll-boundary", false, false, true, false, false);
        addCssProp("scroll-boundary-bottom", false, false, true, false, false);
        addCssProp("scroll-boundary-left", false, false, true, false, false);
        addCssProp("scroll-boundary-right", false, false, true, false, false);
        addCssProp("scroll-boundary-top", false, false, true, false, false);
        addCssProp("scroll-chaining", false, false, true, false, false);
        addCssProp("scroll-rails", false, false, true, false, false);
        addCssProp("scroll-snap-points-x", false, false, true, false, false);
        addCssProp("scroll-snap-points-y", false, false, true, false, false);
        addCssProp("scroll-snap-type", false, false, true, false, false);
        addCssProp("scroll-snap-x", false, false, true, false, false);
        addCssProp("scroll-snap-y", false, false, true, false, false);
        addCssProp("stack-sizing", false, true, false, false, false);
        addCssProp("svg-shadow", true, false, false, false, false);
        addCssProp("table-baseline", false, false, false, true, false);
        addCssProp("tab-size", false, true, false, true, false);
        addCssProp("text-align-last", false, false, true, false, false);
        addCssProp("text-autospace", false, false, true, false, false);
        addCssProp("text-blink", false, true, false, false, false);
        addCssProp("text-combine", true, false, false, false, false);
        addCssProp("text-decoration-color", false, true, false, false, false);
        addCssProp("text-decoration-line", false, true, false, false, false);
        addCssProp("text-decorations-in-effect", true, false, false, false, false);
        addCssProp("text-decoration-style", false, true, false, false, false);
        addCssProp("text-emphasis-color", true, false, false, false, false);
        addCssProp("text-emphasis-position", true, false, false, false, false);
        addCssProp("text-emphasis-style", true, false, false, false, false);
        addCssProp("text-fill-color", true, false, false, false, false);
        addCssProp("text-justify", false, false, true, false, false);
        addCssProp("text-kashida-space", false, false, true, false, false);
        addCssProp("text-orientation", true, false, false, false, false);
        addCssProp("text-overflow", false, false, true, false, false);
        addCssProp("text-security", true, false, false, false, false);
        addCssProp("text-size-adjust", false, true, true, false, false);
        addCssProp("text-stroke-color", true, false, false, false, false);
        addCssProp("text-stroke-width", true, false, false, false, false);
        addCssProp("text-underline-position", false, false, true, false, false);
        addCssProp("touch-action", false, false, true, false, false);
        addCssProp("transform", true, true, true, true, false);
        addCssProp("transform-origin", true, true, true, true, false);
        addCssProp("transform-origin-x", false, false, true, false, false);
        addCssProp("transform-origin-y", false, false, true, false, false);
        addCssProp("transform-origin-z", false, false, true, false, false);
        addCssProp("transform-style", true, true, true, false, false);
        addCssProp("transition", true, true, true, true, false);
        addCssProp("transition-delay", true, true, true, true, false);
        addCssProp("transition-duration", true, true, true, true, false);
        addCssProp("transition-property", true, true, true, true, false);
        addCssProp("transition-timing-function", true, true, true, true, false);
        addCssProp("user-drag", true, false, false, false, false);
        addCssProp("user-focus", false, true, false, false, false);
        addCssProp("user-input", false, true, false, false, false);
        addCssProp("user-modify", true, true, false, false, false);
        addCssProp("user-select", true, true, true, false, false);
        addCssProp("window-shadow", false, true, false, false, false);
        addCssProp("word-break", false, false, true, false, false);
        addCssProp("word-wrap", false, false, true, false, false);
        addCssProp("wrap-flow", false, false, true, false, false);
        addCssProp("wrap-margin", false, false, true, false, false);
        addCssProp("wrap-through", false, false, true, false, false);
        addCssProp("writing-mode", true, false, true, false, false);
    }

    private void addCssProp(String prop, boolean webkit, boolean moz, boolean ms, boolean o, boolean khtml) {
        cssProperties.put(prop, new CssPrefixEntry(prop, webkit, moz, ms, o, khtml));
    }

    public void load() {
        String val = Settings.get("emmet.expand.key");
        if (val != null && !val.isBlank()) {
            expandAbbreviationWith = val;
        }

        val = Settings.get("emmet.css.enabled");
        if (val != null) enableCssEmmet = Boolean.parseBoolean(val);

        val = Settings.get("emmet.css.fuzzy");
        if (val != null) enableFuzzySearch = Boolean.parseBoolean(val);

        val = Settings.get("emmet.css.unknown");
        if (val != null) enableUnknownProperties = Boolean.parseBoolean(val);

        val = Settings.get("emmet.css.prefixes");
        if (val != null) autoInsertVendorPrefixes = Boolean.parseBoolean(val);

        val = Settings.get("emmet.enabled");
        if (val != null) enableEmmet = Boolean.parseBoolean(val);

        val = Settings.get("emmet.html.enabled");
        if (val != null) enableXmlHtmlEmmet = Boolean.parseBoolean(val);

        val = Settings.get("emmet.html.preview");
        if (val != null) enableAbbreviationPreview = Boolean.parseBoolean(val);

        val = Settings.get("emmet.html.auto_url");
        if (val != null) enableAutoUrlRecognition = Boolean.parseBoolean(val);

        val = Settings.get("emmet.html.add_edit_point");
        if (val != null) addEditPointAtEndOfTemplate = Boolean.parseBoolean(val);

        val = Settings.get("emmet.html.bem.element");
        if (val != null && !val.isBlank()) bemElementSeparator = val;

        val = Settings.get("emmet.html.bem.modifier");
        if (val != null && !val.isBlank()) bemModifierSeparator = val;

        val = Settings.get("emmet.html.bem.short_prefix");
        if (val != null && !val.isBlank()) bemShortElementPrefix = val;

        val = Settings.get("emmet.html.filter.xsl");
        if (val != null) filterXslTuning = Boolean.parseBoolean(val);

        val = Settings.get("emmet.html.filter.comment");
        if (val != null) filterCommentTags = Boolean.parseBoolean(val);

        val = Settings.get("emmet.html.filter.escape");
        if (val != null) filterEscape = Boolean.parseBoolean(val);

        val = Settings.get("emmet.html.filter.single_line");
        if (val != null) filterSingleLine = Boolean.parseBoolean(val);

        val = Settings.get("emmet.html.filter.bem");
        if (val != null) filterBem = Boolean.parseBoolean(val);

        val = Settings.get("emmet.html.filter.trim");
        if (val != null) filterTrimLineMarkers = Boolean.parseBoolean(val);

        val = Settings.get("emmet.jsx.enabled");
        if (val != null) enableJsxEmmet = Boolean.parseBoolean(val);

        // Load custom/modified properties
        for (CssPrefixEntry e : cssProperties.values()) {
            String pKey = "emmet.css.prop." + e.getProperty().toLowerCase();
            String pVal = Settings.get(pKey);
            if (pVal != null) {
                String[] parts = pVal.split(",");
                if (parts.length >= 5) {
                    e.setWebkit(Boolean.parseBoolean(parts[0]));
                    e.setMoz(Boolean.parseBoolean(parts[1]));
                    e.setMs(Boolean.parseBoolean(parts[2]));
                    e.setO(Boolean.parseBoolean(parts[3]));
                    e.setKhtml(Boolean.parseBoolean(parts[4]));
                }
            }
        }
    }

    public void save() {
        Settings.put("emmet.enabled", String.valueOf(enableEmmet));
        Settings.put("emmet.expand.key", expandAbbreviationWith);
        Settings.put("emmet.css.enabled", String.valueOf(enableCssEmmet));
        Settings.put("emmet.css.fuzzy", String.valueOf(enableFuzzySearch));
        Settings.put("emmet.css.unknown", String.valueOf(enableUnknownProperties));
        Settings.put("emmet.css.prefixes", String.valueOf(autoInsertVendorPrefixes));

        Settings.put("emmet.html.enabled", String.valueOf(enableXmlHtmlEmmet));
        Settings.put("emmet.html.preview", String.valueOf(enableAbbreviationPreview));
        Settings.put("emmet.html.auto_url", String.valueOf(enableAutoUrlRecognition));
        Settings.put("emmet.html.add_edit_point", String.valueOf(addEditPointAtEndOfTemplate));

        Settings.put("emmet.html.bem.element", bemElementSeparator);
        Settings.put("emmet.html.bem.modifier", bemModifierSeparator);
        Settings.put("emmet.html.bem.short_prefix", bemShortElementPrefix);

        Settings.put("emmet.html.filter.xsl", String.valueOf(filterXslTuning));
        Settings.put("emmet.html.filter.comment", String.valueOf(filterCommentTags));
        Settings.put("emmet.html.filter.escape", String.valueOf(filterEscape));
        Settings.put("emmet.html.filter.single_line", String.valueOf(filterSingleLine));
        Settings.put("emmet.html.filter.bem", String.valueOf(filterBem));
        Settings.put("emmet.html.filter.trim", String.valueOf(filterTrimLineMarkers));

        Settings.put("emmet.jsx.enabled", String.valueOf(enableJsxEmmet));

        for (CssPrefixEntry e : cssProperties.values()) {
            String pKey = "emmet.css.prop." + e.getProperty().toLowerCase();
            String pVal = String.join(",",
                    String.valueOf(e.isWebkit()),
                    String.valueOf(e.isMoz()),
                    String.valueOf(e.isMs()),
                    String.valueOf(e.isO()),
                    String.valueOf(e.isKhtml())
            );
            Settings.put(pKey, pVal);
        }
        notifyListeners();
    }

    public EmmetSettings copy() {
        EmmetSettings clone = new EmmetSettings();
        clone.applyFrom(this);
        return clone;
    }

    public void applyFrom(EmmetSettings other) {
        if (other == null) return;
        this.enableEmmet = other.enableEmmet;
        this.expandAbbreviationWith = other.expandAbbreviationWith;
        this.enableCssEmmet = other.enableCssEmmet;
        this.enableFuzzySearch = other.enableFuzzySearch;
        this.enableUnknownProperties = other.enableUnknownProperties;
        this.autoInsertVendorPrefixes = other.autoInsertVendorPrefixes;

        this.enableXmlHtmlEmmet = other.enableXmlHtmlEmmet;
        this.enableAbbreviationPreview = other.enableAbbreviationPreview;
        this.enableAutoUrlRecognition = other.enableAutoUrlRecognition;
        this.addEditPointAtEndOfTemplate = other.addEditPointAtEndOfTemplate;

        this.bemElementSeparator = other.bemElementSeparator;
        this.bemModifierSeparator = other.bemModifierSeparator;
        this.bemShortElementPrefix = other.bemShortElementPrefix;

        this.filterXslTuning = other.filterXslTuning;
        this.filterCommentTags = other.filterCommentTags;
        this.filterEscape = other.filterEscape;
        this.filterSingleLine = other.filterSingleLine;
        this.filterBem = other.filterBem;
        this.filterTrimLineMarkers = other.filterTrimLineMarkers;

        this.enableJsxEmmet = other.enableJsxEmmet;

        this.cssProperties.clear();
        for (CssPrefixEntry e : other.cssProperties.values()) {
            this.cssProperties.put(e.getProperty(), e.copy());
        }
    }

    public boolean isModified(EmmetSettings other) {
        if (other == null) return true;
        if (this.enableEmmet != other.enableEmmet) return true;
        if (!Objects.equals(this.expandAbbreviationWith, other.expandAbbreviationWith)) return true;
        if (this.enableCssEmmet != other.enableCssEmmet) return true;
        if (this.enableFuzzySearch != other.enableFuzzySearch) return true;
        if (this.enableUnknownProperties != other.enableUnknownProperties) return true;
        if (this.autoInsertVendorPrefixes != other.autoInsertVendorPrefixes) return true;

        if (this.enableXmlHtmlEmmet != other.enableXmlHtmlEmmet) return true;
        if (this.enableAbbreviationPreview != other.enableAbbreviationPreview) return true;
        if (this.enableAutoUrlRecognition != other.enableAutoUrlRecognition) return true;
        if (this.addEditPointAtEndOfTemplate != other.addEditPointAtEndOfTemplate) return true;

        if (!Objects.equals(this.bemElementSeparator, other.bemElementSeparator)) return true;
        if (!Objects.equals(this.bemModifierSeparator, other.bemModifierSeparator)) return true;
        if (!Objects.equals(this.bemShortElementPrefix, other.bemShortElementPrefix)) return true;

        if (this.filterXslTuning != other.filterXslTuning) return true;
        if (this.filterCommentTags != other.filterCommentTags) return true;
        if (this.filterEscape != other.filterEscape) return true;
        if (this.filterSingleLine != other.filterSingleLine) return true;
        if (this.filterBem != other.filterBem) return true;
        if (this.filterTrimLineMarkers != other.filterTrimLineMarkers) return true;

        if (this.enableJsxEmmet != other.enableJsxEmmet) return true;

        if (this.cssProperties.size() != other.cssProperties.size()) return true;
        for (Map.Entry<String, CssPrefixEntry> entry : this.cssProperties.entrySet()) {
            CssPrefixEntry otherEntry = other.cssProperties.get(entry.getKey());
            if (otherEntry == null || !entry.getValue().equals(otherEntry)) {
                return true;
            }
        }
        return false;
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
        for (Listener l : listeners) {
            try {
                l.onSettingsChanged(this);
            } catch (Exception ignored) {}
        }
    }

    // Getters and Setters
    public boolean isEnableEmmet() {
        return enableEmmet;
    }

    public void setEnableEmmet(boolean enableEmmet) {
        this.enableEmmet = enableEmmet;
    }

    public String getExpandAbbreviationWith() {
        return expandAbbreviationWith;
    }

    public void setExpandAbbreviationWith(String expandAbbreviationWith) {
        this.expandAbbreviationWith = expandAbbreviationWith;
    }

    public boolean isEnableCssEmmet() {
        return enableCssEmmet;
    }

    public void setEnableCssEmmet(boolean enableCssEmmet) {
        this.enableCssEmmet = enableCssEmmet;
    }

    public boolean isEnableFuzzySearch() {
        return enableFuzzySearch;
    }

    public void setEnableFuzzySearch(boolean enableFuzzySearch) {
        this.enableFuzzySearch = enableFuzzySearch;
    }

    public boolean isEnableUnknownProperties() {
        return enableUnknownProperties;
    }

    public void setEnableUnknownProperties(boolean enableUnknownProperties) {
        this.enableUnknownProperties = enableUnknownProperties;
    }

    public boolean isAutoInsertVendorPrefixes() {
        return autoInsertVendorPrefixes;
    }

    public void setAutoInsertVendorPrefixes(boolean autoInsertVendorPrefixes) {
        this.autoInsertVendorPrefixes = autoInsertVendorPrefixes;
    }

    public List<CssPrefixEntry> getCssProperties() {
        return new ArrayList<>(cssProperties.values());
    }

    public CssPrefixEntry getCssEntry(String property) {
        if (property == null) return null;
        return cssProperties.get(property.trim());
    }

    public void addOrUpdateCssProperty(String property, boolean webkit, boolean moz, boolean ms, boolean o, boolean khtml) {
        if (property != null && !property.isBlank()) {
            cssProperties.put(property.trim(), new CssPrefixEntry(property.trim(), webkit, moz, ms, o, khtml));
            notifyListeners();
        }
    }

    public void removeCssProperty(String property) {
        if (property != null && cssProperties.remove(property) != null) {
            notifyListeners();
        }
    }

    public boolean isEnableXmlHtmlEmmet() {
        return enableXmlHtmlEmmet;
    }

    public void setEnableXmlHtmlEmmet(boolean enableXmlHtmlEmmet) {
        this.enableXmlHtmlEmmet = enableXmlHtmlEmmet;
    }

    public boolean isEnableHtmlEmmet() {
        return enableXmlHtmlEmmet;
    }

    public void setEnableHtmlEmmet(boolean enableHtmlEmmet) {
        this.enableXmlHtmlEmmet = enableHtmlEmmet;
    }

    public boolean isEnableAbbreviationPreview() {
        return enableAbbreviationPreview;
    }

    public void setEnableAbbreviationPreview(boolean enableAbbreviationPreview) {
        this.enableAbbreviationPreview = enableAbbreviationPreview;
    }

    public boolean isEnableAutoUrlRecognition() {
        return enableAutoUrlRecognition;
    }

    public void setEnableAutoUrlRecognition(boolean enableAutoUrlRecognition) {
        this.enableAutoUrlRecognition = enableAutoUrlRecognition;
    }

    public boolean isAddEditPointAtEndOfTemplate() {
        return addEditPointAtEndOfTemplate;
    }

    public void setAddEditPointAtEndOfTemplate(boolean addEditPointAtEndOfTemplate) {
        this.addEditPointAtEndOfTemplate = addEditPointAtEndOfTemplate;
    }

    public String getBemElementSeparator() {
        return bemElementSeparator;
    }

    public void setBemElementSeparator(String bemElementSeparator) {
        this.bemElementSeparator = bemElementSeparator != null ? bemElementSeparator : "__";
    }

    public String getBemModifierSeparator() {
        return bemModifierSeparator;
    }

    public void setBemModifierSeparator(String bemModifierSeparator) {
        this.bemModifierSeparator = bemModifierSeparator != null ? bemModifierSeparator : "_";
    }

    public String getBemShortElementPrefix() {
        return bemShortElementPrefix;
    }

    public void setBemShortElementPrefix(String bemShortElementPrefix) {
        this.bemShortElementPrefix = bemShortElementPrefix != null ? bemShortElementPrefix : "-";
    }

    public boolean isFilterXslTuning() {
        return filterXslTuning;
    }

    public void setFilterXslTuning(boolean filterXslTuning) {
        this.filterXslTuning = filterXslTuning;
    }

    public boolean isFilterCommentTags() {
        return filterCommentTags;
    }

    public void setFilterCommentTags(boolean filterCommentTags) {
        this.filterCommentTags = filterCommentTags;
    }

    public boolean isFilterEscape() {
        return filterEscape;
    }

    public void setFilterEscape(boolean filterEscape) {
        this.filterEscape = filterEscape;
    }

    public boolean isFilterSingleLine() {
        return filterSingleLine;
    }

    public void setFilterSingleLine(boolean filterSingleLine) {
        this.filterSingleLine = filterSingleLine;
    }

    public boolean isFilterBem() {
        return filterBem;
    }

    public void setFilterBem(boolean filterBem) {
        this.filterBem = filterBem;
    }

    public boolean isFilterTrimLineMarkers() {
        return filterTrimLineMarkers;
    }

    public void setFilterTrimLineMarkers(boolean filterTrimLineMarkers) {
        this.filterTrimLineMarkers = filterTrimLineMarkers;
    }

    public boolean isEnableJsxEmmet() {
        return enableJsxEmmet;
    }

    public void setEnableJsxEmmet(boolean enableJsxEmmet) {
        this.enableJsxEmmet = enableJsxEmmet;
    }

    public boolean isAutoInsertClosingTag() {
        return true;
    }

    public void setAutoInsertClosingTag(boolean autoInsertClosingTag) {
    }

    public boolean isUseClassName() {
        return true;
    }

    public void setUseClassName(boolean useClassName) {
    }
}
