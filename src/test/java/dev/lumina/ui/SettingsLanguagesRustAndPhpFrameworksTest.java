package dev.lumina.ui;

import dev.lumina.php.PhpSettingsManager;
import dev.lumina.php.PhpSmartySettings;
import dev.lumina.rust.RustSettings;
import dev.lumina.rust.RustSettingsManager;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SettingsLanguagesRustAndPhpFrameworksTest {

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
        RustSettingsManager.getInstance().resetDefaults();
    }

    @Test
    void testPhpFrameworksPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesPhpFrameworksPage page = new SettingsLanguagesPhpFrameworksPage();
                assertNotNull(page);
                assertFalse(page.isModified());

                AtomicBoolean navigated = new AtomicBoolean(false);
                page.setOnNavigateToPlugins(() -> navigated.set(true));

                page.apply();
                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testPhpSmartyPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesPhpSmartyPage page = new SettingsLanguagesPhpSmartyPage();
                assertFalse(page.isModified());

                PhpSmartySettings current = PhpSettingsManager.getInstance().getSmartySettings();
                assertEquals("{", current.getLeftDelimiter());
                assertEquals("}", current.getRightDelimiter());
                assertTrue(current.isUseSmarty3WhitespacesPolicy());

                current.setLeftDelimiter("<{");
                current.setRightDelimiter("}>");
                current.setUseSmarty3WhitespacesPolicy(false);
                PhpSettingsManager.getInstance().setSmartySettings(current);

                page.loadFromManager();
                assertFalse(page.isModified());

                page.apply();
                PhpSmartySettings saved = PhpSettingsManager.getInstance().getSmartySettings();
                assertEquals("<{", saved.getLeftDelimiter());
                assertEquals("}>", saved.getRightDelimiter());
                assertFalse(saved.isUseSmarty3WhitespacesPolicy());

                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testRustSettingsPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsRustPage page = new SettingsRustPage();
                assertFalse(page.isModified());

                RustSettings rs = RustSettingsManager.getInstance().getSettings();
                assertNotNull(rs.getToolchainLocation());

                rs.setToolchainLocation("/custom/cargo/bin");
                rs.setStandardLibrary("/custom/rustlib");
                rs.setEnvironmentVariables("RUST_BACKTRACE=1");
                rs.setExpandMacros(false);
                rs.setInjectRustIntoDocComments(false);
                RustSettingsManager.getInstance().setSettings(rs);

                page.loadFromManager();
                assertFalse(page.isModified());

                page.apply();
                RustSettings saved = RustSettingsManager.getInstance().getSettings();
                assertEquals("/custom/cargo/bin", saved.getToolchainLocation());
                assertEquals("/custom/rustlib", saved.getStandardLibrary());
                assertEquals("RUST_BACKTRACE=1", saved.getEnvironmentVariables());
                assertFalse(saved.isExpandMacros());
                assertFalse(saved.isInjectRustIntoDocComments());

                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testRustExternalLintersPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsRustExternalLintersPage page = new SettingsRustExternalLintersPage();
                assertFalse(page.isModified());

                RustSettings rs = RustSettingsManager.getInstance().getSettings();
                assertTrue(rs.getExternalLinters().isRunOnTheFly());
                assertEquals("Cargo Check", rs.getExternalLinters().getExternalTool());

                rs.getExternalLinters().setRunOnTheFly(false);
                rs.getExternalLinters().setExternalTool("Clippy");
                rs.getExternalLinters().setAdditionalArguments("--all-targets");
                rs.getExternalLinters().setChannel("nightly");
                rs.getExternalLinters().setEnvironmentVariables("RUST_LOG=debug");
                RustSettingsManager.getInstance().setSettings(rs);

                page.loadFromManager();
                assertFalse(page.isModified());

                page.apply();
                RustSettings saved = RustSettingsManager.getInstance().getSettings();
                assertFalse(saved.getExternalLinters().isRunOnTheFly());
                assertEquals("Clippy", saved.getExternalLinters().getExternalTool());
                assertEquals("--all-targets", saved.getExternalLinters().getAdditionalArguments());
                assertEquals("nightly", saved.getExternalLinters().getChannel());
                assertEquals("RUST_LOG=debug", saved.getExternalLinters().getEnvironmentVariables());

                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testRustfmtPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsRustfmtPage page = new SettingsRustfmtPage();
                assertFalse(page.isModified());

                RustSettings rs = RustSettingsManager.getInstance().getSettings();
                assertTrue(rs.getRustfmt().isUseRustfmtInsteadOfBuiltIn());

                rs.getRustfmt().setUseRustfmtInsteadOfBuiltIn(false);
                rs.getRustfmt().setAdditionalArguments("--edition 2021");
                rs.getRustfmt().setChannel("stable");
                rs.getRustfmt().setEnvironmentVariables("RUSTFMT_CONFIG=1");
                RustSettingsManager.getInstance().setSettings(rs);

                page.loadFromManager();
                assertFalse(page.isModified());

                page.apply();
                RustSettings saved = RustSettingsManager.getInstance().getSettings();
                assertFalse(saved.getRustfmt().isUseRustfmtInsteadOfBuiltIn());
                assertEquals("--edition 2021", saved.getRustfmt().getAdditionalArguments());
                assertEquals("stable", saved.getRustfmt().getChannel());
                assertEquals("RUSTFMT_CONFIG=1", saved.getRustfmt().getEnvironmentVariables());

                AtomicBoolean nav = new AtomicBoolean(false);
                page.setOnNavigateToActionsOnSave(() -> nav.set(true));

                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testRustSettingsManagerDetection() {
        RustSettingsManager rm = RustSettingsManager.getInstance();
        assertNotNull(rm.getSettings());

        String toolchain = RustSettingsManager.detectToolchainLocation();
        assertNotNull(toolchain);
        assertFalse(toolchain.isBlank(), "Dynamic detection should locate cargo/rust toolchain");

        String version = RustSettingsManager.detectToolchainVersion(toolchain);
        assertNotNull(version);

        String stdlib = RustSettingsManager.detectStandardLibrary(toolchain);
        assertNotNull(stdlib);
    }

    @Test
    void testSettingsDialogNavigationToAllPages() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDialog dialog = new SettingsDialog((javafx.stage.Stage) null);

                // 1. PHP > Frameworks
                dialog.selectCategory("PHP", "Frameworks");
                assertNotNull(dialog.getCurrentPhpFrameworksPage());

                // 2. PHP > Smarty
                dialog.selectCategory("PHP", "Smarty");
                assertNotNull(dialog.getCurrentPhpSmartyPage());

                // 3. Rust parent page
                dialog.selectCategory("Languages & Frameworks", "Rust");
                assertNotNull(dialog.getCurrentRustPage());

                // 4. Rust > External Linters
                dialog.selectCategory("Rust", "External Linters");
                assertNotNull(dialog.getCurrentRustExternalLintersPage());

                // 5. Rust > Rustfmt
                dialog.selectCategory("Rust", "Rustfmt");
                assertNotNull(dialog.getCurrentRustfmtPage());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
