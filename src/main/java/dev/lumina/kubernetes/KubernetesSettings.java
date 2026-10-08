package dev.lumina.kubernetes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Encapsulates all Kubernetes settings for Lumina IDE matching IntelliJ IDEA 1:1.
 */
public class KubernetesSettings implements Cloneable {

    // 1. Tool locations (global settings)
    private String kubectlPath = "kubectl";
    private String helmPath = "helm";

    // 2. Configuration
    private List<KubernetesConfigFile> configFiles = new ArrayList<>();
    private boolean reloadConfigAutomatically = true;
    private boolean refreshClusterResources = true;

    // 3. Pod Shell
    private String shellCommand = "/bin/sh";
    private boolean shellCommandGlobal = true;

    // 4. Appearance
    private String floatingToolbarMode = "Always Show"; // "Always Show", "Never Show", "Auto"

    // 5. Logs (global settings)
    private String downloadLogsMode = "SCRATCHES"; // "SCRATCHES", "ASK"
    private String customDownloadPath = "Download to Scratches";
    private boolean appendTimestampToLogFileName = false;
    private boolean dropAnsiSymbols = false;
    private boolean displayTimestamp = true;
    private boolean displaySource = true;
    private boolean displayMessage = true;
    private String clusterEventsPresentationMode = "In editor"; // "In editor", "In tool window"
    private int logCacheSizeMb = 300;
    private int logEditorUpdateDelayMs = 250;

    // 6. Filters
    private List<KubernetesLogFilter> logFilters = new ArrayList<>();

    // 7. Namespaces (applied when they cannot be loaded from the cluster)
    private List<KubernetesNamespaceItem> namespaces = new ArrayList<>();

    // 8. Kubectl Custom Arguments
    private boolean appendServerPathFlag = true;
    private List<KubernetesCustomArg> customArgs = new ArrayList<>();

    // 9. Ephemeral Debug Containers
    private List<KubernetesEphemeralContainer> ephemeralContainers = new ArrayList<>();

    public KubernetesSettings() {
        initDefaultFilters();
    }

    public void initDefaultFilters() {
        logFilters.clear();
        logFilters.add(new KubernetesLogFilter(true, "(?i)\\b(e(rr(or)?)?|severe)\\b", true, false, "#E05555"));
        logFilters.add(new KubernetesLogFilter(true, "(?i)\\b(w(arn(ing)?)?)\\b", false, false, "#E5A84B"));
        logFilters.add(new KubernetesLogFilter(false, "(?i)\\b(i(nfo)?)\\b", false, false, "#59A869"));
    }

    // Getters and Setters

    public String getKubectlPath() {
        return kubectlPath;
    }

    public void setKubectlPath(String kubectlPath) {
        this.kubectlPath = kubectlPath;
    }

    public String getHelmPath() {
        return helmPath;
    }

    public void setHelmPath(String helmPath) {
        this.helmPath = helmPath;
    }

    public List<KubernetesConfigFile> getConfigFiles() {
        return configFiles;
    }

    public void setConfigFiles(List<KubernetesConfigFile> configFiles) {
        this.configFiles = configFiles != null ? configFiles : new ArrayList<>();
    }

    public boolean isReloadConfigAutomatically() {
        return reloadConfigAutomatically;
    }

    public void setReloadConfigAutomatically(boolean reloadConfigAutomatically) {
        this.reloadConfigAutomatically = reloadConfigAutomatically;
    }

    public boolean isRefreshClusterResources() {
        return refreshClusterResources;
    }

    public void setRefreshClusterResources(boolean refreshClusterResources) {
        this.refreshClusterResources = refreshClusterResources;
    }

    public String getShellCommand() {
        return shellCommand;
    }

    public void setShellCommand(String shellCommand) {
        this.shellCommand = shellCommand;
    }

    public boolean isShellCommandGlobal() {
        return shellCommandGlobal;
    }

