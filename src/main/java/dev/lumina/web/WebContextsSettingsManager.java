package dev.lumina.web;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Languages & Frameworks > Web Contexts in Lumina IDE.
 * Dynamically resolves web path mappings and notifies listeners upon changes.
 */
public class WebContextsSettingsManager {

    public static final String KEY_WEB_CONTEXT_MAPPINGS = "web.contexts.mappings";

    private static volatile WebContextsSettingsManager instance;
    private WebContextsSettings currentSettings;
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private WebContextsSettingsManager() {
        loadSettings();
    }

    public static WebContextsSettingsManager getInstance() {
        if (instance == null) {
            synchronized (WebContextsSettingsManager.class) {
                if (instance == null) {
                    instance = new WebContextsSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized WebContextsSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(WebContextsSettings settings) {
        if (settings == null) return;
        this.currentSettings = settings.clone();
        saveSettings();
        notifyListeners();
    }

    public synchronized void loadSettings() {
        WebContextsSettings s = new WebContextsSettings();

        String mappingsStr = Settings.get(KEY_WEB_CONTEXT_MAPPINGS);
        if (mappingsStr != null && !mappingsStr.isBlank()) {
            List<WebContextMapping> list = new ArrayList<>();
            String[] entries = mappingsStr.split("###");
            for (String entry : entries) {
                if (!entry.isBlank()) {
                    String[] parts = entry.split("@@@", 2);
                    String path = parts[0];
                    String ctx = parts.length > 1 ? parts[1] : "/";
                    list.add(new WebContextMapping(path, ctx));
                }
            }
            s.setMappings(list);
        }

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        StringBuilder sb = new StringBuilder();
        for (WebContextMapping m : currentSettings.getMappings()) {
            if (sb.length() > 0) sb.append("###");
            sb.append(m.getPath() != null ? m.getPath() : "")
              .append("@@@")
              .append(m.getWebContext() != null ? m.getWebContext() : "/");
        }
        Settings.put(KEY_WEB_CONTEXT_MAPPINGS, sb.toString());
    }

    /**
     * Resolves the Web Context for a given file or directory path (longest prefix match).
     */
    public synchronized String resolveWebContext(String path) {
        if (currentSettings == null) {
            loadSettings();
        }
        if (path != null && !path.isBlank()) {
            String bestMatchCtx = null;
            int bestMatchLen = -1;
            for (WebContextMapping mapping : currentSettings.getMappings()) {
                String mapPath = mapping.getPath();
                if (mapPath != null && !mapPath.isBlank()) {
                    if (path.equals(mapPath) || path.startsWith(mapPath)) {
                        if (mapPath.length() > bestMatchLen) {
                            bestMatchLen = mapPath.length();
                            bestMatchCtx = mapping.getWebContext();
                        }
                    }
                }
            }
            if (bestMatchCtx != null) {
                return bestMatchCtx;
            }
        }
        return "/";
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
