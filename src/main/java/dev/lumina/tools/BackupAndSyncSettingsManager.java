package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton configuration manager for Backup and Sync in Lumina IDE.
 */
public class BackupAndSyncSettingsManager {

    private static final String KEY_ENABLE_BACKUP = "backup_and_sync.enable";
    private static final String KEY_ACCOUNT = "backup_and_sync.account";
    private static final String KEY_ACCOUNTS_LIST = "backup_and_sync.accounts_list";
    private static final String KEY_SYNC_UI = "backup_and_sync.sync_ui";
    private static final String KEY_SYNC_CODE = "backup_and_sync.sync_code";
    private static final String KEY_SYNC_KEYMAPS = "backup_and_sync.sync_keymaps";
    private static final String KEY_SYNC_PLUGINS = "backup_and_sync.sync_plugins";
    private static final String KEY_SYNC_TOOLS = "backup_and_sync.sync_tools";
    private static final String KEY_LAST_SYNC = "backup_and_sync.last_sync_time";

    private static final BackupAndSyncSettingsManager INSTANCE = new BackupAndSyncSettingsManager();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private BackupAndSyncSettingsManager() {
    }

    public static BackupAndSyncSettingsManager getInstance() {
        return INSTANCE;
    }

    public BackupAndSyncSettings load() {
        return getSettings();
    }

    public BackupAndSyncSettings getSettings() {
        BackupAndSyncSettings s = new BackupAndSyncSettings();

        String en = Settings.get(KEY_ENABLE_BACKUP);
        if (en != null) s.setEnableBackupAndSync(Boolean.parseBoolean(en));

        String acc = Settings.get(KEY_ACCOUNT);
        if (acc != null && !acc.isBlank()) s.setSyncAccount(acc);

        String accList = Settings.get(KEY_ACCOUNTS_LIST);
        if (accList != null && !accList.isBlank()) {
            List<String> list = new ArrayList<>(Arrays.asList(accList.split(",")));
            if (!list.isEmpty()) s.setAvailableAccounts(list);
        }

        String ui = Settings.get(KEY_SYNC_UI);
        if (ui != null) s.setSyncUi(Boolean.parseBoolean(ui));

        String code = Settings.get(KEY_SYNC_CODE);
        if (code != null) s.setSyncCodeAndSystem(Boolean.parseBoolean(code));

        String km = Settings.get(KEY_SYNC_KEYMAPS);
        if (km != null) s.setSyncKeymaps(Boolean.parseBoolean(km));

        String pl = Settings.get(KEY_SYNC_PLUGINS);
        if (pl != null) s.setSyncPlugins(Boolean.parseBoolean(pl));

        String tl = Settings.get(KEY_SYNC_TOOLS);
        if (tl != null) s.setSyncTools(Boolean.parseBoolean(tl));

        String ls = Settings.get(KEY_LAST_SYNC);
        if (ls != null) {
            try { s.setLastSyncTimeMs(Long.parseLong(ls)); } catch (NumberFormatException ignored) {}
        }

        return s;
    }

    public void setSettings(BackupAndSyncSettings s) {
        if (s == null) return;

        Settings.put(KEY_ENABLE_BACKUP, String.valueOf(s.isEnableBackupAndSync()));
        Settings.put(KEY_ACCOUNT, s.getSyncAccount());
        Settings.put(KEY_ACCOUNTS_LIST, String.join(",", s.getAvailableAccounts()));
        Settings.put(KEY_SYNC_UI, String.valueOf(s.isSyncUi()));
        Settings.put(KEY_SYNC_CODE, String.valueOf(s.isSyncCodeAndSystem()));
        Settings.put(KEY_SYNC_KEYMAPS, String.valueOf(s.isSyncKeymaps()));
        Settings.put(KEY_SYNC_PLUGINS, String.valueOf(s.isSyncPlugins()));
        Settings.put(KEY_SYNC_TOOLS, String.valueOf(s.isSyncTools()));
        Settings.put(KEY_LAST_SYNC, String.valueOf(s.getLastSyncTimeMs()));

        notifyListeners();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
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
