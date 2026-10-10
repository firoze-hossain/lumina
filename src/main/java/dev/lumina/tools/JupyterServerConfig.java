package dev.lumina.tools;

import java.util.Objects;
import java.util.UUID;

/**
 * Model representing a configured Jupyter server matching Image 5.
 */
public class JupyterServerConfig implements Cloneable {

    public static final String TYPE_IDE_MANAGED = "IDE-Managed Server";
    public static final String TYPE_EXTERNAL = "External Server";
    public static final String TYPE_RUNNING_LOCAL = "Running Local Server";

    private String id;
    private String name;
    private String serverType;
    private boolean autodetectedExecutionMode;
    private String url;
    private String token;

    public JupyterServerConfig() {
        this(UUID.randomUUID().toString(), TYPE_IDE_MANAGED, TYPE_IDE_MANAGED, true, "", "");
    }

    public JupyterServerConfig(String id, String name, String serverType, boolean autodetectedExecutionMode, String url, String token) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = name != null ? name : TYPE_IDE_MANAGED;
        this.serverType = serverType != null ? serverType : TYPE_IDE_MANAGED;
        this.autodetectedExecutionMode = autodetectedExecutionMode;
        this.url = url != null ? url : "";
        this.token = token != null ? token : "";
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
        this.name = name != null ? name : "";
    }

    public String getServerType() {
        return serverType;
    }

    public void setServerType(String serverType) {
        this.serverType = serverType != null ? serverType : TYPE_IDE_MANAGED;
    }

    public boolean isAutodetectedExecutionMode() {
        return autodetectedExecutionMode;
    }

    public void setAutodetectedExecutionMode(boolean autodetectedExecutionMode) {
        this.autodetectedExecutionMode = autodetectedExecutionMode;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url != null ? url : "";
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token != null ? token : "";
    }

    public JupyterServerConfig copy() {
        return clone();
    }

    @Override
    public JupyterServerConfig clone() {
        return new JupyterServerConfig(id, name, serverType, autodetectedExecutionMode, url, token);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JupyterServerConfig that = (JupyterServerConfig) o;
        return autodetectedExecutionMode == that.autodetectedExecutionMode &&
                Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(serverType, that.serverType) &&
                Objects.equals(url, that.url) &&
                Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, serverType, autodetectedExecutionMode, url, token);
    }

    @Override
    public String toString() {
        return name + (autodetectedExecutionMode ? " Auto" : "");
    }
}
