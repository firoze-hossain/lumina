package dev.lumina.git;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Central singleton managing Version Control Confirmation policies and settings.
 * Controls:
 * - "When files are created" (Add silently / Do not add / Ask)
 * - "When files are deleted" (Remove silently / Do not remove / Ask)
 * - Confirmation checkboxes (Checkout, Update, Read-only unlock, Drop commits)
 * - Changes settings (Server conflict check interval, changed files days highlight,
 *   Project tree directory highlight, patch creation, workspace restore, history row limit)
 * - Gutter settings (Gutter change markers, error stripe on scrollbar, whitespace-only modifications)
 *
 * Backed by Java Preferences for dynamic persistence across IDE sessions.
 */
public class GitConfirmationManager {

    public enum FileCreationPolicy {
        ADD_SILENTLY("Add silently"),
        DO_NOT_ADD("Do not add"),
        ASK("Ask");

        private final String displayName;

        FileCreationPolicy(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum FileDeletionPolicy {
        REMOVE_SILENTLY("Remove silently"),
        DO_NOT_REMOVE("Do not remove"),
        ASK("Ask");

        private final String displayName;

        FileDeletionPolicy(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum PatchCreationPolicy {
        SHOW_OPTIONS("Show options"),
        ASK("Ask"),
        DO_NOT_ASK("Do not ask");

        private final String displayName;

        PatchCreationPolicy(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private static final GitConfirmationManager INSTANCE = new GitConfirmationManager();

    private final Preferences prefs = Preferences.userNodeForPackage(GitConfirmationManager.class);
    private final List<Runnable> listeners = new ArrayList<>();

    // Confirmation section
    private FileCreationPolicy fileCreationPolicy = FileCreationPolicy.ADD_SILENTLY;
    private boolean applyCreationPolicyToExternalFiles = false;
    private FileDeletionPolicy fileDeletionPolicy = FileDeletionPolicy.ASK;
    private boolean showOptionsBeforeCheckout = true;
    private boolean showOptionsBeforeUpdate = true;
    private boolean askToUnlockReadOnlyFiles = true;
    private boolean askConfirmationToDropCommits = true;

    // Changes section
    private boolean checkForServerConflicts = false;
    private int conflictCheckIntervalMinutes = 60;
    private boolean highlightFilesChangedInDays = false;
    private int highlightDays = 31;
    private boolean highlightDirectoriesWithModifiedFiles = true;
    private PatchCreationPolicy patchCreationPolicy = PatchCreationPolicy.ASK;
    private boolean restoreWorkspaceWhenSwitchingBranches = true;
    private boolean limitHistory = true;
    private int historyLimitRows = 1000;

    // Gutter section
    private boolean highlightModifiedLinesInGutter = true;
    private boolean highlightModifiedLinesInErrorStripe = true;
    private boolean highlightWhitespaceOnlyModifications = true;

    // Legacy compatibility fields
    private boolean showPromptWhenCheckoutFiles = true;
    private boolean clearUnversionedFilesOnReload = false;

    private GitConfirmationManager() {
        loadPreferences();
    }

    public static GitConfirmationManager getInstance() {
        return INSTANCE;
    }

    // --- Confirmation getters and setters ---

    public synchronized FileCreationPolicy getFileCreationPolicy() {
        return fileCreationPolicy;
    }

    public synchronized void setFileCreationPolicy(FileCreationPolicy policy) {
        if (policy != null && this.fileCreationPolicy != policy) {
            this.fileCreationPolicy = policy;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isApplyCreationPolicyToExternalFiles() {
        return applyCreationPolicyToExternalFiles;
    }

    public synchronized void setApplyCreationPolicyToExternalFiles(boolean val) {
        if (this.applyCreationPolicyToExternalFiles != val) {
            this.applyCreationPolicyToExternalFiles = val;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized FileDeletionPolicy getFileDeletionPolicy() {
        return fileDeletionPolicy;
    }

    public synchronized void setFileDeletionPolicy(FileDeletionPolicy policy) {
        if (policy != null && this.fileDeletionPolicy != policy) {
            this.fileDeletionPolicy = policy;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isShowOptionsBeforeCheckout() {
        return showOptionsBeforeCheckout;
    }

    public synchronized void setShowOptionsBeforeCheckout(boolean val) {
        if (this.showOptionsBeforeCheckout != val) {
            this.showOptionsBeforeCheckout = val;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isShowOptionsBeforeUpdate() {
        return showOptionsBeforeUpdate;
    }

    public synchronized void setShowOptionsBeforeUpdate(boolean val) {
        if (this.showOptionsBeforeUpdate != val) {
            this.showOptionsBeforeUpdate = val;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isAskToUnlockReadOnlyFiles() {
        return askToUnlockReadOnlyFiles;
    }

    public synchronized void setAskToUnlockReadOnlyFiles(boolean val) {
        if (this.askToUnlockReadOnlyFiles != val) {
            this.askToUnlockReadOnlyFiles = val;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isAskConfirmationToDropCommits() {
        return askConfirmationToDropCommits;
    }

    public synchronized void setAskConfirmationToDropCommits(boolean val) {
        if (this.askConfirmationToDropCommits != val) {
            this.askConfirmationToDropCommits = val;
            savePreferences();
            notifyListeners();
        }
    }

    // --- Changes section getters and setters ---

    public synchronized boolean isCheckForServerConflicts() {
        return checkForServerConflicts;
    }

    public synchronized void setCheckForServerConflicts(boolean val) {
        if (this.checkForServerConflicts != val) {
            this.checkForServerConflicts = val;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized int getConflictCheckIntervalMinutes() {
        return conflictCheckIntervalMinutes;
    }

    public synchronized void setConflictCheckIntervalMinutes(int minutes) {
        if (this.conflictCheckIntervalMinutes != minutes && minutes > 0) {
            this.conflictCheckIntervalMinutes = minutes;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isHighlightFilesChangedInDays() {
        return highlightFilesChangedInDays;
    }

    public synchronized void setHighlightFilesChangedInDays(boolean val) {
        if (this.highlightFilesChangedInDays != val) {
            this.highlightFilesChangedInDays = val;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized int getHighlightDays() {
        return highlightDays;
    }

    public synchronized void setHighlightDays(int days) {
        if (this.highlightDays != days && days > 0) {
            this.highlightDays = days;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isHighlightDirectoriesWithModifiedFiles() {
        return highlightDirectoriesWithModifiedFiles;
    }

    public synchronized void setHighlightDirectoriesWithModifiedFiles(boolean val) {
        if (this.highlightDirectoriesWithModifiedFiles != val) {
            this.highlightDirectoriesWithModifiedFiles = val;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized PatchCreationPolicy getPatchCreationPolicy() {
        return patchCreationPolicy;
    }

    public synchronized void setPatchCreationPolicy(PatchCreationPolicy policy) {
        if (policy != null && this.patchCreationPolicy != policy) {
            this.patchCreationPolicy = policy;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isRestoreWorkspaceWhenSwitchingBranches() {
        return restoreWorkspaceWhenSwitchingBranches;
    }

    public synchronized void setRestoreWorkspaceWhenSwitchingBranches(boolean val) {
        if (this.restoreWorkspaceWhenSwitchingBranches != val) {
            this.restoreWorkspaceWhenSwitchingBranches = val;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isLimitHistory() {
        return limitHistory;
    }

    public synchronized void setLimitHistory(boolean val) {
        if (this.limitHistory != val) {
            this.limitHistory = val;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized int getHistoryLimitRows() {
        return historyLimitRows;
    }

    public synchronized void setHistoryLimitRows(int rows) {
        if (this.historyLimitRows != rows && rows > 0) {
            this.historyLimitRows = rows;
            savePreferences();
            notifyListeners();
        }
    }

    // --- Gutter section getters and setters ---

    public synchronized boolean isHighlightModifiedLinesInGutter() {
        return highlightModifiedLinesInGutter;
    }

    public synchronized void setHighlightModifiedLinesInGutter(boolean val) {
        if (this.highlightModifiedLinesInGutter != val) {
            this.highlightModifiedLinesInGutter = val;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isHighlightModifiedLinesInErrorStripe() {
        return highlightModifiedLinesInErrorStripe;
    }

    public synchronized void setHighlightModifiedLinesInErrorStripe(boolean val) {
        if (this.highlightModifiedLinesInErrorStripe != val) {
            this.highlightModifiedLinesInErrorStripe = val;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isHighlightWhitespaceOnlyModifications() {
        return highlightWhitespaceOnlyModifications;
    }

    public synchronized void setHighlightWhitespaceOnlyModifications(boolean val) {
        if (this.highlightWhitespaceOnlyModifications != val) {
            this.highlightWhitespaceOnlyModifications = val;
            savePreferences();
            notifyListeners();
        }
    }

    // --- Legacy compatibility methods ---

    public synchronized boolean isRestoreWorkspaceOnBranchSwitch() {
        return restoreWorkspaceWhenSwitchingBranches;
    }

    public synchronized void setRestoreWorkspaceOnBranchSwitch(boolean value) {
        setRestoreWorkspaceWhenSwitchingBranches(value);
    }

    public synchronized boolean isShowPromptWhenCheckoutFiles() {
        return showPromptWhenCheckoutFiles;
    }

    public synchronized void setShowPromptWhenCheckoutFiles(boolean value) {
        if (this.showPromptWhenCheckoutFiles != value) {
            this.showPromptWhenCheckoutFiles = value;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isClearUnversionedFilesOnReload() {
        return clearUnversionedFilesOnReload;
    }

    public synchronized void setClearUnversionedFilesOnReload(boolean value) {
        if (this.clearUnversionedFilesOnReload != value) {
            this.clearUnversionedFilesOnReload = value;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized void revertToDefaults() {
        fileCreationPolicy = FileCreationPolicy.ADD_SILENTLY;
        applyCreationPolicyToExternalFiles = false;
        fileDeletionPolicy = FileDeletionPolicy.ASK;
        showOptionsBeforeCheckout = true;
        showOptionsBeforeUpdate = true;
        askToUnlockReadOnlyFiles = true;
        askConfirmationToDropCommits = true;

        checkForServerConflicts = false;
        conflictCheckIntervalMinutes = 60;
        highlightFilesChangedInDays = false;
        highlightDays = 31;
        highlightDirectoriesWithModifiedFiles = true;
        patchCreationPolicy = PatchCreationPolicy.ASK;
        restoreWorkspaceWhenSwitchingBranches = true;
        limitHistory = true;
        historyLimitRows = 1000;

        highlightModifiedLinesInGutter = true;
        highlightModifiedLinesInErrorStripe = true;
        highlightWhitespaceOnlyModifications = true;

        showPromptWhenCheckoutFiles = true;
        clearUnversionedFilesOnReload = false;

        savePreferences();
        notifyListeners();
    }

    public synchronized void addListener(Runnable listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable l : new ArrayList<>(listeners)) {
            try {
                l.run();
            } catch (Exception ignored) {}
        }
    }

    private void loadPreferences() {
        String creation = prefs.get("git_file_creation_policy", FileCreationPolicy.ADD_SILENTLY.name());
        try {
            fileCreationPolicy = FileCreationPolicy.valueOf(creation);
        } catch (Exception e) {
            fileCreationPolicy = FileCreationPolicy.ADD_SILENTLY;
        }

        applyCreationPolicyToExternalFiles = prefs.getBoolean("git_apply_creation_external", false);

        String deletion = prefs.get("git_file_deletion_policy", FileDeletionPolicy.ASK.name());
        try {
            fileDeletionPolicy = FileDeletionPolicy.valueOf(deletion);
        } catch (Exception e) {
            fileDeletionPolicy = FileDeletionPolicy.ASK;
        }

        showOptionsBeforeCheckout = prefs.getBoolean("git_show_options_checkout", true);
        showOptionsBeforeUpdate = prefs.getBoolean("git_show_options_update", true);
        askToUnlockReadOnlyFiles = prefs.getBoolean("git_ask_unlock_readonly", true);
        askConfirmationToDropCommits = prefs.getBoolean("git_ask_drop_commits", true);

        checkForServerConflicts = prefs.getBoolean("git_check_server_conflicts", false);
        conflictCheckIntervalMinutes = prefs.getInt("git_conflict_check_interval", 60);
        highlightFilesChangedInDays = prefs.getBoolean("git_highlight_changed_days", false);
        highlightDays = prefs.getInt("git_highlight_days_count", 31);
        highlightDirectoriesWithModifiedFiles = prefs.getBoolean("git_highlight_modified_dirs", true);

        String patch = prefs.get("git_patch_creation_policy", PatchCreationPolicy.ASK.name());
        try {
            patchCreationPolicy = PatchCreationPolicy.valueOf(patch);
        } catch (Exception e) {
            patchCreationPolicy = PatchCreationPolicy.ASK;
        }

        restoreWorkspaceWhenSwitchingBranches = prefs.getBoolean("git_restore_workspace", true);
        limitHistory = prefs.getBoolean("git_limit_history", true);
        historyLimitRows = prefs.getInt("git_history_limit_rows", 1000);

        highlightModifiedLinesInGutter = prefs.getBoolean("git_gutter_highlight", true);
        highlightModifiedLinesInErrorStripe = prefs.getBoolean("git_error_stripe_highlight", true);
        highlightWhitespaceOnlyModifications = prefs.getBoolean("git_whitespace_highlight", true);

        showPromptWhenCheckoutFiles = prefs.getBoolean("git_prompt_checkout", true);
        clearUnversionedFilesOnReload = prefs.getBoolean("git_clear_unversioned", false);
    }

    private void savePreferences() {
        prefs.put("git_file_creation_policy", fileCreationPolicy.name());
        prefs.putBoolean("git_apply_creation_external", applyCreationPolicyToExternalFiles);
        prefs.put("git_file_deletion_policy", fileDeletionPolicy.name());
        prefs.putBoolean("git_show_options_checkout", showOptionsBeforeCheckout);
        prefs.putBoolean("git_show_options_update", showOptionsBeforeUpdate);
        prefs.putBoolean("git_ask_unlock_readonly", askToUnlockReadOnlyFiles);
        prefs.putBoolean("git_ask_drop_commits", askConfirmationToDropCommits);

        prefs.putBoolean("git_check_server_conflicts", checkForServerConflicts);
        prefs.putInt("git_conflict_check_interval", conflictCheckIntervalMinutes);
        prefs.putBoolean("git_highlight_changed_days", highlightFilesChangedInDays);
        prefs.putInt("git_highlight_days_count", highlightDays);
        prefs.putBoolean("git_highlight_modified_dirs", highlightDirectoriesWithModifiedFiles);

        prefs.put("git_patch_creation_policy", patchCreationPolicy.name());
        prefs.putBoolean("git_restore_workspace", restoreWorkspaceWhenSwitchingBranches);
        prefs.putBoolean("git_limit_history", limitHistory);
        prefs.putInt("git_history_limit_rows", historyLimitRows);

        prefs.putBoolean("git_gutter_highlight", highlightModifiedLinesInGutter);
        prefs.putBoolean("git_error_stripe_highlight", highlightModifiedLinesInErrorStripe);
        prefs.putBoolean("git_whitespace_highlight", highlightWhitespaceOnlyModifications);

        prefs.putBoolean("git_prompt_checkout", showPromptWhenCheckoutFiles);
        prefs.putBoolean("git_clear_unversioned", clearUnversionedFilesOnReload);
    }
}
