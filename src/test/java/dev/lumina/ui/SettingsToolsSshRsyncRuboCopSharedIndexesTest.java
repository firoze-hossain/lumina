package dev.lumina.ui;

import dev.lumina.tools.*;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit, UI lifecycle, dialog integration, and brand isolation test suite
 * for the 5 dynamic Tools settings pages:
 * 1. Rsync
 * 2. RuboCop
 * 3. Shared Indexes
 * 4. SSH Configurations
 * 5. SSH Terminal
 */
public class SettingsToolsSshRsyncRuboCopSharedIndexesTest {

    private static boolean javaFxAvailable = false;

    @BeforeAll
    static void initFx() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            try {
                Platform.startup(() -> {
                    javaFxAvailable = true;
                    latch.countDown();
                });
            } catch (IllegalStateException e) {
                javaFxAvailable = true;
                latch.countDown();
            }
            javaFxAvailable = latch.await(5, TimeUnit.SECONDS);
        } catch (Throwable t) {
            javaFxAvailable = false;
        }
    }

    private void runOnFx(Runnable action) throws Exception {
        if (!javaFxAvailable) return;
        CountDownLatch latch = new CountDownLatch(1);
        Throwable[] err = new Throwable[1];
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                err[0] = t;
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS), "JavaFX thread timed out");
        if (err[0] != null) {
            throw new RuntimeException(err[0]);
        }
    }

    // ------------------------------------------------------------- 1. Rsync

    @Test
    void testRsyncModelDefaultsAndCloning() {
        RsyncSettings settings = new RsyncSettings();
        assertEquals(RsyncSettings.DEFAULT_RSYNC_EXECUTABLE, settings.getRsyncExecutablePath());
        assertEquals(RsyncSettings.DEFAULT_RSYNC_OPTIONS, settings.getRsyncOptions());
        assertEquals(RsyncSettings.DEFAULT_SHELL_EXECUTABLE, settings.getShellExecutablePath());

        RsyncSettings clone = settings.clone();
        assertEquals(settings, clone);
        assertEquals(settings.hashCode(), clone.hashCode());

        clone.setRsyncOptions("-avzP");
        clone.setShellExecutablePath("/usr/bin/ssh");
        assertNotEquals(settings, clone);
    }

    @Test
    void testRsyncManagerLifecycle() {
        RsyncSettingsManager manager = RsyncSettingsManager.getInstance();
        assertNotNull(manager);

        RsyncSettings original = manager.getSettings().clone();
        try {
            RsyncSettings custom = new RsyncSettings();
            custom.setRsyncExecutablePath("/opt/homebrew/bin/rsync");
            custom.setRsyncOptions("-avz --delete");
            custom.setShellExecutablePath("/opt/homebrew/bin/ssh");

            AtomicBoolean listenerFired = new AtomicBoolean(false);
            Runnable listener = () -> listenerFired.set(true);
            manager.addChangeListener(listener);

            manager.setSettings(custom);
            assertTrue(listenerFired.get());

            RsyncSettings loaded = manager.getSettings();
            assertEquals("/opt/homebrew/bin/rsync", loaded.getRsyncExecutablePath());
            assertEquals("-avz --delete", loaded.getRsyncOptions());
            assertEquals("/opt/homebrew/bin/ssh", loaded.getShellExecutablePath());

            manager.removeChangeListener(listener);
        } finally {
            manager.setSettings(original);
        }
    }

    @Test
    void testSettingsToolsRsyncPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        RsyncSettings original = RsyncSettingsManager.getInstance().getSettings().clone();
        try {
            runOnFx(() -> {
                SettingsToolsRsyncPage page = new SettingsToolsRsyncPage();
                assertFalse(page.isModified());

                page.getRsyncOptionsField().setText("-avzP");
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());
                assertEquals("-avzP", RsyncSettingsManager.getInstance().getSettings().getRsyncOptions());

                page.reset();
                assertFalse(page.isModified());
            });
        } finally {
            RsyncSettingsManager.getInstance().setSettings(original);
        }
    }

    // ------------------------------------------------------------- 2. RuboCop

    @Test
    void testRuboCopModelDefaultsAndCloning() {
        RuboCopSettings settings = new RuboCopSettings();
        assertEquals("", settings.getConfigFile());
        assertFalse(settings.isUseStandardGem());
        assertFalse(settings.isRunRuboCopOnSave());

        RuboCopSettings clone = settings.clone();
        assertEquals(settings, clone);
        assertEquals(settings.hashCode(), clone.hashCode());

        clone.setConfigFile("/path/to/.rubocop.yml");
        clone.setUseStandardGem(true);
        clone.setRunRuboCopOnSave(true);
        assertNotEquals(settings, clone);
    }

    @Test
    void testRuboCopManagerLifecycle() {
        RuboCopSettingsManager manager = RuboCopSettingsManager.getInstance();
        assertNotNull(manager);

        RuboCopSettings original = manager.getSettings().clone();
        try {
            RuboCopSettings custom = new RuboCopSettings();
            custom.setConfigFile(".rubocop-custom.yml");
            custom.setUseStandardGem(true);
            custom.setRunRuboCopOnSave(true);

            AtomicBoolean listenerFired = new AtomicBoolean(false);
            Runnable listener = () -> listenerFired.set(true);
            manager.addChangeListener(listener);

            manager.setSettings(custom);
            assertTrue(listenerFired.get());

            RuboCopSettings loaded = manager.getSettings();
            assertEquals(".rubocop-custom.yml", loaded.getConfigFile());
            assertTrue(loaded.isUseStandardGem());
            assertTrue(loaded.isRunRuboCopOnSave());

            manager.removeChangeListener(listener);
        } finally {
            manager.setSettings(original);
        }
    }

    @Test
    void testSettingsToolsRuboCopPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        RuboCopSettings original = RuboCopSettingsManager.getInstance().getSettings().clone();
        try {
            runOnFx(() -> {
                SettingsToolsRuboCopPage page = new SettingsToolsRuboCopPage();
                assertFalse(page.isModified());

                page.getUseStandardGemCheck().setSelected(true);
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());
                assertTrue(RuboCopSettingsManager.getInstance().getSettings().isUseStandardGem());

                page.reset();
                assertFalse(page.isModified());
            });
        } finally {
            RuboCopSettingsManager.getInstance().setSettings(original);
        }
    }

    // ------------------------------------------------------------- 3. Shared Indexes

    @Test
    void testSharedIndexesModelDefaultsAndCloning() {
        SharedIndexesSettings settings = new SharedIndexesSettings();
        assertEquals(SharedIndexesSettings.MODE_MANUAL, settings.getDownloadMode());
        assertEquals("", settings.getCustomServerUrl());
        assertEquals("", settings.getLocalCacheDirectory());
        assertFalse(settings.isDownloadJdkIndexes());
        assertFalse(settings.isDownloadMavenIndexes());

        SharedIndexesSettings clone = settings.clone();
        assertEquals(settings, clone);
        assertEquals(settings.hashCode(), clone.hashCode());

        clone.setDownloadMode(SharedIndexesSettings.MODE_AUTO_DOWNLOAD);
        clone.setDownloadJdkIndexes(true);
        assertNotEquals(settings, clone);
    }

    @Test
    void testSharedIndexesManagerLifecycle() {
        SharedIndexesSettingsManager manager = SharedIndexesSettingsManager.getInstance();
        assertNotNull(manager);

        SharedIndexesSettings original = manager.getSettings().clone();
        try {
            SharedIndexesSettings custom = new SharedIndexesSettings();
            custom.setDownloadMode(SharedIndexesSettings.MODE_CUSTOM_SERVER);
            custom.setCustomServerUrl("https://indexes.company.internal");
            custom.setDownloadJdkIndexes(true);
            custom.setDownloadMavenIndexes(true);

            AtomicBoolean listenerFired = new AtomicBoolean(false);
            Runnable listener = () -> listenerFired.set(true);
            manager.addChangeListener(listener);

            manager.setSettings(custom);
            assertTrue(listenerFired.get());

            SharedIndexesSettings loaded = manager.getSettings();
            assertEquals(SharedIndexesSettings.MODE_CUSTOM_SERVER, loaded.getDownloadMode());
            assertEquals("https://indexes.company.internal", loaded.getCustomServerUrl());
            assertTrue(loaded.isDownloadJdkIndexes());
            assertTrue(loaded.isDownloadMavenIndexes());

            manager.removeChangeListener(listener);
        } finally {
            manager.setSettings(original);
        }
    }

    @Test
    void testSettingsToolsSharedIndexesPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        SharedIndexesSettings original = SharedIndexesSettingsManager.getInstance().getSettings().clone();
        try {
            runOnFx(() -> {
                SettingsToolsSharedIndexesPage page = new SettingsToolsSharedIndexesPage();
                assertFalse(page.isModified());

                page.setCurrentMode(SharedIndexesSettings.MODE_AUTO_DOWNLOAD);
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());
                assertEquals(SharedIndexesSettings.MODE_AUTO_DOWNLOAD, SharedIndexesSettingsManager.getInstance().getSettings().getDownloadMode());

                page.reset();
                assertFalse(page.isModified());
            });
        } finally {
            SharedIndexesSettingsManager.getInstance().setSettings(original);
        }
    }

    // ------------------------------------------------------------- 4. SSH Configurations

    @Test
    void testSshConfigurationsModelDefaultsAndCloning() {
        SshConfigurationsSettings settings = new SshConfigurationsSettings();
        List<SshConfigurationEntry> list = settings.getConfigurations();
        assertNotNull(list);
        assertFalse(list.isEmpty());

        SshConfigurationEntry first = list.get(0);
        assertEquals("localhost", first.getHost());
        assertEquals(22, first.getPort());
        assertEquals(SshConfigurationEntry.AUTH_PASSWORD, first.getAuthType());
        assertTrue(first.isParseConfigFile());
        assertTrue(first.getDisplayName().contains("localhost:22"));

        SshConfigurationsSettings clone = settings.clone();
        assertEquals(settings, clone);
        assertEquals(settings.hashCode(), clone.hashCode());

        SshConfigurationEntry second = new SshConfigurationEntry("remote-server.com", 2222, "dev");
        clone.getConfigurations().add(second);
        assertNotEquals(settings, clone);
    }

    @Test
    void testSshConfigurationsManagerLifecycle() {
        SshConfigurationsSettingsManager manager = SshConfigurationsSettingsManager.getInstance();
        assertNotNull(manager);

        SshConfigurationsSettings original = manager.getSettings().clone();
        try {
            SshConfigurationsSettings custom = new SshConfigurationsSettings();
            SshConfigurationEntry entry = new SshConfigurationEntry("prod-bastion.internal", 22, "admin");
            entry.setAuthType(SshConfigurationEntry.AUTH_KEY_PAIR);
            entry.setPrivateKeyPath("~/.ssh/id_rsa");
            entry.setSendKeepAlive(true);
            entry.setKeepAliveIntervalSeconds(120);
            custom.setConfigurations(new ArrayList<>(List.of(entry)));

            AtomicBoolean listenerFired = new AtomicBoolean(false);
            Runnable listener = () -> listenerFired.set(true);
            manager.addChangeListener(listener);

            manager.setSettings(custom);
            assertTrue(listenerFired.get());

            SshConfigurationsSettings loaded = manager.getSettings();
            assertEquals(1, loaded.getConfigurations().size());
            SshConfigurationEntry loadedEntry = loaded.getConfigurations().get(0);
            assertEquals("prod-bastion.internal", loadedEntry.getHost());
            assertEquals("admin", loadedEntry.getUsername());
            assertEquals(SshConfigurationEntry.AUTH_KEY_PAIR, loadedEntry.getAuthType());
            assertTrue(loadedEntry.isSendKeepAlive());
            assertEquals(120, loadedEntry.getKeepAliveIntervalSeconds());

            manager.removeChangeListener(listener);
        } finally {
            manager.setSettings(original);
        }
    }

    @Test
    void testSettingsToolsSshConfigurationsPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        SshConfigurationsSettings original = SshConfigurationsSettingsManager.getInstance().getSettings().clone();
        try {
            runOnFx(() -> {
                SettingsToolsSshConfigurationsPage page = new SettingsToolsSshConfigurationsPage();
                assertFalse(page.isModified());

                page.getHostField().setText("new-ssh-host.local");
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());
                assertTrue(SshConfigurationsSettingsManager.getInstance().getSettings().getConfigurations()
                        .stream().anyMatch(c -> "new-ssh-host.local".equals(c.getHost())));

                page.reset();
                assertFalse(page.isModified());
            });
        } finally {
            SshConfigurationsSettingsManager.getInstance().setSettings(original);
        }
    }

    // ------------------------------------------------------------- 5. SSH Terminal

    @Test
    void testSshTerminalModelDefaultsAndCloning() {
        SshTerminalSettings settings = new SshTerminalSettings();
        assertEquals(SshTerminalSettings.CONN_SSH_CONFIG, settings.getConnectionMode());
        assertEquals(SshTerminalSettings.DEFAULT_SSH_CONFIG_SELECT, settings.getSshConfiguration());
        assertEquals("UTF-8", settings.getDefaultEncoding());

        SshTerminalSettings clone = settings.clone();
        assertEquals(settings, clone);
        assertEquals(settings.hashCode(), clone.hashCode());

        clone.setConnectionMode(SshTerminalSettings.CONN_CURRENT_VAGRANT);
        clone.setDefaultEncoding("ISO-8859-1");
        assertNotEquals(settings, clone);
    }

    @Test
    void testSshTerminalManagerLifecycle() {
        SshTerminalSettingsManager manager = SshTerminalSettingsManager.getInstance();
        assertNotNull(manager);

        SshTerminalSettings original = manager.getSettings().clone();
        try {
            SshTerminalSettings custom = new SshTerminalSettings();
            custom.setConnectionMode(SshTerminalSettings.CONN_DEFAULT_PYTHON_REMOTE);
            custom.setDefaultEncoding("windows-1252");

            AtomicBoolean listenerFired = new AtomicBoolean(false);
            Runnable listener = () -> listenerFired.set(true);
            manager.addChangeListener(listener);

            manager.setSettings(custom);
            assertTrue(listenerFired.get());

            SshTerminalSettings loaded = manager.getSettings();
            assertEquals(SshTerminalSettings.CONN_DEFAULT_PYTHON_REMOTE, loaded.getConnectionMode());
            assertEquals("windows-1252", loaded.getDefaultEncoding());

            manager.removeChangeListener(listener);
        } finally {
            manager.setSettings(original);
        }
    }

    @Test
    void testSettingsToolsSshTerminalPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        SshTerminalSettings original = SshTerminalSettingsManager.getInstance().getSettings().clone();
        try {
            runOnFx(() -> {
                SettingsToolsSshTerminalPage page = new SettingsToolsSshTerminalPage();
                assertFalse(page.isModified());

                page.getCurrentVagrantRadio().setSelected(true);
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());
                assertEquals(SshTerminalSettings.CONN_CURRENT_VAGRANT, SshTerminalSettingsManager.getInstance().getSettings().getConnectionMode());

                page.reset();
                assertFalse(page.isModified());
            });
        } finally {
            SshTerminalSettingsManager.getInstance().setSettings(original);
        }
    }

    // ------------------------------------------------------------- 6. SettingsDialog Integration

    @Test
    void testSettingsDialogToolsNavigationIntegration() throws Exception {
        if (!javaFxAvailable) return;

        runOnFx(() -> {
            SettingsDialog dialog = new SettingsDialog(null);
            assertNotNull(dialog);

            // 1. Rsync
            dialog.selectCategory("Tools", "Rsync");
            assertNotNull(dialog.getCurrentToolsRsyncPage());

            // 2. RuboCop
            dialog.selectCategory("Tools", "RuboCop");
            assertNotNull(dialog.getCurrentToolsRuboCopPage());

            // 3. Shared Indexes
            dialog.selectCategory("Tools", "Shared Indexes");
            assertNotNull(dialog.getCurrentToolsSharedIndexesPage());

            // 4. SSH Configurations
            dialog.selectCategory("Tools", "SSH Configurations");
            assertNotNull(dialog.getCurrentToolsSshConfigurationsPage());

            // 5. SSH Terminal
            dialog.selectCategory("Tools", "SSH Terminal");
            assertNotNull(dialog.getCurrentToolsSshTerminalPage());
        });
    }

    // ------------------------------------------------------------- 7. Strict Brand Isolation

    @Test
    void testStrictBrandIsolation() throws Exception {
        String[] filesToCheck = {
                "src/main/java/dev/lumina/tools/RsyncSettings.java",
                "src/main/java/dev/lumina/tools/RsyncSettingsManager.java",
                "src/main/java/dev/lumina/tools/RuboCopSettings.java",
                "src/main/java/dev/lumina/tools/RuboCopSettingsManager.java",
                "src/main/java/dev/lumina/tools/SharedIndexesSettings.java",
                "src/main/java/dev/lumina/tools/SharedIndexesSettingsManager.java",
                "src/main/java/dev/lumina/tools/SshConfigurationEntry.java",
                "src/main/java/dev/lumina/tools/SshConfigurationsSettings.java",
                "src/main/java/dev/lumina/tools/SshConfigurationsSettingsManager.java",
                "src/main/java/dev/lumina/tools/SshTerminalSettings.java",
                "src/main/java/dev/lumina/tools/SshTerminalSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsToolsRsyncPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsRuboCopPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsSharedIndexesPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsSshConfigurationsPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsSshTerminalPage.java"
        };

        String[] forbidden = {
                "intellij",
                "jetbrains",
                "pycharm",
                "webstorm",
                "clion"
        };

        for (String filePath : filesToCheck) {
            String content = Files.readString(Paths.get(filePath));
            for (String f : forbidden) {
                assertFalse(content.toLowerCase().contains(f),
                        "File " + filePath + " must not contain forbidden competitor keyword: " + f);
            }
        }
    }
}
