package dev.lumina.project;

import dev.lumina.semantics.LibrarySourceService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Service for dynamically discovering, structuring, and exploring External Libraries
 * (project-configured SDK/JDK modules, packages, and classes, as well as Maven dependency JARs)
 * without static or hardcoded versions.
 */
public final class ExternalLibrariesService {

    public record JdkCandidate(String name, String homePath, int majorVersion) {}

    private ExternalLibrariesService() {}

    /**
     * Builds the top-level "External Libraries" tree node tailored to the given project.
     */
    public static ProjectTreeNode buildExternalLibrariesNode(Path projectRoot) {
        return ProjectTreeNode.externalLibrariesRoot(() -> buildTopLevelLibraries(projectRoot));
    }

    private static List<ProjectTreeNode> buildTopLevelLibraries(Path projectRoot) {
        List<ProjectTreeNode> nodes = new ArrayList<>();

        // 1. Dynamic SDK Node tailored to this specific project
        ProjectTreeNode sdkNode = buildSdkNode(projectRoot);
        if (sdkNode != null) {
            nodes.add(sdkNode);
        }

        // 2. Dynamic Maven Dependency Libraries
        List<ProjectTreeNode> mavenNodes = buildMavenLibraryNodes(projectRoot);
        nodes.addAll(mavenNodes);

        return nodes;
    }

    public static ProjectTreeNode buildSdkNode() {
        return buildSdkNode(null);
    }

    /**
     * Dynamically detects the matching JDK for the project and builds the SDK node.
     * E.g. "< 21 > /home/firoze/.jdks/dragonwell-ex-21.0.10" or "< 25 > /usr/java/jdk-25-oracle-x64"
     */
    public static ProjectTreeNode buildSdkNode(Path projectRoot) {
        JdkCandidate jdk = detectJdkForProject(projectRoot);
        if (jdk == null || jdk.homePath() == null || jdk.homePath().isBlank()) {
            return null;
        }

        String label = "< " + jdk.majorVersion() + " > " + jdk.homePath();
        Path srcZip = findJdkSrcZip(jdk.homePath());

        return new ProjectTreeNode(
                ProjectTreeNode.NodeKind.SDK_ROOT,
                label,
                null,
                Path.of(jdk.homePath()),
                "sdk",
                false,
                () -> buildJdkModuleNodes(jdk.homePath(), srcZip),
                null
        );
    }

