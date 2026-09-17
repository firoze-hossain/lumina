package dev.lumina.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GoMetadataTest {

    @Test
    void testDiscoverGoRoots() {
        List<GoMetadata.GoSdk> sdks = GoMetadata.discoverGoRoots();
        assertNotNull(sdks);
    }

    @Test
    void testIsValidGoRoot(@TempDir Path tempDir) throws IOException {
        assertFalse(GoMetadata.isValidGoRoot(tempDir.toFile()));

        // Create valid Go root with bin/go and VERSION
        Path bin = tempDir.resolve("bin");
        Files.createDirectories(bin);
        Path goExe = bin.resolve(System.getProperty("os.name", "").toLowerCase().contains("win") ? "go.exe" : "go");
        Files.writeString(goExe, "#!/bin/sh\n", StandardCharsets.UTF_8);
        goExe.toFile().setExecutable(true);

        assertTrue(GoMetadata.isValidGoRoot(tempDir.toFile()));

        // Test VERSION + src/runtime structure
        Path tempDir2 = Files.createTempDirectory("go-root-test");
        try {
            Files.writeString(tempDir2.resolve("VERSION"), "go1.24.1\n", StandardCharsets.UTF_8);
            Files.createDirectories(tempDir2.resolve("src"));
            assertTrue(GoMetadata.isValidGoRoot(tempDir2.toFile()));
        } finally {
            deleteRecursively(tempDir2);
        }
    }

    @Test
    void testDetectGoVersion(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("VERSION"), "go1.24.1\n", StandardCharsets.UTF_8);
        String version = GoMetadata.detectGoVersion(tempDir.toString());
        assertEquals("go1.24.1", version);
    }

    @Test
    void testExtractLanguageVersion() {
        assertEquals("1.24", GoMetadata.extractLanguageVersion("go1.24.1"));
        assertEquals("1.23", GoMetadata.extractLanguageVersion("go1.23.0"));
        assertEquals("1.22", GoMetadata.extractLanguageVersion("1.22.5"));
        assertEquals("1.24", GoMetadata.extractLanguageVersion(""));
    }

    @Test
    void testFetchAvailableReleases() {
        List<GoMetadata.GoRelease> stable = GoMetadata.fetchAvailableReleases(false);
        assertNotNull(stable);
        assertFalse(stable.isEmpty());
        assertTrue(stable.size() <= 4, "Default view should show top stable releases matching IntelliJ");

        List<GoMetadata.GoRelease> all = GoMetadata.fetchAvailableReleases(true);
        assertNotNull(all);
        assertTrue(all.size() >= stable.size());
    }

    @Test
    void testResolveDownloadUrl() {
        String url = GoMetadata.resolveDownloadUrl("go1.24.1");
        assertNotNull(url);
        assertTrue(url.startsWith("https://go.dev/dl/go1.24.1."));
        assertTrue(url.endsWith(".tar.gz") || url.endsWith(".zip"));
    }

    @Test
    void testGenerateIdeaModulesXml() {
        String xml = GoMetadata.generateIdeaModulesXml("my_go_app");
        assertNotNull(xml);
        assertTrue(xml.contains("ProjectModuleManager"));
        assertTrue(xml.contains("my_go_app.iml"));
    }

    @Test
    void testGenerateIdeaIml() {
        String iml = GoMetadata.generateIdeaIml();
        assertNotNull(iml);
        assertTrue(iml.contains("WEB_MODULE"));
        assertTrue(iml.contains("<component name=\"Go\" enabled=\"true\" />"));
    }

    @Test
    void testGenerateIdeaMiscXml() {
        String misc = GoMetadata.generateIdeaMiscXml("go1.24.1");
        assertNotNull(misc);
        assertTrue(misc.contains("project-jdk-name=\"Go 1.24.1\""));
        assertTrue(misc.contains("project-jdk-type=\"Go SDK\""));
    }

    @Test
    void testGenerateGitIgnore() {
        String gitignore = GoMetadata.generateGitIgnore();
        assertNotNull(gitignore);
        assertTrue(gitignore.contains("go.work"));
        assertTrue(gitignore.contains("*.exe"));
    }

    private void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var s = Files.list(path)) {
                for (Path p : s.toList()) {
                    deleteRecursively(p);
                }
            }
        }
        Files.deleteIfExists(path);
    }
}
