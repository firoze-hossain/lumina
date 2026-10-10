package dev.lumina.tools;

import java.util.Objects;
import java.util.UUID;

/**
 * Model representing a single SSH Configuration entry in Lumina IDE.
 */
public class SshConfigurationEntry implements Cloneable {

    public static final String AUTH_PASSWORD = "Password";
    public static final String AUTH_KEY_PAIR = "Key pair";
    public static final String AUTH_OPENSSH = "OpenSSH config and authentication agent";

    public static final String PROXY_NONE = "No proxy";
    public static final String PROXY_HTTP = "HTTP";
    public static final String PROXY_SOCKS = "SOCKS";

    public static final String PROXY_AUTH_NONE = "No authentication";
    public static final String PROXY_AUTH_PASSWORD = "Password";

    private String id = UUID.randomUUID().toString();
    private String name = "";
    private boolean visibleOnlyForThisProject = false;
    private String host = "localhost";
    private int port = 22;
    private String username = "";
    private String authType = AUTH_PASSWORD;
    private String password = "";
    private boolean savePassword = true;
    private String privateKeyPath = "";
    private String passphrase = "";
    private boolean parseConfigFile = true;

    // Connection parameters
    private boolean sendKeepAlive = false;
    private int keepAliveIntervalSeconds = 300;
    private String strictHostKeyChecking = "Ask";
    private boolean hashHosts = false;

    // HTTP/SOCKS Proxy
    private boolean useGlobalProxy = false;
    private String proxyType = PROXY_NONE;
    private String proxyHost = "";
    private int proxyPort = 1080;
    private String proxyAuthType = PROXY_AUTH_NONE;
    private String proxyUser = "";
    private String proxyPassword = "";

    public SshConfigurationEntry() {
    }

    public SshConfigurationEntry(String host, int port, String username) {
        this.host = host != null ? host : "localhost";
        this.port = port > 0 ? port : 22;
        this.username = username != null ? username : "";
    }

    public String getDisplayName() {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        String userPart = (username != null && !username.trim().isEmpty()) ? username : "<username>";
        String hostPart = (host != null && !host.trim().isEmpty()) ? host : "localhost";
        String authSummary = AUTH_PASSWORD.equals(authType) ? "password" : (AUTH_KEY_PAIR.equals(authType) ? "key pair" : "openssh");
        return userPart + "@" + hostPart + ":" + port + " " + authSummary;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id != null ? id : UUID.randomUUID().toString();
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public boolean isVisibleOnlyForThisProject() {
        return visibleOnlyForThisProject;
    }

    public void setVisibleOnlyForThisProject(boolean visibleOnlyForThisProject) {
        this.visibleOnlyForThisProject = visibleOnlyForThisProject;
    }

    public String getHost() {
        return host != null ? host : "";
    }

    public void setHost(String host) {
        this.host = host != null ? host.trim() : "";
    }

    public int getPort() {
        return port > 0 ? port : 22;
    }

    public void setPort(int port) {
        this.port = port > 0 && port <= 65535 ? port : 22;
    }

    public String getUsername() {
        return username != null ? username : "";
    }

    public void setUsername(String username) {
        this.username = username != null ? username.trim() : "";
    }

    public String getAuthType() {
        return authType != null ? authType : AUTH_PASSWORD;
    }

    public void setAuthType(String authType) {
        this.authType = authType != null ? authType : AUTH_PASSWORD;
    }

    public String getPassword() {
        return password != null ? password : "";
    }

    public void setPassword(String password) {
        this.password = password != null ? password : "";
    }

    public boolean isSavePassword() {
        return savePassword;
    }

    public void setSavePassword(boolean savePassword) {
        this.savePassword = savePassword;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath != null ? privateKeyPath : "";
    }

    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath != null ? privateKeyPath.trim() : "";
    }

    public String getPassphrase() {
        return passphrase != null ? passphrase : "";
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase != null ? passphrase : "";
    }

    public boolean isParseConfigFile() {
        return parseConfigFile;
    }

    public void setParseConfigFile(boolean parseConfigFile) {
        this.parseConfigFile = parseConfigFile;
    }

    public boolean isSendKeepAlive() {
        return sendKeepAlive;
    }

    public void setSendKeepAlive(boolean sendKeepAlive) {
        this.sendKeepAlive = sendKeepAlive;
    }

    public int getKeepAliveIntervalSeconds() {
        return keepAliveIntervalSeconds;
    }

    public void setKeepAliveIntervalSeconds(int keepAliveIntervalSeconds) {
        this.keepAliveIntervalSeconds = keepAliveIntervalSeconds > 0 ? keepAliveIntervalSeconds : 300;
    }

    public String getStrictHostKeyChecking() {
        return strictHostKeyChecking != null ? strictHostKeyChecking : "Ask";
    }

