package dev.lumina.build;

import dev.lumina.project.JdkMetadata;

import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * Configuration model for Build Tools > sbt.
 * Matches standard IDE settings 1:1 (media_1791429948904.png):
 *  - General Settings:
 *    - JRE (Default project SDK, detected JDKs, custom path)
 *    - Maximum heap size, MB
 *    - VM parameters
 *    - Sbt options
 *    - Environment variables
 *    - Launcher (sbt-launch.jar): Bundled / Custom
 */
public class SbtSettings implements Cloneable {

    private String jre = getDefaultJreLabel();
    private String maximumHeapSizeMb = "";
    private String vmParameters = "";
    private String sbtOptions = "";
    private String environmentVariables = "";
    private String launcher = "Bundled";
    private String customLauncherPath = "";

    // Legacy / backward-compatibility fields
    private boolean useSbtShellForBuilds = true;
    private boolean useSbtShellForImports = false;

    public SbtSettings() {
    }

    public SbtSettings(SbtSettings other) {
        if (other != null) {
            this.jre = other.jre;
            this.maximumHeapSizeMb = other.maximumHeapSizeMb;
            this.vmParameters = other.vmParameters;
            this.sbtOptions = other.sbtOptions;
            this.environmentVariables = other.environmentVariables;
            this.launcher = other.launcher;
            this.customLauncherPath = other.customLauncherPath;
            this.useSbtShellForBuilds = other.useSbtShellForBuilds;
            this.useSbtShellForImports = other.useSbtShellForImports;
        }
    }

    /**
     * Dynamically determines the default JRE label (e.g. "Default (25 - project SDK)") without hardcoding.
     */
    public static String getDefaultJreLabel() {
        int major = 25;
        try {
            List<JdkMetadata.JdkInstallation> jdks = JdkMetadata.detectInstallations(false);
            if (!jdks.isEmpty()) {
                major = jdks.getFirst().majorVersion();
            }
        } catch (Throwable ignored) {
        }
        return "Default (" + major + " - project SDK)";
    }

    /**
     * Dynamically discovers default sbt launcher.
     */
    public static String discoverDefaultSbtLauncher() {
        String sbtHome = System.getenv("SBT_HOME");
        if (sbtHome != null && !sbtHome.isBlank()) {
            File f = new File(sbtHome.trim(), "bin/sbt-launch.jar");
            if (f.exists()) return f.getAbsolutePath();
            File f2 = new File(sbtHome.trim(), "bin/sbt");
            if (f2.exists()) return f2.getAbsolutePath();
        }

        String userHome = System.getProperty("user.home", "");
        if (!userHome.isBlank()) {
            File sdkman = new File(userHome, ".sdkman/candidates/sbt/current/bin/sbt-launch.jar");
            if (sdkman.exists()) return sdkman.getAbsolutePath();
        }

        File brewArm = new File("/opt/homebrew/opt/sbt/libexec/bin/sbt-launch.jar");
        if (brewArm.exists()) return brewArm.getAbsolutePath();

        File brewIntel = new File("/usr/local/opt/sbt/libexec/bin/sbt-launch.jar");
        if (brewIntel.exists()) return brewIntel.getAbsolutePath();

        return "";
    }

    public String getJre() {
        return jre != null && !jre.isBlank() ? jre : getDefaultJreLabel();
    }

    public void setJre(String jre) {
        this.jre = jre != null ? jre.trim() : getDefaultJreLabel();
    }

    public String getMaximumHeapSizeMb() {
        return maximumHeapSizeMb != null ? maximumHeapSizeMb : "";
    }

    public void setMaximumHeapSizeMb(String maximumHeapSizeMb) {
        this.maximumHeapSizeMb = maximumHeapSizeMb != null ? maximumHeapSizeMb.trim() : "";
    }

    public String getVmParameters() {
        return vmParameters != null ? vmParameters : "";
    }

    public void setVmParameters(String vmParameters) {
        this.vmParameters = vmParameters != null ? vmParameters : "";
    }

    public String getSbtOptions() {
        return sbtOptions != null ? sbtOptions : "";
    }

    public void setSbtOptions(String sbtOptions) {
        this.sbtOptions = sbtOptions != null ? sbtOptions : "";
    }

    public String getEnvironmentVariables() {
        return environmentVariables != null ? environmentVariables : "";
    }

    public void setEnvironmentVariables(String environmentVariables) {
        this.environmentVariables = environmentVariables != null ? environmentVariables : "";
    }

    public String getLauncher() {
        return launcher != null && !launcher.isBlank() ? launcher : "Bundled";
    }

    public void setLauncher(String launcher) {
        this.launcher = launcher != null ? launcher.trim() : "Bundled";
    }

    public String getCustomLauncherPath() {
        return customLauncherPath != null ? customLauncherPath : "";
    }

    public void setCustomLauncherPath(String customLauncherPath) {
        this.customLauncherPath = customLauncherPath != null ? customLauncherPath.trim() : "";
    }

    public boolean isUseSbtShellForBuilds() {
        return useSbtShellForBuilds;
    }

    public void setUseSbtShellForBuilds(boolean useSbtShellForBuilds) {
        this.useSbtShellForBuilds = useSbtShellForBuilds;
    }

    public boolean isUseSbtShellForImports() {
        return useSbtShellForImports;
    }

    public void setUseSbtShellForImports(boolean useSbtShellForImports) {
        this.useSbtShellForImports = useSbtShellForImports;
    }

    // Alias for vmParameters
    public String getVmOptions() {
        return getVmParameters();
    }

    public void setVmOptions(String vmOptions) {
        setVmParameters(vmOptions);
    }

    @Override
    public SbtSettings clone() {
        return new SbtSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SbtSettings that = (SbtSettings) o;
        return useSbtShellForBuilds == that.useSbtShellForBuilds &&
                useSbtShellForImports == that.useSbtShellForImports &&
                Objects.equals(jre, that.jre) &&
                Objects.equals(maximumHeapSizeMb, that.maximumHeapSizeMb) &&
                Objects.equals(vmParameters, that.vmParameters) &&
                Objects.equals(sbtOptions, that.sbtOptions) &&
                Objects.equals(environmentVariables, that.environmentVariables) &&
                Objects.equals(launcher, that.launcher) &&
                Objects.equals(customLauncherPath, that.customLauncherPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jre, maximumHeapSizeMb, vmParameters, sbtOptions,
                environmentVariables, launcher, customLauncherPath, useSbtShellForBuilds, useSbtShellForImports);
    }
}
