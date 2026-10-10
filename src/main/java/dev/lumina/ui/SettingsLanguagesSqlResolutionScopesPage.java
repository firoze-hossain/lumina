package dev.lumina.ui;

import dev.lumina.sql.SqlResolutionScopeMapping;
import dev.lumina.sql.SqlResolutionScopesSettings;
import dev.lumina.sql.SqlResolutionScopesSettingsManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Modality;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Settings page for Languages & Frameworks > SQL Resolution Scopes.
 * Matches reference screenshot media_1791600399648_fe125a89.png:
 *  - Project mapping: [ <Default> v ]
 *  - Toolbar: +  −  ✏
 *  - TableView:
 *      Columns: Path ^ | Resolution Scope
 *      Placeholder: New Mapping ⌘N
 *  - Bottom text:
 *      To configure the unqualified SQL names resolution for a file, a directory, or the entire project,
 *      add its path if necessary and then choose the desired data sources, databases and schemas in the
 *      drop-down.
 */
public class SettingsLanguagesSqlResolutionScopesPage extends VBox {

    private final SqlResolutionScopesSettingsManager manager = SqlResolutionScopesSettingsManager.getInstance();

    private final ComboBox<String> projectMappingCombo = new ComboBox<>();

    private final TableView<SqlResolutionScopeMapping> table = new TableView<>();
    private final TableColumn<SqlResolutionScopeMapping, String> pathCol = new TableColumn<>("Path");
    private final TableColumn<SqlResolutionScopeMapping, String> scopeCol = new TableColumn<>("Resolution Scope");
    private final ObservableList<SqlResolutionScopeMapping> tableData = FXCollections.observableArrayList();

    private final Button addBtn = new Button();
    private final Button removeBtn = new Button();
    private final Button editBtn = new Button();

    private SqlResolutionScopesSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsLanguagesSqlResolutionScopesPage() {
        setSpacing(10);
        setPadding(new Insets(14, 20, 16, 20));
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadData();
    }

    private void buildUI() {
        // --- 1. Top Control: Project mapping ---
        ObservableList<String> scopesList = FXCollections.observableArrayList(SqlResolutionScopesSettings.STANDARD_SCOPES);
        projectMappingCombo.setItems(scopesList);
        projectMappingCombo.setPrefWidth(130);
        projectMappingCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; " +
                "-fx-background-radius: 4; -fx-font-size: 13px; -fx-text-fill: #DFE1E5;");
        projectMappingCombo.setOnAction(e -> fireModified());

        Label projectLabel = new Label("Project mapping:");
        projectLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        projectLabel.setPrefWidth(120);

        HBox topRow = new HBox(12, projectLabel, projectMappingCombo);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // --- 2. Toolbar (+, -, edit) ---
        HBox toolbar = buildToolbar();

        // --- 3. TableView ---
        buildTable();

        VBox tableContainer = new VBox(0, toolbar, table);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        // --- 4. Bottom Description ---
        Label footerDesc = new Label("To configure the unqualified SQL names resolution for a file, a directory, or the entire project, " +
                "add its path if necessary and then choose the desired data sources, databases and schemas in the drop-down.");
        footerDesc.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px;");
        footerDesc.setWrapText(true);
        footerDesc.setPadding(new Insets(6, 0, 0, 0));

