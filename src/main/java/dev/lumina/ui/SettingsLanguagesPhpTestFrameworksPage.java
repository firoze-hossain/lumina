package dev.lumina.ui;

import dev.lumina.php.PhpSettingsManager;
import dev.lumina.php.PhpTestFrameworkConfig;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Settings page for Languages & Frameworks > PHP > Test Frameworks.
 * Faithfully matches Image 5.
 */
public class SettingsLanguagesPhpTestFrameworksPage extends VBox {

    private final PhpSettingsManager manager = PhpSettingsManager.getInstance();

    private final ObservableList<PhpTestFrameworkConfig> configsList = FXCollections.observableArrayList();
    private ListView<PhpTestFrameworkConfig> configsListView;

    // Detail controls
    private RadioButton useComposerAutoloaderRadio;
    private RadioButton pathToPhpunitPharRadio;
    private ToggleGroup libraryGroup;

    private TextField pathToScriptField;
    private Button browseScriptBtn;
    private Button refreshScriptBtn;

    private Label versionStatusLabel;
    private Label autoloaderStatusLabel;
    private HBox versionStatusRow;
    private HBox autoloaderStatusRow;

    private CheckBox defaultConfigFileCheck;
    private TextField defaultConfigField;
    private Button browseConfigFileBtn;

    private CheckBox defaultBootstrapFileCheck;
    private TextField defaultBootstrapField;
    private Button browseBootstrapFileBtn;

    private TextField paraTestBinaryField;
    private Button browseParaTestBtn;

    private Label testRootsDirectoryLabel;

    private VBox detailContainer;
    private PhpTestFrameworkConfig currentlyEditingConfig = null;
    private boolean isUpdatingFields = false;

    // Initial snapshot for dirty checking
    private List<PhpTestFrameworkConfig> initialConfigs = new ArrayList<>();

    private Runnable onModified;

    public SettingsLanguagesPhpTestFrameworksPage() {
        setSpacing(0);
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadFromManager();
    }

    public void setOnModified(Runnable onModified) {
        this.onModified = onModified;
    }

    private void notifyModified() {
        updateValidationStatus();
        if (onModified != null) {
            onModified.run();
        }
    }

