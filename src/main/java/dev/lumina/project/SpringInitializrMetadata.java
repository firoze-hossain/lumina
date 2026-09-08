package dev.lumina.project;

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
import java.util.List;

/**
 * Fetches the live project catalog from start.spring.io/metadata/client \u2014
 * the same endpoint and the same JSON format IntelliJ's Spring Initializr
 * wizard queries. Current Spring Boot versions and the full, current
 * dependency list (with real descriptions) come from here, not from a
 * baked-in list, so the New Project wizard stays correct as Spring ships
 * new releases and starters.
 *
 * <p>Network access and JSON parsing are separated (fetch() vs. parse())
 * so the parsing logic can be exercised with a saved JSON sample without
 * a live connection.
 */
public final class SpringInitializrMetadata {

    public record Dependency(String id, String name, String description) {
    }

    public record Category(String name, List<Dependency> dependencies) {
    }

    public record Metadata(List<String> bootVersions, String defaultBootVersion,
                           List<Category> categories) {
    }

    private static final String METADATA_URL = "https://start.spring.io/metadata/client";
    private static final String ACCEPT = "application/vnd.initializr.v2.2+json";

    private SpringInitializrMetadata() {
    }

    /**
     * Blocking network call \u2014 always run this off the FX thread. Throws on
     * any failure (timeout, non-200, malformed body) so the caller can fall
     * back to the bundled offline catalog, per Lumina's fallback-first rule.
     */
    public static Metadata fetch() throws IOException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(METADATA_URL))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", ACCEPT)
                .header("User-Agent", "Lumina-IDE")
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Metadata request interrupted", e);
        }
        if (response.statusCode() != 200) {
            throw new IOException("start.spring.io/metadata/client returned HTTP "
                    + response.statusCode());
        }
        return parse(response.body());
    }

    /** Pure parsing: no network, safe to unit-test with a saved JSON body. */
    public static Metadata parse(String json) throws IOException {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            List<String> versions = new ArrayList<>();
            String defaultVersion = "";
            if (root.has("bootVersion")) {
                JsonObject bootVersion = root.getAsJsonObject("bootVersion");
                if (bootVersion.has("default")) {
                    defaultVersion = bootVersion.get("default").getAsString();
                }
                if (bootVersion.has("values")) {
                    for (JsonElement el : bootVersion.getAsJsonArray("values")) {
                        JsonObject v = el.getAsJsonObject();
                        if (v.has("id")) versions.add(v.get("id").getAsString());
                    }
                }
            }
            if (versions.isEmpty()) {
                throw new IOException("metadata had no boot versions");
            }

            List<Category> categories = new ArrayList<>();
            if (root.has("dependencies")) {
                JsonObject dependencies = root.getAsJsonObject("dependencies");
                if (dependencies.has("values")) {
                    for (JsonElement catEl : dependencies.getAsJsonArray("values")) {
                        JsonObject cat = catEl.getAsJsonObject();
                        String categoryName = cat.has("name")
                                ? cat.get("name").getAsString() : "Other";
                        List<Dependency> deps = new ArrayList<>();
                        if (cat.has("values")) {
                            for (JsonElement depEl : cat.getAsJsonArray("values")) {
                                Dependency dep = readDependency(depEl.getAsJsonObject());
                                if (dep != null) deps.add(dep);
                            }
                        }
                        if (!deps.isEmpty()) {
                            categories.add(new Category(categoryName, deps));
                        }
                    }
                }
            }
            if (categories.isEmpty()) {
                throw new IOException("metadata had no dependency categories");
            }

            return new Metadata(versions, defaultVersion, categories);
        } catch (IllegalStateException | ClassCastException | NullPointerException e) {
            throw new IOException("Unexpected metadata format: " + e.getMessage(), e);
        }
    }

    private static Dependency readDependency(JsonObject obj) {
        if (!obj.has("id") || !obj.has("name")) return null;
        String id = obj.get("id").getAsString();
        String name = obj.get("name").getAsString();
        String description = obj.has("description") && !obj.get("description").isJsonNull()
                ? obj.get("description").getAsString() : "";
        return new Dependency(id, name, description);
    }
}
