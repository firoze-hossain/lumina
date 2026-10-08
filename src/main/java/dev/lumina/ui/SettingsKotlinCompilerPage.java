package dev.lumina.ui;

import dev.lumina.build.KotlinCompilerSettings;
import dev.lumina.build.KotlinCompilerSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.util.Objects;

/**
 * Settings page for Build, Execution, Deployment > Compiler > Kotlin Compiler.
 * Matches 1:1 with reference screenshot media_1791449881924_e5b30675.png:
 *  - Report compiler warnings
 *  - Kotlin compiler version
 *  - Language version
 *  - API version
 *  - Additional command line parameters
 *  - Keep compiler process alive between invocations
 *  - Kotlin to JVM:
 *    - Enable incremental compilation
 *    - Target JVM version
 *  - Kotlin to JavaScript:
 *    - Enable incremental compilation
 *    - Generate source maps
 *    - Embed source code into source map
 *    - Copy library runtime files
 *    - Destination directory
 *    - Module kind
 *  - Kotlin Script (Beta):
 *    - Script definition template classes to load explicitly
 *    - Classpath required for loading script definition template classes
 */
public class SettingsKotlinCompilerPage extends VBox {

    private final KotlinCompilerSettingsManager manager = KotlinCompilerSettingsManager.getInstance();

    private final CheckBox reportWarningsCheck = new CheckBox("Report compiler warnings");
    private final ComboBox<String> compilerVersionCombo = new ComboBox<>();
    private final ComboBox<String> languageVersionCombo = new ComboBox<>();
    private final ComboBox<String> apiVersionCombo = new ComboBox<>();
    private final TextField additionalParamsField = new TextField();
    private final Button expandParamsBtn = new Button("⤢");
    private final CheckBox keepAliveCheck = new CheckBox("Keep compiler process alive between invocations");

    // Kotlin to JVM
    private final CheckBox jvmIncrementalCheck = new CheckBox("Enable incremental compilation");
    private final ComboBox<String> targetJvmVersionCombo = new ComboBox<>();

    // Kotlin to JavaScript
    private final CheckBox jsIncrementalCheck = new CheckBox("Enable incremental compilation");
    private final CheckBox generateSourceMapsCheck = new CheckBox("Generate source maps");
    private final TextField sourceMapPrefixField = new TextField();
    private final ComboBox<String> embedSourceCodeCombo = new ComboBox<>();
    private final CheckBox copyRuntimeFilesCheck = new CheckBox("Copy library runtime files");
    private final TextField destinationDirectoryField = new TextField();
    private final Button browseDestBtn = new Button("📁");
    private final ComboBox<String> moduleKindCombo = new ComboBox<>();

    // Kotlin Script (Beta)
    private final TextField scriptTemplatesField = new TextField();
    private final TextField scriptClasspathField = new TextField();

