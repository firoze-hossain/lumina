package dev.lumina.ui;

import dev.lumina.build.PythonConsoleSettings;
import dev.lumina.build.PythonConsoleSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * Settings page for Build, Execution, Deployment > Console > Python Console.
 * Matches 1:1 with reference screenshot media_1791450036784_1847c275.png:
 *  - Environment:
 *    - Environment variables (field + browse dialog)
 *    - Python interpreter (Use SDK of module / Use specified interpreter)
 *    - Interpreter options (field + expand dialog)
 *    - Working directory (field + directory chooser)
 *    - Add content roots to PYTHONPATH
 *    - Add source roots to PYTHONPATH
 *  - Starting script (code editor area)
 */
public class SettingsPythonConsolePage extends VBox {

    private final PythonConsoleSettingsManager manager = PythonConsoleSettingsManager.getInstance();

    // Environment controls
    private final TextField envVarsField = new TextField();
    private final Button envVarsBtn = new Button("▤");

    private final ToggleGroup interpreterGroup = new ToggleGroup();
    private final RadioButton useModuleSdkRadio = new RadioButton("Use SDK of module:");
    private final ComboBox<String> moduleCombo = new ComboBox<>();
    private final RadioButton useSpecifiedInterpreterRadio = new RadioButton("Use specified interpreter:");
    private final ComboBox<String> specifiedInterpreterCombo = new ComboBox<>();

    private final TextField interpreterOptionsField = new TextField();
    private final Button expandOptionsBtn = new Button("⤢");

    private final TextField workingDirectoryField = new TextField();
    private final Button browseWorkingDirBtn = new Button("📁");

    private final CheckBox addContentRootsCheck = new CheckBox("Add content roots to PYTHONPATH");
    private final CheckBox addSourceRootsCheck = new CheckBox("Add source roots to PYTHONPATH");

    // Starting script
    private final TextArea startingScriptArea = new TextArea();

