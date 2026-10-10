package dev.lumina.ui;

import dev.lumina.database.versioning.DatabaseDiffChangesSettings;
import dev.lumina.database.versioning.DatabaseDiffChangesSettingsManager;
import dev.lumina.database.versioning.DiffChangeExcludedItem;
import dev.lumina.database.versioning.DiffChangeRule;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Tools > Database Versioning > Diff Changes settings page in Lumina IDE matching 1:1 design of the reference IDE.
 */
public class SettingsDatabaseVersioningDiffChangesPage extends VBox {

    private final DatabaseDiffChangesSettingsManager manager;
    private DatabaseDiffChangesSettings initialSettings;
    private DatabaseDiffChangesSettings currentSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    // Tree and Inspector
    private TreeView<Object> rulesTreeView;
    private ComboBox<String> locationCombo;
    private ComboBox<String> dangerLevelCombo;
    private TextField contextField;
    private TextField labelsField;
    private DiffChangeRule selectedRule;

    // Excluded changes table
    private TableView<DiffChangeExcludedItem> excludedTable;
    private ObservableList<DiffChangeExcludedItem> excludedData;
    private Button addExcludedBtn;
    private Button removeExcludedBtn;
    private Button upExcludedBtn;
    private Button downExcludedBtn;

    public SettingsDatabaseVersioningDiffChangesPage() {
        this.manager = DatabaseDiffChangesSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(12);
        setStyle("-fx-background-color: #1E1F22;");

        // 1. Description label
        Label descLabel = new Label(
                "Highlight level is used in the Liquibase Changelog or Flyway Versioned Migration\n" +
                "preview dialog and allows you to multiselect and deal easily with many similar change sets at once.\n" +
                "You can also completely disable change generation for some categories."
        );
        descLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-line-spacing: 2px;");

        // 2. Center split: Left tree, right inspector
        HBox splitBox = new HBox(16);
        VBox.setVgrow(splitBox, Priority.ALWAYS);

        // Left TreeView
        rulesTreeView = new TreeView<>();
        rulesTreeView.setShowRoot(false);
        rulesTreeView.setPrefWidth(420);
        rulesTreeView.setPrefHeight(270);
        rulesTreeView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        rulesTreeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else if (item instanceof String categoryName) {
                    setText(categoryName);
                    setGraphic(null);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-background-color: transparent;");
                } else if (item instanceof DiffChangeRule rule) {
                    setText(null);
                    HBox cellBox = new HBox(8);
                    cellBox.setAlignment(Pos.CENTER_LEFT);

                    Label actionLabel = new Label(rule.getAction());
                    String textFill = rule.getColor();
                    boolean strikethrough = "Disregard".equalsIgnoreCase(rule.getLocation()) || "Mark Ignored".equalsIgnoreCase(rule.getLocation());
                    actionLabel.setStyle("-fx-text-fill: " + textFill + ";" + (strikethrough ? " -fx-strikethrough: true;" : ""));

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label locLabel = new Label(rule.getLocation());
                    locLabel.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px;");

                    cellBox.getChildren().addAll(actionLabel, spacer, locLabel);
                    setGraphic(cellBox);
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });

        rulesTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() instanceof DiffChangeRule rule) {
                selectedRule = rule;
                updateInspectorFromRule(rule);
            } else {
                selectedRule = null;
            }
        });

        // Right Inspector Panel
        VBox inspectorPanel = new VBox(12);
        inspectorPanel.setPrefWidth(380);
        inspectorPanel.setPadding(new Insets(8, 12, 8, 12));

        // Location row
        Label locLabel = createFieldLabel("Location:");
        locLabel.setPrefWidth(90);
        locationCombo = new ComboBox<>();
        locationCombo.getItems().addAll("Primary", "Disregard", "Mark Ignored");
        locationCombo.setPrefWidth(220);
        styleComboBox(locationCombo);
        locationCombo.valueProperty().addListener((obs, o, n) -> {
            if (!updating && selectedRule != null && n != null) {
                selectedRule.setLocation(n);
                rulesTreeView.refresh();
                notifyModified();
            }
        });
        HBox locRow = new HBox(8, locLabel, locationCombo);
        locRow.setAlignment(Pos.CENTER_LEFT);

        // Danger level row
        Label dangerLabel = createFieldLabel("Danger level:");
        dangerLabel.setPrefWidth(90);
        dangerLevelCombo = new ComboBox<>();
        dangerLevelCombo.getItems().addAll("SAFE", "WARNING", "DANGER");
        dangerLevelCombo.setPrefWidth(220);
        styleComboBox(dangerLevelCombo);
        dangerLevelCombo.valueProperty().addListener((obs, o, n) -> {
            if (!updating && selectedRule != null && n != null) {
                selectedRule.setDangerLevel(n);
                notifyModified();
            }
        });
        HBox dangerRow = new HBox(8, dangerLabel, dangerLevelCombo);
        dangerRow.setAlignment(Pos.CENTER_LEFT);

        // Liquibase Section Header
        Label liquibaseHeader = new Label("Liquibase");
        liquibaseHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        // Context row
        Label contextLabel = createFieldLabel("Context:");
        contextLabel.setPrefWidth(90);
        contextField = createTextField(220);
        contextField.textProperty().addListener((obs, o, n) -> {
            if (!updating && selectedRule != null) {
                selectedRule.setContext(n);
                notifyModified();
            }
        });
        HBox contextRow = new HBox(8, contextLabel, contextField);
        contextRow.setAlignment(Pos.CENTER_LEFT);

        // Labels row
        Label labelsLabel = createFieldLabel("Labels:");
        labelsLabel.setPrefWidth(90);
        labelsField = createTextField(220);
        labelsField.textProperty().addListener((obs, o, n) -> {
            if (!updating && selectedRule != null) {
                selectedRule.setLabels(n);
                notifyModified();
            }
        });
        HBox labelsRow = new HBox(8, labelsLabel, labelsField);
        labelsRow.setAlignment(Pos.CENTER_LEFT);

        inspectorPanel.getChildren().addAll(locRow, dangerRow, liquibaseHeader, contextRow, labelsRow);

        splitBox.getChildren().addAll(rulesTreeView, inspectorPanel);

        // 3. Bottom Section: Exclude changes from liquibase/flyway diff generation
        Label bottomHeader = new Label("Exclude changes from liquibase/flyway diff generation");
        bottomHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        HBox toolbar = new HBox(6);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(4, 6, 4, 6));
        toolbar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 1 1 0 1; -fx-border-radius: 4 4 0 0; -fx-background-radius: 4 4 0 0;");

        addExcludedBtn = createToolbarButton("+", "Add excluded change");
        removeExcludedBtn = createToolbarButton("-", "Remove excluded change");
        upExcludedBtn = createToolbarButton("↑", "Move up");
        downExcludedBtn = createToolbarButton("↓", "Move down");

        addExcludedBtn.setOnAction(e -> handleAddExcluded());
        removeExcludedBtn.setOnAction(e -> handleRemoveExcluded());
        upExcludedBtn.setOnAction(e -> handleMoveUpExcluded());
        downExcludedBtn.setOnAction(e -> handleMoveDownExcluded());

        toolbar.getChildren().addAll(addExcludedBtn, removeExcludedBtn, upExcludedBtn, downExcludedBtn);

        excludedTable = new TableView<>();
        excludedTable.setEditable(true);
        excludedTable.setPrefHeight(130);
        excludedTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        excludedTable.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 1 1 1; -fx-border-radius: 0 0 4 4; -fx-background-radius: 0 0 4 4;");
        excludedTable.setPlaceholder(new Label("No excluded changes"));

        TableColumn<DiffChangeExcludedItem, String> tagCol = new TableColumn<>("Tag Name");
        tagCol.setPrefWidth(250);
        tagCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTagName()));
        tagCol.setCellFactory(TextFieldTableCell.forTableColumn());
        tagCol.setOnEditCommit(evt -> {
            evt.getRowValue().setTagName(evt.getNewValue());
            syncTableToSettings();
            notifyModified();
        });

        TableColumn<DiffChangeExcludedItem, String> targetCol = new TableColumn<>("Table/Column/Constraint/Index Name");
        targetCol.setPrefWidth(450);
        targetCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTargetName()));
        targetCol.setCellFactory(TextFieldTableCell.forTableColumn());
        targetCol.setOnEditCommit(evt -> {
            evt.getRowValue().setTargetName(evt.getNewValue());
            syncTableToSettings();
            notifyModified();
        });

        excludedTable.getColumns().addAll(tagCol, targetCol);

        excludedData = FXCollections.observableArrayList();
        excludedTable.setItems(excludedData);
        excludedTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> updateExcludedToolbarState());

        VBox tableContainer = new VBox(0, toolbar, excludedTable);

        getChildren().addAll(descLabel, splitBox, bottomHeader, tableContainer);
    }

    private Label createFieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return l;
    }

    private TextField createTextField(double width) {
        TextField tf = new TextField();
        tf.setPrefWidth(width);
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 7 3 7;");
        return tf;
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

    private void updateInspectorFromRule(DiffChangeRule rule) {
        updating = true;
        try {
            locationCombo.setValue(rule.getLocation());
            dangerLevelCombo.setValue(rule.getDangerLevel());
            contextField.setText(rule.getContext());
            labelsField.setText(rule.getLabels());
        } finally {
            updating = false;
        }
    }

    private void updateExcludedToolbarState() {
        int idx = excludedTable.getSelectionModel().getSelectedIndex();
        removeExcludedBtn.setDisable(idx < 0);
        upExcludedBtn.setDisable(idx <= 0);
        downExcludedBtn.setDisable(idx < 0 || idx >= excludedData.size() - 1);
    }

    private void handleAddExcluded() {
        DiffChangeExcludedItem item = new DiffChangeExcludedItem("column", "new_column");
        excludedData.add(item);
        excludedTable.getSelectionModel().select(item);
        syncTableToSettings();
        notifyModified();
    }

    private void handleRemoveExcluded() {
        int idx = excludedTable.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            excludedData.remove(idx);
            syncTableToSettings();
            notifyModified();
        }
    }

    private void handleMoveUpExcluded() {
        int idx = excludedTable.getSelectionModel().getSelectedIndex();
        if (idx > 0) {
            DiffChangeExcludedItem item = excludedData.remove(idx);
            excludedData.add(idx - 1, item);
            excludedTable.getSelectionModel().select(item);
            syncTableToSettings();
            notifyModified();
        }
    }

    private void handleMoveDownExcluded() {
        int idx = excludedTable.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < excludedData.size() - 1) {
            DiffChangeExcludedItem item = excludedData.remove(idx);
            excludedData.add(idx + 1, item);
            excludedTable.getSelectionModel().select(item);
            syncTableToSettings();
            notifyModified();
        }
    }

    private void syncTableToSettings() {
        if (currentSettings != null) {
            currentSettings.setExcludedChanges(new ArrayList<>(excludedData));
        }
    }

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();
            currentSettings = initialSettings.clone();

            // Populate Tree
            TreeItem<Object> root = new TreeItem<>("Root");
            String[] categories = {"Column", "Comment", "Foreign Key", "Index", "Primary Key", "Sequence", "Table", "Unique Constraint"};

            TreeItem<Object> firstLeaf = null;

            for (String cat : categories) {
                TreeItem<Object> catItem = new TreeItem<>(cat);
                catItem.setExpanded(true);
                for (DiffChangeRule r : currentSettings.getRules()) {
                    if (cat.equalsIgnoreCase(r.getCategory())) {
                        TreeItem<Object> ruleItem = new TreeItem<>(r);
                        catItem.getChildren().add(ruleItem);
                        if (firstLeaf == null) {
                            firstLeaf = ruleItem;
                        }
                    }
                }
                root.getChildren().add(catItem);
            }
            rulesTreeView.setRoot(root);

            if (firstLeaf != null) {
                rulesTreeView.getSelectionModel().select(firstLeaf);
                selectedRule = (DiffChangeRule) firstLeaf.getValue();
                updateInspectorFromRule(selectedRule);
            }

            // Excluded table
            excludedData.clear();
            for (DiffChangeExcludedItem it : currentSettings.getExcludedChanges()) {
                excludedData.add(it.clone());
            }
            updateExcludedToolbarState();
        } finally {
            updating = false;
        }
    }

    public boolean isModified() {
        if (initialSettings == null || currentSettings == null) return false;
        return !Objects.equals(initialSettings, currentSettings);
    }

    public void apply() {
        manager.setSettings(currentSettings);
        initialSettings = currentSettings.clone();
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
    public TreeView<Object> getRulesTreeView() { return rulesTreeView; }
    public ComboBox<String> getLocationCombo() { return locationCombo; }
    public ComboBox<String> getDangerLevelCombo() { return dangerLevelCombo; }
    public TextField getContextField() { return contextField; }
    public TextField getLabelsField() { return labelsField; }
    public TableView<DiffChangeExcludedItem> getExcludedTable() { return excludedTable; }
    public Button getAddExcludedBtn() { return addExcludedBtn; }
    public Button getRemoveExcludedBtn() { return removeExcludedBtn; }
    public DiffChangeRule getSelectedRule() { return selectedRule; }
}
