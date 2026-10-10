package dev.lumina.stylesheets;

import java.util.Objects;

/**
 * Model for Languages & Frameworks > Style Sheets > Stylelint settings in Lumina IDE.
 * Matches reference screenshot media_1791600461236_fc374953.png:
 *  - [ ] Enable (default: false)
 *  - Stylelint package: ""
 *  - Configuration file: "Auto-detect"
 *  - Run for files: "**\/*.{css}"
 *  - [ ] Run stylelint --fix on save (default: false)
 */
public class StylelintSettings implements Cloneable {

    private boolean enabled = false;
    private String packagePath = "";
    private String configurationFile = "Auto-detect";
    private String runForFiles = "**/*.{css}";
    private boolean fixOnSave = false;

    public StylelintSettings() {
    }

    public StylelintSettings(StylelintSettings other) {
        if (other != null) {
            this.enabled = other.enabled;
            this.packagePath = other.packagePath != null ? other.packagePath : "";
            this.configurationFile = other.configurationFile != null ? other.configurationFile : "Auto-detect";
            this.runForFiles = other.runForFiles != null ? other.runForFiles : "**/*.{css}";
            this.fixOnSave = other.fixOnSave;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPackagePath() {
        return packagePath;
    }

    public void setPackagePath(String packagePath) {
        this.packagePath = packagePath != null ? packagePath : "";
    }

    public String getConfigurationFile() {
        return configurationFile;
    }

    public void setConfigurationFile(String configurationFile) {
        this.configurationFile = configurationFile != null ? configurationFile : "Auto-detect";
    }

    public String getRunForFiles() {
        return runForFiles;
    }

    public void setRunForFiles(String runForFiles) {
        this.runForFiles = runForFiles != null ? runForFiles : "**/*.{css}";
    }

    public boolean isFixOnSave() {
        return fixOnSave;
    }

    public void setFixOnSave(boolean fixOnSave) {
        this.fixOnSave = fixOnSave;
    }

    @Override
    public StylelintSettings clone() {
        return new StylelintSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StylelintSettings that = (StylelintSettings) o;
        return enabled == that.enabled &&
                fixOnSave == that.fixOnSave &&
                Objects.equals(packagePath, that.packagePath) &&
                Objects.equals(configurationFile, that.configurationFile) &&
                Objects.equals(runForFiles, that.runForFiles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, packagePath, configurationFile, runForFiles, fixOnSave);
    }

    @Override
    public String toString() {
        return "StylelintSettings{" +
                "enabled=" + enabled +
                ", packagePath='" + packagePath + '\'' +
                ", configurationFile='" + configurationFile + '\'' +
                ", runForFiles='" + runForFiles + '\'' +
                ", fixOnSave=" + fixOnSave +
                '}';
    }
}
