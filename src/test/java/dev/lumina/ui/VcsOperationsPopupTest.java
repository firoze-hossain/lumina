package dev.lumina.ui;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class VcsOperationsPopupTest {

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
    void testPopupItemsGenerationAndMnemonics() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<VcsOperationsPopup> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                VcsOperationsPopup popup = new VcsOperationsPopup(
                        new VcsOperationsPopup.VcsContext() {
                            @Override public Path getProjectRoot() { return Path.of("/tmp/test-project"); }
                            @Override public Path getActiveFile() { return Path.of("/tmp/test-project/TrustedLocationsManager.java"); }
                            @Override public String getActiveBranch() { return "master"; }
                        },
                        new VcsOperationsPopup.VcsCallbacks() {
                            @Override public void onCommit() {}
                            @Override public void onRollback() {}
                            @Override public void onShowHistory(Path file) {}
                            @Override public void onShowDiff(Path file) {}
                            @Override public void onBranches() {}
                            @Override public void onPush() {}
                            @Override public void onStash() {}
                            @Override public void onUnstash() {}
                            @Override public void onCopyBranchName(String branch) {}
                        }
                );
                popup.refreshItems();
                ref.set(popup);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS));

        VcsOperationsPopup popup = ref.get();
        assertNotNull(popup);

        var items = popup.getCurrentItems();
        assertFalse(items.isEmpty());

        // Verify mnemonics 1 through 9, 0
        VcsOperationsPopup.VcsItem item1 = items.stream().filter(i -> "1".equals(i.getMnemonic())).findFirst().orElse(null);
        assertNotNull(item1);
        assertEquals("Commit...", item1.getText());
        assertEquals("Ctrl+K", item1.getShortcut());

        VcsOperationsPopup.VcsItem item3 = items.stream().filter(i -> "3".equals(i.getMnemonic())).findFirst().orElse(null);
        assertNotNull(item3);
        assertEquals("Rollback...", item3.getText());
        assertEquals("Ctrl+Alt+Z", item3.getShortcut());

        // Context-aware file history label
        VcsOperationsPopup.VcsItem item4 = items.stream().filter(i -> "4".equals(i.getMnemonic())).findFirst().orElse(null);
        assertNotNull(item4);
        assertEquals("Show History for TrustedLocationsManager.java", item4.getText());

        VcsOperationsPopup.VcsItem item6 = items.stream().filter(i -> "6".equals(i.getMnemonic())).findFirst().orElse(null);
        assertNotNull(item6);
        assertEquals("Show Diff", item6.getText());
        assertEquals("Ctrl+D", item6.getShortcut());

        VcsOperationsPopup.VcsItem item7 = items.stream().filter(i -> "7".equals(i.getMnemonic())).findFirst().orElse(null);
        assertNotNull(item7);
        assertEquals("Branches...", item7.getText());
        assertEquals("Ctrl+Shift+`", item7.getShortcut());

        VcsOperationsPopup.VcsItem item8 = items.stream().filter(i -> "8".equals(i.getMnemonic())).findFirst().orElse(null);
        assertNotNull(item8);
        assertEquals("Push...", item8.getText());
        assertEquals("Ctrl+Shift+K", item8.getShortcut());

        VcsOperationsPopup.VcsItem item9 = items.stream().filter(i -> "9".equals(i.getMnemonic())).findFirst().orElse(null);
        assertNotNull(item9);
        assertEquals("Stash Changes...", item9.getText());

        VcsOperationsPopup.VcsItem item0 = items.stream().filter(i -> "0".equals(i.getMnemonic())).findFirst().orElse(null);
        assertNotNull(item0);
        assertEquals("Unstash Changes...", item0.getText());

        VcsOperationsPopup.VcsItem copyBranch = items.stream().filter(i -> "Copy Branch Name".equals(i.getText())).findFirst().orElse(null);
        assertNotNull(copyBranch);
    }

    @Test
    void testDynamicFallbackWhenNoActiveFile() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<VcsOperationsPopup> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                VcsOperationsPopup popup = new VcsOperationsPopup(
                        new VcsOperationsPopup.VcsContext() {
                            @Override public Path getProjectRoot() { return Path.of("/tmp/test-project"); }
                            @Override public Path getActiveFile() { return null; }
                            @Override public String getActiveBranch() { return "feature/test"; }
                        },
                        new VcsOperationsPopup.VcsCallbacks() {
                            @Override public void onCommit() {}
                            @Override public void onRollback() {}
                            @Override public void onShowHistory(Path file) {}
                            @Override public void onShowDiff(Path file) {}
                            @Override public void onBranches() {}
                            @Override public void onPush() {}
                            @Override public void onStash() {}
                            @Override public void onUnstash() {}
                            @Override public void onCopyBranchName(String branch) {}
                        }
                );
                popup.refreshItems();
                ref.set(popup);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS));

        VcsOperationsPopup popup = ref.get();
        assertNotNull(popup);

        var items = popup.getCurrentItems();
        VcsOperationsPopup.VcsItem item4 = items.stream().filter(i -> "4".equals(i.getMnemonic())).findFirst().orElse(null);
        assertNotNull(item4);
        assertEquals("Show History", item4.getText(), "Should display 'Show History' when no file is active");
    }

    @Test
    void testActionCallbacksExecution() throws Exception {
        if (!javaFxAvailable) return;

        AtomicBoolean commitCalled = new AtomicBoolean(false);
        AtomicBoolean copyBranchCalled = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<VcsOperationsPopup> ref = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                VcsOperationsPopup popup = new VcsOperationsPopup(
                        new VcsOperationsPopup.VcsContext() {
                            @Override public Path getProjectRoot() { return Path.of("/tmp/test-project"); }
                            @Override public Path getActiveFile() { return Path.of("/tmp/test-project/Main.java"); }
                            @Override public String getActiveBranch() { return "master"; }
                        },
                        new VcsOperationsPopup.VcsCallbacks() {
                            @Override public void onCommit() { commitCalled.set(true); }
                            @Override public void onRollback() {}
                            @Override public void onShowHistory(Path file) {}
                            @Override public void onShowDiff(Path file) {}
                            @Override public void onBranches() {}
                            @Override public void onPush() {}
                            @Override public void onStash() {}
                            @Override public void onUnstash() {}
                            @Override public void onCopyBranchName(String branch) {
                                if ("master".equals(branch)) copyBranchCalled.set(true);
                            }
                        }
                );
                popup.refreshItems();
                ref.set(popup);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS));

        VcsOperationsPopup popup = ref.get();
        var items = popup.getCurrentItems();

        VcsOperationsPopup.VcsItem commitItem = items.stream().filter(i -> "1".equals(i.getMnemonic())).findFirst().orElse(null);
        assertNotNull(commitItem);
        assertNotNull(commitItem.getAction());
        commitItem.getAction().run();
        assertTrue(commitCalled.get());

        VcsOperationsPopup.VcsItem copyItem = items.stream().filter(i -> "Copy Branch Name".equals(i.getText())).findFirst().orElse(null);
        assertNotNull(copyItem);
        assertNotNull(copyItem.getAction());
        copyItem.getAction().run();
        assertTrue(copyBranchCalled.get());
    }
}
