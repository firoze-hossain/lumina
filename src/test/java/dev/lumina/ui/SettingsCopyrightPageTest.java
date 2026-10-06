package dev.lumina.ui;

import dev.lumina.copyright.CopyrightManager;
import dev.lumina.copyright.CopyrightProfile;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class SettingsCopyrightPageTest {

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
    void testCopyrightPageConstructionAndDirtyState() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                CopyrightManager.getInstance().resetToDefaults();
                CopyrightManager.getInstance().addProfile(new CopyrightProfile("CompanyPolicy", true));

                SettingsCopyrightPage page = new SettingsCopyrightPage();
                assertNotNull(page);
                assertNotNull(page.getDefaultCopyrightCombo());
                assertNotNull(page.getMappingTable());

                assertTrue(page.getDefaultCopyrightCombo().getItems().contains("No copyright"));
                assertTrue(page.getDefaultCopyrightCombo().getItems().contains("CompanyPolicy"));

                assertFalse(page.isModified());

                // Dirty state listener
                boolean[] modifiedFired = {false};
                page.setOnModifiedListener(() -> modifiedFired[0] = true);

                // Add mapping
                page.addMapping("Project Files", "CompanyPolicy");
                assertTrue(page.isModified());
                assertTrue(modifiedFired[0]);

                // Reset restores clean state
                page.reset();
                assertFalse(page.isModified());

                // Change default copyright
                page.getDefaultCopyrightCombo().setValue("CompanyPolicy");
                assertTrue(page.isModified());

                // Apply
                page.apply();
                assertFalse(page.isModified());
                assertEquals("CompanyPolicy", CopyrightManager.getInstance().getDefaultProjectCopyright());
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
