package dev.lumina.project;

import dev.lumina.ui.GeneratorIcons;
import javafx.scene.Node;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PhpGeneratorTest {

    @Test
    void testPhpIconCreated() {
        Node icon = GeneratorIcons.getIcon("PHP");
        assertNotNull(icon, "PHP icon must not be null");
    }

    @Test
    void testGeneratePhpProjectWithoutComposer(@TempDir Path tempDir) throws IOException {
        List<String> logMessages = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.PHP,
                "php-demo",
                tempDir,
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.PHP,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.example",
                "php-demo",
                "com.example.phpdemo",
                "25",
                "", "", "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "",
                "", "", "", "MAVEN", false,
                "", "", "", "",
                "", "", "", "", "", "",
                "", "", false, "", "", "", "",
                "", "", "", "", "", false,
                "", "", "", "",
                ProjectSpec.GradleDsl.KOTLIN, "Wrapper", "9.2.0", "", false,
                "5.1.1", "2.0.9", "3.9.0", false, false, false, "", "php-demo",
                ProjectSpec.PythonInterpreterType.PROJECT_VENV, "/usr/bin/python3", "3.12", "",
                "", true, "Virtualenv", "", false, false,
                false // phpAddComposerJson = false
        );

        Path projectDir = ProjectGenerator.generate(spec, logMessages::add);
        assertNotNull(projectDir);
        assertTrue(Files.exists(projectDir));

        // index.php
        Path indexPath = projectDir.resolve("index.php");
        assertTrue(Files.exists(indexPath));
        String indexContent = Files.readString(indexPath);
        assertTrue(indexContent.contains("<?php"));
        assertTrue(indexContent.contains("Hello, World!"));

        // .gitignore
        Path gitignorePath = projectDir.resolve(".gitignore");
        assertTrue(Files.exists(gitignorePath));
        String gitignoreContent = Files.readString(gitignorePath);
        assertTrue(gitignoreContent.contains("/vendor/"));
        assertTrue(gitignoreContent.contains(".idea/"));

        // composer.json should NOT exist when unchecked
        Path composerJson = projectDir.resolve("composer.json");
        assertFalse(Files.exists(composerJson), "composer.json should not exist when phpAddComposerJson is false");

        // .idea configuration
        Path ideaDir = projectDir.resolve(".idea");
        assertTrue(Files.exists(ideaDir));
        assertTrue(Files.exists(ideaDir.resolve("modules.xml")));
        assertTrue(Files.exists(ideaDir.resolve("php-demo.iml")));
        assertTrue(Files.exists(ideaDir.resolve("php.xml")));

        String imlContent = Files.readString(ideaDir.resolve("php-demo.iml"));
        assertTrue(imlContent.contains("type=\"WEB_MODULE\""));

        String phpXmlContent = Files.readString(ideaDir.resolve("php.xml"));
        assertTrue(phpXmlContent.contains("PhpProjectSharedConfiguration"));
        assertTrue(phpXmlContent.contains("php_language_level="));
    }

    @Test
    void testGeneratePhpProjectWithComposer(@TempDir Path tempDir) throws IOException {
        List<String> logMessages = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.PHP,
                "composer-app",
                tempDir,
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.PHP,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.example",
                "composer-app",
                "com.example.composerapp",
                "25",
                "", "", "", "", "", "1.0-SNAPSHOT", "",
                "", "", "", "",
                "", "", "", "MAVEN", false,
                "", "", "", "",
                "", "", "", "", "", "",
                "", "", false, "", "", "", "",
                "", "", "", "", "", false,
                "", "", "", "",
                ProjectSpec.GradleDsl.KOTLIN, "Wrapper", "9.2.0", "", false,
                "5.1.1", "2.0.9", "3.9.0", false, false, false, "", "composer-app",
                ProjectSpec.PythonInterpreterType.PROJECT_VENV, "/usr/bin/python3", "3.12", "",
                "", true, "Virtualenv", "", false, false,
                true // phpAddComposerJson = true
        );

        Path projectDir = ProjectGenerator.generate(spec, logMessages::add);
        assertNotNull(projectDir);
        assertTrue(Files.exists(projectDir));

        // composer.json should exist when checked
        Path composerJson = projectDir.resolve("composer.json");
        assertTrue(Files.exists(composerJson), "composer.json must exist when phpAddComposerJson is true");
        String composerContent = Files.readString(composerJson);
        assertTrue(composerContent.contains("\"name\":"));
        assertTrue(composerContent.contains("/composer-app\""));
        assertTrue(composerContent.contains("\"type\": \"project\""));
        assertTrue(composerContent.contains("\"require\": {}"));

        // src directory
        Path srcDir = projectDir.resolve("src");
        assertTrue(Files.exists(srcDir) && Files.isDirectory(srcDir), "src directory should be created with composer.json");

        // index.php & .gitignore & .idea
        assertTrue(Files.exists(projectDir.resolve("index.php")));
        assertTrue(Files.exists(projectDir.resolve(".gitignore")));
        assertTrue(Files.exists(projectDir.resolve(".idea/php.xml")));
    }
}
