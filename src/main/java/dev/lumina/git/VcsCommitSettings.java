package dev.lumina.git;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Central singleton managing Version Control Commit options matching IntelliJ IDEA:
 * - Clear initial commit message checkbox
 * - Commit message inspections (Blank line between subject and body, Limit body line,
 *   Limit subject line, Spelling) with configurable severity (Warning, Information, Error, Weak Warning)
 * - Commit Checks (Reformat code, Rearrange code, Optimize imports, Cleanup, Update copyright,
 *   Check malicious dependencies, Run rustfmt, Go fmt)
 * - Advanced Commit Checks (Analyze code, Check TODO, Run Configuration, Run advanced checks after commit is done)
 *
 * Backed by Java Preferences for dynamic persistence.
 */
public class VcsCommitSettings {

    public enum InspectionSeverity {
        WARNING("Warning"),
        INFO("Information"),
        ERROR("Error"),
        WEAK_WARNING("Weak Warning");

        private final String displayName;

        InspectionSeverity(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private static final VcsCommitSettings INSTANCE = new VcsCommitSettings();

    private final Preferences prefs = Preferences.userNodeForPackage(VcsCommitSettings.class);
    private final List<Runnable> listeners = new ArrayList<>();

    // Top setting
    private boolean clearInitialCommitMessage = false;

    // Commit message inspections
    private boolean inspectionBlankLine = false;
    private InspectionSeverity inspectionBlankLineSeverity = InspectionSeverity.WARNING;

    private boolean inspectionLimitBodyLine = false;
    private InspectionSeverity inspectionLimitBodyLineSeverity = InspectionSeverity.WARNING;

    private boolean inspectionLimitSubjectLine = false;
    private InspectionSeverity inspectionLimitSubjectLineSeverity = InspectionSeverity.WARNING;

    private boolean inspectionSpelling = true;
    private InspectionSeverity inspectionSpellingSeverity = InspectionSeverity.WARNING;

    // Commit Checks
    private boolean checkReformatCode = false;
    private boolean checkRearrangeCode = false;
    private boolean checkOptimizeImports = false;
    private boolean checkCleanup = false;
    private String cleanupProfile = "Default";
    private boolean checkUpdateCopyright = false;
    private boolean checkMaliciousDependencies = false;
    private boolean checkRunRustfmt = false;
    private boolean checkGoFmt = true;

    // Advanced Commit Checks
    private boolean checkAnalyzeCode = true;
    private String analysisProfile = "Default";
    private boolean checkTodo = true;
    private boolean checkRunConfiguration = false;
    private String runConfigurationName = "None";
    private boolean runAdvancedChecksAfterCommit = true;

    private VcsCommitSettings() {
        loadPreferences();
    }

    public static VcsCommitSettings getInstance() {
        return INSTANCE;
    }

    public synchronized boolean isClearInitialCommitMessage() {
        return clearInitialCommitMessage;
    }

    public synchronized void setClearInitialCommitMessage(boolean val) {
        if (this.clearInitialCommitMessage != val) {
            this.clearInitialCommitMessage = val;
            savePreferences();
            notifyListeners();
        }
    }

    // Inspections
    public synchronized boolean isInspectionBlankLine() { return inspectionBlankLine; }
    public synchronized void setInspectionBlankLine(boolean val) {
        if (this.inspectionBlankLine != val) {
            this.inspectionBlankLine = val;
            savePreferences();
            notifyListeners();
        }
    }
    public synchronized InspectionSeverity getInspectionBlankLineSeverity() { return inspectionBlankLineSeverity; }
    public synchronized void setInspectionBlankLineSeverity(InspectionSeverity s) {
        if (s != null && this.inspectionBlankLineSeverity != s) {
            this.inspectionBlankLineSeverity = s;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isInspectionLimitBodyLine() { return inspectionLimitBodyLine; }
    public synchronized void setInspectionLimitBodyLine(boolean val) {
        if (this.inspectionLimitBodyLine != val) {
            this.inspectionLimitBodyLine = val;
            savePreferences();
            notifyListeners();
        }
    }
    public synchronized InspectionSeverity getInspectionLimitBodyLineSeverity() { return inspectionLimitBodyLineSeverity; }
    public synchronized void setInspectionLimitBodyLineSeverity(InspectionSeverity s) {
        if (s != null && this.inspectionLimitBodyLineSeverity != s) {
            this.inspectionLimitBodyLineSeverity = s;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isInspectionLimitSubjectLine() { return inspectionLimitSubjectLine; }
    public synchronized void setInspectionLimitSubjectLine(boolean val) {
        if (this.inspectionLimitSubjectLine != val) {
            this.inspectionLimitSubjectLine = val;
            savePreferences();
            notifyListeners();
        }
    }
    public synchronized InspectionSeverity getInspectionLimitSubjectLineSeverity() { return inspectionLimitSubjectLineSeverity; }
    public synchronized void setInspectionLimitSubjectLineSeverity(InspectionSeverity s) {
        if (s != null && this.inspectionLimitSubjectLineSeverity != s) {
            this.inspectionLimitSubjectLineSeverity = s;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isInspectionSpelling() { return inspectionSpelling; }
    public synchronized void setInspectionSpelling(boolean val) {
        if (this.inspectionSpelling != val) {
            this.inspectionSpelling = val;
            savePreferences();
            notifyListeners();
        }
    }
    public synchronized InspectionSeverity getInspectionSpellingSeverity() { return inspectionSpellingSeverity; }
    public synchronized void setInspectionSpellingSeverity(InspectionSeverity s) {
        if (s != null && this.inspectionSpellingSeverity != s) {
            this.inspectionSpellingSeverity = s;
            savePreferences();
            notifyListeners();
        }
    }

    // Commit Checks
    public synchronized boolean isCheckReformatCode() { return checkReformatCode; }
    public synchronized void setCheckReformatCode(boolean val) {
        if (this.checkReformatCode != val) {
            this.checkReformatCode = val;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isCheckRearrangeCode() { return checkRearrangeCode; }
    public synchronized void setCheckRearrangeCode(boolean val) {
        if (this.checkRearrangeCode != val) {
            this.checkRearrangeCode = val;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isCheckOptimizeImports() { return checkOptimizeImports; }
    public synchronized void setCheckOptimizeImports(boolean val) {
        if (this.checkOptimizeImports != val) {
            this.checkOptimizeImports = val;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isCheckCleanup() { return checkCleanup; }
    public synchronized void setCheckCleanup(boolean val) {
        if (this.checkCleanup != val) {
            this.checkCleanup = val;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized String getCleanupProfile() { return cleanupProfile; }
    public synchronized void setCleanupProfile(String p) {
        if (p != null && !p.equals(this.cleanupProfile)) {
            this.cleanupProfile = p;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isCheckUpdateCopyright() { return checkUpdateCopyright; }
    public synchronized void setCheckUpdateCopyright(boolean val) {
        if (this.checkUpdateCopyright != val) {
            this.checkUpdateCopyright = val;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isCheckMaliciousDependencies() { return checkMaliciousDependencies; }
    public synchronized void setCheckMaliciousDependencies(boolean val) {
        if (this.checkMaliciousDependencies != val) {
            this.checkMaliciousDependencies = val;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isCheckRunRustfmt() { return checkRunRustfmt; }
    public synchronized void setCheckRunRustfmt(boolean val) {
        if (this.checkRunRustfmt != val) {
            this.checkRunRustfmt = val;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isCheckGoFmt() { return checkGoFmt; }
    public synchronized void setCheckGoFmt(boolean val) {
        if (this.checkGoFmt != val) {
            this.checkGoFmt = val;
            savePreferences();
            notifyListeners();
        }
    }

    // Advanced Commit Checks
    public synchronized boolean isCheckAnalyzeCode() { return checkAnalyzeCode; }
    public synchronized void setCheckAnalyzeCode(boolean val) {
        if (this.checkAnalyzeCode != val) {
            this.checkAnalyzeCode = val;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized String getAnalysisProfile() { return analysisProfile; }
    public synchronized void setAnalysisProfile(String p) {
        if (p != null && !p.equals(this.analysisProfile)) {
            this.analysisProfile = p;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isCheckTodo() { return checkTodo; }
    public synchronized void setCheckTodo(boolean val) {
        if (this.checkTodo != val) {
            this.checkTodo = val;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isCheckRunConfiguration() { return checkRunConfiguration; }
    public synchronized void setCheckRunConfiguration(boolean val) {
        if (this.checkRunConfiguration != val) {
            this.checkRunConfiguration = val;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized String getRunConfigurationName() { return runConfigurationName; }
    public synchronized void setRunConfigurationName(String n) {
        if (n != null && !n.equals(this.runConfigurationName)) {
            this.runConfigurationName = n;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isRunAdvancedChecksAfterCommit() { return runAdvancedChecksAfterCommit; }
    public synchronized void setRunAdvancedChecksAfterCommit(boolean val) {
        if (this.runAdvancedChecksAfterCommit != val) {
            this.runAdvancedChecksAfterCommit = val;
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized void revertToDefaults() {
        clearInitialCommitMessage = false;

        inspectionBlankLine = false;
        inspectionBlankLineSeverity = InspectionSeverity.WARNING;
        inspectionLimitBodyLine = false;
        inspectionLimitBodyLineSeverity = InspectionSeverity.WARNING;
        inspectionLimitSubjectLine = false;
        inspectionLimitSubjectLineSeverity = InspectionSeverity.WARNING;
        inspectionSpelling = true;
        inspectionSpellingSeverity = InspectionSeverity.WARNING;

        checkReformatCode = false;
        checkRearrangeCode = false;
        checkOptimizeImports = false;
        checkCleanup = false;
        cleanupProfile = "Default";
        checkUpdateCopyright = false;
        checkMaliciousDependencies = false;
        checkRunRustfmt = false;
        checkGoFmt = true;

        checkAnalyzeCode = true;
        analysisProfile = "Default";
        checkTodo = true;
        checkRunConfiguration = false;
        runConfigurationName = "None";
        runAdvancedChecksAfterCommit = true;

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
        clearInitialCommitMessage = prefs.getBoolean("vcs_clear_initial_commit_msg", false);

        inspectionBlankLine = prefs.getBoolean("vcs_insp_blank_line", false);
        inspectionBlankLineSeverity = loadSeverity("vcs_insp_blank_line_sev", InspectionSeverity.WARNING);

        inspectionLimitBodyLine = prefs.getBoolean("vcs_insp_limit_body", false);
        inspectionLimitBodyLineSeverity = loadSeverity("vcs_insp_limit_body_sev", InspectionSeverity.WARNING);

        inspectionLimitSubjectLine = prefs.getBoolean("vcs_insp_limit_subject", false);
        inspectionLimitSubjectLineSeverity = loadSeverity("vcs_insp_limit_subject_sev", InspectionSeverity.WARNING);

        inspectionSpelling = prefs.getBoolean("vcs_insp_spelling", true);
        inspectionSpellingSeverity = loadSeverity("vcs_insp_spelling_sev", InspectionSeverity.WARNING);

        checkReformatCode = prefs.getBoolean("vcs_check_reformat", false);
        checkRearrangeCode = prefs.getBoolean("vcs_check_rearrange", false);
        checkOptimizeImports = prefs.getBoolean("vcs_check_opt_imports", false);
        checkCleanup = prefs.getBoolean("vcs_check_cleanup", false);
        cleanupProfile = prefs.get("vcs_check_cleanup_profile", "Default");
        checkUpdateCopyright = prefs.getBoolean("vcs_check_copyright", false);
        checkMaliciousDependencies = prefs.getBoolean("vcs_check_malicious_deps", false);
        checkRunRustfmt = prefs.getBoolean("vcs_check_rustfmt", false);
        checkGoFmt = prefs.getBoolean("vcs_check_gofmt", true);

        checkAnalyzeCode = prefs.getBoolean("vcs_adv_analyze_code", true);
        analysisProfile = prefs.get("vcs_adv_analysis_profile", "Default");
        checkTodo = prefs.getBoolean("vcs_adv_check_todo", true);
        checkRunConfiguration = prefs.getBoolean("vcs_adv_run_config", false);
        runConfigurationName = prefs.get("vcs_adv_run_config_name", "None");
        runAdvancedChecksAfterCommit = prefs.getBoolean("vcs_adv_run_after_commit", true);
    }

    private InspectionSeverity loadSeverity(String key, InspectionSeverity defaultVal) {
        String val = prefs.get(key, defaultVal.name());
        try {
            return InspectionSeverity.valueOf(val);
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private void savePreferences() {
        prefs.putBoolean("vcs_clear_initial_commit_msg", clearInitialCommitMessage);

        prefs.putBoolean("vcs_insp_blank_line", inspectionBlankLine);
        prefs.put("vcs_insp_blank_line_sev", inspectionBlankLineSeverity.name());

        prefs.putBoolean("vcs_insp_limit_body", inspectionLimitBodyLine);
        prefs.put("vcs_insp_limit_body_sev", inspectionLimitBodyLineSeverity.name());

        prefs.putBoolean("vcs_insp_limit_subject", inspectionLimitSubjectLine);
        prefs.put("vcs_insp_limit_subject_sev", inspectionLimitSubjectLineSeverity.name());

        prefs.putBoolean("vcs_insp_spelling", inspectionSpelling);
        prefs.put("vcs_insp_spelling_sev", inspectionSpellingSeverity.name());

        prefs.putBoolean("vcs_check_reformat", checkReformatCode);
        prefs.putBoolean("vcs_check_rearrange", checkRearrangeCode);
        prefs.putBoolean("vcs_check_opt_imports", checkOptimizeImports);
        prefs.putBoolean("vcs_check_cleanup", checkCleanup);
        prefs.put("vcs_check_cleanup_profile", cleanupProfile);
        prefs.putBoolean("vcs_check_copyright", checkUpdateCopyright);
        prefs.putBoolean("vcs_check_malicious_deps", checkMaliciousDependencies);
        prefs.putBoolean("vcs_check_rustfmt", checkRunRustfmt);
        prefs.putBoolean("vcs_check_gofmt", checkGoFmt);

        prefs.putBoolean("vcs_adv_analyze_code", checkAnalyzeCode);
        prefs.put("vcs_adv_analysis_profile", analysisProfile);
        prefs.putBoolean("vcs_adv_check_todo", checkTodo);
        prefs.putBoolean("vcs_adv_run_config", checkRunConfiguration);
        prefs.put("vcs_adv_run_config_name", runConfigurationName);
        prefs.putBoolean("vcs_adv_run_after_commit", runAdvancedChecksAfterCommit);
    }
}
