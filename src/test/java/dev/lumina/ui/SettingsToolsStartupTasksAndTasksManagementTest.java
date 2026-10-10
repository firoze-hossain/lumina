package dev.lumina.ui;

import dev.lumina.tools.*;
import dev.lumina.util.Settings;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for Tools > Startup Tasks, Tasks, Servers, and Time Tracking.
 * Validates:
 * 1. POJO data models, default values, deep cloning, equality, and hash code.
 * 2. Configuration managers, Settings persistence, and change listener notifications.
 * 3. JavaFX UI pages lifecycle (modification detection, apply, reset, revert).
 * 4. SettingsDialog category tree hierarchy and navigation integration.
 * 5. Strict brand isolation (0 competitor keywords in source code).
 */
public class SettingsToolsStartupTasksAndTasksManagementTest {

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

    // ------------------------------------------------------------- 1. Startup Tasks

    @Test
    void testStartupTaskEntryModel() {
        StartupTaskEntry entry = new StartupTaskEntry("App Run", "Application", false);
        assertEquals("App Run", entry.getName());
        assertEquals("Application", entry.getConfigurationType());
        assertFalse(entry.isShared());

        StartupTaskEntry clone = entry.clone();
        assertEquals(entry, clone);
        assertEquals(entry.hashCode(), clone.hashCode());

        clone.setShared(true);
        assertTrue(clone.isShared());
        assertNotEquals(entry, clone);
        assertTrue(clone.toString().contains("App Run"));
    }

    @Test
    void testStartupTasksSettingsAndManager() {
        StartupTasksSettingsManager manager = StartupTasksSettingsManager.getInstance();
        assertNotNull(manager);

        StartupTasksSettings original = manager.getSettings();
        assertNotNull(original);

        StartupTasksSettings modified = new StartupTasksSettings();
        modified.getTasks().add(new StartupTaskEntry("Docker Dev", "Docker", true));
        modified.getTasks().add(new StartupTaskEntry("npm test", "npm", false));

        AtomicBoolean listenerCalled = new AtomicBoolean(false);
        Runnable listener = () -> listenerCalled.set(true);
        manager.addChangeListener(listener);

        try {
            manager.setSettings(modified);
            assertTrue(listenerCalled.get(), "Listener should be triggered on settings update");

            StartupTasksSettings loaded = manager.getSettings();
            assertEquals(2, loaded.getTasks().size());
            assertEquals("Docker Dev", loaded.getTasks().get(0).getName());
            assertTrue(loaded.getTasks().get(0).isShared());
            assertEquals("npm test", loaded.getTasks().get(1).getName());
            assertFalse(loaded.getTasks().get(1).isShared());
        } finally {
            manager.removeChangeListener(listener);
            manager.setSettings(original);
        }
    }

    @Test
    void testStartupTasksUiPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsToolsStartupTasksPage page = new SettingsToolsStartupTasksPage();
            assertNotNull(page);
            assertFalse(page.isModified());

            AtomicBoolean modifiedTriggered = new AtomicBoolean(false);
            page.setOnModified(() -> modifiedTriggered.set(true));

            // Add task via table
            page.getTableData().add(new StartupTaskEntry("Test Task", "Shell Script", false));
            assertTrue(page.isModified());

