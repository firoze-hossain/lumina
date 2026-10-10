package dev.lumina.ui;

import dev.lumina.stylesheets.*;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class SettingsLanguagesStyleSheetsPagesTest {

    private static boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFx() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            try {
                Platform.startup(() -> {
                    javaFxAvailable = true;
                    latch.countDown();
                });
            } catch (IllegalStateException e) {
                javaFxAvailable = true;
                latch.countDown();
            }
            javaFxAvailable = latch.await(5, TimeUnit.SECONDS);
        } catch (Throwable t) {
            javaFxAvailable = false;
        }
    }

    private void runOnFx(Runnable action) throws Exception {
        if (!javaFxAvailable) return;
        CountDownLatch latch = new CountDownLatch(1);
        Throwable[] err = new Throwable[1];
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                err[0] = t;
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX operation timed out");
        if (err[0] != null) {
            if (err[0] instanceof Exception) throw (Exception) err[0];
            throw new RuntimeException(err[0]);
        }
    }

    // ============================================================
    // 1. CSS Dialects (Image 2)
    // ============================================================

    @Test
    void testCssDialectsModelAndManager() {
        CssDialectsSettingsManager manager = CssDialectsSettingsManager.getInstance();

        CssDialectsSettings settings = new CssDialectsSettings();
        assertEquals("<None>", settings.getProjectDialect());
        assertTrue(settings.getMappings().isEmpty());

        settings.setProjectDialect("SCSS");
        settings.addMapping(new CssDialectMapping("src/styles/custom.css", "PostCSS"));

        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addChangeListener(() -> notified.set(true));

        manager.setSettings(settings);
        assertTrue(notified.get());

        CssDialectsSettings loaded = manager.getSettings();
        assertEquals("SCSS", loaded.getProjectDialect());
        assertEquals(1, loaded.getMappings().size());
        assertEquals("src/styles/custom.css", loaded.getMappings().get(0).getPath());
        assertEquals("PostCSS", loaded.getMappings().get(0).getDialect());

        // Dynamic resolution tests
        assertEquals("PostCSS", manager.resolveDialect("src/styles/custom.css"));
        assertEquals("SCSS", manager.resolveDialect("other/file.css")); // falls back to project dialect

        // Extension inference fallback when project is <None>
        settings.setProjectDialect("<None>");
        manager.setSettings(settings);
        assertEquals("Less", manager.resolveDialect("theme.less"));
        assertEquals("Sass", manager.resolveDialect("app.sass"));

        // Reset
        manager.setSettings(new CssDialectsSettings());
    }

    @Test
    void testCssDialectsPageLifecycle() throws Exception {
        runOnFx(() -> {
            SettingsLanguagesStyleSheetsDialectsPage page = new SettingsLanguagesStyleSheetsDialectsPage();
            assertNotNull(page.getProjectDialectCombo());
            assertNotNull(page.getTable());
            assertNotNull(page.getAddBtn());
            assertNotNull(page.getRemoveBtn());
            assertNotNull(page.getEditBtn());

            assertFalse(page.isModified());

            page.getProjectDialectCombo().setValue("SCSS");
            assertTrue(page.isModified());

            page.getTableData().add(new CssDialectMapping("styles/main.css", "Sass"));
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());

            // Revert changes
            page.getProjectDialectCombo().setValue("Less");
            page.revertChanges();
            assertEquals("SCSS", page.getProjectDialectCombo().getValue());
            assertEquals(1, page.getTableData().size());

            // Reset back
            page.getProjectDialectCombo().setValue("<None>");
            page.getTableData().clear();
            page.apply();
        });
    }

    // ============================================================
    // 2. Stylelint (Image 3)
    // ============================================================

    @Test
    void testStylelintModelAndManager() {
        StylelintSettingsManager manager = StylelintSettingsManager.getInstance();

        StylelintSettings settings = new StylelintSettings();
        assertFalse(settings.isEnabled());
        assertEquals("", settings.getPackagePath());
        assertEquals("Auto-detect", settings.getConfigurationFile());
        assertEquals("**/*.{css}", settings.getRunForFiles());
        assertFalse(settings.isFixOnSave());

        settings.setEnabled(true);
        settings.setPackagePath("/node_modules/stylelint");
        settings.setConfigurationFile(".stylelintrc.json");
        settings.setRunForFiles("**/*.{css,scss}");
        settings.setFixOnSave(true);

        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addChangeListener(() -> notified.set(true));

        manager.setSettings(settings);
        assertTrue(notified.get());

        StylelintSettings loaded = manager.getSettings();
        assertTrue(loaded.isEnabled());
        assertEquals("/node_modules/stylelint", loaded.getPackagePath());
        assertEquals(".stylelintrc.json", loaded.getConfigurationFile());
        assertEquals("**/*.{css,scss}", loaded.getRunForFiles());
        assertTrue(loaded.isFixOnSave());

        // Reset
        manager.setSettings(new StylelintSettings());
    }

    @Test
    void testStylelintPageLifecycle() throws Exception {
        runOnFx(() -> {
            SettingsLanguagesStyleSheetsStylelintPage page = new SettingsLanguagesStyleSheetsStylelintPage();
            assertNotNull(page.getEnableCheck());
            assertNotNull(page.getOptionsContainer());
            assertNotNull(page.getPackageField());
            assertNotNull(page.getPackageBrowseBtn());
            assertNotNull(page.getConfigFileField());
            assertNotNull(page.getConfigBrowseBtn());
            assertNotNull(page.getRunForFilesField());
            assertNotNull(page.getGlobLink());
            assertNotNull(page.getFixOnSaveCheck());

            assertFalse(page.isModified());

            // By default not enabled, so optionsContainer is disabled
            assertTrue(page.getOptionsContainer().isDisable());

            // Enable
            page.getEnableCheck().setSelected(true);
            assertFalse(page.getOptionsContainer().isDisable());
            assertTrue(page.isModified());

            page.getPackageField().setText("/usr/local/lib/node_modules/stylelint");
            page.getConfigFileField().setText("/etc/stylelint.json");
            page.getFixOnSaveCheck().setSelected(true);

            page.apply();
            assertFalse(page.isModified());

            // Revert changes
            page.getFixOnSaveCheck().setSelected(false);
            page.revertChanges();
            assertTrue(page.getFixOnSaveCheck().isSelected());

            // Cleanup & reset
            page.getEnableCheck().setSelected(false);
            page.apply();
        });
    }

    // ============================================================
    // 3. Tailwind CSS (Images 4 & 5)
    // ============================================================

    @Test
    void testTailwindCssModelAndManager() {
        TailwindCssSettingsManager manager = TailwindCssSettingsManager.getInstance();

        TailwindCssSettings settings = new TailwindCssSettings();
        assertEquals(TailwindCssSettings.DEFAULT_SERVER, settings.getLanguageServer());
        assertEquals(TailwindCssSettings.DEFAULT_VERSION, settings.getLanguageServerVersion());
        assertNotNull(settings.getConfigurationJson());
        assertTrue(settings.getConfigurationJson().contains("\"includeLanguages\""));
        assertTrue(settings.getConfigurationJson().contains("\"files\""));
        assertTrue(settings.getConfigurationJson().contains("\"emmetCompletions\""));

        settings.setLanguageServer("/custom/tailwind-lsp");
        settings.setConfigurationJson("{\n  \"emmetCompletions\": true\n}");

        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addChangeListener(() -> notified.set(true));

        manager.setSettings(settings);
        assertTrue(notified.get());

        TailwindCssSettings loaded = manager.getSettings();
        assertEquals("/custom/tailwind-lsp", loaded.getLanguageServer());
        assertTrue(loaded.getConfigurationJson().contains("\"emmetCompletions\": true"));

        // Reset
        manager.setSettings(new TailwindCssSettings());
    }

    @Test
    void testTailwindCssPageLifecycle() throws Exception {
        runOnFx(() -> {
            SettingsLanguagesStyleSheetsTailwindPage page = new SettingsLanguagesStyleSheetsTailwindPage();
            assertNotNull(page.getLanguageServerCombo());
            assertNotNull(page.getBrowseServerBtn());
            assertNotNull(page.getSeeOptionsLink());
            assertNotNull(page.getConfigurationEditor());

            assertFalse(page.isModified());

            assertEquals("@tailwindcss/language-server (Default)", page.getLanguageServerCombo().getValue());
            assertTrue(page.getConfigurationEditor().getText().contains("\"includeLanguages\""));

            // Modify configuration
            String modifiedConfig = page.getConfigurationEditor().getText() + "\n// modified";
            page.getConfigurationEditor().setText(modifiedConfig);
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());

            // Revert
            page.getConfigurationEditor().setText("changed text");
            page.revertChanges();
            assertEquals(modifiedConfig, page.getConfigurationEditor().getText());

            // Restore defaults
            page.getConfigurationEditor().setText(TailwindCssSettings.DEFAULT_CONFIG_JSON);
            page.apply();
        });
    }

    // ============================================================
    // 4. SettingsDialog Tree & Routing (Images 1, 2, 3, 4)
    // ============================================================

    @Test
    void testSettingsDialogStyleSheetsTreeAndRouting() throws Exception {
        runOnFx(() -> {
            SettingsDialog dialog = new SettingsDialog(null);
            TreeItem<String> root = dialog.getTree().getRoot();
            assertNotNull(root);

            TreeItem<String> languages = findTreeItem(root, "Languages & Frameworks");
            assertNotNull(languages);

            TreeItem<String> styleSheets = findTreeItem(languages, "Style Sheets");
            assertNotNull(styleSheets, "Style Sheets item must exist under Languages & Frameworks");
            assertEquals(3, styleSheets.getChildren().size(), "Style Sheets must have exactly 3 sub-pages");

            TreeItem<String> dialectsItem = findTreeItem(styleSheets, "Dialects");
            assertNotNull(dialectsItem, "Dialects must exist under Style Sheets");

            TreeItem<String> stylelintItem = findTreeItem(styleSheets, "Stylelint");
            assertNotNull(stylelintItem, "Stylelint must exist under Style Sheets");

            TreeItem<String> tailwindItem = findTreeItem(styleSheets, "Tailwind CSS");
            assertNotNull(tailwindItem, "Tailwind CSS must exist under Style Sheets");

            // Navigation to Style Sheets parent should show overview with 3 links (Image 1)
            dialog.getTree().getSelectionModel().select(styleSheets);

            // Navigate to Dialects (Image 2)
            dialog.getTree().getSelectionModel().select(dialectsItem);
            assertNotNull(dialog.getCurrentLanguagesStyleSheetsDialectsPage(), "Dialects page must be constructed");

            // Navigate to Stylelint (Image 3)
            dialog.getTree().getSelectionModel().select(stylelintItem);
            assertNotNull(dialog.getCurrentLanguagesStyleSheetsStylelintPage(), "Stylelint page must be constructed");

            // Navigate to Tailwind CSS (Image 4)
            dialog.getTree().getSelectionModel().select(tailwindItem);
            assertNotNull(dialog.getCurrentLanguagesStyleSheetsTailwindPage(), "Tailwind CSS page must be constructed");
        });
    }

    private TreeItem<String> findTreeItem(TreeItem<String> current, String value) {
        if (current == null) return null;
        if (value.equals(current.getValue())) return current;
        for (TreeItem<String> child : current.getChildren()) {
            TreeItem<String> found = findTreeItem(child, value);
            if (found != null) return found;
        }
        return null;
    }

    // ============================================================
    // 5. Strict Brand Isolation Check
    // ============================================================

    @Test
    void testStrictBrandIsolation() throws Exception {
        List<String> filesToCheck = List.of(
                "src/main/java/dev/lumina/stylesheets/CssDialectMapping.java",
                "src/main/java/dev/lumina/stylesheets/CssDialectsSettings.java",
                "src/main/java/dev/lumina/stylesheets/CssDialectsSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsLanguagesStyleSheetsDialectsPage.java",
                "src/main/java/dev/lumina/stylesheets/StylelintSettings.java",
                "src/main/java/dev/lumina/stylesheets/StylelintSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsLanguagesStyleSheetsStylelintPage.java",
                "src/main/java/dev/lumina/stylesheets/TailwindCssSettings.java",
                "src/main/java/dev/lumina/stylesheets/TailwindCssSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsLanguagesStyleSheetsTailwindPage.java"
        );

        for (String filePath : filesToCheck) {
            File f = new File(filePath);
            assertTrue(f.exists(), "File must exist: " + filePath);
            String content = Files.readString(f.toPath()).toLowerCase();

            assertFalse(content.contains("intellij"), "File " + filePath + " must not contain 'intellij'");
            assertFalse(content.contains("jetbrains"), "File " + filePath + " must not contain 'jetbrains'");
            assertFalse(content.matches("(?s).*\\bidea\\b.*"), "File " + filePath + " must not contain standalone word 'idea'");
        }
    }
}
