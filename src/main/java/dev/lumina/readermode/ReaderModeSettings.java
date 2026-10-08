package dev.lumina.readermode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Encapsulates the configuration state of Reader Mode in Lumina IDE.
 * Supports built-in toggles as well as dynamically contributed option states.
 */
public class ReaderModeSettings {

    private boolean enabled = true;
    private boolean renderedDocs = true;
    private boolean errorHighlighting = false;
    private boolean fontLigatures = false;
    private boolean increasedLineHeight = false;
    private double lineHeightMultiplier = 1.2;
    private boolean codeVisionHints = true;
    private boolean formatCode = true;
    private boolean useActiveScheme = true;
    private String chosenScheme = "Default IDE";

    // Dynamic options contributed by plugins/extensions
    private final Map<String, Boolean> dynamicOptions = new LinkedHashMap<>();

    public ReaderModeSettings() {
    }

    public ReaderModeSettings(ReaderModeSettings other) {
        if (other != null) {
            this.enabled = other.enabled;
            this.renderedDocs = other.renderedDocs;
            this.errorHighlighting = other.errorHighlighting;
            this.fontLigatures = other.fontLigatures;
            this.increasedLineHeight = other.increasedLineHeight;
            this.lineHeightMultiplier = other.lineHeightMultiplier;
            this.codeVisionHints = other.codeVisionHints;
            this.formatCode = other.formatCode;
            this.useActiveScheme = other.useActiveScheme;
            this.chosenScheme = other.chosenScheme;
            this.dynamicOptions.putAll(other.dynamicOptions);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRenderedDocs() {
        return renderedDocs;
    }

    public void setRenderedDocs(boolean renderedDocs) {
        this.renderedDocs = renderedDocs;
    }

    public boolean isErrorHighlighting() {
        return errorHighlighting;
    }

    public void setErrorHighlighting(boolean errorHighlighting) {
        this.errorHighlighting = errorHighlighting;
    }

    public boolean isFontLigatures() {
        return fontLigatures;
    }

    public void setFontLigatures(boolean fontLigatures) {
        this.fontLigatures = fontLigatures;
    }

    public boolean isIncreasedLineHeight() {
        return increasedLineHeight;
    }

    public void setIncreasedLineHeight(boolean increasedLineHeight) {
        this.increasedLineHeight = increasedLineHeight;
    }

    public double getLineHeightMultiplier() {
        return lineHeightMultiplier;
    }

    public void setLineHeightMultiplier(double lineHeightMultiplier) {
        this.lineHeightMultiplier = lineHeightMultiplier;
    }

    public boolean isCodeVisionHints() {
        return codeVisionHints;
    }

    public void setCodeVisionHints(boolean codeVisionHints) {
        this.codeVisionHints = codeVisionHints;
    }

    public boolean isFormatCode() {
        return formatCode;
    }

    public void setFormatCode(boolean formatCode) {
        this.formatCode = formatCode;
    }

    public boolean isUseActiveScheme() {
        return useActiveScheme;
    }

    public void setUseActiveScheme(boolean useActiveScheme) {
        this.useActiveScheme = useActiveScheme;
    }

    public String getChosenScheme() {
        return chosenScheme;
    }

    public void setChosenScheme(String chosenScheme) {
        this.chosenScheme = chosenScheme != null ? chosenScheme : "Default IDE";
    }

    public Map<String, Boolean> getDynamicOptions() {
        return dynamicOptions;
    }

    public boolean getDynamicOption(String optionId, boolean defaultValue) {
        return dynamicOptions.getOrDefault(optionId, defaultValue);
    }

    public void setDynamicOption(String optionId, boolean value) {
        dynamicOptions.put(optionId, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReaderModeSettings that)) return false;
        return enabled == that.enabled &&
                renderedDocs == that.renderedDocs &&
                errorHighlighting == that.errorHighlighting &&
                fontLigatures == that.fontLigatures &&
                increasedLineHeight == that.increasedLineHeight &&
                Double.compare(that.lineHeightMultiplier, lineHeightMultiplier) == 0 &&
                codeVisionHints == that.codeVisionHints &&
                formatCode == that.formatCode &&
                useActiveScheme == that.useActiveScheme &&
                Objects.equals(chosenScheme, that.chosenScheme) &&
                Objects.equals(dynamicOptions, that.dynamicOptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, renderedDocs, errorHighlighting, fontLigatures,
                increasedLineHeight, lineHeightMultiplier, codeVisionHints, formatCode,
                useActiveScheme, chosenScheme, dynamicOptions);
    }
}
