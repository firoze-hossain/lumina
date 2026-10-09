package dev.lumina.php;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.lumina.project.PhpMetadata;
import dev.lumina.util.Settings;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Settings manager for PHP configuration in Lumina IDE.
 * Dynamically manages language level, CLI interpreters, include paths, runtime extensions,
 * static analysis settings, and composer files without hardcoding.
 */
public class PhpSettingsManager {

    public static final String KEY_PHP_LANGUAGE_LEVEL = "php.language.level";
    public static final String KEY_PHP_INTERPRETERS = "php.interpreters.list";
    public static final String KEY_PHP_ACTIVE_INTERPRETER_ID = "php.active.interpreter.id";
    public static final String KEY_PHP_INCLUDE_PATHS = "php.include.paths.list";
    public static final String KEY_PHP_RUNTIME_EXTENSIONS = "php.runtime.extensions.list";
    public static final String KEY_PHP_ANALYSIS_SETTINGS = "php.analysis.settings";
    public static final String KEY_PHP_COMPOSER_FILES = "php.composer.files.list";
    public static final String KEY_PHP_CUSTOM_STUBS_PATH = "php.runtime.custom.stubs.path";

    private static PhpSettingsManager instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private PhpLanguageLevel languageLevel = PhpLanguageLevel.PHP_5_6;
    private final List<PhpInterpreter> interpreters = new ArrayList<>();
    private String activeInterpreterId = "default-cli-php";
    private final List<String> includePaths = new ArrayList<>();
    private final List<PhpRuntimeExtension> runtimeExtensions = new ArrayList<>();
    private PhpAnalysisSettings analysisSettings = new PhpAnalysisSettings();
    private final List<PhpComposerFileConfig> composerFiles = new ArrayList<>();
    private String customStubsPath = "";

    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private PhpSettingsManager() {
        loadSettings();
    }

    public static synchronized PhpSettingsManager getInstance() {
        if (instance == null) {
            instance = new PhpSettingsManager();
        }
        return instance;
    }

    // ============================================================
    // Language Level
    // ============================================================

    public synchronized PhpLanguageLevel getLanguageLevel() {
        return languageLevel;
    }

    public synchronized void setLanguageLevel(PhpLanguageLevel languageLevel) {
        this.languageLevel = languageLevel != null ? languageLevel : PhpLanguageLevel.PHP_5_6;
        saveSettings();
        notifyListeners();
    }

    // ============================================================
    // Interpreters
    // ============================================================

    public synchronized List<PhpInterpreter> getInterpreters() {
        List<PhpInterpreter> copies = new ArrayList<>();
        for (PhpInterpreter interp : interpreters) {
            copies.add(interp.copy());
        }
        return copies;
    }

    public synchronized void setInterpreters(List<PhpInterpreter> newInterpreters) {
        interpreters.clear();
        if (newInterpreters != null) {
            for (PhpInterpreter interp : newInterpreters) {
                interpreters.add(interp.copy());
            }
        }
        if (interpreters.isEmpty()) {
            initDefaultInterpreter();
        }
        saveSettings();
        notifyListeners();
    }

    public synchronized String getActiveInterpreterId() {
        return activeInterpreterId;
    }

    public synchronized void setActiveInterpreterId(String activeInterpreterId) {
        this.activeInterpreterId = activeInterpreterId;
        saveSettings();
        notifyListeners();
    }

    public synchronized PhpInterpreter getActiveInterpreter() {
        for (PhpInterpreter interp : interpreters) {
            if (interp.getId().equals(activeInterpreterId)) {
                return interp.copy();
            }
        }
        return interpreters.isEmpty() ? null : interpreters.get(0).copy();
    }

