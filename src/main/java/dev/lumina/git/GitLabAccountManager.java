package dev.lumina.git;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * Manages GitLab accounts and settings matching IntelliJ IDEA's "Version Control > GitLab".
 * Backed by Java Preferences for dynamic persistence.
 */
public class GitLabAccountManager {

    public static final class GitLabAccount {
        private String name;
        private String username;
        private String server;
        private String token;
        private boolean isDefault;

        public GitLabAccount(String name, String username, String server, String token, boolean isDefault) {
            this.name = name != null ? name.trim() : "";
            this.username = username != null ? username.trim() : "";
            this.server = server != null && !server.isBlank() ? server.trim() : "https://gitlab.com";
            this.token = token != null ? token.trim() : "";
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

        public boolean isDefault() { return isDefault; }
        public void setDefault(boolean isDefault) { this.isDefault = isDefault; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GitLabAccount that)) return false;
            return Objects.equals(username, that.username) && Objects.equals(server, that.server);
        }

        @Override
        public int hashCode() {
            return Objects.hash(username, server);
        }
    }

    private static final GitLabAccountManager INSTANCE = new GitLabAccountManager();

    private final Preferences prefs = Preferences.userNodeForPackage(GitLabAccountManager.class);
    private final List<Runnable> listeners = new ArrayList<>();
    private final List<GitLabAccount> accounts = new ArrayList<>();

    private boolean autoMarkFilesAsViewed = false;
    private boolean cloneUsingSsh = false;

    private GitLabAccountManager() {
        loadPreferences();
    }

    public static GitLabAccountManager getInstance() {
        return INSTANCE;
    }

    public synchronized List<GitLabAccount> getAccounts() {
        return Collections.unmodifiableList(new ArrayList<>(accounts));
    }

    public synchronized void setAccounts(List<GitLabAccount> newAccounts) {
        accounts.clear();
        if (newAccounts != null) {
            accounts.addAll(newAccounts);
        }
        ensureDefaultAccount();
        savePreferences();
        notifyListeners();
    }

    public synchronized void addAccount(GitLabAccount account) {
        if (account == null || account.getUsername().isBlank()) return;
        accounts.remove(account);
        accounts.add(account);
        ensureDefaultAccount();
        savePreferences();
        notifyListeners();
    }

    public synchronized void removeAccount(GitLabAccount account) {
        if (accounts.remove(account)) {
            ensureDefaultAccount();
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized void setDefaultAccount(GitLabAccount account) {
        for (GitLabAccount a : accounts) {
            a.setDefault(a.equals(account));
        }
        savePreferences();
        notifyListeners();
    }

    public synchronized GitLabAccount getDefaultAccount() {
        for (GitLabAccount a : accounts) {
            if (a.isDefault()) return a;
        }
        return accounts.isEmpty() ? null : accounts.get(0);
    }

    private void ensureDefaultAccount() {
        if (accounts.isEmpty()) return;
        boolean hasDefault = false;
        for (GitLabAccount a : accounts) {
            if (a.isDefault()) {
                hasDefault = true;
                break;
            }
        }
        if (!hasDefault) {
            accounts.get(0).setDefault(true);
        }
    }

    public synchronized boolean isAutoMarkFilesAsViewed() { return autoMarkFilesAsViewed; }
    public synchronized void setAutoMarkFilesAsViewed(boolean val) {
        if (this.autoMarkFilesAsViewed != val) {
            this.autoMarkFilesAsViewed = val;
            prefs.putBoolean("gitlab_auto_mark_files_viewed", val);
            notifyListeners();
        }
    }

    public synchronized boolean isCloneUsingSsh() { return cloneUsingSsh; }
    public synchronized void setCloneUsingSsh(boolean val) {
        if (this.cloneUsingSsh != val) {
            this.cloneUsingSsh = val;
            prefs.putBoolean("gitlab_clone_using_ssh", val);
            notifyListeners();
        }
    }

    public synchronized void revertToDefaults() {
        accounts.clear();
        autoMarkFilesAsViewed = false;
        cloneUsingSsh = false;
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
        autoMarkFilesAsViewed = prefs.getBoolean("gitlab_auto_mark_files_viewed", false);
        cloneUsingSsh = prefs.getBoolean("gitlab_clone_using_ssh", false);

        String raw = prefs.get("gitlab_accounts", "");
        accounts.clear();
        if (!raw.isBlank()) {
            String[] entries = raw.split(";;;");
            for (String e : entries) {
                String[] parts = e.split(":::", 5);
                if (parts.length >= 4) {
                    String name = parts[0];
                    String username = parts[1];
                    String server = parts[2];
                    String token = parts[3];
                    boolean def = parts.length >= 5 && Boolean.parseBoolean(parts[4]);
                    accounts.add(new GitLabAccount(name, username, server, token, def));
                }
            }
        }
        ensureDefaultAccount();
    }

    private void savePreferences() {
        prefs.putBoolean("gitlab_auto_mark_files_viewed", autoMarkFilesAsViewed);
        prefs.putBoolean("gitlab_clone_using_ssh", cloneUsingSsh);

        List<String> entries = new ArrayList<>();
        for (GitLabAccount a : accounts) {
            entries.add(a.getName() + ":::" + a.getUsername() + ":::" + a.getServer() + ":::" + a.getToken() + ":::" + a.isDefault());
        }
        prefs.put("gitlab_accounts", String.join(";;;", entries));
    }
}
