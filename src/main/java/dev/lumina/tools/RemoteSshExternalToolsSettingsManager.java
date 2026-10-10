package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager handling persistence and change listeners for Tools > Remote SSH External Tools settings.
 */
public class RemoteSshExternalToolsSettingsManager {

    private static final RemoteSshExternalToolsSettingsManager INSTANCE = new RemoteSshExternalToolsSettingsManager();

    private static final String KEY_PREFIX = "tools.remote_ssh_external.";
    private static final String KEY_COUNT = KEY_PREFIX + "count";
    private static final String KEY_NAME = KEY_PREFIX + "name.";
    private static final String KEY_GROUP = KEY_PREFIX + "group.";
    private static final String KEY_DESC = KEY_PREFIX + "desc.";
    private static final String KEY_PROG = KEY_PREFIX + "prog.";
    private static final String KEY_ARGS = KEY_PREFIX + "args.";
    private static final String KEY_WORKDIR = KEY_PREFIX + "workdir.";
    private static final String KEY_CONNTYPE = KEY_PREFIX + "conntype.";
    private static final String KEY_SSHCFG = KEY_PREFIX + "sshcfg.";
    private static final String KEY_SYNC = KEY_PREFIX + "sync.";
    private static final String KEY_OPENCONSOLE = KEY_PREFIX + "openconsole.";
    private static final String KEY_STDOUT = KEY_PREFIX + "stdout.";
    private static final String KEY_STDERR = KEY_PREFIX + "stderr.";
    private static final String KEY_FILTERS = KEY_PREFIX + "filters.";

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private RemoteSshExternalToolsSettingsManager() {
    }

    public static RemoteSshExternalToolsSettingsManager getInstance() {
        return INSTANCE;
    }

    public RemoteSshExternalToolsSettings load() {
        return getSettings();
    }

    public RemoteSshExternalToolsSettings getSettings() {
        RemoteSshExternalToolsSettings s = new RemoteSshExternalToolsSettings();

        String countStr = Settings.get(KEY_COUNT);
        if (countStr != null) {
            try {
                int count = Integer.parseInt(countStr);
                List<RemoteSshExternalToolEntry> list = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    RemoteSshExternalToolEntry entry = new RemoteSshExternalToolEntry();
                    String name = Settings.get(KEY_NAME + i);
                    if (name != null) entry.setName(name);

                    String group = Settings.get(KEY_GROUP + i);
                    if (group != null) entry.setGroup(group);

                    String desc = Settings.get(KEY_DESC + i);
                    if (desc != null) entry.setDescription(desc);

                    String prog = Settings.get(KEY_PROG + i);
                    if (prog != null) entry.setProgram(prog);

                    String args = Settings.get(KEY_ARGS + i);
                    if (args != null) entry.setArguments(args);

                    String wd = Settings.get(KEY_WORKDIR + i);
                    if (wd != null) entry.setWorkingDirectory(wd);

                    String ct = Settings.get(KEY_CONNTYPE + i);
                    if (ct != null) entry.setConnectionType(ct);

                    String sshcfg = Settings.get(KEY_SSHCFG + i);
                    if (sshcfg != null) entry.setSshConfiguration(sshcfg);

                    String sync = Settings.get(KEY_SYNC + i);
                    if (sync != null) entry.setSynchronizeFilesAfterExecution(Boolean.parseBoolean(sync));

                    String oc = Settings.get(KEY_OPENCONSOLE + i);
                    if (oc != null) entry.setOpenConsoleForToolOutput(Boolean.parseBoolean(oc));

                    String out = Settings.get(KEY_STDOUT + i);
                    if (out != null) entry.setMakeConsoleActiveOnStdout(Boolean.parseBoolean(out));

                    String err = Settings.get(KEY_STDERR + i);
                    if (err != null) entry.setMakeConsoleActiveOnStderr(Boolean.parseBoolean(err));

                    String f = Settings.get(KEY_FILTERS + i);
                    if (f != null) entry.setOutputFilters(f);

                    list.add(entry);
                }
                s.setTools(list);
            } catch (NumberFormatException ignored) {
            }
        }

        return s;
    }

    public void setSettings(RemoteSshExternalToolsSettings s) {
        if (s == null) return;

        List<RemoteSshExternalToolEntry> list = s.getTools();
        Settings.put(KEY_COUNT, String.valueOf(list.size()));
        for (int i = 0; i < list.size(); i++) {
            RemoteSshExternalToolEntry entry = list.get(i);
            Settings.put(KEY_NAME + i, entry.getName());
            Settings.put(KEY_GROUP + i, entry.getGroup());
            Settings.put(KEY_DESC + i, entry.getDescription());
            Settings.put(KEY_PROG + i, entry.getProgram());
            Settings.put(KEY_ARGS + i, entry.getArguments());
            Settings.put(KEY_WORKDIR + i, entry.getWorkingDirectory());
            Settings.put(KEY_CONNTYPE + i, entry.getConnectionType());
            Settings.put(KEY_SSHCFG + i, entry.getSshConfiguration());
            Settings.put(KEY_SYNC + i, String.valueOf(entry.isSynchronizeFilesAfterExecution()));
            Settings.put(KEY_OPENCONSOLE + i, String.valueOf(entry.isOpenConsoleForToolOutput()));
            Settings.put(KEY_STDOUT + i, String.valueOf(entry.isMakeConsoleActiveOnStdout()));
            Settings.put(KEY_STDERR + i, String.valueOf(entry.isMakeConsoleActiveOnStderr()));
            Settings.put(KEY_FILTERS + i, entry.getOutputFilters());
        }

        notifyListeners();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeChangeListener(Runnable listener) {
        if (listener != null) listeners.remove(listener);
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
