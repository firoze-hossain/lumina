package dev.lumina.build;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service managing Maven configuration and discovering installations in Lumina IDE.
 */
public class MavenSettingsManager {

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
    }
}
