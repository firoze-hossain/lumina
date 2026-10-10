package dev.lumina.ui;

import dev.lumina.tools.GitHubCopilotGeneralSettings;
import dev.lumina.tools.GitHubCopilotGeneralSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Tools > GitHub Copilot > General settings page in Lumina IDE matching 1:1 design of the reference IDE.
 */
public class SettingsToolsGitHubCopilotGeneralPage extends VBox {

    private final GitHubCopilotGeneralSettingsManager manager;
    private GitHubCopilotGeneralSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    private Hyperlink accountSettingsLink;
    private CheckBox screenReaderCheck;
    private ComboBox<String> updateChannelCombo;
    private CheckBox checkUpdatesCheck;
    private CheckBox preferDeviceCodeCheck;
    private TextField authProviderField;
    private CheckBox sendTelemetryCheck;

    public SettingsToolsGitHubCopilotGeneralPage() {
        this.manager = GitHubCopilotGeneralSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(20);
        setStyle("-fx-background-color: #1E1F22;");

        // 1. General section
        VBox generalSection = new VBox(8);
        Label generalHeader = createSectionHeader("General");
        accountSettingsLink = new Hyperlink("GitHub Copilot account settings");
        accountSettingsLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 0;");
        accountSettingsLink.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("GitHub Copilot");
            alert.setHeaderText(null);
            alert.setContentText("GitHub Copilot account authentication status is active.");
            alert.showAndWait();
        });
        generalSection.getChildren().addAll(generalHeader, accountSettingsLink);

        // 2. Accessibility section
        VBox accessSection = new VBox(8);
        Label accessHeader = createSectionHeader("Accessibility");
        screenReaderCheck = createCheckBox("Enable screen reader support");
        accessSection.getChildren().addAll(accessHeader, screenReaderCheck);

        // 3. Plugin section
        VBox pluginSection = new VBox(8);
        Label pluginHeader = createSectionHeader("Plugin");

        Label channelLabel = createFieldLabel("Update channel:");
        channelLabel.setPrefWidth(120);
        updateChannelCombo = new ComboBox<>();
        updateChannelCombo.getItems().addAll("Stable", "Beta", "Nightly");
        updateChannelCombo.setValue("Stable");
        updateChannelCombo.setPrefWidth(140);
        styleComboBox(updateChannelCombo);
        updateChannelCombo.valueProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });

        HBox channelRow = new HBox(12, channelLabel, updateChannelCombo);
        channelRow.setAlignment(Pos.CENTER_LEFT);

        checkUpdatesCheck = createCheckBox("Check for plugin updates");
        pluginSection.getChildren().addAll(pluginHeader, channelRow, checkUpdatesCheck);

        // 4. Authentication section
        VBox authSection = new VBox(8);
        Label authHeader = createSectionHeader("Authentication");

        preferDeviceCodeCheck = createCheckBox("Prefer device code to sign in");
        Label deviceCodeHint = createHintLabel("When true, prioritize the device code flow for authentication instead of other available flows. This is recommended for environments like WSL where the local server or URL handler flows may not work as expected.");
        deviceCodeHint.setPadding(new Insets(0, 0, 0, 22));

        Label authProviderLabel = createFieldLabel("Authentication provider");
        authProviderLabel.setPrefWidth(160);
        authProviderField = new TextField();
        authProviderField.setPrefWidth(220);
        styleTextField(authProviderField);
        authProviderField.textProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });

        HBox authProviderRow = new HBox(12, authProviderLabel, authProviderField);
        authProviderRow.setAlignment(Pos.CENTER_LEFT);

        Label providerHint = createHintLabel(
                "Enter the URI for your GHE.com or GitHub Enterprise Server instance. For example:\n\n" +
                " • GHE.com: https://octocat.ghe.com\n" +
                " • GitHub Enterprise Server: https://github.octocat.com\n\n" +
                "Note: This should not be set to a GitHub.com URI. If your account exists on GitHub.com or is a GitHub Enterprise Managed User, you do not need any additional configuration and can simply log in to GitHub."
        );
        providerHint.setPadding(new Insets(4, 0, 0, 0));

        authSection.getChildren().addAll(authHeader, preferDeviceCodeCheck, deviceCodeHint, authProviderRow, providerHint);

        // 5. Telemetry section
        VBox telemetrySection = new VBox(8);
        Label telemetryHeader = createSectionHeader("Telemetry");
        sendTelemetryCheck = createCheckBox("Send usage telemetry");
        Label telemetryHint = createHintLabel("Telemetry helps us better understand how GitHub Copilot is performing, where improvements need to be made, and how features are being used.");
        telemetryHint.setPadding(new Insets(0, 0, 0, 22));
        telemetrySection.getChildren().addAll(telemetryHeader, sendTelemetryCheck, telemetryHint);

        getChildren().addAll(generalSection, accessSection, pluginSection, authSection, telemetrySection);
    }

    private Label createSectionHeader(String title) {
        Label l = new Label(title);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
        return l;
    }

    private Label createFieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return l;
    }

    private Label createHintLabel(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        return l;
    }

    private CheckBox createCheckBox(String text) {
        CheckBox cb = new CheckBox(text);
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        cb.selectedProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        return cb;
    }

    private void styleTextField(TextField tf) {
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 7 3 7;");
    }

    private void styleComboBox(ComboBox<String> cb) {
        cb.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
    }

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();
            screenReaderCheck.setSelected(initialSettings.isEnableScreenReaderSupport());
            updateChannelCombo.setValue(initialSettings.getUpdateChannel());
            checkUpdatesCheck.setSelected(initialSettings.isCheckForPluginUpdates());
            preferDeviceCodeCheck.setSelected(initialSettings.isPreferDeviceCodeSignIn());
            authProviderField.setText(initialSettings.getAuthenticationProvider());
            sendTelemetryCheck.setSelected(initialSettings.isSendUsageTelemetry());
        } finally {
            updating = false;
        }
    }

    private GitHubCopilotGeneralSettings getCurrentSettingsFromUI() {
        GitHubCopilotGeneralSettings s = new GitHubCopilotGeneralSettings();
        s.setEnableScreenReaderSupport(screenReaderCheck.isSelected());
        s.setUpdateChannel(updateChannelCombo.getValue());
        s.setCheckForPluginUpdates(checkUpdatesCheck.isSelected());
        s.setPreferDeviceCodeSignIn(preferDeviceCodeCheck.isSelected());
        s.setAuthenticationProvider(authProviderField.getText().trim());
        s.setSendUsageTelemetry(sendTelemetryCheck.isSelected());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        GitHubCopilotGeneralSettings updated = getCurrentSettingsFromUI();
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

    // Getters for testing
    public Hyperlink getAccountSettingsLink() { return accountSettingsLink; }
    public CheckBox getScreenReaderCheck() { return screenReaderCheck; }
    public ComboBox<String> getUpdateChannelCombo() { return updateChannelCombo; }
    public CheckBox getCheckUpdatesCheck() { return checkUpdatesCheck; }
    public CheckBox getPreferDeviceCodeCheck() { return preferDeviceCodeCheck; }
    public TextField getAuthProviderField() { return authProviderField; }
    public CheckBox getSendTelemetryCheck() { return sendTelemetryCheck; }
}
