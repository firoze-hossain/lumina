package dev.lumina.ui;

import dev.lumina.php.PhpQualityToolsSettings;
import dev.lumina.php.PhpQualityToolsSettings.CustomRuleset;
import dev.lumina.php.PhpQualityToolsSettings.PhpMessDetectorConfig;
import dev.lumina.php.PhpSettingsManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings page for Languages & Frameworks > PHP > Quality Tools > Mess Detector.
 * Faithfully matches Image 5.
 */
public class SettingsLanguagesPhpQualityToolsMessDetectorPage extends VBox {

    private final PhpSettingsManager manager = PhpSettingsManager.getInstance();

    private Button inspectionToggleBtn;
    private ComboBox<String> configurationCombo;
    private Button configureBtn;
    private Hyperlink showIgnoredFilesLink;

    private CheckBox codeSizeRulesCheck;
    private CheckBox controversialRulesCheck;
    private CheckBox designRulesCheck;
    private CheckBox namingRulesCheck;
    private CheckBox unusedCodeRulesCheck;

    private final ObservableList<CustomRuleset> customRulesetsList = FXCollections.observableArrayList();
    private TableView<CustomRuleset> customRulesetsTable;

    private HBox errorBubbleBox;
    private Label errorLabel;

    private boolean inspectionEnabled = false;
    private String phpmdPath = "";

    private PhpMessDetectorConfig initialConfig = new PhpMessDetectorConfig();
    private Runnable onModified;

