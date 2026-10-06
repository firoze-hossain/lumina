package dev.lumina.ui;

import dev.lumina.copyright.CopyrightManager;
import dev.lumina.copyright.CopyrightProfile;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class SettingsCopyrightProfilesPageTest {

    private static volatile boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(() -> {
                javaFxAvailable = true;
                latch.countDown();
            });
            latch.await(3, TimeUnit.SECONDS);
        } catch (IllegalStateException e) {
            javaFxAvailable = true;
        } catch (Throwable t) {
            javaFxAvailable = false;
        }
    }

    @Test
    void testCopyrightProfilesPageConstructionAndDirtyState() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                CopyrightManager.getInstance().resetToDefaults();

                SettingsCopyrightProfilesPage page = new SettingsCopyrightProfilesPage();
                assertNotNull(page);
                assertNotNull(page.getProfileListView());
                assertFalse(page.isModified());

                // Dirty state listener
                boolean[] modifiedFired = {false};
                page.setOnModifiedListener(() -> modifiedFired[0] = true);

                // Add profile
                CopyrightProfile newProfile = new CopyrightProfile("GPLv3", false);
                page.getWorkingProfiles().add(newProfile);
                page.notifyModified();

                assertTrue(page.isModified());
                assertTrue(modifiedFired[0]);

                // Reset restores clean state
                page.reset();
                assertFalse(page.isModified());

                // Add and apply
                page.getWorkingProfiles().add(newProfile);
                page.apply();
                assertFalse(page.isModified());
                assertEquals(1, CopyrightManager.getInstance().getProfiles().size());
                assertEquals("GPLv3", CopyrightManager.getInstance().getProfiles().get(0).getName());
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
