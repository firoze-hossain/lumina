package dev.lumina.build;

import java.io.File;
import java.util.Objects;

/**
 * Model representing Maven build settings in Lumina IDE.
 * Matches Build, Execution, Deployment > Build Tools > Maven.
 */
public class MavenSettings implements Cloneable {

    private boolean workOffline = false;
    private boolean executeGoalsRecursively = true;
    private boolean printExceptionStackTraces = false;
    private boolean alwaysUpdateSnapshots = false;

    private String outputLevel = "Info";
    private String checksumPolicy = "No Global Policy";
    private String multiprojectFailPolicy = "Default";
    private String threadCount = "";

    private String mavenHome = "Bundled (Maven 3)";
    private String mavenVersion = "3.9.11";

    private boolean userSettingsOverride = false;
    private String userSettingsFile = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "settings.xml";

    private boolean localRepoOverride = false;
    private String localRepo = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository";

    private boolean useMavenConfig = true;

    public MavenSettings() {
    }

    public MavenSettings(MavenSettings other) {
        if (other != null) {
            this.workOffline = other.workOffline;
            this.executeGoalsRecursively = other.executeGoalsRecursively;
            this.printExceptionStackTraces = other.printExceptionStackTraces;
            this.alwaysUpdateSnapshots = other.alwaysUpdateSnapshots;
            this.outputLevel = other.outputLevel;
            this.checksumPolicy = other.checksumPolicy;
            this.multiprojectFailPolicy = other.multiprojectFailPolicy;
            this.threadCount = other.threadCount;
            this.mavenHome = other.mavenHome;
            this.mavenVersion = other.mavenVersion;
            this.userSettingsOverride = other.userSettingsOverride;
            this.userSettingsFile = other.userSettingsFile;
            this.localRepoOverride = other.localRepoOverride;
            this.localRepo = other.localRepo;
            this.useMavenConfig = other.useMavenConfig;
        }
    }

    public boolean isWorkOffline() {
        return workOffline;
    }

    public void setWorkOffline(boolean workOffline) {
        this.workOffline = workOffline;
    }

    public boolean isExecuteGoalsRecursively() {
        return executeGoalsRecursively;
    }

    public void setExecuteGoalsRecursively(boolean executeGoalsRecursively) {
        this.executeGoalsRecursively = executeGoalsRecursively;
    }

    public boolean isPrintExceptionStackTraces() {
        return printExceptionStackTraces;
    }

    public void setPrintExceptionStackTraces(boolean printExceptionStackTraces) {
        this.printExceptionStackTraces = printExceptionStackTraces;
    }

    public boolean isAlwaysUpdateSnapshots() {
        return alwaysUpdateSnapshots;
    }

    public void setAlwaysUpdateSnapshots(boolean alwaysUpdateSnapshots) {
        this.alwaysUpdateSnapshots = alwaysUpdateSnapshots;
    }

    public String getOutputLevel() {
        return outputLevel;
    }

    public void setOutputLevel(String outputLevel) {
        this.outputLevel = outputLevel != null ? outputLevel : "Info";
    }

    public String getChecksumPolicy() {
        return checksumPolicy;
    }

    public void setChecksumPolicy(String checksumPolicy) {
        this.checksumPolicy = checksumPolicy != null ? checksumPolicy : "No Global Policy";
    }

    public String getMultiprojectFailPolicy() {
        return multiprojectFailPolicy;
    }

    public void setMultiprojectFailPolicy(String multiprojectFailPolicy) {
        this.multiprojectFailPolicy = multiprojectFailPolicy != null ? multiprojectFailPolicy : "Default";
    }

    public String getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(String threadCount) {
        this.threadCount = threadCount != null ? threadCount : "";
    }

    public String getMavenHome() {
        return mavenHome;
    }

    public void setMavenHome(String mavenHome) {
        this.mavenHome = mavenHome != null ? mavenHome : "Bundled (Maven 3)";
    }

    public String getMavenVersion() {
        return mavenVersion;
    }

    public void setMavenVersion(String mavenVersion) {
        this.mavenVersion = mavenVersion != null ? mavenVersion : "3.9.11";
    }

    public boolean isUserSettingsOverride() {
        return userSettingsOverride;
    }

    public void setUserSettingsOverride(boolean userSettingsOverride) {
        this.userSettingsOverride = userSettingsOverride;
    }

    public String getUserSettingsFile() {
        return userSettingsFile;
    }

    public void setUserSettingsFile(String userSettingsFile) {
        this.userSettingsFile = userSettingsFile;
    }

    public boolean isLocalRepoOverride() {
        return localRepoOverride;
    }

    public void setLocalRepoOverride(boolean localRepoOverride) {
        this.localRepoOverride = localRepoOverride;
    }

    public String getLocalRepo() {
        return localRepo;
    }

    public void setLocalRepo(String localRepo) {
        this.localRepo = localRepo;
    }

    public boolean isUseMavenConfig() {
        return useMavenConfig;
    }

    public void setUseMavenConfig(boolean useMavenConfig) {
        this.useMavenConfig = useMavenConfig;
    }

    @Override
    public MavenSettings clone() {
        return new MavenSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MavenSettings that = (MavenSettings) o;
        return workOffline == that.workOffline &&
                executeGoalsRecursively == that.executeGoalsRecursively &&
                printExceptionStackTraces == that.printExceptionStackTraces &&
                alwaysUpdateSnapshots == that.alwaysUpdateSnapshots &&
                userSettingsOverride == that.userSettingsOverride &&
                localRepoOverride == that.localRepoOverride &&
                useMavenConfig == that.useMavenConfig &&
                Objects.equals(outputLevel, that.outputLevel) &&
                Objects.equals(checksumPolicy, that.checksumPolicy) &&
                Objects.equals(multiprojectFailPolicy, that.multiprojectFailPolicy) &&
                Objects.equals(threadCount, that.threadCount) &&
                Objects.equals(mavenHome, that.mavenHome) &&
                Objects.equals(mavenVersion, that.mavenVersion) &&
                Objects.equals(userSettingsFile, that.userSettingsFile) &&
                Objects.equals(localRepo, that.localRepo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workOffline, executeGoalsRecursively, printExceptionStackTraces,
                alwaysUpdateSnapshots, outputLevel, checksumPolicy, multiprojectFailPolicy,
                threadCount, mavenHome, mavenVersion, userSettingsOverride, userSettingsFile,
                localRepoOverride, localRepo, useMavenConfig);
    }
}
