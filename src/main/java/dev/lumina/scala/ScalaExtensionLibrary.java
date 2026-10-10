package dev.lumina.scala;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing a known Scala extension library.
 */
public class ScalaExtensionLibrary {

    private String name;
    private String path;
    private boolean enabled;
    private List<String> extensions = new ArrayList<>();

    public ScalaExtensionLibrary() {
        this("", "", true, new ArrayList<>());
    }

    public ScalaExtensionLibrary(String name, String path, boolean enabled, List<String> extensions) {
        this.name = name != null ? name : "";
        this.path = path != null ? path : "";
        this.enabled = enabled;
        if (extensions != null) {
            this.extensions.addAll(extensions);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path != null ? path : "";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public void setExtensions(List<String> extensions) {
        this.extensions = new ArrayList<>();
        if (extensions != null) {
            this.extensions.addAll(extensions);
        }
    }

    public ScalaExtensionLibrary copy() {
        return new ScalaExtensionLibrary(name, path, enabled, new ArrayList<>(extensions));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScalaExtensionLibrary that = (ScalaExtensionLibrary) o;
        return enabled == that.enabled &&
                Objects.equals(name, that.name) &&
                Objects.equals(path, that.path) &&
                Objects.equals(extensions, that.extensions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, path, enabled, extensions);
    }

    @Override
    public String toString() {
        return "ScalaExtensionLibrary{" +
                "name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", enabled=" + enabled +
                ", extensions=" + extensions +
                '}';
    }
}
