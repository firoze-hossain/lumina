package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton configuration manager for Tools > SSH Configurations in Lumina IDE.
 */
public class SshConfigurationsSettingsManager {

    private static final String KEY_PREFIX = "tools.ssh_configurations.";
    private static final String KEY_COUNT = KEY_PREFIX + "count";
    private static final String KEY_ID = KEY_PREFIX + "id.";
    private static final String KEY_NAME = KEY_PREFIX + "name.";
    private static final String KEY_VISIBLE_PROJECT = KEY_PREFIX + "visible_project.";
    private static final String KEY_HOST = KEY_PREFIX + "host.";
    private static final String KEY_PORT = KEY_PREFIX + "port.";
    private static final String KEY_USERNAME = KEY_PREFIX + "username.";
    private static final String KEY_AUTH_TYPE = KEY_PREFIX + "auth_type.";
    private static final String KEY_PASSWORD = KEY_PREFIX + "password.";
    private static final String KEY_SAVE_PASSWORD = KEY_PREFIX + "save_password.";
    private static final String KEY_KEY_PATH = KEY_PREFIX + "key_path.";
    private static final String KEY_PASSPHRASE = KEY_PREFIX + "passphrase.";
    private static final String KEY_PARSE_CONFIG = KEY_PREFIX + "parse_config.";
    private static final String KEY_SEND_KEEPALIVE = KEY_PREFIX + "send_keepalive.";
    private static final String KEY_KEEPALIVE_INTERVAL = KEY_PREFIX + "keepalive_interval.";
    private static final String KEY_STRICT_HOST_KEY = KEY_PREFIX + "strict_host_key.";
    private static final String KEY_HASH_HOSTS = KEY_PREFIX + "hash_hosts.";
    private static final String KEY_USE_GLOBAL_PROXY = KEY_PREFIX + "use_global_proxy.";
    private static final String KEY_PROXY_TYPE = KEY_PREFIX + "proxy_type.";
    private static final String KEY_PROXY_HOST = KEY_PREFIX + "proxy_host.";
    private static final String KEY_PROXY_PORT = KEY_PREFIX + "proxy_port.";
    private static final String KEY_PROXY_AUTH = KEY_PREFIX + "proxy_auth.";
    private static final String KEY_PROXY_USER = KEY_PREFIX + "proxy_user.";
    private static final String KEY_PROXY_PASSWORD = KEY_PREFIX + "proxy_password.";

    private static SshConfigurationsSettingsManager instance;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private SshConfigurationsSettings cachedSettings;

    private SshConfigurationsSettingsManager() {
    }

    public static synchronized SshConfigurationsSettingsManager getInstance() {
        if (instance == null) {
            instance = new SshConfigurationsSettingsManager();
        }
        return instance;
    }

