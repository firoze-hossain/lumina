package dev.lumina.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GoGeneratorTest {

    @Test
    void testGenerateGoProjectWithSampleCode(@TempDir Path tempDir) throws IOException {
        List<String> logs = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.GO,
                "demo_go_app",
                tempDir,
                true, // initGit
                ProjectSpec.BuildSystem.INTELLIJ,
                ProjectSpec.Language.GO,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "", "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "",
                true, // addSampleCode
                "", "", "", "", "", "", "", "", "", "", "", "",
                false, "", "", "", "", "", "", "", "", "", false,
                "", "", "", "",
                ProjectSpec.GradleDsl.KOTLIN, "Wrapper", "9.2.0", "", false,
                "5.1.1", "2.0.9", "3.9.0", false, false, false, "", "",
                ProjectSpec.PythonInterpreterType.PROJECT_VENV, "", "", "", "",
                false, "Virtualenv", "", false, false, false,
                "", false,
                "", // goRoot
                true, // goVendoring
                "GOPROXY=https://proxy.golang.org,direct" // goEnvironment
        );

        Path projectDir = ProjectGenerator.generate(spec, logs::add);
        assertNotNull(projectDir);
        assertTrue(Files.isDirectory(projectDir));

        // main.go
        Path mainGo = projectDir.resolve("main.go");
        assertTrue(Files.exists(mainGo), "main.go should be created when addSampleCode is true");
        String mainContent = Files.readString(mainGo);
        assertTrue(mainContent.contains("package main"));
        assertTrue(mainContent.contains("fmt.Println(\"Hello, World!\")"));

        // go.mod
        Path goMod = projectDir.resolve("go.mod");
        assertTrue(Files.exists(goMod), "go.mod should be created");
        String modContent = Files.readString(goMod);
        assertTrue(modContent.contains("module demo_go_app"));
        assertTrue(modContent.contains("go 1."));

        // vendor
        Path vendor = projectDir.resolve("vendor");
        assertTrue(Files.isDirectory(vendor), "vendor directory should be created when goVendoring is true");

        // .gitignore
        Path gitignore = projectDir.resolve(".gitignore");
        assertTrue(Files.exists(gitignore));

        // .idea metadata
        Path ideaDir = projectDir.resolve(".idea");
        assertTrue(Files.isDirectory(ideaDir));
        assertTrue(Files.exists(ideaDir.resolve("modules.xml")));
        assertTrue(Files.exists(ideaDir.resolve("demo_go_app.iml")));
        assertTrue(Files.exists(ideaDir.resolve("misc.xml")));

        // git
        assertTrue(Files.isDirectory(projectDir.resolve(".git")), "git repo should be initialized");
    }

    @Test
    void testGenerateGoProjectWithoutSampleCode(@TempDir Path tempDir) throws IOException {
        List<String> logs = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.GO,
                "clean_go_app",
                tempDir,
                false, // initGit
                ProjectSpec.BuildSystem.INTELLIJ,
                ProjectSpec.Language.GO,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "", "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "",
                false, // addSampleCode = false
                "", "", "", "", "", "", "", "", "", "", "", "",
                false, "", "", "", "", "", "", "", "", "", false,
                "", "", "", "",
                ProjectSpec.GradleDsl.KOTLIN, "Wrapper", "9.2.0", "", false,
                "5.1.1", "2.0.9", "3.9.0", false, false, false, "", "",
                ProjectSpec.PythonInterpreterType.PROJECT_VENV, "", "", "", "",
                false, "Virtualenv", "", false, false, false,
                "", false,
                "", // goRoot
                false, // goVendoring
                "" // goEnvironment
        );

        Path projectDir = ProjectGenerator.generate(spec, logs::add);
        assertNotNull(projectDir);

        // main.go should NOT exist
        Path mainGo = projectDir.resolve("main.go");
        assertFalse(Files.exists(mainGo), "main.go should not exist when addSampleCode is false");

        // go.mod should exist
        Path goMod = projectDir.resolve("go.mod");
        assertTrue(Files.exists(goMod));
        assertTrue(Files.readString(goMod).contains("module clean_go_app"));

        // git repo should not be initialized
        assertFalse(Files.exists(projectDir.resolve(".git")));
    }
}
