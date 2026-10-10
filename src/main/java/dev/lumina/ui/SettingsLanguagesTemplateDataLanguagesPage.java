package dev.lumina.ui;

import dev.lumina.templates.TemplateDataLanguageMapping;
import dev.lumina.templates.TemplateDataLanguagesSettings;
import dev.lumina.templates.TemplateDataLanguagesSettingsManager;
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
 * Settings page for Languages & Frameworks > Template Data Languages.
 * Matches reference screenshot media_1791602845046_bc1f39dd.png:
 *  - Project Language: [ <None> v ]
 *  - Toolbar: [ + ] [ - ] [ ✏ ]
 *  - TableView: Path ^ | Language
 *  - Placeholder: New Mapping ⌘N
 *  - Bottom explanation using Lumina branding.
 */
public class SettingsLanguagesTemplateDataLanguagesPage extends VBox {

    private final TemplateDataLanguagesSettingsManager manager = TemplateDataLanguagesSettingsManager.getInstance();

    private final ComboBox<String> projectLanguageCombo = new ComboBox<>();
    private final TableView<TemplateDataLanguageMapping> table = new TableView<>();
    private final ObservableList<TemplateDataLanguageMapping> tableData = FXCollections.observableArrayList();

    private final Button addBtn = new Button();
    private final Button removeBtn = new Button();
    private final Button editBtn = new Button();

    private TemplateDataLanguagesSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsLanguagesTemplateDataLanguagesPage() {
        setSpacing(8);
        setPadding(new Insets(14, 20, 16, 20));
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadData();
    }

    private void buildUI() {
        // --- 1. Project Language row ---
        ObservableList<String> languagesList = FXCollections.observableArrayList(TemplateDataLanguagesSettings.STANDARD_LANGUAGES);
        projectLanguageCombo.setItems(languagesList);
        projectLanguageCombo.setValue("<None>");
        projectLanguageCombo.setPrefWidth(130);
        projectLanguageCombo.setStyle(
                "-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; " +
                "-fx-background-radius: 4; -fx-font-size: 13px; -fx-text-fill: #DFE1E5;"
        );
        projectLanguageCombo.setOnAction(e -> fireModified());

        Label projectLabel = new Label("Project Language:");
        projectLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        projectLabel.setPrefWidth(130);

        HBox projectRow = new HBox(8, projectLabel, projectLanguageCombo);
        projectRow.setAlignment(Pos.CENTER_LEFT);

        // --- 2. Toolbar (+, -, edit) ---
        HBox toolbar = buildToolbar();

        // --- 3. TableView ---
        buildTable();

        VBox tableContainer = new VBox(0, toolbar, table);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        // --- 4. Bottom Description ---
        Label footerDesc = new Label(
                "Template data languages are the underlying languages in template files like those of " +
                "FreeMarker/Velocity frameworks. To change template data language settings Lumina uses for " +
                "a file, a directory, or the entire project, add its path if necessary and then select a " +
                "language from the drop-down list."
        );
        footerDesc.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px;");
        footerDesc.setWrapText(true);
        footerDesc.setPadding(new Insets(6, 0, 0, 0));

        getChildren().addAll(projectRow, tableContainer, footerDesc);
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
        TableColumn<TemplateDataLanguageMapping, String> pathCol = new TableColumn<>("Path");
        pathCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPath()));
        pathCol.setCellFactory(TextFieldTableCell.forTableColumn());
        pathCol.setPrefWidth(350);

        // Language Column
        TableColumn<TemplateDataLanguageMapping, String> langCol = new TableColumn<>("Language");
        langCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLanguage()));
        langCol.setPrefWidth(200);

        table.getColumns().setAll(pathCol, langCol);

        // Placeholder: New Mapping ⌘N
        Label placeholder = new Label("New Mapping ⌘N");
        placeholder.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 13px;");
        table.setPlaceholder(placeholder);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            boolean hasSelection = newV != null;
            removeBtn.setDisable(!hasSelection);
            editBtn.setDisable(!hasSelection);
        });

        table.setRowFactory(tv -> {
            TableRow<TemplateDataLanguageMapping> row = new TableRow<>();
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
        TemplateDataLanguageMapping selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showMappingDialog(selected);
        }
    }

    private void onRemove() {
        TemplateDataLanguageMapping selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            tableData.remove(selected);
            fireModified();
        }
    }

    private void showMappingDialog(TemplateDataLanguageMapping existing) {
        Dialog<TemplateDataLanguageMapping> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Template Data Language Mapping" : "Edit Template Data Language Mapping");

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
            fc.setTitle("Select Template File");
            File f = fc.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (f != null) {
                pathField.setText(f.getAbsolutePath());
            }
        });

        Button browseDirBtn = new Button("Directory...");
        browseDirBtn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        browseDirBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Template Directory");
            File d = dc.showDialog(getScene() != null ? getScene().getWindow() : null);
            if (d != null) {
                pathField.setText(d.getAbsolutePath());
            }
        });

        HBox pathRow = new HBox(6, pathField, browseFileBtn, browseDirBtn);
        pathRow.setAlignment(Pos.CENTER_LEFT);

        Label langLabel = new Label("Template Data Language:");
        langLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        ComboBox<String> langCombo = new ComboBox<>(FXCollections.observableArrayList(TemplateDataLanguagesSettings.STANDARD_LANGUAGES));
        langCombo.setValue(existing != null ? existing.getLanguage() : "HTML");
        langCombo.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #43454A; -fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        langCombo.setMaxWidth(Double.MAX_VALUE);

        content.getChildren().addAll(pathLabel, pathRow, langLabel, langCombo);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn == okBtnType) {
                String path = pathField.getText().trim();
                String lang = langCombo.getValue();
                if (!path.isEmpty()) {
                    return new TemplateDataLanguageMapping(path, lang != null ? lang : "<None>");
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(mapping -> {
            if (existing != null) {
                existing.setPath(mapping.getPath());
                existing.setLanguage(mapping.getLanguage());
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

    private void applySettingsToUI(TemplateDataLanguagesSettings s) {
        if (s == null) return;
        projectLanguageCombo.setValue(s.getProjectLanguage());
        tableData.clear();
        for (TemplateDataLanguageMapping m : s.getMappings()) {
            tableData.add(m.clone());
        }
    }

    public TemplateDataLanguagesSettings getCurrentSettingsFromUI() {
        TemplateDataLanguagesSettings s = new TemplateDataLanguagesSettings();
        s.setProjectLanguage(projectLanguageCombo.getValue());
        List<TemplateDataLanguageMapping> list = new ArrayList<>();
        for (TemplateDataLanguageMapping m : tableData) {
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
        TemplateDataLanguagesSettings updated = getCurrentSettingsFromUI();
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

    // Getters for programmatic inspection and testing
    public ComboBox<String> getProjectLanguageCombo() {
        return projectLanguageCombo;
    }

    public TableView<TemplateDataLanguageMapping> getTable() {
        return table;
    }

    public ObservableList<TemplateDataLanguageMapping> getTableData() {
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
