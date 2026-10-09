package dev.lumina.openapi;

import java.util.Objects;

/**
 * Represents a remote OpenAPI / Swagger specification in Lumina IDE.
 */
public class RemoteOpenAPISpec {

    private String name;
    private String urlOrSource;
    private String type; // "URL" or "SwaggerHub"

    public RemoteOpenAPISpec() {
        this("", "", "URL");
    }

    public RemoteOpenAPISpec(String name, String urlOrSource, String type) {
        this.name = name != null ? name : "";
        this.urlOrSource = urlOrSource != null ? urlOrSource : "";
        this.type = type != null ? type : "URL";
    }

    public RemoteOpenAPISpec(RemoteOpenAPISpec other) {
        if (other != null) {
            this.name = other.name;
            this.urlOrSource = other.urlOrSource;
            this.type = other.type;
        }
    }

    public RemoteOpenAPISpec copy() {
        return new RemoteOpenAPISpec(this);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public String getUrlOrSource() {
        return urlOrSource;
    }

    public void setUrlOrSource(String urlOrSource) {
        this.urlOrSource = urlOrSource != null ? urlOrSource : "";
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type != null ? type : "URL";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RemoteOpenAPISpec that = (RemoteOpenAPISpec) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(urlOrSource, that.urlOrSource) &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, urlOrSource, type);
    }
}
