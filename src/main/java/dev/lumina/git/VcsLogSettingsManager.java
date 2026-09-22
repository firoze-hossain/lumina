package dev.lumina.git;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Manages VCS Log & File History settings matching IntelliJ IDEA's "Version Control > Log".
 * Backed by Java Preferences for dynamic persistence.
 */
public class VcsLogSettingsManager {

    public enum DiffPreviewLocation {
        BOTTOM("Bottom"),
        RIGHT("Right");

        private final String label;

        DiffPreviewLocation(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private static final VcsLogSettingsManager INSTANCE = new VcsLogSettingsManager();

    private final Preferences prefs = Preferences.userNodeForPackage(VcsLogSettingsManager.class);
    private final List<Runnable> listeners = new ArrayList<>();

    // 1. View Options
    private boolean showOnlyFirstRef = true;
    private boolean showTagNames = false;
    private boolean showCommitTime = false;
    private boolean showLeftReferences = false;
    private boolean displayMergedCommitsSeparately = false;
    private boolean showDiffPreview = false;
    private DiffPreviewLocation diffPreviewLocation = DiffPreviewLocation.BOTTOM;

    // View Options > Visible Columns
    private boolean authorVisible = true;
    private boolean hashVisible = false;
    private boolean dateVisible = true;
    private boolean gpgSignatureVisible = false;
    private boolean githubCommitChecksVisible = true;

    // 2. Indexing
    private boolean enableIndexing = true;

    // 3. File History
    private boolean fileHistoryDetailsPanel = false;
    private boolean fileHistoryShowFileNames = false;
    private boolean fileHistoryDiffPreview = true;
    private DiffPreviewLocation fileHistoryDiffPreviewLocation = DiffPreviewLocation.RIGHT;

    // File History > Visible Columns
    private boolean fileHistoryAuthorVisible = true;
    private boolean fileHistoryHashVisible = false;
    private boolean fileHistoryDateVisible = true;
    private boolean fileHistoryGpgSignatureVisible = false;
    private boolean fileHistoryGithubCommitChecksVisible = true;

    private VcsLogSettingsManager() {
        loadPreferences();
    }

    public static VcsLogSettingsManager getInstance() {
        return INSTANCE;
    }

    // --- View Options Getters/Setters ---

    public synchronized boolean isShowOnlyFirstRef() { return showOnlyFirstRef; }
    public synchronized void setShowOnlyFirstRef(boolean val) {
        if (this.showOnlyFirstRef != val) {
            this.showOnlyFirstRef = val;
            prefs.putBoolean("log_show_only_first_ref", val);
            notifyListeners();
        }
    }

    public synchronized boolean isShowTagNames() { return showTagNames; }
    public synchronized void setShowTagNames(boolean val) {
        if (this.showTagNames != val) {
            this.showTagNames = val;
            prefs.putBoolean("log_show_tag_names", val);
            notifyListeners();
        }
    }

    public synchronized boolean isShowCommitTime() { return showCommitTime; }
    public synchronized void setShowCommitTime(boolean val) {
        if (this.showCommitTime != val) {
            this.showCommitTime = val;
            prefs.putBoolean("log_show_commit_time", val);
            notifyListeners();
        }
    }

    public synchronized boolean isShowLeftReferences() { return showLeftReferences; }
    public synchronized void setShowLeftReferences(boolean val) {
        if (this.showLeftReferences != val) {
            this.showLeftReferences = val;
            prefs.putBoolean("log_show_left_references", val);
            notifyListeners();
        }
    }

    public synchronized boolean isDisplayMergedCommitsSeparately() { return displayMergedCommitsSeparately; }
    public synchronized void setDisplayMergedCommitsSeparately(boolean val) {
        if (this.displayMergedCommitsSeparately != val) {
            this.displayMergedCommitsSeparately = val;
            prefs.putBoolean("log_display_merged_commits_separately", val);
            notifyListeners();
        }
    }

    public synchronized boolean isShowDiffPreview() { return showDiffPreview; }
    public synchronized void setShowDiffPreview(boolean val) {
        if (this.showDiffPreview != val) {
            this.showDiffPreview = val;
            prefs.putBoolean("log_show_diff_preview", val);
            notifyListeners();
        }
    }

    public synchronized DiffPreviewLocation getDiffPreviewLocation() { return diffPreviewLocation; }
    public synchronized void setDiffPreviewLocation(DiffPreviewLocation loc) {
        if (loc != null && this.diffPreviewLocation != loc) {
            this.diffPreviewLocation = loc;
            prefs.put("log_diff_preview_location", loc.name());
            notifyListeners();
        }
    }

    // View Options Visible Columns
    public synchronized boolean isAuthorVisible() { return authorVisible; }
    public synchronized void setAuthorVisible(boolean val) {
        if (this.authorVisible != val) {
            this.authorVisible = val;
            prefs.putBoolean("log_col_author", val);
            notifyListeners();
        }
    }

    public synchronized boolean isHashVisible() { return hashVisible; }
    public synchronized void setHashVisible(boolean val) {
        if (this.hashVisible != val) {
            this.hashVisible = val;
            prefs.putBoolean("log_col_hash", val);
            notifyListeners();
        }
    }

    public synchronized boolean isDateVisible() { return dateVisible; }
    public synchronized void setDateVisible(boolean val) {
        if (this.dateVisible != val) {
            this.dateVisible = val;
            prefs.putBoolean("log_col_date", val);
            notifyListeners();
        }
    }

    public synchronized boolean isGpgSignatureVisible() { return gpgSignatureVisible; }
    public synchronized void setGpgSignatureVisible(boolean val) {
        if (this.gpgSignatureVisible != val) {
            this.gpgSignatureVisible = val;
            prefs.putBoolean("log_col_gpg_signature", val);
            notifyListeners();
        }
    }

    public synchronized boolean isGithubCommitChecksVisible() { return githubCommitChecksVisible; }
    public synchronized void setGithubCommitChecksVisible(boolean val) {
        if (this.githubCommitChecksVisible != val) {
            this.githubCommitChecksVisible = val;
            prefs.putBoolean("log_col_github_commit_checks", val);
            notifyListeners();
        }
    }

    // --- Indexing ---

    public synchronized boolean isEnableIndexing() { return enableIndexing; }
    public synchronized void setEnableIndexing(boolean val) {
        if (this.enableIndexing != val) {
            this.enableIndexing = val;
            prefs.putBoolean("log_enable_indexing", val);
            notifyListeners();
        }
    }

    // --- File History ---

    public synchronized boolean isFileHistoryDetailsPanel() { return fileHistoryDetailsPanel; }
    public synchronized void setFileHistoryDetailsPanel(boolean val) {
        if (this.fileHistoryDetailsPanel != val) {
            this.fileHistoryDetailsPanel = val;
            prefs.putBoolean("file_history_details_panel", val);
            notifyListeners();
        }
    }

    public synchronized boolean isFileHistoryShowFileNames() { return fileHistoryShowFileNames; }
    public synchronized void setFileHistoryShowFileNames(boolean val) {
        if (this.fileHistoryShowFileNames != val) {
            this.fileHistoryShowFileNames = val;
            prefs.putBoolean("file_history_show_file_names", val);
            notifyListeners();
        }
    }

    public synchronized boolean isFileHistoryDiffPreview() { return fileHistoryDiffPreview; }
    public synchronized void setFileHistoryDiffPreview(boolean val) {
        if (this.fileHistoryDiffPreview != val) {
            this.fileHistoryDiffPreview = val;
            prefs.putBoolean("file_history_diff_preview", val);
            notifyListeners();
        }
    }

    public synchronized DiffPreviewLocation getFileHistoryDiffPreviewLocation() { return fileHistoryDiffPreviewLocation; }
    public synchronized void setFileHistoryDiffPreviewLocation(DiffPreviewLocation loc) {
        if (loc != null && this.fileHistoryDiffPreviewLocation != loc) {
            this.fileHistoryDiffPreviewLocation = loc;
            prefs.put("file_history_diff_preview_location", loc.name());
            notifyListeners();
        }
    }

    public synchronized boolean isFileHistoryAuthorVisible() { return fileHistoryAuthorVisible; }
    public synchronized void setFileHistoryAuthorVisible(boolean val) {
        if (this.fileHistoryAuthorVisible != val) {
            this.fileHistoryAuthorVisible = val;
            prefs.putBoolean("file_history_col_author", val);
            notifyListeners();
        }
    }

    public synchronized boolean isFileHistoryHashVisible() { return fileHistoryHashVisible; }
    public synchronized void setFileHistoryHashVisible(boolean val) {
        if (this.fileHistoryHashVisible != val) {
            this.fileHistoryHashVisible = val;
            prefs.putBoolean("file_history_col_hash", val);
            notifyListeners();
        }
    }

    public synchronized boolean isFileHistoryDateVisible() { return fileHistoryDateVisible; }
    public synchronized void setFileHistoryDateVisible(boolean val) {
        if (this.fileHistoryDateVisible != val) {
            this.fileHistoryDateVisible = val;
            prefs.putBoolean("file_history_col_date", val);
            notifyListeners();
        }
    }

    public synchronized boolean isFileHistoryGpgSignatureVisible() { return fileHistoryGpgSignatureVisible; }
    public synchronized void setFileHistoryGpgSignatureVisible(boolean val) {
        if (this.fileHistoryGpgSignatureVisible != val) {
            this.fileHistoryGpgSignatureVisible = val;
            prefs.putBoolean("file_history_col_gpg_signature", val);
            notifyListeners();
        }
    }

    public synchronized boolean isFileHistoryGithubCommitChecksVisible() { return fileHistoryGithubCommitChecksVisible; }
    public synchronized void setFileHistoryGithubCommitChecksVisible(boolean val) {
        if (this.fileHistoryGithubCommitChecksVisible != val) {
            this.fileHistoryGithubCommitChecksVisible = val;
            prefs.putBoolean("file_history_col_github_commit_checks", val);
            notifyListeners();
        }
    }

    // --- Reset Defaults ---

    public synchronized void revertToDefaults() {
        showOnlyFirstRef = true;
        showTagNames = false;
        showCommitTime = false;
        showLeftReferences = false;
        displayMergedCommitsSeparately = false;
        showDiffPreview = false;
        diffPreviewLocation = DiffPreviewLocation.BOTTOM;

        authorVisible = true;
        hashVisible = false;
        dateVisible = true;
        gpgSignatureVisible = false;
        githubCommitChecksVisible = true;

        enableIndexing = true;

        fileHistoryDetailsPanel = false;
        fileHistoryShowFileNames = false;
        fileHistoryDiffPreview = true;
        fileHistoryDiffPreviewLocation = DiffPreviewLocation.RIGHT;

        fileHistoryAuthorVisible = true;
        fileHistoryHashVisible = false;
        fileHistoryDateVisible = true;
        fileHistoryGpgSignatureVisible = false;
        fileHistoryGithubCommitChecksVisible = true;

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
        showOnlyFirstRef = prefs.getBoolean("log_show_only_first_ref", true);
        showTagNames = prefs.getBoolean("log_show_tag_names", false);
        showCommitTime = prefs.getBoolean("log_show_commit_time", false);
        showLeftReferences = prefs.getBoolean("log_show_left_references", false);
        displayMergedCommitsSeparately = prefs.getBoolean("log_display_merged_commits_separately", false);
        showDiffPreview = prefs.getBoolean("log_show_diff_preview", false);
        try {
            diffPreviewLocation = DiffPreviewLocation.valueOf(prefs.get("log_diff_preview_location", DiffPreviewLocation.BOTTOM.name()));
        } catch (Exception e) {
            diffPreviewLocation = DiffPreviewLocation.BOTTOM;
        }

        authorVisible = prefs.getBoolean("log_col_author", true);
        hashVisible = prefs.getBoolean("log_col_hash", false);
        dateVisible = prefs.getBoolean("log_col_date", true);
        gpgSignatureVisible = prefs.getBoolean("log_col_gpg_signature", false);
        githubCommitChecksVisible = prefs.getBoolean("log_col_github_commit_checks", true);

        enableIndexing = prefs.getBoolean("log_enable_indexing", true);

        fileHistoryDetailsPanel = prefs.getBoolean("file_history_details_panel", false);
        fileHistoryShowFileNames = prefs.getBoolean("file_history_show_file_names", false);
        fileHistoryDiffPreview = prefs.getBoolean("file_history_diff_preview", true);
        try {
            fileHistoryDiffPreviewLocation = DiffPreviewLocation.valueOf(prefs.get("file_history_diff_preview_location", DiffPreviewLocation.RIGHT.name()));
        } catch (Exception e) {
            fileHistoryDiffPreviewLocation = DiffPreviewLocation.RIGHT;
        }

        fileHistoryAuthorVisible = prefs.getBoolean("file_history_col_author", true);
        fileHistoryHashVisible = prefs.getBoolean("file_history_col_hash", false);
        fileHistoryDateVisible = prefs.getBoolean("file_history_col_date", true);
        fileHistoryGpgSignatureVisible = prefs.getBoolean("file_history_col_gpg_signature", false);
        fileHistoryGithubCommitChecksVisible = prefs.getBoolean("file_history_col_github_commit_checks", true);
    }

    private void savePreferences() {
        prefs.putBoolean("log_show_only_first_ref", showOnlyFirstRef);
        prefs.putBoolean("log_show_tag_names", showTagNames);
        prefs.putBoolean("log_show_commit_time", showCommitTime);
        prefs.putBoolean("log_show_left_references", showLeftReferences);
        prefs.putBoolean("log_display_merged_commits_separately", displayMergedCommitsSeparately);
        prefs.putBoolean("log_show_diff_preview", showDiffPreview);
        prefs.put("log_diff_preview_location", diffPreviewLocation.name());

        prefs.putBoolean("log_col_author", authorVisible);
        prefs.putBoolean("log_col_hash", hashVisible);
        prefs.putBoolean("log_col_date", dateVisible);
        prefs.putBoolean("log_col_gpg_signature", gpgSignatureVisible);
        prefs.putBoolean("log_col_github_commit_checks", githubCommitChecksVisible);

        prefs.putBoolean("log_enable_indexing", enableIndexing);

        prefs.putBoolean("file_history_details_panel", fileHistoryDetailsPanel);
        prefs.putBoolean("file_history_show_file_names", fileHistoryShowFileNames);
        prefs.putBoolean("file_history_diff_preview", fileHistoryDiffPreview);
        prefs.put("file_history_diff_preview_location", fileHistoryDiffPreviewLocation.name());

        prefs.putBoolean("file_history_col_author", fileHistoryAuthorVisible);
        prefs.putBoolean("file_history_col_hash", fileHistoryHashVisible);
        prefs.putBoolean("file_history_col_date", fileHistoryDateVisible);
        prefs.putBoolean("file_history_col_gpg_signature", fileHistoryGpgSignatureVisible);
        prefs.putBoolean("file_history_col_github_commit_checks", fileHistoryGithubCommitChecksVisible);
    }
}
