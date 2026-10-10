package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing a Key-Value resource attribute for Open Telemetry in GitHub Copilot settings.
 */
public class TelemetryResourceAttribute implements Cloneable {

    private String key = "";
    private String value = "";

    public TelemetryResourceAttribute() {
    }

    public TelemetryResourceAttribute(String key, String value) {
        this.key = key != null ? key : "";
        this.value = value != null ? value : "";
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key != null ? key : "";
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value != null ? value : "";
    }

    @Override
    public TelemetryResourceAttribute clone() {
        try {
            return (TelemetryResourceAttribute) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TelemetryResourceAttribute that = (TelemetryResourceAttribute) o;
        return Objects.equals(key, that.key) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override
    public String toString() {
        return "TelemetryResourceAttribute{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
