package dev.lumina.ui;

import dev.lumina.git.GitService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * "Git Remotes" dialog matching IntelliJ IDEA's exact visual design (media_1790164497718.png).
 * Displays dynamically queried remotes, with toolbar actions to Add, Remove, and Edit remotes.
 */
public class GitRemotesDialog extends Stage {

    private final Path projectRoot;
    private final Consumer<String> onRemotesChanged;
    private final ObservableList<GitService.RemoteEntry> remotesList = FXCollections.observableArrayList();
    private final TableView<GitService.RemoteEntry> tableView = new TableView<>(remotesList);

    private final Button addButton = new Button();
    private final Button removeButton = new Button();
    private final Button editButton = new Button();

    public GitRemotesDialog(Stage owner, Path projectRoot, Consumer<String> onRemotesChanged) {
        this.projectRoot = projectRoot;
        this.onRemotesChanged = onRemotesChanged;

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);
        setTitle("Git Remotes");
        setMinWidth(520);
        setMinHeight(300);
        setWidth(580);
        setHeight(340);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1E1F22;");

        // ---- Top Toolbar ----
        HBox toolbar = new HBox(4);
        toolbar.setPadding(new Insets(8, 12, 6, 12));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent transparent #2B2D30 transparent; -fx-border-width: 0 0 1 0;");

        styleToolbarButton(addButton, GitIcons.plusIcon(13, "#DFE1E5"), "Add");
        styleToolbarButton(removeButton, GitIcons.minusIcon(13, "#DFE1E5"), "Remove");
        styleToolbarButton(editButton, GitIcons.editIcon(13, "#DFE1E5"), "Edit");

        addButton.setOnAction(e -> showDefineRemoteDialog(null));
        removeButton.setOnAction(e -> removeSelectedRemote());
        editButton.setOnAction(e -> editSelectedRemote());

        toolbar.getChildren().addAll(addButton, removeButton, editButton);
        root.setTop(toolbar);

        // ---- Center: TableView ----
        tableView.setStyle(
                "-fx-background-color: #1E1F22; " +
                "-fx-border-color: transparent; " +
                "-fx-table-cell-border-color: transparent;"
        );
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableView.setPlaceholder(new Label("No remotes configured"));

