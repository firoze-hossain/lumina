package dev.lumina.ui;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class GitIconsTest {

    private static volatile boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            String display = System.getenv("DISPLAY");
            if (display == null || display.isBlank() || java.awt.GraphicsEnvironment.isHeadless()) {
                return;
            }
            Platform.startup(() -> javaFxAvailable = true);
            javaFxAvailable = true;
        } catch (Throwable ignored) {}
    }

    @Test
    void testAllIcons() {
        if (!javaFxAvailable) return;

        assertNotNull(GitIcons.gitHubIcon());
        assertNotNull(GitIcons.gitLabIcon());
        assertNotNull(GitIcons.gitBranchIcon());
        assertNotNull(GitIcons.syncIcon());
        assertNotNull(GitIcons.globeIcon());
        assertNotNull(GitIcons.clockIcon());
        assertNotNull(GitIcons.diffIcon());
        assertNotNull(GitIcons.plusIcon());
        assertNotNull(GitIcons.minusIcon());
        assertNotNull(GitIcons.editIcon());
        assertNotNull(GitIcons.folderIcon());
    }
}
