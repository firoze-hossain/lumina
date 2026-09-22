package dev.lumina.git;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class VcsLogSettingsManagerTest {

    private VcsLogSettingsManager manager;

    @BeforeEach
    void setUp() {
        manager = VcsLogSettingsManager.getInstance();
        manager.revertToDefaults();
    }

    @Test
    void testDefaultsMatchingIntelliJ() {
        // View options
        assertTrue(manager.isShowOnlyFirstRef());
        assertFalse(manager.isShowTagNames());
        assertFalse(manager.isShowCommitTime());
        assertFalse(manager.isShowLeftReferences());
        assertFalse(manager.isDisplayMergedCommitsSeparately());
        assertFalse(manager.isShowDiffPreview());
        assertEquals(VcsLogSettingsManager.DiffPreviewLocation.BOTTOM, manager.getDiffPreviewLocation());

        // View options visible columns
        assertTrue(manager.isAuthorVisible());
        assertFalse(manager.isHashVisible());
        assertTrue(manager.isDateVisible());
        assertFalse(manager.isGpgSignatureVisible());
        assertTrue(manager.isGithubCommitChecksVisible());

        // Indexing
        assertTrue(manager.isEnableIndexing());

        // File history
        assertFalse(manager.isFileHistoryDetailsPanel());
        assertFalse(manager.isFileHistoryShowFileNames());
        assertTrue(manager.isFileHistoryDiffPreview());
        assertEquals(VcsLogSettingsManager.DiffPreviewLocation.RIGHT, manager.getFileHistoryDiffPreviewLocation());

        // File history visible columns
        assertTrue(manager.isFileHistoryAuthorVisible());
        assertFalse(manager.isFileHistoryHashVisible());
        assertTrue(manager.isFileHistoryDateVisible());
        assertFalse(manager.isFileHistoryGpgSignatureVisible());
        assertTrue(manager.isFileHistoryGithubCommitChecksVisible());
    }

    @Test
    void testSettersAndListeners() {
        AtomicBoolean called = new AtomicBoolean(false);
        Runnable listener = () -> called.set(true);
        manager.addListener(listener);

        manager.setShowTagNames(true);
        assertTrue(called.get());
        assertTrue(manager.isShowTagNames());

        called.set(false);
        manager.setDiffPreviewLocation(VcsLogSettingsManager.DiffPreviewLocation.RIGHT);
        assertTrue(called.get());
        assertEquals(VcsLogSettingsManager.DiffPreviewLocation.RIGHT, manager.getDiffPreviewLocation());

        called.set(false);
        manager.setHashVisible(true);
        assertTrue(called.get());
        assertTrue(manager.isHashVisible());

        called.set(false);
        manager.setEnableIndexing(false);
        assertTrue(called.get());
        assertFalse(manager.isEnableIndexing());

        called.set(false);
        manager.setFileHistoryDiffPreviewLocation(VcsLogSettingsManager.DiffPreviewLocation.BOTTOM);
        assertTrue(called.get());
        assertEquals(VcsLogSettingsManager.DiffPreviewLocation.BOTTOM, manager.getFileHistoryDiffPreviewLocation());

        manager.removeListener(listener);
    }
}
