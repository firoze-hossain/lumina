package dev.lumina.tools;

import dev.lumina.util.Settings;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic configuration manager for Tools > Diff & Merge > External Diff Tools in Lumina IDE.
 */
public class ExternalDiffToolsSettingsManager {

    private static final String KEY_PREFIX = "tools.diffmerge.external.";
    private static final String KEY_ENABLE = KEY_PREFIX + "enable";
    private static final String KEY_TOOLS_COUNT = KEY_PREFIX + "tools.count";
    private static final String KEY_TOOL_PREFIX = KEY_PREFIX + "tool.";
    private static final String KEY_ASSOC_COUNT = KEY_PREFIX + "assoc.count";
    private static final String KEY_ASSOC_PREFIX = KEY_PREFIX + "assoc.";

    private static volatile ExternalDiffToolsSettingsManager instance;

    private ExternalDiffToolsSettings currentSettings;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private ExternalDiffToolsSettingsManager() {
        this.currentSettings = loadSettings();
    }

    public static ExternalDiffToolsSettingsManager getInstance() {
        if (instance == null) {
            synchronized (ExternalDiffToolsSettingsManager.class) {
                if (instance == null) {
                    instance = new ExternalDiffToolsSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized ExternalDiffToolsSettings getSettings() {
        if (currentSettings == null) {
            currentSettings = loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(ExternalDiffToolsSettings newSettings) {
        if (newSettings == null) {
            this.currentSettings = new ExternalDiffToolsSettings();
        } else {
            this.currentSettings = newSettings.clone();
        }
        saveSettings();
        notifyListeners();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            try {
                listener.run();
            } catch (Exception ignored) {
            }
        }
    }

    private ExternalDiffToolsSettings loadSettings() {
        ExternalDiffToolsSettings s = new ExternalDiffToolsSettings();

        String en = Settings.get(KEY_ENABLE);
        if (en != null) s.setEnableExternalTools(Boolean.parseBoolean(en));

        String toolsCountStr = Settings.get(KEY_TOOLS_COUNT);
        if (toolsCountStr != null) {
            try {
                int count = Integer.parseInt(toolsCountStr);
                List<ExternalDiffToolDefinition> list = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    String name = Settings.get(KEY_TOOL_PREFIX + i + ".name");
                    String path = Settings.get(KEY_TOOL_PREFIX + i + ".path");
                    String args = Settings.get(KEY_TOOL_PREFIX + i + ".args");
                    if (name != null) {
                        list.add(new ExternalDiffToolDefinition(name, path, args));
                    }
                }
                s.setConfiguredTools(list);
            } catch (NumberFormatException ignored) {}
        }

        String assocCountStr = Settings.get(KEY_ASSOC_COUNT);
        if (assocCountStr != null) {
            try {
                int count = Integer.parseInt(assocCountStr);
                List<ExternalDiffToolAssociation> list = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    String fType = Settings.get(KEY_ASSOC_PREFIX + i + ".ftype");
                    String diff = Settings.get(KEY_ASSOC_PREFIX + i + ".diff");
                    String merge = Settings.get(KEY_ASSOC_PREFIX + i + ".merge");
                    if (fType != null) {
                        list.add(new ExternalDiffToolAssociation(fType, diff, merge));
                    }
                }
                if (!list.isEmpty()) {
                    s.setAssociations(list);
                }
            } catch (NumberFormatException ignored) {}
        }

        return s;
    }

    private void saveSettings() {
        if (currentSettings == null) return;

        Settings.put(KEY_ENABLE, String.valueOf(currentSettings.isEnableExternalTools()));

        List<ExternalDiffToolDefinition> tools = currentSettings.getConfiguredTools();
        Settings.put(KEY_TOOLS_COUNT, String.valueOf(tools.size()));
        for (int i = 0; i < tools.size(); i++) {
            ExternalDiffToolDefinition t = tools.get(i);
            Settings.put(KEY_TOOL_PREFIX + i + ".name", t.getName());
            Settings.put(KEY_TOOL_PREFIX + i + ".path", t.getProgramPath());
            Settings.put(KEY_TOOL_PREFIX + i + ".args", t.getArguments());
        }

        List<ExternalDiffToolAssociation> assocs = currentSettings.getAssociations();
        Settings.put(KEY_ASSOC_COUNT, String.valueOf(assocs.size()));
        for (int i = 0; i < assocs.size(); i++) {
            ExternalDiffToolAssociation a = assocs.get(i);
            Settings.put(KEY_ASSOC_PREFIX + i + ".ftype", a.getFileType());
            Settings.put(KEY_ASSOC_PREFIX + i + ".diff", a.getDiffTool());
            Settings.put(KEY_ASSOC_PREFIX + i + ".merge", a.getMergeTool());
        }
    }
}
