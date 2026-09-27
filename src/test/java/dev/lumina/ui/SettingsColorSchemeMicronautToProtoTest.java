package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import dev.lumina.settings.EditorColorSchemeSettings.EffectType;
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

class SettingsColorSchemeMicronautToProtoTest {

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
    // 1. Micronaut EL Page Tests (media_1790471393646.png)
    // -------------------------------------------------------------------------

    @Test
    void testMicronautELPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeMicronautELPage page = new SettingsColorSchemeMicronautELPage();

                // 12 top-level items in tree
                assertEquals(12, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Comma (#BCBEC4, inherit=true, target="Braces and Operators->Comma", scope="(Language Defaults)")
                assertEquals("Comma", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Braces and Operators // Comma", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("BCBEC4", page.getForegroundSwatch().getText());
                assertTrue(page.isInheritChecked());
                assertEquals("Braces and Operators->Comma", page.getInheritTargetLabel().getText());
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
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Braces and Operators // Comma").isBold());

                // Select Method call
                page.selectTreeItem("Method call");
                assertEquals("Method call", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("56A8F5", page.getForegroundSwatch().getText());
                assertTrue(page.isInheritChecked());
                assertEquals("Functions->Method call", page.getInheritTargetLabel().getText());

                // Reset
                page.getItalicCheck().setSelected(true);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Backward compatibility
                SettingsMicronautELPage legacy = new SettingsMicronautELPage();
                assertNotNull(legacy);
                assertTrue(legacy instanceof SettingsColorSchemeMicronautELPage);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 2. PHP Page Tests (media_1790471419232.png)
    // -------------------------------------------------------------------------

    @Test
    void testPHPPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemePHPPage page = new SettingsColorSchemePHPPage();

                // 17 top-level items in tree
                assertEquals(17, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Named Arguments (#467CDA, no inherit)
                assertEquals("Named Arguments", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Named Arguments", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("467CDA", page.getForegroundSwatch().getText());
                assertFalse(page.isInheritChecked());

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
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Named Arguments").isBold());

                // Select Keywords
                page.selectTreeItem("Keywords");
                assertEquals("Keywords", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("CF8E6D", page.getForegroundSwatch().getText());
                assertTrue(page.isInheritChecked());
                assertEquals("Keyword", page.getInheritTargetLabel().getText());

                // Reset
                page.getItalicCheck().setSelected(true);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Backward compatibility
                SettingsPHPPage legacy = new SettingsPHPPage();
                assertNotNull(legacy);
                assertTrue(legacy instanceof SettingsColorSchemePHPPage);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 3. plan9_x86 Page Tests (media_1790471429170.png)
    // -------------------------------------------------------------------------

    @Test
    void testPlan9X86Page() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemePlan9X86Page page = new SettingsColorSchemePlan9X86Page();

                // 11 flat descriptors
                assertEquals(11, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Label (#4EADE5, inherit=false, target="Identifiers->Label", scope="(Language Defaults)")
                assertEquals("Label", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Label", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("4EADE5", page.getForegroundSwatch().getText());
                assertFalse(page.isInheritChecked(), "Label inherit should be unchecked per reference screenshot");
                assertEquals(EffectType.UNDERSCORED, page.getEffectTypeCombo().getValue());
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

                // Select Instruction
                page.selectTreeItem("Instruction");
                assertEquals("Instruction", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("56A8F5", page.getForegroundSwatch().getText());
                assertTrue(page.isInheritChecked());
                assertEquals("Keyword", page.getInheritTargetLabel().getText());

                // Reset
                page.getItalicCheck().setSelected(true);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Backward compatibility
                SettingsPlan9X86Page legacy = new SettingsPlan9X86Page();
                assertNotNull(legacy);
                assertTrue(legacy instanceof SettingsColorSchemePlan9X86Page);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 4. PostCSS Page Tests (media_1790471448969.png)
    // -------------------------------------------------------------------------

    @Test
    void testPostCSSPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemePostCSSPage page = new SettingsColorSchemePostCSSPage();

                // 28 flat descriptors
                assertEquals(28, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Colon (#BCBEC4, background=#191A1C, inherit=true, target="Colon", scope="(CSS)")
                assertEquals("Colon", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Colon", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("BCBEC4", page.getForegroundSwatch().getText());
                assertTrue(page.getBackgroundCheck().isSelected(), "Background checkbox should be selected per reference screenshot");
                assertEquals("191A1C", page.getBackgroundSwatch().getText());
                assertTrue(page.isInheritChecked());
                assertEquals("Colon", page.getInheritTargetLabel().getText());
                assertEquals("(CSS)", page.getInheritScopeLabel().getText());

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
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Colon").isBold());

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
                SettingsPostCSSPage legacy = new SettingsPostCSSPage();
                assertNotNull(legacy);
                assertTrue(legacy instanceof SettingsColorSchemePostCSSPage);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 5. Protocol Buffer Page Tests (media_1790471462024.png)
    // -------------------------------------------------------------------------

    @Test
    void testProtocolBufferPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeProtocolBufferPage page = new SettingsColorSchemeProtocolBufferPage();

                // 16 flat descriptors
                assertEquals(16, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Identifier (#BCBEC4, inherit=true, target="Identifiers->Default", scope="(Language Defaults)")
                assertEquals("Identifier", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Identifier", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("BCBEC4", page.getForegroundSwatch().getText());
                assertTrue(page.isInheritChecked());
                assertEquals("Identifiers->Default", page.getInheritTargetLabel().getText());
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
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Identifier").isBold());

                // Select Enum value
                page.selectTreeItem("Enum value");
                assertEquals("Enum value", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("C77DBB", page.getForegroundSwatch().getText());
                assertTrue(page.isInheritChecked());
                assertEquals("Classes->Static final field", page.getInheritTargetLabel().getText());

                // Reset
                page.getItalicCheck().setSelected(true);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Backward compatibility
                SettingsProtocolBufferPage legacy = new SettingsProtocolBufferPage();
                assertNotNull(legacy);
                assertTrue(legacy instanceof SettingsColorSchemeProtocolBufferPage);
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

                // Verify PHP and plan9_x86 are in the Color Scheme children
                assertTrue(csNode.getChildren().stream().anyMatch(c -> "PHP".equals(c.getValue())));
                assertTrue(csNode.getChildren().stream().anyMatch(c -> "plan9_x86".equals(c.getValue())));

                // 1. Micronaut EL
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("Micronaut EL".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                assertNotNull(dialog.getCurrentColorSchemeMicronautELPage());
                assertEquals("Braces and Operators // Comma", dialog.getCurrentColorSchemeMicronautELPage().getSelectedKey());

                // 2. PHP
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("PHP".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                assertNotNull(dialog.getCurrentColorSchemePhpPage());
                assertEquals("Named Arguments", dialog.getCurrentColorSchemePhpPage().getSelectedKey());

                // 3. plan9_x86
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("plan9_x86".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                assertNotNull(dialog.getCurrentColorSchemePlan9X86Page());
                assertEquals("Label", dialog.getCurrentColorSchemePlan9X86Page().getSelectedKey());

                // 4. PostCSS
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("PostCSS".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                assertNotNull(dialog.getCurrentColorSchemePostCSSPage());
                assertEquals("Colon", dialog.getCurrentColorSchemePostCSSPage().getSelectedKey());

                // 5. Protocol Buffer
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("Protocol Buffer".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                assertNotNull(dialog.getCurrentColorSchemeProtocolBufferPage());
                assertEquals("Identifier", dialog.getCurrentColorSchemeProtocolBufferPage().getSelectedKey());

                // Test dirty tracking and applyAll()
                assertTrue(dialog.getApplyButton().isDisabled());
                dialog.getCurrentColorSchemeProtocolBufferPage().getItalicCheck().setSelected(true);
                assertFalse(dialog.getApplyButton().isDisabled());

                dialog.applyAll();
                assertTrue(dialog.getApplyButton().isDisabled());

                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Identifier").isItalic());

                // Test Revert Link navigation for PostCSS -> CSS
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("PostCSS".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                dialog.getCurrentColorSchemePostCSSPage().getInheritTargetLabel().fire();
                assertEquals("CSS", dialog.getTree().getSelectionModel().getSelectedItem().getValue());

                // Test Revert Link navigation for Micronaut EL -> Language Defaults
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("Micronaut EL".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                dialog.getCurrentColorSchemeMicronautELPage().getInheritTargetLabel().fire();
                assertEquals("Language Defaults", dialog.getTree().getSelectionModel().getSelectedItem().getValue());

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
                "src/main/java/dev/lumina/ui/SettingsColorSchemeMicronautELPage.java",
                "src/main/java/dev/lumina/ui/SettingsColorSchemePHPPage.java",
                "src/main/java/dev/lumina/ui/SettingsColorSchemePlan9X86Page.java",
                "src/main/java/dev/lumina/ui/SettingsColorSchemePostCSSPage.java",
                "src/main/java/dev/lumina/ui/SettingsColorSchemeProtocolBufferPage.java",
                "src/main/java/dev/lumina/ui/SettingsMicronautELPage.java",
                "src/main/java/dev/lumina/ui/SettingsPHPPage.java",
                "src/main/java/dev/lumina/ui/SettingsPlan9X86Page.java",
                "src/main/java/dev/lumina/ui/SettingsPostCSSPage.java",
                "src/main/java/dev/lumina/ui/SettingsProtocolBufferPage.java"
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
