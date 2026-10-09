package dev.lumina.ui;

import dev.lumina.php.PhpComposerSettings;
import dev.lumina.php.PhpSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

/**
 * Settings page for Languages & Frameworks > PHP > Composer.
 * Faithfully matches Image 4.
 */
public class SettingsLanguagesPhpComposerPage extends VBox {

    private final PhpSettingsManager manager = PhpSettingsManager.getInstance();

    private TextField composerJsonPathField;
    private CheckBox addPackagesAsLibrariesCheck;
    private CheckBox synchronizeIdeSettingsCheck;
    private CheckBox checkForPackageUpdatesCheck;
    private CheckBox showComposerJsonTopPanelCheck;
    private CheckBox notifyMissingVendorCheck;
    private CheckBox runWithIgnorePlatformReqsCheck;

    private RadioButton executableRadio;
    private RadioButton pharRadio;
    private ToggleGroup executionGroup;

    private Label executionExecutableLabel;
    private TextField composerExecutableField;
    private Button browseExecutableBtn;
    private HBox executableRow;

    private HBox warningBox;
    private Label warningLabel;

    // Snapshot for dirty checking
    private PhpComposerSettings initialSettings = new PhpComposerSettings();

    private Runnable onModified;

    public SettingsLanguagesPhpComposerPage() {
        setSpacing(12);
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
        updateWarningVisibility();
        if (onModified != null) {
            onModified.run();
        }
    }

    private void buildUI() {
        // 1. Path to composer.json
        HBox composerJsonRow = new HBox(8);
        composerJsonRow.setAlignment(Pos.CENTER_LEFT);

        Label composerJsonLabel = new Label("Path to composer.json:");
        composerJsonLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        composerJsonLabel.setPrefWidth(160);

        composerJsonPathField = new TextField();
        composerJsonPathField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-padding: 4 8; -fx-font-size: 13px;");
        HBox.setHgrow(composerJsonPathField, Priority.ALWAYS);
        composerJsonPathField.textProperty().addListener((obs, ov, nv) -> notifyModified());

        Button browseComposerJsonBtn = new Button("📁");
        browseComposerJsonBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 3 6; -fx-cursor: hand;");
        browseComposerJsonBtn.setOnAction(e -> {
            Window win = getScene() != null ? getScene().getWindow() : null;
            SelectPathDialog dlg = new SelectPathDialog(win, composerJsonPathField.getText());
            String chosen = dlg.showAndWait();
            if (chosen != null && !chosen.isBlank()) {
                composerJsonPathField.setText(chosen);
            }
        });

        composerJsonRow.getChildren().addAll(composerJsonLabel, composerJsonPathField, browseComposerJsonBtn);

        // 2. Options Checkboxes
        addPackagesAsLibrariesCheck = createCheckBox("Add packages as libraries");
        synchronizeIdeSettingsCheck = createCheckBox("Synchronize IDE Settings with composer.json");

        Label syncHint = new Label("Synchronization for PSR-0/PSR-4 roots and PHP Language Level is supported");
        syncHint.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px; -fx-padding: 0 0 4 22;");

        VBox syncBox = new VBox(2, synchronizeIdeSettingsCheck, syncHint);

        checkForPackageUpdatesCheck = createCheckBox("Check for available package updates");
        showComposerJsonTopPanelCheck = createCheckBox("Show composer.json top panel with quick actions");
        notifyMissingVendorCheck = createCheckBox("Notify about missing 'vendor' directory");
        runWithIgnorePlatformReqsCheck = createCheckBox("Run install/update with --ignore-platform-reqs");

        VBox optionsBox = new VBox(8,
                addPackagesAsLibrariesCheck,
                syncBox,
                checkForPackageUpdatesCheck,
                showComposerJsonTopPanelCheck,
                notifyMissingVendorCheck,
                runWithIgnorePlatformReqsCheck
        );
        optionsBox.setPadding(new Insets(4, 0, 8, 0));

        // 3. Execution Section
        HBox execHeader = createSectionHeader("Execution");

        executionGroup = new ToggleGroup();
        executableRadio = new RadioButton("'composer' executable");
        executableRadio.setToggleGroup(executionGroup);
        executableRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        pharRadio = new RadioButton("composer.phar");
        pharRadio.setToggleGroup(executionGroup);
        pharRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox radioRow = new HBox(20, executableRadio, pharRadio);
        radioRow.setAlignment(Pos.CENTER_LEFT);
        radioRow.setPadding(new Insets(4, 0, 4, 0));

        executableRadio.selectedProperty().addListener((obs, ov, nv) -> {
            executionExecutableLabel.setText(nv ? "'composer' executable" : "composer.phar:");
            notifyModified();
        });

        // Executable input row
        executableRow = new HBox(8);
        executableRow.setAlignment(Pos.CENTER_LEFT);

        executionExecutableLabel = new Label("'composer' executable");
        executionExecutableLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        executionExecutableLabel.setPrefWidth(160);

        composerExecutableField = new TextField("composer");
        composerExecutableField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-padding: 4 8; -fx-font-size: 13px;");
        HBox.setHgrow(composerExecutableField, Priority.ALWAYS);
        composerExecutableField.textProperty().addListener((obs, ov, nv) -> notifyModified());

        browseExecutableBtn = new Button("📁");
        browseExecutableBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 3 6; -fx-cursor: hand;");
        browseExecutableBtn.setOnAction(e -> {
            Window win = getScene() != null ? getScene().getWindow() : null;
            SelectPathDialog dlg = new SelectPathDialog(win, composerExecutableField.getText());
            String chosen = dlg.showAndWait();
            if (chosen != null && !chosen.isBlank()) {
                composerExecutableField.setText(chosen);
            }
        });

        executableRow.getChildren().addAll(executionExecutableLabel, composerExecutableField, browseExecutableBtn);

        // Spacer to push warning to bottom
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // 4. Bottom Warning
        warningBox = new HBox(6);
        warningBox.setAlignment(Pos.CENTER_LEFT);
        warningBox.setPadding(new Insets(8, 0, 0, 0));

        Label warnIcon = new Label("⚠");
        warnIcon.setStyle("-fx-text-fill: #EAA037; -fx-font-size: 14px; -fx-font-weight: bold;");

        warningLabel = new Label("Empty path to composer.json");
        warningLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        warningBox.getChildren().addAll(warnIcon, warningLabel);

        getChildren().addAll(
                composerJsonRow,
                optionsBox,
                execHeader,
                radioRow,
                executableRow,
                spacer,
                warningBox
        );
    }

