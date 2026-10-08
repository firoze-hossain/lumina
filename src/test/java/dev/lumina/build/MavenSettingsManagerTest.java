package dev.lumina.build;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MavenSettings, MavenInstallationProvider SPI, and MavenSettingsManager.
 */
public class MavenSettingsManagerTest {

    private MavenSettingsManager manager;

    @BeforeEach
    void setUp() {
        manager = MavenSettingsManager.getInstance();
    }

    @Test
    void testDefaultSettings() {
        MavenSettings s = manager.getSettings();
        assertFalse(s.isWorkOffline(), "Work offline should be false by default");
        assertTrue(s.isExecuteGoalsRecursively(), "Execute recursively should be true by default");
        assertFalse(s.isPrintExceptionStackTraces(), "Print exception stack traces should be false by default");
        assertFalse(s.isAlwaysUpdateSnapshots(), "Always update snapshots should be false by default");
        assertEquals("Info", s.getOutputLevel());
        assertEquals("No Global Policy", s.getChecksumPolicy());
        assertEquals("Default", s.getMultiprojectFailPolicy());
        assertEquals("", s.getThreadCount());
        assertEquals("Bundled (Maven 3)", s.getMavenHome());
        assertEquals("3.9.11", s.getMavenVersion());
        assertFalse(s.isUserSettingsOverride());
        assertFalse(s.isLocalRepoOverride());
        assertTrue(s.isUseMavenConfig());
    }

    @Test
    void testMavenInstallationDiscovery() {
        List<MavenInstallation> installs = manager.getDiscoveredInstallations();
        assertNotNull(installs);
        assertFalse(installs.isEmpty());

        boolean hasBundled = installs.stream().anyMatch(i -> "Bundled (Maven 3)".equals(i.getName()));
        boolean hasWrapper = installs.stream().anyMatch(i -> "Use Maven Wrapper".equals(i.getName()));

        assertTrue(hasBundled, "Should discover bundled Maven");
        assertTrue(hasWrapper, "Should discover Maven wrapper option");
    }

    @Test
    void testCustomMavenInstallationProvider() {
        MavenInstallationProvider customProvider = new MavenInstallationProvider() {
            @Override
            public List<MavenInstallation> discoverInstallations() {
                return List.of(new MavenInstallation("Custom Enterprise Maven 3.9", "/opt/custom-mvn", "3.9.6", false));
            }

            @Override
            public String detectVersion(String homePath) {
                if ("/opt/custom-mvn".equals(homePath)) return "3.9.6";
                return null;
            }
        };

        manager.registerInstallationProvider(customProvider);
        List<MavenInstallation> installs = manager.getDiscoveredInstallations();
        assertTrue(installs.stream().anyMatch(i -> "Custom Enterprise Maven 3.9".equals(i.getName())));
        assertEquals("3.9.6", manager.detectVersion("/opt/custom-mvn"));
    }

    @Test
    void testCloneAndEquals() {
        MavenSettings s1 = new MavenSettings();
        MavenSettings s2 = s1.clone();
        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());

        s2.setWorkOffline(true);
        assertNotEquals(s1, s2);

        s2.setWorkOffline(false);
        assertEquals(s1, s2);

        s2.setOutputLevel("Debug");
        assertNotEquals(s1, s2);
    }

    @Test
    void testPersistenceCycle() {
        MavenSettings original = manager.getSettings();

        MavenSettings modified = new MavenSettings();
        modified.setWorkOffline(true);
        modified.setExecuteGoalsRecursively(false);
        modified.setPrintExceptionStackTraces(true);
        modified.setAlwaysUpdateSnapshots(true);
        modified.setOutputLevel("Debug");
        modified.setChecksumPolicy("Strict");
        modified.setMultiprojectFailPolicy("Fail never");
        modified.setThreadCount("4");
        modified.setMavenHome("/usr/local/maven");
        modified.setUserSettingsOverride(true);
        modified.setUserSettingsFile("/custom/settings.xml");
        modified.setLocalRepoOverride(true);
        modified.setLocalRepo("/custom/repo");
        modified.setUseMavenConfig(false);

        manager.setSettings(modified);
        manager.saveSettings();

        manager.loadSettings();
        MavenSettings reloaded = manager.getSettings();
        assertEquals(modified, reloaded);

        manager.setSettings(original);
    }
}
