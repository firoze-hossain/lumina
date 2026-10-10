package dev.lumina.ui;

import dev.lumina.typescript.TypeScriptSettings;
import dev.lumina.typescript.TypeScriptSettingsManager;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Objects;

/**
 * Settings page for Languages & Frameworks > TypeScript.
 * Matches reference screenshot media_1791602860333_b0b06ef7.png:
 *  - Node interpreter: [ Project node (/usr/local/bin/node)   24.11.1 v ] [ ... ]
 *  - TypeScript: [ Bundled   5.7.3 v ] [ ... ]
 *  - [x] TypeScript language service
 *        [x] Show project errors
 *        [x] Show suggestions
 *        [ ] Enable service-powered type engine
 *        [ ] Recompile on changes
 *  - Options: [                                                         ]
 */
public class SettingsLanguagesTypeScriptPage extends VBox {

    private final TypeScriptSettingsManager manager = TypeScriptSettingsManager.getInstance();

    private final ComboBox<String> nodeInterpreterCombo = new ComboBox<>();
    private final Button browseNodeBtn = new Button("...");

    private final ComboBox<String> typeScriptCombo = new ComboBox<>();
    private final Button browseTypeScriptBtn = new Button("...");

    private final CheckBox useLanguageServiceCheck = new CheckBox("TypeScript language service");
    private final CheckBox showProjectErrorsCheck = new CheckBox("Show project errors");
    private final CheckBox showSuggestionsCheck = new CheckBox("Show suggestions");
    private final CheckBox enableTypeEngineCheck = new CheckBox("Enable service-powered type engine");
    private final CheckBox recompileOnChangesCheck = new CheckBox("Recompile on changes");

    private final TextField optionsField = new TextField();

    private TypeScriptSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsLanguagesTypeScriptPage() {
        setSpacing(12);
        setPadding(new Insets(16, 24, 24, 24));
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadData();
    }

    private void buildUI() {
        // --- 1. Node interpreter row ---
        Label nodeLabel = new Label("Node interpreter:");
        nodeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        nodeLabel.setPrefWidth(130);

        nodeInterpreterCombo.setItems(FXCollections.observableArrayList(manager.discoverNodeInterpreters()));
        nodeInterpreterCombo.setValue(TypeScriptSettings.DEFAULT_NODE);
        nodeInterpreterCombo.setStyle(
                "-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; " +
                "-fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px;"
        );
        nodeInterpreterCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nodeInterpreterCombo, Priority.ALWAYS);
        nodeInterpreterCombo.valueProperty().addListener((obs, o, n) -> notifyModified());

