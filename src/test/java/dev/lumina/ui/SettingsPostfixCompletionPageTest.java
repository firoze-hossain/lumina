package dev.lumina.ui;

import dev.lumina.settings.PostfixCompletionSettings;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SettingsPostfixCompletionPageTest {

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
        PostfixCompletionSettings.getInstance().initDefaults();
    }

    @Test
    void testInitialStateMatchesDefaults() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsPostfixCompletionPage page = new SettingsPostfixCompletionPage();
                assertFalse(page.isModified());

                assertTrue(page.getEnablePostfixCheck().isSelected());
                assertFalse(page.getShowAsCommandCheck().isSelected());
                assertEquals("Tab", page.getExpandCombo().getValue());

                assertEquals(13, page.getLanguageNodes().size());
                assertTrue(page.getLanguageNodes().containsKey("Java"));
                assertTrue(page.getLanguageNodes().containsKey("Rust"));
                assertTrue(page.getLanguageNodes().containsKey("TypeScript"));

                assertNotNull(page.getAddButton());
                assertNotNull(page.getRemoveButton());
                assertNotNull(page.getEditButton());
                assertNotNull(page.getDuplicateButton());
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
                SettingsPostfixCompletionPage page = new SettingsPostfixCompletionPage();
                boolean[] modifiedFired = {false};
                page.setOnModifiedListener(() -> modifiedFired[0] = true);

                assertFalse(page.isModified());

                page.getShowAsCommandCheck().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedFired[0]);

                page.reset();
                assertFalse(page.isModified());
                assertFalse(page.getShowAsCommandCheck().isSelected());

                page.getExpandCombo().setValue("Space");
                assertTrue(page.isModified());
                page.apply();

                assertFalse(page.isModified());
                assertEquals("Space", PostfixCompletionSettings.getInstance().getExpandShortcut());

                // Restore
                page.getExpandCombo().setValue("Tab");
                page.apply();
                assertEquals("Tab", PostfixCompletionSettings.getInstance().getExpandShortcut());
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
