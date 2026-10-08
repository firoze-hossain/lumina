package dev.lumina.build;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PythonDebuggerSettings and PythonDebuggerManager.
 */
public class PythonDebuggerManagerTest {

    private PythonDebuggerManager manager;

    @BeforeEach
    void setUp() {
        manager = PythonDebuggerManager.getInstance();
    }

    @Test
    void testDefaultSettings() {
        PythonDebuggerSettings settings = manager.getSettings();
        assertTrue(settings.isAttachToSubprocess(), "Attach to subprocess should be true by default");
        assertFalse(settings.isCollectRunTimeTypes(), "Collect run-time types should be false by default");
        assertFalse(settings.isGeventCompatible(), "Gevent compatible should be false by default");
        assertFalse(settings.isDropIntoDebuggerOnFailedTests(), "Drop into debugger on failed tests should be false by default");
        assertTrue(settings.isPyQtCompatible(), "PyQt compatible should be true by default");
        assertEquals("Auto", settings.getPyQtBackend());
        assertEquals("python", settings.getAttachProcessFilter());
        assertEquals(60000, settings.getEvalResponseTimeoutMs());
    }

    @Test
    void testCloneAndEquals() {
        PythonDebuggerSettings s1 = new PythonDebuggerSettings();
        PythonDebuggerSettings s2 = s1.clone();
        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());

        s2.setGeventCompatible(true);
        assertNotEquals(s1, s2);

        s2.setGeventCompatible(false);
        assertEquals(s1, s2);

        s2.setEvalResponseTimeoutMs(30000);
        assertNotEquals(s1, s2);
    }

    @Test
    void testPyQtBackendExtensibility() {
        assertTrue(manager.getAvailablePyQtBackends().contains("Auto"));
        assertTrue(manager.getAvailablePyQtBackends().contains("PyQt5"));
        assertTrue(manager.getAvailablePyQtBackends().contains("PySide6"));

        manager.registerPyQtBackend("PyQtCustom");
        assertTrue(manager.getAvailablePyQtBackends().contains("PyQtCustom"));
    }

    @Test
    void testClearCachesCallback() {
        AtomicBoolean cleared = new AtomicBoolean(false);
        Runnable listener = () -> cleared.set(true);
        manager.addCacheClearListener(listener);

        manager.clearCaches();
        assertTrue(cleared.get(), "Clear cache listener should be notified");

        manager.removeCacheClearListener(listener);
    }

    @Test
    void testPersistenceCycle() {
        PythonDebuggerSettings original = manager.getSettings();

        PythonDebuggerSettings modified = new PythonDebuggerSettings();
        modified.setAttachToSubprocess(false);
        modified.setCollectRunTimeTypes(true);
        modified.setGeventCompatible(true);
        modified.setDropIntoDebuggerOnFailedTests(true);
        modified.setPyQtCompatible(false);
        modified.setPyQtBackend("PyQt6");
        modified.setAttachProcessFilter("lumina-py");
        modified.setEvalResponseTimeoutMs(120000);

        manager.setSettings(modified);
        manager.saveSettings();

        // Reload
        manager.loadSettings();
        PythonDebuggerSettings reloaded = manager.getSettings();
        assertEquals(modified, reloaded);

        // Restore
        manager.setSettings(original);
    }
}
