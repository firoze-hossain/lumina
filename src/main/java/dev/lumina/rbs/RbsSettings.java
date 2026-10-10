package dev.lumina.rbs;

import java.util.Objects;

/**
 * Settings model for Languages & Frameworks > RBS in Lumina IDE.
 */
public class RbsSettings {

    private boolean improvedTypeSupportWithRbsCollection = false;

    public RbsSettings() {
    }

    public RbsSettings(boolean improvedTypeSupportWithRbsCollection) {
        this.improvedTypeSupportWithRbsCollection = improvedTypeSupportWithRbsCollection;
    }

    public RbsSettings(RbsSettings other) {
        if (other != null) {
            this.improvedTypeSupportWithRbsCollection = other.improvedTypeSupportWithRbsCollection;
        }
    }

    public RbsSettings copy() {
        return new RbsSettings(this);
    }

    public boolean isImprovedTypeSupportWithRbsCollection() {
        return improvedTypeSupportWithRbsCollection;
    }

    public void setImprovedTypeSupportWithRbsCollection(boolean improvedTypeSupportWithRbsCollection) {
        this.improvedTypeSupportWithRbsCollection = improvedTypeSupportWithRbsCollection;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RbsSettings that = (RbsSettings) o;
        return improvedTypeSupportWithRbsCollection == that.improvedTypeSupportWithRbsCollection;
    }

    @Override
    public int hashCode() {
        return Objects.hash(improvedTypeSupportWithRbsCollection);
    }
}
