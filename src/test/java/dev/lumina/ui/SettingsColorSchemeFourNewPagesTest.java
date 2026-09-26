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

class SettingsColorSchemeFourNewPagesTest {

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
    // 1. Debugger Page Tests (media_1790422678161.png)
    // -------------------------------------------------------------------------

    @Test
    void testDebuggerPageStructureAndDefaults() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeDebuggerPage page = new SettingsColorSchemeDebuggerPage();

                // 11 flat attributes
                assertEquals(11, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: "Inlined modified values"
                assertEquals("Inlined modified values", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());

                // Attribute controls verification from Screenshot 1
                assertFalse(page.getBoldCheck().isSelected());
                assertTrue(page.getItalicCheck().isSelected());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("B2AE60", page.getForegroundSwatch().getText());
                assertFalse(page.getBackgroundCheck().isSelected());
                assertFalse(page.getErrorStripeCheck().isSelected());
                assertFalse(page.getEffectsCheck().isSelected());
                assertEquals(EffectType.BORDERED, page.getEffectTypeCombo().getValue());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testDebuggerPageModificationAndApply() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeDebuggerPage page = new SettingsColorSchemeDebuggerPage();
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                assertFalse(page.isModified());

                // Modify bold
                page.getBoldCheck().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                // Apply saves changes
                page.apply();
                assertFalse(page.isModified());

                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Inlined modified values").isBold());

                // Reset restores defaults
                page.getBoldCheck().setSelected(false);
                page.apply();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 2. Diff & Merge Page Tests (media_1790422711346.png)
    // -------------------------------------------------------------------------

    @Test
    void testDiffMergePageStructureAndSelection() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeDiffMergePage page = new SettingsColorSchemeDiffMergePage();

                // Two root branches: Changed lines and Folded unchanged fragments
                assertEquals(2, page.getCategoryTree().getRoot().getChildren().size());
                assertEquals("Changed lines", page.getCategoryTree().getRoot().getChildren().get(0).getValue());
                assertEquals(4, page.getCategoryTree().getRoot().getChildren().get(0).getChildren().size());
                assertEquals("Folded unchanged fragments", page.getCategoryTree().getRoot().getChildren().get(1).getValue());
                assertEquals(1, page.getCategoryTree().getRoot().getChildren().get(1).getChildren().size());

                // Initial selection: Changed
                assertEquals("Changed", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());

                // Custom controls verification from Screenshot 2
                assertEquals("385570", page.getImportantSwatch().getText());
                assertEquals("436980", page.getErrorStripeSwatch().getText());
                assertTrue(page.getInheritIgnoredCheck().isSelected());

                // Select Wave
                page.selectTreeItem("Wave");
                assertEquals("Wave", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("5C616B", page.getImportantSwatch().getText());
                assertFalse(page.getIgnoredSwatch().isVisible());
                assertFalse(page.getErrorStripeSwatch().isVisible());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testDiffMergePagePreviewAndClicks() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeDiffMergePage page = new SettingsColorSchemeDiffMergePage();

                // 3 preview panes: Left, Center, Right
                assertTrue(page.getLeftPaneBox().getChildren().size() > 10);
                assertTrue(page.getCenterPaneBox().getChildren().size() > 10);
                assertTrue(page.getRightPaneBox().getChildren().size() > 10);

                // Clicking inserted line in left pane selects "Inserted" in tree
                for (Node node : page.getLeftPaneBox().getChildren()) {
                    if (node instanceof HBox row && "Inserted".equals(row.getUserData())) {
                        row.getOnMouseClicked().handle(null);
                        break;
                    }
                }
                assertEquals("Inserted", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());

                // Clicking conflict line in right pane selects "Conflict" in tree
                for (Node node : page.getRightPaneBox().getChildren()) {
                    if (node instanceof HBox row && "Conflict".equals(row.getUserData())) {
                        row.getOnMouseClicked().handle(null);
                        break;
                    }
                }
                assertEquals("Conflict", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 3. JVM Logging Page Tests (media_1790422729751.png & media_1790422751274.png)
    // -------------------------------------------------------------------------

    @Test
    void testJvmLoggingPageStructureAndEffects() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeJvmLoggingPage page = new SettingsColorSchemeJvmLoggingPage();

                // Two branches: Classes, Log string
                assertEquals(2, page.getCategoryTree().getRoot().getChildren().size());
                assertEquals("Classes", page.getCategoryTree().getRoot().getChildren().get(0).getValue());
                assertEquals("Log string", page.getCategoryTree().getRoot().getChildren().get(1).getValue());

                // Default selection: Placeholder (Screenshot 3)
                assertEquals("Placeholder", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("CF8E6D", page.getForegroundSwatch().getText());
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("String->Escape sequence->Valid (Language Defaults)", page.getInheritLink().getText());

                // Select Class name (Screenshot 4)
                page.selectTreeItem("Class name");
                assertEquals("Class name", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertTrue(page.getEffectsCheck().isSelected());
                assertEquals("888880", page.getEffectsSwatch().getText());
                assertEquals(EffectType.DOTTED_LINE, page.getEffectTypeCombo().getValue());
                assertTrue(page.getEffectTypeCombo().getItems().contains(EffectType.DOTTED_LINE));
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testJvmLoggingPreviewInteraction() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeJvmLoggingPage page = new SettingsColorSchemeJvmLoggingPage();

                // Preview contains lines
                assertEquals(3, page.getCodeLinesBox().getChildren().size());

                // Line 1 contains ClassName label
                HBox line1 = (HBox) page.getCodeLinesBox().getChildren().get(0);
                Label classLabel = (Label) line1.getChildren().get(1);
                assertEquals("ClassName", classLabel.getText());

                // Click ClassName in preview selects "Class name" in tree
                page.selectTreeItem("Placeholder");
                classLabel.getOnMouseClicked().handle(null);
                assertEquals("Class name", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());

                // Line 3 contains placeholder {}
                HBox line3 = (HBox) page.getCodeLinesBox().getChildren().get(2);
                Label ph1 = (Label) line3.getChildren().get(1);
                assertEquals("{}", ph1.getText());

                // Click {} in preview selects "Placeholder" in tree
                ph1.getOnMouseClicked().handle(null);
                assertEquals("Placeholder", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 4. User-Defined File Types Page Tests (media_1790422763100.png)
    // -------------------------------------------------------------------------

    @Test
    void testUserDefinedFileTypesStructureAndDefaults() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeUserDefinedFileTypesPage page = new SettingsColorSchemeUserDefinedFileTypesPage();

                // 10 flat attributes
                assertEquals(10, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Keyword2 (Screenshot 5)
                assertEquals("Keyword2", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("C77DBB", page.getForegroundSwatch().getText());
                assertFalse(page.getBoldCheck().isSelected());
                assertFalse(page.getItalicCheck().isSelected());
                assertFalse(page.getBackgroundCheck().isSelected());
                assertFalse(page.getErrorStripeCheck().isSelected());
                assertFalse(page.getEffectsCheck().isSelected());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testUserDefinedFileTypesPreviewInteraction() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeUserDefinedFileTypesPage page = new SettingsColorSchemeUserDefinedFileTypesPage();

                // Verify preview lines are populated
                assertTrue(page.getCodeLinesBox().getChildren().size() >= 10);

                // Clicking line comment selects "Line comment"
                HBox line1 = (HBox) page.getCodeLinesBox().getChildren().get(0);
                Label lineCommentLbl = (Label) line1.getChildren().get(0);
                lineCommentLbl.getOnMouseClicked().handle(null);
                assertEquals("Line comment", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());

                // Clicking Keyword1 selects "Keyword1"
                HBox line2 = (HBox) page.getCodeLinesBox().getChildren().get(1);
                Label kw1Lbl = (Label) line2.getChildren().get(0);
                assertEquals("aKeyword1", kw1Lbl.getText());
                kw1Lbl.getOnMouseClicked().handle(null);
                assertEquals("Keyword1", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());

                // Clicking Number selects "Number"
                Label numLbl = (Label) line2.getChildren().get(2);
                assertEquals("123", numLbl.getText());
                numLbl.getOnMouseClicked().handle(null);
                assertEquals("Number", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 5. SettingsDialog Navigation & Lifecycle Tests
    // -------------------------------------------------------------------------

    @Test
    void testSettingsDialogNavigationAndLifecycleForNewPages() throws Exception {
        if (!javaFxAvailable) return;
        String display = System.getenv("DISPLAY");
        if (display == null || display.isBlank() || java.awt.GraphicsEnvironment.isHeadless()) {
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDialog dialog = new SettingsDialog(null, "Appearance");

                // Navigate to Debugger
                dialog.selectCategory("Debugger");
                assertNotNull(dialog.getCurrentColorSchemeDebuggerPage());

                // Navigate to Diff & Merge
                dialog.selectCategory("Diff & Merge");
                assertNotNull(dialog.getCurrentColorSchemeDiffMergePage());

                // Navigate to JVM Logging
                dialog.selectCategory("JVM Logging");
                assertNotNull(dialog.getCurrentColorSchemeJvmLoggingPage());

                // Navigate to User-Defined File Types
                dialog.selectCategory("User-Defined File Types");
                assertNotNull(dialog.getCurrentColorSchemeUserDefinedFileTypesPage());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
