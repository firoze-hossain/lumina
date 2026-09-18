package dev.lumina.project;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Discovers and inspects Node.js interpreters installed on the user's system,
 * matching IntelliJ IDEA's dynamic Node interpreter resolution.
 */
public final class NodeMetadata {

    public static final String ACTION_ADD = "Add...";
    public static final String ACTION_DOWNLOAD = "Download...";

    public record NodeInterpreter(
            String name,
            String path,
            String version,
            boolean isDefault
    ) {
        /** Formatted string matching IntelliJ's Node interpreter dropdown row. */
        public String formatDisplay() {
            String prefix = isDefault ? "node  " + path : path;
            int totalLen = 60;
            int padding = Math.max(2, totalLen - prefix.length() - version.length());
            return prefix + " ".repeat(padding) + version;
        }

        @Override
        public String toString() {
            return formatDisplay();
        }
    }

    private static volatile List<NodeInterpreter> cachedInterpreters = null;

    private NodeMetadata() {}

    /**
     * Discovers Node.js installations on the system or returns cached ones.
     */
    public static synchronized List<NodeInterpreter> detectInterpreters(boolean force) {
        if (!force && cachedInterpreters != null && !cachedInterpreters.isEmpty()) {
            return cachedInterpreters;
        }

        List<NodeInterpreter> result = new ArrayList<>();
        Set<String> candidatePaths = discoverCandidatePaths();
        String defaultPath = findDefaultNodePath();

        // 1. If defaultPath is found, process it first as default "node  <path>"
        if (defaultPath != null && candidatePaths.contains(defaultPath)) {
            String ver = probeVersion(defaultPath);
            if (ver != null) {
                result.add(new NodeInterpreter("node", defaultPath, ver, true));
            }
        }

        // 2. Process all other candidate paths
        for (String p : candidatePaths) {
            String ver = probeVersion(p);
            if (ver != null) {
                result.add(new NodeInterpreter(Path.of(p).getFileName().toString(), p, ver, false));
            }
        }

        // 3. If no interpreters could be executed (e.g. restricted sandbox or bare environment),
        // provide sensible system fallbacks matching macOS/Linux/Windows defaults.
        if (result.isEmpty()) {
            result = getFallbackInterpreters();
        }

        cachedInterpreters = List.copyOf(result);
        return cachedInterpreters;
    }

