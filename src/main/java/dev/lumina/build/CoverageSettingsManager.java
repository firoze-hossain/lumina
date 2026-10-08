package dev.lumina.build;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Singleton manager responsible for loading and saving Coverage settings in Lumina IDE.
 */
public class CoverageSettingsManager {

    public static final String KEY_GATHER_POLICY = "coverage.gather.policy";
    public static final String KEY_ACTIVATE_VIEW = "coverage.activate.view";
    public static final String KEY_SHOW_IN_PROJECT_VIEW = "coverage.show.in.project.view";
    public static final String KEY_PYTHON_USE_BUNDLED = "coverage.python.use.bundled";
    public static final String KEY_PYTHON_BRANCH = "coverage.python.branch";
    public static final String KEY_JAVA_RUNNER = "coverage.java.runner";
    public static final String KEY_JAVA_BRANCH = "coverage.java.branch";
    public static final String KEY_JAVA_TRACK_PER_TEST = "coverage.java.track.per.test";
    public static final String KEY_JAVA_TEST_FOLDERS = "coverage.java.test.folders";
    public static final String KEY_JAVA_IGNORE_DEFAULT_CONSTRUCTORS = "coverage.java.ignore.default.constructors";
    public static final String KEY_EXCLUDE_ANNOTATIONS = "coverage.exclude.annotations";

    private static volatile CoverageSettingsManager instance;
    private CoverageSettings currentSettings;

    private CoverageSettingsManager() {
        loadSettings();
    }

    public static CoverageSettingsManager getInstance() {
        if (instance == null) {
            synchronized (CoverageSettingsManager.class) {
                if (instance == null) {
                    instance = new CoverageSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized CoverageSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(CoverageSettings newSettings) {
        if (newSettings == null) return;
        this.currentSettings = newSettings.clone();
        saveSettings();
    }

    public synchronized void loadSettings() {
        CoverageSettings s = new CoverageSettings();

        String pol = Settings.get(KEY_GATHER_POLICY);
        if (pol != null) {
            try {
                s.setGatherPolicy(CoverageSettings.GatherPolicy.valueOf(pol));
            } catch (IllegalArgumentException ignored) {}
        }

        String act = Settings.get(KEY_ACTIVATE_VIEW);
        if (act != null) s.setActivateCoverageView(Boolean.parseBoolean(act));

        String showProj = Settings.get(KEY_SHOW_IN_PROJECT_VIEW);
        if (showProj != null) s.setShowCoverageInProjectView(Boolean.parseBoolean(showProj));

        String pyBundled = Settings.get(KEY_PYTHON_USE_BUNDLED);
        if (pyBundled != null) s.setPythonUseBundledCoverage(Boolean.parseBoolean(pyBundled));

        String pyBranch = Settings.get(KEY_PYTHON_BRANCH);
        if (pyBranch != null) s.setPythonBranchCoverage(Boolean.parseBoolean(pyBranch));

        String runner = Settings.get(KEY_JAVA_RUNNER);
        if (runner != null && !runner.isBlank()) s.setJavaCoverageRunner(runner);

        String javaBranch = Settings.get(KEY_JAVA_BRANCH);
        if (javaBranch != null) s.setJavaBranchCoverage(Boolean.parseBoolean(javaBranch));

        String trackTest = Settings.get(KEY_JAVA_TRACK_PER_TEST);
        if (trackTest != null) s.setJavaTrackPerTestCoverage(Boolean.parseBoolean(trackTest));

        String testFolders = Settings.get(KEY_JAVA_TEST_FOLDERS);
        if (testFolders != null) s.setJavaCollectInTestFolders(Boolean.parseBoolean(testFolders));

        String ignoreConstructors = Settings.get(KEY_JAVA_IGNORE_DEFAULT_CONSTRUCTORS);
        if (ignoreConstructors != null) s.setJavaIgnoreDefaultConstructors(Boolean.parseBoolean(ignoreConstructors));

        String excludes = Settings.get(KEY_EXCLUDE_ANNOTATIONS);
        if (excludes != null) {
            List<String> list = new ArrayList<>();
            for (String item : excludes.split(";")) {
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) {
                    list.add(trimmed);
                }
            }
            s.setExcludeAnnotations(list);
        }

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.set(KEY_GATHER_POLICY, currentSettings.getGatherPolicy().name());
        Settings.set(KEY_ACTIVATE_VIEW, String.valueOf(currentSettings.isActivateCoverageView()));
        Settings.set(KEY_SHOW_IN_PROJECT_VIEW, String.valueOf(currentSettings.isShowCoverageInProjectView()));
        Settings.set(KEY_PYTHON_USE_BUNDLED, String.valueOf(currentSettings.isPythonUseBundledCoverage()));
        Settings.set(KEY_PYTHON_BRANCH, String.valueOf(currentSettings.isPythonBranchCoverage()));
        Settings.set(KEY_JAVA_RUNNER, currentSettings.getJavaCoverageRunner());
        Settings.set(KEY_JAVA_BRANCH, String.valueOf(currentSettings.isJavaBranchCoverage()));
        Settings.set(KEY_JAVA_TRACK_PER_TEST, String.valueOf(currentSettings.isJavaTrackPerTestCoverage()));
        Settings.set(KEY_JAVA_TEST_FOLDERS, String.valueOf(currentSettings.isJavaCollectInTestFolders()));
        Settings.set(KEY_JAVA_IGNORE_DEFAULT_CONSTRUCTORS, String.valueOf(currentSettings.isJavaIgnoreDefaultConstructors()));
        Settings.set(KEY_EXCLUDE_ANNOTATIONS, String.join(";", currentSettings.getExcludeAnnotations()));
    }
}
