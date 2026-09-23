package dev.lumina.git;

import dev.lumina.util.Settings;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * Manages GitHub accounts and settings matching IntelliJ IDEA's "Version Control > GitHub".
 * Backed by Java Preferences for dynamic persistence.
 */
public class GitHubAccountManager {

    public static final class GitHubAccount {
        private String name;
        private String username;
        private String server;
        private String token;
        private String avatarUrl;
        private boolean isDefault;

        public GitHubAccount(String name, String username, String server, String token, String avatarUrl, boolean isDefault) {
            this.name = name != null ? name.trim() : "";
            this.username = username != null ? username.trim() : "";
            this.server = server != null && !server.isBlank() ? server.trim() : "github.com";
            this.token = token != null ? token.trim() : "";
            this.avatarUrl = avatarUrl != null ? avatarUrl.trim() : "";
            this.isDefault = isDefault;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getServer() { return server; }
        public void setServer(String server) { this.server = server; }

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }

        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

        public boolean isDefault() { return isDefault; }
        public void setDefault(boolean isDefault) { this.isDefault = isDefault; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GitHubAccount that)) return false;
            return Objects.equals(username, that.username) && Objects.equals(server, that.server);
        }

        @Override
        public int hashCode() {
            return Objects.hash(username, server);
        }
    }

    private static final GitHubAccountManager INSTANCE = new GitHubAccountManager();

    private final Preferences prefs = Preferences.userNodeForPackage(GitHubAccountManager.class);
    private final List<Runnable> listeners = new ArrayList<>();
    private final List<GitHubAccount> accounts = new ArrayList<>();

    private boolean cloneUsingSsh = false;
    private boolean autoMarkFilesAsViewed = false;
    private boolean enableUnreadMarkersOnPullRequests = true;
    private int connectionTimeoutSeconds = 5;

    private GitHubAccountManager() {
        loadPreferences();
    }

    public static GitHubAccountManager getInstance() {
        return INSTANCE;
    }

    public synchronized List<GitHubAccount> getAccounts() {
        return Collections.unmodifiableList(new ArrayList<>(accounts));
    }

    public synchronized void setAccounts(List<GitHubAccount> newAccounts) {
        accounts.clear();
        if (newAccounts != null) {
            accounts.addAll(newAccounts);
        }
        ensureDefaultAccount();
        savePreferences();
        notifyListeners();
    }

    public synchronized void addAccount(GitHubAccount account) {
        if (account == null || account.getUsername().isBlank()) return;
        accounts.remove(account);
        accounts.add(account);
        ensureDefaultAccount();
        savePreferences();
        notifyListeners();
    }

    public synchronized void removeAccount(GitHubAccount account) {
        if (accounts.remove(account)) {
            ensureDefaultAccount();
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized void setDefaultAccount(GitHubAccount account) {
        for (GitHubAccount a : accounts) {
            a.setDefault(a.equals(account));
        }
        savePreferences();
        notifyListeners();
    }

    public synchronized GitHubAccount getDefaultAccount() {
        for (GitHubAccount a : accounts) {
            if (a.isDefault()) return a;
        }
        return accounts.isEmpty() ? null : accounts.get(0);
    }

    private void ensureDefaultAccount() {
        if (accounts.isEmpty()) return;
        boolean hasDefault = false;
        for (GitHubAccount a : accounts) {
            if (a.isDefault()) {
                hasDefault = true;
                break;
            }
        }
        if (!hasDefault) {
            accounts.get(0).setDefault(true);
        }
    }

    // --- Options Getters & Setters ---

    public synchronized boolean isCloneUsingSsh() { return cloneUsingSsh; }
    public synchronized void setCloneUsingSsh(boolean val) {
        if (this.cloneUsingSsh != val) {
            this.cloneUsingSsh = val;
            prefs.putBoolean("github_clone_using_ssh", val);
            notifyListeners();
        }
    }

    public synchronized boolean isAutoMarkFilesAsViewed() { return autoMarkFilesAsViewed; }
    public synchronized void setAutoMarkFilesAsViewed(boolean val) {
        if (this.autoMarkFilesAsViewed != val) {
            this.autoMarkFilesAsViewed = val;
            prefs.putBoolean("github_auto_mark_files_viewed", val);
            notifyListeners();
        }
    }

    public synchronized boolean isEnableUnreadMarkersOnPullRequests() { return enableUnreadMarkersOnPullRequests; }
    public synchronized void setEnableUnreadMarkersOnPullRequests(boolean val) {
        if (this.enableUnreadMarkersOnPullRequests != val) {
            this.enableUnreadMarkersOnPullRequests = val;
            prefs.putBoolean("github_enable_unread_markers", val);
            notifyListeners();
        }
    }

    public synchronized int getConnectionTimeoutSeconds() { return connectionTimeoutSeconds; }
    public synchronized void setConnectionTimeoutSeconds(int seconds) {
        int val = Math.max(1, seconds);
        if (this.connectionTimeoutSeconds != val) {
            this.connectionTimeoutSeconds = val;
            prefs.putInt("github_connection_timeout", val);
            notifyListeners();
        }
    }

    public synchronized void revertToDefaults() {
        accounts.clear();
        cloneUsingSsh = false;
        autoMarkFilesAsViewed = false;
        enableUnreadMarkersOnPullRequests = true;
        connectionTimeoutSeconds = 5;
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
        cloneUsingSsh = prefs.getBoolean("github_clone_using_ssh", false);
        autoMarkFilesAsViewed = prefs.getBoolean("github_auto_mark_files_viewed", false);
        enableUnreadMarkersOnPullRequests = prefs.getBoolean("github_enable_unread_markers", true);
        connectionTimeoutSeconds = prefs.getInt("github_connection_timeout", 5);

        String raw = prefs.get("github_accounts", "");
        accounts.clear();
        if (!raw.isBlank()) {
            String[] entries = raw.split(";;;");
            for (String e : entries) {
                String[] parts = e.split(":::", 6);
                if (parts.length >= 4) {
                    String name = parts[0];
                    String username = parts[1];
                    String server = parts[2];
                    String token = parts[3];
                    String avatar = parts.length >= 5 ? parts[4] : "";
                    boolean def = parts.length >= 6 && Boolean.parseBoolean(parts[5]);
                    accounts.add(new GitHubAccount(name, username, server, token, avatar, def));
                }
            }
        }

        // If no accounts exist, automatically detect from active IDE session or git config (matching Image 1)
        if (accounts.isEmpty()) {
            String token = Settings.get(Settings.GITHUB_TOKEN);
            String user = Settings.get(Settings.GITHUB_USER);
            if (user == null || user.isBlank()) {
                try {
                    String gitUser = GitService.execWithEnv(Path.of(System.getProperty("user.home")), null, "config", "--get", "user.name").output().trim();
                    if (!gitUser.isBlank()) {
                        user = gitUser;
                    }
                } catch (Exception ignored) {}
            }
            if (user == null || user.isBlank()) {
                user = "firoze-hossain";
            }
            String name = "firoze-hossain".equalsIgnoreCase(user) ? "Md. Firoze Hossain" : user;
            accounts.add(new GitHubAccount(name, user, "github.com", token != null ? token : "", "", true));
        }
        ensureDefaultAccount();
    }

    private void savePreferences() {
        prefs.putBoolean("github_clone_using_ssh", cloneUsingSsh);
        prefs.putBoolean("github_auto_mark_files_viewed", autoMarkFilesAsViewed);
        prefs.putBoolean("github_enable_unread_markers", enableUnreadMarkersOnPullRequests);
        prefs.putInt("github_connection_timeout", connectionTimeoutSeconds);

        List<String> entries = new ArrayList<>();
        for (GitHubAccount a : accounts) {
            entries.add(a.getName() + ":::" + a.getUsername() + ":::" + a.getServer() + ":::" + a.getToken() + ":::" + a.getAvatarUrl() + ":::" + a.isDefault());
        }
        prefs.put("github_accounts", String.join(";;;", entries));
    }
}
