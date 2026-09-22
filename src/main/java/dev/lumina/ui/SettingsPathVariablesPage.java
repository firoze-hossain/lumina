package dev.lumina.ui;

import dev.lumina.pathvar.PathVariable;
import dev.lumina.pathvar.PathVariablesManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

/**
 * Path Variables settings page.
 * Strictly matches media_1790046850137.png.
 */
public class SettingsPathVariablesPage extends VBox {

    private final PathVariablesManager manager = PathVariablesManager.getInstance();
    private final TableView<PathVariable> table = new TableView<>();
    private final ObservableList<PathVariable> tableData = FXCollections.observableArrayList();

    private final Button addButton = new Button("+");
    private final Button removeButton = new Button("−");
    private final Button editButton = new Button("✎");

    private final TextField ignoredVarsField = new TextField();

    public SettingsPathVariablesPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(14, 24, 16, 24));
        setSpacing(10);
        VBox.setVgrow(this, Priority.ALWAYS);

        // 1. Toolbar (+, -, Edit)
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 4, 0));

        styleToolButton(addButton, "Add variable");
        addButton.setOnAction(e -> onAdd());

        styleToolButton(removeButton, "Remove");
        removeButton.setOnAction(e -> onRemove());

        styleToolButton(editButton, "Edit");
        editButton.setOnAction(e -> onEdit());

        toolbar.getChildren().addAll(addButton, removeButton, editButton);

        // 2. TableView
        table.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-background: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<PathVariable, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(d -> d.getValue().nameProperty());
        nameCol.setPrefWidth(220);
        nameCol.setCellFactory(col -> createStyledCell());

        TableColumn<PathVariable, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(d -> d.getValue().valueProperty());
        valueCol.setPrefWidth(420);
        valueCol.setCellFactory(col -> createStyledCell());

        table.getColumns().addAll(nameCol, valueCol);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> updateButtonStates());

        table.setRowFactory(tv -> {
            TableRow<PathVariable> row = new TableRow<>() {
                @Override
                protected void updateItem(PathVariable item, boolean empty) {
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

        // 3. Bottom Ignored Variables
        VBox bottomBox = new VBox(4);
        bottomBox.setPadding(new Insets(10, 0, 0, 0));

        HBox ignoredRow = new HBox(12);
        ignoredRow.setAlignment(Pos.CENTER_LEFT);

        Label ignoredLbl = new Label("Ignored Variables:");
        ignoredLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-min-width: 110px;");

        ignoredVarsField.setText(manager.getIgnoredVariables());
        ignoredVarsField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8 4 8; -fx-font-size: 12px;");
        HBox.setHgrow(ignoredVarsField, Priority.ALWAYS);
        ignoredVarsField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                manager.setIgnoredVariables(newV);
            }
        });

        ignoredRow.getChildren().addAll(ignoredLbl, ignoredVarsField);

        Label hintLbl = new Label("Use ; to separate ignored variables");
        hintLbl.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px; -fx-padding: 0 0 0 122;");

        bottomBox.getChildren().addAll(ignoredRow, hintLbl);

        getChildren().addAll(toolbar, table, bottomBox);

        manager.addListener(this::reloadTableData);
        reloadTableData();
    }

    private TableCell<PathVariable, String> createStyledCell() {
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
        tableData.setAll(manager.getVariables());
        table.setItems(tableData);
        ignoredVarsField.setText(manager.getIgnoredVariables());
        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean hasSel = table.getSelectionModel().getSelectedItem() != null;
        removeButton.setDisable(!hasSel);
        editButton.setDisable(!hasSel);
    }

    private void onAdd() {
        Window owner = getScene() != null ? getScene().getWindow() : null;
        PathVariableDialog dialog = new PathVariableDialog(owner, null);
        PathVariable created = dialog.showAndWait();
        if (created != null) {
            manager.addVariable(created);
            table.getSelectionModel().select(created);
        }
    }

    private void onEdit() {
        PathVariable selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Window owner = getScene() != null ? getScene().getWindow() : null;
        PathVariableDialog dialog = new PathVariableDialog(owner, selected);
        PathVariable updated = dialog.showAndWait();
        if (updated != null) {
            manager.updateVariable(selected, updated);
            table.getSelectionModel().select(updated);
        }
    }

    private void onRemove() {
        PathVariable selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            manager.removeVariable(selected);
        }
    }
}