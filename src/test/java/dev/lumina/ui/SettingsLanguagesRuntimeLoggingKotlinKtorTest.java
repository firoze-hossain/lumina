package dev.lumina.ui;

import dev.lumina.javascript.JavaScriptRuntimeSettings;
import dev.lumina.javascript.JavaScriptSettingsManager;
import dev.lumina.jvm.JvmLoggingSettings;
import dev.lumina.jvm.JvmLoggingSettingsManager;
import dev.lumina.kotlin.KotlinScriptDefinition;
import dev.lumina.kotlin.KotlinScriptingSettings;
import dev.lumina.kotlin.KotlinScriptingSettingsManager;
import dev.lumina.ktor.KtorSettings;
import dev.lumina.ktor.KtorSettingsManager;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SettingsLanguagesRuntimeLoggingKotlinKtorTest {

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

    // ============================================================
    // 1. JavaScript Runtime Tests
    // ============================================================

    @Test
    void testJavaScriptRuntimeModelAndDiscovery() {
        JavaScriptRuntimeSettings settings = new JavaScriptRuntimeSettings();
        assertEquals("Node.js Auto-detected", settings.getPreferredRuntime());
        assertEquals("npm", settings.getPackageManager());
        assertEquals("/usr/bin/npm", settings.getPackageManagerPath());
        assertEquals("10.9.9", settings.getPackageManagerVersion());
        assertEquals("node", settings.getNodeRuntime());
        assertEquals("/usr/bin/node", settings.getNodeRuntimePath());
        assertEquals("22.23.3", settings.getNodeRuntimeVersion());
        assertFalse(settings.isCodingAssistanceForNode());

        // Copy and mutation
        JavaScriptRuntimeSettings copy = settings.copy();
        assertEquals(settings, copy);

        copy.setPreferredRuntime("Bun");
        copy.setCodingAssistanceForNode(true);
        assertNotEquals(settings, copy);
        assertTrue(copy.isCodingAssistanceForNode());
        assertEquals("Bun", copy.getPreferredRuntime());

        // Dynamic system discovery checks
        List<JavaScriptRuntimeSettings.RuntimeItem> nodes = JavaScriptRuntimeSettings.detectSystemNodeRuntimes();
        assertNotNull(nodes);
        assertFalse(nodes.isEmpty());

        List<JavaScriptRuntimeSettings.RuntimeItem> packageManagers = JavaScriptRuntimeSettings.detectSystemPackageManagers();
        assertNotNull(packageManagers);
        assertFalse(packageManagers.isEmpty());
    }

    @Test
    void testJavaScriptRuntimeManagerPersistence() {
        JavaScriptSettingsManager manager = JavaScriptSettingsManager.getInstance();
        JavaScriptRuntimeSettings original = manager.getRuntimeSettings();

        JavaScriptRuntimeSettings modified = original.copy();
        modified.setPreferredRuntime("Deno");
        modified.setCodingAssistanceForNode(true);
        manager.setRuntimeSettings(modified);

        assertEquals("Deno", manager.getRuntimeSettings().getPreferredRuntime());
        assertTrue(manager.getRuntimeSettings().isCodingAssistanceForNode());

        // Restore
        manager.setRuntimeSettings(original);
    }

    @Test
    void testJavaScriptRuntimePageUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesJavaScriptRuntimePage page = new SettingsLanguagesJavaScriptRuntimePage();
                assertNotNull(page);
                assertFalse(page.isModified());

                assertEquals("Node.js Auto-detected", page.getPreferredRuntimeCombo().getValue());
                assertNotNull(page.getPackageManagerCombo().getValue());
                assertNotNull(page.getNodeRuntimeCombo().getValue());
                assertFalse(page.getCodingAssistanceCheckBox().isSelected());

                // Modify preferred runtime
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getPreferredRuntimeCombo().setValue("Bun");
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                // Toggle coding assistance
                page.getCodingAssistanceCheckBox().setSelected(true);
                assertTrue(page.isModified());

                // Apply changes
                page.apply();
                assertFalse(page.isModified());
                assertEquals("Bun", JavaScriptSettingsManager.getInstance().getRuntimeSettings().getPreferredRuntime());
                assertTrue(JavaScriptSettingsManager.getInstance().getRuntimeSettings().isCodingAssistanceForNode());

                // Reset / revert
                page.reset();
                assertFalse(page.isModified());

                // Revert manager to defaults
                JavaScriptSettingsManager.getInstance().setRuntimeSettings(new JavaScriptRuntimeSettings());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // ============================================================
    // 2. JVM Logging Tests
    // ============================================================

    @Test
    void testJvmLoggingModelAndManager() {
        JvmLoggingSettings settings = new JvmLoggingSettings();
        assertEquals("log", settings.getVariableName());
        assertEquals("Unspecified", settings.getLogger());

        assertTrue(JvmLoggingSettings.AVAILABLE_LOGGERS.contains("SLF4J"));
        assertTrue(JvmLoggingSettings.AVAILABLE_LOGGERS.contains("Log4j 2"));
        assertTrue(JvmLoggingSettings.AVAILABLE_LOGGERS.contains("Apache Commons Logging"));
        assertTrue(JvmLoggingSettings.AVAILABLE_LOGGERS.contains("JUL (java.util.logging)"));

        JvmLoggingSettings copy = settings.copy();
        assertEquals(settings, copy);

        copy.setVariableName("logger");
        copy.setLogger("SLF4J");
        assertNotEquals(settings, copy);

        JvmLoggingSettingsManager manager = JvmLoggingSettingsManager.getInstance();
        manager.setSettings(copy);
        assertEquals("logger", manager.getSettings().getVariableName());
        assertEquals("SLF4J", manager.getSettings().getLogger());

        manager.resetDefaults();
        assertEquals("log", manager.getSettings().getVariableName());
        assertEquals("Unspecified", manager.getSettings().getLogger());
    }

    @Test
    void testJvmLoggingPageUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesJvmLoggingPage page = new SettingsLanguagesJvmLoggingPage();
                assertNotNull(page);
                assertFalse(page.isModified());

                assertEquals("log", page.getVariableNameField().getText());
                assertEquals("Unspecified", page.getLoggerCombo().getValue());

                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getVariableNameField().setText("customLog");
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.getLoggerCombo().setValue("Log4j 2");
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());
                assertEquals("customLog", JvmLoggingSettingsManager.getInstance().getSettings().getVariableName());
                assertEquals("Log4j 2", JvmLoggingSettingsManager.getInstance().getSettings().getLogger());

                page.reset();
                assertFalse(page.isModified());

                JvmLoggingSettingsManager.getInstance().resetDefaults();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // ============================================================
    // 3. Kotlin Overview Page Tests
    // ============================================================

    @Test
    void testKotlinOverviewPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                AtomicBoolean navigated = new AtomicBoolean(false);
                SettingsLanguagesKotlinPage page = new SettingsLanguagesKotlinPage(() -> navigated.set(true));
                assertNotNull(page);

                assertNotNull(page.getScriptingLink());
                assertEquals("Kotlin Scripting", page.getScriptingLink().getText());

                page.getScriptingLink().fire();
                assertTrue(navigated.get());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // ============================================================
    // 4. Kotlin Scripting Tests
    // ============================================================

    @Test
    void testKotlinScriptingModelAndManager() {
        KotlinScriptingSettings settings = new KotlinScriptingSettings();
        List<KotlinScriptDefinition> defs = settings.getDefinitions();
        assertEquals(4, defs.size());

        assertEquals("MainKtsScript", defs.get(0).getName());
        assertEquals(".main.kts", defs.get(0).getPattern());
        assertTrue(defs.get(0).isEnabled());
        assertFalse(defs.get(0).isLocked());

        assertEquals("Kotlin Notebooks", defs.get(1).getName());
        assertEquals(".jupyter.kts", defs.get(1).getPattern());

        assertEquals("Qodana .inspection.kts", defs.get(2).getName());
        assertEquals(".inspection.kts", defs.get(2).getPattern());

        assertEquals("Default Kotlin Script", defs.get(3).getName());
        assertEquals(".kts", defs.get(3).getPattern());
        assertTrue(defs.get(3).isLocked());

        // Locked item cannot be disabled
        defs.get(3).setEnabled(false);
        assertTrue(defs.get(3).isEnabled());

        KotlinScriptingSettingsManager manager = KotlinScriptingSettingsManager.getInstance();
        assertNotNull(manager.getSettings());
        assertEquals(4, manager.scanClasspath());
    }

    @Test
    void testKotlinScriptingPageUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesKotlinScriptingPage page = new SettingsLanguagesKotlinScriptingPage();
                assertNotNull(page);
                assertFalse(page.isModified());

                assertEquals(4, page.getTableItems().size());
                assertEquals(3, page.getTableView().getColumns().size());

                // Select first row (index 0)
                page.getTableView().getSelectionModel().select(0);
                // Move Up disabled on top row
                assertTrue(page.getMoveUpButton().isDisable());
                assertFalse(page.getMoveDownButton().isDisable());

                // Move down
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getMoveDownButton().fire();
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());
                assertEquals("Kotlin Notebooks", page.getTableItems().get(0).getName());
                assertEquals("MainKtsScript", page.getTableItems().get(1).getName());

                // Scan Classpath button
                page.getScanClasspathButton().fire();
                assertTrue(page.getScanStatusLabel().isVisible());

                // Apply
                page.apply();
                assertFalse(page.isModified());

                // Reset to defaults
                KotlinScriptingSettingsManager.getInstance().resetDefaults();
                page.reset();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // ============================================================
    // 5. Ktor Tests
    // ============================================================

    @Test
    void testKtorModelAndManager() {
        KtorSettings settings = new KtorSettings();
        assertFalse(settings.isCreateRunConfigurationAutomatically());

        KtorSettings copy = settings.copy();
        assertEquals(settings, copy);

        copy.setCreateRunConfigurationAutomatically(true);
        assertNotEquals(settings, copy);
        assertTrue(copy.isCreateRunConfigurationAutomatically());

        KtorSettingsManager manager = KtorSettingsManager.getInstance();
        manager.setSettings(copy);
        assertTrue(manager.getSettings().isCreateRunConfigurationAutomatically());

        manager.resetDefaults();
        assertFalse(manager.getSettings().isCreateRunConfigurationAutomatically());
    }

    @Test
    void testKtorPageUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesKtorPage page = new SettingsLanguagesKtorPage();
                assertNotNull(page);
                assertFalse(page.isModified());

                assertFalse(page.getCreateRunConfigurationCheckBox().isSelected());

                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getCreateRunConfigurationCheckBox().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.apply();
                assertFalse(page.isModified());
                assertTrue(KtorSettingsManager.getInstance().getSettings().isCreateRunConfigurationAutomatically());

                page.reset();
                assertFalse(page.isModified());

                KtorSettingsManager.getInstance().resetDefaults();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // ============================================================
    // 6. SettingsDialog Integration & Navigation Tests
    // ============================================================

    @Test
    void testSettingsDialogTreeAndNavigationForNewPages() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDialog dialog = new SettingsDialog(null);
                assertNotNull(dialog);

                TreeItem<String> languagesNode = null;
                for (TreeItem<String> child : dialog.getTree().getRoot().getChildren()) {
                    if ("Languages & Frameworks".equals(child.getValue())) {
                        languagesNode = child;
                        break;
                    }
                }
                assertNotNull(languagesNode);

                // 1. JavaScript Runtime
                TreeItem<String> jsRuntimeItem = findChild(languagesNode, "JavaScript Runtime");
                assertNotNull(jsRuntimeItem);
                dialog.getTree().getSelectionModel().select(jsRuntimeItem);
                assertNotNull(dialog.getCurrentLanguagesJavaScriptRuntimePage());

                // 2. JVM Logging
                TreeItem<String> jvmLoggingItem = findChild(languagesNode, "JVM Logging");
                assertNotNull(jvmLoggingItem);
                dialog.getTree().getSelectionModel().select(jvmLoggingItem);
                assertNotNull(dialog.getCurrentLanguagesJvmLoggingPage());

                // 3. Kotlin & Kotlin Scripting
                TreeItem<String> kotlinItem = findChild(languagesNode, "Kotlin");
                assertNotNull(kotlinItem);
                assertEquals(1, kotlinItem.getChildren().size());
                assertEquals("Kotlin Scripting", kotlinItem.getChildren().get(0).getValue());

                dialog.getTree().getSelectionModel().select(kotlinItem);
                assertNotNull(dialog.getCurrentLanguagesKotlinPage());

                // Click link on Kotlin page to navigate to Kotlin Scripting
                dialog.getCurrentLanguagesKotlinPage().getScriptingLink().fire();
                TreeItem<String> selectedItem = dialog.getTree().getSelectionModel().getSelectedItem();
                assertNotNull(selectedItem);
                assertEquals("Kotlin Scripting", selectedItem.getValue());
                assertNotNull(dialog.getCurrentLanguagesKotlinScriptingPage());

                // 4. Ktor
                TreeItem<String> ktorItem = findChild(languagesNode, "Ktor");
                assertNotNull(ktorItem);
                dialog.getTree().getSelectionModel().select(ktorItem);
                assertNotNull(dialog.getCurrentLanguagesKtorPage());

            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    private TreeItem<String> findChild(TreeItem<String> parent, String value) {
        for (TreeItem<String> child : parent.getChildren()) {
            if (value.equals(child.getValue())) {
                return child;
            }
        }
        return null;
    }
}
