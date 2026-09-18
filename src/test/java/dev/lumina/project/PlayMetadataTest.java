package dev.lumina.project;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayMetadataTest {

    @Test
    void testParsePlayVersionsXml() {
        String mockXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <metadata>
                  <groupId>org.playframework</groupId>
                  <artifactId>play_3</artifactId>
                  <versioning>
                    <latest>3.1.0-M9</latest>
                    <release>3.1.0-M9</release>
                    <versions>
                      <version>3.0.0-M1</version>
                      <version>3.0.0</version>
                      <version>3.0.1</version>
                      <version>3.0.4-M1</version>
                      <version>3.0.4</version>
                      <version>3.0.8</version>
                      <version>3.0.9</version>
                      <version>3.0.10</version>
                      <version>3.0.11</version>
                      <version>3.1.0-M1</version>
                      <version>3.1.0-RC1</version>
                    </versions>
                  </versioning>
                </metadata>
                """;

        List<String> versions = PlayMetadata.parsePlayVersionsXml(mockXml);

        assertNotNull(versions);
        assertFalse(versions.isEmpty());

        // Verify latest stable first
        assertEquals("3.0.11", versions.getFirst());

        // Verify order is descending
        assertEquals(List.of("3.0.11", "3.0.10", "3.0.9", "3.0.8", "3.0.4", "3.0.1", "3.0.0"), versions);

        // Verify milestones and release candidates are filtered out
        assertFalse(versions.contains("3.0.0-M1"));
        assertFalse(versions.contains("3.0.4-M1"));
        assertFalse(versions.contains("3.1.0-M1"));
        assertFalse(versions.contains("3.1.0-RC1"));
    }

    @Test
    void testFallbackVersions() {
        assertNotNull(PlayMetadata.PLAY_FALLBACK_VERSIONS);
        assertFalse(PlayMetadata.PLAY_FALLBACK_VERSIONS.isEmpty());
        assertEquals("3.0.11", PlayMetadata.PLAY_FALLBACK_VERSIONS.getFirst());
        assertTrue(PlayMetadata.PLAY_FALLBACK_VERSIONS.contains("3.0.10"));
        assertTrue(PlayMetadata.PLAY_FALLBACK_VERSIONS.contains("3.0.9"));
        assertTrue(PlayMetadata.PLAY_FALLBACK_VERSIONS.contains("3.0.8"));
        assertEquals("3.0.11", PlayMetadata.DEFAULT_PLAY_VERSION);
    }

    @Test
    void testCompareVersions() {
        assertTrue(PlayMetadata.compareVersions("3.0.11", "3.0.10") > 0);
        assertTrue(PlayMetadata.compareVersions("3.0.9", "3.0.11") < 0);
        assertEquals(0, PlayMetadata.compareVersions("3.0.11", "3.0.11"));
    }
}
