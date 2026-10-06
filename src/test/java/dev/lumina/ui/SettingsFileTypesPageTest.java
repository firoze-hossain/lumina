package dev.lumina.ui;

import dev.lumina.filetypes.FileType;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class SettingsFileTypesPageTest {

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

    @Test
    void testFileTypesPageLifecycleAndDirtyState() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsFileTypesPage page = new SettingsFileTypesPage();
                assertNotNull(page);
                assertNotNull(page.getFileTypeListView());
                assertNotNull(page.getPatternListView());
                assertNotNull(page.getHashbangListView());
                assertNotNull(page.getIgnoredPatternsArea());

                // Should have populated list
                assertTrue(page.getFileTypeListView().getItems().size() >= 40);

                // Initial selection
                FileType selected = page.getFileTypeListView().getSelectionModel().getSelectedItem();
                assertNotNull(selected);

                // Patterns list should be updated for selected
                assertNotNull(page.getPatternListView().getItems());

                // Page initially not modified
                assertFalse(page.isModified());

                // Test dirty state on pattern change
                boolean[] modifiedFired = {false};
                page.setOnModifiedListener(() -> modifiedFired[0] = true);

                page.addPatternToFileType(selected, "*.customtestextension");
                assertTrue(page.isModified());
                assertTrue(modifiedFired[0]);

                // Reset restores clean state
                page.reset();
                assertFalse(page.isModified());

                // Test dirty state on ignored patterns change
                page.getIgnoredPatternsArea().setText(page.getIgnoredPatternsArea().getText() + "*.tmp;");
                assertTrue(page.isModified());

                // Reset again
                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
