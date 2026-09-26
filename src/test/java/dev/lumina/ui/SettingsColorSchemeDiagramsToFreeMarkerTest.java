package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import dev.lumina.settings.EditorColorSchemeSettings.EffectType;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SettingsColorSchemeDiagramsToFreeMarkerTest {

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
    // 1. Diagrams Page Tests (media_1790428910213.png)
    // -------------------------------------------------------------------------

    @Test
    void testDiagramsPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeDiagramsPage page = new SettingsColorSchemeDiagramsPage();

                // 10 top-level items in tree (5 groups + 5 leaves)
                assertEquals(10, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Edge selection
                assertEquals("Edge selection", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Edge selection", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("CC7832", page.getForegroundSwatch().getText());
                assertTrue(page.getEffectsCheck().isSelected());
                assertEquals(EffectType.UNDERSCORED, page.getSelectedEffectType());

                // Modification and Apply
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getBoldCheck().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.apply();
                assertFalse(page.isModified());

                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Edges // Edge selection").isBold());

                // Select Annotation edge
                page.selectTreeItem("Annotation edge");
                assertEquals("Annotation edge", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("56A8F5", page.getForegroundSwatch().getText());

                // Select Realization edge
                page.selectTreeItem("Realization edge");
                assertEquals("Realization edge", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("499C54", page.getForegroundSwatch().getText());

                // Reset
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
    // 2. Dockerfile Page Tests (media_1790428926346.png)
    // -------------------------------------------------------------------------

    @Test
    void testDockerfilePage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeDockerfilePage page = new SettingsColorSchemeDockerfilePage();

                // 9 flat attributes
                assertEquals(9, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Number
                assertEquals("Number", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Number", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("2AACB8", page.getForegroundSwatch().getText());
                assertTrue(page.isInheritChecked());
                assertEquals("Number", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

                // Preview has code lines
                assertTrue(page.getCodeLinesBox().getChildren().size() >= 10);

                // Modification and Apply
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getBoldCheck().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.apply();
                assertFalse(page.isModified());

                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Number").isBold());

                // Select Keyword
                page.selectTreeItem("Keyword");
                assertEquals("Keyword", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("CF8E6D", page.getForegroundSwatch().getText());
                assertEquals("Keyword", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

                // Reset
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
    // 3. EditorConfig Page Tests (media_1790428937225.png)
    // -------------------------------------------------------------------------

    @Test
    void testEditorConfigPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeEditorConfigPage page = new SettingsColorSchemeEditorConfigPage();

                // 13 flat attributes
                assertEquals(13, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Property key
                assertEquals("Property key", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Property key", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("9876AA", page.getForegroundSwatch().getText());
                assertTrue(page.isInheritChecked());
                assertEquals("Classes->Instance field", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

                // Preview has code lines
                assertTrue(page.getCodeLinesBox().getChildren().size() >= 15);

                // Modification and Apply
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getBoldCheck().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.apply();
                assertFalse(page.isModified());

                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Property key").isBold());

                // Select Header
                page.selectTreeItem("Header");
                assertEquals("Header", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("CF8E6D", page.getForegroundSwatch().getText());
                assertEquals("Keyword", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

                // Reset
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
    // 4. ERB Page Tests (media_1790428949903.png)
    // -------------------------------------------------------------------------

    @Test
    void testErbPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeErbPage page = new SettingsColorSchemeErbPage();

                // 7 flat attributes
                assertEquals(7, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Comment
                assertEquals("Comment", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Comment", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("BC9458", page.getForegroundSwatch().getText());
                assertTrue(page.getItalicCheck().isSelected());
                // Inherit is unchecked by default in screenshot media_1790428949903.png
                assertFalse(page.isInheritChecked());
                assertEquals("Comment", page.getInheritTargetLabel().getText());
                assertEquals("(HTML)", page.getInheritScopeLabel().getText());

                // Preview has code lines
                assertTrue(page.getCodeLinesBox().getChildren().size() >= 5);

                // Modification and Apply
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getBoldCheck().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.apply();
                assertFalse(page.isModified());

                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Comment").isBold());

                // Select Execution tags
                page.selectTreeItem("Execution tags");
                assertEquals("Execution tags", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());

                // Reset
                page.getItalicCheck().setSelected(false);
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
    // 5. FreeMarker Page Tests (media_1790428959949.png)
    // -------------------------------------------------------------------------

    @Test
    void testFreeMarkerPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeFreeMarkerPage page = new SettingsColorSchemeFreeMarkerPage();

                // 14 flat attributes
                assertEquals(14, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Escape
                assertEquals("Escape", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Escape", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("CF8E6D", page.getForegroundSwatch().getText());
                assertTrue(page.getBoldCheck().isSelected());
                assertTrue(page.isInheritChecked());
                assertEquals("String->Escape sequence->Valid", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

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
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Escape").isItalic());

                // Select Directive
                page.selectTreeItem("Directive");
                assertEquals("Directive", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("E8BF6A", page.getForegroundSwatch().getText());

                // Reset
                page.getBoldCheck().setSelected(false);
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
    // 6. SettingsDialog Integration Test
    // -------------------------------------------------------------------------

    @Test
    void testSettingsDialogRoutingForFivePages() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDialog dialog = new SettingsDialog(null);

                // Helper to find and select under Editor > Color Scheme
                TreeItem<String> csNode = null;
                for (TreeItem<String> edChild : dialog.getTree().getRoot().getChildren().get(2).getChildren()) {
                    if ("Color Scheme".equals(edChild.getValue())) {
                        csNode = edChild;
                        break;
                    }
                }
                assertNotNull(csNode, "Color Scheme category node should exist under Editor");

                // 1. Diagrams
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("Diagrams".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                assertNotNull(dialog.getCurrentColorSchemeDiagramsPage());
                assertEquals("Edge selection", dialog.getCurrentColorSchemeDiagramsPage().getSelectedKey());

                // 2. Dockerfile
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("Dockerfile".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                assertNotNull(dialog.getCurrentColorSchemeDockerfilePage());
                assertEquals("Number", dialog.getCurrentColorSchemeDockerfilePage().getSelectedKey());

                // 3. EditorConfig
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("EditorConfig".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                assertNotNull(dialog.getCurrentColorSchemeEditorConfigPage());
                assertEquals("Property key", dialog.getCurrentColorSchemeEditorConfigPage().getSelectedKey());

                // 4. ERB
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("ERB".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                assertNotNull(dialog.getCurrentColorSchemeErbPage());
                assertEquals("Comment", dialog.getCurrentColorSchemeErbPage().getSelectedKey());

                // 5. FreeMarker
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("FreeMarker".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                assertNotNull(dialog.getCurrentColorSchemeFreeMarkerPage());
                assertEquals("Escape", dialog.getCurrentColorSchemeFreeMarkerPage().getSelectedKey());

                // Test dirty tracking and applyAll()
                assertTrue(dialog.getApplyButton().isDisabled());
                dialog.getCurrentColorSchemeFreeMarkerPage().getItalicCheck().setSelected(true);
                assertFalse(dialog.getApplyButton().isDisabled());

                dialog.applyAll();
                assertTrue(dialog.getApplyButton().isDisabled());

                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Escape").isItalic());

            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
