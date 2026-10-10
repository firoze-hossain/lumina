package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing Tools > HTTP Client settings matching Image 5.
 */
public class HttpClientSettings implements Cloneable {

    private String customHttpMethods = "";

    public HttpClientSettings() {
    }

    public HttpClientSettings(String customHttpMethods) {
        this.customHttpMethods = customHttpMethods != null ? customHttpMethods : "";
    }

    public String getCustomHttpMethods() {
        return customHttpMethods;
    }

    public void setCustomHttpMethods(String customHttpMethods) {
        this.customHttpMethods = customHttpMethods != null ? customHttpMethods : "";
    }

    @Override
    public HttpClientSettings clone() {
        return new HttpClientSettings(customHttpMethods);
    }

    public HttpClientSettings copy() {
        return clone();
    }

    @Override
    public String toString() {
        return "HttpClientSettings{customHttpMethods='" + customHttpMethods + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpClientSettings that = (HttpClientSettings) o;
        return Objects.equals(customHttpMethods, that.customHttpMethods);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customHttpMethods);
    }
}
