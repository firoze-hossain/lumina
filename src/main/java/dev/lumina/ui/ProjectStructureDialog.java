package dev.lumina.ui;

import dev.lumina.project.JdkMetadata;
import dev.lumina.project.ProjectSdk;
import dev.lumina.project.ProjectStructureModel;
import dev.lumina.project.ProjectStructureModel.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Dynamic IntelliJ IDEA-identical Project Structure dialog supporting
 * Java, Spring Boot, Python, PHP, Kotlin, Web, and multi-SDK projects.
 */
public class ProjectStructureDialog {

    private final Stage stage;
    private final ProjectStructureModel model;
    private final Path projectRoot;
    private final TreeView<String> categoryTree;
    private final StackPane contentContainer = new StackPane();
    private final List<String> navHistory = new ArrayList<>();
    private int navIndex = -1;
    private boolean updatingNav = false;

    public ProjectStructureDialog(Stage owner, String projectName, Path projectRoot) {
        this.stage = new Stage();
        this.projectRoot = projectRoot != null ? projectRoot : Path.of(".");
        this.model = ProjectStructureModel.Service.load(this.projectRoot);
        if (projectName != null && !projectName.isBlank()) {
            model.setProjectName(projectName);
        }

        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.DECORATED);
        stage.setTitle("Project Structure");

        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("app-root", "project-structure-dialog");

        // ---- Left: category navigation bar + tree ----
        VBox leftPanel = new VBox();
        leftPanel.setPrefWidth(240);
        leftPanel.setMinWidth(220);
        leftPanel.getStyleClass().add("ps-sidebar");

        HBox navBar = buildNavBar();
        this.categoryTree = buildCategoryTree();
        VBox.setVgrow(categoryTree, Priority.ALWAYS);
        leftPanel.getChildren().addAll(navBar, categoryTree);

        // ---- Center: dynamic content area ----
        contentContainer.getStyleClass().add("ps-page");
        contentContainer.setPadding(new Insets(16, 20, 16, 20));

        // ---- Bottom: dialog button bar ----
        HBox buttonBar = buildButtonBar();

        root.setLeft(leftPanel);
        root.setCenter(contentContainer);
        root.setBottom(buttonBar);

