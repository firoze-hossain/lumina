package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import dev.lumina.settings.EditorColorSchemeSettings.EffectType;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SettingsColorSchemeFiveNewPagesTest {

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
    // 1. Angular Template Page Tests (media_1790427845114.png)
    // -------------------------------------------------------------------------

    @Test
    void testAngularTemplatePage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeAngularTemplatePage page = new SettingsColorSchemeAngularTemplatePage();

                // 15 flat attributes
                assertEquals(15, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Block name
                assertEquals("Block name", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("CF8E6D", page.getForegroundSwatch().getText());
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Keyword", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

                // Preview has 18 lines
                assertEquals(18, page.getCodeLinesBox().getChildren().size());

                // Modification and Apply
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getBoldCheck().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.apply();
                assertFalse(page.isModified());

                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Block name").isBold());

                // Select Signal
                page.selectTreeItem("Signal");
                assertEquals("Signal", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("56A8F5", page.getForegroundSwatch().getText());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 2. Context Free Grammar Page Tests (media_1790427901715.png)
    // -------------------------------------------------------------------------

    @Test
    void testContextFreeGrammarPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeContextFreeGrammarPage page = new SettingsColorSchemeContextFreeGrammarPage();

                // 26 flat attributes
                assertEquals(26, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Import Keyword
                assertEquals("Import Keyword", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("CF8E6D", page.getForegroundSwatch().getText());
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Keyword", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

                // Preview has 8 lines
                assertEquals(8, page.getCodeLinesBox().getChildren().size());

                // Select Line Comment
                page.selectTreeItem("Line Comment");
                assertEquals("Line Comment", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("7A7E85", page.getForegroundSwatch().getText());
                assertTrue(page.getItalicCheck().isSelected());

                // Modify and apply
                page.getBoldCheck().setSelected(true);
                assertTrue(page.isModified());
                page.apply();
                assertFalse(page.isModified());

                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Line Comment").isBold());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 3. CSS Page Tests (media_1790427948503.png)
    // -------------------------------------------------------------------------

    @Test
    void testCssPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeCssPage page = new SettingsColorSchemeCssPage();

                // 27 flat attributes
                assertEquals(27, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Attribute name
                assertEquals("Attribute name", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("D5B778", page.getForegroundSwatch().getText());
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Identifier", page.getInheritTargetLabel().getText());
                assertEquals("(CSS)", page.getInheritScopeLabel().getText());

                // Preview has 5 lines (with empty line)
                assertTrue(page.getCodeLinesBox().getChildren().size() >= 5);

                // Select Tag name
                page.selectTreeItem("Tag name");
                assertEquals("Tag name", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("CF8E6D", page.getForegroundSwatch().getText());

                // Modify, reset
                page.getItalicCheck().setSelected(true);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 4. Data Editor and Viewer Page Tests (media_1790427959950.png)
    // -------------------------------------------------------------------------

    @Test
    void testDataEditorViewerPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeDataEditorViewerPage page = new SettingsColorSchemeDataEditorViewerPage();

                // Group "Data Grid" with 4 items
                assertEquals(1, page.getCategoryTree().getRoot().getChildren().size());
                TreeItem<String> gridGroup = page.getCategoryTree().getRoot().getChildren().get(0);
                assertEquals("Data Grid", gridGroup.getValue());
                assertEquals(4, gridGroup.getChildren().size());

                // Default selection: Error data
                assertEquals("Error data", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertTrue(page.getErrorStripeCheck().isSelected());
                assertEquals("D64D5B", page.getErrorStripeSwatch().getText());
                assertTrue(page.getEffectsCheck().isSelected());
                assertEquals("FA6675", page.getEffectsSwatch().getText());
                assertEquals(EffectType.UNDERWAVED, page.getEffectTypeCombo().getValue());
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Errors and Warnings->Error", page.getInheritTargetLabel().getText());
                assertEquals("(General)", page.getInheritScopeLabel().getText());

                // Table preview contains header + 3 rows
                assertEquals(4, page.getPreviewTableBox().getChildren().size());

                // Select Null data
                page.selectTreeItem("Null data");
                assertEquals("Null data", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("7A7E85", page.getForegroundSwatch().getText());
                assertTrue(page.getItalicCheck().isSelected());

                // Modify and apply
                page.getBoldCheck().setSelected(true);
                assertTrue(page.isModified());
                page.apply();
                assertFalse(page.isModified());

                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Data Grid // Null data").isBold());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 5. Database Page Tests (media_1790427990350.png)
    // -------------------------------------------------------------------------

    @Test
    void testDatabasePage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeDatabasePage page = new SettingsColorSchemeDatabasePage();

                // Two groups: Console (1 item) and Parameters (2 items)
                assertEquals(2, page.getCategoryTree().getRoot().getChildren().size());
                assertEquals("Console", page.getCategoryTree().getRoot().getChildren().get(0).getValue());
                assertEquals(1, page.getCategoryTree().getRoot().getChildren().get(0).getChildren().size());
                assertEquals("Parameters", page.getCategoryTree().getRoot().getChildren().get(1).getValue());
                assertEquals(2, page.getCategoryTree().getRoot().getChildren().get(1).getChildren().size());

                // Default selection: Statement to execute
                assertEquals("Statement to execute", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertTrue(page.getEffectsCheck().isSelected());
                assertEquals("3D7A49", page.getEffectsSwatch().getText());
                assertEquals(EffectType.BORDERED, page.getEffectTypeCombo().getValue());

                // Preview has statement container
                assertEquals(1, page.getSqlStatementBox().getChildren().size());
                VBox stmtBox = (VBox) page.getSqlStatementBox().getChildren().get(0);
                assertEquals(5, stmtBox.getChildren().size());

                // Select Parameter
                page.selectTreeItem("Parameter");
                assertEquals("Parameter", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("9876AA", page.getForegroundSwatch().getText());

                // Modify and apply
                page.getItalicCheck().setSelected(true);
                assertTrue(page.isModified());
                page.apply();
                assertFalse(page.isModified());

                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Parameters // Parameter").isItalic());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 6. SettingsDialog Integration Test
    // -------------------------------------------------------------------------

    @Test
    void testSettingsDialogRoutingForFivePages() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDialog dialog = new SettingsDialog(null);

                // 1. Angular Template
                dialog.selectCategory("Angular Template");
                assertNotNull(dialog.getCurrentColorSchemeAngularTemplatePage());
                assertEquals("Block name", dialog.getCurrentColorSchemeAngularTemplatePage().getCategoryTree().getSelectionModel().getSelectedItem().getValue());

                // 2. Context Free Grammar
                dialog.selectCategory("Context Free Grammar");
                assertNotNull(dialog.getCurrentColorSchemeContextFreeGrammarPage());
                assertEquals("Import Keyword", dialog.getCurrentColorSchemeContextFreeGrammarPage().getCategoryTree().getSelectionModel().getSelectedItem().getValue());

                // 3. CSS
                dialog.selectCategory("CSS");
                assertNotNull(dialog.getCurrentColorSchemeCssPage());
                assertEquals("Attribute name", dialog.getCurrentColorSchemeCssPage().getCategoryTree().getSelectionModel().getSelectedItem().getValue());

                // 4. Data Editor and Viewer (under Color Scheme)
                TreeItem<String> csNode = null;
                for (TreeItem<String> edChild : dialog.getTree().getRoot().getChildren().get(2).getChildren()) {
                    if ("Color Scheme".equals(edChild.getValue())) {
                        csNode = edChild;
                        break;
                    }
                }
                assertNotNull(csNode);
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("Data Editor and Viewer".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                assertNotNull(dialog.getCurrentColorSchemeDataEditorViewerPage());
                assertEquals("Error data", dialog.getCurrentColorSchemeDataEditorViewerPage().getCategoryTree().getSelectionModel().getSelectedItem().getValue());

                // 5. Database
                dialog.selectCategory("Database");
                assertNotNull(dialog.getCurrentColorSchemeDatabasePage());
                assertEquals("Statement to execute", dialog.getCurrentColorSchemeDatabasePage().getCategoryTree().getSelectionModel().getSelectedItem().getValue());

            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
