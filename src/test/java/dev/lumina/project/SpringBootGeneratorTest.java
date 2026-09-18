package dev.lumina.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class SpringBootGeneratorTest {

    @Test
    void testDynamicFallbackMetadata() {
        SpringInitializrMetadata.Metadata metadata = SpringInitializrMetadata.getFallbackMetadata();
        assertNotNull(metadata);

        // Verify dynamic versions and defaults
        assertTrue(metadata.bootVersions().contains("3.4.3"));
        assertEquals("3.4.3", metadata.defaultBootVersion());

        assertTrue(metadata.javaVersions().contains("17"));
        assertTrue(metadata.javaVersions().contains("21"));
        assertTrue(metadata.javaVersions().contains("25"));

        assertEquals("demo", metadata.defaultArtifactId());
        assertEquals("org.example", metadata.defaultGroupId());
        assertEquals("com.example.demo", metadata.defaultPackageName());

        assertTrue(metadata.packagings().contains("jar"));
        assertTrue(metadata.packagings().contains("war"));

        assertTrue(metadata.languages().contains("java"));
        assertTrue(metadata.languages().contains("kotlin"));
        assertTrue(metadata.languages().contains("groovy"));

        assertTrue(metadata.types().contains("gradle-project"));
        assertTrue(metadata.types().contains("gradle-project-kotlin"));
        assertTrue(metadata.types().contains("maven-project"));

        // Verify categories and starters
        assertFalse(metadata.categories().isEmpty());
        SpringInitializrMetadata.Category web = metadata.categories().stream()
                .filter(c -> "Web".equals(c.name()))
                .findFirst()
                .orElse(null);
        assertNotNull(web);
        assertTrue(web.dependencies().stream().anyMatch(d -> "web".equals(d.id())));
    }

    @Test
    void testVersionRangeCompatibility() {
        // [3.0.0, ) open-ended
        assertTrue(SpringInitializrMetadata.isVersionCompatible("3.4.3", "[3.0.0, )"));
        assertTrue(SpringInitializrMetadata.isVersionCompatible("3.0.0", "[3.0.0, )"));
        assertFalse(SpringInitializrMetadata.isVersionCompatible("2.7.18", "[3.0.0, )"));

        // [2.5.0, 3.2.0) bounded half-open
        assertTrue(SpringInitializrMetadata.isVersionCompatible("2.7.0", "[2.5.0, 3.2.0)"));
        assertTrue(SpringInitializrMetadata.isVersionCompatible("2.5.0", "[2.5.0, 3.2.0)"));
        assertFalse(SpringInitializrMetadata.isVersionCompatible("3.2.0", "[2.5.0, 3.2.0)"));
        assertFalse(SpringInitializrMetadata.isVersionCompatible("3.4.3", "[2.5.0, 3.2.0)"));

        // exact version or range with milestones/snapshots
        assertTrue(SpringInitializrMetadata.isVersionCompatible("3.4.3-SNAPSHOT", "[3.0.0, )"));
        assertTrue(SpringInitializrMetadata.isVersionCompatible("3.5.0-M1", "[3.0.0, )"));

        // Blank or null range is universally compatible
        assertTrue(SpringInitializrMetadata.isVersionCompatible("3.4.3", null));
        assertTrue(SpringInitializrMetadata.isVersionCompatible("3.4.3", ""));
    }

    @Test
    void testServerUrlNormalization() {
        assertEquals("https://start.spring.io",
                SpringInitializrMetadata.normalizeServerUrl("start.spring.io"));
        assertEquals("https://start.spring.io",
                SpringInitializrMetadata.normalizeServerUrl("https://start.spring.io/"));
        assertEquals("http://localhost:8080",
                SpringInitializrMetadata.normalizeServerUrl("http://localhost:8080/metadata/client"));
        assertEquals("https://my-initializr.internal",
                SpringInitializrMetadata.normalizeServerUrl("my-initializr.internal/"));
    }

    @Test
    void testServerUrlGetAndSet() {
        String original = SpringInitializrMetadata.getServerUrl();
        try {
            SpringInitializrMetadata.setServerUrl("start.aliyun.com");
            assertEquals("https://start.aliyun.com", SpringInitializrMetadata.getServerUrl());
        } finally {
            SpringInitializrMetadata.setServerUrl(original);
        }
    }

    @Test
    void testSpringBootProjectSpecConfiguration(@TempDir Path tempDir) {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.SPRING_BOOT,
                "demo",
                tempDir,
                true,
                ProjectSpec.BuildSystem.GRADLE,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.YAML,
                "org.example",
                "demo",
                "com.example.demo",
                "17",
                "web,devtools",
                "3.4.3",
                "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "",
                "", "", "", "",
                true, "", "", "", "",
                "", "", "", "", "",
                "", "", "", false,
                "", "", "", "",
                "", "", "", "", "",
                false,
                "", "", "", "",
                ProjectSpec.GradleDsl.GROOVY,
                "Wrapper", "8.11.1", "",
                false, "5.1.1", "2.0.9", "3.9.0", false,
                false, false, "", ""
        );

        assertEquals("demo", spec.name());
        assertEquals("org.example", spec.group());
        assertEquals("demo", spec.artifact());
        assertEquals("com.example.demo", spec.packageName());
        assertEquals("17", spec.javaVersion());
        assertEquals(ProjectSpec.Packaging.JAR, spec.packaging());
        assertEquals(ProjectSpec.ConfigFormat.YAML, spec.configFormat());
        assertEquals(ProjectSpec.GradleDsl.GROOVY, spec.gradleDsl());
        assertEquals(ProjectSpec.BuildSystem.GRADLE, spec.buildSystem());
        assertEquals("3.4.3", spec.springBootVersion());
        assertEquals("web,devtools", spec.springDependencies());
    }
}
