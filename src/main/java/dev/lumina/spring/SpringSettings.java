package dev.lumina.spring;

import java.util.Objects;

/**
 * Model for Languages & Frameworks > Spring settings in Lumina IDE.
 * Matches reference screenshot media_1791600365242_13139971.png:
 *  - Configuration Files Editor:
 *    - Show Profiles panel (default: true)
 *    - Show Multiple Contexts panel (default: true)
 *  - Completion:
 *    - Smart beans completion (default: true)
 *  - Spring Initializr:
 *    - Reformat code when creating a new project (default: true)
 *  - Autowiring Code Style:
 *    - Default bean injector strategy: Constructor (default) / Field / Setter
 *    - Use method parameter injection for @Bean methods (default: true)
 *  - Spring Boot:
 *    - Refresh health in the Actuator tab every [15] sec. (default: true, 15s)
 *  - Project Import:
 *    - Create run configuration automatically (default: true)
 */
public class SpringSettings implements Cloneable {

    public enum BeanInjectorStrategy {
        CONSTRUCTOR("Constructor"),
        FIELD("Field"),
        SETTER("Setter");

        private final String label;

        BeanInjectorStrategy(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private boolean showProfilesPanel = true;
    private boolean showMultipleContextsPanel = true;
    private boolean smartBeansCompletion = true;
    private boolean reformatCodeWhenCreatingProject = true;
    private BeanInjectorStrategy defaultBeanInjectorStrategy = BeanInjectorStrategy.CONSTRUCTOR;
    private boolean useMethodParameterInjectionForBeanMethods = true;
    private boolean refreshHealthInActuatorTab = true;
    private int actuatorRefreshIntervalSeconds = 15;
    private boolean createRunConfigurationAutomatically = true;

    public SpringSettings() {
    }

    public SpringSettings(SpringSettings other) {
        if (other != null) {
            this.showProfilesPanel = other.showProfilesPanel;
            this.showMultipleContextsPanel = other.showMultipleContextsPanel;
            this.smartBeansCompletion = other.smartBeansCompletion;
            this.reformatCodeWhenCreatingProject = other.reformatCodeWhenCreatingProject;
            this.defaultBeanInjectorStrategy = other.defaultBeanInjectorStrategy != null ?
                    other.defaultBeanInjectorStrategy : BeanInjectorStrategy.CONSTRUCTOR;
            this.useMethodParameterInjectionForBeanMethods = other.useMethodParameterInjectionForBeanMethods;
            this.refreshHealthInActuatorTab = other.refreshHealthInActuatorTab;
            this.actuatorRefreshIntervalSeconds = other.actuatorRefreshIntervalSeconds;
            this.createRunConfigurationAutomatically = other.createRunConfigurationAutomatically;
        }
    }

    public boolean isShowProfilesPanel() {
        return showProfilesPanel;
    }

    public void setShowProfilesPanel(boolean showProfilesPanel) {
        this.showProfilesPanel = showProfilesPanel;
    }

    public boolean isShowMultipleContextsPanel() {
        return showMultipleContextsPanel;
    }

    public void setShowMultipleContextsPanel(boolean showMultipleContextsPanel) {
        this.showMultipleContextsPanel = showMultipleContextsPanel;
    }

    public boolean isSmartBeansCompletion() {
        return smartBeansCompletion;
    }

    public void setSmartBeansCompletion(boolean smartBeansCompletion) {
        this.smartBeansCompletion = smartBeansCompletion;
    }

    public boolean isReformatCodeWhenCreatingProject() {
        return reformatCodeWhenCreatingProject;
    }

    public void setReformatCodeWhenCreatingProject(boolean reformatCodeWhenCreatingProject) {
        this.reformatCodeWhenCreatingProject = reformatCodeWhenCreatingProject;
    }

    public BeanInjectorStrategy getDefaultBeanInjectorStrategy() {
        return defaultBeanInjectorStrategy;
    }

    public void setDefaultBeanInjectorStrategy(BeanInjectorStrategy defaultBeanInjectorStrategy) {
        this.defaultBeanInjectorStrategy = defaultBeanInjectorStrategy != null ?
                defaultBeanInjectorStrategy : BeanInjectorStrategy.CONSTRUCTOR;
    }

    public boolean isUseMethodParameterInjectionForBeanMethods() {
        return useMethodParameterInjectionForBeanMethods;
    }

    public void setUseMethodParameterInjectionForBeanMethods(boolean useMethodParameterInjectionForBeanMethods) {
        this.useMethodParameterInjectionForBeanMethods = useMethodParameterInjectionForBeanMethods;
    }

    public boolean isRefreshHealthInActuatorTab() {
        return refreshHealthInActuatorTab;
    }

    public void setRefreshHealthInActuatorTab(boolean refreshHealthInActuatorTab) {
        this.refreshHealthInActuatorTab = refreshHealthInActuatorTab;
    }

    public int getActuatorRefreshIntervalSeconds() {
        return actuatorRefreshIntervalSeconds;
    }

    public void setActuatorRefreshIntervalSeconds(int actuatorRefreshIntervalSeconds) {
        this.actuatorRefreshIntervalSeconds = actuatorRefreshIntervalSeconds > 0 ? actuatorRefreshIntervalSeconds : 15;
    }

    public boolean isCreateRunConfigurationAutomatically() {
        return createRunConfigurationAutomatically;
    }

    public void setCreateRunConfigurationAutomatically(boolean createRunConfigurationAutomatically) {
        this.createRunConfigurationAutomatically = createRunConfigurationAutomatically;
    }

    @Override
    public SpringSettings clone() {
        return new SpringSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpringSettings that = (SpringSettings) o;
        return showProfilesPanel == that.showProfilesPanel &&
                showMultipleContextsPanel == that.showMultipleContextsPanel &&
                smartBeansCompletion == that.smartBeansCompletion &&
                reformatCodeWhenCreatingProject == that.reformatCodeWhenCreatingProject &&
                useMethodParameterInjectionForBeanMethods == that.useMethodParameterInjectionForBeanMethods &&
                refreshHealthInActuatorTab == that.refreshHealthInActuatorTab &&
                actuatorRefreshIntervalSeconds == that.actuatorRefreshIntervalSeconds &&
                createRunConfigurationAutomatically == that.createRunConfigurationAutomatically &&
                defaultBeanInjectorStrategy == that.defaultBeanInjectorStrategy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(showProfilesPanel, showMultipleContextsPanel, smartBeansCompletion,
                reformatCodeWhenCreatingProject, defaultBeanInjectorStrategy,
                useMethodParameterInjectionForBeanMethods, refreshHealthInActuatorTab,
                actuatorRefreshIntervalSeconds, createRunConfigurationAutomatically);
    }

    @Override
    public String toString() {
        return "SpringSettings{" +
                "showProfilesPanel=" + showProfilesPanel +
                ", showMultipleContextsPanel=" + showMultipleContextsPanel +
                ", smartBeansCompletion=" + smartBeansCompletion +
                ", reformatCodeWhenCreatingProject=" + reformatCodeWhenCreatingProject +
                ", defaultBeanInjectorStrategy=" + defaultBeanInjectorStrategy +
                ", useMethodParameterInjectionForBeanMethods=" + useMethodParameterInjectionForBeanMethods +
                ", refreshHealthInActuatorTab=" + refreshHealthInActuatorTab +
                ", actuatorRefreshIntervalSeconds=" + actuatorRefreshIntervalSeconds +
                ", createRunConfigurationAutomatically=" + createRunConfigurationAutomatically +
                '}';
    }
}
