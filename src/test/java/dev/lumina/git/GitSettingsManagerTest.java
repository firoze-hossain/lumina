package dev.lumina.git;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class GitSettingsManagerTest {

    private GitSettingsManager manager;

    @BeforeEach
    void setUp() {
        manager = GitSettingsManager.getInstance();
        manager.revertToDefaults();
    }

    @Test
    void testDefaultsMatchingIntelliJ() {
        // Executable
        assertEquals("", manager.getGitExecutablePath());
        assertFalse(manager.isSetPathOnlyForProject());
        assertFalse(manager.isAutoExcludeIgnoredDirectories());

        // Commit
        assertFalse(manager.isEnableStagingArea());
        assertTrue(manager.isWarnCrlf());
        assertTrue(manager.isWarnDetachedHead());
        assertTrue(manager.isWarnFilesLargerThanEnabled());
        assertEquals(50, manager.getWarnFilesLargerThanMb());
        assertTrue(manager.isWarnCrossPlatformFilenames());
        assertTrue(manager.isAddCherryPickSuffix());
        assertFalse(manager.isSignCommitsWithGpg());
        assertEquals("", manager.getGpgKeyId());

        // Push
        assertFalse(manager.isAutoUpdateOnRejectedPush());
        assertTrue(manager.isShowPushDialog());
        assertFalse(manager.isShowPushDialogOnlyProtected());
        assertEquals("master;main", manager.getProtectedBranches());
        assertTrue(manager.isLoadBranchProtectionFromGitHub());

        // Update
        assertEquals(GitSettingsManager.UpdateMethod.MERGE, manager.getUpdateMethod());
        assertEquals(GitSettingsManager.CleanWorkingTreeMethod.SHELVE, manager.getCleanWorkingTreeMethod());
        assertEquals(GitSettingsManager.FilterUpdatePaths.ALL, manager.getFilterUpdatePaths());
        assertEquals(GitSettingsManager.IncomingCommitsCheck.AUTO, manager.getIncomingCommitsCheck());
        assertEquals(GitSettingsManager.FetchTagsMode.AUTO_FOLLOW_GIT_CONFIG, manager.getFetchTagsMode());
        assertFalse(manager.isUseCredentialHelper());

        // Stash
        assertFalse(manager.isCombineStashesAndShelves());
        assertEquals(GitSettingsManager.StashDiffComparison.LOCAL_VERSION, manager.getStashDiffComparison());
        assertTrue(manager.isActivateVirtualenvForHooks());
    }

    @Test
    void testAutoDetectionAndEffectiveExecutable() {
        String autoDetected = manager.getAutoDetectedGitPath();
        assertNotNull(autoDetected);
        assertFalse(autoDetected.isBlank());

        assertEquals(autoDetected, manager.getEffectiveGitExecutable());

        manager.setGitExecutablePath("/custom/bin/git");
        assertEquals("/custom/bin/git", manager.getEffectiveGitExecutable());

        manager.setGitExecutablePath("");
        assertEquals(autoDetected, manager.getEffectiveGitExecutable());
    }

    @Test
    void testTestGitExecutable() {
        GitSettingsManager.TestResult res = manager.testGitExecutable(null);
        assertTrue(res.success());
        assertTrue(res.message().startsWith("Git version is ") || res.message().contains("version"));
    }

    @Test
    void testTogglingAndListeners() {
        AtomicBoolean called = new AtomicBoolean(false);
        Runnable listener = () -> called.set(true);
        manager.addListener(listener);

        manager.setEnableStagingArea(true);
        assertTrue(called.get());
        assertTrue(manager.isEnableStagingArea());

        called.set(false);
        manager.setWarnFilesLargerThanMb(100);
        assertTrue(called.get());
        assertEquals(100, manager.getWarnFilesLargerThanMb());

        called.set(false);
        manager.setUpdateMethod(GitSettingsManager.UpdateMethod.REBASE);
        assertTrue(called.get());
        assertEquals(GitSettingsManager.UpdateMethod.REBASE, manager.getUpdateMethod());

        called.set(false);
        manager.setCleanWorkingTreeMethod(GitSettingsManager.CleanWorkingTreeMethod.STASH);
        assertTrue(called.get());
        assertEquals(GitSettingsManager.CleanWorkingTreeMethod.STASH, manager.getCleanWorkingTreeMethod());

        called.set(false);
        manager.setFetchTagsMode(GitSettingsManager.FetchTagsMode.SYNC_PRUNE_TAGS);
        assertTrue(called.get());
        assertEquals(GitSettingsManager.FetchTagsMode.SYNC_PRUNE_TAGS, manager.getFetchTagsMode());

        called.set(false);
        manager.setStashDiffComparison(GitSettingsManager.StashDiffComparison.PARENT_COMMIT);
        assertTrue(called.get());
        assertEquals(GitSettingsManager.StashDiffComparison.PARENT_COMMIT, manager.getStashDiffComparison());

        manager.removeListener(listener);
    }
}
