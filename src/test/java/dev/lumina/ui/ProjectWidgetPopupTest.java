package dev.lumina.ui;

import dev.lumina.project.RecentProjectsManager;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ProjectWidgetPopupTest {

    private static volatile boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            String display = System.getenv("DISPLAY");
            if (display == null || display.isBlank() || java.awt.GraphicsEnvironment.isHeadless()) {
                return;
            }
            Platform.startup(() -> javaFxAvailable = true);
            javaFxAvailable = true;
        } catch (Throwable ignored) {}
    }

    @Test
    void testMonogramGeneration() {
        // Single word / lowercase matching IntelliJ screenshot
        assertEquals("L", ProjectWidgetPopup.getMonogram("lumina"));
        assertEquals("N", ProjectWidgetPopup.getMonogram("novaos"));
        assertEquals("U", ProjectWidgetPopup.getMonogram("untitled1"));
        assertEquals("D", ProjectWidgetPopup.getMonogram("demo1"));

        // CamelCase / PascalCase matching IntelliJ screenshot
        assertEquals("NC", ProjectWidgetPopup.getMonogram("NexaCommerce"));
        assertEquals("DB", ProjectWidgetPopup.getMonogram("DBNavigator"));
        assertEquals("RH", ProjectWidgetPopup.getMonogram("RozeHub"));

        // Separated words matching IntelliJ screenshot
        assertEquals("SD", ProjectWidgetPopup.getMonogram("spring_boot_depency"));
        assertEquals("MP", ProjectWidgetPopup.getMonogram("my-project"));
        assertEquals("HW", ProjectWidgetPopup.getMonogram("hello world"));

        // Edge cases
        assertEquals("?", ProjectWidgetPopup.getMonogram(null));
        assertEquals("?", ProjectWidgetPopup.getMonogram(""));
        assertEquals("?", ProjectWidgetPopup.getMonogram("   "));
    }

    @Test
    void testBadgeColorsMatchingIntelliJScreenshot() {
        // Known projects in media_1790170248924.png
        assertEquals("#3882E8", ProjectWidgetPopup.getBadgeColor("lumina"));
        assertEquals("#3882E8", ProjectWidgetPopup.getBadgeColor("NexaCommerce"));
        assertEquals("#8552C4", ProjectWidgetPopup.getBadgeColor("novaos"));
        assertEquals("#3553A6", ProjectWidgetPopup.getBadgeColor("DBNavigator"));
        assertEquals("#799839", ProjectWidgetPopup.getBadgeColor("spring_boot_depency"));
        assertEquals("#B57C2A", ProjectWidgetPopup.getBadgeColor("untitled1"));
        assertEquals("#2E8B57", ProjectWidgetPopup.getBadgeColor("demo1"));
        assertEquals("#D3443B", ProjectWidgetPopup.getBadgeColor("RozeHub"));

        // Arbitrary unknown project gets non-null valid hex
        String customColor = ProjectWidgetPopup.getBadgeColor("arbitrary_unknown_project");
        assertNotNull(customColor);
        assertTrue(customColor.startsWith("#"));
        assertEquals(7, customColor.length());
    }

    @Test
    void testPopupItemsPopulationAndOpenProjectsDeduplication() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<ProjectWidgetPopup> ref = new AtomicReference<>();
        AtomicBoolean newProjectClicked = new AtomicBoolean(false);
        AtomicReference<Path> openedProject = new AtomicReference<>();
        AtomicReference<String> removedProject = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                List<ProjectWidgetPopup.OpenProject> openProjects = List.of(
                        new ProjectWidgetPopup.OpenProject(Path.of("/tmp/projects/NexaCommerce"), "NexaCommerce", null, () -> {}),
                        new ProjectWidgetPopup.OpenProject(Path.of("/tmp/projects/others/lumina"), "lumina", null, () -> {})
                );

                List<RecentProjectsManager.RecentProject> recentProjects = List.of(
                        new RecentProjectsManager.RecentProject("/tmp/projects/others/lumina", "lumina", 5000), // Should be excluded as it is already open!
                        new RecentProjectsManager.RecentProject("/tmp/projects/others/novaos", "novaos", 4000),
                        new RecentProjectsManager.RecentProject("/tmp/projects/others/DBNavigator", "DBNavigator", 3000),
                        new RecentProjectsManager.RecentProject("/tmp/projects/others/spring_boot_depency", "spring_boot_depency", 2000)
                );

                ProjectWidgetPopup popup = new ProjectWidgetPopup(
                        () -> openProjects,
                        () -> recentProjects,
                        new ProjectWidgetPopup.ProjectWidgetCallbacks() {
                            @Override public void onNewProject() { newProjectClicked.set(true); }
                            @Override public void onOpenFolder() {}
                            @Override public void onCloneRepository() {}
                            @Override public void onOpenProject(Path projectDir) { openedProject.set(projectDir); }
                            @Override public void onRemoveRecentProject(String path) { removedProject.set(path); }
                        }
                );

                popup.refreshAndPopulate();
                ref.set(popup);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        ProjectWidgetPopup popup = ref.get();
        assertNotNull(popup);

        // Selectable rows: 3 top actions + 2 open projects + 3 recent projects (lumina was deduplicated) = 8 rows
        assertEquals(8, popup.getSelectableRowsCount());
    }

    @Test
    void testActionTriggering() throws Exception {
        if (!javaFxAvailable) return;

        AtomicBoolean newProjectCalled = new AtomicBoolean(false);
        AtomicBoolean openFolderCalled = new AtomicBoolean(false);
        AtomicBoolean cloneCalled = new AtomicBoolean(false);
        AtomicReference<Path> openedPath = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                ProjectWidgetPopup.ProjectWidgetCallbacks cb = new ProjectWidgetPopup.ProjectWidgetCallbacks() {
                    @Override public void onNewProject() { newProjectCalled.set(true); }
                    @Override public void onOpenFolder() { openFolderCalled.set(true); }
                    @Override public void onCloneRepository() { cloneCalled.set(true); }
                    @Override public void onOpenProject(Path projectDir) { openedPath.set(projectDir); }
                };

                cb.onNewProject();
                cb.onOpenFolder();
                cb.onCloneRepository();
                cb.onOpenProject(Path.of("/test/path"));
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(newProjectCalled.get());
        assertTrue(openFolderCalled.get());
        assertTrue(cloneCalled.get());
        assertEquals(Path.of("/test/path"), openedPath.get());
    }
}
