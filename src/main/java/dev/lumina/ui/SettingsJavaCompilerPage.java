package dev.lumina.ui;

import dev.lumina.build.JavaCompilerSettings;
import dev.lumina.build.JavaCompilerSettingsManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Settings page for Build, Execution, Deployment > Compiler > Java Compiler.
 * Matches 1:1 with reference screenshot media_1791449869458_bd73df6d.png:
 *  - Use compiler: Javac / Eclipse / ajc
 *  - Use '--release' option for cross-compilation (Java 9 and later)
 *  - Project bytecode version: Same as language level
 *  - Per-module bytecode version: table with Module and Target bytecode version
 *  - Javac Options:
 *    - Use compiler from module target JDK when possible
 *    - Generate debugging info
 *    - Report use of deprecated features
 *    - Generate no warnings
 *    - Additional command line parameters (with expand button and cross-platform hint)
 *  - Override compiler parameters per-module: table with Module and Compilation options
 */
public class SettingsJavaCompilerPage extends VBox {

    private final JavaCompilerSettingsManager manager = JavaCompilerSettingsManager.getInstance();

    private final ComboBox<String> useCompilerCombo = new ComboBox<>();
    private final CheckBox useReleaseOptionCheck = new CheckBox("Use '--release' option for cross-compilation (Java 9 and later)");
    private final ComboBox<String> projectBytecodeVersionCombo = new ComboBox<>();

    // Per-module bytecode version table
    private final ObservableList<JavaCompilerSettings.ModuleBytecodeVersion> perModuleBytecodeList = FXCollections.observableArrayList();
    private final TableView<JavaCompilerSettings.ModuleBytecodeVersion> perModuleBytecodeTable = new TableView<>();
    private final Button addBytecodeBtn = new Button("+");
    private final Button removeBytecodeBtn = new Button("−");

    // Compiler Options
    private final Label compilerOptionsHeader = new Label("Javac Options");
    private final CheckBox useCompilerFromModuleJdkCheck = new CheckBox("Use compiler from module target JDK when possible");
    private final CheckBox generateDebuggingInfoCheck = new CheckBox("Generate debugging info");
    private final CheckBox reportDeprecatedCheck = new CheckBox("Report use of deprecated features");
    private final CheckBox generateNoWarningsCheck = new CheckBox("Generate no warnings");

    private final TextField additionalParamsField = new TextField();
    private final Button expandParamsBtn = new Button("⤢");

    // Override compiler parameters per-module table
    private final ObservableList<JavaCompilerSettings.ModuleCompilerParameter> perModuleParamsList = FXCollections.observableArrayList();
    private final TableView<JavaCompilerSettings.ModuleCompilerParameter> perModuleParamsTable = new TableView<>();
    private final Button addParamBtn = new Button("+");
    private final Button removeParamBtn = new Button("−");

