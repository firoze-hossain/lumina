package dev.lumina.project;

import dev.lumina.ui.FileExplorer;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class ExternalLibrariesAndScratchesTest {

    private static volatile boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                javaFxAvailable = true;
                latch.countDown();
            });
            latch.await(3, TimeUnit.SECONDS);
        } catch (IllegalStateException alreadyStarted) {
            try {
                Platform.setImplicitExit(false);
                CountDownLatch checkLatch = new CountDownLatch(1);
                Platform.runLater(() -> {
                    javaFxAvailable = true;
                    checkLatch.countDown();
                });
                checkLatch.await(1, TimeUnit.SECONDS);
            } catch (Throwable t) {
                javaFxAvailable = false;
            }
        } catch (Throwable ignored) {
            javaFxAvailable = false;
        }
    }

    @Test
    void testExternalLibrariesDynamicDiscovery() {
        Path projectRoot = Path.of(".").toAbsolutePath().normalize();
        ProjectTreeNode extLibsNode = ExternalLibrariesService.buildExternalLibrariesNode(projectRoot);

        assertNotNull(extLibsNode);
        assertEquals("External Libraries", extLibsNode.getDisplayName());
        assertEquals("external-libraries", extLibsNode.getIconKind());
        assertFalse(extLibsNode.isLeaf());

        List<ProjectTreeNode> topLevel = extLibsNode.loadChildren();
        assertFalse(topLevel.isEmpty(), "External Libraries should contain SDK and Maven dependencies");

        // 1. Verify SDK node
        ProjectTreeNode sdkNode = topLevel.get(0);
        assertEquals(ProjectTreeNode.NodeKind.SDK_ROOT, sdkNode.getKind());
        assertTrue(sdkNode.getDisplayName().startsWith("< ") && sdkNode.getDisplayName().contains(" > "),
                "SDK display name should match '< version > path': " + sdkNode.getDisplayName());
        assertEquals("sdk", sdkNode.getIconKind());

        // Verify SDK module children
        List<ProjectTreeNode> modules = sdkNode.loadChildren();
        assertFalse(modules.isEmpty(), "SDK should contain modular runtime libraries");
        assertTrue(modules.stream().anyMatch(m -> m.getDisplayName().startsWith("java.base")),
                "SDK must contain java.base module");

        // Find and expand java.base
        ProjectTreeNode javaBase = modules.stream()
                .filter(m -> m.getDisplayName().startsWith("java.base"))
                .findFirst()
                .orElse(null);
        assertNotNull(javaBase);
        assertTrue(javaBase.getDisplayName().endsWith("library root"));

        List<ProjectTreeNode> javaBaseChildren = javaBase.loadChildren();
        assertFalse(javaBaseChildren.isEmpty(), "java.base must have package children");
        assertTrue(javaBaseChildren.stream().anyMatch(p -> "java".equals(p.getDisplayName())),
                "java.base must contain 'java' package");

        // Find and expand 'java' package
        ProjectTreeNode javaPkg = javaBaseChildren.stream()
                .filter(p -> "java".equals(p.getDisplayName()))
                .findFirst()
                .orElse(null);
        assertNotNull(javaPkg);

        List<ProjectTreeNode> subPackages = javaPkg.loadChildren();
        assertTrue(subPackages.stream().anyMatch(p -> "io".equals(p.getDisplayName())),
                "java package must contain 'io'");
        assertTrue(subPackages.stream().anyMatch(p -> "lang".equals(p.getDisplayName())),
                "java package must contain 'lang'");

        // Find and expand 'io' package
        ProjectTreeNode ioPkg = subPackages.stream()
                .filter(p -> "io".equals(p.getDisplayName()))
                .findFirst()
                .orElse(null);
        assertNotNull(ioPkg);

        List<ProjectTreeNode> ioClasses = ioPkg.loadChildren();
        assertTrue(ioClasses.stream().anyMatch(c -> "BufferedInputStream".equals(c.getDisplayName())),
                "java.io must contain BufferedInputStream");
        assertTrue(ioClasses.stream().anyMatch(c -> "Closeable".equals(c.getDisplayName())),
                "java.io must contain Closeable");
        assertTrue(ioClasses.stream().anyMatch(c -> "File".equals(c.getDisplayName())),
                "java.io must contain File");

        // Verify interface and class detection
        ProjectTreeNode closeable = ioClasses.stream()
                .filter(c -> "Closeable".equals(c.getDisplayName()))
                .findFirst()
                .orElse(null);
        assertNotNull(closeable);
        assertEquals("interface", closeable.getIconKind());

        ProjectTreeNode bufferedIn = ioClasses.stream()
                .filter(c -> "BufferedInputStream".equals(c.getDisplayName()))
                .findFirst()
                .orElse(null);
        assertNotNull(bufferedIn);
        assertEquals("class", bufferedIn.getIconKind());

        // 2. Verify Maven libraries
        List<ProjectTreeNode> mavenLibs = topLevel.stream()
                .filter(n -> n.getKind() == ProjectTreeNode.NodeKind.LIBRARY_ROOT)
                .toList();
        assertFalse(mavenLibs.isEmpty(), "Should dynamically discover Maven libraries from pom.xml");
        assertTrue(mavenLibs.stream().anyMatch(l -> l.getDisplayName().startsWith("Maven: ")),
                "Maven libraries must have 'Maven: ' prefix");
    }

    @Test
    void testScratchesAndConsolesDynamicStructure() {
        Path projectRoot = Path.of(".").toAbsolutePath().normalize();
        ProjectTreeNode scratchesRoot = ScratchesAndConsolesService.buildScratchesAndConsolesNode(projectRoot);

        assertNotNull(scratchesRoot);
        assertEquals("Scratches and Consoles", scratchesRoot.getDisplayName());
        assertEquals("scratches", scratchesRoot.getIconKind());

        List<ProjectTreeNode> topLevel = scratchesRoot.loadChildren();
        assertFalse(topLevel.isEmpty(), "Scratches and Consoles should contain Extensions and Scratches");

        // 1. Verify Extensions
        ProjectTreeNode extNode = topLevel.stream()
                .filter(n -> "Extensions".equals(n.getDisplayName()))
                .findFirst()
                .orElse(null);
        assertNotNull(extNode, "Extensions node must exist");

        List<ProjectTreeNode> extChildren = extNode.loadChildren();
        assertTrue(extChildren.stream().anyMatch(c -> "Database Tools and SQL".equals(c.getDisplayName())),
                "Extensions must contain Database Tools and SQL");

        ProjectTreeNode dbTools = extChildren.stream()
                .filter(c -> "Database Tools and SQL".equals(c.getDisplayName()))
                .findFirst()
                .orElse(null);
        assertNotNull(dbTools);

        List<ProjectTreeNode> dbFolders = dbTools.loadChildren();
        assertTrue(dbFolders.stream().anyMatch(f -> "data".equals(f.getDisplayName())),
                "Database Tools and SQL must contain 'data'");
        assertTrue(dbFolders.stream().anyMatch(f -> "schema".equals(f.getDisplayName())),
                "Database Tools and SQL must contain 'schema'");
        assertTrue(dbFolders.stream().anyMatch(f -> "schema.layouts".equals(f.getDisplayName())),
                "Database Tools and SQL must contain 'schema.layouts'");

        // Expand 'data' -> 'aggregators' and 'extractors'
        ProjectTreeNode dataFolder = dbFolders.stream()
                .filter(f -> "data".equals(f.getDisplayName()))
                .findFirst()
                .orElse(null);
        assertNotNull(dataFolder);

        List<ProjectTreeNode> dataSubs = dataFolder.loadChildren();
        assertTrue(dataSubs.stream().anyMatch(s -> "aggregators".equals(s.getDisplayName())),
                "data must contain aggregators");
        assertTrue(dataSubs.stream().anyMatch(s -> "extractors".equals(s.getDisplayName())),
                "data must contain extractors");

        // Check aggregators scripts
        ProjectTreeNode aggregators = dataSubs.stream()
                .filter(s -> "aggregators".equals(s.getDisplayName()))
                .findFirst()
                .orElse(null);
        assertNotNull(aggregators);
        List<ProjectTreeNode> aggScripts = aggregators.loadChildren();
        assertTrue(aggScripts.stream().anyMatch(s -> "AVG.groovy".equals(s.getDisplayName())),
                "aggregators must contain AVG.groovy");
        assertTrue(aggScripts.stream().anyMatch(s -> "COUNT.groovy".equals(s.getDisplayName())),
                "aggregators must contain COUNT.groovy");

        // Check extractors scripts
        ProjectTreeNode extractors = dataSubs.stream()
                .filter(s -> "extractors".equals(s.getDisplayName()))
                .findFirst()
                .orElse(null);
        assertNotNull(extractors);
        List<ProjectTreeNode> extScripts = extractors.loadChildren();
        assertTrue(extScripts.stream().anyMatch(s -> "CSV-Groovy.csv.groovy".equals(s.getDisplayName())),
                "extractors must contain CSV-Groovy.csv.groovy");
        assertTrue(extScripts.stream().anyMatch(s -> "JSON-Groovy.json.groovy".equals(s.getDisplayName())),
                "extractors must contain JSON-Groovy.json.groovy");
    }

    @Test
    void testFileExplorerThreeTopLevelSections() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                Path projectRoot = Path.of(".").toAbsolutePath().normalize();
                FileExplorer explorer = new FileExplorer(p -> {}, () -> null);

                explorer.setRoot(projectRoot);

                assertNotNull(explorer.getTree().getRoot());
                assertFalse(explorer.getTree().isShowRoot(), "Root should be hidden for multi-section project view");

                List<TreeItem<ProjectTreeNode>> rootSections = explorer.getTree().getRoot().getChildren();
                assertEquals(3, rootSections.size(), "Project tree must have 3 top-level sections");

                // 1. Project Root Directory
                TreeItem<ProjectTreeNode> projectSection = rootSections.get(0);
                assertEquals(ProjectTreeNode.NodeKind.PROJECT_ROOT, projectSection.getValue().getKind());
                assertEquals(projectRoot.getFileName().toString(), projectSection.getValue().getDisplayName());
                assertTrue(projectSection.isExpanded(), "Project root should be expanded by default");
                assertFalse(projectSection.getChildren().isEmpty(), "Project root must list project files");

                // 2. External Libraries
                TreeItem<ProjectTreeNode> extLibsSection = rootSections.get(1);
                assertEquals(ProjectTreeNode.NodeKind.EXTERNAL_LIBRARIES_ROOT, extLibsSection.getValue().getKind());
                assertEquals("External Libraries", extLibsSection.getValue().getDisplayName());

                // 3. Scratches and Consoles
                TreeItem<ProjectTreeNode> scratchesSection = rootSections.get(2);
                assertEquals(ProjectTreeNode.NodeKind.SCRATCHES_ROOT, scratchesSection.getValue().getKind());
                assertEquals("Scratches and Consoles", scratchesSection.getValue().getDisplayName());

                // Test selectFile and getSelectedPath
                Path pomPath = projectRoot.resolve("pom.xml");
                explorer.selectFile(pomPath);
                assertEquals(pomPath, explorer.getSelectedPath());

            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testProjectSpecificJdkResolution(@TempDir Path tempDir) throws Exception {
        // Create a mock Maven project configured with Java 21
        String pomXml = """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.thundercall</groupId>
                  <artifactId>thundercall-backend</artifactId>
                  <version>1.0.0</version>
                  <properties>
                    <java.version>21</java.version>
                  </properties>
                </project>
                """;
        Files.writeString(tempDir.resolve("pom.xml"), pomXml);

        ExternalLibrariesService.JdkCandidate cand = ExternalLibrariesService.detectJdkForProject(tempDir);
        assertNotNull(cand, "Dynamic JDK detection must resolve a candidate");
        assertEquals(21, cand.majorVersion(), "Project with java.version 21 must dynamically resolve to major version 21");

        ProjectTreeNode sdkNode = ExternalLibrariesService.buildSdkNode(tempDir);
        assertNotNull(sdkNode, "SDK node should be built for project");
        assertTrue(sdkNode.getDisplayName().startsWith("< 21 > "),
                "SDK display name must dynamically start with '< 21 > ', but was: " + sdkNode.getDisplayName());
        assertFalse(sdkNode.getDisplayName().contains("< 25 >"),
                "Java 21 project must not be hardcoded to Java 25");

        // Verify ProjectStructureModel also assigns matching JDK
        ProjectStructureModel model = ProjectStructureModel.Service.load(tempDir);
        assertEquals("21", model.getLanguageLevel(), "Language level should be 21");
        assertNotNull(model.getProjectSdk(), "Project SDK should not be null");
        assertTrue(model.getProjectSdk().version().contains("21") || model.getProjectSdk().name().contains("21"),
                "Model Project SDK should match Java 21: " + model.getProjectSdk());
    }

    @Test
    void testBrandIsolation() throws Exception {
        Pattern competitorPattern = Pattern.compile("(?i)\\b(intellij|jetbrains|idea)\\b");

        List<String> filesToCheck = List.of(
                "src/main/java/dev/lumina/project/ProjectTreeNode.java",
                "src/main/java/dev/lumina/project/ExternalLibrariesService.java",
                "src/main/java/dev/lumina/project/ScratchesAndConsolesService.java",
                "src/main/java/dev/lumina/ui/FileExplorer.java"
        );

        for (String relPath : filesToCheck) {
            Path path = Path.of(relPath);
            assertTrue(Files.exists(path), "File must exist: " + relPath);
            String content = Files.readString(path);
            var matcher = competitorPattern.matcher(content);
            assertFalse(matcher.find(), "Competitor brand found in file " + relPath + ": " + (matcher.hitEnd() ? "" : matcher.group()));
        }
    }
}
