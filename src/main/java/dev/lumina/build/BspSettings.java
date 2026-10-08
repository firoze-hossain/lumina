package dev.lumina.build;

import java.util.Objects;

/**
 * Configuration model for Build Tools > BSP (Build Server Protocol).
 * Matches standard IDE settings:
 *  - BSP trace log: Enable
 */
public class BspSettings implements Cloneable {

    private boolean bspTraceLogEnabled = false;

    public BspSettings() {
    }

    public BspSettings(BspSettings other) {
        if (other != null) {
            this.bspTraceLogEnabled = other.bspTraceLogEnabled;
        }
    }

    public boolean isBspTraceLogEnabled() {
        return bspTraceLogEnabled;
    }

    public void setBspTraceLogEnabled(boolean bspTraceLogEnabled) {
        this.bspTraceLogEnabled = bspTraceLogEnabled;
    }

    @Override
    public BspSettings clone() {
        return new BspSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BspSettings that = (BspSettings) o;
        return bspTraceLogEnabled == that.bspTraceLogEnabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bspTraceLogEnabled);
    }
}
