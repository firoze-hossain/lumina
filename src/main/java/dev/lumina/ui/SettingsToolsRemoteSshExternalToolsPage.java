package dev.lumina.ui;

import dev.lumina.tools.RemoteSshExternalToolEntry;
import dev.lumina.tools.RemoteSshExternalToolsSettings;
import dev.lumina.tools.RemoteSshExternalToolsSettingsManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Tools > Remote SSH External Tools settings page in Lumina IDE.
 * Matches screenshot 5 1:1 with dynamic configuration and Create/Edit Tool dialog.
 */
public class SettingsToolsRemoteSshExternalToolsPage extends VBox {

    private final RemoteSshExternalToolsSettingsManager manager;
    private RemoteSshExternalToolsSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    private final ObservableList<RemoteSshExternalToolEntry> tableData = FXCollections.observableArrayList();
    private TableView<RemoteSshExternalToolEntry> tableView;

    private Button addButton;
    private Button removeButton;
    private Button editButton;
    private Button upButton;
    private Button downButton;
    private Button duplicateButton;

    public SettingsToolsRemoteSshExternalToolsPage() {
        this.manager = RemoteSshExternalToolsSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(10);
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        // Toolbar
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #2B2D30; -fx-padding: 4 6; -fx-border-color: #393B40; -fx-border-width: 1 1 0 1;");

        addButton = createToolbarButton("+", "Add Remote SSH Tool");
        removeButton = createToolbarButton("−", "Remove Selected Tool");
        editButton = createToolbarButton("✏", "Edit Selected Tool");
        upButton = createToolbarButton("▲", "Move Up");
        downButton = createToolbarButton("▼", "Move Down");
        duplicateButton = createToolbarButton("📋", "Duplicate Tool");

        toolbar.getChildren().addAll(addButton, removeButton, editButton, upButton, downButton, duplicateButton);

        // Table
        tableView = new TableView<>(tableData);
        tableView.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 1; -fx-text-fill: #DFE1E5;");
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        TableColumn<RemoteSshExternalToolEntry, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(160);

        TableColumn<RemoteSshExternalToolEntry, String> groupCol = new TableColumn<>("Group");
        groupCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getGroup()));
        groupCol.setPrefWidth(140);

        TableColumn<RemoteSshExternalToolEntry, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        descCol.setPrefWidth(300);

        tableView.getColumns().addAll(nameCol, groupCol, descCol);

        tableView.setRowFactory(tv -> {
            TableRow<RemoteSshExternalToolEntry> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showEditDialog(row.getItem());
                }
            });
            return row;
        });

        getChildren().addAll(toolbar, tableView);

        setupListeners();
    }

    private void setupListeners() {
        addButton.setOnAction(e -> showAddDialog());
        removeButton.setOnAction(e -> {
            RemoteSshExternalToolEntry sel = tableView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                tableData.remove(sel);
                notifyModified();
            }
        });
        editButton.setOnAction(e -> {
            RemoteSshExternalToolEntry sel = tableView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                showEditDialog(sel);
            }
        });
        upButton.setOnAction(e -> {
            int idx = tableView.getSelectionModel().getSelectedIndex();
            if (idx > 0) {
                RemoteSshExternalToolEntry item = tableData.remove(idx);
                tableData.add(idx - 1, item);
                tableView.getSelectionModel().select(idx - 1);
                notifyModified();
            }
        });
        downButton.setOnAction(e -> {
            int idx = tableView.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < tableData.size() - 1) {
                RemoteSshExternalToolEntry item = tableData.remove(idx);
                tableData.add(idx + 1, item);
                tableView.getSelectionModel().select(idx + 1);
                notifyModified();
            }
        });
        duplicateButton.setOnAction(e -> {
            RemoteSshExternalToolEntry sel = tableView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                RemoteSshExternalToolEntry copy = sel.clone();
                copy.setName(copy.getName() + " (copy)");
                tableData.add(copy);
                notifyModified();
            }
        });

        tableData.addListener((javafx.collections.ListChangeListener<RemoteSshExternalToolEntry>) c -> notifyModified());
    }

    private void showAddDialog() {
        RemoteSshExternalToolEntry newEntry = new RemoteSshExternalToolEntry("", "Remote Tools", "");
        openToolDialog("Create Tool", newEntry, true);
    }

    private void showEditDialog(RemoteSshExternalToolEntry entry) {
        openToolDialog("Edit Tool", entry, false);
    }

    private void openToolDialog(String title, RemoteSshExternalToolEntry entry, boolean isNew) {
        Dialog<RemoteSshExternalToolEntry> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);

        VBox rootBox = new VBox(12);
        rootBox.setPadding(new Insets(16, 20, 16, 20));
        rootBox.setStyle("-fx-background-color: #1E1F22;");
        rootBox.setPrefWidth(540);

        // Row 1: Name and Group
        Label nameLabel = new Label("Name:");
        nameLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        TextField nameField = new TextField(entry.getName());
        nameField.setPrefWidth(220);
        styleControl(nameField);

        Label groupLabel = new Label("Group:");
        groupLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        ComboBox<String> groupCombo = new ComboBox<>();
        groupCombo.getItems().addAll("Remote Tools", "Build", "Deploy", "Custom");
        groupCombo.setValue(entry.getGroup());
        groupCombo.setEditable(true);
        groupCombo.setPrefWidth(150);
        styleControl(groupCombo);

        HBox nameGroupRow = new HBox(10, nameLabel, nameField, groupLabel, groupCombo);
        nameGroupRow.setAlignment(Pos.CENTER_LEFT);

        // Row 2: Description
        Label descLabel = new Label("Description:");
        descLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        TextField descField = new TextField(entry.getDescription());
        descField.setPrefWidth(420);
        styleControl(descField);

        HBox descRow = new HBox(10, descLabel, descField);
        descRow.setAlignment(Pos.CENTER_LEFT);

        // Tool Settings Divider
        HBox toolSettingsHeader = createDividerHeader("Tool Settings");

        GridPane toolGrid = new GridPane();
        toolGrid.setHgap(10);
        toolGrid.setVgap(8);

        // Program
        Label progLabel = new Label("Program:");
        progLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        TextField progField = new TextField(entry.getProgram());
        progField.setPrefWidth(340);
        styleControl(progField);
        Button progMacroBtn = createMiniButton("+", "Insert Macro");
        Button progBrowseBtn = createMiniButton("📁", "Browse Program");
        progBrowseBtn.setOnAction(e -> chooseFile("Select Program", progField));
        HBox progBox = new HBox(4, progField, progMacroBtn, progBrowseBtn);

        toolGrid.add(progLabel, 0, 0);
        toolGrid.add(progBox, 1, 0);

        // Arguments
        Label argsLabel = new Label("Arguments:");
        argsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        TextField argsField = new TextField(entry.getArguments());
        argsField.setPrefWidth(340);
        styleControl(argsField);
        Button argsMacroBtn = createMiniButton("+", "Insert Macro");
        Button argsExpandBtn = createMiniButton("⤢", "Expand Arguments");
        argsExpandBtn.setOnAction(e -> openTextEditDialog("Arguments", argsField));
        HBox argsBox = new HBox(4, argsField, argsMacroBtn, argsExpandBtn);

        toolGrid.add(argsLabel, 0, 1);
        toolGrid.add(argsBox, 1, 1);

        // Working directory
        Label wdLabel = new Label("Working directory:");
        wdLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        TextField wdField = new TextField(entry.getWorkingDirectory());
        wdField.setPrefWidth(340);
        styleControl(wdField);
        Button wdMacroBtn = createMiniButton("+", "Insert Macro");
        Button wdBrowseBtn = createMiniButton("📁", "Browse Directory");
        wdBrowseBtn.setOnAction(e -> chooseFile("Select Working Directory", wdField));
        HBox wdBox = new HBox(4, wdField, wdMacroBtn, wdBrowseBtn);

        toolGrid.add(wdLabel, 0, 2);
        toolGrid.add(wdBox, 1, 2);

        // Connection settings
        Label connLabel = new Label("Connection settings");
        connLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        ToggleGroup connGroup = new ToggleGroup();
        RadioButton vagrantRadio = new RadioButton("Current Vagrant");
        vagrantRadio.setToggleGroup(connGroup);
        vagrantRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        RadioButton pythonRadio = new RadioButton("Default Python Remote Interpreter");
        pythonRadio.setToggleGroup(connGroup);
        pythonRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        RadioButton sshRadio = new RadioButton("SSH configuration");
        sshRadio.setToggleGroup(connGroup);
        sshRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        ComboBox<String> sshCombo = new ComboBox<>();
        sshCombo.getItems().addAll("Select SSH configuration on every run", "production-server", "staging-server", "dev-vm");
        sshCombo.setValue(entry.getSshConfiguration());
        sshCombo.setPrefWidth(240);
        styleControl(sshCombo);

        HBox sshRow = new HBox(8, sshRadio, sshCombo);
        sshRow.setAlignment(Pos.CENTER_LEFT);

        if (RemoteSshExternalToolEntry.CONN_CURRENT_VAGRANT.equals(entry.getConnectionType())) {
            vagrantRadio.setSelected(true);
        } else if (RemoteSshExternalToolEntry.CONN_DEFAULT_PYTHON_REMOTE.equals(entry.getConnectionType())) {
            pythonRadio.setSelected(true);
        } else {
            sshRadio.setSelected(true);
        }

        VBox connBox = new VBox(6, connLabel, vagrantRadio, pythonRadio, sshRow);
        connBox.setPadding(new Insets(4, 0, 0, 16));

        // Advanced Options
        HBox advancedHeader = createDividerHeader("Advanced Options");

        CheckBox syncFilesCheck = new CheckBox("Synchronize files after execution");
        syncFilesCheck.setSelected(entry.isSynchronizeFilesAfterExecution());
        syncFilesCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        CheckBox openConsoleCheck = new CheckBox("Open console for tool output");
        openConsoleCheck.setSelected(entry.isOpenConsoleForToolOutput());
        openConsoleCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        CheckBox stdoutCheck = new CheckBox("Make console active on message in stdout");
        stdoutCheck.setSelected(entry.isMakeConsoleActiveOnStdout());
        stdoutCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        CheckBox stderrCheck = new CheckBox("Make console active on message in stderr");
        stderrCheck.setSelected(entry.isMakeConsoleActiveOnStderr());
        stderrCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        VBox consoleSubBox = new VBox(6, stdoutCheck, stderrCheck);
        consoleSubBox.setPadding(new Insets(2, 0, 0, 20));
        consoleSubBox.disableProperty().bind(openConsoleCheck.selectedProperty().not());

        Label filtersLabel = new Label("Output filters:");
        filtersLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        TextField filtersField = new TextField(entry.getOutputFilters());
        filtersField.setPrefWidth(350);
        styleControl(filtersField);
        Button filtersExpandBtn = createMiniButton("⤢", "Expand Filters");
        filtersExpandBtn.setOnAction(e -> openTextEditDialog("Output filters", filtersField));
        HBox filtersBox = new HBox(4, filtersField, filtersExpandBtn);

        Label filtersHint = new Label("Each line is a regex, available macros: $FILE_PATH$, $LINE$ and $COLUMN$");
        filtersHint.setStyle("-fx-text-fill: #7A7E85; -fx-font-size: 11px;");

        HBox filtersRow = new HBox(8, filtersLabel, filtersBox);
        filtersRow.setAlignment(Pos.CENTER_LEFT);

        VBox advancedBox = new VBox(8, syncFilesCheck, openConsoleCheck, consoleSubBox, filtersRow, filtersHint);
        advancedBox.setPadding(new Insets(4, 0, 0, 16));

        rootBox.getChildren().addAll(
                nameGroupRow, descRow,
                toolSettingsHeader, toolGrid, connBox,
                advancedHeader, advancedBox
        );

        ScrollPane scroll = new ScrollPane(rootBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #1E1F22; -fx-background-color: #1E1F22; -fx-border-color: transparent;");
        scroll.setPrefHeight(520);

        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                RemoteSshExternalToolEntry res = new RemoteSshExternalToolEntry();
                res.setName(nameField.getText().trim());
                res.setGroup(groupCombo.getValue() != null ? groupCombo.getValue().trim() : "Remote Tools");
                res.setDescription(descField.getText().trim());
                res.setProgram(progField.getText().trim());
                res.setArguments(argsField.getText().trim());
                res.setWorkingDirectory(wdField.getText().trim());
                if (vagrantRadio.isSelected()) {
                    res.setConnectionType(RemoteSshExternalToolEntry.CONN_CURRENT_VAGRANT);
                } else if (pythonRadio.isSelected()) {
                    res.setConnectionType(RemoteSshExternalToolEntry.CONN_DEFAULT_PYTHON_REMOTE);
                } else {
                    res.setConnectionType(RemoteSshExternalToolEntry.CONN_SSH_CONFIG);
                }
                res.setSshConfiguration(sshCombo.getValue() != null ? sshCombo.getValue() : "Select SSH configuration on every run");
                res.setSynchronizeFilesAfterExecution(syncFilesCheck.isSelected());
                res.setOpenConsoleForToolOutput(openConsoleCheck.isSelected());
                res.setMakeConsoleActiveOnStdout(stdoutCheck.isSelected());
                res.setMakeConsoleActiveOnStderr(stderrCheck.isSelected());
                res.setOutputFilters(filtersField.getText().trim());
                return res;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(res -> {
            if (isNew) {
                tableData.add(res);
            } else {
                int idx = tableData.indexOf(entry);
                if (idx != -1) {
                    tableData.set(idx, res);
                }
            }
            notifyModified();
        });
    }

    private void chooseFile(String title, TextField targetField) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            targetField.setText(file.getAbsolutePath());
        }
    }

    private void openTextEditDialog(String title, TextField targetField) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("Edit " + title);

        TextArea textArea = new TextArea(targetField.getText());
        textArea.setPrefRowCount(8);
        textArea.setPrefColumnCount(40);
        textArea.setStyle("-fx-control-inner-background: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-family: monospace;");

        dialog.getDialogPane().setContent(new VBox(10, textArea));
        dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
        dialog.setResultConverter(btn -> btn == javafx.scene.control.ButtonType.OK ? textArea.getText().trim() : null);

        dialog.showAndWait().ifPresent(targetField::setText);
    }

    private Button createToolbarButton(String text, String tooltipText) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-min-width: 24px; -fx-min-height: 24px; -fx-padding: 2 6;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-cursor: hand; -fx-min-width: 24px; -fx-min-height: 24px; -fx-padding: 2 6;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-min-width: 24px; -fx-min-height: 24px; -fx-padding: 2 6;"));
        if (tooltipText != null) {
            btn.setTooltip(new Tooltip(tooltipText));
        }
        return btn;
    }

    private Button createMiniButton(String text, String tooltipText) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand; -fx-min-width: 24px; -fx-min-height: 24px;");
        if (tooltipText != null) {
            btn.setTooltip(new Tooltip(tooltipText));
        }
        return btn;
    }

    private HBox createDividerHeader(String text) {
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(6, 0, 4, 0));

        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #393B40; -fx-opacity: 0.5;");
        HBox.setHgrow(sep, Priority.ALWAYS);

        header.getChildren().addAll(label, sep);
        return header;
    }

    private void styleControl(javafx.scene.control.Control control) {
        control.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4;");
    }

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();
            applySettingsToUI(initialSettings);
        } finally {
            updating = false;
        }
    }

    private void applySettingsToUI(RemoteSshExternalToolsSettings s) {
        if (s == null) return;
        tableData.clear();
        for (RemoteSshExternalToolEntry e : s.getTools()) {
            tableData.add(e.clone());
        }
    }

    private RemoteSshExternalToolsSettings getCurrentSettingsFromUI() {
        RemoteSshExternalToolsSettings s = new RemoteSshExternalToolsSettings();
        List<RemoteSshExternalToolEntry> list = new ArrayList<>();
        for (RemoteSshExternalToolEntry e : tableData) {
            list.add(e.clone());
        }
        s.setTools(list);
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        RemoteSshExternalToolsSettings current = getCurrentSettingsFromUI();
        return !Objects.equals(initialSettings, current);
    }

    public void apply() {
        RemoteSshExternalToolsSettings current = getCurrentSettingsFromUI();
        manager.setSettings(current);
        initialSettings = current.clone();
        notifyModified();
    }

    public void revertChanges() {
        if (initialSettings != null) {
            updating = true;
            try {
                applySettingsToUI(initialSettings);
            } finally {
                updating = false;
            }
            notifyModified();
        }
    }

    public void reset() {
        revertChanges();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public TableView<RemoteSshExternalToolEntry> getTableView() {
        return tableView;
    }

    public ObservableList<RemoteSshExternalToolEntry> getTableData() {
        return tableData;
    }

    public Button getAddButton() {
        return addButton;
    }

    public Button getRemoveButton() {
        return removeButton;
    }

    public Button getEditButton() {
        return editButton;
    }

    public Button getUpButton() {
        return upButton;
    }

    public Button getDownButton() {
        return downButton;
    }

    public Button getDuplicateButton() {
        return duplicateButton;
    }
}
