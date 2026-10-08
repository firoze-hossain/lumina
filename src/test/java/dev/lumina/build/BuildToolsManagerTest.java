package dev.lumina.build;

import dev.lumina.build.BuildToolsSettings.SyncTrigger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BuildToolsSettings and BuildToolsManager.
 */
public class BuildToolsManagerTest {

    private BuildToolsManager manager;

    @BeforeEach
    void setUp() {
        manager = BuildToolsManager.getInstance();
    }

    @Test
    void testDefaultSettings() {
        BuildToolsSettings settings = manager.getSettings();
        assertTrue(settings.isSyncOnBuildScriptChanges(), "Sync on changes should be true by default");
        assertEquals(SyncTrigger.EXTERNAL_CHANGES, settings.getSyncTrigger(), "External changes should be selected by default");
    }

    @Test
    void testCloneAndEquals() {
        BuildToolsSettings s1 = new BuildToolsSettings();
        BuildToolsSettings s2 = s1.clone();
        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());

        s2.setSyncTrigger(SyncTrigger.ANY_CHANGES);
        assertNotEquals(s1, s2);

        s2.setSyncTrigger(SyncTrigger.EXTERNAL_CHANGES);
        assertEquals(s1, s2);

        s2.setSyncOnBuildScriptChanges(false);
        assertNotEquals(s1, s2);
    }

    @Test
    void testPersistenceCycle() {
        BuildToolsSettings original = manager.getSettings();

        BuildToolsSettings modified = new BuildToolsSettings();
        modified.setSyncOnBuildScriptChanges(false);
        modified.setSyncTrigger(SyncTrigger.ANY_CHANGES);

        manager.setSettings(modified);
        manager.saveSettings();

        manager.loadSettings();
        BuildToolsSettings reloaded = manager.getSettings();
        assertEquals(modified, reloaded);

        manager.setSettings(original);
    }
}
