package dev.lumina.settings;

import dev.lumina.util.Settings;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic persistent configuration model for Editor > General > Sticky Lines settings.
 * Backed by ~/.lumina/lumina.properties, supporting listeners, dirty tracking, and real-time updates.
 */
public final class StickyLinesSettings {

    public static final List<String> COLUMN_1_LANGUAGES = List.of(
            "CSS", "ERB", "FreeMarker", "Go", "Groovy", "HTML", "Java", "JavaScript", "JSON", "JSP"
    );

    public static final List<String> COLUMN_2_LANGUAGES = List.of(
            "JSPX", "Jupyter", "Kotlin", "Less", "Markdown", "PHP", "protobuf", "Python", "Ruby", "Rust"
    );

    public static final List<String> COLUMN_3_LANGUAGES = List.of(
            "Sass", "Scala", "SCSS", "SQL", "TypeScript", "VTL", "XHTML", "XML", "YAML"
    );

    public static final List<String> ALL_LANGUAGES;
    static {
        List<String> all = new ArrayList<>();
        all.addAll(COLUMN_1_LANGUAGES);
        all.addAll(COLUMN_2_LANGUAGES);
        all.addAll(COLUMN_3_LANGUAGES);
        ALL_LANGUAGES = Collections.unmodifiableList(all);
    }

    private static final StickyLinesSettings INSTANCE = new StickyLinesSettings();

    public static StickyLinesSettings getInstance() {
        return INSTANCE;
    }

    @FunctionalInterface
    public interface Listener {
        void onSettingsChanged(StickyLinesSettings settings);
    }

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private boolean showStickyLines = true;
    private int maxLines = 5;
    private final Set<String> enabledLanguages = new LinkedHashSet<>();

    public StickyLinesSettings() {
        initDefaults();
        load();
    }

    public void initDefaults() {
        showStickyLines = true;
        maxLines = 5;
        enabledLanguages.clear();
        enabledLanguages.addAll(ALL_LANGUAGES);
    }

    public void load() {
        String val = Settings.get("stickylines.enabled");
        if (val != null) {
            showStickyLines = Boolean.parseBoolean(val);
        }

        val = Settings.get("stickylines.max.lines");
        if (val != null) {
            try {
                maxLines = Math.max(1, Math.min(50, Integer.parseInt(val.trim())));
            } catch (NumberFormatException ignored) {}
        }

        val = Settings.get("stickylines.languages");
        if (val != null) {
            enabledLanguages.clear();
            if (!val.isBlank()) {
                String[] parts = val.split(",");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        enabledLanguages.add(trimmed);
                    }
                }
            }
        }
    }

    public void save() {
        Settings.put("stickylines.enabled", String.valueOf(showStickyLines));
        Settings.put("stickylines.max.lines", String.valueOf(maxLines));
        Settings.put("stickylines.languages", String.join(",", enabledLanguages));

        notifyListeners();
    }

    public StickyLinesSettings copy() {
        StickyLinesSettings clone = new StickyLinesSettings();
        clone.applyFrom(this);
        return clone;
    }

    public void applyFrom(StickyLinesSettings other) {
        if (other == null) return;
        this.showStickyLines = other.showStickyLines;
        this.maxLines = other.maxLines;
        this.enabledLanguages.clear();
        this.enabledLanguages.addAll(other.enabledLanguages);
    }

    public boolean isModified(StickyLinesSettings other) {
        if (other == null) return true;
        return this.showStickyLines != other.showStickyLines
                || this.maxLines != other.maxLines
                || !this.enabledLanguages.equals(other.enabledLanguages);
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

    // --- Getters & Setters ---

    public boolean isShowStickyLines() {
        return showStickyLines;
    }

    public void setShowStickyLines(boolean showStickyLines) {
        this.showStickyLines = showStickyLines;
    }

    public int getMaxLines() {
        return maxLines;
    }

    public void setMaxLines(int maxLines) {
        this.maxLines = Math.max(1, Math.min(50, maxLines));
    }

    public Set<String> getEnabledLanguages() {
        return Collections.unmodifiableSet(enabledLanguages);
    }

    public void setEnabledLanguages(Collection<String> languages) {
        this.enabledLanguages.clear();
        if (languages != null) {
            this.enabledLanguages.addAll(languages);
        }
    }

    public boolean isLanguageEnabled(String language) {
        return enabledLanguages.contains(language);
    }

    public void setLanguageEnabled(String language, boolean enabled) {
        if (language == null) return;
        if (enabled) {
            enabledLanguages.add(language);
        } else {
            enabledLanguages.remove(language);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StickyLinesSettings that = (StickyLinesSettings) o;
        return !isModified(that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(showStickyLines, maxLines, enabledLanguages);
    }
}
