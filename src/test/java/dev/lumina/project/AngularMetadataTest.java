package dev.lumina.project;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AngularMetadataTest {

    @Test
    void testConstantsAndFallbacks() {
        assertEquals("@angular/cli", AngularMetadata.PACKAGE_NAME);
        assertEquals("22.1.8", AngularMetadata.DEFAULT_VERSION);
        assertEquals("npx --package @angular/cli ng", AngularMetadata.CLI_PREFIX);
        assertEquals("Select...", AngularMetadata.ACTION_SELECT);

        List<String> fallbacks = AngularMetadata.FALLBACK_VERSIONS;
        assertNotNull(fallbacks);
        assertFalse(fallbacks.isEmpty());
        assertTrue(fallbacks.contains("22.1.8"));
        assertTrue(fallbacks.contains("19.2.0"));
        assertTrue(fallbacks.contains("18.2.0"));
        assertTrue(fallbacks.contains("17.3.0"));
    }

    @Test
    void testFormatCliDisplay() {
        String formatted = AngularMetadata.formatCliDisplay("22.1.8");
        assertNotNull(formatted);
        assertTrue(formatted.startsWith("npx --package @angular/cli ng"));
        assertTrue(formatted.endsWith("22.1.8"));

        String defaultFormatted = AngularMetadata.formatCliDisplay(null);
        assertTrue(defaultFormatted.endsWith(AngularMetadata.DEFAULT_VERSION));

        String blankFormatted = AngularMetadata.formatCliDisplay("   ");
        assertTrue(blankFormatted.endsWith(AngularMetadata.DEFAULT_VERSION));
    }

    @Test
    void testParseVersionFromDisplay() {
        assertEquals("22.1.8", AngularMetadata.parseVersionFromDisplay(
                "npx --package @angular/cli ng                         22.1.8"));
        assertEquals("20.1.0", AngularMetadata.parseVersionFromDisplay("20.1.0"));
        assertEquals("22.1.8", AngularMetadata.parseVersionFromDisplay("Select..."));
        assertEquals("22.1.8", AngularMetadata.parseVersionFromDisplay(null));
        assertEquals("22.1.8", AngularMetadata.parseVersionFromDisplay(""));
    }

    @Test
    void testDetectAngularClis() {
        List<AngularMetadata.AngularCliEntry> entries = AngularMetadata.detectAngularClis();
        assertNotNull(entries);
        assertTrue(entries.size() >= 2, "Should have at least npx entry and Select... action");

        AngularMetadata.AngularCliEntry first = entries.getFirst();
        assertEquals("npx --package @angular/cli ng", first.runnerPrefix());
        assertFalse(first.isAction());
        assertNotNull(first.version());
        assertFalse(first.version().isBlank());

        AngularMetadata.AngularCliEntry last = entries.getLast();
        assertEquals("Select...", last.runnerPrefix());
        assertTrue(last.isAction());
    }

    @Test
    void testFetchAllVersions() {
        List<String> versions = AngularMetadata.fetchAllVersions(false);
        assertNotNull(versions);
        assertFalse(versions.isEmpty());
        assertTrue(versions.contains("22.1.8"));
    }

    @Test
    void testNodeMetadataLinuxCandidates() {
        List<NodeMetadata.NodeInterpreter> interpreters = NodeMetadata.detectInterpreters(false);
        assertNotNull(interpreters);
        assertFalse(interpreters.isEmpty());

        boolean hasNode = interpreters.stream().anyMatch(i -> i.path().contains("node"));
        assertTrue(hasNode, "Should detect at least one Node installation");
    }
}
