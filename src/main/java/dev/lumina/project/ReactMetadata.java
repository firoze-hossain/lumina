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
            TYPE_REACT_NATIVE, List.of("20.2.0", "20.1.1", "20.0.0", "19.1.2", "19.0.0", "18.0.0", "17.0.0", "16.0.0", "15.1.3", "15.0.0", "14.1.0"),
            TYPE_NEXT_JS, List.of("15.2.0", "15.1.6", "14.2.24")
    );

    private static final Map<String, String> CLI_PACKAGES = Map.of(
            TYPE_REACT, "create-react-app",
            TYPE_REACT_NATIVE, "@react-native-community/cli",
            TYPE_NEXT_JS, "create-next-app"
    );

    private static final Map<String, String> CLI_LABELS = Map.of(
            TYPE_REACT, "create-react-app:",
            TYPE_REACT_NATIVE, "React Native:",
            TYPE_NEXT_JS, "create-next-app:"
    );

    private static final Map<String, String> CACHED_LATEST = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> CACHED_ALL_VERSIONS = new ConcurrentHashMap<>();

    private ReactMetadata() {}

    public static String getCliLabel(String projectType) {
        return CLI_LABELS.getOrDefault(projectType, "create-react-app:");
    }

    public static String getCliPackage(String projectType) {
        return CLI_PACKAGES.getOrDefault(projectType, "create-react-app");
    }

    public static String formatCliDisplay(String projectType, String version) {
        String prefix;
        if (TYPE_REACT_NATIVE.equalsIgnoreCase(projectType)) {
            prefix = "npx --package @react-native-community/cli rnc-cli";
        } else {
            prefix = "npx " + getCliPackage(projectType);
        }
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

    /**
     * Fetches all available versions for the generator package from npm registry.
     */
    public static List<String> fetchAllVersions(String projectType, boolean force) {
        if (!force && CACHED_ALL_VERSIONS.containsKey(projectType)) {
            return CACHED_ALL_VERSIONS.get(projectType);
        }

        String pkg = getCliPackage(projectType);
        String registryUrl = "https://registry.npmjs.org/" + pkg;

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(3000))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(registryUrl))
                    .header("Accept", "application/json")
                    .timeout(Duration.ofMillis(4000))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String body = response.body();
                List<String> parsed = parseNpmVersions(body);
                if (!parsed.isEmpty()) {
                    CACHED_ALL_VERSIONS.put(projectType, parsed);
                    return parsed;
                }
            }
        } catch (Exception ignored) {}

        List<String> fallbacks = FALLBACK_VERSIONS.getOrDefault(projectType, List.of("20.2.0"));
        CACHED_ALL_VERSIONS.put(projectType, fallbacks);
        return fallbacks;
    }

    /**
     * Extracts version list from npm registry JSON metadata.
     */
    public static List<String> parseNpmVersions(String json) {
        List<String> result = new ArrayList<>();
        Pattern p = Pattern.compile("\"([0-9]+\\.[0-9]+\\.[0-9]+(?:-[a-zA-Z0-9.]+)?)\"\\s*:\\s*\\{");
        Matcher m = p.matcher(json);
        while (m.find()) {
            String v = m.group(1);
            if (!result.contains(v) && !v.contains("alpha") && !v.contains("beta") && !v.contains("canary")) {
                result.add(v);
            }
        }
        if (result.isEmpty()) {
            Pattern p2 = Pattern.compile("\"([0-9]+\\.[0-9]+\\.[0-9]+)\"\\s*:\\s*\"[0-9]{4}-");
            Matcher m2 = p2.matcher(json);
            while (m2.find()) {
                String v = m2.group(1);
                if (!result.contains(v)) {
                    result.add(v);
                }
            }
        }
        Collections.reverse(result);
        return result;
    }

    public static void clearCache() {
        CACHED_LATEST.clear();
        CACHED_ALL_VERSIONS.clear();
    }
}

