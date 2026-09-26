package dev.lumina.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Comprehensive Settings dialog with:
 * - Left: Settings category tree with interactive search filtering
 * - Right Top: Dynamic breadcrumbs header (e.g. Appearance & Behavior › Appearance) and ← → navigation history
 * - Right Center: Dynamic page routing (Category overview for parent nodes, SettingsIdeAppearancePage for Appearance, etc.)
 * - Bottom: Status bar with help button and dialog actions (Cancel, Apply, OK)
 */
public class SettingsDialog {

    private final Stage stage;
    private final TreeView<String> tree;
    private final TextField searchField;

    // Header & History navigation
    private final HBox breadcrumbBox = new HBox(6);
    private final Button backButton = new Button("\u2190");
    private final Button forwardButton = new Button("\u2192");
    private final List<TreeItem<String>> navHistory = new ArrayList<>();
    private int navHistoryIndex = -1;
    private boolean isNavigatingHistory = false;

    // Content container
    private final StackPane contentContainer = new StackPane();
    private SettingsIdeAppearancePage currentIdeAppearancePage;
    private SettingsMenusToolbarsPage currentMenusToolbarsPage;
    private SettingsQuickListsPage currentQuickListsPage;
    private SettingsSystemPage currentSystemPage;
    private SettingsEditorGeneralPage currentEditorGeneralPage;
    private SettingsAutoImportPage currentAutoImportPage;
    private SettingsAppearancePage currentEditorAppearancePage;
    private SettingsBreadcrumbsPage currentBreadcrumbsPage;
    private SettingsCodeCompletionPage currentCodeCompletionPage;
    private SettingsCodeFoldingPage currentCodeFoldingPage;
    private SettingsConsolePage currentConsolePage;
    private SettingsEditorTabsPage currentEditorTabsPage;
    private SettingsGutterIconsPage currentGutterIconsPage;
    private SettingsInlineCompletionPage currentInlineCompletionPage;
    private SettingsPostfixCompletionPage currentPostfixCompletionPage;
    private SettingsSmartKeysPage currentSmartKeysPage;
    private SettingsSmartKeysYAMLPage currentSmartKeysYamlPage;
    private SettingsSmartKeysHTMLCSSPage currentSmartKeysHtmlCssPage;
    private SettingsSmartKeysPythonPage currentSmartKeysPythonPage;
    private SettingsSmartKeysJSONPage currentSmartKeysJsonPage;
    private SettingsSmartKeysRustPage currentSmartKeysRustPage;
    private SettingsSmartKeysMarkdownPage currentSmartKeysMarkdownPage;
    private SettingsSmartKeysScalaPage currentSmartKeysScalaPage;
    private SettingsSmartKeysSQLPage currentSmartKeysSqlPage;
    private SettingsSmartKeysRubyPage currentSmartKeysRubyPage;
    private SettingsSmartKeysJavaScriptPage currentSmartKeysJsPage;
    private SettingsSmartKeysPHPPage currentSmartKeysPhpPage;
    private SettingsStickyLinesPage currentStickyLinesPage;
    private SettingsCodeEditingPage currentCodeEditingPage;
    private SettingsFontPage currentFontPage;
    private SettingsColorSchemePage currentColorSchemePage;
    private SettingsColorSchemeGeneralPage currentColorSchemeGeneralPage;
    private SettingsColorSchemeLanguageDefaultsPage currentColorSchemeLanguageDefaultsPage;
    private SettingsColorSchemeFontPage currentColorSchemeFontPage;
    private SettingsConsoleFontPage currentConsoleFontPage;
    private SettingsConsoleColorsPage currentConsoleColorsPage;
    private SettingsCodeWithMePage currentCodeWithMePage;
    private SettingsColorSchemeDebuggerPage currentColorSchemeDebuggerPage;
    private SettingsColorSchemeDiffMergePage currentColorSchemeDiffMergePage;
    private SettingsColorSchemeJvmLoggingPage currentColorSchemeJvmLoggingPage;
    private SettingsColorSchemeUserDefinedFileTypesPage currentColorSchemeUserDefinedFileTypesPage;
    private SettingsColorSchemeVcsPage currentColorSchemeVcsPage;
    private SettingsColorSchemeJavaPage currentColorSchemeJavaPage;
    private SettingsColorSchemeAngularTemplatePage currentColorSchemeAngularTemplatePage;
    private SettingsColorSchemeContextFreeGrammarPage currentColorSchemeContextFreeGrammarPage;
    private SettingsColorSchemeCssPage currentColorSchemeCssPage;
    private SettingsColorSchemeDataEditorViewerPage currentColorSchemeDataEditorViewerPage;
    private SettingsColorSchemeDatabasePage currentColorSchemeDatabasePage;
    private SettingsColorSchemeDiagramsPage currentColorSchemeDiagramsPage;
    private SettingsColorSchemeDockerfilePage currentColorSchemeDockerfilePage;
    private SettingsColorSchemeEditorConfigPage currentColorSchemeEditorConfigPage;
    private SettingsColorSchemeErbPage currentColorSchemeErbPage;
    private SettingsColorSchemeFreeMarkerPage currentColorSchemeFreeMarkerPage;
    private SettingsColorSchemeGitLabCiExpressionPage currentColorSchemeGitLabCiExpressionPage;
    private SettingsColorSchemeGoPage currentColorSchemeGoPage;
    private SettingsColorSchemeGradleDeclarativePage currentColorSchemeGradleDeclarativePage;
    private SettingsColorSchemeGroovyPage currentColorSchemeGroovyPage;
    private SettingsColorSchemeHtmlPage currentColorSchemeHtmlPage;
    private SettingsColorSchemeHttpRequestPage currentColorSchemeHttpRequestPage;
    private SettingsColorSchemeJavaScriptPage currentColorSchemeJavaScriptPage;
    private Button applyButton;

    public SettingsDialog(Stage owner) {
        this(owner, "Appearance");
    }

