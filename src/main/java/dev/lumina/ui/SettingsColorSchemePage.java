// SettingsColorSchemePage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.HashMap;
import java.util.Map;

/**
 * IntelliJ-style Editor > Color Scheme settings page.
 * Complete implementation with all sub-pages.
 */
public class SettingsColorSchemePage extends VBox {

    private final TreeView<String> schemeTree = new TreeView<>();
    private final VBox contentArea = new VBox(14);
    private final Map<String, javafx.scene.Node> pageCache = new HashMap<>();

    public SettingsColorSchemePage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(8, 0, 8, 0));
        setSpacing(14);

        // ============================================================
        // Main layout: Tree on left, content on right
        // ============================================================
        HBox mainLayout = new HBox(16);
        mainLayout.setPadding(new Insets(4, 0, 8, 0));

        // Build the scheme tree
        schemeTree.setPrefWidth(280);
        schemeTree.setPrefHeight(400);
        schemeTree.getStyleClass().add("settings-tree");
        schemeTree.setShowRoot(false);
        schemeTree.setRoot(buildSchemeTree());

        // Content area on the right
        contentArea.setPadding(new Insets(4, 0, 0, 0));
        HBox.setHgrow(contentArea, Priority.ALWAYS);

        // Show default page (General)
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

        mainLayout.getChildren().addAll(schemeTree, contentArea);

        getChildren().addAll(mainLayout);
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

        // Check if page is in cache
        if (pageCache.containsKey(pageName)) {
            contentArea.getChildren().add(pageCache.get(pageName));
            return;
        }

        javafx.scene.Node page = createPage(pageName);
        pageCache.put(pageName, page);
        contentArea.getChildren().add(page);
    }

    private javafx.scene.Node createPage(String pageName) {
        VBox page = new VBox(12);
        page.setPadding(new Insets(4, 0, 0, 0));

        Label title = new Label(pageName);
        title.getStyleClass().add("settings-section");

        // For each page, show appropriate content
        if (pageName.equals("General")) {
            page.getChildren().addAll(title, buildGeneralContent());
        } else if (pageName.equals("Language Defaults")) {
            page.getChildren().addAll(title, buildLanguageDefaultsContent());
        } else if (pageName.equals("Color Scheme Font")) {
            page.getChildren().addAll(title, buildFontContent());
        } else if (pageName.equals("Console Font")) {
            page.getChildren().addAll(title, buildConsoleFontContent());
        } else if (pageName.equals("Console Colors")) {
            page.getChildren().addAll(title, buildConsoleColorsContent());
        } else if (pageName.equals("Debugger")) {
            page.getChildren().addAll(title, buildDebuggerContent());
        } else if (pageName.equals("Diff & Merge")) {
            page.getChildren().addAll(title, buildDiffMergeContent());
        } else if (pageName.equals("VCS")) {
            page.getChildren().addAll(title, buildVCSContent());
        } else {
            // Language-specific pages - show placeholder with sample color settings
            page.getChildren().addAll(title, buildLanguageContent(pageName));
        }

        return page;
    }

    private VBox buildGeneralContent() {
        VBox box = new VBox(6);
        box.setPadding(new Insets(4, 0, 8, 20));

        // Scheme dropdown
        HBox schemeRow = new HBox(8);
        schemeRow.setAlignment(Pos.CENTER_LEFT);
        Label schemeLabel = new Label("Scheme:");
        schemeLabel.getStyleClass().add("settings-label");
        ComboBox<String> schemeCombo = new ComboBox<>();
        schemeCombo.getItems().addAll("Island's Dark Theme default", "Darcula", "IntelliJ Light");
        schemeCombo.getSelectionModel().selectFirst();
        schemeCombo.getStyleClass().add("settings-combo");
        schemeCombo.setPrefWidth(220);
        schemeRow.getChildren().addAll(schemeLabel, schemeCombo);

        // Buttons
        HBox buttonRow = new HBox(8);
        buttonRow.setAlignment(Pos.CENTER_LEFT);
        Button exportBtn = new Button("Export...");
        exportBtn.getStyleClass().add("dialog-secondary");
        Button importBtn = new Button("Import...");
        importBtn.getStyleClass().add("dialog-secondary");
        Button duplicateBtn = new Button("Duplicate...");
        duplicateBtn.getStyleClass().add("dialog-secondary");
        Button resetBtn = new Button("Reset");
        resetBtn.getStyleClass().add("dialog-secondary");
        resetBtn.setStyle("-fx-border-color: #E5534B; -fx-text-fill: #E5534B;");
        buttonRow.getChildren().addAll(exportBtn, importBtn, duplicateBtn, resetBtn);

        box.getChildren().addAll(schemeRow, buttonRow);
        return box;
    }

    private VBox buildLanguageDefaultsContent() {
        VBox box = new VBox(6);
        box.setPadding(new Insets(4, 0, 8, 20));

        Label desc = new Label("Configure default color settings for all languages.");
        desc.getStyleClass().add("settings-hint");
        desc.setWrapText(true);

        // Sample color settings
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(6);
        grid.setPadding(new Insets(8, 0, 0, 0));

        String[][] items = {
            {"Default", "#D8DBE6", "Bold", "Italic"},
            {"Keywords", "#CF8E6D", "Bold", "Normal"},
            {"Strings", "#6AAB73", "Normal", "Normal"},
            {"Comments", "#7A7E85", "Normal", "Italic"},
            {"Numbers", "#2AACB8", "Normal", "Normal"},
            {"Types", "#BCBEC4", "Normal", "Normal"},
            {"Methods", "#56A8F5", "Normal", "Normal"},
            {"Annotations", "#B3AE60", "Normal", "Normal"}
        };

        for (int i = 0; i < items.length; i++) {
            Label name = new Label(items[i][0]);
            name.getStyleClass().add("settings-label");
            Label color = new Label("■");
            color.setStyle("-fx-text-fill: " + items[i][1] + "; -fx-font-size: 16px;");
            CheckBox bold = new CheckBox("Bold");
            bold.setSelected(items[i][2].equals("Bold"));
            bold.getStyleClass().add("settings-check");
            CheckBox italic = new CheckBox("Italic");
            italic.setSelected(items[i][3].equals("Italic"));
            italic.getStyleClass().add("settings-check");
            grid.add(name, 0, i);
            grid.add(color, 1, i);
            grid.add(bold, 2, i);
            grid.add(italic, 3, i);
        }

        box.getChildren().addAll(desc, grid);
        return box;
    }

    private VBox buildFontContent() {
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
        Spinner<Integer> sizeSpinner = new Spinner<>(8, 30, 13);
        sizeSpinner.setPrefWidth(70);
        sizeSpinner.getStyleClass().add("settings-spinner");
        sizeRow.getChildren().addAll(sizeLabel, sizeSpinner);

        CheckBox ligatures = new CheckBox("Enable ligatures");
        ligatures.setSelected(false);
        ligatures.getStyleClass().add("settings-check");

        box.getChildren().addAll(fontRow, sizeRow, ligatures);
        return box;
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

        // Sample color settings for the language
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
}