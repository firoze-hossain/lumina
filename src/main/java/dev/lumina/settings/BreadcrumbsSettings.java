package dev.lumina.settings;

import dev.lumina.util.Settings;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic persistent configuration and model for IntelliJ IDEA-style Editor > General > Breadcrumbs settings.
 * Backed by ~/.lumina/lumina.properties, supporting dynamic listeners,
 * real-time UI synchronization, and language-specific breadcrumbs toggling.
 */
public final class BreadcrumbsSettings {

    // 29 languages in exact alphabetical order from IntelliJ IDEA screenshot
    public static final List<String> ALL_LANGUAGES = List.of(
            "CSS", "ERB", "FreeMarker", "Go", "Groovy", "HTML", "Java", "JavaScript", "JSON", "JSP",
            "JSPX", "Jupyter", "Kotlin", "Less", "Markdown", "PHP", "protobuf", "Python", "Ruby", "Rust",
            "Sass", "Scala", "SCSS", "SQL", "TypeScript", "VTL", "XHTML", "XML", "YAML"
    );

    // Default checked languages matching media_1790268628875.png
    private static final Set<String> DEFAULT_ENABLED_LANGUAGES = Set.of(
            "ERB", "Go", "PHP", "protobuf", "Ruby", "Scala"
    );

    private static final BreadcrumbsSettings INSTANCE = new BreadcrumbsSettings();

    public static BreadcrumbsSettings getInstance() {
        return INSTANCE;
    }

    public enum BreadcrumbsPlacement {
        TOP("Top"),
        BOTTOM("Bottom");

        private final String label;

        BreadcrumbsPlacement(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static BreadcrumbsPlacement fromLabel(String label) {
            for (BreadcrumbsPlacement p : values()) {
                if (p.label.equalsIgnoreCase(label) || p.name().equalsIgnoreCase(label)) {
                    return p;
                }
            }
            return BOTTOM;
        }
    }

    @FunctionalInterface
    public interface Listener {
        void onSettingsChanged(BreadcrumbsSettings settings);
    }

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private boolean showBreadcrumbs = true;
    private BreadcrumbsPlacement placement = BreadcrumbsPlacement.BOTTOM;
    private final Map<String, Boolean> languages = new LinkedHashMap<>();

    public BreadcrumbsSettings() {
        initDefaults();
        load();
    }

    private void initDefaults() {
        showBreadcrumbs = true;
        placement = BreadcrumbsPlacement.BOTTOM;
        languages.clear();
        for (String lang : ALL_LANGUAGES) {
            languages.put(lang, DEFAULT_ENABLED_LANGUAGES.contains(lang));
        }
    }

    public synchronized void resetToDefaults() {
        initDefaults();
        fireChanged();
    }

    public synchronized void load() {
        String val;

        val = Settings.get("editor.breadcrumbs.show");
        if (val != null) showBreadcrumbs = Boolean.parseBoolean(val);

        val = Settings.get("editor.breadcrumbs.placement");
        if (val != null) placement = BreadcrumbsPlacement.fromLabel(val);

        val = Settings.get("editor.breadcrumbs.languages");
        if (val != null && !val.isBlank()) {
            Set<String> enabledSet = new HashSet<>(Arrays.asList(val.split(",")));
            for (String lang : ALL_LANGUAGES) {
                languages.put(lang, enabledSet.contains(lang));
            }
        }
    }

    public synchronized void save() {
        Settings.put("editor.breadcrumbs.show", String.valueOf(showBreadcrumbs));
        Settings.put("editor.breadcrumbs.placement", placement.name());

        List<String> enabledList = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : languages.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                enabledList.add(entry.getKey());
            }
        }
        Settings.put("editor.breadcrumbs.languages", String.join(",", enabledList));

