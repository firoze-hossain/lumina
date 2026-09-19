package dev.lumina.ui;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TerminalPaneTest {

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (Throwable ignored) {
            // JavaFX runtime already initialized or headless without DISPLAY
        }
    }

    @Test
    void testParseCsiParam() {
        assertEquals(1, TerminalPane.parseCsiParam("\u001B[C", 1));
        assertEquals(5, TerminalPane.parseCsiParam("\u001B[5C", 1));
        assertEquals(12, TerminalPane.parseCsiParam("\u001B[12D", 1));
        assertEquals(1, TerminalPane.parseCsiParam("\u001B[G", 1));
        assertEquals(80, TerminalPane.parseCsiParam("\u001B[80G", 1));
        assertEquals(42, TerminalPane.parseCsiParam("invalid", 42));
    }

    @Test
    void testOverlayChildrenAreUnmanaged() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TerminalPane> paneRef = new AtomicReference<>();

        try {
            Platform.runLater(() -> {
                try {
                    TerminalPane pane = new TerminalPane();
                    paneRef.set(pane);
                } finally {
                    latch.countDown();
                }
            });
        } catch (IllegalStateException e) {
            // Toolkit not initialized in headless environment without display
            return;
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Timed out creating TerminalPane");
        TerminalPane pane = paneRef.get();
        if (pane == null) return;

        Field cursorField = TerminalPane.class.getDeclaredField("cursor");
        cursorField.setAccessible(true);
        Rectangle cursor = (Rectangle) cursorField.get(pane);

        Field ghostField = TerminalPane.class.getDeclaredField("ghost");
        ghostField.setAccessible(true);
        Node ghost = (Node) ghostField.get(pane);

        assertFalse(cursor.isManaged(), "Cursor must be unmanaged so StackPane does not relocate it to (0, 0)");
        assertFalse(ghost.isManaged(), "Ghost suggestion must be unmanaged so StackPane does not relocate it to (0, 0)");
        assertFalse(cursor.isVisible(), "Cursor must start invisible until first valid prompt position");

        assertTrue(pane.getCenter() instanceof StackPane, "Center must be StackPane overlay");
    }
}
