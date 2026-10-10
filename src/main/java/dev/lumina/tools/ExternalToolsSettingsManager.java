package dev.lumina.tools;

import dev.lumina.util.Settings;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic configuration manager for Tools > External Tools in Lumina IDE.
 */
public class ExternalToolsSettingsManager {

    private static final String KEY_PREFIX = "tools.external_tools.";
    private static final String KEY_COUNT = KEY_PREFIX + "count";
    private static final String KEY_TOOL_PREFIX = KEY_PREFIX + "tool.";

    private static volatile ExternalToolsSettingsManager instance;

    private ExternalToolsSettings currentSettings;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private ExternalToolsSettingsManager() {
        this.currentSettings = loadSettings();
    }

    public static ExternalToolsSettingsManager getInstance() {
        if (instance == null) {
            synchronized (ExternalToolsSettingsManager.class) {
                if (instance == null) {
                    instance = new ExternalToolsSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized ExternalToolsSettings getSettings() {
        if (currentSettings == null) {
            currentSettings = loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(ExternalToolsSettings newSettings) {
        if (newSettings == null) {
            this.currentSettings = new ExternalToolsSettings();
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

    private ExternalToolsSettings loadSettings() {
        ExternalToolsSettings s = new ExternalToolsSettings();

        String countStr = Settings.get(KEY_COUNT);
        if (countStr != null) {
            try {
                int count = Integer.parseInt(countStr);
                List<ExternalToolItem> list = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    String pfx = KEY_TOOL_PREFIX + i + ".";
                    String name = Settings.get(pfx + "name");
                    if (name != null) {
                        ExternalToolItem item = new ExternalToolItem();
                        item.setName(name);
                        item.setGroup(Settings.get(pfx + "group"));
                        item.setDescription(Settings.get(pfx + "desc"));
                        item.setProgram(Settings.get(pfx + "program"));
                        item.setArguments(Settings.get(pfx + "args"));
                        item.setWorkingDirectory(Settings.get(pfx + "workdir"));
                        item.setSynchronizeFiles(Boolean.parseBoolean(Settings.get(pfx + "sync")));
                        item.setOpenConsole(Boolean.parseBoolean(Settings.get(pfx + "console")));
                        item.setMakeActiveOnStdout(Boolean.parseBoolean(Settings.get(pfx + "stdout")));
                        item.setMakeActiveOnStderr(Boolean.parseBoolean(Settings.get(pfx + "stderr")));
                        item.setOutputFilters(Settings.get(pfx + "filters"));
                        list.add(item);
                    }
                }
                s.setTools(list);
            } catch (NumberFormatException ignored) {}
        }

        return s;
    }

    private void saveSettings() {
        if (currentSettings == null) return;

        List<ExternalToolItem> list = currentSettings.getTools();
        Settings.put(KEY_COUNT, String.valueOf(list.size()));
        for (int i = 0; i < list.size(); i++) {
            ExternalToolItem item = list.get(i);
            String pfx = KEY_TOOL_PREFIX + i + ".";
            Settings.put(pfx + "name", item.getName());
            Settings.put(pfx + "group", item.getGroup());
            Settings.put(pfx + "desc", item.getDescription());
            Settings.put(pfx + "program", item.getProgram());
            Settings.put(pfx + "args", item.getArguments());
            Settings.put(pfx + "workdir", item.getWorkingDirectory());
            Settings.put(pfx + "sync", String.valueOf(item.isSynchronizeFiles()));
            Settings.put(pfx + "console", String.valueOf(item.isOpenConsole()));
            Settings.put(pfx + "stdout", String.valueOf(item.isMakeActiveOnStdout()));
            Settings.put(pfx + "stderr", String.valueOf(item.isMakeActiveOnStderr()));
            Settings.put(pfx + "filters", item.getOutputFilters());
        }
    }
}
