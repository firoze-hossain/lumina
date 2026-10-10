package dev.lumina.ui;

import dev.lumina.tools.*;
import dev.lumina.util.Settings;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Features Trainer and GitHub Copilot (Overview, General, Chat) settings models,
 * managers, UI pages, SettingsDialog integration, and brand isolation.
 */
public class SettingsToolsCopilotTrainerTest {

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
        FeaturesTrainerSettingsManager.getInstance().clear();
        GitHubCopilotGeneralSettingsManager.getInstance().clear();
        GitHubCopilotChatSettingsManager.getInstance().clear();
        GitHubCopilotSandboxSettingsManager.getInstance().clear();
        GitHubCopilotCompletionsSettingsManager.getInstance().clear();
        GitHubCopilotCustomizationsSettingsManager.getInstance().clear();
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
    // 1. Features Trainer Model & Manager Tests
    // ==========================================

    @Test
    void testFeaturesTrainerSettingsModel() {
        FeaturesTrainerSettings settings = new FeaturesTrainerSettings();
        assertEquals("Java", settings.getMainLanguage());
        assertTrue(settings.isShowNotificationsOnNewLessons());

        settings.setMainLanguage("Kotlin");
        settings.setShowNotificationsOnNewLessons(false);
        assertEquals("Kotlin", settings.getMainLanguage());
        assertFalse(settings.isShowNotificationsOnNewLessons());

        FeaturesTrainerSettings cloned = settings.clone();
        assertEquals(settings, cloned);
        assertEquals(settings.hashCode(), cloned.hashCode());
        assertNotSame(settings, cloned);
        assertTrue(settings.toString().contains("Kotlin"));
    }

    @Test
    void testFeaturesTrainerSettingsManager() {
        FeaturesTrainerSettingsManager manager = FeaturesTrainerSettingsManager.getInstance();
        FeaturesTrainerSettings defaultSettings = manager.getSettings();
        assertEquals("Java", defaultSettings.getMainLanguage());
        assertTrue(defaultSettings.isShowNotificationsOnNewLessons());

        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addListener(listener);

        FeaturesTrainerSettings updated = new FeaturesTrainerSettings();
        updated.setMainLanguage("Python");
        updated.setShowNotificationsOnNewLessons(false);
        manager.setSettings(updated);

        assertTrue(notified.get());
        FeaturesTrainerSettings reloaded = manager.getSettings();
        assertEquals("Python", reloaded.getMainLanguage());
        assertFalse(reloaded.isShowNotificationsOnNewLessons());

        manager.removeListener(listener);
    }

    // ==========================================
    // 2. Features Trainer UI Page Tests
    // ==========================================

    @Test
    void testSettingsToolsFeaturesTrainerPageUI() throws Exception {
        runOnFx(() -> {
            SettingsToolsFeaturesTrainerPage page = new SettingsToolsFeaturesTrainerPage();
            assertFalse(page.isModified());

            assertEquals("Java", page.getLanguageCombo().getValue());
            assertTrue(page.getShowNotificationsCheck().isSelected());

            AtomicBoolean modified = new AtomicBoolean(false);
            page.setOnModifiedListener(() -> modified.set(true));

            page.getLanguageCombo().setValue("Kotlin");
            assertTrue(page.isModified());
            assertTrue(modified.get());

            page.apply();
            assertFalse(page.isModified());
            assertEquals("Kotlin", FeaturesTrainerSettingsManager.getInstance().getSettings().getMainLanguage());

            page.getShowNotificationsCheck().setSelected(false);
            assertTrue(page.isModified());

            page.reset();
            assertFalse(page.isModified());
            assertTrue(page.getShowNotificationsCheck().isSelected());

            page.getLanguageCombo().setValue("Go");
            assertTrue(page.isModified());
            page.revertChanges();
            assertFalse(page.isModified());
            assertEquals("Kotlin", page.getLanguageCombo().getValue());

            page.getResetLessonsButton().fire();
            assertTrue(page.isLessonsResetTriggered());
        });
    }

    // ==========================================
    // 3. GitHub Copilot General Model & Manager Tests
    // ==========================================

    @Test
    void testGitHubCopilotGeneralSettingsModel() {
        GitHubCopilotGeneralSettings settings = new GitHubCopilotGeneralSettings();
        assertFalse(settings.isEnableScreenReaderSupport());
        assertEquals("Stable", settings.getUpdateChannel());
        assertTrue(settings.isCheckForPluginUpdates());
        assertFalse(settings.isPreferDeviceCodeSignIn());
        assertEquals("", settings.getAuthenticationProvider());
        assertTrue(settings.isSendUsageTelemetry());

        settings.setEnableScreenReaderSupport(true);
        settings.setUpdateChannel("Beta");
        settings.setCheckForPluginUpdates(false);
        settings.setPreferDeviceCodeSignIn(true);
        settings.setAuthenticationProvider("https://enterprise.internal/auth");
        settings.setSendUsageTelemetry(true);

        GitHubCopilotGeneralSettings cloned = settings.clone();
        assertEquals(settings, cloned);
        assertEquals(settings.hashCode(), cloned.hashCode());
        assertNotSame(settings, cloned);
        assertTrue(settings.toString().contains("enterprise.internal"));
    }

