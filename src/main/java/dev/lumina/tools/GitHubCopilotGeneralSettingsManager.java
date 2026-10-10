package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manager handling persistence and change notifications for Tools > GitHub Copilot > General settings.
 */
public class GitHubCopilotGeneralSettingsManager {

    private static final GitHubCopilotGeneralSettingsManager INSTANCE = new GitHubCopilotGeneralSettingsManager();

    private static final String KEY_SCREEN_READER = "tools.copilot.general.screen_reader";
    private static final String KEY_UPDATE_CHANNEL = "tools.copilot.general.update_channel";
    private static final String KEY_CHECK_UPDATES = "tools.copilot.general.check_updates";
    private static final String KEY_PREFER_DEVICE_CODE = "tools.copilot.general.prefer_device_code";
    private static final String KEY_AUTH_PROVIDER = "tools.copilot.general.auth_provider";
    private static final String KEY_SEND_TELEMETRY = "tools.copilot.general.send_telemetry";

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private GitHubCopilotGeneralSettingsManager() {
    }

    public static GitHubCopilotGeneralSettingsManager getInstance() {
        return INSTANCE;
    }

    public GitHubCopilotGeneralSettings getSettings() {
        GitHubCopilotGeneralSettings s = new GitHubCopilotGeneralSettings();

        String sr = Settings.get(KEY_SCREEN_READER);
        if (sr != null) s.setEnableScreenReaderSupport(Boolean.parseBoolean(sr));

        String uc = Settings.get(KEY_UPDATE_CHANNEL);
        if (uc != null && !uc.isBlank()) s.setUpdateChannel(uc);

        String cu = Settings.get(KEY_CHECK_UPDATES);
        if (cu != null) s.setCheckForPluginUpdates(Boolean.parseBoolean(cu));

        String dc = Settings.get(KEY_PREFER_DEVICE_CODE);
        if (dc != null) s.setPreferDeviceCodeSignIn(Boolean.parseBoolean(dc));

        String ap = Settings.get(KEY_AUTH_PROVIDER);
        if (ap != null) s.setAuthenticationProvider(ap);

        String st = Settings.get(KEY_SEND_TELEMETRY);
        if (st != null) s.setSendUsageTelemetry(Boolean.parseBoolean(st));

        return s;
    }

    public void setSettings(GitHubCopilotGeneralSettings s) {
        if (s == null) return;
        Settings.put(KEY_SCREEN_READER, String.valueOf(s.isEnableScreenReaderSupport()));
        Settings.put(KEY_UPDATE_CHANNEL, s.getUpdateChannel());
        Settings.put(KEY_CHECK_UPDATES, String.valueOf(s.isCheckForPluginUpdates()));
        Settings.put(KEY_PREFER_DEVICE_CODE, String.valueOf(s.isPreferDeviceCodeSignIn()));
        Settings.put(KEY_AUTH_PROVIDER, s.getAuthenticationProvider());
        Settings.put(KEY_SEND_TELEMETRY, String.valueOf(s.isSendUsageTelemetry()));
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
        Settings.put(KEY_SCREEN_READER, null);
        Settings.put(KEY_UPDATE_CHANNEL, null);
        Settings.put(KEY_CHECK_UPDATES, null);
        Settings.put(KEY_PREFER_DEVICE_CODE, null);
        Settings.put(KEY_AUTH_PROVIDER, null);
        Settings.put(KEY_SEND_TELEMETRY, null);
        notifyListeners();
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
