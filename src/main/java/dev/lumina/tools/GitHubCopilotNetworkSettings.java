package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing Tools > GitHub Copilot > Network settings matching Image 4.
 */
public class GitHubCopilotNetworkSettings implements Cloneable {

    public static final String NETWORKING_AUTO = "Auto";
    public static final String NETWORKING_MANUAL = "Manual";
    public static final String NETWORKING_DISABLED = "Disabled";

    private boolean customizeHttpProxy = false;
    private String hostName = "";
    private int portNumber = 0;
    private boolean proxyAuthentication = false;
    private String login = "";
    private String password = "";
    private String networking = NETWORKING_AUTO;
    private String overrideKerberosProxyPrincipalName = "";

    public GitHubCopilotNetworkSettings() {
    }

    public GitHubCopilotNetworkSettings(boolean customizeHttpProxy, String hostName, int portNumber,
                                       boolean proxyAuthentication, String login, String password,
                                       String networking, String overrideKerberosProxyPrincipalName) {
        this.customizeHttpProxy = customizeHttpProxy;
        this.hostName = hostName != null ? hostName : "";
        this.portNumber = portNumber;
        this.proxyAuthentication = proxyAuthentication;
        this.login = login != null ? login : "";
        this.password = password != null ? password : "";
        this.networking = networking != null ? networking : NETWORKING_AUTO;
        this.overrideKerberosProxyPrincipalName = overrideKerberosProxyPrincipalName != null ? overrideKerberosProxyPrincipalName : "";
    }

    public boolean isCustomizeHttpProxy() {
        return customizeHttpProxy;
    }

    public void setCustomizeHttpProxy(boolean customizeHttpProxy) {
        this.customizeHttpProxy = customizeHttpProxy;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName != null ? hostName : "";
    }

    public int getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }

    public boolean isProxyAuthentication() {
        return proxyAuthentication;
    }

    public void setProxyAuthentication(boolean proxyAuthentication) {
        this.proxyAuthentication = proxyAuthentication;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login != null ? login : "";
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password != null ? password : "";
    }

    public String getNetworking() {
        return networking;
    }

    public void setNetworking(String networking) {
        this.networking = networking != null ? networking : NETWORKING_AUTO;
    }

    public String getOverrideKerberosProxyPrincipalName() {
        return overrideKerberosProxyPrincipalName;
    }

    public String getOverrideKerberosPrincipal() {
        return overrideKerberosProxyPrincipalName;
    }

    public void setOverrideKerberosProxyPrincipalName(String overrideKerberosProxyPrincipalName) {
        this.overrideKerberosProxyPrincipalName = overrideKerberosProxyPrincipalName != null ? overrideKerberosProxyPrincipalName : "";
    }

    public void setOverrideKerberosPrincipal(String overrideKerberosPrincipal) {
        setOverrideKerberosProxyPrincipalName(overrideKerberosPrincipal);
    }

    @Override
    public GitHubCopilotNetworkSettings clone() {
        return new GitHubCopilotNetworkSettings(customizeHttpProxy, hostName, portNumber,
                proxyAuthentication, login, password, networking, overrideKerberosProxyPrincipalName);
    }

    public GitHubCopilotNetworkSettings copy() {
        return clone();
    }

    @Override
    public String toString() {
        return "GitHubCopilotNetworkSettings{host=" + hostName + ", port=" + portNumber + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GitHubCopilotNetworkSettings that = (GitHubCopilotNetworkSettings) o;
        return customizeHttpProxy == that.customizeHttpProxy &&
                portNumber == that.portNumber &&
                proxyAuthentication == that.proxyAuthentication &&
                Objects.equals(hostName, that.hostName) &&
                Objects.equals(login, that.login) &&
                Objects.equals(password, that.password) &&
                Objects.equals(networking, that.networking) &&
                Objects.equals(overrideKerberosProxyPrincipalName, that.overrideKerberosProxyPrincipalName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customizeHttpProxy, hostName, portNumber, proxyAuthentication,
                login, password, networking, overrideKerberosProxyPrincipalName);
    }
}