    private JavaCompilerSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsJavaCompilerPage() {
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(12);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    private void buildUI() {
        // 1. Top row: Use compiler
        Label useCompilerLabel = new Label("Use compiler:");
        useCompilerLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        useCompilerCombo.getItems().addAll("Javac", "Eclipse", "ajc");
        useCompilerCombo.setValue("Javac");
        styleComboBox(useCompilerCombo);
        useCompilerCombo.setOnAction(e -> {
            String sel = useCompilerCombo.getValue();
            compilerOptionsHeader.setText((sel != null ? sel : "Javac") + " Options");
            fireModified();
        });

        HBox compilerRow = new HBox(12, useCompilerLabel, useCompilerCombo);
        compilerRow.setAlignment(Pos.CENTER_LEFT);

        // 2. Release option checkbox
        useReleaseOptionCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        useReleaseOptionCheck.setOnAction(e -> fireModified());

        // 3. Project bytecode version
        Label projectBytecodeLabel = new Label("Project bytecode version:");
        projectBytecodeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        projectBytecodeVersionCombo.getItems().addAll(
                "Same as language level", "8", "11", "17", "21", "22", "23", "24", "25"
        );
        projectBytecodeVersionCombo.setValue("Same as language level");
        styleComboBox(projectBytecodeVersionCombo);
        projectBytecodeVersionCombo.setOnAction(e -> fireModified());

        HBox bytecodeRow = new HBox(12, projectBytecodeLabel, projectBytecodeVersionCombo);
        bytecodeRow.setAlignment(Pos.CENTER_LEFT);

        // 4. Per-module bytecode version table
        Label perModuleBytecodeLabel = new Label("Per-module bytecode version:");
        perModuleBytecodeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox bytecodeToolbar = createMiniToolbar(addBytecodeBtn, removeBytecodeBtn);
        addBytecodeBtn.setOnAction(e -> showAddModuleBytecodeDialog());
        removeBytecodeBtn.setOnAction(e -> {
            JavaCompilerSettings.ModuleBytecodeVersion sel = perModuleBytecodeTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                perModuleBytecodeList.remove(sel);
                fireModified();
            }
        });

        setupPerModuleBytecodeTable();
        VBox perModuleBytecodeBox = new VBox(4, perModuleBytecodeLabel, bytecodeToolbar, perModuleBytecodeTable);

        // 5. Compiler Options Section
        compilerOptionsHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 0 2 0;");

        styleCheckBox(useCompilerFromModuleJdkCheck);
        styleCheckBox(generateDebuggingInfoCheck);
        styleCheckBox(reportDeprecatedCheck);
        styleCheckBox(generateNoWarningsCheck);

        useCompilerFromModuleJdkCheck.setOnAction(e -> fireModified());
        generateDebuggingInfoCheck.setOnAction(e -> fireModified());
        reportDeprecatedCheck.setOnAction(e -> fireModified());
        generateNoWarningsCheck.setOnAction(e -> fireModified());

        VBox compilerChecks = new VBox(6,
                useCompilerFromModuleJdkCheck,
                generateDebuggingInfoCheck,
                reportDeprecatedCheck,
                generateNoWarningsCheck
        );

        // 6. Additional command line parameters
        Label additionalParamsLabel = new Label("Additional command line parameters:");
        additionalParamsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        styleTextField(additionalParamsField);
        HBox.setHgrow(additionalParamsField, Priority.ALWAYS);
        additionalParamsField.textProperty().addListener((obs, o, n) -> fireModified());

        styleMiniButton(expandParamsBtn);
        expandParamsBtn.setOnAction(e -> openExpandDialog("Additional command line parameters", additionalParamsField));

        HBox paramsRow = new HBox(6, additionalParamsField, expandParamsBtn);
        paramsRow.setAlignment(Pos.CENTER_LEFT);

        Label paramsHint = new Label("'/' recommended in paths for cross-platform configurations");
        paramsHint.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px;");

        VBox paramsBox = new VBox(4, additionalParamsLabel, paramsRow, paramsHint);

        // 7. Override compiler parameters per-module table
        Label overrideParamsLabel = new Label("Override compiler parameters per-module:");
        overrideParamsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 6 0 0 0;");

        HBox paramsToolbar = createMiniToolbar(addParamBtn, removeParamBtn);
        addParamBtn.setOnAction(e -> showAddModuleParamDialog());
        removeParamBtn.setOnAction(e -> {
            JavaCompilerSettings.ModuleCompilerParameter sel = perModuleParamsTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                perModuleParamsList.remove(sel);
                fireModified();
            }
        });

        setupPerModuleParamsTable();
        VBox perModuleParamsBox = new VBox(4, overrideParamsLabel, paramsToolbar, perModuleParamsTable);

