package dev.lumina.ui;

import dev.lumina.jvm.JvmLoggingSettings;
import dev.lumina.jvm.JvmLoggingSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Languages & Frameworks > JVM Logging settings page in Lumina IDE.
 */
public class SettingsLanguagesJvmLoggingPage extends VBox {

    private final JvmLoggingSettingsManager manager = JvmLoggingSettingsManager.getInstance();
    private Runnable onModifiedListener;

    private TextField variableNameField;
    private ComboBox<String> loggerCombo;

    private JvmLoggingSettings initialSettings;

    public SettingsLanguagesJvmLoggingPage() {
        setSpacing(12);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        JvmLoggingSettings current = manager.getSettings();

        // 1. Section header: Java
        HBox javaHeader = createSectionHeader("Java");

        // 2. Variable name row
        HBox varNameRow = new HBox(10);
        varNameRow.setAlignment(Pos.CENTER_LEFT);

        Label varNameLabel = new Label("Variable name:");
        varNameLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        varNameLabel.setPrefWidth(100);

        variableNameField = new TextField(current.getVariableName());
        variableNameField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px;");
        variableNameField.setPrefWidth(200);
        variableNameField.textProperty().addListener((obs, oldV, newV) -> fireModified());

        varNameRow.getChildren().addAll(varNameLabel, variableNameField);

        // 3. Logger row
        HBox loggerRow = new HBox(10);
        loggerRow.setAlignment(Pos.CENTER_LEFT);

        Label loggerLabel = new Label("Logger:");
        loggerLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        loggerLabel.setPrefWidth(100);

        loggerCombo = new ComboBox<>();
        loggerCombo.getItems().addAll(JvmLoggingSettings.AVAILABLE_LOGGERS);
        loggerCombo.setValue(current.getLogger());
        loggerCombo.setPrefWidth(200);
        loggerCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px;");
        loggerCombo.valueProperty().addListener((obs, oldV, newV) -> fireModified());

        loggerRow.getChildren().addAll(loggerLabel, loggerCombo);

        getChildren().addAll(javaHeader, varNameRow, loggerRow);
    }

    private HBox createSectionHeader(String titleText) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(0, 0, 4, 0));

        Label label = new Label(titleText);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setPrefHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: #393B40;");
        HBox.setHgrow(line, Priority.ALWAYS);

        box.getChildren().addAll(label, line);
        return box;
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

    private JvmLoggingSettings getFormSettings() {
        String varName = variableNameField.getText() != null ? variableNameField.getText().trim() : "";
        String logger = loggerCombo.getValue() != null ? loggerCombo.getValue() : JvmLoggingSettings.DEFAULT_LOGGER;
        return new JvmLoggingSettings(varName, logger);
    }

    public boolean isModified() {
        return !Objects.equals(getFormSettings(), initialSettings);
    }

    public void apply() {
        manager.setSettings(getFormSettings());
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        if (initialSettings != null) {
            variableNameField.setText(initialSettings.getVariableName());
            loggerCombo.setValue(initialSettings.getLogger());
        }
        fireModified();
    }

    public void revertChanges() {
        reset();
    }

    public TextField getVariableNameField() {
        return variableNameField;
    }

    public ComboBox<String> getLoggerCombo() {
        return loggerCombo;
    }
}