        getChildren().addAll(topRow, tableContainer, footerDesc);
    }

    private HBox buildToolbar() {
        HBox bar = new HBox(4);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 0, 4, 0));

        // Plus
        SVGPath plus = new SVGPath();
        plus.setContent("M 6 2 L 6 10 M 2 6 L 10 6");
        styleSvg(plus);
        addBtn.setGraphic(plus);
        styleToolBtn(addBtn, "Add (Alt+Insert / ⌘N)");
        addBtn.setOnAction(e -> onAdd());

        // Minus
        SVGPath minus = new SVGPath();
        minus.setContent("M 2 6 L 10 6");
        styleSvg(minus);
        removeBtn.setGraphic(minus);
        styleToolBtn(removeBtn, "Remove (Delete / ⌫)");
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

    private void styleToolBtn(Button btn, String tooltip) {
        btn.setStyle("-fx-background-color: transparent; -fx-padding: 4 6 4 6; -fx-background-radius: 3; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #393B40; -fx-padding: 4 6 4 6; -fx-background-radius: 3; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-padding: 4 6 4 6; -fx-background-radius: 3; -fx-cursor: hand;"));
        btn.setTooltip(new Tooltip(tooltip));
    }

    private void buildTable() {
        table.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-table-cell-border-color: #2B2D30; " +
                "-fx-border-color: #393B40; -fx-border-radius: 3;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        pathCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPath()));
        pathCol.setCellFactory(col -> new TableCell<>() {
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

        scopeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getResolutionScope()));
        scopeCol.setCellFactory(col -> new TableCell<>() {
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

        table.getColumns().setAll(pathCol, scopeCol);
        table.setItems(tableData);

        Label placeholder = new Label("New Mapping ⌘N");
        placeholder.setStyle("-fx-text-fill: #707278; -fx-font-size: 13px;");
        table.setPlaceholder(placeholder);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            boolean hasSel = newSel != null;
            removeBtn.setDisable(!hasSel);
            editBtn.setDisable(!hasSel);
        });

        table.setRowFactory(tv -> {
            TableRow<SqlResolutionScopeMapping> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    onEdit();
                }
            });
            return row;
        });
    }

    private void onAdd() {
        showMappingDialog(null).ifPresent(mapping -> {
            tableData.add(mapping);
            table.getSelectionModel().select(mapping);
            fireModified();
        });
    }

    private void onEdit() {
        SqlResolutionScopeMapping selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        showMappingDialog(selected).ifPresent(updated -> {
            selected.setPath(updated.getPath());
            selected.setResolutionScope(updated.getResolutionScope());
            table.refresh();
            fireModified();
        });
    }

    private void onRemove() {
        SqlResolutionScopeMapping selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            tableData.remove(selected);
            fireModified();
        }
    }

    private Optional<SqlResolutionScopeMapping> showMappingDialog(SqlResolutionScopeMapping existing) {
        Dialog<SqlResolutionScopeMapping> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add SQL Resolution Scope Mapping" : "Edit SQL Resolution Scope Mapping");
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (getScene() != null && getScene().getWindow() != null) {
            dialog.initOwner(getScene().getWindow());
        }

        dialog.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField pathField = new TextField(existing != null ? existing.getPath() : "");
        pathField.setPromptText("File or directory path");
        pathField.setPrefWidth(280);
        pathField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #707278; " +
                "-fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8; -fx-font-size: 13px;");

        Button browseBtn = new Button("📁");
        browseBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #8C8C8C; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand; -fx-font-size: 12px;");
        browseBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select SQL File or Directory");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("SQL Files (*.sql, *.ddl, *.dml)", "*.sql", "*.ddl", "*.dml"),
                    new FileChooser.ExtensionFilter("All Files (*.*)", "*.*")
            );
            File f = chooser.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
            if (f != null) {
                pathField.setText(f.getAbsolutePath());
            }
        });

        HBox pathRow = new HBox(6, pathField, browseBtn);
        pathRow.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> scopeCombo = new ComboBox<>(FXCollections.observableArrayList(SqlResolutionScopesSettings.STANDARD_SCOPES));
        scopeCombo.setEditable(true);
        scopeCombo.setPrefWidth(200);
        scopeCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; " +
                "-fx-background-radius: 4; -fx-font-size: 13px; -fx-text-fill: #DFE1E5;");
        scopeCombo.setValue(existing != null ? existing.getResolutionScope() : "<Default>");

        Label pathLabel = new Label("Path:");
        pathLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        pathLabel.setPrefWidth(110);

        Label scopeLabel = new Label("Resolution Scope:");
        scopeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        scopeLabel.setPrefWidth(110);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 24, 16, 24));
        grid.addRow(0, pathLabel, pathRow);
        grid.addRow(1, scopeLabel, scopeCombo);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btnType -> {
            if (btnType == ButtonType.OK) {
                String path = pathField.getText().trim();
                String scope = scopeCombo.getValue() != null ? scopeCombo.getValue().trim() : "<Default>";
                if (scope.isBlank()) scope = "<Default>";
                return new SqlResolutionScopeMapping(path, scope);
            }
            return null;
        });

        return dialog.showAndWait();
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        projectMappingCombo.setValue(initialSettings.getProjectMapping());

        tableData.clear();
        for (SqlResolutionScopeMapping m : initialSettings.getMappings()) {
            tableData.add(m.clone());
        }

        updating = false;
    }

    public SqlResolutionScopesSettings getCurrentSettings() {
        SqlResolutionScopesSettings s = new SqlResolutionScopesSettings();
        s.setProjectMapping(projectMappingCombo.getValue() != null ? projectMappingCombo.getValue() : "<Default>");

        List<SqlResolutionScopeMapping> mappings = new ArrayList<>();
        for (SqlResolutionScopeMapping m : tableData) {
            mappings.add(m.clone());
        }
        s.setMappings(mappings);
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettings());
    }

    public void apply() {
        SqlResolutionScopesSettings current = getCurrentSettings();
        manager.setSettings(current);
        initialSettings = current.clone();
        if (onModifiedListener != null) onModifiedListener.run();
    }

    public void reset() {
        loadData();
        if (onModifiedListener != null) onModifiedListener.run();
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

    // Getters for programmatic inspection and testing
    public ComboBox<String> getProjectMappingCombo() {
        return projectMappingCombo;
    }

    public TableView<SqlResolutionScopeMapping> getTable() {
        return table;
    }

    public ObservableList<SqlResolutionScopeMapping> getTableData() {
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
