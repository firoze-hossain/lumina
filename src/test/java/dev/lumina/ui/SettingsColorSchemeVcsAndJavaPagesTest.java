package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import dev.lumina.settings.EditorColorSchemeSettings.EffectType;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SettingsColorSchemeVcsAndJavaPagesTest {

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
    // 1. VCS Page Tests (media_1790425840060.png)
    // -------------------------------------------------------------------------

    @Test
    void testVcsPageStructureAndDefaults() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeVcsPage page = new SettingsColorSchemeVcsPage();

                // Two root branches: Editor Gutter (9 items) and VCS Annotations (7 items)
                assertEquals(2, page.getCategoryTree().getRoot().getChildren().size());
                assertEquals("Editor Gutter", page.getCategoryTree().getRoot().getChildren().get(0).getValue());
                assertEquals(9, page.getCategoryTree().getRoot().getChildren().get(0).getChildren().size());
                assertEquals("VCS Annotations", page.getCategoryTree().getRoot().getChildren().get(1).getValue());
                assertEquals(7, page.getCategoryTree().getRoot().getChildren().get(1).getChildren().size());

                // Default selection: Changed lines popup
                assertEquals("Changed lines popup", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());

                // Attribute controls verification from Screenshot 1
                assertFalse(page.getBoldCheck().isSelected());
                assertFalse(page.getItalicCheck().isSelected());
                assertFalse(page.getForegroundCheck().isSelected());
                assertFalse(page.getBackgroundCheck().isSelected());
                assertFalse(page.getErrorStripeCheck().isSelected());
                assertFalse(page.getEffectsCheck().isSelected());
                assertEquals(EffectType.UNDERSCORED, page.getEffectTypeCombo().getValue());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testVcsPageSelectionAndModification() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeVcsPage page = new SettingsColorSchemeVcsPage();
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                assertFalse(page.isModified());

                // Select "Added lines"
                page.selectTreeItem("Added lines");
                assertEquals("Added lines", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertTrue(page.getBackgroundCheck().isSelected());
                assertEquals("385E39", page.getBackgroundSwatch().getText());
                assertTrue(page.getErrorStripeCheck().isSelected());
                assertEquals("43698D", page.getErrorStripeSwatch().getText());

                // Select "Background color #1"
                page.selectTreeItem("Background color #1");
                assertEquals("Background color #1", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertTrue(page.getBackgroundCheck().isSelected());
                assertEquals("23382B", page.getBackgroundSwatch().getText());

                // Modify bold
                page.selectTreeItem("Foreground for last commit");
                assertTrue(page.getBoldCheck().isSelected());
                page.getBoldCheck().setSelected(false);
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                // Apply
                page.apply();
                assertFalse(page.isModified());

                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertFalse(s.getAttribute(s.getActiveSchemeName(), "Foreground for last commit").isBold());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testVcsPreviewInteractivity() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeVcsPage page = new SettingsColorSchemeVcsPage();

                // 16 lines in preview
                assertEquals(16, page.getEditorLinesColumn().getChildren().size());
                assertEquals(16, page.getAnnotationColumn().getChildren().size());

                // Clicking Line 3 selects "Modified lines"
                HBox line3 = (HBox) page.getEditorLinesColumn().getChildren().get(2);
                line3.getOnMouseClicked().handle(null);
                assertEquals("Modified lines", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());

                // Clicking Line 5 selects "Added lines"
                HBox line5 = (HBox) page.getEditorLinesColumn().getChildren().get(4);
                line5.getOnMouseClicked().handle(null);
                assertEquals("Added lines", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());

                // Clicking Line 7 selects "Whitespace-modified lines"
                HBox line7 = (HBox) page.getEditorLinesColumn().getChildren().get(6);
                line7.getOnMouseClicked().handle(null);
                assertEquals("Whitespace-modified lines", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());

                // Clicking Line 1 selects "Changed lines popup"
                HBox line1 = (HBox) page.getEditorLinesColumn().getChildren().get(0);
                line1.getOnMouseClicked().handle(null);
                assertEquals("Changed lines popup", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());

                // Clicking Annotation Row 1 selects "Background color #1"
                HBox annot1 = (HBox) page.getAnnotationColumn().getChildren().get(0);
                annot1.getOnMouseClicked().handle(null);
                assertEquals("Background color #1", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());

                // Clicking Annotation Row 6 selects "Background color #2"
                HBox annot6 = (HBox) page.getAnnotationColumn().getChildren().get(5);
                annot6.getOnMouseClicked().handle(null);
                assertEquals("Background color #2", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 2. Java Page Tests (Screenshots 2 through 5)
    // -------------------------------------------------------------------------

    @Test
    void testJavaPageStructureAndScreenshot2Defaults() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeJavaPage page = new SettingsColorSchemeJavaPage();

                // 13 root categories
                assertEquals(13, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: "Annotation name" (Screenshot 2)
                assertEquals("Annotation name", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());

                // Attribute controls from Screenshot 2
                assertFalse(page.getBoldCheck().isSelected());
                assertFalse(page.getItalicCheck().isSelected());
                assertTrue(page.getForegroundCheck().isSelected());
                assertTrue(page.getForegroundSwatch().getText().equalsIgnoreCase("B3AF60")
                        || page.getForegroundSwatch().getText().equalsIgnoreCase("B3AE60"));
                assertFalse(page.getBackgroundCheck().isSelected());
                assertFalse(page.getErrorStripeCheck().isSelected());
                assertFalse(page.getEffectsCheck().isSelected());
                assertEquals(EffectType.BORDERED, page.getEffectTypeCombo().getValue());

                // Inheritance controls from Screenshot 2
                assertTrue(page.getInheritBox().isVisible());
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Metadata", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testJavaPageScreenshots3Through5Selections() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeJavaPage page = new SettingsColorSchemeJavaPage();

                // Screenshot 3: Braces and Operators // Brackets
                page.selectTreeItem("Brackets");
                assertEquals("Brackets", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertFalse(page.getBoldCheck().isSelected());
                assertFalse(page.getItalicCheck().isSelected());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("BCBEC4", page.getForegroundSwatch().getText());
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Braces and Operators->Brackets", page.getInheritTargetLabel().getText());

                // Screenshot 4: Class Fields // Instance field
                page.selectTreeItem("Instance field");
                assertEquals("Instance field", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertFalse(page.getBoldCheck().isSelected());
                assertFalse(page.getItalicCheck().isSelected());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("C77DBB", page.getForegroundSwatch().getText());
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Classes->Instance field", page.getInheritTargetLabel().getText());

                // Screenshot 5: Classes and Interfaces // Class
                page.selectTreeItem("Class");
                assertEquals("Class", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertFalse(page.getBoldCheck().isSelected());
                assertFalse(page.getItalicCheck().isSelected());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("BCBEC4", page.getForegroundSwatch().getText());
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Classes->Class name", page.getInheritTargetLabel().getText());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testJavaPreviewInteractivity() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeJavaPage page = new SettingsColorSchemeJavaPage();

                // 19 lines in code preview
                assertEquals(19, page.getCodeLinesBox().getChildren().size());

                // Clicking "AnnotationType" on Line 15 selects "Annotation name"
                HBox line15 = (HBox) page.getCodeLinesBox().getChildren().get(14);
                Label annotationTypeTok = (Label) line15.getChildren().get(1);
                assertEquals("AnnotationType", annotationTypeTok.getText());
                annotationTypeTok.getOnMouseClicked().handle(null);
                assertEquals("Annotation name", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());

                // Clicking "field" on Line 2 selects "Instance field"
                HBox line2 = (HBox) page.getCodeLinesBox().getChildren().get(1);
                Label fieldTok = (Label) line2.getChildren().get(1);
                assertEquals("field", fieldTok.getText());
                fieldTok.getOnMouseClicked().handle(null);
                assertEquals("Instance field", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());

                // Clicking "SomeClass" on Line 3 selects "Class"
                HBox line3 = (HBox) page.getCodeLinesBox().getChildren().get(2);
                Label someClassTok = (Label) line3.getChildren().get(1);
                assertEquals("SomeClass", someClassTok.getText());
                someClassTok.getOnMouseClicked().handle(null);
                assertEquals("Class", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 3. SettingsDialog Navigation & Lifecycle Tests
    // -------------------------------------------------------------------------

    @Test
    void testSettingsDialogNavigationAndLifecycleForVcsAndJava() throws Exception {
        if (!javaFxAvailable) return;
        String display = System.getenv("DISPLAY");
        if (display == null || display.isBlank() || java.awt.GraphicsEnvironment.isHeadless()) {
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDialog dialog = new SettingsDialog(null, "Appearance");

                // Navigate to VCS
                dialog.selectCategory("VCS");
                assertNotNull(dialog.getCurrentColorSchemeVcsPage());

                // Navigate to Java
                dialog.selectCategory("Java");
                assertNotNull(dialog.getCurrentColorSchemeJavaPage());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