    public synchronized SshConfigurationsSettings getSettings() {
        if (cachedSettings != null) {
            return cachedSettings.clone();
        }

        SshConfigurationsSettings s = new SshConfigurationsSettings();
        String countStr = Settings.get(KEY_COUNT);
        if (countStr != null) {
            try {
                int count = Integer.parseInt(countStr);
                List<SshConfigurationEntry> list = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    SshConfigurationEntry entry = new SshConfigurationEntry();
                    String id = Settings.get(KEY_ID + i);
                    if (id != null) entry.setId(id);
                    String name = Settings.get(KEY_NAME + i);
                    if (name != null) entry.setName(name);
                    String vis = Settings.get(KEY_VISIBLE_PROJECT + i);
                    if (vis != null) entry.setVisibleOnlyForThisProject(Boolean.parseBoolean(vis));
                    String host = Settings.get(KEY_HOST + i);
                    if (host != null) entry.setHost(host);
                    String portStr = Settings.get(KEY_PORT + i);
                    if (portStr != null) {
                        try { entry.setPort(Integer.parseInt(portStr)); } catch (NumberFormatException ignored) {}
                    }
                    String user = Settings.get(KEY_USERNAME + i);
                    if (user != null) entry.setUsername(user);
                    String auth = Settings.get(KEY_AUTH_TYPE + i);
                    if (auth != null) entry.setAuthType(auth);
                    String pwd = Settings.get(KEY_PASSWORD + i);
                    if (pwd != null) entry.setPassword(pwd);
                    String savePwd = Settings.get(KEY_SAVE_PASSWORD + i);
                    if (savePwd != null) entry.setSavePassword(Boolean.parseBoolean(savePwd));
                    String keyPath = Settings.get(KEY_KEY_PATH + i);
                    if (keyPath != null) entry.setPrivateKeyPath(keyPath);
                    String passphrase = Settings.get(KEY_PASSPHRASE + i);
                    if (passphrase != null) entry.setPassphrase(passphrase);
                    String parseCfg = Settings.get(KEY_PARSE_CONFIG + i);
                    if (parseCfg != null) entry.setParseConfigFile(Boolean.parseBoolean(parseCfg));
                    String sendKa = Settings.get(KEY_SEND_KEEPALIVE + i);
                    if (sendKa != null) entry.setSendKeepAlive(Boolean.parseBoolean(sendKa));
                    String kaInt = Settings.get(KEY_KEEPALIVE_INTERVAL + i);
                    if (kaInt != null) {
                        try { entry.setKeepAliveIntervalSeconds(Integer.parseInt(kaInt)); } catch (NumberFormatException ignored) {}
                    }
                    String strictHk = Settings.get(KEY_STRICT_HOST_KEY + i);
                    if (strictHk != null) entry.setStrictHostKeyChecking(strictHk);
                    String hashHosts = Settings.get(KEY_HASH_HOSTS + i);
                    if (hashHosts != null) entry.setHashHosts(Boolean.parseBoolean(hashHosts));
                    String useProxy = Settings.get(KEY_USE_GLOBAL_PROXY + i);
                    if (useProxy != null) entry.setUseGlobalProxy(Boolean.parseBoolean(useProxy));
                    String proxyType = Settings.get(KEY_PROXY_TYPE + i);
                    if (proxyType != null) entry.setProxyType(proxyType);
                    String proxyHost = Settings.get(KEY_PROXY_HOST + i);
                    if (proxyHost != null) entry.setProxyHost(proxyHost);
                    String proxyPortStr = Settings.get(KEY_PROXY_PORT + i);
                    if (proxyPortStr != null) {
                        try { entry.setProxyPort(Integer.parseInt(proxyPortStr)); } catch (NumberFormatException ignored) {}
                    }
                    String proxyAuth = Settings.get(KEY_PROXY_AUTH + i);
                    if (proxyAuth != null) entry.setProxyAuthType(proxyAuth);
                    String proxyUser = Settings.get(KEY_PROXY_USER + i);
                    if (proxyUser != null) entry.setProxyUser(proxyUser);
                    String proxyPwd = Settings.get(KEY_PROXY_PASSWORD + i);
                    if (proxyPwd != null) entry.setProxyPassword(proxyPwd);

                    list.add(entry);
                }
                s.setConfigurations(list);
            } catch (NumberFormatException ignored) {
            }
        }

        cachedSettings = s.clone();
        return s;
    }

    public synchronized void setSettings(SshConfigurationsSettings s) {
        if (s == null) return;
        cachedSettings = s.clone();

        List<SshConfigurationEntry> list = s.getConfigurations();
        Settings.put(KEY_COUNT, String.valueOf(list.size()));
        for (int i = 0; i < list.size(); i++) {
            SshConfigurationEntry e = list.get(i);
            Settings.put(KEY_ID + i, e.getId());
            Settings.put(KEY_NAME + i, e.getName());
            Settings.put(KEY_VISIBLE_PROJECT + i, String.valueOf(e.isVisibleOnlyForThisProject()));
            Settings.put(KEY_HOST + i, e.getHost());
            Settings.put(KEY_PORT + i, String.valueOf(e.getPort()));
            Settings.put(KEY_USERNAME + i, e.getUsername());
            Settings.put(KEY_AUTH_TYPE + i, e.getAuthType());
            Settings.put(KEY_PASSWORD + i, e.getPassword());
            Settings.put(KEY_SAVE_PASSWORD + i, String.valueOf(e.isSavePassword()));
            Settings.put(KEY_KEY_PATH + i, e.getPrivateKeyPath());
            Settings.put(KEY_PASSPHRASE + i, e.getPassphrase());
            Settings.put(KEY_PARSE_CONFIG + i, String.valueOf(e.isParseConfigFile()));
            Settings.put(KEY_SEND_KEEPALIVE + i, String.valueOf(e.isSendKeepAlive()));
            Settings.put(KEY_KEEPALIVE_INTERVAL + i, String.valueOf(e.getKeepAliveIntervalSeconds()));
            Settings.put(KEY_STRICT_HOST_KEY + i, e.getStrictHostKeyChecking());
            Settings.put(KEY_HASH_HOSTS + i, String.valueOf(e.isHashHosts()));
            Settings.put(KEY_USE_GLOBAL_PROXY + i, String.valueOf(e.isUseGlobalProxy()));
            Settings.put(KEY_PROXY_TYPE + i, e.getProxyType());
            Settings.put(KEY_PROXY_HOST + i, e.getProxyHost());
            Settings.put(KEY_PROXY_PORT + i, String.valueOf(e.getProxyPort()));
            Settings.put(KEY_PROXY_AUTH + i, e.getProxyAuthType());
            Settings.put(KEY_PROXY_USER + i, e.getProxyUser());
            Settings.put(KEY_PROXY_PASSWORD + i, e.getProxyPassword());
        }

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
