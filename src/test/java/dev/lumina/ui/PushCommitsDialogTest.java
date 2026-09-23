package dev.lumina.ui;

import dev.lumina.git.GitService;
import dev.lumina.git.GitService.CommitFile;
import dev.lumina.git.GitService.OutgoingCommit;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PushCommitsDialogTest {

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
    void testCommitFileParsing() {
        CommitFile cf = CommitFile.fromLine("M\tsrc/main/resources/application.yml");
        assertNotNull(cf);
        assertEquals("application.yml", cf.fileName());
        assertEquals("src/main/resources", cf.dirPath());
        assertEquals("src/main/resources/application.yml", cf.relativePath());
        assertEquals("M", cf.statusPrefix());

        CommitFile cf2 = CommitFile.fromLine("A\tmvnw");
        assertNotNull(cf2);
        assertEquals("mvnw", cf2.fileName());
        assertEquals("", cf2.dirPath());
        assertEquals("mvnw", cf2.relativePath());
        assertEquals("A", cf2.statusPrefix());
    }

    @Test
    void testOutgoingCommitRecord() {
        CommitFile cf = new CommitFile("src/main/resources/application.yml", "application.yml", "src/main/resources", "M");
        OutgoingCommit commit = new OutgoingCommit("347f04bc4735", "347f04b", "config up yml", "developer", "2 hours ago", List.of(cf));

        assertEquals("347f04bc4735", commit.hash());
        assertEquals("347f04b", commit.shortHash());
        assertEquals("config up yml", commit.subject());
        assertEquals("developer", commit.author());
        assertEquals("2 hours ago", commit.date());
        assertEquals(1, commit.files().size());
        assertEquals("application.yml", commit.files().get(0).fileName());
    }

    @Test
    void testPushCommitsDialogComponentsClean() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<PushCommitsDialog> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                PushCommitsDialog dialog = new PushCommitsDialog(null, Path.of("."), msg -> {}, 0);
                ref.set(dialog);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS));

        PushCommitsDialog dialog = ref.get();
        assertNotNull(dialog);
        assertEquals("Push", dialog.getPushMainBtn().getText());
        assertNotNull(dialog.getPushChevronBtn());
        assertNotNull(dialog.getCancelBtn());
        assertNotNull(dialog.getPushTagsCheck());
        assertNotNull(dialog.getTagModeCombo());
        assertNotNull(dialog.getCommitsListView());
        assertNotNull(dialog.getFilesTreeView());
    }

    @Test
    void testPushCommitsDialogWithProblems() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<PushCommitsDialog> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                PushCommitsDialog dialog = new PushCommitsDialog(null, Path.of("."), msg -> {}, 1);
                ref.set(dialog);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS));

        PushCommitsDialog dialog = ref.get();
        assertNotNull(dialog);
        // Matching image 1 & 2: Push Anyway when problems exist
        assertEquals("Push Anyway", dialog.getPushMainBtn().getText());
    }
}
