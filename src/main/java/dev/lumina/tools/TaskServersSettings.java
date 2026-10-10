package dev.lumina.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing Tools > Tasks > Servers collection in Lumina IDE.
 */
public class TaskServersSettings implements Cloneable {

    private List<TaskServerEntry> servers = new ArrayList<>();

    public TaskServersSettings() {
    }

    public List<TaskServerEntry> getServers() {
        return servers;
    }

    public void setServers(List<TaskServerEntry> servers) {
        this.servers = servers != null ? servers : new ArrayList<>();
    }

    @Override
    public TaskServersSettings clone() {
        try {
            TaskServersSettings copy = (TaskServersSettings) super.clone();
            copy.servers = new ArrayList<>();
            for (TaskServerEntry entry : this.servers) {
                copy.servers.add(entry.clone());
            }
            return copy;
        } catch (CloneNotSupportedException ex) {
            TaskServersSettings copy = new TaskServersSettings();
            copy.servers = new ArrayList<>();
            for (TaskServerEntry entry : this.servers) {
                copy.servers.add(entry.clone());
            }
            return copy;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskServersSettings that = (TaskServersSettings) o;
        return Objects.equals(servers, that.servers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(servers);
    }
}
