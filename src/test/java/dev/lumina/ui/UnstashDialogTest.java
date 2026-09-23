package dev.lumina.ui;

import dev.lumina.git.GitService;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class UnstashDialogTest {

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
    void testUnstashDialogComponents() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<UnstashDialog> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                UnstashDialog dialog = new UnstashDialog(null, Path.of("."), msg -> {}, () -> {});
                ref.set(dialog);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS));

        UnstashDialog dialog = ref.get();
        assertNotNull(dialog);
        assertEquals("Unstash Changes", dialog.getTitle());

        assertNotNull(dialog.getStashListView());
        assertNotNull(dialog.getPopStashCheck());
        assertFalse(dialog.getPopStashCheck().isSelected());

        assertNotNull(dialog.getReinstateIndexCheck());
        assertFalse(dialog.getReinstateIndexCheck().isSelected());

        assertNotNull(dialog.getApplyBtn());
        assertEquals("Apply Stash", dialog.getApplyBtn().getText());

        assertNotNull(dialog.getDropBtn());
        assertEquals("Drop Stash", dialog.getDropBtn().getText());

        assertNotNull(dialog.getCancelBtn());
        assertEquals("Cancel", dialog.getCancelBtn().getText());
    }

    @Test
    void testGitServiceStashQueries() {
        Path repo = Path.of(".");
        if (!GitService.isRepository(repo)) return;

        List<GitService.StashEntry> stashes = GitService.stashListDetailed(repo);
        assertNotNull(stashes);

        if (!stashes.isEmpty()) {
            GitService.StashEntry first = stashes.get(0);
            assertNotNull(first.ref());
            assertNotNull(first.message());
            assertNotNull(first.branch());

            var files = GitService.stashFiles(repo, first.ref());
            assertNotNull(files);
        }
    }
}
