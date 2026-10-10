package dev.lumina.ui;

import dev.lumina.tables.TablesSettings;
import dev.lumina.tables.TablesSettingsManager;
import dev.lumina.templates.TemplateDataLanguageMapping;
import dev.lumina.templates.TemplateDataLanguagesSettings;
import dev.lumina.templates.TemplateDataLanguagesSettingsManager;
import dev.lumina.typescript.*;
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

public class SettingsLanguagesTablesTemplatesTypeScriptTest {

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
    // 1. Tables (Image 1)
    // ============================================================

    @Test
    void testTablesModelAndManager() {
        TablesSettingsManager manager = TablesSettingsManager.getInstance();

        TablesSettings settings = new TablesSettings();
        assertEquals(TablesSettings.MODE_OFF, settings.getColumnStatisticsMode());
        assertTrue(settings.isAutoCompactForSmallTables());
        assertTrue(settings.isDisplayTensorsAsTable());
        assertTrue(settings.isLimitRenderedColumns());
        assertEquals(1200, settings.getMaxRenderedColumns());
        assertFalse(settings.isEnableLocalFiltersByDefault());

        settings.setColumnStatisticsMode(TablesSettings.MODE_COMPACT);
        settings.setAutoCompactForSmallTables(false);
        settings.setDisplayTensorsAsTable(false);
        settings.setLimitRenderedColumns(false);
        settings.setMaxRenderedColumns(500);
        settings.setEnableLocalFiltersByDefault(true);

        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addChangeListener(() -> notified.set(true));

        manager.setSettings(settings);
        assertTrue(notified.get());

        TablesSettings loaded = manager.getSettings();
        assertEquals(TablesSettings.MODE_COMPACT, loaded.getColumnStatisticsMode());
        assertFalse(loaded.isAutoCompactForSmallTables());
        assertFalse(loaded.isDisplayTensorsAsTable());
        assertFalse(loaded.isLimitRenderedColumns());
        assertEquals(500, loaded.getMaxRenderedColumns());
        assertTrue(loaded.isEnableLocalFiltersByDefault());

        // Reset
        manager.setSettings(new TablesSettings());
    }

    @Test
    void testTablesPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsLanguagesTablesPage page = new SettingsLanguagesTablesPage();
            assertFalse(page.isModified());

            page.getStatisticsModeCombo().setValue(TablesSettings.MODE_DETAILED);
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertEquals(TablesSettings.MODE_DETAILED, TablesSettingsManager.getInstance().getSettings().getColumnStatisticsMode());

            page.getMaxColumnsField().setText("2000");
            assertTrue(page.isModified());

            page.revertChanges();
            assertFalse(page.isModified());
            assertEquals(TablesSettings.MODE_DETAILED, page.getStatisticsModeCombo().getValue());

