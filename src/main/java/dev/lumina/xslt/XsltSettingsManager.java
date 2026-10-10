package dev.lumina.xslt;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Languages & Frameworks > XSLT settings in Lumina IDE.
 */
public class XsltSettingsManager {

    public static final String KEY_SHOW_ASSOCIATED_FILES = "xslt.show_associated_files_in_project_view";

    private static volatile XsltSettingsManager instance;
    private XsltSettings currentSettings;
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private XsltSettingsManager() {
        loadSettings();
    }

    public static XsltSettingsManager getInstance() {
        if (instance == null) {
            synchronized (XsltSettingsManager.class) {
                if (instance == null) {
                    instance = new XsltSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized XsltSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(XsltSettings settings) {
        if (settings == null) return;
        this.currentSettings = settings.clone();
        saveSettings();
        notifyListeners();
    }

    public synchronized void loadSettings() {
        XsltSettings s = new XsltSettings();

        String show = Settings.get(KEY_SHOW_ASSOCIATED_FILES);
        if (show != null) {
            s.setShowAssociatedFilesInProjectView(Boolean.parseBoolean(show));
        }

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.put(KEY_SHOW_ASSOCIATED_FILES, String.valueOf(currentSettings.isShowAssociatedFilesInProjectView()));
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : changeListeners) {
            try {
                listener.run();
            } catch (Throwable ignored) {}
        }
    }
}
