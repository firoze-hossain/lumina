package dev.lumina.tools;

import dev.lumina.util.Settings;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic configuration and CLI discovery manager for Claude Code [Beta] in Lumina IDE.
 */
public class ClaudeCodeSettingsManager {

    private static final String KEY_CLAUDE_COMMAND = "tools.claude_code.command";
    private static final String KEY_CONFIG_DIR = "tools.claude_code.config_dir";
    private static final String KEY_SUPPRESS_NOT_FOUND = "tools.claude_code.suppress_not_found";
    private static final String KEY_HIDE_TOOLBAR = "tools.claude_code.hide_toolbar";
    private static final String KEY_OPTION_ENTER = "tools.claude_code.option_enter";
    private static final String KEY_AUTO_UPDATES = "tools.claude_code.auto_updates";
    private static final String KEY_ACCEPT_ALL_INTERFACES = "tools.claude_code.accept_all_interfaces";

    private static volatile ClaudeCodeSettingsManager instance;

    private ClaudeCodeSettings currentSettings;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private ClaudeCodeSettingsManager() {
        this.currentSettings = loadSettings();
    }

    public static ClaudeCodeSettingsManager getInstance() {
        if (instance == null) {
            synchronized (ClaudeCodeSettingsManager.class) {
                if (instance == null) {
                    instance = new ClaudeCodeSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized ClaudeCodeSettings getSettings() {
        if (currentSettings == null) {
            currentSettings = loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(ClaudeCodeSettings newSettings) {
        if (newSettings == null) {
            this.currentSettings = new ClaudeCodeSettings();
        } else {
            this.currentSettings = newSettings.clone();
        }
        saveSettings();
        notifyListeners();
    }

    /**
     * Dynamically discovers Claude Code executable locations on the host system.
     */
    public List<String> discoverClaudeCommands() {
        Set<String> commands = new LinkedHashSet<>();
        commands.add(ClaudeCodeSettings.DEFAULT_COMMAND);

        String home = System.getProperty("user.home", "");
        String[] candidates = {
                "/usr/local/bin/claude",
                "/opt/homebrew/bin/claude",
                home + "/.npm-global/bin/claude",
                home + "/.local/bin/claude"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists() && f.canExecute()) {
                commands.add(f.getAbsolutePath());
            }
        }

        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String dir : pathEnv.split(File.pathSeparator)) {
                File cl = new File(dir, "claude");
                if (cl.exists() && cl.canExecute()) {
                    commands.add(cl.getAbsolutePath());
                }
            }
        }

        commands.add("npx @anthropic/claude");
        return new ArrayList<>(commands);
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

    private ClaudeCodeSettings loadSettings() {
        ClaudeCodeSettings settings = new ClaudeCodeSettings();
        String cmd = Settings.get(KEY_CLAUDE_COMMAND);
        if (cmd != null && !cmd.isBlank()) {
            settings.setClaudeCommand(cmd);
        }
        String dir = Settings.get(KEY_CONFIG_DIR);
        if (dir != null && !dir.isBlank()) {
            settings.setConfigDirectory(dir);
        } else {
            String envDir = System.getenv("CLAUDE_CONFIG_DIR");
            if (envDir != null) {
                settings.setConfigDirectory(envDir);
            }
        }
        String suppress = Settings.get(KEY_SUPPRESS_NOT_FOUND);
        if (suppress != null) {
            settings.setSuppressNotificationNotFound(Boolean.parseBoolean(suppress));
        }
        String hide = Settings.get(KEY_HIDE_TOOLBAR);
        if (hide != null) {
            settings.setHideToolbarButton(Boolean.parseBoolean(hide));
        }
        String optEnter = Settings.get(KEY_OPTION_ENTER);
        if (optEnter != null) {
            settings.setOptionEnterMultiLine(Boolean.parseBoolean(optEnter));
        }
        String autoUp = Settings.get(KEY_AUTO_UPDATES);
        if (autoUp != null) {
            settings.setAutomaticUpdates(Boolean.parseBoolean(autoUp));
        }
        String allIntf = Settings.get(KEY_ACCEPT_ALL_INTERFACES);
        if (allIntf != null) {
            settings.setAcceptConnectionsAllInterfaces(Boolean.parseBoolean(allIntf));
        }
        return settings;
    }

    private void saveSettings() {
        if (currentSettings == null) return;
        Settings.put(KEY_CLAUDE_COMMAND, currentSettings.getClaudeCommand());
        Settings.put(KEY_CONFIG_DIR, currentSettings.getConfigDirectory());
        Settings.put(KEY_SUPPRESS_NOT_FOUND, String.valueOf(currentSettings.isSuppressNotificationNotFound()));
        Settings.put(KEY_HIDE_TOOLBAR, String.valueOf(currentSettings.isHideToolbarButton()));
        Settings.put(KEY_OPTION_ENTER, String.valueOf(currentSettings.isOptionEnterMultiLine()));
        Settings.put(KEY_AUTO_UPDATES, String.valueOf(currentSettings.isAutomaticUpdates()));
        Settings.put(KEY_ACCEPT_ALL_INTERFACES, String.valueOf(currentSettings.isAcceptConnectionsAllInterfaces()));
    }
}
