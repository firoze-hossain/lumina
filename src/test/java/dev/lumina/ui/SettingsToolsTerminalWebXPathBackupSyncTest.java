package dev.lumina.ui;

import dev.lumina.tools.*;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for:
 * 1. Tools > Terminal
 * 2. Tools > Web Browsers and Preview
 * 3. Tools > XPath Viewer
 * 4. Backup and Sync
 *
 * Validates models, configuration managers, UI page lifecycles,
 * SettingsDialog hierarchy/navigation, and strict brand isolation.
 */
public class SettingsToolsTerminalWebXPathBackupSyncTest {

    private static boolean javaFxAvailable = false;

    @BeforeAll
    static void initFx() {
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
        assertTrue(latch.await(10, TimeUnit.SECONDS), "JavaFX thread timed out");
        if (err[0] != null) {
            throw new RuntimeException(err[0]);
        }
    }

    // ------------------------------------------------------------- 1. Terminal

    @Test
    void testTerminalSettingsModel() {
        TerminalSettings s = new TerminalSettings();
        assertEquals("Reworked 2025", s.getTerminalEngine());
        assertTrue(s.isShowCompletionPopup());
        assertEquals("Only for parameters", s.getCompletionPopupMode());
        assertEquals("Ctrl+Space", s.getShowCompletionPopupShortcut());
        assertEquals("Enter", s.getInsertSuggestionShortcut());
        assertEquals(13.0, s.getFontSize());
        assertEquals(1.0, s.getLineHeight());
        assertEquals(1.0, s.getColumnWidth());
        assertEquals("/bin/bash", s.getShellPath());
        assertEquals("Local", s.getDefaultTabName());
        assertTrue(s.isEnforceMinimumContrastRatio());
        assertEquals(4.5, s.getContrastRatio());
        assertTrue(s.isShowSeparatorsBetweenExecutedCommands());
        assertTrue(s.isAudibleBell());
        assertTrue(s.isCloseSessionWhenItEnds());
        assertTrue(s.isMouseReporting());
        assertTrue(s.isMoveFocusToEditorWithEscape());
        assertTrue(s.isPasteOnMiddleMouseButtonClick());
        assertTrue(s.isOverrideIdeShortcuts());
        assertTrue(s.isShellIntegration());
        assertTrue(s.isHighlightHyperlinks());
        assertTrue(s.isActivateVirtualenv());
        assertTrue(s.isAddDefaultPhpInterpreterToPath());
        assertEquals("Block", s.getCursorShape());

        TerminalSettings clone = s.clone();
        assertEquals(s, clone);
        assertEquals(s.hashCode(), clone.hashCode());

        clone.setCursorShape("Underline");
        clone.setDefaultTabName("DevSession");
        clone.setShellPath("/bin/zsh");
        assertNotEquals(s, clone);
    }

    @Test
    void testTerminalSettingsManagerLifecycle() {
        TerminalSettingsManager manager = TerminalSettingsManager.getInstance();
        assertNotNull(manager);

        TerminalSettings original = manager.getSettings();
        TerminalSettings test = original.clone();
        test.setDefaultTabName("TestTab");
        test.setCursorShape("Vertical Bar");

        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addChangeListener(listener);

        try {
            manager.setSettings(test);
            assertTrue(notified.get());

            TerminalSettings loaded = manager.getSettings();
            assertEquals("TestTab", loaded.getDefaultTabName());
            assertEquals("Vertical Bar", loaded.getCursorShape());
        } finally {
            manager.removeChangeListener(listener);
            manager.setSettings(original);
        }
    }

    @Test
    void testTerminalUiPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsToolsTerminalPage page = new SettingsToolsTerminalPage();
            assertNotNull(page);
            assertFalse(page.isModified());

            AtomicBoolean modifiedNotified = new AtomicBoolean(false);
            page.setOnModified(() -> modifiedNotified.set(true));

            page.getDefaultTabNameField().setText("NewTabName");
            assertTrue(page.isModified());
            assertTrue(modifiedNotified.get());

