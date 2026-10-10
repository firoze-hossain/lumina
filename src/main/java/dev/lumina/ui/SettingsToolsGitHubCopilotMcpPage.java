package dev.lumina.ui;

import dev.lumina.tools.GitHubCopilotMcpSettings;
import dev.lumina.tools.GitHubCopilotMcpSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Tools > GitHub Copilot > Model Context Protocol (MCP) settings page in Lumina IDE matching Image 3.
 */
public class SettingsToolsGitHubCopilotMcpPage extends VBox {

    private final GitHubCopilotMcpSettingsManager manager;
    private GitHubCopilotMcpSettings initialSettings;
    private Runnable onModifiedListener;
    private Consumer<String> onNavigateToCategory;
    private boolean updating = false;

    // Controls
    private Button mcpConfigureBtn;
    private TextField registryBaseUrlField;
    private Button allowedModelsBtn;
    private Hyperlink serverLevelConfigLink;
    private Button autoApproveConfigureBtn;

    public SettingsToolsGitHubCopilotMcpPage() {
        this(null);
    }

    public SettingsToolsGitHubCopilotMcpPage(Consumer<String> onNavigateToCategory) {
        this.manager = GitHubCopilotMcpSettingsManager.getInstance();
        this.onNavigateToCategory = onNavigateToCategory;
        buildUI();
        loadData();
    }

    public void setOnNavigateToCategory(Consumer<String> onNavigateToCategory) {
        this.onNavigateToCategory = onNavigateToCategory;
    }

