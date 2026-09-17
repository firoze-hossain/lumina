package dev.lumina.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class KtorGeneratorTest {

    @Test
    void testFallbackCatalogPlugins() {
        List<KtorMetadata.KtorPlugin> plugins = KtorMetadata.FALLBACK_PLUGINS;
        assertNotNull(plugins);
        assertFalse(plugins.isEmpty());

        // Verify AsyncAPI plugin matching IntelliJ IDEA screenshot
        KtorMetadata.KtorPlugin asyncApi = plugins.stream()
                .filter(p -> "asyncapi".equals(p.id()))
                .findFirst()
                .orElse(null);
        assertNotNull(asyncApi, "AsyncAPI plugin must be present in fallback catalog");
        assertEquals("AsyncAPI", asyncApi.name());
        assertEquals("AsyncAPI", asyncApi.group());
        assertEquals("1.0.0", asyncApi.version());
        assertTrue(asyncApi.description().contains("AsyncAPI"));
        assertTrue(asyncApi.usageMarkdown().contains("AsyncAPI"));
        assertTrue(asyncApi.usageMarkdown().contains("AsyncApiPlugin"));

        // Verify other plugins visible in IntelliJ IDEA screenshots
        assertTrue(plugins.stream().anyMatch(p -> "cors".equals(p.id())), "CORS must be present");
        assertTrue(plugins.stream().anyMatch(p -> "caching-headers".equals(p.id())), "Caching Headers must be present");
        assertTrue(plugins.stream().anyMatch(p -> "compression".equals(p.id())), "Compression must be present");
        assertTrue(plugins.stream().anyMatch(p -> "conditional-headers".equals(p.id())), "Conditional Headers must be present");
        assertTrue(plugins.stream().anyMatch(p -> "default-headers".equals(p.id())), "Default Headers must be present");
        assertTrue(plugins.stream().anyMatch(p -> "forwarded-headers".equals(p.id())), "Forwarded Headers must be present");
        assertTrue(plugins.stream().anyMatch(p -> "hsts".equals(p.id())), "HSTS must be present");
        assertTrue(plugins.stream().anyMatch(p -> "routing".equals(p.id())), "Routing must be present");
        assertTrue(plugins.stream().anyMatch(p -> "call-logging".equals(p.id())), "Call Logging must be present");
        assertTrue(plugins.stream().anyMatch(p -> "content-negotiation".equals(p.id())), "Content Negotiation must be present");
        assertTrue(plugins.stream().anyMatch(p -> "kotlinx-serialization".equals(p.id())), "Kotlinx Serialization must be present");
    }

    @Test
    void testPluginJsonParser() {
        String json = """
                [
                  {
                    "id": "my-plugin",
                    "name": "My Plugin",
                    "description": "Provides custom features",
                    "category": "Administration",
                    "group": "JetBrains",
                    "version": "3.5.2",
                    "url": "https://ktor.io",
                    "usage": "install(MyPlugin)"
                  }
                ]
                """;
        List<KtorMetadata.KtorPlugin> parsed = KtorMetadata.parsePlugins(json);
        assertNotNull(parsed);
        assertEquals(1, parsed.size());
        KtorMetadata.KtorPlugin p = parsed.get(0);
        assertEquals("my-plugin", p.id());
        assertEquals("My Plugin", p.name());
        assertEquals("Provides custom features", p.description());
        assertEquals("Administration", p.category());
        assertEquals("JetBrains", p.group());
        assertEquals("3.5.2", p.version());
        assertEquals("https://ktor.io", p.githubUrl());
        assertEquals("install(MyPlugin)", p.usageMarkdown());

        // Test invalid/empty returns empty list
        List<KtorMetadata.KtorPlugin> emptyParsed = KtorMetadata.parsePlugins("{}");
        assertNotNull(emptyParsed);
        assertTrue(emptyParsed.isEmpty());
    }

    @Test
    void testLocalKtorGenerationGradleKotlin(@TempDir Path tempDir) throws IOException {
        List<String> logMessages = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.KTOR,
                "ktor-sample",
                tempDir,
                true,
                ProjectSpec.BuildSystem.GRADLE,
                ProjectSpec.Language.KOTLIN,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.YAML,
                "com.example",
                "ktor-sample",
                "com.example.ktorsample",
                "21",
                "",
                "",
                "",
                "",
                "",
                "0.0.1",
                "",
                "",
                "",
                "",
                "",
                QuarkusMetadata.DEFAULT_SERVER_URL,
                "",
                "",
                "GRADLE_KOTLIN_DSL",
                true,
                JakartaMetadata.EE_11,
                JakartaMetadata.TEMPLATE_REST,
                "",
                "<No application server>",
                MicronautMetadata.DEFAULT_SERVER_URL,
                MicronautMetadata.DEFAULT_VERSION,
                "JUNIT",
                "default",
                "",
                "gradle_kotlin",
                "http://invalid-ktor-url-to-trigger-fallback:12345",
                "Netty",
                true,
                "Kotlin",
                "3.5.2",
                "YAML File",
                "routing,cors,call-logging"
        );

        Path projectDir = ProjectGenerator.generate(spec, logMessages::add);
        assertTrue(Files.isDirectory(projectDir), "Project directory must be created");

        // Verify Gradle Kotlin DSL files
        Path buildKts = projectDir.resolve("build.gradle.kts");
        assertTrue(Files.exists(buildKts), "build.gradle.kts must exist");
        String buildContent = Files.readString(buildKts);
        assertTrue(buildContent.contains("io.ktor.plugin"), "Must contain Ktor Gradle plugin");
        assertTrue(buildContent.contains("ktor-server-core"), "Must depend on ktor-server-core");
        assertTrue(buildContent.contains("ktor-server-netty"), "Must depend on ktor-server-netty");
        assertTrue(buildContent.contains("ktor-server-cors"), "Must depend on ktor-server-cors");
        assertTrue(buildContent.contains("ktor-server-call-logging"), "Must depend on ktor-server-call-logging");

        Path settingsKts = projectDir.resolve("settings.gradle.kts");
        assertTrue(Files.exists(settingsKts), "settings.gradle.kts must exist");
        assertTrue(Files.readString(settingsKts).contains("rootProject.name = \"ktor-sample\""));

        Path gradleProps = projectDir.resolve("gradle.properties");
        assertTrue(Files.exists(gradleProps), "gradle.properties must exist");
        assertTrue(Files.readString(gradleProps).contains("ktor.version="));

        // Verify config
        Path configYaml = projectDir.resolve("src/main/resources/application.yaml");
        assertTrue(Files.exists(configYaml), "application.yaml must exist");
        String yamlContent = Files.readString(configYaml);
        assertTrue(yamlContent.contains("ktor:"), "Must configure ktor in YAML");
        assertTrue(yamlContent.contains("com.example.ktorsample.ApplicationKt.module"));

        // Verify Kotlin source files
        Path appKt = projectDir.resolve("src/main/kotlin/com/example/ktorsample/Application.kt");
        assertTrue(Files.exists(appKt), "Application.kt must exist");
        String appContent = Files.readString(appKt);
        assertTrue(appContent.contains("fun main(args: Array<String>)"));
        assertTrue(appContent.contains("fun Application.module()"));
        assertTrue(appContent.contains("configureRouting()"));
        assertTrue(appContent.contains("configureHTTP()"));
        assertTrue(appContent.contains("configureMonitoring()"));

        Path routingKt = projectDir.resolve("src/main/kotlin/com/example/ktorsample/plugins/Routing.kt");
        assertTrue(Files.exists(routingKt), "Routing.kt must exist");
        String routingContent = Files.readString(routingKt);
        assertTrue(routingContent.contains("fun Application.configureRouting()"));
        assertTrue(routingContent.contains("Hello World!"));

        Path testKt = projectDir.resolve("src/test/kotlin/com/example/ktorsample/ApplicationTest.kt");
        assertTrue(Files.exists(testKt), "ApplicationTest.kt must exist");
    }

    @Test
    void testLocalKtorGenerationMavenHocon(@TempDir Path tempDir) throws IOException {
        List<String> logMessages = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.KTOR,
                "my-ktor-app",
                tempDir,
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.KOTLIN,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "dev.test",
                "my-ktor-app",
                "dev.test.app",
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
                QuarkusMetadata.DEFAULT_SERVER_URL,
                "",
                "",
                "MAVEN",
                true,
                JakartaMetadata.EE_11,
                JakartaMetadata.TEMPLATE_REST,
                "",
                "<No application server>",
                MicronautMetadata.DEFAULT_SERVER_URL,
                MicronautMetadata.DEFAULT_VERSION,
                "JUNIT",
                "default",
                "",
                "maven",
                "http://invalid-ktor-url-to-trigger-fallback:12345",
                "CIO",
                true,
                "Maven",
                "3.5.2",
                "HOCON file",
                "content-negotiation,kotlinx-serialization"
        );

        Path projectDir = ProjectGenerator.generate(spec, logMessages::add);
        assertTrue(Files.isDirectory(projectDir));

        // Verify Maven pom.xml
        Path pomXml = projectDir.resolve("pom.xml");
        assertTrue(Files.exists(pomXml), "pom.xml must exist");
        String pomContent = Files.readString(pomXml);
        assertTrue(pomContent.contains("<groupId>dev.test</groupId>"));
        assertTrue(pomContent.contains("<artifactId>my-ktor-app</artifactId>"));
        assertTrue(pomContent.contains("ktor-server-cio"));
        assertTrue(pomContent.contains("ktor-server-content-negotiation"));
        assertTrue(pomContent.contains("ktor-serialization-kotlinx-json"));

        // Verify HOCON configuration
        Path confFile = projectDir.resolve("src/main/resources/application.conf");
        assertTrue(Files.exists(confFile), "application.conf must exist");
        String confContent = Files.readString(confFile);
        assertTrue(confContent.contains("ktor {"));
        assertTrue(confContent.contains("dev.test.app.ApplicationKt.module"));

        // Verify Application.kt
        Path appKt = projectDir.resolve("src/main/kotlin/dev/test/app/Application.kt");
        assertTrue(Files.exists(appKt));
        String appContent = Files.readString(appKt);
        assertTrue(appContent.contains("configureSerialization()"));
    }

    @Test
    void testLocalKtorGenerationGradleGroovyCodeConfig(@TempDir Path tempDir) throws IOException {
        List<String> logMessages = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.KTOR,
                "code-ktor",
                tempDir,
                false,
                ProjectSpec.BuildSystem.GRADLE,
                ProjectSpec.Language.KOTLIN,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.YAML,
                "org.sample",
                "code-ktor",
                "org.sample.codektor",
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
                QuarkusMetadata.DEFAULT_SERVER_URL,
                "",
                "",
                "GRADLE",
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
                "http://invalid-ktor-url-to-trigger-fallback:12345",
                "Tomcat",
                false,
                "Gradle",
                "3.5.2",
                "Code in application.kt",
                "asyncapi"
        );

        Path projectDir = ProjectGenerator.generate(spec, logMessages::add);
        assertTrue(Files.isDirectory(projectDir));

        // Verify build.gradle (Groovy DSL)
        Path buildGradle = projectDir.resolve("build.gradle");
        assertTrue(Files.exists(buildGradle), "build.gradle must exist for Gradle Groovy");
        String buildContent = Files.readString(buildGradle);
        assertTrue(buildContent.contains("ktor-server-tomcat"));

        // Verify code in application.kt
        Path appKt = projectDir.resolve("src/main/kotlin/org/sample/codektor/Application.kt");
        assertTrue(Files.exists(appKt));
        String appContent = Files.readString(appKt);
        assertTrue(appContent.contains("embeddedServer(Tomcat, port = 8080"));
    }
}
