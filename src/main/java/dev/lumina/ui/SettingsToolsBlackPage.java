package dev.lumina.ui;

import dev.lumina.tools.BlackSettings;
import dev.lumina.tools.BlackSettingsManager;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Black code formatter settings page in Lumina IDE matching 1:1 design of the reference IDE.
 */
public class SettingsToolsBlackPage extends VBox {

    private final BlackSettingsManager manager;
    private BlackSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    private ComboBox<String> executionModeCombo;
    private ComboBox<String> pythonInterpreterCombo;
    private CheckBox onCodeReformatCheck;
    private CheckBox onSaveCheck;
    private TextField argumentsField;

    private Consumer<String> onNavigate;

    public SettingsToolsBlackPage() {
        this(null);
    }

    public SettingsToolsBlackPage(Consumer<String> onNavigate) {
        this.manager = BlackSettingsManager.getInstance();
        this.onNavigate = onNavigate;
        buildUI();
        loadData();
    }

    public void setOnNavigate(Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;
    }

    private void buildUI() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(16);
        setStyle("-fx-background-color: #1E1F22;");

        final double labelWidth = 140;

        // 1. Execution mode: Package (?)
        HBox modeRow = new HBox(8);
        modeRow.setAlignment(Pos.CENTER_LEFT);

        Label modeLabel = new Label("Execution mode:");
        modeLabel.setMinWidth(labelWidth);
        modeLabel.setPrefWidth(labelWidth);
        modeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        executionModeCombo = new ComboBox<>();
        executionModeCombo.getItems().addAll(BlackSettings.MODE_PACKAGE, BlackSettings.MODE_BINARY);
        executionModeCombo.setValue(BlackSettings.MODE_PACKAGE);
        executionModeCombo.setPrefWidth(130);
        executionModeCombo.setStyle(
                "-fx-background-color: #2B2D30; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-radius: 4px; " +
                "-fx-background-radius: 4px; " +
                "-fx-font-size: 13px;"
        );
        executionModeCombo.valueProperty().addListener((obs, oldVal, newVal) -> notifyModified());

