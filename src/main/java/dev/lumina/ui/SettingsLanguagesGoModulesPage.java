package dev.lumina.ui;

import dev.lumina.go.GoSettings;
import dev.lumina.go.GoSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * Settings page for Languages & Frameworks > Go > Go Modules.
 * Faithfully matches Image 5.
 */
public class SettingsLanguagesGoModulesPage extends VBox {

    private final GoSettingsManager manager = GoSettingsManager.getInstance();

    private CheckBox enableModulesCheck;
    private TextField environmentField;
    private Button environmentBtn;
    private CheckBox vendoringCheck;
    private ComboBox<String> downloadDepsCombo;

    private boolean initialEnableModules = true;
    private String initialEnvironment = "";
    private boolean initialVendoring = true;
    private String initialDownloadDeps = "Enable for all projects";

    private Runnable onModified;

    public SettingsLanguagesGoModulesPage() {
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
        if (onModified != null) {
            onModified.run();
        }
    }

    private void buildUI() {
        // 1. Enable Go modules integration
        enableModulesCheck = new CheckBox("Enable Go modules integration");
        enableModulesCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        enableModulesCheck.setSelected(true);
        enableModulesCheck.selectedProperty().addListener((obs, ov, nv) -> {
            updateDisabledState(!nv);
            notifyModified();
        });

        // 2. Environment: [TextField] [🗔]
        Label envLabel = new Label("Environment:");
        envLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        envLabel.setMinWidth(100);

        environmentField = new TextField();
        environmentField.setPromptText("Environment variables");
        environmentField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; "
                + "-fx-border-color: #4E5157; -fx-border-radius: 4px; -fx-padding: 4 8; -fx-font-size: 13px;");
        HBox.setHgrow(environmentField, Priority.ALWAYS);
        environmentField.setMaxWidth(480);
        environmentField.textProperty().addListener((obs, ov, nv) -> notifyModified());

        environmentBtn = new Button();
        environmentBtn.setGraphic(GeneratorIcons.envVariablesIcon());
        environmentBtn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4px; -fx-cursor: hand; -fx-padding: 3 6;");
        environmentBtn.setOnAction(e -> {
            EnvironmentVariablesDialog.show(getOwnerStage(), environmentField.getText().trim())
                    .ifPresent(val -> {
                        environmentField.setText(val);
                        notifyModified();
                    });
        });

        HBox envInputRow = new HBox(8, environmentField, environmentBtn);
        envInputRow.setAlignment(Pos.CENTER_LEFT);

        Label envSubtext = new Label("GOPROXY, GOPRIVATE, and other environment variables");
        envSubtext.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 11px;");

        VBox envFieldBox = new VBox(4, envInputRow, envSubtext);

        HBox envRow = new HBox(12, envLabel, envFieldBox);
        envRow.setAlignment(Pos.TOP_LEFT);

        // 3. Enable vendoring support automatically
        vendoringCheck = new CheckBox("Enable vendoring support automatically");
        vendoringCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        vendoringCheck.setSelected(true);
        vendoringCheck.selectedProperty().addListener((obs, ov, nv) -> notifyModified());

        Label vendoringHelp = createHelpIcon("Vendoring support: automatically run 'go mod vendor' or use vendor folder");

        HBox vendoringRow = new HBox(6, vendoringCheck, vendoringHelp);
        vendoringRow.setAlignment(Pos.CENTER_LEFT);

        // 4. Download Go module dependencies: [ComboBox] [?]
        Label downloadLabel = new Label("Download Go module dependencies:");
        downloadLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        downloadDepsCombo = new ComboBox<>();
        downloadDepsCombo.getItems().addAll(
                "Enable for all projects",
                "Enable for non-vendored projects only",
                "Disabled"
        );
        downloadDepsCombo.setValue("Enable for all projects");
        downloadDepsCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; "
                + "-fx-border-color: #4E5157; -fx-border-radius: 4px; -fx-font-size: 13px;");
        downloadDepsCombo.valueProperty().addListener((obs, ov, nv) -> notifyModified());

        Label downloadHelp = createHelpIcon("Controls automatic downloading of Go module dependencies");

        HBox downloadRow = new HBox(10, downloadLabel, downloadDepsCombo, downloadHelp);
        downloadRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(enableModulesCheck, envRow, vendoringRow, downloadRow);
    }

    private void updateDisabledState(boolean disabled) {
        environmentField.setDisable(disabled);
        environmentBtn.setDisable(disabled);
        vendoringCheck.setDisable(disabled);
        downloadDepsCombo.setDisable(disabled);
    }

    private Label createHelpIcon(String tooltipText) {
        Label help = new Label("?");
        help.setStyle("-fx-text-fill: #707890; -fx-font-size: 11px; -fx-cursor: hand; "
                + "-fx-border-color: #707890; -fx-border-radius: 8; -fx-min-width: 15px; -fx-min-height: 15px; "
                + "-fx-alignment: center; -fx-padding: 0 3 0 3;");
        Tooltip.install(help, new Tooltip(tooltipText));
        return help;
    }

    private Stage getOwnerStage() {
        if (getScene() != null && getScene().getWindow() instanceof Stage stage) {
            return stage;
        }
        return null;
    }

    public void loadFromManager() {
        GoSettings s = manager.getSettings();
        enableModulesCheck.setSelected(s.isEnableGoModulesIntegration());
        environmentField.setText(s.getEnvironmentVariables());
        vendoringCheck.setSelected(s.isEnableVendoringSupportAutomatically());
        downloadDepsCombo.setValue(s.getDownloadGoModuleDependencies());

        updateDisabledState(!s.isEnableGoModulesIntegration());

        initialEnableModules = s.isEnableGoModulesIntegration();
        initialEnvironment = s.getEnvironmentVariables();
        initialVendoring = s.isEnableVendoringSupportAutomatically();
        initialDownloadDeps = s.getDownloadGoModuleDependencies();
    }

    public boolean isModified() {
        return enableModulesCheck.isSelected() != initialEnableModules ||
                !Objects.equals(environmentField.getText().trim(), initialEnvironment.trim()) ||
                vendoringCheck.isSelected() != initialVendoring ||
                !Objects.equals(downloadDepsCombo.getValue(), initialDownloadDeps);
    }

    public void apply() {
        GoSettings s = manager.getSettings();
        s.setEnableGoModulesIntegration(enableModulesCheck.isSelected());
        s.setEnvironmentVariables(environmentField.getText().trim());
        s.setEnableVendoringSupportAutomatically(vendoringCheck.isSelected());
        s.setDownloadGoModuleDependencies(downloadDepsCombo.getValue());
        manager.setSettings(s);

        initialEnableModules = enableModulesCheck.isSelected();
        initialEnvironment = environmentField.getText().trim();
        initialVendoring = vendoringCheck.isSelected();
        initialDownloadDeps = downloadDepsCombo.getValue();
    }

    public void reset() {
        loadFromManager();
    }

    public void revertChanges() {
        reset();
    }

    public boolean isEnableGoModulesIntegration() {
        return enableModulesCheck.isSelected();
    }

    public String getEnvironmentVariables() {
        return environmentField.getText().trim();
    }

    public boolean isEnableVendoringSupportAutomatically() {
        return vendoringCheck.isSelected();
    }

    public String getDownloadGoModuleDependencies() {
        return downloadDepsCombo.getValue();
    }
}
