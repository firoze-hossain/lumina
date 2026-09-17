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
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Metadata and dynamic discovery for Express project generator,
 * matching IntelliJ IDEA's express-generator integration, View Engine options,
 * Stylesheet Engine options, and package versioning.
 */
public final class ExpressMetadata {

    public static final String TYPE_EXPRESS = "Express";
    public static final String PACKAGE_NAME = "express-generator";
    public static final String DEFAULT_VERSION = "4.16.1";
    public static final String ACTION_SELECT = "Select...";

    public record ViewEngine(
            String name,
            String cliOption,
            String fileExt,
            String npmPackage
    ) {
        @Override
        public String toString() {
            return name;
        }
    }

    public record StylesheetEngine(
            String name,
            String cliOption,
            String fileExt,
            String npmPackage
    ) {
        @Override
        public String toString() {
            return name;
        }
    }

    // Official express-generator view engines matching IntelliJ IDEA (media_1789618251443.png)
    private static final List<ViewEngine> VIEW_ENGINES = List.of(
            new ViewEngine("Dust", "dust", "dust", "dustjs-linkedin"),
            new ViewEngine("EJS", "ejs", "ejs", "ejs"),
            new ViewEngine("Handlebars", "hbs", "hbs", "hbs"),
            new ViewEngine("Hogan.js", "hjs", "hjs", "hjs"),
            new ViewEngine("Pug (Jade)", "pug", "pug", "pug"),
            new ViewEngine("Twig", "twig", "twig", "twig"),
            new ViewEngine("Vash", "vash", "vash", "vash"),
            new ViewEngine("None", "no-view", "html", "")
    );

    // Official express-generator stylesheet engines matching IntelliJ IDEA (media_1789618251437.png)
    private static final List<StylesheetEngine> STYLESHEET_ENGINES = List.of(
            new StylesheetEngine("Plain CSS", "css", "css", ""),
            new StylesheetEngine("Stylus", "stylus", "styl", "stylus"),
            new StylesheetEngine("LESS", "less", "less", "less-middleware"),
            new StylesheetEngine("Compass", "compass", "scss", "node-compass"),
            new StylesheetEngine("SASS", "sass", "sass", "node-sass-middleware")
    );

    private static final List<String> FALLBACK_VERSIONS = List.of(
            "4.16.1", "4.16.0", "4.15.5", "4.15.0", "4.14.1", "4.13.4", "4.12.4"
    );

    private static volatile String cachedLatestVersion = null;
    private static volatile List<String> cachedAllVersions = null;

    private ExpressMetadata() {}

    public static List<ViewEngine> getViewEngines() {
        return VIEW_ENGINES;
    }

    public static List<StylesheetEngine> getStylesheetEngines() {
        return STYLESHEET_ENGINES;
    }

    public static ViewEngine getDefaultViewEngine() {
        return VIEW_ENGINES.stream()
                .filter(v -> "Pug (Jade)".equalsIgnoreCase(v.name()))
                .findFirst()
                .orElse(VIEW_ENGINES.getFirst());
    }

    public static StylesheetEngine getDefaultStylesheetEngine() {
        return STYLESHEET_ENGINES.stream()
                .filter(s -> "Plain CSS".equalsIgnoreCase(s.name()))
                .findFirst()
                .orElse(STYLESHEET_ENGINES.getFirst());
    }

    public static ViewEngine findViewEngine(String name) {
        if (name == null || name.isBlank()) return getDefaultViewEngine();
        return VIEW_ENGINES.stream()
                .filter(v -> v.name().equalsIgnoreCase(name.trim()))
                .findFirst()
                .orElse(getDefaultViewEngine());
    }

    public static StylesheetEngine findStylesheetEngine(String name) {
        if (name == null || name.isBlank()) return getDefaultStylesheetEngine();
        return STYLESHEET_ENGINES.stream()
                .filter(s -> s.name().equalsIgnoreCase(name.trim()))
                .findFirst()
                .orElse(getDefaultStylesheetEngine());
    }

    /**
     * Formats the express-generator command line row matching IntelliJ IDEA:
     * npx --package express-generator express            4.16.1
     */
    public static String formatCliDisplay(String version) {
        String prefix = "npx --package express-generator express";
        int totalLen = 60;
        int padding = Math.max(2, totalLen - prefix.length() - version.length());
        return prefix + " ".repeat(padding) + version;
    }

    public static String fetchLatestVersion(boolean force) {
        if (!force && cachedLatestVersion != null) {
            return cachedLatestVersion;
        }

        String registryUrl = "https://registry.npmjs.org/" + PACKAGE_NAME + "/latest";
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
                    cachedLatestVersion = m.group(1).trim();
                    return cachedLatestVersion;
                }
            }
        } catch (Exception ignored) {}

        cachedLatestVersion = DEFAULT_VERSION;
        return cachedLatestVersion;
    }

    public static List<String> fetchAllVersions(boolean force) {
        if (!force && cachedAllVersions != null) {
            return cachedAllVersions;
        }

        String registryUrl = "https://registry.npmjs.org/" + PACKAGE_NAME;
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
                List<String> parsed = ReactMetadata.parseNpmVersions(body);
                if (!parsed.isEmpty()) {
                    cachedAllVersions = List.copyOf(parsed);
                    return cachedAllVersions;
                }
            }
        } catch (Exception ignored) {}

        cachedAllVersions = FALLBACK_VERSIONS;
        return cachedAllVersions;
    }

    /**
     * Attempts dynamic inspection of installed express CLI help to discover supported flags.
     */
    public static List<String> probeCliHelp(String nodePath) {
        List<String> detectedEngines = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder("npx", "--package", PACKAGE_NAME, "express", "--help");
            Process p = pb.start();
            boolean done = p.waitFor(2000, TimeUnit.MILLISECONDS);
            if (done && p.exitValue() == 0) {
                String out = new String(p.getInputStream().readAllBytes());
                Matcher m = Pattern.compile("--view\\s+<engine>.*?\\(([^)]+)\\)").matcher(out);
                if (m.find()) {
                    String[] engines = m.group(1).split("\\|");
                    for (String eng : engines) {
                        detectedEngines.add(eng.trim());
                    }
                }
            }
        } catch (Exception ignored) {}
        return detectedEngines;
    }

    public static void clearCache() {
        cachedLatestVersion = null;
        cachedAllVersions = null;
    }
}
