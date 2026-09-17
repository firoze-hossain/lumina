package dev.lumina.project;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service providing dynamic Gradle releases querying via official Gradle API
 * (https://services.gradle.org/versions/all), JDK-version compatibility resolution,
 * local Gradle installation detection, and offline fallbacks matching IntelliJ IDEA.
 */
public final class GradleMetadata {

    public static final String GRADLE_VERSIONS_URL = "https://services.gradle.org/versions/all";

    public record GradleRelease(
            String version,
            boolean current,
            boolean snapshot,
            boolean nightly,
            boolean releaseCandidate,
            boolean broken,
            String downloadUrl
    ) implements Comparable<GradleRelease> {

        @Override
        public int compareTo(GradleRelease other) {
            return compareVersions(this.version, other.version);
        }

        @Override
        public String toString() {
            return version;
        }
    }

    private static final List<String> FALLBACK_VERSIONS = List.of(
            "9.2.0", "9.1.0", "8.12.1", "8.12", "8.11.1", "8.10.2",
            "8.9", "8.8", "8.7", "8.6", "8.5", "8.4", "8.3",
            "8.2.1", "8.1.1", "8.0.2", "7.6.4", "7.5.1", "7.4.2", "7.3.3"
    );

    private static volatile List<GradleRelease> cachedReleases = null;
    private static final Object LOCK = new Object();

    private GradleMetadata() {
    }

    /**
     * Parses the JSON array from https://services.gradle.org/versions/all into stable GradleRelease objects.
     */
    public static List<GradleRelease> parseReleasesJson(String json) {
        List<GradleRelease> list = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return list;
        }
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonArray()) {
                return list;
            }
            JsonArray array = root.getAsJsonArray();
            for (JsonElement el : array) {
                if (!el.isJsonObject()) continue;
                JsonObject obj = el.getAsJsonObject();

                String version = obj.has("version") && !obj.get("version").isJsonNull()
                        ? obj.get("version").getAsString().trim() : "";
                if (version.isBlank()) continue;

                boolean current = obj.has("current") && !obj.get("current").isJsonNull() && obj.get("current").getAsBoolean();
                boolean snapshot = obj.has("snapshot") && !obj.get("snapshot").isJsonNull() && obj.get("snapshot").getAsBoolean();
                boolean nightly = obj.has("nightly") && !obj.get("nightly").isJsonNull() && obj.get("nightly").getAsBoolean();
                boolean releaseCandidate = obj.has("releaseCandidate") && !obj.get("releaseCandidate").isJsonNull() && obj.get("releaseCandidate").getAsBoolean();
                boolean broken = obj.has("broken") && !obj.get("broken").isJsonNull() && obj.get("broken").getAsBoolean();
                String downloadUrl = obj.has("downloadUrl") && !obj.get("downloadUrl").isJsonNull()
                        ? obj.get("downloadUrl").getAsString().trim()
                        : "https://services.gradle.org/distributions/gradle-" + version + "-bin.zip";

                // We only want stable final releases matching IntelliJ IDEA's default dropdown
                if (snapshot || nightly || broken || releaseCandidate || version.contains("-")) {
                    continue;
                }

                list.add(new GradleRelease(version, current, snapshot, nightly, releaseCandidate, broken, downloadUrl));
            }

            list.sort((a, b) -> compareVersions(b.version(), a.version())); // descending
        } catch (Exception ignored) {
        }
        return list;
    }

    /**
     * Synchronously fetches Gradle releases or returns cached/fallback releases.
     */
    public static List<GradleRelease> fetchReleases(boolean forceRefresh) {
        if (!forceRefresh && cachedReleases != null && !cachedReleases.isEmpty()) {
            return cachedReleases;
        }
        synchronized (LOCK) {
            if (!forceRefresh && cachedReleases != null && !cachedReleases.isEmpty()) {
                return cachedReleases;
            }
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(GRADLE_VERSIONS_URL))
                        .timeout(Duration.ofSeconds(4))
                        .header("Accept", "application/json")
                        .GET()
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    List<GradleRelease> parsed = parseReleasesJson(resp.body());
                    if (!parsed.isEmpty()) {
                        cachedReleases = Collections.unmodifiableList(parsed);
                        return cachedReleases;
                    }
                }
            } catch (Exception ignored) {
            }

            // Fallback list
            List<GradleRelease> fallbacks = new ArrayList<>();
            for (String v : FALLBACK_VERSIONS) {
                fallbacks.add(new GradleRelease(v, "9.2.0".equals(v), false, false, false, false,
                        "https://services.gradle.org/distributions/gradle-" + v + "-bin.zip"));
            }
            cachedReleases = Collections.unmodifiableList(fallbacks);
            return cachedReleases;
        }
    }

    /**
     * Asynchronously fetches Gradle releases and calls callback on JavaFX or current thread.
     */
    public static void fetchReleasesAsync(Consumer<List<GradleRelease>> callback) {
        CompletableFuture.supplyAsync(() -> fetchReleases(false))
                .thenAccept(releases -> {
                    if (callback != null) {
                        try {
                            javafx.application.Platform.runLater(() -> callback.accept(releases));
                        } catch (IllegalStateException e) {
                            callback.accept(releases);
                        }
                    }
                });
    }

    /**
     * Returns the minimum Gradle version required for a specific Java major version.
     * Based on official Gradle Compatibility Matrix:
     * https://docs.gradle.org/current/userguide/compatibility.html
     */
    public static double getMinGradleVersionForJava(int javaMajorVersion) {
        if (javaMajorVersion >= 25) return 9.1;
        if (javaMajorVersion == 24) return 8.12;
        if (javaMajorVersion == 23) return 8.10;
        if (javaMajorVersion == 22) return 8.7;
        if (javaMajorVersion == 21) return 8.5;
        if (javaMajorVersion == 20) return 8.3;
        if (javaMajorVersion == 19) return 7.6;
        if (javaMajorVersion == 18) return 7.5;
        if (javaMajorVersion == 17) return 7.3;
        if (javaMajorVersion == 16) return 7.0;
        if (javaMajorVersion == 15) return 6.7;
        if (javaMajorVersion == 14) return 6.3;
        if (javaMajorVersion == 13) return 6.0;
        if (javaMajorVersion == 12) return 5.4;
        if (javaMajorVersion == 11) return 5.0;
        return 2.0;
    }

    /**
     * Checks if a Gradle release version string (e.g. "9.2.0", "8.12.1") supports the given Java major version.
     */
    public static boolean isCompatible(String gradleVersion, int javaMajorVersion) {
        if (gradleVersion == null || gradleVersion.isBlank()) return false;
        double minRequired = getMinGradleVersionForJava(javaMajorVersion);
        double actual = parseVersionAsDouble(gradleVersion);
        return actual >= minRequired;
    }

    /**
     * Filters a list of releases to those compatible with the specified Java major version.
     * If no compatible versions are found (e.g. preview Java release > 25), returns the latest available releases.
     */
    public static List<GradleRelease> filterCompatibleVersions(List<GradleRelease> releases, int javaMajorVersion) {
        if (releases == null || releases.isEmpty()) {
            releases = fetchReleases(false);
        }
        List<GradleRelease> compatible = new ArrayList<>();
        for (GradleRelease r : releases) {
            if (isCompatible(r.version(), javaMajorVersion)) {
                compatible.add(r);
            }
        }
        if (compatible.isEmpty() && !releases.isEmpty()) {
            // For preview / very new Java versions, offer the newest Gradle versions available
            compatible.addAll(releases.subList(0, Math.min(releases.size(), 3)));
        }
        // In IntelliJ IDEA, compatible versions in the dropdown are sorted in ascending order (e.g. 9.1.0, 9.2.0)
        compatible.sort((a, b) -> compareVersions(a.version(), b.version()));
        return compatible;
    }

    /**
     * Returns the latest compatible Gradle version string for the given Java version.
     */
    public static String getLatestCompatibleVersion(int javaMajorVersion) {
        List<GradleRelease> comp = filterCompatibleVersions(fetchReleases(false), javaMajorVersion);
        if (!comp.isEmpty()) {
            return comp.stream()
                    .max((a, b) -> compareVersions(a.version(), b.version()))
                    .map(GradleRelease::version)
                    .orElse("9.2.0");
        }
        return "9.2.0";
    }

    /**
     * Detects local Gradle installations on the host system.
     */
    public static List<String> detectLocalInstallations() {
        List<String> detected = new ArrayList<>();

        // 1. GRADLE_HOME
        String envHome = System.getenv("GRADLE_HOME");
        if (envHome != null && isValidGradleHome(Path.of(envHome))) {
            detected.add(envHome);
        }

        // 2. Common Linux / macOS package manager locations
        List<Path> searchPaths = List.of(
                Path.of("/usr/share/gradle"),
                Path.of("/usr/lib/gradle"),
                Path.of("/opt/gradle"),
                Path.of("/usr/local/gradle"),
                Path.of("/opt/homebrew/opt/gradle"),
                Path.of("/usr/local/opt/gradle"),
                Path.of("/opt/homebrew/bin/gradle"),
                Path.of("/usr/local/bin/gradle"),
                Path.of(System.getProperty("user.home"), ".sdkman/candidates/gradle/current")
        );

        for (Path p : searchPaths) {
            if (Files.exists(p)) {
                try {
                    Path real = p.toRealPath();
                    Path home = Files.isDirectory(real) && Files.exists(real.resolve("bin/gradle"))
                            ? real
                            : (Files.isRegularFile(real) && real.getParent() != null && real.getParent().getParent() != null
                                    ? real.getParent().getParent() : null);
                    if (home != null && isValidGradleHome(home) && !detected.contains(home.toString())) {
                        detected.add(home.toString());
                    }
                } catch (Exception ignored) {
                }
            }
        }

        // 3. Search PATH for 'gradle' binary
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null && !pathEnv.isBlank()) {
            for (String part : pathEnv.split(File.pathSeparator)) {
                if (part.isBlank()) continue;
                Path binDir = Path.of(part);
                Path gradleBin = binDir.resolve("gradle");
                if (Files.isRegularFile(gradleBin)) {
                    try {
                        Path realBin = gradleBin.toRealPath();
                        Path homeCandidate = realBin.getParent() != null ? realBin.getParent().getParent() : null;
                        if (homeCandidate != null && isValidGradleHome(homeCandidate) && !detected.contains(homeCandidate.toString())) {
                            detected.add(homeCandidate.toString());
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        return detected;
    }

    /**
     * Inspects a local Gradle home directory to determine its release version.
     */
    public static String detectGradleVersionFromHome(Path home) {
        if (home == null || !Files.isDirectory(home)) return null;
        Path libDir = home.resolve("lib");
        if (Files.isDirectory(libDir)) {
            try (var stream = Files.list(libDir)) {
                for (Path jar : stream.toList()) {
                    String fileName = jar.getFileName().toString();
                    Matcher m = Pattern.compile("^gradle-(?:launcher|core|base-services)-([0-9]+(?:\\.[0-9]+)+.*?)\\.jar$").matcher(fileName);
                    if (m.find()) {
                        return m.group(1);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static boolean isValidGradleHome(Path home) {
        if (home == null || !Files.isDirectory(home)) return false;
        Path bin = home.resolve("bin");
        return Files.isRegularFile(bin.resolve("gradle")) || Files.isRegularFile(bin.resolve("gradle.bat"));
    }

    /**
     * Converts a semantic version like "9.2.0" to a comparable double 9.2 (or 8.12 -> 8.12).
     */
    public static double parseVersionAsDouble(String version) {
        if (version == null) return 0.0;
        Matcher m = Pattern.compile("^(\\d+)(?:\\.(\\d+))?").matcher(version.trim());
        if (m.find()) {
            int major = Integer.parseInt(m.group(1));
            int minor = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
            return major + (minor < 10 ? minor * 0.1 : minor * 0.01);
        }
        return 0.0;
    }

    /**
     * Compares two semantic version strings (e.g. "9.2.0" vs "9.1.0").
     */
    public static int compareVersions(String v1, String v2) {
        if (v1 == null && v2 == null) return 0;
        if (v1 == null) return -1;
        if (v2 == null) return 1;

        String[] parts1 = v1.split("[.\\-]");
        String[] parts2 = v2.split("[.\\-]");
        int len = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < len; i++) {
            int n1 = 0;
            if (i < parts1.length) {
                try { n1 = Integer.parseInt(parts1[i]); } catch (NumberFormatException ignored) {}
            }
            int n2 = 0;
            if (i < parts2.length) {
                try { n2 = Integer.parseInt(parts2[i]); } catch (NumberFormatException ignored) {}
            }
            if (n1 != n2) {
                return Integer.compare(n1, n2);
            }
        }
        return v1.compareTo(v2);
    }
}
