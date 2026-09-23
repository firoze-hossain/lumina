package dev.lumina.ui;

import dev.lumina.git.GitLogCommit;
import dev.lumina.git.GitService;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GitLogPanelTest {

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
    void testGitServiceLogQueriesOnCurrentRepo() {
        Path repo = Path.of(".");
        if (!GitService.isRepository(repo)) return;

        List<GitLogCommit> commits = GitService.getLogCommits(repo, 10, "All");
        assertNotNull(commits);
        assertFalse(commits.isEmpty());

        GitLogCommit top = commits.get(0);
        assertNotNull(top.hash());
        assertNotNull(top.shortHash());
        assertNotNull(top.subject());
        assertNotNull(top.authorName());
        assertFalse(top.getFormattedDate().isBlank());

        List<GitService.CommitFile> files = GitService.getCommitFiles(repo, top.hash());
        assertNotNull(files);

        List<String> branches = GitService.getBranchesContaining(repo, top.hash());
        assertNotNull(branches);
    }

    @Test
    void testPanelConstructionAndTabSwitching() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<GitLogPanel> panelRef = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                GitLogPanel panel = new GitLogPanel(() -> Path.of("."));
                assertNotNull(panel);

                panel.selectConsoleTab();
                panel.selectLogTab();

                panelRef.set(panel);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNotNull(panelRef.get());
    }

    @Test
    void testToolWindowHeaderButtonsAndOptions() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                GitLogPanel panel = new GitLogPanel(() -> Path.of("."));

                // Header buttons
                assertNotNull(panel.getOptionsButton(), "Options button should not be null");
                assertEquals("⋮", panel.getOptionsButton().getText());
                assertEquals("Options", panel.getOptionsButton().getTooltip().getText());

                assertNotNull(panel.getHideButton(), "Hide button should not be null");
                assertEquals("—", panel.getHideButton().getText());
                assertTrue(panel.getHideButton().getTooltip().getText().contains("Hide"));

                // Hide callback
                boolean[] hideCalled = {false};
                panel.setOnHideToolWindow(() -> hideCalled[0] = true);
                panel.getHideButton().fire();
                assertTrue(hideCalled[0], "Firing hide button should invoke onHideToolWindow");

                // Dynamic toolbar toggle
                assertTrue(panel.isShowCenterToolbar(), "Center toolbar should initially be shown");
                panel.setShowCenterToolbar(false);
                assertFalse(panel.isShowCenterToolbar());
                assertFalse(panel.getCenterToolbar().isVisible());
                assertFalse(panel.getCenterToolbar().isManaged());

                panel.setShowCenterToolbar(true);
                assertTrue(panel.isShowCenterToolbar());
                assertTrue(panel.getCenterToolbar().isVisible());
                assertTrue(panel.getCenterToolbar().isManaged());

                // Double-click mode toggle
                assertTrue(panel.isOpenDiffOnDoubleClick(), "Default double click should be diff");
                panel.setOpenDiffOnDoubleClick(false);
                assertFalse(panel.isOpenDiffOnDoubleClick());
                panel.setOpenDiffOnDoubleClick(true);
                assertTrue(panel.isOpenDiffOnDoubleClick());

                // Speed search unhides toolbar if hidden
                panel.setShowCenterToolbar(false);
                panel.focusSpeedSearch();
                assertTrue(panel.isShowCenterToolbar(), "Speed search should unhide toolbar");

                // Tab close
                panel.selectConsoleTab();
                panel.closeConsoleTab();
                panel.closeAllTabs();

            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testBrandIsolation() throws Exception {
        Path gitLogPanelPath = Path.of("src/main/java/dev/lumina/ui/GitLogPanel.java");
        Path gitLogCommitPath = Path.of("src/main/java/dev/lumina/git/GitLogCommit.java");
        Path gitLogGraphPath = Path.of("src/main/java/dev/lumina/git/GitLogGraph.java");

        for (Path p : List.of(gitLogPanelPath, gitLogCommitPath, gitLogGraphPath)) {
            if (Files.exists(p)) {
                String content = Files.readString(p);
                assertFalse(content.contains("JetBrains"), "Found JetBrains in " + p);
                assertFalse(content.contains("IntelliJ"), "Found IntelliJ in " + p);
            }
        }
    }
}
