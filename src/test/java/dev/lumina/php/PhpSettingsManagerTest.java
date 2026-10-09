package dev.lumina.php;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PhpSettingsManagerTest {

    private PhpSettingsManager manager;

    @BeforeEach
    void setUp() {
        manager = PhpSettingsManager.getInstance();
        manager.resetDefaults();
    }

    @Test
    void testDefaultValues() {
        // PHP language level defaults to 5.6 (matching Screenshot 2)
        assertEquals(PhpLanguageLevel.PHP_5_6, manager.getLanguageLevel());
        assertEquals("5.6 (variadic functions, argument unpacking)", manager.getLanguageLevel().getDisplayName());

        // Interpreters default populated
        assertFalse(manager.getInterpreters().isEmpty());
        assertNotNull(manager.getActiveInterpreter());

        // Include paths default empty (Nothing to show)
        assertTrue(manager.getIncludePaths().isEmpty());

        // Runtime extensions populated with categories
        List<PhpRuntimeExtension> exts = manager.getRuntimeExtensions();
        assertFalse(exts.isEmpty());
        assertTrue(exts.stream().anyMatch(e -> "Core".equals(e.getCategory()) && e.isEnabled()));
        assertTrue(exts.stream().anyMatch(e -> "Bundled".equals(e.getCategory()) && e.isEnabled()));
        assertTrue(exts.stream().anyMatch(e -> "External".equals(e.getCategory())));
        assertTrue(exts.stream().anyMatch(e -> "PECL".equals(e.getCategory())));
        assertTrue(exts.stream().anyMatch(e -> "Others".equals(e.getCategory())));

        // Analysis settings defaults (matching Screenshot 4)
        PhpAnalysisSettings analysis = manager.getAnalysisSettings();
        assertEquals("1", analysis.getCallTreeAnalysisDepth());
        assertTrue(analysis.isSkipCallsWithConstantParams());
        assertEquals(List.of("\\RuntimeException", "\\LogicException", "\\Error"), analysis.getUncheckedExceptions());
        assertEquals("$_SERVER['DOCUMENT_ROOT']", analysis.getDocumentRoot());

        // Composer files default empty (Nothing to show)
        assertTrue(manager.getComposerFiles().isEmpty());
    }

    @Test
    void testLanguageLevelPersistence() {
        manager.setLanguageLevel(PhpLanguageLevel.PHP_8_3);
        assertEquals(PhpLanguageLevel.PHP_8_3, manager.getLanguageLevel());

        manager.loadSettings();
        assertEquals(PhpLanguageLevel.PHP_8_3, manager.getLanguageLevel());

        manager.setLanguageLevel(PhpLanguageLevel.PHP_5_6);
        assertEquals(PhpLanguageLevel.PHP_5_6, manager.getLanguageLevel());
    }

    @Test
    void testInterpreterManagement() {
        PhpInterpreter custom = new PhpInterpreter("custom-php-8.4", "/usr/local/bin/php (8.4.0)", "/usr/local/bin/php", "8.4.0");
        manager.addOrUpdateInterpreter(custom);
        manager.setActiveInterpreterId("custom-php-8.4");

        assertEquals("custom-php-8.4", manager.getActiveInterpreterId());
        assertEquals("/usr/local/bin/php", manager.getActiveInterpreter().getPath());
        assertEquals("8.4.0", manager.getActiveInterpreter().getPhpVersion());

        manager.loadSettings();
        assertEquals("custom-php-8.4", manager.getActiveInterpreterId());
    }

    @Test
    void testIncludePaths() {
        manager.addIncludePath("/opt/vendor/lib");
        manager.addIncludePath("/var/www/shared");
        // Non-duplicate test
        manager.addIncludePath("/opt/vendor/lib");

        assertEquals(2, manager.getIncludePaths().size());
        assertTrue(manager.getIncludePaths().contains("/opt/vendor/lib"));
        assertTrue(manager.getIncludePaths().contains("/var/www/shared"));

        manager.removeIncludePath("/opt/vendor/lib");
        assertEquals(1, manager.getIncludePaths().size());
        assertFalse(manager.getIncludePaths().contains("/opt/vendor/lib"));
    }

    @Test
    void testRuntimeExtensionsToggleAndSync() {
        manager.setExtensionEnabled("v8js", true);
        assertTrue(manager.getRuntimeExtensions().stream().anyMatch(e -> "v8js".equals(e.getName()) && e.isEnabled()));

        int count = manager.syncExtensionsWithInterpreter(manager.getActiveInterpreter());
        assertTrue(count >= 0);
    }

    @Test
    void testAnalysisSettingsModification() {
        PhpAnalysisSettings custom = new PhpAnalysisSettings(
                "3",
                false,
                List.of("\\RuntimeException", "\\CustomException"),
                "/var/www/html/public"
        );
        manager.setAnalysisSettings(custom);

        PhpAnalysisSettings loaded = manager.getAnalysisSettings();
        assertEquals("3", loaded.getCallTreeAnalysisDepth());
        assertFalse(loaded.isSkipCallsWithConstantParams());
        assertEquals(List.of("\\RuntimeException", "\\CustomException"), loaded.getUncheckedExceptions());
        assertEquals("/var/www/html/public", loaded.getDocumentRoot());
    }

    @Test
    void testComposerFilesManagement() {
        PhpComposerFileConfig file1 = new PhpComposerFileConfig("comp-1", "/projects/app/composer.json");
        manager.addComposerFile(file1);

        assertEquals(1, manager.getComposerFiles().size());
        assertEquals("/projects/app/composer.json", manager.getComposerFiles().get(0).getPath());

        manager.removeComposerFile("comp-1");
        assertTrue(manager.getComposerFiles().isEmpty());
    }

    @Test
    void testResetDefaults() {
        manager.setLanguageLevel(PhpLanguageLevel.PHP_8_1);
        manager.addIncludePath("/some/path");
        manager.setCustomStubsPath("/custom/stubs");

        manager.resetDefaults();

        assertEquals(PhpLanguageLevel.PHP_5_6, manager.getLanguageLevel());
        assertTrue(manager.getIncludePaths().isEmpty());
        assertEquals("", manager.getCustomStubsPath());
    }

    @Test
    void testQualityToolsSettingsPersistence() {
        PhpQualityToolsSettings qs = manager.getQualityToolsSettings();
        assertNotNull(qs);
        assertEquals("none", qs.getExternalFormatter());
        assertFalse(qs.getCodeSniffer().isInspectionEnabled());
        assertFalse(qs.getCsFixer().isInspectionEnabled());
        assertFalse(qs.getLaravelPint().isInspectionEnabled());
        assertFalse(qs.getMessDetector().isInspectionEnabled());

        qs.setExternalFormatter("laravel_pint");
        qs.getCodeSniffer().setInspectionEnabled(true);
        qs.getCodeSniffer().setPhpcsPath("/usr/bin/phpcs");
        qs.getCsFixer().setInspectionEnabled(true);
        qs.getCsFixer().setRuleset("Symfony");
        qs.getLaravelPint().setInspectionEnabled(true);
        qs.getLaravelPint().setPathToPintJson("/app/pint.json");
        qs.getMessDetector().setInspectionEnabled(true);
        qs.getMessDetector().setCodeSizeRules(true);

        manager.setQualityToolsSettings(qs);

        manager.loadSettings();
        PhpQualityToolsSettings loaded = manager.getQualityToolsSettings();
        assertEquals("laravel_pint", loaded.getExternalFormatter());
        assertTrue(loaded.getCodeSniffer().isInspectionEnabled());
        assertEquals("/usr/bin/phpcs", loaded.getCodeSniffer().getPhpcsPath());
        assertTrue(loaded.getCsFixer().isInspectionEnabled());
        assertEquals("Symfony", loaded.getCsFixer().getRuleset());
        assertTrue(loaded.getLaravelPint().isInspectionEnabled());
        assertEquals("/app/pint.json", loaded.getLaravelPint().getPathToPintJson());
        assertTrue(loaded.getMessDetector().isInspectionEnabled());
        assertTrue(loaded.getMessDetector().isCodeSizeRules());

        manager.resetDefaults();
        PhpQualityToolsSettings reset = manager.getQualityToolsSettings();
        assertEquals("none", reset.getExternalFormatter());
        assertFalse(reset.getCodeSniffer().isInspectionEnabled());
    }

    @Test
    void testDynamicDetectors() {
        assertNotNull(manager.detectPhpcsPath());
        assertNotNull(manager.detectPhpcbfPath());
        assertNotNull(manager.detectPhpCsFixerPath());
        assertNotNull(manager.detectLaravelPintPath());
        assertNotNull(manager.detectPintJsonPath());
        assertNotNull(manager.detectPhpmdPath());
    }
}
