package dev.lumina.plugin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class RequiredPluginsManagerTest {

    private RequiredPluginsManager manager;

    @BeforeEach
    void setUp() {
        manager = RequiredPluginsManager.getInstance();
        manager.clear();
    }

    @Test
    void testVersionCompatibility() {
        RequiredPlugin p = new RequiredPlugin("angular", "Angular", "1.5.0", "3.0.0");
        assertTrue(p.isVersionCompatible("1.5.0"));
        assertTrue(p.isVersionCompatible("2.0.0"));
        assertTrue(p.isVersionCompatible("3.0.0"));
        assertFalse(p.isVersionCompatible("1.4.9"));
        assertFalse(p.isVersionCompatible("3.1.0"));
        assertFalse(p.isVersionCompatible(null));
        assertFalse(p.isVersionCompatible(""));

        // Only min version specified
        RequiredPlugin minOnly = new RequiredPlugin("go", "Go", "2024.1.0", "");
        assertTrue(minOnly.isVersionCompatible("2024.1.0"));
        assertTrue(minOnly.isVersionCompatible("2024.2.0"));
        assertFalse(minOnly.isVersionCompatible("2023.3.0"));

        // Only max version specified
        RequiredPlugin maxOnly = new RequiredPlugin("rust", "Rust", "", "1.10.0");
        assertTrue(maxOnly.isVersionCompatible("1.0.0"));
        assertTrue(maxOnly.isVersionCompatible("1.10.0"));
        assertFalse(maxOnly.isVersionCompatible("1.11.0"));

        // No version constraint
        RequiredPlugin anyVer = new RequiredPlugin("python", "Python", "", "");
        assertTrue(anyVer.isVersionCompatible("1.0.0"));
        assertTrue(anyVer.isVersionCompatible("99.9.9"));
    }

    @Test
    void testAddUpdateRemoveRequiredPlugin() {
        RequiredPlugin angular = new RequiredPlugin("angular", "Angular", "1.0", "2.0");
        manager.addRequiredPlugin(angular);

        List<RequiredPlugin> list = manager.getRequiredPlugins();
        assertEquals(1, list.size());
        assertEquals("Angular", list.get(0).getPluginName());
        assertEquals("1.0", list.get(0).getMinVersion());
        assertEquals("2.0", list.get(0).getMaxVersion());

        // Update
        RequiredPlugin angularUpdated = new RequiredPlugin("angular", "Angular", "2.0", "4.0");
        manager.updateRequiredPlugin(angular, angularUpdated);

        list = manager.getRequiredPlugins();
        assertEquals(1, list.size());
        assertEquals("2.0", list.get(0).getMinVersion());
        assertEquals("4.0", list.get(0).getMaxVersion());

        // Remove
        manager.removeRequiredPlugin(angularUpdated);
        assertTrue(manager.getRequiredPlugins().isEmpty());
    }

    @Test
    void testAvailablePluginCatalog() {
        List<String> available = manager.getAvailablePluginNames();
        assertNotNull(available);
        assertFalse(available.isEmpty());

        // Standard plugins from media_1790045975642.png
        assertTrue(available.contains("Angular"));
        assertTrue(available.contains("AOP Pointcut Language"));
        assertTrue(available.contains("Apache Velocity"));
        assertTrue(available.contains("Artifacts Repository Search"));
        assertTrue(available.contains("Async Profiler for IDE Performance Testing"));
        assertTrue(available.contains("Backup and Sync"));
        assertTrue(available.contains("Bytecode Viewer"));
        assertTrue(available.contains("Chinese (Simplified) Language Pack / 中文语言包"));
    }

    @Test
    void testValidationDiagnostics() {
        // Add a plugin known to exist in PluginRegistry (e.g. Go, 2024.1.0)
        RequiredPlugin goReq = new RequiredPlugin("go", "Go", "2024.1.0", "2025.0.0");
        manager.addRequiredPlugin(goReq);

        // Add a non-existent plugin
        RequiredPlugin nonExistent = new RequiredPlugin("unknown.plugin", "Unknown Plugin", "1.0", "2.0");
        manager.addRequiredPlugin(nonExistent);

        List<String> issues = manager.validateRequirements();
        assertNotNull(issues);
        assertFalse(issues.isEmpty());

        boolean hasUnknownWarning = issues.stream().anyMatch(s -> s.contains("Unknown Plugin") && s.contains("not installed"));
        assertTrue(hasUnknownWarning);

        // Clean up
        manager.clear();
    }

    @Test
    void testListenerNotification() {
        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addListener(listener);

        RequiredPlugin plugin = new RequiredPlugin("rust", "Rust", "1.0", "");
        manager.addRequiredPlugin(plugin);
        assertTrue(notified.get());

        notified.set(false);
        manager.removeRequiredPlugin(plugin);
        assertTrue(notified.get());

        manager.removeListener(listener);
    }
}
