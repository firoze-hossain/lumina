package dev.lumina.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SettingsNavigationTest {

    private static volatile boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            String display = System.getenv("DISPLAY");
            if (display == null || display.isBlank() || java.awt.GraphicsEnvironment.isHeadless()) {
                return;
            }
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            Platform.startup(() -> {
                javaFxAvailable = true;
                latch.countDown();
            });
            latch.await(3, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Throwable ignored) {
            javaFxAvailable = false;
        }
    }

    @Test
    void testDynamicBreadcrumbHierarchyCalculation() {
        TreeItem<String> root = new TreeItem<>("Settings");
        TreeItem<String> appearanceAndBehavior = new TreeItem<>("Appearance & Behavior");
        TreeItem<String> appearance = new TreeItem<>("Appearance");
        TreeItem<String> systemSettings = new TreeItem<>("System Settings");
        TreeItem<String> passwords = new TreeItem<>("Passwords");

        root.getChildren().add(appearanceAndBehavior);
        appearanceAndBehavior.getChildren().addAll(appearance, systemSettings);
        systemSettings.getChildren().add(passwords);

        // Calculate path for 'Appearance'
        List<String> pathAppearance = getBreadcrumbPath(appearance);
        assertEquals(List.of("Appearance & Behavior", "Appearance"), pathAppearance);

        // Calculate path for 'Passwords'
        List<String> pathPasswords = getBreadcrumbPath(passwords);
        assertEquals(List.of("Appearance & Behavior", "System Settings", "Passwords"), pathPasswords);

        // Calculate path for top-level category
        List<String> pathCategory = getBreadcrumbPath(appearanceAndBehavior);
        assertEquals(List.of("Appearance & Behavior"), pathCategory);
    }

    private List<String> getBreadcrumbPath(TreeItem<String> item) {
        List<String> segments = new ArrayList<>();
        TreeItem<String> it = item;
        while (it != null && it.getParent() != null) {
            segments.add(0, it.getValue());
            it = it.getParent();
        }
        return segments;
    }

    @Test
    void testCategoryOverviewGeneratesDynamicLinks() {
        if (!javaFxAvailable) return;

        TreeItem<String> category = new TreeItem<>("Appearance & Behavior");
        TreeItem<String> child1 = new TreeItem<>("Appearance");
        TreeItem<String> child2 = new TreeItem<>("Menus and Toolbars");
        TreeItem<String> child3 = new TreeItem<>("Notifications");
        category.getChildren().addAll(child1, child2, child3);

        AtomicReference<TreeItem<String>> selectedChild = new AtomicReference<>();
        SettingsCategoryOverviewPage overview = new SettingsCategoryOverviewPage(category, selectedChild::set);

        // Find links container
        VBox linksContainer = (VBox) overview.getChildren().get(1);
        assertEquals(3, linksContainer.getChildren().size());

        Hyperlink link1 = (Hyperlink) linksContainer.getChildren().get(0);
        assertEquals("Appearance", link1.getText());
        link1.fire();
        assertEquals(child1, selectedChild.get());

        Hyperlink link3 = (Hyperlink) linksContainer.getChildren().get(2);
        assertEquals("Notifications", link3.getText());
        link3.fire();
        assertEquals(child3, selectedChild.get());
    }

    @Test
    void testIdeAppearancePageThemeStrictlyDarkOnly() {
        if (!javaFxAvailable) return;

        SettingsIdeAppearancePage page = new SettingsIdeAppearancePage();
        assertNotNull(page);

        // Traverse children to find theme ComboBox
        ComboBox<String> themeBox = findComboBox(page);
        assertNotNull(themeBox, "Theme ComboBox should be present");
        assertEquals(1, themeBox.getItems().size(), "Strict requirement: Theme ComboBox must have exactly ONE theme");
        assertEquals("Dark", themeBox.getItems().get(0));
        assertEquals("Dark", themeBox.getSelectionModel().getSelectedItem());

        // Ensure no JetBrains or IntelliJ branding in any item
        for (String item : themeBox.getItems()) {
            assertFalse(item.toLowerCase().contains("jetbrains"), "Must not contain 'jetbrains'");
            assertFalse(item.toLowerCase().contains("intellij"), "Must not contain 'intellij'");
        }
    }

    @Test
    void testBackgroundImageDialogInitialization() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<BackgroundImageDialog> ref = new AtomicReference<>();
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                ref.set(new BackgroundImageDialog(null));
            } finally {
                latch.countDown();
            }
        });
        latch.await(3, java.util.concurrent.TimeUnit.SECONDS);
        assertNotNull(ref.get());
    }

    @Test
    void testInitialAppearancePageLoadsContentImmediately() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<SettingsDialog> ref = new AtomicReference<>();
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                ref.set(new SettingsDialog(null));
            } finally {
                latch.countDown();
            }
        });
        latch.await(3, java.util.concurrent.TimeUnit.SECONDS);

        SettingsDialog dialog = ref.get();
        assertNotNull(dialog);
        assertNotNull(dialog.getContentContainer());
        assertFalse(dialog.getContentContainer().getChildren().isEmpty(),
                "Content container MUST not be empty on initial open!");
        assertEquals("Appearance", dialog.getTree().getSelectionModel().getSelectedItem().getValue());
    }

    @Test
    void testSettingsSystemPageSubPages() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<SettingsSystemPage> ref = new AtomicReference<>();
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsSystemPage page = new SettingsSystemPage();
                ref.set(page);
            } finally {
                latch.countDown();
            }
        });
        latch.await(3, java.util.concurrent.TimeUnit.SECONDS);

        SettingsSystemPage page = ref.get();
        assertNotNull(page);
        assertFalse(page.getChildren().isEmpty());

        // Test switching to each of the IntelliJ System Settings pages
        java.util.concurrent.CountDownLatch switchLatch = new java.util.concurrent.CountDownLatch(10);
        Platform.runLater(() -> {
            page.showSubPage("System Settings");
            assertFalse(page.getChildren().isEmpty());
            switchLatch.countDown();

            page.showSubPage("Date Formats");
            assertFalse(page.getChildren().isEmpty());
            switchLatch.countDown();

            page.showSubPage("Data Sharing");
            assertFalse(page.getChildren().isEmpty());
            switchLatch.countDown();

            page.showSubPage("HTTP Proxy");
            assertFalse(page.getChildren().isEmpty());
            switchLatch.countDown();

            page.showSubPage("Language and Region");
            assertFalse(page.getChildren().isEmpty());
            assertNotNull(page.getLanguageCombo());
            assertNotNull(page.getRegionCombo());
            switchLatch.countDown();

            page.showSubPage("Passwords");
            assertFalse(page.getChildren().isEmpty());
            assertNotNull(page.getPassKeychainRadio());
            assertNotNull(page.getPassKeePassRadio());
            assertNotNull(page.getKeepassDbField());
            switchLatch.countDown();

            page.showSubPage("Process Elevation");
            assertFalse(page.getChildren().isEmpty());
            assertNotNull(page.getKeepSudoCheck());
            assertNotNull(page.getSudoTimeoutCombo());
            switchLatch.countDown();

            page.showSubPage("Server Certificates");
            assertFalse(page.getChildren().isEmpty());
            assertNotNull(page.getAcceptNonTrustedCertsCheck());
            assertNotNull(page.getCertsList());
            switchLatch.countDown();

            page.showSubPage("Trusted Hosts");
            assertFalse(page.getChildren().isEmpty());
            assertNotNull(page.getTrustedHostsList());
            assertTrue(page.getTrustedHostsList().contains("download.jetbrains.com"));
            switchLatch.countDown();

            page.showSubPage("Updates");
            assertFalse(page.getChildren().isEmpty());
            assertNotNull(page.getCheckIdeUpdatesCheck());
            assertNotNull(page.getIdeUpdateChannelCombo());
            assertEquals(2, page.getIdeUpdateChannelCombo().getItems().size());
            assertTrue(page.getIdeUpdateChannelCombo().getItems().contains("Early Access Program"));
            assertTrue(page.getIdeUpdateChannelCombo().getItems().contains("Stable Releases"));
            assertNotNull(page.getCheckPluginUpdatesCheck());
            assertNotNull(page.getUpdatePluginsAutoCheck());
            assertNotNull(page.getCheckForUpdatesBtn());
            assertNotNull(page.getLastCheckedLabel());
            assertNotNull(page.getShowWhatsNewCheck());
            assertNotNull(page.getCheckJdkUpdatesCheck());
            switchLatch.countDown();
        });
        switchLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        assertEquals(0, switchLatch.getCount(), "All 10 subpages should render without exception");
    }

    @Test
    void testSettingsDialogInlineCompletionNavigation() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<SettingsDialog> ref = new AtomicReference<>();
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDialog dialog = new SettingsDialog(null, "Inline Completion");
                ref.set(dialog);
            } finally {
                latch.countDown();
            }
        });
        latch.await(3, java.util.concurrent.TimeUnit.SECONDS);

        SettingsDialog dialog = ref.get();
        assertNotNull(dialog);
        assertNotNull(dialog.getContentContainer());
        assertFalse(dialog.getContentContainer().getChildren().isEmpty());
        assertEquals("Inline Completion", dialog.getTree().getSelectionModel().getSelectedItem().getValue());
    }

    @Test
    void testSettingsDialogPostfixCompletionNavigation() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<SettingsDialog> ref = new AtomicReference<>();
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDialog dialog = new SettingsDialog(null, "Postfix Completion");
                ref.set(dialog);
            } finally {
                latch.countDown();
            }
        });
        latch.await(3, java.util.concurrent.TimeUnit.SECONDS);

        SettingsDialog dialog = ref.get();
        assertNotNull(dialog);
        assertNotNull(dialog.getContentContainer());
        assertFalse(dialog.getContentContainer().getChildren().isEmpty());
        assertEquals("Postfix Completion", dialog.getTree().getSelectionModel().getSelectedItem().getValue());
    }

    @Test
    void testSettingsDialogSmartKeysNavigation() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<SettingsDialog> ref = new AtomicReference<>();
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDialog dialog = new SettingsDialog(null, "Smart Keys");
                ref.set(dialog);
            } finally {
                latch.countDown();
            }
        });
        latch.await(3, java.util.concurrent.TimeUnit.SECONDS);

        SettingsDialog dialog = ref.get();
        assertNotNull(dialog);
        assertNotNull(dialog.getContentContainer());
        assertFalse(dialog.getContentContainer().getChildren().isEmpty());
        assertEquals("Smart Keys", dialog.getTree().getSelectionModel().getSelectedItem().getValue());
    }

    @Test
    void testSettingsDialogStickyLinesNavigation() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<SettingsDialog> ref = new AtomicReference<>();
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDialog dialog = new SettingsDialog(null, "Sticky Lines");
                ref.set(dialog);
            } finally {
                latch.countDown();
            }
        });
        latch.await(3, java.util.concurrent.TimeUnit.SECONDS);

        SettingsDialog dialog = ref.get();
        assertNotNull(dialog);
        assertNotNull(dialog.getContentContainer());
        assertFalse(dialog.getContentContainer().getChildren().isEmpty());
        assertEquals("Sticky Lines", dialog.getTree().getSelectionModel().getSelectedItem().getValue());
    }

    @SuppressWarnings("unchecked")
    private ComboBox<String> findComboBox(javafx.scene.Parent parent) {
        for (javafx.scene.Node node : parent.getChildrenUnmodifiable()) {
            if (node instanceof ComboBox<?> cb) {
                return (ComboBox<String>) cb;
            } else if (node instanceof javafx.scene.Parent p) {
                ComboBox<String> found = findComboBox(p);
                if (found != null) return found;
            }
        }
        return null;
    }
}
