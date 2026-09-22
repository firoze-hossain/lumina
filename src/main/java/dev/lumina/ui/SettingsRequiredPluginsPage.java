package dev.lumina.ui;

import dev.lumina.plugin.RequiredPlugin;
import dev.lumina.plugin.RequiredPluginsManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

/**
 * Required Plugins settings page.
 * Strictly matches media_1790045975642.png.
 */
public class SettingsRequiredPluginsPage extends VBox {

    private final RequiredPluginsManager manager = RequiredPluginsManager.getInstance();
    private final TableView<RequiredPlugin> table = new TableView<>();
    private final ObservableList<RequiredPlugin> tableData = FXCollections.observableArrayList();

    private final Button addButton = new Button("+");
    private final Button removeButton = new Button("−");
    private final Button editButton = new Button("✎");

    public SettingsRequiredPluginsPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(14, 24, 16, 24));
        setSpacing(10);
        VBox.setVgrow(this, Priority.ALWAYS);

        // 1. Top Description Header (Strictly from screenshot)
        VBox headerBox = new VBox(3);
        Label line1 = new Label("Specify a list of plugins required for your project.");
        line1.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        Label line2 = new Label("Lumina IDE will notify you if a required plugin is missing or needs an update.");
        line2.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 13px;");

        headerBox.getChildren().addAll(line1, line2);

        // 2. Toolbar (+, -, Edit)
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 0, 4, 0));

        styleToolButton(addButton, "Add required plugin");
        addButton.setOnAction(e -> onAdd());

        styleToolButton(removeButton, "Remove");
        removeButton.setOnAction(e -> onRemove());

        styleToolButton(editButton, "Edit");
        editButton.setOnAction(e -> onEdit());

        toolbar.getChildren().addAll(addButton, removeButton, editButton);

        // 3. TableView
        table.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-background: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<RequiredPlugin, String> pluginCol = new TableColumn<>("Plugin");
        pluginCol.setCellValueFactory(data -> data.getValue().pluginNameProperty());
        pluginCol.setPrefWidth(320);
        pluginCol.setCellFactory(col -> createStyledCell());

        TableColumn<RequiredPlugin, String> minCol = new TableColumn<>("Minimum version");
        minCol.setCellValueFactory(data -> data.getValue().minVersionProperty());
        minCol.setPrefWidth(160);
        minCol.setCellFactory(col -> createStyledCell());

        TableColumn<RequiredPlugin, String> maxCol = new TableColumn<>("Maximum version");
        maxCol.setCellValueFactory(data -> data.getValue().maxVersionProperty());
        maxCol.setPrefWidth(160);
        maxCol.setCellFactory(col -> createStyledCell());

        table.getColumns().addAll(pluginCol, minCol, maxCol);
        table.setPlaceholder(new Label("No required plugins specified."));

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> updateButtonStates());

        table.setRowFactory(tv -> {
            TableRow<RequiredPlugin> row = new TableRow<>() {
                @Override
                protected void updateItem(RequiredPlugin item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("-fx-background-color: #1E1F22;");
                    } else if (isSelected()) {
                        setStyle("-fx-background-color: #2E436E;");
                    } else {
                        setStyle("-fx-background-color: #1E1F22;");
                    }
                }
            };
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    onEdit();
                }
            });
            return row;
        });

        getChildren().addAll(headerBox, toolbar, table);

        manager.addListener(this::reloadTableData);
        reloadTableData();
    }

    private TableCell<RequiredPlugin, String> createStyledCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    String textFill = isSelected() ? "#FFFFFF" : "#DFE1E5";
                    setStyle("-fx-text-fill: " + textFill + "; -fx-font-size: 12px; -fx-padding: 5 8 5 8;");
                }
            }
        };
    }

    private void styleToolButton(Button btn, String tooltipText) {
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #8C9099; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 8 2 8;");
        btn.setTooltip(new Tooltip(tooltipText));
        btn.setOnMouseEntered(e -> {
            if (!btn.isDisabled()) btn.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-background-radius: 3; -fx-text-fill: #DFE1E5; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 8 2 8;");
        });
        btn.setOnMouseExited(e -> {
            if (!btn.isDisabled()) btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #8C9099; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 8 2 8;");
        });
    }

    private void reloadTableData() {
        tableData.setAll(manager.getRequiredPlugins());
        table.setItems(tableData);
        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean hasSel = table.getSelectionModel().getSelectedItem() != null;
        removeButton.setDisable(!hasSel);
        editButton.setDisable(!hasSel);
    }

    private void onAdd() {
        Window owner = getScene() != null ? getScene().getWindow() : null;
        RequiredPluginDialog dialog = new RequiredPluginDialog(owner, null);
        RequiredPlugin created = dialog.showAndWait();
        if (created != null) {
            manager.addRequiredPlugin(created);
            table.getSelectionModel().select(created);
        }
    }

    private void onEdit() {
        RequiredPlugin selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Window owner = getScene() != null ? getScene().getWindow() : null;
        RequiredPluginDialog dialog = new RequiredPluginDialog(owner, selected);
        RequiredPlugin updated = dialog.showAndWait();
        if (updated != null) {
            manager.updateRequiredPlugin(selected, updated);
            table.getSelectionModel().select(updated);
        }
    }

    private void onRemove() {
        RequiredPlugin selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            manager.removeRequiredPlugin(selected);
        }
    }
}