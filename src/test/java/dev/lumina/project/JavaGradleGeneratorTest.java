package dev.lumina.project;

import dev.lumina.run.RunConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

class JavaGradleGeneratorTest {

    @Test
    void testGradleReleasesJsonParsing() {
        String json = """
                [
                    {
                        "version": "9.2.0",
                        "buildTime": "20250214120000+0000",
                        "current": true,
                        "snapshot": false,
                        "nightly": false,
                        "releaseCandidate": false,
                        "broken": false,
                        "downloadUrl": "https://services.gradle.org/distributions/gradle-9.2.0-bin.zip"
                    },
                    {
                        "version": "9.3.0-rc-1",
                        "current": false,
                        "snapshot": false,
                        "nightly": false,
                        "releaseCandidate": true,
                        "broken": false,
                        "downloadUrl": "https://services.gradle.org/distributions/gradle-9.3.0-rc-1-bin.zip"
                    },
                    {
                        "version": "9.1.0",
                        "current": false,
                        "snapshot": false,
                        "nightly": false,
                        "releaseCandidate": false,
                        "broken": false,
                        "downloadUrl": "https://services.gradle.org/distributions/gradle-9.1.0-bin.zip"
                    },
                    {
                        "version": "8.12.1",
                        "current": false,
                        "snapshot": false,
                        "nightly": false,
                        "releaseCandidate": false,
                        "broken": false,
                        "downloadUrl": "https://services.gradle.org/distributions/gradle-8.12.1-bin.zip"
                    },
                    {
                        "version": "8.13.0-nightly",
                        "current": false,
                        "snapshot": false,
                        "nightly": true,
                        "releaseCandidate": false,
                        "broken": false,
                        "downloadUrl": "https://services.gradle.org/distributions/gradle-8.13.0-nightly-bin.zip"
                    }
                ]
                """;
        List<GradleMetadata.GradleRelease> releases = GradleMetadata.parseReleasesJson(json);
        assertNotNull(releases);
        assertEquals(3, releases.size(), "Should filter out RC and nightly releases");
        assertEquals("9.2.0", releases.get(0).version());
        assertEquals("9.1.0", releases.get(1).version());
        assertEquals("8.12.1", releases.get(2).version());
        assertTrue(releases.get(0).current());
    }

    @Test
    void testJdkCompatibilityMatrix() {
        // JDK 25 requires Gradle 9.1+
        assertTrue(GradleMetadata.isCompatible("9.2.0", 25));
        assertTrue(GradleMetadata.isCompatible("9.1.0", 25));
        assertFalse(GradleMetadata.isCompatible("8.12.1", 25));
        assertFalse(GradleMetadata.isCompatible("8.5", 25));

        // JDK 24 requires Gradle 8.12+
        assertTrue(GradleMetadata.isCompatible("9.2.0", 24));
        assertTrue(GradleMetadata.isCompatible("8.12.1", 24));
        assertFalse(GradleMetadata.isCompatible("8.10.2", 24));

        // JDK 21 requires Gradle 8.5+
        assertTrue(GradleMetadata.isCompatible("9.2.0", 21));
        assertTrue(GradleMetadata.isCompatible("8.5", 21));
        assertFalse(GradleMetadata.isCompatible("7.6.4", 21));

        // JDK 17 requires Gradle 7.3+
        assertTrue(GradleMetadata.isCompatible("8.5", 17));
        assertTrue(GradleMetadata.isCompatible("7.6.4", 17));
        assertTrue(GradleMetadata.isCompatible("7.3.3", 17));

        // Filter compatible releases - must be sorted ascending matching IntelliJ IDEA dropdown
        List<GradleMetadata.GradleRelease> all = GradleMetadata.fetchReleases(false);
        List<GradleMetadata.GradleRelease> jdk25Releases = GradleMetadata.filterCompatibleVersions(all, 25);
        assertFalse(jdk25Releases.isEmpty());
        for (GradleMetadata.GradleRelease r : jdk25Releases) {
            assertTrue(GradleMetadata.parseVersionAsDouble(r.version()) >= 9.1,
                    "JDK 25 releases must be >= 9.1, got: " + r.version());
        }
        assertEquals("9.1.0", jdk25Releases.get(0).version(), "First compatible release for JDK 25 in ascending order should be 9.1.0");
        assertEquals("9.2.0", jdk25Releases.get(1).version(), "Second compatible release for JDK 25 in ascending order should be 9.2.0");

        // Latest compatible version - auto-selected
        String latest25 = GradleMetadata.getLatestCompatibleVersion(25);
        assertEquals("9.2.0", latest25, "Latest auto-detected version for JDK 25 should be 9.2.0");
    }

