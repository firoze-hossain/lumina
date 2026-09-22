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
}
