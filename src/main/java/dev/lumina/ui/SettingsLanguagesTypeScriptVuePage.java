package dev.lumina.ui;

import dev.lumina.typescript.VueServiceSettings;
import dev.lumina.typescript.VueServiceSettingsManager;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Objects;

/**
 * Settings page for Languages & Frameworks > TypeScript > Vue.
 * Matches reference screenshot media_1791604015621_895d532c.png:
 *  - Header: Vue Service
 *  - Vue Language Server: [ @vue/language-server (Default)   2.2.10 v ] [ ... ]
 *  - ( ) Disabled
 *  - (•) Auto
 *  - ( ) Classic TypeScript Service
 *  - Separator
 *  - [ ] Enable service-powered type engine [Alpha]
 *  - [ ] Vue LS 3.0 preview
 */
public class SettingsLanguagesTypeScriptVuePage extends VBox {

    private final VueServiceSettingsManager manager = VueServiceSettingsManager.getInstance();

    private final ComboBox<String> serverCombo = new ComboBox<>();
    private final Button browseServerBtn = new Button("...");

    private final ToggleGroup modeGroup = new ToggleGroup();
    private final RadioButton disabledRadio = new RadioButton("Disabled");
    private final RadioButton autoRadio = new RadioButton("Auto");
    private final RadioButton classicRadio = new RadioButton("Classic TypeScript Service");

    private final CheckBox enableTypeEngineCheck = new CheckBox("Enable service-powered type engine");
    private final Label alphaBadge = new Label("Alpha");
    private final CheckBox ls3PreviewCheck = new CheckBox("Vue LS 3.0 preview");

    private VueServiceSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsLanguagesTypeScriptVuePage() {
        setSpacing(12);
        setPadding(new Insets(16, 24, 24, 24));
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadData();
    }

