package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton configuration manager for Tools > Tasks > Servers in Lumina IDE.
 */
public class TaskServersSettingsManager {

    private static final String KEY_PREFIX = "tools.task_servers.";
    private static final String KEY_COUNT = KEY_PREFIX + "count";
    private static final String KEY_ID = KEY_PREFIX + "id.";
    private static final String KEY_TYPE = KEY_PREFIX + "type.";
    private static final String KEY_NAME = KEY_PREFIX + "name.";
    private static final String KEY_URL = KEY_PREFIX + "url.";
    private static final String KEY_USERNAME = KEY_PREFIX + "username.";
    private static final String KEY_PASSWORD = KEY_PREFIX + "password.";
    private static final String KEY_SHARE_URL = KEY_PREFIX + "share_url.";
    private static final String KEY_COMMIT_MSG = KEY_PREFIX + "commit_msg.";
    private static final String KEY_USE_HTTP_AUTH = KEY_PREFIX + "use_http_auth.";
    private static final String KEY_HTTP_USER = KEY_PREFIX + "http_user.";
    private static final String KEY_HTTP_PASS = KEY_PREFIX + "http_pass.";

    private static final TaskServersSettingsManager INSTANCE = new TaskServersSettingsManager();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private TaskServersSettingsManager() {
    }

    public static TaskServersSettingsManager getInstance() {
        return INSTANCE;
    }

    public TaskServersSettings load() {
        return getSettings();
    }

    public TaskServersSettings getSettings() {
        TaskServersSettings s = new TaskServersSettings();

        String countStr = Settings.get(KEY_COUNT);
        if (countStr != null) {
            try {
                int count = Integer.parseInt(countStr);
                List<TaskServerEntry> list = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    TaskServerEntry entry = new TaskServerEntry();
                    String id = Settings.get(KEY_ID + i);
                    if (id != null) entry.setId(id);
                    String type = Settings.get(KEY_TYPE + i);
                    if (type != null) entry.setServerType(type);
                    String name = Settings.get(KEY_NAME + i);
                    if (name != null) entry.setName(name);
                    String url = Settings.get(KEY_URL + i);
                    if (url != null) entry.setUrl(url);
                    String user = Settings.get(KEY_USERNAME + i);
                    if (user != null) entry.setUsername(user);
                    String pass = Settings.get(KEY_PASSWORD + i);
                    if (pass != null) entry.setPassword(pass);
                    String share = Settings.get(KEY_SHARE_URL + i);
                    if (share != null) entry.setShareUrl(Boolean.parseBoolean(share));
                    String commit = Settings.get(KEY_COMMIT_MSG + i);
                    if (commit != null) entry.setCommitMessageFormat(commit);
                    String httpAuth = Settings.get(KEY_USE_HTTP_AUTH + i);
                    if (httpAuth != null) entry.setUseHttpAuthentication(Boolean.parseBoolean(httpAuth));
                    String httpUser = Settings.get(KEY_HTTP_USER + i);
                    if (httpUser != null) entry.setHttpUsername(httpUser);
                    String httpPass = Settings.get(KEY_HTTP_PASS + i);
                    if (httpPass != null) entry.setHttpPassword(httpPass);

                    list.add(entry);
                }
                s.setServers(list);
            } catch (NumberFormatException ignored) {
            }
        }

        return s;
    }

    public void setSettings(TaskServersSettings s) {
        if (s == null) return;

        List<TaskServerEntry> list = s.getServers();
        Settings.put(KEY_COUNT, String.valueOf(list.size()));
        for (int i = 0; i < list.size(); i++) {
            TaskServerEntry e = list.get(i);
            Settings.put(KEY_ID + i, e.getId());
            Settings.put(KEY_TYPE + i, e.getServerType());
            Settings.put(KEY_NAME + i, e.getName());
            Settings.put(KEY_URL + i, e.getUrl());
            Settings.put(KEY_USERNAME + i, e.getUsername());
            Settings.put(KEY_PASSWORD + i, e.getPassword());
            Settings.put(KEY_SHARE_URL + i, String.valueOf(e.isShareUrl()));
            Settings.put(KEY_COMMIT_MSG + i, e.getCommitMessageFormat());
            Settings.put(KEY_USE_HTTP_AUTH + i, String.valueOf(e.isUseHttpAuthentication()));
            Settings.put(KEY_HTTP_USER + i, e.getHttpUsername());
            Settings.put(KEY_HTTP_PASS + i, e.getHttpPassword());
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
