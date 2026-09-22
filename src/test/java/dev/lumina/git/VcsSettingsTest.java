package dev.lumina.git;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class VcsSettingsTest {

    private VcsCommitSettings commitSettings;
    private VcsChangelistSettings changelistSettings;

    @BeforeEach
    void setUp() {
        commitSettings = VcsCommitSettings.getInstance();
        commitSettings.revertToDefaults();

        changelistSettings = VcsChangelistSettings.getInstance();
        changelistSettings.revertToDefaults();
    }

    @Test
    void testCommitSettingsDefaults() {
        assertFalse(commitSettings.isClearInitialCommitMessage());

        assertFalse(commitSettings.isInspectionBlankLine());
        assertFalse(commitSettings.isInspectionLimitBodyLine());
        assertFalse(commitSettings.isInspectionLimitSubjectLine());
        assertTrue(commitSettings.isInspectionSpelling());
        assertEquals(VcsCommitSettings.InspectionSeverity.WARNING, commitSettings.getInspectionSpellingSeverity());

        assertFalse(commitSettings.isCheckReformatCode());
        assertFalse(commitSettings.isCheckRearrangeCode());
        assertFalse(commitSettings.isCheckOptimizeImports());
        assertFalse(commitSettings.isCheckCleanup());
        assertFalse(commitSettings.isCheckUpdateCopyright());
        assertFalse(commitSettings.isCheckMaliciousDependencies());
        assertFalse(commitSettings.isCheckRunRustfmt());
        assertTrue(commitSettings.isCheckGoFmt());

        assertTrue(commitSettings.isCheckAnalyzeCode());
        assertTrue(commitSettings.isCheckTodo());
        assertFalse(commitSettings.isCheckRunConfiguration());
        assertTrue(commitSettings.isRunAdvancedChecksAfterCommit());
    }

    @Test
    void testCommitSettingsChangesAndListeners() {
        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        commitSettings.addListener(listener);

        commitSettings.setClearInitialCommitMessage(true);
        assertTrue(commitSettings.isClearInitialCommitMessage());
        assertTrue(notified.get());

        notified.set(false);
        commitSettings.setInspectionBlankLine(true);
        assertTrue(commitSettings.isInspectionBlankLine());
        assertTrue(notified.get());

        commitSettings.setInspectionBlankLineSeverity(VcsCommitSettings.InspectionSeverity.ERROR);
        assertEquals(VcsCommitSettings.InspectionSeverity.ERROR, commitSettings.getInspectionBlankLineSeverity());

        commitSettings.setCheckReformatCode(true);
        assertTrue(commitSettings.isCheckReformatCode());

        commitSettings.setCleanupProfile("Strict");
        assertEquals("Strict", commitSettings.getCleanupProfile());

        commitSettings.removeListener(listener);
    }

    @Test
    void testChangelistSettingsDefaults() {
        assertFalse(changelistSettings.isCreateChangelistsAutomatically());
        assertTrue(changelistSettings.isAllowPuttingChangesWithinOneFileIntoDifferentChangelists());

        assertFalse(changelistSettings.isHighlightFilesFromInactiveChangelists());
        assertFalse(changelistSettings.isShowDialogOnAttemptToEditFileFromInactiveChangelist());
        assertEquals(VcsChangelistSettings.EmptyChangelistInactiveAction.SHOW_OPTIONS,
                changelistSettings.getEmptyChangelistInactiveAction());

        assertTrue(changelistSettings.isHighlightFilesWithChangelistConflicts());
        assertTrue(changelistSettings.getIgnoredConflicts().isEmpty());
    }

    @Test
    void testChangelistSettingsChangesAndIgnoredConflicts() {
        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        changelistSettings.addListener(listener);

        changelistSettings.setCreateChangelistsAutomatically(true);
        assertTrue(changelistSettings.isCreateChangelistsAutomatically());
        assertTrue(notified.get());

        changelistSettings.setEmptyChangelistInactiveAction(VcsChangelistSettings.EmptyChangelistInactiveAction.REMOVE_SILENTLY);
        assertEquals(VcsChangelistSettings.EmptyChangelistInactiveAction.REMOVE_SILENTLY,
                changelistSettings.getEmptyChangelistInactiveAction());

        changelistSettings.addIgnoredConflict("src/Main.java");
        assertEquals(1, changelistSettings.getIgnoredConflicts().size());
        assertEquals("src/Main.java", changelistSettings.getIgnoredConflicts().get(0));

        changelistSettings.clearIgnoredConflicts();
        assertTrue(changelistSettings.getIgnoredConflicts().isEmpty());

        changelistSettings.removeListener(listener);
    }
}
