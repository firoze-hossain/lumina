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

class GemGeneratorTest {

    @Test
    void testGemIconAvailable() {
        Node icon = GeneratorIcons.getIcon("Gem");
        assertNotNull(icon, "Gem icon must not be null");
    }

    @Test
    void testGenerateDefaultGemProject(@TempDir Path tempDir) throws IOException {
        List<String> logMessages = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.GEM,
                "untitled1",
                tempDir,
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.RUBY,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.example",
                "untitled1",
                "com.example.untitled1",
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
                "5.1.1", "2.0.9", "3.9.0", false, false, false, "", "untitled1",
                ProjectSpec.PythonInterpreterType.PROJECT_VENV, "/usr/bin/python3", "3.12", "",
                "", true, "Virtualenv", "", false, false,
                false,
                "", // rubyInterpreterPath
                false,
                "", false, "",
                "", "", "", true, true, "",
                "minitest", // gemTestingFramework
                true,       // gemCodeOfConduct (default: checked)
                true,       // gemMitLicense (default: checked)
                false,      // gemBinaryExecutable (default: unchecked)
                false       // gemCExtension (default: unchecked)
        );

        Path projectDir = ProjectGenerator.generate(spec, logMessages::add);
        assertNotNull(projectDir);
        assertTrue(Files.exists(projectDir));

        // Core files
        assertTrue(Files.exists(projectDir.resolve("untitled1.gemspec")));
        assertTrue(Files.exists(projectDir.resolve("Gemfile")));
        assertTrue(Files.exists(projectDir.resolve("Rakefile")));
        assertTrue(Files.exists(projectDir.resolve("README.md")));
        assertTrue(Files.exists(projectDir.resolve(".gitignore")));

        // lib/
        Path libFile = projectDir.resolve("lib").resolve("untitled1.rb");
        assertTrue(Files.exists(libFile));
        String libContent = Files.readString(libFile);
        assertTrue(libContent.contains("module Untitled1"));

        Path versionFile = projectDir.resolve("lib").resolve("untitled1").resolve("version.rb");
        assertTrue(Files.exists(versionFile));
        String versionContent = Files.readString(versionFile);
        assertTrue(versionContent.contains("VERSION = \"0.1.0\""));

        // Minitest
        assertTrue(Files.exists(projectDir.resolve("test").resolve("test_helper.rb")));
        assertTrue(Files.exists(projectDir.resolve("test").resolve("test_untitled1.rb")));
        assertFalse(Files.exists(projectDir.resolve("spec")));

        // Defaults: Code of conduct & MIT license present
        assertTrue(Files.exists(projectDir.resolve("CODE_OF_CONDUCT.md")));
        assertTrue(Files.exists(projectDir.resolve("LICENSE.txt")));

        // Defaults: Binary executable & C extension NOT present
        assertFalse(Files.exists(projectDir.resolve("exe")));
        assertFalse(Files.exists(projectDir.resolve("ext")));

        // .idea configuration
        Path ideaDir = projectDir.resolve(".idea");
        assertTrue(Files.exists(ideaDir));
        assertTrue(Files.exists(ideaDir.resolve("modules.xml")));
        assertTrue(Files.exists(ideaDir.resolve("untitled1.iml")));
        assertTrue(Files.exists(ideaDir.resolve("misc.xml")));

        String imlContent = Files.readString(ideaDir.resolve("untitled1.iml"));
        assertTrue(imlContent.contains("type=\"RUBY_MODULE\""));
    }

    @Test
    void testGenerateCustomGemProjectWithRSpecAndAllOptions(@TempDir Path tempDir) throws IOException {
        List<String> logMessages = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.GEM,
                "super-tool",
                tempDir,
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.RUBY,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.example",
                "super-tool",
                "com.example.supertool",
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
                "5.1.1", "2.0.9", "3.9.0", false, false, false, "", "super-tool",
                ProjectSpec.PythonInterpreterType.PROJECT_VENV, "/usr/bin/python3", "3.12", "",
                "", true, "Virtualenv", "", false, false,
                false,
                "",
                false,
                "", false, "",
                "", "", "", true, true, "",
                "rspec", // gemTestingFramework
                false,   // gemCodeOfConduct
                false,   // gemMitLicense
                true,    // gemBinaryExecutable
                true     // gemCExtension
        );

        Path projectDir = ProjectGenerator.generate(spec, logMessages::add);
        assertNotNull(projectDir);
        assertTrue(Files.exists(projectDir));

        // RSpec files
        assertTrue(Files.exists(projectDir.resolve(".rspec")));
        assertTrue(Files.exists(projectDir.resolve("spec").resolve("spec_helper.rb")));
        assertTrue(Files.exists(projectDir.resolve("spec").resolve("super-tool_spec.rb")));
        assertFalse(Files.exists(projectDir.resolve("test")));

        // Optional options
        assertFalse(Files.exists(projectDir.resolve("CODE_OF_CONDUCT.md")));
        assertFalse(Files.exists(projectDir.resolve("LICENSE.txt")));

        // Executable in exe/
        Path exeFile = projectDir.resolve("exe").resolve("super-tool");
        assertTrue(Files.exists(exeFile));
        String exeContent = Files.readString(exeFile);
        assertTrue(exeContent.contains("require \"super-tool\""));

        // C extension in ext/
        Path extDir = projectDir.resolve("ext").resolve("super-tool");
        assertTrue(Files.exists(extDir));
        assertTrue(Files.exists(extDir.resolve("extconf.rb")));
        assertTrue(Files.exists(extDir.resolve("super-tool.h")));
        assertTrue(Files.exists(extDir.resolve("super-tool.c")));

        String gemspec = Files.readString(projectDir.resolve("super-tool.gemspec"));
        assertTrue(gemspec.contains("spec.extensions = [\"ext/super-tool/extconf.rb\"]"));
        assertTrue(gemspec.contains("Super::Tool::VERSION"));
    }

    @Test
    void testProjectSpecGemGetters() {
        ProjectSpec defaultSpec = new ProjectSpec(
                ProjectSpec.Generator.GEM,
                "untitled1",
                Path.of("/tmp"),
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.RUBY,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.example", "untitled1", "com.example.untitled1", "25",
                "", "", "", "", "", "1.0", "",
                "", "", "", "",
                "", "", "", "MAVEN", false,
                "", "", "", "",
                "", "", "", "", "", "",
                "", "", false, "", "", "", "",
                "", "", "", "", "", false,
                "", "", "", "",
                ProjectSpec.GradleDsl.KOTLIN, "Wrapper", "9.2.0", "", false,
                "5.1.1", "2.0.9", "3.9.0", false, false, false, "", "untitled1",
                ProjectSpec.PythonInterpreterType.PROJECT_VENV, "/usr/bin/python3", "3.12", "",
                "", true, "Virtualenv", "", false, false,
                false, "", false, "", false, "",
                "", "", "", true, true, ""
        );

        assertEquals("minitest", defaultSpec.safeGemTestingFramework());
        assertTrue(defaultSpec.safeGemCodeOfConduct());
        assertTrue(defaultSpec.safeGemMitLicense());
        assertFalse(defaultSpec.safeGemBinaryExecutable());
        assertFalse(defaultSpec.safeGemCExtension());
    }
}