        TableColumn<GitService.RemoteEntry, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));
        nameCol.setMinWidth(110);
        nameCol.setPrefWidth(120);
        nameCol.setMaxWidth(160);

        TableColumn<GitService.RemoteEntry, String> urlCol = new TableColumn<>("URL");
        urlCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDisplayUrl()));
        urlCol.setMinWidth(250);

        tableView.getColumns().addAll(nameCol, urlCol);

        // Selection bindings
        removeButton.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());
        editButton.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());

        // Keyboard actions
        tableView.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                editSelectedRemote();
            } else if (e.getCode() == KeyCode.DELETE) {
                removeSelectedRemote();
            }
        });

        // Double-click to edit
        tableView.setRowFactory(tv -> {
            TableRow<GitService.RemoteEntry> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    editSelectedRemote();
                }
            });
            return row;
        });

        VBox centerBox = new VBox(tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);
        centerBox.setPadding(new Insets(4, 12, 4, 12));
        root.setCenter(centerBox);

        // ---- Bottom Bar ----
        HBox bottomBar = new HBox(8);
        bottomBar.setPadding(new Insets(10, 14, 12, 14));
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #2B2D30 transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        Button okButton = new Button("OK");
        okButton.setDefaultButton(true);
        okButton.setPrefWidth(72);
        okButton.setStyle(
                "-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 5 16 5 16;"
        );
        okButton.setOnAction(e -> close());

        bottomBar.getChildren().add(okButton);
        root.setBottom(bottomBar);

        Scene scene = new Scene(root);
        try {
            var css = getClass().getResource("/css/lumina-dark.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
        } catch (Exception ignored) {}

        setScene(scene);
        loadRemotes();
    }

    private void styleToolbarButton(Button btn, Node icon, String tooltip) {
        btn.setGraphic(icon);
        btn.setTooltip(new Tooltip(tooltip));
        btn.setMinSize(24, 24);
        btn.setPrefSize(24, 24);
        btn.setMaxSize(24, 24);
        btn.setStyle(
                "-fx-background-color: transparent; -fx-border-color: transparent; " +
                "-fx-background-radius: 4; -fx-padding: 2; -fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #2B2D30; -fx-border-color: transparent; " +
                "-fx-background-radius: 4; -fx-padding: 2; -fx-cursor: hand;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: transparent; -fx-border-color: transparent; " +
                "-fx-background-radius: 4; -fx-padding: 2; -fx-cursor: hand;"
        ));
    }

    public void loadRemotes() {
        remotesList.clear();
        if (projectRoot != null) {
            List<GitService.RemoteEntry> entries = GitService.remoteEntries(projectRoot);
            remotesList.addAll(entries);
            if (!remotesList.isEmpty()) {
                tableView.getSelectionModel().select(0);
            }
        }
    }

    private void editSelectedRemote() {
        GitService.RemoteEntry selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showDefineRemoteDialog(selected);
        }
    }

    private void removeSelectedRemote() {
        GitService.RemoteEntry selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null || projectRoot == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(this);
        confirm.setTitle("Remove Remote");
        confirm.setHeaderText("Remove Git remote '" + selected.name() + "'?");
        confirm.setContentText("URL: " + selected.getDisplayUrl());
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                GitService.Result r = GitService.removeRemote(projectRoot, selected.name());
                if (r.ok()) {
                    loadRemotes();
                    if (onRemotesChanged != null) {
                        onRemotesChanged.accept("Removed remote " + selected.name());
                    }
                } else {
                    showError("Failed to remove remote", r.output());
                }
            }
        });
    }

    private void showDefineRemoteDialog(GitService.RemoteEntry existing) {
        Stage dlg = new Stage();
        dlg.initOwner(this);
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.initStyle(StageStyle.DECORATED);
        dlg.setTitle(existing == null ? "Define Remote" : "Edit Remote");
        dlg.setMinWidth(460);
        dlg.setMinHeight(190);
        dlg.setWidth(480);
        dlg.setHeight(200);

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.setStyle("-fx-background-color: #1E1F22;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        Label nameLbl = new Label("Name:");
        nameLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        TextField nameField = new TextField();
        nameField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4;");
        GridPane.setHgrow(nameField, Priority.ALWAYS);

        if (existing != null) {
            nameField.setText(existing.name());
        } else {
            boolean hasOrigin = remotesList.stream().anyMatch(r -> "origin".equalsIgnoreCase(r.name()));
            nameField.setText(hasOrigin ? "upstream" : "origin");
        }

        Label urlLbl = new Label("URL:");
        urlLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        TextField urlField = new TextField();
        urlField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4;");
        GridPane.setHgrow(urlField, Priority.ALWAYS);

        if (existing != null) {
            urlField.setText(existing.getDisplayUrl());
        }

        grid.add(nameLbl, 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(urlLbl, 0, 1);
        grid.add(urlField, 1, 1);

        HBox btnBox = new HBox(8);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(8, 0, 0, 0));

        Button ok = new Button("OK");
        ok.setDefaultButton(true);
        ok.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-padding: 5 16;");

        Button cancel = new Button("Cancel");
        cancel.setCancelButton(true);
        cancel.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-background-radius: 4; -fx-padding: 5 14;");
        cancel.setOnAction(e -> dlg.close());

        ok.setOnAction(e -> {
            String newName = nameField.getText().trim();
            String newUrl = urlField.getText().trim();
            if (newName.isEmpty()) {
                showError("Validation Error", "Remote name cannot be empty.");
                return;
            }
            if (newUrl.isEmpty()) {
                showError("Validation Error", "Remote URL cannot be empty.");
                return;
            }

            if (existing == null) {
                // Add new remote
                GitService.Result r = GitService.addRemote(projectRoot, newName, newUrl);
                if (r.ok()) {
                    dlg.close();
                    loadRemotes();
                    if (onRemotesChanged != null) {
                        onRemotesChanged.accept("Added remote " + newName);
                    }
                } else {
                    showError("Failed to add remote", r.output());
                }
            } else {
                // Edit existing remote
                boolean okOp = true;
                if (!existing.getDisplayUrl().equals(newUrl)) {
                    GitService.Result r = GitService.setRemoteUrl(projectRoot, existing.name(), newUrl);
                    if (!r.ok()) {
                        showError("Failed to update URL", r.output());
                        okOp = false;
                    }
                }
                if (okOp && !existing.name().equals(newName)) {
                    GitService.Result r = GitService.renameRemote(projectRoot, existing.name(), newName);
                    if (!r.ok()) {
                        showError("Failed to rename remote", r.output());
                        okOp = false;
                    }
                }
                if (okOp) {
                    dlg.close();
                    loadRemotes();
                    if (onRemotesChanged != null) {
                        onRemotesChanged.accept("Updated remote " + newName);
                    }
                }
            }
        });

        btnBox.getChildren().addAll(ok, cancel);
        content.getChildren().addAll(grid, btnBox);

        Scene dlgScene = new Scene(content);
        try {
            var css = getClass().getResource("/css/lumina-dark.css");
            if (css != null) dlgScene.getStylesheets().add(css.toExternalForm());
        } catch (Exception ignored) {}

        dlg.setScene(dlgScene);
        dlg.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(this);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public TableView<GitService.RemoteEntry> getTableView() { return tableView; }
    public ObservableList<GitService.RemoteEntry> getRemotesList() { return remotesList; }
    public Button getAddButton() { return addButton; }
    public Button getRemoveButton() { return removeButton; }
    public Button getEditButton() { return editButton; }
}
