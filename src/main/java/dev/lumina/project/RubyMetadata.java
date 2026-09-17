package dev.lumina.project;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service providing dynamic Ruby interpreter discovery, version probing,
 * and project template generation matching IntelliJ IDEA / RubyMine.
 */
public final class RubyMetadata {

    public record RubyInstallation(
            String label,        // e.g. "ruby-3.3.0"
            String executable,   // e.g. "/usr/bin/ruby"
            String version,      // e.g. "3.3.0"
            String displayLabel  // e.g. "Ruby 3.3.0 (/usr/bin/ruby)"
    ) {
        @Override
        public String toString() {
            return displayLabel;
        }
    }

    public static final String NO_INTERPRETER_LABEL = "[No interpreter selected]";

    private static volatile List<RubyInstallation> cachedInstallations = null;
    private static final Object LOCK = new Object();

    private RubyMetadata() {
    }

    /**
     * Synchronously discovers Ruby installations on the system with caching.
     */
    public static List<RubyInstallation> fetchRubyInstallations(boolean forceRefresh) {
        if (!forceRefresh && cachedInstallations != null) {
            return cachedInstallations;
        }
        synchronized (LOCK) {
            if (!forceRefresh && cachedInstallations != null) {
                return cachedInstallations;
            }
            List<RubyInstallation> discovered = discoverSystemRubies();
            cachedInstallations = Collections.unmodifiableList(discovered);
            return cachedInstallations;
        }
    }

    /**
     * Scans system paths, version managers (rbenv, rvm, asdf, chruby), and PATH for ruby executables.
     */
    public static List<RubyInstallation> discoverSystemRubies() {
        Set<String> candidatePaths = new LinkedHashSet<>();

        // 1. Common Linux / macOS system locations
        List<String> knownPaths = List.of(
                "/usr/bin/ruby",
                "/usr/local/bin/ruby",
                "/opt/homebrew/bin/ruby",
                "/opt/local/bin/ruby"
        );
        for (String p : knownPaths) {
            File f = new File(p);
            if (f.exists() && f.canExecute()) {
                candidatePaths.add(f.getAbsolutePath());
            }
        }

        // 2. User home version managers: rbenv, rvm, asdf, chruby
        String userHome = System.getProperty("user.home", "");
        if (!userHome.isBlank()) {
            // rbenv
            addExecutableIfExists(candidatePaths, userHome + "/.rbenv/shims/ruby");
            scanVersionsDirectory(candidatePaths, userHome + "/.rbenv/versions");

            // rvm
            scanVersionsDirectory(candidatePaths, userHome + "/.rvm/rubies");

            // asdf
            addExecutableIfExists(candidatePaths, userHome + "/.asdf/shims/ruby");
            scanVersionsDirectory(candidatePaths, userHome + "/.asdf/installs/ruby");

            // chruby
            scanVersionsDirectory(candidatePaths, userHome + "/.rubies");
            scanVersionsDirectory(candidatePaths, "/opt/rubies");
        }

        // 3. Scan PATH
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            boolean isWin = System.getProperty("os.name", "").toLowerCase().contains("win");
            String binName = isWin ? "ruby.exe" : "ruby";
            for (String dir : pathEnv.split(Pattern.quote(File.pathSeparator))) {
                if (dir.isBlank()) continue;
                File f = new File(dir, binName);
                if (f.exists() && f.canExecute() && !f.isDirectory()) {
                    candidatePaths.add(f.getAbsolutePath());
                }
            }
        }

        // 4. Windows standard paths
        String systemDrive = System.getenv("SystemDrive");
        if (systemDrive != null) {
            File root = new File(systemDrive + "\\");
            File[] files = root.listFiles((d, name) -> name.toLowerCase().startsWith("ruby"));
            if (files != null) {
                for (File d : files) {
                    File exe = new File(d, "bin\\ruby.exe");
                    if (exe.exists() && exe.canExecute()) {
                        candidatePaths.add(exe.getAbsolutePath());
                    }
                }
            }
        }

