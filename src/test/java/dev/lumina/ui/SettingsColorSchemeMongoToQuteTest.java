package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import dev.lumina.settings.EditorColorSchemeSettings.AttributesDescriptor;
import dev.lumina.settings.EditorColorSchemeSettings.EffectType;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class SettingsColorSchemeMongoToQuteTest {

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
    // 1. MongoDB JSON Page Tests (media_1790472970073.png)
    // -------------------------------------------------------------------------

    @Test
    void testMongoDBJSONPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeMongoDBJSONPage page = new SettingsColorSchemeMongoDBJSONPage();

                // 9 top-level items in tree
                assertEquals(9, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Keyword (#CF8E6D, inherit=true, target="Keyword", scope="(Language Defaults)")
                assertEquals("Keyword", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Keyword", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("CF8E6D", page.getForegroundSwatch().getText());
                assertTrue(page.isInheritChecked());
                assertEquals("Keyword", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

                // Preview has code lines
                assertTrue(page.getCodeLinesBox().getChildren().size() >= 5);

                // Modification and Apply
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getBoldCheck().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.apply();
                assertFalse(page.isModified());

                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Keyword").isBold());

                // Select Brackets under Braces and Operators
                page.selectTreeItem("Braces and Operators // Brackets");
                assertEquals("Braces and Operators // Brackets", page.getSelectedKey());
                assertTrue(page.isInheritChecked());
                assertEquals("Braces and Operators->Brackets", page.getInheritTargetLabel().getText());

            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 2. Properties Page Tests (media_1790472995925.png)
    // -------------------------------------------------------------------------

    @Test
    void testPropertiesPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemePropertiesPage page = new SettingsColorSchemePropertiesPage();

                // 6 top-level items in tree
                assertEquals(6, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Key/value separator (#808080, inherit=false, target="Braces and Operators->Operation sign")
                assertEquals("Key/value separator", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Key/value separator", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("808080", page.getForegroundSwatch().getText());
                assertFalse(page.isInheritChecked());
                assertEquals("Braces and Operators->Operation sign", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

                // Preview has code lines
                assertTrue(page.getCodeLinesBox().getChildren().size() >= 5);

                // Modification and Apply
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getItalicCheck().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.apply();
                assertFalse(page.isModified());

                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Key/value separator").isItalic());

                // Select Key
                page.selectTreeItem("Key");
                assertEquals("Key", page.getSelectedKey());
                assertTrue(page.isInheritChecked());
                assertEquals("CF8E6D", page.getForegroundSwatch().getText());

            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 3. Protocol Buffer Text Page Tests (media_1790473030971.png)
    // -------------------------------------------------------------------------

    @Test
    void testProtocolBufferTextPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeProtocolBufferTextPage page = new SettingsColorSchemeProtocolBufferTextPage();

                // 15 top-level items in tree
                assertEquals(15, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Line comment (#7A7E85, italic=true, inherit=true, target="Comments->Line comment")
                assertEquals("Line comment", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Line comment", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("7A7E85", page.getForegroundSwatch().getText());
                assertTrue(page.getItalicCheck().isSelected());
                assertTrue(page.isInheritChecked());
                assertEquals("Comments->Line comment", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

                // Preview has code lines
                assertTrue(page.getCodeLinesBox().getChildren().size() >= 5);

                // Modification and Apply
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getBoldCheck().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.apply();
                assertFalse(page.isModified());

                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Line comment").isBold());

                // Select Field name
                page.selectTreeItem("Field name");
                assertEquals("Field name", page.getSelectedKey());
                assertTrue(page.isInheritChecked());
                assertEquals("Identifiers->Field", page.getInheritTargetLabel().getText());

            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 4. Python Page Tests (media_1790473064608.png)
    // -------------------------------------------------------------------------

    @Test
    void testPythonPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemePythonPage page = new SettingsColorSchemePythonPage();

                // 16 top-level items / categories in tree
                assertEquals(16, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Keyword argument (#AA4926, inherit=false, target="Identifiers->Parameter")
                assertEquals("Keyword argument", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Keyword argument", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("AA4926", page.getForegroundSwatch().getText());
                assertFalse(page.isInheritChecked());
                assertEquals("Identifiers->Parameter", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

                // Preview has code lines
                assertTrue(page.getCodeLinesBox().getChildren().size() >= 10);

                // Modification and Apply
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getBoldCheck().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.apply();
                assertFalse(page.isModified());

                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Keyword argument").isBold());

                // Select Keywords // from
                page.selectTreeItem("Keywords // from");
                assertEquals("Keywords // from", page.getSelectedKey());
                assertTrue(page.isInheritChecked());
                assertEquals("Keyword", page.getInheritTargetLabel().getText());

            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 5. Qute Page Tests (media_1790473087185.png)
    // -------------------------------------------------------------------------

    @Test
    void testQutePage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeQutePage page = new SettingsColorSchemeQutePage();

                // 9 top-level items in tree
                assertEquals(9, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Template background (Background=#27292B checked, foreground unchecked, inherit=false, target="Template language")
                assertEquals("Template background", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Template background", page.getSelectedKey());
                assertFalse(page.getForegroundCheck().isSelected());
                assertTrue(page.getBackgroundCheck().isSelected());
                assertEquals("27292B", page.getBackgroundSwatch().getText());
                assertFalse(page.isInheritChecked());
                assertEquals("Template language", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

                // Preview has code lines
                assertTrue(page.getCodeLinesBox().getChildren().size() >= 5);

                // Modification and Apply
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getItalicCheck().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.apply();
                assertFalse(page.isModified());

                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Template background").isItalic());

                // Select Tag name
                page.selectTreeItem("Tag name");
                assertEquals("Tag name", page.getSelectedKey());
                assertTrue(page.isInheritChecked());
                assertEquals("CF8E6D", page.getForegroundSwatch().getText());

            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 6. Backward Compatibility Wrappers
    // -------------------------------------------------------------------------

    @Test
    void testBackwardCompatibilityWrappers() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsMongoDBJSONPage mongoPage = new SettingsMongoDBJSONPage();
                assertTrue(mongoPage instanceof SettingsColorSchemeMongoDBJSONPage);

                SettingsPropertiesPage propPage = new SettingsPropertiesPage();
                assertTrue(propPage instanceof SettingsColorSchemePropertiesPage);

                SettingsProtocolBufferTextPage protoTextPage = new SettingsProtocolBufferTextPage();
                assertTrue(protoTextPage instanceof SettingsColorSchemeProtocolBufferTextPage);

                SettingsPythonPage pyPage = new SettingsPythonPage();
                assertTrue(pyPage instanceof SettingsColorSchemePythonPage);

                SettingsQutePage qutePage = new SettingsQutePage();
                assertTrue(qutePage instanceof SettingsColorSchemeQutePage);

            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 7. Descriptor Lists & EditorColorSchemeSettings Registration
    // -------------------------------------------------------------------------

    @Test
    void testDescriptorListsAndRegistration() {
        assertEquals(14, EditorColorSchemeSettings.getMongoDbJsonDescriptors().size());
        assertEquals(6, EditorColorSchemeSettings.getPropertiesDescriptors().size());
        assertEquals(15, EditorColorSchemeSettings.getProtocolBufferTextDescriptors().size());
        assertEquals(34, EditorColorSchemeSettings.getPythonDescriptors().size());
        assertEquals(14, EditorColorSchemeSettings.getQuteDescriptors().size());

        for (AttributesDescriptor desc : EditorColorSchemeSettings.getMongoDbJsonDescriptors()) {
            assertNotNull(EditorColorSchemeSettings.getDescriptor(desc.getKey()), "Descriptor missing for: " + desc.getKey());
        }
        for (AttributesDescriptor desc : EditorColorSchemeSettings.getPropertiesDescriptors()) {
            assertNotNull(EditorColorSchemeSettings.getDescriptor(desc.getKey()), "Descriptor missing for: " + desc.getKey());
        }
        for (AttributesDescriptor desc : EditorColorSchemeSettings.getProtocolBufferTextDescriptors()) {
            assertNotNull(EditorColorSchemeSettings.getDescriptor(desc.getKey()), "Descriptor missing for: " + desc.getKey());
        }
        for (AttributesDescriptor desc : EditorColorSchemeSettings.getPythonDescriptors()) {
            assertNotNull(EditorColorSchemeSettings.getDescriptor(desc.getKey()), "Descriptor missing for: " + desc.getKey());
        }
        for (AttributesDescriptor desc : EditorColorSchemeSettings.getQuteDescriptors()) {
            assertNotNull(EditorColorSchemeSettings.getDescriptor(desc.getKey()), "Descriptor missing for: " + desc.getKey());
        }
    }

    // -------------------------------------------------------------------------
    // 8. SettingsDialog Integration & Navigation
    // -------------------------------------------------------------------------

    @Test
    void testSettingsDialogIntegration() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDialog dialog = new SettingsDialog(null);

                // Find Color Scheme node in dialog tree
                TreeItem<String> csNode = null;
                for (TreeItem<String> top : dialog.getTree().getRoot().getChildren()) {
                    if ("Editor".equals(top.getValue())) {
                        for (TreeItem<String> child : top.getChildren()) {
                            if ("Color Scheme".equals(child.getValue())) {
                                csNode = child;
                                break;
                            }
                        }
                    }
                }
                assertNotNull(csNode, "Color Scheme tree item must exist");

                // Verify Python, MongoDB JSON, Properties, Protocol Buffer Text, Qute items exist
                boolean foundMongo = false;
                boolean foundProps = false;
                boolean foundProtoText = false;
                boolean foundPython = false;
                boolean foundQute = false;

                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("MongoDB JSON".equals(child.getValue())) foundMongo = true;
                    if ("Properties".equals(child.getValue())) foundProps = true;
                    if ("Protocol Buffer Text".equals(child.getValue())) foundProtoText = true;
                    if ("Python".equals(child.getValue())) foundPython = true;
                    if ("Qute".equals(child.getValue())) foundQute = true;
                }

                assertTrue(foundMongo, "MongoDB JSON should be in Color Scheme tree");
                assertTrue(foundProps, "Properties should be in Color Scheme tree");
                assertTrue(foundProtoText, "Protocol Buffer Text should be in Color Scheme tree");
                assertTrue(foundPython, "Python should be in Color Scheme tree");
                assertTrue(foundQute, "Qute should be in Color Scheme tree");

                // Test selection of MongoDB JSON
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("MongoDB JSON".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                assertNotNull(dialog.getCurrentColorSchemeMongoDBJSONPage());
                assertEquals("Keyword", dialog.getCurrentColorSchemeMongoDBJSONPage().getSelectedKey());

                // Test selection of Properties
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("Properties".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                assertNotNull(dialog.getCurrentColorSchemePropertiesPage());
                assertEquals("Key/value separator", dialog.getCurrentColorSchemePropertiesPage().getSelectedKey());

                // Test selection of Protocol Buffer Text
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("Protocol Buffer Text".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                assertNotNull(dialog.getCurrentColorSchemeProtocolBufferTextPage());
                assertEquals("Line comment", dialog.getCurrentColorSchemeProtocolBufferTextPage().getSelectedKey());

                // Test selection of Python page
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("Python".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                assertNotNull(dialog.getCurrentColorSchemePythonPage());
                assertEquals("Keyword argument", dialog.getCurrentColorSchemePythonPage().getSelectedKey());

                // Test selection of Qute page
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("Qute".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                assertNotNull(dialog.getCurrentColorSchemeQutePage());
                assertEquals("Template background", dialog.getCurrentColorSchemeQutePage().getSelectedKey());

                // Test Revert Link navigation for Python -> Language Defaults
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("Python".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                dialog.getCurrentColorSchemePythonPage().getInheritTargetLabel().fire();
                assertEquals("Language Defaults", dialog.getTree().getSelectionModel().getSelectedItem().getValue());

            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 9. Brand Isolation Test
    // -------------------------------------------------------------------------

    @Test
    void testBrandIsolation() throws Exception {
        Pattern competitorPattern = Pattern.compile("(?i)\\b(intellij|jetbrains|idea)\\b");

        List<String> filesToCheck = List.of(
                "src/main/java/dev/lumina/ui/SettingsColorSchemeMongoDBJSONPage.java",
                "src/main/java/dev/lumina/ui/SettingsColorSchemePropertiesPage.java",
                "src/main/java/dev/lumina/ui/SettingsColorSchemeProtocolBufferTextPage.java",
                "src/main/java/dev/lumina/ui/SettingsColorSchemePythonPage.java",
                "src/main/java/dev/lumina/ui/SettingsColorSchemeQutePage.java",
                "src/main/java/dev/lumina/ui/SettingsMongoDBJSONPage.java",
                "src/main/java/dev/lumina/ui/SettingsPropertiesPage.java",
                "src/main/java/dev/lumina/ui/SettingsProtocolBufferTextPage.java",
                "src/main/java/dev/lumina/ui/SettingsPythonPage.java",
                "src/main/java/dev/lumina/ui/SettingsQutePage.java"
        );

        for (String relPath : filesToCheck) {
            Path path = Path.of(relPath);
            assertTrue(Files.exists(path), "File must exist: " + relPath);
            String content = Files.readString(path);
            var matcher = competitorPattern.matcher(content);
            assertFalse(matcher.find(), "Competitor brand found in file " + relPath + ": " + (matcher.hitEnd() ? "" : matcher.group()));
        }
    }
}
