package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import dev.lumina.settings.EditorColorSchemeSettings.AttributesDescriptor;
import dev.lumina.settings.EditorColorSchemeSettings.ColorAttribute;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
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

class SettingsColorSchemeKotlinToMarkdownTest {

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
    // 1. Kotlin Page Tests (media_1790468925307.png)
    // -------------------------------------------------------------------------

    @Test
    void testKotlinPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeKotlinPage page = new SettingsColorSchemeKotlinPage();

                // Check category tree has groups + leaves
                assertTrue(page.getCategoryTree().getRoot().getChildren().size() >= 10);

                // Default selection: Label (#32B8AF, inherit=false, target="Identifiers->Label", scope="(Language Defaults)")
                assertEquals("Label", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Label", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("32B8AF", page.getForegroundSwatch().getText());
                assertFalse(page.isInheritChecked(), "Label inherit checkbox should be unchecked per reference screenshot");
                assertEquals("Identifiers->Label", page.getInheritTargetLabel().getText());
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
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Label").isBold());

                // Select Named argument
                page.selectTreeItem("Named argument");
                assertEquals("Named argument", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("56A8F5", page.getForegroundSwatch().getText());
                assertTrue(page.isInheritChecked());
                assertEquals("Identifiers->Parameter", page.getInheritTargetLabel().getText());

                // Reset
                page.getItalicCheck().setSelected(true);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Backward compatibility
                SettingsKotlinPage legacy = new SettingsKotlinPage();
                assertNotNull(legacy);
                assertTrue(legacy instanceof SettingsColorSchemeKotlinPage);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 2. Kubernetes Page Tests (media_1790468934030.png)
    // -------------------------------------------------------------------------

    @Test
    void testKubernetesPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeKubernetesPage page = new SettingsColorSchemeKubernetesPage();

                // 5 flat descriptors
                assertEquals(5, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Enumeration (#56A8F5, inherit=true, target="Identifiers->Function declaration", scope="(Language Defaults)")
                assertEquals("Enumeration", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Enumeration", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("56A8F5", page.getForegroundSwatch().getText());
                assertTrue(page.isInheritChecked());
                assertEquals("Identifiers->Function declaration", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

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
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Enumeration").isBold());

                // Select Group, version, kind
                page.selectTreeItem("Group, version, kind");
                assertEquals("Group, version, kind", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("9876AA", page.getForegroundSwatch().getText());
                assertTrue(page.isInheritChecked());
                assertEquals("Identifiers->Constant", page.getInheritTargetLabel().getText());

                // Reset
                page.getItalicCheck().setSelected(true);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Backward compatibility
                SettingsKubernetesPage legacy = new SettingsKubernetesPage();
                assertNotNull(legacy);
                assertTrue(legacy instanceof SettingsColorSchemeKubernetesPage);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 3. Less Page Tests (media_1790468961840.png)
    // -------------------------------------------------------------------------

    @Test
    void testLessPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeLessPage page = new SettingsColorSchemeLessPage();

                // Check descriptors count (31 descriptors)
                assertEquals(31, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Comma (#BCBEC4, inherit=true, target="Comma", scope="(CSS)")
                assertEquals("Comma", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Comma", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("BCBEC4", page.getForegroundSwatch().getText());
                assertTrue(page.isInheritChecked());
                assertEquals("Comma", page.getInheritTargetLabel().getText());
                assertEquals("(CSS)", page.getInheritScopeLabel().getText());

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
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Comma").isBold());

                // Select Class name
                page.selectTreeItem("Class name");
                assertEquals("Class name", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("56A8F5", page.getForegroundSwatch().getText());
                assertTrue(page.isInheritChecked());
                assertEquals("Class name", page.getInheritTargetLabel().getText());
                assertEquals("(CSS)", page.getInheritScopeLabel().getText());

                // Reset
                page.getItalicCheck().setSelected(true);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Backward compatibility
                SettingsLessPage legacy = new SettingsLessPage();
                assertNotNull(legacy);
                assertTrue(legacy instanceof SettingsColorSchemeLessPage);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 4. Lombok Config Page Tests (media_1790468970525.png)
    // -------------------------------------------------------------------------

    @Test
    void testLombokConfigPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeLombokConfigPage page = new SettingsColorSchemeLombokConfigPage();

                // 5 flat descriptors
                assertEquals(5, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Key (#CF8E6D, inherit=true, target="Keyword", scope="(Language Defaults)")
                assertEquals("Key", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Key", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("CF8E6D", page.getForegroundSwatch().getText());
                assertTrue(page.isInheritChecked());
                assertEquals("Keyword", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

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
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Key").isBold());

                // Select Separator
                page.selectTreeItem("Separator");
                assertEquals("Separator", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("BCBEC4", page.getForegroundSwatch().getText());
                assertTrue(page.isInheritChecked());
                assertEquals("Operation sign", page.getInheritTargetLabel().getText());

                // Reset
                page.getItalicCheck().setSelected(true);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Backward compatibility
                SettingsLombokConfigPage legacy = new SettingsLombokConfigPage();
                assertNotNull(legacy);
                assertTrue(legacy instanceof SettingsColorSchemeLombokConfigPage);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 5. Markdown Page Tests (media_1790469000420.png)
    // -------------------------------------------------------------------------

    @Test
    void testMarkdownPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeMarkdownPage page = new SettingsColorSchemeMarkdownPage();

                // Check category tree has groups + leaves
                assertTrue(page.getCategoryTree().getRoot().getChildren().size() >= 8);

                // Default selection: Blockquote (#6AAB73, inherit=true, target="String->String text", scope="(Language Defaults)")
                assertEquals("Blockquote", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Blockquote", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("6AAB73", page.getForegroundSwatch().getText());
                assertTrue(page.isInheritChecked());
                assertEquals("String->String text", page.getInheritTargetLabel().getText());
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
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Blockquote // Blockquote").isBold());

                // Select Code block
                page.selectTreeItem("Code block");
                assertEquals("Code block", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("BCBEC4", page.getForegroundSwatch().getText());
                assertFalse(page.isInheritChecked());

                // Reset
                page.getItalicCheck().setSelected(true);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Backward compatibility
                SettingsMarkdownPage legacy = new SettingsMarkdownPage();
                assertNotNull(legacy);
                assertTrue(legacy instanceof SettingsColorSchemeMarkdownPage);
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

                // 1. Kotlin
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("Kotlin".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                assertNotNull(dialog.getCurrentColorSchemeKotlinPage());
                assertEquals("Label", dialog.getCurrentColorSchemeKotlinPage().getSelectedKey());

                // 2. Kubernetes
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("Kubernetes".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                assertNotNull(dialog.getCurrentColorSchemeKubernetesPage());
                assertEquals("Enumeration", dialog.getCurrentColorSchemeKubernetesPage().getSelectedKey());

                // 3. Less
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("Less".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                assertNotNull(dialog.getCurrentColorSchemeLessPage());
                assertEquals("Comma", dialog.getCurrentColorSchemeLessPage().getSelectedKey());

                // 4. Lombok Config
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("Lombok Config".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                assertNotNull(dialog.getCurrentColorSchemeLombokConfigPage());
                assertEquals("Key", dialog.getCurrentColorSchemeLombokConfigPage().getSelectedKey());

                // 5. Markdown
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("Markdown".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                assertNotNull(dialog.getCurrentColorSchemeMarkdownPage());
                assertEquals("Blockquote", dialog.getCurrentColorSchemeMarkdownPage().getSelectedKey());

                // Test dirty tracking and applyAll()
                assertTrue(dialog.getApplyButton().isDisabled());
                dialog.getCurrentColorSchemeMarkdownPage().getItalicCheck().setSelected(true);
                assertFalse(dialog.getApplyButton().isDisabled());

                dialog.applyAll();
                assertTrue(dialog.getApplyButton().isDisabled());

                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Blockquote // Blockquote").isItalic());

                // Test Revert Link navigation for Kotlin -> Language Defaults
                dialog.getCurrentColorSchemeKotlinPage().getInheritTargetLabel().fire();
                assertEquals("Language Defaults", dialog.getTree().getSelectionModel().getSelectedItem().getValue());

                // Return to Less and test Revert Link navigation -> CSS
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("Less".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                dialog.getCurrentColorSchemeLessPage().getInheritTargetLabel().fire();
                assertEquals("CSS", dialog.getTree().getSelectionModel().getSelectedItem().getValue());

            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 7. Brand Isolation Test
    // -------------------------------------------------------------------------

    @Test
    void testBrandIsolation() throws Exception {
        Pattern competitorPattern = Pattern.compile("(?i)\\b(intellij|jetbrains|idea)\\b");

        List<String> filesToCheck = List.of(
                "src/main/java/dev/lumina/ui/SettingsColorSchemeKotlinPage.java",
                "src/main/java/dev/lumina/ui/SettingsColorSchemeKubernetesPage.java",
                "src/main/java/dev/lumina/ui/SettingsColorSchemeLessPage.java",
                "src/main/java/dev/lumina/ui/SettingsColorSchemeLombokConfigPage.java",
                "src/main/java/dev/lumina/ui/SettingsColorSchemeMarkdownPage.java",
                "src/main/java/dev/lumina/ui/SettingsKotlinPage.java",
                "src/main/java/dev/lumina/ui/SettingsKubernetesPage.java",
                "src/main/java/dev/lumina/ui/SettingsLessPage.java",
                "src/main/java/dev/lumina/ui/SettingsLombokConfigPage.java",
                "src/main/java/dev/lumina/ui/SettingsMarkdownPage.java"
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
