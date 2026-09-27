package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;
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

class SettingsColorSchemeRDocToRustTest {

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
    // 1. RDoc Page Tests (media_1790478261399.png)
    // -------------------------------------------------------------------------

    @Test
    void testRDocPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeRDocPage page = new SettingsColorSchemeRDocPage();

                // 6 items in tree: Directive, Email, Heading, Identifier, Tag, Url
                assertEquals(6, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Heading (bold=true by default)
                assertEquals("Heading", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Heading", page.getSelectedKey());
                assertTrue(page.getBoldCheck().isSelected());
                assertFalse(page.getItalicCheck().isSelected());

                // Preview has code lines
                assertTrue(page.getCodeLinesBox().getChildren().size() >= 6);

                // Modification and Apply
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getItalicCheck().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.apply();
                assertFalse(page.isModified());

                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                assertTrue(s.getAttribute(s.getActiveSchemeName(), "Heading").isItalic());

                // Select Email
                page.selectTreeItem("Email");
                assertEquals("Email", page.getSelectedKey());

            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 2. RegExp Page Tests (media_1790478266097.png)
    // -------------------------------------------------------------------------

    @Test
    void testRegExpPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeRegExpPage page = new SettingsColorSchemeRegExpPage();

                // 18 top-level items in tree
                assertEquals(18, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Comment (#7A7E85, inherits from Comments->Line comment)
                assertEquals("Comment", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Comment", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("7A7E85", page.getForegroundSwatch().getText());
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Comments->Line comment", page.getInheritTargetLabel().getText());
                assertEquals("(Language Defaults)", page.getInheritScopeLabel().getText());

                // Preview has code lines
                assertTrue(page.getCodeLinesBox().getChildren().size() >= 3);

                // Modification and Apply
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getBoldCheck().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.apply();
                assertFalse(page.isModified());

                // Select Invalid escape sequence
                page.selectTreeItem("Invalid escape sequence");
                assertEquals("Invalid escape sequence", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("F75464", page.getForegroundSwatch().getText());

            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 3. Ruby Page Tests (media_1790478262001.png)
    // -------------------------------------------------------------------------

    @Test
    void testRubyPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeRubyPage page = new SettingsColorSchemeRubyPage();

                // 10 top-level groups/items
                assertEquals(10, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Constants // Constant declaration
                assertEquals("Constant declaration", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Constants // Constant declaration", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("FFC66D", page.getForegroundSwatch().getText());
                assertTrue(page.getItalicCheck().isSelected());
                assertFalse(page.getBoldCheck().isSelected());
                assertFalse(page.getInheritCheck().isSelected());
                assertEquals("Identifiers->Constant", page.getInheritTargetLabel().getText());
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

                // Select Method call under Methods
                page.selectTreeItem("Methods // Method call");
                assertEquals("Methods // Method call", page.getSelectedKey());
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Identifiers->Function call", page.getInheritTargetLabel().getText());

            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 4. Rust Page Tests (media_1790478264456.png & media_1790478266841.png)
    // -------------------------------------------------------------------------

    @Test
    void testRustPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeRustPage page = new SettingsColorSchemeRustPage();

                // 16 top-level groups/items
                assertEquals(16, page.getCategoryTree().getRoot().getChildren().size());

                // Default selection: Variables // Default
                assertEquals("Default", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Variables // Default", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("BCBEC4", page.getForegroundSwatch().getText());
                assertTrue(page.getInheritCheck().isSelected());
                assertEquals("Identifiers->Default", page.getInheritTargetLabel().getText());

                // Select Functions // Function declaration (media_1790478266841.png)
                page.selectTreeItem("Functions // Function declaration");
                assertEquals("Functions // Function declaration", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("6AA2D7", page.getForegroundSwatch().getText());
                assertFalse(page.getInheritCheck().isSelected());
                assertEquals("Identifiers->Function declaration", page.getInheritTargetLabel().getText());

                // Preview has code lines
                assertTrue(page.getCodeLinesBox().getChildren().size() >= 8);

                // Modification and Apply
                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                page.getItalicCheck().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                page.apply();
                assertFalse(page.isModified());

            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 5. SettingsDialog Navigation & Integration Tests
    // -------------------------------------------------------------------------

    @Test
    void testSettingsDialogIntegration() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDialog dialog = new SettingsDialog(new Stage());

                TreeItem<String> csNode = null;
                for (TreeItem<String> child : dialog.getTree().getRoot().getChildren()) {
                    if ("Editor".equals(child.getValue())) {
                        for (TreeItem<String> edChild : child.getChildren()) {
                            if ("Color Scheme".equals(edChild.getValue())) {
                                csNode = edChild;
                                break;
                            }
                        }
                    }
                }
                assertNotNull(csNode);

                // Navigate to RDoc
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("RDoc".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                assertNotNull(dialog.getCurrentColorSchemeRDocPage());

                // Navigate to RegExp
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("RegExp".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                assertNotNull(dialog.getCurrentColorSchemeRegExpPage());

                // Navigate to Ruby
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("Ruby".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                assertNotNull(dialog.getCurrentColorSchemeRubyPage());

                // Navigate to Rust
                for (TreeItem<String> child : csNode.getChildren()) {
                    if ("Rust".equals(child.getValue())) {
                        dialog.getTree().getSelectionModel().select(child);
                        break;
                    }
                }
                assertNotNull(dialog.getCurrentColorSchemeRustPage());

            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 6. Brand Isolation Test
    // -------------------------------------------------------------------------

    @Test
    void testBrandIsolation() throws Exception {
        Pattern competitorPattern = Pattern.compile("(?i)\\b(intellij|jetbrains|idea)\\b");

        List<String> filesToCheck = List.of(
                "src/main/java/dev/lumina/ui/SettingsColorSchemeRDocPage.java",
                "src/main/java/dev/lumina/ui/SettingsColorSchemeRegExpPage.java",
                "src/main/java/dev/lumina/ui/SettingsColorSchemeRubyPage.java",
                "src/main/java/dev/lumina/ui/SettingsColorSchemeRustPage.java"
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
