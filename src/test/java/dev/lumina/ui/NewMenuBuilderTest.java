package dev.lumina.ui;

import javafx.application.Platform;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NewMenuBuilderTest {

    private static volatile boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            String display = System.getenv("DISPLAY");
            if (display == null || display.isBlank() || java.awt.GraphicsEnvironment.isHeadless()) {
                return;
            }
            Platform.startup(() -> javaFxAvailable = true);
            javaFxAvailable = true;
        } catch (Throwable ignored) {
            // Headless environment without DISPLAY
        }
    }

    @Test
    void testIsInsideSourceRoot() {
        // Outside source root
        assertFalse(NewMenuBuilder.isInsideSourceRoot(null));
        assertFalse(NewMenuBuilder.isInsideSourceRoot(Path.of("/projects/lumina")));
        assertFalse(NewMenuBuilder.isInsideSourceRoot(Path.of("/projects/lumina/src")));
        assertFalse(NewMenuBuilder.isInsideSourceRoot(Path.of("/projects/lumina/src/main")));
        assertFalse(NewMenuBuilder.isInsideSourceRoot(Path.of("/projects/lumina/src/main/resources")));
        assertFalse(NewMenuBuilder.isInsideSourceRoot(Path.of("/projects/lumina/.idea")));

        // Inside source root
        assertTrue(NewMenuBuilder.isInsideSourceRoot(Path.of("/projects/lumina/src/main/java")));
        assertTrue(NewMenuBuilder.isInsideSourceRoot(Path.of("/projects/lumina/src/main/java/dev/lumina/diagnostics")));
        assertTrue(NewMenuBuilder.isInsideSourceRoot(Path.of("/projects/lumina/src/main/java/dev/lumina/diagnostics/FieldDiagnostics.java")));
        assertTrue(NewMenuBuilder.isInsideSourceRoot(Path.of("/projects/lumina/src/test/java")));
        assertTrue(NewMenuBuilder.isInsideSourceRoot(Path.of("/projects/lumina/src/test/java/dev/lumina/diagnostics/MethodDiagnosticsTest.java")));
        assertTrue(NewMenuBuilder.isInsideSourceRoot(Path.of("/projects/lumina/src/main/kotlin/app")));
        assertTrue(NewMenuBuilder.isInsideSourceRoot(Path.of("/projects/lumina/src/test/kotlin/app")));
        assertTrue(NewMenuBuilder.isInsideSourceRoot(Path.of("/projects/lumina/src/main/groovy/app")));
    }

    @Test
    void testIsProjectRoot() throws Exception {
        java.nio.file.Path tempProject = java.nio.file.Files.createTempDirectory("lumina_test_proj");
        try {
            java.nio.file.Files.createFile(tempProject.resolve("pom.xml"));
            java.nio.file.Path srcDir = java.nio.file.Files.createDirectories(tempProject.resolve("src/main/java"));
            java.nio.file.Path luminaDir = java.nio.file.Files.createDirectories(tempProject.resolve(".lumina"));

            assertTrue(NewMenuBuilder.isProjectRoot(tempProject), "Directory with pom.xml should be recognized as project root");
            assertFalse(NewMenuBuilder.isProjectRoot(tempProject.resolve("src")), "src should not be recognized as project root");
            assertFalse(NewMenuBuilder.isProjectRoot(tempProject.resolve("src/main")), "src/main should not be recognized as project root");
            assertFalse(NewMenuBuilder.isProjectRoot(luminaDir), ".lumina subfolder should not be recognized as project root");
            assertFalse(NewMenuBuilder.isProjectRoot(srcDir), "src/main/java should not be recognized as project root");
            assertFalse(NewMenuBuilder.isProjectRoot(null), "null path should return false");
        } finally {
            // Clean up temporary files
            try (java.util.stream.Stream<Path> stream = java.nio.file.Files.walk(tempProject)) {
                stream.sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);
            } catch (Exception ignored) {}
        }
    }

    @Test
    void testMenuItemSpecsInContextMenuProjectRoot() {
        // Image 1 & 2: Right-click on project root (e.g. lumina [lumina-ide])
        Path rootPath = Path.of("/projects/lumina");
        List<NewMenuBuilder.ItemSpec> specs = NewMenuBuilder.getMenuItemSpecs(rootPath, true, true);

        List<String> labels = extractLabels(specs);

        // First item must be Module... (Image 1)
        assertEquals("Module\u2026", labels.get(0), "First item at project root must be Module...");

        // Multi-language file items (Image 1 & 2)
        assertTrue(labels.contains("Python File"), "Should contain Python File");
        assertTrue(labels.contains("Jupyter Notebook"), "Should contain Jupyter Notebook");
        assertTrue(labels.contains("Go File"), "Should contain Go File");
        assertTrue(labels.contains("PHP File"), "Should contain PHP File");
        assertTrue(labels.contains("PHP Class"), "Should contain PHP Class");
        assertTrue(labels.contains("File"), "Should contain File");
        assertTrue(labels.contains("Go Modules File"), "Should contain Go Modules File");
        assertTrue(labels.contains("Scratch File"), "Should contain Scratch File");
        assertTrue(labels.contains("Directory"), "Should contain Directory");
        assertTrue(labels.contains("Python Package"), "Should contain Python Package");
        assertTrue(labels.contains("Kotlin Script"), "Should contain Kotlin Script");
        assertTrue(labels.contains("Kotlin Notebook"), "Should contain Kotlin Notebook");
        assertTrue(labels.contains("JavaScript File"), "Should contain JavaScript File");
        assertTrue(labels.contains("TypeScript File"), "Should contain TypeScript File");
        assertTrue(labels.contains("HTML File"), "Should contain HTML File");
        assertTrue(labels.contains("Stylesheet"), "Should contain Stylesheet");
        assertTrue(labels.contains("Dockerfile"), "Should contain Dockerfile");
        assertTrue(labels.contains("Dev Container Config\u2026"), "Should contain Dev Container Config...");
        assertTrue(labels.contains("Jupyter Connection"), "Should contain Jupyter Connection on project root");
        assertTrue(labels.contains("HTTP Request"), "Should contain HTTP Request");
        assertTrue(labels.contains("OpenAPI Specification"), "Should contain OpenAPI Specification");
        assertTrue(labels.contains("Kubernetes Resource"), "Should contain Kubernetes Resource");
        assertTrue(labels.contains("Helm Chart"), "Should contain Helm Chart");
        assertTrue(labels.contains("ERB File"), "Should contain ERB File");
        assertTrue(labels.contains("composer.json File"), "Should contain composer.json File");
        assertTrue(labels.contains("Resource Bundle"), "Should contain Resource Bundle");
        assertTrue(labels.contains("EditorConfig File"), "Should contain EditorConfig File");
        assertTrue(labels.contains("Data Source in Path"), "Should contain Data Source in Path");
        assertTrue(labels.contains("PHP Test"), "Should contain PHP Test");

        // Must NOT contain Java compilation unit options (no Java Class, Package, FXML, etc.)
        assertFalse(labels.contains("Java Class"), "Context menu at project root should not contain Java Class");
        assertFalse(labels.contains("Package"), "Context menu at project root should not contain Package");
        assertFalse(labels.contains("FXML File"), "Context menu at project root should not contain FXML File");
        assertFalse(labels.contains("JavaFX Application"), "Context menu at project root should not contain JavaFX Application");
        assertFalse(labels.contains("package-info.java"), "Context menu at project root should not contain package-info.java");
        assertFalse(labels.contains("module-info.java"), "Context menu at project root should not contain module-info.java");
    }

    @Test
    void testMenuItemSpecsInContextMenuUnderRootBeforeSourceRoot() {
        // Image 3: Right-click on folder under root before source root (e.g. .lumina, src, src/main)
        Path underRootPath = Path.of("/projects/lumina/.lumina");
        List<NewMenuBuilder.ItemSpec> specs = NewMenuBuilder.getMenuItemSpecs(underRootPath, true, false);

        List<String> labels = extractLabels(specs);

        // First item must be Python File, NOT Module... (Image 3)
        assertEquals("Python File", labels.get(0), "First item under root before source root must be Python File");
        assertFalse(labels.contains("Module\u2026"), "Should NOT contain Module... under root");

        // Jupyter Connection is NOT present under root (Image 3)
        assertFalse(labels.contains("Jupyter Connection"), "Should NOT contain Jupyter Connection under root");

        // Multi-language file items (Image 3)
        assertTrue(labels.contains("Jupyter Notebook"), "Should contain Jupyter Notebook");
        assertTrue(labels.contains("Go File"), "Should contain Go File");
        assertTrue(labels.contains("PHP File"), "Should contain PHP File");
        assertTrue(labels.contains("PHP Class"), "Should contain PHP Class");
        assertTrue(labels.contains("File"), "Should contain File");
        assertTrue(labels.contains("Go Modules File"), "Should contain Go Modules File");
        assertTrue(labels.contains("Scratch File"), "Should contain Scratch File");
        assertTrue(labels.contains("Directory"), "Should contain Directory");
        assertTrue(labels.contains("Python Package"), "Should contain Python Package");
        assertTrue(labels.contains("Kotlin Script"), "Should contain Kotlin Script");
        assertTrue(labels.contains("Kotlin Notebook"), "Should contain Kotlin Notebook");
        assertTrue(labels.contains("JavaScript File"), "Should contain JavaScript File");
        assertTrue(labels.contains("TypeScript File"), "Should contain TypeScript File");
        assertTrue(labels.contains("HTML File"), "Should contain HTML File");
        assertTrue(labels.contains("Stylesheet"), "Should contain Stylesheet");
        assertTrue(labels.contains("Dockerfile"), "Should contain Dockerfile");
        assertTrue(labels.contains("Dev Container Config\u2026"), "Should contain Dev Container Config...");
        assertTrue(labels.contains("HTTP Request"), "Should contain HTTP Request");
        assertTrue(labels.contains("OpenAPI Specification"), "Should contain OpenAPI Specification");
        assertTrue(labels.contains("Kubernetes Resource"), "Should contain Kubernetes Resource");
        assertTrue(labels.contains("Helm Chart"), "Should contain Helm Chart");
        assertTrue(labels.contains("ERB File"), "Should contain ERB File");
        assertTrue(labels.contains("composer.json File"), "Should contain composer.json File");
        assertTrue(labels.contains("Resource Bundle"), "Should contain Resource Bundle");
        assertTrue(labels.contains("EditorConfig File"), "Should contain EditorConfig File");
        assertTrue(labels.contains("Data Source in Path"), "Should contain Data Source in Path");
        assertTrue(labels.contains("PHP Test"), "Should contain PHP Test");

        // Must NOT contain Java compilation unit options
        assertFalse(labels.contains("Java Class"), "Context menu under root should not contain Java Class");
        assertFalse(labels.contains("Package"), "Context menu under root should not contain Package");
        assertFalse(labels.contains("FXML File"), "Context menu under root should not contain FXML File");
        assertFalse(labels.contains("JavaFX Application"), "Context menu under root should not contain JavaFX Application");
    }

    @Test
    void testMenuItemSpecsInContextMenuInsideSourceRoot() {
        // Image 1: Right-click in src/main/java/dev/lumina/diagnostics
        Path sourcePath = Path.of("/projects/lumina/src/main/java/dev/lumina/diagnostics");
        List<NewMenuBuilder.ItemSpec> specs = NewMenuBuilder.getMenuItemSpecs(sourcePath, true);

        List<String> labels = extractLabels(specs);

        // Must start directly with compilation unit creations (Image 1)
        assertTrue(labels.contains("Java Class"), "Should contain Java Class");
        assertTrue(labels.contains("Kotlin Class/File"), "Should contain Kotlin Class/File");
        assertTrue(labels.contains("Package"), "Should contain Package");
        assertTrue(labels.contains("File"), "Should contain File");
        assertTrue(labels.contains("FXML File"), "Should contain FXML File");
        assertTrue(labels.contains("JavaFX Application"), "Should contain JavaFX Application");
        assertTrue(labels.contains("package-info.java"), "Should contain package-info.java");
        assertTrue(labels.contains("module-info.java"), "Should contain module-info.java");
        assertTrue(labels.contains("Kotlin Notebook"), "Should contain Kotlin Notebook");
        assertTrue(labels.contains("Resource Bundle"), "Should contain Resource Bundle");

        // Context menu must NOT have top-level Project or Module items (Image 1)
        assertFalse(labels.contains("Project\u2026"), "Context menu should not contain Project...");
        assertFalse(labels.contains("Project from Existing Sources\u2026"), "Context menu should not contain Project from Existing Sources...");
        assertFalse(labels.contains("Project from Version Control\u2026"), "Context menu should not contain Project from VCS");
        assertFalse(labels.contains("Module\u2026"), "Context menu should not contain Module...");
        assertFalse(labels.contains("Module from Existing Sources\u2026"), "Context menu should not contain Module from Existing Sources...");

        // Must NOT include non-source root creations
        assertFalse(labels.contains("Python File"), "Should not contain Python File");
        assertFalse(labels.contains("Go File"), "Should not contain Go File");
        assertFalse(labels.contains("PHP File"), "Should not contain PHP File");
        assertFalse(labels.contains("Dockerfile"), "Should not contain Dockerfile");
        assertFalse(labels.contains("Data Source"), "Should not contain Data Source");
    }

    @Test
    void testMenuItemSpecsWhenNoFolderSelected() {
        // Images 2 & 3: File -> New when no folder is selected
        List<NewMenuBuilder.ItemSpec> specs = NewMenuBuilder.getMenuItemSpecs(null, false);

        List<String> labels = extractLabels(specs);

        // Project / Module creation items (Image 2)
        assertTrue(labels.contains("Project\u2026"), "Should contain Project...");
        assertTrue(labels.contains("Project from Existing Sources\u2026"), "Should contain Project from Existing Sources...");
        assertTrue(labels.contains("Project from Version Control\u2026"), "Should contain Project from VCS");
        assertTrue(labels.contains("Module\u2026"), "Should contain Module...");
        assertTrue(labels.contains("Module from Existing Sources\u2026"), "Should contain Module from Existing Sources...");

        // Scratch File and Data Sources (Image 2)
        assertTrue(labels.contains("Scratch File"), "Should contain Scratch File");
        assertTrue(labels.contains("Data Source"), "Should contain Data Source");
        assertTrue(labels.contains("DDL Data Source"), "Should contain DDL Data Source");
        assertTrue(labels.contains("Data Source from URL"), "Should contain Data Source from URL");
        assertTrue(labels.contains("Data Source from Cloud Provider"), "Should contain Data Source from Cloud Provider");
        assertTrue(labels.contains("Data Source from Path"), "Should contain Data Source from Path");
        assertTrue(labels.contains("Driver"), "Should contain Driver");

        // Must NOT contain file/folder creation items that require a directory
        assertFalse(labels.contains("Java Class"), "Should not contain Java Class");
        assertFalse(labels.contains("Kotlin Class/File"), "Should not contain Kotlin Class/File");
        assertFalse(labels.contains("File"), "Should not contain File");
        assertFalse(labels.contains("Directory"), "Should not contain Directory");
        assertFalse(labels.contains("Package"), "Should not contain Package");
        assertFalse(labels.contains("Python File"), "Should not contain Python File");
        assertFalse(labels.contains("Go File"), "Should not contain Go File");
        assertFalse(labels.contains("HTML File"), "Should not contain HTML File");
        assertFalse(labels.contains("Dockerfile"), "Should not contain Dockerfile");
    }

    @Test
    void testMenuItemSpecsInsideSourceRoot() {
        Path sourcePath = Path.of("/projects/lumina/src/main/java/dev/lumina/diagnostics/FieldDiagnostics.java");
        List<NewMenuBuilder.ItemSpec> specs = NewMenuBuilder.getMenuItemSpecs(sourcePath, false);

        List<String> labels = extractLabels(specs);

        // Top group common to File -> New
        assertTrue(labels.contains("Project\u2026"), "Should contain Project...");
        assertTrue(labels.contains("Module\u2026"), "Should contain Module...");

        // Source root creations
        assertTrue(labels.contains("Java Class"), "Should contain Java Class");
        assertTrue(labels.contains("Kotlin Class/File"), "Should contain Kotlin Class/File");
        assertTrue(labels.contains("Package"), "Should contain Package");
        assertTrue(labels.contains("File"), "Should contain File");
        assertTrue(labels.contains("FXML File"), "Should contain FXML File");
        assertTrue(labels.contains("JavaFX Application"), "Should contain JavaFX Application");
        assertTrue(labels.contains("package-info.java"), "Should contain package-info.java");
        assertTrue(labels.contains("module-info.java"), "Should contain module-info.java");
        assertTrue(labels.contains("Resource Bundle"), "Should contain Resource Bundle");

        // Must NOT include non-source root creations
        assertFalse(labels.contains("Python File"), "Should not contain Python File");
        assertFalse(labels.contains("Go File"), "Should not contain Go File");
        assertFalse(labels.contains("PHP File"), "Should not contain PHP File");
        assertFalse(labels.contains("Dockerfile"), "Should not contain Dockerfile");
        assertFalse(labels.contains("Data Source"), "Should not contain Data Source");
    }

    @Test
    void testMenuItemSpecsAtProjectRoot() {
        Path rootPath = Path.of("/projects/lumina");
        List<NewMenuBuilder.ItemSpec> specs = NewMenuBuilder.getMenuItemSpecs(rootPath, false);

        List<String> labels = extractLabels(specs);

        // Top group common to File -> New
        assertTrue(labels.contains("Project\u2026"), "Should contain Project...");
        assertTrue(labels.contains("Module\u2026"), "Should contain Module...");

        // Must include project-level creations
        assertTrue(labels.contains("Python File"), "Should contain Python File");
        assertTrue(labels.contains("Jupyter Notebook"), "Should contain Jupyter Notebook");
        assertTrue(labels.contains("Go File"), "Should contain Go File");
        assertTrue(labels.contains("PHP File"), "Should contain PHP File");
        assertTrue(labels.contains("File"), "Should contain File");
        assertTrue(labels.contains("Directory"), "Should contain Directory");
        assertTrue(labels.contains("HTML File"), "Should contain HTML File");
        assertTrue(labels.contains("Stylesheet"), "Should contain Stylesheet");
        assertTrue(labels.contains("Dockerfile"), "Should contain Dockerfile");
        assertTrue(labels.contains("Resource Bundle"), "Should contain Resource Bundle");
        assertTrue(labels.contains("Data Source"), "Should contain Data Source");

        // Must NOT include Java source-only compilation unit options
        assertFalse(labels.contains("Java Class"), "Should not contain Java Class");
        assertFalse(labels.contains("Package"), "Should not contain Package");
        assertFalse(labels.contains("FXML File"), "Should not contain FXML File");
        assertFalse(labels.contains("JavaFX Application"), "Should not contain JavaFX Application");
        assertFalse(labels.contains("package-info.java"), "Should not contain package-info.java");
        assertFalse(labels.contains("module-info.java"), "Should not contain module-info.java");
    }

    @Test
    void testPopulateJavaFXMenuWhenAvailable() {
        if (!javaFxAvailable) return;
        Menu menu = new Menu("New");
        Path rootPath = Path.of("/projects/lumina");
        DummyHandlers handlers = new DummyHandlers();

        NewMenuBuilder.populateNewMenu(menu, rootPath, handlers);
        List<String> labels = menu.getItems().stream()
                .map(MenuItem::getText)
                .filter(t -> t != null && !t.isBlank())
                .toList();
        assertTrue(labels.contains("Python File"));
    }

    private static List<String> extractLabels(List<NewMenuBuilder.ItemSpec> specs) {
        List<String> labels = new ArrayList<>();
        for (NewMenuBuilder.ItemSpec spec : specs) {
            if (spec.text() != null && !spec.text().isBlank()) {
                labels.add(spec.text());
            }
        }
        return labels;
    }

    private static class DummyHandlers implements NewMenuBuilder.CreationHandlers {
        @Override public void onNewProject() {}
        @Override public void onNewProjectFromExisting() {}
        @Override public void onNewProjectFromVCS() {}
        @Override public void onNewModule() {}
        @Override public void onNewModuleFromExisting() {}
        @Override public void onNewJavaClass(Path dir) {}
        @Override public void onNewKotlinClass(Path dir) {}
        @Override public void onNewFile(Path dir) {}
        @Override public void onNewPackage(Path dir) {}
        @Override public void onNewDirectory(Path dir) {}
        @Override public void onNewFxml(Path dir) {}
        @Override public void onNewJavaFxApp(Path dir) {}
        @Override public void onNewPackageInfo(Path dir) {}
        @Override public void onNewModuleInfo(Path dir) {}
        @Override public void onNewKotlinNotebook(Path dir) {}
        @Override public void onNewResourceBundle(Path dir) {}
        @Override public void onNewScratchFile(Path dir) {}
        @Override public void onNewSpecificFile(Path dir, String defaultName, String defaultContent) {}
        @Override public void onPlaceholder(String title) {}
    }
}

