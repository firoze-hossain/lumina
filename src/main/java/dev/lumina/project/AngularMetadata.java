package dev.lumina.project;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Metadata and dynamic discovery for Angular CLI project generator,
 * matching IntelliJ IDEA's Angular CLI integration, version resolution from
 * npm registry, and local/global CLI executable detection.
 */
public final class AngularMetadata {

    public static final String TYPE_ANGULAR = "Angular";
    public static final String PACKAGE_NAME = "@angular/cli";
    public static final String DEFAULT_VERSION = "22.1.8";
    public static final String CLI_PREFIX = "npx --package @angular/cli ng";
    public static final String ACTION_SELECT = "Select...";

    public static final List<String> FALLBACK_VERSIONS = List.of(
            "22.1.8", "20.1.0", "19.2.0", "19.1.0", "19.0.0",
            "18.2.0", "18.1.0", "18.0.0", "17.3.0", "16.2.0"
    );

    public record AngularCliEntry(
            String runnerPrefix,
            String path,
            String version,
            boolean isAction
    ) {
        public String formatDisplay() {
            if (isAction) {
                return runnerPrefix;
            }
            String prefix = (path != null && !path.isBlank()) ? runnerPrefix + "  " + path : runnerPrefix;
            int totalLen = 60;
            int padding = Math.max(2, totalLen - prefix.length() - (version != null ? version.length() : 0));
            return prefix + " ".repeat(padding) + (version != null ? version : "");
        }

        @Override
        public String toString() {
            return formatDisplay();
        }
    }

    private static volatile String cachedLatestVersion = null;
    private static volatile List<String> cachedAllVersions = null;

    private AngularMetadata() {}

    /**
     * Formats an npx Angular CLI runner string matching IntelliJ IDEA.
     * e.g. "npx --package @angular/cli ng                         22.1.8"
     */
    public static String formatCliDisplay(String version) {
        String ver = (version == null || version.isBlank()) ? DEFAULT_VERSION : version.trim();
        int totalLen = 60;
        int padding = Math.max(2, totalLen - CLI_PREFIX.length() - ver.length());
        return CLI_PREFIX + " ".repeat(padding) + ver;
    }

    /**
     * Extracts version string from a formatted display or raw version.
     */
    public static String parseVersionFromDisplay(String display) {
        if (display == null || display.isBlank() || ACTION_SELECT.equals(display)) {
            return DEFAULT_VERSION;
        }
        String trimmed = display.trim();
        String[] parts = trimmed.split("\\s+");
        if (parts.length > 0) {
            String last = parts[parts.length - 1];
            if (last.matches("\\d+(\\.\\d+)*.*")) {
                return last;
            }
        }
        return DEFAULT_VERSION;
    }

    /**
     * Discovers installed or available Angular CLI entries on the system.
     * Matches IntelliJ IDEA Image 1 & 3:
     * 1) npx --package @angular/cli ng     <version>
     * 2) ng <path>                         <version> (if globally installed)
     * 3) Select...
     */
    public static List<AngularCliEntry> detectAngularClis() {
        List<AngularCliEntry> entries = new ArrayList<>();
        String latestVer = cachedLatestVersion != null ? cachedLatestVersion : DEFAULT_VERSION;

        // 1. Default npx runner entry
        entries.add(new AngularCliEntry(CLI_PREFIX, null, latestVer, false));

        // 2. Discover system-installed global ng binary if available
        Set<String> ngPaths = discoverCandidateNgPaths();
        for (String p : ngPaths) {
            String ver = probeNgVersion(p);
            entries.add(new AngularCliEntry("ng", p, ver != null ? ver : latestVer, false));
        }

        // 3. Action item: Select...
        entries.add(new AngularCliEntry(ACTION_SELECT, null, "", true));

        return entries;
    }