    public void setShellCommandGlobal(boolean shellCommandGlobal) {
        this.shellCommandGlobal = shellCommandGlobal;
    }

    public String getFloatingToolbarMode() {
        return floatingToolbarMode;
    }

    public void setFloatingToolbarMode(String floatingToolbarMode) {
        this.floatingToolbarMode = floatingToolbarMode;
    }

    public String getDownloadLogsMode() {
        return downloadLogsMode;
    }

    public void setDownloadLogsMode(String downloadLogsMode) {
        this.downloadLogsMode = downloadLogsMode;
    }

    public String getCustomDownloadPath() {
        return customDownloadPath;
    }

    public void setCustomDownloadPath(String customDownloadPath) {
        this.customDownloadPath = customDownloadPath;
    }

    public boolean isAppendTimestampToLogFileName() {
        return appendTimestampToLogFileName;
    }

    public void setAppendTimestampToLogFileName(boolean appendTimestampToLogFileName) {
        this.appendTimestampToLogFileName = appendTimestampToLogFileName;
    }

    public boolean isDropAnsiSymbols() {
        return dropAnsiSymbols;
    }

    public void setDropAnsiSymbols(boolean dropAnsiSymbols) {
        this.dropAnsiSymbols = dropAnsiSymbols;
    }

    public boolean isDisplayTimestamp() {
        return displayTimestamp;
    }

    public void setDisplayTimestamp(boolean displayTimestamp) {
        this.displayTimestamp = displayTimestamp;
    }

    public boolean isDisplaySource() {
        return displaySource;
    }

    public void setDisplaySource(boolean displaySource) {
        this.displaySource = displaySource;
    }

    public boolean isDisplayMessage() {
        return displayMessage;
    }

    public void setDisplayMessage(boolean displayMessage) {
        this.displayMessage = displayMessage;
    }

    public String getClusterEventsPresentationMode() {
        return clusterEventsPresentationMode;
    }

    public void setClusterEventsPresentationMode(String clusterEventsPresentationMode) {
        this.clusterEventsPresentationMode = clusterEventsPresentationMode;
    }

    public int getLogCacheSizeMb() {
        return logCacheSizeMb;
    }

    public void setLogCacheSizeMb(int logCacheSizeMb) {
        this.logCacheSizeMb = logCacheSizeMb;
    }

    public int getLogEditorUpdateDelayMs() {
        return logEditorUpdateDelayMs;
    }

    public void setLogEditorUpdateDelayMs(int logEditorUpdateDelayMs) {
        this.logEditorUpdateDelayMs = logEditorUpdateDelayMs;
    }

    public List<KubernetesLogFilter> getLogFilters() {
        return logFilters;
    }

    public void setLogFilters(List<KubernetesLogFilter> logFilters) {
        this.logFilters = logFilters != null ? logFilters : new ArrayList<>();
    }

    public List<KubernetesNamespaceItem> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(List<KubernetesNamespaceItem> namespaces) {
        this.namespaces = namespaces != null ? namespaces : new ArrayList<>();
    }

    public boolean isAppendServerPathFlag() {
        return appendServerPathFlag;
    }

    public void setAppendServerPathFlag(boolean appendServerPathFlag) {
        this.appendServerPathFlag = appendServerPathFlag;
    }

    public List<KubernetesCustomArg> getCustomArgs() {
        return customArgs;
    }

    public void setCustomArgs(List<KubernetesCustomArg> customArgs) {
        this.customArgs = customArgs != null ? customArgs : new ArrayList<>();
    }

    public List<KubernetesEphemeralContainer> getEphemeralContainers() {
        return ephemeralContainers;
    }

    public void setEphemeralContainers(List<KubernetesEphemeralContainer> ephemeralContainers) {
        this.ephemeralContainers = ephemeralContainers != null ? ephemeralContainers : new ArrayList<>();
    }

