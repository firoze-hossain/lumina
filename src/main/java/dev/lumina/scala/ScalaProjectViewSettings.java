package dev.lumina.scala;

import java.util.Objects;

/**
 * Model for Languages & Frameworks > Scala > Project View settings.
 * Matches Image 1:
 *  - Group package object with package (default: false)
 *  - Highlight nodes with errors (default: false)
 */
public class ScalaProjectViewSettings {

    private boolean groupPackageObjectWithPackage = false;
    private boolean highlightNodesWithErrors = false;

    public ScalaProjectViewSettings() {
    }

    public ScalaProjectViewSettings(boolean groupPackageObjectWithPackage, boolean highlightNodesWithErrors) {
        this.groupPackageObjectWithPackage = groupPackageObjectWithPackage;
        this.highlightNodesWithErrors = highlightNodesWithErrors;
    }

    public boolean isGroupPackageObjectWithPackage() {
        return groupPackageObjectWithPackage;
    }

    public void setGroupPackageObjectWithPackage(boolean groupPackageObjectWithPackage) {
        this.groupPackageObjectWithPackage = groupPackageObjectWithPackage;
    }

    public boolean isHighlightNodesWithErrors() {
        return highlightNodesWithErrors;
    }

    public void setHighlightNodesWithErrors(boolean highlightNodesWithErrors) {
        this.highlightNodesWithErrors = highlightNodesWithErrors;
    }

    public ScalaProjectViewSettings copy() {
        return new ScalaProjectViewSettings(groupPackageObjectWithPackage, highlightNodesWithErrors);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScalaProjectViewSettings that = (ScalaProjectViewSettings) o;
        return groupPackageObjectWithPackage == that.groupPackageObjectWithPackage &&
                highlightNodesWithErrors == that.highlightNodesWithErrors;
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupPackageObjectWithPackage, highlightNodesWithErrors);
    }

    @Override
    public String toString() {
        return "ScalaProjectViewSettings{" +
                "groupPackageObjectWithPackage=" + groupPackageObjectWithPackage +
                ", highlightNodesWithErrors=" + highlightNodesWithErrors +
                '}';
    }
}
