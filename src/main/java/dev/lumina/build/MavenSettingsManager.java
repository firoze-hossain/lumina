package dev.lumina.build;

import dev.lumina.project.JdkMetadata;
import dev.lumina.project.MavenArchetypeMetadata.CatalogEntry;
import dev.lumina.util.Settings;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service managing Maven configuration, discovering installations,
 * and handling dynamic persistence for Maven subpages in Lumina IDE.
 */
public class MavenSettingsManager {

    // --- Base Keys ---
    public static final String KEY_WORK_OFFLINE = "maven.work.offline";
    public static final String KEY_EXECUTE_RECURSIVELY = "maven.execute.goals.recursively";
    public static final String KEY_PRINT_STACKTRACES = "maven.print.stacktraces";
    public static final String KEY_UPDATE_SNAPSHOTS = "maven.always.update.snapshots";
    public static final String KEY_OUTPUT_LEVEL = "maven.output.level";
    public static final String KEY_CHECKSUM_POLICY = "maven.checksum.policy";
    public static final String KEY_FAIL_POLICY = "maven.multiproject.fail.policy";
    public static final String KEY_THREAD_COUNT = "maven.thread.count";
    public static final String KEY_MAVEN_HOME = "maven.home.path";
    public static final String KEY_MAVEN_VERSION = "maven.home.version";
    public static final String KEY_SETTINGS_OVERRIDE = "maven.user.settings.override";
    public static final String KEY_USER_SETTINGS = "maven.user.settings";
    public static final String KEY_REPO_OVERRIDE = "maven.local.repo.override";
    public static final String KEY_LOCAL_REPO = "maven.local.repo";
    public static final String KEY_USE_MVN_CONFIG = "maven.use.mvn.config";

    // --- Subpage Keys ---
    public static final String KEY_ARCHETYPE_CUSTOM_CATALOGS = "maven.archetype.custom.catalogs";
    public static final String KEY_IGNORED_PATH_PATTERNS = "maven.ignored.path.patterns";
    public static final String KEY_IGNORED_FILES = "maven.ignored.files";

    public static final String KEY_IMPORT_DETECT_COMPILER = "maven.importing.detect.compiler";
    public static final String KEY_IMPORT_EXCLUDE_TARGET = "maven.importing.exclude.target";
    public static final String KEY_IMPORT_USE_OUTPUT_DIRS = "maven.importing.use.output.dirs";
    public static final String KEY_IMPORT_GEN_SOURCES_MODE = "maven.importing.gen.sources.mode";
    public static final String KEY_IMPORT_FOLDERS_PHASE = "maven.importing.folders.phase";
    public static final String KEY_IMPORT_DOWNLOAD_SOURCES = "maven.importing.download.sources";
    public static final String KEY_IMPORT_DOWNLOAD_DOCS = "maven.importing.download.docs";
    public static final String KEY_IMPORT_DOWNLOAD_ANNOTATIONS = "maven.importing.download.annotations";
    public static final String KEY_IMPORT_DEPENDENCY_TYPES = "maven.importing.dependency.types";
    public static final String KEY_IMPORT_VM_OPTIONS = "maven.importing.vm.options";
    public static final String KEY_IMPORT_JDK = "maven.importing.jdk";

    public static final String KEY_REPO_UPDATED_TIMESTAMPS = "maven.repositories.updated.timestamps";
    public static final String KEY_REPO_CUSTOM = "maven.repositories.custom";

    public static final String KEY_RUNNER_DELEGATE = "maven.runner.delegate";
    public static final String KEY_RUNNER_VM_OPTIONS = "maven.runner.vm.options";
    public static final String KEY_RUNNER_JRE = "maven.runner.jre";
    public static final String KEY_RUNNER_ENV_VARS = "maven.runner.env.vars";
    public static final String KEY_RUNNER_SKIP_TESTS = "maven.runner.skip.tests";
    public static final String KEY_RUNNER_PROPERTIES = "maven.runner.properties";

    public static final String KEY_TEST_PASS_ARG_LINE = "maven.test.pass.arg.line";
    public static final String KEY_TEST_PASS_SYS_PROPS = "maven.test.pass.sys.props";
    public static final String KEY_TEST_PASS_ENV_VARS = "maven.test.pass.env.vars";

    private static MavenSettingsManager instance;

    private MavenSettings settings = new MavenSettings();
    private final List<MavenInstallationProvider> providers = new CopyOnWriteArrayList<>();

