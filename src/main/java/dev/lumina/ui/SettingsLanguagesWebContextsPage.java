package dev.lumina.ui;

import dev.lumina.web.WebContextMapping;
import dev.lumina.web.WebContextsSettings;
import dev.lumina.web.WebContextsSettingsManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings page for Languages & Frameworks > Web Contexts.
 * Matches reference screenshot media_1791604029588_6d4f3b81.png:
 *  - Toolbar: [ + ] [ - ] [ ✏ ]
 *  - TableView: Path ^ | Web Context
 *  - Placeholder: No web directories found, add one in Project Settings | Facets first
 *  - Footer explanation using Lumina branding.
 */
public class SettingsLanguagesWebContextsPage extends VBox {

    private final WebContextsSettingsManager manager = WebContextsSettingsManager.getInstance();

    private final TableView<WebContextMapping> table = new TableView<>();
    private final ObservableList<WebContextMapping> tableData = FXCollections.observableArrayList();

    private final Button addBtn = new Button();
    private final Button removeBtn = new Button();
    private final Button editBtn = new Button();

    private WebContextsSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsLanguagesWebContextsPage() {
        setSpacing(8);
        setPadding(new Insets(14, 20, 16, 20));
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadData();
    }

    private void buildUI() {
        // --- 1. Toolbar (+, -, edit) ---
        HBox toolbar = buildToolbar();

        // --- 2. TableView ---
        buildTable();

        VBox tableContainer = new VBox(0, toolbar, table);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        // --- 3. Bottom Description ---
        Label footerDesc = new Label(
                "To change Web Context settings that Lumina uses to resolve web paths in HTML and JSP for " +
                "a file, a directory, or the entire project, add its path if necessary and then choose a context from the drop-down list."
        );
        footerDesc.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px;");
        footerDesc.setWrapText(true);
        footerDesc.setPadding(new Insets(6, 0, 0, 0));

        getChildren().addAll(tableContainer, footerDesc);
    }

    private HBox buildToolbar() {
        HBox bar = new HBox(4);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(4, 0, 4, 0));

        // Plus
        SVGPath plus = new SVGPath();
        plus.setContent("M 6 2 L 6 10 M 2 6 L 10 6");
        styleSvg(plus);
        addBtn.setGraphic(plus);
        styleToolBtn(addBtn, "Add");
        addBtn.setOnAction(e -> onAdd());

        // Minus
        SVGPath minus = new SVGPath();
        minus.setContent("M 2 6 L 10 6");
        styleSvg(minus);
        removeBtn.setGraphic(minus);
        styleToolBtn(removeBtn, "Remove");
        removeBtn.setDisable(true);
        removeBtn.setOnAction(e -> onRemove());

        // Edit
        SVGPath edit = new SVGPath();
        edit.setContent("M 2 8.5 L 2 10 L 3.5 10 L 9 4.5 L 7.5 3 Z M 8 2 L 9.5 3.5");
        styleSvg(edit);
        editBtn.setGraphic(edit);
        styleToolBtn(editBtn, "Edit");
        editBtn.setDisable(true);
        editBtn.setOnAction(e -> onEdit());

