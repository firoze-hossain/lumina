package dev.lumina.ui;

import dev.lumina.settings.SystemSettings;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * IntelliJ IDEA / DataGrip styled System Settings page:
 * - System Settings (Main page)
 * - Date Formats
 * - Data Sharing
 * - HTTP Proxy
 * - Language and Region, Passwords, Process Elevation, Server Certificates, Trusted Hosts, Updates
 */
public class SettingsSystemPage extends VBox {

    private final SystemSettings settings = SystemSettings.getInstance();

    // ---- System Settings (Main Page) Controls ----
    private CheckBox confirmExitCheck;
    private RadioButton procTerminateRadio;
    private RadioButton procDisconnectRadio;
    private RadioButton procAskRadio;

    private CheckBox reopenProjectsCheck;
    private RadioButton openProjNewRadio;
    private RadioButton openProjCurrentRadio;
    private RadioButton openProjAskRadio;
    private TextField defaultProjectDirField;

    private CheckBox idleAutosaveCheck;
    private TextField idleSecondsField;
    private CheckBox focusLostAutosaveCheck;
    private CheckBox backupFilesCheck;
    private CheckBox syncOnFocusCheck;
    private CheckBox syncPeriodicallyCheck;

    // ---- Date Formats Controls ----
    private CheckBox overrideDateFormatCheck;
    private ComboBox<String> dateFormatCombo;
    private CheckBox use24HourTimeCheck;
    private Label dateFormatPreviewLabel;
    private CheckBox prettyFormattingCheck;

    // ---- Data Sharing Controls ----
    private CheckBox sendAnonymousCheck;
    private CheckBox sendDetailedCheck;

    // ---- HTTP Proxy Controls ----
    private RadioButton noProxyRadio;
    private RadioButton autoDetectProxyRadio;
    private CheckBox autoConfigUrlCheck;
    private TextField autoConfigUrlField;
    private Button clearPasswordsBtn;

    private RadioButton manualProxyRadio;
    private RadioButton manualHttpRadio;
    private RadioButton manualSocksRadio;
    private TextField proxyHostField;
    private TextField proxyPortField;
    private TextField noProxyForField;
    private CheckBox proxyAuthCheck;
    private TextField proxyLoginField;
    private PasswordField proxyPasswordField;
    private CheckBox proxyRememberCheck;
    private Button checkConnectionBtn;

    private VBox autoDetectBox;
    private VBox manualProxyBox;
    private VBox authFieldsBox;
    private VBox dateFormatOptionsBox;

    public SettingsSystemPage() {
        setPadding(new Insets(16, 24, 24, 24));
        setSpacing(14);
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        showSubPage("System Settings");
    }

    public void showSubPage(String pageName) {
        getChildren().clear();

        Node pageContent = switch (pageName) {
            case "System Settings" -> buildSystemSettingsMainPage();
            case "Date Formats" -> buildDateFormatsPage();
            case "Data Sharing" -> buildDataSharingPage();
            case "HTTP Proxy" -> buildHttpProxyPage();
            case "Language and Region" -> buildLanguageRegionPage();
            case "Passwords" -> buildPasswordsPage();
            case "Process Elevation" -> buildProcessElevationPage();
            case "Server Certificates" -> buildServerCertificatesPage();
            case "Trusted Hosts" -> buildTrustedHostsPage();
            case "Updates" -> buildUpdatesPage();
            default -> buildPlaceholderPage(pageName);
        };

        getChildren().add(pageContent);
    }

