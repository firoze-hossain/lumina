package dev.lumina.docker;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Configuration for a Docker daemon connection (Screenshot 3).
 */
public class DockerServerConfig implements Cloneable {

    private String id = UUID.randomUUID().toString();
    private String name = "Docker";
    private boolean detectExecutablePathsAutomatically = true;
    private DockerDaemonType daemonType = DockerDaemonType.UNIX_SOCKET;

    private String unixSocketPath = "default unix:///var/run/docker.sock";
    private String tcpEngineApiUrl = "tcp://localhost:2375";
    private String tcpCertificatesFolder = "";
    private String sshConfiguration = "<create configuration>";

    private List<DockerPathMapping> pathMappings = new ArrayList<>();

    public DockerServerConfig() {
    }

    public DockerServerConfig(String name) {
        this.name = name != null ? name : "Docker";
    }

    public DockerServerConfig(DockerServerConfig other) {
        if (other != null) {
            this.id = other.id;
            this.name = other.name;
            this.detectExecutablePathsAutomatically = other.detectExecutablePathsAutomatically;
            this.daemonType = other.daemonType != null ? other.daemonType : DockerDaemonType.UNIX_SOCKET;
            this.unixSocketPath = other.unixSocketPath;
            this.tcpEngineApiUrl = other.tcpEngineApiUrl;
            this.tcpCertificatesFolder = other.tcpCertificatesFolder;
            this.sshConfiguration = other.sshConfiguration;

            this.pathMappings = new ArrayList<>();
            for (DockerPathMapping m : other.pathMappings) {
                this.pathMappings.add(m.clone());
            }
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id != null ? id : UUID.randomUUID().toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "Docker";
    }

    public boolean isDetectExecutablePathsAutomatically() {
        return detectExecutablePathsAutomatically;
    }

    public void setDetectExecutablePathsAutomatically(boolean detectExecutablePathsAutomatically) {
        this.detectExecutablePathsAutomatically = detectExecutablePathsAutomatically;
    }

    public DockerDaemonType getDaemonType() {
        return daemonType;
    }

    public void setDaemonType(DockerDaemonType daemonType) {
        this.daemonType = daemonType != null ? daemonType : DockerDaemonType.UNIX_SOCKET;
    }

    public String getUnixSocketPath() {
        return unixSocketPath;
    }

    public void setUnixSocketPath(String unixSocketPath) {
        this.unixSocketPath = unixSocketPath != null ? unixSocketPath : "default unix:///var/run/docker.sock";
    }

    public String getTcpEngineApiUrl() {
        return tcpEngineApiUrl;
    }

    public void setTcpEngineApiUrl(String tcpEngineApiUrl) {
        this.tcpEngineApiUrl = tcpEngineApiUrl != null ? tcpEngineApiUrl : "";
    }

    public String getTcpCertificatesFolder() {
        return tcpCertificatesFolder;
    }

    public void setTcpCertificatesFolder(String tcpCertificatesFolder) {
        this.tcpCertificatesFolder = tcpCertificatesFolder != null ? tcpCertificatesFolder : "";
    }

    public String getSshConfiguration() {
        return sshConfiguration;
    }

    public void setSshConfiguration(String sshConfiguration) {
        this.sshConfiguration = sshConfiguration != null ? sshConfiguration : "<create configuration>";
    }

    public List<DockerPathMapping> getPathMappings() {
        return pathMappings;
    }

    public void setPathMappings(List<DockerPathMapping> pathMappings) {
        this.pathMappings = pathMappings != null ? new ArrayList<>(pathMappings) : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DockerServerConfig that)) return false;
        return detectExecutablePathsAutomatically == that.detectExecutablePathsAutomatically &&
                Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                daemonType == that.daemonType &&
                Objects.equals(unixSocketPath, that.unixSocketPath) &&
                Objects.equals(tcpEngineApiUrl, that.tcpEngineApiUrl) &&
                Objects.equals(tcpCertificatesFolder, that.tcpCertificatesFolder) &&
                Objects.equals(sshConfiguration, that.sshConfiguration) &&
                Objects.equals(pathMappings, that.pathMappings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, detectExecutablePathsAutomatically, daemonType,
                unixSocketPath, tcpEngineApiUrl, tcpCertificatesFolder, sshConfiguration, pathMappings);
    }

    @Override
    public DockerServerConfig clone() {
        return new DockerServerConfig(this);
    }
}
