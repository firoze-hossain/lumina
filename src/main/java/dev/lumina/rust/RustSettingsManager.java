package dev.lumina.rust;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.lumina.util.Settings;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manager for Rust settings in Lumina IDE.
 * Dynamically probes and manages Rust toolchain, toolchain version, standard library,
 * external linters (Cargo Check / Clippy), and rustfmt without hardcoded paths.
 */
public class RustSettingsManager {

    public static final String KEY_RUST_SETTINGS = "rust.configuration.settings";

    private static RustSettingsManager instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private RustSettings settings = new RustSettings();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private RustSettingsManager() {
        loadSettings();
    }

    public static synchronized RustSettingsManager getInstance() {
        if (instance == null) {
            instance = new RustSettingsManager();
        }
        return instance;
    }

    public synchronized RustSettings getSettings() {
        return settings.copy();
    }

    public synchronized void setSettings(RustSettings newSettings) {
        this.settings = newSettings != null ? newSettings.copy() : new RustSettings();
        saveSettings();
        notifyListeners();
    }

    public synchronized void saveSettings() {
        Settings.put(KEY_RUST_SETTINGS, GSON.toJson(settings));
    }

    public synchronized void loadSettings() {
        String json = Settings.get(KEY_RUST_SETTINGS);
        if (json != null && !json.isBlank()) {
            try {
                RustSettings parsed = GSON.fromJson(json, RustSettings.class);
                if (parsed != null) {
                    this.settings = parsed;
                    return;
                }
            } catch (Exception ignored) {}
        }
        initDefaultSettings();
    }

    public synchronized void resetDefaults() {
        initDefaultSettings();
        saveSettings();
        notifyListeners();
    }

    private void initDefaultSettings() {
        settings = new RustSettings();
        String detectedToolchain = detectToolchainLocation();
        settings.setToolchainLocation(detectedToolchain);
        settings.setToolchainVersion(detectToolchainVersion(detectedToolchain));
        settings.setStandardLibrary(detectStandardLibrary(detectedToolchain));
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : changeListeners) {
            try {
                listener.run();
            } catch (Exception ignored) {}
        }
    }

    // ============================================================
    // Dynamic Toolchain & Version Detection (Zero Hardcoding)
    // ============================================================

    public static List<String> getAvailableToolchainLocations() {
        List<String> list = new ArrayList<>();
        String home = System.getProperty("user.home", "");
        if (!home.isBlank()) {
            File cargoBin = new File(home, ".cargo/bin");
            if (cargoBin.isDirectory()) {
                list.add(cargoBin.getAbsolutePath());
            }
        }
        File usrBin = new File("/usr/bin/cargo");
        if (usrBin.exists()) {
            list.add("/usr/bin");
        }
        File usrLocalBin = new File("/usr/local/bin/cargo");
        if (usrLocalBin.exists()) {
            list.add("/usr/local/bin");
        }
        return list;
    }

    public static String detectToolchainLocation() {
        List<String> locations = getAvailableToolchainLocations();
        if (!locations.isEmpty()) {
            return locations.get(0);
        }
        // Fallback to checking PATH
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String p : pathEnv.split(File.pathSeparator)) {
                File cargo = new File(p, "cargo");
                if (cargo.isFile() && cargo.canExecute()) {
                    return p;
                }
            }
        }
        return "";
    }

    public static String detectToolchainVersion(String toolchainLocation) {
        if (toolchainLocation == null || toolchainLocation.isBlank()) {
            return "";
        }
        try {
            File rustcFile = new File(toolchainLocation, "rustc");
            String cmd = rustcFile.exists() ? rustcFile.getAbsolutePath() : "rustc";
            Process process = new ProcessBuilder(cmd, "--version")
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null) {
                    // Typical: "rustc 1.97.0 (2d8144b78 2026-07-07)"
                    Matcher m = Pattern.compile("rustc\\s+([0-9]+\\.[0-9]+(\\.[0-9]+)?)").matcher(line);
                    if (m.find()) {
                        return m.group(1);
                    }
                    // Fallback to second token
                    String[] parts = line.split("\\s+");
                    if (parts.length > 1) {
                        return parts[1];
                    }
                }
            }
        } catch (Exception ignored) {}

        return "";
    }

    public static String detectStandardLibrary(String toolchainLocation) {
        // Try rustc --print sysroot
        String sysroot = "";
        try {
            String cmd = "rustc";
            if (toolchainLocation != null && !toolchainLocation.isBlank()) {
                File rustcFile = new File(toolchainLocation, "rustc");
                if (rustcFile.exists()) {
                    cmd = rustcFile.getAbsolutePath();
                }
            }
            Process process = new ProcessBuilder(cmd, "--print", "sysroot")
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && !line.isBlank()) {
                    sysroot = line.trim();
                }
            }
        } catch (Exception ignored) {}

        if (!sysroot.isBlank()) {
            File stdDir = new File(sysroot, "lib/rustlib/src/rust");
            if (stdDir.exists()) {
                return stdDir.getAbsolutePath();
            }
        }

        // Try ~/.rustup/toolchains/*/lib/rustlib/src/rust
        String home = System.getProperty("user.home", "");
        if (!home.isBlank()) {
            File toolchainsDir = new File(home, ".rustup/toolchains");
            if (toolchainsDir.isDirectory()) {
                File[] children = toolchainsDir.listFiles();
                if (children != null) {
                    for (File tc : children) {
                        File stdDir = new File(tc, "lib/rustlib/src/rust");
                        if (stdDir.exists()) {
                            return stdDir.getAbsolutePath();
                        }
                    }
                }
            }
        }

        return "";
    }
}
