package dev.lumina.ui;

import dev.lumina.tools.*;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JPA Entity Declaration, JPA Reverse Engineering, Jupyter Overview,
 * Jupyter General, Jupyter Servers, and Jupyter VCS models, managers, UI pages,
 * SettingsDialog integration, and brand isolation.
 */
public class SettingsToolsJpaAndJupyterTest {

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
        JpaEntityDeclarationSettingsManager.getInstance().clear();
        JpaReverseEngineeringSettingsManager.getInstance().clear();
        JupyterGeneralSettingsManager.getInstance().clear();
        JupyterServersSettingsManager.getInstance().clear();
        JupyterVcsSettingsManager.getInstance().clear();
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
    // 1. JPA Entity Declaration Model & Manager
    // ==========================================

    @Test
    void testJpaNameTemplateEntry() {
        JpaNameTemplateEntry entry = new JpaNameTemplateEntry(true, "Table", "Lower", "tbl_", "", true, false);
        assertTrue(entry.isEnabled());
        assertEquals("Table", entry.getTarget());
        assertEquals("Lower", entry.getCaseFormat());
        assertEquals("tbl_", entry.getPrefix());
        assertEquals("", entry.getPostfix());
        assertTrue(entry.isUnderscore());
        assertFalse(entry.isPluralize());

        JpaNameTemplateEntry copy = entry.copy();
        assertEquals(entry, copy);
        assertEquals(entry.hashCode(), copy.hashCode());
        assertTrue(copy.toString().contains("Table"));
    }

    @Test
    void testJpaEntityDeclarationSettingsModel() {
        JpaEntityDeclarationSettings settings = new JpaEntityDeclarationSettings();
        assertFalse(settings.isGenerateJpaAnnotationsOnGetterMethod());
        assertFalse(settings.isGenerateSerialVersionUidField());
        assertFalse(settings.isRegisterHibernateCustomTypesOnEntity());
        assertFalse(settings.isUseFetchTypeLazy());
        assertFalse(settings.isGenerateReturnThisInAttributeSetters());
        assertEquals("Always Ask", settings.getScaffoldingLanguage());
        assertEquals("Private", settings.getDefaultEntityAttributeAccessModifier());
        assertEquals(3, settings.getNameTemplates().size());
        assertEquals("lower", settings.getIndexConstraintNamesCase());
        assertTrue(settings.isLombokGenerateGetterAndSetter());
        assertFalse(settings.isLombokGenerateBuilder());
        assertFalse(settings.isGenerateConstantsForNewObjectNames());

        settings.setGenerateJpaAnnotationsOnGetterMethod(true);
        settings.setScaffoldingLanguage("Kotlin");
        settings.setDefaultEntityAttributeAccessModifier("Protected");
        settings.setLombokGenerateBuilder(true);
        settings.setGenerateConstantsForNewObjectNames(true);

        assertTrue(settings.isGenerateJpaAnnotationsOnGetterMethod());
        assertEquals("Kotlin", settings.getScaffoldingLanguage());
        assertEquals("Protected", settings.getDefaultEntityAttributeAccessModifier());
        assertTrue(settings.isLombokGenerateBuilder());
        assertTrue(settings.isGenerateConstantsForNewObjectNames());

        JpaEntityDeclarationSettings clone = settings.copy();
        assertEquals(settings, clone);
        assertEquals(settings.hashCode(), clone.hashCode());
    }

    @Test
    void testJpaEntityDeclarationSettingsManagerPersistence() {
        JpaEntityDeclarationSettingsManager manager = JpaEntityDeclarationSettingsManager.getInstance();
        JpaEntityDeclarationSettings settings = manager.getSettings();
        assertFalse(settings.isGenerateJpaAnnotationsOnGetterMethod());

        settings.setGenerateJpaAnnotationsOnGetterMethod(true);
        settings.setScaffoldingLanguage("Java");
        settings.setLombokGenerateNoArgsConstructor(true);

        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addChangeListener(() -> notified.set(true));

        manager.setSettings(settings);
        assertTrue(notified.get());

        manager.load();
        JpaEntityDeclarationSettings loaded = manager.getSettings();
        assertTrue(loaded.isGenerateJpaAnnotationsOnGetterMethod());
        assertEquals("Java", loaded.getScaffoldingLanguage());
        assertTrue(loaded.isLombokGenerateNoArgsConstructor());

        manager.resetDefaults();
        assertFalse(manager.getSettings().isGenerateJpaAnnotationsOnGetterMethod());
        assertEquals("Always Ask", manager.getSettings().getScaffoldingLanguage());
    }

