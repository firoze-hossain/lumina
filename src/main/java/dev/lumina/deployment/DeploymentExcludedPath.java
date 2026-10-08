package dev.lumina.deployment;

import java.util.Objects;

/**
 * Excluded path definition for deployment sync.
 */
public class DeploymentExcludedPath implements Cloneable {

    private String localPath = "";
    private String deploymentPath = "";

    public DeploymentExcludedPath() {
    }

    public DeploymentExcludedPath(String localPath, String deploymentPath) {
        this.localPath = localPath != null ? localPath : "";
        this.deploymentPath = deploymentPath != null ? deploymentPath : "";
    }

    public DeploymentExcludedPath(DeploymentExcludedPath other) {
        if (other != null) {
            this.localPath = other.localPath;
            this.deploymentPath = other.deploymentPath;
        }
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath != null ? localPath : "";
    }

    public String getDeploymentPath() {
        return deploymentPath;
    }

    public void setDeploymentPath(String deploymentPath) {
        this.deploymentPath = deploymentPath != null ? deploymentPath : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeploymentExcludedPath that)) return false;
        return Objects.equals(localPath, that.localPath) &&
                Objects.equals(deploymentPath, that.deploymentPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localPath, deploymentPath);
    }

    @Override
    public DeploymentExcludedPath clone() {
        return new DeploymentExcludedPath(this);
    }
}
