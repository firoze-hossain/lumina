package dev.lumina.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NuxtGeneratorTest {

    @Test
    void testNuxtProjectGeneration(@TempDir Path tempDir) throws IOException {
        List<String> logs = new ArrayList<>();
        ProjectSpec spec = ProjectSpec.forNuxt(
                "untitled1",
                tempDir,
                "/usr/bin/node",
                NuxtMetadata.RUNNER_NUXI_LATEST
        );

        assertEquals("/usr/bin/node", spec.safeNuxtNodeInterpreter());
        assertEquals(NuxtMetadata.RUNNER_NUXI_LATEST, spec.safeNuxtCliPackage());
        assertEquals(ProjectSpec.Generator.NUXT, spec.generator());

        Path outDir = ProjectGenerator.generate(spec, logs::add);
        assertNotNull(outDir);
        assertTrue(Files.exists(outDir));
        assertTrue(Files.exists(outDir.resolve("package.json")));
        assertTrue(Files.exists(outDir.resolve("nuxt.config.ts")));
        assertTrue(Files.exists(outDir.resolve("app.vue")));
        assertTrue(Files.exists(outDir.resolve("tsconfig.json")));
        assertTrue(Files.exists(outDir.resolve("server/api/hello.ts")));
        assertTrue(Files.exists(outDir.resolve(".gitignore")));
        assertTrue(Files.exists(outDir.resolve("README.md")));
        assertTrue(Files.exists(outDir.resolve(".idea/workspace.xml")));
        assertTrue(Files.exists(outDir.resolve(".idea/modules.xml")));
        assertTrue(Files.exists(outDir.resolve(".idea/untitled1.iml")));
        assertTrue(Files.exists(outDir.resolve(".idea/misc.xml")));

        String pkgJson = Files.readString(outDir.resolve("package.json"));
        assertTrue(pkgJson.contains("\"name\": \"untitled1\""));
        assertTrue(pkgJson.contains("\"nuxt\":"));
    }

    @Test
    void testNuxtProjectSpecFactory() {
        Path dummyPath = Path.of("/tmp/test-nuxt");
        ProjectSpec spec = ProjectSpec.forNuxt("my-nuxt", dummyPath, "/opt/node/bin/node", "custom-nuxi");

        assertEquals(ProjectSpec.Generator.NUXT, spec.generator());
        assertEquals("my-nuxt", spec.name());
        assertEquals(dummyPath, spec.location());
        assertEquals("/opt/node/bin/node", spec.safeNuxtNodeInterpreter());
        assertEquals("custom-nuxi", spec.safeNuxtCliPackage());
        assertFalse(spec.initGit());
    }
}
