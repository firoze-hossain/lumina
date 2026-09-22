package dev.lumina.git;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class GitConfirmationManagerTest {

    private GitConfirmationManager manager;

    @BeforeEach
    void setUp() {
        manager = GitConfirmationManager.getInstance();
        manager.revertToDefaults();
    }

    @Test
    void testDefaults() {
        assertEquals(GitConfirmationManager.FileCreationPolicy.ASK, manager.getFileCreationPolicy());
        assertEquals(GitConfirmationManager.FileDeletionPolicy.ASK, manager.getFileDeletionPolicy());
        assertTrue(manager.isRestoreWorkspaceOnBranchSwitch());
        assertTrue(manager.isShowPromptWhenCheckoutFiles());
        assertFalse(manager.isClearUnversionedFilesOnReload());
    }

    @Test
    void testPolicyChangeAndPersistence() {
        manager.setFileCreationPolicy(GitConfirmationManager.FileCreationPolicy.ADD_SILENTLY);
        assertEquals(GitConfirmationManager.FileCreationPolicy.ADD_SILENTLY, manager.getFileCreationPolicy());

        manager.setFileCreationPolicy(GitConfirmationManager.FileCreationPolicy.DO_NOT_ADD);
        assertEquals(GitConfirmationManager.FileCreationPolicy.DO_NOT_ADD, manager.getFileCreationPolicy());

        manager.setFileDeletionPolicy(GitConfirmationManager.FileDeletionPolicy.REMOVE_SILENTLY);
        assertEquals(GitConfirmationManager.FileDeletionPolicy.REMOVE_SILENTLY, manager.getFileDeletionPolicy());

        manager.setRestoreWorkspaceOnBranchSwitch(false);
        assertFalse(manager.isRestoreWorkspaceOnBranchSwitch());

        manager.setShowPromptWhenCheckoutFiles(false);
        assertFalse(manager.isShowPromptWhenCheckoutFiles());

        manager.setClearUnversionedFilesOnReload(true);
        assertTrue(manager.isClearUnversionedFilesOnReload());
    }

    @Test
    void testBrandIsolation() {
        // Strict requirement: zero occurrences of "JetBrains" or "IntelliJ" across policies
        for (GitConfirmationManager.FileCreationPolicy p : GitConfirmationManager.FileCreationPolicy.values()) {
            assertFalse(p.getDisplayName().toLowerCase().contains("intellij"));
            assertFalse(p.getDisplayName().toLowerCase().contains("jetbrains"));
        }
        for (GitConfirmationManager.FileDeletionPolicy p : GitConfirmationManager.FileDeletionPolicy.values()) {
            assertFalse(p.getDisplayName().toLowerCase().contains("intellij"));
            assertFalse(p.getDisplayName().toLowerCase().contains("jetbrains"));
        }
    }

    @Test
    void testListenerNotification() {
        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addListener(listener);

        notified.set(false);
        manager.setFileCreationPolicy(GitConfirmationManager.FileCreationPolicy.ADD_SILENTLY);
        assertTrue(notified.get());

        notified.set(false);
        manager.setFileDeletionPolicy(GitConfirmationManager.FileDeletionPolicy.REMOVE_SILENTLY);
        assertTrue(notified.get());

        manager.removeListener(listener);
    }
}
