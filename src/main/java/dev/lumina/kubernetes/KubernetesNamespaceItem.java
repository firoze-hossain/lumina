package dev.lumina.kubernetes;

import java.util.Objects;

/**
 * Model representing a fallback namespace entry in Kubernetes settings.
 */
public class KubernetesNamespaceItem implements Cloneable {

    private String namespace;
    private String scope;

    public KubernetesNamespaceItem() {
        this("", "Project");
    }

    public KubernetesNamespaceItem(String namespace, String scope) {
        this.namespace = namespace;
        this.scope = scope;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    public KubernetesNamespaceItem clone() {
        try {
            return (KubernetesNamespaceItem) super.clone();
        } catch (CloneNotSupportedException e) {
            return new KubernetesNamespaceItem(this.namespace, this.scope);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KubernetesNamespaceItem that = (KubernetesNamespaceItem) o;
        return Objects.equals(namespace, that.namespace) &&
                Objects.equals(scope, that.scope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, scope);
    }
}
