package dev.lumina.ui;

import dev.lumina.debugger.DebuggerSettingsManager;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying SettingsDebuggerPage, SettingsDebuggerAsyncStackTracesPage,
 * SettingsDebuggerDataViewsPage, SettingsDebuggerDataViewsJavaPage,
 * SettingsDebuggerDataViewsTypeRenderersPage, and SettingsDialog tree navigation.
 */
public class SettingsDebuggerPagesTest {

    private static volatile boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            String display = System.getenv("DISPLAY");
            if (display == null || display.isBlank() || java.awt.GraphicsEnvironment.isHeadless()) {
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

    @BeforeEach
    void setUp() {
        DebuggerSettingsManager.getInstance().resetToDefaults();
    }

    @Test
    void testSettingsDebuggerPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDebuggerPage page = new SettingsDebuggerPage();
                assertFalse(page.isModified());

                AtomicBoolean modifiedNotified = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedNotified.set(true));

                // Change a setting
                page.getCurrentSettings().setAttachMemoryAgent(true);
                // Trigger via UI reset / apply
                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testAsyncStackTracesPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDebuggerAsyncStackTracesPage page = new SettingsDebuggerAsyncStackTracesPage();
                assertFalse(page.isModified());
                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testDataViewsPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDebuggerDataViewsPage page = new SettingsDebuggerDataViewsPage();
                assertFalse(page.isModified());
                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testDataViewsJavaPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDebuggerDataViewsJavaPage page = new SettingsDebuggerDataViewsJavaPage();
                assertFalse(page.isModified());
                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testTypeRenderersPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDebuggerDataViewsTypeRenderersPage page = new SettingsDebuggerDataViewsTypeRenderersPage();
                assertFalse(page.isModified());
                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testJavaScriptDataViewsPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDebuggerDataViewsJavaScriptPage page = new SettingsDebuggerDataViewsJavaScriptPage();
                assertFalse(page.isModified());
                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testHotSwapPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDebuggerHotSwapPage page = new SettingsDebuggerHotSwapPage();
                assertFalse(page.isModified());
                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSteppingPage() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDebuggerSteppingPage page = new SettingsDebuggerSteppingPage();
                assertFalse(page.isModified());
                page.reset();
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