    public void setStrictHostKeyChecking(String strictHostKeyChecking) {
        this.strictHostKeyChecking = strictHostKeyChecking != null ? strictHostKeyChecking : "Ask";
    }

    public boolean isHashHosts() {
        return hashHosts;
    }

    public void setHashHosts(boolean hashHosts) {
        this.hashHosts = hashHosts;
    }

    public boolean isUseGlobalProxy() {
        return useGlobalProxy;
    }

    public void setUseGlobalProxy(boolean useGlobalProxy) {
        this.useGlobalProxy = useGlobalProxy;
    }

    public String getProxyType() {
        return proxyType != null ? proxyType : PROXY_NONE;
    }

    public void setProxyType(String proxyType) {
        this.proxyType = proxyType != null ? proxyType : PROXY_NONE;
    }

    public String getProxyHost() {
        return proxyHost != null ? proxyHost : "";
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost != null ? proxyHost.trim() : "";
    }

    public int getProxyPort() {
        return proxyPort > 0 ? proxyPort : 1080;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort > 0 && proxyPort <= 65535 ? proxyPort : 1080;
    }

    public String getProxyAuthType() {
        return proxyAuthType != null ? proxyAuthType : PROXY_AUTH_NONE;
    }

    public void setProxyAuthType(String proxyAuthType) {
        this.proxyAuthType = proxyAuthType != null ? proxyAuthType : PROXY_AUTH_NONE;
    }

    public String getProxyUser() {
        return proxyUser != null ? proxyUser : "";
    }

    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser != null ? proxyUser.trim() : "";
    }

    public String getProxyPassword() {
        return proxyPassword != null ? proxyPassword : "";
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword != null ? proxyPassword : "";
    }

    @Override
    public SshConfigurationEntry clone() {
        try {
            return (SshConfigurationEntry) super.clone();
        } catch (CloneNotSupportedException e) {
            SshConfigurationEntry copy = new SshConfigurationEntry();
            copy.id = this.id;
            copy.name = this.name;
            copy.visibleOnlyForThisProject = this.visibleOnlyForThisProject;
            copy.host = this.host;
            copy.port = this.port;
            copy.username = this.username;
            copy.authType = this.authType;
            copy.password = this.password;
            copy.savePassword = this.savePassword;
            copy.privateKeyPath = this.privateKeyPath;
            copy.passphrase = this.passphrase;
            copy.parseConfigFile = this.parseConfigFile;
            copy.sendKeepAlive = this.sendKeepAlive;
            copy.keepAliveIntervalSeconds = this.keepAliveIntervalSeconds;
            copy.strictHostKeyChecking = this.strictHostKeyChecking;
            copy.hashHosts = this.hashHosts;
            copy.useGlobalProxy = this.useGlobalProxy;
            copy.proxyType = this.proxyType;
            copy.proxyHost = this.proxyHost;
            copy.proxyPort = this.proxyPort;
            copy.proxyAuthType = this.proxyAuthType;
            copy.proxyUser = this.proxyUser;
            copy.proxyPassword = this.proxyPassword;
            return copy;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SshConfigurationEntry that = (SshConfigurationEntry) o;
        return visibleOnlyForThisProject == that.visibleOnlyForThisProject &&
                port == that.port &&
                savePassword == that.savePassword &&
                parseConfigFile == that.parseConfigFile &&
                sendKeepAlive == that.sendKeepAlive &&
                keepAliveIntervalSeconds == that.keepAliveIntervalSeconds &&
                hashHosts == that.hashHosts &&
                useGlobalProxy == that.useGlobalProxy &&
                proxyPort == that.proxyPort &&
                Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(host, that.host) &&
                Objects.equals(username, that.username) &&
                Objects.equals(authType, that.authType) &&
                Objects.equals(password, that.password) &&
                Objects.equals(privateKeyPath, that.privateKeyPath) &&
                Objects.equals(passphrase, that.passphrase) &&
                Objects.equals(strictHostKeyChecking, that.strictHostKeyChecking) &&
                Objects.equals(proxyType, that.proxyType) &&
                Objects.equals(proxyHost, that.proxyHost) &&
                Objects.equals(proxyAuthType, that.proxyAuthType) &&
                Objects.equals(proxyUser, that.proxyUser) &&
                Objects.equals(proxyPassword, that.proxyPassword);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, visibleOnlyForThisProject, host, port, username, authType,
                password, savePassword, privateKeyPath, passphrase, parseConfigFile,
                sendKeepAlive, keepAliveIntervalSeconds, strictHostKeyChecking, hashHosts,
                useGlobalProxy, proxyType, proxyHost, proxyPort, proxyAuthType, proxyUser, proxyPassword);
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
