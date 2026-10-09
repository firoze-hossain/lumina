package dev.lumina.ui;

import dev.lumina.javascript.JavaScriptSettingsManager;
import dev.lumina.javascript.WebpackSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Objects;

/**
 * Languages & Frameworks > JavaScript > Webpack settings page in Lumina IDE.
 */
public class SettingsLanguagesJSWebpackPage extends VBox {

    private final JavaScriptSettingsManager manager = JavaScriptSettingsManager.getInstance();
    private Runnable onModifiedListener;

    private RadioButton disabledRadio;
    private RadioButton automaticRadio;
    private RadioButton manualRadio;
    private ToggleGroup modeGroup;

    private HBox manualRow;
    private TextField configFileField;
    private Button browseButton;

    private WebpackSettings initialSettings;

    public SettingsLanguagesJSWebpackPage() {
        setSpacing(14);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        WebpackSettings current = manager.getWebpackSettings();

        Label titleLabel = new Label("Detect Webpack configuration files for module resolution:");
        titleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        modeGroup = new ToggleGroup();

        disabledRadio = new RadioButton("Disabled");
        disabledRadio.setToggleGroup(modeGroup);
        disabledRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");

        automaticRadio = new RadioButton("Automatically");
        automaticRadio.setToggleGroup(modeGroup);
        automaticRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");

        Label autoHelp = new Label("\u24D8"); // circled info icon
        autoHelp.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 13px; -fx-cursor: hand;");
        autoHelp.setTooltip(new Tooltip("Detects Webpack configuration files (webpack.config.js, etc.) automatically for module resolution."));
        HBox autoBox = new HBox(6, automaticRadio, autoHelp);
        autoBox.setAlignment(Pos.CENTER_LEFT);

        manualRadio = new RadioButton("Manually");
        manualRadio.setToggleGroup(modeGroup);
        manualRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");

        switch (current.getMode()) {
            case DISABLED -> disabledRadio.setSelected(true);
            case AUTOMATIC -> automaticRadio.setSelected(true);
            case MANUAL -> manualRadio.setSelected(true);
        }

        modeGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            updateControlStates();
            fireModified();
        });

        // Manual configuration row
        manualRow = new HBox(10);
        manualRow.setAlignment(Pos.CENTER_LEFT);
        manualRow.setPadding(new Insets(4, 0, 0, 24));

        Label configLabel = new Label("Configuration file:");
        configLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        configLabel.setPrefWidth(120);

        configFileField = new TextField(current.getConfigurationFile());
        configFileField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px;");
        configFileField.setPrefWidth(420);
        configFileField.textProperty().addListener((obs, oldV, newV) -> fireModified());

        browseButton = new Button("...");
        browseButton.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px; -fx-cursor: hand;");
        browseButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Webpack Configuration File");
            File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (file != null) {
                configFileField.setText(file.getAbsolutePath());
            }
        });

        manualRow.getChildren().addAll(configLabel, configFileField, browseButton);

        getChildren().addAll(
                titleLabel,
                disabledRadio,
                autoBox,
                manualRadio,
                manualRow
        );

        updateControlStates();
    }

    private void updateControlStates() {
        boolean isManual = manualRadio.isSelected();
        manualRow.setVisible(isManual);
        manualRow.setManaged(isManual);
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

    private WebpackSettings getFormSettings() {
        WebpackSettings.Mode mode = WebpackSettings.Mode.AUTOMATIC;
        if (disabledRadio.isSelected()) {
            mode = WebpackSettings.Mode.DISABLED;
        } else if (manualRadio.isSelected()) {
            mode = WebpackSettings.Mode.MANUAL;
        }
        return new WebpackSettings(mode, configFileField.getText() != null ? configFileField.getText() : "");
    }

    public boolean isModified() {
        return !Objects.equals(getFormSettings(), initialSettings);
    }

    public void apply() {
        manager.setWebpackSettings(getFormSettings());
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        if (initialSettings != null) {
            switch (initialSettings.getMode()) {
                case DISABLED -> disabledRadio.setSelected(true);
                case AUTOMATIC -> automaticRadio.setSelected(true);
                case MANUAL -> manualRadio.setSelected(true);
            }
            configFileField.setText(initialSettings.getConfigurationFile());
            updateControlStates();
        }
        fireModified();
    }

    public void revertChanges() {
        reset();
    }

    public RadioButton getDisabledRadio() {
        return disabledRadio;
    }

    public RadioButton getAutomaticRadio() {
        return automaticRadio;
    }

    public RadioButton getManualRadio() {
        return manualRadio;
    }

    public TextField getConfigFileField() {
        return configFileField;
    }
}
