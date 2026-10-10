package dev.lumina.ui;

import dev.lumina.tools.RuboCopSettings;
import dev.lumina.tools.RuboCopSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Objects;

/**
 * Settings UI page for Tools > RuboCop in Lumina IDE.
 */
public class SettingsToolsRuboCopPage extends VBox {

    private final TextField configFileField;
    private final CheckBox useStandardGemCheck;
    private final CheckBox runRuboCopOnSaveCheck;

    private Runnable onModified;
    private boolean suppressEvents = false;

    public SettingsToolsRuboCopPage() {
        setSpacing(12);
        setPadding(new Insets(16, 20, 20, 20));
        setStyle("-fx-background-color: #1E1F22;");

        // Row 1: Config file:
        Label configLabel = new Label("Config file:");
        configLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 90px;");

        configFileField = new TextField();
        configFileField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 8;");
        HBox.setHgrow(configFileField, Priority.ALWAYS);

        Button browseBtn = new Button("📁");
        browseBtn.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 4 8; -fx-cursor: hand;");
        browseBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select RuboCop Configuration File");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("YAML files (*.yml, *.yaml)", "*.yml", "*.yaml"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            File f = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (f != null) {
                configFileField.setText(f.getAbsolutePath());
                notifyModified();
            }
        });

        HBox configRow = new HBox(10, configLabel, configFileField, browseBtn);
        configRow.setAlignment(Pos.CENTER_LEFT);

        // Row 2: Use the 'standard' gem
        useStandardGemCheck = new CheckBox("Use the 'standard' gem");
        useStandardGemCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        Hyperlink standardLink = new Hyperlink("standard ↗");
        standardLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-underline: false; -fx-border-color: transparent; -fx-padding: 0;");
        standardLink.setOnMouseEntered(e -> standardLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-underline: true; -fx-border-color: transparent; -fx-padding: 0;"));
        standardLink.setOnMouseExited(e -> standardLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-underline: false; -fx-border-color: transparent; -fx-padding: 0;"));
        standardLink.setOnAction(e -> showInfoAlert("Standard Ruby Linter", "Standard is an opinionated Ruby style guide and linter wrapper for RuboCop."));

        Label prefixLabel1 = new Label("Enable linting with ");
        prefixLabel1.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");
        Label suffixLabel1 = new Label(" .");
        suffixLabel1.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");

        HBox standardSubRow = new HBox(prefixLabel1, standardLink, suffixLabel1);
        standardSubRow.setAlignment(Pos.CENTER_LEFT);
        standardSubRow.setPadding(new Insets(0, 0, 8, 22));

        // Row 3: Run 'rubocop -a' on save
        runRuboCopOnSaveCheck = new CheckBox("Run 'rubocop -a' on save");
        runRuboCopOnSaveCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        Hyperlink autocorrectLink = new Hyperlink("safe autocorrect mode ↗");
        autocorrectLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-underline: false; -fx-border-color: transparent; -fx-padding: 0;");
        autocorrectLink.setOnMouseEntered(e -> autocorrectLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-underline: true; -fx-border-color: transparent; -fx-padding: 0;"));
        autocorrectLink.setOnMouseExited(e -> autocorrectLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-underline: false; -fx-border-color: transparent; -fx-padding: 0;"));
        autocorrectLink.setOnAction(e -> showInfoAlert("Safe Autocorrect Mode", "Executes RuboCop with '-a' on file save to apply safe non-breaking autocorrects."));

        Label prefixLabel2 = new Label("Run RuboCop in ");
        prefixLabel2.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");
        Label suffixLabel2 = new Label(" on save.");
        suffixLabel2.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");

        HBox autocorrectSubRow = new HBox(prefixLabel2, autocorrectLink, suffixLabel2);
        autocorrectSubRow.setAlignment(Pos.CENTER_LEFT);
        autocorrectSubRow.setPadding(new Insets(0, 0, 0, 22));

        getChildren().addAll(
                configRow,
                useStandardGemCheck,
                standardSubRow,
                runRuboCopOnSaveCheck,
                autocorrectSubRow
        );

        setupListeners();
        loadSettings();
    }

    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void setupListeners() {
        configFileField.textProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        useStandardGemCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        runRuboCopOnSaveCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());
    }

    public void setOnModified(Runnable onModified) {
        this.onModified = onModified;
    }

    private void notifyModified() {
        if (!suppressEvents && onModified != null) {
            onModified.run();
        }
    }

    public void loadSettings() {
        suppressEvents = true;
        try {
            RuboCopSettings s = RuboCopSettingsManager.getInstance().getSettings();
            configFileField.setText(s.getConfigFile());
            useStandardGemCheck.setSelected(s.isUseStandardGem());
            runRuboCopOnSaveCheck.setSelected(s.isRunRuboCopOnSave());
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        RuboCopSettings current = RuboCopSettingsManager.getInstance().getSettings();
        return !Objects.equals(configFileField.getText().trim(), current.getConfigFile()) ||
                useStandardGemCheck.isSelected() != current.isUseStandardGem() ||
                runRuboCopOnSaveCheck.isSelected() != current.isRunRuboCopOnSave();
    }

    public void apply() {
        RuboCopSettings s = new RuboCopSettings();
        s.setConfigFile(configFileField.getText().trim());
        s.setUseStandardGem(useStandardGemCheck.isSelected());
        s.setRunRuboCopOnSave(runRuboCopOnSaveCheck.isSelected());
        RuboCopSettingsManager.getInstance().setSettings(s);
    }

    public void reset() {
        loadSettings();
    }

    public void revertChanges() {
        reset();
    }

    public TextField getConfigFileField() {
        return configFileField;
    }

    public CheckBox getUseStandardGemCheck() {
        return useStandardGemCheck;
    }

    public CheckBox getRunRuboCopOnSaveCheck() {
        return runRuboCopOnSaveCheck;
    }
}