    private KotlinCompilerSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsKotlinCompilerPage() {
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(10);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    private void buildUI() {
        // 1. Top checkbox
        styleCheckBox(reportWarningsCheck);
        reportWarningsCheck.setOnAction(e -> fireModified());

        // 2. Main options grid
        GridPane mainGrid = new GridPane();
        mainGrid.setHgap(16);
        mainGrid.setVgap(8);
        mainGrid.getColumnConstraints().addAll(
                new ColumnConstraints(180),
                new ColumnConstraints(200, 400, Double.MAX_VALUE, Priority.ALWAYS, javafx.geometry.HPos.LEFT, true)
        );

        Label verLabel = createLabel("Kotlin compiler version");
        compilerVersionCombo.getItems().addAll("Bundled (2.1.21-release-317)", "2.1.20", "2.0.21", "1.9.24");
        styleComboBox(compilerVersionCombo);
        compilerVersionCombo.setMaxWidth(Double.MAX_VALUE);
        compilerVersionCombo.setOnAction(e -> fireModified());
        mainGrid.addRow(0, verLabel, compilerVersionCombo);

        Label langLabel = createLabel("Language version");
        languageVersionCombo.getItems().addAll("2.2", "2.1", "2.0", "1.9");
        styleComboBox(languageVersionCombo);
        languageVersionCombo.setMaxWidth(Double.MAX_VALUE);
        languageVersionCombo.setOnAction(e -> fireModified());
        mainGrid.addRow(1, langLabel, languageVersionCombo);

        Label apiLabel = createLabel("API version");
        apiVersionCombo.getItems().addAll("2.2", "2.1", "2.0", "1.9");
        styleComboBox(apiVersionCombo);
        apiVersionCombo.setMaxWidth(Double.MAX_VALUE);
        apiVersionCombo.setOnAction(e -> fireModified());
        mainGrid.addRow(2, apiLabel, apiVersionCombo);

        Label addParamsLabel = createLabel("Additional command line parameters:");
        styleTextField(additionalParamsField);
        HBox.setHgrow(additionalParamsField, Priority.ALWAYS);
        additionalParamsField.textProperty().addListener((obs, o, n) -> fireModified());
        styleMiniButton(expandParamsBtn);
        expandParamsBtn.setOnAction(e -> openExpandDialog("Additional command line parameters", additionalParamsField));
        HBox paramsRow = new HBox(6, additionalParamsField, expandParamsBtn);
        paramsRow.setAlignment(Pos.CENTER_LEFT);
        mainGrid.addRow(3, addParamsLabel, paramsRow);

        // 3. Keep alive
        styleCheckBox(keepAliveCheck);
        keepAliveCheck.setOnAction(e -> fireModified());

        // 4. Kotlin to JVM
        Label jvmHeader = createSectionHeader("Kotlin to JVM");
        styleCheckBox(jvmIncrementalCheck);
        jvmIncrementalCheck.setOnAction(e -> fireModified());

        GridPane jvmGrid = new GridPane();
        jvmGrid.setHgap(16);
        jvmGrid.setVgap(8);
        jvmGrid.getColumnConstraints().addAll(
                new ColumnConstraints(180),
                new ColumnConstraints(200, 400, Double.MAX_VALUE, Priority.ALWAYS, javafx.geometry.HPos.LEFT, true)
        );

        Label jvmTargetLabel = createLabel("Target JVM version");
        targetJvmVersionCombo.getItems().addAll("1.8", "9", "10", "11", "17", "21", "22", "23", "24", "25");
        styleComboBox(targetJvmVersionCombo);
        targetJvmVersionCombo.setMaxWidth(Double.MAX_VALUE);
        targetJvmVersionCombo.setOnAction(e -> fireModified());
        jvmGrid.addRow(0, jvmTargetLabel, targetJvmVersionCombo);

        // 5. Kotlin to JavaScript
        Label jsHeader = createSectionHeader("Kotlin to JavaScript");
        styleCheckBox(jsIncrementalCheck);
        jsIncrementalCheck.setOnAction(e -> fireModified());
        styleCheckBox(generateSourceMapsCheck);
        generateSourceMapsCheck.setOnAction(e -> {
            updateSourceMapState();
            fireModified();
        });

        GridPane jsGrid = new GridPane();
        jsGrid.setHgap(16);
        jsGrid.setVgap(8);
        jsGrid.getColumnConstraints().addAll(
                new ColumnConstraints(180),
                new ColumnConstraints(200, 400, Double.MAX_VALUE, Priority.ALWAYS, javafx.geometry.HPos.LEFT, true)
        );

        Label prefixLabel = createLabel("");
        styleTextField(sourceMapPrefixField);
        sourceMapPrefixField.textProperty().addListener((obs, o, n) -> fireModified());
        jsGrid.addRow(0, prefixLabel, sourceMapPrefixField);

        Label embedLabel = createLabel("Embed source code into source map:");
        embedSourceCodeCombo.getItems().addAll(
                "Never", "Always", "When inlining a function from other module with embedded sources"
        );
        styleComboBox(embedSourceCodeCombo);
        embedSourceCodeCombo.setMaxWidth(Double.MAX_VALUE);
        embedSourceCodeCombo.setOnAction(e -> fireModified());
        jsGrid.addRow(1, embedLabel, embedSourceCodeCombo);

        styleCheckBox(copyRuntimeFilesCheck);
        copyRuntimeFilesCheck.setOnAction(e -> fireModified());

        Label destLabel = createLabel("Destination directory");
        styleTextField(destinationDirectoryField);
        HBox.setHgrow(destinationDirectoryField, Priority.ALWAYS);
        destinationDirectoryField.textProperty().addListener((obs, o, n) -> fireModified());
        styleMiniButton(browseDestBtn);
        browseDestBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Choose Destination Directory");
            File f = dc.showDialog(getScene() != null ? getScene().getWindow() : null);
            if (f != null) {
                destinationDirectoryField.setText(f.getAbsolutePath());
                fireModified();
            }
        });
        HBox destRow = new HBox(6, destinationDirectoryField, browseDestBtn);
        destRow.setAlignment(Pos.CENTER_LEFT);
        jsGrid.addRow(2, destLabel, destRow);

