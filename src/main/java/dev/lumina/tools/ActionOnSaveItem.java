package dev.lumina.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing an individual action item executed on file save.
 */
public class ActionOnSaveItem implements Cloneable {

    private String id;
    private String title;
    private boolean enabled;
    private String activatedOn;
    private String subtext;
    private boolean warning;
    private String configureTarget;

    private List<String> scopeOptions = new ArrayList<>();
    private String selectedScope;

    private List<String> modeOptions = new ArrayList<>();
    private String selectedMode;

    private List<String> profileOptions = new ArrayList<>();
    private String selectedProfile;

    public ActionOnSaveItem() {
        this.activatedOn = "Any save";
    }

    public ActionOnSaveItem(String id, String title, boolean enabled, String activatedOn) {
        this.id = id;
        this.title = title;
        this.enabled = enabled;
        this.activatedOn = activatedOn != null ? activatedOn : "Any save";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getActivatedOn() {
        return activatedOn;
    }

    public void setActivatedOn(String activatedOn) {
        this.activatedOn = activatedOn;
    }

    public String getSubtext() {
        return subtext;
    }

    public void setSubtext(String subtext) {
        this.subtext = subtext;
    }

    public boolean isWarning() {
        return warning;
    }

    public void setWarning(boolean warning) {
        this.warning = warning;
    }

    public String getConfigureTarget() {
        return configureTarget;
    }

    public void setConfigureTarget(String configureTarget) {
        this.configureTarget = configureTarget;
    }

    public List<String> getScopeOptions() {
        return scopeOptions;
    }

    public void setScopeOptions(List<String> scopeOptions) {
        this.scopeOptions = scopeOptions != null ? new ArrayList<>(scopeOptions) : new ArrayList<>();
    }

    public String getSelectedScope() {
        return selectedScope;
    }

    public void setSelectedScope(String selectedScope) {
        this.selectedScope = selectedScope;
    }

    public List<String> getModeOptions() {
        return modeOptions;
    }

    public void setModeOptions(List<String> modeOptions) {
        this.modeOptions = modeOptions != null ? new ArrayList<>(modeOptions) : new ArrayList<>();
    }

    public String getSelectedMode() {
        return selectedMode;
    }

    public void setSelectedMode(String selectedMode) {
        this.selectedMode = selectedMode;
    }

    public List<String> getProfileOptions() {
        return profileOptions;
    }

    public void setProfileOptions(List<String> profileOptions) {
        this.profileOptions = profileOptions != null ? new ArrayList<>(profileOptions) : new ArrayList<>();
    }

    public String getSelectedProfile() {
        return selectedProfile;
    }

    public void setSelectedProfile(String selectedProfile) {
        this.selectedProfile = selectedProfile;
    }

    @Override
    public ActionOnSaveItem clone() {
        try {
            ActionOnSaveItem copy = (ActionOnSaveItem) super.clone();
            copy.scopeOptions = new ArrayList<>(this.scopeOptions);
            copy.modeOptions = new ArrayList<>(this.modeOptions);
            copy.profileOptions = new ArrayList<>(this.profileOptions);
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionOnSaveItem that = (ActionOnSaveItem) o;
        return enabled == that.enabled &&
                warning == that.warning &&
                Objects.equals(id, that.id) &&
                Objects.equals(title, that.title) &&
                Objects.equals(activatedOn, that.activatedOn) &&
                Objects.equals(subtext, that.subtext) &&
                Objects.equals(configureTarget, that.configureTarget) &&
                Objects.equals(scopeOptions, that.scopeOptions) &&
                Objects.equals(selectedScope, that.selectedScope) &&
                Objects.equals(modeOptions, that.modeOptions) &&
                Objects.equals(selectedMode, that.selectedMode) &&
                Objects.equals(profileOptions, that.profileOptions) &&
                Objects.equals(selectedProfile, that.selectedProfile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, enabled, activatedOn, subtext, warning, configureTarget,
                scopeOptions, selectedScope, modeOptions, selectedMode, profileOptions, selectedProfile);
    }
}
