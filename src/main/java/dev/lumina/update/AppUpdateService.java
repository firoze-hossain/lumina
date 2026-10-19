package dev.lumina.update;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.lumina.plugin.RozeHubClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Dynamic dual-source application-update client for Lumina IDE.
 * Priority 1: Checks website / RozeHub API endpoint.
 * Priority 2: Falls back smoothly to GitHub Releases (firoze-hossain/lumina)
 * if the website is offline, unlaunched, or has no new release.
 */
public final class AppUpdateService {

    public static final String DEFAULT_GITHUB_REPO = "firoze-hossain/lumina";
    private static final Path UPDATE_DIR = Path.of(System.getProperty("user.home"), ".lumina", "updates");
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private AppUpdateService() {}

    public static String currentVersion() {
        String version = AppUpdateService.class.getPackage().getImplementationVersion();
        if (version == null || version.isBlank()) {
            version = System.getProperty("lumina.version", "1.0.0");
        }
        return version.startsWith("v") || version.startsWith("V") ? version.substring(1) : version;
    }

    public static String platform() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) return "Windows";
        if (os.contains("mac") || os.contains("darwin")) return "macOS";
        return "Linux";
    }

    public static String architecture() {
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        return arch.contains("aarch64") || arch.contains("arm64") ? "ARM64" : "x64";
    }

    /**
     * Compares two semantic version strings (e.g., "1.0.1" vs "1.0.0", "v2026.1.4" vs "1.0.0").
     * Returns > 0 if v1 > v2, < 0 if v1 < v2, and 0 if equal.
     */
    public static int compareVersions(String v1, String v2) {
        if (v1 == null && v2 == null) return 0;
        if (v1 == null) return -1;
        if (v2 == null) return 1;

        String clean1 = v1.trim().replaceFirst("^[vV]", "").split("[-_+]")[0];
        String clean2 = v2.trim().replaceFirst("^[vV]", "").split("[-_+]")[0];

        String[] parts1 = clean1.split("\\.");
        String[] parts2 = clean2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            long p1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            long p2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;
            if (p1 != p2) {
                return Long.compare(p1, p2);
            }
        }
        return 0;
    }

    private static long parseVersionPart(String part) {
        if (part == null || part.isBlank()) return 0;
        try {
            String digits = part.replaceAll("[^0-9].*$", "");
            return digits.isEmpty() ? 0 : Long.parseLong(digits);
        } catch (Exception e) {
            return 0;
        }
    }

    public static boolean isNewerVersion(String candidate, String current) {
        return compareVersions(candidate, current) > 0;
    }

    public static CompletableFuture<AppUpdate> checkForUpdate() {
        String endpoint = RozeHubClient.getInstance().getBaseUrl() + "/api/v1/updates/lumina";
        return checkForUpdate(endpoint, "Stable", DEFAULT_GITHUB_REPO);
    }

    /**
     * Checks website/RozeHub API first. If offline, unreachable, or no newer version,
     * seamlessly falls back to GitHub Releases for the configured repository.
     */
    public static CompletableFuture<AppUpdate> checkForUpdate(String endpoint, String channel, String githubRepo) {
        return checkForUpdateFromWebsite(endpoint, channel)
                .thenCompose(websiteUpdate -> {
                    if (websiteUpdate != null && websiteUpdate.available && websiteUpdate.release != null
                            && isNewerVersion(websiteUpdate.release.version, currentVersion())) {
                        websiteUpdate.source = "website";
                        return CompletableFuture.completedFuture(websiteUpdate);
                    }
                    return checkForUpdateFromGitHub(githubRepo, channel);
                })
                .exceptionallyCompose(error -> checkForUpdateFromGitHub(githubRepo, channel));
    }

    /**
     * Priority 1: Check website / RozeHub update API.
     */
    public static CompletableFuture<AppUpdate> checkForUpdateFromWebsite(String endpoint, String channel) {
        String base = (endpoint == null || endpoint.isBlank())
                ? RozeHubClient.getInstance().getBaseUrl() + "/api/v1/updates/lumina"
                : endpoint.trim();

        String separator = base.contains("?") ? "&" : "?";
        String uri = base + separator
                + "version=" + encode(currentVersion())
                + "&platform=" + encode(platform())
                + "&architecture=" + encode(architecture())
                + "&arch=" + encode(architecture())
                + "&channel=" + encode(channel == null || channel.isBlank() ? "Stable" : channel);

        HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                .timeout(Duration.ofSeconds(4)) // 4s timeout for quick fallback
                .header("Accept", "application/json")
                .header("User-Agent", "Lumina-IDE/" + currentVersion())
                .GET()
                .build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenCompose(response -> {
                    if (response.statusCode() / 100 != 2) {
                        return CompletableFuture.failedFuture(
                                new IOException("Website update server returned HTTP " + response.statusCode()));
                    }
                    try {
                        JsonObject root = GSON.fromJson(response.body(), JsonObject.class);
                        AppUpdate update = parseWebsiteUpdate(root, channel);
                        return CompletableFuture.completedFuture(update);
                    } catch (Exception e) {
                        return CompletableFuture.failedFuture(new IOException("Invalid website update response", e));
                    }
                });
    }

    private static AppUpdate parseWebsiteUpdate(JsonObject root, String channel) {
        AppUpdate update = new AppUpdate();
        update.project = "lumina";
        update.currentVersion = currentVersion();
        update.available = root.has("available") && root.get("available").getAsBoolean();
        update.mandatory = root.has("mandatory") && root.get("mandatory").getAsBoolean();
        update.latestVersion = root.has("latestVersion") && !root.get("latestVersion").isJsonNull()
                ? root.get("latestVersion").getAsString() : currentVersion();
        update.channel = channel != null ? channel : "Stable";
        update.platform = platform();
        update.architecture = architecture();
        update.source = "website";

        if (root.has("release") && root.get("release").isJsonObject()) {
            JsonObject rel = root.getAsJsonObject("release");
            AppUpdate.Release release = new AppUpdate.Release();
            release.version = update.latestVersion;
            release.channel = channel;
            release.notes = rel.has("notes") && !rel.get("notes").isJsonNull() ? rel.get("notes").getAsString() : "";
            release.fileName = rel.has("fileName") && !rel.get("fileName").isJsonNull() ? rel.get("fileName").getAsString() : "";
            release.fileSize = rel.has("fileSize") && !rel.get("fileSize").isJsonNull() ? rel.get("fileSize").getAsLong() : 0;
            release.sha256 = rel.has("sha256") && !rel.get("sha256").isJsonNull() ? rel.get("sha256").getAsString() : null;

            if (rel.has("downloadUrl") && !rel.get("downloadUrl").isJsonNull()) {
                String dUrl = rel.get("downloadUrl").getAsString();
                if (dUrl.startsWith("/")) {
                    dUrl = RozeHubClient.getInstance().getBaseUrl() + dUrl;
                }
                release.downloadUrl = dUrl;
            }
            update.release = release;
        }

        return update;
    }

    /**
     * Priority 2: Check GitHub Releases API for the repository.
     */
    public static CompletableFuture<AppUpdate> checkForUpdateFromGitHub(String repo, String channel) {
        String targetRepo = (repo == null || repo.isBlank()) ? DEFAULT_GITHUB_REPO : repo.trim();
        String latestUrl = "https://api.github.com/repos/" + targetRepo + "/releases/latest";

        HttpRequest request = HttpRequest.newBuilder(URI.create(latestUrl))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Lumina-IDE/" + currentVersion())
                .GET()
                .build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenCompose(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonObject rootNode = GSON.fromJson(response.body(), JsonObject.class);
                            return CompletableFuture.completedFuture(parseGitHubRelease(rootNode, currentVersion(), channel, targetRepo));
                        } catch (Exception e) {
                            return CompletableFuture.failedFuture(e);
                        }
                    } else if (response.statusCode() == 404) {
                        return queryGitHubReleasesList(targetRepo, channel);
                    } else {
                        return CompletableFuture.failedFuture(
                                new IOException("GitHub API returned HTTP " + response.statusCode()));
                    }
                })
                .exceptionallyCompose(err -> queryGitHubTagsFallback(targetRepo, channel));
    }

    private static CompletableFuture<AppUpdate> queryGitHubReleasesList(String targetRepo, String channel) {
        String releasesUrl = "https://api.github.com/repos/" + targetRepo + "/releases";
        HttpRequest request = HttpRequest.newBuilder(URI.create(releasesUrl))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Lumina-IDE/" + currentVersion())
                .GET()
                .build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenCompose(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonArray array = GSON.fromJson(response.body(), JsonArray.class);
                            if (array != null && array.size() > 0) {
                                for (JsonElement elem : array) {
                                    if (elem.isJsonObject()) {
                                        JsonObject item = elem.getAsJsonObject();
                                        boolean draft = item.has("draft") && item.get("draft").getAsBoolean();
                                        if (!draft) {
                                            return CompletableFuture.completedFuture(parseGitHubRelease(item, currentVersion(), channel, targetRepo));
                                        }
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                    return queryGitHubTagsFallback(targetRepo, channel);
                });
    }

    private static CompletableFuture<AppUpdate> queryGitHubTagsFallback(String targetRepo, String channel) {
        String tagsUrl = "https://api.github.com/repos/" + targetRepo + "/tags";
        HttpRequest request = HttpRequest.newBuilder(URI.create(tagsUrl))
                .timeout(Duration.ofSeconds(6))
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Lumina-IDE/" + currentVersion())
                .GET()
                .build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonArray array = GSON.fromJson(response.body(), JsonArray.class);
                            if (array != null && array.size() > 0) {
                                JsonObject first = array.get(0).getAsJsonObject();
                                String tagName = first.has("name") ? first.get("name").getAsString() : "";
                                String cleanVer = tagName.replaceFirst("^[vV]", "");
                                boolean newer = isNewerVersion(cleanVer, currentVersion());

                                AppUpdate update = new AppUpdate();
                                update.source = "github";
                                update.project = "lumina";
                                update.currentVersion = currentVersion();
                                update.latestVersion = cleanVer;
                                update.available = newer;
                                update.channel = channel != null ? channel : "Stable";
                                update.platform = platform();
                                update.architecture = architecture();

                                if (newer) {
                                    AppUpdate.Release rel = new AppUpdate.Release();
                                    rel.version = cleanVer;
                                    rel.channel = channel;
                                    rel.notes = "New release tag " + tagName + " published on GitHub (" + targetRepo + ").";
                                    rel.htmlUrl = "https://github.com/" + targetRepo + "/releases/tag/" + tagName;
                                    rel.fileName = "lumina-" + cleanVer + ".zip";
                                    rel.downloadUrl = rel.htmlUrl;
                                    update.release = rel;
                                }
                                return update;
                            }
                        } catch (Exception ignored) {}
                    }
                    AppUpdate noUpdate = new AppUpdate();
                    noUpdate.source = "github";
                    noUpdate.currentVersion = currentVersion();
                    noUpdate.latestVersion = currentVersion();
                    noUpdate.available = false;
                    return noUpdate;
                });
    }

    public static AppUpdate parseGitHubRelease(JsonObject releaseNode, String currentVersion, String channel, String targetRepo) {
        String tagName = releaseNode.has("tag_name") && !releaseNode.get("tag_name").isJsonNull()
                ? releaseNode.get("tag_name").getAsString() : "";
        if (tagName.isBlank() && releaseNode.has("name") && !releaseNode.get("name").isJsonNull()) {
            tagName = releaseNode.get("name").getAsString();
        }
        String latestVersion = tagName.replaceFirst("^[vV]", "").trim();
        boolean newer = isNewerVersion(latestVersion, currentVersion);

        AppUpdate update = new AppUpdate();
        update.project = "lumina";
        update.currentVersion = currentVersion;
        update.latestVersion = latestVersion;
        update.available = newer;
        update.source = "github";
        update.channel = channel != null ? channel : "Stable";
        update.platform = platform();
        update.architecture = architecture();

        AppUpdate.Release release = new AppUpdate.Release();
        release.id = releaseNode.has("id") ? releaseNode.get("id").getAsLong() : 0;
        release.version = latestVersion;
        release.channel = channel;
        release.notes = releaseNode.has("body") && !releaseNode.get("body").isJsonNull()
                ? releaseNode.get("body").getAsString() : "No release notes were provided.";
        release.publishedAt = releaseNode.has("published_at") && !releaseNode.get("published_at").isJsonNull()
                ? releaseNode.get("published_at").getAsString() : "";
        release.htmlUrl = releaseNode.has("html_url") && !releaseNode.get("html_url").isJsonNull()
                ? releaseNode.get("html_url").getAsString() : "";

        // Select best matching asset for OS & Architecture
        String os = platform();
        String arch = architecture();
        JsonObject bestAsset = null;
        int bestScore = -1;

        if (releaseNode.has("assets") && releaseNode.get("assets").isJsonArray()) {
            JsonArray assets = releaseNode.getAsJsonArray("assets");
            for (JsonElement assetElem : assets) {
                if (assetElem.isJsonObject()) {
                    JsonObject asset = assetElem.getAsJsonObject();
                    String name = asset.has("name") ? asset.get("name").getAsString().toLowerCase(Locale.ROOT) : "";
                    int score = scoreAsset(name, os, arch);
                    if (score > bestScore) {
                        bestScore = score;
                        bestAsset = asset;
                    }
                }
            }
        }

        if (bestAsset != null) {
            release.fileName = bestAsset.has("name") ? bestAsset.get("name").getAsString() : "";
            release.fileSize = bestAsset.has("size") ? bestAsset.get("size").getAsLong() : 0;
            release.downloadUrl = bestAsset.has("browser_download_url") ? bestAsset.get("browser_download_url").getAsString() : "";
        } else {
            release.fileName = "lumina-" + latestVersion + ".zip";
            release.downloadUrl = releaseNode.has("zipball_url") && !releaseNode.get("zipball_url").isJsonNull()
                    ? releaseNode.get("zipball_url").getAsString() : release.htmlUrl;
        }

        update.release = release;
        return update;
    }

    private static int scoreAsset(String name, String os, String arch) {
        int score = 0;
        if ("Linux".equals(os)) {
            if (name.endsWith(".deb")) score += 1000;
            else if (name.endsWith(".appimage")) score += 800;
            else if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) score += 600;
            else if (name.endsWith(".rpm")) score += 300;
            else if (name.endsWith(".zip")) score += 200;

            if (score > 0) {
                if ("ARM64".equals(arch) && (name.contains("arm64") || name.contains("aarch64"))) score += 300;
                if ("x64".equals(arch) && (name.contains("x86_64") || name.contains("amd64") || name.contains("x64"))) score += 300;
            }
        } else if ("Windows".equals(os)) {
            if (name.endsWith(".exe")) score += 1000;
            else if (name.endsWith(".msi")) score += 900;
            else if (name.endsWith(".zip")) score += 400;

            if (score > 0) {
                if ("ARM64".equals(arch) && name.contains("arm64")) score += 300;
                if ("x64".equals(arch) && (name.contains("x64") || name.contains("x86_64") || name.contains("win64"))) score += 300;
            }
        } else if ("macOS".equals(os)) {
            if (name.endsWith(".dmg")) score += 1000;
            else if (name.endsWith(".pkg")) score += 900;
            else if (name.endsWith(".zip")) score += 400;

            if (score > 0) {
                if ("ARM64".equals(arch) && (name.contains("arm64") || name.contains("aarch64"))) score += 300;
                if ("x64".equals(arch) && (name.contains("x86_64") || name.contains("x64"))) score += 300;
            }
        }
        return score;
    }

    public static CompletableFuture<Path> download(AppUpdate.Release release, Consumer<Double> progress) {
        if (release == null) {
            return CompletableFuture.failedFuture(new IOException("No update release was provided."));
        }
        String downloadUrl = release.effectiveDownloadUrl();
        String fileName = release.effectiveFileName();
        long fileSize = release.effectiveFileSize();
        String expectedSha256 = release.effectiveSha256();

        if (downloadUrl == null || downloadUrl.isBlank()) {
            return CompletableFuture.failedFuture(new IOException("No download URL was provided for update."));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Files.createDirectories(UPDATE_DIR);
                String safeName = sanitizeFileName(fileName);
                Path partial = UPDATE_DIR.resolve(safeName + ".part");
                Path target = UPDATE_DIR.resolve(safeName);
                Files.deleteIfExists(partial);

                HttpRequest request = HttpRequest.newBuilder(URI.create(downloadUrl))
                        .timeout(Duration.ofMinutes(30))
                        .header("Accept", "application/octet-stream")
                        .header("User-Agent", "Lumina-IDE/" + currentVersion())
                        .GET()
                        .build();

                HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() / 100 != 2) {
                    throw new IOException("Download failed with HTTP " + response.statusCode());
                }

                long expectedSize = fileSize;
                if (expectedSize <= 0) {
                    expectedSize = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
                }

                long downloaded = 0;
                try (InputStream in = response.body();
                     OutputStream out = Files.newOutputStream(partial)) {
                    byte[] buffer = new byte[1024 * 1024];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                        downloaded += read;
                        if (progress != null && expectedSize > 0) {
                            progress.accept(Math.min(1d, downloaded / (double) expectedSize));
                        }
                    }
                }

                if (expectedSize > 0 && downloaded != expectedSize) {
                    throw new IOException("Downloaded file size (" + downloaded + ") does not match metadata (" + expectedSize + ").");
                }

                if (expectedSha256 != null && !expectedSha256.isBlank()) {
                    String actualHash = sha256(partial);
                    if (!actualHash.equalsIgnoreCase(expectedSha256.trim())) {
                        throw new IOException("SHA-256 verification failed. The downloaded package was not verified.");
                    }
                }

                Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                if (progress != null) progress.accept(1d);
                return target;
            } catch (Exception e) {
                try { Files.deleteIfExists(UPDATE_DIR.resolve(sanitizeFileName(fileName) + ".part")); }
                catch (Exception ignored) {}
                throw new RuntimeException(e.getMessage(), e);
            }
        });
    }

    /**
     * Executes the update installer for the current OS and restarts the application cleanly.
     */
    public static void installAndRestart(Path packageFile) throws IOException {
        if (packageFile == null || !Files.isRegularFile(packageFile)) {
            throw new IOException("Update package was not found: " + packageFile);
        }

        String name = packageFile.getFileName().toString().toLowerCase(Locale.ROOT);
        String platform = platform();
        ProcessBuilder builder;

        if (platform.equals("Windows")) {
            if (name.endsWith(".msi")) {
                builder = new ProcessBuilder("msiexec.exe", "/i", packageFile.toAbsolutePath().toString());
            } else if (name.endsWith(".exe")) {
                builder = new ProcessBuilder(packageFile.toAbsolutePath().toString());
            } else {
                builder = new ProcessBuilder("explorer.exe", "/select,", packageFile.toAbsolutePath().toString());
            }
        } else if (platform.equals("macOS")) {
            if (name.endsWith(".pkg")) {
                builder = new ProcessBuilder("open", "-W", packageFile.toAbsolutePath().toString());
            } else if (name.endsWith(".dmg")) {
                builder = new ProcessBuilder("open", packageFile.toAbsolutePath().toString());
            } else {
                builder = new ProcessBuilder("open", "-R", packageFile.toAbsolutePath().toString());
            }
        } else {
            // Linux
            if (name.endsWith(".deb")) {
                try {
                    builder = new ProcessBuilder("pkexec", "dpkg", "-i", packageFile.toAbsolutePath().toString());
                } catch (Exception e) {
                    builder = new ProcessBuilder("xdg-open", packageFile.toAbsolutePath().toString());
                }
            } else if (name.endsWith(".appimage")) {
                packageFile.toFile().setExecutable(true, false);
                builder = new ProcessBuilder(packageFile.toAbsolutePath().toString());
            } else {
                builder = new ProcessBuilder("xdg-open", packageFile.getParent().toAbsolutePath().toString());
            }
        }

        builder.inheritIO().start();
        javafx.application.Platform.exit();
        System.exit(0);
    }

    private static String sanitizeFileName(String value) {
        String name = value == null || value.isBlank() ? "lumina-update" : Path.of(value).getFileName().toString();
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String sha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[1024 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) digest.update(buffer, 0, read);
        }
        StringBuilder result = new StringBuilder(64);
        for (byte b : digest.digest()) result.append(String.format("%02x", b));
        return result.toString();
    }
}