        Label kindLabel = createLabel("Module kind:");
        moduleKindCombo.getItems().addAll("Plain (put to global scope)", "AMD", "CommonJS", "UMD", "ES");
        styleComboBox(moduleKindCombo);
        moduleKindCombo.setMaxWidth(Double.MAX_VALUE);
        moduleKindCombo.setOnAction(e -> fireModified());
        jsGrid.addRow(3, kindLabel, moduleKindCombo);

        // 6. Kotlin Script (Beta)
        Label scriptHeader = createSectionHeader("Kotlin Script (Beta)");
        GridPane scriptGrid = new GridPane();
        scriptGrid.setHgap(16);
        scriptGrid.setVgap(8);
        scriptGrid.getColumnConstraints().addAll(
                new ColumnConstraints(180),
                new ColumnConstraints(200, 400, Double.MAX_VALUE, Priority.ALWAYS, javafx.geometry.HPos.LEFT, true)
        );

        Label scriptDefLabel = createLabel("Script definition template classes to load explicitly");
        scriptDefLabel.setWrapText(true);
        styleTextField(scriptTemplatesField);
        scriptTemplatesField.textProperty().addListener((obs, o, n) -> fireModified());
        scriptGrid.addRow(0, scriptDefLabel, scriptTemplatesField);

        Label scriptCpLabel = createLabel("Classpath required for loading script definition template classes");
        scriptCpLabel.setWrapText(true);
        styleTextField(scriptClasspathField);
        scriptClasspathField.textProperty().addListener((obs, o, n) -> fireModified());
        scriptGrid.addRow(1, scriptCpLabel, scriptClasspathField);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #1E1F22; -fx-background-color: transparent; -fx-padding: 0;");

