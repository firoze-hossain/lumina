package dev.lumina.schemas;

import java.util.Objects;

/**
 * Model representing an external schema/DTD resource mapping.
 */
public class ExternalResourceEntry {

    private String uri;
    private String location;

    public ExternalResourceEntry() {
        this("", "");
    }

    public ExternalResourceEntry(String uri, String location) {
        this.uri = uri != null ? uri : "";
        this.location = location != null ? location : "";
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri != null ? uri : "";
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location != null ? location : "";
    }

    public ExternalResourceEntry copy() {
        return new ExternalResourceEntry(uri, location);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExternalResourceEntry that = (ExternalResourceEntry) o;
        return Objects.equals(uri, that.uri) &&
                Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri, location);
    }

    @Override
    public String toString() {
        return "ExternalResourceEntry{" +
                "uri='" + uri + '\'' +
                ", location='" + location + '\'' +
                '}';
    }
}
