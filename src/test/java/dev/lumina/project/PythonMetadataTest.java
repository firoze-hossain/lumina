package dev.lumina.project;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PythonMetadataTest {

    @Test
    void testSystemPythonDiscoveryReturnsResults() {
        List<PythonMetadata.PythonInstallation> installations = PythonMetadata.fetchPythonInstallations(false);
        assertNotNull(installations, "Python installations list must not be null");
        assertFalse(installations.isEmpty(), "At least one Python installation (or fallback) must be discovered");

        PythonMetadata.PythonInstallation first = installations.getFirst();
        assertNotNull(first.label(), "Label must not be null");
        assertNotNull(first.executable(), "Executable path must not be null");
        assertNotNull(first.type(), "Type must not be null");
        assertNotNull(first.displayLabel(), "Display label must not be null");
        assertTrue(first.displayLabel().contains(first.label()), "Display label should contain version label");
        assertTrue(first.displayLabel().contains(first.executable()), "Display label should contain executable path");
    }

    @Test
    void testUvVersionsListMatchesIntelliJ() {
        List<String> versions = PythonMetadata.fetchUvPythonVersions();
        assertNotNull(versions);
        assertEquals("Default", versions.getFirst(), "First item should be Default");
        assertTrue(versions.contains("3.14"));
        assertTrue(versions.contains("3.13"));
        assertTrue(versions.contains("3.12"));
        assertTrue(versions.contains("3.11"));
        assertTrue(versions.contains("3.10"));
        assertTrue(versions.contains("3.9"));
        assertTrue(versions.contains("3.8"));
    }

    @Test
    void testDetectUvPathNonNull() {
        String uvPath = PythonMetadata.detectUvPath();
        assertNotNull(uvPath, "Uv path should not be null");
        assertFalse(uvPath.isBlank(), "Uv path should not be blank");
    }

    @Test
    void testIsValidUv() {
        assertFalse(PythonMetadata.isValidUv(null));
        assertFalse(PythonMetadata.isValidUv(""));
        assertFalse(PythonMetadata.isValidUv("/non/existent/path/uv"));
    }

    @Test
    void testDetectCondaPathNonNull() {
        String condaPath = PythonMetadata.detectCondaPath();
        assertNotNull(condaPath, "Conda path should not be null");
        // May be empty if conda is not installed on the system (like in the IntelliJ screenshot)
    }

    @Test
    void testIsValidConda() {
        assertFalse(PythonMetadata.isValidConda(null));
        assertFalse(PythonMetadata.isValidConda(""));
        assertFalse(PythonMetadata.isValidConda("/non/existent/path/conda"));
    }

    @Test
    void testCondaVersionsMatchesIntelliJ() {
        List<String> versions = PythonMetadata.fetchCondaPythonVersions();
        assertNotNull(versions);
        assertTrue(versions.contains("3.12"));
        assertTrue(versions.contains("3.11"));
        assertTrue(versions.contains("3.10"));
        assertTrue(versions.contains("3.9"));
        assertTrue(versions.contains("3.8"));
    }

    @Test
    void testCustomEnvironmentTypesMatchIntelliJ() {
        // Generate new types matching screenshot 2
        assertEquals(List.of("Virtualenv", "Conda", "Pipenv", "Poetry", "uv", "Hatch"),
                PythonMetadata.CUSTOM_ENV_GENERATE_NEW_TYPES);

        // Select existing types matching screenshot 4
        assertEquals(List.of("Python", "Conda"),
                PythonMetadata.CUSTOM_ENV_SELECT_EXISTING_TYPES);
    }

    @Test
    void testMinicondaInstallerUrlMatchesPlatform() {
        String url = PythonMetadata.getMinicondaInstallerUrl();
        assertNotNull(url);
        assertTrue(url.startsWith("https://repo.anaconda.com/miniconda/Miniconda3-latest-"));
        assertTrue(url.endsWith(".sh") || url.endsWith(".exe") || url.endsWith(".pkg"));
    }

    @Test
    void testDefaultMinicondaInstallDir() {
        java.nio.file.Path dir = PythonMetadata.getDefaultMinicondaInstallDir();
        assertNotNull(dir);
        assertTrue(dir.toString().contains("miniconda3"));
    }

    @Test
    void testGetExpectedCondaBinary() {
        java.nio.file.Path installDir = java.nio.file.Path.of("/tmp/test_miniconda");
        java.nio.file.Path binary = PythonMetadata.getExpectedCondaBinary(installDir);
        assertNotNull(binary);
        assertTrue(binary.toString().startsWith(installDir.toString()));
        assertTrue(binary.toString().contains("conda"));
    }
}

