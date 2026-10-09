package dev.lumina.ui;

import dev.lumina.play.PlaySettings;
import dev.lumina.play.PlaySettingsManager;
import dev.lumina.protobuf.ProtobufImportPath;
import dev.lumina.protobuf.ProtobufSettings;
import dev.lumina.protobuf.ProtobufSettingsManager;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class SettingsLanguagesPlayProtobufTest {

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
    void testPlaySettingsModelAndManager() {
        PlaySettingsManager manager = PlaySettingsManager.getInstance();
        manager.resetDefaults();

        PlaySettings s = manager.getSettings();
        assertFalse(s.isUsePlayCompiler());
        assertTrue(s.isDontCompileWithinIdeBeforeRun());
        assertNotNull(s.getPlayModule());
        assertEquals("", s.getProjectUri());
        assertEquals("", s.getAdditionalSbtOptions());
        assertEquals(8, s.getMinSpacesForRoutesFormatting());
        assertFalse(s.isIgnoreUrlDepthInRouteFiles());
        assertFalse(s.isReformatRoutesFileOnEnter());
        assertTrue(s.isExcludeTargetDirOnRefresh());
        assertTrue(s.isColoredOutputConsole());
        assertTrue(s.isShowCodeRefsInOutputConsole());
        assertFalse(s.isSetTemplateImportsManually());
        assertTrue(s.getTemplateImports().isEmpty());
        assertTrue(s.isGutterActionMethods());
        assertTrue(s.isGutterViewCalls());
        assertTrue(s.isGutterResultCalls());

        // Dynamic modules detection
        List<String> modules = PlaySettings.detectAvailableModules();
        assertFalse(modules.isEmpty());
        assertTrue(modules.get(0).startsWith("Module: '"));

        // Mutation & Persistence
        s.setUsePlayCompiler(true);
        s.setMinSpacesForRoutesFormatting(12);
        s.setProjectUri("http://localhost:9000");
        s.setTemplateImports(List.of("views.html._", "models._"));
        manager.setSettings(s);

        PlaySettings loaded = manager.getSettings();
        assertTrue(loaded.isUsePlayCompiler());
        assertEquals(12, loaded.getMinSpacesForRoutesFormatting());
        assertEquals("http://localhost:9000", loaded.getProjectUri());
        assertEquals(2, loaded.getTemplateImports().size());

        manager.resetDefaults();
        assertFalse(manager.getSettings().isUsePlayCompiler());
    }

    @Test
    void testPlayPageUiInteractions() throws Exception {
        runOnFx(() -> {
            PlaySettingsManager.getInstance().resetDefaults();

            SettingsLanguagesPlayPage page = new SettingsLanguagesPlayPage();
            assertFalse(page.isModified());

            // Check tabs
            page.getRoutesTabBtn().fire();
            page.getOtherTabBtn().fire();
            page.getCompilerTabBtn().fire();

            // Toggle compiler checkbox
            page.getUsePlayCompilerCheckBox().setSelected(true);
            assertTrue(page.isModified());

            // Apply
            page.apply();
            assertFalse(page.isModified());
            assertTrue(PlaySettingsManager.getInstance().getSettings().isUsePlayCompiler());

            // Change routes formatting
            page.getMinSpacesSpinner().getValueFactory().setValue(16);
            assertTrue(page.isModified());

            // Reset / Revert
            page.revertChanges();
            assertFalse(page.isModified());
            assertEquals(8, page.getMinSpacesSpinner().getValue());
        });
    }

    @Test
    void testProtobufSettingsModelAndManager() {
        ProtobufSettingsManager manager = ProtobufSettingsManager.getInstance();
        manager.resetDefaults();

        ProtobufSettings s = manager.getSettings();
        assertTrue(s.isApplyThirdPartyConfigurations());
        assertTrue(s.isIncludeStandardProtoDirectories());
        assertTrue(s.isIncludeProjectContentRoots());
        assertFalse(s.isSearchForImportedFilesInIndexes());
        assertTrue(s.isIncludeBundledWellKnownProtoFiles());
        assertTrue(s.isWarnAboutMissingSchemaAssociations());
        assertEquals("", s.getDescriptorPath());

        // Import paths verification
        List<ProtobufImportPath> paths = s.getImportPaths();
        assertTrue(paths.size() >= 4);
        assertTrue(paths.stream().anyMatch(p -> p.getLocation().contains("third-party contributed paths")));
        assertTrue(paths.stream().anyMatch(p -> p.getLocation().contains("proto directories")));
        assertTrue(paths.stream().anyMatch(p -> p.getLocation().contains("content roots")));
        assertTrue(paths.stream().anyMatch(p -> p.getLocation().contains("Well Known Proto files")));

        // Dynamic detection helpers
        assertTrue(ProtobufSettings.detectContentRootsCount() > 0);
        assertTrue(ProtobufSettings.detectProtoDirectoriesCount() >= 0);
        assertNotNull(ProtobufSettings.detectAvailableDescriptorFiles());

        // Mutation & Persistence with custom paths
        s.setSearchForImportedFilesInIndexes(true);
        s.setDescriptorPath("/tmp/compiled_schema.desc");
        s.getImportPaths().add(new ProtobufImportPath("/custom/proto/dir", "com.mycompany.proto", false));
        manager.setSettings(s);

        ProtobufSettings loaded = manager.getSettings();
        assertTrue(loaded.isSearchForImportedFilesInIndexes());
        assertEquals("/tmp/compiled_schema.desc", loaded.getDescriptorPath());
        assertTrue(loaded.getImportPaths().stream().anyMatch(p -> p.getLocation().equals("/custom/proto/dir")));

        manager.resetDefaults();
        assertFalse(manager.getSettings().isSearchForImportedFilesInIndexes());
    }

    @Test
    void testProtobufPageUiInteractions() throws Exception {
        runOnFx(() -> {
            ProtobufSettingsManager.getInstance().resetDefaults();

            SettingsLanguagesProtobufPage page = new SettingsLanguagesProtobufPage();
            assertFalse(page.isModified());

            page.getSearchInIndexesCheckBox().setSelected(true);
            assertTrue(page.isModified());

            page.getPathsList().add(new ProtobufImportPath("/workspace/my-proto", "pkg.custom", false));
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertTrue(ProtobufSettingsManager.getInstance().getSettings().isSearchForImportedFilesInIndexes());

            page.getApplyThirdPartyCheckBox().setSelected(false);
            assertTrue(page.isModified());

            page.revertChanges();
            assertFalse(page.isModified());
            assertTrue(page.getApplyThirdPartyCheckBox().isSelected());
        });
    }

    @Test
    void testProtobufTextFormatPageUi() throws Exception {
        runOnFx(() -> {
            ProtobufSettingsManager.getInstance().resetDefaults();

            SettingsLanguagesProtobufTextFormatPage page = new SettingsLanguagesProtobufTextFormatPage();
            assertFalse(page.isModified());
            assertTrue(page.getWarnMissingSchemaCheckBox().isSelected());

            page.getWarnMissingSchemaCheckBox().setSelected(false);
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertFalse(ProtobufSettingsManager.getInstance().getSettings().isWarnAboutMissingSchemaAssociations());

            page.getWarnMissingSchemaCheckBox().setSelected(true);
            page.revertChanges();
            assertFalse(page.isModified());
            assertFalse(page.getWarnMissingSchemaCheckBox().isSelected());
        });
    }

    @Test
    void testSettingsDialogTreeNavigationToPlayAndProtobuf() throws Exception {
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
            assertNotNull(languagesNode, "Languages & Frameworks node must exist");

            // 1. Navigate to Play
            TreeItem<String> playItem = findChild(languagesNode, "Play");
            assertNotNull(playItem, "Play tree item must exist under Languages & Frameworks");
            dialog.getTree().getSelectionModel().select(playItem);

            assertNotNull(dialog.getCurrentLanguagesPlayPage(), "Play page must be instantiated");
            assertNotNull(dialog.getCurrentLanguagesPlayPage().getUsePlayCompilerCheckBox());

            // 2. Navigate to Protocol Buffers
            TreeItem<String> protoItem = findChild(languagesNode, "Protocol Buffers");
            assertNotNull(protoItem, "Protocol Buffers tree item must exist");
            dialog.getTree().getSelectionModel().select(protoItem);

            assertNotNull(dialog.getCurrentLanguagesProtobufPage(), "Protocol Buffers page must be instantiated");
            assertNotNull(dialog.getCurrentLanguagesProtobufPage().getApplyThirdPartyCheckBox());

            // 3. Navigate to Protocol Buffers > Text Format
            TreeItem<String> textFormatItem = findChild(protoItem, "Text Format");
            assertNotNull(textFormatItem, "Text Format tree item must exist under Protocol Buffers");
            dialog.getTree().getSelectionModel().select(textFormatItem);

            assertNotNull(dialog.getCurrentLanguagesProtobufTextFormatPage(), "Text Format page must be instantiated");
            assertNotNull(dialog.getCurrentLanguagesProtobufTextFormatPage().getWarnMissingSchemaCheckBox());
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