        VBox contentBox = new VBox(10,
                reportWarningsCheck,
                mainGrid,
                keepAliveCheck,
                jvmHeader,
                jvmIncrementalCheck,
                jvmGrid,
                jsHeader,
                jsIncrementalCheck,
                generateSourceMapsCheck,
                jsGrid,
                copyRuntimeFilesCheck,
                scriptHeader,
                scriptGrid
        );
        scroll.setContent(contentBox);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().add(scroll);
    }

    private void updateSourceMapState() {
        boolean enabled = generateSourceMapsCheck.isSelected();
        sourceMapPrefixField.setDisable(!enabled);
        embedSourceCodeCombo.setDisable(!enabled);
    }

    private Label createLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return l;
    }

    private Label createSectionHeader(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 8 0 2 0;");
        return l;
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
            if (btn == okType) return area.getText();
            return null;
        });

        dialog.showAndWait().ifPresent(res -> {
            targetField.setText(res);
            fireModified();
        });
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        reportWarningsCheck.setSelected(initialSettings.isReportCompilerWarnings());
        compilerVersionCombo.setValue(initialSettings.getKotlinCompilerVersion());
        languageVersionCombo.setValue(initialSettings.getLanguageVersion());
        apiVersionCombo.setValue(initialSettings.getApiVersion());
        additionalParamsField.setText(initialSettings.getAdditionalCommandLineParameters());
        keepAliveCheck.setSelected(initialSettings.isKeepCompilerProcessAlive());

        jvmIncrementalCheck.setSelected(initialSettings.isJvmEnableIncrementalCompilation());
        targetJvmVersionCombo.setValue(initialSettings.getTargetJvmVersion());

        jsIncrementalCheck.setSelected(initialSettings.isJsEnableIncrementalCompilation());
        generateSourceMapsCheck.setSelected(initialSettings.isGenerateSourceMaps());
        sourceMapPrefixField.setText(initialSettings.getSourceMapPrefix());
        embedSourceCodeCombo.setValue(initialSettings.getEmbedSourceCodeIntoSourceMap());
        copyRuntimeFilesCheck.setSelected(initialSettings.isCopyLibraryRuntimeFiles());
        destinationDirectoryField.setText(initialSettings.getDestinationDirectory());
        moduleKindCombo.setValue(initialSettings.getModuleKind());

        scriptTemplatesField.setText(initialSettings.getScriptDefinitionTemplateClasses());
        scriptClasspathField.setText(initialSettings.getScriptClasspath());

        updateSourceMapState();
        updating = false;
    }

    public KotlinCompilerSettings getCurrentSettings() {
        KotlinCompilerSettings s = new KotlinCompilerSettings();
        s.setReportCompilerWarnings(reportWarningsCheck.isSelected());
        s.setKotlinCompilerVersion(compilerVersionCombo.getValue());
        s.setLanguageVersion(languageVersionCombo.getValue());
        s.setApiVersion(apiVersionCombo.getValue());
        s.setAdditionalCommandLineParameters(additionalParamsField.getText());
        s.setKeepCompilerProcessAlive(keepAliveCheck.isSelected());

        s.setJvmEnableIncrementalCompilation(jvmIncrementalCheck.isSelected());
        s.setTargetJvmVersion(targetJvmVersionCombo.getValue());

        s.setJsEnableIncrementalCompilation(jsIncrementalCheck.isSelected());
        s.setGenerateSourceMaps(generateSourceMapsCheck.isSelected());
        s.setSourceMapPrefix(sourceMapPrefixField.getText());
        s.setEmbedSourceCodeIntoSourceMap(embedSourceCodeCombo.getValue());
        s.setCopyLibraryRuntimeFiles(copyRuntimeFilesCheck.isSelected());
        s.setDestinationDirectory(destinationDirectoryField.getText());
        s.setModuleKind(moduleKindCombo.getValue());

        s.setScriptDefinitionTemplateClasses(scriptTemplatesField.getText());
        s.setScriptClasspath(scriptClasspathField.getText());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettings());
    }

    public void apply() {
        KotlinCompilerSettings current = getCurrentSettings();
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

    public CheckBox getReportWarningsCheck() { return reportWarningsCheck; }
    public ComboBox<String> getCompilerVersionCombo() { return compilerVersionCombo; }
    public ComboBox<String> getLanguageVersionCombo() { return languageVersionCombo; }
    public ComboBox<String> getApiVersionCombo() { return apiVersionCombo; }
    public TextField getAdditionalParamsField() { return additionalParamsField; }
    public CheckBox getKeepAliveCheck() { return keepAliveCheck; }
    public CheckBox getJvmIncrementalCheck() { return jvmIncrementalCheck; }
    public ComboBox<String> getTargetJvmVersionCombo() { return targetJvmVersionCombo; }
    public CheckBox getJsIncrementalCheck() { return jsIncrementalCheck; }
    public CheckBox getGenerateSourceMapsCheck() { return generateSourceMapsCheck; }
    public ComboBox<String> getEmbedSourceCodeCombo() { return embedSourceCodeCombo; }
    public CheckBox getCopyRuntimeFilesCheck() { return copyRuntimeFilesCheck; }
    public TextField getDestinationDirectoryField() { return destinationDirectoryField; }
    public ComboBox<String> getModuleKindCombo() { return moduleKindCombo; }
    public TextField getScriptTemplatesField() { return scriptTemplatesField; }
    public TextField getScriptClasspathField() { return scriptClasspathField; }
}
