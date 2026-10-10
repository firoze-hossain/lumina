package dev.lumina.ui;

import dev.lumina.tools.McpServerSettings;
import dev.lumina.tools.McpServerSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;

import java.util.Objects;

/**
 * Tools > MCP Server settings page in Lumina IDE.
 * Matches 1:1 with screenshot 5.
 */
public class SettingsToolsMcpServerPage extends VBox {

    private final McpServerSettingsManager manager;
    private McpServerSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    private CheckBox enableMcpServerCheck;
    private Hyperlink allMcpToolsLink;
    private VBox detectedClientsBox;

    public SettingsToolsMcpServerPage() {
        this.manager = McpServerSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        enableMcpServerCheck = new CheckBox("Enable MCP Server");
        enableMcpServerCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        // Description with hyperlink
        Label descPart = new Label("The MCP Server allows external AI clients to use functionality from the IDE, integrating the power of your IDE into your AI tools. ");
        descPart.setStyle("-fx-text-fill: #7A7E85; -fx-font-size: 13px;");

        allMcpToolsLink = new Hyperlink("All MCP Tools ↗");
        allMcpToolsLink.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 13px; -fx-underline: false; -fx-padding: 0;");
        allMcpToolsLink.setOnAction(e -> {
            // Hyperlink action
        });

        TextFlow descFlow = new TextFlow(descPart, allMcpToolsLink);
        descFlow.setMaxWidth(680);

        Label subHeader = new Label("When enabled, these detected clients can be auto-configured to use IDE features in one click:");
        subHeader.setStyle("-fx-text-fill: #7A7E85; -fx-font-size: 13px;");

        detectedClientsBox = new VBox(8);
        detectedClientsBox.setPadding(new Insets(4, 0, 0, 10));

        Label claudeAppBullet = createBulletItem("Claude App");
        Label claudeCodeBullet = createBulletItem("Claude Code");

        detectedClientsBox.getChildren().addAll(claudeAppBullet, claudeCodeBullet);

        getChildren().addAll(enableMcpServerCheck, descFlow, subHeader, detectedClientsBox);

        setupListeners();
    }

    private Label createBulletItem(String text) {
        Label l = new Label("• " + text);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return l;
    }

    private void setupListeners() {
        enableMcpServerCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            updateEnabledStates();
            notifyModified();
        });
    }

    private void updateEnabledStates() {
        boolean enabled = enableMcpServerCheck.isSelected();
        detectedClientsBox.setOpacity(enabled ? 1.0 : 0.7);
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

    private void applySettingsToUI(McpServerSettings s) {
        if (s == null) return;
        enableMcpServerCheck.setSelected(s.isEnableMcpServer());
        updateEnabledStates();
    }

    private McpServerSettings getCurrentSettingsFromUI() {
        McpServerSettings s = new McpServerSettings();
        s.setEnableMcpServer(enableMcpServerCheck.isSelected());
        if (initialSettings != null) {
            s.setDetectedClients(initialSettings.getDetectedClients());
        }
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        McpServerSettings current = getCurrentSettingsFromUI();
        return !Objects.equals(initialSettings, current);
    }

    public void apply() {
        McpServerSettings current = getCurrentSettingsFromUI();
        manager.setSettings(current);
        initialSettings = current.clone();
        notifyModified();
    }

    public void revertChanges() {
        if (initialSettings != null) {
            updating = true;
            try {
                applySettingsToUI(initialSettings);
            } finally {
                updating = false;
            }
            notifyModified();
        }
    }

    public void reset() {
        revertChanges();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public CheckBox getEnableMcpServerCheck() {
        return enableMcpServerCheck;
    }

    public Hyperlink getAllMcpToolsLink() {
        return allMcpToolsLink;
    }

    public VBox getDetectedClientsBox() {
        return detectedClientsBox;
    }
}
