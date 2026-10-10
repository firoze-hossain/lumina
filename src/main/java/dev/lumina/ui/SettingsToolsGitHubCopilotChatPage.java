package dev.lumina.ui;

import dev.lumina.tools.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Tools > GitHub Copilot > Chat settings page in Lumina IDE matching 1:1 design of reference IDE.
 */
public class SettingsToolsGitHubCopilotChatPage extends VBox {

    private final GitHubCopilotChatSettingsManager manager;
    private GitHubCopilotChatSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    // General
    private CheckBox autoModelCheck;
    private ComboBox<String> naturalLangCombo;
    private ComboBox<String> diffViewModeCombo;
    private TextField autoAcceptDelayField;
    private CheckBox showInlineGutterCheck;
    private CheckBox semanticSearchCheck;

    // Agent
    private VBox agentContentBox;
    private CheckBox agentModeCheck;
    private TextField agentMaxReqField;
    private TextField anthropicTokensField;
    private CheckBox customAgentCheck;
    private CheckBox orgCustomAgentsCheck;
    private CheckBox subagentCheck;
    private CheckBox cloudAgentCheck;
    private CheckBox skillsCheck;
    private CheckBox hooksCheck;
    private CheckBox pluginsCheck;
    private CheckBox codeReviewCheck;
    private CheckBox byokCheck;
    private CheckBox copilotRemoteCheck;
    private CheckBox debugLoggingCheck;
    private CheckBox notifyAttentionCheck;
    private CheckBox claudeCliCheck;
    private TextField claudeCliPathField;
    private Button claudeCliBrowseBtn;
    private CheckBox codexCliCheck;
    private TextField codexCliPathField;
    private Button codexCliBrowseBtn;

    // Auto Approve: Terminal
    private VBox autoApproveContentBox;
    private ObservableList<TerminalAutoApproveRule> terminalRulesData = FXCollections.observableArrayList();
    private TableView<TerminalAutoApproveRule> terminalRulesTable;
    private Button addTerminalRuleBtn;
    private Button removeTerminalRuleBtn;
    private Button resetTerminalRulesBtn;
    private CheckBox uncoveredCommandsCheck;

    // Auto Approve: Edits
    private ObservableList<FileEditAutoApproveRule> editRulesData = FXCollections.observableArrayList();
    private TableView<FileEditAutoApproveRule> editRulesTable;
    private Button addEditRuleBtn;
    private Button removeEditRuleBtn;
    private Button resetEditRulesBtn;
    private CheckBox uncoveredEditsCheck;

    // Auto Approve: MCP & Global
    private CheckBox trustMcpAnnotationsCheck;
    private Button configureMcpBtn;
    private CheckBox globalAutoApproveCheck;

    // Open Telemetry
    private VBox otelContentBox;
    private CheckBox enableOtelExportCheck;
    private ComboBox<String> otelExporterTypeCombo;
    private ComboBox<String> otelProtocolCombo;
    private TextField otelEndpointField;
    private TextField otelOutputFileField;
    private CheckBox otelCaptureContentCheck;
    private TextField otelServiceNameField;
    private ObservableList<TelemetryResourceAttribute> otelAttrsData = FXCollections.observableArrayList();
    private TableView<TelemetryResourceAttribute> otelAttrsTable;
    private Button addOtelAttrBtn;
    private Button removeOtelAttrBtn;

