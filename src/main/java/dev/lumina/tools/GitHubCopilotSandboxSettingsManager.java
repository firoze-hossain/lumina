package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manager handling persistence and change notifications for Tools > GitHub Copilot > Sandbox settings.
 */
public class GitHubCopilotSandboxSettingsManager {

    private static final GitHubCopilotSandboxSettingsManager INSTANCE = new GitHubCopilotSandboxSettingsManager();

    private static final String KEY_ENABLED = "tools.copilot.sandbox.enabled";
    private static final String KEY_FS_INCLUDE_WORK_DIR = "tools.copilot.sandbox.fs_include_work_dir";
    private static final String KEY_FS_CLEAR_ON_EXIT = "tools.copilot.sandbox.fs_clear_on_exit";
    private static final String KEY_FS_PERMS_COUNT = "tools.copilot.sandbox.fs_perms.count";
    private static final String KEY_FS_PERM_PATH_PREFIX = "tools.copilot.sandbox.fs_perm.path.";
    private static final String KEY_FS_PERM_VAL_PREFIX = "tools.copilot.sandbox.fs_perm.val.";

    private static final String KEY_NET_ALLOW_OUTBOUND = "tools.copilot.sandbox.net_allow_outbound";
    private static final String KEY_NET_ALLOW_LOCAL = "tools.copilot.sandbox.net_allow_local";
    private static final String KEY_NET_HOSTS_COUNT = "tools.copilot.sandbox.net_hosts.count";
    private static final String KEY_NET_HOST_NAME_PREFIX = "tools.copilot.sandbox.net_host.name.";
    private static final String KEY_NET_HOST_ACCESS_PREFIX = "tools.copilot.sandbox.net_host.access.";

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private GitHubCopilotSandboxSettingsManager() {
    }

    public static GitHubCopilotSandboxSettingsManager getInstance() {
        return INSTANCE;
    }

    public GitHubCopilotSandboxSettings getSettings() {
        GitHubCopilotSandboxSettings s = new GitHubCopilotSandboxSettings();

        String en = Settings.get(KEY_ENABLED);
        if (en != null) s.setEnableLocalSandbox(Boolean.parseBoolean(en));

        String fswd = Settings.get(KEY_FS_INCLUDE_WORK_DIR);
        if (fswd != null) s.setFilesystemIncludeWorkingDirectory(Boolean.parseBoolean(fswd));

        String fsce = Settings.get(KEY_FS_CLEAR_ON_EXIT);
        if (fsce != null) s.setFilesystemClearPolicyOnExit(Boolean.parseBoolean(fsce));

        String fsCntStr = Settings.get(KEY_FS_PERMS_COUNT);
        if (fsCntStr != null) {
            try {
                int count = Integer.parseInt(fsCntStr);
                List<SandboxPathPermission> list = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    String p = Settings.get(KEY_FS_PERM_PATH_PREFIX + i);
                    String val = Settings.get(KEY_FS_PERM_VAL_PREFIX + i);
                    if (p != null) {
                        list.add(new SandboxPathPermission(p, val != null ? val : "Read"));
                    }
                }
                s.setFilesystemPermissions(list);
            } catch (NumberFormatException ignored) {}
        }

        String netOut = Settings.get(KEY_NET_ALLOW_OUTBOUND);
        if (netOut != null) s.setNetworkAllowOutbound(Boolean.parseBoolean(netOut));

        String netLoc = Settings.get(KEY_NET_ALLOW_LOCAL);
        if (netLoc != null) s.setNetworkAllowLocalNetwork(Boolean.parseBoolean(netLoc));

