package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import dev.lumina.settings.EditorColorSchemeSettings.EffectType;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SettingsColorSchemeLanguageDefaultsPageTest {

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
                SettingsColorSchemeLanguageDefaultsPage page = new SettingsColorSchemeLanguageDefaultsPage();
                assertFalse(page.isModified());

                assertEquals("Islands Dark Theme default", page.getHeaderBar().getSchemeCombo().getValue());
                assertNotNull(page.getCategoryTree().getRoot());
                assertFalse(page.getCategoryTree().getRoot().getChildren().isEmpty());

                // Verify root items match the 13 categories from Screenshot 1
                assertEquals(13, page.getCategoryTree().getRoot().getChildren().size());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testScreenshots2Through5CategoriesAndAttributes() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeLanguageDefaultsPage page = new SettingsColorSchemeLanguageDefaultsPage();

                // 1. Screenshot 2: Bad character
                page.selectTreeItem("Bad character");
                assertTrue(page.getAttributeEditorBox().isVisible());
                assertFalse(page.getBoldCheck().isSelected());
                assertFalse(page.getItalicCheck().isSelected());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("F75464", page.getForegroundSwatch().getText());
                assertFalse(page.getBackgroundCheck().isSelected());
                assertFalse(page.getErrorStripeCheck().isSelected());
                assertFalse(page.getEffectsCheck().isSelected());
                assertEquals(EffectType.UNDERWAVED, page.getEffectTypeCombo().getValue());
                assertFalse(page.getInheritBox().isVisible());

                // 2. Screenshot 3: Braces and Operators // Brackets
                page.selectTreeItem("Braces and Operators // Brackets");
                assertTrue(page.getAttributeEditorBox().isVisible());
                assertFalse(page.getBoldCheck().isSelected());
                assertFalse(page.getItalicCheck().isSelected());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("BCBEC4", page.getForegroundSwatch().getText());
                assertFalse(page.getBackgroundCheck().isSelected());
                assertFalse(page.getErrorStripeCheck().isSelected());
                assertFalse(page.getEffectsCheck().isSelected());
                assertEquals(EffectType.BORDERED, page.getEffectTypeCombo().getValue());
                assertFalse(page.getInheritBox().isVisible());

                // 3. Screenshot 4: Classes // Class reference
                page.selectTreeItem("Classes // Class reference");
                assertTrue(page.getAttributeEditorBox().isVisible());
                assertFalse(page.getBoldCheck().isSelected());
                assertFalse(page.getItalicCheck().isSelected());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("BCBEC4", page.getForegroundSwatch().getText());
                assertFalse(page.getForegroundCheck().isDisabled());
                assertTrue(page.getInheritBox().isVisible());
                assertFalse(page.getInheritCheck().isSelected()); // Unchecked in Screenshot 4!
                assertEquals("Identifiers->Default", page.getInheritLink().getText());

                // 4. Screenshot 5: Comments // Doc comment // Link in rendered view
                page.selectTreeItem("Comments // Doc comment // Link in rendered view");
                assertTrue(page.getAttributeEditorBox().isVisible());
                assertFalse(page.getBoldCheck().isSelected());
                assertFalse(page.getItalicCheck().isSelected());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("3887A1", page.getForegroundSwatch().getText());
                assertFalse(page.getBackgroundCheck().isSelected());
                assertFalse(page.getErrorStripeCheck().isSelected());
                assertFalse(page.getEffectsCheck().isSelected());
                assertEquals(EffectType.UNDERSCORED, page.getEffectTypeCombo().getValue());
                assertFalse(page.getInheritBox().isVisible());

            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testCategoryGroupSelectionHidesAttributeEditor() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeLanguageDefaultsPage page = new SettingsColorSchemeLanguageDefaultsPage();

                // Select group "Braces and Operators"
                page.selectTreeItem("Braces and Operators");
                assertFalse(page.getAttributeEditorBox().isVisible());

                // Select group "Classes"
                page.selectTreeItem("Classes");
                assertFalse(page.getAttributeEditorBox().isVisible());

                // Select group "Comments"
                page.selectTreeItem("Comments");
                assertFalse(page.getAttributeEditorBox().isVisible());

                // Select subgroup "Comments // Doc comment"
                page.selectTreeItem("Comments // Doc comment");
                assertFalse(page.getAttributeEditorBox().isVisible());

                // Select leaf again
                page.selectTreeItem("Keyword");
                assertTrue(page.getAttributeEditorBox().isVisible());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testInheritanceToggleAndNavigation() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeLanguageDefaultsPage page = new SettingsColorSchemeLanguageDefaultsPage();
                page.selectTreeItem("Classes // Class reference");

                assertFalse(page.getInheritCheck().isSelected());
                assertFalse(page.getForegroundCheck().isDisabled());

                // Toggle inherit check on
                page.getInheritCheck().setSelected(true);
                assertTrue(page.getInheritCheck().isSelected());
                assertTrue(page.getForegroundCheck().isDisabled());

                // Click inherit link to navigate to Identifiers // Default
                page.getInheritLink().fire();
                assertEquals("Default", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertTrue(page.getAttributeEditorBox().isVisible());
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
                SettingsColorSchemeLanguageDefaultsPage page = new SettingsColorSchemeLanguageDefaultsPage();
                page.selectTreeItem("Keyword");

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
                SettingsColorSchemeLanguageDefaultsPage page = new SettingsColorSchemeLanguageDefaultsPage();
                page.selectTreeItem("Number");
                page.getBoldCheck().setSelected(true);
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());

                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Number").isBold());

                // Restore defaults
                s.initDefaults();
                s.save();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
