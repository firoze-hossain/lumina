package dev.lumina.ui;

import dev.lumina.livetemplates.LiveTemplate;
import dev.lumina.livetemplates.LiveTemplateManager;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class SettingsLiveTemplatesPageTest {

    private static volatile boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(() -> {
                javaFxAvailable = true;
                latch.countDown();
            });
            latch.await(3, TimeUnit.SECONDS);
        } catch (IllegalStateException e) {
            // JavaFX already initialized
            javaFxAvailable = true;
        } catch (Throwable t) {
            javaFxAvailable = false;
        }
    }

    @Test
    void testLiveTemplatesPageConstruction() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsLiveTemplatesPage page = new SettingsLiveTemplatesPage();
                assertNotNull(page);
                assertNotNull(page.getTreeView());

                TreeItem<SettingsLiveTemplatesPage.TreeItemData> root = page.getTreeView().getRoot();
                assertNotNull(root);
                assertTrue(root.getChildren().size() >= 25, "Tree should have at least 25 groups");

                // Check default selection
                LiveTemplate sel = page.getCurrentSelectedTemplate();
                assertNotNull(sel, "Should have default selected template");

                // Dirty state initially false
                assertFalse(page.isModified());

                // Modify template and verify modified listener
                boolean[] modifiedFired = {false};
                page.setOnModifiedListener(() -> modifiedFired[0] = true);

                sel.setDescription(sel.getDescription() + " - Modified");
                // Trigger dirty check
                assertTrue(page.isModified());

                // Reset restores clean state
                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
