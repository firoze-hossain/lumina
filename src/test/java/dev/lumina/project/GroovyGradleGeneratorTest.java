package dev.lumina.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GroovyGradleGeneratorTest {

    @Test
    void testGroovyGradleGroovyDslSingleModule(@TempDir Path tempDir) throws IOException {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.GROOVY,
                "groovy-single",
                tempDir,
                false,
                ProjectSpec.BuildSystem.GRADLE,
                ProjectSpec.Language.GROOVY,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "org.example",
                "groovy-single",
                "org.example",
                "21",
                "", "", "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "",
                "", "", "", "GRADLE",
                true,
                "", "", "", "", "", "", "", "", "", "",
                "", "", false, "", "", "", "",
                "HTML5 Boilerplate", "v9.0.1", "React", "", "5.1.0", false,
                "", "", "", "",
                ProjectSpec.GradleDsl.GROOVY,
                "Wrapper",
                "9.2.0",
                "",
                false
        );

        Path projectDir = ProjectGenerator.generate(spec, msg -> {});
        assertEquals(tempDir.resolve("groovy-single"), projectDir);

        // Root settings.gradle
        Path settingsPath = projectDir.resolve("settings.gradle");
        assertTrue(Files.isRegularFile(settingsPath), "settings.gradle must exist");
        String settings = Files.readString(settingsPath);
        assertTrue(settings.contains("rootProject.name = 'groovy-single'"));

        // Root build.gradle
        Path buildPath = projectDir.resolve("build.gradle");
        assertTrue(Files.isRegularFile(buildPath), "build.gradle must exist");
        String build = Files.readString(buildPath);
        assertTrue(build.contains("id 'groovy'"));
        assertTrue(build.contains("id 'application'"));
        assertTrue(build.contains("org.apache.groovy:groovy:4.0.24"));
        assertTrue(build.contains("JavaLanguageVersion.of(21)"));
        assertTrue(build.contains("mainClass = 'org.example.Main'"));

        // Sources
        Path mainGroovy = projectDir.resolve("src/main/groovy/org/example/Main.groovy");
        assertTrue(Files.isRegularFile(mainGroovy), "Main.groovy must exist");
        String mainContent = Files.readString(mainGroovy);
        assertTrue(mainContent.contains("package org.example"));
        assertTrue(mainContent.contains("static void main(String[] args)"));

        // Tests
        Path mainTest = projectDir.resolve("src/test/groovy/org/example/MainTest.groovy");
        assertTrue(Files.isRegularFile(mainTest), "MainTest.groovy must exist");

        // .idea/gradle.xml
        Path ideaGradleXml = projectDir.resolve(".idea/gradle.xml");
        assertTrue(Files.isRegularFile(ideaGradleXml));
        String ideaGradle = Files.readString(ideaGradleXml);
        assertTrue(ideaGradle.contains("DEFAULT_WRAPPED"));
    }

    @Test
    void testGroovyGradleKotlinDslMultiModule(@TempDir Path tempDir) throws IOException {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.GROOVY,
                "multi-groovy",
                tempDir,
                false,
                ProjectSpec.BuildSystem.GRADLE,
                ProjectSpec.Language.GROOVY,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "org.example",
                "multi-groovy",
                "org.example",
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
                "",
                true // multi-module
        );

        Path projectDir = ProjectGenerator.generate(spec, msg -> {});

        // Root settings.gradle.kts
        Path settingsPath = projectDir.resolve("settings.gradle.kts");
        assertTrue(Files.isRegularFile(settingsPath));
        String settings = Files.readString(settingsPath);
        assertTrue(settings.contains("rootProject.name = \"multi-groovy\""));
        assertTrue(settings.contains("include(\"app\")"));

        // Subproject app/build.gradle.kts
        Path appBuild = projectDir.resolve("app/build.gradle.kts");
        assertTrue(Files.isRegularFile(appBuild));
        String appContent = Files.readString(appBuild);
        assertTrue(appContent.contains("groovy"));
        assertTrue(appContent.contains("application"));
        assertTrue(appContent.contains("org.apache.groovy:groovy:4.0.24"));

        // Subproject sources
        Path mainGroovy = projectDir.resolve("app/src/main/groovy/org/example/Main.groovy");
        assertTrue(Files.isRegularFile(mainGroovy));
    }
}
