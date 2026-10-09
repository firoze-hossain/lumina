package dev.lumina.ui;

import dev.lumina.rust.RustSettings;
import dev.lumina.rust.RustSettings.RustfmtConfig;
import dev.lumina.rust.RustSettingsManager;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Settings page for Languages & Frameworks > Rust > Rustfmt.
 * Faithfully matches Image 5.
 */
public class SettingsRustfmtPage extends VBox {

    private final RustSettingsManager manager = RustSettingsManager.getInstance();

    private TextField additionalArgumentsField;
    private ComboBox<String> channelCombo;
    private TextField environmentVariablesField;
    private Button envVarsBtn;
    private CheckBox useRustfmtInsteadOfBuiltInCheck;
    private Hyperlink configureActionsOnSaveLink;

    private RustfmtConfig initialConfig = new RustfmtConfig();
    private Runnable onModified;
    private Runnable onNavigateToActionsOnSave;

    public SettingsRustfmtPage() {
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

    public void setOnNavigateToActionsOnSave(Runnable onNavigateToActionsOnSave) {
        this.onNavigateToActionsOnSave = onNavigateToActionsOnSave;
    }

    private void notifyModified() {
        if (onModified != null) {
            onModified.run();
        }
    }

    private void buildUI() {
        // 1. Additional arguments + Channel
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

        Label argsSub = new Label("Additional arguments for rustfmt / cargo fmt");
        argsSub.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px; -fx-padding: 0 0 0 172;");

        VBox argsBox = new VBox(3, argsRow, argsSub);

        // 2. Environment variables
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

        // 3. Use Rustfmt instead of built-in formatter
        useRustfmtInsteadOfBuiltInCheck = new CheckBox("Use Rustfmt instead of the built-in formatter");
        useRustfmtInsteadOfBuiltInCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        useRustfmtInsteadOfBuiltInCheck.setSelected(true);
        useRustfmtInsteadOfBuiltInCheck.selectedProperty().addListener((obs, ov, nv) -> notifyModified());

        Label rustfmtSub = new Label("Rustfmt will only be used to format whole files. For code fragments, the IDE will switch to the built-in formatter.");
        rustfmtSub.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px; -fx-padding: 0 0 0 22;");

        VBox rustfmtBox = new VBox(3, useRustfmtInsteadOfBuiltInCheck, rustfmtSub);

        // 4. Configure actions on save link
        configureActionsOnSaveLink = new Hyperlink("Configure actions on save...");
        configureActionsOnSaveLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 4 0;");
        configureActionsOnSaveLink.setOnAction(e -> {
            if (onNavigateToActionsOnSave != null) {
                onNavigateToActionsOnSave.run();
            }
        });

        getChildren().addAll(argsBox, envRow, rustfmtBox, configureActionsOnSaveLink);
    }

    public void loadFromManager() {
        RustfmtConfig cfg = manager.getSettings().getRustfmt();
        additionalArgumentsField.setText(cfg.getAdditionalArguments());
        channelCombo.setValue(cfg.getChannel());
        environmentVariablesField.setText(cfg.getEnvironmentVariables());
        useRustfmtInsteadOfBuiltInCheck.setSelected(cfg.isUseRustfmtInsteadOfBuiltIn());

        initialConfig = cfg.copy();
    }

    private RustfmtConfig buildCurrentConfig() {
        RustfmtConfig cfg = new RustfmtConfig();
        cfg.setAdditionalArguments(additionalArgumentsField.getText().trim());
        cfg.setChannel(channelCombo.getValue());
        cfg.setEnvironmentVariables(environmentVariablesField.getText().trim());
        cfg.setUseRustfmtInsteadOfBuiltIn(useRustfmtInsteadOfBuiltInCheck.isSelected());
        return cfg;
    }

    public boolean isModified() {
        return !buildCurrentConfig().equals(initialConfig);
    }

    public void apply() {
        RustSettings current = manager.getSettings();
        RustfmtConfig cfg = buildCurrentConfig();
        current.setRustfmt(cfg);
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
