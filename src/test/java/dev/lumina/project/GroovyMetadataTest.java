package dev.lumina.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GroovyMetadataTest {

    @Test
    void testGroovyGroupId() {
        assertEquals("org.codehaus.groovy", GroovyMetadata.getGroovyGroupId("2.4.21"));
        assertEquals("org.codehaus.groovy", GroovyMetadata.getGroovyGroupId("2.5.23"));
        assertEquals("org.apache.groovy", GroovyMetadata.getGroovyGroupId("3.0.25"));
        assertEquals("org.apache.groovy", GroovyMetadata.getGroovyGroupId("4.0.33"));
        assertEquals("org.apache.groovy", GroovyMetadata.getGroovyGroupId("5.1.1"));
        assertEquals("org.apache.groovy", GroovyMetadata.getGroovyGroupId("6.0.0-beta-3"));
    }

    @Test
    void testSelectLatestPerBranchIncludesSpecifyHome() {
        List<String> input = List.of(
                "2.4.20", "2.4.21",
                "2.5.22", "2.5.23",
                "3.0.24", "3.0.25",
                "4.0.32", "4.0.33",
                "5.0.7", "5.0.8",
                "5.1.0", "5.1.1",
                "6.0.0-beta-2", "6.0.0-beta-3"
        );

        List<String> result = GroovyMetadata.selectLatestPerBranch(input);

        assertTrue(result.contains("5.1.1"));
        assertTrue(result.contains("5.0.8"));
        assertTrue(result.contains("4.0.33"));
        assertTrue(result.contains("3.0.25"));
        assertTrue(result.contains("2.5.23"));
        assertTrue(result.contains("2.4.21"));
        assertTrue(result.contains("6.0.0-beta-3"));
        assertTrue(result.contains(GroovyMetadata.SPECIFY_HOME_OPTION));

        // Ensure 5.1.1 is at the top (latest stable)
        assertEquals("5.1.1", result.getFirst());
        // Ensure specify home is at the bottom
        assertEquals(GroovyMetadata.SPECIFY_HOME_OPTION, result.get(result.size() - 1));
    }

    @Test
    void testDetectGroovyVersionFromHome(@TempDir Path tempDir) throws IOException {
        // 1. Directory with lib/groovy-5.1.1.jar
        Path sdkHome = tempDir.resolve("groovy-5.1.1");
        Path libDir = Files.createDirectories(sdkHome.resolve("lib"));
        Files.createFile(libDir.resolve("groovy-5.1.1.jar"));
        Files.createFile(libDir.resolve("groovy-ant-5.1.1.jar"));
        Files.createFile(libDir.resolve("groovy-5.1.1-javadoc.jar"));

        String detected = GroovyMetadata.detectGroovyVersionFromHome(sdkHome);
        assertEquals("5.1.1", detected);

        // 2. Directory with lib/groovy-all-2.4.21.jar
        Path oldHome = tempDir.resolve("groovy-2.4.21");
        Path oldLib = Files.createDirectories(oldHome.resolve("lib"));
        Files.createFile(oldLib.resolve("groovy-all-2.4.21.jar"));

        String oldDetected = GroovyMetadata.detectGroovyVersionFromHome(oldHome);
        assertEquals("2.4.21", oldDetected);

        // 3. Null or non-existent
        assertNull(GroovyMetadata.detectGroovyVersionFromHome(null));
        assertNull(GroovyMetadata.detectGroovyVersionFromHome(tempDir.resolve("does-not-exist")));
    }
}
