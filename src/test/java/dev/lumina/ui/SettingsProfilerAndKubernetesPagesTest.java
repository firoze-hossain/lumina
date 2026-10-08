package dev.lumina.ui;

import dev.lumina.kubernetes.KubernetesSettings;
import dev.lumina.kubernetes.KubernetesSettingsManager;
import dev.lumina.profiler.JavaProfilerConfig;
import dev.lumina.profiler.JavaProfilerSettingsManager;
import dev.lumina.profiler.ProfilerFilterGroup;
import dev.lumina.util.Settings;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UI lifecycle tests for Java Profiler, Filters, and Kubernetes settings pages:
 * - SettingsJavaProfilerPage
 * - SettingsJavaProfilerFiltersPage
 * - SettingsBuildKubernetesPage
 * - SettingsDialog integration and routing
 */
public class SettingsProfilerAndKubernetesPagesTest {

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
        JavaProfilerSettingsManager.getInstance().setProfilers(List.of(JavaProfilerSettingsManager.createDefaultProfiler()));
        JavaProfilerSettingsManager.getInstance().resetFilterGroupsToDefaults();
        KubernetesSettingsManager.getInstance().resetToDefaults();
    }

    @Test
    void testJavaProfilerPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsJavaProfilerPage page = new SettingsJavaProfilerPage();
                assertFalse(page.isModified());

                assertEquals("IntelliJ Profiler", page.getNameField().getText());
                assertEquals("event=wall,interval=10ms,jfrsync=profile", page.getAgentOptionsField().getText());
                assertEquals("Bundled (Version: 4.1)", page.getAgentPathField().getText());
                assertFalse(page.getCollectNativeCallsCheck().isSelected());

                // Modify name
                AtomicBoolean modifiedNotified = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedNotified.set(true));

                page.getNameField().setText("High Performance Profiler");
                assertTrue(page.isModified());
                assertTrue(modifiedNotified.get());

                // Apply
                page.apply();
                assertFalse(page.isModified());
                assertEquals("High Performance Profiler", JavaProfilerSettingsManager.getInstance().getProfilers().get(0).getName());

                // Reset
                page.getNameField().setText("Temporary Change");
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());
                assertEquals("High Performance Profiler", page.getNameField().getText());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testJavaProfilerFiltersPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsJavaProfilerFiltersPage page = new SettingsJavaProfilerFiltersPage();
                assertFalse(page.isModified());

                assertEquals(14, page.getFilterList().size());
                assertEquals("Java", page.getFilterList().get(0).getGroupName());

                // Modify an item
                AtomicBoolean modifiedNotified = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedNotified.set(true));

                page.getFilterList().add(new ProfilerFilterGroup("CustomGroup", "com.custom.*"));
                assertTrue(page.isModified());
                assertTrue(modifiedNotified.get());

                // Apply
                page.apply();
                assertFalse(page.isModified());
                assertEquals(15, JavaProfilerSettingsManager.getInstance().getFilterGroups().size());

                // Reset to defaults
                page.getResetButton().fire();
                assertTrue(page.isModified());
                page.apply();
                assertEquals(14, JavaProfilerSettingsManager.getInstance().getFilterGroups().size());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testBuildKubernetesPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsBuildKubernetesPage page = new SettingsBuildKubernetesPage();
                assertFalse(page.isModified());

                // Verify initial values
                assertEquals("kubectl", page.getKubectlPathField().getText());
                assertEquals("helm", page.getHelmPathField().getText());
                assertTrue(page.getReloadConfigAutoCheck().isSelected());
                assertTrue(page.getRefreshClusterResourcesCheck().isSelected());
                assertEquals("/bin/sh", page.getShellCommandCombo().getValue());
                assertEquals("Always Show", page.getFloatingToolbarCombo().getValue());
                assertTrue(page.getPathDownloadLogsRadio().isSelected());
                assertEquals("300", page.getLogCacheSizeField().getText());
                assertEquals(3, page.getLogFiltersList().size());

                // Modify
                AtomicBoolean modifiedNotified = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedNotified.set(true));

                page.getKubectlPathField().setText("/usr/bin/kubectl");
                assertTrue(page.isModified());
                assertTrue(modifiedNotified.get());

                page.getShellCommandCombo().setValue("/bin/bash");
                page.getLogCacheSizeField().setText("600");

                // Apply
                page.apply();
                assertFalse(page.isModified());

                KubernetesSettings saved = KubernetesSettingsManager.getInstance().getSettings();
                assertEquals("/usr/bin/kubectl", saved.getKubectlPath());
                assertEquals("/bin/bash", saved.getShellCommand());
                assertEquals(600, saved.getLogCacheSizeMb());

                // Reset
                page.getKubectlPathField().setText("/tmp/fake-kubectl");
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());
                assertEquals("/usr/bin/kubectl", page.getKubectlPathField().getText());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSettingsDialogRoutingForProfilerAndKubernetes() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDialog dialog = new SettingsDialog(null);

                // 1. Find Build, Execution, Deployment
                TreeItem<String> buildNode = dialog.findItem(dialog.getTree().getRoot(), "Build, Execution, Deployment");
                assertNotNull(buildNode, "Build, Execution, Deployment category node should exist");

                // 2. Select Java Profiler
                TreeItem<String> profilerItem = dialog.findItem(buildNode, "Java Profiler");
                assertNotNull(profilerItem, "Java Profiler node should exist under Build, Execution, Deployment");
                dialog.getTree().getSelectionModel().select(profilerItem);
                assertNotNull(dialog.getCurrentJavaProfilerPage());

                // 3. Select Filters under Java Profiler
                TreeItem<String> filtersItem = dialog.findItem(profilerItem, "Filters");
                assertNotNull(filtersItem, "Filters node should exist under Java Profiler");
                dialog.getTree().getSelectionModel().select(filtersItem);
                assertNotNull(dialog.getCurrentJavaProfilerFiltersPage());

                // 4. Select Kubernetes under Build, Execution, Deployment
                TreeItem<String> k8sItem = dialog.findItem(buildNode, "Kubernetes");
                assertNotNull(k8sItem, "Kubernetes node should exist under Build, Execution, Deployment");
                dialog.getTree().getSelectionModel().select(k8sItem);
                assertNotNull(dialog.getCurrentBuildKubernetesPage());

                // 5. Test compatibility alias for "Profilers"
                TreeItem<String> aliasItem = dialog.findItem(buildNode, "Profilers");
                assertNotNull(aliasItem, "Profilers alias should resolve for backward compatibility");
                assertEquals("Java Profiler", aliasItem.getValue());

            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