    // ============================================================
    // 1. System Settings (Main Page) - Matches Image 2
    // ============================================================
    private Node buildSystemSettingsMainPage() {
        VBox root = new VBox(14);
        root.setStyle("-fx-background-color: #1E1F22;");

        // Top Options
        confirmExitCheck = new CheckBox("Confirm before exiting the IDE");
        confirmExitCheck.setSelected(settings.isConfirmExit());
        styleCheck(confirmExitCheck);

        Label procLabel = new Label("When closing a tool window with a running process:");
        procLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        ToggleGroup procGroup = new ToggleGroup();
        procTerminateRadio = new RadioButton("Terminate process");
        procDisconnectRadio = new RadioButton("Disconnect");
        procAskRadio = new RadioButton("Ask");

        procTerminateRadio.setToggleGroup(procGroup);
        procDisconnectRadio.setToggleGroup(procGroup);
        procAskRadio.setToggleGroup(procGroup);

        styleRadio(procTerminateRadio);
        styleRadio(procDisconnectRadio);
        styleRadio(procAskRadio);

        switch (settings.getProcessClosePolicy()) {
            case TERMINATE -> procTerminateRadio.setSelected(true);
            case DISCONNECT -> procDisconnectRadio.setSelected(true);
            case ASK -> procAskRadio.setSelected(true);
        }

        HBox procRow = new HBox(16, procLabel, procTerminateRadio, procDisconnectRadio, procAskRadio);
        procRow.setAlignment(Pos.CENTER_LEFT);

        // Project Section
        HBox projectSectionHeader = buildSectionHeader("Project");

        reopenProjectsCheck = new CheckBox("Reopen projects on startup");
        reopenProjectsCheck.setSelected(settings.isReopenProjectsOnStartup());
        styleCheck(reopenProjectsCheck);

        Label openProjInLabel = new Label("Open project in");
        openProjInLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        ToggleGroup openProjGroup = new ToggleGroup();
        openProjNewRadio = new RadioButton("New window");
        openProjCurrentRadio = new RadioButton("Current window");
        openProjAskRadio = new RadioButton("Ask");

        openProjNewRadio.setToggleGroup(openProjGroup);
        openProjCurrentRadio.setToggleGroup(openProjGroup);
        openProjAskRadio.setToggleGroup(openProjGroup);

        styleRadio(openProjNewRadio);
        styleRadio(openProjCurrentRadio);
        styleRadio(openProjAskRadio);

        switch (settings.getOpenProjectMode()) {
            case NEW_WINDOW -> openProjNewRadio.setSelected(true);
            case CURRENT_WINDOW -> openProjCurrentRadio.setSelected(true);
            case ASK -> openProjAskRadio.setSelected(true);
        }

        HBox openProjRow = new HBox(16, openProjInLabel, openProjNewRadio, openProjCurrentRadio, openProjAskRadio);
        openProjRow.setAlignment(Pos.CENTER_LEFT);

        Label defaultDirLabel = new Label("Default project directory:");
        defaultDirLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 160px;");

        defaultProjectDirField = new TextField(settings.getDefaultProjectDirectory());
        styleField(defaultProjectDirField);
        defaultProjectDirField.setPrefWidth(380);

        Button browseDirBtn = new Button("📁");
        browseDirBtn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 3 8 3 8;");
        browseDirBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Default Project Directory");
            File current = new File(defaultProjectDirField.getText().trim());
            if (current.isDirectory()) {
                chooser.setInitialDirectory(current);
            }
            File selected = chooser.showDialog(getScene() != null ? getScene().getWindow() : null);
            if (selected != null) {
                defaultProjectDirField.setText(selected.getAbsolutePath());
            }
        });

        HBox defaultDirRow = new HBox(8, defaultDirLabel, defaultProjectDirField, browseDirBtn);
        defaultDirRow.setAlignment(Pos.CENTER_LEFT);

        Label defaultDirHint = new Label("This directory is preselected in \"Open...\" and \"New | Project...\" dialogs.");
        defaultDirHint.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px; -fx-padding: 0 0 0 168;");

        VBox projectBox = new VBox(10, reopenProjectsCheck, openProjRow, defaultDirRow, defaultDirHint);

        // Autosave Section
        HBox autosaveSectionHeader = buildSectionHeader("Autosave");

        idleAutosaveCheck = new CheckBox("Save files if the IDE is idle for");
        idleAutosaveCheck.setSelected(settings.isIdleAutosaveEnabled());
        styleCheck(idleAutosaveCheck);

        idleSecondsField = new TextField(String.valueOf(settings.getIdleAutosaveSeconds()));
        styleField(idleSecondsField);
        idleSecondsField.setPrefWidth(46);
        idleSecondsField.setAlignment(Pos.CENTER);

        Label secondsLabel = new Label("seconds");
        secondsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox idleRow = new HBox(8, idleAutosaveCheck, idleSecondsField, secondsLabel);
        idleRow.setAlignment(Pos.CENTER_LEFT);

        focusLostAutosaveCheck = new CheckBox("Save files when switching to a different application or a built-in terminal");
        focusLostAutosaveCheck.setSelected(settings.isSaveOnFocusLost());
        styleCheck(focusLostAutosaveCheck);

        backupFilesCheck = new CheckBox("Back up files before saving");
        backupFilesCheck.setSelected(settings.isBackupFilesBeforeSaving());
        styleCheck(backupFilesCheck);

        Label syncExternalLabel = new Label("Sync external changes:");
        syncExternalLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: normal;");

        syncOnFocusCheck = new CheckBox("When switching to the IDE window or opening an editor tab");
        syncOnFocusCheck.setSelected(settings.isSyncExternalOnFocus());
        styleCheck(syncOnFocusCheck);

        syncPeriodicallyCheck = new CheckBox("Periodically when the IDE is inactive (experimental)");
        syncPeriodicallyCheck.setSelected(settings.isSyncExternalPeriodically());
        styleCheck(syncPeriodicallyCheck);

        VBox syncBox = new VBox(8, syncExternalLabel, indent(syncOnFocusCheck, 18), indent(syncPeriodicallyCheck, 18));
        syncBox.setPadding(new Insets(4, 0, 4, 0));

        HBox footerBox = new HBox(4);
        footerBox.setAlignment(Pos.CENTER_LEFT);
        Label autosaveNote = new Label("Autosave cannot be disabled completely.");
        autosaveNote.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 12px;");
        Hyperlink howItWorks = new Hyperlink("How it works");
        howItWorks.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-padding: 0; -fx-underline: false;");
        footerBox.getChildren().addAll(autosaveNote, howItWorks);

        VBox autosaveBox = new VBox(10, idleRow, focusLostAutosaveCheck, backupFilesCheck, syncBox, footerBox);

        root.getChildren().addAll(
                confirmExitCheck,
                procRow,
                projectSectionHeader,
                projectBox,
                autosaveSectionHeader,
                autosaveBox
        );
        return root;
    }

    // ============================================================
    // 2. Date Formats Page - Matches Image 3
    // ============================================================
    private Node buildDateFormatsPage() {
        VBox root = new VBox(14);
        root.setStyle("-fx-background-color: #1E1F22;");

        overrideDateFormatCheck = new CheckBox("Override system date and time format");
        overrideDateFormatCheck.setSelected(settings.isOverrideSystemDateFormat());
        styleCheck(overrideDateFormatCheck);

        dateFormatOptionsBox = new VBox(10);
        dateFormatOptionsBox.setPadding(new Insets(2, 0, 10, 20));

        Label formatLabel = new Label("Date format:");
        formatLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 90px;");

        dateFormatCombo = new ComboBox<>();
        dateFormatCombo.setEditable(true);
        dateFormatCombo.getItems().addAll("dd MMM yyyy", "yyyy-MM-dd", "MM/dd/yyyy", "dd/MM/yyyy", "d MMM yyyy");
        dateFormatCombo.setValue(settings.getDateFormatPattern());
        styleCombo(dateFormatCombo);
        dateFormatCombo.setPrefWidth(180);

        HBox formatRow = new HBox(12, formatLabel, dateFormatCombo);
        formatRow.setAlignment(Pos.CENTER_LEFT);

        use24HourTimeCheck = new CheckBox("Use 24-hour time");
        use24HourTimeCheck.setSelected(settings.isUse24HourTime());
        styleCheck(use24HourTimeCheck);

        dateFormatPreviewLabel = new Label(updateDatePreview());
        dateFormatPreviewLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 13px; -fx-padding: 4 0 0 0;");

        // Live preview listeners
        dateFormatCombo.valueProperty().addListener((obs, old, v) -> dateFormatPreviewLabel.setText(updateDatePreview()));
        if (dateFormatCombo.getEditor() != null) {
            dateFormatCombo.getEditor().textProperty().addListener((obs, old, v) -> dateFormatPreviewLabel.setText(updateDatePreview()));
        }
        use24HourTimeCheck.selectedProperty().addListener((obs, old, v) -> dateFormatPreviewLabel.setText(updateDatePreview()));

        dateFormatOptionsBox.getChildren().addAll(formatRow, use24HourTimeCheck, dateFormatPreviewLabel);
        dateFormatOptionsBox.setDisable(!overrideDateFormatCheck.isSelected());
        overrideDateFormatCheck.selectedProperty().addListener((obs, old, sel) -> dateFormatOptionsBox.setDisable(!sel));

        prettyFormattingCheck = new CheckBox("Use pretty formatting");
        prettyFormattingCheck.setSelected(settings.isUsePrettyFormatting());
        styleCheck(prettyFormattingCheck);

        Label prettyDesc = new Label("Replace numeric date with Today, Yesterday, and 10 minutes ago");
        prettyDesc.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 12px; -fx-padding: 0 0 0 24;");

        root.getChildren().addAll(
                overrideDateFormatCheck,
                dateFormatOptionsBox,
                prettyFormattingCheck,
                prettyDesc
        );
        return root;
    }

    private String updateDatePreview() {
        String pat = dateFormatCombo != null && dateFormatCombo.getValue() != null && !dateFormatCombo.getValue().isBlank()
                ? dateFormatCombo.getValue().trim() : "dd MMM yyyy";
        boolean use24 = use24HourTimeCheck == null || use24HourTimeCheck.isSelected();
        String timePart = use24 ? "HH:mm" : "hh:mm a";
        LocalDateTime sample = LocalDateTime.of(2100, 12, 31, 23, 59);
        try {
            return sample.format(DateTimeFormatter.ofPattern(pat + " " + timePart, Locale.getDefault()));
        } catch (Exception e) {
            return "31 Dec 2100 " + (use24 ? "23:59" : "11:59 PM");
        }
    }

    // ============================================================
    // 3. Data Sharing Page - Matches Image 4
    // ============================================================
    private Node buildDataSharingPage() {
        VBox root = new VBox(14);
        root.setStyle("-fx-background-color: #1E1F22;");

        Label headerText = new Label("Help shape the future of JetBrains products. By sharing your data and usage statistics, you allow us to better understand how you use our tools and how we can improve them. Learn more here ↗.");
        headerText.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 13px;");
        headerText.setWrapText(true);

        // Section 1
        HBox allProductsHeader = buildSectionHeader("Applied to All Installed JetBrains Products");

        sendAnonymousCheck = new CheckBox("Send anonymous usage statistics");
        sendAnonymousCheck.setSelected(settings.isSendAnonymousStats());
        styleCheck(sendAnonymousCheck);

        Label anonDesc1 = new Label("This information includes, but is not limited to, anonymous data about your feature and plugin usage, hardware and software configuration, file type statistics, and the number of files per project.");
        anonDesc1.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 12px;");
        anonDesc1.setWrapText(true);

        Label anonDesc2 = new Label("No personal data or sensitive information, such as source code or file names, is shared with us.\nLearn more here ↗.");
        anonDesc2.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 12px;");
        anonDesc2.setWrapText(true);

        VBox anonBox = new VBox(6, sendAnonymousCheck, indent(anonDesc1, 24), indent(anonDesc2, 24));

        // Section 2
        HBox currentIdeHeader = buildSectionHeader("Applied Only to Current IDE");

        sendDetailedCheck = new CheckBox("Send detailed code-related data");
        sendDetailedCheck.setSelected(settings.isSendDetailedData());
        styleCheck(sendDetailedCheck);

        Label detailedDesc = new Label("This includes an expanded range of IDE data with associated code snippets, such as AI feature usage, run configurations, and terminal commands. This data will be used for product improvement and model training purposes. Find more details here ↗.");
        detailedDesc.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 12px;");
        detailedDesc.setWrapText(true);

        VBox detailedBox = new VBox(6, sendDetailedCheck, indent(detailedDesc, 24));

        root.getChildren().addAll(
                headerText,
                allProductsHeader,
                anonBox,
                currentIdeHeader,
                detailedBox
        );
        return root;
    }

    // ============================================================
    // 4. HTTP Proxy Page - Matches Image 5
    // ============================================================
    private Node buildHttpProxyPage() {
        VBox root = new VBox(14);
        root.setStyle("-fx-background-color: #1E1F22;");

        ToggleGroup proxyGroup = new ToggleGroup();
        noProxyRadio = new RadioButton("No proxy");
        autoDetectProxyRadio = new RadioButton("Auto-detect proxy settings");
        manualProxyRadio = new RadioButton("Manual proxy configuration");

        noProxyRadio.setToggleGroup(proxyGroup);
        autoDetectProxyRadio.setToggleGroup(proxyGroup);
        manualProxyRadio.setToggleGroup(proxyGroup);

        styleRadio(noProxyRadio);
        styleRadio(autoDetectProxyRadio);
        styleRadio(manualProxyRadio);

        switch (settings.getProxyType()) {
            case NO_PROXY -> noProxyRadio.setSelected(true);
            case AUTO_DETECT -> autoDetectProxyRadio.setSelected(true);
            case MANUAL -> manualProxyRadio.setSelected(true);
        }

        // Auto-detect sub block
        autoConfigUrlCheck = new CheckBox("Automatic proxy configuration URL:");
        autoConfigUrlCheck.setSelected(settings.isAutoConfigUrlEnabled());
        styleCheck(autoConfigUrlCheck);

        autoConfigUrlField = new TextField(settings.getAutoConfigUrl());
        autoConfigUrlField.setPromptText("https://example.com/wpad.pac");
        styleField(autoConfigUrlField);
        autoConfigUrlField.setPrefWidth(420);

        HBox autoUrlRow = new HBox(8, autoConfigUrlCheck, autoConfigUrlField);
        autoUrlRow.setAlignment(Pos.CENTER_LEFT);

        Label autoUrlHint = new Label("Example: https://example.com/wpad.pac");
        autoUrlHint.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px; -fx-padding: 0 0 0 250;");

        clearPasswordsBtn = new Button("Clear Passwords");
        styleSecondaryButton(clearPasswordsBtn);
        clearPasswordsBtn.setOnAction(e -> {
            settings.clearProxyPasswords();
            if (proxyPasswordField != null) proxyPasswordField.clear();
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Proxy passwords have been cleared.", ButtonType.OK);
            alert.initOwner(getScene() != null ? getScene().getWindow() : null);
            alert.showAndWait();
        });

        autoDetectBox = new VBox(8, autoUrlRow, autoUrlHint, clearPasswordsBtn);
        autoDetectBox.setPadding(new Insets(2, 0, 10, 20));

        // Manual proxy sub block
        ToggleGroup protoGroup = new ToggleGroup();
        manualHttpRadio = new RadioButton("HTTP");
        manualSocksRadio = new RadioButton("SOCKS");
        manualHttpRadio.setToggleGroup(protoGroup);
        manualSocksRadio.setToggleGroup(protoGroup);
        styleRadio(manualHttpRadio);
        styleRadio(manualSocksRadio);

        if (settings.getManualProtocol() == SystemSettings.ManualProtocol.SOCKS) {
            manualSocksRadio.setSelected(true);
        } else {
            manualHttpRadio.setSelected(true);
        }

        HBox protoRow = new HBox(16, manualHttpRadio, manualSocksRadio);
        protoRow.setAlignment(Pos.CENTER_LEFT);

        Label hostLabel = new Label("Host name:");
        hostLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 90px;");
        proxyHostField = new TextField(settings.getProxyHost());
        styleField(proxyHostField);
        proxyHostField.setPrefWidth(320);
        HBox hostRow = new HBox(10, hostLabel, proxyHostField);
        hostRow.setAlignment(Pos.CENTER_LEFT);

        Label portLabel = new Label("Port number:");
        portLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 90px;");
        proxyPortField = new TextField(String.valueOf(settings.getProxyPort()));
        styleField(proxyPortField);
        proxyPortField.setPrefWidth(70);
        HBox portRow = new HBox(10, portLabel, proxyPortField);
        portRow.setAlignment(Pos.CENTER_LEFT);

        Label noProxyLabel = new Label("No proxy for:");
        noProxyLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 90px;");
        noProxyForField = new TextField(settings.getNoProxyFor());
        styleField(noProxyForField);
        noProxyForField.setPrefWidth(380);
        HBox noProxyRow = new HBox(10, noProxyLabel, noProxyForField);
        noProxyRow.setAlignment(Pos.CENTER_LEFT);

        Label noProxyHint = new Label("Example: *.example.com, 192.168.*");
        noProxyHint.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px; -fx-padding: 0 0 0 100;");

        // Proxy Authentication
        proxyAuthCheck = new CheckBox("Proxy authentication");
        proxyAuthCheck.setSelected(settings.isProxyAuthEnabled());
        styleCheck(proxyAuthCheck);

        Label loginLabel = new Label("Login:");
        loginLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 70px;");
        proxyLoginField = new TextField(settings.getProxyLogin());
        styleField(proxyLoginField);
        proxyLoginField.setPrefWidth(220);
        HBox loginRow = new HBox(10, loginLabel, proxyLoginField);
        loginRow.setAlignment(Pos.CENTER_LEFT);

        Label passLabel = new Label("Password:");
        passLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 70px;");
        proxyPasswordField = new PasswordField();
        proxyPasswordField.setText(settings.getProxyPassword());
        styleField(proxyPasswordField);
        proxyPasswordField.setPrefWidth(220);
        HBox passRow = new HBox(10, passLabel, proxyPasswordField);
        passRow.setAlignment(Pos.CENTER_LEFT);

        proxyRememberCheck = new CheckBox("Remember");
        proxyRememberCheck.setSelected(settings.isProxyRemember());
        styleCheck(proxyRememberCheck);

        authFieldsBox = new VBox(8, loginRow, passRow, proxyRememberCheck);
        authFieldsBox.setPadding(new Insets(2, 0, 4, 20));
        authFieldsBox.setDisable(!proxyAuthCheck.isSelected());
        proxyAuthCheck.selectedProperty().addListener((obs, old, sel) -> authFieldsBox.setDisable(!sel));

        manualProxyBox = new VBox(10, protoRow, hostRow, portRow, noProxyRow, noProxyHint, proxyAuthCheck, authFieldsBox);
        manualProxyBox.setPadding(new Insets(2, 0, 10, 20));

        // Radio group interaction
        autoDetectBox.setDisable(!autoDetectProxyRadio.isSelected());
        manualProxyBox.setDisable(!manualProxyRadio.isSelected());

        proxyGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            autoDetectBox.setDisable(sel != autoDetectProxyRadio);
            manualProxyBox.setDisable(sel != manualProxyRadio);
        });

        // Bottom Check Connection button
        checkConnectionBtn = new Button("Check Connection");
        styleSecondaryButton(checkConnectionBtn);
        checkConnectionBtn.setOnAction(e -> showCheckConnectionDialog());

        root.getChildren().addAll(
                noProxyRadio,
                autoDetectProxyRadio,
                autoDetectBox,
                manualProxyRadio,
                manualProxyBox,
                checkConnectionBtn
        );
        return root;
    }

    private void showCheckConnectionDialog() {
        Stage dialog = new Stage();
        dialog.initOwner(getScene() != null ? getScene().getWindow() : null);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Check Proxy Connection");

        VBox content = new VBox(12);
        content.setPadding(new Insets(16, 20, 16, 20));
        content.setStyle("-fx-background-color: #1E1F22;");

        Label label = new Label("Specify URL to check connection:");
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        TextField urlField = new TextField("https://jetbrains.com");
        styleField(urlField);
        urlField.setPrefWidth(320);

        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size: 12px;");

        Button testBtn = new Button("Test");
        testBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-padding: 5 16 5 16; -fx-background-radius: 4; -fx-cursor: hand;");

        Button closeBtn = new Button("Close");
        styleSecondaryButton(closeBtn);
        closeBtn.setOnAction(e -> dialog.close());

        testBtn.setOnAction(e -> {
            testBtn.setDisable(true);
            statusLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
            statusLabel.setText("Connecting...");

            saveToSettings(); // sync form data before testing

            new Thread(() -> {
                SystemSettings.ProxyTestResult result = settings.checkConnection(urlField.getText().trim());
                Platform.runLater(() -> {
                    testBtn.setDisable(false);
                    if (result.success()) {
                        statusLabel.setStyle("-fx-text-fill: #62B543; -fx-font-size: 12px;");
                        statusLabel.setText("Success! " + result.message() + " in " + result.responseTimeMs() + " ms");
                    } else {
                        statusLabel.setStyle("-fx-text-fill: #ED6C63; -fx-font-size: 12px;");
                        statusLabel.setText("Failed: " + result.message() + " (" + result.responseTimeMs() + " ms)");
                    }
                });
            }).start();
        });

        HBox buttons = new HBox(8, testBtn, closeBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        content.getChildren().addAll(label, urlField, statusLabel, buttons);
        Scene scene = new Scene(content, 420, 180);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // ============================================================
    // Save / Sync Logic
    // ============================================================
    public void save() {
        saveToSettings();
        settings.save();
    }

    private void saveToSettings() {
        // System Settings
        if (confirmExitCheck != null) settings.setConfirmExit(confirmExitCheck.isSelected());
        if (procTerminateRadio != null && procTerminateRadio.isSelected()) {
            settings.setProcessClosePolicy(SystemSettings.ProcessClosePolicy.TERMINATE);
        } else if (procDisconnectRadio != null && procDisconnectRadio.isSelected()) {
            settings.setProcessClosePolicy(SystemSettings.ProcessClosePolicy.DISCONNECT);
        } else if (procAskRadio != null && procAskRadio.isSelected()) {
            settings.setProcessClosePolicy(SystemSettings.ProcessClosePolicy.ASK);
        }

        if (reopenProjectsCheck != null) settings.setReopenProjectsOnStartup(reopenProjectsCheck.isSelected());
        if (openProjNewRadio != null && openProjNewRadio.isSelected()) {
            settings.setOpenProjectMode(SystemSettings.OpenProjectMode.NEW_WINDOW);
        } else if (openProjCurrentRadio != null && openProjCurrentRadio.isSelected()) {
            settings.setOpenProjectMode(SystemSettings.OpenProjectMode.CURRENT_WINDOW);
        } else if (openProjAskRadio != null && openProjAskRadio.isSelected()) {
            settings.setOpenProjectMode(SystemSettings.OpenProjectMode.ASK);
        }

        if (defaultProjectDirField != null) {
            String dir = defaultProjectDirField.getText().trim();
            if (!dir.isBlank()) settings.setDefaultProjectDirectory(dir);
        }

        if (idleAutosaveCheck != null) settings.setIdleAutosaveEnabled(idleAutosaveCheck.isSelected());
        if (idleSecondsField != null) {
            try {
                int secs = Math.max(1, Integer.parseInt(idleSecondsField.getText().trim()));
                settings.setIdleAutosaveSeconds(secs);
            } catch (Exception ignored) {}
        }
        if (focusLostAutosaveCheck != null) settings.setSaveOnFocusLost(focusLostAutosaveCheck.isSelected());
        if (backupFilesCheck != null) settings.setBackupFilesBeforeSaving(backupFilesCheck.isSelected());
        if (syncOnFocusCheck != null) settings.setSyncExternalOnFocus(syncOnFocusCheck.isSelected());
        if (syncPeriodicallyCheck != null) settings.setSyncExternalPeriodically(syncPeriodicallyCheck.isSelected());

        // Date Formats
        if (overrideDateFormatCheck != null) settings.setOverrideSystemDateFormat(overrideDateFormatCheck.isSelected());
        if (dateFormatCombo != null && dateFormatCombo.getValue() != null && !dateFormatCombo.getValue().isBlank()) {
            settings.setDateFormatPattern(dateFormatCombo.getValue().trim());
        }
        if (use24HourTimeCheck != null) settings.setUse24HourTime(use24HourTimeCheck.isSelected());
        if (prettyFormattingCheck != null) settings.setUsePrettyFormatting(prettyFormattingCheck.isSelected());

        // Data Sharing
        if (sendAnonymousCheck != null) settings.setSendAnonymousStats(sendAnonymousCheck.isSelected());
        if (sendDetailedCheck != null) settings.setSendDetailedData(sendDetailedCheck.isSelected());

        // HTTP Proxy
        if (noProxyRadio != null && noProxyRadio.isSelected()) {
            settings.setProxyType(SystemSettings.ProxyType.NO_PROXY);
        } else if (autoDetectProxyRadio != null && autoDetectProxyRadio.isSelected()) {
            settings.setProxyType(SystemSettings.ProxyType.AUTO_DETECT);
        } else if (manualProxyRadio != null && manualProxyRadio.isSelected()) {
            settings.setProxyType(SystemSettings.ProxyType.MANUAL);
        }

        if (autoConfigUrlCheck != null) settings.setAutoConfigUrlEnabled(autoConfigUrlCheck.isSelected());
        if (autoConfigUrlField != null) settings.setAutoConfigUrl(autoConfigUrlField.getText().trim());

        if (manualSocksRadio != null && manualSocksRadio.isSelected()) {
            settings.setManualProtocol(SystemSettings.ManualProtocol.SOCKS);
        } else if (manualHttpRadio != null && manualHttpRadio.isSelected()) {
            settings.setManualProtocol(SystemSettings.ManualProtocol.HTTP);
        }

        if (proxyHostField != null) settings.setProxyHost(proxyHostField.getText().trim());
        if (proxyPortField != null) {
            try {
                int p = Integer.parseInt(proxyPortField.getText().trim());
                settings.setProxyPort(p);
            } catch (Exception ignored) {}
        }
        if (noProxyForField != null) settings.setNoProxyFor(noProxyForField.getText().trim());
        if (proxyAuthCheck != null) settings.setProxyAuthEnabled(proxyAuthCheck.isSelected());
        if (proxyLoginField != null) settings.setProxyLogin(proxyLoginField.getText().trim());
        if (proxyPasswordField != null) settings.setProxyPassword(proxyPasswordField.getText());
        if (proxyRememberCheck != null) settings.setProxyRemember(proxyRememberCheck.isSelected());
    }

    // ============================================================
    // Additional Sub-Pages
    // ============================================================
    private Node buildLanguageRegionPage() {
        VBox page = new VBox(14);
        page.setStyle("-fx-background-color: #1E1F22;");

        Label languageLabel = new Label("Language:");
        languageLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 90px;");
        ComboBox<String> languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll("English", "中文", "日本語", "한국어", "Français", "Deutsch", "Español");
        languageCombo.getSelectionModel().selectFirst();
        styleCombo(languageCombo);
        languageCombo.setPrefWidth(200);

        Label restartHint = new Label("Requires restart");
        restartHint.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 12px;");

        HBox languageRow = new HBox(12, languageLabel, languageCombo, restartHint);
        languageRow.setAlignment(Pos.CENTER_LEFT);

        Label regionLabel = new Label("Region:");
        regionLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 90px;");
        ComboBox<String> regionCombo = new ComboBox<>();
        regionCombo.getItems().addAll("Not specified", "United States", "United Kingdom", "Germany", "France", "Japan", "China");
        regionCombo.getSelectionModel().selectFirst();
        styleCombo(regionCombo);
        regionCombo.setPrefWidth(200);

        HBox regionRow = new HBox(12, regionLabel, regionCombo);
        regionRow.setAlignment(Pos.CENTER_LEFT);

        Label regionDesc = new Label("Select a region to ensure that licensing, Marketplace, and other region-specific features and links work correctly. Requires restart. See the documentation for details.");
        regionDesc.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 12px;");
        regionDesc.setWrapText(true);

        page.getChildren().addAll(languageRow, regionRow, regionDesc);
        return page;
    }

    private Node buildPasswordsPage() {
        VBox page = new VBox(14);
        page.setStyle("-fx-background-color: #1E1F22;");

        Label saveLabel = new Label("Save passwords:");
        saveLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        RadioButton nativeKeychain = new RadioButton("In native Keychain");
        nativeKeychain.setSelected(true);
        styleRadio(nativeKeychain);

        RadioButton keepass = new RadioButton("In KeePass");
        styleRadio(keepass);

        VBox keepassBox = new VBox(6);
        keepassBox.setPadding(new Insets(4, 0, 8, 20));

        Label dbLabel = new Label("Database:");
        dbLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        TextField dbField = new TextField(System.getProperty("user.home") + "/.config/Lumina/c.kdbx");
        styleField(dbField);
        dbField.setPrefWidth(420);
        dbField.setEditable(false);

        Label weakEncryption = new Label("Stored using standard encryption. Storing on an encrypted volume is recommended for additional security.");
        weakEncryption.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 12px;");
        weakEncryption.setWrapText(true);

        CheckBox pgpKey = new CheckBox("Protect master password using PGP key (No keys configured)");
        styleCheck(pgpKey);

        keepassBox.getChildren().addAll(dbLabel, dbField, weakEncryption, pgpKey);

        RadioButton noSave = new RadioButton("Do not save, forget passwords after restart");
        styleRadio(noSave);

        ToggleGroup saveGroup = new ToggleGroup();
        nativeKeychain.setToggleGroup(saveGroup);
        keepass.setToggleGroup(saveGroup);
        noSave.setToggleGroup(saveGroup);

        page.getChildren().addAll(saveLabel, nativeKeychain, keepass, keepassBox, noSave);
        return page;
    }

    private Node buildProcessElevationPage() {
        VBox page = new VBox(14);
        page.setStyle("-fx-background-color: #1E1F22;");

        Label description = new Label("Running privileged processes requires 'sudo' authorization.");
        description.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        Label description2 = new Label("Lumina utilizes a service process to do this. You can set it to keep running for a certain amount of time so you don't have to authorize it again each time you run or debug.");
        description2.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 12px;");
        description2.setWrapText(true);

        Label important = new Label("Important: Enabling this option grants the IDE unrestricted access to your system.");
        important.setStyle("-fx-text-fill: #E88A8A; -fx-font-size: 12px;");
        important.setWrapText(true);

        CheckBox keepSudo = new CheckBox("Keep 'sudo' authorization for 15 min");
        styleCheck(keepSudo);

        CheckBox extendTimeout = new CheckBox("Extend the time limit when starting a new process");
        extendTimeout.setSelected(true);
        styleCheck(extendTimeout);

        Label extendDesc = new Label("The timeout will reset each time a new elevated process is launched within the specified time frame.");
        extendDesc.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 12px; -fx-padding: 0 0 0 24;");
        extendDesc.setWrapText(true);

        page.getChildren().addAll(description, description2, important, keepSudo, extendTimeout, extendDesc);
        return page;
    }

    private Node buildServerCertificatesPage() {
        VBox page = new VBox(14);
        page.setStyle("-fx-background-color: #1E1F22;");

        CheckBox acceptNonTrusted = new CheckBox("Accept non-trusted certificates automatically");
        styleCheck(acceptNonTrusted);

        Label acceptedLabel = new Label("Accepted certificates:");
        acceptedLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        ListView<String> certList = new ListView<>();
        certList.getItems().addAll("Default Root CA", "Java Default TrustStore");
        certList.setPrefHeight(180);
        certList.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label noCertSelected = new Label("No custom certificate installed");
        noCertSelected.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 12px;");

        page.getChildren().addAll(acceptNonTrusted, acceptedLabel, certList, noCertSelected);
        return page;
    }

    private Node buildTrustedHostsPage() {
        VBox page = new VBox(14);
        page.setStyle("-fx-background-color: #1E1F22;");

        Label description = new Label("These hosts are trusted for downloading plugins, SDKs, and updates. No confirmation is required.");
        description.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        description.setWrapText(true);

        ListView<String> hostList = new ListView<>();
        hostList.getItems().addAll(
                "repo.maven.apache.org",
                "repo1.maven.org",
                "download.oracle.com",
                "github.com",
                "plugins.jetbrains.com"
        );
        hostList.setPrefHeight(140);
        hostList.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4;");

        page.getChildren().addAll(description, hostList);
        return page;
    }

    private Node buildUpdatesPage() {
        VBox page = new VBox(14);
        page.setStyle("-fx-background-color: #1E1F22;");

        Label versionLabel = new Label("Current version: Lumina IDE 0.1.0");
        versionLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        CheckBox checkIdeUpdates = new CheckBox("Check IDE updates for: Stable Releases");
        checkIdeUpdates.setSelected(true);
        styleCheck(checkIdeUpdates);

        CheckBox checkPluginUpdates = new CheckBox("Check for plugin updates");
        checkPluginUpdates.setSelected(true);
        styleCheck(checkPluginUpdates);

        CheckBox updatePluginsAuto = new CheckBox("Update plugins automatically");
        updatePluginsAuto.setSelected(false);
        styleCheck(updatePluginsAuto);

        HBox checkButtons = new HBox(12);
        checkButtons.setAlignment(Pos.CENTER_LEFT);
        Button checkUpdates = new Button("Check for Updates...");
        styleSecondaryButton(checkUpdates);
        Label lastChecked = new Label("Last checked: Today");
        lastChecked.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 12px;");
        checkButtons.getChildren().addAll(checkUpdates, lastChecked);

        page.getChildren().addAll(versionLabel, checkIdeUpdates, checkPluginUpdates, updatePluginsAuto, checkButtons);
        return page;
    }

    private Node buildPlaceholderPage(String pageName) {
        VBox page = new VBox(16);
        Label placeholder = new Label("Settings for '" + pageName + "' will be available in a future update.");
        placeholder.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 13px;");
        page.getChildren().add(placeholder);
        return page;
    }

    // ============================================================
    // Style Helpers
    // ============================================================
    private HBox buildSectionHeader(String title) {
        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: normal;");

        Region divider = new Region();
        divider.setStyle("-fx-background-color: #393B40; -fx-pref-height: 1px; -fx-max-height: 1px; -fx-min-height: 1px;");
        HBox.setHgrow(divider, Priority.ALWAYS);

        HBox header = new HBox(12, label, divider);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 0, 4, 0));
        return header;
    }

    private void styleCheck(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");
    }

    private void styleRadio(RadioButton rb) {
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");
    }

    private void styleField(TextField tf) {
        tf.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 4 8 4 8;");
    }

    private void styleCombo(ComboBox<String> cb) {
        cb.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
    }

    private void styleSecondaryButton(Button btn) {
        btn.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 5 14 5 14; -fx-background-radius: 4; -fx-border-radius: 4; -fx-cursor: hand;");
    }

    private Node indent(Node node, double leftPadding) {
        VBox box = new VBox(node);
        box.setPadding(new Insets(0, 0, 0, leftPadding));
        return box;
    }
}