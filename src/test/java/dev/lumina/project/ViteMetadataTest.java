package dev.lumina.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ViteMetadataTest {

    @Test
    void testCliEntries() {
        List<ViteMetadata.ViteCliEntry> entries = ViteMetadata.getCliEntries();
        assertNotNull(entries);
        assertEquals(2, entries.size());

        ViteMetadata.ViteCliEntry createVite = entries.get(0);
        assertEquals(ViteMetadata.RUNNER_CREATE_VITE, createVite.runnerPrefix());
        assertFalse(createVite.isAction());
        assertNotNull(createVite.version());
        assertTrue(createVite.formatDisplay().contains("npx create-vite"));

        ViteMetadata.ViteCliEntry select = entries.get(1);
        assertTrue(select.isAction());
        assertEquals(ViteMetadata.ACTION_SELECT, select.runnerPrefix());
    }

    @Test
    void testParseVersionAndRunner() {
        String display = ViteMetadata.formatCliDisplay(ViteMetadata.RUNNER_CREATE_VITE, "9.2.1");
        assertTrue(display.contains("npx create-vite"));
        assertTrue(display.contains("9.2.1"));

        String parsedVer = ViteMetadata.parseVersionFromDisplay(display);
        assertEquals("9.2.1", parsedVer);

        String parsedRunner = ViteMetadata.parseRunnerFromDisplay(display);
        assertEquals(ViteMetadata.RUNNER_CREATE_VITE, parsedRunner);
    }

    @Test
    void testTemplates() {
        List<String> templates = ViteMetadata.getTemplates();
        assertNotNull(templates);
        assertEquals(8, templates.size());
        assertEquals("Vanilla", templates.get(0));
        assertTrue(templates.contains("React"));
        assertTrue(templates.contains("Vue"));
        assertTrue(templates.contains("Preact"));
        assertTrue(templates.contains("Lit"));
        assertTrue(templates.contains("Svelte"));
        assertTrue(templates.contains("Solid"));
        assertTrue(templates.contains("Qwik"));
    }

    @Test
    void testScaffoldProjectVanillaJs(@TempDir Path tempDir) throws IOException {
        Path projectDir = tempDir.resolve("my-vanilla-app");
        List<String> logs = new ArrayList<>();

        ViteMetadata.scaffoldProject(
                projectDir,
                "my-vanilla-app",
                "/usr/bin/node",
                ViteMetadata.RUNNER_CREATE_VITE,
                "9.2.1",
                "Vanilla",
                false,
                logs::add
        );

        assertTrue(Files.exists(projectDir));
        assertTrue(Files.exists(projectDir.resolve("package.json")));
        assertTrue(Files.exists(projectDir.resolve("index.html")));
        assertTrue(Files.exists(projectDir.resolve("src/main.js")));
        assertTrue(Files.exists(projectDir.resolve("src/style.css")));
        assertTrue(Files.exists(projectDir.resolve("src/counter.js")));
        assertTrue(Files.exists(projectDir.resolve("public/vite.svg")));
        assertTrue(Files.exists(projectDir.resolve(".gitignore")));
        assertTrue(Files.exists(projectDir.resolve("README.md")));
        assertTrue(Files.exists(projectDir.resolve(".idea/modules.xml")));
        assertTrue(Files.exists(projectDir.resolve(".idea/workspace.xml")));
        assertTrue(Files.exists(projectDir.resolve(".idea/my-vanilla-app.iml")));
        assertTrue(Files.exists(projectDir.resolve(".idea/misc.xml")));

        String pkgJson = Files.readString(projectDir.resolve("package.json"));
        assertTrue(pkgJson.contains("\"name\": \"my-vanilla-app\""));
        assertTrue(pkgJson.contains("\"vite\":"));
        assertTrue(pkgJson.contains("\"type\": \"module\""));

        String html = Files.readString(projectDir.resolve("index.html"));
        assertTrue(html.contains("src=\"/src/main.js\""));
        assertTrue(html.contains("<title>my-vanilla-app</title>"));

        assertFalse(logs.isEmpty());
    }

    @Test
    void testScaffoldProjectVanillaTs(@TempDir Path tempDir) throws IOException {
        Path projectDir = tempDir.resolve("my-vanilla-ts");
        List<String> logs = new ArrayList<>();

        ViteMetadata.scaffoldProject(
                projectDir,
                "my-vanilla-ts",
                "/usr/bin/node",
                ViteMetadata.RUNNER_CREATE_VITE,
                "9.2.1",
                "Vanilla",
                true,
                logs::add
        );

        assertTrue(Files.exists(projectDir));
        assertTrue(Files.exists(projectDir.resolve("package.json")));
        assertTrue(Files.exists(projectDir.resolve("tsconfig.json")));
        assertTrue(Files.exists(projectDir.resolve("src/main.ts")));
        assertTrue(Files.exists(projectDir.resolve("src/counter.ts")));
        assertTrue(Files.exists(projectDir.resolve("src/style.css")));
        assertTrue(Files.exists(projectDir.resolve("src/vite-env.d.ts")));
        assertTrue(Files.exists(projectDir.resolve("index.html")));

        String pkgJson = Files.readString(projectDir.resolve("package.json"));
        assertTrue(pkgJson.contains("\"typescript\":"));

        String html = Files.readString(projectDir.resolve("index.html"));
        assertTrue(html.contains("src=\"/src/main.ts\""));
    }

    @Test
    void testScaffoldProjectReactTs(@TempDir Path tempDir) throws IOException {
        Path projectDir = tempDir.resolve("my-react-ts");
        List<String> logs = new ArrayList<>();

        ViteMetadata.scaffoldProject(
                projectDir,
                "my-react-ts",
                "/usr/bin/node",
                ViteMetadata.RUNNER_CREATE_VITE,
                "9.2.1",
                "React",
                true,
                logs::add
        );

        assertTrue(Files.exists(projectDir));
        assertTrue(Files.exists(projectDir.resolve("package.json")));
        assertTrue(Files.exists(projectDir.resolve("tsconfig.json")));
        assertTrue(Files.exists(projectDir.resolve("vite.config.ts")));
        assertTrue(Files.exists(projectDir.resolve("src/App.tsx")));
        assertTrue(Files.exists(projectDir.resolve("src/main.tsx")));
        assertTrue(Files.exists(projectDir.resolve("index.html")));

        String pkgJson = Files.readString(projectDir.resolve("package.json"));
        assertTrue(pkgJson.contains("\"react\":"));
        assertTrue(pkgJson.contains("\"@vitejs/plugin-react\":"));
        assertTrue(pkgJson.contains("\"typescript\":"));

        String viteConfig = Files.readString(projectDir.resolve("vite.config.ts"));
        assertTrue(viteConfig.contains("defineConfig"));
        assertTrue(viteConfig.contains("react()"));
    }

    @Test
    void testScaffoldProjectVueJs(@TempDir Path tempDir) throws IOException {
        Path projectDir = tempDir.resolve("my-vue-vite");
        List<String> logs = new ArrayList<>();

        ViteMetadata.scaffoldProject(
                projectDir,
                "my-vue-vite",
                "/usr/bin/node",
                ViteMetadata.RUNNER_CREATE_VITE,
                "9.2.1",
                "Vue",
                false,
                logs::add
        );

        assertTrue(Files.exists(projectDir));
        assertTrue(Files.exists(projectDir.resolve("package.json")));
        assertTrue(Files.exists(projectDir.resolve("vite.config.js")));
        assertTrue(Files.exists(projectDir.resolve("src/App.vue")));
        assertTrue(Files.exists(projectDir.resolve("src/main.js")));

        String pkgJson = Files.readString(projectDir.resolve("package.json"));
        assertTrue(pkgJson.contains("\"vue\":"));
        assertTrue(pkgJson.contains("\"@vitejs/plugin-vue\":"));
    }

    @Test
    void testScaffoldProjectSvelteTs(@TempDir Path tempDir) throws IOException {
        Path projectDir = tempDir.resolve("my-svelte-ts");
        List<String> logs = new ArrayList<>();

        ViteMetadata.scaffoldProject(
                projectDir,
                "my-svelte-ts",
                "/usr/bin/node",
                ViteMetadata.RUNNER_CREATE_VITE,
                "9.2.1",
                "Svelte",
                true,
                logs::add
        );

        assertTrue(Files.exists(projectDir));
        assertTrue(Files.exists(projectDir.resolve("package.json")));
        assertTrue(Files.exists(projectDir.resolve("vite.config.ts")));
        assertTrue(Files.exists(projectDir.resolve("src/App.svelte")));
        assertTrue(Files.exists(projectDir.resolve("src/main.ts")));

        String pkgJson = Files.readString(projectDir.resolve("package.json"));
        assertTrue(pkgJson.contains("\"svelte\":"));
        assertTrue(pkgJson.contains("\"@sveltejs/vite-plugin-svelte\":"));
    }

    @Test
    void testScaffoldProjectSolidAndQwik(@TempDir Path tempDir) throws IOException {
        Path solidDir = tempDir.resolve("my-solid");
        List<String> logs = new ArrayList<>();

        ViteMetadata.scaffoldProject(
                solidDir,
                "my-solid",
                "/usr/bin/node",
                ViteMetadata.RUNNER_CREATE_VITE,
                "9.2.1",
                "Solid",
                false,
                logs::add
        );

        assertTrue(Files.exists(solidDir.resolve("package.json")));
        assertTrue(Files.exists(solidDir.resolve("src/App.jsx")));
        assertTrue(Files.readString(solidDir.resolve("package.json")).contains("solid-js"));

        Path qwikDir = tempDir.resolve("my-qwik");
        ViteMetadata.scaffoldProject(
                qwikDir,
                "my-qwik",
                "/usr/bin/node",
                ViteMetadata.RUNNER_CREATE_VITE,
                "9.2.1",
                "Qwik",
                true,
                logs::add
        );

        assertTrue(Files.exists(qwikDir.resolve("package.json")));
        assertTrue(Files.exists(qwikDir.resolve("src/root.tsx")));
        assertTrue(Files.readString(qwikDir.resolve("package.json")).contains("@builder.io/qwik"));
    }
}
