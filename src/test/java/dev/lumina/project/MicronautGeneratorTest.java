package dev.lumina.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MicronautGeneratorTest {

    @Test
    void testFallbackCatalogCategoriesAndFeatures() {
        MicronautMetadata.MicronautCatalog catalog = MicronautMetadata.FALLBACK_CATALOG;
        assertNotNull(catalog);
        assertEquals("5.1.5", catalog.version());

        List<MicronautMetadata.MicronautCategory> categories = catalog.categories();
        assertFalse(categories.isEmpty());

        // Verify key categories matching IntelliJ IDEA screenshots
        assertTrue(categories.stream().anyMatch(c -> c.name().equals("Server")));
        assertTrue(categories.stream().anyMatch(c -> c.name().equals("Client")));
        assertTrue(categories.stream().anyMatch(c -> c.name().equals("Database")));
        assertTrue(categories.stream().anyMatch(c -> c.name().equals("Validation")));
        assertTrue(categories.stream().anyMatch(c -> c.name().equals("Logging")));
        assertTrue(categories.stream().anyMatch(c -> c.name().equals("Security")));
        assertTrue(categories.stream().anyMatch(c -> c.name().equals("Management")));
        assertTrue(categories.stream().anyMatch(c -> c.name().equals("Messaging")));
        assertTrue(categories.stream().anyMatch(c -> c.name().equals("Reactive")));
        assertTrue(categories.stream().anyMatch(c -> c.name().equals("View Rendering")));
        assertTrue(categories.stream().anyMatch(c -> c.name().equals("SSL")));
        assertTrue(categories.stream().anyMatch(c -> c.name().equals("Serverless")));
        assertTrue(categories.stream().anyMatch(c -> c.name().equals("API")));
        assertTrue(categories.stream().anyMatch(c -> c.name().equals("Cloud")));
        assertTrue(categories.stream().anyMatch(c -> c.name().equals("Documentation")));
        assertTrue(categories.stream().anyMatch(c -> c.name().equals("Development Tools")));
        assertTrue(categories.stream().anyMatch(c -> c.name().equals("AI")));
        assertTrue(categories.stream().anyMatch(c -> c.name().equals("Language Models")));
        assertTrue(categories.stream().anyMatch(c -> c.name().equals("Embedded Store")));
        assertTrue(categories.stream().anyMatch(c -> c.name().equals("MCP")));
        assertTrue(categories.stream().anyMatch(c -> c.name().equals("Metrics")));
        assertTrue(categories.stream().anyMatch(c -> c.name().equals("Resilience")));
        assertTrue(categories.stream().anyMatch(c -> c.name().equals("Spring Framework")));

        // Verify Server category features matching screenshot 2
        MicronautMetadata.MicronautCategory serverCat = categories.stream()
                .filter(c -> c.name().equals("Server"))
                .findFirst()
                .orElseThrow();
        List<MicronautMetadata.MicronautFeature> serverFeatures = serverCat.features();
        assertTrue(serverFeatures.stream().anyMatch(f -> f.title().equals("Plain Old Java HTTP Application")));
        assertTrue(serverFeatures.stream().anyMatch(f -> f.title().equals("Built-In Java HTTP Server Runtime")));
        assertTrue(serverFeatures.stream().anyMatch(f -> f.title().equals("JAX-RS support")));
        assertTrue(serverFeatures.stream().anyMatch(f -> f.title().equals("Jetty Server")));
        assertTrue(serverFeatures.stream().anyMatch(f -> f.title().equals("Ktor")));
        assertTrue(serverFeatures.stream().anyMatch(f -> f.title().equals("Netty Server")));
        assertTrue(serverFeatures.stream().anyMatch(f -> f.title().equals("Tomcat Server")));
        assertTrue(serverFeatures.stream().anyMatch(f -> f.title().equals("Undertow Server")));
        assertTrue(serverFeatures.stream().anyMatch(f -> f.title().equals("Websocket")));
    }

    @Test
    void testVersionParser() {
        String json1 = "{\"micronaut.version\":\"5.1.5\",\"graalvm.version\":\"23.1.2\"}";
        assertEquals("5.1.5", MicronautMetadata.parseVersion(json1));

        String json2 = "{\"micronaut\":{\"version\":\"5.2.0\"}}";
        assertEquals("5.2.0", MicronautMetadata.parseVersion(json2));

        String json3 = "{\"version\":\"4.8.0\"}";
        assertEquals("4.8.0", MicronautMetadata.parseVersion(json3));

        String invalid = "{}";
        assertEquals("5.1.5", MicronautMetadata.parseVersion(invalid));
    }

    @Test
    void testFeaturesParser() throws IOException {
        String json = """
                {
                  "features": [
                    {
                      "name": "netty-server",
                      "title": "Netty Server",
                      "description": "Adds support for a Netty server",
                      "category": "Server",
                      "preview": false,
                      "community": false
                    },
                    {
                      "name": "validation",
                      "title": "Micronaut Validation",
                      "description": "Adds support for Micronaut Validation",
                      "category": "Validation",
                      "preview": false,
                      "community": false
                    }
                  ]
                }
                """;

        List<MicronautMetadata.MicronautCategory> cats = MicronautMetadata.parseFeatures(json);
        assertEquals(2, cats.size());
        assertEquals("Server", cats.get(0).name());
        assertEquals(1, cats.get(0).features().size());
        assertEquals("Netty Server", cats.get(0).features().get(0).title());
        assertEquals("Validation", cats.get(1).name());
        assertEquals("Micronaut Validation", cats.get(1).features().get(0).title());
    }

    @Test
    void testGenerateMicronautMavenJavaProject(@TempDir Path tempDir) throws IOException {
        List<String> logs = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.MICRONAUT,
                "demo",
                tempDir,
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "org.example",
                "demo",
                "org.example.demo",
                "21",
                "",
                "",
                "",
                "",
                "",
                "1.0-SNAPSHOT",
                "",
                "",
                "",
                "",
                "",
                "https://launch.micronaut.io",
                "",
                "",
                "MAVEN",
                false,
                JakartaMetadata.EE_11,
                JakartaMetadata.TEMPLATE_REST,
                "",
                "<No application server>",
                "https://invalid-host-for-local-fallback.test",
                "5.1.5",
                "JUNIT",
                "default",
                "validation,websocket",
                "maven"
        );

        Path projectDir = ProjectGenerator.generate(spec, logs::add);
        assertTrue(Files.exists(projectDir));

        // Verify pom.xml
        Path pomFile = projectDir.resolve("pom.xml");
        assertTrue(Files.exists(pomFile));
        String pom = Files.readString(pomFile);

        assertTrue(pom.contains("<groupId>org.example</groupId>"));
        assertTrue(pom.contains("<artifactId>demo</artifactId>"));
        assertTrue(pom.contains("<version>5.1.5</version>"));
        assertTrue(pom.contains("micronaut-parent"));
        assertTrue(pom.contains("micronaut-http-server-netty"));
        assertTrue(pom.contains("micronaut-test-junit5"));

        // Verify Application.java
        Path appFile = projectDir.resolve("src/main/java/org/example/demo/Application.java");
        assertTrue(Files.exists(appFile));
        String app = Files.readString(appFile);
        assertTrue(app.contains("package org.example.demo;"));
        assertTrue(app.contains("Micronaut.run(Application.class, args);"));

        // Verify ApplicationTest.java
        Path testFile = projectDir.resolve("src/test/java/org/example/demo/ApplicationTest.java");
        assertTrue(Files.exists(testFile));
        String test = Files.readString(testFile);
        assertTrue(test.contains("@MicronautTest"));

        // Verify resources
        assertTrue(Files.exists(projectDir.resolve("src/main/resources/application.properties")));
        assertTrue(Files.exists(projectDir.resolve("src/main/resources/logback.xml")));
    }

    @Test
    void testGenerateMicronautGradleKotlinProject(@TempDir Path tempDir) throws IOException {
        List<String> logs = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.MICRONAUT,
                "demo-kt",
                tempDir,
                false,
                ProjectSpec.BuildSystem.GRADLE,
                ProjectSpec.Language.KOTLIN,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "org.example",
                "demo-kt",
                "org.example.demokt",
                "21",
                "",
                "",
                "",
                "",
                "",
                "1.0-SNAPSHOT",
                "",
                "",
                "",
                "",
                "",
                "https://launch.micronaut.io",
                "",
                "",
                "GRADLE_KOTLIN_DSL",
                false,
                JakartaMetadata.EE_11,
                JakartaMetadata.TEMPLATE_REST,
                "",
                "<No application server>",
                "https://invalid-host-for-local-fallback.test",
                "5.1.5",
                "KOTEST",
                "default",
                "validation",
                "gradle_kotlin"
        );

        Path projectDir = ProjectGenerator.generate(spec, logs::add);
        assertTrue(Files.exists(projectDir));

        // Verify build.gradle.kts
        Path buildFile = projectDir.resolve("build.gradle.kts");
        assertTrue(Files.exists(buildFile));
        String build = Files.readString(buildFile);

        assertTrue(build.contains("id(\"io.micronaut.application\") version \"5.1.5\""));
        assertTrue(build.contains("io.micronaut:micronaut-http-server-netty"));

        // Verify Application.kt
        Path appFile = projectDir.resolve("src/main/kotlin/org/example/demokt/Application.kt");
        assertTrue(Files.exists(appFile));
        String app = Files.readString(appFile);
        assertTrue(app.contains("package org.example.demokt"));
        assertTrue(app.contains("fun main(args: Array<String>)"));

        // Verify ApplicationTest.kt
        Path testFile = projectDir.resolve("src/test/kotlin/org/example/demokt/ApplicationTest.kt");
        assertTrue(Files.exists(testFile));
        String test = Files.readString(testFile);
        assertTrue(test.contains("@MicronautTest"));
    }

    @Test
    void testGenerateMicronautGradleGroovySpockProject(@TempDir Path tempDir) throws IOException {
        List<String> logs = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.MICRONAUT,
                "demo-groovy",
                tempDir,
                false,
                ProjectSpec.BuildSystem.GRADLE,
                ProjectSpec.Language.GROOVY,
                ProjectSpec.Packaging.JAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "org.example",
                "demo-groovy",
                "org.example.demogroovy",
                "21",
                "",
                "",
                "",
                "",
                "",
                "1.0-SNAPSHOT",
                "",
                "",
                "",
                "",
                "",
                "https://launch.micronaut.io",
                "",
                "",
                "GRADLE",
                false,
                JakartaMetadata.EE_11,
                JakartaMetadata.TEMPLATE_REST,
                "",
                "<No application server>",
                "https://invalid-host-for-local-fallback.test",
                "5.1.5",
                "SPOCK",
                "default",
                "security",
                "gradle"
        );

        Path projectDir = ProjectGenerator.generate(spec, logs::add);
        assertTrue(Files.exists(projectDir));

        // Verify build.gradle
        Path buildFile = projectDir.resolve("build.gradle");
        assertTrue(Files.exists(buildFile));
        String build = Files.readString(buildFile);

        assertTrue(build.contains("id 'io.micronaut.application' version '5.1.5'"));
        assertTrue(build.contains("io.micronaut:micronaut-http-server-netty"));

        // Verify Application.groovy
        Path appFile = projectDir.resolve("src/main/groovy/org/example/demogroovy/Application.groovy");
        assertTrue(Files.exists(appFile));
        String app = Files.readString(appFile);
        assertTrue(app.contains("package org.example.demogroovy"));
        assertTrue(app.contains("class Application"));

        // Verify ApplicationSpec.groovy
        Path testFile = projectDir.resolve("src/test/groovy/org/example/demogroovy/ApplicationSpec.groovy");
        assertTrue(Files.exists(testFile));
        String test = Files.readString(testFile);
        assertTrue(test.contains("class ApplicationSpec extends Specification"));
    }
}
