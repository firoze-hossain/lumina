package dev.lumina.ui;

import dev.lumina.git.VcsDirectoryMappingManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

import java.util.List;

/**
 * Version Control > Directory Mappings settings page matching IntelliJ IDEA Images 1–4.
 */
public class SettingsVcsDirectoryMappingsPage extends VBox {

    private final VcsDirectoryMappingManager manager = VcsDirectoryMappingManager.getInstance();

    private final TableView<VcsDirectoryMappingManager.VcsMapping> table = new TableView<>();
    private final TableColumn<VcsDirectoryMappingManager.VcsMapping, String> dirCol = new TableColumn<>("Directory");
    private final TableColumn<VcsDirectoryMappingManager.VcsMapping, String> vcsCol = new TableColumn<>("VCS");

    private final Button addBtn = new Button();
    private final Button removeBtn = new Button();
    private final Button editBtn = new Button();

    private final CheckBox autoDetectCheck = new CheckBox("Enable automatic mapping detection");

    public SettingsVcsDirectoryMappingsPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(14, 20, 14, 20));
        setSpacing(10);
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");

        // 1. Toolbar
        HBox toolbar = buildToolbar();

        // 2. TableView
        buildTable();

        // 3. Bottom Description
        Label desc = new Label("<Project> - Content roots of all modules, all immediate descendants of project base directory, and .idea directory contents");
        desc.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");

        // 4. Auto detection checkbox
        autoDetectCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        autoDetectCheck.setSelected(manager.isAutomaticMappingDetection());
        autoDetectCheck.setOnAction(e -> manager.setAutomaticMappingDetection(autoDetectCheck.isSelected()));

        Label infoIcon = new Label("?");
        infoIcon.setStyle("-fx-background-color: transparent; -fx-border-color: #6F737A; -fx-border-radius: 10; -fx-text-fill: #848BA3; -fx-font-size: 10px; -fx-min-width: 14px; -fx-min-height: 14px; -fx-max-width: 14px; -fx-max-height: 14px; -fx-alignment: center; -fx-cursor: hand;");
        infoIcon.setTooltip(new Tooltip("When enabled, Lumina automatically detects VCS roots when opening or modifying projects"));

        HBox autoDetectBox = new HBox(6, autoDetectCheck, infoIcon);
        autoDetectBox.setAlignment(Pos.CENTER_LEFT);
        autoDetectBox.setPadding(new Insets(4, 0, 0, 0));

        getChildren().addAll(toolbar, table, desc, autoDetectBox);
        VBox.setVgrow(table, Priority.ALWAYS);

        manager.addListener(this::refreshTable);
        refreshTable();
    }

    private HBox buildToolbar() {
        HBox bar = new HBox(6);
        bar.setAlignment(Pos.CENTER_LEFT);

        // Plus
        SVGPath plus = new SVGPath();
        plus.setContent("M 6 2 L 6 10 M 2 6 L 10 6");
        styleSvg(plus);
        addBtn.setGraphic(plus);
        styleToolBtn(addBtn, "Add (Alt+Insert)");
        addBtn.setOnAction(e -> onAdd());

        // Minus
        SVGPath minus = new SVGPath();
        minus.setContent("M 2 6 L 10 6");
        styleSvg(minus);
        removeBtn.setGraphic(minus);
        styleToolBtn(removeBtn, "Remove (Delete)");
        removeBtn.setOnAction(e -> onRemove());

        // Edit pencil
        SVGPath edit = new SVGPath();
        edit.setContent("M 2 8.5 L 2 10 L 3.5 10 L 9 4.5 L 7.5 3 Z M 8 2 L 9.5 3.5");
        styleSvg(edit);
        editBtn.setGraphic(edit);
        styleToolBtn(editBtn, "Edit (Enter)");
        editBtn.setOnAction(e -> onEdit());

        bar.getChildren().addAll(addBtn, removeBtn, editBtn);
        return bar;
    }

    private void styleSvg(SVGPath p) {
        p.setFill(Color.TRANSPARENT);
        p.setStroke(Color.web("#AFB1B6"));
        p.setStrokeWidth(1.2);
    }

    private void styleToolBtn(Button btn, String tooltip) {
        btn.setStyle("-fx-background-color: transparent; -fx-padding: 4 6 4 6; -fx-background-radius: 3; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #393B40; -fx-padding: 4 6 4 6; -fx-background-radius: 3; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-padding: 4 6 4 6; -fx-background-radius: 3; -fx-cursor: hand;"));
        btn.setTooltip(new Tooltip(tooltip));
    }

    private void buildTable() {
        table.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-table-cell-border-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 3;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(380);

        dirCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDirectory()));
        dirCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (VcsDirectoryMappingManager.PROJECT_MAPPING.equalsIgnoreCase(item)) {
                        HBox box = new HBox(8);
                        box.setAlignment(Pos.CENTER_LEFT);
                        Label projLbl = new Label(item);
                        projLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

                        int detected = manager.getDetectedRepositoriesCount();
                        Label detLbl = new Label("Detected " + detected);
                        detLbl.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");

                        box.getChildren().addAll(projLbl, detLbl);
                        setGraphic(box);
                        setText(null);
                    } else {
                        setText(item);
                        setGraphic(null);
                        setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                    }
                }
            }
        });

        vcsCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getVcs()));
        vcsCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                }
            }
        });

        dirCol.prefWidthProperty().bind(table.widthProperty().multiply(0.85));
        vcsCol.prefWidthProperty().bind(table.widthProperty().multiply(0.15));

        table.getColumns().setAll(dirCol, vcsCol);

        table.setRowFactory(tv -> {
            TableRow<VcsDirectoryMappingManager.VcsMapping> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    onEdit();
                }
            });
            return row;
        });

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> {
            boolean hasSel = item != null;
            removeBtn.setDisable(!hasSel);
            editBtn.setDisable(!hasSel);
        });
    }

    private void onAdd() {
        Stage owner = (Stage) getScene().getWindow();
        VcsDirectoryMappingDialog dialog = new VcsDirectoryMappingDialog(owner, false, "", "Git");
        dialog.showAndWait();
        if (dialog.isSaved()) {
            manager.addMapping(dialog.getResultDirectory(), dialog.getResultVcs());
            refreshTable();
        }
    }

    private void onEdit() {
        VcsDirectoryMappingManager.VcsMapping selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        int idx = table.getSelectionModel().getSelectedIndex();

        Stage owner = (Stage) getScene().getWindow();
        VcsDirectoryMappingDialog dialog = new VcsDirectoryMappingDialog(owner, true, selected.getDirectory(), selected.getVcs());
        dialog.showAndWait();
        if (dialog.isSaved()) {
            manager.updateMapping(idx, dialog.getResultDirectory(), dialog.getResultVcs());
            refreshTable();
        }
    }

    private void onRemove() {
        int idx = table.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            manager.removeMapping(idx);
            refreshTable();
        }
    }

    private void refreshTable() {
        List<VcsDirectoryMappingManager.VcsMapping> list = manager.getMappings();
        table.getItems().setAll(list);
        if (!table.getItems().isEmpty() && table.getSelectionModel().isEmpty()) {
            table.getSelectionModel().selectFirst();
        }
    }
}
