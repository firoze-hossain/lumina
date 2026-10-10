package dev.lumina.ui;

import dev.lumina.tools.ExternalToolItem;
import dev.lumina.tools.ExternalToolsSettings;
import dev.lumina.tools.ExternalToolsSettingsManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Tools > External Tools settings page in Lumina IDE matching 1:1 design of the reference IDE.
 */
public class SettingsToolsExternalToolsPage extends VBox {

    private final ExternalToolsSettingsManager manager;
    private ExternalToolsSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    private Button addBtn;
    private Button removeBtn;
    private Button editBtn;
    private Button upBtn;
    private Button downBtn;
    private Button duplicateBtn;

    private TableView<ExternalToolItem> toolsTable;
    private ObservableList<ExternalToolItem> toolsData;

    public SettingsToolsExternalToolsPage() {
        this.manager = ExternalToolsSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(10);
        setStyle("-fx-background-color: #1E1F22;");

        // Top toolbar
        HBox toolbar = new HBox(6);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        addBtn = createIconButton("+", "Add external tool");
        removeBtn = createIconButton("—", "Remove external tool");
        editBtn = createIconButton("✎", "Edit external tool");
        upBtn = createIconButton("▲", "Move tool up");
        downBtn = createIconButton("▼", "Move tool down");
        duplicateBtn = createIconButton("❐", "Duplicate external tool");

        removeBtn.setDisable(true);
        editBtn.setDisable(true);
        upBtn.setDisable(true);
        downBtn.setDisable(true);
        duplicateBtn.setDisable(true);

        addBtn.setOnAction(e -> handleAdd());
        removeBtn.setOnAction(e -> handleRemove());
        editBtn.setOnAction(e -> handleEdit());
        upBtn.setOnAction(e -> handleMoveUp());
        downBtn.setOnAction(e -> handleMoveDown());
        duplicateBtn.setOnAction(e -> handleDuplicate());

        toolbar.getChildren().addAll(addBtn, removeBtn, editBtn, upBtn, downBtn, duplicateBtn);

        // Tools table
        toolsTable = createTableView();
        VBox.setVgrow(toolsTable, Priority.ALWAYS);

        getChildren().addAll(toolbar, toolsTable);
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
    private TableView<ExternalToolItem> createTableView() {
        TableView<ExternalToolItem> tv = new TableView<>();
        tv.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ExternalToolItem, String> groupCol = new TableColumn<>("Group");
        groupCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getGroup()));
        groupCol.setPrefWidth(160);

        TableColumn<ExternalToolItem, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        nameCol.setPrefWidth(200);

