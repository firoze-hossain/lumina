package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import dev.lumina.settings.EditorColorSchemeSettings.EffectType;
import javafx.application.Platform;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class SettingsColorSchemeBatchElevenTest {

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
    // 1. XSLT Page Tests (media_1790501633444.png)
    // -------------------------------------------------------------------------

    @Test
    void testXSLTPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeXSLTPage page = new SettingsColorSchemeXSLTPage();

                // Has descriptors
                assertEquals(1, EditorColorSchemeSettings.getXsltDescriptors().size());

                // Default selection in screenshot: XSLT Directive
                assertEquals("XSLT Directive", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("XSLT Directive", page.getSelectedKey());

                // Background: checked
                assertTrue(page.getBackgroundCheck().isSelected());

                // Inherit values from checked in screenshot, shows Template language (Language Defaults)
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Template language", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

                // Modification and Apply
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getInheritCheck().setSelected(false);
                page.getBoldCheck().setSelected(true);
                assertTrue(modifiedFired.get());
                assertTrue(page.isModified());

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
            if (error.get() instanceof Exception ex) throw ex;
            throw new RuntimeException(error.get());
        }
    }

    // -------------------------------------------------------------------------
    // 2. YAML Page Tests (media_1790501633442.png)
    // -------------------------------------------------------------------------

    @Test
    void testYAMLPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeYAMLPage page = new SettingsColorSchemeYAMLPage();

                // Has descriptors (9 categories matching screenshot)
                assertEquals(9, EditorColorSchemeSettings.getYamlDescriptors().size());

                // Default selection in screenshot: Comment
                assertEquals("Comment", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Comment", page.getSelectedKey());

                // Font style: Italic is checked
                assertTrue(page.getItalicCheck().isSelected());

                // Foreground: checked
                assertTrue(page.getForegroundCheck().isSelected());

                // Inherit values from: checked, Comments->Doc comment->Text (Language Defaults)
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Comments->Doc comment->Text", page.getInheritTargetLabel().getText());

                // Category selection
                page.selectTreeItem("Key");
                assertEquals("Key", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());

                page.selectTreeItem("Double quoted string");
                assertEquals("Double quoted string", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());

                page.selectTreeItem("Anchor/Alias");
                assertEquals("Anchor/Alias", page.getSelectedKey());

                // Modification and Apply
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getInheritCheck().setSelected(false);
                page.getBoldCheck().setSelected(true);
                assertTrue(modifiedFired.get());
                assertTrue(page.isModified());

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
            if (error.get() instanceof Exception ex) throw ex;
            throw new RuntimeException(error.get());
        }
    }

    // -------------------------------------------------------------------------
    // 3. By Scope Page Tests (media_1790501633440.png)
    // -------------------------------------------------------------------------

    @Test
    void testByScopePage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeByScopePage page = new SettingsColorSchemeByScopePage();

                // Has descriptors (9 scopes matching screenshot)
                assertEquals(9, EditorColorSchemeSettings.getByScopeDescriptors().size());

                // Default selection in screenshot: Project Files
                assertEquals("Project Files", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Project Files", page.getSelectedKey());

                // Manage Scopes... button
                assertNotNull(page.getManageScopesButton());
                assertEquals("Manage Scopes...", page.getManageScopesButton().getText());

                AtomicBoolean manageScopesFired = new AtomicBoolean(false);
                page.setOnManageScopesListener(() -> manageScopesFired.set(true));
                page.getManageScopesButton().fire();
                assertTrue(manageScopesFired.get());

                // Select other scopes
                page.selectTreeItem("All");
                assertEquals("All", page.getSelectedKey());

                page.selectTreeItem("Tests");
                assertEquals("Tests", page.getSelectedKey());

                // Modification and Apply
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getBoldCheck().setSelected(true);
                assertTrue(modifiedFired.get());
                assertTrue(page.isModified());

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
            if (error.get() instanceof Exception ex) throw ex;
            throw new RuntimeException(error.get());
        }
    }

    // -------------------------------------------------------------------------
    // 4. Images Page Tests (media_1790501633439.png)
    // -------------------------------------------------------------------------

    @Test
    void testImagesPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeImagesPage page = new SettingsColorSchemeImagesPage();

                // Has descriptors (4 categories matching screenshot)
                assertEquals(4, EditorColorSchemeSettings.getImagesDescriptors().size());

                // Default selection in screenshot: Background
                assertEquals("Background", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Background", page.getSelectedKey());

                // Background: checked
                assertTrue(page.getBackgroundCheck().isSelected());

                // Effects: dropdown defaults to UNDERSCORED as shown in screenshot
                assertEquals(EffectType.UNDERSCORED, page.getEffectTypeCombo().getValue());

                // Select other categories
                page.selectTreeItem("'Black' cell");
                assertEquals("'Black' cell", page.getSelectedKey());

                page.selectTreeItem("'White' cell");
                assertEquals("'White' cell", page.getSelectedKey());

                page.selectTreeItem("Grid line");
                assertEquals("Grid line", page.getSelectedKey());

                // Modification and Apply
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getBoldCheck().setSelected(true);
                assertTrue(modifiedFired.get());
                assertTrue(page.isModified());

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
            if (error.get() instanceof Exception ex) throw ex;
            throw new RuntimeException(error.get());
        }
    }

    // -------------------------------------------------------------------------
    // 5. Legacy Page Subclasses
    // -------------------------------------------------------------------------

    @Test
    void testLegacyPageSubclasses() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                SettingsXSLTPage xslt = new SettingsXSLTPage();
                assertTrue(xslt instanceof SettingsColorSchemeXSLTPage);

                SettingsYAMLPage yaml = new SettingsYAMLPage();
                assertTrue(yaml instanceof SettingsColorSchemeYAMLPage);

                SettingsByScopePage scope = new SettingsByScopePage();
                assertTrue(scope instanceof SettingsColorSchemeByScopePage);

                SettingsImagesPage images = new SettingsImagesPage();
                assertTrue(images instanceof SettingsColorSchemeImagesPage);
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (error.get() != null) {
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
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                SettingsDialog dialog = new SettingsDialog(stage, "Appearance");

                // Route to XSLT
                dialog.selectCategory("XSLT");
                assertNotNull(dialog.getCurrentColorSchemeXSLTPage());

                // Route to YAML
                dialog.selectCategory("YAML");
                assertNotNull(dialog.getCurrentColorSchemeYAMLPage());

                // Route to By Scope
                dialog.selectCategory("By Scope");
                assertNotNull(dialog.getCurrentColorSchemeByScopePage());

                // Route to Images
                dialog.selectCategory("Images");
                assertNotNull(dialog.getCurrentColorSchemeImagesPage());

                // Apply button tracking
                assertNotNull(dialog.getApplyButton());
                assertTrue(dialog.getApplyButton().isDisable());

                dialog.getCurrentColorSchemeImagesPage().getBoldCheck().setSelected(true);
                assertFalse(dialog.getApplyButton().isDisable());

                dialog.getCurrentColorSchemeImagesPage().reset();
                assertTrue(dialog.getApplyButton().isDisable());

                stage.close();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (error.get() != null) {
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
                Path.of("src/main/java/dev/lumina/ui/SettingsColorSchemeXSLTPage.java"),
                Path.of("src/main/java/dev/lumina/ui/SettingsXSLTPage.java"),
                Path.of("src/main/java/dev/lumina/ui/SettingsColorSchemeYAMLPage.java"),
                Path.of("src/main/java/dev/lumina/ui/SettingsYAMLPage.java"),
                Path.of("src/main/java/dev/lumina/ui/SettingsColorSchemeByScopePage.java"),
                Path.of("src/main/java/dev/lumina/ui/SettingsByScopePage.java"),
                Path.of("src/main/java/dev/lumina/ui/SettingsColorSchemeImagesPage.java"),
                Path.of("src/main/java/dev/lumina/ui/SettingsImagesPage.java")
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