    public SettingsDialog(Stage owner, String initialCategory) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.DECORATED);
        stage.setTitle("Settings");

        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("app-root", "settings-dialog");
        root.setStyle("-fx-background-color: #1E1F22;");

        // ---- Left: Category search + tree ----
        tree = buildCategoryTree();
        tree.setPrefWidth(260);
        tree.setMinWidth(240);
        tree.getStyleClass().add("settings-tree");
        tree.setStyle("-fx-background-color: #1E1F22;");

        HBox searchBox = new HBox(6);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(8, 12, 8, 12));
        searchBox.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent transparent #393B40 transparent; -fx-border-width: 0 0 1 0;");

        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px;");

        searchField = new TextField();
        searchField.setPromptText("Search settings");
        searchField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #6F737A; -fx-font-size: 12px; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8 4 8;");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchBox.getChildren().addAll(searchIcon, searchField);

        searchField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && !newV.trim().isEmpty()) {
                TreeItem<String> match = searchTree(tree.getRoot(), newV.trim().toLowerCase());
                if (match != null) {
                    expandAncestors(match);
                    tree.getSelectionModel().select(match);
                    tree.scrollTo(tree.getRow(match));
                }
            }
        });

        VBox.setVgrow(tree, Priority.ALWAYS);
        VBox leftPane = new VBox(searchBox, tree);
        leftPane.setPrefWidth(260);
        leftPane.setMinWidth(240);
        leftPane.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent #393B40 transparent transparent; -fx-border-width: 0 1 0 0;");

        // ---- Right: Header + Content ----
        BorderPane rightPane = new BorderPane();
        rightPane.setStyle("-fx-background-color: #1E1F22;");

        HBox header = buildHeader();
        rightPane.setTop(header);
        rightPane.setCenter(contentContainer);

        // ---- Bottom: Buttons ----
        HBox buttons = buildButtonBar();

        root.setLeft(leftPane);
        root.setCenter(rightPane);
        root.setBottom(buttons);

        // Selection listener
        tree.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                if (!isNavigatingHistory) {
                    if (navHistoryIndex >= 0 && navHistoryIndex < navHistory.size() - 1) {
                        navHistory.subList(navHistoryIndex + 1, navHistory.size()).clear();
                    }
                    navHistory.add(selected);
                    navHistoryIndex = navHistory.size() - 1;
                    updateNavButtons();
                }
                updateBreadcrumbs(selected);
                showPage(selected);
            }
        });

        // Initial selection
        TreeItem<String> initialItem = findItem(tree.getRoot(), initialCategory);
        if (initialItem == null) initialItem = findItem(tree.getRoot(), "Appearance");
        if (initialItem != null) {
            expandAncestors(initialItem);
            tree.getSelectionModel().select(initialItem);
            if (contentContainer.getChildren().isEmpty()) {
                updateBreadcrumbs(initialItem);
                showPage(initialItem);
            }
        }

        Scene scene = new Scene(root, 960, 640);
        scene.getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
        stage.setScene(scene);
    }

    public void show() {
        stage.showAndWait();
    }

    StackPane getContentContainer() {
        return contentContainer;
    }

    TreeView<String> getTree() {
        return tree;
    }

    // --------------------------------------------------- Header & Navigation

    private HBox buildHeader() {
        HBox bar = new HBox(6);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 24, 12, 24));
        bar.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent transparent #393B40 transparent; -fx-border-width: 0 0 1 0;");

        breadcrumbBox.setAlignment(Pos.CENTER_LEFT);
        breadcrumbBox.setSpacing(6);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        backButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #6F737A; -fx-font-size: 13px; -fx-cursor: default; -fx-padding: 0 4 0 4;");
        backButton.setDisable(true);
        backButton.setOnAction(e -> navigateHistory(-1));

        forwardButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #6F737A; -fx-font-size: 13px; -fx-cursor: default; -fx-padding: 0 4 0 4;");
        forwardButton.setDisable(true);
        forwardButton.setOnAction(e -> navigateHistory(1));

        bar.getChildren().addAll(breadcrumbBox, spacer, backButton, forwardButton);
        return bar;
    }

    private void updateBreadcrumbs(TreeItem<String> selected) {
        breadcrumbBox.getChildren().clear();
        List<TreeItem<String>> chain = new ArrayList<>();
        TreeItem<String> it = selected;
        while (it != null && it.getParent() != null) {
            chain.add(0, it);
            it = it.getParent();
        }

        for (int i = 0; i < chain.size(); i++) {
            final TreeItem<String> item = chain.get(i);
            if (i < chain.size() - 1) {
                Hyperlink link = new Hyperlink(item.getValue());
                link.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: false;");
                link.setOnMouseEntered(e -> link.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: true;"));
                link.setOnMouseExited(e -> link.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: false;"));
                link.setOnAction(e -> {
                    expandAncestors(item);
                    tree.getSelectionModel().select(item);
                });

                Label chevron = new Label("\u203A");
                chevron.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 14px; -fx-font-weight: bold;");
                breadcrumbBox.getChildren().addAll(link, chevron);
            } else {
                Label current = new Label(item.getValue());
                current.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
                breadcrumbBox.getChildren().add(current);

                if ("Menus and Toolbars".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("🗄 Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentMenusToolbarsPage != null) {
                            currentMenusToolbarsPage.revertChanges();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Quick Lists".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentQuickListsPage != null) {
                            currentQuickListsPage.revert();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Code Completion".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentCodeCompletionPage != null) {
                            currentCodeCompletionPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Code Folding".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentCodeFoldingPage != null) {
                            currentCodeFoldingPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Console".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentConsolePage != null) {
                            currentConsolePage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Editor Tabs".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentEditorTabsPage != null) {
                            currentEditorTabsPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Gutter Icons".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentGutterIconsPage != null) {
                            currentGutterIconsPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Inline Completion".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentInlineCompletionPage != null) {
                            currentInlineCompletionPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Postfix Completion".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentPostfixCompletionPage != null) {
                            currentPostfixCompletionPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Smart Keys".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentSmartKeysPage != null) {
                            currentSmartKeysPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("YAML".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentSmartKeysYamlPage != null) {
                            currentSmartKeysYamlPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("HTML/CSS".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentSmartKeysHtmlCssPage != null) {
                            currentSmartKeysHtmlCssPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Python".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentSmartKeysPythonPage != null) {
                            currentSmartKeysPythonPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("JSON".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentSmartKeysJsonPage != null) {
                            currentSmartKeysJsonPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Rust".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentSmartKeysRustPage != null) {
                            currentSmartKeysRustPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Markdown".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentSmartKeysMarkdownPage != null) {
                            currentSmartKeysMarkdownPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Scala".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentSmartKeysScalaPage != null) {
                            currentSmartKeysScalaPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("SQL".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentSmartKeysSqlPage != null) {
                            currentSmartKeysSqlPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Ruby".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentSmartKeysRubyPage != null) {
                            currentSmartKeysRubyPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("JavaScript".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentSmartKeysJsPage != null) {
                            currentSmartKeysJsPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("PHP".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentSmartKeysPhpPage != null) {
                            currentSmartKeysPhpPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Sticky Lines".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentStickyLinesPage != null) {
                            currentStickyLinesPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Code Editing".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentCodeEditingPage != null) {
                            currentCodeEditingPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Font".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentFontPage != null) {
                            currentFontPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Color Scheme".equals(item.getValue())) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemePage != null) {
                            currentColorSchemePage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("General".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeGeneralPage != null) {
                            currentColorSchemeGeneralPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Language Defaults".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeLanguageDefaultsPage != null) {
                            currentColorSchemeLanguageDefaultsPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Debugger".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeDebuggerPage != null) {
                            currentColorSchemeDebuggerPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Diff & Merge".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeDiffMergePage != null) {
                            currentColorSchemeDiffMergePage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("JVM Logging".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeJvmLoggingPage != null) {
                            currentColorSchemeJvmLoggingPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("User-Defined File Types".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeUserDefinedFileTypesPage != null) {
                            currentColorSchemeUserDefinedFileTypesPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("VCS".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeVcsPage != null) {
                            currentColorSchemeVcsPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Java".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeJavaPage != null) {
                            currentColorSchemeJavaPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Angular Template".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeAngularTemplatePage != null) {
                            currentColorSchemeAngularTemplatePage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Context Free Grammar".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeContextFreeGrammarPage != null) {
                            currentColorSchemeContextFreeGrammarPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("CSS".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeCssPage != null) {
                            currentColorSchemeCssPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Data Editor and Viewer".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeDataEditorViewerPage != null) {
                            currentColorSchemeDataEditorViewerPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Database".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeDatabasePage != null) {
                            currentColorSchemeDatabasePage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Diagrams".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeDiagramsPage != null) {
                            currentColorSchemeDiagramsPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Dockerfile".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeDockerfilePage != null) {
                            currentColorSchemeDockerfilePage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("EditorConfig".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeEditorConfigPage != null) {
                            currentColorSchemeEditorConfigPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("ERB".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeErbPage != null) {
                            currentColorSchemeErbPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("FreeMarker".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeFreeMarkerPage != null) {
                            currentColorSchemeFreeMarkerPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("GitLab CI Expression".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeGitLabCiExpressionPage != null) {
                            currentColorSchemeGitLabCiExpressionPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Go".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeGoPage != null) {
                            currentColorSchemeGoPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Gradle Declarative Configuration".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeGradleDeclarativePage != null) {
                            currentColorSchemeGradleDeclarativePage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Groovy".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeGroovyPage != null) {
                            currentColorSchemeGroovyPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("HTML".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeHtmlPage != null) {
                            currentColorSchemeHtmlPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("HTTP Request".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeHttpRequestPage != null) {
                            currentColorSchemeHttpRequestPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("JavaScript".equals(item.getValue()) && chain.stream().anyMatch(ci -> "Color Scheme".equals(ci.getValue()))) {
                    Hyperlink revertLink = new Hyperlink("Revert changes");
                    revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;");
                    revertLink.setOnMouseEntered(e -> revertLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnMouseExited(e -> revertLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0 0 0 16;"));
                    revertLink.setOnAction(e -> {
                        if (currentColorSchemeJavaScriptPage != null) {
                            currentColorSchemeJavaScriptPage.reset();
                            updateApplyButtonState();
                        }
                    });
                    breadcrumbBox.getChildren().add(revertLink);
                } else if ("Required Plugins".equals(item.getValue())) {
                    Label projectIcon = new Label("📦");
                    projectIcon.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 0 0 0 6;");
                    breadcrumbBox.getChildren().add(projectIcon);
                }

                if (isProjectSetting(item.getValue())) {
                    SVGPath pIcon = new SVGPath();
                    pIcon.setContent("M 1 2 L 11 2 L 11 10 L 1 10 Z M 1 4 L 11 4");
                    pIcon.setFill(Color.TRANSPARENT);
                    pIcon.setStroke(Color.web("#848BA3"));
                    pIcon.setStrokeWidth(0.9);
                    HBox.setMargin(pIcon, new Insets(0, 0, 0, 4));
                    breadcrumbBox.getChildren().add(pIcon);
                }
            }
        }
    }

    private void navigateHistory(int delta) {
        int target = navHistoryIndex + delta;
        if (target >= 0 && target < navHistory.size()) {
            isNavigatingHistory = true;
            navHistoryIndex = target;
            TreeItem<String> item = navHistory.get(target);
            expandAncestors(item);
            tree.getSelectionModel().select(item);
            isNavigatingHistory = false;
            updateNavButtons();
        }
    }

    private void updateNavButtons() {
        boolean canBack = navHistoryIndex > 0;
        boolean canFwd = navHistoryIndex < navHistory.size() - 1;

        backButton.setDisable(!canBack);
        backButton.setStyle(canBack
                ? "-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0 4 0 4;"
                : "-fx-background-color: transparent; -fx-text-fill: #6F737A; -fx-font-size: 13px; -fx-cursor: default; -fx-padding: 0 4 0 4;");

        forwardButton.setDisable(!canFwd);
        forwardButton.setStyle(canFwd
                ? "-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0 4 0 4;"
                : "-fx-background-color: transparent; -fx-text-fill: #6F737A; -fx-font-size: 13px; -fx-cursor: default; -fx-padding: 0 4 0 4;");
    }

    // --------------------------------------------------- Page Routing

    private void showPage(TreeItem<String> selected) {
        if (currentIdeAppearancePage != null) {
            currentIdeAppearancePage.save();
            currentIdeAppearancePage = null;
        }
        if (currentSystemPage != null) {
            currentSystemPage.save();
            currentSystemPage = null;
        }
        contentContainer.getChildren().clear();

        String pageName = selected.getValue();

        // 1. If a category node with children is selected (e.g. Appearance & Behavior), show Category Overview
        if (!selected.getChildren().isEmpty() &&
                (selected.getParent() == tree.getRoot() || "Appearance & Behavior".equals(pageName))) {
            SettingsCategoryOverviewPage overview = new SettingsCategoryOverviewPage(selected, child -> {
                expandAncestors(child);
                tree.getSelectionModel().select(child);
            });
            wrapInScroll(overview);
            return;
        }

        // 2. Ancestor hierarchy detection
        TreeItem<String> ancestor = selected.getParent();
        boolean underAppearanceGroup = false;
        boolean underEditorGroup = false;
        boolean underColorScheme = false;

        while (ancestor != null) {
            String v = ancestor.getValue();
            if (v != null) {
                if (v.equals("Appearance & Behavior")) underAppearanceGroup = true;
                if (v.equals("Editor")) underEditorGroup = true;
                if (v.equals("Color Scheme")) underColorScheme = true;
            }
            ancestor = ancestor.getParent();
        }

        // 3. Appearance & Behavior > Appearance
        if (underAppearanceGroup && "Appearance".equals(pageName)) {
            currentIdeAppearancePage = new SettingsIdeAppearancePage();
            wrapInScroll(currentIdeAppearancePage);
            return;
        }

        // 4. Color Scheme subpages
        if (underColorScheme) {
            Runnable navigateToTheme = () -> {
                TreeItem<String> appRoot = findItem(tree.getRoot(), "Appearance & Behavior");
                if (appRoot != null) {
                    TreeItem<String> appItem = findItem(appRoot, "Appearance");
                    if (appItem != null) {
                        expandAncestors(appItem);
                        tree.getSelectionModel().select(appItem);
                    }
                }
            };

            if ("General".equals(pageName)) {
                if (currentColorSchemeGeneralPage == null) {
                    currentColorSchemeGeneralPage = new SettingsColorSchemeGeneralPage();
                }
                currentColorSchemeGeneralPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeGeneralPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentColorSchemeGeneralPage);
                updateApplyButtonState();
                return;
            }
            if ("Language Defaults".equals(pageName)) {
                if (currentColorSchemeLanguageDefaultsPage == null) {
                    currentColorSchemeLanguageDefaultsPage = new SettingsColorSchemeLanguageDefaultsPage();
                }
                currentColorSchemeLanguageDefaultsPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeLanguageDefaultsPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentColorSchemeLanguageDefaultsPage);
                updateApplyButtonState();
                return;
            }
            if ("Color Scheme Font".equals(pageName)) {
                if (currentColorSchemeFontPage == null) {
                    currentColorSchemeFontPage = new SettingsColorSchemeFontPage();
                }
                currentColorSchemeFontPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeFontPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentColorSchemeFontPage);
                updateApplyButtonState();
                return;
            }
            if ("Console Font".equals(pageName)) {
                if (currentConsoleFontPage == null) {
                    currentConsoleFontPage = new SettingsConsoleFontPage();
                }
                currentConsoleFontPage.setOnModifiedListener(this::updateApplyButtonState);
                currentConsoleFontPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentConsoleFontPage);
                updateApplyButtonState();
                return;
            }
            if ("Console Colors".equals(pageName)) {
                if (currentConsoleColorsPage == null) {
                    currentConsoleColorsPage = new SettingsConsoleColorsPage();
                }
                currentConsoleColorsPage.setOnModifiedListener(this::updateApplyButtonState);
                currentConsoleColorsPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentConsoleColorsPage);
                updateApplyButtonState();
                return;
            }
            if ("Code With Me".equals(pageName)) {
                if (currentCodeWithMePage == null) {
                    currentCodeWithMePage = new SettingsCodeWithMePage();
                }
                currentCodeWithMePage.setOnModifiedListener(this::updateApplyButtonState);
                currentCodeWithMePage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentCodeWithMePage);
                updateApplyButtonState();
                return;
            }
            if ("Debugger".equals(pageName)) {
                if (currentColorSchemeDebuggerPage == null) {
                    currentColorSchemeDebuggerPage = new SettingsColorSchemeDebuggerPage();
                }
                currentColorSchemeDebuggerPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeDebuggerPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentColorSchemeDebuggerPage);
                updateApplyButtonState();
                return;
            }
            if ("Diff & Merge".equals(pageName)) {
                if (currentColorSchemeDiffMergePage == null) {
                    currentColorSchemeDiffMergePage = new SettingsColorSchemeDiffMergePage();
                }
                currentColorSchemeDiffMergePage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeDiffMergePage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentColorSchemeDiffMergePage);
                updateApplyButtonState();
                return;
            }
            if ("JVM Logging".equals(pageName)) {
                if (currentColorSchemeJvmLoggingPage == null) {
                    currentColorSchemeJvmLoggingPage = new SettingsColorSchemeJvmLoggingPage();
                }
                currentColorSchemeJvmLoggingPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeJvmLoggingPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentColorSchemeJvmLoggingPage);
                updateApplyButtonState();
                return;
            }
            if ("User-Defined File Types".equals(pageName)) {
                if (currentColorSchemeUserDefinedFileTypesPage == null) {
                    currentColorSchemeUserDefinedFileTypesPage = new SettingsColorSchemeUserDefinedFileTypesPage();
                }
                currentColorSchemeUserDefinedFileTypesPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeUserDefinedFileTypesPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentColorSchemeUserDefinedFileTypesPage);
                updateApplyButtonState();
                return;
            }
            if ("VCS".equals(pageName)) {
                if (currentColorSchemeVcsPage == null) {
                    currentColorSchemeVcsPage = new SettingsColorSchemeVcsPage();
                }
                currentColorSchemeVcsPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeVcsPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentColorSchemeVcsPage);
                updateApplyButtonState();
                return;
            }
            if ("Java".equals(pageName)) {
                if (currentColorSchemeJavaPage == null) {
                    currentColorSchemeJavaPage = new SettingsColorSchemeJavaPage();
                }
                currentColorSchemeJavaPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeJavaPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeJavaPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeJavaPage);
                updateApplyButtonState();
                return;
            }
            if ("Angular Template".equals(pageName)) {
                if (currentColorSchemeAngularTemplatePage == null) {
                    currentColorSchemeAngularTemplatePage = new SettingsColorSchemeAngularTemplatePage();
                }
                currentColorSchemeAngularTemplatePage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeAngularTemplatePage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeAngularTemplatePage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeAngularTemplatePage);
                updateApplyButtonState();
                return;
            }
            if ("Context Free Grammar".equals(pageName)) {
                if (currentColorSchemeContextFreeGrammarPage == null) {
                    currentColorSchemeContextFreeGrammarPage = new SettingsColorSchemeContextFreeGrammarPage();
                }
                currentColorSchemeContextFreeGrammarPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeContextFreeGrammarPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeContextFreeGrammarPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeContextFreeGrammarPage);
                updateApplyButtonState();
                return;
            }
            if ("CSS".equals(pageName)) {
                if (currentColorSchemeCssPage == null) {
                    currentColorSchemeCssPage = new SettingsColorSchemeCssPage();
                }
                currentColorSchemeCssPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeCssPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentColorSchemeCssPage);
                updateApplyButtonState();
                return;
            }
            if ("Data Editor and Viewer".equals(pageName)) {
                if (currentColorSchemeDataEditorViewerPage == null) {
                    currentColorSchemeDataEditorViewerPage = new SettingsColorSchemeDataEditorViewerPage();
                }
                currentColorSchemeDataEditorViewerPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeDataEditorViewerPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeDataEditorViewerPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("General");
                    if (currentColorSchemeGeneralPage != null) {
                        currentColorSchemeGeneralPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeDataEditorViewerPage);
                updateApplyButtonState();
                return;
            }
            if ("Database".equals(pageName)) {
                if (currentColorSchemeDatabasePage == null) {
                    currentColorSchemeDatabasePage = new SettingsColorSchemeDatabasePage();
                }
                currentColorSchemeDatabasePage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeDatabasePage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentColorSchemeDatabasePage);
                updateApplyButtonState();
                return;
            }
            if ("Diagrams".equals(pageName)) {
                if (currentColorSchemeDiagramsPage == null) {
                    currentColorSchemeDiagramsPage = new SettingsColorSchemeDiagramsPage();
                }
                currentColorSchemeDiagramsPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeDiagramsPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                wrapInScroll(currentColorSchemeDiagramsPage);
                updateApplyButtonState();
                return;
            }
            if ("Dockerfile".equals(pageName)) {
                if (currentColorSchemeDockerfilePage == null) {
                    currentColorSchemeDockerfilePage = new SettingsColorSchemeDockerfilePage();
                }
                currentColorSchemeDockerfilePage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeDockerfilePage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeDockerfilePage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeDockerfilePage);
                updateApplyButtonState();
                return;
            }
            if ("EditorConfig".equals(pageName)) {
                if (currentColorSchemeEditorConfigPage == null) {
                    currentColorSchemeEditorConfigPage = new SettingsColorSchemeEditorConfigPage();
                }
                currentColorSchemeEditorConfigPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeEditorConfigPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeEditorConfigPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeEditorConfigPage);
                updateApplyButtonState();
                return;
            }
            if ("ERB".equals(pageName)) {
                if (currentColorSchemeErbPage == null) {
                    currentColorSchemeErbPage = new SettingsColorSchemeErbPage();
                }
                currentColorSchemeErbPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeErbPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeErbPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeErbPage);
                updateApplyButtonState();
                return;
            }
            if ("FreeMarker".equals(pageName)) {
                if (currentColorSchemeFreeMarkerPage == null) {
                    currentColorSchemeFreeMarkerPage = new SettingsColorSchemeFreeMarkerPage();
                }
                currentColorSchemeFreeMarkerPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeFreeMarkerPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeFreeMarkerPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeFreeMarkerPage);
                updateApplyButtonState();
                return;
            }
            if ("GitLab CI Expression".equals(pageName)) {
                if (currentColorSchemeGitLabCiExpressionPage == null) {
                    currentColorSchemeGitLabCiExpressionPage = new SettingsColorSchemeGitLabCiExpressionPage();
                }
                currentColorSchemeGitLabCiExpressionPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeGitLabCiExpressionPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeGitLabCiExpressionPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeGitLabCiExpressionPage);
                updateApplyButtonState();
                return;
            }
            if ("Go".equals(pageName)) {
                if (currentColorSchemeGoPage == null) {
                    currentColorSchemeGoPage = new SettingsColorSchemeGoPage();
                }
                currentColorSchemeGoPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeGoPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeGoPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeGoPage);
                updateApplyButtonState();
                return;
            }
            if ("Gradle Declarative Configuration".equals(pageName)) {
                if (currentColorSchemeGradleDeclarativePage == null) {
                    currentColorSchemeGradleDeclarativePage = new SettingsColorSchemeGradleDeclarativePage();
                }
                currentColorSchemeGradleDeclarativePage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeGradleDeclarativePage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeGradleDeclarativePage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeGradleDeclarativePage);
                updateApplyButtonState();
                return;
            }
            if ("Groovy".equals(pageName)) {
                if (currentColorSchemeGroovyPage == null) {
                    currentColorSchemeGroovyPage = new SettingsColorSchemeGroovyPage();
                }
                currentColorSchemeGroovyPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeGroovyPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeGroovyPage.setOnNavigateToInheritedListener(targetKey -> {
                    if (targetKey != null && targetKey.endsWith("(Groovy)")) {
                        String baseKey = targetKey.substring(0, targetKey.indexOf("(Groovy)")).trim();
                        currentColorSchemeGroovyPage.selectCategory(baseKey);
                    } else {
                        selectCategory("Language Defaults");
                        if (currentColorSchemeLanguageDefaultsPage != null) {
                            currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                        }
                    }
                });
                wrapInScroll(currentColorSchemeGroovyPage);
                updateApplyButtonState();
                return;
            }
            if ("HTML".equals(pageName)) {
                if (currentColorSchemeHtmlPage == null) {
                    currentColorSchemeHtmlPage = new SettingsColorSchemeHtmlPage();
                }
                currentColorSchemeHtmlPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeHtmlPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeHtmlPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeHtmlPage);
                updateApplyButtonState();
                return;
            }
            if ("HTTP Request".equals(pageName)) {
                if (currentColorSchemeHttpRequestPage == null) {
                    currentColorSchemeHttpRequestPage = new SettingsColorSchemeHttpRequestPage();
                }
                currentColorSchemeHttpRequestPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeHttpRequestPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeHttpRequestPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeHttpRequestPage);
                updateApplyButtonState();
                return;
            }
            if ("JavaScript".equals(pageName)) {
                if (currentColorSchemeJavaScriptPage == null) {
                    currentColorSchemeJavaScriptPage = new SettingsColorSchemeJavaScriptPage();
                }
                currentColorSchemeJavaScriptPage.setOnModifiedListener(this::updateApplyButtonState);
                currentColorSchemeJavaScriptPage.getHeaderBar().setOnNavigateToTheme(navigateToTheme);
                currentColorSchemeJavaScriptPage.setOnNavigateToInheritedListener(targetKey -> {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                });
                wrapInScroll(currentColorSchemeJavaScriptPage);
                updateApplyButtonState();
                return;
            }
            buildColorSchemePage(pageName);
            return;
        }

        // 5. Editor and subpages
        if (underEditorGroup || "Editor".equals(pageName) || isEditorSubPage(pageName)) {
            if ("General".equals(pageName)) {
                if (currentEditorGeneralPage == null) {
                    currentEditorGeneralPage = new SettingsEditorGeneralPage();
                }
                currentEditorGeneralPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentEditorGeneralPage);
                updateApplyButtonState();
                return;
            }
            if ("Auto Import".equals(pageName)) {
                if (currentAutoImportPage == null) {
                    currentAutoImportPage = new SettingsAutoImportPage();
                }
                currentAutoImportPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentAutoImportPage);
                updateApplyButtonState();
                return;
            }
            if ("Appearance".equals(pageName)) {
                if (currentEditorAppearancePage == null) {
                    currentEditorAppearancePage = new SettingsAppearancePage();
                }
                currentEditorAppearancePage.setOnModifiedListener(this::updateApplyButtonState);
                currentEditorAppearancePage.setOnNavigateReaderMode(() -> {
                    TreeItem<String> readerModeItem = findItem(tree.getRoot(), "Reader Mode");
                    if (readerModeItem != null) {
                        expandAncestors(readerModeItem);
                        tree.getSelectionModel().select(readerModeItem);
                    }
                });
                wrapInScroll(currentEditorAppearancePage);
                updateApplyButtonState();
                return;
            }
            if ("Breadcrumbs".equals(pageName)) {
                if (currentBreadcrumbsPage == null) {
                    currentBreadcrumbsPage = new SettingsBreadcrumbsPage();
                }
                currentBreadcrumbsPage.setOnModifiedListener(this::updateApplyButtonState);
                currentBreadcrumbsPage.setOnManageColors(() -> {
                    TreeItem<String> csRoot = findItem(tree.getRoot(), "Color Scheme");
                    if (csRoot != null) {
                        TreeItem<String> csGeneral = findItem(csRoot, "General");
                        if (csGeneral != null) {
                            expandAncestors(csGeneral);
                            tree.getSelectionModel().select(csGeneral);
                        }
                    }
                });
                wrapInScroll(currentBreadcrumbsPage);
                updateApplyButtonState();
                return;
            }
            if ("Code Completion".equals(pageName)) {
                if (currentCodeCompletionPage == null) {
                    currentCodeCompletionPage = new SettingsCodeCompletionPage();
                }
                currentCodeCompletionPage.setOnModifiedListener(this::updateApplyButtonState);
                currentCodeCompletionPage.setOnNavigateInlineCompletion(() -> {
                    TreeItem<String> item = findItem(tree.getRoot(), "Inline Completion");
                    if (item != null) {
                        expandAncestors(item);
                        tree.getSelectionModel().select(item);
                    }
                });
                wrapInScroll(currentCodeCompletionPage);
                updateApplyButtonState();
                return;
            }
            if ("Code Folding".equals(pageName)) {
                if (currentCodeFoldingPage == null) {
                    currentCodeFoldingPage = new SettingsCodeFoldingPage();
                }
                currentCodeFoldingPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentCodeFoldingPage);
                updateApplyButtonState();
                return;
            }
            if ("Console".equals(pageName)) {
                if (currentConsolePage == null) {
                    currentConsolePage = new SettingsConsolePage();
                }
                currentConsolePage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentConsolePage);
                updateApplyButtonState();
                return;
            }
            if ("Editor Tabs".equals(pageName)) {
                if (currentEditorTabsPage == null) {
                    currentEditorTabsPage = new SettingsEditorTabsPage();
                }
                currentEditorTabsPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentEditorTabsPage);
                updateApplyButtonState();
                return;
            }
            if ("Gutter Icons".equals(pageName)) {
                if (currentGutterIconsPage == null) {
                    currentGutterIconsPage = new SettingsGutterIconsPage();
                }
                currentGutterIconsPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentGutterIconsPage);
                updateApplyButtonState();
                return;
            }
            if ("Inline Completion".equals(pageName)) {
                if (currentInlineCompletionPage == null) {
                    currentInlineCompletionPage = new SettingsInlineCompletionPage();
                }
                currentInlineCompletionPage.setOnModifiedListener(this::updateApplyButtonState);
                currentInlineCompletionPage.setOnNavigateCodeCompletion(() -> {
                    TreeItem<String> item = findItem(tree.getRoot(), "Code Completion");
                    if (item != null) {
                        expandAncestors(item);
                        tree.getSelectionModel().select(item);
                    }
                });
                wrapInScroll(currentInlineCompletionPage);
                updateApplyButtonState();
                return;
            }
            if ("Postfix Completion".equals(pageName)) {
                if (currentPostfixCompletionPage == null) {
                    currentPostfixCompletionPage = new SettingsPostfixCompletionPage();
                }
                currentPostfixCompletionPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentPostfixCompletionPage);
                updateApplyButtonState();
                return;
            }
            if ("Smart Keys".equals(pageName)) {
                if (currentSmartKeysPage == null) {
                    currentSmartKeysPage = new SettingsSmartKeysPage();
                }
                currentSmartKeysPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentSmartKeysPage);
                updateApplyButtonState();
                return;
            }
            if ("YAML".equals(pageName)) {
                if (currentSmartKeysYamlPage == null) {
                    currentSmartKeysYamlPage = new SettingsSmartKeysYAMLPage();
                }
                currentSmartKeysYamlPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentSmartKeysYamlPage);
                updateApplyButtonState();
                return;
            }
            if ("HTML/CSS".equals(pageName)) {
                if (currentSmartKeysHtmlCssPage == null) {
                    currentSmartKeysHtmlCssPage = new SettingsSmartKeysHTMLCSSPage();
                }
                currentSmartKeysHtmlCssPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentSmartKeysHtmlCssPage);
                updateApplyButtonState();
                return;
            }
            if ("Python".equals(pageName)) {
                if (currentSmartKeysPythonPage == null) {
                    currentSmartKeysPythonPage = new SettingsSmartKeysPythonPage();
                }
                currentSmartKeysPythonPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentSmartKeysPythonPage);
                updateApplyButtonState();
                return;
            }
            if ("JSON".equals(pageName)) {
                if (currentSmartKeysJsonPage == null) {
                    currentSmartKeysJsonPage = new SettingsSmartKeysJSONPage();
                }
                currentSmartKeysJsonPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentSmartKeysJsonPage);
                updateApplyButtonState();
                return;
            }
            if ("Rust".equals(pageName)) {
                if (currentSmartKeysRustPage == null) {
                    currentSmartKeysRustPage = new SettingsSmartKeysRustPage();
                }
                currentSmartKeysRustPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentSmartKeysRustPage);
                updateApplyButtonState();
                return;
            }
            if ("Markdown".equals(pageName)) {
                if (currentSmartKeysMarkdownPage == null) {
                    currentSmartKeysMarkdownPage = new SettingsSmartKeysMarkdownPage();
                }
                currentSmartKeysMarkdownPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentSmartKeysMarkdownPage);
                updateApplyButtonState();
                return;
            }
            if ("Scala".equals(pageName)) {
                if (currentSmartKeysScalaPage == null) {
                    currentSmartKeysScalaPage = new SettingsSmartKeysScalaPage();
                }
                currentSmartKeysScalaPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentSmartKeysScalaPage);
                updateApplyButtonState();
                return;
            }
            if ("SQL".equals(pageName)) {
                if (currentSmartKeysSqlPage == null) {
                    currentSmartKeysSqlPage = new SettingsSmartKeysSQLPage();
                }
                currentSmartKeysSqlPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentSmartKeysSqlPage);
                updateApplyButtonState();
                return;
            }
            if ("Ruby".equals(pageName)) {
                if (currentSmartKeysRubyPage == null) {
                    currentSmartKeysRubyPage = new SettingsSmartKeysRubyPage();
                }
                currentSmartKeysRubyPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentSmartKeysRubyPage);
                updateApplyButtonState();
                return;
            }
            if ("JavaScript".equals(pageName)) {
                if (currentSmartKeysJsPage == null) {
                    currentSmartKeysJsPage = new SettingsSmartKeysJavaScriptPage();
                }
                currentSmartKeysJsPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentSmartKeysJsPage);
                updateApplyButtonState();
                return;
            }
            if ("PHP".equals(pageName)) {
                if (currentSmartKeysPhpPage == null) {
                    currentSmartKeysPhpPage = new SettingsSmartKeysPHPPage();
                }
                currentSmartKeysPhpPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentSmartKeysPhpPage);
                updateApplyButtonState();
                return;
            }
            if ("Sticky Lines".equals(pageName)) {
                if (currentStickyLinesPage == null) {
                    currentStickyLinesPage = new SettingsStickyLinesPage();
                }
                currentStickyLinesPage.setOnModifiedListener(this::updateApplyButtonState);
                currentStickyLinesPage.setOnManageColors(() -> {
                    TreeItem<String> csRoot = findItem(tree.getRoot(), "Color Scheme");
                    if (csRoot != null) {
                        TreeItem<String> csGeneral = findItem(csRoot, "General");
                        if (csGeneral != null) {
                            expandAncestors(csGeneral);
                            tree.getSelectionModel().select(csGeneral);
                        }
                    }
                });
                wrapInScroll(currentStickyLinesPage);
                updateApplyButtonState();
                return;
            }
            if ("Code Editing".equals(pageName)) {
                if (currentCodeEditingPage == null) {
                    currentCodeEditingPage = new SettingsCodeEditingPage();
                }
                currentCodeEditingPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentCodeEditingPage);
                updateApplyButtonState();
                return;
            }
            if ("Font".equals(pageName)) {
                if (currentFontPage == null) {
                    currentFontPage = new SettingsFontPage();
                }
                currentFontPage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentFontPage);
                updateApplyButtonState();
                return;
            }
            if ("Color Scheme".equals(pageName)) {
                if (currentColorSchemePage == null) {
                    currentColorSchemePage = new SettingsColorSchemePage();
                }
                currentColorSchemePage.setOnNavigate(cat -> {
                    TreeItem<String> csRoot = findItem(tree.getRoot(), "Color Scheme");
                    if (csRoot != null) {
                        TreeItem<String> item = findItem(csRoot, cat);
                        if (item != null) {
                            expandAncestors(item);
                            tree.getSelectionModel().select(item);
                        }
                    }
                });
                currentColorSchemePage.getHeaderBar().setOnNavigateToTheme(() -> {
                    TreeItem<String> appRoot = findItem(tree.getRoot(), "Appearance & Behavior");
                    if (appRoot != null) {
                        TreeItem<String> appItem = findItem(appRoot, "Appearance");
                        if (appItem != null) {
                            expandAncestors(appItem);
                            tree.getSelectionModel().select(appItem);
                        }
                    }
                });
                currentColorSchemePage.setOnModifiedListener(this::updateApplyButtonState);
                wrapInScroll(currentColorSchemePage);
                updateApplyButtonState();
                return;
            }
            buildEditorPage(pageName);
            return;
        }

        // 6. Other Appearance & Behavior or root pages
        if ("Menus and Toolbars".equals(pageName)) {
            buildMenusToolbarsPage();
        } else if ("System Settings".equals(pageName)) {
            buildSystemSettingsPage();
        } else if (isSystemSettingsSubPage(pageName)) {
            buildSystemSettingsSubPage(pageName);
        } else if ("File Colors".equals(pageName)) {
            buildFileColorsPage();
        } else if ("Scopes".equals(pageName)) {
            buildScopesPage();
        } else if ("Notifications".equals(pageName)) {
            buildNotificationsPage();
        } else if ("Data Editor and Viewer".equals(pageName)) {
            buildDataEditorPage();
        } else if ("Quick Lists".equals(pageName)) {
            buildQuickListsPage();
        } else if ("Required Plugins".equals(pageName)) {
            buildRequiredPluginsPage();
        } else if ("Trusted Locations".equals(pageName)) {
            buildTrustedLocationsPage();
        } else if ("Path Variables".equals(pageName)) {
            buildPathVariablesPage();
        } else if ("Presentation Assistant".equals(pageName)) {
            buildPresentationAssistantPage();
        } else if (isKeymapPage(pageName)) {
            buildKeymapPage();
        } else if ("Changelists".equals(pageName)) {
            buildVcsChangelistsPage();
        } else if ("Commit".equals(pageName)) {
            buildVcsCommitPage();
        } else if ("Confirmation".equals(pageName)) {
            buildVcsConfirmationPage();
        } else if ("Directory Mappings".equals(pageName)) {
            buildVcsDirectoryMappingsPage();
        } else if ("File Status Colors".equals(pageName)) {
            buildVcsFileStatusColorsPage();
        } else if ("Issue Navigation".equals(pageName)) {
            buildVcsIssueNavigationPage();
        } else if ("Log".equals(pageName)) {
            buildVcsLogPage();
        } else if ("Shelf".equals(pageName)) {
            buildVcsShelfPage();
        } else if ("Git".equals(pageName)) {
            buildVcsGitPage();
        } else if ("GitHub".equals(pageName)) {
            buildVcsGitHubPage();
        } else if ("GitLab".equals(pageName)) {
            buildVcsGitLabPage();
        } else if ("Mercurial".equals(pageName)) {
            buildVcsMercurialPage();
        } else if ("Perforce".equals(pageName)) {
            buildVcsPerforcePage();
        } else if ("Perforce MCP".equals(pageName)) {
            buildVcsPerforceMcpPage();
        } else if ("Subversion".equals(pageName)) {
            buildVcsSubversionPage();
        } else if ("Network".equals(pageName) && isUnderSubversion(selected)) {
            buildVcsSubversionNetworkPage();
        } else if ("Presentation".equals(pageName) && isUnderSubversion(selected)) {
            buildVcsSubversionPresentationPage();
        } else if ("SSH".equals(pageName) && isUnderSubversion(selected)) {
            buildVcsSubversionSshPage();
        } else if ("Terminal".equals(pageName)) {
            buildTerminalSettingsPage();
        } else {
            // If it has children, show category overview
            if (!selected.getChildren().isEmpty()) {
                SettingsCategoryOverviewPage overview = new SettingsCategoryOverviewPage(selected, child -> {
                    expandAncestors(child);
                    tree.getSelectionModel().select(child);
                });
                wrapInScroll(overview);
            } else {
                // Placeholder for other pages
                Label title = new Label(pageName);
                title.getStyleClass().add("settings-page-title");

                Label placeholder = new Label("Settings for '" + pageName + "' will be available in a future update.");
                placeholder.getStyleClass().add("settings-placeholder");

                VBox box = new VBox(20, title, placeholder);
                box.setPadding(new Insets(40, 24, 20, 24));
                box.getStyleClass().add("settings-page");
                wrapInScroll(box);
            }
        }
    }

    private void wrapInScroll(javafx.scene.Node page) {
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("settings-scroll");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: #1E1F22; -fx-background: #1E1F22; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        contentContainer.setStyle("-fx-background-color: #1E1F22;");
        contentContainer.getChildren().setAll(scroll);
    }

    private void buildEditorPage(String pageName) {
        SettingsEditorPage page = new SettingsEditorPage();
        page.showEditorPage(pageName);
        wrapInScroll(page);
    }

    private void buildKeymapPage() {
        SettingsKeymapPage page = new SettingsKeymapPage(() -> {
            TreeItem<String> pluginsItem = findItem(tree.getRoot(), "Plugins");
            if (pluginsItem != null) {
                expandAncestors(pluginsItem);
                tree.getSelectionModel().select(pluginsItem);
            }
        });
        VBox.setVgrow(page, Priority.ALWAYS);
        contentContainer.getChildren().setAll(page);
    }

    private void buildMenusToolbarsPage() {
        currentMenusToolbarsPage = new SettingsMenusToolbarsPage();
        VBox.setVgrow(currentMenusToolbarsPage, Priority.ALWAYS);
        contentContainer.getChildren().setAll(currentMenusToolbarsPage);
    }

    private void buildFileColorsPage() {
        SettingsFileColorsPage page = new SettingsFileColorsPage(() -> {
            TreeItem<String> scopesItem = findItem(tree.getRoot(), "Scopes");
            if (scopesItem != null) {
                expandAncestors(scopesItem);
                tree.getSelectionModel().select(scopesItem);
            }
        });
        wrapInScroll(page);
    }

    private void buildTerminalSettingsPage() {
        SettingsTerminalPage page = new SettingsTerminalPage();
        wrapInScroll(page);
    }

    private void buildVcsChangelistsPage() {
        SettingsVcsChangelistsPage page = new SettingsVcsChangelistsPage();
        wrapInScroll(page);
    }

    private void buildVcsCommitPage() {
        SettingsVcsCommitPage page = new SettingsVcsCommitPage();
        wrapInScroll(page);
    }

    private void buildVcsConfirmationPage() {
        SettingsVcsConfirmationPage page = new SettingsVcsConfirmationPage();
        wrapInScroll(page);
    }

    private void buildVcsDirectoryMappingsPage() {
        SettingsVcsDirectoryMappingsPage page = new SettingsVcsDirectoryMappingsPage();
        wrapInScroll(page);
    }

    private void buildVcsFileStatusColorsPage() {
        SettingsVcsFileStatusColorsPage page = new SettingsVcsFileStatusColorsPage();
        wrapInScroll(page);
    }

    private void buildVcsIssueNavigationPage() {
        SettingsVcsIssueNavigationPage page = new SettingsVcsIssueNavigationPage();
        wrapInScroll(page);
    }

    private void buildVcsLogPage() {
        SettingsVcsLogPage page = new SettingsVcsLogPage();
        wrapInScroll(page);
    }

    private void buildVcsShelfPage() {
        SettingsVcsShelfPage page = new SettingsVcsShelfPage();
        wrapInScroll(page);
    }

    private void buildVcsGitPage() {
        SettingsVcsGitPage page = new SettingsVcsGitPage();
        wrapInScroll(page);
    }

    private void buildVcsGitHubPage() {
        SettingsVcsGitHubPage page = new SettingsVcsGitHubPage();
        wrapInScroll(page);
    }

    private void buildVcsGitLabPage() {
        SettingsVcsGitLabPage page = new SettingsVcsGitLabPage();
        wrapInScroll(page);
    }

    private void buildVcsMercurialPage() {
        SettingsVcsMercurialPage page = new SettingsVcsMercurialPage();
        wrapInScroll(page);
    }

    private void buildVcsPerforcePage() {
        SettingsVcsPerforcePage page = new SettingsVcsPerforcePage();
        wrapInScroll(page);
    }

    private void buildVcsPerforceMcpPage() {
        SettingsVcsPerforceMcpPage page = new SettingsVcsPerforceMcpPage();
        wrapInScroll(page);
    }

    private void buildVcsSubversionPage() {
        SettingsVcsSubversionPage page = new SettingsVcsSubversionPage();
        wrapInScroll(page);
    }

    private void buildVcsSubversionNetworkPage() {
        SettingsVcsSubversionNetworkPage page = new SettingsVcsSubversionNetworkPage();
        wrapInScroll(page);
    }

    private void buildVcsSubversionPresentationPage() {
        SettingsVcsSubversionPresentationPage page = new SettingsVcsSubversionPresentationPage();
        wrapInScroll(page);
    }

    private void buildVcsSubversionSshPage() {
        SettingsVcsSubversionSshPage page = new SettingsVcsSubversionSshPage();
        wrapInScroll(page);
    }

    private boolean isUnderSubversion(TreeItem<String> item) {
        TreeItem<String> p = item != null ? item.getParent() : null;
        while (p != null) {
            if ("Subversion".equals(p.getValue())) return true;
            p = p.getParent();
        }
        return false;
    }

    private void buildScopesPage() {
        SettingsScopesPage page = new SettingsScopesPage();
        VBox.setVgrow(page, Priority.ALWAYS);
        contentContainer.getChildren().setAll(page);
    }

    private void buildNotificationsPage() {
        SettingsNotificationsPage page = new SettingsNotificationsPage();
        wrapInScroll(page);
    }

    private void buildDataEditorPage() {
        SettingsDataEditorPage page = new SettingsDataEditorPage();
        wrapInScroll(page);
    }

    private void buildQuickListsPage() {
        currentQuickListsPage = new SettingsQuickListsPage();
        VBox.setVgrow(currentQuickListsPage, Priority.ALWAYS);
        contentContainer.getChildren().setAll(currentQuickListsPage);
    }

    private void buildRequiredPluginsPage() {
        SettingsRequiredPluginsPage page = new SettingsRequiredPluginsPage();
        VBox.setVgrow(page, Priority.ALWAYS);
        contentContainer.getChildren().setAll(page);
    }

    private void buildTrustedLocationsPage() {
        SettingsTrustedLocationsPage page = new SettingsTrustedLocationsPage();
        VBox.setVgrow(page, Priority.ALWAYS);
        contentContainer.getChildren().setAll(page);
    }

    private void buildPathVariablesPage() {
        SettingsPathVariablesPage page = new SettingsPathVariablesPage();
        VBox.setVgrow(page, Priority.ALWAYS);
        contentContainer.getChildren().setAll(page);
    }

    private void buildPresentationAssistantPage() {
        SettingsPresentationAssistantPage page = new SettingsPresentationAssistantPage();
        wrapInScroll(page);
    }

    private void buildColorSchemePage(String pageName) {
        if ("Color Scheme Font".equals(pageName)) {
            if (currentColorSchemeFontPage == null) {
                currentColorSchemeFontPage = new SettingsColorSchemeFontPage();
            }
            currentColorSchemeFontPage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentColorSchemeFontPage);
            return;
        }
        if ("Console Font".equals(pageName)) {
            if (currentConsoleFontPage == null) {
                currentConsoleFontPage = new SettingsConsoleFontPage();
            }
            currentConsoleFontPage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentConsoleFontPage);
            return;
        }
        if ("Console Colors".equals(pageName)) {
            if (currentConsoleColorsPage == null) {
                currentConsoleColorsPage = new SettingsConsoleColorsPage();
            }
            currentConsoleColorsPage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentConsoleColorsPage);
            return;
        }
        if ("Code With Me".equals(pageName)) {
            if (currentCodeWithMePage == null) {
                currentCodeWithMePage = new SettingsCodeWithMePage();
            }
            currentCodeWithMePage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentCodeWithMePage);
            return;
        }
        if ("Debugger".equals(pageName)) {
            if (currentColorSchemeDebuggerPage == null) {
                currentColorSchemeDebuggerPage = new SettingsColorSchemeDebuggerPage();
            }
            currentColorSchemeDebuggerPage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentColorSchemeDebuggerPage);
            return;
        }
        if ("Diff & Merge".equals(pageName)) {
            if (currentColorSchemeDiffMergePage == null) {
                currentColorSchemeDiffMergePage = new SettingsColorSchemeDiffMergePage();
            }
            currentColorSchemeDiffMergePage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentColorSchemeDiffMergePage);
            return;
        }
        if ("JVM Logging".equals(pageName)) {
            if (currentColorSchemeJvmLoggingPage == null) {
                currentColorSchemeJvmLoggingPage = new SettingsColorSchemeJvmLoggingPage();
            }
            currentColorSchemeJvmLoggingPage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentColorSchemeJvmLoggingPage);
            return;
        }
        if ("User-Defined File Types".equals(pageName)) {
            if (currentColorSchemeUserDefinedFileTypesPage == null) {
                currentColorSchemeUserDefinedFileTypesPage = new SettingsColorSchemeUserDefinedFileTypesPage();
            }
            currentColorSchemeUserDefinedFileTypesPage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentColorSchemeUserDefinedFileTypesPage);
            return;
        }
        if ("VCS".equals(pageName)) {
            if (currentColorSchemeVcsPage == null) {
                currentColorSchemeVcsPage = new SettingsColorSchemeVcsPage();
            }
            currentColorSchemeVcsPage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentColorSchemeVcsPage);
            return;
        }
        if ("Java".equals(pageName)) {
            if (currentColorSchemeJavaPage == null) {
                currentColorSchemeJavaPage = new SettingsColorSchemeJavaPage();
            }
            currentColorSchemeJavaPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeJavaPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeJavaPage);
            return;
        }
        if ("Angular Template".equals(pageName)) {
            if (currentColorSchemeAngularTemplatePage == null) {
                currentColorSchemeAngularTemplatePage = new SettingsColorSchemeAngularTemplatePage();
            }
            currentColorSchemeAngularTemplatePage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeAngularTemplatePage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeAngularTemplatePage);
            return;
        }
        if ("Context Free Grammar".equals(pageName)) {
            if (currentColorSchemeContextFreeGrammarPage == null) {
                currentColorSchemeContextFreeGrammarPage = new SettingsColorSchemeContextFreeGrammarPage();
            }
            currentColorSchemeContextFreeGrammarPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeContextFreeGrammarPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeContextFreeGrammarPage);
            return;
        }
        if ("CSS".equals(pageName)) {
            if (currentColorSchemeCssPage == null) {
                currentColorSchemeCssPage = new SettingsColorSchemeCssPage();
            }
            currentColorSchemeCssPage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentColorSchemeCssPage);
            return;
        }
        if ("Data Editor and Viewer".equals(pageName)) {
            if (currentColorSchemeDataEditorViewerPage == null) {
                currentColorSchemeDataEditorViewerPage = new SettingsColorSchemeDataEditorViewerPage();
            }
            currentColorSchemeDataEditorViewerPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeDataEditorViewerPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("General");
                if (currentColorSchemeGeneralPage != null) {
                    currentColorSchemeGeneralPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeDataEditorViewerPage);
            return;
        }
        if ("Database".equals(pageName)) {
            if (currentColorSchemeDatabasePage == null) {
                currentColorSchemeDatabasePage = new SettingsColorSchemeDatabasePage();
            }
            currentColorSchemeDatabasePage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentColorSchemeDatabasePage);
            return;
        }
        if ("Diagrams".equals(pageName)) {
            if (currentColorSchemeDiagramsPage == null) {
                currentColorSchemeDiagramsPage = new SettingsColorSchemeDiagramsPage();
            }
            currentColorSchemeDiagramsPage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentColorSchemeDiagramsPage);
            return;
        }
        if ("Dockerfile".equals(pageName)) {
            if (currentColorSchemeDockerfilePage == null) {
                currentColorSchemeDockerfilePage = new SettingsColorSchemeDockerfilePage();
            }
            currentColorSchemeDockerfilePage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentColorSchemeDockerfilePage);
            return;
        }
        if ("EditorConfig".equals(pageName)) {
            if (currentColorSchemeEditorConfigPage == null) {
                currentColorSchemeEditorConfigPage = new SettingsColorSchemeEditorConfigPage();
            }
            currentColorSchemeEditorConfigPage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentColorSchemeEditorConfigPage);
            return;
        }
        if ("ERB".equals(pageName)) {
            if (currentColorSchemeErbPage == null) {
                currentColorSchemeErbPage = new SettingsColorSchemeErbPage();
            }
            currentColorSchemeErbPage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentColorSchemeErbPage);
            return;
        }
        if ("FreeMarker".equals(pageName)) {
            if (currentColorSchemeFreeMarkerPage == null) {
                currentColorSchemeFreeMarkerPage = new SettingsColorSchemeFreeMarkerPage();
            }
            currentColorSchemeFreeMarkerPage.setOnModifiedListener(this::updateApplyButtonState);
            wrapInScroll(currentColorSchemeFreeMarkerPage);
            return;
        }
        if ("GitLab CI Expression".equals(pageName)) {
            if (currentColorSchemeGitLabCiExpressionPage == null) {
                currentColorSchemeGitLabCiExpressionPage = new SettingsColorSchemeGitLabCiExpressionPage();
            }
            currentColorSchemeGitLabCiExpressionPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeGitLabCiExpressionPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeGitLabCiExpressionPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeGitLabCiExpressionPage);
            return;
        }
        if ("Go".equals(pageName)) {
            if (currentColorSchemeGoPage == null) {
                currentColorSchemeGoPage = new SettingsColorSchemeGoPage();
            }
            currentColorSchemeGoPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeGoPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeGoPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeGoPage);
            return;
        }
        if ("Gradle Declarative Configuration".equals(pageName)) {
            if (currentColorSchemeGradleDeclarativePage == null) {
                currentColorSchemeGradleDeclarativePage = new SettingsColorSchemeGradleDeclarativePage();
            }
            currentColorSchemeGradleDeclarativePage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeGradleDeclarativePage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeGradleDeclarativePage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeGradleDeclarativePage);
            return;
        }
        if ("Groovy".equals(pageName)) {
            if (currentColorSchemeGroovyPage == null) {
                currentColorSchemeGroovyPage = new SettingsColorSchemeGroovyPage();
            }
            currentColorSchemeGroovyPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeGroovyPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeGroovyPage.setOnNavigateToInheritedListener(targetKey -> {
                if (targetKey != null && targetKey.endsWith("(Groovy)")) {
                    String baseKey = targetKey.substring(0, targetKey.indexOf("(Groovy)")).trim();
                    currentColorSchemeGroovyPage.selectCategory(baseKey);
                } else {
                    selectCategory("Language Defaults");
                    if (currentColorSchemeLanguageDefaultsPage != null) {
                        currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                    }
                }
            });
            wrapInScroll(currentColorSchemeGroovyPage);
            return;
        }
        if ("HTML".equals(pageName)) {
            if (currentColorSchemeHtmlPage == null) {
                currentColorSchemeHtmlPage = new SettingsColorSchemeHtmlPage();
            }
            currentColorSchemeHtmlPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeHtmlPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeHtmlPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeHtmlPage);
            return;
        }
        if ("HTTP Request".equals(pageName)) {
            if (currentColorSchemeHttpRequestPage == null) {
                currentColorSchemeHttpRequestPage = new SettingsColorSchemeHttpRequestPage();
            }
            currentColorSchemeHttpRequestPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeHttpRequestPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeHttpRequestPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeHttpRequestPage);
            return;
        }
        if ("JavaScript".equals(pageName)) {
            if (currentColorSchemeJavaScriptPage == null) {
                currentColorSchemeJavaScriptPage = new SettingsColorSchemeJavaScriptPage();
            }
            currentColorSchemeJavaScriptPage.setOnModifiedListener(this::updateApplyButtonState);
            currentColorSchemeJavaScriptPage.getHeaderBar().setOnNavigateToTheme(() -> selectCategory("Appearance"));
            currentColorSchemeJavaScriptPage.setOnNavigateToInheritedListener(targetKey -> {
                selectCategory("Language Defaults");
                if (currentColorSchemeLanguageDefaultsPage != null) {
                    currentColorSchemeLanguageDefaultsPage.selectTreeItem(targetKey);
                }
            });
            wrapInScroll(currentColorSchemeJavaScriptPage);
            return;
        }
        VBox page = new VBox(12);
        page.setPadding(new Insets(16, 24, 20, 24));
        page.setStyle("-fx-background-color: #1E1F22;");
        ColorSchemeHeaderBar bar = new ColorSchemeHeaderBar();
        Label title = new Label(pageName);
        title.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label desc = new Label("Configure color scheme settings for " + pageName + ".");
        desc.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 13px;");
        page.getChildren().addAll(bar, title, desc);
        wrapInScroll(page);
    }

    private void buildSystemSettingsPage() {
        currentSystemPage = new SettingsSystemPage();
        currentSystemPage.showSubPage("System Settings");
        wrapInScroll(currentSystemPage);
    }

    private void buildSystemSettingsSubPage(String pageName) {
        currentSystemPage = new SettingsSystemPage();
        currentSystemPage.showSubPage(pageName);
        wrapInScroll(currentSystemPage);
    }

    private boolean isSystemSettingsSubPage(String pageName) {
        return List.of(
                "Data Sharing", "Date Formats", "HTTP Proxy", "Language and Region",
                "Passwords", "Process Elevation", "Server Certificates", "Trusted Hosts", "Updates"
        ).contains(pageName);
    }

    private boolean isEditorSubPage(String pageName) {
        return List.of(
                "General", "Auto Import", "Appearance", "Breadcrumbs", "Code Completion",
                "Code Folding", "Console", "Editor Tabs", "Gutter Icons", "Inline Completion",
                "Postfix Completion", "Sticky Lines", "Smart Keys", "YAML", "HTML/CSS", "Python",
                "JSON", "Rust", "Markdown", "Scala", "SQL", "Ruby", "JavaScript", "PHP",
                "Code Editing", "Font", "Color Scheme",
                "Code Style", "Inspections", "File and Code Templates", "File Encodings",
                "Live Templates", "File Types", "Copyright", "Inlay Hints", "Duplicates",
                "Emmet", "Intentions", "Language Injections", "Natural Languages",
                "Reader Mode", "TextMate Bundles", "TODO"
        ).contains(pageName);
    }

    private boolean isKeymapPage(String pageName) {
        return "Keymap".equals(pageName) || List.of(
                "Editor Actions", "Main Menu", "Tool Windows", "External Tools",
                "External Build Systems", "Version Control Systems", "Debugger Actions",
                "Remote External Tools", "Database", "Macros", "Intentions",
                "Quick Lists", "Plugins", "Other"
        ).contains(pageName);
    }

    // --------------------------------------------------- Category Tree

    private TreeView<String> buildCategoryTree() {
        TreeItem<String> root = new TreeItem<>("Settings");
        root.setExpanded(true);

        // Appearance & Behavior
        TreeItem<String> appearance = new TreeItem<>("Appearance & Behavior");
        TreeItem<String> appearanceSub = new TreeItem<>("Appearance");
        TreeItem<String> menus = new TreeItem<>("Menus and Toolbars");

        // System Settings with sub-pages
        TreeItem<String> system = new TreeItem<>("System Settings");
        system.getChildren().addAll(
                new TreeItem<>("Data Sharing"),
                new TreeItem<>("Date Formats"),
                new TreeItem<>("HTTP Proxy"),
                new TreeItem<>("Language and Region"),
                new TreeItem<>("Passwords"),
                new TreeItem<>("Process Elevation"),
                new TreeItem<>("Server Certificates"),
                new TreeItem<>("Trusted Hosts"),
                new TreeItem<>("Updates")
        );

        appearance.getChildren().addAll(
                appearanceSub, menus, system,
                new TreeItem<>("File Colors"),
                new TreeItem<>("Scopes"),
                new TreeItem<>("Notifications"),
                new TreeItem<>("Data Editor and Viewer"),
                new TreeItem<>("Quick Lists"),
                new TreeItem<>("Required Plugins"),
                new TreeItem<>("Trusted Locations"),
                new TreeItem<>("Path Variables"),
                new TreeItem<>("Presentation Assistant")
        );

        // Keymap
        TreeItem<String> keymap = new TreeItem<>("Keymap");

        // Editor
        TreeItem<String> editor = new TreeItem<>("Editor");
        TreeItem<String> general = new TreeItem<>("General");

        TreeItem<String> smartKeys = new TreeItem<>("Smart Keys");
        smartKeys.getChildren().addAll(
                new TreeItem<>("YAML"),
                new TreeItem<>("HTML/CSS"),
                new TreeItem<>("Python"),
                new TreeItem<>("JSON"),
                new TreeItem<>("Rust"),
                new TreeItem<>("Markdown"),
                new TreeItem<>("Scala"),
                new TreeItem<>("SQL"),
                new TreeItem<>("Ruby"),
                new TreeItem<>("JavaScript"),
                new TreeItem<>("PHP")
        );

        general.getChildren().addAll(
                new TreeItem<>("Auto Import"),
                new TreeItem<>("Appearance"),
                new TreeItem<>("Breadcrumbs"),
                new TreeItem<>("Code Completion"),
                new TreeItem<>("Code Folding"),
                new TreeItem<>("Console"),
                new TreeItem<>("Editor Tabs"),
                new TreeItem<>("Gutter Icons"),
                new TreeItem<>("Inline Completion"),
                new TreeItem<>("Postfix Completion"),
                smartKeys,
                new TreeItem<>("Sticky Lines")
        );

        TreeItem<String> colorSchemeNode = new TreeItem<>("Color Scheme");
        colorSchemeNode.getChildren().addAll(
                new TreeItem<>("General"),
                new TreeItem<>("Language Defaults"),
                new TreeItem<>("Color Scheme Font"),
                new TreeItem<>("Console Font"),
                new TreeItem<>("Code With Me"),
                new TreeItem<>("Console Colors"),
                new TreeItem<>("Debugger"),
                new TreeItem<>("Diff & Merge"),
                new TreeItem<>("JVM Logging"),
                new TreeItem<>("User-Defined File Types"),
                new TreeItem<>("VCS"),
                new TreeItem<>("Java"),
                new TreeItem<>("Angular Template"),
                new TreeItem<>("Context Free Grammar"),
                new TreeItem<>("CSS"),
                new TreeItem<>("Data Editor and Viewer"),
                new TreeItem<>("Database"),
                new TreeItem<>("Diagrams"),
                new TreeItem<>("Dockerfile"),
                new TreeItem<>("EditorConfig"),
                new TreeItem<>("ERB"),
                new TreeItem<>("FreeMarker"),
                new TreeItem<>("GitLab CI Expression"),
                new TreeItem<>("Go"),
                new TreeItem<>("Gradle Declarative Configuration"),
                new TreeItem<>("Groovy"),
                new TreeItem<>("HTML"),
                new TreeItem<>("HTTP Request"),
                new TreeItem<>("JavaScript"),
                new TreeItem<>("JPA/Hibernate QL"),
                new TreeItem<>("JSON"),
                new TreeItem<>("JSONPath"),
                new TreeItem<>("JSP"),
                new TreeItem<>("Jupyter Notebooks"),
                new TreeItem<>("Kotlin"),
                new TreeItem<>("Kubernetes"),
                new TreeItem<>("Less"),
                new TreeItem<>("Lombok Config"),
                new TreeItem<>("Markdown"),
                new TreeItem<>("Micronaut EL"),
                new TreeItem<>("MongoDB JSON"),
                new TreeItem<>("PostCSS"),
                new TreeItem<>("Properties"),
                new TreeItem<>("Protocol Buffer"),
                new TreeItem<>("Protocol Buffer Text"),
                new TreeItem<>("Qute"),
                new TreeItem<>("RegExp"),
                new TreeItem<>("Rust"),
                new TreeItem<>("Sass/SCSS"),
                new TreeItem<>("Shell Script"),
                new TreeItem<>("Spring EL"),
                new TreeItem<>("SQL"),
                new TreeItem<>("Table Diff"),
                new TreeItem<>("TOML"),
                new TreeItem<>("TypeScript"),
                new TreeItem<>("Velocity"),
                new TreeItem<>("XML"),
                new TreeItem<>("XPath"),
                new TreeItem<>("XSLT"),
                new TreeItem<>("YAML"),
                new TreeItem<>("By Scope"),
                new TreeItem<>("Images")
        );

        TreeItem<String> copyright = new TreeItem<>("Copyright");
        TreeItem<String> copyrightProfiles = new TreeItem<>("Copyright Profiles");
        TreeItem<String> formatting = new TreeItem<>("Formatting");
        formatting.getChildren().addAll(
                new TreeItem<>("CSS"), new TreeItem<>("DTD"), new TreeItem<>("Groovy"),
                new TreeItem<>("HTML"), new TreeItem<>("Java"), new TreeItem<>("JavaScript"),
                new TreeItem<>("JSP"), new TreeItem<>("JSPX"), new TreeItem<>("Kotlin"),
                new TreeItem<>("Less"), new TreeItem<>("PostCSS"), new TreeItem<>("Properties"),
                new TreeItem<>("Rust"), new TreeItem<>("Sass"), new TreeItem<>("SCSS"),
                new TreeItem<>("Shell Script"), new TreeItem<>("SPI"), new TreeItem<>("SQL"),
                new TreeItem<>("SVG"), new TreeItem<>("TypeScript"), new TreeItem<>("Vue template"),
                new TreeItem<>("XHTML"), new TreeItem<>("XML")
        );
        copyright.getChildren().addAll(copyrightProfiles, formatting);

        editor.getChildren().addAll(
                general,
                new TreeItem<>("Code Editing"),
                new TreeItem<>("Font"),
                colorSchemeNode,
                new TreeItem<>("Code Style"),
                new TreeItem<>("Inspections"),
                new TreeItem<>("File and Code Templates"),
                new TreeItem<>("File Encodings"),
                new TreeItem<>("Live Templates"),
                new TreeItem<>("File Types"),
                copyright,
                new TreeItem<>("Inlay Hints"),
                new TreeItem<>("Duplicates"),
                new TreeItem<>("Emmet"),
                new TreeItem<>("Intentions"),
                new TreeItem<>("Language Injections"),
                new TreeItem<>("Natural Languages"),
                new TreeItem<>("Reader Mode"),
                new TreeItem<>("TextMate Bundles"),
                new TreeItem<>("TODO")
        );

        // Additional root categories
        TreeItem<String> plugins = new TreeItem<>("Plugins");
        TreeItem<String> versionControl = new TreeItem<>("Version Control");
        TreeItem<String> perforce = new TreeItem<>("Perforce");
        perforce.getChildren().add(new TreeItem<>("Perforce MCP"));
        TreeItem<String> subversion = new TreeItem<>("Subversion");
        subversion.getChildren().addAll(
                new TreeItem<>("Network"),
                new TreeItem<>("Presentation"),
                new TreeItem<>("SSH")
        );

        versionControl.getChildren().addAll(
                new TreeItem<>("Changelists"),
                new TreeItem<>("Commit"),
                new TreeItem<>("Confirmation"),
                new TreeItem<>("Directory Mappings"),
                new TreeItem<>("File Status Colors"),
                new TreeItem<>("Issue Navigation"),
                new TreeItem<>("Log"),
                new TreeItem<>("Shelf"),
                new TreeItem<>("Git"),
                new TreeItem<>("GitHub"),
                new TreeItem<>("GitLab"),
                new TreeItem<>("Mercurial"),
                perforce,
                subversion
        );
        TreeItem<String> build = new TreeItem<>("Build, Execution, Deployment");
        TreeItem<String> languages = new TreeItem<>("Languages & Frameworks");

        TreeItem<String> tools = new TreeItem<>("Tools");
        tools.getChildren().addAll(
                new TreeItem<>("Actions on Save"), new TreeItem<>("AI Assistant"),
                new TreeItem<>("Code Provenance"), new TreeItem<>("Code With Me"),
                new TreeItem<>("CSV Formats"), new TreeItem<>("Database"),
                new TreeItem<>("Database Versioning"), new TreeItem<>("Diagrams"),
                new TreeItem<>("Diff & Merge"), new TreeItem<>("External Tools"),
                new TreeItem<>("Features Suggester"), new TreeItem<>("Features Trainer"),
                new TreeItem<>("HTTP Client"), new TreeItem<>("JPA Entity Declaration"),
                new TreeItem<>("JPA Reverse Engineering"), new TreeItem<>("Junie"),
                new TreeItem<>("Jupyter"), new TreeItem<>("Kotlin Notebook"),
                new TreeItem<>("MCP Server"), new TreeItem<>("Qodana"),
                new TreeItem<>("Remote SSH External Tools"), new TreeItem<>("Rsync"),
                new TreeItem<>("Shared Indexes"), new TreeItem<>("SSH Configurations"),
                new TreeItem<>("SSH Terminal"), new TreeItem<>("Startup Tasks"),
                new TreeItem<>("Tasks"), new TreeItem<>("Terminal"),
                new TreeItem<>("Web Browsers and Preview"), new TreeItem<>("XPath Viewer")
        );

        TreeItem<String> backup = new TreeItem<>("Backup and Sync");
        TreeItem<String> advanced = new TreeItem<>("Advanced Settings");

        root.getChildren().addAll(
                appearance, keymap, editor, plugins, versionControl,
                build, languages, tools, backup, advanced
        );

        TreeView<String> tv = new TreeView<>(root);
        tv.setShowRoot(false);
        tv.getStyleClass().add("settings-tree");

        tv.setCellFactory(view -> new TreeCell<>() {
            private final Label titleLabel = new Label();
            private final Region spacer = new Region();
            private final Label badgeLabel = new Label();
            private final SVGPath projectIcon = new SVGPath();
            private final HBox cellBox = new HBox(4, titleLabel, spacer);

            {
                HBox.setHgrow(spacer, Priority.ALWAYS);
                cellBox.setAlignment(Pos.CENTER_LEFT);
                projectIcon.setContent("M 1 2 L 11 2 L 11 10 L 1 10 Z M 1 4 L 11 4");
                projectIcon.setFill(Color.TRANSPARENT);
                projectIcon.setStroke(Color.web("#6F737A"));
                projectIcon.setStrokeWidth(0.8);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    titleLabel.setText(item);
                    titleLabel.setStyle("-fx-text-fill: inherit; -fx-font-size: 13px;");
                    cellBox.getChildren().setAll(titleLabel, spacer);

                    if ("Plugins".equals(item)) {
                        badgeLabel.setText("10");
                        badgeLabel.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-font-size: 10px; -fx-padding: 1 6 1 6; -fx-background-radius: 8;");
                        cellBox.getChildren().add(badgeLabel);
                    } else if (isProjectSetting(item)) {
                        cellBox.getChildren().add(projectIcon);
                    }
                    setText(null);
                    setGraphic(cellBox);
                }
            }
        });

        return tv;
    }

    private static final Set<String> PROJECT_SETTINGS = Set.of(
            "Version Control", "Changelists", "Commit", "Confirmation",
            "Directory Mappings", "Issue Navigation", "Log", "Shelf",
            "Git", "GitHub", "GitLab", "Mercurial", "Perforce", "Subversion",
            "Perforce MCP", "Network", "Presentation", "SSH"
    );

    private static boolean isProjectSetting(String name) {
        return name != null && PROJECT_SETTINGS.contains(name);
    }

    private TreeItem<String> searchTree(TreeItem<String> root, String query) {
        for (TreeItem<String> child : root.getChildren()) {
            if (child.getValue() != null && child.getValue().toLowerCase().contains(query)) {
                return child;
            }
            TreeItem<String> sub = searchTree(child, query);
            if (sub != null) return sub;
        }
        return null;
    }

    private TreeItem<String> findItem(TreeItem<String> root, String text) {
        if (root.getValue() != null && root.getValue().equals(text)) {
            return root;
        }
        for (TreeItem<String> child : root.getChildren()) {
            TreeItem<String> found = findItem(child, text);
            if (found != null) return found;
        }
        return null;
    }

    public void selectCategory(String categoryName) {
        if (categoryName == null || tree == null) return;
        TreeItem<String> item = findItem(tree.getRoot(), categoryName);
        if (item != null) {
            expandAncestors(item);
            tree.getSelectionModel().select(item);
        }
    }

    private void expandAncestors(TreeItem<String> item) {
        TreeItem<String> p = item.getParent();
        while (p != null) {
            p.setExpanded(true);
            p = p.getParent();
        }
    }

    public void applyAll() {
        if (currentIdeAppearancePage != null) {
            currentIdeAppearancePage.save();
        }
        if (currentSystemPage != null) {
            currentSystemPage.save();
        }
        if (currentEditorGeneralPage != null && currentEditorGeneralPage.isModified()) {
            currentEditorGeneralPage.apply();
        }
        if (currentAutoImportPage != null && currentAutoImportPage.isModified()) {
            currentAutoImportPage.apply();
        }
        if (currentEditorAppearancePage != null && currentEditorAppearancePage.isModified()) {
            currentEditorAppearancePage.apply();
        }
        if (currentBreadcrumbsPage != null && currentBreadcrumbsPage.isModified()) {
            currentBreadcrumbsPage.apply();
        }
        if (currentCodeCompletionPage != null && currentCodeCompletionPage.isModified()) {
            currentCodeCompletionPage.apply();
        }
        if (currentCodeFoldingPage != null && currentCodeFoldingPage.isModified()) {
            currentCodeFoldingPage.apply();
        }
        if (currentConsolePage != null && currentConsolePage.isModified()) {
            currentConsolePage.apply();
        }
        if (currentEditorTabsPage != null && currentEditorTabsPage.isModified()) {
            currentEditorTabsPage.apply();
        }
        if (currentGutterIconsPage != null && currentGutterIconsPage.isModified()) {
            currentGutterIconsPage.apply();
        }
        if (currentInlineCompletionPage != null && currentInlineCompletionPage.isModified()) {
            currentInlineCompletionPage.apply();
        }
        if (currentPostfixCompletionPage != null && currentPostfixCompletionPage.isModified()) {
            currentPostfixCompletionPage.apply();
        }
        if (currentSmartKeysPage != null && currentSmartKeysPage.isModified()) {
            currentSmartKeysPage.apply();
        }
        if (currentSmartKeysYamlPage != null && currentSmartKeysYamlPage.isModified()) {
            currentSmartKeysYamlPage.apply();
        }
        if (currentSmartKeysHtmlCssPage != null && currentSmartKeysHtmlCssPage.isModified()) {
            currentSmartKeysHtmlCssPage.apply();
        }
        if (currentSmartKeysPythonPage != null && currentSmartKeysPythonPage.isModified()) {
            currentSmartKeysPythonPage.apply();
        }
        if (currentSmartKeysJsonPage != null && currentSmartKeysJsonPage.isModified()) {
            currentSmartKeysJsonPage.apply();
        }
        if (currentSmartKeysRustPage != null && currentSmartKeysRustPage.isModified()) {
            currentSmartKeysRustPage.apply();
        }
        if (currentSmartKeysMarkdownPage != null && currentSmartKeysMarkdownPage.isModified()) {
            currentSmartKeysMarkdownPage.apply();
        }
        if (currentSmartKeysScalaPage != null && currentSmartKeysScalaPage.isModified()) {
            currentSmartKeysScalaPage.apply();
        }
        if (currentSmartKeysSqlPage != null && currentSmartKeysSqlPage.isModified()) {
            currentSmartKeysSqlPage.apply();
        }
        if (currentSmartKeysRubyPage != null && currentSmartKeysRubyPage.isModified()) {
            currentSmartKeysRubyPage.apply();
        }
        if (currentSmartKeysJsPage != null && currentSmartKeysJsPage.isModified()) {
            currentSmartKeysJsPage.apply();
        }
        if (currentSmartKeysPhpPage != null && currentSmartKeysPhpPage.isModified()) {
            currentSmartKeysPhpPage.apply();
        }
        if (currentStickyLinesPage != null && currentStickyLinesPage.isModified()) {
            currentStickyLinesPage.apply();
        }
        if (currentCodeEditingPage != null && currentCodeEditingPage.isModified()) {
            currentCodeEditingPage.apply();
        }
        if (currentFontPage != null && currentFontPage.isModified()) {
            currentFontPage.apply();
        }
        if (currentColorSchemePage != null && currentColorSchemePage.isModified()) {
            currentColorSchemePage.apply();
        }
        if (currentColorSchemeGeneralPage != null && currentColorSchemeGeneralPage.isModified()) {
            currentColorSchemeGeneralPage.apply();
        }
        if (currentColorSchemeLanguageDefaultsPage != null && currentColorSchemeLanguageDefaultsPage.isModified()) {
            currentColorSchemeLanguageDefaultsPage.apply();
        }
        if (currentColorSchemeFontPage != null && currentColorSchemeFontPage.isModified()) {
            currentColorSchemeFontPage.apply();
        }
        if (currentConsoleFontPage != null && currentConsoleFontPage.isModified()) {
            currentConsoleFontPage.apply();
        }
        if (currentConsoleColorsPage != null && currentConsoleColorsPage.isModified()) {
            currentConsoleColorsPage.apply();
        }
        if (currentCodeWithMePage != null && currentCodeWithMePage.isModified()) {
            currentCodeWithMePage.apply();
        }
        if (currentColorSchemeDebuggerPage != null && currentColorSchemeDebuggerPage.isModified()) {
            currentColorSchemeDebuggerPage.apply();
        }
        if (currentColorSchemeDiffMergePage != null && currentColorSchemeDiffMergePage.isModified()) {
            currentColorSchemeDiffMergePage.apply();
        }
        if (currentColorSchemeJvmLoggingPage != null && currentColorSchemeJvmLoggingPage.isModified()) {
            currentColorSchemeJvmLoggingPage.apply();
        }
        if (currentColorSchemeUserDefinedFileTypesPage != null && currentColorSchemeUserDefinedFileTypesPage.isModified()) {
            currentColorSchemeUserDefinedFileTypesPage.apply();
        }
        if (currentColorSchemeVcsPage != null && currentColorSchemeVcsPage.isModified()) {
            currentColorSchemeVcsPage.apply();
        }
        if (currentColorSchemeJavaPage != null && currentColorSchemeJavaPage.isModified()) {
            currentColorSchemeJavaPage.apply();
        }
        if (currentColorSchemeAngularTemplatePage != null && currentColorSchemeAngularTemplatePage.isModified()) {
            currentColorSchemeAngularTemplatePage.apply();
        }
        if (currentColorSchemeContextFreeGrammarPage != null && currentColorSchemeContextFreeGrammarPage.isModified()) {
            currentColorSchemeContextFreeGrammarPage.apply();
        }
        if (currentColorSchemeCssPage != null && currentColorSchemeCssPage.isModified()) {
            currentColorSchemeCssPage.apply();
        }
        if (currentColorSchemeDataEditorViewerPage != null && currentColorSchemeDataEditorViewerPage.isModified()) {
            currentColorSchemeDataEditorViewerPage.apply();
        }
        if (currentColorSchemeDatabasePage != null && currentColorSchemeDatabasePage.isModified()) {
            currentColorSchemeDatabasePage.apply();
        }
        if (currentColorSchemeDiagramsPage != null && currentColorSchemeDiagramsPage.isModified()) {
            currentColorSchemeDiagramsPage.apply();
        }
        if (currentColorSchemeDockerfilePage != null && currentColorSchemeDockerfilePage.isModified()) {
            currentColorSchemeDockerfilePage.apply();
        }
        if (currentColorSchemeEditorConfigPage != null && currentColorSchemeEditorConfigPage.isModified()) {
            currentColorSchemeEditorConfigPage.apply();
        }
        if (currentColorSchemeErbPage != null && currentColorSchemeErbPage.isModified()) {
            currentColorSchemeErbPage.apply();
        }
        if (currentColorSchemeFreeMarkerPage != null && currentColorSchemeFreeMarkerPage.isModified()) {
            currentColorSchemeFreeMarkerPage.apply();
        }
        if (currentColorSchemeGitLabCiExpressionPage != null && currentColorSchemeGitLabCiExpressionPage.isModified()) {
            currentColorSchemeGitLabCiExpressionPage.apply();
        }
        if (currentColorSchemeGoPage != null && currentColorSchemeGoPage.isModified()) {
            currentColorSchemeGoPage.apply();
        }
        if (currentColorSchemeGradleDeclarativePage != null && currentColorSchemeGradleDeclarativePage.isModified()) {
            currentColorSchemeGradleDeclarativePage.apply();
        }
        if (currentColorSchemeGroovyPage != null && currentColorSchemeGroovyPage.isModified()) {
            currentColorSchemeGroovyPage.apply();
        }
        if (currentColorSchemeHtmlPage != null && currentColorSchemeHtmlPage.isModified()) {
            currentColorSchemeHtmlPage.apply();
        }
        if (currentColorSchemeHttpRequestPage != null && currentColorSchemeHttpRequestPage.isModified()) {
            currentColorSchemeHttpRequestPage.apply();
        }
        if (currentColorSchemeJavaScriptPage != null && currentColorSchemeJavaScriptPage.isModified()) {
            currentColorSchemeJavaScriptPage.apply();
        }
        updateApplyButtonState();
    }

    private void updateApplyButtonState() {
        if (applyButton == null) return;
        boolean modified = (currentEditorGeneralPage != null && currentEditorGeneralPage.isModified())
                || (currentAutoImportPage != null && currentAutoImportPage.isModified())
                || (currentEditorAppearancePage != null && currentEditorAppearancePage.isModified())
                || (currentBreadcrumbsPage != null && currentBreadcrumbsPage.isModified())
                || (currentCodeCompletionPage != null && currentCodeCompletionPage.isModified())
                || (currentCodeFoldingPage != null && currentCodeFoldingPage.isModified())
                || (currentConsolePage != null && currentConsolePage.isModified())
                || (currentEditorTabsPage != null && currentEditorTabsPage.isModified())
                || (currentGutterIconsPage != null && currentGutterIconsPage.isModified())
                || (currentInlineCompletionPage != null && currentInlineCompletionPage.isModified())
                || (currentPostfixCompletionPage != null && currentPostfixCompletionPage.isModified())
                || (currentSmartKeysPage != null && currentSmartKeysPage.isModified())
                || (currentSmartKeysYamlPage != null && currentSmartKeysYamlPage.isModified())
                || (currentSmartKeysHtmlCssPage != null && currentSmartKeysHtmlCssPage.isModified())
                || (currentSmartKeysPythonPage != null && currentSmartKeysPythonPage.isModified())
                || (currentSmartKeysJsonPage != null && currentSmartKeysJsonPage.isModified())
                || (currentSmartKeysRustPage != null && currentSmartKeysRustPage.isModified())
                || (currentSmartKeysMarkdownPage != null && currentSmartKeysMarkdownPage.isModified())
                || (currentSmartKeysScalaPage != null && currentSmartKeysScalaPage.isModified())
                || (currentSmartKeysSqlPage != null && currentSmartKeysSqlPage.isModified())
                || (currentSmartKeysRubyPage != null && currentSmartKeysRubyPage.isModified())
                || (currentSmartKeysJsPage != null && currentSmartKeysJsPage.isModified())
                || (currentSmartKeysPhpPage != null && currentSmartKeysPhpPage.isModified())
                || (currentStickyLinesPage != null && currentStickyLinesPage.isModified())
                || (currentCodeEditingPage != null && currentCodeEditingPage.isModified())
                || (currentFontPage != null && currentFontPage.isModified())
                || (currentColorSchemePage != null && currentColorSchemePage.isModified())
                || (currentColorSchemeGeneralPage != null && currentColorSchemeGeneralPage.isModified())
                || (currentColorSchemeLanguageDefaultsPage != null && currentColorSchemeLanguageDefaultsPage.isModified())
                || (currentColorSchemeFontPage != null && currentColorSchemeFontPage.isModified())
                || (currentConsoleFontPage != null && currentConsoleFontPage.isModified())
                || (currentConsoleColorsPage != null && currentConsoleColorsPage.isModified())
                || (currentCodeWithMePage != null && currentCodeWithMePage.isModified())
                || (currentColorSchemeDebuggerPage != null && currentColorSchemeDebuggerPage.isModified())
                || (currentColorSchemeDiffMergePage != null && currentColorSchemeDiffMergePage.isModified())
                || (currentColorSchemeJvmLoggingPage != null && currentColorSchemeJvmLoggingPage.isModified())
                || (currentColorSchemeUserDefinedFileTypesPage != null && currentColorSchemeUserDefinedFileTypesPage.isModified())
                || (currentColorSchemeVcsPage != null && currentColorSchemeVcsPage.isModified())
                || (currentColorSchemeJavaPage != null && currentColorSchemeJavaPage.isModified())
                || (currentColorSchemeAngularTemplatePage != null && currentColorSchemeAngularTemplatePage.isModified())
                || (currentColorSchemeContextFreeGrammarPage != null && currentColorSchemeContextFreeGrammarPage.isModified())
                || (currentColorSchemeCssPage != null && currentColorSchemeCssPage.isModified())
                || (currentColorSchemeDataEditorViewerPage != null && currentColorSchemeDataEditorViewerPage.isModified())
                || (currentColorSchemeDatabasePage != null && currentColorSchemeDatabasePage.isModified())
                || (currentColorSchemeDiagramsPage != null && currentColorSchemeDiagramsPage.isModified())
                || (currentColorSchemeDockerfilePage != null && currentColorSchemeDockerfilePage.isModified())
                || (currentColorSchemeEditorConfigPage != null && currentColorSchemeEditorConfigPage.isModified())
                || (currentColorSchemeErbPage != null && currentColorSchemeErbPage.isModified())
                || (currentColorSchemeFreeMarkerPage != null && currentColorSchemeFreeMarkerPage.isModified())
                || (currentColorSchemeGitLabCiExpressionPage != null && currentColorSchemeGitLabCiExpressionPage.isModified())
                || (currentColorSchemeGoPage != null && currentColorSchemeGoPage.isModified())
                || (currentColorSchemeGradleDeclarativePage != null && currentColorSchemeGradleDeclarativePage.isModified())
                || (currentColorSchemeGroovyPage != null && currentColorSchemeGroovyPage.isModified())
                || (currentColorSchemeHtmlPage != null && currentColorSchemeHtmlPage.isModified())
                || (currentColorSchemeHttpRequestPage != null && currentColorSchemeHttpRequestPage.isModified())
                || (currentColorSchemeJavaScriptPage != null && currentColorSchemeJavaScriptPage.isModified());
        applyButton.setDisable(!modified);
        applyButton.setStyle(modified
                ? "-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-padding: 6 16 6 16; -fx-background-radius: 4; -fx-cursor: hand;"
                : "-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-text-fill: #6F737A; -fx-font-size: 12px; -fx-padding: 6 16 6 16; -fx-background-radius: 4; -fx-border-radius: 4; -fx-cursor: default;");
    }

    // --------------------------------------------------- Button Bar

    private HBox buildButtonBar() {
        Button helpBtn = new Button("?");
        helpBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #4E5157; -fx-border-radius: 12; -fx-background-radius: 12; -fx-text-fill: #848BA3; -fx-font-size: 12px; -fx-cursor: hand; -fx-min-width: 24px; -fx-min-height: 24px; -fx-max-width: 24px; -fx-max-height: 24px; -fx-padding: 0;");

        Button ok = new Button("OK");
        ok.getStyleClass().add("dialog-primary");
        ok.setDefaultButton(true);
        ok.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 6 18 6 18; -fx-background-radius: 4; -fx-cursor: hand;");
        ok.setOnAction(e -> {
            applyAll();
            stage.close();
        });

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("dialog-secondary");
        cancel.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 6 16 6 16; -fx-background-radius: 4; -fx-border-radius: 4; -fx-cursor: hand;");
        cancel.setOnAction(e -> {
            if (currentEditorGeneralPage != null) {
                currentEditorGeneralPage.reset();
            }
            if (currentAutoImportPage != null) {
                currentAutoImportPage.reset();
            }
            if (currentEditorAppearancePage != null) {
                currentEditorAppearancePage.reset();
            }
            if (currentBreadcrumbsPage != null) {
                currentBreadcrumbsPage.reset();
            }
            if (currentCodeCompletionPage != null) {
                currentCodeCompletionPage.reset();
            }
            if (currentCodeFoldingPage != null) {
                currentCodeFoldingPage.reset();
            }
            if (currentConsolePage != null) {
                currentConsolePage.reset();
            }
            if (currentEditorTabsPage != null) {
                currentEditorTabsPage.reset();
            }
            if (currentGutterIconsPage != null) {
                currentGutterIconsPage.reset();
            }
            if (currentInlineCompletionPage != null) {
                currentInlineCompletionPage.reset();
            }
            if (currentPostfixCompletionPage != null) {
                currentPostfixCompletionPage.reset();
            }
            if (currentSmartKeysPage != null) {
                currentSmartKeysPage.reset();
            }
            if (currentSmartKeysYamlPage != null) {
                currentSmartKeysYamlPage.reset();
            }
            if (currentSmartKeysHtmlCssPage != null) {
                currentSmartKeysHtmlCssPage.reset();
            }
            if (currentSmartKeysPythonPage != null) {
                currentSmartKeysPythonPage.reset();
            }
            if (currentSmartKeysJsonPage != null) {
                currentSmartKeysJsonPage.reset();
            }
            if (currentSmartKeysRustPage != null) {
                currentSmartKeysRustPage.reset();
            }
            if (currentSmartKeysMarkdownPage != null) {
                currentSmartKeysMarkdownPage.reset();
            }
            if (currentSmartKeysScalaPage != null) {
                currentSmartKeysScalaPage.reset();
            }
            if (currentSmartKeysSqlPage != null) {
                currentSmartKeysSqlPage.reset();
            }
            if (currentSmartKeysRubyPage != null) {
                currentSmartKeysRubyPage.reset();
            }
            if (currentSmartKeysJsPage != null) {
                currentSmartKeysJsPage.reset();
            }
            if (currentSmartKeysPhpPage != null) {
                currentSmartKeysPhpPage.reset();
            }
            if (currentStickyLinesPage != null) {
                currentStickyLinesPage.reset();
            }
            if (currentCodeEditingPage != null) {
                currentCodeEditingPage.reset();
            }
            if (currentFontPage != null) {
                currentFontPage.reset();
            }
            if (currentColorSchemePage != null) {
                currentColorSchemePage.reset();
            }
            if (currentColorSchemeGeneralPage != null) {
                currentColorSchemeGeneralPage.reset();
            }
            if (currentColorSchemeLanguageDefaultsPage != null) {
                currentColorSchemeLanguageDefaultsPage.reset();
            }
            if (currentColorSchemeDebuggerPage != null) {
                currentColorSchemeDebuggerPage.reset();
            }
            if (currentColorSchemeDiffMergePage != null) {
                currentColorSchemeDiffMergePage.reset();
            }
            if (currentColorSchemeJvmLoggingPage != null) {
                currentColorSchemeJvmLoggingPage.reset();
            }
            if (currentColorSchemeUserDefinedFileTypesPage != null) {
                currentColorSchemeUserDefinedFileTypesPage.reset();
            }
            if (currentColorSchemeVcsPage != null) {
                currentColorSchemeVcsPage.reset();
            }
            if (currentColorSchemeJavaPage != null) {
                currentColorSchemeJavaPage.reset();
            }
            if (currentColorSchemeAngularTemplatePage != null) {
                currentColorSchemeAngularTemplatePage.reset();
            }
            if (currentColorSchemeContextFreeGrammarPage != null) {
                currentColorSchemeContextFreeGrammarPage.reset();
            }
            if (currentColorSchemeCssPage != null) {
                currentColorSchemeCssPage.reset();
            }
            if (currentColorSchemeDataEditorViewerPage != null) {
                currentColorSchemeDataEditorViewerPage.reset();
            }
            if (currentColorSchemeDatabasePage != null) {
                currentColorSchemeDatabasePage.reset();
            }
            if (currentColorSchemeDiagramsPage != null) {
                currentColorSchemeDiagramsPage.reset();
            }
            if (currentColorSchemeDockerfilePage != null) {
                currentColorSchemeDockerfilePage.reset();
            }
            if (currentColorSchemeEditorConfigPage != null) {
                currentColorSchemeEditorConfigPage.reset();
            }
            if (currentColorSchemeErbPage != null) {
                currentColorSchemeErbPage.reset();
            }
            if (currentColorSchemeFreeMarkerPage != null) {
                currentColorSchemeFreeMarkerPage.reset();
            }
            if (currentColorSchemeGitLabCiExpressionPage != null) {
                currentColorSchemeGitLabCiExpressionPage.reset();
            }
            if (currentColorSchemeGoPage != null) {
                currentColorSchemeGoPage.reset();
            }
            if (currentColorSchemeGradleDeclarativePage != null) {
                currentColorSchemeGradleDeclarativePage.reset();
            }
            if (currentColorSchemeGroovyPage != null) {
                currentColorSchemeGroovyPage.reset();
            }
            if (currentColorSchemeHtmlPage != null) {
                currentColorSchemeHtmlPage.reset();
            }
            if (currentColorSchemeHttpRequestPage != null) {
                currentColorSchemeHttpRequestPage.reset();
            }
            if (currentColorSchemeJavaScriptPage != null) {
                currentColorSchemeJavaScriptPage.reset();
            }
            stage.close();
        });

        applyButton = new Button("Apply");
        applyButton.getStyleClass().add("dialog-secondary");
        applyButton.setOnAction(e -> applyAll());
        updateApplyButtonState();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(10, helpBtn, spacer, ok, cancel, applyButton);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 20, 12, 20));
        bar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40 transparent transparent transparent; -fx-border-width: 1 0 0 0;");
        return bar;
    }

    public SettingsColorSchemeDebuggerPage getCurrentColorSchemeDebuggerPage() {
        return currentColorSchemeDebuggerPage;
    }

    public SettingsColorSchemeDiffMergePage getCurrentColorSchemeDiffMergePage() {
        return currentColorSchemeDiffMergePage;
    }

    public SettingsColorSchemeJvmLoggingPage getCurrentColorSchemeJvmLoggingPage() {
        return currentColorSchemeJvmLoggingPage;
    }

    public SettingsColorSchemeUserDefinedFileTypesPage getCurrentColorSchemeUserDefinedFileTypesPage() {
        return currentColorSchemeUserDefinedFileTypesPage;
    }

    public SettingsColorSchemeVcsPage getCurrentColorSchemeVcsPage() {
        return currentColorSchemeVcsPage;
    }

    public SettingsColorSchemeJavaPage getCurrentColorSchemeJavaPage() {
        return currentColorSchemeJavaPage;
    }

    public SettingsColorSchemeAngularTemplatePage getCurrentColorSchemeAngularTemplatePage() {
        return currentColorSchemeAngularTemplatePage;
    }

    public SettingsColorSchemeContextFreeGrammarPage getCurrentColorSchemeContextFreeGrammarPage() {
        return currentColorSchemeContextFreeGrammarPage;
    }

    public SettingsColorSchemeCssPage getCurrentColorSchemeCssPage() {
        return currentColorSchemeCssPage;
    }

    public SettingsColorSchemeDataEditorViewerPage getCurrentColorSchemeDataEditorViewerPage() {
        return currentColorSchemeDataEditorViewerPage;
    }

    public SettingsColorSchemeDatabasePage getCurrentColorSchemeDatabasePage() {
        return currentColorSchemeDatabasePage;
    }

    public SettingsColorSchemeDiagramsPage getCurrentColorSchemeDiagramsPage() {
        return currentColorSchemeDiagramsPage;
    }

    public SettingsColorSchemeDockerfilePage getCurrentColorSchemeDockerfilePage() {
        return currentColorSchemeDockerfilePage;
    }

    public SettingsColorSchemeEditorConfigPage getCurrentColorSchemeEditorConfigPage() {
        return currentColorSchemeEditorConfigPage;
    }

    public SettingsColorSchemeErbPage getCurrentColorSchemeErbPage() {
        return currentColorSchemeErbPage;
    }

    public SettingsColorSchemeFreeMarkerPage getCurrentColorSchemeFreeMarkerPage() {
        return currentColorSchemeFreeMarkerPage;
    }

    public SettingsColorSchemeGitLabCiExpressionPage getCurrentColorSchemeGitLabCiExpressionPage() {
        return currentColorSchemeGitLabCiExpressionPage;
    }

    public SettingsColorSchemeGoPage getCurrentColorSchemeGoPage() {
        return currentColorSchemeGoPage;
    }

    public SettingsColorSchemeGradleDeclarativePage getCurrentColorSchemeGradleDeclarativePage() {
        return currentColorSchemeGradleDeclarativePage;
    }

    public SettingsColorSchemeGroovyPage getCurrentColorSchemeGroovyPage() {
        return currentColorSchemeGroovyPage;
    }

    public SettingsColorSchemeHtmlPage getCurrentColorSchemeHtmlPage() {
        return currentColorSchemeHtmlPage;
    }

    public SettingsColorSchemeHttpRequestPage getCurrentColorSchemeHttpRequestPage() {
        return currentColorSchemeHttpRequestPage;
    }

    public SettingsColorSchemeJavaScriptPage getCurrentColorSchemeJavaScriptPage() {
        return currentColorSchemeJavaScriptPage;
    }

    public Button getApplyButton() {
        return applyButton;
    }
}