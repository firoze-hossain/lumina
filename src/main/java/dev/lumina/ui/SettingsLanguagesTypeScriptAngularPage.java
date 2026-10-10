package dev.lumina.ui;

import dev.lumina.typescript.AngularPluginSettings;
import dev.lumina.typescript.AngularPluginSettingsManager;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Objects;

/**
 * Settings page for Languages & Frameworks > TypeScript > Angular.
 * Matches reference screenshot media_1791602871731_fc5fd682.png:
 *  - Header: Angular TypeScript Plugin
 *  - ( ) Disabled
 *        Select this option to turn language service off. Only the internal IDE inspections will be used.
 *  - (•) Auto
 *        Select this option to enable Lumina custom Angular TypeScript Plugin when possible...
 *  - [ ] Enable service-powered type engine
 */
public class SettingsLanguagesTypeScriptAngularPage extends VBox {

    private final AngularPluginSettingsManager manager = AngularPluginSettingsManager.getInstance();

    private final ToggleGroup modeGroup = new ToggleGroup();
    private final RadioButton disabledRadio = new RadioButton("Disabled");
    private final RadioButton autoRadio = new RadioButton("Auto");
    private final CheckBox enableTypeEngineCheck = new CheckBox("Enable service-powered type engine");

    private AngularPluginSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsLanguagesTypeScriptAngularPage() {
        setSpacing(14);
        setPadding(new Insets(16, 24, 24, 24));
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadData();
    }

    private void buildUI() {
        // --- 1. Section Header ---
        Label headerLabel = new Label("Angular TypeScript Plugin");
        headerLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        // --- 2. Radio Options ---
        disabledRadio.setToggleGroup(modeGroup);
        disabledRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        disabledRadio.selectedProperty().addListener((obs, o, n) -> notifyModified());

        Label disabledDesc = new Label("Select this option to turn language service off. Only the internal IDE inspections will be used.");
        disabledDesc.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px; -fx-padding: 0 0 0 22;");
        disabledDesc.setWrapText(true);

        VBox disabledBox = new VBox(4, disabledRadio, disabledDesc);

        autoRadio.setToggleGroup(modeGroup);
        autoRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        autoRadio.selectedProperty().addListener((obs, o, n) -> notifyModified());

        Label autoDesc = new Label(
                "Select this option to enable Lumina custom Angular TypeScript Plugin when possible. " +
                "Lumina will use TypeScript language server inspections to analyze template expressions. " +
                "If \"use types from server\" options is enabled, the type evaluation will also happen within TypeScript Language Service."
        );
        autoDesc.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px; -fx-padding: 0 0 0 22;");
        autoDesc.setWrapText(true);

        VBox autoBox = new VBox(4, autoRadio, autoDesc);

        // --- 3. Enable service-powered type engine Checkbox ---
        enableTypeEngineCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 8 0 0 0;");
        enableTypeEngineCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        getChildren().addAll(
                headerLabel,
                disabledBox,
                autoBox,
                enableTypeEngineCheck
        );
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

    private void applySettingsToUI(AngularPluginSettings s) {
        if (s == null) return;
        if (AngularPluginSettings.MODE_DISABLED.equalsIgnoreCase(s.getMode())) {
            disabledRadio.setSelected(true);
        } else {
            autoRadio.setSelected(true);
        }
        enableTypeEngineCheck.setSelected(s.isEnableServicePoweredTypeEngine());
    }

    public AngularPluginSettings getCurrentSettingsFromUI() {
        AngularPluginSettings s = new AngularPluginSettings();
        s.setMode(disabledRadio.isSelected() ? AngularPluginSettings.MODE_DISABLED : AngularPluginSettings.MODE_AUTO);
        s.setEnableServicePoweredTypeEngine(enableTypeEngineCheck.isSelected());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        AngularPluginSettings updated = getCurrentSettingsFromUI();
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

    // Getters for testing and programmatic inspection
    public RadioButton getDisabledRadio() {
        return disabledRadio;
    }

    public RadioButton getAutoRadio() {
        return autoRadio;
    }

    public CheckBox getEnableTypeEngineCheck() {
        return enableTypeEngineCheck;
    }
}
