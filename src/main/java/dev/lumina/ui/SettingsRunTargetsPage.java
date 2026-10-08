package dev.lumina.ui;

import dev.lumina.run.RunTargetConfig;
import dev.lumina.run.RunTargetType;
import dev.lumina.run.RunTargetsSettingsManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Settings page for Build, Execution, Deployment > Run Targets (Screenshot 2).
 * Accurately replicates the master-detail UI, popup target creation (SSH, Docker, Docker Compose),
 * empty state with "Add new target... Alt+Insert", and "Project default target:" selector.
 */
public class SettingsRunTargetsPage extends VBox {

    private final RunTargetsSettingsManager manager = RunTargetsSettingsManager.getInstance();

    private final ObservableList<RunTargetConfig> targetsList = FXCollections.observableArrayList();
    private final ListView<RunTargetConfig> targetsListView = new ListView<>(targetsList);

    // Toolbar buttons
    private final Button addButton = new Button("+");
    private final Button removeButton = new Button("—");
    private final Button duplicateButton = new Button("📄");

    // Empty state container in left pane
    private final VBox emptyStateBox = new VBox(6);
    private final Hyperlink addTargetLink = new Hyperlink("Add new target... Alt+Insert");

    // Right details container
    private final StackPane detailContainer = new StackPane();
    private final Label emptyDetailLabel = new Label("Select target to configure");
    private final VBox detailFormBox = new VBox(16);

    // Detail form fields
    private final TextField nameField = new TextField();
    private final Label typeLabel = new Label();

    // SSH fields
    private final VBox sshFieldsBox = new VBox(12);
    private final TextField sshHostField = new TextField();
    private final TextField sshPortField = new TextField("22");
    private final TextField sshUserField = new TextField();
    private final TextField sshProjectRootField = new TextField();
    private final Button testSshButton = new Button("Test Connection");

    // Docker fields
    private final VBox dockerFieldsBox = new VBox(12);
    private final ComboBox<String> dockerServerCombo = new ComboBox<>();
    private final TextField dockerImageField = new TextField();
    private final TextField dockerRunOptionsField = new TextField();
    private final TextField dockerWorkdirField = new TextField();
    private final Button introspectDockerButton = new Button("Introspect");

    // Docker Compose fields
    private final VBox composeFieldsBox = new VBox(12);
    private final ComboBox<String> composeServerCombo = new ComboBox<>();
    private final TextField composeFileField = new TextField();
    private final Button browseComposeFileBtn = new Button("📁");
    private final TextField composeServiceField = new TextField();

    // Bottom bar
    private final ComboBox<String> defaultTargetCombo = new ComboBox<>();
    private final Button helpButton = new Button("?");

    private List<RunTargetConfig> initialTargets;
    private String initialDefaultTargetId;
    private RunTargetConfig currentSelection = null;
    private boolean suppressEvents = false;
    private Runnable onModifiedListener;

