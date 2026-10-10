package dev.lumina.ui;

import dev.lumina.tools.GitHubCopilotNetworkSettings;
import dev.lumina.tools.GitHubCopilotNetworkSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Tools > GitHub Copilot > Network settings page in Lumina IDE matching Image 4.
 */
public class SettingsToolsGitHubCopilotNetworkPage extends VBox {

    private final GitHubCopilotNetworkSettingsManager manager;
    private GitHubCopilotNetworkSettings initialSettings;
    private Runnable onModifiedListener;
    private Consumer<String> onNavigateToCategory;
    private boolean updating = false;

    // Controls
    private Hyperlink manageProxyLink;
    private CheckBox customizeProxyCheck;
    private TextField hostNameField;
    private TextField portNumberField;
    private CheckBox proxyAuthCheck;
    private TextField loginField;
    private PasswordField passwordField;
    private ComboBox<String> networkingCombo;
    private TextField kerberosPrincipalField;

    public SettingsToolsGitHubCopilotNetworkPage() {
        this(null);
    }

    public SettingsToolsGitHubCopilotNetworkPage(Consumer<String> onNavigateToCategory) {
        this.manager = GitHubCopilotNetworkSettingsManager.getInstance();
        this.onNavigateToCategory = onNavigateToCategory;
        buildUI();
        loadData();
    }

    public void setOnNavigateToCategory(Consumer<String> onNavigateToCategory) {
        this.onNavigateToCategory = onNavigateToCategory;
    }

    private void buildUI() {
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(16);
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        // 1. Manage IDE HTTP proxy settings link
        manageProxyLink = new Hyperlink("Manage IDE HTTP proxy settings (requires restart)");
        manageProxyLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-border-color: transparent; -fx-padding: 0;");
        manageProxyLink.setOnAction(e -> {
            if (onNavigateToCategory != null) {
                onNavigateToCategory.accept("HTTP Proxy");
            }
        });

        // 2. Customize HTTP proxy section
        customizeProxyCheck = new CheckBox("Customize HTTP proxy");
        customizeProxyCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        customizeProxyCheck.selectedProperty().addListener((obs, o, n) -> {
            updateControlStates();
            if (!updating) notifyModified();
        });

        // Indented fields
        VBox proxyFieldsBox = new VBox(10);
        proxyFieldsBox.setPadding(new Insets(0, 0, 0, 24));

        // Host name row
        HBox hostRow = new HBox(12);
        hostRow.setAlignment(Pos.CENTER_LEFT);
        Label hostLabel = createFieldLabel("Host name:");
        hostLabel.setPrefWidth(100);
        hostNameField = new TextField();
        hostNameField.setPrefWidth(220);
        styleTextField(hostNameField);
        hostNameField.textProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        hostRow.getChildren().addAll(hostLabel, hostNameField);

        // Port number row
        HBox portRow = new HBox(12);
        portRow.setAlignment(Pos.CENTER_LEFT);
        Label portLabel = createFieldLabel("Port number:");
        portLabel.setPrefWidth(100);
        portNumberField = new TextField("0");
        portNumberField.setPrefWidth(220);
        styleTextField(portNumberField);
        portNumberField.textProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        portRow.getChildren().addAll(portLabel, portNumberField);

        // Proxy authentication sub-checkbox
        proxyAuthCheck = new CheckBox("Proxy authentication");
        proxyAuthCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        proxyAuthCheck.selectedProperty().addListener((obs, o, n) -> {
            updateControlStates();
            if (!updating) notifyModified();
        });

        // Login & password fields
        VBox authFieldsBox = new VBox(8);
        authFieldsBox.setPadding(new Insets(0, 0, 0, 22));

        HBox loginRow = new HBox(12);
        loginRow.setAlignment(Pos.CENTER_LEFT);
        Label loginLabel = createFieldLabel("Login:");
        loginLabel.setPrefWidth(80);
        loginField = new TextField();
        loginField.setPrefWidth(200);
        styleTextField(loginField);
        loginField.textProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        loginRow.getChildren().addAll(loginLabel, loginField);

        HBox passwordRow = new HBox(12);
        passwordRow.setAlignment(Pos.CENTER_LEFT);
        Label passwordLabel = createFieldLabel("Password:");
        passwordLabel.setPrefWidth(80);
        passwordField = new PasswordField();
        passwordField.setPrefWidth(200);
        styleTextField(passwordField);
        passwordField.textProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        passwordRow.getChildren().addAll(passwordLabel, passwordField);

        authFieldsBox.getChildren().addAll(loginRow, passwordRow);
        proxyFieldsBox.getChildren().addAll(hostRow, portRow, proxyAuthCheck, authFieldsBox);

        // 3. Networking dropdown row
        HBox netRow = new HBox(12);
        netRow.setAlignment(Pos.CENTER_LEFT);
        Label netLabel = createFieldLabel("Networking");
        netLabel.setPrefWidth(124);
        networkingCombo = new ComboBox<>();
        networkingCombo.getItems().addAll("Auto", "Manual", "Disabled");
        networkingCombo.setValue("Auto");
        networkingCombo.setPrefWidth(120);
        styleComboBox(networkingCombo);
        networkingCombo.valueProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        netRow.getChildren().addAll(netLabel, networkingCombo);

        // 4. Override Kerberos Proxy Service Principal Name
        HBox kerberosRow = new HBox(12);
        kerberosRow.setAlignment(Pos.CENTER_LEFT);
        Label kerberosLabel = createFieldLabel("Override Kerberos Proxy Service Principal Name");
        kerberosLabel.setPrefWidth(300);
        kerberosPrincipalField = new TextField();
        kerberosPrincipalField.setPrefWidth(220);
        HBox.setHgrow(kerberosPrincipalField, Priority.ALWAYS);
        styleTextField(kerberosPrincipalField);
        kerberosPrincipalField.textProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        kerberosRow.getChildren().addAll(kerberosLabel, kerberosPrincipalField);

        getChildren().addAll(
                manageProxyLink,
                customizeProxyCheck,
                proxyFieldsBox,
                netRow,
                kerberosRow
        );

        updateControlStates();
    }

