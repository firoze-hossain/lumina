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
        assertEquals(GitConfirmationManager.FileCreationPolicy.ADD_SILENTLY, manager.getFileCreationPolicy());
        assertFalse(manager.isApplyCreationPolicyToExternalFiles());
        assertEquals(GitConfirmationManager.FileDeletionPolicy.ASK, manager.getFileDeletionPolicy());
        assertTrue(manager.isShowOptionsBeforeCheckout());
        assertTrue(manager.isShowOptionsBeforeUpdate());
        assertTrue(manager.isAskToUnlockReadOnlyFiles());
        assertTrue(manager.isAskConfirmationToDropCommits());

        assertFalse(manager.isCheckForServerConflicts());
        assertEquals(60, manager.getConflictCheckIntervalMinutes());
        assertFalse(manager.isHighlightFilesChangedInDays());
        assertEquals(31, manager.getHighlightDays());
        assertTrue(manager.isHighlightDirectoriesWithModifiedFiles());
        assertEquals(GitConfirmationManager.PatchCreationPolicy.ASK, manager.getPatchCreationPolicy());
        assertTrue(manager.isRestoreWorkspaceWhenSwitchingBranches());
        assertTrue(manager.isLimitHistory());
        assertEquals(1000, manager.getHistoryLimitRows());

        assertTrue(manager.isHighlightModifiedLinesInGutter());
        assertTrue(manager.isHighlightModifiedLinesInErrorStripe());
        assertTrue(manager.isHighlightWhitespaceOnlyModifications());
    }

    @Test
    void testPolicyChangeAndPersistence() {
        manager.setFileCreationPolicy(GitConfirmationManager.FileCreationPolicy.DO_NOT_ADD);
        assertEquals(GitConfirmationManager.FileCreationPolicy.DO_NOT_ADD, manager.getFileCreationPolicy());

        manager.setFileCreationPolicy(GitConfirmationManager.FileCreationPolicy.ASK);
        assertEquals(GitConfirmationManager.FileCreationPolicy.ASK, manager.getFileCreationPolicy());

        manager.setFileDeletionPolicy(GitConfirmationManager.FileDeletionPolicy.REMOVE_SILENTLY);
        assertEquals(GitConfirmationManager.FileDeletionPolicy.REMOVE_SILENTLY, manager.getFileDeletionPolicy());

        manager.setApplyCreationPolicyToExternalFiles(true);
        assertTrue(manager.isApplyCreationPolicyToExternalFiles());

        manager.setShowOptionsBeforeCheckout(false);
        assertFalse(manager.isShowOptionsBeforeCheckout());

        manager.setShowOptionsBeforeUpdate(false);
        assertFalse(manager.isShowOptionsBeforeUpdate());

        manager.setAskToUnlockReadOnlyFiles(false);
        assertFalse(manager.isAskToUnlockReadOnlyFiles());

        manager.setAskConfirmationToDropCommits(false);
        assertFalse(manager.isAskConfirmationToDropCommits());
    }

    @Test
    void testChangesAndGutterSettings() {
        manager.setCheckForServerConflicts(true);
        assertTrue(manager.isCheckForServerConflicts());
        manager.setConflictCheckIntervalMinutes(30);
        assertEquals(30, manager.getConflictCheckIntervalMinutes());

        manager.setHighlightFilesChangedInDays(true);
        assertTrue(manager.isHighlightFilesChangedInDays());
        manager.setHighlightDays(14);
        assertEquals(14, manager.getHighlightDays());

        manager.setHighlightDirectoriesWithModifiedFiles(false);
        assertFalse(manager.isHighlightDirectoriesWithModifiedFiles());

        manager.setPatchCreationPolicy(GitConfirmationManager.PatchCreationPolicy.SHOW_OPTIONS);
        assertEquals(GitConfirmationManager.PatchCreationPolicy.SHOW_OPTIONS, manager.getPatchCreationPolicy());

        manager.setRestoreWorkspaceWhenSwitchingBranches(false);
        assertFalse(manager.isRestoreWorkspaceWhenSwitchingBranches());

        manager.setLimitHistory(false);
        assertFalse(manager.isLimitHistory());
        manager.setHistoryLimitRows(500);
        assertEquals(500, manager.getHistoryLimitRows());

        manager.setHighlightModifiedLinesInGutter(false);
        assertFalse(manager.isHighlightModifiedLinesInGutter());

        manager.setHighlightModifiedLinesInErrorStripe(false);
        assertFalse(manager.isHighlightModifiedLinesInErrorStripe());

        manager.setHighlightWhitespaceOnlyModifications(false);
        assertFalse(manager.isHighlightWhitespaceOnlyModifications());
    }

    @Test
    void testBrandIsolation() {
        for (GitConfirmationManager.FileCreationPolicy p : GitConfirmationManager.FileCreationPolicy.values()) {
            assertFalse(p.getDisplayName().toLowerCase().contains("intellij"));
            assertFalse(p.getDisplayName().toLowerCase().contains("jetbrains"));
        }
        for (GitConfirmationManager.FileDeletionPolicy p : GitConfirmationManager.FileDeletionPolicy.values()) {
            assertFalse(p.getDisplayName().toLowerCase().contains("intellij"));
            assertFalse(p.getDisplayName().toLowerCase().contains("jetbrains"));
        }
        for (GitConfirmationManager.PatchCreationPolicy p : GitConfirmationManager.PatchCreationPolicy.values()) {
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
        manager.setFileCreationPolicy(GitConfirmationManager.FileCreationPolicy.ASK);
        assertTrue(notified.get());

        notified.set(false);
        manager.setHighlightDirectoriesWithModifiedFiles(false);
        assertTrue(notified.get());

        notified.set(false);
        manager.setHighlightModifiedLinesInGutter(false);
        assertTrue(notified.get());

        manager.removeListener(listener);
    }
}
