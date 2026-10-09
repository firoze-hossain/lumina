package dev.lumina.ui;

import dev.lumina.rust.RustSettings;
import dev.lumina.rust.RustSettings.ExternalLintersConfig;
import dev.lumina.rust.RustSettingsManager;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Settings page for Languages & Frameworks > Rust > External Linters.
 * Faithfully matches Image 4.
 */
public class SettingsRustExternalLintersPage extends VBox {

    private final RustSettingsManager manager = RustSettingsManager.getInstance();

    private CheckBox runOnTheFlyCheck;
    private ComboBox<String> externalToolCombo;
    private TextField additionalArgumentsField;
    private ComboBox<String> channelCombo;
    private TextField environmentVariablesField;
    private Button envVarsBtn;

    private ExternalLintersConfig initialConfig = new ExternalLintersConfig();
    private Runnable onModified;

    public SettingsRustExternalLintersPage() {
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
        // 1. Run external linter on the fly
        runOnTheFlyCheck = new CheckBox("Run external linter on the fly");
        runOnTheFlyCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        runOnTheFlyCheck.setSelected(true);
        runOnTheFlyCheck.selectedProperty().addListener((obs, ov, nv) -> notifyModified());

        Label runSub = new Label("Adds code highlighting based on external linter results. May affect IDE performance.");
        runSub.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px; -fx-padding: 0 0 0 22;");

        VBox runBox = new VBox(3, runOnTheFlyCheck, runSub);

        // 2. External tool
        HBox toolRow = new HBox(12);
        toolRow.setAlignment(Pos.CENTER_LEFT);

        Label toolLabel = new Label("External tool:");
        toolLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        toolLabel.setPrefWidth(160);

        externalToolCombo = new ComboBox<>(FXCollections.observableArrayList("Cargo Check", "Clippy"));
        externalToolCombo.setValue("Cargo Check");
        externalToolCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-font-size: 12px;");
        externalToolCombo.valueProperty().addListener((obs, ov, nv) -> notifyModified());

        toolRow.getChildren().addAll(toolLabel, externalToolCombo);

        Label toolSub = new Label("Performs additional code analysis");
        toolSub.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px; -fx-padding: 0 0 0 172;");

        VBox toolBox = new VBox(3, toolRow, toolSub);

        // 3. Additional arguments + Channel
        HBox argsRow = new HBox(12);
        argsRow.setAlignment(Pos.CENTER_LEFT);

        Label argsLabel = new Label("Additional arguments:");
        argsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        argsLabel.setPrefWidth(160);

        additionalArgumentsField = new TextField();
        additionalArgumentsField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-padding: 4 8; -fx-font-size: 13px;");
        HBox.setHgrow(additionalArgumentsField, Priority.ALWAYS);
        additionalArgumentsField.textProperty().addListener((obs, ov, nv) -> notifyModified());

        Label channelLabel = new Label("Channel:");
        channelLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        channelCombo = new ComboBox<>(FXCollections.observableArrayList("[default]", "stable", "beta", "nightly"));
        channelCombo.setValue("[default]");
        channelCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-font-size: 12px;");
        channelCombo.valueProperty().addListener((obs, ov, nv) -> notifyModified());

        argsRow.getChildren().addAll(argsLabel, additionalArgumentsField, channelLabel, channelCombo);

        Label argsSub = new Label("Additional arguments for cargo check / cargo clippy");
        argsSub.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px; -fx-padding: 0 0 0 172;");

        VBox argsBox = new VBox(3, argsRow, argsSub);

        // 4. Environment variables
        HBox envRow = new HBox(12);
        envRow.setAlignment(Pos.CENTER_LEFT);

        Label envLabel = new Label("Environment variables:");
        envLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        envLabel.setPrefWidth(160);

        environmentVariablesField = new TextField();
        environmentVariablesField.setPromptText("Environment variables");
        environmentVariablesField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-padding: 4 8; -fx-font-size: 13px;");
        HBox.setHgrow(environmentVariablesField, Priority.ALWAYS);
        environmentVariablesField.textProperty().addListener((obs, ov, nv) -> notifyModified());

        envVarsBtn = new Button("🗔");
        envVarsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-cursor: hand; -fx-padding: 3 6;");
        envVarsBtn.setOnAction(e -> {
            TextInputDialog dlg = new TextInputDialog(environmentVariablesField.getText());
            dlg.setTitle("Environment Variables");
            dlg.setHeaderText("Specify environment variables (e.g. KEY1=VAL1;KEY2=VAL2):");
            dlg.showAndWait().ifPresent(val -> environmentVariablesField.setText(val));
        });

        envRow.getChildren().addAll(envLabel, environmentVariablesField, envVarsBtn);

        getChildren().addAll(runBox, toolBox, argsBox, envRow);
    }

    public void loadFromManager() {
        ExternalLintersConfig cfg = manager.getSettings().getExternalLinters();
        runOnTheFlyCheck.setSelected(cfg.isRunOnTheFly());
        externalToolCombo.setValue(cfg.getExternalTool());
        additionalArgumentsField.setText(cfg.getAdditionalArguments());
        channelCombo.setValue(cfg.getChannel());
        environmentVariablesField.setText(cfg.getEnvironmentVariables());

        initialConfig = cfg.copy();
    }

    private ExternalLintersConfig buildCurrentConfig() {
        ExternalLintersConfig cfg = new ExternalLintersConfig();
        cfg.setRunOnTheFly(runOnTheFlyCheck.isSelected());
        cfg.setExternalTool(externalToolCombo.getValue());
        cfg.setAdditionalArguments(additionalArgumentsField.getText().trim());
        cfg.setChannel(channelCombo.getValue());
        cfg.setEnvironmentVariables(environmentVariablesField.getText().trim());
        return cfg;
    }

    public boolean isModified() {
        return !buildCurrentConfig().equals(initialConfig);
    }

    public void apply() {
        RustSettings current = manager.getSettings();
        ExternalLintersConfig cfg = buildCurrentConfig();
        current.setExternalLinters(cfg);
        manager.setSettings(current);
        initialConfig = cfg.copy();
    }

    public void reset() {
        loadFromManager();
    }

    public void revertChanges() {
        reset();
    }
}
