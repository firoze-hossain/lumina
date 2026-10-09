package dev.lumina.ui;

import javafx.application.Platform;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SettingsLanguagesOverviewAndTreeTest {

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
    void testLanguagesAndFrameworksTreeStructure() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDialog dialog = new SettingsDialog(null);
                assertNotNull(dialog);

                // Locate "Languages & Frameworks" node
                TreeItem<String> languagesNode = null;
                for (TreeItem<String> child : dialog.getTree().getRoot().getChildren()) {
                    if ("Languages & Frameworks".equals(child.getValue())) {
                        languagesNode = child;
                        break;
                    }
                }
                assertNotNull(languagesNode, "Languages & Frameworks node must exist in the root");

                // Verify child nodes in exact order matching Screenshot 1
                List<String> expectedChildren = List.of(
                        "PHP",
                        "Rust",
                        "Python Template Languages",
                        "Go",
                        "JavaFX",
                        "JavaScript",
                        "JavaScript Runtime",
                        "JVM Logging",
                        "Kotlin",
                        "Ktor",
                        "Kubernetes",
                        "Lombok",
                        "Markdown",
                        "Micronaut",
                        "OpenAPI Specifications",
                        "Play",
                        "Protocol Buffers",
                        "Quarkus",
                        "RBS",
                        "Scala",
                        "Schemas and DTDs",
                        "Spring",
                        "SQL Dialects",
                        "SQL Resolution Scopes",
                        "Style Sheets",
                        "Tables",
                        "Template Data Languages",
                        "TypeScript",
                        "Web Contexts",
                        "XSLT",
                        "XSLT File Associations"
                );

                List<String> actualChildren = languagesNode.getChildren().stream()
                        .map(TreeItem::getValue)
                        .toList();

                assertEquals(expectedChildren, actualChildren, "Languages & Frameworks children must match Screenshot 1");

                // Test Category Overview page generated
                SettingsCategoryOverviewPage overview = new SettingsCategoryOverviewPage(languagesNode, null);
                assertNotNull(overview);
                // Child links match all items
                long linkCount = overview.getChildren().stream()
                        .filter(node -> node instanceof javafx.scene.layout.VBox)
                        .map(node -> (javafx.scene.layout.VBox) node)
                        .flatMap(vbox -> vbox.getChildren().stream())
                        .filter(n -> n instanceof Hyperlink)
                        .count();
                assertEquals(expectedChildren.size(), linkCount);

                // Test navigation to PHP under Languages & Frameworks
                TreeItem<String> phpNode = languagesNode.getChildren().get(0);
                assertEquals("PHP", phpNode.getValue());

                dialog.getTree().getSelectionModel().select(phpNode);
                assertNotNull(dialog.getCurrentLanguagesPhpPage());
                assertEquals("5.6 (variadic functions, argument unpacking)", dialog.getCurrentLanguagesPhpPage().getLanguageLevelCombo().getValue());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
