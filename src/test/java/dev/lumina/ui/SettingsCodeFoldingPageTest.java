package dev.lumina.ui;

import dev.lumina.folding.CodeFoldingSettings;
import dev.lumina.folding.CodeFoldingSettings.FoldingArrowsMode;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SettingsCodeFoldingPageTest {

    private static volatile boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            String display = System.getenv("DISPLAY");
            if (display == null || display.isBlank() || GraphicsEnvironment.isHeadless()) {
                return;
            }
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(() -> {
                javaFxAvailable = true;
                latch.countDown();
            });
            latch.await(3, TimeUnit.SECONDS);
        } catch (Throwable ignored) {
            javaFxAvailable = false;
        }
    }

    @BeforeEach
    void setUp() {
        CodeFoldingSettings.getInstance().initDefaults();
    }

    @Test
    void testPageInstantiationAndDefaults() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<SettingsCodeFoldingPage> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                ref.set(new SettingsCodeFoldingPage());
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        SettingsCodeFoldingPage page = ref.get();
        assertNotNull(page);
        assertFalse(page.isModified());
        assertTrue(page.getChildren().size() > 15);
    }

    @Test
    void testModificationDetectionAndReset() throws Exception {
        if (!javaFxAvailable) return;

        AtomicBoolean modifiedFired = new AtomicBoolean(false);
        AtomicReference<SettingsCodeFoldingPage> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                SettingsCodeFoldingPage page = new SettingsCodeFoldingPage();
                ref.set(page);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                CheckBox bottomArrowsCheck = findCheckBox(page, "Show bottom arrows");
                assertNotNull(bottomArrowsCheck, "Bottom arrows checkbox should exist");
                assertFalse(bottomArrowsCheck.isSelected());

                bottomArrowsCheck.setSelected(true);
                assertTrue(page.isModified(), "Page should be marked modified after toggling checkbox");
                assertTrue(modifiedFired.get(), "onModifiedListener should have fired");

                page.reset();
                assertFalse(page.isModified(), "Page should not be modified after reset");
                assertFalse(bottomArrowsCheck.isSelected(), "Bottom arrows checkbox should be restored to false");
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testApplySavesSettings() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                SettingsCodeFoldingPage page = new SettingsCodeFoldingPage();
                CheckBox methodBodiesCheck = findCheckBox(page, "Method bodies");
                assertNotNull(methodBodiesCheck);
                assertFalse(methodBodiesCheck.isSelected());

                methodBodiesCheck.setSelected(true);
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());
                assertTrue(CodeFoldingSettings.getInstance().isFoldMethodBodies());

                // Restore
                methodBodiesCheck.setSelected(false);
                page.apply();
                assertFalse(CodeFoldingSettings.getInstance().isFoldMethodBodies());
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testAllSixteenGroupsPresent() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                SettingsCodeFoldingPage page = new SettingsCodeFoldingPage();

                assertNotNull(findCheckBox(page, "Show code folding arrows"));
                assertNotNull(findCheckBox(page, "Show bottom arrows"));
                assertNotNull(findCheckBox(page, "File header"));
                assertNotNull(findCheckBox(page, "Imports"));
                assertNotNull(findCheckBox(page, "Queries"));
                assertNotNull(findCheckBox(page, "Show key count in folded JSON"));
                assertNotNull(findCheckBox(page, "One-line methods"));
                assertNotNull(findCheckBox(page, "One-line functions in JavaScript and TypeScript"));
                assertNotNull(findCheckBox(page, "Value references in Helm templates"));
                assertNotNull(findCheckBox(page, "Collapse front matter"));
                assertNotNull(findCheckBox(page, "Class body"));
                assertNotNull(findCheckBox(page, "Long string literals"));
                assertNotNull(findCheckBox(page, "I18n strings"));
                assertNotNull(findCheckBox(page, "Put underscores inside numeric literals (6-digit or longer)"));
                assertNotNull(findCheckBox(page, "Block comments"));
                assertNotNull(findCheckBox(page, "XML tags"));
                assertNotNull(findCheckBox(page, "Limit folded keys and values to"));
                assertNotNull(findCheckBox(page, "One-line if error handling"));
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    private static CheckBox findCheckBox(Node root, String text) {
        if (root instanceof CheckBox cb) {
            if (text.equals(cb.getText())) {
                return cb;
            }
        }
        if (root instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                CheckBox found = findCheckBox(child, text);
                if (found != null) return found;
            }
        }
        return null;
    }
}
