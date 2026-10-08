package dev.lumina.run;

import dev.lumina.util.Settings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RunTargetsSettingsManagerTest {

    private RunTargetsSettingsManager manager;

    @BeforeEach
    void setUp() {
        Settings.clear();
        manager = RunTargetsSettingsManager.getInstance();
        manager.resetToDefaults();
    }

    @Test
    void testInitialDefaultState() {
        List<RunTargetConfig> list = manager.getTargets();
        assertNotNull(list);
        assertTrue(list.isEmpty());
        assertEquals("local", manager.getProjectDefaultTargetId());
    }

    @Test
    void testConfigCloneAndEquals() {
        RunTargetConfig ssh = new RunTargetConfig("Dev SSH", RunTargetType.SSH);
        ssh.setHost("192.168.1.50");
        ssh.setPort(2222);
        ssh.setUserName("ubuntu");

        RunTargetConfig clone = ssh.clone();
        assertEquals(ssh, clone);
        assertEquals(ssh.hashCode(), clone.hashCode());

        clone.setHost("10.0.0.1");
        assertNotEquals(ssh, clone);
    }

    @Test
    void testAddDuplicateRemoveTargets() {
        RunTargetConfig ssh = new RunTargetConfig("Staging SSH", RunTargetType.SSH);
        manager.addTarget(ssh);
        assertEquals(1, manager.getTargets().size());

        // Duplicate
        RunTargetConfig dup = manager.duplicateTarget(ssh.getId());
        assertNotNull(dup);
        assertEquals("Staging SSH (Copy)", dup.getName());
        assertEquals(2, manager.getTargets().size());

        // Add Docker target
        RunTargetConfig docker = new RunTargetConfig("Docker JDK", RunTargetType.DOCKER);
        docker.setImageName("eclipse-temurin:21");
        manager.addTarget(docker);
        assertEquals(3, manager.getTargets().size());

        // Set as default
        manager.setProjectDefaultTargetId(docker.getId());
        assertEquals(docker.getId(), manager.getProjectDefaultTargetId());

        // Remove docker target -> default falls back to local
        manager.removeTarget(docker.getId());
        assertEquals(2, manager.getTargets().size());
        assertEquals("local", manager.getProjectDefaultTargetId());
    }

    @Test
    void testPersistence() {
        RunTargetConfig compose = new RunTargetConfig("Compose Backend", RunTargetType.DOCKER_COMPOSE);
        compose.setComposeFile("src/docker/compose.yml");
        compose.setServiceName("api-service");

        manager.addTarget(compose);
        manager.setProjectDefaultTargetId(compose.getId());
        manager.saveSettings();

        // Reload
        manager.loadSettings();
        assertEquals(1, manager.getTargets().size());
        RunTargetConfig loaded = manager.getTargetById(compose.getId());
        assertNotNull(loaded);
        assertEquals("Compose Backend", loaded.getName());
        assertEquals(RunTargetType.DOCKER_COMPOSE, loaded.getType());
        assertEquals("src/docker/compose.yml", loaded.getComposeFile());
        assertEquals(compose.getId(), manager.getProjectDefaultTargetId());
    }
}