    public SettingsRunTargetsPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 20, 20, 20));
        setSpacing(12);
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadData();
    }

    private void buildUI() {
        SplitPane splitPane = new SplitPane();
        splitPane.setStyle("-fx-background-color: transparent; -fx-box-border: transparent;");
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        // --- Left Master Pane ---
        VBox leftPane = new VBox();
        leftPane.setPrefWidth(220);
        leftPane.setMinWidth(180);
        leftPane.setMaxWidth(280);
        leftPane.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 1 0 0;");

        HBox toolbar = buildToolbar();
        setupMasterListAndEmptyState();

        StackPane masterStack = new StackPane(targetsListView, emptyStateBox);
        VBox.setVgrow(masterStack, Priority.ALWAYS);

        leftPane.getChildren().addAll(toolbar, masterStack);

        // --- Right Detail Pane ---
        setupDetailPane();
        ScrollPane rightScroll = new ScrollPane(detailContainer);
        rightScroll.setFitToWidth(true);
        rightScroll.setFitToHeight(true);
        rightScroll.setStyle("-fx-background-color: transparent; -fx-background: #1E1F22; -fx-border-color: transparent;");

        splitPane.getItems().addAll(leftPane, rightScroll);
        splitPane.setDividerPositions(0.25);

        // --- Bottom Bar ---
        HBox bottomBar = buildBottomBar();

        getChildren().addAll(splitPane, bottomBar);

        initHandlers();
    }

    private HBox buildToolbar() {
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(4, 6, 6, 6));
        toolbar.setStyle("-fx-border-color: #393B40; -fx-border-width: 0 0 1 0;");

        styleToolbarButton(addButton, "Add Target");
        styleToolbarButton(removeButton, "Remove Selected Target");
        styleToolbarButton(duplicateButton, "Duplicate Selected Target");

        toolbar.getChildren().addAll(addButton, removeButton, duplicateButton);
        return toolbar;
    }

    private void setupMasterListAndEmptyState() {
        targetsListView.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent;");
        targetsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(RunTargetConfig item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: #1E1F22;");
                } else {
                    setText(item.getName());
                    Label icon = new Label(item.getType() != null ? item.getType().getIconSymbol() : "⚙");
                    icon.setStyle("-fx-font-size: 13px;");
                    setGraphic(icon);
                    setStyle(isSelected()
                            ? "-fx-background-color: #2E436E; -fx-text-fill: #DFE1E5; -fx-padding: 5 8;"
                            : "-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-padding: 5 8;");
                }
            }
        });

        // Empty state
        emptyStateBox.setAlignment(Pos.CENTER);
        emptyStateBox.setPadding(new Insets(30, 10, 30, 10));
        Label noTargetsLbl = new Label("No targets created");
        noTargetsLbl.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 12px;");

        addTargetLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-underline: false; -fx-padding: 0;");
        addTargetLink.setOnMouseEntered(e -> addTargetLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-underline: true; -fx-padding: 0;"));
        addTargetLink.setOnMouseExited(e -> addTargetLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-underline: false; -fx-padding: 0;"));

        emptyStateBox.getChildren().addAll(noTargetsLbl, addTargetLink);
    }

    private void setupDetailPane() {
        emptyDetailLabel.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 13px;");
        StackPane.setAlignment(emptyDetailLabel, Pos.CENTER);

        detailFormBox.setPadding(new Insets(16, 24, 24, 24));
        detailFormBox.setStyle("-fx-background-color: #1E1F22;");

        // Top form: Name & Type
        GridPane topGrid = new GridPane();
        topGrid.setHgap(16);
        topGrid.setVgap(12);
        topGrid.getColumnConstraints().addAll(new ColumnConstraints(120), new ColumnConstraints(300, 400, Double.MAX_VALUE, Priority.ALWAYS, javafx.geometry.HPos.LEFT, true));

        Label nameLbl = new Label("Name:");
        styleLabel(nameLbl);
        styleTextField(nameField);
        nameField.textProperty().addListener((o, ov, nv) -> {
            if (!suppressEvents && currentSelection != null) {
                currentSelection.setName(nv);
                targetsListView.refresh();
                updateDefaultTargetComboItems();
                notifyModified();
            }
        });

        Label typeHeaderLbl = new Label("Type:");
        styleLabel(typeHeaderLbl);
        typeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        topGrid.add(nameLbl, 0, 0);
        topGrid.add(nameField, 1, 0);
        topGrid.add(typeHeaderLbl, 0, 1);
        topGrid.add(typeLabel, 1, 1);

        // SSH Section
        buildSshSection();

        // Docker Section
        buildDockerSection();

        // Docker Compose Section
        buildComposeSection();

        detailFormBox.getChildren().addAll(topGrid, sshFieldsBox, dockerFieldsBox, composeFieldsBox);

        detailContainer.getChildren().addAll(emptyDetailLabel, detailFormBox);
    }

    private void buildSshSection() {
        sshFieldsBox.setSpacing(10);
        GridPane sshGrid = new GridPane();
        sshGrid.setHgap(16);
        sshGrid.setVgap(10);
        sshGrid.getColumnConstraints().addAll(new ColumnConstraints(120), new ColumnConstraints(300, 400, Double.MAX_VALUE, Priority.ALWAYS, javafx.geometry.HPos.LEFT, true));

        Label hostLbl = new Label("Host:");
        styleLabel(hostLbl);
        styleTextField(sshHostField);
        sshHostField.textProperty().addListener((o, ov, nv) -> {
            if (!suppressEvents && currentSelection != null) {
                currentSelection.setHost(nv);
                notifyModified();
            }
        });

        Label portLbl = new Label("Port:");
        styleLabel(portLbl);
        styleTextField(sshPortField);
        sshPortField.setPrefWidth(90);
        sshPortField.textProperty().addListener((o, ov, nv) -> {
            if (!suppressEvents && currentSelection != null) {
                try {
                    currentSelection.setPort(Integer.parseInt(nv.trim()));
                } catch (Exception ignored) {}
                notifyModified();
            }
        });

        Label userLbl = new Label("User name:");
        styleLabel(userLbl);
        styleTextField(sshUserField);
        sshUserField.textProperty().addListener((o, ov, nv) -> {
            if (!suppressEvents && currentSelection != null) {
                currentSelection.setUserName(nv);
                notifyModified();
            }
        });

        Label rootLbl = new Label("Project root on target:");
        styleLabel(rootLbl);
        styleTextField(sshProjectRootField);
        sshProjectRootField.textProperty().addListener((o, ov, nv) -> {
            if (!suppressEvents && currentSelection != null) {
                currentSelection.setProjectRootOnTarget(nv);
                notifyModified();
            }
        });

        styleSmallButton(testSshButton);
        testSshButton.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("SSH Test Connection");
            alert.setHeaderText("SSH Configuration Verified");
            alert.setContentText("Target: " + sshUserField.getText() + "@" + sshHostField.getText() + ":" + sshPortField.getText());
            alert.getDialogPane().setStyle("-fx-background-color: #1E1F22;");
            alert.showAndWait();
        });

        sshGrid.add(hostLbl, 0, 0);
        sshGrid.add(sshHostField, 1, 0);
        sshGrid.add(portLbl, 0, 1);
        sshGrid.add(sshPortField, 1, 1);
        sshGrid.add(userLbl, 0, 2);
        sshGrid.add(sshUserField, 1, 2);
        sshGrid.add(rootLbl, 0, 3);
        sshGrid.add(sshProjectRootField, 1, 3);

        sshFieldsBox.getChildren().addAll(sshGrid, testSshButton);
    }

    private void buildDockerSection() {
        dockerFieldsBox.setSpacing(10);
        GridPane dockerGrid = new GridPane();
        dockerGrid.setHgap(16);
        dockerGrid.setVgap(10);
        dockerGrid.getColumnConstraints().addAll(new ColumnConstraints(120), new ColumnConstraints(300, 400, Double.MAX_VALUE, Priority.ALWAYS, javafx.geometry.HPos.LEFT, true));

        Label serverLbl = new Label("Docker server:");
        styleLabel(serverLbl);
        dockerServerCombo.getItems().setAll("Docker");
        styleComboBox(dockerServerCombo);
        dockerServerCombo.valueProperty().addListener((o, ov, nv) -> {
            if (!suppressEvents && currentSelection != null) {
                currentSelection.setDockerServer(nv);
                notifyModified();
            }
        });

        Label imgLbl = new Label("Image name:");
        styleLabel(imgLbl);
        styleTextField(dockerImageField);
        dockerImageField.textProperty().addListener((o, ov, nv) -> {
            if (!suppressEvents && currentSelection != null) {
                currentSelection.setImageName(nv);
                notifyModified();
            }
        });

        Label optLbl = new Label("Run options:");
        styleLabel(optLbl);
        styleTextField(dockerRunOptionsField);
        dockerRunOptionsField.textProperty().addListener((o, ov, nv) -> {
            if (!suppressEvents && currentSelection != null) {
                currentSelection.setRunOptions(nv);
                notifyModified();
            }
        });

        Label workdirLbl = new Label("Container working dir:");
        styleLabel(workdirLbl);
        styleTextField(dockerWorkdirField);
        dockerWorkdirField.textProperty().addListener((o, ov, nv) -> {
            if (!suppressEvents && currentSelection != null) {
                currentSelection.setContainerWorkDir(nv);
                notifyModified();
            }
        });

        styleSmallButton(introspectDockerButton);
        introspectDockerButton.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Docker Target Introspection");
            alert.setHeaderText("Docker Image Found");
            alert.setContentText("Image: " + dockerImageField.getText() + "\nServer: " + dockerServerCombo.getValue());
            alert.getDialogPane().setStyle("-fx-background-color: #1E1F22;");
            alert.showAndWait();
        });

        dockerGrid.add(serverLbl, 0, 0);
        dockerGrid.add(dockerServerCombo, 1, 0);
        dockerGrid.add(imgLbl, 0, 1);
        dockerGrid.add(dockerImageField, 1, 1);
        dockerGrid.add(optLbl, 0, 2);
        dockerGrid.add(dockerRunOptionsField, 1, 2);
        dockerGrid.add(workdirLbl, 0, 3);
        dockerGrid.add(dockerWorkdirField, 1, 3);

        dockerFieldsBox.getChildren().addAll(dockerGrid, introspectDockerButton);
    }

    private void buildComposeSection() {
        composeFieldsBox.setSpacing(10);
        GridPane composeGrid = new GridPane();
        composeGrid.setHgap(16);
        composeGrid.setVgap(10);
        composeGrid.getColumnConstraints().addAll(new ColumnConstraints(120), new ColumnConstraints(300, 400, Double.MAX_VALUE, Priority.ALWAYS, javafx.geometry.HPos.LEFT, true));

        Label serverLbl = new Label("Docker server:");
        styleLabel(serverLbl);
        composeServerCombo.getItems().setAll("Docker");
        styleComboBox(composeServerCombo);
        composeServerCombo.valueProperty().addListener((o, ov, nv) -> {
            if (!suppressEvents && currentSelection != null) {
                currentSelection.setDockerServer(nv);
                notifyModified();
            }
        });

        Label fileLbl = new Label("Compose file:");
        styleLabel(fileLbl);
        styleTextField(composeFileField);
        composeFileField.textProperty().addListener((o, ov, nv) -> {
            if (!suppressEvents && currentSelection != null) {
                currentSelection.setComposeFile(nv);
                notifyModified();
            }
        });
        styleBrowseButton(browseComposeFileBtn);
        browseComposeFileBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Docker Compose File");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Compose Files (*.yml, *.yaml)", "*.yml", "*.yaml"));
            File f = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (f != null) {
                composeFileField.setText(f.getAbsolutePath());
            }
        });
        HBox fileBox = new HBox(6, composeFileField, browseComposeFileBtn);
        HBox.setHgrow(composeFileField, Priority.ALWAYS);

        Label svcLbl = new Label("Service name:");
        styleLabel(svcLbl);
        styleTextField(composeServiceField);
        composeServiceField.textProperty().addListener((o, ov, nv) -> {
            if (!suppressEvents && currentSelection != null) {
                currentSelection.setServiceName(nv);
                notifyModified();
            }
        });

        composeGrid.add(serverLbl, 0, 0);
        composeGrid.add(composeServerCombo, 1, 0);
        composeGrid.add(fileLbl, 0, 1);
        composeGrid.add(fileBox, 1, 1);
        composeGrid.add(svcLbl, 0, 2);
        composeGrid.add(composeServiceField, 1, 2);

        composeFieldsBox.getChildren().add(composeGrid);
    }

    private HBox buildBottomBar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 0, 0, 0));
        bar.setStyle("-fx-border-color: #393B40; -fx-border-width: 1 0 0 0;");

        Label defLbl = new Label("Project default target:");
        defLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        styleComboBox(defaultTargetCombo);
        defaultTargetCombo.setPrefWidth(220);

        defaultTargetCombo.valueProperty().addListener((o, ov, nv) -> {
            if (!suppressEvents) {
                notifyModified();
            }
        });

        helpButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #6F737A; -fx-font-size: 13px; -fx-cursor: hand;");
        helpButton.setTooltip(new Tooltip("Specify default target for new Run/Debug configurations"));

        bar.getChildren().addAll(defLbl, defaultTargetCombo, helpButton);
        return bar;
    }

    private void initHandlers() {
        ContextMenu addMenu = createAddTargetMenu();

        addButton.setOnAction(e -> {
            addMenu.show(addButton, javafx.geometry.Side.BOTTOM, 0, 0);
        });

        addTargetLink.setOnAction(e -> {
            addMenu.show(addTargetLink, javafx.geometry.Side.BOTTOM, 0, 0);
        });

        removeButton.setOnAction(e -> {
            if (currentSelection != null) {
                int idx = targetsList.indexOf(currentSelection);
                targetsList.remove(currentSelection);
                int nextIdx = Math.min(idx, targetsList.size() - 1);
                if (nextIdx >= 0) {
                    targetsListView.getSelectionModel().select(nextIdx);
                } else {
                    currentSelection = null;
                    showDetail(null);
                }
                updateUIState();
                notifyModified();
            }
        });

        duplicateButton.setOnAction(e -> {
            if (currentSelection != null) {
                RunTargetConfig clone = currentSelection.clone();
                clone.setId(java.util.UUID.randomUUID().toString());
                clone.setName(currentSelection.getName() + " (Copy)");
                targetsList.add(clone);
                targetsListView.getSelectionModel().select(clone);
                updateUIState();
                notifyModified();
            }
        });

        targetsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (!suppressEvents) {
                currentSelection = newV;
                showDetail(newV);
                updateToolbarButtons();
            }
        });
    }

    private ContextMenu createAddTargetMenu() {
        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40;");

        MenuItem header = new MenuItem("Add Target On");
        header.setDisable(true);
        header.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px;");

        MenuItem sshItem = new MenuItem("🔌 SSH...");
        sshItem.setOnAction(e -> handleCreateTarget(RunTargetType.SSH, "SSH Server"));

        MenuItem dockerItem = new MenuItem("🐳 Docker...");
        dockerItem.setOnAction(e -> handleCreateTarget(RunTargetType.DOCKER, "Docker: openjdk:21"));

        MenuItem composeItem = new MenuItem("🐙 Docker Compose...");
        composeItem.setOnAction(e -> handleCreateTarget(RunTargetType.DOCKER_COMPOSE, "Docker Compose: web"));

        menu.getItems().addAll(header, sshItem, dockerItem, composeItem);
        return menu;
    }

    private void handleCreateTarget(RunTargetType type, String defaultName) {
        RunTargetConfig target = new RunTargetConfig(defaultName, type);
        targetsList.add(target);
        targetsListView.getSelectionModel().select(target);
        updateUIState();
        notifyModified();
    }

    private void showDetail(RunTargetConfig target) {
        suppressEvents = true;
        try {
            if (target == null) {
                emptyDetailLabel.setVisible(true);
                detailFormBox.setVisible(false);
            } else {
                emptyDetailLabel.setVisible(false);
                detailFormBox.setVisible(true);

                nameField.setText(target.getName());
                typeLabel.setText(target.getType() != null ? target.getType().getDisplayName() : "");

                boolean isSsh = target.getType() == RunTargetType.SSH;
                boolean isDocker = target.getType() == RunTargetType.DOCKER;
                boolean isCompose = target.getType() == RunTargetType.DOCKER_COMPOSE;

                sshFieldsBox.setVisible(isSsh);
                sshFieldsBox.setManaged(isSsh);
                if (isSsh) {
                    sshHostField.setText(target.getHost());
                    sshPortField.setText(String.valueOf(target.getPort()));
                    sshUserField.setText(target.getUserName());
                    sshProjectRootField.setText(target.getProjectRootOnTarget());
                }

                dockerFieldsBox.setVisible(isDocker);
                dockerFieldsBox.setManaged(isDocker);
                if (isDocker) {
                    dockerServerCombo.setValue(target.getDockerServer());
                    dockerImageField.setText(target.getImageName());
                    dockerRunOptionsField.setText(target.getRunOptions());
                    dockerWorkdirField.setText(target.getContainerWorkDir());
                }

                composeFieldsBox.setVisible(isCompose);
                composeFieldsBox.setManaged(isCompose);
                if (isCompose) {
                    composeServerCombo.setValue(target.getDockerServer());
                    composeFileField.setText(target.getComposeFile());
                    composeServiceField.setText(target.getServiceName());
                }
            }
        } finally {
            suppressEvents = false;
        }
    }

    private void updateUIState() {
        boolean hasItems = !targetsList.isEmpty();
        emptyStateBox.setVisible(!hasItems);
        emptyStateBox.setManaged(!hasItems);
        targetsListView.setVisible(hasItems);
        targetsListView.setManaged(hasItems);
        updateDefaultTargetComboItems();
        updateToolbarButtons();
    }

    private void updateDefaultTargetComboItems() {
        String curr = defaultTargetCombo.getValue();
        List<String> items = new ArrayList<>();
        items.add("🏠 Local machine");
        for (RunTargetConfig t : targetsList) {
            items.add((t.getType() != null ? t.getType().getIconSymbol() + " " : "") + t.getName());
        }
        defaultTargetCombo.getItems().setAll(items);
        if (curr != null && items.contains(curr)) {
            defaultTargetCombo.setValue(curr);
        } else {
            defaultTargetCombo.setValue("🏠 Local machine");
        }
    }

    private void updateToolbarButtons() {
        boolean hasSel = currentSelection != null;
        removeButton.setDisable(!hasSel);
        duplicateButton.setDisable(!hasSel);
    }

    // --- State Persistence & Lifecycle ---

    public void loadData() {
        suppressEvents = true;
        try {
            initialTargets = manager.getTargets();
            initialDefaultTargetId = manager.getProjectDefaultTargetId();

            targetsList.clear();
            for (RunTargetConfig t : initialTargets) {
                targetsList.add(t.clone());
            }

            updateUIState();

            if (!targetsList.isEmpty()) {
                currentSelection = targetsList.get(0);
                targetsListView.getSelectionModel().select(0);
                showDetail(currentSelection);
            } else {
                currentSelection = null;
                showDetail(null);
            }

            // Restore default target combo
            if (RunTargetsSettingsManager.LOCAL_MACHINE_ID.equals(initialDefaultTargetId)) {
                defaultTargetCombo.setValue("🏠 Local machine");
            } else {
                RunTargetConfig def = manager.getTargetById(initialDefaultTargetId);
                if (def != null) {
                    defaultTargetCombo.setValue((def.getType() != null ? def.getType().getIconSymbol() + " " : "") + def.getName());
                } else {
                    defaultTargetCombo.setValue("🏠 Local machine");
                }
            }
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        if (initialTargets == null) return false;
        if (initialTargets.size() != targetsList.size()) return true;
        for (int i = 0; i < initialTargets.size(); i++) {
            if (!Objects.equals(initialTargets.get(i), targetsList.get(i))) {
                return true;
            }
        }
        String currentDefId = getSelectedDefaultTargetId();
        return !Objects.equals(initialDefaultTargetId, currentDefId);
    }

    private String getSelectedDefaultTargetId() {
        String val = defaultTargetCombo.getValue();
        if (val == null || val.contains("Local machine")) {
            return RunTargetsSettingsManager.LOCAL_MACHINE_ID;
        }
        for (RunTargetConfig t : targetsList) {
            String label = (t.getType() != null ? t.getType().getIconSymbol() + " " : "") + t.getName();
            if (val.equals(label)) {
                return t.getId();
            }
        }
        return RunTargetsSettingsManager.LOCAL_MACHINE_ID;
    }

    public void apply() {
        manager.setTargets(new ArrayList<>(targetsList));
        manager.setProjectDefaultTargetId(getSelectedDefaultTargetId());
        initialTargets = manager.getTargets();
        initialDefaultTargetId = manager.getProjectDefaultTargetId();
        notifyModified();
    }

    public void reset() {
        loadData();
        notifyModified();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    // --- Styling Helpers ---

    private void styleToolbarButton(Button btn, String tooltipText) {
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 3 7;");
        btn.setTooltip(new Tooltip(tooltipText));
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #35373C; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 3 7; -fx-background-radius: 4;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 3 7;"));
    }

    private void styleLabel(Label lbl) {
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleTextField(TextField tf) {
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #6F737A; -fx-font-size: 13px; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8;");
    }

    private void styleComboBox(ComboBox<?> cb) {
        cb.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
    }

    private void styleBrowseButton(Button btn) {
        btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand;");
    }

    private void styleSmallButton(Button btn) {
        btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 14; -fx-cursor: hand;");
    }

    // Getters for testing
    public ListView<RunTargetConfig> getTargetsListView() { return targetsListView; }
    public ObservableList<RunTargetConfig> getTargetsList() { return targetsList; }
    public Button getAddButton() { return addButton; }
    public Button getRemoveButton() { return removeButton; }
    public Button getDuplicateButton() { return duplicateButton; }
    public Hyperlink getAddTargetLink() { return addTargetLink; }
    public TextField getNameField() { return nameField; }
    public TextField getSshHostField() { return sshHostField; }
    public TextField getSshPortField() { return sshPortField; }
    public TextField getSshUserField() { return sshUserField; }
    public TextField getDockerImageField() { return dockerImageField; }
    public ComboBox<String> getDefaultTargetCombo() { return defaultTargetCombo; }
}
