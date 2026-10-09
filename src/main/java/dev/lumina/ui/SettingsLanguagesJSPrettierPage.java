package dev.lumina.ui;

import dev.lumina.javascript.JavaScriptSettingsManager;
import dev.lumina.javascript.PrettierSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Objects;

/**
 * Languages & Frameworks > JavaScript > Prettier settings page in Lumina IDE.
 */
public class SettingsLanguagesJSPrettierPage extends VBox {

    private final JavaScriptSettingsManager manager = JavaScriptSettingsManager.getInstance();
    private Runnable onModifiedListener;

    private RadioButton disableRadio;
    private RadioButton automaticRadio;
    private RadioButton manualRadio;
    private ToggleGroup modeGroup;

    // Manual configuration panel controls
    private VBox manualConfigPane;
    private ComboBox<String> prettierPackageCombo;
    private RadioButton autoSearchConfigRadio;
    private RadioButton customConfigFileRadio;
    private ToggleGroup configSourceGroup;
    private TextField customConfigFileField;
    private Button browseCustomConfigFileButton;

    // Common fields
    private TextField runForFilesField;
    private HBox globHintBox;
    private CheckBox runOnSaveCheck;
    private CheckBox runOnPasteCheck;
    private CheckBox preferPrettierCheck;

    private PrettierSettings initialSettings;