    @Test
    void testGitHubCopilotGeneralSettingsManager() {
        GitHubCopilotGeneralSettingsManager manager = GitHubCopilotGeneralSettingsManager.getInstance();
        GitHubCopilotGeneralSettings initial = manager.getSettings();
        assertFalse(initial.isEnableScreenReaderSupport());

        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addListener(listener);

        GitHubCopilotGeneralSettings custom = new GitHubCopilotGeneralSettings();
        custom.setEnableScreenReaderSupport(true);
        custom.setUpdateChannel("Preview");
        custom.setCheckForPluginUpdates(true);
        custom.setPreferDeviceCodeSignIn(true);
        custom.setAuthenticationProvider("https://copilot.company.org");
        custom.setSendUsageTelemetry(true);
        manager.setSettings(custom);

        assertTrue(notified.get());
        GitHubCopilotGeneralSettings reloaded = manager.getSettings();
        assertTrue(reloaded.isEnableScreenReaderSupport());
        assertEquals("Preview", reloaded.getUpdateChannel());
        assertEquals("https://copilot.company.org", reloaded.getAuthenticationProvider());
        assertTrue(reloaded.isSendUsageTelemetry());

        manager.removeListener(listener);
    }

    // ==========================================
    // 4. GitHub Copilot General UI Page Tests
    // ==========================================

    @Test
    void testSettingsToolsGitHubCopilotGeneralPageUI() throws Exception {
        runOnFx(() -> {
            SettingsToolsGitHubCopilotGeneralPage page = new SettingsToolsGitHubCopilotGeneralPage();
            assertFalse(page.isModified());

            assertFalse(page.getScreenReaderCheck().isSelected());
            assertEquals("Stable", page.getUpdateChannelCombo().getValue());
            assertTrue(page.getCheckUpdatesCheck().isSelected());
            assertFalse(page.getPreferDeviceCodeCheck().isSelected());
            assertEquals("", page.getAuthProviderField().getText());
            assertTrue(page.getSendTelemetryCheck().isSelected());

            AtomicBoolean modified = new AtomicBoolean(false);
            page.setOnModifiedListener(() -> modified.set(true));

            page.getScreenReaderCheck().setSelected(true);
            assertTrue(page.isModified());
            assertTrue(modified.get());

            page.getAuthProviderField().setText("https://lumina.copilot.net");
            page.apply();
            assertFalse(page.isModified());
            assertEquals("https://lumina.copilot.net", GitHubCopilotGeneralSettingsManager.getInstance().getSettings().getAuthenticationProvider());
            assertTrue(GitHubCopilotGeneralSettingsManager.getInstance().getSettings().isEnableScreenReaderSupport());

            page.getUpdateChannelCombo().setValue("Beta");
            assertTrue(page.isModified());
            page.revertChanges();
            assertFalse(page.isModified());
            assertEquals("Stable", page.getUpdateChannelCombo().getValue());
        });
    }

    // ==========================================
    // 5. GitHub Copilot Overview Page Tests
    // ==========================================

    @Test
    void testSettingsToolsGitHubCopilotOverviewPage() throws Exception {
        runOnFx(() -> {
            AtomicReference<String> navigatedCat = new AtomicReference<>();
            SettingsToolsGitHubCopilotOverviewPage page = new SettingsToolsGitHubCopilotOverviewPage(navigatedCat::set);

            assertEquals(8, SettingsToolsGitHubCopilotOverviewPage.CATEGORIES.size());
            assertTrue(SettingsToolsGitHubCopilotOverviewPage.CATEGORIES.contains("General"));
            assertTrue(SettingsToolsGitHubCopilotOverviewPage.CATEGORIES.contains("Chat"));
            assertTrue(SettingsToolsGitHubCopilotOverviewPage.CATEGORIES.contains("Sandbox"));
            assertTrue(SettingsToolsGitHubCopilotOverviewPage.CATEGORIES.contains("Completions"));
            assertTrue(SettingsToolsGitHubCopilotOverviewPage.CATEGORIES.contains("Customizations"));
            assertTrue(SettingsToolsGitHubCopilotOverviewPage.CATEGORIES.contains("Keymap"));
            assertTrue(SettingsToolsGitHubCopilotOverviewPage.CATEGORIES.contains("Model Context Protocol (MCP)"));
            assertTrue(SettingsToolsGitHubCopilotOverviewPage.CATEGORIES.contains("Network"));
        });
    }

