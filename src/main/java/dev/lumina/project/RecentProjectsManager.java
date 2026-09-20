package dev.lumina.project;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.lumina.git.GitService;
import dev.lumina.util.Settings;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Dynamically manages the user's recent projects list matching IntelliJ IDEA:
 * <ul>
 *   <li>Persists recent project paths and timestamps to {@code ~/.lumina/recent-projects.json}.</li>
 *   <li>Automatically discovers and imports existing recent projects from JetBrains/IntelliJ
 *       installations on the local system ({@code recentProjects.xml}).</li>
 *   <li>Detects project name collisions (e.g. multiple {@code DBNavigator} directories) and
 *       disambiguates them with home-relative paths (e.g. {@code ~/projects/others/DBNavigator}).</li>
 *   <li>Dynamically extracts the current Git branch (e.g. {@code [master]}) for repository projects.</li>
 *   <li>Provides clean labels for unique project names (e.g. {@code lumina}, {@code NexaCommerce}).</li>
 * </ul>
 */
public class RecentProjectsManager {

    public record RecentProject(String path, String name, long lastOpenedTimestamp) {}

    private static final Path DEFAULT_STORAGE = Path.of(
            System.getProperty("user.home"), ".lumina", "recent-projects.json");

    private static final RecentProjectsManager INSTANCE = new RecentProjectsManager(DEFAULT_STORAGE, true);

    private final Path storageFile;
    private final boolean discoverFromSystem;
    private final List<RecentProject> entries = new CopyOnWriteArrayList<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static RecentProjectsManager getInstance() {
        return INSTANCE;
    }

    /** Package-private constructor for custom storage paths in unit testing. */
    RecentProjectsManager(Path storageFile) {
        this(storageFile, true);
    }

    RecentProjectsManager(Path storageFile, boolean discoverFromSystem) {
        this.storageFile = storageFile;
        this.discoverFromSystem = discoverFromSystem;
        load();
    }

    /**
     * Records that a project was opened, placing it at the top of the recent projects list.
     */
    public synchronized void recordProjectOpened(Path path) {
        if (path == null) return;
        Path abs = path.toAbsolutePath().normalize();
        String pathStr = abs.toString();
        String name = abs.getFileName() != null ? abs.getFileName().toString() : pathStr;
        long now = System.currentTimeMillis();

        entries.removeIf(p -> p.path().equals(pathStr));
        entries.add(0, new RecentProject(pathStr, name, now));
        save();
    }

    /**
     * Removes a project from the recent projects list.
     */
    public synchronized void removeProject(String path) {
        if (path == null) return;
        Path norm = Path.of(path).toAbsolutePath().normalize();
        entries.removeIf(p -> {
            try {
                return Path.of(p.path()).toAbsolutePath().normalize().equals(norm);
            } catch (Exception e) {
                return p.path().equals(path);
            }
        });
        save();
    }

    /**
     * Clears all recent projects.
     */
    public synchronized void clear() {
        entries.clear();
        save();
    }

    /**
     * Returns an unmodifiable copy of all existing recent projects, sorted by
     * most recently opened first. Projects whose directories no longer exist on disk
     * are pruned.
     */
    public synchronized List<RecentProject> getRecentProjects() {
        // Filter out non-existent directories on disk
        List<RecentProject> valid = entries.stream()
                .filter(p -> {
                    try {
                        return Files.isDirectory(Path.of(p.path()));
                    } catch (Exception e) {
                        return false;
                    }
                })
                .sorted(Comparator.comparingLong(RecentProject::lastOpenedTimestamp).reversed())
                .collect(Collectors.toList());

        if (valid.size() != entries.size()) {
            entries.clear();
            entries.addAll(valid);
            save();
        }
        return Collections.unmodifiableList(valid);
    }