    @Test
    void testDynamicGradleVersionSelectionForMultipleJdks() {
        List<GradleMetadata.GradleRelease> all = GradleMetadata.fetchReleases(false);

        // JDK 25 -> [9.1.0, 9.2.0], auto-selected 9.2.0
        List<GradleMetadata.GradleRelease> jdk25 = GradleMetadata.filterCompatibleVersions(all, 25);
        List<String> jdk25Versions = jdk25.stream().map(GradleMetadata.GradleRelease::version).toList();
        assertEquals(List.of("9.1.0", "9.2.0"), jdk25Versions);
        assertEquals("9.2.0", GradleMetadata.getLatestCompatibleVersion(25));

        // JDK 21 -> min Gradle 8.5, auto-selected 9.2.0
        List<GradleMetadata.GradleRelease> jdk21 = GradleMetadata.filterCompatibleVersions(all, 21);
        List<String> jdk21Versions = jdk21.stream().map(GradleMetadata.GradleRelease::version).toList();
        assertTrue(jdk21Versions.contains("8.5"));
        assertTrue(jdk21Versions.contains("9.1.0"));
        assertTrue(jdk21Versions.contains("9.2.0"));
        assertFalse(jdk21Versions.contains("7.6.4"));
        assertEquals("9.2.0", GradleMetadata.getLatestCompatibleVersion(21));
        // Verify ascending order
        for (int i = 0; i < jdk21Versions.size() - 1; i++) {
            assertTrue(GradleMetadata.compareVersions(jdk21Versions.get(i), jdk21Versions.get(i + 1)) <= 0);
        }
    }

    @Test
    void testSemanticVersionComparison() {
        assertTrue(GradleMetadata.compareVersions("9.2.0", "9.1.0") > 0);
        assertTrue(GradleMetadata.compareVersions("9.1.0", "8.12.1") > 0);
        assertTrue(GradleMetadata.compareVersions("8.12.1", "8.12") > 0);
        assertTrue(GradleMetadata.compareVersions("8.12", "8.11.1") > 0);
        assertEquals(0, GradleMetadata.compareVersions("9.2.0", "9.2.0"));
        assertTrue(GradleMetadata.compareVersions("8.5", "9.1.0") < 0);
    }

    @Test
    void testGenerateJavaGradleKotlinDslWithSampleCode(@TempDir Path tempDir) throws IOException {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.JAVA,
                "untitled",
                tempDir,
                true,
                ProjectSpec.BuildSystem.GRADLE,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.stratosdb",
                "untitled",
                "com.stratosdb",
                "25",
                "", "", "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "",
                "", "", "", "GRADLE",
                true, // Add sample code
                "", "", "", "", "", "", "", "", "", "",
                "", "", false, "", "", "", "",
                "HTML5 Boilerplate", "v9.0.1", "React", "", "5.1.0", false,
                "", "", "", "",
                ProjectSpec.GradleDsl.KOTLIN,
                "Wrapper",
                "9.2.0",
                ""
        );

        Path projectDir = ProjectGenerator.generate(spec, msg -> {});
        assertEquals(tempDir.resolve("untitled"), projectDir);
        assertTrue(Files.isDirectory(projectDir));

        // 1. Verify settings.gradle.kts
        Path settingsPath = projectDir.resolve("settings.gradle.kts");
        assertTrue(Files.isRegularFile(settingsPath), "settings.gradle.kts must exist");
        String settings = Files.readString(settingsPath);
        assertTrue(settings.contains("rootProject.name = \"untitled\""), "settings should configure rootProject.name");