        List<RubyInstallation> results = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (String exePath : candidatePaths) {
            try {
                File f = new File(exePath);
                if (!f.exists() || !f.canExecute()) continue;
                String canonical = f.getCanonicalPath();
                if (seen.contains(canonical)) continue;
                seen.add(canonical);

                String version = probeRubyVersion(exePath);
                if (version == null || version.isBlank()) {
                    continue;
                }

                String label = "ruby-" + version;
                String display = "Ruby " + version + " (" + exePath + ")";
                results.add(new RubyInstallation(label, exePath, version, display));
            } catch (Exception ignored) {
            }
        }

        return results;
    }

    private static void addExecutableIfExists(Set<String> list, String path) {
        File f = new File(path);
        if (f.exists() && f.canExecute()) {
            list.add(f.getAbsolutePath());
        }
    }

    private static void scanVersionsDirectory(Set<String> list, String baseDir) {
        File dir = new File(baseDir);
        if (dir.isDirectory()) {
            File[] subDirs = dir.listFiles(File::isDirectory);
            if (subDirs != null) {
                for (File sub : subDirs) {
                    File exe = new File(sub, "bin/ruby");
                    if (exe.exists() && exe.canExecute()) {
                        list.add(exe.getAbsolutePath());
                    }
                }
            }
        }
    }

    /**
     * Probes the Ruby executable for its numeric version (e.g. "3.3.0", "3.2.2").
     */
    public static String probeRubyVersion(String exePath) {
        if (exePath == null || exePath.isBlank()) return null;
        try {
            ProcessBuilder pb = new ProcessBuilder(exePath, "-e", "puts RUBY_VERSION");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && line.matches("\\d+\\.\\d+(\\.\\d+)?")) {
                    return line.trim();
                }
            }
        } catch (Exception ignored) {
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(exePath, "-v");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = reader.readLine();
                if (line != null) {
                    Matcher m = Pattern.compile("ruby\\s+(\\d+\\.\\d+(\\.\\d+)?)").matcher(line);
                    if (m.find()) {
                        return m.group(1);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    /**
     * Validates whether a given file path is a valid Ruby executable.
     */
    public static boolean isValidRuby(String path) {
        if (path == null || path.isBlank()) return false;
        File f = new File(path);
        return f.exists() && f.canExecute() && probeRubyVersion(path) != null;
    }

    /**
     * Creates a RubyInstallation from an arbitrary executable path.
     */
    public static RubyInstallation createInstallationFromPath(String path) {
        String ver = probeRubyVersion(path);
        if (ver == null || ver.isBlank()) {
            ver = "3.2.0";
        }
        String label = "ruby-" + ver;
        String display = "Ruby " + ver + " (" + path + ")";
        return new RubyInstallation(label, path, ver, display);
    }

    /**
     * Generates sample main.rb matching IntelliJ / RubyMine.
     */
    public static String generateSampleCode() {
        return """
                def print_hi(name)
                  puts "Hi, #{name}"
                end

                print_hi('Ruby')
                """;
    }

    /**
     * Generates standard .gitignore for Ruby projects.
     */
    public static String generateGitignore() {
        return """
                /.bundle/
                /vendor/bundle/
                *.gem
                *.rbc
                .idea/
                .env
                """;
    }

    /**
     * Generates IntelliJ IDEA .idea/<name>.iml with RUBY_MODULE type.
     */
    public static String generateIdeaIml() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <module type="RUBY_MODULE" version="4">
                  <component name="NewModuleRootManager">
                    <content url="file://$MODULE_DIR$" />
                    <orderEntry type="inheritedJdk" />
                    <orderEntry type="sourceFolder" forTests="false" />
                  </component>
                </module>
                """;
    }

    /**
     * Generates IntelliJ IDEA .idea/modules.xml.
     */
    public static String generateIdeaModulesXml(String moduleName) {
        String name = moduleName != null && !moduleName.isBlank() ? moduleName : "untitled";
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
     * Generates IntelliJ IDEA .idea/misc.xml.
     */
    public static String generateIdeaMiscXml(String sdkName) {
        String name = sdkName != null && !sdkName.isBlank() ? sdkName : "Ruby";
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project version="4">
                  <component name="ProjectRootManager" version="2" project-jdk-name="%s" project-jdk-type="RUBY_SDK" />
                </project>
                """.formatted(name);
    }
}
