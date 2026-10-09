package dev.lumina.ui;

import dev.lumina.php.PhpQualityToolsSettings;
import dev.lumina.php.PhpQualityToolsSettings.PhpCsFixerConfig;
import dev.lumina.php.PhpSettingsManager;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Settings page for Languages & Frameworks > PHP > Quality Tools > PHP CS Fixer.
 * Faithfully matches Image 3.
 */
public class SettingsLanguagesPhpQualityToolsCsFixerPage extends VBox {

    private final PhpSettingsManager manager = PhpSettingsManager.getInstance();

    private Button inspectionToggleBtn;
    private ComboBox<String> configurationCombo;
    private Button configureBtn;
    private Hyperlink showIgnoredFilesLink;

    private CheckBox allowRiskyRulesCheck;
    private ComboBox<String> rulesetCombo;

    private HBox errorBubbleBox;
    private Label errorLabel;

    private boolean inspectionEnabled = false;
    private String phpCsFixerPath = "";

    private PhpCsFixerConfig initialConfig = new PhpCsFixerConfig();
    private Runnable onModified;

    public SettingsLanguagesPhpQualityToolsCsFixerPage() {
        setSpacing(14);
        setPadding(new Insets(16, 20, 20, 20));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadFromManager();
    }

    public void setOnModified(Runnable onModified) {
        this.onModified = onModified;
    }

    private void notifyModified() {
        updateErrorBubble();
        if (onModified != null) {
            onModified.run();
        }
    }

