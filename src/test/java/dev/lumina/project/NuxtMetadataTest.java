package dev.lumina.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NuxtMetadataTest {

    @Test
    void testCliEntries() {
        List<NuxtMetadata.NuxtCliEntry> entries = NuxtMetadata.getCliEntries();
        assertNotNull(entries);
        assertEquals(2, entries.size());

        NuxtMetadata.NuxtCliEntry nuxi = entries.get(0);
        assertEquals(NuxtMetadata.RUNNER_NUXI_LATEST, nuxi.runnerPrefix());
        assertFalse(nuxi.isAction());
        assertNotNull(nuxi.version());
        assertTrue(nuxi.formatDisplay().contains("npx nuxi@latest"));

        NuxtMetadata.NuxtCliEntry select = entries.get(1);
        assertTrue(select.isAction());
        assertEquals(NuxtMetadata.ACTION_SELECT, select.runnerPrefix());
    }

    @Test
    void testParseVersionAndRunner() {
        String display = NuxtMetadata.formatCliDisplay(NuxtMetadata.RUNNER_NUXI_LATEST, "3.37.0");
        assertTrue(display.contains("npx nuxi@latest"));
        assertTrue(display.contains("3.37.0"));

        String parsedVer = NuxtMetadata.parseVersionFromDisplay(display);
        assertEquals("3.37.0", parsedVer);

        String parsedRunner = NuxtMetadata.parseRunnerFromDisplay(display);
        assertEquals(NuxtMetadata.RUNNER_NUXI_LATEST, parsedRunner);
    }

    @Test
    void testScaffoldNuxtProject(@TempDir Path tempDir) throws IOException {
        Path projectDir = tempDir.resolve("my-nuxt-app");
        List<String> logs = new ArrayList<>();

        ProjectSpec spec = ProjectSpec.forNuxt(
                "my-nuxt-app",
                tempDir,
                "/usr/bin/node",
                NuxtMetadata.RUNNER_NUXI_LATEST
        );

        NuxtMetadata.scaffoldProject(spec, projectDir, logs::add);

        assertTrue(Files.exists(projectDir));
        assertTrue(Files.exists(projectDir.resolve("package.json")));
        assertTrue(Files.exists(projectDir.resolve("nuxt.config.ts")));
        assertTrue(Files.exists(projectDir.resolve("app.vue")));
        assertTrue(Files.exists(projectDir.resolve("tsconfig.json")));
        assertTrue(Files.exists(projectDir.resolve("server/api/hello.ts")));
        assertTrue(Files.exists(projectDir.resolve(".gitignore")));
        assertTrue(Files.exists(projectDir.resolve("README.md")));
        assertTrue(Files.exists(projectDir.resolve(".idea/modules.xml")));
        assertTrue(Files.exists(projectDir.resolve(".idea/workspace.xml")));
        assertTrue(Files.exists(projectDir.resolve(".idea/my-nuxt-app.iml")));
        assertTrue(Files.exists(projectDir.resolve(".idea/misc.xml")));

        String pkgJson = Files.readString(projectDir.resolve("package.json"));
        assertTrue(pkgJson.contains("\"name\": \"my-nuxt-app\""));
        assertTrue(pkgJson.contains("\"nuxt\":"));
        assertTrue(pkgJson.contains("\"type\": \"module\""));

        String nuxtConfig = Files.readString(projectDir.resolve("nuxt.config.ts"));
        assertTrue(nuxtConfig.contains("defineNuxtConfig"));

        String appVue = Files.readString(projectDir.resolve("app.vue"));
        assertTrue(appVue.contains("<NuxtWelcome />"));

        assertFalse(logs.isEmpty());
    }
}