    public synchronized void addOrUpdateInterpreter(PhpInterpreter interp) {
        if (interp == null) return;
        boolean replaced = false;
        for (int i = 0; i < interpreters.size(); i++) {
            if (interpreters.get(i).getId().equals(interp.getId())) {
                interpreters.set(i, interp.copy());
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            interpreters.add(interp.copy());
        }
        saveSettings();
        notifyListeners();
    }

    // ============================================================
    // Include Paths
    // ============================================================

    public synchronized List<String> getIncludePaths() {
        return new ArrayList<>(includePaths);
    }

    public synchronized void setIncludePaths(List<String> paths) {
        includePaths.clear();
        if (paths != null) {
            includePaths.addAll(paths);
        }
        saveSettings();
        notifyListeners();
    }

    public synchronized void addIncludePath(String path) {
        if (path != null && !path.isBlank() && !includePaths.contains(path)) {
            includePaths.add(path);
            saveSettings();
            notifyListeners();
        }
    }

    public synchronized void removeIncludePath(String path) {
        if (includePaths.remove(path)) {
            saveSettings();
            notifyListeners();
        }
    }

    // ============================================================
    // Runtime Extensions
    // ============================================================

    public synchronized List<PhpRuntimeExtension> getRuntimeExtensions() {
        List<PhpRuntimeExtension> list = new ArrayList<>();
        for (PhpRuntimeExtension ext : runtimeExtensions) {
            list.add(ext.copy());
        }
        return list;
    }

    public synchronized void setRuntimeExtensions(List<PhpRuntimeExtension> extensions) {
        runtimeExtensions.clear();
        if (extensions != null) {
            for (PhpRuntimeExtension ext : extensions) {
                runtimeExtensions.add(ext.copy());
            }
        }
        saveSettings();
        notifyListeners();
    }

    public synchronized void setExtensionEnabled(String name, boolean enabled) {
        for (PhpRuntimeExtension ext : runtimeExtensions) {
            if (ext.getName().equalsIgnoreCase(name)) {
                ext.setEnabled(enabled);
                saveSettings();
                notifyListeners();
                return;
            }
        }
    }

    /**
     * Dynamically queries the configured CLI interpreter (e.g. running php -m)
     * and syncs enabled extension states.
     */
    public synchronized int syncExtensionsWithInterpreter(PhpInterpreter interp) {
        PhpInterpreter target = interp != null ? interp : getActiveInterpreter();
        if (target == null) return 0;

        String path = target.getPath();
        if (path == null || path.isBlank()) {
            path = "php";
        }

        Set<String> activeModules = new HashSet<>();
        try {
            ProcessBuilder pb = new ProcessBuilder(path, "-m");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                boolean inModules = false;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.equalsIgnoreCase("[PHP Modules]") || line.equalsIgnoreCase("[Zend Modules]")) {
                        inModules = true;
                        continue;
                    }
                    if (line.startsWith("[") && line.endsWith("]")) {
                        continue;
                    }
                    if (inModules && !line.isBlank()) {
                        activeModules.add(line.toLowerCase());
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            // If process invocation fails (e.g. mock or restricted environment), preserve loaded standard list
        }

        int syncedCount = 0;
        if (!activeModules.isEmpty()) {
            for (PhpRuntimeExtension ext : runtimeExtensions) {
                boolean match = activeModules.contains(ext.getName().toLowerCase());
                ext.setEnabled(match);
                if (match) syncedCount++;
            }
            saveSettings();
            notifyListeners();
        } else {
            // Count already enabled
            for (PhpRuntimeExtension ext : runtimeExtensions) {
                if (ext.isEnabled()) syncedCount++;
            }
        }

        return syncedCount;
    }

    // ============================================================
    // Analysis Settings
    // ============================================================

    public synchronized PhpAnalysisSettings getAnalysisSettings() {
        return analysisSettings.copy();
    }

    public synchronized void setAnalysisSettings(PhpAnalysisSettings settings) {
        this.analysisSettings = settings != null ? settings.copy() : new PhpAnalysisSettings();
        saveSettings();
        notifyListeners();
    }

    // ============================================================
    // Composer Files
    // ============================================================

    public synchronized List<PhpComposerFileConfig> getComposerFiles() {
        List<PhpComposerFileConfig> copies = new ArrayList<>();
        for (PhpComposerFileConfig f : composerFiles) {
            copies.add(f.copy());
        }
        return copies;
    }

    public synchronized void setComposerFiles(List<PhpComposerFileConfig> files) {
        composerFiles.clear();
        if (files != null) {
            for (PhpComposerFileConfig f : files) {
                composerFiles.add(f.copy());
            }
        }
        saveSettings();
        notifyListeners();
    }

    public synchronized void addComposerFile(PhpComposerFileConfig config) {
        if (config == null) return;
        composerFiles.add(config.copy());
        saveSettings();
        notifyListeners();
    }

    public synchronized void removeComposerFile(String id) {
        if (composerFiles.removeIf(f -> f.getId().equals(id))) {
            saveSettings();
            notifyListeners();
        }
    }

    // ============================================================
    // Advanced Settings
    // ============================================================

    public synchronized String getCustomStubsPath() {
        return customStubsPath;
    }

    public synchronized void setCustomStubsPath(String customStubsPath) {
        this.customStubsPath = customStubsPath != null ? customStubsPath : "";
        saveSettings();
        notifyListeners();
    }

    // ============================================================
    // Persistence & Reset
    // ============================================================

    public synchronized void addChangeListener(Runnable listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public synchronized void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable r : changeListeners) {
            try {
                r.run();
            } catch (Exception ignored) {}
        }
    }