            // Revert changes
            page.revertChanges();
            assertFalse(page.isModified());
        });
    }

    // ------------------------------------------------------------- 2. Tasks General

    @Test
    void testTasksGeneralSettingsModel() {
        TasksGeneralSettings settings = new TasksGeneralSettings();
        assertEquals("${id} ${summary}", settings.getChangelistNameFormat());
        assertEquals("${id}", settings.getFeatureBranchNameFormat());
        assertFalse(settings.isLowercased());
        assertEquals("-", settings.getReplaceSpacesWith());
        assertEquals(50, settings.getTaskHistoryLength());
        assertEquals(5000, settings.getConnectionTimeoutMs());
        assertFalse(settings.isShowTaskWidgetIfNoActiveTasks());
        assertTrue(settings.isSaveContextOnCommit());
        assertTrue(settings.isEnableCache());
        assertEquals(100, settings.getUpdateIssuesCount());
        assertEquals(20, settings.getCacheIntervalMinutes());

        TasksGeneralSettings clone = settings.clone();
        assertEquals(settings, clone);
        assertEquals(settings.hashCode(), clone.hashCode());

        clone.setLowercased(true);
        clone.setReplaceSpacesWith("_");
        clone.setTaskHistoryLength(100);
        clone.setConnectionTimeoutMs(10000);
        clone.setShowTaskWidgetIfNoActiveTasks(true);
        clone.setSaveContextOnCommit(false);
        clone.setEnableCache(false);
        clone.setUpdateIssuesCount(200);
        clone.setCacheIntervalMinutes(30);

        assertNotEquals(settings, clone);
        assertTrue(clone.isLowercased());
        assertEquals("_", clone.getReplaceSpacesWith());
        assertEquals(100, clone.getTaskHistoryLength());
        assertEquals(10000, clone.getConnectionTimeoutMs());
        assertTrue(clone.isShowTaskWidgetIfNoActiveTasks());
        assertFalse(clone.isSaveContextOnCommit());
        assertFalse(clone.isEnableCache());
        assertEquals(200, clone.getUpdateIssuesCount());
        assertEquals(30, clone.getCacheIntervalMinutes());
    }

    @Test
    void testTasksGeneralSettingsManagerLifecycle() {
        TasksGeneralSettingsManager manager = TasksGeneralSettingsManager.getInstance();
        assertNotNull(manager);

        TasksGeneralSettings original = manager.getSettings();
        TasksGeneralSettings test = original.clone();
        test.setChangelistNameFormat("TASK-${id} ${summary}");
        test.setLowercased(true);
        test.setTaskHistoryLength(75);

        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addChangeListener(listener);

        try {
            manager.setSettings(test);
            assertTrue(notified.get());

            TasksGeneralSettings loaded = manager.getSettings();
            assertEquals("TASK-${id} ${summary}", loaded.getChangelistNameFormat());
            assertTrue(loaded.isLowercased());
            assertEquals(75, loaded.getTaskHistoryLength());
        } finally {
            manager.removeChangeListener(listener);
            manager.setSettings(original);
        }
    }

    @Test
    void testTasksGeneralUiPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsToolsTasksPage page = new SettingsToolsTasksPage();
            assertNotNull(page);
            assertFalse(page.isModified());

            AtomicBoolean modifiedNotified = new AtomicBoolean(false);
            page.setOnModified(() -> modifiedNotified.set(true));

            page.getChangelistFormatField().setText("NEW-${id}");
            assertTrue(page.isModified());
            assertTrue(modifiedNotified.get());

            page.revertChanges();
            assertFalse(page.isModified());
        });
    }

    // ------------------------------------------------------------- 3. Task Servers

    @Test
    void testTaskServerEntryAndModel() {
        TaskServerEntry custom = new TaskServerEntry("Bugzilla", "Custom Bug Tracker", "https://bugs.example.com");
        custom.setUsername("admin");
        custom.setPassword("secret");
        custom.setShareUrl(true);
        custom.setCommitMessageFormat("{id}: {summary}");
        custom.setUseHttpAuthentication(true);
        custom.setHttpUsername("http_user");
        custom.setHttpPassword("http_pass");

        assertEquals("Custom Bug Tracker", custom.getName());
        assertEquals("Bugzilla", custom.getServerType());
        assertEquals("https://bugs.example.com", custom.getUrl());
        assertEquals("admin", custom.getUsername());
        assertEquals("secret", custom.getPassword());
        assertTrue(custom.isShareUrl());
        assertEquals("{id}: {summary}", custom.getCommitMessageFormat());
        assertTrue(custom.isUseHttpAuthentication());
        assertEquals("http_user", custom.getHttpUsername());
        assertEquals("http_pass", custom.getHttpPassword());

        TaskServerEntry clone = custom.clone();
        assertEquals(custom, clone);
        assertEquals(custom.hashCode(), clone.hashCode());
    }

    @Test
    void testTaskServersSettingsManagerLifecycle() {
        TaskServersSettingsManager manager = TaskServersSettingsManager.getInstance();
        assertNotNull(manager);

        TaskServersSettings original = manager.getSettings();
        TaskServersSettings test = new TaskServersSettings();
        test.getServers().add(new TaskServerEntry("GitHub", "My GitHub", "https://api.github.com"));

        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addChangeListener(listener);

        try {
            manager.setSettings(test);
            assertTrue(notified.get());

            TaskServersSettings loaded = manager.getSettings();
            assertEquals(1, loaded.getServers().size());
            assertEquals("My GitHub", loaded.getServers().get(0).getName());
            assertEquals("https://api.github.com", loaded.getServers().get(0).getUrl());
        } finally {
            manager.removeChangeListener(listener);
            manager.setSettings(original);
        }
    }

    @Test
    void testTaskServersUiPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsToolsTaskServersPage page = new SettingsToolsTaskServersPage();
            assertNotNull(page);
            assertFalse(page.isModified());

            AtomicBoolean modifiedNotified = new AtomicBoolean(false);
            page.setOnModified(() -> modifiedNotified.set(true));

            // Add server
            page.getServersList().add(new TaskServerEntry("JIRA", "Test JIRA", "https://jira.example.com"));
            assertTrue(page.isModified());

            page.revertChanges();
            assertFalse(page.isModified());
        });
    }

    // ------------------------------------------------------------- 4. Time Tracking

    @Test
    void testTimeTrackingSettingsModel() {
        TimeTrackingSettings settings = new TimeTrackingSettings();
        assertFalse(settings.isEnableTimeTracking());
        assertEquals(600, settings.getSuspendDelaySeconds());

        TimeTrackingSettings clone = settings.clone();
        assertEquals(settings, clone);
        assertEquals(settings.hashCode(), clone.hashCode());

        clone.setEnableTimeTracking(true);
        clone.setSuspendDelaySeconds(300);
        assertNotEquals(settings, clone);
        assertTrue(clone.isEnableTimeTracking());
        assertEquals(300, clone.getSuspendDelaySeconds());
    }

    @Test
    void testTimeTrackingSettingsManagerLifecycle() {
        TimeTrackingSettingsManager manager = TimeTrackingSettingsManager.getInstance();
        assertNotNull(manager);

        TimeTrackingSettings original = manager.getSettings();
        TimeTrackingSettings test = new TimeTrackingSettings();
        test.setEnableTimeTracking(true);
        test.setSuspendDelaySeconds(900);

        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addChangeListener(listener);

        try {
            manager.setSettings(test);
            assertTrue(notified.get());

            TimeTrackingSettings loaded = manager.getSettings();
            assertTrue(loaded.isEnableTimeTracking());
            assertEquals(900, loaded.getSuspendDelaySeconds());
        } finally {
            manager.removeChangeListener(listener);
            manager.setSettings(original);
        }
    }

    @Test
    void testTimeTrackingUiPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsToolsTimeTrackingPage page = new SettingsToolsTimeTrackingPage();
            assertNotNull(page);
            assertFalse(page.isModified());

            AtomicBoolean modifiedNotified = new AtomicBoolean(false);
            page.setOnModified(() -> modifiedNotified.set(true));

            page.getEnableTimeTrackingCheck().setSelected(true);
            assertTrue(page.isModified());
            assertTrue(modifiedNotified.get());

            page.revertChanges();
            assertFalse(page.isModified());
        });
    }

    // ------------------------------------------------------------- 5. SettingsDialog Hierarchy & Navigation

    @Test
    void testSettingsDialogTreeAndNavigation() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsDialog dialog = new SettingsDialog(null);
            assertNotNull(dialog);

            // Locate Tools category in tree
            TreeItem<String> root = dialog.getTree().getRoot();
            assertNotNull(root);

            TreeItem<String> toolsItem = null;
            for (TreeItem<String> child : root.getChildren()) {
                if ("Tools".equals(child.getValue())) {
                    toolsItem = child;
                    break;
                }
            }
            assertNotNull(toolsItem, "Tools node should exist in SettingsDialog category tree");

            // Verify Startup Tasks exists under Tools
            TreeItem<String> startupTasksItem = null;
            TreeItem<String> tasksItem = null;
            for (TreeItem<String> child : toolsItem.getChildren()) {
                if ("Startup Tasks".equals(child.getValue())) {
                    startupTasksItem = child;
                } else if ("Tasks".equals(child.getValue())) {
                    tasksItem = child;
                }
            }
            assertNotNull(startupTasksItem, "Startup Tasks should be a child of Tools");
            assertNotNull(tasksItem, "Tasks should be a child of Tools");

            // Verify Tasks has children Servers and Time Tracking
            TreeItem<String> serversItem = null;
            TreeItem<String> timeTrackingItem = null;
            for (TreeItem<String> child : tasksItem.getChildren()) {
                if ("Servers".equals(child.getValue())) {
                    serversItem = child;
                } else if ("Time Tracking".equals(child.getValue())) {
                    timeTrackingItem = child;
                }
            }
            assertNotNull(serversItem, "Servers should be a child of Tasks");
            assertNotNull(timeTrackingItem, "Time Tracking should be a child of Tasks");

            // Test navigation to Startup Tasks
            dialog.selectCategory("Tools", "Startup Tasks");
            assertNotNull(dialog.getCurrentToolsStartupTasksPage());

            // Test navigation to Tasks
            dialog.selectCategory("Tools", "Tasks");
            assertNotNull(dialog.getCurrentToolsTasksPage());

            // Test navigation to Servers
            dialog.selectCategory("Tasks", "Servers");
            assertNotNull(dialog.getCurrentToolsTaskServersPage());

            // Test navigation to Time Tracking
            dialog.selectCategory("Tasks", "Time Tracking");
            assertNotNull(dialog.getCurrentToolsTimeTrackingPage());
        });
    }

    // ------------------------------------------------------------- 6. Strict Brand Isolation

    @Test
    void testStrictBrandIsolation() throws Exception {
        String[] filesToCheck = {
                "src/main/java/dev/lumina/tools/StartupTaskEntry.java",
                "src/main/java/dev/lumina/tools/StartupTasksSettings.java",
                "src/main/java/dev/lumina/tools/StartupTasksSettingsManager.java",
                "src/main/java/dev/lumina/tools/TasksGeneralSettings.java",
                "src/main/java/dev/lumina/tools/TasksGeneralSettingsManager.java",
                "src/main/java/dev/lumina/tools/TaskServerEntry.java",
                "src/main/java/dev/lumina/tools/TaskServersSettings.java",
                "src/main/java/dev/lumina/tools/TaskServersSettingsManager.java",
                "src/main/java/dev/lumina/tools/TimeTrackingSettings.java",
                "src/main/java/dev/lumina/tools/TimeTrackingSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsToolsStartupTasksPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsTasksPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsTaskServersPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsTimeTrackingPage.java"
        };

        String[] forbiddenKeywords = {
                "Jet" + "Brains",
                "Intelli" + "J",
                "Py" + "Charm",
                "Web" + "Storm",
                "Php" + "Storm",
                "Go" + "Land",
                "CLion",
                "Data" + "Grip",
                "Fleet",
                "Rider"
        };

        for (String relPath : filesToCheck) {
            String content = Files.readString(Paths.get(relPath));
            for (String kw : forbiddenKeywords) {
                assertFalse(content.contains(kw),
                        "File " + relPath + " must not contain forbidden competitor keyword: " + kw);
            }
        }
    }
}
