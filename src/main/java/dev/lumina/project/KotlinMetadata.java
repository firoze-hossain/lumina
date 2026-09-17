package dev.lumina.project;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service providing dynamic Kotlin releases resolution via Maven Central metadata
 * and JDK JVM-target compatibility resolution matching IntelliJ IDEA.
 */
public final class KotlinMetadata {

    public static final String KOTLIN_METADATA_URL =
            "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/maven-metadata.xml";

    public static final String DEFAULT_KOTLIN_VERSION = "2.1.10";

    private static final List<String> FALLBACK_VERSIONS = List.of(
            "2.1.10", "2.1.0", "2.0.21", "2.0.20", "2.0.0", "1.9.25", "1.9.24", "1.8.22"
    );

    private static volatile List<String> cachedVersions = null;
    private static final Object LOCK = new Object();

    private KotlinMetadata() {
    }

    /**
     * Parses the maven-metadata.xml from Maven Central into a list of stable version strings,
     * sorted in descending semantic version order.
     */
    public static List<String> parseVersionsXml(String xml) {
        List<String> list = new ArrayList<>();
        if (xml == null || xml.isBlank()) {
            return list;
        }

        Pattern pattern = Pattern.compile("<version>([^<]+)</version>");
        Matcher matcher = pattern.matcher(xml);
        while (matcher.find()) {
            String v = matcher.group(1).trim();
            if (isStableRelease(v)) {
                list.add(v);
            }
        }

        list.sort((a, b) -> compareVersions(b, a)); // Descending
        return list;
    }

    private static boolean isStableRelease(String v) {
        if (v == null || v.isBlank()) return false;
        String lower = v.toLowerCase();
        // Filter out milestones, release candidates, betas, dev builds, and snapshots
        if (lower.contains("-") || lower.contains("m") || lower.contains("rc") ||
                lower.contains("beta") || lower.contains("alpha") || lower.contains("dev") ||
                lower.contains("eap") || lower.contains("snapshot")) {
            return false;
        }
        return Pattern.matches("^\\d+\\.\\d+(\\.\\d+)?$", v);
    }

    /**
     * Fetches stable Kotlin versions synchronously (with caching and fallback).
     */
    public static List<String> fetchVersions(boolean forceRefresh) {
        if (!forceRefresh && cachedVersions != null && !cachedVersions.isEmpty()) {
            return cachedVersions;
        }
        synchronized (LOCK) {
            if (!forceRefresh && cachedVersions != null && !cachedVersions.isEmpty()) {
                return cachedVersions;
            }
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(KOTLIN_METADATA_URL))
                        .timeout(Duration.ofSeconds(4))
                        .header("Accept", "application/xml")
                        .GET()
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    List<String> parsed = parseVersionsXml(resp.body());
                    if (!parsed.isEmpty()) {
                        cachedVersions = Collections.unmodifiableList(parsed);
                        return cachedVersions;
                    }
                }
            } catch (Exception ignored) {
            }

            cachedVersions = Collections.unmodifiableList(FALLBACK_VERSIONS);
            return cachedVersions;
        }
    }

    /**
     * Asynchronously fetches versions and notifies callback on the calling or UI thread.
     */
    public static void fetchVersionsAsync(Consumer<List<String>> callback) {
        CompletableFuture.supplyAsync(() -> fetchVersions(false))
                .thenAccept(versions -> {
                    if (callback != null) {
                        try {
                            javafx.application.Platform.runLater(() -> callback.accept(versions));
                        } catch (IllegalStateException e) {
                            callback.accept(versions);
                        }
                    }
                });
    }

    /**
     * Returns the latest stable Kotlin version.
     */
    public static String getLatestVersion() {
        List<String> versions = fetchVersions(false);
        if (!versions.isEmpty()) {
            return versions.getFirst();
        }
        return DEFAULT_KOTLIN_VERSION;
    }

    /**
     * Maps a JDK major version to the corresponding Kotlin compiler jvmTarget.
     * In modern Kotlin (2.0+), jvmTarget supports up to 22 (with 21 LTS as standard for Java 21+).
     */
    public static String getJvmTarget(int javaMajorVersion) {
        if (javaMajorVersion >= 21) {
            return "21";
        }
        if (javaMajorVersion >= 17) {
            return "17";
        }
        if (javaMajorVersion >= 11) {
            return "11";
        }
        return "1.8";
    }

    /**
     * Compares two semantic version strings (e.g. "2.1.10" vs "2.0.21").
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
