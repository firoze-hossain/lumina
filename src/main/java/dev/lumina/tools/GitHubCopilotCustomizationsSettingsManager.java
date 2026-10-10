package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manager handling persistence and change notifications for Tools > GitHub Copilot > Customizations settings.
 */
public class GitHubCopilotCustomizationsSettingsManager {

    private static final GitHubCopilotCustomizationsSettingsManager INSTANCE = new GitHubCopilotCustomizationsSettingsManager();

    private static final String KEY_INSTR_COUNT = "tools.copilot.customizations.instr.count";
    private static final String KEY_INSTR_PATH_PREFIX = "tools.copilot.customizations.instr.path.";
    private static final String KEY_INSTR_ENABLED_PREFIX = "tools.copilot.customizations.instr.enabled.";

    private static final String KEY_USE_AGENTS_MD = "tools.copilot.customizations.use_agents_md";
    private static final String KEY_USE_NESTED_AGENTS_MD = "tools.copilot.customizations.use_nested_agents_md";
    private static final String KEY_USE_CLAUDE_MD = "tools.copilot.customizations.use_claude_md";
    private static final String KEY_USE_NESTED_CLAUDE_MD = "tools.copilot.customizations.use_nested_claude_md";

    private static final String KEY_PROMPT_COUNT = "tools.copilot.customizations.prompt.count";
    private static final String KEY_PROMPT_PATH_PREFIX = "tools.copilot.customizations.prompt.path.";
    private static final String KEY_PROMPT_ENABLED_PREFIX = "tools.copilot.customizations.prompt.enabled.";

    private static final String KEY_AGENT_COUNT = "tools.copilot.customizations.agent.count";
    private static final String KEY_AGENT_PATH_PREFIX = "tools.copilot.customizations.agent.path.";
    private static final String KEY_AGENT_ENABLED_PREFIX = "tools.copilot.customizations.agent.enabled.";

    private static final String KEY_PLUGIN_COUNT = "tools.copilot.customizations.plugin.count";
    private static final String KEY_PLUGIN_URL_PREFIX = "tools.copilot.customizations.plugin.url.";

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private GitHubCopilotCustomizationsSettingsManager() {
    }

    public static GitHubCopilotCustomizationsSettingsManager getInstance() {
        return INSTANCE;
    }

    public GitHubCopilotCustomizationsSettings getSettings() {
        GitHubCopilotCustomizationsSettings s = new GitHubCopilotCustomizationsSettings();

        // Instructions
        String instrCntStr = Settings.get(KEY_INSTR_COUNT);
        if (instrCntStr != null) {
            try {
                int count = Integer.parseInt(instrCntStr);
                List<CustomizationLocationEntry> list = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    String p = Settings.get(KEY_INSTR_PATH_PREFIX + i);
                    String en = Settings.get(KEY_INSTR_ENABLED_PREFIX + i);
                    if (p != null) {
                        list.add(new CustomizationLocationEntry(p, en != null ? Boolean.parseBoolean(en) : true));
                    }
                }
                s.setInstructionLocations(list);
            } catch (NumberFormatException ignored) {}
        }

        String uam = Settings.get(KEY_USE_AGENTS_MD);
        if (uam != null) s.setUseAgentsMd(Boolean.parseBoolean(uam));

        String unam = Settings.get(KEY_USE_NESTED_AGENTS_MD);
        if (unam != null) s.setUseNestedAgentsMd(Boolean.parseBoolean(unam));

        String ucm = Settings.get(KEY_USE_CLAUDE_MD);
        if (ucm != null) s.setUseClaudeMd(Boolean.parseBoolean(ucm));

        String uncm = Settings.get(KEY_USE_NESTED_CLAUDE_MD);
        if (uncm != null) s.setUseNestedClaudeMd(Boolean.parseBoolean(uncm));

