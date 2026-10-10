package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton configuration manager for Tools > Startup Tasks in Lumina IDE.
 */
public class StartupTasksSettingsManager {

    private static final String KEY_PREFIX = "tools.startup_tasks.";
    private static final String KEY_COUNT = KEY_PREFIX + "count";
    private static final String KEY_ID = KEY_PREFIX + "id.";
    private static final String KEY_NAME = KEY_PREFIX + "name.";
    private static final String KEY_TYPE = KEY_PREFIX + "type.";
    private static final String KEY_SHARED = KEY_PREFIX + "shared.";

    private static final StartupTasksSettingsManager INSTANCE = new StartupTasksSettingsManager();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private StartupTasksSettingsManager() {
    }

    public static StartupTasksSettingsManager getInstance() {
        return INSTANCE;
    }

    public StartupTasksSettings load() {
        return getSettings();
    }

    public StartupTasksSettings getSettings() {
        StartupTasksSettings s = new StartupTasksSettings();

        String countStr = Settings.get(KEY_COUNT);
        if (countStr != null) {
            try {
                int count = Integer.parseInt(countStr);
                List<StartupTaskEntry> list = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    StartupTaskEntry entry = new StartupTaskEntry();
                    String id = Settings.get(KEY_ID + i);
                    if (id != null) entry.setId(id);
                    String name = Settings.get(KEY_NAME + i);
                    if (name != null) entry.setName(name);
                    String type = Settings.get(KEY_TYPE + i);
                    if (type != null) entry.setConfigurationType(type);
                    String shared = Settings.get(KEY_SHARED + i);
                    if (shared != null) entry.setShared(Boolean.parseBoolean(shared));

                    list.add(entry);
                }
                s.setTasks(list);
            } catch (NumberFormatException ignored) {
            }
        }

        return s;
    }

    public void setSettings(StartupTasksSettings s) {
        if (s == null) return;

        List<StartupTaskEntry> list = s.getTasks();
        Settings.put(KEY_COUNT, String.valueOf(list.size()));
        for (int i = 0; i < list.size(); i++) {
            StartupTaskEntry e = list.get(i);
            Settings.put(KEY_ID + i, e.getId());
            Settings.put(KEY_NAME + i, e.getName());
            Settings.put(KEY_TYPE + i, e.getConfigurationType());
            Settings.put(KEY_SHARED + i, String.valueOf(e.isShared()));
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
