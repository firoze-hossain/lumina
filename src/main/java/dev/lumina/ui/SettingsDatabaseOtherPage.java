package dev.lumina.ui;

import dev.lumina.database.DatabaseOtherSettings;
import dev.lumina.database.DatabaseOtherSettingsManager;
import dev.lumina.database.DatabaseVirtualForeignKey;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Tools > Database > Other settings page in Lumina IDE matching 1:1 design of the reference IDE.
 */
public class SettingsDatabaseOtherPage extends VBox {

    private final DatabaseOtherSettingsManager manager;
    private DatabaseOtherSettings initialSettings;
    private DatabaseOtherSettings currentSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    // Modify Object
    private CheckBox confirmCancellationCheck;

    // Refactoring
    private CheckBox showPreviewValidScriptCheck;

    // DDL Mappings
    private CheckBox suggestDumpingDdlCheck;

    // Code Generation
    private ComboBox<String> generateContextTemplatesCombo;

    // Database Explorer
    private CheckBox rememberFilterCheck;

    // Virtual Foreign Keys
    private Button addVfkButton;
    private Button removeVfkButton;
    private Button testVfkButton;
    private TableView<DatabaseVirtualForeignKey> vfkTable;
    private ObservableList<DatabaseVirtualForeignKey> vfkTableData;

    // SQL Resolution
    private ComboBox<String> defaultResolveModeCombo;

    // Editor
    private TextField statementDelimiterField;

    public SettingsDatabaseOtherPage() {
        this.manager = DatabaseOtherSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");

        // 1. Modify Object
        VBox modifyObjectBox = new VBox(6);
        modifyObjectBox.getChildren().add(createSectionHeader("Modify Object"));
        confirmCancellationCheck = createCheckBox("Confirm cancellation for dialogs that modify schema");
        modifyObjectBox.getChildren().add(confirmCancellationCheck);

        // 2. Refactoring
        VBox refactoringBox = new VBox(6);
        refactoringBox.getChildren().add(createSectionHeader("Refactoring"));
        showPreviewValidScriptCheck = createCheckBox("Show preview of valid script when updating source text");
        refactoringBox.getChildren().add(showPreviewValidScriptCheck);

        // 3. DDL Mappings
        VBox ddlMappingsBox = new VBox(6);
        ddlMappingsBox.getChildren().add(createSectionHeader("DDL Mappings"));
        suggestDumpingDdlCheck = createCheckBox("Suggest dumping DDL for new mappings");
        ddlMappingsBox.getChildren().add(suggestDumpingDdlCheck);

        // 4. Code Generation
        VBox codeGenBox = new VBox(6);
        codeGenBox.getChildren().add(createSectionHeader("Code Generation"));
        Label genTemplatesLabel = createFieldLabel("Generate context templates:");
        genTemplatesLabel.setPrefWidth(200);
        generateContextTemplatesCombo = new ComboBox<>();
        generateContextTemplatesCombo.getItems().addAll(
                "Append to existing console",
                "Open in new console",
                "Copy to clipboard"
        );
        generateContextTemplatesCombo.setPrefWidth(220);
        styleComboBox(generateContextTemplatesCombo);
        generateContextTemplatesCombo.valueProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        HBox genTemplatesRow = new HBox(8, genTemplatesLabel, generateContextTemplatesCombo);
        genTemplatesRow.setAlignment(Pos.CENTER_LEFT);
        codeGenBox.getChildren().add(genTemplatesRow);

        // 5. Database Explorer
        VBox dbExplorerBox = new VBox(6);
        dbExplorerBox.getChildren().add(createSectionHeader("Database Explorer"));
        rememberFilterCheck = createCheckBox("Remember whether the filter is ON");
        dbExplorerBox.getChildren().add(rememberFilterCheck);

        // 6. Virtual Foreign Keys
        VBox vfkBox = new VBox(0);
        Label vfkHeader = createSectionHeader("Virtual Foreign Keys");
        vfkHeader.setPadding(new Insets(0, 0, 6, 0));

        HBox toolbar = new HBox(6);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(4, 6, 4, 6));
        toolbar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 1 1 0 1; -fx-border-radius: 4 4 0 0; -fx-background-radius: 4 4 0 0;");

