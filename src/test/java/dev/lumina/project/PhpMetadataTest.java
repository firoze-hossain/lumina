package dev.lumina.project;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhpMetadataTest {

    @Test
    void testDetectPhpPath() {
        String phpPath = PhpMetadata.detectPhpPath();
        assertNotNull(phpPath, "PHP path should not be null");
        // On systems with PHP installed (like the test runner), it should find the binary
        if (!phpPath.isBlank()) {
            assertTrue(phpPath.contains("php"), "Path should contain php: " + phpPath);
        }
    }

    @Test
    void testDetectPhpLanguageLevel() {
        String level = PhpMetadata.detectPhpLanguageLevel();
        assertNotNull(level);
        assertFalse(level.isBlank());
        assertTrue(level.matches("\\d+\\.\\d+"), "PHP language level should match major.minor (e.g. 8.3): " + level);
    }

    @Test
    void testDetectComposerPath() {
        String composerPath = PhpMetadata.detectComposerPath();
        assertNotNull(composerPath, "Composer path should not be null");
        if (!composerPath.isBlank()) {
            assertTrue(composerPath.contains("composer"), "Path should contain composer: " + composerPath);
        }
    }

    @Test
    void testGenerateComposerJson() {
        String json = PhpMetadata.generateComposerJson("my-php-app");
        assertNotNull(json);
        assertTrue(json.contains("\"name\":"));
        assertTrue(json.contains("/my-php-app\""));
        assertTrue(json.contains("\"type\": \"project\""));
        assertTrue(json.contains("\"require\": {}"));
    }

    @Test
    void testGenerateIdeaPhpXml() {
        String xml = PhpMetadata.generateIdeaPhpXml("8.3");
        assertNotNull(xml);
        assertTrue(xml.contains("php_language_level=\"8.3\""));
        assertTrue(xml.contains("PhpProjectSharedConfiguration"));
    }

    @Test
    void testGenerateIdeaIml() {
        String iml = PhpMetadata.generateIdeaIml();
        assertNotNull(iml);
        assertTrue(iml.contains("type=\"WEB_MODULE\""));
        assertTrue(iml.contains("url=\"file://$MODULE_DIR$/vendor\""));
    }

    @Test
    void testGenerateGitignore() {
        String gitignore = PhpMetadata.generateGitignore();
        assertNotNull(gitignore);
        assertTrue(gitignore.contains("/vendor/"));
        assertTrue(gitignore.contains(".idea/"));
        assertTrue(gitignore.contains("composer.phar"));
    }

    @Test
    void testGenerateStarterIndexPhp() {
        String code = PhpMetadata.generateStarterIndexPhp();
        assertNotNull(code);
        assertTrue(code.contains("<?php"));
        assertTrue(code.contains("echo \"Hello, World!\";"));
    }
}
