package dev.lumina.ui;

import dev.lumina.debugger.DebuggerSettingsManager;
import dev.lumina.debugger.DebuggerSteppingSettings;
import dev.lumina.debugger.SteppingFilter;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings page for Build, Execution, Deployment > Debugger > Stepping.
 * Accurately replicates the UI and behavior shown in Images 3, 4, 5.
 */
public class SettingsDebuggerSteppingPage extends VBox {

    private final DebuggerSettingsManager manager = DebuggerSettingsManager.getInstance();

    // Java section
    private final CheckBox alwaysSmartStepIntoCheck = new CheckBox("Always do smart step into");
    private final CheckBox skipSyntheticCheck = new CheckBox("Skip synthetic methods");
    private final CheckBox skipConstructorsCheck = new CheckBox("Skip constructors");
    private final CheckBox skipClassLoadersCheck = new CheckBox("Skip class loaders");
    private final CheckBox skipSimpleGettersCheck = new CheckBox("Skip simple getters");

    private final CheckBox filterClassesCheck = new CheckBox("Do not step into the classes");
    private final ObservableList<SteppingFilter> classFiltersList = FXCollections.observableArrayList();
    private final ListView<SteppingFilter> classFiltersView = new ListView<>(classFiltersList);
    private final VBox classFiltersContainer = new VBox();

    private final CheckBox hideStackFramesCheck = new CheckBox("Hide stack frames using stepping filters");

    private final ToggleGroup evaluateFinallyGroup = new ToggleGroup();
    private final RadioButton evalAlwaysRadio = new RadioButton("Always");
    private final RadioButton evalNeverRadio = new RadioButton("Never");
    private final RadioButton evalAskRadio = new RadioButton("Ask");

    private final CheckBox resumeOnlyCurrentThreadCheck = new CheckBox("Resume only the current thread");

    // Groovy section
    private final CheckBox skipGroovyClassesCheck = new CheckBox("Do not step into specific Groovy classes");

    // Oracle section
    private final ComboBox<String> oracleModeCombo = new ComboBox<>();
    private final CheckBox oraclePauseAtBeginCheck = new CheckBox();

    // Kotlin section
    private final CheckBox kotlinSkipRuntimeClassesCheck = new CheckBox("Do not step into Kotlin runtime library implementation classes");
    private final CheckBox kotlinAlwaysSmartStepCheck = new CheckBox("Always do smart step into");

    // JavaScript section
    private final CheckBox jsAlwaysSmartStepCheck = new CheckBox("Always do smart step into");
    private final CheckBox jsSkipLibraryScriptsCheck = new CheckBox("Do not step into library scripts");
    private final CheckBox jsSkipScriptsCheck = new CheckBox("Do not step into scripts:");

    private final ObservableList<SteppingFilter> jsScriptFiltersList = FXCollections.observableArrayList();
    private final ListView<SteppingFilter> jsScriptFiltersView = new ListView<>(jsScriptFiltersList);
    private final VBox jsScriptFiltersContainer = new VBox();

