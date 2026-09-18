package dev.lumina.project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Metadata and dynamic discovery utilities for Maven Archetypes and Catalogs matching IntelliJ IDEA.
 */
public final class MavenArchetypeMetadata {

    private MavenArchetypeMetadata() {}

    public record CatalogEntry(String name, String type, String location, boolean isSystem) {
        public CatalogEntry(String name, String location) {
            this(name, "Custom", location, false);
        }
    }

    public static final List<String> INTERNAL_ARCHETYPES = List.of(
            "org.apache.maven.archetypes:maven-archetype-archetype",
            "org.apache.maven.archetypes:maven-archetype-j2ee-simple",
            "org.apache.maven.archetypes:maven-archetype-plugin",
            "org.apache.maven.archetypes:maven-archetype-plugin-site",
            "org.apache.maven.archetypes:maven-archetype-portlet",
            "org.apache.maven.archetypes:maven-archetype-profiles",
            "org.apache.maven.archetypes:maven-archetype-quickstart",
            "org.apache.maven.archetypes:maven-archetype-site",
            "org.apache.maven.archetypes:maven-archetype-site-simple",
            "org.apache.maven.archetypes:maven-archetype-webapp"
    );

    public static final List<String> MAVEN_CENTRAL_POPULAR_ARCHETYPES = List.of(
            "org.apache.maven.archetypes:maven-archetype-quickstart",
            "org.apache.maven.archetypes:maven-archetype-webapp",
            "io.quarkus:quarkus-archetype",
            "org.jetbrains.kotlin:kotlin-archetype-jvm",
            "net.alchim31.maven:scala-archetype-simple",
            "org.apache.flink:flink-quickstart-java",
            "org.apache.camel.archetypes:camel-archetype-java",
            "org.apache.maven.archetypes:maven-archetype-archetype",
            "org.apache.maven.archetypes:maven-archetype-plugin"
    );

    /**
     * Resolves the default local Maven repository path dynamically.
     */
    public static String resolveDefaultLocalRepo() {
        String m2Repo = System.getenv("M2_REPO");
        if (m2Repo != null && !m2Repo.isBlank() && Files.isDirectory(Path.of(m2Repo))) {
            return m2Repo.trim();
        }
        String userHome = System.getProperty("user.home", ".");
        Path defaultPath = Path.of(userHome, ".m2", "repository");
        return defaultPath.toAbsolutePath().normalize().toString();
    }

    /**
     * Returns the default System catalogs matching IntelliJ IDEA.
     */
    public static List<CatalogEntry> defaultCatalogs() {
        List<CatalogEntry> list = new ArrayList<>();
        list.add(new CatalogEntry("Internal", "System", "", true));
        list.add(new CatalogEntry("Default Local", "System", resolveDefaultLocalRepo(), true));
        list.add(new CatalogEntry("Maven Central", "System", "https://repo.maven.apache.org/maven2", true));
        return list;
    }

    private static volatile List<String> cachedCentralArchetypes = null;
    private static final Map<String, List<String>> CATALOG_ARCHETYPE_VERSIONS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.atomic.AtomicBoolean refreshingCentral = new java.util.concurrent.atomic.AtomicBoolean(false);

    /**
     * Dynamically loads archetypes for a given catalog.
     */
    public static List<String> getArchetypesForCatalog(CatalogEntry catalog) {
        if (catalog == null || "Internal".equalsIgnoreCase(catalog.name())) {
            return INTERNAL_ARCHETYPES;
        }

        if ("Default Local".equalsIgnoreCase(catalog.name())) {
            return discoverLocalArchetypes(catalog.location());
        }

        if ("Maven Central".equalsIgnoreCase(catalog.name())) {
            return getMavenCentralArchetypes();
        }

        // Custom catalog: local file/directory or remote URL
        String loc = catalog.location();
        if (loc != null && !loc.isBlank()) {
            if (loc.startsWith("http://") || loc.startsWith("https://")) {
                List<String> remote = fetchRemoteCatalog(loc);
                if (!remote.isEmpty()) return remote;
            }
            try {
                Path path = Path.of(loc);
                if (Files.exists(path)) {
                    if (Files.isRegularFile(path)) {
                        return parseCatalogXml(path);
                    } else if (Files.isDirectory(path)) {
                        return discoverLocalArchetypes(loc);
                    }
                }
            } catch (Exception ignored) {}
        }

        return INTERNAL_ARCHETYPES;
    }