    // ==========================================
    // 6. TerminalAutoApproveRule & Copilot Chat Model / Manager Tests
    // ==========================================

    @Test
    void testTerminalAutoApproveRuleModel() {
        TerminalAutoApproveRule rule = new TerminalAutoApproveRule("git *", true);
        assertEquals("git *", rule.getPattern());
        assertTrue(rule.isAutoApprove());

        TerminalAutoApproveRule cloned = rule.clone();
        assertEquals(rule, cloned);
        assertEquals(rule.hashCode(), cloned.hashCode());
        assertNotSame(rule, cloned);

        rule.setPattern("mvn *");
        rule.setAutoApprove(false);
        assertEquals("mvn *", rule.getPattern());
        assertFalse(rule.isAutoApprove());
        assertNotEquals(rule, cloned);
        assertTrue(rule.toString().contains("mvn *"));
    }

    @Test
    void testGitHubCopilotChatSettingsModel() {
        GitHubCopilotChatSettings settings = new GitHubCopilotChatSettings();
        assertTrue(settings.isEnableAutoModel());
        assertEquals("English", settings.getNaturalLanguage());
        assertEquals("Copilot (Modern Inline)", settings.getDiffViewMode());
        assertEquals(0, settings.getAutoAcceptDelay());
        assertTrue(settings.isShowInlineChatGutterIcon());
        assertFalse(settings.isEnableSemanticSearch());

        assertTrue(settings.isEnableAgentMode());
        assertEquals(50, settings.getAgentMaxRequests());
        assertEquals(1024, settings.getAnthropicThinkingBudgetTokens());
        assertTrue(settings.isEnableCustomAgent());
        assertTrue(settings.isEnableOrganizationCustomAgents());
        assertTrue(settings.isEnableSubagent());
        assertFalse(settings.isEnableCloudAgent());

        assertFalse(settings.isEnableClaudeCodeCliPreview());
        assertEquals("", settings.getClaudeCodeCliPath());
        assertFalse(settings.isEnableCodexCliPreview());
        assertEquals("", settings.getCodexCliPath());

        assertFalse(settings.getTerminalAutoApproveRules().isEmpty());
        assertFalse(settings.isAutoApproveUncoveredCommands());

        GitHubCopilotChatSettings cloned = settings.clone();
        assertEquals(settings, cloned);
        assertEquals(settings.hashCode(), cloned.hashCode());
        assertNotSame(settings, cloned);
    }

    @Test
    void testGitHubCopilotChatSettingsManager() {
        GitHubCopilotChatSettingsManager manager = GitHubCopilotChatSettingsManager.getInstance();
        GitHubCopilotChatSettings initial = manager.getSettings();
        assertTrue(initial.isEnableAutoModel());
        assertEquals("English", initial.getNaturalLanguage());

        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addListener(listener);

        GitHubCopilotChatSettings updated = initial.clone();
        updated.setNaturalLanguage("Chinese");
        updated.setAutoAcceptDelay(10);
        updated.setEnableCloudAgent(true);
        updated.getTerminalAutoApproveRules().add(new TerminalAutoApproveRule("gradle *", true));
        manager.setSettings(updated);

        assertTrue(notified.get());
        GitHubCopilotChatSettings reloaded = manager.getSettings();
        assertEquals("Chinese", reloaded.getNaturalLanguage());
        assertEquals(10, reloaded.getAutoAcceptDelay());
        assertTrue(reloaded.isEnableCloudAgent());
        assertTrue(reloaded.getTerminalAutoApproveRules().stream().anyMatch(r -> "gradle *".equals(r.getPattern())));

        manager.removeListener(listener);
    }

    // ==========================================
    // 7. GitHub Copilot Chat UI Page Tests
    // ==========================================

