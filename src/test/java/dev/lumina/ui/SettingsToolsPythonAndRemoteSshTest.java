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
 * 1. Python External Documentation
 * 2. Python Integrated Tools
 * 3. Python Plots
 * 4. Qodana
 * 5. Remote SSH External Tools
 */
public class SettingsToolsPythonAndRemoteSshTest {

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

    // ------------------------------------------------------------- 1. Python External Documentation

    @Test
    void testPythonExternalDocumentationModelDefaultsAndCloning() {
        PythonExternalDocumentationSettings settings = new PythonExternalDocumentationSettings();
        List<PythonDocUrlEntry> entries = settings.getEntries();
        assertNotNull(entries);
        assertFalse(entries.isEmpty());

        // Check well-known default modules
        assertTrue(entries.stream().anyMatch(e -> "pyramid".equals(e.getModuleName())));
        assertTrue(entries.stream().anyMatch(e -> "pandas".equals(e.getModuleName())));
        assertTrue(entries.stream().anyMatch(e -> "flask".equals(e.getModuleName())));
        assertTrue(entries.stream().anyMatch(e -> "matplotlib".equals(e.getModuleName())));

        PythonExternalDocumentationSettings clone = settings.clone();
        assertEquals(settings, clone);
        assertEquals(settings.hashCode(), clone.hashCode());

        clone.getEntries().add(new PythonDocUrlEntry("requests", "https://requests.readthedocs.io/en/latest/api/#{element.name}"));
        assertNotEquals(settings, clone);
    }

    @Test
    void testPythonExternalDocumentationManager() {
        PythonExternalDocumentationSettingsManager manager = PythonExternalDocumentationSettingsManager.getInstance();
        assertNotNull(manager);

        PythonExternalDocumentationSettings original = manager.getSettings().clone();
        try {
            PythonExternalDocumentationSettings custom = new PythonExternalDocumentationSettings();
            custom.setEntries(new ArrayList<>(List.of(
                    new PythonDocUrlEntry("scipy", "https://docs.scipy.org/doc/scipy/reference/{element.name}.html")
            )));

            AtomicBoolean listenerFired = new AtomicBoolean(false);
            Runnable listener = () -> listenerFired.set(true);
            manager.addChangeListener(listener);

            manager.setSettings(custom);
            assertTrue(listenerFired.get());

            PythonExternalDocumentationSettings loaded = manager.getSettings();
            assertEquals(1, loaded.getEntries().size());
            assertEquals("scipy", loaded.getEntries().get(0).getModuleName());

            manager.removeChangeListener(listener);
        } finally {
            manager.setSettings(original);
        }
    }

