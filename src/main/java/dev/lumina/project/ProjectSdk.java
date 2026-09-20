package dev.lumina.project;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.prefs.Preferences;

/**
 * Universal model and discovery service for SDKs (JDKs, Python, PHP, Node, etc.)
 * matching IntelliJ IDEA's Platform Settings -> SDKs architecture.
 */
public final class ProjectSdk {

    public enum SdkType {
        JDK("JavaSDK", "JDK", "☕"),
        PYTHON("PythonSDK", "Python SDK", "🐍"),
        PHP("PhpSDK", "PHP Runtime", "🐘"),
        RUBY("RubySDK", "Ruby SDK", "💎"),
        JRUBY("JRubySDK", "JRuby SDK", "🐦"),
        NODE("NodeSDK", "Node.js", "◆"),
        GENERIC("GenericSDK", "SDK", "📁");

        private final String ideaTypeId;
        private final String displayLabel;
        private final String iconGlyph;

        SdkType(String ideaTypeId, String displayLabel, String iconGlyph) {
            this.ideaTypeId = ideaTypeId;
            this.displayLabel = displayLabel;
            this.iconGlyph = iconGlyph;
        }

        public String ideaTypeId() {
            return ideaTypeId;
        }

        public String displayLabel() {
            return displayLabel;
        }

        public String iconGlyph() {
            return iconGlyph;
        }
    }

    public record SdkItem(
            String id,
            String name,
            SdkType type,
            String homePath,
            String version,
            boolean isRegistered,
            boolean isDetected,
            List<String> classpathEntries
    ) {
        @Override
        public String toString() {
            return name;
        }

        public String getShortDisplay() {
            return name;
        }

        public String getFullDisplay() {
            if (homePath != null && !homePath.isBlank()) {
                return name + "  " + homePath;
            }
            return name;
        }
    }

    private static final String PREF_REGISTERED_SDKS = "lumina.registered.sdks";
    private static final Preferences PREFS = Preferences.userNodeForPackage(ProjectSdk.class);

    private ProjectSdk() {}

    /**
     * Resolves all available SDKs (both registered in settings and detected on system).
     */
    public static List<SdkItem> discoverAllSdks() {
        List<SdkItem> result = new ArrayList<>();
        Set<String> seenHomes = new HashSet<>();

        // 1. Load user-registered SDKs
        List<SdkItem> registered = loadRegisteredSdks();
        for (SdkItem item : registered) {
            result.add(item);
            if (item.homePath() != null) seenHomes.add(item.homePath());
        }

        // 2. Discover local JDKs via JdkMetadata
        try {
            List<JdkMetadata.JdkInstallation> jdks = JdkMetadata.detectInstallations(true);
            for (JdkMetadata.JdkInstallation jdk : jdks) {
                if (jdk.homePath() != null && !seenHomes.contains(jdk.homePath())) {
                    seenHomes.add(jdk.homePath());
                    result.add(new SdkItem(
                            "jdk-" + jdk.name() + "-" + jdk.majorVersion(),
                            jdk.formatDisplay(),
                            SdkType.JDK,
                            jdk.homePath(),
                            jdk.version(),
                            false,
                            true,
                            detectJdkClasspath(jdk.homePath())
                    ));
                }
            }
        } catch (Exception ignored) {}

        // 3. Discover Python interpreters via PythonMetadata
        try {
            List<PythonMetadata.PythonInstallation> pythons = PythonMetadata.fetchPythonInstallations(false);
            for (PythonMetadata.PythonInstallation py : pythons) {
                if (py.executable() != null && !seenHomes.contains(py.executable())) {
                    seenHomes.add(py.executable());
                    result.add(new SdkItem(
                            "python-" + py.label(),
                            py.label(),
                            SdkType.PYTHON,
                            py.executable(),
                            py.label(),
                            false,
                            true,
                            detectPythonClasspath(py.executable())
                    ));
                }
            }
        } catch (Exception ignored) {}

        // 4. Discover PHP via PhpMetadata
        try {
            String phpPath = PhpMetadata.detectPhpPath();
            if (phpPath != null && !phpPath.isBlank() && !seenHomes.contains(phpPath)) {
                seenHomes.add(phpPath);
                String phpVer = PhpMetadata.detectPhpLanguageLevel();
                String name = "PHP " + (phpVer.isBlank() ? "8" : phpVer);
                result.add(new SdkItem(
                        "php-" + phpVer,
                        name,
                        SdkType.PHP,
                        phpPath,
                        phpVer,
                        false,
                        true,
                        List.of(phpPath)
                ));
            }
        } catch (Exception ignored) {}

        // 5. Discover Node via NodeMetadata
        try {
            List<NodeMetadata.NodeInterpreter> interpreters = NodeMetadata.detectInterpreters(false);
            if (interpreters != null) {
                for (NodeMetadata.NodeInterpreter inter : interpreters) {
                    if (inter.path() != null && !inter.path().isBlank() && !seenHomes.contains(inter.path())) {
                        seenHomes.add(inter.path());
                        String ver = inter.version() != null ? inter.version() : "LTS";
                        String name = "Node.js " + ver;
                        result.add(new SdkItem(
                                "node-" + ver,
                                name,
                                SdkType.NODE,
                                inter.path(),
                                ver,
                                false,
                                true,
                                List.of(inter.path())
                        ));
                    }
                }
            }
        } catch (Exception ignored) {}

        return result;
    }

