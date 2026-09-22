package dev.lumina.git;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class PerforceMcpSettingsManagerTest {

    private PerforceMcpSettingsManager manager;

    @BeforeEach
    void setUp() {
        manager = PerforceMcpSettingsManager.getInstance();
        manager.revertToDefaults();
    }

    @Test
    void testDefaultsMatchingIntelliJImage2() {
        assertEquals("", manager.getMcpExecutablePath());
        assertTrue(manager.isReadOnlyMode());
        assertFalse(manager.isAllowAnonymousUsageStats());
        assertEquals(PerforceMcpSettingsManager.McpEnvironmentMode.USE_PROJECT_SETTINGS, manager.getEnvironmentMode());
        assertEquals("", manager.getCustomServerPort());
        assertEquals("", manager.getCustomUser());
        assertEquals("", manager.getCustomClientWorkspace());

        assertTrue(manager.isAllToolsetsEnabled());
        assertTrue(manager.isToolsetFiles());
        assertTrue(manager.isToolsetChangelists());
        assertTrue(manager.isToolsetShelves());
        assertTrue(manager.isToolsetWorkspaces());
        assertTrue(manager.isToolsetJobs());
    }

    @Test
    void testEnvironmentModeAndCustomParameters() {
        manager.setEnvironmentMode(PerforceMcpSettingsManager.McpEnvironmentMode.OVERRIDE_CUSTOM);
        manager.setCustomServerPort("ssl:p4.corp.net:1666");
        manager.setCustomUser("ci_user");
        manager.setCustomClientWorkspace("ci_ws");

        assertEquals(PerforceMcpSettingsManager.McpEnvironmentMode.OVERRIDE_CUSTOM, manager.getEnvironmentMode());
        assertEquals("ssl:p4.corp.net:1666", manager.getCustomServerPort());
        assertEquals("ci_user", manager.getCustomUser());
        assertEquals("ci_ws", manager.getCustomClientWorkspace());
    }

    @Test
    void testToolsetsToggling() {
        manager.setToolsetJobs(false);
        assertFalse(manager.isToolsetJobs());
        assertFalse(manager.isAllToolsetsEnabled());

        manager.setAllToolsets(false);
        assertFalse(manager.isToolsetFiles());
        assertFalse(manager.isToolsetChangelists());
        assertFalse(manager.isToolsetShelves());
        assertFalse(manager.isToolsetWorkspaces());
        assertFalse(manager.isToolsetJobs());
        assertFalse(manager.isAllToolsetsEnabled());

        manager.setAllToolsets(true);
        assertTrue(manager.isAllToolsetsEnabled());
        assertTrue(manager.isToolsetFiles());
    }

    @Test
    void testListenersNotification() {
        AtomicBoolean triggered = new AtomicBoolean(false);
        Runnable listener = () -> triggered.set(true);
        manager.addListener(listener);

        manager.setMcpExecutablePath("/usr/local/bin/p4-mcp");
        assertTrue(triggered.get());
        assertEquals("/usr/local/bin/p4-mcp", manager.getMcpExecutablePath());

        triggered.set(false);
        manager.setReadOnlyMode(false);
        assertTrue(triggered.get());
        assertFalse(manager.isReadOnlyMode());

        triggered.set(false);
        manager.setAllowAnonymousUsageStats(true);
        assertTrue(triggered.get());
        assertTrue(manager.isAllowAnonymousUsageStats());

        manager.removeListener(listener);
    }
}
