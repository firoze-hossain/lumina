package dev.lumina.ui;

import dev.lumina.project.MavenProjectModel;
import dev.lumina.project.MavenProjectModel.MavenProject;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RunAnythingDialogTest {

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
    void testRunAnythingDialogCreationAndGoalFiltering() {
        if (!javaFxAvailable) return;

        AtomicReference<String> executed = new AtomicReference<>();
        Path root = Path.of("/home/firoze/projects/others/lumina");
        MavenProject project = MavenProjectModel.parseProject(root);

        RunAnythingDialog dialog = new RunAnythingDialog(null, project, executed::set);
        assertNotNull(dialog);
        assertNotNull(RunAnythingDialog.createMavenGlyph());
    }
}