    @Override
    public KubernetesSettings clone() {
        KubernetesSettings copy = new KubernetesSettings();
        copy.kubectlPath = this.kubectlPath;
        copy.helmPath = this.helmPath;
        copy.configFiles = new ArrayList<>();
        for (KubernetesConfigFile f : this.configFiles) copy.configFiles.add(f.clone());
        copy.reloadConfigAutomatically = this.reloadConfigAutomatically;
        copy.refreshClusterResources = this.refreshClusterResources;
        copy.shellCommand = this.shellCommand;
        copy.shellCommandGlobal = this.shellCommandGlobal;
        copy.floatingToolbarMode = this.floatingToolbarMode;
        copy.downloadLogsMode = this.downloadLogsMode;
        copy.customDownloadPath = this.customDownloadPath;
        copy.appendTimestampToLogFileName = this.appendTimestampToLogFileName;
        copy.dropAnsiSymbols = this.dropAnsiSymbols;
        copy.displayTimestamp = this.displayTimestamp;
        copy.displaySource = this.displaySource;
        copy.displayMessage = this.displayMessage;
        copy.clusterEventsPresentationMode = this.clusterEventsPresentationMode;
        copy.logCacheSizeMb = this.logCacheSizeMb;
        copy.logEditorUpdateDelayMs = this.logEditorUpdateDelayMs;
        copy.logFilters = new ArrayList<>();
        for (KubernetesLogFilter f : this.logFilters) copy.logFilters.add(f.clone());
        copy.namespaces = new ArrayList<>();
        for (KubernetesNamespaceItem n : this.namespaces) copy.namespaces.add(n.clone());
        copy.appendServerPathFlag = this.appendServerPathFlag;
        copy.customArgs = new ArrayList<>();
        for (KubernetesCustomArg a : this.customArgs) copy.customArgs.add(a.clone());
        copy.ephemeralContainers = new ArrayList<>();
        for (KubernetesEphemeralContainer c : this.ephemeralContainers) copy.ephemeralContainers.add(c.clone());
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KubernetesSettings that = (KubernetesSettings) o;
        return reloadConfigAutomatically == that.reloadConfigAutomatically &&
                refreshClusterResources == that.refreshClusterResources &&
                shellCommandGlobal == that.shellCommandGlobal &&
                appendTimestampToLogFileName == that.appendTimestampToLogFileName &&
                dropAnsiSymbols == that.dropAnsiSymbols &&
                displayTimestamp == that.displayTimestamp &&
                displaySource == that.displaySource &&
                displayMessage == that.displayMessage &&
                logCacheSizeMb == that.logCacheSizeMb &&
                logEditorUpdateDelayMs == that.logEditorUpdateDelayMs &&
                appendServerPathFlag == that.appendServerPathFlag &&
                Objects.equals(kubectlPath, that.kubectlPath) &&
                Objects.equals(helmPath, that.helmPath) &&
                Objects.equals(configFiles, that.configFiles) &&
                Objects.equals(shellCommand, that.shellCommand) &&
                Objects.equals(floatingToolbarMode, that.floatingToolbarMode) &&
                Objects.equals(downloadLogsMode, that.downloadLogsMode) &&
                Objects.equals(customDownloadPath, that.customDownloadPath) &&
                Objects.equals(clusterEventsPresentationMode, that.clusterEventsPresentationMode) &&
                Objects.equals(logFilters, that.logFilters) &&
                Objects.equals(namespaces, that.namespaces) &&
                Objects.equals(customArgs, that.customArgs) &&
                Objects.equals(ephemeralContainers, that.ephemeralContainers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kubectlPath, helmPath, configFiles, reloadConfigAutomatically, refreshClusterResources,
                shellCommand, shellCommandGlobal, floatingToolbarMode, downloadLogsMode, customDownloadPath,
                appendTimestampToLogFileName, dropAnsiSymbols, displayTimestamp, displaySource, displayMessage,
                clusterEventsPresentationMode, logCacheSizeMb, logEditorUpdateDelayMs, logFilters, namespaces,
                appendServerPathFlag, customArgs, ephemeralContainers);
    }
}