        // Selection listener
        categoryTree.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null && selected.isLeaf() && selected.getParent() != null) {
                String page = selected.getValue();
                if (!updatingNav) {
                    if (navIndex < navHistory.size() - 1) {
                        navHistory.subList(navIndex + 1, navHistory.size()).clear();
                    }
                    navHistory.add(page);
                    navIndex = navHistory.size() - 1;
                }
                showPage(page);
            }
        });

        // Initial selection: Project
        navigateTo("Project");

        Scene scene = new Scene(root, 980, 680);
        scene.getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
        stage.setScene(scene);
    }

    public void show() {
        stage.showAndWait();
    }

    public void navigateTo(String pageName) {
        TreeItem<String> item = findItem(categoryTree.getRoot(), pageName);
        if (item != null) {
            categoryTree.getSelectionModel().select(item);
        }
    }

    // --------------------------------------------------- Navigation bar

    private HBox buildNavBar() {
        Button backBtn = new Button("←");
        backBtn.getStyleClass().add("console-button");
        backBtn.setTooltip(new Tooltip("Back"));
        backBtn.setOnAction(e -> {
            if (navIndex > 0) {
                navIndex--;
                updatingNav = true;
                navigateTo(navHistory.get(navIndex));
                updatingNav = false;
            }
        });

        Button forwardBtn = new Button("→");
        forwardBtn.getStyleClass().add("console-button");
        forwardBtn.setTooltip(new Tooltip("Forward"));
        forwardBtn.setOnAction(e -> {
            if (navIndex < navHistory.size() - 1) {
                navIndex++;
                updatingNav = true;
                navigateTo(navHistory.get(navIndex));
                updatingNav = false;
            }
        });

        HBox bar = new HBox(6, backBtn, forwardBtn);
        bar.setPadding(new Insets(10, 14, 6, 14));
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    // --------------------------------------------------- Category tree

    private TreeView<String> buildCategoryTree() {
        TreeItem<String> root = new TreeItem<>("Root");
        root.setExpanded(true);

        TreeItem<String> projectSettings = new TreeItem<>("Project Settings");
        projectSettings.setExpanded(true);
        projectSettings.getChildren().addAll(
                new TreeItem<>("Project"),
                new TreeItem<>("Modules"),
                new TreeItem<>("Libraries"),
                new TreeItem<>("Facets"),
                new TreeItem<>("Artifacts")
        );

        TreeItem<String> platformSettings = new TreeItem<>("Platform Settings");
        platformSettings.setExpanded(true);
        platformSettings.getChildren().addAll(
                new TreeItem<>("SDKs"),
                new TreeItem<>("Global Libraries")
        );

        TreeItem<String> problems = new TreeItem<>("Problems");

        root.getChildren().addAll(projectSettings, platformSettings, problems);

        TreeView<String> tree = new TreeView<>(root);
        tree.setShowRoot(false);
        tree.getStyleClass().add("ps-tree");
        tree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle(null);
                    return;
                }
                setText(item);
                if ("Project Settings".equals(item) || "Platform Settings".equals(item)) {
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #848BA3; -fx-font-size: 11px;");
                    setDisable(true);
                } else {
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                    setDisable(false);
                }
            }
        });
        return tree;
    }

    private TreeItem<String> findItem(TreeItem<String> current, String text) {
        if (current == null) return null;
        if (text.equals(current.getValue())) return current;
        for (TreeItem<String> child : current.getChildren()) {
            TreeItem<String> f = findItem(child, text);
            if (f != null) return f;
        }
        return null;
    }

    // --------------------------------------------------- Bottom button bar

    private HBox buildButtonBar() {
        Button helpBtn = new Button("?");
        helpBtn.getStyleClass().add("console-button");
        helpBtn.setTooltip(new Tooltip("Project Structure Help"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("dialog-secondary");
        cancel.setOnAction(e -> stage.close());

        Button apply = new Button("Apply");
        apply.getStyleClass().add("dialog-secondary");
        apply.setOnAction(e -> ProjectStructureModel.Service.save(model));

        Button ok = new Button("OK");
        ok.getStyleClass().add("dialog-primary");
        ok.setDefaultButton(true);
        ok.setOnAction(e -> {
            ProjectStructureModel.Service.save(model);
            stage.close();
        });

        HBox bar = new HBox(10, helpBtn, spacer, cancel, apply, ok);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPadding(new Insets(10, 20, 14, 20));
        bar.getStyleClass().add("dialog-footer");
        return bar;
    }

    // --------------------------------------------------- Page Dispatcher

    private void showPage(String pageName) {
        contentContainer.getChildren().clear();
        Node pageNode = switch (pageName) {
            case "Project" -> buildProjectPage();
            case "Modules" -> buildModulesPage();
            case "Libraries" -> buildLibrariesPage();
            case "Facets" -> buildFacetsPage();
            case "Artifacts" -> buildArtifactsPage();
            case "SDKs" -> buildSdksPage();
            case "Global Libraries" -> buildGlobalLibrariesPage();
            case "Problems" -> buildProblemsPage();
            default -> new Label("Settings for " + pageName);
        };
        contentContainer.getChildren().add(wrapInScroll(pageNode));
    }

    // --------------------------------------------------- 1. PROJECT PAGE

    private Node buildProjectPage() {
        VBox box = new VBox(14);

        Label title = new Label("Project");
        title.getStyleClass().add("ps-page-title");

        Label subtitle = new Label("Default settings for all modules. Configure these parameters for each module on the module page as needed.");
        subtitle.getStyleClass().add("ps-subtitle");
        subtitle.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);
        grid.setPadding(new Insets(8, 0, 8, 0));

        ColumnConstraints c0 = new ColumnConstraints(120);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c0, c1);

        int row = 0;

        // 1. Name
        grid.add(formLabel("Name:"), 0, row);
        TextField nameField = new TextField(model.getProjectName());
        nameField.setPrefWidth(350);
        nameField.textProperty().addListener((obs, old, v) -> model.setProjectName(v.trim()));
        grid.add(nameField, 1, row++);

        // 2. SDK
        grid.add(formLabel("SDK:"), 0, row);
        ComboBox<Object> sdkCombo = buildSdkComboBox();
        Button editSdkBtn = new Button("Edit");
        editSdkBtn.getStyleClass().add("dialog-secondary");
        editSdkBtn.setOnAction(e -> navigateTo("SDKs"));
        HBox sdkRow = new HBox(8, sdkCombo, editSdkBtn);
        sdkRow.setAlignment(Pos.CENTER_LEFT);
        grid.add(sdkRow, 1, row++);

        // 3. Language level
        grid.add(formLabel("Language level:"), 0, row);
        ComboBox<String> langCombo = buildLanguageLevelComboBox();
        langCombo.setPrefWidth(380);
        langCombo.valueProperty().addListener((obs, old, v) -> {
            if (v != null && !v.startsWith("---")) model.setLanguageLevel(v);
        });
        grid.add(langCombo, 1, row++);

        // 4. Compiler output
        grid.add(formLabel("Compiler output:"), 0, row);
        TextField outputField = new TextField(model.getCompilerOutput());
        outputField.setPrefWidth(380);
        outputField.textProperty().addListener((obs, old, v) -> model.setCompilerOutput(v.trim()));
        Button browseOutBtn = new Button("📁");
        browseOutBtn.getStyleClass().add("console-button");
        browseOutBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Compiler Output Path");
            if (projectRoot != null && Files.isDirectory(projectRoot)) dc.setInitialDirectory(projectRoot.toFile());
            File chosen = dc.showDialog(stage);
            if (chosen != null) {
                outputField.setText(chosen.getAbsolutePath());
            }
        });
        HBox outRow = new HBox(8, outputField, browseOutBtn);
        outRow.setAlignment(Pos.CENTER_LEFT);

        Label outputHint = new Label("Used for module subdirectories, Production and Test directories for the corresponding sources.");
        outputHint.getStyleClass().add("ps-hint");
        VBox outBox = new VBox(4, outRow, outputHint);
        grid.add(outBox, 1, row++);

        box.getChildren().addAll(title, subtitle, grid);
        return box;
    }

    private ComboBox<Object> buildSdkComboBox() {
        ComboBox<Object> combo = new ComboBox<>();
        combo.setPrefWidth(380);
        refreshSdkComboBoxItems(combo);
        selectCurrentSdk(combo);

        // Dropdown Popup List Cell Factory matching media_1789873732873.png
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    return;
                }
                if (item instanceof String s) {
                    if ("Detected SDKs".equals(s)) {
                        setText("Detected SDKs");
                        setGraphic(null);
                        setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 6 8 2 8;");
                        setDisable(true);
                    } else if (s.startsWith("---")) {
                        setText(s.replace("---", "").trim());
                        setGraphic(null);
                        setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 6 8 2 8;");
                        setDisable(true);
                    } else if ("+ Add SDK".equals(s)) {
                        setText(null);
                        HBox addRow = new HBox(8);
                        addRow.setAlignment(Pos.CENTER_LEFT);
                        Label addLbl = new Label("+ Add SDK");
                        addLbl.setStyle("-fx-text-fill: #548AF7; -fx-font-weight: bold; -fx-font-size: 12px;");
                        Region spacer = new Region();
                        HBox.setHgrow(spacer, Priority.ALWAYS);
                        Label arrow = new Label("›");
                        arrow.setStyle("-fx-text-fill: #848BA3; -fx-font-weight: bold; -fx-font-size: 14px;");
                        addRow.getChildren().addAll(addLbl, spacer, arrow);
                        setGraphic(addRow);
                        setStyle("-fx-padding: 4 8 4 8;");
                        setDisable(false);
                    } else if ("<No SDK>".equals(s)) {
                        setText("<No SDK>");
                        Label globe = new Label("🌐");
                        globe.setStyle("-fx-text-fill: #ED5565; -fx-font-size: 13px;");
                        setGraphic(globe);
                        setStyle("-fx-text-fill: #ED5565; -fx-padding: 4 8 4 8;");
                        setDisable(false);
                    } else {
                        setText(s);
                        setGraphic(null);
                        setStyle("-fx-text-fill: #DFE1E5; -fx-padding: 4 8 4 8;");
                        setDisable(false);
                    }
                } else if (item instanceof ProjectSdk.SdkItem sdk) {
                    setText(null);
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.CENTER_LEFT);

                    Label icon;
                    if (sdk.isRegistered()) {
                        icon = new Label("📁");
                        icon.setStyle("-fx-text-fill: #548AF7; -fx-font-size: 13px;");
                    } else if (sdk.type() == ProjectSdk.SdkType.JDK) {
                        icon = new Label("🔗");
                        icon.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 13px;");
                    } else {
                        icon = new Label(sdk.type().iconGlyph());
                        icon.setStyle("-fx-font-size: 13px;");
                    }

                    Label nameLbl = new Label(sdk.name());
                    nameLbl.setStyle("-fx-text-fill: #EDEFF6; -fx-font-size: 12px;");
                    row.getChildren().addAll(icon, nameLbl);

                    // Show path next to name for detected SDKs matching IntelliJ IDEA
                    if (!sdk.isRegistered() && sdk.homePath() != null && !sdk.homePath().isBlank()) {
                        Label pathLbl = new Label(" " + sdk.homePath());
                        pathLbl.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");
                        row.getChildren().add(pathLbl);
                    }

                    setGraphic(row);
                    setStyle("-fx-padding: 4 8 4 8;");
                    setDisable(false);
                }
            }
        });

        // Button Cell (Closed display)
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                if (item instanceof ProjectSdk.SdkItem sdk) {
                    setText(sdk.getShortDisplay());
                    Label icon = new Label(sdk.isRegistered() ? "📁" : (sdk.type() == ProjectSdk.SdkType.JDK ? "📁" : sdk.type().iconGlyph()));
                    icon.setStyle("-fx-text-fill: #548AF7; -fx-font-size: 13px;");
                    setGraphic(icon);
                    setStyle("-fx-text-fill: #EDEFF6;");
                } else if ("<No SDK>".equals(item)) {
                    setText("<No SDK>");
                    Label globe = new Label("🌐");
                    globe.setStyle("-fx-text-fill: #ED5565; -fx-font-size: 13px;");
                    setGraphic(globe);
                    setStyle("-fx-text-fill: #ED5565;");
                } else {
                    setText(item.toString());
                    setGraphic(null);
                    setStyle("-fx-text-fill: #DFE1E5;");
                }
            }
        });

        combo.setOnAction(e -> {
            Object selected = combo.getSelectionModel().getSelectedItem();
            if (selected instanceof ProjectSdk.SdkItem sdk) {
                model.setProjectSdk(sdk);
            } else if ("<No SDK>".equals(selected)) {
                model.setProjectSdk(null);
            } else if ("+ Add SDK".equals(selected) || (selected instanceof String s && s.contains("Add SDK"))) {
                selectCurrentSdk(combo);
                showAddSdkMenu(combo, () -> {
                    refreshSdkComboBoxItems(combo);
                    selectCurrentSdk(combo);
                });
            }
        });

        return combo;
    }

    private void refreshSdkComboBoxItems(ComboBox<Object> combo) {
        List<ProjectSdk.SdkItem> all = ProjectSdk.discoverAllSdks();
        ObservableList<Object> items = FXCollections.observableArrayList();
        items.add("<No SDK>");

        List<ProjectSdk.SdkItem> registered = all.stream().filter(ProjectSdk.SdkItem::isRegistered).toList();
        items.addAll(registered);

        items.add("+ Add SDK");

        List<ProjectSdk.SdkItem> detected = all.stream().filter(s -> !s.isRegistered()).toList();
        if (!detected.isEmpty()) {
            items.add("Detected SDKs");
            items.addAll(detected);
        }

        combo.setItems(items);
    }

    private void selectCurrentSdk(ComboBox<Object> combo) {
        if (model.getProjectSdk() != null) {
            String currHome = model.getProjectSdk().homePath();
            String currName = model.getProjectSdk().name();
            Object match = null;
            for (Object obj : combo.getItems()) {
                if (obj instanceof ProjectSdk.SdkItem s) {
                    if (currHome != null && !currHome.isBlank() && currHome.equals(s.homePath())) {
                        match = s;
                        break;
                    }
                    if (currName != null && currName.equals(s.name())) {
                        match = s;
                        break;
                    }
                }
            }
            if (match != null) {
                combo.getSelectionModel().select(match);
            } else {
                combo.getSelectionModel().selectFirst();
            }
        } else {
            combo.getSelectionModel().select("<No SDK>");
        }
    }

    private void showAddSdkMenu(Node anchor, Runnable onSdkAdded) {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("ps-add-sdk-menu");

        // 1. Download JDK... matching media_1789873732873.png
        MenuItem downloadJdk = new MenuItem("Download JDK…");
        Label dlIcon = new Label("📥");
        dlIcon.setStyle("-fx-font-size: 13px;");
        downloadJdk.setGraphic(dlIcon);
        downloadJdk.setOnAction(e -> {
            int initVer = 25;
            if (model.getProjectSdk() != null && model.getProjectSdk().type() == ProjectSdk.SdkType.JDK) {
                try {
                    initVer = Integer.parseInt(model.getProjectSdk().version().split("\\.")[0]);
                } catch (Exception ignored) {}
            }
            DownloadJdkDialog dialog = new DownloadJdkDialog(stage, initVer, installed -> {
                ProjectSdk.SdkItem newItem = ProjectSdk.registerFromInstallation(installed);
                model.setProjectSdk(newItem);
                if (onSdkAdded != null) onSdkAdded.run();
                showPage("Project");
            });
            dialog.show();
        });

        // 2. JDK... matching media_1789873732873.png
        MenuItem jdkDisk = new MenuItem("JDK…");
        Label jdkIcon = new Label("📁");
        jdkIcon.setStyle("-fx-text-fill: #548AF7; -fx-font-size: 13px;");
        jdkDisk.setGraphic(jdkIcon);
        jdkDisk.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select JDK Home Directory");
            File f = dc.showDialog(stage);
            if (f != null) {
                ProjectSdk.SdkItem newItem = ProjectSdk.registerSdk(
                        f.getName(), ProjectSdk.SdkType.JDK, f.getAbsolutePath(), "Custom");
                model.setProjectSdk(newItem);
                if (onSdkAdded != null) onSdkAdded.run();
                showPage("Project");
            }
        });

        // 3. Python SDK... matching media_1789873732873.png
        MenuItem pyDisk = new MenuItem("Python SDK…");
        Label pyIcon = new Label("🐍");
        pyIcon.setStyle("-fx-font-size: 13px;");
        pyDisk.setGraphic(pyIcon);
        pyDisk.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Python Executable");
            File f = fc.showOpenDialog(stage);
            if (f != null) {
                ProjectSdk.SdkItem newItem = ProjectSdk.registerSdk(
                        "Python (" + f.getName() + ")", ProjectSdk.SdkType.PYTHON, f.getAbsolutePath(), "Custom");
                model.setProjectSdk(newItem);
                if (onSdkAdded != null) onSdkAdded.run();
                showPage("Project");
            }
        });

        // 4. Ruby Interpreter... matching media_1789873732873.png
        MenuItem rubyDisk = new MenuItem("Ruby Interpreter…");
        Label rubyIcon = new Label("💎");
        rubyIcon.setStyle("-fx-font-size: 13px;");
        rubyDisk.setGraphic(rubyIcon);
        rubyDisk.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Ruby Executable");
            File f = fc.showOpenDialog(stage);
            if (f != null) {
                ProjectSdk.SdkItem newItem = ProjectSdk.registerSdk(
                        "Ruby (" + f.getName() + ")", ProjectSdk.SdkType.RUBY, f.getAbsolutePath(), "Custom");
                model.setProjectSdk(newItem);
                if (onSdkAdded != null) onSdkAdded.run();
                showPage("Project");
            }
        });

        // 5. JRuby Interpreter... matching media_1789873732873.png
        MenuItem jrubyDisk = new MenuItem("JRuby Interpreter…");
        Label jrubyIcon = new Label("🐦");
        jrubyIcon.setStyle("-fx-font-size: 13px;");
        jrubyDisk.setGraphic(jrubyIcon);
        jrubyDisk.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select JRuby Executable");
            File f = fc.showOpenDialog(stage);
            if (f != null) {
                ProjectSdk.SdkItem newItem = ProjectSdk.registerSdk(
                        "JRuby (" + f.getName() + ")", ProjectSdk.SdkType.JRUBY, f.getAbsolutePath(), "Custom");
                model.setProjectSdk(newItem);
                if (onSdkAdded != null) onSdkAdded.run();
                showPage("Project");
            }
        });

        // 6. PHP Interpreter... matching media_1789873732873.png
        MenuItem phpDisk = new MenuItem("PHP Interpreter…");
        Label phpIcon = new Label("🐘");
        phpIcon.setStyle("-fx-font-size: 13px;");
        phpDisk.setGraphic(phpIcon);
        phpDisk.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select PHP Executable");
            File f = fc.showOpenDialog(stage);
            if (f != null) {
                ProjectSdk.SdkItem newItem = ProjectSdk.registerSdk(
                        "PHP (" + f.getName() + ")", ProjectSdk.SdkType.PHP, f.getAbsolutePath(), "Custom");
                model.setProjectSdk(newItem);
                if (onSdkAdded != null) onSdkAdded.run();
                showPage("Project");
            }
        });

        menu.getItems().addAll(downloadJdk, jdkDisk, pyDisk, rubyDisk, jrubyDisk, phpDisk);
        menu.show(anchor, javafx.geometry.Side.RIGHT, 0, 0);
    }

    private ComboBox<String> buildLanguageLevelComboBox() {
        ComboBox<String> combo = new ComboBox<>();
        ModuleModel primary = model.getPrimaryModule();
        boolean isPython = "PYTHON_MODULE".equals(primary.getType());
        boolean isPhp = "PHP_MODULE".equals(primary.getType());

        ObservableList<String> items = FXCollections.observableArrayList();
        items.add("SDK default");
        items.add("--- Supported Versions ---");

        if (isPython) {
            items.addAll("Python 3.14", "Python 3.13", "Python 3.12", "Python 3.11", "Python 3.10", "Python 3.9", "Python 3.8");
        } else if (isPhp) {
            items.addAll("8.4", "8.3", "8.2", "8.1", "8.0", "7.4");
        } else {
            // Java Language Levels matching IntelliJ IDEA media_1789872977063.png
            items.addAll(
                    "25 - Compact source files, module imports",
                    "25 (Preview) - Primitive Types in Patterns, etc.",
                    "24 - Stream gatherers",
                    "24 (Preview) - Flexible constructor bodies, simple source files, etc.",
                    "23 - Markdown documentation comments",
                    "23 (Preview) - Primitive types in patterns, implicitly declared classes, etc.",
                    "22 - Unnamed variables and patterns",
                    "22 (Preview) - Statements before super(), string templates (2nd preview), etc.",
                    "21 - Record patterns, pattern matching for switch",
                    "21 (Preview) - String templates, unnamed classes and instance main methods, etc.",
                    "17 - Sealed types, always-strict floating-point semantics",
                    "11 - Local variable syntax for lambda parameters",
                    "8 - Lambdas, type annotations etc."
            );
        }

        combo.setItems(items);

        String cur = model.getLanguageLevel();
        if (cur != null && !cur.isBlank()) {
            for (String it : items) {
                if (it.startsWith(cur) || it.contains(cur)) {
                    combo.getSelectionModel().select(it);
                    break;
                }
            }
        }
        if (combo.getSelectionModel().isEmpty()) {
            combo.getSelectionModel().selectFirst();
        }

        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item);
                if (item.startsWith("---")) {
                    setText(item.replace("---", "").trim());
                    setStyle("-fx-text-fill: #848BA3; -fx-font-weight: bold; -fx-font-size: 11px;");
                    setDisable(true);
                } else {
                    setStyle("-fx-text-fill: #DFE1E5;");
                    setDisable(false);
                }
            }
        });

        return combo;
    }

    // --------------------------------------------------- 2. MODULES PAGE

    private Node buildModulesPage() {
        VBox outer = new VBox(12);

        // Modules toolbar & list + details split
        SplitPane split = new SplitPane();
        split.setDividerPositions(0.25);
        VBox.setVgrow(split, Priority.ALWAYS);

        // --- Left: Modules list ---
        VBox leftModuleBox = new VBox(6);
        leftModuleBox.setPadding(new Insets(4));
        HBox modToolbar = new HBox(6);
        Button addModBtn = new Button("+");
        addModBtn.getStyleClass().add("ps-add-button");
        Button remModBtn = new Button("-");
        remModBtn.getStyleClass().add("ps-add-button");
        Button copyModBtn = new Button("📋");
        copyModBtn.getStyleClass().add("ps-add-button");
        modToolbar.getChildren().addAll(addModBtn, remModBtn, copyModBtn);

        ListView<ModuleModel> moduleList = new ListView<>();
        moduleList.setItems(FXCollections.observableArrayList(model.getModules()));
        moduleList.getSelectionModel().selectFirst();
        VBox.setVgrow(moduleList, Priority.ALWAYS);
        moduleList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ModuleModel m, boolean empty) {
                super.updateItem(m, empty);
                if (empty || m == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(m.getName());
                Label icon = new Label("▣");
                icon.setStyle("-fx-text-fill: #548AF7;");
                setGraphic(icon);
            }
        });
        leftModuleBox.getChildren().addAll(modToolbar, moduleList);

        // --- Right: Module details tab pane ---
        VBox rightDetails = new VBox(10);
        rightDetails.setPadding(new Insets(0, 0, 0, 12));

        ModuleModel selectedModule = moduleList.getSelectionModel().getSelectedItem() != null
                ? moduleList.getSelectionModel().getSelectedItem()
                : model.getPrimaryModule();

        HBox nameRow = new HBox(12);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLbl = formLabel("Name:");
        TextField moduleNameField = new TextField(selectedModule.getName());
        moduleNameField.setPrefWidth(300);
        moduleNameField.textProperty().addListener((obs, old, v) -> {
            selectedModule.setName(v.trim());
            moduleList.refresh();
        });
        nameRow.getChildren().addAll(nameLbl, moduleNameField);

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("settings-tab-pane");
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Tabs: Sources, Paths, Dependencies
        Tab sourcesTab = new Tab("Sources", buildModuleSourcesTab(selectedModule));
        sourcesTab.setClosable(false);

        Tab pathsTab = new Tab("Paths", buildModulePathsTab(selectedModule));
        pathsTab.setClosable(false);

        Tab depsTab = new Tab("Dependencies", buildModuleDependenciesTab(selectedModule));
        depsTab.setClosable(false);

        tabPane.getTabs().addAll(sourcesTab, pathsTab, depsTab);
        rightDetails.getChildren().addAll(nameRow, tabPane);

        split.getItems().addAll(leftModuleBox, rightDetails);
        outer.getChildren().add(split);
        return outer;
    }

    private Node buildModuleSourcesTab(ModuleModel module) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10, 0, 0, 0));

        // 1. Language level
        HBox langRow = new HBox(8);
        langRow.setAlignment(Pos.CENTER_LEFT);
        Label langLbl = formLabel("Language level:");
        ComboBox<String> langCombo = buildLanguageLevelComboBox();
        langCombo.valueProperty().addListener((obs, old, v) -> {
            if (v != null && !v.startsWith("---")) module.setLanguageLevel(v);
        });
        langRow.getChildren().addAll(langLbl, langCombo);

        // 2. Mark as toolbar matching media_1789872972878.png
        HBox markBar = new HBox(8);
        markBar.setAlignment(Pos.CENTER_LEFT);
        Label markLbl = formLabel("Mark as:");

        Button btnSources = createMarkButton("📁 Sources", "#548AF7");
        Button btnTests = createMarkButton("📁 Tests", "#59A869");
        Button btnResources = createMarkButton("📁 Resources", "#9876AA");
        Button btnTestResources = createMarkButton("📁 Test Resources", "#BBB529");
        Button btnExcluded = createMarkButton("📁 Excluded", "#ED5565");

        markBar.getChildren().addAll(markLbl, btnSources, btnTests, btnResources, btnTestResources, btnExcluded);

        // 3. Two-column split: File Tree on left, Content Roots on right
        SplitPane sourceSplit = new SplitPane();
        sourceSplit.setDividerPositions(0.45);
        VBox.setVgrow(sourceSplit, Priority.ALWAYS);

        // File tree
        TreeView<FileItem> fileTree = buildProjectDirectoryTree(module);
        VBox.setVgrow(fileTree, Priority.ALWAYS);

        // Content root list on right
        VBox contentRootsPanel = buildContentRootsList(module, fileTree);
        VBox.setVgrow(contentRootsPanel, Priority.ALWAYS);

        // Wire mark buttons to currently selected tree item
        btnSources.setOnAction(e -> markSelectedTreeItem(fileTree, module, FolderType.SOURCE, contentRootsPanel));
        btnTests.setOnAction(e -> markSelectedTreeItem(fileTree, module, FolderType.TEST_SOURCE, contentRootsPanel));
        btnResources.setOnAction(e -> markSelectedTreeItem(fileTree, module, FolderType.RESOURCE, contentRootsPanel));
        btnTestResources.setOnAction(e -> markSelectedTreeItem(fileTree, module, FolderType.TEST_RESOURCE, contentRootsPanel));
        btnExcluded.setOnAction(e -> markSelectedTreeItem(fileTree, module, FolderType.EXCLUDED, contentRootsPanel));

        sourceSplit.getItems().addAll(fileTree, contentRootsPanel);

        // 4. Exclude files
        HBox exclRow = new HBox(8);
        exclRow.setAlignment(Pos.CENTER_LEFT);
        Label exclLbl = formLabel("Exclude files:");
        TextField exclField = new TextField(module.getExcludePatterns());
        exclField.setPromptText("Use ; to separate name patterns, * for any number of symbols, ? for one.");
        exclField.textProperty().addListener((obs, old, v) -> module.setExcludePatterns(v.trim()));
        HBox.setHgrow(exclField, Priority.ALWAYS);
        exclRow.getChildren().addAll(exclLbl, exclField);

        box.getChildren().addAll(langRow, markBar, sourceSplit, exclRow);
        return box;
    }

    private Button createMarkButton(String text, String colorHex) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + colorHex + "; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 4 8;");
        return btn;
    }

    private static record FileItem(String name, String relativePath, boolean isDir) {
        @Override
        public String toString() { return name; }
    }

    private TreeView<FileItem> buildProjectDirectoryTree(ModuleModel module) {
        Path root = module.getContentRoot() != null ? module.getContentRoot() : projectRoot;
        TreeItem<FileItem> rootItem = new TreeItem<>(new FileItem(root.getFileName().toString(), "", true));
        rootItem.setExpanded(true);
        populateFileTree(rootItem, root, "");

        TreeView<FileItem> tree = new TreeView<>(rootItem);
        tree.getStyleClass().add("ps-file-tree");
        tree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(FileItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle(null);
                    return;
                }
                setText(item.name());
                FolderType ft = module.getFolderType(item.relativePath());
                if (ft != null) {
                    Label glyph = new Label(ft.glyph());
                    glyph.setStyle("-fx-text-fill: " + ft.hexColor() + ";");
                    setGraphic(glyph);
                    setStyle("-fx-text-fill: " + ft.hexColor() + ";");
                } else {
                    Label glyph = new Label(item.isDir() ? "📁" : "📄");
                    glyph.setStyle("-fx-text-fill: #848BA3;");
                    setGraphic(glyph);
                    setStyle("-fx-text-fill: #B9BECF;");
                }
            }
        });
        return tree;
    }

    private void populateFileTree(TreeItem<FileItem> parentItem, Path currentDir, String relPrefix) {
        try (var s = Files.list(currentDir)) {
            s.sorted(Comparator.comparing(p -> !Files.isDirectory(p)))
             .forEach(p -> {
                 String name = p.getFileName().toString();
                 if (".git".equals(name)) return;
                 String rel = relPrefix.isBlank() ? name : relPrefix + "/" + name;
                 boolean isDir = Files.isDirectory(p);
                 TreeItem<FileItem> child = new TreeItem<>(new FileItem(name, rel, isDir));
                 parentItem.getChildren().add(child);
                 if (isDir) {
                     // Automatically expand 1 level deep for src
                     if ("src".equals(name) || "main".equals(name)) child.setExpanded(true);
                     populateFileTree(child, p, rel);
                 }
             });
        } catch (Exception ignored) {}
    }

    private void markSelectedTreeItem(TreeView<FileItem> tree, ModuleModel module, FolderType type, VBox contentRootsPanel) {
        TreeItem<FileItem> sel = tree.getSelectionModel().getSelectedItem();
        if (sel != null && sel.getValue() != null && sel.getValue().isDir()) {
            module.markFolder(sel.getValue().relativePath(), type);
            tree.refresh();
            refreshContentRootsPanel(module, tree, contentRootsPanel);
        }
    }

    private VBox buildContentRootsList(ModuleModel module, TreeView<FileItem> fileTree) {
        VBox panel = new VBox(8);
        panel.getStyleClass().add("ps-content-roots");
        panel.setPadding(new Insets(6));
        refreshContentRootsPanel(module, fileTree, panel);
        return panel;
    }

    private void refreshContentRootsPanel(ModuleModel module, TreeView<FileItem> fileTree, VBox panel) {
        panel.getChildren().clear();

        Button addRootBtn = new Button("+ Add Content Root");
        addRootBtn.getStyleClass().add("dialog-secondary");
        addRootBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Add Content Root");
            File chosen = dc.showDialog(stage);
            if (chosen != null) {
                module.setContentRoot(chosen.toPath());
                refreshContentRootsPanel(module, fileTree, panel);
            }
        });

        HBox rootHeader = new HBox(8);
        rootHeader.setAlignment(Pos.CENTER_LEFT);
        String rootDisplay = module.getContentRoot() != null ? module.getContentRoot().toString() : projectRoot.toString();
        Label rootLbl = new Label(rootDisplay);
        rootLbl.setStyle("-fx-text-fill: #EDEFF6; -fx-font-weight: bold; -fx-font-size: 12px;");
        rootHeader.getChildren().addAll(rootLbl);

        panel.getChildren().addAll(addRootBtn, rootHeader);

        // Category sections matching media_1789872972878.png
        addFolderCategoryView(panel, module, fileTree, "Source Folders", FolderType.SOURCE, module.getSourceFolders());
        addFolderCategoryView(panel, module, fileTree, "Test Source Folders", FolderType.TEST_SOURCE, module.getTestSourceFolders());
        addFolderCategoryView(panel, module, fileTree, "Resource Folders", FolderType.RESOURCE, module.getResourceFolders());
        addFolderCategoryView(panel, module, fileTree, "Test Resource Folders", FolderType.TEST_RESOURCE, module.getTestResourceFolders());
        addFolderCategoryView(panel, module, fileTree, "Excluded Folders", FolderType.EXCLUDED, module.getExcludedFolders());
    }

    private void addFolderCategoryView(VBox panel, ModuleModel module, TreeView<FileItem> fileTree,
                                       String header, FolderType type, Set<String> folders) {
        if (folders.isEmpty()) return;

        Label headerLbl = new Label(header);
        headerLbl.setStyle("-fx-text-fill: " + type.hexColor() + "; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 4 0 2 0;");
        panel.getChildren().add(headerLbl);

        for (String f : new ArrayList<>(folders)) {
            HBox itemRow = new HBox(8);
            itemRow.setAlignment(Pos.CENTER_LEFT);
            itemRow.setPadding(new Insets(1, 0, 1, 14));

            Label pathLbl = new Label(f);
            pathLbl.setStyle("-fx-text-fill: " + type.hexColor() + "; -fx-font-size: 12px;");
            HBox.setHgrow(pathLbl, Priority.ALWAYS);

            Button removeBtn = new Button("✕");
            removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #848BA3; -fx-cursor: hand; -fx-font-size: 11px;");
            removeBtn.setOnAction(e -> {
                module.unmarkFolder(f);
                fileTree.refresh();
                refreshContentRootsPanel(module, fileTree, panel);
            });

            itemRow.getChildren().addAll(pathLbl, removeBtn);
            panel.getChildren().add(itemRow);
        }
    }

    private Node buildModulePathsTab(ModuleModel module) {
        VBox box = new VBox(14);
        box.setPadding(new Insets(14));

        ToggleGroup outGroup = new ToggleGroup();
        RadioButton inheritRadio = new RadioButton("Inherit project compile output path");
        inheritRadio.setToggleGroup(outGroup);
        inheritRadio.setSelected(module.isInheritCompilerOutput());

        RadioButton customRadio = new RadioButton("Use module compile output path");
        customRadio.setToggleGroup(outGroup);
        customRadio.setSelected(!module.isInheritCompilerOutput());

        outGroup.selectedToggleProperty().addListener((obs, old, n) -> {
            module.setInheritCompilerOutput(inheritRadio.isSelected());
        });

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setPadding(new Insets(4, 0, 4, 24));

        grid.add(formLabel("Output path:"), 0, 0);
        TextField outField = new TextField(module.getOutputPath());
        outField.setPrefWidth(350);
        outField.textProperty().addListener((obs, old, v) -> module.setOutputPath(v.trim()));
        grid.add(outField, 1, 0);

        grid.add(formLabel("Test output path:"), 0, 1);
        TextField testOutField = new TextField(module.getTestOutputPath());
        testOutField.setPrefWidth(350);
        testOutField.textProperty().addListener((obs, old, v) -> module.setTestOutputPath(v.trim()));
        grid.add(testOutField, 1, 1);

        CheckBox exclCheck = new CheckBox("Exclude output paths");
        exclCheck.setSelected(true);

        box.getChildren().addAll(inheritRadio, customRadio, grid, exclCheck);
        return box;
    }

    private Node buildModuleDependenciesTab(ModuleModel module) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));

        // Module SDK picker
        HBox sdkRow = new HBox(8);
        sdkRow.setAlignment(Pos.CENTER_LEFT);
        sdkRow.getChildren().addAll(formLabel("Module SDK:"), buildSdkComboBox());

        // Dependencies list
        HBox toolRow = new HBox(6);
        Button addDepBtn = new Button("+");
        addDepBtn.getStyleClass().add("ps-add-button");
        Button remDepBtn = new Button("-");
        remDepBtn.getStyleClass().add("ps-add-button");
        toolRow.getChildren().addAll(addDepBtn, remDepBtn);

        TableView<DependencyItem> table = new TableView<>();
        table.setPrefHeight(300);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<DependencyItem, String> scopeCol = new TableColumn<>("Scope");
        scopeCol.setPrefWidth(90);
        scopeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getScope()));

        TableColumn<DependencyItem, String> nameCol = new TableColumn<>("Dependency");
        nameCol.setPrefWidth(450);
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        table.getColumns().addAll(scopeCol, nameCol);
        table.setItems(FXCollections.observableArrayList(module.getDependencies()));

        addDepBtn.setOnAction(e -> {
            TextInputDialog tid = new TextInputDialog("groupId:artifactId:version");
            tid.setTitle("Add Dependency");
            tid.setHeaderText("Enter dependency identifier:");
            tid.showAndWait().ifPresent(dep -> {
                if (!dep.isBlank()) {
                    DependencyItem di = new DependencyItem(dep.trim(), "Compile", false);
                    module.getDependencies().add(di);
                    table.getItems().add(di);
                }
            });
        });

        remDepBtn.setOnAction(e -> {
            DependencyItem sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                module.getDependencies().remove(sel);
                table.getItems().remove(sel);
            }
        });

        box.getChildren().addAll(sdkRow, toolRow, table);
        return box;
    }

    // --------------------------------------------------- 3. LIBRARIES PAGE

    private Node buildLibrariesPage() {
        VBox box = new VBox(12);
        Label title = new Label("Libraries");
        title.getStyleClass().add("ps-page-title");

        HBox tools = new HBox(8);
        Button addBtn = new Button("+");
        addBtn.getStyleClass().add("ps-add-button");
        Button remBtn = new Button("-");
        remBtn.getStyleClass().add("ps-add-button");
        tools.getChildren().addAll(addBtn, remBtn);

        ListView<LibraryModel> list = new ListView<>(FXCollections.observableArrayList(model.getLibraries()));
        list.setPrefHeight(250);
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(LibraryModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getName());
                    Label icon = new Label("📚");
                    setGraphic(icon);
                }
            }
        });

        box.getChildren().addAll(title, tools, list);
        return box;
    }

    // --------------------------------------------------- 4. FACETS PAGE

    private Node buildFacetsPage() {
        VBox box = new VBox(12);
        Label title = new Label("Facets");
        title.getStyleClass().add("ps-page-title");

        HBox tools = new HBox(8);
        Button addBtn = new Button("+");
        addBtn.getStyleClass().add("ps-add-button");
        Button remBtn = new Button("-");
        remBtn.getStyleClass().add("ps-add-button");
        tools.getChildren().addAll(addBtn, remBtn);

        ListView<FacetModel> list = new ListView<>(FXCollections.observableArrayList(model.getFacets()));
        list.setPrefHeight(220);
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(FacetModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getName() + " (" + item.getType() + ")");
                    Label icon = new Label("✦");
                    icon.setStyle("-fx-text-fill: #E8B450;");
                    setGraphic(icon);
                }
            }
        });

        box.getChildren().addAll(title, tools, list);
        return box;
    }

    // --------------------------------------------------- 5. ARTIFACTS PAGE

    private Node buildArtifactsPage() {
        VBox box = new VBox(12);
        Label title = new Label("Artifacts");
        title.getStyleClass().add("ps-page-title");

        HBox tools = new HBox(8);
        Button addBtn = new Button("+");
        addBtn.getStyleClass().add("ps-add-button");
        Button remBtn = new Button("-");
        remBtn.getStyleClass().add("ps-add-button");
        tools.getChildren().addAll(addBtn, remBtn);

        ListView<ArtifactModel> list = new ListView<>(FXCollections.observableArrayList(model.getArtifacts()));
        list.setPrefHeight(220);
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ArtifactModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getName() + " (" + item.getType() + ") -> " + item.getOutputPath());
                    Label icon = new Label("📦");
                    setGraphic(icon);
                }
            }
        });

        box.getChildren().addAll(title, tools, list);
        return box;
    }

    // --------------------------------------------------- 6. SDKS PAGE

    private Node buildSdksPage() {
        VBox outer = new VBox(12);
        Label title = new Label("SDKs");
        title.getStyleClass().add("ps-page-title");

        SplitPane split = new SplitPane();
        split.setDividerPositions(0.3);
        VBox.setVgrow(split, Priority.ALWAYS);

        // Left: SDK list with + and -
        VBox left = new VBox(6);
        List<ProjectSdk.SdkItem> allSdks = ProjectSdk.discoverAllSdks();
        ListView<ProjectSdk.SdkItem> sdkList = new ListView<>(FXCollections.observableArrayList(allSdks));

        HBox tools = new HBox(6);
        Button addBtn = new Button("+");
        addBtn.getStyleClass().add("ps-add-button");
        addBtn.setOnAction(e -> showAddSdkMenu(addBtn, () -> showPage("SDKs")));

        Button remBtn = new Button("-");
        remBtn.getStyleClass().add("ps-add-button");
        remBtn.setOnAction(e -> {
            ProjectSdk.SdkItem sel = sdkList.getSelectionModel().getSelectedItem();
            if (sel != null && sel.isRegistered()) {
                List<ProjectSdk.SdkItem> registered = new ArrayList<>(ProjectSdk.loadRegisteredSdks());
                registered.removeIf(item -> item.id().equals(sel.id()) || item.name().equals(sel.name()));
                ProjectSdk.saveRegisteredSdks(registered);
                showPage("SDKs");
            }
        });
        tools.getChildren().addAll(addBtn, remBtn);
        sdkList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ProjectSdk.SdkItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.name());
                    Label icon = new Label(item.type().iconGlyph());
                    setGraphic(icon);
                }
            }
        });
        sdkList.getSelectionModel().selectFirst();
        VBox.setVgrow(sdkList, Priority.ALWAYS);
        left.getChildren().addAll(tools, sdkList);

        // Right: Selected SDK details
        VBox right = new VBox(10);
        right.setPadding(new Insets(0, 0, 0, 12));

        Label sdkNameLbl = new Label("Name:");
        TextField nameField = new TextField();
        Label homeLbl = new Label("Home path:");
        TextField homeField = new TextField();
        homeField.setEditable(false);

        TabPane sdkTabs = new TabPane();
        Tab cpTab = new Tab("Classpath");
        cpTab.setClosable(false);
        ListView<String> cpList = new ListView<>();
        cpTab.setContent(cpList);

        Tab srcTab = new Tab("Sourcepath");
        srcTab.setClosable(false);
        ListView<String> srcList = new ListView<>();
        srcTab.setContent(srcList);

        Tab docTab = new Tab("Documentation Paths");
        docTab.setClosable(false);
        ListView<String> docList = new ListView<>();
        docTab.setContent(docList);

        sdkTabs.getTabs().addAll(cpTab, srcTab, docTab);
        VBox.setVgrow(sdkTabs, Priority.ALWAYS);

        Runnable updateRight = () -> {
            ProjectSdk.SdkItem sel = sdkList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                nameField.setText(sel.name());
                homeField.setText(sel.homePath() != null ? sel.homePath() : "");
                cpList.getItems().setAll(sel.classpathEntries());
                srcList.getItems().clear();
                if (sel.homePath() != null && Files.isRegularFile(Path.of(sel.homePath()).resolve("lib/src.zip"))) {
                    srcList.getItems().add(Path.of(sel.homePath()).resolve("lib/src.zip").toString());
                }
                docList.getItems().clear();
                if (sel.type() == ProjectSdk.SdkType.JDK) {
                    docList.getItems().add("https://docs.oracle.com/en/java/javase/" + sel.version() + "/docs/api");
                }
            }
        };

        sdkList.getSelectionModel().selectedItemProperty().addListener((obs, old, n) -> updateRight.run());
        updateRight.run();

        right.getChildren().addAll(
                new HBox(8, formLabel("Name:"), nameField),
                new HBox(8, formLabel("Home:"), homeField),
                sdkTabs);

        split.getItems().addAll(left, right);
        outer.getChildren().addAll(title, split);
        return outer;
    }

    // --------------------------------------------------- 7. GLOBAL LIBRARIES PAGE

    private Node buildGlobalLibrariesPage() {
        VBox box = new VBox(12);
        Label title = new Label("Global Libraries");
        title.getStyleClass().add("ps-page-title");

        HBox btnRow = new HBox(8);
        Button javaBtn = new Button("Java");
        javaBtn.getStyleClass().add("dialog-secondary");
        Button mavenBtn = new Button("From Maven…");
        mavenBtn.getStyleClass().add("dialog-secondary");
        btnRow.getChildren().addAll(javaBtn, mavenBtn);

        Label empty = new Label("No global libraries configured");
        empty.getStyleClass().add("ps-empty");

        box.getChildren().addAll(title, btnRow, empty);
        return box;
    }

    // --------------------------------------------------- 8. PROBLEMS PAGE

    private Node buildProblemsPage() {
        VBox box = new VBox(14);
        Label title = new Label("Problems");
        title.getStyleClass().add("ps-page-title");

        List<Node> problemRows = new ArrayList<>();

        if (model.getProjectSdk() == null) {
            HBox p = createProblemRow("⚠ Project SDK is not defined", "Setup SDK", () -> navigateTo("Project"));
            problemRows.add(p);
        }

        if (model.getCompilerOutput() == null || model.getCompilerOutput().isBlank()) {
            HBox p = createProblemRow("⚠ Project compiler output path is not specified", "Configure", () -> navigateTo("Project"));
            problemRows.add(p);
        }

        ModuleModel primary = model.getPrimaryModule();
        if (primary != null) {
            for (String sf : primary.getSourceFolders()) {
                if (projectRoot != null && !Files.isDirectory(projectRoot.resolve(sf))) {
                    HBox p = createProblemRow("⚠ Source folder does not exist: " + sf, "Create Folder", () -> {
                        try {
                            Files.createDirectories(projectRoot.resolve(sf));
                            showPage("Problems");
                        } catch (Exception ignored) {}
                    });
                    problemRows.add(p);
                }
            }
        }

        if (problemRows.isEmpty()) {
            Label good = new Label("✓ No problems found in project structure");
            good.setStyle("-fx-text-fill: #59A869; -fx-font-size: 13px; -fx-font-weight: bold;");
            box.getChildren().addAll(title, good);
        } else {
            box.getChildren().add(title);
            box.getChildren().addAll(problemRows);
        }

        return box;
    }

    private HBox createProblemRow(String text, String actionText, Runnable action) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #241E1E; -fx-padding: 8 12; -fx-background-radius: 6; -fx-border-color: #6E3B3B; -fx-border-radius: 6;");

        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #F28B82; -fx-font-size: 12px;");
        HBox.setHgrow(lbl, Priority.ALWAYS);

        Button actBtn = new Button(actionText);
        actBtn.getStyleClass().add("dialog-primary");
        actBtn.setOnAction(e -> action.run());

        row.getChildren().addAll(lbl, actBtn);
        return row;
    }

    // --------------------------------------------------- Helpers

    private Label formLabel(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("ps-label");
        lbl.setMinWidth(110);
        return lbl;
    }

    private ScrollPane wrapInScroll(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("ps-scroll");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }
}
