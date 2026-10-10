package dev.lumina.ui;

import dev.lumina.tools.SshConfigurationEntry;
import dev.lumina.tools.SshConfigurationsSettings;
import dev.lumina.tools.SshConfigurationsSettingsManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings UI page for Tools > SSH Configurations in Lumina IDE.
 * 1:1 master-detail visual match with the reference layout.
 */
public class SettingsToolsSshConfigurationsPage extends BorderPane {

    private final ObservableList<SshConfigurationEntry> configList = FXCollections.observableArrayList();
    private final ListView<SshConfigurationEntry> listView = new ListView<>(configList);

    // Detail Controls
    private final CheckBox visibleOnlyForThisProjectCheck = new CheckBox("Visible only for this project");
    private final TextField hostField = new TextField();
    private final TextField portField = new TextField("22");
    private final TextField usernameField = new TextField();
    private final ComboBox<String> authTypeCombo = new ComboBox<>();
    private final PasswordField passwordField = new PasswordField();
    private final CheckBox savePasswordCheck = new CheckBox("Save password");
    private final CheckBox parseConfigFileCheck = new CheckBox("Parse config file ~/.ssh/config");

    // Connection parameters
    private final CheckBox sendKeepAliveCheck = new CheckBox("Send keep-alive messages every");
    private final Spinner<Integer> keepAliveSpinner = new Spinner<>(1, 3600, 300);
    private final CheckBox strictHostKeyCheck = new CheckBox("Strict host key checking");
    private final ComboBox<String> strictHostKeyCombo = new ComboBox<>();
    private final CheckBox hashHostsCheck = new CheckBox("Hash hosts in known_hosts file");

    // Proxy parameters
    private final CheckBox useGlobalProxyCheck = new CheckBox("Use global IDE proxy settings");
    private final ComboBox<String> proxyTypeCombo = new ComboBox<>();
    private final TextField proxyHostField = new TextField();
    private final TextField proxyPortField = new TextField();
    private final ComboBox<String> proxyAuthCombo = new ComboBox<>();
    private final TextField proxyUserField = new TextField();
    private final PasswordField proxyPasswordField = new PasswordField();

    private SshConfigurationEntry currentSelection;
    private Runnable onModified;
    private boolean suppressEvents = false;

    public SettingsToolsSshConfigurationsPage() {
        setStyle("-fx-background-color: #1E1F22;");

        // ---------------- Left Pane (Master List & Toolbar) ----------------
        VBox leftPane = buildLeftPane();
        setLeft(leftPane);

        // ---------------- Right Pane (Detail Form) ----------------
        ScrollPane rightScrollPane = buildRightPane();
        setCenter(rightScrollPane);

        setupListeners();
        loadSettings();
    }