    // ==========================================
    // 2. JPA Reverse Engineering Model & Manager
    // ==========================================

    @Test
    void testJpaTypeMappingEntry() {
        JpaTypeMappingEntry entry = new JpaTypeMappingEntry("mysql", "TINYINT(1)", "java.lang.Boolean");
        assertEquals("mysql", entry.getDatabaseEngine());
        assertEquals("TINYINT(1)", entry.getSqlType());
        assertEquals("java.lang.Boolean", entry.getTargetType());

        JpaTypeMappingEntry copy = entry.copy();
        assertEquals(entry, copy);
        assertEquals(entry.hashCode(), copy.hashCode());
        assertTrue(copy.toString().contains("TINYINT(1)"));
    }

    @Test
    void testJpaReverseEngineeringSettingsModel() {
        JpaReverseEngineeringSettings settings = new JpaReverseEngineeringSettings();
        assertTrue(settings.isUseFetchTypeLazy());
        assertTrue(settings.isUseValidationAnnotations());
        assertTrue(settings.isConvertTableNameToSingular());
        assertFalse(settings.isReplaceOrmReferencesWithBasicTypes());
        assertEquals("Ignore", settings.getTableAndColumnComments());
        assertEquals("Configs", settings.getNamingRulesMode());
        assertEquals("Field", settings.getReservedKeywordFieldSuffix());
        assertEquals("mysql", settings.getSelectedDatabaseEngine());

        settings.setReplaceOrmReferencesWithBasicTypes(true);
        settings.setTableAndColumnComments("@Comment annotation");
        settings.setNamingRulesMode("Algorithm");
        settings.setPrefixesToSkipInTableName("tbl_, sys_");
        settings.setReservedKeywordFieldSuffix("Col");

        assertTrue(settings.isReplaceOrmReferencesWithBasicTypes());
        assertEquals("@Comment annotation", settings.getTableAndColumnComments());
        assertEquals("Algorithm", settings.getNamingRulesMode());
        assertEquals("tbl_, sys_", settings.getPrefixesToSkipInTableName());
        assertEquals("Col", settings.getReservedKeywordFieldSuffix());

        JpaReverseEngineeringSettings copy = settings.copy();
        assertEquals(settings, copy);
        assertEquals(settings.hashCode(), copy.hashCode());
    }

    @Test
    void testJpaReverseEngineeringSettingsManagerPersistence() {
        JpaReverseEngineeringSettingsManager manager = JpaReverseEngineeringSettingsManager.getInstance();
        JpaReverseEngineeringSettings settings = manager.getSettings();
        assertTrue(settings.isUseFetchTypeLazy());

        settings.setUseFetchTypeLazy(false);
        settings.setPrefixesToSkipInColumnName("col_");
        settings.getTypeMappings().add(new JpaTypeMappingEntry("postgresql", "CITEXT", "java.lang.String"));

        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addChangeListener(() -> notified.set(true));

        manager.setSettings(settings);
        assertTrue(notified.get());

        manager.load();
        JpaReverseEngineeringSettings loaded = manager.getSettings();
        assertFalse(loaded.isUseFetchTypeLazy());
        assertEquals("col_", loaded.getPrefixesToSkipInColumnName());
        assertEquals(1, loaded.getTypeMappings().size());
        assertEquals("CITEXT", loaded.getTypeMappings().get(0).getSqlType());

        manager.resetDefaults();
        assertTrue(manager.getSettings().isUseFetchTypeLazy());
    }

    // ==========================================
    // 3. Jupyter General Model & Manager
    // ==========================================

    @Test
    void testJupyterGeneralSettingsModel() {
        JupyterGeneralSettings settings = new JupyterGeneralSettings();
        assertTrue(settings.isShowAddCellPopup());
        assertFalse(settings.isShowRunAndDebugActions());
        assertTrue(settings.isInvertImageOutputsForDarkThemes());
        assertEquals(-1, settings.getMaxOutputHeightInTextLines());
        assertFalse(settings.isAsciiColoringInErrorOutputs());
        assertEquals(100, settings.getMarkdownFontScale());
        assertTrue(settings.isRenderMarkdownCellsAutomatically());
        assertTrue(settings.isOpenVariablesOnFirstCellExecution());
        assertTrue(settings.isShowInlineValues());
        assertEquals("In the current line", settings.getInlineValuesScope());
        assertTrue(settings.isNotifyWhenCellExecutionExceeds());
        assertEquals(60, settings.getCellExecutionTimeoutSeconds());
        assertFalse(settings.isShowTimestampOnExecutionLabel());
        assertEquals("Compact (recommended)", settings.getExecutionTimeDisplayMode());
        assertTrue(settings.isIncludeProjectSourceRootsToPythonPath());
        assertTrue(settings.isUploadSupportLibsToJupyterServer());

        settings.setMarkdownFontScale(120);
        settings.setExecutionTimeDisplayMode("Detailed");
        settings.setInlineValuesScope("In the entire notebook");

        assertEquals(120, settings.getMarkdownFontScale());
        assertEquals("Detailed", settings.getExecutionTimeDisplayMode());
        assertEquals("In the entire notebook", settings.getInlineValuesScope());

        JupyterGeneralSettings copy = settings.copy();
        assertEquals(settings, copy);
        assertEquals(settings.hashCode(), copy.hashCode());
    }

