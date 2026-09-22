package dev.lumina.ui;

import dev.lumina.git.PerforceSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * Version Control > Perforce settings page matching IntelliJ IDEA Image 1.
 */
public class SettingsVcsPerforcePage extends VBox {

    private final PerforceSettingsManager manager = PerforceSettingsManager.getInstance();

    // Top
    private final CheckBox perforceOnlineCheck = new CheckBox("Perforce is online");
    private final CheckBox switchOfflineAutoCheck = new CheckBox("Switch to offline mode automatically if Perforce is unavailable");

    // Config Settings
    private final ComboBox<String> charsetCombo = new ComboBox<>();
    private final RadioButton useEnvValuesRadio = new RadioButton("Use environment values");
    private final RadioButton useConnectionParamsRadio = new RadioButton("Use connection parameters:");
    private final TextField serverPortField = new TextField();
    private final TextField userField = new TextField();
    private final TextField clientWorkspaceField = new TextField();

    // Ignore Settings
    private final RadioButton useP4IgnoreEnvRadio = new RadioButton("Use P4IGNORE environment variable");
    private final RadioButton useIgnoreSettingsRadio = new RadioButton("Use ignore settings:");
    private final TextField pathToIgnoreField = new TextField();
    private final Button ignoreBrowseBtn = new Button();

    // Lower Options
    private final CheckBox dumpCommandsCheck = new CheckBox("Dump Perforce Commands");
    private final Label dumpLogFileLabel = new Label();
    private final CheckBox useLoginAuthCheck = new CheckBox("Use login authentication");
    private final Button testConnectionBtn = new Button("Test Connection");
    private final Label testResultLabel = new Label();

    private final TextField p4PathField = new TextField();
    private final Button p4BrowseBtn = new Button();
    private final TextField p4vcPathField = new TextField();
    private final Button p4vcBrowseBtn = new Button();

    private final CheckBox showBranchingHistoryCheck = new CheckBox("Show branching history (for \"File History\" action, \"Compare With...\" list)");
    private final CheckBox showIntegratedChangelistsCheck = new CheckBox("Show integrated changelists in committed changes");
    private final Spinner<Integer> serverTimeoutSpinner = new Spinner<>(1, 3600, 20);
    private final CheckBox enableJobsSupportCheck = new CheckBox("Enable Perforce Jobs support");
    private final CheckBox findIgnoredFilesCheck = new CheckBox("Find ignored files using P4 executable");
    private final CheckBox alwaysSyncChangelistsCheck = new CheckBox("Always sync local changelists with Perforce");

