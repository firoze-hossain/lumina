package dev.lumina.tools;

import java.util.Objects;
import java.util.UUID;

/**
 * Model representing a single startup task in Lumina IDE.
 */
public class StartupTaskEntry implements Cloneable {

    private String id = UUID.randomUUID().toString();
    private String name = "";
    private String configurationType = "Application";
    private boolean shared = false;

    public StartupTaskEntry() {
    }

    public StartupTaskEntry(String name, String configurationType, boolean shared) {
        this.name = name != null ? name : "";
        this.configurationType = configurationType != null ? configurationType : "Application";
        this.shared = shared;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id != null ? id : UUID.randomUUID().toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public String getConfigurationType() {
        return configurationType;
    }

    public void setConfigurationType(String configurationType) {
        this.configurationType = configurationType != null ? configurationType : "Application";
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    @Override
    public StartupTaskEntry clone() {
        try {
            return (StartupTaskEntry) super.clone();
        } catch (CloneNotSupportedException e) {
            StartupTaskEntry copy = new StartupTaskEntry();
            copy.id = this.id;
            copy.name = this.name;
            copy.configurationType = this.configurationType;
            copy.shared = this.shared;
            return copy;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StartupTaskEntry that = (StartupTaskEntry) o;
        return shared == that.shared &&
                Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(configurationType, that.configurationType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, configurationType, shared);
    }

    @Override
    public String toString() {
        return name + " (" + configurationType + ")";
    }
}