        addVfkButton = createToolbarButton("+", "Add virtual foreign key pattern");
        removeVfkButton = createToolbarButton("-", "Remove selected pattern");
        testVfkButton = createToolbarButton("▶", "Evaluate virtual foreign key pattern");

        addVfkButton.setOnAction(e -> handleAddVfk());
        removeVfkButton.setOnAction(e -> handleRemoveVfk());

        toolbar.getChildren().addAll(addVfkButton, removeVfkButton, testVfkButton);

        vfkTable = new TableView<>();
        vfkTable.setEditable(true);
        vfkTable.setPrefHeight(150);
        vfkTable.setMaxWidth(700);
        vfkTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        vfkTable.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 1 1 1; -fx-border-radius: 0 0 4 4; -fx-background-radius: 0 0 4 4;");

        TableColumn<DatabaseVirtualForeignKey, String> colPatternCol = new TableColumn<>("Column pattern");
        colPatternCol.setPrefWidth(300);
        colPatternCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getColumnPattern()));
        colPatternCol.setCellFactory(TextFieldTableCell.forTableColumn());
        colPatternCol.setOnEditCommit(evt -> {
            evt.getRowValue().setColumnPattern(evt.getNewValue());
            syncTableToSettings();
            notifyModified();
        });

        TableColumn<DatabaseVirtualForeignKey, String> targetColPatternCol = new TableColumn<>("Target column pattern");
        targetColPatternCol.setPrefWidth(350);
        targetColPatternCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTargetColumnPattern()));
        targetColPatternCol.setCellFactory(TextFieldTableCell.forTableColumn());
        targetColPatternCol.setOnEditCommit(evt -> {
            evt.getRowValue().setTargetColumnPattern(evt.getNewValue());
            syncTableToSettings();
            notifyModified();
        });

        vfkTable.getColumns().addAll(colPatternCol, targetColPatternCol);

        vfkTableData = FXCollections.observableArrayList();
        vfkTable.setItems(vfkTableData);

        vfkTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> updateToolbarState());

        vfkBox.getChildren().addAll(vfkHeader, toolbar, vfkTable);
        vfkBox.setMaxWidth(700);

        // 7. SQL Resolution
        VBox sqlResolutionBox = new VBox(6);
        sqlResolutionBox.getChildren().add(createSectionHeader("SQL Resolution"));
        Label resolveLabel = createFieldLabel("Default resolve mode for consoles:");
        resolveLabel.setPrefWidth(250);
        defaultResolveModeCombo = new ComboBox<>();
        defaultResolveModeCombo.getItems().addAll("Playground", "Production", "Schema-only");
        defaultResolveModeCombo.setPrefWidth(160);
        styleComboBox(defaultResolveModeCombo);
        defaultResolveModeCombo.valueProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        HBox resolveRow = new HBox(8, resolveLabel, defaultResolveModeCombo);
        resolveRow.setAlignment(Pos.CENTER_LEFT);
        sqlResolutionBox.getChildren().add(resolveRow);

        // 8. Editor
        VBox editorBox = new VBox(6);
        editorBox.getChildren().add(createSectionHeader("Editor"));
        Label delimLabel = createFieldLabel("Statement delimiter:");
        delimLabel.setPrefWidth(160);
        statementDelimiterField = new TextField();
        statementDelimiterField.setPrefWidth(120);
        statementDelimiterField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 7 3 7;");
        statementDelimiterField.textProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        HBox delimRow = new HBox(8, delimLabel, statementDelimiterField);
        delimRow.setAlignment(Pos.CENTER_LEFT);
        editorBox.getChildren().add(delimRow);

        getChildren().addAll(
                modifyObjectBox,
                refactoringBox,
                ddlMappingsBox,
                codeGenBox,
                dbExplorerBox,
                vfkBox,
                sqlResolutionBox,
                editorBox
        );
    }

    private Label createSectionHeader(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
        return l;
    }

    private Label createFieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return l;
    }

    private CheckBox createCheckBox(String text) {
        CheckBox cb = new CheckBox(text);
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        cb.selectedProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
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

    private void styleComboBox(ComboBox<String> cb) {
        cb.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
    }

    private void updateToolbarState() {
        int idx = vfkTable.getSelectionModel().getSelectedIndex();
        removeVfkButton.setDisable(idx < 0);
    }

    private void handleAddVfk() {
        DatabaseVirtualForeignKey item = new DatabaseVirtualForeignKey("(.*)_(?i)id", "$1\\.(?i)id");
        vfkTableData.add(item);
        vfkTable.getSelectionModel().select(item);
        syncTableToSettings();
        notifyModified();
    }

    private void handleRemoveVfk() {
        int idx = vfkTable.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            vfkTableData.remove(idx);
            syncTableToSettings();
            notifyModified();
        }
    }

    private void syncTableToSettings() {
        if (currentSettings != null) {
            currentSettings.setVirtualForeignKeys(new ArrayList<>(vfkTableData));
        }
    }

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();
            currentSettings = initialSettings.clone();

            confirmCancellationCheck.setSelected(currentSettings.isConfirmCancellationForDialogsModifySchema());
            showPreviewValidScriptCheck.setSelected(currentSettings.isShowPreviewOfValidScript());
            suggestDumpingDdlCheck.setSelected(currentSettings.isSuggestDumpingDdl());
            generateContextTemplatesCombo.setValue(currentSettings.getGenerateContextTemplates());
            rememberFilterCheck.setSelected(currentSettings.isRememberWhetherFilterIsOn());

            vfkTableData.clear();
            for (DatabaseVirtualForeignKey k : currentSettings.getVirtualForeignKeys()) {
                vfkTableData.add(k.clone());
            }
            if (!vfkTableData.isEmpty()) {
                vfkTable.getSelectionModel().select(0);
            }
            updateToolbarState();

            defaultResolveModeCombo.setValue(currentSettings.getDefaultResolveModeForConsoles());
            statementDelimiterField.setText(currentSettings.getStatementDelimiter());
        } finally {
            updating = false;
        }
    }

    private DatabaseOtherSettings getCurrentSettingsFromUI() {
        DatabaseOtherSettings s = new DatabaseOtherSettings();
        s.setConfirmCancellationForDialogsModifySchema(confirmCancellationCheck.isSelected());
        s.setShowPreviewOfValidScript(showPreviewValidScriptCheck.isSelected());
        s.setSuggestDumpingDdl(suggestDumpingDdlCheck.isSelected());
        s.setGenerateContextTemplates(generateContextTemplatesCombo.getValue());
        s.setRememberWhetherFilterIsOn(rememberFilterCheck.isSelected());
        s.setVirtualForeignKeys(new ArrayList<>(vfkTableData));
        s.setDefaultResolveModeForConsoles(defaultResolveModeCombo.getValue());
        s.setStatementDelimiter(statementDelimiterField.getText());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        DatabaseOtherSettings updated = getCurrentSettingsFromUI();
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

    // Getters for testing
    public CheckBox getConfirmCancellationCheck() { return confirmCancellationCheck; }
    public CheckBox getShowPreviewValidScriptCheck() { return showPreviewValidScriptCheck; }
    public CheckBox getSuggestDumpingDdlCheck() { return suggestDumpingDdlCheck; }
    public ComboBox<String> getGenerateContextTemplatesCombo() { return generateContextTemplatesCombo; }
    public CheckBox getRememberFilterCheck() { return rememberFilterCheck; }
    public TableView<DatabaseVirtualForeignKey> getVfkTable() { return vfkTable; }
    public ComboBox<String> getDefaultResolveModeCombo() { return defaultResolveModeCombo; }
    public TextField getStatementDelimiterField() { return statementDelimiterField; }
    public Button getAddVfkButton() { return addVfkButton; }
    public Button getRemoveVfkButton() { return removeVfkButton; }
}
