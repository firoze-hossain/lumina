package dev.lumina.typescript;

import java.util.Objects;

/**
 * Model for Languages & Frameworks > TypeScript > Vue settings in Lumina IDE.
 * Matches reference screenshot media_1791604015621_895d532c.png:
 *  - Vue Language Server package
 *  - Mode: Disabled | Auto | Classic TypeScript Service
 *  - Enable service-powered type engine (Alpha)
 *  - Vue LS 3.0 preview
 */
public class VueServiceSettings implements Cloneable {

    public static final String DEFAULT_SERVER = "@vue/language-server (Default)   2.2.10";

    public static final String MODE_DISABLED = "Disabled";
    public static final String MODE_AUTO = "Auto";
    public static final String MODE_CLASSIC = "Classic";

    private String serverPackage = DEFAULT_SERVER;
    private String mode = MODE_AUTO;
    private boolean enableServicePoweredTypeEngine = false;
    private boolean vueLs3Preview = false;

    public VueServiceSettings() {
    }

    public VueServiceSettings(VueServiceSettings other) {
        if (other != null) {
            this.serverPackage = other.serverPackage;
            this.mode = other.mode;
            this.enableServicePoweredTypeEngine = other.enableServicePoweredTypeEngine;
            this.vueLs3Preview = other.vueLs3Preview;
        }
    }

    public String getServerPackage() {
        return serverPackage;
    }

    public void setServerPackage(String serverPackage) {
        this.serverPackage = serverPackage != null ? serverPackage : DEFAULT_SERVER;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        if (MODE_DISABLED.equalsIgnoreCase(mode)) {
            this.mode = MODE_DISABLED;
        } else if (MODE_CLASSIC.equalsIgnoreCase(mode)) {
            this.mode = MODE_CLASSIC;
        } else {
            this.mode = MODE_AUTO;
        }
    }

    public boolean isEnableServicePoweredTypeEngine() {
        return enableServicePoweredTypeEngine;
    }

    public void setEnableServicePoweredTypeEngine(boolean enableServicePoweredTypeEngine) {
        this.enableServicePoweredTypeEngine = enableServicePoweredTypeEngine;
    }

    public boolean isVueLs3Preview() {
        return vueLs3Preview;
    }

    public void setVueLs3Preview(boolean vueLs3Preview) {
        this.vueLs3Preview = vueLs3Preview;
    }

    @Override
    public VueServiceSettings clone() {
        return new VueServiceSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VueServiceSettings that = (VueServiceSettings) o;
        return enableServicePoweredTypeEngine == that.enableServicePoweredTypeEngine &&
                vueLs3Preview == that.vueLs3Preview &&
                Objects.equals(serverPackage, that.serverPackage) &&
                Objects.equals(mode, that.mode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serverPackage, mode, enableServicePoweredTypeEngine, vueLs3Preview);
    }
}
