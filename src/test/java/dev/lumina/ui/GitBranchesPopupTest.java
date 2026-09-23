package dev.lumina.ui;

import dev.lumina.git.GitService;
import javafx.application.Platform;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
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
        AtomicReference<String> mergedBranch = new AtomicReference<>();
        AtomicReference<String> rebasedTarget = new AtomicReference<>();

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
                    @Override public void onMergeBranch(String branch) { mergedBranch.set(branch); }
                    @Override public void onRebaseBranch(String target) { rebasedTarget.set(target); }
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
        assertNotNull(popup.getSubMenu());
    }

    @Test
    void testBrandIsolation() throws Exception {
        Path p = Path.of("src/main/java/dev/lumina/ui/GitBranchesPopup.java");
        if (Files.exists(p)) {
            String content = Files.readString(p);
            assertFalse(content.contains("JetBrains"), "Found JetBrains in " + p);
            assertFalse(content.contains("IntelliJ"), "Found IntelliJ in " + p);
        }
    }
}
