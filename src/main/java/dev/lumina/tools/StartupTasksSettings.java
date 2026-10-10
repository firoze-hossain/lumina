package dev.lumina.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing Tools > Startup Tasks collection in Lumina IDE.
 */
public class StartupTasksSettings implements Cloneable {

    private List<StartupTaskEntry> tasks = new ArrayList<>();

    public StartupTasksSettings() {
    }

    public List<StartupTaskEntry> getTasks() {
        return tasks;
    }

    public void setTasks(List<StartupTaskEntry> tasks) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
    }

    @Override
    public StartupTasksSettings clone() {
        try {
            StartupTasksSettings copy = (StartupTasksSettings) super.clone();
            copy.tasks = new ArrayList<>();
            for (StartupTaskEntry entry : this.tasks) {
                copy.tasks.add(entry.clone());
            }
            return copy;
        } catch (CloneNotSupportedException ex) {
            StartupTasksSettings copy = new StartupTasksSettings();
            copy.tasks = new ArrayList<>();
            for (StartupTaskEntry entry : this.tasks) {
                copy.tasks.add(entry.clone());
            }
            return copy;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StartupTasksSettings that = (StartupTasksSettings) o;
        return Objects.equals(tasks, that.tasks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tasks);
    }
}
