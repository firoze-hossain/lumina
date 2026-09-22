package dev.lumina.git;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Manages all Git configuration options matching IntelliJ IDEA's "Version Control > Git".
 * Backed by Java Preferences for dynamic persistence with zero hardcoding.
 */
public class GitSettingsManager {

    public enum UpdateMethod {
        MERGE("Merge"),
        REBASE("Rebase");

        private final String label;
        UpdateMethod(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum CleanWorkingTreeMethod {
        STASH("Stash"),
        SHELVE("Shelve");

        private final String label;
        CleanWorkingTreeMethod(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum FilterUpdatePaths {
        ALL("All"),
        SELECT("Select..."),
        SELECT_IN_TREE("Select in Tree...");

        private final String label;
        FilterUpdatePaths(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum IncomingCommitsCheck {
        AUTO("Auto"),
        ALWAYS("Always"),
        NEVER("Never");

        private final String label;
        IncomingCommitsCheck(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum FetchTagsMode {
        AUTO_FOLLOW_GIT_CONFIG("Auto Follow git config"),
        SYNC_PRUNE_TAGS("Sync --prune-tags"),
        ALWAYS_TAGS("Always --tags"),
        NEVER_NO_TAGS("Never --no-tags");

        private final String label;
        FetchTagsMode(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum StashDiffComparison {
        LOCAL_VERSION("With the local version of a file"),
        PARENT_COMMIT("With the parent commit");

        private final String label;
        StashDiffComparison(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public record TestResult(boolean success, String message) {}

    private static final GitSettingsManager INSTANCE = new GitSettingsManager();

    private final Preferences prefs = Preferences.userNodeForPackage(GitSettingsManager.class);
    private final List<Runnable> listeners = new ArrayList<>();

    // 1. Top Executable Settings
    private String gitExecutablePath = "";
    private boolean setPathOnlyForProject = false;
    private boolean autoExcludeIgnoredDirectories = false;

    // 2. Commit Section
    private boolean enableStagingArea = false;
    private boolean warnCrlf = true;
    private boolean warnDetachedHead = true;
    private boolean warnFilesLargerThanEnabled = true;
    private int warnFilesLargerThanMb = 50;
    private boolean warnCrossPlatformFilenames = true;
    private boolean addCherryPickSuffix = true;
    private boolean signCommitsWithGpg = false;
    private String gpgKeyId = "";

    // 3. Push Section
    private boolean autoUpdateOnRejectedPush = false;
    private boolean showPushDialog = true;
    private boolean showPushDialogOnlyProtected = false;
    private String protectedBranches = "master;main";
    private boolean loadBranchProtectionFromGitHub = true;

    // 4. Update Section
    private UpdateMethod updateMethod = UpdateMethod.MERGE;
    private CleanWorkingTreeMethod cleanWorkingTreeMethod = CleanWorkingTreeMethod.SHELVE;
    private FilterUpdatePaths filterUpdatePaths = FilterUpdatePaths.ALL;
    private IncomingCommitsCheck incomingCommitsCheck = IncomingCommitsCheck.AUTO;
    private FetchTagsMode fetchTagsMode = FetchTagsMode.AUTO_FOLLOW_GIT_CONFIG;
    private boolean useCredentialHelper = false;

    // 5. Stash Section
    private boolean combineStashesAndShelves = false;
    private StashDiffComparison stashDiffComparison = StashDiffComparison.LOCAL_VERSION;
    private boolean activateVirtualenvForHooks = true;

    private GitSettingsManager() {
        loadPreferences();
    }

    public static GitSettingsManager getInstance() {
        return INSTANCE;
    }

    // --- Git Executable Discovery & Resolution ---

    public String getAutoDetectedGitPath() {
        // Search standard UNIX / Windows paths
        String[] candidates = {
                "/usr/bin/git",
                "/usr/local/bin/git",
                "/bin/git",
                "/opt/homebrew/bin/git",
                "C:\\Program Files\\Git\\bin\\git.exe",
                "C:\\Program Files (x86)\\Git\\bin\\git.exe"
        };
        for (String c : candidates) {
            if (new File(c).canExecute()) {
                return c;
            }
        }
        return "git";
    }

    public String getEffectiveGitExecutable() {
        if (gitExecutablePath != null && !gitExecutablePath.isBlank()) {
            return gitExecutablePath.trim();
        }
        return getAutoDetectedGitPath();
    }

    public TestResult testGitExecutable(String path) {
        String exec = (path != null && !path.isBlank()) ? path.trim() : getEffectiveGitExecutable();
        try {
            Process p = new ProcessBuilder(exec, "--version")
                    .redirectErrorStream(true)
                    .start();
            String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            int code = p.waitFor();
            if (code == 0) {
                // e.g. "git version 2.43.0" -> "Git version is 2.43.0"
                String clean = output.replaceFirst("(?i)^git version\\s*", "");
                return new TestResult(true, "Git version is " + clean);
            } else {
                return new TestResult(false, "Git exited with code " + code + ": " + output);
            }
        } catch (Exception e) {
            return new TestResult(false, "Cannot run Git at '" + exec + "': " + e.getMessage());
        }
    }

    public List<String> detectGpgSecretKeys() {
        List<String> keys = new ArrayList<>();
        try {
            Process p = new ProcessBuilder("gpg", "--list-secret-keys", "--keyid-format=long")
                    .redirectErrorStream(true)
                    .start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (p.waitFor() == 0) {
                for (String line : out.split("\\R")) {
                    line = line.trim();
                    if (line.startsWith("sec")) {
                        // e.g. "sec   rsa4096/3AA5C34371567BD2 2016-03-10 [SC]"
                        String[] tokens = line.split("\\s+");
                        if (tokens.length >= 2 && tokens[1].contains("/")) {
                            keys.add(tokens[1]);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return keys;
    }

    // --- Getters & Setters ---

    public synchronized String getGitExecutablePath() { return gitExecutablePath; }
    public synchronized void setGitExecutablePath(String path) {
        this.gitExecutablePath = path != null ? path.trim() : "";
        prefs.put("git_executable_path", this.gitExecutablePath);
        notifyListeners();
    }

    public synchronized boolean isSetPathOnlyForProject() { return setPathOnlyForProject; }
    public synchronized void setSetPathOnlyForProject(boolean val) {
        this.setPathOnlyForProject = val;
        prefs.putBoolean("git_path_only_for_project", val);
        notifyListeners();
    }

    public synchronized boolean isAutoExcludeIgnoredDirectories() { return autoExcludeIgnoredDirectories; }
    public synchronized void setAutoExcludeIgnoredDirectories(boolean val) {
        this.autoExcludeIgnoredDirectories = val;
        prefs.putBoolean("git_auto_exclude_ignored_dirs", val);
        notifyListeners();
    }

    // Commit
    public synchronized boolean isEnableStagingArea() { return enableStagingArea; }
    public synchronized void setEnableStagingArea(boolean val) {
        this.enableStagingArea = val;
        prefs.putBoolean("git_enable_staging_area", val);
        notifyListeners();
    }

    public synchronized boolean isWarnCrlf() { return warnCrlf; }
    public synchronized void setWarnCrlf(boolean val) {
        this.warnCrlf = val;
        prefs.putBoolean("git_warn_crlf", val);
        notifyListeners();
    }

    public synchronized boolean isWarnDetachedHead() { return warnDetachedHead; }
    public synchronized void setWarnDetachedHead(boolean val) {
        this.warnDetachedHead = val;
        prefs.putBoolean("git_warn_detached_head", val);
        notifyListeners();
    }

    public synchronized boolean isWarnFilesLargerThanEnabled() { return warnFilesLargerThanEnabled; }
    public synchronized void setWarnFilesLargerThanEnabled(boolean val) {
        this.warnFilesLargerThanEnabled = val;
        prefs.putBoolean("git_warn_files_larger_enabled", val);
        notifyListeners();
    }

    public synchronized int getWarnFilesLargerThanMb() { return warnFilesLargerThanMb; }
    public synchronized void setWarnFilesLargerThanMb(int mb) {
        this.warnFilesLargerThanMb = Math.max(1, mb);
        prefs.putInt("git_warn_files_larger_mb", this.warnFilesLargerThanMb);
        notifyListeners();
    }

    public synchronized boolean isWarnCrossPlatformFilenames() { return warnCrossPlatformFilenames; }
    public synchronized void setWarnCrossPlatformFilenames(boolean val) {
        this.warnCrossPlatformFilenames = val;
        prefs.putBoolean("git_warn_cross_platform_filenames", val);
        notifyListeners();
    }

    public synchronized boolean isAddCherryPickSuffix() { return addCherryPickSuffix; }
    public synchronized void setAddCherryPickSuffix(boolean val) {
        this.addCherryPickSuffix = val;
        prefs.putBoolean("git_add_cherry_pick_suffix", val);
        notifyListeners();
    }

    public synchronized boolean isSignCommitsWithGpg() { return signCommitsWithGpg; }
    public synchronized void setSignCommitsWithGpg(boolean val) {
        this.signCommitsWithGpg = val;
        prefs.putBoolean("git_sign_commits_with_gpg", val);
        notifyListeners();
    }

    public synchronized String getGpgKeyId() { return gpgKeyId; }
    public synchronized void setGpgKeyId(String keyId) {
        this.gpgKeyId = keyId != null ? keyId.trim() : "";
        prefs.put("git_gpg_key_id", this.gpgKeyId);
        notifyListeners();
    }

    // Push
    public synchronized boolean isAutoUpdateOnRejectedPush() { return autoUpdateOnRejectedPush; }
    public synchronized void setAutoUpdateOnRejectedPush(boolean val) {
        this.autoUpdateOnRejectedPush = val;
        prefs.putBoolean("git_auto_update_rejected_push", val);
        notifyListeners();
    }

    public synchronized boolean isShowPushDialog() { return showPushDialog; }
    public synchronized void setShowPushDialog(boolean val) {
        this.showPushDialog = val;
        prefs.putBoolean("git_show_push_dialog", val);
        notifyListeners();
    }

    public synchronized boolean isShowPushDialogOnlyProtected() { return showPushDialogOnlyProtected; }
    public synchronized void setShowPushDialogOnlyProtected(boolean val) {
        this.showPushDialogOnlyProtected = val;
        prefs.putBoolean("git_show_push_dialog_only_protected", val);
        notifyListeners();
    }

    public synchronized String getProtectedBranches() { return protectedBranches; }
    public synchronized void setProtectedBranches(String branches) {
        this.protectedBranches = branches != null ? branches.trim() : "master;main";
        prefs.put("git_protected_branches", this.protectedBranches);
        notifyListeners();
    }

    public synchronized boolean isLoadBranchProtectionFromGitHub() { return loadBranchProtectionFromGitHub; }
    public synchronized void setLoadBranchProtectionFromGitHub(boolean val) {
        this.loadBranchProtectionFromGitHub = val;
        prefs.putBoolean("git_load_branch_protection_github", val);
        notifyListeners();
    }

    // Update
    public synchronized UpdateMethod getUpdateMethod() { return updateMethod; }
    public synchronized void setUpdateMethod(UpdateMethod method) {
        if (method != null) {
            this.updateMethod = method;
            prefs.put("git_update_method", method.name());
            notifyListeners();
        }
    }

    public synchronized CleanWorkingTreeMethod getCleanWorkingTreeMethod() { return cleanWorkingTreeMethod; }
    public synchronized void setCleanWorkingTreeMethod(CleanWorkingTreeMethod method) {
        if (method != null) {
            this.cleanWorkingTreeMethod = method;
            prefs.put("git_clean_working_tree_method", method.name());
            notifyListeners();
        }
    }

    public synchronized FilterUpdatePaths getFilterUpdatePaths() { return filterUpdatePaths; }
    public synchronized void setFilterUpdatePaths(FilterUpdatePaths mode) {
        if (mode != null) {
            this.filterUpdatePaths = mode;
            prefs.put("git_filter_update_paths", mode.name());
            notifyListeners();
        }
    }

    public synchronized IncomingCommitsCheck getIncomingCommitsCheck() { return incomingCommitsCheck; }
    public synchronized void setIncomingCommitsCheck(IncomingCommitsCheck check) {
        if (check != null) {
            this.incomingCommitsCheck = check;
            prefs.put("git_incoming_commits_check", check.name());
            notifyListeners();
        }
    }

    public synchronized FetchTagsMode getFetchTagsMode() { return fetchTagsMode; }
    public synchronized void setFetchTagsMode(FetchTagsMode mode) {
        if (mode != null) {
            this.fetchTagsMode = mode;
            prefs.put("git_fetch_tags_mode", mode.name());
            notifyListeners();
        }
    }

    public synchronized boolean isUseCredentialHelper() { return useCredentialHelper; }
    public synchronized void setUseCredentialHelper(boolean val) {
        this.useCredentialHelper = val;
        prefs.putBoolean("git_use_credential_helper", val);
        notifyListeners();
    }

    // Stash
    public synchronized boolean isCombineStashesAndShelves() { return combineStashesAndShelves; }
    public synchronized void setCombineStashesAndShelves(boolean val) {
        this.combineStashesAndShelves = val;
        prefs.putBoolean("git_combine_stashes_shelves", val);
        notifyListeners();
    }

    public synchronized StashDiffComparison getStashDiffComparison() { return stashDiffComparison; }
    public synchronized void setStashDiffComparison(StashDiffComparison comparison) {
        if (comparison != null) {
            this.stashDiffComparison = comparison;
            prefs.put("git_stash_diff_comparison", comparison.name());
            notifyListeners();
        }
    }

    public synchronized boolean isActivateVirtualenvForHooks() { return activateVirtualenvForHooks; }
    public synchronized void setActivateVirtualenvForHooks(boolean val) {
        this.activateVirtualenvForHooks = val;
        prefs.putBoolean("git_activate_virtualenv_hooks", val);
        notifyListeners();
    }

    // --- Defaults & Listeners ---

    public synchronized void revertToDefaults() {
        gitExecutablePath = "";
        setPathOnlyForProject = false;
        autoExcludeIgnoredDirectories = false;

        enableStagingArea = false;
        warnCrlf = true;
        warnDetachedHead = true;
        warnFilesLargerThanEnabled = true;
        warnFilesLargerThanMb = 50;
        warnCrossPlatformFilenames = true;
        addCherryPickSuffix = true;
        signCommitsWithGpg = false;
        gpgKeyId = "";

        autoUpdateOnRejectedPush = false;
        showPushDialog = true;
        showPushDialogOnlyProtected = false;
        protectedBranches = "master;main";
        loadBranchProtectionFromGitHub = true;

        updateMethod = UpdateMethod.MERGE;
        cleanWorkingTreeMethod = CleanWorkingTreeMethod.SHELVE;
        filterUpdatePaths = FilterUpdatePaths.ALL;
        incomingCommitsCheck = IncomingCommitsCheck.AUTO;
        fetchTagsMode = FetchTagsMode.AUTO_FOLLOW_GIT_CONFIG;
        useCredentialHelper = false;

        combineStashesAndShelves = false;
        stashDiffComparison = StashDiffComparison.LOCAL_VERSION;
        activateVirtualenvForHooks = true;

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
        gitExecutablePath = prefs.get("git_executable_path", "");
        setPathOnlyForProject = prefs.getBoolean("git_path_only_for_project", false);
        autoExcludeIgnoredDirectories = prefs.getBoolean("git_auto_exclude_ignored_dirs", false);

        enableStagingArea = prefs.getBoolean("git_enable_staging_area", false);
        warnCrlf = prefs.getBoolean("git_warn_crlf", true);
        warnDetachedHead = prefs.getBoolean("git_warn_detached_head", true);
        warnFilesLargerThanEnabled = prefs.getBoolean("git_warn_files_larger_enabled", true);
        warnFilesLargerThanMb = prefs.getInt("git_warn_files_larger_mb", 50);
        warnCrossPlatformFilenames = prefs.getBoolean("git_warn_cross_platform_filenames", true);
        addCherryPickSuffix = prefs.getBoolean("git_add_cherry_pick_suffix", true);
        signCommitsWithGpg = prefs.getBoolean("git_sign_commits_with_gpg", false);
        gpgKeyId = prefs.get("git_gpg_key_id", "");

        autoUpdateOnRejectedPush = prefs.getBoolean("git_auto_update_rejected_push", false);
        showPushDialog = prefs.getBoolean("git_show_push_dialog", true);
        showPushDialogOnlyProtected = prefs.getBoolean("git_show_push_dialog_only_protected", false);
        protectedBranches = prefs.get("git_protected_branches", "master;main");
        loadBranchProtectionFromGitHub = prefs.getBoolean("git_load_branch_protection_github", true);

        try {
            updateMethod = UpdateMethod.valueOf(prefs.get("git_update_method", UpdateMethod.MERGE.name()));
        } catch (Exception e) {
            updateMethod = UpdateMethod.MERGE;
        }

        try {
            cleanWorkingTreeMethod = CleanWorkingTreeMethod.valueOf(prefs.get("git_clean_working_tree_method", CleanWorkingTreeMethod.SHELVE.name()));
        } catch (Exception e) {
            cleanWorkingTreeMethod = CleanWorkingTreeMethod.SHELVE;
        }

        try {
            filterUpdatePaths = FilterUpdatePaths.valueOf(prefs.get("git_filter_update_paths", FilterUpdatePaths.ALL.name()));
        } catch (Exception e) {
            filterUpdatePaths = FilterUpdatePaths.ALL;
        }

        try {
            incomingCommitsCheck = IncomingCommitsCheck.valueOf(prefs.get("git_incoming_commits_check", IncomingCommitsCheck.AUTO.name()));
        } catch (Exception e) {
            incomingCommitsCheck = IncomingCommitsCheck.AUTO;
        }

        try {
            fetchTagsMode = FetchTagsMode.valueOf(prefs.get("git_fetch_tags_mode", FetchTagsMode.AUTO_FOLLOW_GIT_CONFIG.name()));
        } catch (Exception e) {
            fetchTagsMode = FetchTagsMode.AUTO_FOLLOW_GIT_CONFIG;
        }

        useCredentialHelper = prefs.getBoolean("git_use_credential_helper", false);
        combineStashesAndShelves = prefs.getBoolean("git_combine_stashes_shelves", false);

        try {
            stashDiffComparison = StashDiffComparison.valueOf(prefs.get("git_stash_diff_comparison", StashDiffComparison.LOCAL_VERSION.name()));
        } catch (Exception e) {
            stashDiffComparison = StashDiffComparison.LOCAL_VERSION;
        }

        activateVirtualenvForHooks = prefs.getBoolean("git_activate_virtualenv_hooks", true);
    }

    private void savePreferences() {
        prefs.put("git_executable_path", gitExecutablePath);
        prefs.putBoolean("git_path_only_for_project", setPathOnlyForProject);
        prefs.putBoolean("git_auto_exclude_ignored_dirs", autoExcludeIgnoredDirectories);

        prefs.putBoolean("git_enable_staging_area", enableStagingArea);
        prefs.putBoolean("git_warn_crlf", warnCrlf);
        prefs.putBoolean("git_warn_detached_head", warnDetachedHead);
        prefs.putBoolean("git_warn_files_larger_enabled", warnFilesLargerThanEnabled);
        prefs.putInt("git_warn_files_larger_mb", warnFilesLargerThanMb);
        prefs.putBoolean("git_warn_cross_platform_filenames", warnCrossPlatformFilenames);
        prefs.putBoolean("git_add_cherry_pick_suffix", addCherryPickSuffix);
        prefs.putBoolean("git_sign_commits_with_gpg", signCommitsWithGpg);
        prefs.put("git_gpg_key_id", gpgKeyId);

        prefs.putBoolean("git_auto_update_rejected_push", autoUpdateOnRejectedPush);
        prefs.putBoolean("git_show_push_dialog", showPushDialog);
        prefs.putBoolean("git_show_push_dialog_only_protected", showPushDialogOnlyProtected);
        prefs.put("git_protected_branches", protectedBranches);
        prefs.putBoolean("git_load_branch_protection_github", loadBranchProtectionFromGitHub);

        prefs.put("git_update_method", updateMethod.name());
        prefs.put("git_clean_working_tree_method", cleanWorkingTreeMethod.name());
        prefs.put("git_filter_update_paths", filterUpdatePaths.name());
        prefs.put("git_incoming_commits_check", incomingCommitsCheck.name());
        prefs.put("git_fetch_tags_mode", fetchTagsMode.name());
        prefs.putBoolean("git_use_credential_helper", useCredentialHelper);

        prefs.putBoolean("git_combine_stashes_shelves", combineStashesAndShelves);
        prefs.put("git_stash_diff_comparison", stashDiffComparison.name());
        prefs.putBoolean("git_activate_virtualenv_hooks", activateVirtualenvForHooks);
    }
}