        bar.getChildren().addAll(addBtn, removeBtn, editBtn);
        return bar;
    }

    private void styleSvg(SVGPath p) {
        p.setFill(Color.TRANSPARENT);
        p.setStroke(Color.web("#AFB1B6"));
        p.setStrokeWidth(1.2);
    }

    private void styleToolBtn(Button btn, String tooltipText) {
        btn.setStyle("-fx-background-color: transparent; -fx-padding: 4 6 4 6; -fx-cursor: hand;");
        btn.setTooltip(new Tooltip(tooltipText));
        btn.setOnMouseEntered(e -> {
            if (!btn.isDisable()) btn.setStyle("-fx-background-color: #35373B; -fx-padding: 4 6 4 6; -fx-background-radius: 4; -fx-cursor: hand;");
        });
        btn.setOnMouseExited(e -> {
            if (!btn.isDisable()) btn.setStyle("-fx-background-color: transparent; -fx-padding: 4 6 4 6; -fx-cursor: hand;");
        });
    }

    @SuppressWarnings("unchecked")
    private void buildTable() {
        table.setItems(tableData);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle(
                "-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 1; " +
                "-fx-font-size: 13px; -fx-text-fill: #DFE1E5;"
        );
        VBox.setVgrow(table, Priority.ALWAYS);

        // Path Column
        TableColumn<WebContextMapping, String> pathCol = new TableColumn<>("Path");
        pathCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPath()));
        pathCol.setCellFactory(TextFieldTableCell.forTableColumn());
        pathCol.setPrefWidth(350);

        // Web Context Column
        TableColumn<WebContextMapping, String> ctxCol = new TableColumn<>("Web Context");
        ctxCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getWebContext()));
        ctxCol.setPrefWidth(200);

        table.getColumns().setAll(pathCol, ctxCol);

        // Placeholder matching screenshot exactly
        Label placeholder = new Label("No web directories found, add one in Project Settings | Facets first");
        placeholder.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 13px;");
        table.setPlaceholder(placeholder);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            boolean hasSelection = newV != null;
            removeBtn.setDisable(!hasSelection);
            editBtn.setDisable(!hasSelection);
        });

        table.setRowFactory(tv -> {
            TableRow<WebContextMapping> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    onEdit();
                }
            });
            return row;
        });
    }

    private void onAdd() {
        showMappingDialog(null);
    }

    private void onEdit() {
        WebContextMapping selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showMappingDialog(selected);
        }
    }

    private void onRemove() {
        WebContextMapping selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            tableData.remove(selected);
            fireModified();
        }
    }

    private void showMappingDialog(WebContextMapping existing) {
        Dialog<WebContextMapping> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Web Context Mapping" : "Edit Web Context Mapping");

        ButtonType okBtnType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okBtnType, ButtonType.CANCEL);

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.setPrefWidth(460);
        content.setStyle("-fx-background-color: #2B2D30;");
        dialog.getDialogPane().setStyle("-fx-background-color: #2B2D30;");

        Label pathLabel = new Label("Path:");
        pathLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        TextField pathField = new TextField(existing != null ? existing.getPath() : "");
        pathField.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #43454A; -fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        HBox.setHgrow(pathField, Priority.ALWAYS);

        Button browseFileBtn = new Button("File...");
        browseFileBtn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        browseFileBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Web Resource File");
            File f = fc.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (f != null) {
                pathField.setText(f.getAbsolutePath());
            }
        });

        Button browseDirBtn = new Button("Directory...");
        browseDirBtn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        browseDirBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Web Directory");
            File d = dc.showDialog(getScene() != null ? getScene().getWindow() : null);
            if (d != null) {
                pathField.setText(d.getAbsolutePath());
            }
        });

        HBox pathRow = new HBox(6, pathField, browseFileBtn, browseDirBtn);
        pathRow.setAlignment(Pos.CENTER_LEFT);

        Label ctxLabel = new Label("Web Context:");
        ctxLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        TextField ctxField = new TextField(existing != null ? existing.getWebContext() : "/");
        ctxField.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #43454A; -fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        content.getChildren().addAll(pathLabel, pathRow, ctxLabel, ctxField);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn == okBtnType) {
                String path = pathField.getText().trim();
                String ctx = ctxField.getText().trim();
                if (!path.isEmpty()) {
                    return new WebContextMapping(path, ctx.isEmpty() ? "/" : ctx);
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(mapping -> {
            if (existing != null) {
                existing.setPath(mapping.getPath());
                existing.setWebContext(mapping.getWebContext());
                table.refresh();
            } else {
                tableData.removeIf(m -> m.getPath().equals(mapping.getPath()));
                tableData.add(mapping);
            }
            fireModified();
        });
    }

    public void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();
            applySettingsToUI(initialSettings);
        } finally {
            updating = false;
        }
    }

    private void applySettingsToUI(WebContextsSettings s) {
        if (s == null) return;
        tableData.clear();
        for (WebContextMapping m : s.getMappings()) {
            tableData.add(m.clone());
        }
    }

    public WebContextsSettings getCurrentSettingsFromUI() {
        WebContextsSettings s = new WebContextsSettings();
        List<WebContextMapping> list = new ArrayList<>();
        for (WebContextMapping m : tableData) {
            list.add(m.clone());
        }
        s.setMappings(list);
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        WebContextsSettings updated = getCurrentSettingsFromUI();
        manager.setSettings(updated);
        initialSettings = updated.clone();
        fireModified();
    }

    public void reset() {
        loadData();
        fireModified();
    }

    public void revertChanges() {
        reset();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    // Getters for inspection and testing
    public TableView<WebContextMapping> getTable() {
        return table;
    }

    public ObservableList<WebContextMapping> getTableData() {
        return tableData;
    }

    public Button getAddBtn() {
        return addBtn;
    }

    public Button getRemoveBtn() {
        return removeBtn;
    }

    public Button getEditBtn() {
        return editBtn;
    }
}
