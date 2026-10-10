package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing Tools > SSH Terminal configuration settings in Lumina IDE.
 */
public class SshTerminalSettings implements Cloneable {

    public static final String CONN_CURRENT_VAGRANT = "Current Vagrant";
    public static final String CONN_DEFAULT_PYTHON_REMOTE = "Default Python Remote Interpreter";
    public static final String CONN_SSH_CONFIG = "SSH configuration";

    public static final String DEFAULT_SSH_CONFIG_SELECT = "Select SSH configuration on every run";
    public static final String DEFAULT_ENCODING = "UTF-8";

    private String connectionMode = CONN_SSH_CONFIG;
    private String sshConfiguration = DEFAULT_SSH_CONFIG_SELECT;
    private String defaultEncoding = DEFAULT_ENCODING;

    public SshTerminalSettings() {
    }

    public String getConnectionMode() {
        return connectionMode != null ? connectionMode : CONN_SSH_CONFIG;
    }

    public void setConnectionMode(String connectionMode) {
        this.connectionMode = connectionMode != null ? connectionMode : CONN_SSH_CONFIG;
    }

    public String getSshConfiguration() {
        return sshConfiguration != null ? sshConfiguration : DEFAULT_SSH_CONFIG_SELECT;
    }

    public void setSshConfiguration(String sshConfiguration) {
        this.sshConfiguration = sshConfiguration != null ? sshConfiguration : DEFAULT_SSH_CONFIG_SELECT;
    }

    public String getDefaultEncoding() {
        return defaultEncoding != null ? defaultEncoding : DEFAULT_ENCODING;
    }

    public void setDefaultEncoding(String defaultEncoding) {
        this.defaultEncoding = defaultEncoding != null ? defaultEncoding : DEFAULT_ENCODING;
    }

    @Override
    public SshTerminalSettings clone() {
        try {
            return (SshTerminalSettings) super.clone();
        } catch (CloneNotSupportedException e) {
            SshTerminalSettings copy = new SshTerminalSettings();
            copy.connectionMode = this.connectionMode;
            copy.sshConfiguration = this.sshConfiguration;
            copy.defaultEncoding = this.defaultEncoding;
            return copy;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SshTerminalSettings that = (SshTerminalSettings) o;
        return Objects.equals(connectionMode, that.connectionMode) &&
                Objects.equals(sshConfiguration, that.sshConfiguration) &&
                Objects.equals(defaultEncoding, that.defaultEncoding);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectionMode, sshConfiguration, defaultEncoding);
    }
}
