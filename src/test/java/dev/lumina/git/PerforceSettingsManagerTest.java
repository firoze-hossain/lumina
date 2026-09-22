package dev.lumina.git;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class PerforceSettingsManagerTest {

    private PerforceSettingsManager manager;

    @BeforeEach
    void setUp() {
        manager = PerforceSettingsManager.getInstance();
        manager.revertToDefaults();
    }

    @Test
    void testDefaultsMatchingIntelliJImage1() {
        assertTrue(manager.isPerforceOnline());
        assertFalse(manager.isSwitchOfflineAuto());
        assertEquals("none", manager.getCharset());
        assertEquals(PerforceSettingsManager.ConfigMode.ENVIRONMENT_VALUES, manager.getConfigMode());
        assertEquals(PerforceSettingsManager.IgnoreMode.P4IGNORE_ENV, manager.getIgnoreMode());
        assertFalse(manager.isDumpCommands());
        assertTrue(manager.isUseLoginAuthentication());
        assertEquals("p4", manager.getP4ExecutablePath());
        assertEquals("p4vc", manager.getP4vcExecutablePath());
        assertTrue(manager.isShowBranchingHistory());
        assertTrue(manager.isShowIntegratedChangelists());
        assertEquals(20, manager.getServerTimeoutSeconds());
        assertFalse(manager.isEnableJobsSupport());
        assertTrue(manager.isFindIgnoredFilesUsingP4());
        assertTrue(manager.isAlwaysSyncLocalChangelists());
    }

    @Test
    void testConnectionParametersAndMode() {
        manager.setConfigMode(PerforceSettingsManager.ConfigMode.CONNECTION_PARAMETERS);
        manager.setServerPort("perforce.internal:1666");
        manager.setUser("developer");
        manager.setClientWorkspace("dev-workspace");

        assertEquals(PerforceSettingsManager.ConfigMode.CONNECTION_PARAMETERS, manager.getConfigMode());
        assertEquals("perforce.internal:1666", manager.getServerPort());
        assertEquals("developer", manager.getUser());
        assertEquals("dev-workspace", manager.getClientWorkspace());
    }

    @Test
    void testIgnoreModeAndPath() {
        manager.setIgnoreMode(PerforceSettingsManager.IgnoreMode.IGNORE_SETTINGS);
        manager.setPathToIgnoreFile("/path/to/.p4ignore");

        assertEquals(PerforceSettingsManager.IgnoreMode.IGNORE_SETTINGS, manager.getIgnoreMode());
        assertEquals("/path/to/.p4ignore", manager.getPathToIgnoreFile());
    }

    @Test
    void testTestConnectionWithInvalidBinary() {
        manager.setP4ExecutablePath("non_existent_p4_binary_xyz_123");
        PerforceSettingsManager.TestResult result = manager.testConnection();
        assertFalse(result.success());
        assertTrue(result.message().contains("Cannot run program") || result.message().contains("Failed"));
    }

    @Test
    void testListenersNotification() {
        AtomicBoolean triggered = new AtomicBoolean(false);
        Runnable listener = () -> triggered.set(true);
        manager.addListener(listener);

        manager.setPerforceOnline(false);
        assertTrue(triggered.get());
        assertFalse(manager.isPerforceOnline());

        triggered.set(false);
        manager.setCharset("utf8");
        assertTrue(triggered.get());
        assertEquals("utf8", manager.getCharset());

        triggered.set(false);
        manager.setServerTimeoutSeconds(45);
        assertTrue(triggered.get());
        assertEquals(45, manager.getServerTimeoutSeconds());

        manager.removeListener(listener);
    }
}