        styleBrowseButton(browseNodeBtn);
        browseNodeBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Node.js Interpreter");
            File f = fc.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (f != null) {
                String path = f.getAbsolutePath();
                if (!nodeInterpreterCombo.getItems().contains(path)) {
                    nodeInterpreterCombo.getItems().add(path);
                }
                nodeInterpreterCombo.setValue(path);
                notifyModified();
            }
        });

        HBox nodeRow = new HBox(8, nodeLabel, nodeInterpreterCombo, browseNodeBtn);
        nodeRow.setAlignment(Pos.CENTER_LEFT);

        // --- 2. TypeScript row ---
        Label tsLabel = new Label("TypeScript:");
        tsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        tsLabel.setPrefWidth(130);

        typeScriptCombo.setItems(FXCollections.observableArrayList(
                TypeScriptSettings.DEFAULT_TYPESCRIPT,
                "Detect from node_modules",
                "Custom..."
        ));
        typeScriptCombo.setValue(TypeScriptSettings.DEFAULT_TYPESCRIPT);
        typeScriptCombo.setStyle(
                "-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; " +
                "-fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px;"
        );
        typeScriptCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(typeScriptCombo, Priority.ALWAYS);
        typeScriptCombo.valueProperty().addListener((obs, o, n) -> notifyModified());

        styleBrowseButton(browseTypeScriptBtn);
        browseTypeScriptBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select TypeScript Package");
            File f = fc.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (f != null) {
                String path = f.getAbsolutePath();
                if (!typeScriptCombo.getItems().contains(path)) {
                    typeScriptCombo.getItems().add(path);
                }
                typeScriptCombo.setValue(path);
                notifyModified();
            }
        });

        HBox tsRow = new HBox(8, tsLabel, typeScriptCombo, browseTypeScriptBtn);
        tsRow.setAlignment(Pos.CENTER_LEFT);

        // --- 3. TypeScript language service & sub-options ---
        VBox serviceBox = new VBox(8);
        serviceBox.setPadding(new Insets(6, 0, 4, 0));

        useLanguageServiceCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        useLanguageServiceCheck.selectedProperty().addListener((obs, o, n) -> {
            boolean enabled = Boolean.TRUE.equals(n);
            showProjectErrorsCheck.setDisable(!enabled);
            showSuggestionsCheck.setDisable(!enabled);
            enableTypeEngineCheck.setDisable(!enabled);
            recompileOnChangesCheck.setDisable(!enabled);
            notifyModified();
        });

        VBox subOptionsBox = new VBox(6);
        subOptionsBox.setPadding(new Insets(0, 0, 0, 22));

        showProjectErrorsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        showProjectErrorsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        showSuggestionsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        showSuggestionsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        enableTypeEngineCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        enableTypeEngineCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        recompileOnChangesCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        recompileOnChangesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        subOptionsBox.getChildren().addAll(
                showProjectErrorsCheck,
                showSuggestionsCheck,
                enableTypeEngineCheck,
                recompileOnChangesCheck
        );

        serviceBox.getChildren().addAll(useLanguageServiceCheck, subOptionsBox);

        // --- 4. Options row ---
        Label optionsLabel = new Label("Options:");
        optionsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        optionsLabel.setPrefWidth(130);

        optionsField.setStyle(
                "-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; " +
                "-fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px;"
        );
        optionsField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(optionsField, Priority.ALWAYS);
        optionsField.textProperty().addListener((obs, o, n) -> notifyModified());

        HBox optionsRow = new HBox(8, optionsLabel, optionsField);
        optionsRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(nodeRow, tsRow, serviceBox, optionsRow);
    }

    private void styleBrowseButton(Button btn) {
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

    public void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();
            applySettingsToUI(initialSettings);
        } finally {
            updating = false;
        }
    }

    private void applySettingsToUI(TypeScriptSettings s) {
        if (s == null) return;
        if (!nodeInterpreterCombo.getItems().contains(s.getNodeInterpreter())) {
            nodeInterpreterCombo.getItems().add(s.getNodeInterpreter());
        }
        nodeInterpreterCombo.setValue(s.getNodeInterpreter());

        if (!typeScriptCombo.getItems().contains(s.getTypeScriptPackage())) {
            typeScriptCombo.getItems().add(s.getTypeScriptPackage());
        }
        typeScriptCombo.setValue(s.getTypeScriptPackage());

        useLanguageServiceCheck.setSelected(s.isUseLanguageService());
        showProjectErrorsCheck.setSelected(s.isShowProjectErrors());
        showSuggestionsCheck.setSelected(s.isShowSuggestions());
        enableTypeEngineCheck.setSelected(s.isEnableServicePoweredTypeEngine());
        recompileOnChangesCheck.setSelected(s.isRecompileOnChanges());

        boolean enabled = s.isUseLanguageService();
        showProjectErrorsCheck.setDisable(!enabled);
        showSuggestionsCheck.setDisable(!enabled);
        enableTypeEngineCheck.setDisable(!enabled);
        recompileOnChangesCheck.setDisable(!enabled);

        optionsField.setText(s.getOptions());
    }

    public TypeScriptSettings getCurrentSettingsFromUI() {
        TypeScriptSettings s = new TypeScriptSettings();
        s.setNodeInterpreter(nodeInterpreterCombo.getValue());
        s.setTypeScriptPackage(typeScriptCombo.getValue());
        s.setUseLanguageService(useLanguageServiceCheck.isSelected());
        s.setShowProjectErrors(showProjectErrorsCheck.isSelected());
        s.setShowSuggestions(showSuggestionsCheck.isSelected());
        s.setEnableServicePoweredTypeEngine(enableTypeEngineCheck.isSelected());
        s.setRecompileOnChanges(recompileOnChangesCheck.isSelected());
        s.setOptions(optionsField.getText() != null ? optionsField.getText() : "");
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        TypeScriptSettings updated = getCurrentSettingsFromUI();
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
    public ComboBox<String> getNodeInterpreterCombo() {
        return nodeInterpreterCombo;
    }

    public ComboBox<String> getTypeScriptCombo() {
        return typeScriptCombo;
    }

    public CheckBox getUseLanguageServiceCheck() {
        return useLanguageServiceCheck;
    }

    public CheckBox getShowProjectErrorsCheck() {
        return showProjectErrorsCheck;
    }

    public CheckBox getShowSuggestionsCheck() {
        return showSuggestionsCheck;
    }

    public CheckBox getEnableTypeEngineCheck() {
        return enableTypeEngineCheck;
    }

    public CheckBox getRecompileOnChangesCheck() {
        return recompileOnChangesCheck;
    }

    public TextField getOptionsField() {
        return optionsField;
    }
}