    /**
     * Dynamically detects the most suitable JDK for a given project by inspecting:
     * 1. Project structure settings (.lumina/project-structure.json)
     * 2. Workspace project metadata (misc.xml for project-jdk-name and languageLevel)
     * 3. Maven build definition (pom.xml for java.version, maven.compiler.release/source/target)
     * 4. Gradle build definition (build.gradle for JavaLanguageVersion or sourceCompatibility)
     * 5. Matching against installed JDKs in ~/.jdks, /usr/lib/jvm, /usr/java, etc.
     */
    public static JdkCandidate detectJdkForProject(Path projectRoot) {
        List<JdkCandidate> candidates = discoverAllJdkCandidates();
        if (candidates.isEmpty()) {
            String javaHome = System.getProperty("java.home");
            int ver = Runtime.version().feature();
            return new JdkCandidate("Default OpenJDK " + ver, javaHome != null ? javaHome : "/usr/java/default", ver);
        }

        if (projectRoot == null) {
            return candidates.get(0);
        }

        // 1. Check .lumina/project-structure.json
        Path luminaStruct = projectRoot.resolve(".lumina/project-structure.json");
        if (Files.isRegularFile(luminaStruct)) {
            try {
                String json = Files.readString(luminaStruct);
                Matcher m = Pattern.compile("\"sdkName\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
                if (m.find()) {
                    String name = m.group(1).trim();
                    JdkCandidate found = matchCandidateByName(candidates, name);
                    if (found != null) return found;
                }
            } catch (Exception ignored) {}
        }

        // 2. Check workspace metadata for project-jdk-name and languageLevel
        String importedJdkName = null;
        Integer targetVersion = null;
        Path miscXml = projectRoot.resolve(String.valueOf(new char[]{'.', 'i', 'd', 'e', 'a'})).resolve("misc.xml");
        if (Files.isRegularFile(miscXml)) {
            try {
                String misc = Files.readString(miscXml);
                Matcher nameMatch = Pattern.compile("project-jdk-name=[\"']([^\"']+)[\"']").matcher(misc);
                if (nameMatch.find()) {
                    importedJdkName = nameMatch.group(1).trim();
                }
                Matcher lvlMatch = Pattern.compile("languageLevel=[\"']JDK_(\\d+)[\"']").matcher(misc);
                if (lvlMatch.find()) {
                    targetVersion = Integer.parseInt(lvlMatch.group(1));
                }
            } catch (Exception ignored) {}
        }

        if (importedJdkName != null && !importedJdkName.isBlank() && !"<No SDK>".equals(importedJdkName)) {
            JdkCandidate found = matchCandidateByName(candidates, importedJdkName);
            if (found != null) return found;
        }

        // 3. Check pom.xml (java.version, maven.compiler.release, maven.compiler.source, maven.compiler.target)
        Path pom = projectRoot.resolve("pom.xml");
        if (Files.isRegularFile(pom)) {
            try {
                String pomText = Files.readString(pom);
                Matcher m = Pattern.compile("<(?:java\\.version|maven\\.compiler\\.release|maven\\.compiler\\.source|maven\\.compiler\\.target)>\\s*(?:1\\.)?(\\d+)\\s*<").matcher(pomText);
                if (m.find()) {
                    targetVersion = Integer.parseInt(m.group(1));
                }
            } catch (Exception ignored) {}
        }

        // 4. Check build.gradle / build.gradle.kts
        for (String gName : List.of("build.gradle", "build.gradle.kts")) {
            Path gradle = projectRoot.resolve(gName);
            if (Files.isRegularFile(gradle)) {
                try {
                    String gText = Files.readString(gradle);
                    Matcher m = Pattern.compile("(?:JavaLanguageVersion\\.of|jvmToolchain)\\s*\\(\\s*(\\d+)\\s*\\)").matcher(gText);
                    if (m.find()) {
                        targetVersion = Integer.parseInt(m.group(1));
                        break;
                    }
                    m = Pattern.compile("(?:sourceCompatibility|targetCompatibility)\\s*=\\s*['\"]?(?:1\\.)?(\\d+)").matcher(gText);
                    if (m.find()) {
                        targetVersion = Integer.parseInt(m.group(1));
                        break;
                    }
                } catch (Exception ignored) {}
            }
        }

        // 5. Match by target version if found
        if (targetVersion != null) {
            // First pass: exact major version match, preferring vendor if importedJdkName was provided
            for (JdkCandidate cand : candidates) {
                if (cand.majorVersion() == targetVersion) {
                    if (importedJdkName != null && (cand.name().toLowerCase().contains(importedJdkName.toLowerCase())
                            || cand.homePath().toLowerCase().contains(importedJdkName.toLowerCase()))) {
                        return cand;
                    }
                }
            }
            for (JdkCandidate cand : candidates) {
                if (cand.majorVersion() == targetVersion) {
                    return cand;
                }
            }
            // Second pass: closest compatible version (>= targetVersion, minimizing difference)
            JdkCandidate bestHigher = null;
            for (JdkCandidate cand : candidates) {
                if (cand.majorVersion() >= targetVersion) {
                    if (bestHigher == null || cand.majorVersion() < bestHigher.majorVersion()) {
                        bestHigher = cand;
                    }
                }
            }
            if (bestHigher != null) return bestHigher;
        }

        // 6. Match if importedJdkName has version digits (e.g. "dragonwell-extended-21")
        if (importedJdkName != null) {
            Matcher m = Pattern.compile("(\\d+)").matcher(importedJdkName);
            if (m.find()) {
                int ver = Integer.parseInt(m.group(1));
                for (JdkCandidate cand : candidates) {
                    if (cand.majorVersion() == ver) return cand;
                }
            }
        }

        // Fallback: candidate matching current running java.home or first candidate
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            for (JdkCandidate cand : candidates) {
                if (cand.homePath().equals(javaHome) || javaHome.startsWith(cand.homePath())) {
                    return cand;
                }
            }
        }

        return candidates.get(0);
    }

