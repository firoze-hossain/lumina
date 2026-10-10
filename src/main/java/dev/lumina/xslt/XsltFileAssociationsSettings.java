package dev.lumina.xslt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model for Languages & Frameworks > XSLT File Associations settings in Lumina IDE.
 * Matches reference screenshots media_1791604079444_011b2914.png & media_1791604099408_0878138e.png.
 */
public class XsltFileAssociationsSettings implements Cloneable {

    private List<XsltFileAssociation> associations = new ArrayList<>();

    public XsltFileAssociationsSettings() {
    }

    public XsltFileAssociationsSettings(XsltFileAssociationsSettings other) {
        if (other != null && other.associations != null) {
            this.associations = new ArrayList<>();
            for (XsltFileAssociation a : other.associations) {
                this.associations.add(a.clone());
            }
        }
    }

    public List<XsltFileAssociation> getAssociations() {
        return associations;
    }

    public void setAssociations(List<XsltFileAssociation> associations) {
        this.associations = associations != null ? new ArrayList<>(associations) : new ArrayList<>();
    }

    public XsltFileAssociation getAssociationFor(String xsltPath) {
        if (xsltPath == null) return null;
        for (XsltFileAssociation a : associations) {
            if (xsltPath.equals(a.getXsltFilePath())) {
                return a;
            }
        }
        return null;
    }

    public void setAssociatedFiles(String xsltPath, List<String> files) {
        if (xsltPath == null) return;
        XsltFileAssociation existing = getAssociationFor(xsltPath);
        if (existing != null) {
            existing.setAssociatedFiles(files);
        } else {
            associations.add(new XsltFileAssociation(xsltPath, files));
        }
    }

    @Override
    public XsltFileAssociationsSettings clone() {
        return new XsltFileAssociationsSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        XsltFileAssociationsSettings that = (XsltFileAssociationsSettings) o;
        return Objects.equals(associations, that.associations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(associations);
    }
}
