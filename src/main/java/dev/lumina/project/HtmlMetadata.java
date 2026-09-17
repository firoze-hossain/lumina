package dev.lumina.project;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Metadata provider and release version fetcher for HTML5 Boilerplate and Bootstrap.
 * Matches IntelliJ IDEA's HTML generator behavior:
 * - Dynamic remote version fetching from GitHub releases / npm registry
 * - Built-in fallback version lists matching IntelliJ's releases
 * - Thread-safe in-memory caching
 */
public final class HtmlMetadata {

    public static final String TYPE_H5BP = "HTML5 Boilerplate";
    public static final String TYPE_BOOTSTRAP = "Bootstrap";

    public static final List<String> FALLBACK_H5BP_VERSIONS = List.of(
            "v9.0.1",
            "v9.0.0",
            "v8.0.0",
            "v8.0.0-RC2",
            "v8.0.0-RC1",
            "v7.3.0",
            "v7.2.0",
            "v7.1.0",
            "v7.0.1",
            "v7.0.0"
    );

    public static final List<String> FALLBACK_BOOTSTRAP_VERSIONS = List.of(
            "v5.3.8",
            "v5.3.7",
            "v5.3.6",
            "v5.3.5",
            "v5.3.4",
            "v5.3.3",
            "v5.3.2",
            "v5.3.1",
            "v5.3.0",
            "v5.2.3",
            "v5.1.3",
            "v5.0.2",
            "v4.6.2"
    );

    private static volatile List<String> cachedH5bpVersions = null;
    private static volatile List<String> cachedBootstrapVersions = null;

    private HtmlMetadata() {}

    public static List<String> getH5bpVersions() {
        List<String> list = cachedH5bpVersions;
        return (list != null && !list.isEmpty()) ? list : FALLBACK_H5BP_VERSIONS;
    }

    public static List<String> getBootstrapVersions() {
        List<String> list = cachedBootstrapVersions;
        return (list != null && !list.isEmpty()) ? list : FALLBACK_BOOTSTRAP_VERSIONS;
    }

    /**
     * Fetches the latest versions for the specified project type ("HTML5 Boilerplate" or "Bootstrap").
     * Tries GitHub API first, then falls back to npm registry, and finally fallback defaults.
     */
    public static List<String> fetchVersions(String projectType, boolean forceRefresh) {
        boolean isBootstrap = projectType != null && projectType.equalsIgnoreCase(TYPE_BOOTSTRAP);

        if (!forceRefresh) {
            if (isBootstrap && cachedBootstrapVersions != null && !cachedBootstrapVersions.isEmpty()) {
                return cachedBootstrapVersions;
            }
            if (!isBootstrap && cachedH5bpVersions != null && !cachedH5bpVersions.isEmpty()) {
                return cachedH5bpVersions;
            }
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        List<String> fetched = isBootstrap ? fetchBootstrapRemote(client) : fetchH5bpRemote(client);

        if (fetched != null && !fetched.isEmpty()) {
            if (isBootstrap) {
                cachedBootstrapVersions = Collections.unmodifiableList(fetched);
            } else {
                cachedH5bpVersions = Collections.unmodifiableList(fetched);
            }
            return fetched;
        }

        return isBootstrap ? FALLBACK_BOOTSTRAP_VERSIONS : FALLBACK_H5BP_VERSIONS;
    }

    private static List<String> fetchH5bpRemote(HttpClient client) {
        // Attempt 1: GitHub releases API
        try {
            List<String> fromGh = fetchFromGitHub(client, "h5bp/html5-boilerplate");
            if (!fromGh.isEmpty()) return fromGh;
        } catch (Exception ignored) {}

        // Attempt 2: npm registry
        try {
            List<String> fromNpm = fetchFromNpm(client, "html5-boilerplate");
            if (!fromNpm.isEmpty()) return fromNpm;
        } catch (Exception ignored) {}

        return Collections.emptyList();
    }

    private static List<String> fetchBootstrapRemote(HttpClient client) {
        // Attempt 1: GitHub releases API
        try {
            List<String> fromGh = fetchFromGitHub(client, "twbs/bootstrap");
            if (!fromGh.isEmpty()) return fromGh;
        } catch (Exception ignored) {}

        // Attempt 2: npm registry
        try {
            List<String> fromNpm = fetchFromNpm(client, "bootstrap");
            if (!fromNpm.isEmpty()) return fromNpm;
        } catch (Exception ignored) {}

        return Collections.emptyList();
    }

    private static List<String> fetchFromGitHub(HttpClient client, String repo) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.github.com/repos/" + repo + "/releases?per_page=30"))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Lumina-IDE")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200 && response.body() != null) {
            JsonElement root = JsonParser.parseString(response.body());
            if (root.isJsonArray()) {
                Set<String> versions = new LinkedHashSet<>();
                for (JsonElement el : root.getAsJsonArray()) {
                    if (el.isJsonObject()) {
                        JsonObject obj = el.getAsJsonObject();
                        if (obj.has("tag_name")) {
                            String tag = obj.get("tag_name").getAsString().trim();
                            if (!tag.isEmpty()) {
                                if (!tag.startsWith("v") && Character.isDigit(tag.charAt(0))) {
                                    tag = "v" + tag;
                                }
                                versions.add(tag);
                            }
                        }
                    }
                }
                if (!versions.isEmpty()) {
                    return new ArrayList<>(versions);
                }
            }
        }
        return Collections.emptyList();
    }

    private static List<String> fetchFromNpm(HttpClient client, String pkgName) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://registry.npmjs.org/" + pkgName))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/json")
                .header("User-Agent", "Lumina-IDE")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200 && response.body() != null) {
            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            if (root.has("versions") && root.get("versions").isJsonObject()) {
                JsonObject versionsObj = root.getAsJsonObject("versions");
                List<String> rawKeys = new ArrayList<>(versionsObj.keySet());
                Collections.reverse(rawKeys); // latest releases are at end of keys
                List<String> list = new ArrayList<>();
                for (String k : rawKeys) {
                    String v = k.startsWith("v") ? k : "v" + k;
                    list.add(v);
                    if (list.size() >= 25) break;
                }
                return list;
            }
        }
        return Collections.emptyList();
    }
}