    private void updateControlStates() {
        boolean customProxy = customizeProxyCheck.isSelected();
        hostNameField.setDisable(!customProxy);
        portNumberField.setDisable(!customProxy);
        proxyAuthCheck.setDisable(!customProxy);

        boolean authEnabled = customProxy && proxyAuthCheck.isSelected();
        loginField.setDisable(!authEnabled);
        passwordField.setDisable(!authEnabled);
    }

    private Label createFieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return l;
    }

    private void styleTextField(TextField tf) {
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8;");
    }

    private void styleComboBox(ComboBox<String> cb) {
        cb.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
    }

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();
            applySettingsToUI(initialSettings);
        } finally {
            updating = false;
        }
    }

    private void applySettingsToUI(GitHubCopilotNetworkSettings s) {
        if (s == null) return;
        customizeProxyCheck.setSelected(s.isCustomizeHttpProxy());
        hostNameField.setText(s.getHostName());
        portNumberField.setText(String.valueOf(s.getPortNumber()));
        proxyAuthCheck.setSelected(s.isProxyAuthentication());
        loginField.setText(s.getLogin());
        passwordField.setText(s.getPassword());
        networkingCombo.setValue(s.getNetworking());
        kerberosPrincipalField.setText(s.getOverrideKerberosProxyPrincipalName());
        updateControlStates();
    }

    private GitHubCopilotNetworkSettings getCurrentSettingsFromUI() {
        GitHubCopilotNetworkSettings s = new GitHubCopilotNetworkSettings();
        s.setCustomizeHttpProxy(customizeProxyCheck.isSelected());
        s.setHostName(hostNameField.getText().trim());

        try {
            s.setPortNumber(Integer.parseInt(portNumberField.getText().trim()));
        } catch (NumberFormatException e) {
            s.setPortNumber(0);
        }

        s.setProxyAuthentication(proxyAuthCheck.isSelected());
        s.setLogin(loginField.getText().trim());
        s.setPassword(passwordField.getText());
        s.setNetworking(networkingCombo.getValue());
        s.setOverrideKerberosProxyPrincipalName(kerberosPrincipalField.getText().trim());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        GitHubCopilotNetworkSettings current = getCurrentSettingsFromUI();
        manager.setSettings(current);
        initialSettings = current.clone();
        notifyModified();
    }

    public void reset() {
        loadData();
        notifyModified();
    }

    public void revertChanges() {
        reset();
    }

    public void resetDefaults() {
        applySettingsToUI(new GitHubCopilotNetworkSettings());
        notifyModified();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    // Accessors for testing
    public Hyperlink getManageProxyLink() {
        return manageProxyLink;
    }

    public CheckBox getCustomizeProxyCheck() {
        return customizeProxyCheck;
    }

    public CheckBox getCustomizeHttpProxyCheckBox() {
        return customizeProxyCheck;
    }

    public TextField getHostNameField() {
        return hostNameField;
    }

    public TextField getPortNumberField() {
        return portNumberField;
    }

    public CheckBox getProxyAuthCheck() {
        return proxyAuthCheck;
    }

    public CheckBox getProxyAuthCheckBox() {
        return proxyAuthCheck;
    }

    public TextField getLoginField() {
        return loginField;
    }

    public PasswordField getPasswordField() {
        return passwordField;
    }

    public ComboBox<String> getNetworkingCombo() {
        return networkingCombo;
    }

    public ComboBox<String> getNetworkingComboBox() {
        return networkingCombo;
    }

    public TextField getKerberosPrincipalField() {
        return kerberosPrincipalField;
    }

    public TextField getKerberosField() {
        return kerberosPrincipalField;
    }
}
