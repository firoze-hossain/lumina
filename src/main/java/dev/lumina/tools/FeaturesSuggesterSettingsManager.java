package dev.lumina.tools;

import dev.lumina.util.Settings;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic configuration manager for Tools > Features Suggester in Lumina IDE.
 */
public class FeaturesSuggesterSettingsManager {

    private static final String KEY_PREFIX = "tools.features_suggester.";
    private static final String KEY_SHOW = KEY_PREFIX + "show_suggestions";
    private static final String KEY_ACTION_PREFIX = KEY_PREFIX + "action.";

    private static volatile FeaturesSuggesterSettingsManager instance;

    private FeaturesSuggesterSettings currentSettings;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private FeaturesSuggesterSettingsManager() {
        this.currentSettings = loadSettings();
    }

    public static FeaturesSuggesterSettingsManager getInstance() {
        if (instance == null) {
            synchronized (FeaturesSuggesterSettingsManager.class) {
                if (instance == null) {
                    instance = new FeaturesSuggesterSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized FeaturesSuggesterSettings getSettings() {
        if (currentSettings == null) {
            currentSettings = loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(FeaturesSuggesterSettings newSettings) {
        if (newSettings == null) {
            this.currentSettings = new FeaturesSuggesterSettings();
        } else {
            this.currentSettings = newSettings.clone();
        }
        saveSettings();
        notifyListeners();
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

    private FeaturesSuggesterSettings loadSettings() {
        FeaturesSuggesterSettings s = new FeaturesSuggesterSettings();

        String show = Settings.get(KEY_SHOW);
        if (show != null) s.setShowSuggestions(Boolean.parseBoolean(show));

        for (String action : s.getSuggestedActions().keySet()) {
            String val = Settings.get(KEY_ACTION_PREFIX + action);
            if (val != null) {
                s.getSuggestedActions().put(action, Boolean.parseBoolean(val));
            }
        }

        return s;
    }

    private void saveSettings() {
        if (currentSettings == null) return;

        Settings.put(KEY_SHOW, String.valueOf(currentSettings.isShowSuggestions()));

        for (Map.Entry<String, Boolean> entry : currentSettings.getSuggestedActions().entrySet()) {
            Settings.put(KEY_ACTION_PREFIX + entry.getKey(), String.valueOf(entry.getValue()));
        }
    }
}
