package dev.lumina.project;

import dev.lumina.run.RunConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReactGeneratorTest {

    @Test
    void testNodeMetadataDetection() {
        List<NodeMetadata.NodeInterpreter> interpreters = NodeMetadata.detectInterpreters(false);
        assertNotNull(interpreters, "Interpreters list should not be null");
        assertFalse(interpreters.isEmpty(), "Should detect at least one Node interpreter or fallback");

        NodeMetadata.NodeInterpreter first = interpreters.getFirst();
        assertNotNull(first.name());
        assertNotNull(first.path());
        assertNotNull(first.version());
        assertFalse(first.path().isBlank());
        assertFalse(first.version().isBlank());

        String display = first.formatDisplay();
        assertTrue(display.contains(first.path()), "Display should contain the path");
        assertTrue(display.contains(first.version()), "Display should contain the version");
    }

    @Test
    void testReactMetadataLabelsAndFormatting() {
        assertEquals("create-react-app:", ReactMetadata.getCliLabel(ReactMetadata.TYPE_REACT));
        assertEquals("React Native:", ReactMetadata.getCliLabel(ReactMetadata.TYPE_REACT_NATIVE));
        assertEquals("create-next-app:", ReactMetadata.getCliLabel(ReactMetadata.TYPE_NEXT_JS));

        assertEquals("create-react-app", ReactMetadata.getCliPackage(ReactMetadata.TYPE_REACT));
        assertEquals("@react-native-community/cli", ReactMetadata.getCliPackage(ReactMetadata.TYPE_REACT_NATIVE));
        assertEquals("create-next-app", ReactMetadata.getCliPackage(ReactMetadata.TYPE_NEXT_JS));

        List<String> reactVersions = ReactMetadata.getVersions(ReactMetadata.TYPE_REACT);
        assertFalse(reactVersions.isEmpty());
        assertTrue(reactVersions.contains("5.1.0"));

        String formatted = ReactMetadata.formatCliDisplay(ReactMetadata.TYPE_REACT, "5.1.0");
        assertTrue(formatted.contains("npx create-react-app"));
        assertTrue(formatted.endsWith("5.1.0"));

        String rnFormatted = ReactMetadata.formatCliDisplay(ReactMetadata.TYPE_REACT_NATIVE, "20.2.0");
        assertTrue(rnFormatted.contains("npx --package @react-native-community/cli rnc-cli"));
        assertTrue(rnFormatted.endsWith("20.2.0"));

        List<String> rnVersions = ReactMetadata.fetchAllVersions(ReactMetadata.TYPE_REACT_NATIVE, false);
        assertFalse(rnVersions.isEmpty());
        assertTrue(rnVersions.contains("20.2.0"));

        String nextFormatted = ReactMetadata.formatCliDisplay(ReactMetadata.TYPE_NEXT_JS, "16.3.5");
        assertTrue(nextFormatted.contains("npx create-next-app"));
        assertTrue(nextFormatted.endsWith("16.3.5"));

        List<String> nextVersions = ReactMetadata.fetchAllVersions(ReactMetadata.TYPE_NEXT_JS, false);
        assertFalse(nextVersions.isEmpty());
        assertTrue(nextVersions.contains("16.3.5"));
    }

    @Test
    void testParseNpmVersions() {
        String sampleJson = """
                {
                  "name": "@react-native-community/cli",
                  "versions": {
                    "19.0.0": {},
                    "20.0.0": {},
                    "20.1.1": {},
                    "20.2.0": {},
                    "21.0.0-alpha.1": {}
                  }
                }
                """;
        List<String> versions = ReactMetadata.parseNpmVersions(sampleJson);
        assertNotNull(versions);
        assertEquals(4, versions.size(), "Should parse non-alpha versions");
        assertEquals("20.2.0", versions.get(0), "Latest version should be first after reverse");
        assertEquals("20.1.1", versions.get(1));
        assertEquals("20.0.0", versions.get(2));
        assertEquals("19.0.0", versions.get(3));
    }

    @Test
    void testGenerateReactJavaScriptProject(@TempDir Path tempDir) throws IOException {
        Path projectDir = tempDir.resolve("my-react-app");
        List<String> log = new ArrayList<>();

        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.REACT,
                "my-react-app",
                tempDir,
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.YAML,
                "",
                "my-react-app",
                "",
                "21",
                "",
                "",
                "",
                "",
                "",
                "0.1.0",
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
                JakartaMetadata.EE_11,
                JakartaMetadata.TEMPLATE_REST,
                "",
                "<No application server>",
                MicronautMetadata.DEFAULT_SERVER_URL,
                MicronautMetadata.DEFAULT_VERSION,
                "JUNIT",
                "default",
                "",
                "gradle",
                KtorMetadata.DEFAULT_SERVER_URL,
                "Netty",
                true,
                "Gradle",
                "3.5.2",
                "YAML File",
                "",
                HtmlMetadata.TYPE_H5BP,
                "v9.0.1",
                ReactMetadata.TYPE_REACT,
                "/usr/local/bin/node",
                "5.1.0",
                false
        );

        ProjectGenerator.generate(spec, log::add);

        assertTrue(Files.exists(projectDir.resolve("public/index.html")), "public/index.html should exist");
        assertTrue(Files.exists(projectDir.resolve("public/manifest.json")), "public/manifest.json should exist");
        assertTrue(Files.exists(projectDir.resolve("public/robots.txt")), "public/robots.txt should exist");
        assertTrue(Files.exists(projectDir.resolve("src/App.js")), "src/App.js should exist");
        assertTrue(Files.exists(projectDir.resolve("src/index.js")), "src/index.js should exist");
        assertTrue(Files.exists(projectDir.resolve("src/App.css")), "src/App.css should exist");
        assertTrue(Files.exists(projectDir.resolve("src/index.css")), "src/index.css should exist");
        assertTrue(Files.exists(projectDir.resolve("package.json")), "package.json should exist");
        assertTrue(Files.exists(projectDir.resolve(".gitignore")), ".gitignore should exist");
        assertTrue(Files.exists(projectDir.resolve("README.md")), "README.md should exist");

        String packageJson = Files.readString(projectDir.resolve("package.json"));
        assertTrue(packageJson.contains("\"react\":"), "package.json should specify react");
        assertTrue(packageJson.contains("\"react-scripts\":"), "package.json should specify react-scripts");
        assertTrue(packageJson.contains("\"start\": \"react-scripts start\""), "package.json should contain start script");

        String appJs = Files.readString(projectDir.resolve("src/App.js"));
        assertTrue(appJs.contains("my-react-app"), "App.js should include project name");
        assertTrue(appJs.contains("useState"), "App.js should use React state hook");

        // RunConfiguration detection
        List<RunConfiguration> runConfigs = RunConfiguration.detect(projectDir);
        assertFalse(runConfigs.isEmpty(), "Should detect RunConfiguration for React project");
        assertTrue(runConfigs.stream().anyMatch(rc -> rc.label().contains("npm start")), "Should propose 'npm start' run config");
    }

    @Test
    void testGenerateReactTypeScriptProject(@TempDir Path tempDir) throws IOException {
        Path projectDir = tempDir.resolve("my-react-ts");
        List<String> log = new ArrayList<>();

        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.REACT,
                "my-react-ts",
                tempDir,
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.YAML,
                "",
                "my-react-ts",
                "",
                "21",
                "",
                "",
                "",
                "",
                "",
                "0.1.0",
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
                JakartaMetadata.EE_11,
                JakartaMetadata.TEMPLATE_REST,
                "",
                "<No application server>",
                MicronautMetadata.DEFAULT_SERVER_URL,
                MicronautMetadata.DEFAULT_VERSION,
                "JUNIT",
                "default",
                "",
                "gradle",
                KtorMetadata.DEFAULT_SERVER_URL,
                "Netty",
                true,
                "Gradle",
                "3.5.2",
                "YAML File",
                "",
                HtmlMetadata.TYPE_H5BP,
                "v9.0.1",
                ReactMetadata.TYPE_REACT,
                "/usr/local/bin/node",
                "5.1.0",
                true
        );

        ProjectGenerator.generate(spec, log::add);

        assertTrue(Files.exists(projectDir.resolve("src/App.tsx")), "src/App.tsx should exist");
        assertTrue(Files.exists(projectDir.resolve("src/index.tsx")), "src/index.tsx should exist");
        assertTrue(Files.exists(projectDir.resolve("src/react-app-env.d.ts")), "src/react-app-env.d.ts should exist");
        assertTrue(Files.exists(projectDir.resolve("tsconfig.json")), "tsconfig.json should exist");
        assertTrue(Files.exists(projectDir.resolve("package.json")), "package.json should exist");

        String packageJson = Files.readString(projectDir.resolve("package.json"));
        assertTrue(packageJson.contains("\"typescript\":"), "package.json should contain typescript");
        assertTrue(packageJson.contains("\"@types/react\":"), "package.json should contain @types/react");

        // RunConfiguration detection
        List<RunConfiguration> runConfigs = RunConfiguration.detect(projectDir);
        assertTrue(runConfigs.stream().anyMatch(rc -> rc.label().contains("npm start")), "Should propose 'npm start'");
    }

    @Test
    void testGenerateNextJsProject(@TempDir Path tempDir) throws IOException {
        Path projectDir = tempDir.resolve("my-next-app");
        List<String> log = new ArrayList<>();

        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.REACT,
                "my-next-app",
                tempDir,
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.YAML,
                "",
                "my-next-app",
                "",
                "21",
                "",
                "",
                "",
                "",
                "",
                "0.1.0",
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
                JakartaMetadata.EE_11,
                JakartaMetadata.TEMPLATE_REST,
                "",
                "<No application server>",
                MicronautMetadata.DEFAULT_SERVER_URL,
                MicronautMetadata.DEFAULT_VERSION,
                "JUNIT",
                "default",
                "",
                "gradle",
                KtorMetadata.DEFAULT_SERVER_URL,
                "Netty",
                true,
                "Gradle",
                "3.5.2",
                "YAML File",
                "",
                HtmlMetadata.TYPE_H5BP,
                "v9.0.1",
                ReactMetadata.TYPE_NEXT_JS,
                "/usr/local/bin/node",
                "16.3.5",
                true
        );

        ProjectGenerator.generate(spec, log::add);

        assertTrue(Files.exists(projectDir.resolve("next.config.mjs")), "next.config.mjs should exist");
        assertTrue(Files.exists(projectDir.resolve("app/layout.tsx")), "app/layout.tsx should exist");
        assertTrue(Files.exists(projectDir.resolve("app/page.tsx")), "app/page.tsx should exist");
        assertTrue(Files.exists(projectDir.resolve("app/globals.css")), "app/globals.css should exist");
        assertTrue(Files.exists(projectDir.resolve("tsconfig.json")), "tsconfig.json should exist");
        assertTrue(Files.exists(projectDir.resolve("next-env.d.ts")), "next-env.d.ts should exist");
        assertTrue(Files.exists(projectDir.resolve("package.json")), "package.json should exist");

        String packageJson = Files.readString(projectDir.resolve("package.json"));
        assertTrue(packageJson.contains("\"next\": \"^16.3.5\""), "package.json should contain next ^16.3.5");
        assertTrue(packageJson.contains("\"dev\": \"next dev\""), "package.json should contain dev script");

        // RunConfiguration detection
        List<RunConfiguration> runConfigs = RunConfiguration.detect(projectDir);
        assertTrue(runConfigs.stream().anyMatch(rc -> rc.label().contains("npm run dev")), "Should propose 'npm run dev'");
    }

    @Test
    void testGenerateReactNativeProject(@TempDir Path tempDir) throws IOException {
        Path projectDir = tempDir.resolve("my-rn-app");
        List<String> log = new ArrayList<>();

        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.REACT,
                "my-rn-app",
                tempDir,
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.YAML,
                "",
                "my-rn-app",
                "",
                "21",
                "",
                "",
                "",
                "",
                "",
                "0.1.0",
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
                JakartaMetadata.EE_11,
                JakartaMetadata.TEMPLATE_REST,
                "",
                "<No application server>",
                MicronautMetadata.DEFAULT_SERVER_URL,
                MicronautMetadata.DEFAULT_VERSION,
                "JUNIT",
                "default",
                "",
                "gradle",
                KtorMetadata.DEFAULT_SERVER_URL,
                "Netty",
                true,
                "Gradle",
                "3.5.2",
                "YAML File",
                "",
                HtmlMetadata.TYPE_H5BP,
                "v9.0.1",
                ReactMetadata.TYPE_REACT_NATIVE,
                "/usr/local/bin/node",
                "20.2.0",
                false
        );

        ProjectGenerator.generate(spec, log::add);

        assertTrue(Files.exists(projectDir.resolve("app.json")), "app.json should exist");
        assertTrue(Files.exists(projectDir.resolve("index.js")), "index.js should exist");
        assertTrue(Files.exists(projectDir.resolve("App.tsx")), "App.tsx should exist");
        assertTrue(Files.exists(projectDir.resolve("metro.config.js")), "metro.config.js should exist");
        assertTrue(Files.exists(projectDir.resolve("babel.config.js")), "babel.config.js should exist");
        assertTrue(Files.exists(projectDir.resolve("tsconfig.json")), "tsconfig.json should exist");
        assertTrue(Files.exists(projectDir.resolve("Gemfile")), "Gemfile should exist");
        assertTrue(Files.exists(projectDir.resolve("android")), "android directory should exist");
        assertTrue(Files.exists(projectDir.resolve("ios")), "ios directory should exist");
        assertTrue(Files.exists(projectDir.resolve("package.json")), "package.json should exist");

        String packageJson = Files.readString(projectDir.resolve("package.json"));
        assertTrue(packageJson.contains("\"react-native\":"), "package.json should contain react-native");
        assertTrue(packageJson.contains("\"@react-native-community/cli\": \"20.2.0\""), "package.json should contain cli version");

        // RunConfiguration detection
        List<RunConfiguration> runConfigs = RunConfiguration.detect(projectDir);
        assertTrue(runConfigs.stream().anyMatch(rc -> rc.label().contains("npm start")), "Should propose 'npm start'");
    }
}
