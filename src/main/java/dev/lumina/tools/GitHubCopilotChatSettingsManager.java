package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manager handling persistence and change notifications for Tools > GitHub Copilot > Chat settings.
 */
public class GitHubCopilotChatSettingsManager {

    private static final GitHubCopilotChatSettingsManager INSTANCE = new GitHubCopilotChatSettingsManager();

    private static final String KEY_AUTO_MODEL = "tools.copilot.chat.auto_model";
    private static final String KEY_NATURAL_LANG = "tools.copilot.chat.natural_language";
    private static final String KEY_DIFF_VIEW_MODE = "tools.copilot.chat.diff_view_mode";
    private static final String KEY_AUTO_ACCEPT_DELAY = "tools.copilot.chat.auto_accept_delay";
    private static final String KEY_INLINE_GUTTER = "tools.copilot.chat.show_inline_gutter";
    private static final String KEY_SEMANTIC_SEARCH = "tools.copilot.chat.semantic_search";

    private static final String KEY_AGENT_MODE = "tools.copilot.chat.agent_mode";
    private static final String KEY_AGENT_MAX_REQ = "tools.copilot.chat.agent_max_requests";
    private static final String KEY_ANTHROPIC_TOKENS = "tools.copilot.chat.anthropic_tokens";
    private static final String KEY_CUSTOM_AGENT = "tools.copilot.chat.custom_agent";
    private static final String KEY_ORG_CUSTOM_AGENTS = "tools.copilot.chat.org_custom_agents";
    private static final String KEY_SUBAGENT = "tools.copilot.chat.subagent";
    private static final String KEY_CLOUD_AGENT = "tools.copilot.chat.cloud_agent";
    private static final String KEY_SKILLS = "tools.copilot.chat.skills";
    private static final String KEY_HOOKS = "tools.copilot.chat.hooks";
    private static final String KEY_PLUGINS = "tools.copilot.chat.plugins";
    private static final String KEY_CODE_REVIEW = "tools.copilot.chat.code_review";
    private static final String KEY_BYOK = "tools.copilot.chat.byok";
    private static final String KEY_REMOTE = "tools.copilot.chat.remote";
    private static final String KEY_DEBUG_LOGGING = "tools.copilot.chat.debug_logging";
    private static final String KEY_NOTIFY_ATTENTION = "tools.copilot.chat.notify_attention";
    private static final String KEY_CLAUDE_CLI = "tools.copilot.chat.claude_cli_enabled";
    private static final String KEY_CLAUDE_PATH = "tools.copilot.chat.claude_cli_path";
    private static final String KEY_CODEX_CLI = "tools.copilot.chat.codex_cli_enabled";
    private static final String KEY_CODEX_PATH = "tools.copilot.chat.codex_cli_path";

    // Auto Approve: Terminal
    private static final String KEY_RULES_COUNT = "tools.copilot.chat.rules.count";
    private static final String KEY_RULE_PATTERN_PREFIX = "tools.copilot.chat.rule.pattern.";
    private static final String KEY_RULE_APPROVE_PREFIX = "tools.copilot.chat.rule.approve.";
    private static final String KEY_UNCOVERED_COMMANDS = "tools.copilot.chat.uncovered_commands";

    // Auto Approve: Edits
    private static final String KEY_EDIT_RULES_COUNT = "tools.copilot.chat.edit_rules.count";
    private static final String KEY_EDIT_RULE_PATTERN_PREFIX = "tools.copilot.chat.edit_rule.pattern.";
    private static final String KEY_EDIT_RULE_DESC_PREFIX = "tools.copilot.chat.edit_rule.desc.";
    private static final String KEY_EDIT_RULE_TYPE_PREFIX = "tools.copilot.chat.edit_rule.type.";
    private static final String KEY_EDIT_RULE_APPROVE_PREFIX = "tools.copilot.chat.edit_rule.approve.";
    private static final String KEY_UNCOVERED_EDITS = "tools.copilot.chat.uncovered_edits";

    // Auto Approve: MCP & Global
    private static final String KEY_TRUST_MCP_ANNOTATIONS = "tools.copilot.chat.trust_mcp_annotations";
    private static final String KEY_GLOBAL_AUTO_APPROVE = "tools.copilot.chat.global_auto_approve";

