package dev.lumina.ui;

import dev.lumina.util.Settings;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class UpdateProjectDialogTest {

    private static volatile boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            String display = System.getenv("DISPLAY");
            if (display == null || display.isBlank() || java.awt.GraphicsEnvironment.isHeadless()) {
                return;
            }
            Platform.startup(() -> javaFxAvailable = true);
            javaFxAvailable = true;
        } catch (Throwable ignored) {}
    }

    @Test
    void testDialogCreationAndInitialValues() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<UpdateProjectDialog> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                UpdateProjectDialog dialog = new UpdateProjectDialog(null, Path.of("."), msg -> {}, () -> {});
                ref.set(dialog);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS));

        UpdateProjectDialog dialog = ref.get();
        assertNotNull(dialog);
        assertEquals("Update Project", dialog.getTitle());
        assertNotNull(dialog.getMergeRadio());
        assertNotNull(dialog.getRebaseRadio());
        assertNotNull(dialog.getDontShowAgainCheck());
        assertNotNull(dialog.getOkBtn());
        assertNotNull(dialog.getCancelBtn());

        // Default should be merge
        assertTrue(dialog.getMergeRadio().isSelected());
        assertFalse(dialog.getRebaseRadio().isSelected());
        assertFalse(dialog.getDontShowAgainCheck().isSelected());
    }

    @Test
    void testRadioToggleBehavior() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<UpdateProjectDialog> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                UpdateProjectDialog dialog = new UpdateProjectDialog(null, Path.of("."), msg -> {}, () -> {});
                dialog.getRebaseRadio().setSelected(true);
                ref.set(dialog);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS));

        UpdateProjectDialog dialog = ref.get();
        assertNotNull(dialog);
        assertTrue(dialog.getRebaseRadio().isSelected());
        assertFalse(dialog.getMergeRadio().isSelected());
    }
}
