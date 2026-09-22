package dev.lumina.git;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Manages Perforce VCS settings matching IntelliJ IDEA's "Version Control > Perforce".
 * Backed by Java Preferences for dynamic persistence.
 */
public class PerforceSettingsManager {

    public enum ConfigMode {
        ENVIRONMENT_VALUES,
        CONNECTION_PARAMETERS
    }

    public enum IgnoreMode {
        P4IGNORE_ENV,
        IGNORE_SETTINGS
    }

    public record TestResult(boolean success, String message) {}

    private static final PerforceSettingsManager INSTANCE = new PerforceSettingsManager();

    private final Preferences prefs = Preferences.userNodeForPackage(PerforceSettingsManager.class);
    private final List<Runnable> listeners = new ArrayList<>();

    private boolean perforceOnline = true;
    private boolean switchOfflineAuto = false;

    // Config Settings
    private String charset = "none";
    private ConfigMode configMode = ConfigMode.ENVIRONMENT_VALUES;
    private String serverPort = "<perforce_server>:1666";
    private String user = "";
    private String clientWorkspace = "";

    // Ignore Settings
    private IgnoreMode ignoreMode = IgnoreMode.P4IGNORE_ENV;
    private String pathToIgnoreFile = ".p4ignore";

    // Command Dump & Login
    private boolean dumpCommands = false;
    private String logFilePath = Path.of(System.getProperty("user.home"), ".lumina", "log", "p4output.log").toString();
    private boolean useLoginAuthentication = true;

    // Executables
    private String p4ExecutablePath = "p4";
    private String p4vcExecutablePath = "p4vc";

    // Options
    private boolean showBranchingHistory = true;
    private boolean showIntegratedChangelists = true;
    private int serverTimeoutSeconds = 20;
    private boolean enableJobsSupport = false;
    private boolean findIgnoredFilesUsingP4 = true;
    private boolean alwaysSyncLocalChangelists = true;

    private PerforceSettingsManager() {
        loadPreferences();
    }

    public static PerforceSettingsManager getInstance() {
        return INSTANCE;
    }

