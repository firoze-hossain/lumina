package dev.lumina.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JakartaGeneratorTest {

    @Test
    void testJakartaMetadataCatalogAndDefaults() {
        List<JakartaMetadata.JakartaCategory> categories = JakartaMetadata.getCategories();
        assertEquals(2, categories.size());

        JakartaMetadata.JakartaCategory specs = categories.get(0);
        assertEquals("Specifications", specs.name());
        assertTrue(specs.dependencies().stream().anyMatch(d -> d.id().equals("full-platform")));
        assertTrue(specs.dependencies().stream().anyMatch(d -> d.id().equals("cdi")));
        assertTrue(specs.dependencies().stream().anyMatch(d -> d.id().equals("jaxrs")));
        assertTrue(specs.dependencies().stream().anyMatch(d -> d.id().equals("servlet")));
        assertTrue(specs.dependencies().stream().anyMatch(d -> d.id().equals("jpa")));
        assertTrue(specs.dependencies().stream().anyMatch(d -> d.id().equals("data")));

        JakartaMetadata.JakartaCategory impls = categories.get(1);
        assertEquals("Implementations", impls.name());
        assertTrue(impls.dependencies().stream().anyMatch(d -> d.id().equals("hibernate")));
        assertTrue(impls.dependencies().stream().anyMatch(d -> d.id().equals("jersey-server")));
        assertTrue(impls.dependencies().stream().anyMatch(d -> d.id().equals("eclipselink")));
        assertTrue(impls.dependencies().stream().anyMatch(d -> d.id().equals("weld-se")));

        // Verify template default dependency selections
        List<String> restDefaults = JakartaMetadata.getDefaultDependenciesForTemplate("REST service");
        assertEquals(List.of("cdi", "jaxrs", "servlet"), restDefaults);

        List<String> webDefaults = JakartaMetadata.getDefaultDependenciesForTemplate("Web application");
        assertEquals(List.of("servlet"), webDefaults);

        List<String> libDefaults = JakartaMetadata.getDefaultDependenciesForTemplate("Library");
        assertTrue(libDefaults.isEmpty());
    }

    @Test
    void testDynamicVersionsAcrossJakartaReleases() {
        // CDI versions across EE 11, 10, 9.1, 8
        assertEquals("4.1.0", JakartaMetadata.getVersion("cdi", JakartaMetadata.EE_11));
        assertEquals("4.0.1", JakartaMetadata.getVersion("cdi", JakartaMetadata.EE_10));
        assertEquals("3.0.0", JakartaMetadata.getVersion("cdi", JakartaMetadata.EE_9_1));
        assertEquals("2.0.2", JakartaMetadata.getVersion("cdi", JakartaMetadata.EE_8));

        // JAX-RS versions across releases
        assertEquals("4.0.0", JakartaMetadata.getVersion("jaxrs", JakartaMetadata.EE_11));
        assertEquals("3.1.0", JakartaMetadata.getVersion("jaxrs", JakartaMetadata.EE_10));
        assertEquals("3.0.0", JakartaMetadata.getVersion("jaxrs", JakartaMetadata.EE_9_1));
        assertEquals("2.1.6", JakartaMetadata.getVersion("jaxrs", JakartaMetadata.EE_8));

        // Servlet versions across releases
        assertEquals("6.1.0", JakartaMetadata.getVersion("servlet", JakartaMetadata.EE_11));
        assertEquals("6.0.0", JakartaMetadata.getVersion("servlet", JakartaMetadata.EE_10));
        assertEquals("5.0.0", JakartaMetadata.getVersion("servlet", JakartaMetadata.EE_9_1));
        assertEquals("4.0.4", JakartaMetadata.getVersion("servlet", JakartaMetadata.EE_8));

        // Hibernate implementations across releases
        assertEquals("7.0.4.Final", JakartaMetadata.getVersion("hibernate", JakartaMetadata.EE_11));
        assertEquals("6.4.4.Final", JakartaMetadata.getVersion("hibernate", JakartaMetadata.EE_10));
        assertEquals("5.6.15.Final", JakartaMetadata.getVersion("hibernate", JakartaMetadata.EE_9_1));
        assertEquals("5.4.33.Final", JakartaMetadata.getVersion("hibernate", JakartaMetadata.EE_8));

        // Eclipse Jersey Server implementation
        assertEquals("4.0.0-M2", JakartaMetadata.getVersion("jersey-server", JakartaMetadata.EE_11));
        assertEquals("3.1.5", JakartaMetadata.getVersion("jersey-server", JakartaMetadata.EE_10));
        assertEquals("3.0.3", JakartaMetadata.getVersion("jersey-server", JakartaMetadata.EE_9_1));
        assertEquals("2.35", JakartaMetadata.getVersion("jersey-server", JakartaMetadata.EE_8));
    }

    @Test
    void testGenerateJakartaRestMavenProject(@TempDir Path tempDir) throws IOException {
        List<String> logs = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.JAKARTA_EE,
                "rest-demo",
                tempDir,
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.WAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.example",
                "rest-demo",
                "com.example.restdemo",
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
                "",
                "",
                "",
                "MAVEN",
                true,
                JakartaMetadata.EE_11,
                JakartaMetadata.TEMPLATE_REST,
                "cdi,jaxrs,servlet",
                "<No application server>"
        );

        Path projectDir = ProjectGenerator.generate(spec, logs::add);
        assertTrue(Files.exists(projectDir));

        // Verify pom.xml
        Path pomFile = projectDir.resolve("pom.xml");
        assertTrue(Files.exists(pomFile));
        String pomContent = Files.readString(pomFile);

        assertTrue(pomContent.contains("<packaging>war</packaging>"));
        assertTrue(pomContent.contains("<groupId>jakarta.ws.rs</groupId>"));
        assertTrue(pomContent.contains("<artifactId>jakarta.ws.rs-api</artifactId>"));
        assertTrue(pomContent.contains("<version>4.0.0</version>"));
        assertTrue(pomContent.contains("<scope>provided</scope>"));

        assertTrue(pomContent.contains("<groupId>jakarta.enterprise</groupId>"));
        assertTrue(pomContent.contains("<artifactId>jakarta.enterprise.cdi-api</artifactId>"));
        assertTrue(pomContent.contains("<version>4.1.0</version>"));

        assertTrue(pomContent.contains("<groupId>jakarta.servlet</groupId>"));
        assertTrue(pomContent.contains("<artifactId>jakarta.servlet-api</artifactId>"));
        assertTrue(pomContent.contains("<version>6.1.0</version>"));

        assertTrue(pomContent.contains("maven-war-plugin"));
        assertTrue(pomContent.contains("<failOnMissingWebXml>false</failOnMissingWebXml>"));

        // Verify JAX-RS starter source files
        Path appFile = projectDir.resolve("src/main/java/com/example/restdemo/HelloApplication.java");
        assertTrue(Files.exists(appFile));
        String appContent = Files.readString(appFile);
        assertTrue(appContent.contains("@ApplicationPath(\"/api\")"));
        assertTrue(appContent.contains("import jakarta.ws.rs.ApplicationPath;"));

        Path resFile = projectDir.resolve("src/main/java/com/example/restdemo/HelloResource.java");
        assertTrue(Files.exists(resFile));
        String resContent = Files.readString(resFile);
        assertTrue(resContent.contains("@Path(\"/hello-world\")"));
        assertTrue(resContent.contains("@Produces(MediaType.TEXT_PLAIN)"));

        // Verify CDI beans.xml
        Path beansXml = projectDir.resolve("src/main/resources/META-INF/beans.xml");
        assertTrue(Files.exists(beansXml));
    }

    @Test
    void testGenerateJakartaWebGradleProject(@TempDir Path tempDir) throws IOException {
        List<String> logs = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.JAKARTA_EE,
                "web-demo",
                tempDir,
                false,
                ProjectSpec.BuildSystem.GRADLE,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.WAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "org.example",
                "web-demo",
                "org.example.webdemo",
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
                "",
                "",
                "",
                "GRADLE",
                true,
                JakartaMetadata.EE_11,
                JakartaMetadata.TEMPLATE_WEB,
                "servlet",
                "<No application server>"
        );

        Path projectDir = ProjectGenerator.generate(spec, logs::add);
        assertTrue(Files.exists(projectDir));

        // Verify build.gradle
        Path buildFile = projectDir.resolve("build.gradle");
        assertTrue(Files.exists(buildFile));
        String gradleContent = Files.readString(buildFile);

        assertTrue(gradleContent.contains("id 'war'"));
        assertTrue(gradleContent.contains("providedCompile('jakarta.servlet:jakarta.servlet-api:6.1.0')"));

        // Verify Servlet file
        Path servletFile = projectDir.resolve("src/main/java/org/example/webdemo/HelloServlet.java");
        assertTrue(Files.exists(servletFile));
        String servletContent = Files.readString(servletFile);
        assertTrue(servletContent.contains("@WebServlet(name = \"helloServlet\", value = \"/hello-servlet\")"));

        // Verify index.jsp and web.xml
        assertTrue(Files.exists(projectDir.resolve("src/main/webapp/index.jsp")));
        assertTrue(Files.exists(projectDir.resolve("src/main/webapp/WEB-INF/web.xml")));
    }

    @Test
    void testGenerateJakartaWithJpaHibernate(@TempDir Path tempDir) throws IOException {
        List<String> logs = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.JAKARTA_EE,
                "jpa-demo",
                tempDir,
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.WAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.example",
                "jpa-demo",
                "com.example.jpademo",
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
                "",
                "",
                "",
                "MAVEN",
                true,
                JakartaMetadata.EE_11,
                JakartaMetadata.TEMPLATE_REST,
                "jpa,hibernate",
                "<No application server>"
        );

        Path projectDir = ProjectGenerator.generate(spec, logs::add);
        assertTrue(Files.exists(projectDir));

        Path pomFile = projectDir.resolve("pom.xml");
        String pom = Files.readString(pomFile);

        // Specification JPA is provided
        assertTrue(pom.contains("<groupId>jakarta.persistence</groupId>"));
        assertTrue(pom.contains("<artifactId>jakarta.persistence-api</artifactId>"));
        assertTrue(pom.contains("<version>3.2.0</version>"));
        assertTrue(pom.contains("<scope>provided</scope>"));

        // Implementation Hibernate is compile scope
        assertTrue(pom.contains("<groupId>org.hibernate.orm</groupId>"));
        assertTrue(pom.contains("<artifactId>hibernate-core</artifactId>"));
        assertTrue(pom.contains("<version>7.0.4.Final</version>"));

        // persistence.xml generated with Hibernate provider
        Path persistenceXml = projectDir.resolve("src/main/resources/META-INF/persistence.xml");
        assertTrue(Files.exists(persistenceXml));
        String persistenceContent = Files.readString(persistenceXml);
        assertTrue(persistenceContent.contains("org.hibernate.jpa.HibernatePersistenceProvider"));
    }

    @Test
    void testDynamicJakartaEE10VersionsInBuildFile(@TempDir Path tempDir) throws IOException {
        List<String> logs = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.JAKARTA_EE,
                "ee10-demo",
                tempDir,
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.WAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.example",
                "ee10-demo",
                "com.example.ee10demo",
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
                "",
                "",
                "",
                "MAVEN",
                true,
                JakartaMetadata.EE_10,
                JakartaMetadata.TEMPLATE_REST,
                "cdi,jaxrs,servlet",
                "<No application server>"
        );

        Path projectDir = ProjectGenerator.generate(spec, logs::add);
        String pom = Files.readString(projectDir.resolve("pom.xml"));

        // Must dynamically use EE 10 versions, not EE 11!
        assertTrue(pom.contains("<version>3.1.0</version>")); // JAX-RS for EE 10
        assertTrue(pom.contains("<version>6.0.0</version>")); // Servlet for EE 10
        assertTrue(pom.contains("<version>4.0.1</version>")); // CDI for EE 10
        assertFalse(pom.contains("<version>4.0.0</version>"));
        assertFalse(pom.contains("<version>6.1.0</version>"));
        assertFalse(pom.contains("<version>4.1.0</version>"));
    }

    @Test
    void testDynamicJakartaEE8NamespacesAndVersions(@TempDir Path tempDir) throws IOException {
        List<String> logs = new ArrayList<>();
        ProjectSpec spec = new ProjectSpec(
                ProjectSpec.Generator.JAKARTA_EE,
                "ee8-demo",
                tempDir,
                false,
                ProjectSpec.BuildSystem.MAVEN,
                ProjectSpec.Language.JAVA,
                ProjectSpec.Packaging.WAR,
                ProjectSpec.ConfigFormat.PROPERTIES,
                "com.example",
                "ee8-demo",
                "com.example.ee8demo",
                "17",
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
                "",
                "",
                "",
                "MAVEN",
                true,
                JakartaMetadata.EE_8,
                JakartaMetadata.TEMPLATE_REST,
                "jaxrs,servlet",
                "<No application server>"
        );

        Path projectDir = ProjectGenerator.generate(spec, logs::add);
        String pom = Files.readString(projectDir.resolve("pom.xml"));

        // Must dynamically use EE 8 versions
        assertTrue(pom.contains("<version>2.1.6</version>")); // JAX-RS for EE 8
        assertTrue(pom.contains("<version>4.0.4</version>")); // Servlet for EE 8

        // Must use javax namespace in source code for EE 8
        String app = Files.readString(projectDir.resolve("src/main/java/com/example/ee8demo/HelloApplication.java"));
        assertTrue(app.contains("import javax.ws.rs.ApplicationPath;"));
        assertTrue(app.contains("import javax.ws.rs.core.Application;"));
    }
}