    /**
     * Resolves real classpath modules/jars for a JDK home.
     */
    public static List<String> detectJdkClasspath(String homePath) {
        List<String> list = new ArrayList<>();
        if (homePath == null || homePath.isBlank()) return list;

        Path home = Path.of(homePath);
        // Modern JDKs: check jmods/
        Path jmods = home.resolve("jmods");
        if (Files.isDirectory(jmods)) {
            try (var s = Files.list(jmods)) {
                s.filter(p -> p.toString().endsWith(".jmod"))
                 .sorted()
                 .forEach(p -> list.add(p.toString()));
            } catch (Exception ignored) {}
        }

        // If no jmods or in addition, check lib/modules or lib/rt.jar
        if (list.isEmpty()) {
            Path modulesFile = home.resolve("lib/modules");
            if (Files.isRegularFile(modulesFile)) {
                list.add(modulesFile.toString());
            }
            Path rtJar = home.resolve("jre/lib/rt.jar");
            if (Files.isRegularFile(rtJar)) {
                list.add(rtJar.toString());
            } else {
                Path directRt = home.resolve("lib/rt.jar");
                if (Files.isRegularFile(directRt)) {
                    list.add(directRt.toString());
                }
            }
        }

        // Standard standard module names as fallback display
        if (list.isEmpty()) {
            String[] baseMods = {
                    "java.base", "java.compiler", "java.datatransfer", "java.desktop",
                    "java.instrument", "java.logging", "java.management", "java.naming",
                    "java.net.http", "java.prefs", "java.rmi", "java.scripting",
                    "java.se", "java.security.jgss", "java.security.sasl", "java.sql",
                    "java.xml", "jdk.attach", "jdk.compiler", "jdk.javadoc"
            };
            for (String mod : baseMods) {
                list.add(homePath + "/" + mod);
            }
        }

        return list;
    }

    /**
     * Resolves classpath entries / site-packages for Python interpreter.
     */
    public static List<String> detectPythonClasspath(String executable) {
        List<String> list = new ArrayList<>();
        if (executable == null || executable.isBlank()) return list;

        list.add(executable);
        File exe = new File(executable);
        File parent = exe.getParentFile();
        if (parent != null) {
            File libDir = new File(parent.getParentFile(), "lib");
            if (libDir.isDirectory()) {
                File[] children = libDir.listFiles(f -> f.isDirectory() && f.getName().startsWith("python"));
                if (children != null) {
                    for (File c : children) {
                        list.add(c.getAbsolutePath());
                        File sitePkgs = new File(c, "site-packages");
                        if (sitePkgs.isDirectory()) {
                            list.add(sitePkgs.getAbsolutePath());
                        }
                    }
                }
            }
        }
        return list;
    }

    /**
     * Loads registered SDKs from IDE preferences.
     */
    public static List<SdkItem> loadRegisteredSdks() {
        List<SdkItem> list = new ArrayList<>();
        String raw = PREFS.get(PREF_REGISTERED_SDKS, "");
        if (raw.isBlank()) {
            // Default seed: register current host JDK if detected
            try {
                List<JdkMetadata.JdkInstallation> jdks = JdkMetadata.detectInstallations(false);
                if (!jdks.isEmpty()) {
                    JdkMetadata.JdkInstallation primary = jdks.getFirst();
                    SdkItem item = new SdkItem(
                            "jdk-" + primary.majorVersion(),
                            primary.formatDisplay(),
                            SdkType.JDK,
                            primary.homePath(),
                            primary.version(),
                            true,
                            true,
                            detectJdkClasspath(primary.homePath())
                    );
                    list.add(item);
                }
            } catch (Exception ignored) {}
            return list;
        }

        for (String entry : raw.split(";;")) {
            if (entry.isBlank()) continue;
            String[] parts = entry.split("\\|\\|", -1);
            if (parts.length >= 5) {
                SdkType type;
                try {
                    type = SdkType.valueOf(parts[2]);
                } catch (Exception e) {
                    type = SdkType.JDK;
                }
                list.add(new SdkItem(
                        parts[0],
                        parts[1],
                        type,
                        parts[3],
                        parts[4],
                        true,
                        false,
                        type == SdkType.JDK ? detectJdkClasspath(parts[3]) : List.of(parts[3])
                ));
            }
        }
        return list;
    }

    /**
     * Saves user-registered SDKs to IDE preferences.
     */
    public static void saveRegisteredSdks(List<SdkItem> sdks) {
        StringBuilder sb = new StringBuilder();
        for (SdkItem sdk : sdks) {
            if (!sdk.isRegistered()) continue;
            if (!sb.isEmpty()) sb.append(";;");
            sb.append(sdk.id()).append("||")
              .append(sdk.name()).append("||")
              .append(sdk.type().name()).append("||")
              .append(sdk.homePath() != null ? sdk.homePath() : "").append("||")
              .append(sdk.version() != null ? sdk.version() : "");
        }
        PREFS.put(PREF_REGISTERED_SDKS, sb.toString());
    }

    /**
     * Registers a new SDK into preferences.
     */
    public static SdkItem registerSdk(String name, SdkType type, String homePath, String version) {
        List<SdkItem> existing = new ArrayList<>(loadRegisteredSdks());
        String id = type.name().toLowerCase() + "-" + System.currentTimeMillis();
        SdkItem newItem = new SdkItem(
                id,
                name,
                type,
                homePath,
                version,
                true,
                false,
                type == SdkType.JDK ? detectJdkClasspath(homePath) : List.of(homePath)
        );
        existing.removeIf(item -> item.homePath() != null && item.homePath().equals(homePath));
        existing.add(newItem);
        saveRegisteredSdks(existing);
        return newItem;
    }

    public static SdkItem registerFromInstallation(JdkMetadata.JdkInstallation inst) {
        String name = inst.majorVersion() + " " + inst.vendor() + " " + inst.version() + " - " + inst.architecture();
        return registerSdk(name, SdkType.JDK, inst.homePath(), String.valueOf(inst.majorVersion()));
    }
}
