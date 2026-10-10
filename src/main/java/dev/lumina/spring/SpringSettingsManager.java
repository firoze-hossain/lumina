package dev.lumina.spring;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Languages & Frameworks > Spring settings in Lumina IDE.
 */
public class SpringSettingsManager {

    public static final String KEY_SHOW_PROFILES_PANEL = "spring.config.editor.show.profiles";
    public static final String KEY_SHOW_MULTIPLE_CONTEXTS_PANEL = "spring.config.editor.show.multiple.contexts";
    public static final String KEY_SMART_BEANS_COMPLETION = "spring.completion.smart.beans";
    public static final String KEY_REFORMAT_CODE_ON_CREATE = "spring.initializr.reformat.code";
    public static final String KEY_DEFAULT_BEAN_INJECTOR_STRATEGY = "spring.autowiring.injector.strategy";
    public static final String KEY_METHOD_PARAM_INJECTION = "spring.autowiring.method.parameter.injection";
    public static final String KEY_REFRESH_HEALTH_ACTUATOR = "spring.boot.actuator.refresh.health";
    public static final String KEY_ACTUATOR_REFRESH_INTERVAL = "spring.boot.actuator.refresh.interval.sec";
    public static final String KEY_CREATE_RUN_CONFIG_AUTOMATICALLY = "spring.project.import.create.run.config";

    private static volatile SpringSettingsManager instance;
    private SpringSettings currentSettings;
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private SpringSettingsManager() {
        loadSettings();
    }

    public static SpringSettingsManager getInstance() {
        if (instance == null) {
            synchronized (SpringSettingsManager.class) {
                if (instance == null) {
                    instance = new SpringSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized SpringSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(SpringSettings settings) {
        if (settings == null) return;
        this.currentSettings = settings.clone();
        saveSettings();
        notifyListeners();
    }

    public synchronized void loadSettings() {
        SpringSettings s = new SpringSettings();

        String profiles = Settings.get(KEY_SHOW_PROFILES_PANEL);
        if (profiles != null) s.setShowProfilesPanel(Boolean.parseBoolean(profiles));

        String contexts = Settings.get(KEY_SHOW_MULTIPLE_CONTEXTS_PANEL);
        if (contexts != null) s.setShowMultipleContextsPanel(Boolean.parseBoolean(contexts));

        String smartBeans = Settings.get(KEY_SMART_BEANS_COMPLETION);
        if (smartBeans != null) s.setSmartBeansCompletion(Boolean.parseBoolean(smartBeans));

        String reformat = Settings.get(KEY_REFORMAT_CODE_ON_CREATE);
        if (reformat != null) s.setReformatCodeWhenCreatingProject(Boolean.parseBoolean(reformat));

        String strategy = Settings.get(KEY_DEFAULT_BEAN_INJECTOR_STRATEGY);
        if (strategy != null) {
            try {
                s.setDefaultBeanInjectorStrategy(SpringSettings.BeanInjectorStrategy.valueOf(strategy));
            } catch (IllegalArgumentException ignored) {}
        }

        String methodParam = Settings.get(KEY_METHOD_PARAM_INJECTION);
        if (methodParam != null) s.setUseMethodParameterInjectionForBeanMethods(Boolean.parseBoolean(methodParam));

        String actuatorHealth = Settings.get(KEY_REFRESH_HEALTH_ACTUATOR);
        if (actuatorHealth != null) s.setRefreshHealthInActuatorTab(Boolean.parseBoolean(actuatorHealth));

        String interval = Settings.get(KEY_ACTUATOR_REFRESH_INTERVAL);
        if (interval != null) {
            try {
                s.setActuatorRefreshIntervalSeconds(Integer.parseInt(interval));
            } catch (NumberFormatException ignored) {}
        }

        String runConfig = Settings.get(KEY_CREATE_RUN_CONFIG_AUTOMATICALLY);
        if (runConfig != null) s.setCreateRunConfigurationAutomatically(Boolean.parseBoolean(runConfig));

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.put(KEY_SHOW_PROFILES_PANEL, String.valueOf(currentSettings.isShowProfilesPanel()));
        Settings.put(KEY_SHOW_MULTIPLE_CONTEXTS_PANEL, String.valueOf(currentSettings.isShowMultipleContextsPanel()));
        Settings.put(KEY_SMART_BEANS_COMPLETION, String.valueOf(currentSettings.isSmartBeansCompletion()));
        Settings.put(KEY_REFORMAT_CODE_ON_CREATE, String.valueOf(currentSettings.isReformatCodeWhenCreatingProject()));
        Settings.put(KEY_DEFAULT_BEAN_INJECTOR_STRATEGY, currentSettings.getDefaultBeanInjectorStrategy().name());
        Settings.put(KEY_METHOD_PARAM_INJECTION, String.valueOf(currentSettings.isUseMethodParameterInjectionForBeanMethods()));
        Settings.put(KEY_REFRESH_HEALTH_ACTUATOR, String.valueOf(currentSettings.isRefreshHealthInActuatorTab()));
        Settings.put(KEY_ACTUATOR_REFRESH_INTERVAL, String.valueOf(currentSettings.getActuatorRefreshIntervalSeconds()));
        Settings.put(KEY_CREATE_RUN_CONFIG_AUTOMATICALLY, String.valueOf(currentSettings.isCreateRunConfigurationAutomatically()));
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable r : changeListeners) {
            try {
                r.run();
            } catch (Exception ignored) {}
        }
    }
}