    @Test
    void testSettingsToolsGitHubCopilotChatPageUI() throws Exception {
        runOnFx(() -> {
            SettingsToolsGitHubCopilotChatPage page = new SettingsToolsGitHubCopilotChatPage();
            assertFalse(page.isModified());

            assertTrue(page.getAutoModelCheck().isSelected());
            assertEquals("English", page.getNaturalLangCombo().getValue());
            assertEquals("Copilot (Modern Inline)", page.getDiffViewModeCombo().getValue());
            assertEquals("0", page.getAutoAcceptDelayField().getText());
            assertTrue(page.getShowInlineGutterCheck().isSelected());
            assertFalse(page.getSemanticSearchCheck().isSelected());

            assertTrue(page.getAgentModeCheck().isSelected());
            assertEquals("50", page.getAgentMaxReqField().getText());
            assertEquals("1024", page.getAnthropicTokensField().getText());
            assertTrue(page.getCustomAgentCheck().isSelected());
            assertTrue(page.getOrgCustomAgentsCheck().isSelected());
            assertTrue(page.getSubagentCheck().isSelected());
            assertFalse(page.getCloudAgentCheck().isSelected());

            AtomicBoolean modified = new AtomicBoolean(false);
            page.setOnModifiedListener(() -> modified.set(true));

            page.getAutoModelCheck().setSelected(false);
            assertTrue(page.isModified());
            assertTrue(modified.get());

            page.apply();
            assertFalse(page.isModified());
            assertFalse(GitHubCopilotChatSettingsManager.getInstance().getSettings().isEnableAutoModel());

            page.getNaturalLangCombo().setValue("German");
            page.getAutoAcceptDelayField().setText("7");
            page.getSubagentCheck().setSelected(false);
            assertTrue(page.isModified());

            page.revertChanges();
            assertFalse(page.isModified());
            assertEquals("English", page.getNaturalLangCombo().getValue());
            assertEquals("0", page.getAutoAcceptDelayField().getText());
            assertTrue(page.getSubagentCheck().isSelected());

            // Add rule test
            int initialCount = page.getTerminalRulesData().size();
            page.getTerminalRulesData().add(new TerminalAutoApproveRule("custom-tool *", true));
            assertTrue(page.isModified());
            assertEquals(initialCount + 1, page.getTerminalRulesData().size());

            page.apply();
            assertFalse(page.isModified());
            GitHubCopilotChatSettings saved = GitHubCopilotChatSettingsManager.getInstance().getSettings();
            assertTrue(saved.getTerminalAutoApproveRules().stream().anyMatch(r -> "custom-tool *".equals(r.getPattern())));

            // Reset rules test
            page.getResetTerminalRulesBtn().fire();
            assertTrue(page.isModified());
            page.apply();
            assertFalse(page.isModified());
        });
    }

    // ==========================================
    // 8. SettingsDialog Integration Tests
    // ==========================================