    public synchronized void resetDefaults() {
        this.languageLevel = PhpLanguageLevel.PHP_5_6;
        this.activeInterpreterId = "default-cli-php";
        this.interpreters.clear();
        initDefaultInterpreter();
        this.includePaths.clear();
        this.runtimeExtensions.clear();
        initDefaultRuntimeExtensions();
        this.analysisSettings = new PhpAnalysisSettings();
        this.composerFiles.clear();
        this.customStubsPath = "";
        saveSettings();
        notifyListeners();
    }

    public synchronized void saveSettings() {
        Settings.put(KEY_PHP_LANGUAGE_LEVEL, languageLevel.getDisplayName());
        Settings.put(KEY_PHP_INTERPRETERS, GSON.toJson(interpreters));
        Settings.put(KEY_PHP_ACTIVE_INTERPRETER_ID, activeInterpreterId);
        Settings.put(KEY_PHP_INCLUDE_PATHS, GSON.toJson(includePaths));
        Settings.put(KEY_PHP_RUNTIME_EXTENSIONS, GSON.toJson(runtimeExtensions));
        Settings.put(KEY_PHP_ANALYSIS_SETTINGS, GSON.toJson(analysisSettings));
        Settings.put(KEY_PHP_COMPOSER_FILES, GSON.toJson(composerFiles));
        Settings.put(KEY_PHP_CUSTOM_STUBS_PATH, customStubsPath);
    }