    public SettingsLanguagesPhpQualityToolsMessDetectorPage() {
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

        Label inspLabel = new Label("Mess Detector inspection:");
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

        // Standard Rulesets
        codeSizeRulesCheck = new CheckBox("Code Size Rules");
        codeSizeRulesCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        codeSizeRulesCheck.selectedProperty().addListener((obs, ov, nv) -> notifyModified());

        controversialRulesCheck = new CheckBox("Controversial Rules");
        controversialRulesCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        controversialRulesCheck.selectedProperty().addListener((obs, ov, nv) -> notifyModified());

        designRulesCheck = new CheckBox("Design Rules");
        designRulesCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        designRulesCheck.selectedProperty().addListener((obs, ov, nv) -> notifyModified());

        namingRulesCheck = new CheckBox("Naming Rules");
        namingRulesCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        namingRulesCheck.selectedProperty().addListener((obs, ov, nv) -> notifyModified());

        unusedCodeRulesCheck = new CheckBox("Unused Code Rules");
        unusedCodeRulesCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        unusedCodeRulesCheck.selectedProperty().addListener((obs, ov, nv) -> notifyModified());

        VBox standardRulesBox = new VBox(8, codeSizeRulesCheck, controversialRulesCheck, designRulesCheck, namingRulesCheck, unusedCodeRulesCheck);
        standardRulesBox.setPadding(new Insets(6, 0, 8, 4));

        // Custom Rulesets section
        Label customRulesetsLabel = new Label("Custom rulesets:");
        customRulesetsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        HBox customToolbar = new HBox(4);
        customToolbar.setAlignment(Pos.CENTER_LEFT);
        customToolbar.setPadding(new Insets(2, 0, 4, 0));

        Button addBtn = new Button("+");
        addBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-padding: 2 8; -fx-font-size: 12px; -fx-cursor: hand;");
        addBtn.setOnAction(e -> handleAddCustomRuleset());

        Button removeBtn = new Button("—");
        removeBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-padding: 2 8; -fx-font-size: 12px; -fx-cursor: hand;");
        removeBtn.setOnAction(e -> handleRemoveCustomRuleset());

        customToolbar.getChildren().addAll(addBtn, removeBtn);

        customRulesetsTable = new TableView<>(customRulesetsList);
        customRulesetsTable.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4;");
        customRulesetsTable.setPrefHeight(180);
        VBox.setVgrow(customRulesetsTable, Priority.ALWAYS);

        TableColumn<CustomRuleset, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(200);

        TableColumn<CustomRuleset, String> fileCol = new TableColumn<>("File");
        fileCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFile()));
        fileCol.setPrefWidth(350);

        customRulesetsTable.getColumns().addAll(nameCol, fileCol);
        customRulesetsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Label emptyLabel = new Label("Nothing to show");
        emptyLabel.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 13px;");
        customRulesetsTable.setPlaceholder(emptyLabel);

        VBox customBox = new VBox(6, customRulesetsLabel, customToolbar, customRulesetsTable);
        VBox.setVgrow(customBox, Priority.ALWAYS);

        // Error bubble
        errorBubbleBox = new HBox(8);
        errorBubbleBox.setAlignment(Pos.CENTER_LEFT);
        errorBubbleBox.setPadding(new Insets(6, 12, 6, 12));
        errorBubbleBox.setStyle("-fx-background-color: #432426; -fx-border-color: #7A3338; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label errIcon = new Label("⛔");
        errIcon.setStyle("-fx-text-fill: #F75F69; -fx-font-size: 13px;");

        errorLabel = new Label("Mess Detector path is empty for selected configuration");
        errorLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        errorBubbleBox.getChildren().addAll(errIcon, errorLabel);

        getChildren().addAll(topRow, standardRulesBox, customBox, errorBubbleBox);
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
        boolean empty = phpmdPath.isBlank();
        errorBubbleBox.setVisible(empty);
        errorBubbleBox.setManaged(empty);
    }

    private void handleAddCustomRuleset() {
        Stage dlgStage = new Stage();
        dlgStage.setTitle("Add Custom Ruleset");
        dlgStage.initModality(Modality.APPLICATION_MODAL);
        if (getScene() != null && getScene().getWindow() != null) {
            dlgStage.initOwner(getScene().getWindow());
        }

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.setStyle("-fx-background-color: #1E1F22;");

        Label nameLabel = new Label("Ruleset name:");
        nameLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        TextField nameField = new TextField();
        nameField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-font-size: 13px;");

        Label fileLabel = new Label("Ruleset XML file:");
        fileLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        TextField fileField = new TextField();
        fileField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-font-size: 13px;");
        HBox.setHgrow(fileField, Priority.ALWAYS);

        Button browseBtn = new Button("📁");
        browseBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-cursor: hand;");
        browseBtn.setOnAction(e -> {
            SelectPathDialog dlg = new SelectPathDialog(dlgStage, fileField.getText());
            String chosen = dlg.showAndWait();
            if (chosen != null && !chosen.isBlank()) {
                fileField.setText(chosen);
            }
        });

        HBox fileRow = new HBox(8, fileField, browseBtn);
        fileRow.setAlignment(Pos.CENTER_LEFT);

        HBox btnRow = new HBox(8);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        Button okBtn = new Button("OK");
        okBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-padding: 4 14; -fx-cursor: hand; -fx-background-radius: 4;");
        okBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            String file = fileField.getText().trim();
            if (!name.isEmpty() || !file.isEmpty()) {
                customRulesetsList.add(new CustomRuleset(name, file));
                notifyModified();
            }
            dlgStage.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-padding: 4 14; -fx-cursor: hand; -fx-background-radius: 4;");
        cancelBtn.setOnAction(e -> dlgStage.close());

        btnRow.getChildren().addAll(okBtn, cancelBtn);

        content.getChildren().addAll(nameLabel, nameField, fileLabel, fileRow, btnRow);
        dlgStage.setScene(new javafx.scene.Scene(content, 450, 200));
        dlgStage.showAndWait();
    }

    private void handleRemoveCustomRuleset() {
        CustomRuleset selected = customRulesetsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            customRulesetsList.remove(selected);
            notifyModified();
        }
    }

    private void showConfigurationDialog() {
        Stage dlgStage = new Stage();
        dlgStage.setTitle("Mess Detector by " + configurationCombo.getValue());
        dlgStage.initModality(Modality.APPLICATION_MODAL);
        if (getScene() != null && getScene().getWindow() != null) {
            dlgStage.initOwner(getScene().getWindow());
        }

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.setStyle("-fx-background-color: #1E1F22;");

        Label pathLabel = new Label("Mess Detector path:");
        pathLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        TextField pathInput = new TextField(phpmdPath);
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
            phpmdPath = pathInput.getText().trim();
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

    public void loadFromManager() {
        PhpMessDetectorConfig cfg = manager.getQualityToolsSettings().getMessDetector();
        inspectionEnabled = cfg.isInspectionEnabled();
        updateInspectionToggleBtn();
        configurationCombo.setValue(cfg.getConfigurationName());
        phpmdPath = cfg.getPhpmdPath();

        codeSizeRulesCheck.setSelected(cfg.isCodeSizeRules());
        controversialRulesCheck.setSelected(cfg.isControversialRules());
        designRulesCheck.setSelected(cfg.isDesignRules());
        namingRulesCheck.setSelected(cfg.isNamingRules());
        unusedCodeRulesCheck.setSelected(cfg.isUnusedCodeRules());

        customRulesetsList.clear();
        for (CustomRuleset cr : cfg.getCustomRulesets()) {
            customRulesetsList.add(cr.copy());
        }

        initialConfig = cfg.copy();
        updateErrorBubble();
    }

    private PhpMessDetectorConfig buildCurrentConfig() {
        PhpMessDetectorConfig cfg = new PhpMessDetectorConfig();
        cfg.setInspectionEnabled(inspectionEnabled);
        cfg.setConfigurationName(configurationCombo.getValue());
        cfg.setPhpmdPath(phpmdPath);

        cfg.setCodeSizeRules(codeSizeRulesCheck.isSelected());
        cfg.setControversialRules(controversialRulesCheck.isSelected());
        cfg.setDesignRules(designRulesCheck.isSelected());
        cfg.setNamingRules(namingRulesCheck.isSelected());
        cfg.setUnusedCodeRules(unusedCodeRulesCheck.isSelected());

        List<CustomRuleset> rulesets = new ArrayList<>();
        for (CustomRuleset cr : customRulesetsList) {
            rulesets.add(cr.copy());
        }
        cfg.setCustomRulesets(rulesets);

        return cfg;
    }

    public boolean isModified() {
        return !buildCurrentConfig().equals(initialConfig);
    }

    public void apply() {
        PhpQualityToolsSettings qs = manager.getQualityToolsSettings();
        PhpMessDetectorConfig cfg = buildCurrentConfig();
        qs.setMessDetector(cfg);
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
