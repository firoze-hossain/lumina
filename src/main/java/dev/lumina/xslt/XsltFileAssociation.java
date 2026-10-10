package dev.lumina.xslt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents an association between an XSLT file and its associated data/schema files.
 */
public class XsltFileAssociation implements Cloneable {

    private String xsltFilePath = "";
    private List<String> associatedFiles = new ArrayList<>();

    public XsltFileAssociation() {
    }

    public XsltFileAssociation(String xsltFilePath, List<String> associatedFiles) {
        this.xsltFilePath = xsltFilePath != null ? xsltFilePath : "";
        this.associatedFiles = associatedFiles != null ? new ArrayList<>(associatedFiles) : new ArrayList<>();
    }

    public XsltFileAssociation(XsltFileAssociation other) {
        if (other != null) {
            this.xsltFilePath = other.xsltFilePath;
            this.associatedFiles = other.associatedFiles != null ? new ArrayList<>(other.associatedFiles) : new ArrayList<>();
        }
    }

    public String getXsltFilePath() {
        return xsltFilePath;
    }

    public void setXsltFilePath(String xsltFilePath) {
        this.xsltFilePath = xsltFilePath != null ? xsltFilePath : "";
    }

    public List<String> getAssociatedFiles() {
        return associatedFiles;
    }

    public void setAssociatedFiles(List<String> associatedFiles) {
        this.associatedFiles = associatedFiles != null ? new ArrayList<>(associatedFiles) : new ArrayList<>();
    }

    public void addAssociatedFile(String file) {
        if (file != null && !file.isBlank() && !associatedFiles.contains(file)) {
            associatedFiles.add(file);
        }
    }

    public void removeAssociatedFile(String file) {
        associatedFiles.remove(file);
    }

    @Override
    public XsltFileAssociation clone() {
        return new XsltFileAssociation(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        XsltFileAssociation that = (XsltFileAssociation) o;
        return Objects.equals(xsltFilePath, that.xsltFilePath) &&
                Objects.equals(associatedFiles, that.associatedFiles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(xsltFilePath, associatedFiles);
    }
}
