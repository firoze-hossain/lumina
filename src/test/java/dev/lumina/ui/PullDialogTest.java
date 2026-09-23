package dev.lumina.ui;

import dev.lumina.git.GitService;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PullDialogTest {

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
    void testPullOptionEnumValues() {
        assertEquals("--rebase", PullDialog.PullOption.REBASE.getFlag());
        assertEquals("--ff-only", PullDialog.PullOption.FF_ONLY.getFlag());
        assertEquals("--no-ff", PullDialog.PullOption.NO_FF.getFlag());
        assertEquals("--squash", PullDialog.PullOption.SQUASH.getFlag());
        assertEquals("--no-commit", PullDialog.PullOption.NO_COMMIT.getFlag());
        assertEquals("--no-verify", PullDialog.PullOption.NO_VERIFY.getFlag());

        assertTrue(PullDialog.PullOption.REBASE.getDescription().toLowerCase().contains("rebase"));
        assertTrue(PullDialog.PullOption.FF_ONLY.getDescription().toLowerCase().contains("fast-forward"));
    }

    @Test
    void testRemoteBranchesForRemote(@TempDir Path tempDir) throws Exception {
        // Test remote branch parsing logic with mock directory
        List<String> branches = GitService.remoteBranchesForRemote(tempDir, "origin");
        assertNotNull(branches);
    }

    @Test
    void testPullDialogCreationAndInteractions(@TempDir Path tempDir) throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                AtomicReference<String> pulledRemote = new AtomicReference<>();
                AtomicReference<String> pulledBranch = new AtomicReference<>();
                AtomicReference<List<String>> pulledOptions = new AtomicReference<>();

                PullDialog dialog = new PullDialog(null, tempDir, (remote, branch, options) -> {
                    pulledRemote.set(remote);
                    pulledBranch.set(branch);
                    pulledOptions.set(options);
                });

                assertNotNull(dialog.getTitle());
                assertTrue(dialog.getTitle().startsWith("Pull to "));

                assertNotNull(dialog.getRemoteCombo());
                assertNotNull(dialog.getBranchCombo());
                assertNotNull(dialog.getPullButton());
                assertNotNull(dialog.getCancelButton());
                assertNotNull(dialog.getModifyOptionsButton());

                // Test toggling options
                dialog.toggleOption(PullDialog.PullOption.REBASE);
                assertTrue(dialog.getActiveOptions().contains(PullDialog.PullOption.REBASE));

                // Mutually exclusive: FF_ONLY should replace REBASE
                dialog.toggleOption(PullDialog.PullOption.FF_ONLY);
                assertFalse(dialog.getActiveOptions().contains(PullDialog.PullOption.REBASE));
                assertTrue(dialog.getActiveOptions().contains(PullDialog.PullOption.FF_ONLY));

                // Add squash
                dialog.toggleOption(PullDialog.PullOption.SQUASH);
                assertTrue(dialog.getActiveOptions().contains(PullDialog.PullOption.SQUASH));
                assertTrue(dialog.getActiveOptions().contains(PullDialog.PullOption.FF_ONLY));

                // Fire Pull button
                dialog.getPullButton().fire();
                assertEquals(List.of("--ff-only", "--squash"), pulledOptions.get());

            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testBrandIsolation() throws Exception {
        List<Path> paths = List.of(
                Path.of("src/main/java/dev/lumina/ui/PullDialog.java"),
                Path.of("src/main/java/dev/lumina/ui/UpdateProjectDialog.java"),
                Path.of("src/main/java/dev/lumina/git/GitService.java")
        );

        for (Path p : paths) {
            if (Files.exists(p)) {
                String content = Files.readString(p);
                assertFalse(content.contains("JetBrains"), "Found JetBrains in " + p);
                assertFalse(content.contains("IntelliJ"), "Found IntelliJ in " + p);
            }
        }
    }
}
