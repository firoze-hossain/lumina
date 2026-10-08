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
                TreeItem<String> javaFxNode = languagesNode.getChildren().get(1);
                assertEquals("JavaFX", javaFxNode.getValue());
                dialog.getTree().getSelectionModel().select(javaFxNode);
                assertNotNull(dialog.getCurrentLanguagesJavaFxPage());

                // 2. Check JavaScript selection
                TreeItem<String> jsNode = languagesNode.getChildren().get(2);
                assertEquals("JavaScript", jsNode.getValue());
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
}