    private MavenSettingsManager() {
        registerInstallationProvider(new DefaultMavenInstallationProvider());
        loadSettings();
    }

    public static synchronized MavenSettingsManager getInstance() {
        if (instance == null) {
            instance = new MavenSettingsManager();
        }
        return instance;
    }

    public void registerInstallationProvider(MavenInstallationProvider provider) {
        if (provider != null) {
            providers.add(provider);
        }
    }

    public List<MavenInstallation> getDiscoveredInstallations() {
        List<MavenInstallation> result = new ArrayList<>();
        for (MavenInstallationProvider p : providers) {
            List<MavenInstallation> discovered = p.discoverInstallations();
            if (discovered != null) {
                result.addAll(discovered);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public String detectVersion(String homePath) {
        for (MavenInstallationProvider p : providers) {
            String v = p.detectVersion(homePath);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "3.9.11";
    }

    public MavenSettings getSettings() {
        return settings.clone();
    }

    public void setSettings(MavenSettings newSettings) {
        if (newSettings == null) return;
        this.settings = newSettings.clone();
        saveSettings();
    }

    public void loadSettings() {
        String offline = Settings.get(KEY_WORK_OFFLINE);
        if (offline != null) settings.setWorkOffline(Boolean.parseBoolean(offline));

        String recursive = Settings.get(KEY_EXECUTE_RECURSIVELY);
        if (recursive != null) settings.setExecuteGoalsRecursively(Boolean.parseBoolean(recursive));

        String traces = Settings.get(KEY_PRINT_STACKTRACES);
        if (traces != null) settings.setPrintExceptionStackTraces(Boolean.parseBoolean(traces));

        String snapshots = Settings.get(KEY_UPDATE_SNAPSHOTS);
        if (snapshots != null) settings.setAlwaysUpdateSnapshots(Boolean.parseBoolean(snapshots));

        String level = Settings.get(KEY_OUTPUT_LEVEL);
        if (level != null && !level.isBlank()) settings.setOutputLevel(level);

        String checksum = Settings.get(KEY_CHECKSUM_POLICY);
        if (checksum != null && !checksum.isBlank()) settings.setChecksumPolicy(checksum);

        String fail = Settings.get(KEY_FAIL_POLICY);
        if (fail != null && !fail.isBlank()) settings.setMultiprojectFailPolicy(fail);

        String thread = Settings.get(KEY_THREAD_COUNT);
        if (thread != null) settings.setThreadCount(thread);

        String home = Settings.get(KEY_MAVEN_HOME);
        if (home != null && !home.isBlank()) settings.setMavenHome(home);

        String ver = Settings.get(KEY_MAVEN_VERSION);
        if (ver != null && !ver.isBlank()) settings.setMavenVersion(ver);

        String setOverride = Settings.get(KEY_SETTINGS_OVERRIDE);
        if (setOverride != null) settings.setUserSettingsOverride(Boolean.parseBoolean(setOverride));

        String userSet = Settings.get(KEY_USER_SETTINGS);
        if (userSet != null && !userSet.isBlank()) settings.setUserSettingsFile(userSet);

        String repoOverride = Settings.get(KEY_REPO_OVERRIDE);
        if (repoOverride != null) settings.setLocalRepoOverride(Boolean.parseBoolean(repoOverride));

        String repo = Settings.get(KEY_LOCAL_REPO);
        if (repo != null && !repo.isBlank()) settings.setLocalRepo(repo);

        String mvnCfg = Settings.get(KEY_USE_MVN_CONFIG);
        if (mvnCfg != null) settings.setUseMavenConfig(Boolean.parseBoolean(mvnCfg));

        // Catalogs
        String customCatRaw = Settings.get(KEY_ARCHETYPE_CUSTOM_CATALOGS);
        if (customCatRaw != null && !customCatRaw.isBlank()) {
            List<CatalogEntry> catalogs = new ArrayList<>();
            for (String item : customCatRaw.split(";;")) {
                if (item.isBlank()) continue;
                String[] parts = item.split("\\|\\|", -1);
                if (parts.length >= 2) {
                    catalogs.add(new CatalogEntry(parts[0], "Custom", parts[1], false));
                }
            }
            settings.setCustomArchetypeCatalogs(catalogs);
        }

        // Ignored files
        String ignoredPat = Settings.get(KEY_IGNORED_PATH_PATTERNS);
        if (ignoredPat != null) settings.setIgnoredPathPatterns(ignoredPat);

        String ignoredFilesRaw = Settings.get(KEY_IGNORED_FILES);
        if (ignoredFilesRaw != null && !ignoredFilesRaw.isBlank()) {
            Set<String> files = new LinkedHashSet<>();
            for (String path : ignoredFilesRaw.split(";;")) {
                if (!path.isBlank()) files.add(path);
            }
            settings.setIgnoredFiles(files);
        }

        // Importing
        String detComp = Settings.get(KEY_IMPORT_DETECT_COMPILER);
        if (detComp != null) settings.setDetectCompilerAutomatically(Boolean.parseBoolean(detComp));

        String exclTarget = Settings.get(KEY_IMPORT_EXCLUDE_TARGET);
        if (exclTarget != null) settings.setExcludeTargetDirectory(Boolean.parseBoolean(exclTarget));

        String useOutDirs = Settings.get(KEY_IMPORT_USE_OUTPUT_DIRS);
        if (useOutDirs != null) settings.setUseMavenOutputDirectories(Boolean.parseBoolean(useOutDirs));

        String genMode = Settings.get(KEY_IMPORT_GEN_SOURCES_MODE);
        if (genMode != null && !genMode.isBlank()) settings.setGeneratedSourcesMode(genMode);

        String phase = Settings.get(KEY_IMPORT_FOLDERS_PHASE);
        if (phase != null && !phase.isBlank()) settings.setFoldersUpdatePhase(phase);

        String dlSrc = Settings.get(KEY_IMPORT_DOWNLOAD_SOURCES);
        if (dlSrc != null) settings.setDownloadSources(Boolean.parseBoolean(dlSrc));

        String dlDocs = Settings.get(KEY_IMPORT_DOWNLOAD_DOCS);
        if (dlDocs != null) settings.setDownloadDocumentation(Boolean.parseBoolean(dlDocs));

        String dlAnn = Settings.get(KEY_IMPORT_DOWNLOAD_ANNOTATIONS);
        if (dlAnn != null) settings.setDownloadAnnotations(Boolean.parseBoolean(dlAnn));

        String depTypes = Settings.get(KEY_IMPORT_DEPENDENCY_TYPES);
        if (depTypes != null) settings.setDependencyTypes(depTypes);

        String impVm = Settings.get(KEY_IMPORT_VM_OPTIONS);
        if (impVm != null) settings.setImporterVmOptions(impVm);

        String impJdk = Settings.get(KEY_IMPORT_JDK);
        if (impJdk != null) settings.setImporterJdk(impJdk);

        // Repositories
        String repoTimestampsRaw = Settings.get(KEY_REPO_UPDATED_TIMESTAMPS);
        if (repoTimestampsRaw != null && !repoTimestampsRaw.isBlank()) {
            Map<String, String> map = new LinkedHashMap<>();
            for (String pair : repoTimestampsRaw.split(";;")) {
                String[] parts = pair.split("\\|\\|", -1);
                if (parts.length >= 2) {
                    map.put(parts[0], parts[1]);
                }
            }
            settings.setRepositoryUpdatedTimestamps(map);
        }

        String customReposRaw = Settings.get(KEY_REPO_CUSTOM);
        if (customReposRaw != null && !customReposRaw.isBlank()) {
            List<String> list = new ArrayList<>();
            for (String r : customReposRaw.split(";;")) {
                if (!r.isBlank()) list.add(r);
            }
            settings.setCustomRepositories(list);
        }

        // Runner
        String runDel = Settings.get(KEY_RUNNER_DELEGATE);
        if (runDel != null) settings.setDelegateBuildRunToMaven(Boolean.parseBoolean(runDel));

        String runVm = Settings.get(KEY_RUNNER_VM_OPTIONS);
        if (runVm != null) settings.setRunnerVmOptions(runVm);

        String runJre = Settings.get(KEY_RUNNER_JRE);
        if (runJre != null) settings.setRunnerJre(runJre);

        String runEnv = Settings.get(KEY_RUNNER_ENV_VARS);
        if (runEnv != null) settings.setEnvironmentVariables(runEnv);

        String runSkip = Settings.get(KEY_RUNNER_SKIP_TESTS);
        if (runSkip != null) settings.setSkipTests(Boolean.parseBoolean(runSkip));

        String runPropsRaw = Settings.get(KEY_RUNNER_PROPERTIES);
        if (runPropsRaw != null && !runPropsRaw.isBlank()) {
            Map<String, String> props = new LinkedHashMap<>();
            for (String pair : runPropsRaw.split(";;")) {
                String[] parts = pair.split("\\|\\|", -1);
                if (parts.length >= 2) {
                    props.put(parts[0], parts[1]);
                }
            }
            settings.setRunnerProperties(props);
        }

        // Running Tests
        String testArg = Settings.get(KEY_TEST_PASS_ARG_LINE);
        if (testArg != null) settings.setPassArgLine(Boolean.parseBoolean(testArg));

        String testSys = Settings.get(KEY_TEST_PASS_SYS_PROPS);
        if (testSys != null) settings.setPassSystemPropertyVariables(Boolean.parseBoolean(testSys));

        String testEnv = Settings.get(KEY_TEST_PASS_ENV_VARS);
        if (testEnv != null) settings.setPassEnvironmentVariables(Boolean.parseBoolean(testEnv));
    }

    public void saveSettings() {
        Settings.set(KEY_WORK_OFFLINE, String.valueOf(settings.isWorkOffline()));
        Settings.set(KEY_EXECUTE_RECURSIVELY, String.valueOf(settings.isExecuteGoalsRecursively()));
        Settings.set(KEY_PRINT_STACKTRACES, String.valueOf(settings.isPrintExceptionStackTraces()));
        Settings.set(KEY_UPDATE_SNAPSHOTS, String.valueOf(settings.isAlwaysUpdateSnapshots()));
        Settings.set(KEY_OUTPUT_LEVEL, settings.getOutputLevel());
        Settings.set(KEY_CHECKSUM_POLICY, settings.getChecksumPolicy());
        Settings.set(KEY_FAIL_POLICY, settings.getMultiprojectFailPolicy());
        Settings.set(KEY_THREAD_COUNT, settings.getThreadCount());
        Settings.set(KEY_MAVEN_HOME, settings.getMavenHome());
        Settings.set(KEY_MAVEN_VERSION, settings.getMavenVersion());
        Settings.set(KEY_SETTINGS_OVERRIDE, String.valueOf(settings.isUserSettingsOverride()));
        Settings.set(KEY_USER_SETTINGS, settings.getUserSettingsFile());
        Settings.set(KEY_REPO_OVERRIDE, String.valueOf(settings.isLocalRepoOverride()));
        Settings.set(KEY_LOCAL_REPO, settings.getLocalRepo());
        Settings.set(KEY_USE_MVN_CONFIG, String.valueOf(settings.isUseMavenConfig()));

        // Catalogs
        StringBuilder sbCat = new StringBuilder();
        for (CatalogEntry c : settings.getCustomArchetypeCatalogs()) {
            if (!sbCat.isEmpty()) sbCat.append(";;");
            sbCat.append(c.name()).append("||").append(c.location());
        }
        Settings.set(KEY_ARCHETYPE_CUSTOM_CATALOGS, sbCat.toString());

        // Ignored files
        Settings.set(KEY_IGNORED_PATH_PATTERNS, settings.getIgnoredPathPatterns());
        StringBuilder sbIgn = new StringBuilder();
        for (String f : settings.getIgnoredFiles()) {
            if (!sbIgn.isEmpty()) sbIgn.append(";;");
            sbIgn.append(f);
        }
        Settings.set(KEY_IGNORED_FILES, sbIgn.toString());

        // Importing
        Settings.set(KEY_IMPORT_DETECT_COMPILER, String.valueOf(settings.isDetectCompilerAutomatically()));
        Settings.set(KEY_IMPORT_EXCLUDE_TARGET, String.valueOf(settings.isExcludeTargetDirectory()));
        Settings.set(KEY_IMPORT_USE_OUTPUT_DIRS, String.valueOf(settings.isUseMavenOutputDirectories()));
        Settings.set(KEY_IMPORT_GEN_SOURCES_MODE, settings.getGeneratedSourcesMode());
        Settings.set(KEY_IMPORT_FOLDERS_PHASE, settings.getFoldersUpdatePhase());
        Settings.set(KEY_IMPORT_DOWNLOAD_SOURCES, String.valueOf(settings.isDownloadSources()));
        Settings.set(KEY_IMPORT_DOWNLOAD_DOCS, String.valueOf(settings.isDownloadDocumentation()));
        Settings.set(KEY_IMPORT_DOWNLOAD_ANNOTATIONS, String.valueOf(settings.isDownloadAnnotations()));
        Settings.set(KEY_IMPORT_DEPENDENCY_TYPES, settings.getDependencyTypes());
        Settings.set(KEY_IMPORT_VM_OPTIONS, settings.getImporterVmOptions());
        Settings.set(KEY_IMPORT_JDK, settings.getImporterJdk());

        // Repositories
        StringBuilder sbRepo = new StringBuilder();
        for (Map.Entry<String, String> e : settings.getRepositoryUpdatedTimestamps().entrySet()) {
            if (!sbRepo.isEmpty()) sbRepo.append(";;");
            sbRepo.append(e.getKey()).append("||").append(e.getValue());
        }
        Settings.set(KEY_REPO_UPDATED_TIMESTAMPS, sbRepo.toString());

        StringBuilder sbCustomRepo = new StringBuilder();
        for (String r : settings.getCustomRepositories()) {
            if (!sbCustomRepo.isEmpty()) sbCustomRepo.append(";;");
            sbCustomRepo.append(r);
        }
        Settings.set(KEY_REPO_CUSTOM, sbCustomRepo.toString());

        // Runner
        Settings.set(KEY_RUNNER_DELEGATE, String.valueOf(settings.isDelegateBuildRunToMaven()));
        Settings.set(KEY_RUNNER_VM_OPTIONS, settings.getRunnerVmOptions());
        Settings.set(KEY_RUNNER_JRE, settings.getRunnerJre());
        Settings.set(KEY_RUNNER_ENV_VARS, settings.getEnvironmentVariables());
        Settings.set(KEY_RUNNER_SKIP_TESTS, String.valueOf(settings.isSkipTests()));

        StringBuilder sbProps = new StringBuilder();
        for (Map.Entry<String, String> e : settings.getRunnerProperties().entrySet()) {
            if (!sbProps.isEmpty()) sbProps.append(";;");
            sbProps.append(e.getKey()).append("||").append(e.getValue());
        }
        Settings.set(KEY_RUNNER_PROPERTIES, sbProps.toString());

        // Running Tests
        Settings.set(KEY_TEST_PASS_ARG_LINE, String.valueOf(settings.isPassArgLine()));
        Settings.set(KEY_TEST_PASS_SYS_PROPS, String.valueOf(settings.isPassSystemPropertyVariables()));
        Settings.set(KEY_TEST_PASS_ENV_VARS, String.valueOf(settings.isPassEnvironmentVariables()));
    }

    // --- Dynamic JDK Utilities ---

    /**
     * Resolves the display label for the project JDK dynamically:
     * "Use Project JDK (<Vendor> <Version> - <arch>, path: <shortenedPath>)"
     */
    public static String getDefaultProjectJdkDisplay() {
        try {
            List<JdkMetadata.JdkInstallation> jdks = JdkMetadata.detectInstallations(false);
            if (!jdks.isEmpty()) {
                JdkMetadata.JdkInstallation primary = jdks.getFirst();
                String shortenedPath = shortenPathWithEllipsis(primary.homePath(), 65);
                return "Use Project JDK (" + primary.name() + " - " + primary.architecture() + ", path: " + shortenedPath + ")";
            }
        } catch (Exception ignored) {}

        String javaHome = System.getProperty("java.home", "/opt/homebrew/opt/openjdk");
        String javaVer = System.getProperty("java.version", "25.0.1");
        String arch = System.getProperty("os.arch", "aarch64");
        String shortenedPath = shortenPathWithEllipsis(javaHome, 65);
        return "Use Project JDK (Homebrew OpenJDK " + javaVer + " - " + arch + ", path: " + shortenedPath + ")";
    }

    /**
     * Shortens a file path with middle ellipsis to fit UI layouts cleanly.
     */
    public static String shortenPathWithEllipsis(String path, int maxLength) {
        if (path == null) return "";
        if (path.length() <= maxLength) return path;
        int prefixLen = Math.max(15, maxLength / 2 - 10);
        int suffixLen = Math.max(25, maxLength - prefixLen - 3);
        if (prefixLen + suffixLen >= path.length()) return path;
        return path.substring(0, prefixLen) + "..." + path.substring(path.length() - suffixLen);
    }

    /**
     * Discovers all available JDK options for ComboBoxes.
     */
    public static List<String> getAvailableJdkOptions() {
        List<String> list = new ArrayList<>();
        list.add(getDefaultProjectJdkDisplay());

        try {
            List<JdkMetadata.JdkInstallation> jdks = JdkMetadata.detectInstallations(false);
            for (JdkMetadata.JdkInstallation inst : jdks) {
                String option = inst.formatDisplay() + " (" + inst.homePath() + ")";
                if (!list.contains(option)) {
                    list.add(option);
                }
            }
        } catch (Exception ignored) {}

        return list;
    }
}
