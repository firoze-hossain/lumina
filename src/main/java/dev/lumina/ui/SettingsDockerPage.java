package dev.lumina.ui;

import dev.lumina.docker.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;

/**
 * Settings page for Build, Execution, Deployment > Docker (Screenshot 3).
 * Accurately replicates the UI and behavior shown in IntelliJ IDEA.
 */
public class SettingsDockerPage extends VBox {

    private final DockerSettingsManager manager = DockerSettingsManager.getInstance();

    private final ObservableList<DockerServerConfig> serverList = FXCollections.observableArrayList();
    private final ListView<DockerServerConfig> serverListView = new ListView<>(serverList);

    private final Button addButton = new Button("+");
    private final Button removeButton = new Button("—");

    private final TextField nameField = new TextField("Docker");
    private final CheckBox detectPathsAutoCheck = new CheckBox("Detect executable paths automatically");

    // Daemon connection type radio group
    private final ToggleGroup daemonTypeGroup = new ToggleGroup();
    private final RadioButton unixSocketRadio = new RadioButton("Unix socket:");
    private final ComboBox<String> unixSocketCombo = new ComboBox<>();
    private final Button refreshDaemonBtn = new Button("🔄");

    private final RadioButton tcpSocketRadio = new RadioButton("TCP socket");
    private final TextField tcpEngineApiUrlField = new TextField("tcp://localhost:2375");
    private final TextField tcpCertsFolderField = new TextField();
    private final Button browseCertsBtn = new Button("...");

    private final RadioButton podmanRadio = new RadioButton("Podman");
    private final Label podmanBetaBadge = new Label("Beta");

    private final RadioButton dindRadio = new RadioButton("Docker-in-Docker");

    private final RadioButton sshRadio = new RadioButton("SSH:");
    private final ComboBox<String> sshConfigCombo = new ComboBox<>();
    private final Button sshConfigMoreBtn = new Button("...");

    private final Label connectionStatusLabel = new Label();

    // Path mappings section
    private final ObservableList<DockerPathMapping> pathMappingsList = FXCollections.observableArrayList();
    private final TableView<DockerPathMapping> pathMappingsTable = new TableView<>(pathMappingsList);
    private final Button addMappingBtn = new Button("+");
    private final Button removeMappingBtn = new Button("—");
    private final Button editMappingBtn = new Button("✏");

