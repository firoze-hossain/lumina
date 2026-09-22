package dev.lumina.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class TrustedLocationsManagerTest {

    private TrustedLocationsManager manager;

    @BeforeEach
    void setUp() {
        manager = TrustedLocationsManager.getInstance();
        manager.revertToDefaults();
    }

    @Test
    void testDefaults() {
        List<String> locations = manager.getLocations();
        assertNotNull(locations);
        assertFalse(locations.isEmpty());
        // Verify default locations are present
        String userHome = System.getProperty("user.home").replace('\\', '/');
        assertTrue(locations.stream().anyMatch(l -> l.startsWith(userHome)));
    }

    @Test
    void testNormalizePath() {
        assertEquals("", TrustedLocationsManager.normalizePath(null));
        assertEquals("", TrustedLocationsManager.normalizePath("   "));
        assertEquals("/home/user/project", TrustedLocationsManager.normalizePath("  /home/user/project  "));
        assertEquals("C:/Users/user/project", TrustedLocationsManager.normalizePath("C:\\Users\\user\\project"));
    }

    @Test
    void testAddAndRemoveLocation() {
        manager.clear();
        assertEquals(0, manager.getLocations().size());

        manager.addLocation("/custom/workspace/alpha");
        assertEquals(1, manager.getLocations().size());
        assertEquals("/custom/workspace/alpha", manager.getLocations().get(0));

        // Duplicate addition should be ignored
        manager.addLocation("/custom/workspace/alpha");
        assertEquals(1, manager.getLocations().size());

        // Null and blank addition ignored
        manager.addLocation(null);
        manager.addLocation("   ");
        assertEquals(1, manager.getLocations().size());

        // Remove by path
        manager.removeLocation("/custom/workspace/alpha");
        assertEquals(0, manager.getLocations().size());

        // Add two and remove by index
        manager.addLocation("/custom/workspace/1");
        manager.addLocation("/custom/workspace/2");
        assertEquals(2, manager.getLocations().size());
        manager.removeLocation(0);
        assertEquals(1, manager.getLocations().size());
        assertEquals("/custom/workspace/2", manager.getLocations().get(0));
    }

    @Test
    void testMoveUpAndDown() {
        manager.clear();
        manager.addLocation("/path/A");
        manager.addLocation("/path/B");
        manager.addLocation("/path/C");

        assertEquals(List.of("/path/A", "/path/B", "/path/C"), manager.getLocations());

        // Move B up
        manager.moveUp(1);
        assertEquals(List.of("/path/B", "/path/A", "/path/C"), manager.getLocations());

        // Moving top item up should do nothing
        manager.moveUp(0);
        assertEquals(List.of("/path/B", "/path/A", "/path/C"), manager.getLocations());

        // Move B down
        manager.moveDown(0);
        assertEquals(List.of("/path/A", "/path/B", "/path/C"), manager.getLocations());

        // Moving bottom item down should do nothing
        manager.moveDown(2);
        assertEquals(List.of("/path/A", "/path/B", "/path/C"), manager.getLocations());
    }

    @Test
    void testIsTrusted() {
        manager.clear();
        manager.addLocation("/trusted/projects");

        // Null check
        assertFalse(manager.isTrusted(null));

        // Exact match
        assertTrue(manager.isTrusted(Paths.get("/trusted/projects")));

        // Subdirectory match
        assertTrue(manager.isTrusted(Paths.get("/trusted/projects/lumina/src")));

        // Untrusted path with partial name overlap
        assertFalse(manager.isTrusted(Paths.get("/trusted/projects_untrusted")));

        // Completely untrusted path
        assertFalse(manager.isTrusted(Paths.get("/tmp/malicious")));
    }

    @Test
    void testListenerNotification() {
        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addListener(listener);

        notified.set(false);
        manager.addLocation("/listener/test/path");
        assertTrue(notified.get());

        notified.set(false);
        manager.removeLocation("/listener/test/path");
        assertTrue(notified.get());

        manager.removeListener(listener);
    }
}
