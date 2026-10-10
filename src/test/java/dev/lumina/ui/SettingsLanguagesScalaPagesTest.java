package dev.lumina.ui;

import dev.lumina.scala.*;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class SettingsLanguagesScalaPagesTest {

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
    // 1. Scala Project View (Image 1)
    // ============================================================

    @Test
    void testScalaProjectViewModelAndManager() {
        ScalaLanguageSettingsManager manager = ScalaLanguageSettingsManager.getInstance();
        manager.resetDefaults();

        ScalaProjectViewSettings pv = manager.getProjectViewSettings();
        assertFalse(pv.isGroupPackageObjectWithPackage());
        assertFalse(pv.isHighlightNodesWithErrors());

        pv.setGroupPackageObjectWithPackage(true);
        pv.setHighlightNodesWithErrors(true);
        manager.setProjectViewSettings(pv);

        ScalaProjectViewSettings updated = manager.getProjectViewSettings();
        assertTrue(updated.isGroupPackageObjectWithPackage());
        assertTrue(updated.isHighlightNodesWithErrors());

        manager.resetDefaults();
        assertFalse(manager.getProjectViewSettings().isGroupPackageObjectWithPackage());
    }

    @Test
    void testScalaProjectViewPageUi() throws Exception {
        runOnFx(() -> {
            ScalaLanguageSettingsManager.getInstance().resetDefaults();

            SettingsLanguagesScalaProjectViewPage page = new SettingsLanguagesScalaProjectViewPage();
            assertFalse(page.isModified());
            assertFalse(page.getGroupPackageObjectCheckBox().isSelected());
            assertFalse(page.getHighlightNodesWithErrorsCheckBox().isSelected());

            page.getGroupPackageObjectCheckBox().setSelected(true);
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertTrue(ScalaLanguageSettingsManager.getInstance().getProjectViewSettings().isGroupPackageObjectWithPackage());

            page.getGroupPackageObjectCheckBox().setSelected(false);
            assertTrue(page.isModified());
            page.revertChanges();
            assertTrue(page.getGroupPackageObjectCheckBox().isSelected());
            assertFalse(page.isModified());

            page.resetDefaults();
            assertFalse(page.getGroupPackageObjectCheckBox().isSelected());
        });
    }

    // ============================================================
    // 2. Scala Performance (Image 2)
    // ============================================================

    @Test
    void testScalaPerformanceModelAndManager() {
        ScalaLanguageSettingsManager manager = ScalaLanguageSettingsManager.getInstance();
        manager.resetDefaults();

        ScalaPerformanceSettings perf = manager.getPerformanceSettings();
        assertEquals(-1, perf.getImplicitParametersSearchDepth());
        assertEquals("Enabled", perf.getScalaMetaProgramsExecution());
        assertEquals("Metadata", perf.getLocalIvyCacheIndexingMode());
        assertTrue(perf.isTrimMethodBodiesExpandedByScalaMeta());
        assertFalse(perf.isSearchAllSymbolsIncludeLocals());
        assertFalse(perf.isDisableParsingOfDocComments());
        assertFalse(perf.isDisableLanguageInjectionInScalaFiles());
        assertFalse(perf.isDontCacheCompoundTypes());

        perf.setImplicitParametersSearchDepth(5);
        perf.setScalaMetaProgramsExecution("Disabled");
        perf.setLocalIvyCacheIndexingMode("Full");
        perf.setTrimMethodBodiesExpandedByScalaMeta(false);
        perf.setSearchAllSymbolsIncludeLocals(true);
        perf.setDisableParsingOfDocComments(true);
        perf.setDisableLanguageInjectionInScalaFiles(true);
        perf.setDontCacheCompoundTypes(true);
        manager.setPerformanceSettings(perf);

        ScalaPerformanceSettings updated = manager.getPerformanceSettings();
        assertEquals(5, updated.getImplicitParametersSearchDepth());
        assertEquals("Disabled", updated.getScalaMetaProgramsExecution());
        assertEquals("Full", updated.getLocalIvyCacheIndexingMode());
        assertFalse(updated.isTrimMethodBodiesExpandedByScalaMeta());
        assertTrue(updated.isSearchAllSymbolsIncludeLocals());
        assertTrue(updated.isDisableParsingOfDocComments());
        assertTrue(updated.isDisableLanguageInjectionInScalaFiles());
        assertTrue(updated.isDontCacheCompoundTypes());

        manager.resetDefaults();
        assertEquals(-1, manager.getPerformanceSettings().getImplicitParametersSearchDepth());
    }

    @Test
    void testScalaPerformancePageUi() throws Exception {
        runOnFx(() -> {
            ScalaLanguageSettingsManager.getInstance().resetDefaults();

            SettingsLanguagesScalaPerformancePage page = new SettingsLanguagesScalaPerformancePage();
            assertFalse(page.isModified());
            assertEquals(-1, page.getImplicitSearchDepthSpinner().getValue());
            assertEquals("Enabled", page.getScalaMetaProgramsComboBox().getValue());
            assertEquals("Metadata", page.getIvyCacheIndexingModeComboBox().getValue());
            assertTrue(page.getTrimMethodBodiesCheckBox().isSelected());
            assertFalse(page.getSearchAllSymbolsCheckBox().isSelected());
            assertFalse(page.getDisableDocCommentsCheckBox().isSelected());
            assertFalse(page.getDisableLanguageInjectionCheckBox().isSelected());
            assertFalse(page.getDontCacheCompoundTypesCheckBox().isSelected());

            // Advanced toggle test
            assertTrue(page.getAdvancedContentBox().isVisible());
            page.getAdvancedToggleBtn().fire();
            assertFalse(page.getAdvancedContentBox().isVisible());
            page.getAdvancedToggleBtn().fire();
            assertTrue(page.getAdvancedContentBox().isVisible());

            // Modifications
            page.getImplicitSearchDepthSpinner().getValueFactory().setValue(10);
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertEquals(10, ScalaLanguageSettingsManager.getInstance().getPerformanceSettings().getImplicitParametersSearchDepth());

            page.getImplicitSearchDepthSpinner().getValueFactory().setValue(20);
            assertTrue(page.isModified());
            page.revertChanges();
            assertEquals(10, page.getImplicitSearchDepthSpinner().getValue());
            assertFalse(page.isModified());

            page.resetDefaults();
            assertEquals(-1, page.getImplicitSearchDepthSpinner().getValue());
        });
    }

    // ============================================================
    // 3. Scala Worksheet (Image 3)
    // ============================================================

    @Test
    void testScalaWorksheetModelAndManager() {
        ScalaLanguageSettingsManager manager = ScalaLanguageSettingsManager.getInstance();
        manager.resetDefaults();

        ScalaWorksheetSettings ws = manager.getWorksheetSettings();
        assertEquals("Always Worksheet", ws.getTreatScFilesAs());
        assertTrue(ws.isRunWorksheetInCompilerProcessPlainMode());
        assertFalse(ws.isUseEclipseCompatibilityMode());
        assertTrue(ws.isTreatScalaScratchFilesAsWorksheet());
        assertTrue(ws.isCollapseLongOutputByDefault());
        assertEquals(35, ws.getOutputCutoffLimit());
        assertEquals(1400, ws.getDelayBeforeAutoRunMs());

        ws.setTreatScFilesAs("Always Ammonite");
        ws.setRunWorksheetInCompilerProcessPlainMode(false);
        ws.setUseEclipseCompatibilityMode(true);
        ws.setTreatScalaScratchFilesAsWorksheet(false);
        ws.setCollapseLongOutputByDefault(false);
        ws.setOutputCutoffLimit(50);
        ws.setDelayBeforeAutoRunMs(2000);
        manager.setWorksheetSettings(ws);

        ScalaWorksheetSettings updated = manager.getWorksheetSettings();
        assertEquals("Always Ammonite", updated.getTreatScFilesAs());
        assertFalse(updated.isRunWorksheetInCompilerProcessPlainMode());
        assertTrue(updated.isUseEclipseCompatibilityMode());
        assertFalse(updated.isTreatScalaScratchFilesAsWorksheet());
        assertFalse(updated.isCollapseLongOutputByDefault());
        assertEquals(50, updated.getOutputCutoffLimit());
        assertEquals(2000, updated.getDelayBeforeAutoRunMs());

        manager.resetDefaults();
        assertEquals("Always Worksheet", manager.getWorksheetSettings().getTreatScFilesAs());
    }

    @Test
    void testScalaWorksheetPageUi() throws Exception {
        runOnFx(() -> {
            ScalaLanguageSettingsManager.getInstance().resetDefaults();

            SettingsLanguagesScalaWorksheetPage page = new SettingsLanguagesScalaWorksheetPage();
            assertFalse(page.isModified());
            assertEquals("Always Worksheet", page.getTreatScFilesAsComboBox().getValue());
            assertTrue(page.getRunInCompilerProcessCheckBox().isSelected());
            assertFalse(page.getUseEclipseCompatibilityCheckBox().isSelected());
            assertTrue(page.getTreatScratchFilesAsWorksheetCheckBox().isSelected());
            assertTrue(page.getCollapseLongOutputCheckBox().isSelected());
            assertEquals(35, page.getOutputCutoffLimitSpinner().getValue());
            assertEquals(1400, page.getDelayBeforeAutoRunSpinner().getValue());

            page.getTreatScFilesAsComboBox().setValue("Always Ammonite");
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertEquals("Always Ammonite", ScalaLanguageSettingsManager.getInstance().getWorksheetSettings().getTreatScFilesAs());

            page.getTreatScFilesAsComboBox().setValue("Always Worksheet");
            assertTrue(page.isModified());
            page.revertChanges();
            assertEquals("Always Ammonite", page.getTreatScFilesAsComboBox().getValue());
            assertFalse(page.isModified());

            page.resetDefaults();
            assertEquals("Always Worksheet", page.getTreatScFilesAsComboBox().getValue());
        });
    }

    // ============================================================
    // 4. Scala Base Package (Image 4)
    // ============================================================

    @Test
    void testScalaBasePackageModelAndManager() {
        ScalaLanguageSettingsManager manager = ScalaLanguageSettingsManager.getInstance();
        manager.resetDefaults();

        ScalaBasePackageSettings bp = manager.getBasePackageSettings();
        assertTrue(bp.isInheritFromPackagePrefix());
        assertTrue(bp.getCustomBasePackages().isEmpty());

        bp.setInheritFromPackagePrefix(false);
        bp.setCustomBasePackages(List.of(
                new ScalaBasePackageEntry("core", "org.lumina.core"),
                new ScalaBasePackageEntry("api", "org.lumina.api")
        ));
        manager.setBasePackageSettings(bp);

        ScalaBasePackageSettings updated = manager.getBasePackageSettings();
        assertFalse(updated.isInheritFromPackagePrefix());
        assertEquals(2, updated.getCustomBasePackages().size());
        assertEquals("core", updated.getCustomBasePackages().get(0).getModule());
        assertEquals("org.lumina.core", updated.getCustomBasePackages().get(0).getBasePackage());

        manager.resetDefaults();
        assertTrue(manager.getBasePackageSettings().isInheritFromPackagePrefix());
        assertTrue(manager.getBasePackageSettings().getCustomBasePackages().isEmpty());
    }

    @Test
    void testScalaBasePackagePageUi() throws Exception {
        runOnFx(() -> {
            ScalaLanguageSettingsManager.getInstance().resetDefaults();

            SettingsLanguagesScalaBasePackagePage page = new SettingsLanguagesScalaBasePackagePage();
            assertFalse(page.isModified());
            assertTrue(page.getInheritRadioButton().isSelected());
            assertFalse(page.getCustomRadioButton().isSelected());
            assertTrue(page.getTable().isDisable());
            assertTrue(page.getAddBtn().isDisable());
            assertTrue(page.getRemoveBtn().isDisable());

            // Switch to custom
            page.getCustomRadioButton().setSelected(true);
            assertTrue(page.isModified());
            assertFalse(page.getTable().isDisable());
            assertFalse(page.getAddBtn().isDisable());

            // Add row
            page.getAddBtn().fire();
            assertEquals(1, page.getTableItems().size());
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            ScalaBasePackageSettings saved = ScalaLanguageSettingsManager.getInstance().getBasePackageSettings();
            assertFalse(saved.isInheritFromPackagePrefix());
            assertEquals(1, saved.getCustomBasePackages().size());

            // Remove row
            page.getTable().getSelectionModel().select(0);
            page.getRemoveBtn().fire();
            assertEquals(0, page.getTableItems().size());
            assertTrue(page.isModified());

            page.revertChanges();
            assertEquals(1, page.getTableItems().size());
            assertFalse(page.isModified());

            page.resetDefaults();
            assertTrue(page.getInheritRadioButton().isSelected());
            assertEquals(0, page.getTableItems().size());
        });
    }

    // ============================================================
    // 5. Scala Misc (Image 5)
    // ============================================================

    @Test
    void testScalaMiscModelAndManager() {
        ScalaLanguageSettingsManager manager = ScalaLanguageSettingsManager.getInstance();
        manager.resetDefaults();

        ScalaMiscSettings misc = manager.getMiscSettings();
        assertEquals("org.scalatest.funsuite.AnyFunSuiteLike", misc.getScalaTestDefaultSuperClass());
        assertEquals("Auto", misc.getTrailingCommas());
        assertEquals(19, misc.getInjections().size());

        // Check key injection default rules
        assertEquals("css", misc.getInjections().get(0).getPrefix());
        assertEquals("CSS", misc.getInjections().get(0).getLanguageId());
        assertEquals("sql", misc.getInjections().get(12).getPrefix());
        assertEquals("SQL", misc.getInjections().get(12).getLanguageId());
        assertEquals("yaml", misc.getInjections().get(18).getPrefix());
        assertEquals("yaml", misc.getInjections().get(18).getLanguageId());

        misc.setScalaTestDefaultSuperClass("org.scalatest.flatspec.AnyFlatSpec");
        misc.setTrailingCommas("Always");
        misc.getInjections().add(new ScalaInterpolatedStringInjection("graphql", "GraphQL"));
        manager.setMiscSettings(misc);

        ScalaMiscSettings updated = manager.getMiscSettings();
        assertEquals("org.scalatest.flatspec.AnyFlatSpec", updated.getScalaTestDefaultSuperClass());
        assertEquals("Always", updated.getTrailingCommas());
        assertEquals(20, updated.getInjections().size());

        manager.resetDefaults();
        assertEquals("org.scalatest.funsuite.AnyFunSuiteLike", manager.getMiscSettings().getScalaTestDefaultSuperClass());
        assertEquals(19, manager.getMiscSettings().getInjections().size());
    }

    @Test
    void testScalaMiscPageUi() throws Exception {
        runOnFx(() -> {
            ScalaLanguageSettingsManager.getInstance().resetDefaults();

            SettingsLanguagesScalaMiscPage page = new SettingsLanguagesScalaMiscPage();
            assertFalse(page.isModified());
            assertEquals("org.scalatest.funsuite.AnyFunSuiteLike", page.getScalaTestSuperClassField().getText());
            assertEquals("Auto", page.getTrailingCommasComboBox().getValue());
            assertEquals(19, page.getTableItems().size());

            page.getScalaTestSuperClassField().setText("custom.Suite");
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertEquals("custom.Suite", ScalaLanguageSettingsManager.getInstance().getMiscSettings().getScalaTestDefaultSuperClass());

            // Add new injection row
            page.getAddBtn().fire();
            assertEquals(20, page.getTableItems().size());
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertEquals(20, ScalaLanguageSettingsManager.getInstance().getMiscSettings().getInjections().size());

            // Remove the added row
            page.getTable().getSelectionModel().select(19);
            page.getRemoveBtn().fire();
            assertEquals(19, page.getTableItems().size());
            assertTrue(page.isModified());

            page.revertChanges();
            assertEquals(20, page.getTableItems().size());
            assertFalse(page.isModified());

            page.resetDefaults();
            assertEquals("org.scalatest.funsuite.AnyFunSuiteLike", page.getScalaTestSuperClassField().getText());
            assertEquals(19, page.getTableItems().size());
        });
    }

    // ============================================================
    // 6. SettingsDialog Integration Tests
    // ============================================================

    @Test
    void testSettingsDialogTreeAndScalaPageBuilders() throws Exception {
        runOnFx(() -> {
            SettingsDialog dialog = new SettingsDialog(null);

            TreeItem<String> languagesItem = null;
            for (TreeItem<String> rootItem : dialog.getTree().getRoot().getChildren()) {
                if ("Languages & Frameworks".equals(rootItem.getValue())) {
                    languagesItem = rootItem;
                    break;
                }
            }
            assertNotNull(languagesItem, "Languages & Frameworks category item must exist");

            TreeItem<String> scalaItem = null;
            for (TreeItem<String> child : languagesItem.getChildren()) {
                if ("Scala".equals(child.getValue())) {
                    scalaItem = child;
                    break;
                }
            }
            assertNotNull(scalaItem, "Scala category item must exist");

            // Verify children of Scala: Editor, X-Ray Mode, Project View, Performance, Worksheet, Base Package, Misc
            String[] expectedSubpages = {
                    "Editor", "X-Ray Mode", "Project View", "Performance",
                    "Worksheet", "Base Package", "Misc"
            };
            for (String sub : expectedSubpages) {
                boolean found = false;
                for (TreeItem<String> child : scalaItem.getChildren()) {
                    if (sub.equals(child.getValue())) {
                        found = true;
                        break;
                    }
                }
                assertTrue(found, "Subpage " + sub + " must exist under Scala");
            }

            // Test Project View navigation
            for (TreeItem<String> child : scalaItem.getChildren()) {
                if ("Project View".equals(child.getValue())) {
                    dialog.getTree().getSelectionModel().select(child);
                    break;
                }
            }
            assertNotNull(dialog.getCurrentLanguagesScalaProjectViewPage(), "Scala Project View page must be built");

            // Test Performance navigation
            for (TreeItem<String> child : scalaItem.getChildren()) {
                if ("Performance".equals(child.getValue())) {
                    dialog.getTree().getSelectionModel().select(child);
                    break;
                }
            }
            assertNotNull(dialog.getCurrentLanguagesScalaPerformancePage(), "Scala Performance page must be built");

            // Test Worksheet navigation
            for (TreeItem<String> child : scalaItem.getChildren()) {
                if ("Worksheet".equals(child.getValue())) {
                    dialog.getTree().getSelectionModel().select(child);
                    break;
                }
            }
            assertNotNull(dialog.getCurrentLanguagesScalaWorksheetPage(), "Scala Worksheet page must be built");

            // Test Base Package navigation
            for (TreeItem<String> child : scalaItem.getChildren()) {
                if ("Base Package".equals(child.getValue())) {
                    dialog.getTree().getSelectionModel().select(child);
                    break;
                }
            }
            assertNotNull(dialog.getCurrentLanguagesScalaBasePackagePage(), "Scala Base Package page must be built");

            // Test Misc navigation
            for (TreeItem<String> child : scalaItem.getChildren()) {
                if ("Misc".equals(child.getValue())) {
                    dialog.getTree().getSelectionModel().select(child);
                    break;
                }
            }
            assertNotNull(dialog.getCurrentLanguagesScalaMiscPage(), "Scala Misc page must be built");
        });
    }
}
