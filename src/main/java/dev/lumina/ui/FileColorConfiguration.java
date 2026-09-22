package dev.lumina.ui;

import java.io.Serializable;
import java.util.Objects;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Represents a File Color mapping entry between a NamedScope and an IDE color.
 */
public class FileColorConfiguration implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    private final StringProperty scopeName = new SimpleStringProperty("");
    private final StringProperty colorName = new SimpleStringProperty("Blue");
    private final StringProperty customHex = new SimpleStringProperty("#2E436E");
    private final BooleanProperty sharedThroughVcs = new SimpleBooleanProperty(false);

    public FileColorConfiguration() {
        this("", "Blue", false);
    }

    public FileColorConfiguration(String scopeName, String colorName, boolean sharedThroughVcs) {
        setScopeName(scopeName);
        setColorName(colorName);
        setSharedThroughVcs(sharedThroughVcs);
    }

    public FileColorConfiguration(String scopeName, String colorName, String customHex, boolean sharedThroughVcs) {
        setScopeName(scopeName);
        setColorName(colorName);
        setCustomHex(customHex);
        setSharedThroughVcs(sharedThroughVcs);
    }

    public String getScopeName() {
        return scopeName.get();
    }

    public void setScopeName(String value) {
        this.scopeName.set(value != null ? value : "");
    }

    public StringProperty scopeNameProperty() {
        return scopeName;
    }

    public String getColorName() {
        return colorName.get();
    }

    public void setColorName(String value) {
        this.colorName.set(value != null ? value : "Blue");
    }

    public StringProperty colorNameProperty() {
        return colorName;
    }

    public String getCustomHex() {
        return customHex.get();
    }

    public void setCustomHex(String value) {
        this.customHex.set(value != null ? value : "#2E436E");
    }

    public StringProperty customHexProperty() {
        return customHex;
    }

    public boolean isSharedThroughVcs() {
        return sharedThroughVcs.get();
    }

    public void setSharedThroughVcs(boolean value) {
        this.sharedThroughVcs.set(value);
    }

    public BooleanProperty sharedThroughVcsProperty() {
        return sharedThroughVcs;
    }

    public FileColorConfiguration deepCopy() {
        return new FileColorConfiguration(getScopeName(), getColorName(), getCustomHex(), isSharedThroughVcs());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileColorConfiguration that = (FileColorConfiguration) o;
        return isSharedThroughVcs() == that.isSharedThroughVcs() &&
                Objects.equals(getScopeName(), that.getScopeName()) &&
                Objects.equals(getColorName(), that.getColorName()) &&
                Objects.equals(getCustomHex(), that.getCustomHex());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getScopeName(), getColorName(), getCustomHex(), isSharedThroughVcs());
    }

    @Override
    public String toString() {
        return getScopeName() + " -> " + getColorName() + (isSharedThroughVcs() ? " [VCS]" : "");
    }
}
