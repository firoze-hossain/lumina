package dev.lumina.ui;

import dev.lumina.tools.ClaudeCodeSettings;
import dev.lumina.tools.ClaudeCodeSettingsManager;
import java.util.Objects;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Claude Code [Beta] settings page in Lumina IDE matching 1:1 design of the reference IDE.
 */
public class SettingsToolsClaudeCodePage extends VBox {

    private final ClaudeCodeSettingsManager manager;
    private ClaudeCodeSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    private TextField claudeCommandField;
    private TextField configDirectoryField;
    private CheckBox suppressNotificationCheck;
    private CheckBox hideToolbarButtonCheck;
    private CheckBox optionEnterMultiLineCheck;
    private CheckBox automaticUpdatesCheck;
    private CheckBox acceptConnectionsAllInterfacesCheck;

    public SettingsToolsClaudeCodePage() {
        this.manager = ClaudeCodeSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");

        final double labelWidth = 140;

        VBox contentBox = new VBox(14);
        contentBox.setStyle("-fx-background-color: #1E1F22;");

        // 1. General Header
        Label generalHeader = new Label("General");
        generalHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        // 2. Claude command:
        HBox cmdRow = new HBox(8);
        cmdRow.setAlignment(Pos.TOP_LEFT);

        Label cmdLabel = new Label("Claude command:");
        cmdLabel.setMinWidth(labelWidth);
        cmdLabel.setPrefWidth(labelWidth);
        cmdLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 0 0 0;");

        VBox cmdFieldBox = new VBox(4);
        claudeCommandField = new TextField();
        claudeCommandField.setPrefWidth(280);
        claudeCommandField.setMaxWidth(350);
        claudeCommandField.setStyle(
                "-fx-background-color: #2B2D30; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-radius: 4px; " +
                "-fx-background-radius: 4px; " +
                "-fx-text-fill: #DFE1E5; " +
                "-fx-font-size: 13px; " +
                "-fx-padding: 4 8 4 8;"
        );
        claudeCommandField.textProperty().addListener((obs, oldVal, newVal) -> notifyModified());

        Label cmdDesc = new Label("Specify the command to run Claude (e.g., 'claude', '/usr/local/bin/claude', or 'npx @anthropic/claude')");
        cmdDesc.setWrapText(true);
        cmdDesc.setMaxWidth(750);
        cmdDesc.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 12px;");

        cmdFieldBox.getChildren().addAll(claudeCommandField, cmdDesc);
        cmdRow.getChildren().addAll(cmdLabel, cmdFieldBox);

        // 3. Config directory:
        HBox cfgRow = new HBox(8);
        cfgRow.setAlignment(Pos.TOP_LEFT);

        Label cfgLabel = new Label("Config directory:");
        cfgLabel.setMinWidth(labelWidth);
        cfgLabel.setPrefWidth(labelWidth);
        cfgLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 0 0 0;");

        VBox cfgFieldBox = new VBox(4);
        configDirectoryField = new TextField();
        configDirectoryField.setPrefWidth(280);
        configDirectoryField.setMaxWidth(350);
        configDirectoryField.setStyle(
                "-fx-background-color: #2B2D30; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-radius: 4px; " +
                "-fx-background-radius: 4px; " +
                "-fx-text-fill: #DFE1E5; " +
                "-fx-font-size: 13px; " +
                "-fx-padding: 4 8 4 8;"
        );
        configDirectoryField.textProperty().addListener((obs, oldVal, newVal) -> notifyModified());

        Label cfgDesc = new Label("Custom Claude config directory. This should be set to the same value as the CLAUDE_CONFIG_DIR environment variable.");
        cfgDesc.setWrapText(true);
        cfgDesc.setMaxWidth(750);
        cfgDesc.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 12px;");

        cfgFieldBox.getChildren().addAll(configDirectoryField, cfgDesc);
        cfgRow.getChildren().addAll(cfgLabel, cfgFieldBox);

        // 4. Suppress notification for when Claude Command is not found
        VBox suppressBox = buildCheckOption(
                "Suppress notification for when Claude Command is not found",
                "When enabled, the IDE will skip notifications about not finding the Claude command"
        );
        suppressNotificationCheck = (CheckBox) suppressBox.getChildren().get(0);

        // 5. Hide Claude Code toolbar button
        VBox hideToolbarBox = buildCheckOption(
                "Hide Claude Code toolbar button",
                "When enabled, the Claude Code button will be hidden from the toolbar"
        );
        hideToolbarButtonCheck = (CheckBox) hideToolbarBox.getChildren().get(0);

        // 6. Enable using Option+Enter for multi-line prompts
        VBox optionEnterBox = buildCheckOption(
                "Enable using Option+Enter for multi-line prompts",
                "When enabled, Option+Enter can be used to insert new lines in Claude Code (requires restarting the terminal)"
        );
        optionEnterMultiLineCheck = (CheckBox) optionEnterBox.getChildren().get(0);

        // 7. Enable automatic updates
        VBox autoUpdateBox = buildCheckOption(
                "Enable automatic updates",
                "When enabled, the plugin will automatically check for and install updates (applied on restart)"
        );
        automaticUpdatesCheck = (CheckBox) autoUpdateBox.getChildren().get(0);

        // 8. Networking (Advanced) Header
        Label networkHeader = new Label("Networking (Advanced)");
        networkHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 8 0 0 0;");

        // 9. Accept connections from all network interfaces
        VBox allInterfacesBox = buildCheckOption(
                "Accept connections from all network interfaces",
                "When enabled, the plugin will accept connections from all network interfaces instead of localhost only (requires restarting the IDE)"
        );
        acceptConnectionsAllInterfacesCheck = (CheckBox) allInterfacesBox.getChildren().get(0);

        contentBox.getChildren().addAll(
                generalHeader, cmdRow, cfgRow,
                suppressBox, hideToolbarBox, optionEnterBox, autoUpdateBox,
                networkHeader, allInterfacesBox
        );

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().add(scrollPane);
    }

