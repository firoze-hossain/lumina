package dev.lumina.git;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class GitHubAccountManagerTest {

    private GitHubAccountManager manager;

    @BeforeEach
    void setUp() {
        manager = GitHubAccountManager.getInstance();
        manager.revertToDefaults();
    }

    @Test
    void testDefaults() {
        assertFalse(manager.isCloneUsingSsh());
        assertFalse(manager.isAutoMarkFilesAsViewed());
        assertTrue(manager.isEnableUnreadMarkersOnPullRequests());
        assertEquals(5, manager.getConnectionTimeoutSeconds());
    }

    @Test
    void testAddRemoveAndDefaultAccount() {
        GitHubAccountManager.GitHubAccount acc1 = new GitHubAccountManager.GitHubAccount(
                "Md. Firoze Hossain", "firoze-hossain", "github.com", "token123", "", true
        );
        GitHubAccountManager.GitHubAccount acc2 = new GitHubAccountManager.GitHubAccount(
                "Work Account", "work-dev", "github.com", "token456", "", false
        );

        manager.addAccount(acc1);
        manager.addAccount(acc2);

        assertEquals(2, manager.getAccounts().size());
        assertEquals(acc1, manager.getDefaultAccount());

        manager.setDefaultAccount(acc2);
        assertEquals(acc2, manager.getDefaultAccount());

        manager.removeAccount(acc1);
        assertEquals(1, manager.getAccounts().size());
        assertEquals(acc2, manager.getDefaultAccount());
    }

    @Test
    void testOptionsAndListeners() {
        AtomicBoolean called = new AtomicBoolean(false);
        Runnable listener = () -> called.set(true);
        manager.addListener(listener);

        manager.setCloneUsingSsh(true);
        assertTrue(called.get());
        assertTrue(manager.isCloneUsingSsh());

        called.set(false);
        manager.setAutoMarkFilesAsViewed(true);
        assertTrue(called.get());
        assertTrue(manager.isAutoMarkFilesAsViewed());

        called.set(false);
        manager.setEnableUnreadMarkersOnPullRequests(false);
        assertTrue(called.get());
        assertFalse(manager.isEnableUnreadMarkersOnPullRequests());

        called.set(false);
        manager.setConnectionTimeoutSeconds(15);
        assertTrue(called.get());
        assertEquals(15, manager.getConnectionTimeoutSeconds());

        manager.removeListener(listener);
    }
}
