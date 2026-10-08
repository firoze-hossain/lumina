package dev.lumina.profiler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.lumina.util.Settings;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Settings manager for Java Profiler configurations and filter groups.
 * Handles dynamic JSON persistence, default presets, and reordering.
 */
public class JavaProfilerSettingsManager {

    public static final String KEY_PROFILER_CONFIGS = "profiler.configurations";
    public static final String KEY_PROFILER_SELECTED_ID = "profiler.selected.id";
    public static final String KEY_PROFILER_FILTER_GROUPS = "profiler.filter.groups";

    private static JavaProfilerSettingsManager instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final List<JavaProfilerConfig> profilers = new ArrayList<>();
    private String selectedProfilerId;
    private final List<ProfilerFilterGroup> filterGroups = new ArrayList<>();

    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private JavaProfilerSettingsManager() {
        loadSettings();
    }

    public static synchronized JavaProfilerSettingsManager getInstance() {
        if (instance == null) {
            instance = new JavaProfilerSettingsManager();
        }
        return instance;
    }

    // ============================================================
    // Profiler Configurations
    // ============================================================

    public synchronized List<JavaProfilerConfig> getProfilers() {
        List<JavaProfilerConfig> copy = new ArrayList<>();
        for (JavaProfilerConfig c : profilers) {
            copy.add(c.clone());
        }
        return copy;
    }

    public synchronized void setProfilers(List<JavaProfilerConfig> newProfilers) {
        profilers.clear();
        if (newProfilers != null) {
            for (JavaProfilerConfig c : newProfilers) {
                profilers.add(c.clone());
            }
        }
        if (profilers.isEmpty()) {
            profilers.add(createDefaultProfiler());
        }
        if (selectedProfilerId == null || getProfilerById(selectedProfilerId) == null) {
            selectedProfilerId = profilers.get(0).getId();
        }
        saveSettings();
        notifyChanged();
    }

    public synchronized JavaProfilerConfig getProfilerById(String id) {
        if (id == null) return null;
        for (JavaProfilerConfig c : profilers) {
            if (id.equals(c.getId())) {
                return c.clone();
            }
        }
        return null;
    }

    public synchronized String getSelectedProfilerId() {
        return selectedProfilerId;
    }

    public synchronized void setSelectedProfilerId(String selectedProfilerId) {
        this.selectedProfilerId = selectedProfilerId;
        saveSettings();
        notifyChanged();
    }

    public synchronized void addProfiler(JavaProfilerConfig config) {
        if (config != null) {
            profilers.add(config.clone());
            selectedProfilerId = config.getId();
            saveSettings();
            notifyChanged();
        }
    }

    public synchronized JavaProfilerConfig duplicateProfiler(String id) {
        JavaProfilerConfig original = getProfilerById(id);
        if (original != null) {
            JavaProfilerConfig clone = original.clone();
            clone.setId(java.util.UUID.randomUUID().toString());
            clone.setName(original.getName() + " (Copy)");
            int idx = -1;
            for (int i = 0; i < profilers.size(); i++) {
                if (profilers.get(i).getId().equals(id)) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0 && idx < profilers.size() - 1) {
                profilers.add(idx + 1, clone);
            } else {
                profilers.add(clone);
            }
            selectedProfilerId = clone.getId();
            saveSettings();
            notifyChanged();
            return clone;
        }
        return null;
    }

    public synchronized void removeProfiler(String id) {
        if (id != null && profilers.size() > 1) {
            int removeIdx = -1;
            for (int i = 0; i < profilers.size(); i++) {
                if (profilers.get(i).getId().equals(id)) {
                    removeIdx = i;
                    break;
                }
            }
            if (removeIdx >= 0) {
                profilers.remove(removeIdx);
                if (id.equals(selectedProfilerId)) {
                    int nextIdx = Math.min(removeIdx, profilers.size() - 1);
                    selectedProfilerId = profilers.get(nextIdx).getId();
                }
                saveSettings();
                notifyChanged();
            }
        }
    }

    public synchronized void moveProfilerUp(String id) {
        int idx = getIndexById(id);
        if (idx > 0) {
            Collections.swap(profilers, idx, idx - 1);
            saveSettings();
            notifyChanged();
        }
    }

    public synchronized void moveProfilerDown(String id) {
        int idx = getIndexById(id);
        if (idx >= 0 && idx < profilers.size() - 1) {
            Collections.swap(profilers, idx, idx + 1);
            saveSettings();
            notifyChanged();
        }
    }

