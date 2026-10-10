package dev.lumina.tools;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Objects;

/**
 * Model representing a location entry (path and enabled flag) in Copilot Customizations settings.
 */
public class CustomizationLocationEntry {

    private final StringProperty path = new SimpleStringProperty("");
    private final BooleanProperty enabled = new SimpleBooleanProperty(true);

    public CustomizationLocationEntry() {
    }

    public CustomizationLocationEntry(String path, boolean enabled) {
        setPath(path);
        setEnabled(enabled);
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

    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    public BooleanProperty enabledProperty() {
        return enabled;
    }

    public CustomizationLocationEntry clone() {
        return new CustomizationLocationEntry(getPath(), isEnabled());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomizationLocationEntry that = (CustomizationLocationEntry) o;
        return isEnabled() == that.isEnabled() &&
                Objects.equals(getPath(), that.getPath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPath(), isEnabled());
    }

    @Override
    public String toString() {
        return "CustomizationLocationEntry{" +
                "path='" + getPath() + '\'' +
                ", enabled=" + isEnabled() +
                '}';
    }
}
