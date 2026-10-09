package dev.lumina.lombok;

import java.util.Objects;

/**
 * Settings model for Lombok support in Lumina IDE.
 */
public class LombokSettings {

    private boolean autoAddTrackApDependencies = true;

    public LombokSettings() {
    }

    public LombokSettings(boolean autoAddTrackApDependencies) {
        this.autoAddTrackApDependencies = autoAddTrackApDependencies;
    }

    public LombokSettings(LombokSettings other) {
        if (other != null) {
            this.autoAddTrackApDependencies = other.autoAddTrackApDependencies;
        }
    }

    public LombokSettings copy() {
        return new LombokSettings(this);
    }

    public boolean isAutoAddTrackApDependencies() {
        return autoAddTrackApDependencies;
    }

    public void setAutoAddTrackApDependencies(boolean autoAddTrackApDependencies) {
        this.autoAddTrackApDependencies = autoAddTrackApDependencies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LombokSettings that = (LombokSettings) o;
        return autoAddTrackApDependencies == that.autoAddTrackApDependencies;
    }

    @Override
    public int hashCode() {
        return Objects.hash(autoAddTrackApDependencies);
    }
}
