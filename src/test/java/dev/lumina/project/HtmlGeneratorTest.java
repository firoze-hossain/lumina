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

public class HtmlGeneratorTest {

    @Test
    void testHtmlMetadataFallbackVersions() {
        List<String> h5bpVersions = HtmlMetadata.getH5bpVersions();
        assertNotNull(h5bpVersions);
        assertFalse(h5bpVersions.isEmpty());
        assertTrue(h5bpVersions.contains("v9.0.1"), "Should contain v9.0.1");
        assertTrue(h5bpVersions.contains("v9.0.0"), "Should contain v9.0.0");
        assertTrue(h5bpVersions.contains("v8.0.0"), "Should contain v8.0.0");

        List<String> bsVersions = HtmlMetadata.getBootstrapVersions();
        assertNotNull(bsVersions);
        assertFalse(bsVersions.isEmpty());
        assertTrue(bsVersions.contains("v5.3.8"), "Should contain v5.3.8");
        assertTrue(bsVersions.contains("v5.3.3"), "Should contain v5.3.3");
        assertTrue(bsVersions.contains("v5.2.3"), "Should contain v5.2.3");
    }

    @Test
    void testHtmlMetadataFetchVersionsCached() {
        List<String> h5bp = HtmlMetadata.fetchVersions(HtmlMetadata.TYPE_H5BP, false);
        assertNotNull(h5bp);
        assertFalse(h5bp.isEmpty());

        List<String> bs = HtmlMetadata.fetchVersions(HtmlMetadata.TYPE_BOOTSTRAP, false);
        assertNotNull(bs);
        assertFalse(bs.isEmpty());
    }

    @Test
    void testGenerateH5bpProject(@TempDir Path tempDir) throws IOException {
        Path projectDir = tempDir.resolve("my-h5bp-app");
        List<String> log = new ArrayList<>();

        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.HTML,
                "my-h5bp-app",
                tempDir,
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.YAML,
                "",
                "my-h5bp-app",
                "",
                "21",
                "",
                "",
                "",
                "",
                "",
                "1.0.0",
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
                "v9.0.1"
        );

        ProjectGenerator.generate(spec, log::add);

        assertTrue(Files.exists(projectDir.resolve("index.html")), "index.html should exist");
        assertTrue(Files.exists(projectDir.resolve("css/style.css")), "css/style.css should exist");
        assertTrue(Files.exists(projectDir.resolve("js/app.js")), "js/app.js should exist");
        assertTrue(Files.exists(projectDir.resolve("robots.txt")), "robots.txt should exist");
        assertTrue(Files.exists(projectDir.resolve("site.webmanifest")), "site.webmanifest should exist");
        assertTrue(Files.exists(projectDir.resolve("404.html")), "404.html should exist");
        assertTrue(Files.exists(projectDir.resolve("package.json")), "package.json should exist");
        assertTrue(Files.exists(projectDir.resolve(".gitignore")), ".gitignore should exist");
        assertTrue(Files.exists(projectDir.resolve("README.md")), "README.md should exist");

        String indexHtml = Files.readString(projectDir.resolve("index.html"));
        assertTrue(indexHtml.contains("HTML5 Boilerplate"), "index.html should reference HTML5 Boilerplate");
        assertTrue(indexHtml.contains("my-h5bp-app"), "index.html should reference project name");

        // Test run configuration detection
        List<RunConfiguration> runConfigs = RunConfiguration.detect(projectDir);
        assertFalse(runConfigs.isEmpty(), "Should detect run configuration for HTML project");
        RunConfiguration htmlConfig = runConfigs.stream()
                .filter(rc -> rc.label().contains("Open index.html"))
                .findFirst()
                .orElse(null);
        assertNotNull(htmlConfig, "Open index.html configuration should be detected");
        assertEquals(projectDir, htmlConfig.workDir());
        assertNotNull(htmlConfig.commands());
        assertFalse(htmlConfig.commands().isEmpty());
    }

    @Test
    void testGenerateBootstrapProject(@TempDir Path tempDir) throws IOException {
        Path projectDir = tempDir.resolve("my-bs-app");
        List<String> log = new ArrayList<>();

        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.HTML,
                "my-bs-app",
                tempDir,
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.YAML,
                "",
                "my-bs-app",
                "",
                "21",
                "",
                "",
                "",
                "",
                "",
                "1.0.0",
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
                HtmlMetadata.TYPE_BOOTSTRAP,
                "v5.3.8"
        );

        ProjectGenerator.generate(spec, log::add);

        assertTrue(Files.exists(projectDir.resolve("index.html")), "index.html should exist");
        assertTrue(Files.exists(projectDir.resolve("css/bootstrap.min.css")), "css/bootstrap.min.css should exist");
        assertTrue(Files.exists(projectDir.resolve("js/bootstrap.bundle.min.js")), "js/bootstrap.bundle.min.js should exist");
        assertTrue(Files.exists(projectDir.resolve("css/style.css")), "css/style.css should exist");
        assertTrue(Files.exists(projectDir.resolve("js/main.js")), "js/main.js should exist");
        assertTrue(Files.exists(projectDir.resolve("package.json")), "package.json should exist");
        assertTrue(Files.exists(projectDir.resolve("README.md")), "README.md should exist");

        String indexHtml = Files.readString(projectDir.resolve("index.html"));
        assertTrue(indexHtml.contains("bootstrap.min.css"), "index.html should link bootstrap.min.css");
        assertTrue(indexHtml.contains("bootstrap.bundle.min.js"), "index.html should link bootstrap.bundle.min.js");
        assertTrue(indexHtml.contains("navbar"), "index.html should contain navbar component");
        assertTrue(indexHtml.contains("container"), "index.html should contain container layout");
        assertTrue(indexHtml.contains("my-bs-app"), "index.html should reference project name");

        // Test run configuration detection
        List<RunConfiguration> runConfigs = RunConfiguration.detect(projectDir);
        assertFalse(runConfigs.isEmpty(), "Should detect run configuration for HTML project");
        RunConfiguration htmlConfig = runConfigs.stream()
                .filter(rc -> rc.label().contains("Open index.html"))
                .findFirst()
                .orElse(null);
        assertNotNull(htmlConfig, "Open index.html configuration should be detected");
    }
}
