package dev.lumina.ui;

import dev.lumina.tools.*;
import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class SettingsToolsPagesTest {

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

    // ============================================================
    // 1. Tools Overview Page (Image 1)
    // ============================================================

    @Test
    void testToolsOverviewPageAndLinks() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            AtomicReference<String> navigated = new AtomicReference<>();
            SettingsToolsOverviewPage page = new SettingsToolsOverviewPage(navigated::set);

            assertNotNull(page);
            assertTrue(SettingsToolsOverviewPage.DEFAULT_TOOL_LINKS.contains("Actions on Save"));
            assertTrue(SettingsToolsOverviewPage.DEFAULT_TOOL_LINKS.contains("Black"));
            assertTrue(SettingsToolsOverviewPage.DEFAULT_TOOL_LINKS.contains("Bundler"));
            assertTrue(SettingsToolsOverviewPage.DEFAULT_TOOL_LINKS.contains("Claude Code [Beta]"));
            assertTrue(SettingsToolsOverviewPage.DEFAULT_TOOL_LINKS.contains("Database"));
            assertTrue(SettingsToolsOverviewPage.DEFAULT_TOOL_LINKS.contains("Terminal"));
            assertEquals(35, SettingsToolsOverviewPage.DEFAULT_TOOL_LINKS.size());
        });
    }

    // ============================================================
    // 2. Actions on Save (Image 2)
    // ============================================================

    @Test
    void testActionsOnSaveModelAndManager() {
        ActionsOnSaveManager manager = ActionsOnSaveManager.getInstance();
        ActionsOnSaveSettings settings = new ActionsOnSaveSettings();

        assertNotNull(settings.getItems());
        assertEquals(11, settings.getItems().size());

        ActionOnSaveItem reformat = settings.findItemById("reformat.code");
        assertNotNull(reformat);
        assertTrue(reformat.isEnabled());
        assertEquals("Any save", reformat.getActivatedOn());
        assertEquals("Go files", reformat.getSelectedScope());
        assertEquals("Whole file", reformat.getSelectedMode());

        ActionOnSaveItem black = settings.findItemById("black");
        assertNotNull(black);
        assertFalse(black.isEnabled());
        assertTrue(black.isWarning());
        assertEquals("Black", black.getConfigureTarget());

        // Dynamic registration of a plugin action
        ActionOnSaveItem customAction = new ActionOnSaveItem("custom.linter", "Run Custom Linter", false, "Any save");
        manager.registerAction(customAction);

        ActionsOnSaveSettings loaded = manager.getSettings();
        assertNotNull(loaded.findItemById("custom.linter"));

        // Cleanup
        manager.setSettings(new ActionsOnSaveSettings());
    }

    @Test
    void testActionsOnSavePageLifecycle() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsToolsActionsOnSavePage page = new SettingsToolsActionsOnSavePage();
            assertFalse(page.isModified());

            CheckBox reformatCb = page.getCheckBoxes().get("reformat.code");
            assertNotNull(reformatCb);
            assertTrue(reformatCb.isSelected());

            // Toggle reformat checkbox
            reformatCb.setSelected(false);
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertFalse(ActionsOnSaveManager.getInstance().getSettings().findItemById("reformat.code").isEnabled());

            // Revert changes back
            reformatCb.setSelected(true);
            assertTrue(page.isModified());

            page.revertChanges();
            assertFalse(page.isModified());
            assertFalse(reformatCb.isSelected());

            // Reset to defaults
            ActionsOnSaveManager.getInstance().setSettings(new ActionsOnSaveSettings());
            page.reset();
        });
    }

    // ============================================================
    // 3. Black Code Formatter (Image 3)
    // ============================================================

    @Test
    void testBlackSettingsModelAndManager() {
        BlackSettingsManager manager = BlackSettingsManager.getInstance();
        BlackSettings settings = new BlackSettings();

        assertEquals(BlackSettings.MODE_PACKAGE, settings.getExecutionMode());
        assertEquals(BlackSettings.NO_INTERPRETER, settings.getPythonInterpreter());
        assertFalse(settings.isOnCodeReformat());
        assertFalse(settings.isOnSave());
        assertEquals("", settings.getArguments());

        settings.setExecutionMode(BlackSettings.MODE_BINARY);
        settings.setArguments("--line-length 100");
        settings.setOnSave(true);

        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addChangeListener(() -> notified.set(true));

        manager.setSettings(settings);
        assertTrue(notified.get());

        BlackSettings loaded = manager.getSettings();
        assertEquals(BlackSettings.MODE_BINARY, loaded.getExecutionMode());
        assertEquals("--line-length 100", loaded.getArguments());
        assertTrue(loaded.isOnSave());

        // Check sync with Actions on Save
        ActionsOnSaveSettings aosSettings = ActionsOnSaveManager.getInstance().getSettings();
        ActionOnSaveItem blackItem = aosSettings.findItemById("black");
        assertNotNull(blackItem);
        assertTrue(blackItem.isEnabled());

        // Check dynamic interpreter discovery
        List<String> discovered = manager.discoverPythonInterpreters();
        assertNotNull(discovered);
        assertFalse(discovered.isEmpty());
        assertTrue(discovered.contains(BlackSettings.NO_INTERPRETER));

        // Cleanup
        manager.setSettings(new BlackSettings());
    }

    @Test
    void testBlackPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsToolsBlackPage page = new SettingsToolsBlackPage();
            assertFalse(page.isModified());

            page.getArgumentsField().setText("--fast --line-length 120");
            assertTrue(page.isModified());

            page.getOnCodeReformatCheck().setSelected(true);
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertEquals("--fast --line-length 120", BlackSettingsManager.getInstance().getSettings().getArguments());
            assertTrue(BlackSettingsManager.getInstance().getSettings().isOnCodeReformat());

            page.getArgumentsField().setText("--custom");
            assertTrue(page.isModified());

            page.revertChanges();
            assertFalse(page.isModified());
            assertEquals("--fast --line-length 120", page.getArgumentsField().getText());

            // Cleanup
            BlackSettingsManager.getInstance().setSettings(new BlackSettings());
            page.reset();
        });
    }

    // ============================================================
    // 4. Bundler (Image 4)
    // ============================================================

    @Test
    void testBundlerSettingsModelAndManager() {
        BundlerSettingsManager manager = BundlerSettingsManager.getInstance();
        BundlerSettings settings = new BundlerSettings();

        assertTrue(settings.isAlwaysInstallRequiredVersion());
        assertTrue(settings.isUseDefaultArguments());
        assertEquals("", settings.getDefaultArguments());

        settings.setAlwaysInstallRequiredVersion(false);
        settings.setDefaultArguments("--clean --jobs 4");

        manager.setSettings(settings);
        BundlerSettings loaded = manager.getSettings();
        assertFalse(loaded.isAlwaysInstallRequiredVersion());
        assertEquals("--clean --jobs 4", loaded.getDefaultArguments());

        List<String> discovered = manager.discoverBundlerExecutables();
        assertNotNull(discovered);

        // Cleanup
        manager.setSettings(new BundlerSettings());
    }

    @Test
    void testBundlerPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsToolsBundlerPage page = new SettingsToolsBundlerPage();
            assertFalse(page.isModified());

            assertTrue(page.getAlwaysInstallRequiredVersionCheck().isSelected());
            assertTrue(page.getUseDefaultArgumentsCheck().isSelected());
            assertFalse(page.getDefaultArgumentsField().isDisable());

            page.getDefaultArgumentsField().setText("--without development test");
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertEquals("--without development test", BundlerSettingsManager.getInstance().getSettings().getDefaultArguments());

            // Uncheck use default arguments -> field disables
            page.getUseDefaultArgumentsCheck().setSelected(false);
            assertTrue(page.getDefaultArgumentsField().isDisable());
            assertTrue(page.isModified());

            page.revertChanges();
            assertFalse(page.isModified());
            assertTrue(page.getUseDefaultArgumentsCheck().isSelected());
            assertFalse(page.getDefaultArgumentsField().isDisable());

            // Cleanup
            BundlerSettingsManager.getInstance().setSettings(new BundlerSettings());
            page.reset();
        });
    }

    // ============================================================
    // 5. Claude Code [Beta] (Image 5)
    // ============================================================

    @Test
    void testClaudeCodeSettingsModelAndManager() {
        ClaudeCodeSettingsManager manager = ClaudeCodeSettingsManager.getInstance();
        ClaudeCodeSettings settings = new ClaudeCodeSettings();

        assertEquals("claude", settings.getClaudeCommand());
        assertEquals("", settings.getConfigDirectory());
        assertFalse(settings.isSuppressNotificationNotFound());
        assertFalse(settings.isHideToolbarButton());
        assertTrue(settings.isOptionEnterMultiLine());
        assertTrue(settings.isAutomaticUpdates());
        assertFalse(settings.isAcceptConnectionsAllInterfaces());

        settings.setClaudeCommand("/usr/local/bin/claude");
        settings.setSuppressNotificationNotFound(true);
        settings.setHideToolbarButton(true);

        manager.setSettings(settings);
        ClaudeCodeSettings loaded = manager.getSettings();
        assertEquals("/usr/local/bin/claude", loaded.getClaudeCommand());
        assertTrue(loaded.isSuppressNotificationNotFound());
        assertTrue(loaded.isHideToolbarButton());

        List<String> discovered = manager.discoverClaudeCommands();
        assertNotNull(discovered);
        assertTrue(discovered.contains("claude"));
        assertTrue(discovered.contains("npx @anthropic/claude"));

        // Cleanup
        manager.setSettings(new ClaudeCodeSettings());
    }

    @Test
    void testClaudeCodePageLifecycle() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsToolsClaudeCodePage page = new SettingsToolsClaudeCodePage();
            assertFalse(page.isModified());

            page.getClaudeCommandField().setText("npx @anthropic/claude");
            assertTrue(page.isModified());

            page.getSuppressNotificationCheck().setSelected(true);
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertEquals("npx @anthropic/claude", ClaudeCodeSettingsManager.getInstance().getSettings().getClaudeCommand());
            assertTrue(ClaudeCodeSettingsManager.getInstance().getSettings().isSuppressNotificationNotFound());

            page.getClaudeCommandField().setText("custom-claude");
            assertTrue(page.isModified());

            page.revertChanges();
            assertFalse(page.isModified());
            assertEquals("npx @anthropic/claude", page.getClaudeCommandField().getText());

            // Cleanup
            ClaudeCodeSettingsManager.getInstance().setSettings(new ClaudeCodeSettings());
            page.reset();
        });
    }

    // ============================================================
    // 6. SettingsDialog Integration
    // ============================================================

    @Test
    void testSettingsDialogToolsIntegration() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsDialog dialog = new SettingsDialog(null);
            assertNotNull(dialog);

            dialog.selectCategory("Tools");
            assertNotNull(dialog.getCurrentToolsOverviewPage());

            dialog.selectCategory("Actions on Save");
            assertNotNull(dialog.getCurrentToolsActionsOnSavePage());

            dialog.selectCategory("Black");
            assertNotNull(dialog.getCurrentToolsBlackPage());

            dialog.selectCategory("Bundler");
            assertNotNull(dialog.getCurrentToolsBundlerPage());

            dialog.selectCategory("Claude Code [Beta]");
            assertNotNull(dialog.getCurrentToolsClaudeCodePage());

            dialog.selectCategory("Tools", "Diff & Merge");
            assertNotNull(dialog.getCurrentToolsDiffMergePage());

            dialog.selectCategory("Diff & Merge", "External Diff Tools");
            assertNotNull(dialog.getCurrentToolsExternalDiffToolsPage());

            dialog.selectCategory("Tools", "External Tools");
            assertNotNull(dialog.getCurrentToolsExternalToolsPage());

            dialog.selectCategory("Tools", "Features Suggester");
            assertNotNull(dialog.getCurrentToolsFeaturesSuggesterPage());
        });
    }

    // ============================================================
    // 7. Diff & Merge (Image 2)
    // ============================================================

    @Test
    void testDiffMergeModelAndManager() {
        DiffMergeSettingsManager manager = DiffMergeSettingsManager.getInstance();
        DiffMergeSettings settings = new DiffMergeSettings();

        assertEquals(4, settings.getContextLines());
        assertTrue(settings.isGoToNextFileAfterLastChange());
        assertEquals("Until the diff is closed", settings.getNavigationHistoryPolicy());
        assertFalse(settings.isAutoApplyNonConflictingChanges());
        assertFalse(settings.isAutoResolveConflictsInImports());
        assertTrue(settings.isHighlightModifiedLinesInGutter());

        DiffMergeSettings custom = settings.clone();
        custom.setContextLines(8);
        custom.setGoToNextFileAfterLastChange(false);
        custom.setNavigationHistoryPolicy("Always");
        custom.setAutoApplyNonConflictingChanges(true);
        custom.setAutoResolveConflictsInImports(true);
        custom.setHighlightModifiedLinesInGutter(false);

        assertNotEquals(settings, custom);

        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addChangeListener(listener);

        manager.setSettings(custom);
        assertTrue(notified.get());

        DiffMergeSettings loaded = manager.getSettings();
        assertEquals(8, loaded.getContextLines());
        assertFalse(loaded.isGoToNextFileAfterLastChange());
        assertEquals("Always", loaded.getNavigationHistoryPolicy());
        assertTrue(loaded.isAutoApplyNonConflictingChanges());
        assertTrue(loaded.isAutoResolveConflictsInImports());
        assertFalse(loaded.isHighlightModifiedLinesInGutter());

        manager.removeChangeListener(listener);
        manager.setSettings(settings);
    }

    @Test
    void testDiffMergePageUI() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsToolsDiffMergePage page = new SettingsToolsDiffMergePage();
            assertNotNull(page);
            assertFalse(page.isModified());

            // Modify slider
            page.getContextLinesSlider().setValue(0); // 1 context line
            assertTrue(page.isModified());
            page.revertChanges();
            assertFalse(page.isModified());
            assertEquals(2.0, page.getContextLinesSlider().getValue());

            // Modify checkbox (default is true, so set to false)
            page.getGoToNextFileCheck().setSelected(false);
            assertTrue(page.isModified());
            page.apply();
            assertFalse(page.isModified());
            assertFalse(DiffMergeSettingsManager.getInstance().getSettings().isGoToNextFileAfterLastChange());

            // Reset
            DiffMergeSettingsManager.getInstance().setSettings(new DiffMergeSettings());
            page.reset();
            assertFalse(page.isModified());
            assertTrue(page.getGoToNextFileCheck().isSelected());
        });
    }

    // ============================================================
    // 8. External Diff Tools (Image 3)
    // ============================================================

    @Test
    void testExternalDiffToolsModelAndManager() {
        ExternalDiffToolsSettingsManager manager = ExternalDiffToolsSettingsManager.getInstance();
        ExternalDiffToolsSettings settings = new ExternalDiffToolsSettings();

        assertFalse(settings.isEnableExternalTools());
        assertNotNull(settings.getConfiguredTools());
        assertTrue(settings.getConfiguredTools().isEmpty());
        assertNotNull(settings.getAssociations());
        assertEquals(1, settings.getAssociations().size());
        assertEquals("Default", settings.getAssociations().get(0).getFileType());
        assertEquals("Built-in", settings.getAssociations().get(0).getDiffTool());
        assertEquals("Built-in", settings.getAssociations().get(0).getMergeTool());

        ExternalDiffToolsSettings custom = settings.clone();
        custom.setEnableExternalTools(true);
        custom.getConfiguredTools().add(new ExternalDiffToolDefinition("Beyond Compare", "/usr/local/bin/bcomp", "%1 %2"));
        custom.getAssociations().add(new ExternalDiffToolAssociation("*.java", "Beyond Compare", "Beyond Compare"));

        assertNotEquals(settings, custom);

        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addChangeListener(listener);

        manager.setSettings(custom);
        assertTrue(notified.get());

        ExternalDiffToolsSettings loaded = manager.getSettings();
        assertTrue(loaded.isEnableExternalTools());
        assertEquals(1, loaded.getConfiguredTools().size());
        assertEquals("Beyond Compare", loaded.getConfiguredTools().get(0).getName());
        assertEquals(2, loaded.getAssociations().size());

        manager.removeChangeListener(listener);
        manager.setSettings(settings);
    }

    @Test
    void testExternalDiffToolsPageUI() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsToolsExternalDiffToolsPage page = new SettingsToolsExternalDiffToolsPage();
            assertNotNull(page);
            assertFalse(page.isModified());

            page.getEnableExternalToolsCheck().setSelected(true);
            assertTrue(page.isModified());
            page.revertChanges();
            assertFalse(page.isModified());
            assertFalse(page.getEnableExternalToolsCheck().isSelected());

            page.getToolsTable().getItems().add(new ExternalDiffToolDefinition("KDiff3", "/usr/bin/kdiff3", "%1 %2"));
            assertTrue(page.isModified());
            page.apply();
            assertFalse(page.isModified());
            assertEquals(1, ExternalDiffToolsSettingsManager.getInstance().getSettings().getConfiguredTools().size());

            ExternalDiffToolsSettingsManager.getInstance().setSettings(new ExternalDiffToolsSettings());
            page.reset();
            assertFalse(page.isModified());
        });
    }

    // ============================================================
    // 9. External Tools (Image 4)
    // ============================================================

    @Test
    void testExternalToolsModelAndManager() {
        ExternalToolsSettingsManager manager = ExternalToolsSettingsManager.getInstance();
        ExternalToolsSettings settings = new ExternalToolsSettings();

        assertNotNull(settings.getTools());
        assertTrue(settings.getTools().isEmpty());

        ExternalToolItem tool = new ExternalToolItem();
        tool.setName("Flake8");
        tool.setGroup("Linters");
        tool.setDescription("Python linter");
        tool.setProgram("flake8");
        tool.setArguments("$FilePath$");
        tool.setWorkingDirectory("$ProjectFileDir$");
        tool.setSynchronizeFiles(true);
        tool.setOpenConsole(true);
        tool.setMakeActiveOnStdout(true);
        tool.setMakeActiveOnStderr(true);
        tool.setOutputFilters(".*");

        ExternalToolsSettings custom = settings.clone();
        custom.getTools().add(tool);

        assertNotEquals(settings, custom);

        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addChangeListener(listener);

        manager.setSettings(custom);
        assertTrue(notified.get());

        ExternalToolsSettings loaded = manager.getSettings();
        assertEquals(1, loaded.getTools().size());
        assertEquals("Flake8", loaded.getTools().get(0).getName());
        assertEquals("Linters", loaded.getTools().get(0).getGroup());

        manager.removeChangeListener(listener);
        manager.setSettings(settings);
    }

    @Test
    void testExternalToolsPageUI() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsToolsExternalToolsPage page = new SettingsToolsExternalToolsPage();
            assertNotNull(page);
            assertFalse(page.isModified());

            ExternalToolItem tool = new ExternalToolItem("Make", "Build");
            tool.setDescription("Run make");
            tool.setProgram("make");
            tool.setArguments("all");
            page.getToolsTable().getItems().add(tool);
            assertTrue(page.isModified());

            // Duplicate item
            page.getToolsTable().getSelectionModel().select(0);
            page.getDuplicateBtn().fire();
            assertEquals(2, page.getToolsTable().getItems().size());
            assertEquals("Make (copy)", page.getToolsTable().getItems().get(1).getName());

            page.revertChanges();
            assertFalse(page.isModified());
            assertEquals(0, page.getToolsTable().getItems().size());
        });
    }

    // ============================================================
    // 10. Features Suggester (Image 5)
    // ============================================================

    @Test
    void testFeaturesSuggesterModelAndManager() {
        FeaturesSuggesterSettingsManager manager = FeaturesSuggesterSettingsManager.getInstance();
        FeaturesSuggesterSettings settings = new FeaturesSuggesterSettings();

        assertTrue(settings.isShowSuggestions());
        assertNotNull(settings.getSuggestedActions());
        assertTrue(settings.getSuggestedActions().containsKey("Quick Evaluate"));
        assertTrue(settings.getSuggestedActions().get("Quick Evaluate"));

        FeaturesSuggesterSettings custom = settings.clone();
        custom.setShowSuggestions(false);
        custom.getSuggestedActions().put("Quick Evaluate", false);

        assertNotEquals(settings, custom);

        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addChangeListener(listener);

        manager.setSettings(custom);
        assertTrue(notified.get());

        FeaturesSuggesterSettings loaded = manager.getSettings();
        assertFalse(loaded.isShowSuggestions());
        assertFalse(loaded.getSuggestedActions().get("Quick Evaluate"));

        manager.removeChangeListener(listener);
        manager.setSettings(settings);
    }

    @Test
    void testFeaturesSuggesterPageUI() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsToolsFeaturesSuggesterPage page = new SettingsToolsFeaturesSuggesterPage();
            assertNotNull(page);
            assertFalse(page.isModified());

            page.getMasterCheck().setSelected(false);
            assertTrue(page.isModified());

            page.revertChanges();
            assertFalse(page.isModified());
            assertTrue(page.getMasterCheck().isSelected());

            CheckBox qe = page.getActionCheckBoxes().get("Quick Evaluate");
            assertNotNull(qe);
            qe.setSelected(false);
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertFalse(FeaturesSuggesterSettingsManager.getInstance().getSettings().getSuggestedActions().get("Quick Evaluate"));

            FeaturesSuggesterSettingsManager.getInstance().setSettings(new FeaturesSuggesterSettings());
            page.reset();
            assertFalse(page.isModified());
            assertTrue(qe.isSelected());
        });
    }

    // ============================================================
    // 11. Diagrams Settings Remaining Categories (Image 1)
    // ============================================================

    @Test
    void testDiagramsSettingsRemainingCategories() {
        DiagramsSettings ds = new DiagramsSettings();
        var cat = ds.getCategorySelections();
        assertNotNull(cat);

        // Database Schema Diagram
        assertTrue(cat.containsKey("Database Schema Diagram"));
        assertTrue(cat.containsKey("Database Schema Diagram/Key columns"));
        assertTrue(cat.containsKey("Database Schema Diagram/Columns"));
        assertTrue(cat.containsKey("Database Schema Diagram/Virtual foreign keys"));
        assertTrue(cat.containsKey("Database Schema Diagram/Comments"));

        // Graphical Explain Plan
        assertTrue(cat.containsKey("Graphical Explain Plan"));
        assertTrue(cat.containsKey("Graphical Explain Plan/Attributes"));

        // JPA ER Diagram
        assertTrue(cat.containsKey("JPA ER Diagram"));
        assertTrue(cat.containsKey("JPA ER Diagram/Properties"));
        assertTrue(cat.containsKey("JPA ER Diagram/Embeddables"));
        assertTrue(cat.containsKey("JPA ER Diagram/Superclasses"));

        // PHP Class Diagrams
        assertTrue(cat.containsKey("PHP Class Diagrams"));
        assertTrue(cat.containsKey("PHP Class Diagrams/Fields"));
        assertTrue(cat.containsKey("PHP Class Diagrams/Constants"));
        assertTrue(cat.containsKey("PHP Class Diagrams/Constructors"));
        assertTrue(cat.containsKey("PHP Class Diagrams/Methods"));

        // Services diagram
        assertTrue(cat.containsKey("Services diagram"));
        assertTrue(cat.containsKey("Services diagram/From tests"));
        assertTrue(cat.containsKey("Services diagram/From libraries"));
        assertTrue(cat.containsKey("Services diagram/Show Neighbors of Selected Nodes"));
        assertTrue(cat.containsKey("Services diagram/Show Borders"));
    }

    // ============================================================
    // 12. Strict Brand Isolation Assertion
    // ============================================================

    @Test
    void testStrictBrandIsolation() throws Exception {
        String[] filesToCheck = {
                "src/main/java/dev/lumina/tools/ActionOnSaveItem.java",
                "src/main/java/dev/lumina/tools/ActionsOnSaveSettings.java",
                "src/main/java/dev/lumina/tools/ActionsOnSaveManager.java",
                "src/main/java/dev/lumina/tools/BlackSettings.java",
                "src/main/java/dev/lumina/tools/BlackSettingsManager.java",
                "src/main/java/dev/lumina/tools/BundlerSettings.java",
                "src/main/java/dev/lumina/tools/BundlerSettingsManager.java",
                "src/main/java/dev/lumina/tools/ClaudeCodeSettings.java",
                "src/main/java/dev/lumina/tools/ClaudeCodeSettingsManager.java",
                "src/main/java/dev/lumina/tools/CodeWithMeSettings.java",
                "src/main/java/dev/lumina/tools/CodeWithMeSettingsManager.java",
                "src/main/java/dev/lumina/tools/CsvFormat.java",
                "src/main/java/dev/lumina/tools/CsvFormatsSettings.java",
                "src/main/java/dev/lumina/tools/CsvFormatsSettingsManager.java",
                "src/main/java/dev/lumina/tools/DiagramsSettings.java",
                "src/main/java/dev/lumina/tools/DiagramsSettingsManager.java",
                "src/main/java/dev/lumina/tools/DiffMergeSettings.java",
                "src/main/java/dev/lumina/tools/DiffMergeSettingsManager.java",
                "src/main/java/dev/lumina/tools/ExternalDiffToolDefinition.java",
                "src/main/java/dev/lumina/tools/ExternalDiffToolAssociation.java",
                "src/main/java/dev/lumina/tools/ExternalDiffToolsSettings.java",
                "src/main/java/dev/lumina/tools/ExternalDiffToolsSettingsManager.java",
                "src/main/java/dev/lumina/tools/ExternalToolItem.java",
                "src/main/java/dev/lumina/tools/ExternalToolsSettings.java",
                "src/main/java/dev/lumina/tools/ExternalToolsSettingsManager.java",
                "src/main/java/dev/lumina/tools/FeaturesSuggesterSettings.java",
                "src/main/java/dev/lumina/tools/FeaturesSuggesterSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsToolsOverviewPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsActionsOnSavePage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsBlackPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsBundlerPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsClaudeCodePage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsCodeWithMePage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsCsvFormatsPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsDiagramsPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsDiffMergePage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsExternalDiffToolsPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsExternalToolsPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsFeaturesSuggesterPage.java"
        };

        String[] forbidden = {
                "IntelliJ", "intellij",
                "JetBrains", "jetbrains",
                "WebStorm", "webstorm",
                "IDEA"
        };

        for (String filePath : filesToCheck) {
            File f = new File(filePath);
            assertTrue(f.exists(), "Source file should exist: " + filePath);
            String content = Files.readString(f.toPath());
            for (String brand : forbidden) {
                assertFalse(content.contains(brand),
                        "Source file " + filePath + " must not contain competitor brand: " + brand);
            }
        }
    }
}
