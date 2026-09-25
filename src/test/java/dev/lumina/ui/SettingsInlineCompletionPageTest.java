package dev.lumina.ui;

import dev.lumina.settings.InlineCompletionSettings;
import dev.lumina.settings.InlineCompletionSettings.DownloadModelsMode;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SettingsInlineCompletionPageTest {

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
        InlineCompletionSettings.getInstance().initDefaults();
    }

    @Test
    void testInitialStateMatchesDefaults() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsInlineCompletionPage page = new SettingsInlineCompletionPage();
                assertFalse(page.isModified());

                assertTrue(page.getEnableLocalFullLineCheck().isSelected());
                assertEquals(DownloadModelsMode.ASK_BEFORE_DOWNLOADING, page.getDownloadModelsCombo().getValue());

                assertTrue(page.getLanguageCheck("Kotlin").isSelected());
                assertTrue(page.getLanguageCheck("Java").isSelected());
                assertFalse(page.getLanguageCheck("Rust").isSelected());

                assertNotNull(page.getDownloadLink("Rust"));
                assertEquals("Download (100 MB)", page.getDownloadLink("Rust").getText());

                assertFalse(page.getEnableCloudCompletionCheck().isSelected());
                assertTrue(page.getEnableCloudCompletionCheck().isDisable());

                assertTrue(page.getEnableAutomaticOnTypingCheck().isSelected());
                assertTrue(page.getEnableMultilineCheck().isSelected());
                assertFalse(page.getSyncInlineAndPopupCheck().isSelected());
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testDirtyTrackingAndApplyReset() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsInlineCompletionPage page = new SettingsInlineCompletionPage();
                boolean[] modifiedFired = {false};
                page.setOnModifiedListener(() -> modifiedFired[0] = true);

                assertFalse(page.isModified());

                // Modify multiline
                page.getEnableMultilineCheck().setSelected(false);
                assertTrue(page.isModified());
                assertTrue(modifiedFired[0]);

                // Reset
                page.reset();
                assertFalse(page.isModified());
                assertTrue(page.getEnableMultilineCheck().isSelected());

                // Modify and apply
                page.getEnableLocalFullLineCheck().setSelected(false);
                assertTrue(page.isModified());
                page.apply();

                assertFalse(page.isModified());
                assertFalse(InlineCompletionSettings.getInstance().isEnableLocalFullLine());

                // Restore
                page.getEnableLocalFullLineCheck().setSelected(true);
                page.apply();
                assertTrue(InlineCompletionSettings.getInstance().isEnableLocalFullLine());
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testDownloadLinkSimulation() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsInlineCompletionPage page = new SettingsInlineCompletionPage();
                var rustLink = page.getDownloadLink("Rust");
                assertNotNull(rustLink);
                assertFalse(page.getLanguageCheck("Rust").isSelected());

                // Simulate clicking download
                rustLink.fire();

                assertEquals("Installed", rustLink.getText());
                assertTrue(rustLink.isDisable());
                assertTrue(page.getLanguageCheck("Rust").isSelected());
                assertTrue(page.isModified());

                page.apply();
                assertTrue(InlineCompletionSettings.getInstance().isLanguageDownloaded("Rust"));
                assertTrue(InlineCompletionSettings.getInstance().isLanguageEnabled("Rust"));

                // Reset defaults
                InlineCompletionSettings.getInstance().initDefaults();
                InlineCompletionSettings.getInstance().save();
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
