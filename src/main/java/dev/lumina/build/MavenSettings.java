package dev.lumina.build;

import dev.lumina.project.MavenArchetypeMetadata;
import dev.lumina.project.MavenArchetypeMetadata.CatalogEntry;
import dev.lumina.util.Settings;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Model representing Maven build settings in Lumina IDE.
 * Matches Build, Execution, Deployment > Build Tools > Maven subpages.
 */
public class MavenSettings implements Cloneable {

    // --- Base Maven Settings ---
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

    // --- Archetype Catalogs ---
    private List<CatalogEntry> customArchetypeCatalogs = new ArrayList<>();

    // --- Ignored Files ---
    private String ignoredPathPatterns = "";
    private Set<String> ignoredFiles = new LinkedHashSet<>();

    // --- Importing ---
    private boolean detectCompilerAutomatically = true;
    private boolean excludeTargetDirectory = true;
    private boolean useMavenOutputDirectories = true;
    private String generatedSourcesMode = "Detect automatically";
    private String foldersUpdatePhase = "process-resources";
    private boolean downloadSources = false;
    private boolean downloadDocumentation = false;
    private boolean downloadAnnotations = false;
    private String dependencyTypes = "jar, test-jar, maven-plugin, ejb, ejb-client, jboss-har, jboss-sar, war, ear, bundle";
    private String importerVmOptions = "";
    private String importerJdk = "";

    // --- Repositories ---
    private Map<String, String> repositoryUpdatedTimestamps = new LinkedHashMap<>();
    private List<String> customRepositories = new ArrayList<>();

    // --- Runner ---
    private boolean delegateBuildRunToMaven = false;
    private String runnerVmOptions = "";
    private String runnerJre = "";
    private String environmentVariables = "";
    private boolean skipTests = false;
    private Map<String, String> runnerProperties = new LinkedHashMap<>();

    // --- Running Tests ---
    private boolean passArgLine = true;
    private boolean passSystemPropertyVariables = true;
    private boolean passEnvironmentVariables = true;

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

            if (other.customArchetypeCatalogs != null) {
                this.customArchetypeCatalogs = new ArrayList<>(other.customArchetypeCatalogs);
            }
            this.ignoredPathPatterns = other.ignoredPathPatterns;
            if (other.ignoredFiles != null) {
                this.ignoredFiles = new LinkedHashSet<>(other.ignoredFiles);
            }

            this.detectCompilerAutomatically = other.detectCompilerAutomatically;
            this.excludeTargetDirectory = other.excludeTargetDirectory;
            this.useMavenOutputDirectories = other.useMavenOutputDirectories;
            this.generatedSourcesMode = other.generatedSourcesMode;
            this.foldersUpdatePhase = other.foldersUpdatePhase;
            this.downloadSources = other.downloadSources;
            this.downloadDocumentation = other.downloadDocumentation;
            this.downloadAnnotations = other.downloadAnnotations;
            this.dependencyTypes = other.dependencyTypes;
            this.importerVmOptions = other.importerVmOptions;
            this.importerJdk = other.importerJdk;

            if (other.repositoryUpdatedTimestamps != null) {
                this.repositoryUpdatedTimestamps = new LinkedHashMap<>(other.repositoryUpdatedTimestamps);
            }
            if (other.customRepositories != null) {
                this.customRepositories = new ArrayList<>(other.customRepositories);
            }

            this.delegateBuildRunToMaven = other.delegateBuildRunToMaven;
            this.runnerVmOptions = other.runnerVmOptions;
            this.runnerJre = other.runnerJre;
            this.environmentVariables = other.environmentVariables;
            this.skipTests = other.skipTests;
            if (other.runnerProperties != null) {
                this.runnerProperties = new LinkedHashMap<>(other.runnerProperties);
            }