    private DebuggerSteppingSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsDebuggerSteppingPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 20, 20, 20));
        setSpacing(10);

        buildUI();
        loadData();
    }

    private void buildUI() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #1E1F22;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox content = new VBox(14);
        content.setStyle("-fx-background-color: #1E1F22;");
        content.setPadding(new Insets(4, 8, 16, 4));

        // --- 1. Java Section ---
        VBox javaBox = buildJavaSection();
        // --- 2. Groovy Section ---
        VBox groovyBox = buildGroovySection();
        // --- 3. Oracle Section ---
        VBox oracleBox = buildOracleSection();
        // --- 4. Kotlin Section ---
        VBox kotlinBox = buildKotlinSection();
        // --- 5. JavaScript Section ---
        VBox jsBox = buildJavaScriptSection();

        content.getChildren().addAll(javaBox, groovyBox, oracleBox, kotlinBox, jsBox);
        scrollPane.setContent(content);

        getChildren().add(scrollPane);
    }

    private VBox buildJavaSection() {
        VBox box = new VBox(8);
        Node header = createSectionHeader("Java");

        styleCheck(alwaysSmartStepIntoCheck);
        styleCheck(skipSyntheticCheck);
        styleCheck(skipConstructorsCheck);
        styleCheck(skipClassLoadersCheck);
        styleCheck(skipSimpleGettersCheck);
        styleCheck(filterClassesCheck);

        alwaysSmartStepIntoCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        skipSyntheticCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        skipConstructorsCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        skipClassLoadersCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        skipSimpleGettersCheck.selectedProperty().addListener((obs, o, n) -> checkModified());

        filterClassesCheck.selectedProperty().addListener((obs, o, n) -> {
            classFiltersContainer.setDisable(!n);
            checkModified();
        });

        buildClassFiltersUI();
        VBox.setMargin(classFiltersContainer, new Insets(0, 0, 0, 20));

        styleCheck(hideStackFramesCheck);
        hideStackFramesCheck.selectedProperty().addListener((obs, o, n) -> checkModified());

        // Evaluate finally blocks row
        HBox evalRow = new HBox(12);
        evalRow.setAlignment(Pos.CENTER_LEFT);
        Label evalLabel = new Label("Evaluate finally blocks on pop frame and early return:");
        evalLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        evalAlwaysRadio.setToggleGroup(evaluateFinallyGroup);
        evalNeverRadio.setToggleGroup(evaluateFinallyGroup);
        evalAskRadio.setToggleGroup(evaluateFinallyGroup);

        styleRadio(evalAlwaysRadio);
        styleRadio(evalNeverRadio);
        styleRadio(evalAskRadio);

        evaluateFinallyGroup.selectedToggleProperty().addListener((obs, o, n) -> checkModified());
        evalRow.getChildren().addAll(evalLabel, evalAlwaysRadio, evalNeverRadio, evalAskRadio);

        styleCheck(resumeOnlyCurrentThreadCheck);
        resumeOnlyCurrentThreadCheck.selectedProperty().addListener((obs, o, n) -> checkModified());

        box.getChildren().addAll(
                header,
                alwaysSmartStepIntoCheck,
                skipSyntheticCheck,
                skipConstructorsCheck,
                skipClassLoadersCheck,
                skipSimpleGettersCheck,
                filterClassesCheck,
                classFiltersContainer,
                hideStackFramesCheck,
                evalRow,
                resumeOnlyCurrentThreadCheck
        );
        return box;
    }

    private void buildClassFiltersUI() {
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 1 1 0 1; -fx-padding: 3 6 3 6;");

        Button addBtn = createToolbarButton("+", "Add Class Filter");
        Button removeBtn = createToolbarButton("—", "Remove Filter");

        addBtn.setOnAction(e -> showSteppingFilterDialog(pattern -> {
            for (String p : pattern.split(";")) {
                String clean = p.trim();
                if (!clean.isEmpty()) {
                    SteppingFilter filter = new SteppingFilter(true, clean);
                    classFiltersList.add(filter);
                }
            }
            checkModified();
        }));

        removeBtn.setOnAction(e -> {
            SteppingFilter sel = classFiltersView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                classFiltersList.remove(sel);
                checkModified();
            }
        });

        toolbar.getChildren().addAll(addBtn, removeBtn);

        classFiltersView.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 1 1 1;");
        classFiltersView.setPrefHeight(180);
        classFiltersView.setMaxHeight(220);

        classFiltersView.setCellFactory(lv -> new ListCell<>() {
            private final CheckBox check = new CheckBox();
            private final Label label = new Label();
            private final HBox box = new HBox(8, check, label);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                check.setOnAction(e -> {
                    SteppingFilter item = getItem();
                    if (item != null) {
                        item.setEnabled(check.isSelected());
                        checkModified();
                    }
                });
            }

            @Override
            protected void updateItem(SteppingFilter item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    check.setSelected(item.isEnabled());
                    label.setText(item.getPattern());
                    label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
                    setGraphic(box);
                }
            }
        });

        classFiltersContainer.getChildren().addAll(toolbar, classFiltersView);
    }

    private VBox buildGroovySection() {
        VBox box = new VBox(8);
        Node header = createSectionHeader("Groovy");

        styleCheck(skipGroovyClassesCheck);
        skipGroovyClassesCheck.selectedProperty().addListener((obs, o, n) -> checkModified());

        box.getChildren().addAll(header, skipGroovyClassesCheck);
        return box;
    }

    private VBox buildOracleSection() {
        VBox box = new VBox(8);
        Node header = createSectionHeader("Oracle");

        HBox modeRow = new HBox(12);
        modeRow.setAlignment(Pos.CENTER_LEFT);
        Label modeLabel = new Label("Stepping mode:");
        modeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        oracleModeCombo.getItems().setAll("Graceful", "Normal", "Aggressive");
        oracleModeCombo.setValue("Graceful");
        oracleModeCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4;");
        oracleModeCombo.valueProperty().addListener((obs, o, n) -> checkModified());

        modeRow.getChildren().addAll(modeLabel, oracleModeCombo);

        HBox pauseRow = new HBox(12);
        pauseRow.setAlignment(Pos.CENTER_LEFT);
        Label pauseLabel = new Label("Pause at begin:");
        pauseLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        oraclePauseAtBeginCheck.selectedProperty().addListener((obs, o, n) -> checkModified());

        pauseRow.getChildren().addAll(pauseLabel, oraclePauseAtBeginCheck);

        box.getChildren().addAll(header, modeRow, pauseRow);
        return box;
    }

    private VBox buildKotlinSection() {
        VBox box = new VBox(8);
        Node header = createSectionHeader("Kotlin");

        styleCheck(kotlinSkipRuntimeClassesCheck);
        styleCheck(kotlinAlwaysSmartStepCheck);

        kotlinSkipRuntimeClassesCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        kotlinAlwaysSmartStepCheck.selectedProperty().addListener((obs, o, n) -> checkModified());

        box.getChildren().addAll(header, kotlinSkipRuntimeClassesCheck, kotlinAlwaysSmartStepCheck);
        return box;
    }

    private VBox buildJavaScriptSection() {
        VBox box = new VBox(8);
        Node header = createSectionHeader("JavaScript");

        styleCheck(jsAlwaysSmartStepCheck);
        styleCheck(jsSkipLibraryScriptsCheck);
        styleCheck(jsSkipScriptsCheck);

        jsAlwaysSmartStepCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        jsSkipLibraryScriptsCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        jsSkipScriptsCheck.selectedProperty().addListener((obs, o, n) -> {
            jsScriptFiltersContainer.setDisable(!n);
            checkModified();
        });

        // Script filters table / list
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 1 1 0 1; -fx-padding: 3 6 3 6;");

        Button addBtn = createToolbarButton("+", "Add Script Filter");
        Button removeBtn = createToolbarButton("—", "Remove Filter");
        Button editBtn = createToolbarButton("✎", "Edit Filter");
        Button upBtn = createToolbarButton("↑", "Move Up");
        Button downBtn = createToolbarButton("↓", "Move Down");
        Button copyBtn = createToolbarButton("⧉", "Duplicate Filter");

        addBtn.setOnAction(e -> {
            TextInputDialog tid = new TextInputDialog("*.min.js");
            tid.setTitle("Add Script Filter");
            tid.setHeaderText("Enter script pattern to skip:");
            tid.showAndWait().ifPresent(pat -> {
                if (!pat.isBlank()) {
                    jsScriptFiltersList.add(new SteppingFilter(true, pat.trim()));
                    checkModified();
                }
            });
        });

        removeBtn.setOnAction(e -> {
            SteppingFilter sel = jsScriptFiltersView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                jsScriptFiltersList.remove(sel);
                checkModified();
            }
        });

        toolbar.getChildren().addAll(addBtn, removeBtn, editBtn, upBtn, downBtn, copyBtn);

        jsScriptFiltersView.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 1 1 1;");
        jsScriptFiltersView.setPrefHeight(120);
        jsScriptFiltersView.setMaxHeight(160);
        jsScriptFiltersView.setPlaceholder(new Label("No script filters configured"));

        jsScriptFiltersContainer.getChildren().addAll(toolbar, jsScriptFiltersView);
        VBox.setMargin(jsScriptFiltersContainer, new Insets(0, 0, 0, 20));

        box.getChildren().addAll(
                header,
                jsAlwaysSmartStepCheck,
                jsSkipLibraryScriptsCheck,
                jsSkipScriptsCheck,
                jsScriptFiltersContainer
        );
        return box;
    }

    /**
     * Stepping Filter dialog matching Image 5.
     */
    private void showSteppingFilterDialog(java.util.function.Consumer<String> onConfirm) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Stepping Filter");
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox content = new VBox(10);
        content.setPadding(new Insets(16));
        content.setStyle("-fx-background-color: #1E1F22;");

        Label promptLabel = new Label("Specify wildcard ('*' and '?' allowed, semicolon ';' as name separator):");
        promptLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        TextField patternField = new TextField();
        patternField.setStyle(
                "-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; " +
                "-fx-border-color: #3574F0; -fx-border-radius: 4; -fx-padding: 6 8 6 8;"
        );
        patternField.setPrefWidth(380);

        content.getChildren().addAll(promptLabel, patternField);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color: #1E1F22;");

        ButtonType okType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, cancelType);

        dialog.setResultConverter(btn -> btn == okType ? patternField.getText() : null);

        dialog.showAndWait().ifPresent(res -> {
            if (res != null && !res.isBlank()) {
                onConfirm.accept(res.trim());
            }
        });
    }

    private Node createSectionHeader(String title) {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #8C9199; -fx-font-size: 13px;");

        Region line = new Region();
        HBox.setHgrow(line, Priority.ALWAYS);
        line.setPrefHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: #393B40;");

        header.getChildren().addAll(label, line);
        return header;
    }

    private Button createToolbarButton(String text, String tooltipText) {
        Button btn = new Button(text);
        btn.setTooltip(new Tooltip(tooltipText));
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6 2 6;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #35373B; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6 2 6;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6 2 6;"));
        return btn;
    }

    private void styleCheck(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleRadio(RadioButton rb) {
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    public void loadData() {
        suppressEvents = true;
        initialSettings = manager.getDebuggerSteppingSettings();

        alwaysSmartStepIntoCheck.setSelected(initialSettings.isAlwaysSmartStepInto());
        skipSyntheticCheck.setSelected(initialSettings.isSkipSyntheticMethods());
        skipConstructorsCheck.setSelected(initialSettings.isSkipConstructors());
        skipClassLoadersCheck.setSelected(initialSettings.isSkipClassLoaders());
        skipSimpleGettersCheck.setSelected(initialSettings.isSkipSimpleGetters());

        filterClassesCheck.setSelected(initialSettings.isFilterClasses());
        classFiltersContainer.setDisable(!initialSettings.isFilterClasses());

        classFiltersList.clear();
        for (SteppingFilter f : initialSettings.getClassFilters()) {
            classFiltersList.add(f.clone());
        }

        hideStackFramesCheck.setSelected(initialSettings.isHideStackFramesUsingSteppingFilters());

        if (initialSettings.getEvaluateFinallyBlocks() == DebuggerSteppingSettings.EvaluateFinallyMode.ALWAYS) {
            evalAlwaysRadio.setSelected(true);
        } else if (initialSettings.getEvaluateFinallyBlocks() == DebuggerSteppingSettings.EvaluateFinallyMode.NEVER) {
            evalNeverRadio.setSelected(true);
        } else {
            evalAskRadio.setSelected(true);
        }

        resumeOnlyCurrentThreadCheck.setSelected(initialSettings.isResumeOnlyCurrentThread());

        skipGroovyClassesCheck.setSelected(initialSettings.isSkipSpecificGroovyClasses());

        oracleModeCombo.setValue(initialSettings.getOracleSteppingMode());
        oraclePauseAtBeginCheck.setSelected(initialSettings.isOraclePauseAtBegin());

        kotlinSkipRuntimeClassesCheck.setSelected(initialSettings.isKotlinSkipRuntimeLibraryClasses());
        kotlinAlwaysSmartStepCheck.setSelected(initialSettings.isKotlinAlwaysSmartStepInto());

        jsAlwaysSmartStepCheck.setSelected(initialSettings.isJsAlwaysSmartStepInto());
        jsSkipLibraryScriptsCheck.setSelected(initialSettings.isJsSkipLibraryScripts());
        jsSkipScriptsCheck.setSelected(initialSettings.isJsSkipScripts());
        jsScriptFiltersContainer.setDisable(!initialSettings.isJsSkipScripts());

        jsScriptFiltersList.clear();
        for (SteppingFilter f : initialSettings.getJsScriptFilters()) {
            jsScriptFiltersList.add(f.clone());
        }

        suppressEvents = false;
        checkModified();
    }

    public DebuggerSteppingSettings getCurrentSettings() {
        DebuggerSteppingSettings s = new DebuggerSteppingSettings();
        s.setAlwaysSmartStepInto(alwaysSmartStepIntoCheck.isSelected());
        s.setSkipSyntheticMethods(skipSyntheticCheck.isSelected());
        s.setSkipConstructors(skipConstructorsCheck.isSelected());
        s.setSkipClassLoaders(skipClassLoadersCheck.isSelected());
        s.setSkipSimpleGetters(skipSimpleGettersCheck.isSelected());
        s.setFilterClasses(filterClassesCheck.isSelected());

        List<SteppingFilter> cList = new ArrayList<>();
        for (SteppingFilter f : classFiltersList) {
            cList.add(f.clone());
        }
        s.setClassFilters(cList);

        s.setHideStackFramesUsingSteppingFilters(hideStackFramesCheck.isSelected());

        if (evalAlwaysRadio.isSelected()) {
            s.setEvaluateFinallyBlocks(DebuggerSteppingSettings.EvaluateFinallyMode.ALWAYS);
        } else if (evalNeverRadio.isSelected()) {
            s.setEvaluateFinallyBlocks(DebuggerSteppingSettings.EvaluateFinallyMode.NEVER);
        } else {
            s.setEvaluateFinallyBlocks(DebuggerSteppingSettings.EvaluateFinallyMode.ASK);
        }

        s.setResumeOnlyCurrentThread(resumeOnlyCurrentThreadCheck.isSelected());

        s.setSkipSpecificGroovyClasses(skipGroovyClassesCheck.isSelected());

        s.setOracleSteppingMode(oracleModeCombo.getValue());
        s.setOraclePauseAtBegin(oraclePauseAtBeginCheck.isSelected());

        s.setKotlinSkipRuntimeLibraryClasses(kotlinSkipRuntimeClassesCheck.isSelected());
        s.setKotlinAlwaysSmartStepInto(kotlinAlwaysSmartStepCheck.isSelected());

        s.setJsAlwaysSmartStepInto(jsAlwaysSmartStepCheck.isSelected());
        s.setJsSkipLibraryScripts(jsSkipLibraryScriptsCheck.isSelected());
        s.setJsSkipScripts(jsSkipScriptsCheck.isSelected());

        List<SteppingFilter> jsList = new ArrayList<>();
        for (SteppingFilter f : jsScriptFiltersList) {
            jsList.add(f.clone());
        }
        s.setJsScriptFilters(jsList);

        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettings());
    }

    public void apply() {
        if (isModified()) {
            initialSettings = getCurrentSettings();
            manager.setDebuggerSteppingSettings(initialSettings);
            checkModified();
        }
    }

    public void reset() {
        loadData();
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