    private void buildUI() {
        // Top Inspection Row
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label inspLabel = new Label("PHP CS Fixer inspection:");
        inspLabel.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px;");

        inspectionToggleBtn = new Button("OFF");
        inspectionToggleBtn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #8C9099; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 10; -fx-cursor: hand;");
        inspectionToggleBtn.setOnAction(e -> {
            inspectionEnabled = !inspectionEnabled;
            updateInspectionToggleBtn();
            notifyModified();
        });

        Label configLabel = new Label("Configuration:");
        configLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 0 0 0 10;");

        configurationCombo = new ComboBox<>(FXCollections.observableArrayList("System PHP", "Local", "Custom"));
        configurationCombo.setValue("System PHP");
        configurationCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-font-size: 12px;");
        configurationCombo.valueProperty().addListener((obs, ov, nv) -> notifyModified());

        configureBtn = new Button("...");
        configureBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-padding: 2 8; -fx-cursor: hand;");
        configureBtn.setOnAction(e -> showConfigurationDialog());

        showIgnoredFilesLink = new Hyperlink("Show ignored files");
        showIgnoredFilesLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0 0 0 10;");

        topRow.getChildren().addAll(inspLabel, inspectionToggleBtn, configLabel, configurationCombo, configureBtn, showIgnoredFilesLink);

        // Options Section
        HBox optionsHeader = createSectionHeader("Options");

        allowRiskyRulesCheck = new CheckBox("Allow risky rules for built-in rulesets");
        allowRiskyRulesCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        allowRiskyRulesCheck.selectedProperty().addListener((obs, ov, nv) -> notifyModified());

        HBox rulesetRow = new HBox(12);
        rulesetRow.setAlignment(Pos.CENTER_LEFT);

        Label rulesetLabel = new Label("Ruleset:");
        rulesetLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        rulesetLabel.setPrefWidth(60);

        rulesetCombo = new ComboBox<>(FXCollections.observableArrayList("PSR2", "PSR1", "PSR12", "Symfony", "PhpCsFixer", "PER", "Custom"));
        rulesetCombo.setValue("PSR2");
        rulesetCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-font-size: 12px;");
        rulesetCombo.setPrefWidth(120);
        rulesetCombo.valueProperty().addListener((obs, ov, nv) -> notifyModified());

        rulesetRow.getChildren().addAll(rulesetLabel, rulesetCombo);

        VBox optionsBox = new VBox(10, allowRiskyRulesCheck, rulesetRow);
        optionsBox.setPadding(new Insets(4, 0, 8, 16));

        // Error bubble
        errorBubbleBox = new HBox(8);
        errorBubbleBox.setAlignment(Pos.CENTER_LEFT);
        errorBubbleBox.setPadding(new Insets(6, 12, 6, 12));
        errorBubbleBox.setStyle("-fx-background-color: #432426; -fx-border-color: #7A3338; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label errIcon = new Label("⛔");
        errIcon.setStyle("-fx-text-fill: #F75F69; -fx-font-size: 13px;");

        errorLabel = new Label("PHP CS Fixer path is empty for selected configuration");
        errorLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        errorBubbleBox.getChildren().addAll(errIcon, errorLabel);

        getChildren().addAll(topRow, optionsHeader, optionsBox, errorBubbleBox);
    }

    private void updateInspectionToggleBtn() {
        if (inspectionEnabled) {
            inspectionToggleBtn.setText("ON");
            inspectionToggleBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 10; -fx-cursor: hand;");
        } else {
            inspectionToggleBtn.setText("OFF");
            inspectionToggleBtn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #8C9099; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 10; -fx-cursor: hand;");
        }
    }

    private void updateErrorBubble() {
        boolean empty = phpCsFixerPath.isBlank();
        errorBubbleBox.setVisible(empty);
        errorBubbleBox.setManaged(empty);
    }

    private void showConfigurationDialog() {
        Stage dlgStage = new Stage();
        dlgStage.setTitle("PHP CS Fixer by " + configurationCombo.getValue());
        dlgStage.initModality(Modality.APPLICATION_MODAL);
        if (getScene() != null && getScene().getWindow() != null) {
            dlgStage.initOwner(getScene().getWindow());
        }

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.setStyle("-fx-background-color: #1E1F22;");

        Label pathLabel = new Label("PHP CS Fixer path:");
        pathLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        TextField pathInput = new TextField(phpCsFixerPath);
        pathInput.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-font-size: 13px;");
        HBox.setHgrow(pathInput, Priority.ALWAYS);

        Button browseBtn = new Button("📁");
        browseBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-cursor: hand;");
        browseBtn.setOnAction(e -> {
            SelectPathDialog dlg = new SelectPathDialog(dlgStage, pathInput.getText());
            String chosen = dlg.showAndWait();
            if (chosen != null && !chosen.isBlank()) pathInput.setText(chosen);
        });

        HBox pathRow = new HBox(8, pathLabel, pathInput, browseBtn);
        pathRow.setAlignment(Pos.CENTER_LEFT);

        HBox btnRow = new HBox(8);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        Button okBtn = new Button("OK");
        okBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-padding: 4 14; -fx-cursor: hand; -fx-background-radius: 4;");
        okBtn.setOnAction(e -> {
            phpCsFixerPath = pathInput.getText().trim();
            dlgStage.close();
            notifyModified();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-padding: 4 14; -fx-cursor: hand; -fx-background-radius: 4;");
        cancelBtn.setOnAction(e -> dlgStage.close());

        btnRow.getChildren().addAll(okBtn, cancelBtn);

        content.getChildren().addAll(pathRow, btnRow);
        dlgStage.setScene(new javafx.scene.Scene(content, 480, 130));
        dlgStage.showAndWait();
    }

    private HBox createSectionHeader(String titleText) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);

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

    public void loadFromManager() {
        PhpCsFixerConfig cfg = manager.getQualityToolsSettings().getCsFixer();
        inspectionEnabled = cfg.isInspectionEnabled();
        updateInspectionToggleBtn();
        configurationCombo.setValue(cfg.getConfigurationName());
        phpCsFixerPath = cfg.getPhpCsFixerPath();
        allowRiskyRulesCheck.setSelected(cfg.isAllowRiskyRules());
        rulesetCombo.setValue(cfg.getRuleset());

        initialConfig = cfg.copy();
        updateErrorBubble();
    }

    private PhpCsFixerConfig buildCurrentConfig() {
        PhpCsFixerConfig cfg = new PhpCsFixerConfig();
        cfg.setInspectionEnabled(inspectionEnabled);
        cfg.setConfigurationName(configurationCombo.getValue());
        cfg.setPhpCsFixerPath(phpCsFixerPath);
        cfg.setAllowRiskyRules(allowRiskyRulesCheck.isSelected());
        cfg.setRuleset(rulesetCombo.getValue());
        return cfg;
    }

    public boolean isModified() {
        return !buildCurrentConfig().equals(initialConfig);
    }

    public void apply() {
        PhpQualityToolsSettings qs = manager.getQualityToolsSettings();
        PhpCsFixerConfig cfg = buildCurrentConfig();
        qs.setCsFixer(cfg);
        manager.setQualityToolsSettings(qs);
        initialConfig = cfg.copy();
    }

    public void reset() {
        loadFromManager();
    }

    public void revertChanges() {
        reset();
    }
}
