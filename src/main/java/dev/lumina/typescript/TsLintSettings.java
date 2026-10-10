package dev.lumina.typescript;

import java.util.Objects;

/**
 * Model for Languages & Frameworks > TypeScript > TSLint in Lumina IDE.
 * Matches reference screenshot media_1791602885285_4debafea.png:
 *  - Mode: Disable TSLint | Automatic TSLint configuration | Manual TSLint configuration
 *  - Manual fields: TSLint package, Configuration file, Rules directory
 */
public class TsLintSettings implements Cloneable {

    public static final String MODE_DISABLE = "Disable";
    public static final String MODE_AUTOMATIC = "Automatic";
    public static final String MODE_MANUAL = "Manual";

    private String mode = MODE_DISABLE;
    private String tslintPackage = "";
    private String configFile = "";
    private String rulesDirectory = "";

    public TsLintSettings() {
    }

    public TsLintSettings(TsLintSettings other) {
        if (other != null) {
            this.mode = other.mode;
            this.tslintPackage = other.tslintPackage;
            this.configFile = other.configFile;
            this.rulesDirectory = other.rulesDirectory;
        }
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        if (MODE_AUTOMATIC.equalsIgnoreCase(mode)) {
            this.mode = MODE_AUTOMATIC;
        } else if (MODE_MANUAL.equalsIgnoreCase(mode)) {
            this.mode = MODE_MANUAL;
        } else {
            this.mode = MODE_DISABLE;
        }
    }

    public String getTslintPackage() {
        return tslintPackage;
    }

    public void setTslintPackage(String tslintPackage) {
        this.tslintPackage = tslintPackage != null ? tslintPackage : "";
    }

    public String getConfigFile() {
        return configFile;
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile != null ? configFile : "";
    }

    public String getRulesDirectory() {
        return rulesDirectory;
    }

    public void setRulesDirectory(String rulesDirectory) {
        this.rulesDirectory = rulesDirectory != null ? rulesDirectory : "";
    }

    @Override
    public TsLintSettings clone() {
        return new TsLintSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TsLintSettings that = (TsLintSettings) o;
        return Objects.equals(mode, that.mode) &&
                Objects.equals(tslintPackage, that.tslintPackage) &&
                Objects.equals(configFile, that.configFile) &&
                Objects.equals(rulesDirectory, that.rulesDirectory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, tslintPackage, configFile, rulesDirectory);
    }
}