    /**
     * Formats the menu display label for a recent project matching IntelliJ IDEA:
     * <ul>
     *   <li>If the folder name is unique among all recent projects, displays the folder name (e.g. {@code lumina}).</li>
     *   <li>If multiple projects share the same folder name, disambiguates using the home-relative path
     *       (e.g. {@code ~/projects/others/DBNavigator}).</li>
     *   <li>If the project is a Git repository, appends the active branch in brackets (e.g. {@code  [master]}).</li>
     * </ul>
     */
    public String getDisplayLabel(RecentProject project, List<RecentProject> allProjects) {
        if (project == null) return "";
        String baseName = getBaseName(project.path());
        long duplicateCount = allProjects.stream()
                .filter(p -> getBaseName(p.path()).equalsIgnoreCase(baseName))
                .count();

        String branch = getGitBranch(Path.of(project.path()));
        String branchSuffix = (branch != null && !branch.isBlank()) ? " [" + branch + "]" : "";

        if (duplicateCount > 1 || baseName.isBlank()) {
            String homeRel = toHomeRelativePath(project.path());
            return homeRel + branchSuffix;
        } else {
            return baseName;
        }
    }

    public static String getBaseName(String pathStr) {
        if (pathStr == null || pathStr.isBlank()) return "";
        try {
            Path p = Path.of(pathStr).toAbsolutePath().normalize();
            Path fn = p.getFileName();
            return fn != null ? fn.toString() : pathStr;
        } catch (Exception e) {
            return pathStr;
        }
    }

    public static String toHomeRelativePath(String pathStr) {
        if (pathStr == null) return "";
        String userHome = System.getProperty("user.home");
        if (userHome != null && pathStr.startsWith(userHome)) {
            return "~" + pathStr.substring(userHome.length());
        }
        return pathStr;
    }

    /**
     * Dynamically reads the current Git branch of a project directory without external process lag.
     */
    public static String getGitBranch(Path projectDir) {
        if (projectDir == null) return null;
        try {
            Path gitDir = projectDir.resolve(".git");
            if (!Files.exists(gitDir)) return null;

            Path head = Files.isDirectory(gitDir) ? gitDir.resolve("HEAD") : null;
            if (head != null && Files.isRegularFile(head)) {
                try (BufferedReader r = Files.newBufferedReader(head, StandardCharsets.UTF_8)) {
                    String line = r.readLine();
                    if (line != null) {
                        line = line.trim();
                        if (line.startsWith("ref: refs/heads/")) {
                            return line.substring("ref: refs/heads/".length()).trim();
                        } else if (!line.isBlank()) {
                            // Detached HEAD or commit hash
                            return line.substring(0, Math.min(7, line.length()));
                        }
                    }
                }
            }
            // Fallback to GitService if available
            return GitService.currentBranch(projectDir);
        } catch (Exception ignored) {
            return null;
        }
    }

    // -------------------------------------------------------- persistence & import

