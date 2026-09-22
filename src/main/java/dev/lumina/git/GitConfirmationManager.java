package dev.lumina.git;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Central singleton managing Version Control Confirmation policies and settings.
 * Controls "When files are created" (Add silently / Do not add / Ask)
 * and "When files are deleted" (Remove silently / Do not remove / Ask) with Java Preferences persistence.
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

    private static final GitConfirmationManager INSTANCE = new GitConfirmationManager();

    private final Preferences prefs = Preferences.userNodeForPackage(GitConfirmationManager.class);
    private final List<Runnable> listeners = new ArrayList<>();

    private FileCreationPolicy fileCreationPolicy = FileCreationPolicy.ASK;
    private FileDeletionPolicy fileDeletionPolicy = FileDeletionPolicy.ASK;
    private boolean restoreWorkspaceOnBranchSwitch = true;
    private boolean showPromptWhenCheckoutFiles = true;
    private boolean clearUnversionedFilesOnReload = false;

    private GitConfirmationManager() {
        loadPreferences();
    }

    public static GitConfirmationManager getInstance() {
        return INSTANCE;
    }

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

    public synchronized boolean isRestoreWorkspaceOnBranchSwitch() {
        return restoreWorkspaceOnBranchSwitch;
    }

    public synchronized void setRestoreWorkspaceOnBranchSwitch(boolean value) {
        if (this.restoreWorkspaceOnBranchSwitch != value) {
            this.restoreWorkspaceOnBranchSwitch = value;
            savePreferences();
            notifyListeners();
        }
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
        fileCreationPolicy = FileCreationPolicy.ASK;
        fileDeletionPolicy = FileDeletionPolicy.ASK;
        restoreWorkspaceOnBranchSwitch = true;
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
        String creation = prefs.get("git_file_creation_policy", FileCreationPolicy.ASK.name());
        try {
            fileCreationPolicy = FileCreationPolicy.valueOf(creation);
        } catch (Exception e) {
            fileCreationPolicy = FileCreationPolicy.ASK;
        }

        String deletion = prefs.get("git_file_deletion_policy", FileDeletionPolicy.ASK.name());
        try {
            fileDeletionPolicy = FileDeletionPolicy.valueOf(deletion);
        } catch (Exception e) {
            fileDeletionPolicy = FileDeletionPolicy.ASK;
        }

        restoreWorkspaceOnBranchSwitch = prefs.getBoolean("git_restore_workspace", true);
        showPromptWhenCheckoutFiles = prefs.getBoolean("git_prompt_checkout", true);
        clearUnversionedFilesOnReload = prefs.getBoolean("git_clear_unversioned", false);
    }

    private void savePreferences() {
        prefs.put("git_file_creation_policy", fileCreationPolicy.name());
        prefs.put("git_file_deletion_policy", fileDeletionPolicy.name());
        prefs.putBoolean("git_restore_workspace", restoreWorkspaceOnBranchSwitch);
        prefs.putBoolean("git_prompt_checkout", showPromptWhenCheckoutFiles);
        prefs.putBoolean("git_clear_unversioned", clearUnversionedFilesOnReload);
    }
}
