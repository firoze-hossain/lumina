package dev.lumina.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class VueMetadataTest {

    @Test
    void testCliEntries() {
        List<VueMetadata.VueCliEntry> entries = VueMetadata.getCliEntries();
        assertNotNull(entries);
        assertEquals(3, entries.size());

        VueMetadata.VueCliEntry createVue = entries.get(0);
        assertEquals(VueMetadata.RUNNER_CREATE_VUE, createVue.runnerPrefix());
        assertFalse(createVue.isAction());
        assertNotNull(createVue.version());
        assertTrue(createVue.formatDisplay().contains("npx create-vue"));

        VueMetadata.VueCliEntry vueCli = entries.get(1);
        assertEquals(VueMetadata.RUNNER_VUE_CLI, vueCli.runnerPrefix());
        assertFalse(vueCli.isAction());
        assertNotNull(vueCli.version());
        assertTrue(vueCli.formatDisplay().contains("npx --package @vue/cli vue"));

        VueMetadata.VueCliEntry select = entries.get(2);
        assertTrue(select.isAction());
        assertEquals(VueMetadata.ACTION_SELECT, select.runnerPrefix());
    }

    @Test
    void testParseVersionAndRunner() {
        String display = VueMetadata.formatCliDisplay(VueMetadata.RUNNER_CREATE_VUE, "3.24.0");
        assertTrue(display.contains("npx create-vue"));
        assertTrue(display.contains("3.24.0"));

        String parsedVer = VueMetadata.parseVersionFromDisplay(display);
        assertEquals("3.24.0", parsedVer);

        String parsedRunner = VueMetadata.parseRunnerFromDisplay(display);
        assertEquals(VueMetadata.RUNNER_CREATE_VUE, parsedRunner);

        String display2 = VueMetadata.formatCliDisplay(VueMetadata.RUNNER_VUE_CLI, "5.0.9");
        assertEquals("5.0.9", VueMetadata.parseVersionFromDisplay(display2));
        assertEquals(VueMetadata.RUNNER_VUE_CLI, VueMetadata.parseRunnerFromDisplay(display2));
    }

    @Test
    void testScaffoldProject(@TempDir Path tempDir) throws IOException {
        Path projectDir = tempDir.resolve("my-vue-app");
        List<String> logs = new ArrayList<>();

        VueMetadata.scaffoldProject(
                projectDir,
                "my-vue-app",
                "/usr/bin/node",
                VueMetadata.RUNNER_CREATE_VUE,
                "3.24.0",
                true,
                logs::add
        );

        assertTrue(Files.exists(projectDir));
        assertTrue(Files.exists(projectDir.resolve("package.json")));
        assertTrue(Files.exists(projectDir.resolve("vite.config.js")));
        assertTrue(Files.exists(projectDir.resolve("index.html")));
        assertTrue(Files.exists(projectDir.resolve("src/main.js")));
        assertTrue(Files.exists(projectDir.resolve("src/App.vue")));
        assertTrue(Files.exists(projectDir.resolve("src/components/HelloWorld.vue")));
        assertTrue(Files.exists(projectDir.resolve("src/assets/main.css")));
        assertTrue(Files.exists(projectDir.resolve("src/assets/base.css")));
        assertTrue(Files.exists(projectDir.resolve("public/favicon.ico")));
        assertTrue(Files.exists(projectDir.resolve("README.md")));
        assertTrue(Files.exists(projectDir.resolve(".gitignore")));
        assertTrue(Files.exists(projectDir.resolve(".idea/modules.xml")));
        assertTrue(Files.exists(projectDir.resolve(".idea/workspace.xml")));
        assertTrue(Files.exists(projectDir.resolve(".idea/my-vue-app.iml")));

        String pkgJson = Files.readString(projectDir.resolve("package.json"));
        assertTrue(pkgJson.contains("\"name\": \"my-vue-app\""));
        assertTrue(pkgJson.contains("\"vue\": \"^3.5.13\""));
        assertTrue(pkgJson.contains("\"vite\":"));
        assertTrue(pkgJson.contains("\"@vitejs/plugin-vue\": \"^5.2.1\""));

        String appVue = Files.readString(projectDir.resolve("src/App.vue"));
        assertTrue(appVue.contains("<template>"));
        assertTrue(appVue.contains("HelloWorld"));

        String viteConfig = Files.readString(projectDir.resolve("vite.config.js"));
        assertTrue(viteConfig.contains("defineConfig"));
        assertTrue(viteConfig.contains("vue()"));

        assertFalse(logs.isEmpty());
    }
}