    private synchronized void load() {
        entries.clear();
        Map<String, RecentProject> projectMap = new LinkedHashMap<>();

        // 1. Load Lumina's own saved recent projects
        if (Files.isRegularFile(storageFile)) {
            try (Reader r = Files.newBufferedReader(storageFile, StandardCharsets.UTF_8)) {
                Type type = new TypeToken<List<RecentProject>>() {}.getType();
                List<RecentProject> loaded = gson.fromJson(r, type);
                if (loaded != null) {
                    for (RecentProject p : loaded) {
                        if (p != null && p.path() != null) {
                            Path norm = Path.of(p.path()).toAbsolutePath().normalize();
                            if (Files.isDirectory(norm)) {
                                projectMap.put(norm.toString(),
                                        new RecentProject(norm.toString(), p.name(), p.lastOpenedTimestamp()));
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // 2. Discover and import from IntelliJ IDEA on the system if available
        if (discoverFromSystem) {
            discoverIntelliJRecentProjects(projectMap);

            // 3. Ensure lastProject from Settings is also included
            try {
                String last = Settings.get(Settings.LAST_PROJECT);
                if (last != null && !last.isBlank()) {
                    Path norm = Path.of(last).toAbsolutePath().normalize();
                    if (Files.isDirectory(norm)) {
                        RecentProject existing = projectMap.get(norm.toString());
                        long ts = (existing != null) ? Math.max(existing.lastOpenedTimestamp(), System.currentTimeMillis())
                                : System.currentTimeMillis();
                        String name = norm.getFileName() != null ? norm.getFileName().toString() : norm.toString();
                        projectMap.put(norm.toString(), new RecentProject(norm.toString(), name, ts));
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // Sort by timestamp descending
        List<RecentProject> sorted = new ArrayList<>(projectMap.values());
        sorted.sort(Comparator.comparingLong(RecentProject::lastOpenedTimestamp).reversed());
        entries.addAll(sorted);
    }

    private synchronized void save() {
        try {
            if (storageFile.getParent() != null) {
                Files.createDirectories(storageFile.getParent());
            }
            try (Writer w = Files.newBufferedWriter(storageFile, StandardCharsets.UTF_8)) {
                gson.toJson(entries, w);
            }
        } catch (Exception ignored) {
            // Best-effort persistence
        }
    }

    /**
     * Scans known JetBrains configuration directories for {@code options/recentProjects.xml}
     * and merges valid projects into the map.
     */
    void discoverIntelliJRecentProjects(Map<String, RecentProject> projectMap) {
        List<Path> xmlFiles = findIntelliJRecentProjectsXmlFiles();
        for (Path xmlFile : xmlFiles) {
            parseRecentProjectsXml(xmlFile, projectMap);
        }
    }

    List<Path> findIntelliJRecentProjectsXmlFiles() {
        List<Path> candidates = new ArrayList<>();
        String userHome = System.getProperty("user.home");
        if (userHome == null) return candidates;

        List<Path> searchRoots = new ArrayList<>();
        // Linux
        searchRoots.add(Path.of(userHome, ".config", "JetBrains"));
        // macOS
        searchRoots.add(Path.of(userHome, "Library", "Application Support", "JetBrains"));
        // Windows
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            searchRoots.add(Path.of(appData, "JetBrains"));
        }

        for (Path root : searchRoots) {
            if (Files.isDirectory(root)) {
                try (Stream<Path> dirs = Files.list(root)) {
                    dirs.filter(Files::isDirectory)
                            .sorted(Comparator.comparing(Path::toString).reversed()) // latest version first
                            .forEach(dir -> {
                                Path xml = dir.resolve("options").resolve("recentProjects.xml");
                                if (Files.isRegularFile(xml)) {
                                    candidates.add(xml);
                                }
                            });
                } catch (Exception ignored) {
                }
            }
        }
        return candidates;
    }

    /**
     * Parses an IntelliJ {@code recentProjects.xml} file and extracts valid project entries.
     */
    void parseRecentProjectsXml(Path xmlFile, Map<String, RecentProject> projectMap) {
        if (!Files.isRegularFile(xmlFile)) return;
        String userHome = System.getProperty("user.home");

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Secure XML parsing
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile.toFile());

            NodeList entryNodes = doc.getElementsByTagName("entry");
            for (int i = 0; i < entryNodes.getLength(); i++) {
                if (entryNodes.item(i) instanceof Element entryEl) {
                    String rawKey = entryEl.getAttribute("key");
                    if (rawKey == null || rawKey.isBlank()) continue;

                    String pathStr = rawKey.replace("$USER_HOME$", userHome);
                    Path path;
                    try {
                        path = Path.of(pathStr).toAbsolutePath().normalize();
                    } catch (Exception e) {
                        continue;
                    }

                    if (!Files.isDirectory(path)) continue;

                    long actTimestamp = 0;
                    long openTimestamp = 0;
                    NodeList optNodes = entryEl.getElementsByTagName("option");
                    for (int j = 0; j < optNodes.getLength(); j++) {
                        if (optNodes.item(j) instanceof Element optEl) {
                            String optName = optEl.getAttribute("name");
                            String optVal = optEl.getAttribute("value");
                            if ("activationTimestamp".equals(optName)) {
                                try { actTimestamp = Long.parseLong(optVal); } catch (Exception ignored) {}
                            } else if ("projectOpenTimestamp".equals(optName)) {
                                try { openTimestamp = Long.parseLong(optVal); } catch (Exception ignored) {}
                            }
                        }
                    }

                    long timestamp = Math.max(actTimestamp, openTimestamp);
                    if (timestamp <= 0) {
                        timestamp = Files.getLastModifiedTime(path).toMillis();
                    }

                    String name = path.getFileName() != null ? path.getFileName().toString() : pathStr;
                    RecentProject existing = projectMap.get(path.toString());
                    if (existing == null || existing.lastOpenedTimestamp() < timestamp) {
                        projectMap.put(path.toString(), new RecentProject(path.toString(), name, timestamp));
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}
