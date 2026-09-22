package dev.lumina.ui;

import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CommitPanelTest {

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
    void testCommitPanelInitialization() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<CommitPanel> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                CommitPanel panel = new CommitPanel(() -> Path.of("."), msg -> {});
                ref.set(panel);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS));

        CommitPanel panel = ref.get();
        assertNotNull(panel);
        assertNotNull(panel.getTreeView());
        assertNotNull(panel.getMessage());
        assertEquals("Commit Message", panel.getMessage().getPromptText());
        assertNotNull(panel.getCommitBtn());
        assertNotNull(panel.getCommitAndPushBtn());
        assertNotNull(panel.getAmendCheck());
    }

    @Test
    void testCategorySelectionAndTriState() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                CommitPanel.CategoryItem category = new CommitPanel.CategoryItem("Changes");
                CommitPanel.FileItem file1 = new CommitPanel.FileItem("src/A.java", CommitPanel.ChangeType.MODIFIED, false, false, true);
                CommitPanel.FileItem file2 = new CommitPanel.FileItem("src/B.java", CommitPanel.ChangeType.MODIFIED, false, false, true);
                category.getFiles().addAll(file1, file2);

                category.updateSelectionState();
                assertTrue(category.selectedProperty().get());
                assertFalse(category.indeterminateProperty().get());

                // Uncheck one -> should be indeterminate
                file1.setSelected(false);
                category.updateSelectionState();
                assertFalse(category.selectedProperty().get());
                assertTrue(category.indeterminateProperty().get());

                // Uncheck all -> should be unselected
                file2.setSelected(false);
                category.updateSelectionState();
                assertFalse(category.selectedProperty().get());
                assertFalse(category.indeterminateProperty().get());

                // Toggle all on
                category.toggleAll(true);
                assertTrue(file1.isSelected());
                assertTrue(file2.isSelected());
                assertTrue(category.selectedProperty().get());
                assertFalse(category.indeterminateProperty().get());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS));
    }

    @Test
    void testFileItemParsingPaths() {
        CommitPanel.FileItem item1 = new CommitPanel.FileItem("src/main/resources/application.yml", CommitPanel.ChangeType.MODIFIED, false, false, true);
        assertEquals("application.yml", item1.getFileName());
        assertEquals("src/main/resources", item1.getDirectoryPath());
        assertFalse(item1.isUnversioned());

        CommitPanel.FileItem item2 = new CommitPanel.FileItem(".gitignore", CommitPanel.ChangeType.UNTRACKED, true, false, false);
        assertEquals(".gitignore", item2.getFileName());
        assertEquals("", item2.getDirectoryPath());
        assertTrue(item2.isUnversioned());
    }

    @Test
    void testFileStatusColorsMatchingIntelliJ() {
        // Untracked / unversioned files must be light red / coral (#ED6C63) matching IntelliJ
        assertEquals("#ED6C63", CommitPanel.getStatusColor(CommitPanel.ChangeType.UNTRACKED));
        // Staged added files must be green (#629755)
        assertEquals("#629755", CommitPanel.getStatusColor(CommitPanel.ChangeType.ADDED));
        // Modified files must be cyan/blue (#56A8F5)
        assertEquals("#56A8F5", CommitPanel.getStatusColor(CommitPanel.ChangeType.MODIFIED));
    }

    @Test
    void testStashFileParsing() {
        dev.lumina.git.GitService.StashFile sf = dev.lumina.git.GitService.StashFile.fromLine("M\tsrc/main/java/dev/lumina/ui/FileExplorer.java");
        assertNotNull(sf);
        assertEquals("FileExplorer.java", sf.fileName());
        assertEquals("src/main/java/dev/lumina/ui", sf.dirPath());
        assertEquals("src/main/java/dev/lumina/ui/FileExplorer.java", sf.relativePath());
        assertEquals("M", sf.statusPrefix());
    }

    @Test
    void testStashEntry() {
        dev.lumina.git.GitService.StashEntry entry = new dev.lumina.git.GitService.StashEntry(
                0, "stash@{0}", "master", "added starter in pom and grade build", "2 hours ago"
        );
        assertEquals(0, entry.index());
        assertEquals("stash@{0}", entry.ref());
        assertEquals("master", entry.branch());
        assertEquals("added starter in pom and grade build", entry.message());
        assertEquals("2 hours ago", entry.date());
    }
}
