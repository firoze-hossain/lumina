package dev.lumina.protobuf;

import java.util.Objects;

/**
 * Model representing an import path entry in Languages & Frameworks > Protocol Buffers.
 */
public class ProtobufImportPath {

    private String location;
    private String prefix;
    private boolean system;

    public ProtobufImportPath() {
        this("", "", false);
    }

    public ProtobufImportPath(String location, String prefix, boolean system) {
        this.location = location != null ? location : "";
        this.prefix = prefix != null ? prefix : "";
        this.system = system;
    }

    public ProtobufImportPath(ProtobufImportPath other) {
        if (other != null) {
            this.location = other.location;
            this.prefix = other.prefix;
            this.system = other.system;
        }
    }

    public ProtobufImportPath copy() {
        return new ProtobufImportPath(this);
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location != null ? location : "";
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix != null ? prefix : "";
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProtobufImportPath that = (ProtobufImportPath) o;
        return system == that.system &&
                Objects.equals(location, that.location) &&
                Objects.equals(prefix, that.prefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, prefix, system);
    }

    @Override
    public String toString() {
        return "ProtobufImportPath{" +
                "location='" + location + '\'' +
                ", prefix='" + prefix + '\'' +
                ", system=" + system +
                '}';
    }
}
