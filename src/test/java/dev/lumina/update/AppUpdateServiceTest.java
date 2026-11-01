package dev.lumina.update;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AppUpdateServiceTest {

    private final Gson gson = new Gson();

    @Test
    void testVersionComparison() {
        assertTrue(AppUpdateService.isNewerVersion("1.0.1", "1.0.0"));
        assertTrue(AppUpdateService.isNewerVersion("1.10.0", "1.9.0"));
        assertTrue(AppUpdateService.isNewerVersion("2.0.0", "1.99.99"));
        assertTrue(AppUpdateService.isNewerVersion("v1.0.1", "1.0.0"));
        assertTrue(AppUpdateService.isNewerVersion("1.0.1", "v1.0.0"));

        assertFalse(AppUpdateService.isNewerVersion("1.0.0", "1.0.0"));
        assertFalse(AppUpdateService.isNewerVersion("v1.0.0", "1.0.0"));
        assertFalse(AppUpdateService.isNewerVersion("1.0.0", "1.0.1"));
        assertFalse(AppUpdateService.isNewerVersion("1.0.0", "2.0.0"));
        assertEquals(0, AppUpdateService.compareVersions("1.0.0", "1.0.0"));
        assertEquals(0, AppUpdateService.compareVersions("v1.0.0", "1.0.0"));
    }

    @Test
    void testUpdatePackagePreferredOverFullInstaller() {
        String json = """
        {
          "tag_name": "v1.0.1",
          "name": "Lumina 1.0.1",
          "body": "In-place seamless updates",
          "published_at": "2026-11-01T10:00:00Z",
          "html_url": "https://github.com/firoze-hossain/lumina/releases/tag/v1.0.1",
          "assets": [
            {
              "name": "lumina_1.0.1_amd64.deb",
              "size": 84000000,
              "browser_download_url": "https://github.com/firoze-hossain/lumina/releases/download/v1.0.1/lumina_1.0.1_amd64.deb"
            },
            {
              "name": "lumina-1.0.1-linux-update.tar.gz",
              "size": 75000000,
              "browser_download_url": "https://github.com/firoze-hossain/lumina/releases/download/v1.0.1/lumina-1.0.1-linux-update.tar.gz"
            },
            {
              "name": "Lumina-Setup-1.0.1.exe",
              "size": 89000000,
              "browser_download_url": "https://github.com/firoze-hossain/lumina/releases/download/v1.0.1/Lumina-Setup-1.0.1.exe"
            },
            {
              "name": "Lumina-1.0.1-windows-update.zip",
              "size": 78000000,
              "browser_download_url": "https://github.com/firoze-hossain/lumina/releases/download/v1.0.1/Lumina-1.0.1-windows-update.zip"
            },
            {
              "name": "Lumina-1.0.1.dmg",
              "size": 95000000,
              "browser_download_url": "https://github.com/firoze-hossain/lumina/releases/download/v1.0.1/Lumina-1.0.1.dmg"
            },
            {
              "name": "Lumina-1.0.1.pkg",
              "size": 93000000,
              "browser_download_url": "https://github.com/firoze-hossain/lumina/releases/download/v1.0.1/Lumina-1.0.1.pkg"
            },
            {
              "name": "Lumina-1.0.1-macos-update.tar.gz",
              "size": 80000000,
              "browser_download_url": "https://github.com/firoze-hossain/lumina/releases/download/v1.0.1/Lumina-1.0.1-macos-update.tar.gz"
            }
          ]
        }
        """;

        JsonObject releaseNode = gson.fromJson(json, JsonObject.class);
        AppUpdate update = AppUpdateService.parseGitHubRelease(releaseNode, "1.0.0", "Stable", "firoze-hossain/lumina");

        assertNotNull(update);
        assertTrue(update.available);
        assertNotNull(update.release);

        String platform = AppUpdateService.platform();
        if ("Linux".equals(platform)) {
            assertEquals("lumina-1.0.1-linux-update.tar.gz", update.release.effectiveFileName());
        } else if ("Windows".equals(platform)) {
            assertEquals("Lumina-1.0.1-windows-update.zip", update.release.effectiveFileName());
        } else if ("macOS".equals(platform)) {
            assertEquals("Lumina-1.0.1-macos-update.tar.gz", update.release.effectiveFileName());
        }
    }

    @Test
    void testGetInstallationDirectory() {
        assertNotNull(AppUpdateService.getInstallationDirectory());
    }
}
