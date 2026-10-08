package dev.lumina.kubernetes;

import java.util.Objects;

/**
 * Model representing an ephemeral debug container configuration in Kubernetes settings.
 */
public class KubernetesEphemeralContainer implements Cloneable {

    private String name;
    private String parameters;

    public KubernetesEphemeralContainer() {
        this("", "");
    }

    public KubernetesEphemeralContainer(String name, String parameters) {
        this.name = name;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    @Override
    public KubernetesEphemeralContainer clone() {
        try {
            return (KubernetesEphemeralContainer) super.clone();
        } catch (CloneNotSupportedException e) {
            return new KubernetesEphemeralContainer(this.name, this.parameters);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KubernetesEphemeralContainer that = (KubernetesEphemeralContainer) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, parameters);
    }
}
