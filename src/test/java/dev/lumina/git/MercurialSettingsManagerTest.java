package dev.lumina.git;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class MercurialSettingsManagerTest {

    private MercurialSettingsManager manager;

    @BeforeEach
    void setUp() {
        manager = MercurialSettingsManager.getInstance();
        manager.revertToDefaults();
    }

    @Test
    void testDefaultsMatchingIntelliJ() {
        assertEquals("", manager.getHgExecutablePath());
        assertFalse(manager.isSetPathOnlyForProject());
        assertFalse(manager.isCheckIncomingOutgoingChangesets());
        assertTrue(manager.isIgnoreWhitespaceInAnnotations());
    }

    @Test
    void testAutoDetectionAndEffectiveExecutable() {
        String autoDetected = manager.getAutoDetectedHgPath();
        assertNotNull(autoDetected);
        assertFalse(autoDetected.isBlank());

        assertEquals(autoDetected, manager.getEffectiveHgExecutable());

        manager.setHgExecutablePath("/opt/custom/hg");
        assertEquals("/opt/custom/hg", manager.getEffectiveHgExecutable());

        manager.setHgExecutablePath("");
        assertEquals(autoDetected, manager.getEffectiveHgExecutable());
    }

    @Test
    void testTestHgExecutable() {
        MercurialSettingsManager.TestResult res = manager.testHgExecutable("non_existent_hg_binary_123");
        assertFalse(res.success());
        assertTrue(res.message().contains("Cannot run Mercurial"));
    }

    @Test
    void testTogglingAndListeners() {
        AtomicBoolean called = new AtomicBoolean(false);
        Runnable listener = () -> called.set(true);
        manager.addListener(listener);

        manager.setCheckIncomingOutgoingChangesets(true);
        assertTrue(called.get());
        assertTrue(manager.isCheckIncomingOutgoingChangesets());

        called.set(false);
        manager.setIgnoreWhitespaceInAnnotations(false);
        assertTrue(called.get());
        assertFalse(manager.isIgnoreWhitespaceInAnnotations());

        called.set(false);
        manager.setSetPathOnlyForProject(true);
        assertTrue(called.get());
        assertTrue(manager.isSetPathOnlyForProject());

        manager.removeListener(listener);
    }
}
