package dev.lumina.ui;

import dev.lumina.tools.JupyterServerConfig;
import dev.lumina.tools.JupyterServersSettings;
import dev.lumina.tools.JupyterServersSettingsManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Tools > Jupyter > Jupyter Servers settings page matching Image 5.
 */
public class SettingsToolsJupyterServersPage extends VBox {

    private final JupyterServersSettingsManager manager;
    private JupyterServersSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    // Left pane
    private ListView<JupyterServerConfig> serversList;
    private ObservableList<JupyterServerConfig> serversItems;
    private Button addBtn;
    private Button removeBtn;

    // Right pane
    private TextField nameField;
    private CheckBox autodetectModeCheck;
    private Label descriptionLabel;
    private VBox manualConfigBox;
    private TextField urlField;
    private TextField tokenField;

    public SettingsToolsJupyterServersPage() {
        this.manager = JupyterServersSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        // Split master-detail
        HBox masterDetail = new HBox(16);
        VBox.setVgrow(masterDetail, Priority.ALWAYS);

        // 1. Left pane
        VBox leftPane = new VBox(8);
        leftPane.setPrefWidth(240);
        leftPane.setMinWidth(200);

        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        Label serversLabel = new Label("Servers:");
        serversLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
        HBox.setHgrow(serversLabel, Priority.ALWAYS);

        addBtn = createToolbarButton("+");
        removeBtn = createToolbarButton("—");
        headerRow.getChildren().addAll(serversLabel, addBtn, removeBtn);

        // Setup add menu popup
        ContextMenu addMenu = new ContextMenu();
        MenuItem ideManagedItem = new MenuItem("IDE-Managed Server");
        MenuItem externalItem = new MenuItem("External Server");
        MenuItem localItem = new MenuItem("Running Local Server");
        addMenu.getItems().addAll(ideManagedItem, externalItem, localItem);

        ideManagedItem.setOnAction(e -> addNewServer("IDE-Managed Server", JupyterServerConfig.TYPE_IDE_MANAGED, true));
        externalItem.setOnAction(e -> addNewServer("External Server", JupyterServerConfig.TYPE_EXTERNAL, false));
        localItem.setOnAction(e -> addNewServer("Running Local Server", JupyterServerConfig.TYPE_RUNNING_LOCAL, false));

        addBtn.setOnAction(e -> addMenu.show(addBtn, javafx.geometry.Side.BOTTOM, 0, 0));

        serversList = new ListView<>();
        serversList.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(serversList, Priority.ALWAYS);

        serversItems = FXCollections.observableArrayList();
        serversList.setItems(serversItems);

        serversList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(JupyterServerConfig item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    HBox cell = new HBox(8);
                    cell.setAlignment(Pos.CENTER_LEFT);

                    Label icon = new Label("square;");
                    icon.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px;");

                    Label name = new Label(item.getName());
                    name.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
                    HBox.setHgrow(name, Priority.ALWAYS);

                    cell.getChildren().addAll(icon, name);
                    if (item.isAutodetectedExecutionMode()) {
                        Label badge = new Label("Auto");
                        badge.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 11px;");
                        cell.getChildren().add(badge);
                    }

                    setGraphic(cell);
                    setText(null);
                    setStyle("-fx-background-color: transparent; -fx-padding: 4 6;");
                }
            }
        });

        leftPane.getChildren().addAll(headerRow, serversList);

        // 2. Right pane
        VBox rightPane = new VBox(12);
        HBox.setHgrow(rightPane, Priority.ALWAYS);
        rightPane.setPadding(new Insets(24, 0, 0, 16));

        HBox nameRow = new HBox(12);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label("Name:");
        nameLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        nameLabel.setPrefWidth(60);

        nameField = new TextField();
        nameField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8;");
        HBox.setHgrow(nameField, Priority.ALWAYS);
        nameRow.getChildren().addAll(nameLabel, nameField);

        autodetectModeCheck = new CheckBox("Autodetected execution mode");
        autodetectModeCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        descriptionLabel = new Label("The IDE Server automatically selects the execution method for Jupyter Notebooks. To configure the server manually, clear this checkbox and adjust the settings.");
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px; -fx-line-spacing: 2px;");

        // Manual configuration fields (for non-autodetected external servers)
        manualConfigBox = new VBox(8);
        HBox urlRow = new HBox(12);
        urlRow.setAlignment(Pos.CENTER_LEFT);
        Label urlLabel = new Label("URL:");
        urlLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        urlLabel.setPrefWidth(60);
        urlField = new TextField();
        urlField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8;");
        HBox.setHgrow(urlField, Priority.ALWAYS);
        urlRow.getChildren().addAll(urlLabel, urlField);

        HBox tokenRow = new HBox(12);
        tokenRow.setAlignment(Pos.CENTER_LEFT);
        Label tokenLabel = new Label("Token:");
        tokenLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        tokenLabel.setPrefWidth(60);
        tokenField = new TextField();
        tokenField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8;");
        HBox.setHgrow(tokenField, Priority.ALWAYS);
        tokenRow.getChildren().addAll(tokenLabel, tokenField);

        manualConfigBox.getChildren().addAll(urlRow, tokenRow);

        rightPane.getChildren().addAll(nameRow, autodetectModeCheck, descriptionLabel, manualConfigBox);

        masterDetail.getChildren().addAll(leftPane, rightPane);
        getChildren().add(masterDetail);

        setupListeners();
    }

    private void setupListeners() {
        serversList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                populateRightPane(newV);
            }
        });

        removeBtn.setOnAction(e -> {
            JupyterServerConfig sel = serversList.getSelectionModel().getSelectedItem();
            if (sel != null && serversItems.size() > 1) {
                serversItems.remove(sel);
                notifyModified();
            }
        });

        nameField.textProperty().addListener((obs, oldV, newV) -> {
            if (!updating) {
                JupyterServerConfig sel = serversList.getSelectionModel().getSelectedItem();
                if (sel != null && !Objects.equals(sel.getName(), newV)) {
                    sel.setName(newV);
                    serversList.refresh();
                    notifyModified();
                }
            }
        });

        autodetectModeCheck.setOnAction(e -> {
            if (!updating) {
                JupyterServerConfig sel = serversList.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    sel.setAutodetectedExecutionMode(autodetectModeCheck.isSelected());
                    manualConfigBox.setVisible(!autodetectModeCheck.isSelected());
                    manualConfigBox.setManaged(!autodetectModeCheck.isSelected());
                    serversList.refresh();
                    notifyModified();
                }
            }
        });

        urlField.textProperty().addListener((obs, oldV, newV) -> {
            if (!updating) {
                JupyterServerConfig sel = serversList.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    sel.setUrl(newV);
                    notifyModified();
                }
            }
        });

        tokenField.textProperty().addListener((obs, oldV, newV) -> {
            if (!updating) {
                JupyterServerConfig sel = serversList.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    sel.setToken(newV);
                    notifyModified();
                }
            }
        });
    }

    private void populateRightPane(JupyterServerConfig s) {
        updating = true;
        try {
            nameField.setText(s.getName());
            autodetectModeCheck.setSelected(s.isAutodetectedExecutionMode());
            urlField.setText(s.getUrl());
            tokenField.setText(s.getToken());

            boolean auto = s.isAutodetectedExecutionMode();
            manualConfigBox.setVisible(!auto);
            manualConfigBox.setManaged(!auto);
        } finally {
            updating = false;
        }
    }

    private void addNewServer(String defaultName, String type, boolean auto) {
        JupyterServerConfig cfg = new JupyterServerConfig(
                UUID.randomUUID().toString(),
                defaultName,
                type,
                auto,
                "",
                ""
        );
        serversItems.add(cfg);
        serversList.getSelectionModel().select(cfg);
        notifyModified();
    }

    private Button createToolbarButton(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-border-color: transparent; " +
                "-fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 2 6;");
        return b;
    }

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();
            applySettingsToUI(initialSettings);
        } finally {
            updating = false;
        }
    }

    private void applySettingsToUI(JupyterServersSettings s) {
        if (s == null) return;
        serversItems.clear();
        for (JupyterServerConfig cfg : s.getServers()) {
            serversItems.add(cfg.copy());
        }

        if (!serversItems.isEmpty()) {
            JupyterServerConfig target = null;
            for (JupyterServerConfig c : serversItems) {
                if (Objects.equals(c.getId(), s.getSelectedServerId())) {
                    target = c;
                    break;
                }
            }
            if (target == null) target = serversItems.get(0);
            serversList.getSelectionModel().select(target);
            populateRightPane(target);
        }
    }

    private JupyterServersSettings getCurrentSettingsFromUI() {
        JupyterServersSettings s = new JupyterServersSettings();
        List<JupyterServerConfig> list = new ArrayList<>();
        for (JupyterServerConfig c : serversItems) {
            list.add(c.copy());
        }
        s.setServers(list);

        JupyterServerConfig sel = serversList.getSelectionModel().getSelectedItem();
        if (sel != null) {
            s.setSelectedServerId(sel.getId());
        }
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        JupyterServersSettings current = getCurrentSettingsFromUI();
        manager.setSettings(current);
        initialSettings = current.clone();
        notifyModified();
    }

    public void reset() {
        loadData();
        notifyModified();
    }

    public void revertChanges() {
        reset();
    }

    public void resetDefaults() {
        applySettingsToUI(new JupyterServersSettings());
        notifyModified();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    // Accessors for testing
    public ListView<JupyterServerConfig> getServersList() {
        return serversList;
    }

    public ObservableList<JupyterServerConfig> getServersItems() {
        return serversItems;
    }

    public TextField getNameField() {
        return nameField;
    }

    public CheckBox getAutodetectModeCheck() {
        return autodetectModeCheck;
    }

    public Button getAddBtn() {
        return addBtn;
    }

    public Button getRemoveBtn() {
        return removeBtn;
    }
}
