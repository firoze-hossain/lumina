package dev.lumina.deployment;

import java.util.Objects;

/**
 * Mapping between local paths, deployment paths on server, and web URL paths.
 */
public class DeploymentMapping implements Cloneable {

    private String localPath = "";
    private String deploymentPath = "/";
    private String webPath = "/";

    public DeploymentMapping() {
    }

    public DeploymentMapping(String localPath, String deploymentPath, String webPath) {
        this.localPath = localPath != null ? localPath : "";
        this.deploymentPath = deploymentPath != null ? deploymentPath : "/";
        this.webPath = webPath != null ? webPath : "/";
    }

    public DeploymentMapping(DeploymentMapping other) {
        if (other != null) {
            this.localPath = other.localPath;
            this.deploymentPath = other.deploymentPath;
            this.webPath = other.webPath;
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
        this.deploymentPath = deploymentPath != null ? deploymentPath : "/";
    }

    public String getWebPath() {
        return webPath;
    }

    public void setWebPath(String webPath) {
        this.webPath = webPath != null ? webPath : "/";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeploymentMapping that)) return false;
        return Objects.equals(localPath, that.localPath) &&
                Objects.equals(deploymentPath, that.deploymentPath) &&
                Objects.equals(webPath, that.webPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localPath, deploymentPath, webPath);
    }

    @Override
    public DeploymentMapping clone() {
        return new DeploymentMapping(this);
    }
}
