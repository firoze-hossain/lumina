package dev.lumina.ui;

import dev.lumina.javascript.ESLintSettings;
import dev.lumina.javascript.JavaScriptSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * 1:1 dynamic replica of IntelliJ IDEA Languages & Frameworks > JavaScript > Code Quality Tools > ESLint.
 * Matching Screenshot 4.
 */
public class SettingsLanguagesJSEsLintPage extends VBox {

    private final JavaScriptSettingsManager manager = JavaScriptSettingsManager.getInstance();
    private Runnable onModifiedListener;
    private Runnable onNavigateToActionsOnSave;

    private RadioButton disableRadio;
    private RadioButton automaticRadio;
    private RadioButton manualRadio;
    private ToggleGroup modeGroup;

    private TextField runForFilesField;
    private Label globHintLabel;
    private CheckBox runOnSaveCheck;
    private Hyperlink actionsOnSaveLink;

    private ESLintSettings initialSettings;

    public SettingsLanguagesJSEsLintPage() {
        setSpacing(14);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        ESLintSettings current = manager.getEslintSettings();

        // 1. Radio modes
        modeGroup = new ToggleGroup();

        disableRadio = new RadioButton("Disable ESLint");
        disableRadio.setToggleGroup(modeGroup);
        disableRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");

        automaticRadio = new RadioButton("Automatic ESLint configuration");
        automaticRadio.setToggleGroup(modeGroup);
        automaticRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");

        Label autoHelp = new Label("?");
        autoHelp.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px; -fx-padding: 0 0 0 4;");
        autoHelp.setTooltip(new Tooltip("Detects ESLint configuration and dependencies in project"));
        HBox autoBox = new HBox(4, automaticRadio, autoHelp);
        autoBox.setAlignment(Pos.CENTER_LEFT);

        manualRadio = new RadioButton("Manual ESLint configuration");
        manualRadio.setToggleGroup(modeGroup);
        manualRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");

        switch (current.getMode()) {
            case DISABLED -> disableRadio.setSelected(true);
            case AUTOMATIC -> automaticRadio.setSelected(true);
            case MANUAL -> manualRadio.setSelected(true);
        }

        modeGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            updateControlStates();
            fireModified();
        });

        // 2. Run for files
        HBox runForFilesRow = new HBox(12);
        runForFilesRow.setAlignment(Pos.CENTER_LEFT);

        Label runForFilesLabel = new Label("Run for files:");
        runForFilesLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        runForFilesLabel.setPrefWidth(90);

        runForFilesField = new TextField(current.getRunForFiles());
        runForFilesField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px; -fx-padding: 5 8 5 8;");
        runForFilesField.setMaxWidth(600);
        HBox.setHgrow(runForFilesField, Priority.ALWAYS);
        runForFilesField.textProperty().addListener((obs, oldV, newV) -> fireModified());

        runForFilesRow.getChildren().addAll(runForFilesLabel, runForFilesField);

        globHintLabel = new Label("Use a glob pattern ↗, for example, **/*.{js,ts}");
        globHintLabel.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px; -fx-padding: 0 0 0 102;");

        // 3. Run eslint --fix on save
        HBox saveRow = new HBox(8);
        saveRow.setAlignment(Pos.CENTER_LEFT);

        runOnSaveCheck = new CheckBox("Run eslint --fix on save");
        runOnSaveCheck.setSelected(current.isRunOnSave());
        runOnSaveCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");
        runOnSaveCheck.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        actionsOnSaveLink = new Hyperlink("All actions on save...");
        actionsOnSaveLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 0; -fx-underline: false;");
        actionsOnSaveLink.setOnMouseEntered(e -> actionsOnSaveLink.setStyle("-fx-text-fill: #70AAFF; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 0; -fx-underline: true;"));
        actionsOnSaveLink.setOnMouseExited(e -> actionsOnSaveLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 0; -fx-underline: false;"));
        actionsOnSaveLink.setOnAction(e -> {
            if (onNavigateToActionsOnSave != null) {
                onNavigateToActionsOnSave.run();
            }
        });

        saveRow.getChildren().addAll(runOnSaveCheck, actionsOnSaveLink);

        getChildren().addAll(
                disableRadio,
                autoBox,
                manualRadio,
                runForFilesRow,
                globHintLabel,
                saveRow
        );

        updateControlStates();
    }

    private void updateControlStates() {
        boolean isEnabled = !disableRadio.isSelected();
        runForFilesField.setDisable(!isEnabled);
        runOnSaveCheck.setDisable(!isEnabled);
    }

    public void setOnNavigateToActionsOnSave(Runnable listener) {
        this.onNavigateToActionsOnSave = listener;
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void takeSnapshot() {
        this.initialSettings = getFormSettings();
    }

    private ESLintSettings getFormSettings() {
        ESLintSettings.Mode mode = ESLintSettings.Mode.DISABLED;
        if (automaticRadio.isSelected()) {
            mode = ESLintSettings.Mode.AUTOMATIC;
        } else if (manualRadio.isSelected()) {
            mode = ESLintSettings.Mode.MANUAL;
        }
        return new ESLintSettings(mode, runForFilesField.getText(), runOnSaveCheck.isSelected());
    }

    public boolean isModified() {
        return !Objects.equals(getFormSettings(), initialSettings);
    }

    public void apply() {
        manager.setEslintSettings(getFormSettings());
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        if (initialSettings != null) {
            switch (initialSettings.getMode()) {
                case DISABLED -> disableRadio.setSelected(true);
                case AUTOMATIC -> automaticRadio.setSelected(true);
                case MANUAL -> manualRadio.setSelected(true);
            }
            runForFilesField.setText(initialSettings.getRunForFiles());
            runOnSaveCheck.setSelected(initialSettings.isRunOnSave());
            updateControlStates();
        }
        fireModified();
    }

    public void revertChanges() {
        reset();
    }

    public RadioButton getDisableRadio() {
        return disableRadio;
    }

    public RadioButton getAutomaticRadio() {
        return automaticRadio;
    }

    public RadioButton getManualRadio() {
        return manualRadio;
    }

    public TextField getRunForFilesField() {
        return runForFilesField;
    }

    public CheckBox getRunOnSaveCheck() {
        return runOnSaveCheck;
    }
}
