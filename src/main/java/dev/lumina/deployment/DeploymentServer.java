package dev.lumina.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Dynamic configuration model for a Deployment server matching IntelliJ IDEA.
 */
public class DeploymentServer implements Cloneable {

    private String id = UUID.randomUUID().toString();
    private String name = "Unnamed";
    private DeploymentServerType type = DeploymentServerType.SFTP;
    private boolean defaultServer = false;

    // Connection properties
    private String host = "localhost";
    private int port = 22;
    private String rootPath = "/";
    private String webServerUrl = "http://localhost";
    private String username = "";
    private String password = "";
    private String sshConfiguration = "<none>";
    private String localFolderPath = "";

    // Mappings and exclusions
    private List<DeploymentMapping> mappings = new ArrayList<>();
    private List<DeploymentExcludedPath> excludedPaths = new ArrayList<>();
    private List<String> groupServerIds = new ArrayList<>();

    public DeploymentServer() {
        initDefaultMappings();
    }

    public DeploymentServer(String name, DeploymentServerType type) {
        this.name = name != null ? name : "Unnamed";
        this.type = type != null ? type : DeploymentServerType.SFTP;
        applyTypeDefaults(this.type);
        initDefaultMappings();
    }

    public DeploymentServer(DeploymentServer other) {
        if (other != null) {
            this.id = other.id;
            this.name = other.name;
            this.type = other.type;
            this.defaultServer = other.defaultServer;
            this.host = other.host;
            this.port = other.port;
            this.rootPath = other.rootPath;
            this.webServerUrl = other.webServerUrl;
            this.username = other.username;
            this.password = other.password;
            this.sshConfiguration = other.sshConfiguration;
            this.localFolderPath = other.localFolderPath;

            this.mappings = new ArrayList<>();
            for (DeploymentMapping m : other.mappings) {
                this.mappings.add(m.clone());
            }

            this.excludedPaths = new ArrayList<>();
            for (DeploymentExcludedPath p : other.excludedPaths) {
                this.excludedPaths.add(p.clone());
            }

            this.groupServerIds = new ArrayList<>(other.groupServerIds);
        }
    }

    public void applyTypeDefaults(DeploymentServerType type) {
        if (type == null) return;
        switch (type) {
            case SFTP -> this.port = 22;
            case FTP -> this.port = 21;
            case FTPS -> this.port = 990;
            case WEBDAV -> this.port = 80;
            default -> {}
        }
    }

    private void initDefaultMappings() {
        if (this.mappings.isEmpty()) {
            this.mappings.add(new DeploymentMapping("", "/", "/"));
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
        this.name = name != null ? name : "Unnamed";
    }

    public DeploymentServerType getType() {
        return type;
    }

    public void setType(DeploymentServerType type) {
        this.type = type != null ? type : DeploymentServerType.SFTP;
    }

    public boolean isDefaultServer() {
        return defaultServer;
    }

    public void setDefaultServer(boolean defaultServer) {
        this.defaultServer = defaultServer;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host != null ? host : "";
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath != null ? rootPath : "/";
    }

    public String getWebServerUrl() {
        return webServerUrl;
    }

    public void setWebServerUrl(String webServerUrl) {
        this.webServerUrl = webServerUrl != null ? webServerUrl : "";
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username != null ? username : "";
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password != null ? password : "";
    }

    public String getSshConfiguration() {
        return sshConfiguration;
    }

    public void setSshConfiguration(String sshConfiguration) {
        this.sshConfiguration = sshConfiguration != null ? sshConfiguration : "<none>";
    }

    public String getLocalFolderPath() {
        return localFolderPath;
    }

    public void setLocalFolderPath(String localFolderPath) {
        this.localFolderPath = localFolderPath != null ? localFolderPath : "";
    }

    public List<DeploymentMapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<DeploymentMapping> mappings) {
        this.mappings = mappings != null ? new ArrayList<>(mappings) : new ArrayList<>();
    }

    public List<DeploymentExcludedPath> getExcludedPaths() {
        return excludedPaths;
    }

    public void setExcludedPaths(List<DeploymentExcludedPath> excludedPaths) {
        this.excludedPaths = excludedPaths != null ? new ArrayList<>(excludedPaths) : new ArrayList<>();
    }

    public List<String> getGroupServerIds() {
        return groupServerIds;
    }

    public void setGroupServerIds(List<String> groupServerIds) {
        this.groupServerIds = groupServerIds != null ? new ArrayList<>(groupServerIds) : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeploymentServer that)) return false;
        return port == that.port &&
                defaultServer == that.defaultServer &&
                Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                type == that.type &&
                Objects.equals(host, that.host) &&
                Objects.equals(rootPath, that.rootPath) &&
                Objects.equals(webServerUrl, that.webServerUrl) &&
                Objects.equals(username, that.username) &&
                Objects.equals(password, that.password) &&
                Objects.equals(sshConfiguration, that.sshConfiguration) &&
                Objects.equals(localFolderPath, that.localFolderPath) &&
                Objects.equals(mappings, that.mappings) &&
                Objects.equals(excludedPaths, that.excludedPaths) &&
                Objects.equals(groupServerIds, that.groupServerIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, type, defaultServer, host, port, rootPath,
                webServerUrl, username, password, sshConfiguration, localFolderPath,
                mappings, excludedPaths, groupServerIds);
    }

    @Override
    public DeploymentServer clone() {
        return new DeploymentServer(this);
    }
}
