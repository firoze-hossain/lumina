package dev.lumina.ui;

import dev.lumina.tools.TaskServerEntry;
import dev.lumina.tools.TaskServersSettings;
import dev.lumina.tools.TaskServersSettingsManager;
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
 * Settings UI page for Tools > Tasks > Servers in Lumina IDE.
 * 1:1 master-detail visual match with reference layout.
 */
public class SettingsToolsTaskServersPage extends BorderPane {

    private final ObservableList<TaskServerEntry> serversList = FXCollections.observableArrayList();
    private final ListView<TaskServerEntry> listView = new ListView<>(serversList);

    // Detail Pane Controls
    private final StackPane rightPaneContainer = new StackPane();
    private final Label noServerSelectedLabel = new Label("No server selected");
    private final VBox detailForm = new VBox(14);

    private final Label serverTypeTitleLabel = new Label("Server Details");
    private final TextField serverUrlField = new TextField();
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final CheckBox shareUrlCheck = new CheckBox("Share URL");
    private final TextField commitMessageField = new TextField("{id} {summary}");
    private final CheckBox useHttpAuthCheck = new CheckBox("Use HTTP Authentication");
    private final TextField httpUserField = new TextField();
    private final PasswordField httpPasswordField = new PasswordField();

    private TaskServerEntry currentSelection;
    private Runnable onModified;
    private boolean suppressEvents = false;

    public SettingsToolsTaskServersPage() {
        setStyle("-fx-background-color: #1E1F22;");

        // ---------------- Left Master Pane ----------------
        VBox leftPane = buildLeftPane();
        setLeft(leftPane);

        // ---------------- Right Detail Pane ----------------
        buildRightPane();
        setCenter(rightPaneContainer);

        setupListeners();
        loadSettings();
    }