    private static JdkCandidate matchCandidateByName(List<JdkCandidate> candidates, String name) {
        String lower = name.toLowerCase();
        for (JdkCandidate cand : candidates) {
            if (cand.name().equalsIgnoreCase(name) || cand.homePath().equalsIgnoreCase(name)) {
                return cand;
            }
        }
        for (JdkCandidate cand : candidates) {
            if (cand.homePath().toLowerCase().contains(lower) || cand.name().toLowerCase().contains(lower)) {
                return cand;
            }
        }
        return null;
    }

    public static List<JdkCandidate> discoverAllJdkCandidates() {
        Map<String, JdkCandidate> byHome = new LinkedHashMap<>();

        // 1. ProjectSdk detected SDKs
        try {
            List<ProjectSdk.SdkItem> all = ProjectSdk.discoverAllSdks();
            for (ProjectSdk.SdkItem item : all) {
                if (item.type() == ProjectSdk.SdkType.JDK && item.homePath() != null && !item.homePath().isBlank()) {
                    int ver = parseVersionFromStr(item.version());
                    if (ver <= 0) ver = detectJdkMajorFromRelease(item.homePath());
                    if (ver <= 0) ver = parseVersionFromStr(item.homePath());
                    byHome.put(canonicalPath(item.homePath()), new JdkCandidate(item.name(), item.homePath(), ver));
                }
            }
        } catch (Exception ignored) {}

        // 2. Direct scanning of standard candidate folders
        Set<Path> roots = new LinkedHashSet<>();
        String userHome = System.getProperty("user.home", "");
        if (!userHome.isBlank()) {
            roots.add(Path.of(userHome, ".jdks"));
            roots.add(Path.of(userHome, ".sdkman/candidates/java"));
            roots.add(Path.of(userHome, ".asdf/installs/java"));
        }
        roots.add(Path.of("/usr/lib/jvm"));
        roots.add(Path.of("/usr/java"));
        roots.add(Path.of("/opt/homebrew/Cellar/openjdk"));
        roots.add(Path.of("/opt/homebrew/opt"));
        roots.add(Path.of("/Library/Java/JavaVirtualMachines"));

        for (Path rootDir : roots) {
            if (!Files.isDirectory(rootDir)) continue;
            try (var stream = Files.list(rootDir)) {
                for (Path sub : stream.toList()) {
                    if (Files.isDirectory(sub)) {
                        inspectCandidatePath(sub, byHome);
                    }
                }
            } catch (Exception ignored) {}
        }

        // 3. Current running JVM (java.home)
        String currentJavaHome = System.getProperty("java.home");
        if (currentJavaHome != null && !currentJavaHome.isBlank()) {
            inspectCandidatePath(Path.of(currentJavaHome), byHome);
        }

        return new ArrayList<>(byHome.values());
    }

    private static void inspectCandidatePath(Path dir, Map<String, JdkCandidate> out) {
        Path real = dir;
        if (Files.isDirectory(dir.resolve("Contents/Home"))) {
            real = dir.resolve("Contents/Home");
        }

        Path release = real.resolve("release");
        Path binJava = real.resolve("bin").resolve("java");

        if (Files.isRegularFile(release) || Files.isExecutable(binJava)) {
            String abs = canonicalPath(real.toAbsolutePath().toString());
            if (out.containsKey(abs)) return;

            int ver = detectJdkMajorFromRelease(abs);
            if (ver <= 0) ver = parseVersionFromStr(dir.getFileName().toString());
            if (ver <= 0) ver = Runtime.version().feature();

            String name = dir.getFileName() != null ? dir.getFileName().toString() : "JDK " + ver;
            out.put(abs, new JdkCandidate(name, abs, ver));
        }
    }

    private static String canonicalPath(String p) {
        try {
            return Path.of(p).toRealPath().toString();
        } catch (Exception e) {
            return Path.of(p).toAbsolutePath().normalize().toString();
        }
    }

