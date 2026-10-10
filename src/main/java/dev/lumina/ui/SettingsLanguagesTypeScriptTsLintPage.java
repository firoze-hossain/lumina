package dev.lumina.ui;

import dev.lumina.typescript.TsLintSettings;
import dev.lumina.typescript.TsLintSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Objects;

/**
 * Settings page for Languages & Frameworks > TypeScript > TSLint.
 * Matches reference screenshot media_1791602885285_4debafea.png:
 *  - (•) Disable TSLint
 *  - ( ) Automatic TSLint configuration (?)
 *  - ( ) Manual TSLint configuration
 */
public class SettingsLanguagesTypeScriptTsLintPage extends VBox {

    private final TsLintSettingsManager manager = TsLintSettingsManager.getInstance();

    private final ToggleGroup modeGroup = new ToggleGroup();
    private final RadioButton disableRadio = new RadioButton("Disable TSLint");
    private final RadioButton autoRadio = new RadioButton("Automatic TSLint configuration");
    private final Label autoHelpLabel = new Label("?");
    private final RadioButton manualRadio = new RadioButton("Manual TSLint configuration");

    private final VBox manualConfigBox = new VBox(10);
    private final TextField packageField = new TextField();
    private final Button browsePackageBtn = new Button("...");
    private final TextField configFileField = new TextField();
    private final Button browseConfigBtn = new Button("...");
    private final TextField rulesDirField = new TextField();
    private final Button browseRulesBtn = new Button("...");

    private TsLintSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsLanguagesTypeScriptTsLintPage() {
        setSpacing(12);
        setPadding(new Insets(16, 24, 24, 24));
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadData();
    }

