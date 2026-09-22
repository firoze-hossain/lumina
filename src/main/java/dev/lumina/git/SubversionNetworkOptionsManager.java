package dev.lumina.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Manages Subversion network options corresponding to the 'servers' runtime configuration file.
 * Handles both User file (~/.subversion/servers) and System file (/etc/subversion/servers).
 */
public class SubversionNetworkOptionsManager {

    public enum FileSource {
        USER_FILE("User file"),
        SYSTEM_FILE("System file");

        private final String label;
        FileSource(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public static class NetworkGroup {
        private String name;
        private String urlPatterns = "";
        private String exceptions = "";
        private String server = "";
        private String user = "";
        private String port = "";
        private String password = "";
        private String timeout = "";
        private String caCertFiles = "";
        private String clientCertFile = "";
        private String clientCertPassphrase = "";
        private boolean trustDefaultCas = true;

        public NetworkGroup(String name) {
            this.name = name;
        }

        public NetworkGroup copy(String newName) {
            NetworkGroup clone = new NetworkGroup(newName);
            clone.urlPatterns = this.urlPatterns;
            clone.exceptions = this.exceptions;
            clone.server = this.server;
            clone.user = this.user;
            clone.port = this.port;
            clone.password = this.password;
            clone.timeout = this.timeout;
            clone.caCertFiles = this.caCertFiles;
            clone.clientCertFile = this.clientCertFile;
            clone.clientCertPassphrase = this.clientCertPassphrase;
            clone.trustDefaultCas = this.trustDefaultCas;
            return clone;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getUrlPatterns() { return urlPatterns; }
        public void setUrlPatterns(String urlPatterns) { this.urlPatterns = urlPatterns != null ? urlPatterns : ""; }

        public String getExceptions() { return exceptions; }
        public void setExceptions(String exceptions) { this.exceptions = exceptions != null ? exceptions : ""; }

        public String getServer() { return server; }
        public void setServer(String server) { this.server = server != null ? server : ""; }

        public String getUser() { return user; }
        public void setUser(String user) { this.user = user != null ? user : ""; }

        public String getPort() { return port; }
        public void setPort(String port) { this.port = port != null ? port : ""; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password != null ? password : ""; }

        public String getTimeout() { return timeout; }
        public void setTimeout(String timeout) { this.timeout = timeout != null ? timeout : ""; }

        public String getCaCertFiles() { return caCertFiles; }
        public void setCaCertFiles(String caCertFiles) { this.caCertFiles = caCertFiles != null ? caCertFiles : ""; }

        public String getClientCertFile() { return clientCertFile; }
        public void setClientCertFile(String clientCertFile) { this.clientCertFile = clientCertFile != null ? clientCertFile : ""; }

        public String getClientCertPassphrase() { return clientCertPassphrase; }
        public void setClientCertPassphrase(String clientCertPassphrase) { this.clientCertPassphrase = clientCertPassphrase != null ? clientCertPassphrase : ""; }

        public boolean isTrustDefaultCas() { return trustDefaultCas; }
        public void setTrustDefaultCas(boolean trustDefaultCas) { this.trustDefaultCas = trustDefaultCas; }
    }

    private final Map<String, NetworkGroup> groups = new LinkedHashMap<>();

    public SubversionNetworkOptionsManager() {
        // Ensure default 'global' group exists
        NetworkGroup global = new NetworkGroup("global");
        groups.put("global", global);
    }

    public static Path getUserServersPath() {
        SubversionSettingsManager sm = SubversionSettingsManager.getInstance();
        String dir = sm.isUseCustomConfigDirectory() && !sm.getCustomConfigDirectoryPath().isBlank()
                ? sm.getCustomConfigDirectoryPath()
                : Path.of(System.getProperty("user.home"), ".subversion").toString();
        return Path.of(dir, "servers");
    }

    public static Path getSystemServersPath() {
        return Path.of("/etc/subversion/servers");
    }

    public Map<String, NetworkGroup> getGroups() {
        return groups;
    }

    public NetworkGroup getGroup(String name) {
        return groups.get(name);
    }

    public NetworkGroup getOrCreateGroup(String name) {
        return groups.computeIfAbsent(name, NetworkGroup::new);
    }

    public boolean addGroup(String name) {
        if (name == null || name.isBlank() || groups.containsKey(name)) {
            return false;
        }
        groups.put(name, new NetworkGroup(name));
        return true;
    }

    public boolean removeGroup(String name) {
        if ("global".equalsIgnoreCase(name)) {
            return false; // Cannot remove 'global'
        }
        return groups.remove(name) != null;
    }

    public void loadFromFile(Path filePath) {
        groups.clear();
        NetworkGroup global = new NetworkGroup("global");
        groups.put("global", global);

        if (!Files.exists(filePath)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(filePath);
            String currentSection = null;
            Map<String, String> groupUrlPatterns = new HashMap<>();

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    currentSection = trimmed.substring(1, trimmed.length() - 1).trim();
                    if (!"groups".equalsIgnoreCase(currentSection)) {
                        getOrCreateGroup(currentSection);
                    }
                    continue;
                }

                int eq = trimmed.indexOf('=');
                if (eq > 0 && currentSection != null) {
                    String key = trimmed.substring(0, eq).trim();
                    String val = trimmed.substring(eq + 1).trim();

                    if ("groups".equalsIgnoreCase(currentSection)) {
                        groupUrlPatterns.put(key, val);
                    } else {
                        NetworkGroup grp = getOrCreateGroup(currentSection);
                        applyOption(grp, key, val);
                    }
                }
            }

            for (Map.Entry<String, String> e : groupUrlPatterns.entrySet()) {
                NetworkGroup grp = getOrCreateGroup(e.getKey());
                grp.setUrlPatterns(e.getValue());
            }

        } catch (IOException ignored) {}
    }