            // Cleanup
            TablesSettingsManager.getInstance().setSettings(new TablesSettings());
            page.reset();
        });
    }

    // ============================================================
    // 2. Template Data Languages (Image 2)
    // ============================================================

    @Test
    void testTemplateDataLanguagesModelAndManager() {
        TemplateDataLanguagesSettingsManager manager = TemplateDataLanguagesSettingsManager.getInstance();

        TemplateDataLanguagesSettings settings = new TemplateDataLanguagesSettings();
        assertEquals("<None>", settings.getProjectLanguage());
        assertTrue(settings.getMappings().isEmpty());

        settings.setProjectLanguage("HTML");
        settings.addMapping(new TemplateDataLanguageMapping("templates/emails", "Velocity"));
        settings.addMapping(new TemplateDataLanguageMapping("templates/web/header.ftl", "FreeMarker"));

        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addChangeListener(() -> notified.set(true));

        manager.setSettings(settings);
        assertTrue(notified.get());

        TemplateDataLanguagesSettings loaded = manager.getSettings();
        assertEquals("HTML", loaded.getProjectLanguage());
        assertEquals(2, loaded.getMappings().size());

        // Resolution tests
        assertEquals("Velocity", manager.resolveLanguage("templates/emails/welcome.vm"));
        assertEquals("FreeMarker", manager.resolveLanguage("templates/web/header.ftl"));
        assertEquals("HTML", manager.resolveLanguage("templates/other/index.html")); // falls back to project language

        // Cleanup
        manager.setSettings(new TemplateDataLanguagesSettings());
    }

    @Test
    void testTemplateDataLanguagesPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsLanguagesTemplateDataLanguagesPage page = new SettingsLanguagesTemplateDataLanguagesPage();
            assertFalse(page.isModified());

            page.getProjectLanguageCombo().setValue("JSON");
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertEquals("JSON", TemplateDataLanguagesSettingsManager.getInstance().getSettings().getProjectLanguage());

            page.getTableData().add(new TemplateDataLanguageMapping("views/home.ftl", "FreeMarker"));
            assertTrue(page.isModified());

            page.revertChanges();
            assertFalse(page.isModified());
            assertTrue(page.getTableData().isEmpty());

            // Cleanup
            TemplateDataLanguagesSettingsManager.getInstance().setSettings(new TemplateDataLanguagesSettings());
            page.reset();
        });
    }

    // ============================================================
    // 3. TypeScript (Image 3)
    // ============================================================

    @Test
    void testTypeScriptModelAndManager() {
        TypeScriptSettingsManager manager = TypeScriptSettingsManager.getInstance();

        TypeScriptSettings settings = new TypeScriptSettings();
        assertEquals(TypeScriptSettings.DEFAULT_NODE, settings.getNodeInterpreter());
        assertEquals(TypeScriptSettings.DEFAULT_TYPESCRIPT, settings.getTypeScriptPackage());
        assertTrue(settings.isUseLanguageService());
        assertTrue(settings.isShowProjectErrors());
        assertTrue(settings.isShowSuggestions());
        assertFalse(settings.isEnableServicePoweredTypeEngine());
        assertFalse(settings.isRecompileOnChanges());
        assertEquals("", settings.getOptions());

        settings.setShowProjectErrors(false);
        settings.setEnableServicePoweredTypeEngine(true);
        settings.setOptions("--max-old-space-size=4096");

        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addChangeListener(() -> notified.set(true));

        manager.setSettings(settings);
        assertTrue(notified.get());

        TypeScriptSettings loaded = manager.getSettings();
        assertFalse(loaded.isShowProjectErrors());
        assertTrue(loaded.isEnableServicePoweredTypeEngine());
        assertEquals("--max-old-space-size=4096", loaded.getOptions());

        List<String> discovered = manager.discoverNodeInterpreters();
        assertNotNull(discovered);
        assertFalse(discovered.isEmpty());

        // Cleanup
        manager.setSettings(new TypeScriptSettings());
    }

    @Test
    void testTypeScriptPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsLanguagesTypeScriptPage page = new SettingsLanguagesTypeScriptPage();
            assertFalse(page.isModified());

            page.getOptionsField().setText("--preserveSymlinks");
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertEquals("--preserveSymlinks", TypeScriptSettingsManager.getInstance().getSettings().getOptions());

            // Disabling language service should disable sub-options
            page.getUseLanguageServiceCheck().setSelected(false);
            assertTrue(page.getShowProjectErrorsCheck().isDisable());
            assertTrue(page.getShowSuggestionsCheck().isDisable());
            assertTrue(page.getEnableTypeEngineCheck().isDisable());
            assertTrue(page.getRecompileOnChangesCheck().isDisable());

            page.revertChanges();
            assertFalse(page.isModified());
            assertTrue(page.getUseLanguageServiceCheck().isSelected());
            assertFalse(page.getShowProjectErrorsCheck().isDisable());

            // Cleanup
            TypeScriptSettingsManager.getInstance().setSettings(new TypeScriptSettings());
            page.reset();
        });
    }

    // ============================================================
    // 4. TypeScript > Angular (Image 4)
    // ============================================================

    @Test
    void testAngularPluginModelAndManager() {
        AngularPluginSettingsManager manager = AngularPluginSettingsManager.getInstance();

        AngularPluginSettings settings = new AngularPluginSettings();
        assertEquals(AngularPluginSettings.MODE_AUTO, settings.getMode());
        assertFalse(settings.isEnableServicePoweredTypeEngine());

        settings.setMode(AngularPluginSettings.MODE_DISABLED);
        settings.setEnableServicePoweredTypeEngine(true);

        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addChangeListener(() -> notified.set(true));

        manager.setSettings(settings);
        assertTrue(notified.get());

        AngularPluginSettings loaded = manager.getSettings();
        assertEquals(AngularPluginSettings.MODE_DISABLED, loaded.getMode());
        assertTrue(loaded.isEnableServicePoweredTypeEngine());

        // Cleanup
        manager.setSettings(new AngularPluginSettings());
    }

    @Test
    void testAngularPluginPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsLanguagesTypeScriptAngularPage page = new SettingsLanguagesTypeScriptAngularPage();
            assertFalse(page.isModified());

            page.getDisabledRadio().setSelected(true);
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertEquals(AngularPluginSettings.MODE_DISABLED, AngularPluginSettingsManager.getInstance().getSettings().getMode());

            page.getEnableTypeEngineCheck().setSelected(true);
            assertTrue(page.isModified());

            page.revertChanges();
            assertFalse(page.isModified());
            assertFalse(page.getEnableTypeEngineCheck().isSelected());

            // Cleanup
            AngularPluginSettingsManager.getInstance().setSettings(new AngularPluginSettings());
            page.reset();
        });
    }

    // ============================================================
    // 5. TypeScript > TSLint (Image 5)
    // ============================================================

    @Test
    void testTsLintModelAndManager() {
        TsLintSettingsManager manager = TsLintSettingsManager.getInstance();

        TsLintSettings settings = new TsLintSettings();
        assertEquals(TsLintSettings.MODE_DISABLE, settings.getMode());
        assertEquals("", settings.getTslintPackage());
        assertEquals("", settings.getConfigFile());
        assertEquals("", settings.getRulesDirectory());

        settings.setMode(TsLintSettings.MODE_AUTOMATIC);
        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addChangeListener(() -> notified.set(true));

        manager.setSettings(settings);
        assertTrue(notified.get());

        TsLintSettings loaded = manager.getSettings();
        assertEquals(TsLintSettings.MODE_AUTOMATIC, loaded.getMode());

        settings.setMode(TsLintSettings.MODE_MANUAL);
        settings.setTslintPackage("/usr/local/lib/node_modules/tslint");
        settings.setConfigFile("/project/tslint.json");
        manager.setSettings(settings);

        loaded = manager.getSettings();
        assertEquals(TsLintSettings.MODE_MANUAL, loaded.getMode());
        assertEquals("/usr/local/lib/node_modules/tslint", loaded.getTslintPackage());
        assertEquals("/project/tslint.json", loaded.getConfigFile());

        // Cleanup
        manager.setSettings(new TsLintSettings());
    }

    @Test
    void testTsLintPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsLanguagesTypeScriptTsLintPage page = new SettingsLanguagesTypeScriptTsLintPage();
            assertFalse(page.isModified());
            assertTrue(page.getDisableRadio().isSelected());
            assertFalse(page.getManualConfigBox().isVisible());

            page.getAutoRadio().setSelected(true);
            assertTrue(page.isModified());
            assertFalse(page.getManualConfigBox().isVisible());

            page.getManualRadio().setSelected(true);
            assertTrue(page.isModified());
            assertTrue(page.getManualConfigBox().isVisible());

            page.getPackageField().setText("/path/to/tslint");
            page.apply();
            assertFalse(page.isModified());
            assertEquals(TsLintSettings.MODE_MANUAL, TsLintSettingsManager.getInstance().getSettings().getMode());
            assertEquals("/path/to/tslint", TsLintSettingsManager.getInstance().getSettings().getTslintPackage());

            page.revertChanges();
            assertFalse(page.isModified());

            // Cleanup
            TsLintSettingsManager.getInstance().setSettings(new TsLintSettings());
            page.reset();
        });
    }

    // ============================================================
    // 6. SettingsDialog Integration and Tree
    // ============================================================

    @Test
    void testSettingsDialogTreeAndRouting() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsDialog dialog = new SettingsDialog(null);
            TreeItem<String> root = dialog.getTree().getRoot();
            assertNotNull(root);

            TreeItem<String> langRoot = null;
            for (TreeItem<String> top : root.getChildren()) {
                if ("Languages & Frameworks".equals(top.getValue())) {
                    langRoot = top;
                    break;
                }
            }
            assertNotNull(langRoot, "Languages & Frameworks tree node should exist");

            // Verify Tables node
            TreeItem<String> tablesItem = null;
            for (TreeItem<String> item : langRoot.getChildren()) {
                if ("Tables".equals(item.getValue())) {
                    tablesItem = item;
                    break;
                }
            }
            assertNotNull(tablesItem, "Tables tree node should exist under Languages & Frameworks");

            // Verify Template Data Languages node
            TreeItem<String> tdlItem = null;
            for (TreeItem<String> item : langRoot.getChildren()) {
                if ("Template Data Languages".equals(item.getValue())) {
                    tdlItem = item;
                    break;
                }
            }
            assertNotNull(tdlItem, "Template Data Languages tree node should exist under Languages & Frameworks");

            // Verify TypeScript node and children (Angular, TSLint, Vue)
            TreeItem<String> typeScriptItem = null;
            for (TreeItem<String> item : langRoot.getChildren()) {
                if ("TypeScript".equals(item.getValue())) {
                    typeScriptItem = item;
                    break;
                }
            }
            assertNotNull(typeScriptItem, "TypeScript tree node should exist under Languages & Frameworks");

            List<String> tsChildren = typeScriptItem.getChildren().stream().map(TreeItem::getValue).toList();
            assertTrue(tsChildren.contains("Angular"), "TypeScript should have Angular child");
            assertTrue(tsChildren.contains("TSLint"), "TypeScript should have TSLint child");
            assertTrue(tsChildren.contains("Vue"), "TypeScript should have Vue child");

            // Test page routing
            dialog.selectCategory("Tables");
            assertNotNull(dialog.getCurrentLanguagesTablesPage(), "Tables page should be built");

            dialog.selectCategory("Template Data Languages");
            assertNotNull(dialog.getCurrentLanguagesTemplateDataLanguagesPage(), "Template Data Languages page should be built");

            dialog.selectCategory("TypeScript");
            assertNotNull(dialog.getCurrentLanguagesTypeScriptPage(), "TypeScript page should be built");

            dialog.selectCategory("Angular");
            assertNotNull(dialog.getCurrentLanguagesTypeScriptAngularPage(), "Angular page should be built");

            dialog.selectCategory("TSLint");
            assertNotNull(dialog.getCurrentLanguagesTypeScriptTsLintPage(), "TSLint page should be built");
        });
    }

    // ============================================================
    // 7. Strict Brand Isolation Test
    // ============================================================

    @Test
    void testStrictBrandIsolation() throws Exception {
        String[] filesToCheck = {
                "src/main/java/dev/lumina/tables/TablesSettings.java",
                "src/main/java/dev/lumina/tables/TablesSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsLanguagesTablesPage.java",
                "src/main/java/dev/lumina/templates/TemplateDataLanguageMapping.java",
                "src/main/java/dev/lumina/templates/TemplateDataLanguagesSettings.java",
                "src/main/java/dev/lumina/templates/TemplateDataLanguagesSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsLanguagesTemplateDataLanguagesPage.java",
                "src/main/java/dev/lumina/typescript/TypeScriptSettings.java",
                "src/main/java/dev/lumina/typescript/TypeScriptSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsLanguagesTypeScriptPage.java",
                "src/main/java/dev/lumina/typescript/AngularPluginSettings.java",
                "src/main/java/dev/lumina/typescript/AngularPluginSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsLanguagesTypeScriptAngularPage.java",
                "src/main/java/dev/lumina/typescript/TsLintSettings.java",
                "src/main/java/dev/lumina/typescript/TsLintSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsLanguagesTypeScriptTsLintPage.java"
        };

        String[] forbidden = {
                "IntelliJ", "intellij",
                "JetBrains", "jetbrains",
                "WebStorm", "webstorm",
                "IDEA"
        };

        for (String filePath : filesToCheck) {
            File f = new File(filePath);
            assertTrue(f.exists(), "File must exist: " + filePath);
            String content = Files.readString(f.toPath());
            for (String brand : forbidden) {
                assertFalse(content.contains(brand),
                        "File " + filePath + " contains forbidden brand reference: " + brand);
            }
        }
    }
}
