package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manager handling persistence and change notifications for Tools > HTTP Client settings.
 */
public class HttpClientSettingsManager {

    private static final HttpClientSettingsManager INSTANCE = new HttpClientSettingsManager();

    private static final String KEY_CUSTOM_HTTP_METHODS = "tools.http_client.custom_methods";

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private HttpClientSettingsManager() {
    }

    public static HttpClientSettingsManager getInstance() {
        return INSTANCE;
    }

    public HttpClientSettings load() {
        return getSettings();
    }

    public HttpClientSettings getSettings() {
        HttpClientSettings s = new HttpClientSettings();
        String cm = Settings.get(KEY_CUSTOM_HTTP_METHODS);
        if (cm != null) s.setCustomHttpMethods(cm);
        return s;
    }

    public void setSettings(HttpClientSettings s) {
        if (s == null) return;
        Settings.put(KEY_CUSTOM_HTTP_METHODS, s.getCustomHttpMethods());
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
        Settings.put(KEY_CUSTOM_HTTP_METHODS, null);
        notifyListeners();
    }

    public void resetDefaults() {
        setSettings(new HttpClientSettings());
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
