package dev.lumina.ui;

import dev.lumina.tools.JupyterVcsSettings;
import dev.lumina.tools.JupyterVcsSettingsManager;
import dev.lumina.tools.KotlinNotebookNewNotebooksSettings;
import dev.lumina.tools.KotlinNotebookNewNotebooksSettingsManager;
import dev.lumina.tools.KotlinNotebookSettings;
import dev.lumina.tools.KotlinNotebookSettingsManager;
import dev.lumina.tools.McpServerSettings;
import dev.lumina.tools.McpServerSettingsManager;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit and UI lifecycle test suite for Kotlin Notebook, Settings for New Notebooks,
 * MCP Server, and updated Jupyter VCS settings in Lumina IDE.
 */
public class SettingsToolsKotlinNotebookAndMcpServerTest {

    private static boolean javaFxAvailable = false;

    @BeforeAll
    static void initFx() {
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
        assertTrue(latch.await(10, TimeUnit.SECONDS), "JavaFX thread timed out");
        if (err[0] != null) {
            throw new RuntimeException(err[0]);
        }
    }

    @Test
    void testKotlinNotebookSettingsModelDefaultsAndCloning() {
        KotlinNotebookSettings settings = new KotlinNotebookSettings();

        assertEquals(KotlinNotebookSettings.DEFAULT_KERNEL_VERSION, settings.getKernelVersion());
        assertEquals("Project SDK 25", settings.getJdkPath());
        assertEquals("Selected JDK default", settings.getJvmTargetForSnippets());
        assertEquals(3256, settings.getMaxHeapSize());
        assertEquals("", settings.getJvmExtraArguments());
        assertEquals("", settings.getEnvironmentVariables());

        assertFalse(settings.isShowNotebookSessionVariables());
        assertFalse(settings.isOpenVariablesTabAfterCellExecution());

        assertTrue(settings.isStopExecutionOnFailure());
        assertTrue(settings.isResolveSources());
        assertFalse(settings.isResolveMultiplatformDependencies());

        assertTrue(settings.isRenderKandyPlotsNatively());
        assertTrue(settings.isRenderDataFrameTablesNatively());

        assertFalse(settings.isShowTypeHintsOnlyInActiveCell());

        assertTrue(settings.isDisplayExecutionCount());
        assertTrue(settings.isShowFoldableRegions());

        // Clone
        KotlinNotebookSettings copy = settings.clone();
        assertEquals(settings, copy);
        assertEquals(settings.hashCode(), copy.hashCode());

        copy.setMaxHeapSize(4096);
        copy.setJvmExtraArguments("-Xms1g -Xmx4g");
        copy.setShowNotebookSessionVariables(true);
        assertNotEquals(settings, copy);
    }

    @Test
    void testKotlinNotebookSettingsManagerLifecycle() {
        KotlinNotebookSettingsManager manager = KotlinNotebookSettingsManager.getInstance();
        assertNotNull(manager);

        KotlinNotebookSettings original = manager.getSettings().clone();
        try {
            KotlinNotebookSettings custom = new KotlinNotebookSettings();
            custom.setKernelVersion("0.15.0");
            custom.setMaxHeapSize(4096);
            custom.setJvmExtraArguments("-Dfile.encoding=UTF-8");
            custom.setStopExecutionOnFailure(false);

            AtomicBoolean notified = new AtomicBoolean(false);
            Runnable listener = () -> notified.set(true);
            manager.addChangeListener(listener);

            manager.setSettings(custom);
            assertTrue(notified.get());

            KotlinNotebookSettings loaded = manager.getSettings();
            assertEquals("0.15.0", loaded.getKernelVersion());
            assertEquals(4096, loaded.getMaxHeapSize());
            assertEquals("-Dfile.encoding=UTF-8", loaded.getJvmExtraArguments());
            assertFalse(loaded.isStopExecutionOnFailure());

            manager.removeChangeListener(listener);
        } finally {
            manager.setSettings(original);
        }
    }