            page.revertChanges();
            assertFalse(page.isModified());
        });
    }

    // ------------------------------------------------------------- 2. Web Browsers and Preview

    @Test
    void testWebBrowsersSettingsModel() {
        WebBrowsersSettings s = new WebBrowsersSettings();
        assertEquals("System default", s.getDefaultBrowser());
        assertEquals("", s.getCustomBrowserPath());
        assertTrue(s.isShowPopupForHtml());
        assertFalse(s.isShowPopupForXml());
        assertEquals("On Save", s.getReloadInBrowser());
        assertEquals("On Save", s.getReloadInBuiltInPreview());
        assertEquals(63342, s.getBuiltInServerPort());
        assertFalse(s.isCanAcceptExternalConnections());
        assertFalse(s.isAllowUnsignedRequests());

        assertFalse(s.getBrowsers().isEmpty());
        assertTrue(s.getBrowsers().stream().anyMatch(b -> "Chrome".equals(b.getName()) && b.isActive()));
        assertTrue(s.getBrowsers().stream().anyMatch(b -> "Firefox".equals(b.getName()) && b.isActive()));

        WebBrowsersSettings clone = s.clone();
        assertEquals(s, clone);
        assertEquals(s.hashCode(), clone.hashCode());

        clone.setDefaultBrowser("Chrome");
        clone.setBuiltInServerPort(8080);
        assertNotEquals(s, clone);
    }

    @Test
    void testWebBrowsersSettingsManagerLifecycle() {
        WebBrowsersSettingsManager manager = WebBrowsersSettingsManager.getInstance();
        assertNotNull(manager);

        WebBrowsersSettings original = manager.getSettings();
        WebBrowsersSettings test = original.clone();
        test.setDefaultBrowser("Firefox");
        test.setShowPopupForXml(true);

        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addChangeListener(listener);

        try {
            manager.setSettings(test);
            assertTrue(notified.get());

            WebBrowsersSettings loaded = manager.getSettings();
            assertEquals("Firefox", loaded.getDefaultBrowser());
            assertTrue(loaded.isShowPopupForXml());
        } finally {
            manager.removeChangeListener(listener);
            manager.setSettings(original);
        }
    }

    @Test
    void testWebBrowsersUiPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsToolsWebBrowsersPage page = new SettingsToolsWebBrowsersPage();
            assertNotNull(page);
            assertFalse(page.isModified());

            AtomicBoolean modifiedNotified = new AtomicBoolean(false);
            page.setOnModified(() -> modifiedNotified.set(true));

            page.getServerPortField().setText("8081");
            assertTrue(page.isModified());
            assertTrue(modifiedNotified.get());

            page.revertChanges();
            assertFalse(page.isModified());
        });
    }

    // ------------------------------------------------------------- 3. XPath Viewer

    @Test
    void testXPathViewerSettingsModel() {
        XPathViewerSettings s = new XPathViewerSettings();
        assertTrue(s.isScrollFirstHitIntoVisibleArea());
        assertTrue(s.isUseNodeAtCursorAsContextNode());
        assertTrue(s.isHighlightOnlyStartTag());
        assertTrue(s.isAddErrorStripeMarkers());
        assertEquals("#FFD578", s.getHighlightColor());
        assertEquals("#C2FFD4", s.getContextNodeColor());

        XPathViewerSettings clone = s.clone();
        assertEquals(s, clone);
        assertEquals(s.hashCode(), clone.hashCode());

        clone.setHighlightColor("#FF0000");
        clone.setScrollFirstHitIntoVisibleArea(false);
        assertNotEquals(s, clone);
    }

    @Test
    void testXPathViewerSettingsManagerLifecycle() {
        XPathViewerSettingsManager manager = XPathViewerSettingsManager.getInstance();
        assertNotNull(manager);

        XPathViewerSettings original = manager.getSettings();
        XPathViewerSettings test = original.clone();
        test.setHighlightOnlyStartTag(false);
        test.setContextNodeColor("#AABBCC");

        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addChangeListener(listener);

        try {
            manager.setSettings(test);
            assertTrue(notified.get());

            XPathViewerSettings loaded = manager.getSettings();
            assertFalse(loaded.isHighlightOnlyStartTag());
            assertEquals("#AABBCC", loaded.getContextNodeColor());
        } finally {
            manager.removeChangeListener(listener);
            manager.setSettings(original);
        }
    }

    @Test
    void testXPathViewerUiPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsToolsXPathViewerPage page = new SettingsToolsXPathViewerPage();
            assertNotNull(page);
            assertFalse(page.isModified());

            AtomicBoolean modifiedNotified = new AtomicBoolean(false);
            page.setOnModified(() -> modifiedNotified.set(true));

            page.getScrollFirstHitCheck().setSelected(false);
            assertTrue(page.isModified());
            assertTrue(modifiedNotified.get());

            page.revertChanges();
            assertFalse(page.isModified());
        });
    }

    // ------------------------------------------------------------- 4. Backup and Sync

    @Test
    void testBackupAndSyncSettingsModel() {
        BackupAndSyncSettings s = new BackupAndSyncSettings();
        assertTrue(s.isEnableBackupAndSync());
        assertEquals("15103202@iubat.edu", s.getSyncAccount());
        assertTrue(s.isSyncUi());
        assertTrue(s.isSyncCodeAndSystem());
        assertTrue(s.isSyncKeymaps());
        assertTrue(s.isSyncPlugins());
        assertTrue(s.isSyncTools());

        BackupAndSyncSettings clone = s.clone();
        assertEquals(s, clone);
        assertEquals(s.hashCode(), clone.hashCode());

        clone.setEnableBackupAndSync(false);
        clone.setSyncAccount("user@example.com");
        assertNotEquals(s, clone);
    }

    @Test
    void testBackupAndSyncSettingsManagerLifecycle() {
        BackupAndSyncSettingsManager manager = BackupAndSyncSettingsManager.getInstance();
        assertNotNull(manager);

        BackupAndSyncSettings original = manager.getSettings();
        BackupAndSyncSettings test = original.clone();
        test.setSyncAccount("developer@lumina.dev");
        test.setSyncPlugins(false);

        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addChangeListener(listener);

        try {
            manager.setSettings(test);
            assertTrue(notified.get());

            BackupAndSyncSettings loaded = manager.getSettings();
            assertEquals("developer@lumina.dev", loaded.getSyncAccount());
            assertFalse(loaded.isSyncPlugins());
        } finally {
            manager.removeChangeListener(listener);
            manager.setSettings(original);
        }
    }

    @Test
    void testBackupAndSyncUiPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsBackupAndSyncPage page = new SettingsBackupAndSyncPage();
            assertNotNull(page);
            assertFalse(page.isModified());

            AtomicBoolean modifiedNotified = new AtomicBoolean(false);
            page.setOnModified(() -> modifiedNotified.set(true));

            page.getEnableBackupCheck().setSelected(false);
            assertTrue(page.isModified());
            assertTrue(modifiedNotified.get());

            page.revertChanges();
            assertFalse(page.isModified());
        });
    }

    // ------------------------------------------------------------- 5. SettingsDialog Hierarchy & Navigation

    @Test
    void testSettingsDialogHierarchyAndNavigation() throws Exception {
        if (!javaFxAvailable) return;
        runOnFx(() -> {
            SettingsDialog dialog = new SettingsDialog(null);
            assertNotNull(dialog);

            // Locate Tools category in tree
            TreeItem<String> root = dialog.getTree().getRoot();
            assertNotNull(root);

            TreeItem<String> toolsItem = null;
            TreeItem<String> backupAndSyncItem = null;
            for (TreeItem<String> child : root.getChildren()) {
                if ("Tools".equals(child.getValue())) {
                    toolsItem = child;
                } else if ("Backup and Sync".equals(child.getValue())) {
                    backupAndSyncItem = child;
                }
            }
            assertNotNull(toolsItem, "Tools node should exist");
            assertNotNull(backupAndSyncItem, "Backup and Sync node should exist at root level");

            // Verify Terminal, Web Browsers and Preview, XPath Viewer exist under Tools
            TreeItem<String> terminalItem = null;
            TreeItem<String> webBrowsersItem = null;
            TreeItem<String> xpathItem = null;
            for (TreeItem<String> child : toolsItem.getChildren()) {
                if ("Terminal".equals(child.getValue())) {
                    terminalItem = child;
                } else if ("Web Browsers and Preview".equals(child.getValue())) {
                    webBrowsersItem = child;
                } else if ("XPath Viewer".equals(child.getValue())) {
                    xpathItem = child;
                }
            }
            assertNotNull(terminalItem, "Terminal should be a child of Tools");
            assertNotNull(webBrowsersItem, "Web Browsers and Preview should be a child of Tools");
            assertNotNull(xpathItem, "XPath Viewer should be a child of Tools");

            // Test navigation to Terminal
            dialog.selectCategory("Tools", "Terminal");
            assertNotNull(dialog.getCurrentToolsTerminalPage());

            // Test navigation to Web Browsers and Preview
            dialog.selectCategory("Tools", "Web Browsers and Preview");
            assertNotNull(dialog.getCurrentToolsWebBrowsersPage());

            // Test navigation to XPath Viewer
            dialog.selectCategory("Tools", "XPath Viewer");
            assertNotNull(dialog.getCurrentToolsXPathViewerPage());

            // Test navigation to Backup and Sync
            dialog.selectCategory("Backup and Sync");
            assertNotNull(dialog.getCurrentBackupAndSyncPage());
        });
    }

    // ------------------------------------------------------------- 6. Strict Brand Isolation

    @Test
    void testStrictBrandIsolation() throws Exception {
        String[] filesToCheck = {
                "src/main/java/dev/lumina/tools/TerminalSettings.java",
                "src/main/java/dev/lumina/tools/TerminalSettingsManager.java",
                "src/main/java/dev/lumina/tools/WebBrowserEntry.java",
                "src/main/java/dev/lumina/tools/WebBrowsersSettings.java",
                "src/main/java/dev/lumina/tools/WebBrowsersSettingsManager.java",
                "src/main/java/dev/lumina/tools/XPathViewerSettings.java",
                "src/main/java/dev/lumina/tools/XPathViewerSettingsManager.java",
                "src/main/java/dev/lumina/tools/BackupAndSyncSettings.java",
                "src/main/java/dev/lumina/tools/BackupAndSyncSettingsManager.java",
                "src/main/java/dev/lumina/ui/SettingsToolsTerminalPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsWebBrowsersPage.java",
                "src/main/java/dev/lumina/ui/SettingsToolsXPathViewerPage.java",
                "src/main/java/dev/lumina/ui/SettingsBackupAndSyncPage.java"
        };

        String[] forbiddenKeywords = {
                "Intelli" + "J",
                "Py" + "Charm",
                "Web" + "Storm",
                "Php" + "Storm",
                "Go" + "Land",
                "CLion",
                "Data" + "Grip",
                "Fleet",
                "Rider"
        };

        for (String relPath : filesToCheck) {
            String content = Files.readString(Paths.get(relPath));
            for (String kw : forbiddenKeywords) {
                assertFalse(content.contains(kw),
                        "File " + relPath + " must not contain forbidden competitor keyword: " + kw);
            }
        }
    }
}
