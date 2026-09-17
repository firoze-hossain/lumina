package dev.lumina.project;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service providing dynamic Python interpreter discovery, uv discovery,
 * and supported version metadata matching IntelliJ IDEA.
 */
public final class PythonMetadata {

    public record PythonInstallation(
            String label,        // e.g. "Python 3.12"
            String executable,   // e.g. "/usr/bin/python3"
            String type,         // e.g. "system", "pyenv", "conda"
            String displayLabel  // e.g. "Python 3.12 (/usr/bin/python3) system"
    ) {
        @Override
        public String toString() {
            return displayLabel;
        }
    }

    public static final List<String> UV_PYTHON_VERSIONS = List.of(
            "Default", "3.14", "3.13", "3.12", "3.11", "3.10", "3.9", "3.8"
    );

    private static volatile List<PythonInstallation> cachedInstallations = null;
    private static final Object LOCK = new Object();

    private PythonMetadata() {
    }

    /**
     * Synchronously discovers Python installations on the system with caching.
     */
    public static List<PythonInstallation> fetchPythonInstallations(boolean forceRefresh) {
        if (!forceRefresh && cachedInstallations != null && !cachedInstallations.isEmpty()) {
            return cachedInstallations;
        }
        synchronized (LOCK) {
            if (!forceRefresh && cachedInstallations != null && !cachedInstallations.isEmpty()) {
                return cachedInstallations;
            }
            List<PythonInstallation> discovered = discoverSystemPython();
            if (discovered.isEmpty()) {
                discovered = List.of(new PythonInstallation(
                        "Python 3.12",
                        "/usr/bin/python3",
                        "system",
                        "Python 3.12 (/usr/bin/python3) system"
                ));
            }
            cachedInstallations = Collections.unmodifiableList(discovered);
            return cachedInstallations;
        }
    }

    /**
     * Asynchronously discovers Python installations and triggers callback on completion.
     */
    public static CompletableFuture<List<PythonInstallation>> fetchPythonInstallationsAsync(
            Consumer<List<PythonInstallation>> callback) {
        return CompletableFuture.supplyAsync(() -> fetchPythonInstallations(true))
                .thenApply(installations -> {
                    if (callback != null) {
                        javafx.application.Platform.runLater(() -> callback.accept(installations));
                    }
                    return installations;
                });
    }

