package dev.lumina.build;

import dev.lumina.util.Settings;

import java.util.*;

/**
 * Singleton manager responsible for loading and saving Annotation Processors configuration.
 */
public class AnnotationProcessingSettingsManager {

    public static final String KEY_PROFILES_LIST = "compiler.annotation.processors.profiles";
    public static final String PREFIX_PROFILE = "compiler.annotation.profile.";

    private static volatile AnnotationProcessingSettingsManager instance;
    private AnnotationProcessingSettings currentSettings;

    private AnnotationProcessingSettingsManager() {
        loadSettings();
    }

    public static AnnotationProcessingSettingsManager getInstance() {
        if (instance == null) {
            synchronized (AnnotationProcessingSettingsManager.class) {
                if (instance == null) {
                    instance = new AnnotationProcessingSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized AnnotationProcessingSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(AnnotationProcessingSettings newSettings) {
        if (newSettings == null) return;
        this.currentSettings = newSettings.clone();
        saveSettings();
    }

    public synchronized void loadSettings() {
        AnnotationProcessingSettings s = new AnnotationProcessingSettings();
        String list = Settings.get(KEY_PROFILES_LIST);
        if (list != null && !list.isBlank()) {
            List<AnnotationProcessingProfile> loaded = new ArrayList<>();
            for (String profileName : list.split(";;")) {
                if (profileName.isBlank()) continue;
                String prefix = PREFIX_PROFILE + profileName + ".";
                AnnotationProcessingProfile p = new AnnotationProcessingProfile(profileName);

                String enabled = Settings.get(prefix + "enabled");
                if (enabled != null) p.setEnabled(Boolean.parseBoolean(enabled));

                String classpath = Settings.get(prefix + "classpath");
                if (classpath != null) p.setObtainFromClasspath(Boolean.parseBoolean(classpath));

                String path = Settings.get(prefix + "processorPath");
                if (path != null) p.setProcessorPath(path);

                String modulePath = Settings.get(prefix + "modulePath");
                if (modulePath != null) p.setUseProcessorModulePath(Boolean.parseBoolean(modulePath));

                String relative = Settings.get(prefix + "relativeToContentRoot");
                if (relative != null) p.setStoreRelativeToContentRoot(Boolean.parseBoolean(relative));

                String prodSrc = Settings.get(prefix + "prodSources");
                if (prodSrc != null) p.setProductionSourcesDirectory(prodSrc);

                String testSrc = Settings.get(prefix + "testSources");
                if (testSrc != null) p.setTestSourcesDirectory(testSrc);

                String separate = Settings.get(prefix + "runSeparate");
                if (separate != null) p.setRunInSeparateStep(Boolean.parseBoolean(separate));

                String mods = Settings.get(prefix + "modules");
                if (mods != null && !mods.isBlank()) {
                    p.setModules(new ArrayList<>(Arrays.asList(mods.split(","))));
                }

                loaded.add(p);
            }
            if (!loaded.isEmpty()) {
                s.setProfiles(loaded);
            }
        }
        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        StringBuilder sb = new StringBuilder();
        for (AnnotationProcessingProfile p : currentSettings.getProfiles()) {
            if (!sb.isEmpty()) sb.append(";;");
            sb.append(p.getName());

            String prefix = PREFIX_PROFILE + p.getName() + ".";
            Settings.set(prefix + "enabled", String.valueOf(p.isEnabled()));
            Settings.set(prefix + "classpath", String.valueOf(p.isObtainFromClasspath()));
            Settings.set(prefix + "processorPath", p.getProcessorPath());
            Settings.set(prefix + "modulePath", String.valueOf(p.isUseProcessorModulePath()));
            Settings.set(prefix + "relativeToContentRoot", String.valueOf(p.isStoreRelativeToContentRoot()));
            Settings.set(prefix + "prodSources", p.getProductionSourcesDirectory());
            Settings.set(prefix + "testSources", p.getTestSourcesDirectory());
            Settings.set(prefix + "runSeparate", String.valueOf(p.isRunInSeparateStep()));
            Settings.set(prefix + "modules", String.join(",", p.getModules()));
        }
        Settings.set(KEY_PROFILES_LIST, sb.toString());
    }
}
