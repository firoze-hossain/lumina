package dev.lumina.javascript;

import java.util.Objects;

/**
 * Model representing Prettier configuration in Lumina IDE.
 */
public class PrettierSettings {

    public enum Mode {
        DISABLED,
        AUTOMATIC,
        MANUAL
    }

    private Mode mode = Mode.DISABLED;
    private String runForFiles = "**/*.{js,ts,jsx,tsx,cjs,cts,mjs,mts,json,vue,astro}";
    private boolean runOnSave = false;
    private boolean runOnPaste = true;
    private boolean preferPrettierToIdeCodeStyle = true;
    private String prettierPackage = "node_modules/prettier";
    private boolean customConfigurationFile = false;
    private String configurationFile = "";

    public PrettierSettings() {
    }

    public PrettierSettings(Mode mode, String runForFiles, boolean runOnSave, boolean runOnPaste, boolean preferPrettierToIdeCodeStyle) {
        this.mode = mode != null ? mode : Mode.DISABLED;
        this.runForFiles = runForFiles != null ? runForFiles : "**/*.{js,ts,jsx,tsx,cjs,cts,mjs,mts,json,vue,astro}";
        this.runOnSave = runOnSave;
        this.runOnPaste = runOnPaste;
        this.preferPrettierToIdeCodeStyle = preferPrettierToIdeCodeStyle;
    }

    public PrettierSettings copy() {
        PrettierSettings copy = new PrettierSettings(mode, runForFiles, runOnSave, runOnPaste, preferPrettierToIdeCodeStyle);
        copy.setPrettierPackage(prettierPackage);
        copy.setCustomConfigurationFile(customConfigurationFile);
        copy.setConfigurationFile(configurationFile);
        return copy;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode != null ? mode : Mode.DISABLED;
    }

    public String getRunForFiles() {
        return runForFiles;
    }

    public void setRunForFiles(String runForFiles) {
        this.runForFiles = runForFiles != null ? runForFiles : "**/*.{js,ts,jsx,tsx,cjs,cts,mjs,mts,json,vue,astro}";
    }

    public boolean isRunOnSave() {
        return runOnSave;
    }

    public void setRunOnSave(boolean runOnSave) {
        this.runOnSave = runOnSave;
    }

    public boolean isRunOnPaste() {
        return runOnPaste;
    }

    public void setRunOnPaste(boolean runOnPaste) {
        this.runOnPaste = runOnPaste;
    }

    public boolean isPreferPrettierToIdeCodeStyle() {
        return preferPrettierToIdeCodeStyle;
    }

    public void setPreferPrettierToIdeCodeStyle(boolean preferPrettierToIdeCodeStyle) {
        this.preferPrettierToIdeCodeStyle = preferPrettierToIdeCodeStyle;
    }

    public String getPrettierPackage() {
        return prettierPackage;
    }

    public void setPrettierPackage(String prettierPackage) {
        this.prettierPackage = prettierPackage != null ? prettierPackage : "node_modules/prettier";
    }

    public boolean isCustomConfigurationFile() {
        return customConfigurationFile;
    }

    public void setCustomConfigurationFile(boolean customConfigurationFile) {
        this.customConfigurationFile = customConfigurationFile;
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
        if (!(o instanceof PrettierSettings that)) return false;
        return runOnSave == that.runOnSave &&
                runOnPaste == that.runOnPaste &&
                preferPrettierToIdeCodeStyle == that.preferPrettierToIdeCodeStyle &&
                customConfigurationFile == that.customConfigurationFile &&
                mode == that.mode &&
                Objects.equals(runForFiles, that.runForFiles) &&
                Objects.equals(prettierPackage, that.prettierPackage) &&
                Objects.equals(configurationFile, that.configurationFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, runForFiles, runOnSave, runOnPaste, preferPrettierToIdeCodeStyle,
                prettierPackage, customConfigurationFile, configurationFile);
    }
}
