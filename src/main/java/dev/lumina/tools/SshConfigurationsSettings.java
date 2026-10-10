package dev.lumina.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing Tools > SSH Configurations collection in Lumina IDE.
 */
public class SshConfigurationsSettings implements Cloneable {

    private List<SshConfigurationEntry> configurations = new ArrayList<>();

    public SshConfigurationsSettings() {
        // Initialize default configuration matching screenshot <username>@localhost:22 password
        SshConfigurationEntry defaultEntry = new SshConfigurationEntry("localhost", 22, "");
        defaultEntry.setAuthType(SshConfigurationEntry.AUTH_PASSWORD);
        defaultEntry.setParseConfigFile(true);
        configurations.add(defaultEntry);
    }

    public List<SshConfigurationEntry> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(List<SshConfigurationEntry> configurations) {
        this.configurations = configurations != null ? configurations : new ArrayList<>();
    }

    @Override
    public SshConfigurationsSettings clone() {
        try {
            SshConfigurationsSettings copy = (SshConfigurationsSettings) super.clone();
            copy.configurations = new ArrayList<>();
            for (SshConfigurationEntry entry : this.configurations) {
                copy.configurations.add(entry.clone());
            }
            return copy;
        } catch (CloneNotSupportedException ex) {
            SshConfigurationsSettings copy = new SshConfigurationsSettings();
            copy.configurations = new ArrayList<>();
            for (SshConfigurationEntry entry : this.configurations) {
                copy.configurations.add(entry.clone());
            }
            return copy;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SshConfigurationsSettings that = (SshConfigurationsSettings) o;
        return Objects.equals(configurations, that.configurations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configurations);
    }
}