    public SettingsVcsPerforcePage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(14, 20, 24, 20));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");

        // 1. Top Online Checkboxes
        initCheckBox(perforceOnlineCheck, manager.isPerforceOnline(), e -> manager.setPerforceOnline(perforceOnlineCheck.isSelected()));
        initCheckBox(switchOfflineAutoCheck, manager.isSwitchOfflineAuto(), e -> manager.setSwitchOfflineAuto(switchOfflineAutoCheck.isSelected()));

        // 2. Config Settings Section
        VBox configSection = buildConfigSection();

        // 3. Ignore Settings Section
        VBox ignoreSection = buildIgnoreSection();

        // 4. Lower Commands, Executables & Options
        VBox lowerSection = buildLowerSection();

        getChildren().addAll(
                perforceOnlineCheck, switchOfflineAutoCheck,
                configSection, ignoreSection, lowerSection
        );

        manager.addListener(this::syncFromManager);
        syncFromManager();
    }

    private VBox buildConfigSection() {
        VBox box = new VBox(8);
        box.getChildren().add(createSectionHeader("Config Settings"));

        // Charset
        Label charsetLabel = new Label("Charset:");
        charsetLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        charsetCombo.getItems().addAll("none", "utf8", "iso8859-1", "shiftjis", "eucjp", "winansi");
        charsetCombo.setValue(manager.getCharset());
        charsetCombo.setPrefWidth(350);
        charsetCombo.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");
        charsetCombo.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) manager.setCharset(newV);
        });
        HBox charsetRow = new HBox(12, charsetLabel, charsetCombo);
        charsetRow.setAlignment(Pos.CENTER_LEFT);

        // Radios
        ToggleGroup configGroup = new ToggleGroup();
        useEnvValuesRadio.setToggleGroup(configGroup);
        useConnectionParamsRadio.setToggleGroup(configGroup);
        initRadio(useEnvValuesRadio, manager.getConfigMode() == PerforceSettingsManager.ConfigMode.ENVIRONMENT_VALUES);
        initRadio(useConnectionParamsRadio, manager.getConfigMode() == PerforceSettingsManager.ConfigMode.CONNECTION_PARAMETERS);

        Label envSubtext = new Label("P4PORT,P4CLIENT,P4USER,P4PASSWD,P4CONFIG,P4IGNORE environment value(s) are not set");
        envSubtext.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");
        envSubtext.setPadding(new Insets(0, 0, 0, 24));

        // Connection Params Grid
        GridPane paramsGrid = new GridPane();
        paramsGrid.setHgap(10);
        paramsGrid.setVgap(6);
        paramsGrid.setPadding(new Insets(4, 0, 4, 24));

        Label serverLabel = new Label("Server (Port):");
        serverLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        serverPortField.setText(manager.getServerPort());
        styleTextField(serverPortField, 300);
        serverPortField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) manager.setServerPort(newV);
        });

        Label userLabel = new Label("User:");
        userLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        userField.setText(manager.getUser());
        styleTextField(userField, 300);
        userField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) manager.setUser(newV);
        });

        Label workspaceLabel = new Label("Workspace (Client):");
        workspaceLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        clientWorkspaceField.setText(manager.getClientWorkspace());
        styleTextField(clientWorkspaceField, 300);
        clientWorkspaceField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) manager.setClientWorkspace(newV);
        });

        paramsGrid.add(serverLabel, 0, 0);
        paramsGrid.add(serverPortField, 1, 0);
        paramsGrid.add(userLabel, 0, 1);
        paramsGrid.add(userField, 1, 1);
        paramsGrid.add(workspaceLabel, 0, 2);
        paramsGrid.add(clientWorkspaceField, 1, 2);

        serverPortField.disableProperty().bind(useConnectionParamsRadio.selectedProperty().not());
        userField.disableProperty().bind(useConnectionParamsRadio.selectedProperty().not());
        clientWorkspaceField.disableProperty().bind(useConnectionParamsRadio.selectedProperty().not());

        configGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            manager.setConfigMode(newV == useConnectionParamsRadio
                    ? PerforceSettingsManager.ConfigMode.CONNECTION_PARAMETERS
                    : PerforceSettingsManager.ConfigMode.ENVIRONMENT_VALUES);
        });

        box.getChildren().addAll(charsetRow, useEnvValuesRadio, envSubtext, useConnectionParamsRadio, paramsGrid);
        return box;
    }

    private VBox buildIgnoreSection() {
        VBox box = new VBox(8);
        box.getChildren().add(createSectionHeader("Ignore Settings"));

        ToggleGroup ignoreGroup = new ToggleGroup();
        useP4IgnoreEnvRadio.setToggleGroup(ignoreGroup);
        useIgnoreSettingsRadio.setToggleGroup(ignoreGroup);
        initRadio(useP4IgnoreEnvRadio, manager.getIgnoreMode() == PerforceSettingsManager.IgnoreMode.P4IGNORE_ENV);
        initRadio(useIgnoreSettingsRadio, manager.getIgnoreMode() == PerforceSettingsManager.IgnoreMode.IGNORE_SETTINGS);

        Label ignoreEnvSubtext = new Label("P4IGNORE environment variable is not set");
        ignoreEnvSubtext.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");
        ignoreEnvSubtext.setPadding(new Insets(0, 0, 0, 24));

        Label pathToIgnoreLabel = new Label("Path to ignore file:");
        pathToIgnoreLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        pathToIgnoreField.setText(manager.getPathToIgnoreFile());
        styleTextField(pathToIgnoreField, 240);
        pathToIgnoreField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) manager.setPathToIgnoreFile(newV);
        });

        styleBrowseBtn(ignoreBrowseBtn, "Select Ignore File", f -> {
            pathToIgnoreField.setText(f.getAbsolutePath());
            manager.setPathToIgnoreFile(f.getAbsolutePath());
        });

        HBox ignoreSettingsBox = new HBox(8, pathToIgnoreLabel, pathToIgnoreField, ignoreBrowseBtn);
        ignoreSettingsBox.setAlignment(Pos.CENTER_LEFT);
        ignoreSettingsBox.setPadding(new Insets(0, 0, 0, 24));

        pathToIgnoreField.disableProperty().bind(useIgnoreSettingsRadio.selectedProperty().not());
        ignoreBrowseBtn.disableProperty().bind(useIgnoreSettingsRadio.selectedProperty().not());

        ignoreGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            manager.setIgnoreMode(newV == useIgnoreSettingsRadio
                    ? PerforceSettingsManager.IgnoreMode.IGNORE_SETTINGS
                    : PerforceSettingsManager.IgnoreMode.P4IGNORE_ENV);
        });

        box.getChildren().addAll(useP4IgnoreEnvRadio, ignoreEnvSubtext, useIgnoreSettingsRadio, ignoreSettingsBox);
        return box;
    }

    private VBox buildLowerSection() {
        VBox box = new VBox(8);

        // Dump Commands
        initCheckBox(dumpCommandsCheck, manager.isDumpCommands(), e -> manager.setDumpCommands(dumpCommandsCheck.isSelected()));
        dumpLogFileLabel.setText("Log File: '" + manager.getLogFilePath() + "'");
        dumpLogFileLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");
        dumpLogFileLabel.setPadding(new Insets(0, 0, 0, 24));

        // Use login auth & Test Connection
        initCheckBox(useLoginAuthCheck, manager.isUseLoginAuthentication(), e -> manager.setUseLoginAuthentication(useLoginAuthCheck.isSelected()));

        testConnectionBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 14 4 14; -fx-font-size: 12px; -fx-cursor: hand;");
        testConnectionBtn.setOnAction(e -> onTestConnection());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox authRow = new HBox(8, useLoginAuthCheck, spacer, testConnectionBtn);
        authRow.setAlignment(Pos.CENTER_LEFT);

        testResultLabel.setStyle("-fx-font-size: 11px;");
        testResultLabel.setVisible(false);
        testResultLabel.setManaged(false);

        // Path to P4 executable
        Label p4Label = new Label("Path to P4 executable:");
        p4Label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        p4PathField.setText(manager.getP4ExecutablePath());
        styleTextField(p4PathField, 300);
        HBox.setHgrow(p4PathField, Priority.ALWAYS);
        p4PathField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) manager.setP4ExecutablePath(newV);
        });
        styleBrowseBtn(p4BrowseBtn, "Select P4 Executable", f -> {
            p4PathField.setText(f.getAbsolutePath());
            manager.setP4ExecutablePath(f.getAbsolutePath());
        });
        HBox p4Row = new HBox(8, p4Label, p4PathField, p4BrowseBtn);
        p4Row.setAlignment(Pos.CENTER_LEFT);

        // Path to P4VC executable
        Label p4vcLabel = new Label("Path to P4VC executable:");
        p4vcLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        p4vcPathField.setText(manager.getP4vcExecutablePath());
        styleTextField(p4vcPathField, 300);
        HBox.setHgrow(p4vcPathField, Priority.ALWAYS);
        p4vcPathField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) manager.setP4vcExecutablePath(newV);
        });
        styleBrowseBtn(p4vcBrowseBtn, "Select P4VC Executable", f -> {
            p4vcPathField.setText(f.getAbsolutePath());
            manager.setP4vcExecutablePath(f.getAbsolutePath());
        });
        HBox p4vcRow = new HBox(8, p4vcLabel, p4vcPathField, p4vcBrowseBtn);
        p4vcRow.setAlignment(Pos.CENTER_LEFT);

        // Options
        initCheckBox(showBranchingHistoryCheck, manager.isShowBranchingHistory(), e -> manager.setShowBranchingHistory(showBranchingHistoryCheck.isSelected()));
        initCheckBox(showIntegratedChangelistsCheck, manager.isShowIntegratedChangelists(), e -> manager.setShowIntegratedChangelists(showIntegratedChangelistsCheck.isSelected()));

        Label timeoutLabel = new Label("Server timeout");
        timeoutLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        serverTimeoutSpinner.getValueFactory().setValue(manager.getServerTimeoutSeconds());
        serverTimeoutSpinner.setPrefWidth(65);
        serverTimeoutSpinner.setStyle("-fx-background-color: #1E1F22; -fx-font-size: 12px;");
        serverTimeoutSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) manager.setServerTimeoutSeconds(newV);
        });
        Label secondsLabel = new Label("seconds");
        secondsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        HBox timeoutRow = new HBox(8, timeoutLabel, serverTimeoutSpinner, secondsLabel);
        timeoutRow.setAlignment(Pos.CENTER_LEFT);

        initCheckBox(enableJobsSupportCheck, manager.isEnableJobsSupport(), e -> manager.setEnableJobsSupport(enableJobsSupportCheck.isSelected()));
        initCheckBox(findIgnoredFilesCheck, manager.isFindIgnoredFilesUsingP4(), e -> manager.setFindIgnoredFilesUsingP4(findIgnoredFilesCheck.isSelected()));
        initCheckBox(alwaysSyncChangelistsCheck, manager.isAlwaysSyncLocalChangelists(), e -> manager.setAlwaysSyncLocalChangelists(alwaysSyncChangelistsCheck.isSelected()));

        box.getChildren().addAll(
                dumpCommandsCheck, dumpLogFileLabel, authRow, testResultLabel,
                p4Row, p4vcRow,
                showBranchingHistoryCheck, showIntegratedChangelistsCheck, timeoutRow,
                enableJobsSupportCheck, findIgnoredFilesCheck, alwaysSyncChangelistsCheck
        );
        return box;
    }

    private void onTestConnection() {
        PerforceSettingsManager.TestResult res = manager.testConnection();
        testResultLabel.setText(res.message());
        testResultLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (res.success() ? "#73BD79" : "#ED5E62") + ";");
        testResultLabel.setVisible(true);
        testResultLabel.setManaged(true);
    }

    private HBox createSectionHeader(String title) {
        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #393B40; -fx-border-color: #393B40;");
        HBox.setHgrow(sep, Priority.ALWAYS);

        HBox bar = new HBox(8, lbl, sep);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 0, 2, 0));
        return bar;
    }

    private void initCheckBox(CheckBox cb, boolean initial, javafx.event.EventHandler<javafx.event.ActionEvent> onAction) {
        cb.setSelected(initial);
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        cb.setOnAction(onAction);
    }

    private void initRadio(RadioButton rb, boolean initial) {
        rb.setSelected(initial);
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
    }

    private void styleTextField(TextField tf, double width) {
        tf.setPrefWidth(width);
        tf.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8 4 8; -fx-font-size: 12px;");
    }

    private void styleBrowseBtn(Button btn, String title, java.util.function.Consumer<File> onFileChosen) {
        SVGPath folderIcon = new SVGPath();
        folderIcon.setContent("M 2 3 L 5 3 L 6.5 5 L 12 5 L 12 11 L 2 11 Z");
        folderIcon.setFill(Color.TRANSPARENT);
        folderIcon.setStroke(Color.web("#848BA3"));
        folderIcon.setStrokeWidth(1.2);
        btn.setGraphic(folderIcon);
        btn.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-padding: 4 8 4 8; -fx-cursor: hand;");
        btn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle(title);
            File f = fc.showOpenDialog(getScene().getWindow());
            if (f != null) {
                onFileChosen.accept(f);
            }
        });
    }

    private void syncFromManager() {
        perforceOnlineCheck.setSelected(manager.isPerforceOnline());
        switchOfflineAutoCheck.setSelected(manager.isSwitchOfflineAuto());
        charsetCombo.setValue(manager.getCharset());
        if (manager.getConfigMode() == PerforceSettingsManager.ConfigMode.CONNECTION_PARAMETERS) {
            useConnectionParamsRadio.setSelected(true);
        } else {
            useEnvValuesRadio.setSelected(true);
        }
        serverPortField.setText(manager.getServerPort());
        userField.setText(manager.getUser());
        clientWorkspaceField.setText(manager.getClientWorkspace());

        if (manager.getIgnoreMode() == PerforceSettingsManager.IgnoreMode.IGNORE_SETTINGS) {
            useIgnoreSettingsRadio.setSelected(true);
        } else {
            useP4IgnoreEnvRadio.setSelected(true);
        }
        pathToIgnoreField.setText(manager.getPathToIgnoreFile());

        dumpCommandsCheck.setSelected(manager.isDumpCommands());
        dumpLogFileLabel.setText("Log File: '" + manager.getLogFilePath() + "'");
        useLoginAuthCheck.setSelected(manager.isUseLoginAuthentication());
        p4PathField.setText(manager.getP4ExecutablePath());
        p4vcPathField.setText(manager.getP4vcExecutablePath());
        showBranchingHistoryCheck.setSelected(manager.isShowBranchingHistory());
        showIntegratedChangelistsCheck.setSelected(manager.isShowIntegratedChangelists());
        serverTimeoutSpinner.getValueFactory().setValue(manager.getServerTimeoutSeconds());
        enableJobsSupportCheck.setSelected(manager.isEnableJobsSupport());
        findIgnoredFilesCheck.setSelected(manager.isFindIgnoredFilesUsingP4());
        alwaysSyncChangelistsCheck.setSelected(manager.isAlwaysSyncLocalChangelists());
    }
}
