package dev.lumina.ui;

import dev.lumina.build.RemoteJarRepositoriesSettingsManager;
import dev.lumina.run.RunTargetConfig;
import dev.lumina.run.RunTargetType;
import dev.lumina.run.RunTargetsSettingsManager;
import dev.lumina.util.Settings;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UI lifecycle tests for Remote Jar Repositories and Run Targets settings pages:
 * - SettingsRemoteJarRepositoriesPage
 * - SettingsRunTargetsPage
 * - SettingsDialog routing
 */
public class SettingsRemoteJarAndRunTargetsPagesTest {

    private static volatile boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            String display = System.getenv("DISPLAY");
            if (display == null || display.isBlank() || java.awt.GraphicsEnvironment.isHeadless()) {
                return;
            }
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(() -> {
                javaFxAvailable = true;
                latch.countDown();
            });
            latch.await(3, TimeUnit.SECONDS);
        } catch (Throwable ignored) {
            javaFxAvailable = false;
        }
    }

    @BeforeEach
    void setUp() {
        Settings.clear();
        RemoteJarRepositoriesSettingsManager.getInstance().resetAllToDefaults();
        RunTargetsSettingsManager.getInstance().resetToDefaults();
    }

    @Test
    void testRemoteJarRepositoriesPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsRemoteJarRepositoriesPage page = new SettingsRemoteJarRepositoriesPage();
                assertFalse(page.isModified());

                assertEquals(3, page.getMavenReposList().size());
                assertEquals(2, page.getNexusUrlsList().size());

                // Modify
                AtomicBoolean modifiedNotified = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedNotified.set(true));

                page.getMavenReposList().add("https://custom.repo.org/maven2");
                assertTrue(page.isModified());
                assertTrue(modifiedNotified.get());

                // Apply
                page.apply();
                assertFalse(page.isModified());
                assertTrue(RemoteJarRepositoriesSettingsManager.getInstance().getMavenRepositories().contains("https://custom.repo.org/maven2"));

                // Reset
                page.getNexusUrlsList().add("https://custom.nexus.org/service/local/");
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());
                assertEquals(2, page.getNexusUrlsList().size());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testRunTargetsPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsRunTargetsPage page = new SettingsRunTargetsPage();
                assertFalse(page.isModified());

                // Empty state initially
                assertTrue(page.getTargetsList().isEmpty());
                assertTrue(page.getAddTargetLink().isVisible());

                // Add a target
                AtomicBoolean modifiedNotified = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedNotified.set(true));

                RunTargetConfig ssh = new RunTargetConfig("Production SSH", RunTargetType.SSH);
                ssh.setHost("prod.server.internal");
                page.getTargetsList().add(ssh);
                page.getTargetsListView().getSelectionModel().select(ssh);

                assertTrue(page.isModified());
                assertTrue(modifiedNotified.get());
                assertEquals("Production SSH", page.getNameField().getText());
                assertEquals("prod.server.internal", page.getSshHostField().getText());

                // Apply
                page.apply();
                assertFalse(page.isModified());
                assertEquals(1, RunTargetsSettingsManager.getInstance().getTargets().size());

                // Select as default target
                page.getDefaultTargetCombo().setValue("🔌 Production SSH");
                assertTrue(page.isModified());
                page.apply();
                assertFalse(page.isModified());
                assertEquals(ssh.getId(), RunTargetsSettingsManager.getInstance().getProjectDefaultTargetId());

                // Reset
                page.getNameField().setText("Temporary Change");
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());
                assertEquals("Production SSH", page.getNameField().getText());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSettingsDialogRoutingForRemoteJarAndRunTargets() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDialog dialog = new SettingsDialog(null);

                TreeItem<String> buildNode = dialog.findItem(dialog.getTree().getRoot(), "Build, Execution, Deployment");
                assertNotNull(buildNode);

                // 1. Remote Jar Repositories
                TreeItem<String> remoteJarItem = dialog.findItem(buildNode, "Remote Jar Repositories");
                assertNotNull(remoteJarItem);
                dialog.getTree().getSelectionModel().select(remoteJarItem);
                assertNotNull(dialog.getCurrentRemoteJarRepositoriesPage());

                // 2. Run Targets
                TreeItem<String> runTargetsItem = dialog.findItem(buildNode, "Run Targets");
                assertNotNull(runTargetsItem);
                dialog.getTree().getSelectionModel().select(runTargetsItem);
                assertNotNull(dialog.getCurrentRunTargetsPage());

            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