            this.passArgLine = other.passArgLine;
            this.passSystemPropertyVariables = other.passSystemPropertyVariables;
            this.passEnvironmentVariables = other.passEnvironmentVariables;
        }
    }

    // --- Base Getters / Setters ---

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

    // --- Archetype Catalogs ---

    public List<CatalogEntry> getArchetypeCatalogs() {
        List<CatalogEntry> list = new ArrayList<>();
        list.add(new CatalogEntry("Internal", "System", "", true));
        list.add(new CatalogEntry("Default Local", "System", getEffectiveLocalRepo(), true));
        list.add(new CatalogEntry("Maven Central", "System", "https://repo.maven.apache.org/maven2", true));
        if (customArchetypeCatalogs != null) {
            list.addAll(customArchetypeCatalogs);
        }
        return list;
    }

    public List<CatalogEntry> getCustomArchetypeCatalogs() {
        return customArchetypeCatalogs != null ? new ArrayList<>(customArchetypeCatalogs) : new ArrayList<>();
    }

    public void setCustomArchetypeCatalogs(List<CatalogEntry> customArchetypeCatalogs) {
        this.customArchetypeCatalogs = customArchetypeCatalogs != null ? new ArrayList<>(customArchetypeCatalogs) : new ArrayList<>();
    }

    public String getEffectiveLocalRepo() {
        if (localRepoOverride && localRepo != null && !localRepo.isBlank()) {
            return localRepo.trim();
        }
        return MavenArchetypeMetadata.resolveDefaultLocalRepo();
    }

    // --- Ignored Files ---

    public String getIgnoredPathPatterns() {
        return ignoredPathPatterns != null ? ignoredPathPatterns : "";
    }

    public void setIgnoredPathPatterns(String ignoredPathPatterns) {
        this.ignoredPathPatterns = ignoredPathPatterns != null ? ignoredPathPatterns : "";
    }

    public Set<String> getIgnoredFiles() {
        return ignoredFiles != null ? new LinkedHashSet<>(ignoredFiles) : new LinkedHashSet<>();
    }

    public void setIgnoredFiles(Set<String> ignoredFiles) {
        this.ignoredFiles = ignoredFiles != null ? new LinkedHashSet<>(ignoredFiles) : new LinkedHashSet<>();
    }

    public boolean isFileIgnored(String filePath) {
        if (filePath == null || filePath.isBlank()) return false;
        String normalized = Path.of(filePath).toAbsolutePath().normalize().toString();
        if (ignoredFiles.contains(normalized) || ignoredFiles.contains(filePath)) {
            return true;
        }
        // Check pattern match
        if (ignoredPathPatterns != null && !ignoredPathPatterns.isBlank()) {
            for (String pattern : ignoredPathPatterns.split(",")) {
                String p = pattern.trim();
                if (!p.isEmpty() && matchesPattern(normalized, p)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesPattern(String text, String glob) {
        String regex = "\\Q" + glob.replace("\\", "/")
                .replace("*", "\\E.*\\Q")
                .replace("?", "\\E.\\Q") + "\\E";
        String normalizedText = text.replace("\\", "/");
        return normalizedText.matches(".*" + regex + ".*") || normalizedText.matches(regex);
    }

    /**
     * Dynamically discovers all pom.xml files in the active project directory.
     */
    public static List<String> discoverProjectPomFiles() {
        List<String> poms = new ArrayList<>();
        String last = Settings.get(Settings.LAST_PROJECT);
        Path root = (last != null && !last.isBlank() && Files.isDirectory(Path.of(last)))
                ? Path.of(last)
                : Path.of("").toAbsolutePath();
        if (Files.isDirectory(root)) {
            try (var stream = Files.walk(root, 6)) {
                stream.filter(p -> p.getFileName().toString().equals("pom.xml")
                                && !p.toString().contains("/target/")
                                && !p.toString().contains("/.git/")
                                && !p.toString().contains("/.idea/"))
                        .forEach(p -> poms.add(p.toAbsolutePath().normalize().toString()));
            } catch (Exception ignored) {}
        }
        Collections.sort(poms);
        return poms;
    }

    // --- Importing ---

    public boolean isDetectCompilerAutomatically() {
        return detectCompilerAutomatically;
    }

    public void setDetectCompilerAutomatically(boolean detectCompilerAutomatically) {
        this.detectCompilerAutomatically = detectCompilerAutomatically;
    }

    public boolean isExcludeTargetDirectory() {
        return excludeTargetDirectory;
    }

    public void setExcludeTargetDirectory(boolean excludeTargetDirectory) {
        this.excludeTargetDirectory = excludeTargetDirectory;
    }

    public boolean isUseMavenOutputDirectories() {
        return useMavenOutputDirectories;
    }

    public void setUseMavenOutputDirectories(boolean useMavenOutputDirectories) {
        this.useMavenOutputDirectories = useMavenOutputDirectories;
    }

    public String getGeneratedSourcesMode() {
        return generatedSourcesMode != null ? generatedSourcesMode : "Detect automatically";
    }

    public void setGeneratedSourcesMode(String generatedSourcesMode) {
        this.generatedSourcesMode = generatedSourcesMode != null ? generatedSourcesMode : "Detect automatically";
    }

    public String getFoldersUpdatePhase() {
        return foldersUpdatePhase != null ? foldersUpdatePhase : "process-resources";
    }

    public void setFoldersUpdatePhase(String foldersUpdatePhase) {
        this.foldersUpdatePhase = foldersUpdatePhase != null ? foldersUpdatePhase : "process-resources";
    }

    public boolean isDownloadSources() {
        return downloadSources;
    }

    public void setDownloadSources(boolean downloadSources) {
        this.downloadSources = downloadSources;
    }

    public boolean isDownloadDocumentation() {
        return downloadDocumentation;
    }

    public void setDownloadDocumentation(boolean downloadDocumentation) {
        this.downloadDocumentation = downloadDocumentation;
    }

    public boolean isDownloadAnnotations() {
        return downloadAnnotations;
    }

    public void setDownloadAnnotations(boolean downloadAnnotations) {
        this.downloadAnnotations = downloadAnnotations;
    }

    public String getDependencyTypes() {
        return dependencyTypes != null ? dependencyTypes : "";
    }

    public void setDependencyTypes(String dependencyTypes) {
        this.dependencyTypes = dependencyTypes != null ? dependencyTypes : "";
    }

    public String getImporterVmOptions() {
        return importerVmOptions != null ? importerVmOptions : "";
    }

    public void setImporterVmOptions(String importerVmOptions) {
        this.importerVmOptions = importerVmOptions != null ? importerVmOptions : "";
    }

    public String getImporterJdk() {
        return importerJdk != null ? importerJdk : "";
    }

    public void setImporterJdk(String importerJdk) {
        this.importerJdk = importerJdk != null ? importerJdk : "";
    }

    // --- Repositories ---

    public record RepositoryItem(String url, String type, String updated) {}

    public List<RepositoryItem> getRepositories() {
        List<RepositoryItem> list = new ArrayList<>();
        String localUrl = getEffectiveLocalRepo();
        list.add(new RepositoryItem(localUrl, "Local", repositoryUpdatedTimestamps.getOrDefault(localUrl, "")));
        list.add(new RepositoryItem("https://repo.maven.apache.org/maven2", "Remote", repositoryUpdatedTimestamps.getOrDefault("https://repo.maven.apache.org/maven2", "")));
        if (customRepositories != null) {
            for (String r : customRepositories) {
                list.add(new RepositoryItem(r, "Remote", repositoryUpdatedTimestamps.getOrDefault(r, "")));
            }
        }
        return list;
    }

    public Map<String, String> getRepositoryUpdatedTimestamps() {
        return repositoryUpdatedTimestamps != null ? new LinkedHashMap<>(repositoryUpdatedTimestamps) : new LinkedHashMap<>();
    }

    public void setRepositoryUpdatedTimestamps(Map<String, String> repositoryUpdatedTimestamps) {
        this.repositoryUpdatedTimestamps = repositoryUpdatedTimestamps != null ? new LinkedHashMap<>(repositoryUpdatedTimestamps) : new LinkedHashMap<>();
    }

    public void setRepositoryUpdated(String url, String timestamp) {
        if (url != null && !url.isBlank()) {
            this.repositoryUpdatedTimestamps.put(url.trim(), timestamp != null ? timestamp : "");
        }
    }

    public List<String> getCustomRepositories() {
        return customRepositories != null ? new ArrayList<>(customRepositories) : new ArrayList<>();
    }

    public void setCustomRepositories(List<String> customRepositories) {
        this.customRepositories = customRepositories != null ? new ArrayList<>(customRepositories) : new ArrayList<>();
    }

    // --- Runner ---

    public boolean isDelegateBuildRunToMaven() {
        return delegateBuildRunToMaven;
    }

    public void setDelegateBuildRunToMaven(boolean delegateBuildRunToMaven) {
        this.delegateBuildRunToMaven = delegateBuildRunToMaven;
    }

    public String getRunnerVmOptions() {
        return runnerVmOptions != null ? runnerVmOptions : "";
    }

    public void setRunnerVmOptions(String runnerVmOptions) {
        this.runnerVmOptions = runnerVmOptions != null ? runnerVmOptions : "";
    }

    public String getRunnerJre() {
        return runnerJre != null ? runnerJre : "";
    }

    public void setRunnerJre(String runnerJre) {
        this.runnerJre = runnerJre != null ? runnerJre : "";
    }

    public String getEnvironmentVariables() {
        return environmentVariables != null ? environmentVariables : "";
    }

    public void setEnvironmentVariables(String environmentVariables) {
        this.environmentVariables = environmentVariables != null ? environmentVariables : "";
    }

    public boolean isSkipTests() {
        return skipTests;
    }

    public void setSkipTests(boolean skipTests) {
        this.skipTests = skipTests;
    }

    public Map<String, String> getRunnerProperties() {
        return runnerProperties != null ? new LinkedHashMap<>(runnerProperties) : new LinkedHashMap<>();
    }

    public void setRunnerProperties(Map<String, String> runnerProperties) {
        this.runnerProperties = runnerProperties != null ? new LinkedHashMap<>(runnerProperties) : new LinkedHashMap<>();
    }

    // --- Running Tests (media_1791428030217.png) ---

    public boolean isPassArgLine() {
        return passArgLine;
    }

    public void setPassArgLine(boolean passArgLine) {
        this.passArgLine = passArgLine;
    }

    public boolean isPassSystemPropertyVariables() {
        return passSystemPropertyVariables;
    }

    public void setPassSystemPropertyVariables(boolean passSystemPropertyVariables) {
        this.passSystemPropertyVariables = passSystemPropertyVariables;
    }

    public boolean isPassEnvironmentVariables() {
        return passEnvironmentVariables;
    }

    public void setPassEnvironmentVariables(boolean passEnvironmentVariables) {
        this.passEnvironmentVariables = passEnvironmentVariables;
    }

    public boolean isPassSystemPropertiesToMaven() {
        return passSystemPropertyVariables;
    }

    public void setPassSystemPropertiesToMaven(boolean passSystemPropertiesToMaven) {
        this.passSystemPropertyVariables = passSystemPropertiesToMaven;
    }

    public boolean isPassEnvironmentVariablesToMaven() {
        return passEnvironmentVariables;
    }

    public void setPassEnvironmentVariablesToMaven(boolean passEnvironmentVariablesToMaven) {
        this.passEnvironmentVariables = passEnvironmentVariablesToMaven;
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
                detectCompilerAutomatically == that.detectCompilerAutomatically &&
                excludeTargetDirectory == that.excludeTargetDirectory &&
                useMavenOutputDirectories == that.useMavenOutputDirectories &&
                downloadSources == that.downloadSources &&
                downloadDocumentation == that.downloadDocumentation &&
                downloadAnnotations == that.downloadAnnotations &&
                delegateBuildRunToMaven == that.delegateBuildRunToMaven &&
                skipTests == that.skipTests &&
                passArgLine == that.passArgLine &&
                passSystemPropertyVariables == that.passSystemPropertyVariables &&
                passEnvironmentVariables == that.passEnvironmentVariables &&
                Objects.equals(outputLevel, that.outputLevel) &&
                Objects.equals(checksumPolicy, that.checksumPolicy) &&
                Objects.equals(multiprojectFailPolicy, that.multiprojectFailPolicy) &&
                Objects.equals(threadCount, that.threadCount) &&
                Objects.equals(mavenHome, that.mavenHome) &&
                Objects.equals(mavenVersion, that.mavenVersion) &&
                Objects.equals(userSettingsFile, that.userSettingsFile) &&
                Objects.equals(localRepo, that.localRepo) &&
                Objects.equals(customArchetypeCatalogs, that.customArchetypeCatalogs) &&
                Objects.equals(ignoredPathPatterns, that.ignoredPathPatterns) &&
                Objects.equals(ignoredFiles, that.ignoredFiles) &&
                Objects.equals(generatedSourcesMode, that.generatedSourcesMode) &&
                Objects.equals(foldersUpdatePhase, that.foldersUpdatePhase) &&
                Objects.equals(dependencyTypes, that.dependencyTypes) &&
                Objects.equals(importerVmOptions, that.importerVmOptions) &&
                Objects.equals(importerJdk, that.importerJdk) &&
                Objects.equals(repositoryUpdatedTimestamps, that.repositoryUpdatedTimestamps) &&
                Objects.equals(customRepositories, that.customRepositories) &&
                Objects.equals(runnerVmOptions, that.runnerVmOptions) &&
                Objects.equals(runnerJre, that.runnerJre) &&
                Objects.equals(environmentVariables, that.environmentVariables) &&
                Objects.equals(runnerProperties, that.runnerProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workOffline, executeGoalsRecursively, printExceptionStackTraces,
                alwaysUpdateSnapshots, outputLevel, checksumPolicy, multiprojectFailPolicy,
                threadCount, mavenHome, mavenVersion, userSettingsOverride, userSettingsFile,
                localRepoOverride, localRepo, useMavenConfig, customArchetypeCatalogs,
                ignoredPathPatterns, ignoredFiles, detectCompilerAutomatically,
                excludeTargetDirectory, useMavenOutputDirectories, generatedSourcesMode,
                foldersUpdatePhase, downloadSources, downloadDocumentation, downloadAnnotations,
                dependencyTypes, importerVmOptions, importerJdk, repositoryUpdatedTimestamps,
                customRepositories, delegateBuildRunToMaven, runnerVmOptions, runnerJre,
                environmentVariables, skipTests, runnerProperties, passArgLine,
                passSystemPropertyVariables, passEnvironmentVariables);
    }
}
