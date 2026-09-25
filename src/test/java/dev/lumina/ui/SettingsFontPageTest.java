package dev.lumina.ui;

import dev.lumina.settings.EditorFontSettings;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SettingsFontPageTest {

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
        EditorFontSettings.getInstance().initDefaults();
    }

    @Test
    void testInitialStateMatchesDefaults() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsFontPage page = new SettingsFontPage();
                assertFalse(page.isModified());

                assertEquals("JetBrains Mono", page.getFontCombo().getValue());
                assertEquals("13.0", page.getSizeField().getText());
                assertEquals("1.2", page.getLineHeightField().getText());
                assertFalse(page.getEnableLigaturesCheck().isSelected());
                assertEquals("Regular", page.getMainWeightCombo().getValue());
                assertEquals("Bold Recommended", page.getBoldWeightCombo().getValue());
                assertEquals("<None>", page.getFallbackFontCombo().getValue());

                // Preview text should contain Lumina sample
                assertNotNull(page.getPreviewTextArea().getText());
                assertTrue(page.getPreviewTextArea().getText().contains("Lumina is an Integrated Development Environment"));
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
                SettingsFontPage page = new SettingsFontPage();
                AtomicBoolean modifiedNotified = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedNotified.set(true));

                assertFalse(page.isModified());

                // Modify size
                page.getSizeField().setText("15.0");
                assertTrue(page.isModified());
                assertTrue(modifiedNotified.get());

                // Modify line height
                page.getLineHeightField().setText("1.4");
                assertTrue(page.isModified());

                // Modify ligatures
                page.getEnableLigaturesCheck().setSelected(true);
                assertTrue(page.isModified());

                // Reset back to defaults
                page.reset();
                assertFalse(page.isModified());
                assertEquals("13.0", page.getSizeField().getText());
                assertEquals("1.2", page.getLineHeightField().getText());
                assertFalse(page.getEnableLigaturesCheck().isSelected());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testApplySavesToSettings() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsFontPage page = new SettingsFontPage();
                page.getSizeField().setText("16.0");
                page.getLineHeightField().setText("1.5");
                page.getEnableLigaturesCheck().setSelected(true);
                page.getMainWeightCombo().setValue("Medium");
                page.getBoldWeightCombo().setValue("ExtraBold");

                assertTrue(page.isModified());
                page.apply();

                assertFalse(page.isModified());
                EditorFontSettings s = EditorFontSettings.getInstance();
                assertEquals(16.0, s.getFontSize(), 0.001);
                assertEquals(1.5, s.getLineHeight(), 0.001);
                assertTrue(s.isEnableLigatures());
                assertEquals("Medium", s.getMainWeight());
                assertEquals("ExtraBold", s.getBoldWeight());

                // Restore defaults
                s.initDefaults();
                s.save();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
