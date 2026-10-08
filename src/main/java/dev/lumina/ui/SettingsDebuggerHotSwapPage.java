package dev.lumina.ui;

import dev.lumina.debugger.DebuggerHotSwapSettings;
import dev.lumina.debugger.DebuggerSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Objects;

/**
 * Settings page for Build, Execution, Deployment > Debugger > HotSwap.
 * Accurately replicates the UI and behavior shown in Image 2.
 */
public class SettingsDebuggerHotSwapPage extends VBox {

    private final DebuggerSettingsManager manager = DebuggerSettingsManager.getInstance();

    // Java section
    private final CheckBox buildBeforeReloadCheck = new CheckBox("Build project before reloading classes");
    private final CheckBox enableJvmHangWarningCheck = new CheckBox("Enable 'JVM will hang' warning");
    private final CheckBox suggestHotSwapCheck = new CheckBox("Suggest HotSwap in the editor when code is modified");

    private final ToggleGroup reloadGroup = new ToggleGroup();
    private final RadioButton reloadAlwaysRadio = new RadioButton("Always");
    private final RadioButton reloadNeverRadio = new RadioButton("Never");
    private final RadioButton reloadAskRadio = new RadioButton("Ask");

    // Groovy section
    private final CheckBox enableGroovyAgentCheck = new CheckBox("Enable hot-swap agent for Groovy code");
    private final Label groovyWarningLabel = new Label("May cause serialization issues in the debugged application");

    private DebuggerHotSwapSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsDebuggerHotSwapPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(12);

        buildUI();
        loadData();
    }

    private void buildUI() {
        // --- 1. Java Section ---
        VBox javaBox = new VBox(10);
        Node javaHeader = createSectionHeader("Java");

        styleCheckBox(buildBeforeReloadCheck);
        styleCheckBox(enableJvmHangWarningCheck);
        styleCheckBox(suggestHotSwapCheck);

        buildBeforeReloadCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        enableJvmHangWarningCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        suggestHotSwapCheck.selectedProperty().addListener((obs, o, n) -> checkModified());

        // Reload classes after compilation row
        HBox reloadRow = new HBox(12);
        reloadRow.setAlignment(Pos.CENTER_LEFT);
        Label reloadLabel = new Label("Reload classes after compilation:");
        reloadLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        reloadAlwaysRadio.setToggleGroup(reloadGroup);
        reloadNeverRadio.setToggleGroup(reloadGroup);
        reloadAskRadio.setToggleGroup(reloadGroup);

        styleRadioButton(reloadAlwaysRadio);
        styleRadioButton(reloadNeverRadio);
        styleRadioButton(reloadAskRadio);

        reloadGroup.selectedToggleProperty().addListener((obs, o, n) -> checkModified());

        reloadRow.getChildren().addAll(reloadLabel, reloadAlwaysRadio, reloadNeverRadio, reloadAskRadio);

        javaBox.getChildren().addAll(
                javaHeader,
                buildBeforeReloadCheck,
                enableJvmHangWarningCheck,
                suggestHotSwapCheck,
                reloadRow
        );
        getChildren().add(javaBox);

        // --- 2. Groovy Section ---
        VBox groovyBox = new VBox(4);
        groovyBox.setPadding(new Insets(12, 0, 0, 0));
        Node groovyHeader = createSectionHeader("Groovy");

        styleCheckBox(enableGroovyAgentCheck);
        enableGroovyAgentCheck.selectedProperty().addListener((obs, o, n) -> checkModified());

        groovyWarningLabel.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 12px;");
        VBox.setMargin(groovyWarningLabel, new Insets(0, 0, 0, 22));

        groovyBox.getChildren().addAll(groovyHeader, enableGroovyAgentCheck, groovyWarningLabel);
        getChildren().add(groovyBox);
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

    private void styleRadioButton(RadioButton rb) {
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    public void loadData() {
        suppressEvents = true;
        initialSettings = manager.getDebuggerHotSwapSettings();

        buildBeforeReloadCheck.setSelected(initialSettings.isBuildProjectBeforeReloading());
        enableJvmHangWarningCheck.setSelected(initialSettings.isEnableJvmWillHangWarning());
        suggestHotSwapCheck.setSelected(initialSettings.isSuggestHotSwapInEditor());

        if (initialSettings.getReloadClassesAfterCompilation() == DebuggerHotSwapSettings.ReloadMode.ALWAYS) {
            reloadAlwaysRadio.setSelected(true);
        } else if (initialSettings.getReloadClassesAfterCompilation() == DebuggerHotSwapSettings.ReloadMode.NEVER) {
            reloadNeverRadio.setSelected(true);
        } else {
            reloadAskRadio.setSelected(true);
        }

        enableGroovyAgentCheck.setSelected(initialSettings.isEnableHotSwapAgentForGroovy());

        suppressEvents = false;
        checkModified();
    }

    public DebuggerHotSwapSettings getCurrentSettings() {
        DebuggerHotSwapSettings s = new DebuggerHotSwapSettings();
        s.setBuildProjectBeforeReloading(buildBeforeReloadCheck.isSelected());
        s.setEnableJvmWillHangWarning(enableJvmHangWarningCheck.isSelected());
        s.setSuggestHotSwapInEditor(suggestHotSwapCheck.isSelected());

        if (reloadAlwaysRadio.isSelected()) {
            s.setReloadClassesAfterCompilation(DebuggerHotSwapSettings.ReloadMode.ALWAYS);
        } else if (reloadNeverRadio.isSelected()) {
            s.setReloadClassesAfterCompilation(DebuggerHotSwapSettings.ReloadMode.NEVER);
        } else {
            s.setReloadClassesAfterCompilation(DebuggerHotSwapSettings.ReloadMode.ASK);
        }

        s.setEnableHotSwapAgentForGroovy(enableGroovyAgentCheck.isSelected());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettings());
    }

    public void apply() {
        if (isModified()) {
            initialSettings = getCurrentSettings();
            manager.setDebuggerHotSwapSettings(initialSettings);
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
