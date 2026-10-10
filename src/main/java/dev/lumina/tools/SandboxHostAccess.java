package dev.lumina.tools;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Objects;

/**
 * Model representing a network host access rule in GitHub Copilot Sandbox settings.
 */
public class SandboxHostAccess {

    private final StringProperty host = new SimpleStringProperty("");
    private final StringProperty access = new SimpleStringProperty("Allow");

    public SandboxHostAccess() {
    }

    public SandboxHostAccess(String host, String access) {
        setHost(host);
        setAccess(access);
    }

    public String getHost() {
        return host.get();
    }

    public void setHost(String host) {
        this.host.set(host != null ? host : "");
    }

    public StringProperty hostProperty() {
        return host;
    }

    public String getAccess() {
        return access.get();
    }

    public void setAccess(String access) {
        this.access.set(access != null ? access : "Allow");
    }

    public StringProperty accessProperty() {
        return access;
    }

    public SandboxHostAccess clone() {
        return new SandboxHostAccess(getHost(), getAccess());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SandboxHostAccess that = (SandboxHostAccess) o;
        return Objects.equals(getHost(), that.getHost()) &&
                Objects.equals(getAccess(), that.getAccess());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getHost(), getAccess());
    }

    @Override
    public String toString() {
        return "SandboxHostAccess{" +
                "host='" + getHost() + '\'' +
                ", access='" + getAccess() + '\'' +
                '}';
    }
}
