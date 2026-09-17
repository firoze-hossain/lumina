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

class RubyGeneratorTest {

    @Test
    void testRubyIconCreated() {
        Node icon = GeneratorIcons.getIcon("Ruby");
        assertNotNull(icon, "Ruby icon must not be null");
    }

    @Test
    void testGenerateRubyProjectWithoutSampleCode(@TempDir Path tempDir) throws IOException {
        List<String> logMessages = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.RUBY,
                "ruby-demo",
                tempDir,
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.RUBY,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.example",
                "ruby-demo",
                "com.example.rubydemo",
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
                "5.1.1", "2.0.9", "3.9.0", false, false, false, "", "ruby-demo",
                ProjectSpec.PythonInterpreterType.PROJECT_VENV, "/usr/bin/python3", "3.12", "",
                "", true, "Virtualenv", "", false, false,
                false, // phpAddComposerJson
                "/usr/bin/ruby", // rubyInterpreterPath
                false  // rubyAddSampleCode
        );

        Path projectDir = ProjectGenerator.generate(spec, logMessages::add);
        assertNotNull(projectDir);
        assertTrue(Files.exists(projectDir));

        // .gitignore
        Path gitignorePath = projectDir.resolve(".gitignore");
        assertTrue(Files.exists(gitignorePath));
        String gitignoreContent = Files.readString(gitignorePath);
        assertTrue(gitignoreContent.contains("/.bundle/"));
        assertTrue(gitignoreContent.contains("/vendor/bundle/"));
        assertTrue(gitignoreContent.contains("*.gem"));
        assertTrue(gitignoreContent.contains(".idea/"));

        // main.rb should NOT exist when unchecked
        Path mainRb = projectDir.resolve("main.rb");
        assertFalse(Files.exists(mainRb), "main.rb should not exist when rubyAddSampleCode is false");

        // .idea configuration
        Path ideaDir = projectDir.resolve(".idea");
        assertTrue(Files.exists(ideaDir));
        assertTrue(Files.exists(ideaDir.resolve("modules.xml")));
        assertTrue(Files.exists(ideaDir.resolve("ruby-demo.iml")));
        assertTrue(Files.exists(ideaDir.resolve("misc.xml")));

        String imlContent = Files.readString(ideaDir.resolve("ruby-demo.iml"));
        assertTrue(imlContent.contains("type=\"RUBY_MODULE\""));

        String miscXmlContent = Files.readString(ideaDir.resolve("misc.xml"));
        assertTrue(miscXmlContent.contains("project-jdk-name=\"ruby (/usr/bin/ruby)\"")
                || miscXmlContent.contains("project-jdk-name=\"Ruby\"")
                || miscXmlContent.contains("project-jdk-name=\"ruby-"));
        assertTrue(miscXmlContent.contains("project-jdk-type=\"RUBY_SDK\""));
    }

    @Test
    void testGenerateRubyProjectWithSampleCode(@TempDir Path tempDir) throws IOException {
        List<String> logMessages = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.RUBY,
                "ruby-sample-app",
                tempDir,
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.RUBY,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.example",
                "ruby-sample-app",
                "com.example.rubysampleapp",
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
                "5.1.1", "2.0.9", "3.9.0", false, false, false, "", "ruby-sample-app",
                ProjectSpec.PythonInterpreterType.PROJECT_VENV, "/usr/bin/python3", "3.12", "",
                "", true, "Virtualenv", "", false, false,
                false, // phpAddComposerJson
                "",    // rubyInterpreterPath empty (no interpreter selected)
                true   // rubyAddSampleCode = true
        );

        Path projectDir = ProjectGenerator.generate(spec, logMessages::add);
        assertNotNull(projectDir);
        assertTrue(Files.exists(projectDir));

        // main.rb should exist when checked
        Path mainRb = projectDir.resolve("main.rb");
        assertTrue(Files.exists(mainRb), "main.rb must exist when rubyAddSampleCode is true");
        String mainRbContent = Files.readString(mainRb);
        assertTrue(mainRbContent.contains("def print_hi(name)"));
        assertTrue(mainRbContent.contains("puts \"Hi, #{name}\""));
        assertTrue(mainRbContent.contains("print_hi('Ruby')"));

        // .idea configuration
        Path ideaDir = projectDir.resolve(".idea");
        assertTrue(Files.exists(ideaDir));
        assertTrue(Files.exists(ideaDir.resolve("misc.xml")));

        String miscXmlContent = Files.readString(ideaDir.resolve("misc.xml"));
        assertTrue(miscXmlContent.contains("project-jdk-name=\"Ruby\""));
        assertTrue(miscXmlContent.contains("project-jdk-type=\"RUBY_SDK\""));
    }

    @Test
    void testGenerateRubyProjectWithGit(@TempDir Path tempDir) throws IOException {
        List<String> logMessages = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.RUBY,
                "ruby-git-app",
                tempDir,
                true, // initGit = true
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.RUBY,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.example",
                "ruby-git-app",
                "com.example.rubygitapp",
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
                "5.1.1", "2.0.9", "3.9.0", false, false, false, "", "ruby-git-app",
                ProjectSpec.PythonInterpreterType.PROJECT_VENV, "/usr/bin/python3", "3.12", "",
                "", true, "Virtualenv", "", false, false,
                false, // phpAddComposerJson
                "/usr/bin/ruby",
                false
        );

        Path projectDir = ProjectGenerator.generate(spec, logMessages::add);
        assertNotNull(projectDir);
        assertTrue(Files.exists(projectDir));
        assertTrue(Files.exists(projectDir.resolve(".git")), ".git folder should be created when initGit is true");
    }
}
