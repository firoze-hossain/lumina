package dev.lumina.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Manages Subversion settings matching IntelliJ IDEA's "Version Control > Subversion".
 * Covers General, Network, and Presentation options backed by Java Preferences.
 */
public class SubversionSettingsManager {

    public enum SslProtocol {
        ALL("All"),
        SSLV3("SSLv3"),
        TLSV1("TLSv1");

        private final String label;
        SslProtocol(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum SshAuthMode {
        PASSWORD("Password"),
        PRIVATE_KEY("Private key"),
        SUBVERSION_CONFIG("Subversion config");

        private final String label;
        SshAuthMode(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private static final SubversionSettingsManager INSTANCE = new SubversionSettingsManager();

    private final Preferences prefs = Preferences.userNodeForPackage(SubversionSettingsManager.class);
    private final List<Runnable> listeners = new ArrayList<>();

    // 1. General Page
    private String svnExecutablePath = "svn";
    private boolean enableInteractiveMode = false;
    private boolean useCustomConfigDirectory = false;
    private String customConfigDirectoryPath = Path.of(System.getProperty("user.home"), ".subversion").toString();

    // 2. Network Page
    private boolean useGeneralProxySettings = false;
    private int httpTimeoutSeconds = 0;
    private int sshConnectionTimeoutSeconds = 30;
    private int sshReadTimeoutSeconds = 30;
    private SslProtocol sslProtocol = SslProtocol.ALL;

    // 3. Presentation Page
    private boolean checkMergeInfo = false;
    private int maxRevisionsLookBack = 500;
    private boolean showMergeSource = true;
    private boolean ignoreWhitespaceInAnnotations = true;

    // 4. SSH Page
    private String sshExecutablePath = "ssh";
    private String sshUserName = "";
    private int sshPort = 22;
    private SshAuthMode sshAuthMode = SshAuthMode.SUBVERSION_CONFIG;
    private String sshPrivateKeyPath = "";
    private String sshTunnel = "$SVN_SSH ssh -q";
    private String svnSshEnv = "";

    private SubversionSettingsManager() {
        loadPreferences();
    }

    public static SubversionSettingsManager getInstance() {
        return INSTANCE;
    }

    public boolean clearAuthCache() {
        Path authDir = Path.of(customConfigDirectoryPath).resolve("auth");
        if (Files.isDirectory(authDir)) {
            try (var stream = Files.walk(authDir)) {
                for (Path p : stream.sorted((a, b) -> b.compareTo(a)).toList()) {
                    if (!p.equals(authDir)) {
                        Files.deleteIfExists(p);
                    }
                }
                return true;
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    // --- General Getters & Setters ---

    public synchronized String getSvnExecutablePath() { return svnExecutablePath; }
    public synchronized void setSvnExecutablePath(String path) {
        this.svnExecutablePath = path != null && !path.isBlank() ? path.trim() : "svn";
        prefs.put("svn_executable_path", this.svnExecutablePath);
        notifyListeners();
    }

    public synchronized boolean isEnableInteractiveMode() { return enableInteractiveMode; }
    public synchronized void setEnableInteractiveMode(boolean val) {
        if (this.enableInteractiveMode != val) {
            this.enableInteractiveMode = val;
            prefs.putBoolean("svn_interactive_mode", val);
            notifyListeners();
        }
    }

    public synchronized boolean isUseCustomConfigDirectory() { return useCustomConfigDirectory; }
    public synchronized void setUseCustomConfigDirectory(boolean val) {
        if (this.useCustomConfigDirectory != val) {
            this.useCustomConfigDirectory = val;
            prefs.putBoolean("svn_use_custom_config_dir", val);
            notifyListeners();
        }
    }

    public synchronized String getCustomConfigDirectoryPath() { return customConfigDirectoryPath; }
    public synchronized void setCustomConfigDirectoryPath(String path) {
        this.customConfigDirectoryPath = path != null ? path.trim() : "";
        prefs.put("svn_custom_config_dir_path", this.customConfigDirectoryPath);
        notifyListeners();
    }

    // --- Network Getters & Setters ---

    public synchronized boolean isUseGeneralProxySettings() { return useGeneralProxySettings; }
    public synchronized void setUseGeneralProxySettings(boolean val) {
        if (this.useGeneralProxySettings != val) {
            this.useGeneralProxySettings = val;
            prefs.putBoolean("svn_use_general_proxy", val);
            notifyListeners();
        }
    }

    public synchronized int getHttpTimeoutSeconds() { return httpTimeoutSeconds; }
    public synchronized void setHttpTimeoutSeconds(int val) {
        int v = Math.max(0, val);
        if (this.httpTimeoutSeconds != v) {
            this.httpTimeoutSeconds = v;
            prefs.putInt("svn_http_timeout", v);
            notifyListeners();
        }
    }

    public synchronized int getSshConnectionTimeoutSeconds() { return sshConnectionTimeoutSeconds; }
    public synchronized void setSshConnectionTimeoutSeconds(int val) {
        int v = Math.max(0, val);
        if (this.sshConnectionTimeoutSeconds != v) {
            this.sshConnectionTimeoutSeconds = v;
            prefs.putInt("svn_ssh_conn_timeout", v);
            notifyListeners();
        }
    }

    public synchronized int getSshReadTimeoutSeconds() { return sshReadTimeoutSeconds; }
    public synchronized void setSshReadTimeoutSeconds(int val) {
        int v = Math.max(0, val);
        if (this.sshReadTimeoutSeconds != v) {
            this.sshReadTimeoutSeconds = v;
            prefs.putInt("svn_ssh_read_timeout", v);
            notifyListeners();
        }
    }

    public synchronized SslProtocol getSslProtocol() { return sslProtocol; }
    public synchronized void setSslProtocol(SslProtocol proto) {
        if (proto != null && this.sslProtocol != proto) {
            this.sslProtocol = proto;
            prefs.put("svn_ssl_protocol", proto.name());
            notifyListeners();
        }
    }

    // --- Presentation Getters & Setters ---

    public synchronized boolean isCheckMergeInfo() { return checkMergeInfo; }
    public synchronized void setCheckMergeInfo(boolean val) {
        if (this.checkMergeInfo != val) {
            this.checkMergeInfo = val;
            prefs.putBoolean("svn_check_merge_info", val);
            notifyListeners();
        }
    }

    public synchronized int getMaxRevisionsLookBack() { return maxRevisionsLookBack; }
    public synchronized void setMaxRevisionsLookBack(int val) {
        int v = Math.max(1, val);
        if (this.maxRevisionsLookBack != v) {
            this.maxRevisionsLookBack = v;
            prefs.putInt("svn_max_revisions_look_back", v);
            notifyListeners();
        }
    }

    public synchronized boolean isShowMergeSource() { return showMergeSource; }
    public synchronized void setShowMergeSource(boolean val) {
        if (this.showMergeSource != val) {
            this.showMergeSource = val;
            prefs.putBoolean("svn_show_merge_source", val);
            notifyListeners();
        }
    }

    public synchronized boolean isIgnoreWhitespaceInAnnotations() { return ignoreWhitespaceInAnnotations; }
    public synchronized void setIgnoreWhitespaceInAnnotations(boolean val) {
        if (this.ignoreWhitespaceInAnnotations != val) {
            this.ignoreWhitespaceInAnnotations = val;
            prefs.putBoolean("svn_ignore_whitespace_annotations", val);
            notifyListeners();
        }
    }

    // --- SSH Getters & Setters ---

    public synchronized String getSshExecutablePath() { return sshExecutablePath; }
    public synchronized void setSshExecutablePath(String path) {
        this.sshExecutablePath = path != null && !path.isBlank() ? path.trim() : "ssh";
        prefs.put("svn_ssh_executable", this.sshExecutablePath);
        notifyListeners();
    }

    public synchronized String getSshUserName() { return sshUserName; }
    public synchronized void setSshUserName(String name) {
        this.sshUserName = name != null ? name : "";
        prefs.put("svn_ssh_user", this.sshUserName);
        notifyListeners();
    }

    public synchronized int getSshPort() { return sshPort; }
    public synchronized void setSshPort(int port) {
        int p = port > 0 ? port : 22;
        if (this.sshPort != p) {
            this.sshPort = p;
            prefs.putInt("svn_ssh_port", p);
            notifyListeners();
        }
    }

    public synchronized SshAuthMode getSshAuthMode() { return sshAuthMode; }
    public synchronized void setSshAuthMode(SshAuthMode mode) {
        if (mode != null && this.sshAuthMode != mode) {
            this.sshAuthMode = mode;
            prefs.put("svn_ssh_auth_mode", mode.name());
            notifyListeners();
        }
    }

    public synchronized String getSshPrivateKeyPath() { return sshPrivateKeyPath; }
    public synchronized void setSshPrivateKeyPath(String path) {
        this.sshPrivateKeyPath = path != null ? path.trim() : "";
        prefs.put("svn_ssh_private_key", this.sshPrivateKeyPath);
        notifyListeners();
    }

    public synchronized String getSshTunnel() { return sshTunnel; }
    public synchronized void setSshTunnel(String tunnel) {
        this.sshTunnel = tunnel != null ? tunnel : "$SVN_SSH ssh -q";
        prefs.put("svn_ssh_tunnel", this.sshTunnel);
        notifyListeners();
    }

    public synchronized String getSvnSshEnv() { return svnSshEnv; }
    public synchronized void setSvnSshEnv(String env) {
        this.svnSshEnv = env != null ? env : "";
        prefs.put("svn_ssh_env", this.svnSshEnv);
        notifyListeners();
    }

    public synchronized void updateSshTunnel() {
        String envVal = System.getenv("SVN_SSH");
        if (envVal != null && !envVal.isBlank()) {
            this.svnSshEnv = envVal;
            this.sshTunnel = envVal + " -q";
        } else {
            this.sshTunnel = "$SVN_SSH ssh -q";
        }
        prefs.put("svn_ssh_tunnel", this.sshTunnel);
        prefs.put("svn_ssh_env", this.svnSshEnv);
        notifyListeners();
    }

    public synchronized void revertToDefaults() {
        svnExecutablePath = "svn";
        enableInteractiveMode = false;
        useCustomConfigDirectory = false;
        customConfigDirectoryPath = Path.of(System.getProperty("user.home"), ".subversion").toString();

        useGeneralProxySettings = false;
        httpTimeoutSeconds = 0;
        sshConnectionTimeoutSeconds = 30;
        sshReadTimeoutSeconds = 30;
        sslProtocol = SslProtocol.ALL;

        checkMergeInfo = false;
        maxRevisionsLookBack = 500;
        showMergeSource = true;
        ignoreWhitespaceInAnnotations = true;

        sshExecutablePath = "ssh";
        sshUserName = "";
        sshPort = 22;
        sshAuthMode = SshAuthMode.SUBVERSION_CONFIG;
        sshPrivateKeyPath = "";
        sshTunnel = "$SVN_SSH ssh -q";
        svnSshEnv = "";

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
        svnExecutablePath = prefs.get("svn_executable_path", "svn");
        enableInteractiveMode = prefs.getBoolean("svn_interactive_mode", false);
        useCustomConfigDirectory = prefs.getBoolean("svn_use_custom_config_dir", false);
        customConfigDirectoryPath = prefs.get("svn_custom_config_dir_path", Path.of(System.getProperty("user.home"), ".subversion").toString());

        useGeneralProxySettings = prefs.getBoolean("svn_use_general_proxy", false);
        httpTimeoutSeconds = prefs.getInt("svn_http_timeout", 0);
        sshConnectionTimeoutSeconds = prefs.getInt("svn_ssh_conn_timeout", 30);
        sshReadTimeoutSeconds = prefs.getInt("svn_ssh_read_timeout", 30);
        try {
            sslProtocol = SslProtocol.valueOf(prefs.get("svn_ssl_protocol", SslProtocol.ALL.name()));
        } catch (Exception e) {
            sslProtocol = SslProtocol.ALL;
        }

        checkMergeInfo = prefs.getBoolean("svn_check_merge_info", false);
        maxRevisionsLookBack = prefs.getInt("svn_max_revisions_look_back", 500);
        showMergeSource = prefs.getBoolean("svn_show_merge_source", true);
        ignoreWhitespaceInAnnotations = prefs.getBoolean("svn_ignore_whitespace_annotations", true);

        sshExecutablePath = prefs.get("svn_ssh_executable", "ssh");
        sshUserName = prefs.get("svn_ssh_user", "");
        sshPort = prefs.getInt("svn_ssh_port", 22);
        try {
            sshAuthMode = SshAuthMode.valueOf(prefs.get("svn_ssh_auth_mode", SshAuthMode.SUBVERSION_CONFIG.name()));
        } catch (Exception e) {
            sshAuthMode = SshAuthMode.SUBVERSION_CONFIG;
        }
        sshPrivateKeyPath = prefs.get("svn_ssh_private_key", "");
        sshTunnel = prefs.get("svn_ssh_tunnel", "$SVN_SSH ssh -q");
        svnSshEnv = prefs.get("svn_ssh_env", "");
    }

    private void savePreferences() {
        prefs.put("svn_executable_path", svnExecutablePath);
        prefs.putBoolean("svn_interactive_mode", enableInteractiveMode);
        prefs.putBoolean("svn_use_custom_config_dir", useCustomConfigDirectory);
        prefs.put("svn_custom_config_dir_path", customConfigDirectoryPath);

        prefs.putBoolean("svn_use_general_proxy", useGeneralProxySettings);
        prefs.putInt("svn_http_timeout", httpTimeoutSeconds);
        prefs.putInt("svn_ssh_conn_timeout", sshConnectionTimeoutSeconds);
        prefs.putInt("svn_ssh_read_timeout", sshReadTimeoutSeconds);
        prefs.put("svn_ssl_protocol", sslProtocol.name());

        prefs.putBoolean("svn_check_merge_info", checkMergeInfo);
        prefs.putInt("svn_max_revisions_look_back", maxRevisionsLookBack);
        prefs.putBoolean("svn_show_merge_source", showMergeSource);
        prefs.putBoolean("svn_ignore_whitespace_annotations", ignoreWhitespaceInAnnotations);

        prefs.put("svn_ssh_executable", sshExecutablePath);
        prefs.put("svn_ssh_user", sshUserName);
        prefs.putInt("svn_ssh_port", sshPort);
        prefs.put("svn_ssh_auth_mode", sshAuthMode.name());
        prefs.put("svn_ssh_private_key", sshPrivateKeyPath);
        prefs.put("svn_ssh_tunnel", sshTunnel);
        prefs.put("svn_ssh_env", svnSshEnv);
    }
}
