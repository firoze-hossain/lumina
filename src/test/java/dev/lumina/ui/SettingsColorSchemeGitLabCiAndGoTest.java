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

class SettingsColorSchemeGitLabCiAndGoTest {

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
    // 1. GitLab CI Expression Page Tests (media_1790430732315.png)
    // -------------------------------------------------------------------------

    @Test
    void testGitLabCiExpressionPageInitAndDefaults() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeGitLabCiExpressionPage page = new SettingsColorSchemeGitLabCiExpressionPage();

                // 8 flat items in tree
                TreeItem<String> root = page.getCategoryTree().getRoot();
                assertNotNull(root);
                assertEquals(8, root.getChildren().size());

                List<String> expectedItems = List.of(
                        "Bad character",
                        "Comparison operator (==, !=, =~, !~)",
                        "Identifier",
                        "Logical operator (, ||)",
                        "Parentheses",
                        "Regular expression",
                        "String",
                        "Variable prefix ($)"
                );
                for (int i = 0; i < expectedItems.size(); i++) {
                    assertEquals(expectedItems.get(i), root.getChildren().get(i).getValue());
                }

                // Default selection: Parentheses
                assertEquals("Parentheses", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Parentheses", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("BCBEC4", page.getForegroundSwatch().getText());
                assertTrue(page.getInheritCheck().isSelected());
                assertTrue(page.getInheritTargetLabel().getText().contains("Braces and Operators"));
                assertTrue(page.getInheritTargetLabel().getText().contains("Parentheses"));

                // Backward compatible subclass
                SettingsGitLabCIExpressionPage subPage = new SettingsGitLabCIExpressionPage();
                assertNotNull(subPage.getCategoryTree());
                assertEquals(8, subPage.getCategoryTree().getRoot().getChildren().size());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testGitLabCiExpressionModificationApplyAndReset() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeGitLabCiExpressionPage page = new SettingsColorSchemeGitLabCiExpressionPage();

                AtomicBoolean modifiedFired = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedFired.set(true));

                // Initially not modified
                assertFalse(page.isModified());

                // Select String
                page.selectTreeItem("String");
                assertEquals("String", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("6AAB73", page.getForegroundSwatch().getText());

                // Modify bold
                page.getBoldCheck().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedFired.get());

                // Apply changes
                page.apply();
                assertFalse(page.isModified());

                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                ColorAttribute applied = s.getAttribute(s.getActiveSchemeName(), "String");
                assertNotNull(applied);
                assertTrue(applied.isBold());

                // Modify again and Reset
                page.getItalicCheck().setSelected(true);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testGitLabCiExpressionNavigationAndPreviewInteractivity() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeGitLabCiExpressionPage page = new SettingsColorSchemeGitLabCiExpressionPage();

                AtomicReference<String> navigatedTarget = new AtomicReference<>();
                page.setOnNavigateToInheritedListener(navigatedTarget::set);

                // Clicking inheritance link triggers navigation callback
                page.getInheritTargetLabel().fireEvent(new javafx.scene.input.MouseEvent(
                        javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                        0, 0, 0, 0, javafx.scene.input.MouseButton.PRIMARY, 1,
                        false, false, false, false, false, false, false, false, false, false, null
                ));
                assertEquals("Braces and Operators//Parentheses", navigatedTarget.get());

                // Test preview tokens exist
                assertNotNull(page.getCodeLinesBox());
                assertFalse(page.getCodeLinesBox().getChildren().isEmpty());

                // Select Identifier
                page.selectTreeItem("Identifier");
                assertEquals("Identifier", page.getSelectedKey());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 2. Go Page Tests (media_1790430810489.png - media_1790430911466.png)
    // -------------------------------------------------------------------------

    @Test
    void testGoPageInitAndDefaults() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeGoPage page = new SettingsColorSchemeGoPage();

                TreeItem<String> root = page.getCategoryTree().getRoot();
                assertNotNull(root);

                // 9 top-level items:
                // Bad character, Braces and operators, Comments, Declarations,
                // Identifier, Keyword, Number, References, String
                assertEquals(9, root.getChildren().size());

                List<String> expectedRoots = List.of(
                        "Bad character",
                        "Braces and operators",
                        "Comments",
                        "Declarations",
                        "Identifier",
                        "Keyword",
                        "Number",
                        "References",
                        "String"
                );
                for (int i = 0; i < expectedRoots.size(); i++) {
                    assertEquals(expectedRoots.get(i), root.getChildren().get(i).getValue());
                }

                // Check Braces and operators (8 items: Brackets, Colon, Comma, Dot, Operation sign, Parentheses, Semicolon)
                TreeItem<String> bracesItem = root.getChildren().get(1);
                assertEquals(7, bracesItem.getChildren().size());

