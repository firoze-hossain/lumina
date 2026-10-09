package dev.lumina.ui;

import dev.lumina.php.PhpComposerSettings;
import dev.lumina.php.PhpDebugSettings;
import dev.lumina.php.PhpQualityToolsSettings;
import dev.lumina.php.PhpQualityToolsSettings.CustomRuleset;
import dev.lumina.php.PhpServer;
import dev.lumina.php.PhpSettingsManager;
import dev.lumina.php.PhpTestFrameworkConfig;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SettingsLanguagesPhpAdvancedPagesTest {

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
    void testStepFiltersPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesPhpDebugStepFiltersPage page = new SettingsLanguagesPhpDebugStepFiltersPage();
                assertFalse(page.isModified());

                // Modify
                PhpDebugSettings ds = PhpSettingsManager.getInstance().getDebugSettings();
                ds.setSkipMagicMethods(true);
                ds.setSkipConstructors(true);
                ds.setSkippedMethods(List.of("MyClass::testMethod"));
                ds.setSkippedFiles(List.of("/path/to/skipped.php"));
                PhpSettingsManager.getInstance().setDebugSettings(ds);

                page.loadFromManager();
                assertFalse(page.isModified());

                page.apply();
                PhpDebugSettings saved = PhpSettingsManager.getInstance().getDebugSettings();
                assertTrue(saved.isSkipMagicMethods());
                assertTrue(saved.isSkipConstructors());
                assertEquals(1, saved.getSkippedMethods().size());
                assertEquals(1, saved.getSkippedFiles().size());

                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testXdebugCloudPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesPhpDebugXdebugCloudPage page = new SettingsLanguagesPhpDebugXdebugCloudPage();
                assertFalse(page.isModified());

                // Modify
                PhpDebugSettings ds = PhpSettingsManager.getInstance().getDebugSettings();
                ds.setConnectToXdebugCloud(true);
                ds.setXdebugCloudId("TEST-CLOUD-123");
                PhpSettingsManager.getInstance().setDebugSettings(ds);

                page.loadFromManager();
                assertFalse(page.isModified());

                page.apply();
                PhpDebugSettings saved = PhpSettingsManager.getInstance().getDebugSettings();
                assertTrue(saved.isConnectToXdebugCloud());
                assertEquals("TEST-CLOUD-123", saved.getXdebugCloudId());

                page.revertChanges();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testServersPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesPhpServersPage page = new SettingsLanguagesPhpServersPage();
                assertFalse(page.isModified());

                List<PhpServer> servers = PhpSettingsManager.getInstance().getServers();
                assertFalse(servers.isEmpty());
                assertEquals("Unnamed", servers.get(0).getName());
                assertEquals(80, servers.get(0).getPort());
                assertEquals("Xdebug", servers.get(0).getDebugger());

                // Add server
                PhpServer s2 = new PhpServer("Web Server", "127.0.0.1", 8080, "Zend Debugger");
                PhpSettingsManager.getInstance().addOrUpdateServer(s2);
                page.loadFromManager();
                assertFalse(page.isModified());

                page.apply();
                List<PhpServer> saved = PhpSettingsManager.getInstance().getServers();
                assertEquals(2, saved.size());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testComposerPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesPhpComposerPage page = new SettingsLanguagesPhpComposerPage();
                assertFalse(page.isModified());

                PhpComposerSettings cs = PhpSettingsManager.getInstance().getComposerSettings();
                assertTrue(cs.isAddPackagesAsLibraries());
                assertTrue(cs.isSynchronizeIdeSettings());
                assertTrue(cs.isCheckForAvailablePackageUpdates());
                assertTrue(cs.isShowComposerJsonTopPanel());
                assertTrue(cs.isNotifyAboutMissingVendor());
                assertFalse(cs.isRunWithIgnorePlatformReqs());
                assertEquals("executable", cs.getExecutionMode());

                // Modify and apply
                cs.setPathToComposerJson("/project/composer.json");
                cs.setRunWithIgnorePlatformReqs(true);
                PhpSettingsManager.getInstance().setComposerSettings(cs);

                page.loadFromManager();
                assertFalse(page.isModified());
                page.apply();

                PhpComposerSettings saved = PhpSettingsManager.getInstance().getComposerSettings();
                assertEquals("/project/composer.json", saved.getPathToComposerJson());
                assertTrue(saved.isRunWithIgnorePlatformReqs());

                page.revertChanges();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testTestFrameworksPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesPhpTestFrameworksPage page = new SettingsLanguagesPhpTestFrameworksPage();
                assertFalse(page.isModified());

                List<PhpTestFrameworkConfig> configs = PhpSettingsManager.getInstance().getTestFrameworkConfigs();
                assertFalse(configs.isEmpty());
                assertTrue(configs.get(0).getName().startsWith("Main Local PHP"));
                assertEquals("tests", configs.get(0).getTestRootsDirectory());

                // Modify and apply
                PhpTestFrameworkConfig cfg = configs.get(0);
                cfg.setPathToScript("/project/vendor/autoload.php");
                cfg.setUseDefaultConfigFile(true);
                cfg.setDefaultConfigFilePath("/project/phpunit.xml");
                PhpSettingsManager.getInstance().addOrUpdateTestFrameworkConfig(cfg);

                page.loadFromManager();
                assertFalse(page.isModified());
                page.apply();

                List<PhpTestFrameworkConfig> saved = PhpSettingsManager.getInstance().getTestFrameworkConfigs();
                assertEquals("/project/vendor/autoload.php", saved.get(0).getPathToScript());
                assertTrue(saved.get(0).isUseDefaultConfigFile());
                assertEquals("/project/phpunit.xml", saved.get(0).getDefaultConfigFilePath());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSettingsDialogNavigationToAllPages() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDialog dialog = new SettingsDialog((javafx.stage.Stage) null);
                dialog.selectCategory("Debug", "Step Filters");
                assertNotNull(dialog.getCurrentLanguagesPhpDebugStepFiltersPage());

                dialog.selectCategory("Debug", "Xdebug Cloud");
                assertNotNull(dialog.getCurrentLanguagesPhpDebugXdebugCloudPage());

                dialog.selectCategory("PHP", "Servers");
                assertNotNull(dialog.getCurrentLanguagesPhpServersPage());

                dialog.selectCategory("PHP", "Composer");
                assertNotNull(dialog.getCurrentLanguagesPhpComposerPage());

                dialog.selectCategory("PHP", "Test Frameworks");
                assertNotNull(dialog.getCurrentLanguagesPhpTestFrameworksPage());

                dialog.selectCategory("PHP", "Quality Tools");
                assertNotNull(dialog.getCurrentPhpQualityToolsPage());

                dialog.selectCategory("Quality Tools", "PHP_CodeSniffer");
                assertNotNull(dialog.getCurrentPhpCodeSnifferPage());

                dialog.selectCategory("Quality Tools", "PHP CS Fixer");
                assertNotNull(dialog.getCurrentPhpCsFixerPage());

                dialog.selectCategory("Quality Tools", "Laravel Pint");
                assertNotNull(dialog.getCurrentPhpLaravelPintPage());

                dialog.selectCategory("Quality Tools", "Mess Detector");
                assertNotNull(dialog.getCurrentPhpMessDetectorPage());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testQualityToolsParentPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesPhpQualityToolsPage page = new SettingsLanguagesPhpQualityToolsPage();
                assertFalse(page.isModified());

                PhpQualityToolsSettings qs = PhpSettingsManager.getInstance().getQualityToolsSettings();
                assertEquals("none", qs.getExternalFormatter());

                qs.setExternalFormatter("laravel_pint");
                PhpSettingsManager.getInstance().setQualityToolsSettings(qs);

                page.loadFromManager();
                assertFalse(page.isModified());

                page.apply();
                assertEquals("laravel_pint", PhpSettingsManager.getInstance().getQualityToolsSettings().getExternalFormatter());

                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testCodeSnifferPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesPhpQualityToolsCodeSnifferPage page = new SettingsLanguagesPhpQualityToolsCodeSnifferPage();
                assertFalse(page.isModified());

                PhpQualityToolsSettings qs = PhpSettingsManager.getInstance().getQualityToolsSettings();
                qs.getCodeSniffer().setInspectionEnabled(true);
                qs.getCodeSniffer().setPhpcsPath("/usr/local/bin/phpcs");
                qs.getCodeSniffer().setCodingStandard("PSR12");
                PhpSettingsManager.getInstance().setQualityToolsSettings(qs);

                page.loadFromManager();
                assertFalse(page.isModified());

                page.apply();
                PhpQualityToolsSettings saved = PhpSettingsManager.getInstance().getQualityToolsSettings();
                assertTrue(saved.getCodeSniffer().isInspectionEnabled());
                assertEquals("/usr/local/bin/phpcs", saved.getCodeSniffer().getPhpcsPath());
                assertEquals("PSR12", saved.getCodeSniffer().getCodingStandard());

                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testCsFixerPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesPhpQualityToolsCsFixerPage page = new SettingsLanguagesPhpQualityToolsCsFixerPage();
                assertFalse(page.isModified());

                PhpQualityToolsSettings qs = PhpSettingsManager.getInstance().getQualityToolsSettings();
                qs.getCsFixer().setInspectionEnabled(true);
                qs.getCsFixer().setPhpCsFixerPath("/usr/local/bin/php-cs-fixer");
                qs.getCsFixer().setAllowRiskyRules(true);
                qs.getCsFixer().setRuleset("Symfony");
                PhpSettingsManager.getInstance().setQualityToolsSettings(qs);

                page.loadFromManager();
                assertFalse(page.isModified());

                page.apply();
                PhpQualityToolsSettings saved = PhpSettingsManager.getInstance().getQualityToolsSettings();
                assertTrue(saved.getCsFixer().isInspectionEnabled());
                assertEquals("/usr/local/bin/php-cs-fixer", saved.getCsFixer().getPhpCsFixerPath());
                assertTrue(saved.getCsFixer().isAllowRiskyRules());
                assertEquals("Symfony", saved.getCsFixer().getRuleset());

                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testLaravelPintPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesPhpQualityToolsLaravelPintPage page = new SettingsLanguagesPhpQualityToolsLaravelPintPage();
                assertFalse(page.isModified());

                PhpQualityToolsSettings qs = PhpSettingsManager.getInstance().getQualityToolsSettings();
                qs.getLaravelPint().setInspectionEnabled(true);
                qs.getLaravelPint().setPintPath("/usr/local/bin/pint");
                qs.getLaravelPint().setPathToPintJson("/my/project/pint.json");
                qs.getLaravelPint().setRuleset("psr12");
                PhpSettingsManager.getInstance().setQualityToolsSettings(qs);

                page.loadFromManager();
                assertFalse(page.isModified());

                page.apply();
                PhpQualityToolsSettings saved = PhpSettingsManager.getInstance().getQualityToolsSettings();
                assertTrue(saved.getLaravelPint().isInspectionEnabled());
                assertEquals("/usr/local/bin/pint", saved.getLaravelPint().getPintPath());
                assertEquals("/my/project/pint.json", saved.getLaravelPint().getPathToPintJson());
                assertEquals("psr12", saved.getLaravelPint().getRuleset());

                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testMessDetectorPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesPhpQualityToolsMessDetectorPage page = new SettingsLanguagesPhpQualityToolsMessDetectorPage();
                assertFalse(page.isModified());

                PhpQualityToolsSettings qs = PhpSettingsManager.getInstance().getQualityToolsSettings();
                qs.getMessDetector().setInspectionEnabled(true);
                qs.getMessDetector().setPhpmdPath("/usr/local/bin/phpmd");
                qs.getMessDetector().setCodeSizeRules(true);
                qs.getMessDetector().setDesignRules(true);
                qs.getMessDetector().setCustomRulesets(List.of(new CustomRuleset("Custom 1", "/path/to/ruleset.xml")));
                PhpSettingsManager.getInstance().setQualityToolsSettings(qs);

                page.loadFromManager();
                assertFalse(page.isModified());

                page.apply();
                PhpQualityToolsSettings saved = PhpSettingsManager.getInstance().getQualityToolsSettings();
                assertTrue(saved.getMessDetector().isInspectionEnabled());
                assertEquals("/usr/local/bin/phpmd", saved.getMessDetector().getPhpmdPath());
                assertTrue(saved.getMessDetector().isCodeSizeRules());
                assertTrue(saved.getMessDetector().isDesignRules());
                assertEquals(1, saved.getMessDetector().getCustomRulesets().size());
                assertEquals("Custom 1", saved.getMessDetector().getCustomRulesets().get(0).getName());

                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
