package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manager handling persistence and change notifications for Features Trainer settings.
 */
public class FeaturesTrainerSettingsManager {

    private static final FeaturesTrainerSettingsManager INSTANCE = new FeaturesTrainerSettingsManager();

    private static final String KEY_MAIN_LANGUAGE = "tools.features_trainer.main_language";
    private static final String KEY_SHOW_NOTIFICATIONS = "tools.features_trainer.show_notifications_on_new_lessons";

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private FeaturesTrainerSettingsManager() {
    }

    public static FeaturesTrainerSettingsManager getInstance() {
        return INSTANCE;
    }

    public FeaturesTrainerSettings getSettings() {
        FeaturesTrainerSettings settings = new FeaturesTrainerSettings();
        String lang = Settings.get(KEY_MAIN_LANGUAGE);
        if (lang != null && !lang.isBlank()) {
            settings.setMainLanguage(lang);
        }

        String notif = Settings.get(KEY_SHOW_NOTIFICATIONS);
        if (notif != null) {
            settings.setShowNotificationsOnNewLessons(Boolean.parseBoolean(notif));
        }

        return settings;
    }

    public void setSettings(FeaturesTrainerSettings settings) {
        if (settings == null) return;
        Settings.put(KEY_MAIN_LANGUAGE, settings.getMainLanguage());
        Settings.put(KEY_SHOW_NOTIFICATIONS, String.valueOf(settings.isShowNotificationsOnNewLessons()));
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
        Settings.put(KEY_MAIN_LANGUAGE, null);
        Settings.put(KEY_SHOW_NOTIFICATIONS, null);
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
