package dev.lumina.build;

import java.io.File;
import java.util.Objects;

/**
 * Configuration model for Build Tools > Gradle.
 * Matches standard IDE settings:
 *  - Gradle user home (dynamic fallback to ~/.gradle or $GRADLE_USER_HOME)
 *  - Generate *.iml files for modules imported from Gradle
 *  - Enable parallel Gradle model fetching for Gradle 7.4+
 */
public class GradleSettings implements Cloneable {

    private String gradleUserHome = "";
    private boolean generateImlFiles = false;
    private boolean parallelModelFetching = false;

    public GradleSettings() {
    }

    public GradleSettings(GradleSettings other) {
        if (other != null) {
            this.gradleUserHome = other.gradleUserHome;
            this.generateImlFiles = other.generateImlFiles;
            this.parallelModelFetching = other.parallelModelFetching;
        }
    }

    /**
     * Dynamically determines the default Gradle user home directory without hardcoding.
     */
    public static String getDefaultGradleUserHome() {
        String env = System.getenv("GRADLE_USER_HOME");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        String userHome = System.getProperty("user.home", "");
        return userHome + File.separator + ".gradle";
    }

    /**
     * Returns the configured Gradle user home, or the dynamic default if blank.
     */
    public String getEffectiveGradleUserHome() {
        if (gradleUserHome != null && !gradleUserHome.isBlank()) {
            return gradleUserHome.trim();
        }
        return getDefaultGradleUserHome();
    }

    public String getGradleUserHome() {
        return gradleUserHome != null ? gradleUserHome : "";
    }

    public void setGradleUserHome(String gradleUserHome) {
        this.gradleUserHome = gradleUserHome != null ? gradleUserHome.trim() : "";
    }

    public boolean isGenerateImlFiles() {
        return generateImlFiles;
    }

    public void setGenerateImlFiles(boolean generateImlFiles) {
        this.generateImlFiles = generateImlFiles;
    }

    public boolean isParallelModelFetching() {
        return parallelModelFetching;
    }

    public void setParallelModelFetching(boolean parallelModelFetching) {
        this.parallelModelFetching = parallelModelFetching;
    }

    @Override
    public GradleSettings clone() {
        return new GradleSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GradleSettings that = (GradleSettings) o;
        return generateImlFiles == that.generateImlFiles &&
                parallelModelFetching == that.parallelModelFetching &&
                Objects.equals(gradleUserHome, that.gradleUserHome);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gradleUserHome, generateImlFiles, parallelModelFetching);
    }
}
