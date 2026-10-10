package dev.lumina.quarkus;

import java.util.Objects;

/**
 * Settings model for Languages & Frameworks > Quarkus in Lumina IDE.
 */
public class QuarkusSettings {

    private boolean createRunConfigurationAutomatically = true;

    public QuarkusSettings() {
    }

    public QuarkusSettings(boolean createRunConfigurationAutomatically) {
        this.createRunConfigurationAutomatically = createRunConfigurationAutomatically;
    }

    public QuarkusSettings(QuarkusSettings other) {
        if (other != null) {
            this.createRunConfigurationAutomatically = other.createRunConfigurationAutomatically;
        }
    }

    public QuarkusSettings copy() {
        return new QuarkusSettings(this);
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
        QuarkusSettings that = (QuarkusSettings) o;
        return createRunConfigurationAutomatically == that.createRunConfigurationAutomatically;
    }

    @Override
    public int hashCode() {
        return Objects.hash(createRunConfigurationAutomatically);
    }
}
