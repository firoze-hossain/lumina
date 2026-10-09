package dev.lumina.javascript;

import java.util.Objects;

/**
 * Model representing ESLint configuration in Lumina IDE.
 */
public class ESLintSettings {

    public enum Mode {
        DISABLED,
        AUTOMATIC,
        MANUAL
    }

    private Mode mode = Mode.DISABLED;
    private String runForFiles = "{**/*,*}.{js,ts,jsx,tsx,cjs,cts,mjs,mts,html,vue}";
    private boolean runOnSave = false;
    private String nodeInterpreter = "Project";
    private String eslintPackage = "";
    private boolean customConfigurationFile = false;
    private String configurationFile = "";
    private String extraRulesDirectory = "";

    public ESLintSettings() {
    }

    public ESLintSettings(Mode mode, String runForFiles, boolean runOnSave) {
        this.mode = mode != null ? mode : Mode.DISABLED;
        this.runForFiles = runForFiles != null ? runForFiles : "{**/*,*}.{js,ts,jsx,tsx,cjs,cts,mjs,mts,html,vue}";
        this.runOnSave = runOnSave;
    }

    public ESLintSettings copy() {
        ESLintSettings copy = new ESLintSettings(mode, runForFiles, runOnSave);
        copy.setNodeInterpreter(nodeInterpreter);
        copy.setEslintPackage(eslintPackage);
        copy.setCustomConfigurationFile(customConfigurationFile);
        copy.setConfigurationFile(configurationFile);
        copy.setExtraRulesDirectory(extraRulesDirectory);
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
        this.runForFiles = runForFiles;
    }

    public boolean isRunOnSave() {
        return runOnSave;
    }

    public void setRunOnSave(boolean runOnSave) {
        this.runOnSave = runOnSave;
    }

    public String getNodeInterpreter() {
        return nodeInterpreter;
    }

    public void setNodeInterpreter(String nodeInterpreter) {
        this.nodeInterpreter = nodeInterpreter;
    }

    public String getEslintPackage() {
        return eslintPackage;
    }

    public void setEslintPackage(String eslintPackage) {
        this.eslintPackage = eslintPackage;
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
        this.configurationFile = configurationFile;
    }

    public String getExtraRulesDirectory() {
        return extraRulesDirectory;
    }

    public void setExtraRulesDirectory(String extraRulesDirectory) {
        this.extraRulesDirectory = extraRulesDirectory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ESLintSettings that)) return false;
        return runOnSave == that.runOnSave &&
                mode == that.mode &&
                customConfigurationFile == that.customConfigurationFile &&
                Objects.equals(runForFiles, that.runForFiles) &&
                Objects.equals(nodeInterpreter, that.nodeInterpreter) &&
                Objects.equals(eslintPackage, that.eslintPackage) &&
                Objects.equals(configurationFile, that.configurationFile) &&
                Objects.equals(extraRulesDirectory, that.extraRulesDirectory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, runForFiles, runOnSave, nodeInterpreter, eslintPackage,
                customConfigurationFile, configurationFile, extraRulesDirectory);
    }
}