    private void buildUI() {
        // --- 1. Radio Options ---
        disableRadio.setToggleGroup(modeGroup);
        disableRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        disableRadio.selectedProperty().addListener((obs, o, n) -> {
            updateManualVisibility();
            notifyModified();
        });

        HBox autoRow = new HBox(6);
        autoRow.setAlignment(Pos.CENTER_LEFT);
        autoRadio.setToggleGroup(modeGroup);
        autoRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        autoRadio.selectedProperty().addListener((obs, o, n) -> {
            updateManualVisibility();
            notifyModified();
        });

        autoHelpLabel.setStyle(
                "-fx-text-fill: #6F737A; -fx-font-size: 11px; -fx-border-color: #4E5157; " +
                "-fx-border-radius: 10; -fx-padding: 0 5 0 5; -fx-cursor: hand;"
        );
        autoHelpLabel.setTooltip(new Tooltip(
                "When enabled, Lumina automatically searches for tslint.json in the current file directory and its parent directories."
        ));
        autoRow.getChildren().addAll(autoRadio, autoHelpLabel);

        manualRadio.setToggleGroup(modeGroup);
        manualRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        manualRadio.selectedProperty().addListener((obs, o, n) -> {
            updateManualVisibility();
            notifyModified();
        });

        // --- 2. Manual Configuration Box ---
        manualConfigBox.setPadding(new Insets(6, 0, 0, 22));
        manualConfigBox.setSpacing(10);

        // Package row
        Label pkgLabel = new Label("TSLint package:");
        pkgLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        pkgLabel.setPrefWidth(140);
        styleTextField(packageField);
        styleBrowseBtn(browsePackageBtn);
        browsePackageBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select TSLint Package Directory");
            File d = dc.showDialog(getScene() != null ? getScene().getWindow() : null);
            if (d != null) {
                packageField.setText(d.getAbsolutePath());
                notifyModified();
            }
        });
        HBox pkgRow = new HBox(8, pkgLabel, packageField, browsePackageBtn);
        pkgRow.setAlignment(Pos.CENTER_LEFT);

        // Config file row
        Label cfgLabel = new Label("Configuration file:");
        cfgLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        cfgLabel.setPrefWidth(140);
        styleTextField(configFileField);
        styleBrowseBtn(browseConfigBtn);
        browseConfigBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select TSLint Configuration File");
            File f = fc.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (f != null) {
                configFileField.setText(f.getAbsolutePath());
                notifyModified();
            }
        });
        HBox cfgRow = new HBox(8, cfgLabel, configFileField, browseConfigBtn);
        cfgRow.setAlignment(Pos.CENTER_LEFT);

        // Rules directory row
        Label rulesLabel = new Label("Rules directory:");
        rulesLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        rulesLabel.setPrefWidth(140);
        styleTextField(rulesDirField);
        styleBrowseBtn(browseRulesBtn);
        browseRulesBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Rules Directory");
            File d = dc.showDialog(getScene() != null ? getScene().getWindow() : null);
            if (d != null) {
                rulesDirField.setText(d.getAbsolutePath());
                notifyModified();
            }
        });
        HBox rulesRow = new HBox(8, rulesLabel, rulesDirField, browseRulesBtn);
        rulesRow.setAlignment(Pos.CENTER_LEFT);

        manualConfigBox.getChildren().addAll(pkgRow, cfgRow, rulesRow);

        getChildren().addAll(disableRadio, autoRow, manualRadio, manualConfigBox);
        updateManualVisibility();
    }

    private void styleTextField(TextField tf) {
        tf.setStyle(
                "-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; " +
                "-fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px;"
        );
        tf.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tf, Priority.ALWAYS);
        tf.textProperty().addListener((obs, o, n) -> notifyModified());
    }

    private void styleBrowseBtn(Button btn) {
        btn.setStyle(
                "-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; " +
                "-fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 4 10 4 10;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #35373B; -fx-border-color: #5A5D63; -fx-border-radius: 4; " +
                "-fx-background-radius: 4; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-padding: 4 10 4 10;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; " +
                "-fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 4 10 4 10;"
        ));
    }

    private void updateManualVisibility() {
        boolean isManual = manualRadio.isSelected();
        manualConfigBox.setVisible(isManual);
        manualConfigBox.setManaged(isManual);
    }

    public void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();
            applySettingsToUI(initialSettings);
        } finally {
            updating = false;
        }
    }

    private void applySettingsToUI(TsLintSettings s) {
        if (s == null) return;
        if (TsLintSettings.MODE_AUTOMATIC.equalsIgnoreCase(s.getMode())) {
            autoRadio.setSelected(true);
        } else if (TsLintSettings.MODE_MANUAL.equalsIgnoreCase(s.getMode())) {
            manualRadio.setSelected(true);
        } else {
            disableRadio.setSelected(true);
        }
        packageField.setText(s.getTslintPackage());
        configFileField.setText(s.getConfigFile());
        rulesDirField.setText(s.getRulesDirectory());
        updateManualVisibility();
    }

    public TsLintSettings getCurrentSettingsFromUI() {
        TsLintSettings s = new TsLintSettings();
        if (autoRadio.isSelected()) {
            s.setMode(TsLintSettings.MODE_AUTOMATIC);
        } else if (manualRadio.isSelected()) {
            s.setMode(TsLintSettings.MODE_MANUAL);
        } else {
            s.setMode(TsLintSettings.MODE_DISABLE);
        }
        s.setTslintPackage(packageField.getText() != null ? packageField.getText() : "");
        s.setConfigFile(configFileField.getText() != null ? configFileField.getText() : "");
        s.setRulesDirectory(rulesDirField.getText() != null ? rulesDirField.getText() : "");
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        TsLintSettings updated = getCurrentSettingsFromUI();
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

    // Getters for testing and inspection
    public RadioButton getDisableRadio() {
        return disableRadio;
    }

    public RadioButton getAutoRadio() {
        return autoRadio;
    }

    public RadioButton getManualRadio() {
        return manualRadio;
    }

    public TextField getPackageField() {
        return packageField;
    }

    public TextField getConfigFileField() {
        return configFileField;
    }

    public TextField getRulesDirField() {
        return rulesDirField;
    }

    public VBox getManualConfigBox() {
        return manualConfigBox;
    }
}
