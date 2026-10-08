package dev.lumina.kubernetes;

import java.util.Objects;

/**
 * Model representing a Kubernetes configuration file entry (kubeconfig).
 */
public class KubernetesConfigFile implements Cloneable {

    private boolean valid;
    private String path;
    private String scope;

    public KubernetesConfigFile() {
        this(true, "", "Determined by kubectl");
    }

    public KubernetesConfigFile(boolean valid, String path, String scope) {
        this.valid = valid;
        this.path = path;
        this.scope = scope;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    public KubernetesConfigFile clone() {
        try {
            return (KubernetesConfigFile) super.clone();
        } catch (CloneNotSupportedException e) {
            return new KubernetesConfigFile(this.valid, this.path, this.scope);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KubernetesConfigFile that = (KubernetesConfigFile) o;
        return valid == that.valid &&
                Objects.equals(path, that.path) &&
                Objects.equals(scope, that.scope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valid, path, scope);
    }
}