    private void applyOption(NetworkGroup grp, String key, String val) {
        switch (key.toLowerCase()) {
            case "http-proxy-host" -> grp.setServer(val);
            case "http-proxy-port" -> grp.setPort(val);
            case "http-proxy-username" -> grp.setUser(val);
            case "http-proxy-password" -> grp.setPassword(val);
            case "http-proxy-exceptions" -> grp.setExceptions(val);
            case "http-timeout" -> grp.setTimeout(val);
            case "ssl-authority-files" -> grp.setCaCertFiles(val);
            case "ssl-client-cert-file" -> grp.setClientCertFile(val);
            case "ssl-client-cert-password" -> grp.setClientCertPassphrase(val);
            case "ssl-trust-default-ca" -> grp.setTrustDefaultCas(!"no".equalsIgnoreCase(val));
        }
    }

    public void saveToFile(Path filePath) throws IOException {
        Path parent = filePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# Subversion runtime configuration file for network layers\n\n");

        // [groups] section
        sb.append("[groups]\n");
        for (NetworkGroup grp : groups.values()) {
            if (!"global".equalsIgnoreCase(grp.getName()) && !grp.getUrlPatterns().isBlank()) {
                sb.append(grp.getName()).append(" = ").append(grp.getUrlPatterns()).append("\n");
            }
        }
        sb.append("\n");

        // Each group section
        for (NetworkGroup grp : groups.values()) {
            sb.append("[").append(grp.getName()).append("]\n");
            if (!grp.getServer().isBlank()) sb.append("http-proxy-host = ").append(grp.getServer()).append("\n");
            if (!grp.getPort().isBlank()) sb.append("http-proxy-port = ").append(grp.getPort()).append("\n");
            if (!grp.getUser().isBlank()) sb.append("http-proxy-username = ").append(grp.getUser()).append("\n");
            if (!grp.getPassword().isBlank()) sb.append("http-proxy-password = ").append(grp.getPassword()).append("\n");
            if (!grp.getExceptions().isBlank()) sb.append("http-proxy-exceptions = ").append(grp.getExceptions()).append("\n");
            if (!grp.getTimeout().isBlank()) sb.append("http-timeout = ").append(grp.getTimeout()).append("\n");
            if (!grp.getCaCertFiles().isBlank()) sb.append("ssl-authority-files = ").append(grp.getCaCertFiles()).append("\n");
            if (!grp.getClientCertFile().isBlank()) sb.append("ssl-client-cert-file = ").append(grp.getClientCertFile()).append("\n");
            if (!grp.getClientCertPassphrase().isBlank()) sb.append("ssl-client-cert-password = ").append(grp.getClientCertPassphrase()).append("\n");
            sb.append("ssl-trust-default-ca = ").append(grp.isTrustDefaultCas() ? "yes" : "no").append("\n");
            sb.append("\n");
        }

        Files.writeString(filePath, sb.toString());
    }
}
