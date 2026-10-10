package dev.lumina.ui;

import dev.lumina.tools.*;
import dev.lumina.util.Settings;
import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GitHub Copilot Keymap, MCP, Network, and HTTP Client
 * models, managers, UI pages, SettingsDialog integration, and brand isolation.
 */
public class SettingsToolsCopilotKeymapMcpNetworkHttpClientTest {

    private static boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFx() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            try {
                Platform.startup(() -> {
                    javaFxAvailable = true;
                    latch.countDown();
                });
            } catch (IllegalStateException e) {
                javaFxAvailable = true;
                latch.countDown();
            }
            javaFxAvailable = latch.await(5, TimeUnit.SECONDS);
        } catch (Throwable t) {
            javaFxAvailable = false;
        }
    }

    @BeforeEach
    void setup() {
        GitHubCopilotKeymapSettingsManager.getInstance().clear();
        GitHubCopilotMcpSettingsManager.getInstance().clear();
        GitHubCopilotNetworkSettingsManager.getInstance().clear();
        HttpClientSettingsManager.getInstance().clear();
    }

    private void runOnFx(Runnable action) throws Exception {
        if (!javaFxAvailable) return;
        CountDownLatch latch = new CountDownLatch(1);
        Throwable[] err = new Throwable[1];
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                err[0] = t;
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX operation timed out");
        if (err[0] != null) {
            if (err[0] instanceof Exception) throw (Exception) err[0];
            throw new RuntimeException(err[0]);
        }
    }

    // ==========================================
    // 1. GitHub Copilot Keymap Model & Manager Tests
    // ==========================================

    @Test
    void testGitHubCopilotKeymapEntry() {
        GitHubCopilotKeymapEntry entry = new GitHubCopilotKeymapEntry("Copilot: Accept Current Diff Block", "Ctrl+Y");
        assertEquals("Copilot: Accept Current Diff Block", entry.getAction());
        assertEquals("Copilot: Accept Current Diff Block", entry.getActionName());
        assertEquals("Ctrl+Y", entry.getKeymap());

        entry.setKeymap("Ctrl+Shift+Y");
        assertEquals("Ctrl+Shift+Y", entry.getKeymap());

        entry.setActionName("Copilot: Accept Block");
        assertEquals("Copilot: Accept Block", entry.getAction());

        GitHubCopilotKeymapEntry copy = entry.copy();
        assertEquals(entry, copy);
        assertEquals(entry.hashCode(), copy.hashCode());
        assertTrue(entry.toString().contains("Copilot: Accept Block"));
    }

    @Test
    void testGitHubCopilotKeymapSettingsModel() {
        GitHubCopilotKeymapSettings settings = new GitHubCopilotKeymapSettings();
        List<GitHubCopilotKeymapEntry> entries = settings.getEntries();
        assertNotNull(entries);
        assertEquals(33, entries.size(), "Must contain 33 standard Copilot keymap actions");

        // Verify specific keymap entries
        assertTrue(entries.stream().anyMatch(e -> e.getActionName().equals("Copilot: Accept Current Diff Block") && e.getKeymap().equals("Ctrl+Y")));
        assertTrue(entries.stream().anyMatch(e -> e.getActionName().equals("Copilot: Accept Next Edit Suggestion") && e.getKeymap().equals("Tab")));
        assertTrue(entries.stream().anyMatch(e -> e.getActionName().equals("Copilot: Disable Completions") && e.getKeymap().equals("Ctrl+Alt+Shift+O")));
        assertTrue(entries.stream().anyMatch(e -> e.getActionName().equals("Copilot: Open Chat") && e.getKeymap().equals("Ctrl+Shift+C")));
        assertTrue(entries.stream().anyMatch(e -> e.getActionName().equals("Open Inline Chat") && e.getKeymap().equals("Ctrl+Shift+G")));

        // Test deep copy
        GitHubCopilotKeymapSettings clone = settings.copy();
        assertEquals(settings, clone);
        clone.getEntries().get(0).setKeymap("Custom+Shortcut");
        assertNotEquals(settings.getEntries().get(0).getKeymap(), clone.getEntries().get(0).getKeymap());
    }

    @Test
    void testGitHubCopilotKeymapSettingsManagerPersistence() {
        GitHubCopilotKeymapSettingsManager manager = GitHubCopilotKeymapSettingsManager.getInstance();
        GitHubCopilotKeymapSettings settings = manager.getSettings();
        assertEquals(33, settings.getEntries().size());

        // Update a shortcut
        settings.getEntries().get(0).setKeymap("Alt+Shift+K");
        manager.setSettings(settings);

        // Verify notification
        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addChangeListener(() -> notified.set(true));

        settings.getEntries().get(1).setKeymap("Ctrl+Alt+M");
        manager.setSettings(settings);
        assertTrue(notified.get());

        // Reload from settings store
        manager.load();
        assertEquals("Alt+Shift+K", manager.getSettings().getEntries().get(0).getKeymap());
        assertEquals("Ctrl+Alt+M", manager.getSettings().getEntries().get(1).getKeymap());

        // Reset defaults
        manager.resetDefaults();
        assertEquals("Ctrl+Y", manager.getSettings().getEntries().get(0).getKeymap());
    }

    // ==========================================
    // 2. GitHub Copilot MCP Model & Manager Tests
    // ==========================================

    @Test
    void testGitHubCopilotMcpSettingsModel() {
        GitHubCopilotMcpSettings settings = new GitHubCopilotMcpSettings();
        assertEquals("https://api.mcp.github.com", settings.getMcpRegistryBaseUrl());
        assertFalse(settings.isAutoApproveSampling());
        assertNotNull(settings.getAllowedModels());

        settings.setMcpRegistryBaseUrl("https://custom.mcp.registry.local");
        settings.setAutoApproveSampling(true);
        settings.setAllowedModels(Arrays.asList("gpt-4o", "claude-3-5-sonnet"));

        assertEquals("https://custom.mcp.registry.local", settings.getMcpRegistryBaseUrl());
        assertTrue(settings.isAutoApproveSampling());
        assertEquals(2, settings.getAllowedModels().size());

        GitHubCopilotMcpSettings copy = settings.copy();
        assertEquals(settings, copy);
        assertEquals(settings.hashCode(), copy.hashCode());
        assertTrue(copy.toString().contains("custom.mcp.registry.local"));
    }

    @Test
    void testGitHubCopilotMcpSettingsManagerPersistence() {
        GitHubCopilotMcpSettingsManager manager = GitHubCopilotMcpSettingsManager.getInstance();
        GitHubCopilotMcpSettings settings = manager.getSettings();
        assertEquals("https://api.mcp.github.com", settings.getMcpRegistryBaseUrl());

        settings.setMcpRegistryBaseUrl("https://hub.example.com/mcp");
        settings.setAutoApproveSampling(true);
        settings.setAllowedModels(List.of("model-a", "model-b"));

        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addChangeListener(() -> notified.set(true));

        manager.setSettings(settings);
        assertTrue(notified.get());

        manager.load();
        assertEquals("https://hub.example.com/mcp", manager.getSettings().getMcpRegistryBaseUrl());
        assertTrue(manager.getSettings().isAutoApproveSampling());
        assertEquals(2, manager.getSettings().getAllowedModels().size());

        manager.resetDefaults();
        assertEquals("https://api.mcp.github.com", manager.getSettings().getMcpRegistryBaseUrl());
        assertFalse(manager.getSettings().isAutoApproveSampling());
    }

    // ==========================================
    // 3. GitHub Copilot Network Model & Manager Tests
    // ==========================================

    @Test
    void testGitHubCopilotNetworkSettingsModel() {
        GitHubCopilotNetworkSettings settings = new GitHubCopilotNetworkSettings();
        assertFalse(settings.isCustomizeHttpProxy());
        assertEquals("", settings.getHostName());
        assertEquals(0, settings.getPortNumber());
        assertFalse(settings.isProxyAuthentication());
        assertEquals("", settings.getLogin());
        assertEquals("", settings.getPassword());
        assertEquals("Auto", settings.getNetworking());
        assertEquals("", settings.getOverrideKerberosPrincipal());

        settings.setCustomizeHttpProxy(true);
        settings.setHostName("proxy.corp.internal");
        settings.setPortNumber(8080);
        settings.setProxyAuthentication(true);
        settings.setLogin("admin");
        settings.setPassword("secretPass123");
        settings.setNetworking("Manual");
        settings.setOverrideKerberosPrincipal("HTTP/proxy.corp.internal@CORP");

        assertTrue(settings.isCustomizeHttpProxy());
        assertEquals("proxy.corp.internal", settings.getHostName());
        assertEquals(8080, settings.getPortNumber());
        assertTrue(settings.isProxyAuthentication());
        assertEquals("admin", settings.getLogin());
        assertEquals("secretPass123", settings.getPassword());
        assertEquals("Manual", settings.getNetworking());
        assertEquals("HTTP/proxy.corp.internal@CORP", settings.getOverrideKerberosPrincipal());

        GitHubCopilotNetworkSettings copy = settings.copy();
        assertEquals(settings, copy);
        assertEquals(settings.hashCode(), copy.hashCode());
        assertTrue(copy.toString().contains("proxy.corp.internal"));
    }

    @Test
    void testGitHubCopilotNetworkSettingsManagerPersistence() {
        GitHubCopilotNetworkSettingsManager manager = GitHubCopilotNetworkSettingsManager.getInstance();
        GitHubCopilotNetworkSettings settings = manager.getSettings();
        assertFalse(settings.isCustomizeHttpProxy());

        settings.setCustomizeHttpProxy(true);
        settings.setHostName("10.0.0.1");
        settings.setPortNumber(3128);
        settings.setProxyAuthentication(true);
        settings.setLogin("usr");
        settings.setPassword("pwd");
        settings.setNetworking("Disabled");
        settings.setOverrideKerberosPrincipal("HTTP/lumina@REALM");

        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addChangeListener(() -> notified.set(true));

        manager.setSettings(settings);
        assertTrue(notified.get());

        manager.load();
        assertTrue(manager.getSettings().isCustomizeHttpProxy());
        assertEquals("10.0.0.1", manager.getSettings().getHostName());
        assertEquals(3128, manager.getSettings().getPortNumber());
        assertTrue(manager.getSettings().isProxyAuthentication());
        assertEquals("usr", manager.getSettings().getLogin());
        assertEquals("pwd", manager.getSettings().getPassword());
        assertEquals("Disabled", manager.getSettings().getNetworking());
        assertEquals("HTTP/lumina@REALM", manager.getSettings().getOverrideKerberosPrincipal());

        manager.resetDefaults();
        assertFalse(manager.getSettings().isCustomizeHttpProxy());
        assertEquals("Auto", manager.getSettings().getNetworking());
    }

    // ==========================================
    // 4. HTTP Client Model & Manager Tests
    // ==========================================

    @Test
    void testHttpClientSettingsModel() {
        HttpClientSettings settings = new HttpClientSettings();
        assertEquals("", settings.getCustomHttpMethods());

        settings.setCustomHttpMethods("PROPFIND, PROPPATCH, MKCOL, COPY, MOVE, LOCK, UNLOCK");
        assertEquals("PROPFIND, PROPPATCH, MKCOL, COPY, MOVE, LOCK, UNLOCK", settings.getCustomHttpMethods());

        HttpClientSettings copy = settings.copy();
        assertEquals(settings, copy);
        assertEquals(settings.hashCode(), copy.hashCode());
        assertTrue(copy.toString().contains("PROPFIND"));
    }

    @Test
    void testHttpClientSettingsManagerPersistence() {
        HttpClientSettingsManager manager = HttpClientSettingsManager.getInstance();
        HttpClientSettings settings = manager.getSettings();
        assertEquals("", settings.getCustomHttpMethods());

        settings.setCustomHttpMethods("PURGE, LINK, UNLINK");
        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addChangeListener(() -> notified.set(true));

        manager.setSettings(settings);
        assertTrue(notified.get());

        manager.load();
        assertEquals("PURGE, LINK, UNLINK", manager.getSettings().getCustomHttpMethods());

        manager.resetDefaults();
        assertEquals("", manager.getSettings().getCustomHttpMethods());
    }

    // ==========================================
    // 5. UI Page Tests (Keymap, MCP, Network, HTTP Client)
    // ==========================================

    @Test
    void testGitHubCopilotKeymapPage() throws Exception {
        runOnFx(() -> {
            SettingsToolsGitHubCopilotKeymapPage page = new SettingsToolsGitHubCopilotKeymapPage();
            assertFalse(page.isModified());

            TableView<GitHubCopilotKeymapEntry> table = page.getTableView();
            assertNotNull(table);
            assertEquals(33, table.getItems().size());

            // Test reset
            page.reset();
            assertFalse(page.isModified());
            assertEquals(33, page.getTableView().getItems().size());

            page.revertChanges();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testGitHubCopilotMcpPage() throws Exception {
        runOnFx(() -> {
            SettingsToolsGitHubCopilotMcpPage page = new SettingsToolsGitHubCopilotMcpPage();
            assertFalse(page.isModified());

            TextField urlField = page.getMcpRegistryBaseUrlField();
            assertNotNull(urlField);
            assertEquals("https://api.mcp.github.com", urlField.getText());

            urlField.setText("https://test.mcp.io");
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertEquals("https://test.mcp.io", GitHubCopilotMcpSettingsManager.getInstance().getSettings().getMcpRegistryBaseUrl());

            urlField.setText("https://modified.mcp.io");
            assertTrue(page.isModified());
            page.revertChanges();
            assertFalse(page.isModified());
            assertEquals("https://test.mcp.io", urlField.getText());

            page.reset();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testGitHubCopilotNetworkPage() throws Exception {
        runOnFx(() -> {
            SettingsToolsGitHubCopilotNetworkPage page = new SettingsToolsGitHubCopilotNetworkPage();
            assertFalse(page.isModified());

            CheckBox customizeCheckbox = page.getCustomizeHttpProxyCheckBox();
            TextField hostField = page.getHostNameField();
            TextField portField = page.getPortNumberField();
            CheckBox authCheckbox = page.getProxyAuthCheckBox();
            TextField loginField = page.getLoginField();
            PasswordField passField = page.getPasswordField();
            ComboBox<String> networkingCombo = page.getNetworkingComboBox();
            TextField kerberosField = page.getKerberosField();

            assertNotNull(customizeCheckbox);
            assertFalse(customizeCheckbox.isSelected());
            assertTrue(hostField.isDisable());
            assertTrue(portField.isDisable());
            assertTrue(authCheckbox.isDisable());
            assertTrue(loginField.isDisable());
            assertTrue(passField.isDisable());

            // Enable proxy
            customizeCheckbox.setSelected(true);
            assertFalse(hostField.isDisable());
            assertFalse(portField.isDisable());
            assertFalse(authCheckbox.isDisable());
            assertTrue(loginField.isDisable());
            assertTrue(passField.isDisable());

            // Enable auth
            authCheckbox.setSelected(true);
            assertFalse(loginField.isDisable());
            assertFalse(passField.isDisable());

            hostField.setText("192.168.1.100");
            portField.setText("8888");
            loginField.setText("proxyuser");
            passField.setText("secretpassword");
            networkingCombo.setValue("Manual");
            kerberosField.setText("HTTP/proxy.local");

            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());

            GitHubCopilotNetworkSettings saved = GitHubCopilotNetworkSettingsManager.getInstance().getSettings();
            assertTrue(saved.isCustomizeHttpProxy());
            assertEquals("192.168.1.100", saved.getHostName());
            assertEquals(8888, saved.getPortNumber());
            assertTrue(saved.isProxyAuthentication());
            assertEquals("proxyuser", saved.getLogin());
            assertEquals("secretpassword", saved.getPassword());
            assertEquals("Manual", saved.getNetworking());
            assertEquals("HTTP/proxy.local", saved.getOverrideKerberosPrincipal());

            // Revert changes
            hostField.setText("invalid.proxy");
            assertTrue(page.isModified());
            page.revertChanges();
            assertFalse(page.isModified());
            assertEquals("192.168.1.100", hostField.getText());

            // Reset
            page.reset();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testHttpClientPage() throws Exception {
        runOnFx(() -> {
            SettingsToolsHttpClientPage page = new SettingsToolsHttpClientPage();
            assertFalse(page.isModified());

            TextField methodsField = page.getCustomHttpMethodsField();
            assertNotNull(methodsField);
            assertEquals("", methodsField.getText());

            methodsField.setText("CUSTOM1, CUSTOM2");
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertEquals("CUSTOM1, CUSTOM2", HttpClientSettingsManager.getInstance().getSettings().getCustomHttpMethods());

            methodsField.setText("CUSTOM3");
            assertTrue(page.isModified());
            page.revertChanges();
            assertFalse(page.isModified());
            assertEquals("CUSTOM1, CUSTOM2", methodsField.getText());

            page.reset();
            assertFalse(page.isModified());
        });
    }

    // ==========================================
    // 6. SettingsDialog Integration Tests
    // ==========================================

    @Test
    void testSettingsDialogIntegration() throws Exception {
        runOnFx(() -> {
            SettingsDialog dialog = new SettingsDialog(null);
            assertNotNull(dialog);
            assertNotNull(dialog.getTree());

            // Find Tools node
            TreeItem<String> toolsNode = null;
            for (TreeItem<String> item : dialog.getTree().getRoot().getChildren()) {
                if ("Tools".equals(item.getValue())) {
                    toolsNode = item;
                    break;
                }
            }
            assertNotNull(toolsNode, "Tools category should exist in tree root");

            TreeItem<String> copilotNode = null;
            TreeItem<String> httpClientNode = null;
            for (TreeItem<String> child : toolsNode.getChildren()) {
                if ("GitHub Copilot".equals(child.getValue())) {
                    copilotNode = child;
                } else if ("HTTP Client".equals(child.getValue())) {
                    httpClientNode = child;
                }
            }
            assertNotNull(copilotNode, "GitHub Copilot should exist under Tools");
            assertNotNull(httpClientNode, "HTTP Client should exist under Tools");

            // Verify Keymap, MCP, and Network under GitHub Copilot
            TreeItem<String> keymapNode = null;
            TreeItem<String> mcpNode = null;
            TreeItem<String> networkNode = null;
            for (TreeItem<String> subChild : copilotNode.getChildren()) {
                if ("Keymap".equals(subChild.getValue())) {
                    keymapNode = subChild;
                } else if ("Model Context Protocol (MCP)".equals(subChild.getValue())) {
                    mcpNode = subChild;
                } else if ("Network".equals(subChild.getValue())) {
                    networkNode = subChild;
                }
            }
            assertNotNull(keymapNode, "Keymap should exist under GitHub Copilot");
            assertNotNull(mcpNode, "Model Context Protocol (MCP) should exist under GitHub Copilot");
            assertNotNull(networkNode, "Network should exist under GitHub Copilot");

            // Test navigation to Keymap page
            dialog.getTree().getSelectionModel().select(keymapNode);
            SettingsToolsGitHubCopilotKeymapPage keymapPage = dialog.getCurrentToolsGitHubCopilotKeymapPage();
            assertNotNull(keymapPage);
            assertEquals(33, keymapPage.getTableView().getItems().size());

            // Test navigation to MCP page
            dialog.getTree().getSelectionModel().select(mcpNode);
            SettingsToolsGitHubCopilotMcpPage mcpPage = dialog.getCurrentToolsGitHubCopilotMcpPage();
            assertNotNull(mcpPage);
            assertEquals("https://api.mcp.github.com", mcpPage.getMcpRegistryBaseUrlField().getText());

            // Test navigation to Network page
            dialog.getTree().getSelectionModel().select(networkNode);
            SettingsToolsGitHubCopilotNetworkPage networkPage = dialog.getCurrentToolsGitHubCopilotNetworkPage();
            assertNotNull(networkPage);
            assertFalse(networkPage.getCustomizeHttpProxyCheckBox().isSelected());

            // Test navigation to HTTP Client page
            dialog.getTree().getSelectionModel().select(httpClientNode);
            SettingsToolsHttpClientPage httpClientPage = dialog.getCurrentToolsHttpClientPage();
            assertNotNull(httpClientPage);
            assertNotNull(httpClientPage.getCustomHttpMethodsField());

            // Test applyAll through dialog
            httpClientPage.getCustomHttpMethodsField().setText("PATCH, REPORT");
            assertTrue(httpClientPage.isModified());
            assertFalse(dialog.getApplyButton().isDisable());

            dialog.applyAll();
            assertFalse(httpClientPage.isModified());
            assertTrue(dialog.getApplyButton().isDisable());
            assertEquals("PATCH, REPORT", HttpClientSettingsManager.getInstance().getSettings().getCustomHttpMethods());
        });
    }

    // ==========================================
    // 7. Strict Brand Isolation Verification
    // ==========================================

    @Test
    void testBrandIsolationInAllCreatedFiles() throws Exception {
        List<String> filesToCheck = List.of(
                "src/main/java/dev/lumina/tools/GitHubCopilotKeymapEntry.java",
                "src/main/java/dev/lumina/tools/GitHubCopilotKeymapSettings.java",
                "src/main/java/dev/lumina/tools/GitHubCopilotKeymapSettingsManager.java",
                "src/main/java/dev/lumina/tools/GitHubCopilotMcpSettings.java",
                "src/main/java/dev/lumina/tools/GitHubCopilotMcpSettingsManager.java",
                "src/main/java/dev/lumina/tools/GitHubCopilotNetworkSettings.java",
                "src/main/java/dev/lumina/tools/GitHubCopilotNetworkSettingsManager.java",
                "src/main/java/dev/lumina/tools/HttpClientSettings.java",
                "src/main/java/dev/lumina/tools/HttpClientSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsToolsGitHubCopilotKeymapPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsGitHubCopilotMcpPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsGitHubCopilotNetworkPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsHttpClientPage.java"
        );

        // Competitor terms (case insensitive)
        List<String> bannedKeywords = List.of(
                "intellij",
                "jetbrains",
                "idea" + "platform"
        );

        for (String filePath : filesToCheck) {
            File f = new File(filePath);
            assertTrue(f.exists(), "File must exist: " + filePath);
            String content = Files.readString(f.toPath()).toLowerCase();
            for (String banned : bannedKeywords) {
                assertFalse(content.contains(banned),
                        "File " + filePath + " contains unauthorized competitor keyword: " + banned);
            }
        }
    }
}
