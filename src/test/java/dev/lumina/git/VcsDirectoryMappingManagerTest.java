package dev.lumina.git;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class VcsDirectoryMappingManagerTest {

    private VcsDirectoryMappingManager manager;

    @BeforeEach
    void setUp() {
        manager = VcsDirectoryMappingManager.getInstance();
        manager.revertToDefaults();
    }

    @Test
    void testDefaults() {
        assertTrue(manager.isAutomaticMappingDetection());
        assertEquals(1, manager.getMappings().size());
        VcsDirectoryMappingManager.VcsMapping projectMapping = manager.getMappings().get(0);
        assertEquals("<Project>", projectMapping.getDirectory());
        assertEquals("Git", projectMapping.getVcs());
        assertTrue(projectMapping.isProject());
    }

    @Test
    void testAddAndUpdateMapping() {
        manager.addMapping("/home/user/module1", "Git");
        assertEquals(2, manager.getMappings().size());
        assertEquals("/home/user/module1", manager.getMappings().get(1).getDirectory());
        assertEquals("Git", manager.getMappings().get(1).getVcs());

        // Update
        manager.updateMapping(1, "/home/user/module1", "Mercurial");
        assertEquals("Mercurial", manager.getMappings().get(1).getVcs());

        // Remove
        manager.removeMapping(1);
        assertEquals(1, manager.getMappings().size());
    }

    @Test
    void testProjectMappingCannotBeDeleted() {
        // Attempt to remove <Project> mapping sets VCS to <none>
        manager.removeMapping(0);
        assertEquals(1, manager.getMappings().size());
        assertEquals("<none>", manager.getMappings().get(0).getVcs());
    }

    @Test
    void testAutoDetection(@TempDir Path tempDir) throws IOException {
        Path gitDir = tempDir.resolve(".git");
        Files.createDirectories(gitDir);

        manager.detectRepositories(tempDir);
        assertTrue(manager.getDetectedRepositoriesCount() >= 1);
    }

    @Test
    void testListenerNotification() {
        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addListener(listener);

        manager.addMapping("/test/dir", "Subversion");
        assertTrue(notified.get());

        notified.set(false);
        manager.setAutomaticMappingDetection(false);
        assertTrue(notified.get());

        manager.removeListener(listener);
    }
}