        Button helpBtn = new Button("?");
        helpBtn.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-border-color: #5A5D63; " +
                "-fx-border-radius: 10px; " +
                "-fx-background-radius: 10px; " +
                "-fx-text-fill: #8C8C8C; " +
                "-fx-font-size: 11px; " +
                "-fx-padding: 0 5 0 5; " +
                "-fx-min-width: 18px; " +
                "-fx-min-height: 18px;"
        );
        helpBtn.setTooltip(new Tooltip("Specify whether Black should be invoked as a module inside the Python interpreter package or directly via a binary executable."));

        modeRow.getChildren().addAll(modeLabel, executionModeCombo, helpBtn);

        // 2. Python interpreter: <No interpreter>
        HBox interpRow = new HBox(8);
        interpRow.setAlignment(Pos.CENTER_LEFT);

        Label interpLabel = new Label("Python interpreter:");
        interpLabel.setMinWidth(labelWidth);
        interpLabel.setPrefWidth(labelWidth);
        interpLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        pythonInterpreterCombo = new ComboBox<>();
        List<String> discovered = manager.discoverPythonInterpreters();
        pythonInterpreterCombo.getItems().setAll(discovered);
        pythonInterpreterCombo.setValue(BlackSettings.NO_INTERPRETER);
        pythonInterpreterCombo.setPrefWidth(550);
        pythonInterpreterCombo.setStyle(
                "-fx-background-color: #2B2D30; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-radius: 4px; " +
                "-fx-background-radius: 4px; " +
                "-fx-font-size: 13px;"
        );
        pythonInterpreterCombo.valueProperty().addListener((obs, oldVal, newVal) -> notifyModified());

        interpRow.getChildren().addAll(interpLabel, pythonInterpreterCombo);

        // 3. Use Black formatter: [ ] On code reformat, [ ] On save
        HBox useRow = new HBox(8);
        useRow.setAlignment(Pos.TOP_LEFT);

        Label useLabel = new Label("Use Black formatter:");
        useLabel.setMinWidth(labelWidth);
        useLabel.setPrefWidth(labelWidth);
        useLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 2 0 0 0;");

        VBox useOptionsBox = new VBox(8);
        useOptionsBox.setAlignment(Pos.TOP_LEFT);

        HBox reformatBox = new HBox(12);
        reformatBox.setAlignment(Pos.CENTER_LEFT);
        onCodeReformatCheck = new CheckBox("On code reformat");
        onCodeReformatCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        onCodeReformatCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());

        Label shortcutBadge = new Label("⌥⌘L");
        shortcutBadge.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 12px;");
        reformatBox.getChildren().addAll(onCodeReformatCheck, shortcutBadge);

        HBox saveBox = new HBox(8);
        saveBox.setAlignment(Pos.CENTER_LEFT);
        onSaveCheck = new CheckBox("On save");
        onSaveCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        onSaveCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());

        Hyperlink actionsOnSaveLink = new Hyperlink("All actions on save...");
        actionsOnSaveLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 0; -fx-underline: false;");
        actionsOnSaveLink.setOnMouseEntered(e -> actionsOnSaveLink.setStyle("-fx-text-fill: #70AAFF; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 0; -fx-underline: true;"));
        actionsOnSaveLink.setOnMouseExited(e -> actionsOnSaveLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 0; -fx-underline: false;"));
        actionsOnSaveLink.setOnAction(e -> {
            if (onNavigate != null) {
                onNavigate.accept("Actions on Save");
            }
        });
        saveBox.getChildren().addAll(onSaveCheck, actionsOnSaveLink);

        useOptionsBox.getChildren().addAll(reformatBox, saveBox);
        useRow.getChildren().addAll(useLabel, useOptionsBox);

        // 4. Settings: text field & documentation hints
        HBox settingsRow = new HBox(8);
        settingsRow.setAlignment(Pos.TOP_LEFT);

        Label settingsLabel = new Label("Settings:");
        settingsLabel.setMinWidth(labelWidth);
        settingsLabel.setPrefWidth(labelWidth);
        settingsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 0 0 0;");

        VBox settingsBox = new VBox(6);
        settingsBox.setAlignment(Pos.TOP_LEFT);

        argumentsField = new TextField();
        argumentsField.setPrefWidth(550);
        argumentsField.setStyle(
                "-fx-background-color: #2B2D30; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-radius: 4px; " +
                "-fx-background-radius: 4px; " +
                "-fx-text-fill: #DFE1E5; " +
                "-fx-font-size: 13px; " +
                "-fx-padding: 4 8 4 8;"
        );
        argumentsField.textProperty().addListener((obs, oldVal, newVal) -> notifyModified());

        HBox learnMoreBox = new HBox(2);
        learnMoreBox.setAlignment(Pos.CENTER_LEFT);
        Label descPart1 = new Label("List command line arguments separated by whitespace.");
        descPart1.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 12px;");

        Hyperlink learnMoreLink = new Hyperlink("Learn more ↗");
        learnMoreLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0 0 0 4; -fx-underline: false;");
        learnMoreLink.setOnMouseEntered(e -> learnMoreLink.setStyle("-fx-text-fill: #70AAFF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0 0 0 4; -fx-underline: true;"));
        learnMoreLink.setOnMouseExited(e -> learnMoreLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0 0 0 4; -fx-underline: false;"));
        learnMoreBox.getChildren().addAll(descPart1, learnMoreLink);

        Label descPart2 = new Label("Settings from pyproject.toml are applied automatically for Black v21.4.0 and higher.");
        descPart2.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 12px;");

        settingsBox.getChildren().addAll(argumentsField, learnMoreBox, descPart2);
        settingsRow.getChildren().addAll(settingsLabel, settingsBox);

        getChildren().addAll(modeRow, interpRow, useRow, settingsRow);
    }

    private void loadData() {
        updating = true;
        BlackSettings s = manager.getSettings();
        executionModeCombo.setValue(s.getExecutionMode());
        if (!pythonInterpreterCombo.getItems().contains(s.getPythonInterpreter())) {
            pythonInterpreterCombo.getItems().add(s.getPythonInterpreter());
        }
        pythonInterpreterCombo.setValue(s.getPythonInterpreter());
        onCodeReformatCheck.setSelected(s.isOnCodeReformat());
        onSaveCheck.setSelected(s.isOnSave());
        argumentsField.setText(s.getArguments());
        initialSettings = getCurrentSettingsFromUI();
        updating = false;
    }

    public BlackSettings getCurrentSettingsFromUI() {
        BlackSettings s = new BlackSettings();
        s.setExecutionMode(executionModeCombo.getValue());
        s.setPythonInterpreter(pythonInterpreterCombo.getValue());
        s.setOnCodeReformat(onCodeReformatCheck.isSelected());
        s.setOnSave(onSaveCheck.isSelected());
        s.setArguments(argumentsField.getText());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        BlackSettings updated = getCurrentSettingsFromUI();
        manager.setSettings(updated);
        initialSettings = updated.clone();
        notifyModified();
    }

    public void reset() {
        loadData();
        notifyModified();
    }

    public void revertChanges() {
        reset();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public ComboBox<String> getExecutionModeCombo() {
        return executionModeCombo;
    }

    public ComboBox<String> getPythonInterpreterCombo() {
        return pythonInterpreterCombo;
    }

    public CheckBox getOnCodeReformatCheck() {
        return onCodeReformatCheck;
    }

    public CheckBox getOnSaveCheck() {
        return onSaveCheck;
    }

    public TextField getArgumentsField() {
        return argumentsField;
    }
}
