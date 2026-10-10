package dev.lumina.ui;

import dev.lumina.scala.ScalaExtensionLibrary;
import dev.lumina.scala.ScalaExtensionsSettings;
import dev.lumina.scala.ScalaLanguageSettingsManager;
import dev.lumina.scala.ScalaUpdatesSettings;
import dev.lumina.schemas.*;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class SettingsLanguagesScalaUpdatesExtensionsAndSchemasTest {

    private static boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFx() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            try {
                Platform.startup(() -> {
                    javaFxAvailable = true;
                    latch.countDown();
                });
            } catch (IllegalStateException e) {
                javaFxAvailable = true;
                latch.countDown();
            }
            javaFxAvailable = latch.await(5, TimeUnit.SECONDS);
        } catch (Throwable t) {
            javaFxAvailable = false;
        }
    }

    private void runOnFx(Runnable action) throws Exception {
        if (!javaFxAvailable) return;
        CountDownLatch latch = new CountDownLatch(1);
        Throwable[] err = new Throwable[1];
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                err[0] = t;
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX operation timed out");
        if (err[0] != null) {
            if (err[0] instanceof Exception) throw (Exception) err[0];
            throw new RuntimeException(err[0]);
        }
    }

    // ============================================================
    // 1. Scala Updates (Image 1)
    // ============================================================

    @Test
    void testScalaUpdatesModelAndManager() {
        ScalaLanguageSettingsManager manager = ScalaLanguageSettingsManager.getInstance();
        manager.resetDefaults();

        ScalaUpdatesSettings updates = manager.getUpdatesSettings();
        assertEquals("Stable Releases", updates.getUpdateChannel());

        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addChangeListener(() -> notified.set(true));

        updates.setUpdateChannel("Early Access Program");
        manager.setUpdatesSettings(updates);

        assertTrue(notified.get());
        assertEquals("Early Access Program", manager.getUpdatesSettings().getUpdateChannel());

        updates.setUpdateChannel("Nightly");
        manager.setUpdatesSettings(updates);
        assertEquals("Nightly", manager.getUpdatesSettings().getUpdateChannel());

        manager.resetDefaults();
        assertEquals("Stable Releases", manager.getUpdatesSettings().getUpdateChannel());
    }

    @Test
    void testScalaUpdatesPageUi() throws Exception {
        runOnFx(() -> {
            ScalaLanguageSettingsManager.getInstance().resetDefaults();

            SettingsLanguagesScalaUpdatesPage page = new SettingsLanguagesScalaUpdatesPage();
            assertFalse(page.isModified());
            assertEquals("Stable Releases", page.getUpdateChannelComboBox().getValue());

            // Check options
            assertTrue(page.getUpdateChannelComboBox().getItems().contains("Stable Releases"));
            assertTrue(page.getUpdateChannelComboBox().getItems().contains("Early Access Program"));
            assertTrue(page.getUpdateChannelComboBox().getItems().contains("Nightly"));

            // Check Now button click
            page.getCheckNowBtn().fire();
            assertTrue(page.getStatusLabel().getText().contains("latest version"));

            // Modify channel
            page.getUpdateChannelComboBox().setValue("Early Access Program");
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertEquals("Early Access Program", ScalaLanguageSettingsManager.getInstance().getUpdatesSettings().getUpdateChannel());

            // Revert changes
            page.getUpdateChannelComboBox().setValue("Nightly");
            assertTrue(page.isModified());
            page.revertChanges();
            assertEquals("Early Access Program", page.getUpdateChannelComboBox().getValue());
            assertFalse(page.isModified());

            // Reset defaults
            page.resetDefaults();
            assertEquals("Stable Releases", page.getUpdateChannelComboBox().getValue());
        });
    }

    // ============================================================
    // 2. Scala Extensions (Image 2)
    // ============================================================

    @Test
    void testScalaExtensionsModelAndManager() {
        ScalaLanguageSettingsManager manager = ScalaLanguageSettingsManager.getInstance();
        manager.resetDefaults();

        ScalaExtensionsSettings ext = manager.getExtensionsSettings();
        assertTrue(ext.isEnableLoadingExternalExtensions());
        assertTrue(ext.getKnownLibraries().isEmpty());

        ext.setEnableLoadingExternalExtensions(false);
        List<ScalaExtensionLibrary> libs = new ArrayList<>();
        libs.add(new ScalaExtensionLibrary("cats-effect-extension", "/path/to/cats.jar", true, List.of("CatsEffectPlugin")));
        ext.setKnownLibraries(libs);
        manager.setExtensionsSettings(ext);

        ScalaExtensionsSettings reloaded = manager.getExtensionsSettings();
        assertFalse(reloaded.isEnableLoadingExternalExtensions());
        assertEquals(1, reloaded.getKnownLibraries().size());
        assertEquals("cats-effect-extension", reloaded.getKnownLibraries().get(0).getName());
        assertEquals("CatsEffectPlugin", reloaded.getKnownLibraries().get(0).getExtensions().get(0));

        manager.resetDefaults();
        assertTrue(manager.getExtensionsSettings().isEnableLoadingExternalExtensions());
        assertTrue(manager.getExtensionsSettings().getKnownLibraries().isEmpty());
    }

    @Test
    void testScalaExtensionsPageUi() throws Exception {
        runOnFx(() -> {
            ScalaLanguageSettingsManager.getInstance().resetDefaults();

            SettingsLanguagesScalaExtensionsPage page = new SettingsLanguagesScalaExtensionsPage();
            assertFalse(page.isModified());
            assertTrue(page.getEnableLoadingCheckBox().isSelected());
            assertTrue(page.getLibraryItems().isEmpty());

            page.getEnableLoadingCheckBox().setSelected(false);
            assertTrue(page.isModified());

            // Add library row
            page.getEnableLoadingCheckBox().setSelected(true);
            SettingsLanguagesScalaExtensionsPage.LibraryRow lib = new SettingsLanguagesScalaExtensionsPage.LibraryRow("zio-ext", "/lib/zio.jar", true, List.of("ZioExtension"));
            page.getLibraryItems().add(lib);
            page.getLibraryTable().getSelectionModel().select(lib);

            assertEquals(1, page.getExtensionsListView().getItems().size());
            assertEquals("ZioExtension", page.getExtensionsListView().getItems().get(0));

            page.apply();
            assertFalse(page.isModified());
            assertTrue(ScalaLanguageSettingsManager.getInstance().getExtensionsSettings().isEnableLoadingExternalExtensions());
            assertEquals(1, ScalaLanguageSettingsManager.getInstance().getExtensionsSettings().getKnownLibraries().size());

            // Revert changes
            page.getEnableLoadingCheckBox().setSelected(false);
            assertTrue(page.isModified());
            page.revertChanges();
            assertTrue(page.getEnableLoadingCheckBox().isSelected());
            assertFalse(page.isModified());

            // Reset defaults
            page.resetDefaults();
            assertTrue(page.getEnableLoadingCheckBox().isSelected());
            assertTrue(page.getLibraryItems().isEmpty());
        });
    }

    // ============================================================
    // 3. Schemas and DTDs Overview (Image 3)
    // ============================================================

    @Test
    void testSchemasAndDtdsModelAndManager() {
        SchemasAndDtdsSettingsManager manager = SchemasAndDtdsSettingsManager.getInstance();
        manager.resetDefaults();

        SchemasAndDtdsSettings settings = manager.getSchemasAndDtdsSettings();
        assertTrue(settings.getExternalResources().isEmpty());
        assertEquals(8, settings.getIgnoredSchemas().size());
        assertTrue(settings.getIgnoredSchemas().contains("http://exslt.org/common"));
        assertTrue(settings.getIgnoredSchemas().contains("urn:lumina:xslt-plugin#extensions"));

        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addChangeListener(() -> notified.set(true));

        List<ExternalResourceEntry> ext = new ArrayList<>();
        ext.add(new ExternalResourceEntry("http://example.com/schema.xsd", "/local/schema.xsd"));
        settings.setExternalResources(ext);

        List<String> ignored = new ArrayList<>(settings.getIgnoredSchemas());
        ignored.add("http://custom.schema/ignored");
        settings.setIgnoredSchemas(ignored);

        manager.setSchemasAndDtdsSettings(settings);
        assertTrue(notified.get());

        SchemasAndDtdsSettings reloaded = manager.getSchemasAndDtdsSettings();
        assertEquals(1, reloaded.getExternalResources().size());
        assertEquals("http://example.com/schema.xsd", reloaded.getExternalResources().get(0).getUri());
        assertEquals(9, reloaded.getIgnoredSchemas().size());

        manager.resetDefaults();
        assertEquals(8, manager.getSchemasAndDtdsSettings().getIgnoredSchemas().size());
        assertTrue(manager.getSchemasAndDtdsSettings().getExternalResources().isEmpty());
    }

    @Test
    void testSchemasAndDtdsPageUi() throws Exception {
        runOnFx(() -> {
            SchemasAndDtdsSettingsManager.getInstance().resetDefaults();

            SettingsLanguagesSchemasAndDtdsPage page = new SettingsLanguagesSchemasAndDtdsPage();
            assertFalse(page.isModified());
            assertTrue(page.getExtItems().isEmpty());
            assertEquals(8, page.getIgnoredItems().size());

            // Add external resource
            page.getExtItems().add(new SettingsLanguagesSchemasAndDtdsPage.ExternalResourceRow("http://test.com/schema.xsd", "/local/test.xsd"));
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertEquals(1, SchemasAndDtdsSettingsManager.getInstance().getSchemasAndDtdsSettings().getExternalResources().size());

            // Remove an ignored schema
            page.getIgnoredItems().remove(0);
            assertTrue(page.isModified());
            page.revertChanges();
            assertFalse(page.isModified());
            assertEquals(8, page.getIgnoredItems().size());

            // Reset defaults
            page.resetDefaults();
            assertTrue(page.getExtItems().isEmpty());
            assertEquals(8, page.getIgnoredItems().size());
        });
    }

    // ============================================================
    // 4. Default XML Schemas (Image 4)
    // ============================================================

    @Test
    void testDefaultXmlSchemasModelAndManager() {
        SchemasAndDtdsSettingsManager manager = SchemasAndDtdsSettingsManager.getInstance();
        manager.resetDefaults();

        DefaultXmlSchemasSettings settings = manager.getDefaultXmlSchemasSettings();
        assertEquals("HTML 5", settings.getHtmlLanguageLevel());
        assertEquals("XML Schema 1.0", settings.getXmlSchemaVersion());
        assertEquals("", settings.getOtherDoctype());

        settings.setHtmlLanguageLevel(DefaultXmlSchemasSettings.HTML_4);
        settings.setXmlSchemaVersion("XML Schema 1.1");
        manager.setDefaultXmlSchemasSettings(settings);

        DefaultXmlSchemasSettings reloaded = manager.getDefaultXmlSchemasSettings();
        assertEquals(DefaultXmlSchemasSettings.HTML_4, reloaded.getHtmlLanguageLevel());
        assertEquals("XML Schema 1.1", reloaded.getXmlSchemaVersion());

        manager.resetDefaults();
        assertEquals("HTML 5", manager.getDefaultXmlSchemasSettings().getHtmlLanguageLevel());
        assertEquals("XML Schema 1.0", manager.getDefaultXmlSchemasSettings().getXmlSchemaVersion());
    }

    @Test
    void testDefaultXmlSchemasPageUi() throws Exception {
        runOnFx(() -> {
            SchemasAndDtdsSettingsManager.getInstance().resetDefaults();

            SettingsLanguagesDefaultXmlSchemasPage page = new SettingsLanguagesDefaultXmlSchemasPage();
            assertFalse(page.isModified());
            assertTrue(page.getHtml5Radio().isSelected());
            assertTrue(page.getXmlSchema10Radio().isSelected());
            assertTrue(page.getOtherDoctypeField().isDisable());

            // Switch to HTML 4
            page.getHtml4Radio().setSelected(true);
            assertTrue(page.isModified());

            // Switch to XML 1.1
            page.getXmlSchema11Radio().setSelected(true);
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertEquals(DefaultXmlSchemasSettings.HTML_4, SchemasAndDtdsSettingsManager.getInstance().getDefaultXmlSchemasSettings().getHtmlLanguageLevel());
            assertEquals("XML Schema 1.1", SchemasAndDtdsSettingsManager.getInstance().getDefaultXmlSchemasSettings().getXmlSchemaVersion());

            // Revert changes
            page.getOtherDoctypeRadio().setSelected(true);
            page.getOtherDoctypeField().setText("custom-doctype");
            assertFalse(page.getOtherDoctypeField().isDisable());
            assertTrue(page.isModified());
            page.revertChanges();
            assertFalse(page.isModified());
            assertTrue(page.getHtml4Radio().isSelected());
            assertTrue(page.getOtherDoctypeField().isDisable());

            // Reset defaults
            page.resetDefaults();
            assertTrue(page.getHtml5Radio().isSelected());
            assertTrue(page.getXmlSchema10Radio().isSelected());
        });
    }

    // ============================================================
    // 5. JSON Schema Mappings (Image 5)
    // ============================================================

    @Test
    void testJsonSchemaMappingsModelAndManager() {
        SchemasAndDtdsSettingsManager manager = SchemasAndDtdsSettingsManager.getInstance();
        manager.resetDefaults();

        JsonSchemaMappingsSettings settings = manager.getJsonSchemaMappingsSettings();
        assertEquals(1, settings.getMappings().size());
        assertEquals("New Schema", settings.getMappings().get(0).getName());
        assertEquals("JSON Schema v4", settings.getMappings().get(0).getSchemaVersion());

        JsonSchemaMapping mapping = new JsonSchemaMapping("Custom Schema", "https://example.com/schema.json", "JSON Schema 2020-12");
        mapping.getPatterns().add(new JsonSchemaPattern("*.custom.json", JsonSchemaPattern.PatternType.PATTERN));
        settings.getMappings().add(mapping);
        manager.setJsonSchemaMappingsSettings(settings);

        JsonSchemaMappingsSettings reloaded = manager.getJsonSchemaMappingsSettings();
        assertEquals(2, reloaded.getMappings().size());
        assertEquals("Custom Schema", reloaded.getMappings().get(1).getName());
        assertEquals("https://example.com/schema.json", reloaded.getMappings().get(1).getSchemaFileOrUrl());
        assertEquals(1, reloaded.getMappings().get(1).getPatterns().size());
        assertEquals("*.custom.json", reloaded.getMappings().get(1).getPatterns().get(0).getPattern());

        manager.resetDefaults();
        assertEquals(1, manager.getJsonSchemaMappingsSettings().getMappings().size());
        assertEquals("New Schema", manager.getJsonSchemaMappingsSettings().getMappings().get(0).getName());
    }

    @Test
    void testJsonSchemaMappingsPageUi() throws Exception {
        runOnFx(() -> {
            SchemasAndDtdsSettingsManager.getInstance().resetDefaults();

            SettingsLanguagesJsonSchemaMappingsPage page = new SettingsLanguagesJsonSchemaMappingsPage();
            assertFalse(page.isModified());
            assertEquals(1, page.getMasterItems().size());
            assertEquals("New Schema", page.getMasterItems().get(0));

            // Detail form checks
            assertEquals("New Schema", page.getNameField().getText());
            assertEquals("JSON Schema v4", page.getSchemaVersionComboBox().getValue());
            assertTrue(page.getPatternsItems().isEmpty());

            // Modify Name reactively
            page.getNameField().setText("Updated Schema");
            assertTrue(page.isModified());
            assertEquals("Updated Schema", page.getMasterItems().get(0));

            // Modify Version
            page.getSchemaVersionComboBox().setValue("JSON Schema 2020-12");
            assertTrue(page.isModified());

            // Add pattern
            page.getPatternsItems().add(new SettingsLanguagesJsonSchemaMappingsPage.PatternRow("config.json", "file"));
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertEquals(1, SchemasAndDtdsSettingsManager.getInstance().getJsonSchemaMappingsSettings().getMappings().size());
            assertEquals("Updated Schema", SchemasAndDtdsSettingsManager.getInstance().getJsonSchemaMappingsSettings().getMappings().get(0).getName());
            assertEquals("JSON Schema 2020-12", SchemasAndDtdsSettingsManager.getInstance().getJsonSchemaMappingsSettings().getMappings().get(0).getSchemaVersion());
            assertEquals(1, SchemasAndDtdsSettingsManager.getInstance().getJsonSchemaMappingsSettings().getMappings().get(0).getPatterns().size());

            // Revert changes
            page.getNameField().setText("Temporary Schema");
            assertTrue(page.isModified());
            page.revertChanges();
            assertFalse(page.isModified());
            assertEquals("Updated Schema", page.getNameField().getText());

            // Reset defaults
            page.resetDefaults();
            assertEquals(1, page.getMasterItems().size());
            assertEquals("New Schema", page.getMasterItems().get(0));
            assertEquals("JSON Schema v4", page.getSchemaVersionComboBox().getValue());
        });
    }

    // ============================================================
    // 6. SettingsDialog Navigation & Integration Test
    // ============================================================

    @Test
    void testSettingsDialogTreeAndPageDispatch() throws Exception {
        runOnFx(() -> {
            SettingsDialog dialog = new SettingsDialog(null);
            assertNotNull(dialog.getTree(), "Tree must exist");

            TreeItem<String> languagesItem = null;
            for (TreeItem<String> rootChild : dialog.getTree().getRoot().getChildren()) {
                if ("Languages & Frameworks".equals(rootChild.getValue())) {
                    languagesItem = rootChild;
                    break;
                }
            }
            assertNotNull(languagesItem, "Languages & Frameworks category must exist");

            TreeItem<String> scalaItem = null;
            TreeItem<String> schemasItem = null;
            for (TreeItem<String> langChild : languagesItem.getChildren()) {
                if ("Scala".equals(langChild.getValue())) {
                    scalaItem = langChild;
                } else if ("Schemas and DTDs".equals(langChild.getValue())) {
                    schemasItem = langChild;
                }
            }
            assertNotNull(scalaItem, "Scala category item must exist");
            assertNotNull(schemasItem, "Schemas and DTDs category item must exist");

            // Verify Scala children contain Updates and Extensions
            boolean hasUpdates = false;
            boolean hasExtensions = false;
            for (TreeItem<String> child : scalaItem.getChildren()) {
                if ("Updates".equals(child.getValue())) hasUpdates = true;
                if ("Extensions".equals(child.getValue())) hasExtensions = true;
            }
            assertTrue(hasUpdates, "Scala must have Updates child");
            assertTrue(hasExtensions, "Scala must have Extensions child");

            // Verify Schemas and DTDs children
            boolean hasDefaultXml = false;
            boolean hasJsonSchema = false;
            for (TreeItem<String> child : schemasItem.getChildren()) {
                if ("Default XML Schemas".equals(child.getValue())) hasDefaultXml = true;
                if ("JSON Schema Mappings".equals(child.getValue())) hasJsonSchema = true;
            }
            assertTrue(hasDefaultXml, "Schemas and DTDs must have Default XML Schemas child");
            assertTrue(hasJsonSchema, "Schemas and DTDs must have JSON Schema Mappings child");

            // Select Scala > Updates
            for (TreeItem<String> child : scalaItem.getChildren()) {
                if ("Updates".equals(child.getValue())) {
                    dialog.getTree().getSelectionModel().select(child);
                    break;
                }
            }
            assertNotNull(dialog.getCurrentLanguagesScalaUpdatesPage(), "Scala Updates page must be built");

            // Select Scala > Extensions
            for (TreeItem<String> child : scalaItem.getChildren()) {
                if ("Extensions".equals(child.getValue())) {
                    dialog.getTree().getSelectionModel().select(child);
                    break;
                }
            }
            assertNotNull(dialog.getCurrentLanguagesScalaExtensionsPage(), "Scala Extensions page must be built");

            // Select Schemas and DTDs parent
            dialog.getTree().getSelectionModel().select(schemasItem);
            assertNotNull(dialog.getCurrentLanguagesSchemasAndDtdsPage(), "Schemas and DTDs overview page must be built");

            // Select Schemas and DTDs > Default XML Schemas
            for (TreeItem<String> child : schemasItem.getChildren()) {
                if ("Default XML Schemas".equals(child.getValue())) {
                    dialog.getTree().getSelectionModel().select(child);
                    break;
                }
            }
            assertNotNull(dialog.getCurrentLanguagesDefaultXmlSchemasPage(), "Default XML Schemas page must be built");

            // Select Schemas and DTDs > JSON Schema Mappings
            for (TreeItem<String> child : schemasItem.getChildren()) {
                if ("JSON Schema Mappings".equals(child.getValue())) {
                    dialog.getTree().getSelectionModel().select(child);
                    break;
                }
            }
            assertNotNull(dialog.getCurrentLanguagesJsonSchemaMappingsPage(), "JSON Schema Mappings page must be built");
        });
    }
}
