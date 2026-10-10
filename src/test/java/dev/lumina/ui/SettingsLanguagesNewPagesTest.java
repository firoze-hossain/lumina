package dev.lumina.ui;

import dev.lumina.schemas.RemoteJsonSchemasSettings;
import dev.lumina.schemas.RemoteJsonSchemasSettingsManager;
import dev.lumina.schemas.XmlCatalogSettings;
import dev.lumina.schemas.XmlCatalogSettingsManager;
import dev.lumina.spring.SpringSettings;
import dev.lumina.spring.SpringSettingsManager;
import dev.lumina.sql.SqlDialectMapping;
import dev.lumina.sql.SqlDialectsSettings;
import dev.lumina.sql.SqlDialectsSettingsManager;
import dev.lumina.sql.SqlResolutionScopeMapping;
import dev.lumina.sql.SqlResolutionScopesSettings;
import dev.lumina.sql.SqlResolutionScopesSettingsManager;
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

public class SettingsLanguagesNewPagesTest {

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
    // 1. Remote JSON Schemas
    // ============================================================

    @Test
    void testRemoteJsonSchemasModelAndManager() {
        RemoteJsonSchemasSettingsManager manager = RemoteJsonSchemasSettingsManager.getInstance();

        RemoteJsonSchemasSettings settings = new RemoteJsonSchemasSettings();
        assertTrue(settings.isAllowDownloadRemoteSchemas());
        assertTrue(settings.isUseSchemaStoreCatalog());
        assertFalse(settings.isAlwaysDownloadMostRecentVersion());
        assertEquals("https://www.schemastore.org/api/json/catalog.json", settings.getSchemaStoreCatalogUrl());

        settings.setAllowDownloadRemoteSchemas(false);
        settings.setUseSchemaStoreCatalog(false);
        settings.setAlwaysDownloadMostRecentVersion(true);

        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addChangeListener(() -> notified.set(true));

        manager.setSettings(settings);
        assertTrue(notified.get());

        RemoteJsonSchemasSettings loaded = manager.getSettings();
        assertFalse(loaded.isAllowDownloadRemoteSchemas());
        assertFalse(loaded.isUseSchemaStoreCatalog());
        assertTrue(loaded.isAlwaysDownloadMostRecentVersion());

        // Restore defaults
        settings.setAllowDownloadRemoteSchemas(true);
        settings.setUseSchemaStoreCatalog(true);
        settings.setAlwaysDownloadMostRecentVersion(false);
        manager.setSettings(settings);
    }