        getChildren().addAll(
                compilerRow,
                useReleaseOptionCheck,
                bytecodeRow,
                perModuleBytecodeBox,
                compilerOptionsHeader,
                compilerChecks,
                paramsBox,
                perModuleParamsBox
        );
    }

    private void setupPerModuleBytecodeTable() {
        perModuleBytecodeTable.setItems(perModuleBytecodeList);
        perModuleBytecodeTable.setPrefHeight(100);
        perModuleBytecodeTable.setMaxHeight(130);
        perModuleBytecodeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        perModuleBytecodeTable.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4;");

        TableColumn<JavaCompilerSettings.ModuleBytecodeVersion, String> colModule = new TableColumn<>("Module");
        colModule.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getModule()));
        colModule.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label icon = new Label("📁");
                    icon.setStyle("-fx-font-size: 11px;");
                    Label text = new Label(item);
                    text.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                    HBox box = new HBox(6, icon, text);
                    box.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box);
                }
            }
        });
        colModule.setPrefWidth(300);

        TableColumn<JavaCompilerSettings.ModuleBytecodeVersion, String> colVer = new TableColumn<>("Target bytecode ver...");
        colVer.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTargetBytecodeVersion()));
        colVer.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER_RIGHT);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-alignment: CENTER-RIGHT;");
                }
            }
        });
        colVer.setPrefWidth(140);

        perModuleBytecodeTable.getColumns().addAll(colModule, colVer);
    }

    private void setupPerModuleParamsTable() {
        perModuleParamsTable.setItems(perModuleParamsList);
        perModuleParamsTable.setPrefHeight(100);
        perModuleParamsTable.setMaxHeight(130);
        perModuleParamsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        perModuleParamsTable.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4;");

        TableColumn<JavaCompilerSettings.ModuleCompilerParameter, String> colModule = new TableColumn<>("Module");
        colModule.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getModule()));
        colModule.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label icon = new Label("📁");
                    icon.setStyle("-fx-font-size: 11px;");
                    Label text = new Label(item);
                    text.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                    HBox box = new HBox(6, icon, text);
                    box.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box);
                }
            }
        });
        colModule.setPrefWidth(300);

        TableColumn<JavaCompilerSettings.ModuleCompilerParameter, String> colOpts = new TableColumn<>("Compilation options");
        colOpts.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCompilationOptions()));
        colOpts.setCellFactory(col -> new TableCell<>() {
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
        colOpts.setPrefWidth(200);

        perModuleParamsTable.getColumns().addAll(colModule, colOpts);
    }

    private void showAddModuleBytecodeDialog() {
        Dialog<JavaCompilerSettings.ModuleBytecodeVersion> dialog = new Dialog<>();
        dialog.setTitle("Add Module Bytecode Version");
        dialog.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-border-color: #43454A;");

        Label modLabel = new Label("Module:");
        modLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        TextField modField = new TextField(JavaCompilerSettings.detectCurrentModuleName());
        styleTextField(modField);

        Label verLabel = new Label("Target Bytecode Version:");
        verLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        ComboBox<String> verCombo = new ComboBox<>();
        verCombo.getItems().addAll("8", "11", "17", "21", "22", "23", "24", "25");
        verCombo.setValue("21");
        styleComboBox(verCombo);

        VBox content = new VBox(10, modLabel, modField, verLabel, verCombo);
        content.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(content);

        ButtonType addType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == addType && !modField.getText().isBlank()) {
                return new JavaCompilerSettings.ModuleBytecodeVersion(modField.getText().trim(), verCombo.getValue());
            }
            return null;
        });

        Optional<JavaCompilerSettings.ModuleBytecodeVersion> res = dialog.showAndWait();
        res.ifPresent(v -> {
            perModuleBytecodeList.add(v);
            fireModified();
        });
    }

    private void showAddModuleParamDialog() {
        Dialog<JavaCompilerSettings.ModuleCompilerParameter> dialog = new Dialog<>();
        dialog.setTitle("Override Compiler Parameters Per-Module");
        dialog.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-border-color: #43454A;");

        Label modLabel = new Label("Module:");
        modLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        TextField modField = new TextField(JavaCompilerSettings.detectCurrentModuleName());
        styleTextField(modField);

        Label optsLabel = new Label("Compilation Options:");
        optsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        TextField optsField = new TextField("-parameters");
        styleTextField(optsField);

        VBox content = new VBox(10, modLabel, modField, optsLabel, optsField);
        content.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(content);

        ButtonType addType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == addType && !modField.getText().isBlank()) {
                return new JavaCompilerSettings.ModuleCompilerParameter(modField.getText().trim(), optsField.getText().trim());
            }
            return null;
        });

        Optional<JavaCompilerSettings.ModuleCompilerParameter> res = dialog.showAndWait();
        res.ifPresent(p -> {
            perModuleParamsList.add(p);
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

        dialog.setResultConverter(btn -> {
            if (btn == okType) {
                return area.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(res -> {
            targetField.setText(res);
            fireModified();
        });
    }

    private void styleTextField(TextField f) {
        f.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #707278; " +
                "-fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8; -fx-font-size: 13px;");
    }

    private void styleComboBox(ComboBox<?> c) {
        c.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px;");
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleMiniButton(Button b) {
        b.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #8C8C8C; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand; -fx-font-size: 12px;");
    }

    private HBox createMiniToolbar(Button... buttons) {
        HBox bar = new HBox(2);
        for (Button b : buttons) {
            b.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 8;");
            bar.getChildren().add(b);
        }
        bar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-width: 1 1 0 1; -fx-padding: 2 4;");
        return bar;
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        useCompilerCombo.setValue(initialSettings.getUseCompiler());
        compilerOptionsHeader.setText(initialSettings.getUseCompiler() + " Options");
        useReleaseOptionCheck.setSelected(initialSettings.isUseReleaseOption());
        projectBytecodeVersionCombo.setValue(initialSettings.getProjectBytecodeVersion());

        perModuleBytecodeList.clear();
        for (JavaCompilerSettings.ModuleBytecodeVersion v : initialSettings.getPerModuleBytecodeVersions()) {
            perModuleBytecodeList.add(new JavaCompilerSettings.ModuleBytecodeVersion(v.getModule(), v.getTargetBytecodeVersion()));
        }

        useCompilerFromModuleJdkCheck.setSelected(initialSettings.isUseCompilerFromModuleTargetJdk());
        generateDebuggingInfoCheck.setSelected(initialSettings.isGenerateDebuggingInfo());
        reportDeprecatedCheck.setSelected(initialSettings.isReportDeprecated());
        generateNoWarningsCheck.setSelected(initialSettings.isGenerateNoWarnings());
        additionalParamsField.setText(initialSettings.getAdditionalCommandLineParameters());

        perModuleParamsList.clear();
        for (JavaCompilerSettings.ModuleCompilerParameter p : initialSettings.getPerModuleCompilerParameters()) {
            perModuleParamsList.add(new JavaCompilerSettings.ModuleCompilerParameter(p.getModule(), p.getCompilationOptions()));
        }

        updating = false;
    }

    public JavaCompilerSettings getCurrentSettings() {
        JavaCompilerSettings s = new JavaCompilerSettings();
        s.setUseCompiler(useCompilerCombo.getValue());
        s.setUseReleaseOption(useReleaseOptionCheck.isSelected());
        s.setProjectBytecodeVersion(projectBytecodeVersionCombo.getValue());

        List<JavaCompilerSettings.ModuleBytecodeVersion> bList = new ArrayList<>();
        for (JavaCompilerSettings.ModuleBytecodeVersion v : perModuleBytecodeList) {
            bList.add(new JavaCompilerSettings.ModuleBytecodeVersion(v.getModule(), v.getTargetBytecodeVersion()));
        }
        s.setPerModuleBytecodeVersions(bList);

        s.setUseCompilerFromModuleTargetJdk(useCompilerFromModuleJdkCheck.isSelected());
        s.setGenerateDebuggingInfo(generateDebuggingInfoCheck.isSelected());
        s.setReportDeprecated(reportDeprecatedCheck.isSelected());
        s.setGenerateNoWarnings(generateNoWarningsCheck.isSelected());
        s.setAdditionalCommandLineParameters(additionalParamsField.getText());

        List<JavaCompilerSettings.ModuleCompilerParameter> pList = new ArrayList<>();
        for (JavaCompilerSettings.ModuleCompilerParameter p : perModuleParamsList) {
            pList.add(new JavaCompilerSettings.ModuleCompilerParameter(p.getModule(), p.getCompilationOptions()));
        }
        s.setPerModuleCompilerParameters(pList);

        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettings());
    }

    public void apply() {
        JavaCompilerSettings current = getCurrentSettings();
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

    public ComboBox<String> getUseCompilerCombo() { return useCompilerCombo; }
    public CheckBox getUseReleaseOptionCheck() { return useReleaseOptionCheck; }
    public ComboBox<String> getProjectBytecodeVersionCombo() { return projectBytecodeVersionCombo; }
    public ObservableList<JavaCompilerSettings.ModuleBytecodeVersion> getPerModuleBytecodeList() { return perModuleBytecodeList; }
    public CheckBox getUseCompilerFromModuleJdkCheck() { return useCompilerFromModuleJdkCheck; }
    public CheckBox getGenerateDebuggingInfoCheck() { return generateDebuggingInfoCheck; }
    public CheckBox getReportDeprecatedCheck() { return reportDeprecatedCheck; }
    public CheckBox getGenerateNoWarningsCheck() { return generateNoWarningsCheck; }
    public TextField getAdditionalParamsField() { return additionalParamsField; }
    public ObservableList<JavaCompilerSettings.ModuleCompilerParameter> getPerModuleParamsList() { return perModuleParamsList; }
}
