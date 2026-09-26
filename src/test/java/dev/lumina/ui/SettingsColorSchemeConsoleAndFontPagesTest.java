package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import dev.lumina.settings.EditorColorSchemeSettings.ConsoleFontPreferences;
import dev.lumina.settings.EditorColorSchemeSettings.SchemeFontPreferences;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SettingsColorSchemeConsoleAndFontPagesTest {

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

    @Test
    void testConsoleColorsPageStructureAndSelection() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                SettingsConsoleColorsPage page = new SettingsConsoleColorsPage();
                assertFalse(page.isModified());

                // Tree structure check
                TreeItem<String> root = page.getCategoryTree().getRoot();
                assertNotNull(root);
                assertEquals(5, root.getChildren().size());

                assertEquals("ANSI colors", root.getChildren().get(0).getValue());
                assertEquals("Console", root.getChildren().get(1).getValue());
                assertEquals("Log console", root.getChildren().get(2).getValue());
                assertEquals("Reworked terminal", root.getChildren().get(3).getValue());
                assertEquals("Terminal", root.getChildren().get(4).getValue());

                // Verify Reworked terminal has all 34 entries matching screenshots 3 and 5
                TreeItem<String> reworkedTerminalItem = root.getChildren().get(3);
                assertEquals(34, reworkedTerminalItem.getChildren().size());

                // 1. Image 1: Console // Error output
                page.selectTreeItem("Console // Error output");
                assertTrue(page.getAttributeEditorBox().isVisible());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("F75464", page.getForegroundSwatch().getText());
                assertFalse(page.getBackgroundCheck().isSelected());
                assertFalse(page.getBoldCheck().isSelected());
                assertFalse(page.getItalicCheck().isSelected());

                // 2. Image 2: Log console // Error and Expired entry
                page.selectTreeItem("Log console // Error");
                assertTrue(page.getAttributeEditorBox().isVisible());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("F75464", page.getForegroundSwatch().getText());

                page.selectTreeItem("Log console // Expired entry");
                assertTrue(page.getAttributeEditorBox().isVisible());
                assertTrue(page.getEffectsCheck().isSelected());
                assertEquals(dev.lumina.settings.EditorColorSchemeSettings.EffectType.STRIKEOUT, page.getEffectTypeCombo().getValue());

                // 3. Image 3: Reworked terminal // Reworked background gradient start
                page.selectTreeItem("Reworked terminal // Reworked background gradient start");
                assertTrue(page.getAttributeEditorBox().isVisible());
                assertTrue(page.getBackgroundCheck().isSelected());
                assertEquals("2B2D30", page.getBackgroundSwatch().getText());
                assertEquals(dev.lumina.settings.EditorColorSchemeSettings.EffectType.UNDERSCORED, page.getEffectTypeCombo().getValue());

                // 4. Image 4: Terminal // Command to run using IDE
                page.selectTreeItem("Terminal // Command to run using IDE");
                assertTrue(page.getAttributeEditorBox().isVisible());
                assertTrue(page.getBackgroundCheck().isSelected());
                assertEquals("40503C", page.getBackgroundSwatch().getText());

                // 5. Image 5: Reworked terminal // Bright Black
                page.selectTreeItem("Reworked terminal // Bright Black");
                assertTrue(page.getAttributeEditorBox().isVisible());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("4E5157", page.getForegroundSwatch().getText());
                assertTrue(page.getBackgroundCheck().isSelected());
                assertEquals("4E5157", page.getBackgroundSwatch().getText());

                // Test dirty state and listener
                AtomicBoolean modifiedNotified = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedNotified.set(true));

                page.getBoldCheck().setSelected(!page.getBoldCheck().isSelected());
                assertTrue(page.isModified());
                assertTrue(modifiedNotified.get());

                page.apply();
                assertFalse(page.isModified());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (error.get() != null) throw new AssertionError(error.get());
    }

    @Test
    void testCodeWithMePageStructureAndSelection() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                SettingsCodeWithMePage page = new SettingsCodeWithMePage();
                assertFalse(page.isModified());

                // 12 user items
                TreeItem<String> root = page.getCategoryTree().getRoot();
                assertNotNull(root);
                assertEquals(12, root.getChildren().size());
                assertEquals("User 1 cursor", root.getChildren().get(0).getValue());
                assertEquals("User 1 selection", root.getChildren().get(1).getValue());
                assertEquals("User 2 cursor", root.getChildren().get(2).getValue());
                assertEquals("User 2 selection", root.getChildren().get(3).getValue());

                // Select User 2 selection (matches Screenshot 2)
                page.selectTreeItem("User 2 selection");
                assertTrue(page.getAttributeEditorBox().isVisible());
                assertFalse(page.getForegroundCheck().isSelected());
                assertTrue(page.getBackgroundCheck().isSelected());
                assertEquals("5E3838", page.getBackgroundSwatch().getText());

                // Select User 1 cursor
                page.selectTreeItem("User 1 cursor");
                assertTrue(page.getAttributeEditorBox().isVisible());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("59A869", page.getForegroundSwatch().getText());

                // Test dirty state
                AtomicBoolean modifiedNotified = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedNotified.set(true));

                page.getItalicCheck().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedNotified.get());

                page.apply();
                assertFalse(page.isModified());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (error.get() != null) throw new AssertionError(error.get());
    }

    @Test
    void testConsoleFontPageToggleAndPreferences() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                SettingsConsoleFontPage page = new SettingsConsoleFontPage();
                assertFalse(page.isModified());

                // Initially false (default)
                assertFalse(page.getUseConsoleFontCheck().isSelected());
                assertTrue(page.getFontCombo().getParent().isDisabled());

                // Toggle on
                page.getUseConsoleFontCheck().setSelected(true);
                assertFalse(page.getFontCombo().getParent().isDisabled());
                assertTrue(page.isModified());

                // Change size
                page.getSizeSpinner().getValueFactory().setValue(15.0);
                page.getFontCombo().setValue("Menlo");

                AtomicBoolean modifiedNotified = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedNotified.set(true));

                page.getEnableLigaturesCheck().setSelected(true);
                assertTrue(modifiedNotified.get());
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());

                // Verify persistence
                ConsoleFontPreferences prefs = EditorColorSchemeSettings.getInstance().getConsoleFontPreferences();
                assertTrue(prefs.isUseConsoleFont());
                assertEquals("Menlo", prefs.getFontFamily());
                assertEquals(15.0, prefs.getFontSize());
                assertTrue(prefs.isEnableLigatures());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (error.get() != null) throw new AssertionError(error.get());
    }

    @Test
    void testColorSchemeFontPageToggleAndPreferences() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeFontPage page = new SettingsColorSchemeFontPage();
                assertFalse(page.isModified());

                // Initially false (default)
                assertFalse(page.getUseColorSchemeFontCheck().isSelected());
                assertTrue(page.getFontCombo().getParent().isDisabled());

                // Toggle on
                page.getUseColorSchemeFontCheck().setSelected(true);
                assertFalse(page.getFontCombo().getParent().isDisabled());
                assertTrue(page.isModified());

                // Change size & font
                page.getSizeSpinner().getValueFactory().setValue(14.0);
                page.getFontCombo().setValue("Fira Code");

                page.apply();
                assertFalse(page.isModified());

                // Verify persistence
                SchemeFontPreferences prefs = EditorColorSchemeSettings.getInstance().getSchemeFontPreferences();
                assertTrue(prefs.isUseSchemeFont());
                assertEquals("Fira Code", prefs.getFontFamily());
                assertEquals(14.0, prefs.getFontSize());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (error.get() != null) throw new AssertionError(error.get());
    }

    @Test
    void testStrictBrandIsolation() throws IOException {
        String[] filesToCheck = {
                "src/main/java/dev/lumina/ui/SettingsConsoleColorsPage.java",
                "src/main/java/dev/lumina/ui/SettingsCodeWithMePage.java",
                "src/main/java/dev/lumina/ui/SettingsConsoleFontPage.java",
                "src/main/java/dev/lumina/ui/SettingsColorSchemeFontPage.java"
        };

        for (String filePath : filesToCheck) {
            String content = Files.readString(Path.of(filePath));
            assertFalse(content.contains("IntelliJ"), "File " + filePath + " must NOT contain 'IntelliJ'");
            assertFalse(content.contains("intellij"), "File " + filePath + " must NOT contain 'intellij'");
            // Ensure no JetBrains branding (JetBrains Mono font is permitted)
            String contentWithoutFont = content.replaceAll("JetBrains Mono", "");
            assertFalse(contentWithoutFont.contains("JetBrains"), "File " + filePath + " must NOT contain 'JetBrains' outside font name");
        }
    }
}
