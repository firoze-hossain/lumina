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

class RailsGeneratorTest {

    @Test
    void testRailsIconAvailable() {
        Node icon1 = GeneratorIcons.getIcon("Ruby on Rails");
        assertNotNull(icon1, "Ruby on Rails icon must not be null");

        Node icon2 = GeneratorIcons.getIcon("Rails");
        assertNotNull(icon2, "Rails icon must not be null");
    }

    @Test
    void testGenerateDefaultRailsProject(@TempDir Path tempDir) throws IOException {
        List<String> logMessages = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.RUBY_ON_RAILS,
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
                "minitest", true, true, false, false,
                "8.0.1",        // railsVersion
                "Ruby on Rails",// railsType
                "SQLite3",      // railsDatabase
                true,           // railsJsFrameworkEnabled
                "Importmap",    // railsJsFramework
                ""              // railsExtraOptions
        );

        assertEquals("8.0.1", spec.safeRailsVersion());
        assertEquals("Ruby on Rails", spec.safeRailsType());
        assertEquals("SQLite3", spec.safeRailsDatabase());
        assertTrue(spec.safeRailsJsFrameworkEnabled());
        assertEquals("Importmap", spec.safeRailsJsFramework());
        assertEquals("", spec.safeRailsExtraOptions());

        Path projectDir = ProjectGenerator.generate(spec, logMessages::add);
        assertNotNull(projectDir);
        assertTrue(Files.exists(projectDir));

        // Core Rails files
        assertTrue(Files.exists(projectDir.resolve("Gemfile")));
        assertTrue(Files.exists(projectDir.resolve("Rakefile")));
        assertTrue(Files.exists(projectDir.resolve("config.ru")));
        assertTrue(Files.exists(projectDir.resolve(".gitignore")));
        assertTrue(Files.exists(projectDir.resolve("README.md")));

        // Config
        assertTrue(Files.exists(projectDir.resolve("config/application.rb")));
        assertTrue(Files.exists(projectDir.resolve("config/database.yml")));
        assertTrue(Files.exists(projectDir.resolve("config/routes.rb")));
        assertTrue(Files.exists(projectDir.resolve("config/boot.rb")));
        assertTrue(Files.exists(projectDir.resolve("config/environment.rb")));
        assertTrue(Files.exists(projectDir.resolve("config/environments/development.rb")));
        assertTrue(Files.exists(projectDir.resolve("config/environments/production.rb")));
        assertTrue(Files.exists(projectDir.resolve("config/environments/test.rb")));

        // Bin executables
        Path binRails = projectDir.resolve("bin/rails");
        assertTrue(Files.exists(binRails));
        Path binSetup = projectDir.resolve("bin/setup");
        assertTrue(Files.exists(binSetup));

        // App MVC
        assertTrue(Files.exists(projectDir.resolve("app/controllers/application_controller.rb")));
        assertTrue(Files.exists(projectDir.resolve("app/models/application_record.rb")));
        assertTrue(Files.exists(projectDir.resolve("app/views/layouts/application.html.erb")));
        assertTrue(Files.exists(projectDir.resolve("app/javascript/application.js")));
        assertTrue(Files.exists(projectDir.resolve("config/importmap.rb")));

        // .idea
        Path ideaDir = projectDir.resolve(".idea");
        assertTrue(Files.exists(ideaDir.resolve("modules.xml")));
        assertTrue(Files.exists(ideaDir.resolve("misc.xml")));
        assertTrue(Files.exists(ideaDir.resolve("untitled1.iml")));
        String iml = Files.readString(ideaDir.resolve("untitled1.iml"));
        assertTrue(iml.contains("<facet type=\"RubyOnRails\" name=\"Ruby on Rails\">"));
    }

    @Test
    void testGenerateRailsApiProject(@TempDir Path tempDir) throws IOException {
        List<String> logMessages = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.RUBY_ON_RAILS,
                "api_demo",
                tempDir,
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.RUBY,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.example",
                "api_demo",
                "com.example.api_demo",
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
                "5.1.1", "2.0.9", "3.9.0", false, false, false, "", "api_demo",
                ProjectSpec.PythonInterpreterType.PROJECT_VENV, "/usr/bin/python3", "3.12", "",
                "", true, "Virtualenv", "", false, false,
                false,
                "", false,
                "", false, "",
                "", "", "", true, true, "",
                "minitest", true, true, false, false,
                "7.2.2",
                "Rails API",
                "PostgreSQL",
                false,
                "Importmap",
                ""
        );

        Path projectDir = ProjectGenerator.generate(spec, logMessages::add);
        assertNotNull(projectDir);
        assertTrue(Files.exists(projectDir));

        String appRb = Files.readString(projectDir.resolve("config/application.rb"));
        assertTrue(appRb.contains("config.api_only = true"));

        String controller = Files.readString(projectDir.resolve("app/controllers/application_controller.rb"));
        assertTrue(controller.contains("ActionController::API"));

        assertFalse(Files.exists(projectDir.resolve("app/views/layouts/application.html.erb")));
    }

    @Test
    void testGenerateMountableEngineProject(@TempDir Path tempDir) throws IOException {
        List<String> logMessages = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.RUBY_ON_RAILS,
                "my_engine",
                tempDir,
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.RUBY,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.example",
                "my_engine",
                "com.example.my_engine",
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
                "5.1.1", "2.0.9", "3.9.0", false, false, false, "", "my_engine",
                ProjectSpec.PythonInterpreterType.PROJECT_VENV, "/usr/bin/python3", "3.12", "",
                "", true, "Virtualenv", "", false, false,
                false,
                "", false,
                "", false, "",
                "", "", "", true, true, "",
                "minitest", true, true, false, false,
                "8.0.1",
                "Mountable Engine",
                "MySQL",
                false,
                "Importmap",
                ""
        );

        Path projectDir = ProjectGenerator.generate(spec, logMessages::add);
        assertNotNull(projectDir);
        assertTrue(Files.exists(projectDir));

        assertTrue(Files.exists(projectDir.resolve("my_engine.gemspec")));
        assertTrue(Files.exists(projectDir.resolve("lib/my_engine.rb")));
        assertTrue(Files.exists(projectDir.resolve("lib/my_engine/version.rb")));
        assertTrue(Files.exists(projectDir.resolve("lib/my_engine/engine.rb")));
        assertTrue(Files.exists(projectDir.resolve("config/routes.rb")));
        assertTrue(Files.exists(projectDir.resolve("app/controllers/my_engine/application_controller.rb")));

        String iml = Files.readString(projectDir.resolve(".idea/my_engine.iml"));
        assertTrue(iml.contains("<facet type=\"RubyOnRails\" name=\"Ruby on Rails\">"));
    }
}
