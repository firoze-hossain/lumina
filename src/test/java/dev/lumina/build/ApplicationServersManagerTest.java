package dev.lumina.build;

import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ApplicationServer, ApplicationServerProvider SPI, and ApplicationServersManager.
 */
public class ApplicationServersManagerTest {

    private ApplicationServersManager manager;

    @BeforeEach
    void setUp() {
        manager = ApplicationServersManager.getInstance();
    }

    @Test
    void testBuiltinProvidersPresent() {
        List<ApplicationServerProvider> providers = manager.getProviders();
        assertTrue(providers.size() >= 3, "Should have at least 3 built-in providers");

        assertNotNull(manager.getProvider(WildFlyServerProvider.TYPE_ID));
        assertNotNull(manager.getProvider(TomcatServerProvider.TYPE_ID));
        assertNotNull(manager.getProvider(TomEEServerProvider.TYPE_ID));

        assertEquals("JBoss/WildFly Server", manager.getProvider(WildFlyServerProvider.TYPE_ID).getDisplayName());
        assertEquals("Tomcat Server", manager.getProvider(TomcatServerProvider.TYPE_ID).getDisplayName());
        assertEquals("TomEE Server", manager.getProvider(TomEEServerProvider.TYPE_ID).getDisplayName());
    }

    @Test
    void testDynamicProviderRegistration() {
        ApplicationServerProvider customProvider = new ApplicationServerProvider() {
            @Override
            public String getTypeId() {
                return "jetty";
            }

            @Override
            public String getDisplayName() {
                return "Jetty Server";
            }

            @Override
            public Node createIcon() {
                return new Rectangle(16, 16);
            }

            @Override
            public String getDefaultName() {
                return "Jetty 12";
            }

            @Override
            public boolean validateHomePath(String path) {
                return path != null && path.contains("jetty");
            }

            @Override
            public String detectVersion(String path) {
                return "Jetty 12.0.0";
            }

            @Override
            public List<String> detectLibraries(String path) {
                return List.of("jetty-server.jar", "jetty-http.jar");
            }

            @Override
            public ApplicationServer createServer(String homePath) {
                ApplicationServer s = new ApplicationServer();
                s.setTypeId("jetty");
                s.setName(getDefaultName());
                s.setHomePath(homePath);
                s.setVersion(detectVersion(homePath));
                s.setLibraries(detectLibraries(homePath));
                return s;
            }
        };

        manager.registerProvider(customProvider);
        assertNotNull(manager.getProvider("jetty"));
        assertEquals("Jetty Server", manager.getProvider("jetty").getDisplayName());

        ApplicationServer created = customProvider.createServer("/opt/jetty");
        assertEquals("Jetty 12", created.getName());
        assertEquals("jetty", created.getTypeId());
        assertEquals(2, created.getLibraries().size());
    }

    @Test
    void testServerCrudOperations() {
        List<ApplicationServer> initial = manager.getServers();

        ApplicationServer s1 = new ApplicationServer("My Tomcat", "tomcat", "/opt/tomcat");
        s1.setVersion("10.1.20");
        s1.addLibrary("catalina.jar");
        s1.addLibrary("servlet-api.jar");

        manager.addServer(s1);

        List<ApplicationServer> current = manager.getServers();
        assertTrue(current.stream().anyMatch(s -> s.getId().equals(s1.getId())));

        manager.removeServer(s1);
        List<ApplicationServer> afterRemoval = manager.getServers();
        assertFalse(afterRemoval.stream().anyMatch(s -> s.getId().equals(s1.getId())));

        manager.setServers(initial);
    }

    @Test
    void testApplicationServerModelCloneAndEquals() {
        ApplicationServer s1 = new ApplicationServer("WildFly 31", "wildfly", "/opt/wildfly");
        s1.setBaseDirectory("/opt/wildfly/standalone");
        s1.setVersion("31.0.0");
        s1.setLibraries(List.of("jboss-modules.jar", "wildfly-ee.jar"));

        ApplicationServer s2 = s1.clone();
        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());

        s2.setName("WildFly 32");
        assertNotEquals(s1, s2);
    }

    @Test
    void testProviderDefaultVersionAndLibs() {
        TomcatServerProvider tomcat = new TomcatServerProvider();
        assertEquals("Tomcat 10.1", tomcat.getDefaultName());
        assertTrue(tomcat.detectLibraries(null).contains("catalina.jar"));
        assertTrue(tomcat.supportsBaseDirectory());

        WildFlyServerProvider wildfly = new WildFlyServerProvider();
        assertEquals("WildFly Server", wildfly.getDefaultName());
        assertTrue(wildfly.detectLibraries(null).contains("jboss-modules.jar"));
        assertTrue(wildfly.supportsBaseDirectory());

        TomEEServerProvider tomee = new TomEEServerProvider();
        assertEquals("TomEE 9.1", tomee.getDefaultName());
        assertTrue(tomee.detectLibraries(null).contains("tomee-common.jar"));
    }
}
