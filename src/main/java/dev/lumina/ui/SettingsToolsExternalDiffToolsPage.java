package dev.lumina.ui;

import dev.lumina.tools.ExternalDiffToolAssociation;
import dev.lumina.tools.ExternalDiffToolDefinition;
import dev.lumina.tools.ExternalDiffToolsSettings;
import dev.lumina.tools.ExternalDiffToolsSettingsManager;
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
 * Tools > Diff & Merge > External Diff Tools settings page in Lumina IDE matching 1:1 design of the reference IDE.
 */
public class SettingsToolsExternalDiffToolsPage extends VBox {

    private final ExternalDiffToolsSettingsManager manager;
    private ExternalDiffToolsSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    private CheckBox enableExternalToolsCheck;

    private VBox section1Box;
    private TableView<ExternalDiffToolDefinition> toolsTable;
    private ObservableList<ExternalDiffToolDefinition> toolsData;
    private Button addToolBtn;
    private Button removeToolBtn;
    private Button editToolBtn;

    private VBox section2Box;
    private TableView<ExternalDiffToolAssociation> associationsTable;
    private ObservableList<ExternalDiffToolAssociation> associationsData;
    private Button addAssocBtn;
    private Button removeAssocBtn;

    public SettingsToolsExternalDiffToolsPage() {
        this.manager = ExternalDiffToolsSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");

        // Top enable checkbox
        enableExternalToolsCheck = new CheckBox("Enable external tools");
        enableExternalToolsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        enableExternalToolsCheck.selectedProperty().addListener((obs, o, n) -> {
            boolean en = n != null && n;
            section1Box.setDisable(!en);
            section2Box.setDisable(!en);
            if (!updating) notifyModified();
        });

        // --- Section 1: Configure external tools ---
        section1Box = new VBox(6);
        Label sec1Label = new Label("Configure external tools:");
        sec1Label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox sec1Toolbar = new HBox(6);
        sec1Toolbar.setAlignment(Pos.CENTER_LEFT);
        addToolBtn = createIconButton("+", "Add external tool");
        removeToolBtn = createIconButton("—", "Remove external tool");
        editToolBtn = createIconButton("✎", "Edit external tool");
        removeToolBtn.setDisable(true);
        editToolBtn.setDisable(true);

        addToolBtn.setOnAction(e -> handleAddTool());
        removeToolBtn.setOnAction(e -> handleRemoveTool());
        editToolBtn.setOnAction(e -> handleEditTool());

        sec1Toolbar.getChildren().addAll(addToolBtn, removeToolBtn, editToolBtn);

        toolsTable = createToolsTable();
        toolsTable.setPrefHeight(160);

        section1Box.getChildren().addAll(sec1Label, sec1Toolbar, toolsTable);

        // --- Section 2: Configure external diff/merge tools associated with a file type ---
        section2Box = new VBox(6);
        Label sec2Label = new Label("Configure external diff/merge tools associated with a file type:");
        sec2Label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox sec2Toolbar = new HBox(6);
        sec2Toolbar.setAlignment(Pos.CENTER_LEFT);
        addAssocBtn = createIconButton("+", "Add association");
        removeAssocBtn = createIconButton("—", "Remove association");
        removeAssocBtn.setDisable(true);

        addAssocBtn.setOnAction(e -> handleAddAssoc());
        removeAssocBtn.setOnAction(e -> handleRemoveAssoc());

        sec2Toolbar.getChildren().addAll(addAssocBtn, removeAssocBtn);

        associationsTable = createAssociationsTable();
        associationsTable.setPrefHeight(160);

        section2Box.getChildren().addAll(sec2Label, sec2Toolbar, associationsTable);

        // Initial disable bindings
        section1Box.setDisable(true);
        section2Box.setDisable(true);

        getChildren().addAll(enableExternalToolsCheck, section1Box, section2Box);
    }

    private Button createIconButton(String icon, String tooltip) {
        Button btn = new Button(icon);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 3 8 3 8; -fx-border-color: #393B40; -fx-border-radius: 3; -fx-background-radius: 3;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 3 8 3 8; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-background-radius: 3;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 3 8 3 8; -fx-border-color: #393B40; -fx-border-radius: 3; -fx-background-radius: 3;"));
        btn.setTooltip(new Tooltip(tooltip));
        return btn;
    }

    @SuppressWarnings("unchecked")
    private TableView<ExternalDiffToolDefinition> createToolsTable() {
        TableView<ExternalDiffToolDefinition> tv = new TableView<>();
        tv.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Label placeholder = new Label("Nothing to show");
        placeholder.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 13px;");
        tv.setPlaceholder(placeholder);

        TableColumn<ExternalDiffToolDefinition, String> nameCol = new TableColumn<>("Tool Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        nameCol.setPrefWidth(200);

        TableColumn<ExternalDiffToolDefinition, String> pathCol = new TableColumn<>("Program Path");
        pathCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProgramPath()));
        pathCol.setPrefWidth(350);

        tv.getColumns().addAll(nameCol, pathCol);

