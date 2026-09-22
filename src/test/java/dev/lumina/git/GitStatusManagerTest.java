package dev.lumina.git;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class GitStatusManagerTest {

    private GitStatusManager manager;

    @BeforeEach
    void setUp() {
        manager = GitStatusManager.getInstance();
        manager.clearCache();
    }

    @Test
    void testGitFileStatusColors() {
        // IntelliJ IDEA color matching:
        // Added: green / sky-green
        assertEquals("#59A869", GitFileStatus.ADDED.getColorHex());
        // Untracked / unversioned: light red
        assertEquals("#ED6C63", GitFileStatus.UNTRACKED.getColorHex());
        // Modified: sky-blue
        assertEquals("#56A8F5", GitFileStatus.MODIFIED.getColorHex());
        // Normal: white / light gray
        assertEquals("#DFE1E5", GitFileStatus.NORMAL.getColorHex());

        assertTrue(GitFileStatus.ADDED.isModifiedOrNew());
        assertTrue(GitFileStatus.UNTRACKED.isModifiedOrNew());
        assertTrue(GitFileStatus.MODIFIED.isModifiedOrNew());
        assertFalse(GitFileStatus.NORMAL.isModifiedOrNew());
    }

    @Test
    void testSetAndGetStatus() {
        Path fakeFile = Path.of("/workspace/project/src/Hello.java");
        assertEquals(GitFileStatus.NORMAL, manager.getStatus(fakeFile));

        // When cancelled, file becomes UNTRACKED (light red)
        manager.setStatus(fakeFile, GitFileStatus.UNTRACKED);
        assertEquals(GitFileStatus.UNTRACKED, manager.getStatus(fakeFile));

        // When added, file becomes ADDED (sky-green)
        manager.setStatus(fakeFile, GitFileStatus.ADDED);
        assertEquals(GitFileStatus.ADDED, manager.getStatus(fakeFile));

        // Remove status
        manager.removeStatus(fakeFile);
        assertEquals(GitFileStatus.NORMAL, manager.getStatus(fakeFile));
    }

    @Test
    void testRepositoryRootDetection(@TempDir Path tempDir) throws IOException {
        Path subDir = tempDir.resolve("src/main/java/com/app");
        Files.createDirectories(subDir);
        Path testFile = subDir.resolve("Main.java");
        Files.createFile(testFile);

        // Not a repo initially
        assertNull(GitStatusManager.findRepositoryRoot(testFile));
        assertFalse(GitStatusManager.isInsideGitRepository(testFile));

        // Create fake .git folder at tempDir
        Files.createDirectory(tempDir.resolve(".git"));

        Path detectedRoot = GitStatusManager.findRepositoryRoot(testFile);
        assertNotNull(detectedRoot);
        assertEquals(tempDir.toAbsolutePath().normalize(), detectedRoot);
        assertTrue(GitStatusManager.isInsideGitRepository(testFile));
    }

    @Test
    void testListenerNotification() {
        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addListener(listener);

        notified.set(false);
        manager.setStatus(Path.of("/workspace/Sample.java"), GitFileStatus.ADDED);
        assertTrue(notified.get());

        manager.removeListener(listener);
    }

    @Test
    void testDirectoryStatusAggregation(@TempDir Path tempDir) throws IOException {
        Path entityDir = tempDir.resolve("order/entity");
        Files.createDirectories(entityDir);
        Path orderFile = entityDir.resolve("Order.java");
        Files.createFile(orderFile);

        // Before modification: NORMAL
        assertEquals(GitFileStatus.NORMAL, manager.getStatus(entityDir));
        assertEquals(GitFileStatus.NORMAL, manager.getStatus(orderFile));

        // When Order.java is modified: both Order.java and entity package become MODIFIED
        manager.setStatus(orderFile, GitFileStatus.MODIFIED);
        assertEquals(GitFileStatus.MODIFIED, manager.getStatus(orderFile));
        assertEquals(GitFileStatus.MODIFIED, manager.getStatus(entityDir));
        assertEquals(GitFileStatus.MODIFIED, manager.getDirectoryStatus(entityDir));

        // Parent package 'order' also reflects MODIFIED
        Path orderDir = tempDir.resolve("order");
        assertEquals(GitFileStatus.MODIFIED, manager.getStatus(orderDir));

        // When changes are committed / reverted back to NORMAL
        manager.setStatus(orderFile, GitFileStatus.NORMAL);
        assertEquals(GitFileStatus.NORMAL, manager.getStatus(orderFile));
        assertEquals(GitFileStatus.NORMAL, manager.getStatus(entityDir));
    }

    @Test
    void testDirectoryStatusPriority(@TempDir Path tempDir) throws IOException {
        Path pkgDir = tempDir.resolve("service");
        Files.createDirectories(pkgDir);
        Path file1 = pkgDir.resolve("ServiceA.java");
        Path file2 = pkgDir.resolve("ServiceB.java");
        Files.createFile(file1);
        Files.createFile(file2);

        // One untracked, one normal -> UNTRACKED
        manager.setStatus(file1, GitFileStatus.UNTRACKED);
        assertEquals(GitFileStatus.UNTRACKED, manager.getStatus(pkgDir));

        // One added, one untracked -> ADDED takes priority
        manager.setStatus(file2, GitFileStatus.ADDED);
        assertEquals(GitFileStatus.ADDED, manager.getStatus(pkgDir));

        // One modified, one added -> MODIFIED takes highest priority
        manager.setStatus(file1, GitFileStatus.MODIFIED);
        assertEquals(GitFileStatus.MODIFIED, manager.getStatus(pkgDir));
    }

    @Test
    void testDirectoryHighlightToggle(@TempDir Path tempDir) throws IOException {
        Path entityDir = tempDir.resolve("order/entity");
        Files.createDirectories(entityDir);
        Path orderFile = entityDir.resolve("Order.java");
        Files.createFile(orderFile);

        manager.setStatus(orderFile, GitFileStatus.MODIFIED);
        GitConfirmationManager.getInstance().setHighlightDirectoriesWithModifiedFiles(true);
        assertEquals(GitFileStatus.MODIFIED, manager.getStatus(entityDir));

        // When toggled off, directories are returned as NORMAL
        GitConfirmationManager.getInstance().setHighlightDirectoriesWithModifiedFiles(false);
        assertEquals(GitFileStatus.NORMAL, manager.getStatus(entityDir));

        // Re-enable
        GitConfirmationManager.getInstance().setHighlightDirectoriesWithModifiedFiles(true);
        assertEquals(GitFileStatus.MODIFIED, manager.getStatus(entityDir));
    }
}
