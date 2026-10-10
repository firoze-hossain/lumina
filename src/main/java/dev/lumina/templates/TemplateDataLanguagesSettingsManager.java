package dev.lumina.templates;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Languages & Frameworks > Template Data Languages in Lumina IDE.
 */
public class TemplateDataLanguagesSettingsManager {

    public static final String KEY_TEMPLATE_LANGUAGE_PROJECT = "template.data.languages.project";
    public static final String KEY_TEMPLATE_LANGUAGE_MAPPINGS = "template.data.languages.mappings";

    private static volatile TemplateDataLanguagesSettingsManager instance;
    private TemplateDataLanguagesSettings currentSettings;
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private TemplateDataLanguagesSettingsManager() {
        loadSettings();
    }

    public static TemplateDataLanguagesSettingsManager getInstance() {
        if (instance == null) {
            synchronized (TemplateDataLanguagesSettingsManager.class) {
                if (instance == null) {
                    instance = new TemplateDataLanguagesSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized TemplateDataLanguagesSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(TemplateDataLanguagesSettings settings) {
        if (settings == null) return;
        this.currentSettings = settings.clone();
        saveSettings();
        notifyListeners();
    }

    public synchronized void loadSettings() {
        TemplateDataLanguagesSettings s = new TemplateDataLanguagesSettings();

        String project = Settings.get(KEY_TEMPLATE_LANGUAGE_PROJECT);
        if (project != null && !project.isBlank()) {
            s.setProjectLanguage(project);
        }

        String mappingsStr = Settings.get(KEY_TEMPLATE_LANGUAGE_MAPPINGS);
        if (mappingsStr != null && !mappingsStr.isBlank()) {
            List<TemplateDataLanguageMapping> list = new ArrayList<>();
            String[] entries = mappingsStr.split("###");
            for (String entry : entries) {
                if (!entry.isBlank()) {
                    String[] parts = entry.split("@@@", 2);
                    String path = parts[0];
                    String lang = parts.length > 1 ? parts[1] : "<None>";
                    list.add(new TemplateDataLanguageMapping(path, lang));
                }
            }
            s.setMappings(list);
        }

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.put(KEY_TEMPLATE_LANGUAGE_PROJECT, currentSettings.getProjectLanguage());

        StringBuilder sb = new StringBuilder();
        for (TemplateDataLanguageMapping m : currentSettings.getMappings()) {
            if (sb.length() > 0) sb.append("###");
            sb.append(m.getPath() != null ? m.getPath() : "")
              .append("@@@")
              .append(m.getLanguage() != null ? m.getLanguage() : "<None>");
        }
        Settings.put(KEY_TEMPLATE_LANGUAGE_MAPPINGS, sb.toString());
    }

    /**
     * Dynamically resolves the effective Template Data Language for a given file or directory path.
     * Searches path mappings first (longest prefix match), falls back to project language.
     */
    public synchronized String resolveLanguage(String path) {
        if (currentSettings == null) {
            loadSettings();
        }
        if (path != null && !path.isBlank()) {
            String bestMatchLang = null;
            int bestMatchLen = -1;
            for (TemplateDataLanguageMapping mapping : currentSettings.getMappings()) {
                String mapPath = mapping.getPath();
                if (mapPath != null && !mapPath.isBlank()) {
                    if (path.equals(mapPath) || path.startsWith(mapPath)) {
                        if (mapPath.length() > bestMatchLen) {
                            bestMatchLen = mapPath.length();
                            bestMatchLang = mapping.getLanguage();
                        }
                    }
                }
            }
            if (bestMatchLang != null && !bestMatchLang.equals("<None>")) {
                return bestMatchLang;
            }
        }

        String project = currentSettings.getProjectLanguage();
        if (project != null && !project.equals("<None>")) {
            return project;
        }

        return "<None>";
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
