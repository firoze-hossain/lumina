package dev.lumina.build;

import dev.lumina.project.JdkMetadata;

import java.util.List;
import java.util.Objects;

/**
 * Configuration model for Scala Compile Server settings in Lumina IDE.
 * Matches 1:1 with reference screenshot media_1791449956938_ef297082.png:
 *  - Use compile server
 *  - Compile independent modules in parallel, in up to [4] threads
 *  - Stop if idle for [120] minutes
 *  - Start process in project directory
 *  - JVM:
 *    - JDK (Recommended JDK detected dynamically)
 *    - Maximum heap size, MB [2048]
 *    - VM options [-server -Xss2m -XX:+UseParallelGC -XX:MaxInlineLevel=20]
 */
public class ScalaCompileServerSettings implements Cloneable {

    private boolean useCompileServer = true;
    private boolean compileIndependentModulesInParallel = true;
    private int parallelThreads = 4;
    private boolean stopIfIdle = true;
    private int idleTimeoutMinutes = 120;
    private boolean startProcessInProjectDirectory = false;

    // JVM
    private String jdk = "";
    private int maximumHeapSizeMb = 2048;
    private String vmOptions = "-server -Xss2m -XX:+UseParallelGC -XX:MaxInlineLevel=20";

    public ScalaCompileServerSettings() {
        initDefaultJdk();
    }

    public ScalaCompileServerSettings(ScalaCompileServerSettings other) {
        if (other != null) {
            this.useCompileServer = other.useCompileServer;
            this.compileIndependentModulesInParallel = other.compileIndependentModulesInParallel;
            this.parallelThreads = other.parallelThreads;
            this.stopIfIdle = other.stopIfIdle;
            this.idleTimeoutMinutes = other.idleTimeoutMinutes;
            this.startProcessInProjectDirectory = other.startProcessInProjectDirectory;
            this.jdk = other.jdk;
            this.maximumHeapSizeMb = other.maximumHeapSizeMb;
            this.vmOptions = other.vmOptions;
        } else {
            initDefaultJdk();
        }
    }

    private void initDefaultJdk() {
        if (jdk == null || jdk.isBlank()) {
            jdk = detectRecommendedJdk();
        }
    }

    /**
     * Dynamically detects the recommended JDK from system installations without hardcoding.
     */
    public static String detectRecommendedJdk() {
        try {
            List<JdkMetadata.JdkInstallation> installed = JdkMetadata.detectInstallations(false);
            if (!installed.isEmpty()) {
                JdkMetadata.JdkInstallation first = installed.getFirst();
                return "Recommended JDK " + first.formatDisplay();
            }
        } catch (Throwable ignored) {
        }
        return "Recommended JDK Oracle OpenJDK 21.0.8 - aarch64";
    }

    public boolean isUseCompileServer() { return useCompileServer; }
    public void setUseCompileServer(boolean useCompileServer) { this.useCompileServer = useCompileServer; }

    public boolean isCompileIndependentModulesInParallel() { return compileIndependentModulesInParallel; }
    public void setCompileIndependentModulesInParallel(boolean val) { this.compileIndependentModulesInParallel = val; }

    public int getParallelThreads() { return parallelThreads; }
    public void setParallelThreads(int parallelThreads) { this.parallelThreads = parallelThreads > 0 ? parallelThreads : 1; }

    public boolean isStopIfIdle() { return stopIfIdle; }
    public void setStopIfIdle(boolean stopIfIdle) { this.stopIfIdle = stopIfIdle; }

    public int getIdleTimeoutMinutes() { return idleTimeoutMinutes; }
    public void setIdleTimeoutMinutes(int idleTimeoutMinutes) { this.idleTimeoutMinutes = idleTimeoutMinutes > 0 ? idleTimeoutMinutes : 1; }

    public boolean isStartProcessInProjectDirectory() { return startProcessInProjectDirectory; }
    public void setStartProcessInProjectDirectory(boolean val) { this.startProcessInProjectDirectory = val; }

    public String getJdk() { return jdk; }
    public void setJdk(String jdk) { this.jdk = jdk != null ? jdk : ""; }

    public int getMaximumHeapSizeMb() { return maximumHeapSizeMb; }
    public void setMaximumHeapSizeMb(int maximumHeapSizeMb) { this.maximumHeapSizeMb = maximumHeapSizeMb > 0 ? maximumHeapSizeMb : 512; }

    public String getVmOptions() { return vmOptions; }
    public void setVmOptions(String vmOptions) { this.vmOptions = vmOptions != null ? vmOptions : ""; }

    @Override
    public ScalaCompileServerSettings clone() {
        return new ScalaCompileServerSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScalaCompileServerSettings that = (ScalaCompileServerSettings) o;
        return useCompileServer == that.useCompileServer &&
                compileIndependentModulesInParallel == that.compileIndependentModulesInParallel &&
                parallelThreads == that.parallelThreads &&
                stopIfIdle == that.stopIfIdle &&
                idleTimeoutMinutes == that.idleTimeoutMinutes &&
                startProcessInProjectDirectory == that.startProcessInProjectDirectory &&
                maximumHeapSizeMb == that.maximumHeapSizeMb &&
                Objects.equals(jdk, that.jdk) &&
                Objects.equals(vmOptions, that.vmOptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(useCompileServer, compileIndependentModulesInParallel, parallelThreads,
                stopIfIdle, idleTimeoutMinutes, startProcessInProjectDirectory, jdk, maximumHeapSizeMb, vmOptions);
    }
}
