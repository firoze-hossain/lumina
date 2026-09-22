package dev.lumina.git;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Manages Mercurial settings matching IntelliJ IDEA's "Version Control > Mercurial".
 * Backed by Java Preferences for dynamic persistence.
 */
public class MercurialSettingsManager {

    public record TestResult(boolean success, String message) {}

    private static final MercurialSettingsManager INSTANCE = new MercurialSettingsManager();

    private final Preferences prefs = Preferences.userNodeForPackage(MercurialSettingsManager.class);
    private final List<Runnable> listeners = new ArrayList<>();

    private String hgExecutablePath = "";
    private boolean setPathOnlyForProject = false;
    private boolean checkIncomingOutgoingChangesets = false;
    private boolean ignoreWhitespaceInAnnotations = true;

    private MercurialSettingsManager() {
        loadPreferences();
    }

    public static MercurialSettingsManager getInstance() {
        return INSTANCE;
    }

    public String getAutoDetectedHgPath() {
        String[] candidates = {
                "/usr/bin/hg",
                "/usr/local/bin/hg",
                "/bin/hg",
                "/opt/homebrew/bin/hg",
                "C:\\Program Files\\TortoiseHg\\hg.exe",
                "C:\\Program Files (x86)\\TortoiseHg\\hg.exe"
        };
        for (String c : candidates) {
            if (new File(c).canExecute()) {
                return c;
            }
        }
        return "hg";
    }

    public String getEffectiveHgExecutable() {
        if (hgExecutablePath != null && !hgExecutablePath.isBlank()) {
            return hgExecutablePath.trim();
        }
        return getAutoDetectedHgPath();
    }

    public TestResult testHgExecutable(String path) {
        String exec = (path != null && !path.isBlank()) ? path.trim() : getEffectiveHgExecutable();
        try {
            Process p = new ProcessBuilder(exec, "--version")
                    .redirectErrorStream(true)
                    .start();
            String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            int code = p.waitFor();
            if (code == 0) {
                // e.g. "Mercurial Distributed SCM (version 6.5.3)" -> "Mercurial version is 6.5.3"
                String firstLine = output.split("\\R")[0];
                return new TestResult(true, firstLine);
            } else {
                return new TestResult(false, "Mercurial exited with code " + code + ": " + output);
            }
        } catch (Exception e) {
            return new TestResult(false, "Cannot run Mercurial at '" + exec + "': " + e.getMessage());
        }
    }

    public synchronized String getHgExecutablePath() { return hgExecutablePath; }
    public synchronized void setHgExecutablePath(String path) {
        this.hgExecutablePath = path != null ? path.trim() : "";
        prefs.put("hg_executable_path", this.hgExecutablePath);
        notifyListeners();
    }

    public synchronized boolean isSetPathOnlyForProject() { return setPathOnlyForProject; }
    public synchronized void setSetPathOnlyForProject(boolean val) {
        this.setPathOnlyForProject = val;
        prefs.putBoolean("hg_path_only_for_project", val);
        notifyListeners();
    }

    public synchronized boolean isCheckIncomingOutgoingChangesets() { return checkIncomingOutgoingChangesets; }
    public synchronized void setCheckIncomingOutgoingChangesets(boolean val) {
        if (this.checkIncomingOutgoingChangesets != val) {
            this.checkIncomingOutgoingChangesets = val;
            prefs.putBoolean("hg_check_incoming_outgoing_changesets", val);
            notifyListeners();
        }
    }

    public synchronized boolean isIgnoreWhitespaceInAnnotations() { return ignoreWhitespaceInAnnotations; }
    public synchronized void setIgnoreWhitespaceInAnnotations(boolean val) {
        if (this.ignoreWhitespaceInAnnotations != val) {
            this.ignoreWhitespaceInAnnotations = val;
            prefs.putBoolean("hg_ignore_whitespace_annotations", val);
            notifyListeners();
        }
    }

    public synchronized void revertToDefaults() {
        hgExecutablePath = "";
        setPathOnlyForProject = false;
        checkIncomingOutgoingChangesets = false;
        ignoreWhitespaceInAnnotations = true;
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
        hgExecutablePath = prefs.get("hg_executable_path", "");
        setPathOnlyForProject = prefs.getBoolean("hg_path_only_for_project", false);
        checkIncomingOutgoingChangesets = prefs.getBoolean("hg_check_incoming_outgoing_changesets", false);
        ignoreWhitespaceInAnnotations = prefs.getBoolean("hg_ignore_whitespace_annotations", true);
    }

    private void savePreferences() {
        prefs.put("hg_executable_path", hgExecutablePath);
        prefs.putBoolean("hg_path_only_for_project", setPathOnlyForProject);
        prefs.putBoolean("hg_check_incoming_outgoing_changesets", checkIncomingOutgoingChangesets);
        prefs.putBoolean("hg_ignore_whitespace_annotations", ignoreWhitespaceInAnnotations);
    }
}
