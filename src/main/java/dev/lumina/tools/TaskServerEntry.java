package dev.lumina.tools;

import java.util.Objects;
import java.util.UUID;

/**
 * Model representing a single issue tracker / task server configuration in Lumina IDE.
 */
public class TaskServerEntry implements Cloneable {

    private String id = UUID.randomUUID().toString();
    private String serverType = "Generic";
    private String name = "Generic Server";
    private String url = "";
    private String username = "";
    private String password = "";
    private boolean shareUrl = false;
    private String commitMessageFormat = "{id} {summary}";
    private boolean useHttpAuthentication = false;
    private String httpUsername = "";
    private String httpPassword = "";

    public TaskServerEntry() {
    }

    public TaskServerEntry(String serverType, String name, String url) {
        this.serverType = serverType != null ? serverType : "Generic";
        this.name = name != null ? name : serverType;
        this.url = url != null ? url : "";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id != null ? id : UUID.randomUUID().toString();
    }

    public String getServerType() {
        return serverType != null ? serverType : "Generic";
    }

    public void setServerType(String serverType) {
        this.serverType = serverType != null ? serverType : "Generic";
    }

    public String getName() {
        return name != null ? name : serverType;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public String getUrl() {
        return url != null ? url : "";
    }

    public void setUrl(String url) {
        this.url = url != null ? url.trim() : "";
    }

    public String getUsername() {
        return username != null ? username : "";
    }

    public void setUsername(String username) {
        this.username = username != null ? username.trim() : "";
    }

    public String getPassword() {
        return password != null ? password : "";
    }

    public void setPassword(String password) {
        this.password = password != null ? password : "";
    }

    public boolean isShareUrl() {
        return shareUrl;
    }

    public void setShareUrl(boolean shareUrl) {
        this.shareUrl = shareUrl;
    }

    public String getCommitMessageFormat() {
        return commitMessageFormat != null ? commitMessageFormat : "{id} {summary}";
    }

    public void setCommitMessageFormat(String commitMessageFormat) {
        this.commitMessageFormat = commitMessageFormat != null ? commitMessageFormat : "{id} {summary}";
    }

    public boolean isUseHttpAuthentication() {
        return useHttpAuthentication;
    }

    public void setUseHttpAuthentication(boolean useHttpAuthentication) {
        this.useHttpAuthentication = useHttpAuthentication;
    }

    public String getHttpUsername() {
        return httpUsername != null ? httpUsername : "";
    }

    public void setHttpUsername(String httpUsername) {
        this.httpUsername = httpUsername != null ? httpUsername.trim() : "";
    }

    public String getHttpPassword() {
        return httpPassword != null ? httpPassword : "";
    }

    public void setHttpPassword(String httpPassword) {
        this.httpPassword = httpPassword != null ? httpPassword : "";
    }

    @Override
    public TaskServerEntry clone() {
        try {
            return (TaskServerEntry) super.clone();
        } catch (CloneNotSupportedException e) {
            TaskServerEntry copy = new TaskServerEntry();
            copy.id = this.id;
            copy.serverType = this.serverType;
            copy.name = this.name;
            copy.url = this.url;
            copy.username = this.username;
            copy.password = this.password;
            copy.shareUrl = this.shareUrl;
            copy.commitMessageFormat = this.commitMessageFormat;
            copy.useHttpAuthentication = this.useHttpAuthentication;
            copy.httpUsername = this.httpUsername;
            copy.httpPassword = this.httpPassword;
            return copy;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskServerEntry that = (TaskServerEntry) o;
        return shareUrl == that.shareUrl &&
                useHttpAuthentication == that.useHttpAuthentication &&
                Objects.equals(id, that.id) &&
                Objects.equals(serverType, that.serverType) &&
                Objects.equals(name, that.name) &&
                Objects.equals(url, that.url) &&
                Objects.equals(username, that.username) &&
                Objects.equals(password, that.password) &&
                Objects.equals(commitMessageFormat, that.commitMessageFormat) &&
                Objects.equals(httpUsername, that.httpUsername) &&
                Objects.equals(httpPassword, that.httpPassword);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, serverType, name, url, username, password, shareUrl,
                commitMessageFormat, useHttpAuthentication, httpUsername, httpPassword);
    }

    @Override
    public String toString() {
        return (name != null && !name.isEmpty()) ? name : (serverType + " (" + url + ")");
    }
}
