package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SettingsColorSchemeGeneralPageTest {

    private static volatile boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            String display = System.getenv("DISPLAY");
            if (display == null || display.isBlank() || java.awt.GraphicsEnvironment.isHeadless()) {
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
        EditorColorSchemeSettings.getInstance().initDefaults();
    }

    @Test
    void testInitialStateMatchesDefaults() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeGeneralPage page = new SettingsColorSchemeGeneralPage();
                assertFalse(page.isModified());

                assertEquals("Islands Dark Theme default", page.getHeaderBar().getSchemeCombo().getValue());
                assertNotNull(page.getCategoryTree().getRoot());
                assertFalse(page.getCategoryTree().getRoot().getChildren().isEmpty());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSelectTreeItemAndAttributeEditor() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeGeneralPage page = new SettingsColorSchemeGeneralPage();

                // Select Code // Identifier under caret (Image 2)
                page.selectTreeItem("Code // Identifier under caret");
                assertTrue(page.getAttributeEditorBox().isVisible());
                assertFalse(page.getForegroundCheck().isSelected());
                assertTrue(page.getBackgroundCheck().isSelected());
                assertEquals("373B39", page.getBackgroundSwatch().getText());
                assertTrue(page.getErrorStripeCheck().isSelected());
                assertEquals("5B786A", page.getErrorStripeSwatch().getText());

                // Select Code // Line number on caret row (Image 3)
                page.selectTreeItem("Code // Line number on caret row");
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("A1A3AB", page.getForegroundSwatch().getText());
                assertFalse(page.getBackgroundCheck().isSelected());

                // Select Editor // Bookmarks (Image 4)
                page.selectTreeItem("Editor // Bookmarks");
                assertFalse(page.getForegroundCheck().isSelected());
                assertFalse(page.getBackgroundCheck().isSelected());
                assertTrue(page.getErrorStripeCheck().isSelected());
                assertEquals("F7E9C6", page.getErrorStripeSwatch().getText());

                // Select Editor // Breadcrumbs // Current (Image 5)
                page.selectTreeItem("Editor // Breadcrumbs // Current");
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("DFE1E5", page.getForegroundSwatch().getText());
                assertTrue(page.getBackgroundCheck().isSelected());
                assertEquals("2B2D30", page.getBackgroundSwatch().getText());

                // Select Editor // Guides // Hard wrap guide (media_1790343452222.png)
                page.selectTreeItem("Editor // Guides // Hard wrap guide");
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("323438", page.getForegroundSwatch().getText());
                assertFalse(page.getBackgroundCheck().isSelected());

                // Select Editor // Sticky Lines // Background (media_1790343472376.png)
                page.selectTreeItem("Editor // Sticky Lines // Background");
                assertFalse(page.getForegroundCheck().isSelected());
                assertFalse(page.getBackgroundCheck().isSelected());
                assertFalse(page.getInheritBox().isVisible());

                // Select Editor // Sticky Lines // Border (media_1790343485724.png)
                page.selectTreeItem("Editor // Sticky Lines // Border");
                assertTrue(page.getInheritBox().isVisible());
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Editor->Guides->Hard wrap guide", page.getInheritLink().getText());
                assertTrue(page.getBackgroundCheck().isSelected());
                assertEquals("323438", page.getBackgroundSwatch().getText());

                // Select Editor // Sticky Lines // Hovered (media_1790343496020.png)
                page.selectTreeItem("Editor // Sticky Lines // Hovered");
                assertTrue(page.getInheritBox().isVisible());
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Editor->Caret row", page.getInheritLink().getText());
                assertTrue(page.getBackgroundCheck().isSelected());
                assertEquals("1F2024", page.getBackgroundSwatch().getText());

                // Select Editor // Tabs // Modified icon color (media_1790343516571.png)
                page.selectTreeItem("Editor // Tabs // Modified icon color");
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("4083C9", page.getForegroundSwatch().getText());
                assertFalse(page.getBackgroundCheck().isSelected());

                // Select Editor // Vertical Scrollbar // Thumb while scrolling (media_1790344007565.png)
                page.selectTreeItem("Editor // Vertical Scrollbar // Thumb while scrolling");
                assertFalse(page.getForegroundCheck().isSelected());
                assertTrue(page.getBackgroundCheck().isSelected());
                assertEquals("A6A6A6", page.getBackgroundSwatch().getText());

                // Select Editor // Notification background (media_1790344023886.png)
                page.selectTreeItem("Editor // Notification background");
                assertFalse(page.getForegroundCheck().isSelected());
                assertTrue(page.getBackgroundCheck().isSelected());
                assertEquals("25324D", page.getBackgroundSwatch().getText());

                // Select Editor // Gutter background (media_1790344034136.png)
                page.selectTreeItem("Editor // Gutter background");
                assertFalse(page.getForegroundCheck().isSelected());
                assertFalse(page.getBackgroundCheck().isSelected());

                // Select Editor // Selection background (media_1790344041477.png)
                page.selectTreeItem("Editor // Selection background");
                assertFalse(page.getForegroundCheck().isSelected());
                assertTrue(page.getBackgroundCheck().isSelected());
                assertEquals("214283", page.getBackgroundSwatch().getText());

                // Select Editor // Selection foreground (media_1790344050573.png)
                page.selectTreeItem("Editor // Selection foreground");
                assertFalse(page.getForegroundCheck().isSelected());
                assertFalse(page.getBackgroundCheck().isSelected());

                // Select Errors and Warnings // Deprecated symbol marked for removal (media_1790345048357.png)
                page.selectTreeItem("Errors and Warnings // Deprecated symbol marked for removal");
                assertFalse(page.getForegroundCheck().isSelected());
                assertFalse(page.getBackgroundCheck().isSelected());
                assertTrue(page.getEffectsCheck().isSelected());
                assertEquals("F75464", page.getEffectsSwatch().getText());
                assertEquals(dev.lumina.settings.EditorColorSchemeSettings.EffectType.STRIKEOUT, page.getEffectTypeCombo().getValue());

                // Select Errors and Warnings // Duplicate from server (media_1790345057788.png)
                page.selectTreeItem("Errors and Warnings // Duplicate from server");
                assertFalse(page.getForegroundCheck().isSelected());
                assertTrue(page.getBackgroundCheck().isSelected());
                assertEquals("5E5339", page.getBackgroundSwatch().getText());

                // Select Errors and Warnings // Warning (media_1790345066960.png)
                page.selectTreeItem("Errors and Warnings // Warning");
                assertFalse(page.getForegroundCheck().isSelected());
                assertFalse(page.getBackgroundCheck().isSelected());
                assertTrue(page.getErrorStripeCheck().isSelected());
                assertEquals("C29E4A", page.getErrorStripeSwatch().getText());
                assertTrue(page.getEffectsCheck().isSelected());
                assertEquals("F2C55C", page.getEffectsSwatch().getText());
                assertEquals(dev.lumina.settings.EditorColorSchemeSettings.EffectType.UNDERWAVED, page.getEffectTypeCombo().getValue());

                // Select Errors and Warnings // Grammar error (media_1790345076709.png)
                page.selectTreeItem("Errors and Warnings // Grammar error");
                assertFalse(page.getForegroundCheck().isSelected());
                assertFalse(page.getBackgroundCheck().isSelected());
                assertTrue(page.getEffectsCheck().isSelected());
                assertEquals("713D40", page.getEffectsSwatch().getText());
                assertEquals(dev.lumina.settings.EditorColorSchemeSettings.EffectType.UNDERWAVED, page.getEffectTypeCombo().getValue());

                // Test clicking inherit link navigates to target item
                page.selectTreeItem("Editor // Sticky Lines // Border");
                page.getInheritLink().fire();
                assertEquals("Editor // Guides // Hard wrap guide", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue().equals("Hard wrap guide") ? "Editor // Guides // Hard wrap guide" : "failed");
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSelectTopLevelGroupHidesAttributeEditor() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeGeneralPage page = new SettingsColorSchemeGeneralPage();
                // Select top-level Code group (Image 1)
                page.getCategoryTree().getSelectionModel().select(page.getCategoryTree().getRoot().getChildren().get(0));
                assertFalse(page.getAttributeEditorBox().isVisible());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testDirtyTrackingAndReset() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeGeneralPage page = new SettingsColorSchemeGeneralPage();
                page.selectTreeItem("Code // Identifier under caret");

                AtomicBoolean modifiedNotified = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedNotified.set(true));

                assertFalse(page.isModified());

                // Modify bold
                page.getBoldCheck().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedNotified.get());

                // Reset back
                page.reset();
                assertFalse(page.isModified());
                assertFalse(page.getBoldCheck().isSelected());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testApplySaves() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeGeneralPage page = new SettingsColorSchemeGeneralPage();
                page.selectTreeItem("Code // Line number on caret row");
                page.getBoldCheck().setSelected(true);
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());

                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Code // Line number on caret row").isBold());

                // Restore
                s.initDefaults();
                s.save();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
