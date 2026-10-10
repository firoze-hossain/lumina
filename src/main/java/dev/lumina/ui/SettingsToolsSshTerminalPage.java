package dev.lumina.ui;

import dev.lumina.tools.SshConfigurationEntry;
import dev.lumina.tools.SshConfigurationsSettingsManager;
import dev.lumina.tools.SshTerminalSettings;
import dev.lumina.tools.SshTerminalSettingsManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings UI page for Tools > SSH Terminal in Lumina IDE.
 * 1:1 visual match with reference layout.
 */
public class SettingsToolsSshTerminalPage extends VBox {

    private final ToggleGroup connectionGroup = new ToggleGroup();
    private final RadioButton currentVagrantRadio = new RadioButton("Current Vagrant");
    private final RadioButton defaultPythonRemoteRadio = new RadioButton("Default Python Remote Interpreter");
    private final RadioButton sshConfigurationRadio = new RadioButton("SSH configuration");

    private final ComboBox<String> sshConfigurationCombo = new ComboBox<>();
    private final Hyperlink setupConfigurationsLink = new Hyperlink("Set up Configurations");
    private final ComboBox<String> defaultEncodingCombo = new ComboBox<>();

    private Runnable onModified;
    private Runnable onNavigateToSshConfigurations;
    private boolean suppressEvents = false;

