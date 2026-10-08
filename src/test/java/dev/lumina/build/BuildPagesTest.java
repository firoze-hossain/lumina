package dev.lumina.build;

import dev.lumina.ui.*;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UI and lifecycle integration tests for Settings pages under Build, Execution, Deployment.
 */
public class BuildPagesTest {

    private static boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> javaFxAvailable = true);
            javaFxAvailable = true;
        } catch (IllegalStateException e) {
            javaFxAvailable = true;
        } catch (Throwable t) {
            javaFxAvailable = false;
        }
    }

    private void runOnFx(Runnable action) {
        if (!javaFxAvailable) return;
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "Timed out waiting for JavaFX thread");
        } catch (InterruptedException e) {
            fail(e);
        }
    }

    @Test
    void testPythonDebuggerPageLifecycle() {
        runOnFx(() -> {
            SettingsPythonDebuggerPage page = new SettingsPythonDebuggerPage();
            assertFalse(page.isModified(), "New page should not be modified");

            AtomicBoolean modifiedNotified = new AtomicBoolean(false);
            page.setOnModifiedListener(() -> modifiedNotified.set(true));

            PythonDebuggerSettings initial = page.getCurrentSettings();
            assertTrue(initial.isAttachToSubprocess());
            assertEquals("python", initial.getAttachProcessFilter());

            // Modify a setting
            initial.setAttachToSubprocess(false);
            initial.setAttachProcessFilter("custom-py");

            // Verify isModified when current values differ
            page.loadData();
            assertFalse(page.isModified());

            page.reset();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testApplicationServersPageLifecycle() {
        runOnFx(() -> {
            SettingsApplicationServersPage page = new SettingsApplicationServersPage();
            assertFalse(page.isModified(), "New page should not be modified");

            page.reset();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testBuildToolsPageLifecycle() {
        runOnFx(() -> {
            SettingsBuildToolsPage page = new SettingsBuildToolsPage();
            assertFalse(page.isModified(), "New page should not be modified");

            BuildToolsSettings s = page.getCurrentSettings();
            assertTrue(s.isSyncOnBuildScriptChanges());
            assertEquals(BuildToolsSettings.SyncTrigger.EXTERNAL_CHANGES, s.getSyncTrigger());

            page.reset();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testMavenPageLifecycle() {
        runOnFx(() -> {
            SettingsMavenPage page = new SettingsMavenPage();
            assertFalse(page.isModified(), "New page should not be modified");

            MavenSettings s = page.getCurrentSettings();
            assertFalse(s.isWorkOffline());
            assertTrue(s.isExecuteGoalsRecursively());
            assertEquals("Info", s.getOutputLevel());
            assertEquals("Bundled (Maven 3)", s.getMavenHome());

            page.reset();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testSettingsDialogTreeAndRouting() {
        runOnFx(() -> {
            SettingsDialog dialog = new SettingsDialog(null);

            // Verify Build, Execution, Deployment tree node hierarchy
            TreeItem<String> buildRoot = dialog.findItem(dialog.getTreeRoot(), "Build, Execution, Deployment");
            assertNotNull(buildRoot, "Build, Execution, Deployment root item should exist");

            assertNotNull(dialog.findItem(buildRoot, "Python Debugger"));
            assertNotNull(dialog.findItem(buildRoot, "Application Servers"));

            TreeItem<String> buildTools = dialog.findItem(buildRoot, "Build Tools");
            assertNotNull(buildTools, "Build Tools item should exist");

            TreeItem<String> maven = dialog.findItem(buildTools, "Maven");
            assertNotNull(maven, "Maven item should exist under Build Tools");

            assertNotNull(dialog.findItem(maven, "Archetype Catalogs"));
            assertNotNull(dialog.findItem(maven, "Ignored Files"));
            assertNotNull(dialog.findItem(maven, "Importing"));
            assertNotNull(dialog.findItem(maven, "Repositories"));
            assertNotNull(dialog.findItem(maven, "Runner"));
            assertNotNull(dialog.findItem(maven, "Running Tests"));

            assertNotNull(dialog.findItem(buildTools, "Gradle"));
            assertNotNull(dialog.findItem(buildTools, "Gant"));
            assertNotNull(dialog.findItem(buildTools, "BSP"));
            assertNotNull(dialog.findItem(buildTools, "Cargo"));
            assertNotNull(dialog.findItem(buildTools, "sbt"));

            assertNotNull(dialog.findItem(buildRoot, "Compiler"));
            assertNotNull(dialog.findItem(buildRoot, "Console"));
            assertNotNull(dialog.findItem(buildRoot, "Coverage"));
            assertNotNull(dialog.findItem(buildRoot, "Debugger"));
            assertNotNull(dialog.findItem(buildRoot, "Deployment"));
            assertNotNull(dialog.findItem(buildRoot, "Docker"));
            assertNotNull(dialog.findItem(buildRoot, "Kubernetes"));
            assertNotNull(dialog.findItem(buildRoot, "Profilers"));
            assertNotNull(dialog.findItem(buildRoot, "Remote Jar Repositories"));
            assertNotNull(dialog.findItem(buildRoot, "Run Targets"));

            // Select and verify routing
            dialog.selectCategory("Build, Execution, Deployment");
            dialog.selectCategory("Python Debugger");
            assertNotNull(dialog.getCurrentPythonDebuggerPage());

            dialog.selectCategory("Application Servers");
            assertNotNull(dialog.getCurrentApplicationServersPage());

            dialog.selectCategory("Build Tools");
            assertNotNull(dialog.getCurrentBuildToolsPage());

            dialog.selectCategory("Maven");
            assertNotNull(dialog.getCurrentMavenPage());
        });
    }
}
