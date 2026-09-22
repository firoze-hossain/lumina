package dev.lumina.git;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SubversionSettingsManagerTest {

    private SubversionSettingsManager manager;

    @BeforeEach
    void setUp() {
        manager = SubversionSettingsManager.getInstance();
        manager.revertToDefaults();
    }

    @Test
    void testDefaultsMatchingIntelliJImages345() {
        // Image 3: General
        assertEquals("svn", manager.getSvnExecutablePath());
        assertFalse(manager.isEnableInteractiveMode());
        assertFalse(manager.isUseCustomConfigDirectory());
        assertEquals(Path.of(System.getProperty("user.home"), ".subversion").toString(), manager.getCustomConfigDirectoryPath());

        // Image 4: Network
        assertFalse(manager.isUseGeneralProxySettings());
        assertEquals(0, manager.getHttpTimeoutSeconds());
        assertEquals(30, manager.getSshConnectionTimeoutSeconds());
        assertEquals(30, manager.getSshReadTimeoutSeconds());
        assertEquals(SubversionSettingsManager.SslProtocol.ALL, manager.getSslProtocol());

        // Image 5: Presentation
        assertFalse(manager.isCheckMergeInfo());
        assertEquals(500, manager.getMaxRevisionsLookBack());
        assertTrue(manager.isShowMergeSource());
        assertTrue(manager.isIgnoreWhitespaceInAnnotations());
    }

    @Test
    void testNetworkSettingsModification() {
        manager.setUseGeneralProxySettings(true);
        manager.setHttpTimeoutSeconds(60);
        manager.setSshConnectionTimeoutSeconds(45);
        manager.setSshReadTimeoutSeconds(45);
        manager.setSslProtocol(SubversionSettingsManager.SslProtocol.TLSV1);

        assertTrue(manager.isUseGeneralProxySettings());
        assertEquals(60, manager.getHttpTimeoutSeconds());
        assertEquals(45, manager.getSshConnectionTimeoutSeconds());
        assertEquals(45, manager.getSshReadTimeoutSeconds());
        assertEquals(SubversionSettingsManager.SslProtocol.TLSV1, manager.getSslProtocol());
    }

    @Test
    void testPresentationSettingsModification() {
        manager.setCheckMergeInfo(true);
        manager.setMaxRevisionsLookBack(1000);
        manager.setShowMergeSource(false);
        manager.setIgnoreWhitespaceInAnnotations(false);

        assertTrue(manager.isCheckMergeInfo());
        assertEquals(1000, manager.getMaxRevisionsLookBack());
        assertFalse(manager.isShowMergeSource());
        assertFalse(manager.isIgnoreWhitespaceInAnnotations());
    }

    @Test
    void testClearAuthCache(@TempDir Path tempDir) throws IOException {
        Path authDir = tempDir.resolve("auth");
        Files.createDirectories(authDir.resolve("svn.simple"));
        Files.writeString(authDir.resolve("svn.simple").resolve("token123"), "dummy-credentials");

        manager.setCustomConfigDirectoryPath(tempDir.toString());
        boolean cleared = manager.clearAuthCache();
        assertTrue(cleared);

        assertFalse(Files.exists(authDir.resolve("svn.simple").resolve("token123")));
        assertTrue(Files.exists(authDir));
    }

    @Test
    void testListenersNotification() {
        AtomicBoolean triggered = new AtomicBoolean(false);
        Runnable listener = () -> triggered.set(true);
        manager.addListener(listener);

        manager.setSvnExecutablePath("/usr/bin/svn");
        assertTrue(triggered.get());
        assertEquals("/usr/bin/svn", manager.getSvnExecutablePath());

        triggered.set(false);
        manager.setEnableInteractiveMode(true);
        assertTrue(triggered.get());
        assertTrue(manager.isEnableInteractiveMode());

        triggered.set(false);
        manager.setMaxRevisionsLookBack(250);
        assertTrue(triggered.get());
        assertEquals(250, manager.getMaxRevisionsLookBack());

        manager.removeListener(listener);
    }
}
