package dev.lumina.ui;

import dev.lumina.git.GitService;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GitRemotesDialogTest {

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
    void testGitRemotesDialogComponents() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<GitRemotesDialog> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                GitRemotesDialog dialog = new GitRemotesDialog(null, Path.of("."), msg -> {});
                ref.set(dialog);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS));

        GitRemotesDialog dialog = ref.get();
        assertNotNull(dialog);
        assertEquals("Git Remotes", dialog.getTitle());

        // Verify toolbar buttons
        assertNotNull(dialog.getAddButton());
        assertNotNull(dialog.getRemoveButton());
        assertNotNull(dialog.getEditButton());

        // Verify TableView
        assertNotNull(dialog.getTableView());
        assertEquals(2, dialog.getTableView().getColumns().size());
        assertEquals("Name", dialog.getTableView().getColumns().get(0).getText());
        assertEquals("URL", dialog.getTableView().getColumns().get(1).getText());

        // Verify dynamic remotes loaded from active repo
        assertNotNull(dialog.getRemotesList());
        assertFalse(dialog.getRemotesList().isEmpty());
        assertTrue(dialog.getRemotesList().stream().anyMatch(r -> "origin".equalsIgnoreCase(r.name())));
    }
}
