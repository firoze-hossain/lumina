package dev.lumina.git;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class GitLabAccountManagerTest {

    private GitLabAccountManager manager;

    @BeforeEach
    void setUp() {
        manager = GitLabAccountManager.getInstance();
        manager.revertToDefaults();
    }

    @Test
    void testDefaults() {
        assertFalse(manager.isAutoMarkFilesAsViewed());
        assertFalse(manager.isCloneUsingSsh());
        assertTrue(manager.getAccounts().isEmpty());
    }

    @Test
    void testAddRemoveAndDefaultAccount() {
        GitLabAccountManager.GitLabAccount acc1 = new GitLabAccountManager.GitLabAccount(
                "John Doe", "johndoe", "https://gitlab.com", "token1", true
        );
        GitLabAccountManager.GitLabAccount acc2 = new GitLabAccountManager.GitLabAccount(
                "Self Hosted", "admin", "https://gitlab.example.org", "token2", false
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

        manager.setAutoMarkFilesAsViewed(true);
        assertTrue(called.get());
        assertTrue(manager.isAutoMarkFilesAsViewed());

        called.set(false);
        manager.setCloneUsingSsh(true);
        assertTrue(called.get());
        assertTrue(manager.isCloneUsingSsh());

        manager.removeListener(listener);
    }
}
