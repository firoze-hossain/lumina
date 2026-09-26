package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import dev.lumina.settings.EditorColorSchemeSettings.ColorAttribute;
import dev.lumina.settings.EditorColorSchemeSettings.EffectType;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SettingsColorSchemeFiveLanguagesTest {

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

    @BeforeEach
    void setUp() {
        EditorColorSchemeSettings.getInstance().initDefaults();
    }

    // -------------------------------------------------------------------------
    // 1. Gradle Declarative Configuration Tests (media_1790432310309.png)
    // -------------------------------------------------------------------------

    @Test
    void testGradleDeclarativePageInitAndDefaults() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeGradleDeclarativePage page = new SettingsColorSchemeGradleDeclarativePage();

                // 8 items in tree
                TreeItem<String> root = page.getCategoryTree().getRoot();
                assertNotNull(root);
                assertEquals(8, root.getChildren().size());

                List<String> expectedItems = List.of(
                        "Annotated element",
                        "Comments",
                        "Identifier",
                        "Invalid Escape String",
                        "Keyword",
                        "Number",
                        "String",
                        "Type reference"
                );
                for (int i = 0; i < expectedItems.size(); i++) {
                    assertEquals(expectedItems.get(i), root.getChildren().get(i).getValue());
                }

                // Default selection: Invalid Escape String
                assertEquals("Invalid Escape String", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Invalid Escape String", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("CF8E6D", page.getForegroundSwatch().getText());
                assertTrue(page.getEffectsCheck().isSelected());
                assertEquals("FA6675", page.getEffectsSwatch().getText());
                assertEquals(EffectType.UNDERWAVED, page.getEffectTypeCombo().getValue());
                assertTrue(page.getInheritCheck().isSelected());
                assertTrue(page.getInheritTargetLabel().getText().contains("Invalid"));

                // Backward compatible subclass
                SettingsGradleDeclarativePage legacyPage = new SettingsGradleDeclarativePage();
                assertNotNull(legacyPage.getCategoryTree());

                // Reset and dirty state
                assertFalse(page.isModified());
                page.getBoldCheck().fire();
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 2. Groovy Page Tests (media_1790432387462.png)
    // -------------------------------------------------------------------------

    @Test
    void testGroovyPageInitAndDefaults() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeGroovyPage page = new SettingsColorSchemeGroovyPage();

                TreeItem<String> root = page.getCategoryTree().getRoot();
                assertNotNull(root);
                assertEquals(9, root.getChildren().size());

                List<String> expectedRoots = List.of(
                        "Bad character",
                        "Braces and Operators",
                        "Comments",
                        "Doc Comments",
                        "Keyword",
                        "Number",
                        "String",
                        "Type reference",
                        "Variables"
                );
                for (int i = 0; i < expectedRoots.size(); i++) {
                    assertEquals(expectedRoots.get(i), root.getChildren().get(i).getValue());
                }

                // Default selection: Lambda expression braces and arrow under Braces and Operators
                TreeItem<String> selected = page.getCategoryTree().getSelectionModel().getSelectedItem();
                assertNotNull(selected);
                assertEquals("Lambda expression braces and arrow", selected.getValue());
                assertEquals("Lambda expression braces and arrow", page.getSelectedKey());

                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("BCBEC4", page.getForegroundSwatch().getText());
                assertTrue(page.getInheritCheck().isSelected());
                assertTrue(page.getInheritTargetLabel().getText().contains("Braces"));

                // Backward compatible subclass
                SettingsGroovyPage legacyPage = new SettingsGroovyPage();
                assertNotNull(legacyPage.getCategoryTree());

                // Modify and reset
                assertFalse(page.isModified());
                page.getBoldCheck().fire();
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 3. HTML Page Tests (media_1790432402285.png)
    // -------------------------------------------------------------------------

    @Test
    void testHtmlPageInitAndDefaults() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeHtmlPage page = new SettingsColorSchemeHtmlPage();

                TreeItem<String> root = page.getCategoryTree().getRoot();
                assertNotNull(root);
                assertEquals(7, root.getChildren().size());

                List<String> expectedRoots = List.of(
                        "Attribute name",
                        "Attribute value",
                        "Comment",
                        "Custom tag name",
                        "Entity reference",
                        "Tag",
                        "Tag name"
                );
                for (int i = 0; i < expectedRoots.size(); i++) {
                    assertEquals(expectedRoots.get(i), root.getChildren().get(i).getValue());
                }

                // Default selection: Tag name
                TreeItem<String> selected = page.getCategoryTree().getSelectionModel().getSelectedItem();
                assertNotNull(selected);
                assertEquals("Tag name", selected.getValue());
                assertEquals("Tag name", page.getSelectedKey());

                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("D5B778", page.getForegroundSwatch().getText());
                // In reference screenshot media_1790432402285.png, Inherit checkbox is initially UNCHECKED
                assertFalse(page.getInheritCheck().isSelected());
                assertTrue(page.getInheritTargetLabel().getText().contains("Keyword"));

                // Backward compatible subclass
                SettingsHTMLPage legacyPage = new SettingsHTMLPage();
                assertNotNull(legacyPage.getCategoryTree());

                // Modify and apply
                assertFalse(page.isModified());
                page.getBoldCheck().fire();
                assertTrue(page.isModified());
                page.apply();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 4. HTTP Request Page Tests (media_1790432436729.png)
    // -------------------------------------------------------------------------

    @Test
    void testHttpRequestPageInitAndDefaults() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeHttpRequestPage page = new SettingsColorSchemeHttpRequestPage();

                TreeItem<String> root = page.getCategoryTree().getRoot();
                assertNotNull(root);
                assertEquals(22, root.getChildren().size());

                // Default selection: Comment
                TreeItem<String> selected = page.getCategoryTree().getSelectionModel().getSelectedItem();
                assertNotNull(selected);
                assertEquals("Comment", selected.getValue());
                assertEquals("Comment", page.getSelectedKey());

                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("5F826B", page.getForegroundSwatch().getText());
                assertTrue(page.getItalicCheck().isSelected());
                assertTrue(page.getInheritCheck().isSelected());
                assertTrue(page.getInheritTargetLabel().getText().contains("Text"));

                // Backward compatible subclass
                SettingsHTTPRequestPage legacyPage = new SettingsHTTPRequestPage();
                assertNotNull(legacyPage.getCategoryTree());

                // Modify and reset
                assertFalse(page.isModified());
                page.getBoldCheck().fire();
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 5. JavaScript Page Tests (media_1790432467203.png)
    // -------------------------------------------------------------------------

    @Test
    void testJavaScriptPageInitAndDefaults() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeJavaScriptPage page = new SettingsColorSchemeJavaScriptPage();

                TreeItem<String> root = page.getCategoryTree().getRoot();
                assertNotNull(root);
                assertEquals(13, root.getChildren().size());

                List<String> expectedRoots = List.of(
                        "Bad character",
                        "Brackets and Operators",
                        "Classes and interfaces",
                        "Comments",
                        "Doc Comments",
                        "Functions and methods",
                        "Keyword",
                        "Number",
                        "Primitive type",
                        "Regular expression",
                        "String",
                        "Variables",
                        "JSX"
                );
                for (int i = 0; i < expectedRoots.size(); i++) {
                    assertEquals(expectedRoots.get(i), root.getChildren().get(i).getValue());
                }

                // Default selection: Bad character
                TreeItem<String> selected = page.getCategoryTree().getSelectionModel().getSelectedItem();
                assertNotNull(selected);
                assertEquals("Bad character", selected.getValue());
                assertEquals("Bad character", page.getSelectedKey());

                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("F75464", page.getForegroundSwatch().getText());
                assertTrue(page.getEffectsCheck().isSelected());
                assertEquals(EffectType.UNDERWAVED, page.getEffectTypeCombo().getValue());
                assertTrue(page.getInheritCheck().isSelected());
                assertTrue(page.getInheritTargetLabel().getText().contains("Bad character"));

                // Backward compatible subclass
                SettingsJavaScriptPage legacyPage = new SettingsJavaScriptPage();
                assertNotNull(legacyPage.getCategoryTree());

                // Modify and reset
                assertFalse(page.isModified());
                page.getItalicCheck().fire();
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 6. SettingsDialog Integration Tests
    // -------------------------------------------------------------------------

    @Test
    void testSettingsDialogIntegration() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDialog dialog = new SettingsDialog(null);

                // 1. Gradle Declarative
                dialog.selectCategory("Gradle Declarative Configuration");
                SettingsColorSchemeGradleDeclarativePage gdPage = dialog.getCurrentColorSchemeGradleDeclarativePage();
                assertNotNull(gdPage);
                assertEquals("Invalid Escape String", gdPage.getSelectedKey());

                // 2. Groovy
                dialog.selectCategory("Groovy");
                SettingsColorSchemeGroovyPage groovyPage = dialog.getCurrentColorSchemeGroovyPage();
                assertNotNull(groovyPage);
                assertEquals("Lambda expression braces and arrow", groovyPage.getSelectedKey());

                // 3. HTML
                dialog.selectCategory("HTML");
                SettingsColorSchemeHtmlPage htmlPage = dialog.getCurrentColorSchemeHtmlPage();
                assertNotNull(htmlPage);
                assertEquals("Tag name", htmlPage.getSelectedKey());

                // 4. HTTP Request
                dialog.selectCategory("HTTP Request");
                SettingsColorSchemeHttpRequestPage httpPage = dialog.getCurrentColorSchemeHttpRequestPage();
                assertNotNull(httpPage);
                assertEquals("Comment", httpPage.getSelectedKey());

                // 5. JavaScript
                dialog.selectCategory("JavaScript");
                SettingsColorSchemeJavaScriptPage jsPage = dialog.getCurrentColorSchemeJavaScriptPage();
                assertNotNull(jsPage);
                assertEquals("Bad character", jsPage.getSelectedKey());

                // Apply button tracking
                jsPage.getBoldCheck().fire();
                assertTrue(jsPage.isModified());
                assertFalse(dialog.getApplyButton().isDisabled());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 7. Strict Lumina Brand Isolation Test
    // -------------------------------------------------------------------------

    @Test
    void testBrandIsolation() throws Exception {
        List<String> filesToCheck = List.of(
                "src/main/java/dev/lumina/ui/SettingsColorSchemeGradleDeclarativePage.java",
                "src/main/java/dev/lumina/ui/SettingsGradleDeclarativePage.java",
                "src/main/java/dev/lumina/ui/SettingsColorSchemeGroovyPage.java",
                "src/main/java/dev/lumina/ui/SettingsGroovyPage.java",
                "src/main/java/dev/lumina/ui/SettingsColorSchemeHtmlPage.java",
                "src/main/java/dev/lumina/ui/SettingsHTMLPage.java",
                "src/main/java/dev/lumina/ui/SettingsColorSchemeHttpRequestPage.java",
                "src/main/java/dev/lumina/ui/SettingsHTTPRequestPage.java",
                "src/main/java/dev/lumina/ui/SettingsColorSchemeJavaScriptPage.java",
                "src/main/java/dev/lumina/ui/SettingsJavaScriptPage.java"
        );

        for (String relPath : filesToCheck) {
            File f = new File(relPath);
            assertTrue(f.exists(), "File should exist: " + relPath);
            String content = Files.readString(f.toPath()).toLowerCase();
            assertFalse(content.contains("intellij"), "File " + relPath + " must not contain competitor name 'intellij'");
            assertFalse(content.contains("jetbrains"), "File " + relPath + " must not contain competitor name 'jetbrains'");
        }
    }
}
