package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton configuration manager for Tools > Rsync settings in Lumina IDE.
 */
public class RsyncSettingsManager {

    private static final String KEY_RSYNC_EXEC_PATH = "tools.rsync.executable_path";
    private static final String KEY_RSYNC_OPTIONS = "tools.rsync.options";
    private static final String KEY_SHELL_EXEC_PATH = "tools.rsync.shell_executable_path";

    private static final RsyncSettingsManager INSTANCE = new RsyncSettingsManager();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private RsyncSettingsManager() {
    }

    public static RsyncSettingsManager getInstance() {
        return INSTANCE;
    }

    public RsyncSettings load() {
        return getSettings();
    }

    public RsyncSettings getSettings() {
        RsyncSettings s = new RsyncSettings();

        String rsyncExec = Settings.get(KEY_RSYNC_EXEC_PATH);
        if (rsyncExec != null) s.setRsyncExecutablePath(rsyncExec);

        String options = Settings.get(KEY_RSYNC_OPTIONS);
        if (options != null) s.setRsyncOptions(options);

        String shellExec = Settings.get(KEY_SHELL_EXEC_PATH);
        if (shellExec != null) s.setShellExecutablePath(shellExec);

        return s;
    }

    public void setSettings(RsyncSettings s) {
        if (s == null) return;

        Settings.put(KEY_RSYNC_EXEC_PATH, s.getRsyncExecutablePath());
        Settings.put(KEY_RSYNC_OPTIONS, s.getRsyncOptions());
        Settings.put(KEY_SHELL_EXEC_PATH, s.getShellExecutablePath());

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
