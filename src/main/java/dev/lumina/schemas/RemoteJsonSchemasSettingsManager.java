package dev.lumina.schemas;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Languages & Frameworks > Schemas and DTDs > Remote JSON Schemas settings in Lumina IDE.
 */
public class RemoteJsonSchemasSettingsManager {

    public static final String KEY_ALLOW_DOWNLOAD = "schemas.remote.json.allow.download";
    public static final String KEY_USE_SCHEMASTORE = "schemas.remote.json.use.schemastore";
    public static final String KEY_ALWAYS_DOWNLOAD_MOST_RECENT = "schemas.remote.json.always.download.most.recent";
    public static final String KEY_SCHEMASTORE_CATALOG_URL = "schemas.remote.json.schemastore.url";

    private static volatile RemoteJsonSchemasSettingsManager instance;
    private RemoteJsonSchemasSettings currentSettings;
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private RemoteJsonSchemasSettingsManager() {
        loadSettings();
    }

    public static RemoteJsonSchemasSettingsManager getInstance() {
        if (instance == null) {
            synchronized (RemoteJsonSchemasSettingsManager.class) {
                if (instance == null) {
                    instance = new RemoteJsonSchemasSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized RemoteJsonSchemasSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(RemoteJsonSchemasSettings settings) {
        if (settings == null) return;
        this.currentSettings = settings.clone();
        saveSettings();
        notifyListeners();
    }

    public synchronized void loadSettings() {
        RemoteJsonSchemasSettings s = new RemoteJsonSchemasSettings();

        String allow = Settings.get(KEY_ALLOW_DOWNLOAD);
        if (allow != null) {
            s.setAllowDownloadRemoteSchemas(Boolean.parseBoolean(allow));
        }

        String useStore = Settings.get(KEY_USE_SCHEMASTORE);
        if (useStore != null) {
            s.setUseSchemaStoreCatalog(Boolean.parseBoolean(useStore));
        }

        String alwaysRecent = Settings.get(KEY_ALWAYS_DOWNLOAD_MOST_RECENT);
        if (alwaysRecent != null) {
            s.setAlwaysDownloadMostRecentVersion(Boolean.parseBoolean(alwaysRecent));
        }

        String url = Settings.get(KEY_SCHEMASTORE_CATALOG_URL);
        if (url != null && !url.isBlank()) {
            s.setSchemaStoreCatalogUrl(url);
        }

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.put(KEY_ALLOW_DOWNLOAD, String.valueOf(currentSettings.isAllowDownloadRemoteSchemas()));
        Settings.put(KEY_USE_SCHEMASTORE, String.valueOf(currentSettings.isUseSchemaStoreCatalog()));
        Settings.put(KEY_ALWAYS_DOWNLOAD_MOST_RECENT, String.valueOf(currentSettings.isAlwaysDownloadMostRecentVersion()));
        Settings.put(KEY_SCHEMASTORE_CATALOG_URL, currentSettings.getSchemaStoreCatalogUrl());
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable r : changeListeners) {
            try {
                r.run();
            } catch (Exception ignored) {}
        }
    }
}