    public SettingsLanguagesJSPrettierPage() {
        setSpacing(14);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        PrettierSettings current = manager.getPrettierSettings();

        // 1. Radio modes
        modeGroup = new ToggleGroup();

        disableRadio = new RadioButton("Disable Prettier");
        disableRadio.setToggleGroup(modeGroup);
        disableRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");

        automaticRadio = new RadioButton("Automatic Prettier configuration");
        automaticRadio.setToggleGroup(modeGroup);
        automaticRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");

        Label autoHelp = new Label("\u24D8"); // circled info icon
        autoHelp.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 13px; -fx-cursor: hand;");
        autoHelp.setTooltip(new Tooltip("Detects Prettier package and configuration file automatically based on project structure."));
        HBox autoBox = new HBox(6, automaticRadio, autoHelp);
        autoBox.setAlignment(Pos.CENTER_LEFT);

        manualRadio = new RadioButton("Manual Prettier configuration");
        manualRadio.setToggleGroup(modeGroup);
        manualRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");

        switch (current.getMode()) {
            case DISABLED -> disableRadio.setSelected(true);
            case AUTOMATIC -> automaticRadio.setSelected(true);
            case MANUAL -> manualRadio.setSelected(true);
        }

        // 2. Manual Configuration Panel (dynamically shown when Manual radio is selected)
        buildManualConfigPane(current);

        modeGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            updateControlStates();
            fireModified();
        });

        // 3. Run for files row
        HBox runForFilesRow = new HBox(12);
        runForFilesRow.setAlignment(Pos.CENTER_LEFT);

        Label runForFilesLabel = new Label("Run for files:");
        runForFilesLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        runForFilesLabel.setPrefWidth(96);

        runForFilesField = new TextField(current.getRunForFiles());
        runForFilesField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px; -fx-padding: 5 8 5 8;");
        runForFilesField.setMaxWidth(620);
        HBox.setHgrow(runForFilesField, Priority.ALWAYS);
        runForFilesField.textProperty().addListener((obs, oldV, newV) -> fireModified());

        runForFilesRow.getChildren().addAll(runForFilesLabel, runForFilesField);

        // Glob hint with link
        globHintBox = new HBox(2);
        globHintBox.setAlignment(Pos.CENTER_LEFT);
        globHintBox.setPadding(new Insets(0, 0, 0, 108));

        Hyperlink globLink = new Hyperlink("Use a glob pattern \u2197");
        globLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0; -fx-underline: false;");
        globLink.setOnMouseEntered(e -> globLink.setStyle("-fx-text-fill: #70AAFF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0; -fx-underline: true;"));
        globLink.setOnMouseExited(e -> globLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0; -fx-underline: false;"));
        globLink.setTooltip(new Tooltip("Learn about glob syntax patterns for file matching"));

        Label globExampleLabel = new Label(" , for example, **/*.{js,ts}");
        globExampleLabel.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px;");

        globHintBox.getChildren().addAll(globLink, globExampleLabel);

        // 4. Checkboxes matching Screenshot 2
        runOnSaveCheck = new CheckBox("Run on save");
        runOnSaveCheck.setSelected(current.isRunOnSave());
        runOnSaveCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");
        runOnSaveCheck.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        runOnPasteCheck = new CheckBox("Run on paste");
        runOnPasteCheck.setSelected(current.isRunOnPaste());
        runOnPasteCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");
        runOnPasteCheck.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        preferPrettierCheck = new CheckBox("Prefer Prettier configuration to IDE code style");
        preferPrettierCheck.setSelected(current.isPreferPrettierToIdeCodeStyle());
        preferPrettierCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");
        preferPrettierCheck.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        getChildren().addAll(
                disableRadio,
                autoBox,
                manualRadio,
                manualConfigPane,
                runForFilesRow,
                globHintBox,
                runOnSaveCheck,
                runOnPasteCheck,
                preferPrettierCheck
        );

        updateControlStates();
    }

    private void buildManualConfigPane(PrettierSettings current) {
        manualConfigPane = new VBox(10);
        manualConfigPane.setPadding(new Insets(6, 0, 10, 24));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        // Prettier package
        Label packageLabel = new Label("Prettier package:");
        packageLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        packageLabel.setPrefWidth(130);

        prettierPackageCombo = new ComboBox<>();
        prettierPackageCombo.setEditable(true);
        prettierPackageCombo.getItems().addAll("node_modules/prettier", "/usr/local/lib/node_modules/prettier");
        prettierPackageCombo.setValue(current.getPrettierPackage());
        prettierPackageCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px;");
        prettierPackageCombo.setMaxWidth(460);
        prettierPackageCombo.valueProperty().addListener((obs, oldV, newV) -> fireModified());

        grid.add(packageLabel, 0, 0);
        grid.add(prettierPackageCombo, 1, 0);

        // Configuration file radio group
        Label configLabel = new Label("Configuration file:");
        configLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        configSourceGroup = new ToggleGroup();
        autoSearchConfigRadio = new RadioButton("Automatic search");
        autoSearchConfigRadio.setToggleGroup(configSourceGroup);
        autoSearchConfigRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");

        customConfigFileRadio = new RadioButton("Configuration file:");
        customConfigFileRadio.setToggleGroup(configSourceGroup);
        customConfigFileRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");

        if (current.isCustomConfigurationFile()) {
            customConfigFileRadio.setSelected(true);
        } else {
            autoSearchConfigRadio.setSelected(true);
        }

        configSourceGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            updateManualConfigControls();
            fireModified();
        });

        HBox customConfigRow = new HBox(8);
        customConfigRow.setAlignment(Pos.CENTER_LEFT);

        customConfigFileField = new TextField(current.getConfigurationFile());
        customConfigFileField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px;");
        customConfigFileField.setPrefWidth(380);
        customConfigFileField.textProperty().addListener((obs, oldV, newV) -> fireModified());

        browseCustomConfigFileButton = new Button("...");
        browseCustomConfigFileButton.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px; -fx-cursor: hand;");
        browseCustomConfigFileButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Prettier Configuration File");
            File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (file != null) {
                customConfigFileField.setText(file.getAbsolutePath());
                customConfigFileRadio.setSelected(true);
            }
        });

        customConfigRow.getChildren().addAll(customConfigFileRadio, customConfigFileField, browseCustomConfigFileButton);

        VBox configSourcesBox = new VBox(6, autoSearchConfigRadio, customConfigRow);
        grid.add(configLabel, 0, 1);
        grid.add(configSourcesBox, 1, 1);

        manualConfigPane.getChildren().add(grid);
    }

    private void updateControlStates() {
        boolean isDisabled = disableRadio.isSelected();
        boolean isManual = manualRadio.isSelected();

        // When Prettier is disabled, fields are disabled
        runForFilesField.setDisable(isDisabled);
        runOnSaveCheck.setDisable(isDisabled);
        runOnPasteCheck.setDisable(isDisabled);
        preferPrettierCheck.setDisable(isDisabled);
        globHintBox.setOpacity(isDisabled ? 0.5 : 1.0);

        // Manual configuration pane is shown only when Manual mode is active
        manualConfigPane.setVisible(isManual);
        manualConfigPane.setManaged(isManual);

        updateManualConfigControls();
    }

    private void updateManualConfigControls() {
        boolean customConfig = customConfigFileRadio.isSelected();
        customConfigFileField.setDisable(!customConfig);
        browseCustomConfigFileButton.setDisable(!customConfig);
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

    private PrettierSettings getFormSettings() {
        PrettierSettings.Mode mode = PrettierSettings.Mode.DISABLED;
        if (automaticRadio.isSelected()) {
            mode = PrettierSettings.Mode.AUTOMATIC;
        } else if (manualRadio.isSelected()) {
            mode = PrettierSettings.Mode.MANUAL;
        }

        PrettierSettings s = new PrettierSettings(mode, runForFilesField.getText(),
                runOnSaveCheck.isSelected(), runOnPasteCheck.isSelected(), preferPrettierCheck.isSelected());
        s.setPrettierPackage(prettierPackageCombo.getValue() != null ? prettierPackageCombo.getValue() : "node_modules/prettier");
        s.setCustomConfigurationFile(customConfigFileRadio.isSelected());
        s.setConfigurationFile(customConfigFileField.getText() != null ? customConfigFileField.getText() : "");
        return s;
    }

    public boolean isModified() {
        return !Objects.equals(getFormSettings(), initialSettings);
    }

    public void apply() {
        manager.setPrettierSettings(getFormSettings());
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
            runOnPasteCheck.setSelected(initialSettings.isRunOnPaste());
            preferPrettierCheck.setSelected(initialSettings.isPreferPrettierToIdeCodeStyle());

            prettierPackageCombo.setValue(initialSettings.getPrettierPackage());
            if (initialSettings.isCustomConfigurationFile()) {
                customConfigFileRadio.setSelected(true);
            } else {
                autoSearchConfigRadio.setSelected(true);
            }
            customConfigFileField.setText(initialSettings.getConfigurationFile());

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

    public CheckBox getRunOnPasteCheck() {
        return runOnPasteCheck;
    }

    public CheckBox getPreferPrettierCheck() {
        return preferPrettierCheck;
    }

    public ComboBox<String> getPrettierPackageCombo() {
        return prettierPackageCombo;
    }

    public TextField getCustomConfigFileField() {
        return customConfigFileField;
    }
}
