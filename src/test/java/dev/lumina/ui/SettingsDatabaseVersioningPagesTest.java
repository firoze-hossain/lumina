package dev.lumina.ui;

import dev.lumina.database.versioning.*;
import dev.lumina.tools.*;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class SettingsDatabaseVersioningPagesTest {

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

    @Test
    void testDatabaseVersioningSettingsAndManager() {
        DatabaseVersioningSettingsManager manager = DatabaseVersioningSettingsManager.getInstance();
        assertNotNull(manager);

        DatabaseVersioningSettings original = manager.getSettings();
        assertNotNull(original);

        DatabaseVersioningSettings custom = new DatabaseVersioningSettings();
        custom.setPhysicalNamingStrategy("CamelCaseToUnderscoresNamingStrategy");
        custom.setSequenceNamingStrategy("Single sequence for all entities");
        custom.setMaxDbIdentifierLength(63);
        custom.setCreateIndexForAssociationFk(true);
        custom.setPrimaryKeyConstraintNamed(true);
        custom.setPkConstraintPrefix("pk_");
        custom.setPkConstraintPattern("custom_pk_{table}");
        custom.setPkConstraintSuffix("_id");

        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addChangeListener(listener);

        manager.setSettings(custom);
        assertTrue(notified.get());

        DatabaseVersioningSettings loaded = manager.getSettings();
        assertEquals("CamelCaseToUnderscoresNamingStrategy", loaded.getPhysicalNamingStrategy());
        assertEquals("Single sequence for all entities", loaded.getSequenceNamingStrategy());
        assertEquals(63, loaded.getMaxDbIdentifierLength());
        assertTrue(loaded.isCreateIndexForAssociationFk());
        assertTrue(loaded.isPrimaryKeyConstraintNamed());
        assertEquals("pk_", loaded.getPkConstraintPrefix());
        assertEquals("custom_pk_{table}", loaded.getPkConstraintPattern());
        assertEquals("_id", loaded.getPkConstraintSuffix());

        manager.removeChangeListener(listener);
        manager.setSettings(original);
    }

    @Test
    void testSettingsDatabaseVersioningPageUI() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<String> navigatedSubpage = new AtomicReference<>();
        runOnFx(() -> {
            SettingsDatabaseVersioningPage page = new SettingsDatabaseVersioningPage(navigatedSubpage::set);
            assertNotNull(page);

            assertFalse(page.isModified());

            page.getMaxDbIdentifierLengthField().setText("128");
            assertTrue(page.isModified());

            page.reset();
            assertFalse(page.isModified());

            page.getCreateIndexForAssociationFkCheck().setSelected(true);
            assertTrue(page.isModified());
            page.apply();
            assertFalse(page.isModified());

            assertEquals(5, SettingsDatabaseVersioningPage.SUBPAGES.size());
        });
    }

    @Test
    void testDatabaseDiffChangesSettingsAndManager() {
        DatabaseDiffChangesSettingsManager manager = DatabaseDiffChangesSettingsManager.getInstance();
        assertNotNull(manager);

        DatabaseDiffChangesSettings original = manager.getSettings();
        assertNotNull(original);
        assertFalse(original.getRules().isEmpty());

        DatabaseDiffChangesSettings clone = original.clone();
        assertEquals(original, clone);
        assertEquals(original.hashCode(), clone.hashCode());

        DiffChangeRule r = new DiffChangeRule("TestCat", "TestAct", "TestLoc", "High", "#FF0000");
        clone.getRules().add(r);
        assertNotEquals(original, clone);

        manager.setSettings(original);
    }

    @Test
    void testSettingsDatabaseVersioningDiffChangesPageUI() throws Exception {
        if (!javaFxAvailable) return;

        runOnFx(() -> {
            SettingsDatabaseVersioningDiffChangesPage page = new SettingsDatabaseVersioningDiffChangesPage();
            assertNotNull(page);
            assertFalse(page.isModified());

            assertNotNull(page.getRulesTreeView());
            assertNotNull(page.getExcludedTable());

            page.getDangerLevelCombo().setValue("High");
            assertTrue(page.isModified());

            page.reset();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testDatabaseFlywaySettingsAndManager() {
        DatabaseFlywaySettingsManager manager = DatabaseFlywaySettingsManager.getInstance();
        assertNotNull(manager);

        DatabaseFlywaySettings original = manager.getSettings();
        assertNotNull(original);

        DatabaseFlywaySettings custom = new DatabaseFlywaySettings();
        custom.setMigrationPrefix("U");
        custom.setVersionPattern("yyyyMMddHHmmss");
        custom.setMigrationSeparator("___");
        custom.setMigrationDescription("Initial");
        custom.setUseFlywayWithoutDependency(true);

        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addChangeListener(listener);

        manager.setSettings(custom);
        assertTrue(notified.get());

        DatabaseFlywaySettings loaded = manager.getSettings();
        assertEquals("U", loaded.getMigrationPrefix());
        assertEquals("yyyyMMddHHmmss", loaded.getVersionPattern());
        assertEquals("___", loaded.getMigrationSeparator());
        assertEquals("Initial", loaded.getMigrationDescription());
        assertTrue(loaded.isUseFlywayWithoutDependency());

        manager.removeChangeListener(listener);
        manager.setSettings(original);
    }

    @Test
    void testSettingsDatabaseVersioningFlywayPageUI() throws Exception {
        if (!javaFxAvailable) return;

        runOnFx(() -> {
            SettingsDatabaseVersioningFlywayPage page = new SettingsDatabaseVersioningFlywayPage();
            assertNotNull(page);
            assertFalse(page.isModified());

            page.getMigrationPrefixField().setText("CUSTOM_V");
            assertTrue(page.isModified());

            page.reset();
            assertFalse(page.isModified());

            page.getUseFlywayWithoutDependencyCheck().setSelected(true);
            assertTrue(page.isModified());
            page.apply();
            assertFalse(page.isModified());
            assertTrue(DatabaseFlywaySettingsManager.getInstance().getSettings().isUseFlywayWithoutDependency());
        });
    }

    @Test
    void testDatabaseHibernateEnversSettingsAndManager() {
        DatabaseHibernateEnversSettingsManager manager = DatabaseHibernateEnversSettingsManager.getInstance();
        assertNotNull(manager);

        DatabaseHibernateEnversSettings original = manager.getSettings();
        assertNotNull(original);

        DatabaseHibernateEnversSettings custom = new DatabaseHibernateEnversSettings();
        custom.setUseValuesInPropertiesFiles(true);
        custom.setAuditTablePrefix("aud_");
        custom.setAuditTableSuffix("_history");
        custom.setRevisionFieldName("revision_id");
        custom.setRevisionTypeFieldName("rev_type");
        custom.setDefaultSchemaName("audit_schema");
        custom.setTreatOptimisticLockingUnversioned(true);
        custom.setTrackEntityNamesChanged(true);
        custom.setActivateModifiedPropertiesFlag(true);
        custom.setSuffixModifiedFlagColumns("_changed");

        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addChangeListener(listener);

        manager.setSettings(custom);
        assertTrue(notified.get());

        DatabaseHibernateEnversSettings loaded = manager.getSettings();
        assertTrue(loaded.isUseValuesInPropertiesFiles());
        assertEquals("aud_", loaded.getAuditTablePrefix());
        assertEquals("_history", loaded.getAuditTableSuffix());
        assertEquals("revision_id", loaded.getRevisionFieldName());
        assertEquals("rev_type", loaded.getRevisionTypeFieldName());
        assertEquals("audit_schema", loaded.getDefaultSchemaName());
        assertTrue(loaded.isTreatOptimisticLockingUnversioned());
        assertTrue(loaded.isTrackEntityNamesChanged());
        assertTrue(loaded.isActivateModifiedPropertiesFlag());
        assertEquals("_changed", loaded.getSuffixModifiedFlagColumns());

        manager.removeChangeListener(listener);
        manager.setSettings(original);
    }

    @Test
    void testSettingsDatabaseVersioningHibernateEnversPageUI() throws Exception {
        if (!javaFxAvailable) return;

        runOnFx(() -> {
            SettingsDatabaseVersioningHibernateEnversPage page = new SettingsDatabaseVersioningHibernateEnversPage();
            assertNotNull(page);
            assertFalse(page.isModified());

            page.getAuditTablePrefixField().setText("aud_");
            assertTrue(page.isModified());

            page.reset();
            assertFalse(page.isModified());

            page.getUseValuesSpecifiedCheck().setSelected(true);
            assertTrue(page.isModified());
            page.apply();
            assertFalse(page.isModified());
            assertTrue(DatabaseHibernateEnversSettingsManager.getInstance().getSettings().isUseValuesInPropertiesFiles());
        });
    }

    @Test
    void testDatabaseLiquibaseSettingsAndManager() {
        DatabaseLiquibaseSettingsManager manager = DatabaseLiquibaseSettingsManager.getInstance();
        assertNotNull(manager);

        DatabaseLiquibaseSettings original = manager.getSettings();
        assertNotNull(original);

        DatabaseLiquibaseSettings custom = new DatabaseLiquibaseSettings();
        custom.setLiquibaseVersion("4.24.0");
        custom.setChangesetAuthor("developer");
        custom.setFileType("YAML");
        custom.setAddEmptyRollback(true);
        custom.setPrimaryDirectory("db/custom/dir");
        custom.setPrimaryName("migration");
        custom.setSecondaryDirectory("db/custom/sec");
        custom.setSecondaryName("migration-sec");
        custom.setEnabledDbTypes(java.util.Set.of("mysql", "postgres"));

        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addChangeListener(listener);

        manager.setSettings(custom);
        assertTrue(notified.get());

        DatabaseLiquibaseSettings loaded = manager.getSettings();
        assertEquals("4.24.0", loaded.getLiquibaseVersion());
        assertEquals("developer", loaded.getChangesetAuthor());
        assertEquals("YAML", loaded.getFileType());
        assertTrue(loaded.isAddEmptyRollback());
        assertEquals("db/custom/dir", loaded.getPrimaryDirectory());
        assertEquals("migration", loaded.getPrimaryName());
        assertEquals("db/custom/sec", loaded.getSecondaryDirectory());
        assertEquals("migration-sec", loaded.getSecondaryName());
        assertTrue(loaded.getEnabledDbTypes().contains("mysql"));
        assertTrue(loaded.getEnabledDbTypes().contains("postgres"));

        manager.removeChangeListener(listener);
        manager.setSettings(original);
    }

    @Test
    void testSettingsDatabaseVersioningLiquibasePageUI() throws Exception {
        if (!javaFxAvailable) return;

        runOnFx(() -> {
            SettingsDatabaseVersioningLiquibasePage page = new SettingsDatabaseVersioningLiquibasePage();
            assertNotNull(page);
            assertFalse(page.isModified());

            page.getChangesetAuthorField().setText("custom_author");
            assertTrue(page.isModified());

            page.reset();
            assertFalse(page.isModified());

            page.getFileTypeCombo().setValue("JSON");
            assertTrue(page.isModified());
            page.apply();
            assertFalse(page.isModified());
            assertEquals("JSON", DatabaseLiquibaseSettingsManager.getInstance().getSettings().getFileType());
        });
    }

    @Test
    void testDatabaseTypeMappingsSettingsAndManager() {
        DatabaseTypeMappingsSettingsManager manager = DatabaseTypeMappingsSettingsManager.getInstance();
        assertNotNull(manager);

        DatabaseTypeMappingsSettings original = manager.getSettings();
        assertNotNull(original);

        DatabaseTypeMappingsSettings custom = new DatabaseTypeMappingsSettings();
        custom.setSelectedDatabase("postgresql");
        custom.getMappingsForDatabase("postgresql").add(new DatabaseTypeMappingItem("UUID", "uuid", ""));

        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addChangeListener(listener);

        manager.setSettings(custom);
        assertTrue(notified.get());

        DatabaseTypeMappingsSettings loaded = manager.getSettings();
        assertEquals("postgresql", loaded.getSelectedDatabase());
        assertFalse(loaded.getMappingsForDatabase("postgresql").isEmpty());
        assertEquals("UUID", loaded.getMappingsForDatabase("postgresql").get(0).getAttributeType());

        manager.removeChangeListener(listener);
        manager.setSettings(original);
    }

    @Test
    void testSettingsDatabaseVersioningTypeMappingsPageUI() throws Exception {
        if (!javaFxAvailable) return;

        runOnFx(() -> {
            SettingsDatabaseVersioningTypeMappingsPage page = new SettingsDatabaseVersioningTypeMappingsPage();
            assertNotNull(page);
            assertFalse(page.isModified());

            page.getDbListView().getSelectionModel().select("oracle");
            assertTrue(page.isModified());

            page.reset();
            assertFalse(page.isModified());

            page.getAddBtn().fire();
            assertTrue(page.isModified());
            page.apply();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testDiagramsSettingsAndManager() {
        DiagramsSettingsManager manager = DiagramsSettingsManager.getInstance();
        assertNotNull(manager);

        DiagramsSettings original = manager.getSettings();
        assertNotNull(original);

        DiagramsSettings custom = new DiagramsSettings();
        custom.setDefaultScope("Production Files");
        custom.setNodeItemStyle("UML Classic");
        custom.setShortenNodeItemsLength(80);
        custom.setShowGridByDefault(false);
        custom.setDefaultLayout("Organic");
        custom.setLayoutAnimationDuration(2000);

        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addChangeListener(listener);

        manager.setSettings(custom);
        assertTrue(notified.get());

        DiagramsSettings loaded = manager.getSettings();
        assertEquals("Production Files", loaded.getDefaultScope());
        assertEquals("UML Classic", loaded.getNodeItemStyle());
        assertEquals(80, loaded.getShortenNodeItemsLength());
        assertFalse(loaded.isShowGridByDefault());
        assertEquals("Organic", loaded.getDefaultLayout());
        assertEquals(2000, loaded.getLayoutAnimationDuration());

        manager.removeChangeListener(listener);
        manager.setSettings(original);
    }

    @Test
    void testSettingsToolsDiagramsPageUI() throws Exception {
        if (!javaFxAvailable) return;

        runOnFx(() -> {
            SettingsToolsDiagramsPage page = new SettingsToolsDiagramsPage();
            assertNotNull(page);
            assertFalse(page.isModified());

            page.getDefaultScopeCombo().setValue("Open Files");
            assertTrue(page.isModified());

            page.reset();
            assertFalse(page.isModified());

            page.getShowGridByDefaultCheck().setSelected(!page.getShowGridByDefaultCheck().isSelected());
            assertTrue(page.isModified());
            page.apply();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testSettingsDialogTreeStructure() throws Exception {
        if (!javaFxAvailable) return;

        runOnFx(() -> {
            SettingsDialog dialog = new SettingsDialog(null);
            assertNotNull(dialog);

            dialog.selectCategory("Tools", "Database Versioning");
            assertNotNull(dialog.getCurrentDatabaseVersioningPage());

            dialog.selectCategory("Database Versioning", "Diff Changes");
            assertNotNull(dialog.getCurrentDatabaseVersioningDiffChangesPage());

            dialog.selectCategory("Database Versioning", "Flyway");
            assertNotNull(dialog.getCurrentDatabaseVersioningFlywayPage());

            dialog.selectCategory("Database Versioning", "Hibernate Envers");
            assertNotNull(dialog.getCurrentDatabaseVersioningHibernateEnversPage());

            dialog.selectCategory("Database Versioning", "Liquibase");
            assertNotNull(dialog.getCurrentDatabaseVersioningLiquibasePage());

            dialog.selectCategory("Database Versioning", "Type Mappings");
            assertNotNull(dialog.getCurrentDatabaseVersioningTypeMappingsPage());

            dialog.selectCategory("Tools", "Diagrams");
            assertNotNull(dialog.getCurrentToolsDiagramsPage());
        });
    }

    @Test
    void testStrictBrandIsolation() {
        Pattern forbidden = Pattern.compile("(?i)\\b(intellij|jetbrains|webstorm|idea)\\b");
        List<String> filesToCheck = List.of(
                "src/main/java/dev/lumina/database/versioning/DatabaseVersioningSettings.java",
                "src/main/java/dev/lumina/database/versioning/DatabaseVersioningSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsDatabaseVersioningPage.java",
                "src/main/java/dev/lumina/database/versioning/DiffChangeRule.java",
                "src/main/java/dev/lumina/database/versioning/DiffChangeExcludedItem.java",
                "src/main/java/dev/lumina/database/versioning/DatabaseDiffChangesSettings.java",
                "src/main/java/dev/lumina/database/versioning/DatabaseDiffChangesSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsDatabaseVersioningDiffChangesPage.java",
                "src/main/java/dev/lumina/database/versioning/DatabaseFlywaySettings.java",
                "src/main/java/dev/lumina/database/versioning/DatabaseFlywaySettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsDatabaseVersioningFlywayPage.java",
                "src/main/java/dev/lumina/database/versioning/DatabaseHibernateEnversSettings.java",
                "src/main/java/dev/lumina/database/versioning/DatabaseHibernateEnversSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsDatabaseVersioningHibernateEnversPage.java",
                "src/main/java/dev/lumina/database/versioning/LiquibaseChangesetTemplateItem.java",
                "src/main/java/dev/lumina/database/versioning/DatabaseLiquibaseSettings.java",
                "src/main/java/dev/lumina/database/versioning/DatabaseLiquibaseSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsDatabaseVersioningLiquibasePage.java",
                "src/main/java/dev/lumina/database/versioning/DatabaseTypeMappingItem.java",
                "src/main/java/dev/lumina/database/versioning/DatabaseTypeMappingsSettings.java",
                "src/main/java/dev/lumina/database/versioning/DatabaseTypeMappingsSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsDatabaseVersioningTypeMappingsPage.java",
                "src/main/java/dev/lumina/tools/DiagramsSettings.java",
                "src/main/java/dev/lumina/tools/DiagramsSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsToolsDiagramsPage.java"
        );

        for (String relPath : filesToCheck) {
            File f = new File(relPath);
            assertTrue(f.exists(), "File must exist: " + relPath);
            try {
                String content = Files.readString(f.toPath());
                var matcher = forbidden.matcher(content);
                assertFalse(matcher.find(), "Found forbidden competitor brand in " + relPath);
            } catch (Exception e) {
                fail("Failed to read " + relPath + ": " + e.getMessage());
            }
        }
    }
}
