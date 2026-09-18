package dev.lumina.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SymfonyMetadataTest {

    @Test
    void testFallbackVersions() {
        List<String> skeleton = SymfonyMetadata.getSkeletonVersions();
        assertNotNull(skeleton);
        assertFalse(skeleton.isEmpty());
        assertEquals("latest", skeleton.get(0));
        assertTrue(skeleton.contains("8.2.x-dev"));
        assertTrue(skeleton.contains("8.1.x-dev"));
        assertTrue(skeleton.contains("v8.1.99"));

        List<String> demo = SymfonyMetadata.getDemoVersions();
        assertNotNull(demo);
        assertFalse(demo.isEmpty());
        assertEquals("latest", demo.get(0));
        assertTrue(demo.contains("v3.1.0"));
        assertTrue(demo.contains("v3.0.2"));
    }

    @Test
    void testGetVersionsForType() {
        List<String> web = SymfonyMetadata.getVersionsForType(SymfonyMetadata.TYPE_WEB);
        assertEquals(SymfonyMetadata.getSkeletonVersions(), web);

        List<String> console = SymfonyMetadata.getVersionsForType(SymfonyMetadata.TYPE_CONSOLE);
        assertEquals(SymfonyMetadata.getSkeletonVersions(), console);

        List<String> demo = SymfonyMetadata.getVersionsForType(SymfonyMetadata.TYPE_DEMO);
        assertEquals(SymfonyMetadata.getDemoVersions(), demo);
    }

    @Test
    void testParseVersionsFromJson() {
        String mockJson = """
                {
                  "package": {
                    "versions": {
                      "dev-main": { "version": "dev-main" },
                      "8.2.x-dev": { "version": "8.2.x-dev" },
                      "8.1.x-dev": { "version": "8.1.x-dev" },
                      "v8.1.99": { "version": "v8.1.99" },
                      "7.4.x-dev": { "version": "7.4.x-dev" }
                    }
                  }
                }
                """;

        List<String> parsed = SymfonyMetadata.parseVersionsFromJson(mockJson, false);
        assertNotNull(parsed);
        assertEquals("latest", parsed.get(0));
        assertTrue(parsed.contains("8.2.x-dev"));
        assertTrue(parsed.contains("8.1.x-dev"));
        assertTrue(parsed.contains("v8.1.99"));
        assertTrue(parsed.contains("7.4.x-dev"));
        assertFalse(parsed.contains("dev-main"));
    }

    @Test
    void testScaffoldWebProject(@TempDir Path tempDir) throws IOException {
        Path projectDir = tempDir.resolve("my-web-symfony");
        List<String> logs = new ArrayList<>();

        SymfonyMetadata.scaffoldProject(
                projectDir,
                "my-web-symfony",
                SymfonyMetadata.TYPE_WEB,
                "latest",
                false,
                logs::add
        );

        assertTrue(Files.exists(projectDir));
        assertTrue(Files.exists(projectDir.resolve("composer.json")));
        assertTrue(Files.exists(projectDir.resolve("public/index.php")));
        assertTrue(Files.exists(projectDir.resolve("bin/console")));
        assertTrue(Files.exists(projectDir.resolve("src/Kernel.php")));
        assertTrue(Files.exists(projectDir.resolve("src/Controller/HomeController.php")));
        assertTrue(Files.exists(projectDir.resolve("templates/base.html.twig")));
        assertTrue(Files.exists(projectDir.resolve("templates/home/index.html.twig")));
        assertTrue(Files.exists(projectDir.resolve("config/bundles.php")));
        assertTrue(Files.exists(projectDir.resolve("config/routes.yaml")));
        assertTrue(Files.exists(projectDir.resolve("config/services.yaml")));
        assertTrue(Files.exists(projectDir.resolve("config/packages/framework.yaml")));
        assertTrue(Files.exists(projectDir.resolve("config/packages/twig.yaml")));
        assertTrue(Files.exists(projectDir.resolve(".env")));
        assertTrue(Files.exists(projectDir.resolve(".env.test")));
        assertTrue(Files.exists(projectDir.resolve(".gitignore")));
        assertTrue(Files.exists(projectDir.resolve(".idea/modules.xml")));
        assertTrue(Files.exists(projectDir.resolve(".idea/workspace.xml")));
        assertTrue(Files.exists(projectDir.resolve(".idea/my-web-symfony.iml")));
        assertTrue(Files.exists(projectDir.resolve(".idea/php.xml")));
        assertTrue(Files.exists(projectDir.resolve(".idea/misc.xml")));

        String composer = Files.readString(projectDir.resolve("composer.json"));
        assertTrue(composer.contains("symfony/framework-bundle"));
        assertTrue(composer.contains("symfony/twig-bundle"));

        String controller = Files.readString(projectDir.resolve("src/Controller/HomeController.php"));
        assertTrue(controller.contains("class HomeController extends AbstractController"));
        assertTrue(controller.contains("#[Route('/', name: 'app_home')]"));

        assertFalse(logs.isEmpty());
    }

    @Test
    void testScaffoldConsoleProject(@TempDir Path tempDir) throws IOException {
        Path projectDir = tempDir.resolve("my-console-symfony");
        List<String> logs = new ArrayList<>();

        SymfonyMetadata.scaffoldProject(
                projectDir,
                "my-console-symfony",
                SymfonyMetadata.TYPE_CONSOLE,
                "8.1.x-dev",
                false,
                logs::add
        );

        assertTrue(Files.exists(projectDir));
        assertTrue(Files.exists(projectDir.resolve("composer.json")));
        assertTrue(Files.exists(projectDir.resolve("bin/console")));
        assertTrue(Files.exists(projectDir.resolve("src/Kernel.php")));
        assertTrue(Files.exists(projectDir.resolve("src/Command/AppCommand.php")));
        assertTrue(Files.exists(projectDir.resolve("config/services.yaml")));
        assertTrue(Files.exists(projectDir.resolve(".env")));
        assertTrue(Files.exists(projectDir.resolve(".idea/my-console-symfony.iml")));

        String composer = Files.readString(projectDir.resolve("composer.json"));
        assertTrue(composer.contains("symfony/console"));
        assertTrue(composer.contains("8.1.*@dev"));

        String command = Files.readString(projectDir.resolve("src/Command/AppCommand.php"));
        assertTrue(command.contains("class AppCommand extends Command"));
        assertTrue(command.contains("#[AsCommand("));
    }

    @Test
    void testScaffoldDemoProject(@TempDir Path tempDir) throws IOException {
        Path projectDir = tempDir.resolve("my-demo-symfony");
        List<String> logs = new ArrayList<>();

        SymfonyMetadata.scaffoldProject(
                projectDir,
                "my-demo-symfony",
                SymfonyMetadata.TYPE_DEMO,
                "v3.1.0",
                false,
                logs::add
        );

        assertTrue(Files.exists(projectDir));
        assertTrue(Files.exists(projectDir.resolve("composer.json")));
        assertTrue(Files.exists(projectDir.resolve("public/index.php")));
        assertTrue(Files.exists(projectDir.resolve("bin/console")));
        assertTrue(Files.exists(projectDir.resolve("src/Kernel.php")));
        assertTrue(Files.exists(projectDir.resolve("src/Entity/Post.php")));
        assertTrue(Files.exists(projectDir.resolve("src/Controller/BlogController.php")));
        assertTrue(Files.exists(projectDir.resolve("templates/blog/index.html.twig")));
        assertTrue(Files.exists(projectDir.resolve(".idea/my-demo-symfony.iml")));

        String composer = Files.readString(projectDir.resolve("composer.json"));
        assertTrue(composer.contains("symfony/symfony-demo"));
        assertTrue(composer.contains("^3.1.0"));

        String controller = Files.readString(projectDir.resolve("src/Controller/BlogController.php"));
        assertTrue(controller.contains("class BlogController extends AbstractController"));
    }
}