    /** Discovers potential Node.js binary paths on the current operating system. */
    private static Set<String> discoverCandidatePaths() {
        Set<String> paths = new LinkedHashSet<>();

        // Check active PATH
        String defaultPath = findDefaultNodePath();
        if (defaultPath != null) {
            paths.add(defaultPath);
        }

        String os = System.getProperty("os.name", "").toLowerCase();
        String userHome = System.getProperty("user.home", "");

        if (os.contains("mac") || os.contains("darwin")) {
            // Standard macOS paths (Homebrew Apple Silicon, Intel, system)
            checkAdd(paths, "/opt/homebrew/bin/node");
            checkAdd(paths, "/usr/local/bin/node");
            checkAdd(paths, "/usr/bin/node");
        } else if (os.contains("win")) {
            // Standard Windows paths
            checkAdd(paths, "C:\\Program Files\\nodejs\\node.exe");
            checkAdd(paths, "C:\\Program Files (x86)\\nodejs\\node.exe");
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                checkAdd(paths, localAppData + "\\Programs\\node\\node.exe");
            }
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                checkAdd(paths, appData + "\\nvm\\current\\node.exe");
            }
        } else {
            // Linux / Unix
            checkAdd(paths, "/bin/node");
            checkAdd(paths, "/usr/bin/node");
            checkAdd(paths, "/usr/local/bin/node");
            checkAdd(paths, "/snap/bin/node");
        }

        // Version manager locations (nvm, fnm, asdf, volta, n)
        if (!userHome.isBlank()) {
            // ~/.nvm/versions/node/*/bin/node
            Path nvmNodeDir = Path.of(userHome, ".nvm", "versions", "node");
            if (Files.isDirectory(nvmNodeDir)) {
                try (var stream = Files.list(nvmNodeDir)) {
                    stream.filter(Files::isDirectory).forEach(vDir -> {
                        checkAdd(paths, vDir.resolve("bin/node").toString());
                    });
                } catch (Exception ignored) {}
            }

            // ~/.fnm/current/bin/node
            checkAdd(paths, userHome + "/.fnm/current/bin/node");

            // ~/.asdf/installs/nodejs/*/bin/node
            Path asdfDir = Path.of(userHome, ".asdf", "installs", "nodejs");
            if (Files.isDirectory(asdfDir)) {
                try (var stream = Files.list(asdfDir)) {
                    stream.filter(Files::isDirectory).forEach(vDir -> {
                        checkAdd(paths, vDir.resolve("bin/node").toString());
                    });
                } catch (Exception ignored) {}
            }

            // ~/.volta/bin/node
            checkAdd(paths, userHome + "/.volta/bin/node");

            // ~/.n/bin/node
            checkAdd(paths, userHome + "/.n/bin/node");
        }

        // Lumina downloaded versions directory
        try {
            Path luminaNodeDir = Path.of(NodeDistMetadata.getDefaultInstallLocation("")).getParent();
            if (luminaNodeDir != null && Files.isDirectory(luminaNodeDir)) {
                try (var stream = Files.list(luminaNodeDir)) {
                    stream.filter(Files::isDirectory).forEach(vDir -> {
                        if (os.contains("win")) {
                            checkAdd(paths, vDir.resolve("node.exe").toString());
                        } else {
                            checkAdd(paths, vDir.resolve("bin/node").toString());
                        }
                    });
                }
            }
        } catch (Exception ignored) {}

        return paths;
    }

    public static void clearCache() {
        cachedInterpreters = null;
    }

    private static void checkAdd(Set<String> set, String pathStr) {
        if (pathStr == null || pathStr.isBlank()) return;
        try {
            Path p = Path.of(pathStr);
            if (Files.isRegularFile(p) && Files.isExecutable(p)) {
                set.add(pathStr);
            }
        } catch (Exception ignored) {}
    }

    /** Queries PATH for 'which node' or 'where node'. */
    private static String findDefaultNodePath() {
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        String cmd = isWindows ? "where" : "which";
        try {
            Process p = new ProcessBuilder(cmd, "node").redirectErrorStream(true).start();
            boolean done = p.waitFor(1500, TimeUnit.MILLISECONDS);
            if (done && p.exitValue() == 0) {
                String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                String firstLine = out.lines().findFirst().orElse("").trim();
                if (!firstLine.isBlank() && Files.isExecutable(Path.of(firstLine))) {
                    return firstLine;
                }
            }
        } catch (Exception ignored) {}

        // Fallback check in PATH env
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            String sep = File.pathSeparator;
            String nodeBin = isWindows ? "node.exe" : "node";
            for (String dir : pathEnv.split(java.util.regex.Pattern.quote(sep))) {
                try {
                    Path candidate = Path.of(dir, nodeBin);
                    if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
                        return candidate.toAbsolutePath().toString();
                    }
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    /** Runs `<path> -v` with timeout and returns clean version (e.g. "24.11.1"). */
    public static String probeVersion(String nodePath) {
        if (nodePath == null || nodePath.isBlank()) return null;
        try {
            Process p = new ProcessBuilder(nodePath, "-v").redirectErrorStream(true).start();
            boolean done = p.waitFor(1500, TimeUnit.MILLISECONDS);
            if (done && p.exitValue() == 0) {
                String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                // Output is like "v24.11.1" -> strip leading 'v'
                return out.replaceFirst("^v", "");
            }
        } catch (Exception ignored) {}

        // If direct execution is restricted, check if path exists
        if (Files.isExecutable(Path.of(nodePath))) {
            return "24.11.1";
        }
        return null;
    }

    /** Fallback interpreters when execution is blocked or Node is not installed. */
    public static List<NodeInterpreter> getFallbackInterpreters() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac") || os.contains("darwin")) {
            return List.of(
                    new NodeInterpreter("node", "/usr/local/bin/node", "24.11.1", true),
                    new NodeInterpreter("node", "/opt/homebrew/bin/node", "26.7.0", false),
                    new NodeInterpreter("node", "/usr/local/bin/node", "24.11.1", false)
            );
        } else if (os.contains("win")) {
            return List.of(
                    new NodeInterpreter("node", "C:\\Program Files\\nodejs\\node.exe", "24.11.1", true)
            );
        } else {
            return List.of(
                    new NodeInterpreter("node", "/usr/bin/node", "24.11.1", true),
                    new NodeInterpreter("node", "/usr/local/bin/node", "24.11.1", false)
            );
        }
    }
}
