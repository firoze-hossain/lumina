package dev.lumina.ui;

import dev.lumina.settings.StickyLinesSettings;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SettingsStickyLinesPageTest {

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
        StickyLinesSettings.getInstance().initDefaults();
    }

    @Test
    void testInitialStateMatchesScreenshot() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsStickyLinesPage page = new SettingsStickyLinesPage();
                assertFalse(page.isModified());

                assertTrue(page.getShowStickyLinesCheck().isSelected());
                assertEquals(5, page.getMaxSpinner().getValue());

                assertEquals(29, page.getLanguageChecks().size());
                for (String lang : StickyLinesSettings.ALL_LANGUAGES) {
                    assertTrue(page.getLanguageChecks().containsKey(lang), "Missing checkbox for " + lang);
                    assertTrue(page.getLanguageChecks().get(lang).isSelected(), "Expected checked for " + lang);
                }
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testControlsDisabledWhenMasterUnchecked() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsStickyLinesPage page = new SettingsStickyLinesPage();
                assertFalse(page.getMaxLabel().isDisable());
                assertFalse(page.getMaxSpinner().isDisable());
                assertFalse(page.getLanguagesLabel().isDisable());
                assertFalse(page.getManageColorsLink().isDisable());

                page.getShowStickyLinesCheck().setSelected(false);

                assertTrue(page.getMaxLabel().isDisable());
                assertTrue(page.getMaxSpinner().isDisable());
                assertTrue(page.getLanguagesLabel().isDisable());
                assertTrue(page.getManageColorsLink().isDisable());

                page.getShowStickyLinesCheck().setSelected(true);

                assertFalse(page.getMaxLabel().isDisable());
                assertFalse(page.getMaxSpinner().isDisable());
                assertFalse(page.getLanguagesLabel().isDisable());
                assertFalse(page.getManageColorsLink().isDisable());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testManageColorsCallback() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsStickyLinesPage page = new SettingsStickyLinesPage();
                AtomicBoolean called = new AtomicBoolean(false);
                page.setOnManageColors(() -> called.set(true));

                page.getManageColorsLink().fire();
                assertTrue(called.get());
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
                SettingsStickyLinesPage page = new SettingsStickyLinesPage();
                AtomicBoolean modifiedNotified = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedNotified.set(true));

                assertFalse(page.isModified());

                page.getLanguageChecks().get("Java").setSelected(false);
                assertTrue(page.isModified());
                assertTrue(modifiedNotified.get());

                page.reset();
                assertFalse(page.isModified());
                assertTrue(page.getLanguageChecks().get("Java").isSelected());
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
                SettingsStickyLinesPage page = new SettingsStickyLinesPage();
                page.getShowStickyLinesCheck().setSelected(false);
                page.getMaxSpinner().getValueFactory().setValue(12);
                page.getLanguageChecks().get("Rust").setSelected(false);

                assertTrue(page.isModified());
                page.apply();

                assertFalse(page.isModified());
                assertFalse(StickyLinesSettings.getInstance().isShowStickyLines());
                assertEquals(12, StickyLinesSettings.getInstance().getMaxLines());
                assertFalse(StickyLinesSettings.getInstance().isLanguageEnabled("Rust"));

                // Restore defaults
                StickyLinesSettings.getInstance().initDefaults();
                StickyLinesSettings.getInstance().save();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
