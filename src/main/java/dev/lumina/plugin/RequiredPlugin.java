package dev.lumina.plugin;

import java.util.Objects;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Model representing a plugin required by the project.
 */
public class RequiredPlugin {

    private final StringProperty pluginId = new SimpleStringProperty("");
    private final StringProperty pluginName = new SimpleStringProperty("");
    private final StringProperty minVersion = new SimpleStringProperty("");
    private final StringProperty maxVersion = new SimpleStringProperty("");

    public RequiredPlugin(String pluginName) {
        this(pluginName, pluginName, "", "");
    }

    public RequiredPlugin(String pluginId, String pluginName, String minVersion, String maxVersion) {
        this.pluginId.set(pluginId != null ? pluginId : "");
        this.pluginName.set(pluginName != null ? pluginName : (pluginId != null ? pluginId : ""));
        this.minVersion.set(minVersion != null ? minVersion.trim() : "");
        this.maxVersion.set(maxVersion != null ? maxVersion.trim() : "");
    }

    public String getPluginId() {
        return pluginId.get();
    }

    public void setPluginId(String value) {
        this.pluginId.set(value != null ? value : "");
    }

    public StringProperty pluginIdProperty() {
        return pluginId;
    }

    public String getPluginName() {
        return pluginName.get();
    }

    public void setPluginName(String value) {
        this.pluginName.set(value != null ? value : "");
    }

    public StringProperty pluginNameProperty() {
        return pluginName;
    }

    public String getMinVersion() {
        return minVersion.get();
    }

    public void setMinVersion(String value) {
        this.minVersion.set(value != null ? value.trim() : "");
    }

    public StringProperty minVersionProperty() {
        return minVersion;
    }

    public String getMaxVersion() {
        return maxVersion.get();
    }

    public void setMaxVersion(String value) {
        this.maxVersion.set(value != null ? value.trim() : "");
    }

    public StringProperty maxVersionProperty() {
        return maxVersion;
    }

    /**
     * Checks whether an installed version satisfies min and max version constraints.
     */
    public boolean isVersionCompatible(String installedVersion) {
        if (installedVersion == null || installedVersion.isBlank()) {
            return false;
        }

        String min = getMinVersion();
        if (!min.isEmpty()) {
            if (compareVersions(installedVersion, min) < 0) {
                return false;
            }
        }

        String max = getMaxVersion();
        if (!max.isEmpty()) {
            if (compareVersions(installedVersion, max) > 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Simple semantic / numeric version comparator.
     */
    public static int compareVersions(String v1, String v2) {
        if (v1 == null && v2 == null) return 0;
        if (v1 == null) return -1;
        if (v2 == null) return 1;

        String[] parts1 = v1.replaceAll("[^0-9.]", "").split("\\.");
        String[] parts2 = v2.replaceAll("[^0-9.]", "").split("\\.");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int num1 = i < parts1.length && !parts1[i].isEmpty() ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length && !parts2[i].isEmpty() ? Integer.parseInt(parts2[i]) : 0;
            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        return 0;
    }

    public RequiredPlugin deepCopy() {
        return new RequiredPlugin(getPluginId(), getPluginName(), getMinVersion(), getMaxVersion());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequiredPlugin that = (RequiredPlugin) o;
        return Objects.equals(getPluginId(), that.getPluginId()) &&
                Objects.equals(getMinVersion(), that.getMinVersion()) &&
                Objects.equals(getMaxVersion(), that.getMaxVersion());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPluginId(), getMinVersion(), getMaxVersion());
    }

    @Override
    public String toString() {
        return getPluginName();
    }
}
