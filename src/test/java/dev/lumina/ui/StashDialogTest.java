package dev.lumina.ui;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class StashDialogTest {

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
    void testStashDialogComponents() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<StashDialog> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                StashDialog dialog = new StashDialog(null, Path.of("."), msg -> {}, () -> {});
                ref.set(dialog);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS));

        StashDialog dialog = ref.get();
        assertNotNull(dialog);
        assertEquals("Stash", dialog.getTitle());

        // Git Root
        assertNotNull(dialog.getGitRootCombo());
        assertFalse(dialog.getGitRootCombo().getItems().isEmpty());

        // Current Branch
        assertNotNull(dialog.getCurrentBranchLabel());
        assertTrue(dialog.getCurrentBranchLabel().getText().startsWith("Current Branch:"));

        // Message
        assertNotNull(dialog.getMessageArea());
        assertEquals("", dialog.getMessageArea().getText());

        // Keep Index
        assertNotNull(dialog.getKeepIndexCheck());
        assertFalse(dialog.getKeepIndexCheck().isSelected());
        assertEquals("Keep index", dialog.getKeepIndexCheck().getText());

        // Buttons
        assertNotNull(dialog.getCreateStashBtn());
        assertEquals("Create Stash", dialog.getCreateStashBtn().getText());
        assertTrue(dialog.getCreateStashBtn().isDefaultButton());

        assertNotNull(dialog.getCancelBtn());
        assertEquals("Cancel", dialog.getCancelBtn().getText());
        assertTrue(dialog.getCancelBtn().isCancelButton());
    }

    @Test
    void testKeepIndexToggle() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<StashDialog> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                StashDialog dialog = new StashDialog(null, Path.of("."), msg -> {}, () -> {});
                dialog.getKeepIndexCheck().setSelected(true);
                dialog.getMessageArea().setText("WIP: stash test message");
                ref.set(dialog);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS));

        StashDialog dialog = ref.get();
        assertNotNull(dialog);
        assertTrue(dialog.getKeepIndexCheck().isSelected());
        assertEquals("WIP: stash test message", dialog.getMessageArea().getText());
    }
}
