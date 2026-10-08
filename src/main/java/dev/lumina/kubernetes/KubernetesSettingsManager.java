package dev.lumina.kubernetes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.lumina.util.Settings;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Settings manager for Kubernetes configuration, tool verification, and persistence in Lumina IDE.
 */
public class KubernetesSettingsManager {

    public static final String KEY_KUBERNETES_SETTINGS = "kubernetes.settings";

    private static KubernetesSettingsManager instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private KubernetesSettings settings = new KubernetesSettings();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    public record ToolTestResult(boolean success, String message, String output) {}

    private KubernetesSettingsManager() {
        loadSettings();
    }

    public static synchronized KubernetesSettingsManager getInstance() {
        if (instance == null) {
            instance = new KubernetesSettingsManager();
        }
        return instance;
    }

    public synchronized KubernetesSettings getSettings() {
        return settings.clone();
    }

    public synchronized void setSettings(KubernetesSettings newSettings) {
        if (newSettings != null) {
            this.settings = newSettings.clone();
            saveSettings();
            notifyChanged();
        }
    }

    public synchronized void updateSettings(KubernetesSettings newSettings) {
        setSettings(newSettings);
    }

    public synchronized void resetToDefaults() {
        this.settings = new KubernetesSettings();
        saveSettings();
        notifyChanged();
    }

    public synchronized void loadSettings() {
        try {
            String json = Settings.get(KEY_KUBERNETES_SETTINGS);
            if (json != null && !json.isBlank()) {
                KubernetesSettings loaded = GSON.fromJson(json, KubernetesSettings.class);
                if (loaded != null) {
                    this.settings = loaded;
                }
            }
        } catch (Exception ignored) {}
    }

    public synchronized void saveSettings() {
        try {
            Settings.put(KEY_KUBERNETES_SETTINGS, GSON.toJson(settings));
        } catch (Exception ignored) {}
    }

    /**
     * Executes the given executable with a quick version flag to verify location & connectivity.
     */
    public ToolTestResult testTool(String executablePath, String toolName) {
        if (executablePath == null || executablePath.isBlank()) {
            return new ToolTestResult(false, toolName + " executable path is empty", "");
        }
        try {
            String[] cmd;
            if ("helm".equalsIgnoreCase(toolName)) {
                cmd = new String[]{executablePath.trim(), "version", "--short"};
            } else {
                cmd = new String[]{executablePath.trim(), "version", "--client"};
            }
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(4, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new ToolTestResult(false, "Timeout testing " + toolName + " executable.", "");
            }

            String output = sb.toString().trim();
            if (process.exitValue() == 0 || !output.isEmpty()) {
                String firstLine = output.lines().findFirst().orElse("Detected");
                return new ToolTestResult(true, toolName + " found: " + firstLine, output);
            } else {
                return new ToolTestResult(false, toolName + " returned exit code " + process.exitValue(), output);
            }
        } catch (Exception e) {
            return new ToolTestResult(false, "Failed to run " + toolName + ": " + e.getMessage(), "");
        }
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void notifyChanged() {
        for (Runnable r : changeListeners) {
            try {
                r.run();
            } catch (Exception ignored) {}
        }
    }
}
