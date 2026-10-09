package dev.lumina.ui;

import dev.lumina.php.PhpQualityToolsSettings;
import dev.lumina.php.PhpQualityToolsSettings.PhpCodeSnifferConfig;
import dev.lumina.php.PhpSettingsManager;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Settings page for Languages & Frameworks > PHP > Quality Tools > PHP_CodeSniffer.
 * Faithfully matches Image 1.
 */
public class SettingsLanguagesPhpQualityToolsCodeSnifferPage extends VBox {

    private final PhpSettingsManager manager = PhpSettingsManager.getInstance();

    private Button inspectionToggleBtn;
    private ComboBox<String> configurationCombo;
    private Button configureBtn;
    private Hyperlink showIgnoredFilesLink;

    private TextField extensionsField;
    private CheckBox showWarningAsCheck;
    private ComboBox<String> warningSeverityCombo;
    private CheckBox showSniffNameCheck;
    private CheckBox installedStandardsCheck;
    private TextField installedStandardsField;
    private Button browseStandardsBtn;
    private ComboBox<String> codingStandardCombo;

    private HBox errorBubbleBox;
    private Label errorLabel;

    private boolean inspectionEnabled = false;
    private String phpcsPath = "";
    private String phpcbfPath = "";

    private PhpCodeSnifferConfig initialConfig = new PhpCodeSnifferConfig();
    private Runnable onModified;

