package dev.lumina.build;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.lumina.util.Settings;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Settings manager for Remote Jar Repositories (Maven Jar Repositories & Artifactory/Nexus Service URLs).
 * Dynamically persists repository endpoints without hardcoding.
 */
public class RemoteJarRepositoriesSettingsManager {

    public static final String KEY_MAVEN_JAR_REPOSITORIES = "remote.jar.maven.repositories";
    public static final String KEY_ARTIFACTORY_NEXUS_URLS = "remote.jar.artifactory.nexus.urls";

    private static RemoteJarRepositoriesSettingsManager instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final List<String> mavenRepositories = new ArrayList<>();
    private final List<String> artifactoryNexusUrls = new ArrayList<>();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    public record TestResult(boolean success, int responseCode, String message) {}

    private RemoteJarRepositoriesSettingsManager() {
        loadSettings();
    }

    public static synchronized RemoteJarRepositoriesSettingsManager getInstance() {
        if (instance == null) {
            instance = new RemoteJarRepositoriesSettingsManager();
        }
        return instance;
    }

    // ============================================================
    // Maven Jar Repositories
    // ============================================================

    public synchronized List<String> getMavenRepositories() {
        return new ArrayList<>(mavenRepositories);
    }

    public synchronized void setMavenRepositories(List<String> newRepos) {
        mavenRepositories.clear();
        if (newRepos != null) {
            mavenRepositories.addAll(newRepos);
        }
        saveSettings();
        notifyChanged();
    }

    public synchronized void addMavenRepository(String url) {
        if (url != null && !url.isBlank() && !mavenRepositories.contains(url.trim())) {
            mavenRepositories.add(url.trim());
            saveSettings();
            notifyChanged();
        }
    }

    public synchronized void updateMavenRepository(int index, String newUrl) {
        if (index >= 0 && index < mavenRepositories.size() && newUrl != null && !newUrl.isBlank()) {
            mavenRepositories.set(index, newUrl.trim());
            saveSettings();
            notifyChanged();
        }
    }

    public synchronized void removeMavenRepository(int index) {
        if (index >= 0 && index < mavenRepositories.size()) {
            mavenRepositories.remove(index);
            saveSettings();
            notifyChanged();
        }
    }

    public synchronized void resetMavenRepositoriesToDefault() {
        mavenRepositories.clear();
        mavenRepositories.addAll(getDefaultMavenRepositories());
        saveSettings();
        notifyChanged();
    }

    // ============================================================
    // Artifactory or Nexus Service URLs
    // ============================================================

    public synchronized List<String> getArtifactoryNexusUrls() {
        return new ArrayList<>(artifactoryNexusUrls);
    }

    public synchronized void setArtifactoryNexusUrls(List<String> newUrls) {
        artifactoryNexusUrls.clear();
        if (newUrls != null) {
            artifactoryNexusUrls.addAll(newUrls);
        }
        saveSettings();
        notifyChanged();
    }

    public synchronized void addArtifactoryNexusUrl(String url) {
        if (url != null && !url.isBlank() && !artifactoryNexusUrls.contains(url.trim())) {
            artifactoryNexusUrls.add(url.trim());
            saveSettings();
            notifyChanged();
        }
    }

    public synchronized void updateArtifactoryNexusUrl(int index, String newUrl) {
        if (index >= 0 && index < artifactoryNexusUrls.size() && newUrl != null && !newUrl.isBlank()) {
            artifactoryNexusUrls.set(index, newUrl.trim());
            saveSettings();
            notifyChanged();
        }
    }

    public synchronized void removeArtifactoryNexusUrl(int index) {
        if (index >= 0 && index < artifactoryNexusUrls.size()) {
            artifactoryNexusUrls.remove(index);
            saveSettings();
            notifyChanged();
        }
    }

    public synchronized void resetArtifactoryNexusUrlsToDefault() {
        artifactoryNexusUrls.clear();
        artifactoryNexusUrls.addAll(getDefaultArtifactoryNexusUrls());
        saveSettings();
        notifyChanged();
    }

    public synchronized void resetAllToDefaults() {
        resetMavenRepositoriesToDefault();
        resetArtifactoryNexusUrlsToDefault();
    }

    // ============================================================
    // Live Connectivity Testing
    // ============================================================

    public TestResult testServiceUrl(String urlString) {
        if (urlString == null || urlString.isBlank()) {
            return new TestResult(false, 0, "URL cannot be empty.");
        }
        try {
            URI uri = URI.create(urlString.trim());
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setInstanceFollowRedirects(true);
            int code = connection.getResponseCode();
            if (code >= 200 && code < 400) {
                return new TestResult(true, code, "Connection successful (HTTP " + code + ").");
            } else if (code == 401 || code == 403) {
                return new TestResult(true, code, "Endpoint reached, authentication required (HTTP " + code + ").");
            } else {
                return new TestResult(false, code, "Server responded with HTTP " + code + ".");
            }
        } catch (IllegalArgumentException | IOException e) {
            return new TestResult(false, 0, "Failed to connect: " + e.getMessage());
        }
    }

    // ============================================================
    // Default Presets (matching Screenshot 1)
    // ============================================================

    public static List<String> getDefaultMavenRepositories() {
        return List.of(
                "https://repo.maven.apache.org/maven2",
                "https://repo1.maven.org/maven2",
                "https://repository.jboss.org/nexus/content/repositories/public/"
        );
    }

    public static List<String> getDefaultArtifactoryNexusUrls() {
        return List.of(
                "https://oss.sonatype.org/service/local/",
                "https://repository.jboss.org/nexus/service/local/"
        );
    }

    // ============================================================
    // Persistence
    // ============================================================

    public synchronized void loadSettings() {
        try {
            String jsonMaven = Settings.get(KEY_MAVEN_JAR_REPOSITORIES);
            if (jsonMaven != null && !jsonMaven.isBlank()) {
                Type type = new TypeToken<List<String>>() {}.getType();
                List<String> loaded = GSON.fromJson(jsonMaven, type);
                mavenRepositories.clear();
                if (loaded != null) {
                    mavenRepositories.addAll(loaded);
                }
            }
        } catch (Exception ignored) {}

        if (mavenRepositories.isEmpty()) {
            mavenRepositories.addAll(getDefaultMavenRepositories());
        }

        try {
            String jsonNexus = Settings.get(KEY_ARTIFACTORY_NEXUS_URLS);
            if (jsonNexus != null && !jsonNexus.isBlank()) {
                Type type = new TypeToken<List<String>>() {}.getType();
                List<String> loaded = GSON.fromJson(jsonNexus, type);
                artifactoryNexusUrls.clear();
                if (loaded != null) {
                    artifactoryNexusUrls.addAll(loaded);
                }
            }
        } catch (Exception ignored) {}

        if (artifactoryNexusUrls.isEmpty()) {
            artifactoryNexusUrls.addAll(getDefaultArtifactoryNexusUrls());
        }
    }

    public synchronized void saveSettings() {
        try {
            Settings.put(KEY_MAVEN_JAR_REPOSITORIES, GSON.toJson(mavenRepositories));
            Settings.put(KEY_ARTIFACTORY_NEXUS_URLS, GSON.toJson(artifactoryNexusUrls));
        } catch (Exception ignored) {}
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void notifyChanged() {
        for (Runnable r : changeListeners) {
            try {
                r.run();
            } catch (Exception ignored) {}
        }
    }
}