    private DockerServerConfig currentlyEditing = null;
    private List<DockerServerConfig> initialServers;
    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsDockerPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 20, 20, 20));
        setSpacing(10);
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadData();
    }

    private void buildUI() {
        SplitPane splitPane = new SplitPane();
        splitPane.setStyle("-fx-background-color: transparent; -fx-box-border: transparent;");
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        // --- Left Sidebar: Master List of Docker Servers ---
        VBox leftPane = new VBox();
        leftPane.setPrefWidth(200);
        leftPane.setMinWidth(160);
        leftPane.setMaxWidth(260);
        leftPane.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 1 0 0;");

        HBox toolbar = buildToolbar();
        setupServerListView();
        VBox.setVgrow(serverListView, Priority.ALWAYS);

        leftPane.getChildren().addAll(toolbar, serverListView);

        // --- Right Pane: Detail Editor ---
        ScrollPane rightScroll = new ScrollPane();
        rightScroll.setFitToWidth(true);
        rightScroll.setStyle("-fx-background-color: transparent; -fx-background: #1E1F22; -fx-border-color: transparent;");

        VBox rightContent = buildDetailContent();
        rightScroll.setContent(rightContent);

        splitPane.getItems().addAll(leftPane, rightScroll);
        splitPane.setDividerPositions(0.22);

        getChildren().add(splitPane);
    }

    private HBox buildToolbar() {
        HBox toolbar = new HBox(2);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(4, 6, 6, 6));
        toolbar.setStyle("-fx-border-color: #393B40; -fx-border-width: 0 0 1 0;");

        styleToolbarButton(addButton, "Add Docker Connection");
        styleToolbarButton(removeButton, "Remove Docker Connection");

        addButton.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog("Docker " + (serverList.size() + 1));
            dialog.setTitle("Add Docker Connection");
            dialog.setHeaderText("Specify Docker server name:");
            dialog.setContentText("Name:");
            dialog.getDialogPane().setStyle("-fx-background-color: #1E1F22;");
            dialog.showAndWait().ifPresent(name -> {
                if (!name.isBlank()) {
                    DockerServerConfig cfg = new DockerServerConfig(name.trim());
                    serverList.add(cfg);
                    serverListView.getSelectionModel().select(cfg);
                    checkModified();
                }
            });
        });

        removeButton.setOnAction(e -> {
            DockerServerConfig sel = serverListView.getSelectionModel().getSelectedItem();
            if (sel != null && serverList.size() > 1) {
                serverList.remove(sel);
                checkModified();
            }
        });

        toolbar.getChildren().addAll(addButton, removeButton);
        return toolbar;
    }

    private void styleToolbarButton(Button btn, String tooltipText) {
        btn.setTooltip(new Tooltip(tooltipText));
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 3 8 3 8;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #35373B; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 3 8 3 8;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 3 8 3 8;"));
    }

    private void setupServerListView() {
        serverListView.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        serverListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(DockerServerConfig item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.CENTER_LEFT);

                    Label icon = new Label("🐳");
                    icon.setStyle("-fx-font-size: 13px;");

                    Label nameLbl = new Label(item.getName());
                    nameLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

                    row.getChildren().addAll(icon, nameLbl);
                    setGraphic(row);
                    setText(null);

                    if (isSelected()) {
                        setStyle("-fx-background-color: #2E436E; -fx-background-radius: 4;");
                    } else {
                        setStyle("-fx-background-color: transparent;");
                    }
                }
            }
        });

        serverListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            saveCurrentDetailValues();
            displayServerDetail(newVal);
        });
    }

    private VBox buildDetailContent() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16, 24, 24, 24));
        box.setStyle("-fx-background-color: #1E1F22;");

        // 1. Name row
        HBox nameRow = new HBox(12);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLbl = new Label("Name:");
        nameLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        nameLbl.setPrefWidth(60);

        nameField.setPrefWidth(350);
        styleTextField(nameField);
        nameField.textProperty().addListener((obs, o, n) -> checkModified());
        nameRow.getChildren().addAll(nameLbl, nameField);

        // 2. Detect executable paths automatically
        styleCheckBox(detectPathsAutoCheck);

        // 3. Connect to Docker daemon with header
        Label daemonHeader = new Label("Connect to Docker daemon with:");
        daemonHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
        VBox.setMargin(daemonHeader, new Insets(8, 0, 0, 0));

        // Setup radio group
        unixSocketRadio.setToggleGroup(daemonTypeGroup);
        tcpSocketRadio.setToggleGroup(daemonTypeGroup);
        podmanRadio.setToggleGroup(daemonTypeGroup);
        dindRadio.setToggleGroup(daemonTypeGroup);
        sshRadio.setToggleGroup(daemonTypeGroup);

        styleRadioButton(unixSocketRadio);
        styleRadioButton(tcpSocketRadio);
        styleRadioButton(podmanRadio);
        styleRadioButton(dindRadio);
        styleRadioButton(sshRadio);

        daemonTypeGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            updateDaemonRadioEnablement();
            testConnection();
            checkModified();
        });

        // Unix socket row
        HBox unixRow = new HBox(12);
        unixRow.setAlignment(Pos.CENTER_LEFT);
        unixSocketRadio.setPrefWidth(120);
        unixSocketCombo.getItems().addAll("default unix:///var/run/docker.sock", "unix:///run/user/1000/docker.sock");
        unixSocketCombo.setValue("default unix:///var/run/docker.sock");
        styleComboBox(unixSocketCombo);
        unixSocketCombo.setPrefWidth(320);

        refreshDaemonBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-cursor: hand;");
        refreshDaemonBtn.setOnAction(e -> testConnection());

        unixRow.getChildren().addAll(unixSocketRadio, unixSocketCombo, refreshDaemonBtn);

        // TCP socket section
        VBox tcpBox = new VBox(8);
        tcpBox.setPadding(new Insets(2, 0, 2, 20));

        HBox tcpUrlRow = new HBox(12);
        tcpUrlRow.setAlignment(Pos.CENTER_LEFT);
        Label tcpUrlLbl = new Label("Engine API URL:");
        tcpUrlLbl.setStyle("-fx-text-fill: #8C9199; -fx-font-size: 13px;");
        tcpUrlLbl.setPrefWidth(140);
        styleTextField(tcpEngineApiUrlField);
        tcpEngineApiUrlField.setPrefWidth(260);
        tcpEngineApiUrlField.textProperty().addListener((obs, o, n) -> checkModified());
        tcpUrlRow.getChildren().addAll(tcpUrlLbl, tcpEngineApiUrlField);

        HBox tcpCertsRow = new HBox(12);
        tcpCertsRow.setAlignment(Pos.CENTER_LEFT);
        Label tcpCertsLbl = new Label("Certificates folder:");
        tcpCertsLbl.setStyle("-fx-text-fill: #8C9199; -fx-font-size: 13px;");
        tcpCertsLbl.setPrefWidth(140);
        styleTextField(tcpCertsFolderField);
        tcpCertsFolderField.setPrefWidth(260);
        tcpCertsFolderField.textProperty().addListener((obs, o, n) -> checkModified());

        browseCertsBtn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-cursor: hand;");
        browseCertsBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Certificates Folder");
            File sel = chooser.showDialog(getScene().getWindow());
            if (sel != null) tcpCertsFolderField.setText(sel.getAbsolutePath());
        });
        tcpCertsRow.getChildren().addAll(tcpCertsLbl, tcpCertsFolderField, browseCertsBtn);

        tcpBox.getChildren().addAll(tcpUrlRow, tcpCertsRow);

        // Podman row with Beta badge
        HBox podmanRow = new HBox(8);
        podmanRow.setAlignment(Pos.CENTER_LEFT);
        podmanBetaBadge.setStyle("-fx-background-color: #382451; -fx-text-fill: #C792EA; -fx-font-size: 10px; -fx-padding: 1 5 1 5; -fx-background-radius: 4;");
        podmanRow.getChildren().addAll(podmanRadio, podmanBetaBadge);

        // SSH row
        HBox sshRow = new HBox(12);
        sshRow.setAlignment(Pos.CENTER_LEFT);
        sshRadio.setPrefWidth(60);
        sshConfigCombo.getItems().addAll("<create configuration>", "SSH Docker Host");
        sshConfigCombo.setValue("<create configuration>");
        styleComboBox(sshConfigCombo);
        sshConfigCombo.setPrefWidth(200);

        sshConfigMoreBtn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-cursor: hand;");
        sshRow.getChildren().addAll(sshRadio, sshConfigCombo, sshConfigMoreBtn);

        // Connection status message
        connectionStatusLabel.setWrapText(true);
        connectionStatusLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        connectionStatusLabel.setPrefWidth(650);
        VBox.setMargin(connectionStatusLabel, new Insets(6, 0, 8, 0));

        // 4. Path mappings section
        VBox mappingsSection = buildPathMappingsSection();

        box.getChildren().addAll(
                nameRow,
                detectPathsAutoCheck,
                daemonHeader,
                unixRow,
                tcpSocketRadio,
                tcpBox,
                podmanRow,
                dindRadio,
                sshRow,
                connectionStatusLabel,
                mappingsSection
        );

        return box;
    }

    private void updateDaemonRadioEnablement() {
        boolean isUnix = unixSocketRadio.isSelected();
        unixSocketCombo.setDisable(!isUnix);
        refreshDaemonBtn.setDisable(!isUnix);

        boolean isTcp = tcpSocketRadio.isSelected();
        tcpEngineApiUrlField.setDisable(!isTcp);
        tcpCertsFolderField.setDisable(!isTcp);
        browseCertsBtn.setDisable(!isTcp);

        boolean isSsh = sshRadio.isSelected();
        sshConfigCombo.setDisable(!isSsh);
        sshConfigMoreBtn.setDisable(!isSsh);
    }

    private VBox buildPathMappingsSection() {
        VBox section = new VBox(8);
        section.setStyle("-fx-border-color: #393B40; -fx-border-width: 1 0 0 0; -fx-padding: 12 0 0 0;");

        HBox bar = new HBox(6);
        bar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Path mappings");
        title.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        styleToolbarButton(addMappingBtn, "Add Path Mapping");
        styleToolbarButton(removeMappingBtn, "Remove Path Mapping");
        styleToolbarButton(editMappingBtn, "Edit Path Mapping");

        addMappingBtn.setOnAction(e -> showPathMappingDialog(null));
        editMappingBtn.setOnAction(e -> {
            DockerPathMapping sel = pathMappingsTable.getSelectionModel().getSelectedItem();
            if (sel != null) showPathMappingDialog(sel);
        });
        removeMappingBtn.setOnAction(e -> {
            DockerPathMapping sel = pathMappingsTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                pathMappingsList.remove(sel);
                checkModified();
            }
        });

        bar.getChildren().addAll(title, spacer, addMappingBtn, removeMappingBtn, editMappingBtn);

        TableColumn<DockerPathMapping, String> vmCol = new TableColumn<>("Virtual machine path");
        vmCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getVirtualMachinePath()));
        vmCol.setPrefWidth(260);

        TableColumn<DockerPathMapping, String> localCol = new TableColumn<>("Local path");
        localCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getLocalPath()));
        localCol.setPrefWidth(260);

        pathMappingsTable.getColumns().setAll(vmCol, localCol);
        pathMappingsTable.setPlaceholder(new Label("Nothing to show"));
        pathMappingsTable.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40;");
        pathMappingsTable.setPrefHeight(160);

        section.getChildren().addAll(bar, pathMappingsTable);
        return section;
    }

    private void showPathMappingDialog(DockerPathMapping existing) {
        Dialog<DockerPathMapping> dialog = new Dialog<>();
        dialog.setTitle("Path mapping");
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (getScene() != null && getScene().getWindow() != null) {
            dialog.initOwner(getScene().getWindow());
        }

        VBox content = new VBox(12);
        content.setPadding(new Insets(16, 20, 16, 20));
        content.setStyle("-fx-background-color: #1E1F22;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        Label vmLabel = new Label("Virtual machine path:");
        vmLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        TextField vmField = new TextField(existing != null ? existing.getVirtualMachinePath() : "");
        styleTextField(vmField);
        vmField.setPrefWidth(280);

        Label localLabel = new Label("Local path:");
        localLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        TextField localField = new TextField(existing != null ? existing.getLocalPath() : "");
        styleTextField(localField);
        localField.setPrefWidth(240);

        Button browseBtn = new Button("...");
        styleToolbarButton(browseBtn, "Browse Local Path");
        browseBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Local Path");
            File sel = dc.showDialog(dialog.getDialogPane().getScene().getWindow());
            if (sel != null) localField.setText(sel.getAbsolutePath());
        });

        HBox localBox = new HBox(6, localField, browseBtn);

        grid.add(vmLabel, 0, 0);
        grid.add(vmField, 1, 0);
        grid.add(localLabel, 0, 1);
        grid.add(localBox, 1, 1);

        content.getChildren().add(grid);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color: #1E1F22;");

        ButtonType okType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, cancelType);

        dialog.setResultConverter(btn -> {
            if (btn == okType) {
                return new DockerPathMapping(vmField.getText(), localField.getText());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(res -> {
            if (existing != null) {
                existing.setVirtualMachinePath(res.getVirtualMachinePath());
                existing.setLocalPath(res.getLocalPath());
                pathMappingsTable.refresh();
            } else {
                pathMappingsList.add(res);
            }
            checkModified();
        });
    }

    private void styleTextField(TextField tf) {
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-padding: 5 8 5 8;");
    }

    private void styleComboBox(ComboBox<String> cb) {
        cb.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4;");
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        cb.selectedProperty().addListener((obs, o, n) -> checkModified());
    }

    private void styleRadioButton(RadioButton rb) {
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void testConnection() {
        DockerServerConfig temp = getCurrentDetailValues();
        String result = manager.testDockerDaemonConnection(temp);
        connectionStatusLabel.setText(result);
        if ("Connection successful".equals(result)) {
            connectionStatusLabel.setStyle("-fx-text-fill: #57965C; -fx-font-size: 12px;");
        } else {
            connectionStatusLabel.setStyle("-fx-text-fill: #E06C75; -fx-font-size: 12px;");
        }
    }

    private void displayServerDetail(DockerServerConfig config) {
        this.currentlyEditing = config;
        if (config == null) return;

        suppressEvents = true;
        nameField.setText(config.getName());
        detectPathsAutoCheck.setSelected(config.isDetectExecutablePathsAutomatically());

        switch (config.getDaemonType()) {
            case UNIX_SOCKET -> unixSocketRadio.setSelected(true);
            case TCP_SOCKET -> tcpSocketRadio.setSelected(true);
            case PODMAN -> podmanRadio.setSelected(true);
            case DOCKER_IN_DOCKER -> dindRadio.setSelected(true);
            case SSH -> sshRadio.setSelected(true);
        }

        unixSocketCombo.setValue(config.getUnixSocketPath());
        tcpEngineApiUrlField.setText(config.getTcpEngineApiUrl());
        tcpCertsFolderField.setText(config.getTcpCertificatesFolder());
        sshConfigCombo.setValue(config.getSshConfiguration());

        pathMappingsList.clear();
        for (DockerPathMapping m : config.getPathMappings()) {
            pathMappingsList.add(m.clone());
        }

        updateDaemonRadioEnablement();
        testConnection();

        suppressEvents = false;
    }

    private DockerServerConfig getCurrentDetailValues() {
        DockerServerConfig cfg = new DockerServerConfig();
        cfg.setName(nameField.getText());
        cfg.setDetectExecutablePathsAutomatically(detectPathsAutoCheck.isSelected());

        if (unixSocketRadio.isSelected()) cfg.setDaemonType(DockerDaemonType.UNIX_SOCKET);
        else if (tcpSocketRadio.isSelected()) cfg.setDaemonType(DockerDaemonType.TCP_SOCKET);
        else if (podmanRadio.isSelected()) cfg.setDaemonType(DockerDaemonType.PODMAN);
        else if (dindRadio.isSelected()) cfg.setDaemonType(DockerDaemonType.DOCKER_IN_DOCKER);
        else if (sshRadio.isSelected()) cfg.setDaemonType(DockerDaemonType.SSH);

        cfg.setUnixSocketPath(unixSocketCombo.getValue());
        cfg.setTcpEngineApiUrl(tcpEngineApiUrlField.getText());
        cfg.setTcpCertificatesFolder(tcpCertsFolderField.getText());
        cfg.setSshConfiguration(sshConfigCombo.getValue());

        List<DockerPathMapping> mList = new ArrayList<>();
        for (DockerPathMapping m : pathMappingsList) {
            mList.add(m.clone());
        }
        cfg.setPathMappings(mList);
        return cfg;
    }

    private void saveCurrentDetailValues() {
        if (currentlyEditing != null && !suppressEvents) {
            DockerServerConfig updated = getCurrentDetailValues();
            currentlyEditing.setName(updated.getName());
            currentlyEditing.setDetectExecutablePathsAutomatically(updated.isDetectExecutablePathsAutomatically());
            currentlyEditing.setDaemonType(updated.getDaemonType());
            currentlyEditing.setUnixSocketPath(updated.getUnixSocketPath());
            currentlyEditing.setTcpEngineApiUrl(updated.getTcpEngineApiUrl());
            currentlyEditing.setTcpCertificatesFolder(updated.getTcpCertificatesFolder());
            currentlyEditing.setSshConfiguration(updated.getSshConfiguration());
            currentlyEditing.setPathMappings(updated.getPathMappings());
        }
    }

    public void loadData() {
        suppressEvents = true;
        initialServers = manager.getDockerServers();

        serverList.clear();
        for (DockerServerConfig s : initialServers) {
            serverList.add(s.clone());
        }

        if (!serverList.isEmpty()) {
            serverListView.getSelectionModel().select(0);
            displayServerDetail(serverList.get(0));
        }

        suppressEvents = false;
        checkModified();
    }

    public List<DockerServerConfig> getCurrentServers() {
        saveCurrentDetailValues();
        List<DockerServerConfig> result = new ArrayList<>();
        for (DockerServerConfig s : serverList) {
            result.add(s.clone());
        }
        return result;
    }

    public boolean isModified() {
        if (initialServers == null) return false;
        saveCurrentDetailValues();
        return !Objects.equals(initialServers, serverList);
    }

    public void apply() {
        if (isModified()) {
            saveCurrentDetailValues();
            manager.setDockerServers(getCurrentServers());
            initialServers = manager.getDockerServers();
            checkModified();
        }
    }

    public void reset() {
        loadData();
    }

    public void revert() {
        manager.resetToDefaults();
        loadData();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void checkModified() {
        if (suppressEvents) return;
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }
}
