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
 * Service providing dynamic Groovy SDK releases resolution via Maven Central metadata
 * matching IntelliJ IDEA.
 */
public final class GroovyMetadata {

    public static final String APACHE_GROOVY_METADATA_URL =
            "https://repo1.maven.org/maven2/org/apache/groovy/groovy/maven-metadata.xml";

    public static final String CODEHAUS_GROOVY_METADATA_URL =
            "https://repo1.maven.org/maven2/org/codehaus/groovy/groovy/maven-metadata.xml";

    public static final String DEFAULT_GROOVY_VERSION = "5.1.1";

    /**
     * Fallback versions representing latest releases per branch, matching IntelliJ IDEA:
     * 5.1.1, 5.0.8, 4.0.33, 3.0.25, 2.5.23, 2.4.21, 6.0.0-beta-3
     */
    public static final List<String> FALLBACK_VERSIONS = List.of(
            "5.1.1", "5.0.8", "4.0.33", "3.0.25", "2.5.23", "2.4.21", "6.0.0-beta-3"
    );

    private static volatile List<String> cachedVersions = null;
    private static final Object LOCK = new Object();

    private GroovyMetadata() {
    }

    /**
     * Returns the appropriate Maven groupId for a given Groovy version.
     * Groovy 2.x uses org.codehaus.groovy, Groovy 3.x+ uses org.apache.groovy.
     */
    public static String getGroovyGroupId(String version) {
        if (version != null && version.trim().startsWith("2.")) {
            return "org.codehaus.groovy";
        }
        return "org.apache.groovy";
    }

    /**
     * Parses Maven Central maven-metadata.xml and groups versions by branch,
     * picking the latest release per branch (e.g. 5.1.1, 5.0.8, 4.0.33, 3.0.25).
     */
    public static List<String> parseVersionsXml(String xml) {
        List<String> raw = new ArrayList<>();
        if (xml == null || xml.isBlank()) return raw;

        Pattern pattern = Pattern.compile("<version>([^<]+)</version>");
        Matcher matcher = pattern.matcher(xml);
        while (matcher.find()) {
            String v = matcher.group(1).trim();
            if (isValidVersion(v)) {
                raw.add(v);
            }
        }
        return selectLatestPerBranch(raw);
    }

    private static boolean isValidVersion(String v) {
        if (v == null || v.isBlank()) return false;
        String lower = v.toLowerCase();
        if (lower.contains("alpha") || lower.contains("snapshot") || lower.contains("cr") || lower.contains("m")) {
            return false;
        }
        return true;
    }

    /**
     * Groups versions into major.minor branches and retains the latest version per branch.
     * Stable branches are sorted descending, followed by preview/beta branches.
     */
    public static List<String> selectLatestPerBranch(Collection<String> versions) {
        Map<String, String> latestPerBranch = new LinkedHashMap<>();
        List<String> previews = new ArrayList<>();

        for (String v : versions) {
            String lower = v.toLowerCase();
            boolean isBeta = lower.contains("beta") || lower.contains("rc");
            if (isBeta) {
                previews.add(v);
                continue;
            }
            Matcher m = Pattern.compile("^(\\d+\\.\\d+)").matcher(v);
            if (m.find()) {
                String branch = m.group(1);
                String currentBest = latestPerBranch.get(branch);
                if (currentBest == null || compareVersions(v, currentBest) > 0) {
                    latestPerBranch.put(branch, v);
                }
            }
        }

        // Sort stable branches descending
        List<String> stable = new ArrayList<>(latestPerBranch.values());
        stable.sort((a, b) -> compareVersions(b, a));

        // Sort previews descending and pick newest
        previews.sort((a, b) -> compareVersions(b, a));
        if (!previews.isEmpty()) {
            stable.add(previews.get(0));
        }

        return stable;
    }

    /**
     * Synchronously fetches Groovy releases (with caching and fallback).
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

                Set<String> allVersions = new LinkedHashSet<>();

                // 1. Apache Groovy (3.x, 4.x, 5.x, 6.x)
                try {
                    HttpRequest req1 = HttpRequest.newBuilder()
                            .uri(URI.create(APACHE_GROOVY_METADATA_URL))
                            .timeout(Duration.ofSeconds(4))
                            .header("Accept", "application/xml")
                            .GET()
                            .build();
                    HttpResponse<String> resp1 = client.send(req1, HttpResponse.BodyHandlers.ofString());
                    if (resp1.statusCode() == 200) {
                        Pattern p = Pattern.compile("<version>([^<]+)</version>");
                        Matcher m = p.matcher(resp1.body());
                        while (m.find()) {
                            String v = m.group(1).trim();
                            if (isValidVersion(v)) allVersions.add(v);
                        }
                    }
                } catch (Exception ignored) {
                }

                // 2. Codehaus Groovy (2.4.x, 2.5.x)
                try {
                    HttpRequest req2 = HttpRequest.newBuilder()
                            .uri(URI.create(CODEHAUS_GROOVY_METADATA_URL))
                            .timeout(Duration.ofSeconds(4))
                            .header("Accept", "application/xml")
                            .GET()
                            .build();
                    HttpResponse<String> resp2 = client.send(req2, HttpResponse.BodyHandlers.ofString());
                    if (resp2.statusCode() == 200) {
                        Pattern p = Pattern.compile("<version>([^<]+)</version>");
                        Matcher m = p.matcher(resp2.body());
                        while (m.find()) {
                            String v = m.group(1).trim();
                            if (isValidVersion(v)) allVersions.add(v);
                        }
                    }
                } catch (Exception ignored) {
                }

                if (!allVersions.isEmpty()) {
                    List<String> selected = selectLatestPerBranch(allVersions);
                    if (!selected.isEmpty()) {
                        cachedVersions = Collections.unmodifiableList(selected);
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
     * Asynchronously fetches versions and notifies callback on JavaFX or current thread.
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
     * Returns the latest stable Groovy version.
     */
    public static String getLatestVersion() {
        List<String> versions = fetchVersions(false);
        if (!versions.isEmpty()) {
            return versions.getFirst();
        }
        return DEFAULT_GROOVY_VERSION;
    }

    /**
     * Compares two semantic version strings (e.g. "5.1.1" vs "5.0.8").
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
