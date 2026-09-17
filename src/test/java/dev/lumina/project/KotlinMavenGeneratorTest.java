package dev.lumina.project;

import dev.lumina.run.RunConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KotlinMavenGeneratorTest {

    @Test
    void testKotlinMetadataXmlParsing() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <metadata>
                  <groupId>org.jetbrains.kotlin</groupId>
                  <artifactId>kotlin-stdlib</artifactId>
                  <versioning>
                    <latest>2.1.10</latest>
                    <release>2.1.10</release>
                    <versions>
                      <version>1.9.20</version>
                      <version>1.9.25</version>
                      <version>2.0.0-RC1</version>
                      <version>2.0.0</version>
                      <version>2.0.20-Beta</version>
                      <version>2.0.21</version>
                      <version>2.1.0</version>
                      <version>2.1.10</version>
                      <version>2.2.0-dev-1234</version>
                    </versions>
                  </versioning>
                </metadata>
                """;
        List<String> versions = KotlinMetadata.parseVersionsXml(xml);
        assertNotNull(versions);
        assertEquals(6, versions.size(), "Should filter out RC, Beta, and dev releases");
        assertEquals("2.1.10", versions.get(0));
        assertEquals("2.1.0", versions.get(1));
        assertEquals("2.0.21", versions.get(2));
        assertEquals("2.0.0", versions.get(3));
        assertEquals("1.9.25", versions.get(4));
        assertEquals("1.9.20", versions.get(5));
    }

    @Test
    void testKotlinMetadataJvmTarget() {
        assertEquals("21", KotlinMetadata.getJvmTarget(25), "JDK 25 should target JVM 21");
        assertEquals("21", KotlinMetadata.getJvmTarget(21), "JDK 21 should target JVM 21");
        assertEquals("17", KotlinMetadata.getJvmTarget(20), "JDK 20 should target JVM 17");
        assertEquals("17", KotlinMetadata.getJvmTarget(17), "JDK 17 should target JVM 17");
        assertEquals("11", KotlinMetadata.getJvmTarget(16), "JDK 16 should target JVM 11");
        assertEquals("11", KotlinMetadata.getJvmTarget(11), "JDK 11 should target JVM 11");
        assertEquals("1.8", KotlinMetadata.getJvmTarget(8), "JDK 8 should target JVM 1.8");
    }

    @Test
    void testVersionComparison() {
        assertTrue(KotlinMetadata.compareVersions("2.1.10", "2.1.0") > 0);
        assertTrue(KotlinMetadata.compareVersions("2.0.21", "2.1.0") < 0);
        assertEquals(0, KotlinMetadata.compareVersions("2.1.10", "2.1.10"));
    }

    @Test
    void testGenerateKotlinMavenWithSampleCode(@TempDir Path tempDir) throws IOException {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.KOTLIN,
                "kotlin-demo",
                tempDir,
                true,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.KOTLIN,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.stratosdb",
                "kotlin-demo",
                "com.stratosdb",
                "25",
                "", "", "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "",
                "", "", "", "MAVEN",
                true // Add sample code
        );

        Path projectDir = ProjectGenerator.generate(spec, msg -> {});
        assertEquals(tempDir.resolve("kotlin-demo"), projectDir);
        assertTrue(Files.isDirectory(projectDir));

        // 1. Verify pom.xml
        Path pomPath = projectDir.resolve("pom.xml");
        assertTrue(Files.isRegularFile(pomPath), "pom.xml must exist");
        String pom = Files.readString(pomPath);
        assertTrue(pom.contains("<groupId>com.stratosdb</groupId>"), "pom.xml should contain groupId");
        assertTrue(pom.contains("<artifactId>kotlin-demo</artifactId>"), "pom.xml should contain artifactId");
        assertTrue(pom.contains("<artifactId>kotlin-maven-plugin</artifactId>"), "pom.xml should contain kotlin-maven-plugin");
        assertTrue(pom.contains("<sourceDirectory>src/main/kotlin</sourceDirectory>"), "pom.xml should have kotlin source directory");
        assertTrue(pom.contains("<testSourceDirectory>src/test/kotlin</testSourceDirectory>"), "pom.xml should have kotlin test source directory");
        assertTrue(pom.contains("<kotlin.compiler.jvmTarget>21</kotlin.compiler.jvmTarget>"), "pom.xml should set jvmTarget 21 for JDK 25");
        assertTrue(pom.contains("<artifactId>kotlin-stdlib</artifactId>"), "pom.xml should contain kotlin-stdlib");
        assertTrue(pom.contains("<artifactId>kotlin-test-junit5</artifactId>"), "pom.xml should contain kotlin-test-junit5");
        assertTrue(pom.contains("<mainClass>com.stratosdb.MainKt</mainClass>"), "pom.xml should configure MainKt execution");

        // 2. Verify Main.kt
        Path mainPath = projectDir.resolve("src/main/kotlin/com/stratosdb/Main.kt");
        assertTrue(Files.isRegularFile(mainPath), "Main.kt must exist when addSampleCode=true");
        String mainContent = Files.readString(mainPath);
        assertTrue(mainContent.contains("package com.stratosdb"), "Main.kt should declare package com.stratosdb");
        assertTrue(mainContent.contains("fun main()"), "Main.kt should have main function");
        assertTrue(mainContent.contains("println(\"Hello, \" + name + \"!\")"), "Main.kt should have IntelliJ welcome greeting");
        assertTrue(mainContent.contains("for (i in 1..5)"), "Main.kt should have 1..5 loop");

        // 3. Verify MainTest.kt
        Path testPath = projectDir.resolve("src/test/kotlin/com/stratosdb/MainTest.kt");
        assertTrue(Files.isRegularFile(testPath), "MainTest.kt must exist");
        String testContent = Files.readString(testPath);
        assertTrue(testContent.contains("package com.stratosdb"), "MainTest.kt should declare package com.stratosdb");
        assertTrue(testContent.contains("class MainTest"), "MainTest.kt should define MainTest class");
        assertTrue(testContent.contains("@Test"), "MainTest.kt should contain JUnit @Test");

        // 4. Verify git and readme
        assertTrue(Files.isRegularFile(projectDir.resolve(".gitignore")));
        assertTrue(Files.isRegularFile(projectDir.resolve("README.md")));

        // 5. Verify RunConfiguration detects Maven run
        List<RunConfiguration> configs = RunConfiguration.detect(projectDir);
        assertTrue(configs.stream().anyMatch(c -> c.label().startsWith("Maven run")),
                "RunConfiguration should detect Maven run for kotlin-maven project");
    }

    @Test
    void testGenerateKotlinMavenWithoutSampleCode(@TempDir Path tempDir) throws IOException {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.KOTLIN,
                "clean-kotlin",
                tempDir,
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.KOTLIN,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "org.example",
                "clean-kotlin",
                "org.example",
                "21",
                "", "", "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "",
                "", "", "", "MAVEN",
                false // No sample code
        );

        Path projectDir = ProjectGenerator.generate(spec, msg -> {});
        assertEquals(tempDir.resolve("clean-kotlin"), projectDir);

        Path pomPath = projectDir.resolve("pom.xml");
        assertTrue(Files.isRegularFile(pomPath));

        Path mainPath = projectDir.resolve("src/main/kotlin/org/example/Main.kt");
        assertFalse(Files.exists(mainPath), "Main.kt must NOT exist when addSampleCode=false");
        assertTrue(Files.isDirectory(projectDir.resolve("src/main/kotlin/org/example")),
                "Package directory must exist");
    }

    @Test
    void testGenerateKotlinGradleKotlinDsl(@TempDir Path tempDir) throws IOException {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.KOTLIN,
                "kotlin-gradle-app",
                tempDir,
                false,
                ProjectSpec.BuildSystem.GRADLE,
                ProjectSpec.Language.KOTLIN,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.mycorp",
                "kotlin-gradle-app",
                "com.mycorp",
                "21",
                "", "", "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "",
                "", "", "", "GRADLE",
                true,
                "", "", "", "",
                "", "", "", "", "", "",
                "", "", false, "", "", "", "",
                "", "", "", "", "", false,
                "", "", "", "",
                ProjectSpec.GradleDsl.KOTLIN, "Wrapper", "9.2.0", ""
        );

        Path projectDir = ProjectGenerator.generate(spec, msg -> {});
        assertTrue(Files.isRegularFile(projectDir.resolve("build.gradle.kts")), "build.gradle.kts must exist");
        assertTrue(Files.isRegularFile(projectDir.resolve("settings.gradle.kts")), "settings.gradle.kts must exist");

        String buildGradle = Files.readString(projectDir.resolve("build.gradle.kts"));
        assertTrue(buildGradle.contains("kotlin(\"jvm\")"));
        assertTrue(buildGradle.contains("group = \"com.mycorp\""));
        assertTrue(buildGradle.contains("jvmToolchain(21)"));
        assertTrue(buildGradle.contains("mainClass.set(\"com.mycorp.MainKt\")"));

        assertTrue(Files.isRegularFile(projectDir.resolve("gradle/wrapper/gradle-wrapper.properties")));
        assertTrue(Files.isRegularFile(projectDir.resolve("src/main/kotlin/com/mycorp/Main.kt")));

        List<RunConfiguration> configs = RunConfiguration.detect(projectDir);
        assertTrue(configs.stream().anyMatch(c -> c.label().startsWith("Gradle run")),
                "RunConfiguration should detect Gradle run for kotlin-gradle project");
    }

    @Test
    void testGenerateKotlinGradleGroovyDsl(@TempDir Path tempDir) throws IOException {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.KOTLIN,
                "kotlin-gradle-groovy",
                tempDir,
                false,
                ProjectSpec.BuildSystem.GRADLE,
                ProjectSpec.Language.KOTLIN,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.mycorp",
                "kotlin-gradle-groovy",
                "com.mycorp",
                "17",
                "", "", "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "",
                "", "", "", "GRADLE",
                true,
                "", "", "", "",
                "", "", "", "", "", "",
                "", "", false, "", "", "", "",
                "", "", "", "", "", false,
                "", "", "", "",
                ProjectSpec.GradleDsl.GROOVY, "Wrapper", "8.12.1", ""
        );

        Path projectDir = ProjectGenerator.generate(spec, msg -> {});
        assertTrue(Files.isRegularFile(projectDir.resolve("build.gradle")), "build.gradle must exist");
        assertTrue(Files.isRegularFile(projectDir.resolve("settings.gradle")), "settings.gradle must exist");

        String buildGradle = Files.readString(projectDir.resolve("build.gradle"));
        assertTrue(buildGradle.contains("id 'org.jetbrains.kotlin.jvm'"));
        assertTrue(buildGradle.contains("group = 'com.mycorp'"));
        assertTrue(buildGradle.contains("jvmToolchain(17)"));
        assertTrue(buildGradle.contains("mainClass = 'com.mycorp.MainKt'"));
    }
}
