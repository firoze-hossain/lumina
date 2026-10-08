package dev.lumina.javafx;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JavaFXSettingsManagerTest {

    private JavaFXSettingsManager manager;

    @BeforeEach
    void setUp() {
        manager = JavaFXSettingsManager.getInstance();
        manager.resetDefaults();
    }

    @Test
    void testDefaultValues() {
        assertEquals("", manager.getPathToSceneBuilder());
    }

    @Test
    void testPathToSceneBuilderPersistence() {
        manager.setPathToSceneBuilder("/opt/SceneBuilder/SceneBuilder");
        assertEquals("/opt/SceneBuilder/SceneBuilder", manager.getPathToSceneBuilder());

        manager.loadSettings();
        assertEquals("/opt/SceneBuilder/SceneBuilder", manager.getPathToSceneBuilder());

        manager.resetDefaults();
        assertEquals("", manager.getPathToSceneBuilder());
    }

    @Test
    void testDetectionMethodRuns() {
        String detected = manager.detectSceneBuilderPath();
        assertNotNull(detected);
    }
}
