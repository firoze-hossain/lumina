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

class SettingsColorSchemeBatchTenTest {

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
    // 1. XPath Page Tests (media_1790496620345.png)
    // -------------------------------------------------------------------------

    @Test
    void testXPathPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeXPathPage page = new SettingsColorSchemeXPathPage();

                // Has descriptors
                assertEquals(11, EditorColorSchemeSettings.getXPathDescriptors().size());

                // Default selection in screenshot: Name
                assertEquals("Name", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Name", page.getSelectedKey());

                // Foreground: checked, CC7832
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("CC7832", page.getForegroundSwatch().getText());

                // Inherit values from unchecked in screenshot, shows Text->Default text (General)
                assertFalse(page.getInheritCheck().isSelected());
                assertEquals("Text->Default text", page.getInheritTargetLabel().getText());
                assertEquals("(General)", page.getInheritScopeLabel().getText());

                // Select Function
                page.selectTreeItem("Function");
                assertEquals("Function", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("56A8F5", page.getForegroundSwatch().getText());
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Functions and Methods->Function call", page.getInheritTargetLabel().getText());

                // Preview has code lines
                assertFalse(page.getCodeLinesBox().getChildren().isEmpty());

                // Modification and Apply
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getBoldCheck().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.apply();
                assertFalse(page.isModified());

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
    // 2. TOML Page Tests (media_1790496620346.png)
    // -------------------------------------------------------------------------

    @Test
    void testTomlPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeTOMLPage page = new SettingsColorSchemeTOMLPage();

                // Has descriptors
                assertEquals(8, EditorColorSchemeSettings.getTomlDescriptors().size());

                // Default selection: Invalid (under String // Escape sequence)
                assertEquals("Invalid", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("String // Escape sequence // Invalid", page.getSelectedKey());

                // Foreground: checked, CF8E6D
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("CF8E6D", page.getForegroundSwatch().getText());

                // Inherit values from checked: String->Escape sequence->Invalid (Language Defaults)
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("String->Escape sequence->Invalid", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

                // Select Comments
                page.selectTreeItem("Comments");
                assertEquals("Comments", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("7A7E85", page.getForegroundSwatch().getText());
                assertTrue(page.getItalicCheck().isSelected());
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Comments->Line comment", page.getInheritTargetLabel().getText());

                // Preview has code lines
                assertTrue(page.getCodeLinesBox().getChildren().size() >= 5);

                // Modification and Apply
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getInheritCheck().setSelected(false);
                page.getBoldCheck().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.apply();
                assertFalse(page.isModified());

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
    // 3. Velocity Page Tests (media_1790496620348.png)
    // -------------------------------------------------------------------------

    @Test
    void testVelocityPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeVelocityPage page = new SettingsColorSchemeVelocityPage();

                // Has descriptors
                assertEquals(16, EditorColorSchemeSettings.getVelocityDescriptors().size());

                // Default selection: String literal
                assertEquals("String literal", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("String literal", page.getSelectedKey());

                // Foreground: checked, 6AAB73
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("6AAB73", page.getForegroundSwatch().getText());

                // Inherit values from checked: String->String text (Language Defaults)
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("String->String text", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

                // Select Keyword
                page.selectTreeItem("Keyword");
                assertEquals("Keyword", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("CF8E6D", page.getForegroundSwatch().getText());
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Keyword", page.getInheritTargetLabel().getText());

                // Preview has code lines
                assertTrue(page.getCodeLinesBox().getChildren().size() >= 5);

                // Modification and Apply
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getInheritCheck().setSelected(false);
                page.getBoldCheck().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.apply();
                assertFalse(page.isModified());

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
    // 4. TypeScript Page Tests (media_1790496620349.png)
    // -------------------------------------------------------------------------

    @Test
    void testTypeScriptPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeTypeScriptPage page = new SettingsColorSchemeTypeScriptPage();

                // Has descriptors
                assertEquals(39, EditorColorSchemeSettings.getTypeScriptDescriptors().size());

                // Default selection: Member (under Enum)
                assertEquals("Member", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Enum // Member", page.getSelectedKey());

                // Foreground: checked, C77DBB (inherited from Classes->Static property)
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("C77DBB", page.getForegroundSwatch().getText());

                // Inherit values from checked: Classes->Static property (TypeScript self-scope)
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Classes->Static property", page.getInheritTargetLabel().getText());
                assertEquals("(TypeScript)", page.getInheritScopeLabel().getText());

                // Select Keyword
                page.selectTreeItem("Keyword");
                assertEquals("Keyword", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("CF8E6D", page.getForegroundSwatch().getText());
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Keyword", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

                // Preview has code lines
                assertTrue(page.getCodeLinesBox().getChildren().size() >= 5);

                // Modification and Apply
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getInheritCheck().setSelected(false);
                page.getBoldCheck().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.apply();
                assertFalse(page.isModified());

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
    // 5. XML Page Tests (media_1790496620376.png)
    // -------------------------------------------------------------------------

    @Test
    void testXmlPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeXMLPage page = new SettingsColorSchemeXMLPage();

                // Has descriptors
                assertEquals(12, EditorColorSchemeSettings.getXmlDescriptors().size());

                // Default selection: Entity Reference
                assertEquals("Entity Reference", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Entity Reference", page.getSelectedKey());

                // Foreground: checked, 56A8F5
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("56A8F5", page.getForegroundSwatch().getText());

                // Inherit values from target and scope
                assertFalse(page.getInheritCheck().isSelected());
                assertEquals("Markup->Entity", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

                // Select Tag
                page.selectTreeItem("Tag");
                assertEquals("Tag", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("BCBEC4", page.getForegroundSwatch().getText());
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("XML/HTML->Tag", page.getInheritTargetLabel().getText());

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
    // 6. SettingsDialog Integration
    // -------------------------------------------------------------------------

    @Test
    void testSettingsDialogIntegration() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                SettingsDialog dialog = new SettingsDialog(stage);

                // XPath
                dialog.selectCategory("XPath");
                assertNotNull(dialog.getCurrentColorSchemeXPathPage());

                // TOML
                dialog.selectCategory("TOML");
                assertNotNull(dialog.getCurrentColorSchemeTOMLPage());

                // Velocity
                dialog.selectCategory("Velocity");
                assertNotNull(dialog.getCurrentColorSchemeVelocityPage());

                // TypeScript (disambiguated under Color Scheme)
                dialog.selectCategory("Color Scheme", "TypeScript");
                assertNotNull(dialog.getCurrentColorSchemeTypeScriptPage());

                // XML (disambiguated under Color Scheme)
                dialog.selectCategory("Color Scheme", "XML");
                assertNotNull(dialog.getCurrentColorSchemeXMLPage());

                // Apply button state on modification
                dialog.getCurrentColorSchemeXPathPage().getBoldCheck().setSelected(true);
                assertTrue(dialog.getApplyButton().isVisible());

            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(6, TimeUnit.SECONDS));
        if (error.get() != null) {
            if (error.get() instanceof AssertionError ae) throw ae;
            if (error.get() instanceof Exception ex) throw ex;
            throw new RuntimeException(error.get());
        }
    }

    // -------------------------------------------------------------------------
    // 7. Strict Brand Isolation
    // -------------------------------------------------------------------------

    @Test
    void testStrictBrandIsolation() throws Exception {
        Pattern forbidden = Pattern.compile("(?i)\\b(intellij|jetbrains|idea\\s*community|pycharm|webstorm|rubymine|clion|appcode|datagrip|rider)\\b");

        List<Path> filesToCheck = List.of(
                Path.of("src/main/java/dev/lumina/ui/SettingsColorSchemeXPathPage.java"),
                Path.of("src/main/java/dev/lumina/ui/SettingsXPathPage.java"),
                Path.of("src/main/java/dev/lumina/ui/SettingsColorSchemeTOMLPage.java"),
                Path.of("src/main/java/dev/lumina/ui/SettingsTOMLPage.java"),
                Path.of("src/main/java/dev/lumina/ui/SettingsColorSchemeVelocityPage.java"),
                Path.of("src/main/java/dev/lumina/ui/SettingsVelocityPage.java"),
                Path.of("src/main/java/dev/lumina/ui/SettingsColorSchemeTypeScriptPage.java"),
                Path.of("src/main/java/dev/lumina/ui/SettingsTypeScriptPage.java"),
                Path.of("src/main/java/dev/lumina/ui/SettingsColorSchemeXMLPage.java"),
                Path.of("src/main/java/dev/lumina/ui/SettingsXMLPage.java")
        );

        for (Path p : filesToCheck) {
            assertTrue(Files.exists(p), "File must exist: " + p);
            List<String> lines = Files.readAllLines(p);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                java.util.regex.Matcher m = forbidden.matcher(line);
                assertFalse(m.find(), "Found forbidden competitor brand in " + p + ":" + (i + 1) + ": " + line);
            }
        }
    }
}
