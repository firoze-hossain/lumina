package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class SettingsColorSchemeSassAndScalaTest {

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
        EditorColorSchemeSettings.getInstance().initDefaults();
    }

    // -------------------------------------------------------------------------
    // 1. Sass/SCSS Page Tests (media_1790480043607.png & media_1790480043758.png)
    // -------------------------------------------------------------------------

    @Test
    void testSassPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeSassPage page = new SettingsColorSchemeSassPage();

                // 35 flat items in tree
                assertEquals(35, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Colon (media_1790480043607.png)
                assertEquals("Colon", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Colon", page.getSelectedKey());

                // Foreground: checked, BCBEC4
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("BCBEC4", page.getForegroundSwatch().getText());

                // Background: checked, 1E1F22
                assertTrue(page.getBackgroundCheck().isSelected());
                assertEquals("1E1F22", page.getBackgroundSwatch().getText());

                // Inherit values from checked: Colon (CSS)
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Colon", page.getInheritTargetLabel().getText());
                assertEquals("(CSS)", page.getInheritScopeLabel().getText());

                // Select Number (media_1790480043758.png)
                page.selectTreeItem("Number");
                assertEquals("Number", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("2AACB8", page.getForegroundSwatch().getText());
                assertFalse(page.getBackgroundCheck().isSelected());
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Number", page.getInheritTargetLabel().getText());
                assertEquals("(CSS)", page.getInheritScopeLabel().getText());

                // Preview has code lines
                assertTrue(page.getCodeLinesBox().getChildren().size() >= 10);

                // Modification and Apply
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getItalicCheck().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.apply();
                assertFalse(page.isModified());

                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Number").isItalic());

                // Reset
                page.getItalicCheck().setSelected(false);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (error.get() != null) {
            if (error.get() instanceof AssertionError ae) throw ae;
            if (error.get() instanceof Exception ex) throw ex;
            throw new RuntimeException(error.get());
        }
    }

    // -------------------------------------------------------------------------
    // 2. Scala Page Tests (media_1790480043317.png, 1790480043848, 1790480044478)
    // -------------------------------------------------------------------------

    @Test
    void testScalaPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeScalaPage page = new SettingsColorSchemeScalaPage();

                // 68 flat items in tree
                assertEquals(68, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Local value (media_1790480043317.png)
                assertEquals("Local value", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Local value", page.getSelectedKey());

                // Foreground: checked, BCBEC4
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("BCBEC4", page.getForegroundSwatch().getText());

                // Background: unchecked
                assertFalse(page.getBackgroundCheck().isSelected());

                // Inherit values from checked: Variables->Local variable (Java)
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Variables->Local variable", page.getInheritTargetLabel().getText());
                assertEquals("(Java)", page.getInheritScopeLabel().getText());

                // Select Class (media_1790480043848.png)
                page.selectTreeItem("Class");
                assertEquals("Class", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("BCBEC4", page.getForegroundSwatch().getText());
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Classes and Interfaces->Class", page.getInheritTargetLabel().getText());
                assertEquals("(Java)", page.getInheritScopeLabel().getText());

                // Select ScalaDoc wiki syntax elements (media_1790480044478.png)
                page.selectTreeItem("ScalaDoc wiki syntax elements");
                assertEquals("ScalaDoc wiki syntax elements", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("68A67E", page.getForegroundSwatch().getText());
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Comments->Doc comment->Markup", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

                // Preview has code lines
                assertTrue(page.getCodeLinesBox().getChildren().size() >= 12);

                // Modification and Apply
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getBoldCheck().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.apply();
                assertFalse(page.isModified());

                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "ScalaDoc wiki syntax elements").isBold());

                // Reset
                page.getBoldCheck().setSelected(false);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (error.get() != null) {
            if (error.get() instanceof AssertionError ae) throw ae;
            if (error.get() instanceof Exception ex) throw ex;
            throw new RuntimeException(error.get());
        }
    }

    // -------------------------------------------------------------------------
    // 3. SettingsDialog Integration Test
    // -------------------------------------------------------------------------

    @Test
    void testSettingsDialogIntegration() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                SettingsDialog dialog = new SettingsDialog(stage, "Appearance");

                // Locate Color Scheme node under Editor
                TreeItem<String> csNode = null;
                for (TreeItem<String> child : dialog.getTree().getRoot().getChildren()) {
                    if ("Editor".equals(child.getValue())) {
                        for (TreeItem<String> edChild : child.getChildren()) {
                            if ("Color Scheme".equals(edChild.getValue())) {
                                csNode = edChild;
                                break;
                            }
                        }
                    }
                }
                assertNotNull(csNode, "Color Scheme tree node must exist under Editor");

                // Navigate to Sass/SCSS
                TreeItem<String> sassNode = null;
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("Sass/SCSS".equals(child.getValue())) {
                        sassNode = child;
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                assertNotNull(sassNode, "Sass/SCSS node must exist in Color Scheme");
                assertNotNull(dialog.getCurrentColorSchemeSassPage(), "Sass/SCSS page must be loaded");

                // Navigate to Scala
                TreeItem<String> scalaNode = null;
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("Scala".equals(child.getValue())) {
                        scalaNode = child;
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                assertNotNull(scalaNode, "Scala node must exist in Color Scheme");
                assertNotNull(dialog.getCurrentColorSchemeScalaPage(), "Scala page must be loaded");

            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (error.get() != null) {
            if (error.get() instanceof AssertionError ae) throw ae;
            if (error.get() instanceof Exception ex) throw ex;
            throw new RuntimeException(error.get());
        }
    }

    // -------------------------------------------------------------------------
    // 4. Brand Isolation Test
    // -------------------------------------------------------------------------

    @Test
    void testBrandIsolation() throws Exception {
        Pattern competitorPattern = Pattern.compile("(?i)\\b(intellij|jetbrains|idea)\\b");

        List<String> filesToCheck = List.of(
                "src/main/java/dev/lumina/ui/SettingsColorSchemeSassPage.java",
                "src/main/java/dev/lumina/ui/SettingsColorSchemeScalaPage.java"
        );

        for (String relPath : filesToCheck) {
            Path path = Path.of(relPath);
            assertTrue(Files.exists(path), "File must exist: " + relPath);
            String content = Files.readString(path);
            var matcher = competitorPattern.matcher(content);
            assertFalse(matcher.find(), "Competitor brand found in file " + relPath + ": " + (matcher.hitEnd() ? "" : matcher.group()));
        }
    }
}
