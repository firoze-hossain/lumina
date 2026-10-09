package dev.lumina.ui;

import dev.lumina.php.PhpServer;
import dev.lumina.php.PhpSettingsManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Settings page for Languages & Frameworks > PHP > Servers.
 * Faithfully matches Image 3.
 */
public class SettingsLanguagesPhpServersPage extends VBox {

    private final PhpSettingsManager manager = PhpSettingsManager.getInstance();

    private final ObservableList<PhpServer> serversList = FXCollections.observableArrayList();
    private ListView<PhpServer> serversListView;

    // Detail controls
    private TextField nameField;
    private CheckBox sharedCheck;
    private TextField hostField;
    private TextField portField;
    private ComboBox<String> debuggerCombo;
    private CheckBox usePathMappingsCheck;
    private VBox pathMappingsBox;
    private VBox detailContainer;

    private PhpServer currentlyEditingServer = null;
    private boolean isUpdatingFields = false;

    // Initial state snapshot for dirty checking
    private List<PhpServer> initialServers = new ArrayList<>();

    private Runnable onModified;

    public SettingsLanguagesPhpServersPage() {
        setSpacing(0);
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadFromManager();
    }

    public void setOnModified(Runnable onModified) {
        this.onModified = onModified;
    }

    private void notifyModified() {
        if (onModified != null) {
            onModified.run();
        }
    }

