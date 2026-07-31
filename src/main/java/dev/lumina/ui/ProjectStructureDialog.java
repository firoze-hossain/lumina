package dev.lumina.ui;

import dev.lumina.util.Settings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * IntelliJ-style Project Structure dialog.
 * Left: category tree. Right: detailed configuration panels.
 */
public class ProjectStructureDialog {

    private final Stage stage;
    private final ProjectStructurePage currentPage = new ProjectStructurePage();
    private final String projectName;
    private final Path projectRoot;

    public ProjectStructureDialog(Stage owner, String projectName, Path projectRoot) {
        this.stage = new Stage();
        this.projectName = projectName;
        this.projectRoot = projectRoot;

        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.DECORATED);
        stage.setTitle("Project Structure");

        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("app-root", "project-structure-dialog");

        // ---- Left: category tree ----
        TreeView<String> tree = buildCategoryTree();
        tree.setPrefWidth(240);
        tree.setMinWidth(220);
        tree.getStyleClass().add("ps-tree");

        // ---- Right: content panel ----
        currentPage.getStyleClass().add("ps-page");

        // ---- Bottom: buttons ----
        HBox buttons = buildButtonBar();

        root.setLeft(tree);
        root.setCenter(currentPage);
        root.setBottom(buttons);

        // Initial selection: Project
        TreeItem<String> projectItem = findItem(tree.getRoot(), "Project");
        if (projectItem != null) {
            tree.getSelectionModel().select(projectItem);
        }