    @Test
    void testJupyterGeneralSettingsManagerPersistence() {
        JupyterGeneralSettingsManager manager = JupyterGeneralSettingsManager.getInstance();
        JupyterGeneralSettings settings = manager.getSettings();
        assertEquals(100, settings.getMarkdownFontScale());

        settings.setMarkdownFontScale(110);
        settings.setShowRunAndDebugActions(true);
        settings.setCellExecutionTimeoutSeconds(90);

        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addChangeListener(() -> notified.set(true));

        manager.setSettings(settings);
        assertTrue(notified.get());

        manager.load();
        JupyterGeneralSettings loaded = manager.getSettings();
        assertEquals(110, loaded.getMarkdownFontScale());
        assertTrue(loaded.isShowRunAndDebugActions());
        assertEquals(90, loaded.getCellExecutionTimeoutSeconds());

        manager.resetDefaults();
        assertEquals(100, manager.getSettings().getMarkdownFontScale());
        assertFalse(manager.getSettings().isShowRunAndDebugActions());
    }

    // ==========================================
    // 4. Jupyter Servers Model & Manager
    // ==========================================

    @Test
    void testJupyterServerConfigModel() {
        JupyterServerConfig config = new JupyterServerConfig("srv-1", "Local Jupyter", JupyterServerConfig.TYPE_EXTERNAL, false, "http://localhost:8888", "token123");
        assertEquals("srv-1", config.getId());
        assertEquals("Local Jupyter", config.getName());
        assertEquals(JupyterServerConfig.TYPE_EXTERNAL, config.getServerType());
        assertFalse(config.isAutodetectedExecutionMode());
        assertEquals("http://localhost:8888", config.getUrl());
        assertEquals("token123", config.getToken());

        JupyterServerConfig copy = config.copy();
        assertEquals(config, copy);
        assertEquals(config.hashCode(), copy.hashCode());
    }

    @Test
    void testJupyterServersSettingsModelAndManager() {
        JupyterServersSettingsManager manager = JupyterServersSettingsManager.getInstance();
        JupyterServersSettings settings = manager.getSettings();
        assertEquals(1, settings.getServers().size());
        assertEquals("IDE-Managed Server", settings.getServers().get(0).getName());
        assertTrue(settings.getServers().get(0).isAutodetectedExecutionMode());

        JupyterServerConfig customServer = new JupyterServerConfig("ext-1", "External GPU Cluster", JupyterServerConfig.TYPE_EXTERNAL, false, "http://cluster:8888", "secret");
        settings.getServers().add(customServer);
        settings.setSelectedServerId("ext-1");

        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addChangeListener(() -> notified.set(true));

        manager.setSettings(settings);
        assertTrue(notified.get());

        manager.load();
        JupyterServersSettings loaded = manager.getSettings();
        assertEquals(2, loaded.getServers().size());
        assertEquals("ext-1", loaded.getSelectedServerId());
        assertEquals("External GPU Cluster", loaded.getSelectedServer().getName());

        manager.resetDefaults();
        assertEquals(1, manager.getSettings().getServers().size());
    }

    // ==========================================
    // 5. Jupyter VCS Model & Manager
    // ==========================================

    @Test
    void testJupyterVcsSettingsModelAndManager() {
        JupyterVcsSettingsManager manager = JupyterVcsSettingsManager.getInstance();
        manager.resetDefaults();
        JupyterVcsSettings settings = manager.getSettings();
        assertTrue(settings.isClearOutputsOnCommit());
        assertTrue(settings.isShowRichDiffForNotebooks());

        settings.setClearOutputsOnCommit(false);
        settings.setShowRichDiffForNotebooks(false);

        manager.setSettings(settings);
        manager.load();
        assertFalse(manager.getSettings().isClearOutputsOnCommit());
        assertFalse(manager.getSettings().isShowRichDiffForNotebooks());

        manager.resetDefaults();
        assertTrue(manager.getSettings().isClearOutputsOnCommit());
    }