    private void buildUI() {
        SplitPane splitPane = new SplitPane();
        splitPane.setStyle("-fx-background-color: #1E1F22; -fx-box-border: transparent;");
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        // ----------------------------------------------------
        // Left Column: Master List
        // ----------------------------------------------------
        VBox leftBox = new VBox();
        leftBox.setMinWidth(180);
        leftBox.setPrefWidth(220);
        leftBox.setMaxWidth(340);
        leftBox.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 1 0 0;");

        // Toolbar: + - copy
        HBox toolbar = new HBox(2);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 8, 6, 8));
        toolbar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 0 0 1 0;");

        Button addBtn = createToolbarButton("+", "Add new server", this::handleAddServer);
        Button removeBtn = createToolbarButton("—", "Remove server", this::handleRemoveServer);
        Button copyBtn = createToolbarButton("📋", "Copy server", this::handleCopyServer);

        toolbar.getChildren().addAll(addBtn, removeBtn, copyBtn);

        serversListView = new ListView<>(serversList);
        serversListView.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent;");
        VBox.setVgrow(serversListView, Priority.ALWAYS);

        serversListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(PhpServer server, boolean empty) {
                super.updateItem(server, empty);
                if (empty || server == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(server.getName());
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 8;");
                    if (isSelected()) {
                        setStyle("-fx-background-color: #2E436E; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-padding: 4 8;");
                    }
                }
            }
        });

        serversListView.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> {
            syncFieldsToServer(ov);
            currentlyEditingServer = nv;
            populateFieldsFromServer(nv);
        });

        leftBox.getChildren().addAll(toolbar, serversListView);

        // ----------------------------------------------------
        // Right Column: Detail Form
        // ----------------------------------------------------
        detailContainer = new VBox(14);
        detailContainer.setPadding(new Insets(14, 20, 20, 20));
        detailContainer.setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(detailContainer, Priority.ALWAYS);

        // Row 1: Name and Shared
        HBox nameRow = new HBox(12);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label("Name:");
        nameLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        nameLabel.setPrefWidth(55);

        nameField = new TextField("Unnamed");
        nameField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-padding: 4 8; -fx-font-size: 13px;");
        nameField.setPrefWidth(300);
        HBox.setHgrow(nameField, Priority.ALWAYS);
        nameField.textProperty().addListener((obs, ov, nv) -> {
            if (!isUpdatingFields && currentlyEditingServer != null) {
                currentlyEditingServer.setName(nv);
                serversListView.refresh();
                notifyModified();
            }
        });

        sharedCheck = new CheckBox("Shared");
        sharedCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        sharedCheck.selectedProperty().addListener((obs, ov, nv) -> {
            if (!isUpdatingFields && currentlyEditingServer != null) {
                currentlyEditingServer.setShared(nv);
                notifyModified();
            }
        });

        nameRow.getChildren().addAll(nameLabel, nameField, sharedCheck);

        // Row 2: Host : Port Debugger
        HBox hostPortRow = new HBox(12);
        hostPortRow.setAlignment(Pos.CENTER_LEFT);

        Label hostLabel = new Label("Host:");
        hostLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        hostLabel.setPrefWidth(55);

        hostField = new TextField("");
        hostField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-padding: 4 8; -fx-font-size: 13px;");
        hostField.setPrefWidth(180);
        HBox.setHgrow(hostField, Priority.ALWAYS);
        hostField.textProperty().addListener((obs, ov, nv) -> {
            if (!isUpdatingFields && currentlyEditingServer != null) {
                currentlyEditingServer.setHost(nv);
                notifyModified();
            }
        });

        Label colonLabel = new Label(":");
        colonLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label portLabel = new Label("Port");
        portLabel.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px;");

        portField = new TextField("80");
        portField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-padding: 4 8; -fx-font-size: 13px;");
        portField.setPrefWidth(60);
        portField.textProperty().addListener((obs, ov, nv) -> {
            if (!isUpdatingFields && currentlyEditingServer != null) {
                try {
                    int p = Integer.parseInt(nv.trim());
                    currentlyEditingServer.setPort(p);
                    notifyModified();
                } catch (Exception ignored) {}
            }
        });

        VBox portBox = new VBox(2, portLabel, portField);

        Label debuggerLabel = new Label("Debugger");
        debuggerLabel.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px;");

        debuggerCombo = new ComboBox<>(FXCollections.observableArrayList("Xdebug", "Zend Debugger"));
        debuggerCombo.setValue("Xdebug");
        debuggerCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-font-size: 13px;");
        debuggerCombo.setPrefWidth(140);
        debuggerCombo.valueProperty().addListener((obs, ov, nv) -> {
            if (!isUpdatingFields && currentlyEditingServer != null && nv != null) {
                currentlyEditingServer.setDebugger(nv);
                notifyModified();
            }
        });

        VBox debuggerBox = new VBox(2, debuggerLabel, debuggerCombo);

        hostPortRow.getChildren().addAll(hostLabel, hostField, colonLabel, portBox, debuggerBox);

        // Row 3: Use path mappings
        usePathMappingsCheck = new CheckBox("Use path mappings (select if the server is remote or symlinks are used)");
        usePathMappingsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        usePathMappingsCheck.selectedProperty().addListener((obs, ov, nv) -> {
            pathMappingsBox.setVisible(nv);
            pathMappingsBox.setManaged(nv);
            if (!isUpdatingFields && currentlyEditingServer != null) {
                currentlyEditingServer.setUsePathMappings(nv);
                notifyModified();
            }
        });

        // Path mappings table container
        pathMappingsBox = new VBox(4);
        pathMappingsBox.setVisible(false);
        pathMappingsBox.setManaged(false);
        HBox mappingTableHeader = new HBox(8);
        mappingTableHeader.setPadding(new Insets(4, 8, 4, 8));
        mappingTableHeader.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 1;");

        Label col1 = new Label("File/Directory");
        col1.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 11px; -fx-font-weight: bold;");
        col1.setPrefWidth(220);

        Label col2 = new Label("Absolute path on the server");
        col2.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 11px; -fx-font-weight: bold;");
        HBox.setHgrow(col2, Priority.ALWAYS);

        mappingTableHeader.getChildren().addAll(col1, col2);

        Label mappingEmpty = new Label("Project root mappings will be automatically configured for debugging.");
        mappingEmpty.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 12px; -fx-padding: 8;");
        pathMappingsBox.getChildren().addAll(mappingTableHeader, mappingEmpty);

        detailContainer.getChildren().addAll(nameRow, hostPortRow, usePathMappingsCheck, pathMappingsBox);

        splitPane.getItems().addAll(leftBox, detailContainer);
        splitPane.setDividerPositions(0.3);

        getChildren().add(splitPane);
    }

    private Button createToolbarButton(String text, String tooltip, Runnable action) {
        Button btn = new Button(text);
        btn.setTooltip(new Tooltip(tooltip));
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-cursor: hand; -fx-background-radius: 3;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-cursor: hand;"));
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void handleAddServer() {
        int nextNum = serversList.size() + 1;
        String name = nextNum == 1 ? "Unnamed" : "Unnamed (" + nextNum + ")";
        PhpServer newServer = new PhpServer(UUID.randomUUID().toString(), name, false, "", 80, "Xdebug", false);
        serversList.add(newServer);
        serversListView.getSelectionModel().select(newServer);
        notifyModified();
    }

    private void handleRemoveServer() {
        int idx = serversListView.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            serversList.remove(idx);
            if (!serversList.isEmpty()) {
                int nextSel = Math.min(idx, serversList.size() - 1);
                serversListView.getSelectionModel().select(nextSel);
            } else {
                currentlyEditingServer = null;
                clearFields();
            }
            notifyModified();
        }
    }

    private void handleCopyServer() {
        PhpServer selected = serversListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            PhpServer copy = selected.copy();
            copy.setId(UUID.randomUUID().toString());
            copy.setName(selected.getName() + " (Copy)");
            serversList.add(copy);
            serversListView.getSelectionModel().select(copy);
            notifyModified();
        }
    }

    private void populateFieldsFromServer(PhpServer server) {
        isUpdatingFields = true;
        try {
            if (server != null) {
                detailContainer.setDisable(false);
                nameField.setText(server.getName());
                sharedCheck.setSelected(server.isShared());
                hostField.setText(server.getHost());
                portField.setText(String.valueOf(server.getPort()));
                debuggerCombo.setValue(server.getDebugger());
                usePathMappingsCheck.setSelected(server.isUsePathMappings());
                pathMappingsBox.setVisible(server.isUsePathMappings());
                pathMappingsBox.setManaged(server.isUsePathMappings());
            } else {
                detailContainer.setDisable(true);
                clearFields();
            }
        } finally {
            isUpdatingFields = false;
        }
    }

    private void syncFieldsToServer(PhpServer server) {
        if (server != null && !isUpdatingFields) {
            server.setName(nameField.getText().trim());
            server.setShared(sharedCheck.isSelected());
            server.setHost(hostField.getText().trim());
            try {
                server.setPort(Integer.parseInt(portField.getText().trim()));
            } catch (Exception ignored) {}
            if (debuggerCombo.getValue() != null) {
                server.setDebugger(debuggerCombo.getValue());
            }
            server.setUsePathMappings(usePathMappingsCheck.isSelected());
        }
    }

    private void clearFields() {
        nameField.setText("");
        sharedCheck.setSelected(false);
        hostField.setText("");
        portField.setText("80");
        debuggerCombo.setValue("Xdebug");
        usePathMappingsCheck.setSelected(false);
        pathMappingsBox.setVisible(false);
        pathMappingsBox.setManaged(false);
    }

    public void loadFromManager() {
        List<PhpServer> loaded = manager.getServers();
        serversList.clear();
        for (PhpServer s : loaded) {
            serversList.add(s.copy());
        }

        initialServers = new ArrayList<>();
        for (PhpServer s : loaded) {
            initialServers.add(s.copy());
        }

        if (!serversList.isEmpty()) {
            serversListView.getSelectionModel().select(0);
        } else {
            clearFields();
        }
    }

    public boolean isModified() {
        syncFieldsToServer(currentlyEditingServer);
        if (serversList.size() != initialServers.size()) return true;
        for (int i = 0; i < serversList.size(); i++) {
            if (!serversList.get(i).equals(initialServers.get(i))) return true;
        }
        return false;
    }

    public void apply() {
        syncFieldsToServer(currentlyEditingServer);
        List<PhpServer> toSave = new ArrayList<>();
        for (PhpServer s : serversList) {
            toSave.add(s.copy());
        }
        manager.setServers(toSave);

        initialServers = new ArrayList<>();
        for (PhpServer s : toSave) {
            initialServers.add(s.copy());
        }
    }

    public void reset() {
        loadFromManager();
    }

    public void revertChanges() {
        reset();
    }
}