    /**
     * Dynamically resolves Maven Central archetypes matching IntelliJ IDEA.
     */
    public static List<String> getMavenCentralArchetypes() {
        if (cachedCentralArchetypes != null && !cachedCentralArchetypes.isEmpty()) {
            return cachedCentralArchetypes;
        }

        String userHome = System.getProperty("user.home", ".");
        Path cachePath = Path.of(userHome, ".cache", "lumina", "archetype-catalog.xml");

        // 1. Check local cache
        if (Files.isRegularFile(cachePath)) {
            try {
                String content = Files.readString(cachePath);
                List<String> list = parseCatalogXmlContent(content);
                if (!list.isEmpty()) {
                    indexCatalogArchetypes(content);
                    cachedCentralArchetypes = list;
                    return list;
                }
            } catch (Exception ignored) {}
        }

        // 2. Bundled classpath resource
        try (var is = MavenArchetypeMetadata.class.getResourceAsStream("/archetypes/archetype-catalog-central.xml")) {
            if (is != null) {
                String content = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                List<String> list = parseCatalogXmlContent(content);
                if (!list.isEmpty()) {
                    indexCatalogArchetypes(content);
                    cachedCentralArchetypes = list;
                    refreshCentralCatalogAsync(cachePath);
                    return list;
                }
            }
        } catch (Exception ignored) {}

        // 3. Fallback
        cachedCentralArchetypes = MAVEN_CENTRAL_POPULAR_ARCHETYPES;
        refreshCentralCatalogAsync(cachePath);
        return cachedCentralArchetypes;
    }

    private static void refreshCentralCatalogAsync(Path cachePath) {
        if (!refreshingCentral.compareAndSet(false, true)) {
            return;
        }
        Thread bg = new Thread(() -> {
            try {
                java.net.URI uri = java.net.URI.create("https://repo.maven.apache.org/maven2/archetype-catalog.xml");
                java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                        .connectTimeout(java.time.Duration.ofSeconds(4))
                        .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                        .build();
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder(uri)
                        .timeout(java.time.Duration.ofSeconds(8))
                        .header("User-Agent", "Lumina-IDE")
                        .GET()
                        .build();
                java.net.http.HttpResponse<String> resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200 && resp.body() != null && !resp.body().isBlank()) {
                    List<String> list = parseCatalogXmlContent(resp.body());
                    if (!list.isEmpty()) {
                        indexCatalogArchetypes(resp.body());
                        cachedCentralArchetypes = list;
                        try {
                            if (cachePath.getParent() != null) {
                                Files.createDirectories(cachePath.getParent());
                            }
                            Files.writeString(cachePath, resp.body());
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception ignored) {
            } finally {
                refreshingCentral.set(false);
            }
        }, "Lumina-CentralCatalogFetcher");
        bg.setDaemon(true);
        bg.start();
    }

    public static List<String> fetchRemoteCatalog(String url) {
        try {
            String fullUrl = url.trim();
            if (!fullUrl.endsWith(".xml")) {
                fullUrl = fullUrl.replaceAll("/+$", "") + "/archetype-catalog.xml";
            }
            java.net.URI uri = java.net.URI.create(fullUrl);
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(4))
                    .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                    .build();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder(uri)
                    .timeout(java.time.Duration.ofSeconds(6))
                    .header("User-Agent", "Lumina-IDE")
                    .GET()
                    .build();
            java.net.http.HttpResponse<String> resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200 && resp.body() != null && !resp.body().isBlank()) {
                List<String> list = parseCatalogXmlContent(resp.body());
                if (!list.isEmpty()) {
                    indexCatalogArchetypes(resp.body());
                    return list;
                }
            }
        } catch (Exception ignored) {}
        return Collections.emptyList();
    }

