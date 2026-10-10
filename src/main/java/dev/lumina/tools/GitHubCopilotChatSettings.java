package dev.lumina.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing Tools > GitHub Copilot > Chat settings in Lumina IDE.
 */
public class GitHubCopilotChatSettings implements Cloneable {

    // General
    private boolean enableAutoModel = true;
    private String naturalLanguage = "English";
    private String diffViewMode = "Copilot (Modern Inline)";
    private int autoAcceptDelay = 0;
    private boolean showInlineChatGutterIcon = true;
    private boolean enableSemanticSearch = false;

    // Agent
    private boolean enableAgentMode = true;
    private int agentMaxRequests = 50;
    private int anthropicThinkingBudgetTokens = 1024;
    private boolean enableCustomAgent = true;
    private boolean enableOrganizationCustomAgents = true;
    private boolean enableSubagent = true;
    private boolean enableCloudAgent = false;
    private boolean enableSkills = false;
    private boolean enableHooks = true;
    private boolean enablePlugins = true;
    private boolean enableCodeReview = false;
    private boolean enableBringYourOwnKey = true;
    private boolean enableCopilotRemote = false;
    private boolean enableAgentDebugFileLogging = false;
    private boolean notifyWhenCopilotNeedsAttention = true;
    private boolean enableClaudeCodeCliPreview = false;
    private String claudeCodeCliPath = "";
    private boolean enableCodexCliPreview = false;
    private String codexCliPath = "";

    // Auto Approve: Terminal
    private List<TerminalAutoApproveRule> terminalAutoApproveRules = new ArrayList<>();
    private boolean autoApproveUncoveredCommands = false;

    // Auto Approve: Edits
    private List<FileEditAutoApproveRule> fileEditAutoApproveRules = new ArrayList<>();
    private boolean autoApproveFileEditsNotCovered = true;

    // Auto Approve: MCP
    private boolean trustMcpToolAnnotations = false;

    // Global: Auto Approve
    private boolean globalAutoApprove = false;

    // Open Telemetry
    private boolean enableOpenTelemetryExport = false;
    private String openTelemetryExporterType = "otlp-http";
    private String openTelemetryProtocol = "http/json";
    private String openTelemetryEndpoint = "";
    private String openTelemetryOutputFile = "";
    private boolean openTelemetryCaptureContent = false;
    private String openTelemetryServiceName = "";
    private List<TelemetryResourceAttribute> openTelemetryResourceAttributes = new ArrayList<>();

    public GitHubCopilotChatSettings() {
        initDefaultRules();
    }

    private void initDefaultRules() {
        terminalAutoApproveRules.clear();
        terminalAutoApproveRules.add(new TerminalAutoApproveRule("^/*find\\b.*-(delete|exec|execdir|fprint|fprint0|fls|ok|okdir)\\b/", false));
        terminalAutoApproveRules.add(new TerminalAutoApproveRule("^/*Remove-Item\\b/", false));
        terminalAutoApproveRules.add(new TerminalAutoApproveRule("^/*sort\\b.*-[o|S]\\b/", false));
        terminalAutoApproveRules.add(new TerminalAutoApproveRule("^/*trash\\b.*-f\\b/", false));

        fileEditAutoApproveRules.clear();
        fileEditAutoApproveRules.add(new FileEditAutoApproveRule("**/.github/instructions/*", "Github instructions files", "Default", false));
        fileEditAutoApproveRules.add(new FileEditAutoApproveRule("**/.lumina/**/*", "Lumina settings files", "Default", false));
        fileEditAutoApproveRules.add(new FileEditAutoApproveRule("**/github-copilot/**/*", "Github Copilot settings and token files", "Default", false));
    }

    // Getters and setters
    public boolean isEnableAutoModel() { return enableAutoModel; }
    public void setEnableAutoModel(boolean enableAutoModel) { this.enableAutoModel = enableAutoModel; }

    public String getNaturalLanguage() { return naturalLanguage; }
    public void setNaturalLanguage(String naturalLanguage) { this.naturalLanguage = naturalLanguage != null ? naturalLanguage : "English"; }

    public String getDiffViewMode() { return diffViewMode; }
    public void setDiffViewMode(String diffViewMode) { this.diffViewMode = diffViewMode != null ? diffViewMode : "Copilot (Modern Inline)"; }

    public int getAutoAcceptDelay() { return autoAcceptDelay; }
    public void setAutoAcceptDelay(int autoAcceptDelay) { this.autoAcceptDelay = autoAcceptDelay; }

    public boolean isShowInlineChatGutterIcon() { return showInlineChatGutterIcon; }
    public void setShowInlineChatGutterIcon(boolean showInlineChatGutterIcon) { this.showInlineChatGutterIcon = showInlineChatGutterIcon; }

