package dev.lumina.ui;

import dev.lumina.readermode.*;
import dev.lumina.textmate.*;
import dev.lumina.todo.*;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for Editor > Reader Mode, TextMate Bundles, and TODO settings in Lumina IDE.
 * Validates dynamic provider extensible SPIs, complete catalogs, subordination rules,
 * dirty tracking lifecycle (isModified, apply, reset), UI page models, and strict brand isolation.
 */
public class SettingsReaderModeTextMateTodoTest {

    private static boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> javaFxAvailable = true);
            javaFxAvailable = true;
        } catch (IllegalStateException e) {
            javaFxAvailable = true;
        } catch (Throwable t) {
            javaFxAvailable = false;
        }
    }

    @BeforeEach
    void setUp() {
        ReaderModeManager.getInstance().resetToFactoryDefaults();
        TextMateBundlesManager.getInstance().resetToFactoryDefaults();
        TodoSettingsManager.getInstance().resetToFactoryDefaults();
    }

    // =========================================================================
    // 1. Reader Mode Tests
    // =========================================================================

    @Test
    void testReaderModeDefaultSettings() {
        ReaderModeManager manager = ReaderModeManager.getInstance();
        ReaderModeSettings s = manager.getWorkingSettings();

        assertTrue(s.isEnabled(), "Reader mode should be enabled by default");
        assertTrue(s.isRenderedDocs(), "Rendered docs should be enabled by default");
        assertFalse(s.isErrorHighlighting(), "Error highlighting should be disabled by default");
        assertFalse(s.isFontLigatures(), "Font ligatures should be disabled by default");
        assertFalse(s.isIncreasedLineHeight(), "Increased line height should be disabled by default");
        assertEquals(1.2, s.getLineHeightMultiplier(), 0.001);
        assertTrue(s.isCodeVisionHints(), "Code vision hints should be enabled by default");
        assertTrue(s.isFormatCode(), "Format code should be enabled by default");
        assertTrue(s.isUseActiveScheme(), "Use active scheme should be selected by default");
        assertEquals("Default IDE", s.getChosenScheme());
        assertFalse(manager.isModified(), "Initial state should not be modified");
    }

    @Test
    void testReaderModeDirtyTrackingAndLifecycle() {
        ReaderModeManager manager = ReaderModeManager.getInstance();
        assertFalse(manager.isModified());

        // Modify font ligatures
        manager.getWorkingSettings().setFontLigatures(true);
        assertTrue(manager.isModified(), "Manager should be modified after setting font ligatures");

        // Apply changes
        manager.apply();
        assertFalse(manager.isModified(), "Manager should not be modified after apply");
        assertTrue(manager.getCommittedSettings().isFontLigatures());

        // Modify rendered docs
        manager.getWorkingSettings().setRenderedDocs(false);
        assertTrue(manager.isModified());

        // Reset changes
        manager.reset();
        assertFalse(manager.isModified(), "Manager should not be modified after reset");
        assertTrue(manager.getWorkingSettings().isRenderedDocs(), "Rendered docs should revert to true");
        assertTrue(manager.getWorkingSettings().isFontLigatures(), "Committed ligatures state should remain true");
    }

    @Test
    void testReaderModeDynamicOptionProvider() {
        ReaderModeManager manager = ReaderModeManager.getInstance();
        int initialCount = manager.getAllContributedOptions().size();

        ReaderModeOption customOpt = ReaderModeOption.of("custom.spellcheck", "Reader Spellchecking", "Highlight unknown terms", true);
        ReaderModeOptionProvider provider = new ReaderModeOptionProvider() {
            @Override
            public String getProviderName() {
                return "Custom Test Provider";
            }

            @Override
            public List<ReaderModeOption> getOptions() {
                return List.of(customOpt);
            }
        };

        manager.registerOptionProvider(provider);
        List<ReaderModeOption> options = manager.getAllContributedOptions();
        assertEquals(initialCount + 1, options.size());
        assertTrue(options.stream().anyMatch(o -> "custom.spellcheck".equals(o.id())));

        manager.unregisterOptionProvider(provider);
        assertEquals(initialCount, manager.getAllContributedOptions().size());
    }

    @Test
    void testReaderModeDynamicSchemeProvider() {
        ReaderModeManager manager = ReaderModeManager.getInstance();

        ReaderModeSchemeProvider customSchemeProvider = new ReaderModeSchemeProvider() {
            @Override
            public String getActiveSchemeName() {
                return "Lumina Dark";
            }

            @Override
            public List<String> getAvailableSchemes() {
                return List.of("Lumina Dark", "Lumina Light", "Default IDE", "Project");
            }
        };

        manager.registerSchemeProvider(customSchemeProvider);
        assertEquals("Lumina Dark", manager.getActiveSchemeName());
        assertTrue(manager.getAvailableSchemes().contains("Lumina Dark"));
        assertTrue(manager.getAvailableSchemes().contains("Lumina Light"));

        manager.unregisterSchemeProvider(customSchemeProvider);
    }

    @Test
    void testReaderModeUiPage() {
        if (!javaFxAvailable) return;

        SettingsReaderModePage page = new SettingsReaderModePage();
        assertTrue(page.getEnableReaderModeCheckBox().isSelected());
        assertTrue(page.getRenderedDocsCheckBox().isSelected());
        assertFalse(page.getErrorHighlightingCheckBox().isSelected());
        assertTrue(page.getUseActiveSchemeRadio().isSelected());
        assertTrue(page.getSchemeComboBox().isDisable(), "Scheme combo should be disabled when active scheme is selected");

        // Toggle choose scheme radio
        page.getChooseSchemeRadio().setSelected(true);
        assertFalse(page.getSchemeComboBox().isDisable(), "Scheme combo should be enabled when choose scheme is selected");

        // Toggle disable reader mode
        page.getEnableReaderModeCheckBox().setSelected(false);
        assertTrue(page.isModified());

        page.reset();
        assertFalse(page.isModified());
        assertTrue(page.getEnableReaderModeCheckBox().isSelected());
    }

    // =========================================================================
    // 2. TextMate Bundles Tests
    // =========================================================================

    @Test
    void testTextMate62BuiltInCatalogCompleteness() {
        TextMateBundlesManager manager = TextMateBundlesManager.getInstance();
        List<TextMateBundle> bundles = manager.getWorkingBundles();

        assertEquals(62, bundles.size(), "Catalog must contain exactly 62 built-in TextMate bundles from reference screenshots");

        List<String> expectedNames = List.of(
                "adoc", "bat", "bicep", "bicepparam", "clojure",
                "cmake", "coffeescript", "cpp", "csharp", "css",
                "dart", "diff", "docker", "erlang", "fsharp",
                "git-base", "go", "groovy", "handlebars", "hcl",
                "hlsl", "html", "ini", "java", "javascript",
                "json", "jsp", "julia", "kconfig", "kotlin",
                "latex", "less", "log", "lua", "make",
                "markdown-basics", "markdown-math", "mdx", "objective-c", "perl",
                "php", "powershell", "pug", "python", "r",
                "razor", "restructuredtext", "ruby", "rust", "scss",
                "search-result", "shaderlab", "shellscript", "sql", "swift",
                "terraform", "twig", "typescript-basics", "vb", "viml",
                "xml", "yaml"
        );

        for (int i = 0; i < expectedNames.size(); i++) {
            String exp = expectedNames.get(i);
            TextMateBundle actual = bundles.get(i);
            assertEquals(exp, actual.getName(), "Bundle at index " + i + " must match expected name");
            assertTrue(actual.isBuiltIn(), "Bundle " + exp + " must be built-in");
            assertTrue(actual.isEnabled(), "Bundle " + exp + " must be enabled by default");
        }
    }

    @Test
    void testTextMateBuiltInRemovalProtection() {
        TextMateBundlesManager manager = TextMateBundlesManager.getInstance();
        assertFalse(manager.removeBundle("adoc"), "Built-in bundle adoc cannot be removed");
        assertFalse(manager.removeBundle("python"), "Built-in bundle python cannot be removed");
        assertFalse(manager.removeBundle("yaml"), "Built-in bundle yaml cannot be removed");

        assertEquals(62, manager.getWorkingBundles().size(), "Bundle count must remain 62");
    }

    @Test
    void testTextMateCustomBundleAddAndRemove() {
        TextMateBundlesManager manager = TextMateBundlesManager.getInstance();

        // Add custom bundle
        boolean added = manager.addCustomBundle("custom-zig", "/opt/bundles/zig.tmbundle");
        assertTrue(added, "Should successfully add custom bundle");
        assertEquals(63, manager.getWorkingBundles().size());

        Optional<TextMateBundle> zig = manager.findBundle("custom-zig");
        assertTrue(zig.isPresent());
        assertFalse(zig.get().isBuiltIn(), "Custom bundle must not be built-in");
        assertEquals("/opt/bundles/zig.tmbundle", zig.get().getPath());
        assertTrue(zig.get().isEnabled());

        // Duplicate rejection
        assertFalse(manager.addCustomBundle("custom-zig", "/other/path"), "Duplicate bundle must be rejected");

        // Remove custom bundle
        assertTrue(manager.removeBundle("custom-zig"), "Should successfully remove custom bundle");
        assertEquals(62, manager.getWorkingBundles().size());
        assertTrue(manager.findBundle("custom-zig").isEmpty());
    }

    @Test
    void testTextMateBundleTogglingAndDirtyTracking() {
        TextMateBundlesManager manager = TextMateBundlesManager.getInstance();
        assertFalse(manager.isModified());

        manager.setBundleEnabled("rust", false);
        assertTrue(manager.isModified());
        assertFalse(manager.findBundle("rust").get().isEnabled());

        manager.apply();
        assertFalse(manager.isModified());
        assertFalse(manager.findBundle("rust").get().isEnabled());

        manager.setBundleEnabled("rust", true);
        assertTrue(manager.isModified());

        manager.reset();
        assertFalse(manager.isModified());
        assertFalse(manager.findBundle("rust").get().isEnabled(), "Should revert to false after reset");
    }

    @Test
    void testTextMateDynamicProvider() {
        TextMateBundlesManager manager = TextMateBundlesManager.getInstance();

        TextMateBundleProvider provider = new TextMateBundleProvider() {
            @Override
            public String getProviderName() {
                return "Elixir Provider";
            }

            @Override
            public List<TextMateBundle> getBundles() {
                return List.of(TextMateBundle.builtIn("elixir"));
            }
        };

        manager.registerProvider(provider);
        assertEquals(63, manager.getWorkingBundles().size());
        assertTrue(manager.findBundle("elixir").isPresent());

        manager.unregisterProvider(provider);
        assertEquals(62, manager.getWorkingBundles().size());
    }

    @Test
    void testTextMateUiPage() {
        if (!javaFxAvailable) return;

        SettingsTextMateBundlesPage page = new SettingsTextMateBundlesPage();
        assertEquals(62, page.getTableData().size());
        assertTrue(page.getRemoveBtn().isDisable(), "Remove button must initially be disabled");

        // Select a built-in bundle
        page.getTableView().getSelectionModel().select(0);
        assertTrue(page.getRemoveBtn().isDisable(), "Remove button must be disabled for built-in bundles");
    }

    // =========================================================================
    // 3. TODO Settings Tests
    // =========================================================================

    @Test
    void testTodoDefaultSettings() {
        TodoSettingsManager manager = TodoSettingsManager.getInstance();

        assertTrue(manager.isTreatIndentedText(), "Treat indented text should be enabled by default");
        List<TodoPattern> patterns = manager.getWorkingPatterns();
        assertEquals(2, patterns.size(), "Should have exactly 2 default patterns");

        TodoPattern p1 = patterns.get(0);
        assertEquals("\\btodo\\b.*", p1.getPattern());
        assertFalse(p1.isCaseSensitive());
        assertEquals(TodoIconType.TODO, p1.getIconType());
        assertTrue(p1.isBuiltIn());

        TodoPattern p2 = patterns.get(1);
        assertEquals("\\bfixme\\b.*", p2.getPattern());
        assertFalse(p2.isCaseSensitive());
        assertEquals(TodoIconType.FIXME, p2.getIconType());
        assertTrue(p2.isBuiltIn());

        assertTrue(manager.getWorkingFilters().isEmpty(), "Filters list should be empty by default");
        assertFalse(manager.isModified());
    }

    @Test
    void testTodoPatternCrudAndValidation() {
        TodoSettingsManager manager = TodoSettingsManager.getInstance();

        // Valid pattern
        TodoPattern custom = TodoPattern.of("\\bOPTIMIZE\\b.*", true, TodoIconType.DEFAULT, false);
        assertTrue(manager.addPattern(custom));
        assertEquals(3, manager.getWorkingPatterns().size());
        assertTrue(manager.isModified());

        // Invalid regex
        TodoPattern invalid = TodoPattern.of("[unclosed(", false, TodoIconType.CUSTOM, false);
        assertFalse(manager.addPattern(invalid), "Invalid regex syntax must be rejected");

        // Update pattern
        custom.setPattern("\\b(OPTIMIZE|HACK)\\b.*");
        assertTrue(manager.updatePattern(custom.getId(), custom));

        // Remove pattern
        assertTrue(manager.removePattern(custom.getId()));
        assertEquals(2, manager.getWorkingPatterns().size());
    }

    @Test
    void testTodoFilterCrud() {
        TodoSettingsManager manager = TodoSettingsManager.getInstance();

        TodoFilter filter = new TodoFilter("f1", "Review Items", Set.of("todo-default-1"));
        assertTrue(manager.addFilter(filter));
        assertEquals(1, manager.getWorkingFilters().size());
        assertTrue(manager.isModified());

        // Update filter
        filter.setName("Important Reviews");
        assertTrue(manager.updateFilter(filter.getId(), filter));

        // Remove filter
        assertTrue(manager.removeFilter(filter.getId()));
        assertTrue(manager.getWorkingFilters().isEmpty());
    }

    @Test
    void testTodoDynamicProviders() {
        TodoSettingsManager manager = TodoSettingsManager.getInstance();

        TodoPatternProvider patternProvider = new TodoPatternProvider() {
            @Override
            public String getProviderName() {
                return "Security Provider";
            }

            @Override
            public List<TodoPattern> getPatterns() {
                return List.of(TodoPattern.of("\\bSECURITY\\b.*", false, TodoIconType.CUSTOM, true));
            }
        };

        manager.registerPatternProvider(patternProvider);
        assertEquals(3, manager.getWorkingPatterns().size());

        manager.unregisterPatternProvider(patternProvider);
        assertEquals(2, manager.getWorkingPatterns().size());
    }

    @Test
    void testTodoUiPage() {
        if (!javaFxAvailable) return;

        SettingsTodoPage page = new SettingsTodoPage();
        assertTrue(page.getTreatIndentedTextCheckBox().isSelected());
        assertEquals(2, page.getPatternsData().size());
        assertTrue(page.getFiltersData().isEmpty(), "Filters table should be empty");

        assertTrue(page.getRemovePatternBtn().isDisable());
        assertTrue(page.getEditPatternBtn().isDisable());
        assertTrue(page.getRemoveFilterBtn().isDisable());
        assertTrue(page.getEditFilterBtn().isDisable());

        page.getPatternsTable().getSelectionModel().select(0);
        assertFalse(page.getRemovePatternBtn().isDisable());
        assertFalse(page.getEditPatternBtn().isDisable());
    }

    @Test
    void testTodoPatternDialogSubordinationAndStyling() {
        if (!javaFxAvailable) return;

        TodoPatternDialog dialog = new TodoPatternDialog(null, null);
        assertNotNull(dialog.getPatternField());
        assertFalse(dialog.getCaseSensitiveBox().isSelected());
        assertTrue(dialog.getUseDefaultColorsBox().isSelected(), "Default colors checkbox should be selected by default");
        assertTrue(dialog.getCustomColorsGroup().isDisable(), "Custom colors group must be disabled when default colors is selected");

        // Uncheck default colors
        dialog.getUseDefaultColorsBox().setSelected(false);
        assertFalse(dialog.getCustomColorsGroup().isDisable(), "Custom colors group must become enabled when default colors is unchecked");

        assertNotNull(dialog.getIconCombo().getValue());
        assertNotNull(dialog.getOkBtn());
        assertNotNull(dialog.getCancelBtn());
    }

    @Test
    void testTodoFilterDialogUi() {
        if (!javaFxAvailable) return;

        List<TodoPattern> patterns = TodoSettingsManager.getInstance().getWorkingPatterns();
        TodoFilterDialog dialog = new TodoFilterDialog(null, null, patterns);

        assertNotNull(dialog.getNameField());
        assertEquals(patterns.size(), dialog.getTableData().size(), "Checklist must contain all available patterns");
        assertNotNull(dialog.getHelpBtn(), "Help button must exist");
        assertNotNull(dialog.getOkBtn());
        assertNotNull(dialog.getCancelBtn());
    }

    // =========================================================================
    // 4. SettingsDialog Integration Tests
    // =========================================================================

    @Test
    void testSettingsDialogGettersAndLifecycle() {
        if (!javaFxAvailable) return;

        SettingsDialog dialog = new SettingsDialog(null, "Reader Mode");
        assertNotNull(dialog);
        assertNotNull(dialog.getCurrentReaderModePage());

        SettingsDialog textMateDialog = new SettingsDialog(null, "TextMate Bundles");
        assertNotNull(textMateDialog);
        assertNotNull(textMateDialog.getCurrentTextMateBundlesPage());

        SettingsDialog todoDialog = new SettingsDialog(null, "TODO");
        assertNotNull(todoDialog);
        assertNotNull(todoDialog.getCurrentTodoPage());
    }

    // =========================================================================
    // 5. Strict Brand Isolation Test
    // =========================================================================

    @Test
    void testStrictBrandIsolation() throws Exception {
        List<String> filesToCheck = List.of(
                "src/main/java/dev/lumina/readermode/ReaderModeOption.java",
                "src/main/java/dev/lumina/readermode/ReaderModeOptionProvider.java",
                "src/main/java/dev/lumina/readermode/ReaderModeSchemeProvider.java",
                "src/main/java/dev/lumina/readermode/ReaderModeSettings.java",
                "src/main/java/dev/lumina/readermode/ReaderModeManager.java",
                "src/main/java/dev/lumina/textmate/TextMateBundle.java",
                "src/main/java/dev/lumina/textmate/TextMateBundleProvider.java",
                "src/main/java/dev/lumina/textmate/DefaultTextMateBundleProvider.java",
                "src/main/java/dev/lumina/textmate/TextMateBundlesManager.java",
                "src/main/java/dev/lumina/todo/TodoIconType.java",
                "src/main/java/dev/lumina/todo/TodoPattern.java",
                "src/main/java/dev/lumina/todo/TodoFilter.java",
                "src/main/java/dev/lumina/todo/TodoPatternProvider.java",
                "src/main/java/dev/lumina/todo/TodoFilterProvider.java",
                "src/main/java/dev/lumina/todo/DefaultTodoPatternProvider.java",
                "src/main/java/dev/lumina/todo/TodoSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsReaderModePage.java",
                "src/main/java/dev/lumina/ui/SettingsTextMateBundlesPage.java",
                "src/main/java/dev/lumina/ui/SettingsTodoPage.java",
                "src/main/java/dev/lumina/ui/TodoPatternDialog.java",
                "src/main/java/dev/lumina/ui/TodoFilterDialog.java"
        );

        List<String> forbiddenBrands = List.of(
                "jetbrains", "intellij", "grazie"
        );

        for (String filePath : filesToCheck) {
            File file = new File(filePath);
            assertTrue(file.exists(), "Target file must exist: " + filePath);
            String content = Files.readString(file.toPath()).toLowerCase(Locale.ROOT);

            for (String brand : forbiddenBrands) {
                assertFalse(content.contains(brand),
                        "File " + filePath + " contains forbidden brand name: " + brand);
            }
        }
    }
}
