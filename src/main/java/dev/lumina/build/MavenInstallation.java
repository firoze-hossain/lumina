package dev.lumina.build;

import java.util.Objects;

/**
 * Represents a detected or configured Maven installation in Lumina IDE.
 */
public class MavenInstallation {

    private String name;
    private String path;
    private String version;
    private boolean bundled;

    public MavenInstallation() {
    }

    public MavenInstallation(String name, String path, String version, boolean bundled) {
        this.name = name;
        this.path = path;
        this.version = version;
        this.bundled = bundled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isBundled() {
        return bundled;
    }

    public void setBundled(boolean bundled) {
        this.bundled = bundled;
    }

    @Override
    public String toString() {
        return name != null ? name : (path != null ? path : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MavenInstallation that = (MavenInstallation) o;
        return bundled == that.bundled &&
                Objects.equals(name, that.name) &&
                Objects.equals(path, that.path) &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, path, version, bundled);
    }
}