    /**
     * Parses an archetype-catalog XML string into sorted coordinates.
     */
    public static List<String> parseCatalogXmlContent(String content) {
        if (content == null || content.isBlank()) {
            return Collections.emptyList();
        }
        Set<String> result = new LinkedHashSet<>();
        Pattern archPattern = Pattern.compile("<archetype\\b[^>]*>(.*?)</archetype>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Pattern groupPattern = Pattern.compile("<groupId>\\s*([^<]+)\\s*</groupId>", Pattern.CASE_INSENSITIVE);
        Pattern artifactPattern = Pattern.compile("<artifactId>\\s*([^<]+)\\s*</artifactId>", Pattern.CASE_INSENSITIVE);

        Matcher archMatcher = archPattern.matcher(content);
        while (archMatcher.find()) {
            String block = archMatcher.group(1);
            Matcher gm = groupPattern.matcher(block);
            Matcher am = artifactPattern.matcher(block);
            if (gm.find() && am.find()) {
                String g = gm.group(1).trim();
                String a = am.group(1).trim();
                if (!g.isEmpty() && !a.isEmpty()) {
                    result.add(g + ":" + a);
                }
            }
        }
        List<String> list = new ArrayList<>(result);
        list.sort(String.CASE_INSENSITIVE_ORDER);
        return list;
    }

    /**
     * Indexes archetype versions from catalog XML content.
     */
    public static void indexCatalogArchetypes(String content) {
        if (content == null || content.isBlank()) return;
        Pattern archPattern = Pattern.compile("<archetype\\b[^>]*>(.*?)</archetype>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Pattern groupPattern = Pattern.compile("<groupId>\\s*([^<]+)\\s*</groupId>", Pattern.CASE_INSENSITIVE);
        Pattern artifactPattern = Pattern.compile("<artifactId>\\s*([^<]+)\\s*</artifactId>", Pattern.CASE_INSENSITIVE);
        Pattern versionPattern = Pattern.compile("<version>\\s*([^<]+)\\s*</version>", Pattern.CASE_INSENSITIVE);

        Matcher archMatcher = archPattern.matcher(content);
        while (archMatcher.find()) {
            String block = archMatcher.group(1);
            Matcher gm = groupPattern.matcher(block);
            Matcher am = artifactPattern.matcher(block);
            if (gm.find() && am.find()) {
                String g = gm.group(1).trim();
                String a = am.group(1).trim();
                String coord = g + ":" + a;
                Matcher vm = versionPattern.matcher(block);
                if (vm.find()) {
                    String v = vm.group(1).trim();
                    if (!v.isEmpty()) {
                        CATALOG_ARCHETYPE_VERSIONS.computeIfAbsent(coord, k -> new ArrayList<>()).add(v);
                    }
                }
            }
        }
    }

    /**
     * Discovers archetypes from a local Maven repository directory dynamically.
     */
    public static List<String> discoverLocalArchetypes(String repoLocation) {
        if (repoLocation == null || repoLocation.isBlank()) {
            return Collections.emptyList();
        }
        Path repoPath = Path.of(repoLocation);
        if (!Files.isDirectory(repoPath)) {
            return Collections.emptyList();
        }

        // 1. Check for archetype-catalog.xml
        Path catalogXml = repoPath.resolve("archetype-catalog.xml");
        if (Files.isRegularFile(catalogXml)) {
            List<String> fromXml = parseCatalogXml(catalogXml);
            if (!fromXml.isEmpty()) {
                return fromXml;
            }
        }

        // 2. Scan directory tree for archetype jars / poms
        Set<String> found = new LinkedHashSet<>();
        try (var stream = Files.walk(repoPath, 6)) {
            stream.filter(p -> p.getFileName().toString().contains("archetype")
                            && (p.toString().endsWith(".jar") || p.toString().endsWith(".pom")))
                    .forEach(p -> {
                        String rel = repoPath.relativize(p.getParent()).toString();
                        String[] parts = rel.replace(File.separatorChar, '/').split("/");
                        if (parts.length >= 3) {
                            String artifactId = parts[parts.length - 2];
                            StringBuilder group = new StringBuilder();
                            for (int i = 0; i < parts.length - 2; i++) {
                                if (group.length() > 0) group.append(".");
                                group.append(parts[i]);
                            }
                            if (group.length() > 0 && !artifactId.isBlank()) {
                                found.add(group + ":" + artifactId);
                            }
                        }
                    });
        } catch (Exception ignored) {}

        List<String> list = new ArrayList<>(found);
        list.sort(String.CASE_INSENSITIVE_ORDER);
        return list;
    }

    /**
     * Parses an archetype-catalog.xml file.
     */
    public static List<String> parseCatalogXml(Path xmlPath) {
        if (xmlPath == null || !Files.exists(xmlPath)) {
            return Collections.emptyList();
        }
        try {
            String content = Files.readString(xmlPath);
            indexCatalogArchetypes(content);
            return parseCatalogXmlContent(content);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    /**
     * Resolves supported versions for a chosen archetype dynamically.
     */
    public static List<String> getVersionsForArchetype(String archetypeId) {
        if (archetypeId == null || archetypeId.isBlank()) {
            return Collections.emptyList();
        }
        List<String> cached = CATALOG_ARCHETYPE_VERSIONS.get(archetypeId.trim());
        if (cached != null && !cached.isEmpty()) {
            List<String> list = new ArrayList<>(new LinkedHashSet<>(cached));
            if (!list.contains("RELEASE")) list.add("RELEASE");
            return list;
        }
        String lower = archetypeId.toLowerCase();
        if (lower.contains("quickstart")) {
            return List.of("1.4", "1.5", "1.1", "RELEASE");
        } else if (lower.contains("webapp")) {
            return List.of("1.4", "1.5", "1.0", "RELEASE");
        } else if (lower.contains("quarkus")) {
            return List.of("3.18.1", "3.15.3", "RELEASE");
        } else if (lower.contains("kotlin")) {
            return List.of("2.1.10", "2.0.21", "1.9.24", "RELEASE");
        } else if (lower.contains("scala")) {
            return List.of("1.8.0", "1.7.0", "RELEASE");
        }
        return List.of("1.4", "1.3", "1.2", "1.1", "1.0", "RELEASE");
    }

    /**
     * Generates .idea/modules.xml for a Maven project.
     */
    public static String generateIdeaModulesXml(String projectName) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project version="4">
                  <component name="ProjectModuleManager">
                    <modules>
                      <module fileurl="file://$PROJECT_DIR$/%s.iml" filepath="$PROJECT_DIR$/%s.iml" />
                    </modules>
                  </component>
                </project>
                """.formatted(projectName, projectName);
    }

    /**
     * Generates .idea/misc.xml for a Maven project with language level / JDK.
     */
    public static String generateIdeaMiscXml(String javaVersion) {
        String level = (javaVersion != null && !javaVersion.isBlank()) ? "JDK_" + javaVersion : "JDK_21";
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project version="4">
                  <component name="ExternalStorageConfigurationManager" enabled="true" />
                  <component name="MavenProjectsManager">
                    <option name="originalFiles">
                      <list>
                        <option value="$PROJECT_DIR$/pom.xml" />
                      </list>
                    </option>
                  </component>
                  <component name="ProjectRootManager" version="2" languageLevel="%s" project-jdk-name="%s" project-jdk-type="JavaSDK">
                    <output url="file://$PROJECT_DIR$/out" />
                  </component>
                </project>
                """.formatted(level, javaVersion != null ? javaVersion : "21");
    }

    /**
     * Generates .idea/vcs.xml mapping project root to Git.
     */
    public static String generateIdeaVcsXml() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project version="4">
                  <component name="VcsDirectoryMappings">
                    <mapping directory="" vcs="Git" />
                  </component>
                </project>
                """;
    }

    /**
     * Generates standard root .gitignore for Maven projects.
     */
    public static String generateGitIgnore() {
        return """
                target/
                !.mvn/wrapper/maven-wrapper.jar
                !**/src/main/**/target/
                !**/src/test/**/target/

                ### IntelliJ IDEA ###
                .idea/
                *.iws
                *.iml
                *.ipr

                ### NetBeans ###
                **/nbproject/private/
                **/suite.properties
                **/build.xml
                **/nbbuild/
                **/dist/
                **/nbdist/
                /.nb-mvn-cache/

                ### VS Code ###
                .vscode/
                """;
    }

    /**
     * Generates a clean fallback pom.xml if Maven CLI is unavailable.
     */
    public static String generateFallbackPom(String groupId, String artifactId, String version, String javaVersion) {
        String jv = (javaVersion != null && !javaVersion.isBlank()) ? javaVersion : "21";
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>

                  <groupId>%s</groupId>
                  <artifactId>%s</artifactId>
                  <version>%s</version>

                  <properties>
                    <maven.compiler.source>%s</maven.compiler.source>
                    <maven.compiler.target>%s</maven.compiler.target>
                    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                  </properties>

                  <dependencies>
                    <dependency>
                      <groupId>org.junit.jupiter</groupId>
                      <artifactId>junit-jupiter-api</artifactId>
                      <version>5.11.4</version>
                      <scope>test</scope>
                    </dependency>
                  </dependencies>
                </project>
                """.formatted(groupId, artifactId, version, jv, jv);
    }
}
