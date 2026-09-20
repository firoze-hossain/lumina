package dev.lumina.ui;

import dev.lumina.project.JdkMetadata;
import dev.lumina.project.ProjectSdk;
import dev.lumina.project.ProjectStructureModel;
import dev.lumina.project.ProjectStructureModel.*;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
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
import java.util.function.Consumer;

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
    private String currentPage = "Project";

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
        this.currentPage = pageName;
        contentContainer.getChildren().clear();
        if ("Project".equals(pageName)) {
            contentContainer.setPadding(new Insets(16, 20, 16, 20));
        } else {
            contentContainer.setPadding(new Insets(0));
        }
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

        // 1. Download JDK... matching media_1789874953190.png
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
                else showPage(currentPage);
            });
            dialog.show();
        });

        // 2. Add JDK from disk... matching media_1789874953190.png
        MenuItem jdkDisk = new MenuItem("Add JDK from disk…");
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
                else showPage(currentPage);
            }
        });

        // 3. Add Python SDK from disk... matching media_1789874953190.png
        MenuItem pyDisk = new MenuItem("Add Python SDK from disk…");
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
                else showPage(currentPage);
            }
        });

        // 4. Add Ruby Interpreter from disk... matching media_1789874953190.png
        MenuItem rubyDisk = new MenuItem("Add Ruby Interpreter from disk…");
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
                else showPage(currentPage);
            }
        });

        // 5. Add JRuby Interpreter from disk... matching media_1789874953190.png
        MenuItem jrubyDisk = new MenuItem("Add JRuby Interpreter from disk…");
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
                else showPage(currentPage);
            }
        });

        // 6. Add PHP Interpreter from disk... matching media_1789874953190.png
        MenuItem phpDisk = new MenuItem("Add PHP Interpreter from disk…");
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
                else showPage(currentPage);
            }
        });

        menu.getItems().addAll(downloadJdk, jdkDisk, pyDisk, rubyDisk, jrubyDisk, phpDisk);

        // Detected SDKs Section matching media_1789874953190.png
        try {
            List<ProjectSdk.SdkItem> all = ProjectSdk.discoverAllSdks();
            List<ProjectSdk.SdkItem> detected = all.stream().filter(s -> !s.isRegistered()).toList();
            if (!detected.isEmpty()) {
                menu.getItems().add(new SeparatorMenuItem());

                CustomMenuItem header = new CustomMenuItem();
                Label headerLbl = new Label("Detected SDKs");
                headerLbl.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 8;");
                header.setContent(headerLbl);
                header.setHideOnClick(false);
                menu.getItems().add(header);

                for (ProjectSdk.SdkItem det : detected) {
                    CustomMenuItem item = new CustomMenuItem();
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setStyle("-fx-padding: 4 8; -fx-cursor: hand;");

                    Label icon = new Label(det.type() == ProjectSdk.SdkType.JDK ? "📎" : det.type().iconGlyph());
                    icon.setStyle("-fx-font-size: 12px; -fx-text-fill: #848BA3;");

                    Label nameLbl = new Label(det.name());
                    nameLbl.setStyle("-fx-text-fill: #EDEFF6; -fx-font-size: 12px;");

                    Label pathLbl = new Label(det.homePath() != null ? det.homePath() : "");
                    pathLbl.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");

                    row.getChildren().addAll(icon, nameLbl, pathLbl);
                    item.setContent(row);

                    item.setOnAction(e -> {
                        ProjectSdk.SdkItem reg = ProjectSdk.registerSdk(det.name(), det.type(), det.homePath(), det.version());
                        model.setProjectSdk(reg);
                        if (onSdkAdded != null) onSdkAdded.run();
                        else showPage(currentPage);
                    });
                    menu.getItems().add(item);
                }
            }
        } catch (Exception ignored) {}

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

        ObservableList<ModuleModel> modItems = FXCollections.observableArrayList(model.getModules());
        ListView<ModuleModel> moduleList = new ListView<>(modItems);
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

        addModBtn.setOnAction(e -> {
            TextInputDialog tid = new TextInputDialog("new-module");
            tid.setTitle("New Module");
            tid.setHeaderText("Enter module name:");
            tid.showAndWait().ifPresent(name -> {
                if (!name.isBlank()) {
                    ModuleModel newMod = new ModuleModel(name.trim(), projectRoot != null ? projectRoot.resolve(name.trim()) : Path.of(name.trim()));
                    newMod.setType("JAVA_MODULE");
                    model.getModules().add(newMod);
                    modItems.add(newMod);
                    moduleList.getSelectionModel().select(newMod);
                }
            });
        });

        remModBtn.setOnAction(e -> {
            ModuleModel sel = moduleList.getSelectionModel().getSelectedItem();
            if (sel != null && modItems.size() > 1) {
                model.getModules().remove(sel);
                modItems.remove(sel);
            }
        });

        copyModBtn.setOnAction(e -> {
            ModuleModel sel = moduleList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                ModuleModel copy = new ModuleModel(sel.getName() + "-copy", sel.getContentRoot());
                copy.setType(sel.getType());
                copy.setLanguageLevel(sel.getLanguageLevel());
                copy.setModuleSdk(sel.getModuleSdk());
                copy.setOutputPath(sel.getOutputPath());
                copy.setTestOutputPath(sel.getTestOutputPath());
                copy.setInheritCompilerOutput(sel.isInheritCompilerOutput());
                copy.setExcludeOutputPaths(sel.isExcludeOutputPaths());
                copy.getSourceFolders().addAll(sel.getSourceFolders());
                copy.getTestSourceFolders().addAll(sel.getTestSourceFolders());
                copy.getResourceFolders().addAll(sel.getResourceFolders());
                copy.getTestResourceFolders().addAll(sel.getTestResourceFolders());
                copy.getExcludedFolders().addAll(sel.getExcludedFolders());
                copy.getDependencies().addAll(sel.getDependencies());
                model.getModules().add(copy);
                modItems.add(copy);
                moduleList.getSelectionModel().select(copy);
            }
        });

        leftModuleBox.getChildren().addAll(modToolbar, moduleList);

        // --- Right: Module details tab pane ---
        VBox rightDetails = new VBox(10);
        rightDetails.setPadding(new Insets(0, 0, 0, 12));

        ModuleModel initialModule = moduleList.getSelectionModel().getSelectedItem() != null
                ? moduleList.getSelectionModel().getSelectedItem()
                : model.getPrimaryModule();

        HBox nameRow = new HBox(12);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLbl = formLabel("Name:");
        TextField moduleNameField = new TextField(initialModule.getName());
        moduleNameField.setPrefWidth(300);
        moduleNameField.textProperty().addListener((obs, old, v) -> {
            ModuleModel sel = moduleList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                sel.setName(v.trim());
                moduleList.refresh();
            }
        });
        nameRow.getChildren().addAll(nameLbl, moduleNameField);

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("settings-tab-pane");
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Tabs: Sources, Paths, Dependencies
        Tab sourcesTab = new Tab("Sources", buildModuleSourcesTab(initialModule));
        sourcesTab.setClosable(false);

        Tab pathsTab = new Tab("Paths", buildModulePathsTab(initialModule));
        pathsTab.setClosable(false);

        Tab depsTab = new Tab("Dependencies", buildModuleDependenciesTab(initialModule));
        depsTab.setClosable(false);

        tabPane.getTabs().addAll(sourcesTab, pathsTab, depsTab);
        rightDetails.getChildren().addAll(nameRow, tabPane);

        moduleList.getSelectionModel().selectedItemProperty().addListener((obs, old, newMod) -> {
            if (newMod != null) {
                moduleNameField.setText(newMod.getName());
                sourcesTab.setContent(buildModuleSourcesTab(newMod));
                pathsTab.setContent(buildModulePathsTab(newMod));
                depsTab.setContent(buildModuleDependenciesTab(newMod));
            }
        });

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
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox box = new VBox(16);
        box.setPadding(new Insets(12, 16, 16, 16));

        // Group 1: Compiler Output
        Label outGroupTitle = new Label("Compiler Output");
        outGroupTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        ToggleGroup outGroup = new ToggleGroup();
        RadioButton inheritRadio = new RadioButton("Inherit project compile output path");
        inheritRadio.setToggleGroup(outGroup);
        inheritRadio.setSelected(module.isInheritCompilerOutput());
        inheritRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        RadioButton customRadio = new RadioButton("Use module compile output path");
        customRadio.setToggleGroup(outGroup);
        customRadio.setSelected(!module.isInheritCompilerOutput());
        customRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        VBox customOutputBox = new VBox(8);
        customOutputBox.setPadding(new Insets(2, 0, 4, 20));

        // Output path row
        HBox outRow = new HBox(8);
        outRow.setAlignment(Pos.CENTER_LEFT);
        Label outLbl = new Label("Output path:");
        outLbl.setPrefWidth(120);
        outLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        TextField outField = new TextField(module.getOutputPath());
        outField.setPrefWidth(420);
        HBox.setHgrow(outField, Priority.ALWAYS);
        outField.textProperty().addListener((obs, o, n) -> module.setOutputPath(n.trim()));
        Button browseOutBtn = new Button("📁");
        browseOutBtn.getStyleClass().add("ps-add-button");
        browseOutBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Compile Output Path");
            if (model.getProjectRoot() != null && Files.exists(model.getProjectRoot())) {
                dc.setInitialDirectory(model.getProjectRoot().toFile());
            }
            File f = dc.showDialog(box.getScene().getWindow());
            if (f != null) {
                outField.setText(f.getAbsolutePath());
            }
        });
        outRow.getChildren().addAll(outLbl, outField, browseOutBtn);

        // Test output path row
        HBox testOutRow = new HBox(8);
        testOutRow.setAlignment(Pos.CENTER_LEFT);
        Label testOutLbl = new Label("Test output path:");
        testOutLbl.setPrefWidth(120);
        testOutLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        TextField testOutField = new TextField(module.getTestOutputPath());
        testOutField.setPrefWidth(420);
        HBox.setHgrow(testOutField, Priority.ALWAYS);
        testOutField.textProperty().addListener((obs, o, n) -> module.setTestOutputPath(n.trim()));
        Button browseTestOutBtn = new Button("📁");
        browseTestOutBtn.getStyleClass().add("ps-add-button");
        browseTestOutBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Test Compile Output Path");
            if (model.getProjectRoot() != null && Files.exists(model.getProjectRoot())) {
                dc.setInitialDirectory(model.getProjectRoot().toFile());
            }
            File f = dc.showDialog(box.getScene().getWindow());
            if (f != null) {
                testOutField.setText(f.getAbsolutePath());
            }
        });
        testOutRow.getChildren().addAll(testOutLbl, testOutField, browseTestOutBtn);

        // Exclude output paths checkbox
        CheckBox exclCheck = new CheckBox("Exclude output paths");
        exclCheck.setSelected(module.isExcludeOutputPaths());
        exclCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        exclCheck.selectedProperty().addListener((obs, o, n) -> module.setExcludeOutputPaths(n));

        customOutputBox.getChildren().addAll(outRow, testOutRow, exclCheck);

        // Enable/disable custom fields based on radio
        customOutputBox.disableProperty().bind(inheritRadio.selectedProperty());
        outGroup.selectedToggleProperty().addListener((obs, old, n) -> {
            module.setInheritCompilerOutput(inheritRadio.isSelected());
        });

        // Section 2: JavaDoc:
        VBox javadocSection = new VBox(6);
        Label javadocTitle = new Label("JavaDoc:");
        javadocTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 12px;");

        HBox javadocTools = new HBox(6);
        Button addDocBtn = new Button("+");
        addDocBtn.getStyleClass().add("ps-add-button");
        Button addDocUrlBtn = new Button("🌐");
        addDocUrlBtn.getStyleClass().add("ps-add-button");
        Button remDocBtn = new Button("-");
        remDocBtn.getStyleClass().add("ps-add-button");
        javadocTools.getChildren().addAll(addDocBtn, addDocUrlBtn, remDocBtn);

        ObservableList<String> javadocListItems = FXCollections.observableArrayList(module.getJavadocPaths());
        ListView<String> javadocListView = new ListView<>(javadocListItems);
        javadocListView.setPrefHeight(90);
        javadocListView.setPlaceholder(new Label("Nothing to show"));
        javadocListView.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");

        addDocBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select JavaDoc Directory / Archive");
            File f = dc.showDialog(box.getScene().getWindow());
            if (f != null) {
                module.getJavadocPaths().add(f.getAbsolutePath());
                javadocListItems.add(f.getAbsolutePath());
            }
        });
        addDocUrlBtn.setOnAction(e -> {
            TextInputDialog tid = new TextInputDialog("https://");
            tid.setTitle("Attach JavaDoc URL");
            tid.setHeaderText("Specify JavaDoc URL:");
            tid.showAndWait().ifPresent(url -> {
                if (!url.isBlank()) {
                    module.getJavadocPaths().add(url.trim());
                    javadocListItems.add(url.trim());
                }
            });
        });
        remDocBtn.setOnAction(e -> {
            String sel = javadocListView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                module.getJavadocPaths().remove(sel);
                javadocListItems.remove(sel);
            }
        });

        Label javadocHint = new Label("Manage external JavaDocs attached to this module.\nExternal JavaDocs override JavaDoc annotations you might have in your module.");
        javadocHint.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");

        javadocSection.getChildren().addAll(javadocTitle, javadocTools, javadocListView, javadocHint);

        // Section 3: External Annotations:
        VBox annoSection = new VBox(6);
        Label annoTitle = new Label("External Annotations:");
        annoTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 12px;");

        HBox annoTools = new HBox(6);
        Button addAnnoBtn = new Button("+");
        addAnnoBtn.getStyleClass().add("ps-add-button");
        Button remAnnoBtn = new Button("-");
        remAnnoBtn.getStyleClass().add("ps-add-button");
        annoTools.getChildren().addAll(addAnnoBtn, remAnnoBtn);

        ObservableList<String> annoListItems = FXCollections.observableArrayList(module.getExternalAnnotationsPaths());
        ListView<String> annoListView = new ListView<>(annoListItems);
        annoListView.setPrefHeight(90);
        annoListView.setPlaceholder(new Label("Nothing to show"));
        annoListView.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");

        addAnnoBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select External Annotations Root");
            File f = dc.showDialog(box.getScene().getWindow());
            if (f != null) {
                module.getExternalAnnotationsPaths().add(f.getAbsolutePath());
                annoListItems.add(f.getAbsolutePath());
            }
        });
        remAnnoBtn.setOnAction(e -> {
            String sel = annoListView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                module.getExternalAnnotationsPaths().remove(sel);
                annoListItems.remove(sel);
            }
        });

        Label annoHint = new Label("Manage external annotations attached to this module.");
        annoHint.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");

        annoSection.getChildren().addAll(annoTitle, annoTools, annoListView, annoHint);

        box.getChildren().addAll(outGroupTitle, inheritRadio, customRadio, customOutputBox, javadocSection, annoSection);
        scroll.setContent(box);
        return scroll;
    }

    private Node buildModuleDependenciesTab(ModuleModel module) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10, 14, 10, 14));

        // Table reference ahead of SDK row so combo box can update table row 0
        TableView<DependencyItem> table = new TableView<>();
        table.setPrefHeight(340);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.getStyleClass().add("ps-deps-table");

        // 1. Module SDK row
        HBox sdkRow = new HBox(10);
        sdkRow.setAlignment(Pos.CENTER_LEFT);
        Label sdkLbl = formLabel("Module SDK:");
        sdkLbl.setPrefWidth(90);

        ComboBox<Object> modSdkCombo = buildModuleSdkComboBox(module, table);
        modSdkCombo.setPrefWidth(350);

        Button editSdkBtn = new Button("Edit");
        editSdkBtn.getStyleClass().add("ps-add-button");
        editSdkBtn.setStyle("-fx-padding: 4 12 4 12; -fx-font-size: 12px;");
        editSdkBtn.setOnAction(e -> showPage("SDKs"));

        sdkRow.getChildren().addAll(sdkLbl, modSdkCombo, editSdkBtn);

        // 2. Toolbar above Table: + - ↑ ↓ ✎
        HBox toolRow = new HBox(6);
        Button addDepBtn = new Button("+");
        addDepBtn.getStyleClass().add("ps-add-button");
        Button remDepBtn = new Button("-");
        remDepBtn.getStyleClass().add("ps-add-button");
        Button moveUpBtn = new Button("↑");
        moveUpBtn.getStyleClass().add("ps-add-button");
        Button moveDownBtn = new Button("↓");
        moveDownBtn.getStyleClass().add("ps-add-button");
        Button editDepBtn = new Button("✎");
        editDepBtn.getStyleClass().add("ps-add-button");
        toolRow.getChildren().addAll(addDepBtn, remDepBtn, moveUpBtn, moveDownBtn, editDepBtn);

        // Ensure dependencies has default rows if empty
        if (module.getDependencies().isEmpty()) {
            ProjectSdk.SdkItem activeSdk = module.getModuleSdk() != null ? module.getModuleSdk() : model.getProjectSdk();
            module.getDependencies().add(DependencyItem.forSdk(activeSdk));
            module.getDependencies().add(DependencyItem.forModuleSource());
            for (LibraryModel lib : model.getLibraries()) {
                module.getDependencies().add(DependencyItem.forLibrary(lib, "Compile", false));
            }
        }

        ObservableList<DependencyItem> depItems = FXCollections.observableArrayList(module.getDependencies());
        table.setItems(depItems);

        // Column 1: Export checkbox
        TableColumn<DependencyItem, Boolean> exportCol = new TableColumn<>("Export");
        exportCol.setPrefWidth(65);
        exportCol.setSortable(false);
        exportCol.setCellValueFactory(data -> new SimpleBooleanProperty(data.getValue().isExport()));
        exportCol.setCellFactory(col -> new TableCell<>() {
            private final CheckBox cb = new CheckBox();
            {
                cb.setOnAction(e -> {
                    DependencyItem item = getTableRow() != null ? getTableRow().getItem() : null;
                    if (item != null) {
                        item.setExport(cb.isSelected());
                    }
                });
            }
            @Override
            protected void updateItem(Boolean val, boolean empty) {
                super.updateItem(val, empty);
                DependencyItem item = getTableRow() != null ? getTableRow().getItem() : null;
                if (empty || item == null || item.isSdk() || item.isModuleSource()) {
                    setGraphic(null);
                } else {
                    cb.setSelected(item.isExport());
                    setGraphic(cb);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Column 2: Item Name with Icon
        TableColumn<DependencyItem, DependencyItem> itemCol = new TableColumn<>("");
        itemCol.setPrefWidth(480);
        itemCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        itemCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(DependencyItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.CENTER_LEFT);
                    Label icon;
                    if (item.isSdk()) {
                        icon = new Label("📁");
                        icon.setStyle("-fx-text-fill: #548AF7; -fx-font-size: 13px;");
                    } else if (item.isModuleSource()) {
                        icon = new Label("📁");
                        icon.setStyle("-fx-text-fill: #548AF7; -fx-font-size: 13px;");
                    } else if (item.isModule()) {
                        icon = new Label("▣");
                        icon.setStyle("-fx-text-fill: #548AF7; -fx-font-size: 13px;");
                    } else {
                        icon = new Label("📚");
                        icon.setStyle("-fx-font-size: 13px;");
                    }

                    Label nameLbl = new Label(item.getName());
                    nameLbl.setStyle("-fx-text-fill: #EDEFF6; -fx-font-size: 12px;");
                    row.getChildren().addAll(icon, nameLbl);
                    setGraphic(row);
                }
            }
        });

        // Column 3: Scope Dropdown
        TableColumn<DependencyItem, String> scopeCol = new TableColumn<>("Scope");
        scopeCol.setPrefWidth(120);
        scopeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getScope()));
        scopeCol.setCellFactory(col -> new TableCell<>() {
            private final ComboBox<String> scopeCombo = new ComboBox<>(
                    FXCollections.observableArrayList("Compile", "Test", "Runtime", "Provided")
            );
            {
                scopeCombo.setPrefWidth(100);
                scopeCombo.setStyle("-fx-font-size: 11px;");
                scopeCombo.setOnAction(e -> {
                    DependencyItem item = getTableRow() != null ? getTableRow().getItem() : null;
                    if (item != null && scopeCombo.getValue() != null) {
                        item.setScope(scopeCombo.getValue());
                    }
                });
            }
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                DependencyItem item = getTableRow() != null ? getTableRow().getItem() : null;
                if (empty || item == null || item.isSdk() || item.isModuleSource()) {
                    setGraphic(null);
                } else {
                    scopeCombo.setValue(item.getScope() != null ? item.getScope() : "Compile");
                    setGraphic(scopeCombo);
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        });

        table.getColumns().addAll(exportCol, itemCol, scopeCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Toolbar actions
        addDepBtn.setOnAction(e -> {
            ContextMenu addMenu = new ContextMenu();
            MenuItem mJars = new MenuItem("1. JARs or Directories...");
            mJars.setOnAction(ev -> {
                FileChooser fc = new FileChooser();
                fc.setTitle("Select JARs");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JAR files", "*.jar", "*.zip"));
                List<File> files = fc.showOpenMultipleDialog(box.getScene().getWindow());
                if (files != null) {
                    for (File f : files) {
                        LibraryModel lib = new LibraryModel(f.getName());
                        lib.getClassesPaths().add(f.getAbsolutePath());
                        model.getLibraries().add(lib);
                        DependencyItem di = DependencyItem.forLibrary(lib, "Compile", false);
                        module.getDependencies().add(di);
                        depItems.add(di);
                    }
                }
            });
            MenuItem mLib = new MenuItem("2. Library...");
            mLib.setOnAction(ev -> {
                ChoiceDialog<LibraryModel> cd = new ChoiceDialog<>(
                        !model.getLibraries().isEmpty() ? model.getLibraries().getFirst() : null,
                        model.getLibraries()
                );
                cd.setTitle("Choose Libraries");
                cd.setHeaderText("Select Library to add to module:");
                cd.showAndWait().ifPresent(lib -> {
                    DependencyItem di = DependencyItem.forLibrary(lib, "Compile", false);
                    module.getDependencies().add(di);
                    depItems.add(di);
                });
            });
            MenuItem mMod = new MenuItem("3. Module Dependency...");
            mMod.setOnAction(ev -> {
                List<String> otherModules = model.getModules().stream()
                        .map(ModuleModel::getName)
                        .filter(n -> !n.equals(module.getName()))
                        .toList();
                if (otherModules.isEmpty()) {
                    new Alert(Alert.AlertType.INFORMATION, "No other modules in the project.").showAndWait();
                    return;
                }
                ChoiceDialog<String> cd = new ChoiceDialog<>(otherModules.getFirst(), otherModules);
                cd.setTitle("Add Module Dependency");
                cd.setHeaderText("Choose module:");
                cd.showAndWait().ifPresent(modName -> {
                    DependencyItem di = DependencyItem.forModule(modName, "Compile", false);
                    module.getDependencies().add(di);
                    depItems.add(di);
                });
            });
            addMenu.getItems().addAll(mJars, mLib, mMod);
            addMenu.show(addDepBtn, Side.BOTTOM, 0, 0);
        });

        remDepBtn.setOnAction(e -> {
            DependencyItem sel = table.getSelectionModel().getSelectedItem();
            if (sel != null && !sel.isSdk() && !sel.isModuleSource()) {
                module.getDependencies().remove(sel);
                depItems.remove(sel);
            }
        });

        moveUpBtn.setOnAction(e -> {
            int idx = table.getSelectionModel().getSelectedIndex();
            if (idx > 2) {
                Collections.swap(module.getDependencies(), idx, idx - 1);
                Collections.swap(depItems, idx, idx - 1);
                table.getSelectionModel().select(idx - 1);
            }
        });

        moveDownBtn.setOnAction(e -> {
            int idx = table.getSelectionModel().getSelectedIndex();
            if (idx >= 2 && idx < depItems.size() - 1) {
                Collections.swap(module.getDependencies(), idx, idx + 1);
                Collections.swap(depItems, idx, idx + 1);
                table.getSelectionModel().select(idx + 1);
            }
        });

        editDepBtn.setOnAction(e -> {
            DependencyItem sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                if (sel.isSdk()) {
                    showPage("SDKs");
                } else if (sel.getLibraryModel() != null) {
                    showPage("Libraries");
                }
            }
        });

        // 3. Footer: Dependencies storage format: [ IntelliJ IDEA (.iml) ▾ ]
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);
        Label formatLbl = new Label("Dependencies storage format:");
        formatLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        ComboBox<String> formatCombo = new ComboBox<>(FXCollections.observableArrayList("IntelliJ IDEA (.iml)", "Eclipse (.classpath)"));
        formatCombo.setValue("IntelliJ IDEA (.iml)");
        formatCombo.setStyle("-fx-font-size: 12px;");
        footer.getChildren().addAll(formatLbl, formatCombo);

        box.getChildren().addAll(sdkRow, toolRow, table, footer);
        return box;
    }

    private ComboBox<Object> buildModuleSdkComboBox(ModuleModel module, TableView<DependencyItem> table) {
        ComboBox<Object> combo = new ComboBox<>();
        combo.setPrefWidth(380);

        String projectSdkDisplay = "Project SDK " + (model.getProjectSdk() != null
                ? (model.getProjectSdk().version() != null ? model.getProjectSdk().version() : model.getProjectSdk().name())
                : "");

        Runnable refreshItems = () -> {
            List<ProjectSdk.SdkItem> all = ProjectSdk.discoverAllSdks();
            ObservableList<Object> items = FXCollections.observableArrayList();
            items.add(projectSdkDisplay);

            List<ProjectSdk.SdkItem> registered = all.stream().filter(ProjectSdk.SdkItem::isRegistered).toList();
            items.addAll(registered);

            items.add("+ Add SDK");

            List<ProjectSdk.SdkItem> detected = all.stream().filter(s -> !s.isRegistered()).toList();
            if (!detected.isEmpty()) {
                items.add("Detected SDKs");
                items.addAll(detected);
            }

            combo.setItems(items);

            if (module.getModuleSdk() == null) {
                combo.setValue(projectSdkDisplay);
            } else {
                combo.setValue(module.getModuleSdk());
            }
        };

        refreshItems.run();

        // Popup cell factory matching media_1789874292762.png
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
                    } else if (s.startsWith("Project SDK")) {
                        setText(null);
                        HBox row = new HBox(8);
                        row.setAlignment(Pos.CENTER_LEFT);
                        Label icon = new Label("📁");
                        icon.setStyle("-fx-text-fill: #548AF7; -fx-font-size: 13px;");
                        Label nameLbl = new Label(s);
                        nameLbl.setStyle("-fx-text-fill: #EDEFF6; -fx-font-size: 12px;");
                        row.getChildren().addAll(icon, nameLbl);
                        setGraphic(row);
                        setStyle("-fx-padding: 4 8 4 8;");
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
                if (item instanceof String s && s.startsWith("Project SDK")) {
                    setText(s);
                    Label icon = new Label("📁");
                    icon.setStyle("-fx-text-fill: #548AF7; -fx-font-size: 13px;");
                    setGraphic(icon);
                    setStyle("-fx-text-fill: #EDEFF6;");
                } else if (item instanceof ProjectSdk.SdkItem sdk) {
                    setText(sdk.getShortDisplay());
                    Label icon = new Label("📁");
                    icon.setStyle("-fx-text-fill: #548AF7; -fx-font-size: 13px;");
                    setGraphic(icon);
                    setStyle("-fx-text-fill: #EDEFF6;");
                } else {
                    setText(item.toString());
                    setGraphic(null);
                }
            }
        });

        combo.setOnAction(e -> {
            Object selected = combo.getSelectionModel().getSelectedItem();
            if (selected instanceof String s && s.startsWith("Project SDK")) {
                module.setModuleSdk(null);
                updateDependenciesSdkRow(module, table);
            } else if (selected instanceof ProjectSdk.SdkItem sdk) {
                module.setModuleSdk(sdk);
                updateDependenciesSdkRow(module, table);
            } else if ("+ Add SDK".equals(selected) || (selected instanceof String str && str.contains("Add SDK"))) {
                showAddSdkMenu(combo, () -> {
                    refreshItems.run();
                    updateDependenciesSdkRow(module, table);
                });
            }
        });

        return combo;
    }

    private void updateDependenciesSdkRow(ModuleModel module, TableView<DependencyItem> table) {
        ProjectSdk.SdkItem activeSdk = module.getModuleSdk() != null ? module.getModuleSdk() : model.getProjectSdk();
        String sdkDisplayName = activeSdk != null
                ? (activeSdk.version() != null ? activeSdk.version() + " (" + activeSdk.name() + ")" : activeSdk.name())
                : "Project SDK";

        if (!module.getDependencies().isEmpty() && module.getDependencies().getFirst().isSdk()) {
            module.getDependencies().getFirst().setName(sdkDisplayName);
            module.getDependencies().getFirst().setSdkItem(activeSdk);
        }
        if (table != null) {
            table.refresh();
        }
    }

    // --------------------------------------------------- 3. LIBRARIES PAGE

    private Node buildLibrariesPage() {
        VBox outer = new VBox(12);

        SplitPane split = new SplitPane();
        split.setDividerPositions(0.32);
        VBox.setVgrow(split, Priority.ALWAYS);

        // --- Left Panel: Libraries list ---
        VBox left = new VBox(6);
        left.setPadding(new Insets(4));

        HBox tools = new HBox(6);
        Button addBtn = new Button("+");
        addBtn.getStyleClass().add("ps-add-button");
        Button remBtn = new Button("-");
        remBtn.getStyleClass().add("ps-add-button");
        Button copyBtn = new Button("📋");
        copyBtn.getStyleClass().add("ps-add-button");
        tools.getChildren().addAll(addBtn, remBtn, copyBtn);

        ObservableList<LibraryModel> libItems = FXCollections.observableArrayList(model.getLibraries());
        ListView<LibraryModel> libList = new ListView<>(libItems);
        VBox.setVgrow(libList, Priority.ALWAYS);
        libList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(LibraryModel lib, boolean empty) {
                super.updateItem(lib, empty);
                if (empty || lib == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(lib.getName());
                    Label icon = new Label("📚");
                    setGraphic(icon);
                }
            }
        });

        left.getChildren().addAll(tools, libList);

        // --- Right Panel: Library Details ---
        VBox right = new VBox(12);
        right.setPadding(new Insets(4, 8, 8, 12));

        Runnable refreshRightPanel = () -> {
            right.getChildren().clear();
            LibraryModel selected = libList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                Label emptyLbl = new Label("No library selected");
                emptyLbl.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 13px;");
                right.setAlignment(Pos.CENTER);
                right.getChildren().add(emptyLbl);
                return;
            }
            right.setAlignment(Pos.TOP_LEFT);

            // Name: [ TextField ]
            HBox nameRow = new HBox(8);
            nameRow.setAlignment(Pos.CENTER_LEFT);
            Label nameLbl = formLabel("Name:");
            nameLbl.setPrefWidth(60);
            TextField nameField = new TextField(selected.getName());
            nameField.setPrefWidth(500);
            HBox.setHgrow(nameField, Priority.ALWAYS);
            nameField.textProperty().addListener((obs, o, n) -> {
                selected.setName(n.trim());
                libList.refresh();
            });
            nameRow.getChildren().addAll(nameLbl, nameField);

            // Detail toolbar: + 🌐 📁 -
            HBox detailTools = new HBox(6);
            Button addRootBtn = new Button("+");
            addRootBtn.getStyleClass().add("ps-add-button");
            Button addUrlBtn = new Button("🌐");
            addUrlBtn.getStyleClass().add("ps-add-button");
            Button addJarDirBtn = new Button("📁");
            addJarDirBtn.getStyleClass().add("ps-add-button");
            Button remRootBtn = new Button("-");
            remRootBtn.getStyleClass().add("ps-add-button");
            detailTools.getChildren().addAll(addRootBtn, addUrlBtn, addJarDirBtn, remRootBtn);

            // TreeView with Classes, Sources, JavaDocs
            TreeItem<String> rootItem = new TreeItem<>("Root");
            rootItem.setExpanded(true);

            TreeItem<String> classesNode = new TreeItem<>("Classes");
            classesNode.setExpanded(true);
            for (String cp : selected.getClassesPaths()) {
                classesNode.getChildren().add(new TreeItem<>(cp));
            }

            TreeItem<String> sourcesNode = new TreeItem<>("Sources");
            sourcesNode.setExpanded(true);
            for (String sp : selected.getSourcesPaths()) {
                sourcesNode.getChildren().add(new TreeItem<>(sp));
            }

            TreeItem<String> javadocNode = new TreeItem<>("JavaDocs");
            javadocNode.setExpanded(true);
            for (String jp : selected.getJavadocPaths()) {
                javadocNode.getChildren().add(new TreeItem<>(jp));
            }

            rootItem.getChildren().addAll(classesNode, sourcesNode, javadocNode);

            TreeView<String> treeView = new TreeView<>(rootItem);
            treeView.setShowRoot(false);
            VBox.setVgrow(treeView, Priority.ALWAYS);
            treeView.getStyleClass().add("ps-file-tree");

            treeView.setCellFactory(tv -> new TreeCell<>() {
                @Override
                protected void updateItem(String val, boolean empty) {
                    super.updateItem(val, empty);
                    if (empty || val == null) {
                        setText(null);
                        setGraphic(null);
                        setStyle("");
                    } else {
                        if ("Classes".equals(val)) {
                            setText("Classes");
                            Label ic = new Label("🛠");
                            setGraphic(ic);
                            setStyle("-fx-text-fill: #EDEFF6; -fx-font-weight: bold;");
                        } else if ("Sources".equals(val)) {
                            setText("Sources");
                            Label ic = new Label("📁");
                            setGraphic(ic);
                            setStyle("-fx-text-fill: #EDEFF6; -fx-font-weight: bold;");
                        } else if ("JavaDocs".equals(val)) {
                            setText("JavaDocs");
                            Label ic = new Label("🌐");
                            setGraphic(ic);
                            setStyle("-fx-text-fill: #EDEFF6; -fx-font-weight: bold;");
                        } else {
                            setText(val);
                            boolean exists = Files.exists(Path.of(val)) || val.startsWith("http");
                            if (val.endsWith(".jar") || val.endsWith(".zip")) {
                                Label ic = new Label("🗄");
                                setGraphic(ic);
                            } else if (val.startsWith("http")) {
                                Label ic = new Label("🌐");
                                setGraphic(ic);
                            } else {
                                Label ic = new Label("📄");
                                setGraphic(ic);
                            }
                            if (!exists) {
                                setStyle("-fx-text-fill: #FF6B6B;");
                            } else {
                                setStyle("-fx-text-fill: #DFE1E5;");
                            }
                        }
                    }
                }
            });

            // Action: addRootBtn
            addRootBtn.setOnAction(e -> {
                FileChooser fc = new FileChooser();
                fc.setTitle("Attach Files or Directories");
                List<File> files = fc.showOpenMultipleDialog(outer.getScene().getWindow());
                if (files != null) {
                    for (File f : files) {
                        selected.getClassesPaths().add(f.getAbsolutePath());
                        classesNode.getChildren().add(new TreeItem<>(f.getAbsolutePath()));
                    }
                }
            });

            addUrlBtn.setOnAction(e -> {
                TextInputDialog tid = new TextInputDialog("https://");
                tid.setTitle("Attach URL");
                tid.setHeaderText("Enter documentation URL:");
                tid.showAndWait().ifPresent(u -> {
                    if (!u.isBlank()) {
                        selected.getJavadocPaths().add(u.trim());
                        javadocNode.getChildren().add(new TreeItem<>(u.trim()));
                    }
                });
            });

            addJarDirBtn.setOnAction(e -> {
                DirectoryChooser dc = new DirectoryChooser();
                dc.setTitle("Attach Jar Directory");
                File f = dc.showDialog(outer.getScene().getWindow());
                if (f != null) {
                    selected.getClassesPaths().add(f.getAbsolutePath());
                    classesNode.getChildren().add(new TreeItem<>(f.getAbsolutePath()));
                }
            });

            remRootBtn.setOnAction(e -> {
                TreeItem<String> selNode = treeView.getSelectionModel().getSelectedItem();
                if (selNode != null && selNode.getParent() != null && selNode.getParent() != rootItem) {
                    String val = selNode.getValue();
                    selected.getClassesPaths().remove(val);
                    selected.getSourcesPaths().remove(val);
                    selected.getJavadocPaths().remove(val);
                    selNode.getParent().getChildren().remove(selNode);
                }
            });

            right.getChildren().addAll(nameRow, detailTools, treeView);
        };

        libList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> refreshRightPanel.run());
        if (!libItems.isEmpty()) {
            libList.getSelectionModel().selectFirst();
        } else {
            refreshRightPanel.run();
        }

        // Action: add library
        addBtn.setOnAction(e -> {
            ContextMenu menu = new ContextMenu();
            MenuItem mJava = new MenuItem("Java...");
            mJava.setOnAction(ev -> {
                FileChooser fc = new FileChooser();
                fc.setTitle("Select Library JARs");
                List<File> files = fc.showOpenMultipleDialog(outer.getScene().getWindow());
                if (files != null && !files.isEmpty()) {
                    String libName = files.getFirst().getName().replace(".jar", "");
                    LibraryModel lm = new LibraryModel(libName);
                    for (File f : files) {
                        lm.getClassesPaths().add(f.getAbsolutePath());
                    }
                    model.getLibraries().add(lm);
                    libItems.add(lm);
                    libList.getSelectionModel().select(lm);
                }
            });
            MenuItem mMvn = new MenuItem("From Maven...");
            mMvn.setOnAction(ev -> {
                TextInputDialog tid = new TextInputDialog("groupId:artifactId:version");
                tid.setTitle("Download Library from Maven Repository");
                tid.setHeaderText("Enter Maven dependency coordinates:");
                tid.showAndWait().ifPresent(coord -> {
                    if (!coord.isBlank()) {
                        String[] parts = coord.trim().split(":");
                        String g = parts.length > 0 ? parts[0] : "";
                        String a = parts.length > 1 ? parts[1] : "";
                        String v = parts.length > 2 ? parts[2] : "";
                        LibraryModel lm = LibraryModel.createMavenLibrary(g, a, v);
                        model.getLibraries().add(lm);
                        libItems.add(lm);
                        libList.getSelectionModel().select(lm);
                    }
                });
            });
            menu.getItems().addAll(mJava, mMvn);
            menu.show(addBtn, Side.BOTTOM, 0, 0);
        });

        remBtn.setOnAction(e -> {
            LibraryModel sel = libList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                model.getLibraries().remove(sel);
                libItems.remove(sel);
            }
        });

        copyBtn.setOnAction(e -> {
            LibraryModel sel = libList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                LibraryModel copy = new LibraryModel(sel.getName() + " (Copy)");
                copy.getClassesPaths().addAll(sel.getClassesPaths());
                copy.getSourcesPaths().addAll(sel.getSourcesPaths());
                copy.getJavadocPaths().addAll(sel.getJavadocPaths());
                model.getLibraries().add(copy);
                libItems.add(copy);
                libList.getSelectionModel().select(copy);
            }
        });

        split.getItems().addAll(left, right);
        outer.getChildren().add(split);
        return outer;
    }

    // --------------------------------------------------- 4. FACETS PAGE

    private Node buildFacetsPage() {
        VBox outer = new VBox(12);

        SplitPane split = new SplitPane();
        split.setDividerPositions(0.28);
        VBox.setVgrow(split, Priority.ALWAYS);

        // --- Left Panel: Facets list ---
        VBox left = new VBox(6);
        left.setPadding(new Insets(4));

        HBox tools = new HBox(6);
        Button addBtn = new Button("+");
        addBtn.getStyleClass().add("ps-add-button");
        Button remBtn = new Button("-");
        remBtn.getStyleClass().add("ps-add-button");
        tools.getChildren().addAll(addBtn, remBtn);

        ObservableList<FacetModel> facetItems = FXCollections.observableArrayList(model.getFacets());
        ListView<FacetModel> facetList = new ListView<>(facetItems);
        VBox.setVgrow(facetList, Priority.ALWAYS);
        facetList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(FacetModel facet, boolean empty) {
                super.updateItem(facet, empty);
                if (empty || facet == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String mod = facet.getModuleName() != null ? " (" + facet.getModuleName() + ")" : "";
                    setText(facet.getName() + mod);
                    Label icon = new Label(FacetModel.getIconGlyph(facet.getType()));
                    icon.setStyle("-fx-font-size: 13px;");
                    setGraphic(icon);
                }
            }
        });

        left.getChildren().addAll(tools, facetList);

        // --- Right Panel: Details ---
        VBox right = new VBox(14);
        right.setPadding(new Insets(16));

        Runnable refreshRight = () -> {
            right.getChildren().clear();
            FacetModel selected = facetList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                // Exact matching media_1789874382402.png empty state!
                Label emptyLbl = new Label("Press the '+' button to add a new facet");
                emptyLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
                right.setAlignment(Pos.CENTER);
                right.getChildren().add(emptyLbl);
                return;
            }
            right.setAlignment(Pos.TOP_LEFT);

            // Facet name row
            HBox nameRow = new HBox(12);
            nameRow.setAlignment(Pos.CENTER_LEFT);
            Label nameLbl = formLabel("Name:");
            nameLbl.setPrefWidth(70);
            TextField nameField = new TextField(selected.getName());
            nameField.setPrefWidth(350);
            nameField.textProperty().addListener((obs, o, n) -> {
                selected.setName(n.trim());
                facetList.refresh();
            });
            nameRow.getChildren().addAll(nameLbl, nameField);

            // Module row
            HBox modRow = new HBox(12);
            modRow.setAlignment(Pos.CENTER_LEFT);
            Label modLbl = formLabel("Module:");
            modLbl.setPrefWidth(70);
            ComboBox<String> modCombo = new ComboBox<>(
                    FXCollections.observableArrayList(model.getModules().stream().map(ModuleModel::getName).toList())
            );
            modCombo.setValue(selected.getModuleName() != null ? selected.getModuleName() : model.getPrimaryModule().getName());
            modCombo.setOnAction(e -> {
                selected.setModuleName(modCombo.getValue());
                facetList.refresh();
            });
            modRow.getChildren().addAll(modLbl, modCombo);

            // Framework-specific settings
            VBox configBox = new VBox(10);
            configBox.setPadding(new Insets(10, 0, 0, 0));
            Label configTitle = new Label(selected.getType() + " Configuration Descriptors");
            configTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 12px;");

            TableView<Map.Entry<String, String>> cfgTable = new TableView<>();
            cfgTable.setPrefHeight(200);
            VBox.setVgrow(cfgTable, Priority.ALWAYS);

            TableColumn<Map.Entry<String, String>, String> keyCol = new TableColumn<>("Property");
            keyCol.setPrefWidth(180);
            keyCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getKey()));

            TableColumn<Map.Entry<String, String>, String> valCol = new TableColumn<>("Value");
            valCol.setPrefWidth(360);
            valCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getValue()));

            cfgTable.getColumns().addAll(keyCol, valCol);
            cfgTable.setPlaceholder(new Label("Default framework configuration active"));

            // Populate detected or default configuration entries
            if (selected.getConfiguration().isEmpty()) {
                if ("Spring".equalsIgnoreCase(selected.getType()) || "Spring Boot".equalsIgnoreCase(selected.getName())) {
                    selected.getConfiguration().put("Application Context", "Spring Boot auto-configured context");
                    selected.getConfiguration().put("Configuration File", "application.properties / application.yml");
                } else if ("Kotlin".equalsIgnoreCase(selected.getType())) {
                    selected.getConfiguration().put("Language Version", "2.1");
                    selected.getConfiguration().put("Target JVM", "21");
                } else if ("Python".equalsIgnoreCase(selected.getType())) {
                    selected.getConfiguration().put("Interpreter", "Project Python SDK");
                    selected.getConfiguration().put("Package Manager", "pip");
                } else if ("Web".equalsIgnoreCase(selected.getType())) {
                    selected.getConfiguration().put("Web Resource Directory", "src/main/webapp");
                    selected.getConfiguration().put("Descriptor", "WEB-INF/web.xml");
                } else if ("Hibernate".equalsIgnoreCase(selected.getType())) {
                    selected.getConfiguration().put("Configuration XML", "hibernate.cfg.xml");
                } else if ("JPA".equalsIgnoreCase(selected.getType())) {
                    selected.getConfiguration().put("Descriptor", "META-INF/persistence.xml");
                }
            }

            cfgTable.setItems(FXCollections.observableArrayList(selected.getConfiguration().entrySet()));
            configBox.getChildren().addAll(configTitle, cfgTable);

            right.getChildren().addAll(nameRow, modRow, configBox);
        };

        facetList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> refreshRight.run());
        if (!facetItems.isEmpty()) {
            facetList.getSelectionModel().selectFirst();
        } else {
            refreshRight.run();
        }

        // Popup menu matching media_1789874382402.png
        addBtn.setOnAction(e -> {
            ContextMenu menu = new ContextMenu();
            menu.getStyleClass().add("ps-facet-popup");

            // Header "Add"
            Label header = new Label("Add");
            header.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-padding: 4 8 4 8; -fx-font-size: 11px;");
            CustomMenuItem headerItem = new CustomMenuItem(header, false);
            menu.getItems().add(headerItem);

            String[][] facetTypes = {
                    {"Hibernate", "🧊"},
                    {"JavaEE Application", "🏢"},
                    {"JPA", "🗄"},
                    {"JRuby", "💎"},
                    {"JRuby on Rails", "🛤"},
                    {"Kotlin", "🔷"},
                    {"Python", "🐍"},
                    {"Spring", "🍃"},
                    {"Web", "🌐"}
            };

            for (String[] ft : facetTypes) {
                String typeName = ft[0];
                String glyph = ft[1];

                HBox itemRow = new HBox(8);
                itemRow.setAlignment(Pos.CENTER_LEFT);
                Label ic = new Label(glyph);
                ic.setStyle("-fx-font-size: 13px;");
                Label lbl = new Label(typeName);
                lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                itemRow.getChildren().addAll(ic, lbl);

                MenuItem mi = new MenuItem(null, itemRow);
                mi.setOnAction(ev -> {
                    String modName = model.getPrimaryModule().getName();
                    FacetModel newFacet = new FacetModel(typeName, typeName, modName);
                    model.getFacets().add(newFacet);
                    facetItems.add(newFacet);
                    facetList.getSelectionModel().select(newFacet);
                });
                menu.getItems().add(mi);
            }

            menu.show(addBtn, Side.BOTTOM, 0, 0);
        });

        remBtn.setOnAction(e -> {
            FacetModel sel = facetList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                model.getFacets().remove(sel);
                facetItems.remove(sel);
            }
        });

        split.getItems().addAll(left, right);
        outer.getChildren().add(split);
        return outer;
    }

    // --------------------------------------------------- 5. ARTIFACTS PAGE

    private Node buildArtifactsPage() {
        VBox outer = new VBox();

        SplitPane split = new SplitPane();
        split.setDividerPositions(0.28);
        VBox.setVgrow(split, Priority.ALWAYS);

        // Left Panel: Toolbar + List
        VBox left = new VBox(8);
        ObservableList<ArtifactModel> artifactItems = FXCollections.observableArrayList(model.getArtifacts());
        ListView<ArtifactModel> artifactList = new ListView<>(artifactItems);
        VBox.setVgrow(artifactList, Priority.ALWAYS);

        HBox tools = new HBox(6);
        Button addBtn = new Button("+");
        addBtn.getStyleClass().add("ps-add-button");
        Button remBtn = new Button("-");
        remBtn.getStyleClass().add("ps-add-button");
        Button dupBtn = new Button("📋");
        dupBtn.getStyleClass().add("ps-add-button");
        dupBtn.setTooltip(new Tooltip("Duplicate Artifact"));
        tools.getChildren().addAll(addBtn, remBtn, dupBtn);

        Label emptyListLbl = new Label("Nothing to show");
        emptyListLbl.getStyleClass().add("ps-hint");
        emptyListLbl.setStyle("-fx-text-fill: #848BA3; -fx-padding: 20;");
        artifactList.setPlaceholder(emptyListLbl);

        artifactList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ArtifactModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getName());
                    Label icon = new Label(ArtifactModel.getIconGlyph(item.getType()));
                    icon.setStyle("-fx-font-size: 13px;");
                    setGraphic(icon);
                }
            }
        });

        // Right details panel
        StackPane rightContainer = new StackPane();
        rightContainer.setPadding(new Insets(0, 0, 0, 12));

        Label placeholder = new Label("Select an artifact or press '+' to add a new artifact");
        placeholder.getStyleClass().add("ps-empty");

        VBox detailsBox = new VBox(10);
        TextField nameField = new TextField();
        TextField typeField = new TextField();
        typeField.setEditable(false);
        typeField.setStyle("-fx-opacity: 0.85;");

        TextField outputDirField = new TextField();
        HBox.setHgrow(outputDirField, Priority.ALWAYS);
        Button browseOutputDirBtn = new Button("…");
        browseOutputDirBtn.getStyleClass().add("dialog-secondary");
        HBox outputDirRow = new HBox(8, outputDirField, browseOutputDirBtn);
        outputDirRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox includeInBuildCb = new CheckBox("Include in project build");
        includeInBuildCb.setStyle("-fx-text-fill: #EDEFF6; -fx-font-size: 12px;");

        // Output Layout Section
        Label layoutTitle = new Label("Output Layout");
        layoutTitle.setStyle("-fx-text-fill: #EDEFF6; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8 0 2 0;");

        HBox layoutToolbar = new HBox(6);
        Button addLayoutBtn = new Button("+");
        addLayoutBtn.getStyleClass().add("ps-add-button");
        Button remLayoutBtn = new Button("-");
        remLayoutBtn.getStyleClass().add("ps-add-button");
        Button createDirBtn = new Button("📁");
        createDirBtn.getStyleClass().add("ps-add-button");
        createDirBtn.setTooltip(new Tooltip("Create Directory"));
        Button createArchiveBtn = new Button("📦");
        createArchiveBtn.getStyleClass().add("ps-add-button");
        createArchiveBtn.setTooltip(new Tooltip("Create Archive"));
        layoutToolbar.getChildren().addAll(addLayoutBtn, remLayoutBtn, createDirBtn, createArchiveBtn);

        TreeView<String> layoutTree = new TreeView<>();
        layoutTree.setShowRoot(true);
        VBox.setVgrow(layoutTree, Priority.ALWAYS);

        detailsBox.getChildren().addAll(
                new HBox(8, formLabel("Name:"), nameField),
                new HBox(8, formLabel("Type:"), typeField),
                new HBox(8, formLabel("Output directory:"), outputDirRow),
                includeInBuildCb,
                layoutTitle,
                layoutToolbar,
                layoutTree
        );

        Consumer<ArtifactModel> showArtifact = (art) -> {
            if (art == null) {
                rightContainer.getChildren().setAll(placeholder);
            } else {
                nameField.setText(art.getName());
                typeField.setText(art.getType());
                outputDirField.setText(art.getOutputPath() != null ? art.getOutputPath() : "");
                includeInBuildCb.setSelected(art.isIncludeInBuild());

                TreeItem<String> root = new TreeItem<>(art.getName() + (art.getType().toLowerCase().contains("jar") ? ".jar" : ""));
                root.setExpanded(true);
                Label rootIcon = new Label(ArtifactModel.getIconGlyph(art.getType()));
                root.setGraphic(rootIcon);

                ModuleModel primary = model.getPrimaryModule();
                String modName = primary != null ? primary.getName() : "lumina";
                TreeItem<String> modOutput = new TreeItem<>("'" + modName + "' compile output");
                modOutput.setGraphic(new Label("📁"));
                root.getChildren().add(modOutput);

                for (String elem : art.getOutputLayout()) {
                    if (!elem.equals(modOutput.getValue())) {
                        TreeItem<String> item = new TreeItem<>(elem);
                        item.setGraphic(new Label("📄"));
                        root.getChildren().add(item);
                    }
                }

                layoutTree.setRoot(root);
                rightContainer.getChildren().setAll(detailsBox);
            }
        };

        artifactList.getSelectionModel().selectedItemProperty().addListener((obs, old, n) -> showArtifact.accept(n));
        if (!artifactItems.isEmpty()) {
            artifactList.getSelectionModel().selectFirst();
        } else {
            showArtifact.accept(null);
        }

        // Action handlers
        nameField.textProperty().addListener((obs, old, n) -> {
            ArtifactModel sel = artifactList.getSelectionModel().getSelectedItem();
            if (sel != null && n != null) {
                sel.setName(n);
                artifactList.refresh();
            }
        });

        outputDirField.textProperty().addListener((obs, old, n) -> {
            ArtifactModel sel = artifactList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                sel.setOutputPath(n);
            }
        });

        browseOutputDirBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Artifact Output Directory");
            if (projectRoot != null) {
                File baseDir = projectRoot.resolve("out/artifacts").toFile();
                if (baseDir.exists()) dc.setInitialDirectory(baseDir);
                else dc.setInitialDirectory(projectRoot.toFile());
            }
            File chosen = dc.showDialog(stage);
            if (chosen != null) {
                outputDirField.setText(chosen.getAbsolutePath());
            }
        });

        includeInBuildCb.selectedProperty().addListener((obs, old, n) -> {
            ArtifactModel sel = artifactList.getSelectionModel().getSelectedItem();
            if (sel != null && n != null) {
                sel.setIncludeInBuild(n);
            }
        });

        remBtn.setOnAction(e -> {
            ArtifactModel sel = artifactList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                model.getArtifacts().remove(sel);
                artifactItems.remove(sel);
                if (artifactItems.isEmpty()) showArtifact.accept(null);
            }
        });

        dupBtn.setOnAction(e -> {
            ArtifactModel sel = artifactList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                ArtifactModel dup = sel.duplicate(sel.getName() + "2");
                model.getArtifacts().add(dup);
                artifactItems.add(dup);
                artifactList.getSelectionModel().select(dup);
            }
        });

        addBtn.setOnAction(e -> showAddArtifactMenu(addBtn, (newArt) -> {
            model.getArtifacts().add(newArt);
            artifactItems.add(newArt);
            artifactList.getSelectionModel().select(newArt);
        }));

        left.getChildren().addAll(tools, artifactList);
        split.getItems().addAll(left, rightContainer);
        outer.getChildren().add(split);
        return outer;
    }

    private void showAddArtifactMenu(Node anchor, Consumer<ArtifactModel> onCreated) {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("ps-facet-popup");

        ModuleModel primary = model.getPrimaryModule();
        String modName = primary != null ? primary.getName() : "app";
        Path baseOut = projectRoot != null ? projectRoot.resolve("out/artifacts") : Path.of("out/artifacts");

        // 1. JAR >
        Menu jarMenu = new Menu("JAR");
        jarMenu.setGraphic(new Label("📦"));
        MenuItem jarFromDeps = new MenuItem("From modules with dependencies…");
        jarFromDeps.setOnAction(e -> {
            String name = modName + ":jar";
            String out = baseOut.resolve(name.replace(':', '_')).toString();
            ArtifactModel art = new ArtifactModel(name, "JAR", out);
            art.getOutputLayout().add("'" + modName + "' compile output");
            onCreated.accept(art);
        });
        MenuItem jarEmpty = new MenuItem("Empty");
        jarEmpty.setOnAction(e -> {
            String name = modName + ":jar empty";
            String out = baseOut.resolve(name.replace(':', '_')).toString();
            ArtifactModel art = new ArtifactModel(name, "JAR", out);
            onCreated.accept(art);
        });
        jarMenu.getItems().addAll(jarFromDeps, jarEmpty);

        // 2. Run-time image (JLink)
        MenuItem jlink = new MenuItem("Run-time image (JLink)");
        jlink.setGraphic(new Label("🔷"));
        jlink.setOnAction(e -> {
            String name = modName + ":jlink";
            String out = baseOut.resolve(name.replace(':', '_')).toString();
            ArtifactModel art = new ArtifactModel(name, "Run-time image (JLink)", out);
            art.getOutputLayout().add("'" + modName + "' compile output");
            onCreated.accept(art);
        });

        // 3. JavaFX application >
        Menu javafxMenu = new Menu("JavaFX application");
        javafxMenu.setGraphic(new Label("🧩"));
        MenuItem fxFromMod = new MenuItem("From module…");
        fxFromMod.setOnAction(e -> {
            String name = modName + ":javafx";
            String out = baseOut.resolve(name.replace(':', '_')).toString();
            ArtifactModel art = new ArtifactModel(name, "JavaFX application", out);
            art.getOutputLayout().add("'" + modName + "' compile output");
            onCreated.accept(art);
        });
        MenuItem fxEmpty = new MenuItem("Empty");
        fxEmpty.setOnAction(e -> {
            String name = modName + ":javafx empty";
            String out = baseOut.resolve(name.replace(':', '_')).toString();
            ArtifactModel art = new ArtifactModel(name, "JavaFX application", out);
            onCreated.accept(art);
        });
        javafxMenu.getItems().addAll(fxFromMod, fxEmpty);

        // 4. Platform specific package >
        Menu pkgMenu = new Menu("Platform specific package");
        pkgMenu.setGraphic(new Label("💿"));
        MenuItem pkgAll = new MenuItem("All packages");
        pkgAll.setOnAction(e -> {
            String name = modName + ":package";
            String out = baseOut.resolve(name.replace(':', '_')).toString();
            onCreated.accept(new ArtifactModel(name, "Platform specific package", out));
        });
        MenuItem pkgDmg = new MenuItem("DMG");
        pkgDmg.setOnAction(e -> {
            String name = modName + ":dmg";
            String out = baseOut.resolve(name.replace(':', '_')).toString();
            onCreated.accept(new ArtifactModel(name, "Platform specific package (DMG)", out));
        });
        MenuItem pkgExe = new MenuItem("EXE/MSI");
        pkgExe.setOnAction(e -> {
            String name = modName + ":exe";
            String out = baseOut.resolve(name.replace(':', '_')).toString();
            onCreated.accept(new ArtifactModel(name, "Platform specific package (EXE)", out));
        });
        MenuItem pkgDeb = new MenuItem("DEB/RPM");
        pkgDeb.setOnAction(e -> {
            String name = modName + ":deb";
            String out = baseOut.resolve(name.replace(':', '_')).toString();
            onCreated.accept(new ArtifactModel(name, "Platform specific package (DEB)", out));
        });
        pkgMenu.getItems().addAll(pkgAll, pkgDmg, pkgExe, pkgDeb);

        // 5. JavaFX preloader
        MenuItem fxPreloader = new MenuItem("JavaFX preloader");
        fxPreloader.setGraphic(new Label("🧩"));
        fxPreloader.setOnAction(e -> {
            String name = modName + ":preloader";
            String out = baseOut.resolve(name.replace(':', '_')).toString();
            onCreated.accept(new ArtifactModel(name, "JavaFX preloader", out));
        });

        // 6. Web Application: Exploded
        MenuItem warExploded = new MenuItem("Web Application: Exploded");
        warExploded.setGraphic(new Label("🌐"));
        warExploded.setOnAction(e -> {
            String name = modName + ":war exploded";
            String out = baseOut.resolve(name.replace(':', '_')).toString();
            ArtifactModel art = new ArtifactModel(name, "Web Application: Exploded", out);
            art.getOutputLayout().add("'" + modName + "' compile output");
            onCreated.accept(art);
        });

        // 7. Web Application: Archive
        MenuItem warArchive = new MenuItem("Web Application: Archive");
        warArchive.setGraphic(new Label("🌐"));
        warArchive.setOnAction(e -> {
            String name = modName + ":war";
            String out = baseOut.resolve(name.replace(':', '_')).toString();
            ArtifactModel art = new ArtifactModel(name, "Web Application: Archive", out);
            art.getOutputLayout().add("'" + modName + "' compile output");
            onCreated.accept(art);
        });

        // 8. Java EE Application: Exploded
        MenuItem earExploded = new MenuItem("Java EE Application: Exploded");
        earExploded.setGraphic(new Label("☕"));
        earExploded.setOnAction(e -> {
            String name = modName + ":ear exploded";
            String out = baseOut.resolve(name.replace(':', '_')).toString();
            onCreated.accept(new ArtifactModel(name, "Java EE Application: Exploded", out));
        });

        // 9. Java EE Application: Archive
        MenuItem earArchive = new MenuItem("Java EE Application: Archive");
        earArchive.setGraphic(new Label("☕"));
        earArchive.setOnAction(e -> {
            String name = modName + ":ear";
            String out = baseOut.resolve(name.replace(':', '_')).toString();
            onCreated.accept(new ArtifactModel(name, "Java EE Application: Archive", out));
        });

        // 10. EJB Application: Exploded
        MenuItem ejbExploded = new MenuItem("EJB Application: Exploded");
        ejbExploded.setGraphic(new Label("☕"));
        ejbExploded.setOnAction(e -> {
            String name = modName + ":ejb exploded";
            String out = baseOut.resolve(name.replace(':', '_')).toString();
            onCreated.accept(new ArtifactModel(name, "EJB Application: Exploded", out));
        });

        // 11. EJB Application: Archive
        MenuItem ejbArchive = new MenuItem("EJB Application: Archive");
        ejbArchive.setGraphic(new Label("☕"));
        ejbArchive.setOnAction(e -> {
            String name = modName + ":ejb";
            String out = baseOut.resolve(name.replace(':', '_')).toString();
            onCreated.accept(new ArtifactModel(name, "EJB Application: Archive", out));
        });

        // 12. Other
        MenuItem other = new MenuItem("Other");
        other.setGraphic(new Label("📁"));
        other.setOnAction(e -> {
            String name = modName + ":artifact";
            String out = baseOut.resolve(name.replace(':', '_')).toString();
            onCreated.accept(new ArtifactModel(name, "Other", out));
        });

        menu.getItems().addAll(
                jarMenu, jlink, javafxMenu, pkgMenu, fxPreloader,
                warExploded, warArchive, earExploded, earArchive,
                ejbExploded, ejbArchive, other
        );

        menu.show(anchor, javafx.geometry.Side.RIGHT, 0, 0);
    }

    // --------------------------------------------------- 6. SDKS PAGE

    private Node buildSdksPage() {
        VBox outer = new VBox();

        SplitPane split = new SplitPane();
        split.setDividerPositions(0.28);
        VBox.setVgrow(split, Priority.ALWAYS);

        // Left: SDK list with + and -
        VBox left = new VBox(6);
        List<ProjectSdk.SdkItem> registeredSdks = ProjectSdk.loadRegisteredSdks();
        ObservableList<ProjectSdk.SdkItem> sdkItems = FXCollections.observableArrayList(registeredSdks);
        ListView<ProjectSdk.SdkItem> sdkList = new ListView<>(sdkItems);
        VBox.setVgrow(sdkList, Priority.ALWAYS);

        HBox tools = new HBox(6);
        Button addBtn = new Button("+");
        addBtn.getStyleClass().add("ps-add-button");
        addBtn.setOnAction(e -> showAddSdkMenu(addBtn, () -> {
            List<ProjectSdk.SdkItem> updated = ProjectSdk.loadRegisteredSdks();
            sdkItems.setAll(updated);
            if (!sdkItems.isEmpty()) {
                sdkList.getSelectionModel().selectLast();
            }
        }));

        Button remBtn = new Button("-");
        remBtn.getStyleClass().add("ps-add-button");
        remBtn.setOnAction(e -> {
            ProjectSdk.SdkItem sel = sdkList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                List<ProjectSdk.SdkItem> current = new ArrayList<>(ProjectSdk.loadRegisteredSdks());
                current.removeIf(item -> (item.id() != null && item.id().equals(sel.id())) || item.name().equals(sel.name()));
                ProjectSdk.saveRegisteredSdks(current);
                sdkItems.remove(sel);
                if (!sdkItems.isEmpty()) {
                    sdkList.getSelectionModel().selectFirst();
                }
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
                    Label icon = new Label("📁");
                    icon.setStyle("-fx-text-fill: #548AF7; -fx-font-size: 13px;");
                    setGraphic(icon);
                }
            }
        });

        left.getChildren().addAll(tools, sdkList);

        // Right: Selected SDK details
        VBox right = new VBox(10);
        right.setPadding(new Insets(0, 0, 0, 12));

        TextField nameField = new TextField();
        TextField homeField = new TextField();
        homeField.setEditable(false);
        homeField.setStyle("-fx-opacity: 0.85;");

        TabPane sdkTabs = new TabPane();
        sdkTabs.getStyleClass().add("ps-sdk-tabs");

        // 1. Classpath Tab
        Tab cpTab = new Tab("Classpath");
        cpTab.setClosable(false);
        VBox cpBox = new VBox(6);
        HBox cpTools = new HBox(6);
        Button addCpBtn = new Button("+");
        addCpBtn.getStyleClass().add("ps-add-button");
        Button remCpBtn = new Button("-");
        remCpBtn.getStyleClass().add("ps-add-button");
        cpTools.getChildren().addAll(addCpBtn, remCpBtn);
        ListView<String> cpList = new ListView<>();
        VBox.setVgrow(cpList, Priority.ALWAYS);
        cpList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    Label icon = new Label(item.endsWith(".jar") || item.endsWith(".jmod") ? "📦" : "📁");
                    icon.setStyle("-fx-text-fill: #548AF7; -fx-font-size: 12px;");
                    setGraphic(icon);
                }
            }
        });
        addCpBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select JAR or Module Archive");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archives (*.jar, *.jmod, *.zip)", "*.jar", "*.jmod", "*.zip"));
            File f = fc.showOpenDialog(stage);
            if (f != null) {
                cpList.getItems().add(f.getAbsolutePath());
            }
        });
        remCpBtn.setOnAction(e -> {
            String sel = cpList.getSelectionModel().getSelectedItem();
            if (sel != null) cpList.getItems().remove(sel);
        });
        cpBox.getChildren().addAll(cpTools, cpList);
        cpTab.setContent(cpBox);

        // 2. Sourcepath Tab
        Tab srcTab = new Tab("Sourcepath");
        srcTab.setClosable(false);
        VBox srcBox = new VBox(6);
        HBox srcTools = new HBox(6);
        Button addSrcBtn = new Button("+");
        addSrcBtn.getStyleClass().add("ps-add-button");
        Button remSrcBtn = new Button("-");
        remSrcBtn.getStyleClass().add("ps-add-button");
        srcTools.getChildren().addAll(addSrcBtn, remSrcBtn);
        ListView<String> srcList = new ListView<>();
        VBox.setVgrow(srcList, Priority.ALWAYS);
        srcList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    Label icon = new Label("🔷");
                    icon.setStyle("-fx-text-fill: #548AF7; -fx-font-size: 12px;");
                    setGraphic(icon);
                }
            }
        });
        addSrcBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Source Archive or Directory");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Source Archives (*.zip, *.jar)", "*.zip", "*.jar"));
            File f = fc.showOpenDialog(stage);
            if (f != null) {
                srcList.getItems().add(f.getAbsolutePath());
            }
        });
        remSrcBtn.setOnAction(e -> {
            String sel = srcList.getSelectionModel().getSelectedItem();
            if (sel != null) srcList.getItems().remove(sel);
        });
        srcBox.getChildren().addAll(srcTools, srcList);
        srcTab.setContent(srcBox);

        // 3. Annotations Tab
        Tab annTab = new Tab("Annotations");
        annTab.setClosable(false);
        VBox annBox = new VBox(6);
        HBox annTools = new HBox(6);
        Button addAnnBtn = new Button("+");
        addAnnBtn.getStyleClass().add("ps-add-button");
        Button remAnnBtn = new Button("-");
        remAnnBtn.getStyleClass().add("ps-add-button");
        annTools.getChildren().addAll(addAnnBtn, remAnnBtn);
        ListView<String> annList = new ListView<>();
        VBox.setVgrow(annList, Priority.ALWAYS);
        annList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    Label icon = new Label("📦");
                    icon.setStyle("-fx-text-fill: #548AF7; -fx-font-size: 12px;");
                    setGraphic(icon);
                }
            }
        });
        addAnnBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Annotations Archive");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JAR Archives (*.jar)", "*.jar"));
            File f = fc.showOpenDialog(stage);
            if (f != null) {
                annList.getItems().add(f.getAbsolutePath());
            }
        });
        remAnnBtn.setOnAction(e -> {
            String sel = annList.getSelectionModel().getSelectedItem();
            if (sel != null) annList.getItems().remove(sel);
        });
        annBox.getChildren().addAll(annTools, annList);
        annTab.setContent(annBox);

        // 4. Documentation Paths Tab matching media_1789875550854.png
        Tab docTab = new Tab("Documentation Paths");
        docTab.setClosable(false);
        VBox docBox = new VBox(6);
        HBox docTools = new HBox(6);
        Button addDocBtn = new Button("+");
        addDocBtn.getStyleClass().add("ps-add-button");
        addDocBtn.setTooltip(new Tooltip("Attach local directory or archive"));
        Button remDocBtn = new Button("-");
        remDocBtn.getStyleClass().add("ps-add-button");
        remDocBtn.setTooltip(new Tooltip("Remove"));
        Button addDocUrlBtn = new Button("🌐");
        addDocUrlBtn.getStyleClass().add("ps-add-button");
        addDocUrlBtn.setTooltip(new Tooltip("Specify Documentation URL"));
        docTools.getChildren().addAll(addDocBtn, remDocBtn, addDocUrlBtn);

        ListView<String> docList = new ListView<>();
        VBox.setVgrow(docList, Priority.ALWAYS);
        Label docEmpty = new Label("Nothing to show");
        docEmpty.getStyleClass().add("ps-hint");
        docEmpty.setStyle("-fx-text-fill: #848BA3; -fx-padding: 20;");
        docList.setPlaceholder(docEmpty);

        docList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    Label icon = new Label("🌐");
                    icon.setStyle("-fx-text-fill: #548AF7; -fx-font-size: 12px;");
                    setGraphic(icon);
                }
            }
        });

        addDocBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Local Documentation Directory");
            File f = dc.showDialog(stage);
            if (f != null) {
                docList.getItems().add(f.getAbsolutePath());
            }
        });

        remDocBtn.setOnAction(e -> {
            String sel = docList.getSelectionModel().getSelectedItem();
            if (sel != null) docList.getItems().remove(sel);
        });

        addDocUrlBtn.setOnAction(e -> {
            ProjectSdk.SdkItem sel = sdkList.getSelectionModel().getSelectedItem();
            String defaultUrl = sel != null ? ProjectSdk.resolveStandardJdkDocUrl(sel.version()) : "https://docs.oracle.com/en/java/javase/21/docs/api/";
            TextInputDialog dialog = new TextInputDialog(defaultUrl);
            dialog.setTitle("Specify Documentation URL");
            dialog.setHeaderText("Enter URL to documentation:");
            dialog.showAndWait().ifPresent(url -> {
                if (!url.isBlank()) docList.getItems().add(url.trim());
            });
        });

        docBox.getChildren().addAll(docTools, docList);
        docTab.setContent(docBox);

        sdkTabs.getTabs().addAll(cpTab, srcTab, annTab, docTab);
        VBox.setVgrow(sdkTabs, Priority.ALWAYS);

        Consumer<ProjectSdk.SdkItem> updateRight = (sel) -> {
            if (sel != null) {
                nameField.setText(sel.name());
                homeField.setText(sel.homePath() != null ? sel.homePath() : "");
                cpList.getItems().setAll(sel.classpathEntries());
                srcList.getItems().setAll(sel.sourcepathEntries());
                annList.getItems().setAll(sel.annotationsEntries());
                docList.getItems().setAll(sel.documentationPaths());
            } else {
                nameField.setText("");
                homeField.setText("");
                cpList.getItems().clear();
                srcList.getItems().clear();
                annList.getItems().clear();
                docList.getItems().clear();
            }
        };

        sdkList.getSelectionModel().selectedItemProperty().addListener((obs, old, n) -> updateRight.accept(n));
        if (!sdkItems.isEmpty()) {
            sdkList.getSelectionModel().selectFirst();
        } else {
            updateRight.accept(null);
        }

        // Renaming support
        nameField.setOnAction(e -> {
            ProjectSdk.SdkItem sel = sdkList.getSelectionModel().getSelectedItem();
            String newName = nameField.getText().trim();
            if (sel != null && !newName.isBlank() && !newName.equals(sel.name())) {
                List<ProjectSdk.SdkItem> current = new ArrayList<>(ProjectSdk.loadRegisteredSdks());
                for (int i = 0; i < current.size(); i++) {
                    if (current.get(i).id().equals(sel.id()) || current.get(i).name().equals(sel.name())) {
                        current.set(i, new ProjectSdk.SdkItem(
                                sel.id(), newName, sel.type(), sel.homePath(), sel.version(),
                                true, false, sel.classpathEntries(), sel.sourcepathEntries(),
                                sel.annotationsEntries(), sel.documentationPaths()));
                        break;
                    }
                }
                ProjectSdk.saveRegisteredSdks(current);
                sdkItems.setAll(current);
                sdkList.getSelectionModel().select(current.stream().filter(s -> s.name().equals(newName)).findFirst().orElse(null));
            }
        });

        right.getChildren().addAll(
                new HBox(8, formLabel("Name:"), nameField),
                new HBox(8, formLabel("JDK home path:"), homeField),
                sdkTabs);

        split.getItems().addAll(left, right);
        outer.getChildren().add(split);
        return outer;
    }

    // --------------------------------------------------- 7. GLOBAL LIBRARIES PAGE

    private Node buildGlobalLibrariesPage() {
        VBox outer = new VBox();

        SplitPane split = new SplitPane();
        split.setDividerPositions(0.28);
        VBox.setVgrow(split, Priority.ALWAYS);

        // Left Panel: Toolbar + List
        VBox left = new VBox(6);
        left.setPadding(new Insets(4));
        ObservableList<LibraryModel> globalLibItems = FXCollections.observableArrayList(GlobalLibraries.load());
        ListView<LibraryModel> globalList = new ListView<>(globalLibItems);
        VBox.setVgrow(globalList, Priority.ALWAYS);

        Label emptyPlaceholder = new Label("Nothing to show");
        emptyPlaceholder.getStyleClass().add("ps-hint");
        emptyPlaceholder.setStyle("-fx-text-fill: #848BA3; -fx-padding: 20;");
        globalList.setPlaceholder(emptyPlaceholder);

        HBox tools = new HBox(6);
        Button addBtn = new Button("+");
        addBtn.getStyleClass().add("ps-add-button");
        Button remBtn = new Button("-");
        remBtn.getStyleClass().add("ps-add-button");
        Button copyBtn = new Button("📋");
        copyBtn.getStyleClass().add("ps-add-button");
        copyBtn.setTooltip(new Tooltip("Duplicate Global Library"));
        tools.getChildren().addAll(addBtn, remBtn, copyBtn);

        globalList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(LibraryModel lib, boolean empty) {
                super.updateItem(lib, empty);
                if (empty || lib == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(lib.getName());
                    Label icon = new Label(LibraryModel.getIconGlyph(lib.getName()));
                    icon.setStyle("-fx-font-size: 13px;");
                    setGraphic(icon);
                }
            }
        });

        // + Popup menu titled "New Global Library" matching media_1789875551490.png
        addBtn.setOnAction(e -> {
            ContextMenu menu = new ContextMenu();
            menu.getStyleClass().add("ps-facet-popup");

            CustomMenuItem header = new CustomMenuItem();
            Label headerLbl = new Label("New Global Library");
            headerLbl.setStyle("-fx-text-fill: #EDEFF6; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 8;");
            header.setContent(headerLbl);
            header.setHideOnClick(false);

            MenuItem javaItem = new MenuItem("Java");
            javaItem.setGraphic(new Label("📚"));
            javaItem.setOnAction(ev -> {
                FileChooser fc = new FileChooser();
                fc.setTitle("Select JAR or ZIP Archives for Global Library");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java Archives (*.jar, *.zip)", "*.jar", "*.zip"));
                List<File> files = fc.showOpenMultipleDialog(stage);
                if (files != null && !files.isEmpty()) {
                    String name = files.getFirst().getName().replaceAll("\\.(jar|zip)$", "");
                    LibraryModel lib = new LibraryModel(name);
                    for (File f : files) {
                        lib.getClassesPaths().add(f.getAbsolutePath());
                    }
                    GlobalLibraries.add(lib);
                    globalLibItems.setAll(GlobalLibraries.load());
                    globalList.getSelectionModel().select(globalLibItems.stream().filter(l -> l.getName().equals(name)).findFirst().orElse(null));
                }
            });

            MenuItem mavenItem = new MenuItem("From Maven…");
            mavenItem.setGraphic(new Label("Ⓜ"));
            mavenItem.setOnAction(ev -> {
                TextInputDialog dialog = new TextInputDialog("com.google.code.gson:gson:2.10.1");
                dialog.setTitle("Download Library from Maven Repository");
                dialog.setHeaderText("Enter Maven Coordinates (groupId:artifactId:version):");
                dialog.showAndWait().ifPresent(coords -> {
                    String[] parts = coords.trim().split(":");
                    if (parts.length >= 3) {
                        LibraryModel lib = parts.length == 4
                                ? LibraryModel.createMavenLibrary(parts[0], parts[1], parts[2], parts[3])
                                : LibraryModel.createMavenLibrary(parts[0], parts[1], parts[2]);
                        GlobalLibraries.add(lib);
                        globalLibItems.setAll(GlobalLibraries.load());
                        globalList.getSelectionModel().select(globalLibItems.stream().filter(l -> l.getName().equals(lib.getName())).findFirst().orElse(null));
                    }
                });
            });

            MenuItem kotlinJsItem = new MenuItem("Kotlin/JS");
            kotlinJsItem.setGraphic(new Label("🟪"));
            kotlinJsItem.setOnAction(ev -> {
                String name = "Kotlin/JS";
                LibraryModel lib = new LibraryModel(name);
                String home = System.getProperty("user.home");
                Path m2 = Path.of(home, ".m2/repository/org/jetbrains/kotlin/kotlin-stdlib-js/2.1.0/kotlin-stdlib-js-2.1.0.jar");
                if (Files.isRegularFile(m2)) {
                    lib.getClassesPaths().add(m2.toString());
                }
                GlobalLibraries.add(lib);
                globalLibItems.setAll(GlobalLibraries.load());
                globalList.getSelectionModel().select(globalLibItems.stream().filter(l -> l.getName().equals(name)).findFirst().orElse(null));
            });

            MenuItem scalaItem = new MenuItem("Scala SDK");
            scalaItem.setGraphic(new Label("🔴"));
            scalaItem.setOnAction(ev -> {
                String name = "scala-sdk-3.3.3";
                LibraryModel lib = new LibraryModel(name);
                GlobalLibraries.add(lib);
                globalLibItems.setAll(GlobalLibraries.load());
                globalList.getSelectionModel().select(globalLibItems.stream().filter(l -> l.getName().equals(name)).findFirst().orElse(null));
            });

            menu.getItems().addAll(header, javaItem, mavenItem, kotlinJsItem, scalaItem);
            menu.show(addBtn, Side.RIGHT, 0, 0);
        });

        remBtn.setOnAction(e -> {
            LibraryModel sel = globalList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                GlobalLibraries.remove(sel);
                globalLibItems.remove(sel);
            }
        });

        copyBtn.setOnAction(e -> {
            LibraryModel sel = globalList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                LibraryModel dup = sel.duplicate(sel.getName() + "2");
                GlobalLibraries.add(dup);
                globalLibItems.setAll(GlobalLibraries.load());
                globalList.getSelectionModel().select(globalLibItems.stream().filter(l -> l.getName().equals(dup.getName())).findFirst().orElse(null));
            }
        });

        left.getChildren().addAll(tools, globalList);

        // Right Detail Panel matching media_1789875551490.png
        StackPane rightContainer = new StackPane();
        rightContainer.setPadding(new Insets(4, 8, 8, 12));

        Label emptyDetail = new Label("Select a library to view or edit its details here");
        emptyDetail.getStyleClass().add("ps-empty");

        VBox detailsBox = new VBox(10);

        Consumer<LibraryModel> showLibDetails = (selected) -> {
            if (selected == null) {
                rightContainer.getChildren().setAll(emptyDetail);
            } else {
                detailsBox.getChildren().clear();

                HBox nameRow = new HBox(8);
                nameRow.setAlignment(Pos.CENTER_LEFT);
                Label nameLbl = formLabel("Name:");
                nameLbl.setPrefWidth(60);
                TextField nameField = new TextField(selected.getName());
                nameField.setPrefWidth(500);
                HBox.setHgrow(nameField, Priority.ALWAYS);
                nameField.textProperty().addListener((obs, o, n) -> {
                    selected.setName(n.trim());
                    globalList.refresh();
                    GlobalLibraries.save(new ArrayList<>(globalLibItems));
                });
                nameRow.getChildren().addAll(nameLbl, nameField);

                HBox detailTools = new HBox(6);
                Button addRootBtn = new Button("+");
                addRootBtn.getStyleClass().add("ps-add-button");
                Button addUrlBtn = new Button("🌐");
                addUrlBtn.getStyleClass().add("ps-add-button");
                Button addJarDirBtn = new Button("📁");
                addJarDirBtn.getStyleClass().add("ps-add-button");
                Button remRootBtn = new Button("-");
                remRootBtn.getStyleClass().add("ps-add-button");
                detailTools.getChildren().addAll(addRootBtn, addUrlBtn, addJarDirBtn, remRootBtn);

                TreeView<String> treeView = new TreeView<>();
                treeView.setShowRoot(false);
                TreeItem<String> rootItem = new TreeItem<>("Root");
                rootItem.setExpanded(true);

                TreeItem<String> classesNode = new TreeItem<>("Classes");
                classesNode.setExpanded(true);
                for (String cp : selected.getClassesPaths()) {
                    classesNode.getChildren().add(new TreeItem<>(cp));
                }

                TreeItem<String> sourcesNode = new TreeItem<>("Sources");
                sourcesNode.setExpanded(true);
                for (String sp : selected.getSourcesPaths()) {
                    sourcesNode.getChildren().add(new TreeItem<>(sp));
                }

                TreeItem<String> javadocNode = new TreeItem<>("JavaDocs");
                javadocNode.setExpanded(true);
                for (String jp : selected.getJavadocPaths()) {
                    javadocNode.getChildren().add(new TreeItem<>(jp));
                }

                rootItem.getChildren().addAll(classesNode, sourcesNode, javadocNode);
                treeView.setRoot(rootItem);
                VBox.setVgrow(treeView, Priority.ALWAYS);

                addRootBtn.setOnAction(ev -> {
                    FileChooser fc = new FileChooser();
                    fc.setTitle("Attach Files");
                    List<File> files = fc.showOpenMultipleDialog(stage);
                    if (files != null) {
                        for (File f : files) {
                            selected.getClassesPaths().add(f.getAbsolutePath());
                            classesNode.getChildren().add(new TreeItem<>(f.getAbsolutePath()));
                        }
                        GlobalLibraries.save(new ArrayList<>(globalLibItems));
                    }
                });

                addJarDirBtn.setOnAction(ev -> {
                    DirectoryChooser dc = new DirectoryChooser();
                    dc.setTitle("Attach Directory");
                    File d = dc.showDialog(stage);
                    if (d != null) {
                        selected.getClassesPaths().add(d.getAbsolutePath());
                        classesNode.getChildren().add(new TreeItem<>(d.getAbsolutePath()));
                        GlobalLibraries.save(new ArrayList<>(globalLibItems));
                    }
                });

                addUrlBtn.setOnAction(ev -> {
                    TextInputDialog td = new TextInputDialog("https://");
                    td.setTitle("Attach JavaDoc URL");
                    td.setHeaderText("Enter URL:");
                    td.showAndWait().ifPresent(u -> {
                        if (!u.isBlank()) {
                            selected.getJavadocPaths().add(u.trim());
                            javadocNode.getChildren().add(new TreeItem<>(u.trim()));
                            GlobalLibraries.save(new ArrayList<>(globalLibItems));
                        }
                    });
                });

                remRootBtn.setOnAction(ev -> {
                    TreeItem<String> selTree = treeView.getSelectionModel().getSelectedItem();
                    if (selTree != null && selTree.getParent() != null && selTree.getParent() != rootItem) {
                        String val = selTree.getValue();
                        String parent = selTree.getParent().getValue();
                        if ("Classes".equals(parent)) selected.getClassesPaths().remove(val);
                        else if ("Sources".equals(parent)) selected.getSourcesPaths().remove(val);
                        else if ("JavaDocs".equals(parent)) selected.getJavadocPaths().remove(val);
                        selTree.getParent().getChildren().remove(selTree);
                        GlobalLibraries.save(new ArrayList<>(globalLibItems));
                    }
                });

                detailsBox.getChildren().addAll(nameRow, detailTools, treeView);
                rightContainer.getChildren().setAll(detailsBox);
            }
        };

        globalList.getSelectionModel().selectedItemProperty().addListener((obs, old, n) -> showLibDetails.accept(n));
        if (!globalLibItems.isEmpty()) {
            globalList.getSelectionModel().selectFirst();
        } else {
            showLibDetails.accept(null);
        }

        split.getItems().addAll(left, rightContainer);
        outer.getChildren().add(split);
        return outer;
    }

    // --------------------------------------------------- 8. PROBLEMS PAGE

    private Node buildProblemsPage() {
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
            // Clean empty container matching IntelliJ IDEA media_1789875550764.png
            VBox emptyBox = new VBox();
            emptyBox.setMinHeight(200);
            return emptyBox;
        }

        VBox box = new VBox(12);
        box.setPadding(new Insets(16, 20, 16, 20));
        box.getChildren().addAll(problemRows);
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
