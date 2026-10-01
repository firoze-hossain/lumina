package dev.lumina.ui;

import dev.lumina.plugin.PluginItem;
import dev.lumina.plugin.PluginManager;
import dev.lumina.plugin.PluginManager.FilterOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite verifying dynamic plugin management,
 * UI components, search/filtering, and strict brand isolation.
 */
public class SettingsPluginsTest {

    private PluginManager pluginManager;

    @BeforeEach
    void setUp() {
        pluginManager = PluginManager.getInstance();
        pluginManager.resetToDefaults();
    }

    @Test
    void testMarketplaceCatalog() {
        List<PluginItem> marketplace = pluginManager.getMarketplacePlugins();
        assertNotNull(marketplace);
        assertTrue(marketplace.size() >= 10, "Marketplace should contain at least 10 plugins");

        // Verify key marketplace plugins from Image 1
        assertTrue(marketplace.stream().anyMatch(p -> p.getName().contains("AI Assistant")), "Should contain AI Assistant");
        assertTrue(marketplace.stream().anyMatch(p -> p.getName().contains("Junie")), "Should contain Junie");
        assertTrue(marketplace.stream().anyMatch(p -> p.getName().contains("Spring Debugger")), "Should contain Spring Debugger");
        assertTrue(marketplace.stream().anyMatch(p -> p.getName().contains("Kotlin Multiplatform")), "Should contain Kotlin Multiplatform");
        assertTrue(marketplace.stream().anyMatch(p -> p.getName().contains("Develocity")), "Should contain Develocity");
        assertTrue(marketplace.stream().anyMatch(p -> p.getName().contains("Key Promoter X")), "Should contain Key Promoter X");
        assertTrue(marketplace.stream().anyMatch(p -> p.getName().equals("Vim")), "Should contain Vim");
        assertTrue(marketplace.stream().anyMatch(p -> p.getName().contains("Confluent")), "Should contain Confluent");
        assertTrue(marketplace.stream().anyMatch(p -> p.getName().contains("GitHub Actions Manager")), "Should contain GitHub Actions Manager");
        assertTrue(marketplace.stream().anyMatch(p -> p.getName().contains("Twig")), "Should contain Twig");

        // Verify sections
        PluginItem ai = pluginManager.findMarketplacePlugin("dev.lumina.ai");
        assertNotNull(ai);
        assertEquals("Staff Picks", ai.getMarketplaceSection());
        assertTrue(ai.isFreemium());
        assertFalse(ai.getTags().isEmpty());
        assertNotNull(ai.getCarouselSlides());
        assertFalse(ai.getCarouselSlides().isEmpty());

        PluginItem twig = pluginManager.findMarketplacePlugin("dev.lumina.twig");
        assertNotNull(twig);
        assertEquals("New and Updated", twig.getMarketplaceSection());
    }

    @Test
    void testInstalledCatalogAndUpdates() {
        List<PluginItem> installed = pluginManager.getInstalledPlugins();
        assertNotNull(installed);
        assertFalse(installed.isEmpty());

        // Verify bundled plugins with updates from Image 2 & 3
        PluginItem http = pluginManager.findInstalledPlugin("dev.lumina.httpclient");
        assertNotNull(http);
        assertTrue(http.isBundled());
        assertTrue(http.hasUpdate());
        assertEquals("253.30387.90", http.getVersion());
        assertEquals("253.30387.92", http.getAvailableVersion());
        assertFalse(http.getFeatures().isEmpty());

        PluginItem svn = pluginManager.findInstalledPlugin("dev.lumina.subversion");
        assertNotNull(svn);
        assertTrue(svn.hasUpdate());
        assertEquals("253.30387.205", svn.getAvailableVersion());

        PluginItem spring = pluginManager.findInstalledPlugin("dev.lumina.spring");
        assertNotNull(spring);
        assertTrue(spring.hasUpdate());

        PluginItem copilot = pluginManager.findInstalledPlugin("com.github.copilot");
        assertNotNull(copilot);
        assertTrue(copilot.hasUpdate());
        assertEquals("1.18.0-251", copilot.getAvailableVersion());

        PluginItem go = pluginManager.findInstalledPlugin("dev.lumina.go");
        assertNotNull(go);
        assertFalse(go.isBundled());
        assertEquals("253.30387.20", go.getVersion());
        assertFalse(go.hasUpdate());

        // Badge count should be 10 (matching screenshots)
        assertEquals(10, pluginManager.getBadgeCount());
    }

