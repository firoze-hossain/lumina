package dev.lumina.tools;

import dev.lumina.util.Settings;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic configuration manager for Code With Me collaborative sessions in Lumina IDE.
 */
public class CodeWithMeSettingsManager {

    private static final String KEY_USER_NAME = "tools.cwm.user_name";
    private static final String KEY_LOBBY_URL = "tools.cwm.lobby_url";

    private static volatile CodeWithMeSettingsManager instance;

    private CodeWithMeSettings currentSettings;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private CodeWithMeSettingsManager() {
        this.currentSettings = loadSettings();
    }

    public static CodeWithMeSettingsManager getInstance() {
        if (instance == null) {
            synchronized (CodeWithMeSettingsManager.class) {
                if (instance == null) {
                    instance = new CodeWithMeSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized CodeWithMeSettings getSettings() {
        if (currentSettings == null) {
            currentSettings = loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(CodeWithMeSettings newSettings) {
        if (newSettings == null) {
            this.currentSettings = new CodeWithMeSettings();
        } else {
            this.currentSettings = newSettings.clone();
        }
        saveSettings();
        notifyListeners();
    }

    public String getSystemUserName() {
        return System.getProperty("user.name", "user");
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            try {
                listener.run();
            } catch (Exception ignored) {
            }
        }
    }

    private CodeWithMeSettings loadSettings() {
        CodeWithMeSettings settings = new CodeWithMeSettings();
        String u = Settings.get(KEY_USER_NAME);
        if (u != null && !u.isBlank()) {
            settings.setUserName(u);
        }
        String lobby = Settings.get(KEY_LOBBY_URL);
        if (lobby != null) {
            settings.setLobbyServerUrl(lobby);
        }
        return settings;
    }

    private void saveSettings() {
        if (currentSettings == null) return;
        Settings.put(KEY_USER_NAME, currentSettings.getUserName());
        Settings.put(KEY_LOBBY_URL, currentSettings.getLobbyServerUrl());
    }
}
