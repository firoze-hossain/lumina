package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manager handling persistence and change notifications for Tools > GitHub Copilot > Network settings.
 */
public class GitHubCopilotNetworkSettingsManager {

    private static final GitHubCopilotNetworkSettingsManager INSTANCE = new GitHubCopilotNetworkSettingsManager();

    private static final String KEY_CUSTOMIZE_PROXY = "tools.copilot.network.customize_proxy";
    private static final String KEY_HOST_NAME = "tools.copilot.network.host_name";
    private static final String KEY_PORT_NUMBER = "tools.copilot.network.port_number";
    private static final String KEY_PROXY_AUTH = "tools.copilot.network.proxy_auth";
    private static final String KEY_LOGIN = "tools.copilot.network.login";
    private static final String KEY_PASSWORD = "tools.copilot.network.password";
    private static final String KEY_NETWORKING = "tools.copilot.network.networking";
    private static final String KEY_KERBEROS_NAME = "tools.copilot.network.kerberos_name";

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private GitHubCopilotNetworkSettingsManager() {
    }

    public static GitHubCopilotNetworkSettingsManager getInstance() {
        return INSTANCE;
    }

    public GitHubCopilotNetworkSettings load() {
        return getSettings();
    }

    public GitHubCopilotNetworkSettings getSettings() {
        GitHubCopilotNetworkSettings s = new GitHubCopilotNetworkSettings();

        String cp = Settings.get(KEY_CUSTOMIZE_PROXY);
        if (cp != null) s.setCustomizeHttpProxy(Boolean.parseBoolean(cp));

        String hn = Settings.get(KEY_HOST_NAME);
        if (hn != null) s.setHostName(hn);

        String pn = Settings.get(KEY_PORT_NUMBER);
        if (pn != null) {
            try {
                s.setPortNumber(Integer.parseInt(pn));
            } catch (Exception ignored) {
            }
        }

        String pa = Settings.get(KEY_PROXY_AUTH);
        if (pa != null) s.setProxyAuthentication(Boolean.parseBoolean(pa));

        String lg = Settings.get(KEY_LOGIN);
        if (lg != null) s.setLogin(lg);

        String pw = Settings.get(KEY_PASSWORD);
        if (pw != null) s.setPassword(pw);

        String nw = Settings.get(KEY_NETWORKING);
        if (nw != null) s.setNetworking(nw);

        String kn = Settings.get(KEY_KERBEROS_NAME);
        if (kn != null) s.setOverrideKerberosProxyPrincipalName(kn);

        return s;
    }

    public void setSettings(GitHubCopilotNetworkSettings s) {
        if (s == null) return;
        Settings.put(KEY_CUSTOMIZE_PROXY, String.valueOf(s.isCustomizeHttpProxy()));
        Settings.put(KEY_HOST_NAME, s.getHostName());
        Settings.put(KEY_PORT_NUMBER, String.valueOf(s.getPortNumber()));
        Settings.put(KEY_PROXY_AUTH, String.valueOf(s.isProxyAuthentication()));
        Settings.put(KEY_LOGIN, s.getLogin());
        Settings.put(KEY_PASSWORD, s.getPassword());
        Settings.put(KEY_NETWORKING, s.getNetworking());
        Settings.put(KEY_KERBEROS_NAME, s.getOverrideKerberosProxyPrincipalName());
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
        Settings.put(KEY_CUSTOMIZE_PROXY, null);
        Settings.put(KEY_HOST_NAME, null);
        Settings.put(KEY_PORT_NUMBER, null);
        Settings.put(KEY_PROXY_AUTH, null);
        Settings.put(KEY_LOGIN, null);
        Settings.put(KEY_PASSWORD, null);
        Settings.put(KEY_NETWORKING, null);
        Settings.put(KEY_KERBEROS_NAME, null);
        notifyListeners();
    }

    public void resetDefaults() {
        setSettings(new GitHubCopilotNetworkSettings());
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
