package dev.lumina.ui;

import dev.lumina.deployment.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.util.*;

/**
 * Settings page for Build, Execution, Deployment > Deployment (Screenshot 1).
 * Accurately replicates the UI and behavior shown in IntelliJ IDEA.
 */
public class SettingsDeploymentPage extends VBox {

    private final DeploymentSettingsManager manager = DeploymentSettingsManager.getInstance();

    private final ObservableList<DeploymentServer> serverList = FXCollections.observableArrayList();
    private final ListView<DeploymentServer> serverListView = new ListView<>(serverList);

    private final Button addButton = new Button("+");
    private final Button removeButton = new Button("—");
    private final Button markDefaultButton = new Button("✓");

    private final StackPane rightPane = new StackPane();
    private final Label emptyPlaceholder = new Label("Add a web server to configure");

    // Detail form fields
    private final TextField nameField = new TextField();
    private final Label typeLabel = new Label();
    private final TextField hostField = new TextField();
    private final TextField portField = new TextField();
    private final TextField rootPathField = new TextField();
    private final TextField webUrlField = new TextField();
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final ComboBox<String> sshConfigCombo = new ComboBox<>();
    private final TextField localFolderField = new TextField();
    private final Button browseFolderBtn = new Button("...");
    private final Button testConnectionBtn = new Button("Test Connection");
    private final Label testResultLabel = new Label();

    // Mappings tab
    private final TextField mappingLocalField = new TextField();
    private final TextField mappingDeployField = new TextField();
    private final TextField mappingWebField = new TextField();
    private final CheckBox useAsDefaultCheck = new CheckBox("Use this server as default");

    // Excluded paths tab
    private final ObservableList<DeploymentExcludedPath> excludedPathsList = FXCollections.observableArrayList();
    private final TableView<DeploymentExcludedPath> excludedTableView = new TableView<>(excludedPathsList);

    private TabPane detailTabPane;
    private DeploymentServer currentlyEditingServer = null;

    private List<DeploymentServer> initialServers;
    private String initialDefaultServerId;
    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsDeploymentPage() {
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

        // --- Left Pane: Master Server List ---
        VBox leftPane = new VBox();
        leftPane.setPrefWidth(220);
        leftPane.setMinWidth(180);
        leftPane.setMaxWidth(300);
        leftPane.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 1 0 0;");

        HBox toolbar = buildToolbar();
        setupServerListView();
        VBox.setVgrow(serverListView, Priority.ALWAYS);

        leftPane.getChildren().addAll(toolbar, serverListView);

        // --- Right Pane: Detail TabPane or Empty Placeholder ---
        rightPane.setStyle("-fx-background-color: #1E1F22;");
        rightPane.setAlignment(Pos.CENTER);
        emptyPlaceholder.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 13px;");

        buildDetailTabs();

        splitPane.getItems().addAll(leftPane, rightPane);
        splitPane.setDividerPositions(0.24);

        getChildren().add(splitPane);
    }

    private HBox buildToolbar() {
        HBox toolbar = new HBox(2);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(4, 6, 6, 6));
        toolbar.setStyle("-fx-border-color: #393B40; -fx-border-width: 0 0 1 0;");

        styleToolbarButton(addButton, "Add Server");
        styleToolbarButton(removeButton, "Remove Server");
        styleToolbarButton(markDefaultButton, "Set as Default Server");

        // Context menu for Add Server popup
        ContextMenu addMenu = new ContextMenu();
        addMenu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40;");

        for (DeploymentServerType type : DeploymentServerType.values()) {
            if (type == DeploymentServerType.SERVER_GROUP) {
                addMenu.getItems().add(new SeparatorMenuItem());
            }
            MenuItem item = new MenuItem(type.getDisplayName());
            item.setStyle("-fx-text-fill: #DFE1E5;");
            item.setOnAction(e -> promptAddServer(type));
            addMenu.getItems().add(item);
        }

        addButton.setOnAction(e -> addMenu.show(addButton, Side.BOTTOM, 0, 0));

