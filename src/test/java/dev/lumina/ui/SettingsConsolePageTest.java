package dev.lumina.ui;

import dev.lumina.settings.ConsoleSettings;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SettingsConsolePageTest {

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
        ConsoleSettings.getInstance().initDefaults();
    }

    @Test
    void testPageInstantiationAndDefaults() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<SettingsConsolePage> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                ref.set(new SettingsConsolePage());
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        SettingsConsolePage page = ref.get();
        assertNotNull(page);
        assertFalse(page.isModified());

        CheckBox softWraps = findCheckBox(page, "Use soft wraps in console");
        assertNotNull(softWraps);
        assertFalse(softWraps.isSelected());

        CheckBox overrideBuffer = findCheckBox(page, "Override console cycle buffer size (1024 KB)");
        assertNotNull(overrideBuffer);
        assertFalse(overrideBuffer.isSelected());

        CheckBox foldStackTrace = findCheckBox(page, "Fold stack trace longer than");
        assertNotNull(foldStackTrace);
        assertTrue(foldStackTrace.isSelected());
    }

    @Test
    void testModificationDetectionAndReset() throws Exception {
        if (!javaFxAvailable) return;

        AtomicBoolean modifiedFired = new AtomicBoolean(false);
        AtomicReference<SettingsConsolePage> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                SettingsConsolePage page = new SettingsConsolePage();
                ref.set(page);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                CheckBox softWraps = findCheckBox(page, "Use soft wraps in console");
                assertNotNull(softWraps);
                softWraps.setSelected(true);

                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.reset();
                assertFalse(page.isModified());
                assertFalse(softWraps.isSelected());
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
                SettingsConsolePage page = new SettingsConsolePage();
                CheckBox softWraps = findCheckBox(page, "Use soft wraps in console");
                assertNotNull(softWraps);

                softWraps.setSelected(true);
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());
                assertTrue(ConsoleSettings.getInstance().isUseSoftWraps());

                // Restore
                softWraps.setSelected(false);
                page.apply();
                assertFalse(ConsoleSettings.getInstance().isUseSoftWraps());
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    private static CheckBox findCheckBox(Node root, String text) {
        if (root instanceof CheckBox cb) {
            if (text.equals(cb.getText())) {
                return cb;
            }
        }
        if (root instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                CheckBox found = findCheckBox(child, text);
                if (found != null) return found;
            }
        }
        return null;
    }
}
