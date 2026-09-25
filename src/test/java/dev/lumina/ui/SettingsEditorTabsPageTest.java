package dev.lumina.ui;

import dev.lumina.settings.EditorTabsSettings;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SettingsEditorTabsPageTest {

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
        EditorTabsSettings.getInstance().initDefaults();
    }

    @Test
    void testPageInstantiationAndDefaults() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<SettingsEditorTabsPage> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                ref.set(new SettingsEditorTabsPage());
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        SettingsEditorTabsPage page = ref.get();
        assertNotNull(page);
        assertFalse(page.isModified());

        // Verify Appearance defaults
        CheckBox fileIcon = findCheckBox(page, "Show file icon");
        assertNotNull(fileIcon);
        assertTrue(fileIcon.isSelected());

        CheckBox fileExt = findCheckBox(page, "Show file extension");
        assertNotNull(fileExt);
        assertTrue(fileExt.isSelected());

        CheckBox markModified = findCheckBox(page, "Mark modified (*)");
        assertNotNull(markModified);
        assertFalse(markModified.isSelected());

        CheckBox fullPathHover = findCheckBox(page, "Show full path on mouse hover");
        assertNotNull(fullPathHover);
        assertTrue(fullPathHover.isSelected());

        // Verify Tab Order defaults
        CheckBox sortAlpha = findCheckBox(page, "Sort tabs alphabetically");
        assertNotNull(sortAlpha);
        assertFalse(sortAlpha.isSelected());

        CheckBox openAtEnd = findCheckBox(page, "Open new tabs at the end");
        assertNotNull(openAtEnd);
        assertFalse(openAtEnd.isSelected());

        // Verify Opening Policy defaults
        CheckBox previewTab = findCheckBox(page, "Enable preview tab");
        assertNotNull(previewTab);
        assertFalse(previewTab.isSelected());

        // Verify Database defaults
        CheckBox qualifiedDb = findCheckBox(page, "Always show qualified names for database objects in tab titles");
        assertNotNull(qualifiedDb);
        assertFalse(qualifiedDb.isSelected());

        CheckBox shortenDb = findCheckBox(page, "Shorten datasource and object names in tab titles");
        assertNotNull(shortenDb);
        assertTrue(shortenDb.isSelected());
    }

    @Test
    void testModificationDetectionAndReset() throws Exception {
        if (!javaFxAvailable) return;

        AtomicBoolean modifiedFired = new AtomicBoolean(false);
        AtomicReference<SettingsEditorTabsPage> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                SettingsEditorTabsPage page = new SettingsEditorTabsPage();
                ref.set(page);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                CheckBox markModified = findCheckBox(page, "Mark modified (*)");
                assertNotNull(markModified);
                markModified.setSelected(true);

                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.reset();
                assertFalse(page.isModified());
                assertFalse(markModified.isSelected());
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
                SettingsEditorTabsPage page = new SettingsEditorTabsPage();
                CheckBox markModified = findCheckBox(page, "Mark modified (*)");
                assertNotNull(markModified);

                markModified.setSelected(true);
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());
                assertTrue(EditorTabsSettings.getInstance().isMarkModified());

                // Restore
                markModified.setSelected(false);
                page.apply();
                assertFalse(EditorTabsSettings.getInstance().isMarkModified());
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
