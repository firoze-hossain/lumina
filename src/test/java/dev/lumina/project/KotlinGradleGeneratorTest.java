package dev.lumina.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class KotlinGradleGeneratorTest {

    @Test
    void testKotlinGradleKtsSingleModule(@TempDir Path tempDir) throws IOException {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.KOTLIN,
                "kotlin-demo",
                tempDir,
                true,
                ProjectSpec.BuildSystem.GRADLE,
                ProjectSpec.Language.KOTLIN,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "org.example",
                "kotlin-demo",
                "org.example",
                "25",
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
                false
        );

        Path projectDir = ProjectGenerator.generate(spec, msg -> {});
        assertEquals(tempDir.resolve("kotlin-demo"), projectDir);

        // Root settings.gradle.kts
        Path settingsPath = projectDir.resolve("settings.gradle.kts");
        assertTrue(Files.isRegularFile(settingsPath), "settings.gradle.kts must exist");
        String settings = Files.readString(settingsPath);
        assertTrue(settings.contains("rootProject.name = \"kotlin-demo\""));
        assertFalse(settings.contains("include(\"app\")"));

        // Root build.gradle.kts
        Path buildPath = projectDir.resolve("build.gradle.kts");
        assertTrue(Files.isRegularFile(buildPath), "build.gradle.kts must exist");
        String build = Files.readString(buildPath);
        assertTrue(build.contains("kotlin(\"jvm\")"));
        assertTrue(build.contains("application"));
        assertTrue(build.contains("group = \"org.example\""));
        assertTrue(build.contains("jvmToolchain(25)"));
        assertTrue(build.contains("mainClass.set(\"org.example.MainKt\")"));

        // Sources
        Path mainKt = projectDir.resolve("src/main/kotlin/org/example/Main.kt");
        assertTrue(Files.isRegularFile(mainKt), "Main.kt must exist");
        String mainContent = Files.readString(mainKt);
        assertTrue(mainContent.contains("package org.example"));
        assertTrue(mainContent.contains("fun main()"));

        // Wrapper files
        assertTrue(Files.isRegularFile(projectDir.resolve("gradlew")));
        assertTrue(Files.isRegularFile(projectDir.resolve("gradlew.bat")));
        assertTrue(Files.isRegularFile(projectDir.resolve("gradle/wrapper/gradle-wrapper.properties")));
        assertTrue(Files.isRegularFile(projectDir.resolve("gradle/wrapper/gradle-wrapper.jar")));

        // .idea/gradle.xml
        Path ideaGradleXml = projectDir.resolve(".idea/gradle.xml");
        assertTrue(Files.isRegularFile(ideaGradleXml), ".idea/gradle.xml must exist");
        String ideaGradle = Files.readString(ideaGradleXml);
        assertTrue(ideaGradle.contains("DEFAULT_WRAPPED"));
    }

    @Test
    void testKotlinGradleGroovyDsl(@TempDir Path tempDir) throws IOException {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.KOTLIN,
                "kotlin-groovy-dsl",
                tempDir,
                false,
                ProjectSpec.BuildSystem.GRADLE,
                ProjectSpec.Language.KOTLIN,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.test",
                "kotlin-groovy-dsl",
                "com.test",
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
                "9.1.0",
                "",
                false
        );

        Path projectDir = ProjectGenerator.generate(spec, msg -> {});
        assertTrue(Files.isRegularFile(projectDir.resolve("settings.gradle")));
        assertTrue(Files.isRegularFile(projectDir.resolve("build.gradle")));

        String build = Files.readString(projectDir.resolve("build.gradle"));
        assertTrue(build.contains("id 'org.jetbrains.kotlin.jvm'"));
        assertTrue(build.contains("id 'application'"));
        assertTrue(build.contains("jvmToolchain(21)"));
        assertTrue(build.contains("mainClass = 'com.test.MainKt'"));
    }

    @Test
    void testKotlinGradleMultiModule(@TempDir Path tempDir) throws IOException {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.KOTLIN,
                "multi-kotlin",
                tempDir,
                true,
                ProjectSpec.BuildSystem.GRADLE,
                ProjectSpec.Language.KOTLIN,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "org.example",
                "multi-kotlin",
                "org.example",
                "25",
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
                true
        );

        Path projectDir = ProjectGenerator.generate(spec, msg -> {});

        // Root settings.gradle.kts must include app subproject
        Path settingsPath = projectDir.resolve("settings.gradle.kts");
        assertTrue(Files.isRegularFile(settingsPath));
        String settings = Files.readString(settingsPath);
        assertTrue(settings.contains("rootProject.name = \"multi-kotlin\""));
        assertTrue(settings.contains("include(\"app\")"));

        // Subproject app/ must contain build.gradle.kts and src/
        Path appBuild = projectDir.resolve("app/build.gradle.kts");
        assertTrue(Files.isRegularFile(appBuild), "app/build.gradle.kts must exist");
        String appBuildContent = Files.readString(appBuild);
        assertTrue(appBuildContent.contains("kotlin(\"jvm\")"));
        assertTrue(appBuildContent.contains("application"));
        assertTrue(appBuildContent.contains("jvmToolchain(25)"));

        // Sources must be in app/src/main/kotlin/
        Path mainKt = projectDir.resolve("app/src/main/kotlin/org/example/Main.kt");
        assertTrue(Files.isRegularFile(mainKt), "app/src/main/kotlin/org/example/Main.kt must exist");
        assertFalse(Files.exists(projectDir.resolve("src/main/kotlin")), "Root src/ must not exist in multi-module build");
    }

    @Test
    void testKotlinGradleLocalInstallation(@TempDir Path tempDir) throws IOException {
        Path localGradle = tempDir.resolve("local-gradle-home");
        Path libDir = localGradle.resolve("lib");
        Files.createDirectories(libDir);
        Files.createFile(libDir.resolve("gradle-launcher-9.1.0.jar"));

        Path projectsParent = tempDir.resolve("projects");
        Files.createDirectories(projectsParent);

        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.KOTLIN,
                "local-kotlin",
                projectsParent,
                false,
                ProjectSpec.BuildSystem.GRADLE,
                ProjectSpec.Language.KOTLIN,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "org.example",
                "local-kotlin",
                "org.example",
                "25",
                "", "", "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "",
                "", "", "", "GRADLE",
                true,
                "", "", "", "", "", "", "", "", "", "",
                "", "", false, "", "", "", "",
                "HTML5 Boilerplate", "v9.0.1", "React", "", "5.1.0", false,
                "", "", "", "",
                ProjectSpec.GradleDsl.KOTLIN,
                "Local installation",
                "9.2.0",
                localGradle.toAbsolutePath().toString(),
                false
        );

        Path projectDir = ProjectGenerator.generate(spec, msg -> {});

        // .idea/gradle.xml must have distributionType LOCAL and gradleHome
        Path ideaGradleXml = projectDir.resolve(".idea/gradle.xml");
        assertTrue(Files.isRegularFile(ideaGradleXml), ".idea/gradle.xml must exist");
        String ideaGradle = Files.readString(ideaGradleXml);
        assertTrue(ideaGradle.contains("value=\"LOCAL\""));
        assertTrue(ideaGradle.contains(localGradle.toAbsolutePath().toString().replace("\\", "/")));

        // gradle-wrapper.properties must have detected version 9.1.0 from the jar
        Path wrapperProps = projectDir.resolve("gradle/wrapper/gradle-wrapper.properties");
        assertTrue(Files.isRegularFile(wrapperProps));
        String propsContent = Files.readString(wrapperProps);
        assertTrue(propsContent.contains("gradle-9.1.0-bin.zip"));
    }
}
