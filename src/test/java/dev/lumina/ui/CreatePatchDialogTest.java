package dev.lumina.ui;

import dev.lumina.git.GitService;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CreatePatchDialogTest {

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
    void testCreatePatchDialogComponents() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<CreatePatchDialog> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                CreatePatchDialog dialog = new CreatePatchDialog(null, Path.of("."), msg -> {}, () -> {});
                ref.set(dialog);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS));

        CreatePatchDialog dialog = ref.get();
        assertNotNull(dialog);
        assertEquals("Create Patch", dialog.getTitle());

        assertNotNull(dialog.getPathField());
        assertTrue(dialog.getPathField().getText().endsWith(".patch"));

        assertNotNull(dialog.getReversePatchCheck());
        assertFalse(dialog.getReversePatchCheck().isSelected());

        assertNotNull(dialog.getCreatePatchBtn());
        assertEquals("Create Patch", dialog.getCreatePatchBtn().getText());

        assertNotNull(dialog.getCancelBtn());
        assertEquals("Cancel", dialog.getCancelBtn().getText());
    }

    @Test
    void testGitServicePatchOperations() throws Exception {
        Path repo = Path.of(".");
        if (!GitService.isRepository(repo)) return;

        Path tmpPatch = Files.createTempFile("lumina-test-", ".patch");
        try {
            GitService.Result r = GitService.createPatch(repo, tmpPatch, false);
            assertNotNull(r);
            assertTrue(Files.exists(tmpPatch));

            // Empty or invalid patch string test
            GitService.Result applyEmpty = GitService.applyPatchFromText(repo, "");
            assertFalse(applyEmpty.ok());
        } finally {
            Files.deleteIfExists(tmpPatch);
        }
    }
}