    @Test
    void testSettingsDialogToolsNavigationAndLifecycle() throws Exception {
        runOnFx(() -> {
            SettingsDialog dialog = new SettingsDialog(null);
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

            // Verify Features Trainer and GitHub Copilot presence in tree
            TreeItem<String> featuresTrainerNode = null;
            TreeItem<String> copilotNode = null;
            for (TreeItem<String> child : toolsNode.getChildren()) {
                if ("Features Trainer".equals(child.getValue())) {
                    featuresTrainerNode = child;
                } else if ("GitHub Copilot".equals(child.getValue())) {
                    copilotNode = child;
                }
            }
            assertNotNull(featuresTrainerNode, "Features Trainer should exist under Tools");
            assertNotNull(copilotNode, "GitHub Copilot should exist under Tools");

            // Verify Copilot subcategories in tree
            assertEquals(8, copilotNode.getChildren().size());
            assertTrue(copilotNode.getChildren().stream().anyMatch(c -> "General".equals(c.getValue())));
            assertTrue(copilotNode.getChildren().stream().anyMatch(c -> "Chat".equals(c.getValue())));
            assertTrue(copilotNode.getChildren().stream().anyMatch(c -> "Sandbox".equals(c.getValue())));
            assertTrue(copilotNode.getChildren().stream().anyMatch(c -> "Completions".equals(c.getValue())));
            assertTrue(copilotNode.getChildren().stream().anyMatch(c -> "Customizations".equals(c.getValue())));
            assertTrue(copilotNode.getChildren().stream().anyMatch(c -> "Keymap".equals(c.getValue())));
            assertTrue(copilotNode.getChildren().stream().anyMatch(c -> "Model Context Protocol (MCP)".equals(c.getValue())));
            assertTrue(copilotNode.getChildren().stream().anyMatch(c -> "Network".equals(c.getValue())));

            // Select Features Trainer
            dialog.getTree().getSelectionModel().select(featuresTrainerNode);
            assertNotNull(dialog.getCurrentToolsFeaturesTrainerPage());

            // Select GitHub Copilot parent
            dialog.getTree().getSelectionModel().select(copilotNode);
            assertNotNull(dialog.getCurrentToolsGitHubCopilotOverviewPage());

            // Select General under Copilot
            TreeItem<String> generalNode = copilotNode.getChildren().stream().filter(c -> "General".equals(c.getValue())).findFirst().orElse(null);
            assertNotNull(generalNode);
            dialog.getTree().getSelectionModel().select(generalNode);
            assertNotNull(dialog.getCurrentToolsGitHubCopilotGeneralPage());

            // Select Chat under Copilot
            TreeItem<String> chatNode = copilotNode.getChildren().stream().filter(c -> "Chat".equals(c.getValue())).findFirst().orElse(null);
            assertNotNull(chatNode);
            dialog.getTree().getSelectionModel().select(chatNode);
            assertNotNull(dialog.getCurrentToolsGitHubCopilotChatPage());

            // Test dirty state and apply via dialog
            dialog.getCurrentToolsGitHubCopilotChatPage().getNaturalLangCombo().setValue("Spanish");
            assertFalse(dialog.getApplyButton().isDisable());
            assertTrue(dialog.getCurrentToolsGitHubCopilotChatPage().isModified());

            dialog.applyAll();
            assertTrue(dialog.getApplyButton().isDisable());
            assertFalse(dialog.getCurrentToolsGitHubCopilotChatPage().isModified());
            assertEquals("Spanish", GitHubCopilotChatSettingsManager.getInstance().getSettings().getNaturalLanguage());

            // Reset via page
            dialog.getCurrentToolsGitHubCopilotChatPage().getNaturalLangCombo().setValue("French");
            assertFalse(dialog.getApplyButton().isDisable());
            assertTrue(dialog.getCurrentToolsGitHubCopilotChatPage().isModified());
            dialog.getCurrentToolsGitHubCopilotChatPage().reset();
            assertTrue(dialog.getApplyButton().isDisable());
            assertFalse(dialog.getCurrentToolsGitHubCopilotChatPage().isModified());
            assertEquals("Spanish", dialog.getCurrentToolsGitHubCopilotChatPage().getNaturalLangCombo().getValue());

            // --- Sandbox Navigation & Lifecycle ---
            dialog.selectCategory("GitHub Copilot", "Sandbox");
            assertNotNull(dialog.getCurrentToolsGitHubCopilotSandboxPage());
            dialog.getCurrentToolsGitHubCopilotSandboxPage().getEnableLocalSandboxCheck().setSelected(true);
            assertFalse(dialog.getApplyButton().isDisable());
            assertTrue(dialog.getCurrentToolsGitHubCopilotSandboxPage().isModified());
            dialog.getApplyButton().fire();
            assertTrue(dialog.getApplyButton().isDisable());
            assertFalse(dialog.getCurrentToolsGitHubCopilotSandboxPage().isModified());
            assertTrue(GitHubCopilotSandboxSettingsManager.getInstance().getSettings().isEnableLocalSandbox());

            // --- Completions Navigation & Lifecycle ---
            dialog.selectCategory("GitHub Copilot", "Completions");
            assertNotNull(dialog.getCurrentToolsGitHubCopilotCompletionsPage());
            dialog.getCurrentToolsGitHubCopilotCompletionsPage().getModelCombo().setValue("Claude 3.5 Sonnet");
            assertFalse(dialog.getApplyButton().isDisable());
            assertTrue(dialog.getCurrentToolsGitHubCopilotCompletionsPage().isModified());
            dialog.getApplyButton().fire();
            assertTrue(dialog.getApplyButton().isDisable());
            assertFalse(dialog.getCurrentToolsGitHubCopilotCompletionsPage().isModified());
            assertEquals("Claude 3.5 Sonnet", GitHubCopilotCompletionsSettingsManager.getInstance().getSettings().getModelForCompletions());

            // --- Customizations Navigation & Lifecycle ---
            dialog.selectCategory("GitHub Copilot", "Customizations");
            assertNotNull(dialog.getCurrentToolsGitHubCopilotCustomizationsPage());
            dialog.getCurrentToolsGitHubCopilotCustomizationsPage().getUseNestedAgentsMdCheck().setSelected(true);
            assertFalse(dialog.getApplyButton().isDisable());
            assertTrue(dialog.getCurrentToolsGitHubCopilotCustomizationsPage().isModified());
            dialog.getApplyButton().fire();
            assertTrue(dialog.getApplyButton().isDisable());
            assertFalse(dialog.getCurrentToolsGitHubCopilotCustomizationsPage().isModified());
            assertTrue(GitHubCopilotCustomizationsSettingsManager.getInstance().getSettings().isUseNestedAgentsMd());
        });
    }

    // ==========================================
    // 9. Sandbox Model, Manager & UI Tests
    // ==========================================

