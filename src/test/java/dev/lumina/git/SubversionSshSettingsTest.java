package dev.lumina.git;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SubversionSshSettingsTest {

    private SubversionSettingsManager manager;

    @BeforeEach
    void setUp() {
        manager = SubversionSettingsManager.getInstance();
        manager.revertToDefaults();
    }

    @Test
    void testDefaultsMatchingIntelliJImage1() {
        assertEquals("ssh", manager.getSshExecutablePath());
        assertEquals("", manager.getSshUserName());
        assertEquals(22, manager.getSshPort());
        assertEquals(SubversionSettingsManager.SshAuthMode.SUBVERSION_CONFIG, manager.getSshAuthMode());
        assertEquals("", manager.getSshPrivateKeyPath());
        assertEquals("$SVN_SSH ssh -q", manager.getSshTunnel());
        assertEquals("", manager.getSvnSshEnv());
    }

    @Test
    void testAuthModeTogglingAndParameters() {
        manager.setSshExecutablePath("/usr/local/bin/custom-ssh");
        manager.setSshUserName("svn_admin");
        manager.setSshPort(2222);

        assertEquals("/usr/local/bin/custom-ssh", manager.getSshExecutablePath());
        assertEquals("svn_admin", manager.getSshUserName());
        assertEquals(2222, manager.getSshPort());

        manager.setSshAuthMode(SubversionSettingsManager.SshAuthMode.PRIVATE_KEY);
        manager.setSshPrivateKeyPath("/home/user/.ssh/id_rsa");

        assertEquals(SubversionSettingsManager.SshAuthMode.PRIVATE_KEY, manager.getSshAuthMode());
        assertEquals("/home/user/.ssh/id_rsa", manager.getSshPrivateKeyPath());

        manager.setSshAuthMode(SubversionSettingsManager.SshAuthMode.PASSWORD);
        assertEquals(SubversionSettingsManager.SshAuthMode.PASSWORD, manager.getSshAuthMode());
    }

    @Test
    void testUpdateSshTunnel() {
        manager.setSshTunnel("custom-tunnel -q");
        assertEquals("custom-tunnel -q", manager.getSshTunnel());

        manager.updateSshTunnel();
        // Since SVN_SSH is likely unset in test env, it should fall back to standard tunnel default
        assertNotNull(manager.getSshTunnel());
        assertFalse(manager.getSshTunnel().isBlank());
    }

    @Test
    void testListenersNotification() {
        AtomicBoolean triggered = new AtomicBoolean(false);
        Runnable listener = () -> triggered.set(true);
        manager.addListener(listener);

        manager.setSshExecutablePath("ssh-custom");
        assertTrue(triggered.get());
        assertEquals("ssh-custom", manager.getSshExecutablePath());

        triggered.set(false);
        manager.setSshPort(2200);
        assertTrue(triggered.get());
        assertEquals(2200, manager.getSshPort());

        triggered.set(false);
        manager.setSshAuthMode(SubversionSettingsManager.SshAuthMode.PASSWORD);
        assertTrue(triggered.get());
        assertEquals(SubversionSettingsManager.SshAuthMode.PASSWORD, manager.getSshAuthMode());

        manager.removeListener(listener);
    }
}