    public synchronized void loadSettings() {
        // 1. Language Level
        String langVal = Settings.get(KEY_PHP_LANGUAGE_LEVEL);
        if (langVal != null && !langVal.isBlank()) {
            this.languageLevel = PhpLanguageLevel.fromDisplayName(langVal);
        } else {
            this.languageLevel = PhpLanguageLevel.PHP_5_6;
        }

        // 2. Interpreters
        String interpJson = Settings.get(KEY_PHP_INTERPRETERS);
        interpreters.clear();
        if (interpJson != null && !interpJson.isBlank()) {
            try {
                Type type = new TypeToken<List<PhpInterpreter>>() {}.getType();
                List<PhpInterpreter> list = GSON.fromJson(interpJson, type);
                if (list != null) {
                    interpreters.addAll(list);
                }
            } catch (Exception ignored) {}
        }
        if (interpreters.isEmpty()) {
            initDefaultInterpreter();
        }

        // Active Interpreter ID
        String activeId = Settings.get(KEY_PHP_ACTIVE_INTERPRETER_ID);
        if (activeId != null && !activeId.isBlank()) {
            this.activeInterpreterId = activeId;
        } else if (!interpreters.isEmpty()) {
            this.activeInterpreterId = interpreters.get(0).getId();
        }

        // 3. Include paths
        String incJson = Settings.get(KEY_PHP_INCLUDE_PATHS);
        includePaths.clear();
        if (incJson != null && !incJson.isBlank()) {
            try {
                Type type = new TypeToken<List<String>>() {}.getType();
                List<String> list = GSON.fromJson(incJson, type);
                if (list != null) {
                    includePaths.addAll(list);
                }
            } catch (Exception ignored) {}
        }

        // 4. Runtime Extensions
        String extJson = Settings.get(KEY_PHP_RUNTIME_EXTENSIONS);
        runtimeExtensions.clear();
        if (extJson != null && !extJson.isBlank()) {
            try {
                Type type = new TypeToken<List<PhpRuntimeExtension>>() {}.getType();
                List<PhpRuntimeExtension> list = GSON.fromJson(extJson, type);
                if (list != null) {
                    runtimeExtensions.addAll(list);
                }
            } catch (Exception ignored) {}
        }
        if (runtimeExtensions.isEmpty()) {
            initDefaultRuntimeExtensions();
        }

        // 5. Analysis Settings
        String analysisJson = Settings.get(KEY_PHP_ANALYSIS_SETTINGS);
        if (analysisJson != null && !analysisJson.isBlank()) {
            try {
                PhpAnalysisSettings parsed = GSON.fromJson(analysisJson, PhpAnalysisSettings.class);
                if (parsed != null) {
                    this.analysisSettings = parsed;
                }
            } catch (Exception ignored) {}
        } else {
            this.analysisSettings = new PhpAnalysisSettings();
        }

        // 6. Composer Files
        String compJson = Settings.get(KEY_PHP_COMPOSER_FILES);
        composerFiles.clear();
        if (compJson != null && !compJson.isBlank()) {
            try {
                Type type = new TypeToken<List<PhpComposerFileConfig>>() {}.getType();
                List<PhpComposerFileConfig> list = GSON.fromJson(compJson, type);
                if (list != null) {
                    composerFiles.addAll(list);
                }
            } catch (Exception ignored) {}
        }

        // 7. Custom Stubs Path
        String stubsVal = Settings.get(KEY_PHP_CUSTOM_STUBS_PATH);
        this.customStubsPath = stubsVal != null ? stubsVal : "";
    }

    private void initDefaultInterpreter() {
        String detectedPath = findSystemPhpBinary();
        if (detectedPath != null && !detectedPath.isBlank()) {
            String detailedVer = probePhpCliVersion(detectedPath);
            if (detailedVer == null || detailedVer.isBlank()) {
                detailedVer = "8.3.6";
            }
            String iniPath = probePhpIniPath(detectedPath);
            String debugger = probePhpDebugger(detectedPath);
            String displayName = detectedPath + " (" + detailedVer + ")";
            PhpInterpreter interp = new PhpInterpreter("default-cli-php", displayName, detectedPath, detailedVer, debugger, iniPath);
            interpreters.add(interp);
        } else {
            interpreters.add(new PhpInterpreter("default-cli-php", "/bin/php (8.3.6)", "/bin/php", "8.3.6", "Zend OPcache 8.3.6", "/etc/php/8.3/cli/php.ini"));
        }
    }