    private PythonConsoleSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsPythonConsolePage() {
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(12);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    private void buildUI() {
        // --- 1. Environment Section Header ---
        Label envHeader = new Label("Environment");
        envHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label foldArrow = new Label("⌄");
        foldArrow.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 12px;");
        Separator envSep = new Separator();
        HBox.setHgrow(envSep, Priority.ALWAYS);
        envSep.setStyle("-fx-background-color: #43454A;");
        HBox envHeaderBox = new HBox(6, foldArrow, envHeader, envSep);
        envHeaderBox.setAlignment(Pos.CENTER_LEFT);
        envHeaderBox.setPadding(new Insets(0, 0, 4, 0));

        // --- Grid of Environment Controls ---
        GridPane envGrid = new GridPane();
        envGrid.setHgap(16);
        envGrid.setVgap(10);
        envGrid.setPadding(new Insets(0, 0, 0, 14));
        envGrid.getColumnConstraints().addAll(
                new ColumnConstraints(140),
                new ColumnConstraints(280, 500, Double.MAX_VALUE, Priority.ALWAYS, javafx.geometry.HPos.LEFT, true)
        );

        // Environment variables row
        Label envVarsLabel = new Label("Environment variables:");
        envVarsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        envVarsField.setPromptText("Environment variables");
        styleTextField(envVarsField);
        HBox.setHgrow(envVarsField, Priority.ALWAYS);
        envVarsField.textProperty().addListener((obs, o, n) -> fireModified());
        styleMiniButton(envVarsBtn);
        Tooltip.install(envVarsBtn, new Tooltip("Edit environment variables"));
        envVarsBtn.setOnAction(e -> openEnvVarsDialog());
        HBox envVarsBox = new HBox(6, envVarsField, envVarsBtn);
        envVarsBox.setAlignment(Pos.CENTER_LEFT);
        envGrid.addRow(0, envVarsLabel, envVarsBox);

        // Python interpreter row
        Label pyInterpLabel = new Label("Python interpreter:");
        pyInterpLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        useModuleSdkRadio.setToggleGroup(interpreterGroup);
        styleRadioButton(useModuleSdkRadio);
        populateModules();
        styleComboBox(moduleCombo);
        moduleCombo.setPrefWidth(120);
        moduleCombo.setOnAction(e -> fireModified());

        useSpecifiedInterpreterRadio.setToggleGroup(interpreterGroup);
        styleRadioButton(useSpecifiedInterpreterRadio);
        populateInterpreters();
        styleComboBox(specifiedInterpreterCombo);
        specifiedInterpreterCombo.setPrefWidth(220);
        HBox.setHgrow(specifiedInterpreterCombo, Priority.ALWAYS);
        specifiedInterpreterCombo.setOnAction(e -> fireModified());

        useModuleSdkRadio.selectedProperty().addListener((obs, o, n) -> {
            updateInterpreterState();
            fireModified();
        });
        useSpecifiedInterpreterRadio.selectedProperty().addListener((obs, o, n) -> {
            updateInterpreterState();
            fireModified();
        });

        HBox interpRow = new HBox(10,
                useModuleSdkRadio, moduleCombo,
                useSpecifiedInterpreterRadio, specifiedInterpreterCombo
        );
        interpRow.setAlignment(Pos.CENTER_LEFT);
        envGrid.addRow(1, pyInterpLabel, interpRow);

        // Interpreter options row
        Label interpOptsLabel = new Label("Interpreter options:");
        interpOptsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        styleTextField(interpreterOptionsField);
        HBox.setHgrow(interpreterOptionsField, Priority.ALWAYS);
        interpreterOptionsField.textProperty().addListener((obs, o, n) -> fireModified());
        styleMiniButton(expandOptionsBtn);
        expandOptionsBtn.setOnAction(e -> openExpandDialog("Interpreter options", interpreterOptionsField));
        HBox optionsBox = new HBox(6, interpreterOptionsField, expandOptionsBtn);
        optionsBox.setAlignment(Pos.CENTER_LEFT);
        envGrid.addRow(2, interpOptsLabel, optionsBox);

        // Working directory row
        Label workingDirLabel = new Label("Working directory:");
        workingDirLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        styleTextField(workingDirectoryField);
        HBox.setHgrow(workingDirectoryField, Priority.ALWAYS);
        workingDirectoryField.textProperty().addListener((obs, o, n) -> fireModified());
        styleMiniButton(browseWorkingDirBtn);
        browseWorkingDirBtn.setOnAction(e -> chooseWorkingDirectory());
        HBox workingDirBox = new HBox(6, workingDirectoryField, browseWorkingDirBtn);
        workingDirBox.setAlignment(Pos.CENTER_LEFT);
        envGrid.addRow(3, workingDirLabel, workingDirBox);

        // Checkboxes row
        styleCheckBox(addContentRootsCheck);
        addContentRootsCheck.setOnAction(e -> fireModified());
        styleCheckBox(addSourceRootsCheck);
        addSourceRootsCheck.setOnAction(e -> fireModified());

        VBox checkBoxes = new VBox(6, addContentRootsCheck, addSourceRootsCheck);
        checkBoxes.setPadding(new Insets(2, 0, 4, 14));

        // --- 2. Starting Script Section ---
        Label startingScriptLabel = new Label("Starting script");
        startingScriptLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 8 0 2 0;");

        startingScriptArea.setWrapText(false);
        startingScriptArea.setPrefRowCount(8);
        startingScriptArea.setMinHeight(120);
        startingScriptArea.setPrefHeight(180);
        startingScriptArea.setStyle("-fx-control-inner-background: #1E1F22; -fx-text-fill: #DFE1E5; " +
                "-fx-font-family: 'Fira Code', 'Menlo', 'Consolas', monospace; -fx-font-size: 12px; " +
                "-fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4;");
        startingScriptArea.textProperty().addListener((obs, o, n) -> fireModified());
        VBox.setVgrow(startingScriptArea, Priority.ALWAYS);

        getChildren().addAll(
                envHeaderBox,
                envGrid,
                checkBoxes,
                startingScriptLabel,
                startingScriptArea
        );
    }

    private void populateModules() {
        moduleCombo.getItems().clear();
        List<String> modules = PythonConsoleSettings.detectAvailableModules();
        moduleCombo.getItems().addAll(modules);
        if (!moduleCombo.getItems().isEmpty()) {
            moduleCombo.setValue(moduleCombo.getItems().get(0));
        }
    }

    private void populateInterpreters() {
        specifiedInterpreterCombo.getItems().clear();
        List<String> interpreters = PythonConsoleSettings.detectAvailableInterpreters();
        specifiedInterpreterCombo.getItems().addAll(interpreters);
        if (!specifiedInterpreterCombo.getItems().isEmpty()) {
            specifiedInterpreterCombo.setValue(specifiedInterpreterCombo.getItems().get(0));
        }
    }

    private void updateInterpreterState() {
        boolean useModule = useModuleSdkRadio.isSelected();
        moduleCombo.setDisable(!useModule);
        specifiedInterpreterCombo.setDisable(useModule);
    }

    private void chooseWorkingDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Working Directory");
        String current = workingDirectoryField.getText();
        if (current != null && !current.isBlank()) {
            File curDir = new File(current);
            if (curDir.isDirectory()) {
                chooser.setInitialDirectory(curDir);
            }
        }
        File selected = chooser.showDialog(getScene() != null ? getScene().getWindow() : null);
        if (selected != null) {
            workingDirectoryField.setText(selected.getAbsolutePath());
            fireModified();
        }
    }

    private void openEnvVarsDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Environment Variables");
        dialog.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-border-color: #43454A;");

