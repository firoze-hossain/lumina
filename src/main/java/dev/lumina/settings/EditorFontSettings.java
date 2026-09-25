package dev.lumina.settings;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic persistent configuration and model for Editor > Font settings.
 * Backed by ~/.lumina/lumina.properties, supporting dynamic listeners,
 * real-time UI synchronization, and live editor font updates.
 */
public final class EditorFontSettings {

    private static final EditorFontSettings INSTANCE = new EditorFontSettings();

    public static EditorFontSettings getInstance() {
        return INSTANCE;
    }

    @FunctionalInterface
    public interface Listener {
        void onSettingsChanged(EditorFontSettings settings);
    }

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private String fontFamily = "JetBrains Mono";
    private double fontSize = 13.0;
    private double lineHeight = 1.2;
    private boolean enableLigatures = false;

    // Typography Settings
    private String mainWeight = "Regular";
    private String boldWeight = "Bold Recommended";
    private String fallbackFont = "<None>";

    public EditorFontSettings() {
        initDefaults();
        load();
    }

    public void initDefaults() {
        fontFamily = "JetBrains Mono";
        fontSize = 13.0;
        lineHeight = 1.2;
        enableLigatures = false;

        mainWeight = "Regular";
        boldWeight = "Bold Recommended";
        fallbackFont = "<None>";
    }

    public void load() {
        String val;

        val = Settings.get("editor.font.family");
        if (val != null && !val.isBlank()) fontFamily = val;

        val = Settings.get("editor.font.size");
        if (val != null) {
            try { fontSize = Double.parseDouble(val); } catch (NumberFormatException ignored) {}
        }

        val = Settings.get("editor.font.line.height");
        if (val != null) {
            try { lineHeight = Double.parseDouble(val); } catch (NumberFormatException ignored) {}
        }

        val = Settings.get("editor.font.enable.ligatures");
        if (val != null) enableLigatures = Boolean.parseBoolean(val);

        val = Settings.get("editor.font.main.weight");
        if (val != null && !val.isBlank()) mainWeight = val;

        val = Settings.get("editor.font.bold.weight");
        if (val != null && !val.isBlank()) boldWeight = val;

        val = Settings.get("editor.font.fallback");
        if (val != null && !val.isBlank()) fallbackFont = val;
    }

    public void save() {
        Settings.put("editor.font.family", fontFamily);
        Settings.put("editor.font.size", String.valueOf(fontSize));
        Settings.put("editor.font.line.height", String.valueOf(lineHeight));
        Settings.put("editor.font.enable.ligatures", String.valueOf(enableLigatures));
        Settings.put("editor.font.main.weight", mainWeight);
        Settings.put("editor.font.bold.weight", boldWeight);
        Settings.put("editor.font.fallback", fallbackFont);

        notifyListeners();
    }

    public EditorFontSettings copy() {
        EditorFontSettings clone = new EditorFontSettings();
        clone.applyFrom(this);
        return clone;
    }

    public void applyFrom(EditorFontSettings other) {
        if (other == null) return;
        this.fontFamily = other.fontFamily;
        this.fontSize = other.fontSize;
        this.lineHeight = other.lineHeight;
        this.enableLigatures = other.enableLigatures;
        this.mainWeight = other.mainWeight;
        this.boldWeight = other.boldWeight;
        this.fallbackFont = other.fallbackFont;
    }

    public boolean isModified(EditorFontSettings other) {
        if (other == null) return true;
        return !Objects.equals(this.fontFamily, other.fontFamily)
                || Double.compare(this.fontSize, other.fontSize) != 0
                || Double.compare(this.lineHeight, other.lineHeight) != 0
                || this.enableLigatures != other.enableLigatures
                || !Objects.equals(this.mainWeight, other.mainWeight)
                || !Objects.equals(this.boldWeight, other.boldWeight)
                || !Objects.equals(this.fallbackFont, other.fallbackFont);
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

    // Getters & Setters
    public String getFontFamily() { return fontFamily; }
    public void setFontFamily(String val) { this.fontFamily = val; }

    public double getFontSize() { return fontSize; }
    public void setFontSize(double val) { this.fontSize = val; }

    public double getLineHeight() { return lineHeight; }
    public void setLineHeight(double val) { this.lineHeight = val; }

    public boolean isEnableLigatures() { return enableLigatures; }
    public void setEnableLigatures(boolean val) { this.enableLigatures = val; }

    public String getMainWeight() { return mainWeight; }
    public void setMainWeight(String val) { this.mainWeight = val; }

    public String getBoldWeight() { return boldWeight; }
    public void setBoldWeight(String val) { this.boldWeight = val; }

    public String getFallbackFont() { return fallbackFont; }
    public void setFallbackFont(String val) { this.fallbackFont = val; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EditorFontSettings that = (EditorFontSettings) o;
        return !isModified(that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fontFamily, fontSize, lineHeight, enableLigatures, mainWeight, boldWeight, fallbackFont);
    }
}