    private void buildUI() {
        // --- 1. Section Header ---
        Label headerLabel = new Label("Vue Service");
        headerLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        // --- 2. Vue Language Server Row ---
        Label serverLabel = new Label("Vue Language Server:");
        serverLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        serverLabel.setPrefWidth(140);

        serverCombo.setItems(FXCollections.observableArrayList(manager.discoverVueServers()));
        serverCombo.setValue(VueServiceSettings.DEFAULT_SERVER);
        serverCombo.setStyle(
                "-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; " +
                "-fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px;"
        );
        serverCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(serverCombo, Priority.ALWAYS);
        serverCombo.valueProperty().addListener((obs, o, n) -> notifyModified());

        browseServerBtn.setStyle(
                "-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; " +
                "-fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 4 10 4 10;"
        );
        browseServerBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Vue Language Server Package");
            File f = fc.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (f != null) {
                String path = f.getAbsolutePath();
                if (!serverCombo.getItems().contains(path)) {
                    serverCombo.getItems().add(path);
                }
                serverCombo.setValue(path);
                notifyModified();
            }
        });

        HBox serverRow = new HBox(8, serverLabel, serverCombo, browseServerBtn);
        serverRow.setAlignment(Pos.CENTER_LEFT);

        // --- 3. Radio Buttons ---
        disabledRadio.setToggleGroup(modeGroup);
        disabledRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        disabledRadio.selectedProperty().addListener((obs, o, n) -> notifyModified());
        Label disabledDesc = new Label("Select this option to turn both language services off. Only the internal IDE inspections will be used.");
        disabledDesc.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px; -fx-padding: 0 0 0 22;");
        disabledDesc.setWrapText(true);
        VBox disabledBox = new VBox(4, disabledRadio, disabledDesc);

        autoRadio.setToggleGroup(modeGroup);
        autoRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        autoRadio.selectedProperty().addListener((obs, o, n) -> notifyModified());
        Label autoDesc = new Label("Select this option to enable Vue Language Server (in Takeover Mode) when possible. The internal IDE inspections will still be used.");
        autoDesc.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px; -fx-padding: 0 0 0 22;");
        autoDesc.setWrapText(true);
        VBox autoBox = new VBox(4, autoRadio, autoDesc);

        classicRadio.setToggleGroup(modeGroup);
        classicRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        classicRadio.selectedProperty().addListener((obs, o, n) -> notifyModified());
        Label classicDesc = new Label("Select this option to forcibly enable classic integration with TypeScript service for Vue files. Because it doesn't work for TypeScript version 5.0 and later, in such cases, internal IDE inspections will be used instead.");
        classicDesc.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px; -fx-padding: 0 0 0 22;");
        classicDesc.setWrapText(true);
        VBox classicBox = new VBox(4, classicRadio, classicDesc);

        // --- 4. Separator ---
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: #393B40;");
        VBox.setMargin(divider, new Insets(6, 0, 6, 0));

        // --- 5. Service-powered type engine (Alpha) ---
        HBox engineBox = new HBox(8);
        engineBox.setAlignment(Pos.CENTER_LEFT);
        enableTypeEngineCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        enableTypeEngineCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        alphaBadge.setStyle("-fx-background-color: #275231; -fx-text-fill: #A8D08D; -fx-font-size: 9px; -fx-padding: 1 4 1 4; -fx-background-radius: 4; -fx-font-weight: bold;");
        engineBox.getChildren().addAll(enableTypeEngineCheck, alphaBadge);

        // --- 6. Vue LS 3.0 preview ---
        VBox ls3Box = new VBox(4);
        ls3PreviewCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        ls3PreviewCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        Label ls3Desc = new Label("Use Vue TypeScript Plugin from Vue LS 3.0");
        ls3Desc.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px; -fx-padding: 0 0 0 22;");
        ls3Box.getChildren().addAll(ls3PreviewCheck, ls3Desc);

        getChildren().addAll(
                headerLabel,
                serverRow,
                disabledBox,
                autoBox,
                classicBox,
                divider,
                engineBox,
                ls3Box
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

    private void applySettingsToUI(VueServiceSettings s) {
        if (s == null) return;
        if (!serverCombo.getItems().contains(s.getServerPackage())) {
            serverCombo.getItems().add(s.getServerPackage());
        }
        serverCombo.setValue(s.getServerPackage());

        if (VueServiceSettings.MODE_DISABLED.equalsIgnoreCase(s.getMode())) {
            disabledRadio.setSelected(true);
        } else if (VueServiceSettings.MODE_CLASSIC.equalsIgnoreCase(s.getMode())) {
            classicRadio.setSelected(true);
        } else {
            autoRadio.setSelected(true);
        }

        enableTypeEngineCheck.setSelected(s.isEnableServicePoweredTypeEngine());
        ls3PreviewCheck.setSelected(s.isVueLs3Preview());
    }

    public VueServiceSettings getCurrentSettingsFromUI() {
        VueServiceSettings s = new VueServiceSettings();
        s.setServerPackage(serverCombo.getValue());
        if (disabledRadio.isSelected()) {
            s.setMode(VueServiceSettings.MODE_DISABLED);
        } else if (classicRadio.isSelected()) {
            s.setMode(VueServiceSettings.MODE_CLASSIC);
        } else {
            s.setMode(VueServiceSettings.MODE_AUTO);
        }
        s.setEnableServicePoweredTypeEngine(enableTypeEngineCheck.isSelected());
        s.setVueLs3Preview(ls3PreviewCheck.isSelected());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        VueServiceSettings updated = getCurrentSettingsFromUI();
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
    public ComboBox<String> getServerCombo() {
        return serverCombo;
    }

    public RadioButton getDisabledRadio() {
        return disabledRadio;
    }

    public RadioButton getAutoRadio() {
        return autoRadio;
    }

    public RadioButton getClassicRadio() {
        return classicRadio;
    }

    public CheckBox getEnableTypeEngineCheck() {
        return enableTypeEngineCheck;
    }

    public CheckBox getLs3PreviewCheck() {
        return ls3PreviewCheck;
    }
}
