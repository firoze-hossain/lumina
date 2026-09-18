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
 * Service providing dynamic Play Framework version resolution via Maven Central metadata,
 * matching IntelliJ IDEA.
 */
public final class PlayMetadata {

    public static final String PLAY3_METADATA_URL =
            "https://repo1.maven.org/maven2/org/playframework/play_3/maven-metadata.xml";

    public static final String PLAY2_METADATA_URL =
            "https://repo1.maven.org/maven2/com/typesafe/play/play_2.13/maven-metadata.xml";

    public static final String DEFAULT_PLAY_VERSION = "3.0.11";

    /**
     * Fallback Play versions matching IntelliJ IDEA (Image 4: 3.0.11, 3.0.10, 3.0.9, 3.0.8, ...).
     */
    public static final List<String> PLAY_FALLBACK_VERSIONS = List.of(
            "3.0.11", "3.0.10", "3.0.9", "3.0.8", "3.0.7", "3.0.6", "3.0.5", "3.0.4",
            "3.0.3", "3.0.2", "3.0.1", "3.0.0", "2.9.11", "2.8.22"
    );

    private static volatile List<String> cachedPlayVersions = null;
    private static final Object PLAY_LOCK = new Object();

    private PlayMetadata() {
    }

    /**
     * Parses Play versions from Maven Central metadata, filtering to stable releases in descending order.
     */
    public static List<String> parsePlayVersionsXml(String xml) {
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
     * Fetches Play releases synchronously with caching.
     */
    public static List<String> fetchPlayVersions(boolean forceRefresh) {
        if (!forceRefresh && cachedPlayVersions != null && !cachedPlayVersions.isEmpty()) {
            return cachedPlayVersions;
        }
        synchronized (PLAY_LOCK) {
            if (!forceRefresh && cachedPlayVersions != null && !cachedPlayVersions.isEmpty()) {
                return cachedPlayVersions;
            }
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();

                HttpRequest req3 = HttpRequest.newBuilder()
                        .uri(URI.create(PLAY3_METADATA_URL))
                        .timeout(Duration.ofSeconds(4))
                        .header("Accept", "application/xml")
                        .GET()
                        .build();

                HttpResponse<String> resp3 = client.send(req3, HttpResponse.BodyHandlers.ofString());
                Set<String> allVersions = new LinkedHashSet<>();
                if (resp3.statusCode() == 200) {
                    allVersions.addAll(parsePlayVersionsXml(resp3.body()));
                }

                // Also fetch Play 2.13 metadata for LTS options
                try {
                    HttpRequest req2 = HttpRequest.newBuilder()
                            .uri(URI.create(PLAY2_METADATA_URL))
                            .timeout(Duration.ofSeconds(3))
                            .header("Accept", "application/xml")
                            .GET()
                            .build();
                    HttpResponse<String> resp2 = client.send(req2, HttpResponse.BodyHandlers.ofString());
                    if (resp2.statusCode() == 200) {
                        for (String v : parsePlayVersionsXml(resp2.body())) {
                            if (v.startsWith("2.9.") || v.startsWith("2.8.")) {
                                allVersions.add(v);
                            }
                        }
                    }
                } catch (Exception ignored) {
                }

                if (!allVersions.isEmpty()) {
                    List<String> list = new ArrayList<>(allVersions);
                    list.sort((a, b) -> compareVersions(b, a));
                    cachedPlayVersions = Collections.unmodifiableList(list);
                    return cachedPlayVersions;
                }
            } catch (Exception ignored) {
            }
            cachedPlayVersions = Collections.unmodifiableList(PLAY_FALLBACK_VERSIONS);
            return cachedPlayVersions;
        }
    }

    /**
     * Asynchronously fetches Play releases and notifies callback on JavaFX or current thread.
     */
    public static void fetchPlayVersionsAsync(Consumer<List<String>> callback) {
        CompletableFuture.supplyAsync(() -> fetchPlayVersions(false))
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
     * Compares two semantic version strings (e.g. "3.0.11" vs "3.0.10").
     */
    public static int compareVersions(String v1, String v2) {
        return ScalaMetadata.compareVersions(v1, v2);
    }
}
