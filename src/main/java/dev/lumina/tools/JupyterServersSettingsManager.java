package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manager handling persistence and change notifications for Tools > Jupyter > Jupyter Servers settings.
 */
public class JupyterServersSettingsManager {

    private static final JupyterServersSettingsManager INSTANCE = new JupyterServersSettingsManager();

    private static final String KEY_PREFIX = "tools.jupyter.servers.";
    private static final String KEY_COUNT = KEY_PREFIX + "count";
    private static final String KEY_SELECTED = KEY_PREFIX + "selected_id";
    private static final String KEY_SERVER_ID = KEY_PREFIX + "id.";
    private static final String KEY_SERVER_NAME = KEY_PREFIX + "name.";
    private static final String KEY_SERVER_TYPE = KEY_PREFIX + "type.";
    private static final String KEY_SERVER_AUTO = KEY_PREFIX + "auto.";
    private static final String KEY_SERVER_URL = KEY_PREFIX + "url.";
    private static final String KEY_SERVER_TOKEN = KEY_PREFIX + "token.";

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private JupyterServersSettingsManager() {
    }

    public static JupyterServersSettingsManager getInstance() {
        return INSTANCE;
    }

    public JupyterServersSettings load() {
        return getSettings();
    }

    public JupyterServersSettings getSettings() {
        JupyterServersSettings s = new JupyterServersSettings();

        String countStr = Settings.get(KEY_COUNT);
        if (countStr != null) {
            try {
                int count = Integer.parseInt(countStr);
                List<JupyterServerConfig> list = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    String id = Settings.get(KEY_SERVER_ID + i);
                    String name = Settings.get(KEY_SERVER_NAME + i);
                    String type = Settings.get(KEY_SERVER_TYPE + i);
                    boolean auto = Boolean.parseBoolean(Settings.get(KEY_SERVER_AUTO + i));
                    String url = Settings.get(KEY_SERVER_URL + i);
                    String token = Settings.get(KEY_SERVER_TOKEN + i);
                    list.add(new JupyterServerConfig(id, name, type, auto, url, token));
                }
                if (!list.isEmpty()) {
                    s.setServers(list);
                }
            } catch (Exception ignored) {
            }
        }

        String sel = Settings.get(KEY_SELECTED);
        if (sel != null && !sel.isEmpty()) {
            s.setSelectedServerId(sel);
        }

        return s;
    }

    public void setSettings(JupyterServersSettings s) {
        if (s == null) return;

        List<JupyterServerConfig> list = s.getServers();
        Settings.put(KEY_COUNT, String.valueOf(list.size()));
        Settings.put(KEY_SELECTED, s.getSelectedServerId());

        for (int i = 0; i < list.size(); i++) {
            JupyterServerConfig cfg = list.get(i);
            Settings.put(KEY_SERVER_ID + i, cfg.getId());
            Settings.put(KEY_SERVER_NAME + i, cfg.getName());
            Settings.put(KEY_SERVER_TYPE + i, cfg.getServerType());
            Settings.put(KEY_SERVER_AUTO + i, String.valueOf(cfg.isAutodetectedExecutionMode()));
            Settings.put(KEY_SERVER_URL + i, cfg.getUrl());
            Settings.put(KEY_SERVER_TOKEN + i, cfg.getToken());
        }

        notifyListeners();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    public void clear() {
        String countStr = Settings.get(KEY_COUNT);
        if (countStr != null) {
            try {
                int count = Integer.parseInt(countStr);
                for (int i = 0; i < count; i++) {
                    Settings.put(KEY_SERVER_ID + i, null);
                    Settings.put(KEY_SERVER_NAME + i, null);
                    Settings.put(KEY_SERVER_TYPE + i, null);
                    Settings.put(KEY_SERVER_AUTO + i, null);
                    Settings.put(KEY_SERVER_URL + i, null);
                    Settings.put(KEY_SERVER_TOKEN + i, null);
                }
            } catch (Exception ignored) {
            }
            Settings.put(KEY_COUNT, null);
        }
        Settings.put(KEY_SELECTED, null);

        notifyListeners();
    }

    public void resetDefaults() {
        setSettings(new JupyterServersSettings());
    }

    private void notifyListeners() {
        for (Runnable l : listeners) {
            try {
                l.run();
            } catch (Throwable ignored) {
            }
        }
    }
}
