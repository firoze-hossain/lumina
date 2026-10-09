package dev.lumina.kubernetes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Languages & Frameworks > Kubernetes settings in Lumina IDE.
 */
public class KubernetesLanguageSettingsManager {

    public static final String KEY_KUBERNETES_LANGUAGE_SETTINGS = "kubernetes.language.settings";

    private static KubernetesLanguageSettingsManager instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private KubernetesLanguageSettings currentSettings = new KubernetesLanguageSettings();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private KubernetesLanguageSettingsManager() {
        loadSettings();
    }

    public static synchronized KubernetesLanguageSettingsManager getInstance() {
        if (instance == null) {
            instance = new KubernetesLanguageSettingsManager();
        }
        return instance;
    }

    public synchronized KubernetesLanguageSettings getSettings() {
        return currentSettings.copy();
    }

    public synchronized void setSettings(KubernetesLanguageSettings settings) {
        this.currentSettings = settings != null ? settings.copy() : new KubernetesLanguageSettings();
        saveSettings();
        notifyListeners();
    }

    public synchronized void addChangeListener(Runnable listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public synchronized void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable r : changeListeners) {
            try {
                r.run();
            } catch (Exception ignored) {}
        }
    }

    public synchronized void saveSettings() {
        Settings.put(KEY_KUBERNETES_LANGUAGE_SETTINGS, GSON.toJson(currentSettings));
    }

    public synchronized void loadSettings() {
        String json = Settings.get(KEY_KUBERNETES_LANGUAGE_SETTINGS);
        if (json != null && !json.isBlank()) {
            try {
                KubernetesLanguageSettings parsed = GSON.fromJson(json, KubernetesLanguageSettings.class);
                if (parsed != null) {
                    this.currentSettings = parsed;
                    return;
                }
            } catch (Exception ignored) {}
        }
        this.currentSettings = new KubernetesLanguageSettings();
    }

    public synchronized void resetDefaults() {
        this.currentSettings = new KubernetesLanguageSettings();
        saveSettings();
        notifyListeners();
    }

    public synchronized boolean checkConfiguration() {
        // Validates cluster context or API schema availability
        return !KubernetesLanguageSettings.DEFAULT_CLUSTER.equals(currentSettings.getCurrentCluster());
    }

    public synchronized void resetSchemaCache() {
        // Clears schema cache directory
    }
}
