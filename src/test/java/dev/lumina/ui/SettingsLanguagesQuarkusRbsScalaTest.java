package dev.lumina.ui;

import dev.lumina.quarkus.QuarkusSettings;
import dev.lumina.quarkus.QuarkusSettingsManager;
import dev.lumina.rbs.RbsSettings;
import dev.lumina.rbs.RbsSettingsManager;
import dev.lumina.scala.ScalaEditorSettings;
import dev.lumina.scala.ScalaLanguageSettingsManager;
import dev.lumina.scala.ScalaXRaySettings;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class SettingsLanguagesQuarkusRbsScalaTest {

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
    // 1. Quarkus Tests (Image 1)
    // ============================================================

    @Test
    void testQuarkusSettingsModelAndManager() {
        QuarkusSettingsManager manager = QuarkusSettingsManager.getInstance();
        manager.resetDefaults();

        QuarkusSettings settings = manager.getSettings();
        assertTrue(settings.isCreateRunConfigurationAutomatically());

        settings.setCreateRunConfigurationAutomatically(false);
        manager.setSettings(settings);
        assertFalse(manager.getSettings().isCreateRunConfigurationAutomatically());

        manager.resetDefaults();
        assertTrue(manager.getSettings().isCreateRunConfigurationAutomatically());
    }

    @Test
    void testQuarkusPageUi() throws Exception {
        runOnFx(() -> {
            QuarkusSettingsManager.getInstance().resetDefaults();

            SettingsLanguagesQuarkusPage page = new SettingsLanguagesQuarkusPage();
            assertFalse(page.isModified());
            assertTrue(page.getCreateRunConfigCheckBox().isSelected());

            page.getCreateRunConfigCheckBox().setSelected(false);
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertFalse(QuarkusSettingsManager.getInstance().getSettings().isCreateRunConfigurationAutomatically());

            page.getCreateRunConfigCheckBox().setSelected(true);
            page.revertChanges();
            assertFalse(page.isModified());
            assertFalse(page.getCreateRunConfigCheckBox().isSelected());

            QuarkusSettingsManager.getInstance().resetDefaults();
        });
    }

    // ============================================================
    // 2. RBS Tests (Image 2)
    // ============================================================

    @Test
    void testRbsSettingsModelAndManager() {
        RbsSettingsManager manager = RbsSettingsManager.getInstance();
        manager.resetDefaults();

        RbsSettings settings = manager.getSettings();
        assertFalse(settings.isImprovedTypeSupportWithRbsCollection());

        settings.setImprovedTypeSupportWithRbsCollection(true);
        manager.setSettings(settings);
        assertTrue(manager.getSettings().isImprovedTypeSupportWithRbsCollection());

        manager.resetDefaults();
        assertFalse(manager.getSettings().isImprovedTypeSupportWithRbsCollection());
    }

    @Test
    void testRbsPageUi() throws Exception {
        runOnFx(() -> {
            RbsSettingsManager.getInstance().resetDefaults();

            SettingsLanguagesRbsPage page = new SettingsLanguagesRbsPage();
            assertFalse(page.isModified());
            assertFalse(page.getImprovedTypeSupportCheckBox().isSelected());
            assertNotNull(page.getRbsCollectionLink());

            page.getImprovedTypeSupportCheckBox().setSelected(true);
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertTrue(RbsSettingsManager.getInstance().getSettings().isImprovedTypeSupportWithRbsCollection());

            page.getImprovedTypeSupportCheckBox().setSelected(false);
            page.revertChanges();
            assertFalse(page.isModified());
            assertTrue(page.getImprovedTypeSupportCheckBox().isSelected());

            RbsSettingsManager.getInstance().resetDefaults();
        });
    }

    // ============================================================
    // 3. Scala Editor Tests (Image 4)
    // ============================================================

    @Test
    void testScalaEditorSettingsModelAndManager() {
        ScalaLanguageSettingsManager manager = ScalaLanguageSettingsManager.getInstance();
        manager.resetDefaults();

        ScalaEditorSettings s = manager.getEditorSettings();
        assertTrue(s.isShowHintsOnTypeMismatch());
        assertTrue(s.isShowHintsIfNoImplicitArgumentsFound());
        assertTrue(s.isShowHintsIfAmbiguousImplicitArgumentsFound());
        assertEquals("Exports", s.getExportAliases());

        assertFalse(s.isHighlightImplicitConversions());
        assertFalse(s.isHighlightArgumentsToByNameParameters());
        assertFalse(s.isIncludeBlockExpressions());
        assertFalse(s.isIncludeLiterals());
        assertFalse(s.isCustomScalaTestKeywordsHighlighting());
        assertEquals("None", s.getCollectionTypeHighlighting());
        assertTrue(s.isAheadOfTimeCompletion());
        assertTrue(s.isUseScalaClassesPriorityOverJavaClasses());
        assertTrue(s.isConvertJavaCodeToScalaOnCopyPaste());
        assertFalse(s.isDontShowDialogOnPasteAndAutomaticallyConvert());
        assertTrue(s.isAddOverrideKeywordToMethodImplementation());

        s.setHighlightImplicitConversions(true);
        s.setCollectionTypeHighlighting("Standard collections");
        s.setExportAliases("Aliases");
        manager.setEditorSettings(s);

        ScalaEditorSettings loaded = manager.getEditorSettings();
        assertTrue(loaded.isHighlightImplicitConversions());
        assertEquals("Standard collections", loaded.getCollectionTypeHighlighting());
        assertEquals("Aliases", loaded.getExportAliases());

        manager.resetDefaults();
        assertFalse(manager.getEditorSettings().isHighlightImplicitConversions());
    }

    @Test
    void testScalaEditorPageUi() throws Exception {
        runOnFx(() -> {
            ScalaLanguageSettingsManager.getInstance().resetDefaults();

            SettingsLanguagesScalaEditorPage page = new SettingsLanguagesScalaEditorPage();
            assertFalse(page.isModified());

            assertTrue(page.getShowHintsOnTypeMismatchCheckBox().isSelected());
            assertEquals("Exports", page.getExportAliasesComboBox().getValue());

            page.getShowHintsOnTypeMismatchCheckBox().setSelected(false);
            assertTrue(page.isModified());

            page.getExportAliasesComboBox().setValue("Aliases");
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertFalse(ScalaLanguageSettingsManager.getInstance().getEditorSettings().isShowHintsOnTypeMismatch());
            assertEquals("Aliases", ScalaLanguageSettingsManager.getInstance().getEditorSettings().getExportAliases());

            page.revertChanges();
            assertFalse(page.isModified());

            // Test sub-item dependency (Highlight arguments to by-name parameters -> block expressions)
            assertFalse(page.getHighlightArgumentsToByNameCheckBox().isSelected());
            assertTrue(page.getIncludeBlockExpressionsCheckBox().isDisable());

            page.getHighlightArgumentsToByNameCheckBox().setSelected(true);
            assertFalse(page.getIncludeBlockExpressionsCheckBox().isDisable());

            ScalaLanguageSettingsManager.getInstance().resetDefaults();
        });
    }

    // ============================================================
    // 4. Scala X-Ray Mode Tests (Image 5)
    // ============================================================

    @Test
    void testScalaXRaySettingsModelAndManager() {
        ScalaLanguageSettingsManager manager = ScalaLanguageSettingsManager.getInstance();
        manager.resetDefaults();

        ScalaXRaySettings s = manager.getXRaySettings();
        assertTrue(s.isDoublePressAndHoldCtrl());
        assertFalse(s.isPressAndHoldCtrl());
        assertTrue(s.isParameterNameHints());
        assertFalse(s.isForAllParameters());
        assertTrue(s.isByNameArgumentHints());
        assertTrue(s.isApplyMethodHints());
        assertTrue(s.isTypeHints());
        assertTrue(s.isMemberVariables());
        assertTrue(s.isLocalVariables());
        assertTrue(s.isMethodResults());
        assertTrue(s.isLambdaParameters());
        assertTrue(s.isLambdaPlaceholders());
        assertTrue(s.isVariablePatterns());
        assertTrue(s.isMethodChainHints());
        assertFalse(s.isTypeArguments());
        assertTrue(s.isImplicitHints());
        assertTrue(s.isIndentGuides());
        assertFalse(s.isMethodSeparators());
        assertEquals("Always", s.getWidgetDisplay());

        s.setMethodSeparators(true);
        s.setWidgetDisplay("When active");
        manager.setXRaySettings(s);

        ScalaXRaySettings loaded = manager.getXRaySettings();
        assertTrue(loaded.isMethodSeparators());
        assertEquals("When active", loaded.getWidgetDisplay());

        manager.resetDefaults();
        assertFalse(manager.getXRaySettings().isMethodSeparators());
    }

    @Test
    void testScalaXRayPageUi() throws Exception {
        runOnFx(() -> {
            ScalaLanguageSettingsManager.getInstance().resetDefaults();

            SettingsLanguagesScalaXRayPage page = new SettingsLanguagesScalaXRayPage();
            assertFalse(page.isModified());

            assertTrue(page.getDoublePressAndHoldCtrlCheckBox().isSelected());
            assertFalse(page.getPressAndHoldCtrlCheckBox().isSelected());
            assertEquals("Always", page.getWidgetDisplayComboBox().getValue());

            page.getMethodSeparatorsCheckBox().setSelected(true);
            assertTrue(page.isModified());

            page.getWidgetDisplayComboBox().setValue("When active");
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertTrue(ScalaLanguageSettingsManager.getInstance().getXRaySettings().isMethodSeparators());
            assertEquals("When active", ScalaLanguageSettingsManager.getInstance().getXRaySettings().getWidgetDisplay());

            page.revertChanges();
            assertFalse(page.isModified());

            ScalaLanguageSettingsManager.getInstance().resetDefaults();
        });
    }

    // ============================================================
    // 5. SettingsDialog Navigation & Category Overview (Image 3)
    // ============================================================

    @Test
    void testSettingsDialogTreeNavigationToQuarkusRbsScalaAndSubpages() throws Exception {
        runOnFx(() -> {
            SettingsDialog dialog = new SettingsDialog(null);
            assertNotNull(dialog);

            TreeItem<String> languagesNode = null;
            for (TreeItem<String> child : dialog.getTree().getRoot().getChildren()) {
                if ("Languages & Frameworks".equals(child.getValue())) {
                    languagesNode = child;
                    break;
                }
            }
            assertNotNull(languagesNode, "Languages & Frameworks must exist in tree");

            // 1. Quarkus
            TreeItem<String> quarkusItem = findChild(languagesNode, "Quarkus");
            assertNotNull(quarkusItem);
            dialog.getTree().getSelectionModel().select(quarkusItem);
            assertNotNull(dialog.getCurrentLanguagesQuarkusPage());
            assertTrue(dialog.getCurrentLanguagesQuarkusPage().getCreateRunConfigCheckBox().isSelected());

            // 2. RBS
            TreeItem<String> rbsItem = findChild(languagesNode, "RBS");
            assertNotNull(rbsItem);
            dialog.getTree().getSelectionModel().select(rbsItem);
            assertNotNull(dialog.getCurrentLanguagesRbsPage());
            assertFalse(dialog.getCurrentLanguagesRbsPage().getImprovedTypeSupportCheckBox().isSelected());

            // 3. Scala (Image 3 Category Overview)
            TreeItem<String> scalaItem = findChild(languagesNode, "Scala");
            assertNotNull(scalaItem);
            assertFalse(scalaItem.getChildren().isEmpty(), "Scala node must have children");
            assertEquals(9, scalaItem.getChildren().size());

            dialog.getTree().getSelectionModel().select(scalaItem);

            // 4. Scala > Editor (Image 4)
            TreeItem<String> editorItem = findChild(scalaItem, "Editor");
            assertNotNull(editorItem);
            dialog.getTree().getSelectionModel().select(editorItem);
            assertNotNull(dialog.getCurrentLanguagesScalaEditorPage());
            assertTrue(dialog.getCurrentLanguagesScalaEditorPage().getShowHintsOnTypeMismatchCheckBox().isSelected());

            // 5. Scala > X-Ray Mode (Image 5)
            TreeItem<String> xRayItem = findChild(scalaItem, "X-Ray Mode");
            assertNotNull(xRayItem);
            dialog.getTree().getSelectionModel().select(xRayItem);
            assertNotNull(dialog.getCurrentLanguagesScalaXRayPage());
            assertTrue(dialog.getCurrentLanguagesScalaXRayPage().getDoublePressAndHoldCtrlCheckBox().isSelected());
        });
    }

    private TreeItem<String> findChild(TreeItem<String> parent, String value) {
        if (parent == null) return null;
        for (TreeItem<String> child : parent.getChildren()) {
            if (value.equals(child.getValue())) {
                return child;
            }
        }
        return null;
    }
}
