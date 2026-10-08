package dev.lumina.build;

import dev.lumina.util.Settings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RemoteJarRepositoriesSettingsManagerTest {

    private RemoteJarRepositoriesSettingsManager manager;

    @BeforeEach
    void setUp() {
        Settings.clear();
        manager = RemoteJarRepositoriesSettingsManager.getInstance();
        manager.resetAllToDefaults();
    }

    @Test
    void testDefaultRepositories() {
        List<String> mavenRepos = manager.getMavenRepositories();
        assertNotNull(mavenRepos);
        assertEquals(3, mavenRepos.size());
        assertEquals("https://repo.maven.apache.org/maven2", mavenRepos.get(0));
        assertEquals("https://repo1.maven.org/maven2", mavenRepos.get(1));
        assertEquals("https://repository.jboss.org/nexus/content/repositories/public/", mavenRepos.get(2));

        List<String> nexusUrls = manager.getArtifactoryNexusUrls();
        assertNotNull(nexusUrls);
        assertEquals(2, nexusUrls.size());
        assertEquals("https://oss.sonatype.org/service/local/", nexusUrls.get(0));
        assertEquals("https://repository.jboss.org/nexus/service/local/", nexusUrls.get(1));
    }

    @Test
    void testAddEditRemoveMavenRepositories() {
        manager.addMavenRepository("https://plugins.gradle.org/m2");
        assertEquals(4, manager.getMavenRepositories().size());
        assertTrue(manager.getMavenRepositories().contains("https://plugins.gradle.org/m2"));

        // Edit
        manager.updateMavenRepository(3, "https://plugins.gradle.org/m2-updated");
        assertEquals("https://plugins.gradle.org/m2-updated", manager.getMavenRepositories().get(3));

        // Remove
        manager.removeMavenRepository(3);
        assertEquals(3, manager.getMavenRepositories().size());
        assertFalse(manager.getMavenRepositories().contains("https://plugins.gradle.org/m2-updated"));

        // Reset
        manager.removeMavenRepository(0);
        assertEquals(2, manager.getMavenRepositories().size());
        manager.resetMavenRepositoriesToDefault();
        assertEquals(3, manager.getMavenRepositories().size());
    }

    @Test
    void testAddEditRemoveNexusUrls() {
        manager.addArtifactoryNexusUrl("https://mycompany.jfrog.io/artifactory/api");
        assertEquals(3, manager.getArtifactoryNexusUrls().size());

        manager.updateArtifactoryNexusUrl(2, "https://mycompany.jfrog.io/artifactory/api/v2");
        assertEquals("https://mycompany.jfrog.io/artifactory/api/v2", manager.getArtifactoryNexusUrls().get(2));

        manager.removeArtifactoryNexusUrl(2);
        assertEquals(2, manager.getArtifactoryNexusUrls().size());

        manager.resetArtifactoryNexusUrlsToDefault();
        assertEquals(2, manager.getArtifactoryNexusUrls().size());
    }

    @Test
    void testPersistence() {
        manager.addMavenRepository("https://maven.google.com");
        manager.addArtifactoryNexusUrl("https://nexus.internal.org/service/local/");
        manager.saveSettings();

        // Reload
        manager.loadSettings();
        assertTrue(manager.getMavenRepositories().contains("https://maven.google.com"));
        assertTrue(manager.getArtifactoryNexusUrls().contains("https://nexus.internal.org/service/local/"));
    }

    @Test
    void testConnectivityTestEmptyUrl() {
        RemoteJarRepositoriesSettingsManager.TestResult res = manager.testServiceUrl("");
        assertFalse(res.success());
        assertEquals("URL cannot be empty.", res.message());
    }
}
