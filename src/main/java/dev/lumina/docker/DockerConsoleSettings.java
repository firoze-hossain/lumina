package dev.lumina.docker;

import java.util.Objects;

/**
 * Settings for Build, Execution, Deployment > Docker > Console (Screenshot 4).
 */
public class DockerConsoleSettings implements Cloneable {

    private boolean foldPreviousSessionsInLogConsole = true;

    public DockerConsoleSettings() {
    }

    public DockerConsoleSettings(boolean foldPreviousSessionsInLogConsole) {
        this.foldPreviousSessionsInLogConsole = foldPreviousSessionsInLogConsole;
    }

    public DockerConsoleSettings(DockerConsoleSettings other) {
        if (other != null) {
            this.foldPreviousSessionsInLogConsole = other.foldPreviousSessionsInLogConsole;
        }
    }

    public boolean isFoldPreviousSessionsInLogConsole() {
        return foldPreviousSessionsInLogConsole;
    }

    public void setFoldPreviousSessionsInLogConsole(boolean foldPreviousSessionsInLogConsole) {
        this.foldPreviousSessionsInLogConsole = foldPreviousSessionsInLogConsole;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DockerConsoleSettings that)) return false;
        return foldPreviousSessionsInLogConsole == that.foldPreviousSessionsInLogConsole;
    }

    @Override
    public int hashCode() {
        return Objects.hash(foldPreviousSessionsInLogConsole);
    }

    @Override
    public DockerConsoleSettings clone() {
        return new DockerConsoleSettings(this);
    }
}
