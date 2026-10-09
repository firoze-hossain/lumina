package dev.lumina.javascript;

import java.util.Objects;

/**
 * Model representing Vite module resolution configuration in Lumina IDE.
 */
public class ViteSettings {

    public enum Mode {
        DISABLED,
        AUTOMATIC,
        MANUAL
    }

    private Mode mode = Mode.AUTOMATIC;
    private String configurationFile = "";

    public ViteSettings() {
    }

    public ViteSettings(Mode mode, String configurationFile) {
        this.mode = mode != null ? mode : Mode.AUTOMATIC;
        this.configurationFile = configurationFile != null ? configurationFile : "";
    }

    public ViteSettings copy() {
        return new ViteSettings(mode, configurationFile);
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode != null ? mode : Mode.AUTOMATIC;
    }

    public String getConfigurationFile() {
        return configurationFile;
    }

    public void setConfigurationFile(String configurationFile) {
        this.configurationFile = configurationFile != null ? configurationFile : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ViteSettings that)) return false;
        return mode == that.mode && Objects.equals(configurationFile, that.configurationFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, configurationFile);
    }
}
