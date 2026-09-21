package dev.lumina.ui;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BackgroundImageServiceTest {

    private BackgroundImageService service;

    @BeforeEach
    void setUp() {
        service = BackgroundImageService.getInstance();
        service.clear(BackgroundImageService.Target.EDITOR_AND_TOOLS);
        service.clear(BackgroundImageService.Target.EMPTY_FRAME);
    }

    @Test
    void testTargetConfigsIndependent() {
        BackgroundImageService.TargetConfig ec = new BackgroundImageService.TargetConfig();
        ec.imagePath = "/path/to/editor.png";
        ec.opacity = 25;
        ec.scaleMode = 1;
        service.setConfig(BackgroundImageService.Target.EDITOR_AND_TOOLS, ec);

        BackgroundImageService.TargetConfig fc = new BackgroundImageService.TargetConfig();
        fc.imagePath = "/path/to/frame.png";
        fc.opacity = 50;
        fc.scaleMode = 2;
        service.setConfig(BackgroundImageService.Target.EMPTY_FRAME, fc);

        // Verify independent retrieval
        BackgroundImageService.TargetConfig retrievedE = service.getConfig(BackgroundImageService.Target.EDITOR_AND_TOOLS);
        assertEquals("/path/to/editor.png", retrievedE.imagePath);
        assertEquals(25, retrievedE.opacity);
        assertEquals(1, retrievedE.scaleMode);

        BackgroundImageService.TargetConfig retrievedF = service.getConfig(BackgroundImageService.Target.EMPTY_FRAME);
        assertEquals("/path/to/frame.png", retrievedF.imagePath);
        assertEquals(50, retrievedF.opacity);
        assertEquals(2, retrievedF.scaleMode);
    }

    @Test
    void testRecentImagesManagement() {
        service.addRecentImage("/path/1.png");
        service.addRecentImage("/path/2.png");
        service.addRecentImage("/path/1.png"); // duplicate moved to top

        assertEquals("/path/1.png", service.getRecentImages().get(0));
        assertEquals("/path/2.png", service.getRecentImages().get(1));
    }

    @Test
    void testChangeListenerNotification() {
        AtomicBoolean notified = new AtomicBoolean(false);
        service.addListener(target -> {
            if (target == BackgroundImageService.Target.EDITOR_AND_TOOLS) {
                notified.set(true);
            }
        });

        BackgroundImageService.TargetConfig ec = new BackgroundImageService.TargetConfig();
        ec.imagePath = "/test/img.png";
        service.setConfig(BackgroundImageService.Target.EDITOR_AND_TOOLS, ec);

        assertTrue(notified.get(), "Listener should receive update event");
    }

    @Test
    void testClearTarget() {
        BackgroundImageService.TargetConfig ec = new BackgroundImageService.TargetConfig();
        ec.imagePath = "/test/clear.png";
        service.setConfig(BackgroundImageService.Target.EDITOR_AND_TOOLS, ec);

        service.clear(BackgroundImageService.Target.EDITOR_AND_TOOLS);
        assertEquals("", service.getConfig(BackgroundImageService.Target.EDITOR_AND_TOOLS).imagePath);
    }
}
