package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing Tools > Rsync configuration settings in Lumina IDE.
 */
public class RsyncSettings implements Cloneable {

    public static final String DEFAULT_RSYNC_OPTIONS = "-zar";
    public static final String DEFAULT_RSYNC_EXECUTABLE = "rsync";
    public static final String DEFAULT_SHELL_EXECUTABLE = "ssh";

    private String rsyncExecutablePath = DEFAULT_RSYNC_EXECUTABLE;
    private String rsyncOptions = DEFAULT_RSYNC_OPTIONS;
    private String shellExecutablePath = DEFAULT_SHELL_EXECUTABLE;

    public RsyncSettings() {
    }

    public String getRsyncExecutablePath() {
        return rsyncExecutablePath != null ? rsyncExecutablePath : DEFAULT_RSYNC_EXECUTABLE;
    }

    public void setRsyncExecutablePath(String rsyncExecutablePath) {
        this.rsyncExecutablePath = rsyncExecutablePath != null ? rsyncExecutablePath.trim() : DEFAULT_RSYNC_EXECUTABLE;
    }

    public String getRsyncOptions() {
        return rsyncOptions != null ? rsyncOptions : DEFAULT_RSYNC_OPTIONS;
    }

    public void setRsyncOptions(String rsyncOptions) {
        this.rsyncOptions = rsyncOptions != null ? rsyncOptions.trim() : DEFAULT_RSYNC_OPTIONS;
    }

    public String getShellExecutablePath() {
        return shellExecutablePath != null ? shellExecutablePath : DEFAULT_SHELL_EXECUTABLE;
    }

    public void setShellExecutablePath(String shellExecutablePath) {
        this.shellExecutablePath = shellExecutablePath != null ? shellExecutablePath.trim() : DEFAULT_SHELL_EXECUTABLE;
    }

    @Override
    public RsyncSettings clone() {
        try {
            return (RsyncSettings) super.clone();
        } catch (CloneNotSupportedException e) {
            RsyncSettings copy = new RsyncSettings();
            copy.rsyncExecutablePath = this.rsyncExecutablePath;
            copy.rsyncOptions = this.rsyncOptions;
            copy.shellExecutablePath = this.shellExecutablePath;
            return copy;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RsyncSettings that = (RsyncSettings) o;
        return Objects.equals(rsyncExecutablePath, that.rsyncExecutablePath) &&
                Objects.equals(rsyncOptions, that.rsyncOptions) &&
                Objects.equals(shellExecutablePath, that.shellExecutablePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rsyncExecutablePath, rsyncOptions, shellExecutablePath);
    }
}
