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
}
