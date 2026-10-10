package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton configuration manager for Tools > SSH Terminal settings in Lumina IDE.
 */
public class SshTerminalSettingsManager {

    private static final String KEY_CONN_MODE = "tools.ssh_terminal.connection_mode";
    private static final String KEY_SSH_CONFIG = "tools.ssh_terminal.ssh_configuration";
    private static final String KEY_DEFAULT_ENCODING = "tools.ssh_terminal.default_encoding";

    private static final SshTerminalSettingsManager INSTANCE = new SshTerminalSettingsManager();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private SshTerminalSettingsManager() {
    }

    public static SshTerminalSettingsManager getInstance() {
        return INSTANCE;
    }

    public SshTerminalSettings load() {
        return getSettings();
    }

    public SshTerminalSettings getSettings() {
        SshTerminalSettings s = new SshTerminalSettings();

        String mode = Settings.get(KEY_CONN_MODE);
        if (mode != null) s.setConnectionMode(mode);

        String config = Settings.get(KEY_SSH_CONFIG);
        if (config != null) s.setSshConfiguration(config);

        String enc = Settings.get(KEY_DEFAULT_ENCODING);
        if (enc != null) s.setDefaultEncoding(enc);

        return s;
    }

    public void setSettings(SshTerminalSettings s) {
        if (s == null) return;

        Settings.put(KEY_CONN_MODE, s.getConnectionMode());
        Settings.put(KEY_SSH_CONFIG, s.getSshConfiguration());
        Settings.put(KEY_DEFAULT_ENCODING, s.getDefaultEncoding());

        notifyListeners();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable r : listeners) {
            try {
                r.run();
            } catch (Throwable ignored) {
            }
        }
    }
}
