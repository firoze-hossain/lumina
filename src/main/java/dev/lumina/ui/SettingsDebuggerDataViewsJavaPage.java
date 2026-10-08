package dev.lumina.ui;

import dev.lumina.debugger.DebuggerSettingsManager;
import dev.lumina.debugger.JavaDataViewsSettings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Settings page for Build, Execution, Deployment > Debugger > Data Views > Java.
 * Accurately replicates the UI and behavior shown in Image 4.
 */
public class SettingsDebuggerDataViewsJavaPage extends VBox {

    private final DebuggerSettingsManager manager = DebuggerSettingsManager.getInstance();

    // Top checkboxes
    private final CheckBox autoscrollCheck = new CheckBox("Autoscroll to new local variables");
    private final CheckBox predictConditionsCheck = new CheckBox("Predict condition values and exceptions based on data flow analysis");
    private final CheckBox grayOutUnreachableCheck = new CheckBox("Gray out blocks of code that are predicted to be unreachable");

    // Show section
    private final CheckBox declaredTypeCheck = new CheckBox("Declared type");
    private final CheckBox syntheticFieldsCheck = new CheckBox("Synthetic fields");
    private final CheckBox valFieldsCheck = new CheckBox("$val fields as local variables");
    private final CheckBox fqNamesCheck = new CheckBox("Fully qualified names");

    private final CheckBox objectIdCheck = new CheckBox("Object id");
    private final CheckBox staticFieldsCheck = new CheckBox("Static fields");
    private final CheckBox staticFinalFieldsCheck = new CheckBox("Static final fields");

    // Individual checkboxes
    private final CheckBox showTypeStringsCheck = new CheckBox("Show type for strings");
    private final CheckBox showHexPrimitivesCheck = new CheckBox("Show hex value for primitives");
    private final CheckBox hideNullElementsCheck = new CheckBox("Hide null elements in arrays and collections");
    private final CheckBox autoPopulateThrowableCheck = new CheckBox("Auto populate Throwable object's stack trace");
    private final CheckBox enableAlternativeCollectionsCheck = new CheckBox("Enable alternative view for Collections classes");

    // toString() object view
    private final CheckBox enableToStringCheck = new CheckBox("Enable 'toString()' object view:");
    private final ToggleGroup toStringGroup = new ToggleGroup();
    private final RadioButton allOverridingRadio = new RadioButton("For all classes that override 'toString()' method");
    private final RadioButton classesFromListRadio = new RadioButton("For classes from the list:");

    private final ObservableList<String> patternItems = FXCollections.observableArrayList();
    private final ListView<String> patternListView = new ListView<>(patternItems);
    private final VBox patternListContainer = new VBox();

    private JavaDataViewsSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsDebuggerDataViewsJavaPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(9);

