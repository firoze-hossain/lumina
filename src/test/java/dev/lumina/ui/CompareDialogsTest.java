package dev.lumina.ui;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CompareDialogsTest {

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
    void testCompareWithRevisionDialog() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<CompareWithRevisionDialog> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                CompareWithRevisionDialog dlg = new CompareWithRevisionDialog(
                        null,
                        Path.of("."),
                        Path.of("pom.xml"),
                        "<project></project>",
                        (title, rev, rel, file, left, right) -> {}
                );
                ref.set(dlg);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS));

        CompareWithRevisionDialog dlg = ref.get();
        assertNotNull(dlg);
        assertTrue(dlg.getTitle().contains("pom.xml"));
        assertNotNull(dlg.getTable());
        assertNotNull(dlg.getCompareBtn());
        assertNotNull(dlg.getCancelBtn());
    }

    @Test
    void testCompareWithBranchDialog() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<CompareWithBranchDialog> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                CompareWithBranchDialog dlg = new CompareWithBranchDialog(
                        null,
                        Path.of("."),
                        Path.of("pom.xml"),
                        "<project></project>",
                        (title, rev, rel, file, left, right) -> {}
                );
                ref.set(dlg);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS));

        CompareWithBranchDialog dlg = ref.get();
        assertNotNull(dlg);
        assertTrue(dlg.getTitle().contains("pom.xml"));
        assertNotNull(dlg.getRefList());
        assertNotNull(dlg.getFilterField());
        assertNotNull(dlg.getCompareBtn());
        assertNotNull(dlg.getCancelBtn());
    }
}
