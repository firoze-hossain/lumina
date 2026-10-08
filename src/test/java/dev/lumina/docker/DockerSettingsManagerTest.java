package dev.lumina.docker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DockerSettingsManager and Docker models.
 */
public class DockerSettingsManagerTest {

    private DockerSettingsManager manager;

    @BeforeEach
    void setUp() {
        manager = DockerSettingsManager.getInstance();
        manager.resetToDefaults();
    }

    @Test
    void testDockerServerDefaultsAndCloning() {
        List<DockerServerConfig> servers = manager.getDockerServers();
        assertFalse(servers.isEmpty());

        DockerServerConfig s = servers.get(0);
        assertEquals("Docker", s.getName());
        assertTrue(s.isDetectExecutablePathsAutomatically());
        assertEquals(DockerDaemonType.UNIX_SOCKET, s.getDaemonType());
        assertEquals("default unix:///var/run/docker.sock", s.getUnixSocketPath());
        assertEquals("tcp://localhost:2375", s.getTcpEngineApiUrl());
        assertEquals("<create configuration>", s.getSshConfiguration());

        DockerServerConfig clone = s.clone();
        assertEquals(s, clone);
        assertEquals(s.hashCode(), clone.hashCode());

        // Add path mapping
        clone.getPathMappings().add(new DockerPathMapping("/app", "/home/user/project"));
        assertNotEquals(s, clone);
        assertEquals(1, clone.getPathMappings().size());
        assertEquals("/app", clone.getPathMappings().get(0).getVirtualMachinePath());
    }

    @Test
    void testDockerConsoleSettings() {
        DockerConsoleSettings console = manager.getConsoleSettings();
        assertTrue(console.isFoldPreviousSessionsInLogConsole());

        DockerConsoleSettings clone = console.clone();
        assertEquals(console, clone);

        clone.setFoldPreviousSessionsInLogConsole(false);
        manager.setConsoleSettings(clone);

        assertFalse(manager.getConsoleSettings().isFoldPreviousSessionsInLogConsole());
    }

    @Test
    void testDockerRegistryDefaultsAndAddressPopulation() {
        List<DockerRegistryConfig> registries = manager.getRegistries();
        assertFalse(registries.isEmpty());

        DockerRegistryConfig r = registries.get(0);
        assertEquals("Docker Registry", r.getName());
        assertEquals(DockerRegistryType.DOCKER_HUB, r.getRegistryType());
        assertEquals("registry-1.docker.io", r.getAddress());

        // Check GitLab and GitHub auto addresses
        DockerRegistryConfig gitlab = new DockerRegistryConfig("GitLab Reg", DockerRegistryType.GITLAB);
        assertEquals("registry.gitlab.com", gitlab.getAddress());

        DockerRegistryConfig gh = new DockerRegistryConfig("GitHub Packages", DockerRegistryType.GITHUB);
        assertEquals("ghcr.io", gh.getAddress());

        DockerRegistryConfig custom = new DockerRegistryConfig("Self-hosted", DockerRegistryType.OTHER);
        assertEquals("", custom.getAddress());
    }

    @Test
    void testConnectionTesting() {
        // Test Registry Connection validation
        DockerRegistryConfig emptyUser = new DockerRegistryConfig("Docker Hub", DockerRegistryType.DOCKER_HUB);
        emptyUser.setUsername("");
        emptyUser.setPassword("");
        assertEquals("Cannot connect: Username required", manager.testRegistryConnection(emptyUser));

        emptyUser.setUsername("testuser");
        assertEquals("Cannot connect: Password required", manager.testRegistryConnection(emptyUser));

        emptyUser.setPassword("secret");
        assertEquals("Connection successful", manager.testRegistryConnection(emptyUser));

        // Test Docker daemon probe
        DockerServerConfig cfg = new DockerServerConfig("Docker");
        String result = manager.testDockerDaemonConnection(cfg);
        assertNotNull(result);
        assertTrue(result.contains("Connection successful") || result.contains("ProcessNotCreatedException"));
    }

    @Test
    void testPersistenceCycle() {
        DockerServerConfig customServer = new DockerServerConfig("Remote Daemon");
        customServer.setDaemonType(DockerDaemonType.TCP_SOCKET);
        customServer.setTcpEngineApiUrl("tcp://192.168.1.100:2375");
        manager.addDockerServer(customServer);

        DockerRegistryConfig reg = new DockerRegistryConfig("My Registry", DockerRegistryType.GITHUB);
        reg.setUsername("developer");
        reg.setPassword("token123");
        manager.addRegistry(reg);

        manager.saveSettings();

        // Reload
        manager.loadSettings();
        assertTrue(manager.getDockerServers().stream().anyMatch(s -> "Remote Daemon".equals(s.getName())));
        assertTrue(manager.getRegistries().stream().anyMatch(r -> "My Registry".equals(r.getName())));
    }
}