    public SettingsToolsSshTerminalPage() {
        setSpacing(14);
        setPadding(new Insets(16, 20, 20, 20));
        setStyle("-fx-background-color: #1E1F22;");

        // Section header
        HBox headerBox = createSectionHeader("Connection settings");

        // Radio Buttons
        currentVagrantRadio.setToggleGroup(connectionGroup);
        currentVagrantRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        defaultPythonRemoteRadio.setToggleGroup(connectionGroup);
        defaultPythonRemoteRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        sshConfigurationRadio.setToggleGroup(connectionGroup);
        sshConfigurationRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        // SSH config combo & link
        sshConfigurationCombo.setPrefWidth(260);
        sshConfigurationCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4;");
        sshConfigurationCombo.disableProperty().bind(sshConfigurationRadio.selectedProperty().not());

        setupConfigurationsLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-underline: false; -fx-border-color: transparent; -fx-padding: 0;");
        setupConfigurationsLink.setOnMouseEntered(e -> setupConfigurationsLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-underline: true; -fx-border-color: transparent; -fx-padding: 0;"));
        setupConfigurationsLink.setOnMouseExited(e -> setupConfigurationsLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-underline: false; -fx-border-color: transparent; -fx-padding: 0;"));
        setupConfigurationsLink.setOnAction(e -> {
            if (onNavigateToSshConfigurations != null) {
                onNavigateToSshConfigurations.run();
            }
        });

        HBox sshConfigRow = new HBox(12, sshConfigurationRadio, sshConfigurationCombo, setupConfigurationsLink);
        sshConfigRow.setAlignment(Pos.CENTER_LEFT);

        VBox radioBox = new VBox(10, currentVagrantRadio, defaultPythonRemoteRadio, sshConfigRow);
        radioBox.setPadding(new Insets(4, 0, 8, 16));

        // Default encoding row
        Label encodingLabel = new Label("Default encoding:");
        encodingLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        ObservableList<String> encodings = FXCollections.observableArrayList(
                "UTF-8", "ISO-8859-1", "US-ASCII", "UTF-16", "UTF-16BE", "UTF-16LE",
                "windows-1252", "GBK", "Shift_JIS", "EUC-KR"
        );
        defaultEncodingCombo.setItems(encodings);
        defaultEncodingCombo.setValue("UTF-8");
        defaultEncodingCombo.setPrefWidth(120);
        defaultEncodingCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4;");

        HBox encodingRow = new HBox(12, encodingLabel, defaultEncodingCombo);
        encodingRow.setAlignment(Pos.CENTER_LEFT);
        encodingRow.setPadding(new Insets(4, 0, 0, 0));

        getChildren().addAll(headerBox, radioBox, encodingRow);

        setupListeners();
        loadSettings();
    }

    private HBox createSectionHeader(String title) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setStyle("-fx-background-color: #393B40; -fx-min-height: 1; -fx-max-height: 1;");
        HBox.setHgrow(line, Priority.ALWAYS);

        HBox box = new HBox(12, titleLabel, line);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void refreshSshConfigsCombo(String selectedValue) {
        List<String> items = new ArrayList<>();
        items.add(SshTerminalSettings.DEFAULT_SSH_CONFIG_SELECT);
        for (SshConfigurationEntry entry : SshConfigurationsSettingsManager.getInstance().getSettings().getConfigurations()) {
            items.add(entry.getDisplayName());
        }
        sshConfigurationCombo.setItems(FXCollections.observableArrayList(items));
        if (selectedValue != null && items.contains(selectedValue)) {
            sshConfigurationCombo.setValue(selectedValue);
        } else {
            sshConfigurationCombo.setValue(SshTerminalSettings.DEFAULT_SSH_CONFIG_SELECT);
        }
    }

    private void setupListeners() {
        connectionGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        sshConfigurationCombo.valueProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        defaultEncodingCombo.valueProperty().addListener((obs, oldVal, newVal) -> notifyModified());
    }

    public void setOnModified(Runnable onModified) {
        this.onModified = onModified;
    }

    public void setOnNavigateToSshConfigurations(Runnable onNavigate) {
        this.onNavigateToSshConfigurations = onNavigate;
    }

    private void notifyModified() {
        if (!suppressEvents && onModified != null) {
            onModified.run();
        }
    }

    public void loadSettings() {
        suppressEvents = true;
        try {
            SshTerminalSettings s = SshTerminalSettingsManager.getInstance().getSettings();
            if (SshTerminalSettings.CONN_CURRENT_VAGRANT.equals(s.getConnectionMode())) {
                currentVagrantRadio.setSelected(true);
            } else if (SshTerminalSettings.CONN_DEFAULT_PYTHON_REMOTE.equals(s.getConnectionMode())) {
                defaultPythonRemoteRadio.setSelected(true);
            } else {
                sshConfigurationRadio.setSelected(true);
            }

            refreshSshConfigsCombo(s.getSshConfiguration());

            String enc = s.getDefaultEncoding();
            if (enc != null && defaultEncodingCombo.getItems().contains(enc)) {
                defaultEncodingCombo.setValue(enc);
            } else {
                defaultEncodingCombo.setValue("UTF-8");
            }
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        SshTerminalSettings current = SshTerminalSettingsManager.getInstance().getSettings();
        String selectedMode = currentVagrantRadio.isSelected() ? SshTerminalSettings.CONN_CURRENT_VAGRANT :
                (defaultPythonRemoteRadio.isSelected() ? SshTerminalSettings.CONN_DEFAULT_PYTHON_REMOTE : SshTerminalSettings.CONN_SSH_CONFIG);

        return !Objects.equals(selectedMode, current.getConnectionMode()) ||
                !Objects.equals(sshConfigurationCombo.getValue(), current.getSshConfiguration()) ||
                !Objects.equals(defaultEncodingCombo.getValue(), current.getDefaultEncoding());
    }

    public void apply() {
        SshTerminalSettings s = new SshTerminalSettings();
        String selectedMode = currentVagrantRadio.isSelected() ? SshTerminalSettings.CONN_CURRENT_VAGRANT :
                (defaultPythonRemoteRadio.isSelected() ? SshTerminalSettings.CONN_DEFAULT_PYTHON_REMOTE : SshTerminalSettings.CONN_SSH_CONFIG);
        s.setConnectionMode(selectedMode);
        s.setSshConfiguration(sshConfigurationCombo.getValue());
        s.setDefaultEncoding(defaultEncodingCombo.getValue());
        SshTerminalSettingsManager.getInstance().setSettings(s);
    }

    public void reset() {
        loadSettings();
    }

    public void revertChanges() {
        reset();
    }

    public RadioButton getCurrentVagrantRadio() {
        return currentVagrantRadio;
    }

    public RadioButton getDefaultPythonRemoteRadio() {
        return defaultPythonRemoteRadio;
    }

    public RadioButton getSshConfigurationRadio() {
        return sshConfigurationRadio;
    }

    public ComboBox<String> getSshConfigurationCombo() {
        return sshConfigurationCombo;
    }

    public ComboBox<String> getDefaultEncodingCombo() {
        return defaultEncodingCombo;
    }

    public Hyperlink getSetupConfigurationsLink() {
        return setupConfigurationsLink;
    }
}