    public SettingsLanguagesPhpQualityToolsCodeSnifferPage() {
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

        Label inspLabel = new Label("PHP_CodeSniffer inspection:");
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

        // Extensions
        HBox extensionsRow = new HBox(12);
        extensionsRow.setAlignment(Pos.CENTER_LEFT);

        Label extLabel = new Label("Check files with extensions:");
        extLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        extLabel.setPrefWidth(200);

        extensionsField = new TextField("php,js,css,inc");
        extensionsField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-padding: 4 8; -fx-font-size: 13px;");
        HBox.setHgrow(extensionsField, Priority.ALWAYS);
        extensionsField.textProperty().addListener((obs, ov, nv) -> notifyModified());

        extensionsRow.getChildren().addAll(extLabel, extensionsField);

        // Show warning as
        HBox warningAsRow = new HBox(12);
        warningAsRow.setAlignment(Pos.CENTER_LEFT);

        showWarningAsCheck = new CheckBox("Show warning as:");
        showWarningAsCheck.setSelected(true);
        showWarningAsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        showWarningAsCheck.setPrefWidth(200);
        showWarningAsCheck.selectedProperty().addListener((obs, ov, nv) -> {
            warningSeverityCombo.setDisable(!nv);
            notifyModified();
        });

        warningSeverityCombo = new ComboBox<>(FXCollections.observableArrayList("Weak Warning", "Warning", "Error", "Server Problem"));
        warningSeverityCombo.setValue("Weak Warning");
        warningSeverityCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-font-size: 12px;");
        warningSeverityCombo.setPrefWidth(220);
        warningSeverityCombo.valueProperty().addListener((obs, ov, nv) -> notifyModified());

        warningAsRow.getChildren().addAll(showWarningAsCheck, warningSeverityCombo);

        // Show sniff name
        showSniffNameCheck = new CheckBox("Show sniff name");
        showSniffNameCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        showSniffNameCheck.selectedProperty().addListener((obs, ov, nv) -> notifyModified());

        // Installed standards path
        HBox standardsRow = new HBox(12);
        standardsRow.setAlignment(Pos.CENTER_LEFT);

        installedStandardsCheck = new CheckBox("Installed standards path:");
        installedStandardsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        installedStandardsCheck.setPrefWidth(200);
        installedStandardsCheck.selectedProperty().addListener((obs, ov, nv) -> {
            installedStandardsField.setDisable(!nv);
            browseStandardsBtn.setDisable(!nv);
            notifyModified();
        });

        installedStandardsField = new TextField("");
        installedStandardsField.setDisable(true);
        installedStandardsField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-padding: 4 8; -fx-font-size: 13px;");
        HBox.setHgrow(installedStandardsField, Priority.ALWAYS);
        installedStandardsField.textProperty().addListener((obs, ov, nv) -> notifyModified());

        browseStandardsBtn = new Button("📁");
        browseStandardsBtn.setDisable(true);
        browseStandardsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 3 6; -fx-cursor: hand;");
        browseStandardsBtn.setOnAction(e -> {
            Window win = getScene() != null ? getScene().getWindow() : null;
            SelectPathDialog dlg = new SelectPathDialog(win, installedStandardsField.getText());
            String chosen = dlg.showAndWait();
            if (chosen != null && !chosen.isBlank()) {
                installedStandardsField.setText(chosen);
            }
        });

        standardsRow.getChildren().addAll(installedStandardsCheck, installedStandardsField, browseStandardsBtn);

        // Coding standard
        HBox standardRow = new HBox(12);
        standardRow.setAlignment(Pos.CENTER_LEFT);

        Label standardLabel = new Label("Coding standard:");
        standardLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        standardLabel.setPrefWidth(200);

        codingStandardCombo = new ComboBox<>(FXCollections.observableArrayList("PSR2", "PSR1", "PSR12", "PEAR", "Squiz", "Zend", "Custom"));
        codingStandardCombo.setValue("PSR2");
        codingStandardCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-font-size: 12px;");
        codingStandardCombo.setPrefWidth(120);
        codingStandardCombo.valueProperty().addListener((obs, ov, nv) -> notifyModified());

        standardRow.getChildren().addAll(standardLabel, codingStandardCombo);

        VBox optionsBox = new VBox(10, extensionsRow, warningAsRow, showSniffNameCheck, standardsRow, standardRow);
        optionsBox.setPadding(new Insets(4, 0, 8, 16));

        // Error bubble
        errorBubbleBox = new HBox(8);
        errorBubbleBox.setAlignment(Pos.CENTER_LEFT);
        errorBubbleBox.setPadding(new Insets(6, 12, 6, 12));
        errorBubbleBox.setStyle("-fx-background-color: #432426; -fx-border-color: #7A3338; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label errIcon = new Label("⛔");
        errIcon.setStyle("-fx-text-fill: #F75F69; -fx-font-size: 13px;");

        errorLabel = new Label("PHP_CodeSniffer path is empty for selected configuration");
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
        boolean empty = phpcsPath.isBlank();
        errorBubbleBox.setVisible(empty);
        errorBubbleBox.setManaged(empty);
    }

    private void showConfigurationDialog() {
        Stage dlgStage = new Stage();
        dlgStage.setTitle("PHP_CodeSniffer by " + configurationCombo.getValue());
        dlgStage.initModality(Modality.APPLICATION_MODAL);
        if (getScene() != null && getScene().getWindow() != null) {
            dlgStage.initOwner(getScene().getWindow());
        }

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.setStyle("-fx-background-color: #1E1F22;");

        Label pathLabel = new Label("PHP_CodeSniffer path:");
        pathLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        TextField pathInput = new TextField(phpcsPath);
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

        Label phpcbfLabel = new Label("Path to phpcbf:");
        phpcbfLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        TextField phpcbfInput = new TextField(phpcbfPath);
        phpcbfInput.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-font-size: 13px;");
        HBox.setHgrow(phpcbfInput, Priority.ALWAYS);

        Button browseCbfBtn = new Button("📁");
        browseCbfBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-cursor: hand;");
        browseCbfBtn.setOnAction(e -> {
            SelectPathDialog dlg = new SelectPathDialog(dlgStage, phpcbfInput.getText());
            String chosen = dlg.showAndWait();
            if (chosen != null && !chosen.isBlank()) phpcbfInput.setText(chosen);
        });

        HBox phpcbfRow = new HBox(8, phpcbfLabel, phpcbfInput, browseCbfBtn);
        phpcbfRow.setAlignment(Pos.CENTER_LEFT);

        HBox btnRow = new HBox(8);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        Button okBtn = new Button("OK");
        okBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-padding: 4 14; -fx-cursor: hand; -fx-background-radius: 4;");
        okBtn.setOnAction(e -> {
            phpcsPath = pathInput.getText().trim();
            phpcbfPath = phpcbfInput.getText().trim();
            dlgStage.close();
            notifyModified();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-padding: 4 14; -fx-cursor: hand; -fx-background-radius: 4;");
        cancelBtn.setOnAction(e -> dlgStage.close());

        btnRow.getChildren().addAll(okBtn, cancelBtn);

        content.getChildren().addAll(pathRow, phpcbfRow, btnRow);
        dlgStage.setScene(new javafx.scene.Scene(content, 480, 180));
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
        PhpCodeSnifferConfig cfg = manager.getQualityToolsSettings().getCodeSniffer();
        inspectionEnabled = cfg.isInspectionEnabled();
        updateInspectionToggleBtn();
        configurationCombo.setValue(cfg.getConfigurationName());
        phpcsPath = cfg.getPhpcsPath();
        phpcbfPath = cfg.getPhpcbfPath();
        extensionsField.setText(cfg.getCheckFilesWithExtensions());
        showWarningAsCheck.setSelected(cfg.isShowWarningAs());
        warningSeverityCombo.setValue(cfg.getWarningSeverity());
        warningSeverityCombo.setDisable(!cfg.isShowWarningAs());
        showSniffNameCheck.setSelected(cfg.isShowSniffName());
        installedStandardsCheck.setSelected(cfg.isInstalledStandardsPathEnabled());
        installedStandardsField.setText(cfg.getInstalledStandardsPath());
        installedStandardsField.setDisable(!cfg.isInstalledStandardsPathEnabled());
        browseStandardsBtn.setDisable(!cfg.isInstalledStandardsPathEnabled());
        codingStandardCombo.setValue(cfg.getCodingStandard());

        initialConfig = cfg.copy();
        updateErrorBubble();
    }

    private PhpCodeSnifferConfig buildCurrentConfig() {
        PhpCodeSnifferConfig cfg = new PhpCodeSnifferConfig();
        cfg.setInspectionEnabled(inspectionEnabled);
        cfg.setConfigurationName(configurationCombo.getValue());
        cfg.setPhpcsPath(phpcsPath);
        cfg.setPhpcbfPath(phpcbfPath);
        cfg.setCheckFilesWithExtensions(extensionsField.getText().trim());
        cfg.setShowWarningAs(showWarningAsCheck.isSelected());
        cfg.setWarningSeverity(warningSeverityCombo.getValue());
        cfg.setShowSniffName(showSniffNameCheck.isSelected());
        cfg.setInstalledStandardsPathEnabled(installedStandardsCheck.isSelected());
        cfg.setInstalledStandardsPath(installedStandardsField.getText().trim());
        cfg.setCodingStandard(codingStandardCombo.getValue());
        return cfg;
    }

    public boolean isModified() {
        return !buildCurrentConfig().equals(initialConfig);
    }

    public void apply() {
        PhpQualityToolsSettings qs = manager.getQualityToolsSettings();
        PhpCodeSnifferConfig cfg = buildCurrentConfig();
        qs.setCodeSniffer(cfg);
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
