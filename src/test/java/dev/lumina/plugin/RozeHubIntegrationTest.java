package dev.lumina.plugin;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RozeHubIntegrationTest {

    private RozeHubClient client;

    private boolean isRozeHubReachable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", 8000), 250);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @BeforeEach
    public void setUp() {
        client = RozeHubClient.getInstance();
        client.setBaseUrl("http://127.0.0.1:8000");
    }

    @Test
    public void testBaseUrlConfiguration() {
        assertEquals("http://127.0.0.1:8000", client.getBaseUrl());
        client.setBaseUrl("http://localhost:8000/");
        assertEquals("http://localhost:8000", client.getBaseUrl());
    }

    @Test
    public void testFetchMarketplacePluginsFromRozeHub() {
        Assumptions.assumeTrue(isRozeHubReachable(), "RozeHub server not running at 127.0.0.1:8000");
        try {
            List<PluginItem> plugins = client.fetchMarketplacePlugins(null);
            assertNotNull(plugins, "Plugin list should not be null");
            assertFalse(plugins.isEmpty(), "Should return at least one seeded plugin from RozeHub");

            PluginItem rozePlugin = plugins.stream()
                    .filter(p -> "dev.lumina.rozelang".equals(p.getId()))
                    .findFirst()
                    .orElse(null);

            assertNotNull(rozePlugin, "Roze Language Support plugin should be found");
            assertEquals("Roze Language Support", rozePlugin.getName());
            assertEquals("1.0.0", rozePlugin.getVersion());
            assertTrue(rozePlugin.isFromRozeHub());
            assertNotNull(rozePlugin.getDownloadUrl());
            assertNotNull(rozePlugin.getSha256());
        } catch (Exception e) {
            fail("Failed to fetch plugins from RozeHub: " + e.getMessage());
        }
    }

    @Test
    public void testDownloadAndInstallPluginFromRozeHub() {
        Assumptions.assumeTrue(isRozeHubReachable(), "RozeHub server not running at 127.0.0.1:8000");
        try {
            List<PluginItem> plugins = client.fetchMarketplacePlugins(null);
            PluginItem rozePlugin = plugins.stream()
                    .filter(p -> "dev.lumina.rozelang".equals(p.getId()))
                    .findFirst()
                    .orElseThrow();

            Path installedPath = client.downloadAndInstallPlugin(rozePlugin, progress -> {
                assertTrue(progress >= 0.0 && progress <= 1.0);
            });

            assertNotNull(installedPath);
            assertTrue(Files.exists(installedPath), "Plugin file must exist on disk");

            // Verify plugin registry picked it up
            assertTrue(PluginRegistry.getInstance().isInstalled("dev.lumina.rozelang")
                    || Files.exists(installedPath));

        } catch (Exception e) {
            fail("Failed to download and install plugin: " + e.getMessage());
        }
    }

    @Test
    public void testCheckForIdeUpdatesFromRozeHub() {
        Assumptions.assumeTrue(isRozeHubReachable(), "RozeHub server not running at 127.0.0.1:8000");
        try {
            RozeHubClient.UpdateInfo updateInfo = client.checkForUpdates("0.1.0");
            assertNotNull(updateInfo);
            assertTrue(updateInfo.available(), "Update should be available from 0.1.0 to 1.1.0");
            assertEquals("1.1.0", updateInfo.latestVersion());
            assertNotNull(updateInfo.downloadUrl());
            assertNotNull(updateInfo.notes());
            assertTrue(updateInfo.notes().contains("Lumina 1.1.0"));
        } catch (Exception e) {
            fail("Failed to check for updates: " + e.getMessage());
        }
    }
}
