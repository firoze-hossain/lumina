package dev.lumina.tools;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Objects;

/**
 * Model representing a path permission rule in GitHub Copilot Sandbox settings.
 */
public class SandboxPathPermission {

    private final StringProperty path = new SimpleStringProperty("");
    private final StringProperty permission = new SimpleStringProperty("Read");

    public SandboxPathPermission() {
    }

    public SandboxPathPermission(String path, String permission) {
        setPath(path);
        setPermission(permission);
    }

    public String getPath() {
        return path.get();
    }

    public void setPath(String path) {
        this.path.set(path != null ? path : "");
    }

    public StringProperty pathProperty() {
        return path;
    }

    public String getPermission() {
        return permission.get();
    }

    public void setPermission(String permission) {
        this.permission.set(permission != null ? permission : "Read");
    }

    public StringProperty permissionProperty() {
        return permission;
    }

    public SandboxPathPermission clone() {
        return new SandboxPathPermission(getPath(), getPermission());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SandboxPathPermission that = (SandboxPathPermission) o;
        return Objects.equals(getPath(), that.getPath()) &&
                Objects.equals(getPermission(), that.getPermission());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPath(), getPermission());
    }

    @Override
    public String toString() {
        return "SandboxPathPermission{" +
                "path='" + getPath() + '\'' +
                ", permission='" + getPermission() + '\'' +
                '}';
    }
}
