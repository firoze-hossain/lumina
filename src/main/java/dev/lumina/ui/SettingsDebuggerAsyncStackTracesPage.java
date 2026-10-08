package dev.lumina.ui;

import dev.lumina.debugger.AsyncStackTraceRule;
import dev.lumina.debugger.AsyncStackTraceSettings;
import dev.lumina.debugger.DebuggerSettingsManager;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings page for Build, Execution, Deployment > Debugger > Async Stack Traces.
 * Accurately replicates the UI and behavior shown in Image 2.
 */
public class SettingsDebuggerAsyncStackTracesPage extends VBox {

    private final DebuggerSettingsManager manager = DebuggerSettingsManager.getInstance();

    private final CheckBox instrumentingAgentCheck = new CheckBox("Instrumenting agent (requires debugger restart)");
    private final Button configureAnnotationsBtn = new Button("Configure Annotations...");
    private final TableView<AsyncStackTraceRule> rulesTable = new TableView<>();
    private final ObservableList<AsyncStackTraceRule> tableData = FXCollections.observableArrayList();
    private final CheckBox captureLocalVariablesCheck = new CheckBox("Capture local variables (may greatly slow down the execution)");

    private AsyncStackTraceSettings initialSettings;
    private List<String> currentAnnotations = new ArrayList<>();
    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsDebuggerAsyncStackTracesPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(12);

