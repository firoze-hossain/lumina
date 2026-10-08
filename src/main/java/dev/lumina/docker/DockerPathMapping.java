package dev.lumina.docker;

import java.util.Objects;

/**
 * Path mapping between virtual machine path and local path (Screenshot 3).
 */
public class DockerPathMapping implements Cloneable {

    private String virtualMachinePath = "";
    private String localPath = "";

    public DockerPathMapping() {
    }

    public DockerPathMapping(String virtualMachinePath, String localPath) {
        this.virtualMachinePath = virtualMachinePath != null ? virtualMachinePath : "";
        this.localPath = localPath != null ? localPath : "";
    }

    public DockerPathMapping(DockerPathMapping other) {
        if (other != null) {
            this.virtualMachinePath = other.virtualMachinePath;
            this.localPath = other.localPath;
        }
    }

    public String getVirtualMachinePath() {
        return virtualMachinePath;
    }

    public void setVirtualMachinePath(String virtualMachinePath) {
        this.virtualMachinePath = virtualMachinePath != null ? virtualMachinePath : "";
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath != null ? localPath : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DockerPathMapping that)) return false;
        return Objects.equals(virtualMachinePath, that.virtualMachinePath) &&
                Objects.equals(localPath, that.localPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(virtualMachinePath, localPath);
    }

    @Override
    public DockerPathMapping clone() {
        return new DockerPathMapping(this);
    }
}