    // ==========================================
    // 6. UI Page Tests
    // ==========================================

    @Test
    void testSettingsToolsJpaEntityDeclarationPage() throws Exception {
        runOnFx(() -> {
            SettingsToolsJpaEntityDeclarationPage page = new SettingsToolsJpaEntityDeclarationPage();
            assertFalse(page.isModified());

            page.getAnnotationsOnGettersCheck().setSelected(true);
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertTrue(JpaEntityDeclarationSettingsManager.getInstance().getSettings().isGenerateJpaAnnotationsOnGetterMethod());

            page.getScaffoldingLangCombo().setValue("Kotlin");
            assertTrue(page.isModified());
            page.revertChanges();
            assertFalse(page.isModified());
            assertEquals("Always Ask", page.getScaffoldingLangCombo().getValue());

            page.reset();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testSettingsToolsJpaReverseEngineeringPage() throws Exception {
        runOnFx(() -> {
            SettingsToolsJpaReverseEngineeringPage page = new SettingsToolsJpaReverseEngineeringPage();
            assertFalse(page.isModified());

            page.getReplaceOrmBasicTypesCheck().setSelected(true);
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertTrue(JpaReverseEngineeringSettingsManager.getInstance().getSettings().isReplaceOrmReferencesWithBasicTypes());

            page.getPrefixesTableField().setText("custom_prefix_");
            assertTrue(page.isModified());
            page.revertChanges();
            assertFalse(page.isModified());

            page.reset();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testSettingsToolsJupyterOverviewPage() throws Exception {
        runOnFx(() -> {
            AtomicReference<String> navigatedTo = new AtomicReference<>(null);
            SettingsToolsJupyterOverviewPage page = new SettingsToolsJupyterOverviewPage(navigatedTo::set);

            assertNotNull(page.getGeneralLink());
            assertNotNull(page.getServersLink());
            assertNotNull(page.getVcsLink());

            page.getGeneralLink().fire();
            assertEquals("Jupyter General", navigatedTo.get());

            page.getServersLink().fire();
            assertEquals("Jupyter Servers", navigatedTo.get());

            page.getVcsLink().fire();
            assertEquals("Jupyter VCS", navigatedTo.get());
        });
    }

    @Test
    void testSettingsToolsJupyterGeneralPage() throws Exception {
        runOnFx(() -> {
            SettingsToolsJupyterGeneralPage page = new SettingsToolsJupyterGeneralPage();
            assertFalse(page.isModified());

            page.getMarkdownFontScaleField().setText("130");
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertEquals(130, JupyterGeneralSettingsManager.getInstance().getSettings().getMarkdownFontScale());

            page.getShowRunDebugActionsCheck().setSelected(true);
            assertTrue(page.isModified());
            page.revertChanges();
            assertFalse(page.isModified());

            page.reset();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testSettingsToolsJupyterServersPage() throws Exception {
        runOnFx(() -> {
            SettingsToolsJupyterServersPage page = new SettingsToolsJupyterServersPage();
            assertFalse(page.isModified());

            assertEquals(1, page.getServersItems().size());
            assertEquals("IDE-Managed Server", page.getNameField().getText());

            page.getNameField().setText("IDE-Managed Server Renamed");
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertEquals("IDE-Managed Server Renamed", JupyterServersSettingsManager.getInstance().getSettings().getServers().get(0).getName());

            page.reset();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testSettingsToolsJupyterVcsPage() throws Exception {
        runOnFx(() -> {
            JupyterVcsSettingsManager.getInstance().resetDefaults();
            SettingsToolsJupyterVcsPage page = new SettingsToolsJupyterVcsPage();
            assertFalse(page.isModified());

            page.getClearOutputsOnCommitCheck().setSelected(false);
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertFalse(JupyterVcsSettingsManager.getInstance().getSettings().isClearOutputsOnCommit());

            page.reset();
            assertFalse(page.isModified());
            JupyterVcsSettingsManager.getInstance().resetDefaults();
        });
    }

    // ==========================================
    // 7. SettingsDialog Integration Tests
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

            TreeItem<String> jpaDeclNode = null;
            TreeItem<String> jpaRevNode = null;
            TreeItem<String> jupyterNode = null;

            for (TreeItem<String> child : toolsNode.getChildren()) {
                if ("JPA Entity Declaration".equals(child.getValue())) {
                    jpaDeclNode = child;
                } else if ("JPA Reverse Engineering".equals(child.getValue())) {
                    jpaRevNode = child;
                } else if ("Jupyter".equals(child.getValue())) {
                    jupyterNode = child;
                }
            }

            assertNotNull(jpaDeclNode, "JPA Entity Declaration should exist under Tools");
            assertNotNull(jpaRevNode, "JPA Reverse Engineering should exist under Tools");
            assertNotNull(jupyterNode, "Jupyter should exist under Tools");

            // Verify Jupyter children
            assertEquals(3, jupyterNode.getChildren().size());
            assertTrue(jupyterNode.getChildren().stream().anyMatch(c -> "Jupyter General".equals(c.getValue())));
            assertTrue(jupyterNode.getChildren().stream().anyMatch(c -> "Jupyter Servers".equals(c.getValue())));
            assertTrue(jupyterNode.getChildren().stream().anyMatch(c -> "Jupyter VCS".equals(c.getValue())));

            // Navigate to JPA Entity Declaration
            dialog.getTree().getSelectionModel().select(jpaDeclNode);
            assertNotNull(dialog.getCurrentToolsJpaEntityDeclarationPage());

            // Navigate to JPA Reverse Engineering
            dialog.getTree().getSelectionModel().select(jpaRevNode);
            assertNotNull(dialog.getCurrentToolsJpaReverseEngineeringPage());

            // Navigate to Jupyter Hub
            dialog.getTree().getSelectionModel().select(jupyterNode);
            assertNotNull(dialog.getCurrentToolsJupyterOverviewPage());

            // Navigate to Jupyter General
            TreeItem<String> jupyterGeneralNode = jupyterNode.getChildren().get(0);
            dialog.getTree().getSelectionModel().select(jupyterGeneralNode);
            assertNotNull(dialog.getCurrentToolsJupyterGeneralPage());

            // Navigate to Jupyter Servers
            TreeItem<String> jupyterServersNode = jupyterNode.getChildren().get(1);
            dialog.getTree().getSelectionModel().select(jupyterServersNode);
            assertNotNull(dialog.getCurrentToolsJupyterServersPage());

            // Navigate to Jupyter VCS
            TreeItem<String> jupyterVcsNode = jupyterNode.getChildren().get(2);
            dialog.getTree().getSelectionModel().select(jupyterVcsNode);
            assertNotNull(dialog.getCurrentToolsJupyterVcsPage());

            // Test modify and applyAll through dialog
            dialog.getCurrentToolsJupyterGeneralPage().getMarkdownFontScaleField().setText("150");
            assertTrue(dialog.getCurrentToolsJupyterGeneralPage().isModified());
            assertFalse(dialog.getApplyButton().isDisable());

            dialog.applyAll();
            assertFalse(dialog.getCurrentToolsJupyterGeneralPage().isModified());
            assertTrue(dialog.getApplyButton().isDisable());
            assertEquals(150, JupyterGeneralSettingsManager.getInstance().getSettings().getMarkdownFontScale());
        });
    }

    // ==========================================
    // 8. Strict Brand Isolation Verification
    // ==========================================

    @Test
    void testBrandIsolationInAllCreatedFiles() throws Exception {
        List<String> filesToCheck = List.of(
                "src/main/java/dev/lumina/tools/JpaNameTemplateEntry.java",
                "src/main/java/dev/lumina/tools/JpaEntityDeclarationSettings.java",
                "src/main/java/dev/lumina/tools/JpaEntityDeclarationSettingsManager.java",
                "src/main/java/dev/lumina/tools/JpaTypeMappingEntry.java",
                "src/main/java/dev/lumina/tools/JpaReverseEngineeringSettings.java",
                "src/main/java/dev/lumina/tools/JpaReverseEngineeringSettingsManager.java",
                "src/main/java/dev/lumina/tools/JupyterGeneralSettings.java",
                "src/main/java/dev/lumina/tools/JupyterGeneralSettingsManager.java",
                "src/main/java/dev/lumina/tools/JupyterServerConfig.java",
                "src/main/java/dev/lumina/tools/JupyterServersSettings.java",
                "src/main/java/dev/lumina/tools/JupyterServersSettingsManager.java",
                "src/main/java/dev/lumina/tools/JupyterVcsSettings.java",
                "src/main/java/dev/lumina/tools/JupyterVcsSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsToolsJpaEntityDeclarationPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsJpaReverseEngineeringPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsJupyterOverviewPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsJupyterGeneralPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsJupyterServersPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsJupyterVcsPage.java"
        );

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
