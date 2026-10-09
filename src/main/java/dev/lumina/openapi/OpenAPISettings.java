package dev.lumina.openapi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings model for Languages & Frameworks > OpenAPI Specifications in Lumina IDE.
 */
public class OpenAPISettings {

    public static final String DEFAULT_SWAGGERHUB_ADDRESS = "api.swaggerhub.com";

    private boolean gutterIconsForEdits = true;
    private String swaggerHubAddress = DEFAULT_SWAGGERHUB_ADDRESS;
    private String swaggerHubApiKey = "";
    private List<RemoteOpenAPISpec> remoteSpecifications = new ArrayList<>();

    public OpenAPISettings() {
    }

    public OpenAPISettings(OpenAPISettings other) {
        if (other != null) {
            this.gutterIconsForEdits = other.gutterIconsForEdits;
            this.swaggerHubAddress = other.swaggerHubAddress;
            this.swaggerHubApiKey = other.swaggerHubApiKey;
            if (other.remoteSpecifications != null) {
                for (RemoteOpenAPISpec spec : other.remoteSpecifications) {
                    this.remoteSpecifications.add(spec.copy());
                }
            }
        }
    }

    public OpenAPISettings copy() {
        return new OpenAPISettings(this);
    }

    public boolean isGutterIconsForEdits() {
        return gutterIconsForEdits;
    }

    public void setGutterIconsForEdits(boolean gutterIconsForEdits) {
        this.gutterIconsForEdits = gutterIconsForEdits;
    }

    public String getSwaggerHubAddress() {
        return swaggerHubAddress;
    }

    public void setSwaggerHubAddress(String swaggerHubAddress) {
        this.swaggerHubAddress = swaggerHubAddress != null ? swaggerHubAddress : DEFAULT_SWAGGERHUB_ADDRESS;
    }

    public String getSwaggerHubApiKey() {
        return swaggerHubApiKey;
    }

    public void setSwaggerHubApiKey(String swaggerHubApiKey) {
        this.swaggerHubApiKey = swaggerHubApiKey != null ? swaggerHubApiKey : "";
    }

    public List<RemoteOpenAPISpec> getRemoteSpecifications() {
        return remoteSpecifications;
    }

    public void setRemoteSpecifications(List<RemoteOpenAPISpec> remoteSpecifications) {
        this.remoteSpecifications.clear();
        if (remoteSpecifications != null) {
            for (RemoteOpenAPISpec s : remoteSpecifications) {
                this.remoteSpecifications.add(s.copy());
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OpenAPISettings that = (OpenAPISettings) o;
        return gutterIconsForEdits == that.gutterIconsForEdits &&
                Objects.equals(swaggerHubAddress, that.swaggerHubAddress) &&
                Objects.equals(swaggerHubApiKey, that.swaggerHubApiKey) &&
                Objects.equals(remoteSpecifications, that.remoteSpecifications);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gutterIconsForEdits, swaggerHubAddress, swaggerHubApiKey, remoteSpecifications);
    }
}
