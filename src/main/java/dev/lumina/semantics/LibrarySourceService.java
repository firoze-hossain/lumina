package dev.lumina.semantics;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Service for locating, downloading, caching, and querying library and JDK sources.
 * Sources are cached persistently in ~/.lumina/cache/sources/ as regular .java files.
 * Navigation to cached files is instant with 0 ms delay and no console noise.
 */
public class LibrarySourceService {

    private static final LibrarySourceService INSTANCE = new LibrarySourceService();

    public static LibrarySourceService getInstance() {
        return INSTANCE;
    }

    private Path cacheDir;
    private final Set<String> negativeCache = ConcurrentHashMap.newKeySet();
    private final Map<String, List<Path>> sourcesJarIndex = new ConcurrentHashMap<>();

    public LibrarySourceService() {
        this(Path.of(System.getProperty("user.home"), ".lumina", "cache", "sources"));
    }

    public LibrarySourceService(Path cacheDir) {
        this.cacheDir = cacheDir;
    }

    public void setCacheDir(Path cacheDir) {
        this.cacheDir = cacheDir;
    }

    public Path getCacheDir() {
        return cacheDir;
    }

    /** Path to cached .java file for a given FQCN. */
    public Path getCachedSourcePath(String fqcn) {
        String topFqcn = getTopLevelFqcn(fqcn);
        return cacheDir.resolve(topFqcn.replace('.', File.separatorChar) + ".java");
    }

    /** Path to cached decompiled .class file if sources are unavailable. */
    public Path getCachedDecompiledPath(String fqcn) {
        String topFqcn = getTopLevelFqcn(fqcn);
        return cacheDir.resolve(topFqcn.replace('.', File.separatorChar) + ".class");
    }

    /** Check if the FQCN is already cached on disk. */
    public boolean isCached(String fqcn) {
        return Files.isRegularFile(getCachedSourcePath(fqcn))
                || Files.isRegularFile(getCachedDecompiledPath(fqcn));
    }

