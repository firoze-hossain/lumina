package dev.lumina.project;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpringInitializrMetadataTest {

    @Test
    void testVersionRangeCompatibility() {
        // [3.0.0-M1, )
        assertTrue(SpringInitializrMetadata.isVersionCompatible("3.4.3", "[3.0.0-M1,)"));
        assertTrue(SpringInitializrMetadata.isVersionCompatible("4.1.1", "[3.0.0-M1,)"));
        assertFalse(SpringInitializrMetadata.isVersionCompatible("2.7.18", "[3.0.0-M1,)"));

        // [3.0.0, 3.4.0)
        assertTrue(SpringInitializrMetadata.isVersionCompatible("3.1.5", "[3.0.0, 3.4.0)"));
        assertTrue(SpringInitializrMetadata.isVersionCompatible("3.0.0", "[3.0.0, 3.4.0)"));
        assertFalse(SpringInitializrMetadata.isVersionCompatible("3.4.0", "[3.0.0, 3.4.0)"));
        assertFalse(SpringInitializrMetadata.isVersionCompatible("3.4.3", "[3.0.0, 3.4.0)"));
        assertFalse(SpringInitializrMetadata.isVersionCompatible("2.7.0", "[3.0.0, 3.4.0)"));

        // Open ranges or null
        assertTrue(SpringInitializrMetadata.isVersionCompatible("3.4.3", null));
        assertTrue(SpringInitializrMetadata.isVersionCompatible("3.4.3", ""));
    }

    @Test
    void testParseMetadataJson() throws IOException {
        String json = """
                {
                  "bootVersion": {
                    "default": "4.1.1",
                    "values": [
                      { "id": "4.1.1", "name": "4.1.1" },
                      { "id": "3.4.3", "name": "3.4.3" }
                    ]
                  },
                  "javaVersion": {
                    "default": "17",
                    "values": [
                      { "id": "17", "name": "17" },
                      { "id": "21", "name": "21" },
                      { "id": "25", "name": "25" }
                    ]
                  },
                  "packaging": {
                    "default": "jar",
                    "values": [
                      { "id": "jar", "name": "Jar" },
                      { "id": "war", "name": "War" }
                    ]
                  },
                  "language": {
                    "default": "java",
                    "values": [
                      { "id": "java", "name": "Java" },
                      { "id": "kotlin", "name": "Kotlin" }
                    ]
                  },
                  "type": {
                    "default": "gradle-project",
                    "values": [
                      { "id": "gradle-project", "name": "Gradle - Groovy" },
                      { "id": "gradle-project-kotlin", "name": "Gradle - Kotlin" },
                      { "id": "maven-project", "name": "Maven" }
                    ]
                  },
                  "groupId": { "default": "org.example" },
                  "artifactId": { "default": "demo" },
                  "packageName": { "default": "com.example.demo" },
                  "dependencies": {
                    "values": [
                      {
                        "name": "Developer Tools",
                        "values": [
                          {
                            "id": "native",
                            "name": "GraalVM Native Support",
                            "description": "Native executable compiler support",
                            "versionRange": "[3.0.0,)"
                          }
                        ]
                      }
                    ]
                  }
                }
                """;

        SpringInitializrMetadata.Metadata metadata = SpringInitializrMetadata.parse(json);
        assertNotNull(metadata);
        assertEquals("4.1.1", metadata.defaultBootVersion());
        assertEquals(List.of("4.1.1", "3.4.3"), metadata.bootVersions());

        assertEquals("17", metadata.defaultJavaVersion());
        assertEquals(List.of("17", "21", "25"), metadata.javaVersions());

        assertEquals("org.example", metadata.defaultGroupId());
        assertEquals("demo", metadata.defaultArtifactId());
        assertEquals("com.example.demo", metadata.defaultPackageName());

        assertEquals(1, metadata.categories().size());
        SpringInitializrMetadata.Category cat = metadata.categories().get(0);
        assertEquals("Developer Tools", cat.name());
        assertEquals(1, cat.dependencies().size());
        SpringInitializrMetadata.Dependency dep = cat.dependencies().get(0);
        assertEquals("native", dep.id());
        assertEquals("GraalVM Native Support", dep.name());
        assertTrue(dep.isCompatibleWith("4.1.1"));
        assertFalse(dep.isCompatibleWith("2.7.0"));
    }

    @Test
    void testServerUrlNormalization() {
        assertEquals("https://start.spring.io", SpringInitializrMetadata.normalizeServerUrl("start.spring.io"));
        assertEquals("https://start.spring.io", SpringInitializrMetadata.normalizeServerUrl("https://start.spring.io/"));
        assertEquals("http://localhost:8080", SpringInitializrMetadata.normalizeServerUrl("http://localhost:8080/"));
    }

    @Test
    void testFallbackMetadata() {
        SpringInitializrMetadata.Metadata fallback = SpringInitializrMetadata.getFallbackMetadata();
        assertNotNull(fallback);
        assertFalse(fallback.bootVersions().isEmpty());
        assertFalse(fallback.categories().isEmpty());
        assertTrue(fallback.javaVersions().contains("17"));
        assertTrue(fallback.javaVersions().contains("21"));
    }
}
