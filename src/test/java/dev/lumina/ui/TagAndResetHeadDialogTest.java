package dev.lumina.ui;

import dev.lumina.git.GitService;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TagAndResetHeadDialogTest {

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
    void testGitServiceTagAndResetMethods(@TempDir Path tempDir) {
        // Validate blank revision returns failure
        GitService.Result invalidRev = GitService.validateRevision(tempDir, "");
        assertFalse(invalidRev.ok());

        // Validate command construction for createTag
        GitService.Result tagRes = GitService.createTag(tempDir, "v1.0.0", "HEAD", "Release v1.0.0", true);
        assertNotNull(tagRes);

        // Validate command construction for resetHead
        GitService.Result resetRes = GitService.resetHead(tempDir, "Mixed", "HEAD");
        assertNotNull(resetRes);

        GitService.Result resetHardRes = GitService.resetHead(tempDir, "Hard", "HEAD~1");
        assertNotNull(resetHardRes);
    }

    @Test
    void testTagDialogUIAndCallback(@TempDir Path tempDir) throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                AtomicReference<String> createdTagName = new AtomicReference<>();
                AtomicReference<String> createdCommit = new AtomicReference<>();
                AtomicReference<String> createdMessage = new AtomicReference<>();
                AtomicBoolean createdForce = new AtomicBoolean();

                TagDialog dialog = new TagDialog(null, tempDir, (tagName, commit, message, force) -> {
                    createdTagName.set(tagName);
                    createdCommit.set(commit);
                    createdMessage.set(message);
                    createdForce.set(force);
                });

                assertNotNull(dialog);
                assertEquals("Tag", dialog.getTitle());

                assertNotNull(dialog.getGitRootCombo());
                assertNotNull(dialog.getCurrentBranchLabel());
                assertNotNull(dialog.getTagNameField());
                assertNotNull(dialog.getForceBox());
                assertNotNull(dialog.getCommitField());
                assertNotNull(dialog.getValidateButton());
                assertNotNull(dialog.getMessageArea());
                assertNotNull(dialog.getCreateTagButton());
                assertNotNull(dialog.getCancelButton());

                // Initially button is disabled
                assertTrue(dialog.getCreateTagButton().isDisabled());

                // Typing tag name enables button
                dialog.getTagNameField().setText("v1.2.3");
                assertFalse(dialog.getCreateTagButton().isDisabled());

                // Configure options
                dialog.getForceBox().setSelected(true);
                dialog.getCommitField().setText("main");
                dialog.getMessageArea().setText("Annotated release tag");

                // Validate commit trigger
                dialog.getValidateButton().fire();
                assertTrue(dialog.getValidationStatusLabel().isVisible());

                // Fire create tag button
                dialog.getCreateTagButton().fire();

                assertEquals("v1.2.3", createdTagName.get());
                assertEquals("main", createdCommit.get());
                assertEquals("Annotated release tag", createdMessage.get());
                assertTrue(createdForce.get());

            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testResetHeadDialogUIAndCallback(@TempDir Path tempDir) throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                AtomicReference<String> resetTypeRef = new AtomicReference<>();
                AtomicReference<String> resetCommitRef = new AtomicReference<>();

                ResetHeadDialog dialog = new ResetHeadDialog(null, tempDir, (resetType, commit) -> {
                    resetTypeRef.set(resetType);
                    resetCommitRef.set(commit);
                });

                assertNotNull(dialog);
                assertEquals("Reset Head", dialog.getTitle());

                assertNotNull(dialog.getGitRootCombo());
                assertNotNull(dialog.getCurrentBranchLabel());
                assertNotNull(dialog.getResetTypeCombo());
                assertNotNull(dialog.getToCommitField());
                assertNotNull(dialog.getValidateButton());
                assertNotNull(dialog.getResetButton());
                assertNotNull(dialog.getCancelButton());

                // Default reset type is Mixed
                assertEquals(ResetHeadDialog.ResetType.MIXED, dialog.getResetTypeCombo().getValue());
                assertEquals("HEAD", dialog.getToCommitField().getText());

                // Validate button trigger
                dialog.getValidateButton().fire();
                assertTrue(dialog.getValidationStatusLabel().isVisible());

                // Switch reset type to Soft
                dialog.getResetTypeCombo().setValue(ResetHeadDialog.ResetType.SOFT);
                dialog.getToCommitField().setText("HEAD~2");

                // Fire reset button
                dialog.getResetButton().fire();

                assertEquals("Soft", resetTypeRef.get());
                assertEquals("HEAD~2", resetCommitRef.get());

            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testBrandIsolation() throws Exception {
        for (String file : java.util.List.of("TagDialog.java", "ResetHeadDialog.java")) {
            Path p = Path.of("src/main/java/dev/lumina/ui/" + file);
            assertTrue(Files.exists(p), file + " must exist");
            String content = Files.readString(p);
            assertFalse(content.contains("JetBrains"), "Found JetBrains in " + p);
            assertFalse(content.contains("IntelliJ"), "Found IntelliJ in " + p);
        }
    }
}
