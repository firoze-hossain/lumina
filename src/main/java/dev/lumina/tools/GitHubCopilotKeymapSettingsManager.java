package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manager handling persistence and change notifications for Tools > GitHub Copilot > Keymap settings.
 */
public class GitHubCopilotKeymapSettingsManager {

    private static final GitHubCopilotKeymapSettingsManager INSTANCE = new GitHubCopilotKeymapSettingsManager();

    private static final String KEY_COUNT = "tools.copilot.keymap.count";
    private static final String KEY_ACTION_PREFIX = "tools.copilot.keymap.action.";
    private static final String KEY_SHORTCUT_PREFIX = "tools.copilot.keymap.shortcut.";

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private GitHubCopilotKeymapSettingsManager() {
    }

    public static GitHubCopilotKeymapSettingsManager getInstance() {
        return INSTANCE;
    }

    public GitHubCopilotKeymapSettings load() {
        return getSettings();
    }

    public GitHubCopilotKeymapSettings getSettings() {
        String cntStr = Settings.get(KEY_COUNT);
        if (cntStr == null) {
            return new GitHubCopilotKeymapSettings();
        }

        try {
            int count = Integer.parseInt(cntStr);
            List<GitHubCopilotKeymapEntry> list = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String act = Settings.get(KEY_ACTION_PREFIX + i);
                String sc = Settings.get(KEY_SHORTCUT_PREFIX + i);
                if (act != null) {
                    list.add(new GitHubCopilotKeymapEntry(act, sc));
                }
            }
            if (list.isEmpty()) {
                return new GitHubCopilotKeymapSettings();
            }
            return new GitHubCopilotKeymapSettings(list);
        } catch (NumberFormatException e) {
            return new GitHubCopilotKeymapSettings();
        }
    }

    public void setSettings(GitHubCopilotKeymapSettings s) {
        if (s == null) return;
        List<GitHubCopilotKeymapEntry> entries = s.getEntries();
        Settings.put(KEY_COUNT, String.valueOf(entries.size()));
        for (int i = 0; i < entries.size(); i++) {
            GitHubCopilotKeymapEntry e = entries.get(i);
            Settings.put(KEY_ACTION_PREFIX + i, e.getAction());
            Settings.put(KEY_SHORTCUT_PREFIX + i, e.getKeymap());
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
        String cntStr = Settings.get(KEY_COUNT);
        if (cntStr != null) {
            try {
                int count = Integer.parseInt(cntStr);
                for (int i = 0; i < count; i++) {
                    Settings.put(KEY_ACTION_PREFIX + i, null);
                    Settings.put(KEY_SHORTCUT_PREFIX + i, null);
                }
            } catch (Exception ignored) {
            }
            Settings.put(KEY_COUNT, null);
        }
        notifyListeners();
    }

    public void resetDefaults() {
        setSettings(new GitHubCopilotKeymapSettings());
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
