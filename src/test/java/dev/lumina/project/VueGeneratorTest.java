package dev.lumina.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class VueGeneratorTest {

    @Test
    void testVueProjectGenerator(@TempDir Path tempDir) throws IOException {
        List<String> logs = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.VUE,
                "untitled1",
                tempDir,
                false,
                ProjectSpec.BuildSystem.INTELLIJ,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.example",
                "untitled1",
                "com.example.untitled1",
                "25",
                "",
                "",
                "",
                "",
                "",
                "1.0-SNAPSHOT",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                false,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                false,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                false,
                "",
                "",
                "",
                "",
                ProjectSpec.GradleDsl.GROOVY,
                "Wrapper",
                "9.2.0",
                "",
                false,
                "5.1.1",
                "2.0.9",
                "3.9.0",
                false,
                false,
                false,
                "",
                "untitled1",
                ProjectSpec.PythonInterpreterType.PROJECT_VENV,
                "",
                "",
                "",
                "",
                false,
                "Virtualenv",
                "",
                false,
                false,
                false,
                "",
                false,
                "",
                false,
                "",
                "",
                "",
                "",
                false,
                false,
                "",
                "",
                false,
                false,
                false,
                false,
                "",
                "",
                "",
                false,
                "",
                "",
                "",
                false,
                false,
                "",
                false,
                "",
                "untitled1",
                tempDir.resolve("untitled1").toString(),
                tempDir.resolve("untitled1").toString(),
                "Directory (.idea)",
                "/usr/bin/node",
                VueMetadata.RUNNER_CREATE_VUE,
                true
        );

        assertEquals("/usr/bin/node", spec.safeVueNodeInterpreter());
        assertEquals(VueMetadata.RUNNER_CREATE_VUE, spec.safeVueCliPackage());
        assertTrue(spec.safeVueDefaultSetup());

        Path outDir = ProjectGenerator.generate(spec, logs::add);
        assertNotNull(outDir);
        assertTrue(Files.exists(outDir));
        assertTrue(Files.exists(outDir.resolve("package.json")));
        assertTrue(Files.exists(outDir.resolve("vite.config.js")));
        assertTrue(Files.exists(outDir.resolve("src/App.vue")));
        assertTrue(Files.exists(outDir.resolve("src/main.js")));
        assertTrue(Files.exists(outDir.resolve("index.html")));
        assertTrue(Files.exists(outDir.resolve(".idea/workspace.xml")));
        assertTrue(Files.exists(outDir.resolve(".idea/untitled1.iml")));

        String content = Files.readString(outDir.resolve("package.json"));
        assertTrue(content.contains("\"name\": \"untitled1\""));
    }
}
