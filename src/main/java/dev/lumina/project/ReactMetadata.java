package dev.lumina.project;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Metadata and dynamic package versioning for React, React Native, and Next.js project generators,
 * matching IntelliJ IDEA's CLI runner resolution.
 */
public final class ReactMetadata {

    public static final String TYPE_REACT = "React";
    public static final String TYPE_REACT_NATIVE = "React Native";
    public static final String TYPE_NEXT_JS = "Next.js";

    public static final String ACTION_SELECT = "Select...";

    private static final Map<String, List<String>> FALLBACK_VERSIONS = Map.of(
            TYPE_REACT, List.of("5.1.0", "5.0.1", "5.0.0"),
            TYPE_REACT_NATIVE, List.of("15.1.3", "15.0.0", "14.1.0"),
            TYPE_NEXT_JS, List.of("15.2.0", "15.1.6", "14.2.24")
    );

    private static final Map<String, String> CLI_PACKAGES = Map.of(
            TYPE_REACT, "create-react-app",
            TYPE_REACT_NATIVE, "@react-native-community/cli",
            TYPE_NEXT_JS, "create-next-app"
    );

    private static final Map<String, String> CLI_LABELS = Map.of(
            TYPE_REACT, "create-react-app:",
            TYPE_REACT_NATIVE, "react-native:",
            TYPE_NEXT_JS, "create-next-app:"
    );

    private static final Map<String, String> CACHED_LATEST = new ConcurrentHashMap<>();

    private ReactMetadata() {}

    public static String getCliLabel(String projectType) {
        return CLI_LABELS.getOrDefault(projectType, "create-react-app:");
    }

    public static String getCliPackage(String projectType) {
        return CLI_PACKAGES.getOrDefault(projectType, "create-react-app");
    }

    public static String formatCliDisplay(String projectType, String version) {
        String pkg = getCliPackage(projectType);
        String prefix = "npx " + pkg;
        int totalLen = 60;
        int padding = Math.max(2, totalLen - prefix.length() - version.length());
        return prefix + " ".repeat(padding) + version;
    }

    public static List<String> getVersions(String projectType) {
        List<String> list = new ArrayList<>();
        String latest = CACHED_LATEST.get(projectType);
        if (latest != null && !latest.isBlank()) {
            list.add(latest);
        }
        List<String> defaults = FALLBACK_VERSIONS.getOrDefault(projectType, List.of("5.1.0"));
        for (String v : defaults) {
            if (!list.contains(v)) {
                list.add(v);
            }
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * Attempts to fetch the latest published version of the generator CLI from the npm registry.
     */
    public static String fetchLatestVersion(String projectType, boolean force) {
        if (!force && CACHED_LATEST.containsKey(projectType)) {
            return CACHED_LATEST.get(projectType);
        }

        String pkg = getCliPackage(projectType);
        String registryUrl = "https://registry.npmjs.org/" + pkg + "/latest";

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(2500))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(registryUrl))
                    .header("Accept", "application/json")
                    .timeout(Duration.ofMillis(2500))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String body = response.body();
                Matcher m = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
                if (m.find()) {
                    String ver = m.group(1).trim();
                    CACHED_LATEST.put(projectType, ver);
                    return ver;
                }
            }
        } catch (Exception ignored) {}

        // Fallback to default
        String fallback = FALLBACK_VERSIONS.getOrDefault(projectType, List.of("5.1.0")).getFirst();
        CACHED_LATEST.put(projectType, fallback);
        return fallback;
    }
}
