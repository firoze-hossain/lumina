package dev.lumina.ui;

import dev.lumina.typescript.VueServiceSettings;
import dev.lumina.typescript.VueServiceSettingsManager;
import dev.lumina.web.WebContextMapping;
import dev.lumina.web.WebContextsSettings;
import dev.lumina.web.WebContextsSettingsManager;
import dev.lumina.xslt.*;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class SettingsLanguagesVueWebContextsXsltTest {

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
    // 1. Vue (Image 1)
    // ============================================================

    @Test
    void testVueModelAndManager() {
        VueServiceSettingsManager manager = VueServiceSettingsManager.getInstance();

        VueServiceSettings settings = new VueServiceSettings();
        assertEquals(VueServiceSettings.DEFAULT_SERVER, settings.getServerPackage());
        assertEquals(VueServiceSettings.MODE_AUTO, settings.getMode());
        assertFalse(settings.isEnableServicePoweredTypeEngine());
        assertFalse(settings.isVueLs3Preview());

        settings.setMode(VueServiceSettings.MODE_CLASSIC);
        settings.setEnableServicePoweredTypeEngine(true);
        settings.setVueLs3Preview(true);

        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addChangeListener(() -> notified.set(true));

        manager.setSettings(settings);
        assertTrue(notified.get());

        VueServiceSettings loaded = manager.getSettings();
        assertEquals(VueServiceSettings.MODE_CLASSIC, loaded.getMode());
        assertTrue(loaded.isEnableServicePoweredTypeEngine());
        assertTrue(loaded.isVueLs3Preview());

        List<String> servers = manager.discoverVueServers();
        assertNotNull(servers);
        assertFalse(servers.isEmpty());

        // Reset
        manager.setSettings(new VueServiceSettings());
    }

    @Test
    void testVuePageLifecycle() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsLanguagesTypeScriptVuePage page = new SettingsLanguagesTypeScriptVuePage();
            assertFalse(page.isModified());

            page.getDisabledRadio().setSelected(true);
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertEquals(VueServiceSettings.MODE_DISABLED, VueServiceSettingsManager.getInstance().getSettings().getMode());

            page.getLs3PreviewCheck().setSelected(true);
            assertTrue(page.isModified());

            page.revertChanges();
            assertFalse(page.isModified());
            assertFalse(page.getLs3PreviewCheck().isSelected());

            // Cleanup
            VueServiceSettingsManager.getInstance().setSettings(new VueServiceSettings());
            page.reset();
        });
    }

    // ============================================================
    // 2. Web Contexts (Image 2)
    // ============================================================

    @Test
    void testWebContextsModelAndManager() {
        WebContextsSettingsManager manager = WebContextsSettingsManager.getInstance();

        WebContextsSettings settings = new WebContextsSettings();
        assertTrue(settings.getMappings().isEmpty());

        settings.addMapping(new WebContextMapping("src/main/webapp", "/"));
        settings.addMapping(new WebContextMapping("src/main/webapp/api", "/api"));

        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addChangeListener(() -> notified.set(true));

        manager.setSettings(settings);
        assertTrue(notified.get());

        WebContextsSettings loaded = manager.getSettings();
        assertEquals(2, loaded.getMappings().size());

        // Resolution tests (longest prefix match)
        assertEquals("/api", manager.resolveWebContext("src/main/webapp/api/v1/user.jsp"));
        assertEquals("/", manager.resolveWebContext("src/main/webapp/index.jsp"));
        assertEquals("/", manager.resolveWebContext("other/random/file.html"));

        // Cleanup
        manager.setSettings(new WebContextsSettings());
    }

    @Test
    void testWebContextsPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsLanguagesWebContextsPage page = new SettingsLanguagesWebContextsPage();
            assertFalse(page.isModified());

            page.getTableData().add(new WebContextMapping("public_html", "/static"));
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertEquals(1, WebContextsSettingsManager.getInstance().getSettings().getMappings().size());

            page.getTableData().clear();
            assertTrue(page.isModified());

            page.revertChanges();
            assertFalse(page.isModified());
            assertEquals(1, page.getTableData().size());

            // Cleanup
            WebContextsSettingsManager.getInstance().setSettings(new WebContextsSettings());
            page.reset();
        });
    }

    // ============================================================
    // 3. XSLT (Image 3)
    // ============================================================

    @Test
    void testXsltModelAndManager() {
        XsltSettingsManager manager = XsltSettingsManager.getInstance();

        XsltSettings settings = new XsltSettings();
        assertTrue(settings.isShowAssociatedFilesInProjectView());

        settings.setShowAssociatedFilesInProjectView(false);

        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addChangeListener(() -> notified.set(true));

        manager.setSettings(settings);
        assertTrue(notified.get());

        XsltSettings loaded = manager.getSettings();
        assertFalse(loaded.isShowAssociatedFilesInProjectView());

        // Cleanup
        manager.setSettings(new XsltSettings());
    }

    @Test
    void testXsltPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsLanguagesXsltPage page = new SettingsLanguagesXsltPage();
            assertFalse(page.isModified());

            page.getShowAssociatedFilesCheck().setSelected(false);
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertFalse(XsltSettingsManager.getInstance().getSettings().isShowAssociatedFilesInProjectView());

            page.getShowAssociatedFilesCheck().setSelected(true);
            assertTrue(page.isModified());

            page.revertChanges();
            assertFalse(page.isModified());
            assertFalse(page.getShowAssociatedFilesCheck().isSelected());

            // Cleanup
            XsltSettingsManager.getInstance().setSettings(new XsltSettings());
            page.reset();
        });
    }

    // ============================================================
    // 4. XSLT File Associations (Images 4 & 5)
    // ============================================================

    @Test
    void testXsltFileAssociationsModelAndManager() {
        XsltFileAssociationsSettingsManager manager = XsltFileAssociationsSettingsManager.getInstance();

        XsltFileAssociationsSettings settings = new XsltFileAssociationsSettings();
        assertTrue(settings.getAssociations().isEmpty());

        settings.setAssociatedFiles("src/main/resources/transform.xsl", List.of("data/sample.xml", "data/schema.xsd"));

        AtomicBoolean notified = new AtomicBoolean(false);
        manager.addChangeListener(() -> notified.set(true));

        manager.setSettings(settings);
        assertTrue(notified.get());

        XsltFileAssociationsSettings loaded = manager.getSettings();
        assertEquals(1, loaded.getAssociations().size());

        List<String> files = manager.getAssociatedFiles("src/main/resources/transform.xsl");
        assertEquals(2, files.size());
        assertTrue(files.contains("data/sample.xml"));
        assertTrue(files.contains("data/schema.xsd"));

        // Cleanup
        manager.setSettings(new XsltFileAssociationsSettings());
    }

    @Test
    void testXsltFileAssociationsPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsLanguagesXsltFileAssociationsPage page = new SettingsLanguagesXsltFileAssociationsPage();
            assertFalse(page.isModified());

            page.getCurrentWorkingMap().put("/project/format.xslt", List.of("/project/input.xml"));
            assertTrue(page.isModified());

            page.apply();
            assertFalse(page.isModified());
            assertEquals(1, XsltFileAssociationsSettingsManager.getInstance().getSettings().getAssociations().size());

            page.getCurrentWorkingMap().clear();
            assertTrue(page.isModified());

            page.revertChanges();
            assertFalse(page.isModified());
            assertEquals(1, page.getCurrentWorkingMap().size());

            // Cleanup
            XsltFileAssociationsSettingsManager.getInstance().setSettings(new XsltFileAssociationsSettings());
            page.reset();
        });
    }

    // ============================================================
    // 5. SettingsDialog Integration and Tree
    // ============================================================

    @Test
    void testSettingsDialogTreeAndRouting() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsDialog dialog = new SettingsDialog(null);
            TreeItem<String> root = dialog.getTree().getRoot();
            assertNotNull(root);

            TreeItem<String> langRoot = null;
            for (TreeItem<String> top : root.getChildren()) {
                if ("Languages & Frameworks".equals(top.getValue())) {
                    langRoot = top;
                    break;
                }
            }
            assertNotNull(langRoot, "Languages & Frameworks tree node should exist");

            // Test Vue routing
            dialog.selectCategory("Vue");
            assertNotNull(dialog.getCurrentLanguagesTypeScriptVuePage(), "Vue page should be built");

            // Test Web Contexts routing
            dialog.selectCategory("Web Contexts");
            assertNotNull(dialog.getCurrentLanguagesWebContextsPage(), "Web Contexts page should be built");

            // Test XSLT routing
            dialog.selectCategory("XSLT");
            assertNotNull(dialog.getCurrentLanguagesXsltPage(), "XSLT page should be built");

            // Test XSLT File Associations routing
            dialog.selectCategory("XSLT File Associations");
            assertNotNull(dialog.getCurrentLanguagesXsltFileAssociationsPage(), "XSLT File Associations page should be built");
        });
    }

    // ============================================================
    // 6. Strict Brand Isolation Test
    // ============================================================

    @Test
    void testStrictBrandIsolation() throws Exception {
        String[] filesToCheck = {
                "src/main/java/dev/lumina/typescript/VueServiceSettings.java",
                "src/main/java/dev/lumina/typescript/VueServiceSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsLanguagesTypeScriptVuePage.java",
                "src/main/java/dev/lumina/web/WebContextMapping.java",
                "src/main/java/dev/lumina/web/WebContextsSettings.java",
                "src/main/java/dev/lumina/web/WebContextsSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsLanguagesWebContextsPage.java",
                "src/main/java/dev/lumina/xslt/XsltSettings.java",
                "src/main/java/dev/lumina/xslt/XsltSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsLanguagesXsltPage.java",
                "src/main/java/dev/lumina/xslt/XsltFileAssociation.java",
                "src/main/java/dev/lumina/xslt/XsltFileAssociationsSettings.java",
                "src/main/java/dev/lumina/xslt/XsltFileAssociationsSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsLanguagesXsltFileAssociationsPage.java"
        };

        String[] forbidden = {
                "IntelliJ", "intellij",
                "JetBrains", "jetbrains",
                "WebStorm", "webstorm",
                "IDEA"
        };

        for (String filePath : filesToCheck) {
            File f = new File(filePath);
            assertTrue(f.exists(), "File must exist: " + filePath);
            String content = Files.readString(f.toPath());
            for (String brand : forbidden) {
                assertFalse(content.contains(brand),
                        "File " + filePath + " contains forbidden brand reference: " + brand);
            }
        }
    }
}
