package dev.lumina.ui;

import dev.lumina.javafx.JavaFXSettingsManager;
import dev.lumina.javascript.ESLintSettings;
import dev.lumina.javascript.JavaScriptLanguageVersion;
import dev.lumina.javascript.JavaScriptSettingsManager;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SettingsLanguagesJavaFXAndJavaScriptPagesTest {

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
        JavaFXSettingsManager.getInstance().resetDefaults();
        JavaScriptSettingsManager.getInstance().resetDefaults();
    }

    @Test
    void testJavaFXPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesJavaFXPage page = new SettingsLanguagesJavaFXPage();
                assertNotNull(page);
                assertFalse(page.isModified());

                page.getSceneBuilderField().setText("/custom/scenebuilder");
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());
                assertEquals("/custom/scenebuilder", JavaFXSettingsManager.getInstance().getPathToSceneBuilder());

                page.getSceneBuilderField().setText("/another/path");
                page.revertChanges();
                assertFalse(page.isModified());
                assertEquals("/custom/scenebuilder", page.getSceneBuilderField().getText());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testJavaScriptPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesJavaScriptPage page = new SettingsLanguagesJavaScriptPage();
                assertNotNull(page);
                assertFalse(page.isModified());

                assertEquals("ECMAScript 6+", page.getLanguageVersionCombo().getValue());
                assertEquals("ECMAScript 2015+, some proposals and JSX", page.getDescriptionLabel().getText());

                page.getLanguageVersionCombo().setValue(JavaScriptLanguageVersion.FLOW.getDisplayName());
                assertTrue(page.isModified());
                assertEquals(JavaScriptLanguageVersion.FLOW.getDescription(), page.getDescriptionLabel().getText());

                page.apply();
                assertFalse(page.isModified());
                assertEquals(JavaScriptLanguageVersion.FLOW, JavaScriptSettingsManager.getInstance().getLanguageVersion());

                page.revertChanges();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testESLintPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesJSEsLintPage page = new SettingsLanguagesJSEsLintPage();
                assertNotNull(page);
                assertFalse(page.isModified());

                assertTrue(page.getDisableRadio().isSelected());
                assertEquals("{**/*,*}.{js,ts,jsx,tsx,cjs,cts,mjs,mts,html,vue}", page.getRunForFilesField().getText());
                assertFalse(page.getRunOnSaveCheck().isSelected());

                page.getAutomaticRadio().setSelected(true);
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());
                assertEquals(ESLintSettings.Mode.AUTOMATIC, JavaScriptSettingsManager.getInstance().getEslintSettings().getMode());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testJSHintPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesJSJsHintPage page = new SettingsLanguagesJSJsHintPage();
                assertNotNull(page);
                assertFalse(page.isModified());

                assertFalse(page.getEnableCheck().isSelected());
                assertEquals("2.13.6 (bundled)", page.getVersionCombo().getValue());
                assertTrue(page.getOptionCheckBoxes().get("bitwise").isSelected());
                assertTrue(page.getOptionCheckBoxes().get("curly").isSelected());

                page.getEnableCheck().setSelected(true);
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());
                assertTrue(JavaScriptSettingsManager.getInstance().getJshintSettings().isEnabled());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testDialogNavigationAndTreeStructure() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDialog dialog = new SettingsDialog(null);

                // Locate Languages & Frameworks
                TreeItem<String> languagesNode = null;
                for (TreeItem<String> child : dialog.getTree().getRoot().getChildren()) {
                    if ("Languages & Frameworks".equals(child.getValue())) {
                        languagesNode = child;
                        break;
                    }
                }
                assertNotNull(languagesNode);

                // 1. Check JavaFX selection
                TreeItem<String> javaFxNode = languagesNode.getChildren().stream()
                        .filter(c -> "JavaFX".equals(c.getValue()))
                        .findFirst().orElse(null);
                assertNotNull(javaFxNode);
                dialog.getTree().getSelectionModel().select(javaFxNode);
                assertNotNull(dialog.getCurrentLanguagesJavaFxPage());

                // 2. Check JavaScript selection
                TreeItem<String> jsNode = languagesNode.getChildren().stream()
                        .filter(c -> "JavaScript".equals(c.getValue()))
                        .findFirst().orElse(null);
                assertNotNull(jsNode);
                dialog.getTree().getSelectionModel().select(jsNode);
                assertNotNull(dialog.getCurrentLanguagesJavaScriptPage());

                // Verify JavaScript child nodes matching screenshots 3, 4, 5
                List<String> jsChildren = jsNode.getChildren().stream().map(TreeItem::getValue).toList();
                assertEquals(List.of("Code Quality Tools", "Libraries", "Prettier", "Styled Components", "Vite", "Webpack"), jsChildren);

                // 3. Check Code Quality Tools selection
                TreeItem<String> cqtNode = jsNode.getChildren().get(0);
                assertEquals("Code Quality Tools", cqtNode.getValue());
                dialog.getTree().getSelectionModel().select(cqtNode);

                // 4. Check ESLint selection
                TreeItem<String> eslintNode = cqtNode.getChildren().get(0);
                assertEquals("ESLint", eslintNode.getValue());
                dialog.getTree().getSelectionModel().select(eslintNode);
                assertNotNull(dialog.getCurrentLanguagesJsEsLintPage());

                // 5. Check JSHint selection
                TreeItem<String> jshintNode = cqtNode.getChildren().get(1);
                assertEquals("JSHint", jshintNode.getValue());
                dialog.getTree().getSelectionModel().select(jshintNode);
                assertNotNull(dialog.getCurrentLanguagesJsJsHintPage());

            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testJSHintOptionAndSettingsModels() {
        List<dev.lumina.javascript.JSHintOption> options = dev.lumina.javascript.JSHintOption.getAllOptions();
        assertNotNull(options);
        assertTrue(options.size() >= 80, "Expected at least 80 options across all categories");

        dev.lumina.javascript.JSHintSettings s = new dev.lumina.javascript.JSHintSettings();
        // Enforcing defaults matching Screenshots
        assertTrue(s.isOptionEnabled("bitwise"));
        assertTrue(s.isOptionEnabled("curly"));
        assertTrue(s.isOptionEnabled("eqeqeq"));
        assertTrue(s.isOptionEnabled("forin"));
        assertTrue(s.isOptionEnabled("noarg"));
        assertTrue(s.isOptionEnabled("noempty"));
        assertTrue(s.isOptionEnabled("nonew"));
        assertTrue(s.isOptionEnabled("undef"));
        assertFalse(s.isOptionEnabled("camelcase"));
        assertFalse(s.isOptionEnabled("es3"));
        assertFalse(s.isOptionEnabled("freeze"));

        // Relaxing defaults
        assertTrue(s.isOptionEnabled("strict"));
        assertFalse(s.isOptionEnabled("plusplus"));
        assertFalse(s.isOptionEnabled("asi"));
        assertFalse(s.isOptionEnabled("boss"));
        assertFalse(s.isOptionEnabled("debug"));

        // Environments defaults
        assertTrue(s.isOptionEnabled("browser"));
        assertFalse(s.isOptionEnabled("node"));
        assertFalse(s.isOptionEnabled("jquery"));

        // Trailing defaults
        assertFalse(s.isOptionEnabled("trailing"));
        assertFalse(s.isOptionEnabled("white"));

        // Parametric defaults
        assertEquals("any", s.getParamOption("esversion"));
        assertEquals("false", s.getParamOption("latedef"));
        assertEquals("50", s.getParamOption("maxerr"));

        // Test mutating and copy
        s.setOptionEnabled("node", true);
        s.setParamOption("maxerr", "100");
        dev.lumina.javascript.JSHintSettings copy = s.copy();
        assertEquals(s, copy);
        assertTrue(copy.isOptionEnabled("node"));
        assertEquals("100", copy.getParamOption("maxerr"));
    }

    @Test
    void testESLintSettingsModel() {
        ESLintSettings s = new ESLintSettings();
        assertEquals(ESLintSettings.Mode.DISABLED, s.getMode());
        assertEquals("{**/*,*}.{js,ts,jsx,tsx,cjs,cts,mjs,mts,html,vue}", s.getRunForFiles());
        assertFalse(s.isRunOnSave());
        assertEquals("Project", s.getNodeInterpreter());
        assertFalse(s.isCustomConfigurationFile());

        s.setMode(ESLintSettings.Mode.MANUAL);
        s.setNodeInterpreter("/usr/bin/node");
        s.setEslintPackage("/path/to/eslint");
        s.setCustomConfigurationFile(true);
        s.setConfigurationFile("/project/.eslintrc.js");
        s.setExtraRulesDirectory("/project/rules");

        ESLintSettings copy = s.copy();
        assertEquals(s, copy);
        assertEquals("/usr/bin/node", copy.getNodeInterpreter());
        assertEquals("/path/to/eslint", copy.getEslintPackage());
        assertTrue(copy.isCustomConfigurationFile());
        assertEquals("/project/.eslintrc.js", copy.getConfigurationFile());
        assertEquals("/project/rules", copy.getExtraRulesDirectory());
    }

    @Test
    void testBrandIsolationInJavaScriptFiles() throws Exception {
        List<String> filesToCheck = List.of(
                "src/main/java/dev/lumina/ui/SettingsLanguagesJSCodeQualityToolsPage.java",
                "src/main/java/dev/lumina/ui/SettingsLanguagesJSEsLintPage.java",
                "src/main/java/dev/lumina/ui/SettingsLanguagesJSJsHintPage.java",
                "src/main/java/dev/lumina/ui/SettingsLanguagesJSLibrariesPage.java",
                "src/main/java/dev/lumina/ui/SettingsLanguagesJSPrettierPage.java",
                "src/main/java/dev/lumina/ui/SettingsLanguagesJSStyledComponentsPage.java",
                "src/main/java/dev/lumina/ui/SettingsLanguagesJSVitePage.java",
                "src/main/java/dev/lumina/ui/SettingsLanguagesJSWebpackPage.java",
                "src/main/java/dev/lumina/javascript/ESLintSettings.java",
                "src/main/java/dev/lumina/javascript/JSHintSettings.java",
                "src/main/java/dev/lumina/javascript/JSHintOption.java",
                "src/main/java/dev/lumina/javascript/JavaScriptLibrary.java",
                "src/main/java/dev/lumina/javascript/PrettierSettings.java",
                "src/main/java/dev/lumina/javascript/StyledComponentsSettings.java",
                "src/main/java/dev/lumina/javascript/ViteSettings.java",
                "src/main/java/dev/lumina/javascript/WebpackSettings.java"
        );

        java.util.regex.Pattern competitorPattern = java.util.regex.Pattern.compile("(?i)\\b(intellij|jetbrains|phpstorm|grazie)\\b");
        for (String file : filesToCheck) {
            String content = java.nio.file.Files.readString(java.nio.file.Path.of(file));
            java.util.regex.Matcher m = competitorPattern.matcher(content);
            assertFalse(m.find(), "File " + file + " must NOT contain competitor mentions: " + (m.find() ? m.group() : ""));
        }
    }

    @Test
    void testJavaScriptLibrariesModelAndPage() throws Exception {
        dev.lumina.javascript.JavaScriptLibrary lib = new dev.lumina.javascript.JavaScriptLibrary("HTML", true, "Predefined");
        assertEquals("HTML", lib.getName());
        assertTrue(lib.isEnabled());
        assertEquals("Predefined", lib.getType());

        dev.lumina.javascript.JavaScriptLibrary copy = lib.copy();
        assertEquals(lib, copy);

        if (!javaFxAvailable) return;
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesJSLibrariesPage page = new SettingsLanguagesJSLibrariesPage();
                assertNotNull(page);
                assertFalse(page.isModified());

                assertEquals(2, page.getLibraryRows().size());
                assertEquals("HTML", page.getLibraryRows().get(0).getName());
                assertTrue(page.getLibraryRows().get(0).isEnabled());
                assertEquals("HTTP Pre-Request and Response Handler", page.getLibraryRows().get(1).getName());
                assertFalse(page.getLibraryRows().get(1).isEnabled());

                // Toggle library enabled
                page.getLibraryRows().get(1).setEnabled(true);
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());
                assertTrue(JavaScriptSettingsManager.getInstance().getLibraries().get(1).isEnabled());

                page.revertChanges();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testPrettierSettingsModelAndPage() throws Exception {
        dev.lumina.javascript.PrettierSettings s = new dev.lumina.javascript.PrettierSettings();
        assertEquals(dev.lumina.javascript.PrettierSettings.Mode.DISABLED, s.getMode());
        assertEquals("**/*.{js,ts,jsx,tsx,cjs,cts,mjs,mts,json,vue,astro}", s.getRunForFiles());
        assertFalse(s.isRunOnSave());
        assertTrue(s.isRunOnPaste());
        assertTrue(s.isPreferPrettierToIdeCodeStyle());

        s.setMode(dev.lumina.javascript.PrettierSettings.Mode.MANUAL);
        s.setPrettierPackage("/custom/prettier");
        dev.lumina.javascript.PrettierSettings copy = s.copy();
        assertEquals(s, copy);
        assertEquals("/custom/prettier", copy.getPrettierPackage());

        if (!javaFxAvailable) return;
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesJSPrettierPage page = new SettingsLanguagesJSPrettierPage();
                assertNotNull(page);
                assertFalse(page.isModified());

                assertTrue(page.getDisableRadio().isSelected());
                assertTrue(page.getRunForFilesField().isDisable());
                assertTrue(page.getRunOnPasteCheck().isSelected());
                assertTrue(page.getPreferPrettierCheck().isSelected());

                page.getAutomaticRadio().setSelected(true);
                assertTrue(page.isModified());
                assertFalse(page.getRunForFilesField().isDisable());

                page.apply();
                assertFalse(page.isModified());
                assertEquals(dev.lumina.javascript.PrettierSettings.Mode.AUTOMATIC, JavaScriptSettingsManager.getInstance().getPrettierSettings().getMode());

                page.revertChanges();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testStyledComponentsSettingsModelAndPage() throws Exception {
        dev.lumina.javascript.StyledComponentsSettings s = new dev.lumina.javascript.StyledComponentsSettings();
        s.addTagPrefix("styled");
        s.addTagPrefix("css");
        assertEquals(List.of("styled", "css"), s.getAdditionalTagPrefixes());

        dev.lumina.javascript.StyledComponentsSettings copy = s.copy();
        assertEquals(s, copy);

        if (!javaFxAvailable) return;
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesJSStyledComponentsPage page = new SettingsLanguagesJSStyledComponentsPage();
                assertNotNull(page);
                assertFalse(page.isModified());

                page.getPrefixes().add("customTag");
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());
                assertTrue(JavaScriptSettingsManager.getInstance().getStyledComponentsSettings().getAdditionalTagPrefixes().contains("customTag"));

                page.getPrefixes().remove("customTag");
                page.apply();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testViteSettingsModelAndPage() throws Exception {
        dev.lumina.javascript.ViteSettings s = new dev.lumina.javascript.ViteSettings();
        assertEquals(dev.lumina.javascript.ViteSettings.Mode.AUTOMATIC, s.getMode());
        assertEquals("", s.getConfigurationFile());

        s.setMode(dev.lumina.javascript.ViteSettings.Mode.MANUAL);
        s.setConfigurationFile("vite.config.ts");
        dev.lumina.javascript.ViteSettings copy = s.copy();
        assertEquals(s, copy);

        if (!javaFxAvailable) return;
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesJSVitePage page = new SettingsLanguagesJSVitePage();
                assertNotNull(page);
                assertFalse(page.isModified());

                assertTrue(page.getAutomaticRadio().isSelected());
                page.getDisabledRadio().setSelected(true);
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());
                assertEquals(dev.lumina.javascript.ViteSettings.Mode.DISABLED, JavaScriptSettingsManager.getInstance().getViteSettings().getMode());

                page.revertChanges();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testWebpackSettingsModelAndPage() throws Exception {
        dev.lumina.javascript.WebpackSettings s = new dev.lumina.javascript.WebpackSettings();
        assertEquals(dev.lumina.javascript.WebpackSettings.Mode.AUTOMATIC, s.getMode());
        assertEquals("", s.getConfigurationFile());

        s.setMode(dev.lumina.javascript.WebpackSettings.Mode.MANUAL);
        s.setConfigurationFile("webpack.config.js");
        dev.lumina.javascript.WebpackSettings copy = s.copy();
        assertEquals(s, copy);

        if (!javaFxAvailable) return;
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLanguagesJSWebpackPage page = new SettingsLanguagesJSWebpackPage();
                assertNotNull(page);
                assertFalse(page.isModified());

                assertTrue(page.getAutomaticRadio().isSelected());
                page.getManualRadio().setSelected(true);
                assertTrue(page.isModified());

                page.getConfigFileField().setText("custom/webpack.config.js");
                page.apply();
                assertFalse(page.isModified());
                assertEquals(dev.lumina.javascript.WebpackSettings.Mode.MANUAL, JavaScriptSettingsManager.getInstance().getWebpackSettings().getMode());
                assertEquals("custom/webpack.config.js", JavaScriptSettingsManager.getInstance().getWebpackSettings().getConfigurationFile());

                page.revertChanges();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testDialogNavigationForNewJSPages() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDialog dialog = new SettingsDialog(null);
                TreeItem<String> languagesNode = null;
                for (TreeItem<String> child : dialog.getTree().getRoot().getChildren()) {
                    if ("Languages & Frameworks".equals(child.getValue())) {
                        languagesNode = child;
                        break;
                    }
                }
                assertNotNull(languagesNode);

                TreeItem<String> jsNode = languagesNode.getChildren().stream()
                        .filter(c -> "JavaScript".equals(c.getValue()))
                        .findFirst().orElse(null);
                assertNotNull(jsNode);

                // Check Libraries selection
                TreeItem<String> libNode = jsNode.getChildren().stream().filter(c -> "Libraries".equals(c.getValue())).findFirst().orElse(null);
                assertNotNull(libNode);
                dialog.getTree().getSelectionModel().select(libNode);
                assertNotNull(dialog.getCurrentLanguagesJsLibrariesPage());

                // Check Prettier selection
                TreeItem<String> prettierNode = jsNode.getChildren().stream().filter(c -> "Prettier".equals(c.getValue())).findFirst().orElse(null);
                assertNotNull(prettierNode);
                dialog.getTree().getSelectionModel().select(prettierNode);
                assertNotNull(dialog.getCurrentLanguagesJsPrettierPage());

                // Check Styled Components selection
                TreeItem<String> scNode = jsNode.getChildren().stream().filter(c -> "Styled Components".equals(c.getValue())).findFirst().orElse(null);
                assertNotNull(scNode);
                dialog.getTree().getSelectionModel().select(scNode);
                assertNotNull(dialog.getCurrentLanguagesJsStyledComponentsPage());

                // Check Vite selection
                TreeItem<String> viteNode = jsNode.getChildren().stream().filter(c -> "Vite".equals(c.getValue())).findFirst().orElse(null);
                assertNotNull(viteNode);
                dialog.getTree().getSelectionModel().select(viteNode);
                assertNotNull(dialog.getCurrentLanguagesJsVitePage());

                // Check Webpack selection
                TreeItem<String> webpackNode = jsNode.getChildren().stream().filter(c -> "Webpack".equals(c.getValue())).findFirst().orElse(null);
                assertNotNull(webpackNode);
                dialog.getTree().getSelectionModel().select(webpackNode);
                assertNotNull(dialog.getCurrentLanguagesJsWebpackPage());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}


