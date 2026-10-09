package dev.lumina.micronaut;

import java.util.Objects;

/**
 * Settings model for Micronaut framework support in Lumina IDE.
 */
public class MicronautSettings {

    private boolean createRunConfigurationAutomatically = true;

    public MicronautSettings() {
    }

    public MicronautSettings(boolean createRunConfigurationAutomatically) {
        this.createRunConfigurationAutomatically = createRunConfigurationAutomatically;
    }

    public MicronautSettings(MicronautSettings other) {
        if (other != null) {
            this.createRunConfigurationAutomatically = other.createRunConfigurationAutomatically;
        }
    }

    public MicronautSettings copy() {
        return new MicronautSettings(this);
    }

    public boolean isCreateRunConfigurationAutomatically() {
        return createRunConfigurationAutomatically;
    }

    public void setCreateRunConfigurationAutomatically(boolean createRunConfigurationAutomatically) {
        this.createRunConfigurationAutomatically = createRunConfigurationAutomatically;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MicronautSettings that = (MicronautSettings) o;
        return createRunConfigurationAutomatically == that.createRunConfigurationAutomatically;
    }

    @Override
    public int hashCode() {
        return Objects.hash(createRunConfigurationAutomatically);
    }
}
