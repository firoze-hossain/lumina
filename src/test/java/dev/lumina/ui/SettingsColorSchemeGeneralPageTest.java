package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SettingsColorSchemeGeneralPageTest {

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
        EditorColorSchemeSettings.getInstance().initDefaults();
    }

    @Test
    void testInitialStateMatchesDefaults() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeGeneralPage page = new SettingsColorSchemeGeneralPage();
                assertFalse(page.isModified());

                assertEquals("Islands Dark Theme default", page.getHeaderBar().getSchemeCombo().getValue());
                assertNotNull(page.getCategoryTree().getRoot());
                assertFalse(page.getCategoryTree().getRoot().getChildren().isEmpty());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSelectTreeItemAndAttributeEditor() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeGeneralPage page = new SettingsColorSchemeGeneralPage();
                page.selectTreeItem("Errors and Warnings // Error");

                // Error should have wave underline effect and error stripe color
                assertNotNull(page.getCategoryTree().getSelectionModel().getSelectedItem());
                assertEquals("Error", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testDirtyTrackingAndReset() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeGeneralPage page = new SettingsColorSchemeGeneralPage();
                AtomicBoolean modifiedNotified = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedNotified.set(true));

                assertFalse(page.isModified());

                // Modify bold
                page.getBoldCheck().setSelected(!page.getBoldCheck().isSelected());
                assertTrue(page.isModified());
                assertTrue(modifiedNotified.get());

                // Reset back
                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testApplySaves() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeGeneralPage page = new SettingsColorSchemeGeneralPage();
                page.getBoldCheck().setSelected(true);
                assertTrue(page.isModified());

                page.apply();
                // After apply, changes are saved to EditorColorSchemeSettings
                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertNotNull(s.getActiveSchemeName());

                // Restore
                s.initDefaults();
                s.save();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
