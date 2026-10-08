package dev.lumina.ui;

import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ChooseExcludeAnnotationDialog and its integration.
 */
public class ChooseExcludeAnnotationDialogTest {

    private static boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> javaFxAvailable = true);
            javaFxAvailable = true;
        } catch (IllegalStateException e) {
            javaFxAvailable = true;
        } catch (Throwable t) {
            javaFxAvailable = false;
        }
    }

    private void runOnFx(Runnable action) {
        if (!javaFxAvailable) return;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "Timed out waiting for JavaFX thread");
            if (error.get() != null) {
                if (error.get() instanceof AssertionError ae) throw ae;
                fail(error.get());
            }
        } catch (InterruptedException e) {
            fail(e);
        }
    }

    @Test
    void testDialogInitializationAndCatalog() {
        runOnFx(() -> {
            ChooseExcludeAnnotationDialog dialog = new ChooseExcludeAnnotationDialog();
            assertEquals("Choose Exclude Annotation", dialog.getTitle());

            // Check catalog contains key annotations from reference design
            List<String> names = dialog.getAllAnnotations().stream()
                    .map(ChooseExcludeAnnotationDialog.AnnotationItem::getSimpleName)
                    .toList();

            assertTrue(names.contains("A"), "Must contain A");
            assertTrue(names.contains("Acceleration"), "Must contain Acceleration");
            assertTrue(names.contains("AccessibleLateinitPropertyLiteral"), "Must contain AccessibleLateinitPropertyLiteral");
            assertTrue(names.contains("AfterAll"), "Must contain AfterAll");
            assertTrue(names.contains("AfterEach"), "Must contain AfterEach");
            assertTrue(names.contains("AggregateWith"), "Must contain AggregateWith");
            assertTrue(names.contains("AllFieldsConstructor"), "Must contain AllFieldsConstructor");
            assertTrue(names.contains("AllowConcurrentEvents"), "Must contain AllowConcurrentEvents");
            assertTrue(names.contains("AlwaysSafe"), "Must contain AlwaysSafe");
            assertTrue(names.contains("Angle"), "Must contain Angle");
            assertTrue(names.contains("AnnotatedFor"), "Must contain AnnotatedFor");
            assertTrue(names.contains("Generated"), "Must contain Generated");
            assertTrue(names.contains("Test"), "Must contain Test");

            // Verify status text
            assertEquals("No matches found in project", dialog.getMatchesLabel().getText());
        });
    }

    @Test
    void testFiltering() {
        runOnFx(() -> {
            ChooseExcludeAnnotationDialog dialog = new ChooseExcludeAnnotationDialog();

            // Filter for 'After'
            dialog.filter("After");
            List<String> filteredNames = dialog.getFilteredAnnotations().stream()
                    .map(ChooseExcludeAnnotationDialog.AnnotationItem::getSimpleName)
                    .toList();

            assertTrue(filteredNames.contains("AfterAll"));
            assertTrue(filteredNames.contains("AfterEach"));
            assertFalse(filteredNames.contains("Acceleration"));

            // Filter with wildcard '*Gen*'
            dialog.filter("*Gen*");
            List<String> wildcardNames = dialog.getFilteredAnnotations().stream()
                    .map(ChooseExcludeAnnotationDialog.AnnotationItem::getSimpleName)
                    .toList();
            assertTrue(wildcardNames.contains("Generated"));

            // Empty filter restores list
            dialog.filter("");
            assertEquals(dialog.getAllAnnotations().size(), dialog.getFilteredAnnotations().size());
        });
    }

    @Test
    void testTabSwitching() {
        runOnFx(() -> {
            ChooseExcludeAnnotationDialog dialog = new ChooseExcludeAnnotationDialog();

            // Switch to project tab
            dialog.switchToProjectTab();
            assertNotNull(dialog.getProjectTreeView().getRoot());
            assertEquals(3, dialog.getProjectTreeView().getRoot().getChildren().size());

            TreeItem<String> projItem = dialog.getProjectTreeView().getRoot().getChildren().get(0);
            TreeItem<String> extLibItem = dialog.getProjectTreeView().getRoot().getChildren().get(1);
            TreeItem<String> scratchItem = dialog.getProjectTreeView().getRoot().getChildren().get(2);

            assertTrue(projItem.getValue().contains("lumina"));
            assertEquals("External Libraries", extLibItem.getValue());
            assertEquals("Scratches and Consoles", scratchItem.getValue());

            // Switch back to search tab
            dialog.switchToSearchTab();
            assertNotNull(dialog.getSearchField());
        });
    }

    @Test
    void testBrandIsolation() {
        List<String> files = List.of(
                "src/main/java/dev/lumina/ui/ChooseExcludeAnnotationDialog.java",
                "src/main/java/dev/lumina/ui/SettingsAnnotationProcessorsPage.java",
                "src/main/java/dev/lumina/ui/SettingsScalaCompilerPage.java",
                "src/main/java/dev/lumina/ui/SettingsCoveragePage.java"
        );
        for (String f : files) {
            File file = new File(f);
            assertTrue(file.exists(), f + " must exist");
            try {
                String content = Files.readString(file.toPath());
                assertFalse(content.contains("IntelliJ"), "File " + f + " must not contain IntelliJ");
                assertFalse(content.contains("JetBrains"), "File " + f + " must not contain JetBrains");
            } catch (Exception e) {
                fail(e);
            }
        }
    }
}
