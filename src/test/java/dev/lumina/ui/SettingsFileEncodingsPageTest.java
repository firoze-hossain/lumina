package dev.lumina.ui;

import dev.lumina.settings.FileEncodingsSettings;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SettingsFileEncodingsPageTest {

    private static volatile boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            String display = System.getenv("DISPLAY");
            if (display == null && System.getProperty("os.name", "").toLowerCase().contains("linux")) {
                return;
            }
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(() -> {
                javaFxAvailable = true;
                latch.countDown();
            });
            latch.await(3, TimeUnit.SECONDS);
        } catch (Throwable ignored) {
            javaFxAvailable = false;
        }
    }

    @Test
    void testFileEncodingsSettingsModel() {
        FileEncodingsSettings settings = FileEncodingsSettings.getInstance();
        assertNotNull(settings);

        // Verify project root detection
        Path root = settings.getProjectRoot();
        assertNotNull(root);

        // Verify default or loaded global encoding
        assertNotNull(settings.getGlobalEncoding());
        assertFalse(settings.getGlobalEncoding().isBlank());

        // Verify loaded mappings from .idea/encodings.xml
        List<FileEncodingsSettings.PathMapping> mappings = settings.getMappings();
        assertNotNull(mappings);
        assertFalse(mappings.isEmpty(), "Should have loaded mappings from .idea/encodings.xml or defaults");

        boolean hasJavaSrc = mappings.stream().anyMatch(m -> m.relativeOrAbsolutePath().contains("src/main/java"));
        assertTrue(hasJavaSrc, "Should contain src/main/java mapping");

        // Verify display path conversion
        FileEncodingsSettings.PathMapping javaMapping = mappings.stream()
                .filter(m -> m.relativeOrAbsolutePath().contains("src/main/java"))
                .findFirst().orElseThrow();
        String display = javaMapping.getDisplayPath(root);
        assertTrue(display.contains("src\\main\\java") || display.contains("src/main/java"));

        // Verify URL conversion
        String ideaUrl = javaMapping.toIdeaUrl(root);
        assertEquals("file://$PROJECT_DIR$/src/main/java", ideaUrl);

        // Effective charset test
        Path sampleJavaFile = root.resolve("src/main/java/dev/lumina/LuminaApp.java");
        assertEquals(StandardCharsets.UTF_8, settings.getEffectiveCharset(sampleJavaFile));
    }

    @Test
    void testSettingsPageInstantiatesAndDirtyTracking() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsFileEncodingsPage page = new SettingsFileEncodingsPage();
                assertNotNull(page);
                assertFalse(page.isModified(), "Initial state should not be modified");

                latch.countDown();
            } catch (Throwable t) {
                t.printStackTrace();
                fail("Failed to instantiate SettingsFileEncodingsPage: " + t.getMessage());
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX thread timed out");
    }
}
