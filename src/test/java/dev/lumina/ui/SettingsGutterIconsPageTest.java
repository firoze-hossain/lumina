package dev.lumina.ui;

import dev.lumina.settings.GutterIconsSettings;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SettingsGutterIconsPageTest {

    private static volatile boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            String display = System.getenv("DISPLAY");
            if (display == null || display.isBlank() || GraphicsEnvironment.isHeadless()) {
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
        GutterIconsSettings.getInstance().initDefaults();
    }

    @Test
    void testPageInstantiationAndDefaults() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<SettingsGutterIconsPage> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                ref.set(new SettingsGutterIconsPage());
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        SettingsGutterIconsPage page = ref.get();
        assertNotNull(page);
        assertFalse(page.isModified());

        // Master toggle
        CheckBox master = page.getShowGutterIconsCheckBox();
        assertNotNull(master);
        assertTrue(master.isSelected());

        // Default enabled item
        CheckBox implemented = page.getItemCheckBox("java.implemented.method");
        assertNotNull(implemented);
        assertTrue(implemented.isSelected());

        // Default disabled item
        CheckBox inferred = page.getItemCheckBox("java.inferred.contract.annotations");
        assertNotNull(inferred);
        assertFalse(inferred.isSelected());

        // Another default enabled
        CheckBox runMarker = page.getItemCheckBox("common.run.line.marker");
        assertNotNull(runMarker);
        assertTrue(runMarker.isSelected());

        // Spring Web items
        CheckBox relatedViews = page.getItemCheckBox("springweb.related.views");
        assertNotNull(relatedViews);
        assertTrue(relatedViews.isSelected());

        CheckBox requestMappings = page.getItemCheckBox("springweb.request.mappings");
        assertNotNull(requestMappings);
        assertTrue(requestMappings.isSelected());
    }

    @Test
    void testModificationDetectionAndReset() throws Exception {
        if (!javaFxAvailable) return;

        AtomicBoolean modifiedFired = new AtomicBoolean(false);
        AtomicReference<SettingsGutterIconsPage> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                SettingsGutterIconsPage page = new SettingsGutterIconsPage();
                ref.set(page);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                CheckBox runMarker = page.getItemCheckBox("common.run.line.marker");
                assertNotNull(runMarker);
                runMarker.setSelected(false);

                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.reset();
                assertFalse(page.isModified());
                assertTrue(runMarker.isSelected());
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testApplySavesSettings() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                SettingsGutterIconsPage page = new SettingsGutterIconsPage();
                CheckBox master = page.getShowGutterIconsCheckBox();
                CheckBox runMarker = page.getItemCheckBox("common.run.line.marker");
                assertNotNull(master);
                assertNotNull(runMarker);

                master.setSelected(false);
                runMarker.setSelected(false);
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());
                assertFalse(GutterIconsSettings.getInstance().isShowGutterIcons());
                assertFalse(GutterIconsSettings.getInstance().isIconConfiguredEnabled("common.run.line.marker"));

                // Restore
                master.setSelected(true);
                runMarker.setSelected(true);
                page.apply();
                assertTrue(GutterIconsSettings.getInstance().isShowGutterIcons());
                assertTrue(GutterIconsSettings.getInstance().isIconConfiguredEnabled("common.run.line.marker"));
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
