package dev.lumina.pathvar;

import java.util.Objects;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Model representing an IDE Path Variable.
 */
public class PathVariable {

    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty value = new SimpleStringProperty("");

    public PathVariable(String name, String value) {
        this.name.set(name != null ? name : "");
        this.value.set(value != null ? value : "");
    }

    public String getName() {
        return name.get();
    }

    public void setName(String value) {
        this.name.set(value != null ? value : "");
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getValue() {
        return value.get();
    }

    public void setValue(String value) {
        this.value.set(value != null ? value : "");
    }

    public StringProperty valueProperty() {
        return value;
    }

    public PathVariable deepCopy() {
        return new PathVariable(getName(), getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathVariable that = (PathVariable) o;
        return Objects.equals(getName(), that.getName()) &&
                Objects.equals(getValue(), that.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getValue());
    }

    @Override
    public String toString() {
        return getName() + "=" + getValue();
    }
}
