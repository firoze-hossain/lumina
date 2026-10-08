package dev.lumina.build;

import java.util.List;

/**
 * Service Provider Interface (SPI) for dynamic Maven installation discovery in Lumina IDE.
 */
public interface MavenInstallationProvider {

    /**
     * Discovers available Maven installations.
     */
    List<MavenInstallation> discoverInstallations();

    /**
     * Attempts to detect the Maven version for a given home directory.
     */
    String detectVersion(String homePath);
}
