package dev.lumina.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ViteGeneratorTest {

    @Test
    void testViteProjectGeneratorVanilla(@TempDir Path tempDir) throws IOException {
        List<String> logs = new ArrayList<>();
        ProjectSpec spec = ProjectSpec.forVite(
                "untitled1",
                tempDir,
                "/usr/bin/node",
                ViteMetadata.RUNNER_CREATE_VITE,
                "Vanilla",
                false
        );

        assertEquals("/usr/bin/node", spec.safeViteNodeInterpreter());
        assertEquals(ViteMetadata.RUNNER_CREATE_VITE, spec.safeViteCliPackage());
        assertEquals("Vanilla", spec.safeViteTemplate());
        assertFalse(spec.safeViteTypeScript());

        Path outDir = ProjectGenerator.generate(spec, logs::add);
        assertNotNull(outDir);
        assertTrue(Files.exists(outDir));
        assertTrue(Files.exists(outDir.resolve("package.json")));
        assertTrue(Files.exists(outDir.resolve("index.html")));
        assertTrue(Files.exists(outDir.resolve("src/main.js")));
        assertTrue(Files.exists(outDir.resolve("src/style.css")));
        assertTrue(Files.exists(outDir.resolve(".idea/workspace.xml")));
        assertTrue(Files.exists(outDir.resolve(".idea/modules.xml")));
        assertTrue(Files.exists(outDir.resolve(".idea/untitled1.iml")));

        String pkgJson = Files.readString(outDir.resolve("package.json"));
        assertTrue(pkgJson.contains("\"name\": \"untitled1\""));
        assertTrue(pkgJson.contains("\"vite\":"));
    }

    @Test
    void testViteProjectGeneratorReactTypeScript(@TempDir Path tempDir) throws IOException {
        List<String> logs = new ArrayList<>();
        ProjectSpec spec = ProjectSpec.forVite(
                "react-ts-demo",
                tempDir,
                "/usr/bin/node",
                ViteMetadata.RUNNER_CREATE_VITE,
                "React",
                true
        );

        assertTrue(spec.safeViteTypeScript());
        assertEquals("React", spec.safeViteTemplate());

        Path outDir = ProjectGenerator.generate(spec, logs::add);
        assertNotNull(outDir);
        assertTrue(Files.exists(outDir));
        assertTrue(Files.exists(outDir.resolve("package.json")));
        assertTrue(Files.exists(outDir.resolve("tsconfig.json")));
        assertTrue(Files.exists(outDir.resolve("vite.config.ts")));
        assertTrue(Files.exists(outDir.resolve("src/App.tsx")));
        assertTrue(Files.exists(outDir.resolve("src/main.tsx")));
        assertTrue(Files.exists(outDir.resolve("index.html")));
        assertTrue(Files.exists(outDir.resolve(".idea/react-ts-demo.iml")));

        String pkgJson = Files.readString(outDir.resolve("package.json"));
        assertTrue(pkgJson.contains("\"react\":"));
        assertTrue(pkgJson.contains("\"typescript\":"));
    }

    @Test
    void testViteProjectGeneratorVue(@TempDir Path tempDir) throws IOException {
        List<String> logs = new ArrayList<>();
        ProjectSpec spec = ProjectSpec.forVite(
                "vue-demo",
                tempDir,
                "/usr/bin/node",
                ViteMetadata.RUNNER_CREATE_VITE,
                "Vue",
                false
        );

        Path outDir = ProjectGenerator.generate(spec, logs::add);
        assertNotNull(outDir);
        assertTrue(Files.exists(outDir));
        assertTrue(Files.exists(outDir.resolve("package.json")));
        assertTrue(Files.exists(outDir.resolve("vite.config.js")));
        assertTrue(Files.exists(outDir.resolve("src/App.vue")));
        assertTrue(Files.exists(outDir.resolve("src/main.js")));
        assertTrue(Files.exists(outDir.resolve(".idea/workspace.xml")));

        String pkgJson = Files.readString(outDir.resolve("package.json"));
        assertTrue(pkgJson.contains("\"vue\":"));
    }

    @Test
    void testSafeViteGettersDefaults() {
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.VITE,
                "test",
                Path.of("/tmp"),
                false,
                ProjectSpec.BuildSystem.INTELLIJ,
                ProjectSpec.Language.JAVA,
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
                null, null, null, false
        );

        assertEquals("", spec.safeViteNodeInterpreter());
        assertEquals(ViteMetadata.RUNNER_CREATE_VITE, spec.safeViteCliPackage());
        assertEquals(ViteMetadata.DEFAULT_TEMPLATE, spec.safeViteTemplate());
        assertFalse(spec.safeViteTypeScript());
    }
}
