package dev.lumina.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PythonGeneratorTest {

    @Test
    void testProjectVenvGeneration(@TempDir Path tempDir) throws IOException {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.PYTHON,
                "my-python-app",
                tempDir,
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.PYTHON,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "",
                "my-python-app",
                "",
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
                false,
                "5.1.1",
                "2.0.9",
                "3.9.0",
                false,
                true,
                false,
                "",
                "my-python-app",
                ProjectSpec.PythonInterpreterType.PROJECT_VENV,
                "/usr/bin/python3",
                "3.12",
                ""
        );

        Path projectDir = ProjectGenerator.generate(spec, msg -> {});
        assertEquals(tempDir.resolve("my-python-app"), projectDir);

        // 1. main.py
        Path mainPy = projectDir.resolve("main.py");
        assertTrue(Files.isRegularFile(mainPy), "main.py must be created");
        String mainContent = Files.readString(mainPy);
        assertTrue(mainContent.contains("def print_hi(name):"));
        assertTrue(mainContent.contains("print_hi('PyCharm')"));

        // 2. .gitignore
        Path gitignore = projectDir.resolve(".gitignore");
        assertTrue(Files.isRegularFile(gitignore));
        String gitignoreContent = Files.readString(gitignore);
        assertTrue(gitignoreContent.contains(".venv/"));
        assertTrue(gitignoreContent.contains("__pycache__/"));

        // 3. .idea/misc.xml
        Path miscXml = projectDir.resolve(".idea/misc.xml");
        assertTrue(Files.isRegularFile(miscXml));
        String miscContent = Files.readString(miscXml);
        assertTrue(miscContent.contains("Python SDK"));
    }

    @Test
    void testUvProjectGeneration(@TempDir Path tempDir) throws IOException {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.PYTHON,
                "uv-python-app",
                tempDir,
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.PYTHON,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "",
                "uv-python-app",
                "",
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
                false,
                "5.1.1",
                "2.0.9",
                "3.9.0",
                false,
                true,
                false,
                "",
                "uv-python-app",
                ProjectSpec.PythonInterpreterType.UV,
                "",
                "3.12",
                "/usr/local/bin/uv"
        );

        Path projectDir = ProjectGenerator.generate(spec, msg -> {});
        assertEquals(tempDir.resolve("uv-python-app"), projectDir);

        // 1. main.py
        Path mainPy = projectDir.resolve("main.py");
        assertTrue(Files.isRegularFile(mainPy));

        // 2. pyproject.toml
        Path pyprojectToml = projectDir.resolve("pyproject.toml");
        assertTrue(Files.isRegularFile(pyprojectToml), "pyproject.toml must be created for uv");
        String toml = Files.readString(pyprojectToml);
        assertTrue(toml.contains("name = \"uv-python-app\""));
        assertTrue(toml.contains("requires-python = \">=3.12\""));

        // 3. .python-version
        Path pyVer = projectDir.resolve(".python-version");
        assertTrue(Files.isRegularFile(pyVer));
        assertEquals("3.12", Files.readString(pyVer).trim());

        // 4. .idea/misc.xml
        Path miscXml = projectDir.resolve(".idea/misc.xml");
        assertTrue(Files.isRegularFile(miscXml));
        String miscContent = Files.readString(miscXml);
        assertTrue(miscContent.contains("(uv)"));
    }

    @Test
    void testPythonIcon() {
        javafx.scene.Node icon = dev.lumina.ui.GeneratorIcons.getIcon("Python");
        assertNotNull(icon, "Python icon must not be null");
        assertTrue(icon instanceof javafx.scene.layout.StackPane, "Python icon should be a StackPane");
    }
}
