package dev.lumina.build;

import dev.lumina.util.Settings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton manager responsible for loading, saving, and discovering Gradle configuration.
 */
public class GradleSettingsManager {

    public static final String KEY_GRADLE_USER_HOME = "gradle.user.home";
    public static final String KEY_GRADLE_GENERATE_IML = "gradle.generate.iml";
    public static final String KEY_GRADLE_PARALLEL_MODEL_FETCHING = "gradle.parallel.model.fetching";

    private static volatile GradleSettingsManager instance;
    private GradleSettings currentSettings;

    private GradleSettingsManager() {
        loadSettings();
    }

    public static GradleSettingsManager getInstance() {
        if (instance == null) {
            synchronized (GradleSettingsManager.class) {
                if (instance == null) {
                    instance = new GradleSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized GradleSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(GradleSettings newSettings) {
        if (newSettings == null) return;
        this.currentSettings = newSettings.clone();
        saveSettings();
    }

    public synchronized void loadSettings() {
        GradleSettings s = new GradleSettings();
        String home = Settings.get(KEY_GRADLE_USER_HOME);
        if (home != null) s.setGradleUserHome(home);

        String iml = Settings.get(KEY_GRADLE_GENERATE_IML);
        if (iml != null) s.setGenerateImlFiles(Boolean.parseBoolean(iml));

        String parallel = Settings.get(KEY_GRADLE_PARALLEL_MODEL_FETCHING);
        if (parallel != null) s.setParallelModelFetching(Boolean.parseBoolean(parallel));

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.set(KEY_GRADLE_USER_HOME, currentSettings.getGradleUserHome());
        Settings.set(KEY_GRADLE_GENERATE_IML, String.valueOf(currentSettings.isGenerateImlFiles()));
        Settings.set(KEY_GRADLE_PARALLEL_MODEL_FETCHING, String.valueOf(currentSettings.isParallelModelFetching()));
    }

    /**
     * Dynamically discovers installed Gradle distributions and SDKs on the current machine.
     */
    public List<String> discoverGradleInstallations() {
        List<String> list = new ArrayList<>();
        String gradleHomeEnv = System.getenv("GRADLE_HOME");
        if (gradleHomeEnv != null && !gradleHomeEnv.isBlank()) {
            File f = new File(gradleHomeEnv.trim());
            if (f.isDirectory() && !list.contains(f.getAbsolutePath())) {
                list.add(f.getAbsolutePath());
            }
        }

        String userHome = System.getProperty("user.home", "");
        if (!userHome.isBlank()) {
            File sdkmanGradle = new File(userHome, ".sdkman/candidates/gradle/current");
            if (sdkmanGradle.exists() && !list.contains(sdkmanGradle.getAbsolutePath())) {
                list.add(sdkmanGradle.getAbsolutePath());
            }
        }

        List<String> standardPaths = List.of(
                "/opt/homebrew/opt/gradle",
                "/usr/local/opt/gradle",
                "/usr/share/gradle"
        );
        for (String p : standardPaths) {
            File dir = new File(p);
            if (dir.exists() && !list.contains(dir.getAbsolutePath())) {
                list.add(dir.getAbsolutePath());
            }
        }

        return list;
    }
}
