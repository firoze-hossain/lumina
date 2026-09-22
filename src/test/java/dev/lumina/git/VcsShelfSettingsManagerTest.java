package dev.lumina.git;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class VcsShelfSettingsManagerTest {

    private VcsShelfSettingsManager manager;

    @BeforeEach
    void setUp() {
        manager = VcsShelfSettingsManager.getInstance();
        manager.revertToDefaults();
    }

    @Test
    void testDefaults() {
        assertFalse(manager.isRemoveAppliedFiles());
        assertTrue(manager.isShelveBaseRevisions());
        assertFalse(manager.isCustomLocationConfigured());
        assertEquals("", manager.getCustomShelvesLocation());
    }

    @Test
    void testPathResolution(@TempDir Path tempDir) {
        manager.setCurrentProjectPath(tempDir);
        Path defaultLoc = manager.getDefaultShelvesLocation();
        assertEquals(tempDir.resolve(".idea").resolve("shelf"), defaultLoc);
        assertEquals(defaultLoc, manager.getCurrentShelvesLocation());
    }

    @Test
    void testCustomLocationAndListeners(@TempDir Path tempDir) {
        AtomicBoolean called = new AtomicBoolean(false);
        Runnable listener = () -> called.set(true);
        manager.addListener(listener);

        Path customDir = tempDir.resolve("my-custom-shelves");
        manager.setShelvesLocation(customDir.toString(), false);

        assertTrue(called.get());
        assertTrue(manager.isCustomLocationConfigured());
        assertEquals(customDir, manager.getCurrentShelvesLocation());

        called.set(false);
        manager.setRemoveAppliedFiles(true);
        assertTrue(called.get());
        assertTrue(manager.isRemoveAppliedFiles());

        called.set(false);
        manager.setShelveBaseRevisions(false);
        assertTrue(called.get());
        assertFalse(manager.isShelveBaseRevisions());

        manager.removeListener(listener);
    }

    @Test
    void testMoveShelves(@TempDir Path tempDir) throws IOException {
        Path projectDir = tempDir.resolve("project");
        Path oldShelf = projectDir.resolve(".idea").resolve("shelf");
        Files.createDirectories(oldShelf);
        Files.writeString(oldShelf.resolve("patch1.patch"), "sample patch content");

        manager.setCurrentProjectPath(projectDir);
        assertEquals(oldShelf, manager.getCurrentShelvesLocation());

        Path newShelf = tempDir.resolve("moved-shelves");
        manager.setShelvesLocation(newShelf.toString(), true);

        assertEquals(newShelf, manager.getCurrentShelvesLocation());
        assertTrue(Files.exists(newShelf.resolve("patch1.patch")));
        assertEquals("sample patch content", Files.readString(newShelf.resolve("patch1.patch")));
    }
}