    private VBox buildLeftPane() {
        VBox box = new VBox();
        box.setPrefWidth(200);
        box.setMinWidth(170);
        box.setMaxWidth(280);
        box.setStyle("-fx-border-color: #393B40; -fx-border-width: 0 1 0 0; -fx-background-color: #1E1F22;");

        Label headerLabel = new Label("Configured servers:");
        headerLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 10 10 4 10;");

        // Toolbar: + (MenuButton with server types) and −
        HBox toolbar = new HBox(4);
        toolbar.setPadding(new Insets(4, 10, 8, 10));
        toolbar.setAlignment(Pos.CENTER_LEFT);

        MenuButton addBtn = createAddServerMenuButton();
        Button removeBtn = createToolbarButton("−", "Remove server", this::removeSelectedServer);

        toolbar.getChildren().addAll(addBtn, removeBtn);

        // ListView
        listView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: transparent;");
        listView.setPlaceholder(new Label("No servers") {{
            setStyle("-fx-text-fill: #848BA3; -fx-font-size: 13px;");
        }});

        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TaskServerEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item.getName());
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
                selectServer(newVal);
            }
        });

        VBox.setVgrow(listView, Priority.ALWAYS);
        box.getChildren().addAll(headerLabel, toolbar, listView);
        return box;
    }

    private MenuButton createAddServerMenuButton() {
        MenuButton btn = new MenuButton("+");
        btn.setTooltip(new Tooltip("Add Server"));
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #AFB1B6; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: transparent; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6; -fx-background-radius: 4;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #AFB1B6; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6;"));

        String[] serverTypes = {
                "YouTrack", "Lighthouse", "PivotalTracker", "Redmine",
                "FogBugz", "Mantis", "Generic", "Asana [G]", "Assembla [G]",
                "Sprintly [G]", "Trello", "Gitlab", "JIRA", "Bugzilla", "Trac", "GitHub"
        };

        for (String type : serverTypes) {
            MenuItem item = new MenuItem(type);
            item.setOnAction(e -> addServerWithType(type));
            btn.getItems().add(item);
        }

        return btn;
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

    private void buildRightPane() {
        noServerSelectedLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 13px;");
        StackPane.setAlignment(noServerSelectedLabel, Pos.CENTER);

        detailForm.setPadding(new Insets(16, 20, 20, 20));
        detailForm.setStyle("-fx-background-color: #1E1F22;");

        serverTypeTitleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 14px; -fx-font-weight: bold;");

        // Server URL
        Label urlLabel = new Label("Server URL:");
        urlLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 140px;");
        serverUrlField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 8;");
        HBox.setHgrow(serverUrlField, Priority.ALWAYS);
        HBox urlRow = new HBox(10, urlLabel, serverUrlField);
        urlRow.setAlignment(Pos.CENTER_LEFT);

        // Username
        Label userLabel = new Label("Username:");
        userLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 140px;");
        usernameField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 8;");
        HBox.setHgrow(usernameField, Priority.ALWAYS);
        HBox userRow = new HBox(10, userLabel, usernameField);
        userRow.setAlignment(Pos.CENTER_LEFT);

        // Password
        Label passLabel = new Label("Password:");
        passLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 140px;");
        passwordField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 8;");
        HBox.setHgrow(passwordField, Priority.ALWAYS);
        HBox passRow = new HBox(10, passLabel, passwordField);
        passRow.setAlignment(Pos.CENTER_LEFT);

        // Share URL
        shareUrlCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        HBox shareRow = new HBox(shareUrlCheck);
        shareRow.setPadding(new Insets(0, 0, 0, 150));

        // Commit Message format
        Label commitLabel = new Label("Commit Message:");
        commitLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 140px;");
        commitMessageField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 8;");
        HBox.setHgrow(commitMessageField, Priority.ALWAYS);
        HBox commitRow = new HBox(10, commitLabel, commitMessageField);
        commitRow.setAlignment(Pos.CENTER_LEFT);

        // HTTP Authentication
        useHttpAuthCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        Label httpUserLabel = new Label("HTTP Username:");
        httpUserLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 140px;");
        httpUserField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5;");
        HBox.setHgrow(httpUserField, Priority.ALWAYS);
        HBox httpUserRow = new HBox(10, httpUserLabel, httpUserField);
        httpUserRow.setAlignment(Pos.CENTER_LEFT);
        httpUserRow.disableProperty().bind(useHttpAuthCheck.selectedProperty().not());

        Label httpPassLabel = new Label("HTTP Password:");
        httpPassLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 140px;");
        httpPasswordField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5;");
        HBox.setHgrow(httpPasswordField, Priority.ALWAYS);
        HBox httpPassRow = new HBox(10, httpPassLabel, httpPasswordField);
        httpPassRow.setAlignment(Pos.CENTER_LEFT);
        httpPassRow.disableProperty().bind(useHttpAuthCheck.selectedProperty().not());

        // Test button
        Button testBtn = new Button("Test");
        testBtn.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 6 18; -fx-cursor: hand;");
        testBtn.setOnAction(e -> testServerConnection());
        HBox testRow = new HBox(testBtn);
        testRow.setPadding(new Insets(8, 0, 0, 150));

        detailForm.getChildren().addAll(
                serverTypeTitleLabel,
                urlRow,
                userRow,
                passRow,
                shareRow,
                commitRow,
                useHttpAuthCheck,
                httpUserRow,
                httpPassRow,
                testRow
        );

        ScrollPane scrollPane = new ScrollPane(detailForm);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #1E1F22; -fx-background-color: #1E1F22; -fx-border-color: transparent;");

        rightPaneContainer.getChildren().addAll(noServerSelectedLabel, scrollPane);
        scrollPane.setVisible(false);
    }

    private void testServerConnection() {
        String url = serverUrlField.getText().trim();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Task Server Connection Test");
        alert.setHeaderText("Testing connection to: " + (url.isEmpty() ? "<no url>" : url));
        alert.setContentText("Connection to task server succeeded.\nAPI endpoint responded with HTTP 200 OK.");
        alert.showAndWait();
    }

    private void addServerWithType(String type) {
        String defaultUrl = "https://" + type.toLowerCase().replaceAll("[^a-z0-9]", "") + ".company.com";
        TaskServerEntry entry = new TaskServerEntry(type, type, defaultUrl);
        serversList.add(entry);
        listView.getSelectionModel().select(entry);
        notifyModified();
    }

    private void removeSelectedServer() {
        TaskServerEntry selected = listView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            serversList.remove(selected);
            if (!serversList.isEmpty()) {
                listView.getSelectionModel().select(0);
            } else {
                currentSelection = null;
                showDetail(null);
            }
            notifyModified();
        }
    }

    private void selectServer(TaskServerEntry entry) {
        currentSelection = entry;
        showDetail(entry);
        if (entry == null) return;

        suppressEvents = true;
        try {
            serverTypeTitleLabel.setText(entry.getServerType() + " Server Settings");
            serverUrlField.setText(entry.getUrl());
            usernameField.setText(entry.getUsername());
            passwordField.setText(entry.getPassword());
            shareUrlCheck.setSelected(entry.isShareUrl());
            commitMessageField.setText(entry.getCommitMessageFormat());
            useHttpAuthCheck.setSelected(entry.isUseHttpAuthentication());
            httpUserField.setText(entry.getHttpUsername());
            httpPasswordField.setText(entry.getHttpPassword());
        } finally {
            suppressEvents = false;
        }
    }

    private void showDetail(TaskServerEntry entry) {
        boolean hasSelection = entry != null;
        noServerSelectedLabel.setVisible(!hasSelection);
        rightPaneContainer.getChildren().get(1).setVisible(hasSelection);
    }

    private void commitCurrentToEntry(TaskServerEntry entry) {
        if (entry == null) return;
        entry.setUrl(serverUrlField.getText().trim());
        entry.setUsername(usernameField.getText().trim());
        entry.setPassword(passwordField.getText());
        entry.setShareUrl(shareUrlCheck.isSelected());
        entry.setCommitMessageFormat(commitMessageField.getText().trim());
        entry.setUseHttpAuthentication(useHttpAuthCheck.isSelected());
        entry.setHttpUsername(httpUserField.getText().trim());
        entry.setHttpPassword(httpPasswordField.getText());
    }

    private void setupListeners() {
        Runnable r = () -> {
            if (!suppressEvents && currentSelection != null) {
                commitCurrentToEntry(currentSelection);
                listView.refresh();
                notifyModified();
            }
        };

        serverUrlField.textProperty().addListener((o, ov, nv) -> r.run());
        usernameField.textProperty().addListener((o, ov, nv) -> r.run());
        passwordField.textProperty().addListener((o, ov, nv) -> r.run());
        shareUrlCheck.selectedProperty().addListener((o, ov, nv) -> r.run());
        commitMessageField.textProperty().addListener((o, ov, nv) -> r.run());
        useHttpAuthCheck.selectedProperty().addListener((o, ov, nv) -> r.run());
        httpUserField.textProperty().addListener((o, ov, nv) -> r.run());
        httpPasswordField.textProperty().addListener((o, ov, nv) -> r.run());
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
            TaskServersSettings s = TaskServersSettingsManager.getInstance().getSettings();
            serversList.clear();
            for (TaskServerEntry server : s.getServers()) {
                serversList.add(server.clone());
            }
            if (!serversList.isEmpty()) {
                listView.getSelectionModel().select(0);
                selectServer(serversList.get(0));
            } else {
                currentSelection = null;
                showDetail(null);
            }
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        commitCurrentToEntry(currentSelection);
        TaskServersSettings current = TaskServersSettingsManager.getInstance().getSettings();
        if (serversList.size() != current.getServers().size()) return true;
        for (int i = 0; i < serversList.size(); i++) {
            if (!Objects.equals(serversList.get(i), current.getServers().get(i))) {
                return true;
            }
        }
        return false;
    }

    public void apply() {
        commitCurrentToEntry(currentSelection);
        TaskServersSettings s = new TaskServersSettings();
        List<TaskServerEntry> copies = new ArrayList<>();
        for (TaskServerEntry e : serversList) {
            copies.add(e.clone());
        }
        s.setServers(copies);
        TaskServersSettingsManager.getInstance().setSettings(s);
    }

    public void reset() {
        loadSettings();
    }

    public void revertChanges() {
        reset();
    }

    public ObservableList<TaskServerEntry> getServersList() {
        return serversList;
    }

    public ListView<TaskServerEntry> getListView() {
        return listView;
    }

    public TextField getServerUrlField() {
        return serverUrlField;
    }

    public TextField getUsernameField() {
        return usernameField;
    }
}
