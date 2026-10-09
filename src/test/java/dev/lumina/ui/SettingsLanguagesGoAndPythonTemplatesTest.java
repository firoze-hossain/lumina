package dev.lumina.ui;

import dev.lumina.go.GoSettings;
import dev.lumina.go.GoSettingsManager;
import dev.lumina.settings.PythonTemplateLanguagesSettings;
import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SettingsLanguagesGoAndPythonTemplatesTest {

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
        GoSettingsManager.getInstance().resetDefaults();
    }

    @Test
    void testPythonTemplateLanguagesSettingsModel() {
        PythonTemplateLanguagesSettings settings = new PythonTemplateLanguagesSettings();
        assertEquals("None", settings.getTemplateLanguage());
        assertEquals(List.of("XHTML", "XML", "HTML"), settings.getTemplateFileTypes());

        settings.setTemplateLanguage("Jinja2");
        assertEquals("Jinja2", settings.getTemplateLanguage());

        settings.addFileType("CustomType");
        assertTrue(settings.getTemplateFileTypes().contains("CustomType"));

        settings.removeFileType("XML");
        assertFalse(settings.getTemplateFileTypes().contains("XML"));

        PythonTemplateLanguagesSettings copy = settings.copy();
        assertEquals("Jinja2", copy.getTemplateLanguage());
        assertEquals(settings.getTemplateFileTypes(), copy.getTemplateFileTypes());
    }

    @Test
    void testGoSettingsModelAndManager() {
        GoSettingsManager manager = GoSettingsManager.getInstance();
        GoSettings s = manager.getSettings();

        // Verify Image 2 defaults
        assertTrue(s.isSuggestParametersNameInCompletion());
        assertTrue(s.isSuggestVariantsRequireAdditionalImports());
        assertFalse(s.isIndentOnEnterInRawStrings());
        assertFalse(s.isShowDocInParameterInfo());
        assertFalse(s.isDetectGoPackagesFromClipboard());
        assertTrue(s.isAskBeforeSharingInGoPlayground());

        assertEquals("Show options", s.getWhenDirectoryRenamed());
        assertEquals("Show options", s.getWhenPackageRenamed());
        assertEquals("Show options", s.getWhenFileRenamed());
        assertEquals("Show options", s.getWhenJsonPasted());
        assertEquals("Show options", s.getWhenTagRenamed());

        // Verify Image 4 defaults
        assertTrue(s.isUseGoPathFromEnv());
        assertFalse(s.isIndexEntireGoPath());

        // Verify Image 5 defaults
        assertTrue(s.isEnableGoModulesIntegration());
        assertTrue(s.isEnableVendoringSupportAutomatically());
        assertEquals("Enable for all projects", s.getDownloadGoModuleDependencies());

        // Test mutating and saving
        s.setIndentOnEnterInRawStrings(true);
        s.setWhenDirectoryRenamed("Rename package");
        s.setEnableVendoringSupportAutomatically(false);
        manager.setSettings(s);

        GoSettings reloaded = manager.getSettings();
        assertTrue(reloaded.isIndentOnEnterInRawStrings());
        assertEquals("Rename package", reloaded.getWhenDirectoryRenamed());
        assertFalse(reloaded.isEnableVendoringSupportAutomatically());
    }

    @Test
    void testSettingsPythonTemplateLanguagesPageUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsPythonTemplateLanguagesPage page = new SettingsPythonTemplateLanguagesPage();
                assertNotNull(page);
                assertFalse(page.isModified());

                assertEquals("None", page.getSelectedLanguage());
                assertTrue(page.getFileTypes().contains("HTML"));

                page.setSelectedLanguage("Django");
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());
                assertEquals("Django", page.getSelectedLanguage());

                page.addFileType("SVG");
                assertTrue(page.isModified());
                assertTrue(page.getFileTypes().contains("SVG"));

                page.revertChanges();
                assertFalse(page.getFileTypes().contains("SVG"));
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSettingsLanguagesGoPageUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesGoPage page = new SettingsLanguagesGoPage();
                assertNotNull(page);
                assertFalse(page.isModified());

                // Find a checkbox inside page and toggle
                page.getChildren().forEach(n -> {
                    if (n instanceof javafx.scene.layout.VBox vb) {
                        vb.getChildren().forEach(inner -> {
                            if (inner instanceof CheckBox cb && cb.getText().contains("Indent on Enter")) {
                                cb.setSelected(true);
                            }
                        });
                    }
                });

                assertTrue(page.isModified());
                page.apply();
                assertFalse(page.isModified());
                assertTrue(GoSettingsManager.getInstance().getSettings().isIndentOnEnterInRawStrings());

                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSettingsLanguagesGoGoRootPageUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesGoGoRootPage page = new SettingsLanguagesGoGoRootPage();
                assertNotNull(page);
                assertFalse(page.isModified());

                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSettingsLanguagesGoGoPathPageUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesGoGoPathPage page = new SettingsLanguagesGoGoPathPage();
                assertNotNull(page);
                assertFalse(page.isModified());
                assertTrue(page.isUseEnvGoPath());
                assertFalse(page.isIndexEntireGoPath());

                page.apply();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSettingsLanguagesGoModulesPageUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesGoModulesPage page = new SettingsLanguagesGoModulesPage();
                assertNotNull(page);
                assertFalse(page.isModified());
                assertTrue(page.isEnableGoModulesIntegration());
                assertTrue(page.isEnableVendoringSupportAutomatically());
                assertEquals("Enable for all projects", page.getDownloadGoModuleDependencies());

                page.apply();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSettingsLanguagesGoBuildTagsPageUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesGoBuildTagsPage page = new SettingsLanguagesGoBuildTagsPage();
                assertNotNull(page);
                assertFalse(page.isModified());

                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSettingsLanguagesGoFormattingFunctionsPageUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesGoFormattingFunctionsPage page = new SettingsLanguagesGoFormattingFunctionsPage();
                assertNotNull(page);
                assertFalse(page.isModified());

                page.addExcludedFunction("fmt.Printf");
                assertTrue(page.isModified());
                assertTrue(page.getExcludedFunctions().contains("fmt.Printf"));

                page.apply();
                assertFalse(page.isModified());

                page.revertChanges();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSettingsLanguagesGoImportsPageUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesGoImportsPage page = new SettingsLanguagesGoImportsPage();
                assertNotNull(page);
                assertFalse(page.isModified());

                assertTrue(page.isShowImportPopup());
                assertTrue(page.isAddUnambiguousImports());
                assertTrue(page.isOptimizeImports());
                assertTrue(page.getExcludedImports().contains("github.com/pkg/errors"));
                assertTrue(page.getExcludedImports().contains("golang.org/x/net/context"));

                page.apply();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSettingsLanguagesJavaFXPageUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesJavaFXPage page = new SettingsLanguagesJavaFXPage();
                assertNotNull(page);
                assertFalse(page.isModified());

                page.getSceneBuilderField().setText("/opt/SceneBuilder/bin/SceneBuilder");
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());

                page.revertChanges();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSettingsLanguagesJavaScriptPageUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesJavaScriptPage page = new SettingsLanguagesJavaScriptPage();
                assertNotNull(page);
                assertFalse(page.isModified());

                assertEquals("ECMAScript 6+", page.getLanguageVersionCombo().getValue());
                assertEquals("ECMAScript 2015+, some proposals and JSX", page.getDescriptionLabel().getText());

                page.getLanguageVersionCombo().setValue("ECMAScript 5.1");
                assertTrue(page.isModified());
                assertEquals("Standard ECMAScript 5.1", page.getDescriptionLabel().getText());

                page.apply();
                assertFalse(page.isModified());

                page.revertChanges();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
