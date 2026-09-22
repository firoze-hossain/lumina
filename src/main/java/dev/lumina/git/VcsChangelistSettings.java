package dev.lumina.git;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Central singleton managing Version Control Changelists options matching IntelliJ IDEA:
 * - Create changelists automatically (checkbox)
 * - Allow putting changes within one file into different changelists (checkbox)
 * - Inactive Changelist:
 *   - Highlight files from inactive changelists (checkbox)
 *   - Show dialog on attempt to edit file from inactive changelist (checkbox)
 *   - When an empty changelist becomes inactive (Show options / Do nothing / Remove silently)
 * - Conflicts:
 *   - Highlight files with changelist conflicts (checkbox)
 *   - Files with ignored conflicts list
 *
 * Backed by Java Preferences for dynamic persistence.
 */
public class VcsChangelistSettings {

    public enum EmptyChangelistInactiveAction {
        SHOW_OPTIONS("Show options"),
        DO_NOTHING("Do nothing"),
        REMOVE_SILENTLY("Remove silently");

        private final String displayName;

        EmptyChangelistInactiveAction(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private static final VcsChangelistSettings INSTANCE = new VcsChangelistSettings();

    private final Preferences prefs = Preferences.userNodeForPackage(VcsChangelistSettings.class);
    private final List<Runnable> listeners = new ArrayList<>();

    private boolean createChangelistsAutomatically = false;
    private boolean allowPuttingChangesWithinOneFileIntoDifferentChangelists = true;

    // Inactive Changelist
    private boolean highlightFilesFromInactiveChangelists = false;
    private boolean showDialogOnAttemptToEditFileFromInactiveChangelist = false;
    private EmptyChangelistInactiveAction emptyChangelistInactiveAction = EmptyChangelistInactiveAction.SHOW_OPTIONS;

    // Conflicts
    private boolean highlightFilesWithChangelistConflicts = true;
    private final List<String> ignoredConflicts = new ArrayList<>();

    private VcsChangelistSettings() {
        loadPreferences();
    }

    public static VcsChangelistSettings getInstance() {
        return INSTANCE;
    }

    public synchronized boolean isCreateChangelistsAutomatically() {
        return createChangelistsAutomatically;
    }

    public synchronized void setCreateChangelistsAutomatically(boolean val) {
        if (this.createChangelistsAutomatically != val) {
            this.createChangelistsAutomatically = val;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isAllowPuttingChangesWithinOneFileIntoDifferentChangelists() {
        return allowPuttingChangesWithinOneFileIntoDifferentChangelists;
    }

    public synchronized void setAllowPuttingChangesWithinOneFileIntoDifferentChangelists(boolean val) {
        if (this.allowPuttingChangesWithinOneFileIntoDifferentChangelists != val) {
            this.allowPuttingChangesWithinOneFileIntoDifferentChangelists = val;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isHighlightFilesFromInactiveChangelists() {
        return highlightFilesFromInactiveChangelists;
    }

    public synchronized void setHighlightFilesFromInactiveChangelists(boolean val) {
        if (this.highlightFilesFromInactiveChangelists != val) {
            this.highlightFilesFromInactiveChangelists = val;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isShowDialogOnAttemptToEditFileFromInactiveChangelist() {
        return showDialogOnAttemptToEditFileFromInactiveChangelist;
    }

    public synchronized void setShowDialogOnAttemptToEditFileFromInactiveChangelist(boolean val) {
        if (this.showDialogOnAttemptToEditFileFromInactiveChangelist != val) {
            this.showDialogOnAttemptToEditFileFromInactiveChangelist = val;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized EmptyChangelistInactiveAction getEmptyChangelistInactiveAction() {
        return emptyChangelistInactiveAction;
    }

    public synchronized void setEmptyChangelistInactiveAction(EmptyChangelistInactiveAction action) {
        if (action != null && this.emptyChangelistInactiveAction != action) {
            this.emptyChangelistInactiveAction = action;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isHighlightFilesWithChangelistConflicts() {
        return highlightFilesWithChangelistConflicts;
    }

    public synchronized void setHighlightFilesWithChangelistConflicts(boolean val) {
        if (this.highlightFilesWithChangelistConflicts != val) {
            this.highlightFilesWithChangelistConflicts = val;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized List<String> getIgnoredConflicts() {
        return Collections.unmodifiableList(new ArrayList<>(ignoredConflicts));
    }

    public synchronized void clearIgnoredConflicts() {
        if (!ignoredConflicts.isEmpty()) {
            ignoredConflicts.clear();
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized void addIgnoredConflict(String filePath) {
        if (filePath != null && !ignoredConflicts.contains(filePath)) {
            ignoredConflicts.add(filePath);
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized void revertToDefaults() {
        createChangelistsAutomatically = false;
        allowPuttingChangesWithinOneFileIntoDifferentChangelists = true;
        highlightFilesFromInactiveChangelists = false;
        showDialogOnAttemptToEditFileFromInactiveChangelist = false;
        emptyChangelistInactiveAction = EmptyChangelistInactiveAction.SHOW_OPTIONS;
        highlightFilesWithChangelistConflicts = true;
        ignoredConflicts.clear();

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
        createChangelistsAutomatically = prefs.getBoolean("vcs_changelists_auto_create", false);
        allowPuttingChangesWithinOneFileIntoDifferentChangelists = prefs.getBoolean("vcs_changelists_multi_in_file", true);
        highlightFilesFromInactiveChangelists = prefs.getBoolean("vcs_changelists_highlight_inactive", false);
        showDialogOnAttemptToEditFileFromInactiveChangelist = prefs.getBoolean("vcs_changelists_dialog_edit_inactive", false);

        String action = prefs.get("vcs_changelists_empty_action", EmptyChangelistInactiveAction.SHOW_OPTIONS.name());
        try {
            emptyChangelistInactiveAction = EmptyChangelistInactiveAction.valueOf(action);
        } catch (Exception e) {
            emptyChangelistInactiveAction = EmptyChangelistInactiveAction.SHOW_OPTIONS;
        }

        highlightFilesWithChangelistConflicts = prefs.getBoolean("vcs_changelists_highlight_conflicts", true);

        String rawIgnored = prefs.get("vcs_changelists_ignored_conflicts", "");
        ignoredConflicts.clear();
        if (!rawIgnored.isBlank()) {
            for (String part : rawIgnored.split(";")) {
                if (!part.isBlank()) {
                    ignoredConflicts.add(part);
                }
            }
        }
    }

    private void savePreferences() {
        prefs.putBoolean("vcs_changelists_auto_create", createChangelistsAutomatically);
        prefs.putBoolean("vcs_changelists_multi_in_file", allowPuttingChangesWithinOneFileIntoDifferentChangelists);
        prefs.putBoolean("vcs_changelists_highlight_inactive", highlightFilesFromInactiveChangelists);
        prefs.putBoolean("vcs_changelists_dialog_edit_inactive", showDialogOnAttemptToEditFileFromInactiveChangelist);
        prefs.put("vcs_changelists_empty_action", emptyChangelistInactiveAction.name());
        prefs.putBoolean("vcs_changelists_highlight_conflicts", highlightFilesWithChangelistConflicts);
        prefs.put("vcs_changelists_ignored_conflicts", String.join(";", ignoredConflicts));
    }
}