    private VBox buildCheckOption(String title, String description) {
        VBox box = new VBox(2);
        CheckBox cb = new CheckBox(title);
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        cb.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());

        Label desc = new Label(description);
        desc.setWrapText(true);
        desc.setMaxWidth(750);
        desc.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 12px; -fx-padding: 0 0 0 22;");

        box.getChildren().addAll(cb, desc);
        return box;
    }

    private void loadData() {
        updating = true;
        ClaudeCodeSettings s = manager.getSettings();
        claudeCommandField.setText(s.getClaudeCommand());
        configDirectoryField.setText(s.getConfigDirectory());
        suppressNotificationCheck.setSelected(s.isSuppressNotificationNotFound());
        hideToolbarButtonCheck.setSelected(s.isHideToolbarButton());
        optionEnterMultiLineCheck.setSelected(s.isOptionEnterMultiLine());
        automaticUpdatesCheck.setSelected(s.isAutomaticUpdates());
        acceptConnectionsAllInterfacesCheck.setSelected(s.isAcceptConnectionsAllInterfaces());
        initialSettings = getCurrentSettingsFromUI();
        updating = false;
    }

    public ClaudeCodeSettings getCurrentSettingsFromUI() {
        ClaudeCodeSettings s = new ClaudeCodeSettings();
        s.setClaudeCommand(claudeCommandField.getText());
        s.setConfigDirectory(configDirectoryField.getText());
        s.setSuppressNotificationNotFound(suppressNotificationCheck.isSelected());
        s.setHideToolbarButton(hideToolbarButtonCheck.isSelected());
        s.setOptionEnterMultiLine(optionEnterMultiLineCheck.isSelected());
        s.setAutomaticUpdates(automaticUpdatesCheck.isSelected());
        s.setAcceptConnectionsAllInterfaces(acceptConnectionsAllInterfacesCheck.isSelected());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        ClaudeCodeSettings updated = getCurrentSettingsFromUI();
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

    public TextField getClaudeCommandField() {
        return claudeCommandField;
    }

    public TextField getConfigDirectoryField() {
        return configDirectoryField;
    }

    public CheckBox getSuppressNotificationCheck() {
        return suppressNotificationCheck;
    }

    public CheckBox getHideToolbarButtonCheck() {
        return hideToolbarButtonCheck;
    }

    public CheckBox getOptionEnterMultiLineCheck() {
        return optionEnterMultiLineCheck;
    }

    public CheckBox getAutomaticUpdatesCheck() {
        return automaticUpdatesCheck;
    }

    public CheckBox getAcceptConnectionsAllInterfacesCheck() {
        return acceptConnectionsAllInterfacesCheck;
    }
}
