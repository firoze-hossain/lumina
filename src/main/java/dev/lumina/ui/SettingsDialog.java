package dev.lumina.ui;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * IntelliJ IDEA-style Settings dialog with:
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
    private SettingsSystemPage currentSystemPage;

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
            buildColorSchemePage(pageName);
            return;
        }

        // 5. Editor and subpages
        if (underEditorGroup || "Editor".equals(pageName) || isEditorSubPage(pageName)) {
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
        SettingsKeymapPage page = new SettingsKeymapPage();
        wrapInScroll(page);
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
        SettingsQuickListsPage page = new SettingsQuickListsPage();
        wrapInScroll(page);
    }

    private void buildRequiredPluginsPage() {
        SettingsRequiredPluginsPage page = new SettingsRequiredPluginsPage();
        wrapInScroll(page);
    }

    private void buildTrustedLocationsPage() {
        SettingsTrustedLocationsPage page = new SettingsTrustedLocationsPage();
        wrapInScroll(page);
    }

    private void buildPathVariablesPage() {
        SettingsPathVariablesPage page = new SettingsPathVariablesPage();
        wrapInScroll(page);
    }

    private void buildPresentationAssistantPage() {
        SettingsPresentationAssistantPage page = new SettingsPresentationAssistantPage();
        wrapInScroll(page);
    }

    private void buildColorSchemePage(String pageName) {
        SettingsColorSchemePage page = new SettingsColorSchemePage();
        page.selectPage(pageName);
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
                "Postfix Completion", "Sticky Lines", "Smart Keys", "YAML", "HTML/CSS", "JSON",
                "Rust", "Markdown", "SQL", "JavaScript", "Code Editing", "Font", "Color Scheme",
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
                new TreeItem<>("Sticky Lines")
        );

        TreeItem<String> smartKeys = new TreeItem<>("Smart Keys");
        smartKeys.getChildren().addAll(
                new TreeItem<>("YAML"),
                new TreeItem<>("HTML/CSS"),
                new TreeItem<>("JSON"),
                new TreeItem<>("Rust"),
                new TreeItem<>("Markdown"),
                new TreeItem<>("SQL"),
                new TreeItem<>("JavaScript")
        );
        general.getChildren().add(smartKeys);

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
                new TreeItem<>("FreeMarker"),
                new TreeItem<>("GitLab CI Expression"),
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
        return tv;
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

    private void expandAncestors(TreeItem<String> item) {
        TreeItem<String> p = item.getParent();
        while (p != null) {
            p.setExpanded(true);
            p = p.getParent();
        }
    }

    private void applyAll() {
        if (currentIdeAppearancePage != null) {
            currentIdeAppearancePage.save();
        }
        if (currentSystemPage != null) {
            currentSystemPage.save();
        }
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
        cancel.setOnAction(e -> stage.close());

        Button apply = new Button("Apply");
        apply.getStyleClass().add("dialog-secondary");
        apply.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 6 16 6 16; -fx-background-radius: 4; -fx-border-radius: 4; -fx-cursor: hand;");
        apply.setOnAction(e -> applyAll());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(10, helpBtn, spacer, ok, cancel, apply);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 20, 12, 20));
        bar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40 transparent transparent transparent; -fx-border-width: 1 0 0 0;");
        return bar;
    }
}