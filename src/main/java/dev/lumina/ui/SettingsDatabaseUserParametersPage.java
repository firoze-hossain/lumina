package dev.lumina.ui;

import dev.lumina.database.DatabaseUserParameterPattern;
import dev.lumina.database.DatabaseUserParametersSettings;
import dev.lumina.database.DatabaseUserParametersSettingsManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Tools > Database > Query Execution > User Parameters settings page in Lumina IDE.
 * Matches 1:1 design with dark IDE theme.
 */
public class SettingsDatabaseUserParametersPage extends VBox {

    private final DatabaseUserParametersSettingsManager manager;
    private DatabaseUserParametersSettings initialSettings;
    private DatabaseUserParametersSettings currentSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    private CheckBox enableInConsolesCheck;
    private CheckBox enableInLiteralsCheck;
    private CheckBox substituteInsideStringsCheck;

    private Button addButton;
    private Button removeButton;
    private Button upButton;
    private Button downButton;

    private TableView<DatabaseUserParameterPattern> patternsTable;
    private ObservableList<DatabaseUserParameterPattern> tableData;

    public SettingsDatabaseUserParametersPage() {
        this.manager = DatabaseUserParametersSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");

        // Top checkboxes
        enableInConsolesCheck = createCheckBox("Enable in query consoles and SQL files");
        enableInLiteralsCheck = createCheckBox("Enable in string literals with SQL injection");
        substituteInsideStringsCheck = createCheckBox("Substitute inside SQL strings");

        VBox checkBoxesBox = new VBox(8, enableInConsolesCheck, enableInLiteralsCheck, substituteInsideStringsCheck);

        // Parameter patterns section
        Label sectionLabel = new Label("Parameter patterns:");
        sectionLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        // Toolbar
        HBox toolbar = new HBox(6);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(4, 6, 4, 6));
        toolbar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 1 1 0 1; -fx-border-radius: 4 4 0 0; -fx-background-radius: 4 4 0 0;");

        addButton = createToolbarButton("+", "Add parameter pattern");
        removeButton = createToolbarButton("-", "Remove selected pattern");
        upButton = createToolbarButton("↑", "Move up");
        downButton = createToolbarButton("↓", "Move down");

        addButton.setOnAction(e -> handleAddPattern());
        removeButton.setOnAction(e -> handleRemovePattern());
        upButton.setOnAction(e -> handleMoveUp());
        downButton.setOnAction(e -> handleMoveDown());

        toolbar.getChildren().addAll(addButton, removeButton, upButton, downButton);

        // Patterns Table
        patternsTable = new TableView<>();
        patternsTable.setEditable(true);
        patternsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        patternsTable.setPrefHeight(260);
        patternsTable.setMaxWidth(650);
        patternsTable.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 1 1 1; -fx-border-radius: 0 0 4 4; -fx-background-radius: 0 0 4 4;");

