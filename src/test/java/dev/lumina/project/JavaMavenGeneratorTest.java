package dev.lumina.project;

import dev.lumina.project.JdkMetadata.JdkInstallation;
import dev.lumina.project.JdkMetadata.JdkPackage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JavaMavenGeneratorTest {

    @Test
    void testLocalJdkDetection() {
        List<JdkInstallation> jdks = JdkMetadata.detectInstallations(true);
        assertNotNull(jdks, "Detected JDK list should not be null");
        assertFalse(jdks.isEmpty(), "Expected at least one JDK detected on this host machine");

        JdkInstallation first = jdks.getFirst();
        assertNotNull(first.name(), "JDK name should not be null");
        assertNotNull(first.homePath(), "JDK homePath should not be null");
        assertTrue(first.majorVersion() > 0, "Major version should be positive, got: " + first.majorVersion());
        assertNotNull(first.architecture(), "Architecture should not be null");
        assertNotNull(first.vendor(), "Vendor should not be null");

        String display = first.formatDisplay();
        assertTrue(display.startsWith(String.valueOf(first.majorVersion())),
                "Formatted display should start with major version: " + display);
    }

    @Test
    void testReleaseFileParsing() {
        String content = """
                IMPLEMENTOR="Homebrew"
                JAVA_VERSION="25.0.1"
                OS_NAME="Darwin"
                OS_ARCH="aarch64"
                """;
        Map<String, String> map = JdkMetadata.parseReleaseFile(content);
        assertEquals("Homebrew", map.get("IMPLEMENTOR"));
        assertEquals("25.0.1", map.get("JAVA_VERSION"));
        assertEquals("Darwin", map.get("OS_NAME"));
        assertEquals("aarch64", map.get("OS_ARCH"));
    }

    @Test
    void testMajorVersionParsing() {
        assertEquals(25, JdkMetadata.parseMajorVersion("25.0.1"));
        assertEquals(21, JdkMetadata.parseMajorVersion("21.0.2"));
        assertEquals(17, JdkMetadata.parseMajorVersion("17.0.9"));
        assertEquals(8, JdkMetadata.parseMajorVersion("1.8.0_392"));
        assertEquals(25, JdkMetadata.parseMajorVersion("25 Homebrew OpenJDK 25.0.1 - aarch64"));
        assertEquals(21, JdkMetadata.parseMajorVersion("Oracle OpenJDK 21.0.1"));
        assertEquals(23, JdkMetadata.parseMajorVersion("23"));
    }

    @Test
    void testFoojayDiscoParsing() {
        String json = """
                {
                    "result": [
                        {"major_version": 25},
                        {"major_version": 21},
                        {"major_version": 17},
                        {"major_version": 11},
                        {"major_version": 8}
                    ]
                }
                """;
        List<Integer> majors = JdkMetadata.parseMajorVersionsJson(json);
        assertEquals(List.of(25, 21, 17, 11, 8), majors);

        List<JdkPackage> fallbacks = JdkMetadata.getFallbackPackages(25, "aarch64", "mac");
        assertFalse(fallbacks.isEmpty(), "Fallback packages should not be empty");
        boolean hasOracle = fallbacks.stream().anyMatch(p -> p.vendorDisplay().contains("Oracle"));
        boolean hasCorretto = fallbacks.stream().anyMatch(p -> p.vendorDisplay().contains("Corretto"));
        assertTrue(hasOracle, "Expected Oracle OpenJDK in packages");
        assertTrue(hasCorretto, "Expected Amazon Corretto in packages");
    }

    @Test
    void testGenerateJavaMavenWithSampleCode(@TempDir Path tempDir) throws IOException {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.JAVA,
                "untitled",
                tempDir,
                true,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.stratosdb",
                "untitled",
                "com.stratosdb",
                "25",
                "", "", "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "",
                "", "", "", "MAVEN",
                true // Add sample code
        );

        Path projectDir = ProjectGenerator.generate(spec, msg -> {});
        assertEquals(tempDir.resolve("untitled"), projectDir);
        assertTrue(Files.isDirectory(projectDir));

        // Verify pom.xml
        Path pomPath = projectDir.resolve("pom.xml");
        assertTrue(Files.isRegularFile(pomPath), "pom.xml must exist");
        String pom = Files.readString(pomPath);
        assertTrue(pom.contains("<groupId>com.stratosdb</groupId>"), "pom.xml should contain groupId");
        assertTrue(pom.contains("<artifactId>untitled</artifactId>"), "pom.xml should contain artifactId");
        assertTrue(pom.contains("<maven.compiler.source>25</maven.compiler.source>"), "pom.xml should contain source 25");
        assertTrue(pom.contains("<maven.compiler.target>25</maven.compiler.target>"), "pom.xml should contain target 25");

        // Verify Main.java with IntelliJ sample code
        Path mainPath = projectDir.resolve("src/main/java/com/stratosdb/Main.java");
        assertTrue(Files.isRegularFile(mainPath), "Main.java must exist when addSampleCode=true");
        String mainContent = Files.readString(mainPath);
        assertTrue(mainContent.contains("package com.stratosdb;"), "Main.java should declare package com.stratosdb");
        assertTrue(mainContent.contains("System.out.println(\"Hello and welcome!\");"), "Main.java should have IntelliJ welcome message");
        assertTrue(mainContent.contains("for (int i = 1; i <= 5; i++)"), "Main.java should have IntelliJ loop");

        // Verify git and readme
        assertTrue(Files.isRegularFile(projectDir.resolve(".gitignore")));
        assertTrue(Files.isRegularFile(projectDir.resolve("README.md")));
    }

    @Test
    void testGenerateJavaMavenWithoutSampleCode(@TempDir Path tempDir) throws IOException {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.JAVA,
                "clean-proj",
                tempDir,
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "org.example",
                "clean-proj",
                "org.example",
                "21",
                "", "", "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "",
                "", "", "", "MAVEN",
                false // No sample code
        );

        Path projectDir = ProjectGenerator.generate(spec, msg -> {});
        assertEquals(tempDir.resolve("clean-proj"), projectDir);

        Path pomPath = projectDir.resolve("pom.xml");
        assertTrue(Files.isRegularFile(pomPath));
        String pom = Files.readString(pomPath);
        assertTrue(pom.contains("<maven.compiler.source>21</maven.compiler.source>"));
        assertTrue(pom.contains("<maven.compiler.target>21</maven.compiler.target>"));

        // Main.java must NOT exist
        Path mainPath = projectDir.resolve("src/main/java/org/example/Main.java");
        assertFalse(Files.exists(mainPath), "Main.java must NOT be created when addSampleCode=false");
        assertTrue(Files.isDirectory(projectDir.resolve("src/main/java/org/example")),
                "Package directory must exist");
    }

    @Test
    void testGenerateJavaGradle(@TempDir Path tempDir) throws IOException {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.JAVA,
                "gradle-app",
                tempDir,
                false,
                ProjectSpec.BuildSystem.GRADLE,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.mycorp",
                "gradle-app",
                "com.mycorp",
                "21",
                "", "", "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "",
                "", "", "", "GRADLE",
                true
        );

        Path projectDir = ProjectGenerator.generate(spec, msg -> {});
        assertTrue(Files.isRegularFile(projectDir.resolve("build.gradle")));
        assertTrue(Files.isRegularFile(projectDir.resolve("settings.gradle")));
        String buildGradle = Files.readString(projectDir.resolve("build.gradle"));
        assertTrue(buildGradle.contains("group = 'com.mycorp'"));
        assertTrue(buildGradle.contains("languageVersion = JavaLanguageVersion.of(21)"));
    }

    @Test
    void testGenerateJavaIntelliJ(@TempDir Path tempDir) throws IOException {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.JAVA,
                "idea-app",
                tempDir,
                false,
                ProjectSpec.BuildSystem.INTELLIJ,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.idea",
                "idea-app",
                "com.idea",
                "25",
                "", "", "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "",
                "", "", "", "INTELLIJ",
                true
        );

        Path projectDir = ProjectGenerator.generate(spec, msg -> {});
        assertTrue(Files.isRegularFile(projectDir.resolve(".idea/misc.xml")));
        assertTrue(Files.isRegularFile(projectDir.resolve(".idea/modules.xml")));
        assertTrue(Files.isRegularFile(projectDir.resolve("idea-app.iml")));
        String misc = Files.readString(projectDir.resolve(".idea/misc.xml"));
        assertTrue(misc.contains("languageLevel=\"JDK_25\""));
    }
}
