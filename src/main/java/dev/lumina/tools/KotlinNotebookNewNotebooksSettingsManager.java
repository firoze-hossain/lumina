package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager handling persistence and change listeners for
 * Tools > Kotlin Notebook > Settings for New Notebooks in Lumina IDE.
 */
public class KotlinNotebookNewNotebooksSettingsManager {

    private static final KotlinNotebookNewNotebooksSettingsManager INSTANCE = new KotlinNotebookNewNotebooksSettingsManager();

    private static final String KEY_PREFIX = "tools.kotlin_notebook.new_notebooks.";
    private static final String KEY_ADD_PROJECT_LIBS = KEY_PREFIX + "add_project_libraries_to_classpath";

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private KotlinNotebookNewNotebooksSettingsManager() {
    }

    public static KotlinNotebookNewNotebooksSettingsManager getInstance() {
        return INSTANCE;
    }

    public KotlinNotebookNewNotebooksSettings load() {
        return getSettings();
    }

    public KotlinNotebookNewNotebooksSettings getSettings() {
        KotlinNotebookNewNotebooksSettings s = new KotlinNotebookNewNotebooksSettings();

        String val = Settings.get(KEY_ADD_PROJECT_LIBS);
        if (val != null) {
            s.setAddProjectLibrariesToClasspath(Boolean.parseBoolean(val));
        }

        return s;
    }

    public void setSettings(KotlinNotebookNewNotebooksSettings s) {
        if (s == null) return;

        Settings.put(KEY_ADD_PROJECT_LIBS, String.valueOf(s.isAddProjectLibrariesToClasspath()));
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