    /**
     * Discovers potential 'ng' binary paths on the current operating system.
     */
    private static Set<String> discoverCandidateNgPaths() {
        Set<String> paths = new LinkedHashSet<>();
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        String cmd = isWindows ? "where" : "which";
        try {
            Process p = new ProcessBuilder(cmd, "ng").redirectErrorStream(true).start();
            boolean done = p.waitFor(1500, TimeUnit.MILLISECONDS);
            if (done && p.exitValue() == 0) {
                String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                String firstLine = out.lines().findFirst().orElse("").trim();
                if (!firstLine.isBlank() && Files.isExecutable(Path.of(firstLine))) {
                    paths.add(firstLine);
                }
            }
        } catch (Exception ignored) {}

        String userHome = System.getProperty("user.home", "");
        if (!userHome.isBlank()) {
            checkAdd(paths, userHome + "/.nvm/current/bin/ng");
            checkAdd(paths, userHome + "/.npm-global/bin/ng");
        }
        checkAdd(paths, "/usr/bin/ng");
        checkAdd(paths, "/usr/local/bin/ng");
        return paths;
    }

    private static void checkAdd(Set<String> set, String pathStr) {
        if (pathStr == null || pathStr.isBlank()) return;
        try {
            Path p = Path.of(pathStr);
            if (Files.isRegularFile(p) && Files.isExecutable(p)) {
                set.add(pathStr);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Probes the version of a local 'ng' binary.
     */
    public static String probeNgVersion(String ngPath) {
        if (ngPath == null || ngPath.isBlank()) return null;
        try {
            Process p = new ProcessBuilder(ngPath, "version").redirectErrorStream(true).start();
            boolean done = p.waitFor(2500, TimeUnit.MILLISECONDS);
            if (done && p.exitValue() == 0) {
                String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                // Search for "Angular CLI: 19.2.0" or "Angular CLI: 22.1.8"
                Matcher m = Pattern.compile("Angular CLI:\\s*([0-9]+\\.[0-9]+\\.[0-9]+[\\w.-]*)", Pattern.CASE_INSENSITIVE).matcher(out);
                if (m.find()) {
                    return m.group(1).trim();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Attempts to fetch the latest published version of @angular/cli from the npm registry.
     */
    public static String fetchLatestVersion(boolean force) {
        if (!force && cachedLatestVersion != null) {
            return cachedLatestVersion;
        }

        String registryUrl = "https://registry.npmjs.org/@angular%2Fcli/latest";
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(2500))
                    .build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(registryUrl))
                    .timeout(Duration.ofMillis(3000))
                    .header("Accept", "application/json")
                    .header("User-Agent", "Lumina-IDE")
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200 && resp.body() != null && !resp.body().isBlank()) {
                Matcher m = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"").matcher(resp.body());
                if (m.find()) {
                    String ver = m.group(1).trim();
                    cachedLatestVersion = ver;
                    return ver;
                }
            }
        } catch (Exception ignored) {}

        return cachedLatestVersion != null ? cachedLatestVersion : DEFAULT_VERSION;
    }

    /**
     * Fetches all available versions of @angular/cli for the version selector dialog.
     */
    public static List<String> fetchAllVersions(boolean force) {
        if (!force && cachedAllVersions != null && !cachedAllVersions.isEmpty()) {
            return cachedAllVersions;
        }

        List<String> result = new ArrayList<>();
        String registryUrl = "https://registry.npmjs.org/@angular%2Fcli";

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(3000))
                    .build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(registryUrl))
                    .timeout(Duration.ofMillis(4000))
                    .header("Accept", "application/json")
                    .header("User-Agent", "Lumina-IDE")
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200 && resp.body() != null && !resp.body().isBlank()) {
                Matcher m = Pattern.compile("\"versions\"\\s*:\\s*\\{([^}]+)\\}").matcher(resp.body());
                if (m.find()) {
                    String block = m.group(1);
                    Matcher vm = Pattern.compile("\"([0-9]+\\.[0-9]+\\.[0-9]+[^\"]*)\"\\s*:").matcher(block);
                    Set<String> set = new LinkedHashSet<>();
                    while (vm.find()) {
                        set.add(vm.group(1));
                    }
                    List<String> list = new ArrayList<>(set);
                    Collections.reverse(list);
                    if (!list.isEmpty()) {
                        result.addAll(list);
                    }
                }
            }
        } catch (Exception ignored) {}

        if (result.isEmpty()) {
            result.addAll(FALLBACK_VERSIONS);
        }

        cachedAllVersions = List.copyOf(result);
        return cachedAllVersions;
    }

    public static void clearCache() {
        cachedLatestVersion = null;
        cachedAllVersions = null;
    }
}