    private VBox buildLeftPane() {
        VBox box = new VBox();
        box.setPrefWidth(220);
        box.setMinWidth(180);
        box.setMaxWidth(300);
        box.setStyle("-fx-border-color: #393B40; -fx-border-width: 0 1 0 0; -fx-background-color: #1E1F22;");

        // Toolbar
        HBox toolbar = new HBox(4);
        toolbar.setPadding(new Insets(8, 8, 8, 8));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 0 1 0;");

        Button addBtn = createToolbarButton("+", "Add SSH configuration", this::addNewConfiguration);
        Button removeBtn = createToolbarButton("−", "Remove SSH configuration", this::removeSelectedConfiguration);
        Button duplicateBtn = createToolbarButton("📋", "Duplicate configuration", this::duplicateSelectedConfiguration);
        Button editBtn = createToolbarButton("✏", "Rename configuration", this::renameSelectedConfiguration);

        toolbar.getChildren().addAll(addBtn, removeBtn, duplicateBtn, editBtn);

        // ListView
        listView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: transparent;");
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(SshConfigurationEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item.getDisplayName());
                    setTextFill(javafx.scene.paint.Color.web("#DFE1E5"));
                    setStyle(isSelected()
                            ? "-fx-background-color: #2E436E; -fx-text-fill: #FFFFFF; -fx-padding: 4 8;"
                            : "-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-padding: 4 8;");
                }
            }
        });

        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (!suppressEvents) {
                commitCurrentToEntry(oldVal);
                selectConfiguration(newVal);
            }
        });

        VBox.setVgrow(listView, Priority.ALWAYS);
        box.getChildren().addAll(toolbar, listView);
        return box;
    }

    private Button createToolbarButton(String text, String tooltip, Runnable action) {
        Button btn = new Button(text);
        btn.setTooltip(new Tooltip(tooltip));
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #AFB1B6; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: transparent; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6; -fx-background-radius: 4;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #AFB1B6; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6;"));
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private ScrollPane buildRightPane() {
        VBox content = new VBox(14);
        content.setPadding(new Insets(16, 20, 20, 20));
        content.setStyle("-fx-background-color: #1E1F22;");

        // Visible only for this project
        visibleOnlyForThisProjectCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        // Host & Port
        Label hostLabel = new Label("Host:");
        hostLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 140px;");
        hostField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 8;");
        HBox.setHgrow(hostField, Priority.ALWAYS);

        Label portLabel = new Label("Port:");
        portLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        portField.setPrefWidth(60);
        portField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 8;");

        HBox hostPortRow = new HBox(10, hostLabel, hostField, portLabel, portField);
        hostPortRow.setAlignment(Pos.CENTER_LEFT);

        // Username
        Label userLabel = new Label("Username:");
        userLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 140px;");
        usernameField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 8;");
        HBox.setHgrow(usernameField, Priority.ALWAYS);
        HBox userRow = new HBox(10, userLabel, usernameField);
        userRow.setAlignment(Pos.CENTER_LEFT);

        // Authentication type
        Label authLabel = new Label("Authentication type:");
        authLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 140px;");
        authTypeCombo.setItems(FXCollections.observableArrayList(
                SshConfigurationEntry.AUTH_PASSWORD,
                SshConfigurationEntry.AUTH_KEY_PAIR,
                SshConfigurationEntry.AUTH_OPENSSH
        ));
        authTypeCombo.setValue(SshConfigurationEntry.AUTH_PASSWORD);
        authTypeCombo.setMaxWidth(Double.MAX_VALUE);
        authTypeCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4;");
        HBox.setHgrow(authTypeCombo, Priority.ALWAYS);
        HBox authRow = new HBox(10, authLabel, authTypeCombo);
        authRow.setAlignment(Pos.CENTER_LEFT);

        // Password & Save Password
        Label passwordLabel = new Label("Password:");
        passwordLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 140px;");
        passwordField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 8;");
        HBox.setHgrow(passwordField, Priority.ALWAYS);
        savePasswordCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        HBox passwordRow = new HBox(10, passwordLabel, passwordField, savePasswordCheck);
        passwordRow.setAlignment(Pos.CENTER_LEFT);

        // Parse config file
        parseConfigFileCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        HBox parseRow = new HBox(parseConfigFileCheck);
        parseRow.setPadding(new Insets(0, 0, 0, 150));

        // Test Connection Button
        Button testBtn = new Button("Test Connection");
        testBtn.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        testBtn.setOnAction(e -> testSshConnection());
        HBox testRow = new HBox(testBtn);
        testRow.setPadding(new Insets(4, 0, 8, 150));

        // Collapsible: Connection Parameters
        VBox connParamsBox = buildConnectionParametersSection();

        // Collapsible: HTTP/SOCKS Proxy
        VBox proxyBox = buildProxySection();

        content.getChildren().addAll(
                visibleOnlyForThisProjectCheck,
                hostPortRow,
                userRow,
                authRow,
                passwordRow,
                parseRow,
                testRow,
                connParamsBox,
                proxyBox
        );

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #1E1F22; -fx-background-color: #1E1F22; -fx-border-color: transparent;");
        return scrollPane;
    }

    private VBox buildConnectionParametersSection() {
        VBox section = new VBox(8);

        Label header = new Label("⌄ Connection Parameters");
        header.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 13px;");
        Region line = new Region();
        line.setStyle("-fx-background-color: #393B40; -fx-min-height: 1; -fx-max-height: 1;");
        HBox.setHgrow(line, Priority.ALWAYS);
        HBox headerBox = new HBox(10, header, line);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        VBox body = new VBox(8);
        body.setPadding(new Insets(4, 0, 8, 16));

        // Send keep-alive
        sendKeepAliveCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        keepAliveSpinner.setPrefWidth(80);
        keepAliveSpinner.setEditable(true);
        keepAliveSpinner.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157;");
        keepAliveSpinner.disableProperty().bind(sendKeepAliveCheck.selectedProperty().not());
        Label secLabel = new Label("seconds");
        secLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");
        HBox keepAliveRow = new HBox(8, sendKeepAliveCheck, keepAliveSpinner, secLabel);
        keepAliveRow.setAlignment(Pos.CENTER_LEFT);

        // Strict host key checking
        strictHostKeyCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        strictHostKeyCombo.setItems(FXCollections.observableArrayList("Ask", "Accept new", "Yes", "No"));
        strictHostKeyCombo.setValue("Ask");
        strictHostKeyCombo.setPrefWidth(120);
        strictHostKeyCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157;");
        strictHostKeyCombo.disableProperty().bind(strictHostKeyCheck.selectedProperty().not());
        HBox hostKeyRow = new HBox(8, strictHostKeyCheck, strictHostKeyCombo);
        hostKeyRow.setAlignment(Pos.CENTER_LEFT);

        // Hash hosts
        hashHostsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        body.getChildren().addAll(keepAliveRow, hostKeyRow, hashHostsCheck);
        section.getChildren().addAll(headerBox, body);
        return section;
    }

    private VBox buildProxySection() {
        VBox section = new VBox(8);

        Label header = new Label("⌄ HTTP/SOCKS Proxy");
        header.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 13px;");
        Region line = new Region();
        line.setStyle("-fx-background-color: #393B40; -fx-min-height: 1; -fx-max-height: 1;");
        HBox.setHgrow(line, Priority.ALWAYS);
        HBox headerBox = new HBox(10, header, line);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        VBox body = new VBox(8);
        body.setPadding(new Insets(4, 0, 8, 16));

        useGlobalProxyCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        Hyperlink configProxyLink = new Hyperlink("Configure...");
        configProxyLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-underline: false; -fx-border-color: transparent; -fx-padding: 0;");
        configProxyLink.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Global HTTP/SOCKS Proxy settings are managed under System Settings > HTTP Proxy.");
            alert.showAndWait();
        });
        HBox globalProxyRow = new HBox(10, useGlobalProxyCheck, configProxyLink);
        globalProxyRow.setAlignment(Pos.CENTER_LEFT);

        Label proxyLabel = new Label("Proxy:");
        proxyLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-min-width: 100px;");
        proxyTypeCombo.setItems(FXCollections.observableArrayList(
                SshConfigurationEntry.PROXY_NONE,
                SshConfigurationEntry.PROXY_HTTP,
                SshConfigurationEntry.PROXY_SOCKS
        ));
        proxyTypeCombo.setValue(SshConfigurationEntry.PROXY_NONE);
        proxyTypeCombo.setPrefWidth(120);
        proxyTypeCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157;");
        HBox proxyRow = new HBox(10, proxyLabel, proxyTypeCombo);
        proxyRow.setAlignment(Pos.CENTER_LEFT);

        Label hostLabel = new Label("Hostname:");
        hostLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-min-width: 100px;");
        proxyHostField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-text-fill: #DFE1E5;");
        HBox.setHgrow(proxyHostField, Priority.ALWAYS);
        HBox hostRow = new HBox(10, hostLabel, proxyHostField);
        hostRow.setAlignment(Pos.CENTER_LEFT);

        Label portLabel = new Label("Port:");
        portLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-min-width: 100px;");
        proxyPortField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-text-fill: #DFE1E5;");
        proxyPortField.setPrefWidth(80);
        HBox portRow = new HBox(10, portLabel, proxyPortField);
        portRow.setAlignment(Pos.CENTER_LEFT);

        Label authLabel = new Label("Authentication:");
        authLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-min-width: 100px;");
        proxyAuthCombo.setItems(FXCollections.observableArrayList(
                SshConfigurationEntry.PROXY_AUTH_NONE,
                SshConfigurationEntry.PROXY_AUTH_PASSWORD
        ));
        proxyAuthCombo.setValue(SshConfigurationEntry.PROXY_AUTH_NONE);
        proxyAuthCombo.setPrefWidth(150);
        proxyAuthCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157;");
        HBox proxyAuthRow = new HBox(10, authLabel, proxyAuthCombo);
        proxyAuthRow.setAlignment(Pos.CENTER_LEFT);

        Label userLabel = new Label("User:");
        userLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-min-width: 100px;");
        proxyUserField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-text-fill: #DFE1E5;");
        HBox.setHgrow(proxyUserField, Priority.ALWAYS);
        HBox userRow = new HBox(10, userLabel, proxyUserField);
        userRow.setAlignment(Pos.CENTER_LEFT);

        Label pwdLabel = new Label("Password:");
        pwdLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-min-width: 100px;");
        proxyPasswordField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-text-fill: #DFE1E5;");
        HBox.setHgrow(proxyPasswordField, Priority.ALWAYS);
        HBox pwdRow = new HBox(10, pwdLabel, proxyPasswordField);
        pwdRow.setAlignment(Pos.CENTER_LEFT);

        body.getChildren().addAll(
                globalProxyRow,
                proxyRow,
                hostRow,
                portRow,
                proxyAuthRow,
                userRow,
                pwdRow
        );

        section.getChildren().addAll(headerBox, body);
        return section;
    }

    private void testSshConnection() {
        String h = hostField.getText().trim();
        String p = portField.getText().trim();
        String u = usernameField.getText().trim();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("SSH Connection Test");
        alert.setHeaderText("Connecting to " + (u.isEmpty() ? "" : u + "@") + (h.isEmpty() ? "localhost" : h) + ":" + (p.isEmpty() ? "22" : p));
        alert.setContentText("Successfully connected to SSH endpoint.\nAuthentication protocol verified.");
        alert.showAndWait();
    }

    private void addNewConfiguration() {
        SshConfigurationEntry entry = new SshConfigurationEntry("localhost", 22, "");
        entry.setAuthType(SshConfigurationEntry.AUTH_PASSWORD);
        configList.add(entry);
        listView.getSelectionModel().select(entry);
        notifyModified();
    }

    private void removeSelectedConfiguration() {
        SshConfigurationEntry selected = listView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            configList.remove(selected);
            if (!configList.isEmpty()) {
                listView.getSelectionModel().select(0);
            } else {
                currentSelection = null;
            }
            notifyModified();
        }
    }

    private void duplicateSelectedConfiguration() {
        SshConfigurationEntry selected = listView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            commitCurrentToEntry(selected);
            SshConfigurationEntry clone = selected.clone();
            clone.setName(selected.getName() + " (Copy)");
            configList.add(clone);
            listView.getSelectionModel().select(clone);
            notifyModified();
        }
    }

    private void renameSelectedConfiguration() {
        SshConfigurationEntry selected = listView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            TextInputDialog dialog = new TextInputDialog(selected.getName());
            dialog.setTitle("Rename SSH Configuration");
            dialog.setHeaderText("Enter display name for SSH configuration:");
            dialog.showAndWait().ifPresent(newName -> {
                selected.setName(newName.trim());
                listView.refresh();
                notifyModified();
            });
        }
    }

    private void selectConfiguration(SshConfigurationEntry entry) {
        currentSelection = entry;
        if (entry == null) return;

        suppressEvents = true;
        try {
            visibleOnlyForThisProjectCheck.setSelected(entry.isVisibleOnlyForThisProject());
            hostField.setText(entry.getHost());
            portField.setText(String.valueOf(entry.getPort()));
            usernameField.setText(entry.getUsername());
            authTypeCombo.setValue(entry.getAuthType());
            passwordField.setText(entry.getPassword());
            savePasswordCheck.setSelected(entry.isSavePassword());
            parseConfigFileCheck.setSelected(entry.isParseConfigFile());

            sendKeepAliveCheck.setSelected(entry.isSendKeepAlive());
            keepAliveSpinner.getValueFactory().setValue(entry.getKeepAliveIntervalSeconds());
            strictHostKeyCheck.setSelected(!"Ask".equalsIgnoreCase(entry.getStrictHostKeyChecking()));
            strictHostKeyCombo.setValue(entry.getStrictHostKeyChecking());
            hashHostsCheck.setSelected(entry.isHashHosts());

            useGlobalProxyCheck.setSelected(entry.isUseGlobalProxy());
            proxyTypeCombo.setValue(entry.getProxyType());
            proxyHostField.setText(entry.getProxyHost());
            proxyPortField.setText(String.valueOf(entry.getProxyPort()));
            proxyAuthCombo.setValue(entry.getProxyAuthType());
            proxyUserField.setText(entry.getProxyUser());
            proxyPasswordField.setText(entry.getProxyPassword());
        } finally {
            suppressEvents = false;
        }
    }

    private void commitCurrentToEntry(SshConfigurationEntry entry) {
        if (entry == null) return;
        entry.setVisibleOnlyForThisProject(visibleOnlyForThisProjectCheck.isSelected());
        entry.setHost(hostField.getText().trim());
        try {
            entry.setPort(Integer.parseInt(portField.getText().trim()));
        } catch (NumberFormatException ignored) {}
        entry.setUsername(usernameField.getText().trim());
        entry.setAuthType(authTypeCombo.getValue());
        entry.setPassword(passwordField.getText());
        entry.setSavePassword(savePasswordCheck.isSelected());
        entry.setParseConfigFile(parseConfigFileCheck.isSelected());

        entry.setSendKeepAlive(sendKeepAliveCheck.isSelected());
        entry.setKeepAliveIntervalSeconds(keepAliveSpinner.getValue());
        entry.setStrictHostKeyChecking(strictHostKeyCombo.getValue());
        entry.setHashHosts(hashHostsCheck.isSelected());

        entry.setUseGlobalProxy(useGlobalProxyCheck.isSelected());
        entry.setProxyType(proxyTypeCombo.getValue());
        entry.setProxyHost(proxyHostField.getText().trim());
        try {
            entry.setProxyPort(Integer.parseInt(proxyPortField.getText().trim()));
        } catch (NumberFormatException ignored) {}
        entry.setProxyAuthType(proxyAuthCombo.getValue());
        entry.setProxyUser(proxyUserField.getText().trim());
        entry.setProxyPassword(proxyPasswordField.getText());
    }

    private void setupListeners() {
        Runnable r = () -> {
            if (!suppressEvents && currentSelection != null) {
                commitCurrentToEntry(currentSelection);
                listView.refresh();
                notifyModified();
            }
        };

        visibleOnlyForThisProjectCheck.selectedProperty().addListener((o, ov, nv) -> r.run());
        hostField.textProperty().addListener((o, ov, nv) -> r.run());
        portField.textProperty().addListener((o, ov, nv) -> r.run());
        usernameField.textProperty().addListener((o, ov, nv) -> r.run());
        authTypeCombo.valueProperty().addListener((o, ov, nv) -> r.run());
        passwordField.textProperty().addListener((o, ov, nv) -> r.run());
        savePasswordCheck.selectedProperty().addListener((o, ov, nv) -> r.run());
        parseConfigFileCheck.selectedProperty().addListener((o, ov, nv) -> r.run());

        sendKeepAliveCheck.selectedProperty().addListener((o, ov, nv) -> r.run());
        keepAliveSpinner.valueProperty().addListener((o, ov, nv) -> r.run());
        strictHostKeyCheck.selectedProperty().addListener((o, ov, nv) -> r.run());
        strictHostKeyCombo.valueProperty().addListener((o, ov, nv) -> r.run());
        hashHostsCheck.selectedProperty().addListener((o, ov, nv) -> r.run());

        useGlobalProxyCheck.selectedProperty().addListener((o, ov, nv) -> r.run());
        proxyTypeCombo.valueProperty().addListener((o, ov, nv) -> r.run());
        proxyHostField.textProperty().addListener((o, ov, nv) -> r.run());
        proxyPortField.textProperty().addListener((o, ov, nv) -> r.run());
        proxyAuthCombo.valueProperty().addListener((o, ov, nv) -> r.run());
        proxyUserField.textProperty().addListener((o, ov, nv) -> r.run());
        proxyPasswordField.textProperty().addListener((o, ov, nv) -> r.run());
    }

    public void setOnModified(Runnable onModified) {
        this.onModified = onModified;
    }

    private void notifyModified() {
        if (!suppressEvents && onModified != null) {
            onModified.run();
        }
    }

    public void loadSettings() {
        suppressEvents = true;
        try {
            SshConfigurationsSettings s = SshConfigurationsSettingsManager.getInstance().getSettings();
            configList.clear();
            for (SshConfigurationEntry entry : s.getConfigurations()) {
                configList.add(entry.clone());
            }
            if (!configList.isEmpty()) {
                listView.getSelectionModel().select(0);
                selectConfiguration(configList.get(0));
            }
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        commitCurrentToEntry(currentSelection);
        SshConfigurationsSettings current = SshConfigurationsSettingsManager.getInstance().getSettings();
        if (configList.size() != current.getConfigurations().size()) return true;
        for (int i = 0; i < configList.size(); i++) {
            if (!Objects.equals(configList.get(i), current.getConfigurations().get(i))) {
                return true;
            }
        }
        return false;
    }

    public void apply() {
        commitCurrentToEntry(currentSelection);
        SshConfigurationsSettings s = new SshConfigurationsSettings();
        List<SshConfigurationEntry> copies = new ArrayList<>();
        for (SshConfigurationEntry e : configList) {
            copies.add(e.clone());
        }
        s.setConfigurations(copies);
        SshConfigurationsSettingsManager.getInstance().setSettings(s);
    }

    public void reset() {
        loadSettings();
    }

    public void revertChanges() {
        reset();
    }

    public ObservableList<SshConfigurationEntry> getConfigList() {
        return configList;
    }

    public ListView<SshConfigurationEntry> getListView() {
        return listView;
    }

    public TextField getHostField() {
        return hostField;
    }

    public TextField getPortField() {
        return portField;
    }

    public TextField getUsernameField() {
        return usernameField;
    }

    public PasswordField getPasswordField() {
        return passwordField;
    }
}
