package dev.lumina.project;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service providing dynamic sbt and Scala versions resolution via Maven Central metadata,
 * matching IntelliJ IDEA.
 */
public final class ScalaMetadata {

    public static final String SBT_METADATA_URL =
            "https://repo1.maven.org/maven2/org/scala-sbt/sbt-launch/maven-metadata.xml";

    public static final String SCALA3_METADATA_URL =
            "https://repo1.maven.org/maven2/org/scala-lang/scala3-compiler_3/maven-metadata.xml";

    public static final String DEFAULT_SBT_VERSION = "2.0.9";
    public static final String DEFAULT_SCALA_VERSION = "3.9.0";

    /**
     * Fallback sbt versions matching IntelliJ IDEA (Image 3: 2.0.9, 2.0.8, 2.0.7...).
     */
    public static final List<String> SBT_FALLBACK_VERSIONS = List.of(
            "2.0.9", "2.0.8", "2.0.7", "2.0.6", "2.0.5", "2.0.4", "2.0.3", "2.0.2", "2.0.1", "2.0.0", "1.10.7"
    );

    /**
     * Fallback Scala versions matching IntelliJ IDEA (Image 4: 3.9.0, 3.8.4, 3.8.3, 3.8.2...).
     */
    public static final List<String> SCALA_FALLBACK_VERSIONS = List.of(
            "3.9.0", "3.8.4", "3.8.3", "3.8.2", "3.8.1", "3.8.0", "3.7.4", "3.7.3", "3.3.5", "2.13.16"
    );

    private static volatile List<String> cachedSbtVersions = null;
    private static volatile List<String> cachedScalaVersions = null;
    private static final Object SBT_LOCK = new Object();
    private static final Object SCALA_LOCK = new Object();

    private ScalaMetadata() {
    }

    /**
     * Parses sbt versions from Maven Central metadata, filtering to stable releases in descending order.
     */
    public static List<String> parseSbtVersionsXml(String xml) {
        if (xml == null || xml.isBlank()) return new ArrayList<>();
        Set<String> versions = new LinkedHashSet<>();
        Pattern pattern = Pattern.compile("<version>([^<]+)</version>");
        Matcher matcher = pattern.matcher(xml);
        while (matcher.find()) {
            String v = matcher.group(1).trim();
            if (isStableRelease(v)) {
                versions.add(v);
            }
        }
        List<String> list = new ArrayList<>(versions);
        list.sort((a, b) -> compareVersions(b, a));
        return list;
    }

    /**
     * Parses Scala versions from Maven Central metadata, filtering to stable releases in descending order.
     */
    public static List<String> parseScalaVersionsXml(String xml) {
        if (xml == null || xml.isBlank()) return new ArrayList<>();
        Set<String> versions = new LinkedHashSet<>();
        Pattern pattern = Pattern.compile("<version>([^<]+)</version>");
        Matcher matcher = pattern.matcher(xml);
        while (matcher.find()) {
            String v = matcher.group(1).trim();
            if (isStableRelease(v)) {
                versions.add(v);
            }
        }
        List<String> list = new ArrayList<>(versions);
        list.sort((a, b) -> compareVersions(b, a));
        return list;
    }

    private static boolean isStableRelease(String v) {
        if (v == null || v.isBlank()) return false;
        String lower = v.toLowerCase();
        return !lower.contains("alpha")
                && !lower.contains("beta")
                && !lower.contains("rc")
                && !lower.contains("-m")
                && !lower.contains(".m")
                && !lower.contains("snapshot")
                && !lower.contains("cr");
    }

    /**
     * Fetches sbt releases synchronously with caching.
     */
    public static List<String> fetchSbtVersions(boolean forceRefresh) {
        if (!forceRefresh && cachedSbtVersions != null && !cachedSbtVersions.isEmpty()) {
            return cachedSbtVersions;
        }
        synchronized (SBT_LOCK) {
            if (!forceRefresh && cachedSbtVersions != null && !cachedSbtVersions.isEmpty()) {
                return cachedSbtVersions;
            }
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(SBT_METADATA_URL))
                        .timeout(Duration.ofSeconds(4))
                        .header("Accept", "application/xml")
                        .GET()
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    List<String> parsed = parseSbtVersionsXml(resp.body());
                    if (!parsed.isEmpty()) {
                        cachedSbtVersions = Collections.unmodifiableList(parsed);
                        return cachedSbtVersions;
                    }
                }
            } catch (Exception ignored) {
            }
            cachedSbtVersions = Collections.unmodifiableList(SBT_FALLBACK_VERSIONS);
            return cachedSbtVersions;
        }
    }

    /**
     * Fetches Scala releases synchronously with caching.
     */
    public static List<String> fetchScalaVersions(boolean forceRefresh) {
        if (!forceRefresh && cachedScalaVersions != null && !cachedScalaVersions.isEmpty()) {
            return cachedScalaVersions;
        }
        synchronized (SCALA_LOCK) {
            if (!forceRefresh && cachedScalaVersions != null && !cachedScalaVersions.isEmpty()) {
                return cachedScalaVersions;
            }
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(SCALA3_METADATA_URL))
                        .timeout(Duration.ofSeconds(4))
                        .header("Accept", "application/xml")
                        .GET()
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    List<String> parsed = parseScalaVersionsXml(resp.body());
                    if (!parsed.isEmpty()) {
                        cachedScalaVersions = Collections.unmodifiableList(parsed);
                        return cachedScalaVersions;
                    }
                }
            } catch (Exception ignored) {
            }
            cachedScalaVersions = Collections.unmodifiableList(SCALA_FALLBACK_VERSIONS);
            return cachedScalaVersions;
        }
    }

    /**
     * Asynchronously fetches sbt releases and notifies callback on JavaFX or current thread.
     */
    public static void fetchSbtVersionsAsync(Consumer<List<String>> callback) {
        CompletableFuture.supplyAsync(() -> fetchSbtVersions(false))
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
     * Asynchronously fetches Scala releases and notifies callback on JavaFX or current thread.
     */
    public static void fetchScalaVersionsAsync(Consumer<List<String>> callback) {
        CompletableFuture.supplyAsync(() -> fetchScalaVersions(false))
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
     * Compares two semantic version strings (e.g. "3.9.0" vs "3.8.4", "2.0.9" vs "2.0.8").
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
