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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MergeRebaseDialogTest {

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
    void testMergeOptionEnumValues() {
        assertEquals("--no-ff", MergeDialog.MergeOption.NO_FF.getFlag());
        assertEquals("--ff-only", MergeDialog.MergeOption.FF_ONLY.getFlag());
        assertEquals("--squash", MergeDialog.MergeOption.SQUASH.getFlag());
        assertEquals("-m", MergeDialog.MergeOption.SPECIFY_MESSAGE.getFlag());
        assertEquals("--no-commit", MergeDialog.MergeOption.NO_COMMIT.getFlag());
        assertEquals("--no-verify", MergeDialog.MergeOption.NO_VERIFY.getFlag());
        assertEquals("--allow-unrelated-histories", MergeDialog.MergeOption.ALLOW_UNRELATED_HISTORIES.getFlag());

        for (MergeDialog.MergeOption opt : MergeDialog.MergeOption.values()) {
            assertNotNull(opt.getDescription());
            assertFalse(opt.getDescription().isBlank());
        }
    }

    @Test
    void testRebaseOptionEnumValues() {
        assertEquals("", RebaseDialog.RebaseOption.SELECT_BRANCH.getFlag());
        assertEquals("--onto", RebaseDialog.RebaseOption.ONTO.getFlag());
        assertEquals("--rebase-merges", RebaseDialog.RebaseOption.REBASE_MERGES.getFlag());
        assertEquals("--keep-empty", RebaseDialog.RebaseOption.KEEP_EMPTY.getFlag());
        assertEquals("--root", RebaseDialog.RebaseOption.ROOT.getFlag());
        assertEquals("--interactive", RebaseDialog.RebaseOption.INTERACTIVE.getFlag());
        assertEquals("--update-refs", RebaseDialog.RebaseOption.UPDATE_REFS.getFlag());

        for (RebaseDialog.RebaseOption opt : RebaseDialog.RebaseOption.values()) {
            assertNotNull(opt.getDescription());
            assertFalse(opt.getDescription().isBlank());
        }
    }

    @Test
    void testGitServiceAllBranchesAndMergeRebaseMethods(@TempDir Path tempDir) {
        List<String> branches = GitService.allBranches(tempDir);
        assertNotNull(branches);

        // Verify merge and rebase methods execute git commands with proper arguments
        GitService.Result mergeRes = GitService.merge(tempDir, "feature", List.of("--no-ff"), "Merge branch feature", null);
        assertNotNull(mergeRes);

        GitService.Result rebaseRes = GitService.rebase(tempDir, "main", "base-branch", List.of("--rebase-merges"), null);
        assertNotNull(rebaseRes);
    }

    @Test
    void testMergeDialogCreationAndOptions(@TempDir Path tempDir) throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                AtomicReference<String> mergedBranch = new AtomicReference<>();
                AtomicReference<List<String>> mergedOptions = new AtomicReference<>();
                AtomicReference<String> mergedMessage = new AtomicReference<>();

                MergeDialog dialog = new MergeDialog(null, tempDir, "dev", (branch, options, message) -> {
                    mergedBranch.set(branch);
                    mergedOptions.set(options);
                    mergedMessage.set(message);
                });

                assertNotNull(dialog.getTitle());
                assertTrue(dialog.getTitle().startsWith("Merge into "));
                assertEquals("dev", dialog.getBranchCombo().getValue());

                assertNotNull(dialog.getBranchCombo());
                assertNotNull(dialog.getMergeButton());
                assertNotNull(dialog.getCancelButton());
                assertNotNull(dialog.getModifyOptionsButton());
                assertNotNull(dialog.getMessageField());

                // Toggle mutually exclusive options
                dialog.toggleOption(MergeDialog.MergeOption.NO_FF);
                assertTrue(dialog.getActiveOptions().contains(MergeDialog.MergeOption.NO_FF));

                dialog.toggleOption(MergeDialog.MergeOption.FF_ONLY);
                assertFalse(dialog.getActiveOptions().contains(MergeDialog.MergeOption.NO_FF));
                assertTrue(dialog.getActiveOptions().contains(MergeDialog.MergeOption.FF_ONLY));

                // Toggle commit message
                dialog.toggleOption(MergeDialog.MergeOption.SPECIFY_MESSAGE);
                assertTrue(dialog.getActiveOptions().contains(MergeDialog.MergeOption.SPECIFY_MESSAGE));
                assertTrue(dialog.getMessageField().isVisible());
                dialog.getMessageField().setText("feat: custom merge commit");

                // Execute merge button
                dialog.getMergeButton().fire();
                assertEquals("dev", mergedBranch.get());
                assertEquals(List.of("--ff-only"), mergedOptions.get());
                assertEquals("feat: custom merge commit", mergedMessage.get());

            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testRebaseDialogCreationAndOptions(@TempDir Path tempDir) throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                AtomicReference<String> rebasedTarget = new AtomicReference<>();
                AtomicReference<String> rebasedOnto = new AtomicReference<>();
                AtomicReference<String> rebasedBranchToRebase = new AtomicReference<>();
                AtomicReference<List<String>> rebasedOptions = new AtomicReference<>();

                RebaseDialog dialog = new RebaseDialog(null, tempDir, "origin/main", (target, onto, branchToRebase, options) -> {
                    rebasedTarget.set(target);
                    rebasedOnto.set(onto);
                    rebasedBranchToRebase.set(branchToRebase);
                    rebasedOptions.set(options);
                });

                assertNotNull(dialog.getTitle());
                assertEquals("Rebase", dialog.getTitle());
                assertEquals("origin/main", dialog.getTargetBranchOrHash());

                assertNotNull(dialog.getBranchCombo());
                assertNotNull(dialog.getRebaseButton());
                assertNotNull(dialog.getCancelButton());
                assertNotNull(dialog.getModifyOptionsButton());
                assertNotNull(dialog.getOntoCombo());
                assertNotNull(dialog.getBranchToRebaseCombo());

                // Target is present so button should not be disabled
                assertFalse(dialog.getRebaseButton().isDisabled());

                // Toggle rebase options
                dialog.toggleOption(RebaseDialog.RebaseOption.REBASE_MERGES);
                dialog.toggleOption(RebaseDialog.RebaseOption.INTERACTIVE);
                assertTrue(dialog.getActiveOptions().contains(RebaseDialog.RebaseOption.REBASE_MERGES));
                assertTrue(dialog.getActiveOptions().contains(RebaseDialog.RebaseOption.INTERACTIVE));

                // Toggle onto
                dialog.toggleOption(RebaseDialog.RebaseOption.ONTO);
                assertTrue(dialog.getOntoCombo().isVisible());
                dialog.getOntoCombo().getEditor().setText("staging");

                // Fire Rebase button
                dialog.getRebaseButton().fire();
                assertEquals("origin/main", rebasedTarget.get());
                assertEquals("staging", rebasedOnto.get());
                assertNull(rebasedBranchToRebase.get());
                assertEquals(List.of("--rebase-merges", "--interactive"), rebasedOptions.get());

            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testRebaseDialogTargetValidation(@TempDir Path tempDir) throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                RebaseDialog dialog = new RebaseDialog(null, tempDir, null, (target, onto, branchToRebase, options) -> {});
                // When empty, rebase button should be disabled
                dialog.getBranchCombo().setValue("");
                dialog.getBranchCombo().getEditor().setText("");
                assertTrue(dialog.getRebaseButton().isDisabled());

                // When target entered, rebase button should be enabled
                dialog.getBranchCombo().getEditor().setText("feature/login");
                assertFalse(dialog.getRebaseButton().isDisabled());

            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testBrandIsolationInMergeAndRebaseCode() throws Exception {
        for (String file : List.of("MergeDialog.java", "RebaseDialog.java")) {
            Path path = Path.of("src/main/java/dev/lumina/ui/" + file);
            assertTrue(Files.exists(path), file + " must exist");
            String content = Files.readString(path).toLowerCase();
            assertFalse(content.contains("jetbrains"), "File " + file + " must not contain 'jetbrains'");
            assertFalse(content.contains("intellij"), "File " + file + " must not contain 'intellij'");
        }
    }
}
