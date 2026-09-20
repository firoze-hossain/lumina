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

    @Test
    void testModulePathsConfiguration(@TempDir Path tempDir) {
        ModuleModel module = new ModuleModel("test-module", tempDir);
        module.setInheritCompilerOutput(false);
        module.setOutputPath("/path/to/classes");
        module.setTestOutputPath("/path/to/test-classes");
        module.setExcludeOutputPaths(false);

        assertFalse(module.isInheritCompilerOutput());
        assertEquals("/path/to/classes", module.getOutputPath());
        assertEquals("/path/to/test-classes", module.getTestOutputPath());
        assertFalse(module.isExcludeOutputPaths());

        module.getJavadocPaths().add("https://docs.oracle.com/en/java/javase/21/docs/api/");
        module.getExternalAnnotationsPaths().add("/path/to/annotations");

        assertEquals(1, module.getJavadocPaths().size());
        assertEquals(1, module.getExternalAnnotationsPaths().size());
        assertTrue(module.getJavadocPaths().getFirst().startsWith("https://"));
        assertEquals("/path/to/annotations", module.getExternalAnnotationsPaths().getFirst());
    }

    @Test
    void testModuleDependenciesHierarchy(@TempDir Path tempDir) throws IOException {
        String pomXml = """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>dev.lumina</groupId>
                  <artifactId>sample-dep-app</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                    <dependency>
                      <groupId>org.openjfx</groupId>
                      <artifactId>javafx-controls</artifactId>
                      <version>23.0.2</version>
                      <scope>compile</scope>
                    </dependency>
                    <dependency>
                      <groupId>org.junit.jupiter</groupId>
                      <artifactId>junit-jupiter</artifactId>
                      <version>5.10.2</version>
                      <scope>test</scope>
                    </dependency>
                  </dependencies>
                </project>
                """;
        Files.writeString(tempDir.resolve("pom.xml"), pomXml);

        ProjectStructureModel model = ProjectStructureModel.Service.load(tempDir);
        ModuleModel module = model.getPrimaryModule();

        assertNotNull(module);
        List<ProjectStructureModel.DependencyItem> deps = module.getDependencies();
        assertTrue(deps.size() >= 4, "Should have SDK, Module source, and at least 2 Maven dependencies");

        // Row 1: SDK
        ProjectStructureModel.DependencyItem row1 = deps.get(0);
        assertTrue(row1.isSdk(), "Row 1 must be SDK entry");

        // Row 2: Module source
        ProjectStructureModel.DependencyItem row2 = deps.get(1);
        assertTrue(row2.isModuleSource(), "Row 2 must be Module Source");
        assertEquals("<Module source>", row2.getName());

        // Row 3: Library with Scope
        ProjectStructureModel.DependencyItem row3 = deps.get(2);
        assertFalse(row3.isSdk());
        assertFalse(row3.isModuleSource());
        assertTrue(row3.getName().contains("javafx-controls"));
        assertEquals("Compile", row3.getScope());
        assertFalse(row3.isExport());

        // Test export toggle
        row3.setExport(true);
        assertTrue(row3.isExport());

        // Row 4: Test scope
        ProjectStructureModel.DependencyItem row4 = deps.get(3);
        assertTrue(row4.getName().contains("junit-jupiter"));
        assertEquals("Test", row4.getScope());
    }

    @Test
    void testLibraryDynamicPathResolution() {
        ProjectStructureModel.LibraryModel lib = ProjectStructureModel.LibraryModel.createMavenLibrary(
                "com.google.code.gson", "gson", "2.10.1"
        );
        assertNotNull(lib);
        assertEquals("Maven: com.google.code.gson:gson:2.10.1", lib.getName());

        assertFalse(lib.getClassesPaths().isEmpty());
        String classJar = lib.getClassesPaths().getFirst();
        assertTrue(classJar.contains(".m2"));
        assertTrue(classJar.contains("gson-2.10.1.jar"));

        assertFalse(lib.getSourcesPaths().isEmpty());
        String sourceJar = lib.getSourcesPaths().getFirst();
        assertTrue(sourceJar.contains("gson-2.10.1-sources.jar"));

        assertFalse(lib.getJavadocPaths().isEmpty());
        String javadocJar = lib.getJavadocPaths().getFirst();
        assertTrue(javadocJar.contains("gson-2.10.1-javadoc.jar"));

        // Classifier test
        ProjectStructureModel.LibraryModel classifierLib = ProjectStructureModel.LibraryModel.createMavenLibrary(
                "org.openjfx", "javafx-controls", "mac-aarch64", "23.0.2"
        );
        assertEquals("Maven: org.openjfx:javafx-controls:mac-aarch64:23.0.2", classifierLib.getName());
        assertTrue(classifierLib.getClassesPaths().getFirst().contains("javafx-controls-23.0.2-mac-aarch64.jar"));
    }

    @Test
    void testFacetsAddAndConfiguration(@TempDir Path tempDir) throws IOException {
        ProjectStructureModel model = ProjectStructureModel.Service.load(tempDir);
        String modName = model.getPrimaryModule().getName();

        // Test glyph lookup
        assertEquals("🍃", ProjectStructureModel.FacetModel.getIconGlyph("Spring"));
        assertEquals("🍃", ProjectStructureModel.FacetModel.getIconGlyph("Spring Boot"));
        assertEquals("🔷", ProjectStructureModel.FacetModel.getIconGlyph("Kotlin"));
        assertEquals("🐍", ProjectStructureModel.FacetModel.getIconGlyph("Python"));
        assertEquals("🧊", ProjectStructureModel.FacetModel.getIconGlyph("Hibernate"));
        assertEquals("🗄", ProjectStructureModel.FacetModel.getIconGlyph("JPA"));
        assertEquals("🌐", ProjectStructureModel.FacetModel.getIconGlyph("Web"));
        assertEquals("🏢", ProjectStructureModel.FacetModel.getIconGlyph("JavaEE Application"));
        assertEquals("💎", ProjectStructureModel.FacetModel.getIconGlyph("JRuby"));
        assertEquals("🛤", ProjectStructureModel.FacetModel.getIconGlyph("JRuby on Rails"));

        // Add facets
        ProjectStructureModel.FacetModel springFacet = new ProjectStructureModel.FacetModel("Spring Boot", "Spring", modName);
        springFacet.getConfiguration().put("Application Context", "application.yml");
        model.getFacets().add(springFacet);

        ProjectStructureModel.FacetModel kotlinFacet = new ProjectStructureModel.FacetModel("Kotlin", "Kotlin", modName);
        kotlinFacet.getConfiguration().put("Language Version", "2.1");
        model.getFacets().add(kotlinFacet);

        // Save & reload to verify persistence
        ProjectStructureModel.Service.save(model);
        ProjectStructureModel reloaded = ProjectStructureModel.Service.load(tempDir);

        assertNotNull(reloaded);
        assertEquals(2, reloaded.getFacets().size());
        assertTrue(reloaded.getFacets().stream().anyMatch(f -> "Spring Boot".equals(f.getName())));
        assertTrue(reloaded.getFacets().stream().anyMatch(f -> "Kotlin".equals(f.getName())));
    }

    @Test
    void testJdkClasspathModulesResolution() {
        List<ProjectSdk.SdkItem> all = ProjectSdk.discoverAllSdks();
        ProjectSdk.SdkItem jdk = all.stream()
                .filter(s -> s.type() == ProjectSdk.SdkType.JDK && s.homePath() != null)
                .findFirst()
                .orElse(null);

        assertNotNull(jdk, "Expected at least one JDK detected");
        List<String> cp = ProjectSdk.detectJdkClasspath(jdk.homePath());
        assertNotNull(cp);
        assertFalse(cp.isEmpty(), "Classpath entries should not be empty");

        // Verify IntelliJ IDEA format: entries contain '!/java.' or end with .jar/.jmod
        boolean hasModuleEntry = cp.stream().anyMatch(e -> e.contains("!/java.base") || e.endsWith("java.base"));
        assertTrue(hasModuleEntry, "JDK classpath should contain java.base module matching IntelliJ IDEA format: " + cp.subList(0, Math.min(5, cp.size())));
    }

    @Test
    void testJdkSourcepathModulesResolution() {
        List<ProjectSdk.SdkItem> all = ProjectSdk.discoverAllSdks();
        ProjectSdk.SdkItem jdk = all.stream()
                .filter(s -> s.type() == ProjectSdk.SdkType.JDK && s.homePath() != null)
                .findFirst()
                .orElse(null);

        assertNotNull(jdk);
        List<String> sp = ProjectSdk.detectJdkSourcepath(jdk.homePath());
        assertNotNull(sp);
        // If host JDK has src.zip, verify it contains !/
        Path srcZip = Path.of(jdk.homePath()).resolve("lib/src.zip");
        if (Files.isRegularFile(srcZip)) {
            assertFalse(sp.isEmpty());
            boolean hasZipBang = sp.stream().anyMatch(e -> e.contains("lib/src.zip!"));
            assertTrue(hasZipBang, "Sourcepath should format entries with lib/src.zip!/<module>: " + sp);
        }
    }

    @Test
    void testJdkAnnotationsAndDocumentation() {
        List<String> annotations = ProjectSdk.detectJdkAnnotations();
        assertNotNull(annotations);
        assertFalse(annotations.isEmpty());
        assertTrue(annotations.getFirst().contains("jdkAnnotations.jar"));

        // By default, IntelliJ IDEA does not bundle local documentation for detected JDKs; list is empty
        List<String> docs21 = ProjectSdk.detectJdkDocumentation("21");
        assertNotNull(docs21);
        assertTrue(docs21.isEmpty(), "JDK documentation list should be empty by default matching IntelliJ IDEA");

        // When the user clicks the 🌐 icon, resolveStandardJdkDocUrl produces the dynamic Oracle JavaDoc URL
        assertEquals("https://docs.oracle.com/en/java/javase/21/docs/api/", ProjectSdk.resolveStandardJdkDocUrl("21"));
        assertEquals("https://docs.oracle.com/en/java/javase/25/docs/api/", ProjectSdk.resolveStandardJdkDocUrl("25.0.1"));
        assertEquals("https://docs.oracle.com/en/java/javase/17/docs/api/", ProjectSdk.resolveStandardJdkDocUrl("17.0.9"));
        assertEquals("https://docs.oracle.com/en/java/javase/21/docs/api/", ProjectSdk.resolveStandardJdkDocUrl(null));
    }

    @Test
    void testArtifactsCreationDuplicationAndLayout() {
        ProjectStructureModel.ArtifactModel jarArt = new ProjectStructureModel.ArtifactModel(
                "lumina:jar", "JAR", "/path/to/out/artifacts/lumina_jar", true
        );
        jarArt.getOutputLayout().add("'lumina' compile output");
        jarArt.getOutputLayout().add("gson-2.10.1.jar");

        assertEquals("lumina:jar", jarArt.getName());
        assertEquals("JAR", jarArt.getType());
        assertEquals("/path/to/out/artifacts/lumina_jar", jarArt.getOutputPath());
        assertTrue(jarArt.isIncludeInBuild());
        assertEquals(2, jarArt.getOutputLayout().size());
        assertEquals("📦", ProjectStructureModel.ArtifactModel.getIconGlyph("JAR"));

        // Duplicate
        ProjectStructureModel.ArtifactModel dup = jarArt.duplicate("lumina:jar2");
        assertEquals("lumina:jar2", dup.getName());
        assertEquals("JAR", dup.getType());
        assertEquals(jarArt.getOutputPath(), dup.getOutputPath());
        assertTrue(dup.isIncludeInBuild());
        assertEquals(2, dup.getOutputLayout().size());

        // Glyph tests
        assertEquals("🔷", ProjectStructureModel.ArtifactModel.getIconGlyph("Run-time image (JLink)"));
        assertEquals("🧩", ProjectStructureModel.ArtifactModel.getIconGlyph("JavaFX application"));
        assertEquals("🌐", ProjectStructureModel.ArtifactModel.getIconGlyph("Web Application: Exploded"));
        assertEquals("☕", ProjectStructureModel.ArtifactModel.getIconGlyph("Java EE Application: Archive"));
        assertEquals("☕", ProjectStructureModel.ArtifactModel.getIconGlyph("EJB Application: Exploded"));
        assertEquals("💿", ProjectStructureModel.ArtifactModel.getIconGlyph("Platform specific package (DMG)"));
    }

    @Test
    void testGlobalLibrariesPersistence() {
        String testLibName = "TestGlobalLibraryUnit";
        ProjectStructureModel.LibraryModel lib = new ProjectStructureModel.LibraryModel(testLibName);
        lib.getClassesPaths().add("/path/to/classes.jar");
        lib.getSourcesPaths().add("/path/to/sources.jar");
        lib.getJavadocPaths().add("https://example.com/api");

        // Add & persist
        ProjectStructureModel.GlobalLibraries.add(lib);

        // Load & verify
        List<ProjectStructureModel.LibraryModel> loaded = ProjectStructureModel.GlobalLibraries.load();
        assertNotNull(loaded);
        assertTrue(loaded.stream().anyMatch(l -> l.getName().equals(testLibName)));

        ProjectStructureModel.LibraryModel found = loaded.stream()
                .filter(l -> l.getName().equals(testLibName))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of("/path/to/classes.jar"), found.getClassesPaths());
        assertEquals(List.of("/path/to/sources.jar"), found.getSourcesPaths());
        assertEquals(List.of("https://example.com/api"), found.getJavadocPaths());

        // Remove & verify
        ProjectStructureModel.GlobalLibraries.remove(lib);
        List<ProjectStructureModel.LibraryModel> afterRemove = ProjectStructureModel.GlobalLibraries.load();
        assertFalse(afterRemove.stream().anyMatch(l -> l.getName().equals(testLibName)));
    }

    @Test
    void testLibraryModelDuplicationAndGlyphs() {
        ProjectStructureModel.LibraryModel lib = new ProjectStructureModel.LibraryModel("Maven: org.junit.jupiter:junit-jupiter:5.10.2");
        lib.getClassesPaths().add("/path/to/junit-jupiter.jar");
        lib.getSourcesPaths().add("/path/to/junit-jupiter-sources.jar");

        ProjectStructureModel.LibraryModel copy = lib.duplicate("Maven: org.junit.jupiter:junit-jupiter:5.10.2 (copy)");
        assertEquals("Maven: org.junit.jupiter:junit-jupiter:5.10.2 (copy)", copy.getName());
        assertEquals(List.of("/path/to/junit-jupiter.jar"), copy.getClassesPaths());
        assertEquals(List.of("/path/to/junit-jupiter-sources.jar"), copy.getSourcesPaths());

        // Glyph checks
        assertEquals("Ⓜ", ProjectStructureModel.LibraryModel.getIconGlyph("Maven: org.example:demo:1.0"));
        assertEquals("🟪", ProjectStructureModel.LibraryModel.getIconGlyph("KotlinJavaRuntime"));
        assertEquals("🔴", ProjectStructureModel.LibraryModel.getIconGlyph("scala-sdk-2.13.12"));
        assertEquals("📚", ProjectStructureModel.LibraryModel.getIconGlyph("custom-library"));
    }

    @Test
    void testSdkRegistrationFromDetected() {
        String testName = "Test Custom JDK 21";
        String testHome = "/Library/Java/JavaVirtualMachines/test-jdk-21";
        ProjectSdk.SdkItem registered = ProjectSdk.registerSdk(testName, ProjectSdk.SdkType.JDK, testHome, "21");

        assertNotNull(registered);
        assertEquals(testName, registered.name());
        assertEquals(ProjectSdk.SdkType.JDK, registered.type());
        assertEquals(testHome, registered.homePath());
        assertTrue(registered.isRegistered());

        // Verify loaded from preferences
        List<ProjectSdk.SdkItem> list = ProjectSdk.loadRegisteredSdks();
        assertTrue(list.stream().anyMatch(s -> s.name().equals(testName) && testHome.equals(s.homePath())));

        // Clean up
        list.removeIf(s -> s.name().equals(testName));
        ProjectSdk.saveRegisteredSdks(list);
    }
}
