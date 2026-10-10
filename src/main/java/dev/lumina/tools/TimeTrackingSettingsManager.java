package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton configuration manager for Tools > Tasks > Time Tracking settings in Lumina IDE.
 */
public class TimeTrackingSettingsManager {

    private static final String KEY_ENABLE_TIME_TRACKING = "tools.tasks.time_tracking.enable";
    private static final String KEY_SUSPEND_DELAY = "tools.tasks.time_tracking.suspend_delay";

    private static final TimeTrackingSettingsManager INSTANCE = new TimeTrackingSettingsManager();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private TimeTrackingSettingsManager() {
    }

    public static TimeTrackingSettingsManager getInstance() {
        return INSTANCE;
    }

    public TimeTrackingSettings load() {
        return getSettings();
    }

    public TimeTrackingSettings getSettings() {
        TimeTrackingSettings s = new TimeTrackingSettings();

        String en = Settings.get(KEY_ENABLE_TIME_TRACKING);
        if (en != null) s.setEnableTimeTracking(Boolean.parseBoolean(en));

        String sd = Settings.get(KEY_SUSPEND_DELAY);
        if (sd != null) {
            try { s.setSuspendDelaySeconds(Integer.parseInt(sd)); } catch (NumberFormatException ignored) {}
        }

        return s;
    }

    public void setSettings(TimeTrackingSettings s) {
        if (s == null) return;

        Settings.put(KEY_ENABLE_TIME_TRACKING, String.valueOf(s.isEnableTimeTracking()));
        Settings.put(KEY_SUSPEND_DELAY, String.valueOf(s.getSuspendDelaySeconds()));

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
