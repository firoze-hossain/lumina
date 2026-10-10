package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager handling persistence and change listeners for Tools > Qodana settings.
 */
public class QodanaSettingsManager {

    private static final QodanaSettingsManager INSTANCE = new QodanaSettingsManager();

    private static final String KEY_PREFIX = "tools.qodana.";
    private static final String KEY_QODANA_URL = KEY_PREFIX + "url";
    private static final String KEY_LOGGED_IN = KEY_PREFIX + "logged_in";
    private static final String KEY_ACCOUNT_NAME = KEY_PREFIX + "account_name";

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private QodanaSettingsManager() {
    }

    public static QodanaSettingsManager getInstance() {
        return INSTANCE;
    }

    public QodanaSettings load() {
        return getSettings();
    }

    public QodanaSettings getSettings() {
        QodanaSettings s = new QodanaSettings();

        String val = Settings.get(KEY_QODANA_URL);
        if (val != null) s.setQodanaUrl(val);

        val = Settings.get(KEY_LOGGED_IN);
        if (val != null) s.setLoggedIn(Boolean.parseBoolean(val));

        val = Settings.get(KEY_ACCOUNT_NAME);
        if (val != null) s.setAccountName(val);

        return s;
    }

    public void setSettings(QodanaSettings s) {
        if (s == null) return;

        Settings.put(KEY_QODANA_URL, s.getQodanaUrl());
        Settings.put(KEY_LOGGED_IN, String.valueOf(s.isLoggedIn()));
        Settings.put(KEY_ACCOUNT_NAME, s.getAccountName());

        notifyListeners();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeChangeListener(Runnable listener) {
        if (listener != null) listeners.remove(listener);
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
