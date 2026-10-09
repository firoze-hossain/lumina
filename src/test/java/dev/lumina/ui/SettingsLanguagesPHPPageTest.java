package dev.lumina.ui;

import dev.lumina.php.PhpComposerFileConfig;
import dev.lumina.php.PhpLanguageLevel;
import dev.lumina.php.PhpSettingsManager;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SettingsLanguagesPHPPageTest {

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
    void testPageInitializationAndInitialState() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesPHPPage page = new SettingsLanguagesPHPPage();
                assertNotNull(page);

                // Top controls initial values
                assertEquals("5.6 (variadic functions, argument unpacking)", page.getLanguageLevelCombo().getValue());
                assertNotNull(page.getInterpreterCombo().getValue());

                // Tabs initial states
                assertTrue(page.getCurrentIncludePaths().isEmpty());
                assertFalse(page.getCurrentExtensionStates().isEmpty());
                assertEquals("1", page.getCallTreeDepthCombo().getValue());
                assertTrue(page.getSkipCallsWithConstantParamsCheck().isSelected());
                assertEquals(3, page.getCurrentUncheckedExceptions().size());
                assertEquals("$_SERVER['DOCUMENT_ROOT']", page.getDocumentRootField().getText());
                assertTrue(page.getCurrentComposerFiles().isEmpty());

                // Not modified initially
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testDirtyTrackingAndApply() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesPHPPage page = new SettingsLanguagesPHPPage();
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                assertFalse(page.isModified());

                // Change language level
                page.getLanguageLevelCombo().setValue(PhpLanguageLevel.PHP_8_3.getDisplayName());
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                // Apply
                page.apply();
                assertFalse(page.isModified());
                assertEquals(PhpLanguageLevel.PHP_8_3, PhpSettingsManager.getInstance().getLanguageLevel());

                // Reset back to initial
                page.getLanguageLevelCombo().setValue(PhpLanguageLevel.PHP_5_6.getDisplayName());
                page.apply();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testRevertChanges() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesPHPPage page = new SettingsLanguagesPHPPage();
                assertFalse(page.isModified());

                page.getLanguageLevelCombo().setValue(PhpLanguageLevel.PHP_8_0.getDisplayName());
                page.getDocumentRootField().setText("/custom/doc/root");
                assertTrue(page.isModified());

                page.revertChanges();
                assertFalse(page.isModified());
                assertEquals("5.6 (variadic functions, argument unpacking)", page.getLanguageLevelCombo().getValue());
                assertEquals("$_SERVER['DOCUMENT_ROOT']", page.getDocumentRootField().getText());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testTabSwitching() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesPHPPage page = new SettingsLanguagesPHPPage();
                page.selectTabForTest(0); // Include Path
                page.selectTabForTest(1); // PHP Runtime
                page.selectTabForTest(2); // Analysis
                page.selectTabForTest(3); // Composer Files
                page.selectTabForTest(0);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSettingsDialogNavigatesToPhpPageUnderLanguagesAndFrameworks() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDialog dialog = new SettingsDialog((javafx.stage.Stage) null);
                dialog.selectCategory("Languages & Frameworks", "PHP");

                SettingsLanguagesPHPPage page = dialog.getCurrentLanguagesPhpPage();
                assertNotNull(page, "getCurrentLanguagesPhpPage should not be null when PHP is selected under Languages & Frameworks");
                assertNotNull(page.getLanguageLevelCombo(), "PHP language level combo should be present");
                assertNotNull(page.getInterpreterCombo(), "CLI Interpreter combo should be present");
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}

