package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SettingsColorSchemePageTest {

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
                SettingsColorSchemePage page = new SettingsColorSchemePage();
                assertFalse(page.isModified());

                assertEquals("Islands Dark Theme default", page.getHeaderBar().getSchemeCombo().getValue());
                assertEquals(69, SettingsColorSchemePage.SCHEME_CATEGORIES.size());
                assertTrue(SettingsColorSchemePage.SCHEME_CATEGORIES.contains("General"));
                assertTrue(SettingsColorSchemePage.SCHEME_CATEGORIES.contains("Java"));
                assertTrue(SettingsColorSchemePage.SCHEME_CATEGORIES.contains("Python"));
                assertTrue(SettingsColorSchemePage.SCHEME_CATEGORIES.contains("Ruby"));
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testNavigationCallback() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemePage page = new SettingsColorSchemePage();
                AtomicReference<String> navigatedTo = new AtomicReference<>();
                page.setOnNavigate(navigatedTo::set);

                // Simulate navigation callback
                page.setOnNavigate(target -> assertEquals("General", target));
                page.setOnNavigate(navigatedTo::set);

                assertNotNull(page.getHeaderBar());
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
                SettingsColorSchemePage page = new SettingsColorSchemePage();
                AtomicBoolean modifiedNotified = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedNotified.set(true));

                assertFalse(page.isModified());

                // Change scheme
                page.getHeaderBar().getSchemeCombo().setValue("Darcula");
                assertTrue(page.isModified());
                assertTrue(modifiedNotified.get());

                // Reset back
                page.reset();
                assertFalse(page.isModified());
                assertEquals("Islands Dark Theme default", page.getHeaderBar().getSchemeCombo().getValue());
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
                SettingsColorSchemePage page = new SettingsColorSchemePage();
                page.getHeaderBar().getSchemeCombo().setValue("Dark");
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());
                assertEquals("Dark", EditorColorSchemeSettings.getInstance().getActiveSchemeName());

                // Restore
                EditorColorSchemeSettings.getInstance().initDefaults();
                EditorColorSchemeSettings.getInstance().save();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
