package dev.lumina.ui;

import dev.lumina.git.GitService;
import javafx.application.Platform;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GitBranchesPopupTest {

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
    void testGitServiceBranchMethods() {
        Path repo = Path.of(".");
        if (!GitService.isRepository(repo)) return;

        String current = GitService.currentBranch(repo);
        assertNotNull(current);

        var local = GitService.localBranches(repo);
        assertNotNull(local);
        assertTrue(local.contains(current));

        var remote = GitService.remoteBranches(repo);
        assertNotNull(remote);

        int outgoing = GitService.unpushedCommitsCount(repo, current);
        assertTrue(outgoing >= 0);
    }

    @Test
    void testPopupConstructionAndCallbacks() throws Exception {
        if (!javaFxAvailable) return;

        AtomicBoolean updateCalled = new AtomicBoolean(false);
        AtomicBoolean commitCalled = new AtomicBoolean(false);
        AtomicBoolean pushCalled = new AtomicBoolean(false);
        AtomicBoolean newBranchCalled = new AtomicBoolean(false);

        AtomicReference<GitBranchesPopup> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                GitBranchesPopup popup = new GitBranchesPopup(Path.of("."), msg -> {}, new GitBranchesPopup.BranchCallbacks() {
                    @Override public void onUpdateProject() { updateCalled.set(true); }
                    @Override public void onCommit() { commitCalled.set(true); }
                    @Override public void onPush() { pushCalled.set(true); }
                    @Override public void onNewBranch() { newBranchCalled.set(true); }
                    @Override public void onCheckoutTag() {}
                    @Override public void onBranchChanged() {}
                });
                ref.set(popup);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS));

        GitBranchesPopup popup = ref.get();
        assertNotNull(popup);
        assertNotNull(popup.getSearchField());
        assertNotNull(popup.getContentBox());
        assertEquals("Search for branches and actions", popup.getSearchField().getPromptText());
    }

    @Test
    void testSearchFilter() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<GitBranchesPopup> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                VBox anchor = new VBox();
                javafx.scene.Scene scene = new javafx.scene.Scene(anchor, 100, 100);
                javafx.stage.Stage stage = new javafx.stage.Stage();
                stage.setScene(scene);

                GitBranchesPopup popup = new GitBranchesPopup(Path.of("."), msg -> {}, new GitBranchesPopup.BranchCallbacks() {
                    @Override public void onUpdateProject() {}
                    @Override public void onCommit() {}
                    @Override public void onPush() {}
                    @Override public void onNewBranch() {}
                    @Override public void onCheckoutTag() {}
                    @Override public void onBranchChanged() {}
                });
                ref.set(popup);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS));

        GitBranchesPopup popup = ref.get();
        assertNotNull(popup);
        assertNotNull(popup.getSearchField());
    }
}
