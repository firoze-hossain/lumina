package dev.lumina.ui;

import dev.lumina.build.BuildConsoleSettings;
import dev.lumina.build.BuildConsoleSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Settings page for Build, Execution, Deployment > Console.
 * Matches 1:1 with reference screenshot media_1791450015890_b8b24dc5.png:
 *  - General settings:
 *    - Always show Debug Console
 *    - Use IPython if available
 *    - Show console variables by default
 *    - Use existing console for "Run with Python Console"
 *    - Command queue for Python Console
 *    - Code completion: Static / Runtime / None
 */
public class SettingsBuildConsolePage extends VBox {

    private final BuildConsoleSettingsManager manager = BuildConsoleSettingsManager.getInstance();

    private final CheckBox alwaysShowDebugConsoleCheck = new CheckBox("Always show Debug Console");
    private final CheckBox useIPythonCheck = new CheckBox("Use IPython if available");
    private final CheckBox showConsoleVariablesCheck = new CheckBox("Show console variables by default");
    private final CheckBox useExistingConsoleCheck = new CheckBox("Use existing console for \"Run with Python Console\"");
    private final CheckBox commandQueueCheck = new CheckBox("Command queue for Python Console");

    private final ComboBox<String> codeCompletionCombo = new ComboBox<>();

    private BuildConsoleSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsBuildConsolePage() {
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(10);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    private void buildUI() {
        Label generalSettingsHeader = new Label("General settings");
        generalSettingsHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 0 0 6 0;");

        styleCheckBox(alwaysShowDebugConsoleCheck);
        styleCheckBox(useIPythonCheck);
        styleCheckBox(showConsoleVariablesCheck);
        styleCheckBox(useExistingConsoleCheck);
        styleCheckBox(commandQueueCheck);

        wireCheckBox(alwaysShowDebugConsoleCheck);
        wireCheckBox(useIPythonCheck);
        wireCheckBox(showConsoleVariablesCheck);
        wireCheckBox(useExistingConsoleCheck);
        wireCheckBox(commandQueueCheck);

        VBox checksBox = new VBox(8,
                alwaysShowDebugConsoleCheck,
                useIPythonCheck,
                showConsoleVariablesCheck,
                useExistingConsoleCheck,
                commandQueueCheck
        );
        checksBox.setPadding(new Insets(0, 0, 8, 16));

        Label codeCompletionLabel = new Label("Code completion");
        codeCompletionLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        codeCompletionCombo.getItems().addAll("Static", "Runtime", "None");
        codeCompletionCombo.setValue("Static");
        codeCompletionCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px;");
        codeCompletionCombo.valueProperty().addListener((obs, o, n) -> fireModified());

        HBox completionRow = new HBox(12, codeCompletionLabel, codeCompletionCombo);
        completionRow.setAlignment(Pos.CENTER_LEFT);
        completionRow.setPadding(new Insets(0, 0, 0, 16));

        getChildren().addAll(generalSettingsHeader, checksBox, completionRow);
    }

    private void wireCheckBox(CheckBox cb) {
        cb.selectedProperty().addListener((obs, oldVal, newVal) -> fireModified());
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        alwaysShowDebugConsoleCheck.setSelected(initialSettings.isAlwaysShowDebugConsole());
        useIPythonCheck.setSelected(initialSettings.isUseIPythonIfAvailable());
        showConsoleVariablesCheck.setSelected(initialSettings.isShowConsoleVariablesByDefault());
        useExistingConsoleCheck.setSelected(initialSettings.isUseExistingConsoleForRunWithPythonConsole());
        commandQueueCheck.setSelected(initialSettings.isCommandQueueForPythonConsole());
        codeCompletionCombo.setValue(initialSettings.getCodeCompletion());

        updating = false;
    }

    public BuildConsoleSettings getCurrentSettings() {
        BuildConsoleSettings s = new BuildConsoleSettings();
        s.setAlwaysShowDebugConsole(alwaysShowDebugConsoleCheck.isSelected());
        s.setUseIPythonIfAvailable(useIPythonCheck.isSelected());
        s.setShowConsoleVariablesByDefault(showConsoleVariablesCheck.isSelected());
        s.setUseExistingConsoleForRunWithPythonConsole(useExistingConsoleCheck.isSelected());
        s.setCommandQueueForPythonConsole(commandQueueCheck.isSelected());
        s.setCodeCompletion(codeCompletionCombo.getValue());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettings());
    }

    public void apply() {
        BuildConsoleSettings current = getCurrentSettings();
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

    public CheckBox getAlwaysShowDebugConsoleCheck() { return alwaysShowDebugConsoleCheck; }
    public CheckBox getUseIPythonCheck() { return useIPythonCheck; }
    public CheckBox getShowConsoleVariablesCheck() { return showConsoleVariablesCheck; }
    public CheckBox getUseExistingConsoleCheck() { return useExistingConsoleCheck; }
    public CheckBox getCommandQueueCheck() { return commandQueueCheck; }
    public ComboBox<String> getCodeCompletionCombo() { return codeCompletionCombo; }
}