    public SettingsToolsGitHubCopilotChatPage() {
        this.manager = GitHubCopilotChatSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(24);
        setStyle("-fx-background-color: #1E1F22;");

        // --- 1. General Section ---
        VBox generalSection = new VBox(12);
        Label generalHeader = createSectionHeader("General");

        autoModelCheck = createCheckBox("Enable Auto Model");
        Label autoModelHint = createHintLabel("Auto selects the best model for your request.");
        autoModelHint.setPadding(new Insets(0, 0, 0, 22));

        Label langLabel = createFieldLabel("Natural Language");
        langLabel.setPrefWidth(160);
        naturalLangCombo = new ComboBox<>();
        naturalLangCombo.getItems().addAll("English", "Chinese", "Japanese", "German", "French", "Spanish", "Korean");
        naturalLangCombo.setValue("English");
        naturalLangCombo.setPrefWidth(180);
        styleComboBox(naturalLangCombo);
        naturalLangCombo.valueProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        HBox langRow = new HBox(12, langLabel, naturalLangCombo);
        langRow.setAlignment(Pos.CENTER_LEFT);
        Label langHint = createHintLabel("The locale will be applied to new conversations. Existing conversations will not be affected.");

        Label diffLabel = createFieldLabel("Diff View Mode");
        diffLabel.setPrefWidth(160);
        diffViewModeCombo = new ComboBox<>();
        diffViewModeCombo.getItems().addAll("Copilot (Modern Inline)", "Unified", "Side-by-Side");
        diffViewModeCombo.setValue("Copilot (Modern Inline)");
        diffViewModeCombo.setPrefWidth(180);
        styleComboBox(diffViewModeCombo);
        diffViewModeCombo.valueProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        HBox diffRow = new HBox(12, diffLabel, diffViewModeCombo);
        diffRow.setAlignment(Pos.CENTER_LEFT);

        Label delayLabel = createFieldLabel("Auto-accept Delay");
        delayLabel.setPrefWidth(160);
        autoAcceptDelayField = new TextField("0");
        autoAcceptDelayField.setPrefWidth(70);
        styleTextField(autoAcceptDelayField);
        autoAcceptDelayField.textProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        Label secLabel = createHintLabel("s");
        HBox delayRow = new HBox(12, delayLabel, autoAcceptDelayField, secLabel);
        delayRow.setAlignment(Pos.CENTER_LEFT);

        showInlineGutterCheck = createCheckBox("Show inline chat gutter icon");
        Label gutterHint = createHintLabel("Show Copilot icon in the editor gutter for lines with context actions.");
        gutterHint.setPadding(new Insets(0, 0, 0, 22));

        semanticSearchCheck = createCheckBox("Enable Semantic Search for Chat");
        Label semanticHint = createHintLabel("Enables index-based semantic codebase retrieval for chat queries.");
        semanticHint.setPadding(new Insets(0, 0, 0, 22));

        generalSection.getChildren().addAll(
                generalHeader,
                autoModelCheck, autoModelHint,
                langRow, langHint,
                diffRow,
                delayRow,
                showInlineGutterCheck, gutterHint,
                semanticSearchCheck, semanticHint
        );

        // --- 2. Agent Section (Collapsible) ---
        VBox agentSection = new VBox(12);
        Button agentToggleBtn = createCollapsibleHeader("Agent");
        agentContentBox = new VBox(12);
        agentContentBox.setPadding(new Insets(4, 0, 4, 16));

        agentToggleBtn.setOnAction(e -> {
            boolean visible = !agentContentBox.isVisible();
            agentContentBox.setVisible(visible);
            agentContentBox.setManaged(visible);
            agentToggleBtn.setText(visible ? "▾  Agent" : "▸  Agent");
        });

        agentModeCheck = createCheckBox("Enable Agent mode");
        Label agentModeHint = createHintLabel("Allows Copilot to autonomously execute multi-step coding tasks and tool invocations.");
        agentModeHint.setPadding(new Insets(0, 0, 0, 22));

        Label maxReqLabel = createFieldLabel("Agent Max Requests");
        maxReqLabel.setPrefWidth(220);
        agentMaxReqField = new TextField("50");
        agentMaxReqField.setPrefWidth(90);
        styleTextField(agentMaxReqField);
        agentMaxReqField.textProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        HBox maxReqRow = new HBox(12, maxReqLabel, agentMaxReqField);
        maxReqRow.setAlignment(Pos.CENTER_LEFT);
        Label maxReqHint = createHintLabel("Maximum consecutive requests agent can make in a single task execution.");

        Label tokensLabel = createFieldLabel("Anthropic Thinking Budget Tokens");
        tokensLabel.setPrefWidth(220);
        anthropicTokensField = new TextField("1024");
        anthropicTokensField.setPrefWidth(90);
        styleTextField(anthropicTokensField);
        anthropicTokensField.textProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        HBox tokensRow = new HBox(12, tokensLabel, anthropicTokensField);
        tokensRow.setAlignment(Pos.CENTER_LEFT);

        customAgentCheck = createCheckBox("Enable Custom Agents");
        Label customAgentHint = createHintLabel("Allows using custom workspace-defined agents.");
        customAgentHint.setPadding(new Insets(0, 0, 0, 22));

        orgCustomAgentsCheck = createCheckBox("Enable Organization Custom Agents");
        Label orgAgentHint = createHintLabel("Sync and execute custom agents provisioned by your GitHub Organization.");
        orgAgentHint.setPadding(new Insets(0, 0, 0, 22));

        subagentCheck = createCheckBox("Enable Subagents");
        Label subagentHint = createHintLabel("Allows primary agent to spawn focused background subagents.");
        subagentHint.setPadding(new Insets(0, 0, 0, 22));

        cloudAgentCheck = createCheckBox("Enable Cloud Agent");
        Label cloudAgentHint = createHintLabel("Delegate heavy tasks to remote cloud instances for Lumina IDEs.");
        cloudAgentHint.setPadding(new Insets(0, 0, 0, 22));

        skillsCheck = createCheckBox("Enable Skills");
        hooksCheck = createCheckBox("Enable Hooks");
        pluginsCheck = createCheckBox("Enable Plugins");
        codeReviewCheck = createCheckBox("Enable Code Review");
        byokCheck = createCheckBox("Enable Bring Your Own Key");
        copilotRemoteCheck = createCheckBox("Enable Copilot Remote");
        debugLoggingCheck = createCheckBox("Enable Agent Debug File Logging");
        notifyAttentionCheck = createCheckBox("Notify when Copilot needs attention");

        // CLI Previews
        claudeCliCheck = createCheckBox("Enable Claude Code CLI Preview");
        Label claudeCliPathLabel = createFieldLabel("Claude Code CLI Path");
        claudeCliPathField = new TextField();
        claudeCliPathField.setPrefWidth(260);
        styleTextField(claudeCliPathField);
        claudeCliPathField.textProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        claudeCliBrowseBtn = createBrowseButton(claudeCliPathField);
        HBox claudeRow = new HBox(10, claudeCliPathLabel, claudeCliPathField, claudeCliBrowseBtn);
        claudeRow.setAlignment(Pos.CENTER_LEFT);
        claudeRow.setPadding(new Insets(0, 0, 0, 22));

        codexCliCheck = createCheckBox("Enable Codex CLI Preview");
        Label codexCliPathLabel = createFieldLabel("Codex CLI Path");
        codexCliPathField = new TextField();
        codexCliPathField.setPrefWidth(260);
        styleTextField(codexCliPathField);
        codexCliPathField.textProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        codexCliBrowseBtn = createBrowseButton(codexCliPathField);
        HBox codexRow = new HBox(10, codexCliPathLabel, codexCliPathField, codexCliBrowseBtn);
        codexRow.setAlignment(Pos.CENTER_LEFT);
        codexRow.setPadding(new Insets(0, 0, 0, 22));

        agentContentBox.getChildren().addAll(
                agentModeCheck, agentModeHint,
                maxReqRow, maxReqHint,
                tokensRow,
                customAgentCheck, customAgentHint,
                orgCustomAgentsCheck, orgAgentHint,
                subagentCheck, subagentHint,
                cloudAgentCheck, cloudAgentHint,
                skillsCheck, hooksCheck, pluginsCheck, codeReviewCheck,
                byokCheck, copilotRemoteCheck, debugLoggingCheck, notifyAttentionCheck,
                claudeCliCheck, claudeRow,
                codexCliCheck, codexRow
        );
        agentSection.getChildren().addAll(agentToggleBtn, agentContentBox);

        // --- 3. Auto Approve Section (Collapsible) ---
        VBox autoApproveSection = new VBox(14);
        Button autoApproveToggleBtn = createCollapsibleHeader("Auto Approve");
        autoApproveContentBox = new VBox(16);
        autoApproveContentBox.setPadding(new Insets(4, 0, 4, 16));

        autoApproveToggleBtn.setOnAction(e -> {
            boolean visible = !autoApproveContentBox.isVisible();
            autoApproveContentBox.setVisible(visible);
            autoApproveContentBox.setManaged(visible);
            autoApproveToggleBtn.setText(visible ? "▾  Auto Approve" : "▸  Auto Approve");
        });

        // 3a. Terminal Auto-approve
        HBox terminalHeaderBox = new HBox(6, createSubHeader("Terminal Auto-approve"), createHelpIcon());
        terminalHeaderBox.setAlignment(Pos.CENTER_LEFT);
        Label terminalHint = createHintLabel("Controls whether chat-initiated terminal commands are automatically approved. Set to true to auto-approve matching commands; set to false to always require explicit approval.");

        HBox terminalToolbar = new HBox(8);
        addTerminalRuleBtn = createIconButton("+", () -> {
            terminalRulesData.add(new TerminalAutoApproveRule("echo *", true));
            notifyModified();
        });
        removeTerminalRuleBtn = createIconButton("-", () -> {
            TerminalAutoApproveRule selected = terminalRulesTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                terminalRulesData.remove(selected);
                notifyModified();
            }
        });
        resetTerminalRulesBtn = createIconButton("↺", this::resetTerminalRulesToDefault);
        terminalToolbar.getChildren().addAll(addTerminalRuleBtn, removeTerminalRuleBtn, resetTerminalRulesBtn);

