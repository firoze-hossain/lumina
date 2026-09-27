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

class SettingsColorSchemeFivePagesBatchTest {

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
    // 1. Shell Script Page Tests (media_1790482662031.png)
    // -------------------------------------------------------------------------

    @Test
    void testShellScriptPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeShellScriptPage page = new SettingsColorSchemeShellScriptPage();

                // Has descriptors
                assertTrue(EditorColorSchemeSettings.getShellScriptDescriptors().size() >= 23);

                // Default selection: Parentheses (under Braces)
                assertEquals("Parentheses", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Braces // Parentheses", page.getSelectedKey());

                // Foreground: checked, BCBEC4
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("BCBEC4", page.getForegroundSwatch().getText());

                // Inherit values from checked: Braces and Operators->Parentheses (Language Defaults)
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Braces and Operators->Parentheses", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

                // Select Shebang comment
                page.selectTreeItem("Shebang comment");
                assertEquals("Shebang comment", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("7A7E85", page.getForegroundSwatch().getText());

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
    // 2. Table Diff Page Tests (media_1790482662636.png)
    // -------------------------------------------------------------------------

    @Test
    void testTableDiffPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeTableDiffPage page = new SettingsColorSchemeTableDiffPage();

                // 3 items in flat tree
                assertEquals(3, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Fuzzy match - mismatched
                assertEquals("Fuzzy match - mismatched", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Fuzzy match - mismatched", page.getSelectedKey());

                // Background: checked, 114957
                assertTrue(page.getBackgroundCheck().isSelected());
                assertEquals("114957", page.getBackgroundSwatch().getText());

                // Error stripe: checked, 72D6D6
                assertTrue(page.getErrorStripeCheck().isSelected());
                assertEquals("72D6D6", page.getErrorStripeSwatch().getText());

                // Effects: checked, 165E70
                assertTrue(page.getEffectsCheck().isSelected());
                assertEquals("165E70", page.getEffectsSwatch().getText());

                // Inherit values from checked: Search Results->Text search result (General)
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Search Results->Text search result", page.getInheritTargetLabel().getText());
                assertEquals("(General)", page.getInheritScopeLabel().getText());

                // Select Excluded from diff
                page.selectTreeItem("Excluded from diff");
                assertEquals("Excluded from diff", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("7A7E85", page.getForegroundSwatch().getText());
                assertTrue(page.getBackgroundCheck().isSelected());
                assertEquals("2A2D33", page.getBackgroundSwatch().getText());

                // Table preview and error stripe gutter bar
                assertTrue(page.getTableContainer().getChildren().size() >= 4);
                assertTrue(page.getErrorStripeBar().getChildren().size() >= 2);

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
    // 3. SQL Page Tests (media_1790482663944.png)
    // -------------------------------------------------------------------------

    @Test
    void testSqlPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeSQLPage page = new SettingsColorSchemeSQLPage();

                // 24 items in flat tree
                assertEquals(24, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Column
                assertEquals("Column", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Column", page.getSelectedKey());

                // Foreground: checked, C77DBB
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("C77DBB", page.getForegroundSwatch().getText());

                // Inherit values from checked: Classes->Instance field (Language Defaults)
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Classes->Instance field", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

                // Select Keyword
                page.selectTreeItem("Keyword");
                assertEquals("Keyword", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("CF8E6D", page.getForegroundSwatch().getText());

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
    // 4. Smarty Page Tests (media_1790482664908.png)
    // -------------------------------------------------------------------------

    @Test
    void testSmartyPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeSmartyPage page = new SettingsColorSchemeSmartyPage();

                // 9 items in flat tree
                assertEquals(9, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Keyword
                assertEquals("Keyword", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Keyword", page.getSelectedKey());

                // Foreground: checked, CF8E6D
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("CF8E6D", page.getForegroundSwatch().getText());

                // Inherit values from checked: Keyword (Language Defaults)
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Keyword", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

                // Select Comment
                page.selectTreeItem("Comment");
                assertEquals("Comment", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("7A7E85", page.getForegroundSwatch().getText());

                // Preview has code lines
                assertTrue(page.getCodeLinesBox().getChildren().size() >= 8);

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
    // 5. Spring EL Page Tests (media_1790482666900.png)
    // -------------------------------------------------------------------------

    @Test
    void testSpringELPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeSpringELPage page = new SettingsColorSchemeSpringELPage();

                // Has descriptors
                assertTrue(EditorColorSchemeSettings.getSpringELDescriptors().size() >= 18);

                // Default selection: Method call
                assertEquals("Method call", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Method call", page.getSelectedKey());

                // Foreground: checked, 56A8F5
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("56A8F5", page.getForegroundSwatch().getText());

                // Inherit values from checked: Classes->Instance method (Language Defaults)
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Classes->Instance method", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

                // Select Bean prefix
                page.selectTreeItem("Bean prefix");
                assertEquals("Bean prefix", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("E0C46C", page.getForegroundSwatch().getText());

                // Preview has code lines
                assertTrue(page.getCodeLinesBox().getChildren().size() >= 7);

                // Modification and Apply
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getItalicCheck().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.apply();
                assertFalse(page.isModified());

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
    // 6. SettingsDialog Integration & Legacy Wrappers
    // -------------------------------------------------------------------------

    @Test
    void testSettingsDialogIntegrationAndLegacyWrappers() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                // Test legacy class instantiation and inheritance
                SettingsShellScriptPage legacyShell = new SettingsShellScriptPage();
                assertNotNull(legacyShell);
                assertTrue(legacyShell instanceof SettingsColorSchemeShellScriptPage);

                SettingsTableDiffPage legacyTable = new SettingsTableDiffPage();
                assertNotNull(legacyTable);
                assertTrue(legacyTable instanceof SettingsColorSchemeTableDiffPage);

                SettingsSQLPage legacySql = new SettingsSQLPage();
                assertNotNull(legacySql);
                assertTrue(legacySql instanceof SettingsColorSchemeSQLPage);

                SettingsSpringELPage legacySpring = new SettingsSpringELPage();
                assertNotNull(legacySpring);
                assertTrue(legacySpring instanceof SettingsColorSchemeSpringELPage);

                // Test SettingsDialog
                SettingsDialog dialog = new SettingsDialog(new Stage());

                // Check category selection and routing
                dialog.selectCategory("Shell Script");
                assertNotNull(dialog.getCurrentColorSchemeShellScriptPage());

                dialog.selectCategory("Smarty");
                assertNotNull(dialog.getCurrentColorSchemeSmartyPage());

                dialog.selectCategory("Spring EL");
                assertNotNull(dialog.getCurrentColorSchemeSpringELPage());

                dialog.selectCategory("Color Scheme", "SQL");
                assertNotNull(dialog.getCurrentColorSchemeSQLPage());

                dialog.selectCategory("Table Diff");
                assertNotNull(dialog.getCurrentColorSchemeTableDiffPage());

                // Apply button state and applyAll
                dialog.getCurrentColorSchemeShellScriptPage().getBoldCheck().setSelected(true);
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
                Path.of("src/main/java/dev/lumina/ui/SettingsColorSchemeShellScriptPage.java"),
                Path.of("src/main/java/dev/lumina/ui/SettingsShellScriptPage.java"),
                Path.of("src/main/java/dev/lumina/ui/SettingsColorSchemeTableDiffPage.java"),
                Path.of("src/main/java/dev/lumina/ui/SettingsTableDiffPage.java"),
                Path.of("src/main/java/dev/lumina/ui/SettingsColorSchemeSQLPage.java"),
                Path.of("src/main/java/dev/lumina/ui/SettingsSQLPage.java"),
                Path.of("src/main/java/dev/lumina/ui/SettingsColorSchemeSmartyPage.java"),
                Path.of("src/main/java/dev/lumina/ui/SettingsColorSchemeSpringELPage.java"),
                Path.of("src/main/java/dev/lumina/ui/SettingsSpringELPage.java")
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
