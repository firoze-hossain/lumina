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

class AppEngineGeneratorTest {

    @Test
    void testAppEngineIconsAvailable() {
        Node icon1 = GeneratorIcons.getIcon("App Engine");
        assertNotNull(icon1, "App Engine icon must not be null");

        Node icon2 = GeneratorIcons.getIcon("Google App Engine");
        assertNotNull(icon2, "Google App Engine icon must not be null");

        Node sqlIcon = GeneratorIcons.getIcon("SQL Support");
        assertNotNull(sqlIcon, "SQL Support icon must not be null");
    }

    @Test
    void testGenerateDefaultAppEngineProject(@TempDir Path tempDir) throws IOException {
        List<String> logMessages = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.APP_ENGINE,
                "untitled1",
                tempDir,
                false,
                ProjectSpec.BuildSystem.INTELLIJ,
                ProjectSpec.Language.GO,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.example",
                "untitled1",
                "untitled1",
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
                "",
                false,
                "/usr/local/go", true, "",
                "", "", "", true, true, "",
                "minitest", true, true, false, false,
                "8.0.1", "Ruby on Rails", "SQLite3", true, "Importmap", "",
                "/usr/local/go",
                false,
                false,
                "<No interpreter>",
                false,
                "untitled1",
                tempDir.resolve("untitled1").toString(),
                tempDir.resolve("untitled1").toString(),
                AppEngineMetadata.DEFAULT_PROJECT_FORMAT
        );

        assertEquals("/usr/local/go", spec.safeAppEngineGoRoot());
        assertFalse(spec.safeAppEngineIndexEntireGopath());
        assertFalse(spec.safeAppEnginePythonSupport());
        assertEquals("<No interpreter>", spec.safeAppEnginePythonSdk());
        assertFalse(spec.safeAppEngineSqlSupport());
        assertEquals("untitled1", spec.safeAppEngineModuleName());
        assertEquals(tempDir.resolve("untitled1").toString(), spec.safeAppEngineContentRoot());
        assertEquals(tempDir.resolve("untitled1").toString(), spec.safeAppEngineModuleFileLocation());
        assertEquals(AppEngineMetadata.DEFAULT_PROJECT_FORMAT, spec.safeAppEngineProjectFormat());

        Path projectDir = ProjectGenerator.generate(spec, logMessages::add);
        assertNotNull(projectDir);
        assertTrue(Files.exists(projectDir));

        // Core App Engine files
        Path appYaml = projectDir.resolve("app.yaml");
        assertTrue(Files.exists(appYaml));
        String appYamlContent = Files.readString(appYaml);
        assertTrue(appYamlContent.contains("runtime: go122"));
        assertTrue(appYamlContent.contains("instance_class: F1"));

        Path mainGo = projectDir.resolve("main.go");
        assertTrue(Files.exists(mainGo));
        String mainGoContent = Files.readString(mainGo);
        assertTrue(mainGoContent.contains("package main"));
        assertTrue(mainGoContent.contains("http.HandleFunc(\"/\", indexHandler)"));
        assertTrue(mainGoContent.contains("http.HandleFunc(\"/_ah/health\", healthHandler)"));

        Path goMod = projectDir.resolve("go.mod");
        assertTrue(Files.exists(goMod));
        String goModContent = Files.readString(goMod);
        assertTrue(goModContent.contains("module untitled1"));

        Path readme = projectDir.resolve("README.md");
        assertTrue(Files.exists(readme));
        assertTrue(Files.readString(readme).contains("Google App Engine"));

        Path gitignore = projectDir.resolve(".gitignore");
        assertTrue(Files.exists(gitignore));

        // No SQL or Python companion by default
        assertFalse(Files.exists(projectDir.resolve("db.go")));
        assertFalse(Files.exists(projectDir.resolve("companion.py")));

        // Idea files
        Path ideaDir = projectDir.resolve(".idea");
        assertTrue(Files.isDirectory(ideaDir));
        assertTrue(Files.exists(ideaDir.resolve("modules.xml")));
        assertTrue(Files.exists(ideaDir.resolve("untitled1.iml")));
        assertTrue(Files.exists(ideaDir.resolve("misc.xml")));