        terminalRulesTable = buildTerminalRulesTable();
        uncoveredCommandsCheck = createCheckBox("Auto-approve commands not covered by rules");
        Label uncoveredCommandsHint = createHintLabel("When enabled, terminal commands not covered by the rules above are automatically approved. When disabled, unmatched commands require explicit approval. Use this setting at your own discretion, as terminal commands outside the defined rules will be auto-approved.");
        uncoveredCommandsHint.setPadding(new Insets(0, 0, 0, 22));

        // 3b. Edits Auto-approve
        HBox editsHeaderBox = new HBox(6, createSubHeader("Edits Auto-approve"), createHelpIcon());
        editsHeaderBox.setAlignment(Pos.CENTER_LEFT);
        Label editsHint = createHintLabel("Controls whether file edits generated by Copilot are approved automatically. Set to true to auto-approve edits to matching files; set to false to always require explicit approval.");

        HBox editsToolbar = new HBox(8);
        addEditRuleBtn = createIconButton("+", () -> {
            editRulesData.add(new FileEditAutoApproveRule("src/**/*.java", "Java sources", "Custom", false));
            notifyModified();
        });
        removeEditRuleBtn = createIconButton("-", () -> {
            FileEditAutoApproveRule selected = editRulesTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                editRulesData.remove(selected);
                notifyModified();
            }
        });
        resetEditRulesBtn = createIconButton("↺", this::resetEditRulesToDefault);
        editsToolbar.getChildren().addAll(addEditRuleBtn, removeEditRuleBtn, resetEditRulesBtn);

        editRulesTable = buildEditRulesTable();
        uncoveredEditsCheck = createCheckBox("Auto-approve file edits not covered by rules");
        Label uncoveredEditsHint = createHintLabel("When enabled, file edits not covered by the rules above are automatically approved. When disabled, unmatched edits require explicit approval. Use this setting at your own discretion, as file edits outside the defined rules will be auto-approved.");
        uncoveredEditsHint.setPadding(new Insets(0, 0, 0, 22));

        // 3c. MCP Tool Auto-approve
        HBox mcpHeaderBox = new HBox(6, createSubHeader("MCP Tool Auto-approve"), createHelpIcon());
        mcpHeaderBox.setAlignment(Pos.CENTER_LEFT);
        Label mcpHint = createHintLabel("Controls whether MCP tool calls require explicit approval before running.");

        trustMcpAnnotationsCheck = createCheckBox("Trust MCP Tool Annotations");
        Label trustMcpHint = createHintLabel("If enabled, Copilot will use tool annotations to decide whether to automatically approve readonly MCP tool calls.");
        trustMcpHint.setPadding(new Insets(0, 0, 0, 22));

        Label mcpConfigLabel = new Label("MCP Server and Tool Auto-approve Configuration");
        mcpConfigLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label mcpConfigHint = createHintLabel("Configure which MCP servers and tools are always auto-approved without confirmation. When you enable a server, all tools from that server are auto-approved.");

        configureMcpBtn = new Button("Configure...");
        configureMcpBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 14 4 14; -fx-cursor: hand;");

        // 3d. Global: Auto Approve
        globalAutoApproveCheck = new CheckBox("Global: Auto Approve ⚠️");
        globalAutoApproveCheck.setStyle("-fx-text-fill: #E3B341; -fx-font-size: 13px; -fx-font-weight: bold;");
        globalAutoApproveCheck.selectedProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        Label globalWarnHint = createHintLabel("Global auto-approve disables manual approval completely for all tools across all workspaces. When enabled, all tool calls are automatically approved without confirmation, overriding all per-category auto-approve settings. This is extremely dangerous and is never recommended.");
        globalWarnHint.setPadding(new Insets(0, 0, 0, 22));

        autoApproveContentBox.getChildren().addAll(
                terminalHeaderBox, terminalHint, terminalToolbar, terminalRulesTable, uncoveredCommandsCheck, uncoveredCommandsHint,
                new Separator(),
                editsHeaderBox, editsHint, editsToolbar, editRulesTable, uncoveredEditsCheck, uncoveredEditsHint,
                new Separator(),
                mcpHeaderBox, mcpHint, trustMcpAnnotationsCheck, trustMcpHint, mcpConfigLabel, mcpConfigHint, configureMcpBtn,
                new Separator(),
                globalAutoApproveCheck, globalWarnHint
        );
        autoApproveSection.getChildren().addAll(autoApproveToggleBtn, autoApproveContentBox);

        // --- 4. Open Telemetry Section (Collapsible) ---
        VBox otelSection = new VBox(12);
        Button otelToggleBtn = createCollapsibleHeader("Open Telemetry");
        otelContentBox = new VBox(12);
        otelContentBox.setPadding(new Insets(4, 0, 4, 16));

        otelToggleBtn.setOnAction(e -> {
            boolean visible = !otelContentBox.isVisible();
            otelContentBox.setVisible(visible);
            otelContentBox.setManaged(visible);
            otelToggleBtn.setText(visible ? "▾  Open Telemetry" : "▸  Open Telemetry");
        });

        enableOtelExportCheck = createCheckBox("Enable Open Telemetry export");
        Label otelExportHint = createHintLabel("Export agent telemetry to an Open Telemetry collector. Applies to the Copilot and Claude Code agents. Requires IDE restart to take effect.");
        otelExportHint.setPadding(new Insets(0, 0, 0, 22));

        Label otelExpLabel = createFieldLabel("Exporter type");
        otelExpLabel.setPrefWidth(160);
        otelExporterTypeCombo = new ComboBox<>();
        otelExporterTypeCombo.getItems().addAll("otlp-http", "otlp-grpc");
        otelExporterTypeCombo.setValue("otlp-http");
        otelExporterTypeCombo.setPrefWidth(180);
        styleComboBox(otelExporterTypeCombo);
        otelExporterTypeCombo.valueProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        HBox otelExpRow = new HBox(12, otelExpLabel, otelExporterTypeCombo);
        otelExpRow.setAlignment(Pos.CENTER_LEFT);

        Label otelProtoLabel = createFieldLabel("OTLP protocol");
        otelProtoLabel.setPrefWidth(160);
        otelProtocolCombo = new ComboBox<>();
        otelProtocolCombo.getItems().addAll("http/json", "http/protobuf");
        otelProtocolCombo.setValue("http/json");
        otelProtocolCombo.setPrefWidth(180);
        styleComboBox(otelProtocolCombo);
        otelProtocolCombo.valueProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        HBox otelProtoRow = new HBox(12, otelProtoLabel, otelProtocolCombo);
        otelProtoRow.setAlignment(Pos.CENTER_LEFT);

        Label otelEndpointLabel = createFieldLabel("OTLP endpoint");
        otelEndpointLabel.setPrefWidth(160);
        otelEndpointField = new TextField();
        otelEndpointField.setPrefWidth(320);
        styleTextField(otelEndpointField);
        otelEndpointField.textProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        HBox otelEndpointRow = new HBox(12, otelEndpointLabel, otelEndpointField);
        otelEndpointRow.setAlignment(Pos.CENTER_LEFT);
        Label otelEndpointHint = createHintLabel("OTLP collector endpoint URL (e.g. http://localhost:4318).");
        otelEndpointHint.setPadding(new Insets(0, 0, 0, 172));

        Label otelFileLabel = createFieldLabel("Output file");
        otelFileLabel.setPrefWidth(160);
        otelOutputFileField = new TextField();
        otelOutputFileField.setPrefWidth(320);
        styleTextField(otelOutputFileField);
        otelOutputFileField.textProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        HBox otelFileRow = new HBox(12, otelFileLabel, otelOutputFileField);
        otelFileRow.setAlignment(Pos.CENTER_LEFT);
        Label otelFileHint = createHintLabel("Specify the output file path. When provided, the exporter type is automatically set to File. Available for Copilot CLI agent only; Claude agent does not support file export.");
        otelFileHint.setPadding(new Insets(0, 0, 0, 172));

        otelCaptureContentCheck = createCheckBox("Capture prompt/response content");
        Label otelCaptureHint = createHintLabel("include prompt and response content in exported telemetry. Contains potentially sensitive data.");
        otelCaptureHint.setPadding(new Insets(0, 0, 0, 22));

        Label otelServiceLabel = createFieldLabel("Service name");
        otelServiceLabel.setPrefWidth(160);
        otelServiceNameField = new TextField();
        otelServiceNameField.setPrefWidth(320);
        styleTextField(otelServiceNameField);
        otelServiceNameField.textProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        HBox otelServiceRow = new HBox(12, otelServiceLabel, otelServiceNameField);
        otelServiceRow.setAlignment(Pos.CENTER_LEFT);

        Label resAttrsLabel = createFieldLabel("Resource attributes");
        HBox resAttrsToolbar = new HBox(8);
        addOtelAttrBtn = createIconButton("+", () -> {
            otelAttrsData.add(new TelemetryResourceAttribute("env", "development"));
            notifyModified();
        });
        removeOtelAttrBtn = createIconButton("-", () -> {
            TelemetryResourceAttribute selected = otelAttrsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                otelAttrsData.remove(selected);
                notifyModified();
            }
        });
        resAttrsToolbar.getChildren().addAll(addOtelAttrBtn, removeOtelAttrBtn);

        otelAttrsTable = buildOtelAttrsTable();

        otelContentBox.getChildren().addAll(
                enableOtelExportCheck, otelExportHint,
                otelExpRow, otelProtoRow,
                otelEndpointRow, otelEndpointHint,
                otelFileRow, otelFileHint,
                otelCaptureContentCheck, otelCaptureHint,
                otelServiceRow,
                resAttrsLabel, resAttrsToolbar, otelAttrsTable
        );
        otelSection.getChildren().addAll(otelToggleBtn, otelContentBox);

        getChildren().addAll(generalSection, agentSection, autoApproveSection, otelSection);
    }

    private TableView<TerminalAutoApproveRule> buildTerminalRulesTable() {
        TableView<TerminalAutoApproveRule> table = new TableView<>(terminalRulesData);
        table.setPrefHeight(150);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");

        TableColumn<TerminalAutoApproveRule, String> cmdCol = new TableColumn<>("Command");
        cmdCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPattern()));
        cmdCol.setCellFactory(TextFieldTableCell.forTableColumn());
        cmdCol.setOnEditCommit(event -> {
            event.getRowValue().setPattern(event.getNewValue());
            notifyModified();
        });
        cmdCol.setPrefWidth(380);

        TableColumn<TerminalAutoApproveRule, Void> approveCol = new TableColumn<>("Auto-Approve");
        approveCol.setCellFactory(param -> new TableCell<>() {
            private final RadioButton alwaysRadio = new RadioButton("Always");
            private final RadioButton neverRadio = new RadioButton("Never");
            private final ToggleGroup group = new ToggleGroup();
            private final HBox radioBox = new HBox(12, alwaysRadio, neverRadio);

            {
                alwaysRadio.setToggleGroup(group);
                neverRadio.setToggleGroup(group);
                alwaysRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                neverRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                radioBox.setAlignment(Pos.CENTER_LEFT);

                alwaysRadio.setOnAction(e -> {
                    TerminalAutoApproveRule rule = getTableView().getItems().get(getIndex());
                    rule.setAutoApprove(true);
                    notifyModified();
                });

                neverRadio.setOnAction(e -> {
                    TerminalAutoApproveRule rule = getTableView().getItems().get(getIndex());
                    rule.setAutoApprove(false);
                    notifyModified();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    TerminalAutoApproveRule rule = getTableView().getItems().get(getIndex());
                    if (rule.isAutoApprove()) {
                        alwaysRadio.setSelected(true);
                    } else {
                        neverRadio.setSelected(true);
                    }
                    setGraphic(radioBox);
                }
            }
        });
        approveCol.setPrefWidth(160);

        table.getColumns().addAll(cmdCol, approveCol);
        return table;
    }

    private TableView<FileEditAutoApproveRule> buildEditRulesTable() {
        TableView<FileEditAutoApproveRule> table = new TableView<>(editRulesData);
        table.setPrefHeight(150);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");

        TableColumn<FileEditAutoApproveRule, String> patCol = new TableColumn<>("Pattern");
        patCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPattern()));
        patCol.setCellFactory(TextFieldTableCell.forTableColumn());
        patCol.setOnEditCommit(event -> {
            event.getRowValue().setPattern(event.getNewValue());
            notifyModified();
        });
        patCol.setPrefWidth(220);

        TableColumn<FileEditAutoApproveRule, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        descCol.setCellFactory(TextFieldTableCell.forTableColumn());
        descCol.setOnEditCommit(event -> {
            event.getRowValue().setDescription(event.getNewValue());
            notifyModified();
        });
        descCol.setPrefWidth(220);

        TableColumn<FileEditAutoApproveRule, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType()));
        typeCol.setPrefWidth(80);

        TableColumn<FileEditAutoApproveRule, Void> approveCol = new TableColumn<>("Auto-Approve");
        approveCol.setCellFactory(param -> new TableCell<>() {
            private final RadioButton alwaysRadio = new RadioButton("Always");
            private final RadioButton neverRadio = new RadioButton("Never");
            private final ToggleGroup group = new ToggleGroup();
            private final HBox radioBox = new HBox(12, alwaysRadio, neverRadio);

            {
                alwaysRadio.setToggleGroup(group);
                neverRadio.setToggleGroup(group);
                alwaysRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                neverRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                radioBox.setAlignment(Pos.CENTER_LEFT);

                alwaysRadio.setOnAction(e -> {
                    FileEditAutoApproveRule rule = getTableView().getItems().get(getIndex());
                    rule.setAutoApprove(true);
                    notifyModified();
                });

                neverRadio.setOnAction(e -> {
                    FileEditAutoApproveRule rule = getTableView().getItems().get(getIndex());
                    rule.setAutoApprove(false);
                    notifyModified();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    FileEditAutoApproveRule rule = getTableView().getItems().get(getIndex());
                    if (rule.isAutoApprove()) {
                        alwaysRadio.setSelected(true);
                    } else {
                        neverRadio.setSelected(true);
                    }
                    setGraphic(radioBox);
                }
            }
        });
        approveCol.setPrefWidth(160);

        table.getColumns().addAll(patCol, descCol, typeCol, approveCol);
        return table;
    }

    private TableView<TelemetryResourceAttribute> buildOtelAttrsTable() {
        TableView<TelemetryResourceAttribute> table = new TableView<>(otelAttrsData);
        table.setPrefHeight(130);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        table.setPlaceholder(new Label("Nothing to show"));

        TableColumn<TelemetryResourceAttribute, String> keyCol = new TableColumn<>("Key");
        keyCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getKey()));
        keyCol.setCellFactory(TextFieldTableCell.forTableColumn());
        keyCol.setOnEditCommit(event -> {
            event.getRowValue().setKey(event.getNewValue());
            notifyModified();
        });
        keyCol.setPrefWidth(220);

        TableColumn<TelemetryResourceAttribute, String> valCol = new TableColumn<>("Value");
        valCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getValue()));
        valCol.setCellFactory(TextFieldTableCell.forTableColumn());
        valCol.setOnEditCommit(event -> {
            event.getRowValue().setValue(event.getNewValue());
            notifyModified();
        });
        valCol.setPrefWidth(280);

        table.getColumns().addAll(keyCol, valCol);
        return table;
    }

    private Button createCollapsibleHeader(String title) {
        Button b = new Button("▾  " + title);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0;");
        return b;
    }

    private Label createSectionHeader(String title) {
        Label l = new Label(title);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
        return l;
    }

    private Label createSubHeader(String title) {
        Label l = new Label(title);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
        return l;
    }

    private Label createHelpIcon() {
        Label icon = new Label("?");
        icon.setStyle("-fx-background-color: #393B40; -fx-text-fill: #868A91; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 1 5 1 5;");
        return icon;
    }

    private Label createFieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return l;
    }

    private Label createHintLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        l.setWrapText(true);
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

    private Button createIconButton(String symbol, Runnable action) {
        Button b = new Button(symbol);
        b.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 2 10 2 10; -fx-font-size: 12px;");
        b.setOnAction(e -> action.run());
        return b;
    }

    private Button createBrowseButton(TextField targetField) {
        Button b = new Button("Browse...");
        b.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 3 10 3 10;");
        b.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select CLI Executable");
            File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (file != null) {
                targetField.setText(file.getAbsolutePath());
                notifyModified();
            }
        });
        return b;
    }

    private void styleTextField(TextField tf) {
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 7 3 7;");
    }

    private void styleComboBox(ComboBox<String> cb) {
        cb.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
    }

    private void resetTerminalRulesToDefault() {
        terminalRulesData.clear();
        terminalRulesData.add(new TerminalAutoApproveRule("^/*find\\b.*-(delete|exec|execdir|fprint|fprint0|fls|ok|okdir)\\b/", false));
        terminalRulesData.add(new TerminalAutoApproveRule("^/*Remove-Item\\b/", false));
        terminalRulesData.add(new TerminalAutoApproveRule("^/*sort\\b.*-[o|S]\\b/", false));
        terminalRulesData.add(new TerminalAutoApproveRule("^/*trash\\b.*-f\\b/", false));
        notifyModified();
    }

    private void resetEditRulesToDefault() {
        editRulesData.clear();
        editRulesData.add(new FileEditAutoApproveRule("**/.github/instructions/*", "Github instructions files", "Default", false));
        editRulesData.add(new FileEditAutoApproveRule("**/.lumina/**/*", "Lumina settings files", "Default", false));
        editRulesData.add(new FileEditAutoApproveRule("**/github-copilot/**/*", "Github Copilot settings and token files", "Default", false));
        notifyModified();
    }

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();

            autoModelCheck.setSelected(initialSettings.isEnableAutoModel());
            naturalLangCombo.setValue(initialSettings.getNaturalLanguage());
            diffViewModeCombo.setValue(initialSettings.getDiffViewMode());
            autoAcceptDelayField.setText(String.valueOf(initialSettings.getAutoAcceptDelay()));
            showInlineGutterCheck.setSelected(initialSettings.isShowInlineChatGutterIcon());
            semanticSearchCheck.setSelected(initialSettings.isEnableSemanticSearch());

            agentModeCheck.setSelected(initialSettings.isEnableAgentMode());
            agentMaxReqField.setText(String.valueOf(initialSettings.getAgentMaxRequests()));
            anthropicTokensField.setText(String.valueOf(initialSettings.getAnthropicThinkingBudgetTokens()));
            customAgentCheck.setSelected(initialSettings.isEnableCustomAgent());
            orgCustomAgentsCheck.setSelected(initialSettings.isEnableOrganizationCustomAgents());
            subagentCheck.setSelected(initialSettings.isEnableSubagent());
            cloudAgentCheck.setSelected(initialSettings.isEnableCloudAgent());
            skillsCheck.setSelected(initialSettings.isEnableSkills());
            hooksCheck.setSelected(initialSettings.isEnableHooks());
            pluginsCheck.setSelected(initialSettings.isEnablePlugins());
            codeReviewCheck.setSelected(initialSettings.isEnableCodeReview());
            byokCheck.setSelected(initialSettings.isEnableBringYourOwnKey());
            copilotRemoteCheck.setSelected(initialSettings.isEnableCopilotRemote());
            debugLoggingCheck.setSelected(initialSettings.isEnableAgentDebugFileLogging());
            notifyAttentionCheck.setSelected(initialSettings.notifyWhenCopilotNeedsAttention());

            claudeCliCheck.setSelected(initialSettings.isEnableClaudeCodeCliPreview());
            claudeCliPathField.setText(initialSettings.getClaudeCodeCliPath());
            codexCliCheck.setSelected(initialSettings.isEnableCodexCliPreview());
            codexCliPathField.setText(initialSettings.getCodexCliPath());

            terminalRulesData.clear();
            for (TerminalAutoApproveRule r : initialSettings.getTerminalAutoApproveRules()) {
                terminalRulesData.add(r.clone());
            }
            uncoveredCommandsCheck.setSelected(initialSettings.isAutoApproveUncoveredCommands());

            editRulesData.clear();
            for (FileEditAutoApproveRule r : initialSettings.getFileEditAutoApproveRules()) {
                editRulesData.add(r.clone());
            }
            uncoveredEditsCheck.setSelected(initialSettings.isAutoApproveFileEditsNotCovered());

            trustMcpAnnotationsCheck.setSelected(initialSettings.isTrustMcpToolAnnotations());
            globalAutoApproveCheck.setSelected(initialSettings.isGlobalAutoApprove());

            enableOtelExportCheck.setSelected(initialSettings.isEnableOpenTelemetryExport());
            otelExporterTypeCombo.setValue(initialSettings.getOpenTelemetryExporterType());
            otelProtocolCombo.setValue(initialSettings.getOpenTelemetryProtocol());
            otelEndpointField.setText(initialSettings.getOpenTelemetryEndpoint());
            otelOutputFileField.setText(initialSettings.getOpenTelemetryOutputFile());
            otelCaptureContentCheck.setSelected(initialSettings.isOpenTelemetryCaptureContent());
            otelServiceNameField.setText(initialSettings.getOpenTelemetryServiceName());

            otelAttrsData.clear();
            for (TelemetryResourceAttribute attr : initialSettings.getOpenTelemetryResourceAttributes()) {
                otelAttrsData.add(attr.clone());
            }
        } finally {
            updating = false;
        }
    }

    private GitHubCopilotChatSettings getCurrentSettingsFromUI() {
        GitHubCopilotChatSettings s = new GitHubCopilotChatSettings();

        s.setEnableAutoModel(autoModelCheck.isSelected());
        s.setNaturalLanguage(naturalLangCombo.getValue());
        s.setDiffViewMode(diffViewModeCombo.getValue());
        try {
            s.setAutoAcceptDelay(Integer.parseInt(autoAcceptDelayField.getText().trim()));
        } catch (NumberFormatException ignored) {}
        s.setShowInlineChatGutterIcon(showInlineGutterCheck.isSelected());
        s.setEnableSemanticSearch(semanticSearchCheck.isSelected());

        s.setEnableAgentMode(agentModeCheck.isSelected());
        try {
            s.setAgentMaxRequests(Integer.parseInt(agentMaxReqField.getText().trim()));
        } catch (NumberFormatException ignored) {}
        try {
            s.setAnthropicThinkingBudgetTokens(Integer.parseInt(anthropicTokensField.getText().trim()));
        } catch (NumberFormatException ignored) {}
        s.setEnableCustomAgent(customAgentCheck.isSelected());
        s.setEnableOrganizationCustomAgents(orgCustomAgentsCheck.isSelected());
        s.setEnableSubagent(subagentCheck.isSelected());
        s.setEnableCloudAgent(cloudAgentCheck.isSelected());
        s.setEnableSkills(skillsCheck.isSelected());
        s.setEnableHooks(hooksCheck.isSelected());
        s.setEnablePlugins(pluginsCheck.isSelected());
        s.setEnableCodeReview(codeReviewCheck.isSelected());
        s.setEnableBringYourOwnKey(byokCheck.isSelected());
        s.setEnableCopilotRemote(copilotRemoteCheck.isSelected());
        s.setEnableAgentDebugFileLogging(debugLoggingCheck.isSelected());
        s.setNotifyWhenCopilotNeedsAttention(notifyAttentionCheck.isSelected());

        s.setEnableClaudeCodeCliPreview(claudeCliCheck.isSelected());
        s.setClaudeCodeCliPath(claudeCliPathField.getText().trim());
        s.setEnableCodexCliPreview(codexCliCheck.isSelected());
        s.setCodexCliPath(codexCliPathField.getText().trim());

        List<TerminalAutoApproveRule> tList = new ArrayList<>();
        for (TerminalAutoApproveRule rule : terminalRulesData) {
            tList.add(rule.clone());
        }
        s.setTerminalAutoApproveRules(tList);
        s.setAutoApproveUncoveredCommands(uncoveredCommandsCheck.isSelected());

        List<FileEditAutoApproveRule> eList = new ArrayList<>();
        for (FileEditAutoApproveRule rule : editRulesData) {
            eList.add(rule.clone());
        }
        s.setFileEditAutoApproveRules(eList);
        s.setAutoApproveFileEditsNotCovered(uncoveredEditsCheck.isSelected());

        s.setTrustMcpToolAnnotations(trustMcpAnnotationsCheck.isSelected());
        s.setGlobalAutoApprove(globalAutoApproveCheck.isSelected());

        s.setEnableOpenTelemetryExport(enableOtelExportCheck.isSelected());
        s.setOpenTelemetryExporterType(otelExporterTypeCombo.getValue());
        s.setOpenTelemetryProtocol(otelProtocolCombo.getValue());
        s.setOpenTelemetryEndpoint(otelEndpointField.getText().trim());
        s.setOpenTelemetryOutputFile(otelOutputFileField.getText().trim());
        s.setOpenTelemetryCaptureContent(otelCaptureContentCheck.isSelected());
        s.setOpenTelemetryServiceName(otelServiceNameField.getText().trim());

        List<TelemetryResourceAttribute> aList = new ArrayList<>();
        for (TelemetryResourceAttribute attr : otelAttrsData) {
            aList.add(attr.clone());
        }
        s.setOpenTelemetryResourceAttributes(aList);

        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        GitHubCopilotChatSettings updated = getCurrentSettingsFromUI();
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
    public CheckBox getAutoModelCheck() { return autoModelCheck; }
    public ComboBox<String> getNaturalLangCombo() { return naturalLangCombo; }
    public ComboBox<String> getDiffViewModeCombo() { return diffViewModeCombo; }
    public TextField getAutoAcceptDelayField() { return autoAcceptDelayField; }
    public CheckBox getShowInlineGutterCheck() { return showInlineGutterCheck; }
    public CheckBox getSemanticSearchCheck() { return semanticSearchCheck; }

    public CheckBox getAgentModeCheck() { return agentModeCheck; }
    public TextField getAgentMaxReqField() { return agentMaxReqField; }
    public TextField getAnthropicTokensField() { return anthropicTokensField; }
    public CheckBox getCustomAgentCheck() { return customAgentCheck; }
    public CheckBox getOrgCustomAgentsCheck() { return orgCustomAgentsCheck; }
    public CheckBox getSubagentCheck() { return subagentCheck; }
    public CheckBox getCloudAgentCheck() { return cloudAgentCheck; }
    public CheckBox getSkillsCheck() { return skillsCheck; }
    public CheckBox getHooksCheck() { return hooksCheck; }
    public CheckBox getPluginsCheck() { return pluginsCheck; }
    public CheckBox getCodeReviewCheck() { return codeReviewCheck; }
    public CheckBox getByokCheck() { return byokCheck; }
    public CheckBox getCopilotRemoteCheck() { return copilotRemoteCheck; }
    public CheckBox getDebugLoggingCheck() { return debugLoggingCheck; }
    public CheckBox getNotifyAttentionCheck() { return notifyAttentionCheck; }
    public CheckBox getClaudeCliCheck() { return claudeCliCheck; }
    public TextField getClaudeCliPathField() { return claudeCliPathField; }
    public Button getClaudeCliBrowseBtn() { return claudeCliBrowseBtn; }
    public CheckBox getCodexCliCheck() { return codexCliCheck; }
    public TextField getCodexCliPathField() { return codexCliPathField; }
    public Button getCodexCliBrowseBtn() { return codexCliBrowseBtn; }

    public ObservableList<TerminalAutoApproveRule> getTerminalRulesData() { return terminalRulesData; }
    public TableView<TerminalAutoApproveRule> getTerminalRulesTable() { return terminalRulesTable; }
    public Button getAddTerminalRuleBtn() { return addTerminalRuleBtn; }
    public Button getRemoveTerminalRuleBtn() { return removeTerminalRuleBtn; }
    public Button getResetTerminalRulesBtn() { return resetTerminalRulesBtn; }
    public CheckBox getUncoveredCommandsCheck() { return uncoveredCommandsCheck; }

    public ObservableList<FileEditAutoApproveRule> getEditRulesData() { return editRulesData; }
    public TableView<FileEditAutoApproveRule> getEditRulesTable() { return editRulesTable; }
    public Button getAddEditRuleBtn() { return addEditRuleBtn; }
    public Button getRemoveEditRuleBtn() { return removeEditRuleBtn; }
    public Button getResetEditRulesBtn() { return resetEditRulesBtn; }
    public CheckBox getUncoveredEditsCheck() { return uncoveredEditsCheck; }

    public CheckBox getTrustMcpAnnotationsCheck() { return trustMcpAnnotationsCheck; }
    public Button getConfigureMcpBtn() { return configureMcpBtn; }
    public CheckBox getGlobalAutoApproveCheck() { return globalAutoApproveCheck; }

    public CheckBox getEnableOtelExportCheck() { return enableOtelExportCheck; }
    public ComboBox<String> getOtelExporterTypeCombo() { return otelExporterTypeCombo; }
    public ComboBox<String> getOtelProtocolCombo() { return otelProtocolCombo; }
    public TextField getOtelEndpointField() { return otelEndpointField; }
    public TextField getOtelOutputFileField() { return otelOutputFileField; }
    public CheckBox getOtelCaptureContentCheck() { return otelCaptureContentCheck; }
    public TextField getOtelServiceNameField() { return otelServiceNameField; }
    public ObservableList<TelemetryResourceAttribute> getOtelAttrsData() { return otelAttrsData; }
    public TableView<TelemetryResourceAttribute> getOtelAttrsTable() { return otelAttrsTable; }
}
