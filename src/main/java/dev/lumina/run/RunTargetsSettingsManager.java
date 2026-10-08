package dev.lumina.run;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.lumina.util.Settings;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Settings manager for Run Targets in Lumina IDE (matching IntelliJ IDEA Run Targets).
 * Dynamically manages SSH, Docker, and Docker Compose execution targets and project default target.
 */
public class RunTargetsSettingsManager {

    public static final String KEY_RUN_TARGETS = "run.targets.list";
    public static final String KEY_DEFAULT_TARGET_ID = "run.targets.default.id";
    public static final String LOCAL_MACHINE_ID = "local";

    private static RunTargetsSettingsManager instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final List<RunTargetConfig> targets = new ArrayList<>();
    private String projectDefaultTargetId = LOCAL_MACHINE_ID;
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private RunTargetsSettingsManager() {
        loadSettings();
    }

    public static synchronized RunTargetsSettingsManager getInstance() {
        if (instance == null) {
            instance = new RunTargetsSettingsManager();
        }
        return instance;
    }

    public synchronized List<RunTargetConfig> getTargets() {
        List<RunTargetConfig> copy = new ArrayList<>();
        for (RunTargetConfig t : targets) {
            copy.add(t.clone());
        }
        return copy;
    }

    public synchronized void setTargets(List<RunTargetConfig> newTargets) {
        targets.clear();
        if (newTargets != null) {
            for (RunTargetConfig t : newTargets) {
                targets.add(t.clone());
            }
        }
        // Verify default target still exists
        if (!LOCAL_MACHINE_ID.equals(projectDefaultTargetId) && getTargetById(projectDefaultTargetId) == null) {
            projectDefaultTargetId = LOCAL_MACHINE_ID;
        }
        saveSettings();
        notifyChanged();
    }

    public synchronized void addTarget(RunTargetConfig target) {
        if (target != null) {
            targets.add(target.clone());
            saveSettings();
            notifyChanged();
        }
    }

    public synchronized RunTargetConfig duplicateTarget(String id) {
        RunTargetConfig original = getTargetById(id);
        if (original != null) {
            RunTargetConfig clone = original.clone();
            clone.setId(UUID.randomUUID().toString());
            clone.setName(original.getName() + " (Copy)");
            targets.add(clone);
            saveSettings();
            notifyChanged();
            return clone;
        }
        return null;
    }

    public synchronized void removeTarget(String id) {
        if (id != null) {
            targets.removeIf(t -> id.equals(t.getId()));
            if (id.equals(projectDefaultTargetId)) {
                projectDefaultTargetId = LOCAL_MACHINE_ID;
            }
            saveSettings();
            notifyChanged();
        }
    }

    public synchronized RunTargetConfig getTargetById(String id) {
        if (id == null) return null;
        for (RunTargetConfig t : targets) {
            if (id.equals(t.getId())) {
                return t.clone();
            }
        }
        return null;
    }

    public synchronized String getProjectDefaultTargetId() {
        return projectDefaultTargetId;
    }

    public synchronized void setProjectDefaultTargetId(String projectDefaultTargetId) {
        this.projectDefaultTargetId = projectDefaultTargetId != null ? projectDefaultTargetId : LOCAL_MACHINE_ID;
        saveSettings();
        notifyChanged();
    }

    public synchronized void resetToDefaults() {
        targets.clear();
        projectDefaultTargetId = LOCAL_MACHINE_ID;
        saveSettings();
        notifyChanged();
    }

    public synchronized void loadSettings() {
        try {
            String json = Settings.get(KEY_RUN_TARGETS);
            if (json != null && !json.isBlank()) {
                Type type = new TypeToken<List<RunTargetConfig>>() {}.getType();
                List<RunTargetConfig> loaded = GSON.fromJson(json, type);
                targets.clear();
                if (loaded != null) {
                    targets.addAll(loaded);
                }
            }
        } catch (Exception ignored) {}

        String defId = Settings.get(KEY_DEFAULT_TARGET_ID);
        projectDefaultTargetId = (defId != null && !defId.isBlank()) ? defId : LOCAL_MACHINE_ID;
    }

    public synchronized void saveSettings() {
        try {
            Settings.put(KEY_RUN_TARGETS, GSON.toJson(targets));
            Settings.put(KEY_DEFAULT_TARGET_ID, projectDefaultTargetId);
        } catch (Exception ignored) {}
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
