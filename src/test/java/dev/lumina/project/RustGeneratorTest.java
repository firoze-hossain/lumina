package dev.lumina.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RustGeneratorTest {

    private ProjectSpec createRustSpec(String name, Path tempDir, String template, boolean initGit) {
        return new ProjectSpec(
                ProjectSpec.Generator.RUST,
                name,
                tempDir,
                initGit,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.RUST,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.example",
                name,
                "com.example." + name.replace("-", "_"),
                "25",
                "", "", "", "", "", "1.0-SNAPSHOT", "",
                RustMetadata.defaultToolchainPath(), // rustToolchainPath
                template,                          // rustTemplate
                "RUST_LOG=debug;CUSTOM_VAR=hello", // rustEnvironment
                "",
                "", "", "", "MAVEN", false,
                "", "", "", "",
                "", "", "", "", "", "",
                "", "", false, "", "", "", "",
                "", "", "", "", "", false,
                "", "", "", "",
                ProjectSpec.GradleDsl.KOTLIN, "Wrapper", "9.2.0", "", false,
                "5.1.1", "2.0.9", "3.9.0", false, false, false, "", name,
                ProjectSpec.PythonInterpreterType.PROJECT_VENV, "/usr/bin/python3", "3.12", "",
                "", true, "Virtualenv", "", false, false,
                false,
                "/usr/bin/ruby",
                false
        );
    }

    @Test
    void testGenerateRustBinaryProject(@TempDir Path tempDir) throws IOException {
        List<String> logMessages = new ArrayList<>();
        ProjectSpec spec = createRustSpec("demo_rust_bin", tempDir, "binary", true);

        Path projectDir = ProjectGenerator.generate(spec, logMessages::add);
        assertNotNull(projectDir);
        assertTrue(Files.exists(projectDir));

        // Verify Cargo.toml
        Path cargoToml = projectDir.resolve("Cargo.toml");
        assertTrue(Files.exists(cargoToml), "Cargo.toml should exist");
        String cargoContent = Files.readString(cargoToml);
        assertTrue(cargoContent.contains("[package]"));
        assertTrue(cargoContent.contains("name = \"demo_rust_bin\""));
        assertTrue(cargoContent.contains("edition ="));

        // Verify src/main.rs
        Path mainRs = projectDir.resolve("src").resolve("main.rs");
        assertTrue(Files.exists(mainRs), "src/main.rs should exist");
        String mainContent = Files.readString(mainRs);
        assertTrue(mainContent.contains("fn main()"));
        assertTrue(mainContent.contains("println!"));

        // Verify .gitignore
        Path gitignore = projectDir.resolve(".gitignore");
        assertTrue(Files.exists(gitignore), ".gitignore should exist");
        String gitignoreContent = Files.readString(gitignore);
        assertTrue(gitignoreContent.contains("/target"));

        // Verify IntelliJ IDEA module files
        Path modulesXml = projectDir.resolve(".idea").resolve("modules.xml");
        assertTrue(Files.exists(modulesXml), ".idea/modules.xml should exist");
        String modulesContent = Files.readString(modulesXml);
        assertTrue(modulesContent.contains("demo_rust_bin.iml"));

        Path iml = projectDir.resolve(".idea").resolve("demo_rust_bin.iml");
        assertTrue(Files.exists(iml), ".idea/demo_rust_bin.iml should exist");
        String imlContent = Files.readString(iml);
        assertTrue(imlContent.contains("EMPTY_MODULE"));
        assertTrue(imlContent.contains("sourceFolder url=\"file://$MODULE_DIR$/src\""));

        // Verify Git repository initialized
        Path gitDir = projectDir.resolve(".git");
        assertTrue(Files.isDirectory(gitDir), ".git directory should exist when initGit is true");
    }

    @Test
    void testGenerateRustLibraryProject(@TempDir Path tempDir) throws IOException {
        List<String> logMessages = new ArrayList<>();
        ProjectSpec spec = createRustSpec("demo_rust_lib", tempDir, "library", false);

        Path projectDir = ProjectGenerator.generate(spec, logMessages::add);
        assertNotNull(projectDir);
        assertTrue(Files.exists(projectDir));

        // Verify Cargo.toml
        Path cargoToml = projectDir.resolve("Cargo.toml");
        assertTrue(Files.exists(cargoToml), "Cargo.toml should exist");
        String cargoContent = Files.readString(cargoToml);
        assertTrue(cargoContent.contains("[package]"));
        assertTrue(cargoContent.contains("name = \"demo_rust_lib\""));

        // Verify src/lib.rs
        Path libRs = projectDir.resolve("src").resolve("lib.rs");
        assertTrue(Files.exists(libRs), "src/lib.rs should exist");
        String libContent = Files.readString(libRs);
        assertTrue(libContent.contains("pub fn add") || libContent.contains("it_works"));

        // Verify .idea
        Path modulesXml = projectDir.resolve(".idea").resolve("modules.xml");
        assertTrue(Files.exists(modulesXml));
        Path iml = projectDir.resolve(".idea").resolve("demo_rust_lib.iml");
        assertTrue(Files.exists(iml));

        // Git should not be initialized
        Path gitDir = projectDir.resolve(".git");
        assertFalse(Files.exists(gitDir), ".git should not exist when initGit is false");
    }
}