    private CheckBox createCheckBox(String text) {
        CheckBox cb = new CheckBox(text);
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        cb.selectedProperty().addListener((obs, ov, nv) -> notifyModified());
        return cb;
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

    private void updateWarningVisibility() {
        boolean empty = composerJsonPathField.getText().trim().isEmpty();
        warningBox.setVisible(empty);
        warningBox.setManaged(empty);
    }

    public void loadFromManager() {
        PhpComposerSettings cs = manager.getComposerSettings();
        composerJsonPathField.setText(cs.getPathToComposerJson());
        addPackagesAsLibrariesCheck.setSelected(cs.isAddPackagesAsLibraries());
        synchronizeIdeSettingsCheck.setSelected(cs.isSynchronizeIdeSettings());
        checkForPackageUpdatesCheck.setSelected(cs.isCheckForAvailablePackageUpdates());
        showComposerJsonTopPanelCheck.setSelected(cs.isShowComposerJsonTopPanel());
        notifyMissingVendorCheck.setSelected(cs.isNotifyAboutMissingVendor());
        runWithIgnorePlatformReqsCheck.setSelected(cs.isRunWithIgnorePlatformReqs());

        if ("phar".equalsIgnoreCase(cs.getExecutionMode())) {
            pharRadio.setSelected(true);
            composerExecutableField.setText(cs.getComposerPharPath());
        } else {
            executableRadio.setSelected(true);
            composerExecutableField.setText(cs.getComposerExecutablePath());
        }

        initialSettings = cs.copy();
        updateWarningVisibility();
    }

    private PhpComposerSettings buildCurrentSettings() {
        PhpComposerSettings cs = new PhpComposerSettings();
        cs.setPathToComposerJson(composerJsonPathField.getText().trim());
        cs.setAddPackagesAsLibraries(addPackagesAsLibrariesCheck.isSelected());
        cs.setSynchronizeIdeSettings(synchronizeIdeSettingsCheck.isSelected());
        cs.setCheckForAvailablePackageUpdates(checkForPackageUpdatesCheck.isSelected());
        cs.setShowComposerJsonTopPanel(showComposerJsonTopPanelCheck.isSelected());
        cs.setNotifyAboutMissingVendor(notifyMissingVendorCheck.isSelected());
        cs.setRunWithIgnorePlatformReqs(runWithIgnorePlatformReqsCheck.isSelected());

        if (pharRadio.isSelected()) {
            cs.setExecutionMode("phar");
            cs.setComposerPharPath(composerExecutableField.getText().trim());
            cs.setComposerExecutablePath(initialSettings.getComposerExecutablePath());
        } else {
            cs.setExecutionMode("executable");
            cs.setComposerExecutablePath(composerExecutableField.getText().trim());
            cs.setComposerPharPath(initialSettings.getComposerPharPath());
        }
        return cs;
    }

    public boolean isModified() {
        return !buildCurrentSettings().equals(initialSettings);
    }

    public void apply() {
        PhpComposerSettings cs = buildCurrentSettings();
        manager.setComposerSettings(cs);
        initialSettings = cs.copy();
    }

    public void reset() {
        loadFromManager();
    }

    public void revertChanges() {
        reset();
    }
}
