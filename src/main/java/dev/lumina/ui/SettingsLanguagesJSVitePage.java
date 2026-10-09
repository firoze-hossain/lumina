package dev.lumina.ui;

import dev.lumina.javascript.JavaScriptSettingsManager;
import dev.lumina.javascript.ViteSettings;
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
 * Languages & Frameworks > JavaScript > Vite settings page in Lumina IDE.
 */
public class SettingsLanguagesJSVitePage extends VBox {

    private final JavaScriptSettingsManager manager = JavaScriptSettingsManager.getInstance();
    private Runnable onModifiedListener;

    private RadioButton disabledRadio;
    private RadioButton automaticRadio;
    private RadioButton manualRadio;
    private ToggleGroup modeGroup;

    private HBox manualRow;
    private TextField configFileField;
    private Button browseButton;

    private ViteSettings initialSettings;

    public SettingsLanguagesJSVitePage() {
        setSpacing(14);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        ViteSettings current = manager.getViteSettings();

        Label titleLabel = new Label("Detect Vite configuration files for module resolution:");
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
        autoHelp.setTooltip(new Tooltip("Detects Vite configuration files (vite.config.js, vite.config.ts, etc.) automatically for module resolution."));
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
            chooser.setTitle("Select Vite Configuration File");
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

    private ViteSettings getFormSettings() {
        ViteSettings.Mode mode = ViteSettings.Mode.AUTOMATIC;
        if (disabledRadio.isSelected()) {
            mode = ViteSettings.Mode.DISABLED;
        } else if (manualRadio.isSelected()) {
            mode = ViteSettings.Mode.MANUAL;
        }
        return new ViteSettings(mode, configFileField.getText() != null ? configFileField.getText() : "");
    }

    public boolean isModified() {
        return !Objects.equals(getFormSettings(), initialSettings);
    }

    public void apply() {
        manager.setViteSettings(getFormSettings());
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