    /**
     * Scans common system directories and PATH for python executables.
     */
    public static List<PythonInstallation> discoverSystemPython() {
        Set<String> candidatePaths = new LinkedHashSet<>();

        // 1. Common Linux / Unix paths
        List<String> knownPaths = List.of(
                "/usr/bin/python3",
                "/usr/bin/python3.13",
                "/usr/bin/python3.12",
                "/usr/bin/python3.11",
                "/usr/bin/python3.10",
                "/usr/bin/python3.9",
                "/usr/bin/python3.8",
                "/usr/bin/python",
                "/usr/local/bin/python3",
                "/usr/local/bin/python"
        );
        for (String p : knownPaths) {
            if (new File(p).exists() && new File(p).canExecute()) {
                candidatePaths.add(p);
            }
        }

        // 2. User home pyenv / local paths
        String userHome = System.getProperty("user.home", "");
        if (!userHome.isBlank()) {
            List<String> userPaths = List.of(
                    userHome + "/.pyenv/shims/python3",
                    userHome + "/.pyenv/shims/python",
                    userHome + "/.local/bin/python3",
                    userHome + "/.local/bin/python"
            );
            for (String p : userPaths) {
                if (new File(p).exists() && new File(p).canExecute()) {
                    candidatePaths.add(p);
                }
            }
        }

        // 3. Scan PATH
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String dir : pathEnv.split(Pattern.quote(File.pathSeparator))) {
                if (dir.isBlank()) continue;
                File d = new File(dir);
                if (d.isDirectory()) {
                    File[] pyFiles = d.listFiles((f, name) -> name.matches("python(3(\\.\\d+)?)?(\\.exe)?"));
                    if (pyFiles != null) {
                        for (File py : pyFiles) {
                            if (py.canExecute() && !py.isDirectory()) {
                                candidatePaths.add(py.getAbsolutePath());
                            }
                        }
                    }
                }
            }
        }

        List<PythonInstallation> results = new ArrayList<>();
        Set<String> seenExecutables = new HashSet<>();

        for (String exePath : candidatePaths) {
            try {
                File f = new File(exePath);
                if (!f.exists() || !f.canExecute()) continue;
                if (seenExecutables.contains(exePath)) continue;
                seenExecutables.add(exePath);

                String version = probePythonVersion(exePath);
                if (version == null || version.isBlank()) {
                    version = extractVersionFromFileName(f.getName());
                }

                String label = version.startsWith("Python ") ? version : "Python " + version;
                String type = classifyPythonType(exePath);
                String display = label + " (" + exePath + ") " + type;

                results.add(new PythonInstallation(label, exePath, type, display));
            } catch (Exception ignored) {
            }
        }

        // Sort by version descending, keeping /usr/bin/python3 first if same version
        results.sort((a, b) -> {
            int cmp = comparePythonVersions(extractNumericVersion(b.label()), extractNumericVersion(a.label()));
            if (cmp != 0) return cmp;
            if (a.executable().equals("/usr/bin/python3")) return -1;
            if (b.executable().equals("/usr/bin/python3")) return 1;
            return a.executable().compareTo(b.executable());
        });

        return results;
    }

    private static String probePythonVersion(String exePath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(exePath, "--version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && line.startsWith("Python ")) {
                    Matcher m = Pattern.compile("Python\\s+(\\d+\\.\\d+)").matcher(line);
                    if (m.find()) {
                        return "Python " + m.group(1);
                    }
                    return line.trim();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String extractVersionFromFileName(String filename) {
        Matcher m = Pattern.compile("python3?\\.(\\d+)").matcher(filename);
        if (m.find()) {
            return "3." + m.group(1);
        }
        return "3.12";
    }

    private static String extractNumericVersion(String label) {
        Matcher m = Pattern.compile("(\\d+\\.\\d+)").matcher(label);
        return m.find() ? m.group(1) : "3.12";
    }

    private static String classifyPythonType(String path) {
        String lower = path.toLowerCase();
        if (lower.contains("pyenv")) return "pyenv";
        if (lower.contains("conda") || lower.contains("anaconda") || lower.contains("miniconda")) return "conda";
        if (path.startsWith("/usr/") || path.startsWith("/System/")) return "system";
        return "system";
    }

    private static int comparePythonVersions(String v1, String v2) {
        String[] p1 = v1.split("\\.");
        String[] p2 = v2.split("\\.");
        int len = Math.max(p1.length, p2.length);
        for (int i = 0; i < len; i++) {
            int n1 = i < p1.length ? parseSafeInt(p1[i]) : 0;
            int n2 = i < p2.length ? parseSafeInt(p2[i]) : 0;
            if (n1 != n2) return Integer.compare(n1, n2);
        }
        return 0;
    }

    private static int parseSafeInt(String s) {
        try {
            return Integer.parseInt(s.replaceAll("\\D.*", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Detects path to uv executable on the system matching IntelliJ IDEA.
     */
    public static String detectUvPath() {
        String userHome = System.getProperty("user.home", "");
        List<String> candidates = new ArrayList<>();

        if (!userHome.isBlank()) {
            candidates.add(userHome + "/.local/bin/uv");
            candidates.add(userHome + "/.cargo/bin/uv");
        }
        candidates.add("/usr/local/bin/uv");
        candidates.add("/usr/bin/uv");
        candidates.add("/opt/homebrew/bin/uv");

        // Windows candidates
        String userProfile = System.getenv("USERPROFILE");
        if (userProfile != null && !userProfile.isBlank()) {
            candidates.add(userProfile + "\\.local\\bin\\uv.exe");
            candidates.add(userProfile + "\\.cargo\\bin\\uv.exe");
            candidates.add(userProfile + "\\AppData\\Local\\Programs\\uv\\uv.exe");
        }

        for (String c : candidates) {
            if (new File(c).exists() && new File(c).canExecute()) {
                return c;
            }
        }

        // Check PATH
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String dir : pathEnv.split(Pattern.quote(File.pathSeparator))) {
                File exe = new File(dir, "uv");
                if (exe.exists() && exe.canExecute()) return exe.getAbsolutePath();
                File exeWin = new File(dir, "uv.exe");
                if (exeWin.exists() && exeWin.canExecute()) return exeWin.getAbsolutePath();
            }
        }

        // Default path shown in IntelliJ screenshot 3
        return !userHome.isBlank() ? userHome + "/.local/bin/uv" : "/usr/local/bin/uv";
    }

    /**
     * Validates whether the given path points to a valid, executable uv binary.
     */
    public static boolean isValidUv(String path) {
        if (path == null || path.isBlank()) return false;
        try {
            File f = new File(path.trim());
            return f.exists() && f.canExecute() && !f.isDirectory();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns the list of supported Python versions for uv matching IntelliJ IDEA.
     */
    public static List<String> fetchUvPythonVersions() {
        return UV_PYTHON_VERSIONS;
    }
}