    private int getIndexById(String id) {
        if (id == null) return -1;
        for (int i = 0; i < profilers.size(); i++) {
            if (profilers.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    // ============================================================
    // Filter Groups
    // ============================================================

    public synchronized List<ProfilerFilterGroup> getFilterGroups() {
        List<ProfilerFilterGroup> copy = new ArrayList<>();
        for (ProfilerFilterGroup g : filterGroups) {
            copy.add(g.clone());
        }
        return copy;
    }

    public synchronized void setFilterGroups(List<ProfilerFilterGroup> newGroups) {
        filterGroups.clear();
        if (newGroups != null) {
            for (ProfilerFilterGroup g : newGroups) {
                filterGroups.add(g.clone());
            }
        }
        saveSettings();
        notifyChanged();
    }

    public synchronized void addFilterGroup(ProfilerFilterGroup group) {
        if (group != null) {
            filterGroups.add(group.clone());
            saveSettings();
            notifyChanged();
        }
    }

    public synchronized void removeFilterGroup(int index) {
        if (index >= 0 && index < filterGroups.size()) {
            filterGroups.remove(index);
            saveSettings();
            notifyChanged();
        }
    }

    public synchronized void resetFilterGroupsToDefaults() {
        filterGroups.clear();
        filterGroups.addAll(createDefaultFilterGroups());
        saveSettings();
        notifyChanged();
    }

    /**
     * Test whether any filter group matches the specified class name.
     */
    public synchronized boolean matchesAnyFilter(String className) {
        for (ProfilerFilterGroup g : filterGroups) {
            if (g.matches(className)) {
                return true;
            }
        }
        return false;
    }

    // ============================================================
    // Default Presets
    // ============================================================

    public static JavaProfilerConfig createDefaultProfiler() {
        JavaProfilerConfig cfg = new JavaProfilerConfig();
        cfg.setName("IntelliJ Profiler");
        cfg.setAgentOptions("event=wall,interval=10ms,jfrsync=profile");
        cfg.setAgentPath("Bundled (Version: 4.1)");
        cfg.setCollectNativeCalls(false);
        return cfg;
    }

    public static List<ProfilerFilterGroup> createDefaultFilterGroups() {
        List<ProfilerFilterGroup> list = new ArrayList<>();
        list.add(new ProfilerFilterGroup("Java", "java.*,javax.*,sun.*,sunw.*,com.sun.*,org.omg.CORBA.*,org.omg.CosNaming.*,COM.rsa.*,jrockit.*,javafx.*,jdk.internal.*"));
        list.add(new ProfilerFilterGroup("Google", "com.google.*"));
        list.add(new ProfilerFilterGroup("Gradle", "org.gradle.*"));
        list.add(new ProfilerFilterGroup("Groovy", "org.codehaus.groovy.*,groovy.lang.*,groovyjarjarasm.*"));
        list.add(new ProfilerFilterGroup("Hibernate", "org.hibernate.*"));
        list.add(new ProfilerFilterGroup("IntelliJ IDEA", "com.intellij.rt.execution.*"));
        list.add(new ProfilerFilterGroup("JBoss", "org.jboss.*,org.jbossmq.*,org.enhydra.*,org.hsql.*"));
        list.add(new ProfilerFilterGroup("JRuby", "org.jruby.*,jline.*"));
        list.add(new ProfilerFilterGroup("Netty", "io.netty.*"));
        list.add(new ProfilerFilterGroup("OSGi", "org.osgi.*"));
        list.add(new ProfilerFilterGroup("SQL", "org.hsqldb.*,com.mysql.*,com.microsoft.sqlserver.*,org.postgresql.*"));
        list.add(new ProfilerFilterGroup("Tests", "junit.*,org.junit.*,org.testng.*,com.beust.*,org.spockframework.*"));
        list.add(new ProfilerFilterGroup("XML & JSON", "org.w3c.*,org.xml.*,org.jdom.*,org.json.*,org.dom4j.*,org.kxml*,org.codehaus.jackson.*,net.sf.json.*,com.fasterxml.*,com.ctc.wstx.*"));
        list.add(new ProfilerFilterGroup("Native & Others", "[unknown],_*,*::*"));
        return list;
    }

    // ============================================================
    // Persistence
    // ============================================================

    public synchronized void loadSettings() {
        try {
            String jsonConfigs = Settings.get(KEY_PROFILER_CONFIGS);
            if (jsonConfigs != null && !jsonConfigs.isBlank()) {
                Type type = new TypeToken<List<JavaProfilerConfig>>() {}.getType();
                List<JavaProfilerConfig> loaded = GSON.fromJson(jsonConfigs, type);
                profilers.clear();
                if (loaded != null) {
                    profilers.addAll(loaded);
                }
            }
        } catch (Exception ignored) {}

        if (profilers.isEmpty()) {
            profilers.add(createDefaultProfiler());
        }

        selectedProfilerId = Settings.get(KEY_PROFILER_SELECTED_ID);
        if (selectedProfilerId == null || getProfilerById(selectedProfilerId) == null) {
            selectedProfilerId = profilers.get(0).getId();
        }

        try {
            String jsonFilters = Settings.get(KEY_PROFILER_FILTER_GROUPS);
            if (jsonFilters != null && !jsonFilters.isBlank()) {
                Type type = new TypeToken<List<ProfilerFilterGroup>>() {}.getType();
                List<ProfilerFilterGroup> loaded = GSON.fromJson(jsonFilters, type);
                filterGroups.clear();
                if (loaded != null) {
                    filterGroups.addAll(loaded);
                }
            }
        } catch (Exception ignored) {}

        if (filterGroups.isEmpty()) {
            filterGroups.addAll(createDefaultFilterGroups());
        }
    }

    public synchronized void saveSettings() {
        try {
            Settings.put(KEY_PROFILER_CONFIGS, GSON.toJson(profilers));
            Settings.put(KEY_PROFILER_SELECTED_ID, selectedProfilerId);
            Settings.put(KEY_PROFILER_FILTER_GROUPS, GSON.toJson(filterGroups));
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