        // Prompts
        String promptCntStr = Settings.get(KEY_PROMPT_COUNT);
        if (promptCntStr != null) {
            try {
                int count = Integer.parseInt(promptCntStr);
                List<CustomizationLocationEntry> list = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    String p = Settings.get(KEY_PROMPT_PATH_PREFIX + i);
                    String en = Settings.get(KEY_PROMPT_ENABLED_PREFIX + i);
                    if (p != null) {
                        list.add(new CustomizationLocationEntry(p, en != null ? Boolean.parseBoolean(en) : true));
                    }
                }
                s.setPromptLocations(list);
            } catch (NumberFormatException ignored) {}
        }

        // Agents
        String agentCntStr = Settings.get(KEY_AGENT_COUNT);
        if (agentCntStr != null) {
            try {
                int count = Integer.parseInt(agentCntStr);
                List<CustomizationLocationEntry> list = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    String p = Settings.get(KEY_AGENT_PATH_PREFIX + i);
                    String en = Settings.get(KEY_AGENT_ENABLED_PREFIX + i);
                    if (p != null) {
                        list.add(new CustomizationLocationEntry(p, en != null ? Boolean.parseBoolean(en) : true));
                    }
                }
                s.setAgentLocations(list);
            } catch (NumberFormatException ignored) {}
        }

        // Plugins
        String pluginCntStr = Settings.get(KEY_PLUGIN_COUNT);
        if (pluginCntStr != null) {
            try {
                int count = Integer.parseInt(pluginCntStr);
                List<String> list = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    String u = Settings.get(KEY_PLUGIN_URL_PREFIX + i);
                    if (u != null) {
                        list.add(u);
                    }
                }
                s.setPluginMarketplaces(list);
            } catch (NumberFormatException ignored) {}
        }

        return s;
    }

    public void setSettings(GitHubCopilotCustomizationsSettings settings) {
        save(settings);
    }

    public void save(GitHubCopilotCustomizationsSettings settings) {
        if (settings == null) return;

        // Clear old instructions
        String prevInstrCnt = Settings.get(KEY_INSTR_COUNT);
        if (prevInstrCnt != null) {
            try {
                int oldCnt = Integer.parseInt(prevInstrCnt);
                for (int i = 0; i < oldCnt; i++) {
                    Settings.put(KEY_INSTR_PATH_PREFIX + i, null);
                    Settings.put(KEY_INSTR_ENABLED_PREFIX + i, null);
                }
            } catch (NumberFormatException ignored) {}
        }
        List<CustomizationLocationEntry> instrs = settings.getInstructionLocations();
        Settings.put(KEY_INSTR_COUNT, String.valueOf(instrs.size()));
        for (int i = 0; i < instrs.size(); i++) {
            CustomizationLocationEntry e = instrs.get(i);
            Settings.put(KEY_INSTR_PATH_PREFIX + i, e.getPath());
            Settings.put(KEY_INSTR_ENABLED_PREFIX + i, String.valueOf(e.isEnabled()));
        }

        Settings.put(KEY_USE_AGENTS_MD, String.valueOf(settings.isUseAgentsMd()));
        Settings.put(KEY_USE_NESTED_AGENTS_MD, String.valueOf(settings.isUseNestedAgentsMd()));
        Settings.put(KEY_USE_CLAUDE_MD, String.valueOf(settings.isUseClaudeMd()));
        Settings.put(KEY_USE_NESTED_CLAUDE_MD, String.valueOf(settings.isUseNestedClaudeMd()));

        // Clear old prompts
        String prevPromptCnt = Settings.get(KEY_PROMPT_COUNT);
        if (prevPromptCnt != null) {
            try {
                int oldCnt = Integer.parseInt(prevPromptCnt);
                for (int i = 0; i < oldCnt; i++) {
                    Settings.put(KEY_PROMPT_PATH_PREFIX + i, null);
                    Settings.put(KEY_PROMPT_ENABLED_PREFIX + i, null);
                }
            } catch (NumberFormatException ignored) {}
        }
        List<CustomizationLocationEntry> prompts = settings.getPromptLocations();
        Settings.put(KEY_PROMPT_COUNT, String.valueOf(prompts.size()));
        for (int i = 0; i < prompts.size(); i++) {
            CustomizationLocationEntry e = prompts.get(i);
            Settings.put(KEY_PROMPT_PATH_PREFIX + i, e.getPath());
            Settings.put(KEY_PROMPT_ENABLED_PREFIX + i, String.valueOf(e.isEnabled()));
        }

        // Clear old agents
        String prevAgentCnt = Settings.get(KEY_AGENT_COUNT);
        if (prevAgentCnt != null) {
            try {
                int oldCnt = Integer.parseInt(prevAgentCnt);
                for (int i = 0; i < oldCnt; i++) {
                    Settings.put(KEY_AGENT_PATH_PREFIX + i, null);
                    Settings.put(KEY_AGENT_ENABLED_PREFIX + i, null);
                }
            } catch (NumberFormatException ignored) {}
        }
        List<CustomizationLocationEntry> agents = settings.getAgentLocations();
        Settings.put(KEY_AGENT_COUNT, String.valueOf(agents.size()));
        for (int i = 0; i < agents.size(); i++) {
            CustomizationLocationEntry e = agents.get(i);
            Settings.put(KEY_AGENT_PATH_PREFIX + i, e.getPath());
            Settings.put(KEY_AGENT_ENABLED_PREFIX + i, String.valueOf(e.isEnabled()));
        }

        // Clear old plugins
        String prevPluginCnt = Settings.get(KEY_PLUGIN_COUNT);
        if (prevPluginCnt != null) {
            try {
                int oldCnt = Integer.parseInt(prevPluginCnt);
                for (int i = 0; i < oldCnt; i++) {
                    Settings.put(KEY_PLUGIN_URL_PREFIX + i, null);
                }
            } catch (NumberFormatException ignored) {}
        }
        List<String> plugins = settings.getPluginMarketplaces();
        Settings.put(KEY_PLUGIN_COUNT, String.valueOf(plugins.size()));
        for (int i = 0; i < plugins.size(); i++) {
            Settings.put(KEY_PLUGIN_URL_PREFIX + i, plugins.get(i));
        }

        notifyListeners();
    }

    public void clear() {
        String prevInstrCnt = Settings.get(KEY_INSTR_COUNT);
        if (prevInstrCnt != null) {
            try {
                int oldCnt = Integer.parseInt(prevInstrCnt);
                for (int i = 0; i < oldCnt; i++) {
                    Settings.put(KEY_INSTR_PATH_PREFIX + i, null);
                    Settings.put(KEY_INSTR_ENABLED_PREFIX + i, null);
                }
            } catch (NumberFormatException ignored) {}
        }
        Settings.put(KEY_INSTR_COUNT, null);

        Settings.put(KEY_USE_AGENTS_MD, null);
        Settings.put(KEY_USE_NESTED_AGENTS_MD, null);
        Settings.put(KEY_USE_CLAUDE_MD, null);
        Settings.put(KEY_USE_NESTED_CLAUDE_MD, null);

        String prevPromptCnt = Settings.get(KEY_PROMPT_COUNT);
        if (prevPromptCnt != null) {
            try {
                int oldCnt = Integer.parseInt(prevPromptCnt);
                for (int i = 0; i < oldCnt; i++) {
                    Settings.put(KEY_PROMPT_PATH_PREFIX + i, null);
                    Settings.put(KEY_PROMPT_ENABLED_PREFIX + i, null);
                }
            } catch (NumberFormatException ignored) {}
        }
        Settings.put(KEY_PROMPT_COUNT, null);

        String prevAgentCnt = Settings.get(KEY_AGENT_COUNT);
        if (prevAgentCnt != null) {
            try {
                int oldCnt = Integer.parseInt(prevAgentCnt);
                for (int i = 0; i < oldCnt; i++) {
                    Settings.put(KEY_AGENT_PATH_PREFIX + i, null);
                    Settings.put(KEY_AGENT_ENABLED_PREFIX + i, null);
                }
            } catch (NumberFormatException ignored) {}
        }
        Settings.put(KEY_AGENT_COUNT, null);

        String prevPluginCnt = Settings.get(KEY_PLUGIN_COUNT);
        if (prevPluginCnt != null) {
            try {
                int oldCnt = Integer.parseInt(prevPluginCnt);
                for (int i = 0; i < oldCnt; i++) {
                    Settings.put(KEY_PLUGIN_URL_PREFIX + i, null);
                }
            } catch (NumberFormatException ignored) {}
        }
        Settings.put(KEY_PLUGIN_COUNT, null);

        notifyListeners();
    }

    public void addListener(Runnable listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        if (listener != null) listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable r : listeners) {
            try {
                r.run();
            } catch (Throwable ignored) {}
        }
    }
}
