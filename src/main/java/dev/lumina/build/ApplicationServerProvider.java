package dev.lumina.build;

import javafx.scene.Node;
import java.util.List;

/**
 * Service Provider Interface (SPI) for dynamic Application Server integration in Lumina IDE.
 * Allows plugins or core modules to contribute server runtimes (Tomcat, TomEE, WildFly, Jetty, etc.)
 * without hardcoding server types into the settings UI.
 */
public interface ApplicationServerProvider {

    /**
     * Unique identifier for this application server type (e.g. "tomcat", "wildfly").
     */
    String getTypeId();

    /**
     * Display name shown in menus and lists (e.g. "Tomcat Server", "JBoss/WildFly Server").
     */
    String getDisplayName();

    /**
     * Creates a vector or graphic icon representing this server type.
     */
    Node createIcon();

    /**
     * Returns default display name for a new instance (e.g. "Tomcat 10.1").
     */
    String getDefaultName();

    /**
     * Checks whether the given directory is a valid installation for this server.
     */
    boolean validateHomePath(String path);

    /**
     * Detects server version string from the installation directory.
     */
    String detectVersion(String path);

    /**
     * Discovers libraries and JARs provided by this application server installation.
     */
    List<String> detectLibraries(String path);

    /**
     * Factory method creating an ApplicationServer instance initialized with detected details.
     */
    ApplicationServer createServer(String homePath);

    /**
     * Indicates whether this server type supports configuring a separate base directory.
     */
    default boolean supportsBaseDirectory() {
        return false;
    }
}