    @Test
    void testDynamicEnableDisable() {
        int initialEnabled = pluginManager.getDownloadedEnabledCount();
        assertTrue(initialEnabled > 0);

        // Toggle Go plugin
        pluginManager.setPluginEnabled("dev.lumina.go", false);
        PluginItem go = pluginManager.findInstalledPlugin("dev.lumina.go");
        assertNotNull(go);
        assertFalse(go.isEnabled());
        assertEquals(initialEnabled - 1, pluginManager.getDownloadedEnabledCount());

        // Re-enable
        pluginManager.setPluginEnabled("dev.lumina.go", true);
        assertTrue(go.isEnabled());
        assertEquals(initialEnabled, pluginManager.getDownloadedEnabledCount());

        // Bulk disable all downloaded plugins (Gear menu option)
        pluginManager.disableAllDownloaded();
        assertEquals(0, pluginManager.getDownloadedEnabledCount());

        // Bulk enable all downloaded plugins (Gear menu option)
        pluginManager.enableAllDownloaded();
        assertEquals(pluginManager.getDownloadedTotalCount(), pluginManager.getDownloadedEnabledCount());
    }

    @Test
    void testDynamicUpdateAction() {
        PluginItem http = pluginManager.findInstalledPlugin("dev.lumina.httpclient");
        assertNotNull(http);
        assertTrue(http.hasUpdate());

        // Perform update
        pluginManager.updatePlugin("dev.lumina.httpclient");
        assertEquals("253.30387.92", http.getVersion());
        assertNull(http.getAvailableVersion());
        assertFalse(http.hasUpdate());

        // Test updateAllPlugins
        assertTrue(pluginManager.getUpdatesCount() > 0);
        pluginManager.updateAllPlugins();
        assertEquals(0, pluginManager.getUpdatesCount());
    }

    @Test
    void testDynamicInstallAndUninstall() {
        // Find uninstalled marketplace plugin: Junie
        PluginItem junie = pluginManager.findMarketplacePlugin("dev.lumina.junie");
        assertNotNull(junie);
        assertFalse(junie.isInstalled());

        int initialInstalledCount = pluginManager.getInstalledPlugins().size();

        // Install
        pluginManager.installPlugin("dev.lumina.junie");
        assertTrue(junie.isInstalled());
        assertEquals(initialInstalledCount + 1, pluginManager.getInstalledPlugins().size());

        PluginItem installedJunie = pluginManager.findInstalledPlugin("dev.lumina.junie");
        assertNotNull(installedJunie);
        assertTrue(installedJunie.isEnabled());
        assertFalse(installedJunie.isBundled());

        // Uninstall
        pluginManager.uninstallPlugin("dev.lumina.junie");
        assertFalse(junie.isInstalled());
        assertEquals(initialInstalledCount, pluginManager.getInstalledPlugins().size());
        assertNull(pluginManager.findInstalledPlugin("dev.lumina.junie"));
    }

    @Test
    void testInstallFromDisk() throws Exception {
        File tempPlugin = File.createTempFile("lumina-test-plugin", ".jar");
        tempPlugin.deleteOnExit();

        int initialCount = pluginManager.getInstalledPlugins().size();
        pluginManager.installPluginFromDisk(tempPlugin);

        assertEquals(initialCount + 1, pluginManager.getInstalledPlugins().size());
        boolean hasDiskPlugin = pluginManager.getInstalledPlugins().stream()
                .anyMatch(p -> p.getId().startsWith("dev.lumina.disk."));
        assertTrue(hasDiskPlugin);
    }