        // 2. Verify build.gradle.kts
        Path buildPath = projectDir.resolve("build.gradle.kts");
        assertTrue(Files.isRegularFile(buildPath), "build.gradle.kts must exist");
        String build = Files.readString(buildPath);
        assertTrue(build.contains("id(\"java\")"), "build.gradle.kts should include java plugin");
        assertTrue(build.contains("id(\"application\")"), "build.gradle.kts should include application plugin");
        assertTrue(build.contains("group = \"com.stratosdb\""), "build.gradle.kts should specify group");
        assertTrue(build.contains("version = \"1.0-SNAPSHOT\""), "build.gradle.kts should specify version");
        assertTrue(build.contains("JavaLanguageVersion.of(25)"), "build.gradle.kts should configure toolchain for JDK 25");
        assertTrue(build.contains("junit-bom:5.10.0"), "build.gradle.kts should declare JUnit BOM");
        assertTrue(build.contains("useJUnitPlatform()"), "build.gradle.kts should use JUnit Platform");
        assertTrue(build.contains("mainClass.set(\"com.stratosdb.Main\")"), "build.gradle.kts should set application mainClass");

        // 3. Verify Main.java
        Path mainPath = projectDir.resolve("src/main/java/com/stratosdb/Main.java");
        assertTrue(Files.isRegularFile(mainPath), "Main.java must exist when addSampleCode=true");
        String mainContent = Files.readString(mainPath);
        assertTrue(mainContent.contains("package com.stratosdb;"), "Main.java must declare package");
        assertTrue(mainContent.contains("System.out.println(\"Hello and welcome!\");"), "Main.java must have IntelliJ welcome line");
        assertTrue(mainContent.contains("for (int i = 1; i <= 5; i++)"), "Main.java must have IntelliJ loop");

        // 4. Verify Gradle wrapper
        Path gradlew = projectDir.resolve("gradlew");
        assertTrue(Files.isRegularFile(gradlew), "gradlew shell script must exist");
        assertTrue(Files.isExecutable(gradlew), "gradlew must have executable permissions");

        Path gradlewBat = projectDir.resolve("gradlew.bat");
        assertTrue(Files.isRegularFile(gradlewBat), "gradlew.bat must exist");

        Path wrapperProps = projectDir.resolve("gradle/wrapper/gradle-wrapper.properties");
        assertTrue(Files.isRegularFile(wrapperProps), "gradle-wrapper.properties must exist");
        String propsContent = Files.readString(wrapperProps);
        assertTrue(propsContent.contains("distributionUrl=https\\://services.gradle.org/distributions/gradle-9.2.0-bin.zip"),
                "wrapper properties must contain correct 9.2.0 distribution URL");

        Path wrapperJar = projectDir.resolve("gradle/wrapper/gradle-wrapper.jar");
        assertTrue(Files.isRegularFile(wrapperJar), "gradle-wrapper.jar must exist");
        try (JarFile jf = new JarFile(wrapperJar.toFile())) {
            assertNotNull(jf.getManifest(), "gradle-wrapper.jar must have valid manifest");
            assertEquals("org.gradle.wrapper.GradleWrapperMain",
                    jf.getManifest().getMainAttributes().getValue("Main-Class"));
        }

        // 5. Verify .gitignore and README.md
        Path gitignore = projectDir.resolve(".gitignore");
        assertTrue(Files.isRegularFile(gitignore));
        String gitignoreContent = Files.readString(gitignore);
        assertTrue(gitignoreContent.contains(".gradle/"));
        assertTrue(gitignoreContent.contains("!gradle/wrapper/gradle-wrapper.jar"));
        assertTrue(gitignoreContent.contains(".idea/"));

