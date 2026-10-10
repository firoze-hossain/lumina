package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing file type to external diff/merge tool association in Lumina IDE.
 */
public class ExternalDiffToolAssociation implements Cloneable {

    private String fileType;
    private String diffTool;
    private String mergeTool;

    public ExternalDiffToolAssociation() {
        this("Default", "Built-in", "Built-in");
    }

    public ExternalDiffToolAssociation(String fileType, String diffTool, String mergeTool) {
        this.fileType = fileType != null ? fileType : "Default";
        this.diffTool = diffTool != null ? diffTool : "Built-in";
        this.mergeTool = mergeTool != null ? mergeTool : "Built-in";
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType != null ? fileType : "Default";
    }

    public String getDiffTool() {
        return diffTool;
    }

    public void setDiffTool(String diffTool) {
        this.diffTool = diffTool != null ? diffTool : "Built-in";
    }

    public String getMergeTool() {
        return mergeTool;
    }

    public void setMergeTool(String mergeTool) {
        this.mergeTool = mergeTool != null ? mergeTool : "Built-in";
    }

    @Override
    public ExternalDiffToolAssociation clone() {
        try {
            return (ExternalDiffToolAssociation) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExternalDiffToolAssociation that = (ExternalDiffToolAssociation) o;
        return Objects.equals(fileType, that.fileType) &&
                Objects.equals(diffTool, that.diffTool) &&
                Objects.equals(mergeTool, that.mergeTool);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileType, diffTool, mergeTool);
    }
}
