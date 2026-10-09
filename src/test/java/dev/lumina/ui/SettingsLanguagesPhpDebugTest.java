package dev.lumina.ui;

import dev.lumina.php.PhpDebugSettings;
import dev.lumina.php.PhpSettingsManager;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SettingsLanguagesPhpDebugTest {

    private static volatile boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                javaFxAvailable = true;
                latch.countDown();
            });
            latch.await(3, TimeUnit.SECONDS);
        } catch (IllegalStateException alreadyStarted) {
            try {
                Platform.setImplicitExit(false);
                CountDownLatch checkLatch = new CountDownLatch(1);
                Platform.runLater(() -> {
                    javaFxAvailable = true;
                    checkLatch.countDown();
                });
                checkLatch.await(1, TimeUnit.SECONDS);
            } catch (Throwable t) {
                javaFxAvailable = false;
            }
        } catch (Throwable ignored) {
            javaFxAvailable = false;
        }
    }

    @BeforeEach
    void setUp() {
        PhpSettingsManager.getInstance().resetDefaults();
    }

    @Test
    void testPhpDebugPageInitializationAndRevert() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesPhpDebugPage page = new SettingsLanguagesPhpDebugPage();
                assertFalse(page.isModified());

                assertEquals("9003,9000", page.getXdebugPortField().getText());
                assertEquals("10137", page.getZendDebugPortField().getText());
                assertFalse(page.getBreakAtFirstLineCheck().isSelected());
                assertNotNull(page.getZendDetectedIdeIpField().getText());

                // Modify
                page.getBreakAtFirstLineCheck().setSelected(true);
                page.getXdebugPortField().setText("9005");
                assertTrue(page.isModified());

                // Revert
                page.revertChanges();
                assertFalse(page.isModified());
                assertFalse(page.getBreakAtFirstLineCheck().isSelected());
                assertEquals("9003,9000", page.getXdebugPortField().getText());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testPhpDebugDbgpProxyPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesPhpDebugDbgpProxyPage page = new SettingsLanguagesPhpDebugDbgpProxyPage();
                assertFalse(page.isModified());

                assertEquals("9001", page.getPortField().getText());
                assertEquals("", page.getIdeKeyField().getText());

                page.getIdeKeyField().setText("LUMINA_KEY");
                page.getHostField().setText("localhost");
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());
                PhpDebugSettings s = PhpSettingsManager.getInstance().getDebugSettings();
                assertEquals("LUMINA_KEY", s.getDbgpIdeKey());
                assertEquals("localhost", s.getDbgpHost());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testPhpDebugSkippedPathsPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesPhpDebugSkippedPathsPage page = new SettingsLanguagesPhpDebugSkippedPathsPage();
                assertFalse(page.isModified());

                assertTrue(page.getNotifySkippedFilesCheck().isSelected());
                assertFalse(page.getPathsList().isEmpty());

                page.getPathsList().add("/var/www/vendor");
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());
                assertTrue(PhpSettingsManager.getInstance().getDebugSettings().getSkippedPaths().contains("/var/www/vendor"));
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSettingsDialogNavigatesToDebugSubpages() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDialog dialog = new SettingsDialog((javafx.stage.Stage) null);

                // 1. Debug main page
                dialog.selectCategory("PHP", "Debug");
                assertNotNull(dialog.getCurrentLanguagesPhpDebugPage(), "Debug page should load");

                // 2. DBGp Proxy
                dialog.selectCategory("Debug", "DBGp Proxy");
                assertNotNull(dialog.getCurrentLanguagesPhpDebugDbgpProxyPage(), "DBGp Proxy page should load");

                // 3. Skipped Paths
                dialog.selectCategory("Debug", "Skipped Paths");
                assertNotNull(dialog.getCurrentLanguagesPhpDebugSkippedPathsPage(), "Skipped Paths page should load");
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