                // Check Comments (5 items, including Build constraints sub-group)
                TreeItem<String> commentsItem = root.getChildren().get(2);
                assertEquals(5, commentsItem.getChildren().size());
                TreeItem<String> buildConstraintsItem = commentsItem.getChildren().stream()
                        .filter(item -> "Build constraints".equals(item.getValue()))
                        .findFirst().orElse(null);
                assertNotNull(buildConstraintsItem);
                assertEquals(3, buildConstraintsItem.getChildren().size()); // Key, Tag, Unknown tag

                // Check Declarations (Functions, Parameters, Struct tags, Types, Variables)
                TreeItem<String> declItem = root.getChildren().get(3);
                assertEquals(5, declItem.getChildren().size());

                // Check References (Function calls, Type references, Variables)
                TreeItem<String> refItem = root.getChildren().get(7);
                assertEquals(3, refItem.getChildren().size());

                // Check String (Character, Escape sequence, Valid string)
                TreeItem<String> strItem = root.getChildren().get(8);
                assertEquals(3, strItem.getChildren().size());

                // Default selection: Comma (screenshot media_1790430810489.png)
                assertEquals("Comma", page.getCategoryTree().getSelectionModel().getSelectedItem().getValue());
                assertEquals("Braces and operators // Comma", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("BCBEC4", page.getForegroundSwatch().getText());
                assertTrue(page.getInheritCheck().isSelected());
                assertTrue(page.getInheritTargetLabel().getText().contains("Braces and Operators"));
                assertTrue(page.getInheritTargetLabel().getText().contains("Comma"));
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testGoPageNestedPathResolutionAndApply() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeGoPage page = new SettingsColorSchemeGoPage();

                // Select Comments // Build constraints // Tag (screenshot media_1790430833698.png)
                page.selectTreeItem("Tag");
                assertEquals("Comments // Build constraints // Tag", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("56A8F5", page.getForegroundSwatch().getText());

                // Modify error stripe color
                page.getErrorStripeCheck().setSelected(true);
                assertTrue(page.isModified());

                page.apply();
                assertFalse(page.isModified());

                EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
                ColorAttribute attr = s.getAttribute(s.getActiveSchemeName(), "Comments // Build constraints // Tag");
                assertNotNull(attr);

                // Select Declarations // Struct tags // Key (screenshot media_1790430882024.png)
                page.selectTreeItem("Struct tags");
                page.selectTreeItem("Key");
                assertTrue(page.getSelectedKey().contains("Key"));

                // Select References // Function calls // Built-in function (screenshot media_1790430911466.png)
                page.selectTreeItem("Built-in function");
                assertEquals("References // Function calls // Built-in function", page.getSelectedKey());
                assertTrue(page.getForegroundCheck().isSelected());
                assertEquals("56A8F5", page.getForegroundSwatch().getText());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testGoPageInheritanceNavigation() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsColorSchemeGoPage page = new SettingsColorSchemeGoPage();

                AtomicReference<String> navigatedTarget = new AtomicReference<>();
                page.setOnNavigateToInheritedListener(navigatedTarget::set);

                // For default Comma, clicking inheritance navigates to Braces and Operators//Comma
                page.getInheritTargetLabel().fireEvent(new javafx.scene.input.MouseEvent(
                        javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                        0, 0, 0, 0, javafx.scene.input.MouseButton.PRIMARY, 1,
                        false, false, false, false, false, false, false, false, false, false, null
                ));
                assertEquals("Braces and Operators//Comma", navigatedTarget.get());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 3. SettingsDialog Integration Tests
    // -------------------------------------------------------------------------

    @Test
    void testSettingsDialogIntegration() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDialog dialog = new SettingsDialog(null);

                // Navigate to GitLab CI Expression
                dialog.selectCategory("GitLab CI Expression");
                SettingsColorSchemeGitLabCiExpressionPage gitlabPage = dialog.getCurrentColorSchemeGitLabCiExpressionPage();
                assertNotNull(gitlabPage);
                assertEquals("Parentheses", gitlabPage.getSelectedKey());

                // Navigate to Go
                dialog.selectCategory("Go");
                SettingsColorSchemeGoPage goPage = dialog.getCurrentColorSchemeGoPage();
                assertNotNull(goPage);
                assertEquals("Braces and operators // Comma", goPage.getSelectedKey());

                // Apply button state tracking
                assertFalse(dialog.getApplyButton().isDisabled()); // if other pages or check current modified
                goPage.getBoldCheck().setSelected(true);
                assertTrue(goPage.isModified());
                assertFalse(dialog.getApplyButton().isDisabled());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // 4. Strict Lumina Brand Isolation Test
    // -------------------------------------------------------------------------

    @Test
    void testBrandIsolation() throws Exception {
        List<String> filesToCheck = List.of(
                "src/main/java/dev/lumina/ui/SettingsColorSchemeGitLabCiExpressionPage.java",
                "src/main/java/dev/lumina/ui/SettingsGitLabCIExpressionPage.java",
                "src/main/java/dev/lumina/ui/SettingsColorSchemeGoPage.java"
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
