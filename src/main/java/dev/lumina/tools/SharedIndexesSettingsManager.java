package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton configuration manager for Tools > Shared Indexes settings in Lumina IDE.
 */
public class SharedIndexesSettingsManager {

    private static final String KEY_DOWNLOAD_MODE = "tools.shared_indexes.download_mode";
    private static final String KEY_CUSTOM_SERVER_URL = "tools.shared_indexes.custom_server_url";
    private static final String KEY_LOCAL_CACHE_DIR = "tools.shared_indexes.local_cache_directory";
    private static final String KEY_DOWNLOAD_JDK = "tools.shared_indexes.download_jdk";
    private static final String KEY_DOWNLOAD_MAVEN = "tools.shared_indexes.download_maven";

    private static final SharedIndexesSettingsManager INSTANCE = new SharedIndexesSettingsManager();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private SharedIndexesSettingsManager() {
    }

    public static SharedIndexesSettingsManager getInstance() {
        return INSTANCE;
    }

    public SharedIndexesSettings load() {
        return getSettings();
    }

    public SharedIndexesSettings getSettings() {
        SharedIndexesSettings s = new SharedIndexesSettings();

        String mode = Settings.get(KEY_DOWNLOAD_MODE);
        if (mode != null) s.setDownloadMode(mode);

        String serverUrl = Settings.get(KEY_CUSTOM_SERVER_URL);
        if (serverUrl != null) s.setCustomServerUrl(serverUrl);

        String cacheDir = Settings.get(KEY_LOCAL_CACHE_DIR);
        if (cacheDir != null) s.setLocalCacheDirectory(cacheDir);

        String jdk = Settings.get(KEY_DOWNLOAD_JDK);
        if (jdk != null) s.setDownloadJdkIndexes(Boolean.parseBoolean(jdk));

        String maven = Settings.get(KEY_DOWNLOAD_MAVEN);
        if (maven != null) s.setDownloadMavenIndexes(Boolean.parseBoolean(maven));

        return s;
    }

    public void setSettings(SharedIndexesSettings s) {
        if (s == null) return;

        Settings.put(KEY_DOWNLOAD_MODE, s.getDownloadMode());
        Settings.put(KEY_CUSTOM_SERVER_URL, s.getCustomServerUrl());
        Settings.put(KEY_LOCAL_CACHE_DIR, s.getLocalCacheDirectory());
        Settings.put(KEY_DOWNLOAD_JDK, String.valueOf(s.isDownloadJdkIndexes()));
        Settings.put(KEY_DOWNLOAD_MAVEN, String.valueOf(s.isDownloadMavenIndexes()));

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