    public boolean isEnableSemanticSearch() { return enableSemanticSearch; }
    public void setEnableSemanticSearch(boolean enableSemanticSearch) { this.enableSemanticSearch = enableSemanticSearch; }

    public boolean isEnableAgentMode() { return enableAgentMode; }
    public void setEnableAgentMode(boolean enableAgentMode) { this.enableAgentMode = enableAgentMode; }

    public int getAgentMaxRequests() { return agentMaxRequests; }
    public void setAgentMaxRequests(int agentMaxRequests) { this.agentMaxRequests = agentMaxRequests; }

    public int getAnthropicThinkingBudgetTokens() { return anthropicThinkingBudgetTokens; }
    public void setAnthropicThinkingBudgetTokens(int anthropicThinkingBudgetTokens) { this.anthropicThinkingBudgetTokens = anthropicThinkingBudgetTokens; }

    public boolean isEnableCustomAgent() { return enableCustomAgent; }
    public void setEnableCustomAgent(boolean enableCustomAgent) { this.enableCustomAgent = enableCustomAgent; }

    public boolean isEnableOrganizationCustomAgents() { return enableOrganizationCustomAgents; }
    public void setEnableOrganizationCustomAgents(boolean enableOrganizationCustomAgents) { this.enableOrganizationCustomAgents = enableOrganizationCustomAgents; }

    public boolean isEnableSubagent() { return enableSubagent; }
    public void setEnableSubagent(boolean enableSubagent) { this.enableSubagent = enableSubagent; }

    public boolean isEnableCloudAgent() { return enableCloudAgent; }
    public void setEnableCloudAgent(boolean enableCloudAgent) { this.enableCloudAgent = enableCloudAgent; }

    public boolean isEnableSkills() { return enableSkills; }
    public void setEnableSkills(boolean enableSkills) { this.enableSkills = enableSkills; }

    public boolean isEnableHooks() { return enableHooks; }
    public void setEnableHooks(boolean enableHooks) { this.enableHooks = enableHooks; }

    public boolean isEnablePlugins() { return enablePlugins; }
    public void setEnablePlugins(boolean enablePlugins) { this.enablePlugins = enablePlugins; }

    public boolean isEnableCodeReview() { return enableCodeReview; }
    public void setEnableCodeReview(boolean enableCodeReview) { this.enableCodeReview = enableCodeReview; }

    public boolean isEnableBringYourOwnKey() { return enableBringYourOwnKey; }
    public void setEnableBringYourOwnKey(boolean enableBringYourOwnKey) { this.enableBringYourOwnKey = enableBringYourOwnKey; }

    public boolean isEnableCopilotRemote() { return enableCopilotRemote; }
    public void setEnableCopilotRemote(boolean enableCopilotRemote) { this.enableCopilotRemote = enableCopilotRemote; }

    public boolean isEnableAgentDebugFileLogging() { return enableAgentDebugFileLogging; }
    public void setEnableAgentDebugFileLogging(boolean enableAgentDebugFileLogging) { this.enableAgentDebugFileLogging = enableAgentDebugFileLogging; }

    public boolean notifyWhenCopilotNeedsAttention() { return notifyWhenCopilotNeedsAttention; }
    public void setNotifyWhenCopilotNeedsAttention(boolean notifyWhenCopilotNeedsAttention) { this.notifyWhenCopilotNeedsAttention = notifyWhenCopilotNeedsAttention; }

    public boolean isEnableClaudeCodeCliPreview() { return enableClaudeCodeCliPreview; }
    public void setEnableClaudeCodeCliPreview(boolean enableClaudeCodeCliPreview) { this.enableClaudeCodeCliPreview = enableClaudeCodeCliPreview; }

    public String getClaudeCodeCliPath() { return claudeCodeCliPath; }
    public void setClaudeCodeCliPath(String claudeCodeCliPath) { this.claudeCodeCliPath = claudeCodeCliPath != null ? claudeCodeCliPath : ""; }

    public boolean isEnableCodexCliPreview() { return enableCodexCliPreview; }
    public void setEnableCodexCliPreview(boolean enableCodexCliPreview) { this.enableCodexCliPreview = enableCodexCliPreview; }

    public String getCodexCliPath() { return codexCliPath; }
    public void setCodexCliPath(String codexCliPath) { this.codexCliPath = codexCliPath != null ? codexCliPath : ""; }

    public List<TerminalAutoApproveRule> getTerminalAutoApproveRules() { return terminalAutoApproveRules; }
    public void setTerminalAutoApproveRules(List<TerminalAutoApproveRule> terminalAutoApproveRules) {
        this.terminalAutoApproveRules = terminalAutoApproveRules != null ? new ArrayList<>(terminalAutoApproveRules) : new ArrayList<>();
    }