        TableColumn<ExternalToolItem, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));
        descCol.setPrefWidth(240);

        TableColumn<ExternalToolItem, String> progCol = new TableColumn<>("Program");
        progCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProgram()));
        progCol.setPrefWidth(240);

        tv.getColumns().addAll(groupCol, nameCol, descCol, progCol);

        tv.getSelectionModel().selectedIndexProperty().addListener((obs, oldIdx, newIdx) -> {
            int idx = newIdx != null ? newIdx.intValue() : -1;
            boolean has = idx >= 0;
            removeBtn.setDisable(!has);
            editBtn.setDisable(!has);
            duplicateBtn.setDisable(!has);
            upBtn.setDisable(idx <= 0);
            downBtn.setDisable(idx < 0 || idx >= toolsData.size() - 1);
        });

        toolsData = FXCollections.observableArrayList();
        tv.setItems(toolsData);
        return tv;
    }

    private void handleAdd() {
        ExternalToolItem item = new ExternalToolItem("", "External Tools");
        boolean ok = showCreateEditDialog("Create Tool", item);
        if (ok && !item.getName().isBlank()) {
            toolsData.add(item);
            toolsTable.getSelectionModel().select(item);
            notifyModified();
        }
    }

    private void handleEdit() {
        ExternalToolItem sel = toolsTable.getSelectionModel().getSelectedItem();
        if (sel != null) {
            ExternalToolItem copy = sel.clone();
            boolean ok = showCreateEditDialog("Edit Tool", copy);
            if (ok) {
                int idx = toolsData.indexOf(sel);
                toolsData.set(idx, copy);
                toolsTable.getSelectionModel().select(copy);
                notifyModified();
            }
        }
    }

    private void handleRemove() {
        ExternalToolItem sel = toolsTable.getSelectionModel().getSelectedItem();
        if (sel != null) {
            toolsData.remove(sel);
            notifyModified();
        }
    }

    private void handleMoveUp() {
        int idx = toolsTable.getSelectionModel().getSelectedIndex();
        if (idx > 0) {
            ExternalToolItem it = toolsData.remove(idx);
            toolsData.add(idx - 1, it);
            toolsTable.getSelectionModel().select(it);
            notifyModified();
        }
    }

    private void handleMoveDown() {
        int idx = toolsTable.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < toolsData.size() - 1) {
            ExternalToolItem it = toolsData.remove(idx);
            toolsData.add(idx + 1, it);
            toolsTable.getSelectionModel().select(it);
            notifyModified();
        }
    }

    private void handleDuplicate() {
        ExternalToolItem sel = toolsTable.getSelectionModel().getSelectedItem();
        if (sel != null) {
            ExternalToolItem copy = sel.clone();
            copy.setName(copy.getName() + " (copy)");
            toolsData.add(copy);
            toolsTable.getSelectionModel().select(copy);
            notifyModified();
        }
    }

    public boolean showCreateEditDialog(String title, ExternalToolItem item) {
        Stage dlg = new Stage();
        dlg.setTitle(title);
        dlg.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(12);
        root.setPadding(new Insets(16, 20, 16, 20));
        root.setStyle("-fx-background-color: #2B2D30;");

        // Row 1: Name and Group
        Label nameLbl = new Label("Name:");
        nameLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        TextField nameField = new TextField(item.getName());
        nameField.setPrefWidth(220);
        styleTextField(nameField);

        Label grpLbl = new Label("Group:");
        grpLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        ComboBox<String> grpCombo = new ComboBox<>();
        grpCombo.setEditable(true);
        grpCombo.getItems().addAll("External Tools", "Build", "Deploy");
        grpCombo.setValue(item.getGroup());
        grpCombo.setPrefWidth(180);
        styleComboBox(grpCombo);

        HBox row1 = new HBox(10, nameLbl, nameField, grpLbl, grpCombo);
        row1.setAlignment(Pos.CENTER_LEFT);

        // Row 2: Description
        Label descLbl = new Label("Description:");
        descLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        descLbl.setPrefWidth(85);
        TextField descField = new TextField(item.getDescription());
        descField.setPrefWidth(430);
        styleTextField(descField);

        HBox row2 = new HBox(10, descLbl, descField);
        row2.setAlignment(Pos.CENTER_LEFT);

        // Section: Tool Settings
        Label toolSecLbl = new Label("Tool Settings");
        toolSecLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 6 0 2 0;");

        Label progLbl = new Label("Program:");
        progLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        progLbl.setPrefWidth(110);
        TextField progField = new TextField(item.getProgram());
        progField.setPrefWidth(350);
        styleTextField(progField);
        Button progMacro = createIconButton("+", "Insert macro");
        Button progBrowse = createIconButton("🗀", "Browse");
        HBox progRow = new HBox(8, progLbl, progField, progMacro, progBrowse);
        progRow.setAlignment(Pos.CENTER_LEFT);

        Label argsLbl = new Label("Arguments:");
        argsLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        argsLbl.setPrefWidth(110);
        TextField argsField = new TextField(item.getArguments());
        argsField.setPrefWidth(350);
        styleTextField(argsField);
        Button argsMacro = createIconButton("+", "Insert macro");
        Button argsExpand = createIconButton("⤢", "Expand editor");
        HBox argsRow = new HBox(8, argsLbl, argsField, argsMacro, argsExpand);
        argsRow.setAlignment(Pos.CENTER_LEFT);

        Label dirLbl = new Label("Working directory:");
        dirLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        dirLbl.setPrefWidth(110);
        TextField dirField = new TextField(item.getWorkingDirectory());
        dirField.setPrefWidth(350);
        styleTextField(dirField);
        Button dirMacro = createIconButton("+", "Insert macro");
        Button dirBrowse = createIconButton("🗀", "Browse");
        HBox dirRow = new HBox(8, dirLbl, dirField, dirMacro, dirBrowse);
        dirRow.setAlignment(Pos.CENTER_LEFT);

        // Section: Advanced Options
        TitledPane advPane = new TitledPane();
        advPane.setText("Advanced Options");
        advPane.setExpanded(true);
        advPane.setStyle("-fx-text-fill: #DFE1E5;");

        VBox advContent = new VBox(8);
        advContent.setStyle("-fx-background-color: #2B2D30; -fx-padding: 6 0 6 0;");

        CheckBox syncCheck = new CheckBox("Synchronize files after execution");
        syncCheck.setSelected(item.isSynchronizeFiles());
        syncCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        CheckBox consoleCheck = new CheckBox("Open console for tool output");
        consoleCheck.setSelected(item.isOpenConsole());
        consoleCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        CheckBox stdoutCheck = new CheckBox("Make console active on message in stdout");
        stdoutCheck.setSelected(item.isMakeActiveOnStdout());
        stdoutCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 0 0 0 20;");

        CheckBox stderrCheck = new CheckBox("Make console active on message in stderr");
        stderrCheck.setSelected(item.isMakeActiveOnStderr());
        stderrCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 0 0 0 20;");

        Label filterLbl = new Label("Output filters:");
        filterLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        filterLbl.setPrefWidth(100);
        TextField filterField = new TextField(item.getOutputFilters());
        filterField.setPrefWidth(360);
        styleTextField(filterField);
        HBox filterRow = new HBox(8, filterLbl, filterField);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        Label filterSubtext = new Label("Each line is a regex, available macros: $FILE_PATH$, $LINE$ and $COLUMN$");
        filterSubtext.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px; -fx-padding: 0 0 0 108;");

        advContent.getChildren().addAll(syncCheck, consoleCheck, stdoutCheck, stderrCheck, filterRow, filterSubtext);
        advPane.setContent(advContent);

        // Buttons
        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 14 5 14; -fx-cursor: hand;");

        Button okBtn = new Button("OK");
        okBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 18 5 18; -fx-cursor: hand; -fx-font-weight: bold;");

        boolean[] result = new boolean[1];

        cancelBtn.setOnAction(e -> dlg.close());
        okBtn.setOnAction(e -> {
            item.setName(nameField.getText().trim());
            item.setGroup(grpCombo.getValue() != null ? grpCombo.getValue().trim() : "External Tools");
            item.setDescription(descField.getText().trim());
            item.setProgram(progField.getText().trim());
            item.setArguments(argsField.getText().trim());
            item.setWorkingDirectory(dirField.getText().trim());
            item.setSynchronizeFiles(syncCheck.isSelected());
            item.setOpenConsole(consoleCheck.isSelected());
            item.setMakeActiveOnStdout(stdoutCheck.isSelected());
            item.setMakeActiveOnStderr(stderrCheck.isSelected());
            item.setOutputFilters(filterField.getText().trim());
            result[0] = true;
            dlg.close();
        });

        btnRow.getChildren().addAll(cancelBtn, okBtn);

        root.getChildren().addAll(row1, row2, toolSecLbl, progRow, argsRow, dirRow, advPane, btnRow);

        Scene scene = new Scene(root, 560, 480);
        dlg.setScene(scene);
        dlg.showAndWait();

        return result[0];
    }

    private void styleTextField(TextField tf) {
        tf.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8 4 8;");
    }

    private void styleComboBox(ComboBox<String> cb) {
        cb.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
    }

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();
            toolsData.clear();
            for (ExternalToolItem item : initialSettings.getTools()) {
                toolsData.add(item.clone());
            }
        } finally {
            updating = false;
        }
    }

    private ExternalToolsSettings getCurrentSettingsFromUI() {
        ExternalToolsSettings s = new ExternalToolsSettings();
        List<ExternalToolItem> list = new ArrayList<>();
        for (ExternalToolItem item : toolsData) {
            list.add(item.clone());
        }
        s.setTools(list);
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        ExternalToolsSettings updated = getCurrentSettingsFromUI();
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
    public TableView<ExternalToolItem> getToolsTable() { return toolsTable; }
    public Button getAddBtn() { return addBtn; }
    public Button getRemoveBtn() { return removeBtn; }
    public Button getEditBtn() { return editBtn; }
    public Button getDuplicateBtn() { return duplicateBtn; }
}