    // Open Telemetry
    private static final String KEY_OTEL_ENABLE = "tools.copilot.chat.otel.enable";
    private static final String KEY_OTEL_EXPORTER = "tools.copilot.chat.otel.exporter";
    private static final String KEY_OTEL_PROTOCOL = "tools.copilot.chat.otel.protocol";
    private static final String KEY_OTEL_ENDPOINT = "tools.copilot.chat.otel.endpoint";
    private static final String KEY_OTEL_OUTPUT_FILE = "tools.copilot.chat.otel.output_file";
    private static final String KEY_OTEL_CAPTURE = "tools.copilot.chat.otel.capture";
    private static final String KEY_OTEL_SERVICE = "tools.copilot.chat.otel.service";
    private static final String KEY_OTEL_ATTRS_COUNT = "tools.copilot.chat.otel.attrs.count";
    private static final String KEY_OTEL_ATTR_KEY_PREFIX = "tools.copilot.chat.otel.attr.key.";
    private static final String KEY_OTEL_ATTR_VAL_PREFIX = "tools.copilot.chat.otel.attr.val.";

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private GitHubCopilotChatSettingsManager() {
    }

    public static GitHubCopilotChatSettingsManager getInstance() {
        return INSTANCE;
    }

    public GitHubCopilotChatSettings getSettings() {
        GitHubCopilotChatSettings s = new GitHubCopilotChatSettings();

        String am = Settings.get(KEY_AUTO_MODEL);
        if (am != null) s.setEnableAutoModel(Boolean.parseBoolean(am));

        String nl = Settings.get(KEY_NATURAL_LANG);
        if (nl != null && !nl.isBlank()) s.setNaturalLanguage(nl);

        String dvm = Settings.get(KEY_DIFF_VIEW_MODE);
        if (dvm != null && !dvm.isBlank()) s.setDiffViewMode(dvm);

        String aad = Settings.get(KEY_AUTO_ACCEPT_DELAY);
        if (aad != null) {
            try { s.setAutoAcceptDelay(Integer.parseInt(aad)); } catch (NumberFormatException ignored) {}
        }

        String ig = Settings.get(KEY_INLINE_GUTTER);
        if (ig != null) s.setShowInlineChatGutterIcon(Boolean.parseBoolean(ig));

        String ss = Settings.get(KEY_SEMANTIC_SEARCH);
        if (ss != null) s.setEnableSemanticSearch(Boolean.parseBoolean(ss));

        String agm = Settings.get(KEY_AGENT_MODE);
        if (agm != null) s.setEnableAgentMode(Boolean.parseBoolean(agm));

        String amr = Settings.get(KEY_AGENT_MAX_REQ);
        if (amr != null) {
            try { s.setAgentMaxRequests(Integer.parseInt(amr)); } catch (NumberFormatException ignored) {}
        }

        String at = Settings.get(KEY_ANTHROPIC_TOKENS);
        if (at != null) {
            try { s.setAnthropicThinkingBudgetTokens(Integer.parseInt(at)); } catch (NumberFormatException ignored) {}
        }

        String ca = Settings.get(KEY_CUSTOM_AGENT);
        if (ca != null) s.setEnableCustomAgent(Boolean.parseBoolean(ca));

        String oca = Settings.get(KEY_ORG_CUSTOM_AGENTS);
        if (oca != null) s.setEnableOrganizationCustomAgents(Boolean.parseBoolean(oca));

        String sa = Settings.get(KEY_SUBAGENT);
        if (sa != null) s.setEnableSubagent(Boolean.parseBoolean(sa));

        String cla = Settings.get(KEY_CLOUD_AGENT);
        if (cla != null) s.setEnableCloudAgent(Boolean.parseBoolean(cla));

        String sk = Settings.get(KEY_SKILLS);
        if (sk != null) s.setEnableSkills(Boolean.parseBoolean(sk));

        String hk = Settings.get(KEY_HOOKS);
        if (hk != null) s.setEnableHooks(Boolean.parseBoolean(hk));

        String pl = Settings.get(KEY_PLUGINS);
        if (pl != null) s.setEnablePlugins(Boolean.parseBoolean(pl));

        String cr = Settings.get(KEY_CODE_REVIEW);
        if (cr != null) s.setEnableCodeReview(Boolean.parseBoolean(cr));

        String by = Settings.get(KEY_BYOK);
        if (by != null) s.setEnableBringYourOwnKey(Boolean.parseBoolean(by));

        String rm = Settings.get(KEY_REMOTE);
        if (rm != null) s.setEnableCopilotRemote(Boolean.parseBoolean(rm));

        String dl = Settings.get(KEY_DEBUG_LOGGING);
        if (dl != null) s.setEnableAgentDebugFileLogging(Boolean.parseBoolean(dl));

        String na = Settings.get(KEY_NOTIFY_ATTENTION);
        if (na != null) s.setNotifyWhenCopilotNeedsAttention(Boolean.parseBoolean(na));

        String cc = Settings.get(KEY_CLAUDE_CLI);
        if (cc != null) s.setEnableClaudeCodeCliPreview(Boolean.parseBoolean(cc));

        String ccp = Settings.get(KEY_CLAUDE_PATH);
        if (ccp != null) s.setClaudeCodeCliPath(ccp);

        String cx = Settings.get(KEY_CODEX_CLI);
        if (cx != null) s.setEnableCodexCliPreview(Boolean.parseBoolean(cx));

        String cxp = Settings.get(KEY_CODEX_PATH);
        if (cxp != null) s.setCodexCliPath(cxp);

        // Terminal Rules
        String rc = Settings.get(KEY_RULES_COUNT);
        if (rc != null) {
            try {
                int count = Integer.parseInt(rc);
                List<TerminalAutoApproveRule> rules = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    String pat = Settings.get(KEY_RULE_PATTERN_PREFIX + i);
                    String app = Settings.get(KEY_RULE_APPROVE_PREFIX + i);
                    if (pat != null) {
                        rules.add(new TerminalAutoApproveRule(pat, Boolean.parseBoolean(app)));
                    }
                }
                s.setTerminalAutoApproveRules(rules);
            } catch (NumberFormatException ignored) {}
        }