    @Test
    void testFilteringAndSearch() {
        // Filter by DOWNLOADED
        List<PluginItem> downloaded = pluginManager.getFilteredInstalledPlugins(FilterOption.DOWNLOADED, null);
        assertFalse(downloaded.isEmpty());
        assertTrue(downloaded.stream().noneMatch(PluginItem::isBundled));

        // Filter by BUNDLED
        List<PluginItem> bundled = pluginManager.getFilteredInstalledPlugins(FilterOption.BUNDLED, null);
        assertFalse(bundled.isEmpty());
        assertTrue(bundled.stream().allMatch(PluginItem::isBundled));

        // Filter by UPDATE_AVAILABLE
        List<PluginItem> updateable = pluginManager.getFilteredInstalledPlugins(FilterOption.UPDATE_AVAILABLE, null);
        assertFalse(updateable.isEmpty());
        assertTrue(updateable.stream().allMatch(PluginItem::hasUpdate));

        // Search text
        List<PluginItem> searched = pluginManager.getFilteredInstalledPlugins(FilterOption.ALL, "http");
        assertEquals(1, searched.size());
        assertEquals("HTTP Client", searched.get(0).getName());

        // Slash command: /update
        List<PluginItem> slashUpdate = pluginManager.getFilteredInstalledPlugins(FilterOption.ALL, "/update");
        assertFalse(slashUpdate.isEmpty());
        assertTrue(slashUpdate.stream().allMatch(PluginItem::hasUpdate));

        // Slash command: /tag:freemium
        List<PluginItem> slashTag = pluginManager.getFilteredMarketplacePlugins("/tag:freemium");
        assertFalse(slashTag.isEmpty());
        assertTrue(slashTag.stream().anyMatch(p -> p.getName().contains("AI Assistant")));
    }

    @Test
    void testGearOptionsAndRepositories() {
        // Auto update toggle
        assertTrue(pluginManager.isAutoUpdateEnabled());
        pluginManager.setAutoUpdateEnabled(false);
        assertFalse(pluginManager.isAutoUpdateEnabled());

        // Repositories
        int initialRepos = pluginManager.getCustomRepositories().size();
        String testRepo = "https://custom.plugins.org/repo";
        pluginManager.addCustomRepository(testRepo);
        assertEquals(initialRepos + 1, pluginManager.getCustomRepositories().size());
        assertTrue(pluginManager.getCustomRepositories().contains(testRepo));

        pluginManager.removeCustomRepository(testRepo);
        assertEquals(initialRepos, pluginManager.getCustomRepositories().size());

        // Certificates
        pluginManager.addCertificate("Enterprise Cert", "Lumina CA", "A1B2C3D4");
        assertFalse(pluginManager.getCertificates().isEmpty());
    }

    @Test
    void testBrandIsolation() throws Exception {
        // Ensure no competitor names in new classes
        List<String> filesToCheck = List.of(
                "src/main/java/dev/lumina/plugin/PluginItem.java",
                "src/main/java/dev/lumina/plugin/PluginManager.java",
                "src/main/java/dev/lumina/ui/PluginIconFactory.java",
                "src/main/java/dev/lumina/ui/SettingsPluginsPage.java",
                "src/main/java/dev/lumina/ui/PluginRepositoriesDialog.java",
                "src/main/java/dev/lumina/ui/PluginCertificatesDialog.java"
        );

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?i)\\b(intellij|jetbrains|idea(?!vim))\\b");

        for (String file : filesToCheck) {
            File f = new File(file);
            assertTrue(f.exists(), "File must exist: " + file);
            String content = Files.readString(f.toPath());
            java.util.regex.Matcher m = pattern.matcher(content);
            assertFalse(m.find(), "Competitor brand found in " + file + ": " + (m.reset().find() ? m.group() : ""));
        }
    }
}