        buildUI();
        loadData();
    }

    private void buildUI() {
        // --- 1. Top Checkboxes ---
        styleCheckBox(autoscrollCheck);
        styleCheckBox(predictConditionsCheck);
        styleCheckBox(grayOutUnreachableCheck);

        autoscrollCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        predictConditionsCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        grayOutUnreachableCheck.selectedProperty().addListener((obs, o, n) -> checkModified());

        getChildren().addAll(autoscrollCheck, predictConditionsCheck, grayOutUnreachableCheck);

        // --- 2. Show Section ---
        VBox showBox = new VBox(6);
        showBox.setPadding(new Insets(6, 0, 4, 0));

        Label showLabel = new Label("Show");
        showLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        styleCheckBox(declaredTypeCheck);
        styleCheckBox(syntheticFieldsCheck);
        styleCheckBox(valFieldsCheck);
        styleCheckBox(fqNamesCheck);
        styleCheckBox(objectIdCheck);
        styleCheckBox(staticFieldsCheck);
        styleCheckBox(staticFinalFieldsCheck);

        declaredTypeCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        syntheticFieldsCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        valFieldsCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        fqNamesCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        objectIdCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        staticFieldsCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        staticFinalFieldsCheck.selectedProperty().addListener((obs, o, n) -> checkModified());

        HBox row1 = new HBox(18, declaredTypeCheck, syntheticFieldsCheck, valFieldsCheck, fqNamesCheck);
        row1.setPadding(new Insets(0, 0, 0, 16));

        HBox row2 = new HBox(18, objectIdCheck, staticFieldsCheck, staticFinalFieldsCheck);
        row2.setPadding(new Insets(0, 0, 0, 16));

        showBox.getChildren().addAll(showLabel, row1, row2);
        getChildren().add(showBox);

        // --- 3. Individual Checkboxes ---
        styleCheckBox(showTypeStringsCheck);
        styleCheckBox(showHexPrimitivesCheck);
        styleCheckBox(hideNullElementsCheck);
        styleCheckBox(autoPopulateThrowableCheck);
        styleCheckBox(enableAlternativeCollectionsCheck);

        showTypeStringsCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        showHexPrimitivesCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        hideNullElementsCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        autoPopulateThrowableCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        enableAlternativeCollectionsCheck.selectedProperty().addListener((obs, o, n) -> checkModified());

        getChildren().addAll(
                showTypeStringsCheck,
                showHexPrimitivesCheck,
                hideNullElementsCheck,
                autoPopulateThrowableCheck,
                enableAlternativeCollectionsCheck
        );

        // --- 4. toString() Object View Section ---
        VBox toStringBox = new VBox(6);
        toStringBox.setPadding(new Insets(6, 0, 0, 0));

        styleCheckBox(enableToStringCheck);
        enableToStringCheck.selectedProperty().addListener((obs, o, n) -> {
            allOverridingRadio.setDisable(!n);
            classesFromListRadio.setDisable(!n);
            patternListContainer.setDisable(!n || !classesFromListRadio.isSelected());
            checkModified();
        });

        allOverridingRadio.setToggleGroup(toStringGroup);
        classesFromListRadio.setToggleGroup(toStringGroup);
        styleRadioButton(allOverridingRadio);
        styleRadioButton(classesFromListRadio);

        VBox.setMargin(allOverridingRadio, new Insets(0, 0, 0, 20));
        VBox.setMargin(classesFromListRadio, new Insets(0, 0, 0, 20));

        toStringGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            patternListContainer.setDisable(!enableToStringCheck.isSelected() || !classesFromListRadio.isSelected());
            checkModified();
        });

        // List Container with Mini Toolbar
        buildPatternListUI();
        VBox.setMargin(patternListContainer, new Insets(0, 0, 0, 36));

        toStringBox.getChildren().addAll(
                enableToStringCheck,
                allOverridingRadio,
                classesFromListRadio,
                patternListContainer
        );
        getChildren().add(toStringBox);
    }

    private void buildPatternListUI() {
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 1 1 0 1; -fx-padding: 3 6 3 6;");

        MenuButton addMenuBtn = new MenuButton("+");
        addMenuBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 0 4 0 4;");

        MenuItem addClassItem = new MenuItem("© Add Class...");
        addClassItem.setOnAction(e -> {
            TextInputDialog tid = new TextInputDialog("java.lang.String");
            tid.setTitle("Add Class");
            tid.setHeaderText("Enter fully qualified class name:");
            tid.showAndWait().ifPresent(cls -> {
                if (!cls.isBlank() && !patternItems.contains(cls.trim())) {
                    patternItems.add(cls.trim());
                    checkModified();
                }
            });
        });

        MenuItem addPatternItem = new MenuItem(".* Add Pattern...");
        addPatternItem.setOnAction(e -> {
            TextInputDialog tid = new TextInputDialog("org.example.*");
            tid.setTitle("Add Pattern");
            tid.setHeaderText("Enter class pattern (e.g. com.company.*):");
            tid.showAndWait().ifPresent(pat -> {
                if (!pat.isBlank() && !patternItems.contains(pat.trim())) {
                    patternItems.add(pat.trim());
                    checkModified();
                }
            });
        });

        addMenuBtn.getItems().addAll(addClassItem, addPatternItem);

        Button removeBtn = new Button("—");
        removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 0 4 0 4;");
        removeBtn.setOnAction(e -> {
            String sel = patternListView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                patternItems.remove(sel);
                checkModified();
            }
        });

        toolbar.getChildren().addAll(addMenuBtn, removeBtn);

        patternListView.setPrefHeight(120);
        patternListView.setMaxHeight(140);
        patternListView.setPlaceholder(new Label("No class patterns configured"));
        patternListView.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 1 1 1;");

        patternListContainer.getChildren().addAll(toolbar, patternListView);
        patternListContainer.setMaxWidth(450);
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleRadioButton(RadioButton rb) {
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    public void loadData() {
        suppressEvents = true;
        initialSettings = manager.getJavaDataViewsSettings();

        autoscrollCheck.setSelected(initialSettings.isAutoscrollToNewLocalVariables());
        predictConditionsCheck.setSelected(initialSettings.isPredictConditionValues());
        grayOutUnreachableCheck.setSelected(initialSettings.isGrayOutPredictedUnreachableCode());

        declaredTypeCheck.setSelected(initialSettings.isShowDeclaredType());
        syntheticFieldsCheck.setSelected(initialSettings.isShowSyntheticFields());
        valFieldsCheck.setSelected(initialSettings.isShowValFieldsAsLocalVariables());
        fqNamesCheck.setSelected(initialSettings.isShowFullyQualifiedNames());
        objectIdCheck.setSelected(initialSettings.isShowObjectId());
        staticFieldsCheck.setSelected(initialSettings.isShowStaticFields());
        staticFinalFieldsCheck.setSelected(initialSettings.isShowStaticFinalFields());

        showTypeStringsCheck.setSelected(initialSettings.isShowTypeForStrings());
        showHexPrimitivesCheck.setSelected(initialSettings.isShowHexForPrimitives());
        hideNullElementsCheck.setSelected(initialSettings.isHideNullElements());
        autoPopulateThrowableCheck.setSelected(initialSettings.isAutoPopulateThrowableStackTrace());
        enableAlternativeCollectionsCheck.setSelected(initialSettings.isEnableAlternativeViewForCollections());

        enableToStringCheck.setSelected(initialSettings.isEnableToStringObjectView());
        if (initialSettings.getToStringMode() == JavaDataViewsSettings.ToStringMode.ALL_OVERRIDING) {
            allOverridingRadio.setSelected(true);
        } else {
            classesFromListRadio.setSelected(true);
        }

        patternItems.clear();
        patternItems.addAll(initialSettings.getToStringClassPatterns());

        boolean enableToString = initialSettings.isEnableToStringObjectView();
        allOverridingRadio.setDisable(!enableToString);
        classesFromListRadio.setDisable(!enableToString);
        patternListContainer.setDisable(!enableToString || !classesFromListRadio.isSelected());

        suppressEvents = false;
        checkModified();
    }

    public JavaDataViewsSettings getCurrentSettings() {
        JavaDataViewsSettings s = new JavaDataViewsSettings();
        s.setAutoscrollToNewLocalVariables(autoscrollCheck.isSelected());
        s.setPredictConditionValues(predictConditionsCheck.isSelected());
        s.setGrayOutPredictedUnreachableCode(grayOutUnreachableCheck.isSelected());

        s.setShowDeclaredType(declaredTypeCheck.isSelected());
        s.setShowSyntheticFields(syntheticFieldsCheck.isSelected());
        s.setShowValFieldsAsLocalVariables(valFieldsCheck.isSelected());
        s.setShowFullyQualifiedNames(fqNamesCheck.isSelected());
        s.setShowObjectId(objectIdCheck.isSelected());
        s.setShowStaticFields(staticFieldsCheck.isSelected());
        s.setShowStaticFinalFields(staticFinalFieldsCheck.isSelected());

        s.setShowTypeForStrings(showTypeStringsCheck.isSelected());
        s.setShowHexForPrimitives(showHexPrimitivesCheck.isSelected());
        s.setHideNullElements(hideNullElementsCheck.isSelected());
        s.setAutoPopulateThrowableStackTrace(autoPopulateThrowableCheck.isSelected());
        s.setEnableAlternativeViewForCollections(enableAlternativeCollectionsCheck.isSelected());

        s.setEnableToStringObjectView(enableToStringCheck.isSelected());
        s.setToStringMode(allOverridingRadio.isSelected() ?
                JavaDataViewsSettings.ToStringMode.ALL_OVERRIDING :
                JavaDataViewsSettings.ToStringMode.CLASSES_FROM_LIST);
        s.setToStringClassPatterns(new ArrayList<>(patternItems));
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettings());
    }

    public void apply() {
        if (isModified()) {
            initialSettings = getCurrentSettings();
            manager.setJavaDataViewsSettings(initialSettings);
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
