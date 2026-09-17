package dev.lumina.project;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service providing dynamic Rust toolchain discovery, version detection,
 * standard library path resolution, and project templates matching IntelliJ IDEA.
 */
public final class RustMetadata {

    public record RustProjectTemplate(
            String name,
            String url,
            String value,
            boolean builtIn
    ) {
        @Override
        public String toString() {
            if (url == null || url.isBlank()) {
                return name;
            }
            return name + " " + url;
        }
    }

    private static final Pattern VERSION_PATTERN = Pattern.compile("rustc\\s+(\\d+\\.\\d+(\\.\\d+)?)");

    private RustMetadata() {
    }

    /**
     * Discovers installed Rust toolchain directories dynamically.
     */
    public static List<String> discoverToolchains() {
        Set<String> locations = new LinkedHashSet<>();

        // 1. Cargo home bin: ~/.cargo/bin or $CARGO_HOME/bin
        String cargoHome = System.getenv("CARGO_HOME");
        if (cargoHome != null && !cargoHome.isBlank()) {
            File bin = new File(cargoHome, "bin");
            if (isToolchainDir(bin)) {
                locations.add(bin.getAbsolutePath());
            }
        }
        String userHome = System.getProperty("user.home", "");
        if (!userHome.isBlank()) {
            File defaultCargoBin = new File(userHome, ".cargo/bin");
            if (isToolchainDir(defaultCargoBin)) {
                locations.add(defaultCargoBin.getAbsolutePath());
            }

            // 2. Rustup toolchains: ~/.rustup/toolchains/*/bin
            File toolchainsDir = new File(userHome, ".rustup/toolchains");
            if (toolchainsDir.isDirectory()) {
                File[] subs = toolchainsDir.listFiles(File::isDirectory);
                if (subs != null) {
                    for (File sub : subs) {
                        File bin = new File(sub, "bin");
                        if (isToolchainDir(bin)) {
                            locations.add(bin.getAbsolutePath());
                        }
                    }
                }
            }
        }

        // 3. Scan PATH
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String part : pathEnv.split(Pattern.quote(File.pathSeparator))) {
                if (part.isBlank()) continue;
                File dir = new File(part);
                if (isToolchainDir(dir)) {
                    locations.add(dir.getAbsolutePath());
                }
            }
        }

        // 4. Standard Linux / macOS / Homebrew locations
        List<String> systemDirs = List.of(
                "/usr/bin",
                "/usr/local/bin",
                "/opt/homebrew/bin",
                "/opt/local/bin"
        );
        for (String p : systemDirs) {
            File dir = new File(p);
            if (isToolchainDir(dir)) {
                locations.add(dir.getAbsolutePath());
            }
        }

        // 5. Windows common locations
        String sysDrive = System.getenv("SystemDrive");
        if (sysDrive != null) {
            File winCargo = new File(sysDrive + "\\Users\\" + System.getProperty("user.name") + "\\.cargo\\bin");
            if (isToolchainDir(winCargo)) {
                locations.add(winCargo.getAbsolutePath());
            }
        }

        return new ArrayList<>(locations);
    }

    private static boolean isToolchainDir(File dir) {
        if (!dir.isDirectory()) return false;
        boolean isWin = System.getProperty("os.name", "").toLowerCase().contains("win");
        String rustcName = isWin ? "rustc.exe" : "rustc";
        String cargoName = isWin ? "cargo.exe" : "cargo";
        return (new File(dir, rustcName).exists() || new File(dir, cargoName).exists());
    }

    /**
     * Resolves the default toolchain directory (e.g. ~/.cargo/bin or system PATH).
     */
    public static String defaultToolchainPath() {
        List<String> toolchains = discoverToolchains();
        if (!toolchains.isEmpty()) {
            return toolchains.getFirst();
        }
        String userHome = System.getProperty("user.home", "");
        return userHome.isBlank() ? "/usr/bin" : userHome + File.separator + ".cargo" + File.separator + "bin";
    }

    /**
     * Detects the Rust version (e.g. "1.97.0") from the given toolchain directory or system.
     */
    public static String detectRustVersion(String toolchainPath) {
        String exe = resolveExecutable(toolchainPath, "rustc");
        try {
            Process process = new ProcessBuilder(exe, "--version").redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                if (line != null) {
                    Matcher m = VERSION_PATTERN.matcher(line);
                    if (m.find()) {
                        return m.group(1);
                    }
                    if (line.startsWith("rustc ")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 2) return parts[1];
                    }
                }
            }
            if (process.waitFor() == 0) {
                return "1.80.0";
            }
        } catch (Exception ignored) {
        }
        return "Not detected";
    }

    /**
     * Detects the Rust standard library source directory via `rustc --print sysroot`.
     */
    public static String detectStandardLibrary(String toolchainPath) {
        String exe = resolveExecutable(toolchainPath, "rustc");
        try {
            Process process = new ProcessBuilder(exe, "--print", "sysroot").redirectErrorStream(true).start();
            String sysroot;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                sysroot = reader.readLine();
            }
            if (process.waitFor() == 0 && sysroot != null && !sysroot.isBlank()) {
                Path base = Path.of(sysroot.trim());
                Path stdlib = base.resolve("lib").resolve("rustlib").resolve("src").resolve("rust");
                if (Files.isDirectory(stdlib)) {
                    return stdlib.toAbsolutePath().toString();
                }
                // Fallback: check rustup toolchain path
                Path direct = base.resolve("lib").resolve("rustlib");
                if (Files.isDirectory(direct)) {
                    return stdlib.toAbsolutePath().toString();
                }
            }
        } catch (Exception ignored) {
        }

        // Fallback standard path
        String userHome = System.getProperty("user.home", "");
        if (!userHome.isBlank()) {
            File fallback = new File(userHome, ".rustup/toolchains/stable-x86_64-unknown-linux-gnu/lib/rustlib/src/rust");
            if (fallback.isDirectory()) {
                return fallback.getAbsolutePath();
            }
        }
        return "";
    }

    private static String resolveExecutable(String toolchainPath, String binaryName) {
        boolean isWin = System.getProperty("os.name", "").toLowerCase().contains("win");
        String name = isWin ? binaryName + ".exe" : binaryName;
        if (toolchainPath != null && !toolchainPath.isBlank()) {
            File candidate = new File(toolchainPath, name);
            if (candidate.exists()) {
                return candidate.getAbsolutePath();
            }
        }
        return name;
    }

    /**
     * Returns default built-in templates matching IntelliJ IDEA Rust plugin.
     */
    public static List<RustProjectTemplate> defaultTemplates() {
        return List.of(
                new RustProjectTemplate("Binary (application)", "", "binary", true),
                new RustProjectTemplate("Library", "", "library", true),
                new RustProjectTemplate("Procedural Macro",
                        "github.com/intellij-rust/rust-procmacro-quickstart-template",
                        "Custom:https://github.com/intellij-rust/rust-procmacro-quickstart-template", false),
                new RustProjectTemplate("WebAssembly Lib",
                        "github.com/intellij-rust/wasm-pack-template",
                        "Custom:https://github.com/intellij-rust/wasm-pack-template", false)
        );
    }

    /**
     * Checks whether cargo-generate is installed in the toolchain directory, ~/.cargo/bin, or PATH.
     */
    public static boolean isCargoGenerateInstalled(String toolchainPath) {
        boolean isWin = System.getProperty("os.name", "").toLowerCase().contains("win");
        String binName = isWin ? "cargo-generate.exe" : "cargo-generate";

        if (toolchainPath != null && !toolchainPath.isBlank()) {
            File inToolchain = new File(toolchainPath, binName);
            if (inToolchain.exists()) return true;
        }

        String userHome = System.getProperty("user.home", "");
        if (!userHome.isBlank()) {
            File inCargoBin = new File(userHome, ".cargo" + File.separator + "bin" + File.separator + binName);
            if (inCargoBin.exists()) return true;
        }

        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String p : pathEnv.split(Pattern.quote(File.pathSeparator))) {
                if (!p.isBlank() && new File(p, binName).exists()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Installs cargo-generate via `cargo install cargo-generate`.
     * Returns process exit code.
     */
    public static int installCargoGenerate(String toolchainPath, java.util.function.Consumer<String> log) {
        String cargoExe = resolveExecutable(toolchainPath, "cargo");
        ProcessBuilder pb = new ProcessBuilder(cargoExe, "install", "cargo-generate").redirectErrorStream(true);
        try {
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (log != null) log.accept(line);
                }
            }
            return process.waitFor();
        } catch (Exception e) {
            if (log != null) log.accept("Failed to run cargo install cargo-generate: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Generates IntelliJ IDEA .idea/modules.xml.
     */
    public static String generateIdeaModulesXml(String projectName) {
        String name = projectName != null && !projectName.isBlank() ? projectName : "untitled";
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project version="4">
                  <component name="ProjectModuleManager">
                    <modules>
                      <module fileurl="file://$PROJECT_DIR$/.idea/%s.iml" filepath="$PROJECT_DIR$/.idea/%s.iml" />
                    </modules>
                  </component>
                </project>
                """.formatted(name, name);
    }

    /**
     * Generates IntelliJ IDEA .idea/<name>.iml for Rust projects.
     */
    public static String generateIdeaIml() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <module type="EMPTY_MODULE" version="4">
                  <component name="NewModuleRootManager">
                    <content url="file://$MODULE_DIR$">
                      <sourceFolder url="file://$MODULE_DIR$/src" isTestSource="false" />
                      <excludeFolder url="file://$MODULE_DIR$/target" />
                    </content>
                    <orderEntry type="inheritedJdk" />
                    <orderEntry type="sourceFolder" forTests="false" />
                  </component>
                </module>
                """;
    }
}
