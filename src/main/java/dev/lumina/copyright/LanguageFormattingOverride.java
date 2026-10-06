package dev.lumina.copyright;

import java.util.Objects;

/**
 * Represents per-language copyright formatting override settings,
 * mirroring IntelliJ IDEA's Editor > Copyright > Formatting > [Language] options.
 */
public class LanguageFormattingOverride {

    public enum Mode {
        NO_COPYRIGHT,
        USE_DEFAULT,
        USE_CUSTOM
    }

    private String language;
    private Mode mode = Mode.USE_DEFAULT;
    private CopyrightFormattingOptions customOptions = new CopyrightFormattingOptions();

    public LanguageFormattingOverride() {
    }

    public LanguageFormattingOverride(String language, Mode mode) {
        this.language = language;
        this.mode = mode != null ? mode : Mode.USE_DEFAULT;
    }

    public LanguageFormattingOverride(String language, Mode mode, CopyrightFormattingOptions customOptions) {
        this.language = language;
        this.mode = mode != null ? mode : Mode.USE_DEFAULT;
        if (customOptions != null) {
            this.customOptions = customOptions.copy();
        }
    }

    public String getLanguage() {
        return language != null ? language : "";
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Mode getMode() {
        return mode != null ? mode : Mode.USE_DEFAULT;
    }

    public void setMode(Mode mode) {
        this.mode = mode != null ? mode : Mode.USE_DEFAULT;
    }

    public CopyrightFormattingOptions getCustomOptions() {
        if (customOptions == null) {
            customOptions = new CopyrightFormattingOptions();
        }
        return customOptions;
    }

    public void setCustomOptions(CopyrightFormattingOptions customOptions) {
        this.customOptions = customOptions != null ? customOptions.copy() : new CopyrightFormattingOptions();
    }

    public LanguageFormattingOverride copy() {
        LanguageFormattingOverride clone = new LanguageFormattingOverride();
        clone.language = this.language;
        clone.mode = this.mode;
        clone.customOptions = this.customOptions != null ? this.customOptions.copy() : new CopyrightFormattingOptions();
        return clone;
    }

    public boolean isEquivalentTo(LanguageFormattingOverride other) {
        if (other == null) return false;
        return Objects.equals(this.language, other.language)
                && this.mode == other.mode
                && (this.customOptions == null ? other.customOptions == null : this.customOptions.isEquivalentTo(other.customOptions));
    }
}