    public TestResult testConnection() {
        if (!perforceOnline) {
            return new TestResult(false, "Perforce is currently in offline mode.");
        }
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(p4ExecutablePath);
            if (configMode == ConfigMode.CONNECTION_PARAMETERS && !serverPort.isBlank()) {
                cmd.add("-p");
                cmd.add(serverPort);
            }
            if (configMode == ConfigMode.CONNECTION_PARAMETERS && !user.isBlank()) {
                cmd.add("-u");
                cmd.add(user);
            }
            if (configMode == ConfigMode.CONNECTION_PARAMETERS && !clientWorkspace.isBlank()) {
                cmd.add("-c");
                cmd.add(clientWorkspace);
            }
            cmd.add("info");

            Process p = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int code = p.waitFor();
            if (code == 0) {
                return new TestResult(true, "Connected to Perforce server successfully.");
            } else {
                return new TestResult(false, "Cannot connect: " + out.trim());
            }
        } catch (Exception e) {
            return new TestResult(false, "Error executing '" + p4ExecutablePath + "': " + e.getMessage());
        }
    }

    // --- Getters & Setters ---

    public synchronized boolean isPerforceOnline() { return perforceOnline; }
    public synchronized void setPerforceOnline(boolean val) {
        if (this.perforceOnline != val) {
            this.perforceOnline = val;
            prefs.putBoolean("p4_online", val);
            notifyListeners();
        }
    }

    public synchronized boolean isSwitchOfflineAuto() { return switchOfflineAuto; }
    public synchronized void setSwitchOfflineAuto(boolean val) {
        if (this.switchOfflineAuto != val) {
            this.switchOfflineAuto = val;
            prefs.putBoolean("p4_switch_offline_auto", val);
            notifyListeners();
        }
    }

    public synchronized String getCharset() { return charset; }
    public synchronized void setCharset(String val) {
        this.charset = val != null ? val : "none";
        prefs.put("p4_charset", this.charset);
        notifyListeners();
    }

    public synchronized ConfigMode getConfigMode() { return configMode; }
    public synchronized void setConfigMode(ConfigMode mode) {
        if (mode != null && this.configMode != mode) {
            this.configMode = mode;
            prefs.put("p4_config_mode", mode.name());
            notifyListeners();
        }
    }

    public synchronized String getServerPort() { return serverPort; }
    public synchronized void setServerPort(String val) {
        this.serverPort = val != null ? val : "";
        prefs.put("p4_server_port", this.serverPort);
        notifyListeners();
    }

    public synchronized String getUser() { return user; }
    public synchronized void setUser(String val) {
        this.user = val != null ? val : "";
        prefs.put("p4_user", this.user);
        notifyListeners();
    }

    public synchronized String getClientWorkspace() { return clientWorkspace; }
    public synchronized void setClientWorkspace(String val) {
        this.clientWorkspace = val != null ? val : "";
        prefs.put("p4_client_workspace", this.clientWorkspace);
        notifyListeners();
    }

    public synchronized IgnoreMode getIgnoreMode() { return ignoreMode; }
    public synchronized void setIgnoreMode(IgnoreMode mode) {
        if (mode != null && this.ignoreMode != mode) {
            this.ignoreMode = mode;
            prefs.put("p4_ignore_mode", mode.name());
            notifyListeners();
        }
    }

    public synchronized String getPathToIgnoreFile() { return pathToIgnoreFile; }
    public synchronized void setPathToIgnoreFile(String val) {
        this.pathToIgnoreFile = val != null ? val : ".p4ignore";
        prefs.put("p4_path_to_ignore_file", this.pathToIgnoreFile);
        notifyListeners();
    }

    public synchronized boolean isDumpCommands() { return dumpCommands; }
    public synchronized void setDumpCommands(boolean val) {
        if (this.dumpCommands != val) {
            this.dumpCommands = val;
            prefs.putBoolean("p4_dump_commands", val);
            notifyListeners();
        }
    }

    public synchronized String getLogFilePath() { return logFilePath; }
    public synchronized void setLogFilePath(String val) {
        this.logFilePath = val != null ? val : "";
        prefs.put("p4_log_file_path", this.logFilePath);
        notifyListeners();
    }

    public synchronized boolean isUseLoginAuthentication() { return useLoginAuthentication; }
    public synchronized void setUseLoginAuthentication(boolean val) {
        if (this.useLoginAuthentication != val) {
            this.useLoginAuthentication = val;
            prefs.putBoolean("p4_use_login_auth", val);
            notifyListeners();
        }
    }

    public synchronized String getP4ExecutablePath() { return p4ExecutablePath; }
    public synchronized void setP4ExecutablePath(String val) {
        this.p4ExecutablePath = val != null && !val.isBlank() ? val.trim() : "p4";
        prefs.put("p4_executable_path", this.p4ExecutablePath);
        notifyListeners();
    }

    public synchronized String getP4vcExecutablePath() { return p4vcExecutablePath; }
    public synchronized void setP4vcExecutablePath(String val) {
        this.p4vcExecutablePath = val != null && !val.isBlank() ? val.trim() : "p4vc";
        prefs.put("p4vc_executable_path", this.p4vcExecutablePath);
        notifyListeners();
    }

    public synchronized boolean isShowBranchingHistory() { return showBranchingHistory; }
    public synchronized void setShowBranchingHistory(boolean val) {
        if (this.showBranchingHistory != val) {
            this.showBranchingHistory = val;
            prefs.putBoolean("p4_show_branching_history", val);
            notifyListeners();
        }
    }

    public synchronized boolean isShowIntegratedChangelists() { return showIntegratedChangelists; }
    public synchronized void setShowIntegratedChangelists(boolean val) {
        if (this.showIntegratedChangelists != val) {
            this.showIntegratedChangelists = val;
            prefs.putBoolean("p4_show_integrated_changelists", val);
            notifyListeners();
        }
    }

    public synchronized int getServerTimeoutSeconds() { return serverTimeoutSeconds; }
    public synchronized void setServerTimeoutSeconds(int val) {
        int v = Math.max(1, val);
        if (this.serverTimeoutSeconds != v) {
            this.serverTimeoutSeconds = v;
            prefs.putInt("p4_server_timeout", v);
            notifyListeners();
        }
    }

    public synchronized boolean isEnableJobsSupport() { return enableJobsSupport; }
    public synchronized void setEnableJobsSupport(boolean val) {
        if (this.enableJobsSupport != val) {
            this.enableJobsSupport = val;
            prefs.putBoolean("p4_enable_jobs_support", val);
            notifyListeners();
        }
    }

    public synchronized boolean isFindIgnoredFilesUsingP4() { return findIgnoredFilesUsingP4; }
    public synchronized void setFindIgnoredFilesUsingP4(boolean val) {
        if (this.findIgnoredFilesUsingP4 != val) {
            this.findIgnoredFilesUsingP4 = val;
            prefs.putBoolean("p4_find_ignored_files", val);
            notifyListeners();
        }
    }

    public synchronized boolean isAlwaysSyncLocalChangelists() { return alwaysSyncLocalChangelists; }
    public synchronized void setAlwaysSyncLocalChangelists(boolean val) {
        if (this.alwaysSyncLocalChangelists != val) {
            this.alwaysSyncLocalChangelists = val;
            prefs.putBoolean("p4_always_sync_changelists", val);
            notifyListeners();
        }
    }

    public synchronized void revertToDefaults() {
        perforceOnline = true;
        switchOfflineAuto = false;
        charset = "none";
        configMode = ConfigMode.ENVIRONMENT_VALUES;
        serverPort = "<perforce_server>:1666";
        user = "";
        clientWorkspace = "";
        ignoreMode = IgnoreMode.P4IGNORE_ENV;
        pathToIgnoreFile = ".p4ignore";
        dumpCommands = false;
        useLoginAuthentication = true;
        p4ExecutablePath = "p4";
        p4vcExecutablePath = "p4vc";
        showBranchingHistory = true;
        showIntegratedChangelists = true;
        serverTimeoutSeconds = 20;
        enableJobsSupport = false;
        findIgnoredFilesUsingP4 = true;
        alwaysSyncLocalChangelists = true;
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
        perforceOnline = prefs.getBoolean("p4_online", true);
        switchOfflineAuto = prefs.getBoolean("p4_switch_offline_auto", false);
        charset = prefs.get("p4_charset", "none");
        try {
            configMode = ConfigMode.valueOf(prefs.get("p4_config_mode", ConfigMode.ENVIRONMENT_VALUES.name()));
        } catch (Exception e) {
            configMode = ConfigMode.ENVIRONMENT_VALUES;
        }
        serverPort = prefs.get("p4_server_port", "<perforce_server>:1666");
        user = prefs.get("p4_user", "");
        clientWorkspace = prefs.get("p4_client_workspace", "");
        try {
            ignoreMode = IgnoreMode.valueOf(prefs.get("p4_ignore_mode", IgnoreMode.P4IGNORE_ENV.name()));
        } catch (Exception e) {
            ignoreMode = IgnoreMode.P4IGNORE_ENV;
        }
        pathToIgnoreFile = prefs.get("p4_path_to_ignore_file", ".p4ignore");
        dumpCommands = prefs.getBoolean("p4_dump_commands", false);
        logFilePath = prefs.get("p4_log_file_path", Path.of(System.getProperty("user.home"), ".lumina", "log", "p4output.log").toString());
        useLoginAuthentication = prefs.getBoolean("p4_use_login_auth", true);
        p4ExecutablePath = prefs.get("p4_executable_path", "p4");
        p4vcExecutablePath = prefs.get("p4vc_executable_path", "p4vc");
        showBranchingHistory = prefs.getBoolean("p4_show_branching_history", true);
        showIntegratedChangelists = prefs.getBoolean("p4_show_integrated_changelists", true);
        serverTimeoutSeconds = prefs.getInt("p4_server_timeout", 20);
        enableJobsSupport = prefs.getBoolean("p4_enable_jobs_support", false);
        findIgnoredFilesUsingP4 = prefs.getBoolean("p4_find_ignored_files", true);
        alwaysSyncLocalChangelists = prefs.getBoolean("p4_always_sync_changelists", true);
    }

    private void savePreferences() {
        prefs.putBoolean("p4_online", perforceOnline);
        prefs.putBoolean("p4_switch_offline_auto", switchOfflineAuto);
        prefs.put("p4_charset", charset);
        prefs.put("p4_config_mode", configMode.name());
        prefs.put("p4_server_port", serverPort);
        prefs.put("p4_user", user);
        prefs.put("p4_client_workspace", clientWorkspace);
        prefs.put("p4_ignore_mode", ignoreMode.name());
        prefs.put("p4_path_to_ignore_file", pathToIgnoreFile);
        prefs.putBoolean("p4_dump_commands", dumpCommands);
        prefs.put("p4_log_file_path", logFilePath);
        prefs.putBoolean("p4_use_login_auth", useLoginAuthentication);
        prefs.put("p4_executable_path", p4ExecutablePath);
        prefs.put("p4vc_executable_path", p4vcExecutablePath);
        prefs.putBoolean("p4_show_branching_history", showBranchingHistory);
        prefs.putBoolean("p4_show_integrated_changelists", showIntegratedChangelists);
        prefs.putInt("p4_server_timeout", serverTimeoutSeconds);
        prefs.putBoolean("p4_enable_jobs_support", enableJobsSupport);
        prefs.putBoolean("p4_find_ignored_files", findIgnoredFilesUsingP4);
        prefs.putBoolean("p4_always_sync_changelists", alwaysSyncLocalChangelists);
    }
}
