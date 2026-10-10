package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing Tools > RuboCop configuration settings in Lumina IDE.
 */
public class RuboCopSettings implements Cloneable {

    private String configFile = "";
    private boolean useStandardGem = false;
    private boolean runRuboCopOnSave = false;

    public RuboCopSettings() {
    }

    public String getConfigFile() {
        return configFile != null ? configFile : "";
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile != null ? configFile.trim() : "";
    }

    public boolean isUseStandardGem() {
        return useStandardGem;
    }

    public void setUseStandardGem(boolean useStandardGem) {
        this.useStandardGem = useStandardGem;
    }

    public boolean isRunRuboCopOnSave() {
        return runRuboCopOnSave;
    }

    public void setRunRuboCopOnSave(boolean runRuboCopOnSave) {
        this.runRuboCopOnSave = runRuboCopOnSave;
    }

    @Override
    public RuboCopSettings clone() {
        try {
            return (RuboCopSettings) super.clone();
        } catch (CloneNotSupportedException e) {
            RuboCopSettings copy = new RuboCopSettings();
            copy.configFile = this.configFile;
            copy.useStandardGem = this.useStandardGem;
            copy.runRuboCopOnSave = this.runRuboCopOnSave;
            return copy;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuboCopSettings that = (RuboCopSettings) o;
        return useStandardGem == that.useStandardGem &&
                runRuboCopOnSave == that.runRuboCopOnSave &&
                Objects.equals(configFile, that.configFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configFile, useStandardGem, runRuboCopOnSave);
    }
}