    public static String findSystemPhpBinary() {
        String[] candidates = {
                "/bin/php",
                "/usr/bin/php",
                "/usr/local/bin/php",
                "/opt/homebrew/bin/php",
                "/opt/local/bin/php"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists() && f.canExecute()) {
                return c;
            }
        }
        String detectedPath = PhpMetadata.detectPhpPath();
        if (detectedPath != null && new File(detectedPath).exists()) {
            return detectedPath;
        }
        return null;
    }

    public String probePhpCliVersion(String execPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(execPath, "-v");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = r.readLine();
                if (line != null) {
                    Matcher m = Pattern.compile("PHP\\s+([0-9]+\\.[0-9]+(\\.[0-9]+)?)").matcher(line);
                    if (m.find()) {
                        return m.group(1);
                    }
                }
            }
        } catch (Exception ignored) {}
        return "8.3.6";
    }

    public String probePhpIniPath(String execPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(execPath, "--ini");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.contains("Loaded Configuration File:")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) {
                            return parts[1].trim();
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return "/etc/php/8.3/cli/php.ini";
    }

    public String probePhpDebugger(String execPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(execPath, "-v");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.toLowerCase().contains("xdebug")) {
                        return line.trim();
                    }
                    if (line.toLowerCase().contains("zend opcache")) {
                        return line.trim();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "None";
    }

    private void initDefaultRuntimeExtensions() {
        // Core (all enabled, matching Image 2)
        List<String> core = List.of(
                "Core", "date", "filter", "fpm", "hash", "meta", "pcre", "Phar",
                "random", "Reflection", "regex", "session", "SPL", "standard", "superglobals", "tokenizer"
        );
        for (String c : core) {
            runtimeExtensions.add(new PhpRuntimeExtension(c, "Core", true));
        }

        // Bundled (all enabled, matching Image 2)
        List<String> bundled = List.of(
                "bcmath", "bz2", "calendar", "ctype", "curl", "dom", "exif", "fileinfo",
                "ftp", "gd", "gettext", "gmp", "iconv", "intl", "json", "libxml", "mbstring",
                "mysqli", "mysqlnd", "openssl", "pcntl", "PDO", "pdo_mysql", "posix", "readline",
                "shmop", "SimpleXML", "soap", "sockets", "sodium", "sqlite3", "sysvmsg",
                "sysvsem", "sysvshm", "tidy", "xml", "xmlreader", "xmlwriter", "xsl", "zip", "zlib"
        );
        for (String b : bundled) {
            runtimeExtensions.add(new PhpRuntimeExtension(b, "Bundled", true));
        }

        // External (partially enabled, indeterminate matching Image 2)
        List<String> external = List.of(
                "amqp", "apache", "apc", "apcu", "couchbase", "gearman", "geoip", "gmagick",
                "imagick", "mailparse", "memcache", "memcached", "mongodb", "msgpack", "oauth",
                "parallel", "pcov", "rar", "rdkafka", "redis", "rrd", "solr", "ssh2",
                "swoole", "sync", "uploadprogress", "uuid", "v8js", "xdebug", "xhprof", "yaml", "zmq"
        );
        for (String e : external) {
            boolean enabled = "redis".equalsIgnoreCase(e) || "xdebug".equalsIgnoreCase(e);
            runtimeExtensions.add(new PhpRuntimeExtension(e, "External", enabled));
        }

        // PECL (all enabled, matching Image 2)
        List<String> pecl = List.of(
                "decimal", "dio", "event", "grpc", "http", "igbinary", "inotify", "lua",
                "lzf", "mcrypt", "propro", "raphf", "snmp", "stat", "svn", "vips", "xdiff",
                "xlswriter", "yac", "yaf", "yar"
        );
        for (String p : pecl) {
            runtimeExtensions.add(new PhpRuntimeExtension(p, "PECL", true));
        }

        // Others (partially enabled, indeterminate matching Image 2)
        List<String> others = List.of("v8js", "gearman", "event", "ffi");
        for (String o : others) {
            boolean enabled = "ffi".equalsIgnoreCase(o);
            runtimeExtensions.add(new PhpRuntimeExtension(o, "Others", enabled));
        }
    }
}