    private void buildUI() {
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(22);
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        // 1. Model Context Protocol (MCP) section
        VBox mcpSection = new VBox(6);
        Label mcpTitle = createSectionTitle("Model Context Protocol (MCP)");
        Label mcpSubtitle = createSubtitle("Model Context Protocol server configurations");

        mcpConfigureBtn = createButton("Configure...");
        mcpConfigureBtn.setOnAction(e -> {
            if (onNavigateToCategory != null) {
                onNavigateToCategory.accept("MCP Server");
            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("MCP Configuration");
                alert.setHeaderText("Model Context Protocol Servers");
                alert.setContentText("Manage your configured MCP server definitions under Tools > MCP Server.");
                alert.showAndWait();
            }
        });

        mcpSection.getChildren().addAll(mcpTitle, mcpSubtitle, mcpConfigureBtn);

        // 2. MCP Registry (Preview) section
        VBox registrySection = new VBox(8);
        Label registryTitle = createSectionTitle("MCP Registry (Preview)");
        Label registrySubtitle = createSubtitle("Base URL for a specification-compliant MCP registry (e.g. https://api.mcp.github.com).");

        HBox urlRow = new HBox(12);
        urlRow.setAlignment(Pos.CENTER_LEFT);
        Label urlLabel = createFieldLabel("MCP Registry Base URL:");
        urlLabel.setPrefWidth(160);

        registryBaseUrlField = new TextField();
        registryBaseUrlField.setPrefWidth(420);
        HBox.setHgrow(registryBaseUrlField, Priority.ALWAYS);
        styleTextField(registryBaseUrlField);
        registryBaseUrlField.textProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });

        urlRow.getChildren().addAll(urlLabel, registryBaseUrlField);
        registrySection.getChildren().addAll(registryTitle, registrySubtitle, urlRow);

        // 3. MCP Sampling (Optional) section
        VBox samplingSection = new VBox(10);
        HBox samplingHeader = createDividerHeader("MCP Sampling (Optional)");

        allowedModelsBtn = createButton("Allowed Models...");
        allowedModelsBtn.setOnAction(e -> openAllowedModelsDialog());

        Label samplingDesc = createSubtitle("Configure which models are exposed to all MCP servers for sampling (making model requests in the background). To configure models for a specific MCP server, go to Server-level configuration.");
        samplingDesc.setWrapText(true);

        serverLevelConfigLink = new Hyperlink("Server Level Configuration");
        serverLevelConfigLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0;");
        serverLevelConfigLink.setOnAction(e -> {
            if (onNavigateToCategory != null) {
                onNavigateToCategory.accept("MCP Server");
            }
        });

        HBox autoApproveRow = new HBox(12);
        autoApproveRow.setAlignment(Pos.CENTER_LEFT);
        Label autoApproveLabel = createFieldLabel("Auto Approve for Sampling:");
        autoApproveLabel.setPrefWidth(160);

        autoApproveConfigureBtn = createButton("Configure...");
        autoApproveConfigureBtn.setOnAction(e -> openAutoApproveDialog());

        autoApproveRow.getChildren().addAll(autoApproveLabel, autoApproveConfigureBtn);

        samplingSection.getChildren().addAll(
                samplingHeader,
                allowedModelsBtn,
                samplingDesc,
                serverLevelConfigLink,
                autoApproveRow
        );

        getChildren().addAll(mcpSection, registrySection, samplingSection);
    }

    private void openAllowedModelsDialog() {
        Dialog<List<String>> dialog = new Dialog<>();
        dialog.setTitle("Allowed Models for MCP Sampling");
        dialog.setHeaderText("Select models exposed to all MCP servers for sampling:");

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        VBox content = new VBox(8);
        content.setPadding(new Insets(10));
        List<String> available = List.of("Claude 3.5 Sonnet", "Claude 3.7 Sonnet", "GPT-4o", "GPT-4.1 Copilot", "Gemini 2.5 Pro");
        List<CheckBox> checkBoxes = new ArrayList<>();

        List<String> current = initialSettings != null ? initialSettings.getAllowedSamplingModels() : List.of();
        for (String model : available) {
            CheckBox cb = new CheckBox(model);
            cb.setStyle("-fx-text-fill: #DFE1E5;");
            cb.setSelected(current.contains(model));
            checkBoxes.add(cb);
            content.getChildren().add(cb);
        }

        dialog.getDialogPane().setContent(content);
        dialog.setResultConverter(btn -> {
            if (btn == saveType) {
                List<String> selected = new ArrayList<>();
                for (CheckBox cb : checkBoxes) {
                    if (cb.isSelected()) selected.add(cb.getText());
                }
                return selected;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(selected -> {
            if (initialSettings != null && !Objects.equals(initialSettings.getAllowedSamplingModels(), selected)) {
                notifyModified();
            }
        });
    }

    private void openAutoApproveDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Auto Approve Sampling");
        dialog.setHeaderText("Auto-approve sampling requests for trusted MCP servers:");

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        CheckBox autoApproveCheck = new CheckBox("Enable auto-approval of sampling for all registered servers");
        autoApproveCheck.setStyle("-fx-text-fill: #DFE1E5;");
        autoApproveCheck.setSelected(initialSettings != null && initialSettings.isAutoApproveSampling());

        content.getChildren().addAll(autoApproveCheck);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> btn == saveType ? autoApproveCheck.isSelected() : null);
        dialog.showAndWait().ifPresent(enabled -> {
            if (initialSettings != null && initialSettings.isAutoApproveSampling() != enabled) {
                notifyModified();
            }
        });
    }

    private Label createSectionTitle(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
        return l;
    }

    private Label createSubtitle(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px;");
        return l;
    }

    private Label createFieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return l;
    }

    private Button createButton(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 14; -fx-cursor: hand; -fx-font-size: 12px;");
        return b;
    }

    private void styleTextField(TextField tf) {
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8;");
    }

    private HBox createDividerHeader(String text) {
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(6, 0, 4, 0));

        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #393B40; -fx-opacity: 0.5;");
        HBox.setHgrow(sep, Priority.ALWAYS);

        header.getChildren().addAll(label, sep);
        return header;
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

    private void applySettingsToUI(GitHubCopilotMcpSettings s) {
        if (s == null) return;
        registryBaseUrlField.setText(s.getMcpRegistryBaseUrl());
    }

    private GitHubCopilotMcpSettings getCurrentSettingsFromUI() {
        GitHubCopilotMcpSettings s = initialSettings != null ? initialSettings.clone() : new GitHubCopilotMcpSettings();
        s.setMcpRegistryBaseUrl(registryBaseUrlField.getText().trim());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        GitHubCopilotMcpSettings current = getCurrentSettingsFromUI();
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
        applySettingsToUI(new GitHubCopilotMcpSettings());
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
    public Button getMcpConfigureBtn() {
        return mcpConfigureBtn;
    }

    public TextField getRegistryBaseUrlField() {
        return registryBaseUrlField;
    }

    public TextField getMcpRegistryBaseUrlField() {
        return registryBaseUrlField;
    }

    public Button getAllowedModelsBtn() {
        return allowedModelsBtn;
    }

    public Hyperlink getServerLevelConfigLink() {
        return serverLevelConfigLink;
    }

    public Button getAutoApproveConfigureBtn() {
        return autoApproveConfigureBtn;
    }
}
