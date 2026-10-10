package dev.lumina.ui;

import dev.lumina.database.*;
import dev.lumina.tools.*;
import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
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

public class SettingsToolsDatabasePagesTest {

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
    void testCodeWithMeSettingsManagerAndModel() {
        CodeWithMeSettingsManager manager = CodeWithMeSettingsManager.getInstance();
        assertNotNull(manager);

        CodeWithMeSettings original = manager.getSettings();
        assertNotNull(original);

        CodeWithMeSettings custom = new CodeWithMeSettings();
        custom.setUserName("PairProgrammerTest");
        custom.setLobbyServerUrl("https://cwm-lobby.lumina-ide.dev");

        manager.setSettings(custom);
        CodeWithMeSettings loaded = manager.getSettings();
        assertEquals("PairProgrammerTest", loaded.getUserName());
        assertEquals("https://cwm-lobby.lumina-ide.dev", loaded.getLobbyServerUrl());

        // Restore
        manager.setSettings(original);
    }

    @Test
    void testSettingsToolsCodeWithMePageLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        runOnFx(() -> {
            AtomicBoolean navigated = new AtomicBoolean(false);
            SettingsToolsCodeWithMePage page = new SettingsToolsCodeWithMePage(() -> navigated.set(true));

            assertFalse(page.isModified());

            AtomicBoolean modifiedNotified = new AtomicBoolean(false);
            page.setOnModifiedListener(() -> modifiedNotified.set(true));

            TextField userField = page.getUserNameField();
            assertNotNull(userField);
            userField.setText("ModifiedUser");

            assertTrue(page.isModified());
            assertTrue(modifiedNotified.get());

            page.apply();
            assertFalse(page.isModified());

            CodeWithMeSettings current = CodeWithMeSettingsManager.getInstance().getSettings();
            assertEquals("ModifiedUser", current.getUserName());

            // Revert changes
            userField.setText("AnotherUser");
            assertTrue(page.isModified());
            page.revertChanges();
            assertFalse(page.isModified());
            assertEquals("ModifiedUser", userField.getText());

            // Use system name button
            page.getUseSystemNameButton().fire();
            assertEquals(CodeWithMeSettingsManager.getInstance().getSystemUserName(), userField.getText());
        });
    }

    @Test
    void testCsvFormatsSettingsManagerAndParsing() {
        CsvFormatsSettingsManager manager = CsvFormatsSettingsManager.getInstance();
        assertNotNull(manager);

        CsvFormatsSettings settings = manager.getSettings();
        assertNotNull(settings.getFormats());
        assertFalse(settings.getFormats().isEmpty());

        CsvFormat csv = settings.getFormatByName("CSV");
        assertNotNull(csv);
        assertEquals("Comma", csv.getValueSeparator());
        assertEquals(",", csv.getDelimiterChar());

        // Test parser
        String sample = "1,John,Doe\n2,Jane,Smith";
        List<List<String>> rows = manager.parseData(sample, csv);
        assertEquals(2, rows.size());
        assertEquals(List.of("1", "John", "Doe"), rows.get(0));
        assertEquals(List.of("2", "Jane", "Smith"), rows.get(1));

        // Test quotes
        String sampleQuoted = "1,\"John, Jr.\",Doe\n2,Jane,Smith";
        List<List<String>> rowsQuoted = manager.parseData(sampleQuoted, csv);
        assertEquals(2, rowsQuoted.size());
        assertEquals("John, Jr.", rowsQuoted.get(0).get(1));
    }

    @Test
    void testSettingsToolsCsvFormatsPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        runOnFx(() -> {
            SettingsToolsCsvFormatsPage page = new SettingsToolsCsvFormatsPage();
            assertFalse(page.isModified());

            AtomicBoolean modifiedNotified = new AtomicBoolean(false);
            page.setOnModifiedListener(() -> modifiedNotified.set(true));

            // Select TSV
            page.getFormatsListView().getSelectionModel().select("TSV");
            assertEquals("TSV", page.getSelectedFormat().getName());

            // Change a property
            page.getTrimWhitespacesCheck().setSelected(!page.getTrimWhitespacesCheck().isSelected());
            assertTrue(page.isModified());
            assertTrue(modifiedNotified.get());

            // Apply
            page.apply();
            assertFalse(page.isModified());

            // Revert
            page.getTrimWhitespacesCheck().setSelected(!page.getTrimWhitespacesCheck().isSelected());
            assertTrue(page.isModified());
            page.revertChanges();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testSettingsDatabaseOverviewPage() throws Exception {
        if (!javaFxAvailable) return;

        runOnFx(() -> {
            AtomicReference<String> navigated = new AtomicReference<>();
            SettingsDatabasePage page = new SettingsDatabasePage(navigated::set);

            assertNotNull(page);
            assertEquals(3, SettingsDatabasePage.DATABASE_SUBPAGES.size());
            assertTrue(SettingsDatabasePage.DATABASE_SUBPAGES.contains("Query Execution"));
            assertTrue(SettingsDatabasePage.DATABASE_SUBPAGES.contains("Data Editor and Viewer"));
            assertTrue(SettingsDatabasePage.DATABASE_SUBPAGES.contains("Other"));
        });
    }

    @Test
    void testDatabaseQueryExecutionSettingsManagerAndModel() {
        DatabaseQueryExecutionSettingsManager manager = DatabaseQueryExecutionSettingsManager.getInstance();
        assertNotNull(manager);

        DatabaseQueryExecutionSettings original = manager.getSettings();
        assertNotNull(original);
        assertNotNull(original.getProfiles());
        assertFalse(original.getProfiles().isEmpty());

        DatabaseQueryExecutionSettings custom = original.clone();
        custom.setSplitScript("By semicolon only");
        custom.setShowWarningUnsafeQueries(false);

        manager.setSettings(custom);
        DatabaseQueryExecutionSettings loaded = manager.getSettings();
        assertEquals("By semicolon only", loaded.getSplitScript());
        assertFalse(loaded.isShowWarningUnsafeQueries());

        // Restore
        manager.setSettings(original);
    }

    @Test
    void testSettingsDatabaseQueryExecutionPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        runOnFx(() -> {
            SettingsDatabaseQueryExecutionPage page = new SettingsDatabaseQueryExecutionPage();
            assertFalse(page.isModified());

            AtomicBoolean modifiedNotified = new AtomicBoolean(false);
            page.setOnModifiedListener(() -> modifiedNotified.set(true));

            page.getShowWarningUnsafeQueriesCheck().setSelected(!page.getShowWarningUnsafeQueriesCheck().isSelected());
            assertTrue(page.isModified());
            assertTrue(modifiedNotified.get());

            page.apply();
            assertFalse(page.isModified());

            page.getShowWarningUnsafeQueriesCheck().setSelected(!page.getShowWarningUnsafeQueriesCheck().isSelected());
            assertTrue(page.isModified());
            page.revertChanges();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testDatabaseOutputResultsSettingsManagerAndModel() {
        DatabaseOutputResultsSettingsManager manager = DatabaseOutputResultsSettingsManager.getInstance();
        assertNotNull(manager);

        DatabaseOutputResultsSettings original = manager.getSettings();
        assertNotNull(original);

        DatabaseOutputResultsSettings custom = original.clone();
        custom.setShowResultsInEditor(false);
        custom.setCreateTitleFromComment(false);
        custom.setTreatTextAsTitleAfter("-- title:");

        manager.setSettings(custom);
        DatabaseOutputResultsSettings loaded = manager.getSettings();
        assertFalse(loaded.isShowResultsInEditor());
        assertFalse(loaded.isCreateTitleFromComment());
        assertEquals("-- title:", loaded.getTreatTextAsTitleAfter());

        // Restore
        manager.setSettings(original);
    }

    @Test
    void testSettingsDatabaseOutputResultsPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        runOnFx(() -> {
            SettingsDatabaseOutputResultsPage page = new SettingsDatabaseOutputResultsPage();
            assertFalse(page.isModified());

            AtomicBoolean modifiedNotified = new AtomicBoolean(false);
            page.setOnModifiedListener(() -> modifiedNotified.set(true));

            page.getCreateTitleFromCommentCheck().setSelected(!page.getCreateTitleFromCommentCheck().isSelected());
            assertTrue(page.isModified());
            assertTrue(modifiedNotified.get());

            page.apply();
            assertFalse(page.isModified());

            page.getCreateTitleFromCommentCheck().setSelected(!page.getCreateTitleFromCommentCheck().isSelected());
            assertTrue(page.isModified());
            page.revertChanges();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testDatabaseUserParametersSettingsManagerAndModel() {
        DatabaseUserParametersSettingsManager manager = DatabaseUserParametersSettingsManager.getInstance();
        assertNotNull(manager);

        DatabaseUserParametersSettings original = manager.getSettings();
        assertNotNull(original);
        assertNotNull(original.getPatterns());
        assertEquals(8, original.getPatterns().size());

        DatabaseUserParametersSettings custom = original.clone();
        custom.setSubstituteInsideSqlStrings(true);
        custom.setEnableInLiteralsWithSqlInjection(false);

        manager.setSettings(custom);
        DatabaseUserParametersSettings loaded = manager.getSettings();
        assertTrue(loaded.isSubstituteInsideSqlStrings());
        assertFalse(loaded.isEnableInLiteralsWithSqlInjection());

        // Restore
        manager.setSettings(original);
    }

    @Test
    void testSettingsDatabaseUserParametersPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        runOnFx(() -> {
            SettingsDatabaseUserParametersPage page = new SettingsDatabaseUserParametersPage();
            assertFalse(page.isModified());

            AtomicBoolean modifiedNotified = new AtomicBoolean(false);
            page.setOnModifiedListener(() -> modifiedNotified.set(true));

            page.getSubstituteInsideStringsCheck().setSelected(!page.getSubstituteInsideStringsCheck().isSelected());
            assertTrue(page.isModified());
            assertTrue(modifiedNotified.get());

            page.apply();
            assertFalse(page.isModified());

            // Revert
            page.getSubstituteInsideStringsCheck().setSelected(!page.getSubstituteInsideStringsCheck().isSelected());
            assertTrue(page.isModified());
            page.revertChanges();
            assertFalse(page.isModified());

            // Add button
            int initialSize = page.getPatternsTable().getItems().size();
            page.getAddButton().fire();
            assertEquals(initialSize + 1, page.getPatternsTable().getItems().size());
            assertTrue(page.isModified());

            // Remove button
            page.getRemoveButton().fire();
            assertEquals(initialSize, page.getPatternsTable().getItems().size());
        });
    }

    @Test
    void testDatabaseDataEditorSettingsManagerAndModel() {
        DatabaseDataEditorSettingsManager manager = DatabaseDataEditorSettingsManager.getInstance();
        assertNotNull(manager);

        DatabaseDataEditorSettings original = manager.getSettings();
        assertNotNull(original);
        assertEquals(500, original.getPageSize());
        assertEquals(100, original.getResultSetPrefetchSize());

        DatabaseDataEditorSettings custom = original.clone();
        custom.setPageSize(1000);
        custom.setEnablePagingInEditorResults(true);
        custom.setSortViaOrderBy(false);

        manager.setSettings(custom);
        DatabaseDataEditorSettings loaded = manager.getSettings();
        assertEquals(1000, loaded.getPageSize());
        assertTrue(loaded.isEnablePagingInEditorResults());
        assertFalse(loaded.isSortViaOrderBy());

        // Restore
        manager.setSettings(original);
    }

    @Test
    void testSettingsDatabaseDataEditorViewerPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        runOnFx(() -> {
            SettingsDatabaseDataEditorViewerPage page = new SettingsDatabaseDataEditorViewerPage();
            assertFalse(page.isModified());

            AtomicBoolean modifiedNotified = new AtomicBoolean(false);
            page.setOnModifiedListener(() -> modifiedNotified.set(true));

            page.getEnablePagingInEditorCheck().setSelected(!page.getEnablePagingInEditorCheck().isSelected());
            assertTrue(page.isModified());
            assertTrue(modifiedNotified.get());

            page.apply();
            assertFalse(page.isModified());

            page.getEnablePagingInEditorCheck().setSelected(!page.getEnablePagingInEditorCheck().isSelected());
            assertTrue(page.isModified());
            page.revertChanges();
            assertFalse(page.isModified());
        });
    }

    @Test
    void testDatabaseOtherSettingsManagerAndModel() {
        DatabaseOtherSettingsManager manager = DatabaseOtherSettingsManager.getInstance();
        assertNotNull(manager);

        DatabaseOtherSettings original = manager.getSettings();
        assertNotNull(original);
        assertTrue(original.isConfirmCancellationForDialogsModifySchema());
        assertEquals("Playground", original.getDefaultResolveModeForConsoles());

        DatabaseOtherSettings custom = original.clone();
        custom.setDefaultResolveModeForConsoles("Production");
        custom.setStatementDelimiter(";");

        manager.setSettings(custom);
        DatabaseOtherSettings loaded = manager.getSettings();
        assertEquals("Production", loaded.getDefaultResolveModeForConsoles());
        assertEquals(";", loaded.getStatementDelimiter());

        // Restore
        manager.setSettings(original);
    }

    @Test
    void testSettingsDatabaseOtherPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        runOnFx(() -> {
            SettingsDatabaseOtherPage page = new SettingsDatabaseOtherPage();
            assertFalse(page.isModified());

            AtomicBoolean modifiedNotified = new AtomicBoolean(false);
            page.setOnModifiedListener(() -> modifiedNotified.set(true));

            page.getConfirmCancellationCheck().setSelected(!page.getConfirmCancellationCheck().isSelected());
            assertTrue(page.isModified());
            assertTrue(modifiedNotified.get());

            page.apply();
            assertFalse(page.isModified());

            page.getConfirmCancellationCheck().setSelected(!page.getConfirmCancellationCheck().isSelected());
            assertTrue(page.isModified());
            page.revertChanges();
            assertFalse(page.isModified());

            // Add/Remove VFK
            int initialCount = page.getVfkTable().getItems().size();
            page.getAddVfkButton().fire();
            assertEquals(initialCount + 1, page.getVfkTable().getItems().size());
            assertTrue(page.isModified());

            page.getRemoveVfkButton().fire();
            assertEquals(initialCount, page.getVfkTable().getItems().size());
        });
    }

    @Test
    void testStrictBrandIsolation() throws Exception {
        String[] filesToCheck = {
                "src/main/java/dev/lumina/tools/CodeWithMeSettings.java",
                "src/main/java/dev/lumina/tools/CodeWithMeSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsToolsCodeWithMePage.java",
                "src/main/java/dev/lumina/tools/CsvFormat.java",
                "src/main/java/dev/lumina/tools/CsvFormatsSettings.java",
                "src/main/java/dev/lumina/tools/CsvFormatsSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsToolsCsvFormatsPage.java",
                "src/main/java/dev/lumina/ui/SettingsDatabasePage.java",
                "src/main/java/dev/lumina/database/DatabaseQueryExecutionSettings.java",
                "src/main/java/dev/lumina/database/DatabaseQueryExecutionSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsDatabaseQueryExecutionPage.java",
                "src/main/java/dev/lumina/database/DatabaseOutputResultsSettings.java",
                "src/main/java/dev/lumina/database/DatabaseOutputResultsSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsDatabaseOutputResultsPage.java",
                "src/main/java/dev/lumina/database/DatabaseUserParameterPattern.java",
                "src/main/java/dev/lumina/database/DatabaseUserParametersSettings.java",
                "src/main/java/dev/lumina/database/DatabaseUserParametersSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsDatabaseUserParametersPage.java",
                "src/main/java/dev/lumina/database/DatabaseDataEditorSettings.java",
                "src/main/java/dev/lumina/database/DatabaseDataEditorSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsDatabaseDataEditorViewerPage.java",
                "src/main/java/dev/lumina/database/DatabaseVirtualForeignKey.java",
                "src/main/java/dev/lumina/database/DatabaseOtherSettings.java",
                "src/main/java/dev/lumina/database/DatabaseOtherSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsDatabaseOtherPage.java"
        };

        Pattern competitorPattern = Pattern.compile("(?i)\\b(intellij|jetbrains|webstorm|idea)\\b");

        for (String relativePath : filesToCheck) {
            File f = new File(relativePath);
            assertTrue(f.exists(), "File must exist: " + relativePath);
            String content = Files.readString(f.toPath());
            var matcher = competitorPattern.matcher(content);
            assertFalse(matcher.find(), "Found competitor brand reference in " + relativePath);
        }
    }
}