        TableColumn<DatabaseUserParameterPattern, String> patternCol = new TableColumn<>("Pattern");
        patternCol.setPrefWidth(180);
        patternCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPattern()));
        patternCol.setCellFactory(TextFieldTableCell.forTableColumn());
        patternCol.setOnEditCommit(evt -> {
            evt.getRowValue().setPattern(evt.getNewValue());
            syncTableToSettings();
            notifyModified();
        });

        TableColumn<DatabaseUserParameterPattern, String> scopeCol = new TableColumn<>("In");
        scopeCol.setPrefWidth(130);
        scopeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getInScope()));
        scopeCol.setCellFactory(TextFieldTableCell.forTableColumn());
        scopeCol.setOnEditCommit(evt -> {
            evt.getRowValue().setInScope(evt.getNewValue());
            syncTableToSettings();
            notifyModified();
        });

        TableColumn<DatabaseUserParameterPattern, String> langCol = new TableColumn<>("Language");
        langCol.setPrefWidth(220);
        langCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLanguage()));
        langCol.setCellFactory(TextFieldTableCell.forTableColumn());
        langCol.setOnEditCommit(evt -> {
            evt.getRowValue().setLanguage(evt.getNewValue());
            syncTableToSettings();
            notifyModified();
        });

        patternsTable.getColumns().addAll(patternCol, scopeCol, langCol);

        tableData = FXCollections.observableArrayList();
        patternsTable.setItems(tableData);

        patternsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> updateToolbarState());

        VBox tableContainer = new VBox(0, toolbar, patternsTable);
        tableContainer.setMaxWidth(650);

        getChildren().addAll(checkBoxesBox, sectionLabel, tableContainer);
    }

    private CheckBox createCheckBox(String text) {
        CheckBox cb = new CheckBox(text);
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        cb.selectedProperty().addListener((obs, oldV, newV) -> {
            if (!updating) {
                notifyModified();
            }
        });
        return cb;
    }

    private Button createToolbarButton(String text, String tooltip) {
        Button btn = new Button(text);
        btn.setTooltip(new Tooltip(tooltip));
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-min-width: 26px; -fx-min-height: 24px; -fx-padding: 2 6 2 6;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-min-width: 26px; -fx-min-height: 24px; -fx-padding: 2 6 2 6; -fx-background-radius: 3;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-min-width: 26px; -fx-min-height: 24px; -fx-padding: 2 6 2 6;"));
        return btn;
    }

    private void updateToolbarState() {
        int idx = patternsTable.getSelectionModel().getSelectedIndex();
        removeButton.setDisable(idx < 0);
        upButton.setDisable(idx <= 0);
        downButton.setDisable(idx < 0 || idx >= tableData.size() - 1);
    }

    private void handleAddPattern() {
        DatabaseUserParameterPattern newPattern = new DatabaseUserParameterPattern("\"#new_param#\"", "everywhere", "");
        tableData.add(newPattern);
        patternsTable.getSelectionModel().select(newPattern);
        syncTableToSettings();
        notifyModified();
    }

    private void handleRemovePattern() {
        int idx = patternsTable.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            tableData.remove(idx);
            syncTableToSettings();
            notifyModified();
        }
    }

    private void handleMoveUp() {
        int idx = patternsTable.getSelectionModel().getSelectedIndex();
        if (idx > 0) {
            DatabaseUserParameterPattern item = tableData.remove(idx);
            tableData.add(idx - 1, item);
            patternsTable.getSelectionModel().select(item);
            syncTableToSettings();
            notifyModified();
        }
    }

    private void handleMoveDown() {
        int idx = patternsTable.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < tableData.size() - 1) {
            DatabaseUserParameterPattern item = tableData.remove(idx);
            tableData.add(idx + 1, item);
            patternsTable.getSelectionModel().select(item);
            syncTableToSettings();
            notifyModified();
        }
    }

    private void syncTableToSettings() {
        if (currentSettings != null) {
            currentSettings.setPatterns(new ArrayList<>(tableData));
        }
    }

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();
            currentSettings = initialSettings.clone();

            enableInConsolesCheck.setSelected(currentSettings.isEnableInConsolesAndSqlFiles());
            enableInLiteralsCheck.setSelected(currentSettings.isEnableInLiteralsWithSqlInjection());
            substituteInsideStringsCheck.setSelected(currentSettings.isSubstituteInsideSqlStrings());

            tableData.clear();
            for (DatabaseUserParameterPattern p : currentSettings.getPatterns()) {
                tableData.add(p.clone());
            }

            if (!tableData.isEmpty()) {
                patternsTable.getSelectionModel().select(0);
            }
            updateToolbarState();
        } finally {
            updating = false;
        }
    }

    private DatabaseUserParametersSettings getCurrentSettingsFromUI() {
        DatabaseUserParametersSettings settings = new DatabaseUserParametersSettings();
        settings.setEnableInConsolesAndSqlFiles(enableInConsolesCheck.isSelected());
        settings.setEnableInLiteralsWithSqlInjection(enableInLiteralsCheck.isSelected());
        settings.setSubstituteInsideSqlStrings(substituteInsideStringsCheck.isSelected());
        settings.setPatterns(new ArrayList<>(tableData));
        return settings;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        DatabaseUserParametersSettings updated = getCurrentSettingsFromUI();
        manager.setSettings(updated);
        initialSettings = updated.clone();
        currentSettings = updated.clone();
        notifyModified();
    }

    public void reset() {
        loadData();
        notifyModified();
    }

    public void revertChanges() {
        reset();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public CheckBox getEnableInConsolesCheck() {
        return enableInConsolesCheck;
    }

    public CheckBox getEnableInLiteralsCheck() {
        return enableInLiteralsCheck;
    }

    public CheckBox getSubstituteInsideStringsCheck() {
        return substituteInsideStringsCheck;
    }

    public TableView<DatabaseUserParameterPattern> getPatternsTable() {
        return patternsTable;
    }

    public Button getAddButton() {
        return addButton;
    }

    public Button getRemoveButton() {
        return removeButton;
    }

    public Button getUpButton() {
        return upButton;
    }

    public Button getDownButton() {
        return downButton;
    }
}
