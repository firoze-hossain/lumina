package dev.lumina.project;

import dev.lumina.project.ProjectStructureModel.FolderType;
import dev.lumina.project.ProjectStructureModel.ModuleModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectStructureTest {

    @Test
    void testDiscoverAllSdks() {
        List<ProjectSdk.SdkItem> sdks = ProjectSdk.discoverAllSdks();
        assertNotNull(sdks, "Discovered SDKs should not be null");
        assertFalse(sdks.isEmpty(), "Expected at least one SDK detected on this machine");

        // Check for presence of at least one JDK
        boolean hasJdk = sdks.stream().anyMatch(s -> s.type() == ProjectSdk.SdkType.JDK);
        assertTrue(hasJdk, "Host machine should have at least one JDK detected");

        ProjectSdk.SdkItem firstJdk = sdks.stream()
                .filter(s -> s.type() == ProjectSdk.SdkType.JDK)
                .findFirst()
                .orElseThrow();
        assertNotNull(firstJdk.homePath());
        assertNotNull(firstJdk.version());
        assertFalse(firstJdk.classpathEntries().isEmpty(), "JDK should have resolved classpath modules or jars");
    }

    @Test
    void testSdkClasspathResolution() {
        List<ProjectSdk.SdkItem> sdks = ProjectSdk.discoverAllSdks();
        ProjectSdk.SdkItem jdk = sdks.stream()
                .filter(s -> s.type() == ProjectSdk.SdkType.JDK)
                .findFirst()
                .orElse(null);

        if (jdk != null) {
            List<String> cp = ProjectSdk.detectJdkClasspath(jdk.homePath());
            assertNotNull(cp);
            assertFalse(cp.isEmpty(), "Classpath entries for JDK should not be empty");
        }
    }

    @Test
    void testMavenProjectInspection(@TempDir Path tempDir) throws IOException {
        // Create Maven structure
        String pomXml = """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>demo-app</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                    <dependency>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-web</artifactId>
                      <version>3.2.0</version>
                    </dependency>
                    <dependency>
                      <groupId>org.openjfx</groupId>
                      <artifactId>javafx-controls</artifactId>
                      <version>21.0.1</version>
                    </dependency>
                  </dependencies>
                </project>
                """;
        Files.writeString(tempDir.resolve("pom.xml"), pomXml);
        Files.createDirectories(tempDir.resolve("src/main/java/com/example"));
        Files.createDirectories(tempDir.resolve("src/main/resources"));
        Files.createDirectories(tempDir.resolve("src/test/java/com/example"));
        Files.createDirectories(tempDir.resolve("src/test/resources"));
        Files.createDirectories(tempDir.resolve("target/classes"));

        ProjectStructureModel model = ProjectStructureModel.Service.load(tempDir);

        assertNotNull(model);
        assertEquals("demo-app", model.getProjectName());
        assertEquals(tempDir.resolve("target/classes").toString(), model.getCompilerOutput());

        assertFalse(model.getModules().isEmpty(), "Should detect at least 1 module for Maven project");
        ModuleModel module = model.getModules().getFirst();
        assertEquals("JAVA_MODULE", module.getType());

        // Check source roots
        assertTrue(module.getSourceFolders().stream().anyMatch(p -> p.endsWith("src/main/java")));
        assertTrue(module.getResourceFolders().stream().anyMatch(p -> p.endsWith("src/main/resources")));
        assertTrue(module.getTestSourceFolders().stream().anyMatch(p -> p.endsWith("src/test/java")));
        assertTrue(module.getTestResourceFolders().stream().anyMatch(p -> p.endsWith("src/test/resources")));
        assertTrue(module.getExcludedFolders().stream().anyMatch(p -> p.endsWith("target")));

        // Check Facets
        assertFalse(model.getFacets().isEmpty(), "Should detect facets from pom.xml dependencies");
        assertTrue(model.getFacets().stream().anyMatch(f -> f.getName().contains("Spring Boot")));
        assertTrue(model.getFacets().stream().anyMatch(f -> f.getName().contains("JavaFX")));

        // Check Dependencies
        assertTrue(module.getDependencies().stream().anyMatch(d -> d.getName().contains("spring-boot-starter-web")));
        assertTrue(module.getDependencies().stream().anyMatch(d -> d.getName().contains("javafx-controls")));
    }

    @Test
    void testPythonProjectInspection(@TempDir Path tempDir) throws IOException {
        // Create Python project structure
        Files.writeString(tempDir.resolve("requirements.txt"), "fastapi==0.100.0\nuvicorn>=0.22.0\npytest>=7.0.0\n");
        Files.createDirectories(tempDir.resolve("tests"));
        Files.createDirectories(tempDir.resolve("src"));
        Files.createDirectories(tempDir.resolve("venv"));

        ProjectStructureModel model = ProjectStructureModel.Service.load(tempDir);
        assertNotNull(model);
        assertFalse(model.getModules().isEmpty());

        ModuleModel module = model.getModules().getFirst();
        assertEquals("PYTHON_MODULE", module.getType());

        // Check excluded venv and test folder
        assertTrue(module.getExcludedFolders().stream().anyMatch(p -> p.endsWith("venv")), "venv should be excluded");
        assertTrue(module.getTestSourceFolders().stream().anyMatch(p -> p.endsWith("tests")), "tests should be test source");

        // Check dependencies
        assertTrue(module.getDependencies().stream().anyMatch(d -> d.getName().contains("fastapi")));
        assertTrue(module.getDependencies().stream().anyMatch(d -> d.getName().contains("uvicorn")));
        assertTrue(module.getDependencies().stream().anyMatch(d -> d.getName().contains("pytest")));
    }

    @Test
    void testPhpProjectInspection(@TempDir Path tempDir) throws IOException {
        // Create PHP project structure
        String composerJson = """
                {
                  "name": "vendor/package",
                  "require": {
                    "php": "^8.2",
                    "monolog/monolog": "^3.0"
                  }
                }
                """;
        Files.writeString(tempDir.resolve("composer.json"), composerJson);
        Files.createDirectories(tempDir.resolve("src"));
        Files.createDirectories(tempDir.resolve("tests"));
        Files.createDirectories(tempDir.resolve("vendor"));

        ProjectStructureModel model = ProjectStructureModel.Service.load(tempDir);
        assertNotNull(model);
        assertFalse(model.getModules().isEmpty());

        ModuleModel module = model.getModules().getFirst();
        assertEquals("PHP_MODULE", module.getType());

        assertTrue(module.getExcludedFolders().stream().anyMatch(p -> p.endsWith("vendor")), "vendor should be excluded");
        assertTrue(module.getTestSourceFolders().stream().anyMatch(p -> p.endsWith("tests")), "tests should be test source");

        assertTrue(module.getDependencies().stream().anyMatch(d -> d.getName().contains("monolog")));
    }

    @Test
    void testFolderMarkingAndUnmarking(@TempDir Path tempDir) {
        ModuleModel module = new ModuleModel("test-module", tempDir);
        String path = "custom_src";

        // Mark as SOURCES
        module.markFolder(path, FolderType.SOURCE);
        assertEquals(FolderType.SOURCE, module.getFolderType(path));
        assertTrue(module.getSourceFolders().contains(path));

        // Re-mark as TEST_SOURCE (should automatically remove from SOURCE)
        module.markFolder(path, FolderType.TEST_SOURCE);
        assertEquals(FolderType.TEST_SOURCE, module.getFolderType(path));
        assertFalse(module.getSourceFolders().contains(path));
        assertTrue(module.getTestSourceFolders().contains(path));

        // Unmark
        module.unmarkFolder(path);
        assertNull(module.getFolderType(path));
        assertFalse(module.getTestSourceFolders().contains(path));
    }

    @Test
    void testPersistenceLoadAndSave(@TempDir Path tempDir) throws IOException {
        // Initial load on empty dir
        ProjectStructureModel model = ProjectStructureModel.Service.load(tempDir);
        assertNotNull(model);

        // Customize settings
        model.setProjectName("CustomProjectName");
        model.setLanguageLevel("17");
        model.setCompilerOutput(tempDir.resolve("out/production/custom").toString());

        ProjectSdk.SdkItem customSdk = new ProjectSdk.SdkItem(
                "custom-jdk-17",
                "Custom-JDK-17",
                ProjectSdk.SdkType.JDK,
                "/path/to/custom/jdk",
                "17.0.0",
                true,
                false,
                List.of()
        );
        model.setProjectSdk(customSdk);

        // Save
        ProjectStructureModel.Service.save(model);

        // Check that .idea and .lumina configuration files were generated
        assertTrue(Files.exists(tempDir.resolve(".idea/misc.xml")), ".idea/misc.xml should exist");
        assertTrue(Files.exists(tempDir.resolve(".idea/modules.xml")), ".idea/modules.xml should exist");
        assertTrue(Files.exists(tempDir.resolve(".lumina/project-structure.json")), ".lumina config should exist");

        // Verify XML content
        String miscXml = Files.readString(tempDir.resolve(".idea/misc.xml"));
        assertTrue(miscXml.contains("project-jdk-name=\"Custom-JDK-17\""));
        assertTrue(miscXml.contains("languageLevel=\"JDK_17\""));

        // Reload and verify
        ProjectStructureModel reloaded = ProjectStructureModel.Service.load(tempDir);
        assertEquals("CustomProjectName", reloaded.getProjectName());
        assertNotNull(reloaded.getProjectSdk());
        assertEquals("Custom-JDK-17", reloaded.getProjectSdk().name());
        assertEquals("17", reloaded.getLanguageLevel());
        assertEquals(tempDir.resolve("out/production/custom").toString(), reloaded.getCompilerOutput());
    }

    @Test
    void testDownloadJdkPackageAndRegistration(@TempDir Path tempDir) {
        List<JdkMetadata.JdkPackage> pkgs = JdkMetadata.fetchPackages(27, false);
        assertNotNull(pkgs);
        assertFalse(pkgs.isEmpty());

        JdkMetadata.JdkPackage pkg = pkgs.stream()
                .filter(p -> p.vendorDisplay().contains("Oracle OpenJDK"))
                .findFirst()
                .orElse(pkgs.getFirst());

        assertNotNull(pkg.formatDisplay());
        assertNotNull(pkg.formatArchiveSize());
        assertTrue(pkg.formatArchiveSize().contains("MB"), "Archive size should be formatted in MB");

        Path installDir = JdkMetadata.getDefaultInstallDir(pkg.vendorDisplay(), pkg.majorVersion(), pkg.javaVersion());
        assertNotNull(installDir);
        assertTrue(installDir.toString().contains("openjdk-27") || installDir.toString().contains("27"));

        // Test registering from a completed installation
        JdkMetadata.JdkInstallation mockInst = new JdkMetadata.JdkInstallation(
                "Oracle OpenJDK 27",
                tempDir.toString(),
                "27.0.0",
                27,
                "Oracle OpenJDK",
                "aarch64",
                true
        );
        ProjectSdk.SdkItem registered = ProjectSdk.registerFromInstallation(mockInst);
        assertNotNull(registered);
        assertEquals(ProjectSdk.SdkType.JDK, registered.type());
        assertTrue(registered.name().contains("Oracle OpenJDK"));
        assertTrue(registered.name().contains("27"));
        assertTrue(registered.isRegistered());

        // Verify it appears in discoverAllSdks()
        List<ProjectSdk.SdkItem> all = ProjectSdk.discoverAllSdks();
        assertTrue(all.stream().anyMatch(s -> s.id().equals(registered.id())));
    }
}