        tv.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            boolean has = n != null;
            removeToolBtn.setDisable(!has);
            editToolBtn.setDisable(!has);
        });

        toolsData = FXCollections.observableArrayList();
        tv.setItems(toolsData);
        return tv;
    }

    @SuppressWarnings("unchecked")
    private TableView<ExternalDiffToolAssociation> createAssociationsTable() {
        TableView<ExternalDiffToolAssociation> tv = new TableView<>();
        tv.setEditable(true);
        tv.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ExternalDiffToolAssociation, String> fTypeCol = new TableColumn<>("File Type");
        fTypeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFileType()));
        fTypeCol.setCellFactory(TextFieldTableCell.forTableColumn());
        fTypeCol.setOnEditCommit(evt -> {
            evt.getRowValue().setFileType(evt.getNewValue());
            notifyModified();
        });
        fTypeCol.setPrefWidth(250);

        TableColumn<ExternalDiffToolAssociation, String> diffCol = new TableColumn<>("Diff Tool");
        diffCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDiffTool()));
        diffCol.setCellFactory(TextFieldTableCell.forTableColumn());
        diffCol.setOnEditCommit(evt -> {
            evt.getRowValue().setDiffTool(evt.getNewValue());
            notifyModified();
        });
        diffCol.setPrefWidth(250);

        TableColumn<ExternalDiffToolAssociation, String> mergeCol = new TableColumn<>("Merge Tool");
        mergeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMergeTool()));
        mergeCol.setCellFactory(TextFieldTableCell.forTableColumn());
        mergeCol.setOnEditCommit(evt -> {
            evt.getRowValue().setMergeTool(evt.getNewValue());
            notifyModified();
        });
        mergeCol.setPrefWidth(250);

        tv.getColumns().addAll(fTypeCol, diffCol, mergeCol);

        tv.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            boolean has = n != null && !"Default".equalsIgnoreCase(n.getFileType());
            removeAssocBtn.setDisable(!has);
        });

        associationsData = FXCollections.observableArrayList();
        tv.setItems(associationsData);
        return tv;
    }

    private void handleAddTool() {
        ExternalDiffToolDefinition tool = new ExternalDiffToolDefinition("New Tool", "/usr/local/bin/diff", "");
        toolsData.add(tool);
        toolsTable.getSelectionModel().select(tool);
        notifyModified();
    }

    private void handleRemoveTool() {
        ExternalDiffToolDefinition sel = toolsTable.getSelectionModel().getSelectedItem();
        if (sel != null) {
            toolsData.remove(sel);
            notifyModified();
        }
    }

    private void handleEditTool() {
        ExternalDiffToolDefinition sel = toolsTable.getSelectionModel().getSelectedItem();
        if (sel != null) {
            sel.setName(sel.getName() + " (edited)");
            toolsTable.refresh();
            notifyModified();
        }
    }

    private void handleAddAssoc() {
        ExternalDiffToolAssociation assoc = new ExternalDiffToolAssociation("*.txt", "Built-in", "Built-in");
        associationsData.add(assoc);
        associationsTable.getSelectionModel().select(assoc);
        notifyModified();
    }

    private void handleRemoveAssoc() {
        ExternalDiffToolAssociation sel = associationsTable.getSelectionModel().getSelectedItem();
        if (sel != null && !"Default".equalsIgnoreCase(sel.getFileType())) {
            associationsData.remove(sel);
            notifyModified();
        }
    }

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();

            enableExternalToolsCheck.setSelected(initialSettings.isEnableExternalTools());
            section1Box.setDisable(!initialSettings.isEnableExternalTools());
            section2Box.setDisable(!initialSettings.isEnableExternalTools());

            toolsData.clear();
            for (ExternalDiffToolDefinition d : initialSettings.getConfiguredTools()) {
                toolsData.add(d.clone());
            }

            associationsData.clear();
            for (ExternalDiffToolAssociation a : initialSettings.getAssociations()) {
                associationsData.add(a.clone());
            }
        } finally {
            updating = false;
        }
    }

    private ExternalDiffToolsSettings getCurrentSettingsFromUI() {
        ExternalDiffToolsSettings s = new ExternalDiffToolsSettings();
        s.setEnableExternalTools(enableExternalToolsCheck.isSelected());

        List<ExternalDiffToolDefinition> tools = new ArrayList<>();
        for (ExternalDiffToolDefinition d : toolsData) {
            tools.add(d.clone());
        }
        s.setConfiguredTools(tools);

        List<ExternalDiffToolAssociation> assocs = new ArrayList<>();
        for (ExternalDiffToolAssociation a : associationsData) {
            assocs.add(a.clone());
        }
        s.setAssociations(assocs);

        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        ExternalDiffToolsSettings updated = getCurrentSettingsFromUI();
        manager.setSettings(updated);
        initialSettings = updated.clone();
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
    public CheckBox getEnableExternalToolsCheck() { return enableExternalToolsCheck; }
    public TableView<ExternalDiffToolDefinition> getToolsTable() { return toolsTable; }
    public TableView<ExternalDiffToolAssociation> getAssociationsTable() { return associationsTable; }
    public Button getAddToolBtn() { return addToolBtn; }
    public Button getRemoveToolBtn() { return removeToolBtn; }
    public Button getAddAssocBtn() { return addAssocBtn; }
    public Button getRemoveAssocBtn() { return removeAssocBtn; }
}
