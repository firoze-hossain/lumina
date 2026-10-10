package dev.lumina.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing Tools > Jupyter > Jupyter Servers settings matching Image 5.
 */
public class JupyterServersSettings implements Cloneable {

    private List<JupyterServerConfig> servers = new ArrayList<>();
    private String selectedServerId = "";

    public JupyterServersSettings() {
        this.servers = createDefaultServers();
        if (!this.servers.isEmpty()) {
            this.selectedServerId = this.servers.get(0).getId();
        }
    }

    public static List<JupyterServerConfig> createDefaultServers() {
        List<JupyterServerConfig> list = new ArrayList<>();
        list.add(new JupyterServerConfig("default-ide-managed", "IDE-Managed Server", JupyterServerConfig.TYPE_IDE_MANAGED, true, "", ""));
        return list;
    }

    public List<JupyterServerConfig> getServers() {
        return servers;
    }

    public void setServers(List<JupyterServerConfig> servers) {
        this.servers = new ArrayList<>();
        if (servers != null) {
            for (JupyterServerConfig s : servers) {
                this.servers.add(s != null ? s.copy() : new JupyterServerConfig());
            }
        }
    }

    public String getSelectedServerId() {
        return selectedServerId;
    }

    public void setSelectedServerId(String selectedServerId) {
        this.selectedServerId = selectedServerId != null ? selectedServerId : "";
    }

    public JupyterServerConfig getSelectedServer() {
        if (selectedServerId == null || selectedServerId.isEmpty()) {
            return !servers.isEmpty() ? servers.get(0) : null;
        }
        for (JupyterServerConfig s : servers) {
            if (selectedServerId.equals(s.getId())) {
                return s;
            }
        }
        return !servers.isEmpty() ? servers.get(0) : null;
    }

    public JupyterServersSettings copy() {
        return clone();
    }

    @Override
    public JupyterServersSettings clone() {
        JupyterServersSettings c = new JupyterServersSettings();
        c.setServers(this.servers);
        c.selectedServerId = this.selectedServerId;
        return c;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JupyterServersSettings that = (JupyterServersSettings) o;
        return Objects.equals(servers, that.servers) &&
                Objects.equals(selectedServerId, that.selectedServerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(servers, selectedServerId);
    }

    @Override
    public String toString() {
        return "JupyterServersSettings{servers=" + servers.size() + ", selected='" + selectedServerId + "'}";
    }
}
