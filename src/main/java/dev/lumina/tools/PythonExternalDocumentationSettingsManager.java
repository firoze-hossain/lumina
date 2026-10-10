package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager handling persistence and change listeners for Tools > Python External Documentation settings.
 */
public class PythonExternalDocumentationSettingsManager {

    private static final PythonExternalDocumentationSettingsManager INSTANCE = new PythonExternalDocumentationSettingsManager();

    private static final String KEY_PREFIX = "tools.python_external_doc.";
    private static final String KEY_ENTRIES_COUNT = KEY_PREFIX + "count";
    private static final String KEY_ENTRY_MODULE = KEY_PREFIX + "module.";
    private static final String KEY_ENTRY_URL = KEY_PREFIX + "url.";

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private PythonExternalDocumentationSettingsManager() {
    }

    public static PythonExternalDocumentationSettingsManager getInstance() {
        return INSTANCE;
    }

    public PythonExternalDocumentationSettings load() {
        return getSettings();
    }

    public PythonExternalDocumentationSettings getSettings() {
        PythonExternalDocumentationSettings s = new PythonExternalDocumentationSettings();

        String countStr = Settings.get(KEY_ENTRIES_COUNT);
        if (countStr != null) {
            try {
                int count = Integer.parseInt(countStr);
                List<PythonDocUrlEntry> list = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    String mod = Settings.get(KEY_ENTRY_MODULE + i);
                    String url = Settings.get(KEY_ENTRY_URL + i);
                    if (mod != null && url != null) {
                        list.add(new PythonDocUrlEntry(mod, url));
                    }
                }
                s.setEntries(list);
            } catch (NumberFormatException ignored) {
            }
        }

        return s;
    }

    public void setSettings(PythonExternalDocumentationSettings s) {
        if (s == null) return;

        List<PythonDocUrlEntry> list = s.getEntries();
        Settings.put(KEY_ENTRIES_COUNT, String.valueOf(list.size()));
        for (int i = 0; i < list.size(); i++) {
            PythonDocUrlEntry e = list.get(i);
            Settings.put(KEY_ENTRY_MODULE + i, e.getModuleName());
            Settings.put(KEY_ENTRY_URL + i, e.getUrlPattern());
        }

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
