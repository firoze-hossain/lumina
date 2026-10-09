package dev.lumina.ktor;

import java.util.Objects;

/**
 * Settings model for Ktor framework in Lumina IDE.
 */
public class KtorSettings {

    private boolean createRunConfigurationAutomatically = false;

    public KtorSettings() {
    }

    public KtorSettings(boolean createRunConfigurationAutomatically) {
        this.createRunConfigurationAutomatically = createRunConfigurationAutomatically;
    }

    public KtorSettings(KtorSettings other) {
        if (other != null) {
            this.createRunConfigurationAutomatically = other.createRunConfigurationAutomatically;
        }
    }

    public KtorSettings copy() {
        return new KtorSettings(this);
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
        KtorSettings that = (KtorSettings) o;
        return createRunConfigurationAutomatically == that.createRunConfigurationAutomatically;
    }

    @Override
    public int hashCode() {
        return Objects.hash(createRunConfigurationAutomatically);
    }
}
