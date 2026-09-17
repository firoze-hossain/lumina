package dev.lumina.project;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service providing dynamic PHP runtime and Composer discovery,
 * language level detection, and composer.json templating matching IntelliJ IDEA.
 */
public final class PhpMetadata {

    public static final String DEFAULT_PHP_LANGUAGE_LEVEL = "8.3";
    public static final String DEFAULT_COMPOSER_TYPE = "project";

    private static volatile String cachedPhpExecutable = null;
    private static volatile String cachedPhpVersion = null;
    private static volatile String cachedComposerExecutable = null;
    private static final Object LOCK = new Object();

    private PhpMetadata() {
    }

    /**
     * Dynamically detects the path to the system php binary.
     */
    public static String detectPhpPath() {
        if (cachedPhpExecutable != null) return cachedPhpExecutable;
        synchronized (LOCK) {
            if (cachedPhpExecutable != null) return cachedPhpExecutable;

            List<String> candidates = new ArrayList<>();

            // 1. Check standard Unix / macOS paths
            candidates.addAll(List.of(
                    "/usr/bin/php",
                    "/usr/local/bin/php",
                    "/opt/homebrew/bin/php",
                    "/opt/local/bin/php",
                    "/usr/bin/php8.3",
                    "/usr/bin/php8.2",
                    "/usr/bin/php8.1"
            ));

            // 2. Check Windows paths
            String systemDrive = System.getenv("SystemDrive");
            if (systemDrive != null) {
                candidates.add(systemDrive + "\\php\\php.exe");
                candidates.add(systemDrive + "\\tools\\php\\php.exe");
                candidates.add(systemDrive + "\\xampp\\php\\php.exe");
            }

            for (String c : candidates) {
                File f = new File(c);
                if (f.exists() && f.canExecute()) {
                    cachedPhpExecutable = f.getAbsolutePath();
                    return cachedPhpExecutable;
                }
            }

            // 3. Scan PATH
            String pathEnv = System.getenv("PATH");
            if (pathEnv != null) {
                for (String dir : pathEnv.split(Pattern.quote(File.pathSeparator))) {
                    if (dir.isBlank()) continue;
                    File f = new File(dir, System.getProperty("os.name", "").toLowerCase().contains("win") ? "php.exe" : "php");
                    if (f.exists() && f.canExecute()) {
                        cachedPhpExecutable = f.getAbsolutePath();
                        return cachedPhpExecutable;
                    }
                }
            }

            cachedPhpExecutable = "";
            return cachedPhpExecutable;
        }
    }

    /**
     * Dynamically probes the PHP language level (e.g. "8.3", "8.2") using the php binary.
     */
    public static String detectPhpLanguageLevel() {
        if (cachedPhpVersion != null) return cachedPhpVersion;
        synchronized (LOCK) {
            if (cachedPhpVersion != null) return cachedPhpVersion;

            String phpPath = detectPhpPath();
            if (!phpPath.isBlank()) {
                try {
                    ProcessBuilder pb = new ProcessBuilder(phpPath, "-r", "echo PHP_MAJOR_VERSION . '.' . PHP_MINOR_VERSION;");
                    pb.redirectErrorStream(true);
                    Process p = pb.start();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                        String line = reader.readLine();
                        if (line != null && line.matches("\\d+\\.\\d+")) {
                            cachedPhpVersion = line.trim();
                            return cachedPhpVersion;
                        }
                    }
                } catch (Exception ignored) {
                }

                // Fallback to parsing php -v
                try {
                    ProcessBuilder pb = new ProcessBuilder(phpPath, "-v");
                    pb.redirectErrorStream(true);
                    Process p = pb.start();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                        String line = reader.readLine();
                        if (line != null) {
                            Matcher m = Pattern.compile("PHP\\s+(\\d+\\.\\d+)").matcher(line);
                            if (m.find()) {
                                cachedPhpVersion = m.group(1);
                                return cachedPhpVersion;
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            cachedPhpVersion = DEFAULT_PHP_LANGUAGE_LEVEL;
            return cachedPhpVersion;
        }
    }

    /**
     * Dynamically detects the path to the composer binary.
     */
    public static String detectComposerPath() {
        if (cachedComposerExecutable != null) return cachedComposerExecutable;
        synchronized (LOCK) {
            if (cachedComposerExecutable != null) return cachedComposerExecutable;

            List<String> candidates = new ArrayList<>();
            candidates.addAll(List.of(
                    "/usr/bin/composer",
                    "/usr/local/bin/composer",
                    "/opt/homebrew/bin/composer"
            ));

            String userHome = System.getProperty("user.home", "");
            if (!userHome.isBlank()) {
                candidates.add(userHome + "/.composer/vendor/bin/composer");
                candidates.add(userHome + "/.config/composer/vendor/bin/composer");
            }

            for (String c : candidates) {
                File f = new File(c);
                if (f.exists() && f.canExecute()) {
                    cachedComposerExecutable = f.getAbsolutePath();
                    return cachedComposerExecutable;
                }
            }

            String pathEnv = System.getenv("PATH");
            if (pathEnv != null) {
                for (String dir : pathEnv.split(Pattern.quote(File.pathSeparator))) {
                    if (dir.isBlank()) continue;
                    File f = new File(dir, System.getProperty("os.name", "").toLowerCase().contains("win") ? "composer.bat" : "composer");
                    if (f.exists() && f.canExecute()) {
                        cachedComposerExecutable = f.getAbsolutePath();
                        return cachedComposerExecutable;
                    }
                }
            }

            cachedComposerExecutable = "";
            return cachedComposerExecutable;
        }
    }

    /**
     * Generates standard composer.json matching IntelliJ / PhpStorm's default template.
     */
    public static String generateComposerJson(String projectName) {
        String vendor = System.getProperty("user.name", "vendor").toLowerCase().replaceAll("[^a-z0-9_-]", "");
        if (vendor.isBlank()) {
            vendor = "vendor";
        }
        String pkgName = projectName != null ? projectName.toLowerCase().replaceAll("[^a-z0-9_-]", "") : "untitled";
        if (pkgName.isBlank()) {
            pkgName = "untitled";
        }

        return """
                {
                    "name": "%s/%s",
                    "type": "project",
                    "require": {}
                }
                """.formatted(vendor, pkgName);
    }

    /**
     * Generates IntelliJ IDEA .idea/php.xml configuration.
     */
    public static String generateIdeaPhpXml(String languageLevel) {
        String level = languageLevel != null && !languageLevel.isBlank() ? languageLevel : detectPhpLanguageLevel();
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project version="4">
                  <component name="PhpProjectSharedConfiguration" php_language_level="%s" />
                </project>
                """.formatted(level);
    }

    /**
     * Generates IntelliJ IDEA .idea/<name>.iml module configuration.
     */
    public static String generateIdeaIml() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <module type="WEB_MODULE" version="4">
                  <component name="NewModuleRootManager">
                    <content url="file://$MODULE_DIR$">
                      <excludeFolder url="file://$MODULE_DIR$/vendor" />
                    </content>
                    <orderEntry type="sourceFolder" forTests="false" />
                  </component>
                </module>
                """;
    }

    /**
     * Generates .gitignore for PHP projects.
     */
    public static String generateGitignore() {
        return """
                /vendor/
                .idea/
                .env
                .env.local
                composer.phar
                """;
    }

    /**
     * Generates standard starter index.php.
     */
    public static String generateStarterIndexPhp() {
        return """
                <?php

                echo "Hello, World!";
                """;
    }
}