    @Test
    void testSandboxModelAndManager() {
        GitHubCopilotSandboxSettingsManager manager = GitHubCopilotSandboxSettingsManager.getInstance();
        GitHubCopilotSandboxSettings defaultSettings = manager.getSettings();

        assertFalse(defaultSettings.isEnableLocalSandbox());
        assertTrue(defaultSettings.isFilesystemIncludeWorkingDirectory());
        assertTrue(defaultSettings.isFilesystemClearPolicyOnExit());
        assertTrue(defaultSettings.getFilesystemPermissions().isEmpty());
        assertTrue(defaultSettings.isNetworkAllowOutbound());
        assertTrue(defaultSettings.isNetworkAllowLocalNetwork());
        assertTrue(defaultSettings.getNetworkHostAccess().isEmpty());

        GitHubCopilotSandboxSettings modified = defaultSettings.clone();
        modified.setEnableLocalSandbox(true);
        modified.setFilesystemIncludeWorkingDirectory(false);
        modified.setFilesystemClearPolicyOnExit(false);
        modified.getFilesystemPermissions().add(new SandboxPathPermission("/var/log", "Read"));
        modified.setNetworkAllowOutbound(false);
        modified.setNetworkAllowLocalNetwork(false);
        modified.getNetworkHostAccess().add(new SandboxHostAccess("api.github.com", "Allow"));

        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addListener(listener);

        manager.save(modified);
        assertTrue(notified.get());

        GitHubCopilotSandboxSettings loaded = manager.getSettings();
        assertEquals(modified, loaded);
        assertEquals(1, loaded.getFilesystemPermissions().size());
        assertEquals("/var/log", loaded.getFilesystemPermissions().get(0).getPath());
        assertEquals("Read", loaded.getFilesystemPermissions().get(0).getPermission());
        assertEquals(1, loaded.getNetworkHostAccess().size());
        assertEquals("api.github.com", loaded.getNetworkHostAccess().get(0).getHost());
        assertEquals("Allow", loaded.getNetworkHostAccess().get(0).getAccess());

        manager.removeListener(listener);
    }

    @Test
    void testSettingsToolsGitHubCopilotSandboxPage() throws Exception {
        runOnFx(() -> {
            SettingsToolsGitHubCopilotSandboxPage page = new SettingsToolsGitHubCopilotSandboxPage();
            assertFalse(page.isModified());

            // Toggle enable
            page.getEnableLocalSandboxCheck().setSelected(true);
            assertTrue(page.isModified());

            // Add FS permission
            int initialFsCount = page.getFsPermissionsData().size();
            page.getAddFsPermBtn().fire();
            assertEquals(initialFsCount + 1, page.getFsPermissionsData().size());
            page.getFsPermissionsData().get(0).setPath("/tmp/test");
            page.getFsPermissionsData().get(0).setPermission("Read/Write");

            // Add Host access
            int initialNetCount = page.getNetHostAccessData().size();
            page.getAddNetHostBtn().fire();
            assertEquals(initialNetCount + 1, page.getNetHostAccessData().size());
            page.getNetHostAccessData().get(0).setHost("example.com");
            page.getNetHostAccessData().get(0).setAccess("Block");

            page.apply();
            assertFalse(page.isModified());

            GitHubCopilotSandboxSettings saved = GitHubCopilotSandboxSettingsManager.getInstance().getSettings();
            assertTrue(saved.isEnableLocalSandbox());
            assertEquals(1, saved.getFilesystemPermissions().size());
            assertEquals("/tmp/test", saved.getFilesystemPermissions().get(0).getPath());
            assertEquals("Read/Write", saved.getFilesystemPermissions().get(0).getPermission());
            assertEquals(1, saved.getNetworkHostAccess().size());
            assertEquals("example.com", saved.getNetworkHostAccess().get(0).getHost());
            assertEquals("Block", saved.getNetworkHostAccess().get(0).getAccess());

            // Revert changes test
            page.getEnableLocalSandboxCheck().setSelected(false);
            assertTrue(page.isModified());
            page.revertChanges();
            assertFalse(page.isModified());
            assertTrue(page.getEnableLocalSandboxCheck().isSelected());
        });
    }

    // ==========================================
    // 10. Completions Model, Manager & UI Tests
    // ==========================================

    @Test
    void testCompletionsModelAndManager() {
        GitHubCopilotCompletionsSettingsManager manager = GitHubCopilotCompletionsSettingsManager.getInstance();
        GitHubCopilotCompletionsSettings defaultSettings = manager.getSettings();

        assertTrue(defaultSettings.isEnableCopilotCompletions());
        assertTrue(defaultSettings.isEnableNextEditSuggestions());
        assertFalse(defaultSettings.isShowIdeCompletionsSideBySide());
        assertTrue(defaultSettings.isShowMultipleSuggestionsInToolWindow());
        assertFalse(defaultSettings.isColorForCompletions());
        assertEquals("#6C707E", defaultSettings.getCompletionColorRgb());
        assertEquals("GPT-4.1 Copilot", defaultSettings.getModelForCompletions());
        assertTrue(defaultSettings.getEnabledLanguages().size() > 20);
        assertTrue(defaultSettings.isLanguageEnabled("Java"));

        GitHubCopilotCompletionsSettings modified = defaultSettings.clone();
        modified.setEnableCopilotCompletions(false);
        modified.setEnableNextEditSuggestions(false);
        modified.setShowIdeCompletionsSideBySide(true);
        modified.setColorForCompletions(true);
        modified.setCompletionColorRgb("#FF5555");
        modified.setModelForCompletions("o3-mini");
        modified.setLanguageEnabled("Java", false);

        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addListener(listener);

        manager.save(modified);
        assertTrue(notified.get());

        GitHubCopilotCompletionsSettings loaded = manager.getSettings();
        assertEquals(modified, loaded);
        assertFalse(loaded.isEnableCopilotCompletions());
        assertFalse(loaded.isEnableNextEditSuggestions());
        assertTrue(loaded.isShowIdeCompletionsSideBySide());
        assertTrue(loaded.isColorForCompletions());
        assertEquals("#FF5555", loaded.getCompletionColorRgb());
        assertEquals("o3-mini", loaded.getModelForCompletions());
        assertFalse(loaded.isLanguageEnabled("Java"));

        manager.removeListener(listener);
    }