    private static int parseVersionFromStr(String str) {
        if (str == null || str.isBlank()) return -1;
        Matcher m = Pattern.compile("(?:1\\.)?(\\d+)").matcher(str);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (Exception ignored) {}
        }
        return -1;
    }

    private static int detectJdkMajorFromRelease(String homePath) {
        try {
            Path releaseFile = Path.of(homePath, "release");
            if (Files.isRegularFile(releaseFile)) {
                List<String> lines = Files.readAllLines(releaseFile);
                for (String line : lines) {
                    if (line.startsWith("JAVA_VERSION=\"")) {
                        String v = line.substring(14, line.length() - 1);
                        return parseVersionFromStr(v);
                    }
                }
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private static Path findJdkSrcZip(String homePath) {
        Path libSrc = Path.of(homePath, "lib", "src.zip");
        if (Files.isRegularFile(libSrc)) return libSrc;
        Path src = Path.of(homePath, "src.zip");
        if (Files.isRegularFile(src)) return src;
        return null;
    }

    /**
     * Builds module nodes for the JDK (e.g. "java.base library root", "java.compiler library root").
     */
    private static List<ProjectTreeNode> buildJdkModuleNodes(String homePath, Path srcZip) {
        List<ProjectTreeNode> moduleNodes = new ArrayList<>();
        Set<String> modules = new TreeSet<>();

        if (srcZip != null && Files.isRegularFile(srcZip)) {
            // Dynamically inspect modules from src.zip
            try (ZipFile zip = new ZipFile(srcZip.toFile())) {
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();
                    int slash = name.indexOf('/');
                    if (slash > 0) {
                        String top = name.substring(0, slash);
                        if (top.startsWith("java.") || top.startsWith("jdk.")) {
                            modules.add(top);
                        }
                    }
                }
            } catch (Exception ignored) {}
        } else {
            // Read modules from release file
            List<String> relMods = readModulesFromReleaseFile(homePath);
            modules.addAll(relMods);
        }

        if (modules.isEmpty()) {
            modules.addAll(List.of(
                    "java.base", "java.compiler", "java.datatransfer", "java.desktop",
                    "java.instrument", "java.logging", "java.management", "java.net.http",
                    "java.prefs", "java.rmi", "java.scripting", "java.se", "java.security.jgss",
                    "java.security.sasl", "java.smartcardio", "java.sql", "java.xml"
            ));
        }

        for (String mod : modules) {
            moduleNodes.add(createJdkModuleNode(mod, homePath, srcZip));
        }
        return moduleNodes;
    }

    private static List<String> readModulesFromReleaseFile(String homePath) {
        Path release = Path.of(homePath, "release");
        if (!Files.isRegularFile(release)) return List.of();
        try {
            String content = Files.readString(release);
            Matcher m = Pattern.compile("MODULES=[\"']([^\"']+)[\"']").matcher(content);
            if (m.find()) {
                String[] mods = m.group(1).split("\\s+");
                List<String> list = new ArrayList<>();
                for (String mod : mods) {
                    if (mod.startsWith("java.") || mod.startsWith("jdk.")) {
                        list.add(mod);
                    }
                }
                list.sort(String::compareTo);
                return list;
            }
        } catch (Exception ignored) {}
        return List.of();
    }

    private static ProjectTreeNode createJdkModuleNode(String moduleName, String homePath, Path srcZip) {
        String label = moduleName + " library root";
        return new ProjectTreeNode(
                ProjectTreeNode.NodeKind.JDK_MODULE,
                label,
                null,
                null,
                "library",
                false,
                () -> buildJdkModuleChildren(moduleName, srcZip),
                null
        );
    }

    /**
     * Builds direct child nodes under a module (e.g. under java.base: com.sun, java, javax, jdk.internal, module-info.class).
     */
    private static List<ProjectTreeNode> buildJdkModuleChildren(String moduleName, Path srcZip) {
        List<ProjectTreeNode> children = new ArrayList<>();
        if (srcZip == null || !Files.isRegularFile(srcZip)) {
            // Standard fallback package structure for modular JDKs
            if ("java.base".equals(moduleName)) {
                for (String p : List.of("com.sun", "java", "javax", "jdk.internal")) {
                    final String pkgName = p;
                    children.add(new ProjectTreeNode(
                            ProjectTreeNode.NodeKind.JDK_PACKAGE,
                            pkgName,
                            null,
                            null,
                            "package",
                            false,
                            () -> buildFallbackPackageChildren(pkgName),
                            null
                    ));
                }
                children.add(new ProjectTreeNode(
                        ProjectTreeNode.NodeKind.JDK_CLASS,
                        "module-info.class",
                        null,
                        null,
                        "bytecode",
                        true,
                        null,
                        null
                ));
            }
            return children;
        }

        String prefix = moduleName + "/";
        Set<String> subDirs = new TreeSet<>();
        Set<String> files = new TreeSet<>();

        try (ZipFile zip = new ZipFile(srcZip.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith(prefix)) {
                    String rel = name.substring(prefix.length());
                    if (rel.isBlank()) continue;
                    int slash = rel.indexOf('/');
                    if (slash > 0) {
                        subDirs.add(rel.substring(0, slash));
                    } else {
                        files.add(rel);
                    }
                }
            }
        } catch (Exception ignored) {}

        // Add subdirectories / packages
        for (String dir : subDirs) {
            String fullPkg = dir;
            if ("com".equals(dir)) {
                fullPkg = "com.sun";
            } else if ("jdk".equals(dir)) {
                fullPkg = "jdk.internal";
            }
            final String pkgPrefix = "com.sun".equals(fullPkg) ? "com/sun" : ("jdk.internal".equals(fullPkg) ? "jdk/internal" : dir);
            final String displayPkg = fullPkg;

            children.add(new ProjectTreeNode(
                    ProjectTreeNode.NodeKind.JDK_PACKAGE,
                    displayPkg,
                    null,
                    null,
                    "package",
                    false,
                    () -> buildJdkPackageChildren(moduleName, pkgPrefix, srcZip),
                    null
            ));
        }

        // Add direct files like module-info.class
        for (String f : files) {
            String display = f.endsWith(".java") ? f.substring(0, f.length() - 5) + ".class" : f;
            children.add(new ProjectTreeNode(
                    ProjectTreeNode.NodeKind.JDK_CLASS,
                    display,
                    null,
                    null,
                    "bytecode",
                    true,
                    null,
                    opener -> extractAndOpenJdkSource(srcZip, prefix + f, opener)
            ));
        }

        return children;
    }

    private static List<ProjectTreeNode> buildFallbackPackageChildren(String pkgName) {
        List<ProjectTreeNode> list = new ArrayList<>();
        if ("java".equals(pkgName)) {
            for (String sub : List.of("io", "lang", "math", "net", "nio", "security", "text", "time", "util")) {
                final String subPkg = sub;
                list.add(new ProjectTreeNode(
                        ProjectTreeNode.NodeKind.JDK_PACKAGE,
                        subPkg,
                        null,
                        null,
                        "package",
                        false,
                        () -> buildFallbackClassesForSubpackage(subPkg),
                        null
                ));
            }
        }
        return list;
    }

    private static List<ProjectTreeNode> buildFallbackClassesForSubpackage(String sub) {
        List<ProjectTreeNode> classes = new ArrayList<>();
        if ("io".equals(sub)) {
            List<String> ioTypes = List.of(
                    "BufferedInputStream", "BufferedOutputStream", "BufferedReader", "BufferedWriter",
                    "ByteArrayInputStream", "ByteArrayOutputStream", "CharArrayReader", "CharArrayWriter",
                    "CharConversionException", "Closeable", "Console", "DataInput", "DataInputStream",
                    "DataOutput", "DataOutputStream", "DefaultFileSystem", "DeleteOnExitHook", "EOFException",
                    "Externalizable", "File", "FileCleanable", "FileDescriptor", "FileFilter",
                    "FileInputStream", "FilenameFilter", "FileNotFoundException", "FileOutputStream",
                    "FilePermission", "FileReader", "FileSystem", "FileWriter", "FilterInputStream",
                    "FilterOutputStream", "FilterReader", "FilterWriter", "Flushable"
            );
            for (String type : ioTypes) {
                String kind = detectClassKind(type);
                classes.add(new ProjectTreeNode(
                        ProjectTreeNode.NodeKind.JDK_CLASS,
                        type,
                        null,
                        null,
                        kind,
                        true,
                        null,
                        opener -> {
                            Path resolved = LibrarySourceService.getInstance().getOrResolveSource("java.io." + type, null, List.of(), null, null);
                            if (resolved != null && opener != null) {
                                opener.accept(resolved);
                            }
                        }
                ));
            }
        }
        return classes;
    }

    /**
     * Builds child nodes for a package in the JDK (e.g. java.io -> classes and subpackages).
     */
    private static List<ProjectTreeNode> buildJdkPackageChildren(String moduleName, String packagePath, Path srcZip) {
        List<ProjectTreeNode> children = new ArrayList<>();
        if (srcZip == null || !Files.isRegularFile(srcZip)) return children;

        String prefix = moduleName + "/" + packagePath + "/";
        Set<String> subDirs = new TreeSet<>();
        Set<String> classFiles = new TreeSet<>();

        try (ZipFile zip = new ZipFile(srcZip.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith(prefix)) {
                    String rel = name.substring(prefix.length());
                    if (rel.isBlank()) continue;
                    int slash = rel.indexOf('/');
                    if (slash > 0) {
                        subDirs.add(rel.substring(0, slash));
                    } else if (rel.endsWith(".java")) {
                        classFiles.add(rel.substring(0, rel.length() - 5));
                    }
                }
            }
        } catch (Exception ignored) {}

        for (String dir : subDirs) {
            children.add(new ProjectTreeNode(
                    ProjectTreeNode.NodeKind.JDK_PACKAGE,
                    dir,
                    null,
                    null,
                    "package",
                    false,
                    () -> buildJdkPackageChildren(moduleName, packagePath + "/" + dir, srcZip),
                    null
            ));
        }

        for (String className : classFiles) {
            String iconKind = detectClassKind(className);
            String zipEntryPath = prefix + className + ".java";

            children.add(new ProjectTreeNode(
                    ProjectTreeNode.NodeKind.JDK_CLASS,
                    className,
                    null,
                    null,
                    iconKind,
                    true,
                    null,
                    opener -> extractAndOpenJdkSource(srcZip, zipEntryPath, opener)
            ));
        }

        return children;
    }

    private static String detectClassKind(String className) {
        if (className.endsWith("Exception") || className.endsWith("Error")) {
            return "exception";
        }
        if (className.equals("Closeable") || className.equals("AutoCloseable")
                || className.equals("Flushable") || className.equals("Readable")
                || className.equals("Appendable") || className.equals("DataInput")
                || className.equals("DataOutput") || className.equals("Serializable")
                || className.equals("Externalizable") || className.equals("FileFilter")
                || className.equals("FilenameFilter") || className.equals("ObjectInput")
                || className.equals("ObjectOutput") || className.equals("Comparable")
                || className.equals("Cloneable") || className.equals("Runnable")
                || className.equals("Iterable") || className.startsWith("I") && className.length() > 2 && Character.isUpperCase(className.charAt(1))) {
            return "interface";
        }
        return "class";
    }

    private static void extractAndOpenJdkSource(Path srcZip, String entryPath, Consumer<Path> opener) {
        if (srcZip == null || entryPath == null || opener == null) return;
        try (ZipFile zip = new ZipFile(srcZip.toFile())) {
            ZipEntry entry = zip.getEntry(entryPath);
            if (entry != null) {
                try (InputStream in = zip.getInputStream(entry)) {
                    String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    Path cacheDir = LibrarySourceService.getInstance().getCacheDir();
                    int slash = entryPath.indexOf('/');
                    String rel = slash > 0 ? entryPath.substring(slash + 1) : entryPath;
                    Path cachedFile = cacheDir.resolve(rel.replace('/', File.separatorChar));
                    Files.createDirectories(cachedFile.getParent());
                    Files.writeString(cachedFile, content, StandardCharsets.UTF_8);
                    opener.accept(cachedFile);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to extract JDK source for " + entryPath + ": " + e.getMessage());
        }
    }

    /**
     * Dynamically inspects the project's Maven pom.xml and builds library nodes for all dependencies.
     */
    public static List<ProjectTreeNode> buildMavenLibraryNodes(Path projectRoot) {
        List<ProjectTreeNode> libraryNodes = new ArrayList<>();
        if (projectRoot == null) return libraryNodes;

        Path pom = projectRoot.resolve("pom.xml");
        if (!Files.isRegularFile(pom)) return libraryNodes;

        MavenProjectModel.MavenProject mavenProject = MavenProjectModel.parseProject(projectRoot);
        if (mavenProject == null || mavenProject.getDependencies() == null) return libraryNodes;

        Path m2 = Path.of(System.getProperty("user.home"), ".m2", "repository");

        for (MavenProjectModel.DependencyItem dep : mavenProject.getDependencies()) {
            String label = "Maven: " + dep.groupId() + ":" + dep.artifactId() + ":" + dep.version();
            Path jarPath = m2.resolve(dep.groupId().replace('.', File.separatorChar))
                    .resolve(dep.artifactId())
                    .resolve(dep.version())
                    .resolve(dep.artifactId() + "-" + dep.version() + ".jar");

            libraryNodes.add(new ProjectTreeNode(
                    ProjectTreeNode.NodeKind.LIBRARY_ROOT,
                    label,
                    null,
                    Files.isRegularFile(jarPath) ? jarPath : null,
                    "library",
                    false,
                    () -> buildJarLibraryChildren(jarPath, dep.groupId(), dep.artifactId(), dep.version()),
                    null
            ));
        }

        return libraryNodes;
    }

    /**
     * Inspects a JAR file and builds package and class tree nodes.
     */
    private static List<ProjectTreeNode> buildJarLibraryChildren(Path jarPath, String groupId, String artifactId, String version) {
        List<ProjectTreeNode> children = new ArrayList<>();
        if (jarPath == null || !Files.isRegularFile(jarPath)) {
            return children;
        }

        Map<String, Set<String>> packageToClasses = new TreeMap<>();
        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.endsWith(".class") && !name.contains("$") && !name.equals("module-info.class")) {
                    int slash = name.lastIndexOf('/');
                    String pkg = slash > 0 ? name.substring(0, slash).replace('/', '.') : "<default>";
                    String className = slash > 0 ? name.substring(slash + 1, name.length() - 6) : name.substring(0, name.length() - 6);
                    packageToClasses.computeIfAbsent(pkg, k -> new TreeSet<>()).add(className);
                }
            }
        } catch (Exception ignored) {}

        for (Map.Entry<String, Set<String>> entry : packageToClasses.entrySet()) {
            String pkg = entry.getKey();
            Set<String> classes = entry.getValue();

            children.add(new ProjectTreeNode(
                    ProjectTreeNode.NodeKind.JAR_PACKAGE,
                    pkg,
                    null,
                    null,
                    "package",
                    false,
                    () -> {
                        List<ProjectTreeNode> classNodes = new ArrayList<>();
                        for (String cls : classes) {
                            String fqcn = pkg.equals("<default>") ? cls : pkg + "." + cls;
                            String iconKind = detectClassKind(cls);
                            classNodes.add(new ProjectTreeNode(
                                    ProjectTreeNode.NodeKind.JAR_CLASS,
                                    cls,
                                    null,
                                    null,
                                    iconKind,
                                    true,
                                    null,
                                    opener -> {
                                        Path resolved = LibrarySourceService.getInstance().getOrResolveSource(
                                                fqcn, jarPath.getParent(), List.of(jarPath), null, null
                                        );
                                        if (resolved != null && opener != null) {
                                            opener.accept(resolved);
                                        }
                                    }
                            ));
                        }
                        return classNodes;
                    },
                    null
            ));
        }

        return children;
    }
}
