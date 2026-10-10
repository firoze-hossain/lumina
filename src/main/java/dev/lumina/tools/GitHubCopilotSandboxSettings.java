package dev.lumina.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model holding settings for Tools > GitHub Copilot > Sandbox.
 */
public class GitHubCopilotSandboxSettings {

    private boolean enableLocalSandbox = false;

    // Filesystem
    private boolean filesystemIncludeWorkingDirectory = true;
    private boolean filesystemClearPolicyOnExit = true;
    private List<SandboxPathPermission> filesystemPermissions = new ArrayList<>();

    // Network
    private boolean networkAllowOutbound = true;
    private boolean networkAllowLocalNetwork = true;
    private List<SandboxHostAccess> networkHostAccess = new ArrayList<>();

    public GitHubCopilotSandboxSettings() {
    }

    public GitHubCopilotSandboxSettings(GitHubCopilotSandboxSettings other) {
        if (other != null) {
            this.enableLocalSandbox = other.enableLocalSandbox;
            this.filesystemIncludeWorkingDirectory = other.filesystemIncludeWorkingDirectory;
            this.filesystemClearPolicyOnExit = other.filesystemClearPolicyOnExit;
            this.filesystemPermissions = new ArrayList<>();
            for (SandboxPathPermission p : other.filesystemPermissions) {
                this.filesystemPermissions.add(p.clone());
            }
            this.networkAllowOutbound = other.networkAllowOutbound;
            this.networkAllowLocalNetwork = other.networkAllowLocalNetwork;
            this.networkHostAccess = new ArrayList<>();
            for (SandboxHostAccess h : other.networkHostAccess) {
                this.networkHostAccess.add(h.clone());
            }
        }
    }

    public boolean isEnableLocalSandbox() {
        return enableLocalSandbox;
    }

    public void setEnableLocalSandbox(boolean enableLocalSandbox) {
        this.enableLocalSandbox = enableLocalSandbox;
    }

    public boolean isFilesystemIncludeWorkingDirectory() {
        return filesystemIncludeWorkingDirectory;
    }

    public void setFilesystemIncludeWorkingDirectory(boolean filesystemIncludeWorkingDirectory) {
        this.filesystemIncludeWorkingDirectory = filesystemIncludeWorkingDirectory;
    }

    public boolean isFilesystemClearPolicyOnExit() {
        return filesystemClearPolicyOnExit;
    }

    public void setFilesystemClearPolicyOnExit(boolean filesystemClearPolicyOnExit) {
        this.filesystemClearPolicyOnExit = filesystemClearPolicyOnExit;
    }

    public List<SandboxPathPermission> getFilesystemPermissions() {
        return filesystemPermissions;
    }

    public void setFilesystemPermissions(List<SandboxPathPermission> filesystemPermissions) {
        this.filesystemPermissions = filesystemPermissions != null ? filesystemPermissions : new ArrayList<>();
    }

    public boolean isNetworkAllowOutbound() {
        return networkAllowOutbound;
    }

    public void setNetworkAllowOutbound(boolean networkAllowOutbound) {
        this.networkAllowOutbound = networkAllowOutbound;
    }

    public boolean isNetworkAllowLocalNetwork() {
        return networkAllowLocalNetwork;
    }

    public void setNetworkAllowLocalNetwork(boolean networkAllowLocalNetwork) {
        this.networkAllowLocalNetwork = networkAllowLocalNetwork;
    }

    public List<SandboxHostAccess> getNetworkHostAccess() {
        return networkHostAccess;
    }

    public void setNetworkHostAccess(List<SandboxHostAccess> networkHostAccess) {
        this.networkHostAccess = networkHostAccess != null ? networkHostAccess : new ArrayList<>();
    }

    public GitHubCopilotSandboxSettings clone() {
        return new GitHubCopilotSandboxSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GitHubCopilotSandboxSettings that = (GitHubCopilotSandboxSettings) o;
        return enableLocalSandbox == that.enableLocalSandbox &&
                filesystemIncludeWorkingDirectory == that.filesystemIncludeWorkingDirectory &&
                filesystemClearPolicyOnExit == that.filesystemClearPolicyOnExit &&
                networkAllowOutbound == that.networkAllowOutbound &&
                networkAllowLocalNetwork == that.networkAllowLocalNetwork &&
                Objects.equals(filesystemPermissions, that.filesystemPermissions) &&
                Objects.equals(networkHostAccess, that.networkHostAccess);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enableLocalSandbox, filesystemIncludeWorkingDirectory, filesystemClearPolicyOnExit,
                filesystemPermissions, networkAllowOutbound, networkAllowLocalNetwork, networkHostAccess);
    }

    @Override
    public String toString() {
        return "GitHubCopilotSandboxSettings{" +
                "enableLocalSandbox=" + enableLocalSandbox +
                ", filesystemIncludeWorkingDirectory=" + filesystemIncludeWorkingDirectory +
                ", filesystemClearPolicyOnExit=" + filesystemClearPolicyOnExit +
                ", filesystemPermissions=" + filesystemPermissions +
                ", networkAllowOutbound=" + networkAllowOutbound +
                ", networkAllowLocalNetwork=" + networkAllowLocalNetwork +
                ", networkHostAccess=" + networkHostAccess +
                '}';
    }
}
