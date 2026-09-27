package dev.lumina.ui;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SettingsColorSchemeTreeViewStylingTest {

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
            javaFxAvailable = true;
        } catch (Throwable t) {
            javaFxAvailable = false;
        }
    }

    @Test
    void testCssContainsDarkRulesForTreeViewAndSearch() throws IOException {
        Path cssPath = Path.of("src/main/resources/css/lumina-dark.css");
        assertTrue(Files.exists(cssPath), "lumina-dark.css must exist");

        String content = Files.readString(cssPath);

        // Check treeview rules
        assertTrue(content.contains(".color-scheme-tree"), "CSS must contain .color-scheme-tree");
        assertTrue(content.contains(".settings-tree-view"), "CSS must contain .settings-tree-view");
        assertTrue(content.contains("-fx-control-inner-background: #2B2D30;"), "CSS must define dark control inner background");
        assertTrue(content.contains("-fx-selection-bar: #2E436E;"), "CSS must define selection bar");
        assertTrue(content.contains(".settings-search-field"), "CSS must style .settings-search-field");
    }

    @Test
    void testTenPagesTreeViewDarkStyling() throws Exception {
        if (!javaFxAvailable) {
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                // 1. Micronaut EL
                SettingsColorSchemeMicronautELPage p1 = new SettingsColorSchemeMicronautELPage();
                assertNotNull(p1.getCategoryTree().getCellFactory(), "Micronaut EL tree must have cell factory");
                assertTrue(p1.getCategoryTree().getStyleClass().contains("color-scheme-tree"));
                assertTrue(p1.getCategoryTree().getStyle().contains("-fx-control-inner-background: #2B2D30"));

                // 2. MongoDB JSON
                SettingsColorSchemeMongoDBJSONPage p2 = new SettingsColorSchemeMongoDBJSONPage();
                assertNotNull(p2.getCategoryTree().getCellFactory(), "MongoDB JSON tree must have cell factory");
                assertTrue(p2.getCategoryTree().getStyleClass().contains("color-scheme-tree"));
                assertTrue(p2.getCategoryTree().getStyle().contains("-fx-control-inner-background: #2B2D30"));

                // 3. PHP
                SettingsColorSchemePHPPage p3 = new SettingsColorSchemePHPPage();
                assertNotNull(p3.getCategoryTree().getCellFactory(), "PHP tree must have cell factory");
                assertTrue(p3.getCategoryTree().getStyleClass().contains("color-scheme-tree"));
                assertTrue(p3.getCategoryTree().getStyle().contains("-fx-control-inner-background: #2B2D30"));

                // 4. plan9_x86
                SettingsColorSchemePlan9X86Page p4 = new SettingsColorSchemePlan9X86Page();
                assertNotNull(p4.getCategoryTree().getCellFactory(), "plan9_x86 tree must have cell factory");
                assertTrue(p4.getCategoryTree().getStyleClass().contains("color-scheme-tree"));
                assertTrue(p4.getCategoryTree().getStyle().contains("-fx-control-inner-background: #2B2D30"));

                // 5. PostCSS
                SettingsColorSchemePostCSSPage p5 = new SettingsColorSchemePostCSSPage();
                assertNotNull(p5.getCategoryTree().getCellFactory(), "PostCSS tree must have cell factory");
                assertTrue(p5.getCategoryTree().getStyleClass().contains("color-scheme-tree"));
                assertTrue(p5.getCategoryTree().getStyle().contains("-fx-control-inner-background: #2B2D30"));

                // 6. Properties
                SettingsColorSchemePropertiesPage p6 = new SettingsColorSchemePropertiesPage();
                assertNotNull(p6.getCategoryTree().getCellFactory(), "Properties tree must have cell factory");
                assertTrue(p6.getCategoryTree().getStyleClass().contains("color-scheme-tree"));
                assertTrue(p6.getCategoryTree().getStyle().contains("-fx-control-inner-background: #2B2D30"));

                // 7. Protocol Buffer
                SettingsColorSchemeProtocolBufferPage p7 = new SettingsColorSchemeProtocolBufferPage();
                assertNotNull(p7.getCategoryTree().getCellFactory(), "Protocol Buffer tree must have cell factory");
                assertTrue(p7.getCategoryTree().getStyleClass().contains("color-scheme-tree"));
                assertTrue(p7.getCategoryTree().getStyle().contains("-fx-control-inner-background: #2B2D30"));

                // 8. Protocol Buffer Text
                SettingsColorSchemeProtocolBufferTextPage p8 = new SettingsColorSchemeProtocolBufferTextPage();
                assertNotNull(p8.getCategoryTree().getCellFactory(), "Protocol Buffer Text tree must have cell factory");
                assertTrue(p8.getCategoryTree().getStyleClass().contains("color-scheme-tree"));
                assertTrue(p8.getCategoryTree().getStyle().contains("-fx-control-inner-background: #2B2D30"));

                // 9. Python
                SettingsColorSchemePythonPage p9 = new SettingsColorSchemePythonPage();
                assertNotNull(p9.getCategoryTree().getCellFactory(), "Python tree must have cell factory");
                assertTrue(p9.getCategoryTree().getStyleClass().contains("color-scheme-tree"));
                assertTrue(p9.getCategoryTree().getStyle().contains("-fx-control-inner-background: #2B2D30"));

                // 10. Qute
                SettingsColorSchemeQutePage p10 = new SettingsColorSchemeQutePage();
                assertNotNull(p10.getCategoryTree().getCellFactory(), "Qute tree must have cell factory");
                assertTrue(p10.getCategoryTree().getStyleClass().contains("color-scheme-tree"));
                assertTrue(p10.getCategoryTree().getStyle().contains("-fx-control-inner-background: #2B2D30"));
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Timeout waiting for JavaFX thread");
        if (err.get() != null) {
            throw new AssertionError(err.get());
        }
    }
}