    /** Check if a given file path belongs to the library source cache. */
    public boolean isLibraryCacheFile(Path file) {
        if (file == null || cacheDir == null) return false;
        try {
            return file.toAbsolutePath().startsWith(cacheDir.toAbsolutePath());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the existing cached source/decompiled file, or resolve and cache it.
     * Returns null if unresolvable.
     */
    public Path getOrResolveSource(String fqcn,
                                  Path projectRoot,
                                  List<Path> classpathJars,
                                  String runClasspath,
                                  Consumer<String> statusLogger) {
        if (fqcn == null || fqcn.isBlank()) return null;

        Path cachedJava = getCachedSourcePath(fqcn);
        if (Files.isRegularFile(cachedJava)) {
            return cachedJava;
        }
        Path cachedClass = getCachedDecompiledPath(fqcn);
        if (Files.isRegularFile(cachedClass)) {
            return cachedClass;
        }

        if (statusLogger != null) {
            statusLogger.accept("Resolving " + fqcn + " \u2026");
        }

        String topFqcn = getTopLevelFqcn(fqcn);

        // 1. Check JDK src.zip
        String jdkSource = findJdkSource(topFqcn);
        if (jdkSource != null) {
            return saveToCache(cachedJava, jdkSource);
        }

        // 2. Check project classpath dependency -sources.jar files directly
        String depSource = findSourceInClasspathJars(topFqcn, classpathJars);
        if (depSource != null) {
            return saveToCache(cachedJava, depSource);
        }

        // 3. Check all -sources.jar in Maven repository (~/.m2/repository)
        String m2Source = findSourceInMavenRepo(topFqcn);
        if (m2Source != null) {
            return saveToCache(cachedJava, m2Source);
        }

        // 4. If Maven project, try downloading dependency sources once if not done
        if (projectRoot != null && Files.isRegularFile(projectRoot.resolve("pom.xml"))) {
            Path marker = Path.of(System.getProperty("user.home"), ".lumina",
                    "sources-" + Integer.toHexString(projectRoot.toAbsolutePath().toString().hashCode()) + ".done");
            if (!Files.exists(marker)) {
                if (statusLogger != null) {
                    statusLogger.accept("Downloading dependency sources (one-time per project)\u2026");
                }
                runMavenSourcesDownload(projectRoot);
                try {
                    Files.createDirectories(marker.getParent());
                    Files.writeString(marker, "done");
                } catch (IOException ignored) {}

                // Re-scan repository after download
                m2Source = findSourceInMavenRepo(topFqcn);
                if (m2Source != null) {
                    return saveToCache(cachedJava, m2Source);
                }
            }
        }

        // 5. Fallback: javap bytecode decompilation
        String decompiled = decompileWithJavap(fqcn, runClasspath);
        if (decompiled != null && !decompiled.isBlank()) {
            return saveToCache(cachedClass, decompiled);
        }

        return null;
    }

    /** Save resolved text to disk cache. */
    private Path saveToCache(Path targetFile, String content) {
        try {
            Files.createDirectories(targetFile.getParent());
            Files.writeString(targetFile, content, StandardCharsets.UTF_8);
            return targetFile;
        } catch (IOException e) {
            System.err.println("Failed to cache library source for " + targetFile + ": " + e.getMessage());
            return null;
        }
    }

    /** Extract source for a JDK class from $JAVA_HOME/lib/src.zip. */
    public String findJdkSource(String fqcn) {
        String javaHome = System.getProperty("java.home");
        if (javaHome == null) return null;
        Path srcZip = Path.of(javaHome, "lib", "src.zip");
        if (!Files.isRegularFile(srcZip)) {
            srcZip = Path.of(javaHome, "src.zip");
        }
        if (!Files.isRegularFile(srcZip)) return null;

        String relPath = fqcn.replace('.', '/') + ".java";
        try (ZipFile zip = new ZipFile(srcZip.toFile())) {
            // Check direct entry
            ZipEntry entry = zip.getEntry(relPath);
            if (entry == null) {
                // Check modular prefix (java.base, java.desktop, java.sql, etc.)
                String[] modules = new String[]{"java.base", "java.desktop", "java.sql", "java.logging",
                        "java.xml", "java.compiler", "jdk.compiler", "java.net.http"};
                for (String mod : modules) {
                    entry = zip.getEntry(mod + "/" + relPath);
                    if (entry != null) break;
                }
            }
            if (entry != null) {
                try (InputStream in = zip.getInputStream(entry)) {
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        } catch (IOException ignored) {}
        return null;
    }

    /**
     * Check if any jar on the project classpath has an adjacent -sources.jar.
     * E.g. /path/to/foo-1.0.jar -> /path/to/foo-1.0-sources.jar.
     */
    public String findSourceInClasspathJars(String fqcn, List<Path> classpathJars) {
        if (classpathJars == null || classpathJars.isEmpty()) return null;
        String entryName = fqcn.replace('.', '/') + ".java";

        for (Path jar : classpathJars) {
            if (jar == null || !Files.isRegularFile(jar)) continue;
            String name = jar.getFileName().toString();
            if (!name.endsWith(".jar")) continue;

            Path sourcesJar = null;
            if (name.endsWith("-sources.jar")) {
                sourcesJar = jar;
            } else {
                String srcName = name.substring(0, name.length() - 4) + "-sources.jar";
                Path sibling = jar.resolveSibling(srcName);
                if (Files.isRegularFile(sibling)) {
                    sourcesJar = sibling;
                }
            }

            if (sourcesJar != null) {
                String source = extractEntry(sourcesJar, entryName);
                if (source != null) return source;
            }
        }
        return null;
    }

    /** Search all -sources.jar under ~/.m2/repository. */
    public String findSourceInMavenRepo(String fqcn) {
        String entryName = fqcn.replace('.', '/') + ".java";
        Path m2 = Path.of(System.getProperty("user.home"), ".m2", "repository");
        if (!Files.isDirectory(m2)) return null;

        // Try direct artifact directory first based on package prefix
        String[] parts = fqcn.split("\\.");
        if (parts.length >= 2) {
            Path directDir = m2.resolve(parts[0]).resolve(parts[1]);
            if (Files.isDirectory(directDir)) {
                String s = scanDirForEntry(directDir, entryName);
                if (s != null) return s;
            }
        }

        return scanDirForEntry(m2, entryName);
    }

    private String scanDirForEntry(Path dir, String entryName) {
        try (Stream<Path> walk = Files.walk(dir)) {
            Iterator<Path> it = walk.filter(p -> p.getFileName().toString().endsWith("-sources.jar")).iterator();
            while (it.hasNext()) {
                Path jar = it.next();
                String source = extractEntry(jar, entryName);
                if (source != null) return source;
            }
        } catch (IOException ignored) {}
        return null;
    }

    private String extractEntry(Path jarFile, String entryName) {
        try (ZipFile zip = new ZipFile(jarFile.toFile())) {
            ZipEntry e = zip.getEntry(entryName);
            if (e != null) {
                try (InputStream in = zip.getInputStream(e)) {
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        } catch (IOException ignored) {}
        return null;
    }

    private void runMavenSourcesDownload(Path projectRoot) {
        try {
            String mvn = Path.of(projectRoot.toString(), "mvnw").toFile().exists()
                    ? projectRoot.resolve("mvnw").toString() : "mvn";
            ProcessBuilder pb = new ProcessBuilder(mvn, "-q", "dependency:sources");
            pb.directory(projectRoot.toFile());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor();
        } catch (Exception ignored) {}
    }

    private String decompileWithJavap(String fqcn, String classpath) {
        try {
            boolean isWin = System.getProperty("os.name", "").toLowerCase().contains("win");
            String javap = Path.of(System.getProperty("java.home"), "bin", isWin ? "javap.exe" : "javap").toString();
            List<String> cmd = new ArrayList<>(List.of(javap, "-p", "-protected"));
            if (classpath != null && !classpath.isBlank()) {
                cmd.add("-classpath");
                cmd.add(classpath);
            }
            cmd.add(fqcn);
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            p.waitFor();
            if (out.isBlank() || out.contains("Error:") || out.contains("not found")) {
                return null;
            }
            return "// Decompiled with javap \u2014 read-only\n// " + fqcn + "\n\n" + out;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Find the exact 1-based declaration line for a member/method/type in source code.
     * Uses JavaParser AST for exact pinpointing, with regex fallback.
     */
    public static int findMemberLine(String sourceText, String memberName, int paramCount) {
        if (sourceText == null || sourceText.isBlank()) return 1;
        if (memberName == null || memberName.isBlank()) return 1;

        try {
            CompilationUnit cu = StaticJavaParser.parse(sourceText);

            // If explicitly searching for a type (paramCount == -2)
            if (paramCount == -2) {
                for (ClassOrInterfaceDeclaration cid : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                    if (cid.getNameAsString().equals(memberName)) {
                        return cid.getName().getRange().map(r -> r.begin.line)
                                .orElse(cid.getRange().map(r -> r.begin.line).orElse(1));
                    }
                }
                for (RecordDeclaration rd : cu.findAll(RecordDeclaration.class)) {
                    if (rd.getNameAsString().equals(memberName)) {
                        return rd.getName().getRange().map(r -> r.begin.line).orElse(1);
                    }
                }
                for (EnumDeclaration ed : cu.findAll(EnumDeclaration.class)) {
                    if (ed.getNameAsString().equals(memberName)) {
                        return ed.getName().getRange().map(r -> r.begin.line).orElse(1);
                    }
                }
            }

            // 1. Method declarations
            MethodDeclaration bestMethod = null;
            for (MethodDeclaration m : cu.findAll(MethodDeclaration.class)) {
                if (m.getNameAsString().equals(memberName)) {
                    if (bestMethod == null) bestMethod = m;
                    if (paramCount >= 0 && m.getParameters().size() == paramCount) {
                        bestMethod = m;
                        break;
                    }
                }
            }
            if (bestMethod != null) {
                if (bestMethod.getName().getRange().isPresent()) {
                    return bestMethod.getName().getRange().get().begin.line;
                }
                if (bestMethod.getRange().isPresent()) {
                    return bestMethod.getRange().get().begin.line;
                }
            }

            // 2. Constructor declarations
            if (paramCount != -2) {
                for (ConstructorDeclaration c : cu.findAll(ConstructorDeclaration.class)) {
                    if (c.getNameAsString().equals(memberName)) {
                        if (paramCount < 0 || c.getParameters().size() == paramCount) {
                            if (c.getName().getRange().isPresent()) {
                                return c.getName().getRange().get().begin.line;
                            }
                            if (c.getRange().isPresent()) {
                                return c.getRange().get().begin.line;
                            }
                        }
                    }
                }
            }

            // 3. Field declarations
            for (FieldDeclaration f : cu.findAll(FieldDeclaration.class)) {
                for (VariableDeclarator v : f.getVariables()) {
                    if (v.getNameAsString().equals(memberName)) {
                        if (v.getName().getRange().isPresent()) {
                            return v.getName().getRange().get().begin.line;
                        }
                        if (v.getRange().isPresent()) {
                            return v.getRange().get().begin.line;
                        }
                    }
                }
            }

            // 4. Class or interface declaration
            for (ClassOrInterfaceDeclaration cid : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                if (cid.getNameAsString().equals(memberName)) {
                    if (cid.getName().getRange().isPresent()) {
                        return cid.getName().getRange().get().begin.line;
                    }
                    if (cid.getRange().isPresent()) {
                        return cid.getRange().get().begin.line;
                    }
                }
            }

            // 5. Record / Enum declarations
            for (RecordDeclaration rd : cu.findAll(RecordDeclaration.class)) {
                if (rd.getNameAsString().equals(memberName)) {
                    if (rd.getName().getRange().isPresent()) {
                        return rd.getName().getRange().get().begin.line;
                    }
                }
            }
            for (EnumDeclaration ed : cu.findAll(EnumDeclaration.class)) {
                if (ed.getNameAsString().equals(memberName)) {
                    if (ed.getName().getRange().isPresent()) {
                        return ed.getName().getRange().get().begin.line;
                    }
                }
            }

        } catch (Throwable ignored) {
            // Parse failed (e.g. javap bytecode or partial snippet) -> regex fallback
        }

        // Regex fallback
        return Docs.findMemberLine(sourceText, memberName, paramCount);
    }

    /**
     * Purges the persistent source cache and resets markers.
     */
    public void clearCache() {
        negativeCache.clear();
        sourcesJarIndex.clear();
        if (cacheDir != null && Files.exists(cacheDir)) {
            try (Stream<Path> walk = Files.walk(cacheDir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {}
                });
            } catch (IOException ignored) {}
        }
    }

    /** Trim nested-class segments: com.example.Foo$Bar or Foo.Bar -> com.example.Foo */
    public static String getTopLevelFqcn(String fqcn) {
        if (fqcn == null) return "";
        if (fqcn.contains("$")) {
            return fqcn.substring(0, fqcn.indexOf('$'));
        }
        String[] parts = fqcn.split("\\.");
        StringBuilder qn = new StringBuilder();
        for (String part : parts) {
            if (!qn.isEmpty()) qn.append('.');
            qn.append(part);
            if (!part.isEmpty() && Character.isUpperCase(part.charAt(0))) {
                break;
            }
        }
        return qn.toString();
    }
}