    @Test
    void testSettingsToolsPythonExternalDocumentationPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        PythonExternalDocumentationSettings original = PythonExternalDocumentationSettingsManager.getInstance().getSettings().clone();
        try {
            runOnFx(() -> {
                SettingsToolsPythonExternalDocumentationPage page = new SettingsToolsPythonExternalDocumentationPage();
                assertFalse(page.isModified());

                page.getTableData().add(new PythonDocUrlEntry("numpy", "https://numpy.org/doc/stable/reference/{element.name}"));
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());
                assertTrue(PythonExternalDocumentationSettingsManager.getInstance().getSettings().getEntries()
                        .stream().anyMatch(e -> "numpy".equals(e.getModuleName())));

                page.revertChanges();
                assertFalse(page.isModified());
            });
        } finally {
            PythonExternalDocumentationSettingsManager.getInstance().setSettings(original);
        }
    }

    // ------------------------------------------------------------- 2. Python Integrated Tools

    @Test
    void testPythonIntegratedToolsModelDefaultsAndCloning() {
        PythonIntegratedToolsSettings settings = new PythonIntegratedToolsSettings();
        assertEquals("", settings.getPackageRequirementsFile());
        assertEquals("", settings.getPipenvExecutablePath());
        assertEquals("Autodetect", settings.getDefaultTestRunner());
        assertFalse(settings.isDetectTestsInJupyterNotebooks());
        assertEquals("reStructuredText", settings.getDocstringFormat());
        assertTrue(settings.isAnalyzePythonCodeInDocstrings());
        assertFalse(settings.isRenderExternalDocumentationForStdlib());
        assertEquals("", settings.getSphinxWorkingDirectory());
        assertFalse(settings.isTreatTxtFilesAsReStructuredText());

        PythonIntegratedToolsSettings clone = settings.clone();
        assertEquals(settings, clone);
        assertEquals(settings.hashCode(), clone.hashCode());

        clone.setDefaultTestRunner("pytest");
        clone.setDocstringFormat("Google");
        assertNotEquals(settings, clone);
    }

    @Test
    void testPythonIntegratedToolsManager() {
        PythonIntegratedToolsSettingsManager manager = PythonIntegratedToolsSettingsManager.getInstance();
        assertNotNull(manager);

        PythonIntegratedToolsSettings original = manager.getSettings().clone();
        try {
            PythonIntegratedToolsSettings custom = new PythonIntegratedToolsSettings();
            custom.setDefaultTestRunner("unittest");
            custom.setDocstringFormat("NumPy");
            custom.setDetectTestsInJupyterNotebooks(true);
            custom.setRenderExternalDocumentationForStdlib(true);

            AtomicBoolean listenerFired = new AtomicBoolean(false);
            Runnable listener = () -> listenerFired.set(true);
            manager.addChangeListener(listener);

            manager.setSettings(custom);
            assertTrue(listenerFired.get());

            PythonIntegratedToolsSettings loaded = manager.getSettings();
            assertEquals("unittest", loaded.getDefaultTestRunner());
            assertEquals("NumPy", loaded.getDocstringFormat());
            assertTrue(loaded.isDetectTestsInJupyterNotebooks());
            assertTrue(loaded.isRenderExternalDocumentationForStdlib());

            manager.removeChangeListener(listener);
        } finally {
            manager.setSettings(original);
        }
    }

    @Test
    void testSettingsToolsPythonIntegratedToolsPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        PythonIntegratedToolsSettings original = PythonIntegratedToolsSettingsManager.getInstance().getSettings().clone();
        try {
            runOnFx(() -> {
                SettingsToolsPythonIntegratedToolsPage page = new SettingsToolsPythonIntegratedToolsPage();
                assertFalse(page.isModified());

                page.getTestRunnerCombo().setValue("pytest");
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());
                assertEquals("pytest", PythonIntegratedToolsSettingsManager.getInstance().getSettings().getDefaultTestRunner());

                page.reset();
                assertFalse(page.isModified());
            });
        } finally {
            PythonIntegratedToolsSettingsManager.getInstance().setSettings(original);
        }
    }

    // ------------------------------------------------------------- 3. Python Plots

    @Test
    void testPythonPlotsModelDefaultsAndCloning() {
        PythonPlotsSettings settings = new PythonPlotsSettings();
        assertTrue(settings.isShowPlotsInToolWindow());
        assertTrue(settings.isUseMpld3InteractivePlots());
        assertEquals(200, settings.getMaxPlotsCount());
        assertTrue(settings.isSuggestInstallKaleido());
        assertFalse(settings.isSuggestInstallMpld3());
        assertTrue(settings.isSuggestInstallPillow());

        PythonPlotsSettings clone = settings.clone();
        assertEquals(settings, clone);
        assertEquals(settings.hashCode(), clone.hashCode());

        clone.setShowPlotsInToolWindow(false);
        clone.setMaxPlotsCount(500);
        assertNotEquals(settings, clone);
    }

    @Test
    void testPythonPlotsManager() {
        PythonPlotsSettingsManager manager = PythonPlotsSettingsManager.getInstance();
        assertNotNull(manager);

        PythonPlotsSettings original = manager.getSettings().clone();
        try {
            PythonPlotsSettings custom = new PythonPlotsSettings();
            custom.setShowPlotsInToolWindow(false);
            custom.setMaxPlotsCount(100);
            custom.setSuggestInstallMpld3(true);

            AtomicBoolean listenerFired = new AtomicBoolean(false);
            Runnable listener = () -> listenerFired.set(true);
            manager.addChangeListener(listener);

            manager.setSettings(custom);
            assertTrue(listenerFired.get());

            PythonPlotsSettings loaded = manager.getSettings();
            assertFalse(loaded.isShowPlotsInToolWindow());
            assertEquals(100, loaded.getMaxPlotsCount());
            assertTrue(loaded.isSuggestInstallMpld3());

            manager.removeChangeListener(listener);
        } finally {
            manager.setSettings(original);
        }
    }

    @Test
    void testSettingsToolsPythonPlotsPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        PythonPlotsSettings original = PythonPlotsSettingsManager.getInstance().getSettings().clone();
        try {
            runOnFx(() -> {
                SettingsToolsPythonPlotsPage page = new SettingsToolsPythonPlotsPage();
                assertFalse(page.isModified());

                page.getShowPlotsInToolWindowCheck().setSelected(false);
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());
                assertFalse(PythonPlotsSettingsManager.getInstance().getSettings().isShowPlotsInToolWindow());

                page.reset();
                assertFalse(page.isModified());
            });
        } finally {
            PythonPlotsSettingsManager.getInstance().setSettings(original);
        }
    }

    // ------------------------------------------------------------- 4. Qodana

    @Test
    void testQodanaModelDefaultsAndCloning() {
        QodanaSettings settings = new QodanaSettings();
        assertEquals("qodana.cloud", settings.getQodanaUrl());
        assertFalse(settings.isLoggedIn());
        assertEquals("", settings.getAccountName());

        QodanaSettings clone = settings.clone();
        assertEquals(settings, clone);
        assertEquals(settings.hashCode(), clone.hashCode());

        clone.setQodanaUrl("custom.qodana.local");
        clone.setLoggedIn(true);
        assertNotEquals(settings, clone);
    }

    @Test
    void testQodanaManager() {
        QodanaSettingsManager manager = QodanaSettingsManager.getInstance();
        assertNotNull(manager);

        QodanaSettings original = manager.getSettings().clone();
        try {
            QodanaSettings custom = new QodanaSettings();
            custom.setQodanaUrl("enterprise.qodana.internal");
            custom.setLoggedIn(true);
            custom.setAccountName("Developer");

            AtomicBoolean listenerFired = new AtomicBoolean(false);
            Runnable listener = () -> listenerFired.set(true);
            manager.addChangeListener(listener);

            manager.setSettings(custom);
            assertTrue(listenerFired.get());

            QodanaSettings loaded = manager.getSettings();
            assertEquals("enterprise.qodana.internal", loaded.getQodanaUrl());
            assertTrue(loaded.isLoggedIn());

            manager.removeChangeListener(listener);
        } finally {
            manager.setSettings(original);
        }
    }

    @Test
    void testSettingsToolsQodanaPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        QodanaSettings original = QodanaSettingsManager.getInstance().getSettings().clone();
        try {
            runOnFx(() -> {
                SettingsToolsQodanaPage page = new SettingsToolsQodanaPage();
                assertFalse(page.isModified());

                page.getQodanaUrlField().setText("new.qodana.cloud");
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());
                assertEquals("new.qodana.cloud", QodanaSettingsManager.getInstance().getSettings().getQodanaUrl());

                page.reset();
                assertFalse(page.isModified());
            });
        } finally {
            QodanaSettingsManager.getInstance().setSettings(original);
        }
    }

    // ------------------------------------------------------------- 5. Remote SSH External Tools

    @Test
    void testRemoteSshExternalToolsModelDefaultsAndCloning() {
        RemoteSshExternalToolsSettings settings = new RemoteSshExternalToolsSettings();
        List<RemoteSshExternalToolEntry> tools = settings.getTools();
        assertNotNull(tools);
        // Can be empty or default
        RemoteSshExternalToolsSettings clone = settings.clone();
        assertEquals(settings, clone);

        RemoteSshExternalToolEntry tool = new RemoteSshExternalToolEntry("Remote Build", "Remote Tools", "Build via SSH");
        tool.setProgram("make");
        tool.setArguments("-j4");
        clone.getTools().add(tool);
        assertNotEquals(settings, clone);
    }

    @Test
    void testRemoteSshExternalToolsManager() {
        RemoteSshExternalToolsSettingsManager manager = RemoteSshExternalToolsSettingsManager.getInstance();
        assertNotNull(manager);

        RemoteSshExternalToolsSettings original = manager.getSettings().clone();
        try {
            RemoteSshExternalToolsSettings custom = new RemoteSshExternalToolsSettings();
            RemoteSshExternalToolEntry tool = new RemoteSshExternalToolEntry("Remote Lint", "Remote Tools", "Run linter remotely");
            tool.setProgram("flake8");
            tool.setConnectionType(RemoteSshExternalToolEntry.CONN_CURRENT_VAGRANT);
            custom.setTools(new ArrayList<>(List.of(tool)));

            AtomicBoolean listenerFired = new AtomicBoolean(false);
            Runnable listener = () -> listenerFired.set(true);
            manager.addChangeListener(listener);

            manager.setSettings(custom);
            assertTrue(listenerFired.get());

            RemoteSshExternalToolsSettings loaded = manager.getSettings();
            assertEquals(1, loaded.getTools().size());
            assertEquals("Remote Lint", loaded.getTools().get(0).getName());
            assertEquals("flake8", loaded.getTools().get(0).getProgram());

            manager.removeChangeListener(listener);
        } finally {
            manager.setSettings(original);
        }
    }

    @Test
    void testSettingsToolsRemoteSshExternalToolsPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        RemoteSshExternalToolsSettings original = RemoteSshExternalToolsSettingsManager.getInstance().getSettings().clone();
        try {
            runOnFx(() -> {
                SettingsToolsRemoteSshExternalToolsPage page = new SettingsToolsRemoteSshExternalToolsPage();
                assertFalse(page.isModified());

                RemoteSshExternalToolEntry tool = new RemoteSshExternalToolEntry("SSH Deploy", "Remote Tools", "Deploy script");
                page.getTableData().add(tool);
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());
                assertTrue(RemoteSshExternalToolsSettingsManager.getInstance().getSettings().getTools()
                        .stream().anyMatch(t -> "SSH Deploy".equals(t.getName())));

                page.reset();
                assertFalse(page.isModified());
            });
        } finally {
            RemoteSshExternalToolsSettingsManager.getInstance().setSettings(original);
        }
    }

    // ------------------------------------------------------------- 6. SettingsDialog Integration

    @Test
    void testSettingsDialogToolsNavigationIntegration() throws Exception {
        if (!javaFxAvailable) return;

        runOnFx(() -> {
            SettingsDialog dialog = new SettingsDialog(null);
            assertNotNull(dialog);

            // 1. Python External Documentation
            dialog.selectCategory("Tools", "Python External Documentation");
            assertNotNull(dialog.getCurrentToolsPythonExternalDocumentationPage());

            // 2. Python Integrated Tools
            dialog.selectCategory("Tools", "Python Integrated Tools");
            assertNotNull(dialog.getCurrentToolsPythonIntegratedToolsPage());

            // 3. Python Plots
            dialog.selectCategory("Tools", "Python Plots");
            assertNotNull(dialog.getCurrentToolsPythonPlotsPage());

            // 4. Qodana
            dialog.selectCategory("Tools", "Qodana");
            assertNotNull(dialog.getCurrentToolsQodanaPage());

            // 5. Remote SSH External Tools
            dialog.selectCategory("Tools", "Remote SSH External Tools");
            assertNotNull(dialog.getCurrentToolsRemoteSshExternalToolsPage());
        });
    }

    // ------------------------------------------------------------- 7. Strict Brand Isolation

    @Test
    void testStrictBrandIsolation() throws Exception {
        String[] filesToCheck = {
                "src/main/java/dev/lumina/tools/PythonDocUrlEntry.java",
                "src/main/java/dev/lumina/tools/PythonExternalDocumentationSettings.java",
                "src/main/java/dev/lumina/tools/PythonExternalDocumentationSettingsManager.java",
                "src/main/java/dev/lumina/tools/PythonIntegratedToolsSettings.java",
                "src/main/java/dev/lumina/tools/PythonIntegratedToolsSettingsManager.java",
                "src/main/java/dev/lumina/tools/PythonPlotsSettings.java",
                "src/main/java/dev/lumina/tools/PythonPlotsSettingsManager.java",
                "src/main/java/dev/lumina/tools/QodanaSettings.java",
                "src/main/java/dev/lumina/tools/QodanaSettingsManager.java",
                "src/main/java/dev/lumina/tools/RemoteSshExternalToolEntry.java",
                "src/main/java/dev/lumina/tools/RemoteSshExternalToolsSettings.java",
                "src/main/java/dev/lumina/tools/RemoteSshExternalToolsSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsToolsPythonExternalDocumentationPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsPythonIntegratedToolsPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsPythonPlotsPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsQodanaPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsRemoteSshExternalToolsPage.java"
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
