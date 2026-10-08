package dev.lumina.run;

import java.util.Objects;
import java.util.UUID;

/**
 * Model representing a configured execution target (SSH, Docker, Docker Compose) in Lumina IDE.
 */
public class RunTargetConfig implements Cloneable {

    private String id;
    private String name;
    private RunTargetType type;

    // SSH fields
    private String host = "localhost";
    private int port = 22;
    private String userName = "root";
    private String projectRootOnTarget = "/home/user/project";

    // Docker fields
    private String dockerServer = "Docker";
    private String imageName = "openjdk:21-slim";
    private String runOptions = "--rm";
    private String containerWorkDir = "/app";

    // Docker Compose fields
    private String composeFile = "docker-compose.yml";
    private String serviceName = "web";

    public RunTargetConfig() {
        this.id = UUID.randomUUID().toString();
        this.name = "New Target";
        this.type = RunTargetType.SSH;
    }

    public RunTargetConfig(String name, RunTargetType type) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.type = type != null ? type : RunTargetType.SSH;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RunTargetType getType() {
        return type;
    }

    public void setType(RunTargetType type) {
        this.type = type;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getProjectRootOnTarget() {
        return projectRootOnTarget;
    }

    public void setProjectRootOnTarget(String projectRootOnTarget) {
        this.projectRootOnTarget = projectRootOnTarget;
    }

    public String getDockerServer() {
        return dockerServer;
    }

    public void setDockerServer(String dockerServer) {
        this.dockerServer = dockerServer;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getRunOptions() {
        return runOptions;
    }

    public void setRunOptions(String runOptions) {
        this.runOptions = runOptions;
    }

    public String getContainerWorkDir() {
        return containerWorkDir;
    }

    public void setContainerWorkDir(String containerWorkDir) {
        this.containerWorkDir = containerWorkDir;
    }

    public String getComposeFile() {
        return composeFile;
    }

    public void setComposeFile(String composeFile) {
        this.composeFile = composeFile;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public RunTargetConfig clone() {
        try {
            return (RunTargetConfig) super.clone();
        } catch (CloneNotSupportedException e) {
            RunTargetConfig c = new RunTargetConfig();
            c.id = this.id;
            c.name = this.name;
            c.type = this.type;
            c.host = this.host;
            c.port = this.port;
            c.userName = this.userName;
            c.projectRootOnTarget = this.projectRootOnTarget;
            c.dockerServer = this.dockerServer;
            c.imageName = this.imageName;
            c.runOptions = this.runOptions;
            c.containerWorkDir = this.containerWorkDir;
            c.composeFile = this.composeFile;
            c.serviceName = this.serviceName;
            return c;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RunTargetConfig that = (RunTargetConfig) o;
        return port == that.port &&
                Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                type == that.type &&
                Objects.equals(host, that.host) &&
                Objects.equals(userName, that.userName) &&
                Objects.equals(projectRootOnTarget, that.projectRootOnTarget) &&
                Objects.equals(dockerServer, that.dockerServer) &&
                Objects.equals(imageName, that.imageName) &&
                Objects.equals(runOptions, that.runOptions) &&
                Objects.equals(containerWorkDir, that.containerWorkDir) &&
                Objects.equals(composeFile, that.composeFile) &&
                Objects.equals(serviceName, that.serviceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, type, host, port, userName, projectRootOnTarget,
                dockerServer, imageName, runOptions, containerWorkDir, composeFile, serviceName);
    }

    @Override
    public String toString() {
        return (type != null ? type.getIconSymbol() + " " : "") + (name != null ? name : "Unnamed Target");
    }
}