    public boolean isAutoApproveUncoveredCommands() { return autoApproveUncoveredCommands; }
    public void setAutoApproveUncoveredCommands(boolean autoApproveUncoveredCommands) { this.autoApproveUncoveredCommands = autoApproveUncoveredCommands; }

    public List<FileEditAutoApproveRule> getFileEditAutoApproveRules() { return fileEditAutoApproveRules; }
    public void setFileEditAutoApproveRules(List<FileEditAutoApproveRule> fileEditAutoApproveRules) {
        this.fileEditAutoApproveRules = fileEditAutoApproveRules != null ? new ArrayList<>(fileEditAutoApproveRules) : new ArrayList<>();
    }

    public boolean isAutoApproveFileEditsNotCovered() { return autoApproveFileEditsNotCovered; }
    public void setAutoApproveFileEditsNotCovered(boolean autoApproveFileEditsNotCovered) { this.autoApproveFileEditsNotCovered = autoApproveFileEditsNotCovered; }

    public boolean isTrustMcpToolAnnotations() { return trustMcpToolAnnotations; }
    public void setTrustMcpToolAnnotations(boolean trustMcpToolAnnotations) { this.trustMcpToolAnnotations = trustMcpToolAnnotations; }

    public boolean isGlobalAutoApprove() { return globalAutoApprove; }
    public void setGlobalAutoApprove(boolean globalAutoApprove) { this.globalAutoApprove = globalAutoApprove; }

    public boolean isEnableOpenTelemetryExport() { return enableOpenTelemetryExport; }
    public void setEnableOpenTelemetryExport(boolean enableOpenTelemetryExport) { this.enableOpenTelemetryExport = enableOpenTelemetryExport; }

    public String getOpenTelemetryExporterType() { return openTelemetryExporterType; }
    public void setOpenTelemetryExporterType(String openTelemetryExporterType) { this.openTelemetryExporterType = openTelemetryExporterType != null ? openTelemetryExporterType : "otlp-http"; }

    public String getOpenTelemetryProtocol() { return openTelemetryProtocol; }
    public void setOpenTelemetryProtocol(String openTelemetryProtocol) { this.openTelemetryProtocol = openTelemetryProtocol != null ? openTelemetryProtocol : "http/json"; }

    public String getOpenTelemetryEndpoint() { return openTelemetryEndpoint; }
    public void setOpenTelemetryEndpoint(String openTelemetryEndpoint) { this.openTelemetryEndpoint = openTelemetryEndpoint != null ? openTelemetryEndpoint : ""; }

    public String getOpenTelemetryOutputFile() { return openTelemetryOutputFile; }
    public void setOpenTelemetryOutputFile(String openTelemetryOutputFile) { this.openTelemetryOutputFile = openTelemetryOutputFile != null ? openTelemetryOutputFile : ""; }

    public boolean isOpenTelemetryCaptureContent() { return openTelemetryCaptureContent; }
    public void setOpenTelemetryCaptureContent(boolean openTelemetryCaptureContent) { this.openTelemetryCaptureContent = openTelemetryCaptureContent; }

    public String getOpenTelemetryServiceName() { return openTelemetryServiceName; }
    public void setOpenTelemetryServiceName(String openTelemetryServiceName) { this.openTelemetryServiceName = openTelemetryServiceName != null ? openTelemetryServiceName : ""; }

    public List<TelemetryResourceAttribute> getOpenTelemetryResourceAttributes() { return openTelemetryResourceAttributes; }
    public void setOpenTelemetryResourceAttributes(List<TelemetryResourceAttribute> openTelemetryResourceAttributes) {
        this.openTelemetryResourceAttributes = openTelemetryResourceAttributes != null ? new ArrayList<>(openTelemetryResourceAttributes) : new ArrayList<>();
    }