    @Test
    void testSettingsToolsKotlinNotebookPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        KotlinNotebookSettings original = KotlinNotebookSettingsManager.getInstance().getSettings().clone();
        try {
            KotlinNotebookSettingsManager.getInstance().setSettings(new KotlinNotebookSettings());

            runOnFx(() -> {
                SettingsToolsKotlinNotebookPage page = new SettingsToolsKotlinNotebookPage();
                assertFalse(page.isModified());

                AtomicBoolean modifiedNotified = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedNotified.set(true));

                // Change heap size
                page.getMaxHeapSizeSpinner().getValueFactory().setValue(4096);
                assertTrue(page.isModified());
                assertTrue(modifiedNotified.get());

                // Apply
                page.apply();
                assertFalse(page.isModified());
                assertEquals(4096, KotlinNotebookSettingsManager.getInstance().getSettings().getMaxHeapSize());

                // Revert test
                page.getJvmExtraArgsField().setText("-Xmx8g");
                assertTrue(page.isModified());
                page.revertChanges();
                assertFalse(page.isModified());
                assertEquals("", page.getJvmExtraArgsField().getText());

                // Revert kernel button
                page.getKernelVersionCombo().setValue("0.14.0");
                assertTrue(page.isModified());
                page.getRevertKernelButton().fire();
                assertEquals(KotlinNotebookSettings.DEFAULT_KERNEL_VERSION, page.getKernelVersionCombo().getValue());

                // Checkbox toggle test
                boolean origFailure = page.getStopExecutionOnFailureCheck().isSelected();
                page.getStopExecutionOnFailureCheck().setSelected(!origFailure);
                assertTrue(page.isModified());
                page.reset();
                assertEquals(origFailure, page.getStopExecutionOnFailureCheck().isSelected());
            });
        } finally {
            KotlinNotebookSettingsManager.getInstance().setSettings(original);
        }
    }

    @Test
    void testKotlinNotebookNewNotebooksSettingsModelAndManager() {
        KotlinNotebookNewNotebooksSettingsManager manager = KotlinNotebookNewNotebooksSettingsManager.getInstance();
        assertNotNull(manager);

        KotlinNotebookNewNotebooksSettings original = manager.getSettings().clone();
        try {
            KotlinNotebookNewNotebooksSettings settings = new KotlinNotebookNewNotebooksSettings();
            assertFalse(settings.isAddProjectLibrariesToClasspath());

            KotlinNotebookNewNotebooksSettings copy = settings.clone();
            assertEquals(settings, copy);

            settings.setAddProjectLibrariesToClasspath(true);
            assertTrue(settings.isAddProjectLibrariesToClasspath());
            assertNotEquals(settings, copy);

            AtomicBoolean notified = new AtomicBoolean(false);
            Runnable listener = () -> notified.set(true);
            manager.addChangeListener(listener);

            manager.setSettings(settings);
            assertTrue(notified.get());
            assertTrue(manager.getSettings().isAddProjectLibrariesToClasspath());

            manager.removeChangeListener(listener);
        } finally {
            manager.setSettings(original);
        }
    }

    @Test
    void testSettingsToolsKotlinNotebookNewNotebooksPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        KotlinNotebookNewNotebooksSettings original = KotlinNotebookNewNotebooksSettingsManager.getInstance().getSettings().clone();
        try {
            KotlinNotebookNewNotebooksSettingsManager.getInstance().setSettings(new KotlinNotebookNewNotebooksSettings());

            runOnFx(() -> {
                SettingsToolsKotlinNotebookNewNotebooksPage page = new SettingsToolsKotlinNotebookNewNotebooksPage();
                assertFalse(page.isModified());

                AtomicBoolean modifiedNotified = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedNotified.set(true));

                page.getAddProjectLibrariesCheck().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedNotified.get());

                page.apply();
                assertFalse(page.isModified());
                assertTrue(KotlinNotebookNewNotebooksSettingsManager.getInstance().getSettings().isAddProjectLibrariesToClasspath());

                page.getAddProjectLibrariesCheck().setSelected(false);
                assertTrue(page.isModified());
                page.revertChanges();
                assertFalse(page.isModified());
                assertTrue(page.getAddProjectLibrariesCheck().isSelected());
            });
        } finally {
            KotlinNotebookNewNotebooksSettingsManager.getInstance().setSettings(original);
        }
    }

    @Test
    void testMcpServerSettingsModelAndManager() {
        McpServerSettingsManager manager = McpServerSettingsManager.getInstance();
        assertNotNull(manager);

        McpServerSettings original = manager.getSettings().clone();
        try {
            McpServerSettings settings = new McpServerSettings();
            assertFalse(settings.isEnableMcpServer());
            assertNotNull(settings.getDetectedClients());
            assertTrue(settings.getDetectedClients().contains("Claude App"));
            assertTrue(settings.getDetectedClients().contains("Claude Code"));

            McpServerSettings copy = settings.clone();
            assertEquals(settings, copy);

            settings.setEnableMcpServer(true);
            assertTrue(settings.isEnableMcpServer());
            assertNotEquals(settings, copy);

            AtomicBoolean notified = new AtomicBoolean(false);
            Runnable listener = () -> notified.set(true);
            manager.addChangeListener(listener);

            manager.setSettings(settings);
            assertTrue(notified.get());
            assertTrue(manager.getSettings().isEnableMcpServer());

            manager.removeChangeListener(listener);
        } finally {
            manager.setSettings(original);
        }
    }

    @Test
    void testSettingsToolsMcpServerPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        McpServerSettings original = McpServerSettingsManager.getInstance().getSettings().clone();
        try {
            McpServerSettingsManager.getInstance().setSettings(new McpServerSettings());

            runOnFx(() -> {
                SettingsToolsMcpServerPage page = new SettingsToolsMcpServerPage();
                assertFalse(page.isModified());
                assertNotNull(page.getAllMcpToolsLink());
                assertNotNull(page.getDetectedClientsBox());

                AtomicBoolean modifiedNotified = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedNotified.set(true));

                page.getEnableMcpServerCheck().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedNotified.get());

                page.apply();
                assertFalse(page.isModified());
                assertTrue(McpServerSettingsManager.getInstance().getSettings().isEnableMcpServer());

                page.getEnableMcpServerCheck().setSelected(false);
                assertTrue(page.isModified());
                page.revertChanges();
                assertFalse(page.isModified());
                assertTrue(page.getEnableMcpServerCheck().isSelected());
            });
        } finally {
            McpServerSettingsManager.getInstance().setSettings(original);
        }
    }

    @Test
    void testUpdatedJupyterVcsSettingsAndPage() throws Exception {
        if (!javaFxAvailable) return;

        JupyterVcsSettings original = JupyterVcsSettingsManager.getInstance().getSettings().clone();
        try {
            JupyterVcsSettingsManager.getInstance().setSettings(new JupyterVcsSettings());

            runOnFx(() -> {
                SettingsToolsJupyterVcsPage page = new SettingsToolsJupyterVcsPage();
                assertFalse(page.isModified());

                assertTrue(page.getClearNotebookOutputsCheck().isSelected());
                assertTrue(page.getSuggestRadio().isSelected());
                assertFalse(page.getAlwaysRadio().isSelected());
                assertEquals(10, page.getSizeSpinner().getValue());
                assertFalse(page.getSizeSpinner().isDisabled());

                AtomicBoolean modifiedNotified = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedNotified.set(true));

                // Switch to always clear
                page.getAlwaysRadio().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedNotified.get());
                assertTrue(page.getSizeSpinner().isDisabled());

                page.apply();
                assertFalse(page.isModified());
                assertEquals(JupyterVcsSettings.MODE_ALWAYS, JupyterVcsSettingsManager.getInstance().getSettings().getClearOutputsMode());

                // Modify spinner value
                page.getSuggestRadio().setSelected(true);
                assertFalse(page.getSizeSpinner().isDisabled());
                page.getSizeSpinner().getValueFactory().setValue(25);
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());
                assertEquals(25, JupyterVcsSettingsManager.getInstance().getSettings().getFileSizeExceedsMb());

                // Uncheck clear outputs -> disables sub-options
                page.getClearNotebookOutputsCheck().setSelected(false);
                assertTrue(page.isModified());
                page.revertChanges();
                assertFalse(page.isModified());
                assertTrue(page.getClearNotebookOutputsCheck().isSelected());
            });
        } finally {
            JupyterVcsSettingsManager.getInstance().setSettings(original);
        }
    }

    @Test
    void testSettingsDialogToolsTreeAndRoutingIntegration() throws Exception {
        if (!javaFxAvailable) return;

        runOnFx(() -> {
            SettingsDialog dialog = new SettingsDialog(null);
            assertNotNull(dialog);

            // Select Kotlin Notebook
            dialog.selectCategory("Tools", "Kotlin Notebook");
            assertNotNull(dialog.getCurrentToolsKotlinNotebookPage());

            // Select Settings for New Notebooks
            dialog.selectCategory("Tools", "Settings for New Notebooks");
            assertNotNull(dialog.getCurrentToolsKotlinNotebookNewNotebooksPage());

            // Select MCP Server
            dialog.selectCategory("Tools", "MCP Server");
            assertNotNull(dialog.getCurrentToolsMcpServerPage());

            // Select Jupyter VCS
            dialog.selectCategory("Tools", "Jupyter VCS");
            assertNotNull(dialog.getCurrentToolsJupyterVcsPage());
        });
    }

    @Test
    void testStrictBrandIsolation() throws Exception {
        String[] filesToCheck = {
                "src/main/java/dev/lumina/tools/KotlinNotebookSettings.java",
                "src/main/java/dev/lumina/tools/KotlinNotebookSettingsManager.java",
                "src/main/java/dev/lumina/tools/KotlinNotebookNewNotebooksSettings.java",
                "src/main/java/dev/lumina/tools/KotlinNotebookNewNotebooksSettingsManager.java",
                "src/main/java/dev/lumina/tools/McpServerSettings.java",
                "src/main/java/dev/lumina/tools/McpServerSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsToolsKotlinNotebookPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsKotlinNotebookNewNotebooksPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsMcpServerPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsJupyterVcsPage.java",
                "src/main/java/dev/lumina/tools/JupyterVcsSettings.java",
                "src/main/java/dev/lumina/tools/JupyterVcsSettingsManager.java"
        };

        String[] forbidden = {
                "intellij",
                "jetbrains",
                "pycharm",
                "webstorm",
                "clion"
        };

        for (String filePath : filesToCheck) {
            String content = Files.readString(Paths.get(filePath));
            for (String f : forbidden) {
                assertFalse(content.toLowerCase().contains(f),
                        "File " + filePath + " must not contain forbidden competitor keyword: " + f);
            }
        }
    }
}