        fireChanged();
    }

    public synchronized BreadcrumbsSettings copy() {
        BreadcrumbsSettings c = new BreadcrumbsSettings();
        c.copyFrom(this);
        return c;
    }

    public synchronized void copyFrom(BreadcrumbsSettings o) {
        this.showBreadcrumbs = o.showBreadcrumbs;
        this.placement = o.placement;
        this.languages.clear();
        this.languages.putAll(o.languages);
    }

    public synchronized boolean isModified(BreadcrumbsSettings o) {
        if (this.showBreadcrumbs != o.showBreadcrumbs) return true;
        if (this.placement != o.placement) return true;
        for (String lang : ALL_LANGUAGES) {
            boolean mine = Boolean.TRUE.equals(this.languages.get(lang));
            boolean other = Boolean.TRUE.equals(o.languages.get(lang));
            if (mine != other) return true;
        }
        return false;
    }

    public void addListener(Listener l) {
        if (l != null && !listeners.contains(l)) listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    private void fireChanged() {
        for (Listener l : listeners) {
            try {
                l.onSettingsChanged(this);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public boolean isLanguageEnabled(String language) {
        if (language == null) return false;
        for (Map.Entry<String, Boolean> entry : languages.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(language)) {
                return Boolean.TRUE.equals(entry.getValue());
            }
        }
        return false;
    }

    public void setLanguageEnabled(String language, boolean enabled) {
        for (String key : ALL_LANGUAGES) {
            if (key.equalsIgnoreCase(language)) {
                languages.put(key, enabled);
                return;
            }
        }
        languages.put(language, enabled);
    }

    public boolean isLanguageEnabledForFile(Path filePath) {
        if (filePath == null) return showBreadcrumbs;
        String fileName = filePath.getFileName().toString().toLowerCase();

        if (fileName.endsWith(".java")) return isLanguageEnabled("Java");
        if (fileName.endsWith(".rs")) return isLanguageEnabled("Rust");
        if (fileName.endsWith(".go")) return isLanguageEnabled("Go");
        if (fileName.endsWith(".py")) return isLanguageEnabled("Python");
        if (fileName.endsWith(".kt") || fileName.endsWith(".kts")) return isLanguageEnabled("Kotlin");
        if (fileName.endsWith(".scala") || fileName.endsWith(".sc")) return isLanguageEnabled("Scala");
        if (fileName.endsWith(".js") || fileName.endsWith(".mjs")) return isLanguageEnabled("JavaScript");
        if (fileName.endsWith(".ts") || fileName.endsWith(".tsx")) return isLanguageEnabled("TypeScript");
        if (fileName.endsWith(".php")) return isLanguageEnabled("PHP");
        if (fileName.endsWith(".rb") || fileName.endsWith(".erb")) return isLanguageEnabled("Ruby") || isLanguageEnabled("ERB");
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) return isLanguageEnabled("HTML");
        if (fileName.endsWith(".xml")) return isLanguageEnabled("XML");
        if (fileName.endsWith(".json")) return isLanguageEnabled("JSON");
        if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) return isLanguageEnabled("YAML");
        if (fileName.endsWith(".css")) return isLanguageEnabled("CSS");
        if (fileName.endsWith(".scss")) return isLanguageEnabled("SCSS");
        if (fileName.endsWith(".sass")) return isLanguageEnabled("Sass");
        if (fileName.endsWith(".less")) return isLanguageEnabled("Less");
        if (fileName.endsWith(".sql")) return isLanguageEnabled("SQL");
        if (fileName.endsWith(".md") || fileName.endsWith(".markdown")) return isLanguageEnabled("Markdown");
        if (fileName.endsWith(".proto")) return isLanguageEnabled("protobuf");

        return showBreadcrumbs;
    }

    public boolean isShowBreadcrumbs() {
        return showBreadcrumbs;
    }

    public void setShowBreadcrumbs(boolean showBreadcrumbs) {
        this.showBreadcrumbs = showBreadcrumbs;
    }

    public BreadcrumbsPlacement getPlacement() {
        return placement;
    }

    public void setPlacement(BreadcrumbsPlacement placement) {
        this.placement = placement != null ? placement : BreadcrumbsPlacement.BOTTOM;
    }

    public Map<String, Boolean> getLanguages() {
        return languages;
    }
}
