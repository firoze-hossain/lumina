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

class CreateBranchDialogTest {

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
    void testCreateBranchServiceLogic() {
        Path repo = Path.of(".");
        if (!GitService.isRepository(repo)) return;

        String testBranch = "test-branch-dry-run-" + System.currentTimeMillis();
        GitService.Result r = GitService.createBranch(repo, testBranch, "HEAD", false, false);
        if (r.ok()) {
            // Clean up
            GitService.deleteBranch(repo, testBranch, true);
        }
    }

    @Test
    void testDialogConstructionHeadlessSafe() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<CreateBranchDialog> dialogRef = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                CreateBranchDialog dlg = new CreateBranchDialog(null, Path.of("."), "origin/feature_test");
                assertNotNull(dlg);
                assertEquals("Create Branch from origin/feature_test", dlg.getTitle());
                dialogRef.set(dlg);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNotNull(dialogRef.get());
    }

    @Test
    void testBrandIsolation() throws Exception {
        Path p = Path.of("src/main/java/dev/lumina/ui/CreateBranchDialog.java");
        if (Files.exists(p)) {
            String content = Files.readString(p);
            assertFalse(content.contains("JetBrains"), "Found JetBrains in " + p);
            assertFalse(content.contains("IntelliJ"), "Found IntelliJ in " + p);
        }
    }
}
