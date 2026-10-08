package dev.lumina.debugger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings for Build, Execution, Deployment > Debugger > Data Views > JavaScript (Image 1).
 */
public class JavaScriptDataViewsSettings implements Cloneable {

    private boolean showObjectProperties = false;
    private List<String> objectProperties = new ArrayList<>();

    public JavaScriptDataViewsSettings() {
        initDefaults();
    }

    public JavaScriptDataViewsSettings(JavaScriptDataViewsSettings other) {
        if (other != null) {
            this.showObjectProperties = other.showObjectProperties;
            this.objectProperties = new ArrayList<>(other.objectProperties);
        }
    }

    public void initDefaults() {
        this.showObjectProperties = false;
        this.objectProperties = new ArrayList<>(List.of("id", "name"));
    }

    public boolean isShowObjectProperties() {
        return showObjectProperties;
    }

    public void setShowObjectProperties(boolean showObjectProperties) {
        this.showObjectProperties = showObjectProperties;
    }

    public boolean isShowPropertiesInObjectNode() {
        return isShowObjectProperties();
    }

    public void setShowPropertiesInObjectNode(boolean showPropertiesInObjectNode) {
        setShowObjectProperties(showPropertiesInObjectNode);
    }

    public List<String> getObjectProperties() {
        return objectProperties;
    }

    public void setObjectProperties(List<String> objectProperties) {
        this.objectProperties = objectProperties != null ? new ArrayList<>(objectProperties) : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JavaScriptDataViewsSettings that)) return false;
        return showObjectProperties == that.showObjectProperties &&
                Objects.equals(objectProperties, that.objectProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(showObjectProperties, objectProperties);
    }

    @Override
    public JavaScriptDataViewsSettings clone() {
        return new JavaScriptDataViewsSettings(this);
    }
}
