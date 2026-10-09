package dev.lumina.javascript;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Settings model for JavaScript Runtime configuration in Lumina IDE.
 * Dynamically detects installed runtimes (Node.js, Deno, Bun) and package managers (npm, pnpm, yarn, bun).
 */
public class JavaScriptRuntimeSettings {

    public static final String DEFAULT_PREFERRED_RUNTIME = "Node.js Auto-detected";
    public static final String DEFAULT_PACKAGE_MANAGER = "npm";
    public static final String DEFAULT_PACKAGE_MANAGER_PATH = "/usr/bin/npm";
    public static final String DEFAULT_PACKAGE_MANAGER_VERSION = "10.9.9";
    public static final String DEFAULT_NODE_RUNTIME = "node";
    public static final String DEFAULT_NODE_RUNTIME_PATH = "/usr/bin/node";
    public static final String DEFAULT_NODE_RUNTIME_VERSION = "22.23.3";

    private String preferredRuntime = DEFAULT_PREFERRED_RUNTIME;
    private String packageManager = DEFAULT_PACKAGE_MANAGER;
    private String packageManagerPath = DEFAULT_PACKAGE_MANAGER_PATH;
    private String packageManagerVersion = DEFAULT_PACKAGE_MANAGER_VERSION;
    private String nodeRuntime = DEFAULT_NODE_RUNTIME;
    private String nodeRuntimePath = DEFAULT_NODE_RUNTIME_PATH;
    private String nodeRuntimeVersion = DEFAULT_NODE_RUNTIME_VERSION;
    private boolean codingAssistanceForNode = false;

    public JavaScriptRuntimeSettings() {
    }

    public JavaScriptRuntimeSettings(JavaScriptRuntimeSettings other) {
        if (other != null) {
            this.preferredRuntime = other.preferredRuntime;
            this.packageManager = other.packageManager;
            this.packageManagerPath = other.packageManagerPath;
            this.packageManagerVersion = other.packageManagerVersion;
            this.nodeRuntime = other.nodeRuntime;
            this.nodeRuntimePath = other.nodeRuntimePath;
            this.nodeRuntimeVersion = other.nodeRuntimeVersion;
            this.codingAssistanceForNode = other.codingAssistanceForNode;
        }
    }

    public JavaScriptRuntimeSettings copy() {
        return new JavaScriptRuntimeSettings(this);
    }

    public String getPreferredRuntime() {
        return preferredRuntime;
    }

    public void setPreferredRuntime(String preferredRuntime) {
        this.preferredRuntime = preferredRuntime != null ? preferredRuntime : DEFAULT_PREFERRED_RUNTIME;
    }

    public String getPackageManager() {
        return packageManager;
    }

    public void setPackageManager(String packageManager) {
        this.packageManager = packageManager != null ? packageManager : DEFAULT_PACKAGE_MANAGER;
    }

    public String getPackageManagerPath() {
        return packageManagerPath;
    }

    public void setPackageManagerPath(String packageManagerPath) {
        this.packageManagerPath = packageManagerPath != null ? packageManagerPath : DEFAULT_PACKAGE_MANAGER_PATH;
    }

    public String getPackageManagerVersion() {
        return packageManagerVersion;
    }

    public void setPackageManagerVersion(String packageManagerVersion) {
        this.packageManagerVersion = packageManagerVersion != null ? packageManagerVersion : DEFAULT_PACKAGE_MANAGER_VERSION;
    }

    public String getNodeRuntime() {
        return nodeRuntime;
    }

    public void setNodeRuntime(String nodeRuntime) {
        this.nodeRuntime = nodeRuntime != null ? nodeRuntime : DEFAULT_NODE_RUNTIME;
    }

    public String getNodeRuntimePath() {
        return nodeRuntimePath;
    }

    public void setNodeRuntimePath(String nodeRuntimePath) {
        this.nodeRuntimePath = nodeRuntimePath != null ? nodeRuntimePath : DEFAULT_NODE_RUNTIME_PATH;
    }

    public String getNodeRuntimeVersion() {
        return nodeRuntimeVersion;
    }

