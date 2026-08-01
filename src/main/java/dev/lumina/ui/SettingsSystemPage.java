// SettingsSystemPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

/**
 * IntelliJ-style System Settings page with all sub-pages.
 * Handles: Data Sharing, Date Formats, HTTP Proxy, Language and Region,
 * Passwords, Process Elevation, Server Certificates, Trusted Hosts, Updates.
 */
public class SettingsSystemPage extends VBox {

    private VBox contentArea;
    private String currentSubPage = "Data Sharing";

    public SettingsSystemPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(12);

        // Show Data Sharing by default
        showSubPage("Data Sharing");
    }

    public void showSubPage(String pageName) {
        this.currentSubPage = pageName;
        getChildren().clear();

        Label title = new Label("Appearance & Behavior → System Settings → " + pageName);
        title.getStyleClass().add("settings-page-title");

        contentArea = new VBox(16);
        contentArea.getStyleClass().add("settings-content");

        switch (pageName) {
            case "Data Sharing" -> buildDataSharingPage();
            case "Date Formats" -> buildDateFormatsPage();
            case "HTTP Proxy" -> buildHttpProxyPage();
            case "Language and Region" -> buildLanguageRegionPage();
            case "Passwords" -> buildPasswordsPage();
            case "Process Elevation" -> buildProcessElevationPage();
            case "Server Certificates" -> buildServerCertificatesPage();
            case "Trusted Hosts" -> buildTrustedHostsPage();
            case "Updates" -> buildUpdatesPage();
            default -> buildPlaceholderPage(pageName);
        }

        ScrollPane scroll = new ScrollPane(contentArea);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("settings-scroll");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(title, scroll);
    }

    // ============================================================
    // Data Sharing Page
    // ============================================================
    private void buildDataSharingPage() {
        VBox page = new VBox(16);

        Label header = new Label("Help shape the future of JetBrains products.");
        header.getStyleClass().add("settings-label");
        header.setWrapText(true);

        Label subHeader = new Label("By sharing your data and usage statistics, you allow us to better understand how you use our tools and how we can improve them.");
        subHeader.getStyleClass().add("settings-hint");
        subHeader.setWrapText(true);

        Hyperlink learnMore = new Hyperlink("Learn more here");
        learnMore.getStyleClass().add("settings-link");

        VBox headerBox = new VBox(6, header, subHeader, learnMore);
        headerBox.setPadding(new Insets(0, 0, 8, 0));

        // Applied to All Installed JetBrains Products
        Label section1 = new Label("Applied to All Installed JetBrains Products");
        section1.getStyleClass().add("settings-section");

        CheckBox sendAnonymous = new CheckBox("Send anonymous usage statistics");
        sendAnonymous.getStyleClass().add("settings-check");
        Label anonymousDesc = new Label("This information includes, but is not limited to, anonymous data about your feature and plugin usage, hardware and software configuration, file type statistics, and the number of files per project.");
        anonymousDesc.getStyleClass().add("settings-hint");
        anonymousDesc.setWrapText(true);
        Hyperlink learnMore2 = new Hyperlink("Learn more here");
        learnMore2.getStyleClass().add("settings-link");

        VBox anonymousBox = new VBox(4, sendAnonymous, anonymousDesc, learnMore2);
        anonymousBox.setPadding(new Insets(4, 0, 8, 20));

        // Applied Only to Current IDE
        Label section2 = new Label("Applied Only to Current IDE");
        section2.getStyleClass().add("settings-section");

        CheckBox sendDetailed = new CheckBox("Send detailed code-related data");
        sendDetailed.getStyleClass().add("settings-check");
        Label detailedDesc = new Label("This includes an expanded range of IDE data with associated code snippets, such as AI feature usage, run configurations, and terminal commands. This data will be used for product improvement and model training purposes.");
        detailedDesc.getStyleClass().add("settings-hint");
        detailedDesc.setWrapText(true);
        Hyperlink learnMore3 = new Hyperlink("Find more details here");
        learnMore3.getStyleClass().add("settings-link");

        VBox detailedBox = new VBox(4, sendDetailed, detailedDesc, learnMore3);
        detailedBox.setPadding(new Insets(4, 0, 0, 20));

        page.getChildren().addAll(headerBox, section1, anonymousBox, section2, detailedBox);
        contentArea.getChildren().add(page);
    }

    // ============================================================
    // Date Formats Page
    // ============================================================
    private void buildDateFormatsPage() {
        VBox page = new VBox(14);

        // Override system date and time format
        CheckBox overrideSystem = new CheckBox("Override system date and time format");
        overrideSystem.getStyleClass().add("settings-check");

        VBox overrideBox = new VBox(8);
        overrideBox.setPadding(new Insets(4, 0, 8, 20));

        HBox dateFormatRow = new HBox(12);
        dateFormatRow.setAlignment(Pos.CENTER_LEFT);
        Label dateLabel = new Label("Date format:");
        dateLabel.getStyleClass().add("settings-label");
        ComboBox<String> dateFormat = new ComboBox<>();
        dateFormat.getItems().addAll("dd MMM yyyy", "MM/dd/yyyy", "dd/MM/yyyy", "yyyy-MM-dd");
        dateFormat.getSelectionModel().selectFirst();
        dateFormat.getStyleClass().add("settings-combo");
        dateFormatRow.getChildren().addAll(dateLabel, dateFormat);

        CheckBox use24Hour = new CheckBox("Use 24-hour time");
        use24Hour.getStyleClass().add("settings-check");

        Label preview = new Label("31 Dec 2100 23:59");
        preview.getStyleClass().add("settings-value");

        overrideBox.getChildren().addAll(dateFormatRow, use24Hour, preview);

        // Date Formats section
        Label dateFormatsLabel = new Label("Date Formats");
        dateFormatsLabel.getStyleClass().add("settings-section");

        CheckBox prettyFormatting = new CheckBox("Use pretty formatting");
        prettyFormatting.getStyleClass().add("settings-check");
        Label prettyDesc = new Label("Replace numeric date with Today, Yesterday, and 10 minutes ago");
        prettyDesc.getStyleClass().add("settings-hint");
        prettyDesc.setPadding(new Insets(0, 0, 0, 20));

        page.getChildren().addAll(overrideSystem, overrideBox, dateFormatsLabel, prettyFormatting, prettyDesc);
        contentArea.getChildren().add(page);
    }

    // ============================================================
    // HTTP Proxy Page
    // ============================================================
    private void buildHttpProxyPage() {
        VBox page = new VBox(14);

        // No proxy
        RadioButton noProxy = new RadioButton("No proxy");
        noProxy.setSelected(true);
        noProxy.getStyleClass().add("settings-radio");

        // Auto-detect
        RadioButton autoDetect = new RadioButton("Auto-detect proxy settings");
        autoDetect.getStyleClass().add("settings-radio");

        // Auto proxy config URL
        RadioButton autoConfig = new RadioButton("Automatic proxy configuration URL:");
        autoConfig.getStyleClass().add("settings-radio");

        TextField autoConfigField = new TextField();
        autoConfigField.setPromptText("https://example.com/wpad.pac");
        autoConfigField.getStyleClass().add("text-field");
        autoConfigField.setPrefWidth(400);
        HBox autoConfigRow = new HBox(8, autoConfig, autoConfigField);
        autoConfigRow.setAlignment(Pos.CENTER_LEFT);
        autoConfigRow.setPadding(new Insets(4, 0, 0, 20));

        // Manual proxy configuration
        RadioButton manualProxy = new RadioButton("Manual proxy configuration");
        manualProxy.getStyleClass().add("settings-radio");

        VBox manualBox = new VBox(8);
        manualBox.setPadding(new Insets(4, 0, 0, 20));

        // HTTP
        Label httpLabel = new Label("HTTP:");
        httpLabel.getStyleClass().add("settings-label");
        TextField httpHost = new TextField();
        httpHost.setPromptText("Host name");
        httpHost.getStyleClass().add("text-field");
        httpHost.setPrefWidth(200);
        TextField httpPort = new TextField();
        httpPort.setPromptText("Port");
        httpPort.setPrefWidth(80);
        httpPort.getStyleClass().add("text-field");
        HBox httpRow = new HBox(8, httpLabel, httpHost, httpPort);
        httpRow.setAlignment(Pos.CENTER_LEFT);

        // SOCKS
        Label socksLabel = new Label("SOCKS:");
        socksLabel.getStyleClass().add("settings-label");
        TextField socksHost = new TextField();
        socksHost.setPromptText("Host name");
        socksHost.getStyleClass().add("text-field");
        socksHost.setPrefWidth(200);
        TextField socksPort = new TextField();
        socksPort.setPromptText("Port");
        socksPort.setPrefWidth(80);
        socksPort.getStyleClass().add("text-field");
        HBox socksRow = new HBox(8, socksLabel, socksHost, socksPort);
        socksRow.setAlignment(Pos.CENTER_LEFT);

        manualBox.getChildren().addAll(httpRow, socksRow);

        // No proxy for
        Label noProxyLabel = new Label("No proxy for:");
        noProxyLabel.getStyleClass().add("settings-label");
        TextField noProxyField = new TextField();
        noProxyField.setPromptText("localhost, *.example.com");
        noProxyField.getStyleClass().add("text-field");
        noProxyField.setPrefWidth(400);
        VBox noProxyBox = new VBox(4, noProxyLabel, noProxyField);

        // Proxy authentication
        CheckBox proxyAuth = new CheckBox("Proxy authentication");
        proxyAuth.getStyleClass().add("settings-check");

        VBox authBox = new VBox(8);
        authBox.setPadding(new Insets(4, 0, 0, 20));

        HBox loginRow = new HBox(8);
        loginRow.setAlignment(Pos.CENTER_LEFT);
        Label loginLabel = new Label("Login:");
        loginLabel.getStyleClass().add("settings-label");
        TextField loginField = new TextField();
        loginField.getStyleClass().add("text-field");
        loginField.setPrefWidth(200);
        loginRow.getChildren().addAll(loginLabel, loginField);

        HBox passwordRow = new HBox(8);
        passwordRow.setAlignment(Pos.CENTER_LEFT);
        Label passwordLabel = new Label("Password:");
        passwordLabel.getStyleClass().add("settings-label");
        PasswordField passwordField = new PasswordField();
        passwordField.getStyleClass().add("text-field");
        passwordField.setPrefWidth(200);
        passwordRow.getChildren().addAll(passwordLabel, passwordField);

        authBox.getChildren().addAll(loginRow, passwordRow);

        // Clear passwords and Check Connection buttons
        HBox buttonRow = new HBox(10);
        Button clearPasswords = new Button("Clear Passwords");
        clearPasswords.getStyleClass().add("dialog-secondary");
        Button checkConnection = new Button("Check Connection");
        checkConnection.getStyleClass().add("dialog-primary");
        buttonRow.getChildren().addAll(clearPasswords, checkConnection);

        ToggleGroup proxyGroup = new ToggleGroup();
        noProxy.setToggleGroup(proxyGroup);
        autoDetect.setToggleGroup(proxyGroup);
        autoConfig.setToggleGroup(proxyGroup);
        manualProxy.setToggleGroup(proxyGroup);

        page.getChildren().addAll(
            noProxy, autoDetect, autoConfigRow,
            manualProxy, manualBox, noProxyBox,
            proxyAuth, authBox, buttonRow
        );
        contentArea.getChildren().add(page);
    }

    // ============================================================
    // Language and Region Page
    // ============================================================
    private void buildLanguageRegionPage() {
        VBox page = new VBox(14);

        // Language
        Label languageLabel = new Label("Language:");
        languageLabel.getStyleClass().add("settings-label");
        ComboBox<String> languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll("English", "中文", "日本語", "한국어", "Français", "Deutsch", "Español");
        languageCombo.getSelectionModel().selectFirst();
        languageCombo.getStyleClass().add("settings-combo");
        languageCombo.setPrefWidth(200);

        Label restartHint = new Label("Requires restart");
        restartHint.getStyleClass().add("settings-hint");

        HBox languageRow = new HBox(12, languageLabel, languageCombo, restartHint);
        languageRow.setAlignment(Pos.CENTER_LEFT);

        // Region
        Label regionLabel = new Label("Region:");
        regionLabel.getStyleClass().add("settings-label");
        ComboBox<String> regionCombo = new ComboBox<>();
        regionCombo.getItems().addAll("Not specified", "United States", "United Kingdom", "Germany", "France", "Japan", "China");
        regionCombo.getSelectionModel().selectFirst();
        regionCombo.getStyleClass().add("settings-combo");
        regionCombo.setPrefWidth(200);

        HBox regionRow = new HBox(12, regionLabel, regionCombo);
        regionRow.setAlignment(Pos.CENTER_LEFT);

        Label regionDesc = new Label("Select a region to ensure that licensing, JetBrains Marketplace, and other region-specific features and links work correctly. Requires restart. See the documentation for details.");
        regionDesc.getStyleClass().add("settings-hint");
        regionDesc.setWrapText(true);

        page.getChildren().addAll(languageRow, regionRow, regionDesc);
        contentArea.getChildren().add(page);
    }

    // ============================================================
    // Passwords Page
    // ============================================================
    private void buildPasswordsPage() {
        VBox page = new VBox(14);

        Label saveLabel = new Label("Save passwords:");
        saveLabel.getStyleClass().add("settings-label");

        RadioButton nativeKeychain = new RadioButton("In native Keychain");
        nativeKeychain.setSelected(true);
        nativeKeychain.getStyleClass().add("settings-radio");

        RadioButton keepass = new RadioButton("In KeePass");
        keepass.getStyleClass().add("settings-radio");

        VBox keepassBox = new VBox(6);
        keepassBox.setPadding(new Insets(4, 0, 8, 20));

        Label dbLabel = new Label("Database:");
        dbLabel.getStyleClass().add("settings-label");
        TextField dbField = new TextField("/home/firoze/.config/JetBrains/IntelliJldea2025.3/c.kdbx");
        dbField.getStyleClass().add("text-field");
        dbField.setPrefWidth(450);
        dbField.setEditable(false);

        Label weakEncryption = new Label("Stored using weak encryption. It is recommended to store on encrypted volume for additional security.");
        weakEncryption.getStyleClass().add("settings-hint");
        weakEncryption.setWrapText(true);

        CheckBox pgpKey = new CheckBox("Protect master password using PGP key (No keys configured)");
        pgpKey.getStyleClass().add("settings-check");

        keepassBox.getChildren().addAll(dbLabel, dbField, weakEncryption, pgpKey);

        RadioButton noSave = new RadioButton("Do not save, forget passwords after restart");
        noSave.getStyleClass().add("settings-radio");

        ToggleGroup saveGroup = new ToggleGroup();
        nativeKeychain.setToggleGroup(saveGroup);
        keepass.setToggleGroup(saveGroup);
        noSave.setToggleGroup(saveGroup);

        page.getChildren().addAll(saveLabel, nativeKeychain, keepass, keepassBox, noSave);
        contentArea.getChildren().add(page);
    }

    // ============================================================
    // Process Elevation Page
    // ============================================================
    private void buildProcessElevationPage() {
        VBox page = new VBox(14);

        Label description = new Label("Running privileged processes requires 'sudo' authorization.");
        description.getStyleClass().add("settings-label");
        description.setWrapText(true);

        Label description2 = new Label("IntelliJ IDEA utilizes a special service process to do this. For your convenience, you can set it to keep running for a certain amount of time so that you don't have to authorize it again each time you run or debug.");
        description2.getStyleClass().add("settings-hint");
        description2.setWrapText(true);

        Label important = new Label("Important: Enabling this option grants the IDE and all its components, including third-party plugins, unrestricted access to your system.");
        important.getStyleClass().add("settings-hint");
        important.setWrapText(true);
        important.setStyle("-fx-text-fill: #E88A8A;");

        CheckBox keepSudo = new CheckBox("Keep 'sudo' authorization for 15 min");
        keepSudo.getStyleClass().add("settings-check");

        CheckBox extendTimeout = new CheckBox("Extend the time limit when starting a new process");
        extendTimeout.setSelected(true);
        extendTimeout.getStyleClass().add("settings-check");
        Label extendDesc = new Label("The timeout will reset each time a new elevated process is launched within the specified time frame.");
        extendDesc.getStyleClass().add("settings-hint");
        extendDesc.setPadding(new Insets(0, 0, 0, 20));
        extendDesc.setWrapText(true);

        page.getChildren().addAll(description, description2, important, keepSudo, extendTimeout, extendDesc);
        contentArea.getChildren().add(page);
    }

    // ============================================================
    // Server Certificates Page
    // ============================================================
    private void buildServerCertificatesPage() {
        VBox page = new VBox(14);

        CheckBox acceptNonTrusted = new CheckBox("Accept non-trusted certificates automatically");
        acceptNonTrusted.getStyleClass().add("settings-check");

        Label acceptedLabel = new Label("Accepted certificates:");
        acceptedLabel.getStyleClass().add("settings-label");

        ListView<String> certList = new ListView<>();
        certList.getItems().addAll(
            "Data Sharing",
            "Date Formats",
            "HTTP Proxy",
            "Language and Region",
            "Passwords",
            "Process Elevation",
            "Server Certificates",
            "Trusted Hosts",
            "Updates",
            "File Colors",
            "Scopes",
            "Notifications",
            "Data Editor and Viewer",
            "Quick Lists",
            "Required Plugins",
            "Trusted Locations",
            "Path Variables",
            "Presentation Assistant"
        );
        certList.getStyleClass().add("settings-list");
        certList.setPrefHeight(200);

        Label noCertSelected = new Label("No certificate selected");
        noCertSelected.getStyleClass().add("settings-hint");

        page.getChildren().addAll(acceptNonTrusted, acceptedLabel, certList, noCertSelected);
        contentArea.getChildren().add(page);
    }

    // ============================================================
    // Trusted Hosts Page
    // ============================================================
    private void buildTrustedHostsPage() {
        VBox page = new VBox(14);

        Label description = new Label("These hosts are trusted for downloading IDE distributions. No confirmation is required.");
        description.getStyleClass().add("settings-label");
        description.setWrapText(true);

        ListView<String> hostList = new ListView<>();
        hostList.getItems().addAll(
            "download.jetbrains.com",
            "download-cf.jetbrains.com",
            "download-cdn.jetbrains.com",
            "cache-redirector.jetbrains.com"
        );
        hostList.getStyleClass().add("settings-list");
        hostList.setPrefHeight(120);

        page.getChildren().addAll(description, hostList);
        contentArea.getChildren().add(page);
    }

    // ============================================================
    // Updates Page
    // ============================================================
    private void buildUpdatesPage() {
        VBox page = new VBox(14);

        Label versionLabel = new Label("Current version: IntelliJ IDEA 2025.3.2 IU-253.30387.90 January 22, 2026");
        versionLabel.getStyleClass().add("settings-label");
        versionLabel.setWrapText(true);

        CheckBox checkIdeUpdates = new CheckBox("Check IDE updates for: Stable Releases");
        checkIdeUpdates.setSelected(true);
        checkIdeUpdates.getStyleClass().add("settings-check");

        CheckBox checkPluginUpdates = new CheckBox("Check for plugin updates");
        checkPluginUpdates.setSelected(true);
        checkPluginUpdates.getStyleClass().add("settings-check");

        CheckBox updatePluginsAuto = new CheckBox("Update plugins automatically");
        updatePluginsAuto.getStyleClass().add("settings-check");
        Label updateAutoDesc = new Label("Updates will be downloaded in the background and applied after restart automatically");
        updateAutoDesc.getStyleClass().add("settings-hint");
        updateAutoDesc.setPadding(new Insets(0, 0, 0, 20));
        updateAutoDesc.setWrapText(true);

        HBox checkButtons = new HBox(10);
        Button checkUpdates = new Button("Check for Updates...");
        checkUpdates.getStyleClass().add("dialog-secondary");
        Label lastChecked = new Label("Last checked: Today 7:13 AM");
        lastChecked.getStyleClass().add("settings-hint");
        checkButtons.getChildren().addAll(checkUpdates, lastChecked);

        CheckBox showWhatsNew = new CheckBox("Show What's New in the editor after an IDE update");
        showWhatsNew.setSelected(true);
        showWhatsNew.getStyleClass().add("settings-check");

        CheckBox checkJdkUpdates = new CheckBox("Check for JDK updates");
        checkJdkUpdates.setSelected(true);
        checkJdkUpdates.getStyleClass().add("settings-check");

        Label toolboxLabel = new Label("We recommend the Toolbox App");
        toolboxLabel.getStyleClass().add("settings-label");
        Hyperlink toolboxLink = new Hyperlink("Toolbox App");
        toolboxLink.getStyleClass().add("settings-link");
        Label toolboxDesc = new Label("Get updates automatically, open your projects with one click, discover other JetBrains products, and more");
        toolboxDesc.getStyleClass().add("settings-hint");
        toolboxDesc.setWrapText(true);

        HBox toolboxRow = new HBox(6, toolboxLabel, toolboxLink);

        VBox pluginBox = new VBox(4, checkPluginUpdates, updatePluginsAuto, updateAutoDesc);
        pluginBox.setPadding(new Insets(4, 0, 8, 20));

        VBox toolboxBox = new VBox(4, toolboxRow, toolboxDesc);

        page.getChildren().addAll(
            versionLabel,
            checkIdeUpdates,
            pluginBox,
            checkButtons,
            showWhatsNew,
            checkJdkUpdates,
            toolboxBox
        );
        contentArea.getChildren().add(page);
    }

    // ============================================================
    // Placeholder
    // ============================================================
    private void buildPlaceholderPage(String pageName) {
        VBox page = new VBox(20);
        Label placeholder = new Label("Settings for '" + pageName + "' will be available in a future update.");
        placeholder.getStyleClass().add("settings-placeholder");
        page.getChildren().add(placeholder);
        contentArea.getChildren().add(page);
    }
}