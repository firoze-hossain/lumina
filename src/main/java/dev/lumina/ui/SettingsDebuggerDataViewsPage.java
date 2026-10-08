package dev.lumina.ui;

import dev.lumina.debugger.DataViewsSettings;
import dev.lumina.debugger.DebuggerSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Objects;

/**
 * Settings page for Build, Execution, Deployment > Debugger > Data Views.
 * Accurately replicates the UI and behavior shown in Image 3.
 */
public class SettingsDebuggerDataViewsPage extends VBox {

    private final DebuggerSettingsManager manager = DebuggerSettingsManager.getInstance();

    private final CheckBox sortAlphabeticallyCheck = new CheckBox("Sort values alphabetically");
    private final CheckBox enableAutoExpressionsCheck = new CheckBox("Enable auto expressions in Variables view");

    // Editor section
    private final CheckBox showValuesInlineCheck = new CheckBox("Show values inline");
    private final CheckBox showValueTooltipCheck = new CheckBox("Show value tooltip.");
    private final Spinner<Integer> tooltipDelaySpinner = new Spinner<>(0, 10000, 700, 50);
    private final Label tooltipHintLabel = new Label("If disabled, use \"alt\" to show/hide tooltips");
    private final CheckBox showTooltipOnCodeSelectionCheck = new CheckBox("Show value tooltip on code selection");

    private DataViewsSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsDebuggerDataViewsPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(10);

        buildUI();
        loadData();
    }

    private void buildUI() {
        // --- 1. Top Checkboxes ---
        styleCheckBox(sortAlphabeticallyCheck);
        styleCheckBox(enableAutoExpressionsCheck);

        sortAlphabeticallyCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        enableAutoExpressionsCheck.selectedProperty().addListener((obs, o, n) -> checkModified());

        getChildren().addAll(sortAlphabeticallyCheck, enableAutoExpressionsCheck);

        // --- 2. Editor Section ---
        VBox editorBox = new VBox(10);
        editorBox.setPadding(new Insets(16, 0, 0, 0));

        Node editorHeader = createSectionHeader("Editor");

        styleCheckBox(showValuesInlineCheck);
        showValuesInlineCheck.selectedProperty().addListener((obs, o, n) -> checkModified());

        // Tooltip row
        HBox tooltipRow = new HBox(8);
        tooltipRow.setAlignment(Pos.CENTER_LEFT);

        styleCheckBox(showValueTooltipCheck);
        showValueTooltipCheck.selectedProperty().addListener((obs, o, n) -> {
            tooltipDelaySpinner.setDisable(!n);
            checkModified();
        });

        Label delayLabel = new Label("Value tooltip delay (ms):");
        delayLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        tooltipDelaySpinner.setPrefWidth(90);
        tooltipDelaySpinner.setEditable(true);
        tooltipDelaySpinner.getEditor().setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        tooltipDelaySpinner.valueProperty().addListener((obs, o, n) -> checkModified());

        tooltipRow.getChildren().addAll(showValueTooltipCheck, delayLabel, tooltipDelaySpinner);

        // Tooltip Hint Label
        tooltipHintLabel.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 12px;");
        VBox.setMargin(tooltipHintLabel, new Insets(0, 0, 0, 22));

        // Tooltip on selection
        styleCheckBox(showTooltipOnCodeSelectionCheck);
        showTooltipOnCodeSelectionCheck.selectedProperty().addListener((obs, o, n) -> checkModified());

        editorBox.getChildren().addAll(
                editorHeader,
                showValuesInlineCheck,
                tooltipRow,
                tooltipHintLabel,
                showTooltipOnCodeSelectionCheck
        );
        getChildren().add(editorBox);
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

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    public void loadData() {
        suppressEvents = true;
        initialSettings = manager.getDataViewsSettings();

        sortAlphabeticallyCheck.setSelected(initialSettings.isSortValuesAlphabetically());
        enableAutoExpressionsCheck.setSelected(initialSettings.isEnableAutoExpressions());
        showValuesInlineCheck.setSelected(initialSettings.isShowValuesInline());
        showValueTooltipCheck.setSelected(initialSettings.isShowValueTooltip());
        tooltipDelaySpinner.getValueFactory().setValue(initialSettings.getValueTooltipDelayMs());
        tooltipDelaySpinner.setDisable(!initialSettings.isShowValueTooltip());
        showTooltipOnCodeSelectionCheck.setSelected(initialSettings.isShowValueTooltipOnCodeSelection());

        suppressEvents = false;
        checkModified();
    }

    public DataViewsSettings getCurrentSettings() {
        DataViewsSettings s = new DataViewsSettings();
        s.setSortValuesAlphabetically(sortAlphabeticallyCheck.isSelected());
        s.setEnableAutoExpressions(enableAutoExpressionsCheck.isSelected());
        s.setShowValuesInline(showValuesInlineCheck.isSelected());
        s.setShowValueTooltip(showValueTooltipCheck.isSelected());
        s.setValueTooltipDelayMs(tooltipDelaySpinner.getValue());
        s.setShowValueTooltipOnCodeSelection(showTooltipOnCodeSelectionCheck.isSelected());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettings());
    }

    public void apply() {
        if (isModified()) {
            initialSettings = getCurrentSettings();
            manager.setDataViewsSettings(initialSettings);
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