    @Test
    void testSettingsToolsGitHubCopilotCompletionsPage() throws Exception {
        runOnFx(() -> {
            SettingsToolsGitHubCopilotCompletionsPage page = new SettingsToolsGitHubCopilotCompletionsPage();
            assertFalse(page.isModified());

            page.getEnableCopilotCompletionsCheck().setSelected(false);
            assertTrue(page.isModified());

            page.getModelCombo().setValue("GPT-4o");
            assertTrue(page.isModified());

            // Toggle language
            Optional<SettingsToolsGitHubCopilotCompletionsPage.LanguageEntry> javaEntry = page.getLanguagesData().stream()
                    .filter(l -> "Java".equals(l.getName()))
                    .findFirst();
            assertTrue(javaEntry.isPresent());
            javaEntry.get().setEnabled(false);
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());

            GitHubCopilotCompletionsSettings saved = GitHubCopilotCompletionsSettingsManager.getInstance().getSettings();
            assertFalse(saved.isEnableCopilotCompletions());
            assertEquals("GPT-4o", saved.getModelForCompletions());
            assertFalse(saved.isLanguageEnabled("Java"));

            // Reset test
            javaEntry.get().setEnabled(true);
            assertTrue(page.isModified());
            page.reset();
            assertFalse(page.isModified());
            Optional<SettingsToolsGitHubCopilotCompletionsPage.LanguageEntry> reloadedJava = page.getLanguagesData().stream()
                    .filter(l -> "Java".equals(l.getName()))
                    .findFirst();
            assertTrue(reloadedJava.isPresent());
            assertFalse(reloadedJava.get().isEnabled());
        });
    }

    // ==========================================
    // 11. Customizations Model, Manager & UI Tests
    // ==========================================

    @Test
    void testCustomizationsModelAndManager() {
        GitHubCopilotCustomizationsSettingsManager manager = GitHubCopilotCustomizationsSettingsManager.getInstance();
        GitHubCopilotCustomizationsSettings defaultSettings = manager.getSettings();

        assertEquals(2, defaultSettings.getInstructionLocations().size());
        assertEquals(".github/instructions", defaultSettings.getInstructionLocations().get(0).getPath());
        assertTrue(defaultSettings.getInstructionLocations().get(0).isEnabled());
        assertEquals("~/.copilot/instructions", defaultSettings.getInstructionLocations().get(1).getPath());
        assertFalse(defaultSettings.getInstructionLocations().get(1).isEnabled());

        assertTrue(defaultSettings.isUseAgentsMd());
        assertFalse(defaultSettings.isUseNestedAgentsMd());
        assertTrue(defaultSettings.isUseClaudeMd());
        assertFalse(defaultSettings.isUseNestedClaudeMd());

        assertEquals(2, defaultSettings.getPromptLocations().size());
        assertEquals(3, defaultSettings.getAgentLocations().size());
        assertTrue(defaultSettings.getPluginMarketplaces().isEmpty());

        GitHubCopilotCustomizationsSettings modified = defaultSettings.clone();
        modified.setUseAgentsMd(false);
        modified.setUseNestedAgentsMd(true);
        modified.setUseClaudeMd(false);
        modified.setUseNestedClaudeMd(true);
        modified.getInstructionLocations().add(new CustomizationLocationEntry(".my/instructions", true));
        modified.getPromptLocations().add(new CustomizationLocationEntry(".my/prompts", true));
        modified.getAgentLocations().add(new CustomizationLocationEntry(".my/agents", true));
        modified.getPluginMarketplaces().add("org/lumina-plugin");

        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addListener(listener);

        manager.save(modified);
        assertTrue(notified.get());

        GitHubCopilotCustomizationsSettings loaded = manager.getSettings();
        assertEquals(modified, loaded);
        assertFalse(loaded.isUseAgentsMd());
        assertTrue(loaded.isUseNestedAgentsMd());
        assertFalse(loaded.isUseClaudeMd());
        assertTrue(loaded.isUseNestedClaudeMd());
        assertEquals(3, loaded.getInstructionLocations().size());
        assertEquals(3, loaded.getPromptLocations().size());
        assertEquals(4, loaded.getAgentLocations().size());
        assertEquals(1, loaded.getPluginMarketplaces().size());
        assertEquals("org/lumina-plugin", loaded.getPluginMarketplaces().get(0));

        manager.removeListener(listener);
    }

    @Test
    void testSettingsToolsGitHubCopilotCustomizationsPage() throws Exception {
        runOnFx(() -> {
            SettingsToolsGitHubCopilotCustomizationsPage page = new SettingsToolsGitHubCopilotCustomizationsPage();
            assertFalse(page.isModified());

            // Add instruction entry
            int initialInstrCount = page.getInstructionLocationsData().size();
            page.getAddInstrBtn().fire();
            assertEquals(initialInstrCount + 1, page.getInstructionLocationsData().size());
            page.getInstructionLocationsData().get(initialInstrCount).setPath(".test/instructions");
            assertTrue(page.isModified());

            // Toggle AGENTS.md
            page.getUseNestedAgentsMdCheck().setSelected(true);
            assertTrue(page.isModified());

            // Add Plugin entry
            int initialPluginCount = page.getPluginMarketplacesData().size();
            page.getAddPluginBtn().fire();
            assertEquals(initialPluginCount + 1, page.getPluginMarketplacesData().size());
            page.getPluginMarketplacesData().get(0).setUrl("github/copilot-plugins");
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());

            GitHubCopilotCustomizationsSettings saved = GitHubCopilotCustomizationsSettingsManager.getInstance().getSettings();
            assertTrue(saved.isUseNestedAgentsMd());
            assertTrue(saved.getInstructionLocations().stream().anyMatch(e -> ".test/instructions".equals(e.getPath())));
            assertEquals(1, saved.getPluginMarketplaces().size());
            assertEquals("github/copilot-plugins", saved.getPluginMarketplaces().get(0));

            // Reset instructions test
            page.getResetInstrBtn().fire();
            assertTrue(page.isModified());
            assertEquals(2, page.getInstructionLocationsData().size());
            page.apply();
            assertFalse(page.isModified());
        });
    }

    // ==========================================
    // 12. Brand Isolation Audit
    // ==========================================

    @Test
    void testBrandIsolationInNewCode() throws Exception {
        String[] filesToCheck = {
                "src/main/java/dev/lumina/tools/FeaturesTrainerSettings.java",
                "src/main/java/dev/lumina/tools/FeaturesTrainerSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsToolsFeaturesTrainerPage.java",
                "src/main/java/dev/lumina/tools/GitHubCopilotGeneralSettings.java",
                "src/main/java/dev/lumina/tools/GitHubCopilotGeneralSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsToolsGitHubCopilotOverviewPage.java",
                "src/main/java/dev/lumina/tools/TerminalAutoApproveRule.java",
                "src/main/java/dev/lumina/tools/FileEditAutoApproveRule.java",
                "src/main/java/dev/lumina/tools/TelemetryResourceAttribute.java",
                "src/main/java/dev/lumina/tools/GitHubCopilotChatSettings.java",
                "src/main/java/dev/lumina/tools/GitHubCopilotChatSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsToolsGitHubCopilotChatPage.java",
                "src/main/java/dev/lumina/tools/SandboxPathPermission.java",
                "src/main/java/dev/lumina/tools/SandboxHostAccess.java",
                "src/main/java/dev/lumina/tools/GitHubCopilotSandboxSettings.java",
                "src/main/java/dev/lumina/tools/GitHubCopilotSandboxSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsToolsGitHubCopilotSandboxPage.java",
                "src/main/java/dev/lumina/tools/GitHubCopilotCompletionsSettings.java",
                "src/main/java/dev/lumina/tools/GitHubCopilotCompletionsSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsToolsGitHubCopilotCompletionsPage.java",
                "src/main/java/dev/lumina/tools/CustomizationLocationEntry.java",
                "src/main/java/dev/lumina/tools/GitHubCopilotCustomizationsSettings.java",
                "src/main/java/dev/lumina/tools/GitHubCopilotCustomizationsSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsToolsGitHubCopilotCustomizationsPage.java"
        };

        String[] forbiddenBrands = {"intellij", "jetbrains", "webstorm", "idea"};

        for (String filePath : filesToCheck) {
            File f = new File(filePath);
            assertTrue(f.exists(), "File should exist: " + filePath);
            String content = Files.readString(f.toPath()).toLowerCase();
            for (String brand : forbiddenBrands) {
                // Ignore general words like 'ideal' if 'idea' is checked
                if ("idea".equals(brand)) {
                    // Check word boundary or exact occurrence
                    boolean containsBrand = content.matches("(?s).*\\bidea\\b.*");
                    assertFalse(containsBrand, "File " + filePath + " must NOT contain competitor brand '" + brand + "'");
                } else {
                    assertFalse(content.contains(brand), "File " + filePath + " must NOT contain competitor brand '" + brand + "'");
                }
            }
        }
    }
}
