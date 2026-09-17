package dev.lumina.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GroovyMavenGeneratorTest {

    @Test
    void testGroovyMavenProjectGeneration(@TempDir Path tempDir) throws IOException {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.GROOVY,
                "groovy-demo",
                tempDir,
                true,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.GROOVY,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "org.example",
                "groovy-demo",
                "org.example",
                "25",
                "", "", "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "",
                "", "", "", "MAVEN",
                true,
                "", "", "", "", "", "", "", "", "", "",
                "", "", false, "", "", "", "",
                "HTML5 Boilerplate", "v9.0.1", "React", "", "5.1.0", false,
                "", "", "", "",
                ProjectSpec.GradleDsl.GROOVY,
                "Wrapper",
                "9.2.0",
                "",
                false,
                "5.1.1"
        );

        Path projectDir = ProjectGenerator.generate(spec, msg -> {});
        assertEquals(tempDir.resolve("groovy-demo"), projectDir);

        // pom.xml
        Path pomPath = projectDir.resolve("pom.xml");
        assertTrue(Files.isRegularFile(pomPath), "pom.xml must exist");
        String pom = Files.readString(pomPath);
        assertTrue(pom.contains("<groupId>org.example</groupId>"));
        assertTrue(pom.contains("<artifactId>groovy-demo</artifactId>"));
        assertTrue(pom.contains("<groovy.version>5.1.1</groovy.version>"));
        assertTrue(pom.contains("<groupId>org.apache.groovy</groupId>"));
        assertTrue(pom.contains("<artifactId>groovy</artifactId>"));
        assertTrue(pom.contains("gmavenplus-plugin"));
        assertTrue(pom.contains("<sourceDirectory>src/main/groovy</sourceDirectory>"));

        // Source code
        Path mainPath = projectDir.resolve("src/main/groovy/org/example/Main.groovy");
        assertTrue(Files.isRegularFile(mainPath), "Main.groovy must exist");
        String mainContent = Files.readString(mainPath);
        assertTrue(mainContent.contains("package org.example"));
        assertTrue(mainContent.contains("static void main(String[] args)"));
        assertTrue(mainContent.contains("println \"Hello and welcome!\""));

        // Test code
        Path testPath = projectDir.resolve("src/test/groovy/org/example/MainTest.groovy");
        assertTrue(Files.isRegularFile(testPath), "MainTest.groovy must exist");
        String testContent = Files.readString(testPath);
        assertTrue(testContent.contains("package org.example"));
        assertTrue(testContent.contains("class MainTest"));
    }

    @Test
    void testGroovy2xMavenProjectGeneration(@TempDir Path tempDir) throws IOException {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.GROOVY,
                "groovy-legacy",
                tempDir,
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.GROOVY,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.mycompany",
                "groovy-legacy",
                "com.mycompany",
                "17",
                "", "", "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "",
                "", "", "", "MAVEN",
                true,
                "", "", "", "", "", "", "", "", "", "",
                "", "", false, "", "", "", "",
                "HTML5 Boilerplate", "v9.0.1", "React", "", "5.1.0", false,
                "", "", "", "",
                ProjectSpec.GradleDsl.GROOVY,
                "Wrapper",
                "9.2.0",
                "",
                false,
                "2.4.21"
        );

        Path projectDir = ProjectGenerator.generate(spec, msg -> {});
        Path pomPath = projectDir.resolve("pom.xml");
        String pom = Files.readString(pomPath);
        assertTrue(pom.contains("<groovy.version>2.4.21</groovy.version>"));
        assertTrue(pom.contains("<groupId>org.codehaus.groovy</groupId>"));
    }

    @Test
    void testGroovyMetadataBranchesAndFallbacks() {
        List<String> versions = GroovyMetadata.fetchVersions(false);
        assertFalse(versions.isEmpty());
        assertEquals("5.1.1", versions.get(0), "Latest Groovy branch version should be 5.1.1");
        assertTrue(versions.contains("5.0.8"));
        assertTrue(versions.contains("4.0.33"));
        assertTrue(versions.contains("3.0.25"));
        assertTrue(versions.contains("2.5.23"));
        assertTrue(versions.contains("2.4.21"));
        assertTrue(versions.contains("6.0.0-beta-3"));

        assertEquals("org.apache.groovy", GroovyMetadata.getGroovyGroupId("5.1.1"));
        assertEquals("org.apache.groovy", GroovyMetadata.getGroovyGroupId("4.0.33"));
        assertEquals("org.codehaus.groovy", GroovyMetadata.getGroovyGroupId("2.5.23"));
        assertEquals("org.codehaus.groovy", GroovyMetadata.getGroovyGroupId("2.4.21"));

        // Test XML parsing and branch selection
        String mockXml = """
                <metadata>
                  <versioning>
                    <versions>
                      <version>3.0.0</version>
                      <version>3.0.25</version>
                      <version>4.0.0</version>
                      <version>4.0.33</version>
                      <version>5.0.0</version>
                      <version>5.0.8</version>
                      <version>5.1.0</version>
                      <version>5.1.1</version>
                      <version>6.0.0-beta-1</version>
                      <version>6.0.0-beta-3</version>
                    </versions>
                  </versioning>
                </metadata>
                """;
        List<String> parsed = GroovyMetadata.parseVersionsXml(mockXml);
        assertEquals(List.of("5.1.1", "5.0.8", "4.0.33", "3.0.25", "6.0.0-beta-3", GroovyMetadata.SPECIFY_HOME_OPTION), parsed);
    }
}