    public void setNodeRuntimeVersion(String nodeRuntimeVersion) {
        this.nodeRuntimeVersion = nodeRuntimeVersion != null ? nodeRuntimeVersion : DEFAULT_NODE_RUNTIME_VERSION;
    }

    public boolean isCodingAssistanceForNode() {
        return codingAssistanceForNode;
    }

    public void setCodingAssistanceForNode(boolean codingAssistanceForNode) {
        this.codingAssistanceForNode = codingAssistanceForNode;
    }

    /**
     * Runtime item record for display in combo boxes.
     */
    public record RuntimeItem(String name, String path, String version) {
        @Override
        public String toString() {
            if (path == null || path.isBlank()) {
                return name;
            }
            return name + "  " + path + (version != null && !version.isBlank() ? "  " + version : "");
        }
    }

    /**
     * Dynamically discovers installed Node runtimes from common locations and system PATH.
     */
    public static List<RuntimeItem> detectSystemNodeRuntimes() {
        List<RuntimeItem> list = new ArrayList<>();
        String[] candidatePaths = {
                "/usr/bin/node",
                "/usr/local/bin/node",
                System.getProperty("user.home") + "/.nvm/current/bin/node",
                System.getProperty("user.home") + "/.asdf/shims/node",
                System.getProperty("user.home") + "/.n/bin/node"
        };

        for (String p : candidatePaths) {
            File f = new File(p);
            if (f.exists() && f.canExecute()) {
                String ver = queryExecutableVersion(p);
                list.add(new RuntimeItem("node", p, ver != null ? ver : DEFAULT_NODE_RUNTIME_VERSION));
            }
        }

        // Fallback default if none found or to guarantee default availability
        if (list.isEmpty()) {
            list.add(new RuntimeItem(DEFAULT_NODE_RUNTIME, DEFAULT_NODE_RUNTIME_PATH, DEFAULT_NODE_RUNTIME_VERSION));
        }
        return list;
    }

    /**
     * Dynamically discovers installed package managers from common locations and system PATH.
     */
    public static List<RuntimeItem> detectSystemPackageManagers() {
        List<RuntimeItem> list = new ArrayList<>();
        String[][] candidates = {
                {"npm", "/usr/bin/npm"},
                {"npm", "/usr/local/bin/npm"},
                {"pnpm", "/usr/bin/pnpm"},
                {"pnpm", "/usr/local/bin/pnpm"},
                {"yarn", "/usr/bin/yarn"},
                {"yarn", "/usr/local/bin/yarn"},
                {"bun", "/usr/bin/bun"},
                {"bun", "/usr/local/bin/bun"}
        };

        for (String[] c : candidates) {
            File f = new File(c[1]);
            if (f.exists() && f.canExecute()) {
                String ver = queryExecutableVersion(c[1]);
                list.add(new RuntimeItem(c[0], c[1], ver != null ? ver : DEFAULT_PACKAGE_MANAGER_VERSION));
            }
        }

        if (list.isEmpty()) {
            list.add(new RuntimeItem(DEFAULT_PACKAGE_MANAGER, DEFAULT_PACKAGE_MANAGER_PATH, DEFAULT_PACKAGE_MANAGER_VERSION));
        }
        return list;
    }

    private static String queryExecutableVersion(String executablePath) {
        try {
            Process process = new ProcessBuilder(executablePath, "--version")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(1, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null) {
                        return line.trim().replaceFirst("^v", "");
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JavaScriptRuntimeSettings that = (JavaScriptRuntimeSettings) o;
        return codingAssistanceForNode == that.codingAssistanceForNode &&
                Objects.equals(preferredRuntime, that.preferredRuntime) &&
                Objects.equals(packageManager, that.packageManager) &&
                Objects.equals(packageManagerPath, that.packageManagerPath) &&
                Objects.equals(packageManagerVersion, that.packageManagerVersion) &&
                Objects.equals(nodeRuntime, that.nodeRuntime) &&
                Objects.equals(nodeRuntimePath, that.nodeRuntimePath) &&
                Objects.equals(nodeRuntimeVersion, that.nodeRuntimeVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(preferredRuntime, packageManager, packageManagerPath, packageManagerVersion,
                nodeRuntime, nodeRuntimePath, nodeRuntimeVersion, codingAssistanceForNode);
    }
}
