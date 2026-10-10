package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing Tools > GitHub Copilot > General settings in Lumina IDE.
 */
public class GitHubCopilotGeneralSettings implements Cloneable {

    private boolean enableScreenReaderSupport = false;
    private String updateChannel = "Stable";
    private boolean checkForPluginUpdates = true;
    private boolean preferDeviceCodeSignIn = false;
    private String authenticationProvider = "";
    private boolean sendUsageTelemetry = true;

    public GitHubCopilotGeneralSettings() {
    }

    public boolean isEnableScreenReaderSupport() {
        return enableScreenReaderSupport;
    }

    public void setEnableScreenReaderSupport(boolean enableScreenReaderSupport) {
        this.enableScreenReaderSupport = enableScreenReaderSupport;
    }

    public String getUpdateChannel() {
        return updateChannel;
    }

    public void setUpdateChannel(String updateChannel) {
        this.updateChannel = updateChannel != null ? updateChannel : "Stable";
    }

    public boolean isCheckForPluginUpdates() {
        return checkForPluginUpdates;
    }

    public void setCheckForPluginUpdates(boolean checkForPluginUpdates) {
        this.checkForPluginUpdates = checkForPluginUpdates;
    }

    public boolean isPreferDeviceCodeSignIn() {
        return preferDeviceCodeSignIn;
    }

    public void setPreferDeviceCodeSignIn(boolean preferDeviceCodeSignIn) {
        this.preferDeviceCodeSignIn = preferDeviceCodeSignIn;
    }

    public String getAuthenticationProvider() {
        return authenticationProvider;
    }

    public void setAuthenticationProvider(String authenticationProvider) {
        this.authenticationProvider = authenticationProvider != null ? authenticationProvider : "";
    }

    public boolean isSendUsageTelemetry() {
        return sendUsageTelemetry;
    }

    public void setSendUsageTelemetry(boolean sendUsageTelemetry) {
        this.sendUsageTelemetry = sendUsageTelemetry;
    }

    @Override
    public GitHubCopilotGeneralSettings clone() {
        try {
            return (GitHubCopilotGeneralSettings) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GitHubCopilotGeneralSettings that = (GitHubCopilotGeneralSettings) o;
        return enableScreenReaderSupport == that.enableScreenReaderSupport &&
                checkForPluginUpdates == that.checkForPluginUpdates &&
                preferDeviceCodeSignIn == that.preferDeviceCodeSignIn &&
                sendUsageTelemetry == that.sendUsageTelemetry &&
                Objects.equals(updateChannel, that.updateChannel) &&
                Objects.equals(authenticationProvider, that.authenticationProvider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enableScreenReaderSupport, updateChannel, checkForPluginUpdates,
                preferDeviceCodeSignIn, authenticationProvider, sendUsageTelemetry);
    }

    @Override
    public String toString() {
        return "GitHubCopilotGeneralSettings{" +
                "enableScreenReaderSupport=" + enableScreenReaderSupport +
                ", updateChannel='" + updateChannel + '\'' +
                ", checkForPluginUpdates=" + checkForPluginUpdates +
                ", preferDeviceCodeSignIn=" + preferDeviceCodeSignIn +
                ", authenticationProvider='" + authenticationProvider + '\'' +
                ", sendUsageTelemetry=" + sendUsageTelemetry +
                '}';
    }
}
