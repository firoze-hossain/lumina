package dev.lumina.build;

import java.io.File;
import java.util.Objects;

/**
 * Configuration model for Build Tools > Gant.
 * Matches standard IDE settings:
 *  - Gant home (dynamic discovery and path configuration)
 */
public class GantSettings implements Cloneable {

    private String gantHome = "";

    public GantSettings() {
    }

    public GantSettings(GantSettings other) {
        if (other != null) {
            this.gantHome = other.gantHome;
        }
    }

    /**
     * Dynamically discovers default or installed Gant home directory if present.
     */
    public static String discoverDefaultGantHome() {
        String env = System.getenv("GANT_HOME");
        if (env != null && !env.isBlank()) {
            File f = new File(env.trim());
            if (f.exists()) return f.getAbsolutePath();
        }

        String userHome = System.getProperty("user.home", "");
        if (!userHome.isBlank()) {
            File sdkman = new File(userHome, ".sdkman/candidates/gant/current");
            if (sdkman.exists()) return sdkman.getAbsolutePath();
        }

        File brewArm = new File("/opt/homebrew/opt/gant");
        if (brewArm.exists()) return brewArm.getAbsolutePath();

        File brewIntel = new File("/usr/local/opt/gant");
        if (brewIntel.exists()) return brewIntel.getAbsolutePath();

        File usrShare = new File("/usr/share/gant");
        if (usrShare.exists()) return usrShare.getAbsolutePath();

        return "";
    }

    public String getEffectiveGantHome() {
        if (gantHome != null && !gantHome.isBlank()) {
            return gantHome.trim();
        }
        return discoverDefaultGantHome();
    }

    public String getGantHome() {
        return gantHome != null ? gantHome : "";
    }

    public void setGantHome(String gantHome) {
        this.gantHome = gantHome != null ? gantHome.trim() : "";
    }

    @Override
    public GantSettings clone() {
        return new GantSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GantSettings that = (GantSettings) o;
        return Objects.equals(gantHome, that.gantHome);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gantHome);
    }
}
