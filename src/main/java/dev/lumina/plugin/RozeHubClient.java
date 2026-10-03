package dev.lumina.plugin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.lumina.util.Settings;

import java.io.*;
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
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Dynamic RozeHub client connecting Lumina IDE with the RozeHub ecosystem platform.
 * Supports marketplace plugin discovery, package download with SHA-256 verification,
 * dynamic registration, and desktop IDE updates.
 */
public class RozeHubClient {

    public static final String DEFAULT_ROZEHUB_URL = "http://localhost:8000";
    public static final String SETTING_KEY_URL = "rozehub.server.url";
    public static final String PROJECT_SLUG = "lumina";

    private static RozeHubClient instance;

    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public record UpdateInfo(
            boolean available,
            boolean mandatory,
            String currentVersion,
            String latestVersion,
            String notes,
            String downloadUrl,
            String fileName,
            long fileSize,
            String sha256
    ) {}

    private RozeHubClient() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(6))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public static synchronized RozeHubClient getInstance() {
        if (instance == null) {
            instance = new RozeHubClient();
        }
        return instance;
    }

    /**
     * Resolves the configured RozeHub base URL.
     * Order of precedence:
     * 1. System property (-Drozehub.url=...)
     * 2. Environment variable (ROZEHUB_URL)
     * 3. ~/.lumina/lumina.properties (rozehub.server.url)
     * 4. Default: http://localhost:8000
     */
    public String getBaseUrl() {
        String sys = System.getProperty("rozehub.url");
        if (sys != null && !sys.isBlank()) return sanitizeUrl(sys);

        String env = System.getenv("ROZEHUB_URL");
        if (env != null && !env.isBlank()) return sanitizeUrl(env);

        String pref = Settings.get(SETTING_KEY_URL);
        if (pref != null && !pref.isBlank()) return sanitizeUrl(pref);

        return DEFAULT_ROZEHUB_URL;
    }

    public void setBaseUrl(String url) {
        if (url == null || url.isBlank() || url.equals(DEFAULT_ROZEHUB_URL)) {
            Settings.put(SETTING_KEY_URL, null);
        } else {
            Settings.put(SETTING_KEY_URL, sanitizeUrl(url));
        }
    }

    private String sanitizeUrl(String url) {
        String clean = url.trim();
        while (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean;
    }

    public String detectPlatform() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) return "Windows";
        if (os.contains("mac")) return "macOS";
        if (os.contains("linux")) return "Linux";
        return "All";
    }

    public String detectArchitecture() {
        String arch = System.getProperty("os.arch", "").toLowerCase();
        if (arch.contains("aarch64") || arch.contains("arm64")) return "ARM64";
        return "x64";
    }

    /**
     * Fetches marketplace plugins dynamically from RozeHub for Lumina.
     */
    public List<PluginItem> fetchMarketplacePlugins(String query) throws IOException, InterruptedException {
        String base = getBaseUrl();
        StringBuilder url = new StringBuilder(base)
                .append("/api/v1/marketplace/").append(PROJECT_SLUG)
                .append("?platform=").append(URLEncoder.encode(detectPlatform(), StandardCharsets.UTF_8))
                .append("&architecture=").append(URLEncoder.encode(detectArchitecture(), StandardCharsets.UTF_8))
                .append("&channel=Stable");

        if (query != null && !query.isBlank()) {
            url.append("&q=").append(URLEncoder.encode(query.trim(), StandardCharsets.UTF_8));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/json")
                .header("User-Agent", "Lumina-IDE/0.1.0 (RozeHub-Client)")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("RozeHub returned HTTP status " + response.statusCode() + " (" + response.body() + ")");
        }

        List<PluginItem> list = new ArrayList<>();
        JsonObject root = gson.fromJson(response.body(), JsonObject.class);
        if (root == null || !root.has("items")) return list;

        JsonArray items = root.getAsJsonArray("items");
        for (JsonElement el : items) {
            if (!el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();

            String id = obj.has("id") && !obj.get("id").isJsonNull() ? obj.get("id").getAsString() : null;
            String name = obj.has("name") && !obj.get("name").isJsonNull() ? obj.get("name").getAsString() : "Plugin";
            String vendor = obj.has("vendor") && !obj.get("vendor").isJsonNull() ? obj.get("vendor").getAsString() : "RozeHub Publisher";
            if (id == null || id.isBlank()) {
                id = obj.has("slug") ? obj.get("slug").getAsString() : name.toLowerCase().replaceAll("[^a-z0-9]", ".");
            }

            String version = "1.0.0";
            String downloadUrl = null;
            String sha256 = null;
            String packageType = "jar";
            String fileName = null;
            String releaseNotes = null;
            long fileSize = 0;

            if (obj.has("latest") && obj.get("latest").isJsonObject()) {
                JsonObject latest = obj.getAsJsonObject("latest");
                if (latest.has("version") && !latest.get("version").isJsonNull()) {
                    version = latest.get("version").getAsString();
                }
                if (latest.has("downloadUrl") && !latest.get("downloadUrl").isJsonNull()) {
                    downloadUrl = latest.get("downloadUrl").getAsString();
                    if (downloadUrl.startsWith("/")) {
                        downloadUrl = base + downloadUrl;
                    }
                }
                if (latest.has("sha256") && !latest.get("sha256").isJsonNull()) {
                    sha256 = latest.get("sha256").getAsString();
                }
                if (latest.has("packageType") && !latest.get("packageType").isJsonNull()) {
                    packageType = latest.get("packageType").getAsString();
                }
                if (latest.has("fileName") && !latest.get("fileName").isJsonNull()) {
                    fileName = latest.get("fileName").getAsString();
                }
                if (latest.has("releaseNotes") && !latest.get("releaseNotes").isJsonNull()) {
                    releaseNotes = latest.get("releaseNotes").getAsString();
                }
                if (latest.has("fileSize") && !latest.get("fileSize").isJsonNull()) {
                    fileSize = latest.get("fileSize").getAsLong();
                }
            }

            PluginItem plugin = new PluginItem(id, name, version, vendor);
            plugin.setFromRozeHub(true);
            plugin.setDownloadUrl(downloadUrl);
            plugin.setSha256(sha256);
            plugin.setPackageType(packageType);
            plugin.setFileName(fileName);
            plugin.setWhatsNew(releaseNotes);

            if (fileSize > 0) {
                double mb = fileSize / (1024.0 * 1024.0);
                plugin.setSize(String.format(Locale.US, "%.1f MB", mb));
            }

            if (obj.has("summary") && !obj.get("summary").isJsonNull()) {
                plugin.setShortDescription(obj.get("summary").getAsString());
            }
            if (obj.has("description") && !obj.get("description").isJsonNull()) {
                plugin.setFullDescription(obj.get("description").getAsString());
            }
            if (obj.has("license") && !obj.get("license").isJsonNull()) {
                plugin.setLicense(obj.get("license").getAsString());
            }
            if (obj.has("website") && !obj.get("website").isJsonNull()) {
                plugin.setHomepageUrl(obj.get("website").getAsString());
            }
            if (obj.has("downloads") && !obj.get("downloads").isJsonNull()) {
                plugin.setDownloads(String.valueOf(obj.get("downloads").getAsInt()));
            }

            // Rating
            if (obj.has("rating") && obj.get("rating").isJsonObject()) {
                JsonObject r = obj.getAsJsonObject("rating");
                if (r.has("average")) {
                    plugin.setRating(String.format(Locale.US, "%.1f", r.get("average").getAsDouble()));
                }
            }

            // Tags
            List<String> tags = new ArrayList<>();
            if (obj.has("category") && !obj.get("category").isJsonNull()) {
                tags.add(obj.get("category").getAsString());
            }
            if (obj.has("type") && !obj.get("type").isJsonNull()) {
                tags.add(obj.get("type").getAsString());
            }
            if (obj.has("permissions") && obj.get("permissions").isJsonArray()) {
                for (JsonElement p : obj.getAsJsonArray("permissions")) {
                    tags.add(p.getAsString());
                }
            }
            plugin.setTags(tags);
            plugin.setMarketplaceSection(obj.has("official") && obj.get("official").getAsBoolean() ? "Staff Picks" : "New and Updated");
            plugin.setIconSymbol(name.length() > 0 ? name.substring(0, 1).toUpperCase() : "P");
            plugin.setIconBgColor("#3B82F6");

            list.add(plugin);
        }

        return list;
    }

    /**
     * Downloads and installs a plugin package into ~/.lumina/plugins.
     * Verifies SHA-256 integrity and registers manifest.
     */
    public Path downloadAndInstallPlugin(PluginItem plugin, Consumer<Double> progressConsumer) throws Exception {
        String downloadUrl = plugin.getDownloadUrl();
        if (downloadUrl == null || downloadUrl.isBlank()) {
            throw new IllegalArgumentException("Plugin " + plugin.getName() + " does not have a valid download URL.");
        }

        Path pluginDir = Path.of(System.getProperty("user.home"), ".lumina", "plugins");
        Files.createDirectories(pluginDir);

        String fileName = plugin.getFileName();
        if (fileName == null || fileName.isBlank()) {
            String ext = "zip".equalsIgnoreCase(plugin.getPackageType()) ? ".zip" : ".jar";
            fileName = plugin.getId() + "-" + plugin.getVersion() + ext;
        }

        Path tempFile = Files.createTempFile("lumina-plugin-", fileName);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .header("User-Agent", "Lumina-IDE/0.1.0 (RozeHub-Client)")
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            Files.deleteIfExists(tempFile);
            throw new IOException("Failed to download package: HTTP " + response.statusCode());
        }

        long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (InputStream in = response.body();
             OutputStream out = Files.newOutputStream(tempFile)) {
            byte[] buf = new byte[8192];
            long totalRead = 0;
            int read;
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
                digest.update(buf, 0, read);
                totalRead += read;
                if (contentLength > 0 && progressConsumer != null) {
                    progressConsumer.accept((double) totalRead / (double) contentLength);
                }
            }
        }

        byte[] hashBytes = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        String calculatedSha256 = hexString.toString();

        // Verify SHA-256 if provided
        String expectedSha256 = plugin.getSha256();
        if (expectedSha256 != null && !expectedSha256.isBlank()) {
            if (!calculatedSha256.equalsIgnoreCase(expectedSha256.trim())) {
                Files.deleteIfExists(tempFile);
                throw new SecurityException("SHA-256 checksum mismatch! Expected: " + expectedSha256 + ", got: " + calculatedSha256);
            }
        }

        Path targetPath;
        if (fileName.endsWith(".zip")) {
            // Unpack ZIP to plugin directory ~/.lumina/plugins/<pluginId>
            targetPath = pluginDir.resolve(plugin.getId());
            Files.createDirectories(targetPath);
            extractZip(tempFile, targetPath);
            Files.deleteIfExists(tempFile);
            loadManifestFromDir(targetPath, plugin);
        } else {
            // Keep JAR in ~/.lumina/plugins/<fileName>
            targetPath = pluginDir.resolve(fileName);
            Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
            loadManifestFromJar(targetPath, plugin);
        }

        return targetPath;
    }

    private void extractZip(Path zipFile, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName()).normalize();
                if (!entryPath.startsWith(targetDir)) {
                    throw new SecurityException("Zip slip detected in: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    private void loadManifestFromJar(Path jarPath, PluginItem plugin) {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            JarEntry entry = jar.getJarEntry("plugin.json");
            if (entry != null) {
                String json = new String(jar.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
                PluginManifest manifest = gson.fromJson(json, PluginManifest.class);
                if (manifest != null) {
                    manifest.setInstalled(true);
                    PluginRegistry.getInstance().getAvailablePlugins(); // triggers ensure
                    PluginRegistry.getInstance().installPlugin(manifest.getId());
                }
            }
        } catch (Throwable ignored) {}
    }

    private void loadManifestFromDir(Path dir, PluginItem plugin) {
        Path manifestFile = dir.resolve("plugin.json");
        if (Files.isRegularFile(manifestFile)) {
            try {
                String json = Files.readString(manifestFile, StandardCharsets.UTF_8);
                PluginManifest manifest = gson.fromJson(json, PluginManifest.class);
                if (manifest != null) {
                    manifest.setInstalled(true);
                    PluginRegistry.getInstance().installPlugin(manifest.getId());
                }
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Checks RozeHub for updates to Lumina IDE.
     */
    public UpdateInfo checkForUpdates(String currentVersion) throws IOException, InterruptedException {
        String base = getBaseUrl();
        String url = base + "/api/v1/updates/" + PROJECT_SLUG +
                "?version=" + URLEncoder.encode(currentVersion, StandardCharsets.UTF_8) +
                "&platform=" + URLEncoder.encode(detectPlatform(), StandardCharsets.UTF_8) +
                "&architecture=" + URLEncoder.encode(detectArchitecture(), StandardCharsets.UTF_8) +
                "&channel=Stable";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(6))
                .header("Accept", "application/json")
                .header("User-Agent", "Lumina-IDE/" + currentVersion + " (RozeHub-Client)")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("RozeHub returned HTTP status " + response.statusCode());
        }

        JsonObject root = gson.fromJson(response.body(), JsonObject.class);
        boolean available = root.has("available") && root.get("available").getAsBoolean();
        boolean mandatory = root.has("mandatory") && root.get("mandatory").getAsBoolean();
        String latestVer = root.has("latestVersion") && !root.get("latestVersion").isJsonNull()
                ? root.get("latestVersion").getAsString() : currentVersion;

        String notes = "";
        String downloadUrl = null;
        String fileName = null;
        long fileSize = 0;
        String sha256 = null;

        if (root.has("release") && root.get("release").isJsonObject()) {
            JsonObject rel = root.getAsJsonObject("release");
            if (rel.has("notes") && !rel.get("notes").isJsonNull()) {
                notes = rel.get("notes").getAsString();
            }
            if (rel.has("downloadUrl") && !rel.get("downloadUrl").isJsonNull()) {
                downloadUrl = rel.get("downloadUrl").getAsString();
                if (downloadUrl.startsWith("/")) {
                    downloadUrl = base + downloadUrl;
                }
            }
            if (rel.has("fileName") && !rel.get("fileName").isJsonNull()) {
                fileName = rel.get("fileName").getAsString();
            }
            if (rel.has("fileSize") && !rel.get("fileSize").isJsonNull()) {
                fileSize = rel.get("fileSize").getAsLong();
            }
            if (rel.has("sha256") && !rel.get("sha256").isJsonNull()) {
                sha256 = rel.get("sha256").getAsString();
            }
        }

        return new UpdateInfo(available, mandatory, currentVersion, latestVer, notes, downloadUrl, fileName, fileSize, sha256);
    }
}