    @Override
    public GitHubCopilotChatSettings clone() {
        try {
            GitHubCopilotChatSettings copy = (GitHubCopilotChatSettings) super.clone();
            copy.terminalAutoApproveRules = new ArrayList<>();
            for (TerminalAutoApproveRule rule : this.terminalAutoApproveRules) {
                copy.terminalAutoApproveRules.add(rule.clone());
            }
            copy.fileEditAutoApproveRules = new ArrayList<>();
            for (FileEditAutoApproveRule rule : this.fileEditAutoApproveRules) {
                copy.fileEditAutoApproveRules.add(rule.clone());
            }
            copy.openTelemetryResourceAttributes = new ArrayList<>();
            for (TelemetryResourceAttribute attr : this.openTelemetryResourceAttributes) {
                copy.openTelemetryResourceAttributes.add(attr.clone());
            }
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GitHubCopilotChatSettings that = (GitHubCopilotChatSettings) o;
        return enableAutoModel == that.enableAutoModel &&
                autoAcceptDelay == that.autoAcceptDelay &&
                showInlineChatGutterIcon == that.showInlineChatGutterIcon &&
                enableSemanticSearch == that.enableSemanticSearch &&
                enableAgentMode == that.enableAgentMode &&
                agentMaxRequests == that.agentMaxRequests &&
                anthropicThinkingBudgetTokens == that.anthropicThinkingBudgetTokens &&
                enableCustomAgent == that.enableCustomAgent &&
                enableOrganizationCustomAgents == that.enableOrganizationCustomAgents &&
                enableSubagent == that.enableSubagent &&
                enableCloudAgent == that.enableCloudAgent &&
                enableSkills == that.enableSkills &&
                enableHooks == that.enableHooks &&
                enablePlugins == that.enablePlugins &&
                enableCodeReview == that.enableCodeReview &&
                enableBringYourOwnKey == that.enableBringYourOwnKey &&
                enableCopilotRemote == that.enableCopilotRemote &&
                enableAgentDebugFileLogging == that.enableAgentDebugFileLogging &&
                notifyWhenCopilotNeedsAttention == that.notifyWhenCopilotNeedsAttention &&
                enableClaudeCodeCliPreview == that.enableClaudeCodeCliPreview &&
                enableCodexCliPreview == that.enableCodexCliPreview &&
                autoApproveUncoveredCommands == that.autoApproveUncoveredCommands &&
                autoApproveFileEditsNotCovered == that.autoApproveFileEditsNotCovered &&
                trustMcpToolAnnotations == that.trustMcpToolAnnotations &&
                globalAutoApprove == that.globalAutoApprove &&
                enableOpenTelemetryExport == that.enableOpenTelemetryExport &&
                openTelemetryCaptureContent == that.openTelemetryCaptureContent &&
                Objects.equals(naturalLanguage, that.naturalLanguage) &&
                Objects.equals(diffViewMode, that.diffViewMode) &&
                Objects.equals(claudeCodeCliPath, that.claudeCodeCliPath) &&
                Objects.equals(codexCliPath, that.codexCliPath) &&
                Objects.equals(terminalAutoApproveRules, that.terminalAutoApproveRules) &&
                Objects.equals(fileEditAutoApproveRules, that.fileEditAutoApproveRules) &&
                Objects.equals(openTelemetryExporterType, that.openTelemetryExporterType) &&
                Objects.equals(openTelemetryProtocol, that.openTelemetryProtocol) &&
                Objects.equals(openTelemetryEndpoint, that.openTelemetryEndpoint) &&
                Objects.equals(openTelemetryOutputFile, that.openTelemetryOutputFile) &&
                Objects.equals(openTelemetryServiceName, that.openTelemetryServiceName) &&
                Objects.equals(openTelemetryResourceAttributes, that.openTelemetryResourceAttributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enableAutoModel, naturalLanguage, diffViewMode, autoAcceptDelay,
                showInlineChatGutterIcon, enableSemanticSearch, enableAgentMode, agentMaxRequests,
                anthropicThinkingBudgetTokens, enableCustomAgent, enableOrganizationCustomAgents,
                enableSubagent, enableCloudAgent, enableSkills, enableHooks, enablePlugins,
                enableCodeReview, enableBringYourOwnKey, enableCopilotRemote, enableAgentDebugFileLogging,
                notifyWhenCopilotNeedsAttention, enableClaudeCodeCliPreview, claudeCodeCliPath,
                enableCodexCliPreview, codexCliPath, terminalAutoApproveRules, autoApproveUncoveredCommands,
                fileEditAutoApproveRules, autoApproveFileEditsNotCovered, trustMcpToolAnnotations,
                globalAutoApprove, enableOpenTelemetryExport, openTelemetryExporterType, openTelemetryProtocol,
                openTelemetryEndpoint, openTelemetryOutputFile, openTelemetryCaptureContent,
                openTelemetryServiceName, openTelemetryResourceAttributes);
    }

    @Override
    public String toString() {
        return "GitHubCopilotChatSettings{" +
                "enableAutoModel=" + enableAutoModel +
                ", naturalLanguage='" + naturalLanguage + '\'' +
                ", diffViewMode='" + diffViewMode + '\'' +
                ", autoAcceptDelay=" + autoAcceptDelay +
                ", enableAgentMode=" + enableAgentMode +
                ", agentMaxRequests=" + agentMaxRequests +
                '}';
    }
}