    private void buildUI() {
        SplitPane splitPane = new SplitPane();
        splitPane.setStyle("-fx-background-color: #1E1F22; -fx-box-border: transparent;");
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        // ----------------------------------------------------
        // Left Column: Master List
        // ----------------------------------------------------
        VBox leftBox = new VBox();
        leftBox.setMinWidth(180);
        leftBox.setPrefWidth(240);
        leftBox.setMaxWidth(360);
        leftBox.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 1 0 0;");

        // Toolbar: + -
        HBox toolbar = new HBox(2);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 8, 6, 8));
        toolbar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 0 0 1 0;");

        Button addBtn = createToolbarButton("+", "Add test framework configuration", this::handleAddConfig);
        Button removeBtn = createToolbarButton("—", "Remove configuration", this::handleRemoveConfig);
        toolbar.getChildren().addAll(addBtn, removeBtn);

        configsListView = new ListView<>(configsList);
        configsListView.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent;");
        VBox.setVgrow(configsListView, Priority.ALWAYS);

        configsListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(PhpTestFrameworkConfig cfg, boolean empty) {
                super.updateItem(cfg, empty);
                if (empty || cfg == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    HBox itemBox = new HBox(6);
                    itemBox.setAlignment(Pos.CENTER_LEFT);

                    Label iconLabel = new Label("U");
                    iconLabel.setStyle("-fx-text-fill: #3574F0; -fx-font-weight: bold; -fx-font-size: 13px;");

                    Label titleLabel = new Label(cfg.getName());
                    titleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

                    itemBox.getChildren().addAll(iconLabel, titleLabel);
                    setGraphic(itemBox);
                    setText(null);

                    if (isSelected()) {
                        setStyle("-fx-background-color: #2E436E; -fx-padding: 3 8;");
                        titleLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 13px;");
                    } else {
                        setStyle("-fx-background-color: transparent; -fx-padding: 3 8;");
                    }
                }
            }
        });

        configsListView.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> {
            syncFieldsToConfig(ov);
            currentlyEditingConfig = nv;
            populateFieldsFromConfig(nv);
        });

        leftBox.getChildren().addAll(toolbar, configsListView);

        // ----------------------------------------------------
        // Right Column: Detail Form
        // ----------------------------------------------------
        detailContainer = new VBox(14);
        detailContainer.setPadding(new Insets(14, 20, 20, 20));
        detailContainer.setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(detailContainer, Priority.ALWAYS);

        // 1. PHPUnit library section
        HBox libHeader = createSectionHeader("PHPUnit library");

        libraryGroup = new ToggleGroup();
        useComposerAutoloaderRadio = new RadioButton("Use Composer autoloader");
        useComposerAutoloaderRadio.setToggleGroup(libraryGroup);
        useComposerAutoloaderRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        pathToPhpunitPharRadio = new RadioButton("Path to phpunit.phar");
        pathToPhpunitPharRadio.setToggleGroup(libraryGroup);
        pathToPhpunitPharRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox radioRow = new HBox(20, useComposerAutoloaderRadio, pathToPhpunitPharRadio);
        radioRow.setAlignment(Pos.CENTER_LEFT);
        useComposerAutoloaderRadio.selectedProperty().addListener((obs, ov, nv) -> {
            if (!isUpdatingFields && currentlyEditingConfig != null) {
                currentlyEditingConfig.setUseComposerAutoloader(nv);
                notifyModified();
            }
        });

        // Path to script row
        HBox scriptRow = new HBox(8);
        scriptRow.setAlignment(Pos.CENTER_LEFT);

        Label scriptLabel = new Label("Path to script:");
        scriptLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        scriptLabel.setPrefWidth(120);

        pathToScriptField = new TextField();
        pathToScriptField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-padding: 4 8; -fx-font-size: 13px;");
        HBox.setHgrow(pathToScriptField, Priority.ALWAYS);
        pathToScriptField.textProperty().addListener((obs, ov, nv) -> {
            if (!isUpdatingFields && currentlyEditingConfig != null) {
                currentlyEditingConfig.setPathToScript(nv);
                notifyModified();
            }
        });

        browseScriptBtn = new Button("📁");
        browseScriptBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 3 6; -fx-cursor: hand;");
        browseScriptBtn.setOnAction(e -> {
            Window win = getScene() != null ? getScene().getWindow() : null;
            SelectPathDialog dlg = new SelectPathDialog(win, pathToScriptField.getText());
            String chosen = dlg.showAndWait();
            if (chosen != null && !chosen.isBlank()) {
                pathToScriptField.setText(chosen);
                runDynamicVersionCheck();
            }
        });

        refreshScriptBtn = new Button("🔄");
        refreshScriptBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 3 6; -fx-cursor: hand;");
        refreshScriptBtn.setOnAction(e -> runDynamicVersionCheck());

        scriptRow.getChildren().addAll(scriptLabel, pathToScriptField, browseScriptBtn, refreshScriptBtn);

        // Status lines
        versionStatusRow = new HBox(6);
        versionStatusRow.setAlignment(Pos.CENTER_LEFT);
        Label vIcon = new Label("⛔");
        vIcon.setStyle("-fx-text-fill: #FA5252; -fx-font-size: 12px;");
        versionStatusLabel = new Label("PHPUnit version: Not installed");
        versionStatusLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        versionStatusRow.getChildren().addAll(vIcon, versionStatusLabel);

        autoloaderStatusRow = new HBox(6);
        autoloaderStatusRow.setAlignment(Pos.CENTER_LEFT);
        Label aIcon = new Label("⛔");
        aIcon.setStyle("-fx-text-fill: #FA5252; -fx-font-size: 12px;");
        autoloaderStatusLabel = new Label("Path to the autoloader file is empty");
        autoloaderStatusLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        autoloaderStatusRow.getChildren().addAll(aIcon, autoloaderStatusLabel);

        VBox statusBox = new VBox(4, versionStatusRow, autoloaderStatusRow);
        statusBox.setPadding(new Insets(2, 0, 8, 20));

        // 2. Test Runner section
        HBox runnerHeader = createSectionHeader("Test Runner");

        // Default configuration file
        HBox configRow = new HBox(8);
        configRow.setAlignment(Pos.CENTER_LEFT);

        defaultConfigFileCheck = new CheckBox("Default configuration file:");
        defaultConfigFileCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        defaultConfigFileCheck.setPrefWidth(210);
        defaultConfigFileCheck.selectedProperty().addListener((obs, ov, nv) -> {
            defaultConfigField.setDisable(!nv);
            if (!isUpdatingFields && currentlyEditingConfig != null) {
                currentlyEditingConfig.setUseDefaultConfigFile(nv);
                notifyModified();
            }
        });

        defaultConfigField = new TextField();
        defaultConfigField.setDisable(true);
        defaultConfigField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-padding: 4 8; -fx-font-size: 13px;");
        HBox.setHgrow(defaultConfigField, Priority.ALWAYS);
        defaultConfigField.textProperty().addListener((obs, ov, nv) -> {
            if (!isUpdatingFields && currentlyEditingConfig != null) {
                currentlyEditingConfig.setDefaultConfigFilePath(nv);
                notifyModified();
            }
        });

        browseConfigFileBtn = new Button("📁");
        browseConfigFileBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 3 6; -fx-cursor: hand;");
        browseConfigFileBtn.setOnAction(e -> {
            Window win = getScene() != null ? getScene().getWindow() : null;
            SelectPathDialog dlg = new SelectPathDialog(win, defaultConfigField.getText());
            String chosen = dlg.showAndWait();
            if (chosen != null && !chosen.isBlank()) {
                defaultConfigField.setText(chosen);
            }
        });

        configRow.getChildren().addAll(defaultConfigFileCheck, defaultConfigField, browseConfigFileBtn);

        // Default bootstrap file
        HBox bootstrapRow = new HBox(8);
        bootstrapRow.setAlignment(Pos.CENTER_LEFT);

        defaultBootstrapFileCheck = new CheckBox("Default bootstrap file:");
        defaultBootstrapFileCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        defaultBootstrapFileCheck.setPrefWidth(210);
        defaultBootstrapFileCheck.selectedProperty().addListener((obs, ov, nv) -> {
            defaultBootstrapField.setDisable(!nv);
            if (!isUpdatingFields && currentlyEditingConfig != null) {
                currentlyEditingConfig.setUseDefaultBootstrapFile(nv);
                notifyModified();
            }
        });

        defaultBootstrapField = new TextField();
        defaultBootstrapField.setDisable(true);
        defaultBootstrapField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-padding: 4 8; -fx-font-size: 13px;");
        HBox.setHgrow(defaultBootstrapField, Priority.ALWAYS);
        defaultBootstrapField.textProperty().addListener((obs, ov, nv) -> {
            if (!isUpdatingFields && currentlyEditingConfig != null) {
                currentlyEditingConfig.setDefaultBootstrapFilePath(nv);
                notifyModified();
            }
        });

        browseBootstrapFileBtn = new Button("📁");
        browseBootstrapFileBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 3 6; -fx-cursor: hand;");
        browseBootstrapFileBtn.setOnAction(e -> {
            Window win = getScene() != null ? getScene().getWindow() : null;
            SelectPathDialog dlg = new SelectPathDialog(win, defaultBootstrapField.getText());
            String chosen = dlg.showAndWait();
            if (chosen != null && !chosen.isBlank()) {
                defaultBootstrapField.setText(chosen);
            }
        });

        bootstrapRow.getChildren().addAll(defaultBootstrapFileCheck, defaultBootstrapField, browseBootstrapFileBtn);

        // Default ParaTest binary
        HBox paraTestRow = new HBox(8);
        paraTestRow.setAlignment(Pos.CENTER_LEFT);

        Label paraTestLabel = new Label("Default ParaTest binary:");
        paraTestLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        paraTestLabel.setPrefWidth(210);

        paraTestBinaryField = new TextField();
        paraTestBinaryField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-padding: 4 8; -fx-font-size: 13px;");
        HBox.setHgrow(paraTestBinaryField, Priority.ALWAYS);
        paraTestBinaryField.textProperty().addListener((obs, ov, nv) -> {
            if (!isUpdatingFields && currentlyEditingConfig != null) {
                currentlyEditingConfig.setDefaultParaTestBinary(nv);
                notifyModified();
            }
        });

        browseParaTestBtn = new Button("📁");
        browseParaTestBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 3 6; -fx-cursor: hand;");
        browseParaTestBtn.setOnAction(e -> {
            Window win = getScene() != null ? getScene().getWindow() : null;
            SelectPathDialog dlg = new SelectPathDialog(win, paraTestBinaryField.getText());
            String chosen = dlg.showAndWait();
            if (chosen != null && !chosen.isBlank()) {
                paraTestBinaryField.setText(chosen);
            }
        });

        paraTestRow.getChildren().addAll(paraTestLabel, paraTestBinaryField, browseParaTestBtn);

        // Info: Test roots
        HBox testRootsHeaderRow = new HBox(6);
        testRootsHeaderRow.setAlignment(Pos.CENTER_LEFT);
        Label infoIcon = new Label("ℹ");
        infoIcon.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label testRootsTitle = new Label("Test roots");
        testRootsTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
        testRootsHeaderRow.getChildren().addAll(infoIcon, testRootsTitle);

        testRootsDirectoryLabel = new Label("Directory: tests");
        testRootsDirectoryLabel.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px; -fx-padding: 0 0 0 20;");

        VBox testRootsBox = new VBox(4, testRootsHeaderRow, testRootsDirectoryLabel);
        testRootsBox.setPadding(new Insets(6, 0, 0, 0));

        detailContainer.getChildren().addAll(
                libHeader,
                radioRow,
                scriptRow,
                statusBox,
                runnerHeader,
                configRow,
                bootstrapRow,
                paraTestRow,
                testRootsBox
        );

        splitPane.getItems().addAll(leftBox, detailContainer);
        splitPane.setDividerPositions(0.3);

        getChildren().add(splitPane);
    }

    private Button createToolbarButton(String text, String tooltip, Runnable action) {
        Button btn = new Button(text);
        btn.setTooltip(new Tooltip(tooltip));
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-cursor: hand; -fx-background-radius: 3;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-cursor: hand;"));
        btn.setOnAction(e -> action.run());
        return btn;
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

    private void handleAddConfig() {
        String ver = "8.3.6";
        if (manager.getActiveInterpreter() != null && manager.getActiveInterpreter().getPhpVersion() != null) {
            ver = manager.getActiveInterpreter().getPhpVersion();
        }
        int num = configsList.size() + 1;
        String name = "Local PHP " + ver + (num > 1 ? " (" + num + ")" : "");
        PhpTestFrameworkConfig cfg = new PhpTestFrameworkConfig(UUID.randomUUID().toString(), name, manager.getActiveInterpreterId());
        cfg.setTestRootsDirectory(PhpSettingsManager.detectTestRoots());
        configsList.add(cfg);
        configsListView.getSelectionModel().select(cfg);
        notifyModified();
    }

    private void handleRemoveConfig() {
        int idx = configsListView.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            configsList.remove(idx);
            if (!configsList.isEmpty()) {
                int nextSel = Math.min(idx, configsList.size() - 1);
                configsListView.getSelectionModel().select(nextSel);
            } else {
                currentlyEditingConfig = null;
                clearFields();
            }
            notifyModified();
        }
    }

    private void runDynamicVersionCheck() {
        String script = pathToScriptField.getText().trim();
        String detected = PhpSettingsManager.detectPhpUnitVersion(script);
        if (currentlyEditingConfig != null) {
            currentlyEditingConfig.setDetectedVersion(detected);
        }
        updateValidationStatus();
        notifyModified();
    }

    private void updateValidationStatus() {
        String script = pathToScriptField.getText().trim();
        boolean empty = script.isEmpty();
        autoloaderStatusRow.setVisible(empty);
        autoloaderStatusRow.setManaged(empty);

        String ver = currentlyEditingConfig != null ? currentlyEditingConfig.getDetectedVersion() : "Not installed";
        versionStatusLabel.setText("PHPUnit version: " + ver);
    }

    private void populateFieldsFromConfig(PhpTestFrameworkConfig cfg) {
        isUpdatingFields = true;
        try {
            if (cfg != null) {
                detailContainer.setDisable(false);
                if (cfg.isUseComposerAutoloader()) {
                    useComposerAutoloaderRadio.setSelected(true);
                } else {
                    pathToPhpunitPharRadio.setSelected(true);
                }
                pathToScriptField.setText(cfg.getPathToScript());
                defaultConfigFileCheck.setSelected(cfg.isUseDefaultConfigFile());
                defaultConfigField.setText(cfg.getDefaultConfigFilePath());
                defaultConfigField.setDisable(!cfg.isUseDefaultConfigFile());
                defaultBootstrapFileCheck.setSelected(cfg.isUseDefaultBootstrapFile());
                defaultBootstrapField.setText(cfg.getDefaultBootstrapFilePath());
                defaultBootstrapField.setDisable(!cfg.isUseDefaultBootstrapFile());
                paraTestBinaryField.setText(cfg.getDefaultParaTestBinary());
                testRootsDirectoryLabel.setText("Directory: " + cfg.getTestRootsDirectory());
                updateValidationStatus();
            } else {
                detailContainer.setDisable(true);
                clearFields();
            }
        } finally {
            isUpdatingFields = false;
        }
    }

    private void syncFieldsToConfig(PhpTestFrameworkConfig cfg) {
        if (cfg != null && !isUpdatingFields) {
            cfg.setUseComposerAutoloader(useComposerAutoloaderRadio.isSelected());
            cfg.setPathToScript(pathToScriptField.getText().trim());
            cfg.setUseDefaultConfigFile(defaultConfigFileCheck.isSelected());
            cfg.setDefaultConfigFilePath(defaultConfigField.getText().trim());
            cfg.setUseDefaultBootstrapFile(defaultBootstrapFileCheck.isSelected());
            cfg.setDefaultBootstrapFilePath(defaultBootstrapField.getText().trim());
            cfg.setDefaultParaTestBinary(paraTestBinaryField.getText().trim());
        }
    }

    private void clearFields() {
        useComposerAutoloaderRadio.setSelected(true);
        pathToScriptField.setText("");
        defaultConfigFileCheck.setSelected(false);
        defaultConfigField.setText("");
        defaultConfigField.setDisable(true);
        defaultBootstrapFileCheck.setSelected(false);
        defaultBootstrapField.setText("");
        defaultBootstrapField.setDisable(true);
        paraTestBinaryField.setText("");
        testRootsDirectoryLabel.setText("Directory: tests");
        updateValidationStatus();
    }

    public void loadFromManager() {
        List<PhpTestFrameworkConfig> loaded = manager.getTestFrameworkConfigs();
        configsList.clear();
        for (PhpTestFrameworkConfig c : loaded) {
            configsList.add(c.copy());
        }

        initialConfigs = new ArrayList<>();
        for (PhpTestFrameworkConfig c : loaded) {
            initialConfigs.add(c.copy());
        }

        if (!configsList.isEmpty()) {
            configsListView.getSelectionModel().select(0);
        } else {
            clearFields();
        }
    }

    public boolean isModified() {
        syncFieldsToConfig(currentlyEditingConfig);
        if (configsList.size() != initialConfigs.size()) return true;
        for (int i = 0; i < configsList.size(); i++) {
            if (!configsList.get(i).equals(initialConfigs.get(i))) return true;
        }
        return false;
    }

    public void apply() {
        syncFieldsToConfig(currentlyEditingConfig);
        List<PhpTestFrameworkConfig> toSave = new ArrayList<>();
        for (PhpTestFrameworkConfig c : configsList) {
            toSave.add(c.copy());
        }
        manager.setTestFrameworkConfigs(toSave);

        initialConfigs = new ArrayList<>();
        for (PhpTestFrameworkConfig c : toSave) {
            initialConfigs.add(c.copy());
        }
    }

    public void reset() {
        loadFromManager();
    }

    public void revertChanges() {
        reset();
    }
}
