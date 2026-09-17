package dev.lumina.project;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScalaMetadataTest {

    @Test
    void testParseSbtVersionsXml() {
        String mockXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <metadata>
                  <groupId>org.scala-sbt</groupId>
                  <artifactId>sbt-launch</artifactId>
                  <versioning>
                    <versions>
                      <version>1.10.6</version>
                      <version>1.10.7</version>
                      <version>2.0.0-RC15</version>
                      <version>2.0.0-RC16</version>
                      <version>2.0.0</version>
                      <version>2.0.1</version>
                      <version>2.0.7</version>
                      <version>2.0.8</version>
                      <version>2.0.9</version>
                      <version>2.1.0-M1</version>
                    </versions>
                  </versioning>
                </metadata>
                """;

        List<String> versions = ScalaMetadata.parseSbtVersionsXml(mockXml);

        assertNotNull(versions);
        assertFalse(versions.isEmpty());
        // Verify latest stable first
        assertEquals("2.0.9", versions.getFirst());
        // Verify order is descending
        assertEquals(List.of("2.0.9", "2.0.8", "2.0.7", "2.0.1", "2.0.0", "1.10.7", "1.10.6"), versions);

        // Verify milestone/RC filtered out
        assertFalse(versions.contains("2.0.0-RC16"));
        assertFalse(versions.contains("2.1.0-M1"));
    }

    @Test
    void testParseScalaVersionsXml() {
        String mockXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <metadata>
                  <groupId>org.scala-lang</groupId>
                  <artifactId>scala3-compiler_3</artifactId>
                  <versioning>
                    <versions>
                      <version>3.7.3</version>
                      <version>3.7.4</version>
                      <version>3.8.0</version>
                      <version>3.8.1</version>
                      <version>3.8.2</version>
                      <version>3.8.3</version>
                      <version>3.8.4-RC2</version>
                      <version>3.8.4</version>
                      <version>3.9.0-RC6</version>
                      <version>3.9.0</version>
                      <version>3.10.0-RC1</version>
                    </versions>
                  </versioning>
                </metadata>
                """;

        List<String> versions = ScalaMetadata.parseScalaVersionsXml(mockXml);

        assertNotNull(versions);
        assertFalse(versions.isEmpty());
        // Verify latest stable first
        assertEquals("3.9.0", versions.getFirst());
        // Verify order is descending
        assertEquals(List.of("3.9.0", "3.8.4", "3.8.3", "3.8.2", "3.8.1", "3.8.0", "3.7.4", "3.7.3"), versions);

        // Verify RC filtered out
        assertFalse(versions.contains("3.9.0-RC6"));
        assertFalse(versions.contains("3.10.0-RC1"));
    }

    @Test
    void testFallbackVersionsNotNull() {
        assertFalse(ScalaMetadata.SBT_FALLBACK_VERSIONS.isEmpty());
        assertEquals("2.0.9", ScalaMetadata.SBT_FALLBACK_VERSIONS.getFirst());

        assertFalse(ScalaMetadata.SCALA_FALLBACK_VERSIONS.isEmpty());
        assertEquals("3.9.0", ScalaMetadata.SCALA_FALLBACK_VERSIONS.getFirst());
    }

    @Test
    void testCompareVersions() {
        assertTrue(ScalaMetadata.compareVersions("3.9.0", "3.8.4") > 0);
        assertTrue(ScalaMetadata.compareVersions("2.0.8", "2.0.9") < 0);
        assertEquals(0, ScalaMetadata.compareVersions("2.0.9", "2.0.9"));
    }
}