        Label desc = new Label("Define environment variables (e.g. KEY1=VAL1;KEY2=VAL2):");
        desc.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        TextArea area = new TextArea(envVarsField.getText());
        area.setWrapText(true);
        area.setPrefSize(420, 160);
        area.setStyle("-fx-control-inner-background: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A;");

        VBox box = new VBox(8, desc, area);
        box.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(box);

        ButtonType okType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> btn == okType ? area.getText() : null);
        dialog.showAndWait().ifPresent(res -> {
            envVarsField.setText(res);
            fireModified();
        });
    }

    private void openExpandDialog(String title, TextField targetField) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-border-color: #43454A;");

        TextArea area = new TextArea(targetField.getText());
        area.setWrapText(true);
        area.setPrefSize(420, 200);
        area.setStyle("-fx-control-inner-background: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-family: monospace; -fx-border-color: #43454A;");

        VBox box = new VBox(8, area);
        box.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(box);

        ButtonType okType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> btn == okType ? area.getText() : null);
        dialog.showAndWait().ifPresent(res -> {
            targetField.setText(res);
            fireModified();
        });
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleRadioButton(RadioButton rb) {
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleTextField(TextField f) {
        f.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #707278; " +
                "-fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8; -fx-font-size: 13px;");
    }

    private void styleComboBox(ComboBox<?> c) {
        c.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px;");
    }

    private void styleMiniButton(Button b) {
        b.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #8C8C8C; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand; -fx-font-size: 12px;");
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        envVarsField.setText(initialSettings.getEnvironmentVariables());
        if (initialSettings.isUseModuleSdk()) {
            useModuleSdkRadio.setSelected(true);
        } else {
            useSpecifiedInterpreterRadio.setSelected(true);
        }

        if (initialSettings.getSelectedModule() != null) {
            if (!moduleCombo.getItems().contains(initialSettings.getSelectedModule())) {
                moduleCombo.getItems().add(initialSettings.getSelectedModule());
            }
            moduleCombo.setValue(initialSettings.getSelectedModule());
        }

        if (initialSettings.getSpecifiedInterpreter() != null) {
            if (!specifiedInterpreterCombo.getItems().contains(initialSettings.getSpecifiedInterpreter())) {
                specifiedInterpreterCombo.getItems().add(initialSettings.getSpecifiedInterpreter());
            }
            specifiedInterpreterCombo.setValue(initialSettings.getSpecifiedInterpreter());
        }

        interpreterOptionsField.setText(initialSettings.getInterpreterOptions());
        workingDirectoryField.setText(initialSettings.getWorkingDirectory());
        addContentRootsCheck.setSelected(initialSettings.isAddContentRootsToPythonPath());
        addSourceRootsCheck.setSelected(initialSettings.isAddSourceRootsToPythonPath());
        startingScriptArea.setText(initialSettings.getStartingScript());

        updateInterpreterState();
        updating = false;
    }

    public PythonConsoleSettings getCurrentSettings() {
        PythonConsoleSettings s = new PythonConsoleSettings();
        s.setEnvironmentVariables(envVarsField.getText());
        s.setUseModuleSdk(useModuleSdkRadio.isSelected());
        s.setSelectedModule(moduleCombo.getValue());
        s.setUseSpecifiedInterpreter(useSpecifiedInterpreterRadio.isSelected());
        s.setSpecifiedInterpreter(specifiedInterpreterCombo.getValue());
        s.setInterpreterOptions(interpreterOptionsField.getText());
        s.setWorkingDirectory(workingDirectoryField.getText());
        s.setAddContentRootsToPythonPath(addContentRootsCheck.isSelected());
        s.setAddSourceRootsToPythonPath(addSourceRootsCheck.isSelected());
        s.setStartingScript(startingScriptArea.getText());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettings());
    }

    public void apply() {
        PythonConsoleSettings current = getCurrentSettings();
        manager.setSettings(current);
        initialSettings = current.clone();
        if (onModifiedListener != null) onModifiedListener.run();
    }

    public void reset() {
        loadData();
        if (onModifiedListener != null) onModifiedListener.run();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public TextField getEnvVarsField() { return envVarsField; }
    public RadioButton getUseModuleSdkRadio() { return useModuleSdkRadio; }
    public ComboBox<String> getModuleCombo() { return moduleCombo; }
    public RadioButton getUseSpecifiedInterpreterRadio() { return useSpecifiedInterpreterRadio; }
    public ComboBox<String> getSpecifiedInterpreterCombo() { return specifiedInterpreterCombo; }
    public TextField getInterpreterOptionsField() { return interpreterOptionsField; }
    public TextField getWorkingDirectoryField() { return workingDirectoryField; }
    public CheckBox getAddContentRootsCheck() { return addContentRootsCheck; }
    public CheckBox getAddSourceRootsCheck() { return addSourceRootsCheck; }
    public TextArea getStartingScriptArea() { return startingScriptArea; }
}
