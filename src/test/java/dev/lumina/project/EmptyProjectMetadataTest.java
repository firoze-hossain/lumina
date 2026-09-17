package dev.lumina.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class EmptyProjectMetadataTest {

    @Test
    void testSuggestUniqueProjectName(@TempDir Path tempDir) throws IOException {
        // When no projects exist
        assertEquals("untitled", EmptyProjectMetadata.suggestUniqueProjectName(tempDir, "untitled"));

        // When "untitled" exists
        Files.createDirectory(tempDir.resolve("untitled"));
        assertEquals("untitled1", EmptyProjectMetadata.suggestUniqueProjectName(tempDir, "untitled"));

        // When "untitled" and "untitled1" exist
        Files.createDirectory(tempDir.resolve("untitled1"));
        assertEquals("untitled2", EmptyProjectMetadata.suggestUniqueProjectName(tempDir, "untitled"));

        // Null safety
        assertEquals("untitled", EmptyProjectMetadata.suggestUniqueProjectName(null, null));
        assertEquals("myproject", EmptyProjectMetadata.suggestUniqueProjectName(tempDir, "myproject"));
    }

    @Test
    void testGenerateIdeaModulesXml() {
        String xml = EmptyProjectMetadata.generateIdeaModulesXml();
        assertNotNull(xml);
        assertTrue(xml.contains("<modules />") || xml.contains("<modules>"));
        assertTrue(xml.contains("ProjectModuleManager"));
    }

    @Test
    void testGenerateIdeaMiscXml() {
        String xml = EmptyProjectMetadata.generateIdeaMiscXml();
        assertNotNull(xml);
        assertTrue(xml.contains("ProjectRootManager"));
    }

    @Test
    void testGenerateIdeaVcsXml() {
        String xml = EmptyProjectMetadata.generateIdeaVcsXml();
        assertNotNull(xml);
        assertTrue(xml.contains("VcsDirectoryMappings"));
        assertTrue(xml.contains("vcs=\"Git\""));
    }

    @Test
    void testGenerateIdeaGitIgnore() {
        String gitignore = EmptyProjectMetadata.generateIdeaGitIgnore();
        assertNotNull(gitignore);
        assertTrue(gitignore.contains("/shelf/"));
        assertTrue(gitignore.contains("/workspace.xml"));
    }

    @Test
    void testGenerateRootGitIgnore() {
        String gitignore = EmptyProjectMetadata.generateRootGitIgnore();
        assertNotNull(gitignore);
        assertTrue(gitignore.contains(".idea/"));
    }
}
