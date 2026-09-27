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
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class SettingsColorSchemeNextFiveLanguagesTest {

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
    // 1. JPA/Hibernate QL Tests (media_1790467528863.png)
    // -------------------------------------------------------------------------

    @Test
    void testJpaHibernateQlPageInitAndDefaults() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeJpaHibernateQlPage page = new SettingsColorSchemeJpaHibernateQlPage();

                TreeItem<String> root = page.getCategoryTree().getRoot();
                assertNotNull(root);
                assertEquals(14, root.getChildren().size());

                // Default selection: Identification variable
                assertEquals("Identification variable", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Identification variable", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("BCBEC4", page.getForegroundSwatch().getText());
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Identifiers->Local variable", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

                // Preview lines
                assertTrue(page.getCodeLinesBox().getChildren().size() >= 5);

                // Modification & dirty state
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getBoldCheck().fire();
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                // Apply
                page.apply();
                assertFalse(page.isModified());
                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Identification variable").isBold());

                // Reset
                page.getBoldCheck().fire();
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Subclass backward compatibility
                SettingsJPAHibernateQLPage legacy = new SettingsJPAHibernateQLPage();
                assertNotNull(legacy);
                assertTrue(legacy instanceof SettingsColorSchemeJpaHibernateQlPage);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 2. JSON Tests (media_1790467543440.png)
    // -------------------------------------------------------------------------

    @Test
    void testJsonPageInitAndDefaults() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeJsonPage page = new SettingsColorSchemeJsonPage();

                TreeItem<String> root = page.getCategoryTree().getRoot();
                assertNotNull(root);
                assertEquals(14, root.getChildren().size());

                // Default selection: Line comment
                assertEquals("Line comment", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Line comment", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("7A7E85", page.getForegroundSwatch().getText());
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Comments->Line comment", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

                // Preview lines
                assertTrue(page.getCodeLinesBox().getChildren().size() >= 5);

                // Modification & dirty state
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getItalicCheck().fire();
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                // Apply
                page.apply();
                assertFalse(page.isModified());
                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Line comment").isItalic());

                // Reset
                page.getItalicCheck().fire();
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Subclass backward compatibility
                SettingsJSONPage legacy = new SettingsJSONPage();
                assertNotNull(legacy);
                assertTrue(legacy instanceof SettingsColorSchemeJsonPage);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 3. JSONPath Tests (media_1790467562785.png)
    // -------------------------------------------------------------------------

    @Test
    void testJsonPathPageInitAndDefaults() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeJsonPathPage page = new SettingsColorSchemeJsonPathPage();

                TreeItem<String> root = page.getCategoryTree().getRoot();
                assertNotNull(root);
                assertEquals(8, root.getChildren().size());

                // Default selection: Boolean
                assertEquals("Boolean", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Boolean", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("2AAC88", page.getForegroundSwatch().getText());
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Number", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

                // Preview lines
                assertTrue(page.getCodeLinesBox().getChildren().size() >= 5);

                // Modification & dirty state
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getBoldCheck().fire();
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                // Apply
                page.apply();
                assertFalse(page.isModified());
                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Boolean").isBold());

                // Reset
                page.getBoldCheck().fire();
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Subclass backward compatibility
                SettingsJSONPathPage legacy = new SettingsJSONPathPage();
                assertNotNull(legacy);
                assertTrue(legacy instanceof SettingsColorSchemeJsonPathPage);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 4. JSP Tests (media_1790467590739.png)
    // -------------------------------------------------------------------------

    @Test
    void testJspPageInitAndDefaults() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeJspPage page = new SettingsColorSchemeJspPage();

                TreeItem<String> root = page.getCategoryTree().getRoot();
                assertNotNull(root);
                assertEquals(7, root.getChildren().size());

                // Default selection: Bounds under Expression Language
                assertEquals("Bounds", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Bounds", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("CF8E6D", page.getForegroundSwatch().getText());
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Keyword", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

                // Preview lines
                assertTrue(page.getCodeLinesBox().getChildren().size() >= 5);

                // Modification & dirty state
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getBoldCheck().fire();
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                // Apply
                page.apply();
                assertFalse(page.isModified());
                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Bounds").isBold());

                // Reset
                page.getBoldCheck().fire();
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Subclass backward compatibility
                SettingsJSPPage legacy = new SettingsJSPPage();
                assertNotNull(legacy);
                assertTrue(legacy instanceof SettingsColorSchemeJspPage);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 5. Jupyter Notebooks Tests (media_1790467611145.png)
    // -------------------------------------------------------------------------

    @Test
    void testJupyterNotebooksPageInitAndDefaults() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeJupyterNotebooksPage page = new SettingsColorSchemeJupyterNotebooksPage();

                TreeItem<String> root = page.getCategoryTree().getRoot();
                assertNotNull(root);
                assertEquals(8, root.getChildren().size());

                // Default selection: Input execution count
                assertEquals("Input execution count", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Input execution count", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("3E7CD7", page.getForegroundSwatch().getText());
                assertTrue(page.getEffectsCheck().isSelected());
                assertEquals(EffectType.UNDERSCORED, page.getEffectTypeCombo().getValue());
                assertEquals("3E7CD7", page.getEffectsSwatch().getText());
                assertFalse(page.getInheritBox().isVisible());

                // Preview lines
                assertTrue(page.getCodeLinesBox().getChildren().size() >= 5);

                // Modification & dirty state
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getBoldCheck().fire();
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                // Apply
                page.apply();
                assertFalse(page.isModified());
                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Input execution count").isBold());

                // Reset
                page.getBoldCheck().fire();
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Subclass backward compatibility
                SettingsJupyterNotebooksPage legacy = new SettingsJupyterNotebooksPage();
                assertNotNull(legacy);
                assertTrue(legacy instanceof SettingsColorSchemeJupyterNotebooksPage);
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

                // 1. JPA/Hibernate QL
                dialog.selectCategory("JPA/Hibernate QL");
                SettingsColorSchemeJpaHibernateQlPage jpaPage = dialog.getCurrentColorSchemeJpaHibernateQlPage();
                assertNotNull(jpaPage);
                assertEquals("Identification variable", jpaPage.getSelectedKey());

                // 2. JSON
                dialog.selectCategory("JSON");
                SettingsColorSchemeJsonPage jsonPage = dialog.getCurrentColorSchemeJsonPage();
                assertNotNull(jsonPage);
                assertEquals("Line comment", jsonPage.getSelectedKey());

                // 3. JSONPath
                dialog.selectCategory("JSONPath");
                SettingsColorSchemeJsonPathPage jsonPathPage = dialog.getCurrentColorSchemeJsonPathPage();
                assertNotNull(jsonPathPage);
                assertEquals("Boolean", jsonPathPage.getSelectedKey());

                // 4. JSP
                dialog.selectCategory("JSP");
                SettingsColorSchemeJspPage jspPage = dialog.getCurrentColorSchemeJspPage();
                assertNotNull(jspPage);
                assertEquals("Bounds", jspPage.getSelectedKey());

                // 5. Jupyter Notebooks
                dialog.selectCategory("Jupyter Notebooks");
                SettingsColorSchemeJupyterNotebooksPage jupyterPage = dialog.getCurrentColorSchemeJupyterNotebooksPage();
                assertNotNull(jupyterPage);
                assertEquals("Input execution count", jupyterPage.getSelectedKey());

                // Apply button tracking
                jupyterPage.getBoldCheck().fire();
                assertTrue(jupyterPage.isModified());
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
                "src/main/java/dev/lumina/ui/SettingsColorSchemeJpaHibernateQlPage.java",
                "src/main/java/dev/lumina/ui/SettingsJPAHibernateQLPage.java",
                "src/main/java/dev/lumina/ui/SettingsColorSchemeJsonPage.java",
                "src/main/java/dev/lumina/ui/SettingsJSONPage.java",
                "src/main/java/dev/lumina/ui/SettingsColorSchemeJsonPathPage.java",
                "src/main/java/dev/lumina/ui/SettingsJSONPathPage.java",
                "src/main/java/dev/lumina/ui/SettingsColorSchemeJspPage.java",
                "src/main/java/dev/lumina/ui/SettingsJSPPage.java",
                "src/main/java/dev/lumina/ui/SettingsColorSchemeJupyterNotebooksPage.java",
                "src/main/java/dev/lumina/ui/SettingsJupyterNotebooksPage.java"
        );

        Pattern competitorPattern = Pattern.compile("\\b(intellij|jetbrains|idea)\\b", Pattern.CASE_INSENSITIVE);

        for (String relPath : filesToCheck) {
            File f = new File(relPath);
            assertTrue(f.exists(), "File should exist: " + relPath);
            String content = Files.readString(f.toPath());
            var matcher = competitorPattern.matcher(content);
            assertFalse(matcher.find(), "File " + relPath + " must not contain competitor brand names");
        }
    }
}
