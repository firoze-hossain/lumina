package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * IntelliJ-style Remote Development dialog.
 * Left: Remote Development tree with SSH and Dev Containers.
 * Right: Recent SSH Projects, Dev Containers, and New Dev Container forms.
 */
public class RemoteDevelopmentDialog {

    private final Stage stage;
    private final RemotePage currentPage = new RemotePage();

    public RemoteDevelopmentDialog(Stage owner) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.DECORATED);
        stage.setTitle("Remote Development");

        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("app-root", "remote-dialog");

        // ---- Left: category tree ----
        TreeView<String> tree = buildCategoryTree();
        tree.setPrefWidth(220);
        tree.setMinWidth(200);
        tree.getStyleClass().add("remote-tree");

        // ---- Right: content panel ----
        currentPage.getStyleClass().add("remote-page");

        // ---- Bottom: buttons (minimal, just a close button) ----
        HBox buttons = buildButtonBar();

        root.setLeft(tree);
        root.setCenter(currentPage);
        root.setBottom(buttons);

        // Initial selection: Remote Development → Remote Development
        TreeItem<String> remoteItem = findItem(tree.getRoot(), "Remote Development");
        if (remoteItem != null) {
            tree.getSelectionModel().select(remoteItem);
        }

        // When tree selection changes, update the page
        tree.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                currentPage.showPage(selected.getValue());
            }
        });

        Scene scene = new Scene(root, 860, 540);
        scene.getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
        stage.setScene(scene);
    }

    public void show() {
        stage.showAndWait();
    }

    // --------------------------------------------------- category tree

    private TreeView<String> buildCategoryTree() {
        TreeItem<String> root = new TreeItem<>("Remote Development");
        root.setExpanded(true);

        // Remote Development
        TreeItem<String> remote = new TreeItem<>("Remote Development");

        // Connections submenu
        TreeItem<String> connections = new TreeItem<>("Connections");
        TreeItem<String> ssh = new TreeItem<>("SSH");
        TreeItem<String> devContainers = new TreeItem<>("Dev Containers");
        connections.getChildren().addAll(ssh, devContainers);
        remote.getChildren().add(connections);

        root.getChildren().add(remote);

        TreeView<String> tree = new TreeView<>(root);
        tree.setShowRoot(true);
        tree.getStyleClass().add("remote-tree");
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
                // Bold for "Remote Development" and "Connections"
                if ("Remote Development".equals(item) || "Connections".equals(item)) {
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #A6ADC4;");
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
        Button close = new Button("Close");
        close.getStyleClass().add("dialog-secondary");
        close.setOnAction(e -> stage.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(10, spacer, close);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPadding(new Insets(10, 20, 14, 20));
        bar.getStyleClass().add("dialog-footer");
        return bar;
    }

    // --------------------------------------------------- remote page

    /**
     * The right-hand panel that shows the Remote Development content.
     */
    private static class RemotePage extends VBox {

        private VBox contentArea;

        public RemotePage() {
            getStyleClass().add("remote-page");
            setPadding(new Insets(20, 24, 20, 24));
            setSpacing(16);

            buildRemotePage();
        }

        private void buildRemotePage() {
            // ---- Header ----
            Label pageTitle = new Label("Remote Development");
            pageTitle.getStyleClass().add("remote-page-title");

            // ---- Scrollable content ----
            contentArea = new VBox(20);
            contentArea.getStyleClass().add("remote-content");

            // ---- Recent SSH Projects section ----
            Label recentTitle = new Label("Recent SSH Projects");
            recentTitle.getStyleClass().add("remote-section-title");

            HBox cards = new HBox(16);
            cards.setAlignment(Pos.CENTER_LEFT);

            // SSH card
            VBox sshCard = new VBox(4);
            sshCard.getStyleClass().add("remote-card");
            sshCard.setPrefWidth(140);
            sshCard.setPrefHeight(80);
            sshCard.setAlignment(Pos.CENTER);
            Label sshIcon = new Label("\uD83D\uDD10");
            sshIcon.getStyleClass().add("remote-card-icon");
            Label sshLabel = new Label("SSH");
            sshLabel.getStyleClass().add("remote-card-label");
            sshCard.getChildren().addAll(sshIcon, sshLabel);

            // Dev Containers card
            VBox devCard = new VBox(4);
            devCard.getStyleClass().add("remote-card");
            devCard.setPrefWidth(140);
            devCard.setPrefHeight(80);
            devCard.setAlignment(Pos.CENTER);
            Label devIcon = new Label("\uD83D\uDCE6");
            devIcon.getStyleClass().add("remote-card-icon");
            Label devLabel = new Label("Dev Containers");
            devLabel.getStyleClass().add("remote-card-label");
            devCard.getChildren().addAll(devIcon, devLabel);

            cards.getChildren().addAll(sshCard, devCard);

            // ---- Info box ----
            VBox infoBox = new VBox(8);
            infoBox.getStyleClass().add("remote-info-box");
            Label infoTitle = new Label("Work on your code on another host using SSH");
            infoTitle.getStyleClass().add("remote-info-title");
            Button newProjectBtn = new Button("New Project");
            newProjectBtn.getStyleClass().add("dialog-primary");
            newProjectBtn.setOnAction(e -> {
                // Open New Project dialog
                new NewProjectDialog(
                        (Stage) getScene().getWindow(),
                        spec -> {
                            // Handle project creation
                            System.out.println("Creating remote project: " + spec.name());
                        }
                ).show();
            });
            HBox infoRow = new HBox(12, infoTitle, newProjectBtn);
            infoRow.setAlignment(Pos.CENTER_LEFT);
            infoBox.getChildren().add(infoRow);

            // ---- Dev Containers section ----
            Label devContainerTitle = new Label("Dev Containers");
            devContainerTitle.getStyleClass().add("remote-section-title");

            // New Dev Container button
            Button newDevContainer = new Button("New Dev Container");
            newDevContainer.getStyleClass().add("dialog-secondary");
            newDevContainer.setOnAction(e -> showNewDevContainerDialog());

            // Docker daemon error
            Label dockerError = new Label("Cannot connect to the Docker daemon at unix:///var/run/docker.sock. Is the docker daemon running?");
            dockerError.getStyleClass().add("remote-error");

            VBox devContainerBox = new VBox(10, newDevContainer, dockerError);

            // ---- Beta info ----
            Label betaLabel = new Label("Try opening Dev Container projects like local");
            betaLabel.getStyleClass().add("remote-beta");
            Hyperlink learnMore = new Hyperlink("Learn more");
            learnMore.getStyleClass().add("remote-link");
            HBox betaRow = new HBox(8, betaLabel, learnMore);
            betaRow.setAlignment(Pos.CENTER_LEFT);

            // ---- Assemble ----
            contentArea.getChildren().addAll(
                    recentTitle,
                    cards,
                    infoBox,
                    devContainerTitle,
                    devContainerBox,
                    betaRow
            );

            // ---- ScrollPane wrapper ----
            ScrollPane scroll = new ScrollPane(contentArea);
            scroll.setFitToWidth(true);
            scroll.getStyleClass().add("remote-scroll");
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            VBox.setVgrow(scroll, Priority.ALWAYS);

            getChildren().addAll(pageTitle, scroll);
        }

        /** Show a page based on the selected tree item. */
        public void showPage(String pageName) {
            getChildren().clear();

            if ("Remote Development".equals(pageName)) {
                buildRemotePage();
            } else if ("SSH".equals(pageName)) {
                buildSshPage();
            } else if ("Dev Containers".equals(pageName)) {
                buildDevContainersPage();
            } else {
                // Placeholder
                Label title = new Label(pageName);
                title.getStyleClass().add("remote-page-title");
                Label placeholder = new Label("Settings for '" + pageName + "' will be available in a future update.");
                placeholder.getStyleClass().add("remote-placeholder");
                VBox box = new VBox(20, title, placeholder);
                box.setPadding(new Insets(40, 24, 20, 24));
                ScrollPane scroll = new ScrollPane(box);
                scroll.setFitToWidth(true);
                scroll.getStyleClass().add("remote-scroll");
                scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                VBox.setVgrow(scroll, Priority.ALWAYS);
                getChildren().addAll(scroll);
            }
        }

        private void buildSshPage() {
            Label title = new Label("SSH Connections");
            title.getStyleClass().add("remote-page-title");

            VBox content = new VBox(16);
            content.setPadding(new Insets(8, 0, 0, 0));

            Button newConnection = new Button("New Connection");
            newConnection.getStyleClass().add("dialog-primary");

            Label empty = new Label("No SSH connections configured.");
            empty.getStyleClass().add("remote-placeholder");

            content.getChildren().addAll(newConnection, empty);

            ScrollPane scroll = new ScrollPane(content);
            scroll.setFitToWidth(true);
            scroll.getStyleClass().add("remote-scroll");
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            VBox.setVgrow(scroll, Priority.ALWAYS);

            getChildren().addAll(title, scroll);
        }

        private void buildDevContainersPage() {
            Label title = new Label("Dev Containers");
            title.getStyleClass().add("remote-page-title");

            VBox content = new VBox(16);
            content.setPadding(new Insets(8, 0, 0, 0));

            Button newContainer = new Button("New Dev Container");
            newContainer.getStyleClass().add("dialog-primary");
            newContainer.setOnAction(e -> showNewDevContainerDialog());

            Label empty = new Label("No Dev Containers configured.");
            empty.getStyleClass().add("remote-placeholder");

            content.getChildren().addAll(newContainer, empty);

            ScrollPane scroll = new ScrollPane(content);
            scroll.setFitToWidth(true);
            scroll.getStyleClass().add("remote-scroll");
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            VBox.setVgrow(scroll, Priority.ALWAYS);

            getChildren().addAll(title, scroll);
        }

        private void showNewDevContainerDialog() {
            Stage dialog = new Stage();
            dialog.initOwner(getScene().getWindow());
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("New Dev Container");

            BorderPane root = new BorderPane();
            root.getStyleClass().addAll("app-root", "remote-dialog");

            // ---- Content ----
            VBox content = new VBox(18);
            content.setPadding(new Insets(24, 28, 20, 28));

            Label title = new Label("New Dev Container");
            title.getStyleClass().add("remote-page-title");

            // Docker row
            HBox dockerRow = new HBox(12);
            dockerRow.setAlignment(Pos.CENTER_LEFT);
            Label dockerLabel = new Label("Docker");
            dockerLabel.getStyleClass().add("remote-label");
            ComboBox<String> dockerCombo = new ComboBox<>();
            dockerCombo.getItems().addAll("Docker", "Podman");
            dockerCombo.getSelectionModel().selectFirst();
            dockerCombo.getStyleClass().add("settings-combo");
            dockerCombo.setPrefWidth(150);
            dockerRow.getChildren().addAll(dockerLabel, dockerCombo);

            // IntelliJ IDEA label
            Label ideaLabel = new Label("IntelliJ IDEA");
            ideaLabel.getStyleClass().add("remote-label-idea");

            // Git Repository
            Label gitLabel = new Label("Git Repository:");
            gitLabel.getStyleClass().add("remote-label");
            TextField gitField = new TextField("git@");
            gitField.getStyleClass().add("text-field");
            HBox gitRow = new HBox(8, gitLabel, gitField);
            gitRow.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(gitField, Priority.ALWAYS);

            // Detection for devcontainer.json
            Label detectionLabel = new Label("Detection for devcontainer.json file:");
            detectionLabel.getStyleClass().add("remote-label");

            HBox detectionRow = new HBox(16);
            detectionRow.setAlignment(Pos.CENTER_LEFT);
            RadioButton automatic = new RadioButton("Automatic");
            automatic.setSelected(true);
            RadioButton specify = new RadioButton("Specify Path");
            ToggleGroup detectGroup = new ToggleGroup();
            automatic.setToggleGroup(detectGroup);
            specify.setToggleGroup(detectGroup);
            detectionRow.getChildren().addAll(automatic, specify);

            // Path field (initially hidden)
            TextField pathField = new TextField();
            pathField.setPromptText("Path to devcontainer.json");
            pathField.getStyleClass().add("text-field");
            pathField.setVisible(false);
            pathField.setManaged(false);

            specify.selectedProperty().addListener((obs, old, selected) -> {
                pathField.setVisible(selected);
                pathField.setManaged(selected);
            });

            VBox detectionBox = new VBox(6, detectionLabel, detectionRow, pathField);

            content.getChildren().addAll(
                    title,
                    dockerRow,
                    ideaLabel,
                    gitRow,
                    detectionBox
            );

            // ---- Buttons ----
            HBox buttons = new HBox(10);
            buttons.setAlignment(Pos.CENTER_RIGHT);
            buttons.setPadding(new Insets(12, 20, 14, 20));
            buttons.getStyleClass().add("dialog-footer");

            Button closeBtn = new Button("Close");
            closeBtn.getStyleClass().add("dialog-secondary");
            closeBtn.setOnAction(e -> dialog.close());

            Button buildBtn = new Button("Build Container and Continue");
            buildBtn.getStyleClass().add("dialog-primary");
            buildBtn.setOnAction(e -> {
                // Handle build
                System.out.println("Building container...");
                dialog.close();
            });

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            buttons.getChildren().addAll(spacer, closeBtn, buildBtn);

            root.setCenter(content);
            root.setBottom(buttons);

            Scene scene = new Scene(root, 580, 380);
            scene.getStylesheets().add(
                    getClass().getResource("/css/lumina-dark.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
        }
    }
}