        String netCntStr = Settings.get(KEY_NET_HOSTS_COUNT);
        if (netCntStr != null) {
            try {
                int count = Integer.parseInt(netCntStr);
                List<SandboxHostAccess> list = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    String h = Settings.get(KEY_NET_HOST_NAME_PREFIX + i);
                    String acc = Settings.get(KEY_NET_HOST_ACCESS_PREFIX + i);
                    if (h != null) {
                        list.add(new SandboxHostAccess(h, acc != null ? acc : "Allow"));
                    }
                }
                s.setNetworkHostAccess(list);
            } catch (NumberFormatException ignored) {}
        }

        return s;
    }

    public void setSettings(GitHubCopilotSandboxSettings settings) {
        save(settings);
    }

    public void save(GitHubCopilotSandboxSettings settings) {
        if (settings == null) return;

        Settings.put(KEY_ENABLED, String.valueOf(settings.isEnableLocalSandbox()));
        Settings.put(KEY_FS_INCLUDE_WORK_DIR, String.valueOf(settings.isFilesystemIncludeWorkingDirectory()));
        Settings.put(KEY_FS_CLEAR_ON_EXIT, String.valueOf(settings.isFilesystemClearPolicyOnExit()));

        // Save fs permissions
        String prevFsCnt = Settings.get(KEY_FS_PERMS_COUNT);
        if (prevFsCnt != null) {
            try {
                int oldCnt = Integer.parseInt(prevFsCnt);
                for (int i = 0; i < oldCnt; i++) {
                    Settings.put(KEY_FS_PERM_PATH_PREFIX + i, null);
                    Settings.put(KEY_FS_PERM_VAL_PREFIX + i, null);
                }
            } catch (NumberFormatException ignored) {}
        }
        List<SandboxPathPermission> perms = settings.getFilesystemPermissions();
        Settings.put(KEY_FS_PERMS_COUNT, String.valueOf(perms.size()));
        for (int i = 0; i < perms.size(); i++) {
            SandboxPathPermission p = perms.get(i);
            Settings.put(KEY_FS_PERM_PATH_PREFIX + i, p.getPath());
            Settings.put(KEY_FS_PERM_VAL_PREFIX + i, p.getPermission());
        }

        Settings.put(KEY_NET_ALLOW_OUTBOUND, String.valueOf(settings.isNetworkAllowOutbound()));
        Settings.put(KEY_NET_ALLOW_LOCAL, String.valueOf(settings.isNetworkAllowLocalNetwork()));

        // Save net hosts
        String prevNetCnt = Settings.get(KEY_NET_HOSTS_COUNT);
        if (prevNetCnt != null) {
            try {
                int oldCnt = Integer.parseInt(prevNetCnt);
                for (int i = 0; i < oldCnt; i++) {
                    Settings.put(KEY_NET_HOST_NAME_PREFIX + i, null);
                    Settings.put(KEY_NET_HOST_ACCESS_PREFIX + i, null);
                }
            } catch (NumberFormatException ignored) {}
        }
        List<SandboxHostAccess> hosts = settings.getNetworkHostAccess();
        Settings.put(KEY_NET_HOSTS_COUNT, String.valueOf(hosts.size()));
        for (int i = 0; i < hosts.size(); i++) {
            SandboxHostAccess h = hosts.get(i);
            Settings.put(KEY_NET_HOST_NAME_PREFIX + i, h.getHost());
            Settings.put(KEY_NET_HOST_ACCESS_PREFIX + i, h.getAccess());
        }

        notifyListeners();
    }

    public void clear() {
        Settings.put(KEY_ENABLED, null);
        Settings.put(KEY_FS_INCLUDE_WORK_DIR, null);
        Settings.put(KEY_FS_CLEAR_ON_EXIT, null);

        String prevFsCnt = Settings.get(KEY_FS_PERMS_COUNT);
        if (prevFsCnt != null) {
            try {
                int oldCnt = Integer.parseInt(prevFsCnt);
                for (int i = 0; i < oldCnt; i++) {
                    Settings.put(KEY_FS_PERM_PATH_PREFIX + i, null);
                    Settings.put(KEY_FS_PERM_VAL_PREFIX + i, null);
                }
            } catch (NumberFormatException ignored) {}
        }
        Settings.put(KEY_FS_PERMS_COUNT, null);

        Settings.put(KEY_NET_ALLOW_OUTBOUND, null);
        Settings.put(KEY_NET_ALLOW_LOCAL, null);

        String prevNetCnt = Settings.get(KEY_NET_HOSTS_COUNT);
        if (prevNetCnt != null) {
            try {
                int oldCnt = Integer.parseInt(prevNetCnt);
                for (int i = 0; i < oldCnt; i++) {
                    Settings.put(KEY_NET_HOST_NAME_PREFIX + i, null);
                    Settings.put(KEY_NET_HOST_ACCESS_PREFIX + i, null);
                }
            } catch (NumberFormatException ignored) {}
        }
        Settings.put(KEY_NET_HOSTS_COUNT, null);

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