        String imlContent = Files.readString(ideaDir.resolve("untitled1.iml"));
        assertTrue(imlContent.contains("google-app-engine-go"));
    }

    @Test
    void testGenerateAppEngineWithSqlAndPythonCompanion(@TempDir Path tempDir) throws IOException {
        List<String> logMessages = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.APP_ENGINE,
                "full-gae-app",
                tempDir,
                false,
                ProjectSpec.BuildSystem.INTELLIJ,
                ProjectSpec.Language.GO,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.example",
                "full-gae-app",
                "full-gae-app",
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
                "5.1.1", "2.0.9", "3.9.0", false, false, false, "", "full-gae-app",
                ProjectSpec.PythonInterpreterType.PROJECT_VENV, "/usr/bin/python3", "3.12", "",
                "", true, "Virtualenv", "", false, false,
                false,
                "",
                false,
                "/usr/local/go", true, "",
                "", "", "", true, true, "",
                "minitest", true, true, false, false,
                "8.0.1", "Ruby on Rails", "SQLite3", true, "Importmap", "",
                "/usr/local/go",
                true,   // index entire GOPATH
                true,   // python support
                "/usr/bin/python3", // python sdk
                true,   // sql support
                "full-gae-app",
                tempDir.resolve("full-gae-app").toString(),
                tempDir.resolve("full-gae-app").toString(),
                AppEngineMetadata.DEFAULT_PROJECT_FORMAT
        );

        assertTrue(spec.safeAppEngineIndexEntireGopath());
        assertTrue(spec.safeAppEnginePythonSupport());
        assertTrue(spec.safeAppEngineSqlSupport());
        assertEquals("/usr/bin/python3", spec.safeAppEnginePythonSdk());

        Path projectDir = ProjectGenerator.generate(spec, logMessages::add);
        assertNotNull(projectDir);
        assertTrue(Files.exists(projectDir));

        // db.go should be present
        Path dbGo = projectDir.resolve("db.go");
        assertTrue(Files.exists(dbGo));
        String dbGoContent = Files.readString(dbGo);
        assertTrue(dbGoContent.contains("initDatabase()"));

        // companion.py should be present
        Path companionPy = projectDir.resolve("companion.py");
        assertTrue(Files.exists(companionPy));
        String pyContent = Files.readString(companionPy);
        assertTrue(pyContent.contains("python3"));

        // app.yaml should have SQL beta settings
        Path appYaml = projectDir.resolve("app.yaml");
        String appYamlContent = Files.readString(appYaml);
        assertTrue(appYamlContent.contains("cloudsql_instances"));

        // main.go should call initDatabase
        Path mainGo = projectDir.resolve("main.go");
        String mainGoContent = Files.readString(mainGo);
        assertTrue(mainGoContent.contains("initDatabase()"));
    }

    @Test
    void testGenerateAppEngineWithCustomSqlDialect(@TempDir Path tempDir) throws IOException {
        List<String> logMessages = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.APP_ENGINE,
                "postgres-gae-app",
                tempDir,
                false,
                ProjectSpec.BuildSystem.INTELLIJ,
                ProjectSpec.Language.GO,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.example",
                "postgres-gae-app",
                "postgres-gae-app",
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
                "5.1.1", "2.0.9", "3.9.0", false, false, false, "", "postgres-gae-app",
                ProjectSpec.PythonInterpreterType.PROJECT_VENV, "/usr/bin/python3", "3.12", "",
                "", true, "Virtualenv", "", false, false,
                false,
                "",
                false,
                "/usr/local/go", true, "",
                "", "", "", true, true, "",
                "minitest", true, true, false, false,
                "8.0.1", "Ruby on Rails", "SQLite3", true, "Importmap", "",
                "/usr/local/go",
                true,
                true,
                "/usr/bin/python3",
                true,
                "PostgreSQL",
                "postgres-gae-app",
                tempDir.resolve("postgres-gae-app").toString(),
                tempDir.resolve("postgres-gae-app").toString(),
                AppEngineMetadata.DEFAULT_PROJECT_FORMAT
        );

        assertEquals("PostgreSQL", spec.safeAppEngineSqlDialect());
        assertTrue(spec.safeAppEngineSqlSupport());

        Path projectDir = ProjectGenerator.generate(spec, logMessages::add);
        assertNotNull(projectDir);
        assertTrue(Files.exists(projectDir));

        // .idea/sqldialects.xml must exist and map PROJECT to PostgreSQL
        Path sqlDialectsXml = projectDir.resolve(".idea/sqldialects.xml");
        assertTrue(Files.exists(sqlDialectsXml));
        String xmlContent = Files.readString(sqlDialectsXml);
        assertTrue(xmlContent.contains("dialect=\"PostgreSQL\""));
        assertTrue(xmlContent.contains("file url=\"PROJECT\""));

        // db.go should mention PostgreSQL
        Path dbGo = projectDir.resolve("db.go");
        assertTrue(Files.exists(dbGo));
        String dbGoContent = Files.readString(dbGo);
        assertTrue(dbGoContent.contains("PostgreSQL"));

        // app.yaml should mention PostgreSQL
        Path appYaml = projectDir.resolve("app.yaml");
        assertTrue(Files.exists(appYaml));
        String appYamlContent = Files.readString(appYaml);
        assertTrue(appYamlContent.contains("PostgreSQL"));
    }
}
