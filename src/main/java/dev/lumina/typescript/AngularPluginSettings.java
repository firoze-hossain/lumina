package dev.lumina.typescript;

import java.util.Objects;

/**
 * Model for Languages & Frameworks > TypeScript > Angular settings in Lumina IDE.
 * Matches reference screenshot media_1791602871731_fc5fd682.png:
 *  - Angular TypeScript Plugin mode: Disabled | Auto
 *  - Enable service-powered type engine checkbox
 */
public class AngularPluginSettings implements Cloneable {

    public static final String MODE_DISABLED = "Disabled";
    public static final String MODE_AUTO = "Auto";

    private String mode = MODE_AUTO;
    private boolean enableServicePoweredTypeEngine = false;

    public AngularPluginSettings() {
    }

    public AngularPluginSettings(AngularPluginSettings other) {
        if (other != null) {
            this.mode = other.mode;
            this.enableServicePoweredTypeEngine = other.enableServicePoweredTypeEngine;
        }
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = (mode != null && mode.equalsIgnoreCase(MODE_DISABLED)) ? MODE_DISABLED : MODE_AUTO;
    }

    public boolean isEnableServicePoweredTypeEngine() {
        return enableServicePoweredTypeEngine;
    }

    public void setEnableServicePoweredTypeEngine(boolean enableServicePoweredTypeEngine) {
        this.enableServicePoweredTypeEngine = enableServicePoweredTypeEngine;
    }

    @Override
    public AngularPluginSettings clone() {
        return new AngularPluginSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AngularPluginSettings that = (AngularPluginSettings) o;
        return enableServicePoweredTypeEngine == that.enableServicePoweredTypeEngine &&
                Objects.equals(mode, that.mode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, enableServicePoweredTypeEngine);
    }
}
