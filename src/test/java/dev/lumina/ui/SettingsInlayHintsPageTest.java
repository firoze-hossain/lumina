package dev.lumina.ui;

import dev.lumina.inlay.InlayHintsManager;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class SettingsInlayHintsPageTest {

    private static volatile boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(() -> {
                javaFxAvailable = true;
                latch.countDown();
            });
            latch.await(3, TimeUnit.SECONDS);
        } catch (IllegalStateException e) {
            javaFxAvailable = true;
        } catch (Throwable t) {
            javaFxAvailable = false;
        }
    }

    @BeforeEach
    void setUp() {
        InlayHintsManager.getInstance().resetToDefaults();
    }

    @Test
    void testInlayHintsPageStructureAndTree() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsInlayHintsPage page = new SettingsInlayHintsPage();
                assertNotNull(page);
                assertFalse(page.isModified());

                var root = page.getTreeView().getRoot();
                assertNotNull(root);
                assertEquals(9, root.getChildren().size());

                // Verify the 9 main categories matching screenshots
                assertEquals("Code vision", root.getChildren().get(0).getValue().getTitle());
                assertEquals("Parameter names", root.getChildren().get(1).getValue().getTitle());
                assertEquals("Types", root.getChildren().get(2).getValue().getTitle());
                assertEquals("Values", root.getChildren().get(3).getValue().getTitle());
                assertEquals("Annotations", root.getChildren().get(4).getValue().getTitle());
                assertEquals("Method chains", root.getChildren().get(5).getValue().getTitle());
                assertEquals("Lambdas", root.getChildren().get(6).getValue().getTitle());
                assertEquals("URL path", root.getChildren().get(7).getValue().getTitle());
                assertEquals("Other", root.getChildren().get(8).getValue().getTitle());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testInlayHintsPageModificationAndLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsInlayHintsPage page = new SettingsInlayHintsPage();
                assertFalse(page.isModified());

                boolean[] modifiedFired = {false};
                page.setOnModifiedListener(() -> modifiedFired[0] = true);

                // Modify a setting
                page.getWorkingSettings().setCodeVisionDefaultPosition("Top");
                page.getWorkingSettings().setCodeVisionMaxAbove(8);

                assertTrue(page.isModified());

                // Apply
                page.apply();
                assertFalse(page.isModified());
                assertEquals("Top", InlayHintsManager.getInstance().getSettings().getCodeVisionDefaultPosition());
                assertEquals(8, InlayHintsManager.getInstance().getSettings().getCodeVisionMaxAbove());

                // Modify again and reset
                page.getWorkingSettings().setCodeVisionMaxAbove(15);
                assertTrue(page.isModified());

                page.reset();
                assertFalse(page.isModified());
                assertEquals(8, page.getWorkingSettings().getCodeVisionMaxAbove());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
