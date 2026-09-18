package dev.lumina.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SymfonyGeneratorTest {

    @Test
    void testSymfonyWebProjectGeneration(@TempDir Path tempDir) throws IOException {
        List<String> logs = new ArrayList<>();
        ProjectSpec spec = ProjectSpec.forSymfony(
                "untitled1",
                tempDir,
                false,
                SymfonyMetadata.TYPE_WEB,
                "latest"
        );

        assertEquals("untitled1", spec.name());
        assertEquals(ProjectSpec.Generator.SYMFONY, spec.generator());
        assertEquals(SymfonyMetadata.TYPE_WEB, spec.safeSymfonyProjectType());
        assertEquals("latest", spec.safeSymfonyVersion());
        assertFalse(spec.initGit());

        Path outDir = ProjectGenerator.generate(spec, logs::add);
        assertNotNull(outDir);
        assertTrue(Files.exists(outDir));
        assertTrue(Files.exists(outDir.resolve("composer.json")));
        assertTrue(Files.exists(outDir.resolve("public/index.php")));
        assertTrue(Files.exists(outDir.resolve("bin/console")));
        assertTrue(Files.exists(outDir.resolve("src/Kernel.php")));
        assertTrue(Files.exists(outDir.resolve("src/Controller/HomeController.php")));
        assertTrue(Files.exists(outDir.resolve("templates/base.html.twig")));
        assertTrue(Files.exists(outDir.resolve(".idea/workspace.xml")));
        assertTrue(Files.exists(outDir.resolve(".idea/untitled1.iml")));

        String composer = Files.readString(outDir.resolve("composer.json"));
        assertTrue(composer.contains("untitled1"));
        assertTrue(composer.contains("symfony/framework-bundle"));
    }

    @Test
    void testSymfonyConsoleProjectGeneration(@TempDir Path tempDir) throws IOException {
        List<String> logs = new ArrayList<>();
        ProjectSpec spec = ProjectSpec.forSymfony(
                "console-app",
                tempDir,
                false,
                SymfonyMetadata.TYPE_CONSOLE,
                "8.1.x-dev"
        );

        assertEquals(SymfonyMetadata.TYPE_CONSOLE, spec.safeSymfonyProjectType());
        assertEquals("8.1.x-dev", spec.safeSymfonyVersion());

        Path outDir = ProjectGenerator.generate(spec, logs::add);
        assertNotNull(outDir);
        assertTrue(Files.exists(outDir.resolve("bin/console")));
        assertTrue(Files.exists(outDir.resolve("src/Command/AppCommand.php")));
        assertTrue(Files.exists(outDir.resolve(".idea/console-app.iml")));
    }

    @Test
    void testSymfonyDemoProjectGeneration(@TempDir Path tempDir) throws IOException {
        List<String> logs = new ArrayList<>();
        ProjectSpec spec = ProjectSpec.forSymfony(
                "demo-app",
                tempDir,
                false,
                SymfonyMetadata.TYPE_DEMO,
                "v3.1.0"
        );

        assertEquals(SymfonyMetadata.TYPE_DEMO, spec.safeSymfonyProjectType());
        assertEquals("v3.1.0", spec.safeSymfonyVersion());

        Path outDir = ProjectGenerator.generate(spec, logs::add);
        assertNotNull(outDir);
        assertTrue(Files.exists(outDir.resolve("composer.json")));
        assertTrue(Files.exists(outDir.resolve("src/Controller/BlogController.php")));
        assertTrue(Files.exists(outDir.resolve("templates/blog/index.html.twig")));
        assertTrue(Files.exists(outDir.resolve(".idea/demo-app.iml")));
    }

    @Test
    void testSafeSymfonyGettersDefaults() {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.SYMFONY,
                "test",
                Path.of("/tmp"),
                false,
                ProjectSpec.BuildSystem.INTELLIJ,
                ProjectSpec.Language.PHP,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "org.example",
                "test",
                "org.example.test",
                "25",
                "", "", "", "", "", "1.0", "",
                "", "", "", "", "", "", "", "", false,
                "", "", "", "", "", "", "", "", "", "",
                "", "", false, "", "", "", "", "", "",
                "", "", "", false, "", "", "", "",
                ProjectSpec.GradleDsl.GROOVY, "Wrapper", "9.2.0", "", false,
                "", "", "", false, false, false,
                "", "", ProjectSpec.PythonInterpreterType.PROJECT_VENV,
                "", "", "", "", false, "", "", false,
                false, false, "", false, "", false, "", "", "",
                "", false, false, "", "", false, false, false,
                false, "", "", "", false, "", "", "", false,
                false, "", false, "", "test",
                "/tmp/test", "/tmp/test", "Directory (.idea)",
                "", "", false,
                "", "", "", false,
                null, null
        );

        assertEquals(SymfonyMetadata.TYPE_WEB, spec.safeSymfonyProjectType());
        assertEquals(SymfonyMetadata.DEFAULT_VERSION, spec.safeSymfonyVersion());
    }
}
