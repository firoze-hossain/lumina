// SettingsColorSchemePage.java - COMPLETE FIXED
package dev.lumina.ui;

import java.util.HashMap;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * IntelliJ-style Editor > Color Scheme settings page.
 * Complete implementation with all sub-pages.
 */
public class SettingsColorSchemePage extends VBox {

    private final TreeView<String> schemeTree = new TreeView<>();
    private final VBox contentArea = new VBox(14);
    private final VBox quickLinksPane;
    private final Map<String, Node> pageCache = new HashMap<>();
    private final HBox mainLayout = new HBox(16);

    // Pages that should take full width (no tree or quick links)
    private static final String[] FULL_WIDTH_PAGES = {
            "General", "Language Defaults", "Color Scheme Font","Console Font"
    };

    public SettingsColorSchemePage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(8, 0, 8, 0));
        setSpacing(14);

        // ============================================================
        // Main layout: Tree on left, content on right
        // ============================================================
        mainLayout.setPadding(new Insets(4, 0, 8, 0));

        // Build the scheme tree
        schemeTree.setPrefWidth(280);
        schemeTree.setPrefHeight(400);
        schemeTree.getStyleClass().add("settings-tree");
        schemeTree.setShowRoot(false);
        schemeTree.setRoot(buildSchemeTree());

        // Quick-links pane (separate middle column)
        quickLinksPane = buildQuickLinksPane();
        quickLinksPane.setPrefWidth(280);
        quickLinksPane.setMinWidth(260);

        // Content area on the right
        contentArea.setPadding(new Insets(4, 0, 0, 0));
        HBox.setHgrow(contentArea, Priority.ALWAYS);

        // Initially show all three panels
        mainLayout.getChildren().addAll(schemeTree, quickLinksPane, contentArea);

        // Show default page (General) - this will update the layout
        showSchemePage("General");

        // Tree selection listener
        schemeTree.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null && selected.getValue() != null) {
                String pageName = selected.getValue();
                if (!pageName.equals("Color Scheme") && !pageName.equals("Language Defaults")) {
                    showSchemePage(pageName);
                }
            }
        });

        // Select General by default
        TreeItem<String> generalItem = findItem(schemeTree.getRoot(), "General");
        if (generalItem != null) {
            schemeTree.getSelectionModel().select(generalItem);
        }

        getChildren().addAll(mainLayout);
    }

    private boolean isFullWidthPage(String pageName) {
        for (String p : FULL_WIDTH_PAGES) {
            if (p.equals(pageName)) return true;
        }
        return false;
    }

    /**
     * Select a specific page inside the color-scheme tree.
     */
    public void selectPage(String pageName) {
        if (schemeTree.getRoot() == null) return;
        TreeItem<String> item = findItem(schemeTree.getRoot(), pageName);
        if (item != null) {
            schemeTree.getSelectionModel().select(item);
            showSchemePage(pageName);
        } else {
            TreeItem<String> generalItem = findItem(schemeTree.getRoot(), "General");
            if (generalItem != null) schemeTree.getSelectionModel().select(generalItem);
            showSchemePage("General");
        }
    }

    private TreeItem<String> buildSchemeTree() {
        TreeItem<String> root = new TreeItem<>("Color Scheme");
        root.setExpanded(true);

        // Main items
        TreeItem<String> general = new TreeItem<>("General");
        TreeItem<String> languageDefaults = new TreeItem<>("Language Defaults");
        TreeItem<String> colorSchemeFont = new TreeItem<>("Color Scheme Font");
        TreeItem<String> consoleFont = new TreeItem<>("Console Font");
        TreeItem<String> codeWithMe = new TreeItem<>("Code With Me");
        TreeItem<String> consoleColors = new TreeItem<>("Console Colors");
        TreeItem<String> debugger = new TreeItem<>("Debugger");
        TreeItem<String> diffMerge = new TreeItem<>("Diff & Merge");
        TreeItem<String> jvmLogging = new TreeItem<>("JVM Logging");
        TreeItem<String> userDefinedFileTypes = new TreeItem<>("User-Defined File Types");
        TreeItem<String> vcs = new TreeItem<>("VCS");

        // Language-specific items
        TreeItem<String> java = new TreeItem<>("Java");
        TreeItem<String> angularTemplate = new TreeItem<>("Angular Template");
        TreeItem<String> contextFreeGrammar = new TreeItem<>("Context Free Grammar");
        TreeItem<String> css = new TreeItem<>("CSS");
        TreeItem<String> dataEditor = new TreeItem<>("Data Editor and Viewer");
        TreeItem<String> database = new TreeItem<>("Database");
        TreeItem<String> diagrams = new TreeItem<>("Diagrams");
        TreeItem<String> dockerfile = new TreeItem<>("Dockerfile");
        TreeItem<String> editorConfig = new TreeItem<>("EditorConfig");
        TreeItem<String> freemarker = new TreeItem<>("FreeMarker");
        TreeItem<String> gitlabCI = new TreeItem<>("GitLab CI Expression");
        TreeItem<String> gradleDeclarative = new TreeItem<>("Gradle Declarative Configuration");
        TreeItem<String> groovy = new TreeItem<>("Groovy");
        TreeItem<String> html = new TreeItem<>("HTML");
        TreeItem<String> httpRequest = new TreeItem<>("HTTP Request");
        TreeItem<String> javascript = new TreeItem<>("JavaScript");
        TreeItem<String> jpaHibernate = new TreeItem<>("JPA/Hibernate QL");
        TreeItem<String> json = new TreeItem<>("JSON");
        TreeItem<String> jsonPath = new TreeItem<>("JSONPath");
        TreeItem<String> jsp = new TreeItem<>("JSP");
        TreeItem<String> jupyter = new TreeItem<>("Jupyter Notebooks");
        TreeItem<String> kotlin = new TreeItem<>("Kotlin");
        TreeItem<String> kubernetes = new TreeItem<>("Kubernetes");
        TreeItem<String> less = new TreeItem<>("Less");
        TreeItem<String> lombok = new TreeItem<>("Lombok Config");
        TreeItem<String> markdown = new TreeItem<>("Markdown");
        TreeItem<String> micronautEL = new TreeItem<>("Micronaut EL");
        TreeItem<String> mongodbJSON = new TreeItem<>("MongoDB JSON");
        TreeItem<String> postcss = new TreeItem<>("PostCSS");
        TreeItem<String> properties = new TreeItem<>("Properties");
        TreeItem<String> protocolBuffer = new TreeItem<>("Protocol Buffer");
        TreeItem<String> protocolBufferText = new TreeItem<>("Protocol Buffer Text");
        TreeItem<String> qute = new TreeItem<>("Qute");
        TreeItem<String> regexp = new TreeItem<>("RegExp");
        TreeItem<String> rust = new TreeItem<>("Rust");
        TreeItem<String> sass = new TreeItem<>("Sass/SCSS");
        TreeItem<String> shellScript = new TreeItem<>("Shell Script");
        TreeItem<String> springEL = new TreeItem<>("Spring EL");
        TreeItem<String> sql = new TreeItem<>("SQL");
        TreeItem<String> tableDiff = new TreeItem<>("Table Diff");
        TreeItem<String> toml = new TreeItem<>("TOML");
        TreeItem<String> typescript = new TreeItem<>("TypeScript");
        TreeItem<String> velocity = new TreeItem<>("Velocity");
        TreeItem<String> xml = new TreeItem<>("XML");
        TreeItem<String> xpath = new TreeItem<>("XPath");
        TreeItem<String> xslt = new TreeItem<>("XSLT");
        TreeItem<String> yaml = new TreeItem<>("YAML");
        TreeItem<String> byScope = new TreeItem<>("By Scope");
        TreeItem<String> images = new TreeItem<>("Images");

        root.getChildren().addAll(
                general,
                languageDefaults,
                colorSchemeFont,
                consoleFont,
                codeWithMe,
                consoleColors,
                debugger,
                diffMerge,
                jvmLogging,
                userDefinedFileTypes,
                vcs,
                // Language-specific
                java,
                angularTemplate,
                contextFreeGrammar,
                css,
                dataEditor,
                database,
                diagrams,
                dockerfile,
                editorConfig,
                freemarker,
                gitlabCI,
                gradleDeclarative,
                groovy,
                html,
                httpRequest,
                javascript,
                jpaHibernate,
                json,
                jsonPath,
                jsp,
                jupyter,
                kotlin,
                kubernetes,
                less,
                lombok,
                markdown,
                micronautEL,
                mongodbJSON,
                postcss,
                properties,
                protocolBuffer,
                protocolBufferText,
                qute,
                regexp,
                rust,
                sass,
                shellScript,
                springEL,
                sql,
                tableDiff,
                toml,
                typescript,
                velocity,
                xml,
                xpath,
                xslt,
                yaml,
                byScope,
                images
        );

        return root;
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

    private void showSchemePage(String pageName) {
        contentArea.getChildren().clear();

        boolean isFullWidth = isFullWidthPage(pageName);

        // Clear and rebuild the layout
        mainLayout.getChildren().clear();

        if (isFullWidth) {
            // For full-width pages, only show content
            mainLayout.getChildren().add(contentArea);
            HBox.setHgrow(contentArea, Priority.ALWAYS);
        } else {
            // For normal pages, show all three
            mainLayout.getChildren().addAll(schemeTree, quickLinksPane, contentArea);
            HBox.setHgrow(contentArea, Priority.ALWAYS);
            // Make sure tree and quick links are visible
            schemeTree.setVisible(true);
            schemeTree.setManaged(true);
            quickLinksPane.setVisible(true);
            quickLinksPane.setManaged(true);
        }

        // Check if page is in cache
        if (pageCache.containsKey(pageName)) {
            contentArea.getChildren().add(pageCache.get(pageName));
            return;
        }

        Node page = createPage(pageName);
        pageCache.put(pageName, page);
        contentArea.getChildren().add(page);
    }

    private Node createPage(String pageName) {
        VBox page = new VBox(12);
        page.setPadding(new Insets(4, 0, 0, 0));

        if (pageName.equals("General")) {
            return new SettingsColorSchemeGeneralPage();
        } else if (pageName.equals("Language Defaults")) {
            return new SettingsColorSchemeLanguageDefaultsPage();
        } else if (pageName.equals("Color Scheme Font")) {
            return new SettingsColorSchemeFontPage();
        } else if (pageName.equals("Console Font")) {
            return new SettingsConsoleFontPage();
        } else if (pageName.equals("Console Colors")) {
            page.getChildren().addAll(new Label(pageName), buildConsoleColorsContent());
        } else if (pageName.equals("Debugger")) {
            page.getChildren().addAll(new Label(pageName), buildDebuggerContent());
        } else if (pageName.equals("Diff & Merge")) {
            page.getChildren().addAll(new Label(pageName), buildDiffMergeContent());
        } else if (pageName.equals("VCS")) {
            page.getChildren().addAll(new Label(pageName), buildVCSContent());
        } else {
            Label title = new Label(pageName);
            title.getStyleClass().add("settings-section");
            page.getChildren().addAll(title, buildLanguageContent(pageName));
        }

        return page;
    }

    private VBox buildConsoleFontContent() {
        VBox box = new VBox(6);
        box.setPadding(new Insets(4, 0, 8, 20));

        HBox fontRow = new HBox(8);
        fontRow.setAlignment(Pos.CENTER_LEFT);
        Label fontLabel = new Label("Font:");
        fontLabel.getStyleClass().add("settings-label");
        ComboBox<String> fontCombo = new ComboBox<>();
        fontCombo.getItems().addAll("JetBrains Mono", "Consolas", "Menlo", "Monaco");
        fontCombo.getSelectionModel().selectFirst();
        fontCombo.getStyleClass().add("settings-combo");
        fontCombo.setPrefWidth(180);
        fontRow.getChildren().addAll(fontLabel, fontCombo);

        HBox sizeRow = new HBox(8);
        sizeRow.setAlignment(Pos.CENTER_LEFT);
        Label sizeLabel = new Label("Size:");
        sizeLabel.getStyleClass().add("settings-label");
        Spinner<Integer> sizeSpinner = new Spinner<>(8, 30, 12);
        sizeSpinner.setPrefWidth(70);
        sizeSpinner.getStyleClass().add("settings-spinner");
        sizeRow.getChildren().addAll(sizeLabel, sizeSpinner);

        box.getChildren().addAll(fontRow, sizeRow);
        return box;
    }

    private VBox buildConsoleColorsContent() {
        VBox box = new VBox(6);
        box.setPadding(new Insets(4, 0, 8, 20));

        Label desc = new Label("Configure console output colors.");
        desc.getStyleClass().add("settings-hint");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(6);
        grid.setPadding(new Insets(8, 0, 0, 0));

        String[][] items = {
                {"Standard output", "#D8DBE6"},
                {"Error output", "#E5534B"},
                {"System output", "#8FCE8F"},
                {"Debug output", "#56A8F5"},
                {"Warning output", "#D8A657"}
        };

        for (int i = 0; i < items.length; i++) {
            Label name = new Label(items[i][0]);
            name.getStyleClass().add("settings-label");
            Label color = new Label("■");
            color.setStyle("-fx-text-fill: " + items[i][1] + "; -fx-font-size: 16px;");
            Button choose = new Button("Choose...");
            choose.getStyleClass().add("dialog-secondary");
            grid.add(name, 0, i);
            grid.add(color, 1, i);
            grid.add(choose, 2, i);
        }

        box.getChildren().addAll(desc, grid);
        return box;
    }

    private VBox buildDebuggerContent() {
        VBox box = new VBox(6);
        box.setPadding(new Insets(4, 0, 8, 20));

        Label desc = new Label("Configure debugger color settings.");
        desc.getStyleClass().add("settings-hint");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(6);
        grid.setPadding(new Insets(8, 0, 0, 0));

        String[][] items = {
                {"Current execution line", "#3A3122"},
                {"Breakpoint line", "#2A1A1A"},
                {"Disabled breakpoint", "#4A3A3A"},
                {"Exception breakpoint", "#5A2A2A"}
        };

        for (int i = 0; i < items.length; i++) {
            Label name = new Label(items[i][0]);
            name.getStyleClass().add("settings-label");
            Label color = new Label("■");
            color.setStyle("-fx-text-fill: " + items[i][1] + "; -fx-font-size: 16px;");
            Button choose = new Button("Choose...");
            choose.getStyleClass().add("dialog-secondary");
            grid.add(name, 0, i);
            grid.add(color, 1, i);
            grid.add(choose, 2, i);
        }

        box.getChildren().addAll(desc, grid);
        return box;
    }

    private VBox buildDiffMergeContent() {
        VBox box = new VBox(6);
        box.setPadding(new Insets(4, 0, 8, 20));

        Label desc = new Label("Configure diff and merge color settings.");
        desc.getStyleClass().add("settings-hint");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(6);
        grid.setPadding(new Insets(8, 0, 0, 0));

        String[][] items = {
                {"Added lines", "#2A4A2A"},
                {"Modified lines", "#3A3A1A"},
                {"Deleted lines", "#4A2A2A"},
                {"Conflict lines", "#5A2A2A"}
        };

        for (int i = 0; i < items.length; i++) {
            Label name = new Label(items[i][0]);
            name.getStyleClass().add("settings-label");
            Label color = new Label("■");
            color.setStyle("-fx-text-fill: " + items[i][1] + "; -fx-font-size: 16px;");
            Button choose = new Button("Choose...");
            choose.getStyleClass().add("dialog-secondary");
            grid.add(name, 0, i);
            grid.add(color, 1, i);
            grid.add(choose, 2, i);
        }

        box.getChildren().addAll(desc, grid);
        return box;
    }

    private VBox buildVCSContent() {
        VBox box = new VBox(6);
        box.setPadding(new Insets(4, 0, 8, 20));

        Label desc = new Label("Configure VCS color settings.");
        desc.getStyleClass().add("settings-hint");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(6);
        grid.setPadding(new Insets(8, 0, 0, 0));

        String[][] items = {
                {"Added", "#2A4A2A"},
                {"Modified", "#3A3A1A"},
                {"Deleted", "#4A2A2A"},
                {"Conflicted", "#5A2A2A"},
                {"Ignored", "#2A2A2A"}
        };

        for (int i = 0; i < items.length; i++) {
            Label name = new Label(items[i][0]);
            name.getStyleClass().add("settings-label");
            Label color = new Label("■");
            color.setStyle("-fx-text-fill: " + items[i][1] + "; -fx-font-size: 16px;");
            Button choose = new Button("Choose...");
            choose.getStyleClass().add("dialog-secondary");
            grid.add(name, 0, i);
            grid.add(color, 1, i);
            grid.add(choose, 2, i);
        }

        box.getChildren().addAll(desc, grid);
        return box;
    }

    private VBox buildLanguageContent(String languageName) {
        VBox box = new VBox(6);
        box.setPadding(new Insets(4, 0, 8, 20));

        Label desc = new Label("Configure color settings for " + languageName + ".");
        desc.getStyleClass().add("settings-hint");
        desc.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(6);
        grid.setPadding(new Insets(8, 0, 0, 0));

        String[][] items = {
                {"Keywords", "#CF8E6D", "Bold"},
                {"Strings", "#6AAB73", "Normal"},
                {"Comments", "#7A7E85", "Italic"},
                {"Numbers", "#2AACB8", "Normal"},
                {"Types", "#BCBEC4", "Normal"},
                {"Methods", "#56A8F5", "Normal"},
                {"Annotations", "#B3AE60", "Normal"},
                {"Operators", "#A8AEC4", "Normal"}
        };

        for (int i = 0; i < items.length; i++) {
            Label name = new Label(items[i][0]);
            name.getStyleClass().add("settings-label");
            Label color = new Label("■");
            color.setStyle("-fx-text-fill: " + items[i][1] + "; -fx-font-size: 16px;");
            Button choose = new Button("Choose...");
            choose.getStyleClass().add("dialog-secondary");
            CheckBox bold = new CheckBox("Bold");
            bold.setSelected(items[i][2].equals("Bold"));
            bold.getStyleClass().add("settings-check");
            CheckBox italic = new CheckBox("Italic");
            italic.setSelected(items[i][2].equals("Italic"));
            italic.getStyleClass().add("settings-check");
            grid.add(name, 0, i);
            grid.add(color, 1, i);
            grid.add(choose, 2, i);
            grid.add(bold, 3, i);
            grid.add(italic, 4, i);
        }

        box.getChildren().addAll(desc, grid);
        return box;
    }

    /**
     * Build the right-side quick links list that mirrors the many options shown in the screenshots.
     */
    private VBox buildQuickLinksPane() {
        VBox box = new VBox(6);
        box.setPadding(new Insets(0, 0, 8, 0));

        Label header = new Label("Configure colors and the font for source code and console output:");
        header.getStyleClass().add("settings-hint");
        header.setWrapText(true);
        header.setMaxWidth(260);

        VBox links = new VBox(10);
        links.setFillWidth(true);
        links.setPrefWidth(260);
        links.setMaxWidth(Double.MAX_VALUE);

        if (schemeTree.getRoot() != null) {
            for (TreeItem<String> item : schemeTree.getRoot().getChildren()) {
                if (item.getValue() == null) continue;
                Hyperlink link = new Hyperlink(item.getValue());
                link.getStyleClass().add("settings-link");
                link.setWrapText(true);
                link.setMaxWidth(Double.MAX_VALUE);
                link.setPrefWidth(240);
                link.setOnAction(evt -> {
                    TreeItem<String> found = findItem(schemeTree.getRoot(), item.getValue());
                    if (found != null) {
                        schemeTree.getSelectionModel().select(found);
                        schemeTree.scrollTo(schemeTree.getRow(found));
                    }
                });
                links.getChildren().add(link);
            }
        }

        ScrollPane scroller = new ScrollPane(links);
        scroller.setFitToWidth(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setPrefViewportHeight(360);
        scroller.setMaxWidth(Double.MAX_VALUE);
        scroller.getStyleClass().add("settings-quicklinks-scroll");

        BorderStroke stroke = new BorderStroke(javafx.scene.paint.Color.web("#2A2A2A"), BorderStrokeStyle.SOLID, new CornerRadii(4), new BorderWidths(1));
        box.setBorder(new Border(stroke));
        box.setPadding(new Insets(8));
        box.getChildren().addAll(header, scroller);
        return box;
    }
}