    @Test
    void testRemoteJsonSchemasPageLifecycle() throws Exception {
        runOnFx(() -> {
            SettingsLanguagesRemoteJsonSchemasPage page = new SettingsLanguagesRemoteJsonSchemasPage();
            assertNotNull(page.getAllowDownloadCheck());
            assertNotNull(page.getUseSchemaStoreCheck());
            assertNotNull(page.getAlwaysDownloadMostRecentCheck());
            assertNotNull(page.getSchemaStoreApiLink());

            assertFalse(page.isModified());

            // Toggle allow download
            page.getAllowDownloadCheck().setSelected(false);
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());

            // Revert / reset
            page.getAllowDownloadCheck().setSelected(true);
            page.revertChanges();
            assertFalse(page.getAllowDownloadCheck().isSelected());

            // Reset back to true and apply
            page.getAllowDownloadCheck().setSelected(true);
            page.apply();
            assertTrue(page.getAllowDownloadCheck().isSelected());
        });
    }

    // ============================================================
    // 2. XML Catalog
    // ============================================================

    @Test
    void testXmlCatalogModelAndManager() {
        XmlCatalogSettingsManager manager = XmlCatalogSettingsManager.getInstance();

        XmlCatalogSettings settings = new XmlCatalogSettings();
        assertEquals("", settings.getCatalogPropertyFile());

        settings.setCatalogPropertyFile("/path/to/catalog.xml");
        assertEquals("/path/to/catalog.xml", settings.getCatalogPropertyFile());

        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addChangeListener(() -> notified.set(true));

        manager.setSettings(settings);
        assertTrue(notified.get());

        XmlCatalogSettings loaded = manager.getSettings();
        assertEquals("/path/to/catalog.xml", loaded.getCatalogPropertyFile());

        // Reset
        settings.setCatalogPropertyFile("");
        manager.setSettings(settings);
    }

    @Test
    void testXmlCatalogPageLifecycle() throws Exception {
        runOnFx(() -> {
            SettingsLanguagesXmlCatalogPage page = new SettingsLanguagesXmlCatalogPage();
            assertNotNull(page.getCatalogPropertyField());
            assertNotNull(page.getBrowseBtn());

            assertFalse(page.isModified());

            page.getCatalogPropertyField().setText("/etc/xml/catalog.properties");
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertEquals("/etc/xml/catalog.properties", page.getCurrentSettings().getCatalogPropertyFile());

            page.getCatalogPropertyField().setText("/other/catalog.xml");
            page.revertChanges();
            assertEquals("/etc/xml/catalog.properties", page.getCatalogPropertyField().getText());

            // Cleanup
            page.getCatalogPropertyField().setText("");
            page.apply();
        });
    }

    // ============================================================
    // 3. Spring
    // ============================================================

    @Test
    void testSpringModelAndManager() {
        SpringSettingsManager manager = SpringSettingsManager.getInstance();

        SpringSettings settings = new SpringSettings();
        assertTrue(settings.isShowProfilesPanel());
        assertTrue(settings.isShowMultipleContextsPanel());
        assertTrue(settings.isSmartBeansCompletion());
        assertTrue(settings.isReformatCodeWhenCreatingProject());
        assertEquals(SpringSettings.BeanInjectorStrategy.CONSTRUCTOR, settings.getDefaultBeanInjectorStrategy());
        assertTrue(settings.isUseMethodParameterInjectionForBeanMethods());
        assertTrue(settings.isRefreshHealthInActuatorTab());
        assertEquals(15, settings.getActuatorRefreshIntervalSeconds());
        assertTrue(settings.isCreateRunConfigurationAutomatically());

        settings.setShowProfilesPanel(false);
        settings.setDefaultBeanInjectorStrategy(SpringSettings.BeanInjectorStrategy.FIELD);
        settings.setActuatorRefreshIntervalSeconds(30);

        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addChangeListener(() -> notified.set(true));

        manager.setSettings(settings);
        assertTrue(notified.get());

        SpringSettings loaded = manager.getSettings();
        assertFalse(loaded.isShowProfilesPanel());
        assertEquals(SpringSettings.BeanInjectorStrategy.FIELD, loaded.getDefaultBeanInjectorStrategy());
        assertEquals(30, loaded.getActuatorRefreshIntervalSeconds());

        // Reset
        SpringSettings def = new SpringSettings();
        manager.setSettings(def);
    }

    @Test
    void testSpringPageLifecycle() throws Exception {
        runOnFx(() -> {
            SettingsLanguagesSpringPage page = new SettingsLanguagesSpringPage();
            assertNotNull(page.getShowProfilesCheck());
            assertNotNull(page.getShowContextsCheck());
            assertNotNull(page.getSmartBeansCheck());
            assertNotNull(page.getReformatCodeCheck());
            assertNotNull(page.getConstructorRadio());
            assertNotNull(page.getFieldRadio());
            assertNotNull(page.getSetterRadio());
            assertNotNull(page.getMethodParamInjectionCheck());
            assertNotNull(page.getRefreshHealthActuatorCheck());
            assertNotNull(page.getActuatorRefreshIntervalSpinner());
            assertNotNull(page.getCreateRunConfigCheck());

            assertFalse(page.isModified());

            // Modify strategy
            page.getFieldRadio().setSelected(true);
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertEquals(SpringSettings.BeanInjectorStrategy.FIELD, page.getCurrentSettings().getDefaultBeanInjectorStrategy());

            // Revert changes
            page.getConstructorRadio().setSelected(true);
            page.revertChanges();
            assertTrue(page.getFieldRadio().isSelected());

            // Reset back to Constructor and apply
            page.getConstructorRadio().setSelected(true);
            page.apply();
            assertTrue(page.getConstructorRadio().isSelected());
        });
    }

    // ============================================================
    // 4. SQL Dialects
    // ============================================================

    @Test
    void testSqlDialectsModelAndManager() {
        SqlDialectsSettingsManager manager = SqlDialectsSettingsManager.getInstance();

        SqlDialectsSettings settings = new SqlDialectsSettings();
        assertEquals("<None>", settings.getGlobalDialect());
        assertEquals("<None>", settings.getProjectDialect());
        assertTrue(settings.getMappings().isEmpty());

        settings.setGlobalDialect("PostgreSQL");
        settings.setProjectDialect("MySQL");
        settings.addMapping(new SqlDialectMapping("src/main/resources/schema.sql", "Oracle"));

        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addChangeListener(() -> notified.set(true));

        manager.setSettings(settings);
        assertTrue(notified.get());

        SqlDialectsSettings loaded = manager.getSettings();
        assertEquals("PostgreSQL", loaded.getGlobalDialect());
        assertEquals("MySQL", loaded.getProjectDialect());
        assertEquals(1, loaded.getMappings().size());
        assertEquals("src/main/resources/schema.sql", loaded.getMappings().get(0).getPath());
        assertEquals("Oracle", loaded.getMappings().get(0).getDialect());

        // Test dynamic resolution hierarchy
        assertEquals("Oracle", manager.resolveDialect("src/main/resources/schema.sql"));
        assertEquals("MySQL", manager.resolveDialect("other/file.sql")); // falls back to project
        settings.setProjectDialect("<None>");
        manager.setSettings(settings);
        assertEquals("PostgreSQL", manager.resolveDialect("other/file.sql")); // falls back to global

        // Reset
        manager.setSettings(new SqlDialectsSettings());
    }

    @Test
    void testSqlDialectsPageLifecycle() throws Exception {
        runOnFx(() -> {
            SettingsLanguagesSqlDialectsPage page = new SettingsLanguagesSqlDialectsPage();
            assertNotNull(page.getGlobalDialectCombo());
            assertNotNull(page.getProjectDialectCombo());
            assertNotNull(page.getTable());
            assertNotNull(page.getAddBtn());
            assertNotNull(page.getRemoveBtn());
            assertNotNull(page.getEditBtn());

            assertFalse(page.isModified());

            // Change global dialect
            page.getGlobalDialectCombo().setValue("PostgreSQL");
            assertTrue(page.isModified());

            // Add table mapping
            page.getTableData().add(new SqlDialectMapping("/db/init.sql", "PostgreSQL"));
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());

            // Revert changes
            page.getGlobalDialectCombo().setValue("MySQL");
            page.revertChanges();
            assertEquals("PostgreSQL", page.getGlobalDialectCombo().getValue());
            assertEquals(1, page.getTableData().size());

            // Reset back
            page.getGlobalDialectCombo().setValue("<None>");
            page.getProjectDialectCombo().setValue("<None>");
            page.getTableData().clear();
            page.apply();
        });
    }

    // ============================================================
    // 5. SQL Resolution Scopes
    // ============================================================

    @Test
    void testSqlResolutionScopesModelAndManager() {
        SqlResolutionScopesSettingsManager manager = SqlResolutionScopesSettingsManager.getInstance();

        SqlResolutionScopesSettings settings = new SqlResolutionScopesSettings();
        assertEquals("<Default>", settings.getProjectMapping());
        assertTrue(settings.getMappings().isEmpty());

        settings.setProjectMapping("<All Data Sources>");
        settings.addMapping(new SqlResolutionScopeMapping("src/test/sql", "TestDB.public"));

        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addChangeListener(() -> notified.set(true));

        manager.setSettings(settings);
        assertTrue(notified.get());

        SqlResolutionScopesSettings loaded = manager.getSettings();
        assertEquals("<All Data Sources>", loaded.getProjectMapping());
        assertEquals(1, loaded.getMappings().size());
        assertEquals("src/test/sql", loaded.getMappings().get(0).getPath());
        assertEquals("TestDB.public", loaded.getMappings().get(0).getResolutionScope());

        // Test dynamic resolution hierarchy
        assertEquals("TestDB.public", manager.resolveScope("src/test/sql/query.sql"));
        assertEquals("<All Data Sources>", manager.resolveScope("other/query.sql"));

        // Reset
        manager.setSettings(new SqlResolutionScopesSettings());
    }

    @Test
    void testSqlResolutionScopesPageLifecycle() throws Exception {
        runOnFx(() -> {
            SettingsLanguagesSqlResolutionScopesPage page = new SettingsLanguagesSqlResolutionScopesPage();
            assertNotNull(page.getProjectMappingCombo());
            assertNotNull(page.getTable());
            assertNotNull(page.getAddBtn());
            assertNotNull(page.getRemoveBtn());
            assertNotNull(page.getEditBtn());

            assertFalse(page.isModified());

            page.getProjectMappingCombo().setValue("<All Data Sources>");
            assertTrue(page.isModified());

            page.getTableData().add(new SqlResolutionScopeMapping("/db/migration", "ProdDB"));
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());

            // Revert
            page.getProjectMappingCombo().setValue("<None>");
            page.revertChanges();
            assertEquals("<All Data Sources>", page.getProjectMappingCombo().getValue());

            // Reset back
            page.getProjectMappingCombo().setValue("<Default>");
            page.getTableData().clear();
            page.apply();
        });
    }

    // ============================================================
    // 6. SettingsDialog Navigation & Routing
    // ============================================================

    @Test
    void testSettingsDialogRoutingAndTreeNodes() throws Exception {
        runOnFx(() -> {
            SettingsDialog dialog = new SettingsDialog(null);
            TreeItem<String> root = dialog.getTree().getRoot();
            assertNotNull(root);

            TreeItem<String> languages = findTreeItem(root, "Languages & Frameworks");
            assertNotNull(languages, "Languages & Frameworks node must exist");

            TreeItem<String> schemas = findTreeItem(languages, "Schemas and DTDs");
            assertNotNull(schemas, "Schemas and DTDs node must exist");

            TreeItem<String> remoteJson = findTreeItem(schemas, "Remote JSON Schemas");
            assertNotNull(remoteJson, "Remote JSON Schemas node must exist");

            TreeItem<String> xmlCatalog = findTreeItem(schemas, "XML Catalog");
            assertNotNull(xmlCatalog, "XML Catalog node must exist");

            TreeItem<String> spring = findTreeItem(languages, "Spring");
            assertNotNull(spring, "Spring node must exist");

            TreeItem<String> sqlDialects = findTreeItem(languages, "SQL Dialects");
            assertNotNull(sqlDialects, "SQL Dialects node must exist");

            TreeItem<String> sqlScopes = findTreeItem(languages, "SQL Resolution Scopes");
            assertNotNull(sqlScopes, "SQL Resolution Scopes node must exist");

            // Navigate to each page and ensure proper page instance is created
            dialog.getTree().getSelectionModel().select(remoteJson);
            assertNotNull(dialog.getCurrentLanguagesRemoteJsonSchemasPage());

            dialog.getTree().getSelectionModel().select(xmlCatalog);
            assertNotNull(dialog.getCurrentLanguagesXmlCatalogPage());

            dialog.getTree().getSelectionModel().select(spring);
            assertNotNull(dialog.getCurrentLanguagesSpringPage());

            dialog.getTree().getSelectionModel().select(sqlDialects);
            assertNotNull(dialog.getCurrentLanguagesSqlDialectsPage());

            dialog.getTree().getSelectionModel().select(sqlScopes);
            assertNotNull(dialog.getCurrentLanguagesSqlResolutionScopesPage());
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
    // 7. Strict Brand Isolation Check
    // ============================================================

    @Test
    void testStrictBrandIsolation() throws Exception {
        List<String> filesToCheck = List.of(
                "src/main/java/dev/lumina/schemas/RemoteJsonSchemasSettings.java",
                "src/main/java/dev/lumina/schemas/RemoteJsonSchemasSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsLanguagesRemoteJsonSchemasPage.java",
                "src/main/java/dev/lumina/schemas/XmlCatalogSettings.java",
                "src/main/java/dev/lumina/schemas/XmlCatalogSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsLanguagesXmlCatalogPage.java",
                "src/main/java/dev/lumina/spring/SpringSettings.java",
                "src/main/java/dev/lumina/spring/SpringSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsLanguagesSpringPage.java",
                "src/main/java/dev/lumina/sql/SqlDialectMapping.java",
                "src/main/java/dev/lumina/sql/SqlDialectsSettings.java",
                "src/main/java/dev/lumina/sql/SqlDialectsSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsLanguagesSqlDialectsPage.java",
                "src/main/java/dev/lumina/sql/SqlResolutionScopeMapping.java",
                "src/main/java/dev/lumina/sql/SqlResolutionScopesSettings.java",
                "src/main/java/dev/lumina/sql/SqlResolutionScopesSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsLanguagesSqlResolutionScopesPage.java"
        );

        for (String filePath : filesToCheck) {
            File f = new File(filePath);
            assertTrue(f.exists(), "File must exist: " + filePath);
            String content = Files.readString(f.toPath()).toLowerCase();

            assertFalse(content.contains("intellij"), "File " + filePath + " must not contain 'intellij'");
            assertFalse(content.contains("jetbrains"), "File " + filePath + " must not contain 'jetbrains'");
            // Check standalone 'idea' word
            assertFalse(content.matches("(?s).*\\bidea\\b.*"), "File " + filePath + " must not contain standalone word 'idea'");
        }
    }
}