        // When tree selection changes, update the page
        tree.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                currentPage.showPage(selected.getValue(), projectName, projectRoot);
            }
        });

        Scene scene = new Scene(root, 960, 660);
        scene.getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
        stage.setScene(scene);
    }

    public void show() {
        stage.showAndWait();
    }

    // --------------------------------------------------- category tree

    private TreeView<String> buildCategoryTree() {
        TreeItem<String> root = new TreeItem<>("Project Structure");
        root.setExpanded(true);

        // Project Settings
        TreeItem<String> projectSettings = new TreeItem<>("Project Settings");
        TreeItem<String> project = new TreeItem<>("Project");
        TreeItem<String> modules = new TreeItem<>("Modules");
        TreeItem<String> libraries = new TreeItem<>("Libraries");
        TreeItem<String> facets = new TreeItem<>("Facets");
        TreeItem<String> artifacts = new TreeItem<>("Artifacts");
        projectSettings.getChildren().addAll(project, modules, libraries, facets, artifacts);

        // Platform Settings
        TreeItem<String> platformSettings = new TreeItem<>("Platform Settings");
        TreeItem<String> sdks = new TreeItem<>("SDKs");
        TreeItem<String> globalLibraries = new TreeItem<>("Global Libraries");
        platformSettings.getChildren().addAll(sdks, globalLibraries);

        // Problems
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
                    return;
                }
                setText(item);
                if ("Project Settings".equals(item) || "Platform Settings".equals(item)) {
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #A6ADC4; -fx-font-size: 11px;");
                } else {
                    setStyle(null);
                }
            }
        });

        return tree;
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

    // --------------------------------------------------- button bar

    private HBox buildButtonBar() {
        Button ok = new Button("OK");
        ok.getStyleClass().add("dialog-primary");
        ok.setDefaultButton(true);
        ok.setOnAction(e -> {
            currentPage.save();
            stage.close();
        });

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("dialog-secondary");
        cancel.setOnAction(e -> stage.close());

        Button apply = new Button("Apply");
        apply.getStyleClass().add("dialog-secondary");
        apply.setOnAction(e -> currentPage.save());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(10, spacer, apply, cancel, ok);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPadding(new Insets(10, 20, 14, 20));
        bar.getStyleClass().add("dialog-footer");
        return bar;
    }

    // --------------------------------------------------- project structure page

    /**
     * The right-hand panel that shows different pages based on selection.
     */
    private static class ProjectStructurePage extends VBox {

        private VBox contentArea;
        private String currentPageName;

        public ProjectStructurePage() {
            getStyleClass().add("ps-page");
            setPadding(new Insets(16, 20, 16, 20));
            setSpacing(12);
        }

        public void showPage(String pageName, String projectName, Path projectRoot) {
            this.currentPageName = pageName;
            getChildren().clear();

            switch (pageName) {
                case "Project" -> buildProjectPage(projectName, projectRoot);
                case "Modules" -> buildModulesPage(projectName, projectRoot);
                case "Libraries" -> buildLibrariesPage(projectName, projectRoot);
                case "Facets" -> buildFacetsPage(projectName, projectRoot);
                case "Artifacts" -> buildArtifactsPage(projectName, projectRoot);
                case "SDKs" -> buildSdksPage(projectName, projectRoot);
                case "Global Libraries" -> buildGlobalLibrariesPage(projectName, projectRoot);
                case "Problems" -> buildProblemsPage(projectName, projectRoot);
                default -> buildPlaceholderPage(pageName);
            }
        }

        public void save() {
            // Save settings
            System.out.println("Saving project structure settings...");
        }

        // --------------------------------------------------- Project page

        private void buildProjectPage(String projectName, Path projectRoot) {
            Label title = new Label("Project");
            title.getStyleClass().add("ps-page-title");

            Label subtitle = new Label("Default settings for all modules. Configure these parameters for each module on the module page as needed.");
            subtitle.getStyleClass().add("ps-subtitle");
            subtitle.setWrapText(true);

            GridPane grid = new GridPane();
            grid.setHgap(14);
            grid.setVgap(12);
            grid.setPadding(new Insets(12, 0, 12, 0));

            // Name
            grid.add(new Label("Name:"), 0, 0);
            TextField nameField = new TextField(projectName != null ? projectName : "untitled");
            nameField.getStyleClass().add("text-field");
            nameField.setPrefWidth(300);
            grid.add(nameField, 1, 0);

            // SDK
            grid.add(new Label("SDK:"), 0, 1);
            ComboBox<String> sdkCombo = new ComboBox<>();
            sdkCombo.getItems().addAll(
                    "25 Oracle OpenJDK 25.0.2",
                    "21 Alibaba Dragonwell 21.0.10",
                    "dragonwell-ex-21 Alibaba Dragonwell 21.0.10"
            );
            sdkCombo.getSelectionModel().selectFirst();
            sdkCombo.getStyleClass().add("settings-combo");
            sdkCombo.setPrefWidth(300);
            HBox sdkRow = new HBox(8, sdkCombo, new Button("Edit"));
            sdkRow.setAlignment(Pos.CENTER_LEFT);
            grid.add(sdkRow, 1, 1);

            // Language level
            grid.add(new Label("Language level:"), 0, 2);
            ComboBox<String> langCombo = new ComboBox<>();
            langCombo.getItems().addAll(
                    "25 - Compact source files, module imports",
                    "25 (Preview) - Primitive Types in Patterns, etc.",
                    "24 - Stream gatherers",
                    "24 (Preview) - Flexible constructor bodies, simple source files, etc.",
                    "23 - Markdown documentation comments",
                    "23 (Preview) - Primitive types in patterns, implicitly declared classes, etc.",
                    "22 - Unnamed variables and patterns",
                    "22 (Preview) - Statements before super(), string templates (2nd preview), etc.",
                    "21 - Record patterns, pattern matching for switch",
                    "21 (Preview) - String templates, unnamed classes and instance main methods, etc."
            );
            langCombo.getSelectionModel().selectFirst();
            langCombo.getStyleClass().add("settings-combo");
            langCombo.setPrefWidth(400);
            grid.add(langCombo, 1, 2);

            // Compiler output
            grid.add(new Label("Compiler output:"), 0, 3);
            TextField outputField = new TextField(projectRoot != null ? projectRoot.resolve("target/classes").toString() : "");
            outputField.getStyleClass().add("text-field");
            outputField.setPrefWidth(400);
            grid.add(outputField, 1, 3);

            VBox content = new VBox(8, title, subtitle, grid);
            content.setPadding(new Insets(0, 0, 12, 0));

            ScrollPane scroll = wrapInScroll(content);
            getChildren().addAll(scroll);
        }

        // --------------------------------------------------- Modules page

        private void buildModulesPage(String projectName, Path projectRoot) {
            Label title = new Label("Modules");
            title.getStyleClass().add("ps-page-title");

            Label moduleName = new Label(projectName != null ? projectName : "untitled");
            moduleName.getStyleClass().add("ps-module-title");

            // ---- Sources tab ----
            Label sourcesLabel = new Label("Sources");
            sourcesLabel.getStyleClass().add("ps-section-title");

            // Language level
            HBox langRow = new HBox(8);
            langRow.setAlignment(Pos.CENTER_LEFT);
            Label langLabel = new Label("Language level:");
            langLabel.getStyleClass().add("ps-label");
            ComboBox<String> langCombo = new ComboBox<>();
            langCombo.getItems().addAll("25 - Compact source files, module imports");
            langCombo.getSelectionModel().selectFirst();
            langCombo.getStyleClass().add("settings-combo");
            langRow.getChildren().addAll(langLabel, langCombo);

            // Mark as buttons
            Label markLabel = new Label("Mark as:");
            markLabel.getStyleClass().add("ps-label");
            HBox markBox = new HBox(6);
            markBox.setAlignment(Pos.CENTER_LEFT);
            String[] marks = {"Sources", "Tests", "Resources", "Test Resources", "Excluded"};
            ToggleGroup markGroup = new ToggleGroup();
            for (String m : marks) {
                ToggleButton tb = new ToggleButton(m);
                tb.setToggleGroup(markGroup);
                tb.getStyleClass().addAll("segment", "segment-first");
                if (m.equals("Sources")) tb.setSelected(true);
                markBox.getChildren().add(tb);
            }

            // ---- File tree view ----
            TreeView<String> fileTree = new TreeView<>();
            fileTree.getStyleClass().add("ps-file-tree");
            TreeItem<String> rootItem = new TreeItem<>(projectRoot != null ? projectRoot.toString() : "/");
            rootItem.setExpanded(true);
            // Add some sample items
            TreeItem<String> idea = new TreeItem<>(".idea");
            TreeItem<String> src = new TreeItem<>(".src");
            TreeItem<String> target = new TreeItem<>(".target");
            rootItem.getChildren().addAll(idea, src, target);
            fileTree.setRoot(rootItem);
            fileTree.setShowRoot(true);
            fileTree.setPrefHeight(180);

            // ---- Bottom section ----
            HBox addContentRow = new HBox(8);
            addContentRow.setAlignment(Pos.CENTER_LEFT);
            Button addContent = new Button("Add Content Root");
            addContent.getStyleClass().add("dialog-secondary");
            TextField contentPath = new TextField(projectRoot != null ? projectRoot.toString() : "");
            contentPath.getStyleClass().add("text-field");
            contentPath.setPrefWidth(300);
            HBox.setHgrow(contentPath, Priority.ALWAYS);
            addContentRow.getChildren().addAll(addContent, contentPath);

            // ---- Folder lists ----
            GridPane folders = new GridPane();
            folders.setHgap(16);
            folders.setVgap(6);
            folders.setPadding(new Insets(8, 0, 0, 0));

            String[][] folderData = {
                    {"Source Folders:", "src/main/java"},
                    {"Test Source Folders:", "src/test/java"},
                    {"Resource Folders:", "src/main/resources"},
                    {"Test Resource Folders:", "src/test/resources"},
                    {"Excluded Folders:", "target"}
            };

            for (int i = 0; i < folderData.length; i++) {
                Label label = new Label(folderData[i][0]);
                label.getStyleClass().add("ps-label");
                Label value = new Label(folderData[i][1]);
                value.getStyleClass().add("ps-folder-value");
                folders.add(label, 0, i);
                folders.add(value, 1, i);
            }

            // ---- Exclude files ----
            Label excludeLabel = new Label("Exclude files:");
            excludeLabel.getStyleClass().add("ps-label");
            TextField excludeField = new TextField();
            excludeField.setPromptText("Use ; to separate name patterns, * for any number of symbols, ? for one.");
            excludeField.getStyleClass().add("text-field");

            VBox content = new VBox(8, title, moduleName, sourcesLabel, langRow,
                    markBox, fileTree, addContentRow, folders, excludeLabel, excludeField);
            content.setPadding(new Insets(0, 0, 12, 0));

            ScrollPane scroll = wrapInScroll(content);
            getChildren().addAll(scroll);
        }

        // --------------------------------------------------- Libraries page

        private void buildLibrariesPage(String projectName, Path projectRoot) {
            Label title = new Label("Libraries");
            title.getStyleClass().add("ps-page-title");

            // Library name
            Label nameLabel = new Label("Name:");
            nameLabel.getStyleClass().add("ps-label");
            TextField nameField = new TextField("lumina-ide");
            nameField.getStyleClass().add("text-field");
            nameField.setPrefWidth(300);

            // Compiler output
            CheckBox inheritOutput = new CheckBox("Inherit project compile output path");
            inheritOutput.getStyleClass().add("settings-check");
            CheckBox useModuleOutput = new CheckBox("Use module compile output path");
            useModuleOutput.setSelected(true);
            useModuleOutput.getStyleClass().add("settings-check");

            GridPane outputGrid = new GridPane();
            outputGrid.setHgap(12);
            outputGrid.setVgap(6);
            outputGrid.setPadding(new Insets(8, 0, 8, 20));

            outputGrid.add(new Label("Output path:"), 0, 0);
            TextField outputPath = new TextField(projectRoot != null ? projectRoot.resolve("target/classes").toString() : "");
            outputPath.getStyleClass().add("text-field");
            outputPath.setPrefWidth(350);
            outputGrid.add(outputPath, 1, 0);

            outputGrid.add(new Label("Test output path:"), 0, 1);
            TextField testOutputPath = new TextField(projectRoot != null ? projectRoot.resolve("target/test-classes").toString() : "");
            testOutputPath.getStyleClass().add("text-field");
            testOutputPath.setPrefWidth(350);
            outputGrid.add(testOutputPath, 1, 1);

            CheckBox excludeOutput = new CheckBox("Exclude output paths");
            excludeOutput.getStyleClass().add("settings-check");

            // JavaDoc section
            Label javadocLabel = new Label("JavaDoc:");
            javadocLabel.getStyleClass().add("ps-label");
            HBox javadocBox = new HBox(6);
            javadocBox.setAlignment(Pos.CENTER_LEFT);
            Button javadocAdd = new Button("+");
            javadocAdd.getStyleClass().add("ps-add-button");
            Button javadocRemove = new Button("-");
            javadocRemove.getStyleClass().add("ps-add-button");
            javadocBox.getChildren().addAll(javadocAdd, javadocRemove);

            Label javadocNote = new Label("Manage external JavaDocs attached to this module. External JavaDocs override JavaDoc annotations you might have in your module.");
            javadocNote.getStyleClass().add("ps-hint");
            javadocNote.setWrapText(true);

            // External Annotations
            Label extLabel = new Label("External Annotations:");
            extLabel.getStyleClass().add("ps-label");
            HBox extBox = new HBox(6);
            extBox.setAlignment(Pos.CENTER_LEFT);
            Button extAdd = new Button("+");
            extAdd.getStyleClass().add("ps-add-button");
            Button extRemove = new Button("-");
            extRemove.getStyleClass().add("ps-add-button");
            extBox.getChildren().addAll(extAdd, extRemove);

            Label extNote = new Label("Manage external annotations attached to this module.");
            extNote.getStyleClass().add("ps-hint");
            extNote.setWrapText(true);

            VBox content = new VBox(8, title, nameLabel, nameField,
                    inheritOutput, useModuleOutput, outputGrid, excludeOutput,
                    javadocLabel, javadocBox, javadocNote,
                    extLabel, extBox, extNote);
            content.setPadding(new Insets(0, 0, 12, 0));

            ScrollPane scroll = wrapInScroll(content);
            getChildren().addAll(scroll);
        }

        // --------------------------------------------------- Facets page

        private void buildFacetsPage(String projectName, Path projectRoot) {
            Label title = new Label("Facets");
            title.getStyleClass().add("ps-page-title");

            Label empty = new Label("No facets are configured");
            empty.getStyleClass().add("ps-empty");

            Label hint = new Label("Press the '+' button to add a new facet");
            hint.getStyleClass().add("ps-hint");

            Button addFacet = new Button("+");
            addFacet.getStyleClass().add("ps-add-button");
            addFacet.setOnAction(e -> showAddFacetDialog());

            HBox buttonRow = new HBox(8, addFacet);
            buttonRow.setAlignment(Pos.CENTER_LEFT);

            VBox content = new VBox(12, title, empty, hint, buttonRow);
            content.setPadding(new Insets(0, 0, 12, 0));

            ScrollPane scroll = wrapInScroll(content);
            getChildren().addAll(scroll);
        }

        private void showAddFacetDialog() {
            Stage dialog = new Stage();
            dialog.initOwner(getScene().getWindow());
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Add Facet");

            VBox content = new VBox(12);
            content.setPadding(new Insets(20));

            ListView<String> list = new ListView<>();
            list.getItems().addAll(
                    "JAR",
                    "Run-time image (JLink)",
                    "JavaFx application",
                    "Platform specific package",
                    "JavaFx preloader",
                    "Web Application: Exploded",
                    "Web Application: Archive",
                    "Java EE Application: Exploded",
                    "Java EE Application: Archive",
                    "EJB Application: Exploded",
                    "EJB Application: Archive",
                    "Other"
            );
            list.getSelectionModel().selectFirst();
            list.setPrefHeight(300);

            Button add = new Button("Add");
            add.getStyleClass().add("dialog-primary");
            add.setOnAction(e -> dialog.close());

            Button cancel = new Button("Cancel");
            cancel.getStyleClass().add("dialog-secondary");
            cancel.setOnAction(e -> dialog.close());

            HBox buttons = new HBox(10, add, cancel);
            buttons.setAlignment(Pos.CENTER_RIGHT);

            VBox root = new VBox(12, list, buttons);
            root.setPadding(new Insets(12, 20, 14, 20));

            Scene scene = new Scene(root, 320, 400);
            scene.getStylesheets().add(
                    getClass().getResource("/css/lumina-dark.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
        }

        // --------------------------------------------------- Artifacts page

        private void buildArtifactsPage(String projectName, Path projectRoot) {
            Label title = new Label("Artifacts");
            title.getStyleClass().add("ps-page-title");

            Label empty = new Label("No artifacts are configured");
            empty.getStyleClass().add("ps-empty");

            Button addArtifact = new Button("+");
            addArtifact.getStyleClass().add("ps-add-button");

            HBox buttonRow = new HBox(8, addArtifact);
            buttonRow.setAlignment(Pos.CENTER_LEFT);

            VBox content = new VBox(12, title, empty, buttonRow);
            content.setPadding(new Insets(0, 0, 12, 0));

            ScrollPane scroll = wrapInScroll(content);
            getChildren().addAll(scroll);
        }

        // --------------------------------------------------- SDKs page

        private void buildSdksPage(String projectName, Path projectRoot) {
            Label title = new Label("SDKs");
            title.getStyleClass().add("ps-page-title");

            // SDK selector
            HBox selectorRow = new HBox(8);
            selectorRow.setAlignment(Pos.CENTER_LEFT);
            Label sdkLabel = new Label("Name:");
            sdkLabel.getStyleClass().add("ps-label");
            ComboBox<String> sdkCombo = new ComboBox<>();
            sdkCombo.getItems().addAll(
                    "21",
                    "25",
                    "dragonwell-ex-21"
            );
            sdkCombo.getSelectionModel().selectFirst();
            sdkCombo.getStyleClass().add("settings-combo");
            selectorRow.getChildren().addAll(sdkLabel, sdkCombo);

            // JDK home path
            Label homeLabel = new Label("JDK home path:");
            homeLabel.getStyleClass().add("ps-label");
            TextField homeField = new TextField("/home/firoze/.jdks/dragonwell-ex-21.0.10");
            homeField.getStyleClass().add("text-field");
            homeField.setPrefWidth(400);

            // Classpath list
            Label classpathLabel = new Label("Classpath");
            classpathLabel.getStyleClass().add("ps-section-title");

            ListView<String> classpathList = new ListView<>();
            String[] cpItems = {
                    "/home/firoze/.jdks/dragonwell-ex-21.0.10/.java.base",
                    "/home/firoze/.jdks/dragonwell-ex-21.0.10/.java.compiler",
                    "/home/firoze/.jdks/dragonwell-ex-21.0.10/.java.datatransfer",
                    "/home/firoze/.jdks/dragonwell-ex-21.0.10/.java.desktop",
                    "/home/firoze/.jdks/dragonwell-ex-21.0.10/.java.instrument",
                    "/home/firoze/.jdks/dragonwell-ex-21.0.10/.java.logging",
                    "/home/firoze/.jdks/dragonwell-ex-21.0.10/.java.management",
                    "/home/firoze/.jdks/dragonwell-ex-21.0.10/.java.naming",
                    "/home/firoze/.jdks/dragonwell-ex-21.0.10/.java.net.http",
                    "/home/firoze/.jdks/dragonwell-ex-21.0.10/.java.prefs",
                    "/home/firoze/.jdks/dragonwell-ex-21.0.10/.java.rmi",
                    "/home/firoze/.jdks/dragonwell-ex-21.0.10/.java.scripting",
                    "/home/firoze/.jdks/dragonwell-ex-21.0.10/.java.se",
                    "/home/firoze/.jdks/dragonwell-ex-21.0.10/.java.security.jgss",
                    "/home/firoze/.jdks/dragonwell-ex-21.0.10/.java.security.sasl",
                    "/home/firoze/.jdks/dragonwell-ex-21.0.10/.java.smartcardio",
                    "/home/firoze/.jdks/dragonwell-ex-21.0.10/.java.sql",
                    "/home/firoze/.jdks/dragonwell-ex-21.0.10/.java.sql.rowset",
                    "/home/firoze/.jdks/dragonwell-ex-21.0.10/.java.transaction.xa",
                    "/home/firoze/.jdks/dragonwell-ex-21.0.10/.java.xml",
                    "/home/firoze/.jdks/dragonwell-ex-21.0.10/.java.xml.crypto",
                    "/home/firoze/.jdks/dragonwell-ex-21.0.10/.jdk.accessibility",
                    "/home/firoze/.jdks/dragonwell-ex-21.0.10/.jdk.attach"
            };
            classpathList.getItems().addAll(cpItems);
            classpathList.setPrefHeight(180);

            VBox content = new VBox(8, title, selectorRow, homeLabel, homeField,
                    classpathLabel, classpathList);
            content.setPadding(new Insets(0, 0, 12, 0));

            ScrollPane scroll = wrapInScroll(content);
            getChildren().addAll(scroll);
        }

        // --------------------------------------------------- Global Libraries page

        private void buildGlobalLibrariesPage(String projectName, Path projectRoot) {
            Label title = new Label("Global Libraries");
            title.getStyleClass().add("ps-page-title");

            // New Global Library
            Label newLabel = new Label("New Global Library");
            newLabel.getStyleClass().add("ps-section-title");

            HBox newRow = new HBox(8);
            newRow.setAlignment(Pos.CENTER_LEFT);
            Button javaBtn = new Button("Java");
            javaBtn.getStyleClass().add("dialog-secondary");
            Button mavenBtn = new Button("From Maven...");
            mavenBtn.getStyleClass().add("dialog-secondary");
            Button kotlinBtn = new Button("Kotlin/JS");
            kotlinBtn.getStyleClass().add("dialog-secondary");
            newRow.getChildren().addAll(javaBtn, mavenBtn, kotlinBtn);

            Label empty = new Label("Nothing to show");
            empty.getStyleClass().add("ps-empty");

            Label hint = new Label("Select a library to view or edit its details here");
            hint.getStyleClass().add("ps-hint");

            VBox content = new VBox(12, title, newLabel, newRow, empty, hint);
            content.setPadding(new Insets(0, 0, 12, 0));

            ScrollPane scroll = wrapInScroll(content);
            getChildren().addAll(scroll);
        }

        // --------------------------------------------------- Problems page

        private void buildProblemsPage(String projectName, Path projectRoot) {
            Label title = new Label("Problems");
            title.getStyleClass().add("ps-page-title");

            Label empty = new Label("No problems found");
            empty.getStyleClass().add("ps-empty");

            VBox content = new VBox(12, title, empty);
            content.setPadding(new Insets(0, 0, 12, 0));

            ScrollPane scroll = wrapInScroll(content);
            getChildren().addAll(scroll);
        }

        // --------------------------------------------------- Placeholder

        private void buildPlaceholderPage(String pageName) {
            Label title = new Label(pageName);
            title.getStyleClass().add("ps-page-title");

            Label placeholder = new Label("Settings for '" + pageName + "' will be available in a future update.");
            placeholder.getStyleClass().add("ps-placeholder");

            VBox content = new VBox(20, title, placeholder);
            content.setPadding(new Insets(20, 24, 20, 24));

            ScrollPane scroll = wrapInScroll(content);
            getChildren().addAll(scroll);
        }

        private ScrollPane wrapInScroll(VBox content) {
            ScrollPane scroll = new ScrollPane(content);
            scroll.setFitToWidth(true);
            scroll.getStyleClass().add("ps-scroll");
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            VBox.setVgrow(scroll, Priority.ALWAYS);
            return scroll;
        }
    }
}