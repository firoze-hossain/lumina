package dev.lumina.php;

import java.util.Objects;

/**
 * Model representing PHP Composer configuration settings in Lumina IDE (Languages & Frameworks > PHP > Composer).
 * Faithfully matches Image 4.
 */
public class PhpComposerSettings {

    private String pathToComposerJson = "";
    private boolean addPackagesAsLibraries = true;
    private boolean synchronizeIdeSettings = true;
    private boolean checkForAvailablePackageUpdates = true;
    private boolean showComposerJsonTopPanel = true;
    private boolean notifyAboutMissingVendor = true;
    private boolean runWithIgnorePlatformReqs = false;
    private String executionMode = "executable"; // "executable" or "phar"
    private String composerExecutablePath = "composer";
    private String composerPharPath = "";

    public PhpComposerSettings() {
    }

    public PhpComposerSettings(PhpComposerSettings other) {
        if (other == null) return;
        this.pathToComposerJson = other.pathToComposerJson;
        this.addPackagesAsLibraries = other.addPackagesAsLibraries;
        this.synchronizeIdeSettings = other.synchronizeIdeSettings;
        this.checkForAvailablePackageUpdates = other.checkForAvailablePackageUpdates;
        this.showComposerJsonTopPanel = other.showComposerJsonTopPanel;
        this.notifyAboutMissingVendor = other.notifyAboutMissingVendor;
        this.runWithIgnorePlatformReqs = other.runWithIgnorePlatformReqs;
        this.executionMode = other.executionMode;
        this.composerExecutablePath = other.composerExecutablePath;
        this.composerPharPath = other.composerPharPath;
    }

    public PhpComposerSettings copy() {
        return new PhpComposerSettings(this);
    }

    public String getPathToComposerJson() {
        return pathToComposerJson;
    }

    public void setPathToComposerJson(String pathToComposerJson) {
        this.pathToComposerJson = pathToComposerJson != null ? pathToComposerJson : "";
    }

    public boolean isAddPackagesAsLibraries() {
        return addPackagesAsLibraries;
    }

    public void setAddPackagesAsLibraries(boolean addPackagesAsLibraries) {
        this.addPackagesAsLibraries = addPackagesAsLibraries;
    }

    public boolean isSynchronizeIdeSettings() {
        return synchronizeIdeSettings;
    }

    public void setSynchronizeIdeSettings(boolean synchronizeIdeSettings) {
        this.synchronizeIdeSettings = synchronizeIdeSettings;
    }

    public boolean isCheckForAvailablePackageUpdates() {
        return checkForAvailablePackageUpdates;
    }

    public void setCheckForAvailablePackageUpdates(boolean checkForAvailablePackageUpdates) {
        this.checkForAvailablePackageUpdates = checkForAvailablePackageUpdates;
    }

    public boolean isShowComposerJsonTopPanel() {
        return showComposerJsonTopPanel;
    }

    public void setShowComposerJsonTopPanel(boolean showComposerJsonTopPanel) {
        this.showComposerJsonTopPanel = showComposerJsonTopPanel;
    }

    public boolean isNotifyAboutMissingVendor() {
        return notifyAboutMissingVendor;
    }

    public void setNotifyAboutMissingVendor(boolean notifyAboutMissingVendor) {
        this.notifyAboutMissingVendor = notifyAboutMissingVendor;
    }

    public boolean isRunWithIgnorePlatformReqs() {
        return runWithIgnorePlatformReqs;
    }

    public void setRunWithIgnorePlatformReqs(boolean runWithIgnorePlatformReqs) {
        this.runWithIgnorePlatformReqs = runWithIgnorePlatformReqs;
    }

    public String getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(String executionMode) {
        this.executionMode = executionMode != null ? executionMode : "executable";
    }

    public String getComposerExecutablePath() {
        return composerExecutablePath;
    }

    public void setComposerExecutablePath(String composerExecutablePath) {
        this.composerExecutablePath = composerExecutablePath != null ? composerExecutablePath : "composer";
    }

    public String getComposerPharPath() {
        return composerPharPath;
    }

    public void setComposerPharPath(String composerPharPath) {
        this.composerPharPath = composerPharPath != null ? composerPharPath : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhpComposerSettings that = (PhpComposerSettings) o;
        return addPackagesAsLibraries == that.addPackagesAsLibraries &&
                synchronizeIdeSettings == that.synchronizeIdeSettings &&
                checkForAvailablePackageUpdates == that.checkForAvailablePackageUpdates &&
                showComposerJsonTopPanel == that.showComposerJsonTopPanel &&
                notifyAboutMissingVendor == that.notifyAboutMissingVendor &&
                runWithIgnorePlatformReqs == that.runWithIgnorePlatformReqs &&
                Objects.equals(pathToComposerJson, that.pathToComposerJson) &&
                Objects.equals(executionMode, that.executionMode) &&
                Objects.equals(composerExecutablePath, that.composerExecutablePath) &&
                Objects.equals(composerPharPath, that.composerPharPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pathToComposerJson, addPackagesAsLibraries, synchronizeIdeSettings,
                checkForAvailablePackageUpdates, showComposerJsonTopPanel, notifyAboutMissingVendor,
                runWithIgnorePlatformReqs, executionMode, composerExecutablePath, composerPharPath);
    }
}