        buildUI();
        loadData();
    }

    private void buildUI() {
        // --- 1. Top row: Instrumenting agent + Configure Annotations ---
        HBox topRow = new HBox(16);
        topRow.setAlignment(Pos.CENTER_LEFT);

        instrumentingAgentCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        instrumentingAgentCheck.selectedProperty().addListener((obs, o, n) -> checkModified());

        configureAnnotationsBtn.setStyle(
                "-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; " +
                "-fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 10 3 10;"
        );
        configureAnnotationsBtn.setOnAction(e -> showConfigureAnnotationsDialog());

        topRow.getChildren().addAll(instrumentingAgentCheck, configureAnnotationsBtn);
        getChildren().add(topRow);

        // --- 2. Section Label ---
        Label sectionLabel = new Label("Breakpoints based:");
        sectionLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        getChildren().add(sectionLabel);

        // --- 3. Toolbar above table ---
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 1 1 0 1; -fx-padding: 4 8 4 8;");

        Button addBtn = createToolbarButton("+", "Add Rule");
        Button removeBtn = createToolbarButton("—", "Remove Rule");
        Button upBtn = createToolbarButton("↑", "Move Up");
        Button downBtn = createToolbarButton("↓", "Move Down");
        Button copyBtn = createToolbarButton("⧉", "Duplicate Rule");
        Button checkAllBtn = createToolbarButton("☑", "Select All");
        Button uncheckAllBtn = createToolbarButton("☐", "Unselect All");
        Button importBtn = createToolbarButton("📥", "Import Rules");
        Button exportBtn = createToolbarButton("📤", "Export Rules");

        addBtn.setOnAction(e -> {
            AsyncStackTraceRule newRule = new AsyncStackTraceRule(true, "", "", "", "", "", "");
            tableData.add(newRule);
            rulesTable.getSelectionModel().select(newRule);
            rulesTable.scrollTo(newRule);
            checkModified();
        });

        removeBtn.setOnAction(e -> {
            AsyncStackTraceRule sel = rulesTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                tableData.remove(sel);
                checkModified();
            }
        });

        upBtn.setOnAction(e -> {
            int idx = rulesTable.getSelectionModel().getSelectedIndex();
            if (idx > 0) {
                AsyncStackTraceRule item = tableData.remove(idx);
                tableData.add(idx - 1, item);
                rulesTable.getSelectionModel().select(idx - 1);
                checkModified();
            }
        });

        downBtn.setOnAction(e -> {
            int idx = rulesTable.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < tableData.size() - 1) {
                AsyncStackTraceRule item = tableData.remove(idx);
                tableData.add(idx + 1, item);
                rulesTable.getSelectionModel().select(idx + 1);
                checkModified();
            }
        });

        copyBtn.setOnAction(e -> {
            AsyncStackTraceRule sel = rulesTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                AsyncStackTraceRule copy = sel.clone();
                copy.setId(java.util.UUID.randomUUID().toString());
                int idx = rulesTable.getSelectionModel().getSelectedIndex();
                tableData.add(idx + 1, copy);
                rulesTable.getSelectionModel().select(copy);
                checkModified();
            }
        });

        checkAllBtn.setOnAction(e -> {
            for (AsyncStackTraceRule r : tableData) {
                r.setEnabled(true);
            }
            rulesTable.refresh();
            checkModified();
        });

        uncheckAllBtn.setOnAction(e -> {
            for (AsyncStackTraceRule r : tableData) {
                r.setEnabled(false);
            }
            rulesTable.refresh();
            checkModified();
        });

        importBtn.setOnAction(e -> importRules());
        exportBtn.setOnAction(e -> exportRules());

        toolbar.getChildren().addAll(
                addBtn, removeBtn, upBtn, downBtn, copyBtn,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                checkAllBtn, uncheckAllBtn,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                importBtn, exportBtn
        );

        // --- 4. TableView ---
        rulesTable.setEditable(true);
        rulesTable.setItems(tableData);
        rulesTable.setPrefHeight(340);
        rulesTable.setMinHeight(200);
        VBox.setVgrow(rulesTable, Priority.ALWAYS);
        rulesTable.setStyle(
                "-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 1 1 1; " +
                "-fx-table-cell-border-color: #2B2D30;"
        );

        // Checkbox column
        TableColumn<AsyncStackTraceRule, Boolean> checkCol = new TableColumn<>("");
        checkCol.setPrefWidth(36);
        checkCol.setMinWidth(36);
        checkCol.setMaxWidth(36);
        checkCol.setCellFactory(CheckBoxTableCell.forTableColumn(param -> {
            AsyncStackTraceRule rule = rulesTable.getItems().get(param);
            SimpleBooleanProperty prop = new SimpleBooleanProperty(rule.isEnabled());
            prop.addListener((obs, o, n) -> {
                rule.setEnabled(n);
                checkModified();
            });
            return prop;
        }));

        TableColumn<AsyncStackTraceRule, String> capClassCol = createTextColumn("Capture class name", 180,
                AsyncStackTraceRule::getCaptureClassName, AsyncStackTraceRule::setCaptureClassName);

        TableColumn<AsyncStackTraceRule, String> capMethodCol = createTextColumn("Capture method name", 150,
                AsyncStackTraceRule::getCaptureMethodName, AsyncStackTraceRule::setCaptureMethodName);

        TableColumn<AsyncStackTraceRule, String> capKeyCol = createTextColumn("Capture key express...", 150,
                AsyncStackTraceRule::getCaptureKeyExpression, AsyncStackTraceRule::setCaptureKeyExpression);

        TableColumn<AsyncStackTraceRule, String> insClassCol = createTextColumn("Insert class name", 180,
                AsyncStackTraceRule::getInsertClassName, AsyncStackTraceRule::setInsertClassName);

        TableColumn<AsyncStackTraceRule, String> insMethodCol = createTextColumn("Insert method name", 150,
                AsyncStackTraceRule::getInsertMethodName, AsyncStackTraceRule::setInsertMethodName);

        TableColumn<AsyncStackTraceRule, String> insKeyCol = createTextColumn("Insert key expression", 150,
                AsyncStackTraceRule::getInsertKeyExpression, AsyncStackTraceRule::setInsertKeyExpression);

        rulesTable.getColumns().addAll(checkCol, capClassCol, capMethodCol, capKeyCol, insClassCol, insMethodCol, insKeyCol);

        VBox tableContainer = new VBox(toolbar, rulesTable);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);
        getChildren().add(tableContainer);

        // --- 5. Bottom Checkbox ---
        captureLocalVariablesCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        captureLocalVariablesCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        getChildren().add(captureLocalVariablesCheck);
    }

    private Button createToolbarButton(String text, String tooltipText) {
        Button btn = new Button(text);
        btn.setTooltip(new Tooltip(tooltipText));
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6 2 6;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #35373B; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6 2 6;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6 2 6;"));
        return btn;
    }

    private TableColumn<AsyncStackTraceRule, String> createTextColumn(String title, double width,
                                                                      java.util.function.Function<AsyncStackTraceRule, String> getter,
                                                                      java.util.function.BiConsumer<AsyncStackTraceRule, String> setter) {
        TableColumn<AsyncStackTraceRule, String> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setCellValueFactory(cellData -> new SimpleStringProperty(getter.apply(cellData.getValue())));
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(event -> {
            setter.accept(event.getRowValue(), event.getNewValue());
            checkModified();
        });
        return col;
    }

    private void showConfigureAnnotationsDialog() {
        Dialog<List<String>> dialog = new Dialog<>();
        dialog.setTitle("Configure Async Annotations");
        dialog.setHeaderText("Specify annotations marking asynchronous execution methods:");

        ObservableList<String> annotationsList = FXCollections.observableArrayList(currentAnnotations);
        ListView<String> listView = new ListView<>(annotationsList);
        listView.setPrefHeight(200);

        Button addBtn = new Button("Add...");
        Button removeBtn = new Button("Remove");
        addBtn.setOnAction(e -> {
            TextInputDialog tid = new TextInputDialog("org.example.annotation.Async");
            tid.setTitle("Add Annotation");
            tid.setHeaderText("Enter fully qualified annotation name:");
            tid.showAndWait().ifPresent(ann -> {
                if (!ann.isBlank() && !annotationsList.contains(ann.trim())) {
                    annotationsList.add(ann.trim());
                }
            });
        });
        removeBtn.setOnAction(e -> {
            String sel = listView.getSelectionModel().getSelectedItem();
            if (sel != null) annotationsList.remove(sel);
        });

        HBox btnBox = new HBox(8, addBtn, removeBtn);
        VBox content = new VBox(10, listView, btnBox);
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btnType -> btnType == ButtonType.OK ? new ArrayList<>(annotationsList) : null);

        dialog.showAndWait().ifPresent(res -> {
            currentAnnotations = res;
            checkModified();
        });
    }

    private void importRules() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Import Async Stack Trace Rules");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        Stage stage = (Stage) getScene().getWindow();
        File file = fc.showOpenDialog(stage);
        if (file != null) {
            try {
                String json = Files.readString(file.toPath());
                boolean ok = manager.importAsyncRulesFromJson(json);
                if (ok) {
                    loadData();
                    checkModified();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void exportRules() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Async Stack Trace Rules");
        fc.setInitialFileName("async_stack_rules.json");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        Stage stage = (Stage) getScene().getWindow();
        File file = fc.showSaveDialog(stage);
        if (file != null) {
            try {
                String json = manager.exportAsyncRulesToJson();
                Files.writeString(file.toPath(), json);
            } catch (Exception ignored) {
            }
        }
    }

    public void loadData() {
        suppressEvents = true;
        initialSettings = manager.getAsyncStackTraceSettings();
        currentAnnotations = new ArrayList<>(initialSettings.getConfiguredAnnotations());

        instrumentingAgentCheck.setSelected(initialSettings.isInstrumentingAgent());
        captureLocalVariablesCheck.setSelected(initialSettings.isCaptureLocalVariables());

        tableData.clear();
        for (AsyncStackTraceRule r : initialSettings.getRules()) {
            tableData.add(r.clone());
        }

        suppressEvents = false;
        checkModified();
    }

    public AsyncStackTraceSettings getCurrentSettings() {
        AsyncStackTraceSettings s = new AsyncStackTraceSettings();
        s.setInstrumentingAgent(instrumentingAgentCheck.isSelected());
        s.setConfiguredAnnotations(new ArrayList<>(currentAnnotations));
        s.setCaptureLocalVariables(captureLocalVariablesCheck.isSelected());

        List<AsyncStackTraceRule> rules = new ArrayList<>();
        for (AsyncStackTraceRule r : tableData) {
            rules.add(r.clone());
        }
        s.setRules(rules);
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettings());
    }

    public void apply() {
        if (isModified()) {
            initialSettings = getCurrentSettings();
            manager.setAsyncStackTraceSettings(initialSettings);
            checkModified();
        }
    }

    public void reset() {
        loadData();
    }

    public void revert() {
        reset();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void checkModified() {
        if (suppressEvents) return;
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }
}
