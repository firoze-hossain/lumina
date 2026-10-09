package dev.lumina.kubernetes;

import java.util.Objects;

/**
 * Represents a custom resource definition (CRD) or OpenAPI specification entry in Lumina IDE.
 */
public class KubernetesResourceSpec {

    private String pathOrUrl;
    private String status;
    private String scope;

    public KubernetesResourceSpec() {
        this("", "Valid", "Project");
    }

    public KubernetesResourceSpec(String pathOrUrl, String status, String scope) {
        this.pathOrUrl = pathOrUrl != null ? pathOrUrl : "";
        this.status = status != null ? status : "Valid";
        this.scope = scope != null ? scope : "Project";
    }

    public KubernetesResourceSpec(KubernetesResourceSpec other) {
        if (other != null) {
            this.pathOrUrl = other.pathOrUrl;
            this.status = other.status;
            this.scope = other.scope;
        }
    }

    public KubernetesResourceSpec copy() {
        return new KubernetesResourceSpec(this);
    }

    public String getPathOrUrl() {
        return pathOrUrl;
    }

    public void setPathOrUrl(String pathOrUrl) {
        this.pathOrUrl = pathOrUrl != null ? pathOrUrl : "";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status != null ? status : "Valid";
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope != null ? scope : "Project";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KubernetesResourceSpec that = (KubernetesResourceSpec) o;
        return Objects.equals(pathOrUrl, that.pathOrUrl) &&
                Objects.equals(status, that.status) &&
                Objects.equals(scope, that.scope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pathOrUrl, status, scope);
    }
}