        String uc = Settings.get(KEY_UNCOVERED_COMMANDS);
        if (uc != null) s.setAutoApproveUncoveredCommands(Boolean.parseBoolean(uc));

        // Edit Rules
        String erc = Settings.get(KEY_EDIT_RULES_COUNT);
        if (erc != null) {
            try {
                int count = Integer.parseInt(erc);
                List<FileEditAutoApproveRule> editRules = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    String pat = Settings.get(KEY_EDIT_RULE_PATTERN_PREFIX + i);
                    String desc = Settings.get(KEY_EDIT_RULE_DESC_PREFIX + i);
                    String typ = Settings.get(KEY_EDIT_RULE_TYPE_PREFIX + i);
                    String app = Settings.get(KEY_EDIT_RULE_APPROVE_PREFIX + i);
                    if (pat != null) {
                        editRules.add(new FileEditAutoApproveRule(pat, desc != null ? desc : "", typ != null ? typ : "Custom", Boolean.parseBoolean(app)));
                    }
                }
                s.setFileEditAutoApproveRules(editRules);
            } catch (NumberFormatException ignored) {}
        }

        String uce = Settings.get(KEY_UNCOVERED_EDITS);
        if (uce != null) s.setAutoApproveFileEditsNotCovered(Boolean.parseBoolean(uce));

        // MCP & Global
        String tma = Settings.get(KEY_TRUST_MCP_ANNOTATIONS);
        if (tma != null) s.setTrustMcpToolAnnotations(Boolean.parseBoolean(tma));

        String gaa = Settings.get(KEY_GLOBAL_AUTO_APPROVE);
        if (gaa != null) s.setGlobalAutoApprove(Boolean.parseBoolean(gaa));

        // Open Telemetry
        String ote = Settings.get(KEY_OTEL_ENABLE);
        if (ote != null) s.setEnableOpenTelemetryExport(Boolean.parseBoolean(ote));

        String otx = Settings.get(KEY_OTEL_EXPORTER);
        if (otx != null) s.setOpenTelemetryExporterType(otx);

        String otp = Settings.get(KEY_OTEL_PROTOCOL);
        if (otp != null) s.setOpenTelemetryProtocol(otp);

        String otep = Settings.get(KEY_OTEL_ENDPOINT);
        if (otep != null) s.setOpenTelemetryEndpoint(otep);

        String otof = Settings.get(KEY_OTEL_OUTPUT_FILE);
        if (otof != null) s.setOpenTelemetryOutputFile(otof);

        String otc = Settings.get(KEY_OTEL_CAPTURE);
        if (otc != null) s.setOpenTelemetryCaptureContent(Boolean.parseBoolean(otc));

        String otsn = Settings.get(KEY_OTEL_SERVICE);
        if (otsn != null) s.setOpenTelemetryServiceName(otsn);

        String oac = Settings.get(KEY_OTEL_ATTRS_COUNT);
        if (oac != null) {
            try {
                int count = Integer.parseInt(oac);
                List<TelemetryResourceAttribute> attrs = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    String k = Settings.get(KEY_OTEL_ATTR_KEY_PREFIX + i);
                    String v = Settings.get(KEY_OTEL_ATTR_VAL_PREFIX + i);
                    if (k != null) {
                        attrs.add(new TelemetryResourceAttribute(k, v != null ? v : ""));
                    }
                }
                s.setOpenTelemetryResourceAttributes(attrs);
            } catch (NumberFormatException ignored) {}
        }

        return s;
    }

    public void setSettings(GitHubCopilotChatSettings s) {
        if (s == null) return;
        Settings.put(KEY_AUTO_MODEL, String.valueOf(s.isEnableAutoModel()));
        Settings.put(KEY_NATURAL_LANG, s.getNaturalLanguage());
        Settings.put(KEY_DIFF_VIEW_MODE, s.getDiffViewMode());
        Settings.put(KEY_AUTO_ACCEPT_DELAY, String.valueOf(s.getAutoAcceptDelay()));
        Settings.put(KEY_INLINE_GUTTER, String.valueOf(s.isShowInlineChatGutterIcon()));
        Settings.put(KEY_SEMANTIC_SEARCH, String.valueOf(s.isEnableSemanticSearch()));

        Settings.put(KEY_AGENT_MODE, String.valueOf(s.isEnableAgentMode()));
        Settings.put(KEY_AGENT_MAX_REQ, String.valueOf(s.getAgentMaxRequests()));
        Settings.put(KEY_ANTHROPIC_TOKENS, String.valueOf(s.getAnthropicThinkingBudgetTokens()));
        Settings.put(KEY_CUSTOM_AGENT, String.valueOf(s.isEnableCustomAgent()));
        Settings.put(KEY_ORG_CUSTOM_AGENTS, String.valueOf(s.isEnableOrganizationCustomAgents()));
        Settings.put(KEY_SUBAGENT, String.valueOf(s.isEnableSubagent()));
        Settings.put(KEY_CLOUD_AGENT, String.valueOf(s.isEnableCloudAgent()));
        Settings.put(KEY_SKILLS, String.valueOf(s.isEnableSkills()));
        Settings.put(KEY_HOOKS, String.valueOf(s.isEnableHooks()));
        Settings.put(KEY_PLUGINS, String.valueOf(s.isEnablePlugins()));
        Settings.put(KEY_CODE_REVIEW, String.valueOf(s.isEnableCodeReview()));
        Settings.put(KEY_BYOK, String.valueOf(s.isEnableBringYourOwnKey()));
        Settings.put(KEY_REMOTE, String.valueOf(s.isEnableCopilotRemote()));
        Settings.put(KEY_DEBUG_LOGGING, String.valueOf(s.isEnableAgentDebugFileLogging()));
        Settings.put(KEY_NOTIFY_ATTENTION, String.valueOf(s.notifyWhenCopilotNeedsAttention()));
        Settings.put(KEY_CLAUDE_CLI, String.valueOf(s.isEnableClaudeCodeCliPreview()));
        Settings.put(KEY_CLAUDE_PATH, s.getClaudeCodeCliPath());
        Settings.put(KEY_CODEX_CLI, String.valueOf(s.isEnableCodexCliPreview()));
        Settings.put(KEY_CODEX_PATH, s.getCodexCliPath());

        // Terminal Rules
        List<TerminalAutoApproveRule> rules = s.getTerminalAutoApproveRules();
        Settings.put(KEY_RULES_COUNT, String.valueOf(rules.size()));
        for (int i = 0; i < rules.size(); i++) {
            TerminalAutoApproveRule r = rules.get(i);
            Settings.put(KEY_RULE_PATTERN_PREFIX + i, r.getPattern());
            Settings.put(KEY_RULE_APPROVE_PREFIX + i, String.valueOf(r.isAutoApprove()));
        }
        Settings.put(KEY_UNCOVERED_COMMANDS, String.valueOf(s.isAutoApproveUncoveredCommands()));

        // Edit Rules
        List<FileEditAutoApproveRule> editRules = s.getFileEditAutoApproveRules();
        Settings.put(KEY_EDIT_RULES_COUNT, String.valueOf(editRules.size()));
        for (int i = 0; i < editRules.size(); i++) {
            FileEditAutoApproveRule r = editRules.get(i);
            Settings.put(KEY_EDIT_RULE_PATTERN_PREFIX + i, r.getPattern());
            Settings.put(KEY_EDIT_RULE_DESC_PREFIX + i, r.getDescription());
            Settings.put(KEY_EDIT_RULE_TYPE_PREFIX + i, r.getType());
            Settings.put(KEY_EDIT_RULE_APPROVE_PREFIX + i, String.valueOf(r.isAutoApprove()));
        }
        Settings.put(KEY_UNCOVERED_EDITS, String.valueOf(s.isAutoApproveFileEditsNotCovered()));

        // MCP & Global
        Settings.put(KEY_TRUST_MCP_ANNOTATIONS, String.valueOf(s.isTrustMcpToolAnnotations()));
        Settings.put(KEY_GLOBAL_AUTO_APPROVE, String.valueOf(s.isGlobalAutoApprove()));

        // Open Telemetry
        Settings.put(KEY_OTEL_ENABLE, String.valueOf(s.isEnableOpenTelemetryExport()));
        Settings.put(KEY_OTEL_EXPORTER, s.getOpenTelemetryExporterType());
        Settings.put(KEY_OTEL_PROTOCOL, s.getOpenTelemetryProtocol());
        Settings.put(KEY_OTEL_ENDPOINT, s.getOpenTelemetryEndpoint());
        Settings.put(KEY_OTEL_OUTPUT_FILE, s.getOpenTelemetryOutputFile());
        Settings.put(KEY_OTEL_CAPTURE, String.valueOf(s.isOpenTelemetryCaptureContent()));
        Settings.put(KEY_OTEL_SERVICE, s.getOpenTelemetryServiceName());

        List<TelemetryResourceAttribute> attrs = s.getOpenTelemetryResourceAttributes();
        Settings.put(KEY_OTEL_ATTRS_COUNT, String.valueOf(attrs.size()));
        for (int i = 0; i < attrs.size(); i++) {
            TelemetryResourceAttribute attr = attrs.get(i);
            Settings.put(KEY_OTEL_ATTR_KEY_PREFIX + i, attr.getKey());
            Settings.put(KEY_OTEL_ATTR_VAL_PREFIX + i, attr.getValue());
        }

        notifyListeners();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    public void addListener(Runnable listener) {
        addChangeListener(listener);
    }

    public void removeListener(Runnable listener) {
        removeChangeListener(listener);
    }

    public void clear() {
        Settings.put(KEY_AUTO_MODEL, null);
        Settings.put(KEY_NATURAL_LANG, null);
        Settings.put(KEY_DIFF_VIEW_MODE, null);
        Settings.put(KEY_AUTO_ACCEPT_DELAY, null);
        Settings.put(KEY_INLINE_GUTTER, null);
        Settings.put(KEY_SEMANTIC_SEARCH, null);
        Settings.put(KEY_AGENT_MODE, null);
        Settings.put(KEY_AGENT_MAX_REQ, null);
        Settings.put(KEY_ANTHROPIC_TOKENS, null);
        Settings.put(KEY_CUSTOM_AGENT, null);
        Settings.put(KEY_ORG_CUSTOM_AGENTS, null);
        Settings.put(KEY_SUBAGENT, null);
        Settings.put(KEY_CLOUD_AGENT, null);
        Settings.put(KEY_SKILLS, null);
        Settings.put(KEY_HOOKS, null);
        Settings.put(KEY_PLUGINS, null);
        Settings.put(KEY_CODE_REVIEW, null);
        Settings.put(KEY_BYOK, null);
        Settings.put(KEY_REMOTE, null);
        Settings.put(KEY_DEBUG_LOGGING, null);
        Settings.put(KEY_NOTIFY_ATTENTION, null);
        Settings.put(KEY_CLAUDE_CLI, null);
        Settings.put(KEY_CLAUDE_PATH, null);
        Settings.put(KEY_CODEX_CLI, null);
        Settings.put(KEY_CODEX_PATH, null);
        Settings.put(KEY_RULES_COUNT, null);
        Settings.put(KEY_UNCOVERED_COMMANDS, null);
        Settings.put(KEY_EDIT_RULES_COUNT, null);
        Settings.put(KEY_UNCOVERED_EDITS, null);
        Settings.put(KEY_TRUST_MCP_ANNOTATIONS, null);
        Settings.put(KEY_GLOBAL_AUTO_APPROVE, null);
        Settings.put(KEY_OTEL_ENABLE, null);
        Settings.put(KEY_OTEL_EXPORTER, null);
        Settings.put(KEY_OTEL_PROTOCOL, null);
        Settings.put(KEY_OTEL_ENDPOINT, null);
        Settings.put(KEY_OTEL_OUTPUT_FILE, null);
        Settings.put(KEY_OTEL_CAPTURE, null);
        Settings.put(KEY_OTEL_SERVICE, null);
        Settings.put(KEY_OTEL_ATTRS_COUNT, null);
        notifyListeners();
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            try {
                listener.run();
            } catch (Throwable ignored) {
            }
        }
    }
}