        assertTrue(Files.isRegularFile(projectDir.resolve("README.md")));
    }

    @Test
    void testGenerateJavaGradleGroovyDslWithSampleCode(@TempDir Path tempDir) throws IOException {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.JAVA,
                "groovy-proj",
                tempDir,
                false,
                ProjectSpec.BuildSystem.GRADLE,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "org.example",
                "groovy-proj",
                "org.example",
                "21",
                "", "", "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "",
                "", "", "", "GRADLE",
                true, // Add sample code
                "", "", "", "", "", "", "", "", "", "",
                "", "", false, "", "", "", "",
                "HTML5 Boilerplate", "v9.0.1", "React", "", "5.1.0", false,
                "", "", "", "",
                ProjectSpec.GradleDsl.GROOVY,
                "Wrapper",
                "8.12.1",
                ""
        );

        Path projectDir = ProjectGenerator.generate(spec, msg -> {});
        assertEquals(tempDir.resolve("groovy-proj"), projectDir);

        // Verify settings.gradle
        Path settingsPath = projectDir.resolve("settings.gradle");
        assertTrue(Files.isRegularFile(settingsPath), "settings.gradle must exist");
        String settings = Files.readString(settingsPath);
        assertTrue(settings.contains("rootProject.name = 'groovy-proj'"));

        // Verify build.gradle
        Path buildPath = projectDir.resolve("build.gradle");
        assertTrue(Files.isRegularFile(buildPath), "build.gradle must exist");
        String build = Files.readString(buildPath);
        assertTrue(build.contains("id 'java'"));
        assertTrue(build.contains("id 'application'"));
        assertTrue(build.contains("group = 'org.example'"));
        assertTrue(build.contains("JavaLanguageVersion.of(21)"));
        assertTrue(build.contains("useJUnitPlatform()"));
        assertTrue(build.contains("mainClass = 'org.example.Main'"));

        // Verify wrapper properties with 8.12.1
        Path wrapperProps = projectDir.resolve("gradle/wrapper/gradle-wrapper.properties");
        assertTrue(Files.isRegularFile(wrapperProps));
        String propsContent = Files.readString(wrapperProps);
        assertTrue(propsContent.contains("gradle-8.12.1-bin.zip"));
    }

    @Test
    void testGenerateJavaGradleWithoutSampleCode(@TempDir Path tempDir) throws IOException {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.JAVA,
                "clean-gradle",
                tempDir,
                false,
                ProjectSpec.BuildSystem.GRADLE,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.corp",
                "clean-gradle",
                "com.corp",
                "21",
                "", "", "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "",
                "", "", "", "GRADLE",
                false, // No sample code
                "", "", "", "", "", "", "", "", "", "",
                "", "", false, "", "", "", "",
                "HTML5 Boilerplate", "v9.0.1", "React", "", "5.1.0", false,
                "", "", "", "",
                ProjectSpec.GradleDsl.KOTLIN,
                "Wrapper",
                "9.2.0",
                ""
        );

        Path projectDir = ProjectGenerator.generate(spec, msg -> {});
        assertTrue(Files.isRegularFile(projectDir.resolve("build.gradle.kts")));
        assertTrue(Files.isRegularFile(projectDir.resolve("settings.gradle.kts")));

        // Main.java must NOT exist
        assertFalse(Files.exists(projectDir.resolve("src/main/java/com/corp/Main.java")),
                "Main.java must not exist when addSampleCode=false");

        // Standard Gradle directories must exist
        assertTrue(Files.isDirectory(projectDir.resolve("src/main/java")));
        assertTrue(Files.isDirectory(projectDir.resolve("src/main/resources")));
        assertTrue(Files.isDirectory(projectDir.resolve("src/test/java")));
    }

    @Test
    void testRunConfigurationForGradleProject(@TempDir Path tempDir) throws IOException {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.JAVA,
                "test-run",
                tempDir,
                false,
                ProjectSpec.BuildSystem.GRADLE,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "dev.test",
                "test-run",
                "dev.test",
                "21",
                "", "", "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "",
                "", "", "", "GRADLE",
                true,
                "", "", "", "", "", "", "", "", "", "",
                "", "", false, "", "", "", "",
                "HTML5 Boilerplate", "v9.0.1", "React", "", "5.1.0", false,
                "", "", "", "",
                ProjectSpec.GradleDsl.KOTLIN,
                "Wrapper",
                "9.2.0",
                ""
        );

        Path projectDir = ProjectGenerator.generate(spec, msg -> {});

        // RunConfiguration checks
        assertTrue(RunConfiguration.isGradleProject(projectDir), "Must be recognized as Gradle project");
        assertFalse(RunConfiguration.isMavenProject(projectDir), "Must not be recognized as Maven project");

        List<RunConfiguration> configs = RunConfiguration.detect(projectDir);
        assertFalse(configs.isEmpty(), "Should detect run configuration for Gradle application");
        assertTrue(configs.stream().anyMatch(c -> c.label().contains("Gradle run")),
                "Should have a 'Gradle run' configuration");

        List<List<String>> compileAndRun = RunConfiguration.compileAndRunClass(projectDir, "dev.test.Main");
        assertNotNull(compileAndRun);
        assertEquals(2, compileAndRun.size());
        assertTrue(compileAndRun.get(0).contains("classes"), "First command should compile classes");
        assertTrue(compileAndRun.get(1).contains("dev.test.Main"), "Second command should invoke main class");
    }

    @Test
    void testDetectLocalGradleInstallations() {
        List<String> installations = GradleMetadata.detectLocalInstallations();
        assertNotNull(installations);
    }
}