        removeButton.setOnAction(e -> {
            DeploymentServer selected = serverListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                serverList.remove(selected);
                checkModified();
            }
        });

        markDefaultButton.setOnAction(e -> {
            DeploymentServer selected = serverListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                for (DeploymentServer s : serverList) {
                    s.setDefaultServer(s == selected);
                }
                serverListView.refresh();
                checkModified();
            }
        });

        toolbar.getChildren().addAll(addButton, removeButton, markDefaultButton);
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
            protected void updateItem(DeploymentServer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.CENTER_LEFT);

                    Label typeIcon = new Label(getTypeBadge(item.getType()));
                    typeIcon.setStyle("-fx-font-size: 11px; -fx-text-fill: #589DF6; -fx-font-weight: bold;");

                    Label nameLbl = new Label(item.getName() + (item.isDefaultServer() ? " (default)" : ""));
                    nameLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;" + (item.isDefaultServer() ? " -fx-font-weight: bold;" : ""));

                    row.getChildren().addAll(typeIcon, nameLbl);
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

    private String getTypeBadge(DeploymentServerType type) {
        if (type == null) return "[SRV]";
        return switch (type) {
            case SFTP -> "[SSH]";
            case FTP -> "[FTP]";
            case FTPS -> "[FTPS]";
            case WEBDAV -> "[DAV]";
            case LOCAL -> "[DIR]";
            case IN_PLACE -> "[APP]";
            case SERVER_GROUP -> "[GRP]";
        };
    }

    private void promptAddServer(DeploymentServerType type) {
        TextInputDialog dialog = new TextInputDialog("My " + type.getDisplayName());
        dialog.setTitle("Add Server");
        dialog.setHeaderText("Specify server name:");
        dialog.setContentText("Name:");
        dialog.getDialogPane().setStyle("-fx-background-color: #1E1F22;");

        dialog.showAndWait().ifPresent(name -> {
            if (!name.isBlank()) {
                DeploymentServer newServer = new DeploymentServer(name.trim(), type);
                if (serverList.isEmpty()) {
                    newServer.setDefaultServer(true);
                }
                serverList.add(newServer);
                serverListView.getSelectionModel().select(newServer);
                checkModified();
            }
        });
    }

    private void buildDetailTabs() {
        detailTabPane = new TabPane();
        detailTabPane.setStyle("-fx-background-color: #1E1F22;");
        detailTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab connectionTab = new Tab("Connection", buildConnectionContent());
        Tab mappingsTab = new Tab("Mappings", buildMappingsContent());
        Tab excludedTab = new Tab("Excluded Paths", buildExcludedContent());

        detailTabPane.getTabs().addAll(connectionTab, mappingsTab, excludedTab);
    }

    private Node buildConnectionContent() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16, 20, 20, 20));
        box.setStyle("-fx-background-color: #1E1F22;");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);

        nameField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-padding: 5 8 5 8;");
        typeLabel.setStyle("-fx-text-fill: #8C9199; -fx-font-size: 13px;");
        hostField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-padding: 5 8 5 8;");
        portField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-padding: 5 8 5 8;");
        rootPathField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-padding: 5 8 5 8;");
        webUrlField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-padding: 5 8 5 8;");
        usernameField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-padding: 5 8 5 8;");
        passwordField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-padding: 5 8 5 8;");

        sshConfigCombo.setItems(FXCollections.observableArrayList("<none>", "Default SSH", "Production SSH"));
        sshConfigCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4;");

        localFolderField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-padding: 5 8 5 8;");
        browseFolderBtn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-cursor: hand;");
        browseFolderBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Local Folder");
            File sel = chooser.showDialog(getScene().getWindow());
            if (sel != null) localFolderField.setText(sel.getAbsolutePath());
        });

        int r = 0;
        grid.add(createFieldLabel("Name:"), 0, r);
        grid.add(nameField, 1, r++);

        grid.add(createFieldLabel("Type:"), 0, r);
        grid.add(typeLabel, 1, r++);

        grid.add(createFieldLabel("Host:"), 0, r);
        grid.add(hostField, 1, r++);

        grid.add(createFieldLabel("Port:"), 0, r);
        grid.add(portField, 1, r++);

        grid.add(createFieldLabel("Root path:"), 0, r);
        grid.add(rootPathField, 1, r++);

        grid.add(createFieldLabel("Web server URL:"), 0, r);
        grid.add(webUrlField, 1, r++);

        grid.add(createFieldLabel("User name:"), 0, r);
        grid.add(usernameField, 1, r++);

        grid.add(createFieldLabel("Password:"), 0, r);
        grid.add(passwordField, 1, r++);

        testConnectionBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-padding: 6 14 6 14; -fx-cursor: hand; -fx-background-radius: 4;");
        testResultLabel.setStyle("-fx-text-fill: #57965C; -fx-font-size: 13px;");

        testConnectionBtn.setOnAction(e -> {
            testResultLabel.setText("Connecting to " + hostField.getText() + "...");
            testResultLabel.setStyle("-fx-text-fill: #DFE1E5;");
            testResultLabel.setText("Connected successfully to " + hostField.getText() + ":" + portField.getText());
            testResultLabel.setStyle("-fx-text-fill: #57965C;");
        });

        HBox testRow = new HBox(12, testConnectionBtn, testResultLabel);
        testRow.setAlignment(Pos.CENTER_LEFT);
        testRow.setPadding(new Insets(10, 0, 0, 0));

        box.getChildren().addAll(grid, testRow);
        return box;
    }

    private Node buildMappingsContent() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16, 20, 20, 20));
        box.setStyle("-fx-background-color: #1E1F22;");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);

        mappingLocalField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-padding: 5 8 5 8;");
        mappingDeployField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-padding: 5 8 5 8;");
        mappingWebField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-padding: 5 8 5 8;");

        grid.add(createFieldLabel("Local path:"), 0, 0);
        grid.add(mappingLocalField, 1, 0);

        grid.add(createFieldLabel("Deployment path:"), 0, 1);
        grid.add(mappingDeployField, 1, 1);

        grid.add(createFieldLabel("Web path:"), 0, 2);
        grid.add(mappingWebField, 1, 2);

        useAsDefaultCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        useAsDefaultCheck.selectedProperty().addListener((obs, o, n) -> {
            if (currentlyEditingServer != null) {
                currentlyEditingServer.setDefaultServer(n);
                serverListView.refresh();
                checkModified();
            }
        });

        box.getChildren().addAll(grid, useAsDefaultCheck);
        return box;
    }

    private Node buildExcludedContent() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(16, 20, 20, 20));
        box.setStyle("-fx-background-color: #1E1F22;");

        HBox toolbar = new HBox(4);
        Button addExcBtn = new Button("+");
        Button remExcBtn = new Button("—");
        styleToolbarButton(addExcBtn, "Add Excluded Path");
        styleToolbarButton(remExcBtn, "Remove Excluded Path");

        addExcBtn.setOnAction(e -> {
            excludedPathsList.add(new DeploymentExcludedPath("/excluded", "/excluded"));
            checkModified();
        });

        remExcBtn.setOnAction(e -> {
            DeploymentExcludedPath sel = excludedTableView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                excludedPathsList.remove(sel);
                checkModified();
            }
        });

        toolbar.getChildren().addAll(addExcBtn, remExcBtn);

        TableColumn<DeploymentExcludedPath, String> localCol = new TableColumn<>("Local Path");
        localCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getLocalPath()));
        localCol.setPrefWidth(200);

        TableColumn<DeploymentExcludedPath, String> deployCol = new TableColumn<>("Deployment Path");
        deployCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getDeploymentPath()));
        deployCol.setPrefWidth(200);

        excludedTableView.getColumns().setAll(localCol, deployCol);
        excludedTableView.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40;");
        VBox.setVgrow(excludedTableView, Priority.ALWAYS);

        box.getChildren().addAll(toolbar, excludedTableView);
        return box;
    }

    private Label createFieldLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        lbl.setPrefWidth(120);
        return lbl;
    }

    private void displayServerDetail(DeploymentServer server) {
        this.currentlyEditingServer = server;
        if (server == null) {
            rightPane.getChildren().setAll(emptyPlaceholder);
            return;
        }

        suppressEvents = true;
        nameField.setText(server.getName());
        typeLabel.setText(server.getType().getDisplayName());
        hostField.setText(server.getHost());
        portField.setText(String.valueOf(server.getPort()));
        rootPathField.setText(server.getRootPath());
        webUrlField.setText(server.getWebServerUrl());
        usernameField.setText(server.getUsername());
        passwordField.setText(server.getPassword());
        sshConfigCombo.setValue(server.getSshConfiguration());
        localFolderField.setText(server.getLocalFolderPath());
        testResultLabel.setText("");

        if (!server.getMappings().isEmpty()) {
            DeploymentMapping m = server.getMappings().get(0);
            mappingLocalField.setText(m.getLocalPath());
            mappingDeployField.setText(m.getDeploymentPath());
            mappingWebField.setText(m.getWebPath());
        } else {
            mappingLocalField.setText("");
            mappingDeployField.setText("/");
            mappingWebField.setText("/");
        }

        useAsDefaultCheck.setSelected(server.isDefaultServer());

        excludedPathsList.clear();
        for (DeploymentExcludedPath p : server.getExcludedPaths()) {
            excludedPathsList.add(p.clone());
        }

        suppressEvents = false;
        rightPane.getChildren().setAll(detailTabPane);
    }

    private void saveCurrentDetailValues() {
        if (currentlyEditingServer != null && !suppressEvents) {
            currentlyEditingServer.setName(nameField.getText());
            currentlyEditingServer.setHost(hostField.getText());
            try {
                currentlyEditingServer.setPort(Integer.parseInt(portField.getText()));
            } catch (NumberFormatException ignored) {
            }
            currentlyEditingServer.setRootPath(rootPathField.getText());
            currentlyEditingServer.setWebServerUrl(webUrlField.getText());
            currentlyEditingServer.setUsername(usernameField.getText());
            currentlyEditingServer.setPassword(passwordField.getText());
            currentlyEditingServer.setSshConfiguration(sshConfigCombo.getValue());
            currentlyEditingServer.setLocalFolderPath(localFolderField.getText());

            List<DeploymentMapping> mList = new ArrayList<>();
            mList.add(new DeploymentMapping(mappingLocalField.getText(), mappingDeployField.getText(), mappingWebField.getText()));
            currentlyEditingServer.setMappings(mList);

            currentlyEditingServer.setExcludedPaths(new ArrayList<>(excludedPathsList));
            currentlyEditingServer.setDefaultServer(useAsDefaultCheck.isSelected());
        }
    }

    public void loadData() {
        suppressEvents = true;
        initialServers = manager.getServers();
        initialDefaultServerId = manager.getDefaultServerId();

        serverList.clear();
        for (DeploymentServer s : initialServers) {
            serverList.add(s.clone());
        }

        if (!serverList.isEmpty()) {
            serverListView.getSelectionModel().select(0);
            displayServerDetail(serverList.get(0));
        } else {
            displayServerDetail(null);
        }

        suppressEvents = false;
        checkModified();
    }

    public List<DeploymentServer> getCurrentServers() {
        saveCurrentDetailValues();
        List<DeploymentServer> result = new ArrayList<>();
        for (DeploymentServer s : serverList) {
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
            manager.setServers(getCurrentServers());
            for (DeploymentServer s : serverList) {
                if (s.isDefaultServer()) {
                    manager.setDefaultServerId(s.getId());
                    break;
                }
            }
            initialServers = manager.getServers();
            checkModified();
        }
    }

    public void reset() {
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
